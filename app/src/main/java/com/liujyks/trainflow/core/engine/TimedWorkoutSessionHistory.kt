package com.liujyks.trainflow.core.engine

import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind

data class TimedSessionStepHistoryRecord(
    val stepId: String,
    val kind: SessionStepKind,
    val timedKind: TimedSessionStepKind,
    val title: String,
    val blockId: String,
    val itemId: String? = null,
    val exerciseId: String? = null,
    val plannedDurationSec: Int,
    val startedAtElapsedSec: Int,
    val endedAtElapsedSec: Int? = null,
    val status: TimedSessionStepHistoryStatus = TimedSessionStepHistoryStatus.STARTED,
    val actualDurationSec: Int? = null,
    val remainingSec: Int? = null,
    val extendedRestSec: Int = 0
)

enum class TimedSessionStepHistoryStatus {
    STARTED,
    COMPLETED,
    SKIPPED,
    ABANDONED
}

data class TimedRestExtensionHistoryRecord(
    val stepId: String,
    val kind: SessionStepKind,
    val title: String,
    val stepIndex: Int,
    val roundIndex: Int? = null,
    val restStageId: String? = null,
    val previousStageId: String? = null,
    val previousStageTitle: String? = null,
    val addedSec: Int,
    val cumulativeAddedSec: Int,
    val plannedRestSec: Int,
    val restElapsedBeforeExtensionSec: Int,
    val extensionAtRemainingSec: Int,
    val elapsedSec: Int
)

data class TimedWorkoutControlHistoryEvent(
    val type: TimedWorkoutControlHistoryType,
    val elapsedSec: Int,
    val stepId: String? = null,
    val stepKind: SessionStepKind? = null,
    val remainingSec: Int? = null,
    val seconds: Int? = null,
    val reason: String? = null
)

enum class TimedWorkoutControlHistoryType {
    START_SESSION,
    PAUSE_SESSION,
    RESUME_SESSION,
    SKIP_STEP,
    EXTEND_REST,
    END_SESSION
}

data class TimedWorkoutEarlyEndRecord(
    val reason: String?,
    val status: SessionStatus = SessionStatus.ABANDONED,
    val elapsedSec: Int,
    val completedStepCount: Int,
    val currentStepId: String?,
    val currentStepKind: SessionStepKind?,
    val currentStepTitle: String?,
    val currentStepRemainingSec: Int?,
    val currentStepActualDurationSec: Int?
)
