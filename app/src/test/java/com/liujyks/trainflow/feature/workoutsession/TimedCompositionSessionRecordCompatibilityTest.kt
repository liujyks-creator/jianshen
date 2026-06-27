package com.liujyks.trainflow.feature.workoutsession

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.data.WorkoutSessionRepository
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.engine.TimedSessionStepKind
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.TIMED_COMPOSITION_CURRENT_VERSION
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionStageGroup
import com.liujyks.trainflow.core.model.TimedCompositionTarget
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedCompositionTimelineAdapter
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStep
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutSession
import com.liujyks.trainflow.feature.history.buildHistoryScreenState
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TimedCompositionSessionRecordCompatibilityTest {
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
    fun v2PlanExecutionCreatesTerminalSessionRecordAndSummaryWithoutCrash() {
        val block = compositionBlock(rounds = 1, restBetweenRoundsSec = 0)
        val plan = workoutPlan(block)
        val state = completedState(plan, sessionId = "v2-terminal-session")

        val session = state.toSessionRecord(plan)
        val summary = requireNotNull(state.toTimedWorkoutSummaryUiState())

        assertEquals(SessionStatus.COMPLETED, state.status)
        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(plan.id, session.planId)
        assertTrue(session.stepHistory.isNotEmpty())
        assertEquals("完成复盘", summary.title)
        assertTrue(summary.metricItems.any { item -> item.label == "步骤进度" })
    }

    @Test
    fun v2PlanSnapshotAndActualStepRecordsRoundTripThroughSessionRepository() = runBlocking {
        val block = compositionBlock()
        val plan = workoutPlan(block)
        val expectedTimeline = TimedCompositionTimelineAdapter.expand(block)
        val session = completedState(plan, sessionId = "v2-repository-session")
            .toSessionRecord(plan)

        repository.upsertSession(session)

        val restored = repository.getSessions().single()
        val restoredBlock = restored.planSnapshot.blocks.single() as TimedCompositionBlock
        val restoredTimelineById = TimedCompositionTimelineAdapter.expand(restoredBlock)
            .steps
            .associateBy { step -> step.id }

        assertEquals("timed-composition-plan", restored.planId)
        assertEquals(TIMED_COMPOSITION_CURRENT_VERSION, restoredBlock.compositionVersion)
        assertEquals("composition-record", restoredBlock.id)
        assertEquals(listOf("target-action", "target-rest"), restoredBlock.stageGroups.single().targets.map { it.id })
        assertEquals(expectedTimeline.steps.map { step -> step.id }, restored.stepHistory.map { record -> record.stepId })
        assertTrue(restored.stepHistory.all { record -> record.stepId in restoredTimelineById })
        assertEquals(expectedTimeline.steps.size, database.workoutSessionDao().stepRecordCount())
    }

    @Test
    fun v2RestTargetExtensionRecordsAddedSecondsInExistingRecordStructure() {
        val block = compositionBlock(
            warmupSec = 0,
            cooldownSec = 0,
            rounds = 1,
            restBetweenRoundsSec = 0
        )
        val plan = workoutPlan(block)
        val restStep = TimedCompositionTimelineAdapter.expand(block).steps.single { step -> step.isRest }

        val activeRest = activeStateAt(plan, restStep.id)
        val extended = TimedWorkoutEngine.dispatch(
            activeRest,
            WorkoutCommand.ExtendRest(seconds = 15)
        ).state
        val abandoned = TimedWorkoutEngine.dispatch(
            extended,
            WorkoutCommand.EndSession(reason = "test")
        ).state
        val session = abandoned.toSessionRecord(plan)
        val record = session.timedRestExtensionRecords.single()

        assertEquals(restStep.id, record.stepId)
        assertEquals(restStep.stepIndexIn(block), record.stepIndex)
        assertEquals(1, record.roundIndex)
        assertEquals("target-rest", record.restStageId)
        assertEquals("Rest target", record.restStageTitle)
        assertEquals("target-action", record.previousStageId)
        assertEquals("Action target", record.previousStageTitle)
        assertEquals(15, record.addedSec)
        assertEquals(restStep.plannedDurationSec, record.plannedRestSec)
        assertEquals(15, record.cumulativeExtraRestSec)
    }

    @Test
    fun v2SyntheticBetweenRoundRestExtensionRecordsCleanly() {
        val block = compositionBlock(
            warmupSec = 0,
            cooldownSec = 0,
            rounds = 2,
            restBetweenRoundsSec = 6,
            targets = listOf(actionTarget())
        )
        val plan = workoutPlan(block)
        val betweenRoundRest = TimedCompositionTimelineAdapter.expand(block)
            .steps
            .single { step -> step.isBetweenRoundRest }

        val activeRest = activeStateAt(plan, betweenRoundRest.id)
        val abandoned = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.dispatch(
                activeRest,
                WorkoutCommand.ExtendRest(seconds = 15)
            ).state,
            WorkoutCommand.EndSession(reason = "test")
        ).state
        val record = abandoned.toSessionRecord(plan).timedRestExtensionRecords.single()

        assertEquals(betweenRoundRest.id, record.stepId)
        assertEquals(betweenRoundRest.targetId, record.restStageId)
        assertEquals(TimedStageType.REST.displayName, record.restStageTitle)
        assertEquals("target-action", record.previousStageId)
        assertEquals(15, record.addedSec)
        assertEquals(6, record.plannedRestSec)
    }

    @Test
    fun v2WorkWarmupAndCooldownStepsDoNotProduceRestExtensionRecords() {
        val block = compositionBlock(
            warmupSec = 3,
            cooldownSec = 2,
            rounds = 1,
            restBetweenRoundsSec = 0,
            targets = listOf(actionTarget(durationSec = 4))
        )
        val plan = workoutPlan(block)
        val nonRestSteps = TimedCompositionTimelineAdapter.expand(block)
            .steps
            .filter { step -> step.isWork }

        nonRestSteps.forEach { step ->
            val activeWork = activeStateAt(plan, step.id)
            val afterExtendAttempt = TimedWorkoutEngine.dispatch(
                activeWork,
                WorkoutCommand.ExtendRest(seconds = 15)
            ).state
            val abandoned = TimedWorkoutEngine.dispatch(
                afterExtendAttempt,
                WorkoutCommand.EndSession(reason = "test")
            ).state
            val session = abandoned.toSessionRecord(plan)

            assertEquals(activeWork.remainingSec, afterExtendAttempt.remainingSec)
            assertTrue("${step.id} should not record rest extension", session.timedRestExtensionRecords.isEmpty())
        }
    }

    @Test
    fun legacyTimedSessionRecordShapeRemainsUnchanged() {
        val plan = legacyTimedPlan()
        val activeRest = activeStateAt(plan, "legacy-circuit-r1-legacy-work-rest")
        val abandoned = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.dispatch(
                activeRest,
                WorkoutCommand.ExtendRest(seconds = 15)
            ).state,
            WorkoutCommand.EndSession(reason = "test")
        ).state
        val session = abandoned.toSessionRecord(plan)
        val record = session.timedRestExtensionRecords.single()

        assertEquals(
            listOf("legacy-circuit-r1-legacy-work-work", "legacy-circuit-r1-legacy-work-rest"),
            session.stepHistory.map { step -> step.stepId }
        )
        assertEquals("legacy-circuit-r1-legacy-work-rest", record.stepId)
        assertEquals("legacy-work", record.restStageId)
        assertEquals("legacy-work", record.previousStageId)
        assertEquals(15, record.addedSec)
        assertTrue(session.planSnapshot.blocks.single() is TimedCircuitBlock)
    }

    @Test
    fun historyMappersCanReadV2SessionSnapshotWithoutCrash() {
        val block = compositionBlock()
        val plan = workoutPlan(block)
        val session = completedState(plan, sessionId = "v2-history-session")
            .toSessionRecord(plan)

        val state = buildHistoryScreenState(listOf(session))
        val detail = requireNotNull(state.selectedDetail)
        val stats = requireNotNull(state.recordStatsUiState)
        val trend = requireNotNull(state.timedComparableRestTrendUiState)

        assertEquals("V2 session record plan", detail.title)
        assertTrue(detail.rows.any { row -> row.label == "完成步骤" })
        assertTrue(stats.rows.any { row -> row.label == "训练总次数" })
        assertTrue(trend.groups.isEmpty())
        assertNotNull(trend.emptyMessage)
    }

    @Test
    fun unsupportedAndEmptyV2PlansFailClosedWithoutMalformedSessionRecords() {
        val unsupported = workoutPlan(
            compositionBlock(
                compositionVersion = TIMED_COMPOSITION_CURRENT_VERSION + 1
            )
        )
        val empty = workoutPlan(
            compositionBlock(
                warmupSec = 0,
                cooldownSec = 0,
                rounds = 1,
                restBetweenRoundsSec = 0,
                targets = emptyList()
            )
        )

        listOf(unsupported, empty).forEach { plan ->
            val state = TimedWorkoutEngine.dispatch(
                TimedWorkoutEngine.create(plan, sessionId = "closed-${plan.blocks.single().id}"),
                WorkoutCommand.StartSession
            ).state
            val session = state.toSessionRecord(plan)

            assertEquals(SessionStatus.COMPLETED, state.status)
            assertTrue(state.steps.isEmpty())
            assertTrue(session.stepHistory.isEmpty())
            assertTrue(session.timedRestExtensionRecords.isEmpty())
        }
    }

    private fun completedState(
        plan: WorkoutPlan,
        sessionId: String
    ): TimedWorkoutEngineState {
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan, sessionId = sessionId),
            WorkoutCommand.StartSession
        ).state
        val totalDuration = started.steps.sumOf { step -> step.durationSec }
        val completed = TimedWorkoutEngine.tick(started, seconds = totalDuration).state

        assertEquals(SessionStatus.COMPLETED, completed.status)
        return completed
    }

    private fun activeStateAt(
        plan: WorkoutPlan,
        stepId: String
    ): TimedWorkoutEngineState {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan, sessionId = "active-$stepId"),
            WorkoutCommand.StartSession
        ).state
        var guardSec = 0

        while (state.currentStep?.id != stepId && !state.isTerminal && guardSec < 600) {
            val advanceBy = state.remainingSec.coerceAtLeast(1)
            state = TimedWorkoutEngine.tick(state, seconds = advanceBy).state
            guardSec += advanceBy
        }

        assertEquals(stepId, state.currentStep?.id)
        return state
    }

    private fun TimedWorkoutEngineState.toSessionRecord(plan: WorkoutPlan): WorkoutSession {
        return toWorkoutSessionRecord(
            plan = plan,
            startedAt = STARTED_AT,
            endedAt = STARTED_AT.plusSeconds(activeElapsedSec.toLong())
        )
    }

    private fun TimedCompositionTimelineStep.stepIndexIn(block: TimedCompositionBlock): Int {
        return TimedCompositionTimelineAdapter.expand(block).steps.indexOfFirst { step -> step.id == id }
    }

    private fun workoutPlan(block: TimedCompositionBlock): WorkoutPlan {
        return workoutPlan(listOf(block))
    }

    private fun workoutPlan(blocks: List<com.liujyks.trainflow.core.model.PlanBlock>): WorkoutPlan {
        return WorkoutPlan(
            id = "timed-composition-plan",
            mode = WorkoutMode.TIMED,
            title = "V2 session record plan",
            blocks = blocks,
            createdAt = "2026-06-28T00:00:00Z",
            updatedAt = "2026-06-28T00:00:00Z"
        )
    }

    private fun compositionBlock(
        compositionVersion: Int = TIMED_COMPOSITION_CURRENT_VERSION,
        warmupSec: Int = 2,
        cooldownSec: Int = 2,
        rounds: Int = 2,
        restBetweenRoundsSec: Int = 5,
        targets: List<TimedCompositionTarget> = listOf(
            actionTarget(),
            restTarget()
        )
    ): TimedCompositionBlock {
        return TimedCompositionBlock(
            id = "composition-record",
            order = 1,
            title = "Record compatibility",
            compositionVersion = compositionVersion,
            warmupSec = warmupSec,
            cooldownSec = cooldownSec,
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec,
            stageGroups = if (targets.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    TimedCompositionStageGroup(
                        id = "group-main",
                        order = 1,
                        name = "Main group",
                        colorHex = TimedStageType.WORK.defaultColorHex,
                        targets = targets
                    )
                )
            }
        )
    }

    private fun actionTarget(
        durationSec: Int = 5
    ): TimedCompositionTarget {
        return TimedCompositionTarget(
            id = "target-action",
            order = 1,
            name = "Action target",
            kind = TimedCompositionTargetKind.ACTION,
            durationSec = durationSec,
            colorHex = TimedStageType.WORK.defaultColorHex
        )
    }

    private fun restTarget(
        durationSec: Int = 4
    ): TimedCompositionTarget {
        return TimedCompositionTarget(
            id = "target-rest",
            order = 2,
            name = "Rest target",
            kind = TimedCompositionTargetKind.REST,
            durationSec = durationSec,
            colorHex = TimedStageType.REST.defaultColorHex
        )
    }

    private fun legacyTimedPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "legacy-timed-plan",
            mode = WorkoutMode.TIMED,
            title = "Legacy timed plan",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "legacy-circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "legacy-work",
                            labelOverride = "Legacy work",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 5,
                            restAfterSec = 4
                        )
                    )
                )
            ),
            createdAt = "2026-06-28T00:00:00Z",
            updatedAt = "2026-06-28T00:00:00Z"
        )
    }

    private companion object {
        val STARTED_AT: Instant = Instant.parse("2026-06-28T10:00:00Z")
    }
}
