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
        val stickyActions = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/PlanEditorStickyActions.kt"
        )

        assertTrue(timedRoute.contains("onStartTimedPlan"))
        assertTrue(timedRoute.contains("startEnabled = uiState.canStartTraining"))
        assertTrue(stickyActions.contains("开始训练"))
        assertFalse(timedRoute.contains("立即开始（E3 接入）"))
        assertTrue(strengthRoute.contains("onStartStrengthPlan"))
        assertTrue(strengthRoute.contains("startEnabled = uiState.canStartTraining"))
        assertTrue(stickyActions.contains("开始训练"))
        assertFalse(strengthRoute.contains("开始力量训练（E4 接入）"))
    }

    @Test
    fun editorHeaderCopyMatchesCurrentStartAndSaveState() {
        val timedRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt"
        )
        val strengthRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/StrengthPlanEditorRoute.kt"
        )

        assertTrue(timedRoute.contains("计时训练不再选择动作库动作"))
        assertTrue(strengthRoute.contains("可直接开始当前草稿，也可保存为本地计划后从计划详情再次启动"))
        assertTrue(timedRoute.contains("长按阶段卡右侧"))
        assertTrue(timedRoute.contains("上移 / 下移保留为备用排序路径"))
        assertTrue(timedRoute.contains("onSaveTimedPlan"))
        assertTrue(strengthRoute.contains("onSaveStrengthPlan"))
        val stickyActions = sourceFile("src/main/java/com/liujyks/trainflow/feature/plans/PlanEditorStickyActions.kt")
        assertTrue(stickyActions.contains("保存计划"))
        assertTrue(stickyActions.contains("PlanEditorStickyActionReserveHeight"))
        assertTrue(stickyActions.contains("containerColor = TrainFlowPrimary"))
        assertFalse(timedRoute.contains("真实保存后续接入"))
        assertFalse(strengthRoute.contains("真实计划保存后续接入"))
        assertFalse(timedRoute.contains("训练执行引擎、真实保存和记录闭环留给后续 story"))
        assertFalse(strengthRoute.contains("计划详情已可启动力量训练，真实保存和 session records 后续接入"))
    }

    @Test
    fun planPreviewCardsDoNotDuplicateStickySaveAndStartActions() {
        val timedPreview = functionBody(
            sourceFile("src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt"),
            "private fun SaveAndPreviewCard"
        )
        val strengthPreview = functionBody(
            sourceFile("src/main/java/com/liujyks/trainflow/feature/plans/StrengthPlanEditorRoute.kt"),
            "private fun StrengthSaveAndPreviewCard"
        )
        val stickyActions = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/PlanEditorStickyActions.kt"
        )

        assertFalse(timedPreview.contains("Text(text = \"保存计划\")"))
        assertFalse(timedPreview.contains("Text(text = \"开始训练\")"))
        assertFalse(strengthPreview.contains("Text(text = \"保存计划\")"))
        assertFalse(strengthPreview.contains("Text(text = \"开始训练\")"))
        assertTrue(stickyActions.contains("Text(text = \"保存计划\")"))
        assertTrue(stickyActions.contains("Text(text = \"开始训练\")"))
        assertTrue(timedPreview.contains("预览卡仅用于确认计划摘要"))
        assertTrue(strengthPreview.contains("预览卡仅用于确认动作摘要"))
    }

    @Test
    fun stickyEditorActionsHideWhenKeyboardIsVisibleInsteadOfJumpingAboveIme() {
        val stickyActions = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/PlanEditorStickyActions.kt"
        )

        assertTrue(stickyActions.contains("WindowInsets.ime.getBottom"))
        assertTrue(stickyActions.contains("if (isKeyboardVisible) return"))
        assertFalse(stickyActions.contains(".imePadding()"))
    }

    @Test
    fun timedStageCardsExposeCollapsedExpandedPatternWithoutCompositionModelChange() {
        val timedRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt"
        )

        assertTrue(timedRoute.contains("expandedStageIds"))
        assertTrue(timedRoute.contains("点按展开设置"))
        assertTrue(timedRoute.contains("if (expanded)"))
        assertFalse(timedRoute.contains("outer target"))
        assertFalse(timedRoute.contains("inner stage"))
    }

    @Test
    fun planPolishRemovesUserFacingContractCopyFromEditors() {
        val timedRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt"
        )
        val strengthRoute = sourceFile(
            "src/main/java/com/liujyks/trainflow/feature/plans/StrengthPlanEditorRoute.kt"
        )

        assertFalse(timedRoute.contains("WorkoutPlan:"))
        assertFalse(timedRoute.contains("interval stage"))
        assertFalse(strengthRoute.contains("WorkoutPlan:"))
        assertFalse(strengthRoute.contains("strength block"))
        assertFalse(strengthRoute.contains("planned set"))
        assertFalse(strengthRoute.contains("manual_start"))
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

    private fun functionBody(source: String, signature: String): String {
        val start = source.indexOf(signature)
        require(start >= 0) { "Missing function signature $signature" }
        val nextFunction = source.indexOf("\nprivate fun ", start + signature.length)
        return if (nextFunction >= 0) source.substring(start, nextFunction) else source.substring(start)
    }
}
