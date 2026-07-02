package com.liujyks.trainflow.feature.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.liujyks.trainflow.core.model.TIMED_COMPOSITION_MAX_TARGETS_PER_STAGE_GROUP
import com.liujyks.trainflow.core.model.TimedStageStyle
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.stageColorPresetFor
import com.liujyks.trainflow.ui.components.StageIconImage
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
import java.time.Instant

@Composable
internal fun TimedPlanEditorRoute(
    onBackToHome: () -> Unit,
    onStartTimedPlan: (WorkoutPlan) -> Unit = {},
    onSaveTimedPlan: (WorkoutPlan) -> Unit = {},
    modifier: Modifier = Modifier,
    planEditorDefaults: PlanEditorDefaults = PlanEditorDefaults(),
    initialPlan: WorkoutPlan? = null
) {
    val draftPlanId = rememberSaveable(initialPlan?.id) {
        initialPlan?.id ?: "plan-timed-${System.currentTimeMillis()}"
    }
    var uiState by remember(initialPlan?.id) {
        mutableStateOf(
            initialPlan?.toTimedCompositionPlanEditorState(defaults = planEditorDefaults)
                ?: buildDefaultTimedCompositionPlanEditorState(
                    planId = draftPlanId,
                    defaults = planEditorDefaults
                )
        )
    }

    TimedPlanEditorScreen(
        uiState = uiState,
        onBackToHome = onBackToHome,
        onTitleChanged = { uiState = uiState.updateTitle(it) },
        onDescriptionChanged = { uiState = uiState.updateDescription(it) },
        onWarmupChanged = { uiState = uiState.updateWarmupText(it) },
        onCooldownChanged = { uiState = uiState.updateCooldownText(it) },
        onRoundsChanged = { uiState = uiState.updateRoundsText(it) },
        onRestBetweenRoundsChanged = { uiState = uiState.updateRestBetweenRoundsText(it) },
        onBoundaryStyleChanged = { target, style ->
            uiState = uiState.updateBoundaryStageStyle(target, style)
        },
        onStageNameChanged = { stageId, name -> uiState = uiState.updateStageName(stageId, name) },
        onStageStyleChanged = { stageId, style -> uiState = uiState.updateStageStyle(stageId, style) },
        onCopyStage = { stageId -> uiState = uiState.copyStage(stageId) },
        onRemoveStage = { stageId -> uiState = uiState.removeStage(stageId) },
        onReorderStages = { stageIds -> uiState = uiState.reorderStages(stageIds) },
        onAddStage = { uiState = uiState.addStage() },
        onAddTarget = { stageId -> uiState = uiState.addTarget(stageId) },
        onRemoveTarget = { stageId, targetId -> uiState = uiState.removeTarget(stageId, targetId) },
        onTargetNameChanged = { stageId, targetId, name ->
            uiState = uiState.updateTargetName(stageId, targetId, name)
        },
        onTargetDurationChanged = { stageId, targetId, seconds ->
            uiState = uiState.updateTargetDurationText(stageId, targetId, seconds)
        },
        onTargetStyleChanged = { stageId, targetId, style ->
            uiState = uiState.updateTargetStyle(stageId, targetId, style)
        },
        onCopyTarget = { stageId, targetId ->
            uiState = uiState.copyTarget(stageId, targetId)
        },
        onMoveTarget = { stageId, fromIndex, toIndex ->
            uiState = uiState.moveTarget(stageId, fromIndex, toIndex)
        },
        onSaveDraft = {
            if (uiState.canSave) {
                val plan = uiState.toWorkoutPlan(timestamp = Instant.now().toString())
                onSaveTimedPlan(plan)
                uiState = uiState.markPlanSaved(plan)
            } else {
                uiState = uiState.saveDraftPlan()
            }
        },
        onStartTimedPlan = {
            if (uiState.canStartTraining) {
                onStartTimedPlan(uiState.toWorkoutPlan(timestamp = DefaultTimedPlanTimestamp))
            }
        },
        modifier = modifier
    )
}

