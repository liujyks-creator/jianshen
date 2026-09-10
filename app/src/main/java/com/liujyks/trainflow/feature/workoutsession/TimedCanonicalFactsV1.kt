package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1
import com.liujyks.trainflow.core.data.RecorderValidationException
import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.CanonicalValidationResult
import com.liujyks.trainflow.core.database.PhaseIdentityV1Validator
import com.liujyks.trainflow.core.database.renderCanonicalJson
import com.liujyks.trainflow.core.engine.TimedSessionStep
import com.liujyks.trainflow.core.engine.TimedSessionStepKind

internal data class TimedCanonicalStepFactsV1(
    val sourceStepId: String,
    val phaseKind: String,
    val plannedDurationMs: Long,
    val phaseIdentityJson: String
)

internal fun legacyCircuitStepFactsV1(
    snapshot: PreparedPlanSnapshotStorageV1,
    step: TimedSessionStep,
    blockStepIndex0: Int
): TimedCanonicalStepFactsV1 {
    val block = snapshot.phaseBindingBlocks().singleOrNull { it.id == step.blockId }
        ?: throw RecorderValidationException("invalid_phase_identity")
    val item = step.itemId?.let { itemId ->
        block.items.singleOrNull { it.id == itemId }
            ?: throw RecorderValidationException("invalid_phase_identity")
    }
    if (block.kind != "timed_circuit" ||
        (item != null && item.stageType !in setOf("work", "custom", "rest")) ||
        (item == null && step.kind == TimedSessionStepKind.WORK)
    ) {
        throw RecorderValidationException("invalid_phase_identity")
    }
    val round = step.round ?: throw RecorderValidationException("invalid_phase_identity")
    val phaseKind = when (step.kind) {
        TimedSessionStepKind.WORK -> "timed_work"
        TimedSessionStepKind.REST -> "timed_rest"
    }
    val variant = when (step.kind) {
        TimedSessionStepKind.WORK -> "circuit_item_work"
        TimedSessionStepKind.REST -> when {
            item == null -> "between_round_rest"
            item.stageType == "rest" -> "circuit_item_rest"
            else -> "circuit_rest_after_item"
        }
    }
    val payload = CanonicalJsonValue.Obj(linkedMapOf(
        "variant" to CanonicalJsonValue.Str(variant),
        "blockId" to CanonicalJsonValue.Str(block.id),
        "stepIndex0" to CanonicalJsonValue.Num(blockStepIndex0.toBigDecimal()),
        "legacyBlockKind" to CanonicalJsonValue.Str(block.kind),
        "legacyStageType" to CanonicalJsonValue.Str(
            if (step.kind == TimedSessionStepKind.WORK) item!!.stageType else "rest"
        ),
        "itemId" to (item?.id?.let { CanonicalJsonValue.Str(it) } ?: CanonicalJsonValue.Null),
        "exerciseId" to (if (item?.stageType == "rest") CanonicalJsonValue.Null
            else item?.exerciseId?.let { CanonicalJsonValue.Str(it) } ?: CanonicalJsonValue.Null),
        "roundIndex0" to CanonicalJsonValue.Num((round - 1).toBigDecimal())
    ))
    return validatedLegacyStepFactsV1(snapshot, step, phaseKind, payload)
}

internal fun legacyBoundaryBlockStepFactsV1(
    snapshot: PreparedPlanSnapshotStorageV1,
    step: TimedSessionStep,
    blockStepIndex0: Int
): TimedCanonicalStepFactsV1 {
    val block = snapshot.phaseBindingBlocks().singleOrNull { it.id == step.blockId }
        ?: throw RecorderValidationException("invalid_phase_identity")
    if (block.kind !in setOf("warmup", "cooldown") || block.items.isNotEmpty() ||
        step.kind != TimedSessionStepKind.WORK || step.itemId != null || step.round != null
    ) {
        throw RecorderValidationException("invalid_phase_identity")
    }
    val payload = CanonicalJsonValue.Obj(linkedMapOf(
        "variant" to CanonicalJsonValue.Str("boundary_block_work"),
        "blockId" to CanonicalJsonValue.Str(block.id),
        "stepIndex0" to CanonicalJsonValue.Num(blockStepIndex0.toBigDecimal()),
        "legacyBlockKind" to CanonicalJsonValue.Str(block.kind),
        "legacyStageType" to CanonicalJsonValue.Str(block.kind),
        "itemId" to CanonicalJsonValue.Null,
        "exerciseId" to CanonicalJsonValue.Null,
        "roundIndex0" to CanonicalJsonValue.Null
    ))
    return validatedLegacyStepFactsV1(snapshot, step, "timed_work", payload)
}

private fun validatedLegacyStepFactsV1(
    snapshot: PreparedPlanSnapshotStorageV1,
    step: TimedSessionStep,
    phaseKind: String,
    payload: CanonicalJsonValue.Obj
): TimedCanonicalStepFactsV1 {
    val identity = CanonicalJsonValue.Obj(linkedMapOf(
        "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
        "family" to CanonicalJsonValue.Str("legacy_timed_v1"),
        "payloadVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
        "mode" to CanonicalJsonValue.Str("timed"),
        "phaseKind" to CanonicalJsonValue.Str(phaseKind),
        "orderedStructureSignature" to CanonicalJsonValue.Obj(linkedMapOf(
            "signatureContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
            "algorithm" to CanonicalJsonValue.Str("sha256"),
            "digestHexLowercase" to CanonicalJsonValue.Str(snapshot.orderedStructureDigestHexLowercase())
        )),
        "payload" to payload
    )).renderCanonicalJson()
    if (PhaseIdentityV1Validator.validate(identity, snapshot.storage(), phaseKind) != CanonicalValidationResult.Valid) {
        throw RecorderValidationException("invalid_phase_identity")
    }
    return TimedCanonicalStepFactsV1(
        sourceStepId = step.id,
        phaseKind = phaseKind,
        plannedDurationMs = step.durationSec.toLong() * 1000L,
        phaseIdentityJson = identity
    )
}
