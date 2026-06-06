package com.liujyks.trainflow.core.domain.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicRecoveryRecommendationGeneratorTest {
    @Test
    fun timedExerciseIdsMapToStableDedupedLowerBodyRecommendations() {
        val recommendation = BasicRecoveryRecommendationGenerator.fromExerciseIds(
            sessionId = "timed-session",
            exerciseIds = listOf(
                "bodyweight-squat",
                "jumping-jacks",
                "bodyweight-squat",
                "glute-bridge"
            )
        )

        assertTrue(recommendation.hasRecommendation)
        assertEquals(
            listOf("lower-body-release", "posterior-chain-release"),
            recommendation.recommendation.areaIds
        )
        assertEquals(
            listOf("quads", "glutes", "full_body", "calves", "hamstrings"),
            recommendation.recommendation.trainedMuscleIds
        )
        assertEquals(
            listOf("徒手深蹲", "开合跳", "臀桥"),
            recommendation.sourceExerciseNames
        )
    }

    @Test
    fun strengthExerciseIdsMapChestShoulderAndUpperBackRecommendations() {
        val recommendation = BasicRecoveryRecommendationGenerator.fromExerciseIds(
            sessionId = "strength-session",
            exerciseIds = listOf("barbell-bench-press", "one-arm-dumbbell-row")
        )

        assertEquals(
            listOf("chest-shoulder-release", "upper-back-release"),
            recommendation.recommendation.areaIds
        )
        assertEquals(
            listOf("chest", "triceps", "lats", "upper_back"),
            recommendation.recommendation.trainedMuscleIds
        )
    }

    @Test
    fun unrecognizedExerciseIdsReturnHonestEmptyRecommendation() {
        val recommendation = BasicRecoveryRecommendationGenerator.fromExerciseIds(
            sessionId = "empty-session",
            exerciseIds = listOf("unknown-action")
        )

        assertFalse(recommendation.hasRecommendation)
        assertEquals(emptyList<String>(), recommendation.recommendation.areaIds)
        assertEquals(emptyList<String>(), recommendation.recommendation.trainedMuscleIds)
        assertEquals(emptyList<String>(), recommendation.sourceExerciseNames)
    }

    @Test
    fun boundaryTextStaysNonMedicalAndDoesNotMentionHeartRateOrCalories() {
        val recommendation = BasicRecoveryRecommendationGenerator.fromExerciseIds(
            sessionId = "boundary-session",
            exerciseIds = listOf("barbell-bench-press")
        )

        assertTrue(recommendation.nonMedicalBoundaryText.contains("基础放松映射"))
        assertTrue(recommendation.nonMedicalBoundaryText.contains("不是医疗诊断"))
        assertTrue(recommendation.nonMedicalBoundaryText.contains("康复治疗"))
        assertFalse(recommendation.nonMedicalBoundaryText.contains("心率"))
        assertFalse(recommendation.nonMedicalBoundaryText.contains("热量"))
        assertFalse(recommendation.nonMedicalBoundaryText.contains("疾病"))
    }
}
