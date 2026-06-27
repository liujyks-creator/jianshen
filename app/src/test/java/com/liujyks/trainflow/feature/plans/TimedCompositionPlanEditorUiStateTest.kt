package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.WorkoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedCompositionPlanEditorUiStateTest {
    @Test
    fun defaultCompositionEditorExportsV2PayloadAndKeepsStartGateClosed() {
        val state = buildDefaultTimedCompositionPlanEditorState(planId = "timed-composition-editor")
            .updateTitle("阶段间歇")
            .updateWarmupText("120")
            .updateCooldownText("90")
            .updateRoundsText("4")
            .updateRestBetweenRoundsText("30")
        val plan = state.toWorkoutPlan(timestamp = "2026-06-27T01:00:00Z")
        val block = plan.blocks.single() as TimedCompositionBlock

        assertTrue(state.canSave)
        assertFalse(state.canStartTraining)
        assertEquals("待执行映射完成后可开始", state.startDisabledReason)
        assertEquals(WorkoutMode.TIMED, plan.mode)
        assertEquals("阶段间歇", plan.title)
        assertEquals(2, block.compositionVersion)
        assertEquals(120, block.warmupSec)
        assertEquals(90, block.cooldownSec)
        assertEquals(4, block.rounds)
        assertEquals(30, block.restBetweenRoundsSec)
        assertEquals(listOf("高强工作", "冲刺组合"), block.stageGroups.map { group -> group.name })
    }

    @Test
    fun stageAndTargetEditsMapThroughDraftAdapter() {
        val initial = buildDefaultTimedCompositionPlanEditorState(planId = "timed-composition-editor")
        val stageId = initial.stageGroups.first().id
        val targetId = initial.stageGroups.first().targets.first().id
        val edited = initial
            .updateStageName(stageId, "主训练")
            .updateStageColor(stageId, "#00BCD4")
            .updateTargetName(stageId, targetId, "冲刺")
            .updateTargetDurationText(stageId, targetId, "50")
            .updateTargetKind(stageId, targetId, TimedCompositionTargetKind.CUSTOM)
        val block = edited.toWorkoutPlan().blocks.single() as TimedCompositionBlock
        val group = block.stageGroups.first()
        val target = group.targets.first()

        assertEquals("主训练", group.name)
        assertEquals("#00BCD4", group.colorHex)
        assertEquals("冲刺", target.name)
        assertEquals(50, target.durationSec)
        assertEquals(TimedCompositionTargetKind.CUSTOM, target.kind)
    }

    @Test
    fun stageAndTargetNamesAreLimitedToFourChineseCharactersWidth() {
        val initial = buildDefaultTimedCompositionPlanEditorState(planId = "timed-composition-editor")
        val stageId = initial.stageGroups.first().id
        val targetId = initial.stageGroups.first().targets.first().id
        val edited = initial
            .updateStageName(stageId, "超长阶段名称")
            .updateTargetName(stageId, targetId, "abcdefghijklmnop")
        val stage = edited.stageGroups.first()
        val target = stage.targets.first()

        assertEquals("超长阶段", stage.name)
        assertEquals("abcdefgh", target.name)
    }

    @Test
    fun addTargetStopsAtFiveTargetsPerStage() {
        val initial = buildDefaultTimedCompositionPlanEditorState()
        val stageId = initial.stageGroups.first().id
        val filled = initial
            .addTarget(stageId)
            .addTarget(stageId)
            .addTarget(stageId)
            .addTarget(stageId)

        assertEquals(5, filled.stageGroups.first().targets.size)
        assertFalse(filled.canAddTarget(stageId))
        assertEquals(5, filled.addTarget(stageId).stageGroups.first().targets.size)
        assertTrue(requireNotNull(filled.addTarget(stageId).statusMessage).contains("最多 5 个目标"))
    }

    @Test
    fun copyTargetAddsMatchingTargetImmediatelyAfterSource() {
        val initial = buildDefaultTimedCompositionPlanEditorState(planId = "timed-composition-editor")
        val stageId = initial.stageGroups.first().id
        val targetId = initial.stageGroups.first().targets.first().id
        val copied = initial
            .updateTargetName(stageId, targetId, "冲刺")
            .updateTargetDurationText(stageId, targetId, "55")
            .updateTargetColor(stageId, targetId, "#00BCD4")
            .updateTargetKind(stageId, targetId, TimedCompositionTargetKind.CUSTOM)
            .copyTarget(stageId, targetId)
        val targets = copied.stageGroups.first().targets
        val source = targets[0]
        val duplicate = targets[1]

        assertEquals(3, targets.size)
        assertTrue(source.id != duplicate.id)
        assertEquals(source.name, duplicate.name)
        assertEquals(source.durationSec, duplicate.durationSec)
        assertEquals(source.colorHex, duplicate.colorHex)
        assertEquals(source.kind, duplicate.kind)
        assertEquals(listOf(1, 2, 3), targets.map { target -> target.order })
    }

    @Test
    fun blankNumericFieldsKeepDraftEditableButBlockSave() {
        val stageId = buildDefaultTimedCompositionPlanEditorState().stageGroups.first().id
        val targetId = buildDefaultTimedCompositionPlanEditorState().stageGroups.first().targets.first().id
        val blankTarget = buildDefaultTimedCompositionPlanEditorState()
            .updateTargetDurationText(stageId, targetId, "")
        val blankRounds = buildDefaultTimedCompositionPlanEditorState().updateRoundsText("")

        assertFalse(blankTarget.canSave)
        assertTrue(requireNotNull(blankTarget.validationMessage).contains("时长秒数"))
        assertFalse(blankRounds.canSave)
        assertTrue(requireNotNull(blankRounds.validationMessage).contains("轮数"))
        assertNull(buildDefaultTimedCompositionPlanEditorState().validationMessage)
    }

    @Test
    fun saveDraftMarksV2PlanSavedWithoutOpeningStartGate() {
        val saved = buildDefaultTimedCompositionPlanEditorState(planId = "timed-composition-editor")
            .saveDraftPlan(timestamp = "2026-06-27T01:00:00Z")
        val plan = requireNotNull(saved.savedPlan)

        assertTrue(plan.blocks.single() is TimedCompositionBlock)
        assertFalse(saved.canStartTraining)
        assertNotNull(saved.statusMessage)
        assertTrue(requireNotNull(saved.statusMessage).contains("执行映射完成后可开始"))
    }
}
