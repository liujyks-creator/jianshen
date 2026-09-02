package com.liujyks.trainflow.core.database

import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1Validator
import com.liujyks.trainflow.core.data.PlanSnapshotPhaseBlockFactsV1
import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1
import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1Result
import com.liujyks.trainflow.core.data.WorkoutPlanSnapshotStorageV1
import com.liujyks.trainflow.core.model.WorkoutMode
import java.math.BigDecimal
import java.util.Collections

sealed interface CanonicalValidationResult {
    data object Valid : CanonicalValidationResult
    data class Invalid(val code: String) : CanonicalValidationResult
    data class UnsupportedVersion(
        val contract: String,
        val actualVersion: String
    ) : CanonicalValidationResult
}

object CanonicalStorageJsonV1Validators {
    fun validateSessionDisplayMetadata(json: String): CanonicalValidationResult =
        validateVersionedObject(
            json = json,
            contract = "session_display_metadata",
            versionKey = "displayMetadataContractVersion"
        ) { root -> validateDisplayMetadataRoot(root) }

    fun validateZoneSnapshot(json: String): CanonicalValidationResult =
        validateVersionedObject(
            json = json,
            contract = "zone_snapshot",
            versionKey = "zoneSnapshotContractVersion"
        ) { root -> validateZoneSnapshotRoot(root) }

    fun validateZoneSnapshot(
        json: String,
        expectedEffectiveMaxBpm: Int,
        expectedEffectiveMaxSource: String
    ): CanonicalValidationResult {
        val structural = validateZoneSnapshot(json)
        if (structural != CanonicalValidationResult.Valid) return structural
        val root = parseCanonicalJson(json) as CanonicalJsonValue.Obj
        return if (
            root.int("effectiveMaxBpm") == expectedEffectiveMaxBpm.toLong() &&
            root.string("effectiveMaxSource") == expectedEffectiveMaxSource
        ) {
            CanonicalValidationResult.Valid
        } else {
            CanonicalValidationResult.Invalid("invalid_zone_snapshot_contract")
        }
    }

    fun validateAnalysisConfig(json: String): CanonicalValidationResult =
        validateVersionedObject(
            json = json,
            contract = "analysis_config",
            versionKey = "analysisConfigContractVersion"
        ) { root ->
            root.hasExactKeys(ANALYSIS_CONFIG_KEYS) &&
                root.int("sampleValidityCapMs") == 2500L &&
                root.int("sampleIntervalContractVersion") == 1L &&
                root.int("partialLowerBoundBasisPoints") == 5000L &&
                root.int("phaseConclusionBasisPoints") == 7000L &&
                root.int("normalBasisPoints") == 8000L &&
                root.string("coverageThresholdRule") == "checked_integer_cross_multiply" &&
                root.string("coverageBasisPointsRule") == "floor_integer_ratio" &&
                root.string("displayPercentRule") == "floor_basis_points_div_100" &&
                root.string("weightedAverageRule") == "checked_integer_time_integral" &&
                root.string("averageDisplayRule") == "positive_integer_half_up" &&
                root.string("zeroCoveredRule") == "null_integral_and_average" &&
                root.string("observedMaxRule") == "eligible_canonical_point_first_tie" &&
                root.int("zoneAttributionContractVersion") == 1L &&
                root.string("zoneAttributionRule") == "checked_cross_multiply_six_zones" &&
                root.int("statusProjectionContractVersion") == 1L &&
                root.int("durationPartitionContractVersion") == 1L
        }

    fun validateZoneDurations(json: String): CanonicalValidationResult =
        validateVersionedObject(
            json = json,
            contract = "zone_durations",
            versionKey = "zoneDurationsContractVersion"
        ) { root ->
            root.hasExactKeys(ZONE_DURATION_KEYS) &&
                ZONE_DURATION_KEYS.minus("zoneDurationsContractVersion")
                    .all { key -> root.isNonNegativeInteger(key) }
        }

    fun validatePhaseAggregates(json: String): CanonicalValidationResult =
        validateVersionedObject(
            json = json,
            contract = "phase_aggregates",
            versionKey = "phaseAggregatesContractVersion"
        ) { root ->
            if (!root.hasExactKeys(PHASE_AGGREGATES_ROOT_KEYS)) return@validateVersionedObject false
            val entries = root.array("aggregates") ?: return@validateVersionedObject false
            var previousSequence = -1L
            entries.all { value ->
                val entry = value as? CanonicalJsonValue.Obj ?: return@all false
                val sequence = entry.int("phaseSequence") ?: return@all false
                val valid = entry.hasExactKeys(PHASE_AGGREGATE_ENTRY_KEYS) &&
                    sequence >= 0 && sequence > previousSequence &&
                    entry.string("phaseKind") in PHASE_KINDS &&
                    entry.isNonNegativeInteger("eligibleDurationMs") &&
                    entry.isNonNegativeInteger("coveredDurationMs") &&
                    entry.isNullableIntegerInRange("coverageBasisPoints", 0, 10000) &&
                    entry.string("coverageStatus") in COVERAGE_STATUSES &&
                    entry.boolean("conclusionEligible") != null &&
                    entry.isNullableNonNegativeInteger("weightedBpmMs") &&
                    entry.isNullableIntegerInRange("observedAvgBpm", 1, 65535) &&
                    entry.isNullableIntegerInRange("observedMaxBpm", 1, 65535) &&
                    entry.isNullableNonNegativeInteger("highestOffsetMs") &&
                    entry.isNullableNonNegativeInteger("highestMutationSequence") &&
                    entry.isNullableNonNegativeInteger("highestSampleSequence")
                previousSequence = sequence
                valid
            }
        }

    fun validateDurationBreakdown(json: String): CanonicalValidationResult =
        validateVersionedObject(
            json = json,
            contract = "duration_breakdown",
            versionKey = "durationBreakdownContractVersion"
        ) { root ->
            root.hasExactKeys(DURATION_BREAKDOWN_ROOT_KEYS) &&
                listOf(
                    "canonicalSessionDurationMs",
                    "recordingWindowDurationMs",
                    "notRequestedBeforeRecordingStartMs"
                ).all(root::isNonNegativeInteger) &&
                root.obj("intentAxis")?.hasNonNegativeExactKeys(INTENT_AXIS_KEYS) == true &&
                root.obj("phaseAxis")?.hasNonNegativeExactKeys(PHASE_AXIS_KEYS) == true &&
                root.obj("primaryAnalysisPartition")
                    ?.hasNonNegativeExactKeys(PRIMARY_PARTITION_KEYS) == true &&
                root.obj("deviceStateDurations")
                    ?.hasNonNegativeExactKeys(DEVICE_STATES) == true &&
                root.obj("deviceReasonDurations")
                    ?.hasNonNegativeExactKeys(DEVICE_REASONS) == true &&
                root.obj("orthogonalityContract")?.let { contract ->
                    contract.hasExactKeys(ORTHOGONALITY_KEYS) &&
                        contract.int("contractVersion") == 1L &&
                        contract.string("rule") ==
                        "primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum"
                } == true
        }

