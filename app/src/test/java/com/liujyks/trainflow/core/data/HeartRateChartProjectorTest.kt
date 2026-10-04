package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.database.CanonicalAnalysisV1
import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1
import com.liujyks.trainflow.core.database.entity.*
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.core.model.*
import com.liujyks.trainflow.feature.workoutsession.TimedStructureResolutionV1
import org.junit.Assert.*
import org.junit.Test

class HeartRateChartProjectorTest {
    @Test
    fun anchorsAndCanonicalTimelinePreserveOriginalFacts() {
        val source = anchorsFixture()
        val graph = (source.source as WorkoutSessionStrictReadResult.CanonicalTerminal).graph
        val snapshot = graph.snapshots.single()
        val p = project(source, selected = graph.samples[2].chartTuple())
        assertEquals(HeartRateChartDomain(0, 120000), p.domain)
        assertEquals(5, p.phaseBands.size)
        assertEquals(45000L, p.phaseBands[2].phase.startOffsetMs)
        assertEquals(45000L, p.phaseBands[2].phase.endOffsetMs)
        assertEquals((0..8).toList(), p.displayOrdinals)
        assertEquals((0..8).toList(), p.mandatoryRaw.map { it.ordinal })
        assertTrue(p.mandatoryRaw.single { it.ordinal == 1 }.semantics.containsAll(setOf(
            HeartRateAnchorSemantic.MAXIMUM, HeartRateAnchorSemantic.ORIGINAL_HIGHEST)))
        assertTrue(p.mandatoryRaw.single { it.ordinal == 2 }.semantics.contains(HeartRateAnchorSemantic.SELECTED))
        assertSame(graph.samples[1], p.originalHighest)
        assertSame(graph.samples[2], p.selectedRaw)
        assertTrue(p.selectedInView)
        assertTrue(p.visibleHighestMarker)
        assertEquals(200, p.raw[4].bpm)
        assertEquals(118, snapshot.observedAvgBpm)
        assertEquals(130, snapshot.observedMaxBpm)
        assertEquals(1L, snapshot.highestSampleSequence)
        assertEquals(118, p.originalAverageBpm)
        assertAxis(p, 97, 209)
        assertSame(source, p.source)
        assertSame(graph.session, p.session)
        assertSame(graph.recording, p.recording)
        assertSame(graph.samples, p.raw)
        assertSame(graph.phases, p.phases)
        assertSame(graph.acquisitions, p.acquisitions)
        assertSame(snapshot, p.snapshot)
        assertEquals(1, p.originalAnalysisVersion)
        assertEquals(graph.session.lastMutationSequence, p.inputLastMutationSequence)
        assertTrue(HeartRateChartProjector.scrub(p, 20000) is HeartRateRawScrubResult.NotRecorded)
        assertEquals(listOf(0, 1, 2, 3, 4), p.phaseBands.map { it.phase.sequence })
    }

