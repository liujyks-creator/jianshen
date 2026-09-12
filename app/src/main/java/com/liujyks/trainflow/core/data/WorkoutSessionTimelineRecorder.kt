package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.CanonicalValidationResult
import com.liujyks.trainflow.core.database.RecordingHeaderV1Validator
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.SessionStepRecordEntity
import com.liujyks.trainflow.core.database.entity.StrengthSetRecordEntity
import com.liujyks.trainflow.core.database.entity.TimedRestExtensionRecordEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.health.HeartRateBindingDisposition
import com.liujyks.trainflow.core.health.HeartRateObservation
import com.liujyks.trainflow.core.health.HeartRateObservationBinding
import com.liujyks.trainflow.core.health.HeartRateObservationPayload
import com.liujyks.trainflow.core.health.HeartRateRuntimeOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class RecorderPhaseInput(val phaseKind: String, val phaseIdentityJson: String)

internal sealed interface RecorderIntentInput {
    data class FirstEnable(val recording: HeartRateRecordingEntity) : RecorderIntentInput
    data class SetEnabled(val enabled: Boolean) : RecorderIntentInput
}

internal data class RecorderActivityInput(
    val elapsedRealtimeMs: Long,
    val nextPhase: RecorderPhaseInput? = null,
    val nextStatus: String? = null,
    val intent: RecorderIntentInput? = null,
    val nextDisplayMetadataJson: String? = null,
    val restExtension: TimedRestExtensionRecordEntity? = null
)

internal data class RecorderActivityConfirmation(
    val confirmedState: RecorderExpectedState,
    val openedPhase: WorkoutPhaseIntervalEntity?
)

internal enum class RecorderTerminalKind { COMPLETED, USER_ABANDONED }

internal class RecorderTerminalInput(
    val elapsedRealtimeMs: Long,
    val kind: RecorderTerminalKind,
    val endedAt: String,
    val totalElapsedSec: Int?,
    val effectiveElapsedSec: Int?,
    val pausedElapsedSec: Int?,
    val sessionDisplayMetadataJson: String,
    stepRecords: List<SessionStepRecordEntity>,
    restExtensions: List<TimedRestExtensionRecordEntity>,
    strengthSets: List<StrengthSetRecordEntity>,
    val snapshotCreatedAt: String
) {
    val stepRecords: List<SessionStepRecordEntity> = java.util.Collections.unmodifiableList(stepRecords.toList())
    val restExtensions: List<TimedRestExtensionRecordEntity> = java.util.Collections.unmodifiableList(restExtensions.toList())
    val strengthSets: List<StrengthSetRecordEntity> = java.util.Collections.unmodifiableList(strengthSets.toList())
}

internal class RecorderTerminalOperation(
    val saved: Deferred<CanonicalFinalizationResult>,
    val released: Deferred<Unit>
)

internal sealed interface RecorderTerminalSubmission {
    data class Accepted(val operation: RecorderTerminalOperation) : RecorderTerminalSubmission
    data class Closed(val originalCause: Throwable?) : RecorderTerminalSubmission
}

internal sealed interface RecorderSubmission {
    data class Accepted(val completion: Deferred<RecorderActivityConfirmation>) : RecorderSubmission
    data class Closed(val originalCause: Throwable?) : RecorderSubmission
}

internal data class RecorderProgress(
    val confirmedState: RecorderExpectedState?,
    val originalCause: Throwable?,
    val inputClosed: Boolean
)

