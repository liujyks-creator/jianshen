package com.liujyks.trainflow.feature.exerciselibrary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.core.domain.exerciselibrary.ExerciseLibraryFilters
import com.liujyks.trainflow.core.domain.exerciselibrary.ExerciseTrainingModeFilter
import com.liujyks.trainflow.core.model.EquipmentKind
import com.liujyks.trainflow.core.model.ExerciseDifficulty
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

@Composable
fun ExerciseLibraryRoute(modifier: Modifier = Modifier) {
    var filters by remember { mutableStateOf(ExerciseLibraryFilters()) }
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }
    val uiState = remember(filters) { buildExerciseLibraryUiState(filters) }

    selectedExerciseId?.let { exerciseId ->
        ExerciseDetailScreen(
            uiState = remember(exerciseId) { findExerciseDetailUiState(exerciseId) },
            onBack = { selectedExerciseId = null },
            modifier = modifier
        )
    } ?: ExerciseLibraryScreen(
        uiState = uiState,
        onTrainingModeSelected = { selected ->
            filters = filters.copy(trainingMode = selected)
        },
        onMuscleSelected = { selected ->
            filters = filters.copy(muscleId = selected.takeUnless { it == filters.muscleId })
        },
        onEquipmentSelected = { selected ->
            filters = filters.copy(equipment = selected.takeUnless { it == filters.equipment })
        },
        onDifficultySelected = { selected ->
            filters = filters.copy(difficulty = selected.takeUnless { it == filters.difficulty })
        },
        onClearFilters = {
            filters = ExerciseLibraryFilters()
        },
        onExerciseSelected = { exerciseId ->
            selectedExerciseId = exerciseId
        },
        modifier = modifier
    )
}

@Composable
private fun ExerciseLibraryScreen(
    uiState: ExerciseLibraryScreenState,
    onTrainingModeSelected: (ExerciseTrainingModeFilter) -> Unit,
    onMuscleSelected: (String) -> Unit,
    onEquipmentSelected: (EquipmentKind) -> Unit,
    onDifficultySelected: (ExerciseDifficulty) -> Unit,
    onClearFilters: () -> Unit,
    onExerciseSelected: (String) -> Unit,
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
            ExerciseLibraryHeader(uiState = uiState)
        }

        item {
            ExerciseFilterPanel(
                uiState = uiState,
                onTrainingModeSelected = onTrainingModeSelected,
                onMuscleSelected = onMuscleSelected,
                onEquipmentSelected = onEquipmentSelected,
                onDifficultySelected = onDifficultySelected,
                onClearFilters = onClearFilters
            )
        }

        if (uiState.isEmpty) {
            item {
                EmptyExerciseLibraryState()
            }
        } else {
            items(uiState.items, key = { it.id }) { item ->
                ExerciseSummaryCard(
                    item = item,
                    onClick = { onExerciseSelected(item.id) }
                )
            }
        }
    }
}

