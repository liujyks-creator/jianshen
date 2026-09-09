package com.liujyks.trainflow.core.data

import androidx.room.withTransaction
import com.liujyks.trainflow.core.database.AnalysisSnapshotV1Validator
import com.liujyks.trainflow.core.database.CanonicalAnalysisV1
import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1Validator
import com.liujyks.trainflow.core.database.CanonicalSessionHeaderV1Result
import com.liujyks.trainflow.core.database.CanonicalSessionHeaderV1Validator
import com.liujyks.trainflow.core.database.CanonicalStorageJsonV1Validators
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.CanonicalValidationResult
import com.liujyks.trainflow.core.database.PhaseIdentityV1Validator
import com.liujyks.trainflow.core.database.SessionDisplayMetadataV1Validator
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.core.database.dao.CanonicalSessionGraphRows
import com.liujyks.trainflow.core.database.dao.WorkoutSessionWithRecords
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.SessionStepRecordEntity
import com.liujyks.trainflow.core.database.entity.StrengthSetRecordEntity
import com.liujyks.trainflow.core.database.entity.TimedRestExtensionRecordEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.health.HeartRateObservation
import com.liujyks.trainflow.core.health.HeartRateObservationBindingId
import com.liujyks.trainflow.core.health.HeartRateRuntimeOwner
import com.liujyks.trainflow.core.health.HeartRateBindingDisposition
import com.liujyks.trainflow.core.health.HeartRateUnbindDisposition
import com.liujyks.trainflow.core.health.HeartRateObservationBinding
import com.liujyks.trainflow.core.health.HeartRateObservationPayload
import com.liujyks.trainflow.core.model.ExerciseSide
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.SessionStepRecord
import com.liujyks.trainflow.core.model.SetEffort
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetRecord
import com.liujyks.trainflow.core.model.TimedCompositionCompatibilitySourceVersion
import com.liujyks.trainflow.core.model.TimedRestExtensionRecord
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class LegacySessionResidual(
    val sessionId: String,
    val status: String,
    val timelineStatus: String
)

internal data class ReconciledCanonicalSession(
    val sessionId: String,
    val expectedTuple: CanonicalTuple,
    val reconciledTuple: CanonicalTuple,
    val reconciliationContractVersion: Int
)

internal enum class RecorderFailureKind {
    INVALID_PARTIAL_CANONICAL_HEADER,
    UNKNOWN_VERSION,
    CORRUPT_JSON,
    INVALID_CANONICAL_HEADER,
    INVALID_CANONICAL_GRAPH
}

internal data class RecorderManualResolutionFailure(
    val sessionId: String,
    val kind: RecorderFailureKind,
    val code: String,
    val retryable: Boolean = false,
    val manualResolutionRequired: Boolean = true
)

internal sealed interface RecorderReconciliationResult {
    val legacyResiduals: List<LegacySessionResidual>

    data class Succeeded(
        override val legacyResiduals: List<LegacySessionResidual>,
        val reconciledSessions: List<ReconciledCanonicalSession>
    ) : RecorderReconciliationResult

    data class ManualResolutionRequired(
        override val legacyResiduals: List<LegacySessionResidual>,
        val failures: List<RecorderManualResolutionFailure>
    ) : RecorderReconciliationResult

}

internal class RecorderGuardedWriteException(
    val guard: String,
    val actualRowCount: Int
) : IllegalStateException("$guard expected rowCount=1 but was $actualRowCount")

internal class RecorderValidationException(
    val code: String
) : IllegalArgumentException(code)

internal class RecorderGateBlockedException(
    val result: RecorderReconciliationResult
) : IllegalStateException("Recorder gate is not ready")

internal data class RecorderExpectedState(
    val sessionId: String,
    val status: String,
    val durableTuple: CanonicalTuple,
    val openPhaseId: String,
    val recordingId: String? = null,
    val openAcquisitionId: String? = null
)

/** Values captured at the original Start cut; later receipts cannot extend this batch. */
internal class FrozenCanonicalStartRequest(
    val session: WorkoutSessionEntity,
    val initialPhase: WorkoutPhaseIntervalEntity,
    val binding: HeartRateObservationBinding,
    receipts: List<HeartRateObservation>,
    val recording: HeartRateRecordingEntity? = null,
    val heartRateEnabledAtStart: Boolean = recording != null
) {
    val receipts: List<HeartRateObservation> = java.util.Collections.unmodifiableList(receipts.toList())
}

internal data class CanonicalActivityRequest(
    val expected: RecorderExpectedState,
    val nextTuple: CanonicalTuple,
    val nextStatus: String = expected.status,
    val nextPhase: WorkoutPhaseIntervalEntity? = null,
    val nextAcquisition: HeartRateAcquisitionIntervalEntity? = null,
    val newRecording: HeartRateRecordingEntity? = null,
    val nextDisplayMetadataJson: String? = null,
    val sample: HeartRateSampleEntity? = null,
    val restExtension: TimedRestExtensionRecordEntity? = null
)

internal sealed interface CanonicalStartResolution {
    data class Committed(val confirmedState: RecorderExpectedState) : CanonicalStartResolution
    data object RolledBack : CanonicalStartResolution
    data object ConflictingGraph : CanonicalStartResolution
    data object Unresolved : CanonicalStartResolution
}

internal data class RecordingFinalizationRequest(
    val sessionId: String,
    val recordingId: String,
    val expectedStatus: String,
    val expectedTuple: CanonicalTuple,
    val finalOffsetMs: Long,
    val terminalStatus: String,
    val terminalReason: String,
    val snapshotCreatedAt: String
)

internal data class RecordingFinalizationResult(
    val sessionId: String,
    val recordingId: String,
    val finalTuple: CanonicalTuple,
    val analysisVersion: Int
)

/** Immutable values captured by the caller at the original terminal cut. */
internal class FrozenCanonicalFinalizationRequest(
    val expected: RecorderExpectedState,
    val finalOffsetMs: Long,
    val terminalStatus: String,
    val terminalReason: String,
    val endedAt: String?,
    val totalElapsedSec: Int?,
    val effectiveElapsedSec: Int?,
    val pausedElapsedSec: Int?,
    val sessionDisplayMetadataJson: String,
    stepRecords: List<SessionStepRecordEntity>,
    restExtensions: List<TimedRestExtensionRecordEntity>,
    strengthSets: List<StrengthSetRecordEntity>,
    val snapshotCreatedAt: String?
) {
    val stepRecords: List<SessionStepRecordEntity> = java.util.Collections.unmodifiableList(stepRecords.sortedBy { it.id })
    val restExtensions: List<TimedRestExtensionRecordEntity> = java.util.Collections.unmodifiableList(restExtensions.sortedBy { it.id })
    val strengthSets: List<StrengthSetRecordEntity> = java.util.Collections.unmodifiableList(strengthSets.sortedBy { it.id })
}

