package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1
import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1Result
import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1ValidationResult
import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1Validator
import com.liujyks.trainflow.core.data.RecorderValidationException
import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.CanonicalValidationResult
import com.liujyks.trainflow.core.database.PhaseIdentityV1Validator
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.core.database.renderCanonicalJson
import com.liujyks.trainflow.core.engine.TimedSessionStep
import com.liujyks.trainflow.core.engine.TimedSessionStepKind
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineResult
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepRecord
import com.liujyks.trainflow.core.model.TimedCompositionTimeline
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStageKind
import com.liujyks.trainflow.core.model.TimedRestExtensionRecord
import com.liujyks.trainflow.core.model.WorkoutEvent
import com.liujyks.trainflow.core.model.WorkoutMode
import java.math.BigDecimal
import java.time.Instant
import java.util.Collections

internal data class TimedFocusPhaseV1(
    val sessionId: String,
    val phaseSequence: Long,
    val phaseKind: String,
    val plannedDurationMs: Long?,
    val phaseIdentityJson: String
)

internal sealed interface TimedStructureResolutionV1 {
    data class Resolved(val structure: TimedResolvedStructureV1) : TimedStructureResolutionV1
    data class InvalidSnapshot(val result: PlanSnapshotStorageV1ValidationResult) : TimedStructureResolutionV1
    data class InvalidPhase(val result: CanonicalValidationResult) : TimedStructureResolutionV1
    data class InvalidInput(val reason: String) : TimedStructureResolutionV1
}

internal data class TimedResolvedStructureV1(
    val sessionId: String,
    val family: String,
    val structureDigestHexLowercase: String,
    val phases: List<TimedResolvedFocusPhaseV1>,
    val hasTrueWork: Boolean,
    val hasTrueRest: Boolean,
    val focusEligible: Boolean
)

internal data class TimedResolvedFocusPhaseV1(
    val phaseSequence: Long,
    val blockId: String?,
    val roundIndex0: Long?,
    val trueWork: Boolean,
    val trueRest: Boolean
)

internal data class TimedFocusIdentityV1(
    val focusContractVersion: Int,
    val sessionId: String,
    val family: String,
    val structureDigestHexLowercase: String,
    val focusKind: String,
    val legacyBlockId: String?,
    val compositionBlockId: String?,
    val roundIndex0: Long?,
    val phaseSequence: Long?
)

internal sealed interface TimedFocusValidationV1 {
    data class Valid(val identity: TimedFocusIdentityV1) : TimedFocusValidationV1
    data object Invalid : TimedFocusValidationV1 {
        val code: String = "invalid_timed_focus_identity"
    }
}

