package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.model.TIMED_COMPOSITION_CURRENT_VERSION
import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.FollowAlongPlanMeta
import com.liujyks.trainflow.core.model.HeartRateDisplayPreference
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.RestBlock
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthExerciseTarget
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionStageGroup
import com.liujyks.trainflow.core.model.TimedCompositionTarget
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanSnapshotStorageV1ValidatorTest {
    @Test
    fun writerEmitsLiteralVersionAndAllSevenRootKeysInStableOrder() {
        val json = emptySnapshot().toStorageJson()

        assertEquals(
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"训练标题\",\"mode\":\"timed\",\"blocks\":[],\"preferences\":null,\"followAlong\":null}",
            json
        )
        val result = PlanSnapshotStorageV1Validator.validate(json, WorkoutMode.TIMED)
        assertTrue(result is PlanSnapshotStorageV1ValidationResult.Valid)
        val valid = result as PlanSnapshotStorageV1ValidationResult.Valid
        assertEquals(json, valid.storage.persistedJson)
        assertArrayEquals(
            json.toByteArray(Charsets.UTF_8),
            valid.storage.persistedJson.toByteArray(Charsets.UTF_8)
        )
    }

    @Test
    fun strictParseAndCanonicalReserializationMustBeByteForByteIdentical() {
        val canonical = emptySnapshot().toStorageJson()
        val reordered =
            "{\"title\":\"训练标题\",\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"mode\":\"timed\",\"blocks\":[],\"preferences\":null,\"followAlong\":null}"
        val whitespaceChanged = canonical.replace(":1,", ": 1,")

        assertInvalid(PlanSnapshotStorageV1Validator.validate(reordered, WorkoutMode.TIMED))
        assertInvalid(PlanSnapshotStorageV1Validator.validate(whitespaceChanged, WorkoutMode.TIMED))
    }

    @Test
    fun rootMissingExtraWrongVersionAndModeMismatchFailClosed() {
        val canonical = emptySnapshot().toStorageJson()
        val missingPlanId = canonical.replace("\"planId\":null,", "")
        val extra = canonical.dropLast(1) + ",\"extra\":true}"
        val unsupported = canonical.replace("ContractVersion\":1", "ContractVersion\":2")
        val wrongType = canonical.replace("ContractVersion\":1", "ContractVersion\":\"1\"")

        assertInvalid(PlanSnapshotStorageV1Validator.validate(missingPlanId, WorkoutMode.TIMED))
        assertInvalid(PlanSnapshotStorageV1Validator.validate(extra, WorkoutMode.TIMED))
        assertTrue(
            PlanSnapshotStorageV1Validator.validate(unsupported, WorkoutMode.TIMED)
                is PlanSnapshotStorageV1ValidationResult.UnsupportedVersion
        )
        assertInvalid(PlanSnapshotStorageV1Validator.validate(wrongType, WorkoutMode.TIMED))
        assertInvalid(PlanSnapshotStorageV1Validator.validate(canonical, WorkoutMode.STRENGTH))
    }

    @Test
    fun timedCompositionV2BlockRoundTripsWithoutNormalizationOrElementLoss() {
        val snapshot = WorkoutPlanSnapshot(
            planId = "plan",
            title = "Intervals",
            mode = WorkoutMode.TIMED,
            blocks = listOf(
                TimedCompositionBlock(
                    id = "composition",
                    order = 0,
                    compositionVersion = TIMED_COMPOSITION_CURRENT_VERSION,
                    warmupSec = 0,
                    cooldownSec = 0,
                    rounds = 1,
                    restBetweenRoundsSec = 0,
                    stageGroups = listOf(
                        TimedCompositionStageGroup(
                            id = "group",
                            order = 0,
                            name = "Pair",
                            colorHex = "#F26B4F",
                            targets = listOf(
                                TimedCompositionTarget(
                                    id = "work",
                                    order = 0,
                                    name = "Work",
                                    kind = TimedCompositionTargetKind.ACTION,
                                    durationSec = 30,
                                    colorHex = "#F26B4F"
                                ),
                                TimedCompositionTarget(
                                    id = "rest",
                                    order = 1,
                                    name = "Rest",
                                    kind = TimedCompositionTargetKind.REST,
                                    durationSec = 15,
                                    colorHex = "#2FBF8F"
                                )
                            )
                        )
                    )
                )
            )
        )

        val json = snapshot.toStorageJson()
        val result = PlanSnapshotStorageV1Validator.validate(json, WorkoutMode.TIMED)

        assertTrue(result is PlanSnapshotStorageV1ValidationResult.Valid)
        assertEquals(json, (result as PlanSnapshotStorageV1ValidationResult.Valid).storage.persistedJson)
        assertTrue("\"compositionVersion\":2" in json)
        assertTrue("\"id\":\"work\"" in json)
        assertTrue("\"id\":\"rest\"" in json)
    }

    @Test
    fun blockShapeRejectsMissingExtraWrongTypeAndUnknownKindOrVersion() {
        val canonical = timedCompositionSnapshot().toStorageJson()

        assertInvalid(
            PlanSnapshotStorageV1Validator.validate(
                canonical.replace("\"compositionVersion\":2,", ""),
                WorkoutMode.TIMED
            )
        )
        assertInvalid(
            PlanSnapshotStorageV1Validator.validate(
                canonical.replace("\"compositionVersion\":2", "\"compositionVersion\":3"),
                WorkoutMode.TIMED
            )
        )
        assertInvalid(
            PlanSnapshotStorageV1Validator.validate(
                canonical.replace("\"kind\":\"timed_composition\"", "\"kind\":\"future_block\""),
                WorkoutMode.TIMED
            )
        )
        assertInvalid(
            PlanSnapshotStorageV1Validator.validate(
                canonical.replace("\"rounds\":1", "\"rounds\":\"1\""),
                WorkoutMode.TIMED
            )
        )
        assertInvalid(
            PlanSnapshotStorageV1Validator.validate(
                canonical.replaceFirst("\"warmupSec\":0", "\"warmupSec\":0,\"extra\":true"),
                WorkoutMode.TIMED
            )
        )
    }

    @Test
    fun everyExistingBlockFamilyAndNestedRootShapeWritesStrictStorageV1() {
        val timedItem = TimedExerciseItem(
            id = "item",
            exerciseId = "exercise",
            stageType = TimedStageType.WORK,
            workDurationSec = 30,
            restAfterSec = 15,
            autoAdvance = true
        )
        val timed = WorkoutPlanSnapshot(
            title = "All timed blocks",
            mode = WorkoutMode.TIMED,
            blocks = listOf(
                WarmupBlock("warmup", 0, durationSec = 60),
                StretchBlock("stretch", 1, items = listOf(timedItem)),
                TimedCircuitBlock("circuit", 2, rounds = 2, items = listOf(timedItem)),
                RestBlock("rest", 3, durationSec = 30, label = "Recover"),
                timedCompositionSnapshot().blocks.single(),
                CooldownBlock("cooldown", 5, durationSec = 60)
            ),
            preferences = PlanPreferences(
                heartRateDisplay = HeartRateDisplayPreference(
                    enabled = true,
                    showDisconnectedPlaceholder = true
                )
            )
        )
        val strength = WorkoutPlanSnapshot(
            title = "Strength",
            mode = WorkoutMode.STRENGTH,
            blocks = listOf(
                StrengthExerciseBlock(
                    id = "strength",
                    order = 0,
                    exerciseId = "squat",
                    sets = listOf(
                        StrengthSetPlan(
                            id = "set",
                            order = 0,
                            kind = StrengthSetKind.WORKING,
                            targetWeight = WeightValue(60.0, WeightUnit.KG),
                            repTarget = RepTarget.Range(8, 12),
                            restAfterSec = 90
                        )
                    ),
                    target = StrengthExerciseTarget(
                        weight = WeightValue(60.0, WeightUnit.KG),
                        repTarget = RepTarget.Fixed(10),
                        restAfterSetSec = 90
                    ),
                    substitutions = listOf("goblet-squat")
                )
            )
        )
        val followAlong = WorkoutPlanSnapshot(
            title = "Follow",
            mode = WorkoutMode.FOLLOW_ALONG,
            blocks = listOf(TimedCircuitBlock("follow", 0, 1, listOf(timedItem))),
            followAlong = FollowAlongPlanMeta(
                preset = true,
                coverMediaId = "cover",
                coachMediaIds = listOf("coach"),
                chapterIds = listOf("chapter"),
                timelineCueIds = listOf("cue"),
                musicTrackIds = listOf("music"),
                aiAnalysisProfileId = "analysis"
            )
        )

        listOf(timed, strength, followAlong).forEach { snapshot ->
            val json = snapshot.toStorageJson()
            val result = PlanSnapshotStorageV1Validator.validate(json, snapshot.mode)
            assertTrue("Writer output failed strict v1 validation: $json", result is PlanSnapshotStorageV1ValidationResult.Valid)
        }
    }

    @Test
    fun duplicateKeysAndElementLossAttemptsFailClosed() {
        val canonical = timedCompositionSnapshot().toStorageJson()
        val duplicateRootKey = canonical.replace(
            "\"planId\":null",
            "\"planId\":null,\"planId\":null"
        )
        val nonObjectBlock = canonical.replaceFirst("{\"id\":\"block\"", "null")

        assertInvalid(PlanSnapshotStorageV1Validator.validate(duplicateRootKey, WorkoutMode.TIMED))
        assertInvalid(PlanSnapshotStorageV1Validator.validate(nonObjectBlock, WorkoutMode.TIMED))
    }

    private fun emptySnapshot(): WorkoutPlanSnapshot = WorkoutPlanSnapshot(
        title = "训练标题",
        mode = WorkoutMode.TIMED,
        blocks = emptyList()
    )

    private fun timedCompositionSnapshot(): WorkoutPlanSnapshot = WorkoutPlanSnapshot(
        title = "Timed",
        mode = WorkoutMode.TIMED,
        blocks = listOf(
            TimedCompositionBlock(
                id = "block",
                order = 0,
                rounds = 1,
                stageGroups = listOf(
                    TimedCompositionStageGroup(
                        id = "group",
                        order = 0,
                        name = "Group",
                        colorHex = "#F26B4F",
                        targets = listOf(
                            TimedCompositionTarget(
                                id = "target",
                                order = 0,
                                name = "Work",
                                kind = TimedCompositionTargetKind.ACTION,
                                durationSec = 30,
                                colorHex = "#F26B4F"
                            )
                        )
                    )
                )
            )
        )
    )

    private fun assertInvalid(result: PlanSnapshotStorageV1ValidationResult) {
        assertTrue(result is PlanSnapshotStorageV1ValidationResult.Invalid)
    }
}
