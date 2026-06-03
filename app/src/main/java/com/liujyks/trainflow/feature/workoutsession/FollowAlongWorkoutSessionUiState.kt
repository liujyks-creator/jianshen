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
import com.liujyks.trainflow.core.model.WorkoutCommand

internal data class FollowAlongWorkoutSessionUiState(
    val planTitle: String,
    val statusLabel: String,
    val phaseLabel: String,
    val currentActionTitle: String,
    val timerText: String,
    val progressLabel: String,
    val nextActionLabel: String,
    val shortCue: String,
    val mediaPlaceholderTitle: String,
    val mediaPlaceholderDescription: String,
    val demoStatusLabel: String,
    val detailRows: List<FollowAlongWorkoutDetailRowUiState>,
    val boundaryCopy: String,
    val heartRate: FollowAlongWorkoutHeartRateUiState,
    val progressFraction: Float,
    val isPaused: Boolean,
    val isTerminal: Boolean,
    val canPause: Boolean,
    val canResume: Boolean,
    val canSkip: Boolean,
    val canEnd: Boolean,
    val lastControlLabel: String,
    val terminalTitle: String? = null,
    val terminalSummary: String? = null
)

internal data class FollowAlongWorkoutDetailRowUiState(
    val label: String,
    val text: String
)

internal data class FollowAlongWorkoutHeartRateUiState(
    val valueText: String,
    val statusText: String
)

internal enum class FollowAlongWorkoutSessionControl {
    PAUSE,
    RESUME,
    SKIP,
    END
}

internal fun FollowAlongWorkoutSessionControl.toWorkoutCommand(): WorkoutCommand {
    return when (this) {
        FollowAlongWorkoutSessionControl.PAUSE -> WorkoutCommand.PauseSession
        FollowAlongWorkoutSessionControl.RESUME -> WorkoutCommand.ResumeSession
        FollowAlongWorkoutSessionControl.SKIP -> WorkoutCommand.SkipStep
        FollowAlongWorkoutSessionControl.END -> WorkoutCommand.EndSession(reason = "user_requested")
    }
}

