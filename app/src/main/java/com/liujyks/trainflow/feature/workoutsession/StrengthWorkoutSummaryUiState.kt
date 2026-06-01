package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.engine.StrengthSessionSetStep
import com.liujyks.trainflow.core.engine.StrengthSessionStepHistoryStatus
import com.liujyks.trainflow.core.engine.StrengthWorkoutControlHistoryType
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngineState
import com.liujyks.trainflow.core.model.Exercise
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.SetEffort
import com.liujyks.trainflow.core.model.StrengthSetRecord
import com.liujyks.trainflow.core.model.WeightValue

internal data class StrengthWorkoutSummaryUiState(
    val title: String,
    val tone: StrengthWorkoutSummaryTone,
    val durationLabel: String,
    val durationSemanticsNote: String,
    val metricItems: List<StrengthWorkoutSummaryMetricUiState>,
    val planVsActualSummary: String,
    val restSummary: String,
    val replacementSummary: String,
    val skippedSummary: String,
    val earlyEndSummary: String,
    val exerciseSummaries: List<StrengthWorkoutSummaryExerciseUiState>,
    val recoveryEntry: StrengthWorkoutRecoveryEntryUiState
)

internal enum class StrengthWorkoutSummaryTone {
    COMPLETED,
    ABANDONED
}

internal data class StrengthWorkoutSummaryMetricUiState(
    val label: String,
    val value: String,
    val helper: String
)

internal data class StrengthWorkoutSummaryExerciseUiState(
    val exerciseName: String,
    val setProgressLabel: String,
    val replacementLabel: String?,
    val skippedLabel: String?,
    val setItems: List<StrengthWorkoutSummarySetUiState>
)

internal data class StrengthWorkoutSummarySetUiState(
    val setLabel: String,
    val actualExerciseLabel: String,
    val plannedWeightLabel: String,
    val actualWeightLabel: String,
    val plannedRepLabel: String,
    val actualRepLabel: String,
    val activeDurationLabel: String,
    val restAfterLabel: String,
    val effortLabel: String,
    val differenceLabel: String
)

internal data class StrengthWorkoutRecoveryEntryUiState(
    val title: String,
    val description: String,
    val enabled: Boolean,
    val generated: Boolean
)

internal fun StrengthWorkoutEngineState.toStrengthWorkoutSummaryUiState(
    exercises: List<Exercise> = FirstActionExerciseFixtures.entries.map { it.exercise }
): StrengthWorkoutSummaryUiState? {
    if (!isTerminal) return null

    val exerciseById = exercises.associateBy { exercise -> exercise.id }
    val skippedSetPlanIds = skippedSetPlanIds()
    val recordBySetPlanId = strengthSetRecords
        .mapNotNull { record -> record.sourceSetPlanId?.let { setPlanId -> setPlanId to record } }
        .toMap()
    val completedActionCount = strengthSetRecords
        .mapNotNull { record -> record.sourceSetPlanId?.let(::setStepByPlanId)?.blockId }
        .distinct()
        .size
    val replacementCount = controlHistory.count { event ->
        event.type == StrengthWorkoutControlHistoryType.REPLACE_EXERCISE
    }
    val skippedSetCount = skippedSetPlanIds.size
    val recordedRestSec = strengthSetRecords.sumOf { record -> record.actualRestAfterSec ?: 0 }
    val missingRestCount = strengthSetRecords.count { record -> record.actualRestAfterSec == null }
    val differenceSummary = buildPlanVsActualSummary()
    val restSummary = buildRestSummary(recordedRestSec, missingRestCount)
    val trainedAreaSummary = buildTrainedAreaSummary(exerciseById)

    return StrengthWorkoutSummaryUiState(
        title = when (status) {
            SessionStatus.COMPLETED -> "力量训练复盘"
            SessionStatus.ABANDONED -> "提前结束记录"
            else -> "力量训练总结"
        },
        tone = when (status) {
            SessionStatus.COMPLETED -> StrengthWorkoutSummaryTone.COMPLETED
            else -> StrengthWorkoutSummaryTone.ABANDONED
        },
        durationLabel = sessionElapsedSec.formatSummaryDuration(),
        durationSemanticsNote = "当前为引擎 session elapsed / active set elapsed 记录，不等同真实 wall-clock startedAt / endedAt；本阶段不写入持久化会话。",
        metricItems = listOf(
            StrengthWorkoutSummaryMetricUiState(
                label = "动作",
                value = "$completedActionCount / ${setSteps.distinctBy { it.blockId }.size}",
                helper = "已确认记录覆盖的动作 / 计划动作"
            ),
            StrengthWorkoutSummaryMetricUiState(
                label = "组数",
                value = "${strengthSetRecords.size} / ${setSteps.size}",
                helper = "已确认组 / 计划组"
            ),
            StrengthWorkoutSummaryMetricUiState(
                label = "跳过",
                value = "$skippedSetCount 组",
                helper = if (skippedSetCount == 0) "没有跳过动作或组" else "查看下方跳过摘要"
            ),
            StrengthWorkoutSummaryMetricUiState(
                label = "替换",
                value = "$replacementCount 次",
                helper = if (replacementCount == 0) "没有替换动作" else "保留原计划动作引用"
            ),
            StrengthWorkoutSummaryMetricUiState(
                label = "组耗时",
                value = strengthSetRecords.sumOf { it.activeDurationSec ?: 0 }.formatSummaryDuration(),
                helper = "来自已确认 strength set records"
            ),
            StrengthWorkoutSummaryMetricUiState(
                label = "实际休息",
                value = recordedRestSec.formatSummaryDuration(),
                helper = if (missingRestCount == 0) "休息均已记录" else "$missingRestCount 组暂无休息记录"
            )
        ),
        planVsActualSummary = differenceSummary,
        restSummary = restSummary,
        replacementSummary = buildReplacementSummary(exerciseById),
        skippedSummary = buildSkippedSummary(exerciseById, skippedSetPlanIds),
        earlyEndSummary = buildEarlyEndSummary(),
        exerciseSummaries = buildExerciseSummaries(
            exerciseById = exerciseById,
            recordBySetPlanId = recordBySetPlanId,
            skippedSetPlanIds = skippedSetPlanIds
        ),
        recoveryEntry = StrengthWorkoutRecoveryEntryUiState(
            title = "查看恢复建议",
            description = if (trainedAreaSummary == "本次未识别到动作部位") {
                "E5.4 将接入完整恢复建议；当前仅保留入口占位。"
            } else {
                "E5.4 将基于 $trainedAreaSummary 接入完整恢复建议；当前仅保留入口占位。"
            },
            enabled = false,
            generated = false
        )
    )
}