    fun validateQualityReasons(json: String): CanonicalValidationResult =
        validateVersionedObject(
            json = json,
            contract = "quality_reasons",
            versionKey = "qualityReasonsContractVersion"
        ) { root ->
            if (!root.hasExactKeys(QUALITY_REASON_ROOT_KEYS)) return@validateVersionedObject false
            val sessionReasons = root.array("sessionReasons") ?: return@validateVersionedObject false
            val phaseReasons = root.array("phaseReasons") ?: return@validateVersionedObject false
            val sessionReasonCodes = mutableSetOf<String>()
            val phaseReasonKeys = mutableSetOf<Pair<Long, String>>()
            sessionReasons.all { value ->
                val item = value as? CanonicalJsonValue.Obj ?: return@all false
                val reasonCode = item.string("reasonCode") ?: return@all false
                item.hasExactKeys(SESSION_REASON_KEYS) &&
                    reasonCode in QUALITY_REASON_CODES && sessionReasonCodes.add(reasonCode) &&
                    item.isNullableNonNegativeInteger("durationMs")
            } && phaseReasons.all { value ->
                val item = value as? CanonicalJsonValue.Obj ?: return@all false
                val phaseSequence = item.int("phaseSequence") ?: return@all false
                val reasonCode = item.string("reasonCode") ?: return@all false
                item.hasExactKeys(PHASE_REASON_KEYS) &&
                    phaseSequence >= 0 && reasonCode in QUALITY_REASON_CODES &&
                    phaseReasonKeys.add(phaseSequence to reasonCode) &&
                    item.isNullableNonNegativeInteger("durationMs")
            }
        }
}

object SessionDisplayMetadataV1Validator {
    fun validateTransition(
        previousJson: String,
        nextJson: String,
        terminal: Boolean
    ): CanonicalValidationResult {
        val previous = validatedDisplayMetadata(previousJson) ?: return invalidDisplayMetadata()
        val next = validatedDisplayMetadata(nextJson) ?: return invalidDisplayMetadata()
        if (terminal) {
            return if (previous == next) CanonicalValidationResult.Valid else invalidDisplayMetadata()
        }
        if (next.size < previous.size || next.take(previous.size) != previous) {
            return invalidDisplayMetadata()
        }
        return CanonicalValidationResult.Valid
    }

    private fun validatedDisplayMetadata(json: String): List<CanonicalJsonValue.Obj>? {
        if (CanonicalStorageJsonV1Validators.validateSessionDisplayMetadata(json) !=
            CanonicalValidationResult.Valid
        ) {
            return null
        }
        val root = parseCanonicalJson(json) as? CanonicalJsonValue.Obj ?: return null
        return root.array("entries")?.map { value -> value as CanonicalJsonValue.Obj }
    }

    private fun invalidDisplayMetadata() =
        CanonicalValidationResult.Invalid("invalid_session_display_metadata_contract")
}

private object PreparedPhaseIdentityV1ContextFactoryProof

internal class PreparedPhaseIdentityV1Context private constructor(
    val expectedMode: String,
    val expectedDigest: String,
    blocks: List<PhaseSnapshotBlockBindingV1>
) {
    private val blocksById = blocks.groupBy { block -> block.id }
    private val compositionBlocksById = blocks
        .filter { block -> block.kind == "timed_composition" }
        .groupBy { block -> block.id }
    private val firstBlockById = buildMap {
        blocks.forEach { block ->
            if (block.id !in this) put(block.id, block)
        }
    }

    internal fun uniqueBlockById(id: String): PhaseSnapshotBlockBindingV1? =
        blocksById[id]?.singleOrNull()

    internal fun uniqueCompositionBlockById(id: String): PhaseSnapshotBlockBindingV1? =
        compositionBlocksById[id]?.singleOrNull()

    internal fun firstBlockById(id: String): PhaseSnapshotBlockBindingV1? = firstBlockById[id]

    internal companion object {
        internal fun fromValidated(
            factoryProof: Any,
            expectedMode: String,
            expectedDigest: String,
            blocks: List<PhaseSnapshotBlockBindingV1>
        ): PreparedPhaseIdentityV1Context? =
            if (factoryProof === PreparedPhaseIdentityV1ContextFactoryProof) {
                PreparedPhaseIdentityV1Context(expectedMode, expectedDigest, blocks)
            } else {
                null
            }
    }
}

internal class PhaseSnapshotBlockBindingV1 private constructor(
    val id: String,
    val kind: String,
    val rounds: Long?,
    val restBetweenRoundsSec: Long?,
    val durationSec: Long?,
    val exerciseId: String?,
    val precedingStrengthSetCount: Int,
    val items: List<PlanSnapshotPhaseBlockFactsV1.Item>,
    val sets: List<PlanSnapshotPhaseBlockFactsV1.SetPlan>,
    val substitutions: List<String>,
    compositionSteps: List<CompositionSnapshotStep>?
) {
    private val compositionSteps = compositionSteps?.let { steps ->
        Collections.unmodifiableList(ArrayList(steps))
    }
    private val firstItemIndexById = buildMap {
        items.forEachIndexed { index, item ->
            val itemId = item.id
            if (itemId !in this) put(itemId, index)
        }
    }
    private val firstSetIndexById = buildMap {
        sets.forEachIndexed { index, set ->
            val setId = set.id
            if (setId !in this) put(setId, index)
        }
    }
    private val itemStepStarts: List<Long>
    private val stepsPerItems: Long

    init {
        var nextStep = 0L
        itemStepStarts = items.map { item ->
            val start = nextStep
            nextStep += 1L + if ((item.restAfterSec ?: 0L) > 0L) 1L else 0L
            start
        }
        stepsPerItems = nextStep
    }

    internal fun firstItemIndexById(id: String): Int? = firstItemIndexById[id]

    internal fun firstSetIndexById(id: String): Int? = firstSetIndexById[id]

    internal fun timedItemStepIndex(
        itemIndex: Int,
        roundIndex: Long?,
        restAfter: Boolean
    ): Long? {
        if (itemIndex !in items.indices) return null
        val roundStart = if (kind == "timed_circuit") {
            val round = roundIndex ?: return null
            val between = if ((restBetweenRoundsSec ?: 0L) > 0L) 1L else 0L
            round * (stepsPerItems + between)
        } else {
            if (roundIndex != null) return null
            0L
        }
        if (restAfter && (items[itemIndex].restAfterSec ?: 0L) <= 0L) return null
        return roundStart + itemStepStarts[itemIndex] + if (restAfter) 1L else 0L
    }

    internal fun timedCircuitBetweenRoundStepIndex(roundIndex: Long): Long =
        roundIndex * (stepsPerItems + 1L) + stepsPerItems

    internal fun compositionPayloadMatches(payload: CanonicalJsonValue.Obj): Boolean {
        val stepIndex = payload.int("stepIndex0") ?: return false
        val expected = compositionSteps?.getOrNull(stepIndex.toInt()) ?: return false
        return expected.matches(payload)
    }

    internal companion object {
        internal fun fromValidated(
            factoryProof: Any,
            facts: PlanSnapshotPhaseBlockFactsV1,
            precedingStrengthSetCount: Int,
            compositionSteps: List<CompositionSnapshotStep>?
        ): PhaseSnapshotBlockBindingV1? =
            if (factoryProof === PreparedPhaseIdentityV1ContextFactoryProof) {
                PhaseSnapshotBlockBindingV1(
                    id = facts.id,
                    kind = facts.kind,
                    rounds = facts.rounds,
                    restBetweenRoundsSec = facts.restBetweenRoundsSec,
                    durationSec = facts.durationSec,
                    exerciseId = facts.exerciseId,
                    precedingStrengthSetCount = precedingStrengthSetCount,
                    items = facts.items,
                    sets = facts.sets,
                    substitutions = facts.substitutions,
                    compositionSteps = compositionSteps
                )
            } else {
                null
            }
    }
}

