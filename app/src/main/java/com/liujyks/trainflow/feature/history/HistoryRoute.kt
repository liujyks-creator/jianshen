package com.liujyks.trainflow.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.core.model.WorkoutSession
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowAction
import com.liujyks.trainflow.ui.theme.TrainFlowFocus
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral200
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral500
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

@Composable
internal fun HistoryRoute(
    sessions: List<WorkoutSession> = emptyList(),
    modifier: Modifier = Modifier
) {
    var uiState by remember(sessions) {
        mutableStateOf(buildHistoryScreenState(sessions))
    }

    HistoryScreen(
        uiState = uiState,
        onSelectSession = { sessionId ->
            uiState = uiState.selectSession(sessionId)
        },
        modifier = modifier
    )
}

@Composable
private fun HistoryScreen(
    uiState: HistoryScreenState,
    onSelectSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TrainFlowSurfaceMuted)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HistoryHeader(uiState)
        }

        if (uiState.isEmpty) {
            item {
                EmptyHistoryCard(uiState)
            }
        } else {
            uiState.recordStatsUiState?.let { stats ->
                item {
                    SectionTitle("基础统计")
                }
                item {
                    StatsCard(stats)
                }
            }

            uiState.aggregateChartsUiState?.let { charts ->
                item {
                    SectionTitle("聚合趋势")
                }
                item {
                    AggregateChartsHeader(charts)
                }
                item {
                    TrendChartCard(charts.countTrend)
                }
                item {
                    TrendChartCard(charts.statusTrend)
                }
                item {
                    TrendChartCard(charts.elapsedTrend)
                }
                item {
                    TrendChartCard(charts.restTrend)
                }
                item {
                    ModeBreakdownCard(charts.modeBreakdown)
                }
                item {
                    HeartRateUnavailableCard(charts.heartRateUnavailableText)
                }
            }

            item {
                SectionTitle("按日期")
            }
            uiState.dateGroups.forEach { group ->
                item {
                    DateGroupHeader(group.dateLabel)
                }
                items(group.items, key = { item -> item.id }) { item ->
                    HistorySessionCard(
                        item = item,
                        onClick = { onSelectSession(item.id) }
                    )
                }
            }

            uiState.selectedDetail?.let { detail ->
                item {
                    SectionTitle("单次记录")
                }
                item {
                    HistoryDetailCard(detail)
                }
            }

            item {
                SectionTitle("记录参考")
            }
            item {
                TrendCard(uiState.actionTrend)
            }
            item {
                TrendCard(uiState.volumeTrend)
            }
        }
    }
}

