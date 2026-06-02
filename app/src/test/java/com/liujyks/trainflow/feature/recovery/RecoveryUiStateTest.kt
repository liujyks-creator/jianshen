package com.liujyks.trainflow.feature.recovery

import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendationGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryUiStateTest {
    @Test
    fun mappedRecommendationBuildsReadableRecoveryScreenState() {
        val uiState = BasicRecoveryRecommendationGenerator.fromExerciseIds(
            sessionId = "session-strength",
            exerciseIds = listOf("barbell-bench-press", "one-arm-dumbbell-row")
        ).toRecoveryScreenState()

        assertFalse(uiState.isEmpty)
        assertTrue(uiState.trainedMuscleSummary.contains("胸部"))
        assertTrue(uiState.trainedMuscleSummary.contains("上背"))
        assertTrue(uiState.sourceExerciseSummary.contains("杠铃卧推"))
        assertEquals(
            listOf("胸肩前侧放松", "上背放松"),
            uiState.areaItems.map { item -> item.name }
        )
        assertTrue(uiState.nonMedicalNotice.contains("不做康复治疗或医疗诊断"))
    }

    @Test
    fun emptyRecommendationBuildsHonestEmptyState() {
        val uiState = BasicRecoveryRecommendationGenerator.fromExerciseIds(
            sessionId = "empty-session",
            exerciseIds = emptyList()
        ).toRecoveryScreenState()

        assertTrue(uiState.isEmpty)
        assertEquals("本次未识别到训练部位", uiState.trainedMuscleSummary)
        assertEquals("暂无可生成的恢复建议", uiState.emptyTitle)
        assertFalse(uiState.emptyDescription.contains("E5.4"))
    }

    @Test
    fun recoveryCopyDoesNotIntroduceUnavailableOrMedicalClaims() {
        val uiState = BasicRecoveryRecommendationGenerator.fromExerciseIds(
            sessionId = "session-timed",
            exerciseIds = listOf("bodyweight-squat")
        ).toRecoveryScreenState()
        val combinedCopy = buildString {
            append(uiState.sourceNote)
            append(uiState.nonMedicalNotice)
            append(uiState.areaItems.joinToString { item -> item.summary + item.cautionText })
        }

        assertFalse(combinedCopy.contains("AI"))
        assertFalse(combinedCopy.contains("心率判断"))
        assertFalse(combinedCopy.contains("热量判断"))
        assertFalse(combinedCopy.contains("疾病适应性"))
        assertFalse(combinedCopy.contains("自动训练建议"))
    }
}