object PhaseIdentityV1Validator {
    fun validate(
        json: String,
        immutableSnapshot: WorkoutPlanSnapshotStorageV1,
        expectedPhaseKind: String? = null
    ): CanonicalValidationResult {
        val root = parseCanonicalJson(json) as? CanonicalJsonValue.Obj
            ?: return invalidPhaseIdentity()
        val structural = validateStructure(root, expectedPhaseKind, immutableSnapshot.mode.contractValue)
        if (structural != CanonicalValidationResult.Valid) return structural
        val context = prepareContext(immutableSnapshot) ?: return invalidPhaseIdentity()
        return validatePreparedRoot(root, context)
    }

    internal fun prepareContext(
        immutableSnapshot: WorkoutPlanSnapshotStorageV1
    ): PreparedPhaseIdentityV1Context? = prepareContext(
        immutableSnapshot.persistedJson,
        immutableSnapshot.mode
    )

    internal fun prepareContext(
        persistedJson: String,
        mode: WorkoutMode
    ): PreparedPhaseIdentityV1Context? {
        val preparedResult = PlanSnapshotStorageV1Validator.prepare(persistedJson, mode)
        val prepared = (preparedResult as? PreparedPlanSnapshotStorageV1Result.Valid)?.prepared
            ?: return null
        return prepareContext(prepared)
    }

    internal fun prepareContext(
        prepared: PreparedPlanSnapshotStorageV1
    ): PreparedPhaseIdentityV1Context? {
        var precedingStrengthSetCount = 0
        val blocks = prepared.phaseBindingBlocks().map { facts ->
            val view = requireNotNull(PhaseSnapshotBlockBindingV1.fromValidated(
                factoryProof = PreparedPhaseIdentityV1ContextFactoryProof,
                facts = facts,
                precedingStrengthSetCount = precedingStrengthSetCount,
                compositionSteps = if (facts.kind == "timed_composition") {
                    compositionSnapshotSteps(facts)
                } else {
                    null
                }
            ))
            if (view.kind == "strength_exercise") {
                precedingStrengthSetCount += view.sets.size
            }
            view
        }
        return PreparedPhaseIdentityV1Context.fromValidated(
            factoryProof = PreparedPhaseIdentityV1ContextFactoryProof,
            expectedMode = prepared.storage().mode.contractValue,
            expectedDigest = prepared.orderedStructureDigestHexLowercase(),
            blocks = blocks
        )
    }

    internal fun validatePrepared(
        json: String,
        context: PreparedPhaseIdentityV1Context,
        expectedPhaseKind: String? = null
    ): CanonicalValidationResult {
        val root = parseCanonicalJson(json) as? CanonicalJsonValue.Obj
            ?: return invalidPhaseIdentity()
        val structural = validateStructure(root, expectedPhaseKind, context.expectedMode)
        if (structural != CanonicalValidationResult.Valid) return structural
        return validatePreparedRoot(root, context)
    }

    private fun validatePreparedRoot(
        root: CanonicalJsonValue.Obj,
        context: PreparedPhaseIdentityV1Context
    ): CanonicalValidationResult {
        val actualDigest = root.obj("orderedStructureSignature")
            ?.string("digestHexLowercase") ?: return invalidPhaseIdentity()
        return if (
            actualDigest == context.expectedDigest &&
            validateSnapshotPayloadBinding(root, context)
        ) {
            CanonicalValidationResult.Valid
        } else {
            invalidPhaseIdentity()
        }
    }

    private fun validateSnapshotPayloadBinding(
        identity: CanonicalJsonValue.Obj,
        context: PreparedPhaseIdentityV1Context
    ): Boolean {
        val payload = identity.obj("payload") ?: return false
        return when (identity.string("family")) {
            "legacy_timed_v1" -> validateLegacySnapshotBinding(payload, context)
            "timed_composition_v2" -> validateCompositionSnapshotBinding(payload, context)
            "strength_v1" -> validateStrengthSnapshotBinding(payload, context)
            "follow_along_v1" -> validateFollowSnapshotBinding(payload, context)
            else -> false
        }
    }

    private fun validateLegacySnapshotBinding(
        payload: CanonicalJsonValue.Obj,
        context: PreparedPhaseIdentityV1Context
    ): Boolean {
        if (payload.string("variant") == "paused") return true
        val blockId = payload.string("blockId") ?: return false
        val blockView = context.uniqueBlockById(blockId) ?: return false
        if (blockView.kind != payload.string("legacyBlockKind")) return false
        val roundIndex = payload.int("roundIndex0")
        if (roundIndex != null && roundIndex !in 0 until (blockView.rounds ?: return false)) {
            return false
        }
        val variant = payload.string("variant") ?: return false
        val itemId = payload.string("itemId")
        if (itemId == null) {
            return when (variant) {
                "boundary_block_work" -> payload.int("stepIndex0") == 0L &&
                    blockView.kind in setOf("warmup", "stretch", "cooldown")
                "between_round_rest" -> blockView.kind == "timed_circuit" &&
                    roundIndex != null && roundIndex < (blockView.rounds ?: return false) - 1 &&
                    (blockView.restBetweenRoundsSec ?: 0L) > 0L &&
                    payload.int("stepIndex0") == blockView.timedCircuitBetweenRoundStepIndex(roundIndex)
                "standalone_rest" -> payload.int("stepIndex0") == 0L && blockView.kind == "rest"
                else -> false
            }
        }
        val itemIndex = blockView.firstItemIndexById(itemId) ?: return false
        val item = blockView.items[itemIndex]
        val restAfter = variant in setOf("boundary_rest_after_item", "circuit_rest_after_item")
        val expectedStep = blockView.timedItemStepIndex(itemIndex, roundIndex, restAfter) ?: return false
        val expectedExercise = if (variant in setOf("boundary_item_rest", "circuit_item_rest")) {
            null
        } else {
            item.exerciseId
        }
        return payload.int("stepIndex0") == expectedStep &&
            payload.string("exerciseId") == expectedExercise &&
            when (variant) {
                "boundary_item_work", "circuit_item_work" ->
                    item.stageType != "rest" && payload.string("legacyStageType") == item.stageType
                "boundary_item_rest", "circuit_item_rest" -> item.stageType == "rest"
                "boundary_rest_after_item", "circuit_rest_after_item" -> (item.restAfterSec ?: 0L) > 0L
                else -> false
            }
    }

