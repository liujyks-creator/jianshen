package com.liujyks.trainflow.core.database

import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1ValidationResult
import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1Validator
import com.liujyks.trainflow.core.data.PlanSnapshotPhaseBlockFactsV1
import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1Result
import com.liujyks.trainflow.core.data.WorkoutPlanSnapshotStorageV1
import com.liujyks.trainflow.core.data.toStorageJson
import com.liujyks.trainflow.core.model.FollowAlongPlanMeta
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.RestBlock
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthExerciseTarget
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class CanonicalStorageJsonValidatorsTest {
    @Test
    fun allEightClosedStorageObjectsAcceptTheirCanonicalShapes() {
        listOf(
            CanonicalStorageJsonV1Validators.validateSessionDisplayMetadata(DISPLAY_METADATA),
            CanonicalStorageJsonV1Validators.validateZoneSnapshot(ZONE_SNAPSHOT),
            PhaseIdentityV1Validator.validateStructure(phaseFixtures().first().json),
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
    fun eachClosedStorageObjectRejectsTheCompleteStructuralMutationMatrix() {
        val cases = listOf(
            ClosedJsonCase(
                "session_display_metadata",
                DISPLAY_METADATA,
                "displayMetadataContractVersion",
                { json -> CanonicalStorageJsonV1Validators.validateSessionDisplayMetadata(json) }
            ),
            ClosedJsonCase(
                "zone_snapshot",
                ZONE_SNAPSHOT,
                "zoneSnapshotContractVersion",
                { json -> CanonicalStorageJsonV1Validators.validateZoneSnapshot(json) }
            ),
            ClosedJsonCase(
                "phase_identity",
                phaseFixtures().first().json,
                "phaseIdentityContractVersion",
                { json -> PhaseIdentityV1Validator.validateStructure(json) }
            ),
            ClosedJsonCase(
                "analysis_config",
                ANALYSIS_CONFIG,
                "analysisConfigContractVersion",
                { json -> CanonicalStorageJsonV1Validators.validateAnalysisConfig(json) }
            ),
            ClosedJsonCase(
                "zone_durations",
                ZONE_DURATIONS,
                "zoneDurationsContractVersion",
                { json -> CanonicalStorageJsonV1Validators.validateZoneDurations(json) }
            ),
            ClosedJsonCase(
                "phase_aggregates",
                PHASE_AGGREGATES,
                "phaseAggregatesContractVersion",
                { json -> CanonicalStorageJsonV1Validators.validatePhaseAggregates(json) }
            ),
            ClosedJsonCase(
                "duration_breakdown",
                DURATION_BREAKDOWN,
                "durationBreakdownContractVersion",
                { json -> CanonicalStorageJsonV1Validators.validateDurationBreakdown(json) }
            ),
            ClosedJsonCase(
                "quality_reasons",
                QUALITY_REASONS,
                "qualityReasonsContractVersion",
                { json -> CanonicalStorageJsonV1Validators.validateQualityReasons(json) }
            )
        )

        cases.forEach { case ->
            val version = "\"${case.versionKey}\":1"
            val mutations = linkedMapOf(
                "missing" to case.canonical.replace("$version,", ""),
                "extra" to case.canonical.dropLast(1) + ",\"extra\":true}",
                "wrong_type" to "[${case.canonical}]",
                "null" to case.canonical.replace(version, "\"${case.versionKey}\":null"),
                "unknown_version" to case.canonical.replace(version, "\"${case.versionKey}\":2"),
                "duplicate" to case.canonical.replace(version, "$version,$version"),
                "number_string" to case.canonical.replace(version, "\"${case.versionKey}\":\"1\""),
                "trailing" to case.canonical + " trailing"
            )
            mutations.forEach { (mutation, json) ->
                val result = case.validator(json)
                assertTrue(
                    "${case.name}/$mutation unexpectedly accepted",
                    result != CanonicalValidationResult.Valid
                )
            }
        }
    }

    @Test
    fun eachClosedObjectCoversRealRequiredNullableNestedAndArrayMembers() {
        fun memberToken(json: String, key: String, occurrence: Int = 0): String {
            val marker = "\"$key\":"
            var start = -1
            var searchFrom = 0
            repeat(occurrence + 1) {
                start = json.indexOf(marker, searchFrom)
                require(start >= 0) { "Missing test member $key occurrence $occurrence" }
                searchFrom = start + marker.length
            }
            val valueStart = start + marker.length
            var index = valueStart
            var inString = false
            var escaped = false
            var depth = 0
            while (index < json.length) {
                val char = json[index]
                if (inString) {
                    if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') inString = false
                } else {
                    when (char) {
                        '"' -> inString = true
                        '{', '[' -> depth += 1
                        '}', ']' -> if (depth > 0) depth -= 1 else break
                        ',' -> if (depth == 0) break
                    }
                }
                index += 1
            }
            return json.substring(start, index)
        }

        fun removeToken(json: String, token: String): String = when {
            json.contains("$token,") -> json.replaceFirst("$token,", "")
            json.contains(",$token") -> json.replaceFirst(",$token", "")
            else -> error("Token is not a removable member: $token")
        }

        fun assertRequiredMembers(
            label: String,
            json: String,
            validator: (String) -> CanonicalValidationResult,
            members: List<Pair<String, Int>>
        ) {
            assertValid(validator(json))
            members.forEach { (key, occurrence) ->
                val token = memberToken(json, key, occurrence)
                val value = token.substringAfter(':')
                val wrongValue = when (value.first()) {
                    '"' -> "0"
                    '{' -> "[]"
                    '[' -> "{}"
                    't', 'f' -> "\"wrong\""
                    else -> "\"wrong\""
                }
                linkedMapOf(
                    "missing" to removeToken(json, token),
                    "wrong_type" to json.replaceFirst(token, "\"$key\":$wrongValue"),
                    "null" to json.replaceFirst(token, "\"$key\":null"),
                    "duplicate" to json.replaceFirst(token, "$token,$token")
                ).forEach { (mutation, candidate) ->
                    assertTrue(
                        "$label.$key[$occurrence]/$mutation unexpectedly accepted",
                        validator(candidate) != CanonicalValidationResult.Valid
                    )
                }
            }
        }

        fun assertNullableMembers(
            label: String,
            json: String,
            validator: (String) -> CanonicalValidationResult,
            members: List<Pair<String, Int>>
        ) {
            assertValid(validator(json))
            members.forEach { (key, occurrence) ->
                val token = memberToken(json, key, occurrence)
                assertTrue("$label.$key[$occurrence] is not the intended NULL fixture", token.endsWith(":null"))
                linkedMapOf(
                    "wrong_type" to json.replaceFirst(token, "\"$key\":{}"),
                    "duplicate" to json.replaceFirst(token, "$token,$token")
                ).forEach { (mutation, candidate) ->
                    assertTrue(
                        "$label.$key[$occurrence]/$mutation unexpectedly accepted",
                        validator(candidate) != CanonicalValidationResult.Valid
                    )
                }
            }
        }

        val displayWithEntry = displayMetadata(entry("exercise-1", "深蹲", "plan_snapshot"))
        assertRequiredMembers(
            "session_display_metadata",
            displayWithEntry,
            CanonicalStorageJsonV1Validators::validateSessionDisplayMetadata,
            listOf("entries", "entityKind", "stableId", "displayNameAtFirstReference", "resolutionSource").map { it to 0 }
        )
        assertNullableMembers(
            "session_display_metadata",
            displayWithEntry,
            CanonicalStorageJsonV1Validators::validateSessionDisplayMetadata,
            listOf("customNameAtFirstReference" to 0)
        )
        assertRequiredMembers(
            "zone_snapshot",
            ZONE_SNAPSHOT,
            CanonicalStorageJsonV1Validators::validateZoneSnapshot,
            listOf("unit", "effectiveMaxBpm", "effectiveMaxSource", "zones", "zoneId", "upperBoundBasisPointsExclusive").map { it to 0 }
        )
        assertNullableMembers(
            "zone_snapshot",
            ZONE_SNAPSHOT,
            CanonicalStorageJsonV1Validators::validateZoneSnapshot,
            listOf("lowerBoundBasisPointsInclusive" to 0, "upperBoundBasisPointsExclusive" to 5)
        )

        val phaseIdentity = phaseFixtures().first().json
        assertRequiredMembers(
            "phase_identity",
            phaseIdentity,
            PhaseIdentityV1Validator::validateStructure,
            listOf(
                "family", "payloadVersion", "mode", "phaseKind", "orderedStructureSignature",
                "signatureContractVersion", "algorithm", "digestHexLowercase", "payload", "variant",
                "blockId", "stepIndex0", "legacyBlockKind", "legacyStageType"
            ).map { it to 0 }
        )
        assertNullableMembers(
            "phase_identity",
            phaseIdentity,
            PhaseIdentityV1Validator::validateStructure,
            listOf("itemId", "exerciseId", "roundIndex0").map { it to 0 }
        )

        assertRequiredMembers(
            "analysis_config",
            ANALYSIS_CONFIG,
            CanonicalStorageJsonV1Validators::validateAnalysisConfig,
            listOf(
                "sampleValidityCapMs", "sampleIntervalContractVersion", "partialLowerBoundBasisPoints",
                "phaseConclusionBasisPoints", "normalBasisPoints", "coverageThresholdRule",
                "coverageBasisPointsRule", "displayPercentRule", "weightedAverageRule",
                "averageDisplayRule", "zeroCoveredRule", "observedMaxRule",
                "zoneAttributionContractVersion", "zoneAttributionRule",
                "statusProjectionContractVersion", "durationPartitionContractVersion"
            ).map { it to 0 }
        )
        assertRequiredMembers(
            "zone_durations",
            ZONE_DURATIONS,
            CanonicalStorageJsonV1Validators::validateZoneDurations,
            listOf(
                "below50DurationMs", "from50To60DurationMs", "from60To70DurationMs",
                "from70To80DurationMs", "from80To90DurationMs", "atOrAbove90DurationMs"
            ).map { it to 0 }
        )

        assertRequiredMembers(
            "phase_aggregates",
            PHASE_AGGREGATES_WITH_ENTRY,
            CanonicalStorageJsonV1Validators::validatePhaseAggregates,
            listOf(
                "aggregates", "phaseSequence", "phaseKind", "eligibleDurationMs", "coveredDurationMs",
                "coverageStatus", "conclusionEligible"
            ).map { it to 0 }
        )
        assertNullableMembers(
            "phase_aggregates",
            PHASE_AGGREGATES_WITH_ENTRY,
            CanonicalStorageJsonV1Validators::validatePhaseAggregates,
            listOf(
                "coverageBasisPoints", "weightedBpmMs", "observedAvgBpm", "observedMaxBpm",
                "highestOffsetMs", "highestMutationSequence", "highestSampleSequence"
            ).map { it to 0 }
        )

        assertRequiredMembers(
            "duration_breakdown",
            DURATION_BREAKDOWN,
            CanonicalStorageJsonV1Validators::validateDurationBreakdown,
            listOf(
                "canonicalSessionDurationMs", "recordingWindowDurationMs", "notRequestedBeforeRecordingStartMs",
                "intentAxis", "expectedRecordingDurationMs", "userExcludedDurationMs", "userTurnedOffDurationMs",
                "userOptedOutDurationMs", "userDisconnectedSuppressRecoveryDurationMs", "phaseAxis",
                "primaryEligibleDurationMs", "phaseExcludedDurationMs", "strengthPrepareExcludedDurationMs",
                "pausedExcludedDurationMs", "primaryAnalysisPartition", "eligibleCoveredDurationMs",
                "eligibleUncoveredDurationMs", "deviceStateDurations", "not_observing", "no_source_selected",
                "permission_required", "bluetooth_unavailable", "searching", "connecting", "waiting_first_sample",
                "live", "stale", "reconnecting", "disconnected", "technical_failure", "deviceReasonDurations",
                "initial_acquisition", "automatic_recovery", "source_not_selected", "source_unavailable",
                "permission_missing", "permission_revoked", "bluetooth_off", "platform_unavailable",
                "first_sample_timeout", "sample_stale_timeout", "unexpected_disconnect", "connection_timeout",
                "measurement_stream_unavailable", "platform_failure", "orthogonalityContract", "contractVersion", "rule"
            ).map { it to 0 }
        )

        val qualityWithEntries =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[{\"reasonCode\":\"unavailable_no_effective_max\",\"durationMs\":null}],\"phaseReasons\":[{\"phaseSequence\":0,\"reasonCode\":\"paused_excluded\",\"durationMs\":5}]}"
        assertRequiredMembers(
            "quality_reasons",
            qualityWithEntries,
            CanonicalStorageJsonV1Validators::validateQualityReasons,
            listOf("sessionReasons" to 0, "phaseReasons" to 0, "reasonCode" to 0, "phaseSequence" to 0, "reasonCode" to 1)
        )
        val qualityWithNullDurations = qualityWithEntries.replace("\"durationMs\":5", "\"durationMs\":null")
        assertNullableMembers(
            "quality_reasons",
            qualityWithNullDurations,
            CanonicalStorageJsonV1Validators::validateQualityReasons,
            listOf("durationMs" to 0, "durationMs" to 1)
        )
    }

    @Test
    fun closedJsonArrayElementsAndNestedObjectsRejectCompleteStructuralMutations() {
        fun assertMutationsInvalid(
            label: String,
            validator: (String) -> CanonicalValidationResult,
            mutations: List<String>
        ) {
            mutations.forEachIndexed { index, json ->
                assertTrue(
                    "$label mutation $index unexpectedly accepted",
                    validator(json) != CanonicalValidationResult.Valid
                )
            }
        }

        val displayEntry = entry("exercise-1", "深蹲", "plan_snapshot")
        val display = displayMetadata(displayEntry)
        assertValid(CanonicalStorageJsonV1Validators.validateSessionDisplayMetadata(display))
        assertMutationsInvalid(
            "entries[]",
            CanonicalStorageJsonV1Validators::validateSessionDisplayMetadata,
            listOf(
                display.replace("\"entityKind\":\"exercise\",", ""),
                display.replace("\"resolutionSource\":\"plan_snapshot\"", "\"resolutionSource\":\"plan_snapshot\",\"extra\":true"),
                display.replace(displayEntry, "0"),
                display.replace(displayEntry, "null"),
                display.replace(displayEntry, "$displayEntry,$displayEntry")
            )
        )

        val firstZone =
            "{\"zoneId\":\"below_50\",\"lowerBoundBasisPointsInclusive\":null,\"upperBoundBasisPointsExclusive\":5000}"
        assertMutationsInvalid(
            "zones[]",
            CanonicalStorageJsonV1Validators::validateZoneSnapshot,
            listOf(
                ZONE_SNAPSHOT.replace("\"zoneId\":\"below_50\",", ""),
                ZONE_SNAPSHOT.replace(firstZone, firstZone.dropLast(1) + ",\"extra\":true}"),
                ZONE_SNAPSHOT.replace(firstZone, "0"),
                ZONE_SNAPSHOT.replace(firstZone, "null"),
                ZONE_SNAPSHOT.replace(firstZone, "$firstZone,$firstZone")
            )
        )

        val aggregate = PHASE_AGGREGATES_WITH_ENTRY
            .substringAfter("\"aggregates\":[")
            .dropLast(2)
        assertMutationsInvalid(
            "aggregates[]",
            CanonicalStorageJsonV1Validators::validatePhaseAggregates,
            listOf(
                PHASE_AGGREGATES_WITH_ENTRY.replace("\"phaseKind\":\"timed_work\",", ""),
                PHASE_AGGREGATES_WITH_ENTRY.replace(aggregate, aggregate.dropLast(1) + ",\"extra\":true}"),
                PHASE_AGGREGATES_WITH_ENTRY.replace(aggregate, "0"),
                PHASE_AGGREGATES_WITH_ENTRY.replace(aggregate, "null"),
                PHASE_AGGREGATES_WITH_ENTRY.replace(aggregate, "$aggregate,$aggregate")
            )
        )

        val sessionReason = "{\"reasonCode\":\"unavailable_no_effective_max\",\"durationMs\":null}"
        val phaseReason = "{\"phaseSequence\":0,\"reasonCode\":\"paused_excluded\",\"durationMs\":5}"
        val quality =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[$sessionReason],\"phaseReasons\":[$phaseReason]}"
        assertValid(CanonicalStorageJsonV1Validators.validateQualityReasons(quality))
        assertMutationsInvalid(
            "sessionReasons[]",
            CanonicalStorageJsonV1Validators::validateQualityReasons,
            listOf(
                quality.replace("\"reasonCode\":\"unavailable_no_effective_max\",", ""),
                quality.replace(sessionReason, sessionReason.dropLast(1) + ",\"extra\":true}"),
                quality.replace(sessionReason, "0"),
                quality.replace(sessionReason, "null"),
                quality.replace(sessionReason, "$sessionReason,$sessionReason")
            )
        )
        assertMutationsInvalid(
            "phaseReasons[]",
            CanonicalStorageJsonV1Validators::validateQualityReasons,
            listOf(
                quality.replace("\"phaseSequence\":0,", ""),
                quality.replace(phaseReason, phaseReason.dropLast(1) + ",\"extra\":true}"),
                quality.replace(phaseReason, "0"),
                quality.replace(phaseReason, "null"),
                quality.replace(phaseReason, "$phaseReason,$phaseReason")
            )
        )

        assertMutationsInvalid(
            "duration nested objects",
            CanonicalStorageJsonV1Validators::validateDurationBreakdown,
            listOf(
                DURATION_BREAKDOWN.replace("\"userDisconnectedSuppressRecoveryDurationMs\":0}", "\"userDisconnectedSuppressRecoveryDurationMs\":0,\"extra\":0}"),
                DURATION_BREAKDOWN.replace("\"pausedExcludedDurationMs\":0}", "\"pausedExcludedDurationMs\":0,\"extra\":0}"),
                DURATION_BREAKDOWN.replace("\"eligibleUncoveredDurationMs\":0}", "\"eligibleUncoveredDurationMs\":0,\"extra\":0}"),
                DURATION_BREAKDOWN.replace("\"technical_failure\":0}", "\"technical_failure\":0,\"extra\":0}"),
                DURATION_BREAKDOWN.replace("\"platform_failure\":0}", "\"platform_failure\":0,\"extra\":0}"),
                DURATION_BREAKDOWN.replace("\"rule\":\"primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum\"}", "\"rule\":\"primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum\",\"extra\":0}")
            )
        )
    }

    @Test
    fun allEightClosedJsonObjectsUseTheirAcceptedNumberGrammarThroughTheRealParser() {
        data class NumberGrammarCase(
            val label: String,
            val canonical: String,
            val token: String,
            val exponent: String,
            val negativeZero: String,
            val negativeZeroValid: Boolean,
            val validator: (String) -> CanonicalValidationResult
        )

        val cases = listOf(
            NumberGrammarCase("display", DISPLAY_METADATA, "\"displayMetadataContractVersion\":1", "\"displayMetadataContractVersion\":1e0", "\"displayMetadataContractVersion\":-0", false, CanonicalStorageJsonV1Validators::validateSessionDisplayMetadata),
            NumberGrammarCase("zone", ZONE_SNAPSHOT, "\"effectiveMaxBpm\":180", "\"effectiveMaxBpm\":180e0", "\"effectiveMaxBpm\":-0", false, CanonicalStorageJsonV1Validators::validateZoneSnapshot),
            NumberGrammarCase("phase_identity", phaseFixtures().first { it.name == "legacy_circuit_work" }.json, "\"stepIndex0\":0", "\"stepIndex0\":0e0", "\"stepIndex0\":-0", true, PhaseIdentityV1Validator::validateStructure),
            NumberGrammarCase("analysis", ANALYSIS_CONFIG, "\"sampleValidityCapMs\":2500", "\"sampleValidityCapMs\":2500e0", "\"sampleValidityCapMs\":-0", false, CanonicalStorageJsonV1Validators::validateAnalysisConfig),
            NumberGrammarCase("zone_duration", ZONE_DURATIONS, "\"below50DurationMs\":0", "\"below50DurationMs\":0e0", "\"below50DurationMs\":-0", true, CanonicalStorageJsonV1Validators::validateZoneDurations),
            NumberGrammarCase("aggregate", PHASE_AGGREGATES_WITH_ENTRY, "\"phaseSequence\":0", "\"phaseSequence\":0e0", "\"phaseSequence\":-0", true, CanonicalStorageJsonV1Validators::validatePhaseAggregates),
            NumberGrammarCase("duration", DURATION_BREAKDOWN, "\"canonicalSessionDurationMs\":0", "\"canonicalSessionDurationMs\":0e0", "\"canonicalSessionDurationMs\":-0", true, CanonicalStorageJsonV1Validators::validateDurationBreakdown),
            NumberGrammarCase("quality", "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[],\"phaseReasons\":[{\"phaseSequence\":0,\"reasonCode\":\"paused_excluded\",\"durationMs\":null}]}", "\"phaseSequence\":0", "\"phaseSequence\":0e0", "\"phaseSequence\":-0", true, CanonicalStorageJsonV1Validators::validateQualityReasons)
        )

        cases.forEach { case ->
            assertValid(case.validator(case.canonical))
            assertTrue(
                "${case.label} leading zero accepted",
                case.validator(case.canonical.replace(case.token, case.token.substringBefore(':') + ":00")) !=
                    CanonicalValidationResult.Valid
            )
            assertTrue(
                "${case.label} fractional non-integer accepted",
                case.validator(case.canonical.replace(case.token, case.token.substringBefore(':') + ":0.5")) !=
                    CanonicalValidationResult.Valid
            )
            assertValid(case.validator(case.canonical.replace(case.token, case.exponent)))
            val negativeZeroResult = case.validator(case.canonical.replace(case.token, case.negativeZero))
            assertEquals(
                "${case.label} negative-zero decision changed",
                case.negativeZeroValid,
                negativeZeroResult == CanonicalValidationResult.Valid
            )
        }
    }

    @Test
    fun compositionStrengthAndFollowPayloadUnionsExposeEveryMemberAndNullableBranch() {
        fun fieldToken(json: String, key: String): String {
            val start = json.indexOf("\"$key\":")
            require(start >= 0) { "Missing $key" }
            var index = start + key.length + 3
            var inString = false
            var escaped = false
            var depth = 0
            while (index < json.length) {
                val char = json[index]
                if (inString) {
                    if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') inString = false
                } else {
                    when (char) {
                        '"' -> inString = true
                        '{', '[' -> depth += 1
                        '}', ']' -> if (depth > 0) depth -= 1 else break
                        ',' -> if (depth == 0) break
                    }
                }
                index += 1
            }
            return json.substring(start, index)
        }
        fun withoutToken(json: String, token: String): String = when {
            json.contains("$token,") -> json.replaceFirst("$token,", "")
            json.contains(",$token") -> json.replaceFirst(",$token", "")
            else -> error("Cannot remove $token")
        }
        fun assertClosedPayload(fixture: PhaseFixture, nonNullMembers: List<String>, allMembers: List<String>) {
            assertValid(PhaseIdentityV1Validator.validateStructure(fixture.json, fixture.phaseKind))
            allMembers.forEach { key ->
                val token = fieldToken(fixture.json.substringAfter("\"payload\":"), key)
                assertInvalid(PhaseIdentityV1Validator.validateStructure(withoutToken(fixture.json, token)))
                assertInvalid(PhaseIdentityV1Validator.validateStructure(fixture.json.replaceFirst(token, "$token,$token")))
            }
            nonNullMembers.forEach { key ->
                val token = fieldToken(fixture.json.substringAfter("\"payload\":"), key)
                assertInvalid(
                    PhaseIdentityV1Validator.validateStructure(
                        fixture.json.replaceFirst(token, "\"$key\":null")
                    )
                )
            }
        }

        val fixtures = phaseFixtures()
        val compositionMembers = listOf(
            "variant", "compositionVersion", "compositionBlockId", TIMELINE_STAGE_ID_KEY,
            "timelineStageKind", "stageGroupId", "targetId", "targetKind", "roundIndex0",
            "stageGroupIndex0", "targetIndex0", "stageInstanceIndex0", TARGET_ORDINAL_KEY, "stepIndex0"
        )
        assertClosedPayload(
            fixtures.first { it.name == "composition_action" },
            compositionMembers,
            compositionMembers
        )
        val strengthMembers = listOf(
            "variant", "blockId", "setPlanId", "plannedExerciseId", "actualExerciseId",
            "exerciseSetIndex0", "globalSetIndex0", "setKind", "substitutedFromExerciseId"
        )
        assertClosedPayload(
            fixtures.first { it.name == "strength_active_set" },
            strengthMembers.minus("substitutedFromExerciseId"),
            strengthMembers
        )
        val substitutedStrength = envelope(
            "strength_v1",
            1,
            "strength",
            "strength_active_set",
            strengthPayload("active_set", "block", "set", "squat", "front-squat", 0, 0, "working", "squat")
        )
        assertValid(PhaseIdentityV1Validator.validateStructure(substitutedStrength, "strength_active_set"))

        val followMembers = listOf(
            "variant", "blockId", "stepIndex0", "followAlongStepKind", "itemId", "exerciseId", "roundIndex0"
        )
        assertClosedPayload(
            fixtures.first { it.name == "follow_circuit_action" },
            followMembers,
            followMembers
        )
        listOf("composition_paused", "strength_paused", "follow_paused", "follow_non_circuit_action").forEach { name ->
            val fixture = fixtures.first { it.name == name }
            assertValid(PhaseIdentityV1Validator.validateStructure(fixture.json, fixture.phaseKind))
        }
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
            val result = PhaseIdentityV1Validator.validateStructure(fixture.json, fixture.phaseKind)
            assertTrue("${fixture.name} failed with $result", result is CanonicalValidationResult.Valid)
        }
    }

    @Test
    fun pausedVariantIndicesAndSignatureAreStrict() {
        val strengthPaused = phaseFixtures().first { it.name == "strength_paused" }.json
        val followPaused = phaseFixtures().first { it.name == "follow_paused" }.json
        val compositionWarmup = phaseFixtures().first { it.name == "composition_warmup" }.json

        assertInvalid(
            PhaseIdentityV1Validator.validateStructure(
                strengthPaused.replace("\"variant\":\"paused\",", "")
            )
        )
        assertInvalid(
            PhaseIdentityV1Validator.validateStructure(
                followPaused.replace("\"blockId\":null", "\"blockId\":\"unexpected\"")
            )
        )
        assertInvalid(
            PhaseIdentityV1Validator.validateStructure(
                compositionWarmup.replace("\"stepIndex0\":0", "\"stepIndex0\":-1")
            )
        )
        assertInvalid(
            PhaseIdentityV1Validator.validateStructure(
                compositionWarmup.replace(DIGEST, DIGEST.uppercase())
            )
        )
    }

    @Test
    fun shapeOnlyDigestCannotValidateAPhaseIdentity() {
        val shapeOnlyIdentity = phaseFixtures().first { it.name == "composition_warmup" }.json
        val snapshotJson =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Timed\",\"mode\":\"timed\",\"blocks\":[],\"preferences\":null,\"followAlong\":null}"
        val snapshot = (PlanSnapshotStorageV1Validator.validate(snapshotJson, WorkoutMode.TIMED) as
            PlanSnapshotStorageV1ValidationResult.Valid).storage

        assertInvalid(PhaseIdentityV1Validator.validate(shapeOnlyIdentity, snapshot))
    }

    @Test
    fun phaseIdentityPayloadMustBindStableIdsKindsAndIndicesToTheSameSnapshot() {
        val snapshotJson =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Timed\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"composition\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":0,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[{\"id\":\"group\",\"order\":0,\"name\":\"Display\",\"colorHex\":\"#111111\",\"targets\":[{\"id\":\"target\",\"order\":0,\"name\":\"Display\",\"kind\":\"action\",\"durationSec\":30,\"colorHex\":\"#222222\",\"autoAdvance\":true}]}]}],\"preferences\":null,\"followAlong\":null}"
        val expectedProjection =
            "{\"signatureInputContractVersion\":1,\"mode\":\"timed\",\"blocks\":[{\"blockId\":\"composition\",\"blockKind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":0,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[{\"stageGroupId\":\"group\",\"order\":0,\"targets\":[{\"targetId\":\"target\",\"order\":0,\"targetKind\":\"action\",\"durationSec\":30,\"autoAdvance\":true}]}]}]}"
        val digest = testSha256(expectedProjection.toByteArray(Charsets.UTF_8))
        val payload = compositionPayload(
            "stage_group_action",
            "composition",
            "composition:r1:g1:group",
            "stage_group",
            "group",
            "target",
            "action",
            0,
            0,
            0,
            0,
            0,
            0
        )
        val identity = envelope(
            "timed_composition_v2",
            2,
            "timed",
            "timed_work",
            payload,
            digest
        )
        val snapshot = (PlanSnapshotStorageV1Validator.validate(snapshotJson, WorkoutMode.TIMED) as
            PlanSnapshotStorageV1ValidationResult.Valid).storage

        assertValid(PhaseIdentityV1Validator.validate(identity, snapshot, "timed_work"))
        listOf(
            identity.replace("\"compositionBlockId\":\"composition\"", "\"compositionBlockId\":\"other\""),
            identity.replace("\"stageGroupId\":\"group\"", "\"stageGroupId\":\"other\""),
            identity.replace("\"targetId\":\"target\"", "\"targetId\":\"other\""),
            identity.replace("\"targetKind\":\"action\"", "\"targetKind\":\"custom\""),
            identity.replace("\"roundIndex0\":0", "\"roundIndex0\":1"),
            identity.replace("\"stageGroupIndex0\":0", "\"stageGroupIndex0\":1"),
            identity.replace("\"targetIndex0\":0", "\"targetIndex0\":1")
        ).forEachIndexed { index, mutation ->
            assertTrue(
                "Stable snapshot binding mutation $index unexpectedly validated",
                PhaseIdentityV1Validator.validate(mutation, snapshot, "timed_work") is
                    CanonicalValidationResult.Invalid
            )
        }
    }

    @Test
    fun allPhaseFamiliesBindSnapshotBoundariesCircuitsRestsSubstitutionsAndPaused() {
        fun validated(snapshot: WorkoutPlanSnapshot) =
            (PlanSnapshotStorageV1Validator.validate(snapshot.toStorageJson(), snapshot.mode) as
                PlanSnapshotStorageV1ValidationResult.Valid).storage
        fun digest(projection: String) = testSha256(projection.toByteArray(Charsets.UTF_8))
        fun assertBound(identity: String, snapshot: WorkoutPlanSnapshotStorageV1, kind: String) {
            val context = requireNotNull(PhaseIdentityV1Validator.prepareContext(snapshot))
            val standalone = PhaseIdentityV1Validator.validate(identity, snapshot, kind)
            val prepared = PhaseIdentityV1Validator.validatePrepared(identity, context, kind)
            assertEquals(standalone, prepared)
            assertValid(prepared)
        }

        val item = TimedExerciseItem(
            id = "item",
            exerciseId = "exercise",
            stageType = TimedStageType.WORK,
            workDurationSec = 20,
            restAfterSec = 10,
            autoAdvance = true
        )
        val sharedBlocks = listOf(
            WarmupBlock("warmup", 0, durationSec = 10),
            StretchBlock("stretch", 1, items = listOf(item)),
            TimedCircuitBlock("circuit", 2, rounds = 2, restBetweenRoundsSec = 5, items = listOf(item)),
            RestBlock("rest", 3, durationSec = 10)
        )
        val sharedProjectionBlocks =
            "[{\"blockId\":\"warmup\",\"blockKind\":\"warmup\",\"order\":0,\"durationSec\":10,\"items\":[]},{\"blockId\":\"stretch\",\"blockKind\":\"stretch\",\"order\":1,\"durationSec\":null,\"items\":[{\"itemId\":\"item\",\"exerciseId\":\"exercise\",\"side\":null,\"stageType\":\"work\",\"workDurationSec\":20,\"restAfterSec\":10,\"autoAdvance\":true}]},{\"blockId\":\"circuit\",\"blockKind\":\"timed_circuit\",\"order\":2,\"rounds\":2,\"restBetweenRoundsSec\":5,\"items\":[{\"itemId\":\"item\",\"exerciseId\":\"exercise\",\"side\":null,\"stageType\":\"work\",\"workDurationSec\":20,\"restAfterSec\":10,\"autoAdvance\":true}]},{\"blockId\":\"rest\",\"blockKind\":\"rest\",\"order\":3,\"durationSec\":10}]"

        val timedSnapshot = validated(WorkoutPlanSnapshot(title = "Timed", mode = WorkoutMode.TIMED, blocks = sharedBlocks))
        val timedDigest = digest("{\"signatureInputContractVersion\":1,\"mode\":\"timed\",\"blocks\":$sharedProjectionBlocks}")
        val legacyIdentities = listOf(
            "timed_work" to legacyPayload("boundary_block_work", "warmup", 0, "warmup", "warmup", null, null, null),
            "timed_work" to legacyPayload("boundary_item_work", "stretch", 0, "stretch", "work", "item", "exercise", null),
            "timed_work" to legacyPayload("circuit_item_work", "circuit", 0, "timed_circuit", "work", "item", "exercise", 0),
            "timed_rest" to legacyPayload("circuit_rest_after_item", "circuit", 1, "timed_circuit", "rest", "item", "exercise", 0),
            "timed_rest" to legacyPayload("between_round_rest", "circuit", 2, "timed_circuit", "rest", null, null, 0),
            "timed_rest" to legacyPayload("standalone_rest", "rest", 0, "rest", "rest", null, null, null),
            "paused" to legacyPayload("paused", null, null, null, null, null, null, null)
        ).map { (kind, payload) -> kind to envelope("legacy_timed_v1", 1, "timed", kind, payload, timedDigest) }
        legacyIdentities.forEach { (kind, identity) -> assertBound(identity, timedSnapshot, kind) }
        linkedMapOf(
            "legacy.blockId" to legacyIdentities[2].second.replace("\"blockId\":\"circuit\"", "\"blockId\":\"other\""),
            "legacy.blockKind" to legacyIdentities[2].second.replace("\"legacyBlockKind\":\"timed_circuit\"", "\"legacyBlockKind\":\"stretch\""),
            "legacy.itemId" to legacyIdentities[2].second.replace("\"itemId\":\"item\"", "\"itemId\":\"other\""),
            "legacy.exerciseId" to legacyIdentities[2].second.replace("\"exerciseId\":\"exercise\"", "\"exerciseId\":\"other\""),
            "legacy.roundIndex0" to legacyIdentities[2].second.replace("\"roundIndex0\":0", "\"roundIndex0\":2"),
            "legacy.stepIndex0" to legacyIdentities[2].second.replace("\"stepIndex0\":0", "\"stepIndex0\":1")
        ).forEach { (field, identity) ->
            assertTrue("$field mismatch validated", PhaseIdentityV1Validator.validate(identity, timedSnapshot) is CanonicalValidationResult.Invalid)
        }

        val followSnapshot = validated(
            WorkoutPlanSnapshot(
                title = "Follow",
                mode = WorkoutMode.FOLLOW_ALONG,
                blocks = sharedBlocks,
                followAlong = FollowAlongPlanMeta(preset = true)
            )
        )
        val followDigest = digest("{\"signatureInputContractVersion\":1,\"mode\":\"follow_along\",\"blocks\":$sharedProjectionBlocks}")
        val followIdentities = listOf(
            "follow_along_action" to followPayload("non_circuit_action", "stretch", 0, "action", "item", "exercise", null),
            "follow_along_action" to followPayload("circuit_action", "circuit", 0, "action", "item", "exercise", 0),
            "follow_along_rest" to followPayload("circuit_rest_after_action", "circuit", 1, "rest_after_action", "item", "exercise", 0),
            "follow_along_rest" to followPayload("non_circuit_rest_after_action", "stretch", 1, "rest_after_action", "item", "exercise", null),
            "follow_along_rest" to followPayload("between_round_rest", "circuit", 2, "between_round_rest", null, null, 0),
            "follow_along_rest" to followPayload("block_rest", "rest", 0, "block_rest", null, null, null),
            "follow_along_action" to followPayload("boundary", "warmup", 0, "boundary", null, null, null),
            "paused" to followPayload("paused", null, null, null, null, null, null)
        ).map { (kind, payload) -> kind to envelope("follow_along_v1", 1, "follow_along", kind, payload, followDigest) }
        followIdentities.forEach { (kind, identity) -> assertBound(identity, followSnapshot, kind) }
        linkedMapOf(
            "follow.blockId" to followIdentities[1].second.replace("\"blockId\":\"circuit\"", "\"blockId\":\"other\""),
            "follow.itemId" to followIdentities[1].second.replace("\"itemId\":\"item\"", "\"itemId\":\"other\""),
            "follow.exerciseId" to followIdentities[1].second.replace("\"exerciseId\":\"exercise\"", "\"exerciseId\":\"other\""),
            "follow.roundIndex0" to followIdentities[1].second.replace("\"roundIndex0\":0", "\"roundIndex0\":2"),
            "follow.stepIndex0" to followIdentities[1].second.replace("\"stepIndex0\":0", "\"stepIndex0\":1")
        ).forEach { (field, identity) ->
            assertTrue("$field mismatch validated", PhaseIdentityV1Validator.validate(identity, followSnapshot) is CanonicalValidationResult.Invalid)
        }

        val strengthSnapshot = validated(
            WorkoutPlanSnapshot(
                title = "Strength",
                mode = WorkoutMode.STRENGTH,
                blocks = listOf(
                    StrengthExerciseBlock(
                        id = "strength",
                        order = 0,
                        exerciseId = "squat",
                        target = StrengthExerciseTarget(WeightValue(60.0, WeightUnit.KG), RepTarget.Fixed(10), 90),
                        sets = listOf(StrengthSetPlan("set", 0, StrengthSetKind.WORKING)),
                        substitutions = listOf("front-squat")
                    )
                )
            )
        )
        val strengthProjection =
            "{\"signatureInputContractVersion\":1,\"mode\":\"strength\",\"blocks\":[{\"blockId\":\"strength\",\"blockKind\":\"strength_exercise\",\"order\":0,\"exerciseId\":\"squat\",\"target\":{\"weight\":{\"value\":60,\"unit\":\"kg\"},\"repTarget\":{\"kind\":\"fixed\",\"fixedReps\":10,\"minReps\":null,\"maxReps\":null},\"restAfterSetSec\":90},\"sets\":[{\"setPlanId\":\"set\",\"order\":0,\"setKind\":\"working\",\"side\":null,\"targetWeight\":null,\"repTarget\":null,\"restAfterSec\":null}],\"substitutions\":[\"front-squat\"],\"setTimerMode\":\"manual_start\"}]}"
        val strengthDigest = digest(strengthProjection)
        listOf(
            "prepare_set" to "strength_prepare_set",
            "active_set" to "strength_active_set",
            "confirm_set" to "strength_confirm_set",
            "rest" to "strength_rest"
        ).forEach { (variant, kind) ->
            assertBound(
                envelope("strength_v1", 1, "strength", kind, strengthPayload(variant, "strength", "set", "squat", "squat", 0, 0, "working", null), strengthDigest),
                strengthSnapshot,
                kind
            )
        }
        val substituted = envelope(
            "strength_v1", 1, "strength", "strength_active_set",
            strengthPayload("active_set", "strength", "set", "squat", "front-squat", 0, 0, "working", "squat"),
            strengthDigest
        )
        assertBound(substituted, strengthSnapshot, "strength_active_set")
        assertBound(
            envelope("strength_v1", 1, "strength", "paused", strengthPayload("paused", null, null, null, null, null, null, null, null), strengthDigest),
            strengthSnapshot,
            "paused"
        )
        linkedMapOf(
            "strength.blockId" to substituted.replace("\"blockId\":\"strength\"", "\"blockId\":\"other\""),
            "strength.setPlanId" to substituted.replace("\"setPlanId\":\"set\"", "\"setPlanId\":\"other\""),
            "strength.plannedExerciseId" to substituted.replace("\"plannedExerciseId\":\"squat\"", "\"plannedExerciseId\":\"other\""),
            "strength.actualExerciseId" to substituted.replace("\"actualExerciseId\":\"front-squat\"", "\"actualExerciseId\":\"unlisted\""),
            "strength.exerciseSetIndex0" to substituted.replace("\"exerciseSetIndex0\":0", "\"exerciseSetIndex0\":1"),
            "strength.globalSetIndex0" to substituted.replace("\"globalSetIndex0\":0", "\"globalSetIndex0\":1"),
            "strength.setKind" to substituted.replace("\"setKind\":\"working\"", "\"setKind\":\"warmup\"")
        ).forEach { (field, identity) ->
            val context = requireNotNull(PhaseIdentityV1Validator.prepareContext(strengthSnapshot))
            val standalone = PhaseIdentityV1Validator.validate(identity, strengthSnapshot)
            val prepared = PhaseIdentityV1Validator.validatePrepared(identity, context)
            assertEquals("$field changed prepared semantics", standalone, prepared)
            assertTrue("$field mismatch validated", prepared is CanonicalValidationResult.Invalid)
        }
    }

    @Test
    fun followActionAndRestAfterActionRequireAnActionSnapshotItem() {
        fun validated(snapshot: WorkoutPlanSnapshot) =
            (PlanSnapshotStorageV1Validator.validate(snapshot.toStorageJson(), snapshot.mode) as
                PlanSnapshotStorageV1ValidationResult.Valid).storage
        val restItem = TimedExerciseItem(
            id = "item",
            exerciseId = "exercise",
            stageType = TimedStageType.REST,
            workDurationSec = 20,
            restAfterSec = 10,
            autoAdvance = true
        )
        val snapshot = WorkoutPlanSnapshot(
            title = "Follow",
            mode = WorkoutMode.FOLLOW_ALONG,
            blocks = listOf(
                StretchBlock("stretch", 0, items = listOf(restItem)),
                TimedCircuitBlock("circuit", 1, rounds = 1, items = listOf(restItem))
            ),
            followAlong = FollowAlongPlanMeta(preset = true)
        )
        val projection =
            "{\"signatureInputContractVersion\":1,\"mode\":\"follow_along\",\"blocks\":[{\"blockId\":\"stretch\",\"blockKind\":\"stretch\",\"order\":0,\"durationSec\":null,\"items\":[{\"itemId\":\"item\",\"exerciseId\":\"exercise\",\"side\":null,\"stageType\":\"rest\",\"workDurationSec\":20,\"restAfterSec\":10,\"autoAdvance\":true}]},{\"blockId\":\"circuit\",\"blockKind\":\"timed_circuit\",\"order\":1,\"rounds\":1,\"restBetweenRoundsSec\":null,\"items\":[{\"itemId\":\"item\",\"exerciseId\":\"exercise\",\"side\":null,\"stageType\":\"rest\",\"workDurationSec\":20,\"restAfterSec\":10,\"autoAdvance\":true}]}]}"
        val digest = testSha256(projection.toByteArray(Charsets.UTF_8))
        val identities = listOf(
            "follow_along_action" to followPayload("non_circuit_action", "stretch", 0, "action", "item", "exercise", null),
            "follow_along_rest" to followPayload("non_circuit_rest_after_action", "stretch", 1, "rest_after_action", "item", "exercise", null),
            "follow_along_action" to followPayload("circuit_action", "circuit", 0, "action", "item", "exercise", 0),
            "follow_along_rest" to followPayload("circuit_rest_after_action", "circuit", 1, "rest_after_action", "item", "exercise", 0)
        ).map { (kind, payload) ->
            kind to envelope("follow_along_v1", 1, "follow_along", kind, payload, digest)
        }

        identities.forEach { (kind, identity) ->
            assertInvalid(PhaseIdentityV1Validator.validate(identity, validated(snapshot), kind))
        }
    }

    @Test
    fun compositionBindingEnumeratesWarmupTargetsRoundRestCooldownAndPaused() {
        val snapshotJson =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Composition\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"composition\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":10,\"rounds\":2,\"restBetweenRoundsSec\":5,\"stageGroups\":[{\"id\":\"group\",\"order\":0,\"name\":\"Display\",\"colorHex\":\"#111111\",\"targets\":[{\"id\":\"work\",\"order\":0,\"name\":\"Work\",\"kind\":\"action\",\"durationSec\":20,\"colorHex\":\"#222222\",\"autoAdvance\":true},{\"id\":\"rest\",\"order\":1,\"name\":\"Rest\",\"kind\":\"rest\",\"durationSec\":10,\"colorHex\":\"#333333\",\"autoAdvance\":true}]}]}],\"preferences\":null,\"followAlong\":null}"
        val projection =
            "{\"signatureInputContractVersion\":1,\"mode\":\"timed\",\"blocks\":[{\"blockId\":\"composition\",\"blockKind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":10,\"rounds\":2,\"restBetweenRoundsSec\":5,\"stageGroups\":[{\"stageGroupId\":\"group\",\"order\":0,\"targets\":[{\"targetId\":\"work\",\"order\":0,\"targetKind\":\"action\",\"durationSec\":20,\"autoAdvance\":true},{\"targetId\":\"rest\",\"order\":1,\"targetKind\":\"rest\",\"durationSec\":10,\"autoAdvance\":true}]}]}]}"
        val digest = testSha256(projection.toByteArray(Charsets.UTF_8))
        val snapshot = (PlanSnapshotStorageV1Validator.validate(snapshotJson, WorkoutMode.TIMED) as
            PlanSnapshotStorageV1ValidationResult.Valid).storage
        val identities = listOf(
            "timed_work" to compositionPayload("warmup", "composition", "composition:warmup", "warmup", "composition:warmup", "composition:warmup:target", "warmup", null, null, 0, 0, 0, 0),
            "timed_work" to compositionPayload("stage_group_action", "composition", "composition:r1:g1:group", "stage_group", "group", "work", "action", 0, 0, 0, 1, 1, 1),
            "timed_rest" to compositionPayload("stage_group_rest", "composition", "composition:r1:g1:group", "stage_group", "group", "rest", "rest", 0, 0, 1, 1, 2, 2),
            "timed_rest" to compositionPayload("between_round_rest", "composition", "composition:r1:between-round-rest", "between_round_rest", "composition:r1:between-round-rest", "composition:r1:between-round-rest:target", "between_round_rest", 0, null, 0, 2, 3, 3),
            "timed_work" to compositionPayload("stage_group_action", "composition", "composition:r2:g1:group", "stage_group", "group", "work", "action", 1, 0, 0, 3, 4, 4),
            "timed_work" to compositionPayload("cooldown", "composition", "composition:cooldown", "cooldown", "composition:cooldown", "composition:cooldown:target", "cooldown", null, null, 0, 4, 6, 6),
            "paused" to compositionPayload("paused", null, null, null, null, null, null, null, null, null, null, null, null)
        ).map { (kind, payload) -> kind to envelope("timed_composition_v2", 2, "timed", kind, payload, digest) }
        identities.forEach { (kind, identity) ->
            assertValid(PhaseIdentityV1Validator.validate(identity, snapshot, kind))
        }
        val action = identities[1].second
        linkedMapOf(
            "timeline_stage_id" to action.replace("\"$TIMELINE_STAGE_ID_KEY\":\"composition:r1:g1:group\"", "\"$TIMELINE_STAGE_ID_KEY\":\"other\""),
            "timelineStageKind" to action.replace("\"timelineStageKind\":\"stage_group\"", "\"timelineStageKind\":\"warmup\""),
            "stageInstanceIndex0" to action.replace("\"stageInstanceIndex0\":1", "\"stageInstanceIndex0\":2"),
            "target_instance_index" to action.replace("\"$TARGET_ORDINAL_KEY\":1", "\"$TARGET_ORDINAL_KEY\":2"),
            "stepIndex0" to action.replace("\"stepIndex0\":1", "\"stepIndex0\":2")
        ).forEach { (field, mutation) ->
            val context = requireNotNull(PhaseIdentityV1Validator.prepareContext(snapshot))
            val standalone = PhaseIdentityV1Validator.validate(mutation, snapshot)
            val prepared = PhaseIdentityV1Validator.validatePrepared(mutation, context)
            assertEquals("Composition $field changed prepared semantics", standalone, prepared)
            assertTrue("Composition $field mismatch validated", prepared is CanonicalValidationResult.Invalid)
        }
    }

    @Test
    fun standaloneAndPreparedPathsPreserveTypedResultsAndFailurePrecedence() {
        val snapshotJson =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Composition\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"composition\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[]}],\"preferences\":null,\"followAlong\":null}"
        val projection =
            "{\"signatureInputContractVersion\":1,\"mode\":\"timed\",\"blocks\":[{\"blockId\":\"composition\",\"blockKind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[]}]}"
        val digest = testSha256(projection.toByteArray(Charsets.UTF_8))
        val snapshot = (PlanSnapshotStorageV1Validator.validate(snapshotJson, WorkoutMode.TIMED) as
            PlanSnapshotStorageV1ValidationResult.Valid).storage
        val context = requireNotNull(PhaseIdentityV1Validator.prepareContext(snapshot))
        val valid = envelope(
            "timed_composition_v2",
            2,
            "timed",
            "timed_work",
            compositionPayload(
                "warmup",
                "composition",
                "composition:warmup",
                "warmup",
                "composition:warmup",
                "composition:warmup:target",
                "warmup",
                null,
                null,
                0,
                0,
                0,
                0
            ),
            digest
        )
        val cases = listOf(
            valid,
            valid.replace("\"phaseIdentityContractVersion\":1", "\"phaseIdentityContractVersion\":2"),
            valid.replace("\"family\":\"timed_composition_v2\",", ""),
            valid.replace("\"payloadVersion\":2", "\"payloadVersion\":\"2\""),
            valid.replace("\"mode\":\"timed\"", "\"mode\":\"strength\""),
            valid.replace("\"phaseKind\":\"timed_work\"", "\"phaseKind\":\"timed_rest\""),
            valid.replace(digest, DIGEST),
            valid.replace("\"compositionBlockId\":\"composition\"", "\"compositionBlockId\":\"other\""),
            valid.replace("\"stepIndex0\":0", "\"stepIndex0\":1"),
            valid.replace("\"payload\":{", "\"extra\":true,\"payload\":{")
        )
        cases.forEachIndexed { index, identity ->
            assertEquals(
                "typed prepared differential $index",
                PhaseIdentityV1Validator.validate(identity, snapshot, "timed_work"),
                PhaseIdentityV1Validator.validatePrepared(identity, context, "timed_work")
            )
        }

        val invalidSnapshot = WorkoutPlanSnapshotStorageV1(WorkoutMode.TIMED, "$snapshotJson ")
        val unsupportedIdentity = cases[1]
        assertEquals(
            CanonicalValidationResult.UnsupportedVersion("phase_identity", "2"),
            PhaseIdentityV1Validator.validate(unsupportedIdentity, invalidSnapshot, "timed_work")
        )
        assertEquals(null, PhaseIdentityV1Validator.prepareContext(invalidSnapshot))
    }

    @Test
    fun forgedPreparedContextCannotValidateWithoutFactoryEvidence() {
        val forged = PreparedPhaseIdentityV1Context.fromValidated(
            factoryProof = Any(),
            expectedMode = "timed",
            expectedDigest = DIGEST,
            blocks = emptyList()
        )

        assertEquals(null, forged)
    }

    @Test
    fun preparedBindingRetainsNoMutableParsedRootAndIsolatesReturnedDerivedBytes() {
        val snapshot = WorkoutPlanSnapshot(
            title = "Mutation isolation",
            mode = WorkoutMode.TIMED,
            blocks = listOf(WarmupBlock("block", 0, durationSec = 10))
        )
        val prepared = (PlanSnapshotStorageV1Validator.prepare(
            snapshot.toStorageJson(),
            WorkoutMode.TIMED
        ) as PreparedPlanSnapshotStorageV1Result.Valid).prepared
        val storage = prepared.storage()
        val digest = requireNotNull(
            com.liujyks.trainflow.core.data.OrderedStructureSignatureInputV1.digestHexLowercase(storage)
        )
        val identity = envelope(
            family = "legacy_timed_v1",
            payloadVersion = 1,
            mode = "timed",
            phaseKind = "timed_work",
            payload = legacyPayload(
                "boundary_block_work",
                "block",
                0,
                "warmup",
                "warmup",
                null,
                null,
                null
            ),
            digest = digest
        )
        val context = requireNotNull(PhaseIdentityV1Validator.prepareContext(prepared))
        assertValid(PhaseIdentityV1Validator.validatePrepared(identity, context, "timed_work"))

        val returnedBytes = prepared.orderedStructureSignatureInputBytes()
        returnedBytes.fill(0)
        assertTrue(!returnedBytes.contentEquals(prepared.orderedStructureSignatureInputBytes()))

        @Suppress("UNCHECKED_CAST")
        val collectionMutation = runCatching {
            (prepared.phaseBindingBlocks() as MutableList<PlanSnapshotPhaseBlockFactsV1>).clear()
        }.exceptionOrNull()
        assertTrue(collectionMutation is UnsupportedOperationException)

        assertValid(PhaseIdentityV1Validator.validatePrepared(identity, context, "timed_work"))
    }

    @Test
    fun preparedBindingDoesNotCollapseDuplicateSnapshotIds() {
        val duplicateSnapshot = WorkoutPlanSnapshot(
            title = "Duplicate",
            mode = WorkoutMode.TIMED,
            blocks = listOf(
                WarmupBlock("duplicate", 0, durationSec = 10),
                WarmupBlock("duplicate", 1, durationSec = 20)
            )
        )
        val storage = (PlanSnapshotStorageV1Validator.validate(
            duplicateSnapshot.toStorageJson(),
            WorkoutMode.TIMED
        ) as PlanSnapshotStorageV1ValidationResult.Valid).storage
        val digest = requireNotNull(
            com.liujyks.trainflow.core.data.OrderedStructureSignatureInputV1.digestHexLowercase(storage)
        )
        val identity = envelope(
            "legacy_timed_v1",
            1,
            "timed",
            "timed_work",
            legacyPayload("boundary_block_work", "duplicate", 0, "warmup", "warmup", null, null, null),
            digest
        )
        val context = requireNotNull(PhaseIdentityV1Validator.prepareContext(storage))
        val standalone = PhaseIdentityV1Validator.validate(identity, storage, "timed_work")
        val prepared = PhaseIdentityV1Validator.validatePrepared(identity, context, "timed_work")

        assertEquals(standalone, prepared)
        assertInvalid(prepared)
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
        payload: String,
        digest: String = DIGEST
    ): String =
        "{\"phaseIdentityContractVersion\":1,\"family\":\"$family\",\"payloadVersion\":$payloadVersion,\"mode\":\"$mode\",\"phaseKind\":\"$phaseKind\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"$digest\"},\"payload\":$payload}"

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

    private fun testSha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private data class PhaseFixture(
        val name: String,
        val phaseKind: String,
        val json: String
    )

    private data class ClosedJsonCase(
        val name: String,
        val canonical: String,
        val versionKey: String,
        val validator: (String) -> CanonicalValidationResult
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
