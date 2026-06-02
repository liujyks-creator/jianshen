package com.liujyks.trainflow.feature.history

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
        assertTrue(detail.sourceNote.contains("内存态 session seed"))
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
        assertEquals(2, trend.rows.size)
        assertEquals("1300 kg-reps", trend.rows.first().metric)
        assertTrue(trend.rows.first().helper.contains("3 组"))
        assertTrue(trend.rows.first().helper.contains("28 次"))
        assertTrue(trend.rows.first().helper.contains("计时训练不纳入重量容量"))
    }

    @Test
    fun emptyStateIsHonestAboutHistoryPersistence() {
        val state = HistoryScreenState(sessions = emptyList())

        assertTrue(state.isEmpty)
        assertEquals("暂无训练历史", state.emptyStateTitle)
        assertTrue(state.emptyStateDescription.contains("真实历史保存将在后续接入"))
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
        }

        assertFalse(combinedCopy.contains("你变强了"))
        assertFalse(combinedCopy.contains("应该加重量"))
        assertFalse(combinedCopy.contains("医疗"))
        assertFalse(combinedCopy.contains("诊断"))
        assertFalse(combinedCopy.contains("心率告警"))
    }
}
