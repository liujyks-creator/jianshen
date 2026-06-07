package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.data.fixture.ActionExerciseFixture
import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.data.fixture.WeightInputStrategy
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.ExerciseSide
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
    val strengthSetTimerMode: StrengthSetTimerMode = StrengthSetTimerMode.MANUAL_START,
    val savedPlan: WorkoutPlan? = null,
    val statusMessage: String? = null
) {
    val validationMessage: String?
        get() = validateStrengthDraft()

    val canSave: Boolean = validationMessage == null

    val canStartTraining: Boolean
        get() = canSave

    val totalSets: Int
        get() = exercises.sumOf { it.totalSets }

    val summary: String
        get() = "${exercises.size} 个动作 · ${totalSets} 组 · 组间休息 ${defaultRestSummary()}"

    fun toWorkoutPlan(
        planId: String = "plan-strength-draft",
        timestamp: String = DefaultStrengthPlanTimestamp
    ): WorkoutPlan {
        require(canSave) {
            validationMessage ?: "力量计划草稿仍有未通过校验的输入。"
        }
        return WorkoutPlan(
            id = planId,
            mode = WorkoutMode.STRENGTH,
            title = title.trim(),
            description = "内存态力量计划草稿",
            blocks = exercises.mapIndexed { index, exercise ->
                exercise.toStrengthExerciseBlock(
                    order = index + 1,
                    setTimerMode = strengthSetTimerMode
                )
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
    val requiresWeightInput: Boolean,
    val targetWeightKg: Double?,
    val targetWeightRawText: String? = null,
    val repTarget: StrengthRepTargetUiState,
    val workingSets: Int,
    val workingSetsRawText: String? = null,
    val warmupSets: Int,
    val warmupSetsRawText: String? = null,
    val restAfterSetSec: Int,
    val restAfterSetRawText: String? = null,
    val perSide: Boolean,
    val substitutions: List<String>,
    val expandedSetTargets: Boolean,
    val setTargets: List<StrengthSetTargetUiState>
) {
    val totalSets: Int
        get() = warmupSets + workingSets

    val targetWeightText: String
        get() = targetWeightRawText ?: targetWeightKg.formatWeightInput()

    val workingSetsText: String
        get() = workingSetsRawText ?: workingSets.toString()

    val warmupSetsText: String
        get() = warmupSetsRawText ?: warmupSets.toString()

    val restAfterSetText: String
        get() = restAfterSetRawText ?: restAfterSetSec.toString()

    val targetSummary: String
        get() {
            val weight = targetWeightKg?.takeIf { it > 0.0 }?.formatWeightKg() ?: "自重/待填重量"
            val sideSuffix = if (perSide) " · 每侧" else ""
            return "$weight · ${repTarget.summary} · ${workingSets} 个正式组$sideSuffix"
        }

    fun toStrengthExerciseBlock(
        order: Int,
        setTimerMode: StrengthSetTimerMode
    ): StrengthExerciseBlock {
        val plannedSide = if (perSide) ExerciseSide.ALTERNATING else null
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
                setTarget.toStrengthSetPlan(blockOrder = order, order = index + 1, side = plannedSide)
            },
            substitutions = substitutions,
            setTimerMode = setTimerMode
        )
    }
}

internal data class StrengthSetTargetUiState(
    val id: String,
    val order: Int,
    val kind: StrengthSetKind,
    val targetWeightKg: Double?,
    val targetWeightRawText: String? = null,
    val repTarget: StrengthRepTargetUiState,
    val restAfterSec: Int
) {
    val targetWeightText: String
        get() = targetWeightRawText ?: targetWeightKg.formatWeightInput()

    val label: String
        get() = when (kind) {
            StrengthSetKind.WARMUP -> "热身组 $order"
            StrengthSetKind.WORKING -> "正式组 $order"
            StrengthSetKind.DROP -> "递减组 $order"
            StrengthSetKind.BACKOFF -> "退阶组 $order"
        }

    fun toStrengthSetPlan(blockOrder: Int, order: Int, side: ExerciseSide?): StrengthSetPlan {
        return StrengthSetPlan(
            id = "strength-set-$blockOrder-$order",
            order = order,
            kind = kind,
            side = side,
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
    val minRepsRawText: String? = null,
    val maxReps: Int = RepTarget.Range.DEFAULT_MAX_REPS,
    val maxRepsRawText: String? = null,
    val fixedReps: Int = RepTarget.Range.DEFAULT_MAX_REPS,
    val fixedRepsRawText: String? = null
) {
    val minRepsText: String
        get() = minRepsRawText ?: minReps.toString()

    val maxRepsText: String
        get() = maxRepsRawText ?: maxReps.toString()

    val fixedRepsText: String
        get() = fixedRepsRawText ?: fixedReps.toString()

    val summary: String
        get() = when (kind) {
            StrengthRepTargetKind.RANGE -> "$minReps-$maxReps 次"
            StrengthRepTargetKind.FIXED -> "$fixedReps 次"
        }

    fun toRepTarget(): RepTarget {
        return when (kind) {
            StrengthRepTargetKind.RANGE -> RepTarget.Range(
                minReps = minReps,
                maxReps = maxReps
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
    entries: List<ActionExerciseFixture> = FirstActionExerciseFixtures.entries,
    defaults: PlanEditorDefaults = PlanEditorDefaults()
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
        selectableExercises = strengthEntries.map { entry -> entry.toStrengthOption() },
        strengthSetTimerMode = defaults.strengthSetTimerMode
    )
}

internal fun StrengthPlanEditorScreenState.updateTitle(value: String): StrengthPlanEditorScreenState {
    return copy(title = value, savedPlan = null, statusMessage = null)
}

internal fun StrengthPlanEditorScreenState.updateTargetWeight(
    exerciseItemId: String,
    kg: Double?
): StrengthPlanEditorScreenState {
    val sanitizedWeight = kg.sanitizeWeightOrNull()
    return updateExercise(exerciseItemId) { exercise ->
        exercise.copy(
            targetWeightKg = sanitizedWeight,
            targetWeightRawText = sanitizedWeight.formatWeightInput(),
            setTargets = exercise.setTargets.map { setTarget ->
                setTarget.copy(
                    targetWeightKg = sanitizedWeight,
                    targetWeightRawText = sanitizedWeight.formatWeightInput()
                )
            }
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateTargetWeightText(
    exerciseItemId: String,
    input: String
): StrengthPlanEditorScreenState {
    val cleaned = input.sanitizeDecimalInput()
    val parsedWeight = cleaned.toDoubleOrNull().sanitizeWeightOrNull()
    return updateExercise(exerciseItemId) { exercise ->
        exercise.copy(
            targetWeightKg = parsedWeight,
            targetWeightRawText = cleaned,
            setTargets = exercise.setTargets.map { setTarget ->
                setTarget.copy(
                    targetWeightKg = parsedWeight,
                    targetWeightRawText = cleaned
                )
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
        updated.copy(
            workingSetsRawText = updated.workingSets.toString(),
            setTargets = updated.defaultSetTargets()
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateWorkingSetsText(
    exerciseItemId: String,
    input: String
): StrengthPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeSetCount(min = 1)
    return updateExercise(exerciseItemId) { exercise ->
        val updated = exercise.copy(
            workingSets = parsed ?: exercise.workingSets,
            workingSetsRawText = cleaned
        )
        if (parsed != null) {
            updated.copy(setTargets = updated.defaultSetTargets())
        } else {
            updated
        }
    }
}

internal fun StrengthPlanEditorScreenState.updateWarmupSets(
    exerciseItemId: String,
    sets: Int
): StrengthPlanEditorScreenState {
    return updateExercise(exerciseItemId) { exercise ->
        val updated = exercise.copy(warmupSets = sets.sanitizeSetCount(min = 0, max = 4))
        updated.copy(
            warmupSetsRawText = updated.warmupSets.toString(),
            setTargets = updated.defaultSetTargets()
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateWarmupSetsText(
    exerciseItemId: String,
    input: String
): StrengthPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeSetCount(min = 0, max = 4)
    return updateExercise(exerciseItemId) { exercise ->
        val updated = exercise.copy(
            warmupSets = parsed ?: exercise.warmupSets,
            warmupSetsRawText = cleaned
        )
        if (parsed != null) {
            updated.copy(setTargets = updated.defaultSetTargets())
        } else {
            updated
        }
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
            restAfterSetRawText = rest.toString(),
            setTargets = exercise.setTargets.map { it.copy(restAfterSec = rest) }
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateRestAfterSetText(
    exerciseItemId: String,
    input: String
): StrengthPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeStrengthDuration()
    return updateExercise(exerciseItemId) { exercise ->
        exercise.copy(
            restAfterSetSec = parsed ?: exercise.restAfterSetSec,
            restAfterSetRawText = cleaned,
            setTargets = if (parsed != null) {
                exercise.setTargets.map { it.copy(restAfterSec = parsed) }
            } else {
                exercise.setTargets
            }
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
            minRepsRawText = sanitizedMin.toString(),
            maxReps = sanitizedMax,
            maxRepsRawText = sanitizedMax.toString()
        )
        exercise.copy(
            repTarget = reps,
            setTargets = exercise.setTargets.map { it.copy(repTarget = reps) }
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateRepRangeText(
    exerciseItemId: String,
    minRepsInput: String,
    maxRepsInput: String
): StrengthPlanEditorScreenState {
    val cleanedMin = minRepsInput.sanitizeIntegerInput()
    val cleanedMax = maxRepsInput.sanitizeIntegerInput()
    val parsedMin = cleanedMin.toIntOrNull()?.sanitizeReps()
    val parsedMax = cleanedMax.toIntOrNull()?.sanitizeReps()
    return updateExercise(exerciseItemId) { exercise ->
        val min = parsedMin ?: exercise.repTarget.minReps
        val max = parsedMax ?: exercise.repTarget.maxReps
        val reps = exercise.repTarget.copy(
            kind = StrengthRepTargetKind.RANGE,
            minReps = min,
            minRepsRawText = cleanedMin,
            maxReps = max,
            maxRepsRawText = cleanedMax
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
            fixedReps = reps.sanitizeReps(),
            fixedRepsRawText = reps.sanitizeReps().toString()
        )
        exercise.copy(
            repTarget = fixed,
            setTargets = exercise.setTargets.map { it.copy(repTarget = fixed) }
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateFixedRepsText(
    exerciseItemId: String,
    input: String
): StrengthPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeReps()
    return updateExercise(exerciseItemId) { exercise ->
        val fixed = exercise.repTarget.copy(
            kind = StrengthRepTargetKind.FIXED,
            fixedReps = parsed ?: exercise.repTarget.fixedReps,
            fixedRepsRawText = cleaned
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
                    val sanitizedWeight = kg.sanitizeWeightOrNull()
                    setTarget.copy(
                        targetWeightKg = sanitizedWeight,
                        targetWeightRawText = sanitizedWeight.formatWeightInput()
                    )
                } else {
                    setTarget
                }
            }
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateSetTargetWeightText(
    exerciseItemId: String,
    setId: String,
    input: String
): StrengthPlanEditorScreenState {
    val cleaned = input.sanitizeDecimalInput()
    val parsedWeight = cleaned.toDoubleOrNull().sanitizeWeightOrNull()
    return updateExercise(exerciseItemId) { exercise ->
        exercise.copy(
            expandedSetTargets = true,
            setTargets = exercise.setTargets.map { setTarget ->
                if (setTarget.id == setId) {
                    setTarget.copy(
                        targetWeightKg = parsedWeight,
                        targetWeightRawText = cleaned
                    )
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
                            fixedReps = reps.sanitizeReps(),
                            fixedRepsRawText = reps.sanitizeReps().toString()
                        )
                    )
                } else {
                    setTarget
                }
            }
        )
    }
}

internal fun StrengthPlanEditorScreenState.updateSetFixedRepsText(
    exerciseItemId: String,
    setId: String,
    input: String
): StrengthPlanEditorScreenState {
    val cleaned = input.sanitizeIntegerInput()
    val parsed = cleaned.toIntOrNull()?.sanitizeReps()
    return updateExercise(exerciseItemId) { exercise ->
        exercise.copy(
            expandedSetTargets = true,
            setTargets = exercise.setTargets.map { setTarget ->
                if (setTarget.id == setId) {
                    setTarget.copy(
                        repTarget = setTarget.repTarget.copy(
                            kind = StrengthRepTargetKind.FIXED,
                            fixedReps = parsed ?: setTarget.repTarget.fixedReps,
                            fixedRepsRawText = cleaned
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
    if (!canSave) {
        return copy(statusMessage = validationMessage ?: "请至少保留一个动作、一个计划组，并填写计划名称。")
    }

    return copy(
        savedPlan = toWorkoutPlan(timestamp = timestamp),
        statusMessage = "已生成本次力量计划草稿，可用于当前内存态计划预览；真实保存后续接入。"
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
        requiresWeightInput = exercise.capabilities.supportsWeight,
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
            targetWeightRawText = targetWeightKg?.times(0.5)?.takeIf { it > 0.0 }.formatWeightInput(),
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
            targetWeightRawText = targetWeightKg.formatWeightInput(),
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

private fun StrengthPlanEditorScreenState.validateStrengthDraft(): String? {
    if (title.isBlank()) return "请填写计划名称。"
    if (exercises.isEmpty()) return "请至少保留一个力量动作。"
    exercises.forEach { exercise ->
        validateWeightText(
            label = "${exercise.exerciseName} 计划重量",
            text = exercise.targetWeightText,
            required = exercise.requiresWeightInput
        )?.let { return it }
        validateRepTarget(exercise.exerciseName, exercise.repTarget)?.let { return it }
        validateIntegerText("${exercise.exerciseName} 正式组数", exercise.workingSetsText, min = 1, max = 12)
            ?.let { return it }
        validateIntegerText("${exercise.exerciseName} 热身组数", exercise.warmupSetsText, min = 0, max = 4)
            ?.let { return it }
        validateIntegerText("${exercise.exerciseName} 组间休息秒数", exercise.restAfterSetText, min = 0, max = 3600)
            ?.let { return it }
        if (exercise.expandedSetTargets) {
            exercise.setTargets.forEach { setTarget ->
                validateWeightText(
                    label = "${exercise.exerciseName} ${setTarget.label} 重量",
                    text = setTarget.targetWeightText,
                    required = exercise.requiresWeightInput
                )?.let { return it }
                validateRepTarget(
                    exerciseName = "${exercise.exerciseName} ${setTarget.label}",
                    repTarget = setTarget.repTarget
                )?.let { return it }
            }
        }
    }
    return null
}

private fun validateRepTarget(
    exerciseName: String,
    repTarget: StrengthRepTargetUiState
): String? {
    return when (repTarget.kind) {
        StrengthRepTargetKind.RANGE -> {
            val minText = repTarget.minRepsText
            val maxText = repTarget.maxRepsText
            val parsedMin = minText.toIntOrNull()
            val parsedMax = maxText.toIntOrNull()
            validateIntegerText("$exerciseName 最少次数", minText, min = 1, max = 200)
                ?: validateIntegerText("$exerciseName 最大次数", maxText, min = 1, max = 200)
                ?: if (parsedMin != null && parsedMax != null && parsedMax < parsedMin) {
                    "$exerciseName 最大次数不能小于最小次数。"
                } else {
                    null
                }
        }

        StrengthRepTargetKind.FIXED -> {
            validateIntegerText("$exerciseName 固定次数", repTarget.fixedRepsText, min = 1, max = 200)
        }
    }
}

private fun validateWeightText(
    label: String,
    text: String,
    required: Boolean
): String? {
    if (text.isBlank()) return if (required) "$label 不能为空。" else null
    val parsed = text.toDoubleOrNull() ?: return "$label 请输入有效数字。"
    return if (parsed > 0.0 && parsed <= 1000.0) null else "$label 请输入 0-1000kg 之间的正数。"
}
