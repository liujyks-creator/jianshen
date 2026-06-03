package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.engine.TimedSessionStep
import com.liujyks.trainflow.core.engine.TimedSessionStepKind
import com.liujyks.trainflow.core.engine.TimedWorkoutControlHistoryEvent
import com.liujyks.trainflow.core.engine.TimedWorkoutControlHistoryType
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.model.Exercise
import com.liujyks.trainflow.core.model.HeartRateAvailability
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind

internal data class TimedWorkoutSessionScreenState(
    val planTitle: String,
    val statusLabel: String,
    val phaseLabel: String,
    val currentTitle: String,
    val timerText: String,
    val progressLabel: String,
    val nextStepLabel: String,
    val shortCue: String,
    val countdownReminder: TimedWorkoutCountdownReminderUiState,
    val heartRate: HeartRateDisplayUiState,
    val progressFraction: Float,
    val isPaused: Boolean,
    val isTerminal: Boolean,
    val shouldShowNextStepPanel: Boolean,
    val canPause: Boolean,
    val canResume: Boolean,
    val canSkip: Boolean,
    val canExtendRest: Boolean,
    val canEnd: Boolean,
    val skippedStepCount: Int,
    val extendedRestTotalSec: Int,
    val historySummaryLabel: String,
    val lastControlLabel: String,
    val terminalTitle: String? = null,
    val terminalSummary: String? = null,
    val summary: TimedWorkoutSummaryUiState? = null
)

internal data class TimedWorkoutCountdownReminderUiState(
    val type: TimedWorkoutCountdownReminderType,
    val remainingSec: Int,
    val message: String,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val emphasisAnimationEnabled: Boolean
) {
    val isActive: Boolean
        get() = type != TimedWorkoutCountdownReminderType.NONE

    companion object {
        val None = TimedWorkoutCountdownReminderUiState(
            type = TimedWorkoutCountdownReminderType.NONE,
            remainingSec = 0,
            message = "",
            soundEnabled = false,
            vibrationEnabled = false,
            emphasisAnimationEnabled = false
        )
    }
}

internal enum class TimedWorkoutCountdownReminderType {
    NONE,
    ACTION_ENDING,
    REST_ENDING
}

