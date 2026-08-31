package com.liujyks.trainflow.core.database

import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.model.WorkoutMode

data class CanonicalTuple(
    val offsetMs: Long,
    val mutationSequence: Long
) : Comparable<CanonicalTuple> {
    override fun compareTo(other: CanonicalTuple): Int {
        val offsetComparison = offsetMs.compareTo(other.offsetMs)
        return if (offsetComparison != 0) offsetComparison else {
            mutationSequence.compareTo(other.mutationSequence)
        }
    }
}

sealed interface CanonicalSessionHeaderV1Result {
    data class Legacy(
        val status: String,
        val timelineStatus: String
    ) : CanonicalSessionHeaderV1Result

    data class CanonicalRunning(
        val durableTuple: CanonicalTuple
    ) : CanonicalSessionHeaderV1Result

    data class CanonicalTerminal(
        val finalTuple: CanonicalTuple
    ) : CanonicalSessionHeaderV1Result

    data class Invalid(val code: String) : CanonicalSessionHeaderV1Result
}

object CanonicalSessionHeaderV1Validator {
    fun validate(session: WorkoutSessionEntity): CanonicalSessionHeaderV1Result {
        val headerValues = listOf(
            session.timelineVersion,
            session.lastDurableOffsetMs,
            session.lastMutationSequence,
            session.trustedEndOffsetMs,
            session.terminalReason,
            session.displayMetadataContractVersion,
            session.sessionDisplayMetadataJson
        )
        if (headerValues.all { value -> value == null }) {
            if (session.status !in SESSION_STATUSES) return invalidCanonicalHeader()
            val timelineStatus = when (session.status) {
                "ready" -> "legacy_incomplete_nonterminal"
                "active", "paused" -> "legacy_noncanonical_nonterminal"
                else -> "legacy_incomplete"
            }
            return CanonicalSessionHeaderV1Result.Legacy(session.status, timelineStatus)
        }

        val requiredCommonPresent = session.timelineVersion != null &&
            session.lastDurableOffsetMs != null &&
            session.lastMutationSequence != null &&
            session.displayMetadataContractVersion != null &&
            session.sessionDisplayMetadataJson != null
        if (!requiredCommonPresent) {
            return CanonicalSessionHeaderV1Result.Invalid("invalid_partial_canonical_header")
        }
        val durableOffset = session.lastDurableOffsetMs
        val mutationSequence = session.lastMutationSequence
        if (
            session.timelineVersion != 1 || durableOffset < 0 || mutationSequence < 0 ||
            session.displayMetadataContractVersion != 1 ||
            CanonicalStorageJsonV1Validators.validateSessionDisplayMetadata(
                session.sessionDisplayMetadataJson
            ) != CanonicalValidationResult.Valid
        ) {
            return invalidCanonicalHeader()
        }
        val tuple = CanonicalTuple(durableOffset, mutationSequence)
        return when (session.status) {
            "active", "paused" -> if (
                session.trustedEndOffsetMs == null && session.terminalReason == null
            ) {
                CanonicalSessionHeaderV1Result.CanonicalRunning(tuple)
            } else {
                invalidCanonicalHeader()
            }

            "completed" -> if (
                session.trustedEndOffsetMs == durableOffset && session.terminalReason == "completed"
            ) {
                CanonicalSessionHeaderV1Result.CanonicalTerminal(tuple)
            } else {
                invalidCanonicalHeader()
            }

            "abandoned" -> if (
                session.trustedEndOffsetMs == durableOffset &&
                session.terminalReason in ABANDONED_REASONS
            ) {
                CanonicalSessionHeaderV1Result.CanonicalTerminal(tuple)
            } else {
                invalidCanonicalHeader()
            }

            else -> invalidCanonicalHeader()
        }
    }

    private fun invalidCanonicalHeader() =
        CanonicalSessionHeaderV1Result.Invalid("invalid_canonical_header_v1")

    private val SESSION_STATUSES = setOf("ready", "active", "paused", "completed", "abandoned")
    private val ABANDONED_REASONS = setOf(
        "user_abandoned",
        "owner_cleared",
        "process_interrupted"
    )
}

object RecordingHeaderV1Validator {
    fun validate(recording: HeartRateRecordingEntity): CanonicalValidationResult {
        if (
            recording.recordingId.isEmpty() || recording.sessionId.isEmpty() ||
            recording.startedOffsetMs < 0 || recording.startedMutationSequence < 0 ||
            recording.sourceContractVersion != 1 || recording.sourceKind != "ble_hrs" ||
            recording.acquisitionContractVersion != 1 ||
            recording.parameterSnapshotVersion != 1 ||
            recording.age?.let { age -> age !in 1..130 } == true ||
            recording.personalMaxBpm?.let { bpm -> bpm !in 30..260 } == true ||
            recording.effectiveMaxBpm?.let { bpm -> bpm !in 30..260 } == true ||
            recording.alertThresholdBpm?.let { bpm -> bpm !in 30..260 } == true
        ) {
            return invalidRecording()
        }
        val start = CanonicalTuple(recording.startedOffsetMs, recording.startedMutationSequence)
        val lifecycleValid = when (recording.status) {
            "active" -> recording.endedOffsetMs == null &&
                recording.endedMutationSequence == null &&
                recording.originalAnalysisVersion == null

            "terminal" -> {
                val endOffset = recording.endedOffsetMs
                val endMutation = recording.endedMutationSequence
                endOffset != null && endMutation != null &&
                    CanonicalTuple(endOffset, endMutation) > start &&
                    recording.originalAnalysisVersion == 1
            }

            else -> false
        }
        if (!lifecycleValid || !validateParameters(recording)) return invalidRecording()
        return CanonicalValidationResult.Valid
    }