    @Test
    fun visualGapUsesExplicitFactsAndIndependentCadenceRule() {
        listOf(2500L to null, 2501L to HeartRateGapStyle.DASHED,
            20000L to HeartRateGapStyle.DASHED, 20001L to HeartRateGapStyle.BROKEN).forEach { (delta, style) ->
            val source = fixture(delta + 1000, listOf(phase(0, 0, delta + 1000, "active_set")), points(0L to 100, delta to 100))
            val p = project(source)
            assertEquals(style, p.visualGaps.singleOrNull()?.style)
            assertEquals(listOf(0, 1), p.mandatoryRaw.map { it.ordinal })
            assertSame((source.source as WorkoutSessionStrictReadResult.CanonicalTerminal).graph.snapshots.single(), p.snapshot)
            if (style == null) assertTrue(HeartRateChartProjector.scrub(p, delta / 2) is HeartRateRawScrubResult.Recorded)
            else assertTrue(HeartRateChartProjector.scrub(p, delta / 2) is HeartRateRawScrubResult.NotRecorded)
            assertTrue(HeartRateChartProjector.scrub(p, 0) is HeartRateRawScrubResult.Recorded)
            assertTrue(HeartRateChartProjector.scrub(p, delta) is HeartRateRawScrubResult.Recorded)
        }
        listOf(1L to null, 24992L to HeartRateGapStyle.BROKEN).forEach { (delta, style) ->
            val acquisition = acquisition(0, 0, delta + 1000, "technical_failure", "platform_failure")
            val source = fixture(delta + 1000, listOf(phase(0, 0, delta + 1000, "active_set")),
                points(0L to 100, delta to 100), acquisitions = listOf(acquisition))
            val p = project(source)
            assertEquals(style, p.visualGaps.singleOrNull()?.style)
            assertSame((source.source as WorkoutSessionStrictReadResult.CanonicalTerminal).graph.acquisitions.single(), p.acquisitions.single())
            assertEquals("platform_failure", p.acquisitions.single().deviceReason)
        }
        val cross = project(fixture(3000, listOf(phase(0, 0, 1000, "active_set"), phase(1, 1000, 3000, "active_set")),
            points(500L to 100, 1500L to 100)))
        assertEquals(HeartRateGapStyle.BROKEN, cross.visualGaps.single().style)
        val stale = listOf(acquisition(0, 0, 500), acquisition(1, 500, 1500, "stale", "sample_stale_timeout"),
            acquisition(2, 1500, 3000))
        val p = project(fixture(3000, listOf(phase(0, 0, 3000, "active_set")), points(0L to 100, 2000L to 100), acquisitions = stale))
        assertEquals(HeartRateGapStyle.DASHED, p.visualGaps.single().style)
        assertEquals(listOf(1), p.visualGaps.single().emptyAcquisitionSequences)
        assertEquals("sample_stale_timeout", p.acquisitions[1].deviceReason)
        assertTrue(HeartRateChartProjector.scrub(p, 1000) is HeartRateRawScrubResult.NotRecorded)
        val zero = listOf(acquisition(0, 0, 1000), acquisition(1, 1000, 1000, "stale", "sample_stale_timeout"),
            acquisition(2, 1000, 3000))
        val zeroSource = fixture(3000, listOf(phase(0, 0, 3000, "active_set")),
            points(0L to 100, 1000L to 100, 2000L to 100), acquisitions = zero)
        val noGap = project(zeroSource)
        assertTrue(noGap.visualGaps.isEmpty())
        assertSame((zeroSource.source as WorkoutSessionStrictReadResult.CanonicalTerminal).graph.acquisitions[1], noGap.acquisitions[1])
        assertEquals(1, noGap.solidSegments.size)
    }

    @Test
    fun rawScrubKeepsCanonicalBurstsAndMissingWindows() {
        val raw = mutableListOf(sample(0, 0, 100))
        repeat(32) { raw.add(sample(it + 1, 1000, 100 + it, it.toLong())) }
        raw.add(sample(33, 2000, 100)); raw.add(sample(34, 7000, 100))
        val p = project(fixture(8000, listOf(phase(0, 0, 8000, "active_set")), raw))
        for (offset in listOf(1000L, 1500L)) {
            val result = HeartRateChartProjector.scrub(p, offset, raw[17].chartTuple()) as HeartRateRawScrubResult.Recorded
            assertEquals(1000L, result.offsetMs)
            assertEquals("0:01", result.formattedTime)
            assertEquals((1L..32L).toList(), result.points.map { it.raw.sampleSequence })
            assertSame(raw[17], result.preferredRaw)
            assertTrue(result.points.all { it.phase === p.phases.single() && it.acquisition === p.acquisitions.single() })
        }
        assertTrue(HeartRateChartProjector.scrub(p, 4000) is HeartRateRawScrubResult.NotRecorded)
        assertSame(raw[34], (HeartRateChartProjector.scrub(p, 7000) as HeartRateRawScrubResult.Recorded).points.single().raw)
        val budget = project(budgetFixture())
        for (i in listOf(4, 5)) {
            assertFalse(i in budget.displayOrdinals)
            assertSame(budget.raw[i], (HeartRateChartProjector.scrub(budget, budget.raw[i].offsetMs) as HeartRateRawScrubResult.Recorded).points.single().raw)
        }
        val late = project(anchorsFixture())
        assertTrue(HeartRateChartProjector.scrub(late, 20000) is HeartRateRawScrubResult.NotRecorded)
        val empty = project(budgetFixture(), HeartRateChartView.Phase(SESSION, "strength_v1", SIGNATURE, 2))
        assertFalse(empty.hasRawInView)
        assertTrue(HeartRateChartProjector.scrub(empty, 5750) is HeartRateRawScrubResult.NotRecorded)
    }

