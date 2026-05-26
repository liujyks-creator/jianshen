package com.liujyks.trainflow.core.model

data class ExerciseCapabilities(
    val supportsTimedTraining: Boolean,
    val supportsReps: Boolean,
    val supportsWeight: Boolean,
    val supportsFollowAlong: Boolean,
    val supportsWarmupRole: Boolean,
    val supportsStretchRole: Boolean,
    val supportsCircuitRole: Boolean,
    val isUnilateral: Boolean
)

data class ExerciseInstructionContent(
    val shortCue: String,
    val steps: List<String>,
    val keyPoints: List<String>,
    val commonMistakes: List<String>,
    val breathingCues: List<String> = emptyList(),
    val cautions: List<String> = emptyList()
)

data class ExerciseRecoveryMapping(
    val trainedMuscleIds: List<String>,
    val recommendedRecoveryAreaIds: List<String>,
    val recoveryContentIds: List<String> = emptyList()
)

data class ExerciseSubstitution(
    val exerciseId: String,
    val reasonTags: List<String> = emptyList(),
    val equipmentFallback: Boolean = false
)

data class ContentSourceMeta(
    val author: String? = null,
    val reviewer: String? = null,
    val sourceRefs: List<String> = emptyList(),
    val updatedAt: String? = null
)

enum class ContentStatus(val contractValue: String) {
    DRAFT("draft"),
    REVIEWED("reviewed"),
    PUBLISHED("published")
}

data class Exercise(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val category: String,
    val primaryMuscleIds: List<String>,
    val secondaryMuscleIds: List<String> = emptyList(),
    val equipment: List<EquipmentKind>,
    val difficulty: ExerciseDifficulty,
    val roles: List<ExerciseRole>,
    val capabilities: ExerciseCapabilities,
    val instructions: ExerciseInstructionContent,
    val media: List<MediaAssetRef> = emptyList(),
    val recovery: ExerciseRecoveryMapping? = null,
    val substitutions: List<ExerciseSubstitution> = emptyList(),
    val contentStatus: ContentStatus,
    val sourceMeta: ContentSourceMeta? = null,
    val extensions: Map<String, Any?> = emptyMap()
)