    private fun validateParameters(recording: HeartRateRecordingEntity): Boolean {
        val effective = recording.effectiveMaxBpm
        val source = recording.effectiveMaxSource
        val zone = recording.zoneSnapshotJson
        return when {
            effective == null && source == null && zone == null ->
                recording.age == null && recording.personalMaxBpm == null

            source == "personal_max" && effective != null && zone != null ->
                recording.personalMaxBpm == effective &&
                    CanonicalStorageJsonV1Validators.validateZoneSnapshot(
                        zone,
                        effective,
                        source
                    ) == CanonicalValidationResult.Valid

            source == "age_220_minus_age" && effective != null && zone != null ->
                recording.personalMaxBpm == null && recording.age != null &&
                    effective == 220 - recording.age &&
                    CanonicalStorageJsonV1Validators.validateZoneSnapshot(
                        zone,
                        effective,
                        source
                    ) == CanonicalValidationResult.Valid

            else -> false
        }
    }

    private fun invalidRecording() =
        CanonicalValidationResult.Invalid("invalid_recording_header_v1")
}

object AcquisitionV1Validator {
    fun validate(interval: HeartRateAcquisitionIntervalEntity): CanonicalValidationResult {
        if (
            interval.id.isEmpty() || interval.recordingId.isEmpty() || interval.sequence < 0 ||
            interval.startOffsetMs < 0 || interval.startMutationSequence < 0 ||
            !validCanonicalIntervalEnd(
                interval.startOffsetMs,
                interval.startMutationSequence,
                interval.endOffsetMs,
                interval.endMutationSequence,
                interval.openMarker
            ) || !validIntent(interval) || !validDevicePair(interval)
        ) {
            return CanonicalValidationResult.Invalid("invalid_acquisition_v1")
        }
        return CanonicalValidationResult.Valid
    }

    private fun validIntent(interval: HeartRateAcquisitionIntervalEntity): Boolean = when (
        interval.recordingIntent
    ) {
        "expected_recording" -> interval.intentReason == null
        "user_excluded" -> interval.intentReason in USER_EXCLUSION_REASONS
        else -> false
    }

    private fun validDevicePair(interval: HeartRateAcquisitionIntervalEntity): Boolean {
        val allowedReasons = DEVICE_REASON_MATRIX[interval.deviceState] ?: return false
        return interval.deviceReason == null || interval.deviceReason in allowedReasons
    }

    private val USER_EXCLUSION_REASONS = setOf(
        "user_turned_off",
        "user_opted_out",
        "user_disconnected_suppress_recovery"
    )
    private val DEVICE_REASON_MATRIX = mapOf(
        "not_observing" to emptySet(),
        "no_source_selected" to setOf("source_not_selected"),
        "permission_required" to setOf("permission_missing", "permission_revoked"),
        "bluetooth_unavailable" to setOf("bluetooth_off", "platform_unavailable"),
        "searching" to setOf("initial_acquisition", "automatic_recovery"),
        "connecting" to setOf("initial_acquisition", "automatic_recovery"),
        "waiting_first_sample" to setOf("initial_acquisition", "automatic_recovery"),
        "live" to emptySet(),
        "stale" to setOf("first_sample_timeout", "sample_stale_timeout"),
        "reconnecting" to setOf("automatic_recovery", "unexpected_disconnect"),
        "disconnected" to setOf(
            "source_unavailable",
            "unexpected_disconnect",
            "connection_timeout"
        ),
        "technical_failure" to setOf("measurement_stream_unavailable", "platform_failure")
    )
}

data class CanonicalSessionGraphV1(
    val session: WorkoutSessionEntity,
    val phases: List<WorkoutPhaseIntervalEntity> = emptyList(),
    val recording: HeartRateRecordingEntity? = null,
    val acquisitions: List<HeartRateAcquisitionIntervalEntity> = emptyList(),
    val samples: List<HeartRateSampleEntity> = emptyList(),
    val snapshots: List<HeartRateAnalysisSnapshotEntity> = emptyList()
)

object CanonicalSessionGraphV1Validator {
    fun validate(graph: CanonicalSessionGraphV1): CanonicalValidationResult {
        return when (val header = CanonicalSessionHeaderV1Validator.validate(graph.session)) {
            is CanonicalSessionHeaderV1Result.Legacy -> validateLegacyGraph(graph)
            is CanonicalSessionHeaderV1Result.CanonicalRunning ->
                validateCanonicalGraph(graph, header.durableTuple, terminal = false)

            is CanonicalSessionHeaderV1Result.CanonicalTerminal ->
                validateCanonicalGraph(graph, header.finalTuple, terminal = true)

            is CanonicalSessionHeaderV1Result.Invalid ->
                CanonicalValidationResult.Invalid(header.code)
        }
    }

