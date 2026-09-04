package com.liujyks.trainflow.core.data

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutSessionRecorderReconciliationTest {
    private lateinit var database: TrainFlowDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TrainFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun concurrentAdmissionsMintOneOwnerAndPendingBlocksBeforeAnyGateCacheUse() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        assertTrue(repository.prepareRecorder() is RecorderReconciliationResult.Succeeded)

        val attempts = coroutineScope {
            (0 until 8).map { index ->
                async(Dispatchers.IO) {
                    runCatching { repository.admitRecorder("entry:race:$index") }
                }
            }.map { it.await() }
        }

        val admitted = attempts.single { it.isSuccess }.getOrThrow()
        assertTrue(attempts.filter { it.isFailure }.all {
            it.exceptionOrNull() is RecorderAdmissionBusyException
        })
        assertEquals(
            admitted.ownerToken,
            (repository.recorderOwnerState() as RecorderOwnerState.Active).ownerToken
        )

        assertTrue(
            repository.beginRecorderOwnerClearHandoff(admitted.ownerToken) is
                RecorderOwnerClearResult.Pending
        )
        assertTrue(repository.recorderOwnerState() is RecorderOwnerState.OwnerClearPending)
        assertTrue(
            runCatching { repository.admitRecorder("entry:pending") }.exceptionOrNull() is
                RecorderAdmissionBusyException
        )
        assertEquals(
            RecorderOwnerReleaseResult.CleanupRequired,
            repository.releaseRecorderOwner(admitted.ownerToken)
        )
        assertTrue(repository.recorderOwnerState() is RecorderOwnerState.OwnerClearPending)
    }

    @Test
    fun freshRepositoryReconcilesDurableProcessInterruptionBeforeMintingOwner() = runBlocking {
        val sessionId = "fresh-process-boundary"
        insertCanonicalRunningSession(sessionId)

        val freshRepository = WorkoutSessionRepository(database)
        val admission = freshRepository.admitRecorder("entry:fresh-process")

        val persisted = requireNotNull(
            database.canonicalTimelineHeartRateDao().canonicalGraphRows(sessionId)
        ).session
        assertEquals("abandoned", persisted.status)
        assertEquals("process_interrupted", persisted.terminalReason)
        assertTrue(freshRepository.recorderOwnerState() is RecorderOwnerState.Active)
        assertEquals(admission.ownerToken,
            (freshRepository.recorderOwnerState() as RecorderOwnerState.Active).ownerToken)
    }

    @Test
    fun legacyReadyActiveAndPausedReturnTypedResidualsWithoutMutationAndAllowCanonicalStart() = runBlocking {
        insertSession(legacySession("legacy-ready", "ready"))
        insertSession(legacySession("legacy-active", "active"))
        insertSession(legacySession("legacy-paused", "paused"))
        val before = databaseSnapshot()
        val repository = WorkoutSessionRepository(database)

        val result = repository.prepareRecorder()

        assertTrue(result is RecorderReconciliationResult.Succeeded)
        result as RecorderReconciliationResult.Succeeded
        assertEquals(
            listOf(
                LegacySessionResidual(
                    sessionId = "legacy-active",
                    status = "active",
                    timelineStatus = "legacy_noncanonical_nonterminal"
                ),
                LegacySessionResidual(
                    sessionId = "legacy-paused",
                    status = "paused",
                    timelineStatus = "legacy_noncanonical_nonterminal"
                ),
                LegacySessionResidual(
                    sessionId = "legacy-ready",
                    status = "ready",
                    timelineStatus = "legacy_incomplete_nonterminal"
                )
            ),
            result.legacyResiduals
        )
        assertTrue(result.reconciledSessions.isEmpty())
        assertEquals(before, databaseSnapshot())

        val started = repository.startCanonicalSession(
            canonicalHeader("canonical-new"),
            openPhase("canonical-new")
        )

        assertSame(result, started)
        assertEquals(
            "active",
            database.canonicalTimelineHeartRateDao().sessionById("canonical-new")?.status
        )
        assertEquals(
            listOf("active", "paused", "ready"),
            listOf("legacy-active", "legacy-paused", "legacy-ready").map { sessionId ->
                requireNotNull(database.canonicalTimelineHeartRateDao().sessionById(sessionId)).status
            }
        )
    }

    @Test
    fun mixedScanClassifiesEverythingBeforeFailingInvalidPartialWithoutReconcilingCanonicalRows() = runBlocking {
        insertSession(legacySession("legacy-active", "active"))
        insertCanonicalRunningSession("canonical-running")
        insertSession(
            legacySession("invalid-partial", "active").copy(timelineVersion = 1)
        )
        val before = databaseSnapshot()

        val result = WorkoutSessionRepository(database).prepareRecorder()

        assertTrue(result is RecorderReconciliationResult.ManualResolutionRequired)
        result as RecorderReconciliationResult.ManualResolutionRequired
        assertEquals(listOf("legacy-active"), result.legacyResiduals.map { it.sessionId })
        assertEquals(1, result.failures.size)
        assertEquals("invalid-partial", result.failures.single().sessionId)
        assertEquals(
            RecorderFailureKind.INVALID_PARTIAL_CANONICAL_HEADER,
            result.failures.single().kind
        )
        assertEquals("invalid_partial_canonical_header", result.failures.single().code)
        assertEquals(false, result.failures.single().retryable)
        assertEquals(true, result.failures.single().manualResolutionRequired)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun unknownVersionAndCorruptJsonAreTypedNonretryableManualResolutionFailures() = runBlocking {
        insertSession(
            canonicalHeader("unknown-timeline").copy(timelineVersion = 2)
        )
        insertSession(
            canonicalHeader("unknown-display-column").copy(displayMetadataContractVersion = 2)
        )
        insertSession(
            canonicalHeader("unknown-display-json").copy(
                sessionDisplayMetadataJson = VALID_DISPLAY_METADATA.replace(
                    "\"displayMetadataContractVersion\":1",
                    "\"displayMetadataContractVersion\":2"
                )
            )
        )
        insertSession(
            canonicalHeader("corrupt-json").copy(sessionDisplayMetadataJson = "{}")
        )
        val before = databaseSnapshot()
        val repository = WorkoutSessionRepository(database)

        val result = repository.prepareRecorder()

        assertTrue(result is RecorderReconciliationResult.ManualResolutionRequired)
        result as RecorderReconciliationResult.ManualResolutionRequired
        assertEquals(
            listOf(
                RecorderFailureKind.CORRUPT_JSON,
                RecorderFailureKind.UNKNOWN_VERSION,
                RecorderFailureKind.UNKNOWN_VERSION,
                RecorderFailureKind.UNKNOWN_VERSION
            ),
            result.failures.sortedBy { it.sessionId }.map { it.kind }
        )
        assertTrue(result.failures.all { failure ->
            !failure.retryable && failure.manualResolutionRequired
        })
        assertEquals(
            mapOf(
                "corrupt-json" to "invalid_session_display_metadata_contract",
                "unknown-display-column" to "unsupported_display_metadata_version_2",
                "unknown-display-json" to "unsupported_session_display_metadata_version_2",
                "unknown-timeline" to "unsupported_timeline_version_2"
            ),
            result.failures.associate { failure -> failure.sessionId to failure.code }
        )
        assertEquals(before, databaseSnapshot())
        assertEquals(null, inFlightOrNull(repository))
        assertSame(result, repository.prepareRecorder())
    }

    @Test
    fun nestedUnknownVersionsKeepSpecificContractAndVersionAcrossIndependentRoomFixtures() = runBlocking {
        insertCanonicalRunningSession(
            sessionId = "unknown-plan-snapshot",
            session = canonicalHeader("unknown-plan-snapshot").copy(
                planSnapshotJson = VALID_PLAN_SNAPSHOT.replace(
                    "\"planSnapshotStorageContractVersion\":1",
                    "\"planSnapshotStorageContractVersion\":2"
                )
            )
        )
        insertCanonicalRunningSession(
            sessionId = "unknown-plan-composition",
            session = canonicalHeader("unknown-plan-composition").copy(
                planSnapshotJson = VALID_PLAN_SNAPSHOT.replace(
                    "\"compositionVersion\":2",
                    "\"compositionVersion\":3"
                )
            )
        )
        insertCanonicalRunningSession(
            sessionId = "unknown-plan-compatibility",
            session = canonicalHeader("unknown-plan-compatibility").copy(
                planSnapshotJson = VALID_PLAN_SNAPSHOT.replace(
                    "\"stageGroups\":[]}",
                    "\"stageGroups\":[],\"compatibility\":{\"sourceVersion\":\"future_v3\"}}"
                )
            )
        )
        insertCanonicalRunningSession(
            sessionId = "unknown-phase-identity",
            phaseIdentityJson = VALID_PHASE_IDENTITY.replace(
                "\"phaseIdentityContractVersion\":1",
                "\"phaseIdentityContractVersion\":2"
            )
        )
        insertCanonicalRunningSession(
            sessionId = "unknown-phase-payload",
            phaseIdentityJson = VALID_PHASE_IDENTITY.replace(
                "\"payloadVersion\":2",
                "\"payloadVersion\":3"
            )
        )
        insertCanonicalRunningSession(
            sessionId = "unknown-signature",
            phaseIdentityJson = VALID_PHASE_IDENTITY.replace(
                "\"signatureContractVersion\":1",
                "\"signatureContractVersion\":2"
            )
        )
        insertCanonicalRunningSession(
            sessionId = "unknown-phase-composition",
            phaseIdentityJson = VALID_PHASE_IDENTITY.replace(
                "\"compositionVersion\":2",
                "\"compositionVersion\":3"
            )
        )
        insertCanonicalRunningSessionWithActiveRecording(
            sessionId = "unknown-recording-source",
            recordingTransform = { recording -> recording.copy(sourceContractVersion = 2) }
        )
        insertCanonicalRunningSessionWithActiveRecording(
            sessionId = "unknown-acquisition",
            recordingTransform = { recording -> recording.copy(acquisitionContractVersion = 2) }
        )
        insertCanonicalRunningSessionWithActiveRecording(
            sessionId = "unknown-parameter-snapshot",
            recordingTransform = { recording -> recording.copy(parameterSnapshotVersion = 2) }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-original-analysis",
            recordingTransform = { recording -> recording.copy(originalAnalysisVersion = 2) }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-zone-snapshot",
            recordingTransform = { recording ->
                recording.copy(
                    personalMaxBpm = 200,
                    effectiveMaxBpm = 200,
                    effectiveMaxSource = "personal_max",
                    zoneSnapshotJson = VALID_ZONE_SNAPSHOT_200.replace(
                        "\"zoneSnapshotContractVersion\":1",
                        "\"zoneSnapshotContractVersion\":2"
                    )
                )
            },
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    zoneStatus = "available",
                    zoneDurationsJson = VALID_ZONE_DURATIONS_120_OF_200
                )
            }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-analysis-snapshot",
            snapshotTransform = { snapshot -> snapshot.copy(analysisVersion = 2) }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-analysis-config",
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    analysisConfigJson = snapshot.analysisConfigJson.replace(
                        "\"analysisConfigContractVersion\":1",
                        "\"analysisConfigContractVersion\":2"
                    )
                )
            }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-sample-interval",
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    analysisConfigJson = snapshot.analysisConfigJson.replace(
                        "\"sampleIntervalContractVersion\":1",
                        "\"sampleIntervalContractVersion\":2"
                    )
                )
            }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-zone-attribution",
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    analysisConfigJson = snapshot.analysisConfigJson.replace(
                        "\"zoneAttributionContractVersion\":1",
                        "\"zoneAttributionContractVersion\":2"
                    )
                )
            }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-status-projection",
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    analysisConfigJson = snapshot.analysisConfigJson.replace(
                        "\"statusProjectionContractVersion\":1",
                        "\"statusProjectionContractVersion\":2"
                    )
                )
            }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-duration-partition",
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    analysisConfigJson = snapshot.analysisConfigJson.replace(
                        "\"durationPartitionContractVersion\":1",
                        "\"durationPartitionContractVersion\":2"
                    )
                )
            }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-zone-durations",
            recordingTransform = { recording ->
                recording.copy(
                    personalMaxBpm = 200,
                    effectiveMaxBpm = 200,
                    effectiveMaxSource = "personal_max",
                    zoneSnapshotJson = VALID_ZONE_SNAPSHOT_200
                )
            },
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    zoneStatus = "available",
                    zoneDurationsJson = VALID_ZONE_DURATIONS_120_OF_200.replace(
                        "\"zoneDurationsContractVersion\":1",
                        "\"zoneDurationsContractVersion\":2"
                    )
                )
            }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-phase-aggregates",
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    phaseAggregatesJson = snapshot.phaseAggregatesJson.replace(
                        "\"phaseAggregatesContractVersion\":1",
                        "\"phaseAggregatesContractVersion\":2"
                    )
                )
            }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-duration-breakdown",
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    durationBreakdownJson = snapshot.durationBreakdownJson.replace(
                        "\"durationBreakdownContractVersion\":1",
                        "\"durationBreakdownContractVersion\":2"
                    )
                )
            }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-duration-orthogonality",
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    durationBreakdownJson = snapshot.durationBreakdownJson.replace(
                        "\"contractVersion\":1",
                        "\"contractVersion\":2"
                    )
                )
            }
        )
        insertCanonicalTerminalSession(
            sessionId = "unknown-quality-reasons",
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    qualityReasonsJson = snapshot.qualityReasonsJson.replace(
                        "\"qualityReasonsContractVersion\":1",
                        "\"qualityReasonsContractVersion\":2"
                    )
                )
            }
        )
        insertCanonicalTerminalSession("valid-terminal-control")
        insertCanonicalTerminalSession(
            sessionId = "valid-zoned-control",
            recordingTransform = { recording ->
                recording.copy(
                    personalMaxBpm = 200,
                    effectiveMaxBpm = 200,
                    effectiveMaxSource = "personal_max",
                    zoneSnapshotJson = VALID_ZONE_SNAPSHOT_200
                )
            },
            snapshotTransform = { snapshot ->
                snapshot.copy(
                    zoneStatus = "available",
                    zoneDurationsJson = VALID_ZONE_DURATIONS_120_OF_200
                )
            }
        )
        val before = databaseSnapshot()

        val result = WorkoutSessionRepository(database).prepareRecorder()

        assertTrue(result is RecorderReconciliationResult.ManualResolutionRequired)
        result as RecorderReconciliationResult.ManualResolutionRequired
        assertTrue(result.failures.all { failure -> failure.kind == RecorderFailureKind.UNKNOWN_VERSION })
        assertEquals(
            mapOf(
                "unknown-acquisition" to "unsupported_acquisition_version_2",
                "unknown-analysis-config" to "unsupported_analysis_config_version_2",
                "unknown-analysis-snapshot" to "unsupported_analysis_snapshot_version_2",
                "unknown-duration-breakdown" to "unsupported_duration_breakdown_version_2",
                "unknown-duration-orthogonality" to
                    "unsupported_duration_breakdown_orthogonality_version_2",
                "unknown-duration-partition" to "unsupported_duration_partition_version_2",
                "unknown-original-analysis" to "unsupported_original_analysis_version_2",
                "unknown-parameter-snapshot" to
                    "unsupported_recording_parameter_snapshot_version_2",
                "unknown-phase-aggregates" to "unsupported_phase_aggregates_version_2",
                "unknown-phase-composition" to
                    "unsupported_phase_identity_timed_composition_version_3",
                "unknown-phase-identity" to "unsupported_phase_identity_version_2",
                "unknown-phase-payload" to "unsupported_phase_identity_payload_version_3",
                "unknown-plan-compatibility" to
                    "unsupported_plan_snapshot_compatibility_version_future_v3",
                "unknown-plan-composition" to
                    "unsupported_plan_snapshot_timed_composition_version_3",
                "unknown-plan-snapshot" to "unsupported_plan_snapshot_storage_version_2",
                "unknown-quality-reasons" to "unsupported_quality_reasons_version_2",
                "unknown-recording-source" to "unsupported_recording_source_version_2",
                "unknown-sample-interval" to "unsupported_sample_interval_version_2",
                "unknown-signature" to "unsupported_ordered_structure_signature_version_2",
                "unknown-status-projection" to "unsupported_status_projection_version_2",
                "unknown-zone-attribution" to "unsupported_zone_attribution_version_2",
                "unknown-zone-durations" to "unsupported_zone_durations_version_2",
                "unknown-zone-snapshot" to "unsupported_zone_snapshot_version_2"
            ),
            result.failures.associate { failure -> failure.sessionId to failure.code }
        )
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun illegalSourceVersionPositionsStayInvalidWhileLegalCompatibilityPathsStayTypedUnknown() = runBlocking {
        fun snapshotWithStageGroups(groupsJson: String): String =
            VALID_PLAN_SNAPSHOT.replace("\"stageGroups\":[]", "\"stageGroups\":[$groupsJson]")

        insertCanonicalRunningSession(
            sessionId = "illegal-root-source-version",
            session = canonicalHeader("illegal-root-source-version").copy(
                planSnapshotJson = VALID_PLAN_SNAPSHOT.replace(
                    "\"planSnapshotStorageContractVersion\":1",
                    "\"planSnapshotStorageContractVersion\":1,\"sourceVersion\":\"future_root_v3\""
                )
            )
        )
        insertCanonicalRunningSession(
            sessionId = "illegal-nested-source-version",
            session = canonicalHeader("illegal-nested-source-version").copy(
                planSnapshotJson = snapshotWithStageGroups(
                    "{\"id\":\"group\",\"order\":0,\"name\":\"Group\",\"colorHex\":\"#FFFFFF\"," +
                        "\"targets\":[],\"sourceVersion\":\"future_nested_v3\"}"
                )
            )
        )
        insertCanonicalRunningSession(
            sessionId = "unknown-block-compatibility",
            session = canonicalHeader("unknown-block-compatibility").copy(
                planSnapshotJson = VALID_PLAN_SNAPSHOT.replace(
                    "\"stageGroups\":[]}",
                    "\"stageGroups\":[],\"compatibility\":{\"sourceVersion\":\"future_block_v3\"}}"
                )
            )
        )
        insertCanonicalRunningSession(
            sessionId = "unknown-group-compatibility",
            session = canonicalHeader("unknown-group-compatibility").copy(
                planSnapshotJson = snapshotWithStageGroups(
                    "{\"id\":\"group\",\"order\":0,\"name\":\"Group\",\"colorHex\":\"#FFFFFF\"," +
                        "\"targets\":[],\"compatibility\":{\"sourceVersion\":\"future_group_v3\"}}"
                )
            )
        )
        insertCanonicalRunningSession(
            sessionId = "unknown-target-compatibility",
            session = canonicalHeader("unknown-target-compatibility").copy(
                planSnapshotJson = snapshotWithStageGroups(
                    "{\"id\":\"group\",\"order\":0,\"name\":\"Group\",\"colorHex\":\"#FFFFFF\"," +
                        "\"targets\":[{\"id\":\"target\",\"order\":0,\"name\":\"Target\",\"kind\":\"action\"," +
                        "\"durationSec\":10,\"colorHex\":\"#FFFFFF\",\"autoAdvance\":true," +
                        "\"compatibility\":{\"sourceVersion\":\"future_target_v3\"}}]}"
                )
            )
        )
        val before = databaseSnapshot()

        val result = WorkoutSessionRepository(database).prepareRecorder()

        assertTrue(result is RecorderReconciliationResult.ManualResolutionRequired)
        result as RecorderReconciliationResult.ManualResolutionRequired
        assertEquals(
            mapOf(
                "illegal-nested-source-version" to
                    (RecorderFailureKind.INVALID_CANONICAL_GRAPH to "invalid_canonical_graph_v1"),
                "illegal-root-source-version" to
                    (RecorderFailureKind.INVALID_CANONICAL_GRAPH to "invalid_canonical_graph_v1"),
                "unknown-block-compatibility" to
                    (RecorderFailureKind.UNKNOWN_VERSION to
                        "unsupported_plan_snapshot_compatibility_version_future_block_v3"),
                "unknown-group-compatibility" to
                    (RecorderFailureKind.UNKNOWN_VERSION to
                        "unsupported_plan_snapshot_compatibility_version_future_group_v3"),
                "unknown-target-compatibility" to
                    (RecorderFailureKind.UNKNOWN_VERSION to
                        "unsupported_plan_snapshot_compatibility_version_future_target_v3")
            ),
            result.failures.associate { failure ->
                failure.sessionId to (failure.kind to failure.code)
            }
        )
        assertTrue(result.failures.all { failure ->
            !failure.retryable && failure.manualResolutionRequired
        })
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun twoRepositoriesRaceOnePersistedCandidateThenFreshRepositoryReentryIsIdempotent() = runBlocking {
        insertCanonicalRunningSession("canonical-running")
        insertUnrelatedUserTableSentinels("canonical-running")
        val repositoryOne = WorkoutSessionRepository(database)
        val repositoryTwo = WorkoutSessionRepository(database)
        val ready = CountDownLatch(2)
        val start = CompletableDeferred<Unit>()

        val results = coroutineScope {
            val contenders = listOf(repositoryOne, repositoryTwo).map { repository ->
                async(Dispatchers.IO) {
                    ready.countDown()
                    start.await()
                    repository.prepareRecorder()
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.complete(Unit)
            contenders.map { contender -> contender.await() }
        }

        assertTrue(results.all { result -> result is RecorderReconciliationResult.Succeeded })
        val succeeded = results.map { result -> result as RecorderReconciliationResult.Succeeded }
        assertEquals(
            listOf(0, 1),
            succeeded.map { result -> result.reconciledSessions.size }.sorted()
        )
        assertEquals(
            ReconciledCanonicalSession(
                sessionId = "canonical-running",
                expectedTuple = CanonicalTuple(offsetMs = 100, mutationSequence = 4),
                reconciledTuple = CanonicalTuple(offsetMs = 100, mutationSequence = 5),
                reconciliationContractVersion = 1
            ),
            succeeded.single { result -> result.reconciledSessions.isNotEmpty() }
                .reconciledSessions.single()
        )
        val session = requireNotNull(
            database.canonicalTimelineHeartRateDao().sessionById("canonical-running")
        )
        assertEquals("abandoned", session.status)
        assertEquals(100L, session.lastDurableOffsetMs)
        assertEquals(5L, session.lastMutationSequence)
        assertEquals(100L, session.trustedEndOffsetMs)
        assertEquals("process_interrupted", session.terminalReason)
        val phase = database.canonicalTimelineHeartRateDao()
            .phaseIntervals("canonical-running")
            .single()
        assertEquals(100L, phase.endOffsetMs)
        assertEquals(5L, phase.endMutationSequence)
        assertEquals(null, phase.openMarker)
        val afterRace = databaseSnapshot()

        val reentered = WorkoutSessionRepository(database).prepareRecorder()

        assertTrue(reentered is RecorderReconciliationResult.Succeeded)
        reentered as RecorderReconciliationResult.Succeeded
        assertTrue(reentered.reconciledSessions.isEmpty())
        assertEquals(afterRace, databaseSnapshot())
    }

    @Test
    fun reconciliationPostValidationKeepsValidatorSignalAndRollsBackWholeTransaction() = runBlocking {
        insertCanonicalRunningSession("post-validation")
        insertUnrelatedUserTableSentinels("post-validation")
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER corrupt_reconciled_phase_after_close
            AFTER UPDATE OF open_marker ON workout_phase_intervals
            WHEN NEW.session_id = 'post-validation' AND NEW.open_marker IS NULL
            BEGIN
              UPDATE workout_phase_intervals SET phase_kind = 'corrupted' WHERE id = NEW.id;
            END
            """.trimIndent()
        )
        val before = databaseSnapshot()

        val failure = runCatching {
            WorkoutSessionRepository(database).prepareRecorder()
        }.exceptionOrNull()

        assertTrue(failure is RecorderValidationException)
        failure as RecorderValidationException
        assertEquals("invalid_canonical_graph_v1", failure.code)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun reconciliationPhaseGuardFailureRollsBackHeaderAndTriggerMutation() = runBlocking {
        insertCanonicalRunningSession("phase-guard")
        insertUnrelatedUserTableSentinels("phase-guard")
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER invalidate_phase_guard_after_header_reconcile
            AFTER UPDATE OF status ON workout_sessions
            WHEN NEW.id = 'phase-guard' AND NEW.status = 'abandoned'
            BEGIN
              UPDATE workout_phase_intervals SET open_marker = NULL WHERE session_id = NEW.id;
            END
            """.trimIndent()
        )
        val before = databaseSnapshot()

        val failure = runCatching {
            WorkoutSessionRepository(database).prepareRecorder()
        }.exceptionOrNull()

        assertTrue(failure is RecorderGuardedWriteException)
        failure as RecorderGuardedWriteException
        assertEquals("close_process_interrupted_phase", failure.guard)
        assertEquals(0, failure.actualRowCount)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun ownerCancellationCompletesSameFlightForWaiterAndAllowsFreshRetry() = runBlocking {
        val queryEntered = CountDownLatch(1)
        val releaseQuery = CountDownLatch(1)
        val queryReleased = CountDownLatch(1)
        val blockFirstGateQuery = AtomicBoolean(true)
        database.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TrainFlowDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCallback(
                RoomDatabase.QueryCallback { sqlQuery, _ ->
                    val normalized = sqlQuery.lowercase()
                    if (
                        normalized.contains("from workout_sessions") &&
                        normalized.contains("order by id") &&
                        blockFirstGateQuery.compareAndSet(true, false)
                    ) {
                        queryEntered.countDown()
                        check(releaseQuery.await(5, TimeUnit.SECONDS))
                        queryReleased.countDown()
                    }
                },
                Executor { command -> command.run() }
            )
            .build()
        insertSession(legacySession("legacy-active", "active"))
        val repository = WorkoutSessionRepository(database)
        val mutexProbe = GateMutexProbe()
        replaceGateMutex(repository, mutexProbe)
        val ownerCancellation = CancellationException("owner_cancelled")
        val owner = async(Dispatchers.IO) { repository.prepareRecorder() }
        assertTrue(queryEntered.await(5, TimeUnit.SECONDS))
        val flight = inFlight(repository)
        val completionCause = AtomicReference<Throwable?>()
        val flightCompleted = CountDownLatch(1)
        flight.invokeOnCompletion { cause ->
            completionCause.set(cause)
            flightCompleted.countDown()
        }
        val waiter = async(start = CoroutineStart.UNDISPATCHED) {
            repository.prepareRecorder()
        }
        mutexProbe.lockForTest()
        try {
            owner.cancel(ownerCancellation)
            releaseQuery.countDown()
            assertTrue(queryReleased.await(5, TimeUnit.SECONDS))
            assertTrue(mutexProbe.cleanupLockAttempt.await(5, TimeUnit.SECONDS))
        } finally {
            mutexProbe.unlockForTest()
        }

        val ownerFailure = withTimeout(5_000) {
            runCatching { owner.await() }.exceptionOrNull()
        }
        val waiterFailure = withTimeoutOrNull(5_000) {
            runCatching { waiter.await() }.exceptionOrNull()
        }

        assertTrue(flightCompleted.await(5, TimeUnit.SECONDS))
        val propagatedCause = requireNotNull(completionCause.get())
        assertTrue(propagatedCause is CancellationException)
        assertEquals(ownerCancellation.message, propagatedCause.message)
        assertTrue(ownerFailure is CancellationException)
        assertNotNull(waiterFailure)
        assertTrue(waiterFailure is CancellationException)
        assertEquals(propagatedCause.message, ownerFailure?.message)
        assertEquals(ownerCancellation.message, waiterFailure?.message)
        assertEquals(null, inFlightOrNull(repository))
        val retry = withTimeout(5_000) { repository.prepareRecorder() }
        assertTrue(retry is RecorderReconciliationResult.Succeeded)
        retry as RecorderReconciliationResult.Succeeded
        assertEquals(listOf("legacy-active"), retry.legacyResiduals.map { residual -> residual.sessionId })
    }

    @Test
    fun exceptionalFlightPropagatesOriginalCauseToOwnerAndWaiterThenAllowsRetry() = runBlocking {
        val queryEntered = CountDownLatch(1)
        val releaseQuery = CountDownLatch(1)
        val blockFirstGateQuery = AtomicBoolean(true)
        database.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TrainFlowDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCallback(
                RoomDatabase.QueryCallback { sqlQuery, _ ->
                    val normalized = sqlQuery.lowercase()
                    if (
                        normalized.contains("from workout_sessions") &&
                        normalized.contains("order by id") &&
                        blockFirstGateQuery.compareAndSet(true, false)
                    ) {
                        queryEntered.countDown()
                        check(releaseQuery.await(5, TimeUnit.SECONDS))
                    }
                },
                Executor { command -> command.run() }
            )
            .build()
        insertCanonicalRunningSession("exceptional-flight")
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER invalidate_exceptional_flight_phase_guard
            AFTER UPDATE OF status ON workout_sessions
            WHEN NEW.id = 'exceptional-flight' AND NEW.status = 'abandoned'
            BEGIN
              UPDATE workout_phase_intervals SET open_marker = NULL WHERE session_id = NEW.id;
            END
            """.trimIndent()
        )
        val before = databaseSnapshot()
        val repository = WorkoutSessionRepository(database)
        val completionCause = AtomicReference<Throwable?>()
        val flightCompleted = CountDownLatch(1)
        val (ownerFailure, waiterFailure) = supervisorScope {
            val owner = async(Dispatchers.IO) { repository.prepareRecorder() }
            assertTrue(queryEntered.await(5, TimeUnit.SECONDS))
            val flight = inFlight(repository)
            flight.invokeOnCompletion { cause ->
                completionCause.set(cause)
                flightCompleted.countDown()
            }
            val waiter = async(start = CoroutineStart.UNDISPATCHED) {
                repository.prepareRecorder()
            }

            releaseQuery.countDown()
            val ownerCause = withTimeout(5_000) {
                runCatching { owner.await() }.exceptionOrNull()
            }
            val waiterCause = withTimeout(5_000) {
                runCatching { waiter.await() }.exceptionOrNull()
            }
            ownerCause to waiterCause
        }

        assertTrue(flightCompleted.await(5, TimeUnit.SECONDS))
        val propagatedCause = requireNotNull(completionCause.get())
        assertTrue(propagatedCause is RecorderGuardedWriteException)
        propagatedCause as RecorderGuardedWriteException
        assertEquals("close_process_interrupted_phase", propagatedCause.guard)
        assertEquals(0, propagatedCause.actualRowCount)
        assertSame(propagatedCause, ownerFailure)
        assertSame(propagatedCause, waiterFailure)
        assertEquals(before, databaseSnapshot())
        assertEquals(null, inFlightOrNull(repository))

        database.openHelper.writableDatabase.execSQL(
            "DROP TRIGGER invalidate_exceptional_flight_phase_guard"
        )
        val retry = withTimeout(5_000) { repository.prepareRecorder() }
        assertTrue(retry is RecorderReconciliationResult.Succeeded)
        retry as RecorderReconciliationResult.Succeeded
        assertEquals(listOf("exceptional-flight"), retry.reconciledSessions.map { it.sessionId })
    }

    @Test
    fun databaseSnapshotManifestCoversEveryRoomUserTableAndEveryPersistedColumn() = runBlocking {
        insertCanonicalTerminalSession("manifest")
        insertUnrelatedUserTableSentinels("manifest")

        val snapshot = databaseSnapshot()

        assertEquals(
            EXPECTED_USER_TABLES,
            snapshot.map { row -> row.substringBefore('|') }.distinct().sorted()
        )
    }

    @Test
    fun canonicalActiveRecordingRelaunchFinalizesTerminalGraphCachesSuccessAndAllowsProtectedStart() = runBlocking {
        insertCanonicalRunningSessionWithActiveRecording("canonical-with-recording")
        val repository = WorkoutSessionRepository(database)
        val callStartedAt = Instant.now()

        val result = repository.prepareRecorder()
        val callFinishedAt = Instant.now()

        assertTrue(result is RecorderReconciliationResult.Succeeded)
        result as RecorderReconciliationResult.Succeeded
        assertTrue(result.legacyResiduals.isEmpty())
        assertEquals(
            listOf(
                ReconciledCanonicalSession(
                    sessionId = "canonical-with-recording",
                    expectedTuple = CanonicalTuple(offsetMs = 100, mutationSequence = 4),
                    reconciledTuple = CanonicalTuple(offsetMs = 100, mutationSequence = 5),
                    reconciliationContractVersion = 1
                )
            ),
            result.reconciledSessions
        )
        val rows = requireNotNull(
            database.canonicalTimelineHeartRateDao()
                .canonicalGraphRows("canonical-with-recording")
        )
        assertEquals("abandoned", rows.session.status)
        assertEquals(100L, rows.session.lastDurableOffsetMs)
        assertEquals(5L, rows.session.lastMutationSequence)
        assertEquals(100L, rows.session.trustedEndOffsetMs)
        assertEquals("process_interrupted", rows.session.terminalReason)
        assertEquals(1, rows.phases.size)
        assertEquals(100L, rows.phases.single().endOffsetMs)
        assertEquals(5L, rows.phases.single().endMutationSequence)
        assertEquals(null, rows.phases.single().openMarker)
        val recordingRows = rows.recordings.single()
        assertEquals("canonical-with-recording:recording", recordingRows.recording.recordingId)
        assertEquals("canonical-with-recording", recordingRows.recording.sessionId)
        assertEquals("terminal", recordingRows.recording.status)
        assertEquals(100L, recordingRows.recording.endedOffsetMs)
        assertEquals(5L, recordingRows.recording.endedMutationSequence)
        assertEquals(1, recordingRows.recording.originalAnalysisVersion)
        assertEquals(1, recordingRows.acquisitions.size)
        assertEquals(100L, recordingRows.acquisitions.single().endOffsetMs)
        assertEquals(5L, recordingRows.acquisitions.single().endMutationSequence)
        assertEquals(null, recordingRows.acquisitions.single().openMarker)
        assertEquals(1, recordingRows.samples.size)
        assertEquals(1, recordingRows.snapshots.size)
        val snapshot = recordingRows.snapshots.single()
        assertEquals("canonical-with-recording:recording", snapshot.recordingId)
        assertEquals(1, snapshot.analysisVersion)
        assertEquals(5L, snapshot.inputLastMutationSequence)
        assertEquals("primary_points_available", snapshot.sampleStatus)
        assertEquals("normal", snapshot.coverageStatus)
        assertEquals("unavailable_no_effective_max", snapshot.zoneStatus)
        assertEquals(1L, snapshot.canonicalSampleCount)
        assertEquals(1L, snapshot.primaryPointSampleCount)
        assertEquals(100L, snapshot.eligibleDurationMs)
        assertEquals(100L, snapshot.coveredDurationMs)
        assertEquals(10_000, snapshot.coverageBasisPoints)
        assertEquals(12_000L, snapshot.weightedBpmMs)
        assertEquals(120, snapshot.observedAvgBpm)
        assertEquals(120, snapshot.observedMaxBpm)
        assertEquals(0L, snapshot.highestOffsetMs)
        assertEquals(0L, snapshot.highestMutationSequence)
        assertEquals(0L, snapshot.highestSampleSequence)
        assertEquals(VALID_ANALYSIS_CONFIG, snapshot.analysisConfigJson)
        assertEquals(null, snapshot.zoneDurationsJson)
        assertEquals(VALID_PHASE_AGGREGATES, snapshot.phaseAggregatesJson)
        assertEquals(VALID_DURATION_BREAKDOWN, snapshot.durationBreakdownJson)
        assertEquals(
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[" +
                "{\"reasonCode\":\"unavailable_no_effective_max\",\"durationMs\":null}," +
                "{\"reasonCode\":\"process_interrupted\",\"durationMs\":null}]," +
                "\"phaseReasons\":[{\"phaseSequence\":0," +
                "\"reasonCode\":\"unavailable_no_effective_max\",\"durationMs\":null}]}",
            snapshot.qualityReasonsJson
        )
        val createdAt = Instant.parse(snapshot.createdAt)
        assertTrue(!createdAt.isBefore(callStartedAt.minusSeconds(10)))
        assertTrue(!createdAt.isAfter(callFinishedAt.plusSeconds(10)))

        val afterFinalization = databaseSnapshot()
        assertSame(result, repository.prepareRecorder())
        assertEquals(afterFinalization, databaseSnapshot())
        val protectedStart = repository.startCanonicalSession(
            canonicalHeader("canonical-after-finalization"),
            openPhase("canonical-after-finalization")
        )
        assertSame(result, protectedStart)
        assertEquals(
            "active",
            database.canonicalTimelineHeartRateDao()
                .sessionById("canonical-after-finalization")?.status
        )
        assertEquals(null, inFlightOrNull(repository))
    }

    @Test
    fun integratedFinalizerFailureKeepsOriginalGuardRollsBackAndDoesNotCache() = runBlocking {
        insertCanonicalRunningSessionWithActiveRecording("binding-failure")
        insertUnrelatedUserTableSentinels("binding-failure")
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER occupy_original_binding_after_snapshot
            AFTER INSERT ON heart_rate_analysis_snapshots
            WHEN NEW.recording_id = 'binding-failure:recording'
            BEGIN
              UPDATE heart_rate_recordings
              SET original_analysis_version = 1
              WHERE recording_id = NEW.recording_id;
            END
            """.trimIndent()
        )
        val before = databaseSnapshot()
        val repository = WorkoutSessionRepository(database)

        repeat(2) {
            val failure = runCatching { repository.prepareRecorder() }.exceptionOrNull()
            assertTrue(failure is RecorderGuardedWriteException)
            failure as RecorderGuardedWriteException
            assertEquals("bind_original_analysis", failure.guard)
            assertEquals(0, failure.actualRowCount)
            assertEquals(before, databaseSnapshot())
            assertEquals(null, inFlightOrNull(repository))
        }

        val protectedFailure = runCatching {
            repository.appendSessionDisplayMetadata(
                expected = RecorderExpectedState(
                    sessionId = "binding-failure",
                    status = "active",
                    durableTuple = CanonicalTuple(offsetMs = 100, mutationSequence = 4),
                    openPhaseId = "binding-failure:phase:0",
                    recordingId = "binding-failure:recording",
                    openAcquisitionId = "binding-failure:acquisition"
                ),
                nextTuple = CanonicalTuple(offsetMs = 100, mutationSequence = 5),
                nextJson = VALID_DISPLAY_METADATA
            )
        }.exceptionOrNull()
        assertTrue(protectedFailure is RecorderGuardedWriteException)
        protectedFailure as RecorderGuardedWriteException
        assertEquals("bind_original_analysis", protectedFailure.guard)
        assertEquals(0, protectedFailure.actualRowCount)
        assertEquals(before, databaseSnapshot())

        database.openHelper.writableDatabase.execSQL(
            "DROP TRIGGER occupy_original_binding_after_snapshot"
        )
        val retry = repository.prepareRecorder()
        assertTrue(retry is RecorderReconciliationResult.Succeeded)
        assertEquals(
            listOf("binding-failure"),
            (retry as RecorderReconciliationResult.Succeeded)
                .reconciledSessions.map { session -> session.sessionId }
        )
    }

    @Test
    fun mixedDatabaseReconcilesNoRecordingAndActiveRecordingWithIdentityBoundOrder() = runBlocking {
        insertSession(legacySession("legacy-active", "active"))
        insertCanonicalRunningSession("no-recording")
        insertCanonicalRunningSessionWithActiveRecording("with-recording")
        insertCanonicalTerminalSession("terminal-recording")

        val result = WorkoutSessionRepository(database).prepareRecorder()

        assertTrue(result is RecorderReconciliationResult.Succeeded)
        result as RecorderReconciliationResult.Succeeded
        assertEquals(
            listOf("legacy-active"),
            result.legacyResiduals.map { residual -> residual.sessionId }
        )
        assertEquals(
            listOf(
                ReconciledCanonicalSession(
                    sessionId = "no-recording",
                    expectedTuple = CanonicalTuple(100, 4),
                    reconciledTuple = CanonicalTuple(100, 5),
                    reconciliationContractVersion = 1
                ),
                ReconciledCanonicalSession(
                    sessionId = "with-recording",
                    expectedTuple = CanonicalTuple(100, 4),
                    reconciledTuple = CanonicalTuple(100, 5),
                    reconciliationContractVersion = 1
                )
            ),
            result.reconciledSessions
        )
        val dao = database.canonicalTimelineHeartRateDao()
        assertEquals("abandoned", dao.sessionById("no-recording")?.status)
        assertEquals("process_interrupted", dao.sessionById("no-recording")?.terminalReason)
        assertEquals("abandoned", dao.sessionById("with-recording")?.status)
        assertEquals("process_interrupted", dao.sessionById("with-recording")?.terminalReason)
        assertEquals("completed", dao.sessionById("terminal-recording")?.status)
        val finalizedRecording = requireNotNull(dao.canonicalGraphRows("with-recording"))
            .recordings.single()
        assertEquals("with-recording:recording", finalizedRecording.recording.recordingId)
        assertEquals("with-recording", finalizedRecording.recording.sessionId)
        assertEquals(1, finalizedRecording.snapshots.size)
        assertEquals("with-recording:recording", finalizedRecording.snapshots.single().recordingId)
        assertEquals(1, finalizedRecording.recording.originalAnalysisVersion)
        val preservedTerminal = requireNotNull(dao.canonicalGraphRows("terminal-recording"))
            .recordings.single()
        assertEquals("terminal-recording:recording", preservedTerminal.recording.recordingId)
        assertEquals("2026-08-25T00:00:00Z", preservedTerminal.snapshots.single().createdAt)
    }

    @Test
    fun concurrentActiveRecordingAccessProducesOneFinalizationAndCachesSameSuccess() = runBlocking {
        insertCanonicalRunningSessionWithActiveRecording("canonical-with-recording")
        val repository = WorkoutSessionRepository(database)

        val concurrent = coroutineScope {
            listOf(
                async(Dispatchers.IO) { repository.prepareRecorder() },
                async(Dispatchers.IO) { repository.prepareRecorder() }
            ).map { deferred -> deferred.await() }
        }

        assertSame(concurrent[0], concurrent[1])
        assertTrue(concurrent[0] is RecorderReconciliationResult.Succeeded)
        val succeeded = concurrent[0] as RecorderReconciliationResult.Succeeded
        assertEquals(
            listOf("canonical-with-recording"),
            succeeded.reconciledSessions.map { session -> session.sessionId }
        )
        val recordingRows = requireNotNull(
            database.canonicalTimelineHeartRateDao()
                .canonicalGraphRows("canonical-with-recording")
        ).recordings.single()
        assertEquals(1, recordingRows.snapshots.size)
        assertEquals(1, recordingRows.recording.originalAnalysisVersion)
        assertSame(concurrent[0], repository.prepareRecorder())
        assertEquals(null, inFlightOrNull(repository))
    }

    @Test
    fun concurrentFirstProtectedAccessSharesOneCompletedGateResult() = runBlocking {
        insertSession(legacySession("legacy-active", "active"))
        val repository = WorkoutSessionRepository(database)

        val results = coroutineScope {
            listOf(
                async(Dispatchers.IO) { repository.prepareRecorder() },
                async(Dispatchers.IO) { repository.prepareRecorder() }
            ).map { deferred -> deferred.await() }
        }
        insertSession(legacySession("legacy-after-gate", "paused"))
        val cached = repository.prepareRecorder()

        assertSame(results[0], results[1])
        assertSame(results[0], cached)
        assertTrue(cached is RecorderReconciliationResult.Succeeded)
        cached as RecorderReconciliationResult.Succeeded
        assertEquals(listOf("legacy-active"), cached.legacyResiduals.map { it.sessionId })
        assertEquals(null, inFlightOrNull(repository))
    }

    private suspend fun insertCanonicalRunningSession(
        sessionId: String,
        session: WorkoutSessionEntity = canonicalHeader(sessionId),
        phaseIdentityJson: String = VALID_PHASE_IDENTITY
    ) {
        insertSession(session)
        database.canonicalTimelineHeartRateDao().insertPhaseInterval(
            openPhase(sessionId, phaseIdentityJson)
        )
    }

    private suspend fun insertCanonicalRunningSessionWithActiveRecording(
        sessionId: String,
        recordingTransform: (HeartRateRecordingEntity) -> HeartRateRecordingEntity = { it }
    ) {
        insertCanonicalRunningSession(sessionId)
        database.canonicalTimelineHeartRateDao().insertRecording(
            recordingTransform(
                HeartRateRecordingEntity(
                    recordingId = "$sessionId:recording",
                    sessionId = sessionId,
                    status = "active",
                    startedOffsetMs = 0,
                    startedMutationSequence = 0,
                    endedOffsetMs = null,
                    endedMutationSequence = null,
                    sourceContractVersion = 1,
                    sourceKind = "ble_hrs",
                    acquisitionContractVersion = 1,
                    parameterSnapshotVersion = 1,
                    originalAnalysisVersion = null
                )
            )
        )
        database.canonicalTimelineHeartRateDao().insertAcquisitionInterval(
            HeartRateAcquisitionIntervalEntity(
                id = "$sessionId:acquisition",
                recordingId = "$sessionId:recording",
                sequence = 0,
                startOffsetMs = 0,
                endOffsetMs = null,
                startMutationSequence = 0,
                endMutationSequence = null,
                openMarker = 1,
                recordingIntent = "expected_recording",
                intentReason = null,
                deviceState = "live",
                deviceReason = null
            )
        )
        database.canonicalTimelineHeartRateDao().insertSample(
            HeartRateSampleEntity(
                recordingId = "$sessionId:recording",
                sampleSequence = 0,
                offsetMs = 0,
                mutationSequence = 0,
                bpm = 120
            )
        )
    }

    private suspend fun insertCanonicalTerminalSession(
        sessionId: String,
        recordingTransform: (HeartRateRecordingEntity) -> HeartRateRecordingEntity = { it },
        snapshotTransform: (HeartRateAnalysisSnapshotEntity) -> HeartRateAnalysisSnapshotEntity = { it }
    ) {
        insertSession(
            canonicalHeader(sessionId).copy(
                status = "completed",
                trustedEndOffsetMs = 100,
                terminalReason = "completed"
            )
        )
        database.canonicalTimelineHeartRateDao().insertPhaseInterval(
            openPhase(sessionId).copy(
                endOffsetMs = 100,
                endMutationSequence = 4,
                openMarker = null
            )
        )
        database.canonicalTimelineHeartRateDao().insertRecording(
            recordingTransform(
                HeartRateRecordingEntity(
                    recordingId = "$sessionId:recording",
                    sessionId = sessionId,
                    status = "terminal",
                    startedOffsetMs = 0,
                    startedMutationSequence = 0,
                    endedOffsetMs = 100,
                    endedMutationSequence = 4,
                    sourceContractVersion = 1,
                    sourceKind = "ble_hrs",
                    acquisitionContractVersion = 1,
                    parameterSnapshotVersion = 1,
                    originalAnalysisVersion = 1
                )
            )
        )
        database.canonicalTimelineHeartRateDao().insertAcquisitionInterval(
            HeartRateAcquisitionIntervalEntity(
                id = "$sessionId:acquisition",
                recordingId = "$sessionId:recording",
                sequence = 0,
                startOffsetMs = 0,
                endOffsetMs = 100,
                startMutationSequence = 0,
                endMutationSequence = 4,
                openMarker = null,
                recordingIntent = "expected_recording",
                intentReason = null,
                deviceState = "live",
                deviceReason = null
            )
        )
        database.canonicalTimelineHeartRateDao().insertSample(
            HeartRateSampleEntity(
                recordingId = "$sessionId:recording",
                sampleSequence = 0,
                offsetMs = 0,
                mutationSequence = 0,
                bpm = 120
            )
        )
        database.canonicalTimelineHeartRateDao().insertAnalysisSnapshot(
            snapshotTransform(
                HeartRateAnalysisSnapshotEntity(
                    recordingId = "$sessionId:recording",
                    analysisVersion = 1,
                    createdAt = "2026-08-25T00:00:00Z",
                    inputLastMutationSequence = 4,
                    sampleStatus = "primary_points_available",
                    coverageStatus = "normal",
                    zoneStatus = "unavailable_no_effective_max",
                    canonicalSampleCount = 1,
                    primaryPointSampleCount = 1,
                    eligibleDurationMs = 100,
                    coveredDurationMs = 100,
                    coverageBasisPoints = 10000,
                    weightedBpmMs = 12000,
                    observedAvgBpm = 120,
                    observedMaxBpm = 120,
                    highestOffsetMs = 0,
                    highestMutationSequence = 0,
                    highestSampleSequence = 0,
                    analysisConfigJson = VALID_ANALYSIS_CONFIG,
                    zoneDurationsJson = null,
                    phaseAggregatesJson = VALID_PHASE_AGGREGATES,
                    durationBreakdownJson = VALID_DURATION_BREAKDOWN,
                    qualityReasonsJson = VALID_QUALITY_REASONS
                )
            )
        )
    }

    private fun openPhase(
        sessionId: String,
        phaseIdentityJson: String = VALID_PHASE_IDENTITY
    ) = WorkoutPhaseIntervalEntity(
        id = "$sessionId:phase:0",
        sessionId = sessionId,
        sequence = 0,
        startOffsetMs = 0,
        endOffsetMs = null,
        startMutationSequence = 0,
        endMutationSequence = null,
        openMarker = 1,
        phaseKind = "timed_work",
        phaseIdentityJson = phaseIdentityJson
    )

    private fun insertUnrelatedUserTableSentinels(sessionId: String) {
        val sql = database.openHelper.writableDatabase
        sql.execSQL(
            "INSERT INTO exercises VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("sentinel-exercise", "Sentinel", "mobility", "[]", "beginner", "{}", "ready", null)
        )
        sql.execSQL(
            "INSERT INTO workout_plans VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                "sentinel-plan", "timed", "Sentinel", null, "[]", null, null, null,
                "2026-08-30T00:00:00Z", "2026-08-30T00:00:00Z"
            )
        )
        sql.execSQL(
            "INSERT INTO session_step_records VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                "sentinel-step", sessionId, "step", "timed_work", "block", "item", null,
                "sentinel-exercise", "2026-08-30T00:00:00Z", null, 0, null, 10
            )
        )
        sql.execSQL(
            "INSERT INTO timed_rest_extension_records VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                "sentinel-rest", sessionId, "step", 0, null, null, "Rest", null, null,
                15, 30, 5, 25, 15, 35
            )
        )
        sql.execSQL(
            "INSERT INTO strength_set_records VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                "sentinel-set", sessionId, "sentinel-exercise", null, 0, "working", null,
                null, null, null, null, null, null, null
            )
        )
        sql.execSQL(
            "INSERT INTO recovery_areas VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("sentinel-area", "Area", "whole_body", "Summary", null, null)
        )
        sql.execSQL(
            "INSERT INTO recovery_recommendations VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>("sentinel-recovery", sessionId, "[]", "[\"sentinel-area\"]", null)
        )
    }

    private fun inFlight(
        repository: WorkoutSessionRepository
    ): CompletableDeferred<RecorderReconciliationResult> = requireNotNull(
        inFlightOrNull(repository)
    )

    @Suppress("UNCHECKED_CAST")
    private fun inFlightOrNull(
        repository: WorkoutSessionRepository
    ): CompletableDeferred<RecorderReconciliationResult>? {
        val field = WorkoutSessionRepository::class.java.getDeclaredField("recorderGateInFlight")
        field.isAccessible = true
        return field.get(repository) as CompletableDeferred<RecorderReconciliationResult>?
    }

    private fun replaceGateMutex(repository: WorkoutSessionRepository, mutex: Mutex) {
        val field = WorkoutSessionRepository::class.java.getDeclaredField("recorderGateMutex")
        field.isAccessible = true
        field.set(repository, mutex)
    }

    private class GateMutexProbe(
        private val delegate: Mutex = Mutex()
    ) : Mutex by delegate {
        private val lockAttempts = AtomicInteger()
        val cleanupLockAttempt = CountDownLatch(1)

        override suspend fun lock(owner: Any?) {
            if (lockAttempts.incrementAndGet() == 3) cleanupLockAttempt.countDown()
            delegate.lock(owner)
        }

        suspend fun lockForTest() {
            delegate.lock()
        }

        fun unlockForTest() {
            delegate.unlock()
        }
    }

    private fun canonicalHeader(sessionId: String) = WorkoutSessionEntity(
        id = sessionId,
        mode = "timed",
        status = "active",
        planSnapshotJson = VALID_PLAN_SNAPSHOT,
        timelineVersion = 1,
        lastDurableOffsetMs = 100,
        lastMutationSequence = 4,
        displayMetadataContractVersion = 1,
        sessionDisplayMetadataJson = VALID_DISPLAY_METADATA
    )

    private suspend fun insertSession(session: WorkoutSessionEntity) {
        assertTrue(database.workoutSessionDao().insertSession(session) != -1L)
    }

    private fun legacySession(id: String, status: String) = WorkoutSessionEntity(
        id = id,
        mode = "timed",
        status = status,
        planSnapshotJson = "{\"title\":\"Legacy\",\"mode\":\"timed\",\"blocks\":[]}"
    )

    private fun databaseSnapshot(): List<String> {
        val sql = database.openHelper.writableDatabase
        val tables = sql.query(
            """
            SELECT name
            FROM sqlite_master
            WHERE type = 'table'
              AND name NOT GLOB 'sqlite_*'
              AND name NOT IN ('android_metadata', 'room_master_table')
            ORDER BY name
            """.trimIndent()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
        check(tables == EXPECTED_USER_TABLES) {
            "Room user-table manifest changed: $tables"
        }
        return tables.flatMap { table ->
            val quotedTable = quoteSqlIdentifier(table)
            val columns = sql.query("PRAGMA table_info($quotedTable)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            SnapshotColumn(
                                name = cursor.getString(1),
                                declaredType = cursor.getString(2),
                                notNull = cursor.getInt(3) == 1,
                                primaryKeyOrder = cursor.getInt(5)
                            )
                        )
                    }
                }
            }.sortedBy { column -> column.name }
            check(columns.isNotEmpty()) { "Room user table has no persisted columns: $table" }
            val primaryKey = columns.filter { column -> column.primaryKeyOrder > 0 }
                .sortedBy { column -> column.primaryKeyOrder }
            check(primaryKey.isNotEmpty()) { "Room user table has no primary key: $table" }
            val selectColumns = columns.joinToString(", ") { column ->
                quoteSqlIdentifier(column.name)
            }
            val orderColumns = primaryKey.joinToString(", ") { column ->
                quoteSqlIdentifier(column.name)
            }
            buildList {
                add(
                    "$table|schema|" + columns.joinToString("|") { column ->
                        "${column.name}:${column.declaredType}:${column.notNull}:${column.primaryKeyOrder}"
                    }
                )
                sql.query(
                    "SELECT $selectColumns FROM $quotedTable ORDER BY $orderColumns"
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        add(
                            "$table|row|" + columns.mapIndexed { index, column ->
                                "${column.name}=${cursor.snapshotValue(index)}"
                            }.joinToString("|")
                        )
                    }
                }
            }
        }
    }

    private data class SnapshotColumn(
        val name: String,
        val declaredType: String,
        val notNull: Boolean,
        val primaryKeyOrder: Int
    )

    private fun Cursor.snapshotValue(index: Int): String = when (getType(index)) {
        Cursor.FIELD_TYPE_NULL -> "null"
        Cursor.FIELD_TYPE_INTEGER -> "integer:${getLong(index)}"
        Cursor.FIELD_TYPE_FLOAT -> "float:${java.lang.Double.toHexString(getDouble(index))}"
        Cursor.FIELD_TYPE_STRING -> getString(index).toByteArray(Charsets.UTF_8).let { bytes ->
            "string:${bytes.size}:${bytes.toHexString()}"
        }

        Cursor.FIELD_TYPE_BLOB -> getBlob(index).let { bytes ->
            "blob:${bytes.size}:${bytes.toHexString()}"
        }

        else -> error("Unsupported SQLite value type ${getType(index)}")
    }

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }

    private fun quoteSqlIdentifier(identifier: String): String =
        "\"${identifier.replace("\"", "\"\"")}\""

    private companion object {
        val EXPECTED_USER_TABLES = listOf(
            "exercises",
            "heart_rate_acquisition_intervals",
            "heart_rate_analysis_snapshots",
            "heart_rate_recordings",
            "heart_rate_samples",
            "recovery_areas",
            "recovery_recommendations",
            "session_step_records",
            "strength_set_records",
            "timed_rest_extension_records",
            "workout_phase_intervals",
            "workout_plans",
            "workout_sessions"
        )
        const val VALID_DISPLAY_METADATA =
            "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
        const val VALID_PLAN_SNAPSHOT =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Timed\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"block\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[]}],\"preferences\":null,\"followAlong\":null}"
        val VALID_PHASE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"timed_work\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855\"},\"payload\":{\"variant\":\"warmup\",\"compositionVersion\":2,\"compositionBlockId\":\"block\",\"${"timelineStage" + "Id"}\":\"block:warmup\",\"timelineStageKind\":\"warmup\",\"stageGroupId\":\"block:warmup\",\"targetId\":\"block:warmup:target\",\"targetKind\":\"warmup\",\"roundIndex0\":null,\"stageGroupIndex0\":null,\"targetIndex0\":0,\"stageInstanceIndex0\":0,\"${"targetInstance" + "Index0"}\":0,\"stepIndex0\":0}}"
        const val VALID_ANALYSIS_CONFIG =
            "{\"analysisConfigContractVersion\":1,\"sampleValidityCapMs\":2500,\"sampleIntervalContractVersion\":1,\"partialLowerBoundBasisPoints\":5000,\"phaseConclusionBasisPoints\":7000,\"normalBasisPoints\":8000,\"coverageThresholdRule\":\"checked_integer_cross_multiply\",\"coverageBasisPointsRule\":\"floor_integer_ratio\",\"displayPercentRule\":\"floor_basis_points_div_100\",\"weightedAverageRule\":\"checked_integer_time_integral\",\"averageDisplayRule\":\"positive_integer_half_up\",\"zeroCoveredRule\":\"null_integral_and_average\",\"observedMaxRule\":\"eligible_canonical_point_first_tie\",\"zoneAttributionContractVersion\":1,\"zoneAttributionRule\":\"checked_cross_multiply_six_zones\",\"statusProjectionContractVersion\":1,\"durationPartitionContractVersion\":1}"
        const val VALID_ZONE_SNAPSHOT_200 =
            "{\"zoneSnapshotContractVersion\":1,\"unit\":\"bpm\",\"effectiveMaxBpm\":200,\"effectiveMaxSource\":\"personal_max\",\"zones\":[{\"zoneId\":\"below_50\",\"lowerBoundBasisPointsInclusive\":null,\"upperBoundBasisPointsExclusive\":5000},{\"zoneId\":\"from_50_to_60\",\"lowerBoundBasisPointsInclusive\":5000,\"upperBoundBasisPointsExclusive\":6000},{\"zoneId\":\"from_60_to_70\",\"lowerBoundBasisPointsInclusive\":6000,\"upperBoundBasisPointsExclusive\":7000},{\"zoneId\":\"from_70_to_80\",\"lowerBoundBasisPointsInclusive\":7000,\"upperBoundBasisPointsExclusive\":8000},{\"zoneId\":\"from_80_to_90\",\"lowerBoundBasisPointsInclusive\":8000,\"upperBoundBasisPointsExclusive\":9000},{\"zoneId\":\"at_or_above_90\",\"lowerBoundBasisPointsInclusive\":9000,\"upperBoundBasisPointsExclusive\":null}]}"
        const val VALID_ZONE_DURATIONS_120_OF_200 =
            "{\"zoneDurationsContractVersion\":1,\"below50DurationMs\":0,\"from50To60DurationMs\":0,\"from60To70DurationMs\":100,\"from70To80DurationMs\":0,\"from80To90DurationMs\":0,\"atOrAbove90DurationMs\":0}"
        const val VALID_PHASE_AGGREGATES =
            "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[{\"phaseSequence\":0,\"phaseKind\":\"timed_work\",\"eligibleDurationMs\":100,\"coveredDurationMs\":100,\"coverageBasisPoints\":10000,\"coverageStatus\":\"normal\",\"conclusionEligible\":true,\"weightedBpmMs\":12000,\"observedAvgBpm\":120,\"observedMaxBpm\":120,\"highestOffsetMs\":0,\"highestMutationSequence\":0,\"highestSampleSequence\":0}]}"
        const val VALID_DURATION_BREAKDOWN =
            "{\"durationBreakdownContractVersion\":1,\"canonicalSessionDurationMs\":100,\"recordingWindowDurationMs\":100,\"notRequestedBeforeRecordingStartMs\":0,\"intentAxis\":{\"expectedRecordingDurationMs\":100,\"userExcludedDurationMs\":0,\"userTurnedOffDurationMs\":0,\"userOptedOutDurationMs\":0,\"userDisconnectedSuppressRecoveryDurationMs\":0},\"phaseAxis\":{\"primaryEligibleDurationMs\":100,\"phaseExcludedDurationMs\":0,\"strengthPrepareExcludedDurationMs\":0,\"pausedExcludedDurationMs\":0},\"primaryAnalysisPartition\":{\"primaryEligibleDurationMs\":100,\"eligibleCoveredDurationMs\":100,\"eligibleUncoveredDurationMs\":0},\"deviceStateDurations\":{\"not_observing\":0,\"no_source_selected\":0,\"permission_required\":0,\"bluetooth_unavailable\":0,\"searching\":0,\"connecting\":0,\"waiting_first_sample\":0,\"live\":100,\"stale\":0,\"reconnecting\":0,\"disconnected\":0,\"technical_failure\":0},\"deviceReasonDurations\":{\"initial_acquisition\":0,\"automatic_recovery\":0,\"source_not_selected\":0,\"source_unavailable\":0,\"permission_missing\":0,\"permission_revoked\":0,\"bluetooth_off\":0,\"platform_unavailable\":0,\"first_sample_timeout\":0,\"sample_stale_timeout\":0,\"unexpected_disconnect\":0,\"connection_timeout\":0,\"measurement_stream_unavailable\":0,\"platform_failure\":0},\"orthogonalityContract\":{\"contractVersion\":1,\"rule\":\"primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum\"}}"
        const val VALID_QUALITY_REASONS =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[],\"phaseReasons\":[]}"
    }
}
