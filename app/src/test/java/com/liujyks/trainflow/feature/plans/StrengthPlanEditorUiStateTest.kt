package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.ExerciseSide
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WorkoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthPlanEditorUiStateTest {
    @Test
    fun defaultEditorCreatesUsableStrengthPlanDraft() {
        val state = buildDefaultStrengthPlanEditorState()
        val plan = state.toWorkoutPlan()
        val blocks = plan.blocks.filterIsInstance<StrengthExerciseBlock>()
        val firstBlock = blocks.first()

        assertTrue(state.canSave)
        assertEquals("基础力量计划", plan.title)
        assertEquals(WorkoutMode.STRENGTH, plan.mode)
        assertEquals(state.exercises.size, blocks.size)
        assertEquals("dumbbell-goblet-squat", firstBlock.exerciseId)
        assertEquals(StrengthSetTimerMode.MANUAL_START, firstBlock.setTimerMode)
        assertEquals(3, firstBlock.sets.size)
        assertTrue(firstBlock.sets.all { it.kind == StrengthSetKind.WORKING })
        val target = requireNotNull(firstBlock.target)
        assertEquals(20.0, requireNotNull(target.weight).value, 0.0)
        assertEquals(WeightUnit.KG, requireNotNull(target.weight).unit)
        assertTrue(requireNotNull(target.repTarget) is RepTarget.Range)
        assertEquals(90, target.restAfterSetSec)
    }

    @Test
    fun defaultsStrengthRepTargetToEightToTwelveWhenAddingActions() {
        val state = buildDefaultStrengthPlanEditorState()
            .addExercise("barbell-bench-press")
        val bench = state.exercises.first { it.exerciseId == "barbell-bench-press" }
        val block = state.toWorkoutPlan().blocks
            .filterIsInstance<StrengthExerciseBlock>()
            .first { it.exerciseId == "barbell-bench-press" }
        val reps = requireNotNull(block.target?.repTarget) as RepTarget.Range

        assertEquals(StrengthRepTargetKind.RANGE, bench.repTarget.kind)
        assertEquals(8, reps.minReps)
        assertEquals(12, reps.maxReps)
        assertEquals(bench.totalSets, block.sets.size)
        assertEquals(bench.warmupSets, block.sets.count { it.kind == StrengthSetKind.WARMUP })
        assertTrue(block.sets.filter { it.kind == StrengthSetKind.WORKING }.all { it.repTarget is RepTarget.Range })
    }

    @Test
    fun perSideStrengthExerciseMapsPlannedSetsToAlternatingSide() {
        val state = buildDefaultStrengthPlanEditorState()
            .addExercise("alternating-reverse-lunge")
        val lunge = state.exercises.first { it.exerciseId == "alternating-reverse-lunge" }
        val block = state.toWorkoutPlan().blocks
            .filterIsInstance<StrengthExerciseBlock>()
            .first { it.exerciseId == "alternating-reverse-lunge" }

        assertTrue(lunge.perSide)
        assertTrue(block.sets.isNotEmpty())
        assertTrue(block.sets.all { it.side == ExerciseSide.ALTERNATING })
    }

    @Test
    fun fieldEditsUpdateActionTargetSetsWarmupAndContractMapping() {
        val exerciseId = buildDefaultStrengthPlanEditorState().exercises.first().id
        val state = buildDefaultStrengthPlanEditorState()
            .updateTitle("胸腿力量")
            .updateTargetWeight(exerciseId, 60.0)
            .updateRepRange(exerciseId, minReps = 6, maxReps = 8)
            .updateWorkingSets(exerciseId, 4)
            .updateWarmupSets(exerciseId, 1)
            .updateRestAfterSet(exerciseId, 120)
        val plan = state.toWorkoutPlan()
        val block = plan.blocks.filterIsInstance<StrengthExerciseBlock>().first()
        val warmup = block.sets.first()
        val working = block.sets.drop(1)
        val targetReps = requireNotNull(block.target?.repTarget) as RepTarget.Range

        assertEquals("胸腿力量", plan.title)
        assertEquals(5, block.sets.size)
        assertEquals(StrengthSetKind.WARMUP, warmup.kind)
        assertEquals(30.0, requireNotNull(warmup.targetWeight).value, 0.0)
        assertEquals(120, warmup.restAfterSec)
        assertTrue(working.all { it.kind == StrengthSetKind.WORKING })
        assertTrue(working.all { it.targetWeight?.value == 60.0 })
        assertTrue(working.all { it.restAfterSec == 120 })
        assertEquals(6, targetReps.minReps)
        assertEquals(8, targetReps.maxReps)
    }

    @Test
    fun expandedPerSetTargetCanOverrideIndividualSetValues() {
        val exercise = buildDefaultStrengthPlanEditorState().exercises.first()
        val secondSetId = exercise.setTargets[1].id
        val state = buildDefaultStrengthPlanEditorState()
            .updateSetTargetWeight(exercise.id, secondSetId, 55.0)
            .updateSetFixedReps(exercise.id, secondSetId, 10)
        val editedExercise = state.exercises.first()
        val block = state.toWorkoutPlan().blocks.filterIsInstance<StrengthExerciseBlock>().first()
        val secondSet = block.sets[1]

        assertTrue(editedExercise.expandedSetTargets)
        assertEquals(55.0, requireNotNull(secondSet.targetWeight).value, 0.0)
        assertEquals(10, (requireNotNull(secondSet.repTarget) as RepTarget.Fixed).reps)
    }

    @Test
    fun decimalWeightInputKeepsTrailingDecimalDraftsParseable() {
        assertEquals("20.", "20.".sanitizeDecimalInput())
        assertEquals(20.0, "20.".sanitizeDecimalInput().toDoubleOrNull())
        assertEquals("7.5", "7.5".sanitizeDecimalInput())
        assertEquals(7.5, "7.5".sanitizeDecimalInput().toDoubleOrNull())
        assertEquals("20.5", "20..5kg".sanitizeDecimalInput())
        assertEquals("20.5", 20.5.formatWeightInput())
        assertEquals("20", 20.0.formatWeightInput())
        assertEquals("", null.formatWeightInput())
    }

    @Test
    fun addAndRemoveStrengthExercisesStayWithinFixtureCapabilities() {
        val initial = buildDefaultStrengthPlanEditorState()
        val added = initial.addExercise("barbell-bench-press")
        val addedAgain = added.addExercise("barbell-bench-press")
        val removed = added.removeExercise(added.exercises.first().id)
        val ignoredTimedOnly = removed.addExercise("jumping-jacks")

        assertEquals(initial.exercises.size + 1, added.exercises.size)
        assertEquals(added.exercises.size, addedAgain.exercises.size)
        assertTrue(added.exercises.any { it.exerciseId == "barbell-bench-press" })
        assertEquals(added.exercises.size - 1, removed.exercises.size)
        assertEquals(removed.exercises.size, ignoredTimedOnly.exercises.size)
        assertFalse(ignoredTimedOnly.exercises.any { it.exerciseId == "jumping-jacks" })
    }

    @Test
    fun saveDraftKeepsPlanInMemoryOnlyForCurrentEditorState() {
        val unsaved = buildDefaultStrengthPlanEditorState()
        val saved = unsaved.saveDraftPlan()
        val editedAfterSave = saved.updateTitle("改名后需要重新保存")

        assertNull(unsaved.savedPlan)
        assertNotNull(saved.savedPlan)
        assertTrue(requireNotNull(saved.statusMessage).contains("真实保存"))
        assertNull(editedAfterSave.savedPlan)
        assertNull(editedAfterSave.statusMessage)
    }
}
