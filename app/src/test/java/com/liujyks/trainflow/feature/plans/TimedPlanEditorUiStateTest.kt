package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.ExerciseSide
import com.liujyks.trainflow.core.model.PlanBlockKind
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.TimedCircuitBlock
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
    fun defaultEditorCreatesUsableTimedPlanDraft() {
        val state = buildDefaultTimedPlanEditorState()
        val plan = state.toWorkoutPlan()
        val circuit = plan.blocks.filterIsInstance<TimedCircuitBlock>().single()

        assertTrue(state.canSave)
        assertEquals("全身计时循环", plan.title)
        assertEquals(WorkoutMode.TIMED, plan.mode)
        assertEquals(3, plan.blocks.size)
        assertTrue(plan.blocks[0] is WarmupBlock)
        assertEquals(PlanBlockKind.TIMED_CIRCUIT, circuit.kind)
        assertEquals(2, circuit.rounds)
        assertEquals(60, circuit.restBetweenRoundsSec)
        assertEquals(3, circuit.items.size)
        assertEquals("jumping-jacks", circuit.items.first().exerciseId)
        assertEquals(30, circuit.items.first().workDurationSec)
        assertEquals(15, circuit.items.first().restAfterSec)
        assertTrue(circuit.items.all { it.autoAdvance })
        assertTrue(plan.blocks[2] is StretchBlock)
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
    fun actionCueThresholdCannotExceedShortestWorkDuration() {
        val state = buildDefaultTimedPlanEditorState()
            .updateActionCueThreshold(60)

        assertEquals(30, state.actionCue.thresholdSec)
        assertEquals(30, state.toWorkoutPlan().preferences?.cueSettings?.actionEnding?.thresholdSec)
    }

    @Test
    fun restCueThresholdCannotExceedShortestPositiveRestDuration() {
        val state = buildDefaultTimedPlanEditorState()
            .updateRestCueThreshold(60)

        assertEquals(15, state.restCue.thresholdSec)
        assertEquals(15, state.toWorkoutPlan().preferences?.cueSettings?.restEnding?.thresholdSec)
    }

    @Test
    fun zeroRestStateDisablesRestCueAndOmitsRestEndingCue() {
        val state = buildDefaultTimedPlanEditorState()
            .updateRestCueThreshold(5)
            .updateRestBetweenRounds(0)
            .withoutItemRests()
            .updateRestCueEnabled(true)

        assertFalse(state.restCue.enabled)
        assertNull(state.toWorkoutPlan().preferences?.cueSettings?.restEnding)
    }

    @Test
    fun updateItemWorkDurationReclampsActionCueThreshold() {
        val firstItemId = buildDefaultTimedPlanEditorState().items.first().id
        val state = buildDefaultTimedPlanEditorState()
            .updateActionCueThreshold(20)
            .updateItemWorkDuration(firstItemId, 5)

        assertEquals(5, state.actionCue.thresholdSec)
        assertEquals(5, state.toWorkoutPlan().preferences?.cueSettings?.actionEnding?.thresholdSec)
    }

    @Test
    fun updateItemRestAfterAndRoundRestReclampRestCueThreshold() {
        val firstItemId = buildDefaultTimedPlanEditorState().items.first().id
        val afterItemRestChange = buildDefaultTimedPlanEditorState()
            .updateRestCueThreshold(20)
            .updateItemRestAfter(firstItemId, 10)
        val afterRoundRestChange = afterItemRestChange.updateRestBetweenRounds(8)

        assertEquals(10, afterItemRestChange.restCue.thresholdSec)
        assertEquals(8, afterRoundRestChange.restCue.thresholdSec)
        assertEquals(8, afterRoundRestChange.toWorkoutPlan().preferences?.cueSettings?.restEnding?.thresholdSec)
    }

    @Test
    fun toWorkoutPlanClampsCueSettingsEvenWhenStateWasBuiltDirectly() {
        val invalidState = buildDefaultTimedPlanEditorState().copy(
            restBetweenRoundsSec = 0,
            actionCue = CountdownCueUiState(thresholdSec = 60),
            restCue = CountdownCueUiState(enabled = true, thresholdSec = 60),
            items = buildDefaultTimedPlanEditorState().items.map { item ->
                item.copy(workDurationSec = 5, restAfterSec = 0)
            }
        )
        val cues = requireNotNull(invalidState.toWorkoutPlan().preferences?.cueSettings)

        assertEquals(5, cues.actionEnding?.thresholdSec)
        assertNull(cues.restEnding)
    }

    @Test
    fun fieldEditsUpdateRoundsRestDurationsAndContractMapping() {
        val firstItemId = buildDefaultTimedPlanEditorState().items.first().id
        val state = buildDefaultTimedPlanEditorState()
            .updateTitle("核心燃脂")
            .updateWarmupDuration(90)
            .updateStretchDuration(0)
            .updateRounds(4)
            .updateRestBetweenRounds(45)
            .updateItemWorkDuration(firstItemId, 50)
            .updateItemRestAfter(firstItemId, 10)
        val plan = state.toWorkoutPlan()
        val circuit = plan.blocks.filterIsInstance<TimedCircuitBlock>().single()

        assertEquals("核心燃脂", plan.title)
        assertEquals(2, plan.blocks.size)
        assertEquals(90, (plan.blocks.first() as WarmupBlock).durationSec)
        assertEquals(4, circuit.rounds)
        assertEquals(45, circuit.restBetweenRoundsSec)
        assertEquals(50, circuit.items.first().workDurationSec)
        assertEquals(10, circuit.items.first().restAfterSec)
        assertEquals(CountdownCue.DEFAULT_THRESHOLD_SEC, plan.preferences?.cueSettings?.actionEnding?.thresholdSec)
    }

    @Test
    fun addAndRemoveTimedExercisesStayWithinFixtureCapabilities() {
        val initial = buildDefaultTimedPlanEditorState()
        val added = initial.addExercise("standing-quad-stretch")
        val addedAgain = added.addExercise("standing-quad-stretch")
        val removed = added.removeItem(added.items.first().id)
        val ignoredStrengthOnly = removed.addExercise("barbell-bench-press")

        assertEquals(initial.items.size + 1, added.items.size)
        assertEquals(added.items.size, addedAgain.items.size)
        assertTrue(added.items.any { it.exerciseId == "standing-quad-stretch" })
        assertEquals(added.items.size - 1, removed.items.size)
        assertEquals(removed.items.size, ignoredStrengthOnly.items.size)
        assertFalse(ignoredStrengthOnly.items.any { it.exerciseId == "barbell-bench-press" })
    }

    @Test
    fun addingStandingQuadStretchMapsTimedDefaultToAlternatingSide() {
        val state = buildDefaultTimedPlanEditorState()
            .addExercise("standing-quad-stretch")
        val plan = state.toWorkoutPlan()
        val circuit = plan.blocks.filterIsInstance<TimedCircuitBlock>().single()
        val quadStretch = circuit.items.single { it.exerciseId == "standing-quad-stretch" }

        assertEquals(ExerciseSide.ALTERNATING, quadStretch.side)
        assertEquals(30, quadStretch.workDurationSec)
        assertEquals(5, quadStretch.restAfterSec)
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

    private fun TimedPlanEditorScreenState.withoutItemRests(): TimedPlanEditorScreenState {
        return items.fold(this) { state, item ->
            state.updateItemRestAfter(item.id, 0)
        }
    }
}
