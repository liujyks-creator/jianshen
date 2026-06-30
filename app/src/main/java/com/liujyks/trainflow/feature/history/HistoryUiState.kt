package com.liujyks.trainflow.feature.history

import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.RestBlock
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.SessionStepRecord
import com.liujyks.trainflow.core.model.SetEffort
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StrengthSetRecord
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionTimelineAdapter
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStageKind
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStep
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStepKind
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedRestExtensionRecord
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.WorkoutSession

internal data class HistoryScreenState(
    val sessions: List<WorkoutSession>,
    val selectedSessionId: String? = sessions.firstOrNull()?.id,
    val modeFilter: HistoryModeFilter = HistoryModeFilter.ALL,
    val statusFilter: HistoryStatusFilter = HistoryStatusFilter.ALL,
    val recordSource: HistoryRecordSource = HistoryRecordSource.PERSISTED,
    val pendingCleanupTarget: HistoryCleanupTarget? = null,
    val statusMessage: String? = null
) {
    val isEmpty: Boolean = sessions.isEmpty()

    val filteredSessions: List<WorkoutSession>
        get() = sessions.filter { session ->
            modeFilter.matches(session) && statusFilter.matches(session)
        }

    val filtersUiState: HistoryFiltersUiState
        get() = toFiltersUiState()

    val dateGroups: List<HistoryDateGroupUiState>
        get() = filteredSessions
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
        get() = selectedSession?.toDetailState(recordSource)

    val recordStats: WorkoutRecordStats?
        get() = if (recordSource == HistoryRecordSource.PERSISTED && sessions.isNotEmpty()) {
            sessions.toWorkoutRecordStats()
        } else {
            null
        }

    val overviewUiState: HistoryOverviewUiState?
        get() = recordStats?.toOverviewUiState()

    val recordStatsUiState: WorkoutRecordStatsUiState?
        get() = recordStats?.toUiState()

    val aggregateChartsUiState: WorkoutAggregateChartsUiState?
        get() = if (recordSource == HistoryRecordSource.PERSISTED && filteredSessions.isNotEmpty()) {
            filteredSessions.toWorkoutAggregateChartsUiState()
        } else {
            null
        }

    val timedComparableRestTrendUiState: TimedComparableRestTrendUiState?
        get() = if (recordSource == HistoryRecordSource.PERSISTED && filteredSessions.isNotEmpty()) {
            filteredSessions.toTimedComparableRestTrendUiState()
        } else {
            null
        }

    val strengthComparableSetTrendUiState: StrengthComparableSetTrendUiState?
        get() = if (recordSource == HistoryRecordSource.PERSISTED && filteredSessions.isNotEmpty()) {
            filteredSessions.toStrengthComparableSetTrendUiState()
        } else {
            null
        }

    val cleanupUiState: HistoryCleanupUiState?
        get() = if (recordSource == HistoryRecordSource.PERSISTED && sessions.isNotEmpty()) {
            sessions.toHistoryCleanupUiState()
        } else {
            null
        }

    val pendingCleanupDialog: HistoryCleanupDialogUiState?
        get() = pendingCleanupTarget?.toDialogUiState()

    val actionTrend: BasicTrendUiState
        get() = sessions.toActionTrend()

    val volumeTrend: BasicTrendUiState
        get() = sessions.toVolumeTrend()

    val emptyStateTitle: String
        get() = "暂无训练历史"

    val emptyStateDescription: String
        get() = "完成一次计时、力量或基础跟练训练后，本地记录会出现在这里。"

    val sourcePillLabel: String
        get() = when (recordSource) {
            HistoryRecordSource.PERSISTED -> "本地真实记录"
            HistoryRecordSource.EXAMPLE -> "示例记录"
        }

    val headerDescription: String
        get() = if (isEmpty) {
            emptyStateDescription
        } else {
            "${sessions.size} 条本地记录 · 先看概览，再按筛选查看最近训练、详情和趋势"
        }

    val boundaryNote: String
        get() = when (recordSource) {
            HistoryRecordSource.PERSISTED -> "当前读取本地 Room session records；只展示已保存的训练结果，不生成结论性判断。"
            HistoryRecordSource.EXAMPLE -> "当前为 preview / 测试示例记录；生产记录页优先读取本地 Room session records。"
        }

    private val selectedSession: WorkoutSession?
        get() = filteredSessions.firstOrNull { session -> session.id == selectedSessionId } ?: filteredSessions.firstOrNull()
}

internal enum class HistoryRecordSource {
    PERSISTED,
    EXAMPLE
}

internal enum class HistoryModeFilter(
    val label: String,
    val helper: String
) {
    ALL("全部类型", "timed / strength / follow-along"),
    TIMED("计时", "legacy timed 与 composition v2 分开解释"),
    STRENGTH("力量", "只看力量组记录与同类 set"),
    FOLLOW_ALONG("跟练", "首版跟练记录保持基础说明");

    fun matches(session: WorkoutSession): Boolean {
        return when (this) {
            ALL -> true
            TIMED -> session.mode == WorkoutMode.TIMED
            STRENGTH -> session.mode == WorkoutMode.STRENGTH
            FOLLOW_ALONG -> session.mode == WorkoutMode.FOLLOW_ALONG
        }
    }
}

internal enum class HistoryStatusFilter(
    val label: String,
    val helper: String
) {
    ALL("全部状态", "completed 与 abandoned 都保留"),
    COMPLETED("已完成", "正常完成的训练"),
    ABANDONED("提前结束", "abandoned 仍参与记录回顾");

    fun matches(session: WorkoutSession): Boolean {
        return when (this) {
            ALL -> true
            COMPLETED -> session.status == SessionStatus.COMPLETED
            ABANDONED -> session.status == SessionStatus.ABANDONED
        }
    }
}

internal enum class HistoryTone {
    SUCCESS,
    WARNING,
    INFO,
    NEUTRAL
}

internal sealed interface HistoryCleanupTarget {
    val count: Int

    data class All(
        override val count: Int
    ) : HistoryCleanupTarget

    data class Plan(
        val planId: String,
        val planTitle: String,
        override val count: Int
    ) : HistoryCleanupTarget

    data class Date(
        val dateLabel: String,
        override val count: Int
    ) : HistoryCleanupTarget
}

internal data class HistoryCleanupUiState(
    val title: String,
    val description: String,
    val allOption: HistoryCleanupOptionUiState,
    val planOptions: List<HistoryCleanupOptionUiState>,
    val dateOptions: List<HistoryCleanupOptionUiState>
)

internal data class HistoryCleanupOptionUiState(
    val label: String,
    val helper: String,
    val target: HistoryCleanupTarget
)

internal data class HistoryCleanupDialogUiState(
    val title: String,
    val message: String,
    val confirmLabel: String
)

internal data class HistoryCleanupConfirmationResult(
    val state: HistoryScreenState,
    val target: HistoryCleanupTarget?
)

internal data class HistoryOverviewUiState(
    val title: String,
    val description: String,
    val metrics: List<HistoryOverviewMetricUiState>
)

internal data class HistoryOverviewMetricUiState(
    val label: String,
    val value: String,
    val helper: String,
    val tone: HistoryTone = HistoryTone.NEUTRAL
)

internal data class HistoryFiltersUiState(
    val title: String,
    val description: String,
    val modeOptions: List<HistoryModeFilterOptionUiState>,
    val statusOptions: List<HistoryStatusFilterOptionUiState>,
    val resultLabel: String,
    val emptyMessage: String?
)

internal data class HistoryModeFilterOptionUiState(
    val filter: HistoryModeFilter,
    val label: String,
    val helper: String,
    val count: Int,
    val selected: Boolean
)

internal data class HistoryStatusFilterOptionUiState(
    val filter: HistoryStatusFilter,
    val label: String,
    val helper: String,
    val count: Int,
    val selected: Boolean
)

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
    val statusTone: HistoryTone,
    val durationLabel: String,
    val keySummary: String,
    val flags: List<HistorySessionFlagUiState>,
    val selected: Boolean
)

