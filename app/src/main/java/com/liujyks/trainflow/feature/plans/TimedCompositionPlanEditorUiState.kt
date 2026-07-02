package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.TIMED_COMPOSITION_MAX_TARGETS_PER_STAGE_GROUP
import com.liujyks.trainflow.core.model.TimedCompositionCompatibilityMeta
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedStageIconKey
import com.liujyks.trainflow.core.model.TimedStageStyle
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.normalized
import com.liujyks.trainflow.core.model.normalizeStageColorHex
import com.liujyks.trainflow.core.model.normalizeTimedStageIconKey
import com.liujyks.trainflow.core.model.stageColorPresetFor
import com.liujyks.trainflow.core.model.stageTextColorHexFor

private const val MinTimedCompositionTargetDurationSec = 5
private const val MaxTimedCompositionDurationSec = 3600
private const val MaxTimedCompositionRounds = 12
private const val MaxCompositionNameHalfWidthUnits = 8

internal data class TimedCompositionPlanEditorScreenState(
    val planId: String,
    val title: String,
    val description: String = "",
    val warmupSec: Int,
    val warmupRawText: String? = null,
    val cooldownSec: Int,
    val cooldownRawText: String? = null,
    val rounds: Int,
    val roundsRawText: String? = null,
    val restBetweenRoundsSec: Int,
    val restBetweenRoundsRawText: String? = null,
    val warmupStyle: TimedStageStyle? = null,
    val cooldownStyle: TimedStageStyle? = null,
    val restBetweenRoundsStyle: TimedStageStyle? = null,
    val stageGroups: List<TimedCompositionStageGroupEditorUiState>,
    val source: TimedCompositionEditorDraftSource,
    val sourcePlan: WorkoutPlan,
    val requiresExplicitConversionForV2: Boolean,
    val nextStageSequence: Int = 1,
    val nextTargetSequence: Int = 1,
    val savedPlan: WorkoutPlan? = null,
    val statusMessage: String? = null
) {
    val warmupText: String
        get() = warmupRawText ?: warmupSec.toString()

    val cooldownText: String
        get() = cooldownRawText ?: cooldownSec.toString()

    val roundsText: String
        get() = roundsRawText ?: rounds.toString()

    val restBetweenRoundsText: String
        get() = restBetweenRoundsRawText ?: restBetweenRoundsSec.toString()

    val validationMessage: String?
        get() = validateCompositionDraft()

    val canSave: Boolean
        get() = validationMessage == null

    val canStartTraining: Boolean
        get() = canSave &&
            source != TimedCompositionEditorDraftSource.UNSUPPORTED &&
            toWorkoutPlan().hasStartableTimedCompositionPayload()

    val startDisabledReason: String?
        get() = if (canStartTraining) {
            null
        } else {
            validationMessage ?: "当前阶段编排暂无可执行步骤。"
        }

    val repeatedDurationSec: Int
        get() = stageGroups.sumOf { group -> group.durationSec }

    val estimatedDurationSec: Int
        get() = warmupSec + cooldownSec + repeatedDurationSec * rounds +
            restBetweenRoundsSec * (rounds - 1).coerceAtLeast(0)

    val summary: String
        get() = "${stageGroups.size} 个阶段 · ${rounds} 轮 · 预计 ${estimatedDurationSec.formatDuration()}"

    val isEditingExistingPlan: Boolean
        get() = sourcePlan.id == planId && sourcePlan.blocks.isNotEmpty()

    fun toDraft(): TimedCompositionEditorDraft {
        return TimedCompositionEditorDraft(
            planId = planId,
            title = title,
            description = description,
            warmupSec = warmupSec,
            cooldownSec = cooldownSec,
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec,
            warmupStyle = warmupStyle.normalized(),
            cooldownStyle = cooldownStyle.normalized(),
            restBetweenRoundsStyle = restBetweenRoundsStyle.normalized(),
            stageGroups = stageGroups.mapIndexed { index, group -> group.toDraft(order = index + 1) },
            source = source,
            sourcePlan = sourcePlan,
            requiresExplicitConversionForV2 = requiresExplicitConversionForV2
        )
    }

    fun toWorkoutPlan(timestamp: String = DefaultTimedPlanTimestamp): WorkoutPlan {
        return toDraft().toWorkoutPlan(
            exportMode = TimedCompositionEditorDraftExportMode.EXPORT_V2_PAYLOAD,
            timestamp = timestamp
        )
    }
}

internal data class TimedCompositionStageGroupEditorUiState(
    val id: String,
    val name: String,
    val colorHex: String,
    val iconKey: String?,
    val order: Int,
    val targets: List<TimedCompositionTargetEditorUiState>,
    val compatibility: TimedCompositionCompatibilityMeta? = null
) {
    val durationSec: Int
        get() = targets.sumOf { target -> target.durationSec }

    fun toDraft(order: Int = this.order): TimedCompositionStageGroupDraft {
        return TimedCompositionStageGroupDraft(
            id = id,
            name = name,
            colorHex = normalizeStageColorHex(
                hex = colorHex,
                fallbackStageType = targets.firstOrNull()?.kind?.defaultStageType ?: TimedStageType.CUSTOM
            ),
            iconKey = iconKey,
            order = order,
            targets = targets.mapIndexed { index, target -> target.toDraft(order = index + 1) },
            compatibility = compatibility
        )
    }
}

