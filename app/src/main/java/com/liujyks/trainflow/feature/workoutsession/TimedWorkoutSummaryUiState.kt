package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendation
import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendationGenerator
import com.liujyks.trainflow.core.engine.TimedSessionStep
import com.liujyks.trainflow.core.engine.TimedSessionStepHistoryRecord
import com.liujyks.trainflow.core.engine.TimedSessionStepHistoryStatus
import com.liujyks.trainflow.core.engine.TimedSessionStepKind
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.model.Exercise
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind

internal data class TimedWorkoutSummaryUiState(
    val title: String,
    val tone: TimedWorkoutSummaryTone,
    val durationLabel: String,
    val durationSemanticsNote: String,
    val metricItems: List<TimedWorkoutSummaryMetricUiState>,
    val skippedSummary: String,
    val restExtensionSummary: String,
    val earlyEndSummary: String,
    val trainedAreaSummary: String,
    val recoveryEntry: TimedWorkoutRecoveryEntryUiState
)

internal enum class TimedWorkoutSummaryTone {
    COMPLETED,
    ABANDONED
}

internal data class TimedWorkoutSummaryMetricUiState(
    val label: String,
    val value: String,
    val helper: String
)

internal data class TimedWorkoutRecoveryEntryUiState(
    val title: String,
    val description: String,
    val enabled: Boolean,
    val generated: Boolean,
    val recommendation: BasicRecoveryRecommendation? = null
)

internal fun TimedWorkoutEngineState.toTimedWorkoutSummaryUiState(
    exercises: List<Exercise> = FirstActionExerciseFixtures.entries.map { it.exercise }
): TimedWorkoutSummaryUiState? {
    if (!isTerminal) return null

    val exerciseById = exercises.associateBy { exercise -> exercise.id }
    val completedPhaseSummary = buildCompletedPhaseSummary()
    val roundSummary = buildRoundSummary()
    val skippedSummary = buildSkippedSummary(exerciseById)
    val restExtensionSummary = buildRestExtensionSummary(exerciseById)
    val earlyEndSummary = buildEarlyEndSummary()
    val trainedAreaSummary = buildTrainedAreaSummary(exerciseById)
    val recoveryRecommendation = BasicRecoveryRecommendationGenerator.fromExerciseIds(
        sessionId = sessionId,
        exerciseIds = completedRecoveryExerciseIds(),
        exercises = exercises
    )
    val recoveryEntry = TimedWorkoutRecoveryEntryUiState(
        title = "查看恢复建议",
        description = if (recoveryRecommendation.hasRecommendation) {
            "已根据本次完成动作生成基础放松方向。"
        } else {
            "本次没有可识别的已完成动作，暂不生成恢复建议。"
        },
        enabled = recoveryRecommendation.hasRecommendation,
        generated = recoveryRecommendation.hasRecommendation,
        recommendation = recoveryRecommendation.takeIf { recommendation -> recommendation.hasRecommendation }
    )

    return TimedWorkoutSummaryUiState(
        title = when (status) {
            SessionStatus.COMPLETED -> "完成复盘"
            SessionStatus.ABANDONED -> "提前结束记录"
            else -> "训练总结"
        },
        tone = when (status) {
            SessionStatus.COMPLETED -> TimedWorkoutSummaryTone.COMPLETED
            else -> TimedWorkoutSummaryTone.ABANDONED
        },
        durationLabel = activeElapsedSec.formatSummaryDuration(),
        durationSemanticsNote = "当前为引擎 active elapsed，并在终态写入本地 session；不等同真实 wall-clock startedAt / endedAt。",
        metricItems = listOf(
            TimedWorkoutSummaryMetricUiState(
                label = "总时长",
                value = activeElapsedSec.formatSummaryDuration(),
                helper = "按训练引擎有效推进时间统计"
            ),
            TimedWorkoutSummaryMetricUiState(
                label = "完成阶段",
                value = completedPhaseSummary.value,
                helper = completedPhaseSummary.helper
            ),
            TimedWorkoutSummaryMetricUiState(
                label = "步骤进度",
                value = "$completedStepCount / ${steps.size}",
                helper = "包含已完成与训练中主动跳过的步骤"
            ),
            TimedWorkoutSummaryMetricUiState(
                label = "轮次进度",
                value = roundSummary.value,
                helper = roundSummary.helper
            ),
            TimedWorkoutSummaryMetricUiState(
                label = "跳过内容",
                value = "${skippedStepHistory.size} 步",
                helper = if (skippedStepHistory.isEmpty()) "没有跳过内容" else "查看下方跳过摘要"
            ),
            TimedWorkoutSummaryMetricUiState(
                label = "延长休息",
                value = extendedRestSec.formatSummaryDuration(),
                helper = if (restExtensionHistory.isEmpty()) "没有延长休息" else "${restExtensionHistory.size} 次延长"
            )
        ),
        skippedSummary = skippedSummary,
        restExtensionSummary = restExtensionSummary,
        earlyEndSummary = earlyEndSummary,
        trainedAreaSummary = trainedAreaSummary,
        recoveryEntry = recoveryEntry
    )
}