    @Test
    fun viewsConsumeExistingIdentitiesAndOriginalAnalysis() {
        for (legacy in listOf(true, false)) {
            val source = timedFixture(legacy)
            val structure = source.timedStructures.single()
            assertTrue(structure.hasTrueWork && structure.hasTrueRest && structure.focusEligible)
            assertEquals(listOf(true, false, true, false, false), structure.phases.map { it.trueWork })
            assertEquals(listOf(false, true, false, true, false), structure.phases.map { it.trueRest })
            for ((kind, sequence, expected) in listOf(Triple("round", null, listOf(2, 3)),
                    Triple("work", 2, listOf(2)), Triple("rest", 3, listOf(3)))) {
                val view = HeartRateChartView.Timed(structure.family, focusJson(source, legacy, kind, sequence))
                val p = project(source, view)
                assertEquals(expected, p.solidSegments.map { it.phaseSequence })
                assertEquals(view, p.effectiveView)
                assertFalse(p.restoredToWhole)
                assertEquals(135, p.originalAverageBpm)
                assertEquals(170, p.snapshot.observedMaxBpm)
                assertEquals(3500L, p.originalHighest!!.offsetMs)
                if (kind == "work") { assertFalse(p.visibleHighestMarker); assertAxis(p, 128, 169) }
                if (kind == "round") { assertTrue(p.visibleHighestMarker); assertAxis(p, 133, 174) }
            }
            val invalid = focusJson(source, legacy, "work", 2).replace("\"sessionId\":\"$SESSION\"", "\"sessionId\":\"wrong\"")
            val restored = project(source, HeartRateChartView.Timed(structure.family, invalid))
            assertEquals(HeartRateChartView.Whole, restored.effectiveView)
            assertTrue(restored.restoredToWhole)
        }
        val source = anchorsFixture()
        val valid = HeartRateChartView.Phase(SESSION, "strength_v1", SIGNATURE, 4)
        val p = project(source, valid)
        assertEquals(HeartRateChartDomain(60000, 120000), p.domain)
        assertEquals(listOf(7, 8), p.displayOrdinals)
        assertEquals(118, p.originalAverageBpm)
        assertEquals(valid, p.effectiveView)
        val payload = p.phaseBands[4].identity.fields.getValue("payload") as CanonicalJsonValue.Obj
        assertEquals(CanonicalJsonValue.Str("block"), payload.fields.getValue("blockId"))
        assertEquals(CanonicalJsonValue.Str("set"), payload.fields.getValue("setPlanId"))
        for (invalid in listOf(valid.copy(sessionId = "wrong"), valid.copy(family = "wrong"),
                valid.copy(structureDigestHexLowercase = "wrong"), valid.copy(phaseSequence = 99))) {
            val restored = project(source, invalid)
            assertTrue(restored.restoredToWhole)
            assertEquals(HeartRateChartView.Whole, restored.effectiveView)
        }
    }

    @Test
    fun axisAndTimeFormatMatchApprovedGoldens() {
        for ((min, max, lower, upper, ticks) in listOf(
                AxisGolden(80, 160, 78, 168, listOf(78, 101, 123, 146, 168)),
                AxisGolden(110, 110, 90, 130, listOf(90, 100, 110, 120, 130)),
                AxisGolden(104, 114, 89, 130, listOf(89, 99, 110, 120, 130)))) {
            val y = HeartRateChartProjector.computeYAxis(min, max, 200, 20)
            assertEquals(lower, y.lowerBpm); assertEquals(upper, y.upperBpm)
            assertEquals(ticks, y.ticks.map { it.bpm })
            y.ticks.forEach { assertEquals((it.bpm - lower).toDouble() / (upper - lower), it.position, 0.0) }
        }
        for ((height, ticks) in listOf(120 to listOf(78, 108, 138, 168), 80 to listOf(78, 123, 168), 40 to listOf(78, 168))) {
            val y = HeartRateChartProjector.computeYAxis(80, 160, height, 20)
            assertEquals(ticks, y.ticks.map { it.bpm })
            y.ticks.forEach { assertEquals((it.bpm - 78).toDouble() / 90, it.position, 0.0) }
        }
        for ((time, label) in listOf(0L to "0:00", 59999L to "0:59", 3599999L to "59:59", 3600000L to "1:00:00", 28800000L to "8:00:00"))
            assertEquals(label, HeartRateChartProjector.formatElapsed(time))
    }