    private fun validateLegacyGraph(graph: CanonicalSessionGraphV1): CanonicalValidationResult =
        if (
            graph.phases.isEmpty() && graph.recording == null && graph.acquisitions.isEmpty() &&
            graph.samples.isEmpty() && graph.snapshots.isEmpty()
        ) {
            CanonicalValidationResult.Valid
        } else {
            CanonicalValidationResult.Invalid("legacy_session_has_canonical_children")
        }

    private fun validateCanonicalGraph(
        graph: CanonicalSessionGraphV1,
        inputCut: CanonicalTuple,
        terminal: Boolean
    ): CanonicalValidationResult {
        if (!validatePhasePartition(graph, inputCut, terminal)) return invalidGraph()
        val recording = graph.recording
        if (recording == null) {
            return if (
                graph.acquisitions.isEmpty() && graph.samples.isEmpty() && graph.snapshots.isEmpty()
            ) {
                CanonicalValidationResult.Valid
            } else {
                invalidGraph()
            }
        }
        if (
            recording.sessionId != graph.session.id ||
            RecordingHeaderV1Validator.validate(recording) != CanonicalValidationResult.Valid ||
            (terminal && recording.status != "terminal") ||
            (!terminal && recording.status != "active")
        ) {
            return invalidGraph()
        }
        val recordingStart = CanonicalTuple(
            recording.startedOffsetMs,
            recording.startedMutationSequence
        )
        if (
            recordingStart < CanonicalTuple(0, 0) || recordingStart > inputCut ||
            recording.startedMutationSequence > inputCut.mutationSequence
        ) {
            return invalidGraph()
        }
        if (terminal) {
            val recordingEnd = CanonicalTuple(
                recording.endedOffsetMs ?: return invalidGraph(),
                recording.endedMutationSequence ?: return invalidGraph()
            )
            if (recordingEnd != inputCut) return invalidGraph()
        }
        if (!validateAcquisitionPartition(graph, recording, recordingStart, inputCut, terminal)) {
            return invalidGraph()
        }
        if (!validateCanonicalSamples(graph.samples, recording.recordingId, recordingStart, inputCut)) {
            return invalidGraph()
        }
        if (!validateSnapshotBinding(graph, recording, terminal)) return invalidGraph()
        return CanonicalValidationResult.Valid
    }

    private fun validatePhasePartition(
        graph: CanonicalSessionGraphV1,
        inputCut: CanonicalTuple,
        terminal: Boolean
    ): Boolean {
        if (graph.phases.isEmpty()) return false
        val mode = WorkoutMode.entries.firstOrNull { value ->
            value.contractValue == graph.session.mode
        } ?: return false
        val context = PhaseIdentityV1Validator.prepareContext(
            persistedJson = graph.session.planSnapshotJson,
            mode = mode
        ) ?: return false
        graph.phases.forEachIndexed { index, phase ->
            if (
                phase.sessionId != graph.session.id || phase.sequence != index ||
                phase.startOffsetMs < 0 || phase.startMutationSequence < 0 ||
                phase.startMutationSequence > inputCut.mutationSequence ||
                PhaseIdentityV1Validator.validatePrepared(
                    phase.phaseIdentityJson,
                    context = context,
                    expectedPhaseKind = phase.phaseKind
                ) !=
                CanonicalValidationResult.Valid ||
                !validCanonicalIntervalEnd(
                    phase.startOffsetMs,
                    phase.startMutationSequence,
                    phase.endOffsetMs,
                    phase.endMutationSequence,
                    phase.openMarker
                ) || CanonicalTuple(phase.startOffsetMs, phase.startMutationSequence) > inputCut ||
                phase.endMutationSequence?.let { sequence ->
                    sequence > inputCut.mutationSequence
                } == true
            ) {
                return false
            }
            if (index == 0 && phase.startOffsetMs != 0L) return false
            if (index > 0) {
                val previous = graph.phases[index - 1]
                if (
                    previous.endOffsetMs != phase.startOffsetMs ||
                    previous.endMutationSequence != phase.startMutationSequence
                ) {
                    return false
                }
            }
            val isLast = index == graph.phases.lastIndex
            if (terminal && phase.openMarker != null) return false
            if (!terminal && (isLast != (phase.openMarker == 1))) return false
            if (phase.openMarker != null && !isLast) return false
        }
        val last = graph.phases.last()
        return if (terminal) {
            last.endOffsetMs == inputCut.offsetMs &&
                last.endMutationSequence == inputCut.mutationSequence
        } else {
            last.endOffsetMs == null && last.endMutationSequence == null && last.openMarker == 1
        }
    }

