package com.liujyks.trainflow.feature.plans

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.notifications.AndroidPlanReminderScheduler
import com.liujyks.trainflow.core.notifications.resolvePlanReminderPermissionState
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
import com.liujyks.trainflow.ui.designsystem.TileFlowMetric
import com.liujyks.trainflow.ui.designsystem.TileFlowMetricStrip
import com.liujyks.trainflow.ui.designsystem.currentCardCorner
import com.liujyks.trainflow.ui.designsystem.currentPageHorizontalPadding
import com.liujyks.trainflow.ui.designsystem.currentSectionSpacing
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin
import com.liujyks.trainflow.ui.theme.isTileFlow

@Composable
internal fun PlanManagementRoute(
    uiState: PlanManagementScreenState,
    onStateChange: (PlanManagementScreenState) -> Unit,
    onStartTimedPlan: (WorkoutPlan) -> Unit = {},
    onStartStrengthPlan: (WorkoutPlan) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheduler = remember(context) { AndroidPlanReminderScheduler(context.applicationContext) }
    var permissionRefreshKey by remember { mutableStateOf(0) }
    val permissionState = remember(permissionRefreshKey) {
        context.resolvePlanReminderPermissionState()
    }
    val displayState = uiState.updateNotificationPermissionState(permissionState)
    val reminderPresetOptions = remember { buildPlanReminderPresetOptions() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionRefreshKey += 1
    }

    PlanManagementScreen(
        uiState = displayState,
        reminderPresetOptions = reminderPresetOptions,
        onSelectPlan = { planId -> onStateChange(displayState.selectPlan(planId)) },
        onCopyPlan = { planId -> onStateChange(displayState.copyPlan(planId)) },
        onRequestDeletePlan = { planId -> onStateChange(displayState.requestDeletePlan(planId)) },
        onConfirmDeletePlan = { onStateChange(displayState.confirmDeletePlan()) },
        onCancelDeletePlan = { onStateChange(displayState.cancelDeletePlan()) },
        onSetPlanReminder = { planId, scheduleAt ->
            val nextState = displayState.setPlanReminder(planId = planId, scheduleAt = scheduleAt)
            nextState.plans
                .firstOrNull { it.id == planId }
                ?.toPlanReminderScheduleRequest(permissionState)
                ?.let(scheduler::schedule)
            onStateChange(nextState)
        },
        onClearPlanReminder = { planId ->
            scheduler.cancel(planId)
            onStateChange(displayState.clearPlanReminder(planId))
        },
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onStartTimedPlan = onStartTimedPlan,
        onStartStrengthPlan = onStartStrengthPlan,
        modifier = modifier
    )
}