internal fun resolveTimedStructureV1(
    sessionId: String,
    family: String,
    snapshotJson: String,
    phases: List<TimedFocusPhaseV1>
): TimedStructureResolutionV1 {
    val prepared = when (val result = PlanSnapshotStorageV1Validator.prepare(snapshotJson, WorkoutMode.TIMED)) {
        is PreparedPlanSnapshotStorageV1Result.Valid -> result.prepared
        is PreparedPlanSnapshotStorageV1Result.Invalid ->
            return TimedStructureResolutionV1.InvalidSnapshot(result.result)
    }
    if (family != "legacy_timed_v1" && family != "timed_composition_v2") {
        return TimedStructureResolutionV1.InvalidInput("unsupported_family")
    }
    val context = PhaseIdentityV1Validator.prepareContext(prepared)
        ?: return TimedStructureResolutionV1.InvalidPhase(
            CanonicalValidationResult.Invalid("invalid_phase_identity")
        )
    val resolved = phases.map { phase ->
        if (phase.sessionId != sessionId) {
            return TimedStructureResolutionV1.InvalidInput("session_mismatch")
        }
        val validation = PhaseIdentityV1Validator.validatePrepared(
            phase.phaseIdentityJson, context, phase.phaseKind
        )
        if (validation != CanonicalValidationResult.Valid) {
            return TimedStructureResolutionV1.InvalidPhase(validation)
        }
        val envelope = parseCanonicalJson(phase.phaseIdentityJson) as CanonicalJsonValue.Obj
        val actualFamily = (envelope.fields.getValue("family") as CanonicalJsonValue.Str).value
        if (actualFamily != family) {
            return TimedStructureResolutionV1.InvalidInput("family_mismatch")
        }
        val payload = envelope.fields.getValue("payload") as CanonicalJsonValue.Obj
        val variant = (payload.fields.getValue("variant") as CanonicalJsonValue.Str).value
        val duration = phase.plannedDurationMs
        val trueWork = when (actualFamily) {
            "legacy_timed_v1" -> when (variant) {
                "circuit_item_work" ->
                    (payload.fields.getValue("legacyStageType") as CanonicalJsonValue.Str).value in
                        setOf("work", "custom") && duration!! > 0
                else -> false
            }
            else -> when (variant) {
                "stage_group_action" -> duration!! > 0
                "stage_group_custom" -> duration!! > 0
                else -> false
            }
        }
        val trueRest = when (actualFamily) {
            "legacy_timed_v1" -> when (variant) {
                "circuit_item_rest" -> duration!! > 0
                "circuit_rest_after_item" -> duration!! > 0
                "between_round_rest" -> duration!! > 0
                else -> false
            }
            else -> when (variant) {
                "stage_group_rest" -> duration!! > 0
                "between_round_rest" -> duration!! > 0
                else -> false
            }
        }
        val blockKey = if (actualFamily == "legacy_timed_v1") "blockId" else "compositionBlockId"
        TimedResolvedFocusPhaseV1(
            phaseSequence = phase.phaseSequence,
            blockId = (payload.fields.getValue(blockKey) as? CanonicalJsonValue.Str)?.value,
            roundIndex0 = (payload.fields.getValue("roundIndex0") as? CanonicalJsonValue.Num)
                ?.value?.longValueExact(),
            trueWork = trueWork,
            trueRest = trueRest
        )
    }
    val hasTrueWork = resolved.any { it.trueWork }
    val hasTrueRest = resolved.any { it.trueRest }
    return TimedStructureResolutionV1.Resolved(TimedResolvedStructureV1(
        sessionId, family, prepared.orderedStructureDigestHexLowercase(),
        Collections.unmodifiableList(resolved), hasTrueWork, hasTrueRest, hasTrueWork && hasTrueRest
    ))
}

internal fun validateTimedFocusV1(
    structure: TimedResolvedStructureV1,
    focusJson: String
): TimedFocusValidationV1 {
    val invalid = TimedFocusValidationV1.Invalid
    val fields = (parseCanonicalJson(focusJson) as? CanonicalJsonValue.Obj)?.fields ?: return invalid
    if (fields.keys != setOf(
            "focusContractVersion", "sessionId", "family", "structureDigestHexLowercase", "focusKind",
            "legacyBlockId", "compositionBlockId", "roundIndex0", "phaseSequence"
        )) return invalid
    val version = (fields.getValue("focusContractVersion") as? CanonicalJsonValue.Num)?.value
        ?: return invalid
    if (version.compareTo(BigDecimal.ONE) != 0) return invalid
    val sessionId = (fields.getValue("sessionId") as? CanonicalJsonValue.Str)?.value ?: return invalid
    val family = (fields.getValue("family") as? CanonicalJsonValue.Str)?.value ?: return invalid
    val digest = (fields.getValue("structureDigestHexLowercase") as? CanonicalJsonValue.Str)?.value
        ?: return invalid
    val kind = (fields.getValue("focusKind") as? CanonicalJsonValue.Str)?.value ?: return invalid
    if (sessionId != structure.sessionId || family != structure.family ||
        digest != structure.structureDigestHexLowercase) return invalid
    for (key in listOf("legacyBlockId", "compositionBlockId")) {
        val value = fields.getValue(key)
        if (value !is CanonicalJsonValue.Str && value != CanonicalJsonValue.Null) return invalid
    }
    for (key in listOf("roundIndex0", "phaseSequence")) {
        val value = fields.getValue(key)
        if (value == CanonicalJsonValue.Null) continue
        if (value !is CanonicalJsonValue.Num || value.value < BigDecimal.ZERO ||
            value.value > Long.MAX_VALUE.toBigDecimal() ||
            value.value.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) return invalid
    }
    val legacyBlockId = (fields.getValue("legacyBlockId") as? CanonicalJsonValue.Str)?.value
    val compositionBlockId = (fields.getValue("compositionBlockId") as? CanonicalJsonValue.Str)?.value
    val round = (fields.getValue("roundIndex0") as? CanonicalJsonValue.Num)?.value?.longValueExact()
    val sequence = (fields.getValue("phaseSequence") as? CanonicalJsonValue.Num)?.value?.longValueExact()
    val blockId = if (family == "legacy_timed_v1") {
        if (compositionBlockId != null) return invalid
        legacyBlockId
    } else {
        if (legacyBlockId != null) return invalid
        compositionBlockId
    }
    when (kind) {
        "whole" -> if (blockId != null || round != null || sequence != null) return invalid
        "round" -> if (blockId == null || round == null || sequence != null ||
            structure.phases.none { it.blockId == blockId && it.roundIndex0 == round }) return invalid
        "work", "rest" -> {
            if (blockId == null || round == null || sequence == null) return invalid
            val phase = structure.phases.singleOrNull { it.phaseSequence == sequence } ?: return invalid
            if (phase.blockId != blockId || phase.roundIndex0 != round ||
                !(if (kind == "work") phase.trueWork else phase.trueRest)) return invalid
        }
        else -> return invalid
    }
    return TimedFocusValidationV1.Valid(TimedFocusIdentityV1(
        1, sessionId, family, digest, kind, legacyBlockId, compositionBlockId, round, sequence
    ))
}