internal fun TimedWorkoutEngineState.toFollowAlongWorkoutSessionUiState(
    heartRateState: HeartRateState = HeartRateState(availability = HeartRateAvailability.NOT_CONNECTED),
    exercises: List<Exercise> = FirstActionExerciseFixtures.entries.map { it.exercise }
): FollowAlongWorkoutSessionUiState {
    val exerciseById = exercises.associateBy { exercise -> exercise.id }
    val current = currentStep
    val nextAction = nextActionStep()
    val currentExercise = current
        ?.takeIf { step -> step.kind == TimedSessionStepKind.WORK }
        ?.exerciseId
        ?.let { id -> exerciseById[id] }
    val nextExercise = nextAction?.exerciseId?.let { id -> exerciseById[id] }
    val detailExercise = currentExercise ?: nextExercise
    val totalSteps = steps.size.coerceAtLeast(1)
    val activeStepNumber = when {
        currentStepIndex >= 0 -> (currentStepIndex + 1).coerceAtMost(steps.size)
        else -> 0
    }
    val progressLabel = buildFollowAlongProgressLabel(current, activeStepNumber, steps.size)
    val phaseLabel = when {
        status == SessionStatus.PAUSED -> "已暂停"
        status == SessionStatus.COMPLETED -> "已完成"
        status == SessionStatus.ABANDONED -> "已结束"
        current == null -> "准备"
        current.kind == TimedSessionStepKind.REST -> "休息"
        current.sessionStepKind == SessionStepKind.STRETCH -> "拉伸"
        else -> "跟练动作"
    }
    val currentTitle = when {
        status == SessionStatus.COMPLETED -> "基础跟练完成"
        status == SessionStatus.ABANDONED -> "基础跟练已提前结束"
        current == null -> "准备开始"
        current.kind == TimedSessionStepKind.REST -> "休息"
        else -> current.displayFollowAlongTitle(exerciseById)
    }
    val nextLabel = when {
        status == SessionStatus.COMPLETED -> "没有下一动作"
        status == SessionStatus.ABANDONED -> "本次基础跟练已提前结束"
        nextAction == null -> "最后一个动作"
        else -> "下一动作 · ${nextAction.displayFollowAlongTitle(exerciseById)}"
    }
    val shortCue = when {
        status == SessionStatus.PAUSED -> "时间已冻结，继续后从当前剩余时间恢复。"
        status == SessionStatus.COMPLETED -> "本次基础跟练已完成，先放松呼吸。"
        status == SessionStatus.ABANDONED -> "本次基础跟练已提前结束。"
        current == null -> "点击开始后进入第一步。"
        current.kind == TimedSessionStepKind.REST -> nextExercise?.let { exercise ->
            "调整呼吸，准备 ${exercise.name}。"
        } ?: "调整呼吸，准备结束本次基础跟练。"
        else -> currentExercise?.instructions?.shortCue ?: "跟随倒计时保持动作质量。"
    }
    val mediaTitle = when {
        status == SessionStatus.COMPLETED -> "完成占位"
        status == SessionStatus.ABANDONED -> "结束占位"
        currentExercise != null -> "${currentExercise.name} 演示占位"
        current?.kind == TimedSessionStepKind.REST -> "休息占位"
        else -> "演示占位"
    }
    val terminalTitle = when (status) {
        SessionStatus.COMPLETED -> "基础跟练完成"
        SessionStatus.ABANDONED -> "基础跟练提前结束"
        else -> null
    }
    val terminalSummary = when (status) {
        SessionStatus.COMPLETED ->
            "已完成 $completedStepCount / ${steps.size} 步。当前仍是引擎内存态总结，不写入真实 session records。"
        SessionStatus.ABANDONED -> {
            val reason = earlyEnd?.reason.toFollowAlongEarlyEndReason()
            "已完成 $completedStepCount / ${steps.size} 步后提前结束。$reason 当前仍是引擎内存态总结，不写入真实 session records。"
        }
        else -> null
    }

    return FollowAlongWorkoutSessionUiState(
        planTitle = planTitle,
        statusLabel = status.toFollowAlongStatusLabel(),
        phaseLabel = phaseLabel,
        currentActionTitle = currentTitle,
        timerText = remainingSec.formatFollowAlongTimer(),
        progressLabel = progressLabel,
        nextActionLabel = nextLabel,
        shortCue = shortCue,
        mediaPlaceholderTitle = mediaTitle,
        mediaPlaceholderDescription = "当前动作没有可播放媒体；首版使用动作短提示、步骤和要点做演示占位，不加载远程资源。",
        demoStatusLabel = "演示占位 · 无真实媒体播放",
        detailRows = detailExercise.toDetailRows(),
        boundaryCopy = "基础跟练雏形：只从 E6.1 preset 启动，复用计时引擎和动作内容；不提供真实媒体播放、动作分析、音乐编排或自动口令。",
        heartRate = heartRateState.toFollowAlongHeartRateUiState(),
        progressFraction = activeStepNumber.toFloat() / totalSteps.toFloat(),
        isPaused = status == SessionStatus.PAUSED,
        isTerminal = isTerminal,
        canPause = status == SessionStatus.ACTIVE,
        canResume = status == SessionStatus.PAUSED,
        canSkip = status == SessionStatus.ACTIVE && current != null,
        canEnd = status == SessionStatus.ACTIVE || status == SessionStatus.PAUSED,
        lastControlLabel = controlHistory.lastOrNull()?.toFollowAlongLabel().orEmpty(),
        terminalTitle = terminalTitle,
        terminalSummary = terminalSummary
    )
}

private fun TimedWorkoutEngineState.nextActionStep(): TimedSessionStep? {
    val startIndex = when {
        currentStepIndex < 0 -> 0
        else -> currentStepIndex + 1
    }
    return steps.drop(startIndex).firstOrNull { step -> step.kind == TimedSessionStepKind.WORK }
}

private fun TimedSessionStep.displayFollowAlongTitle(exerciseById: Map<String, Exercise>): String {
    if (kind == TimedSessionStepKind.REST) {
        return "休息 ${durationSec.formatShortFollowAlongDuration()}"
    }

    return exerciseId
        ?.let { id -> exerciseById[id]?.name }
        ?: title.toFollowAlongFallbackTitle()
}

