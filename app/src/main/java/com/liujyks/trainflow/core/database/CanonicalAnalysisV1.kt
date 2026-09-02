package com.liujyks.trainflow.core.database

import com.liujyks.trainflow.core.data.RecorderValidationException
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import java.math.BigDecimal

internal object CanonicalAnalysisV1 {
    fun derive(
        graph: CanonicalSessionGraphV1,
        snapshotCreatedAt: String
    ): HeartRateAnalysisSnapshotEntity {
        val recording = graph.recording
            ?: throw RecorderValidationException("invalid_analysis_snapshot_v1")
        val finalOffset = graph.session.trustedEndOffsetMs
            ?: throw RecorderValidationException("invalid_analysis_snapshot_v1")
        val finalSequence = graph.session.lastMutationSequence
            ?: throw RecorderValidationException("invalid_analysis_snapshot_v1")
        if (
            recording.status != "terminal" || recording.endedOffsetMs != finalOffset ||
            recording.endedMutationSequence != finalSequence || snapshotCreatedAt.isEmpty()
        ) {
            throw RecorderValidationException("invalid_analysis_snapshot_v1")
        }

        val orderedPhases = graph.phases.sortedBy { phase -> phase.sequence }
        val orderedAcquisitions = graph.acquisitions.sortedBy { interval -> interval.sequence }
        val orderedSamples = graph.samples.sortedWith(SAMPLE_ORDER)
        val primaryPhases = orderedPhases.filter { phase -> phase.phaseKind in PRIMARY_PHASE_KINDS }
        val phaseMetricIndex = IntArray(orderedPhases.size) { -1 }
        primaryPhases.forEachIndexed { index, phase -> phaseMetricIndex[phase.sequence] = index }

        val eligibleByPhase = LongArray(primaryPhases.size)
        val coveredByPhase = LongArray(primaryPhases.size)
        val weightedByPhase = LongArray(primaryPhases.size)
        val pointCountByPhase = LongArray(primaryPhases.size)
        val maximumByPhase = arrayOfNulls<HeartRateSampleEntity>(primaryPhases.size)
        val strengthExcludedByPhase = LongArray(orderedPhases.size)
        val pausedExcludedByPhase = LongArray(orderedPhases.size)
        val userTurnedOffByPhase = LongArray(orderedPhases.size)
        val userOptedOutByPhase = LongArray(orderedPhases.size)
        val userDisconnectedByPhase = LongArray(orderedPhases.size)
        val eligibleSegments = mutableListOf<EligibleSegment>()

        var phaseExcludedDuration = 0L
        var phaseIndex = 0
        var acquisitionIndex = 0
        val recordingStart = CanonicalTuple(
            recording.startedOffsetMs,
            recording.startedMutationSequence
        )
        val finalTuple = CanonicalTuple(finalOffset, finalSequence)
        while (phaseIndex < orderedPhases.size && acquisitionIndex < orderedAcquisitions.size) {
            val phase = orderedPhases[phaseIndex]
            val acquisition = orderedAcquisitions[acquisitionIndex]
            val phaseStart = CanonicalTuple(phase.startOffsetMs, phase.startMutationSequence)
            val phaseEnd = phase.endTuple()
            val acquisitionStart = CanonicalTuple(
                acquisition.startOffsetMs,
                acquisition.startMutationSequence
            )
            val acquisitionEnd = acquisition.endTuple()
            val start = maxOf(phaseStart, acquisitionStart, recordingStart)
            val end = minOf(phaseEnd, acquisitionEnd, finalTuple)
            if (end > start) {
                val duration = nonNegativeDifference(end.offsetMs, start.offsetMs)
                val metricIndex = phaseMetricIndex.getOrElse(phase.sequence) { -1 }
                if (acquisition.recordingIntent == "expected_recording") {
                    if (metricIndex >= 0) {
                        eligibleByPhase[metricIndex] = Math.addExact(
                            eligibleByPhase[metricIndex],
                            duration
                        )
                        eligibleSegments += EligibleSegment(start, end, metricIndex)
                    } else {
                        phaseExcludedDuration = Math.addExact(phaseExcludedDuration, duration)
                        when (phase.phaseKind) {
                            "strength_prepare_set" -> strengthExcludedByPhase[phase.sequence] =
                                Math.addExact(strengthExcludedByPhase[phase.sequence], duration)

                            "paused" -> pausedExcludedByPhase[phase.sequence] =
                                Math.addExact(pausedExcludedByPhase[phase.sequence], duration)
                        }
                    }
                } else {
                    when (acquisition.intentReason) {
                        "user_turned_off" -> userTurnedOffByPhase[phase.sequence] =
                            Math.addExact(userTurnedOffByPhase[phase.sequence], duration)

                        "user_opted_out" -> userOptedOutByPhase[phase.sequence] =
                            Math.addExact(userOptedOutByPhase[phase.sequence], duration)

                        "user_disconnected_suppress_recovery" ->
                            userDisconnectedByPhase[phase.sequence] = Math.addExact(
                                userDisconnectedByPhase[phase.sequence],
                                duration
                            )
                    }
                }
            }
            when {
                phaseEnd < acquisitionEnd -> phaseIndex += 1
                acquisitionEnd < phaseEnd -> acquisitionIndex += 1
                else -> {
                    phaseIndex += 1
                    acquisitionIndex += 1
                }
            }
        }

        val eligibleDuration = eligibleByPhase.checkedSum()
        val zoneDurations = LongArray(ZONE_FIELD_NAMES.size)
        var segmentIndex = 0
        var primaryPointCount = 0L
        var coveredDuration = 0L
        var weightedBpmMs = 0L
        var maximumSample: HeartRateSampleEntity? = null
        orderedSamples.forEachIndexed { sampleIndex, sample ->
            val tuple = CanonicalTuple(sample.offsetMs, sample.mutationSequence)
            while (
                segmentIndex < eligibleSegments.size &&
                tuple >= eligibleSegments[segmentIndex].end
            ) {
                segmentIndex += 1
            }
            val segment = eligibleSegments.getOrNull(segmentIndex)
            if (segment == null || tuple < segment.start || tuple >= segment.end) return@forEachIndexed

            primaryPointCount = Math.addExact(primaryPointCount, 1L)
            pointCountByPhase[segment.phaseMetricIndex] = Math.addExact(
                pointCountByPhase[segment.phaseMetricIndex],
                1L
            )
            val currentMaximum = maximumSample
            if (currentMaximum == null || sample.bpm > currentMaximum.bpm) maximumSample = sample
            val phaseMaximum = maximumByPhase[segment.phaseMetricIndex]
            if (phaseMaximum == null || sample.bpm > phaseMaximum.bpm) {
                maximumByPhase[segment.phaseMetricIndex] = sample
            }

            val nextOffset = orderedSamples.getOrNull(sampleIndex + 1)?.offsetMs ?: Long.MAX_VALUE
            val validityEnd = Math.addExact(sample.offsetMs, SAMPLE_VALIDITY_CAP_MS)
            val contributionEnd = minOf(
                nextOffset,
                validityEnd,
                segment.end.offsetMs,
                finalOffset
            )
            val duration = nonNegativeDifference(contributionEnd, sample.offsetMs)
            coveredDuration = Math.addExact(coveredDuration, duration)
            coveredByPhase[segment.phaseMetricIndex] = Math.addExact(
                coveredByPhase[segment.phaseMetricIndex],
                duration
            )
            val integral = Math.multiplyExact(sample.bpm.toLong(), duration)
            weightedBpmMs = Math.addExact(weightedBpmMs, integral)
            weightedByPhase[segment.phaseMetricIndex] = Math.addExact(
                weightedByPhase[segment.phaseMetricIndex],
                integral
            )
            recording.effectiveMaxBpm?.let { effectiveMax ->
                val zoneIndex = zoneIndex(sample.bpm, effectiveMax)
                zoneDurations[zoneIndex] = Math.addExact(zoneDurations[zoneIndex], duration)
            }
        }

        check(coveredByPhase.checkedSum() == coveredDuration)
        check(weightedByPhase.checkedSum() == weightedBpmMs)
        val coverageBasisPoints = coverageBasisPoints(coveredDuration, eligibleDuration)
        val coverageStatus = coverageStatus(coveredDuration, eligibleDuration)
        val weighted = weightedBpmMs.takeIf { coveredDuration > 0 }
        val average = positiveHalfUpAverage(weighted, coveredDuration)
        val sampleStatus = when {
            graph.samples.isEmpty() -> "no_canonical_samples"
            primaryPointCount == 0L -> "canonical_only_excluded"
            else -> "primary_points_available"
        }
        val zoneStatus = if (recording.effectiveMaxBpm == null) {
            "unavailable_no_effective_max"
        } else {
            "available"
        }
        val zoneDurationsJson = if (recording.effectiveMaxBpm != null && eligibleDuration > 0) {
            check(zoneDurations.checkedSum() == coveredDuration)
            zoneDurationsJson(zoneDurations)
        } else {
            null
        }

        val durationAxes = durationAxes(
            recording = recording,
            finalOffset = finalOffset,
            acquisitions = orderedAcquisitions,
            eligibleDuration = eligibleDuration,
            coveredDuration = coveredDuration,
            phaseExcludedDuration = phaseExcludedDuration,
            strengthExcludedByPhase = strengthExcludedByPhase,
            pausedExcludedByPhase = pausedExcludedByPhase
        )
        val phaseAggregatesJson = phaseAggregatesJson(
            primaryPhases = primaryPhases,
            eligibleByPhase = eligibleByPhase,
            coveredByPhase = coveredByPhase,
            weightedByPhase = weightedByPhase,
            pointCountByPhase = pointCountByPhase,
            maximumByPhase = maximumByPhase
        )
        val qualityReasonsJson = qualityReasonsJson(
            graph = graph,
            sampleStatus = sampleStatus,
            zoneStatus = zoneStatus,
            eligibleDuration = eligibleDuration,
            coveredDuration = coveredDuration,
            coverageStatus = coverageStatus,
            durationAxes = durationAxes,
            primaryPhases = primaryPhases,
            eligibleByPhase = eligibleByPhase,
            coveredByPhase = coveredByPhase,
            pointCountByPhase = pointCountByPhase,
            strengthExcludedByPhase = strengthExcludedByPhase,
            pausedExcludedByPhase = pausedExcludedByPhase,
            userTurnedOffByPhase = userTurnedOffByPhase,
            userOptedOutByPhase = userOptedOutByPhase,
            userDisconnectedByPhase = userDisconnectedByPhase
        )

        return HeartRateAnalysisSnapshotEntity(
            recordingId = recording.recordingId,
            analysisVersion = ANALYSIS_VERSION,
            createdAt = snapshotCreatedAt,
            inputLastMutationSequence = finalSequence,
            sampleStatus = sampleStatus,
            coverageStatus = coverageStatus,
            zoneStatus = zoneStatus,
            canonicalSampleCount = graph.samples.size.toLong(),
            primaryPointSampleCount = primaryPointCount,
            eligibleDurationMs = eligibleDuration,
            coveredDurationMs = coveredDuration,
            coverageBasisPoints = coverageBasisPoints,
            weightedBpmMs = weighted,
            observedAvgBpm = average,
            observedMaxBpm = maximumSample?.bpm,
            highestOffsetMs = maximumSample?.offsetMs,
            highestMutationSequence = maximumSample?.mutationSequence,
            highestSampleSequence = maximumSample?.sampleSequence,
            analysisConfigJson = ANALYSIS_CONFIG_JSON,
            zoneDurationsJson = zoneDurationsJson,
            phaseAggregatesJson = phaseAggregatesJson,
            durationBreakdownJson = durationBreakdownJson(durationAxes),
            qualityReasonsJson = qualityReasonsJson
        )
    }

