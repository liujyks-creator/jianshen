package com.liujyks.trainflow.feature.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
import kotlin.math.roundToInt

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
        onStageColorChanged = { stageId, colorHex -> uiState = uiState.updateStageColor(stageId, colorHex) },
        onCopyStage = { stageId -> uiState = uiState.copyStage(stageId) },
        onRemoveStage = { stageId -> uiState = uiState.removeStage(stageId) },
        onMoveStageUp = { stageId -> uiState = uiState.moveStageUp(stageId) },
        onMoveStageDown = { stageId -> uiState = uiState.moveStageDown(stageId) },
        onMoveStage = { fromIndex, toIndex -> uiState = uiState.moveStage(fromIndex, toIndex) },
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
    onStageColorChanged: (String, String) -> Unit,
    onCopyStage: (String) -> Unit,
    onRemoveStage: (String) -> Unit,
    onMoveStageUp: (String) -> Unit,
    onMoveStageDown: (String) -> Unit,
    onMoveStage: (Int, Int) -> Unit,
    onAddStage: (TimedStageType) -> Unit,
    onSaveDraft: () -> Unit,
    onStartTimedPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedStageId by remember { mutableStateOf<String?>(null) }
    var draggedStageStartIndex by remember { mutableStateOf<Int?>(null) }
    var draggedStageOffsetY by remember { mutableStateOf(0f) }
    var draggedStageHeightPx by remember { mutableStateOf(1) }

    fun resetStageDrag() {
        draggedStageId = null
        draggedStageStartIndex = null
        draggedStageOffsetY = 0f
        draggedStageHeightPx = 1
    }

    fun finishStageDrag() {
        val fromIndex = draggedStageStartIndex
        val stageId = draggedStageId
        if (fromIndex != null && stageId != null && uiState.stages.getOrNull(fromIndex)?.id == stageId) {
            val offsetRows = (draggedStageOffsetY / draggedStageHeightPx.coerceAtLeast(1)).roundToInt()
            val toIndex = (fromIndex + offsetRows).coerceIn(uiState.stages.indices)
            onMoveStage(fromIndex, toIndex)
        }
        resetStageDrag()
    }

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
                onStageColorChanged = { colorHex -> onStageColorChanged(stage.id, colorHex) },
                onCopy = { onCopyStage(stage.id) },
                onRemove = { onRemoveStage(stage.id) },
                onMoveUp = { onMoveStageUp(stage.id) },
                onMoveDown = { onMoveStageDown(stage.id) },
                canMoveUp = uiState.canMoveStageUp(stage.id),
                canMoveDown = uiState.canMoveStageDown(stage.id),
                isDragging = draggedStageId == stage.id,
                dragOffsetY = if (draggedStageId == stage.id) draggedStageOffsetY else 0f,
                onDragStarted = { cardHeightPx ->
                    draggedStageId = stage.id
                    draggedStageStartIndex = uiState.stages.indexOfFirst { currentStage ->
                        currentStage.id == stage.id
                    }
                    draggedStageOffsetY = 0f
                    draggedStageHeightPx = cardHeightPx.coerceAtLeast(1)
                },
                onDragChanged = { draggedStageOffsetY += it },
                onDragStopped = ::finishStageDrag,
                onDragCanceled = ::resetStageDrag
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
            text = "热身固定在开头，放松固定在最后；中间的工作、休息和自定义阶段可排序。计时训练不再选择动作库动作。",
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
    onStageColorChanged: (String) -> Unit,
    onCopy: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    onDragStarted: (Int) -> Unit,
    onDragChanged: (Float) -> Unit,
    onDragStopped: () -> Unit,
    onDragCanceled: () -> Unit
) {
    var cardHeightPx by remember(stage.id) { mutableStateOf(1) }
    val dragModifier = if (isDragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = dragOffsetY
                alpha = 0.92f
                shadowElevation = 14f
            }
    } else {
        Modifier
    }

    EditorCard(
        modifier = Modifier
            .onSizeChanged { size -> cardHeightPx = size.height }
            .then(dragModifier)
    ) {
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
            StageDragHandle(
                enabled = totalCount > 1,
                isDragging = isDragging,
                onDragStarted = { onDragStarted(cardHeightPx) },
                onDragChanged = onDragChanged,
                onDragStopped = onDragStopped,
                onDragCanceled = onDragCanceled
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onMoveUp, enabled = canMoveUp) { Text("上移") }
            TextButton(onClick = onMoveDown, enabled = canMoveDown) { Text("下移") }
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
        Text(
            text = "阶段类型会同步图标；热身 / 放松是固定边界阶段。",
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral700
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimedStageColorOptions.forEach { colorHex ->
                StageColorSwatchButton(
                    colorHex = colorHex,
                    selected = stage.colorHex.equals(colorHex, ignoreCase = true),
                    onClick = { onStageColorChanged(colorHex) }
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
private fun StageColorSwatchButton(
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = colorHex.toComposeColor(),
        border = BorderStroke(
            width = if (selected) 3.dp else 1.dp,
            color = if (selected) TrainFlowPrimary else TrainFlowNeutral100
        ),
        modifier = Modifier
            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .semantics { contentDescription = "阶段颜色 $colorHex" }
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (selected) "✓" else "",
                style = MaterialTheme.typography.labelLarge,
                color = TrainFlowPrimary
            )
        }
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
            text = "长按阶段卡右侧“拖动”手柄可拖拽排序；热身固定在开头，放松固定在最后，上移 / 下移保留为备用排序路径。",
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
private fun StageDragHandle(
    enabled: Boolean,
    isDragging: Boolean,
    onDragStarted: () -> Unit,
    onDragChanged: (Float) -> Unit,
    onDragStopped: () -> Unit,
    onDragCanceled: () -> Unit
) {
    val containerColor = if (isDragging) {
        TrainFlowNeutral100
    } else {
        MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = Modifier
            .sizeIn(minWidth = 56.dp, minHeight = 48.dp)
            .semantics { contentDescription = "阶段拖拽排序手柄" }
            .stageDragHandleGestures(
                enabled = enabled,
                onDragStarted = onDragStarted,
                onDragChanged = onDragChanged,
                onDragStopped = onDragStopped,
                onDragCanceled = onDragCanceled
            ),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "拖动",
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) TrainFlowNeutral700 else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Modifier.stageDragHandleGestures(
    enabled: Boolean,
    onDragStarted: () -> Unit,
    onDragChanged: (Float) -> Unit,
    onDragStopped: () -> Unit,
    onDragCanceled: () -> Unit
): Modifier {
    if (!enabled) return this
    return pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { onDragStarted() },
            onDragEnd = onDragStopped,
            onDragCancel = onDragCanceled,
            onDrag = { change, dragAmount ->
                change.consume()
                onDragChanged(dragAmount.y)
            }
        )
    }
}

@Composable
private fun EditorCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
