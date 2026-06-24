package com.liujyks.trainflow.feature.plans

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.notifications.AndroidPlanReminderScheduler
import com.liujyks.trainflow.core.notifications.PlanReminderNotificationPermissionState
import com.liujyks.trainflow.core.notifications.PlanReminderScheduleResult
import com.liujyks.trainflow.core.notifications.PlanReminderScheduler
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
import com.liujyks.trainflow.ui.theme.TrainFlowSkin
import com.liujyks.trainflow.ui.theme.isTileFlow
import java.time.Instant

@Composable
internal fun PlanManagementRoute(
    uiState: PlanManagementScreenState,
    onStateChange: (PlanManagementScreenState) -> Unit,
    onPersistPlan: (WorkoutPlan) -> Unit = {},
    onDeletePlan: (String) -> Unit = {},
    onEditPlan: (WorkoutPlan) -> Unit = {},
    onCreateTimedPlan: () -> Unit = {},
    onCreateStrengthPlan: () -> Unit = {},
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
        onCopyPlan = { planId ->
            val nextState = displayState.copyPlan(planId, timestamp = Instant.now().toString())
            nextState.selectedPlan?.let(onPersistPlan)
            onStateChange(nextState)
        },
        onRequestDeletePlan = { planId -> onStateChange(displayState.requestDeletePlan(planId)) },
        onConfirmDeletePlan = {
            val deletePlanId = displayState.pendingDeletePlanId
            val nextState = displayState.confirmDeletePlan()
            deletePlanId?.let(scheduler::cancel)
            deletePlanId?.let(onDeletePlan)
            onStateChange(nextState)
        },
        onCancelDeletePlan = { onStateChange(displayState.cancelDeletePlan()) },
        onSetPlanReminder = { planId, scheduleAt ->
            val nextState = displayState.setPlanReminder(
                planId = planId,
                scheduleAt = scheduleAt,
                timestamp = Instant.now().toString()
            )
            nextState.plans
                .firstOrNull { it.id == planId }
                ?.let { plan ->
                    dispatchPlanReminderReplacement(
                        plan = plan,
                        permissionState = permissionState,
                        scheduler = scheduler
                    )
                }
            nextState.plans
                .firstOrNull { it.id == planId }
                ?.let(onPersistPlan)
            onStateChange(nextState)
        },
        onClearPlanReminder = { planId ->
            val nextState = displayState.clearPlanReminder(
                planId = planId,
                timestamp = Instant.now().toString()
            )
            scheduler.cancel(planId)
            nextState.plans
                .firstOrNull { it.id == planId }
                ?.let(onPersistPlan)
            onStateChange(nextState)
        },
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onCreateTimedPlan = onCreateTimedPlan,
        onCreateStrengthPlan = onCreateStrengthPlan,
        onStartTimedPlan = onStartTimedPlan,
        onStartStrengthPlan = onStartStrengthPlan,
        onEditPlan = onEditPlan,
        modifier = modifier
    )
}

internal fun dispatchPlanReminderReplacement(
    plan: WorkoutPlan,
    permissionState: PlanReminderNotificationPermissionState,
    scheduler: PlanReminderScheduler
): PlanReminderScheduleResult {
    scheduler.cancel(plan.id)
    return scheduler.schedule(plan.toPlanReminderScheduleRequest(permissionState))
}

internal fun dispatchPlanReminderReplacementForEditedPlan(
    plan: WorkoutPlan,
    wasEditingExistingPlan: Boolean,
    permissionState: PlanReminderNotificationPermissionState,
    scheduler: PlanReminderScheduler
): PlanReminderScheduleResult? {
    if (!wasEditingExistingPlan) return null
    return dispatchPlanReminderReplacement(
        plan = plan,
        permissionState = permissionState,
        scheduler = scheduler
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
    onCreateTimedPlan: () -> Unit,
    onCreateStrengthPlan: () -> Unit,
    onStartTimedPlan: (WorkoutPlan) -> Unit,
    onStartStrengthPlan: (WorkoutPlan) -> Unit,
    onEditPlan: (WorkoutPlan) -> Unit,
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
                EmptyPlanStateCard(
                    onCreateTimedPlan = onCreateTimedPlan,
                    onCreateStrengthPlan = onCreateStrengthPlan
                )
            }
        } else {
            item {
                SectionTitle(text = "计划播放列表")
            }

            items(uiState.listItems, key = { it.id }) { item ->
                val detail = uiState.selectedDetail?.takeIf { detail -> detail.id == item.id }
                val plan = uiState.plans.firstOrNull { plan -> plan.id == item.id }
                PlanListCard(
                    item = item,
                    detail = detail,
                    plan = plan,
                    reminderPresetOptions = reminderPresetOptions,
                    onSelectPlan = { onSelectPlan(item.id) },
                    onCopyPlan = { onCopyPlan(item.id) },
                    onRequestDeletePlan = { onRequestDeletePlan(item.id) },
                    onSetPlanReminder = { scheduleAt -> onSetPlanReminder(item.id, scheduleAt) },
                    onClearPlanReminder = { onClearPlanReminder(item.id) },
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onEditPlan = onEditPlan,
                    onStartTimedPlan = onStartTimedPlan,
                    onStartStrengthPlan = onStartStrengthPlan
                )
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
                "暂无已保存计划。可从训练首页创建计时或力量计划并保存到本地。"
            } else {
                "${uiState.plans.size} 个本地计划 · 可查看详情、复制、提醒和删除"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "计划提醒和活跃训练通知都只是普通通知；通知关闭后训练仍可正常使用，不承诺闹钟级强提醒或后台可靠计时。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun PlanListCard(
    item: PlanListItemUiState,
    detail: PlanDetailUiState?,
    plan: WorkoutPlan?,
    reminderPresetOptions: List<PlanReminderPresetUiState>,
    onSelectPlan: () -> Unit,
    onCopyPlan: () -> Unit,
    onRequestDeletePlan: () -> Unit,
    onSetPlanReminder: (String) -> Unit,
    onClearPlanReminder: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onEditPlan: (WorkoutPlan) -> Unit,
    onStartTimedPlan: (WorkoutPlan) -> Unit,
    onStartStrengthPlan: (WorkoutPlan) -> Unit
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
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlanColorSwatch(item.planColorHex)
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
                }
                ModePill(
                    mode = item.mode,
                    text = item.modeBadge,
                    skin = skin
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

            Text(
                text = if (item.selected) "收起计划" else "展开计划",
                style = MaterialTheme.typography.labelLarge,
                color = skin.tokens.accent
            )

            if (detail != null) {
                ExpandedPlanContent(
                    detail = detail,
                    plan = plan,
                    reminderPresetOptions = reminderPresetOptions,
                    onCopyPlan = onCopyPlan,
                    onRequestDeletePlan = onRequestDeletePlan,
                    onSetPlanReminder = onSetPlanReminder,
                    onClearPlanReminder = onClearPlanReminder,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onEditPlan = onEditPlan,
                    onStartTimedPlan = onStartTimedPlan,
                    onStartStrengthPlan = onStartStrengthPlan
                )
            }
        }
    }
}

