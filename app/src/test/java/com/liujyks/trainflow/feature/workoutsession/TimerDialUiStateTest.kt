package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.ui.theme.SkinRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerDialUiStateTest {
    @Test
    fun progressValuesAreClamped() {
        val state = TimerDialUiState.Empty.copy(
            totalProgress = 1.4f,
            currentStageProgress = -0.2f,
            totalRemainingSec = -3,
            currentStageRemainingSec = -1,
            stageSegments = listOf(
                TimerDialStageSegmentUiState(
                    id = "work",
                    label = "Work",
                    stageType = TimerDialStageType.WORK,
                    durationSec = -10,
                    progress = Float.NaN,
                    isCurrent = true
                )
            )
        ).clamped()

        assertEquals(1f, state.totalProgress, 0.0001f)
        assertEquals(0f, state.currentStageProgress, 0.0001f)
        assertEquals(0, state.totalRemainingSec)
        assertEquals(0, state.currentStageRemainingSec)
        assertEquals(0, state.stageSegments.single().durationSec)
        assertEquals(0f, state.stageSegments.single().progress, 0.0001f)
    }

    @Test
    fun engineStateMapsTotalAndCurrentStageProgress() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(0.4f, dial.currentStageProgress, 0.0001f)
        assertEquals(4f / 15f, dial.totalProgress, 0.0001f)
        assertEquals(11, dial.totalRemainingSec)
        assertEquals(1, dial.currentStageIndex)
        assertEquals(TimerDialStageType.WORK, dial.currentStageType)
        assertEquals(2, dial.stageSegments.size)
        assertTrue(dial.stageSegments.first().isCurrent)
        assertEquals(0.4f, dial.stageSegments.first().progress, 0.0001f)
        assertEquals(0f, dial.stageSegments.last().progress, 0.0001f)
    }

    @Test
    fun completedSegmentsAndRestSemanticsMapAcrossStageSwitch() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 4, restSec = 6)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 5).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(TimerDialStageType.REST, dial.currentStageType)
        assertEquals(1f, dial.stageSegments.first().progress, 0.0001f)
        assertTrue(dial.stageSegments.last().isCurrent)
        assertEquals(1f / 6f, dial.currentStageProgress, 0.0001f)
        assertTrue(TimerDialStageType.WORK.strokeWidthDp() > TimerDialStageType.REST.strokeWidthDp())
        assertTrue(TimerDialStageType.WARMUP.strokeWidthDp() > TimerDialStageType.REST.strokeWidthDp())
    }

    @Test
    fun visualVariantsStayLimitedToThreePrototypeDirections() {
        val variants = TimerDialVisualVariant.entries

        assertEquals(3, variants.size)
        assertTrue(TimerDialVisualVariant.BLACK_RED_HIGH_CONTRAST in variants)
        assertTrue(TimerDialVisualVariant.CYBER_NEON in variants)
        assertTrue(TimerDialVisualVariant.OFFICIAL_FLOW in variants)
        variants.forEach { variant ->
            val tokens = variant.tokens(SkinRegistry.defaultSkin)
            assertTrue(tokens.work != tokens.rest)
            assertTrue(tokens.textPrimary != tokens.pageBackground)
        }
    }

    @Test
    fun finalCountdownFlagMapsFromActiveCueOnly() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                timerDialPlan(
                    workSec = 8,
                    restSec = 4,
                    cueSettings = CueSettings(actionEnding = CountdownCue(thresholdSec = 5))
                )
            ),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 3).state
        var dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(dial.isFinalCountdown)

        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession).state
        dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(dial.isPaused)
        assertFalse(dial.isFinalCountdown)
    }

    @Test
    fun pausedStatePreservesProgressAndOffersResumeAction() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 12, restSec = 4)),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.tick(state, seconds = 5).state
        val activeDial = state.toTimedWorkoutSessionScreenState().timerDial

        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession).state
        state = TimedWorkoutEngine.tick(state, seconds = 9).state
        val pausedDial = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(pausedDial.isPaused)
        assertTrue(pausedDial.canTogglePause)
        assertEquals("双击继续", pausedDial.centerActionLabel)
        assertEquals(activeDial.currentStageProgress, pausedDial.currentStageProgress, 0.0001f)
        assertEquals(activeDial.totalProgress, pausedDial.totalProgress, 0.0001f)
    }

    private fun timerDialPlan(
        workSec: Int,
        restSec: Int,
        cueSettings: CueSettings? = null
    ): WorkoutPlan {
        return WorkoutPlan(
            id = "timer-dial-test",
            mode = WorkoutMode.TIMED,
            title = "Timer Dial Test",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "timer-dial-circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "warm",
                            labelOverride = "Warmup",
                            stageType = TimedStageType.WARMUP,
                            workDurationSec = 0
                        ),
                        TimedExerciseItem(
                            id = "work",
                            labelOverride = "Work",
                            stageType = TimedStageType.WORK,
                            workDurationSec = workSec,
                            restAfterSec = restSec
                        )
                    )
                )
            ),
            preferences = cueSettings?.let { PlanPreferences(cueSettings = it) },
            createdAt = "2026-06-10T00:00:00Z",
            updatedAt = "2026-06-10T00:00:00Z"
        )
    }
}
