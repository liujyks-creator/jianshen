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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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

internal class WorkoutSessionTimelineRecorder private constructor(
    private val repository: WorkoutSessionRepository,
    private val runtimeOwner: HeartRateRuntimeOwner,
    private val scope: CoroutineScope,
    private val admission: RecorderAdmission,
    private val binding: HeartRatePersistenceBinding
) {
    private val mutableState = MutableStateFlow<WorkoutSessionTimelineRecorderState>(
        WorkoutSessionTimelineRecorderState.Prepared
    )
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val preStartObservations = mutableListOf(binding.snapshot)
    private var expectedReceipt = 1L
    private var latestCause = snapshotCause(binding.snapshot)
    private var activeSession: ActiveSession? = null
    private var terminalCache: TerminalCache? = null

    val state: StateFlow<WorkoutSessionTimelineRecorderState> = mutableState
    val bindingId: HeartRatePersistenceBindingId = admission.bindingId
    val ownerToken: RecorderOwnerToken = admission.ownerToken

    init {
        scope.launch {
            for (command in commands) {
                when (command) {
                    is Command.Observe -> handleObservation(command.observation)
                    is Command.Start -> handleStart(command)
                    is Command.EnableRecording -> handleEnableRecording(command)
                    is Command.SetRecordingExpected -> handleSetRecordingExpected(command)
                    is Command.Terminalize -> handleTerminal(command.request, command.reply)
                    is Command.OwnerClear -> handleOwnerClear(command.request)
                }
            }
        }
    }

    suspend fun start(request: WorkoutTimelineStartRequest): WorkoutTimelineStartResult {
        val reply = CompletableDeferred<WorkoutTimelineStartResult>()
        commands.send(Command.Start(request, reply))
        return reply.await()
    }

    fun acceptRuntimeObservation(observation: HeartRateRuntimeObservation) {
        commands.trySend(Command.Observe(observation)).getOrThrow()
    }

    suspend fun enableRecording(recording: HeartRateRecordingEntity, offsetMs: Long) {
        val reply = CompletableDeferred<Unit>()
        commands.send(Command.EnableRecording(recording, offsetMs, reply))
        reply.await()
    }

    suspend fun setRecordingExpected(
        recordingExpected: Boolean,
        userExclusionReason: String?,
        offsetMs: Long
    ) {
        val reply = CompletableDeferred<Unit>()
        commands.send(
            Command.SetRecordingExpected(
                recordingExpected,
                userExclusionReason,
                offsetMs,
                reply
            )
        )
        reply.await()
    }

    suspend fun terminalize(
        request: WorkoutTimelineTerminalRequest
    ): WorkoutTimelineTerminalResult {
        val reply = CompletableDeferred<WorkoutTimelineTerminalResult>()
        commands.send(Command.Terminalize(request, reply))
        return reply.await()
    }

    fun beginOwnerClearHandoff(
        request: WorkoutTimelineOwnerClearRequest
    ): RecorderOwnerClearResult {
        val transition = repository.beginRecorderOwnerClearHandoff(ownerToken)
        if (transition == RecorderOwnerClearResult.Pending) {
            scope.launch(Dispatchers.Default) {
                delay(1)
                commands.send(Command.OwnerClear(request))
            }
        }
        return transition
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
                repository.releaseRecorderOwner(ownerToken)
                mutableState.value = WorkoutSessionTimelineRecorderState.Released
            }
            is HeartRatePersistenceBindingDisposition.MatchingInstalled -> {
                when (val unbind = runtimeOwner.exactUnbindPersistenceSink(bindingId)) {
                    is HeartRatePersistenceUnbindResult.Unbound,
                    is HeartRatePersistenceUnbindResult.KnownAbsent -> {
                        repository.releaseRecorderOwner(ownerToken)
                        mutableState.value = WorkoutSessionTimelineRecorderState.Released
                    }
                    is HeartRatePersistenceUnbindResult.ConflictingInstalled -> {
                        val secondary = unbind.conflictFailure()
                        repository.blockRecorderOwner(ownerToken, primary, secondary)
                        mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(primary)
                    }
                    is HeartRatePersistenceUnbindResult.Unresolved -> {
                        val secondary = unbind.unresolvedFailure()
                        repository.blockRecorderOwner(ownerToken, primary, secondary)
                        mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(primary)
                    }
                }
            }
            is HeartRatePersistenceBindingDisposition.ConflictingInstalled -> {
                val secondary = disposition.conflictFailure()
                repository.blockRecorderOwner(ownerToken, primary, secondary)
                mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(primary)
            }
            is HeartRatePersistenceBindingDisposition.Unresolved -> {
                val secondary = disposition.unresolvedFailure()
                repository.blockRecorderOwner(ownerToken, primary, secondary)
                mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(primary)
            }
        }
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
        nextTuple: CanonicalTuple,
        cause: HeartRateRuntimeObservationCause
    ) {
        val nextSequence = Math.incrementExact(active.acquisitionSequence)
        val next = acquisition(
            recordingId = requireNotNull(active.recordingId),
            sequence = nextSequence,
            tuple = nextTuple,
            fact = CanonicalHeartRateObservationMapper.acquisition(
                cause,
                active.recordingExpected,
                active.userExclusionReason
            )
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
        active.devicePair = CanonicalHeartRateObservationMapper.mapCause(cause)
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
        val livePair = CanonicalHeartRateObservationMapper.mapCause(
            HeartRateRuntimeObservationCause.LIVE
        )
        if (active.devicePair == livePair) {
            repository.appendRecorderSample(ownerToken, active.expected(), nextTuple, sample)
        } else {
            val nextSequence = Math.incrementExact(active.acquisitionSequence)
            val next = acquisition(
                recordingId,
                nextSequence,
                nextTuple,
                CanonicalHeartRateObservationMapper.acquisition(
                    HeartRateRuntimeObservationCause.LIVE,
                    active.recordingExpected,
                    active.userExclusionReason
                )
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
            active.devicePair = livePair
        }
        active.tuple = nextTuple
        active.nextSampleSequence = Math.incrementExact(active.nextSampleSequence)
    }

    private suspend fun handleEnableRecording(command: Command.EnableRecording) {
        val active = activeSession
        if (mutableState.value != WorkoutSessionTimelineRecorderState.Started || active == null) {
            command.reply.completeExceptionally(IllegalStateException("Recorder is not started"))
            return
        }
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
            active.devicePair = CanonicalHeartRateObservationMapper.mapCause(latestCause)
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
        val active = activeSession
        if (
            mutableState.value != WorkoutSessionTimelineRecorderState.Started ||
            active?.recordingId == null
        ) {
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
        reply: CompletableDeferred<WorkoutTimelineTerminalResult>?
    ) {
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
        mutableState.value = WorkoutSessionTimelineRecorderState.Terminating
        try {
            val result = if (active.recordingId == null) {
                repository.finalizeRecorderWithoutRecording(
                    ownerToken,
                    NoRecordingFinalizationRequest(
                        sessionId = active.sessionId,
                        expectedStatus = "active",
                        expectedTuple = active.tuple,
                        finalOffsetMs = request.finalOffsetMs,
                        terminalStatus = request.terminalStatus,
                        terminalReason = request.terminalReason
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
                        finalOffsetMs = request.finalOffsetMs,
                        terminalStatus = request.terminalStatus,
                        terminalReason = request.terminalReason,
                        snapshotCreatedAt = request.snapshotCreatedAt
                    )
                )
                WorkoutTimelineTerminalResult(
                    finalized.sessionId,
                    finalized.finalTuple,
                    finalized.analysisVersion
                )
            }
            terminalCache = TerminalCache(request, active.tuple, result)
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
        when (val unbind = runtimeOwner.exactUnbindPersistenceSink(bindingId)) {
            is HeartRatePersistenceUnbindResult.Unbound,
            is HeartRatePersistenceUnbindResult.KnownAbsent -> Unit
            is HeartRatePersistenceUnbindResult.ConflictingInstalled -> {
                val failure = unbind.conflictFailure()
                repository.blockRecorderOwner(ownerToken, failure, failure)
                throw failure
            }
            is HeartRatePersistenceUnbindResult.Unresolved -> {
                val failure = unbind.unresolvedFailure()
                repository.blockRecorderOwner(ownerToken, failure, failure)
                throw failure
            }
        }
        if (repository.releaseRecorderOwner(ownerToken) != RecorderOwnerReleaseResult.Released) {
            throw IllegalStateException("Recorder owner release lost exact token identity")
        }
        activeSession?.tuple = result.finalTuple
        mutableState.value = WorkoutSessionTimelineRecorderState.Released
    }

    private fun failTerminal(
        reply: CompletableDeferred<WorkoutTimelineTerminalResult>?,
        failure: Throwable
    ) {
        mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(failure)
        reply?.completeExceptionally(failure)
    }

    private suspend fun handleOwnerClear(request: WorkoutTimelineOwnerClearRequest) {
        when (mutableState.value) {
            WorkoutSessionTimelineRecorderState.Prepared -> {
                val unbind = runtimeOwner.exactUnbindPersistenceSink(bindingId)
                if (
                    unbind is HeartRatePersistenceUnbindResult.Unbound ||
                    unbind is HeartRatePersistenceUnbindResult.KnownAbsent
                ) {
                    repository.releaseRecorderOwner(ownerToken)
                    mutableState.value = WorkoutSessionTimelineRecorderState.Released
                } else {
                    val failure = RecorderBindingDispositionException(
                        "Owner-clear could not prove exact runtime unbind"
                    )
                    repository.blockRecorderOwner(ownerToken, failure, failure)
                    mutableState.value = WorkoutSessionTimelineRecorderState.TerminalFailed(failure)
                }
            }
            WorkoutSessionTimelineRecorderState.Started,
            is WorkoutSessionTimelineRecorderState.ActivePersistenceFailed -> handleTerminal(
                WorkoutTimelineTerminalRequest(
                    terminalStatus = "abandoned",
                    terminalReason = "owner_cleared",
                    finalOffsetMs = request.finalOffsetMs,
                    snapshotCreatedAt = request.snapshotCreatedAt
                ),
                reply = null
            )
            WorkoutSessionTimelineRecorderState.Starting,
            WorkoutSessionTimelineRecorderState.Terminating,
            is WorkoutSessionTimelineRecorderState.TerminalFailed,
            WorkoutSessionTimelineRecorderState.Released -> Unit
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
            acquisitions += acquisition(
                recording.recordingId,
                acquisitionSequence,
                tuple,
                CanonicalHeartRateObservationMapper.acquisition(
                    currentCause,
                    recordingExpected = true,
                    userExclusionReason = null
                )
            )
            observations.drop(1).forEach { observation ->
                tuple = CanonicalTuple(
                    initialTuple.offsetMs,
                    Math.incrementExact(tuple.mutationSequence)
                )
                when (val payload = observation.payload) {
                    is HeartRateRuntimeObservationPayload.CurrentSnapshot -> {
                        currentCause = payload.cause
                        acquisitionSequence = Math.incrementExact(acquisitionSequence)
                        closeLastAcquisition(acquisitions, tuple)
                        acquisitions += acquisition(
                            recording.recordingId,
                            acquisitionSequence,
                            tuple,
                            CanonicalHeartRateObservationMapper.acquisition(
                                currentCause,
                                recordingExpected = true,
                                userExclusionReason = null
                            )
                        )
                    }
                    is HeartRateRuntimeObservationPayload.RuntimeTransition -> {
                        currentCause = payload.cause
                        acquisitionSequence = Math.incrementExact(acquisitionSequence)
                        closeLastAcquisition(acquisitions, tuple)
                        acquisitions += acquisition(
                            recording.recordingId,
                            acquisitionSequence,
                            tuple,
                            CanonicalHeartRateObservationMapper.acquisition(
                                currentCause,
                                recordingExpected = true,
                                userExclusionReason = null
                            )
                        )
                    }
                    is HeartRateRuntimeObservationPayload.ValidMeasurement -> {
                        currentCause = HeartRateRuntimeObservationCause.LIVE
                        val live = CanonicalHeartRateObservationMapper.mapCause(currentCause)
                        val previous = acquisitions.last()
                        if (
                            previous.deviceState != live.deviceState ||
                            previous.deviceReason != live.deviceReason
                        ) {
                            acquisitionSequence = Math.incrementExact(acquisitionSequence)
                            closeLastAcquisition(acquisitions, tuple)
                            acquisitions += acquisition(
                                recording.recordingId,
                                acquisitionSequence,
                                tuple,
                                CanonicalHeartRateObservationMapper.acquisition(
                                    currentCause,
                                    recordingExpected = true,
                                    userExclusionReason = null
                                )
                            )
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
                devicePair = CanonicalHeartRateObservationMapper.mapCause(currentCause)
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
        val openPhaseId: String,
        var tuple: CanonicalTuple,
        val startAnchorMs: Long,
        var recordingId: String?,
        var openAcquisitionId: String?,
        var acquisitionSequence: Int,
        var nextSampleSequence: Long,
        var recordingExpected: Boolean,
        var userExclusionReason: String?,
        var devicePair: CanonicalHeartRateDevicePair
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
        data class Terminalize(
            val request: WorkoutTimelineTerminalRequest,
            val reply: CompletableDeferred<WorkoutTimelineTerminalResult>
        ) : Command
        data class OwnerClear(val request: WorkoutTimelineOwnerClearRequest) : Command
    }

    companion object {
        suspend fun prepare(
            entryId: String,
            repository: WorkoutSessionRepository,
            runtimeOwner: HeartRateRuntimeOwner,
            scope: CoroutineScope
        ): WorkoutSessionTimelineRecorder {
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
                binding
            ).also { created ->
                recorder = created
                while (pending.isNotEmpty()) created.acceptRuntimeObservation(pending.removeFirst())
            }
        }
    }
}
