package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.model.TIMED_COMPOSITION_CURRENT_VERSION
import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
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
import java.security.MessageDigest

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

    @Test
    fun knownProjectionBytesAndIndependentShaGoldensCoverAllModes() {
        val goldens = listOf(
            WorkoutMode.TIMED to (
                "{\"signatureInputContractVersion\":1,\"mode\":\"timed\",\"blocks\":[]}" to
                    "4d606740d7a3e5072ee4506e70d40f03b44ed2c762ec3d334b8791a18ccb002a"
                ),
            WorkoutMode.STRENGTH to (
                "{\"signatureInputContractVersion\":1,\"mode\":\"strength\",\"blocks\":[]}" to
                    "bba02600522e5032df4ef3cd8996a7cc2fa3ef00cb2b49975ebbec3fa678d0ba"
                ),
            WorkoutMode.FOLLOW_ALONG to (
                "{\"signatureInputContractVersion\":1,\"mode\":\"follow_along\",\"blocks\":[]}" to
                    "dc27bf9244329c6b18f6c38a3e780e0e8f28c8ac9f9606c67fe4f8499742ab74"
                )
        )

        goldens.forEach { (mode, expected) ->
            val storage = validatedStorage(
                WorkoutPlanSnapshot(title = "Display only", mode = mode, blocks = emptyList())
            )
            val bytes = requireNotNull(OrderedStructureSignatureInputV1.encode(storage))
            assertArrayEquals(expected.first.toByteArray(Charsets.UTF_8), bytes)
            assertEquals(expected.second, testSha256(bytes))
            assertEquals(expected.second, OrderedStructureSignatureInputV1.digestHexLowercase(storage))
        }
    }

    @Test
    fun strengthProjectionGoldenCoversZeroDecimalExplicitNullEscapingAndMemberOrder() {
        val storage = validatedStorage(
            WorkoutPlanSnapshot(
                title = "Excluded title",
                mode = WorkoutMode.STRENGTH,
                blocks = listOf(
                    StrengthExerciseBlock(
                        id = "strength\"\\\n",
                        order = 0,
                        title = "Excluded block title",
                        exerciseId = "squat",
                        target = StrengthExerciseTarget(
                            weight = WeightValue(0.0, WeightUnit.KG),
                            repTarget = RepTarget.Fixed(10),
                            restAfterSetSec = null
                        ),
                        sets = listOf(
                            StrengthSetPlan(
                                id = "set-1",
                                order = 0,
                                kind = StrengthSetKind.WORKING,
                                targetWeight = WeightValue(62.5, WeightUnit.LB),
                                repTarget = RepTarget.Range(8, 12),
                                restAfterSec = null
                            )
                        ),
                        substitutions = listOf("goblet-squat", "front-squat")
                    )
                )
            )
        )
        val expected =
            "{\"signatureInputContractVersion\":1,\"mode\":\"strength\",\"blocks\":[{\"blockId\":\"strength\\\"\\\\\\n\",\"blockKind\":\"strength_exercise\",\"order\":0,\"exerciseId\":\"squat\",\"target\":{\"weight\":{\"value\":0,\"unit\":\"kg\"},\"repTarget\":{\"kind\":\"fixed\",\"fixedReps\":10,\"minReps\":null,\"maxReps\":null},\"restAfterSetSec\":null},\"sets\":[{\"setPlanId\":\"set-1\",\"order\":0,\"setKind\":\"working\",\"side\":null,\"targetWeight\":{\"value\":62.5,\"unit\":\"lb\"},\"repTarget\":{\"kind\":\"range\",\"fixedReps\":null,\"minReps\":8,\"maxReps\":12},\"restAfterSec\":null}],\"substitutions\":[\"goblet-squat\",\"front-squat\"],\"setTimerMode\":\"manual_start\"}]}"

        val bytes = requireNotNull(OrderedStructureSignatureInputV1.encode(storage))
        assertArrayEquals(expected.toByteArray(Charsets.UTF_8), bytes)
        assertEquals(testSha256(expected.toByteArray(Charsets.UTF_8)), OrderedStructureSignatureInputV1.digestHexLowercase(storage))
    }

    @Test
    fun includedMutationsChangeDigestAndExcludedDisplayCueColorMutationsDoNot() {
        val timed = WorkoutPlanSnapshot(
            planId = "plan-a",
            title = "Title A",
            mode = WorkoutMode.TIMED,
            blocks = listOf(
                TimedCircuitBlock(
                    id = "circuit",
                    order = 0,
                    title = "Circuit A",
                    rounds = 2,
                    restBetweenRoundsSec = 30,
                    items = listOf(
                        TimedExerciseItem(
                            id = "item",
                            exerciseId = "exercise",
                            labelOverride = "Label A",
                            stageType = TimedStageType.WORK,
                            iconKey = "icon-a",
                            colorHex = "#111111",
                            workDurationSec = 40,
                            restAfterSec = 20,
                            cueSettings = CueSettings(
                                actionEnding = CountdownCue(
                                    enabled = true,
                                    thresholdSec = 3,
                                    soundEnabled = true,
                                    vibrationEnabled = true,
                                    emphasisAnimationEnabled = true,
                                    voiceCueEnabled = false
                                )
                            ),
                            autoAdvance = true
                        )
                    )
                )
            )
        )
        val canonical = timed.toStorageJson()
        val originalDigest = digestOf(canonical, WorkoutMode.TIMED)
        listOf(
            canonical.replace("\"id\":\"circuit\"", "\"id\":\"circuit-b\""),
            canonical.replace("\"order\":0", "\"order\":1"),
            canonical.replace("\"rounds\":2", "\"rounds\":3"),
            canonical.replace("\"restBetweenRoundsSec\":30", "\"restBetweenRoundsSec\":31"),
            canonical.replace("\"id\":\"item\"", "\"id\":\"item-b\""),
            canonical.replace("\"exerciseId\":\"exercise\"", "\"exerciseId\":\"exercise-b\""),
            canonical.replace("\"stageType\":\"work\"", "\"stageType\":\"custom\""),
            canonical.replace("\"workDurationSec\":40", "\"workDurationSec\":41"),
            canonical.replace("\"restAfterSec\":20", "\"restAfterSec\":21"),
            canonical.replace("\"autoAdvance\":true", "\"autoAdvance\":false")
        ).forEach { mutation -> assertTrue(originalDigest != digestOf(mutation, WorkoutMode.TIMED)) }

        listOf(
            canonical.replace("\"planId\":\"plan-a\"", "\"planId\":\"plan-b\""),
            canonical.replace("\"title\":\"Title A\"", "\"title\":\"Title B\""),
            canonical.replace("\"title\":\"Circuit A\"", "\"title\":\"Circuit B\""),
            canonical.replace("\"labelOverride\":\"Label A\"", "\"labelOverride\":\"Label B\""),
            canonical.replace("\"iconKey\":\"icon-a\"", "\"iconKey\":\"icon-b\""),
            canonical.replace("\"colorHex\":\"#111111\"", "\"colorHex\":\"#222222\""),
            canonical.replace("\"thresholdSec\":3", "\"thresholdSec\":4")
        ).forEach { mutation -> assertEquals(originalDigest, digestOf(mutation, WorkoutMode.TIMED)) }
    }

    @Test
    fun corruptUnknownMismatchNonFiniteDropAndReorderFailClosed() {
        val canonical = timedCompositionSnapshot().toStorageJson()
        val original = validatedStorage(timedCompositionSnapshot())
        val originalDigest = requireNotNull(OrderedStructureSignatureInputV1.digestHexLowercase(original))
        val dropped = canonical.replace(Regex("\\{\\\"id\\\":\\\"target\\\".*?\\}"), "")
        val reordered = canonical
            .replace("\"stageGroups\":[", "\"stageGroups\":[{\"id\":\"second\",\"order\":1,\"name\":\"Second\",\"colorHex\":\"#000000\",\"targets\":[]},")

        assertEquals(null, OrderedStructureSignatureInputV1.encode(WorkoutPlanSnapshotStorageV1(WorkoutMode.TIMED, canonical.replace("ContractVersion\":1", "ContractVersion\":2"))))
        assertEquals(null, OrderedStructureSignatureInputV1.encode(WorkoutPlanSnapshotStorageV1(WorkoutMode.STRENGTH, canonical)))
        assertEquals(null, OrderedStructureSignatureInputV1.encode(WorkoutPlanSnapshotStorageV1(WorkoutMode.TIMED, canonical.replace("\"kind\":\"timed_composition\"", "\"kind\":\"future\""))))
        assertEquals(null, OrderedStructureSignatureInputV1.encode(WorkoutPlanSnapshotStorageV1(WorkoutMode.TIMED, canonical.replace("\"rounds\":1", "\"rounds\":NaN"))))
        assertTrue(digestOf(dropped, WorkoutMode.TIMED) != originalDigest)
        assertTrue(digestOf(reordered, WorkoutMode.TIMED) != originalDigest)
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

    private fun validatedStorage(snapshot: WorkoutPlanSnapshot): WorkoutPlanSnapshotStorageV1 =
        (PlanSnapshotStorageV1Validator.validate(snapshot.toStorageJson(), snapshot.mode) as
            PlanSnapshotStorageV1ValidationResult.Valid).storage

    private fun digestOf(json: String, mode: WorkoutMode): String =
        requireNotNull(
            OrderedStructureSignatureInputV1.digestHexLowercase(
                (PlanSnapshotStorageV1Validator.validate(json, mode) as
                    PlanSnapshotStorageV1ValidationResult.Valid).storage
            )
        )

    private fun testSha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun assertInvalid(result: PlanSnapshotStorageV1ValidationResult) {
        assertTrue(result is PlanSnapshotStorageV1ValidationResult.Invalid)
    }
}
