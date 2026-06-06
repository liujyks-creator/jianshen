package com.liujyks.trainflow.feature.plans

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MvpAcceptanceChecklistEvidenceTest {
    @Test
    fun editorStartButtonsRemainDisabledInPlanEditorRoutes() {
        val timedRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt"
        )
        val strengthRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/StrengthPlanEditorRoute.kt"
        )

        assertTrue(timedRoute.contains("enabled = false"))
        assertTrue(timedRoute.contains("立即开始（E3 接入）"))
        assertTrue(strengthRoute.contains("enabled = false"))
        assertTrue(strengthRoute.contains("开始力量训练（E4 接入）"))
    }

    @Test
    fun integerNumberFieldParsingCannotRepresentBlankDraftInput() {
        val timedRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt"
        )
        val strengthRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/StrengthPlanEditorRoute.kt"
        )

        assertTrue(timedRoute.contains("value = value.toString()"))
        assertTrue(strengthRoute.contains("value = value.toString()"))
        assertFalse(currentIntegerNumberFieldWouldDispatchUpdate(""))
        assertFalse(currentIntegerNumberFieldWouldDispatchUpdate("abc"))
        assertTrue(currentIntegerNumberFieldWouldDispatchUpdate("0"))
        assertTrue(currentIntegerNumberFieldWouldDispatchUpdate("15"))
    }

    @Test
    fun decimalWeightInputAllowsClearingToEmptyDraft() {
        assertTrue("".sanitizeDecimalInput().isEmpty())
        assertTrue(null.formatWeightInput().isEmpty())
        assertFalse(currentIntegerNumberFieldWouldDispatchUpdate(""))
    }

    private fun currentIntegerNumberFieldWouldDispatchUpdate(input: String): Boolean {
        return input.filter { it.isDigit() }.toIntOrNull() != null
    }

    private fun sourceFile(path: String): String = File(path).readText()
}