    private fun validateCompositionSnapshotBinding(
        payload: CanonicalJsonValue.Obj,
        context: PreparedPhaseIdentityV1Context
    ): Boolean {
        if (payload.string("variant") == "paused") return true
        val blockId = payload.string("compositionBlockId") ?: return false
        val block = context.uniqueCompositionBlockById(blockId) ?: return false
        return block.compositionPayloadMatches(payload)
    }

    private fun compositionSnapshotSteps(
        block: PlanSnapshotPhaseBlockFactsV1
    ): List<CompositionSnapshotStep>? {
        val blockId = block.id
        val rounds = block.rounds?.toInt() ?: return null
        val groups = block.compositionGroups
        val result = mutableListOf<CompositionSnapshotStep>()
        var stageInstanceIndex0 = 0L
        var targetInstanceIndex0 = 0L
        fun add(
            variant: String,
            timelineStageId: String,
            timelineStageKind: String,
            stageGroupId: String,
            targetId: String,
            targetKind: String,
            roundIndex0: Long?,
            stageGroupIndex0: Long?,
            targetIndex0: Long
        ) {
            result += CompositionSnapshotStep(
                variant,
                blockId,
                timelineStageId,
                timelineStageKind,
                stageGroupId,
                targetId,
                targetKind,
                roundIndex0,
                stageGroupIndex0,
                targetIndex0,
                stageInstanceIndex0,
                targetInstanceIndex0,
                result.size.toLong()
            )
            targetInstanceIndex0 += 1
        }
        if ((block.warmupSec ?: return null) > 0) {
            val stageId = "$blockId:warmup"
            add("warmup", stageId, "warmup", stageId, "$stageId:target", "warmup", null, null, 0)
            stageInstanceIndex0 += 1
        }
        repeat(rounds) { roundIndex0 ->
            groups.forEachIndexed { groupIndex0, group ->
                val groupId = group.id
                if (group.order != groupIndex0.toLong()) return null
                val stageId = "$blockId:r${roundIndex0 + 1}:g${groupIndex0 + 1}:$groupId"
                group.targets.forEachIndexed { targetIndex0, target ->
                    if (target.order != targetIndex0.toLong()) return null
                    val kind = target.kind
                    add(
                        "stage_group_$kind",
                        stageId,
                        "stage_group",
                        groupId,
                        target.id,
                        kind,
                        roundIndex0.toLong(),
                        groupIndex0.toLong(),
                        targetIndex0.toLong()
                    )
                }
                stageInstanceIndex0 += 1
            }
            if (roundIndex0 < rounds - 1 && (block.restBetweenRoundsSec ?: return null) > 0) {
                val stageId = "$blockId:r${roundIndex0 + 1}:between-round-rest"
                add(
                    "between_round_rest",
                    stageId,
                    "between_round_rest",
                    stageId,
                    "$stageId:target",
                    "between_round_rest",
                    roundIndex0.toLong(),
                    null,
                    0
                )
                stageInstanceIndex0 += 1
            }
        }
        if ((block.cooldownSec ?: return null) > 0) {
            val stageId = "$blockId:cooldown"
            add("cooldown", stageId, "cooldown", stageId, "$stageId:target", "cooldown", null, null, 0)
        }
        return result
    }

    private fun validateStrengthSnapshotBinding(
        payload: CanonicalJsonValue.Obj,
        context: PreparedPhaseIdentityV1Context
    ): Boolean {
        if (payload.string("variant") == "paused") return true
        val blockId = payload.string("blockId") ?: return false
        val blockView = context.firstBlockById(blockId) ?: return false
        if (blockView.kind != "strength_exercise" ||
            payload.string("plannedExerciseId") != blockView.exerciseId
        ) {
            return false
        }
        val setId = payload.string("setPlanId") ?: return false
        val setIndex = blockView.firstSetIndexById(setId) ?: return false
        val set = blockView.sets[setIndex]
        val plannedExerciseId = blockView.exerciseId ?: return false
        val actualExerciseId = payload.string("actualExerciseId") ?: return false
        val substitutedFrom = payload.string("substitutedFromExerciseId")
        val globalSetIndex = blockView.precedingStrengthSetCount + setIndex
        return payload.int("exerciseSetIndex0") == setIndex.toLong() &&
            payload.int("globalSetIndex0") == globalSetIndex.toLong() &&
            payload.string("setKind") == set.kind &&
            if (substitutedFrom == null) {
                actualExerciseId == plannedExerciseId
            } else {
                substitutedFrom == plannedExerciseId && actualExerciseId != plannedExerciseId &&
                    actualExerciseId in blockView.substitutions
            }
    }

    private fun validateFollowSnapshotBinding(
        payload: CanonicalJsonValue.Obj,
        context: PreparedPhaseIdentityV1Context
    ): Boolean {
        if (payload.string("variant") == "paused") return true
        val blockId = payload.string("blockId") ?: return false
        val blockView = context.uniqueBlockById(blockId) ?: return false
        val roundIndex = payload.int("roundIndex0")
        if (roundIndex != null && roundIndex !in 0 until (blockView.rounds ?: return false)) {
            return false
        }
        val variant = payload.string("variant") ?: return false
        val itemId = payload.string("itemId")
        if (itemId == null) {
            return when (variant) {
                "between_round_rest" -> blockView.kind == "timed_circuit" &&
                    roundIndex != null && roundIndex < (blockView.rounds ?: return false) - 1 &&
                    (blockView.restBetweenRoundsSec ?: 0L) > 0L &&
                    payload.int("stepIndex0") == blockView.timedCircuitBetweenRoundStepIndex(roundIndex)
                "block_rest" -> blockView.kind == "rest" && payload.int("stepIndex0") == 0L
                "boundary" -> blockView.kind in setOf("warmup", "stretch", "cooldown") &&
                    blockView.durationSec != null && payload.int("stepIndex0") == 0L
                else -> false
            }
        }
        val itemIndex = blockView.firstItemIndexById(itemId) ?: return false
        val item = blockView.items[itemIndex]
        val restAfter = variant in setOf("circuit_rest_after_action", "non_circuit_rest_after_action")
        val expectedStep = blockView.timedItemStepIndex(itemIndex, roundIndex, restAfter) ?: return false
        return item.stageType != "rest" &&
            payload.int("stepIndex0") == expectedStep &&
            payload.string("exerciseId") == item.exerciseId &&
            if (restAfter) (item.restAfterSec ?: 0L) > 0L else true
    }

    fun validateStructure(
        json: String,
        expectedPhaseKind: String? = null,
        expectedMode: String? = null
    ): CanonicalValidationResult {
        val root = parseCanonicalJson(json) as? CanonicalJsonValue.Obj
            ?: return invalidPhaseIdentity()
        return validateStructure(root, expectedPhaseKind, expectedMode)
    }