internal fun TimedWorkoutEngineState.toTimedWorkoutSessionScreenState(
    heartRateState: HeartRateState = HeartRateState(availability = HeartRateAvailability.NOT_CONNECTED),
    exercises: List<Exercise> = FirstActionExerciseFixtures.entries.map { it.exercise }
): TimedWorkoutSessionScreenState {
    val exerciseById = exercises.associateBy { exercise -> exercise.id }
    val current = currentStep
    val next = nextDisplayStep()
    val countdownReminder = current.toCountdownReminder(
        status = status,
        remainingSec = remainingSec,
        next = next,
        exerciseById = exerciseById
    )
    val statusLabel = status.toStatusLabel()
    val phaseLabel = when {
        status == SessionStatus.PAUSED -> "已暂停"
        status == SessionStatus.COMPLETED -> "已完成"
        status == SessionStatus.ABANDONED -> "已结束"
        current == null -> "准备"
        current.kind == TimedSessionStepKind.REST -> "休息"
        current.sessionStepKind == SessionStepKind.STRETCH -> "拉伸"
        else -> "动作"
    }
    val currentTitle = when {
        status == SessionStatus.COMPLETED -> "训练完成"
        status == SessionStatus.ABANDONED -> "训练已结束"
        current == null -> "准备开始"
        current.kind == TimedSessionStepKind.REST -> "休息"
        else -> current.displayTitle(exerciseById)
    }
    val nextStepLabel = when {
        status == SessionStatus.COMPLETED -> "没有下一步"
        status == SessionStatus.ABANDONED -> "本次训练已提前结束"
        next == null -> "最后一步"
        else -> "下一步 · ${next.displayTitle(exerciseById)}"
    }
    val shortCue = when {
        status == SessionStatus.PAUSED -> "时间已冻结，继续后从当前剩余时间恢复。"
        status == SessionStatus.COMPLETED -> "本次流程已完成，先放松呼吸。"
        status == SessionStatus.ABANDONED -> "本次训练已提前结束。"
        current == null -> "点击开始后进入第一步。"
        countdownReminder.isActive -> countdownReminder.message
        current.kind == TimedSessionStepKind.REST -> next?.let {
            "调整呼吸，准备 ${it.displayTitle(exerciseById)}。"
        } ?: "调整呼吸，准备结束训练。"
        else -> current.exerciseId
            ?.let { exerciseById[it]?.instructions?.shortCue }
            ?: current.blockFallbackCue()
    }
    val totalSteps = steps.size.coerceAtLeast(1)
    val activeStepNumber = when {
        currentStepIndex >= 0 -> (currentStepIndex + 1).coerceAtMost(steps.size)
        else -> 0
    }
    val progressLabel = buildProgressLabel(current, activeStepNumber, steps.size)
    val terminalTitle = when (status) {
        SessionStatus.COMPLETED -> "计时训练完成"
        SessionStatus.ABANDONED -> "计时训练已提前结束"
        else -> null
    }
    val historySummaryLabel = buildHistorySummaryLabel()
    val terminalSummary = when (status) {
        SessionStatus.COMPLETED -> "已完成 $completedStepCount / ${steps.size} 步。$historySummaryLabel"
        SessionStatus.ABANDONED -> {
            val reason = earlyEnd?.reason.toEarlyEndReasonSummary()
            "已完成 $completedStepCount / ${steps.size} 步后提前结束。$reason$historySummaryLabel"
        }
        else -> null
    }

    return TimedWorkoutSessionScreenState(
        planTitle = planTitle,
        statusLabel = statusLabel,
        phaseLabel = phaseLabel,
        currentTitle = currentTitle,
        timerText = remainingSec.formatTimer(),
        progressLabel = progressLabel,
        nextStepLabel = nextStepLabel,
        shortCue = shortCue,
        countdownReminder = countdownReminder,
        heartRate = heartRateState.toHeartRateDisplayUiState(),
        progressFraction = activeStepNumber.toFloat() / totalSteps.toFloat(),
        isPaused = status == SessionStatus.PAUSED,
        isTerminal = isTerminal,
        shouldShowNextStepPanel = !isTerminal,
        canPause = status == SessionStatus.ACTIVE,
        canResume = status == SessionStatus.PAUSED,
        canSkip = status == SessionStatus.ACTIVE && current != null,
        canExtendRest = status == SessionStatus.ACTIVE && current?.kind == TimedSessionStepKind.REST,
        canEnd = status == SessionStatus.ACTIVE || status == SessionStatus.PAUSED,
        skippedStepCount = skippedStepHistory.size,
        extendedRestTotalSec = extendedRestSec,
        historySummaryLabel = historySummaryLabel,
        lastControlLabel = controlHistory.lastOrNull()?.toLabel().orEmpty(),
        terminalTitle = terminalTitle,
        terminalSummary = terminalSummary,
        summary = toTimedWorkoutSummaryUiState(exercises)
    )
}

private fun String?.toEarlyEndReasonSummary(): String {
    val reasonText = when (this?.trim()) {
        null, "" -> return ""
        "user_requested" -> "用户主动结束"
        else -> "提前结束"
    }

    return " 原因：$reasonText。"
}

private fun TimedWorkoutEngineState.buildHistorySummaryLabel(): String {
    val pauseCount = controlHistory.count { event ->
        event.type == TimedWorkoutControlHistoryType.PAUSE_SESSION
    }
    return "跳过 ${skippedStepHistory.size} 步，休息延长 ${extendedRestSec} 秒，暂停 $pauseCount 次。"
}

private fun TimedWorkoutControlHistoryEvent.toLabel(): String {
    return when (type) {
        TimedWorkoutControlHistoryType.START_SESSION -> "开始训练"
        TimedWorkoutControlHistoryType.PAUSE_SESSION -> "暂停训练"
        TimedWorkoutControlHistoryType.RESUME_SESSION -> "继续训练"
        TimedWorkoutControlHistoryType.SKIP_STEP -> "跳过当前步骤"
        TimedWorkoutControlHistoryType.EXTEND_REST -> "休息延长 ${seconds ?: 0} 秒"
        TimedWorkoutControlHistoryType.END_SESSION -> "提前结束"
    }
}

