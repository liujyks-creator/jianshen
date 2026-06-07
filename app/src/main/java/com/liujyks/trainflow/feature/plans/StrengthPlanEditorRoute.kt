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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowAction
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

@Composable
internal fun StrengthPlanEditorRoute(
    onBackToHome: () -> Unit,
    onStartStrengthPlan: (WorkoutPlan) -> Unit = {},
    modifier: Modifier = Modifier,
    planEditorDefaults: PlanEditorDefaults = PlanEditorDefaults()
) {
    var uiState by remember {
        mutableStateOf(buildDefaultStrengthPlanEditorState(defaults = planEditorDefaults))
    }

    StrengthPlanEditorScreen(
        uiState = uiState,
        onBackToHome = onBackToHome,
        onTitleChanged = { uiState = uiState.updateTitle(it) },
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
        onAddExercise = { exerciseId -> uiState = uiState.addExercise(exerciseId) },
        onRemoveExercise = { itemId -> uiState = uiState.removeExercise(itemId) },
        onSaveDraft = { uiState = uiState.saveDraftPlan() },
        onStartStrengthPlan = {
            if (uiState.canStartTraining) {
                onStartStrengthPlan(
                    uiState.toWorkoutPlan(
                        planId = "plan-strength-editor-start",
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
    onTargetWeightChanged: (String, String) -> Unit,
    onRepRangeChanged: (String, String, String) -> Unit,
    onFixedRepsChanged: (String, String) -> Unit,
    onWorkingSetsChanged: (String, String) -> Unit,
    onWarmupSetsChanged: (String, String) -> Unit,
    onRestAfterSetChanged: (String, String) -> Unit,
    onSetTargetsExpandedChanged: (String, Boolean) -> Unit,
    onSetTargetWeightChanged: (String, String, String) -> Unit,
    onSetFixedRepsChanged: (String, String, String) -> Unit,
    onAddExercise: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onSaveDraft: () -> Unit,
    onStartStrengthPlan: () -> Unit,
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
            StrengthPlanEditorHeader(uiState)
        }

        item {
            StrengthPlanBasicsCard(
                uiState = uiState,
                onTitleChanged = onTitleChanged
            )
        }

        item {
            SectionTitle(text = "动作与计划组")
        }

        items(uiState.exercises, key = { it.id }) { exercise ->
            StrengthExerciseEditorCard(
                exercise = exercise,
                canRemove = uiState.exercises.size > 1,
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
                onRemove = { onRemoveExercise(exercise.id) }
            )
        }

        item {
            AddStrengthExerciseCard(
                uiState = uiState,
                onAddExercise = onAddExercise
            )
        }

        item {
            StrengthSaveAndPreviewCard(
                uiState = uiState,
                onSaveDraft = onSaveDraft,
                onStartStrengthPlan = onStartStrengthPlan
            )
        }
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
            text = "可直接开始当前草稿，也可保存后进入计划详情；真实计划保存后续接入。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun StrengthPlanBasicsCard(
    uiState: StrengthPlanEditorScreenState,
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
    }
}

@Composable
private fun StrengthExerciseEditorCard(
    exercise: StrengthPlanExerciseUiState,
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
    onRemove: () -> Unit
) {
    EditorCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = exercise.shortCue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = exercise.targetSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TrainFlowNeutral700
                )
            }
            if (canRemove) {
                TextButton(onClick = onRemove) {
                    Text(text = "移除")
                }
            }
        }

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

        ToggleRow(
            title = "展开逐组目标",
            checked = exercise.expandedSetTargets,
            onCheckedChange = onSetTargetsExpandedChanged
        )

        if (exercise.expandedSetTargets) {
            exercise.setTargets.forEach { setTarget ->
                StrengthSetTargetRow(
                    setTarget = setTarget,
                    onWeightChanged = { kg -> onSetTargetWeightChanged(setTarget.id, kg) },
                    onFixedRepsChanged = { reps -> onSetFixedRepsChanged(setTarget.id, reps) }
                )
            }
        }

        if (exercise.substitutions.isNotEmpty()) {
            Text(
                text = "替代动作候选已进入草稿映射：${exercise.substitutions.joinToString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral700
            )
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
    onFixedRepsChanged: (String) -> Unit
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
    }
}

@Composable
private fun AddStrengthExerciseCard(
    uiState: StrengthPlanEditorScreenState,
    onAddExercise: (String) -> Unit
) {
    val selectedIds = uiState.exercises.map { it.exerciseId }.toSet()
    val remainingOptions = uiState.selectableExercises.filterNot { it.exerciseId in selectedIds }

    EditorCard {
        SectionTitle(text = "添加力量动作")
        if (remainingOptions.isEmpty()) {
            Text(
                text = "首批可用于力量训练的动作都已加入。后续动作库扩展会继续复用同一契约。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                remainingOptions.forEach { option ->
                    FilterChip(
                        selected = false,
                        onClick = { onAddExercise(option.exerciseId) },
                        label = {
                            Text("${option.exerciseName} · ${option.defaultSummary}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StrengthSaveAndPreviewCard(
    uiState: StrengthPlanEditorScreenState,
    onSaveDraft: () -> Unit,
    onStartStrengthPlan: () -> Unit
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
                text = "WorkoutPlan: ${plan.mode.contractValue} · ${blocks.size} 个 strength block · ${blocks.sumOf { it.sets.size }} 个 planned set",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "默认本组计时：${blocks.firstOrNull()?.setTimerMode?.contractValue ?: "manual_start"}；只保存本次编辑草稿预览。",
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
                onClick = onStartStrengthPlan,
                enabled = uiState.canStartTraining,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "开始力量训练")
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
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
