package com.liujyks.trainflow.feature.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.ui.theme.TrainFlowAction
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral200
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
import java.time.Instant

@Composable
internal fun StrengthPlanEditorRoute(
    onBackToHome: () -> Unit,
    onStartStrengthPlan: (WorkoutPlan) -> Unit = {},
    onSaveStrengthPlan: (WorkoutPlan) -> Unit = {},
    modifier: Modifier = Modifier,
    planEditorDefaults: PlanEditorDefaults = PlanEditorDefaults(),
    initialPlan: WorkoutPlan? = null
) {
    val draftPlanId = rememberSaveable(initialPlan?.id) {
        initialPlan?.id ?: "plan-strength-${System.currentTimeMillis()}"
    }
    var uiState by remember(initialPlan?.id) {
        mutableStateOf(
            initialPlan?.toStrengthPlanEditorState(defaults = planEditorDefaults)
                ?: buildDefaultStrengthPlanEditorState(defaults = planEditorDefaults)
        )
    }

    StrengthPlanEditorScreen(
        uiState = uiState,
        onBackToHome = onBackToHome,
        onTitleChanged = { uiState = uiState.updateTitle(it) },
        onDescriptionChanged = { uiState = uiState.updateDescription(it) },
        onStrengthSetTimerModeChanged = { mode -> uiState = uiState.updateStrengthSetTimerMode(mode) },
        onTargetWeightChanged = { itemId, input -> uiState = uiState.updateTargetWeightText(itemId, input) },
        onRepRangeChanged = { itemId, minRepsInput, maxRepsInput ->
            uiState = uiState.updateRepRangeText(itemId, minRepsInput, maxRepsInput)
        },
        onFixedRepsChanged = { itemId, input -> uiState = uiState.updateFixedRepsText(itemId, input) },
        onWorkingSetsChanged = { itemId, input -> uiState = uiState.updateWorkingSetsText(itemId, input) },
        onWarmupSetsChanged = { itemId, input -> uiState = uiState.updateWarmupSetsText(itemId, input) },
        onRestAfterSetChanged = { itemId, input -> uiState = uiState.updateRestAfterSetText(itemId, input) },
        onSetTargetsExpandedChanged = { itemId, expanded ->
            uiState = uiState.setSetTargetsExpanded(itemId, expanded)
        },
        onSetTargetWeightChanged = { itemId, setId, input ->
            uiState = uiState.updateSetTargetWeightText(itemId, setId, input)
        },
        onSetFixedRepsChanged = { itemId, setId, input ->
            uiState = uiState.updateSetFixedRepsText(itemId, setId, input)
        },
        onSetRestAfterChanged = { itemId, setId, input ->
            uiState = uiState.updateSetRestAfterText(itemId, setId, input)
        },
        onAddExercise = { exerciseId -> uiState = uiState.addExercise(exerciseId) },
        onAddCustomExercise = { exerciseName -> uiState = uiState.addCustomExercise(exerciseName) },
        onRemoveExercise = { itemId -> uiState = uiState.removeExercise(itemId) },
        onReorderExercises = { exerciseIds -> uiState = uiState.reorderExercises(exerciseIds) },
        onSaveDraft = {
            if (uiState.canSave) {
                val plan = uiState.toWorkoutPlan(
                    planId = draftPlanId,
                    timestamp = Instant.now().toString()
                )
                onSaveStrengthPlan(plan)
                uiState = uiState.markPlanSaved(plan)
            } else {
                uiState = uiState.saveDraftPlan(planId = draftPlanId)
            }
        },
        onStartStrengthPlan = {
            if (uiState.canStartTraining) {
                onStartStrengthPlan(
                    uiState.toWorkoutPlan(
                        planId = uiState.sourcePlanId ?: "plan-strength-editor-start",
                        timestamp = DefaultStrengthPlanTimestamp
                    )
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun StrengthPlanEditorScreen(
    uiState: StrengthPlanEditorScreenState,
    onBackToHome: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onStrengthSetTimerModeChanged: (StrengthSetTimerMode) -> Unit,
    onTargetWeightChanged: (String, String) -> Unit,
    onRepRangeChanged: (String, String, String) -> Unit,
    onFixedRepsChanged: (String, String) -> Unit,
    onWorkingSetsChanged: (String, String) -> Unit,
    onWarmupSetsChanged: (String, String) -> Unit,
    onRestAfterSetChanged: (String, String) -> Unit,
    onSetTargetsExpandedChanged: (String, Boolean) -> Unit,
    onSetTargetWeightChanged: (String, String, String) -> Unit,
    onSetFixedRepsChanged: (String, String, String) -> Unit,
    onSetRestAfterChanged: (String, String, String) -> Unit,
    onAddExercise: (String) -> Unit,
    onAddCustomExercise: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onReorderExercises: (List<String>) -> Unit,
    onSaveDraft: () -> Unit,
    onStartStrengthPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dragItemGapPx = with(LocalDensity.current) { 16.dp.roundToPx() }
    var draggedExerciseId by remember { mutableStateOf<String?>(null) }
    var draggedExerciseOrderIds by remember { mutableStateOf<List<String>?>(null) }
    var draggedExerciseStartIndex by remember { mutableStateOf(-1) }
    var draggedExerciseTargetIndex by remember { mutableStateOf(-1) }
    var draggedExerciseOffsetY by remember { mutableStateOf(0f) }
    var draggedExerciseHeightPx by remember { mutableStateOf(1) }

    fun resetExerciseDrag() {
        draggedExerciseId = null
        draggedExerciseOrderIds = null
        draggedExerciseStartIndex = -1
        draggedExerciseTargetIndex = -1
        draggedExerciseOffsetY = 0f
        draggedExerciseHeightPx = 1
    }

    fun updateExerciseDrag(deltaY: Float) {
        val exerciseId = draggedExerciseId
        val sourceOrderIds = draggedExerciseOrderIds
        val startIndex = draggedExerciseStartIndex
        draggedExerciseOffsetY += deltaY
        if (exerciseId != null && sourceOrderIds != null && startIndex in sourceOrderIds.indices) {
            if (sourceOrderIds[startIndex] == exerciseId) {
                val toIndex = draggedItemTargetIndex(
                    fromIndex = startIndex,
                    dragOffsetPx = draggedExerciseOffsetY,
                    itemHeightPx = draggedExerciseHeightPx,
                    lastIndex = sourceOrderIds.lastIndex
                )
                draggedExerciseTargetIndex = toIndex
            }
        }
    }

    fun finishExerciseDrag() {
        draggedExerciseOrderIds
            ?.withItemMoved(draggedExerciseStartIndex, draggedExerciseTargetIndex)
            ?.let(onReorderExercises)
        resetExerciseDrag()
    }

    val displayedExercises = uiState.exercises

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
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
                StrengthPlanEditorHeader(uiState)
            }

            item {
                StrengthPlanBasicsCard(
                    uiState = uiState,
                    onTitleChanged = onTitleChanged,
                    onDescriptionChanged = onDescriptionChanged
                )
            }

            item {
                StrengthSetTimerModeCard(
                    selectedMode = uiState.strengthSetTimerMode,
                    onModeChanged = onStrengthSetTimerModeChanged
                )
            }

            item {
                SectionTitle(text = "动作与目标组")
            }

            itemsIndexed(displayedExercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                val isExerciseDragging = draggedExerciseId == exercise.id
                val placeholderShiftY = if (isExerciseDragging) {
                    0
                } else {
                    placeholderShiftForIndexPx(
                        index = index,
                        draggedIndex = draggedExerciseStartIndex,
                        targetIndex = draggedExerciseTargetIndex,
                        draggedItemHeightPx = draggedExerciseHeightPx,
                        itemGapPx = dragItemGapPx
                    )
                }
                StrengthExerciseEditorCard(
                    modifier = (if (isExerciseDragging) Modifier else Modifier.animateItem())
                        .graphicsLayer { translationY = placeholderShiftY.toFloat() },
                    exercise = exercise,
                    index = index,
                    totalCount = displayedExercises.size,
                    canRemove = displayedExercises.size > 1,
                    onTargetWeightChanged = { kg -> onTargetWeightChanged(exercise.id, kg) },
                    onRepRangeChanged = { minReps, maxReps ->
                        onRepRangeChanged(exercise.id, minReps, maxReps)
                    },
                    onFixedRepsChanged = { reps -> onFixedRepsChanged(exercise.id, reps) },
                    onWorkingSetsChanged = { sets -> onWorkingSetsChanged(exercise.id, sets) },
                    onWarmupSetsChanged = { sets -> onWarmupSetsChanged(exercise.id, sets) },
                    onRestAfterSetChanged = { seconds -> onRestAfterSetChanged(exercise.id, seconds) },
                    onSetTargetsExpandedChanged = { expanded ->
                        onSetTargetsExpandedChanged(exercise.id, expanded)
                    },
                    onSetTargetWeightChanged = { setId, kg ->
                        onSetTargetWeightChanged(exercise.id, setId, kg)
                    },
                    onSetFixedRepsChanged = { setId, reps ->
                        onSetFixedRepsChanged(exercise.id, setId, reps)
                    },
                    onSetRestAfterChanged = { setId, seconds ->
                        onSetRestAfterChanged(exercise.id, setId, seconds)
                    },
                    onRemove = { onRemoveExercise(exercise.id) },
                    isDragging = isExerciseDragging,
                    dragOffsetY = if (isExerciseDragging) {
                        draggedExerciseOffsetY
                    } else {
                        0f
                    },
                    onDragStarted = { cardHeightPx ->
                        draggedExerciseId = exercise.id
                        draggedExerciseOrderIds = displayedExercises.map { displayedExercise -> displayedExercise.id }
                        draggedExerciseStartIndex = index
                        draggedExerciseTargetIndex = index
                        draggedExerciseOffsetY = 0f
                        draggedExerciseHeightPx = cardHeightPx.coerceAtLeast(1)
                    },
                    onDragChanged = ::updateExerciseDrag,
                    onDragStopped = ::finishExerciseDrag,
                    onDragCanceled = ::resetExerciseDrag
                )
            }

            item {
                AddStrengthExerciseCard(
                    uiState = uiState,
                    onAddExercise = onAddExercise,
                    onAddCustomExercise = onAddCustomExercise
                )
            }

            item {
                StrengthSaveAndPreviewCard(
                    uiState = uiState
                )
            }

            item {
                Spacer(modifier = Modifier.height(PlanEditorStickyActionReserveHeight))
            }
        }

        PlanEditorStickyActions(
            onSavePlan = onSaveDraft,
            onStartTraining = onStartStrengthPlan,
            saveEnabled = uiState.canSave,
            startEnabled = uiState.canStartTraining,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun StrengthPlanEditorHeader(uiState: StrengthPlanEditorScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(text = "力量计划编辑", color = TrainFlowAction, contentColor = TrainFlowNeutral50)
        Text(
            text = "力量计划编辑",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = uiState.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "可直接开始当前草稿，也可保存为本地计划后从计划详情再次启动。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun StrengthPlanBasicsCard(
    uiState: StrengthPlanEditorScreenState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit
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
        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("计划描述") },
            minLines = 2
        )
    }
}

@Composable
private fun StrengthSetTimerModeCard(
    selectedMode: StrengthSetTimerMode,
    onModeChanged: (StrengthSetTimerMode) -> Unit
) {
    EditorCard {
        SectionTitle(text = "本组计时模式")
        Text(
            text = "当前计划保存后以这里的设置为准；训练偏好只影响新建计划默认值，不会自动覆盖旧计划。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StrengthSetTimerMode.entries.forEach { mode ->
                StrengthSetTimerModeOption(
                    mode = mode,
                    selected = selectedMode == mode,
                    onClick = { onModeChanged(mode) }
                )
            }
        }
    }
}

@Composable
private fun StrengthSetTimerModeOption(
    mode: StrengthSetTimerMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) TrainFlowAccent else TrainFlowNeutral200
    val backgroundColor = if (selected) TrainFlowSurfaceMuted else MaterialTheme.colorScheme.surface
    val stateText = if (selected) "当前" else "未选"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics {
                contentDescription = "${mode.editorLabel()}，$stateText，${mode.editorDescription()}"
            },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mode.editorLabel(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) TrainFlowPrimary else TrainFlowNeutral700
                    )
                }
                Text(
                    text = mode.editorDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StrengthExerciseEditorCard(
    modifier: Modifier = Modifier,
    exercise: StrengthPlanExerciseUiState,
    index: Int,
    totalCount: Int,
    canRemove: Boolean,
    onTargetWeightChanged: (String) -> Unit,
    onRepRangeChanged: (String, String) -> Unit,
    onFixedRepsChanged: (String) -> Unit,
    onWorkingSetsChanged: (String) -> Unit,
    onWarmupSetsChanged: (String) -> Unit,
    onRestAfterSetChanged: (String) -> Unit,
    onSetTargetsExpandedChanged: (Boolean) -> Unit,
    onSetTargetWeightChanged: (String, String) -> Unit,
    onSetFixedRepsChanged: (String, String) -> Unit,
    onSetRestAfterChanged: (String, String) -> Unit,
    onRemove: () -> Unit,
    isDragging: Boolean,
    dragOffsetY: Float,
    onDragStarted: (Int) -> Unit,
    onDragChanged: (Float) -> Unit,
    onDragStopped: () -> Unit,
    onDragCanceled: () -> Unit
) {
    var cardHeightPx by remember(exercise.id) { mutableStateOf(1) }
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${index + 1}. ${exercise.exerciseName}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = exercise.shortCue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canRemove) {
                TextButton(onClick = onRemove) {
                    Text(text = "移除")
                }
            }
            StrengthDragHandle(
                enabled = totalCount > 1,
                isDragging = isDragging,
                onDragStarted = { onDragStarted(cardHeightPx) },
                onDragChanged = onDragChanged,
                onDragStopped = onDragStopped,
                onDragCanceled = onDragCanceled
            )
        }

        StrengthSetTargetsPanel(
            exercise = exercise,
            onExpandedChanged = onSetTargetsExpandedChanged,
            onTargetWeightChanged = onTargetWeightChanged,
            onRepRangeChanged = onRepRangeChanged,
            onFixedRepsChanged = onFixedRepsChanged,
            onWorkingSetsChanged = onWorkingSetsChanged,
            onWarmupSetsChanged = onWarmupSetsChanged,
            onRestAfterSetChanged = onRestAfterSetChanged,
            onSetTargetWeightChanged = onSetTargetWeightChanged,
            onSetFixedRepsChanged = onSetFixedRepsChanged,
            onSetRestAfterChanged = onSetRestAfterChanged,
            substitutions = exercise.substitutions
        )
    }
}