internal data class HistorySessionFlagUiState(
    val label: String,
    val tone: HistoryTone = HistoryTone.NEUTRAL
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

internal data class WorkoutRecordStats(
    val totalCount: Int,
    val completedCount: Int,
    val abandonedCount: Int,
    val totalElapsedSec: Int,
    val effectiveElapsedSec: Int,
    val pausedElapsedSec: Int,
    val plannedRestSec: Int,
    val actualRestSec: Int,
    val extraRestSec: Int,
    val timedCount: Int,
    val strengthCount: Int,
    val followAlongCount: Int
)

internal data class WorkoutRecordStatsUiState(
    val title: String,
    val description: String,
    val rows: List<HistorySummaryRowUiState>
)

internal data class WorkoutAggregateChartsUiState(
    val title: String,
    val description: String,
    val pointCount: Int,
    val countTrend: AggregateTrendChartUiState,
    val statusTrend: AggregateTrendChartUiState,
    val elapsedTrend: AggregateTrendChartUiState,
    val restTrend: AggregateTrendChartUiState,
    val modeBreakdown: ModeBreakdownChartUiState
)

internal data class AggregateTrendChartUiState(
    val title: String,
    val description: String,
    val dateLabels: List<String>,
    val series: List<AggregateTrendSeriesUiState>,
    val emptyMessage: String? = null,
    val xAxisLabel: String = "X 轴：startedAt 日期",
    val yAxisLabel: String = "Y 轴：数值",
    val unitLabel: String = "",
    val stateLabel: String? = null
) {
    val hasDrawableTrend: Boolean
        get() = emptyMessage == null && dateLabels.size >= 2 && series.any { trendSeries ->
            trendSeries.points.size >= 2
        }

    val legendRows: List<AggregateTrendLegendRowUiState>
        get() = series.map { trendSeries ->
            AggregateTrendLegendRowUiState(
                label = trendSeries.label,
                latestValue = trendSeries.points.lastOrNull()?.valueLabel.orEmpty()
            )
        }

    val yAxisTickLabels: List<String>
        get() {
            val maxValue = series.flatMap { trendSeries -> trendSeries.points }
                .maxOfOrNull { point -> point.value }
                ?.coerceAtLeast(0)
                ?: 0
            if (maxValue == 0) return listOf("0$unitLabel")
            val midpoint = maxValue / 2
            return listOf(
                "0$unitLabel",
                midpoint.axisValueLabel(unitLabel),
                maxValue.axisValueLabel(unitLabel)
            )
        }

    val xAxisTickLabels: List<String>
        get() = when {
            dateLabels.isEmpty() -> emptyList()
            dateLabels.size == 1 -> dateLabels
            else -> listOf(dateLabels.first(), dateLabels.last())
        }
}

internal data class AggregateTrendSeriesUiState(
    val label: String,
    val points: List<AggregateTrendPointUiState>
)

internal data class AggregateTrendPointUiState(
    val dateLabel: String,
    val value: Int,
    val valueLabel: String
)

internal data class AggregateTrendLegendRowUiState(
    val label: String,
    val latestValue: String
)

internal data class ModeBreakdownChartUiState(
    val title: String,
    val description: String,
    val totalCount: Int,
    val rows: List<ModeBreakdownRowUiState>,
    val emptyMessage: String? = null
)

internal data class ModeBreakdownRowUiState(
    val label: String,
    val count: Int,
    val percentLabel: String
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

internal data class TimedComparableRestTrendUiState(
    val title: String,
    val description: String,
    val groupingRows: List<TrendGroupingExplanationUiState>,
    val groups: List<TimedComparableRestTrendGroupUiState>,
    val dataQualityRows: List<TimedComparableRestDataQualityRowUiState>,
    val emptyMessage: String? = null
)

internal data class TimedComparableRestTrendGroupUiState(
    val title: String,
    val ruleLabel: String,
    val rows: List<TimedComparableRestTrendRowUiState>
)

internal data class TimedComparableRestTrendRowUiState(
    val dateLabel: String,
    val sessionTitle: String,
    val plannedRestLabel: String,
    val actualRestLabel: String,
    val extraRestLabel: String,
    val positionLabel: String
)

internal data class TimedComparableRestDataQualityRowUiState(
    val label: String,
    val helper: String
)

internal data class StrengthComparableSetTrendUiState(
    val title: String,
    val description: String,
    val groupingRows: List<TrendGroupingExplanationUiState>,
    val groups: List<StrengthComparableSetTrendGroupUiState>,
    val dataQualityRows: List<StrengthComparableSetDataQualityRowUiState>,
    val emptyMessage: String? = null
)

internal data class TrendGroupingExplanationUiState(
    val label: String,
    val helper: String
)

internal data class StrengthComparableSetTrendGroupUiState(
    val title: String,
    val ruleLabel: String,
    val rows: List<StrengthComparableSetTrendRowUiState>
)

internal data class StrengthComparableSetTrendRowUiState(
    val dateLabel: String,
    val sessionTitle: String,
    val plannedLabel: String,
    val actualLabel: String,
    val activeDurationLabel: String,
    val actualRestLabel: String,
    val effortLabel: String,
    val sourceLabel: String,
    val substitutionLabel: String?
)

internal data class StrengthComparableSetDataQualityRowUiState(
    val label: String,
    val helper: String
)

internal fun buildDefaultHistoryScreenState(): HistoryScreenState {
    return HistoryScreenState(
        sessions = defaultHistorySessions(),
        recordSource = HistoryRecordSource.EXAMPLE
    )
}

internal fun buildHistoryScreenState(sessions: List<WorkoutSession>): HistoryScreenState {
    return HistoryScreenState(sessions = sessions)
}

internal fun HistoryScreenState.selectSession(sessionId: String): HistoryScreenState {
    if (sessions.none { session -> session.id == sessionId }) return this
    return copy(selectedSessionId = sessionId)
}

internal fun HistoryScreenState.applyModeFilter(filter: HistoryModeFilter): HistoryScreenState {
    return copy(modeFilter = filter)
}

internal fun HistoryScreenState.applyStatusFilter(filter: HistoryStatusFilter): HistoryScreenState {
    return copy(statusFilter = filter)
}

internal fun HistoryScreenState.requestCleanup(target: HistoryCleanupTarget): HistoryScreenState {
    if (recordSource != HistoryRecordSource.PERSISTED || sessions.isEmpty()) return this
    return copy(pendingCleanupTarget = target, statusMessage = null)
}

internal fun HistoryScreenState.cancelCleanup(): HistoryScreenState {
    return copy(pendingCleanupTarget = null)
}

internal fun HistoryScreenState.confirmCleanup(): HistoryCleanupConfirmationResult {
    val target = pendingCleanupTarget
    return HistoryCleanupConfirmationResult(
        state = copy(
            pendingCleanupTarget = null,
            statusMessage = if (target == null) {
                statusMessage
            } else {
                "已提交历史清理请求；记录页会按本地 Room 数据自动刷新。"
            }
        ),
        target = target
    )
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
        statusTone = status.statusTone,
        durationLabel = durationSec().formatDuration(),
        keySummary = keySummary(),
        flags = sessionFlags(),
        selected = selected
    )
}

private fun HistoryScreenState.toFiltersUiState(): HistoryFiltersUiState {
    val filteredCount = filteredSessions.size
    val modeOptions = HistoryModeFilter.entries.map { filter ->
        HistoryModeFilterOptionUiState(
            filter = filter,
            label = filter.label,
            helper = filter.helper,
            count = sessions.count { session -> filter.matches(session) },
            selected = filter == modeFilter
        )
    }
    val statusOptions = HistoryStatusFilter.entries.map { filter ->
        HistoryStatusFilterOptionUiState(
            filter = filter,
            label = filter.label,
            helper = filter.helper,
            count = sessions.count { session -> modeFilter.matches(session) && filter.matches(session) },
            selected = filter == statusFilter
        )
    }
    return HistoryFiltersUiState(
        title = "筛选",
        description = "筛选只影响最近训练、选中详情和下方趋势；概览摘要始终展示全部本地记录。",
        modeOptions = modeOptions,
        statusOptions = statusOptions,
        resultLabel = "当前筛选 $filteredCount / ${sessions.size} 条记录",
        emptyMessage = if (sessions.isNotEmpty() && filteredCount == 0) {
            "当前筛选没有匹配训练；不会补假记录或假趋势。"
        } else {
            null
        }
    )
}

private fun WorkoutSession.toDetailState(source: HistoryRecordSource): HistorySessionDetailUiState {
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
        sourceNote = when (source) {
            HistoryRecordSource.PERSISTED -> "当前详情来自本地 session record；计划快照只用于展示当时结构。"
            HistoryRecordSource.EXAMPLE -> "当前详情来自 preview / 测试示例记录；生产记录页优先读取本地 session records。"
        },
        rows = summaryRows
    )
}

private fun WorkoutSession.timedDetailRows(): List<HistorySummaryRowUiState> {
    val completedSteps = stepHistory.count { record -> !record.skipped }
    val skippedSteps = stepHistory.count { record -> record.skipped }
    val plannedSteps = planSnapshot.plannedTimedStepCount()
    val baseRows = listOf(
        HistorySummaryRowUiState("总用时", durationSec().formatDuration(), "来自 totalElapsedSec；缺失时回退 step records"),
        HistorySummaryRowUiState("有效训练时间", effectiveDurationSec().formatDuration(), "来自 effectiveElapsedSec；不包含暂停时间"),
        HistorySummaryRowUiState("暂停时间", pausedDurationSec().formatDuration(), "来自 pausedElapsedSec；不包含额外休息"),
        HistorySummaryRowUiState("计划休息", plannedRestSec().formatDuration(), "来自本次训练保存的 plan snapshot"),
        HistorySummaryRowUiState("实际休息", actualRestSec().formatDuration(), "来自 timed rest step records"),
        HistorySummaryRowUiState(
            "额外休息",
            extraRestSec().formatDuration(),
            "来自 timedRestExtensionRecords.addedSec，不计入暂停时间"
        ),
        HistorySummaryRowUiState("完成步骤", "$completedSteps / ${plannedSteps.coerceAtLeast(completedSteps + skippedSteps)}", "包含热身、拉伸或计时步骤记录"),
        HistorySummaryRowUiState("跳过内容", "$skippedSteps 步", if (skippedSteps == 0) "没有跳过内容" else "仅记录跳过事实，不做表现判断")
    )
    return baseRows + timedLegacyDetailRows() + timedCompositionDetailRows()
}

private fun WorkoutSession.timedLegacyDetailRows(): List<HistorySummaryRowUiState> {
    val legacyBlocks = planSnapshot.blocks.filterIsInstance<TimedCircuitBlock>()
    if (legacyBlocks.isEmpty()) return emptyList()
    val circuitCount = legacyBlocks.size
    val workStepCount = legacyBlocks.sumOf { block ->
        block.items.count { item -> item.stageType != TimedStageType.REST && item.workDurationSec > 0 } *
            block.rounds.coerceAtLeast(0)
    }
    val restStepCount = legacyBlocks.sumOf { block ->
        val itemRestCount = block.items.count { item ->
            item.stageType == TimedStageType.REST && item.workDurationSec > 0 ||
                item.stageType != TimedStageType.REST && (item.restAfterSec ?: 0) > 0
        } * block.rounds.coerceAtLeast(0)
        val roundRestCount = if ((block.restBetweenRoundsSec ?: 0) > 0) {
            (block.rounds - 1).coerceAtLeast(0)
        } else {
            0
        }
        itemRestCount + roundRestCount
    }
    val structure = legacyBlocks.joinToString("；") { block ->
        val stages = block.items.joinToString(" / ") { item ->
            val rest = item.restAfterSec?.takeIf { sec -> sec > 0 }?.let { sec -> " + 休息 ${sec.formatDuration()}" }.orEmpty()
            "${item.stageTitle()} ${item.workDurationSec.formatDuration()}$rest"
        }
        "${block.rounds} 轮 · $stages"
    }
    return listOf(
        HistorySummaryRowUiState(
            label = "legacy timed 解释",
            value = "$circuitCount 个 TimedCircuitBlock · work $workStepCount · rest $restStepCount",
            helper = "按 legacy step/rest 顺序解释：$structure"
        )
    )
}

private data class TimedCompositionExpandedBlock(
    val block: TimedCompositionBlock,
    val steps: List<TimedCompositionTimelineStep>
)

