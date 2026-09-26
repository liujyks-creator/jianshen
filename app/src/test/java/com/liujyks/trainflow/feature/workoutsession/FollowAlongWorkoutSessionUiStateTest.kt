package com.liujyks.trainflow.feature.workoutsession

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowAlongWorkoutSessionUiStateTest {
    @Test
    fun elapsedTimeAndOnlyStopAreShown() {
        val state = buildFollowAlongWorkoutSessionUiState(elapsedSec = 65, active = true)

        assertEquals("跟练中", state.statusLabel)
        assertEquals("01:05", state.timerText)
        assertTrue(state.canStop)
        assertTrue(state.endRequiresConfirmation)
        assertEquals(listOf("停止跟练"), state.immediateControls.map { it.label })
        assertEquals(listOf(WorkoutImmediateControlRole.END_SESSION), state.immediateControls.map { it.role })
        assertEquals(listOf(WorkoutImmediateControlPlacement.FIXED_BOTTOM),
            state.immediateControls.map { it.placement })
        assertFalse(state.isTerminal)
        assertFalse(state.canReturn)
    }

    @Test
    fun savedSummaryUsesPersistedDurationAndHidesStages() {
        val state = buildFollowAlongWorkoutSessionUiState(
            elapsedSec = 66,
            active = false,
            stopping = true,
            saved = true,
            persistedSummary = FollowAlongPersistedSummary(65, "未开启心率")
        )

        assertEquals("跟练完成", state.terminalTitle)
        assertEquals("01:05", state.timerText)
        assertTrue(state.terminalSummary.orEmpty().contains("65 秒"))
        assertTrue(state.terminalSummary.orEmpty().contains("未开启心率"))
        assertFalse(state.terminalSummary.orEmpty().contains("阶段"))
        assertFalse(state.terminalSummary.orEmpty().contains("动作"))
        assertFalse(state.terminalSummary.orEmpty().contains("轮次"))
        assertTrue(state.canReturn)
    }

    @Test
    fun savingAndFailureKeepHonestReturnState() {
        val saving = buildFollowAlongWorkoutSessionUiState(
            elapsedSec = 65, active = false, stopping = true
        )
        assertEquals("正在保存", saving.statusLabel)
        assertFalse(saving.canReturn)
        assertFalse(saving.terminalSummary.orEmpty().contains("已保存"))

        val failure = IllegalStateException("原始保存错误")
        val failed = buildFollowAlongWorkoutSessionUiState(
            elapsedSec = 65, active = false, stopping = true, saveFailure = failure
        )
        assertEquals("保存失败", failed.statusLabel)
        assertTrue(failed.terminalSummary.orEmpty().contains("原始保存错误"))
        assertFalse(failed.terminalSummary.orEmpty().contains("已保存"))
        assertTrue(failed.canReturn)
        assertTrue(failed.immediateControls.isEmpty())
    }
}
