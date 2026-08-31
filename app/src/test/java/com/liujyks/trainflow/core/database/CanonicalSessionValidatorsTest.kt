package com.liujyks.trainflow.core.database

import com.liujyks.trainflow.core.data.OrderedStructureSignatureInputV1
import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1ValidationResult
import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1Validator
import com.liujyks.trainflow.core.data.toStorageJson
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class CanonicalSessionValidatorsTest {
    @Test
    fun allNullHeadersPreserveEveryLegacyStatusIncludingActiveAndPaused() {
        listOf("ready", "active", "paused", "completed", "abandoned").forEach { status ->
            val result = CanonicalSessionHeaderV1Validator.validate(legacySession(status))
            assertTrue("Expected legacy classification for $status", result is CanonicalSessionHeaderV1Result.Legacy)
        }
    }

    @Test
    fun partialAndInvalidCanonicalHeadersFailClosed() {
        val partial = legacySession("active").copy(timelineVersion = 1)
        assertInvalid(CanonicalSessionHeaderV1Validator.validate(partial), "invalid_partial_canonical_header")

        val terminalMismatch = canonicalTerminalSession().copy(
            trustedEndOffsetMs = 99
        )
        assertInvalid(CanonicalSessionHeaderV1Validator.validate(terminalMismatch), "invalid_canonical_header_v1")

        val completedWrongReason = canonicalTerminalSession().copy(
            terminalReason = "user_abandoned"
        )
        assertInvalid(CanonicalSessionHeaderV1Validator.validate(completedWrongReason), "invalid_canonical_header_v1")

        val abandonedWrongReason = canonicalTerminalSession().copy(
            status = "abandoned",
            terminalReason = "completed"
        )
        assertInvalid(CanonicalSessionHeaderV1Validator.validate(abandonedWrongReason), "invalid_canonical_header_v1")
    }

    @Test
    fun runningAndTerminalHeadersUseTheExactCanonicalTupleMatrix() {
        assertTrue(
            CanonicalSessionHeaderV1Validator.validate(canonicalRunningSession())
                is CanonicalSessionHeaderV1Result.CanonicalRunning
        )
        assertTrue(
            CanonicalSessionHeaderV1Validator.validate(canonicalTerminalSession())
                is CanonicalSessionHeaderV1Result.CanonicalTerminal
        )
    }

    @Test
    fun recordingParameterAndAcquisitionPairMatricesAreClosed() {
        assertValid(RecordingHeaderV1Validator.validate(terminalRecording()))
        assertValid(AcquisitionV1Validator.validate(terminalAcquisition()))

        assertInvalid(
            RecordingHeaderV1Validator.validate(
                terminalRecording().copy(sourceKind = "watch_sdk")
            ),
            "invalid_recording_header_v1"
        )
        assertInvalid(
            RecordingHeaderV1Validator.validate(
                terminalRecording().copy(
                    effectiveMaxBpm = 180,
                    effectiveMaxSource = "personal_max",
                    personalMaxBpm = 179,
                    zoneSnapshotJson = VALID_ZONE_SNAPSHOT
                )
            ),
            "invalid_recording_header_v1"
        )
        assertInvalid(
            AcquisitionV1Validator.validate(
                terminalAcquisition().copy(deviceState = "live", deviceReason = "automatic_recovery")
            ),
            "invalid_acquisition_v1"
        )
        assertInvalid(
            AcquisitionV1Validator.validate(
                terminalAcquisition().copy(
                    recordingIntent = "expected_recording",
                    intentReason = "user_turned_off"
                )
            ),
            "invalid_acquisition_v1"
        )
    }

    @Test
    fun canonicalGraphAcceptsOneCompleteTerminalInputCut() {
        assertValid(CanonicalSessionGraphV1Validator.validate(validTerminalGraph()))
    }

    @Test
    fun analysisSnapshotSemanticsBindTypedFieldsToTheSameRawInputCut() {
        val valid = validTerminalGraph()
        val mutations = listOf(
            valid.copy(snapshots = listOf(terminalSnapshot().copy(canonicalSampleCount = 2))),
            valid.copy(snapshots = listOf(terminalSnapshot().copy(primaryPointSampleCount = 0))),
            valid.copy(snapshots = listOf(terminalSnapshot().copy(coverageBasisPoints = 9999))),
            valid.copy(snapshots = listOf(terminalSnapshot().copy(weightedBpmMs = 1))),
            valid.copy(snapshots = listOf(terminalSnapshot().copy(observedAvgBpm = 119))),
            valid.copy(snapshots = listOf(terminalSnapshot().copy(highestOffsetMs = 49)))
        )

        mutations.forEachIndexed { index, graph ->
            assertTrue(
                "Analysis/raw binding mutation $index unexpectedly validated",
                CanonicalSessionGraphV1Validator.validate(graph) is CanonicalValidationResult.Invalid
            )
        }
    }

    @Test
    fun analysisDurationBindingKeepsPhaseAndIntentAxesIndependentAcrossPartitions() {
        val phases = listOf(
            terminalPhase().copy(
                id = "phase-0",
                endOffsetMs = 20,
                endMutationSequence = 1,
                phaseKind = "paused",
                phaseIdentityJson = VALID_PAUSED_PHASE_IDENTITY
            ),
            terminalPhase().copy(
                id = "phase-1",
                sequence = 1,
                startOffsetMs = 20,
                startMutationSequence = 1,
                endOffsetMs = 80,
                endMutationSequence = 3
            ),
            terminalPhase().copy(
                id = "phase-2",
                sequence = 2,
                startOffsetMs = 80,
                startMutationSequence = 3,
                phaseKind = "paused",
                phaseIdentityJson = VALID_PAUSED_PHASE_IDENTITY
            )
        )
        val acquisitions = listOf(
            terminalAcquisition().copy(
                id = "acquisition-0",
                endOffsetMs = 30,
                endMutationSequence = 1
            ),
            terminalAcquisition().copy(
                id = "acquisition-1",
                sequence = 1,
                startOffsetMs = 30,
                startMutationSequence = 1,
                endOffsetMs = 50,
                endMutationSequence = 2,
                recordingIntent = "user_excluded",
                intentReason = "user_turned_off"
            ),
            terminalAcquisition().copy(
                id = "acquisition-2",
                sequence = 2,
                startOffsetMs = 50,
                startMutationSequence = 2,
                endOffsetMs = 90,
                endMutationSequence = 3
            ),
            terminalAcquisition().copy(
                id = "acquisition-3",
                sequence = 3,
                startOffsetMs = 90,
                startMutationSequence = 3,
                recordingIntent = "user_excluded",
                intentReason = "user_turned_off"
            )
        )
        val samples = listOf(
            HeartRateSampleEntity("recording", 0, 20, 1, 120),
            HeartRateSampleEntity("recording", 1, 40, 2, 200),
            HeartRateSampleEntity("recording", 2, 50, 2, 130)
        )
        val phaseAggregates =
            "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[{\"phaseSequence\":1," +
                "\"phaseKind\":\"timed_work\",\"eligibleDurationMs\":40,\"coveredDurationMs\":40," +
                "\"coverageBasisPoints\":10000,\"coverageStatus\":\"normal\"," +
                "\"conclusionEligible\":true,\"weightedBpmMs\":5100,\"observedAvgBpm\":128," +
                "\"observedMaxBpm\":130,\"highestOffsetMs\":50,\"highestMutationSequence\":2," +
                "\"highestSampleSequence\":2}]}"
        val durationBreakdown =
            "{\"durationBreakdownContractVersion\":1,\"canonicalSessionDurationMs\":100," +
                "\"recordingWindowDurationMs\":100,\"notRequestedBeforeRecordingStartMs\":0," +
                "\"intentAxis\":{\"expectedRecordingDurationMs\":70,\"userExcludedDurationMs\":30," +
                "\"userTurnedOffDurationMs\":30,\"userOptedOutDurationMs\":0," +
                "\"userDisconnectedSuppressRecoveryDurationMs\":0}," +
                "\"phaseAxis\":{\"primaryEligibleDurationMs\":40,\"phaseExcludedDurationMs\":30," +
                "\"strengthPrepareExcludedDurationMs\":0,\"pausedExcludedDurationMs\":30}," +
                "\"primaryAnalysisPartition\":{\"primaryEligibleDurationMs\":40," +
                "\"eligibleCoveredDurationMs\":40,\"eligibleUncoveredDurationMs\":0}," +
                "\"deviceStateDurations\":{\"not_observing\":0,\"no_source_selected\":0," +
                "\"permission_required\":0,\"bluetooth_unavailable\":0,\"searching\":0," +
                "\"connecting\":0,\"waiting_first_sample\":0,\"live\":100,\"stale\":0," +
                "\"reconnecting\":0,\"disconnected\":0,\"technical_failure\":0}," +
                "\"deviceReasonDurations\":{\"initial_acquisition\":0,\"automatic_recovery\":0," +
                "\"source_not_selected\":0,\"source_unavailable\":0,\"permission_missing\":0," +
                "\"permission_revoked\":0,\"bluetooth_off\":0,\"platform_unavailable\":0," +
                "\"first_sample_timeout\":0,\"sample_stale_timeout\":0,\"unexpected_disconnect\":0," +
                "\"connection_timeout\":0,\"measurement_stream_unavailable\":0," +
                "\"platform_failure\":0},\"orthogonalityContract\":{\"contractVersion\":1," +
                "\"rule\":\"primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum\"}}"
        val snapshot = terminalSnapshot().copy(
            canonicalSampleCount = 3,
            primaryPointSampleCount = 2,
            eligibleDurationMs = 40,
            coveredDurationMs = 40,
            coverageBasisPoints = 10000,
            weightedBpmMs = 5100,
            observedAvgBpm = 128,
            observedMaxBpm = 130,
            highestOffsetMs = 50,
            highestMutationSequence = 2,
            highestSampleSequence = 2,
            phaseAggregatesJson = phaseAggregates,
            durationBreakdownJson = durationBreakdown
        )
        val graph = validTerminalGraph().copy(
            phases = phases,
            acquisitions = acquisitions,
            samples = samples,
            snapshots = listOf(snapshot)
        )

        assertValid(CanonicalSessionGraphV1Validator.validate(graph))
        assertInvalid(
            CanonicalSessionGraphV1Validator.validate(
                graph.copy(
                    snapshots = listOf(
                        snapshot.copy(
                            durationBreakdownJson = durationBreakdown.replace(
                                "\"phaseExcludedDurationMs\":30",
                                "\"phaseExcludedDurationMs\":40"
                            )
                        )
                    )
                )
            ),
            "invalid_canonical_graph_v1"
        )
    }

    @Test
    fun analysisSnapshotCoversThresholdNullZoneTieAndExcludedInputMatrices() {
        fun aggregate(
            eligible: Long,
            covered: Long,
            basis: Int?,
            status: String,
            weighted: Long?,
            average: Int?,
            maximum: Int?,
            offset: Long?
        ): String =
            "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[{\"phaseSequence\":0,\"phaseKind\":\"timed_work\",\"eligibleDurationMs\":$eligible,\"coveredDurationMs\":$covered,\"coverageBasisPoints\":${basis ?: "null"},\"coverageStatus\":\"$status\",\"conclusionEligible\":${eligible > 0 && covered * 10_000 >= eligible * 7_000},\"weightedBpmMs\":${weighted ?: "null"},\"observedAvgBpm\":${average ?: "null"},\"observedMaxBpm\":${maximum ?: "null"},\"highestOffsetMs\":${offset ?: "null"},\"highestMutationSequence\":${if (offset == null) "null" else "0"},\"highestSampleSequence\":${if (offset == null) "null" else "0"}}]}"
        fun duration(
            primaryEligible: Long,
            covered: Long,
            phaseExcluded: Long = 0,
            pausedExcluded: Long = 0,
            expectedIntent: Long = 100,
            userExcluded: Long = 0,
            userTurnedOff: Long = 0
        ): String =
            "{\"durationBreakdownContractVersion\":1,\"canonicalSessionDurationMs\":100,\"recordingWindowDurationMs\":100,\"notRequestedBeforeRecordingStartMs\":0,\"intentAxis\":{\"expectedRecordingDurationMs\":$expectedIntent,\"userExcludedDurationMs\":$userExcluded,\"userTurnedOffDurationMs\":$userTurnedOff,\"userOptedOutDurationMs\":0,\"userDisconnectedSuppressRecoveryDurationMs\":0},\"phaseAxis\":{\"primaryEligibleDurationMs\":$primaryEligible,\"phaseExcludedDurationMs\":$phaseExcluded,\"strengthPrepareExcludedDurationMs\":0,\"pausedExcludedDurationMs\":$pausedExcluded},\"primaryAnalysisPartition\":{\"primaryEligibleDurationMs\":$primaryEligible,\"eligibleCoveredDurationMs\":$covered,\"eligibleUncoveredDurationMs\":${primaryEligible - covered}},\"deviceStateDurations\":{\"not_observing\":0,\"no_source_selected\":0,\"permission_required\":0,\"bluetooth_unavailable\":0,\"searching\":0,\"connecting\":0,\"waiting_first_sample\":0,\"live\":100,\"stale\":0,\"reconnecting\":0,\"disconnected\":0,\"technical_failure\":0},\"deviceReasonDurations\":{\"initial_acquisition\":0,\"automatic_recovery\":0,\"source_not_selected\":0,\"source_unavailable\":0,\"permission_missing\":0,\"permission_revoked\":0,\"bluetooth_off\":0,\"platform_unavailable\":0,\"first_sample_timeout\":0,\"sample_stale_timeout\":0,\"unexpected_disconnect\":0,\"connection_timeout\":0,\"measurement_stream_unavailable\":0,\"platform_failure\":0},\"orthogonalityContract\":{\"contractVersion\":1,\"rule\":\"primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum\"}}"
        fun coverageGraph(offset: Long, covered: Long, basis: Int, status: String): CanonicalSessionGraphV1 {
            val weighted = 120L * covered
            return validTerminalGraph().copy(
                samples = listOf(HeartRateSampleEntity("recording", 0, offset, 0, 120)),
                snapshots = listOf(
                    terminalSnapshot().copy(
                        coverageStatus = status,
                        coveredDurationMs = covered,
                        coverageBasisPoints = basis,
                        weightedBpmMs = weighted,
                        observedAvgBpm = 120,
                        highestOffsetMs = offset,
                        phaseAggregatesJson = aggregate(100, covered, basis, status, weighted, 120, 120, offset),
                        durationBreakdownJson = duration(100, covered)
                    )
                )
            )
        }

        assertValid(CanonicalSessionGraphV1Validator.validate(coverageGraph(50, 50, 5000, "partial")))
        assertValid(CanonicalSessionGraphV1Validator.validate(coverageGraph(75, 25, 2500, "insufficient")))

        val firstHighestTie = validTerminalGraph().copy(
            samples = listOf(
                HeartRateSampleEntity("recording", 0, 0, 0, 130),
                HeartRateSampleEntity("recording", 1, 50, 1, 130)
            ),
            snapshots = listOf(
                terminalSnapshot().copy(
                    canonicalSampleCount = 2,
                    primaryPointSampleCount = 2,
                    weightedBpmMs = 13000,
                    observedAvgBpm = 130,
                    observedMaxBpm = 130,
                    phaseAggregatesJson = aggregate(100, 100, 10000, "normal", 13000, 130, 130, 0)
                )
            )
        )
        assertValid(CanonicalSessionGraphV1Validator.validate(firstHighestTie))
        assertInvalid(
            CanonicalSessionGraphV1Validator.validate(
                firstHighestTie.copy(
                    snapshots = listOf(firstHighestTie.snapshots.single().copy(highestOffsetMs = 50, highestMutationSequence = 1, highestSampleSequence = 1))
                )
            ),
            "invalid_canonical_graph_v1"
        )

        val zoneAvailable = validTerminalGraph().copy(
            recording = terminalRecording().copy(
                personalMaxBpm = 200,
                effectiveMaxBpm = 200,
                effectiveMaxSource = "personal_max",
                zoneSnapshotJson = VALID_ZONE_SNAPSHOT_200
            ),
            snapshots = listOf(
                terminalSnapshot().copy(
                    zoneStatus = "available",
                    zoneDurationsJson = VALID_ZONE_DURATIONS_120_OF_200
                )
            )
        )
        assertValid(CanonicalSessionGraphV1Validator.validate(zoneAvailable))
        assertInvalid(
            CanonicalSessionGraphV1Validator.validate(
                zoneAvailable.copy(
                    snapshots = listOf(zoneAvailable.snapshots.single().copy(zoneDurationsJson = VALID_ZONE_DURATIONS_WRONG_BUCKET))
                )
            ),
            "invalid_canonical_graph_v1"
        )

        val pausedExcluded = validTerminalGraph().copy(
            phases = listOf(terminalPhase().copy(phaseKind = "paused", phaseIdentityJson = VALID_PAUSED_PHASE_IDENTITY)),
            snapshots = listOf(
                terminalSnapshot().copy(
                    sampleStatus = "canonical_only_excluded",
                    coverageStatus = "no_eligible_duration",
                    primaryPointSampleCount = 0,
                    eligibleDurationMs = 0,
                    coveredDurationMs = 0,
                    coverageBasisPoints = null,
                    weightedBpmMs = null,
                    observedAvgBpm = null,
                    observedMaxBpm = null,
                    highestOffsetMs = null,
                    highestMutationSequence = null,
                    highestSampleSequence = null,
                    phaseAggregatesJson = "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[]}",
                    durationBreakdownJson = duration(0, 0, phaseExcluded = 100, pausedExcluded = 100)
                )
            )
        )
        assertValid(CanonicalSessionGraphV1Validator.validate(pausedExcluded))

        val userExcluded = validTerminalGraph().copy(
            acquisitions = listOf(
                terminalAcquisition().copy(recordingIntent = "user_excluded", intentReason = "user_turned_off")
            ),
            snapshots = listOf(
                terminalSnapshot().copy(
                    sampleStatus = "canonical_only_excluded",
                    coverageStatus = "no_eligible_duration",
                    primaryPointSampleCount = 0,
                    eligibleDurationMs = 0,
                    coveredDurationMs = 0,
                    coverageBasisPoints = null,
                    weightedBpmMs = null,
                    observedAvgBpm = null,
                    observedMaxBpm = null,
                    highestOffsetMs = null,
                    highestMutationSequence = null,
                    highestSampleSequence = null,
                    phaseAggregatesJson = aggregate(0, 0, null, "no_eligible_duration", null, null, null, null),
                    durationBreakdownJson = duration(0, 0, expectedIntent = 0, userExcluded = 100, userTurnedOff = 100)
                )
            )
        )
        assertValid(CanonicalSessionGraphV1Validator.validate(userExcluded))

        listOf(
            terminalSnapshot().copy(coverageBasisPoints = null),
            terminalSnapshot().copy(weightedBpmMs = null),
            terminalSnapshot().copy(observedMaxBpm = null),
            terminalSnapshot().copy(coverageStatus = "partial"),
            terminalSnapshot().copy(sampleStatus = "canonical_only_excluded"),
            terminalSnapshot().copy(phaseAggregatesJson = VALID_PHASE_AGGREGATES.replace("\"weightedBpmMs\":12000", "\"weightedBpmMs\":11999")),
            terminalSnapshot().copy(durationBreakdownJson = VALID_DURATION_BREAKDOWN.replace("\"eligibleCoveredDurationMs\":100", "\"eligibleCoveredDurationMs\":99"))
        ).forEachIndexed { index, snapshot ->
            assertTrue(
                "Analysis required-iff/raw JSON mutation $index unexpectedly validated",
                CanonicalSessionGraphV1Validator.validate(validTerminalGraph().copy(snapshots = listOf(snapshot))) is CanonicalValidationResult.Invalid
            )
        }
    }

    @Test
    fun analysisMembershipUsesTheWholeCanonicalTupleAndKeepsZeroDurationPointsEligible() {
        fun phaseAggregates(maximum: Int, mutationSequence: Long, sampleSequence: Long): String =
            "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[{\"phaseSequence\":1,\"phaseKind\":\"timed_work\",\"eligibleDurationMs\":0,\"coveredDurationMs\":0,\"coverageBasisPoints\":null,\"coverageStatus\":\"no_eligible_duration\",\"conclusionEligible\":false,\"weightedBpmMs\":null,\"observedAvgBpm\":null,\"observedMaxBpm\":$maximum,\"highestOffsetMs\":50,\"highestMutationSequence\":$mutationSequence,\"highestSampleSequence\":$sampleSequence}]}"
        val durationBreakdown =
            "{\"durationBreakdownContractVersion\":1,\"canonicalSessionDurationMs\":100,\"recordingWindowDurationMs\":100,\"notRequestedBeforeRecordingStartMs\":0,\"intentAxis\":{\"expectedRecordingDurationMs\":0,\"userExcludedDurationMs\":100,\"userTurnedOffDurationMs\":100,\"userOptedOutDurationMs\":0,\"userDisconnectedSuppressRecoveryDurationMs\":0},\"phaseAxis\":{\"primaryEligibleDurationMs\":0,\"phaseExcludedDurationMs\":0,\"strengthPrepareExcludedDurationMs\":0,\"pausedExcludedDurationMs\":0},\"primaryAnalysisPartition\":{\"primaryEligibleDurationMs\":0,\"eligibleCoveredDurationMs\":0,\"eligibleUncoveredDurationMs\":0},\"deviceStateDurations\":{\"not_observing\":0,\"no_source_selected\":0,\"permission_required\":0,\"bluetooth_unavailable\":0,\"searching\":0,\"connecting\":0,\"waiting_first_sample\":0,\"live\":100,\"stale\":0,\"reconnecting\":0,\"disconnected\":0,\"technical_failure\":0},\"deviceReasonDurations\":{\"initial_acquisition\":0,\"automatic_recovery\":0,\"source_not_selected\":0,\"source_unavailable\":0,\"permission_missing\":0,\"permission_revoked\":0,\"bluetooth_off\":0,\"platform_unavailable\":0,\"first_sample_timeout\":0,\"sample_stale_timeout\":0,\"unexpected_disconnect\":0,\"connection_timeout\":0,\"measurement_stream_unavailable\":0,\"platform_failure\":0},\"orthogonalityContract\":{\"contractVersion\":1,\"rule\":\"primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum\"}}"
        val graph = validTerminalGraph().copy(
            phases = listOf(
                terminalPhase().copy(
                    id = "before-point",
                    endOffsetMs = 50,
                    endMutationSequence = 1,
                    phaseKind = "paused",
                    phaseIdentityJson = VALID_PAUSED_PHASE_IDENTITY
                ),
                terminalPhase().copy(
                    id = "point",
                    sequence = 1,
                    startOffsetMs = 50,
                    startMutationSequence = 1,
                    endOffsetMs = 50,
                    endMutationSequence = 3
                ),
                terminalPhase().copy(
                    id = "after-point",
                    sequence = 2,
                    startOffsetMs = 50,
                    startMutationSequence = 3,
                    phaseKind = "paused",
                    phaseIdentityJson = VALID_PAUSED_PHASE_IDENTITY
                )
            ),
            acquisitions = listOf(
                terminalAcquisition().copy(
                    id = "before-point",
                    endOffsetMs = 50,
                    endMutationSequence = 1,
                    recordingIntent = "user_excluded",
                    intentReason = "user_turned_off"
                ),
                terminalAcquisition().copy(
                    id = "point",
                    sequence = 1,
                    startOffsetMs = 50,
                    startMutationSequence = 1,
                    endOffsetMs = 50,
                    endMutationSequence = 3
                ),
                terminalAcquisition().copy(
                    id = "after-point",
                    sequence = 2,
                    startOffsetMs = 50,
                    startMutationSequence = 3,
                    recordingIntent = "user_excluded",
                    intentReason = "user_turned_off"
                )
            ),
            samples = listOf(
                HeartRateSampleEntity("recording", 0, 50, 0, 250),
                HeartRateSampleEntity("recording", 1, 50, 1, 130),
                HeartRateSampleEntity("recording", 2, 50, 2, 140),
                HeartRateSampleEntity("recording", 3, 50, 3, 240),
                HeartRateSampleEntity("recording", 4, 50, 4, 230)
            ),
            snapshots = listOf(
                terminalSnapshot().copy(
                    sampleStatus = "primary_points_available",
                    coverageStatus = "no_eligible_duration",
                    canonicalSampleCount = 5,
                    primaryPointSampleCount = 2,
                    eligibleDurationMs = 0,
                    coveredDurationMs = 0,
                    coverageBasisPoints = null,
                    weightedBpmMs = null,
                    observedAvgBpm = null,
                    observedMaxBpm = 140,
                    highestOffsetMs = 50,
                    highestMutationSequence = 2,
                    highestSampleSequence = 2,
                    phaseAggregatesJson = phaseAggregates(140, 2, 2),
                    durationBreakdownJson = durationBreakdown
                )
            )
        )

        assertValid(CanonicalSessionGraphV1Validator.validate(graph))

        val exactSinglePoint = graph.copy(
            phases = graph.phases.mapIndexed { index, phase ->
                when (index) {
                    1 -> phase.copy(endMutationSequence = 2)
                    2 -> phase.copy(startMutationSequence = 2)
                    else -> phase
                }
            },
            acquisitions = graph.acquisitions.mapIndexed { index, acquisition ->
                when (index) {
                    1 -> acquisition.copy(endMutationSequence = 2)
                    2 -> acquisition.copy(startMutationSequence = 2)
                    else -> acquisition
                }
            },
            snapshots = listOf(
                graph.snapshots.single().copy(
                    primaryPointSampleCount = 1,
                    observedMaxBpm = 130,
                    highestMutationSequence = 1,
                    highestSampleSequence = 1,
                    phaseAggregatesJson = phaseAggregates(130, 1, 1)
                )
            )
        )
        assertValid(CanonicalSessionGraphV1Validator.validate(exactSinglePoint))
    }

    @Test
    fun phaseIdentityModeMustMatchTheOwningSessionMode() {
        assertInvalid(
            CanonicalSessionGraphV1Validator.validate(
                validTerminalGraph().copy(
                    session = canonicalTerminalSession().copy(mode = "strength")
                )
            ),
            "invalid_canonical_graph_v1"
        )
    }

    @Test
    fun canonicalGraphRejectsEveryCrossRowBoundaryInsteadOfRepairingIt() {
        val valid = validTerminalGraph()
        val invalidGraphs = listOf(
            valid.copy(
                phases = valid.phases.map { phase -> phase.copy(startOffsetMs = 1) }
            ),
            valid.copy(
                recording = valid.recording?.copy(endedOffsetMs = 99)
            ),
            valid.copy(
                acquisitions = valid.acquisitions.map { acquisition -> acquisition.copy(startMutationSequence = 1) }
            ),
            valid.copy(
                samples = valid.samples.map { sample -> sample.copy(offsetMs = 101) }
            ),
            valid.copy(
                snapshots = valid.snapshots.map { snapshot -> snapshot.copy(inputLastMutationSequence = 3) }
            ),
            valid.copy(snapshots = emptyList()),
            valid.copy(
                recording = valid.recording?.copy(originalAnalysisVersion = null)
            )
        )

        invalidGraphs.forEachIndexed { index, graph ->
            assertTrue(
                "Illegal graph fixture $index unexpectedly validated.",
                CanonicalSessionGraphV1Validator.validate(graph) is CanonicalValidationResult.Invalid
            )
        }
    }

    @Test
    fun everyStartMutationSequenceMustStayWithinTheHeaderInputCut() {
        val openPhasePastCut = WorkoutPhaseIntervalEntity(
            id = "open-phase",
            sessionId = "running",
            sequence = 0,
            startOffsetMs = 0,
            endOffsetMs = null,
            startMutationSequence = 5,
            endMutationSequence = null,
            openMarker = 1,
            phaseKind = "timed_work",
            phaseIdentityJson = VALID_PHASE_IDENTITY
        )
        assertInvalid(
            CanonicalSessionGraphV1Validator.validate(
                CanonicalSessionGraphV1(
                    session = canonicalRunningSession(),
                    phases = listOf(openPhasePastCut)
                )
            ),
            "invalid_canonical_graph_v1"
        )

        val recordingPastCut = HeartRateRecordingEntity(
            recordingId = "running-recording",
            sessionId = "running",
            status = "active",
            startedOffsetMs = 0,
            startedMutationSequence = 5,
            endedOffsetMs = null,
            endedMutationSequence = null,
            sourceContractVersion = 1,
            sourceKind = "ble_hrs",
            acquisitionContractVersion = 1,
            parameterSnapshotVersion = 1
        )
        val acquisitionPastCut = HeartRateAcquisitionIntervalEntity(
            id = "running-acquisition",
            recordingId = "running-recording",
            sequence = 0,
            startOffsetMs = 0,
            endOffsetMs = null,
            startMutationSequence = 5,
            endMutationSequence = null,
            openMarker = 1,
            recordingIntent = "expected_recording",
            intentReason = null,
            deviceState = "live",
            deviceReason = null
        )
        assertInvalid(
            CanonicalSessionGraphV1Validator.validate(
                CanonicalSessionGraphV1(
                    session = canonicalRunningSession(),
                    phases = listOf(openPhasePastCut.copy(startMutationSequence = 0)),
                    recording = recordingPastCut,
                    acquisitions = listOf(acquisitionPastCut)
                )
            ),
            "invalid_canonical_graph_v1"
        )
    }

    @Test
    fun phaseRecordingAcquisitionSampleAndSnapshotCutsRejectTheFullIllegalMatrix() {
        val valid = validSplitTerminalGraph()
        assertValid(CanonicalSessionGraphV1Validator.validate(valid))

        val invalidGraphs = buildList {
            add(valid.copy(phases = valid.phases.mapIndexed { index, phase ->
                if (index == 0) phase.copy(startOffsetMs = 1) else phase
            }))
            add(valid.copy(phases = valid.phases.mapIndexed { index, phase ->
                if (index == 1) phase.copy(startOffsetMs = 51) else phase
            }))
            add(valid.copy(phases = valid.phases.mapIndexed { index, phase ->
                if (index == 1) phase.copy(startOffsetMs = 49) else phase
            }))
            add(valid.copy(phases = valid.phases.mapIndexed { index, phase ->
                if (index == 0) phase.copy(
                    endOffsetMs = null,
                    endMutationSequence = null,
                    openMarker = 1
                ) else phase
            }))
            add(valid.copy(phases = valid.phases.mapIndexed { index, phase ->
                if (index == 1) phase.copy(
                    endOffsetMs = null,
                    endMutationSequence = null,
                    openMarker = 1
                ) else phase
            }))
            add(valid.copy(phases = valid.phases.mapIndexed { index, phase ->
                if (index == 1) phase.copy(endOffsetMs = 99) else phase
            }))
            add(valid.copy(phases = valid.phases.mapIndexed { index, phase ->
                if (index == 1) phase.copy(sequence = 2) else phase
            }))

            add(valid.copy(recording = valid.recording?.copy(startedOffsetMs = 101)))
            add(valid.copy(recording = activeRecordingForTerminalSession()))
            add(valid.copy(recording = valid.recording?.copy(endedOffsetMs = 99)))

            add(valid.copy(acquisitions = valid.acquisitions.mapIndexed { index, interval ->
                if (index == 0) interval.copy(startOffsetMs = 1) else interval
            }))
            add(valid.copy(acquisitions = valid.acquisitions.mapIndexed { index, interval ->
                if (index == 1) interval.copy(startOffsetMs = 51) else interval
            }))
            add(valid.copy(acquisitions = valid.acquisitions.mapIndexed { index, interval ->
                if (index == 1) interval.copy(startOffsetMs = 49) else interval
            }))
            add(valid.copy(acquisitions = valid.acquisitions.mapIndexed { index, interval ->
                if (index == 0) interval.copy(
                    endOffsetMs = null,
                    endMutationSequence = null,
                    openMarker = 1
                ) else interval
            }))
            add(valid.copy(acquisitions = valid.acquisitions.mapIndexed { index, interval ->
                if (index == 1) interval.copy(
                    endOffsetMs = null,
                    endMutationSequence = null,
                    openMarker = 1
                ) else interval
            }))
            add(valid.copy(acquisitions = valid.acquisitions.mapIndexed { index, interval ->
                if (index == 1) interval.copy(endOffsetMs = 99) else interval
            }))
            add(valid.copy(acquisitions = valid.acquisitions.mapIndexed { index, interval ->
                if (index == 1) interval.copy(sequence = 2) else interval
            }))

            add(valid.copy(samples = valid.samples.map { sample -> sample.copy(offsetMs = 101) }))
            add(valid.copy(samples = valid.samples.map { sample -> sample.copy(mutationSequence = 5) }))
            add(valid.copy(samples = valid.samples.map { sample -> sample.copy(recordingId = "other") }))
            add(valid.copy(samples = valid.samples + valid.samples.first().copy(offsetMs = 60)))
            add(valid.copy(samples = valid.samples.map { sample -> sample.copy(bpm = 0) }))

            add(valid.copy(snapshots = valid.snapshots.map { snapshot ->
                snapshot.copy(inputLastMutationSequence = 3)
            }))
            add(valid.copy(snapshots = valid.snapshots.map { snapshot ->
                snapshot.copy(recordingId = "other")
            }))
            add(valid.copy(snapshots = valid.snapshots.map { snapshot ->
                snapshot.copy(sampleStatus = "no_canonical_samples")
            }))
            add(valid.copy(snapshots = emptyList()))
            add(valid.copy(snapshots = valid.snapshots + valid.snapshots.single()))
            add(valid.copy(recording = valid.recording?.copy(originalAnalysisVersion = null)))
        }

        invalidGraphs.forEachIndexed { index, graph ->
            val result = CanonicalSessionGraphV1Validator.validate(graph)
            assertTrue("illegal matrix row $index was accepted: $graph", result is CanonicalValidationResult.Invalid)
            assertEquals("invalid_canonical_graph_v1", (result as CanonicalValidationResult.Invalid).code)
        }
    }

    @Test
    fun canonicalTerminalWithoutHeartRateHasNoSyntheticRecordingIdentity() {
        val noHeartRate = validTerminalGraph().copy(
            recording = null,
            acquisitions = emptyList(),
            samples = emptyList(),
            snapshots = emptyList()
        )
        assertValid(CanonicalSessionGraphV1Validator.validate(noHeartRate))
        assertInvalid(
            CanonicalSessionGraphV1Validator.validate(
                noHeartRate.copy(samples = validTerminalGraph().samples)
            ),
            "invalid_canonical_graph_v1"
        )
    }

    @Test
    fun legacyGraphNeverSynthesizesCanonicalChildren() {
        assertValid(
            CanonicalSessionGraphV1Validator.validate(
                CanonicalSessionGraphV1(session = legacySession("paused"))
            )
        )
        assertInvalid(
            CanonicalSessionGraphV1Validator.validate(
                CanonicalSessionGraphV1(
                    session = legacySession("paused"),
                    phases = listOf(terminalPhase())
                )
            ),
            "legacy_session_has_canonical_children"
        )
    }

    @Test
    fun laterInvalidPhaseAndInvalidSnapshotAlwaysFailTheWholeGraph() {
        val valid = validSplitTerminalGraph()
        val invalidLaterPhase = valid.copy(
            phases = valid.phases.mapIndexed { index, phase ->
                if (index == 1) {
                    phase.copy(
                        phaseIdentityJson = phase.phaseIdentityJson.replace(
                            VALID_PHASE_DIGEST,
                            "0".repeat(64)
                        )
                    )
                } else {
                    phase
                }
            }
        )
        val invalidSnapshot = valid.copy(
            session = valid.session.copy(planSnapshotJson = "${valid.session.planSnapshotJson} ")
        )

        assertValid(CanonicalSessionGraphV1Validator.validate(valid))
        assertInvalid(
            CanonicalSessionGraphV1Validator.validate(invalidLaterPhase),
            "invalid_canonical_graph_v1"
        )
        assertInvalid(
            CanonicalSessionGraphV1Validator.validate(invalidSnapshot),
            "invalid_canonical_graph_v1"
        )
    }

    @Test
    fun preparedGraphValidationIsIsolatedAcrossInvocationsSessionsModesSnapshotsAndThreads() {
        val timedA = validTerminalGraph()
        val invalidB = timedA.copy(
            phases = timedA.phases.map { phase ->
                phase.copy(
                    phaseIdentityJson = phase.phaseIdentityJson.replace(
                        VALID_PHASE_DIGEST,
                        "f".repeat(64)
                    )
                )
            }
        )
        val strengthC = validStrengthRunningGraph()

        assertValid(CanonicalSessionGraphV1Validator.validate(timedA))
        assertInvalid(CanonicalSessionGraphV1Validator.validate(invalidB), "invalid_canonical_graph_v1")
        assertValid(CanonicalSessionGraphV1Validator.validate(strengthC))
        assertValid(CanonicalSessionGraphV1Validator.validate(timedA))

        val executor = Executors.newFixedThreadPool(6)
        try {
            val graphs = List(96) { index ->
                when (index % 3) {
                    0 -> timedA to true
                    1 -> invalidB to false
                    else -> strengthC to true
                }
            }
            val results = executor.invokeAll(
                graphs.map { (graph, _) ->
                    Callable { CanonicalSessionGraphV1Validator.validate(graph) }
                }
            ).map { future -> future.get() }
            graphs.zip(results).forEachIndexed { index, (expectedAndGraph, result) ->
                val expectedValid = expectedAndGraph.second
                assertEquals(
                    "parallel graph $index leaked prepared state",
                    expectedValid,
                    result == CanonicalValidationResult.Valid
                )
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun validTerminalGraph(): CanonicalSessionGraphV1 = CanonicalSessionGraphV1(
        session = canonicalTerminalSession(),
        phases = listOf(terminalPhase()),
        recording = terminalRecording(),
        acquisitions = listOf(terminalAcquisition()),
        samples = listOf(
            HeartRateSampleEntity(
                recordingId = "recording",
                sampleSequence = 0,
                offsetMs = 0,
                mutationSequence = 0,
                bpm = 120
            )
        ),
        snapshots = listOf(terminalSnapshot())
    )

    private fun validSplitTerminalGraph(): CanonicalSessionGraphV1 = validTerminalGraph().copy(
        phases = listOf(
            terminalPhase().copy(id = "phase-0", endOffsetMs = 50, endMutationSequence = 2),
            terminalPhase().copy(
                id = "phase-1",
                sequence = 1,
                startOffsetMs = 50,
                startMutationSequence = 2
            )
        ),
        acquisitions = listOf(
            terminalAcquisition().copy(
                id = "acquisition-0",
                endOffsetMs = 50,
                endMutationSequence = 2
            ),
            terminalAcquisition().copy(
                id = "acquisition-1",
                sequence = 1,
                startOffsetMs = 50,
                startMutationSequence = 2
            )
        ),
        samples = listOf(
            HeartRateSampleEntity("recording", 0, 0, 0, 120),
            HeartRateSampleEntity("recording", 1, 50, 2, 120)
        ),
        snapshots = listOf(
            terminalSnapshot().copy(
                canonicalSampleCount = 2,
                primaryPointSampleCount = 2,
                phaseAggregatesJson = VALID_SPLIT_PHASE_AGGREGATES
            )
        )
    )

    private fun validStrengthRunningGraph(): CanonicalSessionGraphV1 {
        val planSnapshotJson = WorkoutPlanSnapshot(
            title = "Strength",
            mode = WorkoutMode.STRENGTH,
            blocks = listOf(
                StrengthExerciseBlock(
                    id = "strength",
                    order = 0,
                    exerciseId = "squat",
                    sets = listOf(
                        StrengthSetPlan(
                            id = "set",
                            order = 0,
                            kind = StrengthSetKind.WORKING
                        )
                    )
                )
            )
        ).toStorageJson()
        val storage = (PlanSnapshotStorageV1Validator.validate(planSnapshotJson, WorkoutMode.STRENGTH) as
            PlanSnapshotStorageV1ValidationResult.Valid).storage
        val digest = requireNotNull(OrderedStructureSignatureInputV1.digestHexLowercase(storage))
        val identity =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"strength_v1\",\"payloadVersion\":1,\"mode\":\"strength\",\"phaseKind\":\"strength_active_set\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"$digest\"},\"payload\":{\"variant\":\"active_set\",\"blockId\":\"strength\",\"setPlanId\":\"set\",\"plannedExerciseId\":\"squat\",\"actualExerciseId\":\"squat\",\"exerciseSetIndex0\":0,\"globalSetIndex0\":0,\"setKind\":\"working\",\"substitutedFromExerciseId\":null}}"
        return CanonicalSessionGraphV1(
            session = canonicalRunningSession().copy(
                id = "strength-session",
                mode = "strength",
                planSnapshotJson = planSnapshotJson
            ),
            phases = listOf(
                terminalPhase().copy(
                    id = "strength-phase",
                    sessionId = "strength-session",
                    endOffsetMs = null,
                    endMutationSequence = null,
                    openMarker = 1,
                    phaseKind = "strength_active_set",
                    phaseIdentityJson = identity
                )
            )
        )
    }

    private fun activeRecordingForTerminalSession(): HeartRateRecordingEntity =
        terminalRecording().copy(
            status = "active",
            endedOffsetMs = null,
            endedMutationSequence = null,
            originalAnalysisVersion = null
        )

    private fun legacySession(status: String): WorkoutSessionEntity = WorkoutSessionEntity(
        id = "legacy-$status",
        mode = "timed",
        status = status,
        planSnapshotJson = "{\"title\":\"Legacy\",\"mode\":\"timed\",\"blocks\":[]}"
    )

    private fun canonicalRunningSession(): WorkoutSessionEntity = WorkoutSessionEntity(
        id = "running",
        mode = "timed",
        status = "active",
        planSnapshotJson = VALID_PLAN_SNAPSHOT,
        timelineVersion = 1,
        lastDurableOffsetMs = 100,
        lastMutationSequence = 4,
        displayMetadataContractVersion = 1,
        sessionDisplayMetadataJson = VALID_DISPLAY_METADATA
    )

    private fun canonicalTerminalSession(): WorkoutSessionEntity = WorkoutSessionEntity(
        id = "session",
        mode = "timed",
        status = "completed",
        planSnapshotJson = VALID_PLAN_SNAPSHOT,
        timelineVersion = 1,
        lastDurableOffsetMs = 100,
        lastMutationSequence = 4,
        trustedEndOffsetMs = 100,
        terminalReason = "completed",
        displayMetadataContractVersion = 1,
        sessionDisplayMetadataJson = VALID_DISPLAY_METADATA
    )

    private fun terminalPhase(): WorkoutPhaseIntervalEntity = WorkoutPhaseIntervalEntity(
        id = "phase",
        sessionId = "session",
        sequence = 0,
        startOffsetMs = 0,
        endOffsetMs = 100,
        startMutationSequence = 0,
        endMutationSequence = 4,
        openMarker = null,
        phaseKind = "timed_work",
        phaseIdentityJson = VALID_PHASE_IDENTITY
    )

    private fun terminalRecording(): HeartRateRecordingEntity = HeartRateRecordingEntity(
        recordingId = "recording",
        sessionId = "session",
        status = "terminal",
        startedOffsetMs = 0,
        startedMutationSequence = 0,
        endedOffsetMs = 100,
        endedMutationSequence = 4,
        sourceContractVersion = 1,
        sourceKind = "ble_hrs",
        acquisitionContractVersion = 1,
        parameterSnapshotVersion = 1,
        originalAnalysisVersion = 1
    )

    private fun terminalAcquisition(): HeartRateAcquisitionIntervalEntity =
        HeartRateAcquisitionIntervalEntity(
            id = "acquisition",
            recordingId = "recording",
            sequence = 0,
            startOffsetMs = 0,
            endOffsetMs = 100,
            startMutationSequence = 0,
            endMutationSequence = 4,
            openMarker = null,
            recordingIntent = "expected_recording",
            intentReason = null,
            deviceState = "live",
            deviceReason = null
        )

    private fun terminalSnapshot(): HeartRateAnalysisSnapshotEntity =
        HeartRateAnalysisSnapshotEntity(
            recordingId = "recording",
            analysisVersion = 1,
            createdAt = "2026-08-25T00:00:00Z",
            inputLastMutationSequence = 4,
            sampleStatus = "primary_points_available",
            coverageStatus = "normal",
            zoneStatus = "unavailable_no_effective_max",
            canonicalSampleCount = 1,
            primaryPointSampleCount = 1,
            eligibleDurationMs = 100,
            coveredDurationMs = 100,
            coverageBasisPoints = 10000,
            weightedBpmMs = 12000,
            observedAvgBpm = 120,
            observedMaxBpm = 120,
            highestOffsetMs = 0,
            highestMutationSequence = 0,
            highestSampleSequence = 0,
            analysisConfigJson = VALID_ANALYSIS_CONFIG,
            zoneDurationsJson = null,
            phaseAggregatesJson = VALID_PHASE_AGGREGATES,
            durationBreakdownJson = VALID_DURATION_BREAKDOWN,
            qualityReasonsJson = VALID_QUALITY_REASONS
        )

    private fun assertValid(result: CanonicalValidationResult) {
        assertEquals(CanonicalValidationResult.Valid, result)
    }

    private fun assertInvalid(result: CanonicalValidationResult, code: String) {
        assertTrue(result is CanonicalValidationResult.Invalid)
        assertEquals(code, (result as CanonicalValidationResult.Invalid).code)
    }

    private fun assertInvalid(result: CanonicalSessionHeaderV1Result, code: String) {
        assertTrue(result is CanonicalSessionHeaderV1Result.Invalid)
        assertEquals(code, (result as CanonicalSessionHeaderV1Result.Invalid).code)
    }

    private companion object {
        const val VALID_PHASE_DIGEST =
            "38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855"
        const val VALID_DISPLAY_METADATA =
            "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
        const val VALID_PLAN_SNAPSHOT =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Timed\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"block\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[]}],\"preferences\":null,\"followAlong\":null}"
        const val VALID_ZONE_SNAPSHOT =
            "{\"zoneSnapshotContractVersion\":1,\"unit\":\"bpm\",\"effectiveMaxBpm\":180,\"effectiveMaxSource\":\"personal_max\",\"zones\":[{\"zoneId\":\"below_50\",\"lowerBoundBasisPointsInclusive\":null,\"upperBoundBasisPointsExclusive\":5000},{\"zoneId\":\"from_50_to_60\",\"lowerBoundBasisPointsInclusive\":5000,\"upperBoundBasisPointsExclusive\":6000},{\"zoneId\":\"from_60_to_70\",\"lowerBoundBasisPointsInclusive\":6000,\"upperBoundBasisPointsExclusive\":7000},{\"zoneId\":\"from_70_to_80\",\"lowerBoundBasisPointsInclusive\":7000,\"upperBoundBasisPointsExclusive\":8000},{\"zoneId\":\"from_80_to_90\",\"lowerBoundBasisPointsInclusive\":8000,\"upperBoundBasisPointsExclusive\":9000},{\"zoneId\":\"at_or_above_90\",\"lowerBoundBasisPointsInclusive\":9000,\"upperBoundBasisPointsExclusive\":null}]}"
        val VALID_PHASE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"timed_work\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855\"},\"payload\":{\"variant\":\"warmup\",\"compositionVersion\":2,\"compositionBlockId\":\"block\",\"${"timelineStage" + "Id"}\":\"block:warmup\",\"timelineStageKind\":\"warmup\",\"stageGroupId\":\"block:warmup\",\"targetId\":\"block:warmup:target\",\"targetKind\":\"warmup\",\"roundIndex0\":null,\"stageGroupIndex0\":null,\"targetIndex0\":0,\"stageInstanceIndex0\":0,\"${"targetInstance" + "Index0"}\":0,\"stepIndex0\":0}}"
        val VALID_PAUSED_PHASE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"paused\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855\"},\"payload\":{\"variant\":\"paused\",\"compositionVersion\":2,\"compositionBlockId\":null,\"${"timelineStage" + "Id"}\":null,\"timelineStageKind\":null,\"stageGroupId\":null,\"targetId\":null,\"targetKind\":null,\"roundIndex0\":null,\"stageGroupIndex0\":null,\"targetIndex0\":null,\"stageInstanceIndex0\":null,\"${"targetInstance" + "Index0"}\":null,\"stepIndex0\":null}}"
        const val VALID_ZONE_SNAPSHOT_200 =
            "{\"zoneSnapshotContractVersion\":1,\"unit\":\"bpm\",\"effectiveMaxBpm\":200,\"effectiveMaxSource\":\"personal_max\",\"zones\":[{\"zoneId\":\"below_50\",\"lowerBoundBasisPointsInclusive\":null,\"upperBoundBasisPointsExclusive\":5000},{\"zoneId\":\"from_50_to_60\",\"lowerBoundBasisPointsInclusive\":5000,\"upperBoundBasisPointsExclusive\":6000},{\"zoneId\":\"from_60_to_70\",\"lowerBoundBasisPointsInclusive\":6000,\"upperBoundBasisPointsExclusive\":7000},{\"zoneId\":\"from_70_to_80\",\"lowerBoundBasisPointsInclusive\":7000,\"upperBoundBasisPointsExclusive\":8000},{\"zoneId\":\"from_80_to_90\",\"lowerBoundBasisPointsInclusive\":8000,\"upperBoundBasisPointsExclusive\":9000},{\"zoneId\":\"at_or_above_90\",\"lowerBoundBasisPointsInclusive\":9000,\"upperBoundBasisPointsExclusive\":null}]}"
        const val VALID_ZONE_DURATIONS_120_OF_200 =
            "{\"zoneDurationsContractVersion\":1,\"below50DurationMs\":0,\"from50To60DurationMs\":0,\"from60To70DurationMs\":100,\"from70To80DurationMs\":0,\"from80To90DurationMs\":0,\"atOrAbove90DurationMs\":0}"
        const val VALID_ZONE_DURATIONS_WRONG_BUCKET =
            "{\"zoneDurationsContractVersion\":1,\"below50DurationMs\":0,\"from50To60DurationMs\":100,\"from60To70DurationMs\":0,\"from70To80DurationMs\":0,\"from80To90DurationMs\":0,\"atOrAbove90DurationMs\":0}"
        const val VALID_ANALYSIS_CONFIG =
            "{\"analysisConfigContractVersion\":1,\"sampleValidityCapMs\":2500,\"sampleIntervalContractVersion\":1,\"partialLowerBoundBasisPoints\":5000,\"phaseConclusionBasisPoints\":7000,\"normalBasisPoints\":8000,\"coverageThresholdRule\":\"checked_integer_cross_multiply\",\"coverageBasisPointsRule\":\"floor_integer_ratio\",\"displayPercentRule\":\"floor_basis_points_div_100\",\"weightedAverageRule\":\"checked_integer_time_integral\",\"averageDisplayRule\":\"positive_integer_half_up\",\"zeroCoveredRule\":\"null_integral_and_average\",\"observedMaxRule\":\"eligible_canonical_point_first_tie\",\"zoneAttributionContractVersion\":1,\"zoneAttributionRule\":\"checked_cross_multiply_six_zones\",\"statusProjectionContractVersion\":1,\"durationPartitionContractVersion\":1}"
        const val VALID_PHASE_AGGREGATES =
            "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[{\"phaseSequence\":0,\"phaseKind\":\"timed_work\",\"eligibleDurationMs\":100,\"coveredDurationMs\":100,\"coverageBasisPoints\":10000,\"coverageStatus\":\"normal\",\"conclusionEligible\":true,\"weightedBpmMs\":12000,\"observedAvgBpm\":120,\"observedMaxBpm\":120,\"highestOffsetMs\":0,\"highestMutationSequence\":0,\"highestSampleSequence\":0}]}"
        const val VALID_SPLIT_PHASE_AGGREGATES =
            "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[{\"phaseSequence\":0,\"phaseKind\":\"timed_work\",\"eligibleDurationMs\":50,\"coveredDurationMs\":50,\"coverageBasisPoints\":10000,\"coverageStatus\":\"normal\",\"conclusionEligible\":true,\"weightedBpmMs\":6000,\"observedAvgBpm\":120,\"observedMaxBpm\":120,\"highestOffsetMs\":0,\"highestMutationSequence\":0,\"highestSampleSequence\":0},{\"phaseSequence\":1,\"phaseKind\":\"timed_work\",\"eligibleDurationMs\":50,\"coveredDurationMs\":50,\"coverageBasisPoints\":10000,\"coverageStatus\":\"normal\",\"conclusionEligible\":true,\"weightedBpmMs\":6000,\"observedAvgBpm\":120,\"observedMaxBpm\":120,\"highestOffsetMs\":50,\"highestMutationSequence\":2,\"highestSampleSequence\":1}]}"
        const val VALID_DURATION_BREAKDOWN =
            "{\"durationBreakdownContractVersion\":1,\"canonicalSessionDurationMs\":100,\"recordingWindowDurationMs\":100,\"notRequestedBeforeRecordingStartMs\":0,\"intentAxis\":{\"expectedRecordingDurationMs\":100,\"userExcludedDurationMs\":0,\"userTurnedOffDurationMs\":0,\"userOptedOutDurationMs\":0,\"userDisconnectedSuppressRecoveryDurationMs\":0},\"phaseAxis\":{\"primaryEligibleDurationMs\":100,\"phaseExcludedDurationMs\":0,\"strengthPrepareExcludedDurationMs\":0,\"pausedExcludedDurationMs\":0},\"primaryAnalysisPartition\":{\"primaryEligibleDurationMs\":100,\"eligibleCoveredDurationMs\":100,\"eligibleUncoveredDurationMs\":0},\"deviceStateDurations\":{\"not_observing\":0,\"no_source_selected\":0,\"permission_required\":0,\"bluetooth_unavailable\":0,\"searching\":0,\"connecting\":0,\"waiting_first_sample\":0,\"live\":100,\"stale\":0,\"reconnecting\":0,\"disconnected\":0,\"technical_failure\":0},\"deviceReasonDurations\":{\"initial_acquisition\":0,\"automatic_recovery\":0,\"source_not_selected\":0,\"source_unavailable\":0,\"permission_missing\":0,\"permission_revoked\":0,\"bluetooth_off\":0,\"platform_unavailable\":0,\"first_sample_timeout\":0,\"sample_stale_timeout\":0,\"unexpected_disconnect\":0,\"connection_timeout\":0,\"measurement_stream_unavailable\":0,\"platform_failure\":0},\"orthogonalityContract\":{\"contractVersion\":1,\"rule\":\"primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum\"}}"
        const val VALID_QUALITY_REASONS =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[],\"phaseReasons\":[]}"
    }
}
