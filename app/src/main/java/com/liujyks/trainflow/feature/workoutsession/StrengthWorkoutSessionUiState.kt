package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.engine.StrengthSessionSetStep
import com.liujyks.trainflow.core.engine.StrengthWorkoutControlHistoryEvent
import com.liujyks.trainflow.core.engine.StrengthWorkoutControlHistoryType
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngineState
import com.liujyks.trainflow.core.model.Exercise
import com.liujyks.trainflow.core.model.HeartRateAvailability
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue

internal data class StrengthWorkoutSessionScreenState(
    val planTitle: String,
    val statusLabel: String,
    val phaseLabel: String,
    val currentExerciseName: String,
    val setProgressLabel: String,
    val setKindLabel: String,
    val targetSummary: String,
    val primaryMetricLabel: String,
    val primaryMetricText: String,
    val shortCue: String,
    val nextSetLabel: String,
    val confirmSummary: String?,
    val heartRate: StrengthWorkoutHeartRateUiState,
    val progressFraction: Float,
    val isPaused: Boolean,
    val isTerminal: Boolean,
    val canStartSet: Boolean,
    val canCompleteSet: Boolean,
    val canConfirmPlanned: Boolean,
    val canStartNextDuringRest: Boolean,
    val canPause: Boolean,
    val canResume: Boolean,
    val canEnd: Boolean,
    val completedSetCount: Int,
    val totalSetCount: Int,
    val historySummaryLabel: String,
    val lastControlLabel: String,
    val terminalTitle: String? = null,
    val terminalSummary: String? = null
)

internal data class StrengthWorkoutHeartRateUiState(
    val valueText: String,
    val statusText: String,
    val isAvailable: Boolean
)

internal fun StrengthWorkoutEngineState.toStrengthWorkoutSessionScreenState(
    heartRateState: HeartRateState = HeartRateState(availability = HeartRateAvailability.NOT_CONNECTED),
    exercises: List<Exercise> = FirstActionExerciseFixtures.entries.map { it.exercise }
): StrengthWorkoutSessionScreenState {
    val exerciseById = exercises.associateBy { exercise -> exercise.id }
    val current = currentSet
    val next = nextDisplaySet()
    val totalSets = setSteps.size
    val currentName = current?.exerciseName(exerciseById) ?: "准备开始"
    val stepKind = this.currentStepKind
    val phaseLabel = when {
        status == SessionStatus.PAUSED -> "已暂停"
        status == SessionStatus.COMPLETED -> "已完成"
        status == SessionStatus.ABANDONED -> "已结束"
        stepKind == SessionStepKind.STRENGTH_PREPARE_SET -> "准备本组"
        stepKind == SessionStepKind.STRENGTH_ACTIVE_SET -> "本组进行中"
        stepKind == SessionStepKind.STRENGTH_CONFIRM_SET -> "确认记录"
        stepKind == SessionStepKind.STRENGTH_REST -> "休息"
        else -> "准备"
    }
    val primaryMetricLabel = when (stepKind) {
        SessionStepKind.STRENGTH_ACTIVE_SET -> "本组耗时"
        SessionStepKind.STRENGTH_REST -> "休息倒计时"
        SessionStepKind.STRENGTH_CONFIRM_SET -> "完成本组"
        else -> "本组目标"
    }
    val primaryMetricText = when (stepKind) {
        SessionStepKind.STRENGTH_ACTIVE_SET -> activeSetElapsedSec.formatTimer()
        SessionStepKind.STRENGTH_REST -> restRemainingSec.formatTimer()
        SessionStepKind.STRENGTH_CONFIRM_SET -> activeSetElapsedSec.formatTimer()
        else -> current?.targetSummary() ?: "未开始"
    }
    val shortCue = buildShortCue(
        current = current,
        next = next,
        phaseLabel = phaseLabel,
        exerciseById = exerciseById
    )
    val progressBase = totalSets.coerceAtLeast(1)
    val activeNumber = when {
        status == SessionStatus.COMPLETED -> totalSets
        current != null -> current.globalSetIndex + 1
        else -> 0
    }
    val terminalTitle = when (status) {
        SessionStatus.COMPLETED -> "力量训练完成"
        SessionStatus.ABANDONED -> "力量训练已提前结束"
        else -> null
    }
    val historySummaryLabel = buildHistorySummaryLabel()
    val terminalSummary = when (status) {
        SessionStatus.COMPLETED -> "已确认 $completedSetCount / $totalSets 组。$historySummaryLabel"
        SessionStatus.ABANDONED -> {
            val reason = earlyEnd?.reason.toEarlyEndReasonSummary()
            "已确认 $completedSetCount / $totalSets 组后提前结束。$reason$historySummaryLabel"
        }
        else -> null
    }

    return StrengthWorkoutSessionScreenState(
        planTitle = planTitle,
        statusLabel = status.toStatusLabel(),
        phaseLabel = phaseLabel,
        currentExerciseName = when {
            status == SessionStatus.COMPLETED -> "训练完成"
            status == SessionStatus.ABANDONED -> "训练已结束"
            else -> currentName
        },
        setProgressLabel = current?.setProgressLabel() ?: "共 $totalSets 组",
        setKindLabel = current?.setKind?.label.orEmpty(),
        targetSummary = current?.targetSummary() ?: "暂无本组目标",
        primaryMetricLabel = primaryMetricLabel,
        primaryMetricText = primaryMetricText,
        shortCue = shortCue,
        nextSetLabel = next.toNextSetLabel(exerciseById),
        confirmSummary = pendingDraft?.let { draft ->
            "按计划确认：${draft.defaultActualWeight.formatWeight()} · ${draft.defaultActualReps?.let { "$it 次" } ?: "未设次数"}"
        },
        heartRate = heartRateState.toStrengthUiState(),
        progressFraction = activeNumber.toFloat() / progressBase.toFloat(),
        isPaused = status == SessionStatus.PAUSED,
        isTerminal = isTerminal,
        canStartSet = status == SessionStatus.ACTIVE &&
            stepKind == SessionStepKind.STRENGTH_PREPARE_SET,
        canCompleteSet = status == SessionStatus.ACTIVE &&
            stepKind == SessionStepKind.STRENGTH_ACTIVE_SET,
        canConfirmPlanned = status == SessionStatus.ACTIVE &&
            stepKind == SessionStepKind.STRENGTH_CONFIRM_SET &&
            pendingDraft != null,
        canStartNextDuringRest = status == SessionStatus.ACTIVE &&
            stepKind == SessionStepKind.STRENGTH_REST &&
            next != null,
        canPause = status == SessionStatus.ACTIVE && stepKind != null,
        canResume = status == SessionStatus.PAUSED && stepKind != null,
        canEnd = status == SessionStatus.ACTIVE || status == SessionStatus.PAUSED,
        completedSetCount = completedSetCount,
        totalSetCount = totalSets,
        historySummaryLabel = historySummaryLabel,
        lastControlLabel = controlHistory.lastOrNull()?.toLabel().orEmpty(),
        terminalTitle = terminalTitle,
        terminalSummary = terminalSummary
    )
}

