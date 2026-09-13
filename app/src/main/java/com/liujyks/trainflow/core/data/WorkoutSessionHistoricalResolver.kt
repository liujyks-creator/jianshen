package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.feature.workoutsession.TimedFocusPhaseV1
import com.liujyks.trainflow.feature.workoutsession.TimedResolvedStructureV1
import com.liujyks.trainflow.feature.workoutsession.TimedStructureResolutionV1
import com.liujyks.trainflow.feature.workoutsession.resolveTimedStructureV1

internal sealed interface WorkoutSessionHistoricalResult {
    data class Resolved(
        val source: WorkoutSessionStrictReadResult,
        val title: String,
        val mode: String,
        val displayLocale: String,
        val phaseDisplays: List<HistoricalPhaseDisplay>,
        val timedStructures: List<TimedResolvedStructureV1>
    ) : WorkoutSessionHistoricalResult

    data class Forwarded(val source: WorkoutSessionStrictReadResult) : WorkoutSessionHistoricalResult

    data class InvalidPlannedDuration(
        val sessionId: String,
        val family: String,
        val phaseSequence: Int,
        val plannedDurationSec: Long
    ) : WorkoutSessionHistoricalResult

    data class InvalidTimedStructure(
        val sessionId: String,
        val family: String,
        val cause: TimedStructureResolutionV1
    ) : WorkoutSessionHistoricalResult
}

internal data class HistoricalPhaseDisplay(
    val sequence: Int,
    val phaseIdentity: CanonicalJsonValue.Obj,
    val timedPlannedDurationMs: Long?,
    val display: HistoricalDisplayV1
)

internal data class HistoricalDisplayV1(
    val locale: String,
    val resolutionStatus: String,
    val label: String?
) {
    val displayContractVersion: Int = 1
}

