package com.liujyks.trainflow.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.database.mapping.StorageMappingStrategy
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrainFlowDatabaseSmokeTest {
    private lateinit var database: TrainFlowDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TrainFlowDatabase::class.java
        ).allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun createsEmptyRoomDatabaseSkeleton() = runBlocking {
        assertEquals(0, database.exerciseDao().count())
        assertEquals(0, database.workoutPlanDao().count())
        assertEquals(0, database.workoutSessionDao().sessionCount())
        assertEquals(0, database.workoutSessionDao().stepRecordCount())
        assertEquals(0, database.workoutSessionDao().timedRestExtensionRecordCount())
        assertEquals(0, database.workoutSessionDao().strengthSetRecordCount())
        assertEquals(0, database.recoveryDao().areaCount())
        assertEquals(0, database.recoveryDao().recommendationCount())
    }

    @Test
    fun documentsJsonBackedPlanSnapshotBoundary() {
        assertTrue(
            StorageMappingStrategy.jsonBackedColumns.contains(
                "workout_sessions.plan_snapshot_json"
            )
        )
    }

    @Test
    fun freshDatabaseContainsCanonicalTimelineAndHeartRateTables() {
        val canonicalTables = setOf(
            "workout_phase_intervals",
            "heart_rate_recordings",
            "heart_rate_acquisition_intervals",
            "heart_rate_samples",
            "heart_rate_analysis_snapshots"
        )
        val actualTables = mutableSetOf<String>()

        database.openHelper.readableDatabase
            .query("SELECT name FROM sqlite_master WHERE type='table'")
            .use { cursor ->
                while (cursor.moveToNext()) actualTables += cursor.getString(0)
            }

        assertTrue(actualTables.containsAll(canonicalTables))
    }

    @Test
    fun freshVersionFiveEnforcesRoomPhysicalConstraintsAndPureValidatorsOwnSemanticRules() {
        val db = database.openHelper.writableDatabase
        db.execSQL(
            """
            INSERT INTO workout_sessions(
                id, mode, status, plan_snapshot_json
            ) VALUES('fresh-session', 'timed', 'active', '{}')
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO workout_phase_intervals(
                id, session_id, sequence, start_offset_ms, end_offset_ms,
                start_mutation_sequence, end_mutation_sequence, open_marker,
                phase_kind, phase_identity_json
            ) VALUES(
                'semantic-invalid-phase', 'fresh-session', 0, 0, 0,
                0, 0, NULL, 'future_kind', '{}'
            )
            """.trimIndent()
        )
        assertTrue(
            PhaseIdentityV1Validator.validateStructure("{}") is CanonicalValidationResult.Invalid
        )
        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL(
                """
                INSERT INTO workout_phase_intervals(
                    id, session_id, sequence, start_offset_ms, end_offset_ms,
                    start_mutation_sequence, end_mutation_sequence, open_marker,
                    phase_kind, phase_identity_json
                ) VALUES(
                    'duplicate-sequence', 'fresh-session', 0, 1, 2,
                    1, 2, NULL, 'timed_work', '{}'
                )
                """.trimIndent()
            )
        }
    }

    @Test
    fun canonicalDaoReadsRelationsAndSamplesInExplicitCanonicalOrder() = runBlocking {
        val planSnapshot =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"DAO graph\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"composition\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":0,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[{\"id\":\"group\",\"order\":0,\"name\":\"Display\",\"colorHex\":\"#111111\",\"targets\":[{\"id\":\"work\",\"order\":0,\"name\":\"Work\",\"kind\":\"action\",\"durationSec\":50,\"colorHex\":\"#222222\",\"autoAdvance\":true},{\"id\":\"rest\",\"order\":1,\"name\":\"Rest\",\"kind\":\"rest\",\"durationSec\":50,\"colorHex\":\"#333333\",\"autoAdvance\":true}]}]}],\"preferences\":null,\"followAlong\":null}"
        val projection =
            "{\"signatureInputContractVersion\":1,\"mode\":\"timed\",\"blocks\":[{\"blockId\":\"composition\",\"blockKind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":0,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[{\"stageGroupId\":\"group\",\"order\":0,\"targets\":[{\"targetId\":\"work\",\"order\":0,\"targetKind\":\"action\",\"durationSec\":50,\"autoAdvance\":true},{\"targetId\":\"rest\",\"order\":1,\"targetKind\":\"rest\",\"durationSec\":50,\"autoAdvance\":true}]}]}]}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(projection.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        val stageIdentityKey = "timelineStage" + "Id"
        val targetOrdinalKey = "targetInstance" + "Index0"
        fun phaseIdentity(
            phaseKind: String,
            variant: String,
            targetId: String,
            targetKind: String,
            targetIndex0: Int
        ): String =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"$phaseKind\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"$digest\"},\"payload\":{\"variant\":\"$variant\",\"compositionVersion\":2,\"compositionBlockId\":\"composition\",\"$stageIdentityKey\":\"composition:r1:g1:group\",\"timelineStageKind\":\"stage_group\",\"stageGroupId\":\"group\",\"targetId\":\"$targetId\",\"targetKind\":\"$targetKind\",\"roundIndex0\":0,\"stageGroupIndex0\":0,\"targetIndex0\":$targetIndex0,\"stageInstanceIndex0\":0,\"$targetOrdinalKey\":$targetIndex0,\"stepIndex0\":$targetIndex0}}"
        database.workoutSessionDao().insertSession(
            WorkoutSessionEntity(
                id = "canonical-session",
                mode = "timed",
                status = "completed",
                planSnapshotJson = planSnapshot,
                timelineVersion = 1,
                lastDurableOffsetMs = 100,
                lastMutationSequence = 3,
                trustedEndOffsetMs = 100,
                terminalReason = "completed",
                displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson =
                    "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
            )
        )
        val dao = database.canonicalTimelineHeartRateDao()
        listOf(
            WorkoutPhaseIntervalEntity(
                id = "phase-1",
                sessionId = "canonical-session",
                sequence = 1,
                startOffsetMs = 50,
                endOffsetMs = 100,
                startMutationSequence = 1,
                endMutationSequence = 3,
                openMarker = null,
                phaseKind = "timed_rest",
                phaseIdentityJson = phaseIdentity("timed_rest", "stage_group_rest", "rest", "rest", 1)
            ),
            WorkoutPhaseIntervalEntity(
                id = "phase-0",
                sessionId = "canonical-session",
                sequence = 0,
                startOffsetMs = 0,
                endOffsetMs = 50,
                startMutationSequence = 0,
                endMutationSequence = 1,
                openMarker = null,
                phaseKind = "timed_work",
                phaseIdentityJson = phaseIdentity("timed_work", "stage_group_action", "work", "action", 0)
            )
        ).forEach { phase -> dao.insertPhaseInterval(phase) }
        dao.insertRecording(
            HeartRateRecordingEntity(
                recordingId = "recording",
                sessionId = "canonical-session",
                status = "terminal",
                startedOffsetMs = 0,
                startedMutationSequence = 0,
                endedOffsetMs = 100,
                endedMutationSequence = 3,
                sourceContractVersion = 1,
                sourceKind = "ble_hrs",
                acquisitionContractVersion = 1,
                parameterSnapshotVersion = 1,
                originalAnalysisVersion = 1
            )
        )
        listOf(
            HeartRateAcquisitionIntervalEntity(
                id = "acquisition-1",
                recordingId = "recording",
                sequence = 1,
                startOffsetMs = 50,
                endOffsetMs = 100,
                startMutationSequence = 1,
                endMutationSequence = 3,
                openMarker = null,
                recordingIntent = "expected_recording",
                intentReason = null,
                deviceState = "live",
                deviceReason = null
            ),
            HeartRateAcquisitionIntervalEntity(
                id = "acquisition-0",
                recordingId = "recording",
                sequence = 0,
                startOffsetMs = 0,
                endOffsetMs = 50,
                startMutationSequence = 0,
                endMutationSequence = 1,
                openMarker = null,
                recordingIntent = "expected_recording",
                intentReason = null,
                deviceState = "live",
                deviceReason = null
            )
        ).forEach { acquisition -> dao.insertAcquisitionInterval(acquisition) }
        listOf(
            HeartRateSampleEntity("recording", 1, 50, 2, 120),
            HeartRateSampleEntity("recording", 0, 0, 0, 120)
        ).forEach { sample -> dao.insertSample(sample) }
        dao.insertAnalysisSnapshot(
            HeartRateAnalysisSnapshotEntity(
                recordingId = "recording",
                analysisVersion = 1,
                createdAt = "2026-08-30T00:00:00Z",
                inputLastMutationSequence = 3,
                sampleStatus = "primary_points_available",
                coverageStatus = "normal",
                zoneStatus = "unavailable_no_effective_max",
                canonicalSampleCount = 2,
                primaryPointSampleCount = 2,
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

        assertEquals(
            listOf(0L, 1L),
            dao.samplesInCanonicalOrder("recording").map { sample -> sample.sampleSequence }
        )
        val graph = requireNotNull(dao.canonicalGraphRows("canonical-session"))
        assertEquals(listOf("phase-0", "phase-1"), graph.phases.map { phase -> phase.id })
        assertEquals(1, graph.recordings.size)
        assertEquals("recording", graph.recordings.single().recording.recordingId)
        assertEquals(
            listOf("acquisition-0", "acquisition-1"),
            graph.recordings.single().acquisitions.map { acquisition -> acquisition.id }
        )
        assertEquals(
            listOf(0L, 1L),
            graph.recordings.single().samples.map { sample -> sample.sampleSequence }
        )
        assertEquals(listOf(1), graph.recordings.single().snapshots.map { it.analysisVersion })
        val recordingRows = graph.recordings.single()
        assertTrue(
            CanonicalSessionGraphV1Validator.validate(
                CanonicalSessionGraphV1(
                    session = graph.session,
                    phases = graph.phases,
                    recording = recordingRows.recording,
                    acquisitions = recordingRows.acquisitions,
                    samples = recordingRows.samples,
                    snapshots = recordingRows.snapshots
                )
            ) is CanonicalValidationResult.Valid
        )
    }

    private companion object {
        const val VALID_ANALYSIS_CONFIG =
            "{\"analysisConfigContractVersion\":1,\"sampleValidityCapMs\":2500,\"sampleIntervalContractVersion\":1,\"partialLowerBoundBasisPoints\":5000,\"phaseConclusionBasisPoints\":7000,\"normalBasisPoints\":8000,\"coverageThresholdRule\":\"checked_integer_cross_multiply\",\"coverageBasisPointsRule\":\"floor_integer_ratio\",\"displayPercentRule\":\"floor_basis_points_div_100\",\"weightedAverageRule\":\"checked_integer_time_integral\",\"averageDisplayRule\":\"positive_integer_half_up\",\"zeroCoveredRule\":\"null_integral_and_average\",\"observedMaxRule\":\"eligible_canonical_point_first_tie\",\"zoneAttributionContractVersion\":1,\"zoneAttributionRule\":\"checked_cross_multiply_six_zones\",\"statusProjectionContractVersion\":1,\"durationPartitionContractVersion\":1}"
        const val VALID_PHASE_AGGREGATES =
            "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[{\"phaseSequence\":0,\"phaseKind\":\"timed_work\",\"eligibleDurationMs\":50,\"coveredDurationMs\":50,\"coverageBasisPoints\":10000,\"coverageStatus\":\"normal\",\"conclusionEligible\":true,\"weightedBpmMs\":6000,\"observedAvgBpm\":120,\"observedMaxBpm\":120,\"highestOffsetMs\":0,\"highestMutationSequence\":0,\"highestSampleSequence\":0},{\"phaseSequence\":1,\"phaseKind\":\"timed_rest\",\"eligibleDurationMs\":50,\"coveredDurationMs\":50,\"coverageBasisPoints\":10000,\"coverageStatus\":\"normal\",\"conclusionEligible\":true,\"weightedBpmMs\":6000,\"observedAvgBpm\":120,\"observedMaxBpm\":120,\"highestOffsetMs\":50,\"highestMutationSequence\":2,\"highestSampleSequence\":1}]}"
        const val VALID_DURATION_BREAKDOWN =
            "{\"durationBreakdownContractVersion\":1,\"canonicalSessionDurationMs\":100,\"recordingWindowDurationMs\":100,\"notRequestedBeforeRecordingStartMs\":0,\"intentAxis\":{\"expectedRecordingDurationMs\":100,\"userExcludedDurationMs\":0,\"userTurnedOffDurationMs\":0,\"userOptedOutDurationMs\":0,\"userDisconnectedSuppressRecoveryDurationMs\":0},\"phaseAxis\":{\"primaryEligibleDurationMs\":100,\"phaseExcludedDurationMs\":0,\"strengthPrepareExcludedDurationMs\":0,\"pausedExcludedDurationMs\":0},\"primaryAnalysisPartition\":{\"primaryEligibleDurationMs\":100,\"eligibleCoveredDurationMs\":100,\"eligibleUncoveredDurationMs\":0},\"deviceStateDurations\":{\"not_observing\":0,\"no_source_selected\":0,\"permission_required\":0,\"bluetooth_unavailable\":0,\"searching\":0,\"connecting\":0,\"waiting_first_sample\":0,\"live\":100,\"stale\":0,\"reconnecting\":0,\"disconnected\":0,\"technical_failure\":0},\"deviceReasonDurations\":{\"initial_acquisition\":0,\"automatic_recovery\":0,\"source_not_selected\":0,\"source_unavailable\":0,\"permission_missing\":0,\"permission_revoked\":0,\"bluetooth_off\":0,\"platform_unavailable\":0,\"first_sample_timeout\":0,\"sample_stale_timeout\":0,\"unexpected_disconnect\":0,\"connection_timeout\":0,\"measurement_stream_unavailable\":0,\"platform_failure\":0},\"orthogonalityContract\":{\"contractVersion\":1,\"rule\":\"primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum\"}}"
        const val VALID_QUALITY_REASONS =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[],\"phaseReasons\":[]}"
    }
}
