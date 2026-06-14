package com.liujyks.trainflow.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.TrainFlowDatabase
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
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.WorkoutSession
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    private fun timedSession(
        id: String,
        status: SessionStatus,
        totalElapsedSec: Int,
        effectiveElapsedSec: Int,
        pausedElapsedSec: Int,
        restExtensionRecords: List<TimedRestExtensionRecord> = emptyList()
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            planId = "plan-timed",
            mode = WorkoutMode.TIMED,
            planSnapshot = WorkoutPlanSnapshot(
                planId = "plan-timed",
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
            startedAt = "2026-06-07T10:00:00Z",
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
        records: List<StrengthSetRecord>
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            planId = "plan-strength",
            mode = WorkoutMode.STRENGTH,
            planSnapshot = WorkoutPlanSnapshot(
                planId = "plan-strength",
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
            startedAt = "2026-06-07T11:00:00Z",
            endedAt = "2026-06-07T11:08:00Z",
            totalElapsedSec = 480,
            effectiveElapsedSec = 450,
            pausedElapsedSec = 30,
            strengthSetRecords = records
        )
    }
}
