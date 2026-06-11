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
    val totalWorkoutStageCount: Int,
    val completedWorkoutStageCount: Int,
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
            totalWorkoutStageCount = 0,
            completedWorkoutStageCount = 0,
            stageSegments = emptyList(),
            visualVariant = ProductionTimerDialVisualVariant,
            currentStageIconKey = "timer",
            currentStageTimeText = "00:00",
            totalRemainingText = "00:00",
            centerActionLabel = "当前不可切换",
            canTogglePause = false
        )
    }

    fun clamped(): TimerDialUiState {
        val safeTotalRemainingSec = totalRemainingSec.coerceAtLeast(0)
        val safeCurrentStageRemainingSec = currentStageRemainingSec.coerceAtLeast(0)
        return copy(
            totalProgress = totalProgress.clampedProgress(),
            currentStageProgress = currentStageProgress.clampedProgress(),
            totalWorkoutStageCount = totalWorkoutStageCount.coerceAtLeast(0),
            completedWorkoutStageCount = completedWorkoutStageCount.coerceIn(
                0,
                totalWorkoutStageCount.coerceAtLeast(0)
            ),
            stageSegments = stageSegments.map { segment -> segment.clamped() },
            currentStageRemainingSec = safeCurrentStageRemainingSec,
            totalRemainingSec = safeTotalRemainingSec,
            currentStageTimeText = safeCurrentStageRemainingSec.formatTimerText(),
            totalRemainingText = safeTotalRemainingSec.formatTimerText()
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
    visualVariant: TimerDialVisualVariant = ProductionTimerDialVisualVariant
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
    val segments = currentTimerDialCycleSegments(
        currentStepProgress = currentStepProgress
    )
    val workoutCycles = timerDialWorkoutCycles()
    val stageBasedTotalProgress = workoutCycles.stageBasedProgress(
        state = this,
        currentStepProgress = currentStepProgress,
        fallbackTotalPlannedDurationSec = totalPlannedDurationSec
    )
    val completedWorkoutStageCount = workoutCycles.completedCount(
        state = this
    )

    return TimerDialUiState(
        totalRemainingSec = totalRemainingSec,
        totalProgress = when (status) {
            SessionStatus.COMPLETED -> 1f
            else -> stageBasedTotalProgress
        },
        currentStageProgress = currentStepProgress,
        currentStageType = currentType,
        currentStageLabel = screenState.currentTitle,
        currentStageIndex = currentIndex,
        currentStageRemainingSec = if (status == SessionStatus.COMPLETED) 0 else remainingSec,
        isPaused = status == SessionStatus.PAUSED,
        isFinalCountdown = screenState.countdownReminder.isActive &&
            screenState.countdownReminder.emphasisAnimationEnabled &&
            screenState.countdownReminder.remainingSec in 1..5 &&
            status == SessionStatus.ACTIVE,
        totalWorkoutStageCount = workoutCycles.size,
        completedWorkoutStageCount = completedWorkoutStageCount,
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

private fun TimedWorkoutEngineState.currentTimerDialCycleSegments(
    currentStepProgress: Float
): List<TimerDialStageSegmentUiState> {
    if (currentStepIndex !in steps.indices) {
        return emptyList()
    }

    val indexes = currentCycleIndexes().ifEmpty { listOf(currentStepIndex) }
    return indexes.mapNotNull { index ->
        val step = steps.getOrNull(index) ?: return@mapNotNull null
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
}

private fun TimedWorkoutEngineState.currentCycleIndexes(): List<Int> {
    val current = steps.getOrNull(currentStepIndex) ?: return emptyList()
    return when (current.kind) {
        TimedSessionStepKind.WORK -> {
            val nextRestIndex = (currentStepIndex + 1).takeIf { index ->
                val next = steps.getOrNull(index)
                next?.kind == TimedSessionStepKind.REST &&
                    next.blockId == current.blockId &&
                    next.round == current.round
            }
            listOfNotNull(currentStepIndex, nextRestIndex)
        }
        TimedSessionStepKind.REST -> {
            val previousWorkIndex = (currentStepIndex - 1).takeIf { index ->
                val previous = steps.getOrNull(index)
                previous?.kind == TimedSessionStepKind.WORK &&
                    previous.blockId == current.blockId &&
                    previous.round == current.round
            }
            listOfNotNull(previousWorkIndex, currentStepIndex)
        }
    }
}

private data class TimerDialWorkoutCycle(
    val workIndex: Int,
    val restIndex: Int?,
    val durationSec: Int
) {
    val endIndex: Int
        get() = restIndex ?: workIndex
}

private fun TimedWorkoutEngineState.timerDialWorkoutCycles(): List<TimerDialWorkoutCycle> {
    return steps.mapIndexedNotNull { index, step ->
        if (!step.isWorkoutStageForTotalMarker()) {
            return@mapIndexedNotNull null
        }

        val restIndex = (index + 1).takeIf { candidateIndex ->
            val next = steps.getOrNull(candidateIndex)
            next?.kind == TimedSessionStepKind.REST &&
                next.blockId == step.blockId &&
                next.round == step.round
        }
        val restDurationSec = restIndex?.let { steps[it].durationSec } ?: 0
        TimerDialWorkoutCycle(
            workIndex = index,
            restIndex = restIndex,
            durationSec = (step.durationSec + restDurationSec).coerceAtLeast(1)
        )
    }
}

private fun TimedSessionStep.isWorkoutStageForTotalMarker(): Boolean {
    if (kind != TimedSessionStepKind.WORK) {
        return false
    }

    return when (timerDialStageType()) {
        TimerDialStageType.WORK,
        TimerDialStageType.CUSTOM -> true
        TimerDialStageType.WARMUP,
        TimerDialStageType.REST,
        TimerDialStageType.COOLDOWN -> false
    }
}

private fun List<TimerDialWorkoutCycle>.completedCount(
    state: TimedWorkoutEngineState
): Int {
    if (isEmpty()) {
        return 0
    }
    if (state.status == SessionStatus.COMPLETED) {
        return size
    }

    return count { cycle -> state.currentStepIndex > cycle.endIndex }
}

private fun List<TimerDialWorkoutCycle>.stageBasedProgress(
    state: TimedWorkoutEngineState,
    currentStepProgress: Float,
    fallbackTotalPlannedDurationSec: Int
): Float {
    if (isEmpty()) {
        return state.activeElapsedSec.toFloat() / fallbackTotalPlannedDurationSec.toFloat()
    }
    if (state.status == SessionStatus.COMPLETED) {
        return 1f
    }

    val currentCycleIndex = indexOfFirst { cycle ->
        state.currentStepIndex in cycle.workIndex..cycle.endIndex
    }
    if (currentCycleIndex < 0) {
        return completedCount(state).toFloat() / size.toFloat()
    }

    val currentCycle = this[currentCycleIndex]
    val elapsedBeforeCurrentStep = state.steps
        .subList(currentCycle.workIndex, state.currentStepIndex.coerceAtLeast(currentCycle.workIndex))
        .sumOf { step -> step.durationSec }
    val currentStepElapsedSec = state.currentStep?.let { step ->
        step.durationSec.toFloat() * currentStepProgress.clampedProgress()
    } ?: 0f
    val cycleProgress = (
        elapsedBeforeCurrentStep.toFloat() + currentStepElapsedSec
        ) / currentCycle.durationSec.toFloat()

    return (currentCycleIndex.toFloat() + cycleProgress.clampedProgress()) / size.toFloat()
}

private fun Float.clampedProgress(): Float {
    return when {
        isNaN() -> 0f
        else -> coerceIn(0f, 1f)
    }
}

private fun Int.formatTimerText(): String {
    val safeSeconds = coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