@Composable
private fun PlanManagementScreen(
    uiState: PlanManagementScreenState,
    reminderPresetOptions: List<PlanReminderPresetUiState>,
    onSelectPlan: (String) -> Unit,
    onCopyPlan: (String) -> Unit,
    onRequestDeletePlan: (String) -> Unit,
    onConfirmDeletePlan: () -> Unit,
    onCancelDeletePlan: () -> Unit,
    onSetPlanReminder: (String, String) -> Unit,
    onClearPlanReminder: (String) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onStartTimedPlan: (WorkoutPlan) -> Unit,
    onStartStrengthPlan: (WorkoutPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(if (skin.isTileFlow) MaterialTheme.colorScheme.background else TrainFlowSurfaceMuted)
            .padding(horizontal = currentPageHorizontalPadding(), vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(currentSectionSpacing())
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
                        plan = uiState.selectedPlan,
                        reminderPresetOptions = reminderPresetOptions,
                        onCopyPlan = { onCopyPlan(detail.id) },
                        onRequestDeletePlan = { onRequestDeletePlan(detail.id) },
                        onSetPlanReminder = { scheduleAt -> onSetPlanReminder(detail.id, scheduleAt) },
                        onClearPlanReminder = { onClearPlanReminder(detail.id) },
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onStartTimedPlan = onStartTimedPlan,
                        onStartStrengthPlan = onStartStrengthPlan
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
    val skin = LocalTrainFlowSkin.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(text = "计划管理", color = skin.tokens.accent, contentColor = skin.tokens.primary)
        Text(
            text = "计划",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (uiState.isEmpty) {
                "暂无已保存计划。当前演示仍使用内存态计划；真实持久化后续接入。"
            } else {
                "${uiState.plans.size} 个内存态计划 · 可查看详情、复制和删除"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "计划提醒已可用；活跃训练通知已接入训练执行页。首版不启用前台服务，不承诺后台精确计时。",
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
    val skin = LocalTrainFlowSkin.current
    Card(
        onClick = onSelectPlan,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(currentCardCorner()),
        colors = CardDefaults.cardColors(
            containerColor = if (skin.isTileFlow && item.selected) {
                skin.tokens.accent.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.selected) skin.tokens.accent else skin.tokens.neutral100
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
            if (skin.isTileFlow) {
                TileFlowMetricStrip(
                    item.metrics
                        .filterNot { metric -> metric.label == "时长" }
                        .toTileFlowMetrics()
                )
            } else {
                Text(
                    text = item.detailSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.reminderSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrainFlowNeutral700
                )
            }
        }
    }
}

@Composable
private fun PlanDetailCard(
    detail: PlanDetailUiState,
    plan: WorkoutPlan?,
    reminderPresetOptions: List<PlanReminderPresetUiState>,
    onCopyPlan: () -> Unit,
    onRequestDeletePlan: () -> Unit,
    onSetPlanReminder: (String) -> Unit,
    onClearPlanReminder: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onStartTimedPlan: (WorkoutPlan) -> Unit,
    onStartStrengthPlan: (WorkoutPlan) -> Unit
) {
    val skin = LocalTrainFlowSkin.current
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

        if (skin.isTileFlow) {
            TileFlowMetricStrip(detail.metrics.toTileFlowMetrics())
        }

        detail.sections.forEach { section ->
            DetailSection(section)
        }

        PlanReminderSection(
            reminder = detail.reminder,
            presetOptions = reminderPresetOptions,
            onSetPlanReminder = onSetPlanReminder,
            onClearPlanReminder = onClearPlanReminder,
            onRequestNotificationPermission = onRequestNotificationPermission
        )

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
            onClick = {
                if (plan != null) {
                    when (plan.mode) {
                        WorkoutMode.TIMED -> onStartTimedPlan(plan)
                        WorkoutMode.STRENGTH -> onStartStrengthPlan(plan)
                        WorkoutMode.FOLLOW_ALONG -> Unit
                    }
                }
            },
            enabled = detail.canStartTraining,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = detail.startStatus)
        }
    }
}

@Composable
private fun PlanReminderSection(
    reminder: PlanReminderUiState,
    presetOptions: List<PlanReminderPresetUiState>,
    onSetPlanReminder: (String) -> Unit,
    onClearPlanReminder: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "训练提醒",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = reminder.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = reminder.permissionMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = if (reminder.canRequestPermission) TrainFlowAction else TrainFlowNeutral700
        )
        Text(
            text = reminder.boundaryCopy,
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral500
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            presetOptions.forEach { option ->
                Button(
                    onClick = { onSetPlanReminder(option.scheduleAt) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrainFlowAccent)
                ) {
                    Text(text = option.label)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onClearPlanReminder,
                enabled = reminder.enabled,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "关闭提醒")
            }
            if (reminder.canRequestPermission) {
                OutlinedButton(
                    onClick = onRequestNotificationPermission,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "开启通知权限")
                }
            }
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
            text = "还没有可管理的训练计划。真实持久化接入前，本页只维护当前内存态列表。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusMessageCard(message: String) {
    val skin = LocalTrainFlowSkin.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(currentCardCorner()),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, skin.tokens.accent)
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
    val skin = LocalTrainFlowSkin.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(currentCardCorner()),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, skin.tokens.neutral100)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

private fun List<PlanMetricUiState>.toTileFlowMetrics(): List<TileFlowMetric> {
    return map { metric -> TileFlowMetric(label = metric.label, value = metric.value) }
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
        var uiState by remember { mutableStateOf(buildDefaultPlanManagementState()) }
        PlanManagementRoute(
            uiState = uiState,
            onStateChange = { nextState -> uiState = nextState }
        )
    }
}