@Composable
private fun StrengthDragHandle(
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
            .semantics { contentDescription = "力量目标组拖拽排序手柄" }
            .strengthDragHandleGestures(
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

private fun Modifier.strengthDragHandleGestures(
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
private fun StrengthSetTargetsPanel(
    exercise: StrengthPlanExerciseUiState,
    onExpandedChanged: (Boolean) -> Unit,
    onTargetWeightChanged: (String) -> Unit,
    onRepRangeChanged: (String, String) -> Unit,
    onFixedRepsChanged: (String) -> Unit,
    onWorkingSetsChanged: (String) -> Unit,
    onWarmupSetsChanged: (String) -> Unit,
    onRestAfterSetChanged: (String) -> Unit,
    onSetTargetWeightChanged: (String, String) -> Unit,
    onSetFixedRepsChanged: (String, String) -> Unit,
    onSetRestAfterChanged: (String, String) -> Unit,
    substitutions: List<String>
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = TrainFlowSurfaceMuted,
        border = BorderStroke(1.dp, TrainFlowNeutral100),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChanged(!exercise.expandedSetTargets) },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "目标组",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = exercise.targetSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TrainFlowNeutral700
                    )
                    Text(
                        text = exercise.setTargetsSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TrainFlowNeutral700
                    )
                }
                Text(
                    text = if (exercise.expandedSetTargets) "收起" else "展开",
                    style = MaterialTheme.typography.labelLarge,
                    color = TrainFlowPrimary
                )
            }

            if (exercise.expandedSetTargets) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DecimalField(
                        label = "计划重量 kg",
                        value = exercise.targetWeightText,
                        onValueChanged = onTargetWeightChanged,
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        label = "正式组数",
                        value = exercise.workingSetsText,
                        onValueChanged = onWorkingSetsChanged,
                        modifier = Modifier.weight(1f)
                    )
                }

                StrengthRepTargetFields(
                    repTarget = exercise.repTarget,
                    onRepRangeChanged = onRepRangeChanged,
                    onFixedRepsChanged = onFixedRepsChanged
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        label = "热身组数",
                        value = exercise.warmupSetsText,
                        onValueChanged = onWarmupSetsChanged,
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        label = "组间休息秒数",
                        value = exercise.restAfterSetText,
                        onValueChanged = onRestAfterSetChanged,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "逐组目标",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                exercise.setTargets.forEach { setTarget ->
                    StrengthSetTargetRow(
                        setTarget = setTarget,
                        onWeightChanged = { kg -> onSetTargetWeightChanged(setTarget.id, kg) },
                        onFixedRepsChanged = { reps -> onSetFixedRepsChanged(setTarget.id, reps) },
                        onRestAfterChanged = { seconds -> onSetRestAfterChanged(setTarget.id, seconds) }
                    )
                }

                if (substitutions.isNotEmpty()) {
                    Text(
                        text = "可替换动作：${substitutions.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TrainFlowNeutral700
                    )
                }
            }
        }
    }
}

