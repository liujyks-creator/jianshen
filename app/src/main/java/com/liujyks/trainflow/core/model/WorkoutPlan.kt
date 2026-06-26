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

const val TIMED_COMPOSITION_CURRENT_VERSION = 2
const val TIMED_COMPOSITION_MAX_TARGETS_PER_STAGE_GROUP = 5

data class TimedCompositionBlock(
    override val id: String,
    override val order: Int,
    override val title: String? = null,
    val compositionVersion: Int = TIMED_COMPOSITION_CURRENT_VERSION,
    val warmupSec: Int = 0,
    val cooldownSec: Int = 0,
    val rounds: Int,
    val restBetweenRoundsSec: Int = 0,
    val stageGroups: List<TimedCompositionStageGroup>,
    val compatibility: TimedCompositionCompatibilityMeta? = null
) : PlanBlock {
    override val kind: PlanBlockKind = PlanBlockKind.TIMED_COMPOSITION
}

data class TimedCompositionStageGroup(
    val id: String,
    val order: Int,
    val name: String,
    val colorHex: String,
    val iconKey: String? = null,
    val targets: List<TimedCompositionTarget>,
    val cueSettings: CueSettings? = null,
    val compatibility: TimedCompositionCompatibilityMeta? = null
) {
    val durationSec: Int
        get() = targets.sumOf { target -> target.durationSec }
}

data class TimedCompositionTarget(
    val id: String,
    val order: Int,
    val name: String,
    val kind: TimedCompositionTargetKind,
    val durationSec: Int,
    val colorHex: String,
    val iconKey: String? = null,
    val cueSettings: CueSettings? = null,
    val autoAdvance: Boolean = true,
    val compatibility: TimedCompositionCompatibilityMeta? = null
)

enum class TimedCompositionTargetKind(val contractValue: String) {
    ACTION("action"),
    REST("rest"),
    CUSTOM("custom")
}

data class TimedCompositionCompatibilityMeta(
    val sourceVersion: TimedCompositionCompatibilitySourceVersion? = null,
    val legacyBlockId: String? = null,
    val legacyItemId: String? = null,
    val legacyStageType: TimedStageType? = null,
    val convertedAt: String? = null
)

enum class TimedCompositionCompatibilitySourceVersion(val contractValue: String) {
    LEGACY_TIMED_CIRCUIT("legacy_timed_circuit"),
    V2("composition_v2")
}

fun TimedCompositionBlock.normalized(): TimedCompositionBlock {
    return copy(
        compositionVersion = TIMED_COMPOSITION_CURRENT_VERSION,
        warmupSec = warmupSec.coerceAtLeast(0),
        cooldownSec = cooldownSec.coerceAtLeast(0),
        rounds = rounds.coerceAtLeast(1),
        restBetweenRoundsSec = restBetweenRoundsSec.coerceAtLeast(0),
        stageGroups = stageGroups
            .sortedBy { group -> group.order }
            .mapIndexedNotNull { index, group ->
                group.normalized(index + 1)
            }
    )
}

fun TimedCompositionStageGroup.normalized(order: Int = this.order): TimedCompositionStageGroup? {
    val safeTargets = targets
        .sortedBy { target -> target.order }
        .take(TIMED_COMPOSITION_MAX_TARGETS_PER_STAGE_GROUP)
        .mapIndexed { index, target -> target.normalized(index + 1) }
        .filter { target -> target.durationSec > 0 }

    if (safeTargets.isEmpty()) return null

    val fallbackStageType = safeTargets.first().kind.toTimedStageType()
    return copy(
        order = order,
        name = name.trim().ifBlank { "Stage $order" },
        colorHex = normalizeStageColorHex(colorHex, fallbackStageType),
        targets = safeTargets
    )
}

fun TimedCompositionTarget.normalized(order: Int = this.order): TimedCompositionTarget {
    val fallbackStageType = kind.toTimedStageType()
    return copy(
        order = order,
        name = name.trim().ifBlank { kind.defaultName },
        durationSec = durationSec.coerceAtLeast(0),
        colorHex = normalizeStageColorHex(colorHex, fallbackStageType),
        iconKey = iconKey?.ifBlank { null },
        autoAdvance = autoAdvance
    )
}

fun TimedCompositionBlock.derivedRepeatedStageDurationSec(stageGroupId: String): Int? {
    return stageGroups.firstOrNull { group -> group.id == stageGroupId }?.durationSec
}

private val TimedCompositionTargetKind.defaultName: String
    get() = when (this) {
        TimedCompositionTargetKind.ACTION -> "Action"
        TimedCompositionTargetKind.REST -> "Rest"
        TimedCompositionTargetKind.CUSTOM -> "Custom"
    }

private fun TimedCompositionTargetKind.toTimedStageType(): TimedStageType {
    return when (this) {
        TimedCompositionTargetKind.ACTION -> TimedStageType.WORK
        TimedCompositionTargetKind.REST -> TimedStageType.REST
        TimedCompositionTargetKind.CUSTOM -> TimedStageType.CUSTOM
    }
}

data class TimedExerciseItem(
    val id: String,
    val exerciseId: String? = null,
    val labelOverride: String? = null,
    val side: ExerciseSide? = null,
    val stageType: TimedStageType = TimedStageType.WORK,
    val iconKey: String = stageType.defaultIconKey,
    val colorHex: String = stageType.defaultColorHex,
    val workDurationSec: Int,
    val restAfterSec: Int? = null,
    val cueSettings: CueSettings? = null,
    val autoAdvance: Boolean = false
)

enum class TimedStageType(
    val contractValue: String,
    val displayName: String,
    val defaultIconKey: String,
    val defaultColorHex: String
) {
    WARMUP("warmup", "热身", "warmup", "#F2B84B"),
    WORK("work", "工作", "work", "#F26B4F"),
    REST("rest", "休息", "rest", "#2FBF8F"),
    COOLDOWN("cooldown", "放松", "cooldown", "#65A9FF"),
    CUSTOM("custom", "自定义", "custom", "#A8B3BE")
}

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
