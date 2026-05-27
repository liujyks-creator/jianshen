package com.liujyks.trainflow.core.domain.exerciselibrary

import com.liujyks.trainflow.core.model.EquipmentKind
import com.liujyks.trainflow.core.model.Exercise
import com.liujyks.trainflow.core.model.ExerciseDifficulty

data class ExerciseLibraryFilters(
    val trainingMode: ExerciseTrainingModeFilter = ExerciseTrainingModeFilter.ALL,
    val muscleId: String? = null,
    val equipment: EquipmentKind? = null,
    val difficulty: ExerciseDifficulty? = null
) {
    val hasActiveFilters: Boolean
        get() = trainingMode != ExerciseTrainingModeFilter.ALL ||
            muscleId != null ||
            equipment != null ||
            difficulty != null
}

enum class ExerciseTrainingModeFilter {
    ALL,
    TIMED,
    STRENGTH,
    FOLLOW_ALONG
}

fun filterExercises(
    exercises: List<Exercise>,
    filters: ExerciseLibraryFilters
): List<Exercise> = exercises.filter { exercise ->
    exercise.matchesTrainingMode(filters.trainingMode) &&
        exercise.matchesMuscle(filters.muscleId) &&
        exercise.matchesEquipment(filters.equipment) &&
        exercise.matchesDifficulty(filters.difficulty)
}

private fun Exercise.matchesTrainingMode(filter: ExerciseTrainingModeFilter): Boolean {
    return when (filter) {
        ExerciseTrainingModeFilter.ALL -> true
        ExerciseTrainingModeFilter.TIMED -> capabilities.supportsTimedTraining
        ExerciseTrainingModeFilter.STRENGTH -> capabilities.supportsReps || capabilities.supportsWeight
        ExerciseTrainingModeFilter.FOLLOW_ALONG -> capabilities.supportsFollowAlong
    }
}

private fun Exercise.matchesMuscle(muscleId: String?): Boolean {
    if (muscleId == null) return true

    return muscleId in primaryMuscleIds || muscleId in secondaryMuscleIds
}

private fun Exercise.matchesEquipment(equipment: EquipmentKind?): Boolean {
    if (equipment == null) return true

    return equipment in this.equipment
}

private fun Exercise.matchesDifficulty(difficulty: ExerciseDifficulty?): Boolean {
    if (difficulty == null) return true

    return this.difficulty == difficulty
}
