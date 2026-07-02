package com.liujyks.trainflow.feature.plans

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StageStyleIconSemanticsTest {
    @Test
    fun pickerAndTimerDialUseSemanticBuiltInCanvasIconShapes() {
        val pickerSource = File(
            "src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt"
        ).readText(Charsets.UTF_8)
        val timerDialSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDial.kt"
        ).readText(Charsets.UTF_8)

        listOf(
            "drawWarmupFlameIcon",
            "drawWorkActionIcon",
            "drawSpeedUpArrowIcon",
            "drawSprintLightningIcon",
            "drawRestSnowflakeIcon",
            "drawRoundRecoveryIcon",
            "drawCooldownDownshiftIcon"
        ).forEach { helperName ->
            assertTrue("Picker is missing $helperName", pickerSource.contains(helperName))
            assertTrue("TimerDial is missing $helperName", timerDialSource.contains(helperName))
        }
        assertTrue(pickerSource.contains("options.chunked(4)"))
        assertTrue(pickerSource.contains("text = option.label"))
        assertFalse(pickerSource.contains("text = option.key"))
    }

    @Test
    fun iconPickerLabelsDescribeStageMeaningWithoutRawKeys() {
        val picker = buildDefaultTimedCompositionPlanEditorState()
            .stageGroups
            .first()
            .toStageStylePickerUiState()
        val options = picker.iconOptions.associateBy { option -> option.key }

        assertTrue(options.getValue("warmup").contentDescription.contains("火苗"))
        assertTrue(options.getValue("work").contentDescription.contains("动作进行"))
        assertTrue(options.getValue("sprint").contentDescription.contains("闪电"))
        assertTrue(options.getValue("rest").contentDescription.contains("雪花"))
        assertTrue(options.getValue("recover_breathe").contentDescription.contains("循环恢复"))
        assertTrue(options.getValue("cooldown").contentDescription.contains("下行降温"))

        options.values.forEach { option ->
            assertFalse(option.label.contains("_"))
            assertFalse(option.contentDescription.contains(option.key))
        }
    }

    @Test
    fun defaultSprintStageUsesSprintIconKeyForNewDrafts() {
        val sprintStage = buildDefaultTimedCompositionPlanEditorState()
            .stageGroups
            .first { stage -> stage.name == "冲刺组合" }

        assertTrue(sprintStage.iconKey == "sprint")
        assertTrue(sprintStage.targets.single().iconKey == "sprint")
    }
}
