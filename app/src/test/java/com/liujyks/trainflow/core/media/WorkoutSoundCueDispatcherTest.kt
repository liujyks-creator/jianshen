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
    fun finalCountdownMapsEveryRemainingSecondToBeep() {
        val cue = CountdownCue(thresholdSec = 3, soundEnabled = true)

        val requests = (3 downTo 1).map { remainingSec ->
            requireNotNull(
                WorkoutSoundCueDispatcher.requestFor(
                    event = WorkoutEvent.TimedWorkEnding(stepId = "work-1", remainingSec = remainingSec),
                    cue = cue
                )
            )
        }

        assertEquals(
            listOf(
                WorkoutSoundCueKind.COUNTDOWN_BEEP,
                WorkoutSoundCueKind.COUNTDOWN_BEEP,
                WorkoutSoundCueKind.COUNTDOWN_BEEP
            ),
            requests.map { request -> request.kind }
        )
        assertEquals(
            List(3) { WorkoutSoundCueSource.ACTION_ENDING },
            requests.map { request -> request.source }
        )
    }

    @Test
    fun timedPhaseStartedEventsMapToStageBells() {
        val cue = CountdownCue(soundEnabled = true)

        val workStarted = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.TimedWorkStarted(stepId = "work-1"),
            cue = cue
        )
        val restStarted = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.RestStarted(stepId = "rest-1", durationSec = 20),
            cue = cue
        )

        requireNotNull(workStarted)
        requireNotNull(restStarted)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, workStarted.kind)
        assertEquals(WorkoutSoundCueSource.STAGE_TRANSITION, workStarted.source)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, restStarted.kind)
        assertEquals(WorkoutSoundCueSource.REST_STARTED, restStarted.source)
    }

    @Test
    fun strengthPhaseReadyEventsMapToStageBell() {
        val cue = CountdownCue(soundEnabled = true)

        val strengthReady = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.StrengthSetReady(exerciseId = "squat", setPlanId = "set-1"),
            cue = cue
        )

        requireNotNull(strengthReady)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, strengthReady.kind)
        assertEquals(WorkoutSoundCueSource.STAGE_TRANSITION, strengthReady.source)
    }

    @Test
    fun sessionCompletedMapsToStageBellForFinalZeroSecondBoundary() {
        val cue = CountdownCue(soundEnabled = true)

        val completed = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.SessionCompleted(sessionId = "session-1"),
            cue = cue
        )

        requireNotNull(completed)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, completed.kind)
        assertEquals(WorkoutSoundCueSource.STAGE_TRANSITION, completed.source)
    }

    @Test
    fun restEventsMapToRestSoundRequests() {
        val cue = CountdownCue(thresholdSec = 4, soundEnabled = true)

        val restEnding = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.RestEnding(stepId = "rest-1", remainingSec = 3),
            cue = cue
        )

        requireNotNull(restEnding)
        assertEquals(WorkoutSoundCueKind.COUNTDOWN_BEEP, restEnding.kind)
        assertEquals(WorkoutSoundCueSource.REST_ENDING, restEnding.source)
    }

    @Test
    fun strengthRestEndingDispatchesCountdownBeepForFinalFiveSeconds() {
        val player = FakeWorkoutSoundCuePlayer()
        val controller = WorkoutSoundCueController(player)
        val cueSettings = CueSettings(
            restEnding = CountdownCue(thresholdSec = 5, soundEnabled = true)
        )

        (5 downTo 1).forEach { remainingSec ->
            val event = WorkoutEvent.RestEnding(
                stepId = "strength-rest-1",
                remainingSec = remainingSec
            )
            controller.dispatch(
                WorkoutSoundCueDispatcher.requestFor(
                    event = event,
                    cue = WorkoutSoundCueDispatcher.cueFor(
                        event = event,
                        cueSettings = cueSettings
                    )
                )
            )
        }

        assertEquals(
            List(5) { WorkoutSoundCueKind.COUNTDOWN_BEEP },
            player.playedKinds
        )
    }

    @Test
    fun strengthRestEndingDoesNotDispatchCountdownBeepWhenSoundDisabled() {
        val cueSettings = CueSettings(
            restEnding = CountdownCue(thresholdSec = 5, soundEnabled = false)
        )
        val event = WorkoutEvent.RestEnding(
            stepId = "strength-rest-1",
            remainingSec = 5
        )

        val request = WorkoutSoundCueDispatcher.requestFor(
            event = event,
            cue = WorkoutSoundCueDispatcher.cueFor(event = event, cueSettings = cueSettings)
        )

        assertNull(request)
    }

    @Test
    fun strengthNonRestEventsDoNotDispatchCountdownBeep() {
        val cueSettings = CueSettings(
            actionEnding = CountdownCue(thresholdSec = 5, soundEnabled = true),
            restEnding = CountdownCue(thresholdSec = 5, soundEnabled = true)
        )

        val readyRequest = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.StrengthSetReady(exerciseId = "squat", setPlanId = "set-1"),
            cue = WorkoutSoundCueDispatcher.cueFor(
                event = WorkoutEvent.StrengthSetReady(exerciseId = "squat", setPlanId = "set-1"),
                cueSettings = cueSettings
            )
        )
        val startedRequest = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.StrengthSetStarted(exerciseId = "squat", setPlanId = "set-1"),
            cue = WorkoutSoundCueDispatcher.cueFor(
                event = WorkoutEvent.StrengthSetStarted(exerciseId = "squat", setPlanId = "set-1"),
                cueSettings = cueSettings
            )
        )
        val completedRequest = WorkoutSoundCueDispatcher.requestFor(
            event = WorkoutEvent.StrengthSetCompleted(setRecordId = "set-1-record"),
            cue = WorkoutSoundCueDispatcher.cueFor(
                event = WorkoutEvent.StrengthSetCompleted(setRecordId = "set-1-record"),
                cueSettings = cueSettings
            )
        )

        requireNotNull(readyRequest)
        assertEquals(WorkoutSoundCueKind.STAGE_BELL, readyRequest.kind)
        assertNull(startedRequest)
        assertNull(completedRequest)
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
        assertEquals("USAGE_MEDIA", policy.usageLabel)
        assertEquals("CONTENT_TYPE_MUSIC", policy.contentTypeLabel)
    }
}

private class FakeWorkoutSoundCuePlayer : WorkoutSoundCuePlayer {
    val playedKinds = mutableListOf<WorkoutSoundCueKind>()

    override fun play(kind: WorkoutSoundCueKind) {
        playedKinds += kind
    }
}
