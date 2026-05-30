package com.liujyks.trainflow.core.media

import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.WorkoutEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountdownReminderFeedbackDispatcherTest {
    @Test
    fun actionEndingUsesActionCuePreferences() {
        val request = CountdownReminderFeedbackDispatcher.requestFor(
            event = WorkoutEvent.TimedWorkEnding(stepId = "work-1", remainingSec = 3),
            cueSettings = CueSettings(
                actionEnding = CountdownCue(
                    thresholdSec = 5,
                    soundEnabled = false,
                    vibrationEnabled = true,
                    emphasisAnimationEnabled = false
                ),
                restEnding = CountdownCue(
                    thresholdSec = 5,
                    soundEnabled = true,
                    vibrationEnabled = false,
                    emphasisAnimationEnabled = true
                )
            )
        )

        requireNotNull(request)
        assertEquals(CountdownReminderFeedbackType.ACTION_ENDING, request.type)
        assertEquals(3, request.remainingSec)
        assertFalse(request.soundEnabled)
        assertTrue(request.vibrationEnabled)
        assertFalse(request.emphasisAnimationEnabled)
    }

    @Test
    fun restEndingUsesRestCuePreferences() {
        val request = CountdownReminderFeedbackDispatcher.requestFor(
            event = WorkoutEvent.RestEnding(stepId = "rest-1", remainingSec = 2),
            cueSettings = CueSettings(
                actionEnding = CountdownCue(
                    thresholdSec = 5,
                    soundEnabled = false,
                    vibrationEnabled = false,
                    emphasisAnimationEnabled = false
                ),
                restEnding = CountdownCue(
                    thresholdSec = 4,
                    soundEnabled = true,
                    vibrationEnabled = true,
                    emphasisAnimationEnabled = true
                )
            )
        )

        requireNotNull(request)
        assertEquals(CountdownReminderFeedbackType.REST_ENDING, request.type)
        assertEquals(2, request.remainingSec)
        assertTrue(request.soundEnabled)
        assertTrue(request.vibrationEnabled)
        assertTrue(request.emphasisAnimationEnabled)
    }

    @Test
    fun disabledOrOutOfThresholdCueProducesNoFeedback() {
        assertNull(
            CountdownReminderFeedbackDispatcher.requestFor(
                event = WorkoutEvent.TimedWorkEnding(stepId = "work-1", remainingSec = 3),
                cue = CountdownCue(enabled = false, thresholdSec = 5)
            )
        )
        assertNull(
            CountdownReminderFeedbackDispatcher.requestFor(
                event = WorkoutEvent.RestEnding(stepId = "rest-1", remainingSec = 6),
                cue = CountdownCue(enabled = true, thresholdSec = 5)
            )
        )
        assertNull(
            CountdownReminderFeedbackDispatcher.requestFor(
                event = WorkoutEvent.SessionPaused(sessionId = "session-1"),
                cue = CountdownCue()
            )
        )
    }
}
