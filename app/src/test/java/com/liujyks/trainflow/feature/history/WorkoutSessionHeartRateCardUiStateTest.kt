package com.liujyks.trainflow.feature.history

import com.liujyks.trainflow.core.data.StrictSessionExecution
import com.liujyks.trainflow.core.data.WorkoutSessionHistoricalResult
import com.liujyks.trainflow.core.data.WorkoutSessionStrictReadResult
import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.TimedRestExtensionRecordEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.database.parseCanonicalJson
import org.junit.Assert.*
import org.junit.Test

class WorkoutSessionHeartRateCardUiStateTest {
    @Test
    fun persistedHeartRateCardStateMatrix() {
        val session = WorkoutSessionEntity("card-session", mode = "timed", status = "completed",
            planSnapshotJson = "{\"title\":\"冻结计划\",\"mode\":\"timed\",\"blocks\":[]}",
            pausedElapsedSec = 80)
        val plan = parseCanonicalJson(session.planSnapshotJson) as CanonicalJsonValue.Obj
        val execution = StrictSessionExecution(emptyList(), listOf(TimedRestExtensionRecordEntity(
            "extension", session.id, "rest", 1, restStageTitle = "休息", addedSec = 20,
            plannedRestSec = 30, restElapsedBeforeExtensionSec = 10, extensionAtRemainingSec = 20,
            cumulativeExtraRestSec = 20, eventElapsedSec = 20)), emptyList())
        val recording = HeartRateRecordingEntity("card-recording", session.id, "terminal", 0, 0,
            110000, 3, 1, "ble_hrs", 1, 1, effectiveMaxBpm = 200,
            originalAnalysisVersion = 1)
        fun resolved(graph: CanonicalSessionGraphV1) = WorkoutSessionHistoricalResult.Resolved(
            WorkoutSessionStrictReadResult.CanonicalTerminal(graph, plan, execution),
            "冻结计划", "timed", "zh-CN", emptyList(), emptyList())
        fun fixture(covered: Long, eligible: Long = 10000, count: Long = 2,
            primary: Long = count, noZones: Boolean = false): WorkoutSessionHistoricalResult.Resolved {
            val coverage = when {
                eligible == 0L -> "no_eligible_duration"
                covered >= 8000 -> "normal"
                covered >= 5000 -> "partial"
                else -> "insufficient"
            }
            val snapshot = HeartRateAnalysisSnapshotEntity(
                recordingId = recording.recordingId, analysisVersion = 1,
                createdAt = "2026-10-05T00:00:00Z", inputLastMutationSequence = 3,
                sampleStatus = when { count == 0L -> "no_canonical_samples"
                    primary == 0L -> "canonical_only_excluded"; else -> "primary_points_available" },
                coverageStatus = coverage,
                zoneStatus = if (noZones) "unavailable_no_effective_max" else "available",
                canonicalSampleCount = count, primaryPointSampleCount = primary,
                eligibleDurationMs = eligible, coveredDurationMs = covered,
                coverageBasisPoints = if (eligible == 0L) null else covered.toInt(),
                weightedBpmMs = if (covered == 0L) null else covered * 112,
                observedAvgBpm = if (covered == 0L) null else 112,
                observedMaxBpm = if (primary == 0L) null else 151,
                highestOffsetMs = if (primary == 0L) null else 1000,
                highestMutationSequence = if (primary == 0L) null else 1,
                highestSampleSequence = if (primary == 0L) null else 1,
                analysisConfigJson = "{}", zoneDurationsJson = if (noZones || eligible == 0L) null else "{}",
                phaseAggregatesJson = "{}", durationBreakdownJson = "{}",
                qualityReasonsJson = if (noZones && eligible > 0)
                    "{\"sessionReasons\":[{\"reasonCode\":\"unavailable_no_effective_max\",\"durationMs\":null}]}"
                    else "{\"sessionReasons\":[]}")
            return resolved(CanonicalSessionGraphV1(session,
                recording = recording.copy(effectiveMaxBpm = if (noZones) null else 200),
                samples = List(count.toInt()) { HeartRateSampleEntity(recording.recordingId,
                    it.toLong(), it * 1000L, 1, if (it == 1) 151 else 112) }, snapshots = listOf(snapshot)))
        }
        assertEquals(WorkoutSessionHeartRateCardUiState.Hidden,
            buildWorkoutSessionHeartRateCardUiState(resolved(CanonicalSessionGraphV1(session))))
        val legacy = WorkoutSessionHistoricalResult.Resolved(
            WorkoutSessionStrictReadResult.LegacyTerminal(session, plan, execution),
            "冻结计划", "timed", "zh-CN", emptyList(), emptyList())
        assertEquals(WorkoutSessionHeartRateCardUiState.Hidden, buildWorkoutSessionHeartRateCardUiState(legacy))
        val nonterminal = WorkoutSessionHistoricalResult.Forwarded(WorkoutSessionStrictReadResult.Nonterminal(
            session.copy(status = "running"), "canonical", CanonicalSessionGraphV1(session,
                recording = recording.copy(status = "running", endedOffsetMs = null,
                    endedMutationSequence = null, originalAnalysisVersion = null)), execution))
        assertEquals(WorkoutSessionHeartRateCardUiState.Unavailable("本次心率记录尚未形成终态分析。"),
            buildWorkoutSessionHeartRateCardUiState(nonterminal))
        assertEquals(WorkoutSessionHeartRateCardUiState.Hidden, buildWorkoutSessionHeartRateCardUiState(
            WorkoutSessionHistoricalResult.Forwarded(WorkoutSessionStrictReadResult.NotFound)))
        val rejected = WorkoutSessionHistoricalResult.Forwarded(
            WorkoutSessionStrictReadResult.Unavailable("invalid_session_graph"))
        assertEquals(WorkoutSessionHeartRateCardUiState.ReadError(rejected, null),
            buildWorkoutSessionHeartRateCardUiState(rejected))
        val cause = IllegalStateException("read-failure")
        assertSame(cause, (buildWorkoutSessionHeartRateCardUiState(null, cause) as
            WorkoutSessionHeartRateCardUiState.ReadError).cause)
        fun card(source: WorkoutSessionHistoricalResult) = buildWorkoutSessionHeartRateCardUiState(source) as
            WorkoutSessionHeartRateCardUiState.Content
        val noEligible = card(fixture(0, eligible = 0, count = 0, primary = 0))
        assertEquals("no_eligible_duration", noEligible.status)
        assertEquals(listOf("本次没有可纳入主要训练心率统计的时长。"), noEligible.messages)
        assertTrue(noEligible.metrics.isEmpty())
        val zero = card(fixture(0, count = 0, primary = 0))
        assertEquals("zero_samples", zero.status)
        assertEquals(listOf("已开启心率记录，但本次未获得有效心率数据。"), zero.messages)
        assertTrue(zero.metrics.isEmpty())
        val excluded = card(fixture(0, primary = 0))
        assertEquals("insufficient", excluded.status)
        assertEquals("canonical_only_excluded", excluded.sampleStatus)
        assertEquals(listOf("本次有效心率覆盖不足 50%，暂不生成自动摘要。",
            "已记录心率均不计入主要训练统计。"), excluded.messages)
        assertEquals(listOf("未获得可统计的平均", "未获得可统计的最高"), excluded.metrics)
        data class Expected(val covered: Long, val status: String, val percent: Int, val message: String,
            val metrics: List<String>)
        val rows = listOf(
            Expected(4999, "insufficient", 49, "本次有效心率覆盖不足 50%，暂不生成自动摘要。",
                listOf("已记录片段平均 112 bpm", "已记录片段最高 151 bpm")),
            Expected(5000, "partial", 50, "本次心率记录不完整，以下结果仅基于已记录片段。",
                listOf("已记录片段平均 112 bpm", "已记录片段最高 151 bpm")),
            Expected(7999, "partial", 79, "本次心率记录不完整，以下结果仅基于已记录片段。",
                listOf("已记录片段平均 112 bpm", "已记录片段最高 151 bpm")),
            Expected(8000, "recorded", 80, "本次有效心率覆盖 80%。",
                listOf("平均 112 bpm", "最高 151 bpm")))
        rows.forEach { expected ->
            val actual = card(fixture(expected.covered))
            assertEquals(expected.status, actual.status)
            assertEquals(if (expected.status == "recorded") "normal" else expected.status, actual.coverageStatus)
            assertEquals(expected.percent, actual.coveragePercent)
            assertEquals("primary_points_available", actual.sampleStatus)
            assertEquals("available", actual.zoneStatus)
            assertEquals(listOf(expected.message), actual.messages)
            assertEquals(expected.metrics, actual.metrics)
            assertEquals(listOf("已记录样本数 2", "暂停时长 1:20", "额外休息 0:20"), actual.facts)
        }
        listOf(4999L, 7999L, 8000L).forEach { covered ->
            val actual = card(fixture(covered, noZones = true))
            val expected = rows.single { it.covered == covered }
            assertEquals(if (covered == 8000L) "recorded_no_zones" else expected.status, actual.status)
            assertEquals("unavailable_no_effective_max", actual.zoneStatus)
            assertEquals(listOf(expected.message, "本次未设置最大心率，无法计算区间。"), actual.messages)
            assertEquals(expected.metrics, actual.metrics)
        }
        val nullAverage = card(fixture(0))
        assertEquals(listOf("未获得可统计的平均", "已记录片段最高 151 bpm"), nullAverage.metrics)
    }
}
