package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan

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
    val rounds: Int,
    val roundsRawText: String? = null,
    val restBetweenRoundsSec: Int,
    val restBetweenRoundsRawText: String? = null,
    val actionCue: CountdownCueUiState,
    val restCue: CountdownCueUiState,
    val stages: List<TimedPlanEditorStageUiState>,
    val themeColorHex: String = "#2FBF8F",
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

    fun toWorkoutPlan(
        planId: String = "plan-timed-draft",
        timestamp: String = DefaultTimedPlanTimestamp
    ): WorkoutPlan {
        val cueSafeState = constrainCueSettings()
        val blocks = cueSafeState.toPlanBlocks()

        return WorkoutPlan(
            id = planId,
            mode = WorkoutMode.TIMED,
            title = title.trim(),
            description = "E10.2 纯间歇计时器草稿",
            blocks = blocks,
            preferences = PlanPreferences(
                cueSettings = CueSettings(
                    actionEnding = cueSafeState.actionCue.toCountdownCue(),
                    restEnding = cueSafeState.restCue.toCountdownCue().takeIf {
                        cueSafeState.hasPositiveRestDuration()
                    }
                )
            ),
            createdAt = timestamp,
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
    val durationRawText: String? = null
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
    val sanitized = colorHex.takeIf { it.matches(Regex("#[0-9A-Fa-f]{6}")) } ?: return this
    return copy(
        stages = stages.map { stage -> if (stage.id == stageId) stage.copy(colorHex = sanitized) else stage },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.addStage(
    stageType: TimedStageType = TimedStageType.WORK
): TimedPlanEditorScreenState {
    val nextIndex = stages.size + 1
    return copy(
        stages = stages + TimedPlanEditorStageUiState(
            id = "stage-added-$nextIndex",
            name = stageType.displayName,
            stageType = stageType,
            durationSec = if (stageType == TimedStageType.REST) 20 else 45
        ),
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.copyStage(stageId: String): TimedPlanEditorScreenState {
    val index = stages.indexOfFirst { stage -> stage.id == stageId }
    if (index < 0) return this
    val source = stages[index]
    val copied = source.copy(
        id = "${source.id}-copy-${stages.count { it.id.startsWith("${source.id}-copy") } + 1}",
        name = "${source.name} 副本"
    )
    return copy(
        stages = stages.take(index + 1) + copied + stages.drop(index + 1),
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
    return copy(
        stages = stages.toMutableList().also { list ->
            val stage = list.removeAt(fromIndex)
            list.add(toIndex, stage)
        },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.saveDraftPlan(
    timestamp: String = DefaultTimedPlanTimestamp
): TimedPlanEditorScreenState {
    if (!canSave) {
        return copy(statusMessage = validationMessage ?: "请至少保留一个阶段并填写计划名称。")
    }

    return copy(
        savedPlan = toWorkoutPlan(timestamp = timestamp),
        statusMessage = "已生成本次纯间歇计时草稿，可用于当前内存态计划预览；真实保存后续接入。"
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
    val repeatedStages = stages.filterNot { stage ->
        stage.stageType == TimedStageType.WARMUP || stage.stageType == TimedStageType.COOLDOWN
    }
    val blocks = mutableListOf<PlanBlock>()
    stages.forEach { stage ->
        when (stage.stageType) {
            TimedStageType.WARMUP -> blocks += WarmupBlock(
                id = stage.id,
                order = blocks.size + 1,
                title = stage.name,
                durationSec = stage.durationSec
            )

            TimedStageType.COOLDOWN -> Unit
            else -> Unit
        }
    }
    if (repeatedStages.isNotEmpty()) {
        blocks += TimedCircuitBlock(
            id = "timed-interval-stages",
            order = blocks.size + 1,
            title = "间歇阶段",
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec.takeIf { it > 0 },
            items = repeatedStages.map { stage ->
                stage.toTimedExerciseItem()
            }
        )
    }
    stages.forEach { stage ->
        if (stage.stageType == TimedStageType.COOLDOWN) {
            blocks += CooldownBlock(
                id = stage.id,
                order = blocks.size + 1,
                title = stage.name,
                durationSec = stage.durationSec
            )
        }
    }
    return blocks
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