/** Durable only; admission, unbinding and RELEASED belong to the caller's release protocol. */
internal data class CanonicalFinalizationResult(
    val sessionId: String,
    val recordingId: String?,
    val finalTuple: CanonicalTuple,
    val analysisVersion: Int?
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class CanonicalFinalizationConflictException : IllegalStateException("conflicting_terminal_request"),
    kotlinx.coroutines.CopyableThrowable<CanonicalFinalizationConflictException> {
    // The Room body/return boundary uses the original rejection's identity.
    override fun createCopy(): CanonicalFinalizationConflictException? = null
}

internal class RecorderOwnerToken

internal data class RecorderAdmission(
    val entryId: String,
    val sessionId: String,
    val ownerToken: RecorderOwnerToken,
    val bindingId: HeartRateObservationBindingId
)

internal enum class RecorderOwnerDisposition { ACTIVE_OWNER, OWNER_CLEAR_PENDING, OWNER_BLOCKED }
internal class RecorderOwnerBusyException(val disposition: RecorderOwnerDisposition) :
    IllegalStateException("Recorder admission is $disposition")
internal class RecorderStaleOwnerException : IllegalStateException("stale_recorder_owner")
internal class RecorderCleanupUnresolvedException : IllegalStateException("recorder_cleanup_unresolved")
internal class RecorderBindingConflictException : IllegalStateException("recorder_binding_conflict")

internal class WorkoutSessionRepository(
    private val database: TrainFlowDatabase
) {
    private val dao = database.workoutSessionDao()
    private val canonicalDao = database.canonicalTimelineHeartRateDao()
    private val recorderGateMutex = Mutex()

    private val recorderStateLock = Any()
    private var recorderOwner: RecorderOwner? = null

    private enum class RecorderOperation { START, ACTIVITY, CANONICAL_TERMINAL, RECORDING_TERMINAL }
    private enum class RecorderStage { REQUEST, PERSISTED, MUTATION }

    private class RecorderOwner(val admission: RecorderAdmission) {
        var disposition = RecorderOwnerDisposition.ACTIVE_OWNER
        var clearRequested = false
        var primaryCause: Throwable? = null
        var inFlight: RecorderOperation? = null
        var stage = RecorderStage.REQUEST
        var mutated = false
        var startOperation: Job? = null
        var startCandidate: CanonicalSessionGraphV1? = null
        var startRollbackConfirmed = false
        var canonicalTerminalIntent: FrozenCanonicalFinalizationRequest? = null
        var recordingTerminalIntent: RecordingFinalizationRequest? = null
        var terminalConfirmed = false
        var cleanupAttempted = false
    }

    val sessions: Flow<List<WorkoutSession>> = dao.observeSessionsWithRecords()
        .map { rows -> rows.map { row -> row.toDomain() } }

    suspend fun admitRecorder(
        entryId: String, session: WorkoutSessionEntity, initialPhase: WorkoutPhaseIntervalEntity
    ): RecorderAdmission {
        val callerContext = currentCoroutineContext()
        if (entryId.isBlank()) throw RecorderValidationException("invalid_recorder_entry")
        validateSessionTimeMetadata(session)
        requireValidGraph(CanonicalSessionGraphV1(session, listOf(initialPhase)))
        return recorderGateMutex.withLock {
            synchronized(recorderStateLock) {
                recorderOwner?.let { owner ->
                    if (owner.admission.entryId == entryId) {
                        if (owner.admission.sessionId != session.id) throw RecorderValidationException("owner_session_mismatch")
                        if (owner.disposition == RecorderOwnerDisposition.ACTIVE_OWNER && !owner.clearRequested &&
                            owner.canonicalTerminalIntent == null && owner.recordingTerminalIntent == null) {
                            return@withLock owner.admission
                        }
                    }
                    throw RecorderOwnerBusyException(owner.disposition)
                }
            }
            when (val result = runRecorderReconciliation()) {
                is RecorderReconciliationResult.ManualResolutionRequired -> throw RecorderGateBlockedException(result)
                is RecorderReconciliationResult.Succeeded -> Unit
            }
            currentCoroutineContext().ensureActive()
            synchronized(recorderStateLock) {
                callerContext.ensureActive()
                val admission = RecorderAdmission(entryId, session.id, RecorderOwnerToken(), HeartRateObservationBindingId())
                recorderOwner = RecorderOwner(admission)
                admission
            }
        }
    }

    private fun matchingOwner(ownerToken: RecorderOwnerToken, sessionId: String): RecorderOwner =
        synchronized(recorderStateLock) {
            val owner = recorderOwner
            if (owner == null || owner.admission.ownerToken !== ownerToken) throw RecorderStaleOwnerException()
            if (owner.admission.sessionId != sessionId) throw RecorderValidationException("owner_session_mismatch")
            owner
        }

    private fun requireWritableOwner(ownerToken: RecorderOwnerToken, sessionId: String): RecorderOwner =
        synchronized(recorderStateLock) {
            val owner = matchingOwner(ownerToken, sessionId)
            if (owner.disposition != RecorderOwnerDisposition.ACTIVE_OWNER || owner.clearRequested ||
                owner.inFlight == RecorderOperation.START ||
                owner.canonicalTerminalIntent != null || owner.recordingTerminalIntent != null) {
                throw owner.primaryCause ?: RecorderOwnerBusyException(owner.disposition)
            }
            owner
        }

    private fun requireCanonicalOwner(ownerToken: RecorderOwnerToken,
        request: FrozenCanonicalFinalizationRequest): RecorderOwner = synchronized(recorderStateLock) {
        val owner = matchingOwner(ownerToken, request.expected.sessionId)
        val prior = owner.canonicalTerminalIntent
        if (prior != null) {
            if (!sameTerminalRequest(prior, request)) throw CanonicalFinalizationConflictException()
        } else {
            requireWritableOwner(ownerToken, request.expected.sessionId)
        }
        owner
    }

    private fun enterRecorderWrite(owner: RecorderOwner, operation: RecorderOperation) {
        synchronized(recorderStateLock) {
            requireWritableOwner(owner.admission.ownerToken, owner.admission.sessionId)
            if (operation == RecorderOperation.START && owner.startOperation != null) {
                throw RecorderValidationException("canonical_start_already_attempted")
            }
            check(owner.inFlight == null)
            owner.inFlight = operation
            owner.stage = RecorderStage.PERSISTED
            owner.mutated = false
        }
    }

    // Only the serialized original operation sets its error phase. A REQUEST rejection after
    // a mutation is still a persistence failure; nested CS-05 cannot erase outer S04 writes.
    private fun recorderStage(stage: RecorderStage) {
        synchronized(recorderStateLock) {
            recorderOwner?.takeIf { it.inFlight != null }?.let { owner ->
                owner.stage = stage
                if (stage == RecorderStage.MUTATION) owner.mutated = true
            }
        }
    }

    private fun blockRecorderOwner(owner: RecorderOwner, cause: Throwable): Throwable =
        synchronized(recorderStateLock) {
            val primary = owner.primaryCause ?: cause
            if (primary !== cause && primary.suppressed.none { it === cause }) primary.addSuppressed(cause)
            owner.primaryCause = primary
            owner.disposition = RecorderOwnerDisposition.OWNER_BLOCKED
            primary
        }

    private fun finishRecorderWrite(owner: RecorderOwner) {
        synchronized(recorderStateLock) {
            if (owner.clearRequested && !owner.terminalConfirmed) owner.disposition = RecorderOwnerDisposition.OWNER_BLOCKED
            owner.inFlight = null
        }
    }

    private fun failRecorderWrite(owner: RecorderOwner, cause: Throwable, bodyFailure: Throwable?): Throwable =
        synchronized(recorderStateLock) {
            val operation = owner.inFlight ?: return@synchronized (
                if (bodyFailure === cause) cause else blockRecorderOwner(owner, cause))
            val fatal = owner.mutated || owner.stage != RecorderStage.REQUEST || bodyFailure !== cause
            if (bodyFailure != null && bodyFailure !== cause) blockRecorderOwner(owner, bodyFailure)
            val result = if (operation == RecorderOperation.START) {
                if (!owner.clearRequested && owner.disposition != RecorderOwnerDisposition.OWNER_BLOCKED &&
                    bodyFailure === cause) {
                    owner.startRollbackConfirmed = true
                    owner.primaryCause = owner.primaryCause ?: cause
                    owner.disposition = RecorderOwnerDisposition.OWNER_CLEAR_PENDING
                    cause
                } else blockRecorderOwner(owner, cause)
            } else if (fatal || owner.disposition == RecorderOwnerDisposition.OWNER_BLOCKED) {
                blockRecorderOwner(owner, cause)
            } else {
                if (!owner.terminalConfirmed && !owner.clearRequested) {
                    owner.canonicalTerminalIntent = null
                    owner.recordingTerminalIntent = null
                }
                cause
            }
            finishRecorderWrite(owner)
            result
        }

    fun beginOwnerClearHandoff(ownerToken: RecorderOwnerToken, sessionId: String) {
        synchronized(recorderStateLock) {
            val owner = matchingOwner(ownerToken, sessionId)
            owner.clearRequested = true
            if (owner.disposition != RecorderOwnerDisposition.OWNER_BLOCKED) {
                owner.disposition = if (owner.startOperation != null && owner.inFlight == null && !owner.terminalConfirmed)
                    RecorderOwnerDisposition.OWNER_BLOCKED else RecorderOwnerDisposition.OWNER_CLEAR_PENDING
            }
        }
    }

    fun reportRecorderActivityFailure(ownerToken: RecorderOwnerToken, sessionId: String, cause: Throwable) {
        synchronized(recorderStateLock) { blockRecorderOwner(matchingOwner(ownerToken, sessionId), cause) }
    }

    suspend fun releaseRecorderBeforeStart(ownerToken: RecorderOwnerToken, sessionId: String,
        runtime: HeartRateRuntimeOwner, cause: Throwable): Throwable {
        val owner = synchronized(recorderStateLock) {
            val current = matchingOwner(ownerToken, sessionId)
            if (current.disposition == RecorderOwnerDisposition.OWNER_BLOCKED) {
                throw current.primaryCause ?: RecorderCleanupUnresolvedException()
            }
            current.primaryCause = current.primaryCause ?: cause
            if (current.cleanupAttempted || current.inFlight != null || current.terminalConfirmed ||
                current.canonicalTerminalIntent != null || current.recordingTerminalIntent != null ||
                (current.startOperation != null && (!current.startRollbackConfirmed || current.clearRequested))) {
                throw blockRecorderOwner(current, RecorderCleanupUnresolvedException())
            }
            current.cleanupAttempted = true
            current.disposition = RecorderOwnerDisposition.OWNER_CLEAR_PENDING
            current
        }
        try {
            recorderGateMutex.withLock {
                val absent = database.withTransaction {
                    val recordingId = owner.startCandidate?.recording?.recordingId
                    canonicalDao.canonicalGraphRows(sessionId) == null &&
                        startExecutionRowsAreEmpty(sessionId) &&
                        canonicalDao.phaseIntervals(sessionId).isEmpty() &&
                        canonicalDao.recordingsForSession(sessionId).isEmpty() &&
                        (recordingId == null || (canonicalDao.acquisitionsInSequence(recordingId).isEmpty() &&
                            canonicalDao.samplesInCanonicalOrder(recordingId).isEmpty() &&
                            canonicalDao.snapshotsInVersionOrder(recordingId).isEmpty()))
                }
                if (!absent) throw RecorderCleanupUnresolvedException()
                releaseObservationBinding(owner, runtime)
            }
        } catch (cleanup: Throwable) {
            throw blockRecorderOwner(owner, cleanup)
        }
        return requireNotNull(owner.primaryCause)
    }

    suspend fun releaseRecorderAfterTerminal(ownerToken: RecorderOwnerToken, sessionId: String,
        runtime: HeartRateRuntimeOwner) {
        val owner = synchronized(recorderStateLock) {
            val current = matchingOwner(ownerToken, sessionId)
            if (current.disposition == RecorderOwnerDisposition.OWNER_BLOCKED) {
                throw current.primaryCause ?: RecorderCleanupUnresolvedException()
            }
            if (!current.terminalConfirmed || current.cleanupAttempted || current.inFlight != null) {
                throw blockRecorderOwner(current, RecorderCleanupUnresolvedException())
            }
            current.cleanupAttempted = true
            current.disposition = RecorderOwnerDisposition.OWNER_CLEAR_PENDING
            current
        }
        try {
            recorderGateMutex.withLock { releaseObservationBinding(owner, runtime) }
        } catch (cleanup: Throwable) {
            throw blockRecorderOwner(owner, cleanup)
        }
    }

    private suspend fun releaseObservationBinding(owner: RecorderOwner, runtime: HeartRateRuntimeOwner) {
        withContext(Dispatchers.Main.immediate) {
            synchronized(recorderStateLock) {
                matchingOwner(owner.admission.ownerToken, owner.admission.sessionId)
                if (owner.disposition == RecorderOwnerDisposition.OWNER_BLOCKED) {
                    throw owner.primaryCause ?: RecorderCleanupUnresolvedException()
                }
                when (runtime.queryObservationBinding(owner.admission.bindingId)) {
                    HeartRateBindingDisposition.KnownAbsent -> Unit
                    is HeartRateBindingDisposition.ConflictingInstalled -> throw RecorderBindingConflictException()
                    is HeartRateBindingDisposition.MatchingInstalled ->
                        if (runtime.unbindObservations(owner.admission.bindingId) == HeartRateUnbindDisposition.CONFLICTING_INSTALLED) {
                            throw RecorderBindingConflictException()
                        }
                }
            }
        }
        currentCoroutineContext().ensureActive()
        synchronized(recorderStateLock) {
            matchingOwner(owner.admission.ownerToken, owner.admission.sessionId)
            if (owner.disposition == RecorderOwnerDisposition.OWNER_BLOCKED) {
                throw owner.primaryCause ?: RecorderCleanupUnresolvedException()
            }
            recorderOwner = null
        }
    }

    suspend fun finalizeCanonicalSession(ownerToken: RecorderOwnerToken, request: FrozenCanonicalFinalizationRequest): CanonicalFinalizationResult {
        val owner = requireCanonicalOwner(ownerToken, request)
        validateFinalizationRequest(request)
        val expected = request.expected
        val finalTuple = CanonicalTuple(request.finalOffsetMs,
            Math.addExact(expected.durableTuple.mutationSequence, 1))
        return recorderGateMutex.withLock {
            var readOnly = false
            var bodyFailure: Throwable? = null
            try {
                val result = database.withTransaction {
                    try {
                        synchronized(recorderStateLock) {
                            requireCanonicalOwner(ownerToken, request)
                            readOnly = owner.canonicalTerminalIntent != null
                            if (!readOnly) owner.canonicalTerminalIntent = request
                            check(owner.inFlight == null)
                            owner.inFlight = RecorderOperation.CANONICAL_TERMINAL
                            owner.stage = RecorderStage.PERSISTED
                            owner.mutated = false
                        }
                        val graph = loadCanonicalGraph(expected.sessionId) ?: run {
                            recorderStage(RecorderStage.REQUEST)
                            throw owner.primaryCause ?: CanonicalFinalizationConflictException()
                        }
                        validateSessionTimeMetadata(graph.session)
                        requireValidGraph(graph)
                        val steps = dao.stepRecordsForSession(expected.sessionId)
                        val extensions = dao.restExtensionRecordsForSession(expected.sessionId)
                        val sets = dao.strengthSetRecordsForSession(expected.sessionId)
                        recorderStage(RecorderStage.REQUEST)
                        if (graph.session.status in setOf("completed", "abandoned")) {
                            // D5 identity retains the predecessor sequence, not the overwritten old offset.
                            if (graph.session != request.terminalSession(graph.session, finalTuple) ||
                                graph.recording?.recordingId != expected.recordingId ||
                                graph.phases.last().id != expected.openPhaseId ||
                                graph.acquisitions.lastOrNull()?.id != expected.openAcquisitionId ||
                                steps != request.stepRecords || extensions != request.restExtensions || sets != request.strengthSets) {
                                throw CanonicalFinalizationConflictException()
                            }
                            graph.snapshots.singleOrNull()?.let { snapshot ->
                                recorderStage(RecorderStage.PERSISTED)
                                requireValidation(AnalysisSnapshotV1Validator.validate(graph, snapshot), "invalid_analysis_snapshot_v1")
                                recorderStage(RecorderStage.REQUEST)
                            }
                        } else {
                            if (readOnly) throw owner.primaryCause ?: CanonicalFinalizationConflictException()
                            validatedExpectedGraph(expected)
                            requireValidation(SessionDisplayMetadataV1Validator.validateTransition(
                                requireNotNull(graph.session.sessionDisplayMetadataJson), request.sessionDisplayMetadataJson,
                                terminal = false), "invalid_session_display_metadata_contract")
                            // Previously appended execution facts must remain exact; never REPLACE another row's identity.
                            if (!request.stepRecords.containsAll(steps) || !request.restExtensions.containsAll(extensions) ||
                                !request.strengthSets.containsAll(sets)) {
                                throw CanonicalFinalizationConflictException()
                            }
                            recorderStage(RecorderStage.MUTATION)
                            requireExactlyOne("write_canonical_execution_header", dao.writeCanonicalExecutionHeader(
                                expected.sessionId, expected.status, expected.durableTuple.offsetMs,
                                expected.durableTuple.mutationSequence, expected.openPhaseId, expected.recordingId,
                                expected.openAcquisitionId, request.endedAt, request.totalElapsedSec,
                                request.effectiveElapsedSec, request.pausedElapsedSec, request.sessionDisplayMetadataJson))
                            dao.insertCanonicalStepRecords(request.stepRecords - steps.toSet())
                            dao.insertCanonicalStrengthSetRecords(request.strengthSets - sets.toSet())
                            (request.restExtensions - extensions.toSet()).forEach {
                                requireInserted("insert_terminal_rest_extension", canonicalDao.insertRestExtension(it))
                            }
                            if (expected.recordingId != null) {
                                // Room inherits this outer transaction. CS-05 remains the only analysis producer.
                                finalizeRecordingTransaction(RecordingFinalizationRequest(
                                    expected.sessionId, expected.recordingId, expected.status, expected.durableTuple,
                                    request.finalOffsetMs, request.terminalStatus, request.terminalReason,
                                    requireNotNull(request.snapshotCreatedAt)))
                            } else {
                                advanceHeader(expected, finalTuple)
                                requireExactlyOne("close_terminal_phase_without_recording", canonicalDao.closeOpenPhase(
                                    expected.sessionId, expected.status, expected.openPhaseId,
                                    finalTuple.offsetMs, finalTuple.mutationSequence))
                                requireExactlyOne("finalize_session_without_recording", dao.finalizeSessionWithoutRecording(
                                    expected.sessionId, expected.status, expected.openPhaseId, finalTuple.offsetMs,
                                    finalTuple.mutationSequence, request.terminalStatus, request.terminalReason))
                            }
                            val persisted = loadCanonicalGraph(expected.sessionId)
                                ?: throw RecorderGuardedWriteException("terminal_post_write_session", 0)
                            validateSessionTimeMetadata(persisted.session)
                            requireValidGraph(persisted)
                            val terminalGraph = graph.copy(
                                session = request.terminalSession(graph.session, finalTuple),
                                phases = graph.phases.dropLast(1) + graph.phases.last().copy(
                                    endOffsetMs = finalTuple.offsetMs, endMutationSequence = finalTuple.mutationSequence, openMarker = null),
                                recording = graph.recording?.copy(status = "terminal", endedOffsetMs = finalTuple.offsetMs,
                                    endedMutationSequence = finalTuple.mutationSequence, originalAnalysisVersion = 1),
                                acquisitions = if (graph.recording == null) emptyList() else
                                    graph.acquisitions.dropLast(1) + graph.acquisitions.last().copy(
                                        endOffsetMs = finalTuple.offsetMs, endMutationSequence = finalTuple.mutationSequence, openMarker = null),
                                snapshots = persisted.snapshots
                            )
                            persisted.snapshots.singleOrNull()?.let { snapshot ->
                                requireValidation(AnalysisSnapshotV1Validator.validate(persisted, snapshot), "invalid_analysis_snapshot_v1")
                            }
                            if (persisted != terminalGraph || dao.stepRecordsForSession(expected.sessionId) != request.stepRecords ||
                                dao.restExtensionRecordsForSession(expected.sessionId) != request.restExtensions ||
                                dao.strengthSetRecordsForSession(expected.sessionId) != request.strengthSets ||
                                (expected.recordingId != null && persisted.snapshots.single().createdAt != request.snapshotCreatedAt)) {
                                throw RecorderValidationException("terminal_graph_changed_during_write")
                            }
                        }
                        CanonicalFinalizationResult(expected.sessionId, expected.recordingId, finalTuple,
                            if (expected.recordingId == null) null else 1)
                    } catch (cause: Throwable) {
                        bodyFailure = cause
                        throw cause
                    }
                }
                currentCoroutineContext().ensureActive()
                if (!readOnly) synchronized(recorderStateLock) { owner.terminalConfirmed = true }
                finishRecorderWrite(owner)
                result
            } catch (cause: Throwable) {
                throw failRecorderWrite(owner, cause, bodyFailure)
            }
        }
    }

    internal suspend fun finalizeRecordingSession(
        ownerToken: RecorderOwnerToken,
        request: RecordingFinalizationRequest
    ): RecordingFinalizationResult {
        val owner = requireWritableOwner(ownerToken, request.sessionId)
        return recorderGateMutex.withLock {
            var bodyFailure: Throwable? = null
            try {
                val result = database.withTransaction {
                    try {
                        enterRecorderWrite(owner, RecorderOperation.RECORDING_TERMINAL)
                        synchronized(recorderStateLock) { owner.recordingTerminalIntent = request }
                        finalizeRecordingTransaction(request)
                    } catch (cause: Throwable) {
                        bodyFailure = cause
                        throw cause
                    }
                }
                currentCoroutineContext().ensureActive()
                synchronized(recorderStateLock) { owner.terminalConfirmed = true }
                finishRecorderWrite(owner)
                result
            } catch (cause: Throwable) {
                throw failRecorderWrite(owner, cause, bodyFailure)
            }
        }
    }

    private suspend fun finalizeRecordingTransaction(
        request: RecordingFinalizationRequest
    ): RecordingFinalizationResult {
        recorderStage(RecorderStage.REQUEST)
        if (!validTerminalPair(request.terminalStatus, request.terminalReason)) {
            throw RecorderValidationException("invalid_terminal_status_reason_v1")
        }
        if (request.snapshotCreatedAt.isEmpty()) {
            throw RecorderValidationException("invalid_snapshot_created_at_v1")
        }
        if (request.finalOffsetMs < request.expectedTuple.offsetMs) {
            throw RecorderValidationException("invalid_final_tuple_v1")
        }
        return database.withTransaction {
            recorderStage(RecorderStage.PERSISTED)
            val graph = loadCanonicalGraph(request.sessionId) ?: run {
                recorderStage(RecorderStage.REQUEST)
                throw RecorderGuardedWriteException("finalization_expected_session", 0)
            }
            requireValidGraph(graph)
            val sessionTuple = CanonicalTuple(
                graph.session.lastDurableOffsetMs
                    ?: throw RecorderGuardedWriteException("finalization_expected_header", 0),
                graph.session.lastMutationSequence
                    ?: throw RecorderGuardedWriteException("finalization_expected_header", 0)
            )
            val recording = graph.recording
            val openPhase = graph.phases.singleOrNull { phase -> phase.openMarker == 1 }
            val openAcquisition = graph.acquisitions.singleOrNull { interval ->
                interval.openMarker == 1
            }
            recorderStage(RecorderStage.REQUEST)
            if (
                graph.session.id != request.sessionId ||
                graph.session.status != request.expectedStatus ||
                sessionTuple != request.expectedTuple ||
                recording?.recordingId != request.recordingId || recording.status != "active" ||
                openPhase == null || openAcquisition == null ||
                openAcquisition.recordingId != request.recordingId
            ) {
                throw RecorderGuardedWriteException("finalization_expected_state", 0)
            }

            val finalTuple = CanonicalTuple(
                offsetMs = request.finalOffsetMs,
                mutationSequence = Math.addExact(request.expectedTuple.mutationSequence, 1L)
            )
            val terminalSession = graph.session.copy(
                status = request.terminalStatus,
                lastDurableOffsetMs = finalTuple.offsetMs,
                lastMutationSequence = finalTuple.mutationSequence,
                trustedEndOffsetMs = finalTuple.offsetMs,
                terminalReason = request.terminalReason
            )
            val terminalPhases = graph.phases.dropLast(1) + openPhase.copy(
                endOffsetMs = finalTuple.offsetMs,
                endMutationSequence = finalTuple.mutationSequence,
                openMarker = null
            )
            val terminalAcquisitions = graph.acquisitions.dropLast(1) + openAcquisition.copy(
                endOffsetMs = finalTuple.offsetMs,
                endMutationSequence = finalTuple.mutationSequence,
                openMarker = null
            )
            val terminalRecording = recording.copy(
                status = "terminal",
                endedOffsetMs = finalTuple.offsetMs,
                endedMutationSequence = finalTuple.mutationSequence,
                originalAnalysisVersion = 1
            )
            val terminalGraphWithoutSnapshot = graph.copy(
                session = terminalSession,
                phases = terminalPhases,
                recording = terminalRecording,
                acquisitions = terminalAcquisitions
            )
            val snapshot = CanonicalAnalysisV1.derive(
                terminalGraphWithoutSnapshot,
                request.snapshotCreatedAt
            )
            val terminalGraph = terminalGraphWithoutSnapshot.copy(snapshots = listOf(snapshot))
            requireValidGraph(terminalGraph)
            requireValidation(
                AnalysisSnapshotV1Validator.validate(terminalGraph, snapshot),
                "invalid_analysis_snapshot_v1"
            )

            recorderStage(RecorderStage.MUTATION)
            requireExactlyOne(
                "finalize_close_open_phase",
                canonicalDao.finalizeCloseOpenPhase(
                    sessionId = request.sessionId,
                    recordingId = request.recordingId,
                    expectedStatus = request.expectedStatus,
                    expectedOffsetMs = request.expectedTuple.offsetMs,
                    expectedMutationSequence = request.expectedTuple.mutationSequence,
                    expectedOpenPhaseId = openPhase.id,
                    expectedOpenAcquisitionId = openAcquisition.id,
                    finalOffsetMs = finalTuple.offsetMs,
                    finalMutationSequence = finalTuple.mutationSequence
                )
            )
            requireExactlyOne(
                "finalize_close_open_acquisition",
                canonicalDao.finalizeCloseOpenAcquisition(
                    sessionId = request.sessionId,
                    recordingId = request.recordingId,
                    expectedStatus = request.expectedStatus,
                    expectedOffsetMs = request.expectedTuple.offsetMs,
                    expectedMutationSequence = request.expectedTuple.mutationSequence,
                    expectedClosedPhaseId = openPhase.id,
                    expectedOpenAcquisitionId = openAcquisition.id,
                    finalOffsetMs = finalTuple.offsetMs,
                    finalMutationSequence = finalTuple.mutationSequence
                )
            )
            requireExactlyOne(
                "finalize_terminalize_recording",
                canonicalDao.finalizeTerminalizeRecording(
                    sessionId = request.sessionId,
                    recordingId = request.recordingId,
                    expectedStatus = request.expectedStatus,
                    expectedOffsetMs = request.expectedTuple.offsetMs,
                    expectedMutationSequence = request.expectedTuple.mutationSequence,
                    expectedClosedPhaseId = openPhase.id,
                    expectedClosedAcquisitionId = openAcquisition.id,
                    finalOffsetMs = finalTuple.offsetMs,
                    finalMutationSequence = finalTuple.mutationSequence
                )
            )
            requireExactlyOne(
                "finalize_terminalize_session",
                dao.finalizeTerminalizeSession(
                    sessionId = request.sessionId,
                    recordingId = request.recordingId,
                    expectedStatus = request.expectedStatus,
                    expectedOffsetMs = request.expectedTuple.offsetMs,
                    expectedMutationSequence = request.expectedTuple.mutationSequence,
                    expectedClosedPhaseId = openPhase.id,
                    expectedClosedAcquisitionId = openAcquisition.id,
                    finalOffsetMs = finalTuple.offsetMs,
                    finalMutationSequence = finalTuple.mutationSequence,
                    terminalStatus = request.terminalStatus,
                    terminalReason = request.terminalReason
                )
            )
            canonicalDao.insertAnalysisSnapshot(snapshot)
            requireExactlyOne(
                "bind_original_analysis",
                canonicalDao.bindOriginalAnalysisV1(
                    sessionId = request.sessionId,
                    recordingId = request.recordingId,
                    finalOffsetMs = finalTuple.offsetMs,
                    finalMutationSequence = finalTuple.mutationSequence,
                    terminalStatus = request.terminalStatus,
                    terminalReason = request.terminalReason
                )
            )

            val persisted = loadCanonicalGraph(request.sessionId)
                ?: throw RecorderGuardedWriteException("finalization_post_write_session", 0)
            requireValidGraph(persisted)
            val persistedSnapshot = persisted.snapshots.singleOrNull()
                ?: throw RecorderGuardedWriteException("finalization_post_write_snapshot", 0)
            requireValidation(
                AnalysisSnapshotV1Validator.validate(persisted, persistedSnapshot),
                "invalid_analysis_snapshot_v1"
            )
            RecordingFinalizationResult(
                sessionId = request.sessionId,
                recordingId = request.recordingId,
                finalTuple = finalTuple,
                analysisVersion = 1
            )
        }
    }

    suspend fun prepareRecorder(): RecorderReconciliationResult = recorderGateMutex.withLock {
        synchronized(recorderStateLock) {
            recorderOwner?.let { throw RecorderOwnerBusyException(it.disposition) }
        }
        runRecorderReconciliation().also { currentCoroutineContext().ensureActive() }
    }

    suspend fun startCanonicalSession(
        ownerToken: RecorderOwnerToken,
        request: FrozenCanonicalStartRequest
    ): RecorderExpectedState {
        val owner = requireWritableOwner(ownerToken, request.session.id)
        if (request.binding.bindingId !== owner.admission.bindingId) throw RecorderValidationException("owner_binding_mismatch")
        val candidate = frozenStartGraph(request)
        val originalJob = requireNotNull(currentCoroutineContext()[Job])
        return recorderGateMutex.withLock {
            var bodyFailure: Throwable? = null
            try {
                database.withTransaction {
                    try {
                        enterRecorderWrite(owner, RecorderOperation.START)
                        synchronized(recorderStateLock) {
                            owner.startOperation = originalJob
                            owner.startCandidate = candidate
                        }
                        if (!startExecutionRowsAreEmpty(request.session.id)) {
                            throw RecorderValidationException("start_has_execution_rows")
                        }
                        recorderStage(RecorderStage.MUTATION)
                        requireInserted("insert_canonical_session", dao.insertSession(candidate.session))
                        candidate.phases.forEach { requireInserted("insert_initial_phase", canonicalDao.insertPhaseInterval(it)) }
                        candidate.recording?.let { requireInserted("insert_recording", canonicalDao.insertRecording(it)) }
                        candidate.acquisitions.forEach {
                            requireInserted("insert_initial_acquisition", canonicalDao.insertAcquisitionInterval(it))
                        }
                        candidate.samples.forEach { requireInserted("insert_initial_sample", canonicalDao.insertSample(it)) }
                        val persisted = requireNotNull(loadCanonicalGraph(request.session.id))
                        requireValidGraph(persisted)
                        if (persisted != candidate || !startExecutionRowsAreEmpty(request.session.id)) {
                            throw RecorderValidationException("start_graph_changed_during_write")
                        }
                    } catch (cause: Throwable) {
                        bodyFailure = cause
                        throw cause
                    }
                }
                currentCoroutineContext().ensureActive()
                finishRecorderWrite(owner)
                candidate.confirmedState()
            } catch (cause: Throwable) {
                throw failRecorderWrite(owner, cause, bodyFailure)
            }
        }
    }

    suspend fun resolveCanonicalStart(
        ownerToken: RecorderOwnerToken,
        request: FrozenCanonicalStartRequest,
        startOperation: Job
    ): CanonicalStartResolution {
        val owner = matchingOwner(ownerToken, request.session.id)
        if (request.binding.bindingId !== owner.admission.bindingId) throw RecorderValidationException("owner_binding_mismatch")
        val candidate = frozenStartGraph(request)
        synchronized(recorderStateLock) {
            if (owner.startOperation !== startOperation) throw RecorderValidationException("start_operation_mismatch")
            if (owner.startCandidate != candidate) throw RecorderValidationException("start_candidate_mismatch")
        }
        if (!startOperation.isCompleted) return CanonicalStartResolution.Unresolved
        return recorderGateMutex.withLock {
            matchingOwner(ownerToken, request.session.id)
            database.withTransaction {
            val rows = canonicalDao.canonicalGraphRows(request.session.id)
            val executionEmpty = startExecutionRowsAreEmpty(request.session.id)
            if (rows == null) {
                if (executionEmpty && canonicalDao.phaseIntervals(request.session.id).isEmpty() &&
                    canonicalDao.recordingsForSession(request.session.id).isEmpty()) {
                    CanonicalStartResolution.RolledBack
                } else CanonicalStartResolution.ConflictingGraph
            } else if (executionEmpty && rows.toCanonicalGraphOrNull() == candidate) {
                CanonicalStartResolution.Committed(candidate.confirmedState())
            } else CanonicalStartResolution.ConflictingGraph
            }
        }
    }

    private suspend fun startExecutionRowsAreEmpty(sessionId: String): Boolean =
        dao.stepRecordsForSession(sessionId).isEmpty() &&
            dao.restExtensionRecordsForSession(sessionId).isEmpty() &&
            dao.strengthSetRecordsForSession(sessionId).isEmpty()

    suspend fun applyCanonicalActivity(ownerToken: RecorderOwnerToken, request: CanonicalActivityRequest): RecorderExpectedState {
        val owner = requireWritableOwner(ownerToken, request.expected.sessionId)
        val expected = request.expected
        val cut = request.nextTuple
        return recorderGateMutex.withLock {
            var bodyFailure: Throwable? = null
            try {
                val result = database.withTransaction {
                    try {
                        enterRecorderWrite(owner, RecorderOperation.ACTIVITY)
                        val graph = validatedExpectedGraph(expected)
                        recorderStage(RecorderStage.PERSISTED)
                        val previousExtensions = dao.restExtensionRecordsForSession(expected.sessionId)
                        recorderStage(RecorderStage.REQUEST)
                        requireNextTuple(expected.durableTuple, cut)
                        if (cut.mutationSequence != Math.addExact(expected.durableTuple.mutationSequence, 1) ||
                            request.nextStatus !in setOf("active", "paused")) {
                            throw RecorderValidationException("invalid_activity_cut")
                        }
                        if (request.nextStatus != expected.status && request.nextPhase == null) {
                            throw RecorderValidationException("status_change_requires_phase_cut")
                        }
                        val phases = request.nextPhase?.let { next ->
                            if (CanonicalTuple(next.startOffsetMs, next.startMutationSequence) != cut) {
                                throw RecorderValidationException("phase_tuple_must_equal_next_input_cut")
                            }
                            graph.phases.dropLast(1) + graph.phases.last().copy(
                                endOffsetMs = cut.offsetMs, endMutationSequence = cut.mutationSequence, openMarker = null
                            ) + next
                        } ?: graph.phases
                        val recording = request.newRecording ?: graph.recording
                        if (request.newRecording != null && (graph.recording != null || request.nextAcquisition == null ||
                                CanonicalTuple(request.newRecording.startedOffsetMs,
                                    request.newRecording.startedMutationSequence) != cut)) {
                            throw RecorderGuardedWriteException("expected_first_recording_at_input_cut", 0)
                        }
                        val acquisitions = request.nextAcquisition?.let { next ->
                            if (CanonicalTuple(next.startOffsetMs, next.startMutationSequence) != cut) {
                                throw RecorderValidationException("acquisition_tuple_must_equal_next_input_cut")
                            }
                            if (request.newRecording != null) listOf(next) else {
                                if (graph.recording == null) throw RecorderGuardedWriteException("expected_recording", 0)
                                graph.acquisitions.dropLast(1) + graph.acquisitions.last().copy(
                                    endOffsetMs = cut.offsetMs, endMutationSequence = cut.mutationSequence, openMarker = null
                                ) + next
                            }
                        } ?: graph.acquisitions
                        request.nextDisplayMetadataJson?.let { nextJson ->
                            requireValidation(SessionDisplayMetadataV1Validator.validateTransition(
                                requireNotNull(graph.session.sessionDisplayMetadataJson), nextJson, terminal = false
                            ), "invalid_session_display_metadata_contract")
                        }
                        request.sample?.let { sample ->
                            if (CanonicalTuple(sample.offsetMs, sample.mutationSequence) != cut) {
                                throw RecorderValidationException("sample_tuple_must_equal_next_input_cut")
                            }
                        }
                        request.restExtension?.let { extension ->
                            if (graph.session.mode != "timed" || extension.sessionId != expected.sessionId ||
                                extension.id.isEmpty() || extension.stepId.isEmpty() || extension.restStageTitle.isEmpty() ||
                                extension.stepIndex < 0 || (extension.roundIndex != null && extension.roundIndex <= 0) ||
                                extension.addedSec <= 0 || extension.plannedRestSec <= 0 ||
                                extension.restElapsedBeforeExtensionSec < 0 || extension.extensionAtRemainingSec < 0 ||
                                extension.cumulativeExtraRestSec < extension.addedSec || extension.eventElapsedSec < 0) {
                                throw RecorderValidationException("invalid_timed_rest_extension")
                            }
                        }
                        val candidate = graph.copy(
                            session = graph.session.copy(status = request.nextStatus,
                                lastDurableOffsetMs = cut.offsetMs, lastMutationSequence = cut.mutationSequence,
                                sessionDisplayMetadataJson = request.nextDisplayMetadataJson ?: graph.session.sessionDisplayMetadataJson),
                            phases = phases, recording = recording, acquisitions = acquisitions,
                            samples = request.sample?.let { graph.samples + it } ?: graph.samples
                        )
                        requireValidGraph(candidate)
                        recorderStage(RecorderStage.MUTATION)
                        requireExactlyOne("advance_canonical_header", dao.advanceCanonicalHeader(
                            expected.sessionId, expected.status, expected.durableTuple.offsetMs,
                            expected.durableTuple.mutationSequence, expected.openPhaseId, expected.recordingId,
                            expected.openAcquisitionId, cut.offsetMs, cut.mutationSequence,
                            request.nextStatus, request.nextDisplayMetadataJson
                        ))
                        request.nextPhase?.let { next ->
                            requireExactlyOne("close_open_phase", canonicalDao.closeOpenPhase(
                                expected.sessionId, request.nextStatus, expected.openPhaseId, cut.offsetMs, cut.mutationSequence))
                            requireInserted("insert_next_phase", canonicalDao.insertPhaseInterval(next))
                        }
                        request.newRecording?.let { requireInserted("insert_recording", canonicalDao.insertRecording(it)) }
                        request.nextAcquisition?.let { next ->
                            if (request.newRecording == null) {
                                requireExactlyOne("close_open_acquisition", canonicalDao.closeOpenAcquisition(
                                    requireNotNull(expected.recordingId), request.nextStatus,
                                    requireNotNull(expected.openAcquisitionId), cut.offsetMs, cut.mutationSequence))
                            }
                            requireInserted("insert_next_acquisition", canonicalDao.insertAcquisitionInterval(next))
                        }
                        request.sample?.let { requireInserted("insert_sample", canonicalDao.insertSample(it)) }
                        request.restExtension?.let { requireInserted("insert_rest_extension", canonicalDao.insertRestExtension(it)) }
                        val persisted = requireNotNull(loadCanonicalGraph(expected.sessionId))
                        requireValidGraph(persisted)
                        val expectedExtensions = (previousExtensions + listOfNotNull(request.restExtension)).sortedBy { it.id }
                        if (persisted != candidate || dao.restExtensionRecordsForSession(expected.sessionId) != expectedExtensions) {
                            throw RecorderValidationException("activity_graph_changed_during_write")
                        }
                        candidate.confirmedState()
                    } catch (cause: Throwable) {
                        bodyFailure = cause
                        throw cause
                    }
                }
                currentCoroutineContext().ensureActive()
                finishRecorderWrite(owner)
                result
            } catch (cause: Throwable) {
                throw failRecorderWrite(owner, cause, bodyFailure)
            }
        }
    }

    suspend fun startCanonicalSession(
        ownerToken: RecorderOwnerToken,
        session: WorkoutSessionEntity,
        initialPhase: WorkoutPhaseIntervalEntity
    ): Unit {
        val owner = requireWritableOwner(ownerToken, session.id)
        validateSessionTimeMetadata(session)
        val candidate = CanonicalSessionGraphV1(
            session = session,
            phases = listOf(initialPhase)
        )
        requireValidGraph(candidate)
        val originalJob = requireNotNull(currentCoroutineContext()[Job])
        return recorderGateMutex.withLock {
            var bodyFailure: Throwable? = null
            try {
                database.withTransaction {
                    try {
                        enterRecorderWrite(owner, RecorderOperation.START)
                        synchronized(recorderStateLock) {
                            owner.startOperation = originalJob
                            owner.startCandidate = candidate
                        }
                        recorderStage(RecorderStage.MUTATION)
                        requireInserted("insert_canonical_session", dao.insertSession(session))
                        requireInserted("insert_initial_phase", canonicalDao.insertPhaseInterval(initialPhase))
                        requireValidGraph(requireNotNull(loadCanonicalGraph(session.id)))
                    } catch (cause: Throwable) {
                        bodyFailure = cause
                        throw cause
                    }
                }
                currentCoroutineContext().ensureActive()
                finishRecorderWrite(owner)
                Unit
            } catch (cause: Throwable) {
                throw failRecorderWrite(owner, cause, bodyFailure)
            }
        }
    }

    suspend fun appendSessionDisplayMetadata(
        ownerToken: RecorderOwnerToken,
        expected: RecorderExpectedState,
        nextTuple: CanonicalTuple,
        nextJson: String
    ): Unit {
        val owner = requireWritableOwner(ownerToken, expected.sessionId)
        return recorderGateMutex.withLock {
            var bodyFailure: Throwable? = null
            try {
                val result = database.withTransaction {
                    try {
                        enterRecorderWrite(owner, RecorderOperation.ACTIVITY)
                        val graph = validatedExpectedGraph(expected)
                        requireNextTuple(expected.durableTuple, nextTuple)
                        requireValidation(
                            SessionDisplayMetadataV1Validator.validateTransition(
                                previousJson = requireNotNull(graph.session.sessionDisplayMetadataJson),
                                nextJson = nextJson,
                                terminal = false
                            ),
                            "invalid_session_display_metadata_contract"
                        )
                        requireValidGraph(
                            graph.copy(
                                session = graph.session.copy(
                                    lastDurableOffsetMs = nextTuple.offsetMs,
                                    lastMutationSequence = nextTuple.mutationSequence,
                                    sessionDisplayMetadataJson = nextJson
                                )
                            )
                        )
                        recorderStage(RecorderStage.MUTATION)
                        val rowCount = dao.appendCanonicalDisplayMetadata(
                            sessionId = expected.sessionId,
                            expectedStatus = expected.status,
                            expectedOffsetMs = expected.durableTuple.offsetMs,
                            expectedMutationSequence = expected.durableTuple.mutationSequence,
                            expectedOpenPhaseId = expected.openPhaseId,
                            expectedRecordingId = expected.recordingId,
                            expectedOpenAcquisitionId = expected.openAcquisitionId,
                            nextOffsetMs = nextTuple.offsetMs,
                            nextMutationSequence = nextTuple.mutationSequence,
                            nextDisplayMetadataJson = nextJson
                        )
                        requireExactlyOne("append_display_metadata", rowCount)
                        requireValidGraph(requireNotNull(loadCanonicalGraph(expected.sessionId)))
                    } catch (cause: Throwable) {
                        bodyFailure = cause
                        throw cause
                    }
                }
                currentCoroutineContext().ensureActive()
                finishRecorderWrite(owner)
                result
            } catch (cause: Throwable) {
                throw failRecorderWrite(owner, cause, bodyFailure)
            }
        }
    }

    suspend fun transitionPhase(
        ownerToken: RecorderOwnerToken,
        expected: RecorderExpectedState,
        nextTuple: CanonicalTuple,
        nextPhase: WorkoutPhaseIntervalEntity
    ): Unit {
        val owner = requireWritableOwner(ownerToken, expected.sessionId)
        return recorderGateMutex.withLock {
            var bodyFailure: Throwable? = null
            try {
                val result = database.withTransaction {
                    try {
                        enterRecorderWrite(owner, RecorderOperation.ACTIVITY)
                        val graph = validatedExpectedGraph(expected)
                        requireNextTuple(expected.durableTuple, nextTuple)
                        val openPhase = graph.phases.single { phase -> phase.id == expected.openPhaseId }
                        val candidatePhases = graph.phases.dropLast(1) +
                            openPhase.copy(
                                endOffsetMs = nextTuple.offsetMs,
                                endMutationSequence = nextTuple.mutationSequence,
                                openMarker = null
                            ) + nextPhase
                        requireValidGraph(
                            graph.copy(
                                session = graph.session.copy(
                                    lastDurableOffsetMs = nextTuple.offsetMs,
                                    lastMutationSequence = nextTuple.mutationSequence
                                ),
                                phases = candidatePhases
                            )
                        )
                        recorderStage(RecorderStage.MUTATION)
                        advanceHeader(expected, nextTuple)
                        val closeRowCount = canonicalDao.closeOpenPhase(
                            sessionId = expected.sessionId,
                            expectedStatus = expected.status,
                            expectedOpenRowId = expected.openPhaseId,
                            endOffsetMs = nextTuple.offsetMs,
                            endMutationSequence = nextTuple.mutationSequence
                        )
                        requireExactlyOne("close_open_phase", closeRowCount)
                        requireInserted("insert_next_phase", canonicalDao.insertPhaseInterval(nextPhase))
                        requireValidGraph(requireNotNull(loadCanonicalGraph(expected.sessionId)))
                    } catch (cause: Throwable) {
                        bodyFailure = cause
                        throw cause
                    }
                }
                currentCoroutineContext().ensureActive()
                finishRecorderWrite(owner)
                result
            } catch (cause: Throwable) {
                throw failRecorderWrite(owner, cause, bodyFailure)
            }
        }
    }

    suspend fun startHeartRateRecording(
        ownerToken: RecorderOwnerToken,
        expected: RecorderExpectedState,
        nextTuple: CanonicalTuple,
        recording: HeartRateRecordingEntity,
        initialAcquisition: HeartRateAcquisitionIntervalEntity
    ): Unit {
        val owner = requireWritableOwner(ownerToken, expected.sessionId)
        return recorderGateMutex.withLock {
            var bodyFailure: Throwable? = null
            try {
                val result = database.withTransaction {
                    try {
                        enterRecorderWrite(owner, RecorderOperation.ACTIVITY)
                        val graph = validatedExpectedGraph(expected)
                        requireNextTuple(expected.durableTuple, nextTuple)
                        if (expected.recordingId != null || expected.openAcquisitionId != null) {
                            throw RecorderGuardedWriteException("expected_no_recording", 0)
                        }
                        requireValidGraph(
                            graph.copy(
                                session = graph.session.copy(
                                    lastDurableOffsetMs = nextTuple.offsetMs,
                                    lastMutationSequence = nextTuple.mutationSequence
                                ),
                                recording = recording,
                                acquisitions = listOf(initialAcquisition)
                            )
                        )
                        recorderStage(RecorderStage.MUTATION)
                        advanceHeader(expected, nextTuple)
                        requireInserted("insert_recording", canonicalDao.insertRecording(recording))
                        requireInserted(
                            "insert_initial_acquisition",
                            canonicalDao.insertAcquisitionInterval(initialAcquisition)
                        )
                        requireValidGraph(requireNotNull(loadCanonicalGraph(expected.sessionId)))
                    } catch (cause: Throwable) {
                        bodyFailure = cause
                        throw cause
                    }
                }
                currentCoroutineContext().ensureActive()
                finishRecorderWrite(owner)
                result
            } catch (cause: Throwable) {
                throw failRecorderWrite(owner, cause, bodyFailure)
            }
        }
    }

    suspend fun transitionAcquisition(
        ownerToken: RecorderOwnerToken,
        expected: RecorderExpectedState,
        nextTuple: CanonicalTuple,
        nextAcquisition: HeartRateAcquisitionIntervalEntity
    ): Unit {
        val owner = requireWritableOwner(ownerToken, expected.sessionId)
        return recorderGateMutex.withLock {
            var bodyFailure: Throwable? = null
            try {
                val result = database.withTransaction {
                    try {
                        enterRecorderWrite(owner, RecorderOperation.ACTIVITY)
                        val graph = validatedExpectedGraph(expected)
                        requireNextTuple(expected.durableTuple, nextTuple)
                        val openAcquisitionId = requireNotNull(expected.openAcquisitionId)
                        val recordingId = requireNotNull(expected.recordingId)
                        val openAcquisition = graph.acquisitions.single { acquisition ->
                            acquisition.id == openAcquisitionId
                        }
                        val candidateAcquisitions = graph.acquisitions.dropLast(1) +
                            openAcquisition.copy(
                                endOffsetMs = nextTuple.offsetMs,
                                endMutationSequence = nextTuple.mutationSequence,
                                openMarker = null
                            ) + nextAcquisition
                        requireValidGraph(
                            graph.copy(
                                session = graph.session.copy(
                                    lastDurableOffsetMs = nextTuple.offsetMs,
                                    lastMutationSequence = nextTuple.mutationSequence
                                ),
                                acquisitions = candidateAcquisitions
                            )
                        )
                        recorderStage(RecorderStage.MUTATION)
                        advanceHeader(expected, nextTuple)
                        val closeRowCount = canonicalDao.closeOpenAcquisition(
                            recordingId = recordingId,
                            expectedSessionStatus = expected.status,
                            expectedOpenRowId = openAcquisitionId,
                            endOffsetMs = nextTuple.offsetMs,
                            endMutationSequence = nextTuple.mutationSequence
                        )
                        requireExactlyOne("close_open_acquisition", closeRowCount)
                        requireInserted(
                            "insert_next_acquisition",
                            canonicalDao.insertAcquisitionInterval(nextAcquisition)
                        )
                        requireValidGraph(requireNotNull(loadCanonicalGraph(expected.sessionId)))
                    } catch (cause: Throwable) {
                        bodyFailure = cause
                        throw cause
                    }
                }
                currentCoroutineContext().ensureActive()
                finishRecorderWrite(owner)
                result
            } catch (cause: Throwable) {
                throw failRecorderWrite(owner, cause, bodyFailure)
            }
        }
    }

    suspend fun appendHeartRateSample(
        ownerToken: RecorderOwnerToken,
        expected: RecorderExpectedState,
        nextTuple: CanonicalTuple,
        sample: HeartRateSampleEntity
    ): Unit {
        val owner = requireWritableOwner(ownerToken, expected.sessionId)
        return recorderGateMutex.withLock {
            var bodyFailure: Throwable? = null
            try {
                val result = database.withTransaction {
                    try {
                        enterRecorderWrite(owner, RecorderOperation.ACTIVITY)
                        val graph = validatedExpectedGraph(expected)
                        requireNextTuple(expected.durableTuple, nextTuple)
                        if (CanonicalTuple(sample.offsetMs, sample.mutationSequence) != nextTuple) {
                            throw RecorderValidationException("sample_tuple_must_equal_next_input_cut")
                        }
                        requireValidGraph(
                            graph.copy(
                                session = graph.session.copy(
                                    lastDurableOffsetMs = nextTuple.offsetMs,
                                    lastMutationSequence = nextTuple.mutationSequence
                                ),
                                samples = graph.samples + sample
                            )
                        )
                        recorderStage(RecorderStage.MUTATION)
                        advanceHeader(expected, nextTuple)
                        requireInserted("insert_sample", canonicalDao.insertSample(sample))
                        requireValidGraph(requireNotNull(loadCanonicalGraph(expected.sessionId)))
                    } catch (cause: Throwable) {
                        bodyFailure = cause
                        throw cause
                    }
                }
                currentCoroutineContext().ensureActive()
                finishRecorderWrite(owner)
                result
            } catch (cause: Throwable) {
                throw failRecorderWrite(owner, cause, bodyFailure)
            }
        }
    }

    private suspend fun validatedExpectedGraph(
        expected: RecorderExpectedState
    ): CanonicalSessionGraphV1 {
        recorderStage(RecorderStage.PERSISTED)
        val graph = loadCanonicalGraph(expected.sessionId) ?: run {
            recorderStage(RecorderStage.REQUEST)
            throw RecorderGuardedWriteException("expected_session", 0)
        }
        validateSessionTimeMetadata(graph.session)
        requireValidGraph(graph)
        val sessionTuple = CanonicalTuple(
            graph.session.lastDurableOffsetMs
                ?: throw RecorderGuardedWriteException("expected_header", 0),
            graph.session.lastMutationSequence
                ?: throw RecorderGuardedWriteException("expected_header", 0)
        )
        val openPhase = graph.phases.singleOrNull { phase -> phase.openMarker == 1 }
        val recording = graph.recording
        val openAcquisition = graph.acquisitions.singleOrNull { acquisition ->
            acquisition.openMarker == 1
        }
        recorderStage(RecorderStage.REQUEST)
        if (
            graph.session.status != expected.status || sessionTuple != expected.durableTuple ||
            openPhase?.id != expected.openPhaseId || recording?.recordingId != expected.recordingId ||
            openAcquisition?.id != expected.openAcquisitionId
        ) {
            throw RecorderGuardedWriteException("expected_state", 0)
        }
        return graph
    }

    private suspend fun advanceHeader(
        expected: RecorderExpectedState,
        nextTuple: CanonicalTuple
    ) {
        val rowCount = dao.advanceCanonicalHeader(
            sessionId = expected.sessionId,
            expectedStatus = expected.status,
            expectedOffsetMs = expected.durableTuple.offsetMs,
            expectedMutationSequence = expected.durableTuple.mutationSequence,
            expectedOpenPhaseId = expected.openPhaseId,
            expectedRecordingId = expected.recordingId,
            expectedOpenAcquisitionId = expected.openAcquisitionId,
            nextOffsetMs = nextTuple.offsetMs,
            nextMutationSequence = nextTuple.mutationSequence
        )
        requireExactlyOne("advance_canonical_header", rowCount)
    }

    private suspend fun runRecorderReconciliation(): RecorderReconciliationResult =
        database.withTransaction {
            val residuals = mutableListOf<LegacySessionResidual>()
            val candidates = mutableListOf<CanonicalReconciliationCandidate>()
            val failures = mutableListOf<RecorderManualResolutionFailure>()

            dao.sessionsForRecorderGate().forEach { session ->
                when (val classification = classifyForRecorderGate(session)) {
                    is RecorderGateClassification.Legacy -> {
                        if (classification.header.status in LEGACY_NONTERMINAL_STATUSES) {
                            residuals += LegacySessionResidual(
                                sessionId = session.id,
                                status = classification.header.status,
                                timelineStatus = classification.header.timelineStatus
                            )
                        }
                    }

                    is RecorderGateClassification.CanonicalRunning ->
                        candidates += CanonicalReconciliationCandidate(
                            session = session,
                            durableTuple = classification.header.durableTuple,
                            graph = classification.graph
                        )

                    RecorderGateClassification.CanonicalTerminal -> Unit
                    is RecorderGateClassification.Failure -> failures += classification.failure
                }
            }

            if (failures.isNotEmpty()) {
                return@withTransaction RecorderReconciliationResult.ManualResolutionRequired(
                    legacyResiduals = residuals,
                    failures = failures
                )
            }

            val reconciled = candidates.map { candidate ->
                val recording = candidate.graph.recording
                if (recording == null) {
                    reconcileCanonicalCandidate(candidate)
                } else {
                    val finalized = finalizeRecordingTransaction(
                        RecordingFinalizationRequest(
                            sessionId = candidate.session.id,
                            recordingId = recording.recordingId,
                            expectedStatus = candidate.session.status,
                            expectedTuple = candidate.durableTuple,
                            finalOffsetMs = candidate.durableTuple.offsetMs,
                            terminalStatus = "abandoned",
                            terminalReason = "process_interrupted",
                            snapshotCreatedAt = Instant.now().toString()
                        )
                    )
                    ReconciledCanonicalSession(
                        sessionId = finalized.sessionId,
                        expectedTuple = candidate.durableTuple,
                        reconciledTuple = finalized.finalTuple,
                        reconciliationContractVersion = RECONCILIATION_CONTRACT_VERSION
                    )
                }
            }
            RecorderReconciliationResult.Succeeded(
                legacyResiduals = residuals,
                reconciledSessions = reconciled
            )
        }

    private suspend fun classifyForRecorderGate(
        session: WorkoutSessionEntity
    ): RecorderGateClassification {
        val header = CanonicalSessionHeaderV1Validator.validate(session)
        if (header is CanonicalSessionHeaderV1Result.Invalid) {
            return RecorderGateClassification.Failure(headerFailure(session, header.code))
        }
        val graph = loadCanonicalGraph(session.id)
            ?: return RecorderGateClassification.Failure(
                manualFailure(
                    session.id,
                    RecorderFailureKind.INVALID_CANONICAL_GRAPH,
                    "invalid_canonical_session_graph_v1"
                )
            )
        if (header !is CanonicalSessionHeaderV1Result.Legacy) {
            unsupportedPersistedVersionFailure(session.id, graph)?.let { failure ->
                return RecorderGateClassification.Failure(failure)
            }
        }
        val graphValidation = CanonicalSessionGraphV1Validator.validate(graph)
        if (graphValidation != CanonicalValidationResult.Valid) {
            val code = (graphValidation as? CanonicalValidationResult.Invalid)?.code
                ?: "invalid_canonical_session_graph_v1"
            return RecorderGateClassification.Failure(
                manualFailure(session.id, RecorderFailureKind.INVALID_CANONICAL_GRAPH, code)
            )
        }
        return when (header) {
            is CanonicalSessionHeaderV1Result.Legacy -> RecorderGateClassification.Legacy(header)
            is CanonicalSessionHeaderV1Result.CanonicalRunning ->
                RecorderGateClassification.CanonicalRunning(header, graph)

            is CanonicalSessionHeaderV1Result.CanonicalTerminal ->
                RecorderGateClassification.CanonicalTerminal

            is CanonicalSessionHeaderV1Result.Invalid -> error("Handled above")
        }
    }

    private fun headerFailure(
        session: WorkoutSessionEntity,
        validatorCode: String
    ): RecorderManualResolutionFailure {
        if (validatorCode == "invalid_partial_canonical_header") {
            return manualFailure(
                session.id,
                RecorderFailureKind.INVALID_PARTIAL_CANONICAL_HEADER,
                validatorCode
            )
        }
        if (session.timelineVersion != null && session.timelineVersion != RECONCILIATION_CONTRACT_VERSION) {
            return manualFailure(
                session.id,
                RecorderFailureKind.UNKNOWN_VERSION,
                "unsupported_timeline_version_${session.timelineVersion}"
            )
        }
        if (
            session.displayMetadataContractVersion != null &&
            session.displayMetadataContractVersion != DISPLAY_METADATA_CONTRACT_VERSION
        ) {
            return manualFailure(
                session.id,
                RecorderFailureKind.UNKNOWN_VERSION,
                "unsupported_display_metadata_version_${session.displayMetadataContractVersion}"
            )
        }
        val displayJson = session.sessionDisplayMetadataJson
        if (displayJson != null) {
            return when (
                val displayValidation =
                    CanonicalStorageJsonV1Validators.validateSessionDisplayMetadata(displayJson)
            ) {
                is CanonicalValidationResult.UnsupportedVersion -> manualFailure(
                    session.id,
                    RecorderFailureKind.UNKNOWN_VERSION,
                    "unsupported_${displayValidation.contract}_version_${displayValidation.actualVersion}"
                )

                is CanonicalValidationResult.Invalid -> manualFailure(
                    session.id,
                    RecorderFailureKind.CORRUPT_JSON,
                    displayValidation.code
                )

                CanonicalValidationResult.Valid -> manualFailure(
                    session.id,
                    RecorderFailureKind.INVALID_CANONICAL_HEADER,
                    validatorCode
                )
            }
        }
        return manualFailure(
            session.id,
            RecorderFailureKind.INVALID_CANONICAL_HEADER,
            validatorCode
        )
    }

    private fun unsupportedPersistedVersionFailure(
        sessionId: String,
        graph: CanonicalSessionGraphV1
    ): RecorderManualResolutionFailure? {
        val mode = WorkoutMode.entries.firstOrNull { value ->
            value.contractValue == graph.session.mode
        }
        if (mode != null) {
            when (val result = PlanSnapshotStorageV1Validator.validate(graph.session.planSnapshotJson, mode)) {
                is PlanSnapshotStorageV1ValidationResult.UnsupportedVersion ->
                    return unknownVersionFailure(
                        sessionId,
                        "plan_snapshot_storage",
                        result.actualVersion
                    )

                is PlanSnapshotStorageV1ValidationResult.Valid,
                is PlanSnapshotStorageV1ValidationResult.Invalid -> Unit
            }
        }
        unsupportedPlanSnapshotNestedVersion(graph.session.planSnapshotJson)?.let { version ->
            return unknownVersionFailure(sessionId, version.contract, version.actualVersion)
        }

        graph.phases.forEach { phase ->
            unsupportedVersion(
                PhaseIdentityV1Validator.validateStructure(
                    phase.phaseIdentityJson,
                    expectedPhaseKind = phase.phaseKind,
                    expectedMode = graph.session.mode
                )
            )?.let { version ->
                return unknownVersionFailure(sessionId, version.contract, version.actualVersion)
            }
            unsupportedPhaseIdentityNestedVersion(phase.phaseIdentityJson)?.let { version ->
                return unknownVersionFailure(sessionId, version.contract, version.actualVersion)
            }
        }

        graph.recording?.let { recording ->
            if (recording.sourceContractVersion != 1) {
                return unknownVersionFailure(
                    sessionId,
                    "recording_source",
                    recording.sourceContractVersion.toString()
                )
            }
            if (recording.acquisitionContractVersion != 1) {
                return unknownVersionFailure(
                    sessionId,
                    "acquisition",
                    recording.acquisitionContractVersion.toString()
                )
            }
            if (recording.parameterSnapshotVersion != 1) {
                return unknownVersionFailure(
                    sessionId,
                    "recording_parameter_snapshot",
                    recording.parameterSnapshotVersion.toString()
                )
            }
            recording.originalAnalysisVersion?.takeIf { version -> version != 1 }?.let { version ->
                return unknownVersionFailure(sessionId, "original_analysis", version.toString())
            }
            recording.zoneSnapshotJson?.let { json ->
                unsupportedVersion(CanonicalStorageJsonV1Validators.validateZoneSnapshot(json))
                    ?.let { version ->
                        return unknownVersionFailure(
                            sessionId,
                            version.contract,
                            version.actualVersion
                        )
                    }
            }
        }

        graph.snapshots.forEach { snapshot ->
            if (snapshot.analysisVersion != 1) {
                return unknownVersionFailure(
                    sessionId,
                    "analysis_snapshot",
                    snapshot.analysisVersion.toString()
                )
            }
            listOf(
                snapshot.analysisConfigJson to
                    CanonicalStorageJsonV1Validators::validateAnalysisConfig,
                snapshot.zoneDurationsJson to
                    CanonicalStorageJsonV1Validators::validateZoneDurations,
                snapshot.phaseAggregatesJson to
                    CanonicalStorageJsonV1Validators::validatePhaseAggregates,
                snapshot.durationBreakdownJson to
                    CanonicalStorageJsonV1Validators::validateDurationBreakdown,
                snapshot.qualityReasonsJson to
                    CanonicalStorageJsonV1Validators::validateQualityReasons
            ).forEach { (json, validator) ->
                if (json != null) {
                    unsupportedVersion(validator(json))?.let { version ->
                        return unknownVersionFailure(
                            sessionId,
                            version.contract,
                            version.actualVersion
                        )
                    }
                }
            }
            unsupportedAnalysisNestedVersion(snapshot)?.let { version ->
                return unknownVersionFailure(sessionId, version.contract, version.actualVersion)
            }
        }
        return null
    }

    private suspend fun reconcileCanonicalCandidate(
        candidate: CanonicalReconciliationCandidate
    ): ReconciledCanonicalSession {
        val session = candidate.session
        val tuple = candidate.durableTuple
        val reconciledTuple = CanonicalTuple(
            offsetMs = tuple.offsetMs,
            mutationSequence = Math.addExact(tuple.mutationSequence, 1L)
        )
        val sessionRowCount = dao.reconcileProcessInterrupted(
            sessionId = session.id,
            expectedStatus = session.status,
            expectedOffsetMs = tuple.offsetMs,
            expectedMutationSequence = tuple.mutationSequence,
            reconciledMutationSequence = reconciledTuple.mutationSequence,
            reconciliationContractVersion = RECONCILIATION_CONTRACT_VERSION
        )
        requireExactlyOne("reconcile_process_interrupted", sessionRowCount)

        val openPhase = candidate.graph.phases.single { phase -> phase.openMarker == 1 }
        val phaseRowCount = canonicalDao.closeOpenPhaseForProcessInterruption(
            sessionId = session.id,
            expectedOpenRowId = openPhase.id,
            endOffsetMs = reconciledTuple.offsetMs,
            endMutationSequence = reconciledTuple.mutationSequence,
            reconciliationContractVersion = RECONCILIATION_CONTRACT_VERSION
        )
        requireExactlyOne("close_process_interrupted_phase", phaseRowCount)

        val reconciledGraph = requireNotNull(loadCanonicalGraph(session.id))
        requireValidation(
            CanonicalSessionGraphV1Validator.validate(reconciledGraph),
            "invalid_canonical_session_graph_v1"
        )
        return ReconciledCanonicalSession(
            sessionId = session.id,
            expectedTuple = tuple,
            reconciledTuple = reconciledTuple,
            reconciliationContractVersion = RECONCILIATION_CONTRACT_VERSION
        )
    }

    private suspend fun loadCanonicalGraph(sessionId: String): CanonicalSessionGraphV1? {
        val rows = canonicalDao.canonicalGraphRows(sessionId) ?: return null
        return rows.toCanonicalGraphOrNull()
    }

    suspend fun upsertSession(session: WorkoutSession) {
        database.withTransaction {
            val entity = session.toEntity()
            val inserted = dao.insertSession(entity) != -1L
            if (!inserted) {
                val updated = dao.updateLegacySession(
                    id = entity.id,
                    planId = entity.planId,
                    mode = entity.mode,
                    status = entity.status,
                    planSnapshotJson = entity.planSnapshotJson,
                    startedAt = entity.startedAt,
                    endedAt = entity.endedAt,
                    totalElapsedSec = entity.totalElapsedSec,
                    effectiveElapsedSec = entity.effectiveElapsedSec,
                    pausedElapsedSec = entity.pausedElapsedSec
                )
                check(updated == 1) {
                    "Legacy workout-session write rejected for canonical session ${entity.id}"
                }
            }
            dao.deleteStepRecordsForSession(session.id)
            dao.deleteTimedRestExtensionRecordsForSession(session.id)
            dao.deleteStrengthSetRecordsForSession(session.id)
            dao.upsertStepRecords(session.stepHistory.map { record -> record.toEntity(session.id) })
            dao.upsertTimedRestExtensionRecords(
                session.timedRestExtensionRecords.map { record -> record.toEntity(session.id) }
            )
            dao.upsertStrengthSetRecords(session.strengthSetRecords.map { record -> record.toEntity(session.id) })
        }
    }

    suspend fun deleteAllSessions() {
        database.withTransaction {
            dao.deleteAllStepRecords()
            dao.deleteAllTimedRestExtensionRecords()
            dao.deleteAllStrengthSetRecords()
            dao.deleteAllSessions()
        }
    }

    suspend fun deleteSessionsForPlan(planId: String) {
        database.withTransaction {
            dao.deleteStepRecordsForPlan(planId)
            dao.deleteTimedRestExtensionRecordsForPlan(planId)
            dao.deleteStrengthSetRecordsForPlan(planId)
            dao.deleteSessionsForPlan(planId)
        }
    }

    suspend fun deleteSessionsStartedOnDate(date: String) {
        val dateKey = date.take(10)
        database.withTransaction {
            dao.deleteStepRecordsStartedOnDate(dateKey)
            dao.deleteTimedRestExtensionRecordsStartedOnDate(dateKey)
            dao.deleteStrengthSetRecordsStartedOnDate(dateKey)
            dao.deleteSessionsStartedOnDate(dateKey)
        }
    }

    suspend fun getSessions(): List<WorkoutSession> {
        return dao.getSessionsWithRecords().map { row -> row.toDomain() }
    }

    private companion object {
        const val RECONCILIATION_CONTRACT_VERSION = 1
        const val DISPLAY_METADATA_CONTRACT_VERSION = 1
        val LEGACY_NONTERMINAL_STATUSES = setOf("ready", "active", "paused")
    }
}

private fun sameTerminalRequest(first: FrozenCanonicalFinalizationRequest, next: FrozenCanonicalFinalizationRequest): Boolean =
    first.expected.copy(durableTuple = CanonicalTuple(0, first.expected.durableTuple.mutationSequence)) ==
        next.expected.copy(durableTuple = CanonicalTuple(0, next.expected.durableTuple.mutationSequence)) &&
        first.finalOffsetMs == next.finalOffsetMs && first.terminalStatus == next.terminalStatus &&
        first.terminalReason == next.terminalReason && first.endedAt == next.endedAt &&
        first.totalElapsedSec == next.totalElapsedSec && first.effectiveElapsedSec == next.effectiveElapsedSec &&
        first.pausedElapsedSec == next.pausedElapsedSec && first.sessionDisplayMetadataJson == next.sessionDisplayMetadataJson &&
        first.stepRecords == next.stepRecords && first.restExtensions == next.restExtensions && first.strengthSets == next.strengthSets

private fun FrozenCanonicalFinalizationRequest.terminalSession(
    original: WorkoutSessionEntity,
    finalTuple: CanonicalTuple
) = original.copy(
    status = terminalStatus, terminalReason = terminalReason,
    lastDurableOffsetMs = finalTuple.offsetMs, lastMutationSequence = finalTuple.mutationSequence,
    trustedEndOffsetMs = finalTuple.offsetMs, endedAt = endedAt,
    totalElapsedSec = totalElapsedSec, effectiveElapsedSec = effectiveElapsedSec,
    pausedElapsedSec = pausedElapsedSec, sessionDisplayMetadataJson = sessionDisplayMetadataJson
)

private fun validateFinalizationRequest(request: FrozenCanonicalFinalizationRequest) {
    val expected = request.expected
    if (!validTerminalPair(request.terminalStatus, request.terminalReason) ||
        request.terminalReason == "process_interrupted") {
        throw RecorderValidationException("invalid_terminal_status_reason_v1")
    }
    if (expected.status !in setOf("active", "paused") || expected.durableTuple.offsetMs < 0 ||
        expected.durableTuple.mutationSequence < 0 || request.finalOffsetMs < expected.durableTuple.offsetMs ||
        (expected.recordingId == null) != (expected.openAcquisitionId == null)) {
        throw RecorderValidationException("invalid_final_tuple_v1")
    }
    if (expected.recordingId != null && request.snapshotCreatedAt.isNullOrEmpty()) {
        throw RecorderValidationException("invalid_snapshot_created_at_v1")
    }
    request.endedAt?.let(Instant::parse)
    requireValidation(CanonicalStorageJsonV1Validators.validateSessionDisplayMetadata(
        request.sessionDisplayMetadataJson), "invalid_session_display_metadata_contract")
    if (listOfNotNull(request.totalElapsedSec, request.effectiveElapsedSec, request.pausedElapsedSec).any { it < 0 }) {
        throw RecorderValidationException("invalid_terminal_execution_values")
    }
    listOf(request.stepRecords.map { it.id to it.sessionId }, request.restExtensions.map { it.id to it.sessionId },
        request.strengthSets.map { it.id to it.sessionId }).forEach { keys ->
        if (keys.any { (id, sessionId) -> id.isEmpty() || sessionId != expected.sessionId } ||
            keys.map { it.first }.distinct().size != keys.size) {
            throw RecorderValidationException("invalid_terminal_execution_identity")
        }
    }
    request.stepRecords.forEach { step ->
        if (step.stepId.isEmpty() || SessionStepKind.entries.none { it.contractValue == step.kind } ||
            listOfNotNull(step.actualDurationSec, step.plannedDurationSec).any { it < 0 }) {
            throw RecorderValidationException("invalid_terminal_step_record")
        }
        Instant.parse(step.startedAt)
        step.endedAt?.let(Instant::parse)
    }
    request.restExtensions.forEach { extension ->
        if (extension.stepId.isEmpty() || extension.restStageTitle.isEmpty() || extension.stepIndex < 0 ||
            (extension.roundIndex != null && extension.roundIndex <= 0) || extension.addedSec <= 0 ||
            extension.plannedRestSec <= 0 || extension.restElapsedBeforeExtensionSec < 0 ||
            extension.extensionAtRemainingSec < 0 || extension.cumulativeExtraRestSec < extension.addedSec ||
            extension.eventElapsedSec < 0) {
            throw RecorderValidationException("invalid_timed_rest_extension")
        }
    }
    request.strengthSets.forEach { set ->
        if (set.exerciseId.isEmpty() || set.setOrder < 0 || StrengthSetKind.entries.none { it.contractValue == set.setKind } ||
            (set.side != null && ExerciseSide.entries.none { it.contractValue == set.side }) ||
            (set.effort != null && SetEffort.entries.none { it.contractValue == set.effort }) ||
            listOfNotNull(set.activeDurationSec, set.actualRestAfterSec).any { it < 0 }) {
            throw RecorderValidationException("invalid_terminal_strength_set")
        }
    }
}

private data class UnsupportedPersistedVersion(
    val contract: String,
    val actualVersion: String
)

private fun unknownVersionFailure(
    sessionId: String,
    contract: String,
    actualVersion: String
) = manualFailure(
    sessionId = sessionId,
    kind = RecorderFailureKind.UNKNOWN_VERSION,
    code = "unsupported_${contract}_version_$actualVersion"
)

private fun unsupportedVersion(
    result: CanonicalValidationResult
): UnsupportedPersistedVersion? = when (result) {
    is CanonicalValidationResult.UnsupportedVersion -> UnsupportedPersistedVersion(
        contract = result.contract,
        actualVersion = result.actualVersion
    )

    is CanonicalValidationResult.Invalid,
    CanonicalValidationResult.Valid -> null
}

private fun unsupportedPlanSnapshotNestedVersion(
    json: String
): UnsupportedPersistedVersion? {
    val root = parseCanonicalJson(json) as? CanonicalJsonValue.Obj ?: return null
    val blocks = (root.fields["blocks"] as? CanonicalJsonValue.Arr)?.values.orEmpty()
    blocks.forEach { value ->
        val block = value as? CanonicalJsonValue.Obj ?: return@forEach
        if (block.stringValue("kind") == "timed_composition") {
            block.integerValue("compositionVersion")
                ?.takeIf { version -> version != 2L }
                ?.let { version ->
                    return UnsupportedPersistedVersion(
                        "plan_snapshot_timed_composition",
                        version.toString()
                    )
                }
            block.unsupportedTimedCompositionCompatibilitySourceVersion()?.let { version ->
                return version
            }
        }
    }
    return null
}

private fun unsupportedPhaseIdentityNestedVersion(
    json: String
): UnsupportedPersistedVersion? {
    val root = parseCanonicalJson(json) as? CanonicalJsonValue.Obj ?: return null
    val family = root.stringValue("family")
    val acceptedPayloadVersion = when (family) {
        "timed_composition_v2" -> 2L
        "legacy_timed_v1", "strength_v1", "follow_along_v1" -> 1L
        else -> null
    }
    if (acceptedPayloadVersion != null) {
        root.integerValue("payloadVersion")
            ?.takeIf { version -> version != acceptedPayloadVersion }
            ?.let { version ->
                return UnsupportedPersistedVersion(
                    "phase_identity_payload",
                    version.toString()
                )
            }
    }
    root.objectValue("orderedStructureSignature")
        ?.integerValue("signatureContractVersion")
        ?.takeIf { version -> version != 1L }
        ?.let { version ->
            return UnsupportedPersistedVersion(
                "ordered_structure_signature",
                version.toString()
            )
        }
    if (family == "timed_composition_v2") {
        root.objectValue("payload")
            ?.integerValue("compositionVersion")
            ?.takeIf { version -> version != 2L }
            ?.let { version ->
                return UnsupportedPersistedVersion(
                    "phase_identity_timed_composition",
                    version.toString()
                )
            }
    }
    return null
}

private fun unsupportedAnalysisNestedVersion(
    snapshot: HeartRateAnalysisSnapshotEntity
): UnsupportedPersistedVersion? {
    val analysisConfig = parseCanonicalJson(snapshot.analysisConfigJson) as?
        CanonicalJsonValue.Obj
    if (analysisConfig != null) {
        listOf(
            "sampleIntervalContractVersion" to "sample_interval",
            "zoneAttributionContractVersion" to "zone_attribution",
            "statusProjectionContractVersion" to "status_projection",
            "durationPartitionContractVersion" to "duration_partition"
        ).forEach { (key, contract) ->
            analysisConfig.integerValue(key)
                ?.takeIf { version -> version != 1L }
                ?.let { version ->
                    return UnsupportedPersistedVersion(contract, version.toString())
                }
        }
    }
    val durationBreakdown = parseCanonicalJson(snapshot.durationBreakdownJson) as?
        CanonicalJsonValue.Obj
    durationBreakdown?.objectValue("orthogonalityContract")
        ?.integerValue("contractVersion")
        ?.takeIf { version -> version != 1L }
        ?.let { version ->
            return UnsupportedPersistedVersion(
                "duration_breakdown_orthogonality",
                version.toString()
            )
        }
    return null
}

private fun CanonicalJsonValue.Obj.unsupportedTimedCompositionCompatibilitySourceVersion():
    UnsupportedPersistedVersion? {
    objectValue("compatibility")?.unsupportedCompatibilitySourceVersion()?.let { version ->
        return version
    }
    val stageGroups = (fields["stageGroups"] as? CanonicalJsonValue.Arr)?.values.orEmpty()
    for (groupValue in stageGroups) {
        val group = groupValue as? CanonicalJsonValue.Obj ?: continue
        group.objectValue("compatibility")?.unsupportedCompatibilitySourceVersion()?.let { version ->
            return version
        }
        val targets = (group.fields["targets"] as? CanonicalJsonValue.Arr)?.values.orEmpty()
        for (targetValue in targets) {
            val target = targetValue as? CanonicalJsonValue.Obj ?: continue
            target.objectValue("compatibility")?.unsupportedCompatibilitySourceVersion()?.let { version ->
                return version
            }
        }
    }
    return null
}

private fun CanonicalJsonValue.Obj.unsupportedCompatibilitySourceVersion():
    UnsupportedPersistedVersion? {
    val sourceVersion = stringValue("sourceVersion") ?: return null
    if (TimedCompositionCompatibilitySourceVersion.entries.any { source ->
            source.contractValue == sourceVersion
        }
    ) {
        return null
    }
    return UnsupportedPersistedVersion("plan_snapshot_compatibility", sourceVersion)
}

private fun CanonicalJsonValue.Obj.integerValue(key: String): Long? =
    try {
        (fields[key] as? CanonicalJsonValue.Num)?.value?.longValueExact()
    } catch (_: ArithmeticException) {
        null
    }

private fun CanonicalJsonValue.Obj.stringValue(key: String): String? =
    (fields[key] as? CanonicalJsonValue.Str)?.value

private fun CanonicalJsonValue.Obj.objectValue(key: String): CanonicalJsonValue.Obj? =
    fields[key] as? CanonicalJsonValue.Obj

private data class CanonicalReconciliationCandidate(
    val session: WorkoutSessionEntity,
    val durableTuple: CanonicalTuple,
    val graph: CanonicalSessionGraphV1
)

private sealed interface RecorderGateClassification {
    data class Legacy(
        val header: CanonicalSessionHeaderV1Result.Legacy
    ) : RecorderGateClassification

    data class CanonicalRunning(
        val header: CanonicalSessionHeaderV1Result.CanonicalRunning,
        val graph: CanonicalSessionGraphV1
    ) : RecorderGateClassification

    data object CanonicalTerminal : RecorderGateClassification

    data class Failure(
        val failure: RecorderManualResolutionFailure
    ) : RecorderGateClassification
}

/** S03 write and S08B read boundary share this definition; old uncollected rows stay legal. */
internal fun validateSessionTimeMetadata(session: WorkoutSessionEntity, requireCollected: Boolean = false) {
    val values = listOf(session.startLocalDate, session.startZoneId,
        session.startUtcOffsetSeconds, session.timeMetadataSourceContractVersion)
    if (values.all { it == null } && !requireCollected) return
    if (values.any { it == null }) throw RecorderValidationException("incomplete_start_time_metadata")
    if (session.timeMetadataSourceContractVersion != 1L) {
        throw RecorderValidationException("unsupported_time_metadata_source_contract_version")
    }
    val instant = Instant.parse(requireNotNull(session.startedAt))
    val zone = ZoneId.of(requireNotNull(session.startZoneId))
    val date = LocalDate.parse(requireNotNull(session.startLocalDate))
    val zoned = instant.atZone(zone)
    if (date.toString() != session.startLocalDate || date != zoned.toLocalDate() ||
        zoned.offset.totalSeconds.toLong() != session.startUtcOffsetSeconds) {
        throw RecorderValidationException("start_time_metadata_source_mismatch")
    }
}

private fun CanonicalSessionGraphV1.confirmedState() = RecorderExpectedState(
    session.id, session.status,
    CanonicalTuple(requireNotNull(session.lastDurableOffsetMs), requireNotNull(session.lastMutationSequence)),
    phases.last().id, recording?.recordingId, acquisitions.lastOrNull()?.id
)

private fun frozenStartGraph(request: FrozenCanonicalStartRequest): CanonicalSessionGraphV1 {
    validateSessionTimeMetadata(request.session, requireCollected = true)
    val binding = request.binding
    val snapshot = binding.snapshot
    if (snapshot.bindingId !== binding.bindingId || snapshot.receipt != 0L ||
        snapshot.elapsedRealtimeMs != binding.anchorElapsedRealtimeMs ||
        snapshot.payload !is HeartRateObservationPayload.CurrentSnapshot) {
        throw RecorderValidationException("invalid_start_snapshot")
    }
    if (request.session.status != "active" || request.session.lastDurableOffsetMs != 0L ||
        request.session.lastMutationSequence != 0L || request.initialPhase.startMutationSequence != 0L ||
        request.session.endedAt != null || request.session.totalElapsedSec != null ||
        request.session.effectiveElapsedSec != null || request.session.pausedElapsedSec != null) {
        throw RecorderValidationException("invalid_start_cut")
    }
    val recording = request.recording
    if (request.heartRateEnabledAtStart != (recording != null)) {
        throw RecorderValidationException("initial_recording_intent_identity_mismatch")
    }
    if (recording != null && (recording.startedOffsetMs != 0L || recording.startedMutationSequence != 0L ||
            recording.sessionId != request.session.id)) {
        throw RecorderValidationException("invalid_initial_recording_cut")
    }
    var device = CanonicalHeartRateObservationMapper.map(snapshot.payload.cause)
    val acquisitions = mutableListOf<HeartRateAcquisitionIntervalEntity>()
    val samples = mutableListOf<HeartRateSampleEntity>()
    fun acquisition(sequence: Int, tuple: CanonicalTuple) = HeartRateAcquisitionIntervalEntity(
        id = "${requireNotNull(recording).recordingId}:acquisition:$sequence",
        recordingId = recording.recordingId, sequence = sequence,
        startOffsetMs = tuple.offsetMs, endOffsetMs = null,
        startMutationSequence = tuple.mutationSequence, endMutationSequence = null,
        openMarker = 1, recordingIntent = "expected_recording", intentReason = null,
        deviceState = device.deviceState, deviceReason = device.deviceReason
    )
    var tuple = CanonicalTuple(0, 0)
    if (recording != null) acquisitions += acquisition(0, tuple)
    var receipt = 0L
    var elapsed = binding.anchorElapsedRealtimeMs
    for (observation in request.receipts) {
        receipt = Math.addExact(receipt, 1)
        if (observation.bindingId !== binding.bindingId || observation.receipt != receipt ||
            observation.elapsedRealtimeMs < elapsed) {
            throw RecorderValidationException("invalid_initialization_receipt_order")
        }
        elapsed = observation.elapsedRealtimeMs
        val offset = Math.subtractExact(elapsed, binding.anchorElapsedRealtimeMs)
        when (val payload = observation.payload) {
            is HeartRateObservationPayload.CurrentSnapshot ->
                throw RecorderValidationException("snapshot_only_at_receipt_zero")
            is HeartRateObservationPayload.RuntimeTransition -> {
                val nextDevice = CanonicalHeartRateObservationMapper.map(payload.cause)
                if (device != nextDevice) {
                    device = nextDevice
                    if (recording != null) {
                        tuple = CanonicalTuple(offset, Math.addExact(tuple.mutationSequence, 1))
                        val previous = acquisitions.removeAt(acquisitions.lastIndex)
                        acquisitions += previous.copy(endOffsetMs = offset,
                            endMutationSequence = tuple.mutationSequence, openMarker = null)
                        acquisitions += acquisition(Math.addExact(previous.sequence, 1), tuple)
                    }
                }
            }
            is HeartRateObservationPayload.ValidMeasurement -> if (recording != null) {
                tuple = CanonicalTuple(offset, Math.addExact(tuple.mutationSequence, 1))
                samples += HeartRateSampleEntity(recording.recordingId, samples.size.toLong(),
                    offset, tuple.mutationSequence, payload.bpm)
            }
        }
    }
    return CanonicalSessionGraphV1(
        session = request.session.copy(lastDurableOffsetMs = tuple.offsetMs,
            lastMutationSequence = tuple.mutationSequence),
        phases = listOf(request.initialPhase), recording = recording,
        acquisitions = acquisitions, samples = samples
    ).also(::requireValidGraph)
}

private fun CanonicalSessionGraphRows.toCanonicalGraphOrNull(): CanonicalSessionGraphV1? {
    if (recordings.size > 1) return null
    val recordingRows = recordings.singleOrNull()
    return CanonicalSessionGraphV1(
        session = session,
        phases = phases,
        recording = recordingRows?.recording,
        acquisitions = recordingRows?.acquisitions.orEmpty(),
        samples = recordingRows?.samples.orEmpty(),
        snapshots = recordingRows?.snapshots.orEmpty()
    )
}

private fun manualFailure(
    sessionId: String,
    kind: RecorderFailureKind,
    code: String
) = RecorderManualResolutionFailure(
    sessionId = sessionId,
    kind = kind,
    code = code
)

private fun requireExactlyOne(guard: String, actualRowCount: Int) {
    if (actualRowCount != 1) throw RecorderGuardedWriteException(guard, actualRowCount)
}

private fun validTerminalPair(status: String, reason: String): Boolean = when (status) {
    "completed" -> reason == "completed"
    "abandoned" -> reason == "user_abandoned" || reason == "owner_cleared" ||
        reason == "process_interrupted"
    else -> false
}

private fun requireInserted(guard: String, insertedRowId: Long) {
    if (insertedRowId == -1L) throw RecorderGuardedWriteException(guard, 0)
}

private fun requireNextTuple(expected: CanonicalTuple, next: CanonicalTuple) {
    if (next <= expected || next.mutationSequence <= expected.mutationSequence) {
        throw RecorderValidationException("non_monotonic_canonical_tuple")
    }
}

private fun requireValidGraph(graph: CanonicalSessionGraphV1) {
    requireValidation(
        CanonicalSessionGraphV1Validator.validate(graph),
        "invalid_canonical_session_graph_v1"
    )
}

private fun requireValidation(result: CanonicalValidationResult, fallbackCode: String) {
    when (result) {
        CanonicalValidationResult.Valid -> Unit
        is CanonicalValidationResult.Invalid -> throw RecorderValidationException(result.code)
        is CanonicalValidationResult.UnsupportedVersion -> throw RecorderValidationException(
            "unsupported_${result.contract}_version_${result.actualVersion}"
        )
    }
}

private fun WorkoutSession.toEntity(): WorkoutSessionEntity {
    return WorkoutSessionEntity(
        id = id,
        planId = planId,
        mode = mode.contractValue,
        status = status.contractValue,
        planSnapshotJson = planSnapshot.toStorageJson(),
        startedAt = startedAt,
        endedAt = endedAt,
        startLocalDate = startLocalDate,
        startZoneId = startZoneId,
        startUtcOffsetSeconds = startUtcOffsetSeconds,
        timeMetadataSourceContractVersion = timeMetadataSourceContractVersion,
        totalElapsedSec = totalElapsedSec,
        effectiveElapsedSec = effectiveElapsedSec,
        pausedElapsedSec = pausedElapsedSec
    )
}

private fun SessionStepRecord.toEntity(sessionId: String): SessionStepRecordEntity {
    return SessionStepRecordEntity(
        id = "$sessionId:$stepId",
        sessionId = sessionId,
        stepId = stepId,
        kind = kind.contractValue,
        startedAt = startedAt,
        endedAt = endedAt,
        skipped = skipped,
        actualDurationSec = actualDurationSec
    )
}

private fun StrengthSetRecord.toEntity(sessionId: String): StrengthSetRecordEntity {
    return StrengthSetRecordEntity(
        id = "$sessionId:$id",
        sessionId = sessionId,
        exerciseId = exerciseId,
        sourceSetPlanId = sourceSetPlanId,
        setOrder = setOrder,
        setKind = setKind.contractValue,
        side = side?.contractValue,
        plannedJson = encodePlanned(plannedWeight, plannedRepTarget),
        actualJson = encodeActual(actualWeight, actualReps),
        activeDurationSec = activeDurationSec,
        actualRestAfterSec = actualRestAfterSec,
        effort = effort?.contractValue,
        substitutedFromExerciseId = substitutedFromExerciseId,
        notes = notes
    )
}

private fun TimedRestExtensionRecord.toEntity(sessionId: String): TimedRestExtensionRecordEntity {
    return TimedRestExtensionRecordEntity(
        id = "$sessionId:$id",
        sessionId = sessionId,
        stepId = stepId,
        stepIndex = stepIndex,
        roundIndex = roundIndex,
        restStageId = restStageId,
        restStageTitle = restStageTitle,
        previousStageId = previousStageId,
        previousStageTitle = previousStageTitle,
        addedSec = addedSec,
        plannedRestSec = plannedRestSec,
        restElapsedBeforeExtensionSec = restElapsedBeforeExtensionSec,
        extensionAtRemainingSec = extensionAtRemainingSec,
        cumulativeExtraRestSec = cumulativeExtraRestSec,
        eventElapsedSec = eventElapsedSec
    )
}

private fun WorkoutSessionWithRecords.toDomain(): WorkoutSession {
    val mode = workoutModeFrom(session.mode)
    return WorkoutSession(
        id = session.id,
        planId = session.planId,
        mode = mode,
        planSnapshot = session.planSnapshotJson.toPlanSnapshot(fallbackMode = mode),
        status = sessionStatusFrom(session.status),
        startedAt = session.startedAt,
        endedAt = session.endedAt,
        startLocalDate = session.startLocalDate,
        startZoneId = session.startZoneId,
        startUtcOffsetSeconds = session.startUtcOffsetSeconds,
        timeMetadataSourceContractVersion = session.timeMetadataSourceContractVersion,
        totalElapsedSec = session.totalElapsedSec,
        effectiveElapsedSec = session.effectiveElapsedSec,
        pausedElapsedSec = session.pausedElapsedSec,
        stepHistory = stepRecords.sortedBy { record -> record.startedAt }.map { record -> record.toDomain() },
        timedRestExtensionRecords = timedRestExtensionRecords
            .sortedWith(compareBy<TimedRestExtensionRecordEntity> { record -> record.eventElapsedSec }
                .thenBy { record -> record.stepIndex }
                .thenBy { record -> record.cumulativeExtraRestSec }
                .thenBy { record -> record.id })
            .map { record -> record.toDomain() },
        strengthSetRecords = strengthSetRecords.sortedBy { record -> record.setOrder }.map { record -> record.toDomain() }
    )
}

private fun SessionStepRecordEntity.toDomain(): SessionStepRecord {
    return SessionStepRecord(
        stepId = stepId.ifBlank { id.substringAfter(':', id) },
        kind = sessionStepKindFrom(kind),
        startedAt = startedAt,
        endedAt = endedAt,
        skipped = skipped,
        actualDurationSec = actualDurationSec
    )
}

private fun StrengthSetRecordEntity.toDomain(): StrengthSetRecord {
    val planned = plannedJson.decodePlanned()
    val actual = actualJson.decodeActual()
    return StrengthSetRecord(
        id = id.substringAfter(':', id),
        exerciseId = exerciseId,
        sourceSetPlanId = sourceSetPlanId,
        setOrder = setOrder,
        setKind = strengthSetKindFrom(setKind),
        side = side?.let(::exerciseSideFrom),
        plannedWeight = planned.weight,
        plannedRepTarget = planned.repTarget,
        actualWeight = actual.weight,
        actualReps = actual.reps,
        activeDurationSec = activeDurationSec,
        actualRestAfterSec = actualRestAfterSec,
        effort = effort?.let(::setEffortFrom),
        substitutedFromExerciseId = substitutedFromExerciseId,
        notes = notes
    )
}

private fun TimedRestExtensionRecordEntity.toDomain(): TimedRestExtensionRecord {
    return TimedRestExtensionRecord(
        id = id.substringAfter(':', id),
        stepId = stepId,
        stepIndex = stepIndex,
        roundIndex = roundIndex,
        restStageId = restStageId,
        restStageTitle = restStageTitle,
        previousStageId = previousStageId,
        previousStageTitle = previousStageTitle,
        addedSec = addedSec,
        plannedRestSec = plannedRestSec,
        restElapsedBeforeExtensionSec = restElapsedBeforeExtensionSec,
        extensionAtRemainingSec = extensionAtRemainingSec,
        cumulativeExtraRestSec = cumulativeExtraRestSec,
        eventElapsedSec = eventElapsedSec
    )
}

private data class PlannedSetStorage(
    val weight: WeightValue?,
    val repTarget: RepTarget?
)

private data class ActualSetStorage(
    val weight: WeightValue?,
    val reps: Int?
)

private fun encodePlanned(
    weight: WeightValue?,
    repTarget: RepTarget?
): String? {
    val fields = buildList {
        weight?.let { add("weight=${it.value},${it.unit.contractValue}") }
        when (repTarget) {
            is RepTarget.Fixed -> add("rep=fixed,${repTarget.reps}")
            is RepTarget.Range -> add("rep=range,${repTarget.minReps},${repTarget.maxReps}")
            null -> Unit
        }
    }
    return fields.takeIf { it.isNotEmpty() }?.joinToString("|")
}

private fun encodeActual(
    weight: WeightValue?,
    reps: Int?
): String? {
    val fields = buildList {
        weight?.let { add("weight=${it.value},${it.unit.contractValue}") }
        reps?.let { add("reps=$it") }
    }
    return fields.takeIf { it.isNotEmpty() }?.joinToString("|")
}

private fun String?.decodePlanned(): PlannedSetStorage {
    val fields = toFields()
    return PlannedSetStorage(
        weight = fields["weight"]?.toWeightValue(),
        repTarget = fields["rep"]?.toRepTarget()
    )
}

private fun String?.decodeActual(): ActualSetStorage {
    val fields = toFields()
    return ActualSetStorage(
        weight = fields["weight"]?.toWeightValue(),
        reps = fields["reps"]?.toIntOrNull()
    )
}

private fun String?.toFields(): Map<String, String> {
    return this
        ?.split("|")
        ?.mapNotNull { field ->
            val key = field.substringBefore("=", missingDelimiterValue = "")
            val value = field.substringAfter("=", missingDelimiterValue = "")
            if (key.isBlank()) null else key to value
        }
        ?.toMap()
        ?: emptyMap()
}

private fun String.toWeightValue(): WeightValue? {
    val parts = split(",")
    val value = parts.getOrNull(0)?.toDoubleOrNull() ?: return null
    val unit = parts.getOrNull(1)?.let(::weightUnitFrom) ?: return null
    return WeightValue(value = value, unit = unit)
}

private fun String.toRepTarget(): RepTarget? {
    val parts = split(",")
    return when (parts.getOrNull(0)) {
        "fixed" -> parts.getOrNull(1)?.toIntOrNull()?.let { reps -> RepTarget.Fixed(reps) }
        "range" -> {
            val min = parts.getOrNull(1)?.toIntOrNull()
            val max = parts.getOrNull(2)?.toIntOrNull()
            if (min != null && max != null) RepTarget.Range(min, max) else null
        }
        else -> null
    }
}

private fun workoutModeFrom(value: String): WorkoutMode {
    return WorkoutMode.entries.firstOrNull { mode -> mode.contractValue == value } ?: WorkoutMode.TIMED
}

private fun sessionStatusFrom(value: String): SessionStatus {
    return SessionStatus.entries.firstOrNull { status -> status.contractValue == value } ?: SessionStatus.COMPLETED
}

private fun sessionStepKindFrom(value: String): SessionStepKind {
    return SessionStepKind.entries.firstOrNull { kind -> kind.contractValue == value } ?: SessionStepKind.TIMED_WORK
}

private fun strengthSetKindFrom(value: String): StrengthSetKind {
    return StrengthSetKind.entries.firstOrNull { kind -> kind.contractValue == value } ?: StrengthSetKind.WORKING
}

private fun exerciseSideFrom(value: String): ExerciseSide {
    return ExerciseSide.entries.firstOrNull { side -> side.contractValue == value } ?: ExerciseSide.BOTH
}

private fun setEffortFrom(value: String): SetEffort {
    return SetEffort.entries.firstOrNull { effort -> effort.contractValue == value } ?: SetEffort.GOOD
}

private fun weightUnitFrom(value: String): WeightUnit? {
    return WeightUnit.entries.firstOrNull { unit -> unit.contractValue == value }
}
