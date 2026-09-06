package com.liujyks.trainflow.core.model

data class WorkoutSession(
    val id: String,
    val planId: String? = null,
    val mode: WorkoutMode,
    val planSnapshot: WorkoutPlanSnapshot,
    val status: SessionStatus,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val totalElapsedSec: Int? = null,
    val effectiveElapsedSec: Int? = null,
    val pausedElapsedSec: Int? = null,
    val currentStep: SessionStep? = null,
    val stepHistory: List<SessionStepRecord> = emptyList(),
    val timedRestExtensionRecords: List<TimedRestExtensionRecord> = emptyList(),
    val strengthSetRecords: List<StrengthSetRecord> = emptyList(),
    val userFeedback: SessionFeedback? = null,
    val startLocalDate: String? = null,
    val startZoneId: String? = null,
    val startUtcOffsetSeconds: Long? = null,
    val timeMetadataSourceContractVersion: Long? = null
)

enum class SessionStatus(val contractValue: String) {
    READY("ready"),
    ACTIVE("active"),
    PAUSED("paused"),
    COMPLETED("completed"),
    ABANDONED("abandoned")
}

data class WorkoutPlanSnapshot(
    val planId: String? = null,
    val title: String,
    val mode: WorkoutMode,
    val blocks: List<PlanBlock>,
    val preferences: PlanPreferences? = null,
    val followAlong: FollowAlongPlanMeta? = null
)

data class SessionFeedback(
    val overallEffort: OverallEffort? = null,
    val discomfortNotes: String? = null,
    val notes: String? = null
)

enum class OverallEffort(val contractValue: String) {
    EASY("easy"),
    GOOD("good"),
    HARD("hard")
}

data class SessionStep(
    val id: String,
    val kind: SessionStepKind,
    val blockId: String? = null,
    val itemId: String? = null,
    val setPlanId: String? = null,
    val exerciseId: String? = null,
    val startedAt: String? = null,
    val remainingSec: Int? = null,
    val plannedDurationSec: Int? = null
)

data class SessionStepRecord(
    val stepId: String,
    val kind: SessionStepKind,
    val startedAt: String,
    val endedAt: String? = null,
    val skipped: Boolean = false,
    val actualDurationSec: Int? = null
)

data class TimedRestExtensionRecord(
    val id: String,
    val stepId: String,
    val stepIndex: Int,
    val roundIndex: Int? = null,
    val restStageId: String? = null,
    val restStageTitle: String,
    val previousStageId: String? = null,
    val previousStageTitle: String? = null,
    val addedSec: Int,
    val plannedRestSec: Int,
    val restElapsedBeforeExtensionSec: Int,
    val extensionAtRemainingSec: Int,
    val cumulativeExtraRestSec: Int,
    val eventElapsedSec: Int
)

enum class SessionStepKind(val contractValue: String) {
    PREPARE("prepare"),
    TIMED_WORK("timed_work"),
    TIMED_REST("timed_rest"),
    STRENGTH_PREPARE_SET("strength_prepare_set"),
    STRENGTH_ACTIVE_SET("strength_active_set"),
    STRENGTH_CONFIRM_SET("strength_confirm_set"),
    STRENGTH_REST("strength_rest"),
    STRETCH("stretch"),
    COMPLETED("completed")
}

data class StrengthSetRecord(
    val id: String,
    val exerciseId: String,
    val sourceSetPlanId: String? = null,
    val setOrder: Int,
    val setKind: StrengthSetKind,
    val side: ExerciseSide? = null,
    val plannedWeight: WeightValue? = null,
    val plannedRepTarget: RepTarget? = null,
    val actualWeight: WeightValue? = null,
    val actualReps: Int? = null,
    val activeDurationSec: Int? = null,
    val actualRestAfterSec: Int? = null,
    val effort: SetEffort? = null,
    val substitutedFromExerciseId: String? = null,
    val notes: String? = null
)

enum class SetEffort(val contractValue: String) {
    EASY("easy"),
    GOOD("good"),
    HARD("hard"),
    FORM_BREAKDOWN("form_breakdown")
}
