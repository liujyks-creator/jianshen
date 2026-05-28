package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.data.fixture.ActionExerciseFixture
import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.data.fixture.WeightInputStrategy
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthExerciseTarget
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import kotlin.math.roundToInt

internal const val DefaultStrengthPlanTimestamp = "2026-05-28T00:00:00Z"

internal data class StrengthPlanEditorScreenState(
    val title: String,
    val exercises: List<StrengthPlanExerciseUiState>,
    val selectableExercises: List<StrengthExerciseOptionUiState>,
    val savedPlan: WorkoutPlan? = null,
    val statusMessage: String? = null
) {
    val canSave: Boolean = title.isNotBlank() && exercises.isNotEmpty() && exercises.all { it.totalSets > 0 }

    val totalSets: Int
        get() = exercises.sumOf { it.totalSets }

    val summary: String
        get() = "${exercises.size} 个动作 · ${totalSets} 组 · 组间休息 ${defaultRestSummary()}"

    fun toWorkoutPlan(
        planId: String = "plan-strength-draft",
        timestamp: String = DefaultStrengthPlanTimestamp
    ): WorkoutPlan {
        return WorkoutPlan(
            id = planId,
            mode = WorkoutMode.STRENGTH,
            title = title.trim(),
            description = "E2.3 内存态力量计划草稿",
            blocks = exercises.mapIndexed { index, exercise ->
                exercise.toStrengthExerciseBlock(order = index + 1)
            },
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }

    private fun defaultRestSummary(): String {
        val distinctRest = exercises.map { it.restAfterSetSec }.distinct()
        return if (distinctRest.size == 1) {
            "${distinctRest.single()}秒"
        } else {
            "按动作设置"
        }
    }
}

internal data class StrengthPlanExerciseUiState(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val shortCue: String,
    val targetWeightKg: Double?,
    val repTarget: StrengthRepTargetUiState,
    val workingSets: Int,
    val warmupSets: Int,
    val restAfterSetSec: Int,
    val perSide: Boolean,
    val substitutions: List<String>,
    val expandedSetTargets: Boolean,
    val setTargets: List<StrengthSetTargetUiState>
) {
    val totalSets: Int
        get() = warmupSets + workingSets

    val targetSummary: String
        get() {
            val weight = targetWeightKg?.takeIf { it > 0.0 }?.formatWeightKg() ?: "自重/待填重量"
            val sideSuffix = if (perSide) " · 每侧" else ""
            return "$weight · ${repTarget.summary} · ${workingSets} 个正式组$sideSuffix"
        }

    fun toStrengthExerciseBlock(order: Int): StrengthExerciseBlock {
        return StrengthExerciseBlock(
            id = "strength-block-$order-$exerciseId",
            order = order,
            title = exerciseName,
            exerciseId = exerciseId,
            target = StrengthExerciseTarget(
                weight = targetWeightKg.toWeightValueOrNull(),
                repTarget = repTarget.toRepTarget(),
                restAfterSetSec = restAfterSetSec.takeIf { it > 0 }
            ),
            sets = setTargets.mapIndexed { index, setTarget ->
                setTarget.toStrengthSetPlan(blockOrder = order, order = index + 1)
            },
            substitutions = substitutions,
            setTimerMode = StrengthSetTimerMode.MANUAL_START
        )
    }
}

internal data class StrengthSetTargetUiState(
    val id: String,
    val order: Int,
    val kind: StrengthSetKind,
    val targetWeightKg: Double?,
    val repTarget: StrengthRepTargetUiState,
    val restAfterSec: Int
) {
    val label: String
        get() = when (kind) {
            StrengthSetKind.WARMUP -> "热身组 $order"
            StrengthSetKind.WORKING -> "正式组 $order"
            StrengthSetKind.DROP -> "递减组 $order"
            StrengthSetKind.BACKOFF -> "退阶组 $order"
        }

    fun toStrengthSetPlan(blockOrder: Int, order: Int): StrengthSetPlan {
        return StrengthSetPlan(
            id = "strength-set-$blockOrder-$order",
            order = order,
            kind = kind,
            targetWeight = targetWeightKg.toWeightValueOrNull(),
            repTarget = repTarget.toRepTarget(),
            restAfterSec = restAfterSec.takeIf { it > 0 }
        )
    }
}

internal data class StrengthExerciseOptionUiState(
    val exerciseId: String,
    val exerciseName: String,
    val defaultSummary: String
)

internal data class StrengthRepTargetUiState(
    val kind: StrengthRepTargetKind = StrengthRepTargetKind.RANGE,
    val minReps: Int = RepTarget.Range.DEFAULT_MIN_REPS,
    val maxReps: Int = RepTarget.Range.DEFAULT_MAX_REPS,
    val fixedReps: Int = RepTarget.Range.DEFAULT_MAX_REPS
) {
    val summary: String
        get() = when (kind) {
            StrengthRepTargetKind.RANGE -> "$minReps-$maxReps 次"
            StrengthRepTargetKind.FIXED -> "$fixedReps 次"
        }

    fun toRepTarget(): RepTarget {
        return when (kind) {
            StrengthRepTargetKind.RANGE -> RepTarget.Range(
                minReps = minReps,
                maxReps = maxReps.coerceAtLeast(minReps)
            )

            StrengthRepTargetKind.FIXED -> RepTarget.Fixed(reps = fixedReps)
        }
    }
}

internal enum class StrengthRepTargetKind {
    RANGE,
    FIXED
}

internal fun buildDefaultStrengthPlanEditorState(
    entries: List<ActionExerciseFixture> = FirstActionExerciseFixtures.entries
): StrengthPlanEditorScreenState {
    val strengthEntries = entries.strengthCapableEntries()
    val defaultEntries = buildList {
        strengthEntries.firstOrNull {
            it.exercise.capabilities.supportsWeight && it.onboardingSuitable
        }?.let(::add)
        strengthEntries.firstOrNull {
            !it.exercise.capabilities.supportsWeight && it.onboardingSuitable
        }?.let(::add)
    }.ifEmpty {
        strengthEntries.take(2)
    }

    return StrengthPlanEditorScreenState(
        title = "基础力量计划",
        exercises = defaultEntries.mapIndexed { index, entry -> entry.toStrengthEditorExercise(index + 1) },
        selectableExercises = strengthEntries.map { entry -> entry.toStrengthOption() }
    )
}

internal fun StrengthPlanEditorScreenState.updateTitle(value: String): StrengthPlanEditorScreenState {
    return copy(title = value, savedPlan = null, statusMessage = null)
}

internal fun StrengthPlanEditorScreenState.updateTargetWeight(
    exerciseItemId: String,
    kg: Double?
): StrengthPlanEditorScreenState {
    return updateExercise(exerciseItemId) { exercise ->
        exercise.copy(
            targetWeightKg = kg.sanitizeWeightOrNull(),
            setTargets = exercise.setTargets.map { setTarget ->
                setTarget.copy(targetWeightKg = kg.sanitizeWeightOrNull())
            }
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateWorkingSets(
    exerciseItemId: String,
    sets: Int
): StrengthPlanEditorScreenState {
    return updateExercise(exerciseItemId) { exercise ->
        val updated = exercise.copy(workingSets = sets.sanitizeSetCount(min = 1))
        updated.copy(setTargets = updated.defaultSetTargets())
    }
}

internal fun StrengthPlanEditorScreenState.updateWarmupSets(
    exerciseItemId: String,
    sets: Int
): StrengthPlanEditorScreenState {
    return updateExercise(exerciseItemId) { exercise ->
        val updated = exercise.copy(warmupSets = sets.sanitizeSetCount(min = 0, max = 4))
        updated.copy(setTargets = updated.defaultSetTargets())
    }
}

internal fun StrengthPlanEditorScreenState.updateRestAfterSet(
    exerciseItemId: String,
    seconds: Int
): StrengthPlanEditorScreenState {
    return updateExercise(exerciseItemId) { exercise ->
        val rest = seconds.sanitizeStrengthDuration()
        exercise.copy(
            restAfterSetSec = rest,
            setTargets = exercise.setTargets.map { it.copy(restAfterSec = rest) }
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateRepRange(
    exerciseItemId: String,
    minReps: Int,
    maxReps: Int
): StrengthPlanEditorScreenState {
    val sanitizedMin = minReps.sanitizeReps()
    val sanitizedMax = maxReps.sanitizeReps().coerceAtLeast(sanitizedMin)
    return updateExercise(exerciseItemId) { exercise ->
        val reps = exercise.repTarget.copy(
            kind = StrengthRepTargetKind.RANGE,
            minReps = sanitizedMin,
            maxReps = sanitizedMax
        )
        exercise.copy(
            repTarget = reps,
            setTargets = exercise.setTargets.map { it.copy(repTarget = reps) }
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateFixedReps(
    exerciseItemId: String,
    reps: Int
): StrengthPlanEditorScreenState {
    return updateExercise(exerciseItemId) { exercise ->
        val fixed = exercise.repTarget.copy(
            kind = StrengthRepTargetKind.FIXED,
            fixedReps = reps.sanitizeReps()
        )
        exercise.copy(
            repTarget = fixed,
            setTargets = exercise.setTargets.map { it.copy(repTarget = fixed) }
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateSetTargetWeight(
    exerciseItemId: String,
    setId: String,
    kg: Double?
): StrengthPlanEditorScreenState {
    return updateExercise(exerciseItemId) { exercise ->
        exercise.copy(
            expandedSetTargets = true,
            setTargets = exercise.setTargets.map { setTarget ->
                if (setTarget.id == setId) {
                    setTarget.copy(targetWeightKg = kg.sanitizeWeightOrNull())
                } else {
                    setTarget
                }
            }
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateSetFixedReps(
    exerciseItemId: String,
    setId: String,
    reps: Int
): StrengthPlanEditorScreenState {
    return updateExercise(exerciseItemId) { exercise ->
        exercise.copy(
            expandedSetTargets = true,
            setTargets = exercise.setTargets.map { setTarget ->
                if (setTarget.id == setId) {
                    setTarget.copy(
                        repTarget = setTarget.repTarget.copy(
                            kind = StrengthRepTargetKind.FIXED,
                            fixedReps = reps.sanitizeReps()
                        )
                    )
                } else {
                    setTarget
                }
            }
        )
    }
}

internal fun StrengthPlanEditorScreenState.setSetTargetsExpanded(
    exerciseItemId: String,
    expanded: Boolean
): StrengthPlanEditorScreenState {
    return updateExercise(exerciseItemId) { exercise ->
        exercise.copy(expandedSetTargets = expanded)
    }
}

internal fun StrengthPlanEditorScreenState.addExercise(exerciseId: String): StrengthPlanEditorScreenState {
    if (exercises.any { it.exerciseId == exerciseId }) return this

    val entry = FirstActionExerciseFixtures.entries
        .strengthCapableEntries()
        .firstOrNull { it.exercise.id == exerciseId }
        ?: return this

    return copy(
        exercises = exercises + entry.toStrengthEditorExercise(exercises.size + 1),
        savedPlan = null,
        statusMessage = null
    )
}

internal fun StrengthPlanEditorScreenState.removeExercise(exerciseItemId: String): StrengthPlanEditorScreenState {
    if (exercises.size <= 1) return this

    return copy(
        exercises = exercises.filterNot { it.id == exerciseItemId },
        savedPlan = null,
        statusMessage = null
    )
}

internal fun StrengthPlanEditorScreenState.saveDraftPlan(
    timestamp: String = DefaultStrengthPlanTimestamp
): StrengthPlanEditorScreenState {
    if (!canSave) return copy(statusMessage = "请至少保留一个动作、一个计划组，并填写计划名称。")

    return copy(
        savedPlan = toWorkoutPlan(timestamp = timestamp),
        statusMessage = "已生成本次力量计划草稿，后续 E2.4 接入真实保存。"
    )
}

private fun StrengthPlanEditorScreenState.updateExercise(
    exerciseItemId: String,
    transform: (StrengthPlanExerciseUiState) -> StrengthPlanExerciseUiState
): StrengthPlanEditorScreenState {
    return copy(
        exercises = exercises.map { exercise ->
            if (exercise.id == exerciseItemId) transform(exercise) else exercise
        },
        savedPlan = null,
        statusMessage = null
    )
}

private fun List<ActionExerciseFixture>.strengthCapableEntries(): List<ActionExerciseFixture> {
    return filter { entry ->
        entry.strengthDefault != null &&
            (entry.exercise.capabilities.supportsReps || entry.exercise.capabilities.supportsWeight)
    }
}

private fun ActionExerciseFixture.toStrengthEditorExercise(order: Int): StrengthPlanExerciseUiState {
    val default = requireNotNull(strengthDefault) {
        "Strength editor can only consume strength fixture defaults."
    }
    val targetWeightKg = if (default.weightStrategy == WeightInputStrategy.USER_ENTERED) 20.0 else null
    val repTarget = StrengthRepTargetUiState()
    val substitutions = exercise.substitutions.map { it.exerciseId }
    val base = StrengthPlanExerciseUiState(
        id = "strength-editor-item-$order-${exercise.id}",
        exerciseId = exercise.id,
        exerciseName = exercise.name,
        shortCue = exercise.instructions.shortCue,
        targetWeightKg = targetWeightKg,
        repTarget = repTarget,
        workingSets = default.sets,
        warmupSets = default.warmupSets,
        restAfterSetSec = default.restAfterSetSec,
        perSide = default.perSide,
        substitutions = substitutions,
        expandedSetTargets = false,
        setTargets = emptyList()
    )
    return base.copy(setTargets = base.defaultSetTargets())
}

private fun ActionExerciseFixture.toStrengthOption(): StrengthExerciseOptionUiState {
    val default = requireNotNull(strengthDefault) {
        "Strength option can only consume strength fixture defaults."
    }
    val sideSuffix = if (default.perSide) " · 每侧" else ""
    return StrengthExerciseOptionUiState(
        exerciseId = exercise.id,
        exerciseName = exercise.name,
        defaultSummary = "${default.sets}组 · 8-12次 · 休息${default.restAfterSetSec}秒$sideSuffix"
    )
}

private fun StrengthPlanExerciseUiState.defaultSetTargets(): List<StrengthSetTargetUiState> {
    val warmupTargets = (1..warmupSets).map { order ->
        StrengthSetTargetUiState(
            id = "$id-warmup-$order",
            order = order,
            kind = StrengthSetKind.WARMUP,
            targetWeightKg = targetWeightKg?.times(0.5)?.takeIf { it > 0.0 },
            repTarget = StrengthRepTargetUiState(kind = StrengthRepTargetKind.FIXED, fixedReps = 10),
            restAfterSec = restAfterSetSec.coerceAtMost(60)
        )
    }
    val workingTargets = (1..workingSets).map { order ->
        StrengthSetTargetUiState(
            id = "$id-working-$order",
            order = order,
            kind = StrengthSetKind.WORKING,
            targetWeightKg = targetWeightKg,
            repTarget = repTarget,
            restAfterSec = restAfterSetSec
        )
    }
    return warmupTargets + workingTargets
}

private fun Double?.toWeightValueOrNull(): WeightValue? {
    val value = this?.takeIf { it > 0.0 } ?: return null
    return WeightValue(value = value, unit = WeightUnit.KG)
}

internal fun Double.formatWeightKg(): String {
    val tenths = (this * 10.0).roundToInt()
    val formatted = if (tenths % 10 == 0) {
        (tenths / 10).toString()
    } else {
        "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
    }
    return "${formatted}kg"
}

private fun Double?.sanitizeWeightOrNull(): Double? {
    return this?.coerceIn(0.0, 1000.0)?.takeIf { it > 0.0 }
}

private fun Int.sanitizeSetCount(min: Int, max: Int = 12): Int = coerceIn(min, max)

private fun Int.sanitizeStrengthDuration(): Int = coerceIn(0, 3600)

private fun Int.sanitizeReps(): Int = coerceIn(1, 200)
