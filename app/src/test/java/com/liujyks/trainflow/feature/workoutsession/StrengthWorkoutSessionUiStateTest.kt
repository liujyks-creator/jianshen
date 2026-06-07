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
        assertTrue(uiState.immediateControls.any { control ->
            control.role == WorkoutImmediateControlRole.COMPLETE_STRENGTH_SET &&
                control.placement == WorkoutImmediateControlPlacement.FIXED_BOTTOM &&
                control.enabled
        })
        assertTrue(uiState.immediateControls.any { control ->
            control.role == WorkoutImmediateControlRole.PAUSE_SESSION &&
                control.placement == WorkoutImmediateControlPlacement.RHYTHM_SURFACE &&
                control.enabled
        })
        assertTrue(uiState.endRequiresConfirmation)
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
        assertTrue(uiState.immediateControls.any { control ->
            control.role == WorkoutImmediateControlRole.CONFIRM_STRENGTH_SET &&
                control.placement == WorkoutImmediateControlPlacement.FIXED_BOTTOM &&
                control.enabled
        })
        assertTrue(uiState.immediateControls.any { control ->
            control.role == WorkoutImmediateControlRole.END_SESSION &&
                control.placement == WorkoutImmediateControlPlacement.FIXED_BOTTOM &&
                control.enabled
        })
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
    fun confirmationRejectsBlankWeightWhenPlannedWeightExists() {
        val confirmation = completedFirstSet(strengthPlan()).toStrengthWorkoutSessionScreenState()
            .confirmation
            .let(::requireNotNull)

        val validation = confirmation.initialInputState()
            .copy(actualWeightInput = "")
            .validateFor(confirmation)

        assertFalse(validation.canConfirm)
        assertEquals("实际重量不能为空", validation.errorText)
        assertNull(validation.commandInput)
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
        assertFalse(uiState.canReplaceExercise)
        assertTrue(uiState.canSkipExercise)
    }

    @Test
    fun replacementCandidatesOnlyIncludeStrengthCapableExercises() {
        val plan = buildDefaultPlanManagementState().plans[1]
        val started = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val uiState = started.toStrengthWorkoutSessionScreenState()

        assertTrue(uiState.canReplaceExercise)
        assertTrue(uiState.replacementOptions.isNotEmpty())
        assertFalse(uiState.replacementOptions.any { option ->
            option.exerciseId == started.currentSet?.exerciseId
        })
        assertTrue(uiState.replacementOptions.none { option ->
            option.exerciseId == "jumping-jacks" || option.exerciseId == "forearm-plank"
        })
    }

    @Test
    fun currentReplaceAndSkipActionsBuildWorkoutCommandsForRouteDispatch() {
        val plan = buildDefaultPlanManagementState().plans[1]
        val started = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val uiState = started.toStrengthWorkoutSessionScreenState()
        val replacement = uiState.replacementOptions.first()

        assertEquals(
            WorkoutCommand.ReplaceExercise(
                fromExerciseId = requireNotNull(started.currentSet).exerciseId,
                toExerciseId = replacement.exerciseId
            ),
            started.currentReplaceExerciseCommand(replacement.exerciseId)
        )
        assertEquals(WorkoutCommand.SkipStep, started.currentSkipExerciseCommand())
    }

    @Test
    fun substitutedExerciseMapsOriginalSourceLabel() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(
            state,
            WorkoutCommand.ReplaceExercise(
                fromExerciseId = requireNotNull(state.currentSet).exerciseId,
                toExerciseId = "incline-push-up"
            )
        ).state
        val uiState = state.toStrengthWorkoutSessionScreenState()

        assertTrue(uiState.currentExerciseName.contains("上斜俯卧撑"))
        assertTrue(uiState.substitutionSummaryLabel.contains("替换"))
        assertTrue(uiState.canSkipExercise)
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
    fun completedStrengthSummaryShowsPlanActualSetsDurationRestAndNoAutoProgression() {
        val plan = twoSetSummaryPlan()
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 3).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        state = StrengthWorkoutEngine.dispatch(
            state,
            WorkoutCommand.ConfirmStrengthSet(
                StrengthSetCompletionInput(
                    actualWeight = WeightValue(value = 60.0, unit = WeightUnit.KG),
                    actualReps = 9,
                    effort = SetEffort.GOOD
                )
            )
        ).state
        state = StrengthWorkoutEngine.tick(state, seconds = 3).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 4).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        state = StrengthWorkoutEngine.dispatch(
            state,
            WorkoutCommand.ConfirmStrengthSet(
                StrengthSetCompletionInput(
                    actualWeight = WeightValue(value = 57.5, unit = WeightUnit.KG),
                    actualReps = 6,
                    effort = SetEffort.HARD
                )
            )
        ).state

        val summary = requireNotNull(state.toStrengthWorkoutSessionScreenState().summary)
        val metrics = summary.metricItems.associateBy { metric -> metric.label }
        val setRows = summary.exerciseSummaries.single().setItems

        assertEquals("力量训练复盘", summary.title)
        assertEquals(StrengthWorkoutSummaryTone.COMPLETED, summary.tone)
        assertEquals("1 / 1", metrics.getValue("动作").value)
        assertEquals("2 / 2", metrics.getValue("组数").value)
        assertEquals("7秒", metrics.getValue("组耗时").value)
        assertEquals("3秒", metrics.getValue("实际休息").value)
        assertEquals("60 kg", setRows[0].plannedWeightLabel)
        assertEquals("60 kg", setRows[0].actualWeightLabel)
        assertEquals("8-10 次", setRows[0].plannedRepLabel)
        assertEquals("9 次", setRows[0].actualRepLabel)
        assertEquals("3秒", setRows[0].activeDurationLabel)
        assertEquals("3秒", setRows[0].restAfterLabel)
        assertTrue(setRows[1].differenceLabel.contains("57.5 kg"))
        assertTrue(setRows[1].differenceLabel.contains("6 次"))
        assertTrue(summary.planVsActualSummary.contains("重量差异 1 组"))
        assertTrue(summary.planVsActualSummary.contains("次数差异 1 组"))
        assertTrue(summary.planVsActualSummary.contains("不生成"))
        assertFalse(summary.planVsActualSummary.contains("自动"))
        assertTrue(summary.recoveryEntry.enabled)
        assertTrue(summary.recoveryEntry.generated)
        val recovery = requireNotNull(summary.recoveryEntry.recommendation)
        assertTrue(recovery.recommendation.areaIds.contains("chest-shoulder-release"))
        assertTrue(recovery.recommendation.trainedMuscleIds.contains("chest"))
        assertFalse(summary.recoveryEntry.description.contains("E5.4"))
    }

    @Test
    fun strengthSummaryMapsReplacementAndSkippedSets() {
        val plan = replacementAndSkipPlan()
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        state = StrengthWorkoutEngine.dispatch(
            state,
            WorkoutCommand.ReplaceExercise(
                fromExerciseId = "barbell-bench-press",
                toExerciseId = "incline-push-up"
            )
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 2).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state

        val summary = requireNotNull(state.toStrengthWorkoutSessionScreenState().summary)

        assertTrue(summary.replacementSummary.contains("替换 1 次"))
        assertTrue(summary.replacementSummary.contains("->"))
        assertTrue(summary.skippedSummary, summary.skippedSummary.contains("跳过 1 组"))
        assertTrue(summary.earlyEndSummary.contains("包含主动跳过"))
        assertTrue(summary.exerciseSummaries.first().replacementLabel.orEmpty().contains("替换为"))
        assertEquals("跳过 1 组", summary.exerciseSummaries[1].skippedLabel)
    }

    @Test
    fun strengthSummaryKeepsPlannedExerciseCardWhenLaterSetIsReplaced() {
        val plan = twoSetSummaryPlan()
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 2).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.dispatch(
            state,
            WorkoutCommand.ReplaceExercise(
                fromExerciseId = "barbell-bench-press",
                toExerciseId = "incline-push-up"
            )
        ).state
        state = StrengthWorkoutEngine.tick(state, seconds = 2).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())).state

        val summary = requireNotNull(state.toStrengthWorkoutSessionScreenState().summary)
        val exerciseSummary = summary.exerciseSummaries.single()

        assertEquals("杠铃卧推", exerciseSummary.exerciseName)
        assertTrue(exerciseSummary.replacementLabel.orEmpty().contains("上斜俯卧撑"))
        assertEquals("实际动作：杠铃卧推", exerciseSummary.setItems[0].actualExerciseLabel)
        assertEquals("实际动作：上斜俯卧撑（替换自 杠铃卧推）", exerciseSummary.setItems[1].actualExerciseLabel)
        assertEquals("barbell-bench-press", state.strengthSetRecords[1].substitutedFromExerciseId)
    }

    @Test
    fun skippedSummaryCountsOmittedActionGroupsInsteadOfSkippedSets() {
        val plan = multiActionSkipSummaryPlan()
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        repeat(4) {
            state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        }

        val summary = requireNotNull(state.toStrengthWorkoutSessionScreenState().summary)

        assertTrue(summary.skippedSummary, summary.skippedSummary.contains("跳过 6 组"))
        assertTrue(summary.skippedSummary, summary.skippedSummary.contains("杠铃卧推 2 组"))
        assertTrue(summary.skippedSummary, summary.skippedSummary.contains("单臂哑铃划船 1 组"))
        assertTrue(summary.skippedSummary, summary.skippedSummary.contains("哑铃杯式深蹲 2 组"))
        assertTrue(summary.skippedSummary, summary.skippedSummary.contains("另 1 个动作分组"))
        assertFalse(summary.skippedSummary, summary.skippedSummary.contains("等 3 组"))
    }

    @Test
    fun abandonedStrengthSummaryKeepsNeutralReasonAndProgress() {
        val plan = twoSetSummaryPlan()
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        state = StrengthWorkoutEngine.tick(state, seconds = 2).state
        state = StrengthWorkoutEngine.dispatch(
            state,
            WorkoutCommand.EndSession(reason = "user_requested")
        ).state

        val summary = requireNotNull(state.toStrengthWorkoutSessionScreenState().summary)

        assertEquals("提前结束记录", summary.title)
        assertEquals(StrengthWorkoutSummaryTone.ABANDONED, summary.tone)
        assertTrue(summary.earlyEndSummary.contains("原因：用户主动结束"))
        assertTrue(summary.earlyEndSummary.contains("已确认 0 组"))
        assertTrue(summary.earlyEndSummary.contains("当前步骤已执行 2秒"))
        assertFalse(summary.earlyEndSummary.contains("user_requested"))
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

    private fun twoSetSummaryPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "summary-strength-plan",
            mode = WorkoutMode.STRENGTH,
            title = "Summary Strength",
            blocks = listOf(
                StrengthExerciseBlock(
                    id = "bench",
                    order = 1,
                    exerciseId = "barbell-bench-press",
                    target = StrengthExerciseTarget(
                        weight = WeightValue(value = 60.0, unit = WeightUnit.KG),
                        repTarget = RepTarget.Range(minReps = 8, maxReps = 10),
                        restAfterSetSec = 3
                    ),
                    sets = listOf(
                        StrengthSetPlan(
                            id = "bench-working-1",
                            order = 1,
                            kind = StrengthSetKind.WORKING
                        ),
                        StrengthSetPlan(
                            id = "bench-working-2",
                            order = 2,
                            kind = StrengthSetKind.WORKING,
                            restAfterSec = 0
                        )
                    )
                )
            ),
            createdAt = "2026-06-01T00:00:00Z",
            updatedAt = "2026-06-01T00:00:00Z"
        )
    }

    private fun replacementAndSkipPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "replace-skip-summary-plan",
            mode = WorkoutMode.STRENGTH,
            title = "Replace Skip Summary",
            blocks = listOf(
                StrengthExerciseBlock(
                    id = "bench",
                    order = 1,
                    exerciseId = "barbell-bench-press",
                    target = StrengthExerciseTarget(
                        weight = WeightValue(value = 60.0, unit = WeightUnit.KG),
                        repTarget = RepTarget.Fixed(reps = 8),
                        restAfterSetSec = 0
                    ),
                    substitutions = listOf("incline-push-up"),
                    sets = listOf(
                        StrengthSetPlan(
                            id = "bench-working-1",
                            order = 1,
                            kind = StrengthSetKind.WORKING
                        )
                    )
                ),
                StrengthExerciseBlock(
                    id = "row",
                    order = 2,
                    exerciseId = "one-arm-dumbbell-row",
                    target = StrengthExerciseTarget(
                        weight = WeightValue(value = 24.0, unit = WeightUnit.KG),
                        repTarget = RepTarget.Fixed(reps = 10)
                    ),
                    sets = listOf(
                        StrengthSetPlan(
                            id = "row-working-1",
                            order = 1,
                            kind = StrengthSetKind.WORKING
                        )
                    )
                )
            ),
            createdAt = "2026-06-01T00:00:00Z",
            updatedAt = "2026-06-01T00:00:00Z"
        )
    }

    private fun multiActionSkipSummaryPlan(): WorkoutPlan {
        fun block(
            id: String,
            order: Int,
            exerciseId: String,
            weight: Double,
            reps: Int,
            setCount: Int
        ): StrengthExerciseBlock {
            return StrengthExerciseBlock(
                id = id,
                order = order,
                exerciseId = exerciseId,
                target = StrengthExerciseTarget(
                    weight = WeightValue(value = weight, unit = WeightUnit.KG),
                    repTarget = RepTarget.Fixed(reps = reps),
                    restAfterSetSec = 0
                ),
                sets = (1..setCount).map { setNumber ->
                    StrengthSetPlan(
                        id = "$id-working-$setNumber",
                        order = setNumber,
                        kind = StrengthSetKind.WORKING
                    )
                }
            )
        }

        return WorkoutPlan(
            id = "multi-skip-summary-plan",
            mode = WorkoutMode.STRENGTH,
            title = "Multi Skip Summary",
            blocks = listOf(
                block(
                    id = "bench",
                    order = 1,
                    exerciseId = "barbell-bench-press",
                    weight = 60.0,
                    reps = 8,
                    setCount = 2
                ),
                block(
                    id = "row",
                    order = 2,
                    exerciseId = "one-arm-dumbbell-row",
                    weight = 24.0,
                    reps = 10,
                    setCount = 1
                ),
                block(
                    id = "goblet",
                    order = 3,
                    exerciseId = "dumbbell-goblet-squat",
                    weight = 20.0,
                    reps = 10,
                    setCount = 2
                ),
                block(
                    id = "rdl",
                    order = 4,
                    exerciseId = "dumbbell-romanian-deadlift",
                    weight = 24.0,
                    reps = 10,
                    setCount = 1
                )
            ),
            createdAt = "2026-06-01T00:00:00Z",
            updatedAt = "2026-06-01T00:00:00Z"
        )
    }
}