    @Test
    fun mandatoryBudgetAndTimeWeightedBucketsMatchFixedOracle() {
        val p = project(budgetFixture())
        assertEquals(318, p.raw.size)
        assertEquals(312, p.mandatoryRawPointCount)
        assertEquals(listOf(0, 1, 2, 3, 6, 7, 8, 9, 10, 11, 13) + (14..317), p.displayOrdinals)
        assertEquals(listOf(3, 6, 7), p.displayOrdinals.filter { i -> p.mandatoryRaw.none { it.ordinal == i } })
        assertEquals(3, p.nonMandatoryPointCount)
        assertEquals(307, p.phaseBands.size)
        assertEquals(5500L, p.phaseBands[2].phase.startOffsetMs)
        assertEquals(6000L, p.phaseBands[2].phase.endOffsetMs)
        val phases = (0 until 1000).map { phase(it, it * 1000L, (it + 1) * 1000L, "active_set") }
        val samples = (0 until 1000).flatMap { i -> listOf(100, 110, 90, 105).mapIndexed { j, bpm -> sample(i * 4 + j, i * 1000L + j, bpm, if (j == 0) i.toLong() else 0) } }
        val all = project(fixture(1000000, phases, samples))
        assertEquals((0..3999).toList(), all.displayOrdinals)
        assertEquals(4000, all.mandatoryRawPointCount)
        assertEquals(0, all.nonMandatoryPointCount)
        assertTrue(all.mandatoryRaw.first().semantics.containsAll(setOf(HeartRateAnchorSemantic.FIRST, HeartRateAnchorSemantic.PHASE_BOUNDARY)))
    }

    @Test
    fun typedEmptyAndUnavailableResultsPreserveTheirSources() {
        val original = anchorsFixture()
        val strict = original.source as WorkoutSessionStrictReadResult.CanonicalTerminal
        val noRecordingStrict = strict.copy(graph = strict.graph.copy(recording = null, acquisitions = emptyList(), samples = emptyList(), snapshots = emptyList()))
        val noRecording = resolveWorkoutSessionHistorical(noRecordingStrict, "zh-CN")
        assertSame(noRecording, (HeartRateChartProjector.project(noRecording, REQUEST) as HeartRateChartResult.NoRecording).source)
        val legacyStrict = WorkoutSessionStrictReadResult.LegacyTerminal(strict.graph.session.copy(timelineVersion = null), strict.planRoot, strict.execution)
        val legacy = resolveWorkoutSessionHistorical(legacyStrict, "zh-CN")
        assertSame(legacy, (HeartRateChartProjector.project(legacy, REQUEST) as HeartRateChartResult.NoRecording).source)
        val zero = project(fixture(1000, listOf(phase(0, 0, 1000, "active_set")), emptyList()))
        assertTrue(zero.raw.isEmpty() && zero.displayPoints.isEmpty())
        assertNull(zero.yAxis)
        assertEquals("no_canonical_samples", zero.snapshot.sampleStatus)
        assertEquals(1, zero.phaseBands.size)
        val noZone = project(fixture(1000, listOf(phase(0, 0, 1000, "active_set")), points(0L to 100)))
        assertTrue(noZone.hasRawInView)
        assertNull(noZone.recording.zoneSnapshotJson)
        assertNull(noZone.snapshot.zoneDurationsJson)
        val local = project(budgetFixture(), HeartRateChartView.Phase(SESSION, "strength_v1", SIGNATURE, 2))
        assertFalse(local.hasRawInView)
        assertTrue(local.raw.isNotEmpty())
        assertNotEquals("no_canonical_samples", local.snapshot.sampleStatus)
        assertNotNull(local.yAxis)
        val errors = listOf(
            WorkoutSessionHistoricalResult.Forwarded(WorkoutSessionStrictReadResult.Nonterminal(strict.graph.session, "running", strict.graph, strict.execution)),
            WorkoutSessionHistoricalResult.Forwarded(WorkoutSessionStrictReadResult.NotFound),
            WorkoutSessionHistoricalResult.Forwarded(WorkoutSessionStrictReadResult.Unavailable("original_error")),
            WorkoutSessionHistoricalResult.InvalidPlannedDuration(SESSION, "legacy_timed_v1", 0, Long.MAX_VALUE),
            WorkoutSessionHistoricalResult.InvalidTimedStructure(SESSION, "legacy_timed_v1", TimedStructureResolutionV1.InvalidInput("original_error")))
        errors.forEach { assertSame(it, (HeartRateChartProjector.project(it, REQUEST) as HeartRateChartResult.Unavailable).source) }
    }