internal fun restoreTimedFocusV1(
    structure: TimedResolvedStructureV1,
    focusJson: String
): TimedFocusIdentityV1 = when (val result = validateTimedFocusV1(structure, focusJson)) {
    is TimedFocusValidationV1.Valid -> result.identity
    TimedFocusValidationV1.Invalid -> TimedFocusIdentityV1(
        1, structure.sessionId, structure.family, structure.structureDigestHexLowercase,
        "whole", null, null, null, null
    )
}

internal data class TimedCanonicalStepFactsV1(
    val sourceStepId: String,
    val phaseKind: String,
    val plannedDurationMs: Long,
    val phaseIdentityJson: String
)

internal data class TimedCanonicalPhaseFactsV1(
    val sourceStep: TimedSessionStep?,
    val phaseKind: String,
    val plannedDurationMs: Long?,
    val phaseIdentityJson: String
)

internal data class LegacyTimedCompletedStepFactsV1(
    val record: SessionStepRecord,
    val stepFacts: TimedCanonicalStepFactsV1
)

internal data class LegacyTimedRestExtensionFactsV1(
    val record: TimedRestExtensionRecord,
    val restStepFacts: TimedCanonicalStepFactsV1
)

internal data class LegacyTimedTransitionFactsV1(
    val phaseStarts: List<TimedCanonicalPhaseFactsV1>,
    val completedSteps: List<LegacyTimedCompletedStepFactsV1>,
    val restExtensions: List<LegacyTimedRestExtensionFactsV1>,
    val terminalStatus: SessionStatus?
)

internal fun legacyTimedTransitionFactsV1(
    snapshot: PreparedPlanSnapshotStorageV1,
    before: TimedWorkoutEngineState,
    result: TimedWorkoutEngineResult,
    startedAt: Instant
): LegacyTimedTransitionFactsV1 {
    val state = result.state
    val phaseStarts = result.events.mapNotNull { event ->
        val stepId = when (event) {
            is WorkoutEvent.TimedWorkStarted -> event.stepId
            is WorkoutEvent.RestStarted -> event.stepId
            is WorkoutEvent.SessionResumed -> state.currentStep!!.id
            is WorkoutEvent.SessionPaused -> {
                val payload = CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("paused"),
                    "blockId" to CanonicalJsonValue.Null,
                    "stepIndex0" to CanonicalJsonValue.Null,
                    "legacyBlockKind" to CanonicalJsonValue.Null,
                    "legacyStageType" to CanonicalJsonValue.Null,
                    "itemId" to CanonicalJsonValue.Null,
                    "exerciseId" to CanonicalJsonValue.Null,
                    "roundIndex0" to CanonicalJsonValue.Null
                ))
                return@mapNotNull TimedCanonicalPhaseFactsV1(
                    sourceStep = null,
                    phaseKind = "paused",
                    plannedDurationMs = null,
                    phaseIdentityJson = validatedLegacyIdentityJsonV1(snapshot, "paused", payload)
                )
            }
            else -> return@mapNotNull null
        }
        val step = state.steps.single { it.id == stepId }
        val facts = legacyTimedStepFactsV1(snapshot, state.steps, step)
        TimedCanonicalPhaseFactsV1(step, facts.phaseKind, facts.plannedDurationMs, facts.phaseIdentityJson)
    }
    val completedSteps = state.toTimedSessionStepRecords(startedAt)
        .drop(before.stepHistory.count { it.actualDurationSec != null })
        .map { record ->
            val step = state.steps.single { it.id == record.stepId }
            LegacyTimedCompletedStepFactsV1(record, legacyTimedStepFactsV1(snapshot, state.steps, step))
        }
    val restExtensions = state.toTimedRestExtensionRecords()
        .drop(before.restExtensionHistory.size)
        .map { record ->
            val step = state.steps[record.stepIndex]
            LegacyTimedRestExtensionFactsV1(record, legacyTimedStepFactsV1(snapshot, state.steps, step))
        }
    return LegacyTimedTransitionFactsV1(
        phaseStarts = phaseStarts,
        completedSteps = completedSteps,
        restExtensions = restExtensions,
        terminalStatus = state.status.takeIf { state.isTerminal && !before.isTerminal }
    )
}

