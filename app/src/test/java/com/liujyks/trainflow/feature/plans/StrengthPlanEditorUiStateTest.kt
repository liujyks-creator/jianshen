package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.ExerciseSide
import com.liujyks.trainflow.core.model.HeartRateDisplayPreference
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.PlanReminder
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthPlanEditorUiStateTest {
    @Test
    fun savedStrengthPlanBackfillsExercisesTargetsSetsRestOverridesAndSubstitutions() {
        val initialExercise = buildDefaultStrengthPlanEditorState().exercises.first()
        val baseState = buildDefaultStrengthPlanEditorState()
            .updateTitle("保存后的力量")
            .updateDescription("保留逐组计划")
            .updateTargetWeight(initialExercise.id, 64.0)
            .updateRepRange(initialExercise.id, minReps = 5, maxReps = 7)
            .updateWorkingSets(initialExercise.id, 4)
            .updateWarmupSets(initialExercise.id, 1)
            .updateRestAfterSet(initialExercise.id, 150)
        val exercise = baseState.exercises.first()
        val secondSetId = exercise.setTargets[1].id
        val savedPlan = baseState
            .updateSetTargetWeight(exercise.id, secondSetId, 68.0)
            .updateSetFixedReps(exercise.id, secondSetId, 6)
            .toWorkoutPlan(planId = "saved-strength", timestamp = "2026-06-14T01:00:00Z")

        val editor = savedPlan.toStrengthPlanEditorState()
        val editedExercise = editor.exercises.first()
        val resaved = editor.toWorkoutPlan(timestamp = "2026-06-15T01:00:00Z")
        val block = resaved.blocks.filterIsInstance<StrengthExerciseBlock>().first()
        val overriddenSet = block.sets.first { set -> set.id == secondSetId }

        assertEquals("saved-strength", editor.sourcePlanId)
        assertEquals("保存后的力量", editor.title)
        assertEquals("保留逐组计划", editor.description)
        assertEquals(exercise.exerciseId, editedExercise.exerciseId)
        assertEquals(64.0, editedExercise.targetWeightKg ?: 0.0, 0.0)
        assertEquals(4, editedExercise.workingSets)
        assertEquals(1, editedExercise.warmupSets)
        assertEquals(150, editedExercise.restAfterSetSec)
        assertFalse(editedExercise.expandedSetTargets)
        assertTrue(editedExercise.setTargetsSummary.contains("休息150秒"))
        assertEquals(savedPlan.blocks.filterIsInstance<StrengthExerciseBlock>().first().substitutions, editedExercise.substitutions)
        assertEquals(68.0, requireNotNull(overriddenSet.targetWeight).value, 0.0)
        assertEquals(6, (requireNotNull(overriddenSet.repTarget) as RepTarget.Fixed).reps)
        assertEquals("saved-strength", resaved.id)
        assertEquals("2026-06-14T01:00:00Z", resaved.createdAt)
        assertEquals("2026-06-15T01:00:00Z", resaved.updatedAt)
    }

    @Test
    fun editingSavedStrengthPlanKeepsReminderAndPreferences() {
        val savedPlan = buildDefaultStrengthPlanEditorState()
            .toWorkoutPlan(planId = "saved-strength", timestamp = "2026-06-14T01:00:00Z")
            .copy(
                reminder = PlanReminder(
                    enabled = true,
                    scheduleAt = "2026-06-16T11:30:00Z",
                    repeatRule = "weekly"
                ),
                preferences = PlanPreferences(
                    heartRateDisplay = HeartRateDisplayPreference(
                        enabled = false,
                        showDisconnectedPlaceholder = false
                    )
                )
            )

        val editedPlan = savedPlan
            .toStrengthPlanEditorState()
            .updateTitle("力量保留提醒")
            .saveDraftPlan(timestamp = "2026-06-15T01:00:00Z")
            .savedPlan
            .let(::requireNotNull)

        assertEquals(savedPlan.reminder, editedPlan.reminder)
        assertEquals(savedPlan.preferences, editedPlan.preferences)
        assertEquals("weekly", editedPlan.reminder?.repeatRule)
        assertEquals("力量保留提醒", editedPlan.title)
    }

    @Test
    fun editingSavedStrengthPlanUpdatesSamePlanId() {
        val savedPlan = buildDefaultStrengthPlanEditorState().toWorkoutPlan(
            planId = "saved-strength",
            timestamp = "2026-06-14T01:00:00Z"
        )
        val updated = savedPlan
            .toStrengthPlanEditorState()
            .updateTitle("更新后的力量")
            .saveDraftPlan(timestamp = "2026-06-15T01:00:00Z")
        val plan = requireNotNull(updated.savedPlan)

        assertEquals("saved-strength", plan.id)
        assertEquals("更新后的力量", plan.title)
        assertEquals("2026-06-14T01:00:00Z", plan.createdAt)
        assertEquals("2026-06-15T01:00:00Z", plan.updatedAt)
        assertTrue(requireNotNull(updated.statusMessage).contains("已更新"))
    }

    @Test
    fun malformedStrengthPlanBackfillDoesNotCrashAndUsesSafeExercises() {
        val malformed = WorkoutPlan(
            id = "bad-strength",
            mode = WorkoutMode.STRENGTH,
            title = "损坏力量",
            blocks = emptyList(),
            createdAt = "2026-06-14T01:00:00Z",
            updatedAt = "2026-06-14T01:00:00Z"
        )

        val editor = malformed.toStrengthPlanEditorState()

        assertEquals("bad-strength", editor.sourcePlanId)
        assertEquals("损坏力量", editor.title)
        assertTrue(editor.exercises.isNotEmpty())
        assertTrue(editor.canSave)
        assertTrue(requireNotNull(editor.statusMessage).contains("安全默认动作"))
    }

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
    fun createModeStrengthPlanDoesNotInventReminderOrPreferences() {
        val plan = buildDefaultStrengthPlanEditorState().toWorkoutPlan()

        assertNull(plan.reminder)
        assertNull(plan.preferences)
    }

    @Test
    fun newStrengthEditorConsumesStrengthSetTimerModePreferenceDefault() {
        val state = buildDefaultStrengthPlanEditorState(
            defaults = PlanEditorDefaults(
                strengthSetTimerMode = StrengthSetTimerMode.AUTO_AFTER_REST
            )
        ).addExercise("barbell-bench-press")
        val blocks = state.toWorkoutPlan().blocks.filterIsInstance<StrengthExerciseBlock>()

        assertEquals(StrengthSetTimerMode.AUTO_AFTER_REST, state.strengthSetTimerMode)
        assertTrue(blocks.isNotEmpty())
        assertTrue(blocks.all { it.setTimerMode == StrengthSetTimerMode.AUTO_AFTER_REST })
    }

    @Test
    fun changingTrainingPreferenceDefaultsDoesNotRewriteExistingStrengthDraftSetTimerMode() {
        val existingState = buildDefaultStrengthPlanEditorState(
            defaults = PlanEditorDefaults(
                strengthSetTimerMode = StrengthSetTimerMode.MANUAL_START
            )
        ).addExercise("barbell-bench-press")
        val savedBeforePreferenceChange = requireNotNull(existingState.saveDraftPlan().savedPlan)
        val newStateAfterPreferenceChange = buildDefaultStrengthPlanEditorState(
            defaults = PlanEditorDefaults(
                strengthSetTimerMode = StrengthSetTimerMode.AUTO_AFTER_REST
            )
        )
        val existingBlocks = existingState.toWorkoutPlan().blocks.filterIsInstance<StrengthExerciseBlock>()
        val savedBlocks = savedBeforePreferenceChange.blocks.filterIsInstance<StrengthExerciseBlock>()
        val newBlocks = newStateAfterPreferenceChange.toWorkoutPlan().blocks.filterIsInstance<StrengthExerciseBlock>()

        assertTrue(existingBlocks.isNotEmpty())
        assertTrue(existingBlocks.all { it.setTimerMode == StrengthSetTimerMode.MANUAL_START })
        assertTrue(savedBlocks.isNotEmpty())
        assertTrue(savedBlocks.all { it.setTimerMode == StrengthSetTimerMode.MANUAL_START })
        assertTrue(newBlocks.isNotEmpty())
        assertTrue(newBlocks.all { it.setTimerMode == StrengthSetTimerMode.AUTO_AFTER_REST })
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
    fun expandedPerSetTargetCanOverrideRestWithoutChangingSchema() {
        val exercise = buildDefaultStrengthPlanEditorState().exercises.first()
        val firstSetId = exercise.setTargets.first().id
        val state = buildDefaultStrengthPlanEditorState()
            .updateSetRestAfterText(exercise.id, firstSetId, "135")
        val editedExercise = state.exercises.first()
        val block = state.toWorkoutPlan().blocks.filterIsInstance<StrengthExerciseBlock>().first()

        assertTrue(editedExercise.expandedSetTargets)
        assertEquals("135", editedExercise.setTargets.first().restAfterText)
        assertEquals(135, block.sets.first().restAfterSec)
    }

    @Test
    fun strengthNumericFieldsCanBeTemporarilyBlankAndThenReentered() {
        val exercise = buildDefaultStrengthPlanEditorState().exercises.first()
        val blankWeight = buildDefaultStrengthPlanEditorState().updateTargetWeightText(exercise.id, "")
        val blankMinReps = buildDefaultStrengthPlanEditorState().updateRepRangeText(
            exercise.id,
            minRepsInput = "",
            maxRepsInput = exercise.repTarget.maxRepsText
        )
        val blankWorkingSets = buildDefaultStrengthPlanEditorState().updateWorkingSetsText(exercise.id, "")
        val blankRest = buildDefaultStrengthPlanEditorState().updateRestAfterSetText(exercise.id, "")

        assertEquals("", blankWeight.exercises.first().targetWeightText)
        assertFalse(blankWeight.canSave)
        assertTrue(requireNotNull(blankWeight.validationMessage).contains("计划重量"))
        assertFalse(blankMinReps.canStartTraining)
        assertTrue(requireNotNull(blankMinReps.validationMessage).contains("最少次数"))
        assertFalse(blankWorkingSets.canSave)
        assertTrue(requireNotNull(blankWorkingSets.validationMessage).contains("正式组数"))
        assertFalse(blankRest.canSave)
        assertTrue(requireNotNull(blankRest.validationMessage).contains("组间休息秒数"))

        val reentered = blankWeight
            .updateTargetWeightText(exercise.id, "32.5")
            .updateRepRangeText(exercise.id, minRepsInput = "6", maxRepsInput = "10")
            .updateWorkingSetsText(exercise.id, "4")
            .updateWarmupSetsText(exercise.id, "1")
            .updateRestAfterSetText(exercise.id, "120")
        val edited = reentered.exercises.first()

        assertTrue(reentered.canSave)
        assertTrue(reentered.canStartTraining)
        assertEquals(32.5, edited.targetWeightKg ?: 0.0, 0.0)
        assertEquals(6, edited.repTarget.minReps)
        assertEquals(10, edited.repTarget.maxReps)
        assertEquals(4, edited.workingSets)
        assertEquals(1, edited.warmupSets)
        assertEquals(120, edited.restAfterSetSec)
    }

    @Test
    fun visibleRepRangeWithMaxBelowMinDisablesSaveAndStartUntilCorrected() {
        val exercise = buildDefaultStrengthPlanEditorState().exercises.first()
        val invalid = buildDefaultStrengthPlanEditorState()
            .updateRepRangeText(exercise.id, minRepsInput = "12", maxRepsInput = "8")
        val invalidExercise = invalid.exercises.first()
        val failedSave = invalid.saveDraftPlan()

        assertEquals("12", invalidExercise.repTarget.minRepsText)
        assertEquals("8", invalidExercise.repTarget.maxRepsText)
        assertEquals(12, invalidExercise.repTarget.minReps)
        assertEquals(8, invalidExercise.repTarget.maxReps)
        assertFalse(invalid.canSave)
        assertFalse(invalid.canStartTraining)
        assertTrue(requireNotNull(invalid.validationMessage).contains("最大次数不能小于最小次数"))
        assertNull(failedSave.savedPlan)
        assertTrue(requireNotNull(failedSave.statusMessage).contains("最大次数不能小于最小次数"))

        try {
            invalid.toWorkoutPlan()
            throw AssertionError("Invalid visible rep range should not map to a WorkoutPlan.")
        } catch (expected: IllegalArgumentException) {
            assertTrue(requireNotNull(expected.message).contains("最大次数不能小于最小次数"))
        }

        val correctedToEqual = invalid.updateRepRangeText(exercise.id, minRepsInput = "12", maxRepsInput = "12")
        val correctedToRange = invalid.updateRepRangeText(exercise.id, minRepsInput = "12", maxRepsInput = "15")
        val equalRange = requireNotNull(
            correctedToEqual.toWorkoutPlan().blocks.filterIsInstance<StrengthExerciseBlock>().first().target?.repTarget
        ) as RepTarget.Range
        val widerRange = requireNotNull(
            correctedToRange.toWorkoutPlan().blocks.filterIsInstance<StrengthExerciseBlock>().first().target?.repTarget
        ) as RepTarget.Range

        assertTrue(correctedToEqual.canSave)
        assertTrue(correctedToEqual.canStartTraining)
        assertEquals(12, equalRange.minReps)
        assertEquals(12, equalRange.maxReps)
        assertTrue(correctedToRange.canSave)
        assertTrue(correctedToRange.canStartTraining)
        assertEquals(12, widerRange.minReps)
        assertEquals(15, widerRange.maxReps)
    }

    @Test
    fun fixedRepsModeIgnoresStaleInvalidRangeText() {
        val exercise = buildDefaultStrengthPlanEditorState().exercises.first()
        val fixed = buildDefaultStrengthPlanEditorState()
            .updateRepRangeText(exercise.id, minRepsInput = "12", maxRepsInput = "8")
            .updateFixedRepsText(exercise.id, "10")
        val block = fixed.toWorkoutPlan().blocks.filterIsInstance<StrengthExerciseBlock>().first()
        val reps = requireNotNull(block.target?.repTarget) as RepTarget.Fixed

        assertTrue(fixed.canSave)
        assertTrue(fixed.canStartTraining)
        assertEquals(10, reps.reps)
    }

    @Test
    fun fixedAndPerSetStrengthInputsCanBeTemporarilyBlank() {
        val exercise = buildDefaultStrengthPlanEditorState().exercises.first()
        val setId = exercise.setTargets.first().id
        val blankFixedReps = buildDefaultStrengthPlanEditorState()
            .updateFixedRepsText(exercise.id, "")
        val blankSetWeight = buildDefaultStrengthPlanEditorState()
            .setSetTargetsExpanded(exercise.id, true)
            .updateSetTargetWeightText(exercise.id, setId, "")
        val blankSetReps = buildDefaultStrengthPlanEditorState()
            .setSetTargetsExpanded(exercise.id, true)
            .updateSetFixedRepsText(exercise.id, setId, "")

        assertFalse(blankFixedReps.canSave)
        assertTrue(requireNotNull(blankFixedReps.validationMessage).contains("固定次数"))
        assertFalse(blankSetWeight.canSave)
        assertTrue(requireNotNull(blankSetWeight.validationMessage).contains("重量"))
        assertFalse(blankSetReps.canSave)
        assertTrue(requireNotNull(blankSetReps.validationMessage).contains("次数"))

        val reentered = blankSetWeight
            .updateSetTargetWeightText(exercise.id, setId, "18")
            .updateSetFixedRepsText(exercise.id, setId, "9")

        assertTrue(reentered.canSave)
        assertEquals(18.0, reentered.exercises.first().setTargets.first().targetWeightKg ?: 0.0, 0.0)
        assertEquals(9, reentered.exercises.first().setTargets.first().repTarget.fixedReps)
    }

    @Test
    fun strengthEditorStartUsesCurrentValidDraftPlan() {
        val exercise = buildDefaultStrengthPlanEditorState().exercises.first()
        val state = buildDefaultStrengthPlanEditorState()
            .updateTitle("立即开始力量")
            .updateTargetWeightText(exercise.id, "45")
            .updateWorkingSetsText(exercise.id, "2")
        val plan = state.toWorkoutPlan(planId = "plan-strength-editor-start")
        val block = plan.blocks.filterIsInstance<StrengthExerciseBlock>().first()

        assertTrue(state.canStartTraining)
        assertEquals("plan-strength-editor-start", plan.id)
        assertEquals("立即开始力量", plan.title)
        assertEquals(45.0, requireNotNull(block.target?.weight).value, 0.0)
        assertEquals(2, block.sets.size)
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
    fun addCustomStrengthExerciseCreatesPlanLocalStrengthBlock() {
        val initial = buildDefaultStrengthPlanEditorState()
        val added = initial.addCustomExercise("器械推肩")
        val custom = added.exercises.last()
        val block = added.toWorkoutPlan().blocks.filterIsInstance<StrengthExerciseBlock>().last()

        assertEquals(initial.exercises.size + 1, added.exercises.size)
        assertEquals("器械推肩", custom.exerciseName)
        assertTrue(custom.exerciseId.startsWith("custom-strength-"))
        assertEquals("按你的计划完成动作", custom.shortCue)
        assertEquals(3, custom.workingSets)
        assertEquals(90, custom.restAfterSetSec)
        assertEquals(custom.exerciseId, block.exerciseId)
        assertEquals("器械推肩", block.title)
        assertNull(added.savedPlan)
        assertNull(added.statusMessage)
    }

    @Test
    fun addCustomStrengthExerciseKeepsDuplicateNamesUnique() {
        val addedTwice = buildDefaultStrengthPlanEditorState()
            .addCustomExercise("器械推肩")
            .addCustomExercise("器械推肩")
        val customIds = addedTwice.exercises
            .filter { exercise -> exercise.exerciseName == "器械推肩" }
            .map { exercise -> exercise.exerciseId }

        assertEquals(2, customIds.size)
        assertEquals(2, customIds.toSet().size)
    }

    @Test
    fun moveExerciseReordersStrengthTargetsAndPlanBlockOrder() {
        val initial = buildDefaultStrengthPlanEditorState()
        val moved = initial.moveExercise(fromIndex = 1, toIndex = 0)
        val blocks = moved.toWorkoutPlan().blocks.filterIsInstance<StrengthExerciseBlock>()

        assertEquals(
            listOf(initial.exercises[1].exerciseId, initial.exercises[0].exerciseId),
            moved.exercises.map { it.exerciseId }
        )
        assertEquals(listOf(1, 2), blocks.map { it.order })
        assertEquals(initial.exercises[1].exerciseId, blocks.first().exerciseId)
        assertNull(moved.savedPlan)
        assertNull(moved.statusMessage)
    }

    @Test
    fun reorderExercisesCommitsTemporaryDragOrderByIds() {
        val initial = buildDefaultStrengthPlanEditorState()
        val reorderedIds = initial.exercises.map { it.id }.reversed()
        val reordered = initial.saveDraftPlan().reorderExercises(reorderedIds)
        val blocks = reordered.toWorkoutPlan().blocks.filterIsInstance<StrengthExerciseBlock>()

        assertEquals(reorderedIds, reordered.exercises.map { it.id })
        assertEquals(reordered.exercises.map { it.exerciseId }, blocks.map { it.exerciseId })
        assertEquals(listOf(1, 2), blocks.map { it.order })
        assertNull(reordered.savedPlan)
        assertNull(reordered.statusMessage)
    }

    @Test
    fun reorderExercisesIgnoresInvalidTemporaryDragOrder() {
        val initial = buildDefaultStrengthPlanEditorState()
        val initialIds = initial.exercises.map { it.id }

        assertEquals(initialIds, initial.reorderExercises(initialIds.take(1)).exercises.map { it.id })
        assertEquals(initialIds, initial.reorderExercises(listOf(initialIds.first(), "missing")).exercises.map { it.id })
    }

    @Test
    fun saveDraftProducesPersistablePlanForCurrentEditorState() {
        val unsaved = buildDefaultStrengthPlanEditorState()
        val saved = unsaved.saveDraftPlan()
        val editedAfterSave = saved.updateTitle("改名后需要重新保存")

        assertNull(unsaved.savedPlan)
        assertNotNull(saved.savedPlan)
        assertTrue(requireNotNull(saved.statusMessage).contains("本地计划"))
        assertNull(editedAfterSave.savedPlan)
        assertNull(editedAfterSave.statusMessage)
    }
}
