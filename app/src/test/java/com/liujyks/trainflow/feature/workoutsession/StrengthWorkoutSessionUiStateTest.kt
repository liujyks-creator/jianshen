package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngineState
import com.liujyks.trainflow.core.model.HeartRateAvailability
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.RepTarget
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
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthWorkoutSessionUiStateTest {
    @Test
    fun prepareStateMapsCurrentSetTargetAndStartControl() {
        val plan = buildDefaultPlanManagementState().plans[1]
        val started = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val uiState = started.toStrengthWorkoutSessionScreenState()

        assertEquals(plan.title, uiState.planTitle)
        assertEquals("准备本组", uiState.phaseLabel)
        assertEquals("本组目标", uiState.primaryMetricLabel)
        assertTrue(uiState.primaryMetricText.contains("kg"))
        assertTrue(uiState.primaryMetricText.contains("次"))
        assertTrue(uiState.setProgressLabel.contains("第 1"))
        assertTrue(uiState.targetSummary.contains("次"))
        assertTrue(uiState.canStartSet)
        assertFalse(uiState.canCompleteSet)
        assertFalse(uiState.canConfirmPlanned)
        assertTrue(uiState.canPause)
    }

    @Test
    fun activeStateMapsElapsedTimerAndCompleteControl() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 5).state
        val uiState = state.toStrengthWorkoutSessionScreenState()

        assertEquals(SessionStepKind.STRENGTH_ACTIVE_SET, state.currentSessionStep?.kind)
        assertEquals("本组进行中", uiState.phaseLabel)
        assertEquals("本组耗时", uiState.primaryMetricLabel)
        assertEquals("00:05", uiState.primaryMetricText)
        assertTrue(uiState.canCompleteSet)
        assertFalse(uiState.canStartSet)
        assertTrue(uiState.shortCue.isNotBlank())
    }

    @Test
    fun confirmStateMapsEditableConfirmationDefaultsAndRangeShortcuts() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 7).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        val uiState = state.toStrengthWorkoutSessionScreenState()

        assertEquals("确认记录", uiState.phaseLabel)
        assertEquals("完成本组", uiState.primaryMetricLabel)
        assertEquals("00:07", uiState.primaryMetricText)
        assertTrue(uiState.canConfirmPlanned)
        assertNotNull(uiState.confirmSummary)
        assertTrue(requireNotNull(uiState.confirmSummary).contains("按计划确认"))
        assertTrue(uiState.shortCue.contains("确认"))

        val confirmation = requireNotNull(uiState.confirmation)
        assertEquals(uiState.currentExerciseName, confirmation.exerciseName)
        assertEquals("00:07", confirmation.activeDurationLabel)
        assertTrue(confirmation.plannedWeightLabel.contains("kg"))
        assertEquals("8-12 次", confirmation.plannedRepLabel)
        assertEquals("8", confirmation.actualRepsInput)
        assertEquals((8..12).toList(), confirmation.repQuickOptions)
        assertEquals(SetEffort.GOOD, confirmation.selectedEffort)
        assertEquals(
            listOf("轻松", "刚好", "很吃力", "动作变形"),
            confirmation.effortOptions.map { option -> option.label }
        )
        assertTrue(confirmation.canConfirm)
    }

    @Test
    fun confirmationInputBuildsCommandWithEditedActualValuesAndEffort() {
        val confirmation = completedFirstSet(strengthPlan()).toStrengthWorkoutSessionScreenState()
            .confirmation
            .let(::requireNotNull)

        val validation = confirmation.initialInputState()
            .copy(
                actualWeightInput = "61.5",
                actualRepsInput = "10",
                selectedEffort = SetEffort.HARD
            )
            .validateFor(confirmation)

        assertTrue(validation.canConfirm)
        assertNull(validation.errorText)
        assertEquals(
            StrengthSetCompletionInput(
                actualWeight = WeightValue(value = 61.5, unit = WeightUnit.KG),
                actualReps = 10,
                effort = SetEffort.HARD
            ),
            validation.commandInput
        )
    }

    @Test
    fun confirmationUsesSetLevelTargetsBeforeActionDefaults() {
        val confirmation = completedFirstSet(
            strengthPlan(
                actionWeight = WeightValue(value = 60.0, unit = WeightUnit.KG),
                actionRepTarget = RepTarget.Range(minReps = 8, maxReps = 12),
                setWeight = WeightValue(value = 62.5, unit = WeightUnit.KG),
                setRepTarget = RepTarget.Fixed(reps = 5)
            )
        ).toStrengthWorkoutSessionScreenState().confirmation.let(::requireNotNull)

        assertEquals("62.5 kg", confirmation.plannedWeightLabel)
        assertEquals("5 次", confirmation.plannedRepLabel)
        assertEquals("62.5", confirmation.actualWeightInput)
        assertEquals("5", confirmation.actualRepsInput)
        assertTrue(confirmation.repQuickOptions.isEmpty())
    }

    @Test
    fun confirmationAllowsNoPlannedWeightWhenRepsAreValid() {
        val confirmation = completedFirstSet(
            strengthPlan(
                actionWeight = null,
                setWeight = null,
                actionRepTarget = RepTarget.Fixed(reps = 10),
                setRepTarget = null
            )
        ).toStrengthWorkoutSessionScreenState().confirmation.let(::requireNotNull)

        assertEquals("未设重量", confirmation.plannedWeightLabel)
        assertEquals("", confirmation.actualWeightInput)
        assertNull(confirmation.weightUnit)

        val validation = confirmation.initialInputState().validateFor(confirmation)

        assertTrue(validation.canConfirm)
        assertNull(validation.commandInput?.actualWeight)
        assertEquals(10, validation.commandInput?.actualReps)
    }

    @Test
    fun confirmationRejectsNegativeWeightAndInvalidReps() {
        val confirmation = completedFirstSet(strengthPlan()).toStrengthWorkoutSessionScreenState()
            .confirmation
            .let(::requireNotNull)

        val negativeWeight = confirmation.initialInputState()
            .copy(actualWeightInput = "-1")
            .validateFor(confirmation)
        val invalidReps = confirmation.initialInputState()
            .copy(actualRepsInput = "0")
            .validateFor(confirmation)

        assertFalse(negativeWeight.canConfirm)
        assertEquals("重量不能为负数", negativeWeight.errorText)
        assertNull(negativeWeight.commandInput)
        assertFalse(invalidReps.canConfirm)
        assertEquals("次数至少为 1", invalidReps.errorText)
        assertNull(invalidReps.commandInput)
    }

    @Test
    fun restStateMapsCountdownAndNextSetTarget() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 3).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        state = StrengthWorkoutEngine.dispatch(
            state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        ).state
        val uiState = state.toStrengthWorkoutSessionScreenState()

        assertEquals("休息", uiState.phaseLabel)
        assertEquals("休息倒计时", uiState.primaryMetricLabel)
        assertTrue(uiState.primaryMetricText.startsWith("0"))
        assertTrue(uiState.nextSetLabel.startsWith("下一组"))
        assertTrue(uiState.nextSetLabel.contains("kg"))
        assertTrue(uiState.canStartNextDuringRest)
        assertFalse(uiState.canStartSet)
    }

    @Test
    fun completedAndAbandonedStatesMapLightweightTerminalCopy() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        repeat(state.setSteps.size) {
            state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
            state = StrengthWorkoutEngine.tick(state, seconds = 1).state
            state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
            state = StrengthWorkoutEngine.dispatch(
                state,
                WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
            ).state
            if (state.currentSessionStep?.kind == SessionStepKind.STRENGTH_REST) {
                state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
            }
        }
        val completedUiState = state.toStrengthWorkoutSessionScreenState()

        assertTrue(completedUiState.isTerminal)
        assertEquals("力量训练完成", completedUiState.terminalTitle)
        assertTrue(requireNotNull(completedUiState.terminalSummary).contains("已确认"))
        assertFalse(completedUiState.canEnd)

        val abandoned = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.dispatch(
                StrengthWorkoutEngine.create(plan),
                WorkoutCommand.StartSession
            ).state,
            WorkoutCommand.EndSession(reason = "user_requested")
        ).state
        val abandonedUiState = abandoned.toStrengthWorkoutSessionScreenState()

        assertTrue(abandonedUiState.isTerminal)
        assertEquals("力量训练已提前结束", abandonedUiState.terminalTitle)
        assertTrue(requireNotNull(abandonedUiState.terminalSummary).contains("用户主动结束"))
    }

    @Test
    fun heartRatePlaceholderStaysSecondaryAndAbstract() {
        val plan = buildDefaultPlanManagementState().plans[1]
        val started = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val uiState = started.toStrengthWorkoutSessionScreenState(
            heartRateState = HeartRateState(availability = HeartRateAvailability.NOT_CONNECTED)
        )

        assertEquals("-- bpm", uiState.heartRate.valueText)
        assertEquals("未连接设备", uiState.heartRate.statusText)
        assertFalse(uiState.heartRate.isAvailable)
    }

    private fun completedFirstSet(plan: WorkoutPlan): StrengthWorkoutEngineState {
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 7).state
        return StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
    }

    private fun strengthPlan(
        actionWeight: WeightValue? = WeightValue(value = 60.0, unit = WeightUnit.KG),
        actionRepTarget: RepTarget? = RepTarget.Range(minReps = 8, maxReps = 12),
        setWeight: WeightValue? = null,
        setRepTarget: RepTarget? = null
    ): WorkoutPlan {
        return WorkoutPlan(
            id = "ui-strength-plan",
            mode = WorkoutMode.STRENGTH,
            title = "UI Strength",
            blocks = listOf(
                StrengthExerciseBlock(
                    id = "bench",
                    order = 1,
                    exerciseId = "barbell-bench-press",
                    target = StrengthExerciseTarget(
                        weight = actionWeight,
                        repTarget = actionRepTarget,
                        restAfterSetSec = 60
                    ),
                    sets = listOf(
                        StrengthSetPlan(
                            id = "bench-working-1",
                            order = 1,
                            kind = StrengthSetKind.WORKING,
                            targetWeight = setWeight,
                            repTarget = setRepTarget,
                            restAfterSec = 60
                        )
                    )
                )
            ),
            createdAt = "2026-05-31T00:00:00Z",
            updatedAt = "2026-05-31T00:00:00Z"
        )
    }
}