private fun WorkoutSession.timedCompositionDetailRows(): List<HistorySummaryRowUiState> {
    val expandedBlocks = planSnapshot.timedCompositionExpandedBlocks()
    if (expandedBlocks.isEmpty()) return emptyList()

    val steps = expandedBlocks.flatMap { block -> block.steps }
    val stageGroupCount = expandedBlocks.sumOf { expanded -> expanded.block.stageGroups.size }
    val targetCount = expandedBlocks.sumOf { expanded ->
        expanded.block.stageGroups.sumOf { group -> group.targets.size }
    }
    val targetSummary = expandedBlocks
        .flatMap { expanded ->
            expanded.block.stageGroups.map { group ->
                val targetLabels = group.targets.joinToString(" / ") { target ->
                    "${target.name}(${target.id}:${target.kind.contractValue})"
                }
                "${group.name}(${group.id})：$targetLabels"
            }
        }
        .joinToString("；")
    val boundaryRestSteps = steps.filter { step ->
        step.timelineStageKind == TimedCompositionTimelineStageKind.BETWEEN_ROUND_REST
    }
    val extensionSummary = timedRestExtensionRecords.toTimedCompositionRestExtensionSummary(
        descriptors = planSnapshot.toTimedComparableRestDescriptors()
            .filter { descriptor -> descriptor.key.family == TimedTrendKeyFamily.TIMED_COMPOSITION }
    )

    return buildList {
        add(
            HistorySummaryRowUiState(
                label = "v2 编排",
                value = "composition v2 · ${steps.size} 步",
                helper = "从历史 planSnapshot 展开；stageGroup $stageGroupCount 个，target $targetCount 个，不用当前可编辑计划反推旧记录。"
            )
        )
        add(
            HistorySummaryRowUiState(
                label = "v2 阶段 / 目标",
                value = targetSummary.ifBlank { "暂无 stageGroup target" },
                helper = "按 stageGroupId、targetId 和 targetKind 解释阶段内部目标。"
            )
        )
        if (boundaryRestSteps.isNotEmpty()) {
            add(
                HistorySummaryRowUiState(
                    label = "v2 boundary rest",
                    value = boundaryRestSteps.sumOf { step -> step.plannedDurationSec }.formatDuration(),
                    helper = boundaryRestSteps.joinToString("；") { step ->
                        "round ${step.roundIndex} · ${step.timelineStageKind.contractValue} · target ${step.targetId} · planned ${step.plannedDurationSec.formatDuration()}"
                    }
                )
            )
        }
        extensionSummary?.let { summary ->
            add(summary)
        }
    }
}

private fun WorkoutPlanSnapshot.timedCompositionExpandedBlocks(): List<TimedCompositionExpandedBlock> {
    return blocks.sortedBy { block -> block.order }
        .filterIsInstance<TimedCompositionBlock>()
        .mapNotNull { block ->
            val steps = block.expandedCompositionStepsOrEmpty()
            if (steps.isEmpty()) {
                null
            } else {
                TimedCompositionExpandedBlock(block = block, steps = steps)
            }
        }
}

private fun List<TimedRestExtensionRecord>.toTimedCompositionRestExtensionSummary(
    descriptors: List<TimedComparableRestDescriptor>
): HistorySummaryRowUiState? {
    if (isEmpty() || descriptors.isEmpty()) return null
    val descriptorByStepId = descriptors.associateBy { descriptor -> descriptor.stepId }
    val mapped = mapNotNull { record ->
        val descriptor = descriptorByStepId[record.stepId] ?: return@mapNotNull null
        if (record.stepIndex != descriptor.stepIndex) return@mapNotNull null
        if (record.roundIndex != descriptor.roundIndex) return@mapNotNull null
        if (record.restStageId != descriptor.restStageId) return@mapNotNull null
        val descriptorPrevious = descriptor.previousStageId
        if (descriptorPrevious != null && record.previousStageId != descriptorPrevious) return@mapNotNull null
        record to descriptor
    }
    if (mapped.isEmpty()) return null

    return HistorySummaryRowUiState(
        label = "v2 额外休息定位",
        value = mapped.sumOf { pair -> pair.first.addedSec.coerceAtLeast(0) }.formatDuration(),
        helper = mapped.joinToString("；") { (record, descriptor) ->
            val family = descriptor.key.timelineStageKind ?: descriptor.key.targetKind.orEmpty()
            "${descriptor.restStageTitle} +${record.addedSec.formatDuration()} -> target ${descriptor.restStageId} · $family"
        }
    )
}

private fun WorkoutSession.strengthDetailRows(): List<HistorySummaryRowUiState> {
    val plannedSets = planSnapshot.blocks.filterIsInstance<StrengthExerciseBlock>().sumOf { block -> block.sets.size }
    val totalReps = strengthSetRecords.sumOf { record -> record.actualReps ?: 0 }
    val totalLoad = strengthSetRecords.totalLoadKg()
    val replaced = strengthSetRecords.count { record -> record.substitutedFromExerciseId != null }
    val actionSummary = strengthSetRecords.toStrengthActionSummary()
    return listOf(
        HistorySummaryRowUiState("总用时", durationSec().formatDuration(), "来自 totalElapsedSec；缺失时回退组记录"),
        HistorySummaryRowUiState("有效训练时间", effectiveDurationSec().formatDuration(), "来自 effectiveElapsedSec；力量训练不把暂停计入有效时间"),
        HistorySummaryRowUiState("暂停时间", pausedDurationSec().formatDuration(), "来自 pausedElapsedSec，和实际休息分开记录"),
        HistorySummaryRowUiState("计划休息", plannedRestSec().formatDuration(), "来自本次训练保存的 plan snapshot"),
        HistorySummaryRowUiState("实际休息", actualRestSec().formatDuration(), "来自 strength set records 的 actualRestAfterSec"),
        HistorySummaryRowUiState("确认组数", "${strengthSetRecords.size} / $plannedSets", "计划值和实际值保持区分"),
        HistorySummaryRowUiState("动作与组", actionSummary.first, actionSummary.second),
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
            val extra = extraRestSec().takeIf { sec -> sec > 0 }?.let { sec -> " · 额外休息 +${sec.formatDuration()}" }.orEmpty()
            "完成 $completed 步 · 跳过 $skipped 步$extra"
        }

        WorkoutMode.STRENGTH -> {
            val setCount = strengthSetRecords.size
            val reps = strengthSetRecords.sumOf { record -> record.actualReps ?: 0 }
            val replaced = strengthSetRecords.count { record -> record.substitutedFromExerciseId != null }
            val rest = actualRestSec().takeIf { sec -> sec > 0 }?.let { sec -> " · 休息 ${sec.formatDuration()}" }.orEmpty()
            "确认 $setCount 组 · $reps 次 · 替换 $replaced 组$rest"
        }

        WorkoutMode.FOLLOW_ALONG -> "跟练历史后续接入"
    }
}

private fun WorkoutSession.sessionFlags(): List<HistorySessionFlagUiState> {
    return buildList {
        add(HistorySessionFlagUiState(status.statusLabel, status.statusTone))
        val skippedSteps = stepHistory.count { record -> record.skipped }
        if (skippedSteps > 0) {
            add(HistorySessionFlagUiState("跳过 $skippedSteps 步", HistoryTone.WARNING))
        }
        val pausedSec = pausedDurationSec()
        if (pausedSec > 0) {
            add(HistorySessionFlagUiState("暂停 ${pausedSec.formatDuration()}", HistoryTone.INFO))
        }
        val extraRestSec = extraRestSec()
        if (extraRestSec > 0) {
            add(HistorySessionFlagUiState("额外休息 +${extraRestSec.formatDuration()}", HistoryTone.INFO))
        }
        val actualRestSec = actualRestSec()
        if (actualRestSec > 0) {
            add(HistorySessionFlagUiState("实际休息 ${actualRestSec.formatDuration()}", HistoryTone.NEUTRAL))
        }
    }
}

private fun List<StrengthSetRecord>.toStrengthActionSummary(): Pair<String, String> {
    if (isEmpty()) return "暂无确认组" to "没有 strength set records；不补假动作、重量、次数或休息。"
    val grouped = groupBy { record -> record.exerciseId }
    val value = grouped.entries.joinToString("；") { (exerciseId, records) ->
        "${exerciseId.exerciseName()} ${records.size} 组"
    }
    val helper = take(4).joinToString("；") { record ->
        val weight = record.actualWeight.formatWeight()
        val reps = record.actualReps?.let { reps -> "$reps 次" } ?: "未记录次数"
        val rest = record.actualRestAfterSec?.let { sec -> "休息 ${sec.formatDuration()}" } ?: "休息未记录"
        "第 ${record.setOrder} 组 $weight · $reps · $rest"
    }
    return value to helper
}

private fun List<WorkoutSession>.toWorkoutRecordStats(): WorkoutRecordStats {
    return WorkoutRecordStats(
        totalCount = size,
        completedCount = count { session -> session.status == SessionStatus.COMPLETED },
        abandonedCount = count { session -> session.status == SessionStatus.ABANDONED },
        totalElapsedSec = sumOf { session -> session.durationSec() },
        effectiveElapsedSec = sumOf { session -> session.effectiveDurationSec() },
        pausedElapsedSec = sumOf { session -> session.pausedDurationSec() },
        plannedRestSec = sumOf { session -> session.plannedRestSec() },
        actualRestSec = sumOf { session -> session.actualRestSec() },
        extraRestSec = sumOf { session -> session.extraRestSec() },
        timedCount = count { session -> session.mode == WorkoutMode.TIMED },
        strengthCount = count { session -> session.mode == WorkoutMode.STRENGTH },
        followAlongCount = count { session -> session.mode == WorkoutMode.FOLLOW_ALONG }
    )
}

private fun WorkoutRecordStats.toUiState(): WorkoutRecordStatsUiState {
    return WorkoutRecordStatsUiState(
        title = "真实记录基础统计",
        description = "从本地 Room session records 汇总；只做总量和 mode breakdown，不做结论性判断。",
        rows = listOf(
            HistorySummaryRowUiState(
                "训练总次数",
                "$totalCount 次",
                "仅统计真实持久化记录；completed 和 abandoned 分开显示"
            ),
            HistorySummaryRowUiState("completed", "$completedCount 次", "正常完成的 session 数"),
            HistorySummaryRowUiState("abandoned", "$abandonedCount 次", "提前结束的 session 数；仍参与时长、暂停和额外休息统计"),
            HistorySummaryRowUiState("总用时", totalElapsedSec.formatDuration(), "汇总 totalElapsedSec；缺失时回退实际记录时长"),
            HistorySummaryRowUiState("有效训练时间", effectiveElapsedSec.formatDuration(), "汇总 effectiveElapsedSec，不包含暂停"),
            HistorySummaryRowUiState("暂停时间", pausedElapsedSec.formatDuration(), "汇总 pausedElapsedSec，和额外休息分开"),
            HistorySummaryRowUiState("计划休息", plannedRestSec.formatDuration(), "来自每次训练保存的 plan snapshot"),
            HistorySummaryRowUiState("实际休息", actualRestSec.formatDuration(), "来自 timed rest steps 与 strength actual rests"),
            HistorySummaryRowUiState("计时额外休息", extraRestSec.formatDuration(), "来自 timedRestExtensionRecords.addedSec"),
            HistorySummaryRowUiState(
                "模式分布",
                "计时 $timedCount · 力量 $strengthCount · 跟练 $followAlongCount",
                "只做基础 mode breakdown，不比较不可比计划或阶段"
            )
        )
    )
}

