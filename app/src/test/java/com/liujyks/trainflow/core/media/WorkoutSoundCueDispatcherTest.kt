package com.liujyks.trainflow.core.media

import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.WorkoutEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutSoundCueDispatcherTest {
    @Test
    fun finalCountdownMapsToBeepBeforeLastSecondAndBellAtOneSecond() {
        val cue = CountdownCue(thresholdSec = 5, soundEnabled = true)

        val fiveSecondRequest = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.TimedWorkEnding(stepId = "work-1", remainingSec = 5),
            cue = cue
        )
        val oneSecondRequest = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.TimedWorkEnding(stepId = "work-1", remainingSec = 1),
            cue = cue
        )

        requireNotNull(fiveSecondRequest)
        requireNotNull(oneSecondRequest)
        assertEquals(WorkoutSoundCueKind.COUNTDOWN_BEEP, fiveSecondRequest.kind)
        assertEquals(WorkoutSoundCueSource.ACTION_ENDING, fiveSecondRequest.source)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, oneSecondRequest.kind)
        assertEquals(WorkoutSoundCueSource.ACTION_ENDING, oneSecondRequest.source)
    }

    @Test
    fun phaseTransitionEventsMapToStageBell() {
        val cue = CountdownCue(soundEnabled = true)

        val workStarted = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.TimedWorkStarted(stepId = "work-1"),
            cue = cue
        )
        val strengthReady = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.StrengthSetReady(exerciseId = "squat", setPlanId = "set-1"),
            cue = cue
        )

        requireNotNull(workStarted)
        requireNotNull(strengthReady)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, workStarted.kind)
        assertEquals(WorkoutSoundCueSource.STAGE_TRANSITION, workStarted.source)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, strengthReady.kind)
        assertEquals(WorkoutSoundCueSource.STAGE_TRANSITION, strengthReady.source)
    }

    @Test
    fun restEventsMapToRestSoundRequests() {
        val cue = CountdownCue(thresholdSec = 4, soundEnabled = true)

        val restStarted = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.RestStarted(stepId = "rest-1", durationSec = 20),
            cue = cue
        )
        val restEnding = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.RestEnding(stepId = "rest-1", remainingSec = 3),
            cue = cue
        )

        requireNotNull(restStarted)
        requireNotNull(restEnding)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, restStarted.kind)
        assertEquals(WorkoutSoundCueSource.REST_STARTED, restStarted.source)
        assertEquals(WorkoutSoundCueKind.COUNTDOWN_BEEP, restEnding.kind)
        assertEquals(WorkoutSoundCueSource.REST_ENDING, restEnding.source)
    }

    @Test
    fun soundDisabledOrOutOfThresholdDoesNotRequestPlayback() {
        assertNull(
            WorkoutSoundCueDispatcher.requestFor(
                event = WorkoutEvent.TimedWorkEnding(stepId = "work-1", remainingSec = 3),
                cue = CountdownCue(thresholdSec = 5, soundEnabled = false)
            )
        )
        assertNull(
            WorkoutSoundCueDispatcher.requestFor(
                event = WorkoutEvent.RestStarted(stepId = "rest-1", durationSec = 30),
                cue = CountdownCue(enabled = false, soundEnabled = true)
            )
        )
        assertNull(
            WorkoutSoundCueDispatcher.requestFor(
                event = WorkoutEvent.RestEnding(stepId = "rest-1", remainingSec = 7),
                cue = CountdownCue(thresholdSec = 5, soundEnabled = true)
            )
        )
    }

    @Test
    fun cueSettingsSelectActionAndRestBoundaries() {
        val cueSettings = CueSettings(
            actionEnding = CountdownCue(thresholdSec = 5, soundEnabled = true),
            restEnding = CountdownCue(thresholdSec = 3, soundEnabled = false)
        )

        val actionCue = WorkoutSoundCueDispatcher.cueFor(
            event = WorkoutEvent.TimedWorkEnding(stepId = "work-1", remainingSec = 2),
            cueSettings = cueSettings
        )
        val restCue = WorkoutSoundCueDispatcher.cueFor(
            event = WorkoutEvent.RestEnding(stepId = "rest-1", remainingSec = 2),
            cueSettings = cueSettings
        )

        requireNotNull(actionCue)
        requireNotNull(restCue)
        assertEquals(5, actionCue.thresholdSec)
        assertEquals(3, restCue.thresholdSec)
        assertFalse(restCue.soundEnabled)
    }

    @Test
    fun repeatedEventKeyDoesNotTriggerRepeatedPlayback() {
        val player = FakeWorkoutSoundCuePlayer()
        val controller = WorkoutSoundCueController(player)
        val request = WorkoutSoundCueRequest(
            kind = WorkoutSoundCueKind.COUNTDOWN_BEEP,
            eventKey = "timed_work_ending:work-1:5",
            remainingSec = 5,
            source = WorkoutSoundCueSource.ACTION_ENDING,
            audioPolicy = WorkoutSoundCueAudioPolicy.coexistencePolicy
        )

        controller.dispatch(request)
        controller.dispatch(request)

        assertEquals(listOf(WorkoutSoundCueKind.COUNTDOWN_BEEP), player.playedKinds)
    }

    @Test
    fun audioCoexistencePolicyDoesNotRequestDisruptiveFocusOrDucking() {
        val policy = WorkoutSoundCueAudioPolicy.coexistencePolicy

        assertFalse(policy.requestsAudioFocus)
        assertFalse(policy.allowsDucking)
        assertFalse(policy.pausesExternalAudio)
        assertEquals("USAGE_ASSISTANCE_SONIFICATION", policy.usageLabel)
        assertEquals("CONTENT_TYPE_SONIFICATION", policy.contentTypeLabel)
    }
}

private class FakeWorkoutSoundCuePlayer : WorkoutSoundCuePlayer {
    val playedKinds = mutableListOf<WorkoutSoundCueKind>()

    override fun play(kind: WorkoutSoundCueKind) {
        playedKinds += kind
    }
}
