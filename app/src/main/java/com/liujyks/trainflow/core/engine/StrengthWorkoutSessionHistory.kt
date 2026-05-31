package com.liujyks.trainflow.core.engine

import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind

data class StrengthSessionStepHistoryRecord(
    val stepId: String,
    val kind: SessionStepKind,
    val exerciseId: String,
    val blockId: String,
    val setPlanId: String,
    val setOrder: Int,
    val startedAtElapsedSec: Int,
    val endedAtElapsedSec: Int? = null,
    val status: StrengthSessionStepHistoryStatus = StrengthSessionStepHistoryStatus.STARTED,
    val actualDurationSec: Int? = null,
    val remainingSec: Int? = null,
    val substitutedFromExerciseId: String? = null
)

enum class StrengthSessionStepHistoryStatus {
    STARTED,
    COMPLETED,
    SKIPPED,
    ABANDONED
}

data class StrengthWorkoutControlHistoryEvent(
    val type: StrengthWorkoutControlHistoryType,
    val elapsedSec: Int,
    val stepId: String? = null,
    val stepKind: SessionStepKind? = null,
    val setPlanId: String? = null,
    val remainingSec: Int? = null,
    val reason: String? = null,
    val fromExerciseId: String? = null,
    val toExerciseId: String? = null
)

enum class StrengthWorkoutControlHistoryType {
    START_SESSION,
    PAUSE_SESSION,
    RESUME_SESSION,
    START_STRENGTH_SET,
    COMPLETE_STRENGTH_SET,
    CONFIRM_STRENGTH_SET,
    REPLACE_EXERCISE,
    SKIP_EXERCISE,
    END_SESSION
}

data class StrengthWorkoutEarlyEndRecord(
    val reason: String?,
    val status: SessionStatus = SessionStatus.ABANDONED,
    val elapsedSec: Int,
    val completedSetCount: Int,
    val currentStepId: String?,
    val currentStepKind: SessionStepKind?,
    val currentSetPlanId: String?,
    val currentStepRemainingSec: Int?,
    val currentStepActualDurationSec: Int?
)