@Composable
private fun ExpandedPlanContent(
    detail: PlanDetailUiState,
    plan: WorkoutPlan?,
    reminderPresetOptions: List<PlanReminderPresetUiState>,
    onCopyPlan: () -> Unit,
    onRequestDeletePlan: () -> Unit,
    onSetPlanReminder: (String) -> Unit,
    onClearPlanReminder: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onEditPlan: (WorkoutPlan) -> Unit,
    onStartTimedPlan: (WorkoutPlan) -> Unit,
    onStartStrengthPlan: (WorkoutPlan) -> Unit
) {
    val skin = LocalTrainFlowSkin.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            text = "计划颜色 · 默认红色展示。当前模型没有计划级持久化字段，颜色保存拆到后续数据决策。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )

        Text(
            text = detail.editStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )

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
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TrainFlowPrimary,
                contentColor = TrainFlowNeutral50
            )
        ) {
            Text(text = detail.startStatus)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    if (plan != null && detail.canEditPlan) {
                        onEditPlan(plan)
                    }
                },
                enabled = detail.canEditPlan && plan != null,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = detail.editActionLabel)
            }
            Button(
                onClick = onCopyPlan,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrainFlowAccent)
            ) {
                Text(text = "复制计划")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onRequestDeletePlan,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "删除当前计划", color = TrainFlowError)
            }
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
private fun EmptyPlanStateCard(
    onCreateTimedPlan: () -> Unit,
    onCreateStrengthPlan: () -> Unit
) {
    EditorCard {
        SectionTitle(text = "还没有保存的计划")
        Text(
            text = "可以直接创建计时计划或力量计划。跟练完整计划创建还没有进入首版，不提供假的完整入口。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onCreateTimedPlan,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrainFlowAccent)
            ) {
                Text(text = "创建计时计划")
            }
            Button(
                onClick = onCreateStrengthPlan,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrainFlowPrimary,
                    contentColor = TrainFlowNeutral50
                )
            ) {
                Text(text = "创建力量计划")
            }
        }
    }
}

@Composable
private fun PlanColorSwatch(colorHex: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colorHex.toComposeColor(defaultColor = TrainFlowError),
        modifier = Modifier
            .padding(top = 2.dp)
            .size(width = 16.dp, height = 54.dp),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Box(contentAlignment = Alignment.Center) {}
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
            Text(text = "删除当前计划")
        },
        text = {
            Text(text = "确认删除本地计划「$title」？训练历史中的计划快照不会被改写。")
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
    mode: WorkoutMode,
    skin: TrainFlowSkin
) {
    val colors = modePillColors(mode = mode, skin = skin)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = colors.containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = colors.contentColor
        )
    }
}

internal data class ModePillColors(
    val containerColor: Color,
    val contentColor: Color
)

internal fun modePillColors(
    mode: WorkoutMode,
    skin: TrainFlowSkin
): ModePillColors {
    val containerColor = when (mode) {
        WorkoutMode.TIMED -> skin.tokens.accent
        WorkoutMode.STRENGTH -> skin.tokens.action
        WorkoutMode.FOLLOW_ALONG -> skin.tokens.secondary
    }
    return ModePillColors(
        containerColor = containerColor,
        contentColor = mostReadableModePillContentColor(
            containerColor = containerColor,
            skin = skin
        )
    )
}

internal fun modePillContrastRatio(
    contentColor: Color,
    containerColor: Color
): Float {
    val contentLuminance = contentColor.luminance()
    val containerLuminance = containerColor.luminance()
    val lighter = maxOf(contentLuminance, containerLuminance)
    val darker = minOf(contentLuminance, containerLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun mostReadableModePillContentColor(
    containerColor: Color,
    skin: TrainFlowSkin
): Color {
    return listOf(
        skin.tokens.primary,
        skin.tokens.neutral50,
        skin.tokens.neutral900
    ).maxBy { contentColor ->
        modePillContrastRatio(
            contentColor = contentColor,
            containerColor = containerColor
        )
    }
}

private fun String.toComposeColor(defaultColor: Color = TrainFlowAccent): Color {
    return runCatching { Color(android.graphics.Color.parseColor(this)) }
        .getOrElse { defaultColor }
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
