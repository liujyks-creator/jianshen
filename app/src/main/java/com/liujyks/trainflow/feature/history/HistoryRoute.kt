package com.liujyks.trainflow.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowAction
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral500
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

@Composable
internal fun HistoryRoute(
    modifier: Modifier = Modifier
) {
    var uiState by remember {
        mutableStateOf(buildDefaultHistoryScreenState())
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
                SectionTitle("基础趋势")
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
private fun HistoryHeader(uiState: HistoryScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(text = "内存态记录", color = TrainFlowAccent, contentColor = TrainFlowPrimary)
        Text(
            text = "记录",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (uiState.isEmpty) {
                uiState.emptyStateDescription
            } else {
                "${uiState.sessions.size} 条示例历史 · 支持按日期、详情和基础趋势查看"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "当前不读取 Room session records，不生成自动训练建议、医疗结论或心率判断。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
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
        HistoryRoute()
    }
}