private fun TimedWorkoutEngineState.completedRecoveryExerciseIds(): List<String> {
    return stepHistory
        .filter { record ->
            record.timedKind == TimedSessionStepKind.WORK &&
                record.status == TimedSessionStepHistoryStatus.COMPLETED
        }
        .mapNotNull { record -> record.exerciseId }
        .distinct()
}

private data class SummaryValue(
    val value: String,
    val helper: String
)

private fun TimedWorkoutEngineState.buildCompletedPhaseSummary(): SummaryValue {
    val hasSkippedSteps = skippedStepHistory.isNotEmpty()
    val terminalRecords = stepHistory.filter { record ->
        record.status == TimedSessionStepHistoryStatus.COMPLETED ||
            record.status == TimedSessionStepHistoryStatus.SKIPPED
    }
    val workCount = terminalRecords.count { record ->
        record.timedKind == TimedSessionStepKind.WORK &&
            record.kind != SessionStepKind.STRETCH
    }
    val restCount = terminalRecords.count { record ->
        record.timedKind == TimedSessionStepKind.REST
    }
    val stretchCount = terminalRecords.count { record ->
        record.kind == SessionStepKind.STRETCH
    }
    val parts = buildList {
        if (workCount > 0) add("动作 $workCount")
        if (restCount > 0) add("休息 $restCount")
        if (stretchCount > 0) add("拉伸 $stretchCount")
    }
    return SummaryValue(
        value = parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "暂无完成阶段",
        helper = if (hasSkippedSteps) {
            "来自 step history 的完成/跳过记录，含跳过"
        } else {
            "来自 step history 的完成记录"
        }
    )
}

private fun TimedWorkoutEngineState.buildRoundSummary(): SummaryValue {
    val hasSkippedSteps = skippedStepHistory.isNotEmpty()
    val totalRounds = steps.mapNotNull { step -> step.roundCount }.maxOrNull()
        ?: return SummaryValue(value = "无循环轮次", helper = "本计划没有 timed circuit 轮次")
    val completedRounds = (1..totalRounds).count { round ->
        val roundStepIds = steps
            .filter { step -> step.round == round }
            .map { step -> step.id }
        roundStepIds.isNotEmpty() && roundStepIds.all { stepId ->
            stepHistory.any { record ->
                record.stepId == stepId &&
                    record.status != TimedSessionStepHistoryStatus.STARTED &&
                    record.status != TimedSessionStepHistoryStatus.ABANDONED
            }
        }
    }
    val reachedRound = stepHistory
        .mapNotNull { record -> steps.firstOrNull { step -> step.id == record.stepId }?.round }
        .maxOrNull()

    return SummaryValue(
        value = "$completedRounds / $totalRounds 轮",
        helper = reachedRound?.let { round ->
            if (hasSkippedSteps) {
                "已到第 $round 轮，含跳过"
            } else {
                "已到第 $round 轮"
            }
        } ?: "尚未进入循环轮次"
    )
}

private fun TimedWorkoutEngineState.buildSkippedSummary(
    exerciseById: Map<String, Exercise>
): String {
    if (skippedStepHistory.isEmpty()) {
        return "没有跳过内容。"
    }

    val skippedLabels = skippedStepHistory
        .take(3)
        .joinToString("、") { record -> record.displayTitle(exerciseById) }
    val moreCount = (skippedStepHistory.size - 3).coerceAtLeast(0)
    val moreText = if (moreCount > 0) "等 $moreCount 步" else ""

    return "跳过 ${skippedStepHistory.size} 步：$skippedLabels$moreText。"
}