    private fun validateAcquisitionPartition(
        graph: CanonicalSessionGraphV1,
        recording: HeartRateRecordingEntity,
        recordingStart: CanonicalTuple,
        inputCut: CanonicalTuple,
        terminal: Boolean
    ): Boolean {
        if (graph.acquisitions.isEmpty()) return false
        graph.acquisitions.forEachIndexed { index, acquisition ->
            if (
                acquisition.recordingId != recording.recordingId || acquisition.sequence != index ||
                AcquisitionV1Validator.validate(acquisition) != CanonicalValidationResult.Valid ||
                acquisition.startMutationSequence > inputCut.mutationSequence ||
                CanonicalTuple(acquisition.startOffsetMs, acquisition.startMutationSequence) > inputCut ||
                acquisition.endMutationSequence?.let { sequence ->
                    sequence > inputCut.mutationSequence
                } == true
            ) {
                return false
            }
            if (index == 0 && CanonicalTuple(
                    acquisition.startOffsetMs,
                    acquisition.startMutationSequence
                ) != recordingStart
            ) {
                return false
            }
            if (index > 0) {
                val previous = graph.acquisitions[index - 1]
                if (
                    previous.endOffsetMs != acquisition.startOffsetMs ||
                    previous.endMutationSequence != acquisition.startMutationSequence
                ) {
                    return false
                }
            }
            val isLast = index == graph.acquisitions.lastIndex
            if (terminal && acquisition.openMarker != null) return false
            if (!terminal && (isLast != (acquisition.openMarker == 1))) return false
            if (acquisition.openMarker != null && !isLast) return false
        }
        val last = graph.acquisitions.last()
        return if (terminal) {
            last.endOffsetMs == inputCut.offsetMs &&
                last.endMutationSequence == inputCut.mutationSequence
        } else {
            last.endOffsetMs == null && last.endMutationSequence == null && last.openMarker == 1
        }
    }

    internal fun validateCanonicalSamples(
        samples: List<HeartRateSampleEntity>,
        recordingId: String,
        recordingStart: CanonicalTuple,
        inputCut: CanonicalTuple
    ): Boolean {
        val sampleSequences = mutableSetOf<Long>()
        return samples.all { sample ->
            val tuple = CanonicalTuple(sample.offsetMs, sample.mutationSequence)
            sample.recordingId == recordingId && sample.sampleSequence >= 0 &&
                sampleSequences.add(sample.sampleSequence) && sample.bpm in 1..65535 &&
                tuple >= recordingStart && tuple <= inputCut &&
                sample.mutationSequence <= inputCut.mutationSequence
        }
    }

    private fun validateSnapshotBinding(
        graph: CanonicalSessionGraphV1,
        recording: HeartRateRecordingEntity,
        terminal: Boolean
    ): Boolean {
        if (!terminal) return graph.snapshots.isEmpty() && recording.originalAnalysisVersion == null
        if (recording.originalAnalysisVersion != 1 || graph.snapshots.size != 1) return false
        val snapshot = graph.snapshots.single()
        return snapshot.recordingId == recording.recordingId && snapshot.analysisVersion == 1 &&
            snapshot.inputLastMutationSequence == graph.session.lastMutationSequence &&
            snapshot.inputLastMutationSequence == recording.endedMutationSequence &&
            validateSnapshotStructure(graph, recording, snapshot)
    }

    private fun validateSnapshotStructure(
        graph: CanonicalSessionGraphV1,
        recording: HeartRateRecordingEntity,
        snapshot: HeartRateAnalysisSnapshotEntity
    ): Boolean {
        if (
            snapshot.createdAt.isEmpty() || snapshot.inputLastMutationSequence < 0 ||
            snapshot.sampleStatus !in SAMPLE_STATUSES ||
            snapshot.coverageStatus !in COVERAGE_STATUSES ||
            snapshot.zoneStatus !in ZONE_STATUSES ||
            snapshot.canonicalSampleCount < 0 || snapshot.primaryPointSampleCount < 0 ||
            snapshot.primaryPointSampleCount > snapshot.canonicalSampleCount ||
            !validateSampleStatusCounts(snapshot) ||
            snapshot.eligibleDurationMs == null || snapshot.eligibleDurationMs < 0 ||
            snapshot.coveredDurationMs == null || snapshot.coveredDurationMs < 0 ||
            snapshot.coveredDurationMs > snapshot.eligibleDurationMs
        ) {
            return false
        }
        val structuresValid = CanonicalStorageJsonV1Validators.validateAnalysisConfig(
            snapshot.analysisConfigJson
        ) ==
            CanonicalValidationResult.Valid &&
            (snapshot.zoneDurationsJson == null ||
                CanonicalStorageJsonV1Validators.validateZoneDurations(snapshot.zoneDurationsJson) ==
                CanonicalValidationResult.Valid) &&
            CanonicalStorageJsonV1Validators.validatePhaseAggregates(snapshot.phaseAggregatesJson) ==
            CanonicalValidationResult.Valid &&
            CanonicalStorageJsonV1Validators.validateDurationBreakdown(snapshot.durationBreakdownJson) ==
            CanonicalValidationResult.Valid &&
            CanonicalStorageJsonV1Validators.validateQualityReasons(snapshot.qualityReasonsJson) ==
            CanonicalValidationResult.Valid
        return structuresValid && validateAnalysisBinding(graph, recording, snapshot)
    }

