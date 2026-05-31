package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.model.HeartRateAvailability
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.StrengthSetCompletionInput
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthWorkoutSessionUiStateTest {
    @Test
    fun prepareStateMapsCurrentSetTargetAndStartControl() {
        val plan = buildDefaultPlanManagementState().plans[1]
        val started = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val uiState = started.toStrengthWorkoutSessionScreenState()

        assertEquals(plan.title, uiState.planTitle)
        assertEquals("准备本组", uiState.phaseLabel)
        assertEquals("本组目标", uiState.primaryMetricLabel)
        assertTrue(uiState.primaryMetricText.contains("kg"))
        assertTrue(uiState.primaryMetricText.contains("次"))
        assertTrue(uiState.setProgressLabel.contains("第 1"))
        assertTrue(uiState.targetSummary.contains("次"))
        assertTrue(uiState.canStartSet)
        assertFalse(uiState.canCompleteSet)
        assertFalse(uiState.canConfirmPlanned)
        assertTrue(uiState.canPause)
    }

    @Test
    fun activeStateMapsElapsedTimerAndCompleteControl() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 5).state
        val uiState = state.toStrengthWorkoutSessionScreenState()

        assertEquals(SessionStepKind.STRENGTH_ACTIVE_SET, state.currentSessionStep?.kind)
        assertEquals("本组进行中", uiState.phaseLabel)
        assertEquals("本组耗时", uiState.primaryMetricLabel)
        assertEquals("00:05", uiState.primaryMetricText)
        assertTrue(uiState.canCompleteSet)
        assertFalse(uiState.canStartSet)
        assertTrue(uiState.shortCue.isNotBlank())
    }

    @Test
    fun confirmStateOnlyOffersReadOnlyPlannedConfirmation() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 7).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        val uiState = state.toStrengthWorkoutSessionScreenState()

        assertEquals("确认记录", uiState.phaseLabel)
        assertEquals("完成本组", uiState.primaryMetricLabel)
        assertEquals("00:07", uiState.primaryMetricText)
        assertTrue(uiState.canConfirmPlanned)
        assertNotNull(uiState.confirmSummary)
        assertTrue(requireNotNull(uiState.confirmSummary).contains("按计划确认"))
        assertTrue(uiState.shortCue.contains("计划值"))
    }

    @Test
    fun restStateMapsCountdownAndNextSetTarget() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 3).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        state = StrengthWorkoutEngine.dispatch(
            state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        ).state
        val uiState = state.toStrengthWorkoutSessionScreenState()

        assertEquals("休息", uiState.phaseLabel)
        assertEquals("休息倒计时", uiState.primaryMetricLabel)
        assertTrue(uiState.primaryMetricText.startsWith("0"))
        assertTrue(uiState.nextSetLabel.startsWith("下一组"))
        assertTrue(uiState.nextSetLabel.contains("kg"))
        assertTrue(uiState.canStartNextDuringRest)
        assertFalse(uiState.canStartSet)
    }

    @Test
    fun completedAndAbandonedStatesMapLightweightTerminalCopy() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        repeat(state.setSteps.size) {
            state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
            state = StrengthWorkoutEngine.tick(state, seconds = 1).state
            state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
            state = StrengthWorkoutEngine.dispatch(
                state,
                WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
            ).state
            if (state.currentSessionStep?.kind == SessionStepKind.STRENGTH_REST) {
                state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
            }
        }
        val completedUiState = state.toStrengthWorkoutSessionScreenState()

        assertTrue(completedUiState.isTerminal)
        assertEquals("力量训练完成", completedUiState.terminalTitle)
        assertTrue(requireNotNull(completedUiState.terminalSummary).contains("已确认"))
        assertFalse(completedUiState.canEnd)

        val abandoned = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.dispatch(
                StrengthWorkoutEngine.create(plan),
                WorkoutCommand.StartSession
            ).state,
            WorkoutCommand.EndSession(reason = "user_requested")
        ).state
        val abandonedUiState = abandoned.toStrengthWorkoutSessionScreenState()

        assertTrue(abandonedUiState.isTerminal)
        assertEquals("力量训练已提前结束", abandonedUiState.terminalTitle)
        assertTrue(requireNotNull(abandonedUiState.terminalSummary).contains("用户主动结束"))
    }

    @Test
    fun heartRatePlaceholderStaysSecondaryAndAbstract() {
        val plan = buildDefaultPlanManagementState().plans[1]
        val started = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val uiState = started.toStrengthWorkoutSessionScreenState(
            heartRateState = HeartRateState(availability = HeartRateAvailability.NOT_CONNECTED)
        )

        assertEquals("-- bpm", uiState.heartRate.valueText)
        assertEquals("未连接设备", uiState.heartRate.statusText)
        assertFalse(uiState.heartRate.isAvailable)
    }
}
