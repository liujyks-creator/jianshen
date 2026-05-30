package com.liujyks.trainflow.core.engine

import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.SetEffort
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthExerciseTarget
import com.liujyks.trainflow.core.model.StrengthSetCompletionInput
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutEvent
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthWorkoutEngineTest {
    @Test
    fun validStrengthPlanAdvancesPrepareActiveConfirmRestAndCompletes() {
        var result = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(twoSetPlan(restAfterFirstSetSec = 3)),
            command = WorkoutCommand.StartSession
        )

        assertEquals(SessionStatus.ACTIVE, result.state.status)
        assertEquals(SessionStepKind.STRENGTH_PREPARE_SET, result.state.currentSessionStep?.kind)
        assertEquals("bench-working-1", result.state.currentSet?.setPlanId)
        assertTrue(result.events[0] is WorkoutEvent.SessionStarted)
        assertTrue(result.events[1] is WorkoutEvent.StrengthSetReady)

        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.StartStrengthSet())
        assertEquals(SessionStepKind.STRENGTH_ACTIVE_SET, result.state.currentSessionStep?.kind)
        assertTrue(result.events.single() is WorkoutEvent.StrengthSetStarted)

        result = StrengthWorkoutEngine.tick(result.state, seconds = 5)
        assertEquals(5, result.state.activeSetElapsedSec)

        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.CompleteStrengthSet())
        assertEquals(SessionStepKind.STRENGTH_CONFIRM_SET, result.state.currentSessionStep?.kind)
        assertEquals(5, result.state.pendingDraft?.activeDurationSec)
        assertTrue(result.state.strengthSetRecords.isEmpty())
        assertEquals(
            StrengthSessionStepHistoryStatus.COMPLETED,
            result.state.stepHistory.single { record ->
                record.kind == SessionStepKind.STRENGTH_ACTIVE_SET &&
                    record.setPlanId == "bench-working-1"
            }.status
        )
        assertEquals(
            StrengthSessionStepHistoryStatus.STARTED,
            result.state.stepHistory.single { record ->
                record.kind == SessionStepKind.STRENGTH_CONFIRM_SET &&
                    record.setPlanId == "bench-working-1"
            }.status
        )
        assertTrue(result.events.single() is WorkoutEvent.StrengthSetCompleted)

        result = StrengthWorkoutEngine.dispatch(
            result.state,
            WorkoutCommand.ConfirmStrengthSet(
                StrengthSetCompletionInput(effort = SetEffort.GOOD)
            )
        )
        assertEquals(1, result.state.strengthSetRecords.size)
        assertEquals(SessionStepKind.STRENGTH_REST, result.state.currentSessionStep?.kind)
        assertEquals(3, result.state.restRemainingSec)
        assertTrue(result.events.first() is WorkoutEvent.RestStarted)

        result = StrengthWorkoutEngine.tick(result.state, seconds = 3)
        assertEquals(SessionStepKind.STRENGTH_PREPARE_SET, result.state.currentSessionStep?.kind)
        assertEquals("bench-working-2", result.state.currentSet?.setPlanId)
        assertEquals(3, result.state.strengthSetRecords.single().actualRestAfterSec)
        assertTrue(result.events.single() is WorkoutEvent.StrengthSetReady)

        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.StartStrengthSet())
        result = StrengthWorkoutEngine.tick(result.state, seconds = 4)
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.CompleteStrengthSet())
        result = StrengthWorkoutEngine.dispatch(
            result.state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        )

        assertEquals(SessionStatus.COMPLETED, result.state.status)
        assertEquals(2, result.state.completedSetCount)
        assertEquals(2, result.state.strengthSetRecords.size)
        assertTrue(result.events.single() is WorkoutEvent.SessionCompleted)
    }

    @Test
    fun completionDraftBackfillsSetLevelTargetsBeforeActionLevelTargets() {
        var result = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(singleSetPlan()),
            command = WorkoutCommand.StartSession
        )
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.StartStrengthSet())
        result = StrengthWorkoutEngine.tick(result.state, seconds = 7)
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.CompleteStrengthSet())

        assertEquals(weight(62.5), result.state.pendingDraft?.defaultActualWeight)
        assertEquals(5, result.state.pendingDraft?.defaultActualReps)

        result = StrengthWorkoutEngine.dispatch(
            result.state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        )

        val record = result.state.strengthSetRecords.single()
        assertEquals(weight(62.5), record.actualWeight)
        assertEquals(5, record.actualReps)
        assertEquals(7, record.activeDurationSec)
        assertEquals(weight(62.5), record.plannedWeight)
        assertTrue(record.plannedRepTarget is RepTarget.Fixed)
    }

    @Test
    fun rangeRepDraftUsesMinimumRepsAsStableDefault() {
        var result = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(
                singleSetPlan(
                    setRepTarget = null,
                    actionRepTarget = RepTarget.Range(minReps = 8, maxReps = 12)
                )
            ),
            command = WorkoutCommand.StartSession
        )
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.StartStrengthSet())
        result = StrengthWorkoutEngine.tick(result.state, seconds = 2)
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.CompleteStrengthSet())

        assertEquals(8, result.state.pendingDraft?.defaultActualReps)
    }

    @Test
    fun pauseFreezesActiveSetTimerAndRestRemainingTime() {
        var result = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(twoSetPlan(restAfterFirstSetSec = 5)),
            command = WorkoutCommand.StartSession
        )
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.StartStrengthSet())
        result = StrengthWorkoutEngine.tick(result.state, seconds = 2)
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.PauseSession)

        assertEquals(SessionStatus.PAUSED, result.state.status)
        assertEquals(2, result.state.activeSetElapsedSec)
        assertTrue(result.events.single() is WorkoutEvent.SessionPaused)

        result = StrengthWorkoutEngine.tick(result.state, seconds = 10)
        assertEquals(2, result.state.activeSetElapsedSec)
        assertEquals(SessionStatus.PAUSED, result.state.status)

        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.ResumeSession)
        result = StrengthWorkoutEngine.tick(result.state, seconds = 3)
        assertEquals(5, result.state.activeSetElapsedSec)

        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.CompleteStrengthSet())
        result = StrengthWorkoutEngine.dispatch(
            result.state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        )
        result = StrengthWorkoutEngine.tick(result.state, seconds = 2)
        assertEquals(3, result.state.restRemainingSec)

        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.PauseSession)
        result = StrengthWorkoutEngine.tick(result.state, seconds = 10)
        assertEquals(3, result.state.restRemainingSec)

        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.ResumeSession)
        result = StrengthWorkoutEngine.tick(result.state, seconds = 3)
        assertEquals(SessionStepKind.STRENGTH_PREPARE_SET, result.state.currentSessionStep?.kind)
    }

    @Test
    fun restEndingEventsAreSeparateFromSetCompletion() {
        var result = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(
                twoSetPlan(
                    restAfterFirstSetSec = 3,
                    preferences = PlanPreferences(
                        cueSettings = CueSettings(restEnding = CountdownCue(thresholdSec = 2))
                    )
                )
            ),
            command = WorkoutCommand.StartSession
        )
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.StartStrengthSet())
        result = StrengthWorkoutEngine.tick(result.state, seconds = 1)
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.CompleteStrengthSet())
        result = StrengthWorkoutEngine.dispatch(
            result.state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        )

        assertFalse(result.events.any { event -> event is WorkoutEvent.RestEnding })

        result = StrengthWorkoutEngine.tick(result.state)
        assertEquals(listOf(2), result.events.restEndingRemainingSeconds())

        result = StrengthWorkoutEngine.tick(result.state)
        assertEquals(listOf(1), result.events.restEndingRemainingSeconds())
    }

    @Test
    fun earlyStartDuringRestRecordsActualRestAndStartsNextSet() {
        var result = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(twoSetPlan(restAfterFirstSetSec = 10)),
            command = WorkoutCommand.StartSession
        )
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.StartStrengthSet())
        result = StrengthWorkoutEngine.tick(result.state, seconds = 2)
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.CompleteStrengthSet())
        result = StrengthWorkoutEngine.dispatch(
            result.state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        )
        result = StrengthWorkoutEngine.tick(result.state, seconds = 4)

        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.StartStrengthSet())

        assertEquals(SessionStepKind.STRENGTH_ACTIVE_SET, result.state.currentSessionStep?.kind)
        assertEquals("bench-working-2", result.state.currentSet?.setPlanId)
        assertEquals(4, result.state.strengthSetRecords.single().actualRestAfterSec)
        assertTrue(result.events.single() is WorkoutEvent.StrengthSetStarted)
    }

    @Test
    fun illegalCommandsAreIgnoredWithoutHistoryPollution() {
        var result = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(twoSetPlan(restAfterFirstSetSec = 5)),
            command = WorkoutCommand.StartSession
        )
        val prepareState = result.state

        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.CompleteStrengthSet())
        result = StrengthWorkoutEngine.dispatch(
            result.state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        )
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.StartStrengthSet("wrong-set"))

        assertEquals(prepareState, result.state)
        assertTrue(result.events.isEmpty())
        assertEquals(1, result.state.stepHistory.size)
        assertEquals(
            listOf(StrengthWorkoutControlHistoryType.START_SESSION),
            result.state.controlHistory.map { event -> event.type }
        )
    }

    @Test
    fun endSessionMarksAbandonedWithoutCompletedEventAndTerminalIgnoresLateCommands() {
        var result = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(twoSetPlan(restAfterFirstSetSec = 5)),
            command = WorkoutCommand.StartSession
        )
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.StartStrengthSet())
        result = StrengthWorkoutEngine.tick(result.state, seconds = 3)
        result = StrengthWorkoutEngine.dispatch(result.state, WorkoutCommand.EndSession(reason = "user_exit"))

        assertEquals(SessionStatus.ABANDONED, result.state.status)
        assertTrue(result.state.isTerminal)
        assertEquals("user_exit", result.state.earlyEnd?.reason)
        assertEquals(3, result.state.earlyEnd?.currentStepActualDurationSec)
        assertTrue(result.state.eventsAreNotCompleted(result.events))

        val terminalState = result.state
        listOf(
            WorkoutCommand.PauseSession,
            WorkoutCommand.ResumeSession,
            WorkoutCommand.StartStrengthSet(),
            WorkoutCommand.CompleteStrengthSet(),
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput()),
            WorkoutCommand.EndSession(reason = "late")
        ).forEach { command ->
            result = StrengthWorkoutEngine.dispatch(result.state, command)
        }

        assertEquals(terminalState, result.state)
        assertEquals(1, result.state.controlHistory.count { event ->
            event.type == StrengthWorkoutControlHistoryType.END_SESSION
        })
    }

    @Test
    fun emptyStrengthSnapshotCompletesOnStart() {
        val result = StrengthWorkoutEngine.dispatch(
            state = StrengthWorkoutEngine.create(
                WorkoutPlan(
                    id = "empty-strength",
                    mode = WorkoutMode.STRENGTH,
                    title = "Empty",
                    blocks = emptyList(),
                    createdAt = "2026-05-30T00:00:00Z",
                    updatedAt = "2026-05-30T00:00:00Z"
                )
            ),
            command = WorkoutCommand.StartSession
        )

        assertEquals(SessionStatus.COMPLETED, result.state.status)
        assertTrue(result.events.single() is WorkoutEvent.SessionCompleted)
        assertNull(result.state.currentSessionStep)
    }

    private fun singleSetPlan(
        setRepTarget: RepTarget? = RepTarget.Fixed(reps = 5),
        actionRepTarget: RepTarget? = RepTarget.Range(minReps = 8, maxReps = 12)
    ): WorkoutPlan {
        return plan(
            blocks = listOf(
                block(
                    target = StrengthExerciseTarget(
                        weight = weight(60.0),
                        repTarget = actionRepTarget,
                        restAfterSetSec = 90
                    ),
                    sets = listOf(
                        set(
                            id = "bench-working-1",
                            order = 1,
                            targetWeight = weight(62.5),
                            repTarget = setRepTarget
                        )
                    )
                )
            )
        )
    }

    private fun twoSetPlan(
        restAfterFirstSetSec: Int,
        preferences: PlanPreferences? = null
    ): WorkoutPlan {
        return plan(
            preferences = preferences,
            blocks = listOf(
                block(
                    target = StrengthExerciseTarget(
                        weight = weight(60.0),
                        repTarget = RepTarget.Range(minReps = 8, maxReps = 12)
                    ),
                    sets = listOf(
                        set(id = "bench-working-1", order = 1, restAfterSec = restAfterFirstSetSec),
                        set(id = "bench-working-2", order = 2, restAfterSec = 0)
                    )
                )
            )
        )
    }

    private fun plan(
        blocks: List<StrengthExerciseBlock>,
        preferences: PlanPreferences? = null
    ): WorkoutPlan {
        return WorkoutPlan(
            id = "plan-strength",
            mode = WorkoutMode.STRENGTH,
            title = "Strength plan",
            blocks = blocks,
            preferences = preferences,
            createdAt = "2026-05-30T00:00:00Z",
            updatedAt = "2026-05-30T00:00:00Z"
        )
    }

    private fun block(
        target: StrengthExerciseTarget,
        sets: List<StrengthSetPlan>
    ): StrengthExerciseBlock {
        return StrengthExerciseBlock(
            id = "bench",
            order = 1,
            exerciseId = "barbell-bench-press",
            target = target,
            sets = sets
        )
    }

    private fun set(
        id: String,
        order: Int,
        targetWeight: WeightValue? = null,
        repTarget: RepTarget? = null,
        restAfterSec: Int? = null
    ): StrengthSetPlan {
        return StrengthSetPlan(
            id = id,
            order = order,
            kind = StrengthSetKind.WORKING,
            targetWeight = targetWeight,
            repTarget = repTarget,
            restAfterSec = restAfterSec
        )
    }

    private fun weight(value: Double): WeightValue {
        return WeightValue(value = value, unit = WeightUnit.KG)
    }

    private fun List<WorkoutEvent>.restEndingRemainingSeconds(): List<Int> {
        return filterIsInstance<WorkoutEvent.RestEnding>().map { event -> event.remainingSec }
    }

    private fun StrengthWorkoutEngineState.eventsAreNotCompleted(events: List<WorkoutEvent>): Boolean {
        return events.none { event -> event is WorkoutEvent.SessionCompleted }
    }
}
