package com.liujyks.trainflow.core.domain.exerciselibrary

import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.model.EquipmentKind
import com.liujyks.trainflow.core.model.ExerciseDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseLibraryFilterTest {
    private val exercises = FirstActionExerciseFixtures.exercises

    @Test
    fun timedTrainingFilterOnlyReturnsTimedCapableExercises() {
        val filtered = filterExercises(
            exercises = exercises,
            filters = ExerciseLibraryFilters(trainingMode = ExerciseTrainingModeFilter.TIMED)
        )

        assertEquals(7, filtered.size)
        assertTrue(filtered.all { it.capabilities.supportsTimedTraining })
        assertEquals(
            listOf(
                "jumping-jacks",
                "bodyweight-squat",
                "incline-push-up",
                "forearm-plank",
                "alternating-reverse-lunge",
                "glute-bridge",
                "standing-quad-stretch"
            ),
            filtered.map { it.id }
        )
    }

    @Test
    fun strengthFilterSupportsRepsOrWeightForPlanEditingEntry() {
        val filtered = filterExercises(
            exercises = exercises,
            filters = ExerciseLibraryFilters(trainingMode = ExerciseTrainingModeFilter.STRENGTH)
        )

        assertEquals(8, filtered.size)
        assertTrue(filtered.all { it.capabilities.supportsReps || it.capabilities.supportsWeight })
        assertTrue(filtered.any { it.id == "bodyweight-squat" })
        assertTrue(filtered.any { it.id == "barbell-bench-press" })
    }

    @Test
    fun filtersCanBeCombinedAndCleared() {
        val filtered = filterExercises(
            exercises = exercises,
            filters = ExerciseLibraryFilters(
                trainingMode = ExerciseTrainingModeFilter.STRENGTH,
                muscleId = "glutes",
                equipment = EquipmentKind.DUMBBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE
            )
        )

        assertEquals(
            listOf("dumbbell-romanian-deadlift"),
            filtered.map { it.id }
        )

        val cleared = ExerciseLibraryFilters()
        assertEquals(exercises.map { it.id }, filterExercises(exercises, cleared).map { it.id })
    }
}
