package com.liujyks.trainflow.core.model

sealed interface WorkoutEvent {
    data class SessionStarted(
        val sessionId: String
    ) : WorkoutEvent

    data class SessionPaused(
        val sessionId: String
    ) : WorkoutEvent

    data class SessionResumed(
        val sessionId: String
    ) : WorkoutEvent

    data class TimedWorkStarted(
        val stepId: String,
        val exerciseId: String? = null
    ) : WorkoutEvent

    data class TimedWorkEnding(
        val stepId: String,
        val remainingSec: Int
    ) : WorkoutEvent

    data class RestStarted(
        val stepId: String,
        val durationSec: Int
    ) : WorkoutEvent

    data class RestEnding(
        val stepId: String,
        val remainingSec: Int
    ) : WorkoutEvent

    data class StrengthSetReady(
        val exerciseId: String,
        val setPlanId: String? = null
    ) : WorkoutEvent

    data class StrengthSetStarted(
        val exerciseId: String,
        val setPlanId: String? = null
    ) : WorkoutEvent

    data class StrengthSetCompleted(
        val setRecordId: String
    ) : WorkoutEvent

    data class NextExerciseReady(
        val exerciseId: String
    ) : WorkoutEvent

    data class SessionCompleted(
        val sessionId: String
    ) : WorkoutEvent
}