    private data class AxisGolden(val min: Int, val max: Int, val lower: Int, val upper: Int, val ticks: List<Int>)
    private fun assertAxis(p: HeartRateChartProjection, lower: Int, upper: Int) {
        assertEquals(lower, p.yAxis!!.lowerBpm); assertEquals(upper, p.yAxis.upperBpm)
    }
    private fun project(source: WorkoutSessionHistoricalResult, view: HeartRateChartView = HeartRateChartView.Whole,
        selected: HeartRateRawTuple? = null) = (HeartRateChartProjector.project(source, REQUEST.copy(view = view, selectedRawTuple = selected)) as HeartRateChartResult.Available).projection

    private fun anchorsFixture() = fixture(120000, listOf(phase(0, 0, 30000, "prepare_set"),
        phase(1, 30000, 45000, "active_set"), phase(2, 45000, 45000, "paused"),
        phase(3, 45000, 60000, "paused"), phase(4, 60000, 120000, "active_set")),
        points(31000L to 100, 32000L to 130, 33000L to 110, 34000L to 120,
            46000L to 200, 47000L to 190, 48000L to 200, 61000L to 100, 62000L to 130), 30000)

    private fun budgetFixture(): WorkoutSessionHistoricalResult.Resolved {
        val phases = mutableListOf(phase(0, 0, 3500, "active_set"), phase(1, 3500, 5500, "active_set"), phase(2, 5500, 6000, "paused"))
        val raw = mutableListOf<HeartRateSampleEntity>()
        listOf(0L, 500, 1000, 1500, 1750, 2000, 2250, 2500, 3000).zip(listOf(100, 80, 120, 110, 105, 95, 115, 90, 100))
            .forEach { (time, bpm) -> raw.add(sample(raw.size, time, bpm)) }
        listOf(100, 80, 120, 110, 100).forEachIndexed { i, bpm -> raw.add(sample(raw.size, 4000L + i * 250, bpm)) }
        repeat(304) { i ->
            val start = 6000L + 10 * i
            phases.add(phase(3 + i, start, start + 10, "active_set"))
            raw.add(sample(raw.size, start, 100, (3 + i).toLong()))
        }
        return fixture(9040, phases, raw)
    }

    private fun fixture(end: Long, phases: List<WorkoutPhaseIntervalEntity>, raw: List<HeartRateSampleEntity>, start: Long = 0,
        acquisitions: List<HeartRateAcquisitionIntervalEntity> = listOf(acquisition(0, start, end)),
        plan: String = PLAN, mode: String = "strength"): WorkoutSessionHistoricalResult.Resolved {
        val session = WorkoutSessionEntity(SESSION, mode = mode, status = "completed", planSnapshotJson = plan,
            startedAt = CREATED, endedAt = CREATED, timelineVersion = 1, lastDurableOffsetMs = end,
            lastMutationSequence = 1000001, trustedEndOffsetMs = end, terminalReason = "completed",
            displayMetadataContractVersion = 1, sessionDisplayMetadataJson = "{\"displayMetadataContractVersion\":1,\"entries\":[]}")
        val recording = HeartRateRecordingEntity(RECORDING, SESSION, "terminal", start, 0, end, 1000001,
            1, "ble_hrs", 1, 1, originalAnalysisVersion = 1)
        val closedPhases = phases.mapIndexed { i, phase -> if (i == phases.lastIndex) phase.copy(endMutationSequence = 1000001) else phase }
        val closedAcquisitions = acquisitions.mapIndexed { i, aq -> if (i == acquisitions.lastIndex) aq.copy(endMutationSequence = 1000001) else aq }
        val graph = CanonicalSessionGraphV1(session, closedPhases, recording, closedAcquisitions, raw)
        val snapshot = CanonicalAnalysisV1.derive(graph, CREATED)
        val strict = WorkoutSessionStrictReadResult.CanonicalTerminal(graph.copy(snapshots = listOf(snapshot)),
            parseCanonicalJson(plan) as CanonicalJsonValue.Obj, StrictSessionExecution(emptyList(), emptyList(), emptyList()))
        return resolveWorkoutSessionHistorical(strict, "zh-CN") as WorkoutSessionHistoricalResult.Resolved
    }

