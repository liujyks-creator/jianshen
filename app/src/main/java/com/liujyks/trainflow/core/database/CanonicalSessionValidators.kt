package com.liujyks.trainflow.core.database

import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1ValidationResult
import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1Validator
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
        val snapshot = when (
            val result = PlanSnapshotStorageV1Validator.validate(graph.session.planSnapshotJson, mode)
        ) {
            is PlanSnapshotStorageV1ValidationResult.Valid -> result.storage
            else -> return false
        }
        graph.phases.forEachIndexed { index, phase ->
            if (
                phase.sessionId != graph.session.id || phase.sequence != index ||
                phase.startOffsetMs < 0 || phase.startMutationSequence < 0 ||
                phase.startMutationSequence > inputCut.mutationSequence ||
                PhaseIdentityV1Validator.validate(
                    phase.phaseIdentityJson,
                    immutableSnapshot = snapshot,
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
            validateSnapshotStructure(snapshot)
    }

    private fun validateSnapshotStructure(snapshot: HeartRateAnalysisSnapshotEntity): Boolean {
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
        return CanonicalStorageJsonV1Validators.validateAnalysisConfig(snapshot.analysisConfigJson) ==
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
}

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
