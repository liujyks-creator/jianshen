package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.feature.workoutsession.restoreTimedFocusV1
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

internal data class HeartRateRawTuple(
    val recordingId: String, val offsetMs: Long, val mutationSequence: Long, val sampleSequence: Long
)

internal fun HeartRateSampleEntity.chartTuple() =
    HeartRateRawTuple(recordingId, offsetMs, mutationSequence, sampleSequence)

internal sealed interface HeartRateChartView {
    data object Whole : HeartRateChartView
    data class Timed(val structureFamily: String, val focusJson: String) : HeartRateChartView
    data class Phase(
        val sessionId: String, val family: String,
        val structureDigestHexLowercase: String, val phaseSequence: Int
    ) : HeartRateChartView
}

internal data class HeartRateChartRequest(
    val plotWidthPx: Int, val plotHeightPx: Int, val labelLineHeightPx: Int,
    val view: HeartRateChartView = HeartRateChartView.Whole,
    val selectedRawTuple: HeartRateRawTuple? = null
)

internal sealed interface HeartRateChartResult {
    data class NoRecording(val source: WorkoutSessionHistoricalResult) : HeartRateChartResult
    data class Unavailable(val source: WorkoutSessionHistoricalResult) : HeartRateChartResult
    data class Available(val projection: HeartRateChartProjection) : HeartRateChartResult
}

internal data class HeartRateChartDomain(val startOffsetMs: Long, val endOffsetMs: Long) {
    val startLabel: String get() = HeartRateChartProjector.formatElapsed(startOffsetMs)
    val endLabel: String get() = HeartRateChartProjector.formatElapsed(endOffsetMs)
}

internal data class HeartRatePhaseBand(
    val phase: WorkoutPhaseIntervalEntity, val identity: CanonicalJsonValue.Obj,
    val display: HistoricalPhaseDisplay?
)

/** Ordinals refer to the original canonical list, including points omitted from display. */
internal data class HeartRateSolidSegment(val firstOrdinal: Int, val lastOrdinal: Int, val phaseSequence: Int)
internal enum class HeartRateGapStyle { DASHED, BROKEN }
internal data class HeartRateVisualGap(
    val leftOrdinal: Int, val rightOrdinal: Int, val style: HeartRateGapStyle,
    val emptyAcquisitionSequences: List<Int>
)
internal enum class HeartRateAnchorSemantic {
    FIRST, MINIMUM, MAXIMUM, LAST, PHASE_BOUNDARY, GAP_EDGE, ORIGINAL_HIGHEST, SELECTED
}
internal data class HeartRateMandatoryRaw(val ordinal: Int, val semantics: Set<HeartRateAnchorSemantic>)
internal data class HeartRateTimeLandmark(
    val offsetMs: Long, val mutationSequence: Long,
    val phaseStarts: List<Int>, val phaseEnds: List<Int>, val rawOrdinals: List<Int>
)
internal data class HeartRateYAxis(val lowerBpm: Int, val upperBpm: Int, val ticks: List<HeartRateYTick>)
internal data class HeartRateYTick(val bpm: Int, val position: Double)
internal data class HeartRateRawContext(
    val raw: HeartRateSampleEntity, val phase: WorkoutPhaseIntervalEntity,
    val acquisition: HeartRateAcquisitionIntervalEntity
)