@Composable
private fun TimedPlanEditorScreen(
    uiState: TimedCompositionPlanEditorScreenState,
    onBackToHome: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onWarmupChanged: (String) -> Unit,
    onCooldownChanged: (String) -> Unit,
    onRoundsChanged: (String) -> Unit,
    onRestBetweenRoundsChanged: (String) -> Unit,
    onBoundaryStyleChanged: (TimedCompositionBoundaryStyleTarget, TimedStageStyle) -> Unit,
    onStageNameChanged: (String, String) -> Unit,
    onStageStyleChanged: (String, TimedStageStyle) -> Unit,
    onCopyStage: (String) -> Unit,
    onRemoveStage: (String) -> Unit,
    onReorderStages: (List<String>) -> Unit,
    onAddStage: () -> Unit,
    onAddTarget: (String) -> Unit,
    onRemoveTarget: (String, String) -> Unit,
    onTargetNameChanged: (String, String, String) -> Unit,
    onTargetDurationChanged: (String, String, String) -> Unit,
    onTargetStyleChanged: (String, String, TimedStageStyle) -> Unit,
    onCopyTarget: (String, String) -> Unit,
    onMoveTarget: (String, Int, Int) -> Unit,
    onSaveDraft: () -> Unit,
    onStartTimedPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dragItemGapPx = with(LocalDensity.current) { 16.dp.roundToPx() }
    var draggedStageId by remember { mutableStateOf<String?>(null) }
    var draggedStageOrderIds by remember { mutableStateOf<List<String>?>(null) }
    var draggedStageStartIndex by remember { mutableStateOf(-1) }
    var draggedStageTargetIndex by remember { mutableStateOf(-1) }
    var draggedStageOffsetY by remember { mutableStateOf(0f) }
    var draggedStageHeightPx by remember { mutableStateOf(1) }
    var stylePickerBoundaryTarget by remember { mutableStateOf<TimedCompositionBoundaryStyleTarget?>(null) }
    var stylePickerStageId by remember { mutableStateOf<String?>(null) }
    var stylePickerTargetRef by remember { mutableStateOf<Pair<String, String>?>(null) }
    var expandedStageIds by rememberSaveable(uiState.stageGroups.map { it.id }.joinToString("|")) {
        mutableStateOf(emptyList<String>())
    }
    var expandedTargetIds by rememberSaveable(
        uiState.stageGroups.flatMap { group -> group.targets.map { target -> target.id } }.joinToString("|")
    ) {
        mutableStateOf(emptyList<String>())
    }

    fun resetStageDrag() {
        draggedStageId = null
        draggedStageOrderIds = null
        draggedStageStartIndex = -1
        draggedStageTargetIndex = -1
        draggedStageOffsetY = 0f
        draggedStageHeightPx = 1
    }

    fun updateStageDrag(deltaY: Float) {
        val stageId = draggedStageId
        val sourceOrderIds = draggedStageOrderIds
        val startIndex = draggedStageStartIndex
        draggedStageOffsetY += deltaY
        if (stageId != null && sourceOrderIds != null && startIndex in sourceOrderIds.indices) {
            if (sourceOrderIds[startIndex] == stageId) {
                draggedStageTargetIndex = draggedItemTargetIndex(
                    fromIndex = startIndex,
                    dragOffsetPx = draggedStageOffsetY,
                    itemHeightPx = draggedStageHeightPx,
                    lastIndex = sourceOrderIds.lastIndex
                )
            }
        }
    }

    fun finishStageDrag() {
        draggedStageOrderIds
            ?.withItemMoved(draggedStageStartIndex, draggedStageTargetIndex)
            ?.let(onReorderStages)
        resetStageDrag()
    }

    val displayedStages = uiState.stageGroups

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(TrainFlowSurfaceMuted)
                .padding(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TimedPlanEditorHeader(onBackToHome = onBackToHome)
            }

            item {
                PlanBasicsCard(
                    uiState = uiState,
                    onTitleChanged = onTitleChanged,
                    onDescriptionChanged = onDescriptionChanged
                )
            }

            item {
                BaseTimeAndRoundsCard(
                    uiState = uiState,
                    onWarmupChanged = onWarmupChanged,
                    onCooldownChanged = onCooldownChanged,
                    onRoundsChanged = onRoundsChanged,
                    onRestBetweenRoundsChanged = onRestBetweenRoundsChanged,
                    onBoundaryStylePicker = { target -> stylePickerBoundaryTarget = target }
                )
            }

            item {
                SectionTitle(text = "阶段编排 · 轮内重复 · 默认折叠")
            }

            itemsIndexed(displayedStages, key = { _, stage -> stage.id }) { index, stage ->
                val stageExpanded = stage.id in expandedStageIds
                val isStageDragging = draggedStageId == stage.id
                val placeholderShiftY = if (isStageDragging) {
                    0
                } else {
                    placeholderShiftForIndexPx(
                        index = index,
                        draggedIndex = draggedStageStartIndex,
                        targetIndex = draggedStageTargetIndex,
                        draggedItemHeightPx = draggedStageHeightPx,
                        itemGapPx = dragItemGapPx
                    )
                }

                TimedCompositionStageCard(
                    modifier = (if (isStageDragging) Modifier else Modifier.animateItem())
                        .graphicsLayer { translationY = placeholderShiftY.toFloat() },
                    stage = stage,
                    index = index,
                    totalCount = displayedStages.size,
                    expanded = stageExpanded,
                    expandedTargetIds = expandedTargetIds,
                    onExpandedChanged = { expanded ->
                        expandedStageIds = if (expanded) {
                            (expandedStageIds + stage.id).distinct()
                        } else {
                            expandedStageIds - stage.id
                        }
                    },
                    onTargetExpandedChanged = { targetId, expanded ->
                        expandedTargetIds = if (expanded) {
                            (expandedTargetIds + targetId).distinct()
                        } else {
                            expandedTargetIds - targetId
                        }
                    },
                    onNameChanged = { name -> onStageNameChanged(stage.id, name) },
                    onOpenStylePicker = { stylePickerStageId = stage.id },
                    onCopy = { onCopyStage(stage.id) },
                    onRemove = { onRemoveStage(stage.id) },
                    onAddTarget = { onAddTarget(stage.id) },
                    canAddTarget = uiState.canAddTarget(stage.id),
                    onRemoveTarget = { targetId -> onRemoveTarget(stage.id, targetId) },
                    onTargetNameChanged = { targetId, name -> onTargetNameChanged(stage.id, targetId, name) },
                    onTargetDurationChanged = { targetId, seconds ->
                        onTargetDurationChanged(stage.id, targetId, seconds)
                    },
                    onTargetStylePicker = { target -> stylePickerTargetRef = stage.id to target.id },
                    onCopyTarget = { targetId -> onCopyTarget(stage.id, targetId) },
                    onMoveTarget = { fromTargetIndex, toTargetIndex ->
                        onMoveTarget(stage.id, fromTargetIndex, toTargetIndex)
                    },
                    isDragging = isStageDragging,
                    dragOffsetY = if (isStageDragging) draggedStageOffsetY else 0f,
                    onDragStarted = { cardHeightPx ->
                        draggedStageId = stage.id
                        draggedStageOrderIds = displayedStages.map { displayedStage -> displayedStage.id }
                        draggedStageStartIndex = index
                        draggedStageTargetIndex = index
                        draggedStageOffsetY = 0f
                        draggedStageHeightPx = cardHeightPx.coerceAtLeast(1)
                    },
                    onDragChanged = ::updateStageDrag,
                    onDragStopped = ::finishStageDrag,
                    onDragCanceled = ::resetStageDrag
                )
            }

            item {
                AddTimedStageCard(onAddStage = onAddStage)
            }

            item {
                SaveAndPreviewCard(uiState = uiState)
            }

            item {
                Spacer(modifier = Modifier.height(PlanEditorStickyActionReserveHeight))
            }
        }

        PlanEditorStickyActions(
            onSavePlan = onSaveDraft,
            onStartTraining = onStartTimedPlan,
            saveEnabled = uiState.canSave,
            startEnabled = uiState.canStartTraining,
            startDisabledReason = uiState.startDisabledReason,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    stylePickerBoundaryTarget?.let { target ->
        val picker = uiState.toBoundaryStylePickerUiState(target)
        StageStylePickerDialog(
            title = "阶段样式",
            currentText = "${target.displayLabel}当前为 ${picker.selectedColorName} · ${picker.selectedIconLabel}。",
            picker = picker,
            onDismiss = { stylePickerBoundaryTarget = null },
            onStyleSelected = { style -> onBoundaryStyleChanged(target, style) }
        )
    }
    stylePickerStageId?.let { stageId ->
        val stage = uiState.stageGroups.firstOrNull { candidate -> candidate.id == stageId }
        if (stage != null) {
            val picker = stage.toStageStylePickerUiState()
            StageStylePickerDialog(
                title = "阶段样式",
                currentText = "${stage.name} 当前为 ${picker.selectedColorName} · ${picker.selectedIconLabel}。",
                picker = picker,
                onDismiss = { stylePickerStageId = null },
                onStyleSelected = { style -> onStageStyleChanged(stage.id, style) }
            )
        }
    }
    stylePickerTargetRef?.let { (stageId, targetId) ->
        val target = uiState.stageGroups
            .firstOrNull { stage -> stage.id == stageId }
            ?.targets
            ?.firstOrNull { candidate -> candidate.id == targetId }
        if (target != null) {
            val picker = target.toStageStylePickerUiState()
            StageStylePickerDialog(
                title = "目标样式",
                currentText = "${target.name} 当前为 ${picker.selectedColorName} · ${picker.selectedIconLabel}。",
                picker = picker,
                onDismiss = { stylePickerTargetRef = null },
                onStyleSelected = { style -> onTargetStyleChanged(stageId, target.id, style) }
            )
        }
    }
}