private fun StrengthWorkoutEngineState.buildExerciseSummaries(
    exerciseById: Map<String, Exercise>,
    recordBySetPlanId: Map<String, StrengthSetRecord>,
    skippedSetPlanIds: Set<String>
): List<StrengthWorkoutSummaryExerciseUiState> {
    return setSteps
        .groupBy { step -> step.blockId }
        .values
        .map { steps ->
            val firstStep = steps.first()
            val records = steps.mapNotNull { step -> recordBySetPlanId[step.setPlanId] }
            val skippedCount = steps.count { step -> step.setPlanId in skippedSetPlanIds }
            val originalExerciseId = firstStep.plannedExerciseId(records)
            val exerciseName = exerciseById.exerciseName(originalExerciseId)
            val replacementExerciseNames = (steps.map { step -> step.exerciseId } + records.map { record -> record.exerciseId })
                .filterNot { exerciseId -> exerciseId == originalExerciseId }
                .distinct()
                .map(exerciseById::exerciseName)
            val replacementLabel = replacementExerciseNames.takeIf { names -> names.isNotEmpty() }?.let { names ->
                "部分组替换为 ${names.take(3).joinToString("、")}；每组保留实际动作。"
            }
            StrengthWorkoutSummaryExerciseUiState(
                exerciseName = exerciseName,
                setProgressLabel = "完成 ${records.size} / ${steps.size} 组",
                replacementLabel = replacementLabel,
                skippedLabel = skippedCount.takeIf { it > 0 }?.let { "跳过 $it 组" },
                setItems = steps.map { step ->
                    val record = recordBySetPlanId[step.setPlanId]
                    step.toSummarySetUiState(
                        record = record,
                        plannedExerciseId = originalExerciseId,
                        exerciseById = exerciseById
                    )
                }
            )
        }
}

private fun StrengthSessionSetStep.toSummarySetUiState(
    record: StrengthSetRecord?,
    plannedExerciseId: String,
    exerciseById: Map<String, Exercise>
): StrengthWorkoutSummarySetUiState {
    return StrengthWorkoutSummarySetUiState(
        setLabel = "${setKind.summaryLabel} · 第 ${exerciseSetIndex + 1} 组",
        actualExerciseLabel = actualExerciseLabel(
            record = record,
            plannedExerciseId = plannedExerciseId,
            exerciseById = exerciseById
        ),
        plannedWeightLabel = plannedWeight.formatWeight(),
        actualWeightLabel = record?.actualWeight.formatWeight(),
        plannedRepLabel = plannedRepTarget.formatRepTarget(),
        actualRepLabel = record?.actualReps?.let { "$it 次" } ?: "未记录",
        activeDurationLabel = record?.activeDurationSec?.formatSummaryDuration() ?: "未记录",
        restAfterLabel = record?.actualRestAfterSec?.formatSummaryDuration() ?: "未记录",
        effortLabel = record?.effort?.summaryLabel ?: "未记录",
        differenceLabel = record?.differenceLabel(plannedWeight, plannedRepTarget) ?: "未确认记录"
    )
}

