package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanManagementUiStateTest {
    @Test
    fun defaultPlanListShowsTimedAndStrengthSummaries() {
        val state = buildDefaultPlanManagementState()
        val items = state.listItems

        assertEquals(2, items.size)
        assertEquals("计时训练", items[0].modeLabel)
        assertEquals("计时", items[0].modeBadge)
        assertTrue(items[0].summary.contains("预计"))
        assertTrue(items[0].detailSummary.contains("动作提醒"))
        assertEquals("力量训练", items[1].modeLabel)
        assertEquals("力量", items[1].modeBadge)
        assertTrue(items[1].summary.contains("组"))
        assertTrue(items[1].detailSummary.contains("计划值预填"))
    }

    @Test
    fun selectingAPlanMapsDetailSectionsWithoutStartingTraining() {
        val strengthId = buildDefaultPlanManagementState().plans[1].id
        val state = buildDefaultPlanManagementState().selectPlan(strengthId)
        val detail = requireNotNull(state.selectedDetail)

        assertEquals(strengthId, detail.id)
        assertEquals("力量训练", detail.modeLabel)
        assertTrue(detail.canStartTraining)
        assertEquals("开始力量训练", detail.startStatus)
        assertTrue(detail.editStatus.contains("后续接入"))
        assertTrue(detail.sections.any { section -> section.title == "动作与组" })
    }

    @Test
    fun timedPlanDetailEnablesStartTrainingForE3SessionScreen() {
        val state = buildDefaultPlanManagementState()
        val detail = requireNotNull(state.selectedDetail)

        assertEquals("计时训练", detail.modeLabel)
        assertTrue(detail.canStartTraining)
        assertEquals("开始计时训练", detail.startStatus)
    }

    @Test
    fun copyingAPlanCreatesNewPlanWithNewIdAndKeepsContractShape() {
        val state = buildDefaultPlanManagementState()
        val original = state.plans.first()
        val copiedState = state.copyPlan(original.id)
        val copied = requireNotNull(copiedState.selectedPlan)
        val originalCircuit = original.blocks.filterIsInstance<TimedCircuitBlock>().single()
        val copiedCircuit = copied.blocks.filterIsInstance<TimedCircuitBlock>().single()

        assertEquals(3, copiedState.plans.size)
        assertNotEquals(original.id, copied.id)
        assertEquals("全身计时循环 副本", copied.title)
        assertEquals(original.mode, copied.mode)
        assertEquals(originalCircuit.items.size, copiedCircuit.items.size)
        assertNotEquals(originalCircuit.id, copiedCircuit.id)
        assertNotEquals(originalCircuit.items.first().id, copiedCircuit.items.first().id)
        assertTrue(requireNotNull(copiedState.statusMessage).contains("已复制"))
    }

    @Test
    fun deleteRequiresConfirmationAndCanBeCancelled() {
        val state = buildDefaultPlanManagementState()
        val planId = state.plans.first().id
        val pending = state.requestDeletePlan(planId)
        val cancelled = pending.cancelDeletePlan()

        assertEquals(planId, pending.pendingDeletePlanId)
        assertNotNull(pending.pendingDeletePlanTitle)
        assertEquals(2, pending.plans.size)
        assertNull(cancelled.pendingDeletePlanId)
        assertEquals(2, cancelled.plans.size)
    }

    @Test
    fun confirmedDeleteRemovesPlanAndMovesSelection() {
        val state = buildDefaultPlanManagementState()
        val firstId = state.plans.first().id
        val secondId = state.plans[1].id
        val deleted = state
            .requestDeletePlan(firstId)
            .confirmDeletePlan()

        assertEquals(1, deleted.plans.size)
        assertFalse(deleted.plans.any { it.id == firstId })
        assertEquals(secondId, deleted.selectedPlanId)
        assertTrue(requireNotNull(deleted.statusMessage).contains("已删除"))
    }

    @Test
    fun deletingLastPlanShowsEmptyState() {
        val firstPlanOnly = PlanManagementScreenState(
            plans = listOf(buildDefaultPlanManagementState().plans.first())
        )
        val deleted = firstPlanOnly
            .requestDeletePlan(firstPlanOnly.plans.first().id)
            .confirmDeletePlan()

        assertTrue(deleted.isEmpty)
        assertTrue(deleted.listItems.isEmpty())
        assertNull(deleted.selectedDetail)
        assertNull(deleted.selectedPlanId)
    }

    @Test
    fun strengthCopyKeepsStrengthBlocksAndSetCount() {
        val state = buildDefaultPlanManagementState()
        val strength = state.plans[1]
        val copied = requireNotNull(state.copyPlan(strength.id).selectedPlan)
        val originalBlocks = strength.blocks.filterIsInstance<StrengthExerciseBlock>()
        val copiedBlocks = copied.blocks.filterIsInstance<StrengthExerciseBlock>()

        assertEquals(originalBlocks.size, copiedBlocks.size)
        assertEquals(originalBlocks.sumOf { it.sets.size }, copiedBlocks.sumOf { it.sets.size })
        assertNotEquals(originalBlocks.first().sets.first().id, copiedBlocks.first().sets.first().id)
        assertTrue(copied.title.contains("副本"))
    }
}
