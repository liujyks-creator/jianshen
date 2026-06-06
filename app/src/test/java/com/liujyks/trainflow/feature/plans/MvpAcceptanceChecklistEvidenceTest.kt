package com.liujyks.trainflow.feature.plans

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MvpAcceptanceChecklistEvidenceTest {
    @Test
    fun editorStartButtonsAreConnectedInPlanEditorRoutes() {
        val timedRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt"
        )
        val strengthRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/StrengthPlanEditorRoute.kt"
        )

        assertTrue(timedRoute.contains("onStartTimedPlan"))
        assertTrue(timedRoute.contains("enabled = uiState.canStartTraining"))
        assertTrue(timedRoute.contains("立即开始"))
        assertFalse(timedRoute.contains("立即开始（E3 接入）"))
        assertTrue(strengthRoute.contains("onStartStrengthPlan"))
        assertTrue(strengthRoute.contains("enabled = uiState.canStartTraining"))
        assertTrue(strengthRoute.contains("开始力量训练"))
        assertFalse(strengthRoute.contains("开始力量训练（E4 接入）"))
    }

    @Test
    fun integerNumberFieldParsingCanRepresentBlankDraftInput() {
        val timedRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt"
        )
        val strengthRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/StrengthPlanEditorRoute.kt"
        )

        assertTrue(timedRoute.contains("value = value"))
        assertTrue(strengthRoute.contains("value = value"))
        assertTrue("".sanitizeIntegerInput().isEmpty())
        assertTrue("abc".sanitizeIntegerInput().isEmpty())
        assertEquals("0", "0".sanitizeIntegerInput())
        assertEquals("15", "15".sanitizeIntegerInput())
        assertFalse(buildDefaultTimedPlanEditorState().updateRoundsText("").canSave)
        assertFalse(buildDefaultStrengthPlanEditorState()
            .updateWorkingSetsText(buildDefaultStrengthPlanEditorState().exercises.first().id, "")
            .canSave)
    }

    @Test
    fun decimalWeightInputAllowsClearingToEmptyDraft() {
        assertTrue("".sanitizeDecimalInput().isEmpty())
        assertTrue(null.formatWeightInput().isEmpty())
    }

    private fun sourceFile(path: String): String = File(path).readText()
}
