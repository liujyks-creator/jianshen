package com.liujyks.trainflow.feature.exerciselibrary

import com.liujyks.trainflow.core.domain.exerciselibrary.ExerciseLibraryFilters
import com.liujyks.trainflow.core.domain.exerciselibrary.ExerciseTrainingModeFilter
import com.liujyks.trainflow.core.model.EquipmentKind
import com.liujyks.trainflow.core.model.ExerciseDifficulty

data class ExerciseLibraryScreenState(
    val totalCount: Int,
    val visibleCount: Int,
    val filters: ExerciseLibraryFilters,
    val trainingModeOptions: List<ExerciseFilterOption<ExerciseTrainingModeFilter>>,
    val muscleOptions: List<ExerciseFilterOption<String>>,
    val equipmentOptions: List<ExerciseFilterOption<EquipmentKind>>,
    val difficultyOptions: List<ExerciseFilterOption<ExerciseDifficulty>>,
    val items: List<ExerciseLibraryItemUiState>
) {
    val hasActiveFilters: Boolean = filters.hasActiveFilters
    val isEmpty: Boolean = items.isEmpty()
}

data class ExerciseFilterOption<T>(
    val value: T,
    val label: String,
    val selected: Boolean
)

data class ExerciseLibraryItemUiState(
    val id: String,
    val name: String,
    val categoryLabel: String,
    val difficultyLabel: String,
    val muscleLabels: List<String>,
    val equipmentLabels: List<String>,
    val capabilityLabels: List<String>,
    val shortCue: String,
    val defaultSummary: String?
)

data class ExerciseDetailUiState(
    val id: String,
    val name: String,
    val aliasSummary: String?,
    val categoryLabel: String,
    val difficultyLabel: String,
    val primaryMuscleLabels: List<String>,
    val secondaryMuscleLabels: List<String>,
    val equipmentLabels: List<String>,
    val capabilityLabels: List<String>,
    val shortCue: String,
    val defaultSummary: String?,
    val steps: List<String>,
    val keyPoints: List<String>,
    val commonMistakes: List<String>,
    val breathingCues: List<String>,
    val cautions: List<String>,
    val substitutionLabels: List<String>,
    val recoveryAreaLabels: List<String>
)
