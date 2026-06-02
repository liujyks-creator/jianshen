package com.liujyks.trainflow.feature.history

import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.SessionStepRecord
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StrengthSetRecord
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.WorkoutSession

internal data class HistoryScreenState(
    val sessions: List<WorkoutSession>,
    val selectedSessionId: String? = sessions.firstOrNull()?.id
) {
    val isEmpty: Boolean = sessions.isEmpty()

    val dateGroups: List<HistoryDateGroupUiState>
        get() = sessions
            .sortedByDescending { session -> session.dateKey }
            .groupBy { session -> session.dateKey }
            .map { (date, sessionsOnDate) ->
                HistoryDateGroupUiState(
                    dateLabel = date,
                    items = sessionsOnDate.map { session ->
                        session.toListItem(selected = session.id == selectedSessionId)
                    }
                )
            }

    val selectedDetail: HistorySessionDetailUiState?
        get() = selectedSession?.toDetailState()

    val actionTrend: BasicTrendUiState
        get() = sessions.toActionTrend()

    val volumeTrend: BasicTrendUiState
        get() = sessions.toVolumeTrend()

    val emptyStateTitle: String
        get() = "暂无训练历史"

    val emptyStateDescription: String
        get() = "当前仅展示本次内存态或示例记录；真实历史保存将在后续接入。"

    private val selectedSession: WorkoutSession?
        get() = sessions.firstOrNull { session -> session.id == selectedSessionId } ?: sessions.firstOrNull()
}

internal data class HistoryDateGroupUiState(
    val dateLabel: String,
    val items: List<HistorySessionListItemUiState>
)

internal data class HistorySessionListItemUiState(
    val id: String,
    val dateLabel: String,
    val modeBadge: String,
    val modeLabel: String,
    val title: String,
    val statusLabel: String,
    val durationLabel: String,
    val keySummary: String,
    val selected: Boolean
)

internal data class HistorySessionDetailUiState(
    val id: String,
    val title: String,
    val subtitle: String,
    val sourceNote: String,
    val rows: List<HistorySummaryRowUiState>
)

internal data class HistorySummaryRowUiState(
    val label: String,
    val value: String,
    val helper: String
)

internal data class BasicTrendUiState(
    val title: String,
    val description: String,
    val rows: List<BasicTrendRowUiState>,
    val emptyMessage: String? = null
)

internal data class BasicTrendRowUiState(
    val primary: String,
    val secondary: String,
    val metric: String,
    val helper: String
)

internal fun buildDefaultHistoryScreenState(): HistoryScreenState {
    return HistoryScreenState(sessions = defaultHistorySessions())
}

internal fun HistoryScreenState.selectSession(sessionId: String): HistoryScreenState {
    if (sessions.none { session -> session.id == sessionId }) return this
    return copy(selectedSessionId = sessionId)
}

internal fun defaultHistorySessions(): List<WorkoutSession> {
    return listOf(
        WorkoutSession(
            id = "history-strength-2026-06-01",
            planId = "plan-strength-default",
            mode = WorkoutMode.STRENGTH,
            planSnapshot = strengthSnapshot("上肢力量记录"),
            status = SessionStatus.COMPLETED,
            startedAt = "2026-06-01T19:20:00Z",
            endedAt = "2026-06-01T19:58:00Z",
            strengthSetRecords = listOf(
                strengthRecord(
                    id = "set-bench-1",
                    exerciseId = "barbell-bench-press",
                    sourceSetPlanId = "bench-working-1",
                    order = 1,
                    plannedKg = 60.0,
                    actualKg = 60.0,
                    reps = 10,
                    activeSec = 42,
                    restSec = 90
                ),
                strengthRecord(
                    id = "set-bench-2",
                    exerciseId = "barbell-bench-press",
                    sourceSetPlanId = "bench-working-2",
                    order = 2,
                    plannedKg = 60.0,
                    actualKg = 62.5,
                    reps = 8,
                    activeSec = 39,
                    restSec = 100
                ),
                strengthRecord(
                    id = "set-row-1",
                    exerciseId = "one-arm-dumbbell-row",
                    sourceSetPlanId = "row-working-1",
                    order = 3,
                    plannedKg = 20.0,
                    actualKg = 20.0,
                    reps = 10,
                    activeSec = 45,
                    restSec = null
                )
            )
        ),
        WorkoutSession(
            id = "history-timed-2026-05-30",
            planId = "plan-timed-default",
            mode = WorkoutMode.TIMED,
            planSnapshot = timedSnapshot("全身循环记录"),
            status = SessionStatus.COMPLETED,
            startedAt = "2026-05-30T08:10:00Z",
            endedAt = "2026-05-30T08:31:00Z",
            stepHistory = listOf(
                timedStep("warmup", SessionStepKind.TIMED_WORK, 180),
                timedStep("jumping-jacks-r1", SessionStepKind.TIMED_WORK, 40),
                timedStep("rest-r1", SessionStepKind.TIMED_REST, 20),
                timedStep("squat-r1", SessionStepKind.TIMED_WORK, 40),
                timedStep("stretch", SessionStepKind.STRETCH, 120)
            )
        ),
        WorkoutSession(
            id = "history-strength-2026-05-28",
            planId = "plan-strength-default",
            mode = WorkoutMode.STRENGTH,
            planSnapshot = strengthSnapshot("上肢力量记录"),
            status = SessionStatus.ABANDONED,
            startedAt = "2026-05-28T20:00:00Z",
            endedAt = "2026-05-28T20:18:00Z",
            strengthSetRecords = listOf(
                strengthRecord(
                    id = "set-bench-previous",
                    exerciseId = "barbell-bench-press",
                    sourceSetPlanId = "bench-working-1",
                    order = 1,
                    plannedKg = 57.5,
                    actualKg = 57.5,
                    reps = 8,
                    activeSec = 43,
                    restSec = 75
                )
            )
        ),
        WorkoutSession(
            id = "history-timed-2026-05-27",
            planId = "plan-timed-default",
            mode = WorkoutMode.TIMED,
            planSnapshot = timedSnapshot("全身循环记录"),
            status = SessionStatus.ABANDONED,
            startedAt = "2026-05-27T07:45:00Z",
            endedAt = "2026-05-27T07:55:00Z",
            stepHistory = listOf(
                timedStep("warmup-short", SessionStepKind.TIMED_WORK, 120),
                timedStep("jumping-jacks-short", SessionStepKind.TIMED_WORK, 30),
                timedStep("rest-skipped", SessionStepKind.TIMED_REST, 0, skipped = true)
            )
        )
    )
}