private fun StrengthWorkoutEngineState.nextDisplaySet(): StrengthSessionSetStep? {
    val nextIndex = when (currentStepKind) {
        SessionStepKind.STRENGTH_REST -> currentSetIndex + 1
        null -> 0
        else -> currentSetIndex + 1
    }
    return setSteps.getOrNull(nextIndex)
}

private fun StrengthWorkoutEngineState.buildShortCue(
    current: StrengthSessionSetStep?,
    next: StrengthSessionSetStep?,
    phaseLabel: String,
    exerciseById: Map<String, Exercise>
): String {
    return when {
        status == SessionStatus.PAUSED -> "训练已暂停，当前组和休息计时都已冻结。"
        status == SessionStatus.COMPLETED -> "本次力量流程已完成，先放松呼吸。"
        status == SessionStatus.ABANDONED -> "本次力量训练已提前结束。"
        current == null -> "点击开始后进入第一组准备。"
        currentStepKind == SessionStepKind.STRENGTH_PREPARE_SET ->
            "确认器械和站位，准备后点击开始本组。"
        currentStepKind == SessionStepKind.STRENGTH_ACTIVE_SET ->
            exerciseById[current.exerciseId]?.instructions?.shortCue ?: "保持动作质量，完成后点击完成本组。"
        currentStepKind == SessionStepKind.STRENGTH_CONFIRM_SET ->
            "按本组计划值快速确认，保持训练节奏。"
        currentStepKind == SessionStepKind.STRENGTH_REST ->
            next?.let { "调整呼吸，准备 ${it.exerciseName(exerciseById)} · ${it.targetSummary()}。" }
                ?: "调整呼吸，准备结束训练。"
        else -> phaseLabel
    }
}

private fun String?.toEarlyEndReasonSummary(): String {
    val reasonText = when (this?.trim()) {
        null, "" -> return ""
        "user_requested" -> "用户主动结束"
        else -> "提前结束"
    }

    return " 原因：$reasonText。"
}