internal data class HeartRateChartProjection(
    val source: WorkoutSessionHistoricalResult.Resolved,
    val session: WorkoutSessionEntity,
    val recording: HeartRateRecordingEntity,
    val originalAnalysisVersion: Int,
    val inputLastMutationSequence: Long,
    val raw: List<HeartRateSampleEntity>,
    val snapshot: HeartRateAnalysisSnapshotEntity,
    val effectiveView: HeartRateChartView,
    val restoredToWhole: Boolean,
    val domain: HeartRateChartDomain,
    val phaseBands: List<HeartRatePhaseBand>,
    val phases: List<WorkoutPhaseIntervalEntity>,
    val acquisitions: List<HeartRateAcquisitionIntervalEntity>,
    val solidSegments: List<HeartRateSolidSegment>,
    val visualGaps: List<HeartRateVisualGap>,
    val displayPoints: List<HeartRateSampleEntity>,
    val displayOrdinals: List<Int>,
    val mandatoryRaw: List<HeartRateMandatoryRaw>,
    val timeLandmarks: List<HeartRateTimeLandmark>,
    val mandatorySemanticAnchorCount: Int,
    val nonMandatoryPointCount: Int,
    val originalAverageBpm: Int?,
    val originalHighest: HeartRateSampleEntity?,
    val visibleHighestMarker: Boolean,
    val selectedRaw: HeartRateSampleEntity?,
    val selectedInView: Boolean,
    val yAxis: HeartRateYAxis?,
    val hasRawInView: Boolean,
    internal val phaseByOrdinal: IntArray,
    internal val acquisitionByOrdinal: IntArray
) {
    val mandatoryRawPointCount: Int get() = mandatoryRaw.size
}

internal sealed interface HeartRateRawScrubResult {
    data class Recorded(
        val requestedOffsetMs: Long, val offsetMs: Long, val formattedTime: String,
        val points: List<HeartRateRawContext>, val preferredRaw: HeartRateSampleEntity?
    ) : HeartRateRawScrubResult
    data class NotRecorded(
        val requestedOffsetMs: Long, val formattedTime: String,
        val gap: HeartRateVisualGap?, val acquisitions: List<HeartRateAcquisitionIntervalEntity>
    ) : HeartRateRawScrubResult
}

