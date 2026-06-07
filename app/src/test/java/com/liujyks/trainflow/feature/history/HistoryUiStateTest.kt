package com.liujyks.trainflow.feature.history

import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthExerciseTarget
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StrengthSetRecord
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
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
        }

        assertFalse(combinedCopy.contains("你变强了"))
        assertFalse(combinedCopy.contains("应该加重量"))
        assertFalse(combinedCopy.contains("医疗"))
        assertFalse(combinedCopy.contains("诊断"))
        assertFalse(combinedCopy.contains("心率告警"))
        assertFalse(combinedCopy.contains("E5 接入真实记录"))
    }

    private fun strengthSession(
        id: String,
        status: SessionStatus,
        startedAt: String,
        records: List<StrengthSetRecord>,
        plannedSetCount: Int = 0
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
                                repTarget = RepTarget.Range(8, 12)
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
            strengthSetRecords = records
        )
    }

    private fun strengthSetRecord(
        id: String,
        actualWeight: WeightValue?,
        actualReps: Int?
    ): StrengthSetRecord {
        return StrengthSetRecord(
            id = id,
            exerciseId = "barbell-bench-press",
            setOrder = 1,
            setKind = StrengthSetKind.WORKING,
            actualWeight = actualWeight,
            actualReps = actualReps
        )
    }
}