    private fun durationAxes(
        recording: HeartRateRecordingEntity,
        finalOffset: Long,
        acquisitions: List<HeartRateAcquisitionIntervalEntity>,
        eligibleDuration: Long,
        coveredDuration: Long,
        phaseExcludedDuration: Long,
        strengthExcludedByPhase: LongArray,
        pausedExcludedByPhase: LongArray
    ): DurationAxes {
        val intentDurations = LongArray(USER_EXCLUSION_REASONS.size)
        val deviceStateDurations = LongArray(DEVICE_STATES.size)
        val deviceReasonDurations = LongArray(DEVICE_REASONS.size)
        var expectedRecordingDuration = 0L
        var userExcludedDuration = 0L
        acquisitions.forEach { interval ->
            val duration = nonNegativeDifference(
                requireNotNull(interval.endOffsetMs),
                interval.startOffsetMs
            )
            if (interval.recordingIntent == "expected_recording") {
                expectedRecordingDuration = Math.addExact(expectedRecordingDuration, duration)
            } else {
                userExcludedDuration = Math.addExact(userExcludedDuration, duration)
                val reasonIndex = USER_EXCLUSION_REASONS.indexOf(interval.intentReason)
                if (reasonIndex < 0) throw RecorderValidationException("invalid_analysis_snapshot_v1")
                intentDurations[reasonIndex] = Math.addExact(intentDurations[reasonIndex], duration)
            }
            val stateIndex = DEVICE_STATES.indexOf(interval.deviceState)
            if (stateIndex < 0) throw RecorderValidationException("invalid_analysis_snapshot_v1")
            deviceStateDurations[stateIndex] = Math.addExact(deviceStateDurations[stateIndex], duration)
            interval.deviceReason?.let { reason ->
                val reasonIndex = DEVICE_REASONS.indexOf(reason)
                if (reasonIndex < 0) throw RecorderValidationException("invalid_analysis_snapshot_v1")
                deviceReasonDurations[reasonIndex] = Math.addExact(
                    deviceReasonDurations[reasonIndex],
                    duration
                )
            }
        }
        val recordingWindow = nonNegativeDifference(finalOffset, recording.startedOffsetMs)
        val notRequested = recording.startedOffsetMs
        val strengthExcluded = strengthExcludedByPhase.checkedSum()
        val pausedExcluded = pausedExcludedByPhase.checkedSum()
        val uncovered = nonNegativeDifference(eligibleDuration, coveredDuration)

        checkedEquals(
            recordingWindow,
            Math.addExact(expectedRecordingDuration, userExcludedDuration)
        )
        checkedEquals(userExcludedDuration, intentDurations.checkedSum())
        checkedEquals(phaseExcludedDuration, Math.addExact(strengthExcluded, pausedExcluded))
        checkedEquals(eligibleDuration, Math.addExact(coveredDuration, uncovered))
        checkedEquals(recordingWindow, deviceStateDurations.checkedSum())
        if (deviceReasonDurations.checkedSum() > recordingWindow) {
            throw RecorderValidationException("invalid_analysis_snapshot_v1")
        }
        checkedEquals(
            finalOffset,
            Math.addExact(
                notRequested,
                Math.addExact(
                    userExcludedDuration,
                    Math.addExact(phaseExcludedDuration, eligibleDuration)
                )
            )
        )
        return DurationAxes(
            canonicalSessionDuration = finalOffset,
            recordingWindowDuration = recordingWindow,
            notRequestedBeforeRecordingStart = notRequested,
            expectedRecordingDuration = expectedRecordingDuration,
            userExcludedDuration = userExcludedDuration,
            intentDurations = intentDurations,
            primaryEligibleDuration = eligibleDuration,
            phaseExcludedDuration = phaseExcludedDuration,
            strengthPrepareExcludedDuration = strengthExcluded,
            pausedExcludedDuration = pausedExcluded,
            eligibleCoveredDuration = coveredDuration,
            eligibleUncoveredDuration = uncovered,
            deviceStateDurations = deviceStateDurations,
            deviceReasonDurations = deviceReasonDurations
        )
    }

