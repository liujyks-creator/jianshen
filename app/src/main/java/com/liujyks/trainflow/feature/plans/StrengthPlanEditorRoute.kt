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
import androidx.compose.runtime.LaunchedEffect
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
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowAction
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

@Composable
fun StrengthPlanEditorRoute(
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf(buildDefaultStrengthPlanEditorState()) }

    StrengthPlanEditorScreen(
        uiState = uiState,
        onBackToHome = onBackToHome,
        onTitleChanged = { uiState = uiState.updateTitle(it) },
        onTargetWeightChanged = { itemId, kg -> uiState = uiState.updateTargetWeight(itemId, kg) },
        onRepRangeChanged = { itemId, minReps, maxReps ->
            uiState = uiState.updateRepRange(itemId, minReps, maxReps)
        },
        onFixedRepsChanged = { itemId, reps -> uiState = uiState.updateFixedReps(itemId, reps) },
        onWorkingSetsChanged = { itemId, sets -> uiState = uiState.updateWorkingSets(itemId, sets) },
        onWarmupSetsChanged = { itemId, sets -> uiState = uiState.updateWarmupSets(itemId, sets) },
        onRestAfterSetChanged = { itemId, seconds -> uiState = uiState.updateRestAfterSet(itemId, seconds) },
        onSetTargetsExpandedChanged = { itemId, expanded ->
            uiState = uiState.setSetTargetsExpanded(itemId, expanded)
        },
        onSetTargetWeightChanged = { itemId, setId, kg ->
            uiState = uiState.updateSetTargetWeight(itemId, setId, kg)
        },
        onSetFixedRepsChanged = { itemId, setId, reps ->
            uiState = uiState.updateSetFixedReps(itemId, setId, reps)
        },
        onAddExercise = { exerciseId -> uiState = uiState.addExercise(exerciseId) },
        onRemoveExercise = { itemId -> uiState = uiState.removeExercise(itemId) },
        onSaveDraft = { uiState = uiState.saveDraftPlan() },
        modifier = modifier
    )
}

@Composable
private fun StrengthPlanEditorScreen(
    uiState: StrengthPlanEditorScreenState,
    onBackToHome: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onTargetWeightChanged: (String, Double?) -> Unit,
    onRepRangeChanged: (String, Int, Int) -> Unit,
    onFixedRepsChanged: (String, Int) -> Unit,
    onWorkingSetsChanged: (String, Int) -> Unit,
    onWarmupSetsChanged: (String, Int) -> Unit,
    onRestAfterSetChanged: (String, Int) -> Unit,
    onSetTargetsExpandedChanged: (String, Boolean) -> Unit,
    onSetTargetWeightChanged: (String, String, Double?) -> Unit,
    onSetFixedRepsChanged: (String, String, Int) -> Unit,
    onAddExercise: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onSaveDraft: () -> Unit,
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
                onSaveDraft = onSaveDraft
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
            text = "当前生成内存态草稿；计划详情已可启动力量训练，真实保存和 session records 后续接入。",
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
    onTargetWeightChanged: (Double?) -> Unit,
    onRepRangeChanged: (Int, Int) -> Unit,
    onFixedRepsChanged: (Int) -> Unit,
    onWorkingSetsChanged: (Int) -> Unit,
    onWarmupSetsChanged: (Int) -> Unit,
    onRestAfterSetChanged: (Int) -> Unit,
    onSetTargetsExpandedChanged: (Boolean) -> Unit,
    onSetTargetWeightChanged: (String, Double?) -> Unit,
    onSetFixedRepsChanged: (String, Int) -> Unit,
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
                value = exercise.targetWeightKg,
                onValueChanged = onTargetWeightChanged,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "正式组数",
                value = exercise.workingSets,
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
                value = exercise.warmupSets,
                onValueChanged = onWarmupSetsChanged,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "组间休息秒数",
                value = exercise.restAfterSetSec,
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
    onRepRangeChanged: (Int, Int) -> Unit,
    onFixedRepsChanged: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = repTarget.kind == StrengthRepTargetKind.RANGE,
            onClick = { onRepRangeChanged(repTarget.minReps, repTarget.maxReps) },
            label = { Text("次数区间") }
        )
        FilterChip(
            selected = repTarget.kind == StrengthRepTargetKind.FIXED,
            onClick = { onFixedRepsChanged(repTarget.fixedReps) },
            label = { Text("固定次数") }
        )
    }

    if (repTarget.kind == StrengthRepTargetKind.RANGE) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "最少次数",
                value = repTarget.minReps,
                onValueChanged = { onRepRangeChanged(it, repTarget.maxReps) },
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "最多次数",
                value = repTarget.maxReps,
                onValueChanged = { onRepRangeChanged(repTarget.minReps, it) },
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        NumberField(
            label = "固定次数",
            value = repTarget.fixedReps,
            onValueChanged = onFixedRepsChanged,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StrengthSetTargetRow(
    setTarget: StrengthSetTargetUiState,
    onWeightChanged: (Double?) -> Unit,
    onFixedRepsChanged: (Int) -> Unit
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
                value = setTarget.targetWeightKg,
                onValueChanged = onWeightChanged,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "本组次数",
                value = when (val target = setTarget.repTarget.toRepTarget()) {
                    is com.liujyks.trainflow.core.model.RepTarget.Fixed -> target.reps
                    is com.liujyks.trainflow.core.model.RepTarget.Range -> target.maxReps
                },
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
    onSaveDraft: () -> Unit
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
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "开始力量训练（E4 接入）")
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { input ->
            input.filter { it.isDigit() }
                .toIntOrNull()
                ?.let(onValueChanged)
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true
    )
}

@Composable
private fun DecimalField(
    label: String,
    value: Double?,
    onValueChanged: (Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember { mutableStateOf(value.formatWeightInput()) }
    var lastUserParsedValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (value != lastUserParsedValue) {
            textValue = value.formatWeightInput()
            lastUserParsedValue = value
        }
    }

    OutlinedTextField(
        value = textValue,
        onValueChange = { input ->
            val cleaned = input.sanitizeDecimalInput()
            val parsedValue = cleaned.toDoubleOrNull()
            textValue = cleaned
            lastUserParsedValue = parsedValue
            onValueChanged(parsedValue)
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
