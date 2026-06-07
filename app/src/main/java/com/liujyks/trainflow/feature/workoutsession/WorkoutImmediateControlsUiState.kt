package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.model.WorkoutCommand

internal enum class WorkoutImmediateControlPlacement {
    RHYTHM_SURFACE,
    FIXED_BOTTOM
}

internal enum class WorkoutImmediateControlRole {
    START_STRENGTH_SET,
    COMPLETE_STRENGTH_SET,
    CONFIRM_STRENGTH_SET,
    START_NEXT_STRENGTH_SET,
    PAUSE_SESSION,
    RESUME_SESSION,
    SKIP_STEP,
    END_SESSION
}

internal data class WorkoutImmediateControlUiState(
    val role: WorkoutImmediateControlRole,
    val label: String,
    val enabled: Boolean,
    val placement: WorkoutImmediateControlPlacement
)

internal data class WorkoutEndConfirmationUiState(
    val visible: Boolean = false
) {
    fun request(canEnd: Boolean): WorkoutEndConfirmationUiState {
        return copy(visible = canEnd)
    }

    fun cancel(): WorkoutEndConfirmationUiState {
        return copy(visible = false)
    }

    fun confirm(canEnd: Boolean): WorkoutEndConfirmationResult {
        return WorkoutEndConfirmationResult(
            nextState = copy(visible = false),
            command = WorkoutCommand.EndSession(reason = "user_requested").takeIf { visible && canEnd }
        )
    }
}

internal data class WorkoutEndConfirmationResult(
    val nextState: WorkoutEndConfirmationUiState,
    val command: WorkoutCommand.EndSession?
)