private fun StrengthSessionSetStep.actualExerciseLabel(
    record: StrengthSetRecord?,
    plannedExerciseId: String,
    exerciseById: Map<String, Exercise>
): String {
    val actualExerciseId = record?.exerciseId ?: exerciseId
    val actualExerciseName = exerciseById.exerciseName(actualExerciseId)
    val plannedExerciseName = exerciseById.exerciseName(plannedExerciseId)
    val substitutedFromId = record?.substitutedFromExerciseId ?: substitutedFromExerciseId
    val substitutedFromName = exerciseById.exerciseName(substitutedFromId ?: plannedExerciseId)

    return when {
        record == null && (substitutedFromId != null || actualExerciseId != plannedExerciseId) ->
            "实际动作：未记录；跳过时为 $actualExerciseName（替换自 $substitutedFromName）"
        record == null ->
            "实际动作：未记录；计划动作 $plannedExerciseName"
        substitutedFromId != null || actualExerciseId != plannedExerciseId ->
            "实际动作：$actualExerciseName（替换自 $substitutedFromName）"
        else ->
            "实际动作：$actualExerciseName"
    }
}

private fun StrengthWorkoutEngineState.buildPlanVsActualSummary(): String {
    if (strengthSetRecords.isEmpty()) {
        return "暂无已确认组记录，无法形成计划与实际差异。"
    }

    val weightDiffCount = strengthSetRecords.count { record ->
        record.actualWeight != null && record.actualWeight != record.plannedWeight
    }
    val repDiffCount = strengthSetRecords.count { record ->
        record.actualReps != null && !record.actualReps.matches(record.plannedRepTarget)
    }

    return if (weightDiffCount == 0 && repDiffCount == 0) {
        "已确认组的重量和次数都落在计划值内；本总结只记录差异，不生成下次加重量建议。"
    } else {
        "重量差异 $weightDiffCount 组，次数差异 $repDiffCount 组；本总结只记录差异，不生成下次加重量建议。"
    }
}

private fun StrengthWorkoutEngineState.buildRestSummary(
    recordedRestSec: Int,
    missingRestCount: Int
): String {
    if (strengthSetRecords.isEmpty()) {
        return "暂无已确认组记录，未形成实际休息。"
    }
    val missingText = if (missingRestCount > 0) {
        "；$missingRestCount 组暂无实际休息，通常是最后一组、尚未进入休息或提前结束。"
    } else {
        "。"
    }
    return "已记录实际休息 ${recordedRestSec.formatSummaryDuration()}$missingText"
}

private fun StrengthWorkoutEngineState.buildReplacementSummary(
    exerciseById: Map<String, Exercise>
): String {
    val replacementEvents = controlHistory.filter { event ->
        event.type == StrengthWorkoutControlHistoryType.REPLACE_EXERCISE
    }
    if (replacementEvents.isEmpty()) {
        return "没有替换动作。"
    }

    val labels = replacementEvents.take(3).joinToString("、") { event ->
        val fromName = exerciseById[event.fromExerciseId]?.name ?: event.fromExerciseId ?: "原动作"
        val toName = exerciseById[event.toExerciseId]?.name ?: event.toExerciseId ?: "替换动作"
        "$fromName -> $toName"
    }
    val more = (replacementEvents.size - 3).coerceAtLeast(0)
        .takeIf { it > 0 }
        ?.let { "等 $it 次" }
        .orEmpty()
    return "替换 ${replacementEvents.size} 次：$labels$more。"
}

private fun StrengthWorkoutEngineState.buildSkippedSummary(
    exerciseById: Map<String, Exercise>,
    skippedSetPlanIds: Set<String>
): String {
    if (skippedSetPlanIds.isEmpty()) {
        return "没有跳过动作或组。"
    }

    val labels = setSteps
        .filter { step -> step.setPlanId in skippedSetPlanIds }
        .groupBy { step -> step.blockId }
        .values
        .map { steps ->
            val exerciseId = steps.first().plannedExerciseId(records = emptyList())
            val exerciseName = exerciseById.exerciseName(exerciseId)
            "$exerciseName ${steps.size} 组"
        }
    val visibleLabels = labels
        .take(3)
        .joinToString("、")
    val more = labels
        .drop(3)
        .size
        .takeIf { it > 0 }
        ?.let { "，另 $it 个动作分组" }
        .orEmpty()
    return "跳过 ${skippedSetPlanIds.size} 组：$visibleLabels$more。"
}

private fun StrengthSessionSetStep.plannedExerciseId(
    records: List<StrengthSetRecord>
): String {
    return substitutedFromExerciseId
        ?: records.firstNotNullOfOrNull { record -> record.substitutedFromExerciseId }
        ?: exerciseId
}