private fun WorkoutSession.toListItem(selected: Boolean): HistorySessionListItemUiState {
    return HistorySessionListItemUiState(
        id = id,
        dateLabel = dateKey,
        modeBadge = mode.modeBadge,
        modeLabel = mode.modeLabel,
        title = planSnapshot.title,
        statusLabel = status.statusLabel,
        durationLabel = durationSec().formatDuration(),
        keySummary = keySummary(),
        selected = selected
    )
}

private fun WorkoutSession.toDetailState(): HistorySessionDetailUiState {
    val summaryRows = when (mode) {
        WorkoutMode.TIMED -> timedDetailRows()
        WorkoutMode.STRENGTH -> strengthDetailRows()
        WorkoutMode.FOLLOW_ALONG -> listOf(
            HistorySummaryRowUiState(
                label = "跟练",
                value = "后续接入",
                helper = "E6 前不展示完整跟练历史。"
            )
        )
    }
    return HistorySessionDetailUiState(
        id = id,
        title = planSnapshot.title,
        subtitle = "${dateKey} · ${mode.modeLabel} · ${status.statusLabel}",
        sourceNote = "当前详情来自内存态 session seed，不读取 Room session records；计划快照只用于展示当时结构。",
        rows = summaryRows
    )
}

private fun WorkoutSession.timedDetailRows(): List<HistorySummaryRowUiState> {
    val completedSteps = stepHistory.count { record -> !record.skipped }
    val skippedSteps = stepHistory.count { record -> record.skipped }
    val plannedSteps = planSnapshot.blocks.filterIsInstance<TimedCircuitBlock>()
        .sumOf { block -> block.items.size * block.rounds }
    return listOf(
        HistorySummaryRowUiState("训练时长", durationSec().formatDuration(), "按 step history 的实际时长合计"),
        HistorySummaryRowUiState("完成步骤", "$completedSteps / ${plannedSteps.coerceAtLeast(completedSteps + skippedSteps)}", "包含热身、拉伸或计时步骤记录"),
        HistorySummaryRowUiState("跳过内容", "$skippedSteps 步", if (skippedSteps == 0) "没有跳过内容" else "仅记录跳过事实，不做表现判断")
    )
}

private fun WorkoutSession.strengthDetailRows(): List<HistorySummaryRowUiState> {
    val plannedSets = planSnapshot.blocks.filterIsInstance<StrengthExerciseBlock>().sumOf { block -> block.sets.size }
    val totalReps = strengthSetRecords.sumOf { record -> record.actualReps ?: 0 }
    val totalLoad = strengthSetRecords.totalLoadKg()
    val replaced = strengthSetRecords.count { record -> record.substitutedFromExerciseId != null }
    return listOf(
        HistorySummaryRowUiState("训练时长", durationSec().formatDuration(), "按已确认组耗时和实际休息合计"),
        HistorySummaryRowUiState("确认组数", "${strengthSetRecords.size} / $plannedSets", "计划值和实际值保持区分"),
        HistorySummaryRowUiState("总次数", "$totalReps 次", "来自已确认 strength set records"),
        HistorySummaryRowUiState("训练容量", totalLoad.formatLoad(), "按实际重量 * 实际次数轻量汇总"),
        HistorySummaryRowUiState("替换记录", "$replaced 组", "只记录替换来源，不生成自动建议")
    )
}