private fun WorkoutRecordStats.toOverviewUiState(): HistoryOverviewUiState {
    return HistoryOverviewUiState(
        title = "概览摘要",
        description = "全部本地训练记录的轻量总览；completed / abandoned、有效时间、暂停和额外休息分开显示。",
        metrics = listOf(
            HistoryOverviewMetricUiState("记录", "$totalCount 次", "全部本地 WorkoutSession"),
            HistoryOverviewMetricUiState("completed", "$completedCount 次", "正常完成", HistoryTone.SUCCESS),
            HistoryOverviewMetricUiState("abandoned", "$abandonedCount 次", "提前结束仍可回顾", HistoryTone.WARNING),
            HistoryOverviewMetricUiState("总用时", totalElapsedSec.formatDuration(), "totalElapsedSec"),
            HistoryOverviewMetricUiState("有效", effectiveElapsedSec.formatDuration(), "effectiveElapsedSec"),
            HistoryOverviewMetricUiState("暂停", pausedElapsedSec.formatDuration(), "pausedElapsedSec", HistoryTone.INFO),
            HistoryOverviewMetricUiState("计划休息", plannedRestSec.formatDuration(), "历史 planSnapshot"),
            HistoryOverviewMetricUiState("实际休息", actualRestSec.formatDuration(), "step / set records"),
            HistoryOverviewMetricUiState("额外休息", extraRestSec.formatDuration(), "timedRestExtensionRecords", HistoryTone.INFO)
        )
    )
}

private fun List<WorkoutSession>.toWorkoutAggregateChartsUiState(): WorkoutAggregateChartsUiState {
    val daily = toDailyAggregates()
    val notEnoughMessage = "暂无趋势；至少需要 2 个 startedAt 日期点，当前不绘制假曲线。"
    val countTrend = daily.toTrendChart(
        title = "训练总次数趋势",
        description = "按 startedAt 日期聚合真实 WorkoutSession 数量。",
        notEnoughMessage = notEnoughMessage,
        yAxisLabel = "Y 轴：训练次数",
        unitLabel = "次",
        series = listOf("训练总次数" to { aggregate -> aggregate.totalCount })
    ) { value -> "$value 次" }
    val statusTrend = daily.toTrendChart(
        title = "完成 / 提前结束趋势",
        description = "completed 和 abandoned 分开显示，不合并为单一状态。",
        notEnoughMessage = notEnoughMessage,
        yAxisLabel = "Y 轴：记录数",
        unitLabel = "次",
        series = listOf(
            "completed" to { aggregate -> aggregate.completedCount },
            "abandoned" to { aggregate -> aggregate.abandonedCount }
        )
    ) { value -> "$value 次" }
    val elapsedTrend = daily.toTrendChart(
        title = "用时趋势",
        description = "totalElapsedSec、effectiveElapsedSec 和 pausedElapsedSec 分开统计。",
        notEnoughMessage = notEnoughMessage,
        yAxisLabel = "Y 轴：时间",
        unitLabel = "秒",
        series = listOf(
            "总用时" to { aggregate -> aggregate.totalElapsedSec },
            "有效训练时间" to { aggregate -> aggregate.effectiveElapsedSec },
            "暂停时间" to { aggregate -> aggregate.pausedElapsedSec }
        )
    ) { value -> value.formatDuration() }
    val restTrend = daily.toTrendChart(
        title = "休息趋势",
        description = "planned rest 来自历史 planSnapshot；actual rest 来自执行记录；extra rest 独立显示。",
        notEnoughMessage = notEnoughMessage,
        yAxisLabel = "Y 轴：休息时间",
        unitLabel = "秒",
        series = listOf(
            "计划休息" to { aggregate -> aggregate.plannedRestSec },
            "实际休息" to { aggregate -> aggregate.actualRestSec },
            "额外休息" to { aggregate -> aggregate.extraRestSec }
        )
    ) { value -> value.formatDuration() }
    return WorkoutAggregateChartsUiState(
        title = "记录图表与聚合趋势",
        description = "只消费真实持久化 session list；统计和图表不回写历史计划、session 或 plan snapshot。",
        pointCount = daily.size,
        countTrend = countTrend,
        statusTrend = statusTrend,
        elapsedTrend = elapsedTrend,
        restTrend = restTrend,
        modeBreakdown = toModeBreakdownChart()
    )
}

private fun List<WorkoutSession>.toTimedComparableRestTrendUiState(): TimedComparableRestTrendUiState {
    val timedSessions = filter { session ->
        session.mode == WorkoutMode.TIMED && session.planSnapshot.mode == WorkoutMode.TIMED
    }
    val samples = timedSessions.flatMap { session -> session.toTimedComparableRestSamples() }
    val comparableGroups = samples
        .groupBy { sample -> sample.key }
        .values
        .filter { group -> group.size >= 2 }
        .sortedWith(
            compareBy<List<TimedComparableRestSample>>(
                { group -> group.first().key.family.ordinal },
                { group -> group.first().descriptor.structureSignature },
                { group -> group.first().descriptor.roundIndex ?: 0 },
                { group -> group.first().key.stageGroupIndex ?: 0 },
                { group -> group.first().key.targetIndex ?: 0 },
                { group -> group.first().descriptor.stepIndex }
            )
        )
        .map { group -> group.toTimedComparableRestTrendGroup() }
    val fallbackRows = toTimedComparableRestDataQualityRows(
        sampleCount = samples.size,
        comparableGroupCount = comparableGroups.size
    )
    return TimedComparableRestTrendUiState(
        title = "计时同类阶段与额外休息趋势",
        description = "legacy timed 与 composition v2 使用分离 trend key；只比较同一历史计划结构下的计时休息阶段，planned rest 来自 planSnapshot，actual rest 来自 step records，extra rest 只来自 timedRestExtensionRecords.addedSec。",
        groupingRows = listOf(
            TrendGroupingExplanationUiState(
                label = TimedTrendKeyFamily.LEGACY_TIMED.contractValue,
                helper = "legacy TimedCircuitBlock / TimedExerciseItem 只按 legacy step/rest、轮次、顺序和前序阶段比较。"
            ),
            TrendGroupingExplanationUiState(
                label = TimedTrendKeyFamily.TIMED_COMPOSITION.contractValue,
                helper = "composition v2 先由 TimedCompositionTimelineAdapter 从历史 planSnapshot 展开 stageGroup / target / boundary rest，再按 v2 key 比较；不与 legacy overlay。"
            )
        ),
        groups = comparableGroups,
        dataQualityRows = fallbackRows,
        emptyMessage = if (comparableGroups.isEmpty()) {
            "暂无可比计时阶段趋势；至少需要 2 条同一 family、同一计划结构、同一阶段类型、同一顺序、同一轮次和同一休息关系的真实计时记录。"
        } else {
            null
        }
    )
}

private data class TimedComparableRestSample(
    val key: TimedComparableRestKey,
    val session: WorkoutSession,
    val descriptor: TimedComparableRestDescriptor,
    val actualRestSec: Int,
    val extraRestSec: Int
)

private enum class TimedTrendKeyFamily(val contractValue: String) {
    LEGACY_TIMED("legacy_timed"),
    TIMED_COMPOSITION("timed_composition_v2")
}

private data class TimedComparableRestKey(
    val family: TimedTrendKeyFamily,
    val structureSignature: String,
    val plannedDurationSec: Int,
    val restStageId: String,
    val previousStageId: String?,
    val stepIndex: Int,
    val legacyStageType: TimedStageType? = null,
    val legacyStageOrder: Int? = null,
    val roundIndex: Int? = null,
    val compositionVersion: Int? = null,
    val compositionBlockId: String? = null,
    val timelineStageKind: String? = null,
    val stageGroupId: String? = null,
    val targetId: String? = null,
    val targetKind: String? = null,
    val stageGroupIndex: Int? = null,
    val targetIndex: Int? = null,
    val stageInstanceIndex: Int? = null,
    val targetInstanceIndex: Int? = null
)

private data class TimedComparableRestDescriptor(
    val key: TimedComparableRestKey,
    val structureSignature: String,
    val stepId: String,
    val stepIndex: Int,
    val roundIndex: Int?,
    val restStageId: String,
    val restStageTitle: String,
    val previousStageId: String?,
    val previousStageTitle: String,
    val plannedRestSec: Int
)

private fun WorkoutSession.toTimedComparableRestSamples(): List<TimedComparableRestSample> {
    val descriptors = planSnapshot.toTimedComparableRestDescriptors()
    val stepRecordsById = stepHistory
        .filter { record -> record.kind == SessionStepKind.TIMED_REST }
        .associateBy { record -> record.stepId }
    val validExtraRecords = timedRestExtensionRecords.filter { record ->
        record.hasComparableRestKey()
    }
    return descriptors.mapNotNull { descriptor ->
        val stepRecord = stepRecordsById[descriptor.stepId] ?: return@mapNotNull null
        val actualRestSec = stepRecord.actualDurationSec?.coerceAtLeast(0) ?: return@mapNotNull null
        val extraRestSec = validExtraRecords
            .filter { record ->
                record.stepId == descriptor.stepId &&
                    record.stepIndex == descriptor.stepIndex &&
                    record.roundIndex == descriptor.roundIndex &&
                    record.restStageId == descriptor.restStageId &&
                    record.previousStageId == descriptor.previousStageId
            }
            .sumOf { record -> record.addedSec.coerceAtLeast(0) }
        TimedComparableRestSample(
            key = descriptor.key,
            session = this,
            descriptor = descriptor,
            actualRestSec = actualRestSec,
            extraRestSec = extraRestSec
        )
    }
}

