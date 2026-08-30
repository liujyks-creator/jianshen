package com.liujyks.trainflow.core.database

import com.liujyks.trainflow.core.data.RecorderValidationException
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalAnalysisV1Test {
    @Test
    fun statusProjectionUsesTheFrozenEightBranchPriority() {
        assertEquals("not_recorded", StatusProjectionV1.project(null, null))

        val invalidRecording = runCatching {
            StatusProjectionV1.project(terminalRecording().copy(status = "active"), baseSnapshot())
        }.exceptionOrNull()
        assertTrue(invalidRecording is RecorderValidationException)
        assertEquals("invalid_status_projection_v1", (invalidRecording as RecorderValidationException).code)

        val cases = listOf(
            Triple(terminalRecording(), baseSnapshot().copy(
                eligibleDurationMs = 0,
                coveredDurationMs = 0,
                coverageBasisPoints = null,
                canonicalSampleCount = 0,
                primaryPointSampleCount = 0,
                sampleStatus = "no_canonical_samples",
                coverageStatus = "no_eligible_duration",
                weightedBpmMs = null,
                observedAvgBpm = null,
                observedMaxBpm = null,
                highestOffsetMs = null,
                highestMutationSequence = null,
                highestSampleSequence = null
            ), "no_eligible_duration"),
            Triple(terminalRecording(), baseSnapshot().copy(
                canonicalSampleCount = 0,
                primaryPointSampleCount = 0,
                sampleStatus = "no_canonical_samples",
                coveredDurationMs = 0,
                coverageBasisPoints = 0,
                coverageStatus = "insufficient",
                weightedBpmMs = null,
                observedAvgBpm = null,
                observedMaxBpm = null,
                highestOffsetMs = null,
                highestMutationSequence = null,
                highestSampleSequence = null
            ), "zero_samples"),
            Triple(
                terminalRecording(),
                baseSnapshot().copy(
                    coveredDurationMs = 49,
                    coverageBasisPoints = 4_900,
                    coverageStatus = "insufficient",
                    weightedBpmMs = 5_880,
                    observedAvgBpm = 120
                ),
                "insufficient"
            ),
            Triple(
                terminalRecording(),
                baseSnapshot().copy(
                    coveredDurationMs = 50,
                    coverageBasisPoints = 5_000,
                    coverageStatus = "partial",
                    weightedBpmMs = 6_000,
                    observedAvgBpm = 120
                ),
                "partial"
            ),
            Triple(terminalRecording(), baseSnapshot().copy(
                coverageStatus = "normal",
                zoneStatus = "unavailable_no_effective_max",
                zoneDurationsJson = null
            ), "recorded_no_zones"),
            Triple(
                terminalRecording(effectiveMaxBpm = 200),
                baseSnapshot().copy(
                    coverageStatus = "normal",
                    zoneStatus = "available",
                    zoneDurationsJson = "{}"
                ),
                "recorded"
            )
        )
        cases.forEach { (recording, snapshot, expected) ->
            assertEquals(expected, StatusProjectionV1.project(recording, snapshot))
        }

        val invalidTail = runCatching {
            StatusProjectionV1.project(
                terminalRecording(),
                baseSnapshot().copy(coverageStatus = "no_eligible_duration")
            )
        }.exceptionOrNull()
        assertTrue(invalidTail is RecorderValidationException)
        assertEquals("invalid_status_projection_v1", (invalidTail as RecorderValidationException).code)
    }

    @Test
    fun derivationProducesExactScalarAxesSixZonesAndCanonicalJson() {
        val graph = terminalGraph(
            finalOffsetMs = 10_000,
            effectiveMaxBpm = 200,
            samples = listOf(
                sample(0, 0, 0, 99),
                sample(1, 2_500, 1, 100),
                sample(2, 5_000, 2, 100),
                sample(3, 7_500, 3, 101)
            )
        )

        val snapshot = CanonicalAnalysisV1.derive(graph, CREATED_AT)

        assertEquals("primary_points_available", snapshot.sampleStatus)
        assertEquals("normal", snapshot.coverageStatus)
        assertEquals("available", snapshot.zoneStatus)
        assertEquals(4, snapshot.canonicalSampleCount)
        assertEquals(4, snapshot.primaryPointSampleCount)
        assertEquals(10_000L, snapshot.eligibleDurationMs)
        assertEquals(10_000L, snapshot.coveredDurationMs)
        assertEquals(10_000, snapshot.coverageBasisPoints)
        assertEquals(1_000_000L, snapshot.weightedBpmMs)
        assertEquals(100, snapshot.observedAvgBpm)
        assertEquals(101, snapshot.observedMaxBpm)
        assertEquals(7_500L, snapshot.highestOffsetMs)
        assertEquals(3L, snapshot.highestMutationSequence)
        assertEquals(3L, snapshot.highestSampleSequence)
        assertEquals(EXPECTED_ANALYSIS_CONFIG, snapshot.analysisConfigJson)
        assertEquals(EXPECTED_ZONE_DURATIONS, snapshot.zoneDurationsJson)
        assertEquals(EXPECTED_PHASE_AGGREGATES, snapshot.phaseAggregatesJson)
        assertEquals(EXPECTED_DURATION_BREAKDOWN, snapshot.durationBreakdownJson)
        assertEquals(EMPTY_QUALITY_REASONS, snapshot.qualityReasonsJson)
        assertEquals(CanonicalValidationResult.Valid, AnalysisSnapshotV1Validator.validate(graph, snapshot))
        assertEquals("recorded", StatusProjectionV1.project(graph.recording, snapshot))
    }

    @Test
    fun sameOffsetBurstCountsZeroDurationPointsAndSelectsCanonicalFirstHighest() {
        val samples = (0L until 32L).map { sequence ->
            sample(sequence, 0, sequence, 150)
        }
        val snapshot = CanonicalAnalysisV1.derive(
            terminalGraph(finalOffsetMs = 1_000, samples = samples),
            CREATED_AT
        )

        assertEquals(32, snapshot.canonicalSampleCount)
        assertEquals(32, snapshot.primaryPointSampleCount)
        assertEquals(1_000L, snapshot.coveredDurationMs)
        assertEquals(150_000L, snapshot.weightedBpmMs)
        assertEquals(150, snapshot.observedAvgBpm)
        assertEquals(150, snapshot.observedMaxBpm)
        assertEquals(0L, snapshot.highestOffsetMs)
        assertEquals(0L, snapshot.highestMutationSequence)
        assertEquals(0L, snapshot.highestSampleSequence)
    }

    @Test
    fun thresholdEqualityAndPositiveHalfUpAreExact() {
        val cases = listOf(
            listOf(0L, 2_500L) to Triple(5_000L, "partial", false),
            listOf(0L, 2_500L, 4_500L) to Triple(7_000L, "partial", true),
            listOf(0L, 2_500L, 5_000L, 5_500L) to Triple(8_000L, "normal", true)
        )
        cases.forEach { (offsets, expected) ->
            val snapshot = CanonicalAnalysisV1.derive(
                terminalGraph(
                    finalOffsetMs = 10_000,
                    samples = offsets.mapIndexed { index, offset ->
                        sample(index.toLong(), offset, index.toLong(), if (index == offsets.lastIndex) 101 else 100)
                    }
                ),
                CREATED_AT
            )
            assertEquals(expected.first, snapshot.coveredDurationMs)
            assertEquals(expected.first.toInt(), snapshot.coverageBasisPoints)
            assertEquals(expected.second, snapshot.coverageStatus)
            assertEquals(expected.third, snapshot.phaseAggregatesJson.contains("\"conclusionEligible\":true"))
        }

        val halfUp = CanonicalAnalysisV1.derive(
            terminalGraph(
                finalOffsetMs = 2,
                samples = listOf(sample(0, 0, 0, 100), sample(1, 1, 1, 101))
            ),
            CREATED_AT
        )
        assertEquals(101, halfUp.observedAvgBpm)
    }

    @Test
    fun qualityReasonsUseExactRequiredIffScopeDurationAndOrderingMatrix() {
        val graph = complexReasonGraph()
        val snapshot = CanonicalAnalysisV1.derive(graph, CREATED_AT)

        assertEquals(EXPECTED_COMPLEX_QUALITY_REASONS, snapshot.qualityReasonsJson)
        assertEquals(CanonicalValidationResult.Valid, AnalysisSnapshotV1Validator.validate(graph, snapshot))

        val invalidJson = listOf(
            COMPLEX_REASONS_MISSING_REQUIRED,
            COMPLEX_REASONS_EXTRA_FORBIDDEN,
            COMPLEX_REASONS_WRONG_SCOPE,
            COMPLEX_REASONS_WRONG_DURATION,
            COMPLEX_REASONS_DUPLICATE
        )
        invalidJson.forEachIndexed { index, json ->
            val result = AnalysisSnapshotV1Validator.validate(
                graph,
                snapshot.copy(qualityReasonsJson = json)
            )
            assertTrue("reason mutation $index accepted: $json", result is CanonicalValidationResult.Invalid)
            assertEquals("invalid_quality_reasons_v1", (result as CanonicalValidationResult.Invalid).code)
        }
    }

    @Test
    fun noEligibleKeepsSampleAxisReasonAndWholeZoneColumnNull() {
        val graph = terminalGraph(
            finalOffsetMs = 1_000,
            effectiveMaxBpm = 200,
            phases = listOf(phase(0, "paused", 0, 1_000)),
            samples = emptyList()
        )
        val snapshot = CanonicalAnalysisV1.derive(graph, CREATED_AT)

        assertEquals("no_eligible_duration", snapshot.coverageStatus)
        assertEquals(0L, snapshot.eligibleDurationMs)
        assertEquals(0L, snapshot.coveredDurationMs)
        assertNull(snapshot.coverageBasisPoints)
        assertNull(snapshot.zoneDurationsJson)
        assertEquals(
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[{\"reasonCode\":\"no_eligible_duration\",\"durationMs\":null},{\"reasonCode\":\"no_canonical_samples\",\"durationMs\":null},{\"reasonCode\":\"paused_excluded\",\"durationMs\":1000}],\"phaseReasons\":[{\"phaseSequence\":0,\"reasonCode\":\"paused_excluded\",\"durationMs\":1000}]}",
            snapshot.qualityReasonsJson
        )
        assertEquals("no_eligible_duration", StatusProjectionV1.project(graph.recording, snapshot))
    }

    @Test
    fun canonicalOnlyExcludedPreservesWholeAndPhaseReasonSemantics() {
        val graph = terminalGraph(
            finalOffsetMs = 1_000,
            acquisitions = listOf(
                acquisition(0, 0, 500, "user_excluded", "user_turned_off"),
                acquisition(1, 500, 1_000, "expected_recording", null)
            ),
            samples = listOf(sample(0, 0, 0, 120))
        )
        val snapshot = CanonicalAnalysisV1.derive(graph, CREATED_AT)

        assertEquals("canonical_only_excluded", snapshot.sampleStatus)
        assertEquals(0, snapshot.primaryPointSampleCount)
        assertTrue(snapshot.qualityReasonsJson.contains("\"reasonCode\":\"canonical_only_excluded\""))
        assertTrue(snapshot.qualityReasonsJson.contains("\"phaseSequence\":0,\"reasonCode\":\"no_canonical_samples\""))
        assertTrue(
            !snapshot.qualityReasonsJson.contains(
                "\"phaseSequence\":0,\"reasonCode\":\"canonical_only_excluded\""
            )
        )
        assertEquals("insufficient", StatusProjectionV1.project(graph.recording, snapshot))
    }

    @Test
    fun zoneThresholdBeforeExactAndAfterSamplesPopulateAllSixBuckets() {
        val bpms = listOf(99, 100, 119, 120, 139, 140, 159, 160, 179, 180)
        val snapshot = CanonicalAnalysisV1.derive(
            terminalGraph(
                finalOffsetMs = 10_000,
                effectiveMaxBpm = 200,
                samples = bpms.mapIndexed { index, bpm ->
                    sample(index.toLong(), index * 1_000L, index.toLong(), bpm)
                }
            ),
            CREATED_AT
        )
        assertEquals(
            "{\"zoneDurationsContractVersion\":1,\"below50DurationMs\":1000,\"from50To60DurationMs\":2000,\"from60To70DurationMs\":2000,\"from70To80DurationMs\":2000,\"from80To90DurationMs\":2000,\"atOrAbove90DurationMs\":1000}",
            snapshot.zoneDurationsJson
        )
    }

    @Test
    fun checkedOverflowIsNotTranslatedToValidationOrFallback() {
        val graph = terminalGraph(
            finalOffsetMs = Long.MAX_VALUE,
            samples = listOf(sample(0, Long.MAX_VALUE - 1_000, 0, 120))
        )
        val failure = runCatching { CanonicalAnalysisV1.derive(graph, CREATED_AT) }.exceptionOrNull()
        assertTrue(failure is ArithmeticException)
    }

    private fun terminalGraph(
        finalOffsetMs: Long,
        effectiveMaxBpm: Int? = null,
        phases: List<WorkoutPhaseIntervalEntity> = listOf(phase(0, "timed_work", 0, finalOffsetMs)),
        acquisitions: List<HeartRateAcquisitionIntervalEntity> = listOf(
            acquisition(0, 0, finalOffsetMs, "expected_recording", null)
        ),
        samples: List<HeartRateSampleEntity>
    ): CanonicalSessionGraphV1 {
        val recording = terminalRecording(
            finalOffsetMs = finalOffsetMs,
            finalMutationSequence = FINAL_SEQUENCE,
            effectiveMaxBpm = effectiveMaxBpm
        )
        return CanonicalSessionGraphV1(
            session = terminalSession(finalOffsetMs, FINAL_SEQUENCE),
            phases = phases,
            recording = recording,
            acquisitions = acquisitions,
            samples = samples
        )
    }

    private fun complexReasonGraph(): CanonicalSessionGraphV1 {
        val finalOffset = 6_000L
        return CanonicalSessionGraphV1(
            session = terminalSession(finalOffset, FINAL_SEQUENCE, "abandoned", "process_interrupted"),
            phases = listOf(
                phase(0, "timed_work", 0, 1_000),
                phase(1, "strength_prepare_set", 1_000, 2_000),
                phase(2, "paused", 2_000, 3_000),
                phase(3, "timed_rest", 3_000, finalOffset)
            ),
            recording = terminalRecording(finalOffset, FINAL_SEQUENCE, startedOffsetMs = 500),
            acquisitions = listOf(
                acquisition(0, 500, 1_000, "expected_recording", null),
                acquisition(1, 1_000, 2_000, "expected_recording", null),
                acquisition(2, 2_000, 3_000, "expected_recording", null),
                acquisition(3, 3_000, 3_500, "user_excluded", "user_turned_off"),
                acquisition(4, 3_500, 4_000, "user_excluded", "user_opted_out"),
                acquisition(
                    5,
                    4_000,
                    4_500,
                    "user_excluded",
                    "user_disconnected_suppress_recovery"
                ),
                acquisition(6, 4_500, finalOffset, "expected_recording", null)
            ),
            samples = emptyList()
        )
    }

    private fun terminalSession(
        finalOffsetMs: Long,
        finalMutationSequence: Long,
        status: String = "completed",
        reason: String = "completed"
    ) = WorkoutSessionEntity(
        id = SESSION_ID,
        mode = "timed",
        status = status,
        planSnapshotJson = "{}",
        timelineVersion = 1,
        lastDurableOffsetMs = finalOffsetMs,
        lastMutationSequence = finalMutationSequence,
        trustedEndOffsetMs = finalOffsetMs,
        terminalReason = reason,
        displayMetadataContractVersion = 1,
        sessionDisplayMetadataJson = "{}"
    )

    private fun terminalRecording(
        finalOffsetMs: Long = 100,
        finalMutationSequence: Long = FINAL_SEQUENCE,
        effectiveMaxBpm: Int? = null,
        startedOffsetMs: Long = 0
    ) = HeartRateRecordingEntity(
        recordingId = RECORDING_ID,
        sessionId = SESSION_ID,
        status = "terminal",
        startedOffsetMs = startedOffsetMs,
        startedMutationSequence = 0,
        endedOffsetMs = finalOffsetMs,
        endedMutationSequence = finalMutationSequence,
        sourceContractVersion = 1,
        sourceKind = "ble_hrs",
        acquisitionContractVersion = 1,
        parameterSnapshotVersion = 1,
        personalMaxBpm = effectiveMaxBpm,
        effectiveMaxBpm = effectiveMaxBpm,
        effectiveMaxSource = effectiveMaxBpm?.let { "personal_max" },
        zoneSnapshotJson = effectiveMaxBpm?.let { "typed-zone-snapshot-not-read-by-analysis" },
        originalAnalysisVersion = 1
    )

    private fun phase(sequence: Int, kind: String, start: Long, end: Long) =
        WorkoutPhaseIntervalEntity(
            id = "phase-$sequence",
            sessionId = SESSION_ID,
            sequence = sequence,
            startOffsetMs = start,
            endOffsetMs = end,
            startMutationSequence = sequence.toLong(),
            endMutationSequence = if (end == Long.MAX_VALUE) FINAL_SEQUENCE else sequence.toLong() + 1,
            openMarker = null,
            phaseKind = kind,
            phaseIdentityJson = "{}"
        )

    private fun acquisition(
        sequence: Int,
        start: Long,
        end: Long,
        intent: String,
        reason: String?
    ) = HeartRateAcquisitionIntervalEntity(
        id = "acquisition-$sequence",
        recordingId = RECORDING_ID,
        sequence = sequence,
        startOffsetMs = start,
        endOffsetMs = end,
        startMutationSequence = sequence.toLong(),
        endMutationSequence = if (end == Long.MAX_VALUE) FINAL_SEQUENCE else sequence.toLong() + 1,
        openMarker = null,
        recordingIntent = intent,
        intentReason = reason,
        deviceState = "live",
        deviceReason = null
    )

    private fun sample(sequence: Long, offset: Long, mutation: Long, bpm: Int) =
        HeartRateSampleEntity(RECORDING_ID, sequence, offset, mutation, bpm)

    private fun baseSnapshot() = HeartRateAnalysisSnapshotEntity(
        recordingId = RECORDING_ID,
        analysisVersion = 1,
        createdAt = CREATED_AT,
        inputLastMutationSequence = FINAL_SEQUENCE,
        sampleStatus = "primary_points_available",
        coverageStatus = "normal",
        zoneStatus = "unavailable_no_effective_max",
        canonicalSampleCount = 1,
        primaryPointSampleCount = 1,
        eligibleDurationMs = 100,
        coveredDurationMs = 100,
        coverageBasisPoints = 10_000,
        weightedBpmMs = 12_000,
        observedAvgBpm = 120,
        observedMaxBpm = 120,
        highestOffsetMs = 0,
        highestMutationSequence = 0,
        highestSampleSequence = 0,
        analysisConfigJson = EXPECTED_ANALYSIS_CONFIG,
        zoneDurationsJson = null,
        phaseAggregatesJson = "{}",
        durationBreakdownJson = "{}",
        qualityReasonsJson = EMPTY_QUALITY_REASONS
    )

    private companion object {
        const val SESSION_ID = "session"
        const val RECORDING_ID = "recording"
        const val FINAL_SEQUENCE = 40L
        const val CREATED_AT = "2026-08-31T00:00:00Z"
        const val EXPECTED_ANALYSIS_CONFIG =
            "{\"analysisConfigContractVersion\":1,\"sampleValidityCapMs\":2500,\"sampleIntervalContractVersion\":1,\"partialLowerBoundBasisPoints\":5000,\"phaseConclusionBasisPoints\":7000,\"normalBasisPoints\":8000,\"coverageThresholdRule\":\"checked_integer_cross_multiply\",\"coverageBasisPointsRule\":\"floor_integer_ratio\",\"displayPercentRule\":\"floor_basis_points_div_100\",\"weightedAverageRule\":\"checked_integer_time_integral\",\"averageDisplayRule\":\"positive_integer_half_up\",\"zeroCoveredRule\":\"null_integral_and_average\",\"observedMaxRule\":\"eligible_canonical_point_first_tie\",\"zoneAttributionContractVersion\":1,\"zoneAttributionRule\":\"checked_cross_multiply_six_zones\",\"statusProjectionContractVersion\":1,\"durationPartitionContractVersion\":1}"
        const val EXPECTED_ZONE_DURATIONS =
            "{\"zoneDurationsContractVersion\":1,\"below50DurationMs\":2500,\"from50To60DurationMs\":7500,\"from60To70DurationMs\":0,\"from70To80DurationMs\":0,\"from80To90DurationMs\":0,\"atOrAbove90DurationMs\":0}"
        const val EXPECTED_PHASE_AGGREGATES =
            "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[{\"phaseSequence\":0,\"phaseKind\":\"timed_work\",\"eligibleDurationMs\":10000,\"coveredDurationMs\":10000,\"coverageBasisPoints\":10000,\"coverageStatus\":\"normal\",\"conclusionEligible\":true,\"weightedBpmMs\":1000000,\"observedAvgBpm\":100,\"observedMaxBpm\":101,\"highestOffsetMs\":7500,\"highestMutationSequence\":3,\"highestSampleSequence\":3}]}"
        const val EXPECTED_DURATION_BREAKDOWN =
            "{\"durationBreakdownContractVersion\":1,\"canonicalSessionDurationMs\":10000,\"recordingWindowDurationMs\":10000,\"notRequestedBeforeRecordingStartMs\":0,\"intentAxis\":{\"expectedRecordingDurationMs\":10000,\"userExcludedDurationMs\":0,\"userTurnedOffDurationMs\":0,\"userOptedOutDurationMs\":0,\"userDisconnectedSuppressRecoveryDurationMs\":0},\"phaseAxis\":{\"primaryEligibleDurationMs\":10000,\"phaseExcludedDurationMs\":0,\"strengthPrepareExcludedDurationMs\":0,\"pausedExcludedDurationMs\":0},\"primaryAnalysisPartition\":{\"primaryEligibleDurationMs\":10000,\"eligibleCoveredDurationMs\":10000,\"eligibleUncoveredDurationMs\":0},\"deviceStateDurations\":{\"not_observing\":0,\"no_source_selected\":0,\"permission_required\":0,\"bluetooth_unavailable\":0,\"searching\":0,\"connecting\":0,\"waiting_first_sample\":0,\"live\":10000,\"stale\":0,\"reconnecting\":0,\"disconnected\":0,\"technical_failure\":0},\"deviceReasonDurations\":{\"initial_acquisition\":0,\"automatic_recovery\":0,\"source_not_selected\":0,\"source_unavailable\":0,\"permission_missing\":0,\"permission_revoked\":0,\"bluetooth_off\":0,\"platform_unavailable\":0,\"first_sample_timeout\":0,\"sample_stale_timeout\":0,\"unexpected_disconnect\":0,\"connection_timeout\":0,\"measurement_stream_unavailable\":0,\"platform_failure\":0},\"orthogonalityContract\":{\"contractVersion\":1,\"rule\":\"primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum\"}}"
        const val EMPTY_QUALITY_REASONS =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[],\"phaseReasons\":[]}"
        const val EXPECTED_COMPLEX_QUALITY_REASONS =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[{\"reasonCode\":\"no_canonical_samples\",\"durationMs\":null},{\"reasonCode\":\"eligible_uncovered_present\",\"durationMs\":2000},{\"reasonCode\":\"insufficient_coverage\",\"durationMs\":null},{\"reasonCode\":\"unavailable_no_effective_max\",\"durationMs\":null},{\"reasonCode\":\"not_requested_before_recording_start\",\"durationMs\":500},{\"reasonCode\":\"strength_prepare_excluded\",\"durationMs\":1000},{\"reasonCode\":\"paused_excluded\",\"durationMs\":1000},{\"reasonCode\":\"user_turned_off_excluded\",\"durationMs\":500},{\"reasonCode\":\"user_opted_out_excluded\",\"durationMs\":500},{\"reasonCode\":\"user_disconnected_suppress_recovery_excluded\",\"durationMs\":500},{\"reasonCode\":\"process_interrupted\",\"durationMs\":null}],\"phaseReasons\":[{\"phaseSequence\":0,\"reasonCode\":\"no_canonical_samples\",\"durationMs\":null},{\"phaseSequence\":0,\"reasonCode\":\"eligible_uncovered_present\",\"durationMs\":500},{\"phaseSequence\":0,\"reasonCode\":\"insufficient_coverage\",\"durationMs\":null},{\"phaseSequence\":0,\"reasonCode\":\"unavailable_no_effective_max\",\"durationMs\":null},{\"phaseSequence\":1,\"reasonCode\":\"strength_prepare_excluded\",\"durationMs\":1000},{\"phaseSequence\":2,\"reasonCode\":\"paused_excluded\",\"durationMs\":1000},{\"phaseSequence\":3,\"reasonCode\":\"no_canonical_samples\",\"durationMs\":null},{\"phaseSequence\":3,\"reasonCode\":\"eligible_uncovered_present\",\"durationMs\":1500},{\"phaseSequence\":3,\"reasonCode\":\"insufficient_coverage\",\"durationMs\":null},{\"phaseSequence\":3,\"reasonCode\":\"unavailable_no_effective_max\",\"durationMs\":null},{\"phaseSequence\":3,\"reasonCode\":\"user_turned_off_excluded\",\"durationMs\":500},{\"phaseSequence\":3,\"reasonCode\":\"user_opted_out_excluded\",\"durationMs\":500},{\"phaseSequence\":3,\"reasonCode\":\"user_disconnected_suppress_recovery_excluded\",\"durationMs\":500}]}"
        const val COMPLEX_REASONS_MISSING_REQUIRED = EMPTY_QUALITY_REASONS
        const val COMPLEX_REASONS_EXTRA_FORBIDDEN =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[{\"reasonCode\":\"partial_coverage\",\"durationMs\":null}],\"phaseReasons\":[]}"
        const val COMPLEX_REASONS_WRONG_SCOPE =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[],\"phaseReasons\":[{\"phaseSequence\":0,\"reasonCode\":\"process_interrupted\",\"durationMs\":null}]}"
        const val COMPLEX_REASONS_WRONG_DURATION =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[{\"reasonCode\":\"eligible_uncovered_present\",\"durationMs\":1999}],\"phaseReasons\":[]}"
        const val COMPLEX_REASONS_DUPLICATE =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[{\"reasonCode\":\"process_interrupted\",\"durationMs\":null},{\"reasonCode\":\"process_interrupted\",\"durationMs\":null}],\"phaseReasons\":[]}"
    }
}
