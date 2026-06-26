package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.MoreStageColorPresets
import com.liujyks.trainflow.core.model.RecommendedStageColorPresets
import com.liujyks.trainflow.core.model.RestBlock
import com.liujyks.trainflow.core.model.StageColorPreset
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.normalizeStageColorHex
import com.liujyks.trainflow.core.model.stageColorPresetFor
import com.liujyks.trainflow.core.model.stageTextColorHexFor

internal const val DefaultTimedPlanTimestamp = "2026-05-28T00:00:00Z"

private const val MinCueThresholdSec = 1
private const val MaxCueThresholdSec = 60

internal data class PlanEditorDefaults(
    val actionCueEnabled: Boolean = true,
    val restCueEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val emphasisAnimationEnabled: Boolean = true,
    val defaultCountdownThresholdSec: Int = CountdownCue.DEFAULT_THRESHOLD_SEC,
    val strengthSetTimerMode: StrengthSetTimerMode = StrengthSetTimerMode.MANUAL_START
) {
    val safeCountdownThresholdSec: Int
        get() = defaultCountdownThresholdSec.sanitizeCueThreshold()

    fun actionCueDefaults(): CountdownCueUiState {
        return countdownCueDefaults(enabled = actionCueEnabled)
    }

    fun restCueDefaults(): CountdownCueUiState {
        return countdownCueDefaults(enabled = restCueEnabled)
    }

    private fun countdownCueDefaults(enabled: Boolean): CountdownCueUiState {
        return CountdownCueUiState(
            enabled = enabled,
            thresholdSec = safeCountdownThresholdSec,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            emphasisAnimationEnabled = emphasisAnimationEnabled
        )
    }
}

internal data class TimedPlanEditorScreenState(
    val title: String,
    val description: String = "",
    val rounds: Int,
    val roundsRawText: String? = null,
    val restBetweenRoundsSec: Int,
    val restBetweenRoundsRawText: String? = null,
    val actionCue: CountdownCueUiState,
    val restCue: CountdownCueUiState,
    val stages: List<TimedPlanEditorStageUiState>,
    val themeColorHex: String = "#2FBF8F",
    val nextStageSequence: Int = 1,
    val sourcePlanId: String? = null,
    val sourceCreatedAt: String? = null,
    val originalPlanMetadata: OriginalPlanMetadata? = null,
    val savedPlan: WorkoutPlan? = null,
    val statusMessage: String? = null
) {
    val roundsText: String
        get() = roundsRawText ?: rounds.toString()

    val restBetweenRoundsText: String
        get() = restBetweenRoundsRawText ?: restBetweenRoundsSec.toString()

    val validationMessage: String?
        get() = validateTimedDraft()

    val canSave: Boolean = validationMessage == null

    val canStartTraining: Boolean
        get() = canSave

    val estimatedDurationSec: Int
        get() {
            val warmupAndCooldown = stages
                .filter { stage -> stage.stageType == TimedStageType.WARMUP || stage.stageType == TimedStageType.COOLDOWN }
                .sumOf { stage -> stage.durationSec }
            val repeatedStages = stages
                .filterNot { stage -> stage.stageType == TimedStageType.WARMUP || stage.stageType == TimedStageType.COOLDOWN }
            val perRound = repeatedStages.sumOf { stage -> stage.durationSec }
            val roundRest = restBetweenRoundsSec * (rounds - 1).coerceAtLeast(0)
            return warmupAndCooldown + perRound * rounds + roundRest
        }

    val summary: String
        get() = "${stages.size} 个阶段 · ${rounds} 轮 · 预计 ${estimatedDurationSec.formatDuration()}"

    val isEditingExistingPlan: Boolean
        get() = sourcePlanId != null

    fun toWorkoutPlan(
        planId: String = sourcePlanId ?: "plan-timed-draft",
        timestamp: String = DefaultTimedPlanTimestamp
    ): WorkoutPlan {
        val cueSafeState = constrainCueSettings()
        val blocks = cueSafeState.toPlanBlocks()
        val cueSettings = CueSettings(
            actionEnding = cueSafeState.actionCue.toCountdownCue(),
            restEnding = cueSafeState.restCue.toCountdownCue().takeIf {
                cueSafeState.hasPositiveRestDuration()
            }
        )
        val preferences = (originalPlanMetadata?.preferences ?: PlanPreferences()).copy(
            cueSettings = cueSettings
        )

        return WorkoutPlan(
            id = planId,
            mode = WorkoutMode.TIMED,
            title = title.trim(),
            description = description.ifBlank { "本地保存的纯间歇计时器计划" },
            blocks = blocks,
            reminder = originalPlanMetadata?.reminder,
            preferences = preferences,
            createdAt = originalPlanMetadata?.createdAt ?: sourceCreatedAt ?: timestamp,
            updatedAt = timestamp
        )
    }
}