private fun WorkoutPlanSnapshot.toTimedComparableRestDescriptors(): List<TimedComparableRestDescriptor> {
    val structureSignature = timedStructureSignature()
    val descriptors = mutableListOf<TimedComparableRestDescriptor>()
    var stepIndex = 0
    blocks.sortedBy { block -> block.order }.forEach { block ->
        when (block) {
            is RestBlock -> {
                if (block.durationSec > 0) stepIndex += 1
            }
            is TimedCircuitBlock -> {
                if (block.rounds > 0) {
                    (1..block.rounds).forEach { round ->
                        block.items.forEachIndexed { itemIndex, item ->
                            if (item.stageType == TimedStageType.REST) {
                                if (item.workDurationSec > 0) {
                                    timedRestDescriptorOrNull(
                                        structureSignature = structureSignature,
                                        stepId = "${block.id}-r$round-${item.id}-rest",
                                        stepIndex = stepIndex,
                                        roundIndex = round,
                                        stageOrder = itemIndex,
                                        restStageId = item.id,
                                        restStageTitle = item.stageTitle(),
                                        previousStage = block.items.take(itemIndex)
                                            .lastOrNull { previous -> previous.stageType != TimedStageType.REST },
                                        plannedRestSec = item.workDurationSec
                                    )?.let { descriptor -> descriptors += descriptor }
                                    stepIndex += 1
                                }
                            } else {
                                if (item.workDurationSec > 0) stepIndex += 1
                                val restAfterSec = item.restAfterSec ?: 0
                                if (restAfterSec > 0) {
                                    timedRestDescriptorOrNull(
                                        structureSignature = structureSignature,
                                        stepId = "${block.id}-r$round-${item.id}-rest",
                                        stepIndex = stepIndex,
                                        roundIndex = round,
                                        stageOrder = itemIndex,
                                        restStageId = item.id,
                                        restStageTitle = "休息",
                                        previousStage = item,
                                        plannedRestSec = restAfterSec
                                    )?.let { descriptor -> descriptors += descriptor }
                                    stepIndex += 1
                                }
                            }
                        }
                        val roundRestSec = block.restBetweenRoundsSec ?: 0
                        if (round < block.rounds && roundRestSec > 0) {
                            val previousStage = block.items.lastOrNull { item -> item.stageType != TimedStageType.REST }
                            timedRestDescriptorOrNull(
                                structureSignature = structureSignature,
                                stepId = "${block.id}-r$round-round-rest",
                                stepIndex = stepIndex,
                                roundIndex = round,
                                stageOrder = block.items.size,
                                restStageId = block.id,
                                restStageTitle = "轮间休息",
                                previousStage = previousStage,
                                plannedRestSec = roundRestSec
                            )?.let { descriptor -> descriptors += descriptor }
                            stepIndex += 1
                        }
                    }
                }
            }
            is TimedCompositionBlock -> {
                val steps = block.expandedCompositionStepsOrEmpty()
                descriptors += steps.toTimedCompositionRestDescriptors(
                    structureSignature = structureSignature,
                    compositionBlockId = block.id,
                    baseStepIndex = stepIndex
                )
                stepIndex += steps.size
            }
            is com.liujyks.trainflow.core.model.WarmupBlock -> {
                stepIndex += block.timedStepCount()
            }
            is com.liujyks.trainflow.core.model.StretchBlock -> {
                stepIndex += block.timedStepCount()
            }
            is com.liujyks.trainflow.core.model.CooldownBlock -> {
                stepIndex += block.timedStepCount()
            }
            else -> Unit
        }
    }
    return descriptors
}

private fun TimedCompositionBlock.expandedCompositionStepsOrEmpty(): List<TimedCompositionTimelineStep> {
    return runCatching { TimedCompositionTimelineAdapter.expand(this).steps }.getOrDefault(emptyList())
}

private fun List<TimedCompositionTimelineStep>.toTimedCompositionRestDescriptors(
    structureSignature: String,
    compositionBlockId: String,
    baseStepIndex: Int
): List<TimedComparableRestDescriptor> {
    return mapIndexedNotNull { localIndex, step ->
        if (step.stepKind != TimedCompositionTimelineStepKind.REST) return@mapIndexedNotNull null
        val globalStepIndex = baseStepIndex + localIndex
        val previousWorkStep = take(localIndex).lastOrNull { candidate ->
            candidate.stepKind == TimedCompositionTimelineStepKind.WORK
        }
        val key = TimedComparableRestKey(
            family = TimedTrendKeyFamily.TIMED_COMPOSITION,
            structureSignature = structureSignature,
            plannedDurationSec = step.plannedDurationSec.coerceAtLeast(0),
            restStageId = step.targetId,
            previousStageId = previousWorkStep?.targetId,
            stepIndex = globalStepIndex,
            roundIndex = step.roundIndex,
            compositionVersion = step.compositionVersion,
            compositionBlockId = compositionBlockId,
            timelineStageKind = step.timelineStageKind.contractValue,
            stageGroupId = step.stageGroupId,
            targetId = step.targetId,
            targetKind = step.targetKind.contractValue,
            stageGroupIndex = step.stageGroupIndex,
            targetIndex = step.targetIndex,
            stageInstanceIndex = step.stageInstanceIndex,
            targetInstanceIndex = step.targetInstanceIndex
        )
        TimedComparableRestDescriptor(
            key = key,
            structureSignature = structureSignature,
            stepId = step.id,
            stepIndex = globalStepIndex,
            roundIndex = step.roundIndex,
            restStageId = step.targetId,
            restStageTitle = step.displayName,
            previousStageId = previousWorkStep?.targetId,
            previousStageTitle = previousWorkStep?.displayName ?: "前序阶段不足",
            plannedRestSec = step.plannedDurationSec.coerceAtLeast(0)
        )
    }
}

private fun timedRestDescriptorOrNull(
    structureSignature: String,
    stepId: String,
    stepIndex: Int,
    roundIndex: Int,
    stageOrder: Int,
    restStageId: String,
    restStageTitle: String,
    previousStage: TimedExerciseItem?,
    plannedRestSec: Int
): TimedComparableRestDescriptor? {
    val previousStageId = previousStage?.id ?: return null
    val previousStageTitle = previousStage.stageTitle()
    val key = TimedComparableRestKey(
        family = TimedTrendKeyFamily.LEGACY_TIMED,
        structureSignature = structureSignature,
        plannedDurationSec = plannedRestSec.coerceAtLeast(0),
        restStageId = restStageId,
        previousStageId = previousStageId,
        stepIndex = stepIndex,
        legacyStageType = TimedStageType.REST,
        legacyStageOrder = stageOrder,
        roundIndex = roundIndex
    )
    return TimedComparableRestDescriptor(
        key = key,
        structureSignature = structureSignature,
        stepId = stepId,
        stepIndex = stepIndex,
        roundIndex = roundIndex,
        restStageId = restStageId,
        restStageTitle = restStageTitle,
        previousStageId = previousStageId,
        previousStageTitle = previousStageTitle,
        plannedRestSec = plannedRestSec.coerceAtLeast(0)
    )
}

private fun TimedRestExtensionRecord.hasComparableRestKey(): Boolean {
    return roundIndex != null &&
        !restStageId.isNullOrBlank() &&
        !previousStageId.isNullOrBlank() &&
        stepIndex >= 0
}

private fun List<WorkoutSession>.toTimedComparableRestDataQualityRows(
    sampleCount: Int,
    comparableGroupCount: Int
): List<TimedComparableRestDataQualityRowUiState> {
    val rows = mutableListOf<TimedComparableRestDataQualityRowUiState>()
    val timedCount = count { session -> session.mode == WorkoutMode.TIMED && session.planSnapshot.mode == WorkoutMode.TIMED }
    val nonTimedCount = size - timedCount
    if (nonTimedCount > 0) {
        rows += TimedComparableRestDataQualityRowUiState(
            label = "已排除非计时记录",
            helper = "$nonTimedCount 条 strength / follow_along 记录没有进入本计时阶段趋势。"
        )
    }
    val legacyTimedCount = count { session ->
        session.mode == WorkoutMode.TIMED &&
            session.planSnapshot.blocks.any { block -> block is TimedCircuitBlock }
    }
    val v2TimedCount = count { session ->
        session.mode == WorkoutMode.TIMED &&
            session.planSnapshot.blocks.any { block -> block is TimedCompositionBlock }
    }
    if (legacyTimedCount > 0 && v2TimedCount > 0) {
        rows += TimedComparableRestDataQualityRowUiState(
            label = "legacy / v2 趋势已隔离",
            helper = "$legacyTimedCount 条 legacy timed 与 $v2TimedCount 条 composition v2 默认不合并；没有等价 mapper 时不共享同一趋势 key。"
        )
    }
    val unsupportedV2Count = count { session ->
        session.mode == WorkoutMode.TIMED &&
            session.planSnapshot.blocks
                .filterIsInstance<TimedCompositionBlock>()
                .any { block -> block.expandedCompositionStepsOrEmpty().isEmpty() }
    }
    if (unsupportedV2Count > 0) {
        rows += TimedComparableRestDataQualityRowUiState(
            label = "v2 编排暂不可解释",
            helper = "$unsupportedV2Count 条 composition v2 snapshot 无法安全展开或没有可执行 step，保留记录但不进入可比趋势。"
        )
    }
    val malformedExtraRestCount = filter { session -> session.mode == WorkoutMode.TIMED }
        .sumOf { session -> session.timedRestExtensionRecords.count { record -> !record.hasComparableRestKey() } }
    if (malformedExtraRestCount > 0) {
        rows += TimedComparableRestDataQualityRowUiState(
            label = "额外休息位置数据不足",
            helper = "$malformedExtraRestCount 条 timedRestExtensionRecords 缺少 roundIndex、restStageId、previousStageId 或 stepIndex，已降级为不进入阶段级趋势。"
        )
    }
    if (timedCount > 0 && sampleCount == 0) {
        rows += TimedComparableRestDataQualityRowUiState(
            label = "暂无阶段级样本",
            helper = "现有计时记录缺少可匹配的 timed rest step records，暂不生成阶段级趋势。"
        )
    } else if (timedCount > 0 && comparableGroupCount == 0) {
        rows += TimedComparableRestDataQualityRowUiState(
            label = "样本不足",
            helper = "已找到 $sampleCount 个计时休息样本，但没有任一同类阶段达到 2 条真实记录。"
        )
    }
    return rows
}

private fun List<TimedComparableRestSample>.toTimedComparableRestTrendGroup(): TimedComparableRestTrendGroupUiState {
    val firstSample = first()
    val descriptor = firstSample.descriptor
    val rows = sortedBy { sample -> sample.session.startedAt.orEmpty() }
        .map { sample ->
            TimedComparableRestTrendRowUiState(
                dateLabel = sample.session.dateKey,
                sessionTitle = sample.session.planSnapshot.title,
                plannedRestLabel = sample.descriptor.plannedRestSec.formatDuration(),
                actualRestLabel = sample.actualRestSec.formatDuration(),
                extraRestLabel = sample.extraRestSec.formatDuration(),
                positionLabel = sample.descriptor.positionLabel()
            )
        }
    return TimedComparableRestTrendGroupUiState(
        title = descriptor.groupTitle(),
        ruleLabel = descriptor.ruleLabel(),
        rows = rows
    )
}

