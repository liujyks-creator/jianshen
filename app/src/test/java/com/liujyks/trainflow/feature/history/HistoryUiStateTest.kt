package com.liujyks.trainflow.feature.history

import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SetEffort
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthExerciseTarget
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StrengthSetRecord
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.SessionStepRecord
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionStageGroup
import com.liujyks.trainflow.core.model.TimedCompositionTarget
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedCompositionTimelineAdapter
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStep
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedRestExtensionRecord
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryUiStateTest {
    @Test
    fun historyGroupsSessionsByDateDescending() {
        val state = buildDefaultHistoryScreenState()

        assertEquals(
            listOf("2026-06-01", "2026-05-30", "2026-05-28", "2026-05-27"),
            state.dateGroups.map { group -> group.dateLabel }
        )
    }

    @Test
    fun mixedTimedAndStrengthListItemsExposeModeStatusDurationAndKeySummary() {
        val state = buildDefaultHistoryScreenState()
        val items = state.dateGroups.flatMap { group -> group.items }

        val strength = items.first { item -> item.id == "history-strength-2026-06-01" }
        val timed = items.first { item -> item.id == "history-timed-2026-05-30" }

        assertEquals("力量", strength.modeBadge)
        assertEquals("已完成", strength.statusLabel)
        assertTrue(strength.keySummary.contains("确认 3 组"))
        assertTrue(strength.durationLabel.contains("分"))

        assertEquals("计时", timed.modeBadge)
        assertEquals("计时训练", timed.modeLabel)
        assertTrue(timed.keySummary.contains("完成 5 步"))
    }

    @Test
    fun selectingSessionUpdatesDetail() {
        val state = buildDefaultHistoryScreenState()
            .selectSession("history-timed-2026-05-30")

        val detail = requireNotNull(state.selectedDetail)

        assertEquals("history-timed-2026-05-30", detail.id)
        assertTrue(detail.subtitle.contains("计时训练"))
        assertTrue(detail.rows.any { row -> row.label == "完成步骤" })
        assertTrue(detail.sourceNote.contains("示例记录"))
    }

    @Test
    fun realSessionsShowTodayRecordWithoutFixtureOverride() {
        val todaySession = strengthSession(
            id = "real-strength-2026-06-07",
            status = SessionStatus.COMPLETED,
            startedAt = "2026-06-07T10:00:00Z",
            records = listOf(
                strengthSetRecord("confirmed", actualWeight = WeightValue(50.0, WeightUnit.KG), actualReps = 8)
            )
        )
        val state = buildHistoryScreenState(sessions = listOf(todaySession))

        assertEquals("本地真实记录", state.sourcePillLabel)
        assertEquals(listOf("2026-06-07"), state.dateGroups.map { group -> group.dateLabel })
        assertEquals(listOf("real-strength-2026-06-07"), state.dateGroups.single().items.map { item -> item.id })
        assertFalse(state.dateGroups.single().items.any { item -> item.id.startsWith("history-") })
        assertTrue(requireNotNull(state.selectedDetail).sourceNote.contains("本地 session record"))
    }

    @Test
    fun realRecordStatsAreEmptyForNoRecordsAndPreviewFixtures() {
        assertEquals(null, HistoryScreenState(sessions = emptyList()).recordStats)
        assertEquals(null, buildDefaultHistoryScreenState().recordStats)
    }

    @Test
    fun realRecordStatsSummarizeCountsDurationsRestAndModeBreakdown() {
        val state = buildHistoryScreenState(
            sessions = listOf(
                timedSession(
                    id = "real-timed-completed",
                    status = SessionStatus.COMPLETED,
                    totalElapsedSec = 100,
                    effectiveElapsedSec = 80,
                    pausedElapsedSec = 20,
                    actualRestSec = 25,
                    extraRestAdds = listOf(15, 15),
                    plannedRestSec = 90
                ),
                timedSession(
                    id = "real-timed-abandoned",
                    status = SessionStatus.ABANDONED,
                    totalElapsedSec = 50,
                    effectiveElapsedSec = 50,
                    pausedElapsedSec = 0,
                    actualRestSec = 10,
                    extraRestAdds = listOf(15),
                    plannedRestSec = 10
                ),
                strengthSession(
                    id = "real-strength-completed",
                    status = SessionStatus.COMPLETED,
                    startedAt = "2026-06-08T10:00:00Z",
                    records = listOf(
                        strengthSetRecord(
                            id = "set-with-rest",
                            actualWeight = WeightValue(50.0, WeightUnit.KG),
                            actualReps = 8,
                            actualRestAfterSec = 60
                        )
                    ),
                    plannedSetCount = 2,
                    totalElapsedSec = 200,
                    effectiveElapsedSec = 180,
                    pausedElapsedSec = 20,
                    plannedRestAfterSetSec = 60
                ),
                followAlongSession(
                    id = "real-follow-completed",
                    totalElapsedSec = 40,
                    effectiveElapsedSec = 40
                )
            )
        )

        val stats = requireNotNull(state.recordStats)

        assertEquals(4, stats.totalCount)
        assertEquals(3, stats.completedCount)
        assertEquals(1, stats.abandonedCount)
        assertEquals(390, stats.totalElapsedSec)
        assertEquals(350, stats.effectiveElapsedSec)
        assertEquals(40, stats.pausedElapsedSec)
        assertEquals(160, stats.plannedRestSec)
        assertEquals(95, stats.actualRestSec)
        assertEquals(45, stats.extraRestSec)
        assertEquals(2, stats.timedCount)
        assertEquals(1, stats.strengthCount)
        assertEquals(1, stats.followAlongCount)
    }

    @Test
    fun strengthActualRestDoesNotDoubleCountStepHistoryAndSetRecords() {
        val session = strengthSession(
            id = "real-strength-rest-double-source",
            status = SessionStatus.COMPLETED,
            startedAt = "2026-06-08T10:00:00Z",
            records = listOf(
                strengthSetRecord(
                    id = "set-with-rest-record",
                    actualWeight = WeightValue(50.0, WeightUnit.KG),
                    actualReps = 8,
                    actualRestAfterSec = 60
                )
            ),
            stepHistory = listOf(
                SessionStepRecord(
                    stepId = "same-rest-step",
                    kind = SessionStepKind.STRENGTH_REST,
                    startedAt = "2026-06-08T10:02:00Z",
                    endedAt = "2026-06-08T10:03:00Z",
                    actualDurationSec = 60
                )
            )
        )

        val state = buildHistoryScreenState(sessions = listOf(session))
        val actualRestRow = requireNotNull(state.selectedDetail)
            .rows
            .single { row -> row.label == "实际休息" }

        assertEquals(60, requireNotNull(state.recordStats).actualRestSec)
        assertEquals("1分", actualRestRow.value)
    }

    @Test
    fun strengthPlannedRestExcludesFinalGlobalSetRest() {
        val session = strengthSession(
            id = "real-strength-two-sets-one-rest",
            status = SessionStatus.COMPLETED,
            startedAt = "2026-06-08T10:00:00Z",
            records = emptyList(),
            plannedSetCount = 2,
            plannedRestAfterSetSec = 60
        )

        assertEquals(60, requireNotNull(buildHistoryScreenState(listOf(session)).recordStats).plannedRestSec)
    }

    @Test
    fun realRecordStatsUiCopyKeepsExtraRestSeparateFromPausedTime() {
        val state = buildHistoryScreenState(
            sessions = listOf(
                timedSession(
                    id = "real-timed-rest-extension",
                    status = SessionStatus.ABANDONED,
                    totalElapsedSec = 50,
                    effectiveElapsedSec = 50,
                    pausedElapsedSec = 0,
                    actualRestSec = 10,
                    extraRestAdds = listOf(15),
                    plannedRestSec = 10
                )
            )
        )

        val rows = requireNotNull(state.recordStatsUiState).rows.associateBy { row -> row.label }

        assertEquals("15秒", requireNotNull(rows["计时额外休息"]).value)
        assertEquals("0秒", requireNotNull(rows["暂停时间"]).value)
        assertTrue(requireNotNull(rows["计时额外休息"]).helper.contains("timedRestExtensionRecords.addedSec"))
        assertTrue(requireNotNull(rows["暂停时间"]).helper.contains("额外休息分开"))
    }

    @Test
    fun aggregateChartsAreOnlyBuiltForRealPersistedRecords() {
        assertEquals(null, HistoryScreenState(sessions = emptyList()).aggregateChartsUiState)
        assertEquals(null, buildDefaultHistoryScreenState().aggregateChartsUiState)

        val state = buildHistoryScreenState(
            sessions = listOf(
                timedSession(
                    id = "real-timed-chart",
                    status = SessionStatus.COMPLETED,
                    totalElapsedSec = 100,
                    effectiveElapsedSec = 80,
                    pausedElapsedSec = 20,
                    actualRestSec = 25,
                    extraRestAdds = listOf(15),
                    plannedRestSec = 30
                )
            )
        )

        assertNotNull(state.aggregateChartsUiState)
    }

    @Test
    fun aggregateTrendsNeedAtLeastTwoStartedAtDates() {
        val state = buildHistoryScreenState(
            sessions = listOf(
                timedSession(
                    id = "only-one-date",
                    status = SessionStatus.COMPLETED,
                    totalElapsedSec = 100,
                    effectiveElapsedSec = 80,
                    pausedElapsedSec = 20,
                    actualRestSec = 25,
                    extraRestAdds = emptyList(),
                    plannedRestSec = 30
                )
            )
        )

        val charts = requireNotNull(state.aggregateChartsUiState)

        assertEquals(1, charts.pointCount)
        assertFalse(charts.countTrend.hasDrawableTrend)
        assertTrue(requireNotNull(charts.countTrend.emptyMessage).contains("暂无趋势"))
        assertTrue(charts.countTrend.series.isEmpty())
    }

    @Test
    fun aggregateCountAndStatusTrendsGroupRealSessionsByStartedAtDate() {
        val charts = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    timedSession(
                        id = "date-one-completed",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-08T09:00:00Z",
                        totalElapsedSec = 100,
                        effectiveElapsedSec = 80,
                        pausedElapsedSec = 20,
                        actualRestSec = 25,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 30
                    ),
                    strengthSession(
                        id = "date-one-abandoned",
                        status = SessionStatus.ABANDONED,
                        startedAt = "2026-06-08T18:00:00Z",
                        records = emptyList()
                    ),
                    timedSession(
                        id = "date-two-completed",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-09T09:00:00Z",
                        totalElapsedSec = 50,
                        effectiveElapsedSec = 50,
                        pausedElapsedSec = 0,
                        actualRestSec = 10,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 10
                    )
                )
            ).aggregateChartsUiState
        )

        assertEquals(listOf("2026-06-08", "2026-06-09"), charts.countTrend.dateLabels)
        assertEquals(listOf(2, 1), charts.countTrend.series.single().points.map { point -> point.value })

        val statusSeries = charts.statusTrend.series.associateBy { series -> series.label }
        assertEquals(listOf(1, 1), requireNotNull(statusSeries["completed"]).points.map { point -> point.value })
        assertEquals(listOf(1, 0), requireNotNull(statusSeries["abandoned"]).points.map { point -> point.value })
    }

    @Test
    fun aggregateElapsedTrendSeparatesTotalEffectiveAndPausedDurations() {
        val charts = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    timedSession(
                        id = "elapsed-one",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-08T09:00:00Z",
                        totalElapsedSec = 100,
                        effectiveElapsedSec = 70,
                        pausedElapsedSec = 30,
                        actualRestSec = 20,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 20
                    ),
                    timedSession(
                        id = "elapsed-two",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-09T09:00:00Z",
                        totalElapsedSec = 80,
                        effectiveElapsedSec = 75,
                        pausedElapsedSec = 5,
                        actualRestSec = 10,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 10
                    )
                )
            ).aggregateChartsUiState
        )

        val elapsedSeries = charts.elapsedTrend.series.associateBy { series -> series.label }

        assertEquals(listOf(100, 80), requireNotNull(elapsedSeries["总用时"]).points.map { point -> point.value })
        assertEquals(listOf(70, 75), requireNotNull(elapsedSeries["有效训练时间"]).points.map { point -> point.value })
        assertEquals(listOf(30, 5), requireNotNull(elapsedSeries["暂停时间"]).points.map { point -> point.value })
    }

    @Test
    fun aggregateRestTrendSeparatesPlannedActualAndExtraRestWithoutPausedPollution() {
        val charts = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    timedSession(
                        id = "rest-one",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-08T09:00:00Z",
                        totalElapsedSec = 100,
                        effectiveElapsedSec = 70,
                        pausedElapsedSec = 30,
                        actualRestSec = 20,
                        extraRestAdds = listOf(15),
                        plannedRestSec = 10
                    ),
                    timedSession(
                        id = "rest-two",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-09T09:00:00Z",
                        totalElapsedSec = 80,
                        effectiveElapsedSec = 75,
                        pausedElapsedSec = 5,
                        actualRestSec = 12,
                        extraRestAdds = listOf(15, 15),
                        plannedRestSec = 12
                    )
                )
            ).aggregateChartsUiState
        )

        val restSeries = charts.restTrend.series.associateBy { series -> series.label }
        val elapsedSeries = charts.elapsedTrend.series.associateBy { series -> series.label }

        assertEquals(listOf(10, 12), requireNotNull(restSeries["计划休息"]).points.map { point -> point.value })
        assertEquals(listOf(20, 12), requireNotNull(restSeries["实际休息"]).points.map { point -> point.value })
        assertEquals(listOf(15, 30), requireNotNull(restSeries["额外休息"]).points.map { point -> point.value })
        assertEquals(listOf(30, 5), requireNotNull(elapsedSeries["暂停时间"]).points.map { point -> point.value })
    }

    @Test
    fun timedComparableRestTrendIsOnlyBuiltForRealPersistedTimedSessions() {
        assertEquals(null, buildDefaultHistoryScreenState().timedComparableRestTrendUiState)

        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableTimedSession(
                        id = "timed-comparable-one",
                        startedAt = "2026-06-08T09:00:00Z",
                        actualRestSec = 20
                    ),
                    comparableTimedSession(
                        id = "timed-comparable-two",
                        startedAt = "2026-06-09T09:00:00Z",
                        actualRestSec = 24
                    ),
                    strengthSession(
                        id = "strength-not-comparable",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-09T18:00:00Z",
                        records = emptyList()
                    )
                )
            ).timedComparableRestTrendUiState
        )

        assertEquals(1, trend.groups.size)
        assertTrue(trend.dataQualityRows.any { row -> row.label == "已排除非计时记录" })
        assertTrue(trend.description.contains("timedRestExtensionRecords.addedSec"))
    }

    @Test
    fun timedComparableRestTrendComparesSameStructureStageOrderRoundAndRelationship() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableTimedSession(
                        id = "same-stage-one",
                        startedAt = "2026-06-08T09:00:00Z",
                        actualRestSec = 20,
                        extraRestAdds = listOf(15)
                    ),
                    comparableTimedSession(
                        id = "same-stage-two",
                        startedAt = "2026-06-09T09:00:00Z",
                        actualRestSec = 28,
                        extraRestAdds = listOf(15, 15)
                    )
                )
            ).timedComparableRestTrendUiState
        )

        val group = trend.groups.single()

        assertTrue(group.ruleLabel.contains("同一 REST 阶段"))
        assertEquals(listOf("2026-06-08", "2026-06-09"), group.rows.map { row -> row.dateLabel })
        assertEquals(listOf("20秒", "20秒"), group.rows.map { row -> row.plannedRestLabel })
        assertEquals(listOf("20秒", "28秒"), group.rows.map { row -> row.actualRestLabel })
        assertEquals(listOf("15秒", "30秒"), group.rows.map { row -> row.extraRestLabel })
        assertTrue(group.rows.all { row -> row.positionLabel.contains("round 1") })
        assertTrue(group.rows.all { row -> row.positionLabel.contains("step 1") })
        assertTrue(group.rows.all { row -> row.positionLabel.contains("prev snapshot-work") })
    }

    @Test
    fun timedComparableRestTrendDoesNotMixDifferentPlanStructures() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableTimedSession(
                        id = "structure-one",
                        startedAt = "2026-06-08T09:00:00Z",
                        plannedRestSec = 20,
                        actualRestSec = 20
                    ),
                    comparableTimedSession(
                        id = "structure-two",
                        startedAt = "2026-06-09T09:00:00Z",
                        plannedRestSec = 30,
                        actualRestSec = 30
                    )
                )
            ).timedComparableRestTrendUiState
        )

        assertTrue(trend.groups.isEmpty())
        assertTrue(requireNotNull(trend.emptyMessage).contains("暂无可比计时阶段趋势"))
        assertTrue(trend.dataQualityRows.any { row -> row.label == "样本不足" })
    }

    @Test
    fun timedComparableRestTrendDoesNotMixDifferentRounds() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableTimedSession(
                        id = "round-one",
                        startedAt = "2026-06-08T09:00:00Z",
                        roundIndex = 1,
                        actualRestSec = 20
                    ),
                    comparableTimedSession(
                        id = "round-two",
                        startedAt = "2026-06-09T09:00:00Z",
                        roundIndex = 2,
                        actualRestSec = 22
                    )
                )
            ).timedComparableRestTrendUiState
        )

        assertTrue(trend.groups.isEmpty())
        assertTrue(requireNotNull(trend.emptyMessage).contains("同一轮次"))
    }

    @Test
    fun timedComparableRestTrendUsesExtraRestAddedSecWithoutPausedPollution() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableTimedSession(
                        id = "extra-one",
                        startedAt = "2026-06-08T09:00:00Z",
                        actualRestSec = 25,
                        extraRestAdds = listOf(15),
                        pausedElapsedSec = 90
                    ),
                    comparableTimedSession(
                        id = "extra-two",
                        startedAt = "2026-06-09T09:00:00Z",
                        actualRestSec = 40,
                        extraRestAdds = listOf(15, 15),
                        pausedElapsedSec = 120
                    )
                )
            ).timedComparableRestTrendUiState
        )

        val rows = trend.groups.single().rows

        assertEquals(listOf("15秒", "30秒"), rows.map { row -> row.extraRestLabel })
        assertFalse(rows.any { row -> row.extraRestLabel == "1分30秒" || row.extraRestLabel == "2分" })
    }

    @Test
    fun timedComparableRestTrendDowngradesMalformedExtraRestPositionFields() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableTimedSession(
                        id = "malformed-one",
                        startedAt = "2026-06-08T09:00:00Z",
                        actualRestSec = 25,
                        extraRestAdds = listOf(15),
                        malformedExtraRestPosition = true
                    ),
                    comparableTimedSession(
                        id = "malformed-two",
                        startedAt = "2026-06-09T09:00:00Z",
                        actualRestSec = 30,
                        extraRestAdds = emptyList()
                    )
                )
            ).timedComparableRestTrendUiState
        )

        val rows = trend.groups.single().rows

        assertEquals(listOf("0秒", "0秒"), rows.map { row -> row.extraRestLabel })
        assertTrue(trend.dataQualityRows.any { row -> row.label == "额外休息位置数据不足" })
    }

    @Test
    fun timedComparableRestTrendDoesNotInventSamplesWhenStepRecordsAreMissing() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableTimedSession(
                        id = "missing-step-one",
                        startedAt = "2026-06-08T09:00:00Z",
                        actualRestSec = 20,
                        includeRestStepRecord = false
                    ),
                    comparableTimedSession(
                        id = "missing-step-two",
                        startedAt = "2026-06-09T09:00:00Z",
                        actualRestSec = 30,
                        includeRestStepRecord = false
                    )
                )
            ).timedComparableRestTrendUiState
        )

        assertTrue(trend.groups.isEmpty())
        assertTrue(trend.dataQualityRows.any { row -> row.label == "暂无阶段级样本" })
    }

    @Test
    fun v2TimedSessionDetailRowsInterpretStageGroupsTargetsAndBoundaryRest() {
        val state = buildHistoryScreenState(
            sessions = listOf(
                comparableTimedCompositionSession(
                    id = "v2-detail",
                    startedAt = "2026-06-08T09:00:00Z",
                    rounds = 2,
                    restBetweenRoundsSec = 6,
                    targetRestActualSec = 4,
                    betweenRoundRestActualSec = 6,
                    extraTargetRestAdds = listOf(15),
                    extraBetweenRoundRestAdds = listOf(15)
                )
            )
        )

        val rows = requireNotNull(state.selectedDetail).rows.associateBy { row -> row.label }

        assertEquals("14秒", requireNotNull(rows["计划休息"]).value)
        assertTrue(requireNotNull(rows["v2 编排"]).value.contains("composition v2"))
        assertTrue(requireNotNull(rows["v2 编排"]).helper.contains("stageGroup 1 个"))
        assertTrue(requireNotNull(rows["v2 编排"]).helper.contains("target 2 个"))
        assertTrue(requireNotNull(rows["v2 阶段 / 目标"]).value.contains("Main group(group-main)"))
        assertTrue(requireNotNull(rows["v2 阶段 / 目标"]).value.contains("Action target(target-action:action)"))
        assertTrue(requireNotNull(rows["v2 阶段 / 目标"]).value.contains("Rest target(target-rest:rest)"))
        assertTrue(requireNotNull(rows["v2 boundary rest"]).helper.contains("between_round_rest"))
        assertTrue(requireNotNull(rows["v2 boundary rest"]).helper.contains("composition-v2:r1:between-round-rest:target"))
        assertEquals("30秒", requireNotNull(rows["v2 额外休息定位"]).value)
        assertTrue(requireNotNull(rows["v2 额外休息定位"]).helper.contains("target target-rest"))
        assertTrue(requireNotNull(rows["v2 额外休息定位"]).helper.contains("target composition-v2:r1:between-round-rest:target"))
    }

    @Test
    fun v2TimedComparableRestTrendUsesCompositionKeyFields() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableTimedCompositionSession(
                        id = "v2-target-one",
                        startedAt = "2026-06-08T09:00:00Z",
                        rounds = 1,
                        restBetweenRoundsSec = 0,
                        targetRestActualSec = 4,
                        extraTargetRestAdds = listOf(15)
                    ),
                    comparableTimedCompositionSession(
                        id = "v2-target-two",
                        startedAt = "2026-06-09T09:00:00Z",
                        rounds = 1,
                        restBetweenRoundsSec = 0,
                        targetRestActualSec = 6
                    )
                )
            ).timedComparableRestTrendUiState
        )

        val group = trend.groups.single()
        val position = group.rows.first().positionLabel

        assertTrue(group.ruleLabel.contains("composition v2"))
        assertTrue(group.ruleLabel.contains("不与 legacy timed 合并"))
        assertEquals(listOf("4秒", "6秒"), group.rows.map { row -> row.actualRestLabel })
        assertEquals(listOf("15秒", "0秒"), group.rows.map { row -> row.extraRestLabel })
        assertTrue(position.contains("family timed_composition_v2"))
        assertTrue(position.contains("compositionVersion 2"))
        assertTrue(position.contains("block composition-v2"))
        assertTrue(position.contains("stageGroup group-main"))
        assertTrue(position.contains("target target-rest"))
        assertTrue(position.contains("targetKind rest"))
        assertTrue(position.contains("round 1"))
        assertTrue(position.contains("stageGroupIndex 1"))
        assertTrue(position.contains("targetIndex 2"))
        assertTrue(position.contains("planned 4秒"))
        assertTrue(position.contains("signature composition_v2:composition-v2:2"))
    }

    @Test
    fun v2BetweenRoundRestTrendUsesBoundaryRestKeyAndExtensionMapping() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableTimedCompositionSession(
                        id = "v2-boundary-one",
                        startedAt = "2026-06-08T09:00:00Z",
                        rounds = 2,
                        restBetweenRoundsSec = 6,
                        targets = listOf(compositionActionTarget()),
                        betweenRoundRestActualSec = 8,
                        extraBetweenRoundRestAdds = listOf(15)
                    ),
                    comparableTimedCompositionSession(
                        id = "v2-boundary-two",
                        startedAt = "2026-06-09T09:00:00Z",
                        rounds = 2,
                        restBetweenRoundsSec = 6,
                        targets = listOf(compositionActionTarget()),
                        betweenRoundRestActualSec = 6
                    )
                )
            ).timedComparableRestTrendUiState
        )

        val group = trend.groups.single()
        val position = group.rows.first().positionLabel

        assertTrue(group.title.contains("轮间休息"))
        assertEquals(listOf("8秒", "6秒"), group.rows.map { row -> row.actualRestLabel })
        assertEquals(listOf("15秒", "0秒"), group.rows.map { row -> row.extraRestLabel })
        assertTrue(position.contains("stageKind between_round_rest"))
        assertTrue(position.contains("target composition-v2:r1:between-round-rest:target"))
        assertTrue(position.contains("targetKind between_round_rest"))
        assertTrue(position.contains("stageGroupIndex 0"))
        assertTrue(position.contains("targetIndex 1"))
        assertTrue(position.contains("planned 6秒"))
    }

    @Test
    fun legacyAndV2TimedSessionsDoNotMergeIntoSameComparableTrend() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableTimedSession(
                        id = "legacy-single",
                        startedAt = "2026-06-08T09:00:00Z",
                        actualRestSec = 20
                    ),
                    comparableTimedCompositionSession(
                        id = "v2-single",
                        startedAt = "2026-06-09T09:00:00Z",
                        rounds = 1,
                        restBetweenRoundsSec = 0,
                        targetRestActualSec = 20
                    )
                )
            ).timedComparableRestTrendUiState
        )

        assertTrue(trend.groups.isEmpty())
        assertTrue(trend.dataQualityRows.any { row -> row.label == "legacy / v2 趋势已隔离" })
        assertTrue(requireNotNull(trend.emptyMessage).contains("同一 family"))
    }

    @Test
    fun strengthComparableTrendStillBuildsWhenV2TimedSessionsArePresent() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableStrengthSession(
                        id = "strength-with-v2-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        actualKg = 60.0
                    ),
                    comparableStrengthSession(
                        id = "strength-with-v2-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        actualKg = 62.5
                    ),
                    comparableTimedCompositionSession(
                        id = "v2-not-strength",
                        startedAt = "2026-06-09T09:00:00Z",
                        rounds = 1,
                        restBetweenRoundsSec = 0,
                        targetRestActualSec = 4
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        assertEquals(1, trend.groups.size)
        assertTrue(trend.groups.single().ruleLabel.contains("sourceSetPlanId"))
        assertTrue(trend.dataQualityRows.any { row -> row.label == "已排除非力量记录" })
    }

    @Test
    fun strengthComparableSetTrendIsOnlyBuiltForRealPersistedStrengthSessions() {
        assertEquals(null, buildDefaultHistoryScreenState().strengthComparableSetTrendUiState)

        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableStrengthSession(
                        id = "strength-comparable-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        actualKg = 60.0
                    ),
                    comparableStrengthSession(
                        id = "strength-comparable-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        actualKg = 62.5
                    ),
                    timedSession(
                        id = "timed-not-strength",
                        status = SessionStatus.COMPLETED,
                        totalElapsedSec = 100,
                        effectiveElapsedSec = 90,
                        pausedElapsedSec = 10,
                        actualRestSec = 20,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 20
                    ),
                    followAlongSession(
                        id = "follow-not-strength",
                        totalElapsedSec = 40,
                        effectiveElapsedSec = 40
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        assertEquals(1, trend.groups.size)
        assertTrue(trend.dataQualityRows.any { row -> row.label == "已排除非力量记录" })
        assertTrue(trend.description.contains("sourceSetPlanId"))
        assertTrue(trend.description.contains("不输出强弱判断"))
    }

    @Test
    fun strengthComparableSetTrendPrioritizesSameExerciseAndSourceSetPlanId() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableStrengthSession(
                        id = "source-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        plannedKg = 57.5,
                        actualKg = 57.5,
                        actualReps = 8,
                        activeDurationSec = 42,
                        actualRestAfterSec = 75,
                        effort = SetEffort.GOOD
                    ),
                    comparableStrengthSession(
                        id = "source-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        plannedKg = 60.0,
                        actualKg = 60.0,
                        actualReps = 10,
                        activeDurationSec = 45,
                        actualRestAfterSec = 90,
                        effort = SetEffort.HARD
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        val group = trend.groups.single()

        assertTrue(group.ruleLabel.contains("同一 sourceSetPlanId"))
        assertTrue(group.title.contains("杠铃卧推"))
        assertEquals(listOf("2026-06-08", "2026-06-09"), group.rows.map { row -> row.dateLabel })
        assertEquals(listOf("57.5 kg · 8-12 次", "60 kg · 8-12 次"), group.rows.map { row -> row.plannedLabel })
        assertEquals(listOf("57.5 kg · 8 次", "60 kg · 10 次"), group.rows.map { row -> row.actualLabel })
        assertEquals(listOf("42秒", "45秒"), group.rows.map { row -> row.activeDurationLabel })
        assertEquals(listOf("1分15秒", "1分30秒"), group.rows.map { row -> row.actualRestLabel })
        assertEquals(listOf("good", "hard"), group.rows.map { row -> row.effortLabel })
        assertTrue(group.rows.all { row -> row.sourceLabel.contains("bench-working-1") })
    }

    @Test
    fun strengthComparableSetTrendFallsBackToSetOrderAndKindWhenSourceSetPlanIdIsMissing() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableStrengthSession(
                        id = "fallback-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        sourceSetPlanId = null,
                        actualKg = 55.0,
                        setOrder = 2,
                        setKind = StrengthSetKind.WORKING
                    ),
                    comparableStrengthSession(
                        id = "fallback-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        sourceSetPlanId = null,
                        actualKg = 57.5,
                        setOrder = 2,
                        setKind = StrengthSetKind.WORKING
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        val group = trend.groups.single()

        assertTrue(group.ruleLabel.contains("setOrder + setKind"))
        assertTrue(group.rows.all { row -> row.sourceLabel.contains("setOrder 2") })
        assertTrue(group.rows.all { row -> row.sourceLabel.contains("working") })
        assertTrue(trend.dataQualityRows.any { row -> row.label == "使用 setOrder + setKind 降级" })
    }

    @Test
    fun strengthComparableSetTrendDoesNotFallbackToSetOrderWhenSourceSetPlanIdIsPresentButMissing() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableStrengthSession(
                        id = "source-missing-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        sourceSetPlanId = "missing-source-set",
                        snapshotSetPlanId = "bench-working-1",
                        setOrder = 1,
                        setKind = StrengthSetKind.WORKING,
                        plannedKg = null,
                        plannedRepTarget = null,
                        snapshotTargetKg = 50.0,
                        snapshotRepTarget = RepTarget.Fixed(6),
                        actualKg = 50.0
                    ),
                    comparableStrengthSession(
                        id = "source-missing-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        sourceSetPlanId = "missing-source-set",
                        snapshotSetPlanId = "bench-working-1",
                        setOrder = 1,
                        setKind = StrengthSetKind.WORKING,
                        plannedKg = null,
                        plannedRepTarget = null,
                        snapshotTargetKg = 52.5,
                        snapshotRepTarget = RepTarget.Fixed(6),
                        actualKg = 52.5
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        assertTrue(trend.groups.isEmpty())
        assertTrue(requireNotNull(trend.emptyMessage).contains("暂无可比力量 set 趋势"))
        assertTrue(trend.dataQualityRows.any { row -> row.label == "组记录字段不足" })
        assertTrue(trend.dataQualityRows.none { row -> row.label == "使用 setOrder + setKind 降级" })
    }

    @Test
    fun strengthComparableSetTrendDoesNotMixDifferentExerciseOrderOrKind() {
        val differentExercises = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableStrengthSession(
                        id = "bench-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        exerciseId = "barbell-bench-press",
                        sourceSetPlanId = "shared-source"
                    ),
                    comparableStrengthSession(
                        id = "row-one",
                        startedAt = "2026-06-09T18:00:00Z",
                        exerciseId = "one-arm-dumbbell-row",
                        sourceSetPlanId = "shared-source"
                    )
                )
            ).strengthComparableSetTrendUiState
        )
        val differentFallbackOrder = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableStrengthSession(
                        id = "order-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        sourceSetPlanId = null,
                        setOrder = 1,
                        setKind = StrengthSetKind.WORKING
                    ),
                    comparableStrengthSession(
                        id = "order-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        sourceSetPlanId = null,
                        setOrder = 2,
                        setKind = StrengthSetKind.WORKING
                    ),
                    comparableStrengthSession(
                        id = "kind-one",
                        startedAt = "2026-06-10T18:00:00Z",
                        sourceSetPlanId = null,
                        setOrder = 1,
                        setKind = StrengthSetKind.WARMUP
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        assertTrue(differentExercises.groups.isEmpty())
        assertTrue(requireNotNull(differentExercises.emptyMessage).contains("同一 exerciseId"))
        assertTrue(differentFallbackOrder.groups.isEmpty())
        assertTrue(differentFallbackOrder.dataQualityRows.any { row -> row.label == "样本不足" })
    }

    @Test
    fun strengthComparableSetTrendLabelsSubstitutionsWithoutMergingIntoOriginalExercise() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableStrengthSession(
                        id = "original-bench",
                        startedAt = "2026-06-08T18:00:00Z",
                        exerciseId = "barbell-bench-press",
                        sourceSetPlanId = "bench-working-1"
                    ),
                    comparableStrengthSession(
                        id = "substitute-one",
                        startedAt = "2026-06-09T18:00:00Z",
                        exerciseId = "dumbbell-bench-press",
                        sourceSetPlanId = "bench-working-1",
                        substitutedFromExerciseId = "barbell-bench-press"
                    ),
                    comparableStrengthSession(
                        id = "substitute-two",
                        startedAt = "2026-06-10T18:00:00Z",
                        exerciseId = "dumbbell-bench-press",
                        sourceSetPlanId = "bench-working-1",
                        substitutedFromExerciseId = "barbell-bench-press"
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        val group = trend.groups.single()

        assertTrue(group.title.contains("dumbbell-bench-press"))
        assertEquals(2, group.rows.size)
        assertTrue(group.rows.all { row -> requireNotNull(row.substitutionLabel).contains("替换自 杠铃卧推") })
        assertTrue(group.rows.all { row -> requireNotNull(row.substitutionLabel).contains("未并入原动作趋势") })
    }

    @Test
    fun strengthComparableSetTrendUsesHistoricalPlanSnapshotForMissingPlannedFields() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableStrengthSession(
                        id = "snapshot-planned-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        plannedKg = null,
                        plannedRepTarget = null,
                        snapshotTargetKg = 50.0,
                        snapshotRepTarget = RepTarget.Fixed(6),
                        actualKg = 50.0
                    ),
                    comparableStrengthSession(
                        id = "snapshot-planned-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        plannedKg = null,
                        plannedRepTarget = null,
                        snapshotTargetKg = 52.5,
                        snapshotRepTarget = RepTarget.Fixed(6),
                        actualKg = 52.5
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        assertEquals(listOf("50 kg · 6 次", "52.5 kg · 6 次"), trend.groups.single().rows.map { row -> row.plannedLabel })
        assertEquals(listOf("50 kg · 8 次", "52.5 kg · 8 次"), trend.groups.single().rows.map { row -> row.actualLabel })
    }

    @Test
    fun strengthComparableSetTrendDoesNotFallbackToDifferentExerciseWithDuplicateSetPlanIds() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    duplicateSetPlanIdStrengthSession(
                        id = "duplicate-plan-id-row-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        actualKg = 20.0
                    ),
                    duplicateSetPlanIdStrengthSession(
                        id = "duplicate-plan-id-row-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        actualKg = 22.5
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        assertTrue(trend.groups.isEmpty())
        assertTrue(requireNotNull(trend.emptyMessage).contains("暂无可比力量 set 趋势"))
        assertTrue(trend.dataQualityRows.any { row -> row.label == "组记录字段不足" })
        assertTrue(trend.dataQualityRows.any { row -> row.label == "暂无完整 set 样本" })
    }

    @Test
    fun strengthComparableSetTrendFallbackForSubstitutionOnlyUsesSubstitutedFromExerciseBlock() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    substitutedSourceFallbackStrengthSession(
                        id = "substitute-planned-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        originalSnapshotKg = 50.0,
                        actualKg = 50.0
                    ),
                    substitutedSourceFallbackStrengthSession(
                        id = "substitute-planned-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        originalSnapshotKg = 52.5,
                        actualKg = 52.5
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        val group = trend.groups.single()

        assertTrue(group.title.contains("dumbbell-bench-press"))
        assertEquals(listOf("50 kg · 6 次", "52.5 kg · 6 次"), group.rows.map { row -> row.plannedLabel })
        assertFalse(group.rows.any { row -> row.plannedLabel.contains("99 kg") })
        assertTrue(group.rows.all { row -> requireNotNull(row.substitutionLabel).contains("替换自 杠铃卧推") })
    }

    @Test
    fun strengthComparableSetTrendSubstitutionSourceLookupUsesOriginalBlockAsSingleCandidate() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    substitutedCandidateStrengthSession(
                        id = "substitute-source-candidate-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        sourceSetPlanId = "shared-set-plan",
                        originalSetPlanId = "shared-set-plan",
                        substitutedSetPlanId = "shared-set-plan",
                        originalSnapshotKg = 50.0,
                        substitutedSnapshotKg = 99.0,
                        actualKg = 50.0
                    ),
                    substitutedCandidateStrengthSession(
                        id = "substitute-source-candidate-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        sourceSetPlanId = "shared-set-plan",
                        originalSetPlanId = "shared-set-plan",
                        substitutedSetPlanId = "shared-set-plan",
                        originalSnapshotKg = 52.5,
                        substitutedSnapshotKg = 97.5,
                        actualKg = 52.5
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        val group = trend.groups.single()

        assertEquals(listOf("50 kg · 6 次", "52.5 kg · 6 次"), group.rows.map { row -> row.plannedLabel })
        assertFalse(group.rows.any { row -> row.plannedLabel.contains("99 kg") })
        assertFalse(group.rows.any { row -> row.plannedLabel.contains("97.5 kg") })
        assertTrue(group.rows.all { row -> requireNotNull(row.substitutionLabel).contains("替换自 杠铃卧推") })
    }

    @Test
    fun strengthComparableSetTrendSubstitutionMissingSourceDoesNotUseSubstitutedBlockOrFallbackOrder() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    substitutedCandidateStrengthSession(
                        id = "substitute-missing-source-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        sourceSetPlanId = "missing-original-source",
                        originalSetPlanId = "bench-working-1",
                        substitutedSetPlanId = "missing-original-source",
                        originalSnapshotKg = 50.0,
                        substitutedSnapshotKg = 99.0,
                        actualKg = 50.0
                    ),
                    substitutedCandidateStrengthSession(
                        id = "substitute-missing-source-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        sourceSetPlanId = "missing-original-source",
                        originalSetPlanId = "bench-working-1",
                        substitutedSetPlanId = "missing-original-source",
                        originalSnapshotKg = 52.5,
                        substitutedSnapshotKg = 97.5,
                        actualKg = 52.5
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        assertTrue(trend.groups.isEmpty())
        assertTrue(requireNotNull(trend.emptyMessage).contains("暂无可比力量 set 趋势"))
        assertTrue(trend.dataQualityRows.any { row -> row.label == "组记录字段不足" })
        assertTrue(trend.dataQualityRows.none { row -> row.label == "使用 setOrder + setKind 降级" })
    }

    @Test
    fun strengthComparableSetTrendSubstitutionOrderFallbackUsesOriginalBlockOnlyWhenSourceIsMissing() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    substitutedCandidateStrengthSession(
                        id = "substitute-order-fallback-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        sourceSetPlanId = null,
                        originalSetPlanId = "bench-working-1",
                        substitutedSetPlanId = "dumbbell-working-1",
                        originalSnapshotKg = 50.0,
                        substitutedSnapshotKg = 99.0,
                        actualKg = 50.0
                    ),
                    substitutedCandidateStrengthSession(
                        id = "substitute-order-fallback-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        sourceSetPlanId = null,
                        originalSetPlanId = "bench-working-1",
                        substitutedSetPlanId = "dumbbell-working-1",
                        originalSnapshotKg = 52.5,
                        substitutedSnapshotKg = 97.5,
                        actualKg = 52.5
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        val group = trend.groups.single()

        assertTrue(group.ruleLabel.contains("setOrder + setKind"))
        assertEquals(listOf("50 kg · 6 次", "52.5 kg · 6 次"), group.rows.map { row -> row.plannedLabel })
        assertFalse(group.rows.any { row -> row.plannedLabel.contains("99 kg") })
        assertFalse(group.rows.any { row -> row.plannedLabel.contains("97.5 kg") })
    }

    @Test
    fun strengthComparableSetTrendNonSubstitutionUsesCurrentExerciseAsSingleCandidate() {
        val sourceMissing = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    nonSubstitutedCandidateStrengthSession(
                        id = "non-sub-missing-source-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        sourceSetPlanId = "missing-current-source",
                        currentSetPlanId = "bench-working-1",
                        otherSetPlanId = "missing-current-source",
                        currentSnapshotKg = 50.0,
                        otherSnapshotKg = 99.0,
                        actualKg = 50.0
                    ),
                    nonSubstitutedCandidateStrengthSession(
                        id = "non-sub-missing-source-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        sourceSetPlanId = "missing-current-source",
                        currentSetPlanId = "bench-working-1",
                        otherSetPlanId = "missing-current-source",
                        currentSnapshotKg = 52.5,
                        otherSnapshotKg = 97.5,
                        actualKg = 52.5
                    )
                )
            ).strengthComparableSetTrendUiState
        )
        val orderFallback = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    nonSubstitutedCandidateStrengthSession(
                        id = "non-sub-order-fallback-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        sourceSetPlanId = null,
                        currentSetPlanId = "bench-working-1",
                        otherSetPlanId = "row-working-1",
                        currentSnapshotKg = 50.0,
                        otherSnapshotKg = 99.0,
                        actualKg = 50.0
                    ),
                    nonSubstitutedCandidateStrengthSession(
                        id = "non-sub-order-fallback-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        sourceSetPlanId = null,
                        currentSetPlanId = "bench-working-1",
                        otherSetPlanId = "row-working-1",
                        currentSnapshotKg = 52.5,
                        otherSnapshotKg = 97.5,
                        actualKg = 52.5
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        assertTrue(sourceMissing.groups.isEmpty())
        assertTrue(sourceMissing.dataQualityRows.any { row -> row.label == "组记录字段不足" })
        assertEquals(listOf("50 kg · 6 次", "52.5 kg · 6 次"), orderFallback.groups.single().rows.map { row -> row.plannedLabel })
        assertFalse(orderFallback.groups.single().rows.any { row -> row.plannedLabel.contains("99 kg") })
    }

    @Test
    fun strengthComparableSetTrendDoesNotInventSamplesWhenCriticalFieldsAreMissing() {
        val trend = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    comparableStrengthSession(
                        id = "missing-fields-one",
                        startedAt = "2026-06-08T18:00:00Z",
                        actualKg = null
                    ),
                    comparableStrengthSession(
                        id = "missing-fields-two",
                        startedAt = "2026-06-09T18:00:00Z",
                        effort = null
                    )
                )
            ).strengthComparableSetTrendUiState
        )

        assertTrue(trend.groups.isEmpty())
        assertTrue(requireNotNull(trend.emptyMessage).contains("暂无可比力量 set 趋势"))
        assertTrue(trend.dataQualityRows.any { row -> row.label == "组记录字段不足" })
        assertTrue(trend.dataQualityRows.any { row -> row.label == "暂无完整 set 样本" })
    }

    @Test
    fun strengthComparableSetTrendRecalculatesFromRemainingSessionsAfterCleanup() {
        val remaining = comparableStrengthSession(
            id = "remaining-strength-after-cleanup",
            startedAt = "2026-06-09T18:00:00Z",
            actualKg = 60.0
        )
        val state = buildHistoryScreenState(sessions = listOf(remaining))

        val trend = requireNotNull(state.strengthComparableSetTrendUiState)

        assertTrue(trend.groups.isEmpty())
        assertTrue(requireNotNull(trend.emptyMessage).contains("至少需要 2 条"))
        assertTrue(trend.dataQualityRows.any { row -> row.label == "样本不足" })
    }

    @Test
    fun aggregateModeBreakdownCountsTimedStrengthAndFollowAlong() {
        val charts = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    timedSession(
                        id = "mode-timed-one",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-08T09:00:00Z",
                        totalElapsedSec = 100,
                        effectiveElapsedSec = 80,
                        pausedElapsedSec = 20,
                        actualRestSec = 25,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 30
                    ),
                    timedSession(
                        id = "mode-timed-two",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-09T09:00:00Z",
                        totalElapsedSec = 80,
                        effectiveElapsedSec = 80,
                        pausedElapsedSec = 0,
                        actualRestSec = 10,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 10
                    ),
                    strengthSession(
                        id = "mode-strength",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-09T18:00:00Z",
                        records = emptyList()
                    ),
                    followAlongSession(
                        id = "mode-follow",
                        startedAt = "2026-06-10T18:00:00Z",
                        totalElapsedSec = 40,
                        effectiveElapsedSec = 40
                    )
                )
            ).aggregateChartsUiState
        )

        val rows = charts.modeBreakdown.rows.associateBy { row -> row.label }

        assertEquals(4, charts.modeBreakdown.totalCount)
        assertEquals(2, requireNotNull(rows["计时训练"]).count)
        assertEquals("50%", requireNotNull(rows["计时训练"]).percentLabel)
        assertEquals(1, requireNotNull(rows["力量训练"]).count)
        assertEquals(1, requireNotNull(rows["跟练"]).count)
    }

    @Test
    fun aggregatePlannedRestTrendUsesHistoricalPlanSnapshot() {
        val charts = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    timedSession(
                        id = "old-snapshot-rest",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-08T09:00:00Z",
                        totalElapsedSec = 100,
                        effectiveElapsedSec = 100,
                        pausedElapsedSec = 0,
                        actualRestSec = 20,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 10
                    ),
                    timedSession(
                        id = "new-snapshot-rest",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-09T09:00:00Z",
                        totalElapsedSec = 100,
                        effectiveElapsedSec = 100,
                        pausedElapsedSec = 0,
                        actualRestSec = 20,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 45
                    )
                )
            ).aggregateChartsUiState
        )

        val plannedRestSeries = charts.restTrend.series.single { series -> series.label == "计划休息" }

        assertEquals(listOf(10, 45), plannedRestSeries.points.map { point -> point.value })
    }

    @Test
    fun noHeartRateSourceDoesNotOutputAverageHeartRateTrendData() {
        val charts = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    timedSession(
                        id = "hr-one",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-08T09:00:00Z",
                        totalElapsedSec = 100,
                        effectiveElapsedSec = 80,
                        pausedElapsedSec = 20,
                        actualRestSec = 25,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 30
                    ),
                    timedSession(
                        id = "hr-two",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-09T09:00:00Z",
                        totalElapsedSec = 80,
                        effectiveElapsedSec = 80,
                        pausedElapsedSec = 0,
                        actualRestSec = 10,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 10
                    )
                )
            ).aggregateChartsUiState
        )

        assertFalse(
            listOf(
                charts.countTrend,
                charts.statusTrend,
                charts.elapsedTrend,
                charts.restTrend
            ).flatMap { chart -> chart.series }.any { series -> series.label.contains("心率") }
        )
    }

    @Test
    fun cleanupOptionsAreOnlyBuiltForRealPersistedRecords() {
        assertEquals(null, HistoryScreenState(sessions = emptyList()).cleanupUiState)
        assertEquals(null, buildDefaultHistoryScreenState().cleanupUiState)

        val cleanup = requireNotNull(
            buildHistoryScreenState(
                sessions = listOf(
                    timedSession(
                        id = "cleanup-timed",
                        status = SessionStatus.COMPLETED,
                        planId = "plan-cleanup",
                        totalElapsedSec = 100,
                        effectiveElapsedSec = 80,
                        pausedElapsedSec = 20,
                        actualRestSec = 25,
                        extraRestAdds = emptyList(),
                        plannedRestSec = 30
                    ),
                    strengthSession(
                        id = "cleanup-strength",
                        status = SessionStatus.COMPLETED,
                        startedAt = "2026-06-09T18:00:00Z",
                        planId = "plan-cleanup",
                        records = emptyList()
                    )
                )
            ).cleanupUiState
        )

        assertEquals(2, cleanup.allOption.target.count)
        assertEquals(listOf("清除计划：测试计时记录"), cleanup.planOptions.map { option -> option.label })
        assertEquals(2, cleanup.planOptions.single().target.count)
        assertEquals(listOf("清除日期：2026-06-09", "清除日期：2026-06-08"), cleanup.dateOptions.map { it.label })
        assertTrue(cleanup.description.contains("WorkoutSession"))
        assertTrue(cleanup.description.contains("不会删除训练计划"))
    }

    @Test
    fun cleanupRequestMustBeConfirmedBeforeCommandIsEmitted() {
        val state = buildHistoryScreenState(
            sessions = listOf(
                timedSession(
                    id = "cleanup-unconfirmed",
                    status = SessionStatus.COMPLETED,
                    planId = "plan-cleanup",
                    totalElapsedSec = 100,
                    effectiveElapsedSec = 80,
                    pausedElapsedSec = 20,
                    actualRestSec = 25,
                    extraRestAdds = emptyList(),
                    plannedRestSec = 30
                )
            )
        )
        val target = HistoryCleanupTarget.All(count = 1)

        val requested = state.requestCleanup(target)
        val canceled = requested.cancelCleanup()

        assertEquals(target, requested.pendingCleanupTarget)
        assertNotNull(requested.pendingCleanupDialog)
        assertEquals(listOf("cleanup-unconfirmed"), requested.sessions.map { session -> session.id })
        assertEquals(null, canceled.pendingCleanupTarget)
        assertEquals(listOf("cleanup-unconfirmed"), canceled.sessions.map { session -> session.id })

        val result = requested.confirmCleanup()

        assertEquals(target, result.target)
        assertEquals(null, result.state.pendingCleanupTarget)
        assertEquals(listOf("cleanup-unconfirmed"), result.state.sessions.map { session -> session.id })
        assertTrue(requireNotNull(result.state.statusMessage).contains("Room"))
    }

    @Test
    fun planAndDateCleanupConfirmationCopyDistinguishesTargets() {
        val planDialog = HistoryCleanupTarget.Plan(
            planId = "plan-cleanup",
            planTitle = "测试计划",
            count = 3
        ).let { target ->
            buildHistoryScreenState(listOf(defaultHistorySessions().first())).requestCleanup(target).pendingCleanupDialog
        }.let(::requireNotNull)
        val dateDialog = HistoryCleanupTarget.Date(
            dateLabel = "2026-06-08",
            count = 2
        ).let { target ->
            buildHistoryScreenState(listOf(defaultHistorySessions().first())).requestCleanup(target).pendingCleanupDialog
        }.let(::requireNotNull)

        assertTrue(planDialog.title.contains("按计划"))
        assertTrue(planDialog.message.contains("WorkoutPlan 会保留"))
        assertTrue(planDialog.message.contains("plan snapshot 不会被改写"))
        assertTrue(dateDialog.title.contains("按日期"))
        assertTrue(dateDialog.message.contains("其他日期记录会保留"))
    }

    @Test
    fun statsAndChartsRecalculateFromRemainingSessionsAfterCleanup() {
        val remaining = timedSession(
            id = "remaining-after-cleanup",
            status = SessionStatus.COMPLETED,
            startedAt = "2026-06-09T09:00:00Z",
            totalElapsedSec = 80,
            effectiveElapsedSec = 70,
            pausedElapsedSec = 10,
            actualRestSec = 12,
            extraRestAdds = listOf(15),
            plannedRestSec = 12
        )
        val state = buildHistoryScreenState(sessions = listOf(remaining))

        val stats = requireNotNull(state.recordStats)
        val charts = requireNotNull(state.aggregateChartsUiState)

        assertEquals(1, stats.totalCount)
        assertEquals(80, stats.totalElapsedSec)
        assertEquals(70, stats.effectiveElapsedSec)
        assertEquals(10, stats.pausedElapsedSec)
        assertEquals(12, stats.plannedRestSec)
        assertEquals(12, stats.actualRestSec)
        assertEquals(15, stats.extraRestSec)
        assertEquals(1, charts.modeBreakdown.totalCount)
        assertFalse(charts.countTrend.hasDrawableTrend)
    }

    @Test
    fun timedDetailShowsTotalEffectivePausedAndExtraRestFromRealRecord() {
        val state = buildHistoryScreenState(
            sessions = listOf(
                timedSession(
                    id = "real-timed-detail",
                    status = SessionStatus.COMPLETED,
                    totalElapsedSec = 100,
                    effectiveElapsedSec = 80,
                    pausedElapsedSec = 20,
                    actualRestSec = 25,
                    extraRestAdds = listOf(15, 15),
                    plannedRestSec = 90
                )
            )
        )

        val detailRows = requireNotNull(state.selectedDetail).rows.associateBy { row -> row.label }

        assertEquals("1分40秒", requireNotNull(detailRows["总用时"]).value)
        assertEquals("1分20秒", requireNotNull(detailRows["有效训练时间"]).value)
        assertEquals("20秒", requireNotNull(detailRows["暂停时间"]).value)
        assertEquals("1分30秒", requireNotNull(detailRows["计划休息"]).value)
        assertEquals("25秒", requireNotNull(detailRows["实际休息"]).value)
        assertEquals("30秒", requireNotNull(detailRows["额外休息"]).value)
    }

    @Test
    fun plannedRestStatsUseHistoricalPlanSnapshotOnly() {
        val state = buildHistoryScreenState(
            sessions = listOf(
                timedSession(
                    id = "real-timed-old-snapshot",
                    status = SessionStatus.COMPLETED,
                    totalElapsedSec = 100,
                    effectiveElapsedSec = 100,
                    pausedElapsedSec = 0,
                    actualRestSec = 20,
                    extraRestAdds = emptyList(),
                    plannedRestSec = 90
                )
            )
        )

        assertEquals(90, requireNotNull(state.recordStats).plannedRestSec)
    }

    @Test
    fun strengthDetailUsesRestoredPlanSnapshotForPlannedSetCount() {
        val session = strengthSession(
            id = "real-strength-restored-snapshot",
            status = SessionStatus.COMPLETED,
            startedAt = "2026-06-07T10:00:00Z",
            records = listOf(
                strengthSetRecord("confirmed", actualWeight = WeightValue(50.0, WeightUnit.KG), actualReps = 8)
            ),
            plannedSetCount = 2
        )

        val detail = HistoryScreenState(sessions = listOf(session)).selectedDetail.let(::requireNotNull)
        val plannedCountRow = detail.rows.single { row -> row.label == "确认组数" }

        assertEquals("1 / 2", plannedCountRow.value)
        assertFalse(plannedCountRow.value.endsWith("/ 0"))
    }

    @Test
    fun singleActionWeightAndRepsHistoryUsesStrengthRecords() {
        val trend = buildDefaultHistoryScreenState().actionTrend

        assertTrue(trend.title.contains("杠铃卧推"))
        assertEquals(2, trend.rows.size)
        assertTrue(trend.rows.first().metric.contains("62.5 kg"))
        assertTrue(trend.rows.first().metric.contains("18 次"))
        assertTrue(trend.description.contains("不生成自动加重量建议"))
    }

    @Test
    fun volumeTrendSummarizesSetsRepsAndLoadWithoutTimedSessions() {
        val trend = buildDefaultHistoryScreenState().volumeTrend

        assertEquals("训练容量历史", trend.title)
        assertEquals(1, trend.rows.size)
        assertEquals("1300 kg-reps", trend.rows.first().metric)
        assertTrue(trend.rows.first().helper.contains("3 组"))
        assertTrue(trend.rows.first().helper.contains("28 次"))
        assertTrue(trend.rows.first().helper.contains("同时记录实际重量和次数"))
    }

    @Test
    fun abandonedStrengthSessionDoesNotEnterVolumeTrend() {
        val trend = buildDefaultHistoryScreenState().volumeTrend

        assertEquals(listOf("2026-06-01"), trend.rows.map { row -> row.primary })
        assertFalse(trend.rows.any { row -> row.primary == "2026-05-28" })
    }

    @Test
    fun volumeTrendUsesOnlyRecordsWithActualWeightAndActualReps() {
        val session = strengthSession(
            id = "completed-with-partial-actuals",
            status = SessionStatus.COMPLETED,
            startedAt = "2026-06-02T08:00:00Z",
            records = listOf(
                strengthSetRecord("valid", actualWeight = WeightValue(50.0, WeightUnit.KG), actualReps = 5),
                strengthSetRecord("missing-weight", actualWeight = null, actualReps = 10),
                strengthSetRecord("missing-reps", actualWeight = WeightValue(100.0, WeightUnit.KG), actualReps = null)
            )
        )
        val trend = HistoryScreenState(sessions = listOf(session)).volumeTrend

        assertEquals(1, trend.rows.size)
        assertEquals("250 kg-reps", trend.rows.single().metric)
        assertTrue(trend.rows.single().helper.contains("1 组"))
        assertTrue(trend.rows.single().helper.contains("5 次"))
    }

    @Test
    fun completedStrengthSessionWithoutSummarizableActualRecordsShowsEmptyVolumeTrend() {
        val session = strengthSession(
            id = "completed-without-actuals",
            status = SessionStatus.COMPLETED,
            startedAt = "2026-06-02T08:00:00Z",
            records = listOf(
                strengthSetRecord("missing-weight", actualWeight = null, actualReps = 10),
                strengthSetRecord("missing-reps", actualWeight = WeightValue(100.0, WeightUnit.KG), actualReps = null)
            )
        )
        val trend = HistoryScreenState(sessions = listOf(session)).volumeTrend

        assertTrue(trend.rows.isEmpty())
        assertNotNull(trend.emptyMessage)
    }

    @Test
    fun emptyStateIsHonestAboutHistoryPersistence() {
        val state = HistoryScreenState(sessions = emptyList())

        assertTrue(state.isEmpty)
        assertEquals("暂无训练历史", state.emptyStateTitle)
        assertTrue(state.emptyStateDescription.contains("完成一次计时"))
        assertNotNull(state.actionTrend.emptyMessage)
        assertNotNull(state.volumeTrend.emptyMessage)
    }

    @Test
    fun historyCopyDoesNotUseMedicalOrOverConclusiveLanguage() {
        val state = buildDefaultHistoryScreenState()
        val realTimedState = buildHistoryScreenState(
            sessions = listOf(
                comparableTimedSession(
                    id = "copy-timed-one",
                    startedAt = "2026-06-08T09:00:00Z",
                    actualRestSec = 20,
                    extraRestAdds = listOf(15)
                ),
                comparableTimedSession(
                    id = "copy-timed-two",
                    startedAt = "2026-06-09T09:00:00Z",
                    actualRestSec = 24
                )
            )
        )
        val realStrengthState = buildHistoryScreenState(
            sessions = listOf(
                comparableStrengthSession(
                    id = "copy-strength-one",
                    startedAt = "2026-06-08T18:00:00Z",
                    actualKg = 60.0
                ),
                comparableStrengthSession(
                    id = "copy-strength-two",
                    startedAt = "2026-06-09T18:00:00Z",
                    actualKg = 62.5
                )
            )
        )
        val combinedCopy = buildString {
            append(state.emptyStateDescription)
            state.dateGroups.flatMap { group -> group.items }.forEach { item ->
                append(item.keySummary)
                append(item.statusLabel)
            }
            state.selectedDetail?.rows?.forEach { row ->
                append(row.helper)
                append(row.value)
            }
            append(state.actionTrend.title)
            append(state.actionTrend.description)
            state.actionTrend.rows.forEach { row ->
                append(row.helper)
            }
            append(state.volumeTrend.title)
            append(state.volumeTrend.description)
            state.volumeTrend.rows.forEach { row ->
                append(row.helper)
            }
            append(state.boundaryNote)
            state.recordStatsUiState?.let { stats ->
                append(stats.title)
                append(stats.description)
                stats.rows.forEach { row ->
                    append(row.label)
                    append(row.value)
                    append(row.helper)
                }
            }
            realTimedState.timedComparableRestTrendUiState?.let { trend ->
                append(trend.title)
                append(trend.description)
                trend.groups.forEach { group ->
                    append(group.title)
                    append(group.ruleLabel)
                    group.rows.forEach { row ->
                        append(row.plannedRestLabel)
                        append(row.actualRestLabel)
                        append(row.extraRestLabel)
                        append(row.positionLabel)
                    }
                }
                trend.dataQualityRows.forEach { row -> append(row.helper) }
            }
            realStrengthState.strengthComparableSetTrendUiState?.let { trend ->
                append(trend.title)
                append(trend.description)
                trend.groups.forEach { group ->
                    append(group.title)
                    append(group.ruleLabel)
                    group.rows.forEach { row ->
                        append(row.plannedLabel)
                        append(row.actualLabel)
                        append(row.activeDurationLabel)
                        append(row.actualRestLabel)
                        append(row.effortLabel)
                        append(row.sourceLabel)
                        append(row.substitutionLabel)
                    }
                }
                trend.dataQualityRows.forEach { row -> append(row.helper) }
            }
        }

        assertFalse(combinedCopy.contains("你变强了"))
        assertFalse(combinedCopy.contains("应该加重量"))
        assertFalse(combinedCopy.contains("医疗"))
        assertFalse(combinedCopy.contains("诊断"))
        assertFalse(combinedCopy.contains("心率告警"))
        assertFalse(combinedCopy.contains("平均心率趋势"))
        assertFalse(combinedCopy.contains("E5 接入真实记录"))
    }

    private fun strengthSession(
        id: String,
        status: SessionStatus,
        startedAt: String,
        records: List<StrengthSetRecord>,
        planId: String? = null,
        plannedSetCount: Int = 0,
        totalElapsedSec: Int? = null,
        effectiveElapsedSec: Int? = null,
        pausedElapsedSec: Int? = null,
        plannedRestAfterSetSec: Int? = null,
        stepHistory: List<SessionStepRecord> = emptyList()
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            planId = planId,
            mode = WorkoutMode.STRENGTH,
            planSnapshot = WorkoutPlanSnapshot(
                title = "测试力量记录",
                mode = WorkoutMode.STRENGTH,
                blocks = if (plannedSetCount == 0) {
                    emptyList()
                } else {
                    listOf(
                        StrengthExerciseBlock(
                            id = "bench",
                            order = 1,
                            exerciseId = "barbell-bench-press",
                            target = StrengthExerciseTarget(
                                weight = WeightValue(50.0, WeightUnit.KG),
                                repTarget = RepTarget.Range(8, 12),
                                restAfterSetSec = plannedRestAfterSetSec
                            ),
                            sets = (1..plannedSetCount).map { index ->
                                StrengthSetPlan(
                                    id = "bench-working-$index",
                                    order = index,
                                    kind = StrengthSetKind.WORKING
                                )
                            }
                        )
                    )
                }
            ),
            status = status,
            startedAt = startedAt,
            endedAt = startedAt,
            totalElapsedSec = totalElapsedSec,
            effectiveElapsedSec = effectiveElapsedSec,
            pausedElapsedSec = pausedElapsedSec,
            stepHistory = stepHistory,
            strengthSetRecords = records
        )
    }

    private fun strengthSetRecord(
        id: String,
        actualWeight: WeightValue?,
        actualReps: Int?,
        actualRestAfterSec: Int? = null
    ): StrengthSetRecord {
        return StrengthSetRecord(
            id = id,
            exerciseId = "barbell-bench-press",
            setOrder = 1,
            setKind = StrengthSetKind.WORKING,
            actualWeight = actualWeight,
            actualReps = actualReps,
            actualRestAfterSec = actualRestAfterSec
        )
    }

    private fun timedSession(
        id: String,
        status: SessionStatus,
        startedAt: String = "2026-06-08T09:00:00Z",
        totalElapsedSec: Int,
        effectiveElapsedSec: Int,
        pausedElapsedSec: Int,
        actualRestSec: Int,
        extraRestAdds: List<Int>,
        plannedRestSec: Int,
        planId: String? = null
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            planId = planId,
            mode = WorkoutMode.TIMED,
            planSnapshot = WorkoutPlanSnapshot(
                title = "测试计时记录",
                mode = WorkoutMode.TIMED,
                blocks = listOf(
                    TimedCircuitBlock(
                        id = "snapshot-circuit",
                        order = 1,
                        rounds = 1,
                        items = listOf(
                            TimedExerciseItem(
                                id = "snapshot-work",
                                labelOverride = "工作",
                                stageType = TimedStageType.WORK,
                                workDurationSec = 40,
                                restAfterSec = plannedRestSec
                            )
                        )
                    )
                )
            ),
            status = status,
            startedAt = startedAt,
            endedAt = startedAt,
            totalElapsedSec = totalElapsedSec,
            effectiveElapsedSec = effectiveElapsedSec,
            pausedElapsedSec = pausedElapsedSec,
            stepHistory = listOf(
                SessionStepRecord(
                    stepId = "$id-work",
                    kind = SessionStepKind.TIMED_WORK,
                    startedAt = "2026-06-08T09:00:00Z",
                    endedAt = "2026-06-08T09:01:00Z",
                    actualDurationSec = 40
                ),
                SessionStepRecord(
                    stepId = "$id-rest",
                    kind = SessionStepKind.TIMED_REST,
                    startedAt = "2026-06-08T09:01:00Z",
                    endedAt = "2026-06-08T09:02:00Z",
                    actualDurationSec = actualRestSec
                )
            ),
            timedRestExtensionRecords = extraRestAdds.runningFold(0) { total, added -> total + added }
                .drop(1)
                .mapIndexed { index, cumulative ->
                    TimedRestExtensionRecord(
                        id = "$id-rest-extension-$index",
                        stepId = "$id-rest",
                        stepIndex = 1,
                        restStageTitle = "休息",
                        addedSec = extraRestAdds[index],
                        plannedRestSec = plannedRestSec,
                        restElapsedBeforeExtensionSec = 5,
                        extensionAtRemainingSec = 5,
                        cumulativeExtraRestSec = cumulative,
                        eventElapsedSec = 40 + index
                    )
                }
        )
    }

    private fun comparableTimedSession(
        id: String,
        startedAt: String,
        roundIndex: Int = 1,
        plannedRestSec: Int = 20,
        actualRestSec: Int,
        extraRestAdds: List<Int> = emptyList(),
        pausedElapsedSec: Int = 0,
        malformedExtraRestPosition: Boolean = false,
        includeRestStepRecord: Boolean = true
    ): WorkoutSession {
        val stepIndex = if (roundIndex == 1) 1 else 3
        val restStepId = "snapshot-circuit-r$roundIndex-snapshot-work-rest"
        val restStepRecord = SessionStepRecord(
            stepId = restStepId,
            kind = SessionStepKind.TIMED_REST,
            startedAt = startedAt,
            endedAt = startedAt,
            actualDurationSec = actualRestSec
        )
        return WorkoutSession(
            id = id,
            planId = "plan-comparable-timed",
            mode = WorkoutMode.TIMED,
            planSnapshot = WorkoutPlanSnapshot(
                title = "可比计时记录",
                mode = WorkoutMode.TIMED,
                blocks = listOf(
                    TimedCircuitBlock(
                        id = "snapshot-circuit",
                        order = 1,
                        rounds = 2,
                        items = listOf(
                            TimedExerciseItem(
                                id = "snapshot-work",
                                labelOverride = "工作",
                                stageType = TimedStageType.WORK,
                                workDurationSec = 40,
                                restAfterSec = plannedRestSec
                            )
                        )
                    )
                )
            ),
            status = SessionStatus.COMPLETED,
            startedAt = startedAt,
            endedAt = startedAt,
            totalElapsedSec = 80 + actualRestSec,
            effectiveElapsedSec = 80 + actualRestSec,
            pausedElapsedSec = pausedElapsedSec,
            stepHistory = if (includeRestStepRecord) listOf(restStepRecord) else emptyList(),
            timedRestExtensionRecords = extraRestAdds.runningFold(0) { total, added -> total + added }
                .drop(1)
                .mapIndexed { index, cumulative ->
                    TimedRestExtensionRecord(
                        id = "$id-extra-$index",
                        stepId = restStepId,
                        stepIndex = if (malformedExtraRestPosition) -1 else stepIndex,
                        roundIndex = if (malformedExtraRestPosition) null else roundIndex,
                        restStageId = if (malformedExtraRestPosition) null else "snapshot-work",
                        restStageTitle = "休息",
                        previousStageId = if (malformedExtraRestPosition) null else "snapshot-work",
                        previousStageTitle = "工作",
                        addedSec = extraRestAdds[index],
                        plannedRestSec = plannedRestSec,
                        restElapsedBeforeExtensionSec = 5,
                        extensionAtRemainingSec = 5,
                        cumulativeExtraRestSec = cumulative,
                        eventElapsedSec = 40 + index
                    )
                }
        )
    }

    private fun comparableTimedCompositionSession(
        id: String,
        startedAt: String,
        rounds: Int,
        restBetweenRoundsSec: Int,
        targets: List<TimedCompositionTarget> = listOf(
            compositionActionTarget(),
            compositionRestTarget()
        ),
        targetRestActualSec: Int = 4,
        betweenRoundRestActualSec: Int = restBetweenRoundsSec,
        extraTargetRestAdds: List<Int> = emptyList(),
        extraBetweenRoundRestAdds: List<Int> = emptyList()
    ): WorkoutSession {
        val block = TimedCompositionBlock(
            id = "composition-v2",
            order = 1,
            title = "Composition v2",
            warmupSec = 3,
            cooldownSec = 2,
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec,
            stageGroups = listOf(
                TimedCompositionStageGroup(
                    id = "group-main",
                    order = 1,
                    name = "Main group",
                    colorHex = TimedStageType.WORK.defaultColorHex,
                    targets = targets
                )
            )
        )
        val steps = TimedCompositionTimelineAdapter.expand(block).steps
        val stepHistory = steps.map { step ->
            SessionStepRecord(
                stepId = step.id,
                kind = if (step.isRest) SessionStepKind.TIMED_REST else SessionStepKind.TIMED_WORK,
                startedAt = startedAt,
                endedAt = startedAt,
                actualDurationSec = when {
                    step.targetKind.contractValue == TimedCompositionTargetKind.REST.contractValue -> targetRestActualSec
                    step.timelineStageKind.contractValue == "between_round_rest" -> betweenRoundRestActualSec
                    else -> step.plannedDurationSec
                }
            )
        }
        val targetRestStep = steps.firstOrNull { step ->
            step.targetKind.contractValue == TimedCompositionTargetKind.REST.contractValue
        }
        val betweenRoundRestStep = steps.firstOrNull { step ->
            step.timelineStageKind.contractValue == "between_round_rest"
        }
        val restExtensions = buildList {
            targetRestStep?.let { step ->
                addAll(step.restExtensionRecordsFor(id, steps, extraTargetRestAdds))
            }
            betweenRoundRestStep?.let { step ->
                addAll(step.restExtensionRecordsFor(id, steps, extraBetweenRoundRestAdds))
            }
        }

        return WorkoutSession(
            id = id,
            planId = "plan-comparable-composition-v2",
            mode = WorkoutMode.TIMED,
            planSnapshot = WorkoutPlanSnapshot(
                title = "可比 v2 计时记录",
                mode = WorkoutMode.TIMED,
                blocks = listOf(block)
            ),
            status = SessionStatus.COMPLETED,
            startedAt = startedAt,
            endedAt = startedAt,
            totalElapsedSec = stepHistory.sumOf { record -> record.actualDurationSec ?: 0 },
            effectiveElapsedSec = stepHistory.sumOf { record -> record.actualDurationSec ?: 0 },
            pausedElapsedSec = 0,
            stepHistory = stepHistory,
            timedRestExtensionRecords = restExtensions
        )
    }

    private fun TimedCompositionTimelineStep.restExtensionRecordsFor(
        sessionId: String,
        steps: List<TimedCompositionTimelineStep>,
        additions: List<Int>
    ): List<TimedRestExtensionRecord> {
        val stepIndex = steps.indexOfFirst { step -> step.id == id }
        val previousWorkStep = steps.take(stepIndex).lastOrNull { step -> !step.isRest }
        return additions.runningFold(0) { total, added -> total + added }
            .drop(1)
            .mapIndexed { index, cumulative ->
                TimedRestExtensionRecord(
                    id = "$sessionId-${id}-extra-$index",
                    stepId = id,
                    stepIndex = stepIndex,
                    roundIndex = roundIndex,
                    restStageId = targetId,
                    restStageTitle = displayName,
                    previousStageId = previousWorkStep?.targetId,
                    previousStageTitle = previousWorkStep?.displayName,
                    addedSec = additions[index],
                    plannedRestSec = plannedDurationSec,
                    restElapsedBeforeExtensionSec = 1,
                    extensionAtRemainingSec = plannedDurationSec - 1,
                    cumulativeExtraRestSec = cumulative,
                    eventElapsedSec = stepIndex + index
                )
            }
    }

    private fun compositionActionTarget(
        durationSec: Int = 5
    ): TimedCompositionTarget {
        return TimedCompositionTarget(
            id = "target-action",
            order = 1,
            name = "Action target",
            kind = TimedCompositionTargetKind.ACTION,
            durationSec = durationSec,
            colorHex = TimedStageType.WORK.defaultColorHex
        )
    }

    private fun compositionRestTarget(
        durationSec: Int = 4
    ): TimedCompositionTarget {
        return TimedCompositionTarget(
            id = "target-rest",
            order = 2,
            name = "Rest target",
            kind = TimedCompositionTargetKind.REST,
            durationSec = durationSec,
            colorHex = TimedStageType.REST.defaultColorHex
        )
    }

    private fun comparableStrengthSession(
        id: String,
        startedAt: String,
        exerciseId: String = "barbell-bench-press",
        sourceSetPlanId: String? = "bench-working-1",
        setOrder: Int = 1,
        snapshotSetPlanId: String = sourceSetPlanId ?: "bench-working-$setOrder",
        setKind: StrengthSetKind = StrengthSetKind.WORKING,
        plannedKg: Double? = 60.0,
        plannedRepTarget: RepTarget? = RepTarget.Range(8, 12),
        snapshotTargetKg: Double? = plannedKg,
        snapshotRepTarget: RepTarget? = plannedRepTarget,
        actualKg: Double? = 60.0,
        actualReps: Int? = 8,
        activeDurationSec: Int? = 40,
        actualRestAfterSec: Int? = 90,
        effort: SetEffort? = SetEffort.GOOD,
        substitutedFromExerciseId: String? = null
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            planId = "plan-comparable-strength",
            mode = WorkoutMode.STRENGTH,
            planSnapshot = WorkoutPlanSnapshot(
                title = "可比力量记录",
                mode = WorkoutMode.STRENGTH,
                blocks = listOf(
                    StrengthExerciseBlock(
                        id = "bench",
                        order = 1,
                        exerciseId = exerciseId,
                        target = StrengthExerciseTarget(
                            weight = snapshotTargetKg?.let { kg -> WeightValue(kg, WeightUnit.KG) },
                            repTarget = snapshotRepTarget,
                            restAfterSetSec = 90
                        ),
                        sets = listOf(
                            StrengthSetPlan(
                                id = snapshotSetPlanId,
                                order = setOrder,
                                kind = setKind,
                                targetWeight = snapshotTargetKg?.let { kg -> WeightValue(kg, WeightUnit.KG) },
                                repTarget = snapshotRepTarget,
                                restAfterSec = 90
                            )
                        )
                    )
                )
            ),
            status = SessionStatus.COMPLETED,
            startedAt = startedAt,
            endedAt = startedAt,
            totalElapsedSec = (activeDurationSec ?: 0) + (actualRestAfterSec ?: 0),
            effectiveElapsedSec = (activeDurationSec ?: 0) + (actualRestAfterSec ?: 0),
            pausedElapsedSec = 0,
            strengthSetRecords = listOf(
                StrengthSetRecord(
                    id = "$id-set",
                    exerciseId = exerciseId,
                    sourceSetPlanId = sourceSetPlanId,
                    setOrder = setOrder,
                    setKind = setKind,
                    plannedWeight = plannedKg?.let { kg -> WeightValue(kg, WeightUnit.KG) },
                    plannedRepTarget = plannedRepTarget,
                    actualWeight = actualKg?.let { kg -> WeightValue(kg, WeightUnit.KG) },
                    actualReps = actualReps,
                    activeDurationSec = activeDurationSec,
                    actualRestAfterSec = actualRestAfterSec,
                    effort = effort,
                    substitutedFromExerciseId = substitutedFromExerciseId
                )
            )
        )
    }

    private fun duplicateSetPlanIdStrengthSession(
        id: String,
        startedAt: String,
        actualKg: Double
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            planId = "plan-duplicate-set-plan-id",
            mode = WorkoutMode.STRENGTH,
            planSnapshot = WorkoutPlanSnapshot(
                title = "重复 set id 力量记录",
                mode = WorkoutMode.STRENGTH,
                blocks = listOf(
                    StrengthExerciseBlock(
                        id = "bench",
                        order = 1,
                        exerciseId = "barbell-bench-press",
                        target = StrengthExerciseTarget(
                            weight = WeightValue(100.0, WeightUnit.KG),
                            repTarget = RepTarget.Fixed(3),
                            restAfterSetSec = 90
                        ),
                        sets = listOf(
                            StrengthSetPlan(
                                id = "shared-set-plan",
                                order = 1,
                                kind = StrengthSetKind.WORKING,
                                targetWeight = WeightValue(100.0, WeightUnit.KG),
                                repTarget = RepTarget.Fixed(3),
                                restAfterSec = 90
                            )
                        )
                    ),
                    StrengthExerciseBlock(
                        id = "row",
                        order = 2,
                        exerciseId = "one-arm-dumbbell-row",
                        sets = listOf(
                            StrengthSetPlan(
                                id = "shared-set-plan",
                                order = 1,
                                kind = StrengthSetKind.WORKING,
                                restAfterSec = 90
                            )
                        )
                    )
                )
            ),
            status = SessionStatus.COMPLETED,
            startedAt = startedAt,
            endedAt = startedAt,
            totalElapsedSec = 130,
            effectiveElapsedSec = 130,
            pausedElapsedSec = 0,
            strengthSetRecords = listOf(
                StrengthSetRecord(
                    id = "$id-set",
                    exerciseId = "one-arm-dumbbell-row",
                    sourceSetPlanId = "shared-set-plan",
                    setOrder = 1,
                    setKind = StrengthSetKind.WORKING,
                    actualWeight = WeightValue(actualKg, WeightUnit.KG),
                    actualReps = 8,
                    activeDurationSec = 40,
                    actualRestAfterSec = 90,
                    effort = SetEffort.GOOD
                )
            )
        )
    }

    private fun substitutedSourceFallbackStrengthSession(
        id: String,
        startedAt: String,
        originalSnapshotKg: Double,
        actualKg: Double
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            planId = "plan-substitution-source-fallback",
            mode = WorkoutMode.STRENGTH,
            planSnapshot = WorkoutPlanSnapshot(
                title = "替换 planned 回退力量记录",
                mode = WorkoutMode.STRENGTH,
                blocks = listOf(
                    StrengthExerciseBlock(
                        id = "unrelated-machine",
                        order = 1,
                        exerciseId = "machine-chest-press",
                        target = StrengthExerciseTarget(
                            weight = WeightValue(99.0, WeightUnit.KG),
                            repTarget = RepTarget.Fixed(1),
                            restAfterSetSec = 90
                        ),
                        sets = listOf(
                            StrengthSetPlan(
                                id = "bench-working-1",
                                order = 1,
                                kind = StrengthSetKind.WORKING,
                                targetWeight = WeightValue(99.0, WeightUnit.KG),
                                repTarget = RepTarget.Fixed(1),
                                restAfterSec = 90
                            )
                        )
                    ),
                    StrengthExerciseBlock(
                        id = "original-bench",
                        order = 2,
                        exerciseId = "barbell-bench-press",
                        target = StrengthExerciseTarget(
                            weight = WeightValue(originalSnapshotKg, WeightUnit.KG),
                            repTarget = RepTarget.Fixed(6),
                            restAfterSetSec = 90
                        ),
                        sets = listOf(
                            StrengthSetPlan(
                                id = "bench-working-1",
                                order = 1,
                                kind = StrengthSetKind.WORKING,
                                targetWeight = WeightValue(originalSnapshotKg, WeightUnit.KG),
                                repTarget = RepTarget.Fixed(6),
                                restAfterSec = 90
                            )
                        )
                    )
                )
            ),
            status = SessionStatus.COMPLETED,
            startedAt = startedAt,
            endedAt = startedAt,
            totalElapsedSec = 130,
            effectiveElapsedSec = 130,
            pausedElapsedSec = 0,
            strengthSetRecords = listOf(
                StrengthSetRecord(
                    id = "$id-set",
                    exerciseId = "dumbbell-bench-press",
                    sourceSetPlanId = "bench-working-1",
                    setOrder = 1,
                    setKind = StrengthSetKind.WORKING,
                    actualWeight = WeightValue(actualKg, WeightUnit.KG),
                    actualReps = 8,
                    activeDurationSec = 40,
                    actualRestAfterSec = 90,
                    effort = SetEffort.GOOD,
                    substitutedFromExerciseId = "barbell-bench-press"
                )
            )
        )
    }

    private fun substitutedCandidateStrengthSession(
        id: String,
        startedAt: String,
        sourceSetPlanId: String?,
        originalSetPlanId: String,
        substitutedSetPlanId: String,
        originalSnapshotKg: Double,
        substitutedSnapshotKg: Double,
        actualKg: Double
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            planId = "plan-substitution-candidate",
            mode = WorkoutMode.STRENGTH,
            planSnapshot = WorkoutPlanSnapshot(
                title = "替换候选力量记录",
                mode = WorkoutMode.STRENGTH,
                blocks = listOf(
                    StrengthExerciseBlock(
                        id = "substituted-dumbbell",
                        order = 1,
                        exerciseId = "dumbbell-bench-press",
                        target = StrengthExerciseTarget(
                            weight = WeightValue(substitutedSnapshotKg, WeightUnit.KG),
                            repTarget = RepTarget.Fixed(1),
                            restAfterSetSec = 90
                        ),
                        sets = listOf(
                            StrengthSetPlan(
                                id = substitutedSetPlanId,
                                order = 1,
                                kind = StrengthSetKind.WORKING,
                                targetWeight = WeightValue(substitutedSnapshotKg, WeightUnit.KG),
                                repTarget = RepTarget.Fixed(1),
                                restAfterSec = 90
                            )
                        )
                    ),
                    StrengthExerciseBlock(
                        id = "original-bench",
                        order = 2,
                        exerciseId = "barbell-bench-press",
                        target = StrengthExerciseTarget(
                            weight = WeightValue(originalSnapshotKg, WeightUnit.KG),
                            repTarget = RepTarget.Fixed(6),
                            restAfterSetSec = 90
                        ),
                        sets = listOf(
                            StrengthSetPlan(
                                id = originalSetPlanId,
                                order = 1,
                                kind = StrengthSetKind.WORKING,
                                targetWeight = WeightValue(originalSnapshotKg, WeightUnit.KG),
                                repTarget = RepTarget.Fixed(6),
                                restAfterSec = 90
                            )
                        )
                    )
                )
            ),
            status = SessionStatus.COMPLETED,
            startedAt = startedAt,
            endedAt = startedAt,
            totalElapsedSec = 130,
            effectiveElapsedSec = 130,
            pausedElapsedSec = 0,
            strengthSetRecords = listOf(
                StrengthSetRecord(
                    id = "$id-set",
                    exerciseId = "dumbbell-bench-press",
                    sourceSetPlanId = sourceSetPlanId,
                    setOrder = 1,
                    setKind = StrengthSetKind.WORKING,
                    actualWeight = WeightValue(actualKg, WeightUnit.KG),
                    actualReps = 8,
                    activeDurationSec = 40,
                    actualRestAfterSec = 90,
                    effort = SetEffort.GOOD,
                    substitutedFromExerciseId = "barbell-bench-press"
                )
            )
        )
    }

    private fun nonSubstitutedCandidateStrengthSession(
        id: String,
        startedAt: String,
        sourceSetPlanId: String?,
        currentSetPlanId: String,
        otherSetPlanId: String,
        currentSnapshotKg: Double,
        otherSnapshotKg: Double,
        actualKg: Double
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            planId = "plan-non-substitution-candidate",
            mode = WorkoutMode.STRENGTH,
            planSnapshot = WorkoutPlanSnapshot(
                title = "非替换候选力量记录",
                mode = WorkoutMode.STRENGTH,
                blocks = listOf(
                    StrengthExerciseBlock(
                        id = "other-row",
                        order = 1,
                        exerciseId = "one-arm-dumbbell-row",
                        target = StrengthExerciseTarget(
                            weight = WeightValue(otherSnapshotKg, WeightUnit.KG),
                            repTarget = RepTarget.Fixed(1),
                            restAfterSetSec = 90
                        ),
                        sets = listOf(
                            StrengthSetPlan(
                                id = otherSetPlanId,
                                order = 1,
                                kind = StrengthSetKind.WORKING,
                                targetWeight = WeightValue(otherSnapshotKg, WeightUnit.KG),
                                repTarget = RepTarget.Fixed(1),
                                restAfterSec = 90
                            )
                        )
                    ),
                    StrengthExerciseBlock(
                        id = "current-bench",
                        order = 2,
                        exerciseId = "barbell-bench-press",
                        target = StrengthExerciseTarget(
                            weight = WeightValue(currentSnapshotKg, WeightUnit.KG),
                            repTarget = RepTarget.Fixed(6),
                            restAfterSetSec = 90
                        ),
                        sets = listOf(
                            StrengthSetPlan(
                                id = currentSetPlanId,
                                order = 1,
                                kind = StrengthSetKind.WORKING,
                                targetWeight = WeightValue(currentSnapshotKg, WeightUnit.KG),
                                repTarget = RepTarget.Fixed(6),
                                restAfterSec = 90
                            )
                        )
                    )
                )
            ),
            status = SessionStatus.COMPLETED,
            startedAt = startedAt,
            endedAt = startedAt,
            totalElapsedSec = 130,
            effectiveElapsedSec = 130,
            pausedElapsedSec = 0,
            strengthSetRecords = listOf(
                StrengthSetRecord(
                    id = "$id-set",
                    exerciseId = "barbell-bench-press",
                    sourceSetPlanId = sourceSetPlanId,
                    setOrder = 1,
                    setKind = StrengthSetKind.WORKING,
                    actualWeight = WeightValue(actualKg, WeightUnit.KG),
                    actualReps = 8,
                    activeDurationSec = 40,
                    actualRestAfterSec = 90,
                    effort = SetEffort.GOOD
                )
            )
        )
    }

    private fun followAlongSession(
        id: String,
        startedAt: String = "2026-06-08T11:00:00Z",
        totalElapsedSec: Int,
        effectiveElapsedSec: Int
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            mode = WorkoutMode.FOLLOW_ALONG,
            planSnapshot = WorkoutPlanSnapshot(
                title = "测试跟练记录",
                mode = WorkoutMode.FOLLOW_ALONG,
                blocks = emptyList()
            ),
            status = SessionStatus.COMPLETED,
            startedAt = startedAt,
            endedAt = startedAt,
            totalElapsedSec = totalElapsedSec,
            effectiveElapsedSec = effectiveElapsedSec,
            pausedElapsedSec = 0
        )
    }
}
