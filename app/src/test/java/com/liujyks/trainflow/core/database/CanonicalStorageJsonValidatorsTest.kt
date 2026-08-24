package com.liujyks.trainflow.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalStorageJsonValidatorsTest {
    @Test
    fun allEightClosedStorageObjectsAcceptTheirCanonicalShapes() {
        listOf(
            CanonicalStorageJsonV1Validators.validateSessionDisplayMetadata(DISPLAY_METADATA),
            CanonicalStorageJsonV1Validators.validateZoneSnapshot(ZONE_SNAPSHOT),
            PhaseIdentityV1Validator.validate(phaseFixtures().first().json),
            CanonicalStorageJsonV1Validators.validateAnalysisConfig(ANALYSIS_CONFIG),
            CanonicalStorageJsonV1Validators.validateZoneDurations(ZONE_DURATIONS),
            CanonicalStorageJsonV1Validators.validatePhaseAggregates(PHASE_AGGREGATES),
            CanonicalStorageJsonV1Validators.validateDurationBreakdown(DURATION_BREAKDOWN),
            CanonicalStorageJsonV1Validators.validateQualityReasons(QUALITY_REASONS)
        ).forEach(::assertValid)
    }

    @Test
    fun structuralValidatorsRejectMissingExtraWrongTypeNullAndUnknownVersion() {
        assertInvalid(
            CanonicalStorageJsonV1Validators.validateSessionDisplayMetadata(
                DISPLAY_METADATA.replace(",\"entries\":[]", "")
            )
        )
        assertInvalid(
            CanonicalStorageJsonV1Validators.validateZoneSnapshot(
                ZONE_SNAPSHOT.dropLast(1) + ",\"extra\":true}"
            )
        )
        assertInvalid(
            CanonicalStorageJsonV1Validators.validateAnalysisConfig(
                ANALYSIS_CONFIG.replace("\"sampleValidityCapMs\":2500", "\"sampleValidityCapMs\":\"2500\"")
            )
        )
        assertInvalid(
            CanonicalStorageJsonV1Validators.validateZoneDurations(
                ZONE_DURATIONS.replace("\"below50DurationMs\":0", "\"below50DurationMs\":null")
            )
        )
        assertInvalid(
            CanonicalStorageJsonV1Validators.validatePhaseAggregates(
                PHASE_AGGREGATES_WITH_ENTRY.replace(
                    "\"highestSampleSequence\":null",
                    "\"highestSampleSequence\":null,\"zoneDurations\":{}"
                )
            )
        )
        assertInvalid(
            CanonicalStorageJsonV1Validators.validateDurationBreakdown(
                DURATION_BREAKDOWN.replace("\"contractVersion\":1", "\"contractVersion\":null")
            )
        )
        assertTrue(
            CanonicalStorageJsonV1Validators.validateQualityReasons(
                QUALITY_REASONS.replace("ContractVersion\":1", "ContractVersion\":2")
            ) is CanonicalValidationResult.UnsupportedVersion
        )
    }

    @Test
    fun qualityReasonsValidationIsStructuralAndDoesNotStealCs05Semantics() {
        val structurallyValidButSemanticallyOwnedByCs05 =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[],\"phaseReasons\":[{\"phaseSequence\":0,\"reasonCode\":\"process_interrupted\",\"durationMs\":5}]}"

        assertValid(
            CanonicalStorageJsonV1Validators.validateQualityReasons(
                structurallyValidButSemanticallyOwnedByCs05
            )
        )
    }

    @Test
    fun displayMetadataTransitionIsAppendOnlyAndTerminalImmutable() {
        val first = displayMetadata(entry("exercise-1", "深蹲", "plan_snapshot"))
        val appended = displayMetadata(
            entry("exercise-1", "深蹲", "plan_snapshot"),
            entry("exercise-2", "高脚杯深蹲", "runtime_substitution")
        )
        val rewritten = displayMetadata(entry("exercise-1", "新名字", "plan_snapshot"))

        assertValid(SessionDisplayMetadataV1Validator.validateTransition(first, appended, terminal = false))
        assertInvalid(SessionDisplayMetadataV1Validator.validateTransition(first, rewritten, terminal = false))
        assertInvalid(SessionDisplayMetadataV1Validator.validateTransition(first, appended, terminal = true))
    }

    @Test
    fun everyAcceptedPhaseFamilyVariantAndPausedShapeValidates() {
        val fixtures = phaseFixtures()
        assertEquals(32, fixtures.size)
        fixtures.forEach { fixture ->
            val result = PhaseIdentityV1Validator.validate(fixture.json, fixture.phaseKind)
            assertTrue("${fixture.name} failed with $result", result is CanonicalValidationResult.Valid)
        }
    }

    @Test
    fun pausedVariantIndicesAndSignatureAreStrict() {
        val strengthPaused = phaseFixtures().first { it.name == "strength_paused" }.json
        val followPaused = phaseFixtures().first { it.name == "follow_paused" }.json
        val compositionWarmup = phaseFixtures().first { it.name == "composition_warmup" }.json

        assertInvalid(
            PhaseIdentityV1Validator.validate(
                strengthPaused.replace("\"variant\":\"paused\",", "")
            )
        )
        assertInvalid(
            PhaseIdentityV1Validator.validate(
                followPaused.replace("\"blockId\":null", "\"blockId\":\"unexpected\"")
            )
        )
        assertInvalid(
            PhaseIdentityV1Validator.validate(
                compositionWarmup.replace("\"stepIndex0\":0", "\"stepIndex0\":-1")
            )
        )
        assertInvalid(
            PhaseIdentityV1Validator.validate(
                compositionWarmup.replace(DIGEST, DIGEST.uppercase())
            )
        )
    }

    private fun phaseFixtures(): List<PhaseFixture> = buildList {
        fun addLegacy(name: String, phaseKind: String, payload: String) = add(
            PhaseFixture(name, phaseKind, envelope("legacy_timed_v1", 1, "timed", phaseKind, payload))
        )
        fun addComposition(name: String, phaseKind: String, payload: String) = add(
            PhaseFixture(name, phaseKind, envelope("timed_composition_v2", 2, "timed", phaseKind, payload))
        )
        fun addStrength(name: String, phaseKind: String, payload: String) = add(
            PhaseFixture(name, phaseKind, envelope("strength_v1", 1, "strength", phaseKind, payload))
        )
        fun addFollow(name: String, phaseKind: String, payload: String) = add(
            PhaseFixture(name, phaseKind, envelope("follow_along_v1", 1, "follow_along", phaseKind, payload))
        )

        addLegacy("legacy_boundary_warmup", "timed_work", legacyPayload("boundary_block_work", "block", 0, "warmup", "warmup", null, null, null))
        addLegacy("legacy_boundary_stretch", "timed_work", legacyPayload("boundary_block_work", "block", 0, "stretch", "cooldown", null, null, null))
        addLegacy("legacy_boundary_cooldown", "timed_work", legacyPayload("boundary_block_work", "block", 0, "cooldown", "cooldown", null, null, null))
        addLegacy("legacy_boundary_item_work", "timed_work", legacyPayload("boundary_item_work", "block", 0, "warmup", "work", "item", "exercise", null))
        addLegacy("legacy_boundary_item_rest", "timed_rest", legacyPayload("boundary_item_rest", "block", 0, "warmup", "rest", "item", null, null))
        addLegacy("legacy_boundary_rest_after", "timed_rest", legacyPayload("boundary_rest_after_item", "block", 0, "cooldown", "rest", "item", "exercise", null))
        addLegacy("legacy_circuit_work", "timed_work", legacyPayload("circuit_item_work", "block", 0, "timed_circuit", "work", "item", "exercise", 0))
        addLegacy("legacy_circuit_item_rest", "timed_rest", legacyPayload("circuit_item_rest", "block", 1, "timed_circuit", "rest", "item", null, 0))
        addLegacy("legacy_circuit_rest_after", "timed_rest", legacyPayload("circuit_rest_after_item", "block", 2, "timed_circuit", "rest", "item", "exercise", 0))
        addLegacy("legacy_between_round", "timed_rest", legacyPayload("between_round_rest", "block", 3, "timed_circuit", "rest", null, null, 0))
        addLegacy("legacy_standalone_rest", "timed_rest", legacyPayload("standalone_rest", "block", 0, "rest", "rest", null, null, null))
        addLegacy("legacy_paused", "paused", legacyPayload("paused", null, null, null, null, null, null, null))

        addComposition("composition_warmup", "timed_work", compositionPayload("warmup", "block", "block:warmup", "warmup", "block:warmup", "block:warmup:target", "warmup", null, null, 0, 0, 0, 0))
        addComposition("composition_action", "timed_work", compositionPayload("stage_group_action", "block", "stage", "stage_group", "group", "target", "action", 0, 0, 0, 0, 0, 0))
        addComposition("composition_custom", "timed_work", compositionPayload("stage_group_custom", "block", "stage", "stage_group", "group", "target", "custom", 0, 0, 1, 0, 1, 1))
        addComposition("composition_rest", "timed_rest", compositionPayload("stage_group_rest", "block", "stage", "stage_group", "group", "target", "rest", 0, 0, 2, 0, 2, 2))
        addComposition("composition_between_round", "timed_rest", compositionPayload("between_round_rest", "block", "block:r0:between-round-rest", "between_round_rest", "block:r0:between-round-rest", "block:r0:between-round-rest:target", "between_round_rest", 0, null, 0, 1, 3, 3))
        addComposition("composition_cooldown", "timed_work", compositionPayload("cooldown", "block", "block:cooldown", "cooldown", "block:cooldown", "block:cooldown:target", "cooldown", null, null, 0, 2, 4, 4))
        addComposition("composition_paused", "paused", compositionPayload("paused", null, null, null, null, null, null, null, null, null, null, null, null))

        listOf(
            "prepare_set" to "strength_prepare_set",
            "active_set" to "strength_active_set",
            "confirm_set" to "strength_confirm_set",
            "rest" to "strength_rest"
        ).forEach { (variant, phaseKind) ->
            addStrength("strength_$variant", phaseKind, strengthPayload(variant, "block", "set", "planned", "planned", 0, 0, "working", null))
        }
        addStrength("strength_paused", "paused", strengthPayload("paused", null, null, null, null, null, null, null, null))

        addFollow("follow_circuit_action", "follow_along_action", followPayload("circuit_action", "block", 0, "action", "item", "exercise", 0))
        addFollow("follow_non_circuit_action", "follow_along_action", followPayload("non_circuit_action", "block", 0, "action", "item", "exercise", null))
        addFollow("follow_circuit_rest", "follow_along_rest", followPayload("circuit_rest_after_action", "block", 1, "rest_after_action", "item", "exercise", 0))
        addFollow("follow_non_circuit_rest", "follow_along_rest", followPayload("non_circuit_rest_after_action", "block", 1, "rest_after_action", "item", "exercise", null))
        addFollow("follow_between_round", "follow_along_rest", followPayload("between_round_rest", "block", 2, "between_round_rest", null, null, 0))
        addFollow("follow_block_rest", "follow_along_rest", followPayload("block_rest", "block", 3, "block_rest", null, null, null))
        addFollow("follow_boundary", "follow_along_action", followPayload("boundary", "block", 4, "boundary", null, null, null))
        addFollow("follow_paused", "paused", followPayload("paused", null, null, null, null, null, null))
    }

    private fun envelope(
        family: String,
        payloadVersion: Int,
        mode: String,
        phaseKind: String,
        payload: String
    ): String =
        "{\"phaseIdentityContractVersion\":1,\"family\":\"$family\",\"payloadVersion\":$payloadVersion,\"mode\":\"$mode\",\"phaseKind\":\"$phaseKind\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"$DIGEST\"},\"payload\":$payload}"

    private fun legacyPayload(
        variant: String,
        blockId: String?,
        stepIndex0: Int?,
        blockKind: String?,
        stageType: String?,
        itemId: String?,
        exerciseId: String?,
        roundIndex0: Int?
    ): String =
        "{\"variant\":\"$variant\",\"blockId\":${stringOrNull(blockId)},\"stepIndex0\":${numberOrNull(stepIndex0)},\"legacyBlockKind\":${stringOrNull(blockKind)},\"legacyStageType\":${stringOrNull(stageType)},\"itemId\":${stringOrNull(itemId)},\"exerciseId\":${stringOrNull(exerciseId)},\"roundIndex0\":${numberOrNull(roundIndex0)}}"

    private fun compositionPayload(
        variant: String,
        blockId: String?,
        stageId: String?,
        stageKind: String?,
        groupId: String?,
        targetId: String?,
        targetKind: String?,
        roundIndex0: Int?,
        groupIndex0: Int?,
        targetIndex0: Int?,
        stageInstanceIndex0: Int?,
        targetOrdinal0: Int?,
        stepIndex0: Int?
    ): String =
        "{\"variant\":\"$variant\",\"compositionVersion\":2,\"compositionBlockId\":${stringOrNull(blockId)},\"$TIMELINE_STAGE_ID_KEY\":${stringOrNull(stageId)},\"timelineStageKind\":${stringOrNull(stageKind)},\"stageGroupId\":${stringOrNull(groupId)},\"targetId\":${stringOrNull(targetId)},\"targetKind\":${stringOrNull(targetKind)},\"roundIndex0\":${numberOrNull(roundIndex0)},\"stageGroupIndex0\":${numberOrNull(groupIndex0)},\"targetIndex0\":${numberOrNull(targetIndex0)},\"stageInstanceIndex0\":${numberOrNull(stageInstanceIndex0)},\"$TARGET_ORDINAL_KEY\":${numberOrNull(targetOrdinal0)},\"stepIndex0\":${numberOrNull(stepIndex0)}}"

    private fun strengthPayload(
        variant: String,
        blockId: String?,
        setPlanId: String?,
        planned: String?,
        actual: String?,
        exerciseSetIndex0: Int?,
        globalSetIndex0: Int?,
        setKind: String?,
        substitutedFrom: String?
    ): String =
        "{\"variant\":\"$variant\",\"blockId\":${stringOrNull(blockId)},\"setPlanId\":${stringOrNull(setPlanId)},\"plannedExerciseId\":${stringOrNull(planned)},\"actualExerciseId\":${stringOrNull(actual)},\"exerciseSetIndex0\":${numberOrNull(exerciseSetIndex0)},\"globalSetIndex0\":${numberOrNull(globalSetIndex0)},\"setKind\":${stringOrNull(setKind)},\"substitutedFromExerciseId\":${stringOrNull(substitutedFrom)}}"

    private fun followPayload(
        variant: String,
        blockId: String?,
        stepIndex0: Int?,
        stepKind: String?,
        itemId: String?,
        exerciseId: String?,
        roundIndex0: Int?
    ): String =
        "{\"variant\":\"$variant\",\"blockId\":${stringOrNull(blockId)},\"stepIndex0\":${numberOrNull(stepIndex0)},\"followAlongStepKind\":${stringOrNull(stepKind)},\"itemId\":${stringOrNull(itemId)},\"exerciseId\":${stringOrNull(exerciseId)},\"roundIndex0\":${numberOrNull(roundIndex0)}}"

    private fun stringOrNull(value: String?): String = value?.let { "\"$it\"" } ?: "null"
    private fun numberOrNull(value: Int?): String = value?.toString() ?: "null"

    private fun displayMetadata(vararg entries: String): String =
        "{\"displayMetadataContractVersion\":1,\"entries\":[${entries.joinToString(",")}] }".replace("] }", "]}")

    private fun entry(id: String, name: String, source: String): String =
        "{\"entityKind\":\"exercise\",\"stableId\":\"$id\",\"displayNameAtFirstReference\":\"$name\",\"customNameAtFirstReference\":null,\"resolutionSource\":\"$source\"}"

    private fun assertValid(result: CanonicalValidationResult) {
        assertEquals(CanonicalValidationResult.Valid, result)
    }

    private fun assertInvalid(result: CanonicalValidationResult) {
        assertTrue(result is CanonicalValidationResult.Invalid)
    }

    private data class PhaseFixture(
        val name: String,
        val phaseKind: String,
        val json: String
    )

    private companion object {
        const val TIMELINE_STAGE_ID_KEY = "timelineStage" + "Id"
        const val TARGET_ORDINAL_KEY = "targetInstance" + "Index0"
        const val DIGEST = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val DISPLAY_METADATA = "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
        const val ZONE_SNAPSHOT = "{\"zoneSnapshotContractVersion\":1,\"unit\":\"bpm\",\"effectiveMaxBpm\":180,\"effectiveMaxSource\":\"personal_max\",\"zones\":[{\"zoneId\":\"below_50\",\"lowerBoundBasisPointsInclusive\":null,\"upperBoundBasisPointsExclusive\":5000},{\"zoneId\":\"from_50_to_60\",\"lowerBoundBasisPointsInclusive\":5000,\"upperBoundBasisPointsExclusive\":6000},{\"zoneId\":\"from_60_to_70\",\"lowerBoundBasisPointsInclusive\":6000,\"upperBoundBasisPointsExclusive\":7000},{\"zoneId\":\"from_70_to_80\",\"lowerBoundBasisPointsInclusive\":7000,\"upperBoundBasisPointsExclusive\":8000},{\"zoneId\":\"from_80_to_90\",\"lowerBoundBasisPointsInclusive\":8000,\"upperBoundBasisPointsExclusive\":9000},{\"zoneId\":\"at_or_above_90\",\"lowerBoundBasisPointsInclusive\":9000,\"upperBoundBasisPointsExclusive\":null}]}"
        const val ANALYSIS_CONFIG = "{\"analysisConfigContractVersion\":1,\"sampleValidityCapMs\":2500,\"sampleIntervalContractVersion\":1,\"partialLowerBoundBasisPoints\":5000,\"phaseConclusionBasisPoints\":7000,\"normalBasisPoints\":8000,\"coverageThresholdRule\":\"checked_integer_cross_multiply\",\"coverageBasisPointsRule\":\"floor_integer_ratio\",\"displayPercentRule\":\"floor_basis_points_div_100\",\"weightedAverageRule\":\"checked_integer_time_integral\",\"averageDisplayRule\":\"positive_integer_half_up\",\"zeroCoveredRule\":\"null_integral_and_average\",\"observedMaxRule\":\"eligible_canonical_point_first_tie\",\"zoneAttributionContractVersion\":1,\"zoneAttributionRule\":\"checked_cross_multiply_six_zones\",\"statusProjectionContractVersion\":1,\"durationPartitionContractVersion\":1}"
        const val ZONE_DURATIONS = "{\"zoneDurationsContractVersion\":1,\"below50DurationMs\":0,\"from50To60DurationMs\":0,\"from60To70DurationMs\":0,\"from70To80DurationMs\":0,\"from80To90DurationMs\":0,\"atOrAbove90DurationMs\":0}"
        const val PHASE_AGGREGATES = "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[]}"
        const val PHASE_AGGREGATES_WITH_ENTRY = "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[{\"phaseSequence\":0,\"phaseKind\":\"timed_work\",\"eligibleDurationMs\":0,\"coveredDurationMs\":0,\"coverageBasisPoints\":null,\"coverageStatus\":\"no_eligible_duration\",\"conclusionEligible\":false,\"weightedBpmMs\":null,\"observedAvgBpm\":null,\"observedMaxBpm\":null,\"highestOffsetMs\":null,\"highestMutationSequence\":null,\"highestSampleSequence\":null}]}"
        const val DURATION_BREAKDOWN = "{\"durationBreakdownContractVersion\":1,\"canonicalSessionDurationMs\":0,\"recordingWindowDurationMs\":0,\"notRequestedBeforeRecordingStartMs\":0,\"intentAxis\":{\"expectedRecordingDurationMs\":0,\"userExcludedDurationMs\":0,\"userTurnedOffDurationMs\":0,\"userOptedOutDurationMs\":0,\"userDisconnectedSuppressRecoveryDurationMs\":0},\"phaseAxis\":{\"primaryEligibleDurationMs\":0,\"phaseExcludedDurationMs\":0,\"strengthPrepareExcludedDurationMs\":0,\"pausedExcludedDurationMs\":0},\"primaryAnalysisPartition\":{\"primaryEligibleDurationMs\":0,\"eligibleCoveredDurationMs\":0,\"eligibleUncoveredDurationMs\":0},\"deviceStateDurations\":{\"not_observing\":0,\"no_source_selected\":0,\"permission_required\":0,\"bluetooth_unavailable\":0,\"searching\":0,\"connecting\":0,\"waiting_first_sample\":0,\"live\":0,\"stale\":0,\"reconnecting\":0,\"disconnected\":0,\"technical_failure\":0},\"deviceReasonDurations\":{\"initial_acquisition\":0,\"automatic_recovery\":0,\"source_not_selected\":0,\"source_unavailable\":0,\"permission_missing\":0,\"permission_revoked\":0,\"bluetooth_off\":0,\"platform_unavailable\":0,\"first_sample_timeout\":0,\"sample_stale_timeout\":0,\"unexpected_disconnect\":0,\"connection_timeout\":0,\"measurement_stream_unavailable\":0,\"platform_failure\":0},\"orthogonalityContract\":{\"contractVersion\":1,\"rule\":\"primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum\"}}"
        const val QUALITY_REASONS = "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[],\"phaseReasons\":[]}"
    }
}