private fun TimedComparableRestDescriptor.groupTitle(): String {
    return when (key.family) {
        TimedTrendKeyFamily.LEGACY_TIMED -> {
            "轮 ${roundIndex ?: 0} · ${previousStageTitle} 后 $restStageTitle"
        }
        TimedTrendKeyFamily.TIMED_COMPOSITION -> {
            val roundLabel = roundIndex?.let { round -> "轮 $round · " }.orEmpty()
            if (key.timelineStageKind == TimedCompositionTimelineStageKind.BETWEEN_ROUND_REST.contractValue) {
                "composition v2 · ${roundLabel}轮间休息"
            } else {
                "composition v2 · ${roundLabel}${previousStageTitle} 后 $restStageTitle"
            }
        }
    }
}

private fun TimedComparableRestDescriptor.ruleLabel(): String {
    return when (key.family) {
        TimedTrendKeyFamily.LEGACY_TIMED ->
            "legacy timed · 同一结构签名 · 同一 REST 阶段 · 同一顺序 · 同一轮次 · 同一 rest/work 关系"

        TimedTrendKeyFamily.TIMED_COMPOSITION ->
            "composition v2 · 同一 compositionVersion / block / stageGroup / target / targetKind / round / index / plannedDuration / structure signature；不与 legacy timed 合并"
    }
}

private fun TimedComparableRestDescriptor.positionLabel(): String {
    return when (key.family) {
        TimedTrendKeyFamily.LEGACY_TIMED -> {
            "family ${key.family.contractValue} · round ${roundIndex ?: 0} · step $stepIndex · rest $restStageId · prev ${previousStageId.orEmpty()}"
        }
        TimedTrendKeyFamily.TIMED_COMPOSITION -> buildList {
            add("family ${key.family.contractValue}")
            add("compositionVersion ${key.compositionVersion}")
            add("block ${key.compositionBlockId}")
            add("stageKind ${key.timelineStageKind}")
            add("stageGroup ${key.stageGroupId}")
            add("target ${key.targetId}")
            add("targetKind ${key.targetKind}")
            add("round ${key.roundIndex ?: 0}")
            add("stageGroupIndex ${key.stageGroupIndex ?: 0}")
            add("targetIndex ${key.targetIndex ?: 0}")
            add("stageInstance ${key.stageInstanceIndex ?: 0}")
            add("targetInstance ${key.targetInstanceIndex ?: 0}")
            add("planned ${key.plannedDurationSec.formatDuration()}")
            add("signature ${key.structureSignature}")
        }.joinToString(" · ")
    }
}

private fun WorkoutPlanSnapshot.timedStructureSignature(): String {
    return blocks.sortedBy { block -> block.order }.joinToString("|") { block ->
        when (block) {
            is RestBlock -> "rest:${block.id}:${block.durationSec}"
            is TimedCircuitBlock -> {
                val items = block.items.joinToString(",") { item ->
                    "${item.id}:${item.stageType.contractValue}:${item.workDurationSec}:${item.restAfterSec ?: 0}"
                }
                "circuit:${block.id}:${block.rounds}:${block.restBetweenRoundsSec ?: 0}:$items"
            }
            is TimedCompositionBlock -> block.timedCompositionStructureSignature()
            is com.liujyks.trainflow.core.model.WarmupBlock -> "warmup:${block.id}:${block.durationSec ?: 0}:${block.items.timedItemsSignature()}"
            is com.liujyks.trainflow.core.model.StretchBlock -> "stretch:${block.id}:${block.durationSec ?: 0}:${block.items.timedItemsSignature()}"
            is com.liujyks.trainflow.core.model.CooldownBlock -> "cooldown:${block.id}:${block.durationSec ?: 0}:${block.items.timedItemsSignature()}"
            else -> "${block.kind.contractValue}:${block.id}"
        }
    }
}

private fun TimedCompositionBlock.timedCompositionStructureSignature(): String {
    val steps = expandedCompositionStepsOrEmpty()
    if (steps.isEmpty()) {
        return "composition_v2:$id:$compositionVersion:unsupported_or_empty"
    }
    return steps.joinToString(
        prefix = "composition_v2:$id:$compositionVersion:",
        separator = ","
    ) { step ->
        listOf(
            step.timelineStageKind.contractValue,
            step.stageGroupId,
            step.targetId,
            step.targetKind.contractValue,
            step.roundIndex ?: 0,
            step.stageGroupIndex ?: 0,
            step.targetIndex,
            step.stageInstanceIndex,
            step.targetInstanceIndex,
            step.plannedDurationSec
        ).joinToString(":")
    }
}

private fun List<TimedExerciseItem>.timedItemsSignature(): String {
    return joinToString(",") { item ->
        "${item.id}:${item.stageType.contractValue}:${item.workDurationSec}:${item.restAfterSec ?: 0}"
    }
}

private fun WorkoutPlanSnapshot.plannedTimedStepCount(): Int {
    return blocks.sumOf { block ->
        when (block) {
            is RestBlock -> if (block.durationSec > 0) 1 else 0
            is TimedCircuitBlock -> block.timedStepCount()
            is TimedCompositionBlock -> block.expandedCompositionStepsOrEmpty().size
            is com.liujyks.trainflow.core.model.WarmupBlock -> block.timedStepCount()
            is com.liujyks.trainflow.core.model.StretchBlock -> block.timedStepCount()
            is com.liujyks.trainflow.core.model.CooldownBlock -> block.timedStepCount()
            else -> 0
        }
    }
}

private fun TimedCircuitBlock.timedStepCount(): Int {
    val perRoundCount = items.timedStepCount()
    val betweenRoundCount = if ((restBetweenRoundsSec ?: 0) > 0) {
        (rounds - 1).coerceAtLeast(0)
    } else {
        0
    }
    return perRoundCount * rounds.coerceAtLeast(0) + betweenRoundCount
}

private fun List<TimedExerciseItem>.timedStepCount(): Int {
    return sumOf { item ->
        if (item.stageType == TimedStageType.REST) {
            if (item.workDurationSec > 0) 1 else 0
        } else {
            (if (item.workDurationSec > 0) 1 else 0) + (if ((item.restAfterSec ?: 0) > 0) 1 else 0)
        }
    }
}

private fun com.liujyks.trainflow.core.model.WarmupBlock.timedStepCount(): Int {
    return if (items.isNotEmpty()) items.timedStepCount() else if ((durationSec ?: 0) > 0) 1 else 0
}

private fun com.liujyks.trainflow.core.model.StretchBlock.timedStepCount(): Int {
    return if (items.isNotEmpty()) items.timedStepCount() else if ((durationSec ?: 0) > 0) 1 else 0
}

private fun com.liujyks.trainflow.core.model.CooldownBlock.timedStepCount(): Int {
    return if (items.isNotEmpty()) items.timedStepCount() else if ((durationSec ?: 0) > 0) 1 else 0
}

private fun TimedExerciseItem.stageTitle(): String {
    return labelOverride ?: exerciseId ?: stageType.displayName
}

private fun List<WorkoutSession>.toStrengthComparableSetTrendUiState(): StrengthComparableSetTrendUiState {
    val strengthSessions = filter { session ->
        session.mode == WorkoutMode.STRENGTH && session.planSnapshot.mode == WorkoutMode.STRENGTH
    }
    val samples = strengthSessions.flatMap { session -> session.toStrengthComparableSetSamples() }
    val comparableGroups = samples
        .groupBy { sample -> sample.key }
        .values
        .filter { group -> group.size >= 2 }
        .sortedWith(
            compareBy<List<StrengthComparableSetSample>>(
                { group -> group.first().key.exerciseId },
                { group -> group.first().key.sortSource },
                { group -> group.first().key.fallbackSetOrder ?: 0 },
                { group -> group.first().key.fallbackSetKind?.contractValue.orEmpty() }
            )
        )
        .map { group -> group.toStrengthComparableSetTrendGroup() }
    val dataQualityRows = toStrengthComparableSetDataQualityRows(
        sampleCount = samples.size,
        comparableGroupCount = comparableGroups.size
    )
    return StrengthComparableSetTrendUiState(
        title = "力量同类 set 趋势",
        description = "只比较同一 exerciseId 下的真实 strength set records；优先用 sourceSetPlanId，同类来源缺失时才降级到 setOrder + setKind。planned 与 actual 分开展示，不输出强弱判断或加重量建议。",
        groupingRows = listOf(
            TrendGroupingExplanationUiState(
                label = "strength_comparable_set",
                helper = "同一 exerciseId + sourceSetPlanId 优先；sourceSetPlanId 缺失时才用 setOrder + setKind，替换动作保持独立标注。"
            )
        ),
        groups = comparableGroups,
        dataQualityRows = dataQualityRows,
        emptyMessage = if (comparableGroups.isEmpty()) {
            "暂无可比力量 set 趋势；至少需要 2 条同一 exerciseId 且同一 sourceSetPlanId，或 sourceSetPlanId 缺失时同一 setOrder + setKind 的完整真实组记录。"
        } else {
            null
        }
    )
}

private data class StrengthComparableSetSample(
    val key: StrengthComparableSetKey,
    val session: WorkoutSession,
    val record: StrengthSetRecord,
    val plannedWeight: WeightValue,
    val plannedRepTarget: RepTarget,
    val actualWeight: WeightValue,
    val actualReps: Int,
    val activeDurationSec: Int,
    val actualRestAfterSec: Int,
    val effort: SetEffort
)

private data class StrengthComparableSetKey(
    val exerciseId: String,
    val sourceSetPlanId: String?,
    val fallbackSetOrder: Int?,
    val fallbackSetKind: StrengthSetKind?
) {
    val sortSource: String
        get() = sourceSetPlanId ?: "fallback"

    val comparisonRuleLabel: String
        get() = if (sourceSetPlanId != null) {
            "同一 exerciseId · 同一 sourceSetPlanId"
        } else {
            "同一 exerciseId · sourceSetPlanId 缺失后降级为同一 setOrder + setKind"
        }
}

private data class StrengthPlannedSetValues(
    val plannedWeight: WeightValue?,
    val plannedRepTarget: RepTarget?
)

private fun WorkoutSession.toStrengthComparableSetSamples(): List<StrengthComparableSetSample> {
    return strengthSetRecords.mapNotNull { record ->
        val key = record.toStrengthComparableSetKeyOrNull() ?: return@mapNotNull null
        val plannedValues = planSnapshot.plannedValuesFor(record)
        val plannedWeight = record.plannedWeight ?: plannedValues.plannedWeight ?: return@mapNotNull null
        val plannedRepTarget = record.plannedRepTarget ?: plannedValues.plannedRepTarget ?: return@mapNotNull null
        val actualWeight = record.actualWeight ?: return@mapNotNull null
        val actualReps = record.actualReps?.takeIf { reps -> reps >= 0 } ?: return@mapNotNull null
        val activeDurationSec = record.activeDurationSec?.coerceAtLeast(0) ?: return@mapNotNull null
        val actualRestAfterSec = record.actualRestAfterSec?.coerceAtLeast(0) ?: return@mapNotNull null
        val effort = record.effort ?: return@mapNotNull null
        StrengthComparableSetSample(
            key = key,
            session = this,
            record = record,
            plannedWeight = plannedWeight,
            plannedRepTarget = plannedRepTarget,
            actualWeight = actualWeight,
            actualReps = actualReps,
            activeDurationSec = activeDurationSec,
            actualRestAfterSec = actualRestAfterSec,
            effort = effort
        )
    }
}

