package com.liujyks.trainflow.core.model

enum class WorkoutMode(val contractValue: String) {
    TIMED("timed"),
    STRENGTH("strength"),
    FOLLOW_ALONG("follow_along")
}

enum class PlanBlockKind(val contractValue: String) {
    WARMUP("warmup"),
    TIMED_CIRCUIT("timed_circuit"),
    STRENGTH_EXERCISE("strength_exercise"),
    REST("rest"),
    STRETCH("stretch"),
    COOLDOWN("cooldown")
}

enum class ExerciseDifficulty(val contractValue: String) {
    BEGINNER("beginner"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced")
}

enum class ExerciseRole(val contractValue: String) {
    WARMUP("warmup"),
    MAIN("main"),
    STRETCH("stretch"),
    RECOVERY("recovery")
}

enum class ExerciseSide(val contractValue: String) {
    BOTH("both"),
    LEFT("left"),
    RIGHT("right"),
    ALTERNATING("alternating")
}

enum class EquipmentKind(val contractValue: String) {
    BODYWEIGHT("bodyweight"),
    DUMBBELL("dumbbell"),
    BARBELL("barbell"),
    MACHINE("machine"),
    CABLE("cable"),
    BAND("band"),
    KETTLEBELL("kettlebell"),
    MAT("mat"),
    OTHER("other")
}

enum class MediaKind(val contractValue: String) {
    IMAGE("image"),
    VIDEO("video"),
    ANIMATION("animation"),
    AUDIO("audio")
}

enum class MediaRole(val contractValue: String) {
    THUMBNAIL("thumbnail"),
    DEMO("demo"),
    INSTRUCTION("instruction"),
    COACH("coach"),
    RECOVERY("recovery")
}

data class MediaAssetRef(
    val id: String,
    val kind: MediaKind,
    val uri: String,
    val altText: String? = null,
    val posterUri: String? = null,
    val durationMs: Long? = null,
    val locale: String? = null,
    val role: MediaRole? = null
)

data class WeightValue(
    val value: Double,
    val unit: WeightUnit
)

enum class WeightUnit(val contractValue: String) {
    KG("kg"),
    LB("lb")
}

sealed interface RepTarget {
    val kind: RepTargetKind

    data class Fixed(
        val reps: Int
    ) : RepTarget {
        override val kind: RepTargetKind = RepTargetKind.FIXED
    }

    data class Range(
        val minReps: Int = DEFAULT_MIN_REPS,
        val maxReps: Int = DEFAULT_MAX_REPS
    ) : RepTarget {
        override val kind: RepTargetKind = RepTargetKind.RANGE

        companion object {
            const val DEFAULT_MIN_REPS = 8
            const val DEFAULT_MAX_REPS = 12
        }
    }
}

enum class RepTargetKind(val contractValue: String) {
    FIXED("fixed"),
    RANGE("range")
}