internal data class TimedCompositionTargetEditorUiState(
    val id: String,
    val name: String,
    val kind: TimedCompositionTargetKind,
    val colorHex: String,
    val iconKey: String?,
    val durationSec: Int,
    val durationRawText: String? = null,
    val order: Int,
    val compatibility: TimedCompositionCompatibilityMeta? = null
) {
    val durationText: String
        get() = durationRawText ?: durationSec.toString()

    val kindLabel: String
        get() = kind.displayLabel

    fun toDraft(order: Int = this.order): TimedCompositionTargetDraft {
        return TimedCompositionTargetDraft(
            id = id,
            name = name,
            kind = kind,
            colorHex = normalizeStageColorHex(colorHex, kind.defaultStageType),
            iconKey = iconKey,
            durationSec = durationSec,
            order = order,
            compatibility = compatibility
        )
    }
}

internal val TimedCompositionTargetKind.displayLabel: String
    get() = when (this) {
        TimedCompositionTargetKind.ACTION -> "动作"
        TimedCompositionTargetKind.REST -> "休息"
        TimedCompositionTargetKind.CUSTOM -> "自定义"
    }

private val TimedCompositionTargetKind.defaultStageType: TimedStageType
    get() = when (this) {
        TimedCompositionTargetKind.ACTION -> TimedStageType.WORK
        TimedCompositionTargetKind.REST -> TimedStageType.REST
        TimedCompositionTargetKind.CUSTOM -> TimedStageType.CUSTOM
    }

internal val TimedCompositionTargetKind.defaultColorHex: String
    get() = defaultStageType.defaultColorHex

internal val TimedCompositionTargetKind.defaultIconKey: String
    get() = defaultStageType.defaultIconKey

internal enum class TimedCompositionBoundaryStyleTarget(
    val displayLabel: String,
    val fallbackStageType: TimedStageType,
    val defaultIconKey: String
) {
    WARMUP(
        displayLabel = "热身",
        fallbackStageType = TimedStageType.WARMUP,
        defaultIconKey = TimedStageType.WARMUP.defaultIconKey
    ),
    COOLDOWN(
        displayLabel = "放松",
        fallbackStageType = TimedStageType.COOLDOWN,
        defaultIconKey = TimedStageType.COOLDOWN.defaultIconKey
    ),
    REST_BETWEEN_ROUNDS(
        displayLabel = "轮间休息",
        fallbackStageType = TimedStageType.REST,
        defaultIconKey = TimedStageIconKey.RECOVER_BREATHE.contractValue
    )
}

internal data class StageIconOptionUiState(
    val key: String,
    val label: String,
    val description: String,
    val selected: Boolean,
    val contentDescription: String
)

internal data class StageStylePickerUiState(
    val selectedColorHex: String,
    val selectedColorName: String,
    val selectedTextColorHex: String,
    val selectedIconKey: String,
    val selectedIconLabel: String,
    val colorPicker: StageColorPickerUiState,
    val iconOptions: List<StageIconOptionUiState>
)

internal fun buildDefaultTimedCompositionPlanEditorState(
    planId: String = "plan-timed-draft",
    defaults: PlanEditorDefaults = PlanEditorDefaults(),
    timestamp: String = DefaultTimedPlanTimestamp
): TimedCompositionPlanEditorScreenState {
    val sourcePlan = WorkoutPlan(
        id = planId,
        mode = WorkoutMode.TIMED,
        title = "阶段编排计时计划",
        description = "本地保存的阶段编排计时计划",
        blocks = emptyList(),
        preferences = PlanPreferences(
            cueSettings = CueSettings(
                actionEnding = defaults.actionCueDefaults().toCountdownCue(),
                restEnding = defaults.restCueDefaults().toCountdownCue()
            )
        ),
        createdAt = timestamp,
        updatedAt = timestamp
    )
    return TimedCompositionPlanEditorScreenState(
        planId = planId,
        title = sourcePlan.title,
        description = sourcePlan.description.orEmpty(),
        warmupSec = 60,
        cooldownSec = 90,
        rounds = 3,
        restBetweenRoundsSec = 60,
        stageGroups = defaultCompositionStageGroups(),
        source = TimedCompositionEditorDraftSource.V2_PAYLOAD,
        sourcePlan = sourcePlan,
        requiresExplicitConversionForV2 = false,
        nextStageSequence = 3,
        nextTargetSequence = 4
    )
}