private fun StrengthSetRecord.toStrengthComparableSetKeyOrNull(): StrengthComparableSetKey? {
    val comparableExerciseId = exerciseId.takeIf { id -> id.isNotBlank() } ?: return null
    val normalizedSourceSetPlanId = sourceSetPlanId?.takeIf { id -> id.isNotBlank() }
    val safeSetOrder = setOrder.takeIf { order -> order > 0 } ?: return null
    return StrengthComparableSetKey(
        exerciseId = comparableExerciseId,
        sourceSetPlanId = normalizedSourceSetPlanId,
        fallbackSetOrder = if (normalizedSourceSetPlanId == null) safeSetOrder else null,
        fallbackSetKind = if (normalizedSourceSetPlanId == null) setKind else null
    )
}

private fun WorkoutPlanSnapshot.plannedValuesFor(record: StrengthSetRecord): StrengthPlannedSetValues {
    val sourceSetPlanId = record.sourceSetPlanId?.takeIf { id -> id.isNotBlank() }
    val candidateExerciseId = record.substitutedFromExerciseId
        ?.takeIf { id -> id.isNotBlank() }
        ?: record.exerciseId.takeIf { id -> id.isNotBlank() }
        ?: return StrengthPlannedSetValues(plannedWeight = null, plannedRepTarget = null)
    val candidateSets = blocks
        .filterIsInstance<StrengthExerciseBlock>()
        .filter { block -> block.exerciseId == candidateExerciseId }
        .sortedBy { block -> block.order }
        .flatMap { block ->
            block.sets.map { set -> block to set }
        }

    val plannedSet = if (sourceSetPlanId != null) {
        candidateSets.firstOrNull { (_, set) -> set.id == sourceSetPlanId }
    } else {
        candidateSets.firstOrNull { (_, set) ->
            set.order == record.setOrder && set.kind == record.setKind
        }
    }

    return plannedSet?.let { (block, set) ->
        StrengthPlannedSetValues(
            plannedWeight = set.targetWeight ?: block.target?.weight,
            plannedRepTarget = set.repTarget ?: block.target?.repTarget
        )
    } ?: StrengthPlannedSetValues(plannedWeight = null, plannedRepTarget = null)
}

private fun List<WorkoutSession>.toStrengthComparableSetDataQualityRows(
    sampleCount: Int,
    comparableGroupCount: Int
): List<StrengthComparableSetDataQualityRowUiState> {
    val rows = mutableListOf<StrengthComparableSetDataQualityRowUiState>()
    val strengthCount = count { session -> session.mode == WorkoutMode.STRENGTH && session.planSnapshot.mode == WorkoutMode.STRENGTH }
    val nonStrengthCount = size - strengthCount
    if (nonStrengthCount > 0) {
        rows += StrengthComparableSetDataQualityRowUiState(
            label = "已排除非力量记录",
            helper = "$nonStrengthCount 条 timed / follow_along 记录没有进入力量同类 set 趋势。"
        )
    }
    val strengthRecords = filter { session -> session.mode == WorkoutMode.STRENGTH }
        .flatMap { session -> session.strengthSetRecords }
    val fallbackCount = strengthRecords.count { record ->
        record.sourceSetPlanId.isNullOrBlank() && record.toStrengthComparableSetKeyOrNull() != null
    }
    if (fallbackCount > 0) {
        rows += StrengthComparableSetDataQualityRowUiState(
            label = "使用 setOrder + setKind 降级",
            helper = "$fallbackCount 条 strength set record 缺少 sourceSetPlanId，已按同一 exerciseId、setOrder 和 setKind 降级比较。"
        )
    }
    val malformedKeyCount = strengthRecords.count { record -> record.toStrengthComparableSetKeyOrNull() == null }
    if (malformedKeyCount > 0) {
        rows += StrengthComparableSetDataQualityRowUiState(
            label = "同类 key 数据不足",
            helper = "$malformedKeyCount 条 strength set record 缺少 exerciseId 或有效 setOrder，未进入趋势。"
        )
    }
    val incompleteMetricCount = filter { session -> session.mode == WorkoutMode.STRENGTH }
        .sumOf { session ->
            session.strengthSetRecords.count { record ->
                record.toStrengthComparableSetKeyOrNull() != null && !session.hasCompleteComparableStrengthMetrics(record)
            }
        }
    if (incompleteMetricCount > 0) {
        rows += StrengthComparableSetDataQualityRowUiState(
            label = "组记录字段不足",
            helper = "$incompleteMetricCount 条 strength set record 缺少 planned weight/reps、actual weight/reps、active duration、actual rest 或 effort，未造样本。"
        )
    }
    if (strengthCount > 0 && sampleCount == 0) {
        rows += StrengthComparableSetDataQualityRowUiState(
            label = "暂无完整 set 样本",
            helper = "现有力量记录缺少完整 planned / actual / duration / rest / effort 字段，暂不生成同类 set 趋势。"
        )
    } else if (strengthCount > 0 && comparableGroupCount == 0) {
        rows += StrengthComparableSetDataQualityRowUiState(
            label = "样本不足",
            helper = "已找到 $sampleCount 个完整力量 set 样本，但没有任一同类 set 达到 2 条真实记录。"
        )
    }
    return rows
}

private fun WorkoutSession.hasCompleteComparableStrengthMetrics(record: StrengthSetRecord): Boolean {
    val plannedValues = planSnapshot.plannedValuesFor(record)
    return (record.plannedWeight ?: plannedValues.plannedWeight) != null &&
        (record.plannedRepTarget ?: plannedValues.plannedRepTarget) != null &&
        record.actualWeight != null &&
        record.actualReps != null &&
        record.activeDurationSec != null &&
        record.actualRestAfterSec != null &&
        record.effort != null
}

private fun List<StrengthComparableSetSample>.toStrengthComparableSetTrendGroup(): StrengthComparableSetTrendGroupUiState {
    val firstSample = first()
    val rows = sortedBy { sample -> sample.session.startedAt.orEmpty() }
        .map { sample ->
            StrengthComparableSetTrendRowUiState(
                dateLabel = sample.session.dateKey,
                sessionTitle = sample.session.planSnapshot.title,
                plannedLabel = "${sample.plannedWeight.formatWeight()} · ${sample.plannedRepTarget.formatRepTarget()}",
                actualLabel = "${sample.actualWeight.formatWeight()} · ${sample.actualReps} 次",
                activeDurationLabel = sample.activeDurationSec.formatDuration(),
                actualRestLabel = sample.actualRestAfterSec.formatDuration(),
                effortLabel = sample.effort.displayLabel,
                sourceLabel = sample.sourceLabel(),
                substitutionLabel = sample.record.substitutedFromExerciseId?.let { originalId ->
                    "替换自 ${originalId.exerciseName()}；未并入原动作趋势"
                }
            )
        }
    return StrengthComparableSetTrendGroupUiState(
        title = "${firstSample.key.exerciseId.exerciseName()} · ${firstSample.groupTitleSuffix()}",
        ruleLabel = firstSample.key.comparisonRuleLabel,
        rows = rows
    )
}

private fun StrengthComparableSetSample.groupTitleSuffix(): String {
    return key.sourceSetPlanId ?: "第 ${key.fallbackSetOrder} 组 · ${key.fallbackSetKind?.displayName}"
}

private fun StrengthComparableSetSample.sourceLabel(): String {
    return key.sourceSetPlanId?.let { sourceId ->
        "sourceSetPlanId $sourceId"
    } ?: "setOrder ${key.fallbackSetOrder} · setKind ${key.fallbackSetKind?.contractValue}"
}

private fun List<WorkoutSession>.toHistoryCleanupUiState(): HistoryCleanupUiState {
    val planOptions = filter { session -> session.planId != null }
        .groupBy { session -> requireNotNull(session.planId) }
        .map { (planId, planSessions) ->
            val planTitle = planSessions.firstOrNull()?.planSnapshot?.title ?: planId
            HistoryCleanupOptionUiState(
                label = "清除计划：$planTitle",
                helper = "${planSessions.size} 条本地 WorkoutSession；不会删除 WorkoutPlan 或改写历史 plan snapshot。",
                target = HistoryCleanupTarget.Plan(
                    planId = planId,
                    planTitle = planTitle,
                    count = planSessions.size
                )
            )
        }
        .sortedBy { option -> option.label }
    val dateOptions = filter { session -> session.startedAt != null }
        .groupBy { session -> session.dateKey }
        .toSortedMap(compareByDescending { date -> date })
        .map { (date, dateSessions) ->
            HistoryCleanupOptionUiState(
                label = "清除日期：$date",
                helper = "${dateSessions.size} 条本地 WorkoutSession；按 startedAt 展示日期匹配。",
                target = HistoryCleanupTarget.Date(
                    dateLabel = date,
                    count = dateSessions.size
                )
            )
        }
    return HistoryCleanupUiState(
        title = "历史记录清理",
        description = "只清理真实本地 WorkoutSession 记录；不会删除训练计划、动作库、fixture 或 preview 数据。",
        allOption = HistoryCleanupOptionUiState(
            label = "清除全部历史",
            helper = "$size 条本地 WorkoutSession；基础统计和图表会随剩余记录自动刷新。",
            target = HistoryCleanupTarget.All(count = size)
        ),
        planOptions = planOptions,
        dateOptions = dateOptions
    )
}

private fun HistoryCleanupTarget.toDialogUiState(): HistoryCleanupDialogUiState {
    return when (this) {
        is HistoryCleanupTarget.All -> HistoryCleanupDialogUiState(
            title = "清除全部历史记录",
            message = "确认删除全部 $count 条本地 WorkoutSession 历史记录？这不会删除 WorkoutPlan、动作库、fixture 或 preview 数据。",
            confirmLabel = "确认全部清除"
        )

        is HistoryCleanupTarget.Plan -> HistoryCleanupDialogUiState(
            title = "按计划清除历史记录",
            message = "确认删除计划「$planTitle」下的 $count 条本地 WorkoutSession 历史记录？已保存 WorkoutPlan 会保留，历史 plan snapshot 不会被改写。",
            confirmLabel = "确认清除该计划"
        )

        is HistoryCleanupTarget.Date -> HistoryCleanupDialogUiState(
            title = "按日期清除历史记录",
            message = "确认删除 startedAt 日期为 $dateLabel 的 $count 条本地 WorkoutSession 历史记录？其他日期记录会保留。",
            confirmLabel = "确认清除该日期"
        )
    }
}