/** One session owns this queue and its sole worker, inside the original session scope. */
internal class WorkoutSessionTimelineRecorder private constructor(
    private val repository: WorkoutSessionRepository,
    private val scope: CoroutineScope,
    private val session: WorkoutSessionEntity,
    private val initialPhase: WorkoutPhaseIntervalEntity,
    private val initialRecording: HeartRateRecordingEntity?,
    private val admission: RecorderAdmission,
    private val runtime: HeartRateRuntimeOwner
) {
    private sealed interface Input {
        data class Observation(val value: HeartRateObservation) : Input
        data class Activity(
            val value: RecorderActivityInput,
            val completion: CompletableDeferred<RecorderActivityConfirmation>
        ) : Input
        class Terminal(val value: RecorderTerminalInput) : Input {
            val saved = CompletableDeferred<CanonicalFinalizationResult>()
            val released = CompletableDeferred<Unit>()
            val submission = RecorderTerminalSubmission.Accepted(RecorderTerminalOperation(saved, released))
        }
    }

    private val lock = Any()
    private val sessionJob = requireNotNull(scope.coroutineContext[Job])
    private val queue = Channel<Input>(Channel.UNLIMITED)
    private val mutableProgress = MutableStateFlow(RecorderProgress(null, null, false))
    val progress: StateFlow<RecorderProgress> = mutableProgress.asStateFlow()
    private lateinit var binding: HeartRateObservationBinding
    private var startCompletion: CompletableDeferred<RecorderExpectedState>? = null
    private var startRequest: FrozenCanonicalStartRequest? = null
    private var stopCause: Throwable? = null
    private var terminal: Input.Terminal? = null
    private var clearRequested = false
    private var terminalCleanupClaimed = false

    // Only the worker advances these cursors. Confirmed state comes exclusively from S03.
    private lateinit var device: CanonicalHeartRateDeviceState
    private var recording = initialRecording
    private var enabled = initialRecording != null
    private var phaseSequence = 0
    private var acquisitionSequence = if (initialRecording == null) -1 else 0
    private var nextSampleSequence = 0L
    private var receipt = 0L

    fun freezeStart(): Deferred<RecorderExpectedState> = synchronized(lock) {
        startCompletion?.let { return@synchronized it }
        val completion = CompletableDeferred<RecorderExpectedState>()
        startCompletion = completion
        if (!accepting()) {
            completion.completeExceptionally(stopCause ?: CancellationException("recorder_input_closed"))
            return@synchronized completion
        }
        val prefix = buildList {
            while (true) {
                val input = queue.tryReceive().getOrNull() ?: break
                add((input as Input.Observation).value)
            }
        }
        val request = FrozenCanonicalStartRequest(session, initialPhase, binding, prefix, initialRecording)
        startRequest = request
        scope.launch(Dispatchers.IO) { consume(request, completion) }.invokeOnCompletion { cause ->
            if (cause is CancellationException) synchronized(lock) {
                closeInput(cause)
                completion.completeExceptionally(cause)
                terminal?.saved?.completeExceptionally(cause)
                terminal?.released?.completeExceptionally(cause)
            }
        }
        completion
    }

    fun offer(input: RecorderActivityInput): RecorderSubmission = synchronized(lock) {
        if (!accepting()) return@synchronized RecorderSubmission.Closed(mutableProgress.value.originalCause)
        check(startRequest != null) { "freezeStart must precede activity" }
        val completion = CompletableDeferred<RecorderActivityConfirmation>()
        queue.trySend(Input.Activity(input, completion)).getOrThrow()
        RecorderSubmission.Accepted(completion)
    }

    fun freezeTerminal(input: RecorderTerminalInput): RecorderTerminalSubmission = synchronized(lock) {
        terminal?.let { return@synchronized it.submission }
        if (!accepting()) return@synchronized RecorderTerminalSubmission.Closed(stopCause)
        check(startRequest != null) { "freezeStart must precede terminal" }
        val pending = Input.Terminal(input)
        terminal = pending
        queue.trySend(pending).getOrThrow()
        mutableProgress.value = mutableProgress.value.copy(inputClosed = true)
        pending.submission
    }

    fun clear() = synchronized(lock) {
        if (!clearRequested && !terminalCleanupClaimed) {
            clearRequested = true
            closeInput(CancellationException("recorder_owner_cleared"))
            repository.beginOwnerClearHandoff(admission.ownerToken, session.id)
        }
    }

    private fun observe(observation: HeartRateObservation) = synchronized(lock) {
        if (accepting()) queue.trySend(Input.Observation(observation)).getOrThrow()
    }

    /** Called under lock, including immediately before each repository call. */
    private fun accepting(): Boolean {
        return processing() && !mutableProgress.value.inputClosed
    }

    private fun processing(): Boolean {
        if (!sessionJob.isActive) {
            try {
                sessionJob.ensureActive()
            } catch (cause: CancellationException) {
                closeInput(cause)
            }
        }
        return stopCause == null
    }

    private fun closeInput(cause: Throwable) {
        if (stopCause == null || stopCause is CancellationException && cause !is CancellationException) {
            stopCause = cause
        }
        mutableProgress.value = mutableProgress.value.copy(
            inputClosed = true,
            originalCause = mutableProgress.value.originalCause ?: cause.takeUnless { it is CancellationException }
        )
        queue.close()
        while (true) {
            val pending = queue.tryReceive().getOrNull() ?: break
            if (pending is Input.Activity) pending.completion.completeExceptionally(requireNotNull(stopCause))
            if (pending is Input.Terminal) {
                pending.saved.completeExceptionally(requireNotNull(stopCause))
                pending.released.completeExceptionally(requireNotNull(stopCause))
            }
        }
    }

    private suspend fun consume(
        request: FrozenCanonicalStartRequest,
        initialization: CompletableDeferred<RecorderExpectedState>
    ) {
        var active: Input.Activity? = null
        try {
            currentCoroutineContext().ensureActive()
            synchronized(lock) { if (!processing()) throw requireNotNull(stopCause) }
            val confirmed = repository.startCanonicalSession(admission.ownerToken, request)
            currentCoroutineContext().ensureActive()
            device = CanonicalHeartRateObservationMapper.map(
                (request.binding.snapshot.payload as HeartRateObservationPayload.CurrentSnapshot).cause
            )
            for (observation in request.receipts) {
                receipt = observation.receipt
                when (val payload = observation.payload) {
                    is HeartRateObservationPayload.RuntimeTransition -> {
                        val next = CanonicalHeartRateObservationMapper.map(payload.cause)
                        if (next != device) {
                            device = next
                            if (recording != null) acquisitionSequence = Math.addExact(acquisitionSequence, 1)
                        }
                    }
                    is HeartRateObservationPayload.ValidMeasurement ->
                        if (recording != null) nextSampleSequence = Math.addExact(nextSampleSequence, 1)
                    is HeartRateObservationPayload.CurrentSnapshot -> error("snapshot_only_at_receipt_zero")
                }
            }
            synchronized(lock) {
                mutableProgress.value = mutableProgress.value.copy(confirmedState = confirmed)
                initialization.complete(confirmed)
            }
            for (input in queue) {
                active = input as? Input.Activity
                currentCoroutineContext().ensureActive()
                synchronized(lock) { if (!processing()) throw requireNotNull(stopCause) }
                val result = when (input) {
                    is Input.Observation -> consumeObservation(input.value)
                    is Input.Activity -> consumeActivity(input.value)
                    is Input.Terminal -> {
                        consumeTerminal(input)
                        return
                    }
                }
                active?.completion?.complete(result)
                active = null
            }
        } catch (cause: CancellationException) {
            synchronized(lock) {
                closeInput(cause)
                initialization.completeExceptionally(cause)
                active?.completion?.completeExceptionally(cause)
                terminal?.saved?.completeExceptionally(cause)
                terminal?.released?.completeExceptionally(cause)
            }
            throw cause
        } catch (cause: Throwable) {
            synchronized(lock) {
                closeInput(cause)
                repository.reportRecorderActivityFailure(admission.ownerToken, session.id, cause)
                initialization.completeExceptionally(cause)
                active?.completion?.completeExceptionally(cause)
                terminal?.saved?.completeExceptionally(cause)
                terminal?.released?.completeExceptionally(cause)
            }
        }
    }

    private suspend fun consumeTerminal(input: Input.Terminal) {
        val value = input.value
        val expected = requireNotNull(progress.value.confirmedState)
        val request = FrozenCanonicalFinalizationRequest(expected,
            Math.subtractExact(value.elapsedRealtimeMs, binding.anchorElapsedRealtimeMs),
            if (value.kind == RecorderTerminalKind.COMPLETED) "completed" else "abandoned",
            if (value.kind == RecorderTerminalKind.COMPLETED) "completed" else "user_abandoned",
            value.endedAt, value.totalElapsedSec, value.effectiveElapsedSec, value.pausedElapsedSec,
            value.sessionDisplayMetadataJson, value.stepRecords, value.restExtensions, value.strengthSets,
            value.snapshotCreatedAt.takeIf { expected.recordingId != null })
        currentCoroutineContext().ensureActive()
        synchronized(lock) { if (!processing()) throw requireNotNull(stopCause) }
        val saved = repository.finalizeCanonicalSession(admission.ownerToken, request)
        currentCoroutineContext().ensureActive()
        input.saved.complete(saved)
        synchronized(lock) { terminalCleanupClaimed = true }
        try {
            currentCoroutineContext().ensureActive()
            repository.releaseRecorderAfterTerminal(admission.ownerToken, session.id, runtime)
            synchronized(lock) {
                input.released.complete(Unit)
            }
        } catch (cause: Throwable) {
            synchronized(lock) {
                input.released.completeExceptionally(cause)
            }
            if (cause is CancellationException) throw cause
        }
    }

    private suspend fun consumeObservation(observation: HeartRateObservation): RecorderActivityConfirmation {
        if (observation.bindingId !== binding.bindingId || observation.receipt != Math.addExact(receipt, 1)) {
            throw RecorderValidationException("invalid_initialization_receipt_order")
        }
        receipt = observation.receipt
        val expected = requireNotNull(progress.value.confirmedState)
        val offset = Math.subtractExact(observation.elapsedRealtimeMs, binding.anchorElapsedRealtimeMs)
        val payload = observation.payload
        when (payload) {
            is HeartRateObservationPayload.CurrentSnapshot -> throw RecorderValidationException("snapshot_only_at_receipt_zero")
            is HeartRateObservationPayload.RuntimeTransition -> {
                val next = CanonicalHeartRateObservationMapper.map(payload.cause)
                if (device == next) return RecorderActivityConfirmation(expected, null)
                device = next
                if (recording == null) return RecorderActivityConfirmation(expected, null)
                val cut = CanonicalTuple(offset, Math.addExact(expected.durableTuple.mutationSequence, 1))
                acquisitionSequence = Math.addExact(acquisitionSequence, 1)
                return commit(CanonicalActivityRequest(expected, cut, nextAcquisition = acquisition(cut)))
            }
            is HeartRateObservationPayload.ValidMeasurement -> {
                val currentRecording = recording ?: return RecorderActivityConfirmation(expected, null)
                val cut = CanonicalTuple(offset, Math.addExact(expected.durableTuple.mutationSequence, 1))
                val sample = HeartRateSampleEntity(currentRecording.recordingId, nextSampleSequence,
                    offset, cut.mutationSequence, payload.bpm)
                nextSampleSequence = Math.addExact(nextSampleSequence, 1)
                return commit(CanonicalActivityRequest(expected, cut, sample = sample))
            }
        }
    }

    private suspend fun consumeActivity(input: RecorderActivityInput): RecorderActivityConfirmation {
        val expected = requireNotNull(progress.value.confirmedState)
        val offset = Math.subtractExact(input.elapsedRealtimeMs, binding.anchorElapsedRealtimeMs)
        val cut = CanonicalTuple(offset, Math.addExact(expected.durableTuple.mutationSequence, 1))
        var newRecording: HeartRateRecordingEntity? = null
        var nextAcquisition: HeartRateAcquisitionIntervalEntity? = null
        when (val intent = input.intent) {
            is RecorderIntentInput.FirstEnable -> {
                check(recording == null) { "recording parameters are already frozen" }
                newRecording = intent.recording.copy(startedOffsetMs = offset, startedMutationSequence = cut.mutationSequence)
                recording = newRecording
                enabled = true
                acquisitionSequence = 0
                nextAcquisition = acquisition(cut)
            }
            is RecorderIntentInput.SetEnabled -> {
                check(recording != null) { "FirstEnable must establish recording" }
                if (enabled != intent.enabled) {
                    enabled = intent.enabled
                    acquisitionSequence = Math.addExact(acquisitionSequence, 1)
                    nextAcquisition = acquisition(cut)
                }
            }
            null -> Unit
        }
        val phase = input.nextPhase?.let {
            phaseSequence = Math.addExact(phaseSequence, 1)
            WorkoutPhaseIntervalEntity("${session.id}:phase:$phaseSequence", session.id, phaseSequence,
                offset, null, cut.mutationSequence, null, 1, it.phaseKind, it.phaseIdentityJson)
        }
        if (phase == null && nextAcquisition == null && input.nextDisplayMetadataJson == null &&
            input.restExtension == null && (input.nextStatus == null || input.nextStatus == expected.status)) {
            return RecorderActivityConfirmation(expected, null)
        }
        return commit(CanonicalActivityRequest(expected, cut, input.nextStatus ?: expected.status,
            phase, nextAcquisition, newRecording, input.nextDisplayMetadataJson, restExtension = input.restExtension))
    }

    private fun acquisition(cut: CanonicalTuple): HeartRateAcquisitionIntervalEntity {
        val id = requireNotNull(recording).recordingId
        return HeartRateAcquisitionIntervalEntity("$id:acquisition:$acquisitionSequence", id, acquisitionSequence,
            cut.offsetMs, null, cut.mutationSequence, null, 1,
            if (enabled) "expected_recording" else "user_excluded", if (enabled) null else "user_turned_off",
            device.deviceState, device.deviceReason)
    }

    private suspend fun commit(request: CanonicalActivityRequest): RecorderActivityConfirmation {
        currentCoroutineContext().ensureActive()
        synchronized(lock) { if (!processing()) throw requireNotNull(stopCause) }
        val confirmed = repository.applyCanonicalActivity(admission.ownerToken, request)
        currentCoroutineContext().ensureActive()
        synchronized(lock) { mutableProgress.value = mutableProgress.value.copy(confirmedState = confirmed) }
        return RecorderActivityConfirmation(confirmed, request.nextPhase)
    }

    companion object {
        suspend fun admitAndBind(
            repository: WorkoutSessionRepository,
            runtime: HeartRateRuntimeOwner,
            scope: CoroutineScope,
            entryId: String,
            session: WorkoutSessionEntity,
            initialPhase: RecorderPhaseInput,
            initialRecording: HeartRateRecordingEntity?
        ): WorkoutSessionTimelineRecorder {
            val sessionJob = requireNotNull(scope.coroutineContext[Job])
            sessionJob.ensureActive()
            validateSessionTimeMetadata(session, requireCollected = true)
            val recording = initialRecording?.copy(startedOffsetMs = 0, startedMutationSequence = 0)
            if (recording != null) when (val result = RecordingHeaderV1Validator.validate(recording)) {
                CanonicalValidationResult.Valid -> Unit
                is CanonicalValidationResult.Invalid -> throw RecorderValidationException(result.code)
                is CanonicalValidationResult.UnsupportedVersion -> throw RecorderValidationException(
                    "unsupported_${result.contract}_version_${result.actualVersion}")
            }
            val phase = WorkoutPhaseIntervalEntity("${session.id}:phase:0", session.id, 0,
                0, null, 0, null, 1, initialPhase.phaseKind, initialPhase.phaseIdentityJson)
            val admission = repository.admitRecorder(entryId, session, phase)
            try {
                val recorder = WorkoutSessionTimelineRecorder(repository, scope, session, phase, recording, admission, runtime)
                withContext(Dispatchers.Main.immediate) {
                    sessionJob.ensureActive()
                    recorder.binding = when (val result = runtime.bindObservations(admission.bindingId, recorder::observe)) {
                        is HeartRateBindingDisposition.MatchingInstalled -> result.binding
                        is HeartRateBindingDisposition.ConflictingInstalled -> throw RecorderBindingConflictException()
                        HeartRateBindingDisposition.KnownAbsent -> throw RecorderCleanupUnresolvedException()
                    }
                }
                currentCoroutineContext().ensureActive()
                return recorder
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Throwable) {
                sessionJob.ensureActive()
                throw repository.releaseRecorderBeforeStart(admission.ownerToken, session.id, runtime, cause)
            }
        }
    }
}
