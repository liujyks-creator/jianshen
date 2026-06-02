package com.liujyks.trainflow.core.domain.recovery

import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.data.fixture.RecoveryAreaFixtures
import com.liujyks.trainflow.core.model.Exercise
import com.liujyks.trainflow.core.model.RecoveryArea
import com.liujyks.trainflow.core.model.RecoveryRecommendation

data class BasicRecoveryRecommendation(
    val sessionId: String,
    val sourceExerciseIds: List<String>,
    val sourceExerciseNames: List<String>,
    val recommendation: RecoveryRecommendation,
    val recoveryAreas: List<RecoveryArea>,
    val nonMedicalBoundaryText: String = NON_MEDICAL_BOUNDARY_TEXT
) {
    val hasRecommendation: Boolean
        get() = recoveryAreas.isNotEmpty()
}

object BasicRecoveryRecommendationGenerator {
    fun fromExerciseIds(
        sessionId: String,
        exerciseIds: List<String>,
        exercises: List<Exercise> = FirstActionExerciseFixtures.exercises,
        recoveryAreas: List<RecoveryArea> = RecoveryAreaFixtures.areas
    ): BasicRecoveryRecommendation {
        val exerciseById = exercises.associateBy { exercise -> exercise.id }
        val areaById = recoveryAreas.associateBy { area -> area.id }
        val mappedExercises = exerciseIds
            .orderedDistinct()
            .mapNotNull { exerciseId -> exerciseById[exerciseId] }
            .filter { exercise -> exercise.recovery != null }
        val trainedMuscleIds = mappedExercises
            .flatMap { exercise -> requireNotNull(exercise.recovery).trainedMuscleIds }
            .orderedDistinct()
        val resolvedAreas = mappedExercises
            .flatMap { exercise -> requireNotNull(exercise.recovery).recommendedRecoveryAreaIds }
            .orderedDistinct()
            .mapNotNull { areaId -> areaById[areaId] }
        val contentIds = mappedExercises
            .flatMap { exercise -> requireNotNull(exercise.recovery).recoveryContentIds }
            .orderedDistinct()

        return BasicRecoveryRecommendation(
            sessionId = sessionId,
            sourceExerciseIds = mappedExercises.map { exercise -> exercise.id },
            sourceExerciseNames = mappedExercises.map { exercise -> exercise.name },
            recommendation = RecoveryRecommendation(
                sessionId = sessionId,
                trainedMuscleIds = trainedMuscleIds,
                areaIds = resolvedAreas.map { area -> area.id },
                contentIds = contentIds
            ),
            recoveryAreas = resolvedAreas
        )
    }
}

const val NON_MEDICAL_BOUNDARY_TEXT: String = "本建议仅提供训练后放松方向，不做康复治疗或医疗诊断。"

private fun <T> List<T>.orderedDistinct(): List<T> {
    val seen = LinkedHashSet<T>()
    return filter { item -> seen.add(item) }
}
