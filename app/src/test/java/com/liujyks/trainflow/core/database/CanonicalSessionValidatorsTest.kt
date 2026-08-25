package com.liujyks.trainflow.core.database

import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
            add(valid.copy(samples = valid.samples + valid.samples.single().copy(offsetMs = 60)))
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

    private fun validTerminalGraph(): CanonicalSessionGraphV1 = CanonicalSessionGraphV1(
        session = canonicalTerminalSession(),
        phases = listOf(terminalPhase()),
        recording = terminalRecording(),
        acquisitions = listOf(terminalAcquisition()),
        samples = listOf(
            HeartRateSampleEntity(
                recordingId = "recording",
                sampleSequence = 0,
                offsetMs = 50,
                mutationSequence = 2,
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
        )
    )

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
            highestOffsetMs = 50,
            highestMutationSequence = 2,
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
        const val VALID_DISPLAY_METADATA =
            "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
        const val VALID_PLAN_SNAPSHOT =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Timed\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"block\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[]}],\"preferences\":null,\"followAlong\":null}"
        const val VALID_ZONE_SNAPSHOT =
            "{\"zoneSnapshotContractVersion\":1,\"unit\":\"bpm\",\"effectiveMaxBpm\":180,\"effectiveMaxSource\":\"personal_max\",\"zones\":[{\"zoneId\":\"below_50\",\"lowerBoundBasisPointsInclusive\":null,\"upperBoundBasisPointsExclusive\":5000},{\"zoneId\":\"from_50_to_60\",\"lowerBoundBasisPointsInclusive\":5000,\"upperBoundBasisPointsExclusive\":6000},{\"zoneId\":\"from_60_to_70\",\"lowerBoundBasisPointsInclusive\":6000,\"upperBoundBasisPointsExclusive\":7000},{\"zoneId\":\"from_70_to_80\",\"lowerBoundBasisPointsInclusive\":7000,\"upperBoundBasisPointsExclusive\":8000},{\"zoneId\":\"from_80_to_90\",\"lowerBoundBasisPointsInclusive\":8000,\"upperBoundBasisPointsExclusive\":9000},{\"zoneId\":\"at_or_above_90\",\"lowerBoundBasisPointsInclusive\":9000,\"upperBoundBasisPointsExclusive\":null}]}"
        val VALID_PHASE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"timed_work\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855\"},\"payload\":{\"variant\":\"warmup\",\"compositionVersion\":2,\"compositionBlockId\":\"block\",\"${"timelineStage" + "Id"}\":\"block:warmup\",\"timelineStageKind\":\"warmup\",\"stageGroupId\":\"block:warmup\",\"targetId\":\"block:warmup:target\",\"targetKind\":\"warmup\",\"roundIndex0\":null,\"stageGroupIndex0\":null,\"targetIndex0\":0,\"stageInstanceIndex0\":0,\"${"targetInstance" + "Index0"}\":0,\"stepIndex0\":0}}"
        const val VALID_ANALYSIS_CONFIG =
            "{\"analysisConfigContractVersion\":1,\"sampleValidityCapMs\":2500,\"sampleIntervalContractVersion\":1,\"partialLowerBoundBasisPoints\":5000,\"phaseConclusionBasisPoints\":7000,\"normalBasisPoints\":8000,\"coverageThresholdRule\":\"checked_integer_cross_multiply\",\"coverageBasisPointsRule\":\"floor_integer_ratio\",\"displayPercentRule\":\"floor_basis_points_div_100\",\"weightedAverageRule\":\"checked_integer_time_integral\",\"averageDisplayRule\":\"positive_integer_half_up\",\"zeroCoveredRule\":\"null_integral_and_average\",\"observedMaxRule\":\"eligible_canonical_point_first_tie\",\"zoneAttributionContractVersion\":1,\"zoneAttributionRule\":\"checked_cross_multiply_six_zones\",\"statusProjectionContractVersion\":1,\"durationPartitionContractVersion\":1}"
        const val VALID_PHASE_AGGREGATES =
            "{\"phaseAggregatesContractVersion\":1,\"aggregates\":[]}"
        const val VALID_DURATION_BREAKDOWN =
            "{\"durationBreakdownContractVersion\":1,\"canonicalSessionDurationMs\":100,\"recordingWindowDurationMs\":100,\"notRequestedBeforeRecordingStartMs\":0,\"intentAxis\":{\"expectedRecordingDurationMs\":100,\"userExcludedDurationMs\":0,\"userTurnedOffDurationMs\":0,\"userOptedOutDurationMs\":0,\"userDisconnectedSuppressRecoveryDurationMs\":0},\"phaseAxis\":{\"primaryEligibleDurationMs\":100,\"phaseExcludedDurationMs\":0,\"strengthPrepareExcludedDurationMs\":0,\"pausedExcludedDurationMs\":0},\"primaryAnalysisPartition\":{\"primaryEligibleDurationMs\":100,\"eligibleCoveredDurationMs\":100,\"eligibleUncoveredDurationMs\":0},\"deviceStateDurations\":{\"not_observing\":0,\"no_source_selected\":0,\"permission_required\":0,\"bluetooth_unavailable\":0,\"searching\":0,\"connecting\":0,\"waiting_first_sample\":0,\"live\":100,\"stale\":0,\"reconnecting\":0,\"disconnected\":0,\"technical_failure\":0},\"deviceReasonDurations\":{\"initial_acquisition\":0,\"automatic_recovery\":0,\"source_not_selected\":0,\"source_unavailable\":0,\"permission_missing\":0,\"permission_revoked\":0,\"bluetooth_off\":0,\"platform_unavailable\":0,\"first_sample_timeout\":0,\"sample_stale_timeout\":0,\"unexpected_disconnect\":0,\"connection_timeout\":0,\"measurement_stream_unavailable\":0,\"platform_failure\":0},\"orthogonalityContract\":{\"contractVersion\":1,\"rule\":\"primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum\"}}"
        const val VALID_QUALITY_REASONS =
            "{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[],\"phaseReasons\":[]}"
    }
}