@Composable
private fun ExerciseLibraryHeader(uiState: ExerciseLibraryScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "动作库",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "首批 ${uiState.totalCount} 个动作，用于计划创建和训练提示；当前为只读浏览。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "当前显示 ${uiState.visibleCount} 个动作",
            style = MaterialTheme.typography.labelLarge,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun ExerciseFilterPanel(
    uiState: ExerciseLibraryScreenState,
    onTrainingModeSelected: (ExerciseTrainingModeFilter) -> Unit,
    onMuscleSelected: (String) -> Unit,
    onEquipmentSelected: (EquipmentKind) -> Unit,
    onDifficultySelected: (ExerciseDifficulty) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "筛选",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (uiState.hasActiveFilters) {
                    TextButton(onClick = onClearFilters) {
                        Text(text = "清除")
                    }
                }
            }

            FilterRow(title = "训练类型") {
                uiState.trainingModeOptions.forEach { option ->
                    FilterChip(
                        selected = option.selected,
                        onClick = { onTrainingModeSelected(option.value) },
                        label = { Text(option.label) }
                    )
                }
            }

            FilterRow(title = "身体部位") {
                uiState.muscleOptions.forEach { option ->
                    FilterChip(
                        selected = option.selected,
                        onClick = { onMuscleSelected(option.value) },
                        label = { Text(option.label) }
                    )
                }
            }

            FilterRow(title = "器械") {
                uiState.equipmentOptions.forEach { option ->
                    FilterChip(
                        selected = option.selected,
                        onClick = { onEquipmentSelected(option.value) },
                        label = { Text(option.label) }
                    )
                }
            }

            FilterRow(title = "难度") {
                uiState.difficultyOptions.forEach { option ->
                    FilterChip(
                        selected = option.selected,
                        onClick = { onDifficultySelected(option.value) },
                        label = { Text(option.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ExerciseSummaryCard(
    item: ExerciseLibraryItemUiState,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = TrainFlowNeutral700
                )
                Text(
                    text = "${item.categoryLabel} · ${item.difficultyLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = item.shortCue,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "部位 ${item.muscleLabels.joinToString("、")} · 器械 ${item.equipmentLabels.joinToString("、")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            item.defaultSummary?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrainFlowNeutral700
                )
            }

            ChipRow(labels = item.capabilityLabels)
        }
    }
}

@Composable
private fun ExerciseDetailScreen(
    uiState: ExerciseDetailUiState?,
    onBack: () -> Unit,
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
            TextButton(onClick = onBack) {
                Text(text = "返回动作库")
            }
        }

        if (uiState == null) {
            item {
                MissingExerciseDetailState()
            }
        } else {
            item {
                ExerciseDetailHeader(uiState)
            }
            item {
                DetailSection(title = "设置与执行", items = uiState.steps, numbered = true)
            }
            item {
                DetailSection(title = "发力要点", items = uiState.keyPoints)
            }
            item {
                DetailSection(title = "常见错误", items = uiState.commonMistakes)
            }
            item {
                DetailSection(title = "呼吸提示", items = uiState.breathingCues)
            }
            item {
                DetailSection(title = "安全说明", items = uiState.cautions)
            }
            if (uiState.substitutionLabels.isNotEmpty()) {
                item {
                    DetailSection(title = "替代动作", items = uiState.substitutionLabels)
                }
            }
            if (uiState.recoveryAreaLabels.isNotEmpty()) {
                item {
                    DetailSection(title = "恢复映射", items = uiState.recoveryAreaLabels)
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailHeader(uiState: ExerciseDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = uiState.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = uiState.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = TrainFlowNeutral700
                )
                uiState.aliasSummary?.let { aliases ->
                    Text(
                        text = "别名 $aliases",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = uiState.shortCue,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            uiState.defaultSummary?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrainFlowNeutral700
                )
            }

            DetailMetaLine(title = "分类", values = listOf(uiState.categoryLabel, uiState.difficultyLabel))
            DetailMetaLine(title = "主要部位", values = uiState.primaryMuscleLabels)
            if (uiState.secondaryMuscleLabels.isNotEmpty()) {
                DetailMetaLine(title = "辅助部位", values = uiState.secondaryMuscleLabels)
            }
            DetailMetaLine(title = "器械", values = uiState.equipmentLabels)

            ChipRow(labels = uiState.capabilityLabels)
        }
    }
}

@Composable
private fun DetailMetaLine(
    title: String,
    values: List<String>
) {
    Text(
        text = "$title ${values.joinToString("、")}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DetailSection(
    title: String,
    items: List<String>,
    numbered: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            items.forEachIndexed { index, text ->
                Text(
                    text = if (numbered) "${index + 1}. $text" else "• $text",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChipRow(labels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = TrainFlowPrimary
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = TrainFlowNeutral50
                )
            }
        }
    }
}

@Composable
private fun MissingExerciseDetailState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "找不到动作详情",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "该动作不在当前动作库中。返回后重新选择。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyExerciseLibraryState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "没有匹配动作",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "清除部分筛选后再浏览首批动作。当前列表不会自动扩展到课程或内容流。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseLibraryRoutePreview() {
    TrainFlowTheme {
        ExerciseLibraryRoute()
    }
}
