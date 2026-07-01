package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngineResult
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngineState
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
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthWorkoutSoundCueRouteTest {
    @Test
    fun autoAfterRestTransitionRequestsStageBellFromRestCue() {
        val restState = runToRestState(
            setTimerMode = StrengthSetTimerMode.AUTO_AFTER_REST,
            cueSettings = CueSettings(restEnding = soundCue(soundEnabled = true))
        )
        val transition = StrengthWorkoutEngine.tick(restState.state, seconds = 3)

        val stageBellRequests = routeRequests(
            previousState = restState.state,
            result = transition,
            cueSettings = restState.cueSettings,
            isTickResult = true
        ).filter { request -> request.kind == WorkoutSoundCueKind.STAGE_BELL }

        assertEquals(SessionStepKind.STRENGTH_ACTIVE_SET, transition.state.currentSessionStep?.kind)
        assertEquals(1, stageBellRequests.size)
        assertEquals("strength_set_started:barbell-bench-press:bench-working-2", stageBellRequests.single().eventKey)
    }

    @Test
    fun initialPrepareDoesNotRequestStageBell() {
        val cueSettings = CueSettings(
            actionEnding = soundCue(soundEnabled = true),
            restEnding = soundCue(soundEnabled = true)
        )
        val initialState = StrengthWorkoutEngine.create(
            plan = twoSetPlan(
                setTimerMode = StrengthSetTimerMode.AUTO_AFTER_REST,
                cueSettings = cueSettings
            )
        )
        val result = StrengthWorkoutEngine.dispatch(initialState, WorkoutCommand.StartSession)

        val requests = routeRequests(
            previousState = initialState,
            result = result,
            cueSettings = cueSettings,
            isTickResult = false
        )

        assertEquals(SessionStepKind.STRENGTH_PREPARE_SET, result.state.currentSessionStep?.kind)
        assertTrue(result.events.any { event -> event is WorkoutEvent.StrengthSetReady })
        assertTrue(requests.none { request -> request.kind == WorkoutSoundCueKind.STAGE_BELL })
    }

    @Test
    fun manualStartStrengthSetDoesNotRequestTransitionBell() {
        val cueSettings = CueSettings(
            actionEnding = soundCue(soundEnabled = true),
            restEnding = soundCue(soundEnabled = true)
        )
        val started = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(
                plan = twoSetPlan(
                    setTimerMode = StrengthSetTimerMode.AUTO_AFTER_REST,
                    cueSettings = cueSettings
                )
            ),
            command = WorkoutCommand.StartSession
        )
        val result = StrengthWorkoutEngine.dispatch(started.state, WorkoutCommand.StartStrengthSet())

        val requests = routeRequests(
            previousState = started.state,
            result = result,
            cueSettings = cueSettings,
            isTickResult = false
        )

        assertEquals(SessionStepKind.STRENGTH_ACTIVE_SET, result.state.currentSessionStep?.kind)
        assertTrue(result.events.any { event -> event is WorkoutEvent.StrengthSetStarted })
        assertTrue(requests.none { request -> request.kind == WorkoutSoundCueKind.STAGE_BELL })
    }

    @Test
    fun earlyStartDuringRestDoesNotRequestTransitionBell() {
        val restState = runToRestState(
            setTimerMode = StrengthSetTimerMode.AUTO_AFTER_REST,
            cueSettings = CueSettings(restEnding = soundCue(soundEnabled = true))
        )

        val result = StrengthWorkoutEngine.dispatch(restState.state, WorkoutCommand.StartStrengthSet())
        val requests = routeRequests(
            previousState = restState.state,
            result = result,
            cueSettings = restState.cueSettings,
            isTickResult = false
        )

        assertEquals(SessionStepKind.STRENGTH_ACTIVE_SET, result.state.currentSessionStep?.kind)
        assertTrue(result.events.any { event -> event is WorkoutEvent.StrengthSetStarted })
        assertTrue(requests.none { request -> request.kind == WorkoutSoundCueKind.STAGE_BELL })
    }

    @Test
    fun manualStartRestEndDoesNotRequestAutoTransitionBell() {
        val cueSettings = CueSettings(
            actionEnding = soundCue(soundEnabled = true),
            restEnding = soundCue(soundEnabled = true)
        )
        val restState = runToRestState(
            setTimerMode = StrengthSetTimerMode.MANUAL_START,
            cueSettings = cueSettings,
            secondExerciseId = "dumbbell-row"
        )
        val transition = StrengthWorkoutEngine.tick(restState.state, seconds = 3)

        assertEquals(SessionStepKind.STRENGTH_PREPARE_SET, transition.state.currentSessionStep?.kind)
        assertTrue(transition.events.none { event -> event is WorkoutEvent.StrengthSetStarted })

        val requests = routeRequests(
            previousState = restState.state,
            result = transition,
            cueSettings = cueSettings,
            isTickResult = true
        )

        assertTrue(transition.events.any { event -> event is WorkoutEvent.NextExerciseReady })
        assertTrue(transition.events.any { event -> event is WorkoutEvent.StrengthSetReady })
        assertTrue(requests.none { request -> request.kind == WorkoutSoundCueKind.STAGE_BELL })
    }

    @Test
    fun soundDisabledBlocksAutoAfterRestTransitionBell() {
        val restState = runToRestState(
            setTimerMode = StrengthSetTimerMode.AUTO_AFTER_REST,
            cueSettings = CueSettings(restEnding = soundCue(soundEnabled = false))
        )
        val transition = StrengthWorkoutEngine.tick(restState.state, seconds = 3)

        val requests = routeRequests(
            previousState = restState.state,
            result = transition,
            cueSettings = restState.cueSettings,
            isTickResult = true
        )

        assertTrue(transition.events.any { event -> event is WorkoutEvent.StrengthSetStarted })
        assertTrue(requests.none { request -> request.kind == WorkoutSoundCueKind.STAGE_BELL })
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

    private fun routeRequests(
        previousState: StrengthWorkoutEngineState,
        result: StrengthWorkoutEngineResult,
        cueSettings: CueSettings,
        isTickResult: Boolean
    ) = result.events.mapNotNull { event ->
        val naturalRestTickTransition = isTickResult &&
            previousState.currentStepKind == SessionStepKind.STRENGTH_REST
        strengthWorkoutSoundCueRequestFor(
            event = event,
            cueSettings = cueSettings,
            naturalRestTickTransition = naturalRestTickTransition,
            autoAfterRestTransition = naturalRestTickTransition &&
                result.state.currentStepKind == SessionStepKind.STRENGTH_ACTIVE_SET
        )
    }

    private fun runToRestState(
        setTimerMode: StrengthSetTimerMode,
        cueSettings: CueSettings,
        secondExerciseId: String = "barbell-bench-press"
    ): StrengthCueTransition {
        var result = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(
                plan = twoSetPlan(
                    setTimerMode = setTimerMode,
                    cueSettings = cueSettings,
                    secondExerciseId = secondExerciseId
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
            result = result,
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
        cueSettings: CueSettings,
        secondExerciseId: String = "barbell-bench-press"
    ): WorkoutPlan {
        val blocks = if (secondExerciseId == "barbell-bench-press") {
            listOf(
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
            )
        } else {
            listOf(
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
                        )
                    ),
                    setTimerMode = setTimerMode
                ),
                StrengthExerciseBlock(
                    id = "row",
                    order = 2,
                    exerciseId = secondExerciseId,
                    sets = listOf(
                        StrengthSetPlan(
                            id = "row-working-1",
                            order = 1,
                            kind = StrengthSetKind.WORKING,
                            restAfterSec = 0
                        )
                    ),
                    setTimerMode = setTimerMode
                )
            )
        }
        return WorkoutPlan(
            id = "strength-sound-cue-route-test",
            mode = WorkoutMode.STRENGTH,
            title = "Strength sound cue route test",
            blocks = blocks,
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
