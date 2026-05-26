package com.liujyks.trainflow.core.model

data class WorkoutPlan(
    val id: String,
    val mode: WorkoutMode,
    val title: String,
    val description: String? = null,
    val blocks: List<PlanBlock>,
    val reminder: PlanReminder? = null,
    val preferences: PlanPreferences? = null,
    val followAlong: FollowAlongPlanMeta? = null,
    val createdAt: String,
    val updatedAt: String
)

data class PlanReminder(
    val enabled: Boolean,
    val scheduleAt: String? = null,
    val repeatRule: String? = null
)

data class PlanPreferences(
    val cueSettings: CueSettings? = null,
    val heartRateDisplay: HeartRateDisplayPreference? = null
)

data class HeartRateDisplayPreference(
    val enabled: Boolean,
    val showDisconnectedPlaceholder: Boolean
)

sealed interface PlanBlock {
    val id: String
    val kind: PlanBlockKind
    val title: String?
    val order: Int
}

data class WarmupBlock(
    override val id: String,
    override val order: Int,
    override val title: String? = null,
    val durationSec: Int? = null,
    val items: List<TimedExerciseItem> = emptyList()
) : PlanBlock {
    override val kind: PlanBlockKind = PlanBlockKind.WARMUP
}

data class StretchBlock(
    override val id: String,
    override val order: Int,
    override val title: String? = null,
    val durationSec: Int? = null,
    val items: List<TimedExerciseItem> = emptyList()
) : PlanBlock {
    override val kind: PlanBlockKind = PlanBlockKind.STRETCH
}

data class CooldownBlock(
    override val id: String,
    override val order: Int,
    override val title: String? = null,
    val durationSec: Int? = null,
    val items: List<TimedExerciseItem> = emptyList()
) : PlanBlock {
    override val kind: PlanBlockKind = PlanBlockKind.COOLDOWN
}

data class RestBlock(
    override val id: String,
    override val order: Int,
    val durationSec: Int,
    override val title: String? = null,
    val label: String? = null
) : PlanBlock {
    override val kind: PlanBlockKind = PlanBlockKind.REST
}

data class TimedCircuitBlock(
    override val id: String,
    override val order: Int,
    val rounds: Int,
    val items: List<TimedExerciseItem>,
    override val title: String? = null,
    val restBetweenRoundsSec: Int? = null
) : PlanBlock {
    override val kind: PlanBlockKind = PlanBlockKind.TIMED_CIRCUIT
}

data class TimedExerciseItem(
    val id: String,
    val exerciseId: String,
    val labelOverride: String? = null,
    val side: ExerciseSide? = null,
    val workDurationSec: Int,
    val restAfterSec: Int? = null,
    val cueSettings: CueSettings? = null,
    val autoAdvance: Boolean = false
)

data class CueSettings(
    val actionEnding: CountdownCue? = null,
    val restEnding: CountdownCue? = null
)

data class CountdownCue(
    val enabled: Boolean = true,
    val thresholdSec: Int = DEFAULT_THRESHOLD_SEC,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val emphasisAnimationEnabled: Boolean = true,
    val voiceCueEnabled: Boolean = false
) {
    companion object {
        const val DEFAULT_THRESHOLD_SEC = 5
    }
}

data class StrengthExerciseBlock(
    override val id: String,
    override val order: Int,
    val exerciseId: String,
    val sets: List<StrengthSetPlan>,
    override val title: String? = null,
    val target: StrengthExerciseTarget? = null,
    val substitutions: List<String> = emptyList(),
    val setTimerMode: StrengthSetTimerMode = StrengthSetTimerMode.MANUAL_START
) : PlanBlock {
    override val kind: PlanBlockKind = PlanBlockKind.STRENGTH_EXERCISE
}

data class StrengthExerciseTarget(
    val weight: WeightValue? = null,
    val repTarget: RepTarget? = null,
    val restAfterSetSec: Int? = null
)

data class StrengthSetPlan(
    val id: String,
    val order: Int,
    val kind: StrengthSetKind,
    val side: ExerciseSide? = null,
    val targetWeight: WeightValue? = null,
    val repTarget: RepTarget? = null,
    val restAfterSec: Int? = null
)

enum class StrengthSetKind(val contractValue: String) {
    WARMUP("warmup"),
    WORKING("working"),
    DROP("drop"),
    BACKOFF("backoff")
}

enum class StrengthSetTimerMode(val contractValue: String) {
    MANUAL_START("manual_start"),
    AUTO_AFTER_REST("auto_after_rest")
}

data class FollowAlongPlanMeta(
    val preset: Boolean,
    val coverMediaId: String? = null,
    val coachMediaIds: List<String> = emptyList(),
    val chapterIds: List<String> = emptyList(),
    val timelineCueIds: List<String> = emptyList(),
    val musicTrackIds: List<String> = emptyList(),
    val aiAnalysisProfileId: String? = null
)