    private fun phaseAggregatesJson(
        primaryPhases: List<WorkoutPhaseIntervalEntity>,
        eligibleByPhase: LongArray,
        coveredByPhase: LongArray,
        weightedByPhase: LongArray,
        pointCountByPhase: LongArray,
        maximumByPhase: Array<HeartRateSampleEntity?>
    ): String = jsonObject(
        "phaseAggregatesContractVersion" to jsonNumber(1),
        "aggregates" to CanonicalJsonValue.Arr(primaryPhases.mapIndexed { index, phase ->
            val eligible = eligibleByPhase[index]
            val covered = coveredByPhase[index]
            val weighted = weightedByPhase[index].takeIf { covered > 0 }
            val maximum = maximumByPhase[index]
            jsonObject(
                "phaseSequence" to jsonNumber(phase.sequence.toLong()),
                "phaseKind" to CanonicalJsonValue.Str(phase.phaseKind),
                "eligibleDurationMs" to jsonNumber(eligible),
                "coveredDurationMs" to jsonNumber(covered),
                "coverageBasisPoints" to nullableNumber(coverageBasisPoints(covered, eligible)),
                "coverageStatus" to CanonicalJsonValue.Str(coverageStatus(covered, eligible)),
                "conclusionEligible" to CanonicalJsonValue.Bool(
                    eligible > 0 && meetsThreshold(covered, eligible, PHASE_CONCLUSION_BP)
                ),
                "weightedBpmMs" to nullableNumber(weighted),
                "observedAvgBpm" to nullableNumber(positiveHalfUpAverage(weighted, covered)),
                "observedMaxBpm" to nullableNumber(maximum?.bpm),
                "highestOffsetMs" to nullableNumber(maximum?.offsetMs),
                "highestMutationSequence" to nullableNumber(maximum?.mutationSequence),
                "highestSampleSequence" to nullableNumber(maximum?.sampleSequence)
            )
        })
    ).renderCanonicalJson()

