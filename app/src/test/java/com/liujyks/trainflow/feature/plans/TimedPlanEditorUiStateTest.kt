package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedPlanEditorUiStateTest {
    @Test
    fun savedTimedPlanBackfillsTitleDescriptionStagesRoundsRestAndCues() {
        val savedPlan = buildDefaultTimedPlanEditorState()
            .updateTitle("晚间间歇")
            .updateDescription("保留阶段颜色和提醒")
            .updateRounds(5)
            .updateRestBetweenRounds(75)
            .updateActionCueThreshold(6)
            .updateRestCueThreshold(3)
            .updateSoundEnabled(false)
            .let { state ->
                val warmupId = state.stages.first { it.stageType == TimedStageType.WARMUP }.id
                val workId = state.stages.first { it.stageType == TimedStageType.WORK }.id
                state
                    .updateStageColor(warmupId, "#00BCD4")
                    .updateStageColor(workId, "#FFC107")
                    .copy(
                        stages = state
                            .updateStageColor(warmupId, "#00BCD4")
                            .updateStageColor(workId, "#FFC107")
                            .stages
                            .map { stage ->
                                when (stage.id) {
                                    warmupId -> stage.copy(iconKey = "mobility")
                                    workId -> stage.copy(iconKey = "bolt")
                                    else -> stage
                                }
                            }
                    )
            }
            .toWorkoutPlan(planId = "saved-timed", timestamp = "2026-06-14T01:00:00Z")

        val editor = savedPlan.toTimedPlanEditorState()
        val resavedPlan = editor.toWorkoutPlan(timestamp = "2026-06-15T01:00:00Z")
        val circuit = resavedPlan.blocks.filterIsInstance<TimedCircuitBlock>().single()
        val warmup = resavedPlan.blocks.filterIsInstance<WarmupBlock>().single().items.single()

        assertEquals("saved-timed", editor.sourcePlanId)
        assertEquals("晚间间歇", editor.title)
        assertEquals("保留阶段颜色和提醒", editor.description)
        assertEquals(5, editor.rounds)
        assertEquals(75, editor.restBetweenRoundsSec)
        assertEquals(listOf(TimedStageType.WARMUP, TimedStageType.WORK, TimedStageType.REST, TimedStageType.CUSTOM, TimedStageType.COOLDOWN), editor.stages.map { it.stageType })
        assertEquals("mobility", warmup.iconKey)
        assertEquals("#00BCD4", warmup.colorHex)
        assertEquals("bolt", circuit.items.first().iconKey)
        assertEquals("#FFC107", circuit.items.first().colorHex)
        assertEquals(6, resavedPlan.preferences?.cueSettings?.actionEnding?.thresholdSec)
        assertEquals(3, resavedPlan.preferences?.cueSettings?.restEnding?.thresholdSec)
        assertFalse(requireNotNull(resavedPlan.preferences?.cueSettings?.actionEnding).soundEnabled)
    }

    @Test
    fun editingSavedTimedPlanUpdatesSamePlanIdAndKeepsCreatedAt() {
        val savedPlan = buildDefaultTimedPlanEditorState().toWorkoutPlan(
            planId = "saved-timed",
            timestamp = "2026-06-14T01:00:00Z"
        )
        val updated = savedPlan
            .toTimedPlanEditorState()
            .updateTitle("更新后的计时")
            .saveDraftPlan(timestamp = "2026-06-15T01:00:00Z")
        val plan = requireNotNull(updated.savedPlan)

        assertEquals("saved-timed", plan.id)
        assertEquals("更新后的计时", plan.title)
        assertEquals("2026-06-14T01:00:00Z", plan.createdAt)
        assertEquals("2026-06-15T01:00:00Z", plan.updatedAt)
        assertTrue(requireNotNull(updated.statusMessage).contains("已更新"))
    }

    @Test
    fun malformedTimedPlanBackfillDoesNotCrashAndUsesSafeStages() {
        val malformed = WorkoutPlan(
            id = "bad-timed",
            mode = WorkoutMode.TIMED,
            title = "损坏计时",
            blocks = emptyList(),
            createdAt = "2026-06-14T01:00:00Z",
            updatedAt = "2026-06-14T01:00:00Z"
        )

        val editor = malformed.toTimedPlanEditorState()

        assertEquals("bad-timed", editor.sourcePlanId)
        assertEquals("损坏计时", editor.title)
        assertTrue(editor.stages.isNotEmpty())
        assertTrue(editor.canSave)
        assertTrue(requireNotNull(editor.statusMessage).contains("安全默认阶段"))
    }

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
    fun stageAddCopyDeleteAndFallbackSortWork() {
        val initial = buildDefaultTimedPlanEditorState()
        val added = initial.addStage(TimedStageType.REST)
        val workId = added.stages.first { stage -> stage.stageType == TimedStageType.WORK }.id
        val copied = added.copyStage(workId)
        val copiedId = copied.stages[2].id
        val movedDown = copied.moveStageDown(copiedId)
        val movedUp = movedDown.moveStageUp(copiedId)
        val removed = movedUp.removeStage(copiedId)

        assertEquals(initial.stages.size + 1, added.stages.size)
        assertEquals(added.stages.size + 1, copied.stages.size)
        assertTrue(copied.stages[2].name.contains("副本"))
        assertEquals(copiedId, movedDown.stages[3].id)
        assertEquals(copiedId, movedUp.stages[2].id)
        assertEquals(movedUp.stages.size - 1, removed.stages.size)
    }

    @Test
    fun addDeleteAddUsesStableNonReusedStageIds() {
        val firstAdded = buildDefaultTimedPlanEditorState().addStage(TimedStageType.WORK)
        val firstAddedId = firstAdded.stages.first { stage -> stage.id.startsWith("stage-added-") }.id
        val removed = firstAdded.removeStage(firstAddedId)
        val secondAdded = removed.addStage(TimedStageType.WORK)
        val secondAddedId = secondAdded.stages.last { stage -> stage.id.startsWith("stage-added-") }.id

        assertNotEquals(firstAddedId, secondAddedId)
        assertEquals(secondAdded.stages.size, secondAdded.stages.map { it.id }.toSet().size)
    }

    @Test
    fun copyStageUsesUniqueIdsAcrossRepeatedCopies() {
        val workId = buildDefaultTimedPlanEditorState().stages.first { it.stageType == TimedStageType.WORK }.id
        val copiedTwice = buildDefaultTimedPlanEditorState()
            .copyStage(workId)
            .copyStage(workId)

        assertEquals(copiedTwice.stages.size, copiedTwice.stages.map { it.id }.toSet().size)
        assertEquals(2, copiedTwice.stages.count { stage -> stage.id.startsWith("$workId-copy-") })
    }

    @Test
    fun moveStageMovesFromTopToLowerIndex() {
        val state = buildDefaultTimedPlanEditorState()
        val moved = state.moveStage(fromIndex = 1, toIndex = 3)

        assertEquals(
            listOf("stage-warmup", "stage-rest", "stage-custom", "stage-work", "stage-cooldown"),
            moved.stages.map { it.id }
        )
    }

    @Test
    fun moveStageMovesFromBottomToUpperIndex() {
        val state = buildDefaultTimedPlanEditorState()
        val moved = state.moveStage(fromIndex = 3, toIndex = 1)

        assertEquals(
            listOf("stage-warmup", "stage-custom", "stage-work", "stage-rest", "stage-cooldown"),
            moved.stages.map { it.id }
        )
    }

    @Test
    fun moveStageIgnoresOutOfRangeIndexesWithoutChangingList() {
        val state = buildDefaultTimedPlanEditorState()
        val initialStageIds = state.stages.map { it.id }

        assertEquals(initialStageIds, state.moveStage(fromIndex = -1, toIndex = 2).stages.map { it.id })
        assertEquals(initialStageIds, state.moveStage(fromIndex = 2, toIndex = -1).stages.map { it.id })
        assertEquals(initialStageIds, state.moveStage(fromIndex = 0, toIndex = state.stages.size).stages.map { it.id })
    }

    @Test
    fun moveStageKeepsWarmupAndCooldownAsFixedBoundaries() {
        val state = buildDefaultTimedPlanEditorState()
        val initialStageIds = state.stages.map { it.id }

        assertEquals(initialStageIds, state.moveStage(fromIndex = 0, toIndex = 2).stages.map { it.id })
        assertEquals(initialStageIds, state.moveStage(fromIndex = 4, toIndex = 2).stages.map { it.id })
        assertFalse(state.canMoveStageDown("stage-warmup"))
        assertFalse(state.canMoveStageUp("stage-cooldown"))
    }

    @Test
    fun addingBoundaryStagesKeepsEditorOrderAlignedWithExecutionOrder() {
        val state = buildDefaultTimedPlanEditorState()
            .addStage(TimedStageType.COOLDOWN)
            .addStage(TimedStageType.WARMUP)
            .addStage(TimedStageType.WORK)

        assertEquals(TimedStageType.WARMUP, state.stages[0].stageType)
        assertEquals(TimedStageType.WARMUP, state.stages[1].stageType)
        assertEquals(TimedStageType.COOLDOWN, state.stages[state.stages.lastIndex - 1].stageType)
        assertEquals(TimedStageType.COOLDOWN, state.stages.last().stageType)
        assertEquals(
            state.stages.filterNot { stage ->
                stage.stageType == TimedStageType.WARMUP || stage.stageType == TimedStageType.COOLDOWN
            }.map { it.id },
            state.toWorkoutPlan().blocks.filterIsInstance<TimedCircuitBlock>().single().items.map { it.id }
        )
    }

    @Test
    fun moveStageKeepsStageFieldsAssociatedAfterReorder() {
        val workId = buildDefaultTimedPlanEditorState().stages.first { it.stageType == TimedStageType.WORK }.id
        val state = buildDefaultTimedPlanEditorState()
            .updateStageName(workId, "冲刺")
            .updateStageDuration(workId, 55)
            .updateStageType(workId, TimedStageType.CUSTOM)
            .updateStageColor(workId, "#123456")
        val moved = state.moveStage(fromIndex = 1, toIndex = 3)
        val movedStage = moved.stages[3]
        val circuit = moved.toWorkoutPlan().blocks.filterIsInstance<TimedCircuitBlock>().single()

        assertEquals(workId, movedStage.id)
        assertEquals("冲刺", movedStage.name)
        assertEquals(55, movedStage.durationSec)
        assertEquals("#123456", movedStage.colorHex)
        assertEquals(TimedStageType.CUSTOM.defaultIconKey, movedStage.iconKey)
        assertEquals(workId, circuit.items.last().id)
        assertEquals("冲刺", circuit.items.last().labelOverride)
        assertEquals(55, circuit.items.last().workDurationSec)
        assertEquals("#123456", circuit.items.last().colorHex)
        assertEquals(TimedStageType.CUSTOM.defaultIconKey, circuit.items.last().iconKey)
    }

    @Test
    fun stageColorPickerStateSeparatesRecommendedMoreAndSelectedSemantics() {
        val workStage = buildDefaultTimedPlanEditorState()
            .updateStageColor("stage-work", "#FFC107")
            .stages
            .first { it.id == "stage-work" }
        val picker = workStage.toStageColorPickerUiState()
        val selected = picker.moreColors.single { option -> option.hex == "#FFC107" }

        assertEquals("#FFC107", picker.selectedColorHex)
        assertEquals("琥珀", picker.selectedColorName)
        assertTrue(picker.recommendedColors.size in 5..8)
        assertTrue(picker.moreColors.size >= 20)
        assertTrue(selected.selected)
        assertTrue(selected.hasCheckIndicator)
        assertTrue(selected.contentDescription.contains("已选中"))
        assertTrue(selected.contentDescription.contains("适合提醒 / 明亮"))
    }

    @Test
    fun stageColorSelectionUpdatesCurrentStageAndPlanMapping() {
        val workId = buildDefaultTimedPlanEditorState().stages.first { it.stageType == TimedStageType.WORK }.id
        val state = buildDefaultTimedPlanEditorState().updateStageColor(workId, "#00BCD4")
        val stage = state.stages.first { it.id == workId }
        val item = state.toWorkoutPlan().blocks.filterIsInstance<TimedCircuitBlock>().single()
            .items
            .first { it.id == workId }

        assertEquals("#00BCD4", stage.colorHex)
        assertEquals("#00BCD4", item.colorHex)
    }

    @Test
    fun warmupStageColorAndIconPersistToBoundaryBlockItem() {
        val baseState = buildDefaultTimedPlanEditorState()
        val warmupId = baseState.stages.first { it.stageType == TimedStageType.WARMUP }.id
        val colorState = baseState.updateStageColor(warmupId, "#00BCD4")
        val state = colorState.copy(
            stages = colorState.stages.map { stage ->
                if (stage.id == warmupId) stage.copy(iconKey = "mobility") else stage
            }
        )
        val block = state.toWorkoutPlan().blocks.filterIsInstance<WarmupBlock>().single()
        val item = block.items.single()

        assertEquals(180, block.durationSec)
        assertEquals(warmupId, item.id)
        assertEquals("热身", item.labelOverride)
        assertEquals(TimedStageType.WARMUP, item.stageType)
        assertEquals("mobility", item.iconKey)
        assertEquals("#00BCD4", item.colorHex)
        assertEquals(180, item.workDurationSec)
    }

    @Test
    fun cooldownStageColorAndIconPersistToBoundaryBlockItem() {
        val baseState = buildDefaultTimedPlanEditorState()
        val cooldownId = baseState.stages.first { it.stageType == TimedStageType.COOLDOWN }.id
        val colorState = baseState.updateStageColor(cooldownId, "#FFC107")
        val state = colorState.copy(
            stages = colorState.stages.map { stage ->
                if (stage.id == cooldownId) stage.copy(iconKey = "moon") else stage
            }
        )
        val block = state.toWorkoutPlan().blocks.filterIsInstance<CooldownBlock>().single()
        val item = block.items.single()

        assertEquals(120, block.durationSec)
        assertEquals(cooldownId, item.id)
        assertEquals("放松", item.labelOverride)
        assertEquals(TimedStageType.COOLDOWN, item.stageType)
        assertEquals("moon", item.iconKey)
        assertEquals("#FFC107", item.colorHex)
        assertEquals(120, item.workDurationSec)
    }

    @Test
    fun invalidStageColorFallsBackToCurrentStageDefault() {
        val restId = buildDefaultTimedPlanEditorState().stages.first { it.stageType == TimedStageType.REST }.id
        val state = buildDefaultTimedPlanEditorState().updateStageColor(restId, "bad-color")
        val stage = state.stages.first { it.id == restId }
        val item = state.toWorkoutPlan().blocks.filterIsInstance<TimedCircuitBlock>().single()
            .items
            .first { it.id == restId }

        assertEquals(TimedStageType.REST.defaultColorHex, stage.colorHex)
        assertEquals(TimedStageType.REST.defaultColorHex, item.colorHex)
    }

    @Test
    fun updateStageColorAndTypeKeepIconColorAndPlanMapping() {
        val workId = buildDefaultTimedPlanEditorState().stages.first { it.stageType == TimedStageType.WORK }.id
        val state = buildDefaultTimedPlanEditorState()
            .updateStageColor(workId, "#8B6CFF")
            .updateStageType(workId, TimedStageType.CUSTOM)
        val stage = state.stages.first { it.id == workId }
        val item = state.toWorkoutPlan().blocks.filterIsInstance<TimedCircuitBlock>().single()
            .items
            .first { it.id == workId }

        assertEquals(TimedStageType.CUSTOM, stage.stageType)
        assertEquals(TimedStageType.CUSTOM.defaultIconKey, stage.iconKey)
        assertEquals(TimedStageType.CUSTOM.defaultColorHex, stage.colorHex)
        assertEquals(TimedStageType.CUSTOM.defaultIconKey, item.iconKey)
        assertEquals(TimedStageType.CUSTOM.defaultColorHex, item.colorHex)
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
    fun saveDraftProducesPersistablePlanForCurrentEditorState() {
        val unsaved = buildDefaultTimedPlanEditorState()
        val saved = unsaved.saveDraftPlan()
        val editedAfterSave = saved.updateTitle("改名后需要重新保存")

        assertNull(unsaved.savedPlan)
        assertNotNull(saved.savedPlan)
        assertTrue(requireNotNull(saved.statusMessage).contains("本地计划"))
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