/** Pure display owner. The strict reader and historical resolver have already validated its input. */
internal object HeartRateChartProjector {
    fun project(source: WorkoutSessionHistoricalResult, request: HeartRateChartRequest): HeartRateChartResult {
        if (source !is WorkoutSessionHistoricalResult.Resolved) return HeartRateChartResult.Unavailable(source)
        val strict = source.source
        if (strict is WorkoutSessionStrictReadResult.LegacyTerminal) return HeartRateChartResult.NoRecording(source)
        if (strict !is WorkoutSessionStrictReadResult.CanonicalTerminal) return HeartRateChartResult.Unavailable(source)
        val graph = strict.graph
        val recording = graph.recording ?: return HeartRateChartResult.NoRecording(source)
        val snapshot = graph.snapshots.single { it.analysisVersion == recording.originalAnalysisVersion }
        val raw = graph.samples
        val phases = graph.phases
        val acquisitions = graph.acquisitions
        val displays = source.phaseDisplays.associateBy { it.sequence }
        val bands = phases.map { phase ->
            val display = displays[phase.sequence]
            HeartRatePhaseBand(phase, display?.phaseIdentity ?: parseCanonicalJson(phase.phaseIdentityJson) as CanonicalJsonValue.Obj, display)
        }
        val focused = focusPhases(source, bands, request.view)
        val effectiveView = if (focused == null) HeartRateChartView.Whole else request.view
        val whole = effectiveView == HeartRateChartView.Whole
        val firstPhase = if (whole) phases.first() else focused!!.first()
        val lastPhase = if (whole) phases.last() else focused!!.last()
        val domain = HeartRateChartDomain(if (whole) 0L else firstPhase.startOffsetMs,
            if (whole) graph.session.trustedEndOffsetMs!! else lastPhase.endOffsetMs!!)
        fun visible(sample: HeartRateSampleEntity): Boolean =
            (whole || compareTuple(sample.offsetMs, sample.mutationSequence, firstPhase.startOffsetMs, firstPhase.startMutationSequence) >= 0) &&
                (compareTuple(sample.offsetMs, sample.mutationSequence, lastPhase.endOffsetMs!!, lastPhase.endMutationSequence!!) < 0 ||
                    lastPhase === phases.last() && compareTuple(sample.offsetMs, sample.mutationSequence,
                        graph.session.trustedEndOffsetMs!!, graph.session.lastMutationSequence!!) == 0)

        val phaseIndex = IntArray(raw.size)
        val acquisitionIndex = IntArray(raw.size)
        val acquisitionCounts = IntArray(acquisitions.size)
        var p = 0
        var a = 0
        var selectedOrdinal = -1
        var highestOrdinal = -1
        for (i in raw.indices) {
            val point = raw[i]
            while (p < phases.lastIndex && compareTuple(point.offsetMs, point.mutationSequence,
                    phases[p].endOffsetMs!!, phases[p].endMutationSequence!!) >= 0) p++
            while (a < acquisitions.lastIndex && compareTuple(point.offsetMs, point.mutationSequence,
                    acquisitions[a].endOffsetMs!!, acquisitions[a].endMutationSequence!!) >= 0) a++
            phaseIndex[i] = p
            acquisitionIndex[i] = a
            acquisitionCounts[a]++
            if (request.selectedRawTuple != null && matches(point, request.selectedRawTuple)) selectedOrdinal = i
            if (point.sampleSequence == snapshot.highestSampleSequence && point.offsetMs == snapshot.highestOffsetMs &&
                point.mutationSequence == snapshot.highestMutationSequence) highestOrdinal = i
        }
        val emptyInterruptions = acquisitions.indices.filter { index ->
            val interval = acquisitions[index]
            acquisitionCounts[index] == 0 && interval.endOffsetMs!! > interval.startOffsetMs &&
                (interval.deviceState != "live" || interval.recordingIntent != "expected_recording")
        }
        val segments = mutableListOf<HeartRateSolidSegment>()
        val gaps = mutableListOf<HeartRateVisualGap>()
        var first = -1
        var previous = -1
        var emptyCursor = 0
        for (i in raw.indices) {
            if (!visible(raw[i])) continue
            if (previous >= 0) {
                while (emptyCursor < emptyInterruptions.size &&
                    acquisitions[emptyInterruptions[emptyCursor]].endOffsetMs!! <= raw[previous].offsetMs) emptyCursor++
                var e = emptyCursor
                val crossed = mutableListOf<Int>()
                while (e < emptyInterruptions.size && acquisitions[emptyInterruptions[e]].startOffsetMs < raw[i].offsetMs) {
                    crossed.add(emptyInterruptions[e++])
                }
                val samePhase = phaseIndex[previous] == phaseIndex[i]
                val delta = raw[i].offsetMs - raw[previous].offsetMs
                if (!samePhase || delta > 2500L || crossed.isNotEmpty()) {
                    segments.add(HeartRateSolidSegment(first, previous, phases[phaseIndex[previous]].sequence))
                    gaps.add(HeartRateVisualGap(previous, i,
                        if (samePhase && delta <= 20000L) HeartRateGapStyle.DASHED else HeartRateGapStyle.BROKEN, crossed))
                    first = i
                }
            } else first = i
            previous = i
        }
        if (previous >= 0) segments.add(HeartRateSolidSegment(first, previous, phases[phaseIndex[previous]].sequence))

        val flags = IntArray(raw.size)
        fun mark(i: Int, semantic: HeartRateAnchorSemantic) { flags[i] = flags[i] or (1 shl semantic.ordinal) }
        for (segment in segments) {
            var minimum = segment.firstOrdinal
            var maximum = minimum
            for (i in segment.firstOrdinal..segment.lastOrdinal) {
                if (raw[i].bpm < raw[minimum].bpm) minimum = i
                if (raw[i].bpm > raw[maximum].bpm) maximum = i
            }
            mark(segment.firstOrdinal, HeartRateAnchorSemantic.FIRST)
            mark(minimum, HeartRateAnchorSemantic.MINIMUM)
            mark(maximum, HeartRateAnchorSemantic.MAXIMUM)
            mark(segment.lastOrdinal, HeartRateAnchorSemantic.LAST)
        }
        for (gap in gaps) {
            mark(gap.leftOrdinal, HeartRateAnchorSemantic.GAP_EDGE)
            mark(gap.rightOrdinal, HeartRateAnchorSemantic.GAP_EDGE)
        }
        if (highestOrdinal >= 0 && visible(raw[highestOrdinal])) mark(highestOrdinal, HeartRateAnchorSemantic.ORIGINAL_HIGHEST)
        if (selectedOrdinal >= 0 && visible(raw[selectedOrdinal])) mark(selectedOrdinal, HeartRateAnchorSemantic.SELECTED)

        // Coalesce coincident boundary tuples while retaining every phase start/end identity.
        val landmarkBuilders = linkedMapOf<Pair<Long, Long>, LandmarkBuilder>()
        for (phase in phases) {
            landmarkBuilders.getOrPut(phase.startOffsetMs to phase.startMutationSequence) { LandmarkBuilder() }.starts.add(phase.sequence)
            landmarkBuilders.getOrPut(phase.endOffsetMs!! to phase.endMutationSequence!!) { LandmarkBuilder() }.ends.add(phase.sequence)
        }
        val landmarks = landmarkBuilders.map { (tuple, builder) ->
            val at = mutableListOf<Int>()
            var i = lowerBoundTuple(raw, tuple.first, tuple.second)
            while (i < raw.size && raw[i].offsetMs == tuple.first && raw[i].mutationSequence == tuple.second) {
                if (visible(raw[i])) { mark(i, HeartRateAnchorSemantic.PHASE_BOUNDARY); at.add(i) }
                i++
            }
            HeartRateTimeLandmark(tuple.first, tuple.second, builder.starts, builder.ends, at)
        }
        val mandatory = mutableListOf<HeartRateMandatoryRaw>()
        for (i in flags.indices) if (flags[i] != 0) mandatory.add(HeartRateMandatoryRaw(i,
            HeartRateAnchorSemantic.entries.filterTo(linkedSetOf()) { flags[i] and (1 shl it.ordinal) != 0 }))
        val chosen = BooleanArray(raw.size)
        mandatory.forEach { chosen[it.ordinal] = true }
        val bucketCount = ((request.plotWidthPx.coerceIn(320, 1600) - mandatory.size).coerceAtLeast(0)) / 4
        val allocations = allocateBuckets(segments, raw, bucketCount)
        for ((index, segment) in segments.withIndex()) {
            val count = allocations[index]
            if (count == 0) continue
            val start = raw[segment.firstOrdinal].offsetMs
            val span = raw[segment.lastOrdinal].offsetMs - start
            var cursor = segment.firstOrdinal
            for (bucket in 0 until count) {
                val begin = cursor
                var minimum = cursor
                var maximum = cursor
                while (cursor <= segment.lastOrdinal && (bucket == count - 1 ||
                        (raw[cursor].offsetMs - start) * count < span * (bucket + 1))) {
                    if (raw[cursor].bpm < raw[minimum].bpm) minimum = cursor
                    if (raw[cursor].bpm > raw[maximum].bpm) maximum = cursor
                    cursor++
                }
                if (cursor > begin) {
                    chosen[begin] = true; chosen[minimum] = true; chosen[maximum] = true; chosen[cursor - 1] = true
                }
            }
        }
        val ordinals = raw.indices.filter { chosen[it] }
        var minBpm: Int? = snapshot.observedAvgBpm
        var maxBpm: Int? = minBpm
        for (segment in segments) for (i in segment.firstOrdinal..segment.lastOrdinal) {
            minBpm = minBpm?.coerceAtMost(raw[i].bpm) ?: raw[i].bpm
            maxBpm = maxBpm?.coerceAtLeast(raw[i].bpm) ?: raw[i].bpm
        }
        val highestVisible = highestOrdinal >= 0 && visible(raw[highestOrdinal])
        if (highestVisible) {
            minBpm = minBpm?.coerceAtMost(snapshot.observedMaxBpm!!) ?: snapshot.observedMaxBpm
            maxBpm = maxBpm?.coerceAtLeast(snapshot.observedMaxBpm!!) ?: snapshot.observedMaxBpm
        }
        // A recording with no raw has no axis, even if a caller later renders other status facts.
        val y = if (raw.isEmpty() || minBpm == null) null else computeYAxis(minBpm, maxBpm!!, request.plotHeightPx, request.labelLineHeightPx)
        val semanticCount = mandatory.size + landmarks.count { it.rawOrdinals.isEmpty() && it.offsetMs in domain.startOffsetMs..domain.endOffsetMs }
        return HeartRateChartResult.Available(HeartRateChartProjection(
            source, graph.session, recording, recording.originalAnalysisVersion!!, snapshot.inputLastMutationSequence,
            raw, snapshot, effectiveView, request.view != HeartRateChartView.Whole && whole,
            domain, bands, phases, acquisitions, segments, gaps, ordinals.map { raw[it] }, ordinals,
            mandatory, landmarks, semanticCount, ordinals.size - mandatory.size, snapshot.observedAvgBpm,
            if (highestOrdinal >= 0) raw[highestOrdinal] else null, highestVisible,
            if (selectedOrdinal >= 0) raw[selectedOrdinal] else null,
            selectedOrdinal >= 0 && visible(raw[selectedOrdinal]), y, segments.isNotEmpty(), phaseIndex, acquisitionIndex
        ))
    }