@Composable
private fun TimedPlanEditorHeader(onBackToHome: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onBackToHome),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, TrainFlowNeutral100)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("‹", style = MaterialTheme.typography.headlineSmall, color = TrainFlowPrimary)
            }
        }
        Text(
            text = "编辑计时计划",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, TrainFlowNeutral100)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("•••", style = MaterialTheme.typography.titleLarge, color = TrainFlowPrimary)
            }
        }
    }
}

@Composable
private fun PlanBasicsCard(
    uiState: TimedCompositionPlanEditorScreenState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit
) {
    EditorCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = uiState.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "热身 + ${uiState.rounds} 轮 x ${uiState.stageGroups.size} 阶段 + ${uiState.rounds - 1} 次轮休 + 放松 · 总阶段 ${uiState.totalTimelineStageCount()}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TrainFlowNeutral700
                )
            }
            StatusPill(text = "草稿", color = TrainFlowNeutral100, contentColor = TrainFlowPrimary)
        }
    }
}

@Composable
private fun BaseTimeAndRoundsCard(
    uiState: TimedCompositionPlanEditorScreenState,
    onWarmupChanged: (String) -> Unit,
    onCooldownChanged: (String) -> Unit,
    onRoundsChanged: (String) -> Unit,
    onRestBetweenRoundsChanged: (String) -> Unit,
    onBoundaryStylePicker: (TimedCompositionBoundaryStyleTarget) -> Unit
) {
    EditorCard {
        SectionTitle(text = "基础时间与轮次")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "热身",
                value = uiState.warmupText,
                onValueChanged = onWarmupChanged,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "放松",
                value = uiState.cooldownText,
                onValueChanged = onCooldownChanged,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "轮数",
                value = uiState.roundsText,
                onValueChanged = onRoundsChanged,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "轮间休息",
                value = uiState.restBetweenRoundsText,
                onValueChanged = onRestBetweenRoundsChanged,
                modifier = Modifier.weight(1f)
            )
        }
        SectionTitle(text = "阶段样式")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TimedCompositionBoundaryStyleTarget.entries.forEach { target ->
                val style = uiState.boundaryStageStyle(target)
                StyleEntryButton(
                    label = target.displayLabel,
                    style = style,
                    contentDescription = "修改${target.displayLabel}阶段样式，当前${stageStyleColorLabel(style.colorHex)}，图标${stageStyleIconLabel(style.iconKey)}",
                    onClick = { onBoundaryStylePicker(target) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TimedCompositionStageCard(
    modifier: Modifier = Modifier,
    stage: TimedCompositionStageGroupEditorUiState,
    index: Int,
    totalCount: Int,
    expanded: Boolean,
    expandedTargetIds: List<String>,
    onExpandedChanged: (Boolean) -> Unit,
    onTargetExpandedChanged: (String, Boolean) -> Unit,
    onNameChanged: (String) -> Unit,
    onOpenStylePicker: () -> Unit,
    onCopy: () -> Unit,
    onRemove: () -> Unit,
    onAddTarget: () -> Unit,
    canAddTarget: Boolean,
    onRemoveTarget: (String) -> Unit,
    onTargetNameChanged: (String, String) -> Unit,
    onTargetDurationChanged: (String, String) -> Unit,
    onTargetStylePicker: (TimedCompositionTargetEditorUiState) -> Unit,
    onCopyTarget: (String) -> Unit,
    onMoveTarget: (Int, Int) -> Unit,
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
        modifier = modifier
            .onSizeChanged { size -> cardHeightPx = size.height }
            .then(dragModifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StyleSwatchButton(
                style = TimedStageStyle(colorHex = stage.colorHex, iconKey = stage.iconKey),
                size = 34.dp,
                contentDescription = "修改阶段样式，当前${stageStyleColorLabel(stage.colorHex)}，图标${stageStyleIconLabel(stage.iconKey)}",
                onClick = onOpenStylePicker
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExpandedChanged(!expanded) },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "阶段${index.toChineseOrdinal()}·${stage.name}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stage.durationSec.formatSecondsLabel(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.width(84.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TogglePill(
                    text = if (expanded) "收起" else "展开",
                    onClick = { onExpandedChanged(!expanded) },
                    modifier = Modifier.weight(1f)
                )
                StageDragHandle(
                    enabled = totalCount > 1,
                    isDragging = isDragging,
                    onDragStarted = { onDragStarted(cardHeightPx) },
                    onDragChanged = onDragChanged,
                    onDragStopped = onDragStopped,
                    onDragCanceled = onDragCanceled
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier.padding(start = 42.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactTextField(
                    label = "阶段名称",
                    value = stage.name,
                    onValueChanged = onNameChanged,
                    modifier = Modifier.fillMaxWidth()
                )
                DerivedDurationBar(
                    label = "阶段总时长",
                    value = stage.durationSec.formatSecondsLabel()
                )
                SectionTitle(text = "阶段内目标")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stage.targets.forEachIndexed { targetIndex, target ->
                        TimedCompositionTargetRow(
                            target = target,
                            index = targetIndex,
                            totalCount = stage.targets.size,
                            expanded = target.id in expandedTargetIds,
                            onExpandedChanged = { targetExpanded ->
                                onTargetExpandedChanged(target.id, targetExpanded)
                            },
                            onNameChanged = { name -> onTargetNameChanged(target.id, name) },
                            onDurationChanged = { seconds -> onTargetDurationChanged(target.id, seconds) },
                            onStylePicker = { onTargetStylePicker(target) },
                            onCopy = { onCopyTarget(target.id) },
                            onRemove = { onRemoveTarget(target.id) },
                            canCopy = canAddTarget,
                            canRemove = stage.targets.size > 1,
                            onMove = { toIndex -> onMoveTarget(targetIndex, toIndex) }
                        )
                    }
                }
                CompactIconButton(
                    symbol = "+",
                    contentDescription = if (canAddTarget) "增加目标，最多 5 个" else "已达 5 个目标",
                    onClick = onAddTarget,
                    enabled = canAddTarget,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactIconButton(
                        symbol = "⧉",
                        contentDescription = "复制阶段，按相同参数新增阶段",
                        onClick = onCopy,
                        modifier = Modifier.weight(1f)
                    )
                    CompactIconButton(
                        symbol = "×",
                        contentDescription = "删除阶段",
                        onClick = onRemove,
                        enabled = totalCount > 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimedCompositionTargetRow(
    target: TimedCompositionTargetEditorUiState,
    index: Int,
    totalCount: Int,
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    onNameChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onStylePicker: () -> Unit,
    onCopy: () -> Unit,
    onRemove: () -> Unit,
    canCopy: Boolean,
    canRemove: Boolean,
    onMove: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyleSwatchButton(
                    style = TimedStageStyle(colorHex = target.colorHex, iconKey = target.iconKey),
                    size = 30.dp,
                    contentDescription = "修改目标样式，当前${stageStyleColorLabel(target.colorHex)}，图标${stageStyleIconLabel(target.iconKey)}",
                    onClick = onStylePicker
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onExpandedChanged(!expanded) },
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = "${(index + 1).toString().padStart(2, '0')} ${target.name}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.width(124.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DurationPill(text = target.durationSec.formatSecondsLabel(), modifier = Modifier.weight(1f))
                    TogglePill(
                        text = if (expanded) "收起" else "设置",
                        onClick = { onExpandedChanged(!expanded) },
                        modifier = Modifier.width(42.dp)
                    )
                    TargetDragHandle(
                        canMoveUp = index > 0,
                        canMoveDown = index < totalCount - 1,
                        onMoveUp = { onMove(index - 1) },
                        onMoveDown = { onMove(index + 1) }
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp, end = 8.dp, bottom = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CompactTextField(
                            label = "目标名称",
                            value = target.name,
                            onValueChanged = onNameChanged,
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            label = "时长",
                            value = target.durationText,
                            onValueChanged = onDurationChanged,
                            modifier = Modifier.weight(0.78f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.End)
                    ) {
                        CompactIconButton(
                            symbol = "⧉",
                            contentDescription = if (canCopy) {
                                "复制目标，按相同参数新增到下方"
                            } else {
                                "已达 5 个目标，无法复制目标"
                            },
                            onClick = onCopy,
                            enabled = canCopy,
                            modifier = Modifier.width(52.dp)
                        )
                        CompactIconButton(
                            symbol = "×",
                            contentDescription = "删除目标",
                            onClick = onRemove,
                            enabled = canRemove,
                            modifier = Modifier.width(52.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactIconButton(
    symbol: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val contentColor = if (enabled) TrainFlowNeutral700 else TrainFlowNeutral700.copy(alpha = 0.42f)
    Surface(
        modifier = modifier
            .height(38.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun CompactTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.sizeIn(minHeight = 62.dp),
        shape = RoundedCornerShape(8.dp),
        color = TrainFlowSurfaceMuted,
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TrainFlowNeutral700
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun NumberInputField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.sizeIn(minHeight = 62.dp),
        shape = RoundedCornerShape(8.dp),
        color = TrainFlowSurfaceMuted,
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TrainFlowNeutral700
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { input -> onValueChanged(input.sanitizeIntegerInput()) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(text = "秒", style = MaterialTheme.typography.labelLarge, color = TrainFlowNeutral700)
            }
        }
    }
}

@Composable
private fun DerivedDurationBar(
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1FAF6),
        border = BorderStroke(1.dp, Color(0xFFCFE8DC))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18382C)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18382C)
            )
        }
    }
}

@Composable
private fun DurationPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.sizeIn(minHeight = 38.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TogglePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .sizeIn(minHeight = 38.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFEEF4F1)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF253A31)
            )
        }
    }
}

@Composable
private fun StyleSwatchButton(
    style: TimedStageStyle,
    size: Dp,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorHex = style.colorHex ?: "#A8B3BE"
    val iconKey = style.iconKey ?: "custom"
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colorHex.toComposeColor(),
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Box(contentAlignment = Alignment.Center) {
            StageStyleIconGlyph(
                iconKey = iconKey,
                modifier = Modifier.size(size * 0.62f)
            )
        }
    }
}

@Composable
private fun StyleEntryButton(
    label: String,
    style: TimedStageStyle,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .sizeIn(minHeight = 54.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyleSwatchButton(
                style = style,
                size = 34.dp,
                contentDescription = contentDescription,
                onClick = onClick
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${stageStyleColorLabel(style.colorHex)} · ${stageStyleIconLabel(style.iconKey)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "设置",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TrainFlowPrimary
            )
        }
    }
}

@Composable
private fun StageStylePickerDialog(
    title: String,
    currentText: String,
    picker: StageStylePickerUiState,
    onDismiss: () -> Unit,
    onStyleSelected: (TimedStageStyle) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        },
        title = {
            Text(title)
        },
        text = {
            Column(
                modifier = Modifier
                    .sizeIn(maxHeight = 720.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = currentText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrainFlowNeutral700
                )
                StageColorSection(
                    title = "推荐色",
                    options = picker.colorPicker.recommendedColors,
                    onColorSelected = { colorHex ->
                        onStyleSelected(
                            TimedStageStyle(colorHex = colorHex, iconKey = picker.selectedIconKey)
                        )
                    }
                )
                StageIconSection(
                    options = picker.iconOptions,
                    selectedColorHex = picker.selectedColorHex,
                    onIconSelected = { iconKey ->
                        onStyleSelected(
                            TimedStageStyle(colorHex = picker.selectedColorHex, iconKey = iconKey)
                        )
                    }
                )
                StageColorSection(
                    title = "更多颜色",
                    options = picker.colorPicker.moreColors,
                    onColorSelected = { colorHex ->
                        onStyleSelected(
                            TimedStageStyle(colorHex = colorHex, iconKey = picker.selectedIconKey)
                        )
                    }
                )
            }
        }
    )
}

