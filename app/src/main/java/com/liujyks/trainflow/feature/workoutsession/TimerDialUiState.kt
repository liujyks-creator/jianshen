package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedSessionStep
import com.liujyks.trainflow.core.engine.TimedSessionStepKind
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.TimedStageType

internal data class TimerDialUiState(
    val totalRemainingSec: Int,
    val totalProgress: Float,
    val currentStageProgress: Float,
    val currentStageType: TimerDialStageType,
    val currentStageLabel: String,
    val currentStageIndex: Int,
    val currentStageRemainingSec: Int,
    val isPaused: Boolean,
    val isFinalCountdown: Boolean,
    val stageSegments: List<TimerDialStageSegmentUiState>,
    val visualVariant: TimerDialVisualVariant,
    val currentStageIconKey: String,
    val currentStageTimeText: String,
    val totalRemainingText: String,
    val centerActionLabel: String,
    val canTogglePause: Boolean
) {
    companion object {
        val Empty = TimerDialUiState(
            totalRemainingSec = 0,
            totalProgress = 0f,
            currentStageProgress = 0f,
            currentStageType = TimerDialStageType.WORK,
            currentStageLabel = "准备开始",
            currentStageIndex = 0,
            currentStageRemainingSec = 0,
            isPaused = false,
            isFinalCountdown = false,
            stageSegments = emptyList(),
            visualVariant = TimerDialVisualVariant.OFFICIAL_FLOW,
            currentStageIconKey = "timer",
            currentStageTimeText = "00:00",
            totalRemainingText = "00:00",
            centerActionLabel = "当前不可切换",
            canTogglePause = false
        )
    }

    fun clamped(): TimerDialUiState {
        return copy(
            totalProgress = totalProgress.clampedProgress(),
            currentStageProgress = currentStageProgress.clampedProgress(),
            stageSegments = stageSegments.map { segment -> segment.clamped() },
            currentStageRemainingSec = currentStageRemainingSec.coerceAtLeast(0),
            totalRemainingSec = totalRemainingSec.coerceAtLeast(0)
        )
    }
}

internal data class TimerDialStageSegmentUiState(
    val id: String,
    val label: String,
    val stageType: TimerDialStageType,
    val durationSec: Int,
    val progress: Float,
    val isCurrent: Boolean
) {
    fun clamped(): TimerDialStageSegmentUiState {
        return copy(
            durationSec = durationSec.coerceAtLeast(0),
            progress = progress.clampedProgress()
        )
    }
}

internal fun TimedWorkoutEngineState.toTimerDialUiState(
    screenState: TimedWorkoutSessionScreenState,
    visualVariant: TimerDialVisualVariant = TimerDialVisualVariant.OFFICIAL_FLOW
): TimerDialUiState {
    val totalPlannedDurationSec = steps.sumOf { step -> step.durationSec }.coerceAtLeast(1)
    val current = currentStep
    val currentStepProgress = current?.let { step ->
        if (step.durationSec <= 0) {
            0f
        } else {
            (step.durationSec - remainingSec).toFloat() / step.durationSec.toFloat()
        }
    } ?: when (status) {
        SessionStatus.COMPLETED -> 1f
        else -> 0f
    }
    val totalRemainingSec = when {
        status == SessionStatus.COMPLETED -> 0
        status == SessionStatus.ABANDONED -> remainingSec
        currentStepIndex < 0 -> totalPlannedDurationSec
        else -> remainingSec + steps.drop(currentStepIndex + 1).sumOf { step -> step.durationSec }
    }
    val currentIndex = if (currentStepIndex >= 0) {
        (currentStepIndex + 1).coerceAtMost(steps.size)
    } else {
        0
    }
    val currentType = current?.timerDialStageType() ?: TimerDialStageType.WORK
    val segments = steps.mapIndexed { index, step ->
        val progress = when {
            status == SessionStatus.COMPLETED || index < currentStepIndex -> 1f
            index == currentStepIndex -> currentStepProgress
            else -> 0f
        }
        TimerDialStageSegmentUiState(
            id = step.id,
            label = step.title,
            stageType = step.timerDialStageType(),
            durationSec = step.durationSec,
            progress = progress,
            isCurrent = index == currentStepIndex
        )
    }

    return TimerDialUiState(
        totalRemainingSec = totalRemainingSec,
        totalProgress = when (status) {
            SessionStatus.COMPLETED -> 1f
            else -> activeElapsedSec.toFloat() / totalPlannedDurationSec.toFloat()
        },
        currentStageProgress = currentStepProgress,
        currentStageType = currentType,
        currentStageLabel = screenState.currentTitle,
        currentStageIndex = currentIndex,
        currentStageRemainingSec = if (status == SessionStatus.COMPLETED) 0 else remainingSec,
        isPaused = status == SessionStatus.PAUSED,
        isFinalCountdown = screenState.countdownReminder.isActive &&
            screenState.countdownReminder.remainingSec in 1..5 &&
            status == SessionStatus.ACTIVE,
        stageSegments = segments,
        visualVariant = visualVariant,
        currentStageIconKey = screenState.stageIconKey,
        currentStageTimeText = screenState.timerText,
        totalRemainingText = screenState.totalRemainingText,
        centerActionLabel = when {
            screenState.canResume -> "双击继续"
            screenState.canPause -> "双击暂停"
            else -> "当前不可切换"
        },
        canTogglePause = screenState.canPause || screenState.canResume
    ).clamped()
}

private fun TimedSessionStep.timerDialStageType(): TimerDialStageType {
    if (kind == TimedSessionStepKind.REST) {
        return TimerDialStageType.REST
    }

    return when (stageType) {
        TimedStageType.WARMUP -> TimerDialStageType.WARMUP
        TimedStageType.WORK -> TimerDialStageType.WORK
        TimedStageType.REST -> TimerDialStageType.REST
        TimedStageType.COOLDOWN -> TimerDialStageType.COOLDOWN
        TimedStageType.CUSTOM -> TimerDialStageType.CUSTOM
        null -> TimerDialStageType.WORK
    }
}

private fun Float.clampedProgress(): Float {
    return when {
        isNaN() -> 0f
        else -> coerceIn(0f, 1f)
    }
}
