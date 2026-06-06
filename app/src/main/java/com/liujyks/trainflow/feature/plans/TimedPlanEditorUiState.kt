package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.data.fixture.ActionExerciseFixture
import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.ExerciseSide
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
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
    val warmupDurationSec: Int,
    val warmupDurationRawText: String? = null,
    val stretchDurationSec: Int,
    val stretchDurationRawText: String? = null,
    val rounds: Int,
    val roundsRawText: String? = null,
    val restBetweenRoundsSec: Int,
    val restBetweenRoundsRawText: String? = null,
    val actionCue: CountdownCueUiState,
    val restCue: CountdownCueUiState,
    val items: List<TimedPlanEditorItemUiState>,
    val selectableExercises: List<TimedExerciseOptionUiState>,
    val savedPlan: WorkoutPlan? = null,
    val statusMessage: String? = null
) {
    val warmupDurationText: String
        get() = warmupDurationRawText ?: warmupDurationSec.toString()

    val stretchDurationText: String
        get() = stretchDurationRawText ?: stretchDurationSec.toString()

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
            val perRound = items.sumOf { item -> item.workDurationSec + item.restAfterSec }
            val roundRest = restBetweenRoundsSec * (rounds - 1).coerceAtLeast(0)
            return warmupDurationSec + perRound * rounds + roundRest + stretchDurationSec
        }

    val summary: String
        get() = "${items.size} 个动作 · ${rounds} 轮 · 预计 ${estimatedDurationSec.formatDuration()}"

    fun toWorkoutPlan(
        planId: String = "plan-timed-draft",
        timestamp: String = DefaultTimedPlanTimestamp
    ): WorkoutPlan {
        val cueSafeState = constrainCueSettings()
        val blocks = buildList {
            if (warmupDurationSec > 0) {
                add(
                    WarmupBlock(
                        id = "warmup-default",
                        order = size + 1,
                        title = "热身",
                        durationSec = warmupDurationSec
                    )
                )
            }

            add(
                TimedCircuitBlock(
                    id = "timed-circuit-main",
                    order = size + 1,
                    title = "正式训练",
                    rounds = rounds,
                    restBetweenRoundsSec = restBetweenRoundsSec.takeIf { it > 0 },
                    items = items.mapIndexed { index, item ->
                        item.toTimedExerciseItem(order = index + 1)
                    }
                )
            )

            if (stretchDurationSec > 0) {
                add(
                    StretchBlock(
                        id = "stretch-placeholder",
                        order = size + 1,
                        title = "拉伸",
                        durationSec = stretchDurationSec
                    )
                )
            }
        }

        return WorkoutPlan(
            id = planId,
            mode = WorkoutMode.TIMED,
            title = title.trim(),
            description = "E2.2 内存态计时计划草稿",
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

internal data class TimedPlanEditorItemUiState(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val shortCue: String,
    val side: ExerciseSide?,
    val workDurationSec: Int,
    val workDurationRawText: String? = null,
    val restAfterSec: Int,
    val restAfterRawText: String? = null,
    val autoAdvance: Boolean = true
) {
    val workDurationText: String
        get() = workDurationRawText ?: workDurationSec.toString()

    val restAfterText: String
        get() = restAfterRawText ?: restAfterSec.toString()

    fun toTimedExerciseItem(order: Int): TimedExerciseItem {
        return TimedExerciseItem(
            id = "timed-item-$order",
            exerciseId = exerciseId,
            side = side,
            workDurationSec = workDurationSec,
            restAfterSec = restAfterSec.takeIf { it > 0 },
            autoAdvance = autoAdvance
        )
    }
}

internal data class TimedExerciseOptionUiState(
    val exerciseId: String,
    val exerciseName: String,
    val defaultSummary: String
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

internal fun buildDefaultTimedPlanEditorState(
    entries: List<ActionExerciseFixture> = FirstActionExerciseFixtures.entries,
    defaults: PlanEditorDefaults = PlanEditorDefaults()
): TimedPlanEditorScreenState {
    val timedEntries = entries.filter { it.exercise.capabilities.supportsTimedTraining }
    val defaultItems = timedEntries
        .filter { it.onboardingSuitable }
        .take(3)
        .mapIndexed { index, entry -> entry.toEditorItem(index + 1) }

    return TimedPlanEditorScreenState(
        title = "全身计时循环",
        warmupDurationSec = 180,
        stretchDurationSec = 120,
        rounds = 2,
        restBetweenRoundsSec = 60,
        actionCue = defaults.actionCueDefaults(),
        restCue = defaults.restCueDefaults(),
        items = defaultItems,
        selectableExercises = timedEntries.map { entry -> entry.toOption() }
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateTitle(value: String): TimedPlanEditorScreenState {
    return copy(title = value, savedPlan = null, statusMessage = null)
}

internal fun TimedPlanEditorScreenState.updateWarmupDuration(seconds: Int): TimedPlanEditorScreenState {
    val sanitized = seconds.sanitizeDuration()
    return copy(
        warmupDurationSec = sanitized,
        warmupDurationRawText = sanitized.toString(),
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.updateStretchDuration(seconds: Int): TimedPlanEditorScreenState {
    val sanitized = seconds.sanitizeDuration()
    return copy(
        stretchDurationSec = sanitized,
        stretchDurationRawText = sanitized.toString(),
        savedPlan = null,
        statusMessage = null
    )
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
    )
        .constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateWarmupDurationText(input: String): TimedPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeDuration()
    return copy(
        warmupDurationSec = parsed ?: warmupDurationSec,
        warmupDurationRawText = cleaned,
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.updateStretchDurationText(input: String): TimedPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeDuration()
    return copy(
        stretchDurationSec = parsed ?: stretchDurationSec,
        stretchDurationRawText = cleaned,
        savedPlan = null,
        statusMessage = null
    )
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

internal fun TimedPlanEditorScreenState.updateItemWorkDuration(
    itemId: String,
    seconds: Int
): TimedPlanEditorScreenState {
    return copy(
        items = items.map { item ->
            if (item.id == itemId) {
                val sanitized = seconds.sanitizeDuration(min = 5)
                item.copy(workDurationSec = sanitized, workDurationRawText = sanitized.toString())
            } else {
                item
            }
        },
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateItemRestAfter(
    itemId: String,
    seconds: Int
): TimedPlanEditorScreenState {
    return copy(
        items = items.map { item ->
            if (item.id == itemId) {
                val sanitized = seconds.sanitizeDuration()
                item.copy(restAfterSec = sanitized, restAfterRawText = sanitized.toString())
            } else {
                item
            }
        },
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateItemWorkDurationText(
    itemId: String,
    input: String
): TimedPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeDuration(min = 5)
    return copy(
        items = items.map { item ->
            if (item.id == itemId) {
                item.copy(
                    workDurationSec = parsed ?: item.workDurationSec,
                    workDurationRawText = cleaned
                )
            } else {
                item
            }
        },
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.updateItemRestAfterText(
    itemId: String,
    input: String
): TimedPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeDuration()
    return copy(
        items = items.map { item ->
            if (item.id == itemId) {
                item.copy(
                    restAfterSec = parsed ?: item.restAfterSec,
                    restAfterRawText = cleaned
                )
            } else {
                item
            }
        },
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.addExercise(exerciseId: String): TimedPlanEditorScreenState {
    if (items.any { it.exerciseId == exerciseId }) return this

    val entry = FirstActionExerciseFixtures.entries
        .firstOrNull { it.exercise.id == exerciseId && it.exercise.capabilities.supportsTimedTraining }
        ?: return this

    return copy(
        items = items + entry.toEditorItem(items.size + 1),
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.removeItem(itemId: String): TimedPlanEditorScreenState {
    if (items.size <= 1) return this

    return copy(
        items = items.filterNot { it.id == itemId },
        savedPlan = null,
        statusMessage = null
    ).constrainCueSettings()
}

internal fun TimedPlanEditorScreenState.saveDraftPlan(
    timestamp: String = DefaultTimedPlanTimestamp
): TimedPlanEditorScreenState {
    if (!canSave) {
        return copy(statusMessage = validationMessage ?: "请至少保留一个动作并填写计划名称。")
    }

    return copy(
        savedPlan = toWorkoutPlan(timestamp = timestamp),
        statusMessage = "已生成本次计时计划草稿，可用于当前内存态计划预览；真实保存后续接入。"
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

private fun ActionExerciseFixture.toEditorItem(order: Int): TimedPlanEditorItemUiState {
    val default = requireNotNull(timedDefault) {
        "Timed editor can only consume timed fixture defaults."
    }

    return TimedPlanEditorItemUiState(
        id = "editor-item-$order-${exercise.id}",
        exerciseId = exercise.id,
        exerciseName = exercise.name,
        shortCue = exercise.instructions.shortCue,
        side = default.side,
        workDurationSec = default.workDurationSec,
        restAfterSec = default.restAfterSec
    )
}

private fun ActionExerciseFixture.toOption(): TimedExerciseOptionUiState {
    val default = requireNotNull(timedDefault) {
        "Timed option can only consume timed fixture defaults."
    }

    return TimedExerciseOptionUiState(
        exerciseId = exercise.id,
        exerciseName = exercise.name,
        defaultSummary = "${default.workDurationSec}秒 / 休息${default.restAfterSec}秒"
    )
}

private fun Int.sanitizeDuration(min: Int = 0): Int = coerceIn(min, 3600)

private fun TimedPlanEditorScreenState.constrainCueSettings(): TimedPlanEditorScreenState {
    return copy(
        actionCue = actionCue.constrainToDuration(minWorkDurationSec()),
        restCue = restCue.constrainToRestDuration(minPositiveRestDurationSec())
    )
}

private fun TimedPlanEditorScreenState.validateTimedDraft(): String? {
    if (title.isBlank()) return "请填写计划名称。"
    if (items.isEmpty()) return "请至少保留一个动作。"
    validateIntegerText("热身秒数", warmupDurationText, min = 0, max = 3600)?.let { return it }
    validateIntegerText("拉伸秒数", stretchDurationText, min = 0, max = 3600)?.let { return it }
    validateIntegerText("轮数", roundsText, min = 1, max = 12)?.let { return it }
    validateIntegerText("轮间休息秒数", restBetweenRoundsText, min = 0, max = 3600)?.let { return it }
    validateIntegerText("动作提醒阈值秒数", actionCue.thresholdText, min = 1, max = 60)?.let { return it }
    validateIntegerText("休息提醒阈值秒数", restCue.thresholdText, min = 1, max = 60)?.let { return it }
    items.forEach { item ->
        validateIntegerText("${item.exerciseName} 动作秒数", item.workDurationText, min = 5, max = 3600)
            ?.let { return it }
        validateIntegerText("${item.exerciseName} 动作后休息秒数", item.restAfterText, min = 0, max = 3600)
            ?.let { return it }
    }
    return null
}

private fun TimedPlanEditorScreenState.minWorkDurationSec(): Int? {
    return items.minOfOrNull { item -> item.workDurationSec }
}

private fun TimedPlanEditorScreenState.minPositiveRestDurationSec(): Int? {
    return (items.map { item -> item.restAfterSec } + restBetweenRoundsSec)
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