internal fun WorkoutPlan.toTimedCompositionPlanEditorState(
    defaults: PlanEditorDefaults = PlanEditorDefaults()
): TimedCompositionPlanEditorScreenState {
    if (mode != WorkoutMode.TIMED) {
        return buildDefaultTimedCompositionPlanEditorState(
            planId = id,
            defaults = defaults,
            timestamp = updatedAt
        ).copy(
            title = title,
            description = description.orEmpty(),
            source = TimedCompositionEditorDraftSource.UNSUPPORTED,
            sourcePlan = this,
            requiresExplicitConversionForV2 = true,
            statusMessage = "当前计划不是计时训练，已使用安全默认编排草稿。"
        )
    }

    val draft = toTimedCompositionEditorDraft()
    val groups = draft.stageGroups
        .map { group -> group.toEditorUiState() }
        .ifEmpty { defaultCompositionStageGroups() }
        .mapIndexed { index, group -> group.copy(order = index + 1) }
    val nextStageSequence = groups.nextStageSequenceFor(prefix = "stage-added")
    val nextTargetSequence = groups
        .flatMap { group -> group.targets }
        .nextTargetSequenceFor(prefix = "target-added")
    return TimedCompositionPlanEditorScreenState(
        planId = draft.planId,
        title = draft.title,
        description = draft.description,
        warmupSec = draft.warmupSec.coerceIn(0, MaxTimedCompositionDurationSec),
        cooldownSec = draft.cooldownSec.coerceIn(0, MaxTimedCompositionDurationSec),
        rounds = draft.rounds.coerceIn(1, MaxTimedCompositionRounds),
        restBetweenRoundsSec = draft.restBetweenRoundsSec.coerceIn(0, MaxTimedCompositionDurationSec),
        warmupStyle = draft.warmupStyle.normalized(),
        cooldownStyle = draft.cooldownStyle.normalized(),
        restBetweenRoundsStyle = draft.restBetweenRoundsStyle.normalized(),
        stageGroups = groups,
        source = draft.source,
        sourcePlan = draft.sourcePlan,
        requiresExplicitConversionForV2 = draft.requiresExplicitConversionForV2,
        nextStageSequence = nextStageSequence,
        nextTargetSequence = nextTargetSequence,
        statusMessage = if (draft.requiresExplicitConversionForV2) {
            "已载入旧计时计划；保存后将使用新的阶段编排结构。"
        } else {
            "已载入阶段编排草稿，可编辑后保存回同一个本地计划。"
        }
    )
}

internal fun TimedCompositionPlanEditorScreenState.updateTitle(
    value: String
): TimedCompositionPlanEditorScreenState {
    return copy(title = value, savedPlan = null, statusMessage = null)
}

internal fun TimedCompositionPlanEditorScreenState.updateDescription(
    value: String
): TimedCompositionPlanEditorScreenState {
    return copy(description = value, savedPlan = null, statusMessage = null)
}