    fun scrub(projection: HeartRateChartProjection, offsetMs: Long, preferredRawTuple: HeartRateRawTuple? = null): HeartRateRawScrubResult {
        val raw = projection.raw
        val gap = projection.visualGaps.firstOrNull { offsetMs > raw[it.leftOrdinal].offsetMs && offsetMs < raw[it.rightOrdinal].offsetMs }
        fun missing() = HeartRateRawScrubResult.NotRecorded(offsetMs, formatElapsed(offsetMs), gap,
            projection.acquisitions.filter { offsetMs >= it.startOffsetMs && offsetMs < it.endOffsetMs!! })
        if (gap != null || offsetMs !in projection.domain.startOffsetMs..projection.domain.endOffsetMs) return missing()
        val segment = projection.solidSegments.firstOrNull { offsetMs in raw[it.firstOrdinal].offsetMs..raw[it.lastOrdinal].offsetMs }
            ?: return missing()
        val upper = lowerBoundOffset(raw, offsetMs, segment.firstOrdinal, segment.lastOrdinal + 1)
        val ordinal = when {
            upper > segment.lastOrdinal -> segment.lastOrdinal
            upper == segment.firstOrdinal -> upper
            offsetMs - raw[upper - 1].offsetMs <= raw[upper].offsetMs - offsetMs -> upper - 1
            else -> upper
        }
        val time = raw[ordinal].offsetMs
        var first = ordinal
        var last = ordinal
        while (first > 0 && raw[first - 1].offsetMs == time) first--
        while (last < raw.lastIndex && raw[last + 1].offsetMs == time) last++
        val points = (first..last).map { i -> HeartRateRawContext(raw[i], projection.phases[projection.phaseByOrdinal[i]],
            projection.acquisitions[projection.acquisitionByOrdinal[i]]) }
        return HeartRateRawScrubResult.Recorded(offsetMs, time, formatElapsed(time), points,
            if (preferredRawTuple == null) null else points.firstOrNull { matches(it.raw, preferredRawTuple) }?.raw)
    }