private fun legacyTimedStepFactsV1(
    snapshot: PreparedPlanSnapshotStorageV1,
    steps: List<TimedSessionStep>,
    step: TimedSessionStep
): TimedCanonicalStepFactsV1 {
    val block = snapshot.phaseBindingBlocks().singleOrNull { it.id == step.blockId }
        ?: throw RecorderValidationException("invalid_phase_identity")
    val blockStepIndex0 = steps.filter { it.blockId == step.blockId }.indexOf(step)
    return when (block.kind) {
        "timed_circuit" -> legacyCircuitStepFactsV1(snapshot, step, blockStepIndex0)
        "warmup", "stretch", "cooldown" -> if (block.items.isEmpty()) {
            legacyBoundaryBlockStepFactsV1(snapshot, step, blockStepIndex0)
        } else {
            legacyBoundaryItemStepFactsV1(snapshot, step, blockStepIndex0)
        }
        "rest" -> {
            val payload = CanonicalJsonValue.Obj(linkedMapOf(
                "variant" to CanonicalJsonValue.Str("standalone_rest"),
                "blockId" to CanonicalJsonValue.Str(block.id),
                "stepIndex0" to CanonicalJsonValue.Num(blockStepIndex0.toBigDecimal()),
                "legacyBlockKind" to CanonicalJsonValue.Str("rest"),
                "legacyStageType" to CanonicalJsonValue.Str("rest"),
                "itemId" to CanonicalJsonValue.Null,
                "exerciseId" to CanonicalJsonValue.Null,
                "roundIndex0" to CanonicalJsonValue.Null
            ))
            validatedLegacyStepFactsV1(snapshot, step, "timed_rest", payload)
        }
        else -> throw RecorderValidationException("invalid_phase_identity")
    }
}

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
    if (block.kind !in setOf("warmup", "cooldown", "stretch") || block.items.isNotEmpty() ||
        step.kind != TimedSessionStepKind.WORK || step.itemId != null || step.round != null
    ) {
        throw RecorderValidationException("invalid_phase_identity")
    }
    val payload = CanonicalJsonValue.Obj(linkedMapOf(
        "variant" to CanonicalJsonValue.Str("boundary_block_work"),
        "blockId" to CanonicalJsonValue.Str(block.id),
        "stepIndex0" to CanonicalJsonValue.Num(blockStepIndex0.toBigDecimal()),
        "legacyBlockKind" to CanonicalJsonValue.Str(block.kind),
        "legacyStageType" to CanonicalJsonValue.Str(if (block.kind == "stretch") "cooldown" else block.kind),
        "itemId" to CanonicalJsonValue.Null,
        "exerciseId" to CanonicalJsonValue.Null,
        "roundIndex0" to CanonicalJsonValue.Null
    ))
    return validatedLegacyStepFactsV1(snapshot, step, "timed_work", payload)
}

