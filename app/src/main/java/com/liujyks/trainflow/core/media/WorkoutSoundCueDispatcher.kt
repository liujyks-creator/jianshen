package com.liujyks.trainflow.core.media

import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.WorkoutEvent

internal object WorkoutSoundCueDispatcher {
    fun requestFor(event: WorkoutEvent, cue: CountdownCue?): WorkoutSoundCueRequest? {
        return when (event) {
            is WorkoutEvent.TimedWorkEnding -> countdownRequest(
                eventKey = "timed_work_ending:${event.stepId}:${event.remainingSec}",
                remainingSec = event.remainingSec,
                cue = cue,
                source = WorkoutSoundCueSource.ACTION_ENDING
            )
            is WorkoutEvent.RestEnding -> countdownRequest(
                eventKey = "rest_ending:${event.stepId}:${event.remainingSec}",
                remainingSec = event.remainingSec,
                cue = cue,
                source = WorkoutSoundCueSource.REST_ENDING
            )
            is WorkoutEvent.TimedWorkStarted -> stageTransitionRequest(
                eventKey = "timed_work_started:${event.stepId}",
                cue = cue,
                source = WorkoutSoundCueSource.STAGE_TRANSITION
            )
            is WorkoutEvent.RestStarted -> stageTransitionRequest(
                eventKey = "rest_started:${event.stepId}",
                cue = cue,
                source = WorkoutSoundCueSource.REST_STARTED
            )
            is WorkoutEvent.StrengthSetReady -> stageTransitionRequest(
                eventKey = "strength_set_ready:${event.exerciseId}:${event.setPlanId.orEmpty()}",
                cue = cue,
                source = WorkoutSoundCueSource.STAGE_TRANSITION
            )
            is WorkoutEvent.NextExerciseReady -> stageTransitionRequest(
                eventKey = "next_exercise_ready:${event.exerciseId}",
                cue = cue,
                source = WorkoutSoundCueSource.STAGE_TRANSITION
            )
            is WorkoutEvent.SessionCompleted -> stageTransitionRequest(
                eventKey = "session_completed:${event.sessionId}",
                cue = cue,
                source = WorkoutSoundCueSource.STAGE_TRANSITION
            )
            else -> null
        }
    }

    fun cueFor(event: WorkoutEvent, cueSettings: CueSettings?): CountdownCue? {
        return when (event) {
            is WorkoutEvent.TimedWorkStarted,
            is WorkoutEvent.TimedWorkEnding,
            is WorkoutEvent.StrengthSetReady,
            is WorkoutEvent.NextExerciseReady -> cueSettings?.actionEnding
            is WorkoutEvent.RestStarted,
            is WorkoutEvent.RestEnding -> cueSettings?.restEnding
            else -> null
        }
    }

    private fun countdownRequest(
        eventKey: String,
        remainingSec: Int,
        cue: CountdownCue?,
        source: WorkoutSoundCueSource
    ): WorkoutSoundCueRequest? {
        val activeCue = cue?.takeIf {
            it.enabled && it.soundEnabled && remainingSec > 0 && remainingSec <= it.thresholdSec
        } ?: return null
        return WorkoutSoundCueRequest(
            kind = WorkoutSoundCueKind.COUNTDOWN_BEEP,
            eventKey = eventKey,
            remainingSec = remainingSec,
            source = source,
            audioPolicy = WorkoutSoundCueAudioPolicy.coexistencePolicy
        )
    }

    private fun stageTransitionRequest(
        eventKey: String,
        cue: CountdownCue?,
        source: WorkoutSoundCueSource
    ): WorkoutSoundCueRequest? {
        cue?.takeIf { it.enabled && it.soundEnabled } ?: return null
        return WorkoutSoundCueRequest(
            kind = WorkoutSoundCueKind.STAGE_BELL,
            eventKey = eventKey,
            remainingSec = null,
            source = source,
            audioPolicy = WorkoutSoundCueAudioPolicy.coexistencePolicy
        )
    }
}

internal class WorkoutSoundCueController(
    private val player: WorkoutSoundCuePlayer
) {
    private val playedEventKeys = mutableSetOf<String>()

    fun dispatch(request: WorkoutSoundCueRequest?) {
        if (request == null || request.eventKey in playedEventKeys) return
        playedEventKeys += request.eventKey
        player.play(request.kind)
    }
}

internal interface WorkoutSoundCuePlayer {
    fun play(kind: WorkoutSoundCueKind)
}

internal data class WorkoutSoundCueRequest(
    val kind: WorkoutSoundCueKind,
    val eventKey: String,
    val remainingSec: Int?,
    val source: WorkoutSoundCueSource,
    val audioPolicy: SoundCueAudioCoexistencePolicy
)

internal enum class WorkoutSoundCueKind {
    COUNTDOWN_BEEP,
    STAGE_BELL
}

internal enum class WorkoutSoundCueSource {
    ACTION_ENDING,
    REST_ENDING,
    REST_STARTED,
    STAGE_TRANSITION
}

internal data class SoundCueAudioCoexistencePolicy(
    val requestsAudioFocus: Boolean,
    val allowsDucking: Boolean,
    val pausesExternalAudio: Boolean,
    val usageLabel: String,
    val contentTypeLabel: String
)

internal object WorkoutSoundCueAudioPolicy {
    val coexistencePolicy = SoundCueAudioCoexistencePolicy(
        requestsAudioFocus = false,
        allowsDucking = false,
        pausesExternalAudio = false,
        usageLabel = "USAGE_MEDIA",
        contentTypeLabel = "CONTENT_TYPE_MUSIC"
    )
}
