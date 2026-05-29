package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.HeartRateAvailability
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedWorkoutSessionUiStateTest {
    @Test
    fun activeWorkStepMapsCurrentActionTimerAndNextStep() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val uiState = started.toTimedWorkoutSessionScreenState()

        assertEquals(plan.title, uiState.planTitle)
        assertEquals("动作", uiState.phaseLabel)
        assertEquals("03:00", uiState.timerText)
        assertTrue(uiState.currentTitle.isNotBlank())
        assertTrue(uiState.nextStepLabel.startsWith("下一步"))
        assertTrue(uiState.shortCue.isNotBlank())
        assertTrue(uiState.shouldShowNextStepPanel)
        assertTrue(uiState.canPause)
        assertTrue(uiState.canSkip)
        assertFalse(uiState.canExtendRest)
    }

    @Test
    fun restStepMapsRestControlAndPreparesNextAction() {
        val plan = buildDefaultPlanManagementState().plans.first()
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        val uiState = state.toTimedWorkoutSessionScreenState()

        assertEquals("休息", uiState.phaseLabel)
        assertTrue(uiState.currentTitle.contains("休息"))
        assertTrue(uiState.shortCue.contains("准备"))
        assertTrue(uiState.canExtendRest)
    }

    @Test
    fun pausedStateFreezesControlsAndOffersResume() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val paused = TimedWorkoutEngine.dispatch(started, WorkoutCommand.PauseSession).state
        val uiState = paused.toTimedWorkoutSessionScreenState()

        assertEquals("已暂停", uiState.phaseLabel)
        assertTrue(uiState.isPaused)
        assertTrue(uiState.canResume)
        assertFalse(uiState.canPause)
        assertFalse(uiState.canSkip)
        assertTrue(uiState.shortCue.contains("冻结"))
    }

    @Test
    fun heartRatePlaceholderStaysSecondaryAndAbstract() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val uiState = started.toTimedWorkoutSessionScreenState(
            heartRateState = HeartRateState(
                availability = HeartRateAvailability.NOT_CONNECTED
            )
        )

        assertEquals("-- bpm", uiState.heartRate.valueText)
        assertEquals("未连接设备", uiState.heartRate.statusText)
        assertFalse(uiState.heartRate.isAvailable)
    }

    @Test
    fun terminalStatesStayLightweight() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val ended = TimedWorkoutEngine.dispatch(
            started,
            WorkoutCommand.EndSession(reason = "test")
        ).state
        val uiState = ended.toTimedWorkoutSessionScreenState()

        assertTrue(uiState.isTerminal)
        assertEquals("计时训练已提前结束", uiState.terminalTitle)
        assertTrue(requireNotNull(uiState.terminalSummary).contains("已完成"))
        assertFalse(uiState.shouldShowNextStepPanel)
        assertFalse(uiState.canEnd)
    }
}
