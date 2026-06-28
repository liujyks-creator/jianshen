package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.TIMED_COMPOSITION_CURRENT_VERSION
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionStageGroup
import com.liujyks.trainflow.core.model.TimedCompositionTarget
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedCompositionTimelineAdapter
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStageKind
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStep
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerDialCompositionMappingTest {
    @Test
    fun innerMarkerUsesTotalStageInstancesNotCurrentStageGroupTargetCount() {
        val block = compositionBlock(
            warmupSec = 30,
            cooldownSec = 45,
            rounds = 3,
            restBetweenRoundsSec = 20,
            stageGroups = listOf(
                stageGroup(
                    id = "main",
                    order = 1,
                    targets = listOf(
                        actionTarget(id = "jump", durationSec = 45),
                        restTarget(id = "breathe", durationSec = 15, order = 2)
                    )
                ),
                stageGroup(
                    id = "core",
                    order = 2,
                    targets = listOf(
                        actionTarget(id = "plank", durationSec = 30),
                        customTarget(id = "hold", durationSec = 20, order = 2),
                        restTarget(id = "shakeout", durationSec = 10, order = 3)
                    )
                )
            )
        )

        val dial = block.expectedDialFor(
            activeProgress = 0.5f,
            selectActiveStep = { step ->
                step.roundIndex == 3 &&
                    step.stageGroupId == "main" &&
                    step.targetId == "breathe"
            }
        )

        assertEquals(10, dial.totalWorkoutStageCount)
        assertEquals(7, dial.completedWorkoutStageCount)
        assertEquals("10", dial.innerMarkerData().first().label)
        assertEquals(2, dial.stageSegments.size)
        assertEquals(listOf(45, 15), dial.stageSegments.map { segment -> segment.durationSec })
        assertTrue(dial.totalWorkoutStageCount != dial.stageSegments.size)
    }

    @Test
    fun oneTargetStageGroupMapsToOneFullRingSegment() {
        val block = compositionBlock(
            stageGroups = listOf(
                stageGroup(
                    id = "single",
                    order = 1,
                    targets = listOf(actionTarget(id = "only", durationSec = 60))
                )
            )
        )

        val dial = block.expectedDialFor(
            activeProgress = 0.25f,
            selectActiveStep = { step -> step.targetId == "only" }
        )

        assertEquals(1, dial.stageSegments.size)
        assertEquals("only", dial.stageSegments.single().label)
        assertEquals(60, dial.stageSegments.single().durationSec)
        assertEquals(0.25f, dial.stageSegments.single().progress, 0.0001f)
        assertTrue(dial.stageSegments.single().isCurrent)
    }

    @Test
    fun twoTargetsMapByPlannedDurationRatio() {
        val block = compositionBlock(
            stageGroups = listOf(
                stageGroup(
                    id = "work-rest",
                    order = 1,
                    targets = listOf(
                        actionTarget(id = "action", durationSec = 45),
                        restTarget(id = "rest", durationSec = 15, order = 2)
                    )
                )
            )
        )

        val dial = block.expectedDialFor(
            activeProgress = 0.4f,
            selectActiveStep = { step -> step.targetId == "action" }
        )

        assertEquals(listOf(45, 15), dial.stageSegments.map { segment -> segment.durationSec })
        assertEquals(60, dial.stageSegments.sumOf { segment -> segment.durationSec })
        assertEquals(TimerDialStageType.WORK, dial.stageSegments.first().stageType)
        assertEquals(TimerDialStageType.REST, dial.stageSegments.last().stageType)
        assertEquals(0.4f, dial.stageSegments.first().progress, 0.0001f)
        assertEquals(0f, dial.stageSegments.last().progress, 0.0001f)
    }

    @Test
    fun threeToFiveTargetsMapActionCustomRestWithCompletedActiveAndFutureState() {
        val block = compositionBlock(
            stageGroups = listOf(
                stageGroup(
                    id = "five-targets",
                    order = 1,
                    targets = listOf(
                        actionTarget(id = "squat", durationSec = 30),
                        customTarget(id = "pulse", durationSec = 60, order = 2),
                        restTarget(id = "rest", durationSec = 30, order = 3),
                        actionTarget(id = "lunge", durationSec = 15, order = 4),
                        customTarget(id = "hold", durationSec = 15, order = 5)
                    )
                )
            )
        )

        val dial = block.expectedDialFor(
            activeProgress = 0.5f,
            selectActiveStep = { step -> step.targetId == "pulse" }
        )

        assertEquals(5, dial.stageSegments.size)
        assertEquals(listOf(30, 60, 30, 15, 15), dial.stageSegments.map { segment -> segment.durationSec })
        assertEquals(
            listOf(
                TimerDialStageType.WORK,
                TimerDialStageType.CUSTOM,
                TimerDialStageType.REST,
                TimerDialStageType.WORK,
                TimerDialStageType.CUSTOM
            ),
            dial.stageSegments.map { segment -> segment.stageType }
        )
        assertEquals(listOf(1f, 0.5f, 0f, 0f, 0f), dial.stageSegments.map { segment -> segment.progress })
        assertEquals(listOf(false, true, false, false, false), dial.stageSegments.map { segment -> segment.isCurrent })
    }

    @Test
    fun targetColorFallsBackToStageGroupThenSafeDefault() {
        val groupFallbackBlock = compositionBlock(
            stageGroups = listOf(
                stageGroup(
                    id = "colors",
                    order = 1,
                    colorHex = "#00BCD4",
                    targets = listOf(
                        actionTarget(id = "target-color", durationSec = 10, colorHex = "#FFC107"),
                        restTarget(id = "group-color", durationSec = 10, order = 2, colorHex = "bad-target"),
                        customTarget(id = "also-group-color", durationSec = 10, order = 3, colorHex = "bad-custom")
                    )
                )
            )
        )

        val groupFallbackDial = groupFallbackBlock.expectedDialFor(
            activeProgress = 0.1f,
            selectActiveStep = { step -> step.targetId == "group-color" }
        )

        assertEquals(
            listOf("#FFC107", "#00BCD4", "#00BCD4"),
            groupFallbackDial.stageSegments.map { segment -> segment.colorHex }
        )

        val safeFallbackBlock = compositionBlock(
            stageGroups = listOf(
                stageGroup(
                    id = "safe",
                    order = 1,
                    colorHex = "bad-group",
                    targets = listOf(customTarget(id = "safe-custom", colorHex = "bad-target"))
                )
            )
        )

        val safeFallbackDial = safeFallbackBlock.expectedDialFor(
            activeProgress = 0.1f,
            selectActiveStep = { step -> step.targetId == "safe-custom" }
        )

        assertEquals(TimedStageType.CUSTOM.defaultColorHex, safeFallbackDial.stageSegments.single().colorHex)
    }

    @Test
    fun boundaryAndBetweenRoundRestStagesUseSingleCurrentStageFallbackSegment() {
        val block = compositionBlock(
            warmupSec = 20,
            cooldownSec = 40,
            rounds = 2,
            restBetweenRoundsSec = 20,
            stageGroups = listOf(
                stageGroup(
                    id = "main",
                    order = 1,
                    targets = listOf(
                        actionTarget(id = "work", durationSec = 40),
                        restTarget(id = "rest", durationSec = 20, order = 2)
                    )
                )
            )
        )

        val warmupDial = block.expectedDialFor(
            activeProgress = 0.25f,
            selectActiveStep = { step -> step.timelineStageKind == TimedCompositionTimelineStageKind.WARMUP }
        )
        val roundRestDial = block.expectedDialFor(
            activeProgress = 0.5f,
            selectActiveStep = {
                it.timelineStageKind == TimedCompositionTimelineStageKind.BETWEEN_ROUND_REST
            }
        )
        val cooldownDial = block.expectedDialFor(
            activeProgress = 0.75f,
            selectActiveStep = { step -> step.timelineStageKind == TimedCompositionTimelineStageKind.COOLDOWN }
        )

        assertFallbackSegment(warmupDial, TimerDialStageType.WARMUP, 20, 0.25f, completedStages = 0)
        assertFallbackSegment(roundRestDial, TimerDialStageType.REST, 20, 0.5f, completedStages = 2)
        assertFallbackSegment(cooldownDial, TimerDialStageType.COOLDOWN, 40, 0.75f, completedStages = 4)
        assertEquals(5, warmupDial.totalWorkoutStageCount)
        assertEquals(5, roundRestDial.totalWorkoutStageCount)
        assertEquals(5, cooldownDial.totalWorkoutStageCount)
    }

    @Test
    fun restExtensionKeepsPlannedRatiosSegmentCountAndProgressMonotonic() {
        val block = compositionBlock(
            stageGroups = listOf(
                stageGroup(
                    id = "extension",
                    order = 1,
                    targets = listOf(
                        actionTarget(id = "work-a", durationSec = 30),
                        customTarget(id = "custom", durationSec = 20, order = 2),
                        restTarget(id = "active-rest", durationSec = 15, order = 3),
                        actionTarget(id = "work-b", durationSec = 10, order = 4),
                        customTarget(id = "finisher", durationSec = 10, order = 5)
                    )
                )
            )
        )

        val plan = block.toWorkoutPlan()
        val activeRestStep = TimedCompositionTimelineAdapter.expand(block)
            .steps
            .single { step -> step.targetId == "active-rest" }
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.tick(
            state,
            seconds = block.secondsBefore(activeRestStep) + 9
        ).state
        val beforeExtension = state.toTimedWorkoutSessionScreenState(plan = plan).timerDial
        val activeRestStepId = beforeExtension.stageSegments.single { segment -> segment.isCurrent }.id
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ExtendRest(seconds = 15)).state
        val afterExtension = state.toTimedWorkoutSessionScreenState(plan = plan).timerDial

        assertEquals(beforeExtension.stageSegments.map { segment -> segment.id }, afterExtension.stageSegments.map { it.id })
        assertEquals(
            beforeExtension.stageSegments.map { segment -> segment.durationSec },
            afterExtension.stageSegments.map { segment -> segment.durationSec }
        )
        assertEquals(5, afterExtension.stageSegments.size)
        assertEquals(15, afterExtension.stageSegments.single { segment -> segment.isCurrent }.durationSec)
        assertTrue(afterExtension.totalProgress >= beforeExtension.totalProgress)
        assertTrue(
            afterExtension.stageSegments.single { segment -> segment.isCurrent }.progress >=
                beforeExtension.stageSegments.single { segment -> segment.isCurrent }.progress
        )
        assertEquals(beforeExtension.smoothProgressIdentity(), afterExtension.smoothProgressIdentity())
        assertNotEquals(beforeExtension.smoothProgressAnchor(), afterExtension.smoothProgressAnchor())
    }

    @Test
    fun smoothProgressIdentityExcludesPerSecondProgressAndRemainingInputs() {
        val block = compositionBlock(
            stageGroups = listOf(
                stageGroup(
                    id = "identity",
                    order = 1,
                    targets = listOf(
                        actionTarget(id = "work", durationSec = 60),
                        restTarget(id = "rest", durationSec = 20, order = 2)
                    )
                )
            )
        )

        val beforeTick = block.expectedDialFor(
            activeProgress = 0.25f,
            currentRemainingSec = 45,
            selectActiveStep = { step -> step.targetId == "work" }
        )
        val afterTick = block.expectedDialFor(
            activeProgress = 0.5f,
            currentRemainingSec = 30,
            selectActiveStep = { step -> step.targetId == "work" }
        )

        assertEquals(beforeTick.smoothProgressIdentity(), afterTick.smoothProgressIdentity())
        assertNotEquals(beforeTick.smoothProgressAnchor(), afterTick.smoothProgressAnchor())
    }

    @Test
    fun v2ActiveSegmentDisplayedProgressIsMonotonicAcrossSecondTick() {
        val block = compositionBlock(
            stageGroups = listOf(
                stageGroup(
                    id = "monotonic",
                    order = 1,
                    targets = listOf(
                        actionTarget(id = "work", durationSec = 60),
                        restTarget(id = "rest", durationSec = 20, order = 2)
                    )
                )
            )
        )

        val beforeTick = block.expectedDialFor(
            activeProgress = 0.25f,
            currentRemainingSec = 45,
            selectActiveStep = { step -> step.targetId == "work" }
        )
        val displayedBeforeTick = TimerDialDisplayedProgress(
            totalProgress = beforeTick.projectedTotalProgress(elapsedMillis = 1_000),
            currentStageProgress = beforeTick.projectedStageProgress(elapsedMillis = 1_000)
        )
        val afterTick = block.expectedDialFor(
            activeProgress = 0.25f,
            currentRemainingSec = 44,
            selectActiveStep = { step -> step.targetId == "work" }
        )
        val displayedAfterTick = afterTick.monotonicDisplayedProgress(
            elapsedMillis = 0,
            previousDisplayed = displayedBeforeTick
        )

        assertEquals(beforeTick.smoothProgressIdentity(), afterTick.smoothProgressIdentity())
        assertTrue(displayedAfterTick.currentStageProgress >= displayedBeforeTick.currentStageProgress)
        assertTrue(displayedAfterTick.totalProgress >= displayedBeforeTick.totalProgress)
    }

    @Test
    fun legacyTimedPlanKeepsExistingWorkRestCycleSemantics() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(legacyTimedPlan()),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 50).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(2, dial.totalWorkoutStageCount)
        assertEquals(0, dial.completedWorkoutStageCount)
        assertEquals(2, dial.stageSegments.size)
        assertEquals(listOf(45, 15), dial.stageSegments.map { segment -> segment.durationSec })
        assertEquals(listOf(TimerDialStageType.WORK, TimerDialStageType.REST), dial.stageSegments.map { it.stageType })
        assertEquals(listOf(1f, 5f / 15f), dial.stageSegments.map { segment -> segment.progress })
        assertEquals(listOf(false, true), dial.stageSegments.map { segment -> segment.isCurrent })
    }

    private fun assertFallbackSegment(
        dial: TimerDialUiState,
        stageType: TimerDialStageType,
        durationSec: Int,
        progress: Float,
        completedStages: Int
    ) {
        val segment = dial.stageSegments.single()

        assertEquals(stageType, dial.currentStageType)
        assertEquals(stageType, segment.stageType)
        assertEquals(durationSec, segment.durationSec)
        assertEquals(progress, segment.progress, 0.0001f)
        assertTrue(segment.isCurrent)
        assertEquals(completedStages, dial.completedWorkoutStageCount)
    }

    private fun TimedCompositionBlock.expectedDialFor(
        activeProgress: Float,
        currentRemainingSec: Int? = null,
        selectActiveStep: (TimedCompositionTimelineStep) -> Boolean
    ): TimerDialUiState {
        val plan = toWorkoutPlan()
        val timeline = TimedCompositionTimelineAdapter.expand(this)
        val activeStep = timeline.steps.single(selectActiveStep)
        val elapsedInActiveStep = currentRemainingSec
            ?.let { remaining -> activeStep.plannedDurationSec - remaining }
            ?: (activeStep.plannedDurationSec.toFloat() * activeProgress.clamped()).roundToInt()
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(
            state,
            seconds = secondsBefore(activeStep) +
                elapsedInActiveStep.coerceIn(0, activeStep.plannedDurationSec.coerceAtLeast(1) - 1)
        ).state

        return state.toTimedWorkoutSessionScreenState(plan = plan).timerDial
    }

    private fun TimedCompositionBlock.secondsBefore(activeStep: TimedCompositionTimelineStep): Int {
        return TimedCompositionTimelineAdapter.expand(this)
            .steps
            .takeWhile { step -> step.id != activeStep.id }
            .sumOf { step -> step.plannedDurationSec }
    }

    private fun TimedCompositionBlock.toWorkoutPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "${id}-timer-dial-production-plan",
            mode = WorkoutMode.TIMED,
            title = "Timer Dial Production Mapping",
            blocks = listOf(this),
            createdAt = "2026-06-28T00:00:00Z",
            updatedAt = "2026-06-28T00:00:00Z"
        )
    }

    private fun Float.clamped(): Float {
        return when {
            isNaN() -> 0f
            else -> coerceIn(0f, 1f)
        }
    }

    private fun compositionBlock(
        id: String = "composition",
        warmupSec: Int = 0,
        cooldownSec: Int = 0,
        rounds: Int = 1,
        restBetweenRoundsSec: Int = 0,
        stageGroups: List<TimedCompositionStageGroup>
    ): TimedCompositionBlock {
        return TimedCompositionBlock(
            id = id,
            order = 1,
            title = "Composition",
            compositionVersion = TIMED_COMPOSITION_CURRENT_VERSION,
            warmupSec = warmupSec,
            cooldownSec = cooldownSec,
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec,
            stageGroups = stageGroups
        )
    }

    private fun stageGroup(
        id: String,
        order: Int,
        colorHex: String = TimedStageType.WORK.defaultColorHex,
        targets: List<TimedCompositionTarget>
    ): TimedCompositionStageGroup {
        return TimedCompositionStageGroup(
            id = id,
            order = order,
            name = id,
            colorHex = colorHex,
            targets = targets
        )
    }

    private fun actionTarget(
        id: String,
        durationSec: Int = 30,
        order: Int = 1,
        colorHex: String = TimedStageType.WORK.defaultColorHex
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = id,
            kind = TimedCompositionTargetKind.ACTION,
            durationSec = durationSec,
            colorHex = colorHex
        )
    }

    private fun restTarget(
        id: String,
        durationSec: Int = 15,
        order: Int = 1,
        colorHex: String = TimedStageType.REST.defaultColorHex
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = id,
            kind = TimedCompositionTargetKind.REST,
            durationSec = durationSec,
            colorHex = colorHex
        )
    }

    private fun customTarget(
        id: String,
        durationSec: Int = 20,
        order: Int = 1,
        colorHex: String = TimedStageType.CUSTOM.defaultColorHex
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = id,
            kind = TimedCompositionTargetKind.CUSTOM,
            durationSec = durationSec,
            colorHex = colorHex
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

    private fun legacyTimedPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "legacy-timer-dial-composition-guard",
            mode = WorkoutMode.TIMED,
            title = "Legacy Timer Dial Guard",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "legacy-circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "legacy-work-a",
                            labelOverride = "Legacy Work A",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 45,
                            restAfterSec = 15
                        ),
                        TimedExerciseItem(
                            id = "legacy-work-b",
                            labelOverride = "Legacy Work B",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 30,
                            restAfterSec = 10
                        )
                    )
                )
            ),
            createdAt = "2026-06-28T00:00:00Z",
            updatedAt = "2026-06-28T00:00:00Z"
        )
    }
}
