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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
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

internal data class FinalizerPrerequisiteCandidate(
    val sessionId: String,
    val expectedStatus: String,
    val expectedTuple: CanonicalTuple,
    val recordingId: String,
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

    data class FinalizerPrerequisitePending(
        override val legacyResiduals: List<LegacySessionResidual>,
        val reconciledSessions: List<ReconciledCanonicalSession>,
        val candidates: List<FinalizerPrerequisiteCandidate>,
        val code: String = "FINALIZER_PREREQUISITE_PENDING"
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

internal class WorkoutSessionRepository(
    private val database: TrainFlowDatabase
) {
    private val dao = database.workoutSessionDao()
    private val canonicalDao = database.canonicalTimelineHeartRateDao()
    private val recorderGateMutex = Mutex()

    @Volatile
    private var completedRecorderGate: RecorderReconciliationResult? = null
    private var recorderGateInFlight: CompletableDeferred<RecorderReconciliationResult>? = null

    val sessions: Flow<List<WorkoutSession>> = dao.observeSessionsWithRecords()
        .map { rows -> rows.map { row -> row.toDomain() } }

    internal suspend fun finalizeRecordingSession(
        request: RecordingFinalizationRequest
    ): RecordingFinalizationResult {
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
            val graph = loadCanonicalGraph(request.sessionId)
                ?: throw RecorderGuardedWriteException("finalization_expected_session", 0)
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

    suspend fun prepareRecorder(): RecorderReconciliationResult {
        completedRecorderGate?.let { result -> return result }
        val (flight, ownsFlight) = recorderGateMutex.withLock {
            completedRecorderGate?.let { result -> return result }
            recorderGateInFlight?.let { existing -> existing to false }
                ?: CompletableDeferred<RecorderReconciliationResult>().let { created ->
                    recorderGateInFlight = created
                    created to true
                }
        }
        if (!ownsFlight) return flight.await()

        var outcome: Result<RecorderReconciliationResult>? = null
        return try {
            val result = runRecorderReconciliation()
            currentCoroutineContext().ensureActive()
            outcome = Result.success(result)
            result
        } catch (cause: Throwable) {
            outcome = Result.failure(cause)
            throw cause
        } finally {
            val completedOutcome = checkNotNull(outcome)
            withContext(NonCancellable) {
                recorderGateMutex.withLock {
                    check(recorderGateInFlight === flight)
                    completedOutcome.getOrNull()?.let { result ->
                        if (result !is RecorderReconciliationResult.FinalizerPrerequisitePending) {
                            completedRecorderGate = result
                        }
                    }
                    recorderGateInFlight = null
                    completedOutcome.fold(
                        onSuccess = flight::complete,
                        onFailure = flight::completeExceptionally
                    )
                }
            }
        }
    }

    suspend fun startCanonicalSession(
        session: WorkoutSessionEntity,
        initialPhase: WorkoutPhaseIntervalEntity
    ): RecorderReconciliationResult.Succeeded {
        val gate = requireRecorderGateSucceeded()
        val candidate = CanonicalSessionGraphV1(
            session = session,
            phases = listOf(initialPhase)
        )
        requireValidGraph(candidate)
        database.withTransaction {
            requireInserted("insert_canonical_session", dao.insertSession(session))
            requireInserted("insert_initial_phase", canonicalDao.insertPhaseInterval(initialPhase))
            requireValidGraph(requireNotNull(loadCanonicalGraph(session.id)))
        }
        return gate
    }

    suspend fun appendSessionDisplayMetadata(
        expected: RecorderExpectedState,
        nextTuple: CanonicalTuple,
        nextJson: String
    ): RecorderReconciliationResult.Succeeded {
        val gate = requireRecorderGateSucceeded()
        database.withTransaction {
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
        }
        return gate
    }

    suspend fun transitionPhase(
        expected: RecorderExpectedState,
        nextTuple: CanonicalTuple,
        nextPhase: WorkoutPhaseIntervalEntity
    ): RecorderReconciliationResult.Succeeded {
        val gate = requireRecorderGateSucceeded()
        database.withTransaction {
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
        }
        return gate
    }

    suspend fun startHeartRateRecording(
        expected: RecorderExpectedState,
        nextTuple: CanonicalTuple,
        recording: HeartRateRecordingEntity,
        initialAcquisition: HeartRateAcquisitionIntervalEntity
    ): RecorderReconciliationResult.Succeeded {
        val gate = requireRecorderGateSucceeded()
        database.withTransaction {
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
            advanceHeader(expected, nextTuple)
            requireInserted("insert_recording", canonicalDao.insertRecording(recording))
            requireInserted(
                "insert_initial_acquisition",
                canonicalDao.insertAcquisitionInterval(initialAcquisition)
            )
            requireValidGraph(requireNotNull(loadCanonicalGraph(expected.sessionId)))
        }
        return gate
    }

    suspend fun transitionAcquisition(
        expected: RecorderExpectedState,
        nextTuple: CanonicalTuple,
        nextAcquisition: HeartRateAcquisitionIntervalEntity
    ): RecorderReconciliationResult.Succeeded {
        val gate = requireRecorderGateSucceeded()
        database.withTransaction {
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
        }
        return gate
    }

    suspend fun appendHeartRateSample(
        expected: RecorderExpectedState,
        nextTuple: CanonicalTuple,
        sample: HeartRateSampleEntity
    ): RecorderReconciliationResult.Succeeded {
        val gate = requireRecorderGateSucceeded()
        database.withTransaction {
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
            advanceHeader(expected, nextTuple)
            requireInserted("insert_sample", canonicalDao.insertSample(sample))
            requireValidGraph(requireNotNull(loadCanonicalGraph(expected.sessionId)))
        }
        return gate
    }

    private suspend fun requireRecorderGateSucceeded(): RecorderReconciliationResult.Succeeded =
        when (val result = prepareRecorder()) {
            is RecorderReconciliationResult.Succeeded -> result
            is RecorderReconciliationResult.ManualResolutionRequired ->
                throw RecorderGateBlockedException(result)
            is RecorderReconciliationResult.FinalizerPrerequisitePending ->
                throw RecorderGateBlockedException(result)
        }

    private suspend fun validatedExpectedGraph(
        expected: RecorderExpectedState
    ): CanonicalSessionGraphV1 {
        val graph = loadCanonicalGraph(expected.sessionId)
            ?: throw RecorderGuardedWriteException("expected_session", 0)
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
            val pendingCandidates = mutableListOf<FinalizerPrerequisiteCandidate>()
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

                    is RecorderGateClassification.FinalizerPrerequisite ->
                        pendingCandidates += FinalizerPrerequisiteCandidate(
                            sessionId = session.id,
                            expectedStatus = session.status,
                            expectedTuple = classification.header.durableTuple,
                            recordingId = requireNotNull(classification.graph.recording).recordingId,
                            reconciliationContractVersion = RECONCILIATION_CONTRACT_VERSION
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
                reconcileCanonicalCandidate(candidate)
            }
            if (pendingCandidates.isEmpty()) {
                RecorderReconciliationResult.Succeeded(
                    legacyResiduals = residuals,
                    reconciledSessions = reconciled
                )
            } else {
                RecorderReconciliationResult.FinalizerPrerequisitePending(
                    legacyResiduals = residuals,
                    reconciledSessions = reconciled,
                    candidates = pendingCandidates
                )
            }
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
            is CanonicalSessionHeaderV1Result.CanonicalRunning -> {
                if (graph.recording != null) {
                    RecorderGateClassification.FinalizerPrerequisite(header, graph)
                } else {
                    RecorderGateClassification.CanonicalRunning(header, graph)
                }
            }

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

    data class FinalizerPrerequisite(
        val header: CanonicalSessionHeaderV1Result.CanonicalRunning,
        val graph: CanonicalSessionGraphV1
    ) : RecorderGateClassification

    data object CanonicalTerminal : RecorderGateClassification

    data class Failure(
        val failure: RecorderManualResolutionFailure
    ) : RecorderGateClassification
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