    fun computeYAxis(minBpm: Int, maxBpm: Int, plotHeightPx: Int, labelLineHeightPx: Int): HeartRateYAxis {
        val range = (maxBpm - minBpm) / 0.90
        val extra = ((40.0 - range) / 2).coerceAtLeast(0.0)
        val lower = floor(minBpm - 0.02 * range - extra).toInt()
        val upper = ceil(maxBpm + 0.08 * range + extra).toInt()
        val intervals = (plotHeightPx / (2 * labelLineHeightPx)).coerceIn(1, 4)
        val ticks = (0..intervals).map { i ->
            val value = (lower + (upper - lower).toDouble() * i / intervals).roundToInt()
            HeartRateYTick(value, (value - lower).toDouble() / (upper - lower))
        }
        return HeartRateYAxis(lower, upper, ticks)
    }

    fun formatElapsed(offsetMs: Long): String {
        val seconds = offsetMs / 1000
        val tail = (seconds % 60).toString().padStart(2, '0')
        return if (seconds < 3600) "${seconds / 60}:$tail"
        else "${seconds / 3600}:${(seconds / 60 % 60).toString().padStart(2, '0')}:$tail"
    }

    private fun focusPhases(source: WorkoutSessionHistoricalResult.Resolved, bands: List<HeartRatePhaseBand>, view: HeartRateChartView): List<WorkoutPhaseIntervalEntity>? = when (view) {
        HeartRateChartView.Whole -> null
        is HeartRateChartView.Timed -> {
            val structure = source.timedStructures.singleOrNull { it.family == view.structureFamily }
            if (structure == null || !structure.focusEligible) null else {
                val focus = restoreTimedFocusV1(structure, view.focusJson)
                if (focus.focusKind == "whole") null else {
                    val sequences = structure.phases.filter {
                        if (focus.focusKind == "round") it.blockId == (focus.legacyBlockId ?: focus.compositionBlockId) && it.roundIndex0 == focus.roundIndex0
                        else it.phaseSequence == focus.phaseSequence
                    }.mapTo(hashSetOf()) { it.phaseSequence }
                    bands.filter { it.phase.sequence.toLong() in sequences }.map { it.phase }
                }
            }
        }
        is HeartRateChartView.Phase -> bands.singleOrNull { band ->
            val identity = band.identity
            val family = (identity.fields.getValue("family") as CanonicalJsonValue.Str).value
            val signature = identity.fields.getValue("orderedStructureSignature") as CanonicalJsonValue.Obj
            band.phase.sessionId == view.sessionId && band.phase.sequence == view.phaseSequence &&
                family == view.family && family != "legacy_timed_v1" && family != "timed_composition_v2" &&
                (signature.fields.getValue("digestHexLowercase") as CanonicalJsonValue.Str).value == view.structureDigestHexLowercase
        }?.let { listOf(it.phase) }
    }