    private fun durationBreakdownJson(axes: DurationAxes): String = jsonObject(
        "durationBreakdownContractVersion" to jsonNumber(1),
        "canonicalSessionDurationMs" to jsonNumber(axes.canonicalSessionDuration),
        "recordingWindowDurationMs" to jsonNumber(axes.recordingWindowDuration),
        "notRequestedBeforeRecordingStartMs" to jsonNumber(axes.notRequestedBeforeRecordingStart),
        "intentAxis" to jsonObject(
            "expectedRecordingDurationMs" to jsonNumber(axes.expectedRecordingDuration),
            "userExcludedDurationMs" to jsonNumber(axes.userExcludedDuration),
            "userTurnedOffDurationMs" to jsonNumber(axes.intentDurations[0]),
            "userOptedOutDurationMs" to jsonNumber(axes.intentDurations[1]),
            "userDisconnectedSuppressRecoveryDurationMs" to jsonNumber(axes.intentDurations[2])
        ),
        "phaseAxis" to jsonObject(
            "primaryEligibleDurationMs" to jsonNumber(axes.primaryEligibleDuration),
            "phaseExcludedDurationMs" to jsonNumber(axes.phaseExcludedDuration),
            "strengthPrepareExcludedDurationMs" to jsonNumber(axes.strengthPrepareExcludedDuration),
            "pausedExcludedDurationMs" to jsonNumber(axes.pausedExcludedDuration)
        ),
        "primaryAnalysisPartition" to jsonObject(
            "primaryEligibleDurationMs" to jsonNumber(axes.primaryEligibleDuration),
            "eligibleCoveredDurationMs" to jsonNumber(axes.eligibleCoveredDuration),
            "eligibleUncoveredDurationMs" to jsonNumber(axes.eligibleUncoveredDuration)
        ),
        "deviceStateDurations" to namedDurations(DEVICE_STATES, axes.deviceStateDurations),
        "deviceReasonDurations" to namedDurations(DEVICE_REASONS, axes.deviceReasonDurations),
        "orthogonalityContract" to jsonObject(
            "contractVersion" to jsonNumber(1),
            "rule" to CanonicalJsonValue.Str(
                "primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum"
            )
        )
    ).renderCanonicalJson()

