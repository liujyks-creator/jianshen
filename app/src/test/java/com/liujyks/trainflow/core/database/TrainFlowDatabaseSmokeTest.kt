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
        database.workoutSessionDao().insertSession(
            WorkoutSessionEntity(
                id = "canonical-session",
                mode = "timed",
                status = "active",
                planSnapshotJson = "{}",
                timelineVersion = 1,
                lastDurableOffsetMs = 100,
                lastMutationSequence = 3,
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
                phaseIdentityJson = "{}"
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
                phaseIdentityJson = "{}"
            )
        ).forEach { phase -> dao.insertPhaseInterval(phase) }
        dao.insertRecording(
            HeartRateRecordingEntity(
                recordingId = "recording",
                sessionId = "canonical-session",
                status = "active",
                startedOffsetMs = 0,
                startedMutationSequence = 0,
                endedOffsetMs = null,
                endedMutationSequence = null,
                sourceContractVersion = 1,
                sourceKind = "ble_hrs",
                acquisitionContractVersion = 1,
                parameterSnapshotVersion = 1
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
            HeartRateSampleEntity("recording", 0, 10, 3, 130),
            HeartRateSampleEntity("recording", 1, 5, 2, 120),
            HeartRateSampleEntity("recording", 2, 10, 2, 125)
        ).forEach { sample -> dao.insertSample(sample) }

        assertEquals(
            listOf(1L, 2L, 0L),
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
            listOf(1L, 2L, 0L),
            graph.recordings.single().samples.map { sample -> sample.sampleSequence }
        )
        assertTrue(graph.recordings.single().snapshots.isEmpty())
    }
}
