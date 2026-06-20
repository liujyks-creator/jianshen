package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.feature.followalong.buildDefaultFollowAlongScreenState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowAlongWorkoutSessionUiStateTest {
    @Test
    fun mapsCurrentActionMediaCountdownNextActionCueAndHeartRatePlaceholder() {
        val plan = buildDefaultFollowAlongScreenState().plans.single().plan
        val engineState = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(plan),
            command = WorkoutCommand.StartSession
        ).state
        val uiState = engineState.toFollowAlongWorkoutSessionUiState()

        assertEquals("基础跟练：全身动作提示", uiState.planTitle)
        assertEquals("跟练中", uiState.statusLabel)
        assertEquals("跟练动作", uiState.phaseLabel)
        assertTrue(uiState.currentActionTitle.isNotBlank())
        assertTrue(uiState.mediaPlaceholderTitle.contains("演示占位"))
        assertTrue(uiState.mediaPlaceholderDescription.contains("没有可播放媒体"))
        assertTrue(uiState.mediaPlaceholderDescription.contains("不加载远程资源"))
        assertEquals("演示占位 · 无真实媒体播放", uiState.demoStatusLabel)
        assertTrue(uiState.timerText.matches(Regex("\\d{2}:\\d{2}")))
        assertTrue(uiState.progressLabel.contains("步骤 1 /"))
        assertTrue(uiState.nextActionLabel.contains("下一动作"))
        assertTrue(uiState.shortCue.isNotBlank())
        assertEquals("-- bpm", uiState.heartRate.valueText)
        assertEquals("未获取心率", uiState.heartRate.statusText)
        assertTrue(uiState.heartRate.boundaryText.contains("未接入真实设备"))
        assertTrue(uiState.progressFraction > 0f)
        assertTrue(uiState.immediateControls.any { control ->
            control.role == WorkoutImmediateControlRole.PAUSE_SESSION &&
                control.placement == WorkoutImmediateControlPlacement.RHYTHM_SURFACE &&
                control.enabled
        })
        assertTrue(uiState.immediateControls.any { control ->
            control.role == WorkoutImmediateControlRole.SKIP_STEP &&
                control.placement == WorkoutImmediateControlPlacement.FIXED_BOTTOM &&
                control.enabled
        })
        assertTrue(uiState.immediateControls.any { control ->
            control.role == WorkoutImmediateControlRole.END_SESSION &&
                control.placement == WorkoutImmediateControlPlacement.FIXED_BOTTOM &&
                control.enabled
        })
        assertTrue(uiState.endRequiresConfirmation)
    }

    @Test
    fun detailRowsUseFixtureInstructionContent() {
        val plan = buildDefaultFollowAlongScreenState().plans.single().plan
        val engineState = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(plan),
            command = WorkoutCommand.StartSession
        ).state
        val uiState = engineState.toFollowAlongWorkoutSessionUiState()
        val labels = uiState.detailRows.map { row -> row.label }

        assertTrue(labels.contains("步骤"))
        assertTrue(labels.contains("要点"))
        assertTrue(labels.contains("常见错误"))
        assertTrue(uiState.detailRows.all { row -> row.text.isNotBlank() })
    }

    @Test
    fun availableHeartRateStaysSecondaryAndAbstract() {
        val plan = buildDefaultFollowAlongScreenState().plans.single().plan
        val engineState = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(plan),
            command = WorkoutCommand.StartSession
        ).state
        val uiState = engineState.toFollowAlongWorkoutSessionUiState(
            heartRateState = HeartRateState(
                kind = HeartRateStateKind.DEVICE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = 128,
                sourceLabel = "设备数据"
            )
        )

        assertEquals("128 bpm", uiState.heartRate.valueText)
        assertEquals("设备数据", uiState.heartRate.statusText)
    }

    @Test
    fun controlsMapToWorkoutCommands() {
        assertEquals(WorkoutCommand.PauseSession, FollowAlongWorkoutSessionControl.PAUSE.toWorkoutCommand())
        assertEquals(WorkoutCommand.ResumeSession, FollowAlongWorkoutSessionControl.RESUME.toWorkoutCommand())
        assertEquals(WorkoutCommand.SkipStep, FollowAlongWorkoutSessionControl.SKIP.toWorkoutCommand())
        assertEquals(
            WorkoutCommand.EndSession(reason = "user_requested"),
            FollowAlongWorkoutSessionControl.END.toWorkoutCommand()
        )
    }

    @Test
    fun terminalCopyUsesFollowAlongTitlesAndInMemoryBoundary() {
        val plan = buildDefaultFollowAlongScreenState().plans.single().plan
        val completedState = TimedWorkoutEngine.tick(
            state = TimedWorkoutEngine.dispatch(
                state = TimedWorkoutEngine.create(plan),
                command = WorkoutCommand.StartSession
            ).state,
            seconds = 10_000
        ).state
        val completedUi = completedState.toFollowAlongWorkoutSessionUiState()

        assertEquals(SessionStatus.COMPLETED, completedState.status)
        assertEquals("基础跟练完成", completedUi.terminalTitle)
        assertTrue(completedUi.terminalSummary.orEmpty().contains("引擎内存态总结"))
        assertTrue(completedUi.terminalSummary.orEmpty().contains("不写入真实 session records"))

        val abandonedState = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.dispatch(
                state = TimedWorkoutEngine.create(plan),
                command = WorkoutCommand.StartSession
            ).state,
            command = WorkoutCommand.EndSession(reason = "user_requested")
        ).state
        val abandonedUi = abandonedState.toFollowAlongWorkoutSessionUiState()

        assertEquals("基础跟练提前结束", abandonedUi.terminalTitle)
        assertTrue(abandonedUi.terminalSummary.orEmpty().contains("用户主动结束"))
    }

    @Test
    fun boundaryCopyDoesNotUseReservedCapabilityAvailabilityHints() {
        val plan = buildDefaultFollowAlongScreenState().plans.single().plan
        val engineState = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(plan),
            command = WorkoutCommand.StartSession
        ).state
        val uiState = engineState.toFollowAlongWorkoutSessionUiState()
        val copy = listOf(
            uiState.mediaPlaceholderDescription,
            uiState.demoStatusLabel,
            uiState.boundaryCopy
        ).joinToString(" ")
        val reservedAvailabilityHints = listOf(
            "课程平台",
            "教练库",
            "AI 纠错已启用",
            "语音教练",
            "心率告警",
            "热量判断"
        )

        reservedAvailabilityHints.forEach { phrase ->
            assertFalse(copy.contains(phrase))
        }
    }
}
