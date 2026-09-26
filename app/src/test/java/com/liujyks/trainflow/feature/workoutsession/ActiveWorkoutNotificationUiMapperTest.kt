package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.feature.followalong.buildDefaultFollowAlongScreenState
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWorkoutNotificationUiMapperTest {
    @Test
    fun timedSessionMapsActiveSummaryWithoutEngineNotificationDependency() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val engineState = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val uiState = engineState.toTimedWorkoutSessionScreenState()

        val notification = timedActiveWorkoutNotificationState(
            planId = plan.id,
            status = engineState.status,
            uiState = uiState
        )

        assertEquals("timed:${plan.id}", notification.sessionKey)
        assertEquals(WorkoutMode.TIMED, notification.mode)
        assertEquals(SessionStatus.ACTIVE, notification.status)
        assertEquals(uiState.currentTitle, notification.primaryText)
        assertEquals(uiState.timerText, notification.timerText)
        assertTrue(notification.secondaryText.contains("下一步"))
    }

    @Test
    fun strengthSessionMapsSetSummary() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var engineState = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        engineState = StrengthWorkoutEngine.dispatch(engineState, WorkoutCommand.StartStrengthSet()).state
        engineState = StrengthWorkoutEngine.tick(engineState, seconds = 5).state
        val uiState = engineState.toStrengthWorkoutSessionScreenState()

        val notification = strengthActiveWorkoutNotificationState(
            planId = plan.id,
            status = engineState.status,
            uiState = uiState
        )

        assertEquals("strength:${plan.id}", notification.sessionKey)
        assertEquals(WorkoutMode.STRENGTH, notification.mode)
        assertEquals("本组进行中", notification.phaseLabel)
        assertEquals(uiState.currentExerciseName, notification.primaryText)
        assertEquals("00:05", notification.timerText)
        assertTrue(notification.secondaryText.contains("本组耗时"))
    }

    @Test
    fun followAlongSessionMapsElapsedSummary() {
        val uiState = buildFollowAlongWorkoutSessionUiState(elapsedSec = 65, active = true)

        val notification = followAlongActiveWorkoutNotificationState(
            sessionId = "free-session-1",
            status = SessionStatus.ACTIVE,
            uiState = uiState
        )

        assertEquals("follow_along:free-session-1", notification.sessionKey)
        assertEquals(WorkoutMode.FOLLOW_ALONG, notification.mode)
        assertEquals("跟练中", notification.phaseLabel)
        assertEquals("跟练", notification.primaryText)
        assertEquals("01:05", notification.timerText)
        assertEquals("", notification.progressText)
        assertEquals("整场时间", notification.secondaryText)
        assertFalse(notification.secondaryText.contains("下一动作"))
        assertFalse(notification.secondaryText.contains("轮次"))
        assertFalse(notification.secondaryText.contains("语音"))
        assertFalse(notification.secondaryText.contains("媒体播放"))
    }

    @Test
    fun terminalSessionMapsClearHintInsteadOfOngoingCopy() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val completed = TimedWorkoutEngine.tick(
            TimedWorkoutEngine.dispatch(
                TimedWorkoutEngine.create(plan),
                WorkoutCommand.StartSession
            ).state,
            seconds = 10_000
        ).state
        val uiState = completed.toTimedWorkoutSessionScreenState()

        val notification = timedActiveWorkoutNotificationState(
            planId = plan.id,
            status = completed.status,
            uiState = uiState
        )

        assertEquals(SessionStatus.COMPLETED, notification.status)
        assertTrue(notification.secondaryText.contains("会清理"))
        assertFalse(notification.secondaryText.contains("仍在进行"))
    }
}