private data class DailyWorkoutAggregate(
    val dateLabel: String,
    val totalCount: Int,
    val completedCount: Int,
    val abandonedCount: Int,
    val totalElapsedSec: Int,
    val effectiveElapsedSec: Int,
    val pausedElapsedSec: Int,
    val plannedRestSec: Int,
    val actualRestSec: Int,
    val extraRestSec: Int
)

private fun List<WorkoutSession>.toDailyAggregates(): List<DailyWorkoutAggregate> {
    return mapNotNull { session ->
        session.startedAt?.take(10)?.let { date -> date to session }
    }
        .groupBy(keySelector = { (date) -> date }, valueTransform = { (_, session) -> session })
        .toSortedMap()
        .map { (date, sessionsOnDate) ->
            DailyWorkoutAggregate(
                dateLabel = date,
                totalCount = sessionsOnDate.size,
                completedCount = sessionsOnDate.count { session -> session.status == SessionStatus.COMPLETED },
                abandonedCount = sessionsOnDate.count { session -> session.status == SessionStatus.ABANDONED },
                totalElapsedSec = sessionsOnDate.sumOf { session -> session.durationSec() },
                effectiveElapsedSec = sessionsOnDate.sumOf { session -> session.effectiveDurationSec() },
                pausedElapsedSec = sessionsOnDate.sumOf { session -> session.pausedDurationSec() },
                plannedRestSec = sessionsOnDate.sumOf { session -> session.plannedRestSec() },
                actualRestSec = sessionsOnDate.sumOf { session -> session.actualRestSec() },
                extraRestSec = sessionsOnDate.sumOf { session -> session.extraRestSec() }
            )
        }
}

private fun List<DailyWorkoutAggregate>.toTrendChart(
    title: String,
    description: String,
    notEnoughMessage: String,
    yAxisLabel: String,
    unitLabel: String,
    series: List<Pair<String, (DailyWorkoutAggregate) -> Int>>,
    valueLabel: (Int) -> String
): AggregateTrendChartUiState {
    val dateLabels = map { aggregate -> aggregate.dateLabel }
    if (size < 2) {
        return AggregateTrendChartUiState(
            title = title,
            description = description,
            dateLabels = dateLabels,
            series = emptyList(),
            emptyMessage = notEnoughMessage,
            yAxisLabel = yAxisLabel,
            unitLabel = unitLabel,
            stateLabel = if (dateLabels.isEmpty()) "空状态" else "数据不足"
        )
    }
    return AggregateTrendChartUiState(
        title = title,
        description = description,
        dateLabels = dateLabels,
        yAxisLabel = yAxisLabel,
        unitLabel = unitLabel,
        series = series.map { (label, selector) ->
            AggregateTrendSeriesUiState(
                label = label,
                points = map { aggregate ->
                    val value = selector(aggregate).coerceAtLeast(0)
                    AggregateTrendPointUiState(
                        dateLabel = aggregate.dateLabel,
                        value = value,
                        valueLabel = valueLabel(value)
                    )
                }
            )
        }
    )
}

private fun List<WorkoutSession>.toModeBreakdownChart(): ModeBreakdownChartUiState {
    val total = size
    if (total == 0) {
        return ModeBreakdownChartUiState(
            title = "训练类型分布",
            description = "按真实记录的 mode 汇总。",
            totalCount = 0,
            rows = emptyList(),
            emptyMessage = "暂无真实记录可汇总。"
        )
    }
    val timedCount = count { session -> session.mode == WorkoutMode.TIMED }
    val strengthCount = count { session -> session.mode == WorkoutMode.STRENGTH }
    val followAlongCount = count { session -> session.mode == WorkoutMode.FOLLOW_ALONG }
    return ModeBreakdownChartUiState(
        title = "训练类型分布",
        description = "timed / strength / follow_along 分开显示数量和占比。",
        totalCount = total,
        rows = listOf(
            ModeBreakdownRowUiState("计时训练", timedCount, total.percentLabel(timedCount)),
            ModeBreakdownRowUiState("力量训练", strengthCount, total.percentLabel(strengthCount)),
            ModeBreakdownRowUiState("跟练", followAlongCount, total.percentLabel(followAlongCount))
        )
    )
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
    val rows = filter { session -> session.status == SessionStatus.COMPLETED }
        .sortedByDescending { session -> session.dateKey }
        .mapNotNull { session ->
            val actualRecords = session.strengthSetRecords.filter { record ->
                record.actualWeight != null && record.actualReps != null
            }
            if (actualRecords.isEmpty()) return@mapNotNull null

            val sets = actualRecords.size
            val reps = actualRecords.sumOf { record -> record.actualReps ?: 0 }
            val load = actualRecords.totalLoadKg()
            BasicTrendRowUiState(
                primary = session.dateKey,
                secondary = session.planSnapshot.title,
                metric = load.formatLoad(),
                helper = "$sets 组 · $reps 次；仅纳入已完成力量训练中同时记录实际重量和次数的组。"
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

private val SessionStatus.statusTone: HistoryTone
    get() = when (this) {
        SessionStatus.COMPLETED -> HistoryTone.SUCCESS
        SessionStatus.ABANDONED -> HistoryTone.WARNING
        SessionStatus.READY,
        SessionStatus.ACTIVE,
        SessionStatus.PAUSED -> HistoryTone.INFO
    }

private fun WorkoutSession.durationSec(): Int {
    totalElapsedSec?.let { elapsed -> return elapsed.coerceAtLeast(0) }
    return when (mode) {
        WorkoutMode.TIMED -> stepHistory.sumOf { record -> record.actualDurationSec ?: 0 }
        WorkoutMode.STRENGTH -> strengthSetRecords.sumOf { record ->
            (record.activeDurationSec ?: 0) + (record.actualRestAfterSec ?: 0)
        }
        WorkoutMode.FOLLOW_ALONG -> stepHistory.sumOf { record -> record.actualDurationSec ?: 0 }
    }
}

private fun WorkoutSession.effectiveDurationSec(): Int {
    effectiveElapsedSec?.let { elapsed -> return elapsed.coerceAtLeast(0) }
    return (durationSec() - pausedDurationSec()).coerceAtLeast(0)
}

private fun WorkoutSession.pausedDurationSec(): Int {
    return pausedElapsedSec?.coerceAtLeast(0) ?: 0
}

private fun WorkoutSession.extraRestSec(): Int {
    return timedRestExtensionRecords.sumOf { record -> record.addedSec.coerceAtLeast(0) }
}

private fun WorkoutSession.actualRestSec(): Int {
    return when (mode) {
        WorkoutMode.TIMED,
        WorkoutMode.FOLLOW_ALONG -> stepHistory.actualDurationSecFor(SessionStepKind.TIMED_REST)

        WorkoutMode.STRENGTH -> {
            val hasSetRestRecords = strengthSetRecords.any { record -> record.actualRestAfterSec != null }
            if (hasSetRestRecords) {
                strengthSetRecords.sumOf { record -> record.actualRestAfterSec?.coerceAtLeast(0) ?: 0 }
            } else {
                stepHistory.actualDurationSecFor(SessionStepKind.STRENGTH_REST)
            }
        }
    }
}

private fun WorkoutSession.plannedRestSec(): Int {
    return planSnapshot.blocks.sumOf { block ->
        when (block) {
            is RestBlock -> block.durationSec
            is TimedCircuitBlock -> block.plannedTimedRestSec()
            is TimedCompositionBlock -> block.plannedTimedCompositionRestSec()
            else -> 0
        }
    } + planSnapshot.plannedStrengthRestSec()
}

private fun List<SessionStepRecord>.actualDurationSecFor(kind: SessionStepKind): Int {
    return sumOf { record ->
        if (record.kind == kind) {
            record.actualDurationSec?.coerceAtLeast(0) ?: 0
        } else {
            0
        }
    }
}

private fun TimedCircuitBlock.plannedTimedRestSec(): Int {
    val perRoundRest = items.sumOf { item ->
        if (item.stageType == TimedStageType.REST) {
            item.workDurationSec
        } else {
            item.restAfterSec ?: 0
        }
    }
    val betweenRounds = (rounds - 1).coerceAtLeast(0) * (restBetweenRoundsSec ?: 0)
    return perRoundRest * rounds.coerceAtLeast(0) + betweenRounds
}

private fun TimedCompositionBlock.plannedTimedCompositionRestSec(): Int {
    return expandedCompositionStepsOrEmpty()
        .filter { step -> step.stepKind == TimedCompositionTimelineStepKind.REST }
        .sumOf { step -> step.plannedDurationSec.coerceAtLeast(0) }
}

private fun WorkoutPlanSnapshot.plannedStrengthRestSec(): Int {
    val plannedSetRests = blocks
        .filterIsInstance<StrengthExerciseBlock>()
        .sortedBy { block -> block.order }
        .flatMap { block ->
            block.sets
                .sortedBy { set -> set.order }
                .map { set -> set.restAfterSec ?: block.target?.restAfterSetSec ?: 0 }
        }
    return plannedSetRests.dropLast(1).sumOf { restSec -> restSec.coerceAtLeast(0) }
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

private fun RepTarget.formatRepTarget(): String {
    return when (this) {
        is RepTarget.Fixed -> "$reps 次"
        is RepTarget.Range -> "$minReps-$maxReps 次"
    }
}

private val StrengthSetKind.displayName: String
    get() = when (this) {
        StrengthSetKind.WARMUP -> "热身组"
        StrengthSetKind.WORKING -> "正式组"
        StrengthSetKind.DROP -> "递减组"
        StrengthSetKind.BACKOFF -> "回退组"
    }

private val SetEffort.displayLabel: String
    get() = when (this) {
        SetEffort.EASY -> "easy"
        SetEffort.GOOD -> "good"
        SetEffort.HARD -> "hard"
        SetEffort.FORM_BREAKDOWN -> "form breakdown"
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

private fun Int.percentLabel(count: Int): String {
    if (this <= 0) return "0%"
    val percent = count * 100 / this
    return "$percent%"
}

private fun Int.axisValueLabel(unitLabel: String): String {
    return if (unitLabel.isBlank()) {
        toString()
    } else {
        "$this$unitLabel"
    }
}