internal fun legacyBoundaryItemStepFactsV1(
    snapshot: PreparedPlanSnapshotStorageV1,
    step: TimedSessionStep,
    blockStepIndex0: Int
): TimedCanonicalStepFactsV1 {
    val block = snapshot.phaseBindingBlocks().singleOrNull { it.id == step.blockId }
        ?: throw RecorderValidationException("invalid_phase_identity")
    val item = block.items.singleOrNull { it.id == step.itemId }
        ?: throw RecorderValidationException("invalid_phase_identity")
    if (block.kind !in setOf("warmup", "stretch", "cooldown") || step.round != null) {
        throw RecorderValidationException("invalid_phase_identity")
    }
    val phaseKind = when (step.kind) {
        TimedSessionStepKind.WORK -> "timed_work"
        TimedSessionStepKind.REST -> "timed_rest"
    }
    val variant = when (step.kind) {
        TimedSessionStepKind.WORK -> "boundary_item_work"
        TimedSessionStepKind.REST ->
            if (item.stageType == "rest") "boundary_item_rest" else "boundary_rest_after_item"
    }
    val payload = CanonicalJsonValue.Obj(linkedMapOf(
        "variant" to CanonicalJsonValue.Str(variant),
        "blockId" to CanonicalJsonValue.Str(block.id),
        "stepIndex0" to CanonicalJsonValue.Num(blockStepIndex0.toBigDecimal()),
        "legacyBlockKind" to CanonicalJsonValue.Str(block.kind),
        "legacyStageType" to CanonicalJsonValue.Str(
            if (step.kind == TimedSessionStepKind.WORK) item.stageType else "rest"
        ),
        "itemId" to CanonicalJsonValue.Str(item.id),
        "exerciseId" to (if (item.stageType == "rest") CanonicalJsonValue.Null
            else item.exerciseId?.let { CanonicalJsonValue.Str(it) } ?: CanonicalJsonValue.Null),
        "roundIndex0" to CanonicalJsonValue.Null
    ))
    return validatedLegacyStepFactsV1(snapshot, step, phaseKind, payload)
}

private fun validatedLegacyStepFactsV1(
    snapshot: PreparedPlanSnapshotStorageV1,
    step: TimedSessionStep,
    phaseKind: String,
    payload: CanonicalJsonValue.Obj
): TimedCanonicalStepFactsV1 {
    return TimedCanonicalStepFactsV1(
        sourceStepId = step.id,
        phaseKind = phaseKind,
        plannedDurationMs = step.durationSec.toLong() * 1000L,
        phaseIdentityJson = validatedLegacyIdentityJsonV1(snapshot, phaseKind, payload)
    )
}

private fun validatedLegacyIdentityJsonV1(
    snapshot: PreparedPlanSnapshotStorageV1,
    phaseKind: String,
    payload: CanonicalJsonValue.Obj
): String {
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
    return identity
}

internal fun compositionTimedTransitionFactsV1(
    snapshot: PreparedPlanSnapshotStorageV1,
    timelines: List<TimedCompositionTimeline>,
    before: TimedWorkoutEngineState,
    result: TimedWorkoutEngineResult,
    startedAt: Instant
): LegacyTimedTransitionFactsV1 {
    val state = result.state
    val phaseStarts = result.events.mapNotNull { event ->
        val stepId = when (event) {
            is WorkoutEvent.TimedWorkStarted -> event.stepId
            is WorkoutEvent.RestStarted -> event.stepId
            is WorkoutEvent.SessionResumed -> state.currentStep!!.id
            is WorkoutEvent.SessionPaused -> {
                val payload = CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("paused"),
                    "compositionVersion" to CanonicalJsonValue.Num(2.toBigDecimal()),
                    "compositionBlockId" to CanonicalJsonValue.Null,
                    "timelineStageId" to CanonicalJsonValue.Null,
                    "timelineStageKind" to CanonicalJsonValue.Null,
                    "stageGroupId" to CanonicalJsonValue.Null,
                    "targetId" to CanonicalJsonValue.Null,
                    "targetKind" to CanonicalJsonValue.Null,
                    "roundIndex0" to CanonicalJsonValue.Null,
                    "stageGroupIndex0" to CanonicalJsonValue.Null,
                    "targetIndex0" to CanonicalJsonValue.Null,
                    "stageInstanceIndex0" to CanonicalJsonValue.Null,
                    "targetInstanceIndex0" to CanonicalJsonValue.Null,
                    "stepIndex0" to CanonicalJsonValue.Null
                ))
                return@mapNotNull TimedCanonicalPhaseFactsV1(
                    sourceStep = null,
                    phaseKind = "paused",
                    plannedDurationMs = null,
                    phaseIdentityJson = validatedCompositionIdentityJsonV1(snapshot, "paused", payload)
                )
            }
            else -> return@mapNotNull null
        }
        val step = state.steps.single { it.id == stepId }
        val facts = compositionTimedStepFactsV1(snapshot, timelines, step)
        TimedCanonicalPhaseFactsV1(step, facts.phaseKind, facts.plannedDurationMs, facts.phaseIdentityJson)
    }
    val completedSteps = state.toTimedSessionStepRecords(startedAt)
        .drop(before.stepHistory.count { it.actualDurationSec != null })
        .map { record ->
            val step = state.steps.single { it.id == record.stepId }
            LegacyTimedCompletedStepFactsV1(record, compositionTimedStepFactsV1(snapshot, timelines, step))
        }
    val restExtensions = state.toTimedRestExtensionRecords()
        .drop(before.restExtensionHistory.size)
        .map { record ->
            val step = state.steps[record.stepIndex]
            LegacyTimedRestExtensionFactsV1(record, compositionTimedStepFactsV1(snapshot, timelines, step))
        }
    return LegacyTimedTransitionFactsV1(
        phaseStarts = phaseStarts,
        completedSteps = completedSteps,
        restExtensions = restExtensions,
        terminalStatus = state.status.takeIf { state.isTerminal && !before.isTerminal }
    )
}