    private fun allocateBuckets(segments: List<HeartRateSolidSegment>, raw: List<HeartRateSampleEntity>, count: Int): IntArray {
        val result = IntArray(segments.size)
        val spans = segments.map { raw[it.lastOrdinal].offsetMs - raw[it.firstOrdinal].offsetMs }
        val total = spans.sum()
        if (total == 0L || count == 0) return result
        val remainders = LongArray(segments.size)
        for (i in spans.indices) {
            result[i] = (spans[i] * count / total).toInt()
            remainders[i] = spans[i] * count % total
        }
        // Only bucket allocation is ordered by remainder; canonical input is never sorted.
        val order = spans.indices.filter { spans[it] > 0 }.sortedWith(compareByDescending<Int> { remainders[it] }.thenBy { it })
        repeat(count - result.sum()) { result[order[it]]++ }
        return result
    }

    private class LandmarkBuilder(val starts: MutableList<Int> = mutableListOf(), val ends: MutableList<Int> = mutableListOf())
    private fun matches(point: HeartRateSampleEntity, tuple: HeartRateRawTuple) =
        point.recordingId == tuple.recordingId && point.offsetMs == tuple.offsetMs &&
            point.mutationSequence == tuple.mutationSequence && point.sampleSequence == tuple.sampleSequence
    private fun compareTuple(offset: Long, mutation: Long, otherOffset: Long, otherMutation: Long): Int =
        if (offset != otherOffset) offset.compareTo(otherOffset) else mutation.compareTo(otherMutation)
    private fun lowerBoundTuple(raw: List<HeartRateSampleEntity>, offset: Long, mutation: Long): Int {
        var low = 0; var high = raw.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (compareTuple(raw[mid].offsetMs, raw[mid].mutationSequence, offset, mutation) < 0) low = mid + 1 else high = mid
        }
        return low
    }
    private fun lowerBoundOffset(raw: List<HeartRateSampleEntity>, offset: Long, begin: Int, end: Int): Int {
        var low = begin; var high = end
        while (low < high) { val mid = (low + high) ushr 1; if (raw[mid].offsetMs < offset) low = mid + 1 else high = mid }
        return low
    }
}
