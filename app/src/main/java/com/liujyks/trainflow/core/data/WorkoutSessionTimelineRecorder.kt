package com.liujyks.trainflow.core.data

import android.database.sqlite.SQLiteException
import android.os.SystemClock
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.health.HeartRatePersistenceBindResult
import com.liujyks.trainflow.core.health.HeartRatePersistenceBinding
import com.liujyks.trainflow.core.health.HeartRatePersistenceBindingId
import com.liujyks.trainflow.core.health.HeartRatePersistenceBindingDisposition
import com.liujyks.trainflow.core.health.HeartRatePersistenceUnbindResult
import com.liujyks.trainflow.core.health.HeartRateRuntimeObservation
import com.liujyks.trainflow.core.health.HeartRateRuntimeObservationCause
import com.liujyks.trainflow.core.health.HeartRateRuntimeObservationPayload
import com.liujyks.trainflow.core.health.HeartRateRuntimeOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal data class WorkoutTimelineStartRequest(
    val session: WorkoutSessionEntity,
    val initialPhase: WorkoutPhaseIntervalEntity,
    val recording: HeartRateRecordingEntity?
)

internal data class WorkoutTimelineTerminalRequest(
    val terminalStatus: String,
    val terminalReason: String,
    val finalOffsetMs: Long,
    val snapshotCreatedAt: String
)

internal data class WorkoutTimelineOwnerClearRequest(
    val finalOffsetMs: Long,
    val snapshotCreatedAt: String
)

internal data class WorkoutTimelineTerminalResult(
    val sessionId: String,
    val finalTuple: CanonicalTuple,
    val analysisVersion: Int?
)

internal sealed interface WorkoutSessionTimelineRecorderState {
    data object Prepared : WorkoutSessionTimelineRecorderState
    data object Starting : WorkoutSessionTimelineRecorderState
    data object Started : WorkoutSessionTimelineRecorderState
    data class ActivePersistenceFailed(val cause: Throwable) :
        WorkoutSessionTimelineRecorderState
    data object Terminating : WorkoutSessionTimelineRecorderState
    data class TerminalFailed(val cause: Throwable) : WorkoutSessionTimelineRecorderState
    data object Released : WorkoutSessionTimelineRecorderState
}

internal class RecorderBindingDispositionException(message: String) : IllegalStateException(message)

internal class RecorderObservationRejectedException(message: String) : IllegalStateException(message)

internal class RecorderTerminalConflictException(message: String) : IllegalStateException(message)

internal class RecorderOwnerClearPendingException :
    IllegalStateException("Workout timeline recorder owner clear is pending")

