package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthExerciseTarget
import com.liujyks.trainflow.core.model.StrengthSetCompletionInput
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionRecordMappersTest {
    @Test
    fun strengthPrepareTimeCountsTowardTotalButNotEffective() {
        val plan = strengthPlan()
        val state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan, sessionId = "strength-prepare"),
            WorkoutCommand.StartSession
        ).state

        val session = state.toWorkoutSessionRecord(
            plan = plan,
            startedAt = instant(0),
            endedAt = instant(12)
        )

        assertEquals(12, session.totalElapsedSec)
        assertEquals(0, session.effectiveElapsedSec)
        assertEquals(0, session.pausedElapsedSec)
    }

    @Test
    fun strengthConfirmTimeCountsTowardTotalButNotEffective() {
        val plan = strengthPlan()
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan, sessionId = "strength-confirm"),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 5).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state

        val session = state.toWorkoutSessionRecord(
            plan = plan,
            startedAt = instant(0),
            endedAt = instant(20)
        )

        assertEquals(20, session.totalElapsedSec)
        assertEquals(5, session.effectiveElapsedSec)
        assertEquals(0, session.pausedElapsedSec)
    }

    @Test
    fun strengthPauseIncreasesPausedWithoutPollutingEffective() {
        val plan = strengthPlan()
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan, sessionId = "strength-paused"),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 5).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession).state
        state = StrengthWorkoutEngine.tick(state, seconds = 10).state

        val session = state.toWorkoutSessionRecord(
            plan = plan,
            startedAt = instant(0),
            endedAt = instant(15)
        )

        assertEquals(15, session.totalElapsedSec)
        assertEquals(5, session.effectiveElapsedSec)
        assertEquals(10, session.pausedElapsedSec)
    }

    @Test
    fun terminalStrengthRecordsKeepTotalEffectivePausedSemantics() {
        val plan = strengthPlan()
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan, sessionId = "strength-completed"),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 6).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        state = StrengthWorkoutEngine.dispatch(
            state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        ).state

        val completed = state.toWorkoutSessionRecord(
            plan = plan,
            startedAt = instant(0),
            endedAt = instant(18)
        )

        assertEquals(SessionStatus.COMPLETED, completed.status)
        assertEquals(18, completed.totalElapsedSec)
        assertEquals(6, completed.effectiveElapsedSec)
        assertEquals(0, completed.pausedElapsedSec)

        val abandonedState = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.dispatch(
                StrengthWorkoutEngine.create(plan, sessionId = "strength-abandoned"),
                WorkoutCommand.StartSession
            ).state,
            WorkoutCommand.EndSession(reason = "user_requested")
        ).state
        val abandoned = abandonedState.toWorkoutSessionRecord(
            plan = plan,
            startedAt = instant(0),
            endedAt = instant(9)
        )

        assertEquals(SessionStatus.ABANDONED, abandoned.status)
        assertEquals(9, abandoned.totalElapsedSec)
        assertEquals(0, abandoned.effectiveElapsedSec)
        assertEquals(0, abandoned.pausedElapsedSec)
    }

    @Test
    fun timedTotalStillRespectsWallClockAndPausedLowerBound() {
        val plan = timedPlan()
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan, sessionId = "timed-paused"),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.tick(state, seconds = 5).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession).state
        state = TimedWorkoutEngine.tick(state, seconds = 10).state

        val session = state.toWorkoutSessionRecord(
            plan = plan,
            startedAt = instant(0),
            endedAt = instant(12)
        )

        assertEquals(15, session.totalElapsedSec)
        assertEquals(5, session.effectiveElapsedSec)
        assertEquals(10, session.pausedElapsedSec)
        assertTrue(requireNotNull(session.totalElapsedSec) >=
            requireNotNull(session.effectiveElapsedSec) + requireNotNull(session.pausedElapsedSec))
    }

    @Test
    fun completedTimedRecordPersistsRestExtensionWithoutPollutingPausedTimeOrPlan() {
        val plan = timedPlan()
        val originalBlocks = plan.blocks
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan, sessionId = "timed-rest-completed"),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 30).state
        state = TimedWorkoutEngine.tick(state, seconds = 1).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ExtendRest(seconds = 15)).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ExtendRest(seconds = 15)).state
        state = TimedWorkoutEngine.tick(state, seconds = 39).state

        val session = state.toWorkoutSessionRecord(
            plan = plan,
            startedAt = instant(0),
            endedAt = instant(70)
        )

        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(originalBlocks, plan.blocks)
        assertEquals(70, session.totalElapsedSec)
        assertEquals(70, session.effectiveElapsedSec)
        assertEquals(0, session.pausedElapsedSec)
        assertEquals(2, session.timedRestExtensionRecords.size)
        assertEquals(30, session.timedRestExtensionRecords.sumOf { record -> record.addedSec })
        assertEquals(listOf(15, 30), session.timedRestExtensionRecords.map { record ->
            record.cumulativeExtraRestSec
        })
        val first = session.timedRestExtensionRecords.first()
        assertEquals("interval-r1-work-rest", first.stepId)
        assertEquals(1, first.stepIndex)
        assertEquals(1, first.roundIndex)
        assertEquals("work", first.restStageId)
        assertEquals("work", first.previousStageId)
        assertEquals(10, first.plannedRestSec)
        assertEquals(1, first.restElapsedBeforeExtensionSec)
        assertEquals(9, first.extensionAtRemainingSec)
    }

    @Test
    fun abandonedTimedRecordKeepsAlreadyRecordedRestExtension() {
        val plan = timedPlan()
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan, sessionId = "timed-rest-abandoned"),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 30).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ExtendRest(seconds = 15)).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.EndSession(reason = "user_requested")).state

        val session = state.toWorkoutSessionRecord(
            plan = plan,
            startedAt = instant(0),
            endedAt = instant(30)
        )

        assertEquals(SessionStatus.ABANDONED, session.status)
        assertEquals(30, session.totalElapsedSec)
        assertEquals(30, session.effectiveElapsedSec)
        assertEquals(0, session.pausedElapsedSec)
        assertEquals(1, session.timedRestExtensionRecords.size)
        assertEquals(15, session.timedRestExtensionRecords.single().addedSec)
        assertEquals(10, session.timedRestExtensionRecords.single().extensionAtRemainingSec)
    }

    private fun strengthPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "plan-strength",
            mode = WorkoutMode.STRENGTH,
            title = "Strength",
            blocks = listOf(
                StrengthExerciseBlock(
                    id = "bench",
                    order = 1,
                    exerciseId = "barbell-bench-press",
                    target = StrengthExerciseTarget(
                        weight = WeightValue(60.0, WeightUnit.KG),
                        repTarget = RepTarget.Range(8, 12)
                    ),
                    sets = listOf(
                        StrengthSetPlan(
                            id = "bench-working-1",
                            order = 1,
                            kind = StrengthSetKind.WORKING,
                            restAfterSec = 0
                        )
                    )
                )
            ),
            createdAt = "2026-06-07T00:00:00Z",
            updatedAt = "2026-06-07T00:00:00Z"
        )
    }

    private fun timedPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "plan-timed",
            mode = WorkoutMode.TIMED,
            title = "Timed",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "interval",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "work",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 30,
                            restAfterSec = 10
                        )
                    )
                )
            ),
            createdAt = "2026-06-07T00:00:00Z",
            updatedAt = "2026-06-07T00:00:00Z"
        )
    }

    private fun instant(offsetSeconds: Long): Instant {
        return Instant.parse("2026-06-07T10:00:00Z").plusSeconds(offsetSeconds)
    }
}
