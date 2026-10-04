package com.liujyks.trainflow.core.data

import android.os.Debug
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liujyks.trainflow.core.database.CanonicalAnalysisV1
import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1Validator
import com.liujyks.trainflow.core.database.CanonicalValidationResult
import com.liujyks.trainflow.core.database.entity.*
import com.liujyks.trainflow.core.database.parseCanonicalJson
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E17ProjectorPerformanceContractTest {
    @Test
    fun pBalancedV2PureProjectionMeetsEveryPerRunBudget() {
        val source = prepareInput()
        val graph = (source.source as WorkoutSessionStrictReadResult.CanonicalTerminal).graph
        assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(graph))
        assertEquals(250000, graph.samples.size)
        assertEquals(10000, graph.phases.size)
        assertEquals(10000, graph.acquisitions.size)
        assertEquals(28800000L, graph.session.trustedEndOffsetMs)
        assertEquals((0L..31L).toList(), graph.samples.take(32).map { it.sampleSequence })
        assertTrue(graph.samples.take(32).all { it.offsetMs == 5000L && it.mutationSequence == 0L && it.bpm == 80 })
        assertEquals(graph.phases[20].startOffsetMs, graph.phases[20].endOffsetMs)
        assertEquals(graph.acquisitions[20].startOffsetMs, graph.acquisitions[20].endOffsetMs)
        assertEquals(21L, graph.phases[20].endMutationSequence)
        assertEquals(12, graph.acquisitions.map { it.deviceState }.toSet().size)
        assertEquals(DEVICE_FACTS.toSet(), graph.acquisitions.map { it.deviceState to it.deviceReason }.toSet())
        assertEquals(EXCLUSIONS.toSet(), graph.acquisitions.mapNotNull { it.intentReason }.toSet())
        val tail = graph.samples.takeLast(12)
        assertEquals((32L..43L).toList(), tail.map { it.sampleSequence })
        assertEquals(28769999L, tail[0].offsetMs)
        assertEquals(28770000L, tail[1].offsetMs)
        assertEquals(9999L, tail[1].mutationSequence)
        assertEquals(28770001L, tail[2].offsetMs)
        assertEquals(24992L, tail.last().offsetMs - tail[10].offsetMs)
        val snapshot = graph.snapshots.single()
        assertEquals(180, snapshot.observedAvgBpm)
        assertEquals(180, snapshot.observedMaxBpm)
        assertEquals(42L, snapshot.highestSampleSequence)
        assertEquals(28770009L, snapshot.highestOffsetMs)
        assertNotNull(snapshot.zoneDurationsJson)
        val frozenJson = listOf(snapshot.analysisConfigJson, snapshot.zoneDurationsJson, snapshot.phaseAggregatesJson,
            snapshot.durationBreakdownJson, snapshot.qualityReasonsJson)
        val expected = expectedMandatorySequences()
        val selected = graph.samples[graph.samples.lastIndex - 1].chartTuple()
        val request = HeartRateChartRequest(1600, 640, 32, selectedRawTuple = selected)
        repeat(7) { run ->
            val pss = Collections.synchronizedList(mutableListOf<Int>())
            val sampling = AtomicBoolean(true)
            pss.add(totalPssKb())
            val sampler = Thread({
                while (sampling.get()) { pss.add(totalPssKb()); SystemClock.sleep(50) }
            }, "e17-projector-pss")
            sampler.start()
            val projection: HeartRateChartProjection
            val elapsedNs: Long
            try {
                val start = SystemClock.elapsedRealtimeNanos()
                val result = HeartRateChartProjector.project(source, request)
                val end = SystemClock.elapsedRealtimeNanos()
                elapsedNs = end - start
                projection = (result as HeartRateChartResult.Available).projection
                pss.add(totalPssKb())
            } finally {
                sampling.set(false)
                sampler.join()
            }
            pss.add(totalPssKb())
            val peakBytes = pss.max().toLong() * 1024
            val label = if (run < 2) "warmup-${run + 1}" else "measured-${run - 1}"
            println("E17_PROJECTOR $label elapsedNs=$elapsedNs elapsedMs=${elapsedNs / 1000000.0} " +
                "pssSamplesKb=${pss.toList()} absolutePeakPssBytes=$peakBytes " +
                "rawCount=${projection.raw.size} phaseCount=${projection.phaseBands.size} acquisitionCount=${projection.acquisitions.size} " +
                "displayCount=${projection.displayPoints.size} mandatoryRawCount=${projection.mandatoryRawPointCount} " +
                "mandatorySemanticAnchorCount=${projection.mandatorySemanticAnchorCount} nonMandatoryCount=${projection.nonMandatoryPointCount} " +
                "sessionId=${projection.session.id} recordingId=${projection.recording.recordingId} " +
                "originalAnalysisVersion=${projection.originalAnalysisVersion} inputLastMutationSequence=${projection.inputLastMutationSequence}")
            assertEquals(expected, projection.mandatoryRaw.mapTo(linkedSetOf()) { projection.raw[it.ordinal].sampleSequence })
            assertTrue(projection.mandatoryRawPointCount <= 20000)
            assertTrue(projection.mandatorySemanticAnchorCount <= 20000)
            assertTrue(projection.nonMandatoryPointCount <= 1600)
            assertTrue(projection.displayPoints.size <= projection.mandatoryRawPointCount + 1600)
            assertTrue(projection.displayOrdinals.zipWithNext().all { (left, right) -> left < right })
            assertSame(graph.samples, projection.raw)
            assertSame(snapshot, projection.snapshot)
            assertSame(graph.phases, projection.phases)
            assertSame(graph.acquisitions, projection.acquisitions)
            assertEquals(frozenJson, listOf(projection.snapshot.analysisConfigJson, projection.snapshot.zoneDurationsJson,
                projection.snapshot.phaseAggregatesJson, projection.snapshot.durationBreakdownJson, projection.snapshot.qualityReasonsJson))
            assertEquals(10000, projection.phaseBands.size)
            repeat(10000) { i ->
                val boundary = phaseBoundary(i)
                val phase = projection.phaseBands[i].phase
                assertEquals(boundary.first, phase.startOffsetMs); assertEquals(boundary.second, phase.endOffsetMs)
                assertSame(graph.phases[i], phase)
                assertTrue(projection.timeLandmarks.any { it.offsetMs == boundary.first && it.mutationSequence == i.toLong() && i in it.phaseStarts })
            }
            assertEquals(77, projection.yAxis!!.lowerBpm)
            assertEquals(189, projection.yAxis.upperBpm)
            assertEquals(listOf(77, 105, 133, 161, 189), projection.yAxis.ticks.map { it.bpm })
            assertSame(graph.samples[graph.samples.lastIndex - 1], projection.originalHighest)
            assertSame(projection.originalHighest, projection.selectedRaw)
            assertEquals(180, projection.originalAverageBpm)
            assertEquals(HeartRateGapStyle.BROKEN, projection.visualGaps.last().style)
            println("E17_PROJECTOR $label oracle=PASS")
            if (run >= 2) {
                assertTrue("$label elapsedNs=$elapsedNs >1500000000", elapsedNs <= 1500000000L)
                assertTrue("$label absolutePeakPssBytes=$peakBytes >335544320", peakBytes <= 335544320L)
                println("E17_PROJECTOR $label perRunBudget=PASS")
            }
        }
    }

    /** Closed-form oracle uses boundary arithmetic and the known period, never scans raw extrema. */
    private fun expectedMandatorySequences(): Set<Long> {
        val expected = linkedSetOf(0L, 31L, 32L, 33L, 42L, 43L)
        repeat(10000) { phase ->
            val (start, end) = phaseBoundary(phase)
            // Bulk mutation zero precedes every positive start mutation, including exact end offsets.
            val first = (start + if (phase == 0) 0 else 1).coerceAtLeast(10000) - 10000
            val last = end.coerceAtMost(259955) - 10000
            if (end > start && first <= last && first <= 249955 && last >= 0) {
                expected.add(44 + first); expected.add(44 + last)
                val first80 = first + (20 - first % 20) % 20
                val first99 = first + (19 - first % 20 + 20) % 20
                val minimum = if (first80 <= last) first80 else first
                val maximum = if (first99 <= last) first99 else last
                expected.add(44 + minimum); expected.add(44 + maximum)
            }
        }
        return expected
    }

    private fun prepareInput(): WorkoutSessionHistoricalResult.Resolved {
        val session = WorkoutSessionEntity(SESSION, mode = "strength", status = "completed", planSnapshotJson = PLAN,
            startedAt = CREATED, endedAt = CREATED, timelineVersion = 1, lastDurableOffsetMs = 28800000,
            lastMutationSequence = 1000001, trustedEndOffsetMs = 28800000, terminalReason = "completed",
            displayMetadataContractVersion = 1, sessionDisplayMetadataJson = "{\"displayMetadataContractVersion\":1,\"entries\":[]}")
        val phases = (0 until 10000).map { i ->
            val (start, end) = phaseBoundary(i)
            val variant = when (i) { 0 -> "prepare_set"; 9999 -> "active_set"; else -> "paused" }
            val kind = if (variant == "paused") "paused" else "strength_$variant"
            WorkoutPhaseIntervalEntity("$SESSION:phase:$i", SESSION, i, start, end, i.toLong(),
                if (i == 9999) 1000001 else i + 1L, null, kind, identity(variant, kind))
        }
        val acquisitions = (0 until 10000).map { i ->
            val (start, end) = phaseBoundary(i)
            val device = DEVICE_FACTS[i % DEVICE_FACTS.size]
            val expected = i == 0 || i == 1 || i == 9999
            HeartRateAcquisitionIntervalEntity("$RECORDING:acquisition:$i", RECORDING, i, start, end, i.toLong(),
                if (i == 9999) 1000001 else i + 1L, null, if (expected) "expected_recording" else "user_excluded",
                if (expected) null else EXCLUSIONS[(i - 2) % 3], device.first, device.second)
        }
        val raw = ArrayList<HeartRateSampleEntity>(250000)
        repeat(32) { raw.add(HeartRateSampleEntity(RECORDING, it.toLong(), 5000, 0, 80)) }
        repeat(249956) { raw.add(HeartRateSampleEntity(RECORDING, 44L + it, 10000L + it, 0, 80 + it % 20)) }
        raw.add(HeartRateSampleEntity(RECORDING, 32, 28769999, 0, 90))
        listOf(99, 100, 119, 120, 139, 140, 159, 160, 179, 180).forEachIndexed { i, bpm ->
            raw.add(HeartRateSampleEntity(RECORDING, 33L + i, 28770000L + i, if (i == 0) 9999 else 0, bpm))
        }
        raw.add(HeartRateSampleEntity(RECORDING, 43, 28795001, 0, 180))
        val recording = HeartRateRecordingEntity(RECORDING, SESSION, "terminal", 0, 0, 28800000, 1000001,
            1, "ble_hrs", 1, 1, personalMaxBpm = 200, effectiveMaxBpm = 200, effectiveMaxSource = "personal_max",
            zoneSnapshotJson = ZONES, originalAnalysisVersion = 1)
        val graph = CanonicalSessionGraphV1(session, phases, recording, acquisitions, raw)
        val snapshot = CanonicalAnalysisV1.derive(graph, CREATED)
        val strict = WorkoutSessionStrictReadResult.CanonicalTerminal(graph.copy(snapshots = listOf(snapshot)),
            parseCanonicalJson(PLAN) as CanonicalJsonValue.Obj, StrictSessionExecution(emptyList(), emptyList(), emptyList()))
        return resolveWorkoutSessionHistorical(strict, "zh-CN") as WorkoutSessionHistoricalResult.Resolved
    }

    private fun phaseBoundary(index: Int): Pair<Long, Long> {
        if (index == 0) return 0L to 3000L
        if (index == 9999) return 28770000L to 28800000L
        val ordinal = if (index <= 20) index - 1 else index - 2
        val start = 3000L + 28767000L * ordinal / 9997
        return start to if (index == 20) start else 3000L + 28767000L * (ordinal + 1) / 9997
    }
    private fun totalPssKb() = Debug.MemoryInfo().also(Debug::getMemoryInfo).totalPss
    private fun identity(variant: String, kind: String): String {
        val payload = if (variant == "paused") "\"blockId\":null,\"setPlanId\":null,\"plannedExerciseId\":null,\"actualExerciseId\":null,\"exerciseSetIndex0\":null,\"globalSetIndex0\":null,\"setKind\":null"
            else "\"blockId\":\"block\",\"setPlanId\":\"set\",\"plannedExerciseId\":\"exercise\",\"actualExerciseId\":\"exercise\",\"exerciseSetIndex0\":0,\"globalSetIndex0\":0,\"setKind\":\"working\""
        return """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"$kind","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c7e6dd87cd0794071a57be2dcbfde1f1adb2030364d2ff9549631eeda486e0e3"},"payload":{"variant":"$variant",$payload,"substitutedFromExerciseId":null}}"""
    }
    private companion object {
        const val SESSION = "e17-projector-performance"
        const val RECORDING = "e17-projector-performance-recording"
        const val CREATED = "2026-10-04T00:00:00Z"
        const val PLAN = """{"planSnapshotStorageContractVersion":1,"planId":null,"title":"Strength","mode":"strength","blocks":[{"id":"block","kind":"strength_exercise","order":0,"exerciseId":"exercise","sets":[{"id":"set","order":0,"kind":"working"}],"substitutions":[],"setTimerMode":"manual_start"}],"preferences":null,"followAlong":null}"""
        const val ZONES = """{"zoneSnapshotContractVersion":1,"unit":"bpm","effectiveMaxBpm":200,"effectiveMaxSource":"personal_max","zones":[{"zoneId":"below_50","lowerBoundBasisPointsInclusive":null,"upperBoundBasisPointsExclusive":5000},{"zoneId":"from_50_to_60","lowerBoundBasisPointsInclusive":5000,"upperBoundBasisPointsExclusive":6000},{"zoneId":"from_60_to_70","lowerBoundBasisPointsInclusive":6000,"upperBoundBasisPointsExclusive":7000},{"zoneId":"from_70_to_80","lowerBoundBasisPointsInclusive":7000,"upperBoundBasisPointsExclusive":8000},{"zoneId":"from_80_to_90","lowerBoundBasisPointsInclusive":8000,"upperBoundBasisPointsExclusive":9000},{"zoneId":"at_or_above_90","lowerBoundBasisPointsInclusive":9000,"upperBoundBasisPointsExclusive":null}]}"""
        val EXCLUSIONS = listOf("user_turned_off", "user_opted_out", "user_disconnected_suppress_recovery")
        val DEVICE_FACTS = listOf("not_observing" to null, "no_source_selected" to "source_not_selected",
            "permission_required" to "permission_missing", "permission_required" to "permission_revoked",
            "bluetooth_unavailable" to "bluetooth_off", "bluetooth_unavailable" to "platform_unavailable",
            "searching" to "initial_acquisition", "searching" to "automatic_recovery", "connecting" to "initial_acquisition",
            "waiting_first_sample" to "automatic_recovery", "live" to null, "stale" to "first_sample_timeout",
            "stale" to "sample_stale_timeout", "reconnecting" to "automatic_recovery", "reconnecting" to "unexpected_disconnect",
            "disconnected" to "source_unavailable", "disconnected" to "unexpected_disconnect", "disconnected" to "connection_timeout",
            "technical_failure" to "measurement_stream_unavailable", "technical_failure" to "platform_failure")
    }
}