private fun compositionTimedStepFactsV1(
    snapshot: PreparedPlanSnapshotStorageV1,
    timelines: List<TimedCompositionTimeline>,
    step: TimedSessionStep
): TimedCanonicalStepFactsV1 {
    val timeline = timelines.singleOrNull { it.compositionBlockId == step.blockId }
        ?: throw RecorderValidationException("invalid_phase_identity")
    val stepIndex0 = timeline.steps.indexOfFirst { it.id == step.id }
    if (stepIndex0 < 0) throw RecorderValidationException("invalid_phase_identity")
    val metadata = timeline.steps[stepIndex0]
    val variant = when (metadata.timelineStageKind) {
        TimedCompositionTimelineStageKind.STAGE_GROUP -> "stage_group_${metadata.targetKind.contractValue}"
        else -> metadata.timelineStageKind.contractValue
    }
    val phaseKind = when (step.kind) {
        TimedSessionStepKind.WORK -> "timed_work"
        TimedSessionStepKind.REST -> "timed_rest"
    }
    val payload = CanonicalJsonValue.Obj(linkedMapOf(
        "variant" to CanonicalJsonValue.Str(variant),
        "compositionVersion" to CanonicalJsonValue.Num(metadata.compositionVersion.toBigDecimal()),
        "compositionBlockId" to CanonicalJsonValue.Str(metadata.compositionBlockId),
        "timelineStageId" to CanonicalJsonValue.Str(metadata.timelineStageId),
        "timelineStageKind" to CanonicalJsonValue.Str(metadata.timelineStageKind.contractValue),
        "stageGroupId" to CanonicalJsonValue.Str(metadata.stageGroupId),
        "targetId" to CanonicalJsonValue.Str(metadata.targetId),
        "targetKind" to CanonicalJsonValue.Str(metadata.targetKind.contractValue),
        "roundIndex0" to (metadata.roundIndex?.let { CanonicalJsonValue.Num((it - 1).toBigDecimal()) }
            ?: CanonicalJsonValue.Null),
        "stageGroupIndex0" to (metadata.stageGroupIndex?.let { CanonicalJsonValue.Num((it - 1).toBigDecimal()) }
            ?: CanonicalJsonValue.Null),
        "targetIndex0" to CanonicalJsonValue.Num((metadata.targetIndex - 1).toBigDecimal()),
        "stageInstanceIndex0" to CanonicalJsonValue.Num((metadata.stageInstanceIndex - 1).toBigDecimal()),
        "targetInstanceIndex0" to CanonicalJsonValue.Num((metadata.targetInstanceIndex - 1).toBigDecimal()),
        "stepIndex0" to CanonicalJsonValue.Num(stepIndex0.toBigDecimal())
    ))
    return TimedCanonicalStepFactsV1(
        sourceStepId = step.id,
        phaseKind = phaseKind,
        plannedDurationMs = step.durationSec.toLong() * 1000L,
        phaseIdentityJson = validatedCompositionIdentityJsonV1(snapshot, phaseKind, payload)
    )
}

private fun validatedCompositionIdentityJsonV1(
    snapshot: PreparedPlanSnapshotStorageV1,
    phaseKind: String,
    payload: CanonicalJsonValue.Obj
): String {
    val identity = CanonicalJsonValue.Obj(linkedMapOf(
        "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
        "family" to CanonicalJsonValue.Str("timed_composition_v2"),
        "payloadVersion" to CanonicalJsonValue.Num(2.toBigDecimal()),
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
    return identity
}
