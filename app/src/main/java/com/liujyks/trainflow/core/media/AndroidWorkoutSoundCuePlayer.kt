package com.liujyks.trainflow.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.liujyks.trainflow.R

internal class AndroidWorkoutSoundCuePlayer(
    context: Context
) : WorkoutSoundCuePlayer {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(MaxConcurrentCueStreams)
        .setAudioAttributes(workoutSoundCueAudioAttributes())
        .build()
    private val soundIds = mutableMapOf<WorkoutSoundCueKind, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private val pendingKinds = mutableSetOf<WorkoutSoundCueKind>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds += sampleId
                val loadedKind = soundIds.entries.firstOrNull { it.value == sampleId }?.key
                if (loadedKind != null && pendingKinds.remove(loadedKind)) {
                    play(loadedKind)
                }
            }
        }
        soundIds[WorkoutSoundCueKind.COUNTDOWN_BEEP] = soundPool.load(
            context,
            R.raw.countdown_beep1,
            SoundLoadPriority
        )
        soundIds[WorkoutSoundCueKind.STAGE_BELL] = soundPool.load(
            context,
            R.raw.stage_bell_copper_clean,
            SoundLoadPriority
        )
    }

    override fun play(kind: WorkoutSoundCueKind) {
        val soundId = soundIds[kind] ?: return
        if (soundId !in loadedSoundIds) {
            pendingKinds += kind
            return
        }
        soundPool.play(
            soundId,
            CueVolume,
            CueVolume,
            CuePlaybackPriority,
            NoLoop,
            NormalPlaybackRate
        )
    }

    fun release() {
        pendingKinds.clear()
        loadedSoundIds.clear()
        soundPool.release()
    }
}

private fun workoutSoundCueAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}

private const val MaxConcurrentCueStreams = 2
private const val SoundLoadPriority = 1
private const val CuePlaybackPriority = 1
private const val NoLoop = 0
private const val CueVolume = 1f
private const val NormalPlaybackRate = 1f