private fun WorkoutSession.keySummary(): String {
    return when (mode) {
        WorkoutMode.TIMED -> {
            val completed = stepHistory.count { record -> !record.skipped }
            val skipped = stepHistory.count { record -> record.skipped }
            "完成 $completed 步 · 跳过 $skipped 步"
        }

        WorkoutMode.STRENGTH -> {
            val setCount = strengthSetRecords.size
            val reps = strengthSetRecords.sumOf { record -> record.actualReps ?: 0 }
            val replaced = strengthSetRecords.count { record -> record.substitutedFromExerciseId != null }
            "确认 $setCount 组 · $reps 次 · 替换 $replaced 组"
        }

        WorkoutMode.FOLLOW_ALONG -> "跟练历史后续接入"
    }
}

private fun List<WorkoutSession>.toActionTrend(): BasicTrendUiState {
    val rowsByExercise = flatMap { session ->
        session.strengthSetRecords.map { record -> session to record }
    }.groupBy { (_, record) -> record.exerciseId }
    val target = rowsByExercise.maxByOrNull { (_, rows) -> rows.size }
    if (target == null) {
        return BasicTrendUiState(
            title = "单动作重量 / 次数历史",
            description = "当前没有可展示的力量组记录。",
            rows = emptyList(),
            emptyMessage = "暂无单动作重量 / 次数历史；完成力量训练并确认组记录后再展示。"
        )
    }
    val exerciseName = target.key.exerciseName()
    val rows = target.value
        .groupBy { (session) -> session.id }
        .map { (_, sessionRows) ->
            val session = sessionRows.first().first
            val records = sessionRows.map { (_, record) -> record }
            val heaviest = records.mapNotNull { record -> record.actualWeight }.maxByOrNull { weight -> weight.value }
            val reps = records.sumOf { record -> record.actualReps ?: 0 }
            BasicTrendRowUiState(
                primary = session.dateKey,
                secondary = session.planSnapshot.title,
                metric = "${heaviest.formatWeight()} · $reps 次",
                helper = "${records.size} 组；仅展示历史记录，不判断是否需要调整重量。"
            )
        }
        .sortedByDescending { row -> row.primary }
    return BasicTrendUiState(
        title = "$exerciseName 重量 / 次数历史",
        description = "按单动作展示已确认组的重量与次数，不生成自动加重量建议。",
        rows = rows
    )
}

private fun List<WorkoutSession>.toVolumeTrend(): BasicTrendUiState {
    val rows = filter { session -> session.strengthSetRecords.isNotEmpty() }
        .sortedByDescending { session -> session.dateKey }
        .map { session ->
            val sets = session.strengthSetRecords.size
            val reps = session.strengthSetRecords.sumOf { record -> record.actualReps ?: 0 }
            val load = session.strengthSetRecords.totalLoadKg()
            BasicTrendRowUiState(
                primary = session.dateKey,
                secondary = session.planSnapshot.title,
                metric = load.formatLoad(),
                helper = "$sets 组 · $reps 次；计时训练不纳入重量容量。"
            )
        }
    return if (rows.isEmpty()) {
        BasicTrendUiState(
            title = "训练容量历史",
            description = "按力量训练已确认组汇总。",
            rows = emptyList(),
            emptyMessage = "暂无训练容量记录；需要实际重量和次数后才能汇总。"
        )
    } else {
        BasicTrendUiState(
            title = "训练容量历史",
            description = "按总组数、总次数和实际重量 * 次数做基础汇总。",
            rows = rows
        )
    }
}

private fun strengthSnapshot(title: String): WorkoutPlanSnapshot {
    return WorkoutPlanSnapshot(
        title = title,
        mode = WorkoutMode.STRENGTH,
        blocks = listOf(
            StrengthExerciseBlock(
                id = "history-bench",
                order = 1,
                exerciseId = "barbell-bench-press",
                sets = listOf(
                    StrengthSetPlan("bench-working-1", 1, StrengthSetKind.WORKING),
                    StrengthSetPlan("bench-working-2", 2, StrengthSetKind.WORKING)
                )
            ),
            StrengthExerciseBlock(
                id = "history-row",
                order = 2,
                exerciseId = "one-arm-dumbbell-row",
                sets = listOf(
                    StrengthSetPlan("row-working-1", 1, StrengthSetKind.WORKING)
                )
            )
        )
    )
}

