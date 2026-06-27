package com.liujyks.trainflow.core.engine

import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.PlanBlock
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
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStepKind
import com.liujyks.trainflow.core.model.TimedCompositionTimelineTargetKind
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.feature.plans.PlanManagementScreenState
import com.liujyks.trainflow.feature.plans.buildDefaultTimedCompositionPlanEditorState
import com.liujyks.trainflow.feature.plans.selectPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedCompositionEngineBridgeTest {
    @Test
    fun v2CompositionExpandsThroughTimelineAdapterIntoEngineCompatibleTimedSteps() {
        val block = bridgedCompositionBlock()
        val expected = TimedCompositionTimelineAdapter.expand(block)
            .steps
            .map { step -> step.toExpectedEngineStep(roundCount = block.rounds) }

        val state = TimedWorkoutEngine.create(workoutPlan(block))

        assertEquals(expected, state.steps.map { step -> step.toBridgeExpectation() })
    }

    @Test
    fun engineStepIdsComeFromTimelineMetadataAndRepeatedTargetsAreDistinct() {
        val block = bridgedCompositionBlock(
            warmupSec = 0,
            cooldownSec = 0,
            restBetweenRoundsSec = 0,
            stageGroups = listOf(
                stageGroup(
                    id = "main",
                    targets = listOf(actionTarget(id = "repeated-action"))
                )
            )
        )
        val expectedRepeatedIds = TimedCompositionTimelineAdapter.expand(block)
            .steps
            .filter { step -> step.targetId == "repeated-action" }
            .map { step -> step.id }

        val state = TimedWorkoutEngine.create(workoutPlan(block))
        val actualRepeatedIds = state.steps
            .filter { step -> step.itemId == "repeated-action" }
            .map { step -> step.id }

        assertEquals(2, expectedRepeatedIds.size)
        assertEquals(expectedRepeatedIds.distinct(), expectedRepeatedIds)
        assertEquals(expectedRepeatedIds, actualRepeatedIds)
    }

    @Test
    fun v2RestTargetsAndSyntheticBetweenRoundRestMapToRestExtendableStepsOnly() {
        val block = bridgedCompositionBlock()
        val timelineSteps = TimedCompositionTimelineAdapter.expand(block).steps
        val expectedRestStepIds = timelineSteps
            .filter { step -> step.isRest }
            .map { step -> step.id }
        val expectedNonRestStepIds = timelineSteps
            .filterNot { step -> step.isRest }
            .map { step -> step.id }

        val state = TimedWorkoutEngine.create(workoutPlan(block))

        assertEquals(expectedRestStepIds, state.steps.filter { step -> step.kind == TimedSessionStepKind.REST }.map { it.id })
        assertEquals(expectedNonRestStepIds, state.steps.filterNot { step -> step.kind == TimedSessionStepKind.REST }.map { it.id })
    }

    @Test
    fun v2RestTargetAndSyntheticBetweenRoundRestAcceptExtendRestWhenActive() {
        val block = bridgedCompositionBlock()
        val plan = workoutPlan(block)
        val restStepIds = TimedCompositionTimelineAdapter.expand(block)
            .steps
            .filter { step -> step.isRest }
            .map { step -> step.id }

        restStepIds.forEach { restStepId ->
            val activeRest = activeStateAt(plan, restStepId)
            val extended = TimedWorkoutEngine.dispatch(
                activeRest,
                WorkoutCommand.ExtendRest(seconds = 15)
            ).state

            assertEquals(activeRest.remainingSec + 15, extended.remainingSec)
            assertEquals(activeRest.extendedRestSec + 15, extended.extendedRestSec)
            assertTrue(extended.restExtensionHistory.isNotEmpty())
        }
    }

    @Test
    fun v2WorkWarmupAndCooldownStepsDoNotAcceptExtendRest() {
        val block = bridgedCompositionBlock()
        val plan = workoutPlan(block)
        val workStepIds = TimedCompositionTimelineAdapter.expand(block)
            .steps
            .filter { step -> step.isWork }
            .map { step -> step.id }

        workStepIds.forEach { workStepId ->
            val activeWork = activeStateAt(plan, workStepId)
            val afterExtendAttempt = TimedWorkoutEngine.dispatch(
                activeWork,
                WorkoutCommand.ExtendRest(seconds = 15)
            ).state

            assertEquals(activeWork.remainingSec, afterExtendAttempt.remainingSec)
            assertEquals(activeWork.extendedRestSec, afterExtendAttempt.extendedRestSec)
            assertEquals(activeWork.restExtensionHistory, afterExtendAttempt.restExtensionHistory)
        }
    }

    @Test
    fun legacyTimedPlanStillUsesExistingEnginePath() {
        val state = TimedWorkoutEngine.create(legacyTimedPlan())

        assertEquals(
            listOf(
                "legacy-warmup-work",
                "legacy-circuit-r1-main-work",
                "legacy-circuit-r1-main-rest",
                "legacy-circuit-r1-round-rest",
                "legacy-circuit-r2-main-work",
                "legacy-circuit-r2-main-rest",
                "legacy-cooldown-work"
            ),
            state.steps.map { step -> step.id }
        )
        assertEquals(
            listOf(
                TimedSessionStepKind.WORK,
                TimedSessionStepKind.WORK,
                TimedSessionStepKind.REST,
                TimedSessionStepKind.REST,
                TimedSessionStepKind.WORK,
                TimedSessionStepKind.REST,
                TimedSessionStepKind.WORK
            ),
            state.steps.map { step -> step.kind }
        )

        val started = TimedWorkoutEngine.dispatch(state, WorkoutCommand.StartSession).state

        assertEquals(SessionStatus.ACTIVE, started.status)
        assertEquals("legacy-warmup-work", started.currentStep?.id)
    }

    @Test
    fun unsupportedCompositionVersionFailsClosedWithoutExecutableV2Steps() {
        val block = bridgedCompositionBlock(compositionVersion = TIMED_COMPOSITION_CURRENT_VERSION + 1)

        assertThrows(IllegalArgumentException::class.java) {
            TimedCompositionTimelineAdapter.expand(block)
        }

        val createResult = runCatching {
            TimedWorkoutEngine.create(workoutPlan(block))
        }

        createResult.exceptionOrNull()?.let { error ->
            assertTrue(error is IllegalArgumentException)
            return
        }

        val state = createResult.getOrThrow()
        assertTrue(state.steps.isEmpty())
        assertEquals(
            SessionStatus.COMPLETED,
            TimedWorkoutEngine.dispatch(state, WorkoutCommand.StartSession).state.status
        )
    }

    @Test
    fun emptyV2TimelineFailsClosedWithoutExecutableSteps() {
        val block = bridgedCompositionBlock(
            warmupSec = 0,
            cooldownSec = 0,
            rounds = 1,
            restBetweenRoundsSec = 0,
            stageGroups = emptyList()
        )

        assertTrue(TimedCompositionTimelineAdapter.expand(block).steps.isEmpty())

        val state = TimedWorkoutEngine.create(workoutPlan(block))

        assertTrue(state.steps.isEmpty())
        assertEquals(
            SessionStatus.COMPLETED,
            TimedWorkoutEngine.dispatch(state, WorkoutCommand.StartSession).state.status
        )
    }

    @Test
    fun v2StartGateOpensAfterMinimumEngineBridge() {
        val v2Plan = buildDefaultTimedCompositionPlanEditorState(planId = "timed-composition-start-gate")
            .toWorkoutPlan(timestamp = "2026-06-27T01:00:00Z")
        val detail = PlanManagementScreenState(plans = listOf(v2Plan))
            .selectPlan(v2Plan.id)
            .selectedDetail

        assertTrue(v2Plan.blocks.single() is TimedCompositionBlock)
        assertTrue(requireNotNull(detail).canEditPlan)
        assertTrue(detail.canStartTraining)
        assertEquals("开始计时训练", detail.startStatus)
    }

    private fun activeStateAt(
        plan: WorkoutPlan,
        stepId: String
    ): TimedWorkoutEngineState {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        while (state.currentStep?.id != stepId && !state.isTerminal) {
            state = TimedWorkoutEngine.tick(
                state,
                seconds = state.remainingSec.coerceAtLeast(1)
            ).state
        }

        assertEquals(stepId, state.currentStep?.id)
        return state
    }

    private fun legacyTimedPlan(): WorkoutPlan {
        return workoutPlan(
            WarmupBlock(
                id = "legacy-warmup",
                order = 1,
                title = "Legacy warmup",
                durationSec = 10
            ),
            TimedCircuitBlock(
                id = "legacy-circuit",
                order = 2,
                rounds = 2,
                items = listOf(
                    TimedExerciseItem(
                        id = "main",
                        labelOverride = "Main work",
                        stageType = TimedStageType.WORK,
                        workDurationSec = 30,
                        restAfterSec = 15
                    )
                ),
                title = "Legacy circuit",
                restBetweenRoundsSec = 20
            ),
            CooldownBlock(
                id = "legacy-cooldown",
                order = 3,
                title = "Legacy cooldown",
                durationSec = 12
            )
        )
    }

    private fun workoutPlan(vararg blocks: PlanBlock): WorkoutPlan {
        return WorkoutPlan(
            id = "timed-composition-bridge-plan",
            mode = WorkoutMode.TIMED,
            title = "Timed composition bridge plan",
            blocks = blocks.toList(),
            createdAt = "2026-06-27T01:00:00Z",
            updatedAt = "2026-06-27T01:00:00Z"
        )
    }

    private fun bridgedCompositionBlock(
        compositionVersion: Int = TIMED_COMPOSITION_CURRENT_VERSION,
        warmupSec: Int = 10,
        cooldownSec: Int = 12,
        rounds: Int = 2,
        restBetweenRoundsSec: Int = 8,
        stageGroups: List<TimedCompositionStageGroup> = listOf(
            stageGroup(
                id = "group-main",
                targets = listOf(
                    actionTarget(id = "target-action", order = 1),
                    customTarget(id = "target-custom", order = 2),
                    restTarget(id = "target-rest", order = 3)
                )
            )
        )
    ): TimedCompositionBlock {
        return TimedCompositionBlock(
            id = "composition-bridge",
            order = 1,
            title = "Composition bridge",
            compositionVersion = compositionVersion,
            warmupSec = warmupSec,
            cooldownSec = cooldownSec,
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec,
            stageGroups = stageGroups
        )
    }

    private fun stageGroup(
        id: String,
        order: Int = 1,
        targets: List<TimedCompositionTarget>
    ): TimedCompositionStageGroup {
        return TimedCompositionStageGroup(
            id = id,
            order = order,
            name = "Main group",
            colorHex = TimedStageType.WORK.defaultColorHex,
            targets = targets
        )
    }

    private fun actionTarget(
        id: String,
        order: Int = 1,
        durationSec: Int = 40
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = "Jumping jacks",
            kind = TimedCompositionTargetKind.ACTION,
            durationSec = durationSec,
            colorHex = TimedStageType.WORK.defaultColorHex
        )
    }

    private fun customTarget(
        id: String,
        order: Int,
        durationSec: Int = 25
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = "Shadow boxing",
            kind = TimedCompositionTargetKind.CUSTOM,
            durationSec = durationSec,
            colorHex = TimedStageType.CUSTOM.defaultColorHex
        )
    }

    private fun restTarget(
        id: String,
        order: Int,
        durationSec: Int = 20
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = "Breathe",
            kind = TimedCompositionTargetKind.REST,
            durationSec = durationSec,
            colorHex = TimedStageType.REST.defaultColorHex
        )
    }

    private fun target(
        id: String,
        order: Int,
        name: String,
        kind: TimedCompositionTargetKind,
        durationSec: Int,
        colorHex: String
    ): TimedCompositionTarget {
        return TimedCompositionTarget(
            id = id,
            order = order,
            name = name,
            kind = kind,
            durationSec = durationSec,
            colorHex = colorHex
        )
    }

    private fun TimedCompositionTimelineStep.toExpectedEngineStep(
        roundCount: Int
    ): EngineStepExpectation {
        val isRestStep = stepKind == TimedCompositionTimelineStepKind.REST
        return EngineStepExpectation(
            id = id,
            kind = if (isRestStep) TimedSessionStepKind.REST else TimedSessionStepKind.WORK,
            sessionStepKind = if (isRestStep) SessionStepKind.TIMED_REST else SessionStepKind.TIMED_WORK,
            blockId = compositionBlockId,
            itemId = targetId,
            title = displayName,
            durationSec = plannedDurationSec,
            round = roundIndex,
            roundCount = roundIndex?.let { roundCount },
            stageType = targetKind.toExpectedStageType(),
            iconKey = iconKey,
            colorHex = colorHex
        )
    }

    private fun TimedSessionStep.toBridgeExpectation(): EngineStepExpectation {
        return EngineStepExpectation(
            id = id,
            kind = kind,
            sessionStepKind = sessionStepKind,
            blockId = blockId,
            itemId = itemId,
            title = title,
            durationSec = durationSec,
            round = round,
            roundCount = roundCount,
            stageType = stageType,
            iconKey = iconKey,
            colorHex = colorHex
        )
    }

    private fun TimedCompositionTimelineTargetKind.toExpectedStageType(): TimedStageType {
        return when (this) {
            TimedCompositionTimelineTargetKind.ACTION -> TimedStageType.WORK
            TimedCompositionTimelineTargetKind.REST -> TimedStageType.REST
            TimedCompositionTimelineTargetKind.CUSTOM -> TimedStageType.CUSTOM
            TimedCompositionTimelineTargetKind.WARMUP -> TimedStageType.WARMUP
            TimedCompositionTimelineTargetKind.COOLDOWN -> TimedStageType.COOLDOWN
            TimedCompositionTimelineTargetKind.BETWEEN_ROUND_REST -> TimedStageType.REST
        }
    }

    private data class EngineStepExpectation(
        val id: String,
        val kind: TimedSessionStepKind,
        val sessionStepKind: SessionStepKind,
        val blockId: String,
        val itemId: String?,
        val title: String,
        val durationSec: Int,
        val round: Int?,
        val roundCount: Int?,
        val stageType: TimedStageType?,
        val iconKey: String?,
        val colorHex: String?
    )
}
