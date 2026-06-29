package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageStyle
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedCompositionEditorDraftAdapterTest {
    @Test
    fun legacyTimedPlanWrapsAsDraftWithoutChangingSourcePlan() {
        val plan = legacyPlan()
        val draft = plan.toTimedCompositionEditorDraft()
        val preserved = draft.toWorkoutPlan()

        assertEquals(TimedCompositionEditorDraftSource.LEGACY_TIMED, draft.source)
        assertTrue(draft.requiresExplicitConversionForV2)
        assertEquals(60, draft.warmupSec)
        assertEquals(45, draft.cooldownSec)
        assertEquals(3, draft.rounds)
        assertEquals(30, draft.restBetweenRoundsSec)
        assertNull(draft.warmupStyle)
        assertNull(draft.cooldownStyle)
        assertNull(draft.restBetweenRoundsStyle)
        assertEquals(listOf("legacy-circuit-work", "legacy-circuit-custom"), draft.stageGroups.map { group -> group.id })
        assertEquals(listOf(TimedCompositionTargetKind.ACTION, TimedCompositionTargetKind.REST), draft.stageGroups.first().targets.map { target -> target.kind })
        assertEquals(60, draft.stageGroups.first().durationSec)
        assertSame(plan, preserved)
        assertFalse(preserved.blocks.any { block -> block is TimedCompositionBlock })
    }

    @Test
    fun legacyDraftExportsV2OnlyWhenExplicitlyRequested() {
        val exported = legacyPlan()
            .toTimedCompositionEditorDraft()
            .toWorkoutPlan(
                exportMode = TimedCompositionEditorDraftExportMode.EXPORT_V2_PAYLOAD,
                timestamp = "2026-06-26T12:00:00Z"
            )
        val block = exported.blocks.single() as TimedCompositionBlock

        assertEquals(WorkoutMode.TIMED, exported.mode)
        assertEquals("legacy-plan-timed-composition", block.id)
        assertEquals(2, block.compositionVersion)
        assertEquals(60, block.warmupSec)
        assertEquals(45, block.cooldownSec)
        assertEquals(3, block.rounds)
        assertEquals(30, block.restBetweenRoundsSec)
        assertEquals(2, block.stageGroups.size)
        assertEquals("legacy_timed_circuit", block.compatibility?.sourceVersion?.contractValue)
        assertEquals("2026-06-26T12:00:00Z", block.compatibility?.convertedAt)
    }

    @Test
    fun v2PlanWrapsAsDraftAndPreserveSourceKeepsOriginalPayload() {
        val plan = WorkoutPlan(
            id = "v2-plan",
            mode = WorkoutMode.TIMED,
            title = "V2 Plan",
            blocks = listOf(
                TimedCompositionBlock(
                    id = "composition",
                    order = 1,
                    warmupSec = 20,
                    warmupStyle = TimedStageStyle(colorHex = "#F2B84B", iconKey = "warmup"),
                    cooldownSec = 25,
                    cooldownStyle = TimedStageStyle(colorHex = "#65A9FF", iconKey = "cooldown"),
                    rounds = 2,
                    restBetweenRoundsSec = 10,
                    restBetweenRoundsStyle = TimedStageStyle(
                        colorHex = "#2FBF8F",
                        iconKey = "recover_breathe"
                    ),
                    stageGroups = listOf(
                        com.liujyks.trainflow.core.model.TimedCompositionStageGroup(
                            id = "group",
                            order = 1,
                            name = "Group",
                            colorHex = TimedStageType.WORK.defaultColorHex,
                            targets = listOf(
                                com.liujyks.trainflow.core.model.TimedCompositionTarget(
                                    id = "target",
                                    order = 1,
                                    name = "Target",
                                    kind = TimedCompositionTargetKind.ACTION,
                                    durationSec = 30,
                                    colorHex = TimedStageType.WORK.defaultColorHex
                                )
                            )
                        )
                    )
                )
            ),
            createdAt = "2026-06-26T09:00:00Z",
            updatedAt = "2026-06-26T09:00:00Z"
        )

        val draft = plan.toTimedCompositionEditorDraft()
        val preserved = draft.toWorkoutPlan()

        assertEquals(TimedCompositionEditorDraftSource.V2_PAYLOAD, draft.source)
        assertFalse(draft.requiresExplicitConversionForV2)
        assertEquals(115, draft.estimatedDurationSec)
        assertEquals(TimedStageStyle(colorHex = "#F2B84B", iconKey = "warmup"), draft.warmupStyle)
        assertEquals(TimedStageStyle(colorHex = "#65A9FF", iconKey = "cooldown"), draft.cooldownStyle)
        assertEquals(
            TimedStageStyle(colorHex = "#2FBF8F", iconKey = "recover_breathe"),
            draft.restBetweenRoundsStyle
        )
        assertSame(plan, preserved)
        assertTrue(preserved.blocks.single() is TimedCompositionBlock)

        val exportedBlock = draft.toWorkoutPlan(
            exportMode = TimedCompositionEditorDraftExportMode.EXPORT_V2_PAYLOAD,
            timestamp = "2026-06-28T01:00:00Z"
        ).blocks.single() as TimedCompositionBlock
        assertEquals(TimedStageStyle(colorHex = "#F2B84B", iconKey = "warmup"), exportedBlock.warmupStyle)
        assertEquals(TimedStageStyle(colorHex = "#65A9FF", iconKey = "cooldown"), exportedBlock.cooldownStyle)
        assertEquals(
            TimedStageStyle(colorHex = "#2FBF8F", iconKey = "recover_breathe"),
            exportedBlock.restBetweenRoundsStyle
        )
    }

    private fun legacyPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "legacy-plan",
            mode = WorkoutMode.TIMED,
            title = "Legacy Plan",
            description = "Legacy description",
            blocks = listOf(
                WarmupBlock(
                    id = "warmup",
                    order = 1,
                    durationSec = 60
                ),
                TimedCircuitBlock(
                    id = "legacy-circuit",
                    order = 2,
                    rounds = 3,
                    restBetweenRoundsSec = 30,
                    items = listOf(
                        TimedExerciseItem(
                            id = "work",
                            labelOverride = "Work",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 40,
                            restAfterSec = 20
                        ),
                        TimedExerciseItem(
                            id = "custom",
                            labelOverride = "Custom",
                            stageType = TimedStageType.CUSTOM,
                            workDurationSec = 35
                        )
                    )
                ),
                CooldownBlock(
                    id = "cooldown",
                    order = 3,
                    durationSec = 45
                )
            ),
            createdAt = "2026-06-26T09:00:00Z",
            updatedAt = "2026-06-26T09:00:00Z"
        )
    }
}
