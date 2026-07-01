package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngineResult
import com.liujyks.trainflow.core.media.WorkoutSoundCueDispatcher
import com.liujyks.trainflow.core.media.WorkoutSoundCueKind
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetCompletionInput
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutEvent
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthWorkoutSoundCueRouteTest {
    @Test
    fun autoAfterRestTransitionRequestsStageBellFromRestCue() {
        val transition = runToRestEnd(
            setTimerMode = StrengthSetTimerMode.AUTO_AFTER_REST,
            restCue = soundCue(soundEnabled = true)
        )
        val started = transition.events.filterIsInstance<WorkoutEvent.StrengthSetStarted>().single()

        val request = WorkoutSoundCueDispatcher.requestFor(
            event = started,
            cue = strengthWorkoutSoundCueFor(
                event = started,
                cueSettings = transition.cueSettings,
                autoAfterRestTransition = true
            )
        )

        assertNotNull(request)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, request?.kind)
        assertEquals("strength_set_started:barbell-bench-press:bench-working-2", request?.eventKey)
    }

    @Test
    fun manualStartRestEndDoesNotRequestActiveStartBell() {
        val transition = runToRestEnd(
            setTimerMode = StrengthSetTimerMode.MANUAL_START,
            restCue = soundCue(soundEnabled = true)
        )

        assertEquals(SessionStepKind.STRENGTH_PREPARE_SET, transition.state.currentSessionStep?.kind)
        assertTrue(transition.events.none { event -> event is WorkoutEvent.StrengthSetStarted })

        val requests = transition.events.mapNotNull { event ->
            WorkoutSoundCueDispatcher.requestFor(
                event = event,
                cue = strengthWorkoutSoundCueFor(
                    event = event,
                    cueSettings = transition.cueSettings,
                    autoAfterRestTransition = false
                )
            )
        }

        assertTrue(requests.none { request -> request.eventKey.startsWith("strength_set_started:") })
    }

    @Test
    fun soundDisabledBlocksAutoAfterRestTransitionBell() {
        val transition = runToRestEnd(
            setTimerMode = StrengthSetTimerMode.AUTO_AFTER_REST,
            restCue = soundCue(soundEnabled = false)
        )
        val started = transition.events.filterIsInstance<WorkoutEvent.StrengthSetStarted>().single()

        val request = WorkoutSoundCueDispatcher.requestFor(
            event = started,
            cue = strengthWorkoutSoundCueFor(
                event = started,
                cueSettings = transition.cueSettings,
                autoAfterRestTransition = true
            )
        )

        assertNull(request)
    }

    @Test
    fun strengthRestCountdownBeepsStillCoverFiveToOne() {
        val cueSettings = CueSettings(restEnding = soundCue(soundEnabled = true))

        val requests = (5 downTo 1).mapNotNull { remainingSec ->
            val event = WorkoutEvent.RestEnding(stepId = "strength-rest", remainingSec = remainingSec)
            WorkoutSoundCueDispatcher.requestFor(
                event = event,
                cue = strengthWorkoutSoundCueFor(
                    event = event,
                    cueSettings = cueSettings
                )
            )
        }

        assertEquals(List(5) { WorkoutSoundCueKind.COUNTDOWN_BEEP }, requests.map { request -> request.kind })
    }

    private fun runToRestEnd(
        setTimerMode: StrengthSetTimerMode,
        restCue: CountdownCue
    ): StrengthCueTransition {
        val cueSettings = CueSettings(restEnding = restCue)
        var result = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(
                plan = twoSetPlan(
                    setTimerMode = setTimerMode,
                    cueSettings = cueSettings
                )
            ),
            command = WorkoutCommand.StartSession
        )
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.StartStrengthSet())
        result = StrengthWorkoutEngine.tick(result.state, seconds = 2)
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.CompleteStrengthSet())
        result = StrengthWorkoutEngine.dispatch(
            result.state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        )

        return StrengthCueTransition(
            result = StrengthWorkoutEngine.tick(result.state, seconds = 3),
            cueSettings = cueSettings
        )
    }

    private data class StrengthCueTransition(
        val result: StrengthWorkoutEngineResult,
        val cueSettings: CueSettings
    ) {
        val state = result.state
        val events = result.events
    }

    private fun twoSetPlan(
        setTimerMode: StrengthSetTimerMode,
        cueSettings: CueSettings
    ): WorkoutPlan {
        return WorkoutPlan(
            id = "strength-sound-cue-route-test",
            mode = WorkoutMode.STRENGTH,
            title = "Strength sound cue route test",
            blocks = listOf(
                StrengthExerciseBlock(
                    id = "bench",
                    order = 1,
                    exerciseId = "barbell-bench-press",
                    sets = listOf(
                        StrengthSetPlan(
                            id = "bench-working-1",
                            order = 1,
                            kind = StrengthSetKind.WORKING,
                            restAfterSec = 3
                        ),
                        StrengthSetPlan(
                            id = "bench-working-2",
                            order = 2,
                            kind = StrengthSetKind.WORKING,
                            restAfterSec = 0
                        )
                    ),
                    setTimerMode = setTimerMode
                )
            ),
            preferences = PlanPreferences(cueSettings = cueSettings),
            createdAt = "2026-07-01T00:00:00Z",
            updatedAt = "2026-07-01T00:00:00Z"
        )
    }

    private fun soundCue(soundEnabled: Boolean): CountdownCue {
        return CountdownCue(
            enabled = true,
            thresholdSec = 5,
            soundEnabled = soundEnabled,
            vibrationEnabled = false,
            emphasisAnimationEnabled = false
        )
    }
}
