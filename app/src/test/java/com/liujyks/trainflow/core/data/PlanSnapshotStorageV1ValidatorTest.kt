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
        val item = TimedExerciseItem(
            id = "item",
            exerciseId = "exercise",
            stageType = TimedStageType.WORK,
            workDurationSec = 40,
            restAfterSec = 20,
            autoAdvance = true
        )
        val timed = WorkoutPlanSnapshot(
            title = "Excluded timed title",
            mode = WorkoutMode.TIMED,
            blocks = listOf(
                WarmupBlock("warmup", 0, durationSec = 60),
                TimedCircuitBlock("circuit", 1, rounds = 2, restBetweenRoundsSec = 30, items = listOf(item)),
                TimedCompositionBlock(
                    id = "composition",
                    order = 2,
                    rounds = 1,
                    stageGroups = listOf(
                        TimedCompositionStageGroup(
                            id = "group",
                            order = 0,
                            name = "Excluded group name",
                            colorHex = "#111111",
                            targets = listOf(
                                TimedCompositionTarget(
                                    id = "target",
                                    order = 0,
                                    name = "Excluded target name",
                                    kind = TimedCompositionTargetKind.ACTION,
                                    durationSec = 30,
                                    colorHex = "#222222"
                                )
                            )
                        )
                    )
                )
            )
        )
        val strength = WorkoutPlanSnapshot(
            title = "Excluded strength title",
            mode = WorkoutMode.STRENGTH,
            blocks = listOf(
                StrengthExerciseBlock(
                    id = "strength",
                    order = 0,
                    exerciseId = "squat",
                    target = StrengthExerciseTarget(
                        weight = WeightValue(60.0, WeightUnit.KG),
                        repTarget = RepTarget.Fixed(10),
                        restAfterSetSec = 90
                    ),
                    sets = listOf(
                        StrengthSetPlan(
                            id = "set",
                            order = 0,
                            kind = StrengthSetKind.WORKING,
                            targetWeight = WeightValue(62.5, WeightUnit.LB),
                            repTarget = RepTarget.Range(8, 12),
                            restAfterSec = null
                        )
                    ),
                    substitutions = listOf("front-squat")
                )
            )
        )
        val follow = WorkoutPlanSnapshot(
            title = "Excluded follow title",
            mode = WorkoutMode.FOLLOW_ALONG,
            blocks = listOf(
                TimedCircuitBlock("follow", 0, rounds = 1, restBetweenRoundsSec = null, items = listOf(item))
            ),
            followAlong = FollowAlongPlanMeta(preset = true)
        )
        val goldens = listOf(
            timed to
                "{\"signatureInputContractVersion\":1,\"mode\":\"timed\",\"blocks\":[{\"blockId\":\"warmup\",\"blockKind\":\"warmup\",\"order\":0,\"durationSec\":60,\"items\":[]},{\"blockId\":\"circuit\",\"blockKind\":\"timed_circuit\",\"order\":1,\"rounds\":2,\"restBetweenRoundsSec\":30,\"items\":[{\"itemId\":\"item\",\"exerciseId\":\"exercise\",\"side\":null,\"stageType\":\"work\",\"workDurationSec\":40,\"restAfterSec\":20,\"autoAdvance\":true}]},{\"blockId\":\"composition\",\"blockKind\":\"timed_composition\",\"order\":2,\"compositionVersion\":2,\"warmupSec\":0,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[{\"stageGroupId\":\"group\",\"order\":0,\"targets\":[{\"targetId\":\"target\",\"order\":0,\"targetKind\":\"action\",\"durationSec\":30,\"autoAdvance\":true}]}]}]}",
            strength to
                "{\"signatureInputContractVersion\":1,\"mode\":\"strength\",\"blocks\":[{\"blockId\":\"strength\",\"blockKind\":\"strength_exercise\",\"order\":0,\"exerciseId\":\"squat\",\"target\":{\"weight\":{\"value\":60,\"unit\":\"kg\"},\"repTarget\":{\"kind\":\"fixed\",\"fixedReps\":10,\"minReps\":null,\"maxReps\":null},\"restAfterSetSec\":90},\"sets\":[{\"setPlanId\":\"set\",\"order\":0,\"setKind\":\"working\",\"side\":null,\"targetWeight\":{\"value\":62.5,\"unit\":\"lb\"},\"repTarget\":{\"kind\":\"range\",\"fixedReps\":null,\"minReps\":8,\"maxReps\":12},\"restAfterSec\":null}],\"substitutions\":[\"front-squat\"],\"setTimerMode\":\"manual_start\"}]}",
            follow to
                "{\"signatureInputContractVersion\":1,\"mode\":\"follow_along\",\"blocks\":[{\"blockId\":\"follow\",\"blockKind\":\"timed_circuit\",\"order\":0,\"rounds\":1,\"restBetweenRoundsSec\":null,\"items\":[{\"itemId\":\"item\",\"exerciseId\":\"exercise\",\"side\":null,\"stageType\":\"work\",\"workDurationSec\":40,\"restAfterSec\":20,\"autoAdvance\":true}]}]}"
        )

        goldens.forEach { (snapshot, expected) ->
            val storage = validatedStorage(snapshot)
            val bytes = requireNotNull(OrderedStructureSignatureInputV1.encode(storage))
            assertArrayEquals(expected.toByteArray(Charsets.UTF_8), bytes)
            val independentDigest = testSha256(expected.toByteArray(Charsets.UTF_8))
            assertEquals(independentDigest, testSha256(bytes))
            assertEquals(independentDigest, OrderedStructureSignatureInputV1.digestHexLowercase(storage))
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
    fun includedAndExcludedMatricesCoverCompositionStrengthAndFollowAlongMembers() {
        val composition = timedCompositionSnapshot().toStorageJson()
        val compositionDigest = digestOf(composition, WorkoutMode.TIMED)
        linkedMapOf(
            "composition.blockId" to composition.replace("\"id\":\"block\"", "\"id\":\"block-b\""),
            "composition.blockOrder" to composition.replaceFirst("\"order\":0", "\"order\":1"),
            "composition.warmupSec" to composition.replace("\"warmupSec\":0", "\"warmupSec\":1"),
            "composition.cooldownSec" to composition.replace("\"cooldownSec\":0", "\"cooldownSec\":1"),
            "composition.rounds" to composition.replace("\"rounds\":1", "\"rounds\":2"),
            "composition.restBetweenRoundsSec" to composition.replace("\"restBetweenRoundsSec\":0", "\"restBetweenRoundsSec\":1"),
            "composition.stageGroupId" to composition.replace("\"id\":\"group\"", "\"id\":\"group-b\""),
            "composition.stageGroupOrder" to composition.replaceFirst("\"order\":0,\"name\":\"Group\"", "\"order\":1,\"name\":\"Group\""),
            "composition.targetId" to composition.replace("\"id\":\"target\"", "\"id\":\"target-b\""),
            "composition.targetOrder" to composition.replaceFirst("\"order\":0,\"name\":\"Work\"", "\"order\":1,\"name\":\"Work\""),
            "composition.targetKind" to composition.replace("\"kind\":\"action\"", "\"kind\":\"rest\""),
            "composition.durationSec" to composition.replace("\"durationSec\":30", "\"durationSec\":31"),
            "composition.autoAdvance" to composition.replace("\"autoAdvance\":true", "\"autoAdvance\":false")
        ).forEach { (field, mutation) ->
            assertTrue("Included $field did not change digest", compositionDigest != digestOf(mutation, WorkoutMode.TIMED))
        }
        linkedMapOf(
            "root.title" to composition.replace("\"title\":\"Timed\"", "\"title\":\"Timed B\""),
            "composition.blockTitle" to composition.replace("\"title\":null", "\"title\":\"Display\""),
            "composition.groupName" to composition.replace("\"name\":\"Group\"", "\"name\":\"Group B\""),
            "composition.groupColor" to composition.replace("\"colorHex\":\"#F26B4F\"", "\"colorHex\":\"#111111\""),
            "composition.groupIcon" to composition.replaceFirst("\"iconKey\":null", "\"iconKey\":\"group-icon\""),
            "composition.targetName" to composition.replace("\"name\":\"Work\"", "\"name\":\"Work B\""),
            "composition.targetColor" to composition.replace(
                "\"durationSec\":30,\"colorHex\":\"#F26B4F\"",
                "\"durationSec\":30,\"colorHex\":\"#222222\""
            ),
            "composition.targetIcon" to composition.replace(
                "\"durationSec\":30,\"colorHex\":\"#F26B4F\",\"iconKey\":null",
                "\"durationSec\":30,\"colorHex\":\"#F26B4F\",\"iconKey\":\"target-icon\""
            )
        ).forEach { (field, mutation) ->
            assertEquals("Excluded $field changed digest", compositionDigest, digestOf(mutation, WorkoutMode.TIMED))
        }

        val strength = WorkoutPlanSnapshot(
            title = "Strength display",
            mode = WorkoutMode.STRENGTH,
            blocks = listOf(
                StrengthExerciseBlock(
                    id = "strength",
                    order = 0,
                    exerciseId = "squat",
                    target = StrengthExerciseTarget(
                        weight = WeightValue(60.0, WeightUnit.KG),
                        repTarget = RepTarget.Fixed(10),
                        restAfterSetSec = 90
                    ),
                    sets = listOf(
                        StrengthSetPlan(
                            id = "set",
                            order = 0,
                            kind = StrengthSetKind.WORKING,
                            targetWeight = WeightValue(62.5, WeightUnit.LB),
                            repTarget = RepTarget.Range(8, 12),
                            restAfterSec = 80
                        )
                    ),
                    substitutions = listOf("front-squat", "goblet-squat")
                )
            )
        ).toStorageJson()
        val strengthDigest = digestOf(strength, WorkoutMode.STRENGTH)
        linkedMapOf(
            "strength.blockId" to strength.replace("\"id\":\"strength\"", "\"id\":\"strength-b\""),
            "strength.blockOrder" to strength.replaceFirst("\"order\":0", "\"order\":1"),
            "strength.exerciseId" to strength.replace("\"exerciseId\":\"squat\"", "\"exerciseId\":\"deadlift\""),
            "strength.targetWeightValue" to strength.replaceFirst("\"value\":60", "\"value\":61"),
            "strength.targetWeightUnit" to strength.replaceFirst("\"unit\":\"kg\"", "\"unit\":\"lb\""),
            "strength.targetRep" to strength.replace("\"reps\":10", "\"reps\":11"),
            "strength.targetRest" to strength.replace("\"restAfterSetSec\":90", "\"restAfterSetSec\":91"),
            "strength.setId" to strength.replace("\"id\":\"set\"", "\"id\":\"set-b\""),
            "strength.setOrder" to strength.replaceFirst("\"order\":0,\"kind\":\"working\"", "\"order\":1,\"kind\":\"working\""),
            "strength.setKind" to strength.replace("\"kind\":\"working\"", "\"kind\":\"warmup\""),
            "strength.setWeightDecimal" to strength.replace("\"value\":62.5", "\"value\":63.5"),
            "strength.setWeightUnit" to strength.replace("\"unit\":\"lb\"", "\"unit\":\"kg\""),
            "strength.repMin" to strength.replace("\"minReps\":8", "\"minReps\":7"),
            "strength.repMax" to strength.replace("\"maxReps\":12", "\"maxReps\":13"),
            "strength.setRest" to strength.replace("\"restAfterSec\":80", "\"restAfterSec\":90"),
            "strength.substitutionValue" to strength.replace("\"front-squat\"", "\"split-squat\""),
            "strength.substitutionOrder" to strength.replace("[\"front-squat\",\"goblet-squat\"]", "[\"goblet-squat\",\"front-squat\"]"),
            "strength.setTimerMode" to strength.replace("\"manual_start\"", "\"auto_after_rest\"")
        ).forEach { (field, mutation) ->
            assertTrue("Included $field replacement did not change source", mutation != strength)
            assertTrue("Included $field did not change digest", strengthDigest != digestOf(mutation, WorkoutMode.STRENGTH))
        }
        assertEquals(
            strengthDigest,
            digestOf(strength.replace("\"title\":\"Strength display\"", "\"title\":\"Other display\""), WorkoutMode.STRENGTH)
        )

        val follow = WorkoutPlanSnapshot(
            title = "Follow display",
            mode = WorkoutMode.FOLLOW_ALONG,
            blocks = listOf(
                TimedCircuitBlock(
                    "follow",
                    0,
                    rounds = 2,
                    restBetweenRoundsSec = 30,
                    items = listOf(
                        TimedExerciseItem(
                            id = "follow-item",
                            exerciseId = "follow-exercise",
                            labelOverride = "Excluded label",
                            stageType = TimedStageType.WORK,
                            iconKey = "excluded-icon",
                            colorHex = "#123456",
                            workDurationSec = 40,
                            restAfterSec = 20,
                            autoAdvance = true
                        )
                    )
                )
            ),
            followAlong = FollowAlongPlanMeta(preset = true, coverMediaId = "excluded-cover")
        ).toStorageJson()
        val followDigest = digestOf(follow, WorkoutMode.FOLLOW_ALONG)
        linkedMapOf(
            "follow.blockId" to follow.replace("\"id\":\"follow\"", "\"id\":\"follow-b\""),
            "follow.rounds" to follow.replace("\"rounds\":2", "\"rounds\":3"),
            "follow.restBetweenRoundsSec" to follow.replace("\"restBetweenRoundsSec\":30", "\"restBetweenRoundsSec\":31"),
            "follow.itemId" to follow.replace("\"id\":\"follow-item\"", "\"id\":\"follow-item-b\""),
            "follow.exerciseId" to follow.replace("\"exerciseId\":\"follow-exercise\"", "\"exerciseId\":\"follow-exercise-b\""),
            "follow.stageType" to follow.replace("\"stageType\":\"work\"", "\"stageType\":\"custom\""),
            "follow.workDurationSec" to follow.replace("\"workDurationSec\":40", "\"workDurationSec\":41"),
            "follow.restAfterSec" to follow.replace("\"restAfterSec\":20", "\"restAfterSec\":21"),
            "follow.autoAdvance" to follow.replace("\"autoAdvance\":true", "\"autoAdvance\":false")
        ).forEach { (field, mutation) ->
            assertTrue("Included $field did not change digest", followDigest != digestOf(mutation, WorkoutMode.FOLLOW_ALONG))
        }
        linkedMapOf(
            "follow.title" to follow.replace("\"title\":\"Follow display\"", "\"title\":\"Other\""),
            "follow.itemLabel" to follow.replace("\"labelOverride\":\"Excluded label\"", "\"labelOverride\":\"Other\""),
            "follow.itemIcon" to follow.replace("\"iconKey\":\"excluded-icon\"", "\"iconKey\":\"other-icon\""),
            "follow.itemColor" to follow.replace("\"colorHex\":\"#123456\"", "\"colorHex\":\"#654321\""),
            "follow.coverMedia" to follow.replace("\"coverMediaId\":\"excluded-cover\"", "\"coverMediaId\":\"other-cover\"")
        ).forEach { (field, mutation) ->
            assertEquals("Excluded $field changed digest", followDigest, digestOf(mutation, WorkoutMode.FOLLOW_ALONG))
        }
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