    private fun validateStructure(
        root: CanonicalJsonValue.Obj,
        expectedPhaseKind: String?,
        expectedMode: String?
    ): CanonicalValidationResult {
        val version = root.int("phaseIdentityContractVersion")
            ?: return invalidPhaseIdentity()
        if (version != 1L) {
            return CanonicalValidationResult.UnsupportedVersion("phase_identity", version.toString())
        }
        if (!root.hasExactKeys(PHASE_IDENTITY_ROOT_KEYS)) return invalidPhaseIdentity()
        val family = root.string("family") ?: return invalidPhaseIdentity()
        val payloadVersion = root.int("payloadVersion") ?: return invalidPhaseIdentity()
        val mode = root.string("mode") ?: return invalidPhaseIdentity()
        val phaseKind = root.string("phaseKind") ?: return invalidPhaseIdentity()
        if (expectedPhaseKind != null && phaseKind != expectedPhaseKind) return invalidPhaseIdentity()
        if (expectedMode != null && mode != expectedMode) return invalidPhaseIdentity()
        if (phaseKind !in PHASE_KINDS) return invalidPhaseIdentity()
        val signature = root.obj("orderedStructureSignature") ?: return invalidPhaseIdentity()
        if (!validateSignature(signature)) return invalidPhaseIdentity()
        val payload = root.obj("payload") ?: return invalidPhaseIdentity()

        val valid = when (family) {
            "legacy_timed_v1" ->
                payloadVersion == 1L && mode == "timed" && validateLegacyPayload(payload, phaseKind)

            "timed_composition_v2" ->
                payloadVersion == 2L && mode == "timed" &&
                    validateCompositionPayload(payload, phaseKind)

            "strength_v1" ->
                payloadVersion == 1L && mode == "strength" &&
                    validateStrengthPayload(payload, phaseKind)

            "follow_along_v1" ->
                payloadVersion == 1L && mode == "follow_along" &&
                    validateFollowAlongPayload(payload, phaseKind)

            else -> false
        }
        return if (valid) CanonicalValidationResult.Valid else invalidPhaseIdentity()
    }

    private fun validateSignature(signature: CanonicalJsonValue.Obj): Boolean =
        signature.hasExactKeys(SIGNATURE_KEYS) &&
            signature.int("signatureContractVersion") == 1L &&
            signature.string("algorithm") == "sha256" &&
            signature.string("digestHexLowercase")?.matches(LOWERCASE_SHA256) == true

    private fun validateLegacyPayload(
        payload: CanonicalJsonValue.Obj,
        phaseKind: String
    ): Boolean {
        if (!payload.hasExactKeys(LEGACY_PAYLOAD_KEYS)) return false
        val variant = payload.string("variant") ?: return false
        if (variant == "paused") {
            return phaseKind == "paused" && LEGACY_POSITION_KEYS.all(payload::isNull)
        }
        if (!payload.requiredNonEmptyStrings("blockId") || !payload.isNonNegativeInteger("stepIndex0")) {
            return false
        }
        return when (variant) {
            "boundary_block_work" ->
                phaseKind == "timed_work" && payload.isNull("itemId") &&
                    payload.isNull("exerciseId") && payload.isNull("roundIndex0") &&
                    setOf(
                        "warmup" to "warmup",
                        "stretch" to "cooldown",
                        "cooldown" to "cooldown"
                    ).contains(payload.string("legacyBlockKind") to payload.string("legacyStageType"))

            "boundary_item_work" ->
                phaseKind == "timed_work" &&
                    payload.string("legacyBlockKind") in BOUNDARY_BLOCK_KINDS &&
                    payload.string("legacyStageType") in setOf("warmup", "work", "cooldown", "custom") &&
                    payload.requiredNonEmptyStrings("itemId") && payload.isNullableString("exerciseId") &&
                    payload.isNull("roundIndex0")

            "boundary_item_rest" ->
                phaseKind == "timed_rest" &&
                    payload.string("legacyBlockKind") in BOUNDARY_BLOCK_KINDS &&
                    payload.string("legacyStageType") == "rest" &&
                    payload.requiredNonEmptyStrings("itemId") && payload.isNull("exerciseId") &&
                    payload.isNull("roundIndex0")

            "boundary_rest_after_item" ->
                phaseKind == "timed_rest" &&
                    payload.string("legacyBlockKind") in BOUNDARY_BLOCK_KINDS &&
                    payload.string("legacyStageType") == "rest" &&
                    payload.requiredNonEmptyStrings("itemId") && payload.isNullableString("exerciseId") &&
                    payload.isNull("roundIndex0")

            "circuit_item_work" ->
                phaseKind == "timed_work" && payload.string("legacyBlockKind") == "timed_circuit" &&
                    payload.string("legacyStageType") in setOf("work", "custom") &&
                    payload.requiredNonEmptyStrings("itemId") && payload.isNullableString("exerciseId") &&
                    payload.isNonNegativeInteger("roundIndex0")

            "circuit_item_rest" ->
                phaseKind == "timed_rest" && payload.string("legacyBlockKind") == "timed_circuit" &&
                    payload.string("legacyStageType") == "rest" &&
                    payload.requiredNonEmptyStrings("itemId") && payload.isNull("exerciseId") &&
                    payload.isNonNegativeInteger("roundIndex0")

            "circuit_rest_after_item" ->
                phaseKind == "timed_rest" && payload.string("legacyBlockKind") == "timed_circuit" &&
                    payload.string("legacyStageType") == "rest" &&
                    payload.requiredNonEmptyStrings("itemId") && payload.isNullableString("exerciseId") &&
                    payload.isNonNegativeInteger("roundIndex0")

            "between_round_rest" ->
                phaseKind == "timed_rest" && payload.string("legacyBlockKind") == "timed_circuit" &&
                    payload.string("legacyStageType") == "rest" && payload.isNull("itemId") &&
                    payload.isNull("exerciseId") && payload.isNonNegativeInteger("roundIndex0")

            "standalone_rest" ->
                phaseKind == "timed_rest" && payload.string("legacyBlockKind") == "rest" &&
                    payload.string("legacyStageType") == "rest" && payload.isNull("itemId") &&
                    payload.isNull("exerciseId") && payload.isNull("roundIndex0")

            else -> false
        }
    }