private fun Map<String, Exercise>.exerciseName(exerciseId: String): String {
    return this[exerciseId]?.name ?: exerciseId
}

private fun StrengthWorkoutEngineState.buildEarlyEndSummary(): String {
    if (status == SessionStatus.COMPLETED) {
        val hasSkipped = skippedSetPlanIds().isNotEmpty()
        return if (hasSkipped) {
            "本次已到达完成终态，其中包含主动跳过动作或组。"
        } else {
            "本次按力量训练流程完成。"
        }
    }

    val record = earlyEnd ?: return "本次训练提前结束，暂无更多进度记录。"
    val reason = record.reason.localizedEarlyEndReason()
    val currentStep = record.currentStepKind?.summaryLabel ?: "当前步骤"
    val actual = record.currentStepActualDurationSec?.formatSummaryDuration() ?: "0秒"
    val remaining = record.currentStepRemainingSec?.formatSummaryDuration() ?: "0秒"

    return "本次训练提前结束。原因：$reason；结束时在 $currentStep，已确认 ${record.completedSetCount} 组，当前步骤已执行 $actual，剩余 $remaining。"
}

private fun StrengthWorkoutEngineState.buildTrainedAreaSummary(
    exerciseById: Map<String, Exercise>
): String {
    val muscleLabels = strengthSetRecords
        .map { record -> record.exerciseId }
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

private fun StrengthWorkoutEngineState.skippedSetPlanIds(): Set<String> {
    return stepHistory
        .filter { record ->
            record.status == StrengthSessionStepHistoryStatus.SKIPPED &&
                record.kind in skippableSummarySetSteps
        }
        .map { record -> record.setPlanId }
        .filterNot { setPlanId ->
            strengthSetRecords.any { record -> record.sourceSetPlanId == setPlanId }
        }
        .toSet()
}

private fun StrengthWorkoutEngineState.setStepByPlanId(setPlanId: String): StrengthSessionSetStep? {
    return setSteps.firstOrNull { step -> step.setPlanId == setPlanId }
}

private fun StrengthSetRecord.differenceLabel(
    plannedWeight: WeightValue?,
    plannedRepTarget: RepTarget?
): String {
    val parts = buildList {
        if (actualWeight == null && plannedWeight != null) {
            add("重量未记录")
        } else if (actualWeight != null && actualWeight != plannedWeight) {
            add("重量 ${plannedWeight.formatWeight()} -> ${actualWeight.formatWeight()}")
        }

        if (actualReps == null) {
            add("次数未记录")
        } else if (!actualReps.matches(plannedRepTarget)) {
            add("次数 ${plannedRepTarget.formatRepTarget()} -> $actualReps 次")
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("；") ?: "计划内完成"
}

private fun Int?.matches(target: RepTarget?): Boolean {
    val reps = this ?: return false
    return when (target) {
        is RepTarget.Fixed -> reps == target.reps
        is RepTarget.Range -> reps in target.minReps..target.maxReps
        null -> true
    }
}

private fun String?.localizedEarlyEndReason(): String {
    return when (this?.trim()) {
        null, "" -> "用户提前结束"
        "user_requested", "user_exit" -> "用户主动结束"
        else -> "提前结束"
    }
}

private val SessionStepKind.summaryLabel: String
    get() = when (this) {
        SessionStepKind.STRENGTH_PREPARE_SET -> "准备本组"
        SessionStepKind.STRENGTH_ACTIVE_SET -> "本组进行中"
        SessionStepKind.STRENGTH_CONFIRM_SET -> "确认记录"
        SessionStepKind.STRENGTH_REST -> "休息"
        SessionStepKind.COMPLETED -> "完成"
        else -> contractValue
    }

private val com.liujyks.trainflow.core.model.StrengthSetKind.summaryLabel: String
    get() = when (this) {
        com.liujyks.trainflow.core.model.StrengthSetKind.WARMUP -> "热身组"
        com.liujyks.trainflow.core.model.StrengthSetKind.WORKING -> "正式组"
        com.liujyks.trainflow.core.model.StrengthSetKind.DROP -> "递减组"
        com.liujyks.trainflow.core.model.StrengthSetKind.BACKOFF -> "回退组"
    }

private val SetEffort.summaryLabel: String
    get() = when (this) {
        SetEffort.EASY -> "轻松"
        SetEffort.GOOD -> "刚好"
        SetEffort.HARD -> "很吃力"
        SetEffort.FORM_BREAKDOWN -> "动作变形"
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
    return "$valueText ${weight.unit.contractValue}"
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

private val skippableSummarySetSteps = setOf(
    SessionStepKind.STRENGTH_PREPARE_SET,
    SessionStepKind.STRENGTH_ACTIVE_SET,
    SessionStepKind.STRENGTH_CONFIRM_SET
)