    private fun qualityReasonsJson(
        graph: CanonicalSessionGraphV1,
        sampleStatus: String,
        zoneStatus: String,
        eligibleDuration: Long,
        coveredDuration: Long,
        coverageStatus: String,
        durationAxes: DurationAxes,
        primaryPhases: List<WorkoutPhaseIntervalEntity>,
        eligibleByPhase: LongArray,
        coveredByPhase: LongArray,
        pointCountByPhase: LongArray,
        strengthExcludedByPhase: LongArray,
        pausedExcludedByPhase: LongArray,
        userTurnedOffByPhase: LongArray,
        userOptedOutByPhase: LongArray,
        userDisconnectedByPhase: LongArray
    ): String {
        val sessionReasons = mutableListOf<CanonicalJsonValue>()
        fun sessionReason(code: String, duration: Long? = null) {
            sessionReasons += jsonObject(
                "reasonCode" to CanonicalJsonValue.Str(code),
                "durationMs" to nullableNumber(duration)
            )
        }
        if (eligibleDuration == 0L) sessionReason("no_eligible_duration")
        if (sampleStatus == "no_canonical_samples") sessionReason("no_canonical_samples")
        if (sampleStatus == "canonical_only_excluded") sessionReason("canonical_only_excluded")
        if (durationAxes.expectedRecordingDuration > 0 && durationAxes.eligibleUncoveredDuration > 0) {
            sessionReason("eligible_uncovered_present", durationAxes.eligibleUncoveredDuration)
        }
        if (coverageStatus == "insufficient") sessionReason("insufficient_coverage")
        if (coverageStatus == "partial") sessionReason("partial_coverage")
        if (zoneStatus == "unavailable_no_effective_max" && eligibleDuration > 0) {
            sessionReason("unavailable_no_effective_max")
        }
        if (durationAxes.notRequestedBeforeRecordingStart > 0) {
            sessionReason(
                "not_requested_before_recording_start",
                durationAxes.notRequestedBeforeRecordingStart
            )
        }
        if (durationAxes.strengthPrepareExcludedDuration > 0) {
            sessionReason("strength_prepare_excluded", durationAxes.strengthPrepareExcludedDuration)
        }
        if (durationAxes.pausedExcludedDuration > 0) {
            sessionReason("paused_excluded", durationAxes.pausedExcludedDuration)
        }
        if (durationAxes.intentDurations[0] > 0) {
            sessionReason("user_turned_off_excluded", durationAxes.intentDurations[0])
        }
        if (durationAxes.intentDurations[1] > 0) {
            sessionReason("user_opted_out_excluded", durationAxes.intentDurations[1])
        }
        if (durationAxes.intentDurations[2] > 0) {
            sessionReason(
                "user_disconnected_suppress_recovery_excluded",
                durationAxes.intentDurations[2]
            )
        }
        if (graph.session.terminalReason == "process_interrupted") sessionReason("process_interrupted")

        val primaryIndexBySequence = IntArray(graph.phases.size) { -1 }
        primaryPhases.forEachIndexed { index, phase -> primaryIndexBySequence[phase.sequence] = index }
        val phaseReasons = mutableListOf<CanonicalJsonValue>()
        fun phaseReason(sequence: Int, code: String, duration: Long? = null) {
            phaseReasons += jsonObject(
                "phaseSequence" to jsonNumber(sequence.toLong()),
                "reasonCode" to CanonicalJsonValue.Str(code),
                "durationMs" to nullableNumber(duration)
            )
        }
        graph.phases.sortedBy { phase -> phase.sequence }.forEach { phase ->
            val sequence = phase.sequence
            val metricIndex = primaryIndexBySequence[sequence]
            if (metricIndex >= 0) {
                val eligible = eligibleByPhase[metricIndex]
                val covered = coveredByPhase[metricIndex]
                val uncovered = nonNegativeDifference(eligible, covered)
                val status = coverageStatus(covered, eligible)
                if (eligible == 0L) phaseReason(sequence, "no_eligible_duration")
                if (eligible > 0 && pointCountByPhase[metricIndex] == 0L) {
                    phaseReason(sequence, "no_canonical_samples")
                }
                if (uncovered > 0) phaseReason(sequence, "eligible_uncovered_present", uncovered)
                if (status == "insufficient") phaseReason(sequence, "insufficient_coverage")
                if (status == "partial") phaseReason(sequence, "partial_coverage")
                if (zoneStatus == "unavailable_no_effective_max" && eligible > 0) {
                    phaseReason(sequence, "unavailable_no_effective_max")
                }
            }
            if (strengthExcludedByPhase[sequence] > 0) {
                phaseReason(sequence, "strength_prepare_excluded", strengthExcludedByPhase[sequence])
            }
            if (pausedExcludedByPhase[sequence] > 0) {
                phaseReason(sequence, "paused_excluded", pausedExcludedByPhase[sequence])
            }
            if (userTurnedOffByPhase[sequence] > 0) {
                phaseReason(sequence, "user_turned_off_excluded", userTurnedOffByPhase[sequence])
            }
            if (userOptedOutByPhase[sequence] > 0) {
                phaseReason(sequence, "user_opted_out_excluded", userOptedOutByPhase[sequence])
            }
            if (userDisconnectedByPhase[sequence] > 0) {
                phaseReason(
                    sequence,
                    "user_disconnected_suppress_recovery_excluded",
                    userDisconnectedByPhase[sequence]
                )
            }
        }
        return jsonObject(
            "qualityReasonsContractVersion" to jsonNumber(1),
            "sessionReasons" to CanonicalJsonValue.Arr(sessionReasons),
            "phaseReasons" to CanonicalJsonValue.Arr(phaseReasons)
        ).renderCanonicalJson()
    }