    private fun validateCompositionPayload(
        payload: CanonicalJsonValue.Obj,
        phaseKind: String
    ): Boolean {
        if (!payload.hasExactKeys(COMPOSITION_PAYLOAD_KEYS) || payload.int("compositionVersion") != 2L) {
            return false
        }
        val variant = payload.string("variant") ?: return false
        if (variant == "paused") {
            return phaseKind == "paused" && COMPOSITION_POSITION_KEYS.all(payload::isNull)
        }
        if (!payload.requiredNonEmptyStrings("compositionBlockId", "timelineStageId", "stageGroupId", "targetId") ||
            !payload.isNonNegativeInteger("targetIndex0") ||
            !payload.isNonNegativeInteger("stageInstanceIndex0") ||
            !payload.isNonNegativeInteger("targetInstanceIndex0") ||
            !payload.isNonNegativeInteger("stepIndex0")
        ) {
            return false
        }
        val stageId = payload.string("timelineStageId") ?: return false
        return when (variant) {
            "warmup", "cooldown" ->
                phaseKind == "timed_work" &&
                    payload.string("timelineStageKind") == variant &&
                    payload.string("targetKind") == variant &&
                    payload.string("stageGroupId") == stageId &&
                    payload.string("targetId") == "$stageId:target" &&
                    payload.isNull("roundIndex0") && payload.isNull("stageGroupIndex0") &&
                    payload.int("targetIndex0") == 0L

            "stage_group_action", "stage_group_custom", "stage_group_rest" -> {
                val expectedTargetKind = variant.removePrefix("stage_group_")
                val expectedPhase = if (expectedTargetKind == "rest") "timed_rest" else "timed_work"
                phaseKind == expectedPhase && payload.string("timelineStageKind") == "stage_group" &&
                    payload.string("targetKind") == expectedTargetKind &&
                    payload.isNonNegativeInteger("roundIndex0") &&
                    payload.isNonNegativeInteger("stageGroupIndex0")
            }

            "between_round_rest" ->
                phaseKind == "timed_rest" &&
                    payload.string("timelineStageKind") == "between_round_rest" &&
                    payload.string("targetKind") == "between_round_rest" &&
                    payload.string("stageGroupId") == stageId &&
                    payload.string("targetId") == "$stageId:target" &&
                    payload.isNonNegativeInteger("roundIndex0") &&
                    payload.isNull("stageGroupIndex0") && payload.int("targetIndex0") == 0L

            else -> false
        }
    }

    private fun validateStrengthPayload(
        payload: CanonicalJsonValue.Obj,
        phaseKind: String
    ): Boolean {
        if (!payload.hasExactKeys(STRENGTH_PAYLOAD_KEYS)) return false
        val variant = payload.string("variant") ?: return false
        if (variant == "paused") {
            return phaseKind == "paused" && STRENGTH_POSITION_KEYS.all(payload::isNull)
        }
        val expectedPhase = STRENGTH_VARIANT_PHASE[variant] ?: return false
        if (phaseKind != expectedPhase ||
            !payload.requiredNonEmptyStrings(
                "blockId",
                "setPlanId",
                "plannedExerciseId",
                "actualExerciseId"
            ) ||
            !payload.isNonNegativeInteger("exerciseSetIndex0") ||
            !payload.isNonNegativeInteger("globalSetIndex0") ||
            payload.string("setKind") !in STRENGTH_SET_KINDS ||
            !payload.isNullableString("substitutedFromExerciseId")
        ) {
            return false
        }
        val planned = payload.string("plannedExerciseId") ?: return false
        val actual = payload.string("actualExerciseId") ?: return false
        val substituted = payload.string("substitutedFromExerciseId")
        return if (substituted == null) {
            payload.isNull("substitutedFromExerciseId") && actual == planned
        } else {
            substituted == planned && actual != planned
        }
    }

    private fun validateFollowAlongPayload(
        payload: CanonicalJsonValue.Obj,
        phaseKind: String
    ): Boolean {
        if (!payload.hasExactKeys(FOLLOW_PAYLOAD_KEYS)) return false
        val variant = payload.string("variant") ?: return false
        if (variant == "paused") {
            return phaseKind == "paused" && FOLLOW_POSITION_KEYS.all(payload::isNull)
        }
        if (!payload.requiredNonEmptyStrings("blockId") || !payload.isNonNegativeInteger("stepIndex0")) {
            return false
        }
        return when (variant) {
            "circuit_action", "non_circuit_action" ->
                phaseKind == "follow_along_action" && payload.string("followAlongStepKind") == "action" &&
                    payload.requiredNonEmptyStrings("itemId", "exerciseId") &&
                    if (variant == "circuit_action") {
                        payload.isNonNegativeInteger("roundIndex0")
                    } else {
                        payload.isNull("roundIndex0")
                    }

            "circuit_rest_after_action", "non_circuit_rest_after_action" ->
                phaseKind == "follow_along_rest" &&
                    payload.string("followAlongStepKind") == "rest_after_action" &&
                    payload.requiredNonEmptyStrings("itemId", "exerciseId") &&
                    if (variant == "circuit_rest_after_action") {
                        payload.isNonNegativeInteger("roundIndex0")
                    } else {
                        payload.isNull("roundIndex0")
                    }

            "between_round_rest" ->
                phaseKind == "follow_along_rest" &&
                    payload.string("followAlongStepKind") == "between_round_rest" &&
                    payload.isNull("itemId") && payload.isNull("exerciseId") &&
                    payload.isNonNegativeInteger("roundIndex0")

            "block_rest" ->
                phaseKind == "follow_along_rest" &&
                    payload.string("followAlongStepKind") == "block_rest" &&
                    payload.isNull("itemId") && payload.isNull("exerciseId") &&
                    payload.isNull("roundIndex0")

            "boundary" ->
                phaseKind == "follow_along_action" &&
                    payload.string("followAlongStepKind") == "boundary" &&
                    payload.isNull("itemId") && payload.isNull("exerciseId") &&
                    payload.isNull("roundIndex0")

            else -> false
        }
    }

    private fun invalidPhaseIdentity() =
        CanonicalValidationResult.Invalid("invalid_phase_identity_v1")
}

internal data class CompositionSnapshotStep(
    val variant: String,
    val compositionBlockId: String,
    val timelineStageId: String,
    val timelineStageKind: String,
    val stageGroupId: String,
    val targetId: String,
    val targetKind: String,
    val roundIndex0: Long?,
    val stageGroupIndex0: Long?,
    val targetIndex0: Long,
    val stageInstanceIndex0: Long,
    val targetInstanceIndex0: Long,
    val stepIndex0: Long
) {
    fun matches(payload: CanonicalJsonValue.Obj): Boolean =
        payload.string("variant") == variant &&
            payload.string("compositionBlockId") == compositionBlockId &&
            payload.string("timelineStageId") == timelineStageId &&
            payload.string("timelineStageKind") == timelineStageKind &&
            payload.string("stageGroupId") == stageGroupId &&
            payload.string("targetId") == targetId &&
            payload.string("targetKind") == targetKind &&
            payload.matchesNullableInteger("roundIndex0", roundIndex0) &&
            payload.matchesNullableInteger("stageGroupIndex0", stageGroupIndex0) &&
            payload.int("targetIndex0") == targetIndex0 &&
            payload.int("stageInstanceIndex0") == stageInstanceIndex0 &&
            payload.int("targetInstanceIndex0") == targetInstanceIndex0 &&
            payload.int("stepIndex0") == stepIndex0
}

