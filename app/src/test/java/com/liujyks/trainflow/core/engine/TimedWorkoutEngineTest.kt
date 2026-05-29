package com.liujyks.trainflow.core.engine

import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutEvent
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedWorkoutEngineTest {
    @Test
    fun validTimedSnapshotAdvancesThroughActionsRestsRoundsAndCompletes() {
        val snapshot = plan(
            blocks = listOf(
                circuit(
                    rounds = 2,
                    restBetweenRoundsSec = 3,
                    items = listOf(
                        item(id = "jump", exerciseId = "jumping-jacks", workSec = 4, restSec = 2),
                        item(id = "squat", exerciseId = "bodyweight-squat", workSec = 3)
                    )
                )
            )
        ).toSnapshot()

        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(snapshot, sessionId = "session-timed"),
            command = WorkoutCommand.StartSession
        )

        assertEquals(SessionStatus.ACTIVE, result.state.status)
        assertEquals("circuit-r1-jump-work", result.state.currentStep?.id)
        assertEquals(4, result.state.remainingSec)
        assertTrue(result.events[0] is WorkoutEvent.SessionStarted)
        assertTrue(result.events[1] is WorkoutEvent.TimedWorkStarted)
        assertEquals(7, result.state.steps.size)

        result = TimedWorkoutEngine.tick(result.state, seconds = 4)
        assertEquals("circuit-r1-jump-rest", result.state.currentStep?.id)
        assertEquals(2, result.state.remainingSec)
        assertTrue(result.events.first() is WorkoutEvent.RestStarted)

        result = TimedWorkoutEngine.tick(result.state, seconds = 5)
        assertEquals("circuit-r1-round-rest", result.state.currentStep?.id)
        assertEquals(3, result.state.remainingSec)

        result = TimedWorkoutEngine.tick(result.state, seconds = 12)
        assertEquals(SessionStatus.COMPLETED, result.state.status)
        assertEquals(7, result.state.completedStepCount)
        assertEquals(0, result.state.remainingSec)
        assertTrue(result.events.last() is WorkoutEvent.SessionCompleted)
    }

    @Test
    fun pauseFreezesRemainingTimeAndResumeContinuesOriginalStep() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(singleActionPlan(workSec = 5)),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.tick(result.state, seconds = 2)
        assertEquals(3, result.state.remainingSec)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.PauseSession)
        assertEquals(SessionStatus.PAUSED, result.state.status)
        assertTrue(result.events.single() is WorkoutEvent.SessionPaused)

        result = TimedWorkoutEngine.tick(result.state, seconds = 10)
        assertEquals(SessionStatus.PAUSED, result.state.status)
        assertEquals(3, result.state.remainingSec)
        assertTrue(result.events.isEmpty())

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ResumeSession)
        assertEquals(SessionStatus.ACTIVE, result.state.status)
        assertTrue(result.events.single() is WorkoutEvent.SessionResumed)

        result = TimedWorkoutEngine.tick(result.state, seconds = 3)
        assertEquals(SessionStatus.COMPLETED, result.state.status)
    }

    @Test
    fun endingCueEventsFireOncePerStepAndRemainingSecond() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(
                singleActionPlan(
                    workSec = 3,
                    restSec = 2,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(thresholdSec = 2),
                        restEnding = CountdownCue(thresholdSec = 1)
                    )
                )
            ),
            command = WorkoutCommand.StartSession
        )

        assertFalse(result.events.any { event -> event is WorkoutEvent.TimedWorkEnding })

        result = TimedWorkoutEngine.tick(result.state)
        assertEquals(listOf(2), result.events.workEndingRemainingSeconds())

        val noTick = TimedWorkoutEngine.tick(result.state, seconds = 0)
        assertTrue(noTick.events.isEmpty())
        assertEquals(2, noTick.state.remainingSec)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.PauseSession)
        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ResumeSession)
        assertFalse(result.events.any { event -> event is WorkoutEvent.TimedWorkEnding })

        result = TimedWorkoutEngine.tick(result.state)
        assertEquals(listOf(1), result.events.workEndingRemainingSeconds())

        result = TimedWorkoutEngine.tick(result.state)
        assertTrue(result.events.first() is WorkoutEvent.RestStarted)

        result = TimedWorkoutEngine.tick(result.state)
        assertEquals(listOf(1), result.events.restEndingRemainingSeconds())
    }

    @Test
    fun itemCueOverridesGlobalCueAndTooLargeThresholdsAreIgnored() {
        val globalCueTooLarge = CueSettings(
            actionEnding = CountdownCue(thresholdSec = 5),
            restEnding = CountdownCue(thresholdSec = 5)
        )
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(
                singleActionPlan(
                    workSec = 3,
                    restSec = 2,
                    cueSettings = globalCueTooLarge,
                    itemCueSettings = CueSettings(
                        actionEnding = CountdownCue(thresholdSec = 1),
                        restEnding = CountdownCue(thresholdSec = 1)
                    )
                )
            ),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.tick(result.state, seconds = 2)
        assertEquals(listOf(1), result.events.workEndingRemainingSeconds())

        result = TimedWorkoutEngine.tick(result.state)
        assertTrue(result.events.first() is WorkoutEvent.RestStarted)

        result = TimedWorkoutEngine.tick(result.state)
        assertEquals(listOf(1), result.events.restEndingRemainingSeconds())

        result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(
                singleActionPlan(
                    workSec = 3,
                    cueSettings = globalCueTooLarge
                )
            ),
            command = WorkoutCommand.StartSession
        )
        result = TimedWorkoutEngine.tick(result.state, seconds = 3)
        assertTrue(result.events.workEndingRemainingSeconds().isEmpty())
        assertEquals(SessionStatus.COMPLETED, result.state.status)
    }

    @Test
    fun skipStepMovesToNextExecutableStepAndLastSkipCompletes() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(
                plan(
                    blocks = listOf(
                        circuit(
                            items = listOf(
                                item(id = "first", exerciseId = "jumping-jacks", workSec = 10),
                                item(id = "second", exerciseId = "bodyweight-squat", workSec = 10)
                            )
                        )
                    )
                )
            ),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.SkipStep)
        assertEquals("circuit-r1-second-work", result.state.currentStep?.id)
        assertEquals(listOf("circuit-r1-first-work"), result.state.skippedStepIds)
        assertTrue(result.events.single() is WorkoutEvent.TimedWorkStarted)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.SkipStep)
        assertEquals(SessionStatus.COMPLETED, result.state.status)
        assertTrue(result.events.single() is WorkoutEvent.SessionCompleted)
    }

    @Test
    fun extendRestOnlyChangesCurrentRestStep() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(singleActionPlan(workSec = 2, restSec = 2)),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ExtendRest(seconds = 15))
        assertEquals(2, result.state.remainingSec)
        assertEquals(0, result.state.extendedRestSec)

        result = TimedWorkoutEngine.tick(result.state, seconds = 2)
        assertEquals(SessionStepKind.TIMED_REST, result.state.currentSessionStep?.kind)
        assertEquals(2, result.state.remainingSec)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ExtendRest(seconds = 15))
        assertEquals(17, result.state.remainingSec)
        assertEquals(15, result.state.extendedRestSec)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ExtendRest(seconds = -5))
        assertEquals(17, result.state.remainingSec)
        assertEquals(15, result.state.extendedRestSec)
    }

    @Test
    fun endSessionMarksAbandonedWithoutCompletedEvent() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(singleActionPlan(workSec = 5)),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.EndSession(reason = "user_exit"))

        assertEquals(SessionStatus.ABANDONED, result.state.status)
        assertTrue(result.state.isTerminal)
        assertFalse(result.events.any { event -> event is WorkoutEvent.SessionCompleted })
    }

    private fun singleActionPlan(
        workSec: Int,
        restSec: Int? = null,
        cueSettings: CueSettings? = null,
        itemCueSettings: CueSettings? = null
    ): WorkoutPlan {
        return plan(
            preferences = cueSettings?.let { PlanPreferences(cueSettings = it) },
            blocks = listOf(
                circuit(
                    items = listOf(
                        item(
                            id = "jump",
                            exerciseId = "jumping-jacks",
                            workSec = workSec,
                            restSec = restSec,
                            cueSettings = itemCueSettings
                        )
                    )
                )
            )
        )
    }

    private fun plan(
        blocks: List<TimedCircuitBlock>,
        preferences: PlanPreferences? = null
    ): WorkoutPlan {
        return WorkoutPlan(
            id = "plan-timed",
            mode = WorkoutMode.TIMED,
            title = "Timed plan",
            blocks = blocks,
            preferences = preferences,
            createdAt = "2026-05-30T00:00:00Z",
            updatedAt = "2026-05-30T00:00:00Z"
        )
    }

    private fun circuit(
        rounds: Int = 1,
        restBetweenRoundsSec: Int? = null,
        items: List<TimedExerciseItem>
    ): TimedCircuitBlock {
        return TimedCircuitBlock(
            id = "circuit",
            order = 1,
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec,
            items = items
        )
    }

    private fun item(
        id: String,
        exerciseId: String,
        workSec: Int,
        restSec: Int? = null,
        cueSettings: CueSettings? = null
    ): TimedExerciseItem {
        return TimedExerciseItem(
            id = id,
            exerciseId = exerciseId,
            workDurationSec = workSec,
            restAfterSec = restSec,
            cueSettings = cueSettings
        )
    }

    private fun WorkoutPlan.toSnapshot(): WorkoutPlanSnapshot {
        return WorkoutPlanSnapshot(
            title = title,
            mode = mode,
            blocks = blocks,
            preferences = preferences,
            followAlong = followAlong
        )
    }

    private fun List<WorkoutEvent>.workEndingRemainingSeconds(): List<Int> {
        return filterIsInstance<WorkoutEvent.TimedWorkEnding>().map { event -> event.remainingSec }
    }

    private fun List<WorkoutEvent>.restEndingRemainingSeconds(): List<Int> {
        return filterIsInstance<WorkoutEvent.RestEnding>().map { event -> event.remainingSec }
    }
}