    private fun phase(sequence: Int, start: Long, end: Long, variant: String): WorkoutPhaseIntervalEntity {
        val kind = if (variant == "paused") "paused" else "strength_${variant}"
        return WorkoutPhaseIntervalEntity("phase-$sequence", SESSION, sequence, start, end, sequence.toLong(), sequence + 1L, null, kind,
            strengthIdentity(variant, kind))
    }
    private fun acquisition(sequence: Int, start: Long, end: Long, state: String = "live", reason: String? = null) =
        HeartRateAcquisitionIntervalEntity("acquisition-$sequence", RECORDING, sequence, start, end, sequence.toLong(), sequence + 1L,
            null, "expected_recording", null, state, reason)
    private fun sample(sequence: Int, offset: Long, bpm: Int, mutation: Long = 0) = HeartRateSampleEntity(RECORDING, sequence.toLong(), offset, mutation, bpm)
    private fun points(vararg points: Pair<Long, Int>) = points.mapIndexed { i, point -> sample(i, point.first, point.second) }
    private fun strengthIdentity(variant: String, kind: String): String {
        val payload = if (variant == "paused") "\"blockId\":null,\"setPlanId\":null,\"plannedExerciseId\":null,\"actualExerciseId\":null,\"exerciseSetIndex0\":null,\"globalSetIndex0\":null,\"setKind\":null"
            else "\"blockId\":\"block\",\"setPlanId\":\"set\",\"plannedExerciseId\":\"exercise\",\"actualExerciseId\":\"exercise\",\"exerciseSetIndex0\":0,\"globalSetIndex0\":0,\"setKind\":\"working\""
        return """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"$kind","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"$SIGNATURE"},"payload":{"variant":"$variant",$payload,"substitutedFromExerciseId":null}}"""
    }