private inline fun validateVersionedObject(
    json: String,
    contract: String,
    versionKey: String,
    validate: (CanonicalJsonValue.Obj) -> Boolean
): CanonicalValidationResult {
    val root = parseCanonicalJson(json) as? CanonicalJsonValue.Obj
        ?: return CanonicalValidationResult.Invalid("invalid_${contract}_contract")
    val version = root.int(versionKey)
        ?: return CanonicalValidationResult.Invalid("invalid_${contract}_contract")
    if (version != 1L) {
        return CanonicalValidationResult.UnsupportedVersion(contract, version.toString())
    }
    return if (validate(root)) {
        CanonicalValidationResult.Valid
    } else {
        CanonicalValidationResult.Invalid("invalid_${contract}_contract")
    }
}

private fun validateDisplayMetadataRoot(root: CanonicalJsonValue.Obj): Boolean {
    if (!root.hasExactKeys(DISPLAY_METADATA_ROOT_KEYS)) return false
    val entries = root.array("entries") ?: return false
    val stableIds = mutableSetOf<String>()
    return entries.all { value ->
        val entry = value as? CanonicalJsonValue.Obj ?: return@all false
        val stableId = entry.string("stableId") ?: return@all false
        entry.hasExactKeys(DISPLAY_METADATA_ENTRY_KEYS) &&
            entry.string("entityKind") == "exercise" && stableId.isNotEmpty() &&
            stableIds.add(stableId) && entry.string("displayNameAtFirstReference") != null &&
            entry.isNullableString("customNameAtFirstReference") &&
            entry.string("resolutionSource") in DISPLAY_RESOLUTION_SOURCES
    }
}

private fun validateZoneSnapshotRoot(root: CanonicalJsonValue.Obj): Boolean {
    if (!root.hasExactKeys(ZONE_SNAPSHOT_ROOT_KEYS) || root.string("unit") != "bpm") return false
    if (!root.isIntegerInRange("effectiveMaxBpm", 30, 260) ||
        root.string("effectiveMaxSource") !in EFFECTIVE_MAX_SOURCES
    ) {
        return false
    }
    val zones = root.array("zones") ?: return false
    if (zones.size != ZONE_DEFINITIONS.size) return false
    return zones.zip(ZONE_DEFINITIONS).all { (value, definition) ->
        val zone = value as? CanonicalJsonValue.Obj ?: return@all false
        zone.hasExactKeys(ZONE_ENTRY_KEYS) && zone.string("zoneId") == definition.id &&
            zone.matchesNullableInteger("lowerBoundBasisPointsInclusive", definition.lower) &&
            zone.matchesNullableInteger("upperBoundBasisPointsExclusive", definition.upper)
    }
}

private fun CanonicalJsonValue.Obj.hasNonNegativeExactKeys(keys: Set<String>): Boolean =
    hasExactKeys(keys) && keys.all(::isNonNegativeInteger)

private fun CanonicalJsonValue.Obj.hasExactKeys(keys: Set<String>): Boolean = fields.keys == keys

private fun CanonicalJsonValue.Obj.string(key: String): String? =
    (fields[key] as? CanonicalJsonValue.Str)?.value

private fun CanonicalJsonValue.Obj.int(key: String): Long? =
    (fields[key] as? CanonicalJsonValue.Num)?.value?.exactLongOrNull()

private fun BigDecimal.exactLongOrNull(): Long? = try {
    longValueExact()
} catch (_: ArithmeticException) {
    null
}

private fun CanonicalJsonValue.Obj.boolean(key: String): Boolean? =
    (fields[key] as? CanonicalJsonValue.Bool)?.value

private fun CanonicalJsonValue.Obj.obj(key: String): CanonicalJsonValue.Obj? =
    fields[key] as? CanonicalJsonValue.Obj

private fun CanonicalJsonValue.Obj.array(key: String): List<CanonicalJsonValue>? =
    (fields[key] as? CanonicalJsonValue.Arr)?.values

private fun CanonicalJsonValue.Obj.isNull(key: String): Boolean =
    fields[key] === CanonicalJsonValue.Null

private fun CanonicalJsonValue.Obj.isNullableString(key: String): Boolean =
    isNull(key) || fields[key] is CanonicalJsonValue.Str

private fun CanonicalJsonValue.Obj.requiredNonEmptyStrings(vararg keys: String): Boolean =
    keys.all { key -> !string(key).isNullOrEmpty() }

private fun CanonicalJsonValue.Obj.isNonNegativeInteger(key: String): Boolean =
    int(key)?.let { value -> value >= 0 } == true

private fun CanonicalJsonValue.Obj.isIntegerInRange(
    key: String,
    minimum: Long,
    maximum: Long
): Boolean = int(key)?.let { value -> value in minimum..maximum } == true

private fun CanonicalJsonValue.Obj.isNullableNonNegativeInteger(key: String): Boolean =
    isNull(key) || isNonNegativeInteger(key)

private fun CanonicalJsonValue.Obj.isNullableIntegerInRange(
    key: String,
    minimum: Long,
    maximum: Long
): Boolean = isNull(key) || isIntegerInRange(key, minimum, maximum)

private fun CanonicalJsonValue.Obj.matchesNullableInteger(key: String, expected: Long?): Boolean =
    if (expected == null) isNull(key) else int(key) == expected

private data class ZoneDefinition(
    val id: String,
    val lower: Long?,
    val upper: Long?
)

