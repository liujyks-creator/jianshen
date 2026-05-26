package com.liujyks.trainflow.core.model

sealed interface WorkoutCommand {
    data object StartSession : WorkoutCommand
    data object PauseSession : WorkoutCommand
    data object ResumeSession : WorkoutCommand
    data object SkipStep : WorkoutCommand

    data class ExtendRest(
        val seconds: Int
    ) : WorkoutCommand

    data class StartStrengthSet(
        val setPlanId: String? = null
    ) : WorkoutCommand

    data class CompleteStrengthSet(
        val draft: StrengthSetCompletionDraft? = null
    ) : WorkoutCommand

    data class ConfirmStrengthSet(
        val record: StrengthSetCompletionInput
    ) : WorkoutCommand

    data class ReplaceExercise(
        val fromExerciseId: String,
        val toExerciseId: String
    ) : WorkoutCommand

    data class UpdateActualWeight(
        val setRecordId: String,
        val weight: WeightValue
    ) : WorkoutCommand

    data class UpdateActualReps(
        val setRecordId: String,
        val reps: Int
    ) : WorkoutCommand

    data class EndSession(
        val reason: String? = null
    ) : WorkoutCommand
}

data class StrengthSetCompletionDraft(
    val activeDurationSec: Int? = null
)

data class StrengthSetCompletionInput(
    val actualWeight: WeightValue? = null,
    val actualReps: Int? = null,
    val effort: SetEffort? = null,
    val notes: String? = null
)

data class CommandEnvelope(
    val command: WorkoutCommand,
    val source: CommandSource,
    val issuedAt: String
)

enum class CommandSource(val contractValue: String) {
    UI("ui"),
    VOICE("voice"),
    SYSTEM("system"),
    WEARABLE("wearable")
}
