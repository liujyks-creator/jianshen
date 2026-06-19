package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineResult
import com.liujyks.trainflow.core.media.WorkoutSoundCueDispatcher
import com.liujyks.trainflow.core.media.WorkoutSoundCueKind
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutEvent
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedWorkoutSoundCueRouteTest {
    @Test
    fun transitionBellUsesPreviousCompletedStepCueWhenNextStepSoundIsDisabled() {
        val transition = runToFirstRest(
            actionCue = soundCue(soundEnabled = true),
            restCue = soundCue(soundEnabled = false)
        )
        val restStarted = transition.events.filterIsInstance<WorkoutEvent.RestStarted>().single()

        val request = WorkoutSoundCueDispatcher.requestFor(
            event = restStarted,
            cue = transition.state.soundCueFor(restStarted)
        )

        assertNotNull(request)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, request?.kind)
    }

    @Test
    fun transitionBellDoesNotUseNextStepCueWhenPreviousStepSoundIsDisabled() {
        val transition = runToFirstRest(
            actionCue = soundCue(soundEnabled = false),
            restCue = soundCue(soundEnabled = true)
        )
        val restStarted = transition.events.filterIsInstance<WorkoutEvent.RestStarted>().single()

        val request = WorkoutSoundCueDispatcher.requestFor(
            event = restStarted,
            cue = transition.state.soundCueFor(restStarted)
        )

        assertNull(request)
    }

    @Test
    fun initialTimedStageStartHasNoTransitionBellCue() {
        val started = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(twoStepPlan(soundCue(), soundCue())),
            command = WorkoutCommand.StartSession
        )
        val initialWorkStarted = started.events.filterIsInstance<WorkoutEvent.TimedWorkStarted>().single()

        val request = WorkoutSoundCueDispatcher.requestFor(
            event = initialWorkStarted,
            cue = started.state.soundCueFor(initialWorkStarted)
        )

        assertNull(request)
    }

    @Test
    fun skippedPreviousStepDoesNotOwnTransitionBell() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(twoStepPlan(soundCue(), soundCue())),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.SkipStep)
        val restStarted = result.events.filterIsInstance<WorkoutEvent.RestStarted>().single()

        val request = WorkoutSoundCueDispatcher.requestFor(
            event = restStarted,
            cue = result.state.soundCueFor(restStarted)
        )

        assertNull(request)
    }

    @Test
    fun finalStageCompletionKeepsFinalCompletedStepBellCue() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(singleWorkPlan(actionCue = soundCue(soundEnabled = true))),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.tick(result.state, seconds = 1)
        val completed = result.events.filterIsInstance<WorkoutEvent.SessionCompleted>().single()

        val request = WorkoutSoundCueDispatcher.requestFor(
            event = completed,
            cue = result.state.soundCueFor(completed)
        )

        assertNotNull(request)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, request?.kind)
    }

    @Test
    fun previousEnabledStageStillCountsDownBeforeOwningTransitionBell() {
        val started = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(twoStepPlan(soundCue(soundEnabled = true), soundCue(soundEnabled = false))),
            command = WorkoutCommand.StartSession
        )
        val endingCue = started.events.filterIsInstance<WorkoutEvent.TimedWorkEnding>().single()

        val request = WorkoutSoundCueDispatcher.requestFor(
            event = endingCue,
            cue = started.state.soundCueFor(endingCue)
        )

        assertNotNull(request)
        assertEquals(WorkoutSoundCueKind.COUNTDOWN_BEEP, request?.kind)
        assertTrue(endingCue.remainingSec > 0)
    }

    private fun runToFirstRest(
        actionCue: CountdownCue,
        restCue: CountdownCue
    ): TimedWorkoutEngineResult {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(twoStepPlan(actionCue = actionCue, restCue = restCue)),
            command = WorkoutCommand.StartSession
        )
        result = TimedWorkoutEngine.tick(result.state, seconds = 1)
        return result
    }

    private fun twoStepPlan(
        actionCue: CountdownCue,
        restCue: CountdownCue
    ): WorkoutPlan {
        return singleWorkPlan(
            actionCue = actionCue,
            restCue = restCue,
            restAfterSec = 4
        )
    }

    private fun singleWorkPlan(
        actionCue: CountdownCue,
        restCue: CountdownCue? = null,
        restAfterSec: Int? = null
    ): WorkoutPlan {
        return WorkoutPlan(
            id = "sound-cue-route-test",
            mode = WorkoutMode.TIMED,
            title = "Sound cue route test",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "work",
                            labelOverride = "Work",
                            workDurationSec = 1,
                            restAfterSec = restAfterSec,
                            cueSettings = CueSettings(
                                actionEnding = actionCue,
                                restEnding = restCue
                            )
                        )
                    )
                )
            ),
            preferences = PlanPreferences(),
            createdAt = "2026-06-19T00:00:00Z",
            updatedAt = "2026-06-19T00:00:00Z"
        )
    }

    private fun soundCue(soundEnabled: Boolean = true): CountdownCue {
        return CountdownCue(
            enabled = true,
            thresholdSec = 5,
            soundEnabled = soundEnabled,
            vibrationEnabled = false,
            emphasisAnimationEnabled = false
        )
    }
}