private val DISPLAY_METADATA_ROOT_KEYS = setOf("displayMetadataContractVersion", "entries")
private val DISPLAY_METADATA_ENTRY_KEYS = setOf(
    "entityKind",
    "stableId",
    "displayNameAtFirstReference",
    "customNameAtFirstReference",
    "resolutionSource"
)
private val DISPLAY_RESOLUTION_SOURCES = setOf("plan_snapshot", "runtime_substitution")
private val ZONE_SNAPSHOT_ROOT_KEYS = setOf(
    "zoneSnapshotContractVersion",
    "unit",
    "effectiveMaxBpm",
    "effectiveMaxSource",
    "zones"
)
private val ZONE_ENTRY_KEYS = setOf(
    "zoneId",
    "lowerBoundBasisPointsInclusive",
    "upperBoundBasisPointsExclusive"
)
private val ZONE_DEFINITIONS = listOf(
    ZoneDefinition("below_50", null, 5000),
    ZoneDefinition("from_50_to_60", 5000, 6000),
    ZoneDefinition("from_60_to_70", 6000, 7000),
    ZoneDefinition("from_70_to_80", 7000, 8000),
    ZoneDefinition("from_80_to_90", 8000, 9000),
    ZoneDefinition("at_or_above_90", 9000, null)
)
private val EFFECTIVE_MAX_SOURCES = setOf("personal_max", "age_220_minus_age")
private val ANALYSIS_CONFIG_KEYS = setOf(
    "analysisConfigContractVersion",
    "sampleValidityCapMs",
    "sampleIntervalContractVersion",
    "partialLowerBoundBasisPoints",
    "phaseConclusionBasisPoints",
    "normalBasisPoints",
    "coverageThresholdRule",
    "coverageBasisPointsRule",
    "displayPercentRule",
    "weightedAverageRule",
    "averageDisplayRule",
    "zeroCoveredRule",
    "observedMaxRule",
    "zoneAttributionContractVersion",
    "zoneAttributionRule",
    "statusProjectionContractVersion",
    "durationPartitionContractVersion"
)
private val ZONE_DURATION_KEYS = setOf(
    "zoneDurationsContractVersion",
    "below50DurationMs",
    "from50To60DurationMs",
    "from60To70DurationMs",
    "from70To80DurationMs",
    "from80To90DurationMs",
    "atOrAbove90DurationMs"
)
private val PHASE_AGGREGATES_ROOT_KEYS = setOf("phaseAggregatesContractVersion", "aggregates")
private val PHASE_AGGREGATE_ENTRY_KEYS = setOf(
    "phaseSequence",
    "phaseKind",
    "eligibleDurationMs",
    "coveredDurationMs",
    "coverageBasisPoints",
    "coverageStatus",
    "conclusionEligible",
    "weightedBpmMs",
    "observedAvgBpm",
    "observedMaxBpm",
    "highestOffsetMs",
    "highestMutationSequence",
    "highestSampleSequence"
)
private val PHASE_KINDS = setOf(
    "timed_work",
    "timed_rest",
    "strength_prepare_set",
    "strength_active_set",
    "strength_confirm_set",
    "strength_rest",
    "follow_along_action",
    "follow_along_rest",
    "paused"
)
private val COVERAGE_STATUSES = setOf("no_eligible_duration", "insufficient", "partial", "normal")
private val DURATION_BREAKDOWN_ROOT_KEYS = setOf(
    "durationBreakdownContractVersion",
    "canonicalSessionDurationMs",
    "recordingWindowDurationMs",
    "notRequestedBeforeRecordingStartMs",
    "intentAxis",
    "phaseAxis",
    "primaryAnalysisPartition",
    "deviceStateDurations",
    "deviceReasonDurations",
    "orthogonalityContract"
)
private val INTENT_AXIS_KEYS = setOf(
    "expectedRecordingDurationMs",
    "userExcludedDurationMs",
    "userTurnedOffDurationMs",
    "userOptedOutDurationMs",
    "userDisconnectedSuppressRecoveryDurationMs"
)
private val PHASE_AXIS_KEYS = setOf(
    "primaryEligibleDurationMs",
    "phaseExcludedDurationMs",
    "strengthPrepareExcludedDurationMs",
    "pausedExcludedDurationMs"
)
private val PRIMARY_PARTITION_KEYS = setOf(
    "primaryEligibleDurationMs",
    "eligibleCoveredDurationMs",
    "eligibleUncoveredDurationMs"
)
private val DEVICE_STATES = setOf(
    "not_observing",
    "no_source_selected",
    "permission_required",
    "bluetooth_unavailable",
    "searching",
    "connecting",
    "waiting_first_sample",
    "live",
    "stale",
    "reconnecting",
    "disconnected",
    "technical_failure"
)
private val DEVICE_REASONS = setOf(
    "initial_acquisition",
    "automatic_recovery",
    "source_not_selected",
    "source_unavailable",
    "permission_missing",
    "permission_revoked",
    "bluetooth_off",
    "platform_unavailable",
    "first_sample_timeout",
    "sample_stale_timeout",
    "unexpected_disconnect",
    "connection_timeout",
    "measurement_stream_unavailable",
    "platform_failure"
)
private val ORTHOGONALITY_KEYS = setOf("contractVersion", "rule")
private val QUALITY_REASON_ROOT_KEYS = setOf(
    "qualityReasonsContractVersion",
    "sessionReasons",
    "phaseReasons"
)
private val SESSION_REASON_KEYS = setOf("reasonCode", "durationMs")
private val PHASE_REASON_KEYS = setOf("phaseSequence", "reasonCode", "durationMs")
private val QUALITY_REASON_CODES = setOf(
    "no_eligible_duration",
    "no_canonical_samples",
    "canonical_only_excluded",
    "eligible_uncovered_present",
    "insufficient_coverage",
    "partial_coverage",
    "unavailable_no_effective_max",
    "not_requested_before_recording_start",
    "strength_prepare_excluded",
    "paused_excluded",
    "user_turned_off_excluded",
    "user_opted_out_excluded",
    "user_disconnected_suppress_recovery_excluded",
    "process_interrupted"
)
private val PHASE_IDENTITY_ROOT_KEYS = setOf(
    "phaseIdentityContractVersion",
    "family",
    "payloadVersion",
    "mode",
    "phaseKind",
    "orderedStructureSignature",
    "payload"
)
private val SIGNATURE_KEYS = setOf(
    "signatureContractVersion",
    "algorithm",
    "digestHexLowercase"
)
private val LOWERCASE_SHA256 = Regex("[0-9a-f]{64}")
private val LEGACY_PAYLOAD_KEYS = setOf(
    "variant",
    "blockId",
    "stepIndex0",
    "legacyBlockKind",
    "legacyStageType",
    "itemId",
    "exerciseId",
    "roundIndex0"
)
private val LEGACY_POSITION_KEYS = LEGACY_PAYLOAD_KEYS.minus("variant")
private val BOUNDARY_BLOCK_KINDS = setOf("warmup", "stretch", "cooldown")
private val COMPOSITION_PAYLOAD_KEYS = setOf(
    "variant",
    "compositionVersion",
    "compositionBlockId",
    "timelineStageId",
    "timelineStageKind",
    "stageGroupId",
    "targetId",
    "targetKind",
    "roundIndex0",
    "stageGroupIndex0",
    "targetIndex0",
    "stageInstanceIndex0",
    "targetInstanceIndex0",
    "stepIndex0"
)
private val COMPOSITION_POSITION_KEYS = COMPOSITION_PAYLOAD_KEYS.minus(
    setOf("variant", "compositionVersion")
)
private val STRENGTH_PAYLOAD_KEYS = setOf(
    "variant",
    "blockId",
    "setPlanId",
    "plannedExerciseId",
    "actualExerciseId",
    "exerciseSetIndex0",
    "globalSetIndex0",
    "setKind",
    "substitutedFromExerciseId"
)
private val STRENGTH_POSITION_KEYS = STRENGTH_PAYLOAD_KEYS.minus("variant")
private val STRENGTH_VARIANT_PHASE = mapOf(
    "prepare_set" to "strength_prepare_set",
    "active_set" to "strength_active_set",
    "confirm_set" to "strength_confirm_set",
    "rest" to "strength_rest"
)
private val STRENGTH_SET_KINDS = setOf("warmup", "working", "drop", "backoff")
private val FOLLOW_PAYLOAD_KEYS = setOf(
    "variant",
    "blockId",
    "stepIndex0",
    "followAlongStepKind",
    "itemId",
    "exerciseId",
    "roundIndex0"
)
private val FOLLOW_POSITION_KEYS = FOLLOW_PAYLOAD_KEYS.minus("variant")