@Composable
private fun StageColorSection(
    title: String,
    options: List<StageColorOptionUiState>,
    onColorSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.chunked(4).forEach { rowOptions ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { option ->
                        StageColorSwatchButton(
                            option = option,
                            onClick = { onColorSelected(option.hex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StageColorSwatchButton(
    option: StageColorOptionUiState,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = option.hex.toComposeColor(),
        border = BorderStroke(
            width = if (option.selected) 3.dp else 1.dp,
            color = if (option.selected) TrainFlowPrimary else TrainFlowNeutral100
        ),
        modifier = Modifier
            .size(52.dp)
            .semantics {
                contentDescription = option.contentDescription
                stateDescription = if (option.selected) "已选中" else "未选中"
            }
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (option.hasCheckIndicator) "✓" else "",
                style = MaterialTheme.typography.labelLarge,
                color = option.textColor.toComposeColor(defaultColor = TrainFlowPrimary)
            )
        }
    }
}

@Composable
private fun StageIconSection(
    options: List<StageIconOptionUiState>,
    selectedColorHex: String,
    onIconSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "内置图标",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.chunked(4).forEach { rowOptions ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { option ->
                        StageIconOptionButton(
                            option = option,
                            selectedColorHex = selectedColorHex,
                            onClick = { onIconSelected(option.key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StageIconOptionButton(
    option: StageIconOptionUiState,
    selectedColorHex: String,
    onClick: () -> Unit
) {
    val containerColor = if (option.selected) {
        selectedColorHex.toComposeColor()
    } else {
        TrainFlowNeutral700
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(
            width = if (option.selected) 3.dp else 1.dp,
            color = if (option.selected) TrainFlowPrimary else TrainFlowNeutral100
        ),
        modifier = Modifier
            .size(width = 64.dp, height = 62.dp)
            .semantics {
                contentDescription = option.contentDescription
                stateDescription = if (option.selected) "已选中" else "未选中"
            }
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StageStyleIconGlyph(
                iconKey = option.key,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = option.label,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun StageStyleIconGlyph(
    iconKey: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    StageIconImage(
        iconKey = iconKey,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun AddTimedStageCard(onAddStage: () -> Unit) {
    EditorCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(text = "添加阶段")
            OutlinedButton(onClick = onAddStage) {
                Text("+ 阶段")
            }
        }
        Text(
            text = "阶段内最多 ${TIMED_COMPOSITION_MAX_TARGETS_PER_STAGE_GROUP} 个目标。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun SaveAndPreviewCard(uiState: TimedCompositionPlanEditorScreenState) {
    EditorCard {
        SectionTitle(text = "计划预览")
        Text(
            text = uiState.summary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "热身 ${uiState.warmupSec.formatDuration()} · 放松 ${uiState.cooldownSec.formatDuration()} · 轮间休息 ${uiState.restBetweenRoundsSec.formatDuration()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        uiState.statusMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.bodyMedium, color = TrainFlowNeutral700)
        }
        uiState.validationMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        uiState.savedPlan?.let { plan ->
            Text(
                text = "已保存：${plan.title} · ${uiState.stageGroups.size} 个阶段 · ${uiState.rounds} 轮",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "预览卡仅用于确认计划摘要，保存和开始训练操作固定在底部操作区。",
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral700
        )
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
            .sizeIn(minWidth = 30.dp, minHeight = 38.dp)
            .semantics { contentDescription = "阶段拖拽排序手柄" }
            .stageDragHandleGestures(
                enabled = enabled,
                onDragStarted = onDragStarted,
                onDragChanged = onDragChanged,
                onDragStopped = onDragStopped,
                onDragCanceled = onDragCanceled
        ),
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "≡",
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) TrainFlowNeutral700 else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TargetDragHandle(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var dragOffsetY by remember { mutableStateOf(0f) }
    Surface(
        modifier = Modifier
            .sizeIn(minWidth = 34.dp, minHeight = 44.dp)
            .semantics { contentDescription = "目标拖拽排序手柄" }
            .pointerInput(canMoveUp, canMoveDown) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragOffsetY = 0f },
                    onDragEnd = {
                        when {
                            dragOffsetY < -24f && canMoveUp -> onMoveUp()
                            dragOffsetY > 24f && canMoveDown -> onMoveDown()
                        }
                        dragOffsetY = 0f
                    },
                    onDragCancel = { dragOffsetY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "≡",
                style = MaterialTheme.typography.titleLarge,
                color = if (canMoveUp || canMoveDown) TrainFlowNeutral700 else MaterialTheme.colorScheme.onSurfaceVariant
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
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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

private fun Int.formatSecondsLabel(): String = "${this}s"

private fun Int.toChineseOrdinal(): String {
    return when (this + 1) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        7 -> "七"
        8 -> "八"
        9 -> "九"
        10 -> "十"
        else -> (this + 1).toString()
    }
}

private fun TimedCompositionPlanEditorScreenState.totalTimelineStageCount(): Int {
    return warmupSec.positiveStageCount() +
        cooldownSec.positiveStageCount() +
        (stageGroups.size * rounds) +
        ((rounds - 1).coerceAtLeast(0) * restBetweenRoundsSec.positiveStageCount())
}

private fun Int.positiveStageCount(): Int = if (this > 0) 1 else 0

private fun stageStyleColorLabel(colorHex: String?): String {
    return stageColorPresetFor(colorHex)?.name ?: "自定义颜色"
}

private fun String.toComposeColor(defaultColor: Color = TrainFlowAccent): Color {
    return runCatching { Color(android.graphics.Color.parseColor(this)) }
        .getOrElse { defaultColor }
}

@Preview(showBackground = true)
@Composable
private fun TimedPlanEditorRoutePreview() {
    TrainFlowTheme {
        TimedPlanEditorRoute(onBackToHome = {})
    }
}