private fun timedSnapshot(title: String): WorkoutPlanSnapshot {
    return WorkoutPlanSnapshot(
        title = title,
        mode = WorkoutMode.TIMED,
        blocks = listOf(
            TimedCircuitBlock(
                id = "history-circuit",
                order = 1,
                rounds = 2,
                items = listOf(
                    TimedExerciseItem(
                        id = "history-jumping-jacks",
                        exerciseId = "jumping-jacks",
                        workDurationSec = 40,
                        restAfterSec = 20
                    ),
                    TimedExerciseItem(
                        id = "history-squat",
                        exerciseId = "bodyweight-squat",
                        workDurationSec = 40,
                        restAfterSec = 20
                    )
                )
            )
        )
    )
}

private fun strengthRecord(
    id: String,
    exerciseId: String,
    sourceSetPlanId: String,
    order: Int,
    plannedKg: Double,
    actualKg: Double,
    reps: Int,
    activeSec: Int,
    restSec: Int?
): StrengthSetRecord {
    return StrengthSetRecord(
        id = id,
        exerciseId = exerciseId,
        sourceSetPlanId = sourceSetPlanId,
        setOrder = order,
        setKind = StrengthSetKind.WORKING,
        plannedWeight = WeightValue(plannedKg, WeightUnit.KG),
        plannedRepTarget = RepTarget.Range(minReps = 8, maxReps = 12),
        actualWeight = WeightValue(actualKg, WeightUnit.KG),
        actualReps = reps,
        activeDurationSec = activeSec,
        actualRestAfterSec = restSec
    )
}

private fun timedStep(
    id: String,
    kind: SessionStepKind,
    durationSec: Int,
    skipped: Boolean = false
): SessionStepRecord {
    return SessionStepRecord(
        stepId = id,
        kind = kind,
        startedAt = "2026-05-30T08:10:00Z",
        endedAt = "2026-05-30T08:11:00Z",
        skipped = skipped,
        actualDurationSec = durationSec
    )
}

private val WorkoutSession.dateKey: String
    get() = startedAt?.take(10) ?: "未记录日期"

private val WorkoutMode.modeLabel: String
    get() = when (this) {
        WorkoutMode.TIMED -> "计时训练"
        WorkoutMode.STRENGTH -> "力量训练"
        WorkoutMode.FOLLOW_ALONG -> "跟练"
    }

private val WorkoutMode.modeBadge: String
    get() = when (this) {
        WorkoutMode.TIMED -> "计时"
        WorkoutMode.STRENGTH -> "力量"
        WorkoutMode.FOLLOW_ALONG -> "跟练"
    }

private val SessionStatus.statusLabel: String
    get() = when (this) {
        SessionStatus.COMPLETED -> "已完成"
        SessionStatus.ABANDONED -> "提前结束"
        SessionStatus.READY -> "未开始"
        SessionStatus.ACTIVE -> "进行中"
        SessionStatus.PAUSED -> "已暂停"
    }

private fun WorkoutSession.durationSec(): Int {
    return when (mode) {
        WorkoutMode.TIMED -> stepHistory.sumOf { record -> record.actualDurationSec ?: 0 }
        WorkoutMode.STRENGTH -> strengthSetRecords.sumOf { record ->
            (record.activeDurationSec ?: 0) + (record.actualRestAfterSec ?: 0)
        }
        WorkoutMode.FOLLOW_ALONG -> stepHistory.sumOf { record -> record.actualDurationSec ?: 0 }
    }
}

private fun List<StrengthSetRecord>.totalLoadKg(): Double {
    return sumOf { record ->
        val weight = record.actualWeight
        val reps = record.actualReps
        if (weight?.unit == WeightUnit.KG && reps != null) {
            weight.value * reps
        } else {
            0.0
        }
    }
}

private fun String.exerciseName(): String {
    return when (this) {
        "barbell-bench-press" -> "杠铃卧推"
        "one-arm-dumbbell-row" -> "单臂哑铃划船"
        "jumping-jacks" -> "开合跳"
        "bodyweight-squat" -> "徒手深蹲"
        else -> this
    }
}

private fun WeightValue?.formatWeight(): String {
    val weight = this ?: return "未记录重量"
    val valueText = if (weight.value % 1.0 == 0.0) {
        weight.value.toInt().toString()
    } else {
        weight.value.toString()
    }
    return "$valueText ${weight.unit.contractValue}"
}

private fun Double.formatLoad(): String {
    if (this <= 0.0) return "暂无容量"
    val valueText = if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }
    return "$valueText kg-reps"
}

private fun Int.formatDuration(): String {
    val safeSeconds = coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return when {
        minutes > 0 && seconds > 0 -> "${minutes}分${seconds}秒"
        minutes > 0 -> "${minutes}分"
        else -> "${seconds}秒"
    }
}
