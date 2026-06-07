package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedPlanEditorUiStateTest {
    @Test
    fun defaultEditorCreatesPureIntervalPlanWithoutExerciseLibraryDependency() {
        val state = buildDefaultTimedPlanEditorState()
        val plan = state.toWorkoutPlan()
        val circuit = plan.blocks.filterIsInstance<TimedCircuitBlock>().single()

        assertTrue(state.canSave)
        assertTrue(state.canStartTraining)
        assertEquals("纯间歇计时器", plan.title)
        assertEquals(WorkoutMode.TIMED, plan.mode)
        assertTrue(plan.blocks.first() is WarmupBlock)
        assertTrue(plan.blocks.last() is CooldownBlock)
        assertEquals(3, circuit.rounds)
        assertEquals(60, circuit.restBetweenRoundsSec)
        assertEquals(3, circuit.items.size)
        assertTrue(circuit.items.all { item -> item.exerciseId == null })
        assertEquals(listOf(TimedStageType.WORK, TimedStageType.REST, TimedStageType.CUSTOM), circuit.items.map { it.stageType })
        assertEquals("训练", circuit.items.first().labelOverride)
        assertEquals(45, circuit.items.first().workDurationSec)
        assertEquals(null, circuit.items.first().restAfterSec)
    }

    @Test
    fun mapsCueSettingsToPlanPreferencesWithoutVoiceOutput() {
        val state = buildDefaultTimedPlanEditorState()
            .updateActionCueThreshold(7)
            .updateRestCueThreshold(4)
            .updateSoundEnabled(false)
            .updateVibrationEnabled(false)
        val cues = requireNotNull(state.toWorkoutPlan().preferences?.cueSettings)
        val actionCue = requireNotNull(cues.actionEnding)
        val restCue = requireNotNull(cues.restEnding)

        assertEquals(7, actionCue.thresholdSec)
        assertEquals(4, restCue.thresholdSec)
        assertFalse(actionCue.soundEnabled)
        assertFalse(restCue.soundEnabled)
        assertFalse(actionCue.vibrationEnabled)
        assertFalse(restCue.vibrationEnabled)
        assertFalse(actionCue.voiceCueEnabled)
        assertFalse(restCue.voiceCueEnabled)
    }

    @Test
    fun newTimedEditorConsumesTrainingPreferenceDefaults() {
        val state = buildDefaultTimedPlanEditorState(
            defaults = PlanEditorDefaults(
                actionCueEnabled = false,
                restCueEnabled = true,
                soundEnabled = false,
                vibrationEnabled = false,
                emphasisAnimationEnabled = false,
                defaultCountdownThresholdSec = 8
            )
        )
        val cues = requireNotNull(state.toWorkoutPlan().preferences?.cueSettings)

        assertFalse(state.actionCue.enabled)
        assertTrue(state.restCue.enabled)
        assertEquals(8, cues.actionEnding?.thresholdSec)
        assertEquals(8, cues.restEnding?.thresholdSec)
        assertFalse(requireNotNull(cues.actionEnding).enabled)
        assertFalse(requireNotNull(cues.actionEnding).soundEnabled)
        assertFalse(requireNotNull(cues.restEnding).vibrationEnabled)
        assertFalse(requireNotNull(cues.restEnding).emphasisAnimationEnabled)
    }

    @Test
    fun changingTrainingPreferenceDefaultsDoesNotRewriteExistingTimedDraftCueSettings() {
        val existingState = buildDefaultTimedPlanEditorState()
            .updateActionCueThreshold(7)
            .updateRestCueThreshold(4)
            .updateSoundEnabled(false)
            .updateVibrationEnabled(false)
            .updateEmphasisAnimationEnabled(false)
        val savedBeforePreferenceChange = requireNotNull(existingState.saveDraftPlan().savedPlan)
        val changedDefaults = PlanEditorDefaults(
            actionCueEnabled = false,
            restCueEnabled = false,
            soundEnabled = true,
            vibrationEnabled = true,
            emphasisAnimationEnabled = true,
            defaultCountdownThresholdSec = 12
        )
        val newStateAfterPreferenceChange = buildDefaultTimedPlanEditorState(defaults = changedDefaults)

        assertEquals(7, existingState.toWorkoutPlan().preferences?.cueSettings?.actionEnding?.thresholdSec)
        assertEquals(4, existingState.toWorkoutPlan().preferences?.cueSettings?.restEnding?.thresholdSec)
        assertFalse(requireNotNull(savedBeforePreferenceChange.preferences?.cueSettings?.actionEnding).soundEnabled)
        assertEquals(12, newStateAfterPreferenceChange.toWorkoutPlan().preferences?.cueSettings?.actionEnding?.thresholdSec)
        assertFalse(requireNotNull(newStateAfterPreferenceChange.toWorkoutPlan().preferences?.cueSettings?.actionEnding).enabled)
    }

    @Test
    fun cueThresholdsClampToShortestStageAndRestDuration() {
        val state = buildDefaultTimedPlanEditorState()
            .updateActionCueThreshold(60)
            .updateRestCueThreshold(60)

        assertEquals(30, state.actionCue.thresholdSec)
        assertEquals(15, state.restCue.thresholdSec)
        assertEquals(30, state.toWorkoutPlan().preferences?.cueSettings?.actionEnding?.thresholdSec)
        assertEquals(15, state.toWorkoutPlan().preferences?.cueSettings?.restEnding?.thresholdSec)
    }

    @Test
    fun zeroRestStateDisablesRestCueAndOmitsRestEndingCue() {
        val restId = buildDefaultTimedPlanEditorState().stages.first { it.stageType == TimedStageType.REST }.id
        val state = buildDefaultTimedPlanEditorState()
            .updateRestBetweenRounds(0)
            .updateStageType(restId, TimedStageType.WORK)
            .updateRestCueEnabled(true)

        assertFalse(state.restCue.enabled)
        assertNull(state.toWorkoutPlan().preferences?.cueSettings?.restEnding)
    }

    @Test
    fun stageEditsUpdateContractMapping() {
        val workId = buildDefaultTimedPlanEditorState().stages.first { it.stageType == TimedStageType.WORK }.id
        val state = buildDefaultTimedPlanEditorState()
            .updateTitle("核心间歇")
            .updateRounds(4)
            .updateRestBetweenRounds(45)
            .updateStageName(workId, "冲刺")
            .updateStageDuration(workId, 50)
            .updateStageType(workId, TimedStageType.CUSTOM)
        val plan = state.toWorkoutPlan()
        val circuit = plan.blocks.filterIsInstance<TimedCircuitBlock>().single()

        assertEquals("核心间歇", plan.title)
        assertEquals(4, circuit.rounds)
        assertEquals(45, circuit.restBetweenRoundsSec)
        assertEquals("冲刺", circuit.items.first().labelOverride)
        assertEquals(TimedStageType.CUSTOM, circuit.items.first().stageType)
        assertEquals(50, circuit.items.first().workDurationSec)
    }

    @Test
    fun stageAddCopyDeleteAndSortWorkWithoutFakeDrag() {
        val initial = buildDefaultTimedPlanEditorState()
        val added = initial.addStage(TimedStageType.REST)
        val copied = added.copyStage(added.stages.first().id)
        val copiedId = copied.stages[1].id
        val movedDown = copied.moveStageDown(copiedId)
        val movedUp = movedDown.moveStageUp(copiedId)
        val removed = movedUp.removeStage(copiedId)

        assertEquals(initial.stages.size + 1, added.stages.size)
        assertEquals(added.stages.size + 1, copied.stages.size)
        assertTrue(copied.stages[1].name.contains("副本"))
        assertEquals(copiedId, movedDown.stages[2].id)
        assertEquals(copiedId, movedUp.stages[1].id)
        assertEquals(movedUp.stages.size - 1, removed.stages.size)
    }

    @Test
    fun integerFieldsCanBeTemporarilyBlankAndThenReentered() {
        val firstStageId = buildDefaultTimedPlanEditorState().stages.first().id
        val blankStage = buildDefaultTimedPlanEditorState().updateStageDurationText(firstStageId, "")
        val blankRounds = buildDefaultTimedPlanEditorState().updateRoundsText("")
        val blankRoundRest = buildDefaultTimedPlanEditorState().updateRestBetweenRoundsText("")

        assertEquals("", blankStage.stages.first().durationText)
        assertFalse(blankStage.canStartTraining)
        assertTrue(requireNotNull(blankStage.validationMessage).contains("阶段秒数"))
        assertFalse(blankRounds.canSave)
        assertTrue(requireNotNull(blankRounds.validationMessage).contains("轮数"))
        assertFalse(blankRoundRest.canSave)
        assertTrue(requireNotNull(blankRoundRest.validationMessage).contains("轮间休息秒数"))

        val reentered = blankStage
            .updateStageDurationText(firstStageId, "95")
            .updateRoundsText("3")
            .updateRestBetweenRoundsText("45")

        assertTrue(reentered.canSave)
        assertTrue(reentered.canStartTraining)
        assertEquals(95, reentered.stages.first().durationSec)
        assertEquals(3, reentered.rounds)
        assertEquals(45, reentered.restBetweenRoundsSec)
    }

    @Test
    fun estimatedDurationIncludesWarmupStagesRoundsRoundRestAndCooldown() {
        val state = buildDefaultTimedPlanEditorState()

        assertEquals(180 + (45 + 15 + 30) * 3 + 60 * 2 + 120, state.estimatedDurationSec)
    }

    @Test
    fun timedEditorStartUsesCurrentValidPureIntervalDraftPlan() {
        val workId = buildDefaultTimedPlanEditorState().stages.first { it.stageType == TimedStageType.WORK }.id
        val state = buildDefaultTimedPlanEditorState()
            .updateTitle("立即开始计时")
            .updateStageDurationText(workId, "40")

        val plan = state.toWorkoutPlan(planId = "plan-timed-editor-start")
        val circuit = plan.blocks.filterIsInstance<TimedCircuitBlock>().single()

        assertTrue(state.canStartTraining)
        assertEquals("plan-timed-editor-start", plan.id)
        assertEquals("立即开始计时", plan.title)
        assertEquals(40, circuit.items.first().workDurationSec)
        assertTrue(circuit.items.all { it.exerciseId == null })
    }

    @Test
    fun saveDraftKeepsPlanInMemoryOnlyForCurrentEditorState() {
        val unsaved = buildDefaultTimedPlanEditorState()
        val saved = unsaved.saveDraftPlan()
        val editedAfterSave = saved.updateTitle("改名后需要重新保存")

        assertNull(unsaved.savedPlan)
        assertNotNull(saved.savedPlan)
        assertTrue(requireNotNull(saved.statusMessage).contains("真实保存"))
        assertNull(editedAfterSave.savedPlan)
        assertNull(editedAfterSave.statusMessage)
    }

    @Test
    fun toWorkoutPlanClampsCueSettingsEvenWhenStateWasBuiltDirectly() {
        val invalidState = buildDefaultTimedPlanEditorState().copy(
            actionCue = CountdownCueUiState(thresholdSec = 60),
            restCue = CountdownCueUiState(enabled = true, thresholdSec = 60),
            stages = buildDefaultTimedPlanEditorState().stages.map { stage ->
                stage.copy(durationSec = 5)
            }
        )
        val cues = requireNotNull(invalidState.toWorkoutPlan().preferences?.cueSettings)

        assertEquals(5, cues.actionEnding?.thresholdSec)
        assertEquals(5, cues.restEnding?.thresholdSec)
    }
}
