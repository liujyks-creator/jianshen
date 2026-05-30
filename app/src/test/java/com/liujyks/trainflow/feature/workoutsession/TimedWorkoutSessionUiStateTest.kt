package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.HeartRateAvailability
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
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

    @Test
    fun actionEndingReminderMapsCueFlagsAndMessage() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                reminderPlan(
                    workSec = 6,
                    restSec = 4,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(
                            thresholdSec = 3,
                            soundEnabled = false,
                            vibrationEnabled = true,
                            emphasisAnimationEnabled = false
                        ),
                        restEnding = CountdownCue(thresholdSec = 2)
                    )
                )
            ),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 3).state
        val uiState = state.toTimedWorkoutSessionScreenState()

        assertEquals(TimedWorkoutCountdownReminderType.ACTION_ENDING, uiState.countdownReminder.type)
        assertEquals(3, uiState.countdownReminder.remainingSec)
        assertTrue(uiState.countdownReminder.message.contains("动作即将结束"))
        assertFalse(uiState.countdownReminder.soundEnabled)
        assertTrue(uiState.countdownReminder.vibrationEnabled)
        assertFalse(uiState.countdownReminder.emphasisAnimationEnabled)
        assertTrue(uiState.shortCue.contains("动作即将结束"))
    }

    @Test
    fun restEndingReminderIsDistinctFromActionEnding() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                reminderPlan(
                    workSec = 2,
                    restSec = 5,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(thresholdSec = 1),
                        restEnding = CountdownCue(
                            thresholdSec = 2,
                            soundEnabled = true,
                            vibrationEnabled = false,
                            emphasisAnimationEnabled = true
                        )
                    )
                )
            ),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        state = TimedWorkoutEngine.tick(state, seconds = 3).state
        val uiState = state.toTimedWorkoutSessionScreenState()

        assertEquals("休息", uiState.phaseLabel)
        assertEquals(TimedWorkoutCountdownReminderType.REST_ENDING, uiState.countdownReminder.type)
        assertEquals(2, uiState.countdownReminder.remainingSec)
        assertTrue(uiState.countdownReminder.message.contains("休息即将结束"))
        assertTrue(uiState.countdownReminder.soundEnabled)
        assertFalse(uiState.countdownReminder.vibrationEnabled)
        assertTrue(uiState.countdownReminder.emphasisAnimationEnabled)
    }

    @Test
    fun disabledAndTooLargeThresholdsDoNotCreateReminderState() {
        var disabledState = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                reminderPlan(
                    workSec = 4,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(enabled = false, thresholdSec = 3)
                    )
                )
            ),
            WorkoutCommand.StartSession
        ).state

        disabledState = TimedWorkoutEngine.tick(disabledState, seconds = 2).state
        assertFalse(disabledState.toTimedWorkoutSessionScreenState().countdownReminder.isActive)

        var shortDurationState = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                reminderPlan(
                    workSec = 3,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(thresholdSec = 5)
                    )
                )
            ),
            WorkoutCommand.StartSession
        ).state

        shortDurationState = TimedWorkoutEngine.tick(shortDurationState).state
        assertFalse(shortDurationState.toTimedWorkoutSessionScreenState().countdownReminder.isActive)
    }

    private fun reminderPlan(
        workSec: Int,
        restSec: Int? = null,
        cueSettings: CueSettings
    ): WorkoutPlan {
        return WorkoutPlan(
            id = "plan-reminder-test",
            mode = WorkoutMode.TIMED,
            title = "Reminder test",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "jump",
                            exerciseId = "jumping-jack",
                            workDurationSec = workSec,
                            restAfterSec = restSec
                        )
                    )
                )
            ),
            preferences = PlanPreferences(cueSettings = cueSettings),
            createdAt = "2026-05-30T00:00:00Z",
            updatedAt = "2026-05-30T00:00:00Z"
        )
    }
}
