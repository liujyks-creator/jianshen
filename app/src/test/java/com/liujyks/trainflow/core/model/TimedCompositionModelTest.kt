package com.liujyks.trainflow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TimedCompositionModelTest {
    @Test
    fun normalizationReordersClampsAndCapsTargetsPerStageGroup() {
        val block = TimedCompositionBlock(
            id = "composition",
            order = 3,
            compositionVersion = 99,
            warmupSec = -10,
            warmupStyle = TimedStageStyle(colorHex = "#f2b84b", iconKey = "warmup"),
            cooldownSec = 45,
            cooldownStyle = TimedStageStyle(colorHex = "bad", iconKey = "moon.svg"),
            rounds = 0,
            restBetweenRoundsSec = -5,
            restBetweenRoundsStyle = TimedStageStyle(colorHex = "#2fbf8f", iconKey = "recover_breathe"),
            stageGroups = listOf(
                stageGroup(id = "later", order = 20, targetCount = 6),
                stageGroup(id = "earlier", order = 10, targetCount = 2, colorHex = "not-a-color")
            )
        ).normalized()

        assertEquals(TIMED_COMPOSITION_CURRENT_VERSION, block.compositionVersion)
        assertEquals(0, block.warmupSec)
        assertEquals(TimedStageStyle(colorHex = "#F2B84B", iconKey = "warmup"), block.warmupStyle)
        assertEquals(45, block.cooldownSec)
        assertNull(block.cooldownStyle)
        assertEquals(1, block.rounds)
        assertEquals(0, block.restBetweenRoundsSec)
        assertEquals(
            TimedStageStyle(colorHex = "#2FBF8F", iconKey = "recover_breathe"),
            block.restBetweenRoundsStyle
        )
        assertEquals(listOf("earlier", "later"), block.stageGroups.map { group -> group.id })
        assertEquals(listOf(1, 2), block.stageGroups.map { group -> group.order })
        assertEquals(TimedStageType.WORK.defaultColorHex, block.stageGroups.first().colorHex)
        assertEquals(TIMED_COMPOSITION_MAX_TARGETS_PER_STAGE_GROUP, block.stageGroups[1].targets.size)
        assertEquals(listOf(1, 2, 3, 4, 5), block.stageGroups[1].targets.map { target -> target.order })
    }

    @Test
    fun stageGroupDurationIsDerivedFromTargetsAndZeroDurationGroupsAreDropped() {
        val block = TimedCompositionBlock(
            id = "composition",
            order = 1,
            warmupSec = 30,
            cooldownSec = 20,
            rounds = 2,
            restBetweenRoundsSec = 15,
            stageGroups = listOf(
                TimedCompositionStageGroup(
                    id = "empty-after-normalize",
                    order = 1,
                    name = "Empty",
                    colorHex = TimedStageType.WORK.defaultColorHex,
                    targets = listOf(
                        target(id = "zero", order = 1, durationSec = 0),
                        target(id = "negative", order = 2, durationSec = -30)
                    )
                ),
                TimedCompositionStageGroup(
                    id = "main",
                    order = 2,
                    name = "Main",
                    colorHex = TimedStageType.WORK.defaultColorHex,
                    targets = listOf(
                        target(id = "work", order = 1, durationSec = 40),
                        target(id = "rest", order = 2, kind = TimedCompositionTargetKind.REST, durationSec = 20)
                    )
                )
            )
        ).normalized()

        val group = block.stageGroups.single()

        assertEquals("main", group.id)
        assertEquals(60, group.durationSec)
        assertEquals(60, block.derivedRepeatedStageDurationSec("main"))
        assertEquals(30 + 20 + 60 * 2 + 15, block.warmupSec + block.cooldownSec + group.durationSec * block.rounds + block.restBetweenRoundsSec)
        assertNotNull(group.targets.first().compatibility ?: true)
    }

    @Test
    fun stageGroupAndTargetIconKeysNormalizeToBuiltInContract() {
        val block = TimedCompositionBlock(
            id = "composition",
            order = 1,
            rounds = 1,
            stageGroups = listOf(
                TimedCompositionStageGroup(
                    id = "main",
                    order = 1,
                    name = "Main",
                    colorHex = TimedStageType.WORK.defaultColorHex,
                    iconKey = "sprint",
                    targets = listOf(
                        target(
                            id = "valid-icon",
                            order = 1,
                            durationSec = 30,
                            iconKey = "speed_up"
                        ),
                        target(
                            id = "asset-icon",
                            order = 2,
                            durationSec = 15,
                            iconKey = "icons/rest.svg"
                        )
                    )
                ),
                TimedCompositionStageGroup(
                    id = "bad-icon-group",
                    order = 2,
                    name = "Bad Icon",
                    colorHex = TimedStageType.REST.defaultColorHex,
                    iconKey = "uploaded_asset_1",
                    targets = listOf(
                        target(id = "rest", order = 1, kind = TimedCompositionTargetKind.REST, durationSec = 20)
                    )
                )
            )
        ).normalized()

        assertEquals("sprint", block.stageGroups[0].iconKey)
        assertEquals("speed_up", block.stageGroups[0].targets[0].iconKey)
        assertNull(block.stageGroups[0].targets[1].iconKey)
        assertNull(block.stageGroups[1].iconKey)
    }

    private fun stageGroup(
        id: String,
        order: Int,
        targetCount: Int,
        colorHex: String = TimedStageType.WORK.defaultColorHex
    ): TimedCompositionStageGroup {
        return TimedCompositionStageGroup(
            id = id,
            order = order,
            name = id,
            colorHex = colorHex,
            targets = (targetCount downTo 1).map { index ->
                target(id = "$id-target-$index", order = index, durationSec = index * 10)
            }
        )
    }

    private fun target(
        id: String,
        order: Int,
        kind: TimedCompositionTargetKind = TimedCompositionTargetKind.ACTION,
        durationSec: Int,
        iconKey: String? = null
    ): TimedCompositionTarget {
        return TimedCompositionTarget(
            id = id,
            order = order,
            name = id,
            kind = kind,
            durationSec = durationSec,
            colorHex = TimedStageType.WORK.defaultColorHex,
            iconKey = iconKey,
            compatibility = TimedCompositionCompatibilityMeta(
                sourceVersion = TimedCompositionCompatibilitySourceVersion.V2
            )
        )
    }
}
