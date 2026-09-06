package com.liujyks.trainflow.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.SessionStepRecord
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthExerciseTarget
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StrengthSetRecord
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedRestExtensionRecord
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.WorkoutSession
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutSessionRepositoryTest {
    private lateinit var database: TrainFlowDatabase
    private lateinit var repository: WorkoutSessionRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TrainFlowDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = WorkoutSessionRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun timeMetadataWrittenByRepositorySurvivesDatabaseCloseAndReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database.close()
        database = TrainFlowDatabase.create(context)
        repository = WorkoutSessionRepository(database)
        val expected = listOf(
            listOf(null, null, null, null),
            listOf("2026-01-01", "UTC", "0", "1"),
            listOf("2026-01-02", "Asia/Kathmandu", "20700", "1")
        )
        expected.forEachIndexed { index, values ->
            repository.upsertSession(
                timedSession("time-$index", SessionStatus.COMPLETED, 75, 60, 15).copy(
                    startedAt = "2026-01-01T20:30:00Z",
                    startLocalDate = values[0],
                    startZoneId = values[1],
                    startUtcOffsetSeconds = values[2]?.toLong(),
                    timeMetadataSourceContractVersion = values[3]?.toLong()
                )
            )
        }
        database.close()
        database = TrainFlowDatabase.create(context)
        repository = WorkoutSessionRepository(database)

        val entities = database.workoutSessionDao().sessionsForRecorderGate()
        assertEquals(3, entities.size)
        entities.forEachIndexed { index, entity ->
            assertEquals("time-$index", entity.id)
            assertEquals(expected[index], listOf(
                entity.startLocalDate, entity.startZoneId,
                entity.startUtcOffsetSeconds?.toString(),
                entity.timeMetadataSourceContractVersion?.toString()
            ))
        }
        val sessions = repository.getSessions().sortedBy { it.id }
        assertEquals(3, sessions.size)
        sessions.forEachIndexed { index, session ->
            assertEquals(expected[index], listOf(
                session.startLocalDate, session.startZoneId,
                session.startUtcOffsetSeconds?.toString(),
                session.timeMetadataSourceContractVersion?.toString()
            ))
        }
    }

    @Test
    fun entityTimeMetadataReadsThroughRepositoryAfterDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database.close()
        database = TrainFlowDatabase.create(context)
        val expected = listOf(
            listOf(null, null, null, null),
            listOf("2026-01-01", "UTC", "0", "1"),
            listOf("2026-01-02", "Asia/Kathmandu", "20700", "1")
        )
        expected.forEachIndexed { index, values ->
            assertTrue(database.workoutSessionDao().insertSession(WorkoutSessionEntity(
                id = "entity-$index", mode = "timed", status = "completed",
                planSnapshotJson = "{\"title\":\"Old plan\",\"mode\":\"timed\",\"blocks\":[]}",
                startedAt = "2026-01-01T20:30:00Z",
                startLocalDate = values[0], startZoneId = values[1],
                startUtcOffsetSeconds = values[2]?.toLong(),
                timeMetadataSourceContractVersion = values[3]?.toLong()
            )) != -1L)
        }
        database.close()
        database = TrainFlowDatabase.create(context)
        repository = WorkoutSessionRepository(database)
        val sessions = repository.getSessions().sortedBy { it.id }
        assertEquals(3, sessions.size)
        sessions.forEachIndexed { index, session ->
            assertEquals("entity-$index", session.id)
            assertEquals(expected[index], listOf(
                session.startLocalDate, session.startZoneId,
                session.startUtcOffsetSeconds?.toString(),
                session.timeMetadataSourceContractVersion?.toString()
            ))
        }
    }

    @Test
    fun legacyUpdatePreservesFrozenTimeMetadataAndUpdatesExecutionFields() = runBlocking {
        val original = timedSession("frozen-time", SessionStatus.ACTIVE, 75, 60, 15).copy(
            startLocalDate = "2026-01-02", startZoneId = "Asia/Kathmandu",
            startUtcOffsetSeconds = 20700L, timeMetadataSourceContractVersion = 1L
        )
        repository.upsertSession(original)
        repository.upsertSession(original.copy(
            status = SessionStatus.COMPLETED, endedAt = "2026-01-02T00:00:00Z",
            totalElapsedSec = 90, effectiveElapsedSec = 70, pausedElapsedSec = 20,
            stepHistory = original.stepHistory.take(1),
            startLocalDate = "2026-01-01", startZoneId = "UTC",
            startUtcOffsetSeconds = 0L, timeMetadataSourceContractVersion = null
        ))
        val entity = database.workoutSessionDao().sessionsForRecorderGate().single()
        assertEquals("2026-01-02", entity.startLocalDate)
        assertEquals("Asia/Kathmandu", entity.startZoneId)
        assertEquals(20700L, entity.startUtcOffsetSeconds)
        assertEquals(1L, entity.timeMetadataSourceContractVersion)
        val saved = repository.getSessions().single()
        assertEquals("2026-01-02", saved.startLocalDate)
        assertEquals("Asia/Kathmandu", saved.startZoneId)
        assertEquals(20700L, saved.startUtcOffsetSeconds)
        assertEquals(1L, saved.timeMetadataSourceContractVersion)
        assertEquals(SessionStatus.COMPLETED, saved.status)
        assertEquals("2026-01-02T00:00:00Z", saved.endedAt)
        assertEquals(90, saved.totalElapsedSec)
        assertEquals(70, saved.effectiveElapsedSec)
        assertEquals(20, saved.pausedElapsedSec)
        assertEquals(original.stepHistory.take(1), saved.stepHistory)
    }

    @Test
    fun completedTimedSessionWritesAndReadsElapsedBreakdown() = runBlocking {
        repository.upsertSession(
            timedSession(
                id = "timed-completed",
                status = SessionStatus.COMPLETED,
                totalElapsedSec = 75,
                effectiveElapsedSec = 60,
                pausedElapsedSec = 15
            )
        )

        val sessions = repository.getSessions()

        assertEquals(1, sessions.size)
        val session = sessions.single()
        assertEquals("timed-completed", session.id)
        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(75, session.totalElapsedSec)
        assertEquals(60, session.effectiveElapsedSec)
        assertEquals(15, session.pausedElapsedSec)
        assertEquals(2, session.stepHistory.size)
        assertEquals(2, database.workoutSessionDao().stepRecordCount())
    }

    @Test
    fun timedPlanSnapshotBlocksRoundTripThroughRoomJson() = runBlocking {
        repository.upsertSession(
            timedSession(
                id = "timed-snapshot",
                status = SessionStatus.COMPLETED,
                totalElapsedSec = 75,
                effectiveElapsedSec = 60,
                pausedElapsedSec = 15
            )
        )

        val snapshot = repository.getSessions().single().planSnapshot
        val block = snapshot.blocks.single() as TimedCircuitBlock
        val item = block.items.single()

        assertEquals("plan-timed", snapshot.planId)
        assertEquals(WorkoutMode.TIMED, snapshot.mode)
        assertEquals("interval-main", block.id)
        assertEquals(2, block.rounds)
        assertEquals(30, block.restBetweenRoundsSec)
        assertEquals("work-stage", item.id)
        assertEquals(TimedStageType.WORK, item.stageType)
        assertEquals("work", item.iconKey)
        assertEquals("#F26B4F", item.colorHex)
        assertEquals(30, item.workDurationSec)
        assertEquals(10, item.restAfterSec)
        assertEquals(4, snapshot.preferences?.cueSettings?.actionEnding?.thresholdSec)
    }

    @Test
    fun timedRestExtensionRecordsRoundTripThroughRoom() = runBlocking {
        repository.upsertSession(
            timedSession(
                id = "timed-rest-extended",
                status = SessionStatus.COMPLETED,
                totalElapsedSec = 105,
                effectiveElapsedSec = 90,
                pausedElapsedSec = 0,
                restExtensionRecords = listOf(
                    TimedRestExtensionRecord(
                        id = "extension-1",
                        stepId = "interval-r2-work-stage-rest",
                        stepIndex = 3,
                        roundIndex = 2,
                        restStageId = "work-stage",
                        restStageTitle = "Rest",
                        previousStageId = "work-stage",
                        previousStageTitle = "Work",
                        addedSec = 15,
                        plannedRestSec = 10,
                        restElapsedBeforeExtensionSec = 4,
                        extensionAtRemainingSec = 6,
                        cumulativeExtraRestSec = 15,
                        eventElapsedSec = 64
                    ),
                    TimedRestExtensionRecord(
                        id = "extension-2",
                        stepId = "interval-r2-work-stage-rest",
                        stepIndex = 3,
                        roundIndex = 2,
                        restStageId = "work-stage",
                        restStageTitle = "Rest",
                        previousStageId = "work-stage",
                        previousStageTitle = "Work",
                        addedSec = 15,
                        plannedRestSec = 10,
                        restElapsedBeforeExtensionSec = 4,
                        extensionAtRemainingSec = 21,
                        cumulativeExtraRestSec = 30,
                        eventElapsedSec = 64
                    )
                )
            )
        )

        val session = repository.getSessions().single()
        val records = session.timedRestExtensionRecords

        assertEquals(2, database.workoutSessionDao().timedRestExtensionRecordCount())
        assertEquals(30, records.sumOf { record -> record.addedSec })
        assertEquals(listOf(15, 30), records.map { record -> record.cumulativeExtraRestSec })
        assertEquals(2, records.first().roundIndex)
        assertEquals(3, records.first().stepIndex)
        assertEquals("interval-r2-work-stage-rest", records.first().stepId)
        assertEquals("work-stage", records.first().previousStageId)
        assertEquals(10, records.first().plannedRestSec)
        assertEquals(4, records.first().restElapsedBeforeExtensionSec)
        assertEquals(6, records.first().extensionAtRemainingSec)
    }

    @Test
    fun timedRestExtensionRecordsWithSameElapsedSecondSortByCumulativeExtraRest() = runBlocking {
        val sameSecondRecords = (1..12).map { index ->
            TimedRestExtensionRecord(
                id = "extension-$index",
                stepId = "interval-r2-work-stage-rest",
                stepIndex = 3,
                roundIndex = 2,
                restStageId = "work-stage",
                restStageTitle = "Rest",
                previousStageId = "work-stage",
                previousStageTitle = "Work",
                addedSec = 15,
                plannedRestSec = 10,
                restElapsedBeforeExtensionSec = 4,
                extensionAtRemainingSec = 6 + ((index - 1) * 15),
                cumulativeExtraRestSec = index * 15,
                eventElapsedSec = 64
            )
        }

        repository.upsertSession(
            timedSession(
                id = "timed-rest-extended-many",
                status = SessionStatus.COMPLETED,
                totalElapsedSec = 255,
                effectiveElapsedSec = 240,
                pausedElapsedSec = 0,
                restExtensionRecords = sameSecondRecords
            )
        )

        val records = repository.getSessions().single().timedRestExtensionRecords

        assertEquals(12, database.workoutSessionDao().timedRestExtensionRecordCount())
        assertEquals(
            (1..12).map { index -> index * 15 },
            records.map { record -> record.cumulativeExtraRestSec }
        )
        assertEquals(
            (1..12).map { index -> "extension-$index" },
            records.map { record -> record.id }
        )
    }

    @Test
    fun strengthSessionPersistsOnlyConfirmedActualRecords() = runBlocking {
        repository.upsertSession(
            strengthSession(
                id = "strength-completed",
                status = SessionStatus.COMPLETED,
                records = listOf(
                    StrengthSetRecord(
                        id = "confirmed-set",
                        exerciseId = "barbell-bench-press",
                        sourceSetPlanId = "bench-working-1",
                        setOrder = 1,
                        setKind = StrengthSetKind.WORKING,
                        plannedWeight = WeightValue(60.0, WeightUnit.KG),
                        plannedRepTarget = RepTarget.Range(8, 12),
                        actualWeight = WeightValue(62.5, WeightUnit.KG),
                        actualReps = 8,
                        activeDurationSec = 41,
                        actualRestAfterSec = 90
                    )
                )
            )
        )

        val session = repository.getSessions().single()
        val record = session.strengthSetRecords.single()

        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals("confirmed-set", record.id)
        assertEquals(62.5, requireNotNull(record.actualWeight).value, 0.0)
        assertEquals(8, record.actualReps)
        assertEquals(41, record.activeDurationSec)
        assertEquals(90, record.actualRestAfterSec)
        assertEquals(1, database.workoutSessionDao().strengthSetRecordCount())
    }

    @Test
    fun strengthPlanSnapshotBlocksRoundTripThroughRoomJson() = runBlocking {
        repository.upsertSession(
            strengthSession(
                id = "strength-snapshot",
                status = SessionStatus.COMPLETED,
                records = listOf(
                    StrengthSetRecord(
                        id = "confirmed-set",
                        exerciseId = "barbell-bench-press",
                        sourceSetPlanId = "bench-working-1",
                        setOrder = 1,
                        setKind = StrengthSetKind.WORKING,
                        actualWeight = WeightValue(60.0, WeightUnit.KG),
                        actualReps = 8
                    )
                )
            )
        )

        val snapshot = repository.getSessions().single().planSnapshot
        val block = snapshot.blocks.single() as StrengthExerciseBlock
        val firstSet = block.sets.first()

        assertEquals("plan-strength", snapshot.planId)
        assertEquals("barbell-bench-press", block.exerciseId)
        assertEquals(WeightValue(60.0, WeightUnit.KG), block.target?.weight)
        assertEquals(RepTarget.Range(8, 12), block.target?.repTarget)
        assertEquals(2, block.sets.size)
        assertEquals("bench-working-1", firstSet.id)
        assertEquals(StrengthSetKind.WORKING, firstSet.kind)
        assertEquals(90, firstSet.restAfterSec)
        assertEquals(listOf("incline-push-up"), block.substitutions)
    }

    @Test
    fun abandonedSessionStatusRoundTripsWithoutBeingPromotedToCompleted() = runBlocking {
        repository.upsertSession(
            timedSession(
                id = "timed-abandoned",
                status = SessionStatus.ABANDONED,
                totalElapsedSec = 25,
                effectiveElapsedSec = 20,
                pausedElapsedSec = 5
            )
        )

        val session = repository.getSessions().single()

        assertEquals(SessionStatus.ABANDONED, session.status)
        assertNotNull(session.endedAt)
    }

    @Test
    fun legacyRewriteCannotReplaceCanonicalParentOrDeleteAnyExistingChildren() = runBlocking {
        val original = timedSession(
            id = "canonical-protected",
            status = SessionStatus.COMPLETED,
            totalElapsedSec = 75,
            effectiveElapsedSec = 60,
            pausedElapsedSec = 15
        ).copy(
            startLocalDate = "2026-06-07", startZoneId = "UTC",
            startUtcOffsetSeconds = 0L, timeMetadataSourceContractVersion = 1L
        )
        repository.upsertSession(original)
        val sql = database.openHelper.writableDatabase
        sql.execSQL(
            """
            UPDATE workout_sessions
            SET timeline_version=1,
                last_durable_offset_ms=100,
                last_mutation_sequence=4,
                trusted_end_offset_ms=100,
                terminal_reason='completed',
                display_metadata_contract_version=1,
                session_display_metadata_json='{"displayMetadataContractVersion":1,"entries":[]}'
            WHERE id='canonical-protected'
            """.trimIndent()
        )
        sql.execSQL(
            """
            INSERT INTO workout_phase_intervals VALUES(
                'phase', 'canonical-protected', 0, 0, 100, 0, 4, NULL, 'timed_work', '{}'
            )
            """.trimIndent()
        )
        sql.execSQL(
            """
            INSERT INTO heart_rate_recordings VALUES(
                'recording', 'canonical-protected', 'terminal', 0, 0, 100, 4,
                1, 'ble_hrs', 1, 1, NULL, NULL, NULL, NULL, NULL, NULL, 1
            )
            """.trimIndent()
        )
        sql.execSQL(
            """
            INSERT INTO heart_rate_acquisition_intervals VALUES(
                'acquisition', 'recording', 0, 0, 100, 0, 4, NULL,
                'expected_recording', NULL, 'live', NULL
            )
            """.trimIndent()
        )
        sql.execSQL("INSERT INTO heart_rate_samples VALUES('recording', 0, 10, 1, 120)")
        sql.execSQL(
            """
            INSERT INTO heart_rate_analysis_snapshots VALUES(
                'recording', 1, '2026-08-25T00:00:00Z', 4,
                'primary_points_available', 'normal', 'unavailable_no_effective_max',
                1, 1, 100, 100, 10000, 12000, 120, 120, 10, 1, 0,
                '{}', NULL, '{}', '{}', '{}'
            )
            """.trimIndent()
        )

        val tables = listOf("workout_sessions", "session_step_records", "strength_set_records",
            "timed_rest_extension_records", "workout_phase_intervals", "heart_rate_recordings",
            "heart_rate_acquisition_intervals", "heart_rate_samples", "heart_rate_analysis_snapshots")
        val before = tables.associateWith { table ->
            sql.query("SELECT * FROM $table ORDER BY 1, 2").use { cursor ->
                buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getType(it) to cursor.getString(it) }) }
            }
        }
        val rewrite = runCatching {
            repository.upsertSession(original.copy(
                status = SessionStatus.ABANDONED, stepHistory = emptyList(),
                startLocalDate = null, startZoneId = null,
                startUtcOffsetSeconds = null, timeMetadataSourceContractVersion = null
            ))
        }

        assertTrue(rewrite.exceptionOrNull() is IllegalStateException)
        assertEquals("Legacy workout-session write rejected for canonical session canonical-protected",
            rewrite.exceptionOrNull()?.message)
        tables.forEach { table ->
            val after = sql.query("SELECT * FROM $table ORDER BY 1, 2").use { cursor ->
                buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getType(it) to cursor.getString(it) }) }
            }
            assertEquals("rejected transaction preserves every column and child: $table", before.getValue(table), after)
        }
        val entity = database.workoutSessionDao().sessionsForRecorderGate().single()
        assertEquals("2026-06-07", entity.startLocalDate)
        assertEquals("UTC", entity.startZoneId)
        assertEquals(0L, entity.startUtcOffsetSeconds)
        assertEquals(1L, entity.timeMetadataSourceContractVersion)
        val header = sql.query(
            """
            SELECT status, timeline_version, last_durable_offset_ms,
                   last_mutation_sequence, trusted_end_offset_ms, terminal_reason,
                   display_metadata_contract_version, session_display_metadata_json
            FROM workout_sessions WHERE id='canonical-protected'
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            (0 until cursor.columnCount).map { index -> cursor.getString(index) }
        }
        assertEquals(
            listOf(
                "completed", "1", "100", "4", "100", "completed", "1",
                "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
            ),
            header
        )
        val canonicalDao = database.canonicalTimelineHeartRateDao()
        assertEquals(1, canonicalDao.phaseIntervalCount())
        assertEquals(1, canonicalDao.recordingCount())
        assertEquals(1, canonicalDao.acquisitionIntervalCount())
        assertEquals(1, canonicalDao.sampleCount())
        assertEquals(1, canonicalDao.analysisSnapshotCount())
        assertEquals(2, database.workoutSessionDao().stepRecordCount())
    }

    @Test
    fun deleteAllSessionsRemovesSessionsAndChildRecords() = runBlocking {
        repository.upsertSession(
            timedSession(
                id = "timed-delete-all",
                status = SessionStatus.COMPLETED,
                totalElapsedSec = 105,
                effectiveElapsedSec = 90,
                pausedElapsedSec = 0,
                restExtensionRecords = listOf(restExtensionRecord("extension-delete-all"))
            )
        )
        repository.upsertSession(
            strengthSession(
                id = "strength-delete-all",
                status = SessionStatus.COMPLETED,
                records = listOf(confirmedStrengthRecord("confirmed-delete-all"))
            )
        )

        repository.deleteAllSessions()

        assertTrue(repository.getSessions().isEmpty())
        assertEquals(0, database.workoutSessionDao().sessionCount())
        assertEquals(0, database.workoutSessionDao().stepRecordCount())
        assertEquals(0, database.workoutSessionDao().timedRestExtensionRecordCount())
        assertEquals(0, database.workoutSessionDao().strengthSetRecordCount())
    }

    @Test
    fun deleteSessionsForPlanRemovesOnlyThatPlanSessionsAndKeepsWorkoutPlans() = runBlocking {
        val planRepository = WorkoutPlanRepository(database)
        planRepository.upsertPlan(savedPlan("plan-timed"))
        planRepository.upsertPlan(savedPlan("plan-other"))
        repository.upsertSession(
            timedSession(
                id = "timed-plan-delete",
                status = SessionStatus.COMPLETED,
                totalElapsedSec = 75,
                effectiveElapsedSec = 60,
                pausedElapsedSec = 0,
                planId = "plan-timed",
                restExtensionRecords = listOf(restExtensionRecord("extension-plan-delete"))
            )
        )
        repository.upsertSession(
            timedSession(
                id = "timed-plan-keep",
                status = SessionStatus.COMPLETED,
                totalElapsedSec = 75,
                effectiveElapsedSec = 60,
                pausedElapsedSec = 0,
                planId = "plan-other"
            )
        )

        repository.deleteSessionsForPlan("plan-timed")

        val remaining = repository.getSessions()
        assertEquals(listOf("timed-plan-keep"), remaining.map { session -> session.id })
        assertEquals(listOf("plan-other"), remaining.map { session -> session.planId })
        assertEquals(2, planRepository.getPlans().size)
        assertNotNull(planRepository.getPlan("plan-timed"))
        assertNotNull(planRepository.getPlan("plan-other"))
        assertEquals(1, database.workoutSessionDao().sessionCount())
        assertEquals(2, database.workoutSessionDao().stepRecordCount())
        assertEquals(0, database.workoutSessionDao().timedRestExtensionRecordCount())
    }

    @Test
    fun deleteSessionsStartedOnDateRemovesOnlyThatStartedAtDate() = runBlocking {
        repository.upsertSession(
            timedSession(
                id = "date-delete-morning",
                status = SessionStatus.COMPLETED,
                totalElapsedSec = 75,
                effectiveElapsedSec = 60,
                pausedElapsedSec = 0,
                startedAt = "2026-06-07T08:00:00Z",
                restExtensionRecords = listOf(restExtensionRecord("extension-date-delete"))
            )
        )
        repository.upsertSession(
            strengthSession(
                id = "date-delete-evening",
                status = SessionStatus.COMPLETED,
                records = listOf(confirmedStrengthRecord("set-date-delete")),
                startedAt = "2026-06-07T19:00:00Z"
            )
        )
        repository.upsertSession(
            timedSession(
                id = "date-keep",
                status = SessionStatus.COMPLETED,
                totalElapsedSec = 75,
                effectiveElapsedSec = 60,
                pausedElapsedSec = 0,
                startedAt = "2026-06-08T08:00:00Z"
            )
        )

        repository.deleteSessionsStartedOnDate("2026-06-07")

        val remaining = repository.getSessions()
        assertEquals(listOf("date-keep"), remaining.map { session -> session.id })
        assertFalse(remaining.any { session -> session.startedAt?.startsWith("2026-06-07") == true })
        assertEquals(1, database.workoutSessionDao().sessionCount())
        assertEquals(2, database.workoutSessionDao().stepRecordCount())
        assertEquals(0, database.workoutSessionDao().timedRestExtensionRecordCount())
        assertEquals(0, database.workoutSessionDao().strengthSetRecordCount())
    }

    private fun timedSession(
        id: String,
        status: SessionStatus,
        totalElapsedSec: Int,
        effectiveElapsedSec: Int,
        pausedElapsedSec: Int,
        restExtensionRecords: List<TimedRestExtensionRecord> = emptyList(),
        planId: String = "plan-timed",
        startedAt: String = "2026-06-07T10:00:00Z"
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            planId = planId,
            mode = WorkoutMode.TIMED,
            planSnapshot = WorkoutPlanSnapshot(
                planId = planId,
                title = "测试计时",
                mode = WorkoutMode.TIMED,
                blocks = listOf(
                    TimedCircuitBlock(
                        id = "interval-main",
                        order = 1,
                        rounds = 2,
                        restBetweenRoundsSec = 30,
                        items = listOf(
                            TimedExerciseItem(
                                id = "work-stage",
                                stageType = TimedStageType.WORK,
                                iconKey = "work",
                                colorHex = "#F26B4F",
                                workDurationSec = 30,
                                restAfterSec = 10,
                                autoAdvance = true
                            )
                        )
                    )
                ),
                preferences = PlanPreferences(
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(thresholdSec = 4),
                        restEnding = CountdownCue(thresholdSec = 3)
                    )
                )
            ),
            status = status,
            startedAt = startedAt,
            endedAt = "2026-06-07T10:01:15Z",
            totalElapsedSec = totalElapsedSec,
            effectiveElapsedSec = effectiveElapsedSec,
            pausedElapsedSec = pausedElapsedSec,
            timedRestExtensionRecords = restExtensionRecords,
            stepHistory = listOf(
                SessionStepRecord(
                    stepId = "warmup",
                    kind = SessionStepKind.TIMED_WORK,
                    startedAt = "2026-06-07T10:00:00Z",
                    endedAt = "2026-06-07T10:00:30Z",
                    actualDurationSec = 30
                ),
                SessionStepRecord(
                    stepId = "rest",
                    kind = SessionStepKind.TIMED_REST,
                    startedAt = "2026-06-07T10:00:30Z",
                    endedAt = "2026-06-07T10:01:00Z",
                    actualDurationSec = 30
                )
            )
        )
    }

    private fun strengthSession(
        id: String,
        status: SessionStatus,
        records: List<StrengthSetRecord>,
        planId: String = "plan-strength",
        startedAt: String = "2026-06-07T11:00:00Z"
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            planId = planId,
            mode = WorkoutMode.STRENGTH,
            planSnapshot = WorkoutPlanSnapshot(
                planId = planId,
                title = "测试力量",
                mode = WorkoutMode.STRENGTH,
                blocks = listOf(
                    StrengthExerciseBlock(
                        id = "bench",
                        order = 1,
                        exerciseId = "barbell-bench-press",
                        target = StrengthExerciseTarget(
                            weight = WeightValue(60.0, WeightUnit.KG),
                            repTarget = RepTarget.Range(8, 12),
                            restAfterSetSec = 90
                        ),
                        sets = listOf(
                            StrengthSetPlan(
                                id = "bench-working-1",
                                order = 1,
                                kind = StrengthSetKind.WORKING,
                                restAfterSec = 90
                            ),
                            StrengthSetPlan(
                                id = "bench-working-2",
                                order = 2,
                                kind = StrengthSetKind.WORKING,
                                restAfterSec = 0
                            )
                        ),
                        substitutions = listOf("incline-push-up")
                    )
                )
            ),
            status = status,
            startedAt = startedAt,
            endedAt = "2026-06-07T11:08:00Z",
            totalElapsedSec = 480,
            effectiveElapsedSec = 450,
            pausedElapsedSec = 30,
            strengthSetRecords = records
        )
    }

    private fun restExtensionRecord(id: String): TimedRestExtensionRecord {
        return TimedRestExtensionRecord(
            id = id,
            stepId = "interval-r2-work-stage-rest",
            stepIndex = 3,
            roundIndex = 2,
            restStageId = "work-stage",
            restStageTitle = "Rest",
            previousStageId = "work-stage",
            previousStageTitle = "Work",
            addedSec = 15,
            plannedRestSec = 10,
            restElapsedBeforeExtensionSec = 4,
            extensionAtRemainingSec = 6,
            cumulativeExtraRestSec = 15,
            eventElapsedSec = 64
        )
    }

    private fun confirmedStrengthRecord(id: String): StrengthSetRecord {
        return StrengthSetRecord(
            id = id,
            exerciseId = "barbell-bench-press",
            sourceSetPlanId = "bench-working-1",
            setOrder = 1,
            setKind = StrengthSetKind.WORKING,
            actualWeight = WeightValue(60.0, WeightUnit.KG),
            actualReps = 8,
            activeDurationSec = 40,
            actualRestAfterSec = 90
        )
    }

    private fun savedPlan(planId: String): WorkoutPlan {
        return WorkoutPlan(
            id = planId,
            mode = WorkoutMode.TIMED,
            title = "保存计划 $planId",
            blocks = emptyList(),
            createdAt = "2026-06-07T09:00:00Z",
            updatedAt = "2026-06-07T09:00:00Z"
        )
    }
}