@Composable
private fun StrengthRepTargetFields(
    repTarget: StrengthRepTargetUiState,
    onRepRangeChanged: (String, String) -> Unit,
    onFixedRepsChanged: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = repTarget.kind == StrengthRepTargetKind.RANGE,
            onClick = { onRepRangeChanged(repTarget.minRepsText, repTarget.maxRepsText) },
            label = { Text("次数区间") }
        )
        FilterChip(
            selected = repTarget.kind == StrengthRepTargetKind.FIXED,
            onClick = { onFixedRepsChanged(repTarget.fixedRepsText) },
            label = { Text("固定次数") }
        )
    }

    if (repTarget.kind == StrengthRepTargetKind.RANGE) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "最少次数",
                value = repTarget.minRepsText,
                onValueChanged = { onRepRangeChanged(it, repTarget.maxRepsText) },
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "最多次数",
                value = repTarget.maxRepsText,
                onValueChanged = { onRepRangeChanged(repTarget.minRepsText, it) },
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        NumberField(
            label = "固定次数",
            value = repTarget.fixedRepsText,
            onValueChanged = onFixedRepsChanged,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StrengthSetTargetRow(
    setTarget: StrengthSetTargetUiState,
    onWeightChanged: (String) -> Unit,
    onFixedRepsChanged: (String) -> Unit,
    onRestAfterChanged: (String) -> Unit
) {
    val kindLabel = when (setTarget.kind) {
        StrengthSetKind.WARMUP -> "热身"
        StrengthSetKind.WORKING -> "正式"
        StrengthSetKind.DROP -> "递减"
        StrengthSetKind.BACKOFF -> "退阶"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$kindLabel · ${setTarget.label}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DecimalField(
                label = "本组重量 kg",
                value = setTarget.targetWeightText,
                onValueChanged = onWeightChanged,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "本组次数",
                value = setTarget.repTarget.fixedRepsText,
                onValueChanged = onFixedRepsChanged,
                modifier = Modifier.weight(1f)
            )
        }
        NumberField(
            label = "本组休息秒数",
            value = setTarget.restAfterText,
            onValueChanged = onRestAfterChanged,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AddStrengthExerciseCard(
    uiState: StrengthPlanEditorScreenState,
    onAddExercise: (String) -> Unit,
    onAddCustomExercise: (String) -> Unit
) {
    var selectorOpen by rememberSaveable { mutableStateOf(false) }
    var customExerciseName by rememberSaveable { mutableStateOf("") }
    val selectedIds = uiState.exercises.map { it.exerciseId }.toSet()
    val remainingOptions = uiState.selectableExercises.filterNot { it.exerciseId in selectedIds }

    EditorCard {
        SectionTitle(text = "添加力量动作")
        Text(
            text = "从动作库选择，或为当前计划添加一个自定义力量动作。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = { selectorOpen = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("选择 / 自定义力量动作")
        }
    }

    if (selectorOpen) {
        AlertDialog(
            onDismissRequest = { selectorOpen = false },
            confirmButton = {
                TextButton(onClick = { selectorOpen = false }) {
                    Text("完成")
                }
            },
            title = { Text("添加力量动作") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (remainingOptions.isNotEmpty()) {
                        Text(
                            text = "动作库",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyColumn(
                            modifier = Modifier.sizeIn(maxHeight = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(remainingOptions, key = { _, option -> option.exerciseId }) { _, option ->
                                StrengthExerciseOptionRow(
                                    option = option,
                                    onClick = {
                                        onAddExercise(option.exerciseId)
                                        selectorOpen = false
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "首批可用于力量训练的动作都已加入。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "自定义动作",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = customExerciseName,
                        onValueChange = { customExerciseName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("动作名称") },
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            onAddCustomExercise(customExerciseName)
                            customExerciseName = ""
                            selectorOpen = false
                        },
                        enabled = customExerciseName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("添加自定义动作")
                    }
                }
            }
        )
    }
}

@Composable
private fun StrengthExerciseOptionRow(
    option: StrengthExerciseOptionUiState,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = TrainFlowSurfaceMuted,
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = option.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = option.defaultSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral700
            )
        }
    }
}

@Composable
private fun StrengthSaveAndPreviewCard(uiState: StrengthPlanEditorScreenState) {
    EditorCard {
        SectionTitle(text = "计划预览")
        Text(
            text = uiState.summary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        uiState.statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral700
            )
        }
        uiState.validationMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        uiState.savedPlan?.let { plan ->
            val blocks = plan.blocks.filterIsInstance<StrengthExerciseBlock>()
            Text(
                text = "已保存：力量训练 · ${blocks.size} 个动作 · ${blocks.sumOf { it.sets.size }} 个目标组",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = uiState.strengthSetTimerMode.savedPlanInstruction(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "保存和开始训练操作固定在底部操作区，预览卡仅用于确认动作摘要、校验状态和保存结果。",
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
        onValueChange = { input ->
            onValueChanged(input.sanitizeIntegerInput())
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true
    )
}

@Composable
private fun DecimalField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChanged(input.sanitizeDecimalInput())
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true
    )
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

private fun StrengthSetTimerMode.editorLabel(): String {
    return when (this) {
        StrengthSetTimerMode.MANUAL_START -> "手动开始下一组"
        StrengthSetTimerMode.AUTO_AFTER_REST -> "休息后自动开始下一组"
    }
}

private fun StrengthSetTimerMode.editorDescription(): String {
    return when (this) {
        StrengthSetTimerMode.MANUAL_START -> "休息结束后进入下一组准备态，等待你点按开始本组。"
        StrengthSetTimerMode.AUTO_AFTER_REST -> "休息自然结束后直接进入下一组计时，仍可在训练中暂停、提前开始或结束。"
    }
}

private fun StrengthSetTimerMode.savedPlanInstruction(): String {
    return when (this) {
        StrengthSetTimerMode.MANUAL_START ->
            "休息结束后等待手动开始下一组；计划重量和次数会预填到实际记录。"
        StrengthSetTimerMode.AUTO_AFTER_REST ->
            "休息自然结束后自动开始下一组；计划重量和次数会预填到实际记录。"
    }
}

internal fun String.sanitizeDecimalInput(): String {
    var decimalSeen = false
    return filter { character ->
        when {
            character.isDigit() -> true
            character == '.' && !decimalSeen -> {
                decimalSeen = true
                true
            }

            else -> false
        }
    }
}

internal fun Double?.formatWeightInput(): String {
    val value = this ?: return ""
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}

@Preview(showBackground = true)
@Composable
private fun StrengthPlanEditorRoutePreview() {
    TrainFlowTheme {
        StrengthPlanEditorRoute(onBackToHome = {})
    }
}