    private fun zoneDurationsJson(durations: LongArray): String = jsonObject(
        "zoneDurationsContractVersion" to jsonNumber(1),
        *ZONE_FIELD_NAMES.mapIndexed { index, name -> name to jsonNumber(durations[index]) }.toTypedArray()
    ).renderCanonicalJson()

    private fun zoneIndex(bpm: Int, effectiveMaxBpm: Int): Int {
        val scaled = Math.multiplyExact(bpm.toLong(), BASIS_POINT_SCALE)
        return when {
            scaled < Math.multiplyExact(effectiveMaxBpm.toLong(), 5_000L) -> 0
            scaled < Math.multiplyExact(effectiveMaxBpm.toLong(), 6_000L) -> 1
            scaled < Math.multiplyExact(effectiveMaxBpm.toLong(), 7_000L) -> 2
            scaled < Math.multiplyExact(effectiveMaxBpm.toLong(), 8_000L) -> 3
            scaled < Math.multiplyExact(effectiveMaxBpm.toLong(), 9_000L) -> 4
            else -> 5
        }
    }

    private fun coverageBasisPoints(covered: Long, eligible: Long): Int? = if (eligible == 0L) {
        null
    } else {
        Math.toIntExact(Math.multiplyExact(covered, BASIS_POINT_SCALE).floorDiv(eligible))
    }

    private fun coverageStatus(covered: Long, eligible: Long): String = when {
        eligible == 0L -> "no_eligible_duration"
        meetsThreshold(covered, eligible, NORMAL_BP) -> "normal"
        meetsThreshold(covered, eligible, PARTIAL_BP) -> "partial"
        else -> "insufficient"
    }

    private fun meetsThreshold(covered: Long, eligible: Long, threshold: Long): Boolean =
        Math.multiplyExact(covered, BASIS_POINT_SCALE) >= Math.multiplyExact(eligible, threshold)

    private fun positiveHalfUpAverage(weighted: Long?, covered: Long): Int? {
        if (weighted == null || covered == 0L) return null
        val quotient = weighted / covered
        val remainder = weighted % covered
        val rounded = if (remainder >= covered / 2 + covered % 2) {
            Math.addExact(quotient, 1L)
        } else {
            quotient
        }
        return Math.toIntExact(rounded)
    }

    private fun checkedEquals(expected: Long, actual: Long) {
        if (expected != actual) throw RecorderValidationException("invalid_analysis_snapshot_v1")
    }

    private fun nonNegativeDifference(end: Long, start: Long): Long =
        Math.subtractExact(end, start).coerceAtLeast(0L)

    private fun LongArray.checkedSum(): Long = fold(0L, Math::addExact)

    private fun WorkoutPhaseIntervalEntity.endTuple() = CanonicalTuple(
        requireNotNull(endOffsetMs),
        requireNotNull(endMutationSequence)
    )

    private fun HeartRateAcquisitionIntervalEntity.endTuple() = CanonicalTuple(
        requireNotNull(endOffsetMs),
        requireNotNull(endMutationSequence)
    )

    private data class EligibleSegment(
        val start: CanonicalTuple,
        val end: CanonicalTuple,
        val phaseMetricIndex: Int
    )

    private data class DurationAxes(
        val canonicalSessionDuration: Long,
        val recordingWindowDuration: Long,
        val notRequestedBeforeRecordingStart: Long,
        val expectedRecordingDuration: Long,
        val userExcludedDuration: Long,
        val intentDurations: LongArray,
        val primaryEligibleDuration: Long,
        val phaseExcludedDuration: Long,
        val strengthPrepareExcludedDuration: Long,
        val pausedExcludedDuration: Long,
        val eligibleCoveredDuration: Long,
        val eligibleUncoveredDuration: Long,
        val deviceStateDurations: LongArray,
        val deviceReasonDurations: LongArray
    )

