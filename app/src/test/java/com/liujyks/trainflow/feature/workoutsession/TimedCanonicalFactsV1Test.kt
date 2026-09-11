package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1ValidationResult
import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1Validator
import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1
import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1Result
import com.liujyks.trainflow.core.data.toStorageJson
import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.CanonicalValidationResult
import com.liujyks.trainflow.core.database.PhaseIdentityV1Validator
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.core.database.renderCanonicalJson
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.RestBlock
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionStageGroup
import com.liujyks.trainflow.core.model.TimedCompositionTarget
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedCompositionTimelineAdapter
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedCanonicalFactsV1Test {
    @Test
    fun predicateBranchesUsePositiveDurationAndExcludeNonTargets() {
        val legacy = legacyFixture()
        val composition = compositionFixture()
        // Each row isolates one branch with exactly one positive opposite category.
        val rows = listOf(
            Triple(legacy, "A-work", "A-rest"),
            Triple(legacy, "A-custom", "A-rest"),
            Triple(legacy, "A-item-rest", "A-work"),
            Triple(legacy, "A-rest", "A-work"),
            Triple(legacy, "A-between", "A-work"),
            Triple(composition, "A-work", "A-rest"),
            Triple(composition, "A-custom", "A-rest"),
            Triple(composition, "A-rest", "A-work"),
            Triple(composition, "A-between", "A-work")
        )
        val workExpectations = listOf(
            listOf(false, false, false, true, false),
            listOf(true, false, true, true, true)
        )
        val restExpectations = listOf(
            listOf(false, false, true, false, false),
            listOf(false, true, true, true, true)
        )
        val expectations = listOf(
            workExpectations, workExpectations, restExpectations, restExpectations, restExpectations,
            workExpectations, workExpectations, restExpectations, restExpectations
        )
        rows.forEachIndexed { row, (fixture, key, opposite) ->
            listOf(0L, 1000L).forEachIndexed { durationIndex, duration ->
                val structure = resolve(fixture, listOf(
                    fixture.phases.getValue(key).copy(plannedDurationMs = duration),
                    fixture.phases.getValue(opposite)
                ))
                val phase = structure.phases.first()
                assertEquals("$key/$duration", expectations[row][durationIndex], listOf(
                    phase.trueWork, phase.trueRest, structure.hasTrueWork,
                    structure.hasTrueRest, structure.focusEligible
                ))
            }
        }
        val exclusions = listOf(
            legacy to "warmup", legacy to "stretch", legacy to "cooldown",
            legacy to "boundary-work", legacy to "boundary-custom", legacy to "boundary-rest",
            legacy to "boundary-after", legacy to "standalone", legacy to "paused",
            composition to "warmup", composition to "cooldown", composition to "paused"
        )
        exclusions.forEach { (fixture, key) ->
            val structure = resolve(fixture, listOf(fixture.phases.getValue(key)))
            val phase = structure.phases.single()
            assertEquals(key, listOf(false, false, false, false, false), listOf(
                phase.trueWork, phase.trueRest, structure.hasTrueWork,
                structure.hasTrueRest, structure.focusEligible
            ))
        }
    }

    @Test
    fun structureResolutionPreservesExistingFailures() {
        val fixture = compositionFixture()
        val phases = listOf(fixture.phases.getValue("A-work"), fixture.phases.getValue("A-rest"))
        listOf(
            "{" to PlanSnapshotStorageV1ValidationResult.Invalid(),
            fixture.json.replace("\"planSnapshotStorageContractVersion\":1", "\"planSnapshotStorageContractVersion\":2") to
                PlanSnapshotStorageV1ValidationResult.UnsupportedVersion("2"),
            fixture.json.replaceFirst("\"compositionVersion\":2", "\"compositionVersion\":3") to
                PlanSnapshotStorageV1ValidationResult.Invalid()
        ).forEach { (json, expected) ->
            assertEquals(TimedStructureResolutionV1.InvalidSnapshot(expected),
                resolveTimedStructureV1(fixture.session, fixture.family, json, phases))
        }
        val action = phases.first()
        val context = requireNotNull(PhaseIdentityV1Validator.prepareContext(fixture.prepared))
        listOf(
            "{" to CanonicalValidationResult.Invalid("invalid_phase_identity_v1"),
            action.phaseIdentityJson.replace("\"payloadVersion\":2", "\"payloadVersion\":3") to
                CanonicalValidationResult.Invalid("invalid_phase_identity_v1"),
            action.phaseIdentityJson.replace(fixture.digest, "0".repeat(64)) to
                CanonicalValidationResult.Invalid("invalid_phase_identity_v1"),
            action.phaseIdentityJson.replace("\"targetId\":\"action\"", "\"targetId\":\"other\"") to
                CanonicalValidationResult.Invalid("invalid_phase_identity_v1")
        ).forEach { (json, expected) ->
            val result = resolveTimedStructureV1(fixture.session, fixture.family, fixture.json,
                listOf(action.copy(phaseIdentityJson = json), phases.last()))
            assertEquals(TimedStructureResolutionV1.InvalidPhase(expected), result)
            assertEquals(TimedStructureResolutionV1.InvalidPhase(
                PhaseIdentityV1Validator.validatePrepared(json, context, action.phaseKind)), result)
        }
        assertEquals(TimedStructureResolutionV1.InvalidInput("unsupported_family"),
            resolveTimedStructureV1(fixture.session, "strength_v1", fixture.json, phases))
        assertEquals(TimedStructureResolutionV1.InvalidInput("session_mismatch"),
            resolveTimedStructureV1(fixture.session, fixture.family, fixture.json,
                listOf(action.copy(sessionId = "other-session"), phases.last())))
        assertEquals(TimedStructureResolutionV1.InvalidInput("family_mismatch"),
            resolveTimedStructureV1(fixture.session, "legacy_timed_v1", fixture.json, phases))
    }

    @Test
    fun focusIdentityUsesBlockLocalRounds() {
        listOf(legacyFixture(), compositionFixture()).forEach { fixture ->
            val structure = resolve(fixture, focusPhases(fixture))
            focusTuples(fixture).forEach { expected ->
                val result = validateTimedFocusV1(structure, focusJson(expected))
                assertTrue(result is TimedFocusValidationV1.Valid)
                assertIdentity(expected, (result as TimedFocusValidationV1.Valid).identity)
            }
        }
    }

    @Test
    fun focusValidationAndRestoreRejectInvalidIdentity() {
        val legacy = legacyFixture()
        val legacyStructure = resolve(legacy, focusPhases(legacy))
        val whole = focusTuples(legacy).first()
        val wholeFields = (parseCanonicalJson(focusJson(whole)) as CanonicalJsonValue.Obj).fields
        wholeFields.keys.forEach { key ->
            val fields = LinkedHashMap(wholeFields)
            fields.remove(key)
            assertInvalidRestore(legacyStructure, CanonicalJsonValue.Obj(fields).renderCanonicalJson(), whole)
        }
        listOf(
            mutate(focusJson(whole), "extra", "null"),
            mutate(focusJson(whole), "focusContractVersion", "2"),
            mutate(focusJson(whole), "focusContractVersion", "\"1\""),
            mutate(focusJson(whole), "focusKind", "\"other\""),
            mutate(focusJson(whole), "legacyBlockId", "\"L-A\"")
        ).forEach { assertInvalidRestore(legacyStructure, it, whole) }

        listOf(legacy, compositionFixture()).forEach { fixture ->
            val phases = focusPhases(fixture)
            val structure = resolve(fixture, phases)
            val tuples = focusTuples(fixture)
            val currentWhole = tuples.first()
            tuples.forEach { expected ->
                assertIdentity(expected, restoreTimedFocusV1(structure, focusJson(expected)))
            }
            val work = tuples[3]
            val rest = tuples[4]
            val blockKey = if (fixture.family == "legacy_timed_v1") "legacyBlockId" else "compositionBlockId"
            val otherBlockKey = if (fixture.family == "legacy_timed_v1") "compositionBlockId" else "legacyBlockId"
            val blockB = if (fixture.family == "legacy_timed_v1") "L-B" else "C-B"
            val mutations = listOf(
                "sessionId" to "\"other-session\"",
                "family" to "\"${if (fixture.family == "legacy_timed_v1") "timed_composition_v2" else "legacy_timed_v1"}\"",
                "structureDigestHexLowercase" to "\"${"0".repeat(64)}\"",
                blockKey to "\"other\"", blockKey to "\"$blockB\"",
                "roundIndex0" to "1", "roundIndex0" to "null",
                "phaseSequence" to "null", "phaseSequence" to "999", "phaseSequence" to "11",
                otherBlockKey to "\"other-family-block\""
            )
            mutations.forEach { (key, value) ->
                assertInvalidRestore(structure, mutate(focusJson(work), key, value), currentWhole)
            }
            assertInvalidRestore(structure, mutate(focusJson(tuples[1]), "roundIndex0", "1"), currentWhole)
            assertInvalidRestore(resolve(fixture, phases + phases.first()), focusJson(work), currentWhole)
            assertInvalidRestore(resolve(fixture, phases.map {
                if (it.phaseSequence == 10L) it.copy(plannedDurationMs = 0) else it
            }), focusJson(work), currentWhole)
            assertInvalidRestore(resolve(fixture, phases.map {
                if (it.phaseSequence == 11L) it.copy(plannedDurationMs = 0) else it
            }), focusJson(rest), currentWhole)
            val boundary = fixture.phases.getValue("warmup").copy(phaseSequence = 10)
            val boundaryBlock = if (fixture.family == "legacy_timed_v1") "warmup" else "C-A"
            val boundaryFocus = work.copy(
                legacyBlockId = if (fixture.family == "legacy_timed_v1") boundaryBlock else null,
                compositionBlockId = if (fixture.family == "timed_composition_v2") boundaryBlock else null,
                roundIndex0 = null
            )
            assertInvalidRestore(resolve(fixture, listOf(boundary)), focusJson(boundaryFocus), currentWhole)
        }
        val legacyWorkJson = focusJson(focusTuples(legacy)[3])
        listOf("-1", "0.5").forEach {
            assertInvalidRestore(legacyStructure, mutate(legacyWorkJson, "roundIndex0", it), whole)
        }
    }

    @Test
    fun deliveredFactsFeedSharedRulesWithoutReclassification() {
        val legacySnapshot = snapshot(listOf(TimedCircuitBlock(
            id = "legacy-circuit", order = 1, rounds = 1,
            items = listOf(TimedExerciseItem(
                id = "legacy-work", labelOverride = "Legacy work", stageType = TimedStageType.WORK,
                workDurationSec = 5, restAfterSec = 4
            ))
        )), "legacy-timed-plan", "Legacy timed plan")
        val prefix = TimedCompositionBlock(
            id = "composition-prefix", order = 1, title = "Composition bridge", rounds = 1,
            stageGroups = listOf(group("group-p", 1, listOf(
                target("target-p", 1, TimedCompositionTargetKind.ACTION, 1)
            )))
        )
        val block = TimedCompositionBlock(
            id = "composition-bridge", order = 2, title = "Composition bridge",
            warmupSec = 2, cooldownSec = 2, rounds = 2, restBetweenRoundsSec = 3,
            stageGroups = listOf(
                group("group-a", 1, listOf(
                    target("a", 1, TimedCompositionTargetKind.ACTION, 4),
                    target("c", 2, TimedCompositionTargetKind.CUSTOM, 3),
                    target("r", 3, TimedCompositionTargetKind.REST, 2)
                )),
                group("group-b", 2, listOf(target("b", 1, TimedCompositionTargetKind.ACTION, 1)))
            )
        )
        val compositionSnapshot = snapshot(listOf(prefix, block),
            "timed-composition-bridge-plan", "Timed composition bridge plan")
        listOf(legacySnapshot, compositionSnapshot).forEachIndexed { index, frozen ->
            val session = if (index == 0) "a3-legacy-delivered" else "a3-composition-delivered"
            val prepared = prepare(frozen.toStorageJson())
            val startedAt = Instant.parse("2026-09-11T00:00:00Z")
            val initial = TimedWorkoutEngine.create(frozen, sessionId = session)
            val start = TimedWorkoutEngine.dispatch(initial, WorkoutCommand.StartSession)
            val tick = TimedWorkoutEngine.tick(start.state, if (index == 0) 5 else 10)
            val facts = if (index == 0) {
                legacyTimedTransitionFactsV1(prepared, initial, start, startedAt).phaseStarts +
                    legacyTimedTransitionFactsV1(prepared, start.state, tick, startedAt).phaseStarts
            } else {
                val timelines = frozen.blocks.filterIsInstance<TimedCompositionBlock>()
                    .map(TimedCompositionTimelineAdapter::expand)
                compositionTimedTransitionFactsV1(prepared, timelines, initial, start, startedAt).phaseStarts +
                    compositionTimedTransitionFactsV1(prepared, timelines, start.state, tick, startedAt).phaseStarts
            }
            val family = ((parseCanonicalJson(facts.first().phaseIdentityJson) as CanonicalJsonValue.Obj)
                .fields.getValue("family") as CanonicalJsonValue.Str).value
            val assignedSequences = if (index == 0) listOf(100L, 110L) else listOf(100L, 110L, 120L, 130L, 140L)
            val phases = facts.mapIndexed { position, fact ->
                TimedFocusPhaseV1(session, assignedSequences[position], fact.phaseKind,
                    fact.plannedDurationMs, fact.phaseIdentityJson)
            }
            val result = resolveTimedStructureV1(session, family, frozen.toStorageJson(), phases)
            assertTrue(result is TimedStructureResolutionV1.Resolved)
            val structure = (result as TimedStructureResolutionV1.Resolved).structure
            assertEquals(listOf(true, true, true),
                listOf(structure.hasTrueWork, structure.hasTrueRest, structure.focusEligible))
            val expectedFamily = if (index == 0) "legacy_timed_v1" else "timed_composition_v2"
            val expectedSequences = if (index == 0) listOf(100L, 110L) else listOf(120L, 140L)
            listOf("work", "rest").forEachIndexed { kindIndex, kind ->
                val expected = TimedFocusIdentityV1(1, session, expectedFamily,
                    prepared.orderedStructureDigestHexLowercase(), kind,
                    if (index == 0) "legacy-circuit" else null,
                    if (index == 1) "composition-bridge" else null, 0, expectedSequences[kindIndex])
                val focus = validateTimedFocusV1(structure, focusJson(expected))
                assertTrue(focus is TimedFocusValidationV1.Valid)
                assertIdentity(expected, (focus as TimedFocusValidationV1.Valid).identity)
            }
        }
    }

    private data class Fixture(
        val session: String,
        val family: String,
        val json: String,
        val prepared: PreparedPlanSnapshotStorageV1,
        val phases: Map<String, TimedFocusPhaseV1>
    ) {
        val digest: String get() = prepared.orderedStructureDigestHexLowercase()
    }

    private fun legacyFixture(): Fixture {
        val items = listOf(
            TimedExerciseItem(id = "work", stageType = TimedStageType.WORK, workDurationSec = 1, restAfterSec = 1),
            TimedExerciseItem(id = "custom", stageType = TimedStageType.CUSTOM, workDurationSec = 1),
            TimedExerciseItem(id = "rest", stageType = TimedStageType.REST, workDurationSec = 1)
        )
        val json = snapshot(listOf(
            TimedCircuitBlock("L-A", 0, 2, items, restBetweenRoundsSec = 1),
            TimedCircuitBlock("L-B", 1, 2, items, restBetweenRoundsSec = 1),
            WarmupBlock("warmup", 2, durationSec = 1),
            StretchBlock("stretch", 3, durationSec = 1),
            CooldownBlock("cooldown", 4, durationSec = 1),
            WarmupBlock("boundary", 5, items = items),
            RestBlock("standalone", 6, durationSec = 1)
        )).toStorageJson()
        val prepared = prepare(json)
        val phases = linkedMapOf<String, TimedFocusPhaseV1>()
        fun add(key: String, sequence: Long, kind: String, payload: String) {
            phases[key] = phase("legacy-session", "legacy_timed_v1", prepared, sequence, kind, payload)
        }
        listOf("A", "B").forEachIndexed { index, name ->
            val block = "L-$name"
            val sequence = if (index == 0) 10L else 20L
            add("$name-work", sequence, "timed_work", legacyPayload("circuit_item_work", block, 0, "timed_circuit", "work", "work", 0))
            add("$name-rest", sequence + 1, "timed_rest", legacyPayload("circuit_rest_after_item", block, 1, "timed_circuit", "rest", "work", 0))
            add("$name-custom", sequence + 2, "timed_work", legacyPayload("circuit_item_work", block, 2, "timed_circuit", "custom", "custom", 0))
            add("$name-item-rest", sequence + 3, "timed_rest", legacyPayload("circuit_item_rest", block, 3, "timed_circuit", "rest", "rest", 0))
            add("$name-between", sequence + 4, "timed_rest", legacyPayload("between_round_rest", block, 4, "timed_circuit", "rest", null, 0))
        }
        add("warmup", 30, "timed_work", legacyPayload("boundary_block_work", "warmup", 0, "warmup", "warmup", null, null))
        add("stretch", 31, "timed_work", legacyPayload("boundary_block_work", "stretch", 0, "stretch", "cooldown", null, null))
        add("cooldown", 32, "timed_work", legacyPayload("boundary_block_work", "cooldown", 0, "cooldown", "cooldown", null, null))
        add("boundary-work", 33, "timed_work", legacyPayload("boundary_item_work", "boundary", 0, "warmup", "work", "work", null))
        add("boundary-after", 34, "timed_rest", legacyPayload("boundary_rest_after_item", "boundary", 1, "warmup", "rest", "work", null))
        add("boundary-custom", 35, "timed_work", legacyPayload("boundary_item_work", "boundary", 2, "warmup", "custom", "custom", null))
        add("boundary-rest", 36, "timed_rest", legacyPayload("boundary_item_rest", "boundary", 3, "warmup", "rest", "rest", null))
        add("standalone", 37, "timed_rest", legacyPayload("standalone_rest", "standalone", 0, "rest", "rest", null, null))
        add("paused", 38, "paused", legacyPayload("paused", null, null, null, null, null, null))
        return Fixture("legacy-session", "legacy_timed_v1", json, prepared, phases)
    }

    private fun compositionFixture(): Fixture {
        val blocks = listOf("C-A", "C-B").mapIndexed { order, id ->
            TimedCompositionBlock(id = id, order = order, warmupSec = 1, cooldownSec = 1,
                rounds = 2, restBetweenRoundsSec = 1, stageGroups = listOf(group("group", 0, listOf(
                    target("action", 0, TimedCompositionTargetKind.ACTION, 1),
                    target("custom", 1, TimedCompositionTargetKind.CUSTOM, 1),
                    target("rest", 2, TimedCompositionTargetKind.REST, 1)
                ))))
        }
        val json = snapshot(blocks).toStorageJson()
        val prepared = prepare(json)
        val phases = linkedMapOf<String, TimedFocusPhaseV1>()
        fun add(key: String, sequence: Long, kind: String, payload: String) {
            phases[key] = phase("composition-session", "timed_composition_v2", prepared, sequence, kind, payload)
        }
        listOf("A", "B").forEachIndexed { index, name ->
            val block = "C-$name"
            val sequence = if (index == 0) 10L else 20L
            add("$name-work", sequence, "timed_work", compositionPayload("stage_group_action", block, "$block:r1:g1:group", "stage_group", "group", "action", "action", 0, 0, 0, 1, 1, 1))
            add("$name-rest", sequence + 1, "timed_rest", compositionPayload("stage_group_rest", block, "$block:r1:g1:group", "stage_group", "group", "rest", "rest", 0, 0, 2, 1, 3, 3))
            add("$name-custom", sequence + 2, "timed_work", compositionPayload("stage_group_custom", block, "$block:r1:g1:group", "stage_group", "group", "custom", "custom", 0, 0, 1, 1, 2, 2))
            add("$name-between", sequence + 3, "timed_rest", compositionPayload("between_round_rest", block, "$block:r1:between-round-rest", "between_round_rest", "$block:r1:between-round-rest", "$block:r1:between-round-rest:target", "between_round_rest", 0, null, 0, 2, 4, 4))
        }
        add("warmup", 30, "timed_work", compositionPayload("warmup", "C-A", "C-A:warmup", "warmup", "C-A:warmup", "C-A:warmup:target", "warmup", null, null, 0, 0, 0, 0))
        add("cooldown", 31, "timed_work", compositionPayload("cooldown", "C-A", "C-A:cooldown", "cooldown", "C-A:cooldown", "C-A:cooldown:target", "cooldown", null, null, 0, 4, 8, 8))
        add("paused", 32, "paused", compositionPayload("paused", null, null, null, null, null, null, null, null, null, null, null, null))
        return Fixture("composition-session", "timed_composition_v2", json, prepared, phases)
    }

    private fun snapshot(blocks: List<PlanBlock>, planId: String? = null, title: String = "Timed") =
        WorkoutPlanSnapshot(planId = planId, title = title, mode = WorkoutMode.TIMED, blocks = blocks)

    private fun prepare(json: String) =
        (PlanSnapshotStorageV1Validator.prepare(json, WorkoutMode.TIMED) as PreparedPlanSnapshotStorageV1Result.Valid).prepared

    private fun group(id: String, order: Int, targets: List<TimedCompositionTarget>) =
        TimedCompositionStageGroup(id, order, "Main group", TimedStageType.WORK.defaultColorHex, targets = targets)

    private fun target(id: String, order: Int, kind: TimedCompositionTargetKind, seconds: Int): TimedCompositionTarget {
        val (name, color) = when (kind) {
            TimedCompositionTargetKind.ACTION -> "Jumping jacks" to TimedStageType.WORK.defaultColorHex
            TimedCompositionTargetKind.CUSTOM -> "Shadow boxing" to TimedStageType.CUSTOM.defaultColorHex
            TimedCompositionTargetKind.REST -> "Breathe" to TimedStageType.REST.defaultColorHex
        }
        return TimedCompositionTarget(id, order, name, kind, seconds, color)
    }

    private fun phase(session: String, family: String, prepared: PreparedPlanSnapshotStorageV1,
        sequence: Long, kind: String, payload: String): TimedFocusPhaseV1 {
        val version = if (family == "legacy_timed_v1") 1 else 2
        val identity = """{"phaseIdentityContractVersion":1,"family":"$family","payloadVersion":$version,"mode":"timed","phaseKind":"$kind","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"${prepared.orderedStructureDigestHexLowercase()}"},"payload":$payload}"""
        return TimedFocusPhaseV1(session, sequence, kind, if (kind == "paused") null else 1000L, identity)
    }

    private fun legacyPayload(variant: String, block: String?, step: Int?, blockKind: String?,
        stageType: String?, item: String?, round: Int?): String =
        """{"variant":"$variant","blockId":${stringJson(block)},"stepIndex0":$step,"legacyBlockKind":${stringJson(blockKind)},"legacyStageType":${stringJson(stageType)},"itemId":${stringJson(item)},"exerciseId":null,"roundIndex0":$round}"""

    private fun compositionPayload(variant: String, block: String?, stage: String?, stageKind: String?,
        group: String?, target: String?, targetKind: String?, round: Int?, groupIndex: Int?,
        targetIndex: Int?, stageInstance: Int?, targetInstance: Int?, step: Int?): String =
        """{"variant":"$variant","compositionVersion":2,"compositionBlockId":${stringJson(block)},"timelineStageId":${stringJson(stage)},"timelineStageKind":${stringJson(stageKind)},"stageGroupId":${stringJson(group)},"targetId":${stringJson(target)},"targetKind":${stringJson(targetKind)},"roundIndex0":$round,"stageGroupIndex0":$groupIndex,"targetIndex0":$targetIndex,"stageInstanceIndex0":$stageInstance,"targetInstanceIndex0":$targetInstance,"stepIndex0":$step}"""

    private fun stringJson(value: String?): String = value?.let { CanonicalJsonValue.Str(it).renderCanonicalJson() } ?: "null"

    private fun resolve(fixture: Fixture, phases: List<TimedFocusPhaseV1>): TimedResolvedStructureV1 {
        val result = resolveTimedStructureV1(fixture.session, fixture.family, fixture.json, phases)
        assertTrue("$result", result is TimedStructureResolutionV1.Resolved)
        return (result as TimedStructureResolutionV1.Resolved).structure
    }

    private fun focusPhases(fixture: Fixture) = listOf("A-work", "A-rest", "B-work", "B-rest")
        .map(fixture.phases::getValue)

    private fun focusTuples(fixture: Fixture): List<TimedFocusIdentityV1> {
        val legacy = fixture.family == "legacy_timed_v1"
        val a = if (legacy) "L-A" else "C-A"
        val b = if (legacy) "L-B" else "C-B"
        return listOf(
            TimedFocusIdentityV1(1, fixture.session, fixture.family, fixture.digest, "whole", null, null, null, null),
            TimedFocusIdentityV1(1, fixture.session, fixture.family, fixture.digest, "round", if (legacy) a else null, if (legacy) null else a, 0, null),
            TimedFocusIdentityV1(1, fixture.session, fixture.family, fixture.digest, "round", if (legacy) b else null, if (legacy) null else b, 0, null),
            TimedFocusIdentityV1(1, fixture.session, fixture.family, fixture.digest, "work", if (legacy) a else null, if (legacy) null else a, 0, 10),
            TimedFocusIdentityV1(1, fixture.session, fixture.family, fixture.digest, "rest", if (legacy) a else null, if (legacy) null else a, 0, 11),
            TimedFocusIdentityV1(1, fixture.session, fixture.family, fixture.digest, "work", if (legacy) b else null, if (legacy) null else b, 0, 20),
            TimedFocusIdentityV1(1, fixture.session, fixture.family, fixture.digest, "rest", if (legacy) b else null, if (legacy) null else b, 0, 21)
        )
    }

    private fun focusJson(identity: TimedFocusIdentityV1): String = with(identity) {
        """{"focusContractVersion":$focusContractVersion,"sessionId":${stringJson(sessionId)},"family":${stringJson(family)},"structureDigestHexLowercase":${stringJson(structureDigestHexLowercase)},"focusKind":${stringJson(focusKind)},"legacyBlockId":${stringJson(legacyBlockId)},"compositionBlockId":${stringJson(compositionBlockId)},"roundIndex0":$roundIndex0,"phaseSequence":$phaseSequence}"""
    }

    private fun mutate(json: String, key: String, value: String): String {
        val fields = (parseCanonicalJson(json) as CanonicalJsonValue.Obj).fields
        fields[key] = requireNotNull(parseCanonicalJson(value))
        return CanonicalJsonValue.Obj(fields).renderCanonicalJson()
    }

    private fun assertIdentity(expected: TimedFocusIdentityV1, actual: TimedFocusIdentityV1) {
        assertEquals(expected.focusContractVersion, actual.focusContractVersion)
        assertEquals(expected.sessionId, actual.sessionId)
        assertEquals(expected.family, actual.family)
        assertEquals(expected.structureDigestHexLowercase, actual.structureDigestHexLowercase)
        assertEquals(expected.focusKind, actual.focusKind)
        assertEquals(expected.legacyBlockId, actual.legacyBlockId)
        assertEquals(expected.compositionBlockId, actual.compositionBlockId)
        assertEquals(expected.roundIndex0, actual.roundIndex0)
        assertEquals(expected.phaseSequence, actual.phaseSequence)
    }

    private fun assertInvalidRestore(structure: TimedResolvedStructureV1, json: String, whole: TimedFocusIdentityV1) {
        assertEquals(TimedFocusValidationV1.Invalid, validateTimedFocusV1(structure, json))
        assertIdentity(whole, restoreTimedFocusV1(structure, json))
    }
}
