package com.liujyks.trainflow.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.core.data.HeartRateChartProjector
import com.liujyks.trainflow.core.data.WorkoutSessionHistoricalResult
import com.liujyks.trainflow.core.data.WorkoutSessionStrictReadResult
import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.StatusProjectionV1
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin
import com.liujyks.trainflow.ui.designsystem.currentCardCorner

internal sealed interface WorkoutSessionHeartRateCardUiState {
    data object Hidden : WorkoutSessionHeartRateCardUiState
    data class Unavailable(val message: String) : WorkoutSessionHeartRateCardUiState
    data class ReadError(
        val source: WorkoutSessionHistoricalResult?,
        val cause: Throwable?
    ) : WorkoutSessionHeartRateCardUiState
    data class Content(
        val status: String,
        val sampleStatus: String,
        val coverageStatus: String,
        val zoneStatus: String,
        val coveragePercent: Int?,
        val messages: List<String>,
        val metrics: List<String>,
        val facts: List<String>
    ) : WorkoutSessionHeartRateCardUiState
}

internal fun buildWorkoutSessionHeartRateCardUiState(
    historical: WorkoutSessionHistoricalResult?,
    readFailure: Throwable? = null
): WorkoutSessionHeartRateCardUiState {
    val source = when (historical) {
        null -> return if (readFailure == null) WorkoutSessionHeartRateCardUiState.Hidden
            else WorkoutSessionHeartRateCardUiState.ReadError(null, readFailure)
        is WorkoutSessionHistoricalResult.Resolved -> historical.source
        is WorkoutSessionHistoricalResult.Forwarded -> historical.source
        is WorkoutSessionHistoricalResult.InvalidPlannedDuration,
        is WorkoutSessionHistoricalResult.InvalidTimedStructure ->
            return WorkoutSessionHeartRateCardUiState.ReadError(historical, null)
    }
    when (source) {
        is WorkoutSessionStrictReadResult.LegacyTerminal,
        WorkoutSessionStrictReadResult.NotFound -> return WorkoutSessionHeartRateCardUiState.Hidden
        is WorkoutSessionStrictReadResult.Unavailable ->
            return WorkoutSessionHeartRateCardUiState.ReadError(historical, null)
        is WorkoutSessionStrictReadResult.Nonterminal -> return if (source.graph.recording == null) {
            WorkoutSessionHeartRateCardUiState.Hidden
        } else {
            WorkoutSessionHeartRateCardUiState.Unavailable("本次心率记录尚未形成终态分析。")
        }
        is WorkoutSessionStrictReadResult.CanonicalTerminal -> Unit
    }
    if (readFailure != null) return WorkoutSessionHeartRateCardUiState.ReadError(historical, readFailure)
    val recording = source.graph.recording ?: return WorkoutSessionHeartRateCardUiState.Hidden
    val snapshot = source.graph.snapshots.single { it.analysisVersion == recording.originalAnalysisVersion }
    val status = StatusProjectionV1.project(recording, snapshot)
    val coveragePercent = snapshot.coverageBasisPoints?.div(100)
    val messages = buildList {
        add(when (status) {
            "no_eligible_duration" -> "本次没有可纳入主要训练心率统计的时长。"
            "zero_samples" -> "已开启心率记录，但本次未获得有效心率数据。"
            "insufficient" -> "本次有效心率覆盖不足 50%，暂不生成自动摘要。"
            "partial" -> "本次心率记录不完整，以下结果仅基于已记录片段。"
            "recorded", "recorded_no_zones" -> "本次有效心率覆盖 $coveragePercent%。"
            else -> error("Unexpected terminal heart-rate status: $status")
        })
        val quality = parseCanonicalJson(snapshot.qualityReasonsJson) as CanonicalJsonValue.Obj
        val reasons = (quality.fields.getValue("sessionReasons") as CanonicalJsonValue.Arr).values
        if (snapshot.zoneStatus == "unavailable_no_effective_max" && reasons.any {
                ((it as CanonicalJsonValue.Obj).fields.getValue("reasonCode") as CanonicalJsonValue.Str).value ==
                    "unavailable_no_effective_max"
            }) add("本次未设置最大心率，无法计算区间。")
        if (snapshot.sampleStatus == "canonical_only_excluded") {
            add("已记录心率均不计入主要训练统计。")
        }
    }
    val metrics = if (status == "no_eligible_duration" || status == "zero_samples") emptyList() else {
        val prefix = if (snapshot.coverageStatus == "normal") "" else "已记录片段"
        listOf(
            snapshot.observedAvgBpm?.let { "${prefix}平均 $it bpm" } ?: "未获得可统计的平均",
            snapshot.observedMaxBpm?.let { "${prefix}最高 $it bpm" } ?: "未获得可统计的最高"
        )
    }
    val facts = listOf(
        "已记录样本数 ${snapshot.canonicalSampleCount}",
        "暂停时长 ${source.graph.session.pausedElapsedSec?.let { HeartRateChartProjector.formatElapsed(it.toLong() * 1000) } ?: "未记录"}",
        "额外休息 ${HeartRateChartProjector.formatElapsed(source.execution.restExtensions.sumOf { it.addedSec.toLong() } * 1000)}"
    )
    return WorkoutSessionHeartRateCardUiState.Content(status, snapshot.sampleStatus, snapshot.coverageStatus,
        snapshot.zoneStatus, coveragePercent, messages, metrics, facts)
}

@Composable
internal fun WorkoutSessionHeartRateCard(
    state: WorkoutSessionHeartRateCardUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAnalysis: (() -> Unit)? = null
) {
    if (state == WorkoutSessionHeartRateCardUiState.Hidden) return
    val fontScale = LocalTrainFlowSkin.current.tokens.fontScale
    val title = MaterialTheme.typography.titleLarge.let { it.copy(fontSize = it.fontSize * fontScale, lineHeight = it.lineHeight * fontScale) }
    val body = MaterialTheme.typography.bodyMedium.let { it.copy(fontSize = it.fontSize * fontScale, lineHeight = it.lineHeight * fontScale) }
    val metric = MaterialTheme.typography.titleMedium.let { it.copy(fontSize = it.fontSize * fontScale, lineHeight = it.lineHeight * fontScale) }
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(currentCardCorner())) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("主要训练心率", style = title)
            when (state) {
                WorkoutSessionHeartRateCardUiState.Hidden -> Unit
                is WorkoutSessionHeartRateCardUiState.Unavailable ->
                    Text(state.message, style = body)
                is WorkoutSessionHeartRateCardUiState.ReadError -> {
                    Text("暂时无法读取本次心率数据", style = body)
                    Button(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("重试", style = body) }
                }
                is WorkoutSessionHeartRateCardUiState.Content -> {
                    state.messages.forEach { Text(it, style = body) }
                    state.metrics.forEach { Text(it, style = metric) }
                    state.facts.forEach { Text(it, style = body) }
                    if (onOpenAnalysis != null) {
                        Button(onClick = onOpenAnalysis, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("查看心率分析", style = body)
                        }
                    }
                }
            }
        }
    }
}