    private val SAMPLE_ORDER = compareBy<HeartRateSampleEntity> { sample -> sample.offsetMs }
        .thenBy { sample -> sample.mutationSequence }
        .thenBy { sample -> sample.sampleSequence }
    private const val ANALYSIS_VERSION = 1
    private const val SAMPLE_VALIDITY_CAP_MS = 2_500L
    private const val BASIS_POINT_SCALE = 10_000L
    private const val PARTIAL_BP = 5_000L
    private const val PHASE_CONCLUSION_BP = 7_000L
    private const val NORMAL_BP = 8_000L
    private val PRIMARY_PHASE_KINDS = setOf(
        "timed_work",
        "timed_rest",
        "strength_active_set",
        "strength_confirm_set",
        "strength_rest",
        "follow_along_action",
        "follow_along_rest"
    )
    private val USER_EXCLUSION_REASONS = listOf(
        "user_turned_off",
        "user_opted_out",
        "user_disconnected_suppress_recovery"
    )
    private val DEVICE_STATES = listOf(
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
    private val DEVICE_REASONS = listOf(
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
    private val ZONE_FIELD_NAMES = listOf(
        "below50DurationMs",
        "from50To60DurationMs",
        "from60To70DurationMs",
        "from70To80DurationMs",
        "from80To90DurationMs",
        "atOrAbove90DurationMs"
    )
    private const val ANALYSIS_CONFIG_JSON =
        "{\"analysisConfigContractVersion\":1,\"sampleValidityCapMs\":2500,\"sampleIntervalContractVersion\":1,\"partialLowerBoundBasisPoints\":5000,\"phaseConclusionBasisPoints\":7000,\"normalBasisPoints\":8000,\"coverageThresholdRule\":\"checked_integer_cross_multiply\",\"coverageBasisPointsRule\":\"floor_integer_ratio\",\"displayPercentRule\":\"floor_basis_points_div_100\",\"weightedAverageRule\":\"checked_integer_time_integral\",\"averageDisplayRule\":\"positive_integer_half_up\",\"zeroCoveredRule\":\"null_integral_and_average\",\"observedMaxRule\":\"eligible_canonical_point_first_tie\",\"zoneAttributionContractVersion\":1,\"zoneAttributionRule\":\"checked_cross_multiply_six_zones\",\"statusProjectionContractVersion\":1,\"durationPartitionContractVersion\":1}"
}

internal object AnalysisSnapshotV1Validator {
    fun validate(
        graph: CanonicalSessionGraphV1,
        snapshot: HeartRateAnalysisSnapshotEntity
    ): CanonicalValidationResult {
        val recording = graph.recording
            ?: return CanonicalValidationResult.Invalid("invalid_analysis_snapshot_v1")
        if (
            graph.session.trustedEndOffsetMs == null || recording.status != "terminal" ||
            recording.originalAnalysisVersion != 1 || snapshot.recordingId != recording.recordingId ||
            snapshot.analysisVersion != 1 || snapshot.createdAt.isEmpty() ||
            snapshot.inputLastMutationSequence != graph.session.lastMutationSequence ||
            snapshot.inputLastMutationSequence != recording.endedMutationSequence
        ) {
            return CanonicalValidationResult.Invalid("invalid_analysis_snapshot_v1")
        }
        val structures = listOf(
            CanonicalStorageJsonV1Validators.validateAnalysisConfig(snapshot.analysisConfigJson),
            snapshot.zoneDurationsJson?.let(CanonicalStorageJsonV1Validators::validateZoneDurations)
                ?: CanonicalValidationResult.Valid,
            CanonicalStorageJsonV1Validators.validatePhaseAggregates(snapshot.phaseAggregatesJson),
            CanonicalStorageJsonV1Validators.validateDurationBreakdown(snapshot.durationBreakdownJson)
        )
        structures.firstOrNull { result -> result != CanonicalValidationResult.Valid }?.let { return it }
        if (
            CanonicalStorageJsonV1Validators.validateQualityReasons(snapshot.qualityReasonsJson) !=
            CanonicalValidationResult.Valid
        ) {
            return CanonicalValidationResult.Invalid("invalid_quality_reasons_v1")
        }
        val expected = CanonicalAnalysisV1.derive(graph, snapshot.createdAt)
        if (snapshot.qualityReasonsJson != expected.qualityReasonsJson) {
            return CanonicalValidationResult.Invalid("invalid_quality_reasons_v1")
        }
        if (snapshot != expected) {
            return CanonicalValidationResult.Invalid("invalid_analysis_snapshot_v1")
        }
        return try {
            StatusProjectionV1.project(recording, snapshot)
            CanonicalValidationResult.Valid
        } catch (failure: RecorderValidationException) {
            CanonicalValidationResult.Invalid(failure.code)
        }
    }
}

internal object StatusProjectionV1 {
    fun project(
        recording: HeartRateRecordingEntity?,
        snapshot: HeartRateAnalysisSnapshotEntity?
    ): String {
        if (recording == null) return "not_recorded"
        if (
            recording.status != "terminal" || recording.endedOffsetMs == null ||
            recording.endedMutationSequence == null || recording.originalAnalysisVersion != 1 ||
            snapshot == null || snapshot.recordingId != recording.recordingId ||
            snapshot.analysisVersion != 1 || snapshot.createdAt.isEmpty() ||
            snapshot.inputLastMutationSequence != recording.endedMutationSequence ||
            snapshot.eligibleDurationMs == null || snapshot.eligibleDurationMs < 0 ||
            snapshot.coveredDurationMs == null || snapshot.coveredDurationMs < 0 ||
            snapshot.coveredDurationMs > snapshot.eligibleDurationMs ||
            snapshot.canonicalSampleCount < 0 || snapshot.primaryPointSampleCount < 0 ||
            snapshot.primaryPointSampleCount > snapshot.canonicalSampleCount
        ) {
            invalidStatusProjection()
        }
        val eligible = snapshot.eligibleDurationMs
        val covered = snapshot.coveredDurationMs
        val sampleCountsValid = when (snapshot.sampleStatus) {
            "no_canonical_samples" ->
                snapshot.canonicalSampleCount == 0L && snapshot.primaryPointSampleCount == 0L
            "canonical_only_excluded" ->
                snapshot.canonicalSampleCount > 0L && snapshot.primaryPointSampleCount == 0L
            "primary_points_available" -> snapshot.primaryPointSampleCount > 0L
            else -> false
        }
        val weightedValid = if (covered == 0L) {
            snapshot.weightedBpmMs == null && snapshot.observedAvgBpm == null
        } else {
            snapshot.weightedBpmMs != null && snapshot.observedAvgBpm != null
        }
        val anchorValues = listOf(
            snapshot.observedMaxBpm,
            snapshot.highestOffsetMs,
            snapshot.highestMutationSequence,
            snapshot.highestSampleSequence
        )
        val anchorValid = if (snapshot.primaryPointSampleCount == 0L) {
            anchorValues.all { value -> value == null }
        } else {
            anchorValues.all { value -> value != null }
        }
        if (!sampleCountsValid || !weightedValid || !anchorValid) invalidStatusProjection()
        if (eligible == 0L) {
            if (
                covered != 0L || snapshot.coverageBasisPoints != null ||
                snapshot.coverageStatus != "no_eligible_duration" ||
                (recording.effectiveMaxBpm == null &&
                    (snapshot.zoneStatus != "unavailable_no_effective_max" ||
                        snapshot.zoneDurationsJson != null)) ||
                (recording.effectiveMaxBpm != null &&
                    (snapshot.zoneStatus != "available" || snapshot.zoneDurationsJson != null))
            ) {
                invalidStatusProjection()
            }
            return "no_eligible_duration"
        }
        val expectedBasis = Math.toIntExact(
            Math.multiplyExact(covered, 10_000L).floorDiv(eligible)
        )
        val expectedCoverage = when {
            Math.multiplyExact(covered, 10_000L) >= Math.multiplyExact(eligible, 8_000L) -> "normal"
            Math.multiplyExact(covered, 10_000L) >= Math.multiplyExact(eligible, 5_000L) -> "partial"
            else -> "insufficient"
        }
        if (
            snapshot.coverageBasisPoints != expectedBasis || snapshot.coverageStatus != expectedCoverage ||
            (recording.effectiveMaxBpm == null &&
                (snapshot.zoneStatus != "unavailable_no_effective_max" ||
                    snapshot.zoneDurationsJson != null)) ||
            (recording.effectiveMaxBpm != null &&
                (snapshot.zoneStatus != "available" || snapshot.zoneDurationsJson == null))
        ) {
            invalidStatusProjection()
        }
        if (snapshot.canonicalSampleCount == 0L) return "zero_samples"
        return when {
            snapshot.coverageStatus == "insufficient" -> "insufficient"
            snapshot.coverageStatus == "partial" -> "partial"
            snapshot.coverageStatus == "normal" &&
                snapshot.zoneStatus == "unavailable_no_effective_max" &&
                snapshot.zoneDurationsJson == null -> "recorded_no_zones"

            snapshot.coverageStatus == "normal" &&
                snapshot.zoneStatus == "available" && snapshot.zoneDurationsJson != null -> "recorded"

            else -> invalidStatusProjection()
        }
    }

    private fun invalidStatusProjection(): Nothing =
        throw RecorderValidationException("invalid_status_projection_v1")
}

private fun jsonObject(
    vararg fields: Pair<String, CanonicalJsonValue>
): CanonicalJsonValue.Obj = CanonicalJsonValue.Obj(linkedMapOf(*fields))

private fun jsonNumber(value: Long): CanonicalJsonValue.Num =
    CanonicalJsonValue.Num(BigDecimal.valueOf(value))

private fun jsonNumber(value: Int): CanonicalJsonValue.Num = jsonNumber(value.toLong())

private fun nullableNumber(value: Long?): CanonicalJsonValue =
    value?.let(::jsonNumber) ?: CanonicalJsonValue.Null

private fun nullableNumber(value: Int?): CanonicalJsonValue =
    value?.let(::jsonNumber) ?: CanonicalJsonValue.Null

private fun namedDurations(
    names: List<String>,
    durations: LongArray
): CanonicalJsonValue.Obj = jsonObject(
    *names.mapIndexed { index, name -> name to jsonNumber(durations[index]) }.toTypedArray()
)