private fun StrengthWorkoutEngineState.buildHistorySummaryLabel(): String {
    val pauseCount = controlHistory.count { event ->
        event.type == StrengthWorkoutControlHistoryType.PAUSE_SESSION
    }
    val recordedRestSec = strengthSetRecords.sumOf { record -> record.actualRestAfterSec ?: 0 }
    return "记录休息 ${recordedRestSec} 秒，暂停 $pauseCount 次。"
}

private fun StrengthWorkoutControlHistoryEvent.toLabel(): String {
    return when (type) {
        StrengthWorkoutControlHistoryType.START_SESSION -> "开始训练"
        StrengthWorkoutControlHistoryType.PAUSE_SESSION -> "暂停训练"
        StrengthWorkoutControlHistoryType.RESUME_SESSION -> "继续训练"
        StrengthWorkoutControlHistoryType.START_STRENGTH_SET -> "开始本组"
        StrengthWorkoutControlHistoryType.COMPLETE_STRENGTH_SET -> "完成本组"
        StrengthWorkoutControlHistoryType.CONFIRM_STRENGTH_SET -> "按计划确认"
        StrengthWorkoutControlHistoryType.END_SESSION -> "提前结束"
    }
}

private fun StrengthSessionSetStep.exerciseName(exerciseById: Map<String, Exercise>): String {
    return exerciseById[exerciseId]?.name ?: exerciseId
}

private fun StrengthSessionSetStep.setProgressLabel(): String {
    return "第 ${exerciseSetIndex + 1} / $exerciseSetCount 组 · 总 ${globalSetIndex + 1} / $totalSetCount"
}

private fun StrengthSessionSetStep.targetSummary(): String {
    return "${plannedWeight.formatWeight()} · ${plannedRepTarget.formatRepTarget()}"
}

private fun StrengthSessionSetStep?.toNextSetLabel(exerciseById: Map<String, Exercise>): String {
    val set = this ?: return "最后一组"
    return "下一组 · ${set.exerciseName(exerciseById)} · ${set.setProgressLabel()} · ${set.targetSummary()}"
}

private val StrengthSetKind.label: String
    get() = when (this) {
        StrengthSetKind.WARMUP -> "热身组"
        StrengthSetKind.WORKING -> "正式组"
        StrengthSetKind.DROP -> "递减组"
        StrengthSetKind.BACKOFF -> "回退组"
    }

private fun RepTarget?.formatRepTarget(): String {
    return when (this) {
        is RepTarget.Fixed -> "$reps 次"
        is RepTarget.Range -> "$minReps-$maxReps 次"
        null -> "未设次数"
    }
}

private fun WeightValue?.formatWeight(): String {
    val weight = this ?: return "未设重量"
    val valueText = if (weight.value % 1.0 == 0.0) {
        weight.value.toInt().toString()
    } else {
        weight.value.toString()
    }
    return "$valueText ${weight.unit.label}"
}

private val WeightUnit.label: String
    get() = when (this) {
        WeightUnit.KG -> "kg"
        WeightUnit.LB -> "lb"
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

private fun HeartRateState.toStrengthUiState(): StrengthWorkoutHeartRateUiState {
    return when (availability) {
        HeartRateAvailability.DISABLED -> StrengthWorkoutHeartRateUiState(
            valueText = "-- bpm",
            statusText = "心率显示已关闭",
            isAvailable = false
        )
        HeartRateAvailability.NOT_CONNECTED -> StrengthWorkoutHeartRateUiState(
            valueText = "-- bpm",
            statusText = "未连接设备",
            isAvailable = false
        )
        HeartRateAvailability.CONNECTING -> StrengthWorkoutHeartRateUiState(
            valueText = "-- bpm",
            statusText = "等待心率",
            isAvailable = false
        )
        HeartRateAvailability.AVAILABLE -> StrengthWorkoutHeartRateUiState(
            valueText = "${bpm ?: "--"} bpm",
            statusText = message ?: "心率可用",
            isAvailable = bpm != null
        )
        HeartRateAvailability.STALE -> StrengthWorkoutHeartRateUiState(
            valueText = "${bpm ?: "--"} bpm",
            statusText = message ?: "数据短暂中断",
            isAvailable = false
        )
        HeartRateAvailability.ERROR -> StrengthWorkoutHeartRateUiState(
            valueText = "-- bpm",
            statusText = message ?: "心率暂不可用",
            isAvailable = false
        )
    }
}

private fun Int.formatTimer(): String {
    val safeSeconds = coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
