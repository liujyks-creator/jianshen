package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.model.WorkoutSession
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal data class TerminalWorkoutSessionRecordWriteState(
    val attemptedSessionId: String? = null,
    val lastFailureMessage: String? = null
)

internal suspend fun TerminalWorkoutSessionRecordWriteState.recordTerminalSessionOnce(
    session: WorkoutSession,
    onRecordWorkoutSession: suspend (WorkoutSession) -> Unit
): TerminalWorkoutSessionRecordWriteState {
    if (attemptedSessionId == session.id) return this

    val attemptedState = copy(attemptedSessionId = session.id)
    return try {
        withContext(NonCancellable) {
            onRecordWorkoutSession(session)
        }
        attemptedState.copy(lastFailureMessage = null)
    } catch (exception: Exception) {
        attemptedState.copy(
            lastFailureMessage = exception.message ?: exception::class.java.simpleName
        )
    }
}
