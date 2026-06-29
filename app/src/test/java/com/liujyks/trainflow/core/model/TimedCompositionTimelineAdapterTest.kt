package com.liujyks.trainflow.core.model

import java.lang.reflect.Method
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedCompositionTimelineAdapterTest {
    @Test
    fun expandsPositiveWarmupAndCooldownAsBoundaryWorkSteps() {
        val timeline = TimedCompositionTimelineAdapter.expand(
            compositionBlock(
                warmupSec = 30,
                cooldownSec = 45,
                rounds = 1,
                restBetweenRoundsSec = 0,
                stageGroups = listOf(stageGroup(id = "group", order = 1, targets = listOf(actionTarget())))
            )
        )

        assertEquals(
            listOf(
                TimedCompositionTimelineStageKind.WARMUP,
                TimedCompositionTimelineStageKind.STAGE_GROUP,
                TimedCompositionTimelineStageKind.COOLDOWN
            ),
            timeline.steps.map { step -> step.timelineStageKind }
        )
        assertTrue(timeline.steps.first().isWarmup)
        assertTrue(timeline.steps.last().isCooldown)
        assertTrue(timeline.steps.first().isWork)
        assertEquals(3, timeline.stageInstanceCount)
    }

    @Test
    fun expandsRoundsStageGroupsAndTargetsInNormalizedExecutionOrder() {
        val timeline = TimedCompositionTimelineAdapter.expand(
            compositionBlock(
                warmupSec = 0,
                cooldownSec = 0,
                rounds = 2,
                restBetweenRoundsSec = 0,
                stageGroups = listOf(
                    stageGroup(
                        id = "second-group",
                        order = 20,
                        targets = listOf(
                            restTarget(id = "second-rest", order = 2),
                            actionTarget(id = "second-action", order = 1)
                        )
                    ),
                    stageGroup(
                        id = "first-group",
                        order = 10,
                        targets = listOf(
                            customTarget(id = "first-custom", order = 2),
                            actionTarget(id = "first-action", order = 1)
                        )
                    )
                )
            )
        )

        assertEquals(
            listOf(
                "first-action",
                "first-custom",
                "second-action",
                "second-rest",
                "first-action",
                "first-custom",
                "second-action",
                "second-rest"
            ),
            timeline.steps.map { step -> step.targetId }
        )
        assertEquals(
            listOf(1, 1, 1, 1, 2, 2, 2, 2),
            timeline.steps.map { step -> step.roundIndex }
        )
        assertEquals(
            listOf(1, 1, 2, 2, 1, 1, 2, 2),
            timeline.steps.map { step -> step.stageGroupIndex }
        )
        assertEquals(
            listOf(1, 2, 1, 2, 1, 2, 1, 2),
            timeline.steps.map { step -> step.targetIndex }
        )
    }

    @Test
    fun insertsBetweenRoundRestOnlyBetweenRounds() {
        val timeline = TimedCompositionTimelineAdapter.expand(
            compositionBlock(
                rounds = 3,
                restBetweenRoundsSec = 25,
                stageGroups = listOf(stageGroup(id = "group", order = 1, targets = listOf(actionTarget())))
            )
        )

        val betweenRests = timeline.steps.filter { step -> step.isBetweenRoundRest }

        assertEquals(2, betweenRests.size)
        assertEquals(listOf(1, 2), betweenRests.map { step -> step.roundIndex })
        assertEquals(listOf(25, 25), betweenRests.map { step -> step.plannedDurationSec })
        assertEquals(TimedCompositionTimelineStepKind.REST, betweenRests.first().stepKind)
        assertFalse(timeline.steps.last().isBetweenRoundRest)
    }

    @Test
    fun mapsTargetKindsAndSyntheticBoundariesToStepKinds() {
        val timeline = TimedCompositionTimelineAdapter.expand(
            compositionBlock(
                warmupSec = 10,
                cooldownSec = 12,
                rounds = 2,
                restBetweenRoundsSec = 8,
                stageGroups = listOf(
                    stageGroup(
                        id = "group",
                        order = 1,
                        targets = listOf(
                            actionTarget(id = "action"),
                            customTarget(id = "custom", order = 2),
                            restTarget(id = "rest", order = 3)
                        )
                    )
                )
            )
        )

        val warmup = timeline.steps.single { step -> step.isWarmup }
        val action = timeline.steps.first { step -> step.targetId == "action" }
        val custom = timeline.steps.first { step -> step.targetId == "custom" }
        val rest = timeline.steps.first { step -> step.targetId == "rest" }
        val betweenRoundRest = timeline.steps.single { step -> step.isBetweenRoundRest }
        val cooldown = timeline.steps.single { step -> step.isCooldown }

        assertEquals(TimedCompositionTimelineStepKind.WORK, warmup.stepKind)
        assertEquals(TimedCompositionTimelineStepKind.WORK, action.stepKind)
        assertEquals(TimedCompositionTimelineStepKind.WORK, custom.stepKind)
        assertEquals(TimedCompositionTimelineStepKind.REST, rest.stepKind)
        assertEquals(TimedCompositionTimelineStepKind.REST, betweenRoundRest.stepKind)
        assertEquals(TimedCompositionTimelineStepKind.WORK, cooldown.stepKind)
        assertTrue(action.isWork)
        assertTrue(custom.isWork)
        assertTrue(rest.isRest)
        assertTrue(betweenRoundRest.isRest)
    }

    @Test
    fun skipsZeroDurationWarmupCooldownAndBetweenRoundRest() {
        val timeline = TimedCompositionTimelineAdapter.expand(
            compositionBlock(
                warmupSec = 0,
                cooldownSec = 0,
                rounds = 2,
                restBetweenRoundsSec = 0,
                stageGroups = listOf(stageGroup(id = "group", order = 1, targets = listOf(actionTarget())))
            )
        )

        assertEquals(2, timeline.steps.size)
        assertFalse(timeline.steps.any { step -> step.isWarmup })
        assertFalse(timeline.steps.any { step -> step.isCooldown })
        assertFalse(timeline.steps.any { step -> step.isBetweenRoundRest })
        assertEquals(2, timeline.stageInstanceCount)
    }

    @Test
    fun stageGroupDurationIsDerivedButTimelineUsesTargetDurations() {
        val timeline = TimedCompositionTimelineAdapter.expand(
            compositionBlock(
                rounds = 1,
                stageGroups = listOf(
                    stageGroup(
                        id = "group",
                        order = 1,
                        targets = listOf(
                            actionTarget(id = "work", durationSec = 40),
                            restTarget(id = "rest", durationSec = 20)
                        )
                    )
                )
            )
        )

        assertEquals(60, timeline.steps.sumOf { step -> step.plannedDurationSec })
        assertEquals(listOf(40, 20), timeline.steps.map { step -> step.plannedDurationSec })
        assertFalse(timeline.steps.any { step -> step.plannedDurationSec == 60 && step.targetId == "group" })
    }

    @Test
    fun carriesStableMetadataForRealTargetSteps() {
        val timeline = TimedCompositionTimelineAdapter.expand(
            compositionBlock(
                id = "composition",
                warmupSec = 10,
                cooldownSec = 12,
                rounds = 2,
                restBetweenRoundsSec = 8,
                stageGroups = listOf(
                    stageGroup(
                        id = "group-main",
                        order = 1,
                        colorHex = "#F26B4F",
                        targets = listOf(
                            actionTarget(id = "target-work", order = 1, durationSec = 40, colorHex = "#F26B4F"),
                            restTarget(id = "target-rest", order = 2, durationSec = 20, colorHex = "#2FBF8F")
                        )
                    )
                )
            )
        )

        val step = timeline.steps.first { candidate ->
            candidate.roundIndex == 2 && candidate.targetId == "target-rest"
        }

        assertEquals("composition:r2:g1:group-main:t2:target-rest", step.id)
        assertEquals(TIMED_COMPOSITION_CURRENT_VERSION, step.compositionVersion)
        assertEquals("composition", step.compositionBlockId)
        assertEquals("composition:r2:g1:group-main", step.timelineStageId)
        assertEquals(TimedCompositionTimelineStageKind.STAGE_GROUP, step.timelineStageKind)
        assertEquals(4, step.stageInstanceIndex)
        assertEquals(6, step.targetInstanceIndex)
        assertEquals("group-main", step.stageGroupId)
        assertEquals(TimedCompositionTimelineTargetKind.REST, step.targetKind)
        assertEquals(2, step.roundIndex)
        assertEquals(1, step.stageGroupIndex)
        assertEquals(2, step.targetIndex)
        assertEquals(20, step.plannedDurationSec)
        assertEquals("Rest", step.displayName)
        assertEquals("#2FBF8F", step.colorHex)
        assertTrue(step.isRest)
    }

    @Test
    fun carriesBoundaryStylesAsPassiveStepMetadata() {
        val timeline = TimedCompositionTimelineAdapter.expand(
            compositionBlock(
                warmupSec = 10,
                warmupStyle = TimedStageStyle(colorHex = "#00BCD4", iconKey = "mobility"),
                cooldownSec = 12,
                cooldownStyle = TimedStageStyle(colorHex = "#FFC107", iconKey = "cooldown"),
                rounds = 2,
                restBetweenRoundsSec = 8,
                restBetweenRoundsStyle = TimedStageStyle(colorHex = "#8BC34A", iconKey = "recover_breathe"),
                stageGroups = listOf(stageGroup(id = "group", order = 1, targets = listOf(actionTarget())))
            )
        )

        val warmup = timeline.steps.single { step -> step.isWarmup }
        val betweenRoundRest = timeline.steps.single { step -> step.isBetweenRoundRest }
        val cooldown = timeline.steps.single { step -> step.isCooldown }

        assertEquals("#00BCD4", warmup.colorHex)
        assertEquals("mobility", warmup.iconKey)
        assertEquals("#8BC34A", betweenRoundRest.colorHex)
        assertEquals("recover_breathe", betweenRoundRest.iconKey)
        assertEquals("#FFC107", cooldown.colorHex)
        assertEquals("cooldown", cooldown.iconKey)
        assertEquals(
            listOf(
                "composition:warmup:t1",
                "composition:r1:g1:group:t1:action",
                "composition:r1:between-round-rest:t1",
                "composition:r2:g1:group:t1:action",
                "composition:cooldown:t1"
            ),
            timeline.steps.map { step -> step.id }
        )
        assertEquals(listOf(10, 30, 8, 30, 12), timeline.steps.map { step -> step.plannedDurationSec })
    }

    @Test
    fun targetAndStageInstanceIndexesAreStableAcrossBoundaryAndTargetSteps() {
        val timeline = TimedCompositionTimelineAdapter.expand(
            compositionBlock(
                warmupSec = 10,
                cooldownSec = 12,
                rounds = 2,
                restBetweenRoundsSec = 8,
                stageGroups = listOf(
                    stageGroup(
                        id = "group-a",
                        order = 1,
                        targets = listOf(actionTarget(id = "a1"), restTarget(id = "a2"))
                    ),
                    stageGroup(
                        id = "group-b",
                        order = 2,
                        targets = listOf(customTarget(id = "b1"))
                    )
                )
            )
        )

        assertEquals((1..9).toList(), timeline.steps.map { step -> step.targetInstanceIndex })
        assertEquals(
            listOf(1, 2, 2, 3, 4, 5, 5, 6, 7),
            timeline.steps.map { step -> step.stageInstanceIndex }
        )
        assertEquals(7, timeline.stageInstanceCount)
    }

    @Test
    fun targetOrderNormalizationKeepsTimelineStable() {
        val timeline = TimedCompositionTimelineAdapter.expand(
            compositionBlock(
                rounds = 1,
                stageGroups = listOf(
                    stageGroup(
                        id = "group",
                        order = 1,
                        targets = listOf(
                            restTarget(id = "third", order = 30),
                            customTarget(id = "second", order = 20),
                            actionTarget(id = "first", order = 10)
                        )
                    )
                )
            )
        )

        assertEquals(listOf("first", "second", "third"), timeline.steps.map { step -> step.targetId })
        assertEquals(listOf(1, 2, 3), timeline.steps.map { step -> step.targetIndex })
    }

    @Test
    fun maxFiveTargetsRemainGuaranteedByModelNormalization() {
        val timeline = TimedCompositionTimelineAdapter.expand(
            compositionBlock(
                rounds = 1,
                stageGroups = listOf(
                    stageGroup(
                        id = "group",
                        order = 1,
                        targets = (1..6).map { index ->
                            actionTarget(id = "target-$index", order = index, durationSec = 10)
                        }
                    )
                )
            )
        )

        assertEquals(TIMED_COMPOSITION_MAX_TARGETS_PER_STAGE_GROUP, timeline.steps.size)
        assertEquals(
            listOf("target-1", "target-2", "target-3", "target-4", "target-5"),
            timeline.steps.map { step -> step.targetId }
        )
    }

    @Test
    fun unsupportedCompositionVersionFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            TimedCompositionTimelineAdapter.expand(
                compositionBlock(
                    compositionVersion = TIMED_COMPOSITION_CURRENT_VERSION + 1,
                    stageGroups = listOf(stageGroup(id = "group", order = 1, targets = listOf(actionTarget())))
                )
            )
        }
    }

    @Test
    fun legacyTimedBlocksAreNotAdapterInputs() {
        val expandMethods = TimedCompositionTimelineAdapter::class.java.methods
            .filter { method -> method.name == "expand" }

        assertTrue(expandMethods.isNotEmpty())
        assertTrue(expandMethods.all { method -> method.hasOnlyCompositionBlockParameter() })
        assertFalse(expandMethods.any { method ->
            method.parameterTypes.any { parameter ->
                parameter == TimedCircuitBlock::class.java || parameter == PlanBlock::class.java
            }
        })
    }

    private fun Method.hasOnlyCompositionBlockParameter(): Boolean {
        return parameterTypes.toList() == listOf(TimedCompositionBlock::class.java)
    }

    private fun compositionBlock(
        id: String = "composition",
        compositionVersion: Int = TIMED_COMPOSITION_CURRENT_VERSION,
        warmupSec: Int = 0,
        warmupStyle: TimedStageStyle? = null,
        cooldownSec: Int = 0,
        cooldownStyle: TimedStageStyle? = null,
        rounds: Int = 1,
        restBetweenRoundsSec: Int = 0,
        restBetweenRoundsStyle: TimedStageStyle? = null,
        stageGroups: List<TimedCompositionStageGroup>
    ): TimedCompositionBlock {
        return TimedCompositionBlock(
            id = id,
            order = 1,
            title = "Composition",
            compositionVersion = compositionVersion,
            warmupSec = warmupSec,
            warmupStyle = warmupStyle,
            cooldownSec = cooldownSec,
            cooldownStyle = cooldownStyle,
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec,
            restBetweenRoundsStyle = restBetweenRoundsStyle,
            stageGroups = stageGroups
        )
    }

    private fun stageGroup(
        id: String,
        order: Int,
        colorHex: String = TimedStageType.WORK.defaultColorHex,
        targets: List<TimedCompositionTarget>
    ): TimedCompositionStageGroup {
        return TimedCompositionStageGroup(
            id = id,
            order = order,
            name = id,
            colorHex = colorHex,
            targets = targets
        )
    }

    private fun actionTarget(
        id: String = "action",
        order: Int = 1,
        durationSec: Int = 30,
        colorHex: String = TimedStageType.WORK.defaultColorHex
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = "Action",
            kind = TimedCompositionTargetKind.ACTION,
            durationSec = durationSec,
            colorHex = colorHex
        )
    }

    private fun restTarget(
        id: String = "rest",
        order: Int = 2,
        durationSec: Int = 15,
        colorHex: String = TimedStageType.REST.defaultColorHex
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = "Rest",
            kind = TimedCompositionTargetKind.REST,
            durationSec = durationSec,
            colorHex = colorHex
        )
    }

    private fun customTarget(
        id: String = "custom",
        order: Int = 1,
        durationSec: Int = 25,
        colorHex: String = TimedStageType.CUSTOM.defaultColorHex
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = "Custom",
            kind = TimedCompositionTargetKind.CUSTOM,
            durationSec = durationSec,
            colorHex = colorHex
        )
    }

    private fun target(
        id: String,
        order: Int,
        name: String,
        kind: TimedCompositionTargetKind,
        durationSec: Int,
        colorHex: String
    ): TimedCompositionTarget {
        return TimedCompositionTarget(
            id = id,
            order = order,
            name = name,
            kind = kind,
            durationSec = durationSec,
            colorHex = colorHex
        )
    }
}