internal class WorkoutSessionTimelineRecorder private constructor(
    private val repository: WorkoutSessionRepository,
    private val runtimeOwner: HeartRateRuntimeOwner,
    private val scope: CoroutineScope,
    private val admission: RecorderAdmission,
    private val binding: HeartRatePersistenceBinding,
    private val preparedStartRequest: WorkoutTimelineStartRequest
) {
    private val mutableState = MutableStateFlow<WorkoutSessionTimelineRecorderState>(
        WorkoutSessionTimelineRecorderState.Prepared
    )
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val lifecycleLock = Any()
    private val preStartObservations = mutableListOf(binding.snapshot)
    private var expectedReceipt = 1L
    private var latestCause = snapshotCause(binding.snapshot)
    private var activeSession: ActiveSession? = null
    private var terminalCache: TerminalCache? = null
    private var terminalAttempt: TerminalAttempt? = null
    private var ownerClearHandoff: OwnerClearHandoff? = null
    private var ownerBlockToken: RecorderBlockToken? = null

    val state: StateFlow<WorkoutSessionTimelineRecorderState> = mutableState
    val bindingId: HeartRatePersistenceBindingId = admission.bindingId
    val ownerToken: RecorderOwnerToken = admission.ownerToken

    init {
        scope.launch(NonCancellable) {
            for (command in commands) {
                when (command) {
                    is Command.Observe -> handleObservation(command.observation)
                    is Command.Start -> handleStart(command)
                    is Command.EnableRecording -> handleEnableRecording(command)
                    is Command.SetRecordingExpected -> handleSetRecordingExpected(command)
                    is Command.AppendDisplayMetadata -> handleAppendDisplayMetadata(command)
                    is Command.TransitionPhase -> handleTransitionPhase(command)
                    is Command.Terminalize -> handleTerminal(
                        command.request,
                        command.reply,
                        RecorderTerminalAuthority.ORDINARY
                    )
                    is Command.OwnerClear -> handleOwnerClear(command.request)
                }
            }
        }
    }

    suspend fun start(request: WorkoutTimelineStartRequest): WorkoutTimelineStartResult {
        val reply = CompletableDeferred<WorkoutTimelineStartResult>()
        submitMutation(Command.Start(request, reply))
        return reply.await()
    }

    fun acceptRuntimeObservation(observation: HeartRateRuntimeObservation) {
        synchronized(lifecycleLock) {
            if (ownerClearHandoff == null) {
                commands.trySend(Command.Observe(observation)).getOrThrow()
            }
        }
    }

    suspend fun enableRecording(recording: HeartRateRecordingEntity, offsetMs: Long) {
        val reply = CompletableDeferred<Unit>()
        submitMutation(Command.EnableRecording(recording, offsetMs, reply))
        reply.await()
    }

    suspend fun setRecordingExpected(
        recordingExpected: Boolean,
        userExclusionReason: String?,
        offsetMs: Long
    ) {
        val reply = CompletableDeferred<Unit>()
        submitMutation(
            Command.SetRecordingExpected(
                recordingExpected,
                userExclusionReason,
                offsetMs,
                reply
            )
        )
        reply.await()
    }

    suspend fun appendSessionDisplayMetadata(nextJson: String, offsetMs: Long) {
        val reply = CompletableDeferred<Unit>()
        submitMutation(Command.AppendDisplayMetadata(nextJson, offsetMs, reply))
        reply.await()
    }

    suspend fun transitionPhase(
        phaseId: String,
        phaseKind: String,
        phaseIdentityJson: String,
        offsetMs: Long
    ) {
        val reply = CompletableDeferred<Unit>()
        submitMutation(
            Command.TransitionPhase(
                phaseId,
                phaseKind,
                phaseIdentityJson,
                offsetMs,
                reply
            )
        )
        reply.await()
    }

    suspend fun terminalize(
        request: WorkoutTimelineTerminalRequest
    ): WorkoutTimelineTerminalResult {
        validateTerminalRequest(request, RecorderTerminalAuthority.ORDINARY)
        synchronized(lifecycleLock) {
            terminalCache?.let { cached ->
                if (!cached.matches(request)) {
                    throw RecorderTerminalConflictException(
                        "Terminal request conflicts with durable result"
                    )
                }
                if (mutableState.value == WorkoutSessionTimelineRecorderState.Released) {
                    return cached.result
                }
            }
        }
        val reply = CompletableDeferred<WorkoutTimelineTerminalResult>()
        submitMutation(Command.Terminalize(request, reply))
        return reply.await()
    }

    fun beginOwnerClearHandoff(
        request: WorkoutTimelineOwnerClearRequest
    ): RecorderOwnerClearResult = synchronized(lifecycleLock) {
        if (mutableState.value == WorkoutSessionTimelineRecorderState.Released) {
            return@synchronized RecorderOwnerClearResult.AlreadyReleased
        }
        ownerClearHandoff?.let { existing ->
            return@synchronized RecorderOwnerClearResult.AlreadyPending(existing.handoffToken)
        }
        val transition = repository.beginRecorderOwnerClearHandoff(ownerToken)
        when (transition) {
            is RecorderOwnerClearResult.Pending -> {
                val currentFailure = when (val state = mutableState.value) {
                    is WorkoutSessionTimelineRecorderState.ActivePersistenceFailed -> state.cause
                    is WorkoutSessionTimelineRecorderState.TerminalFailed -> state.cause
                    else -> null
                }
                ownerClearHandoff = OwnerClearHandoff(
                    transition.handoffToken,
                    currentFailure ?: RecorderOwnerClearPendingException()
                )
                commands.trySend(Command.OwnerClear(request)).getOrThrow()
            }
            else -> Unit
        }
        transition
    }

    private fun submitMutation(command: Command) {
        synchronized(lifecycleLock) {
            ownerClearHandoff?.let { throw it.mutationFailure }
            when (val state = mutableState.value) {
                is WorkoutSessionTimelineRecorderState.ActivePersistenceFailed -> throw state.cause
                is WorkoutSessionTimelineRecorderState.TerminalFailed -> {
                    if (command !is Command.Terminalize) throw state.cause
                }
                WorkoutSessionTimelineRecorderState.Released ->
                    throw IllegalStateException("Workout timeline recorder is released")
                else -> Unit
            }
            commands.trySend(command).getOrThrow()
        }
    }

    private suspend fun handleObservation(observation: HeartRateRuntimeObservation) {
        when (mutableState.value) {
            WorkoutSessionTimelineRecorderState.Prepared -> {
                observationRejection(observation)?.let { failure ->
                    failBeforeRoom(failure)
                    return
                }
                expectedReceipt = Math.incrementExact(expectedReceipt)
                preStartObservations += observation
                latestCause = observationCause(observation)
            }
            WorkoutSessionTimelineRecorderState.Started -> persistObservation(observation)
            is WorkoutSessionTimelineRecorderState.ActivePersistenceFailed,
            WorkoutSessionTimelineRecorderState.Starting,
            WorkoutSessionTimelineRecorderState.Terminating,
            is WorkoutSessionTimelineRecorderState.TerminalFailed,
            WorkoutSessionTimelineRecorderState.Released -> Unit
        }
    }

    private suspend fun handleStart(command: Command.Start) {
        if (mutableState.value != WorkoutSessionTimelineRecorderState.Prepared) {
            command.reply.completeExceptionally(
                IllegalStateException("Workout timeline recorder is not prepared")
            )
            return
        }
        if (command.request != preparedStartRequest) {
            failStart(
                command.reply,
                IllegalArgumentException("Start request does not match the validated prepared request")
            )
            return
        }
        mutableState.value = WorkoutSessionTimelineRecorderState.Starting
        val startAnchorMs = SystemClock.elapsedRealtime()
        try {
            val prepared = buildStartGraph(command.request, preStartObservations)
            val result = repository.commitRecorderStart(ownerToken, prepared.graph)
            activeSession = prepared.active.copy(startAnchorMs = startAnchorMs)
            mutableState.value = WorkoutSessionTimelineRecorderState.Started
            command.reply.complete(result)
        } catch (failure: SQLiteException) {
            failStart(command.reply, failure)
        } catch (failure: RecorderGuardedWriteException) {
            failStart(command.reply, failure)
        } catch (failure: RecorderValidationException) {
            failStart(command.reply, failure)
        } catch (failure: ArithmeticException) {
            failStart(command.reply, failure)
        } catch (failure: IllegalArgumentException) {
            failStart(command.reply, failure)
        } catch (failure: IllegalStateException) {
            failStart(command.reply, failure)
        }
    }

    private fun failStart(
        reply: CompletableDeferred<WorkoutTimelineStartResult>,
        failure: Throwable
    ) {
        failBeforeRoom(failure)
        reply.completeExceptionally(failure)
    }

    private fun failBeforeRoom(primary: Throwable) {
        when (val disposition = runtimeOwner.persistenceBindingDisposition(bindingId)) {
            is HeartRatePersistenceBindingDisposition.KnownAbsent -> {
                releaseFailedStart(primary, RecorderCleanupProof.KnownAbsent(disposition))
            }
            is HeartRatePersistenceBindingDisposition.MatchingInstalled -> {
                when (val unbind = runtimeOwner.exactUnbindPersistenceSink(bindingId)) {
                    is HeartRatePersistenceUnbindResult.Unbound -> releaseFailedStart(
                        primary,
                        RecorderCleanupProof.Unbound(unbind)
                    )
                    is HeartRatePersistenceUnbindResult.KnownAbsent -> releaseFailedStart(
                        primary,
                        RecorderCleanupProof.ExactKnownAbsent(unbind)
                    )
                    is HeartRatePersistenceUnbindResult.ConflictingInstalled -> {
                        val secondary = unbind.conflictFailure()
                        blockOwner(primary, secondary)
                        mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(primary)
                    }
                    is HeartRatePersistenceUnbindResult.Unresolved -> {
                        val secondary = unbind.unresolvedFailure()
                        blockOwner(primary, secondary)
                        mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(primary)
                    }
                }
            }
            is HeartRatePersistenceBindingDisposition.ConflictingInstalled -> {
                val secondary = disposition.conflictFailure()
                blockOwner(primary, secondary)
                mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(primary)
            }
            is HeartRatePersistenceBindingDisposition.Unresolved -> {
                val secondary = disposition.unresolvedFailure()
                blockOwner(primary, secondary)
                mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(primary)
            }
        }
    }

    private fun releaseFailedStart(primary: Throwable, cleanupProof: RecorderCleanupProof) {
        when (val release = releaseOwner(cleanupProof)) {
            RecorderOwnerReleaseResult.Released -> {
                mutableState.value = WorkoutSessionTimelineRecorderState.Released
            }
            else -> {
                val secondary = RecorderBindingDispositionException(
                    "Failed-start owner release did not complete: $release"
                )
                blockOwner(primary, secondary)
                mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(primary)
            }
        }
    }

    private fun releaseOwner(cleanupProof: RecorderCleanupProof): RecorderOwnerReleaseResult =
        repository.releaseRecorderOwner(
            ownerToken = ownerToken,
            cleanupProof = cleanupProof,
            handoffToken = ownerClearHandoff?.handoffToken,
            blockToken = ownerBlockToken
        )

    private fun blockOwner(primary: Throwable, secondary: Throwable?): RecorderOwnerBlockResult {
        val result = repository.blockRecorderOwner(
            ownerToken = ownerToken,
            primaryCause = primary,
            secondaryCause = secondary,
            handoffToken = ownerClearHandoff?.handoffToken
        )
        ownerBlockToken = when (result) {
            is RecorderOwnerBlockResult.Blocked -> result.blockToken
            is RecorderOwnerBlockResult.AlreadyBlocked -> result.blockToken
            RecorderOwnerBlockResult.Stale -> ownerBlockToken
        }
        return result
    }

    private suspend fun persistObservation(observation: HeartRateRuntimeObservation) {
        val rejection = observationRejection(observation)
        if (rejection != null) {
            mutableState.value = WorkoutSessionTimelineRecorderState.ActivePersistenceFailed(rejection)
            return
        }
        expectedReceipt = Math.incrementExact(expectedReceipt)
        val active = requireNotNull(activeSession)
        latestCause = observationCause(observation)
        if (active.recordingId == null) return

        val offsetMs = maxOf(
            active.tuple.offsetMs,
            Math.subtractExact(observation.elapsedRealtimeMs, active.startAnchorMs).coerceAtLeast(0)
        )
        val nextTuple = CanonicalTuple(
            offsetMs,
            Math.incrementExact(active.tuple.mutationSequence)
        )
        try {
            when (val payload = observation.payload) {
                is HeartRateRuntimeObservationPayload.CurrentSnapshot ->
                    persistAcquisitionTransition(active, nextTuple, payload.cause)
                is HeartRateRuntimeObservationPayload.RuntimeTransition ->
                    persistAcquisitionTransition(active, nextTuple, payload.cause)
                is HeartRateRuntimeObservationPayload.ValidMeasurement ->
                    persistMeasurement(active, nextTuple, payload.bpm)
            }
        } catch (failure: SQLiteException) {
            markActiveFailure(failure)
        } catch (failure: RecorderGuardedWriteException) {
            markActiveFailure(failure)
        } catch (failure: RecorderValidationException) {
            markActiveFailure(failure)
        } catch (failure: ArithmeticException) {
            markActiveFailure(failure)
        } catch (failure: IllegalArgumentException) {
            markActiveFailure(failure)
        } catch (failure: IllegalStateException) {
            markActiveFailure(failure)
        }
    }

    private suspend fun persistAcquisitionTransition(
        active: ActiveSession,
        ignoredNextTuple: CanonicalTuple,
        cause: HeartRateRuntimeObservationCause
    ) {
        val fact = CanonicalHeartRateObservationMapper.acquisition(
            cause,
            active.recordingExpected,
            active.userExclusionReason
        )
        if (fact == active.acquisitionFact) return
        val nextTuple = active.nextTuple(ignoredNextTuple.offsetMs)
        val nextSequence = Math.incrementExact(active.acquisitionSequence)
        val next = acquisition(
            recordingId = requireNotNull(active.recordingId),
            sequence = nextSequence,
            tuple = nextTuple,
            fact = fact
        )
        repository.transitionRecorderAcquisition(
            ownerToken,
            active.expected(),
            nextTuple,
            next
        )
        active.tuple = nextTuple
        active.acquisitionSequence = nextSequence
        active.openAcquisitionId = next.id
        active.acquisitionFact = fact
    }

    private suspend fun persistMeasurement(
        active: ActiveSession,
        nextTuple: CanonicalTuple,
        bpm: Int
    ) {
        val recordingId = requireNotNull(active.recordingId)
        val sample = HeartRateSampleEntity(
            recordingId = recordingId,
            sampleSequence = active.nextSampleSequence,
            offsetMs = nextTuple.offsetMs,
            mutationSequence = nextTuple.mutationSequence,
            bpm = bpm
        )
        val liveFact = CanonicalHeartRateObservationMapper.acquisition(
            HeartRateRuntimeObservationCause.LIVE,
            active.recordingExpected,
            active.userExclusionReason
        )
        if (active.acquisitionFact == liveFact) {
            repository.appendRecorderSample(ownerToken, active.expected(), nextTuple, sample)
        } else {
            val nextSequence = Math.incrementExact(active.acquisitionSequence)
            val next = acquisition(
                recordingId,
                nextSequence,
                nextTuple,
                liveFact
            )
            repository.transitionRecorderAcquisitionAndAppendSample(
                ownerToken,
                active.expected(),
                nextTuple,
                next,
                sample
            )
            active.acquisitionSequence = nextSequence
            active.openAcquisitionId = next.id
            active.acquisitionFact = liveFact
        }
        active.tuple = nextTuple
        active.nextSampleSequence = Math.incrementExact(active.nextSampleSequence)
    }

    private suspend fun handleEnableRecording(command: Command.EnableRecording) {
        val active = activeForMutation(command.reply) ?: return
        if (active.recordingId != null) {
            command.reply.completeExceptionally(IllegalStateException("Recording already exists"))
            return
        }
        try {
            val nextTuple = active.nextTuple(command.offsetMs)
            val recording = command.recording.copy(
                sessionId = active.sessionId,
                status = "active",
                startedOffsetMs = nextTuple.offsetMs,
                startedMutationSequence = nextTuple.mutationSequence,
                endedOffsetMs = null,
                endedMutationSequence = null,
                originalAnalysisVersion = null
            )
            val fact = CanonicalHeartRateObservationMapper.acquisition(
                latestCause,
                recordingExpected = true,
                userExclusionReason = null
            )
            val initial = acquisition(recording.recordingId, 0, nextTuple, fact)
            repository.startRecorderHeartRateRecording(
                ownerToken,
                active.expected(),
                nextTuple,
                recording,
                initial
            )
            active.tuple = nextTuple
            active.recordingId = recording.recordingId
            active.openAcquisitionId = initial.id
            active.acquisitionSequence = 0
            active.nextSampleSequence = 0
            active.recordingExpected = true
            active.userExclusionReason = null
            active.acquisitionFact = fact
            command.reply.complete(Unit)
        } catch (failure: SQLiteException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: RecorderGuardedWriteException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: RecorderValidationException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: ArithmeticException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: IllegalArgumentException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: IllegalStateException) {
            failActiveCommand(command.reply, failure)
        }
    }

    private suspend fun handleSetRecordingExpected(command: Command.SetRecordingExpected) {
        val active = activeForMutation(command.reply) ?: return
        if (active.recordingId == null) {
            command.reply.completeExceptionally(IllegalStateException("Active recording is required"))
            return
        }
        try {
            val nextTuple = active.nextTuple(command.offsetMs)
            val fact = CanonicalHeartRateObservationMapper.acquisition(
                latestCause,
                command.recordingExpected,
                command.userExclusionReason
            )
            if (fact == active.acquisitionFact) {
                command.reply.complete(Unit)
                return
            }
            val nextSequence = Math.incrementExact(active.acquisitionSequence)
            val next = acquisition(active.recordingId!!, nextSequence, nextTuple, fact)
            repository.transitionRecorderAcquisition(
                ownerToken,
                active.expected(),
                nextTuple,
                next
            )
            active.tuple = nextTuple
            active.openAcquisitionId = next.id
            active.acquisitionSequence = nextSequence
            active.recordingExpected = command.recordingExpected
            active.userExclusionReason = command.userExclusionReason
            active.acquisitionFact = fact
            command.reply.complete(Unit)
        } catch (failure: SQLiteException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: RecorderGuardedWriteException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: RecorderValidationException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: ArithmeticException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: IllegalArgumentException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: IllegalStateException) {
            failActiveCommand(command.reply, failure)
        }
    }

    private suspend fun handleAppendDisplayMetadata(command: Command.AppendDisplayMetadata) {
        val active = activeForMutation(command.reply) ?: return
        try {
            val nextTuple = active.nextTuple(command.offsetMs)
            repository.appendRecorderSessionDisplayMetadata(
                ownerToken,
                active.expected(),
                nextTuple,
                command.nextJson
            )
            active.tuple = nextTuple
            command.reply.complete(Unit)
        } catch (failure: SQLiteException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: RecorderGuardedWriteException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: RecorderValidationException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: ArithmeticException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: IllegalArgumentException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: IllegalStateException) {
            failActiveCommand(command.reply, failure)
        }
    }

    private suspend fun handleTransitionPhase(command: Command.TransitionPhase) {
        val active = activeForMutation(command.reply) ?: return
        try {
            val nextTuple = active.nextTuple(command.offsetMs)
            val nextSequence = Math.incrementExact(active.phaseSequence)
            val nextPhase = WorkoutPhaseIntervalEntity(
                id = command.phaseId,
                sessionId = active.sessionId,
                sequence = nextSequence,
                startOffsetMs = nextTuple.offsetMs,
                endOffsetMs = null,
                startMutationSequence = nextTuple.mutationSequence,
                endMutationSequence = null,
                openMarker = 1,
                phaseKind = command.phaseKind,
                phaseIdentityJson = command.phaseIdentityJson
            )
            repository.transitionRecorderPhase(
                ownerToken,
                active.expected(),
                nextTuple,
                nextPhase
            )
            active.tuple = nextTuple
            active.openPhaseId = nextPhase.id
            active.phaseSequence = nextSequence
            command.reply.complete(Unit)
        } catch (failure: SQLiteException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: RecorderGuardedWriteException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: RecorderValidationException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: ArithmeticException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: IllegalArgumentException) {
            failActiveCommand(command.reply, failure)
        } catch (failure: IllegalStateException) {
            failActiveCommand(command.reply, failure)
        }
    }

    private fun activeForMutation(reply: CompletableDeferred<Unit>): ActiveSession? {
        return when (val state = mutableState.value) {
            WorkoutSessionTimelineRecorderState.Started -> activeSession
                ?: error("Started recorder must have an active session")
            is WorkoutSessionTimelineRecorderState.ActivePersistenceFailed -> {
                reply.completeExceptionally(state.cause)
                null
            }
            is WorkoutSessionTimelineRecorderState.TerminalFailed -> {
                reply.completeExceptionally(state.cause)
                null
            }
            else -> {
                reply.completeExceptionally(IllegalStateException("Recorder is not mutable"))
                null
            }
        }
    }

    private fun failActiveCommand(reply: CompletableDeferred<Unit>, failure: Throwable) {
        markActiveFailure(failure)
        reply.completeExceptionally(failure)
    }

    private fun markActiveFailure(failure: Throwable) {
        val current = mutableState.value
        if (current !is WorkoutSessionTimelineRecorderState.ActivePersistenceFailed) {
            mutableState.value = WorkoutSessionTimelineRecorderState.ActivePersistenceFailed(failure)
        }
    }

    private suspend fun handleTerminal(
        request: WorkoutTimelineTerminalRequest,
        reply: CompletableDeferred<WorkoutTimelineTerminalResult>?,
        authority: RecorderTerminalAuthority
    ) {
        try {
            validateTerminalRequest(request, authority)
        } catch (failure: RecorderValidationException) {
            reply?.completeExceptionally(failure)
            return
        }
        val cached = terminalCache
        if (cached != null) {
            if (!cached.matches(request)) {
                reply?.completeExceptionally(
                    RecorderTerminalConflictException("Terminal request conflicts with durable result")
                )
                return
            }
            if (mutableState.value == WorkoutSessionTimelineRecorderState.Released) {
                reply?.complete(cached.result)
                return
            }
            try {
                finishRelease(cached.result)
                reply?.complete(cached.result)
            } catch (failure: IllegalStateException) {
                mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(failure)
                reply?.completeExceptionally(failure)
            }
            return
        }
        val active = activeSession
        if (active == null || mutableState.value == WorkoutSessionTimelineRecorderState.Released) {
            reply?.completeExceptionally(IllegalStateException("Recorder has no active session"))
            return
        }
        val activeFailure = mutableState.value as?
            WorkoutSessionTimelineRecorderState.ActivePersistenceFailed
        if (activeFailure != null) {
            reply?.completeExceptionally(activeFailure.cause)
            return
        }
        val attempt = terminalAttempt
        if (attempt != null && !attempt.matches(request, active.tuple, authority)) {
            reply?.completeExceptionally(
                RecorderTerminalConflictException("Terminal request conflicts with first intent")
            )
            return
        }
        val effectiveRequest = attempt?.request ?: request
        if (attempt == null) {
            terminalAttempt = TerminalAttempt(request, active.tuple, authority)
        }
        mutableState.value = WorkoutSessionTimelineRecorderState.Terminating
        try {
            val result = if (active.recordingId == null) {
                repository.finalizeRecorderWithoutRecording(
                    ownerToken,
                    NoRecordingFinalizationRequest(
                        sessionId = active.sessionId,
                        expectedStatus = "active",
                        expectedTuple = active.tuple,
                        finalOffsetMs = effectiveRequest.finalOffsetMs,
                        terminalStatus = effectiveRequest.terminalStatus,
                        terminalReason = effectiveRequest.terminalReason,
                        authority = authority
                    )
                )
            } else {
                val finalized = repository.finalizeRecorderRecording(
                    ownerToken,
                    RecordingFinalizationRequest(
                        sessionId = active.sessionId,
                        recordingId = active.recordingId!!,
                        expectedStatus = "active",
                        expectedTuple = active.tuple,
                        finalOffsetMs = effectiveRequest.finalOffsetMs,
                        terminalStatus = effectiveRequest.terminalStatus,
                        terminalReason = effectiveRequest.terminalReason,
                        snapshotCreatedAt = effectiveRequest.snapshotCreatedAt,
                        authority = authority
                    )
                )
                WorkoutTimelineTerminalResult(
                    finalized.sessionId,
                    finalized.finalTuple,
                    finalized.analysisVersion
                )
            }
            terminalCache = TerminalCache(effectiveRequest, active.tuple, result)
            finishRelease(result)
            reply?.complete(result)
        } catch (failure: SQLiteException) {
            failTerminal(reply, failure)
        } catch (failure: RecorderGuardedWriteException) {
            failTerminal(reply, failure)
        } catch (failure: RecorderValidationException) {
            failTerminal(reply, failure)
        } catch (failure: ArithmeticException) {
            failTerminal(reply, failure)
        } catch (failure: IllegalArgumentException) {
            failTerminal(reply, failure)
        } catch (failure: IllegalStateException) {
            failTerminal(reply, failure)
        }
    }

    private suspend fun finishRelease(result: WorkoutTimelineTerminalResult) {
        terminalCache = requireNotNull(terminalCache)
        repository.invalidateRecorderGateCache(ownerToken)
        val cleanupProof = when (val unbind = runtimeOwner.exactUnbindPersistenceSink(bindingId)) {
            is HeartRatePersistenceUnbindResult.Unbound -> RecorderCleanupProof.Unbound(unbind)
            is HeartRatePersistenceUnbindResult.KnownAbsent ->
                RecorderCleanupProof.ExactKnownAbsent(unbind)
            is HeartRatePersistenceUnbindResult.ConflictingInstalled -> {
                val failure = unbind.conflictFailure()
                blockOwner(failure, null)
                throw failure
            }
            is HeartRatePersistenceUnbindResult.Unresolved -> {
                val failure = unbind.unresolvedFailure()
                blockOwner(failure, null)
                throw failure
            }
        }
        val release = releaseOwner(cleanupProof)
        if (release != RecorderOwnerReleaseResult.Released) {
            val failure = RecorderBindingDispositionException(
                "Recorder owner release did not complete: $release"
            )
            blockOwner(failure, null)
            throw failure
        }
        activeSession?.tuple = result.finalTuple
        mutableState.value = WorkoutSessionTimelineRecorderState.Released
    }

    private fun failTerminal(
        reply: CompletableDeferred<WorkoutTimelineTerminalResult>?,
        failure: Throwable
    ) {
        if (ownerClearHandoff != null) {
            blockOwner(failure, null)
        }
        mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(failure)
        reply?.completeExceptionally(failure)
    }

    private suspend fun handleOwnerClear(request: WorkoutTimelineOwnerClearRequest) {
        when (val state = mutableState.value) {
            WorkoutSessionTimelineRecorderState.Prepared -> {
                when (val unbind = runtimeOwner.exactUnbindPersistenceSink(bindingId)) {
                    is HeartRatePersistenceUnbindResult.Unbound ->
                        releasePreparedOwner(RecorderCleanupProof.Unbound(unbind))
                    is HeartRatePersistenceUnbindResult.KnownAbsent ->
                        releasePreparedOwner(RecorderCleanupProof.ExactKnownAbsent(unbind))
                    is HeartRatePersistenceUnbindResult.ConflictingInstalled -> {
                        val failure = unbind.conflictFailure()
                        blockOwner(failure, null)
                        mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(failure)
                    }
                    is HeartRatePersistenceUnbindResult.Unresolved -> {
                        val failure = unbind.unresolvedFailure()
                        blockOwner(failure, null)
                        mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(failure)
                    }
                }
            }
            WorkoutSessionTimelineRecorderState.Started -> handleTerminal(
                WorkoutTimelineTerminalRequest(
                    terminalStatus = "abandoned",
                    terminalReason = "owner_cleared",
                    finalOffsetMs = request.finalOffsetMs,
                    snapshotCreatedAt = request.snapshotCreatedAt
                ),
                reply = null,
                authority = RecorderTerminalAuthority.OWNER_CLEAR
            )
            is WorkoutSessionTimelineRecorderState.ActivePersistenceFailed -> {
                blockOwner(state.cause, null)
                mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(state.cause)
            }
            is WorkoutSessionTimelineRecorderState.TerminalFailed -> blockOwner(state.cause, null)
            WorkoutSessionTimelineRecorderState.Starting,
            WorkoutSessionTimelineRecorderState.Terminating,
            WorkoutSessionTimelineRecorderState.Released -> Unit
        }
    }

    private fun releasePreparedOwner(cleanupProof: RecorderCleanupProof) {
        val release = releaseOwner(cleanupProof)
        if (release == RecorderOwnerReleaseResult.Released) {
            mutableState.value = WorkoutSessionTimelineRecorderState.Released
        } else {
            val failure = RecorderBindingDispositionException(
                "Prepared owner release did not complete: $release"
            )
            blockOwner(failure, null)
            mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(failure)
        }
    }

    private fun validateTerminalRequest(
        request: WorkoutTimelineTerminalRequest,
        authority: RecorderTerminalAuthority
    ) {
        val validPair = when (authority) {
            RecorderTerminalAuthority.ORDINARY ->
                (request.terminalStatus == "completed" && request.terminalReason == "completed") ||
                    (request.terminalStatus == "abandoned" &&
                        request.terminalReason == "user_abandoned")
            RecorderTerminalAuthority.OWNER_CLEAR ->
                request.terminalStatus == "abandoned" &&
                    request.terminalReason == "owner_cleared"
            RecorderTerminalAuthority.FRESH_PROCESS -> false
        }
        if (!validPair) throw RecorderValidationException("invalid_terminal_authority_v1")
        if (request.finalOffsetMs < 0) {
            throw RecorderValidationException("invalid_final_tuple_v1")
        }
        if (request.snapshotCreatedAt.isEmpty()) {
            throw RecorderValidationException("invalid_snapshot_created_at_v1")
        }
    }

    private fun observationRejection(
        observation: HeartRateRuntimeObservation
    ): RecorderObservationRejectedException? = when {
        observation.bindingId != bindingId -> RecorderObservationRejectedException(
            "Observation binding ${observation.bindingId.value} does not match ${bindingId.value}"
        )
        observation.receipt != expectedReceipt -> RecorderObservationRejectedException(
            "Observation receipt ${observation.receipt} does not match $expectedReceipt"
        )
        else -> null
    }

    private fun buildStartGraph(
        request: WorkoutTimelineStartRequest,
        observations: List<HeartRateRuntimeObservation>
    ): PreparedStart {
        require(request.initialPhase.sessionId == request.session.id) {
            "Initial phase must belong to the session"
        }
        require(observations.firstOrNull() == binding.snapshot) {
            "Recorder start requires its exact receipt-zero snapshot"
        }
        val initialTuple = CanonicalTuple(
            requireNotNull(request.session.lastDurableOffsetMs),
            requireNotNull(request.session.lastMutationSequence)
        )
        var tuple = initialTuple
        var acquisitionSequence = -1
        var sampleSequence = 0L
        var currentCause = snapshotCause(binding.snapshot)
        var currentFact: CanonicalHeartRateAcquisitionFact? = null
        val acquisitions = mutableListOf<HeartRateAcquisitionIntervalEntity>()
        val samples = mutableListOf<HeartRateSampleEntity>()
        val recording = request.recording?.copy(
            sessionId = request.session.id,
            status = "active",
            startedOffsetMs = initialTuple.offsetMs,
            startedMutationSequence = initialTuple.mutationSequence,
            endedOffsetMs = null,
            endedMutationSequence = null,
            originalAnalysisVersion = null
        )
        if (recording != null) {
            acquisitionSequence = 0
            currentFact = CanonicalHeartRateObservationMapper.acquisition(
                currentCause,
                recordingExpected = true,
                userExclusionReason = null
            )
            acquisitions += acquisition(
                recording.recordingId,
                acquisitionSequence,
                tuple,
                currentFact
            )
            observations.drop(1).forEach { observation ->
                when (val payload = observation.payload) {
                    is HeartRateRuntimeObservationPayload.CurrentSnapshot -> {
                        currentCause = payload.cause
                        val nextFact = CanonicalHeartRateObservationMapper.acquisition(
                            currentCause,
                            recordingExpected = true,
                            userExclusionReason = null
                        )
                        if (nextFact != currentFact) {
                            tuple = CanonicalTuple(
                                initialTuple.offsetMs,
                                Math.incrementExact(tuple.mutationSequence)
                            )
                            acquisitionSequence = Math.incrementExact(acquisitionSequence)
                            closeLastAcquisition(acquisitions, tuple)
                            acquisitions += acquisition(
                                recording.recordingId,
                                acquisitionSequence,
                                tuple,
                                nextFact
                            )
                            currentFact = nextFact
                        }
                    }
                    is HeartRateRuntimeObservationPayload.RuntimeTransition -> {
                        currentCause = payload.cause
                        val nextFact = CanonicalHeartRateObservationMapper.acquisition(
                            currentCause,
                            recordingExpected = true,
                            userExclusionReason = null
                        )
                        if (nextFact != currentFact) {
                            tuple = CanonicalTuple(
                                initialTuple.offsetMs,
                                Math.incrementExact(tuple.mutationSequence)
                            )
                            acquisitionSequence = Math.incrementExact(acquisitionSequence)
                            closeLastAcquisition(acquisitions, tuple)
                            acquisitions += acquisition(
                                recording.recordingId,
                                acquisitionSequence,
                                tuple,
                                nextFact
                            )
                            currentFact = nextFact
                        }
                    }
                    is HeartRateRuntimeObservationPayload.ValidMeasurement -> {
                        currentCause = HeartRateRuntimeObservationCause.LIVE
                        val liveFact = CanonicalHeartRateObservationMapper.acquisition(
                            currentCause,
                            recordingExpected = true,
                            userExclusionReason = null
                        )
                        tuple = CanonicalTuple(
                            initialTuple.offsetMs,
                            Math.incrementExact(tuple.mutationSequence)
                        )
                        if (liveFact != currentFact) {
                            acquisitionSequence = Math.incrementExact(acquisitionSequence)
                            closeLastAcquisition(acquisitions, tuple)
                            acquisitions += acquisition(
                                recording.recordingId,
                                acquisitionSequence,
                                tuple,
                                liveFact
                            )
                            currentFact = liveFact
                        }
                        samples += HeartRateSampleEntity(
                            recording.recordingId,
                            sampleSequence,
                            tuple.offsetMs,
                            tuple.mutationSequence,
                            payload.bpm
                        )
                        sampleSequence = Math.incrementExact(sampleSequence)
                    }
                }
            }
        } else {
            observations.drop(1).forEach { currentCause = observationCause(it) }
        }
        val session = request.session.copy(
            lastDurableOffsetMs = tuple.offsetMs,
            lastMutationSequence = tuple.mutationSequence
        )
        val graph = CanonicalSessionGraphV1(
            session = session,
            phases = listOf(request.initialPhase),
            recording = recording,
            acquisitions = acquisitions,
            samples = samples
        )
        return PreparedStart(
            graph,
            ActiveSession(
                sessionId = session.id,
                openPhaseId = request.initialPhase.id,
                tuple = tuple,
                startAnchorMs = 0,
                recordingId = recording?.recordingId,
                openAcquisitionId = acquisitions.lastOrNull()?.id,
                acquisitionSequence = acquisitionSequence,
                nextSampleSequence = sampleSequence,
                recordingExpected = recording != null,
                userExclusionReason = null,
                acquisitionFact = currentFact,
                phaseSequence = request.initialPhase.sequence
            )
        )
    }

    private fun ActiveSession.nextTuple(offsetMs: Long): CanonicalTuple {
        require(offsetMs >= tuple.offsetMs) { "Recorder offset must be monotonic" }
        return CanonicalTuple(offsetMs, Math.incrementExact(tuple.mutationSequence))
    }

    private fun ActiveSession.expected() = RecorderExpectedState(
        sessionId = sessionId,
        status = "active",
        durableTuple = tuple,
        openPhaseId = openPhaseId,
        recordingId = recordingId,
        openAcquisitionId = openAcquisitionId
    )

    private fun acquisition(
        recordingId: String,
        sequence: Int,
        tuple: CanonicalTuple,
        fact: CanonicalHeartRateAcquisitionFact
    ) = HeartRateAcquisitionIntervalEntity(
        id = "$recordingId:acquisition:$sequence",
        recordingId = recordingId,
        sequence = sequence,
        startOffsetMs = tuple.offsetMs,
        endOffsetMs = null,
        startMutationSequence = tuple.mutationSequence,
        endMutationSequence = null,
        openMarker = 1,
        recordingIntent = fact.recordingIntent,
        intentReason = fact.intentReason,
        deviceState = fact.deviceState,
        deviceReason = fact.deviceReason
    )

    private fun closeLastAcquisition(
        acquisitions: MutableList<HeartRateAcquisitionIntervalEntity>,
        tuple: CanonicalTuple
    ) {
        val previous = acquisitions.removeAt(acquisitions.lastIndex)
        acquisitions += previous.copy(
            endOffsetMs = tuple.offsetMs,
            endMutationSequence = tuple.mutationSequence,
            openMarker = null
        )
    }

    private fun observationCause(observation: HeartRateRuntimeObservation) =
        when (val payload = observation.payload) {
            is HeartRateRuntimeObservationPayload.CurrentSnapshot -> payload.cause
            is HeartRateRuntimeObservationPayload.RuntimeTransition -> payload.cause
            is HeartRateRuntimeObservationPayload.ValidMeasurement ->
                HeartRateRuntimeObservationCause.LIVE
        }

    private fun snapshotCause(observation: HeartRateRuntimeObservation) =
        (observation.payload as? HeartRateRuntimeObservationPayload.CurrentSnapshot)?.cause
            ?: throw IllegalArgumentException("Binding snapshot must be CurrentSnapshot")

    private fun HeartRatePersistenceBindingDisposition.ConflictingInstalled.conflictFailure() =
        RecorderBindingDispositionException(
            "Binding ${installedBindingId.value} conflicts with ${requestedBindingId.value}"
        )

    private fun HeartRatePersistenceBindingDisposition.Unresolved.unresolvedFailure() =
        RecorderBindingDispositionException(
            "Binding disposition is unresolved for ${requestedBindingId.value}"
        )

    private fun HeartRatePersistenceUnbindResult.ConflictingInstalled.conflictFailure() =
        RecorderBindingDispositionException(
            "Binding ${installedBindingId.value} conflicts with exact unbind " +
                requestedBindingId.value
        )

    private fun HeartRatePersistenceUnbindResult.Unresolved.unresolvedFailure() =
        RecorderBindingDispositionException(
            "Exact unbind is unresolved for ${requestedBindingId.value}"
        )

    private data class PreparedStart(
        val graph: CanonicalSessionGraphV1,
        val active: ActiveSession
    )

    private data class ActiveSession(
        val sessionId: String,
        var openPhaseId: String,
        var tuple: CanonicalTuple,
        val startAnchorMs: Long,
        var recordingId: String?,
        var openAcquisitionId: String?,
        var acquisitionSequence: Int,
        var nextSampleSequence: Long,
        var recordingExpected: Boolean,
        var userExclusionReason: String?,
        var acquisitionFact: CanonicalHeartRateAcquisitionFact?,
        var phaseSequence: Int
    )

    private data class TerminalCache(
        val request: WorkoutTimelineTerminalRequest,
        val predecessor: CanonicalTuple,
        val result: WorkoutTimelineTerminalResult
    ) {
        fun matches(candidate: WorkoutTimelineTerminalRequest): Boolean =
            request.terminalStatus == candidate.terminalStatus &&
                request.terminalReason == candidate.terminalReason &&
                request.finalOffsetMs == candidate.finalOffsetMs
    }

    private data class TerminalAttempt(
        val request: WorkoutTimelineTerminalRequest,
        val predecessor: CanonicalTuple,
        val authority: RecorderTerminalAuthority
    ) {
        fun matches(
            candidate: WorkoutTimelineTerminalRequest,
            candidatePredecessor: CanonicalTuple,
            candidateAuthority: RecorderTerminalAuthority
        ): Boolean = authority == candidateAuthority &&
            predecessor == candidatePredecessor &&
            request.terminalStatus == candidate.terminalStatus &&
            request.terminalReason == candidate.terminalReason &&
            request.finalOffsetMs == candidate.finalOffsetMs
    }

    private data class OwnerClearHandoff(
        val handoffToken: RecorderHandoffToken,
        val mutationFailure: Throwable
    )

    private sealed interface Command {
        data class Observe(val observation: HeartRateRuntimeObservation) : Command
        data class Start(
            val request: WorkoutTimelineStartRequest,
            val reply: CompletableDeferred<WorkoutTimelineStartResult>
        ) : Command
        data class EnableRecording(
            val recording: HeartRateRecordingEntity,
            val offsetMs: Long,
            val reply: CompletableDeferred<Unit>
        ) : Command
        data class SetRecordingExpected(
            val recordingExpected: Boolean,
            val userExclusionReason: String?,
            val offsetMs: Long,
            val reply: CompletableDeferred<Unit>
        ) : Command
        data class AppendDisplayMetadata(
            val nextJson: String,
            val offsetMs: Long,
            val reply: CompletableDeferred<Unit>
        ) : Command
        data class TransitionPhase(
            val phaseId: String,
            val phaseKind: String,
            val phaseIdentityJson: String,
            val offsetMs: Long,
            val reply: CompletableDeferred<Unit>
        ) : Command
        data class Terminalize(
            val request: WorkoutTimelineTerminalRequest,
            val reply: CompletableDeferred<WorkoutTimelineTerminalResult>
        ) : Command
        data class OwnerClear(val request: WorkoutTimelineOwnerClearRequest) : Command
    }

    companion object {
        suspend fun prepare(
            entryId: String,
            startRequest: WorkoutTimelineStartRequest,
            repository: WorkoutSessionRepository,
            runtimeOwner: HeartRateRuntimeOwner,
            scope: CoroutineScope
        ): WorkoutSessionTimelineRecorder {
            validateStartRequest(startRequest)
            val admission = repository.admitRecorder(entryId)
            val pending = ArrayDeque<HeartRateRuntimeObservation>()
            var recorder: WorkoutSessionTimelineRecorder? = null
            val bindResult = runtimeOwner.bindPersistenceSink(admission.bindingId) { observation ->
                val target = recorder
                if (target == null) pending.addLast(observation) else {
                    target.acceptRuntimeObservation(observation)
                }
            }
            val binding = when (bindResult) {
                is HeartRatePersistenceBindResult.Installed -> bindResult.binding
                is HeartRatePersistenceBindResult.MatchingInstalled -> bindResult.binding
                is HeartRatePersistenceBindResult.ConflictingInstalled -> {
                    val failure = RecorderBindingDispositionException(
                        "Persistence binding ${bindResult.installedBindingId.value} conflicts with " +
                            bindResult.requestedBindingId.value
                    )
                    repository.blockRecorderOwner(admission.ownerToken, failure, failure)
                    throw failure
                }
                is HeartRatePersistenceBindResult.Unresolved -> {
                    val failure = RecorderBindingDispositionException(
                        "Persistence binding disposition is unresolved for " +
                            bindResult.requestedBindingId.value
                    )
                    repository.blockRecorderOwner(admission.ownerToken, failure, failure)
                    throw failure
                }
            }
            return WorkoutSessionTimelineRecorder(
                repository,
                runtimeOwner,
                scope,
                admission,
                binding,
                startRequest
            ).also { created ->
                recorder = created
                while (pending.isNotEmpty()) created.acceptRuntimeObservation(pending.removeFirst())
            }
        }

        private fun validateStartRequest(request: WorkoutTimelineStartRequest) {
            require(request.session.id.isNotBlank()) { "Session ID must not be blank" }
            require(request.session.status == "active") { "Recorder start session must be active" }
            require(request.initialPhase.sessionId == request.session.id) {
                "Initial phase must belong to the session"
            }
            require(request.initialPhase.sequence == 0) {
                "Initial phase sequence must be zero"
            }
            require(request.initialPhase.openMarker == 1) {
                "Initial phase must be open"
            }
            val offset = requireNotNull(request.session.lastDurableOffsetMs) {
                "Recorder start requires a durable offset"
            }
            val mutation = requireNotNull(request.session.lastMutationSequence) {
                "Recorder start requires a mutation sequence"
            }
            require(request.initialPhase.startOffsetMs == offset) {
                "Initial phase offset must equal the session cut"
            }
            require(request.initialPhase.startMutationSequence == mutation) {
                "Initial phase mutation must equal the session cut"
            }
            request.recording?.let { recording ->
                require(recording.sessionId == request.session.id) {
                    "Recording must belong to the session"
                }
            }
        }
    }
}
