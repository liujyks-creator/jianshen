package com.liujyks.trainflow.feature.history

import com.liujyks.trainflow.core.model.SessionStatus
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

        assertEquals(null, charts.averageHeartRateTrend)
        assertTrue(charts.heartRateUnavailableText.contains("未获取心率"))
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
        plannedSetCount: Int = 0,
        totalElapsedSec: Int? = null,
        effectiveElapsedSec: Int? = null,
        pausedElapsedSec: Int? = null,
        plannedRestAfterSetSec: Int? = null,
        stepHistory: List<SessionStepRecord> = emptyList()
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
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
        plannedRestSec: Int
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
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