internal fun resolveWorkoutSessionHistorical(
    input: WorkoutSessionStrictReadResult,
    displayLocale: String
): WorkoutSessionHistoricalResult {
    val root = when (input) {
        is WorkoutSessionStrictReadResult.CanonicalTerminal -> input.planRoot
        is WorkoutSessionStrictReadResult.LegacyTerminal -> input.planRoot
        else -> return WorkoutSessionHistoricalResult.Forwarded(input)
    }
    val title = (root.fields.getValue("title") as CanonicalJsonValue.Str).value
    val mode = (root.fields.getValue("mode") as CanonicalJsonValue.Str).value
    if (input is WorkoutSessionStrictReadResult.LegacyTerminal) {
        return WorkoutSessionHistoricalResult.Resolved(input, title, mode, displayLocale, emptyList(), emptyList())
    }
    input as WorkoutSessionStrictReadResult.CanonicalTerminal
    val session = input.graph.session
    val blocks = (root.fields.getValue("blocks") as CanonicalJsonValue.Arr).values
        .map { it as CanonicalJsonValue.Obj }
    val metadata = parseCanonicalJson(session.sessionDisplayMetadataJson!!) as CanonicalJsonValue.Obj
    val entries = (metadata.fields.getValue("entries") as CanonicalJsonValue.Arr).values
        .map { it as CanonicalJsonValue.Obj }
    val displays = mutableListOf<HistoricalPhaseDisplay>()
    val timedFamilies = linkedMapOf<String, MutableList<TimedFocusPhaseV1>>()
    for (phase in input.graph.phases) {
        val identity = parseCanonicalJson(phase.phaseIdentityJson) as CanonicalJsonValue.Obj
        val family = (identity.fields.getValue("family") as CanonicalJsonValue.Str).value
        val payload = identity.fields.getValue("payload") as CanonicalJsonValue.Obj
        val variant = (payload.fields.getValue("variant") as CanonicalJsonValue.Str).value
        var name: String? = null
        var plannedSeconds: Long? = null
        if (variant != "paused") {
            when (family) {
                "strength_v1" -> name = frozenExerciseName(
                    (payload.fields.getValue("actualExerciseId") as CanonicalJsonValue.Str).value, entries
                )
                "timed_composition_v2" -> {
                    val blockId = payload.fields.getValue("compositionBlockId")
                    val block = blocks.first { it.fields.getValue("id") == blockId }
                    when (variant) {
                        "warmup" -> plannedSeconds = block.historicalSeconds("warmupSec")
                        "cooldown" -> plannedSeconds = block.historicalSeconds("cooldownSec")
                        "between_round_rest" -> plannedSeconds = block.historicalSeconds("restBetweenRoundsSec")
                        else -> {
                            val groups = (block.fields.getValue("stageGroups") as CanonicalJsonValue.Arr).values
                                .map { it as CanonicalJsonValue.Obj }
                                .sortedBy { (it.fields.getValue("order") as CanonicalJsonValue.Num).value }
                            val groupIndex = (payload.fields.getValue("stageGroupIndex0") as CanonicalJsonValue.Num).value.intValueExact()
                            val targets = (groups[groupIndex].fields.getValue("targets") as CanonicalJsonValue.Arr).values
                                .map { it as CanonicalJsonValue.Obj }
                                .sortedBy { (it.fields.getValue("order") as CanonicalJsonValue.Num).value }
                            val targetIndex = (payload.fields.getValue("targetIndex0") as CanonicalJsonValue.Num).value.intValueExact()
                            val target = targets[targetIndex]
                            name = (target.fields.getValue("name") as CanonicalJsonValue.Str).value
                            plannedSeconds = target.historicalSeconds("durationSec")
                        }
                    }
                }
                else -> {
                    val blockId = payload.fields.getValue("blockId")
                    val block = blocks.first { it.fields.getValue("id") == blockId }
                    when (variant) {
                        "boundary_block_work", "boundary" -> {
                            name = (block.fields["title"] as? CanonicalJsonValue.Str)?.value
                            if (family == "legacy_timed_v1") plannedSeconds = block.historicalSeconds("durationSec")
                        }
                        "standalone_rest", "block_rest" -> {
                            name = ((block.fields["label"] ?: block.fields["title"]) as? CanonicalJsonValue.Str)?.value
                            if (family == "legacy_timed_v1") plannedSeconds = block.historicalSeconds("durationSec")
                        }
                        "between_round_rest" -> {
                            if (family == "legacy_timed_v1") plannedSeconds = block.historicalSeconds("restBetweenRoundsSec")
                        }
                        else -> {
                            val itemId = payload.fields.getValue("itemId")
                            val item = (block.fields.getValue("items") as CanonicalJsonValue.Arr).values
                                .map { it as CanonicalJsonValue.Obj }
                                .first { it.fields.getValue("id") == itemId }
                            name = if (variant == "boundary_item_rest" || variant == "circuit_item_rest") {
                                (item.fields["labelOverride"] as? CanonicalJsonValue.Str)?.value
                            } else {
                                (item.fields["labelOverride"] as? CanonicalJsonValue.Str)?.value
                                    ?: frozenExerciseName((item.fields["exerciseId"] as? CanonicalJsonValue.Str)?.value, entries)
                            }
                            if (family == "legacy_timed_v1") {
                                plannedSeconds = item.historicalSeconds(
                                    if (variant == "boundary_rest_after_item" || variant == "circuit_rest_after_item")
                                        "restAfterSec" else "workDurationSec"
                                )
                            }
                        }
                    }
                }
            }
        }
        if (plannedSeconds != null && plannedSeconds > Long.MAX_VALUE / 1000L) {
            return WorkoutSessionHistoricalResult.InvalidPlannedDuration(session.id, family, phase.sequence, plannedSeconds)
        }
        val plannedMs = plannedSeconds?.times(1000L)
        val display = when (name) {
            null -> HistoricalDisplayV1(displayLocale, "unresolved_missing_metadata", null)
            "" -> HistoricalDisplayV1(displayLocale, "unresolved_invalid_metadata", null)
            else -> HistoricalDisplayV1(displayLocale, "resolved", name)
        }
        displays.add(HistoricalPhaseDisplay(phase.sequence, identity, plannedMs, display))
        if (family == "legacy_timed_v1" || family == "timed_composition_v2") {
            timedFamilies.getOrPut(family) { mutableListOf() }.add(
                TimedFocusPhaseV1(session.id, phase.sequence.toLong(), phase.phaseKind, plannedMs, phase.phaseIdentityJson)
            )
        }
    }
    val structures = mutableListOf<TimedResolvedStructureV1>()
    for ((family, phases) in timedFamilies) {
        when (val result = resolveTimedStructureV1(session.id, family, session.planSnapshotJson, phases)) {
            is TimedStructureResolutionV1.Resolved -> structures.add(result.structure)
            else -> return WorkoutSessionHistoricalResult.InvalidTimedStructure(session.id, family, result)
        }
    }
    return WorkoutSessionHistoricalResult.Resolved(input, title, mode, displayLocale, displays, structures)
}

private fun CanonicalJsonValue.Obj.historicalSeconds(key: String): Long? =
    (fields[key] as? CanonicalJsonValue.Num)?.value?.longValueExact()

private fun frozenExerciseName(exerciseId: String?, entries: List<CanonicalJsonValue.Obj>): String? {
    if (exerciseId == null) return null
    val entry = entries.firstOrNull {
        (it.fields.getValue("entityKind") as CanonicalJsonValue.Str).value == "exercise" &&
            (it.fields.getValue("stableId") as CanonicalJsonValue.Str).value == exerciseId
    } ?: return null
    return (entry.fields.getValue("customNameAtFirstReference") as? CanonicalJsonValue.Str)?.value
        ?: (entry.fields.getValue("displayNameAtFirstReference") as CanonicalJsonValue.Str).value
}