internal data class TimedPlanEditorStageUiState(
    val id: String,
    val name: String,
    val stageType: TimedStageType,
    val iconKey: String = stageType.defaultIconKey,
    val colorHex: String = stageType.defaultColorHex,
    val durationSec: Int,
    val durationRawText: String? = null,
    val cueSettings: CueSettings? = null
) {
    val durationText: String
        get() = durationRawText ?: durationSec.toString()

    val typeLabel: String
        get() = stageType.displayName

    fun toTimedExerciseItem(): TimedExerciseItem {
        return TimedExerciseItem(
            id = id,
            exerciseId = null,
            labelOverride = name.trim().ifBlank { stageType.displayName },
            stageType = stageType,
            iconKey = iconKey,
            colorHex = colorHex,
            workDurationSec = durationSec,
            restAfterSec = null,
            cueSettings = cueSettings,
            autoAdvance = true
        )
    }
}

internal data class TimedStageTypeOptionUiState(
    val stageType: TimedStageType,
    val label: String,
    val iconKey: String,
    val colorHex: String
)

internal data class StageColorOptionUiState(
    val id: String,
    val name: String,
    val hex: String,
    val tone: String,
    val recommendedUse: String,
    val textColor: String,
    val isHighAttention: Boolean,
    val selected: Boolean,
    val hasCheckIndicator: Boolean,
    val contentDescription: String
)

internal data class StageColorPickerUiState(
    val selectedColorHex: String,
    val selectedColorName: String,
    val selectedTextColorHex: String,
    val recommendedColors: List<StageColorOptionUiState>,
    val moreColors: List<StageColorOptionUiState>
)

internal data class CountdownCueUiState(
    val enabled: Boolean = true,
    val thresholdSec: Int = CountdownCue.DEFAULT_THRESHOLD_SEC,
    val thresholdRawText: String? = null,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val emphasisAnimationEnabled: Boolean = true
) {
    val thresholdText: String
        get() = thresholdRawText ?: thresholdSec.toString()

    fun toCountdownCue(): CountdownCue {
        return CountdownCue(
            enabled = enabled,
            thresholdSec = thresholdSec,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            emphasisAnimationEnabled = emphasisAnimationEnabled,
            voiceCueEnabled = false
        )
    }
}

internal val TimedStageTypeOptions: List<TimedStageTypeOptionUiState> = TimedStageType.entries.map { type ->
    TimedStageTypeOptionUiState(
        stageType = type,
        label = type.displayName,
        iconKey = type.defaultIconKey,
        colorHex = type.defaultColorHex
    )
}