private fun TimedSessionStep?.toCountdownReminder(
    status: SessionStatus,
    remainingSec: Int,
    next: TimedSessionStep?,
    exerciseById: Map<String, Exercise>
): TimedWorkoutCountdownReminderUiState {
    val step = this ?: return TimedWorkoutCountdownReminderUiState.None
    val cue = step.endingCue ?: return TimedWorkoutCountdownReminderUiState.None
    if (
        status != SessionStatus.ACTIVE ||
        remainingSec <= 0 ||
        remainingSec > cue.thresholdSec
    ) {
        return TimedWorkoutCountdownReminderUiState.None
    }

    val type = when (step.kind) {
        TimedSessionStepKind.WORK -> TimedWorkoutCountdownReminderType.ACTION_ENDING
        TimedSessionStepKind.REST -> TimedWorkoutCountdownReminderType.REST_ENDING
    }
    val message = when (type) {
        TimedWorkoutCountdownReminderType.ACTION_ENDING ->
            "动作即将结束，还剩 ${remainingSec} 秒。"
        TimedWorkoutCountdownReminderType.REST_ENDING -> {
            val nextTitle = next?.displayTitle(exerciseById) ?: "下一步"
            "休息即将结束，准备 ${nextTitle}，还剩 ${remainingSec} 秒。"
        }
        TimedWorkoutCountdownReminderType.NONE -> ""
    }

    return TimedWorkoutCountdownReminderUiState(
        type = type,
        remainingSec = remainingSec,
        message = message,
        soundEnabled = cue.soundEnabled,
        vibrationEnabled = cue.vibrationEnabled,
        emphasisAnimationEnabled = cue.emphasisAnimationEnabled
    )
}

private fun TimedWorkoutEngineState.nextDisplayStep(): TimedSessionStep? {
    val nextIndex = when {
        currentStepIndex < 0 -> 0
        else -> currentStepIndex + 1
    }
    return steps.getOrNull(nextIndex)
}

private fun TimedSessionStep.displayTitle(exerciseById: Map<String, Exercise>): String {
    if (kind == TimedSessionStepKind.REST) {
        return "休息 ${durationSec.formatShortDuration()}"
    }

    return exerciseId
        ?.let { id -> exerciseById[id]?.name }
        ?: blockFallbackTitle()
}

private fun TimedSessionStep.blockFallbackTitle(): String {
    return when {
        sessionStepKind == SessionStepKind.STRETCH -> "拉伸"
        blockId.contains("warmup", ignoreCase = true) -> "热身"
        blockId.contains("cooldown", ignoreCase = true) -> "冷却"
        else -> "计时动作"
    }
}

private fun TimedSessionStep.blockFallbackCue(): String {
    return when {
        sessionStepKind == SessionStepKind.STRETCH -> "放慢呼吸，保持动作稳定。"
        blockId.contains("warmup", ignoreCase = true) -> "逐步提高活动度，不要急着冲强度。"
        else -> "保持节奏，注意动作质量。"
    }
}

private fun buildProgressLabel(
    current: TimedSessionStep?,
    activeStepNumber: Int,
    totalSteps: Int
): String {
    val stepText = if (activeStepNumber == 0) {
        "准备 · 共 $totalSteps 步"
    } else {
        "步骤 $activeStepNumber / $totalSteps"
    }
    val roundText = current?.round?.let { round ->
        current.roundCount?.let { roundCount -> " · 第 $round / $roundCount 轮" }
    }.orEmpty()
    return stepText + roundText
}

private fun SessionStatus.toStatusLabel(): String {
    return when (this) {
        SessionStatus.READY -> "准备"
        SessionStatus.ACTIVE -> "进行中"
        SessionStatus.PAUSED -> "暂停"
        SessionStatus.COMPLETED -> "完成"
        SessionStatus.ABANDONED -> "已结束"
    }
}

private fun Int.formatTimer(): String {
    val safeSeconds = coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

private fun Int.formatShortDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return when {
        minutes > 0 && seconds > 0 -> "${minutes}分${seconds}秒"
        minutes > 0 -> "${minutes}分"
        else -> "${seconds}秒"
    }
}