    private fun validateAnalysisBinding(
        graph: CanonicalSessionGraphV1,
        recording: HeartRateRecordingEntity,
        snapshot: HeartRateAnalysisSnapshotEntity
    ): Boolean = try {
        val finalOffset = graph.session.trustedEndOffsetMs ?: return false
        val orderedSamples = graph.samples.sortedWith(
            compareBy<HeartRateSampleEntity> { sample -> sample.offsetMs }
                .thenBy { sample -> sample.mutationSequence }
                .thenBy { sample -> sample.sampleSequence }
        )
        val whole = analysisMetrics(
            orderedSamples = orderedSamples,
            phases = graph.phases,
            acquisitions = graph.acquisitions,
            phaseSequence = null,
            recordingStart = CanonicalTuple(
                recording.startedOffsetMs,
                recording.startedMutationSequence
            ),
            finalTuple = CanonicalTuple(
                finalOffset,
                graph.session.lastMutationSequence ?: return false
            )
        ) ?: return false
        if (
            snapshot.canonicalSampleCount != graph.samples.size.toLong() ||
            snapshot.primaryPointSampleCount != whole.primarySamples.size.toLong() ||
            snapshot.sampleStatus != expectedSampleStatus(
                graph.samples.size.toLong(),
                whole.primarySamples.size.toLong()
            ) || snapshot.eligibleDurationMs != whole.eligibleDurationMs ||
            snapshot.coveredDurationMs != whole.coveredDurationMs ||
            snapshot.coverageBasisPoints != whole.coverageBasisPoints ||
            snapshot.coverageStatus != whole.coverageStatus ||
            snapshot.weightedBpmMs != whole.weightedBpmMs ||
            snapshot.observedAvgBpm != whole.observedAvgBpm ||
            !matchesAnchor(snapshot, whole.maximumSample)
        ) {
            return false
        }
        validateZoneBinding(snapshot, recording, whole) &&
            validatePhaseAggregateBinding(
                snapshot.phaseAggregatesJson,
                graph,
                recording,
                orderedSamples,
                finalOffset
            ) &&
            validateDurationBinding(snapshot.durationBreakdownJson, graph, recording, whole, finalOffset)
    } catch (_: ArithmeticException) {
        false
    }

    private fun analysisMetrics(
        orderedSamples: List<HeartRateSampleEntity>,
        phases: List<WorkoutPhaseIntervalEntity>,
        acquisitions: List<HeartRateAcquisitionIntervalEntity>,
        phaseSequence: Int?,
        recordingStart: CanonicalTuple,
        finalTuple: CanonicalTuple
    ): AnalysisMetrics? {
        val eligibleSegments = buildList {
            phases.filter { phase ->
                phase.phaseKind in PRIMARY_PHASE_KINDS &&
                    (phaseSequence == null || phase.sequence == phaseSequence)
            }.forEach { phase ->
                val phaseEnd = CanonicalTuple(
                    phase.endOffsetMs ?: return null,
                    phase.endMutationSequence ?: return null
                )
                acquisitions.filter { interval -> interval.recordingIntent == "expected_recording" }
                    .forEach { acquisition ->
                        val acquisitionEnd = CanonicalTuple(
                            acquisition.endOffsetMs ?: return null,
                            acquisition.endMutationSequence ?: return null
                        )
                        val start = maxOf(
                            CanonicalTuple(phase.startOffsetMs, phase.startMutationSequence),
                            CanonicalTuple(acquisition.startOffsetMs, acquisition.startMutationSequence),
                            recordingStart
                        )
                        val end = minOf(phaseEnd, acquisitionEnd, finalTuple)
                        if (end > start) add(AnalysisSegment(start, end))
                    }
            }
        }
        val eligibleDuration = eligibleSegments.fold(0L) { total, segment ->
            Math.addExact(total, segment.end.offsetMs - segment.start.offsetMs)
        }
        val primarySampleIndexes = orderedSamples.indices.filter { index ->
            val sample = orderedSamples[index]
            val sampleTuple = CanonicalTuple(sample.offsetMs, sample.mutationSequence)
            eligibleSegments.any { segment ->
                sampleTuple >= segment.start && sampleTuple < segment.end
            }
        }
        val primarySamples = primarySampleIndexes.map(orderedSamples::get)
        var coveredDuration = 0L
        var weighted = 0L
        primarySampleIndexes.forEach { globalIndex ->
            val sample = orderedSamples[globalIndex]
            val sampleTuple = CanonicalTuple(sample.offsetMs, sample.mutationSequence)
            val segment = eligibleSegments.singleOrNull { candidate ->
                sampleTuple >= candidate.start && sampleTuple < candidate.end
            } ?: return@forEach
            val nextOffset = orderedSamples.getOrNull(globalIndex + 1)?.offsetMs ?: Long.MAX_VALUE
            val cappedEnd = Math.addExact(sample.offsetMs, SAMPLE_VALIDITY_CAP_MS)
            val contributionEnd = minOf(
                nextOffset,
                cappedEnd,
                segment.end.offsetMs,
                finalTuple.offsetMs
            )
            val duration = (contributionEnd - sample.offsetMs).coerceAtLeast(0)
            coveredDuration = Math.addExact(coveredDuration, duration)
            weighted = Math.addExact(weighted, Math.multiplyExact(sample.bpm.toLong(), duration))
        }
        val basis = if (eligibleDuration == 0L) {
            null
        } else {
            Math.multiplyExact(coveredDuration, 10_000L).floorDiv(eligibleDuration).toInt()
        }
        val coverageStatus = when {
            eligibleDuration == 0L -> "no_eligible_duration"
            meetsThreshold(coveredDuration, eligibleDuration, 8_000L) -> "normal"
            meetsThreshold(coveredDuration, eligibleDuration, 5_000L) -> "partial"
            else -> "insufficient"
        }
        val average = if (coveredDuration == 0L) {
            null
        } else {
            val quotient = weighted / coveredDuration
            val remainder = weighted % coveredDuration
            (quotient + if (remainder >= coveredDuration / 2 + coveredDuration % 2) 1 else 0).toInt()
        }
        val maximum = primarySamples.maxWithOrNull(
            compareBy<HeartRateSampleEntity> { sample -> sample.bpm }
                .thenByDescending { sample -> sample.offsetMs }
                .thenByDescending { sample -> sample.mutationSequence }
                .thenByDescending { sample -> sample.sampleSequence }
        )
        return AnalysisMetrics(
            eligibleDuration,
            coveredDuration,
            basis,
            coverageStatus,
            if (coveredDuration == 0L) null else weighted,
            average,
            primarySamples,
            maximum,
            eligibleSegments,
            orderedSamples
        )
    }

