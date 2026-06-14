package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutEvent
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import com.liujyks.trainflow.feature.plans.buildDefaultTimedPlanEditorState
import java.io.File
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedReadyStartGateTest {
    @Test
    fun enteringTimedRouteStartsInReadyGateWithoutDispatchingStartSession() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val state = TimedWorkoutEngine.create(plan)
        val readyGate = requireNotNull(state.toTimedReadyStartGateUiState())

        assertEquals(SessionStatus.READY, state.status)
        assertEquals(null, state.currentStep)
        assertEquals(0, state.activeElapsedSec)
        assertTrue(state.controlHistory.isEmpty())
        assertEquals(plan.title, readyGate.planTitle)
        assertFalse(state.toTimedWorkoutSessionScreenState().canPause)
        assertFalse(state.toTimedWorkoutSessionScreenState().canEnd)
    }

    @Test
    fun readyGateDoesNotAdvanceActiveElapsedOrRouteTickLoop() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val ready = TimedWorkoutEngine.create(plan)
        val afterTick = TimedWorkoutEngine.tick(ready, seconds = 30).state

        assertFalse(ready.shouldTickTimedRouteClock())
        assertEquals(SessionStatus.READY, afterTick.status)
        assertEquals(0, afterTick.activeElapsedSec)
        assertEquals(0, afterTick.pausedElapsedSec)
        assertEquals(null, afterTick.currentStep)
    }

    @Test
    fun clickingReadyGateCircleDispatchesStartSessionAndStartsExistingExecutionState() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val ready = TimedWorkoutEngine.create(plan)
        val startedResult = ready.startTimedSessionFromReadyGate()
        val started = startedResult.state

        assertEquals(SessionStatus.ACTIVE, started.status)
        assertNotNull(started.currentStep)
        assertTrue(started.controlHistory.any { history -> history.type.name == "START_SESSION" })
        assertTrue(startedResult.events.any { event -> event is WorkoutEvent.SessionStarted })
        assertEquals(null, started.toTimedReadyStartGateUiState())
        assertTrue(started.toTimedWorkoutSessionScreenState().canPause)
    }

    @Test
    fun readyGateDoesNotDispatchCountdownReminderFeedback() {
        val ready = TimedWorkoutEngine.create(buildDefaultPlanManagementState().plans.first())
        val syntheticReadyReminder = com.liujyks.trainflow.core.engine.TimedWorkoutEngineResult(
            state = ready,
            events = listOf(WorkoutEvent.TimedWorkEnding(stepId = "not-started", remainingSec = 3))
        )

        assertFalse(ready.toTimedWorkoutSessionScreenState().countdownReminder.isActive)
        assertFalse(syntheticReadyReminder.shouldDispatchTimedCountdownReminderFeedback())
    }

    @Test
    fun readyGateLeavingRouteDoesNotWriteAbandonedSessionRecord() {
        val ready = TimedWorkoutEngine.create(buildDefaultPlanManagementState().plans.first())
        val endedBeforeStart = TimedWorkoutEngine.dispatch(
            ready,
            com.liujyks.trainflow.core.model.WorkoutCommand.EndSession(reason = "route_disposed")
        ).state

        assertEquals(SessionStatus.ABANDONED, endedBeforeStart.status)
        assertFalse(endedBeforeStart.shouldRecordTimedTerminalSession(startedAt = null))
    }

    @Test
    fun readyGateIgnoresRestExtensionWithoutCreatingRestExtensionRecord() {
        val ready = TimedWorkoutEngine.create(buildDefaultPlanManagementState().plans.first())
        val result = TimedWorkoutEngine.dispatch(
            ready,
            com.liujyks.trainflow.core.model.WorkoutCommand.ExtendRest(seconds = 15)
        )

        assertEquals(SessionStatus.READY, result.state.status)
        assertEquals(0, result.state.extendedRestSec)
        assertTrue(result.state.restExtensionHistory.isEmpty())
        assertTrue(result.state.controlHistory.isEmpty())
    }

    @Test
    fun terminalSessionRecordsStillWriteAfterRealStart() {
        val startedAt = Instant.parse("2026-06-14T09:00:00Z")
        var started = TimedWorkoutEngine.create(buildDefaultPlanManagementState().plans.first())
            .startTimedSessionFromReadyGate()
            .state
        val abandoned = TimedWorkoutEngine.dispatch(
            started,
            com.liujyks.trainflow.core.model.WorkoutCommand.EndSession(reason = "user_requested")
        ).state
        started = TimedWorkoutEngine.create(buildDefaultPlanManagementState().plans.first())
            .startTimedSessionFromReadyGate()
            .state
        val completed = TimedWorkoutEngine.tick(
            started,
            seconds = started.steps.sumOf { step -> step.durationSec }
        ).state

        assertTrue(abandoned.shouldRecordTimedTerminalSession(startedAt))
        assertTrue(completed.shouldRecordTimedTerminalSession(startedAt))
    }

    @Test
    fun editorAndPlanDetailEntriesShareTheSameReadyGate() {
        val savedPlan = buildDefaultPlanManagementState().plans.first()
        val editorPlan = buildDefaultTimedPlanEditorState()
            .toWorkoutPlan(planId = "plan-timed-editor-start")

        val savedReady = TimedWorkoutEngine.create(savedPlan).toTimedReadyStartGateUiState()
        val editorReady = TimedWorkoutEngine.create(editorPlan).toTimedReadyStartGateUiState()

        assertNotNull(savedReady)
        assertNotNull(editorReady)
        assertEquals(savedPlan.title, requireNotNull(savedReady).planTitle)
        assertEquals(editorPlan.title, requireNotNull(editorReady).planTitle)
    }

    @Test
    fun readyGateClickTargetIsTheCenterCircleNotOnlyThePlayGlyph() {
        val source = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)
        val buttonStart = source.indexOf("private fun ReadyStartCenterButton")
        val glyphStart = source.indexOf("private fun ReadyStartPlayGlyph")
        val buttonSource = source.substring(buttonStart, glyphStart)
        val glyphSource = source.substring(glyphStart)

        assertTrue(buttonSource.contains(".clickable(onClick = onClick)"))
        assertTrue(buttonSource.contains("contentDescription = \"开始计时训练\""))
        assertFalse(glyphSource.contains(".clickable"))
    }
}