    // Frozen plan and the four A/B leaf payloads from the accepted S07A fixtures; only sequences become 0..3.
    private fun timedFixture(legacy: Boolean): WorkoutSessionHistoricalResult.Resolved {
        val blocks: List<PlanBlock> = if (legacy) {
            val items = listOf(TimedExerciseItem(id = "work", stageType = TimedStageType.WORK, workDurationSec = 1, restAfterSec = 1),
                TimedExerciseItem(id = "custom", stageType = TimedStageType.CUSTOM, workDurationSec = 1),
                TimedExerciseItem(id = "rest", stageType = TimedStageType.REST, workDurationSec = 1))
            listOf(TimedCircuitBlock("L-A", 0, 2, items, restBetweenRoundsSec = 1), TimedCircuitBlock("L-B", 1, 2, items, restBetweenRoundsSec = 1),
                WarmupBlock("warmup", 2, durationSec = 1), StretchBlock("stretch", 3, durationSec = 1), CooldownBlock("cooldown", 4, durationSec = 1),
                WarmupBlock("boundary", 5, items = items), RestBlock("standalone", 6, durationSec = 1))
        } else listOf("C-A", "C-B").mapIndexed { order, id ->
            TimedCompositionBlock(id = id, order = order, warmupSec = 1, cooldownSec = 1, rounds = 2, restBetweenRoundsSec = 1,
                stageGroups = listOf(TimedCompositionStageGroup("group", 0, "Main group", TimedStageType.WORK.defaultColorHex,
                    targets = listOf(TimedCompositionTarget("action", 0, "Jumping jacks", TimedCompositionTargetKind.ACTION, 1, TimedStageType.WORK.defaultColorHex),
                        TimedCompositionTarget("custom", 1, "Shadow boxing", TimedCompositionTargetKind.CUSTOM, 1, TimedStageType.CUSTOM.defaultColorHex),
                        TimedCompositionTarget("rest", 2, "Breathe", TimedCompositionTargetKind.REST, 1, TimedStageType.REST.defaultColorHex)))))
        }
        val plan = WorkoutPlanSnapshot(planId = null, title = "Timed", mode = WorkoutMode.TIMED, blocks = blocks).toStorageJson()
        val prepared = (PlanSnapshotStorageV1Validator.prepare(plan, WorkoutMode.TIMED) as PreparedPlanSnapshotStorageV1Result.Valid).prepared
        val family = if (legacy) "legacy_timed_v1" else "timed_composition_v2"
        val phases = (0..4).map { i ->
            val block = if (legacy) "L-${if (i < 2) "A" else "B"}" else "C-${if (i < 2) "A" else "B"}"
            val rest = i % 2 == 1
            val kind = if (i == 4) "paused" else if (rest) "timed_rest" else "timed_work"
            val payload = if (legacy) {
                if (i == 4) """{"variant":"paused","blockId":null,"stepIndex0":null,"legacyBlockKind":null,"legacyStageType":null,"itemId":null,"exerciseId":null,"roundIndex0":null}"""
                else """{"variant":"${if (rest) "circuit_rest_after_item" else "circuit_item_work"}","blockId":"$block","stepIndex0":${if (rest) 1 else 0},"legacyBlockKind":"timed_circuit","legacyStageType":"${if (rest) "rest" else "work"}","itemId":"work","exerciseId":null,"roundIndex0":0}"""
            } else {
                if (i == 4) """{"variant":"paused","compositionVersion":2,"compositionBlockId":null,"timelineStageId":null,"timelineStageKind":null,"stageGroupId":null,"targetId":null,"targetKind":null,"roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":null,"stageInstanceIndex0":null,"targetInstanceIndex0":null,"stepIndex0":null}"""
                else """{"variant":"${if (rest) "stage_group_rest" else "stage_group_action"}","compositionVersion":2,"compositionBlockId":"$block","timelineStageId":"$block:r1:g1:group","timelineStageKind":"stage_group","stageGroupId":"group","targetId":"${if (rest) "rest" else "action"}","targetKind":"${if (rest) "rest" else "action"}","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":${if (rest) 2 else 0},"stageInstanceIndex0":1,"targetInstanceIndex0":${if (rest) 3 else 1},"stepIndex0":${if (rest) 3 else 1}}"""
            }
            val identity = """{"phaseIdentityContractVersion":1,"family":"$family","payloadVersion":${if (legacy) 1 else 2},"mode":"timed","phaseKind":"$kind","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"${prepared.orderedStructureDigestHexLowercase()}"},"payload":$payload}"""
            WorkoutPhaseIntervalEntity("phase-$i", SESSION, i, i * 1000L, (i + 1) * 1000L, i.toLong(), i + 1L, null, kind, identity)
        }
        return fixture(5000, phases, points(500L to 100, 1500L to 110, 2500L to 160, 3500L to 170), plan = plan, mode = "timed")
    }
    private fun focusJson(source: WorkoutSessionHistoricalResult.Resolved, legacy: Boolean, kind: String, sequence: Int?): String {
        val structure = source.timedStructures.single()
        return """{"focusContractVersion":1,"sessionId":"$SESSION","family":"${structure.family}","structureDigestHexLowercase":"${structure.structureDigestHexLowercase}","focusKind":"$kind","legacyBlockId":${if (legacy) "\"L-B\"" else "null"},"compositionBlockId":${if (legacy) "null" else "\"C-B\""},"roundIndex0":0,"phaseSequence":$sequence}"""
    }

    private companion object {
        const val SESSION = "projector-session"
        const val RECORDING = "projector-recording"
        const val CREATED = "2026-10-04T00:00:00Z"
        const val SIGNATURE = "c7e6dd87cd0794071a57be2dcbfde1f1adb2030364d2ff9549631eeda486e0e3"
        const val PLAN = """{"planSnapshotStorageContractVersion":1,"planId":null,"title":"Strength","mode":"strength","blocks":[{"id":"block","kind":"strength_exercise","order":0,"exerciseId":"exercise","sets":[{"id":"set","order":0,"kind":"working"}],"substitutions":[],"setTimerMode":"manual_start"}],"preferences":null,"followAlong":null}"""
        val REQUEST = HeartRateChartRequest(320, 200, 20)
    }
}
