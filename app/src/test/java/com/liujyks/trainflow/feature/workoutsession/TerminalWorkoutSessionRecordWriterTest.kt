package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.WorkoutSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TerminalWorkoutSessionRecordWriterTest {
    @Test
    fun sameTerminalStateOnlyRecordsOnce() = runBlocking {
        val session = terminalSession("session-once")
        var inserts = 0
        var state = TerminalWorkoutSessionRecordWriteState()

        state = state.recordTerminalSessionOnce(session) {
            inserts += 1
        }
        state = state.recordTerminalSessionOnce(session) {
            inserts += 1
        }

        assertEquals(1, inserts)
        assertEquals("session-once", state.attemptedSessionId)
        assertNull(state.lastFailureMessage)
    }

    @Test
    fun insertFailureIsSwallowedAndGuarded() = runBlocking {
        val session = terminalSession("session-failure")
        var attempts = 0
        var state = TerminalWorkoutSessionRecordWriteState()

        state = state.recordTerminalSessionOnce(session) {
            attempts += 1
            error("database unavailable")
        }
        state = state.recordTerminalSessionOnce(session) {
            attempts += 1
        }

        assertEquals(1, attempts)
        assertEquals("session-failure", state.attemptedSessionId)
        assertNotNull(state.lastFailureMessage)
    }

    @Test
    fun firstTerminalStateStillRecordsCompletedAndAbandoned() = runBlocking {
        val completed = terminalSession("session-completed", SessionStatus.COMPLETED)
        val abandoned = terminalSession("session-abandoned", SessionStatus.ABANDONED)
        val inserted = mutableListOf<String>()

        var completedState = TerminalWorkoutSessionRecordWriteState()
        completedState = completedState.recordTerminalSessionOnce(completed) { session ->
            inserted += "${session.id}:${session.status.contractValue}"
        }
        var abandonedState = TerminalWorkoutSessionRecordWriteState()
        abandonedState = abandonedState.recordTerminalSessionOnce(abandoned) { session ->
            inserted += "${session.id}:${session.status.contractValue}"
        }

        assertEquals(
            listOf("session-completed:completed", "session-abandoned:abandoned"),
            inserted
        )
        assertNull(completedState.lastFailureMessage)
        assertNull(abandonedState.lastFailureMessage)
    }

    private fun terminalSession(
        id: String,
        status: SessionStatus = SessionStatus.COMPLETED
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            mode = WorkoutMode.TIMED,
            planSnapshot = WorkoutPlanSnapshot(
                planId = "plan",
                title = "Plan",
                mode = WorkoutMode.TIMED,
                blocks = emptyList()
            ),
            status = status,
            startedAt = "2026-06-07T10:00:00Z",
            endedAt = "2026-06-07T10:01:00Z"
        )
    }
}
