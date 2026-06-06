package com.liujyks.trainflow.feature.recovery

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendationGenerator
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral500
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

@Composable
internal fun RecoveryRoute(
    uiState: RecoveryScreenState,
    onBackToRecords: () -> Unit,
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
            RecoveryHeader(uiState)
        }

        if (uiState.isEmpty) {
            item {
                RecoveryCard {
                    Text(
                        text = uiState.emptyTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.emptyDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                RecoveryCard {
                    Text(
                        text = "训练部位",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.trainedMuscleSummary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.sourceExerciseSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TrainFlowNeutral700
                    )
                }
            }

            item {
                SectionTitle("推荐放松区域")
            }
            items(uiState.areaItems, key = { item -> item.id }) { item ->
                RecoveryAreaCard(item)
            }
        }

        item {
            RecoveryNoticeCard(uiState)
        }
        item {
            Button(
                onClick = onBackToRecords,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrainFlowAccent)
            ) {
                Text(text = "返回记录", color = TrainFlowPrimary)
            }
        }
    }
}

@Composable
private fun RecoveryHeader(uiState: RecoveryScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(text = "基础建议", color = TrainFlowAccent, contentColor = TrainFlowPrimary)
        Text(
            text = uiState.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = uiState.sessionLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = uiState.sourceNote,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun RecoveryAreaCard(item: RecoveryAreaUiState) {
    RecoveryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.bodyRegionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrainFlowNeutral700
                )
            }
            StatusPill(text = "放松", color = TrainFlowNeutral100, contentColor = TrainFlowPrimary)
        }
        Text(
            text = item.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = item.cautionText,
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral500
        )
    }
}

@Composable
private fun RecoveryNoticeCard(uiState: RecoveryScreenState) {
    RecoveryCard {
        Text(
            text = "边界说明",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = uiState.nonMedicalNotice,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "不基于健康设备、手表、手环、心率或热量生成判断，也不会反向修改训练计划或动作库内容。",
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral500
        )
    }
}

@Composable
private fun RecoveryCard(content: @Composable ColumnScope.() -> Unit) {
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
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
private fun RecoveryRoutePreview() {
    TrainFlowTheme {
        RecoveryRoute(
            uiState = BasicRecoveryRecommendationGenerator.fromExerciseIds(
                sessionId = "preview-session",
                exerciseIds = listOf("barbell-bench-press", "one-arm-dumbbell-row")
            ).toRecoveryScreenState(),
            onBackToRecords = {}
        )
    }
}
