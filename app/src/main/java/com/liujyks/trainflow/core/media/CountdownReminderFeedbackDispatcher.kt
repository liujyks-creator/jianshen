package com.liujyks.trainflow.core.media

import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.WorkoutEvent

internal object CountdownReminderFeedbackDispatcher {
    fun requestFor(
        event: WorkoutEvent,
        cueSettings: CueSettings?
    ): CountdownReminderFeedbackRequest? {
        val cue = when (event) {
            is WorkoutEvent.TimedWorkEnding -> cueSettings?.actionEnding
            is WorkoutEvent.RestEnding -> cueSettings?.restEnding
            else -> null
        }

        return requestFor(event = event, cue = cue)
    }

    fun requestFor(
        event: WorkoutEvent,
        cue: CountdownCue?
    ): CountdownReminderFeedbackRequest? {
        val cueType = when (event) {
            is WorkoutEvent.TimedWorkEnding -> CountdownReminderFeedbackType.ACTION_ENDING
            is WorkoutEvent.RestEnding -> CountdownReminderFeedbackType.REST_ENDING
            else -> return null
        }
        val remainingSec = when (event) {
            is WorkoutEvent.TimedWorkEnding -> event.remainingSec
            is WorkoutEvent.RestEnding -> event.remainingSec
            else -> return null
        }
        val activeCue = cue?.takeIf {
            it.enabled && remainingSec > 0 && remainingSec <= it.thresholdSec
        } ?: return null

        return CountdownReminderFeedbackRequest(
            type = cueType,
            remainingSec = remainingSec,
            soundEnabled = activeCue.soundEnabled,
            vibrationEnabled = activeCue.vibrationEnabled,
            emphasisAnimationEnabled = activeCue.emphasisAnimationEnabled
        )
    }
}

internal data class CountdownReminderFeedbackRequest(
    val type: CountdownReminderFeedbackType,
    val remainingSec: Int,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val emphasisAnimationEnabled: Boolean
)

internal enum class CountdownReminderFeedbackType {
    ACTION_ENDING,
    REST_ENDING
}