internal fun TimedCompositionPlanEditorScreenState.updateWarmupText(
    input: String
): TimedCompositionPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.coerceIn(0, MaxTimedCompositionDurationSec)
    return copy(
        warmupSec = parsed ?: warmupSec,
        warmupRawText = cleaned,
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.updateCooldownText(
    input: String
): TimedCompositionPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.coerceIn(0, MaxTimedCompositionDurationSec)
    return copy(
        cooldownSec = parsed ?: cooldownSec,
        cooldownRawText = cleaned,
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.updateRoundsText(
    input: String
): TimedCompositionPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.coerceIn(1, MaxTimedCompositionRounds)
    return copy(
        rounds = parsed ?: rounds,
        roundsRawText = cleaned,
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.updateRestBetweenRoundsText(
    input: String
): TimedCompositionPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.coerceIn(0, MaxTimedCompositionDurationSec)
    return copy(
        restBetweenRoundsSec = parsed ?: restBetweenRoundsSec,
        restBetweenRoundsRawText = cleaned,
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.updateBoundaryStageStyle(
    target: TimedCompositionBoundaryStyleTarget,
    style: TimedStageStyle
): TimedCompositionPlanEditorScreenState {
    val normalized = style.normalized()
    return when (target) {
        TimedCompositionBoundaryStyleTarget.WARMUP -> copy(warmupStyle = normalized)
        TimedCompositionBoundaryStyleTarget.COOLDOWN -> copy(cooldownStyle = normalized)
        TimedCompositionBoundaryStyleTarget.REST_BETWEEN_ROUNDS -> copy(restBetweenRoundsStyle = normalized)
    }.copy(
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.updateStageName(
    stageId: String,
    name: String
): TimedCompositionPlanEditorScreenState {
    val limitedName = name.limitHalfWidthUnits(MaxCompositionNameHalfWidthUnits)
    return copy(
        stageGroups = stageGroups.map { group ->
            if (group.id == stageId) group.copy(name = limitedName) else group
        },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.updateStageColor(
    stageId: String,
    colorHex: String
): TimedCompositionPlanEditorScreenState {
    return copy(
        stageGroups = stageGroups.map { group ->
            if (group.id == stageId) {
                val fallbackType = group.targets.firstOrNull()?.kind?.defaultStageType ?: TimedStageType.CUSTOM
                group.copy(colorHex = normalizeStageColorHex(colorHex, fallbackType))
            } else {
                group
            }
        },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.updateStageStyle(
    stageId: String,
    style: TimedStageStyle
): TimedCompositionPlanEditorScreenState {
    return copy(
        stageGroups = stageGroups.map { group ->
            if (group.id == stageId) {
                val fallbackType = group.targets.firstOrNull()?.kind?.defaultStageType ?: TimedStageType.CUSTOM
                group.copy(
                    colorHex = normalizeStageColorHex(style.colorHex, fallbackType),
                    iconKey = normalizeTimedStageIconKey(style.iconKey)
                )
            } else {
                group
            }
        },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.addStage(): TimedCompositionPlanEditorScreenState {
    val (stageId, nextStage) = nextGeneratedStageId()
    val (targetId, nextTarget) = nextGeneratedTargetId()
    val newGroup = TimedCompositionStageGroupEditorUiState(
        id = stageId,
        name = "新阶段",
        colorHex = TimedCompositionTargetKind.ACTION.defaultColorHex,
        iconKey = TimedCompositionTargetKind.ACTION.defaultIconKey,
        order = stageGroups.size + 1,
        targets = listOf(
            TimedCompositionTargetEditorUiState(
                id = targetId,
                name = "动作",
                kind = TimedCompositionTargetKind.ACTION,
                colorHex = TimedCompositionTargetKind.ACTION.defaultColorHex,
                iconKey = TimedCompositionTargetKind.ACTION.defaultIconKey,
                durationSec = 45,
                order = 1
            )
        )
    )
    return copy(
        stageGroups = stageGroups + newGroup,
        nextStageSequence = nextStage,
        nextTargetSequence = nextTarget,
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.copyStage(
    stageId: String
): TimedCompositionPlanEditorScreenState {
    val index = stageGroups.indexOfFirst { group -> group.id == stageId }
    if (index < 0) return this
    val sourceGroup = stageGroups[index]
    val (stageIdCopy, nextStage) = nextGeneratedStageId(prefix = "${sourceGroup.id}-copy")
    var nextTarget = nextTargetSequence
    val copiedTargets = sourceGroup.targets.mapIndexed { targetIndex, target ->
        val generated = nextGeneratedTargetId(prefix = "${target.id}-copy", startingSequence = nextTarget)
        nextTarget = generated.second
        target.copy(id = generated.first, order = targetIndex + 1)
    }
    val copiedGroup = sourceGroup.copy(
        id = stageIdCopy,
        name = "${sourceGroup.name}副本".limitHalfWidthUnits(MaxCompositionNameHalfWidthUnits),
        order = index + 2,
        targets = copiedTargets
    )
    return copy(
        stageGroups = (stageGroups.take(index + 1) + copiedGroup + stageGroups.drop(index + 1))
            .mapIndexed { groupIndex, group -> group.copy(order = groupIndex + 1) },
        nextStageSequence = nextStage,
        nextTargetSequence = nextTarget,
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.removeStage(
    stageId: String
): TimedCompositionPlanEditorScreenState {
    if (stageGroups.size <= 1) return this
    return copy(
        stageGroups = stageGroups
            .filterNot { group -> group.id == stageId }
            .mapIndexed { index, group -> group.copy(order = index + 1) },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.reorderStages(
    stageIds: List<String>
): TimedCompositionPlanEditorScreenState {
    if (stageIds.size != stageGroups.size || stageIds.toSet() != stageGroups.map { it.id }.toSet()) return this
    if (stageIds == stageGroups.map { it.id }) return this
    val groupsById = stageGroups.associateBy { group -> group.id }
    return copy(
        stageGroups = stageIds
            .mapNotNull { stageId -> groupsById[stageId] }
            .mapIndexed { index, group -> group.copy(order = index + 1) },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.moveStage(
    fromIndex: Int,
    toIndex: Int
): TimedCompositionPlanEditorScreenState {
    if (fromIndex !in stageGroups.indices || toIndex !in stageGroups.indices || fromIndex == toIndex) return this
    return copy(
        stageGroups = stageGroups
            .withItemMoved(fromIndex, toIndex)
            .mapIndexed { index, group -> group.copy(order = index + 1) },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.addTarget(
    stageId: String,
    kind: TimedCompositionTargetKind = TimedCompositionTargetKind.ACTION
): TimedCompositionPlanEditorScreenState {
    val group = stageGroups.firstOrNull { candidate -> candidate.id == stageId } ?: return this
    if (group.targets.size >= TIMED_COMPOSITION_MAX_TARGETS_PER_STAGE_GROUP) {
        return copy(statusMessage = "每个阶段最多 5 个目标。")
    }
    val (targetId, nextTarget) = nextGeneratedTargetId()
    val target = TimedCompositionTargetEditorUiState(
        id = targetId,
        name = kind.displayLabel,
        kind = kind,
        colorHex = kind.defaultColorHex,
        iconKey = kind.defaultIconKey,
        durationSec = if (kind == TimedCompositionTargetKind.REST) 15 else 45,
        order = group.targets.size + 1
    )
    return copy(
        stageGroups = stageGroups.map { candidate ->
            if (candidate.id == stageId) {
                candidate.copy(targets = candidate.targets + target)
            } else {
                candidate
            }
        },
        nextTargetSequence = nextTarget,
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.removeTarget(
    stageId: String,
    targetId: String
): TimedCompositionPlanEditorScreenState {
    val group = stageGroups.firstOrNull { candidate -> candidate.id == stageId } ?: return this
    if (group.targets.size <= 1) return this
    return copy(
        stageGroups = stageGroups.map { candidate ->
            if (candidate.id == stageId) {
                candidate.copy(
                    targets = candidate.targets
                        .filterNot { target -> target.id == targetId }
                        .mapIndexed { index, target -> target.copy(order = index + 1) }
                )
            } else {
                candidate
            }
        },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.copyTarget(
    stageId: String,
    targetId: String
): TimedCompositionPlanEditorScreenState {
    val group = stageGroups.firstOrNull { candidate -> candidate.id == stageId } ?: return this
    if (group.targets.size >= TIMED_COMPOSITION_MAX_TARGETS_PER_STAGE_GROUP) {
        return copy(statusMessage = "每个阶段最多 5 个目标。")
    }
    val targetIndex = group.targets.indexOfFirst { target -> target.id == targetId }
    if (targetIndex < 0) return this
    val (newTargetId, nextTarget) = nextGeneratedTargetId(prefix = "${targetId}-copy")
    val copiedTarget = group.targets[targetIndex].copy(
        id = newTargetId,
        order = targetIndex + 2
    )
    return copy(
        stageGroups = stageGroups.map { candidate ->
            if (candidate.id == stageId) {
                candidate.copy(
                    targets = (candidate.targets.take(targetIndex + 1) +
                        copiedTarget +
                        candidate.targets.drop(targetIndex + 1))
                        .mapIndexed { index, target -> target.copy(order = index + 1) }
                )
            } else {
                candidate
            }
        },
        nextTargetSequence = nextTarget,
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.moveTarget(
    stageId: String,
    fromIndex: Int,
    toIndex: Int
): TimedCompositionPlanEditorScreenState {
    val group = stageGroups.firstOrNull { candidate -> candidate.id == stageId } ?: return this
    if (fromIndex !in group.targets.indices || toIndex !in group.targets.indices || fromIndex == toIndex) return this
    return copy(
        stageGroups = stageGroups.map { candidate ->
            if (candidate.id == stageId) {
                candidate.copy(
                    targets = candidate.targets
                        .withItemMoved(fromIndex, toIndex)
                        .mapIndexed { index, target -> target.copy(order = index + 1) }
                )
            } else {
                candidate
            }
        },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedCompositionPlanEditorScreenState.updateTargetName(
    stageId: String,
    targetId: String,
    name: String
): TimedCompositionPlanEditorScreenState {
    val limitedName = name.limitHalfWidthUnits(MaxCompositionNameHalfWidthUnits)
    return updateTarget(stageId, targetId) { target -> target.copy(name = limitedName) }
}

internal fun TimedCompositionPlanEditorScreenState.updateTargetDurationText(
    stageId: String,
    targetId: String,
    input: String
): TimedCompositionPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.coerceIn(
        MinTimedCompositionTargetDurationSec,
        MaxTimedCompositionDurationSec
    )
    return updateTarget(stageId, targetId) { target ->
        target.copy(
            durationSec = parsed ?: target.durationSec,
            durationRawText = cleaned
        )
    }
}

internal fun TimedCompositionPlanEditorScreenState.updateTargetKind(
    stageId: String,
    targetId: String,
    kind: TimedCompositionTargetKind
): TimedCompositionPlanEditorScreenState {
    return updateTarget(stageId, targetId) { target ->
        target.copy(
            kind = kind,
            colorHex = kind.defaultColorHex,
            iconKey = kind.defaultIconKey
        )
    }
}

internal fun TimedCompositionPlanEditorScreenState.updateTargetColor(
    stageId: String,
    targetId: String,
    colorHex: String
): TimedCompositionPlanEditorScreenState {
    return updateTarget(stageId, targetId) { target ->
        target.copy(colorHex = normalizeStageColorHex(colorHex, target.kind.defaultStageType))
    }
}

internal fun TimedCompositionPlanEditorScreenState.updateTargetStyle(
    stageId: String,
    targetId: String,
    style: TimedStageStyle
): TimedCompositionPlanEditorScreenState {
    return updateTarget(stageId, targetId) { target ->
        target.copy(
            colorHex = normalizeStageColorHex(style.colorHex, target.kind.defaultStageType),
            iconKey = normalizeTimedStageIconKey(style.iconKey)
        )
    }
}

internal fun TimedCompositionPlanEditorScreenState.canAddTarget(stageId: String): Boolean {
    return stageGroups
        .firstOrNull { group -> group.id == stageId }
        ?.targets
        ?.size
        ?.let { count -> count < TIMED_COMPOSITION_MAX_TARGETS_PER_STAGE_GROUP }
        ?: false
}

internal fun TimedCompositionPlanEditorScreenState.saveDraftPlan(
    timestamp: String = DefaultTimedPlanTimestamp
): TimedCompositionPlanEditorScreenState {
    if (!canSave) {
        return copy(statusMessage = validationMessage ?: "请检查计时计划草稿。")
    }
    return markPlanSaved(toWorkoutPlan(timestamp = timestamp))
}

internal fun TimedCompositionPlanEditorScreenState.markPlanSaved(
    plan: WorkoutPlan
): TimedCompositionPlanEditorScreenState {
    return copy(
        sourcePlan = plan,
        savedPlan = plan,
        source = TimedCompositionEditorDraftSource.V2_PAYLOAD,
        requiresExplicitConversionForV2 = false,
        statusMessage = if (isEditingExistingPlan) {
            "已更新「${plan.title}」，可以从当前阶段编排开始训练。"
        } else {
            "已保存「${plan.title}」到本地计划，可以从当前阶段编排开始训练。"
        }
    )
}

internal fun TimedCompositionStageGroupEditorUiState.toStageColorPickerUiState(): StageColorPickerUiState {
    val fallbackType = targets.firstOrNull()?.kind?.defaultStageType ?: TimedStageType.CUSTOM
    val selectedHex = normalizeStageColorHex(colorHex, fallbackType)
    return buildStageColorPickerUiState(selectedHex, fallbackType)
}

internal fun TimedCompositionTargetEditorUiState.toStageColorPickerUiState(): StageColorPickerUiState {
    val fallbackType = kind.defaultStageType
    val selectedHex = normalizeStageColorHex(colorHex, fallbackType)
    return buildStageColorPickerUiState(selectedHex, fallbackType)
}

internal fun TimedCompositionPlanEditorScreenState.boundaryStageStyle(
    target: TimedCompositionBoundaryStyleTarget
): TimedStageStyle {
    val explicitStyle = when (target) {
        TimedCompositionBoundaryStyleTarget.WARMUP -> warmupStyle
        TimedCompositionBoundaryStyleTarget.COOLDOWN -> cooldownStyle
        TimedCompositionBoundaryStyleTarget.REST_BETWEEN_ROUNDS -> restBetweenRoundsStyle
    }
    return TimedStageStyle(
        colorHex = normalizeStageColorHex(explicitStyle?.colorHex, target.fallbackStageType),
        iconKey = normalizeTimedStageIconKey(explicitStyle?.iconKey) ?: target.defaultIconKey
    )
}

internal fun TimedCompositionPlanEditorScreenState.toBoundaryStylePickerUiState(
    target: TimedCompositionBoundaryStyleTarget
): StageStylePickerUiState {
    val style = boundaryStageStyle(target)
    return buildStageStylePickerUiState(
        selectedHex = style.colorHex,
        fallbackType = target.fallbackStageType,
        selectedIconKey = style.iconKey,
        fallbackIconKey = target.defaultIconKey
    )
}

internal fun TimedCompositionStageGroupEditorUiState.toStageStylePickerUiState(): StageStylePickerUiState {
    val fallbackType = targets.firstOrNull()?.kind?.defaultStageType ?: TimedStageType.CUSTOM
    return buildStageStylePickerUiState(
        selectedHex = colorHex,
        fallbackType = fallbackType,
        selectedIconKey = iconKey,
        fallbackIconKey = fallbackType.defaultIconKey
    )
}

internal fun TimedCompositionTargetEditorUiState.toStageStylePickerUiState(): StageStylePickerUiState {
    val fallbackType = kind.defaultStageType
    return buildStageStylePickerUiState(
        selectedHex = colorHex,
        fallbackType = fallbackType,
        selectedIconKey = iconKey,
        fallbackIconKey = kind.defaultIconKey
    )
}

internal fun stageStyleIconLabel(iconKey: String?): String {
    val key = normalizeTimedStageIconKey(iconKey)
    return StageStyleIconLabels.firstOrNull { option -> option.key == key }?.label ?: "自定义"
}

private fun buildStageStylePickerUiState(
    selectedHex: String?,
    fallbackType: TimedStageType,
    selectedIconKey: String?,
    fallbackIconKey: String
): StageStylePickerUiState {
    val selectedColorHex = normalizeStageColorHex(selectedHex, fallbackType)
    val normalizedIconKey = normalizeTimedStageIconKey(selectedIconKey) ?: fallbackIconKey
    val iconOptions = StageStyleIconLabels.map { option ->
        val selected = option.key == normalizedIconKey
        StageIconOptionUiState(
            key = option.key,
            label = option.label,
            description = option.description,
            selected = selected,
            contentDescription = buildString {
                if (selected) append("已选中，")
                append("图标 ${option.label}，")
                append(option.description)
            }
        )
    }
    return StageStylePickerUiState(
        selectedColorHex = selectedColorHex,
        selectedColorName = stageColorPresetFor(selectedColorHex)?.name ?: "自定义颜色",
        selectedTextColorHex = stageTextColorHexFor(selectedColorHex, fallbackType),
        selectedIconKey = normalizedIconKey,
        selectedIconLabel = stageStyleIconLabel(normalizedIconKey),
        colorPicker = buildStageColorPickerUiState(selectedColorHex, fallbackType),
        iconOptions = iconOptions
    )
}

private fun buildStageColorPickerUiState(
    selectedHex: String,
    fallbackType: TimedStageType
): StageColorPickerUiState {
    val selectedPreset = stageColorPresetFor(selectedHex)
    return StageColorPickerUiState(
        selectedColorHex = selectedHex,
        selectedColorName = selectedPreset?.name ?: "自定义颜色",
        selectedTextColorHex = stageTextColorHexFor(selectedHex, fallbackType),
        recommendedColors = com.liujyks.trainflow.core.model.RecommendedStageColorPresets.map { preset ->
            preset.toEditorColorOption(selectedHex)
        },
        moreColors = com.liujyks.trainflow.core.model.MoreStageColorPresets.map { preset ->
            preset.toEditorColorOption(selectedHex)
        }
    )
}

private fun com.liujyks.trainflow.core.model.StageColorPreset.toEditorColorOption(
    selectedHex: String
): StageColorOptionUiState {
    val isSelected = hex.equals(selectedHex, ignoreCase = true)
    return StageColorOptionUiState(
        id = id,
        name = name,
        hex = hex,
        tone = tone,
        recommendedUse = recommendedUse,
        textColor = textColor,
        isHighAttention = isHighAttention,
        selected = isSelected,
        hasCheckIndicator = isSelected,
        contentDescription = buildString {
            if (isSelected) append("已选中，")
            append(accessibilityLabel)
        }
    )
}

private data class StageStyleIconLabel(
    val key: String,
    val label: String,
    val description: String
)

private val StageStyleIconLabels: List<StageStyleIconLabel> = listOf(
    StageStyleIconLabel(TimedStageIconKey.WARMUP.contractValue, "热身", "火苗升温符号，适合热身和启动阶段"),
    StageStyleIconLabel(TimedStageIconKey.WORK.contractValue, "训练中", "动作进行符号，适合普通训练目标"),
    StageStyleIconLabel(TimedStageIconKey.SPEED_UP.contractValue, "加速", "速度线箭头符号，适合逐渐提速目标"),
    StageStyleIconLabel(TimedStageIconKey.SPRINT.contractValue, "冲刺", "闪电冲刺符号，适合爆发目标"),
    StageStyleIconLabel(TimedStageIconKey.REST.contractValue, "休息", "雪花冷静符号，适合普通休息"),
    StageStyleIconLabel(TimedStageIconKey.RECOVER_BREATHE.contractValue, "轮间恢复", "循环恢复和呼吸符号，适合轮间休息"),
    StageStyleIconLabel(TimedStageIconKey.COOLDOWN.contractValue, "放松", "下行降温和舒缓符号，适合放松阶段"),
    StageStyleIconLabel(TimedStageIconKey.STRENGTH.contractValue, "力量", "哑铃符号，适合力量训练提示"),
    StageStyleIconLabel(TimedStageIconKey.MOBILITY.contractValue, "灵活", "关节活动符号，适合灵活性和活动度目标"),
    StageStyleIconLabel(TimedStageIconKey.CUSTOM.contractValue, "自定义", "通用圆点符号，适合未归类阶段")
)

private fun TimedCompositionPlanEditorScreenState.updateTarget(
    stageId: String,
    targetId: String,
    transform: (TimedCompositionTargetEditorUiState) -> TimedCompositionTargetEditorUiState
): TimedCompositionPlanEditorScreenState {
    return copy(
        stageGroups = stageGroups.map { group ->
            if (group.id == stageId) {
                group.copy(
                    targets = group.targets.map { target ->
                        if (target.id == targetId) transform(target) else target
                    }
                )
            } else {
                group
            }
        },
        savedPlan = null,
        statusMessage = null
    )
}

private fun TimedCompositionPlanEditorScreenState.validateCompositionDraft(): String? {
    if (title.isBlank()) return "请填写计划名称。"
    validateIntegerText("热身秒数", warmupText, min = 0, max = MaxTimedCompositionDurationSec)?.let { return it }
    validateIntegerText("放松秒数", cooldownText, min = 0, max = MaxTimedCompositionDurationSec)?.let { return it }
    validateIntegerText("轮数", roundsText, min = 1, max = MaxTimedCompositionRounds)?.let { return it }
    validateIntegerText("轮间休息秒数", restBetweenRoundsText, min = 0, max = MaxTimedCompositionDurationSec)
        ?.let { return it }
    if (stageGroups.isEmpty()) return "请至少保留一个阶段。"
    stageGroups.forEach { group ->
        if (group.name.isBlank()) return "阶段名称不能为空。"
        if (group.targets.isEmpty()) return "${group.name} 至少需要一个目标。"
        if (group.targets.size > TIMED_COMPOSITION_MAX_TARGETS_PER_STAGE_GROUP) {
            return "${group.name} 最多 5 个目标。"
        }
        group.targets.forEach { target ->
            if (target.name.isBlank()) return "目标名称不能为空。"
            validateIntegerText(
                "${target.name} 时长秒数",
                target.durationText,
                min = MinTimedCompositionTargetDurationSec,
                max = MaxTimedCompositionDurationSec
            )?.let { return it }
        }
    }
    return null
}

private fun TimedCompositionPlanEditorScreenState.nextGeneratedStageId(
    prefix: String = "stage-added",
    startingSequence: Int = nextStageSequence
): Pair<String, Int> {
    val existingIds = stageGroups.map { group -> group.id }.toSet()
    return nextGeneratedId(prefix = prefix, existingIds = existingIds, startingSequence = startingSequence)
}

private fun TimedCompositionPlanEditorScreenState.nextGeneratedTargetId(
    prefix: String = "target-added",
    startingSequence: Int = nextTargetSequence
): Pair<String, Int> {
    val existingIds = stageGroups.flatMap { group -> group.targets.map { target -> target.id } }.toSet()
    return nextGeneratedId(prefix = prefix, existingIds = existingIds, startingSequence = startingSequence)
}

private fun nextGeneratedId(
    prefix: String,
    existingIds: Set<String>,
    startingSequence: Int
): Pair<String, Int> {
    var candidate = startingSequence.coerceAtLeast(1)
    var id = "$prefix-$candidate"
    while (id in existingIds) {
        candidate += 1
        id = "$prefix-$candidate"
    }
    return id to candidate + 1
}

private fun List<TimedCompositionStageGroupEditorUiState>.nextStageSequenceFor(prefix: String): Int {
    return map { group -> group.id }
        .nextSequenceFor(prefix = prefix)
}

private fun List<TimedCompositionTargetEditorUiState>.nextTargetSequenceFor(prefix: String): Int {
    return map { target -> target.id }
        .nextSequenceFor(prefix = prefix)
}

private fun List<String>.nextSequenceFor(prefix: String): Int {
    val used = mapNotNull { id -> id.removePrefix("$prefix-").toIntOrNull() }
    return (used.maxOrNull() ?: size) + 1
}

private fun TimedCompositionStageGroupDraft.toEditorUiState(): TimedCompositionStageGroupEditorUiState {
    return TimedCompositionStageGroupEditorUiState(
        id = id,
        name = name,
        colorHex = colorHex,
        iconKey = iconKey,
        order = order,
        targets = targets.map { target -> target.toEditorUiState() },
        compatibility = compatibility
    )
}

private fun TimedCompositionTargetDraft.toEditorUiState(): TimedCompositionTargetEditorUiState {
    return TimedCompositionTargetEditorUiState(
        id = id,
        name = name,
        kind = kind,
        colorHex = colorHex,
        iconKey = iconKey,
        durationSec = durationSec.coerceIn(MinTimedCompositionTargetDurationSec, MaxTimedCompositionDurationSec),
        order = order,
        compatibility = compatibility
    )
}

private fun defaultCompositionStageGroups(): List<TimedCompositionStageGroupEditorUiState> {
    return listOf(
        TimedCompositionStageGroupEditorUiState(
            id = "stage-main",
            name = "高强工作",
            colorHex = TimedCompositionTargetKind.ACTION.defaultColorHex,
            iconKey = TimedCompositionTargetKind.ACTION.defaultIconKey,
            order = 1,
            targets = listOf(
                TimedCompositionTargetEditorUiState(
                    id = "target-main-action",
                    name = "动作",
                    kind = TimedCompositionTargetKind.ACTION,
                    colorHex = TimedCompositionTargetKind.ACTION.defaultColorHex,
                    iconKey = TimedCompositionTargetKind.ACTION.defaultIconKey,
                    durationSec = 45,
                    order = 1
                ),
                TimedCompositionTargetEditorUiState(
                    id = "target-main-rest",
                    name = "休息",
                    kind = TimedCompositionTargetKind.REST,
                    colorHex = TimedCompositionTargetKind.REST.defaultColorHex,
                    iconKey = TimedCompositionTargetKind.REST.defaultIconKey,
                    durationSec = 15,
                    order = 2
                )
            )
        ),
        TimedCompositionStageGroupEditorUiState(
            id = "stage-core",
            name = "冲刺组合",
            colorHex = TimedCompositionTargetKind.ACTION.defaultColorHex,
            iconKey = TimedStageIconKey.SPRINT.contractValue,
            order = 2,
            targets = listOf(
                TimedCompositionTargetEditorUiState(
                    id = "target-core-hold",
                    name = "冲刺",
                    kind = TimedCompositionTargetKind.ACTION,
                    colorHex = TimedCompositionTargetKind.ACTION.defaultColorHex,
                    iconKey = TimedStageIconKey.SPRINT.contractValue,
                    durationSec = 120,
                    order = 1
                )
            )
        )
    )
}

private fun String.limitHalfWidthUnits(maxUnits: Int): String {
    var usedUnits = 0
    val builder = StringBuilder()
    for (char in this) {
        val units = if (char.isHalfWidthNameChar()) 1 else 2
        if (usedUnits + units > maxUnits) break
        builder.append(char)
        usedUnits += units
    }
    return builder.toString()
}

private fun Char.isHalfWidthNameChar(): Boolean {
    return code in 0x0000..0x007F || code in 0xFF61..0xFFDC || code in 0xFFE8..0xFFEE
}