    private fun meetsThreshold(covered: Long, eligible: Long, basisPoints: Long): Boolean =
        Math.multiplyExact(covered, 10_000L) >= Math.multiplyExact(eligible, basisPoints)

    private fun expectedSampleStatus(canonicalCount: Long, primaryCount: Long): String = when {
        canonicalCount == 0L -> "no_canonical_samples"
        primaryCount == 0L -> "canonical_only_excluded"
        else -> "primary_points_available"
    }

    private fun matchesAnchor(
        snapshot: HeartRateAnalysisSnapshotEntity,
        sample: HeartRateSampleEntity?
    ): Boolean = if (sample == null) {
        snapshot.observedMaxBpm == null && snapshot.highestOffsetMs == null &&
            snapshot.highestMutationSequence == null && snapshot.highestSampleSequence == null
    } else {
        snapshot.observedMaxBpm == sample.bpm && snapshot.highestOffsetMs == sample.offsetMs &&
            snapshot.highestMutationSequence == sample.mutationSequence &&
            snapshot.highestSampleSequence == sample.sampleSequence
    }

    private fun validateZoneBinding(
        snapshot: HeartRateAnalysisSnapshotEntity,
        recording: HeartRateRecordingEntity,
        metrics: AnalysisMetrics
    ): Boolean {
        val effectiveMax = recording.effectiveMaxBpm
        if (effectiveMax == null) {
            return snapshot.zoneStatus == "unavailable_no_effective_max" &&
                snapshot.zoneDurationsJson == null
        }
        if (snapshot.zoneStatus != "available") return false
        if (metrics.eligibleDurationMs == 0L) return snapshot.zoneDurationsJson == null
        val json = snapshot.zoneDurationsJson ?: return false
        val root = parseCanonicalJson(json) as? CanonicalJsonValue.Obj ?: return false
        val expected = LongArray(6)
        metrics.primarySamples.forEach { sample ->
            val sampleTuple = CanonicalTuple(sample.offsetMs, sample.mutationSequence)
            val segment = metrics.eligibleSegments.singleOrNull { candidate ->
                sampleTuple >= candidate.start && sampleTuple < candidate.end
            } ?: return@forEach
            val index = metrics.orderedSamples.indexOf(sample)
            val nextOffset = metrics.orderedSamples.getOrNull(index + 1)?.offsetMs ?: Long.MAX_VALUE
            val end = minOf(
                nextOffset,
                Math.addExact(sample.offsetMs, SAMPLE_VALIDITY_CAP_MS),
                segment.end.offsetMs
            )
            val duration = (end - sample.offsetMs).coerceAtLeast(0)
            val scaled = Math.multiplyExact(sample.bpm.toLong(), 10_000L)
            val zoneIndex = when {
                scaled < effectiveMax * 5_000L -> 0
                scaled < effectiveMax * 6_000L -> 1
                scaled < effectiveMax * 7_000L -> 2
                scaled < effectiveMax * 8_000L -> 3
                scaled < effectiveMax * 9_000L -> 4
                else -> 5
            }
            expected[zoneIndex] = Math.addExact(expected[zoneIndex], duration)
        }
        val checkedZoneTotal = expected.fold(0L, Math::addExact)
        return ZONE_DURATION_FIELD_NAMES.map { key -> root.jsonLong(key) } ==
            expected.toList() && checkedZoneTotal == metrics.coveredDurationMs
    }

    private fun validatePhaseAggregateBinding(
        json: String,
        graph: CanonicalSessionGraphV1,
        recording: HeartRateRecordingEntity,
        orderedSamples: List<HeartRateSampleEntity>,
        finalOffset: Long
    ): Boolean {
        val root = parseCanonicalJson(json) as? CanonicalJsonValue.Obj ?: return false
        val entries = root.jsonArray("aggregates") ?: return false
        val primaryPhases = graph.phases.filter { phase -> phase.phaseKind in PRIMARY_PHASE_KINDS }
        if (entries.size != primaryPhases.size) return false
        return entries.zip(primaryPhases).all { (value, phase) ->
            val entry = value as? CanonicalJsonValue.Obj ?: return@all false
            val metrics = analysisMetrics(
                orderedSamples,
                graph.phases,
                graph.acquisitions,
                phase.sequence,
                CanonicalTuple(recording.startedOffsetMs, recording.startedMutationSequence),
                CanonicalTuple(
                    finalOffset,
                    graph.session.lastMutationSequence ?: return false
                )
            ) ?: return@all false
            entry.jsonLong("phaseSequence") == phase.sequence.toLong() &&
                entry.jsonString("phaseKind") == phase.phaseKind &&
                entry.jsonLong("eligibleDurationMs") == metrics.eligibleDurationMs &&
                entry.jsonLong("coveredDurationMs") == metrics.coveredDurationMs &&
                entry.jsonLong("coverageBasisPoints") == metrics.coverageBasisPoints?.toLong() &&
                entry.jsonString("coverageStatus") == metrics.coverageStatus &&
                entry.jsonBoolean("conclusionEligible") == (
                    metrics.eligibleDurationMs > 0 && meetsThreshold(
                        metrics.coveredDurationMs,
                        metrics.eligibleDurationMs,
                        7_000L
                    )
                    ) && entry.jsonLong("weightedBpmMs") == metrics.weightedBpmMs &&
                entry.jsonLong("observedAvgBpm") == metrics.observedAvgBpm?.toLong() &&
                matchesAggregateAnchor(entry, metrics.maximumSample)
        }
    }

