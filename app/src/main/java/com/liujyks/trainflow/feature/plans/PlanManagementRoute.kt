package com.liujyks.trainflow.feature.plans

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.liujyks.trainflow.ui.theme.TrainFlowError
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral500
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

@Composable
fun PlanManagementRoute(
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf(buildDefaultPlanManagementState()) }

    PlanManagementScreen(
        uiState = uiState,
        onSelectPlan = { planId -> uiState = uiState.selectPlan(planId) },
        onCopyPlan = { planId -> uiState = uiState.copyPlan(planId) },
        onRequestDeletePlan = { planId -> uiState = uiState.requestDeletePlan(planId) },
        onConfirmDeletePlan = { uiState = uiState.confirmDeletePlan() },
        onCancelDeletePlan = { uiState = uiState.cancelDeletePlan() },
        modifier = modifier
    )
}

@Composable
private fun PlanManagementScreen(
    uiState: PlanManagementScreenState,
    onSelectPlan: (String) -> Unit,
    onCopyPlan: (String) -> Unit,
    onRequestDeletePlan: (String) -> Unit,
    onConfirmDeletePlan: () -> Unit,
    onCancelDeletePlan: () -> Unit,
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
            PlanManagementHeader(uiState)
        }

        uiState.statusMessage?.let { message ->
            item {
                StatusMessageCard(message)
            }
        }

        if (uiState.isEmpty) {
            item {
                EmptyPlanStateCard()
            }
        } else {
            item {
                SectionTitle(text = "计划列表")
            }

            items(uiState.listItems, key = { it.id }) { item ->
                PlanListCard(
                    item = item,
                    onSelectPlan = { onSelectPlan(item.id) }
                )
            }

            uiState.selectedDetail?.let { detail ->
                item {
                    SectionTitle(text = "计划详情")
                }
                item {
                    PlanDetailCard(
                        detail = detail,
                        onCopyPlan = { onCopyPlan(detail.id) },
                        onRequestDeletePlan = { onRequestDeletePlan(detail.id) }
                    )
                }
            }
        }
    }

    uiState.pendingDeletePlanTitle?.let { title ->
        DeletePlanDialog(
            title = title,
            onConfirmDeletePlan = onConfirmDeletePlan,
            onCancelDeletePlan = onCancelDeletePlan
        )
    }
}

@Composable
private fun PlanManagementHeader(uiState: PlanManagementScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(text = "E2.4 计划管理", color = TrainFlowAccent, contentColor = TrainFlowPrimary)
        Text(
            text = "计划",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (uiState.isEmpty) {
                "暂无已保存计划。后续可从计时或力量编辑页接入真实保存。"
            } else {
                "${uiState.plans.size} 个内存态计划 · 可查看详情、复制和删除"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "训练执行、session records、通知、真实心率、语音和跟练闭环仍未接入。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun PlanListCard(
    item: PlanListItemUiState,
    onSelectPlan: () -> Unit
) {
    Card(
        onClick = onSelectPlan,
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
                        text = item.modeLabel,
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
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.detailSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanDetailCard(
    detail: PlanDetailUiState,
    onCopyPlan: () -> Unit,
    onRequestDeletePlan: () -> Unit
) {
    EditorCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${detail.modeLabel} · ${detail.summary}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrainFlowNeutral700
                )
            }
            ModePill(
                text = detail.modeBadge,
                color = if (detail.modeBadge == "力量") TrainFlowAction else TrainFlowAccent
            )
        }

        detail.sections.forEach { section ->
            DetailSection(section)
        }

        Text(
            text = detail.editStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onCopyPlan,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrainFlowAccent)
            ) {
                Text(text = "复制计划")
            }
            OutlinedButton(
                onClick = onRequestDeletePlan,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "删除计划", color = TrainFlowError)
            }
        }

        Button(
            onClick = {},
            enabled = detail.canStartTraining,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = detail.startStatus)
        }
    }
}

@Composable
private fun DetailSection(section: PlanDetailSectionUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        section.rows.forEach { row ->
            Text(
                text = row,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyPlanStateCard() {
    EditorCard {
        SectionTitle(text = "空状态")
        Text(
            text = "还没有可管理的训练计划。E2.4 保留空列表体验；真实持久化接入前，本页不会创建 session 或启动训练。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusMessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TrainFlowAccent)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun DeletePlanDialog(
    title: String,
    onConfirmDeletePlan: () -> Unit,
    onCancelDeletePlan: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelDeletePlan,
        title = {
            Text(text = "删除计划")
        },
        text = {
            Text(text = "确认删除「$title」？本阶段只会从当前内存态列表移除。")
        },
        confirmButton = {
            TextButton(onClick = onConfirmDeletePlan) {
                Text(text = "确认删除", color = TrainFlowError)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelDeletePlan) {
                Text(text = "取消")
            }
        }
    )
}

@Composable
private fun EditorCard(content: @Composable ColumnScope.() -> Unit) {
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
private fun PlanManagementRoutePreview() {
    TrainFlowTheme {
        PlanManagementRoute()
    }
}