internal fun TimedPlanEditorStageUiState.toStageColorPickerUiState(): StageColorPickerUiState {
    val selectedHex = normalizeStageColorHex(colorHex, stageType)
    val selectedPreset = stageColorPresetFor(selectedHex)
    return StageColorPickerUiState(
        selectedColorHex = selectedHex,
        selectedColorName = selectedPreset?.name ?: "自定义颜色",
        selectedTextColorHex = stageTextColorHexFor(selectedHex, stageType),
        recommendedColors = RecommendedStageColorPresets.map { preset ->
            preset.toOption(selectedHex)
        },
        moreColors = MoreStageColorPresets.map { preset ->
            preset.toOption(selectedHex)
        }
    )
}

private fun StageColorPreset.toOption(selectedHex: String): StageColorOptionUiState {
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

internal fun buildDefaultTimedPlanEditorState(
    defaults: PlanEditorDefaults = PlanEditorDefaults()
): TimedPlanEditorScreenState {
    return TimedPlanEditorScreenState(
        title = "纯间歇计时器",
        rounds = 3,
        restBetweenRoundsSec = 60,
        actionCue = defaults.actionCueDefaults(),
        restCue = defaults.restCueDefaults(),
        stages = listOf(
            TimedPlanEditorStageUiState(
                id = "stage-warmup",
                name = "热身",
                stageType = TimedStageType.WARMUP,
                durationSec = 180
            ),
            TimedPlanEditorStageUiState(
                id = "stage-work",
                name = "训练",
                stageType = TimedStageType.WORK,
                durationSec = 45
            ),
            TimedPlanEditorStageUiState(
                id = "stage-rest",
                name = "休息",
                stageType = TimedStageType.REST,
                durationSec = 15
            ),
            TimedPlanEditorStageUiState(
                id = "stage-custom",
                name = "核心保持",
                stageType = TimedStageType.CUSTOM,
                durationSec = 30
            ),
            TimedPlanEditorStageUiState(
                id = "stage-cooldown",
                name = "放松",
                stageType = TimedStageType.COOLDOWN,
                durationSec = 120
            )
        )
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateTitle(value: String): TimedPlanEditorScreenState {
    return copy(title = value, savedPlan = null, statusMessage = null)
}

internal fun TimedPlanEditorScreenState.updateDescription(value: String): TimedPlanEditorScreenState {
    return copy(description = value, savedPlan = null, statusMessage = null)
}

internal fun TimedPlanEditorScreenState.updateRounds(value: Int): TimedPlanEditorScreenState {
    val sanitized = value.coerceIn(1, 12)
    return copy(
        rounds = sanitized,
        roundsRawText = sanitized.toString(),
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.updateRestBetweenRounds(seconds: Int): TimedPlanEditorScreenState {
    val sanitized = seconds.sanitizeDuration()
    return copy(
        restBetweenRoundsSec = sanitized,
        restBetweenRoundsRawText = sanitized.toString(),
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateRoundsText(input: String): TimedPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.coerceIn(1, 12)
    return copy(
        rounds = parsed ?: rounds,
        roundsRawText = cleaned,
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.updateRestBetweenRoundsText(input: String): TimedPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeDuration()
    return copy(
        restBetweenRoundsSec = parsed ?: restBetweenRoundsSec,
        restBetweenRoundsRawText = cleaned,
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateActionCueThreshold(seconds: Int): TimedPlanEditorScreenState {
    val sanitized = seconds.sanitizeCueThreshold()
    return copy(
        actionCue = actionCue.copy(thresholdSec = sanitized, thresholdRawText = sanitized.toString()),
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateRestCueThreshold(seconds: Int): TimedPlanEditorScreenState {
    val sanitized = seconds.sanitizeCueThreshold()
    return copy(
        restCue = restCue.copy(thresholdSec = sanitized, thresholdRawText = sanitized.toString()),
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateActionCueThresholdText(input: String): TimedPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeCueThreshold()
    return copy(
        actionCue = actionCue.copy(
            thresholdSec = parsed ?: actionCue.thresholdSec,
            thresholdRawText = cleaned
        ),
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateRestCueThresholdText(input: String): TimedPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeCueThreshold()
    return copy(
        restCue = restCue.copy(
            thresholdSec = parsed ?: restCue.thresholdSec,
            thresholdRawText = cleaned
        ),
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateActionCueEnabled(enabled: Boolean): TimedPlanEditorScreenState {
    return copy(actionCue = actionCue.copy(enabled = enabled), savedPlan = null, statusMessage = null)
        .constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateRestCueEnabled(enabled: Boolean): TimedPlanEditorScreenState {
    return copy(restCue = restCue.copy(enabled = enabled), savedPlan = null, statusMessage = null)
        .constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateSoundEnabled(enabled: Boolean): TimedPlanEditorScreenState {
    return copy(
        actionCue = actionCue.copy(soundEnabled = enabled),
        restCue = restCue.copy(soundEnabled = enabled),
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.updateVibrationEnabled(enabled: Boolean): TimedPlanEditorScreenState {
    return copy(
        actionCue = actionCue.copy(vibrationEnabled = enabled),
        restCue = restCue.copy(vibrationEnabled = enabled),
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.updateEmphasisAnimationEnabled(enabled: Boolean): TimedPlanEditorScreenState {
    return copy(
        actionCue = actionCue.copy(emphasisAnimationEnabled = enabled),
        restCue = restCue.copy(emphasisAnimationEnabled = enabled),
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.updateStageName(
    stageId: String,
    name: String
): TimedPlanEditorScreenState {
    return copy(
        stages = stages.map { stage -> if (stage.id == stageId) stage.copy(name = name) else stage },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.updateStageDuration(
    stageId: String,
    seconds: Int
): TimedPlanEditorScreenState {
    val sanitized = seconds.sanitizeDuration(min = 5)
    return copy(
        stages = stages.map { stage ->
            if (stage.id == stageId) {
                stage.copy(durationSec = sanitized, durationRawText = sanitized.toString())
            } else {
                stage
            }
        },
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateStageDurationText(
    stageId: String,
    input: String
): TimedPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeDuration(min = 5)
    return copy(
        stages = stages.map { stage ->
            if (stage.id == stageId) {
                stage.copy(
                    durationSec = parsed ?: stage.durationSec,
                    durationRawText = cleaned
                )
            } else {
                stage
            }
        },
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateStageType(
    stageId: String,
    stageType: TimedStageType
): TimedPlanEditorScreenState {
    return copy(
        stages = stages.map { stage ->
            if (stage.id == stageId) {
                stage.copy(
                    stageType = stageType,
                    iconKey = stageType.defaultIconKey,
                    colorHex = stageType.defaultColorHex
                )
            } else {
                stage
            }
        },
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateStageColor(
    stageId: String,
    colorHex: String
): TimedPlanEditorScreenState {
    return copy(
        stages = stages.map { stage ->
            if (stage.id == stageId) {
                stage.copy(colorHex = normalizeStageColorHex(colorHex, stage.stageType))
            } else {
                stage
            }
        },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.addStage(
    stageType: TimedStageType = TimedStageType.WORK
): TimedPlanEditorScreenState {
    val (newStageId, nextSequence) = nextGeneratedStageId(prefix = "stage-added")
    val newStage = TimedPlanEditorStageUiState(
        id = newStageId,
        name = stageType.displayName,
        stageType = stageType,
        durationSec = if (stageType == TimedStageType.REST) 20 else 45
    )
    val insertIndex = stages.insertIndexForNewStage(stageType)
    return copy(
        stages = stages.take(insertIndex) + newStage + stages.drop(insertIndex),
        nextStageSequence = nextSequence,
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.copyStage(stageId: String): TimedPlanEditorScreenState {
    val index = stages.indexOfFirst { stage -> stage.id == stageId }
    if (index < 0) return this
    val source = stages[index]
    val (copiedId, nextSequence) = nextGeneratedStageId(prefix = "${source.id}-copy")
    val copied = source.copy(
        id = copiedId,
        name = "${source.name} 副本"
    )
    return copy(
        stages = stages.take(index + 1) + copied + stages.drop(index + 1),
        nextStageSequence = nextSequence,
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.removeStage(stageId: String): TimedPlanEditorScreenState {
    if (stages.size <= 1) return this
    return copy(
        stages = stages.filterNot { stage -> stage.id == stageId },
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.moveStageUp(stageId: String): TimedPlanEditorScreenState {
    val index = stages.indexOfFirst { stage -> stage.id == stageId }
    return moveStage(fromIndex = index, toIndex = index - 1)
}

internal fun TimedPlanEditorScreenState.moveStageDown(stageId: String): TimedPlanEditorScreenState {
    val index = stages.indexOfFirst { stage -> stage.id == stageId }
    return moveStage(fromIndex = index, toIndex = index + 1)
}

internal fun TimedPlanEditorScreenState.moveStage(
    fromIndex: Int,
    toIndex: Int
): TimedPlanEditorScreenState {
    if (fromIndex !in stages.indices || toIndex !in stages.indices || fromIndex == toIndex) return this
    val movedStages = stages.withItemMoved(fromIndex, toIndex)
    return copy(
        stages = movedStages,
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.reorderStages(stageIds: List<String>): TimedPlanEditorScreenState {
    if (stageIds.size != stages.size || stageIds.toSet() != stages.map { it.id }.toSet()) return this
    if (stageIds == stages.map { it.id }) return this
    val stagesById = stages.associateBy { stage -> stage.id }
    return copy(
        stages = stageIds.mapNotNull { stageId -> stagesById[stageId] },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.canMoveStageUp(stageId: String): Boolean {
    val index = stages.indexOfFirst { stage -> stage.id == stageId }
    return index > 0
}

internal fun TimedPlanEditorScreenState.canMoveStageDown(stageId: String): Boolean {
    val index = stages.indexOfFirst { stage -> stage.id == stageId }
    return index >= 0 && index < stages.lastIndex
}

internal fun TimedPlanEditorScreenState.saveDraftPlan(
    planId: String = sourcePlanId ?: "plan-timed-draft",
    timestamp: String = DefaultTimedPlanTimestamp
): TimedPlanEditorScreenState {
    if (!canSave) {
        return copy(statusMessage = validationMessage ?: "请至少保留一个阶段并填写计划名称。")
    }

    return markPlanSaved(toWorkoutPlan(planId = planId, timestamp = timestamp))
}

internal fun TimedPlanEditorScreenState.markPlanSaved(plan: WorkoutPlan): TimedPlanEditorScreenState {
    return copy(
        savedPlan = plan,
        statusMessage = if (isEditingExistingPlan) {
            "已更新「${plan.title}」；计划详情和 ready gate 会使用最新配置，历史训练快照不改写。"
        } else {
            "已保存「${plan.title}」到本地计划；可切换页面或重新进入计划详情继续启动。"
        }
    )
}

internal fun WorkoutPlan.toTimedPlanEditorState(
    defaults: PlanEditorDefaults = PlanEditorDefaults()
): TimedPlanEditorScreenState {
    if (mode != WorkoutMode.TIMED) {
        return buildDefaultTimedPlanEditorState(defaults).copy(
            title = title,
            description = description.orEmpty(),
            sourcePlanId = id,
            sourceCreatedAt = createdAt,
            originalPlanMetadata = toOriginalPlanMetadata(),
            statusMessage = "当前计划不是计时训练，已使用安全默认草稿。"
        )
    }

    val orderedBlocks = blocks.sortedBy { block -> block.order }
    val circuit = orderedBlocks.filterIsInstance<TimedCircuitBlock>().firstOrNull()
    val mappedStages = orderedBlocks.flatMap { block -> block.toTimedEditorStages() }
    val safeStages = mappedStages.ifEmpty { buildDefaultTimedPlanEditorState(defaults).stages }
    val cueSettings = preferences?.cueSettings
    val state = TimedPlanEditorScreenState(
        title = title,
        description = description.orEmpty(),
        rounds = circuit?.rounds?.coerceIn(1, 12) ?: 1,
        restBetweenRoundsSec = circuit?.restBetweenRoundsSec?.coerceIn(0, 3600) ?: 0,
        actionCue = cueSettings?.actionEnding?.toCountdownCueUiState() ?: defaults.actionCueDefaults(),
        restCue = cueSettings?.restEnding?.toCountdownCueUiState() ?: defaults.restCueDefaults(),
        stages = safeStages,
        nextStageSequence = safeStages.size + 1,
        sourcePlanId = id,
        sourceCreatedAt = createdAt,
        originalPlanMetadata = toOriginalPlanMetadata(),
        statusMessage = if (mappedStages.isEmpty()) {
            "已载入计划基础信息，但原计划没有可回填的计时阶段，已使用安全默认阶段。"
        } else {
            "已载入已保存计划，可编辑后保存回同一个本地计划。"
        }
    )
    return state.constrainCueSettings()
}

private fun PlanBlock.toTimedEditorStages(): List<TimedPlanEditorStageUiState> {
    return when (this) {
        is WarmupBlock -> timedBoundaryStages(
            fallbackStageType = TimedStageType.WARMUP,
            fallbackDurationSec = durationSec,
            fallbackTitle = title
        )

        is CooldownBlock -> timedBoundaryStages(
            fallbackStageType = TimedStageType.COOLDOWN,
            fallbackDurationSec = durationSec,
            fallbackTitle = title
        )

        is StretchBlock -> timedBoundaryStages(
            fallbackStageType = TimedStageType.COOLDOWN,
            fallbackDurationSec = durationSec,
            fallbackTitle = title
        )

        is RestBlock -> listOf(
            TimedPlanEditorStageUiState(
                id = id,
                name = label ?: title ?: TimedStageType.REST.displayName,
                stageType = TimedStageType.REST,
                iconKey = TimedStageType.REST.defaultIconKey,
                colorHex = normalizeStageColorHex(TimedStageType.REST.defaultColorHex, TimedStageType.REST),
                durationSec = durationSec.sanitizeDuration(min = 5)
            )
        )

        is TimedCircuitBlock -> items.map { item -> item.toTimedEditorStage() }
        is StrengthExerciseBlock -> emptyList()
        is TimedCompositionBlock -> emptyList()
    }
}

private fun WarmupBlock.timedBoundaryStages(
    fallbackStageType: TimedStageType,
    fallbackDurationSec: Int?,
    fallbackTitle: String?
): List<TimedPlanEditorStageUiState> {
    return items.map { item ->
        item.toTimedEditorStage(fallbackStageType = fallbackStageType)
    }.ifEmpty {
        listOf(fallbackTimedBoundaryStage(id, fallbackStageType, fallbackDurationSec, fallbackTitle))
    }
}

private fun CooldownBlock.timedBoundaryStages(
    fallbackStageType: TimedStageType,
    fallbackDurationSec: Int?,
    fallbackTitle: String?
): List<TimedPlanEditorStageUiState> {
    return items.map { item ->
        item.toTimedEditorStage(fallbackStageType = fallbackStageType)
    }.ifEmpty {
        listOf(fallbackTimedBoundaryStage(id, fallbackStageType, fallbackDurationSec, fallbackTitle))
    }
}

private fun StretchBlock.timedBoundaryStages(
    fallbackStageType: TimedStageType,
    fallbackDurationSec: Int?,
    fallbackTitle: String?
): List<TimedPlanEditorStageUiState> {
    return items.map { item ->
        item.toTimedEditorStage(fallbackStageType = fallbackStageType)
    }.ifEmpty {
        listOf(fallbackTimedBoundaryStage(id, fallbackStageType, fallbackDurationSec, fallbackTitle))
    }
}

private fun fallbackTimedBoundaryStage(
    id: String,
    fallbackStageType: TimedStageType,
    fallbackDurationSec: Int?,
    fallbackTitle: String?
): TimedPlanEditorStageUiState {
    return TimedPlanEditorStageUiState(
        id = id,
        name = fallbackTitle ?: fallbackStageType.displayName,
        stageType = fallbackStageType,
        iconKey = fallbackStageType.defaultIconKey,
        colorHex = normalizeStageColorHex(fallbackStageType.defaultColorHex, fallbackStageType),
        durationSec = (fallbackDurationSec ?: 60).sanitizeDuration(min = 5)
    )
}

private fun TimedExerciseItem.toTimedEditorStage(
    fallbackStageType: TimedStageType = stageType
): TimedPlanEditorStageUiState {
    val safeStageType = if (stageType == TimedStageType.WORK && fallbackStageType != TimedStageType.WORK) {
        fallbackStageType
    } else {
        stageType
    }
    return TimedPlanEditorStageUiState(
        id = id,
        name = labelOverride?.takeIf { it.isNotBlank() } ?: safeStageType.displayName,
        stageType = safeStageType,
        iconKey = iconKey.ifBlank { safeStageType.defaultIconKey },
        colorHex = normalizeStageColorHex(colorHex, safeStageType),
        durationSec = workDurationSec.sanitizeDuration(min = 5),
        cueSettings = cueSettings
    )
}

private fun CountdownCue.toCountdownCueUiState(): CountdownCueUiState {
    return CountdownCueUiState(
        enabled = enabled,
        thresholdSec = thresholdSec.sanitizeCueThreshold(),
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        emphasisAnimationEnabled = emphasisAnimationEnabled
    )
}


internal fun Int.formatDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return if (seconds == 0) {
        "${minutes}分"
    } else {
        "${minutes}分${seconds}秒"
    }
}

private fun TimedPlanEditorScreenState.toPlanBlocks(): List<PlanBlock> {
    val blocks = mutableListOf<PlanBlock>()
    val circuitStages = mutableListOf<TimedPlanEditorStageUiState>()
    var circuitSequence = 1

    fun flushCircuitStages() {
        if (circuitStages.isEmpty()) return
        blocks += TimedCircuitBlock(
            id = if (circuitSequence == 1) {
                "timed-interval-stages"
            } else {
                "timed-interval-stages-$circuitSequence"
            },
            order = blocks.size + 1,
            title = "间歇阶段",
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec.takeIf { it > 0 },
            items = circuitStages.map { stage -> stage.toTimedExerciseItem() }
        )
        circuitStages.clear()
        circuitSequence += 1
    }

    stages.forEach { stage ->
        when (stage.stageType) {
            TimedStageType.WARMUP -> {
                flushCircuitStages()
                blocks += WarmupBlock(
                    id = stage.id,
                    order = blocks.size + 1,
                    title = stage.name,
                    durationSec = stage.durationSec,
                    items = listOf(stage.toTimedExerciseItem())
                )
            }

            TimedStageType.COOLDOWN -> {
                flushCircuitStages()
                blocks += CooldownBlock(
                    id = stage.id,
                    order = blocks.size + 1,
                    title = stage.name,
                    durationSec = stage.durationSec,
                    items = listOf(stage.toTimedExerciseItem())
                )
            }

            TimedStageType.WORK,
            TimedStageType.REST,
            TimedStageType.CUSTOM -> circuitStages += stage
        }
    }
    flushCircuitStages()
    return blocks
}

private fun TimedPlanEditorScreenState.nextGeneratedStageId(prefix: String): Pair<String, Int> {
    val existingIds = stages.map { stage -> stage.id }.toSet()
    var candidate = nextStageSequence.coerceAtLeast(1)
    var id = "$prefix-$candidate"
    while (id in existingIds) {
        candidate += 1
        id = "$prefix-$candidate"
    }
    return id to candidate + 1
}

private fun List<TimedPlanEditorStageUiState>.insertIndexForNewStage(
    stageType: TimedStageType
): Int {
    return size
}

private fun Int.sanitizeDuration(min: Int = 0): Int = coerceIn(min, 3600)

private fun TimedPlanEditorScreenState.constrainCueSettings(): TimedPlanEditorScreenState {
    return copy(
        actionCue = actionCue.constrainToDuration(minActionDurationSec()),
        restCue = restCue.constrainToRestDuration(minPositiveRestDurationSec())
    )
}

private fun TimedPlanEditorScreenState.validateTimedDraft(): String? {
    if (title.isBlank()) return "请填写计划名称。"
    if (stages.isEmpty()) return "请至少保留一个阶段。"
    validateIntegerText("轮数", roundsText, min = 1, max = 12)?.let { return it }
    validateIntegerText("轮间休息秒数", restBetweenRoundsText, min = 0, max = 3600)?.let { return it }
    validateIntegerText("阶段提醒阈值秒数", actionCue.thresholdText, min = 1, max = 60)?.let { return it }
    validateIntegerText("休息提醒阈值秒数", restCue.thresholdText, min = 1, max = 60)?.let { return it }
    stages.forEach { stage ->
        if (stage.name.isBlank()) return "${stage.typeLabel} 阶段名称不能为空。"
        validateIntegerText("${stage.name} 阶段秒数", stage.durationText, min = 5, max = 3600)
            ?.let { return it }
    }
    return null
}

private fun TimedPlanEditorScreenState.minActionDurationSec(): Int? {
    return stages
        .filter { stage -> stage.stageType != TimedStageType.REST }
        .minOfOrNull { stage -> stage.durationSec }
}

private fun TimedPlanEditorScreenState.minPositiveRestDurationSec(): Int? {
    return (stages
        .filter { stage -> stage.stageType == TimedStageType.REST }
        .map { stage -> stage.durationSec } + restBetweenRoundsSec)
        .filter { durationSec -> durationSec > 0 }
        .minOrNull()
}

private fun TimedPlanEditorScreenState.hasPositiveRestDuration(): Boolean {
    return minPositiveRestDurationSec() != null
}

private fun CountdownCueUiState.constrainToDuration(durationSec: Int?): CountdownCueUiState {
    if (thresholdRawText != null && thresholdRawText.isEmpty()) return this

    val maxThresholdSec = durationSec
        ?.coerceAtLeast(MinCueThresholdSec)
        ?.coerceAtMost(MaxCueThresholdSec)
        ?: MaxCueThresholdSec

    val constrainedThresholdSec = thresholdSec.coerceIn(MinCueThresholdSec, maxThresholdSec)
    return copy(
        thresholdSec = constrainedThresholdSec,
        thresholdRawText = thresholdRawText?.let { constrainedThresholdSec.toString() }
    )
}

private fun CountdownCueUiState.constrainToRestDuration(durationSec: Int?): CountdownCueUiState {
    if (durationSec == null) {
        return copy(enabled = false, thresholdSec = thresholdSec.sanitizeCueThreshold())
    }

    return constrainToDuration(durationSec)
}

private fun Int.sanitizeCueThreshold(): Int = coerceIn(MinCueThresholdSec, MaxCueThresholdSec)

internal fun String.sanitizeIntegerInput(): String {
    return filter { it.isDigit() }
}

internal fun validateIntegerText(
    label: String,
    text: String,
    min: Int,
    max: Int
): String? {
    if (text.isBlank()) return "$label 不能为空。"
    val parsed = text.toIntOrNull() ?: return "$label 请输入 $min-$max 之间的整数。"
    return if (parsed in min..max) null else "$label 请输入 $min-$max 之间的整数。"
}