    private fun matchesAggregateAnchor(
        entry: CanonicalJsonValue.Obj,
        sample: HeartRateSampleEntity?
    ): Boolean = if (sample == null) {
        listOf(
            "observedMaxBpm",
            "highestOffsetMs",
            "highestMutationSequence",
            "highestSampleSequence"
        ).all(entry::jsonNull)
    } else {
        entry.jsonLong("observedMaxBpm") == sample.bpm.toLong() &&
            entry.jsonLong("highestOffsetMs") == sample.offsetMs &&
            entry.jsonLong("highestMutationSequence") == sample.mutationSequence &&
            entry.jsonLong("highestSampleSequence") == sample.sampleSequence
    }

    private fun validateDurationBinding(
        json: String,
        graph: CanonicalSessionGraphV1,
        recording: HeartRateRecordingEntity,
        metrics: AnalysisMetrics,
        finalOffset: Long
    ): Boolean {
        val root = parseCanonicalJson(json) as? CanonicalJsonValue.Obj ?: return false
        val intent = root.jsonObject("intentAxis") ?: return false
        val phaseAxis = root.jsonObject("phaseAxis") ?: return false
        val primary = root.jsonObject("primaryAnalysisPartition") ?: return false
        val deviceStates = root.jsonObject("deviceStateDurations") ?: return false
        val deviceReasons = root.jsonObject("deviceReasonDurations") ?: return false
        val recordingWindow = finalOffset - recording.startedOffsetMs
        var expectedIntent = 0L
        var userExcluded = 0L
        val exclusionDurations = mutableMapOf<String, Long>().withDefault { 0L }
        val deviceStateDurations = mutableMapOf<String, Long>().withDefault { 0L }
        val deviceReasonDurations = mutableMapOf<String, Long>().withDefault { 0L }
        graph.acquisitions.forEach { acquisition ->
            val duration = (acquisition.endOffsetMs ?: return false) - acquisition.startOffsetMs
            if (acquisition.recordingIntent == "expected_recording") {
                expectedIntent = Math.addExact(expectedIntent, duration)
            } else {
                userExcluded = Math.addExact(userExcluded, duration)
                val reason = acquisition.intentReason ?: return false
                exclusionDurations[reason] = Math.addExact(exclusionDurations.getValue(reason), duration)
            }
            deviceStateDurations[acquisition.deviceState] = Math.addExact(
                deviceStateDurations.getValue(acquisition.deviceState),
                duration
            )
            acquisition.deviceReason?.let { reason ->
                deviceReasonDurations[reason] = Math.addExact(
                    deviceReasonDurations.getValue(reason),
                    duration
                )
            }
        }
        val expectedAcquisitions = graph.acquisitions.filter { acquisition ->
            acquisition.recordingIntent == "expected_recording"
        }
        var phaseExcluded = 0L
        var prepareExcluded = 0L
        var pausedExcluded = 0L
        graph.phases.filter { phase -> phase.phaseKind !in PRIMARY_PHASE_KINDS }.forEach { phase ->
            val phaseEnd = phase.endOffsetMs ?: return false
            expectedAcquisitions.forEach { acquisition ->
                val acquisitionEnd = acquisition.endOffsetMs ?: return false
                val start = maxOf(phase.startOffsetMs, acquisition.startOffsetMs, recording.startedOffsetMs)
                val end = minOf(phaseEnd, acquisitionEnd, finalOffset)
                val duration = (end - start).coerceAtLeast(0)
                phaseExcluded = Math.addExact(phaseExcluded, duration)
                if (phase.phaseKind == "strength_prepare_set") {
                    prepareExcluded = Math.addExact(prepareExcluded, duration)
                }
                if (phase.phaseKind == "paused") {
                    pausedExcluded = Math.addExact(pausedExcluded, duration)
                }
            }
        }
        return root.jsonLong("canonicalSessionDurationMs") == finalOffset &&
            root.jsonLong("recordingWindowDurationMs") == recordingWindow &&
            root.jsonLong("notRequestedBeforeRecordingStartMs") == recording.startedOffsetMs &&
            intent.jsonLong("expectedRecordingDurationMs") == expectedIntent &&
            intent.jsonLong("userExcludedDurationMs") == userExcluded &&
            intent.jsonLong("userTurnedOffDurationMs") == exclusionDurations.getValue("user_turned_off") &&
            intent.jsonLong("userOptedOutDurationMs") == exclusionDurations.getValue("user_opted_out") &&
            intent.jsonLong("userDisconnectedSuppressRecoveryDurationMs") ==
            exclusionDurations.getValue("user_disconnected_suppress_recovery") &&
            phaseAxis.jsonLong("primaryEligibleDurationMs") == metrics.eligibleDurationMs &&
            phaseAxis.jsonLong("phaseExcludedDurationMs") == phaseExcluded &&
            phaseAxis.jsonLong("strengthPrepareExcludedDurationMs") == prepareExcluded &&
            phaseAxis.jsonLong("pausedExcludedDurationMs") == pausedExcluded &&
            primary.jsonLong("primaryEligibleDurationMs") == metrics.eligibleDurationMs &&
            primary.jsonLong("eligibleCoveredDurationMs") == metrics.coveredDurationMs &&
            primary.jsonLong("eligibleUncoveredDurationMs") ==
            metrics.eligibleDurationMs - metrics.coveredDurationMs &&
            DEVICE_STATE_NAMES.all { key ->
                deviceStates.jsonLong(key) == deviceStateDurations.getValue(key)
            } && DEVICE_REASON_NAMES.all { key ->
                deviceReasons.jsonLong(key) == deviceReasonDurations.getValue(key)
            }
    }