private fun Exercise?.toDetailRows(): List<FollowAlongWorkoutDetailRowUiState> {
    val exercise = this ?: return listOf(
        FollowAlongWorkoutDetailRowUiState(
            label = "动作详情",
            text = "当前步骤没有绑定动作内容。"
        )
    )
    val instructions = exercise.instructions
    return buildList {
        if (instructions.steps.isNotEmpty()) {
            add(FollowAlongWorkoutDetailRowUiState("步骤", instructions.steps.take(3).joinToString("；")))
        }
        if (instructions.keyPoints.isNotEmpty()) {
            add(FollowAlongWorkoutDetailRowUiState("要点", instructions.keyPoints.take(3).joinToString("；")))
        }
        if (instructions.commonMistakes.isNotEmpty()) {
            add(FollowAlongWorkoutDetailRowUiState("常见错误", instructions.commonMistakes.take(2).joinToString("；")))
        }
        if (instructions.breathingCues.isNotEmpty()) {
            add(FollowAlongWorkoutDetailRowUiState("呼吸", instructions.breathingCues.take(2).joinToString("；")))
        }
        if (instructions.cautions.isNotEmpty()) {
            add(FollowAlongWorkoutDetailRowUiState("注意", instructions.cautions.take(2).joinToString("；")))
        }
    }
}

private fun TimedWorkoutControlHistoryEvent.toFollowAlongLabel(): String {
    return when (type) {
        TimedWorkoutControlHistoryType.START_SESSION -> "开始基础跟练"
        TimedWorkoutControlHistoryType.PAUSE_SESSION -> "暂停"
        TimedWorkoutControlHistoryType.RESUME_SESSION -> "继续"
        TimedWorkoutControlHistoryType.SKIP_STEP -> "跳过当前步骤"
        TimedWorkoutControlHistoryType.EXTEND_REST -> "休息延长 ${seconds ?: 0} 秒"
        TimedWorkoutControlHistoryType.END_SESSION -> "提前结束"
    }
}

private fun HeartRateState.toFollowAlongHeartRateUiState(): FollowAlongWorkoutHeartRateUiState {
    return when (availability) {
        HeartRateAvailability.AVAILABLE -> FollowAlongWorkoutHeartRateUiState(
            valueText = "${bpm ?: "--"} bpm",
            statusText = message ?: "低层级心率占位"
        )
        HeartRateAvailability.DISABLED -> FollowAlongWorkoutHeartRateUiState(
            valueText = "-- bpm",
            statusText = "心率显示已关闭"
        )
        HeartRateAvailability.CONNECTING -> FollowAlongWorkoutHeartRateUiState(
            valueText = "-- bpm",
            statusText = "等待心率占位"
        )
        HeartRateAvailability.STALE -> FollowAlongWorkoutHeartRateUiState(
            valueText = "${bpm ?: "--"} bpm",
            statusText = message ?: "心率数据短暂中断"
        )
        HeartRateAvailability.ERROR -> FollowAlongWorkoutHeartRateUiState(
            valueText = "-- bpm",
            statusText = message ?: "心率暂不可用"
        )
        HeartRateAvailability.NOT_CONNECTED -> FollowAlongWorkoutHeartRateUiState(
            valueText = "-- bpm",
            statusText = "未连接设备，仅保留低层级占位"
        )
    }
}

private fun buildFollowAlongProgressLabel(
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

private fun SessionStatus.toFollowAlongStatusLabel(): String {
    return when (this) {
        SessionStatus.READY -> "准备"
        SessionStatus.ACTIVE -> "跟练中"
        SessionStatus.PAUSED -> "暂停"
        SessionStatus.COMPLETED -> "完成"
        SessionStatus.ABANDONED -> "已结束"
    }
}

private fun String?.toFollowAlongEarlyEndReason(): String {
    val reasonText = when (this?.trim()) {
        null, "" -> return ""
        "user_requested" -> "原因：用户主动结束。"
        else -> "原因：提前结束。"
    }
    return reasonText
}

private fun String.toFollowAlongFallbackTitle(): String {
    return when {
        equals("Warmup", ignoreCase = true) -> "热身"
        equals("Stretch", ignoreCase = true) -> "拉伸"
        equals("Cooldown", ignoreCase = true) -> "冷却"
        equals("Timed work", ignoreCase = true) -> "计时动作"
        else -> this
    }
}

private fun Int.formatFollowAlongTimer(): String {
    val safeSeconds = coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

private fun Int.formatShortFollowAlongDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return when {
        minutes > 0 && seconds > 0 -> "${minutes}分${seconds}秒"
        minutes > 0 -> "${minutes}分"
        else -> "${seconds}秒"
    }
}