@Composable
private fun AggregateChartsHeader(charts: WorkoutAggregateChartsUiState) {
    HistoryCard {
        Text(
            text = charts.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = charts.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
        Text(
            text = "趋势时间点 ${charts.pointCount} 个",
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral500
        )
    }
}

@Composable
private fun HistoryHeader(uiState: HistoryScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(text = uiState.sourcePillLabel, color = TrainFlowAccent, contentColor = TrainFlowPrimary)
        Text(
            text = "记录",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = uiState.headerDescription,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = uiState.boundaryNote,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun TrendChartCard(chart: AggregateTrendChartUiState) {
    HistoryCard {
        Text(
            text = chart.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = chart.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
        if (!chart.hasDrawableTrend) {
            Text(
                text = chart.emptyMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TrendLineCanvas(chart)
            chart.series.forEachIndexed { index, series ->
                TrendLegendRow(
                    color = chartColor(index),
                    label = series.label,
                    latestValue = series.points.lastOrNull()?.valueLabel.orEmpty()
                )
            }
        }
    }
}

@Composable
private fun TrendLineCanvas(chart: AggregateTrendChartUiState) {
    val maxValue = chart.series
        .flatMap { series -> series.points }
        .maxOfOrNull { point -> point.value }
        ?.coerceAtLeast(1)
        ?: 1
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
    ) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = size.height - 20.dp.toPx()
        val chartWidth = (right - left).coerceAtLeast(1f)
        val chartHeight = (bottom - top).coerceAtLeast(1f)
        drawLine(
            color = TrainFlowNeutral200,
            start = Offset(left, bottom),
            end = Offset(right, bottom),
            strokeWidth = 1.dp.toPx()
        )
        chart.series.forEachIndexed { index, series ->
            val points = series.points
            val color = chartColor(index)
            points.windowed(2).forEachIndexed { startIndex, pair ->
                val startPoint = pair.first()
                val endPoint = pair.last()
                val endIndex = startIndex + 1
                drawLine(
                    color = color,
                    start = Offset(
                        x = left + chartWidth * startIndex / (points.size - 1).coerceAtLeast(1),
                        y = bottom - chartHeight * startPoint.value / maxValue
                    ),
                    end = Offset(
                        x = left + chartWidth * endIndex / (points.size - 1).coerceAtLeast(1),
                        y = bottom - chartHeight * endPoint.value / maxValue
                    ),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            points.forEachIndexed { pointIndex, point ->
                drawCircle(
                    color = color,
                    radius = 3.dp.toPx(),
                    center = Offset(
                        x = left + chartWidth * pointIndex / (points.size - 1).coerceAtLeast(1),
                        y = bottom - chartHeight * point.value / maxValue
                    )
                )
            }
        }
    }
}

@Composable
private fun TrendLegendRow(
    color: Color,
    label: String,
    latestValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .width(18.dp)
                    .background(color, RoundedCornerShape(999.dp))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TrainFlowNeutral700
            )
        }
        Text(
            text = latestValue,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ModeBreakdownCard(modeBreakdown: ModeBreakdownChartUiState) {
    HistoryCard {
        Text(
            text = modeBreakdown.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = modeBreakdown.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
        if (modeBreakdown.rows.isEmpty()) {
            Text(
                text = modeBreakdown.emptyMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            modeBreakdown.rows.forEachIndexed { index, row ->
                ModeBreakdownRow(row = row, color = chartColor(index))
            }
        }
    }
}

@Composable
private fun ModeBreakdownRow(
    row: ModeBreakdownRowUiState,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral700
            )
            Text(
                text = "${row.count} 次 · ${row.percentLabel}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(row.percentLabel.removeSuffix("%").toFloatOrNull()?.div(100f)?.coerceIn(0f, 1f) ?: 0f)
                .height(8.dp)
                .background(color, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun HeartRateUnavailableCard(message: String) {
    HistoryCard {
        Text(
            text = "未获取心率",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

private fun chartColor(index: Int): Color {
    return listOf(
        TrainFlowAccent,
        TrainFlowAction,
        TrainFlowFocus,
        TrainFlowNeutral500
    )[index % 4]
}

@Composable
private fun StatsCard(stats: WorkoutRecordStatsUiState) {
    HistoryCard {
        Text(
            text = stats.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stats.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
        stats.rows.forEach { row ->
            SummaryRow(row)
        }
    }
}

@Composable
private fun HistorySessionCard(
    item: HistorySessionListItemUiState,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.selected) TrainFlowAccent else TrainFlowNeutral100
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.modeLabel} · ${item.statusLabel} · ${item.durationLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TrainFlowNeutral700
                    )
                }
                ModePill(
                    text = item.modeBadge,
                    color = if (item.modeBadge == "力量") TrainFlowAction else TrainFlowAccent
                )
            }
            Text(
                text = item.keySummary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HistoryDetailCard(detail: HistorySessionDetailUiState) {
    HistoryCard {
        Text(
            text = detail.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = detail.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
        detail.rows.forEach { row ->
            SummaryRow(row)
        }
        Text(
            text = detail.sourceNote,
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral500
        )
    }
}

@Composable
private fun TrendCard(trend: BasicTrendUiState) {
    HistoryCard {
        Text(
            text = trend.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = trend.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
        if (trend.rows.isEmpty()) {
            Text(
                text = trend.emptyMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            trend.rows.forEach { row ->
                TrendRow(row)
            }
        }
    }
}

@Composable
private fun SummaryRow(row: HistorySummaryRowUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral700
            )
            Text(
                text = row.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = row.helper,
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral500
        )
    }
}

@Composable
private fun TrendRow(row: BasicTrendRowUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = row.secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TrainFlowNeutral700
                )
            }
            Text(
                text = row.metric,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = row.helper,
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral500
        )
    }
}

@Composable
private fun EmptyHistoryCard(uiState: HistoryScreenState) {
    HistoryCard {
        Text(
            text = uiState.emptyStateTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = uiState.emptyStateDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun DateGroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = TrainFlowNeutral700
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ModePill(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (text == "力量") TrainFlowNeutral50 else TrainFlowPrimary
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryRoutePreview() {
    TrainFlowTheme {
        HistoryScreen(
            uiState = buildDefaultHistoryScreenState(),
            onSelectSession = {},
            modifier = Modifier
        )
    }
}
