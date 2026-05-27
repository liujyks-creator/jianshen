package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.data.fixture.ActionExerciseFixture
import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.ExerciseSide
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan

internal const val DefaultTimedPlanTimestamp = "2026-05-28T00:00:00Z"

internal data class TimedPlanEditorScreenState(
    val title: String,
    val warmupDurationSec: Int,
    val stretchDurationSec: Int,
    val rounds: Int,
    val restBetweenRoundsSec: Int,
    val actionCue: CountdownCueUiState,
    val restCue: CountdownCueUiState,
    val items: List<TimedPlanEditorItemUiState>,
    val selectableExercises: List<TimedExerciseOptionUiState>,
    val savedPlan: WorkoutPlan? = null,
    val statusMessage: String? = null
) {
    val canSave: Boolean = title.isNotBlank() && items.isNotEmpty()

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
                    actionEnding = actionCue.toCountdownCue(),
                    restEnding = restCue.toCountdownCue()
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
    val restAfterSec: Int,
    val autoAdvance: Boolean = true
) {
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
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val emphasisAnimationEnabled: Boolean = true
) {
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
    entries: List<ActionExerciseFixture> = FirstActionExerciseFixtures.entries
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
        actionCue = CountdownCueUiState(),
        restCue = CountdownCueUiState(),
        items = defaultItems,
        selectableExercises = timedEntries.map { entry -> entry.toOption() }
    )
}

internal fun TimedPlanEditorScreenState.updateTitle(value: String): TimedPlanEditorScreenState {
    return copy(title = value, savedPlan = null, statusMessage = null)
}

internal fun TimedPlanEditorScreenState.updateWarmupDuration(seconds: Int): TimedPlanEditorScreenState {
    return copy(warmupDurationSec = seconds.sanitizeDuration(), savedPlan = null, statusMessage = null)
}

internal fun TimedPlanEditorScreenState.updateStretchDuration(seconds: Int): TimedPlanEditorScreenState {
    return copy(stretchDurationSec = seconds.sanitizeDuration(), savedPlan = null, statusMessage = null)
}

internal fun TimedPlanEditorScreenState.updateRounds(value: Int): TimedPlanEditorScreenState {
    return copy(rounds = value.coerceIn(1, 12), savedPlan = null, statusMessage = null)
}

internal fun TimedPlanEditorScreenState.updateRestBetweenRounds(seconds: Int): TimedPlanEditorScreenState {
    return copy(restBetweenRoundsSec = seconds.sanitizeDuration(), savedPlan = null, statusMessage = null)
}

internal fun TimedPlanEditorScreenState.updateActionCueThreshold(seconds: Int): TimedPlanEditorScreenState {
    return copy(
        actionCue = actionCue.copy(thresholdSec = seconds.sanitizeCueThreshold()),
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.updateRestCueThreshold(seconds: Int): TimedPlanEditorScreenState {
    return copy(
        restCue = restCue.copy(thresholdSec = seconds.sanitizeCueThreshold()),
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.updateActionCueEnabled(enabled: Boolean): TimedPlanEditorScreenState {
    return copy(actionCue = actionCue.copy(enabled = enabled), savedPlan = null, statusMessage = null)
}

internal fun TimedPlanEditorScreenState.updateRestCueEnabled(enabled: Boolean): TimedPlanEditorScreenState {
    return copy(restCue = restCue.copy(enabled = enabled), savedPlan = null, statusMessage = null)
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
            if (item.id == itemId) item.copy(workDurationSec = seconds.sanitizeDuration(min = 5)) else item
        },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.updateItemRestAfter(
    itemId: String,
    seconds: Int
): TimedPlanEditorScreenState {
    return copy(
        items = items.map { item ->
            if (item.id == itemId) item.copy(restAfterSec = seconds.sanitizeDuration()) else item
        },
        savedPlan = null,
        statusMessage = null
    )
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
    )
}

internal fun TimedPlanEditorScreenState.removeItem(itemId: String): TimedPlanEditorScreenState {
    if (items.size <= 1) return this

    return copy(
        items = items.filterNot { it.id == itemId },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun TimedPlanEditorScreenState.saveDraftPlan(
    timestamp: String = DefaultTimedPlanTimestamp
): TimedPlanEditorScreenState {
    if (!canSave) return copy(statusMessage = "请至少保留一个动作并填写计划名称。")

    return copy(
        savedPlan = toWorkoutPlan(timestamp = timestamp),
        statusMessage = "已生成本次计时计划草稿，后续 E2.4 接入真实保存。"
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

private fun Int.sanitizeCueThreshold(): Int = coerceIn(1, 60)
