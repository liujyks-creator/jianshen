package com.liujyks.trainflow.feature.exerciselibrary

import com.liujyks.trainflow.core.data.fixture.ActionExerciseFixture
import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.domain.exerciselibrary.ExerciseLibraryFilters
import com.liujyks.trainflow.core.domain.exerciselibrary.ExerciseTrainingModeFilter
import com.liujyks.trainflow.core.domain.exerciselibrary.filterExercises
import com.liujyks.trainflow.core.model.EquipmentKind
import com.liujyks.trainflow.core.model.Exercise
import com.liujyks.trainflow.core.model.ExerciseDifficulty
import com.liujyks.trainflow.core.model.RepTarget

internal fun buildExerciseLibraryUiState(
    filters: ExerciseLibraryFilters,
    entries: List<ActionExerciseFixture> = FirstActionExerciseFixtures.entries
): ExerciseLibraryScreenState {
    val exercises = entries.map { it.exercise }
    val visibleExercises = filterExercises(exercises, filters)
    val entriesByExerciseId = entries.associateBy { it.exercise.id }

    return ExerciseLibraryScreenState(
        totalCount = exercises.size,
        visibleCount = visibleExercises.size,
        filters = filters,
        trainingModeOptions = buildTrainingModeOptions(filters),
        muscleOptions = buildMuscleOptions(exercises, filters),
        equipmentOptions = buildEquipmentOptions(exercises, filters),
        difficultyOptions = buildDifficultyOptions(exercises, filters),
        items = visibleExercises.map { exercise ->
            exercise.toUiState(entriesByExerciseId.getValue(exercise.id))
        }
    )
}

private fun buildTrainingModeOptions(
    filters: ExerciseLibraryFilters
): List<ExerciseFilterOption<ExerciseTrainingModeFilter>> {
    return ExerciseTrainingModeFilter.entries.map { mode ->
        ExerciseFilterOption(
            value = mode,
            label = mode.label(),
            selected = filters.trainingMode == mode
        )
    }
}

private fun buildMuscleOptions(
    exercises: List<Exercise>,
    filters: ExerciseLibraryFilters
): List<ExerciseFilterOption<String>> {
    return exercises
        .flatMap { it.primaryMuscleIds + it.secondaryMuscleIds }
        .distinct()
        .sortedBy { it.muscleLabel() }
        .map { muscleId ->
            ExerciseFilterOption(
                value = muscleId,
                label = muscleId.muscleLabel(),
                selected = filters.muscleId == muscleId
            )
        }
}

private fun buildEquipmentOptions(
    exercises: List<Exercise>,
    filters: ExerciseLibraryFilters
): List<ExerciseFilterOption<EquipmentKind>> {
    return exercises
        .flatMap { it.equipment }
        .distinct()
        .sortedBy { it.label() }
        .map { equipment ->
            ExerciseFilterOption(
                value = equipment,
                label = equipment.label(),
                selected = filters.equipment == equipment
            )
        }
}

private fun buildDifficultyOptions(
    exercises: List<Exercise>,
    filters: ExerciseLibraryFilters
): List<ExerciseFilterOption<ExerciseDifficulty>> {
    return ExerciseDifficulty.entries
        .filter { difficulty -> exercises.any { it.difficulty == difficulty } }
        .map { difficulty ->
            ExerciseFilterOption(
                value = difficulty,
                label = difficulty.label(),
                selected = filters.difficulty == difficulty
            )
        }
}

private fun Exercise.toUiState(entry: ActionExerciseFixture): ExerciseLibraryItemUiState {
    return ExerciseLibraryItemUiState(
        id = id,
        name = name,
        categoryLabel = category.categoryLabel(),
        difficultyLabel = difficulty.label(),
        muscleLabels = primaryMuscleIds.map { it.muscleLabel() },
        equipmentLabels = equipment.map { it.label() },
        capabilityLabels = capabilityLabels(),
        shortCue = instructions.shortCue,
        defaultSummary = entry.defaultSummary()
    )
}

private fun Exercise.capabilityLabels(): List<String> = buildList {
    if (capabilities.supportsTimedTraining) add("计时")
    if (capabilities.supportsReps) add("次数")
    if (capabilities.supportsWeight) add("重量")
    if (capabilities.supportsFollowAlong) add("跟练")
    if (capabilities.supportsWarmupRole) add("热身")
    if (capabilities.supportsStretchRole) add("拉伸")
}

private fun ActionExerciseFixture.defaultSummary(): String? {
    val timed = timedDefault?.let { default ->
        "计时 ${default.workDurationSec}秒 / 休息${default.restAfterSec}秒"
    }
    val strength = strengthDefault?.let { default ->
        "力量 ${default.sets}组 ${default.repTarget.label()} / 休息${default.restAfterSetSec}秒"
    }

    return listOfNotNull(timed, strength)
        .joinToString(" · ")
        .takeIf { it.isNotBlank() }
}

private fun RepTarget.label(): String {
    return when (this) {
        is RepTarget.Fixed -> "${reps}次"
        is RepTarget.Range -> "${minReps}-${maxReps}次"
    }
}

internal fun ExerciseTrainingModeFilter.label(): String {
    return when (this) {
        ExerciseTrainingModeFilter.ALL -> "全部"
        ExerciseTrainingModeFilter.TIMED -> "计时"
        ExerciseTrainingModeFilter.STRENGTH -> "力量"
        ExerciseTrainingModeFilter.FOLLOW_ALONG -> "跟练"
    }
}

internal fun EquipmentKind.label(): String {
    return when (this) {
        EquipmentKind.BODYWEIGHT -> "徒手"
        EquipmentKind.DUMBBELL -> "哑铃"
        EquipmentKind.BARBELL -> "杠铃"
        EquipmentKind.MACHINE -> "器械"
        EquipmentKind.CABLE -> "绳索"
        EquipmentKind.BAND -> "弹力带"
        EquipmentKind.KETTLEBELL -> "壶铃"
        EquipmentKind.MAT -> "垫上"
        EquipmentKind.OTHER -> "其他"
    }
}

internal fun ExerciseDifficulty.label(): String {
    return when (this) {
        ExerciseDifficulty.BEGINNER -> "新手"
        ExerciseDifficulty.INTERMEDIATE -> "进阶"
        ExerciseDifficulty.ADVANCED -> "高级"
    }
}

private fun String.categoryLabel(): String {
    return when (this) {
        "warmup" -> "热身"
        "bodyweight" -> "徒手"
        "strength" -> "力量"
        "stretch" -> "拉伸"
        else -> readableIdLabel()
    }
}

private fun String.muscleLabel(): String {
    return when (this) {
        "full_body" -> "全身"
        "calves" -> "小腿"
        "quads" -> "股四头肌"
        "glutes" -> "臀部"
        "hamstrings" -> "腘绳肌"
        "hip_flexors" -> "髋屈肌"
        "core" -> "核心"
        "chest" -> "胸部"
        "triceps" -> "肱三头肌"
        "shoulders" -> "肩部"
        "lats" -> "背阔肌"
        "upper_back" -> "上背"
        "biceps" -> "肱二头肌"
        else -> readableIdLabel()
    }
}

private fun String.readableIdLabel(): String {
    return split('_', '-')
        .filter { it.isNotBlank() }
        .joinToString(" ")
}
