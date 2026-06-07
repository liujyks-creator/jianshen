package com.liujyks.trainflow.feature.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

@Composable
internal fun TimedPlanEditorRoute(
    onBackToHome: () -> Unit,
    onStartTimedPlan: (WorkoutPlan) -> Unit = {},
    modifier: Modifier = Modifier,
    planEditorDefaults: PlanEditorDefaults = PlanEditorDefaults()
) {
    var uiState by remember {
        mutableStateOf(buildDefaultTimedPlanEditorState(defaults = planEditorDefaults))
    }

    TimedPlanEditorScreen(
        uiState = uiState,
        onBackToHome = onBackToHome,
        onTitleChanged = { uiState = uiState.updateTitle(it) },
        onRoundsChanged = { uiState = uiState.updateRoundsText(it) },
        onRestBetweenRoundsChanged = { uiState = uiState.updateRestBetweenRoundsText(it) },
        onActionCueThresholdChanged = { uiState = uiState.updateActionCueThresholdText(it) },
        onRestCueThresholdChanged = { uiState = uiState.updateRestCueThresholdText(it) },
        onActionCueEnabledChanged = { uiState = uiState.updateActionCueEnabled(it) },
        onRestCueEnabledChanged = { uiState = uiState.updateRestCueEnabled(it) },
        onSoundEnabledChanged = { uiState = uiState.updateSoundEnabled(it) },
        onVibrationEnabledChanged = { uiState = uiState.updateVibrationEnabled(it) },
        onEmphasisAnimationEnabledChanged = { uiState = uiState.updateEmphasisAnimationEnabled(it) },
        onStageNameChanged = { stageId, name -> uiState = uiState.updateStageName(stageId, name) },
        onStageDurationChanged = { stageId, seconds -> uiState = uiState.updateStageDurationText(stageId, seconds) },
        onStageTypeChanged = { stageId, type -> uiState = uiState.updateStageType(stageId, type) },
        onCopyStage = { stageId -> uiState = uiState.copyStage(stageId) },
        onRemoveStage = { stageId -> uiState = uiState.removeStage(stageId) },
        onMoveStageUp = { stageId -> uiState = uiState.moveStageUp(stageId) },
        onMoveStageDown = { stageId -> uiState = uiState.moveStageDown(stageId) },
        onAddStage = { type -> uiState = uiState.addStage(type) },
        onSaveDraft = { uiState = uiState.saveDraftPlan() },
        onStartTimedPlan = {
            if (uiState.canStartTraining) {
                onStartTimedPlan(
                    uiState.toWorkoutPlan(
                        planId = "plan-timed-editor-start",
                        timestamp = DefaultTimedPlanTimestamp
                    )
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun TimedPlanEditorScreen(
    uiState: TimedPlanEditorScreenState,
    onBackToHome: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onRoundsChanged: (String) -> Unit,
    onRestBetweenRoundsChanged: (String) -> Unit,
    onActionCueThresholdChanged: (String) -> Unit,
    onRestCueThresholdChanged: (String) -> Unit,
    onActionCueEnabledChanged: (Boolean) -> Unit,
    onRestCueEnabledChanged: (Boolean) -> Unit,
    onSoundEnabledChanged: (Boolean) -> Unit,
    onVibrationEnabledChanged: (Boolean) -> Unit,
    onEmphasisAnimationEnabledChanged: (Boolean) -> Unit,
    onStageNameChanged: (String, String) -> Unit,
    onStageDurationChanged: (String, String) -> Unit,
    onStageTypeChanged: (String, TimedStageType) -> Unit,
    onCopyStage: (String) -> Unit,
    onRemoveStage: (String) -> Unit,
    onMoveStageUp: (String) -> Unit,
    onMoveStageDown: (String) -> Unit,
    onAddStage: (TimedStageType) -> Unit,
    onSaveDraft: () -> Unit,
    onStartTimedPlan: () -> Unit,
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
            TextButton(onClick = onBackToHome) {
                Text(text = "返回训练首页")
            }
        }

        item {
            TimedPlanEditorHeader(uiState)
        }

        item {
            PlanBasicsCard(uiState = uiState, onTitleChanged = onTitleChanged)
        }

        item {
            CircuitSettingsCard(
                uiState = uiState,
                onRoundsChanged = onRoundsChanged,
                onRestBetweenRoundsChanged = onRestBetweenRoundsChanged
            )
        }

        item {
            SectionTitle(text = "阶段编排")
        }

        itemsIndexed(uiState.stages, key = { _, stage -> stage.id }) { index, stage ->
            TimedStageEditorCard(
                stage = stage,
                index = index,
                totalCount = uiState.stages.size,
                onNameChanged = { name -> onStageNameChanged(stage.id, name) },
                onDurationChanged = { seconds -> onStageDurationChanged(stage.id, seconds) },
                onStageTypeChanged = { type -> onStageTypeChanged(stage.id, type) },
                onCopy = { onCopyStage(stage.id) },
                onRemove = { onRemoveStage(stage.id) },
                onMoveUp = { onMoveStageUp(stage.id) },
                onMoveDown = { onMoveStageDown(stage.id) }
            )
        }

        item {
            AddTimedStageCard(onAddStage = onAddStage)
        }

        item {
            CueSettingsCard(
                uiState = uiState,
                onActionCueThresholdChanged = onActionCueThresholdChanged,
                onRestCueThresholdChanged = onRestCueThresholdChanged,
                onActionCueEnabledChanged = onActionCueEnabledChanged,
                onRestCueEnabledChanged = onRestCueEnabledChanged,
                onSoundEnabledChanged = onSoundEnabledChanged,
                onVibrationEnabledChanged = onVibrationEnabledChanged,
                onEmphasisAnimationEnabledChanged = onEmphasisAnimationEnabledChanged
            )
        }

        item {
            SaveAndPreviewCard(
                uiState = uiState,
                onSaveDraft = onSaveDraft,
                onStartTimedPlan = onStartTimedPlan
            )
        }
    }
}

@Composable
private fun TimedPlanEditorHeader(uiState: TimedPlanEditorScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(text = "E10.2 纯间歇计时", color = TrainFlowAccent, contentColor = TrainFlowPrimary)
        Text(
            text = "计时训练编辑",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = uiState.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "热身、工作、休息、放松和自定义阶段直接编排；计时训练不再选择动作库动作。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun PlanBasicsCard(
    uiState: TimedPlanEditorScreenState,
    onTitleChanged: (String) -> Unit
) {
    EditorCard {
        SectionTitle(text = "基础信息")
        OutlinedTextField(
            value = uiState.title,
            onValueChange = onTitleChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("计划名称") },
            singleLine = true
        )
        Text(
            text = "主题色 ${uiState.themeColorHex}，阶段颜色可在阶段卡中选择。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun CircuitSettingsCard(
    uiState: TimedPlanEditorScreenState,
    onRoundsChanged: (String) -> Unit,
    onRestBetweenRoundsChanged: (String) -> Unit
) {
    EditorCard {
        SectionTitle(text = "轮次与轮间休息")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "轮数",
                value = uiState.roundsText,
                onValueChanged = onRoundsChanged,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "轮间休息秒数",
                value = uiState.restBetweenRoundsText,
                onValueChanged = onRestBetweenRoundsChanged,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimedStageEditorCard(
    stage: TimedPlanEditorStageUiState,
    index: Int,
    totalCount: Int,
    onNameChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onStageTypeChanged: (TimedStageType) -> Unit,
    onCopy: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StageSwatch(stage.colorHex)
                    Text(
                        text = "${index + 1}. ${stage.typeLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "图标 ${stage.iconKey} · ${stage.durationSec.formatDuration()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onMoveUp, enabled = index > 0) { Text("上移") }
                TextButton(onClick = onMoveDown, enabled = index < totalCount - 1) { Text("下移") }
            }
        }

        OutlinedTextField(
            value = stage.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("阶段名称") },
            singleLine = true
        )
        NumberField(
            label = "阶段秒数",
            value = stage.durationText,
            onValueChanged = onDurationChanged,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimedStageTypeOptions.forEach { option ->
                FilterChip(
                    selected = stage.stageType == option.stageType,
                    onClick = { onStageTypeChanged(option.stageType) },
                    label = { Text(option.label) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                Text("复制")
            }
            OutlinedButton(onClick = onRemove, enabled = totalCount > 1, modifier = Modifier.weight(1f)) {
                Text("删除")
            }
        }
    }
}

@Composable
private fun StageSwatch(colorHex: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = colorHex.toComposeColor(),
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Text(
            text = "  ",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun AddTimedStageCard(onAddStage: (TimedStageType) -> Unit) {
    EditorCard {
        SectionTitle(text = "添加阶段")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimedStageTypeOptions.forEach { option ->
                FilterChip(
                    selected = false,
                    onClick = { onAddStage(option.stageType) },
                    label = { Text("+ ${option.label}") }
                )
            }
        }
        Text(
            text = "拖拽排序留作后续增强；当前使用明确的上移 / 下移按钮完成排序。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun CueSettingsCard(
    uiState: TimedPlanEditorScreenState,
    onActionCueThresholdChanged: (String) -> Unit,
    onRestCueThresholdChanged: (String) -> Unit,
    onActionCueEnabledChanged: (Boolean) -> Unit,
    onRestCueEnabledChanged: (Boolean) -> Unit,
    onSoundEnabledChanged: (Boolean) -> Unit,
    onVibrationEnabledChanged: (Boolean) -> Unit,
    onEmphasisAnimationEnabledChanged: (Boolean) -> Unit
) {
    EditorCard {
        SectionTitle(text = "临近结束提醒")
        ToggleRow(
            title = "阶段临近结束提醒",
            checked = uiState.actionCue.enabled,
            onCheckedChange = onActionCueEnabledChanged
        )
        NumberField(
            label = "阶段提醒阈值秒数",
            value = uiState.actionCue.thresholdText,
            onValueChanged = onActionCueThresholdChanged,
            modifier = Modifier.fillMaxWidth()
        )
        ToggleRow(
            title = "休息临近结束提醒",
            checked = uiState.restCue.enabled,
            onCheckedChange = onRestCueEnabledChanged
        )
        NumberField(
            label = "休息提醒阈值秒数",
            value = uiState.restCue.thresholdText,
            onValueChanged = onRestCueThresholdChanged,
            modifier = Modifier.fillMaxWidth()
        )
        ToggleRow("声音", uiState.actionCue.soundEnabled && uiState.restCue.soundEnabled, onSoundEnabledChanged)
        ToggleRow("震动", uiState.actionCue.vibrationEnabled && uiState.restCue.vibrationEnabled, onVibrationEnabledChanged)
        ToggleRow(
            "强化动画",
            uiState.actionCue.emphasisAnimationEnabled && uiState.restCue.emphasisAnimationEnabled,
            onEmphasisAnimationEnabledChanged
        )
        Text(
            text = "固定阶段词 cue 仅保留边界；当前不实现语音、TTS 或音频资源。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun SaveAndPreviewCard(
    uiState: TimedPlanEditorScreenState,
    onSaveDraft: () -> Unit,
    onStartTimedPlan: () -> Unit
) {
    EditorCard {
        SectionTitle(text = "草稿预览")
        Text(
            text = uiState.summary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        uiState.statusMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.bodyMedium, color = TrainFlowNeutral700)
        }
        uiState.validationMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        uiState.savedPlan?.let { plan ->
            val circuit = plan.blocks.filterIsInstance<TimedCircuitBlock>().singleOrNull()
            Text(
                text = "WorkoutPlan: ${plan.mode.contractValue} · ${plan.blocks.size} 个 block · ${circuit?.items?.size ?: 0} 个 interval stage",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "阶段提醒 ${plan.preferences?.cueSettings?.actionEnding?.thresholdSec} 秒；休息提醒 ${plan.preferences?.cueSettings?.restEnding?.thresholdSec} 秒。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onSaveDraft,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrainFlowAccent)
            ) {
                Text(text = "保存草稿")
            }
            Button(
                onClick = onStartTimedPlan,
                enabled = uiState.canStartTraining,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "立即开始")
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChanged(input.sanitizeIntegerInput()) },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true
    )
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
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
private fun StatusPill(
    text: String,
    color: Color,
    contentColor: Color
) {
    Surface(shape = RoundedCornerShape(999.dp), color = color) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}

private fun String.toComposeColor(): Color {
    return runCatching { Color(android.graphics.Color.parseColor(this)) }
        .getOrElse { TrainFlowAccent }
}

@Preview(showBackground = true)
@Composable
private fun TimedPlanEditorRoutePreview() {
    TrainFlowTheme {
        TimedPlanEditorRoute(onBackToHome = {})
    }
}