    private fun validateSampleStatusCounts(snapshot: HeartRateAnalysisSnapshotEntity): Boolean =
        when (snapshot.sampleStatus) {
            "no_canonical_samples" ->
                snapshot.canonicalSampleCount == 0L && snapshot.primaryPointSampleCount == 0L
            "canonical_only_excluded" ->
                snapshot.canonicalSampleCount > 0L && snapshot.primaryPointSampleCount == 0L
            "primary_points_available" -> snapshot.primaryPointSampleCount > 0L
            else -> false
        }

    private fun invalidGraph() = CanonicalValidationResult.Invalid("invalid_canonical_graph_v1")

    private val SAMPLE_STATUSES = setOf(
        "no_canonical_samples",
        "canonical_only_excluded",
        "primary_points_available"
    )
    private val COVERAGE_STATUSES = setOf("no_eligible_duration", "insufficient", "partial", "normal")
    private val ZONE_STATUSES = setOf("available", "unavailable_no_effective_max")
    private val PRIMARY_PHASE_KINDS = setOf(
        "timed_work",
        "timed_rest",
        "strength_active_set",
        "strength_confirm_set",
        "strength_rest",
        "follow_along_action",
        "follow_along_rest"
    )
    private const val SAMPLE_VALIDITY_CAP_MS = 2_500L
    private val ZONE_DURATION_FIELD_NAMES = listOf(
        "below50DurationMs",
        "from50To60DurationMs",
        "from60To70DurationMs",
        "from70To80DurationMs",
        "from80To90DurationMs",
        "atOrAbove90DurationMs"
    )
    private val DEVICE_STATE_NAMES = listOf(
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
    private val DEVICE_REASON_NAMES = listOf(
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
}

private data class AnalysisSegment(
    val start: CanonicalTuple,
    val end: CanonicalTuple
)

private data class AnalysisMetrics(
    val eligibleDurationMs: Long,
    val coveredDurationMs: Long,
    val coverageBasisPoints: Int?,
    val coverageStatus: String,
    val weightedBpmMs: Long?,
    val observedAvgBpm: Int?,
    val primarySamples: List<HeartRateSampleEntity>,
    val maximumSample: HeartRateSampleEntity?,
    val eligibleSegments: List<AnalysisSegment>,
    val orderedSamples: List<HeartRateSampleEntity>
)

private fun CanonicalJsonValue.Obj.jsonLong(key: String): Long? =
    (fields[key] as? CanonicalJsonValue.Num)?.value?.let { number ->
        try {
            number.longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

private fun CanonicalJsonValue.Obj.jsonString(key: String): String? =
    (fields[key] as? CanonicalJsonValue.Str)?.value

private fun CanonicalJsonValue.Obj.jsonBoolean(key: String): Boolean? =
    (fields[key] as? CanonicalJsonValue.Bool)?.value

private fun CanonicalJsonValue.Obj.jsonObject(key: String): CanonicalJsonValue.Obj? =
    fields[key] as? CanonicalJsonValue.Obj

private fun CanonicalJsonValue.Obj.jsonArray(key: String): List<CanonicalJsonValue>? =
    (fields[key] as? CanonicalJsonValue.Arr)?.values

private fun CanonicalJsonValue.Obj.jsonNull(key: String): Boolean =
    fields[key] === CanonicalJsonValue.Null

internal fun validCanonicalIntervalEnd(
    startOffsetMs: Long,
    startMutationSequence: Long,
    endOffsetMs: Long?,
    endMutationSequence: Long?,
    openMarker: Int?
): Boolean {
    if (openMarker == 1) return endOffsetMs == null && endMutationSequence == null
    if (openMarker != null || endOffsetMs == null || endMutationSequence == null) return false
    if (endOffsetMs < 0 || endMutationSequence < 0) return false
    return CanonicalTuple(endOffsetMs, endMutationSequence) >
        CanonicalTuple(startOffsetMs, startMutationSequence)
}