private fun TimedWorkoutEngineState.buildRestExtensionSummary(
    exerciseById: Map<String, Exercise>
): String {
    if (restExtensionHistory.isEmpty()) {
        return "没有延长休息。"
    }

    val groupedDetails = restExtensionHistory
        .groupBy { record ->
            RestExtensionDetailKey(
                roundIndex = record.roundIndex,
                previousStageTitle = record.previousStageTitle,
                restTitle = record.title
            )
        }
        .entries
        .take(2)
        .joinToString("；") { (key, records) ->
            val addedSec = records.sumOf { record -> record.addedSec }
            val roundLabel = key.roundIndex?.let { round -> "第 $round 轮 " }.orEmpty()
            val anchor = key.previousStageTitle?.let { title ->
                exerciseById[title]?.name ?: title.toFallbackStepTitle()
            }
                ?: key.restTitle.toRestTitle()
            "$roundLabel${anchor}后 +${addedSec.formatSummaryDuration()}"
        }
    val moreCount = restExtensionHistory.size - restExtensionHistory
        .groupBy { record ->
            RestExtensionDetailKey(
                roundIndex = record.roundIndex,
                previousStageTitle = record.previousStageTitle,
                restTitle = record.title
            )
        }
        .entries
        .take(2)
        .sumOf { entry -> entry.value.size }
    val moreText = if (moreCount > 0) "；另有 $moreCount 次" else ""

    return "额外休息 +${extendedRestSec.formatSummaryDuration()}，共 ${restExtensionHistory.size} 次；$groupedDetails$moreText。"
}

private data class RestExtensionDetailKey(
    val roundIndex: Int?,
    val previousStageTitle: String?,
    val restTitle: String
)

private fun TimedWorkoutEngineState.buildEarlyEndSummary(): String {
    if (status == SessionStatus.COMPLETED) {
        if (skippedStepHistory.isNotEmpty()) {
            return "本次已到达完成终态，其中包含主动跳过内容。"
        }
        return "本次按流程完成。"
    }

    val record = earlyEnd ?: return "本次训练提前结束，暂无更多进度记录。"
    val reason = record.reason.localizedEarlyEndReason()
    val stepTitle = record.currentStepTitle?.toRestTitle() ?: "当前步骤"
    val actual = record.currentStepActualDurationSec?.formatSummaryDuration() ?: "0秒"
    val remaining = record.currentStepRemainingSec?.formatSummaryDuration() ?: "0秒"

    return "本次训练提前结束。原因：$reason；结束时在 $stepTitle，当前步骤已执行 $actual，剩余 $remaining。"
}

private fun TimedWorkoutEngineState.buildTrainedAreaSummary(
    exerciseById: Map<String, Exercise>
): String {
    val muscleLabels = stepHistory
        .filter { record ->
            record.timedKind == TimedSessionStepKind.WORK &&
                record.status == TimedSessionStepHistoryStatus.COMPLETED
        }
        .mapNotNull { record -> record.exerciseId }
        .distinct()
        .flatMap { exerciseId ->
            val exercise = exerciseById[exerciseId] ?: return@flatMap emptyList()
            exercise.primaryMuscleIds + exercise.secondaryMuscleIds
        }
        .distinct()
        .map { muscleId -> muscleId.muscleLabel() }

    return if (muscleLabels.isEmpty()) {
        "本次未识别到动作部位"
    } else {
        "主要部位：${muscleLabels.take(5).joinToString("、")}"
    }
}

private fun TimedSessionStepHistoryRecord.displayTitle(
    exerciseById: Map<String, Exercise>
): String {
    if (timedKind == TimedSessionStepKind.REST) {
        return title.toRestTitle()
    }

    return exerciseId
        ?.let { id -> exerciseById[id]?.name }
        ?: title.toFallbackStepTitle()
}

private fun String?.localizedEarlyEndReason(): String {
    return when (this?.trim()) {
        null, "" -> "用户提前结束"
        "user_requested", "user_exit" -> "用户主动结束"
        else -> "提前结束"
    }
}

private fun String.toRestTitle(): String {
    return if (equals("Rest", ignoreCase = true)) "休息" else toFallbackStepTitle()
}

private fun String.toFallbackStepTitle(): String {
    return when {
        equals("Warmup", ignoreCase = true) -> "热身"
        equals("Stretch", ignoreCase = true) -> "拉伸"
        equals("Cooldown", ignoreCase = true) -> "冷却"
        equals("Timed work", ignoreCase = true) -> "计时动作"
        else -> this
    }
}

private fun String.muscleLabel(): String {
    return when (this) {
        "full_body" -> "全身"
        "calves" -> "小腿"
        "quads" -> "股四头肌"
        "glutes" -> "臀部"
        "hamstrings" -> "腘绳肌"
        "hip_flexors" -> "髋屈肌"
        "core" -> "核心"
        "chest" -> "胸部"
        "triceps" -> "肱三头肌"
        "shoulders" -> "肩部"
        "lats" -> "背阔肌"
        "upper_back" -> "上背"
        "biceps" -> "肱二头肌"
        else -> split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }
}

private fun Int.formatSummaryDuration(): String {
    val safeSeconds = coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return when {
        minutes > 0 && seconds > 0 -> "${minutes}分${seconds}秒"
        minutes > 0 -> "${minutes}分"
        else -> "${seconds}秒"
    }
}
