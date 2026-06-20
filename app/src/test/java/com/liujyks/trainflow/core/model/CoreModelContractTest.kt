package com.liujyks.trainflow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreModelContractTest {
    @Test
    fun defaultCountdownCueMatchesFirstVersionContract() {
        val cue = CountdownCue()

        assertTrue(cue.enabled)
        assertEquals(5, cue.thresholdSec)
        assertTrue(cue.soundEnabled)
        assertTrue(cue.vibrationEnabled)
        assertTrue(cue.emphasisAnimationEnabled)
        assertFalse(cue.voiceCueEnabled)
    }

    @Test
    fun strengthDefaultsKeepManualSetStartAndEightToTwelveRepRange() {
        val block = StrengthExerciseBlock(
            id = "strength-bench-press",
            order = 1,
            exerciseId = "barbell-bench-press",
            sets = listOf(
                StrengthSetPlan(
                    id = "bench-working-1",
                    order = 1,
                    kind = StrengthSetKind.WORKING
                )
            )
        )
        val defaultRepRange = RepTarget.Range()

        assertEquals(StrengthSetTimerMode.MANUAL_START, block.setTimerMode)
        assertEquals(8, defaultRepRange.minReps)
        assertEquals(12, defaultRepRange.maxReps)
    }

    @Test
    fun sessionStoresPlanSnapshotAndActualStrengthRecordSeparately() {
        val plannedWeight = WeightValue(value = 60.0, unit = WeightUnit.KG)
        val block = StrengthExerciseBlock(
            id = "strength-bench-press",
            order = 1,
            exerciseId = "barbell-bench-press",
            target = StrengthExerciseTarget(
                weight = plannedWeight,
                repTarget = RepTarget.Range(),
                restAfterSetSec = 90
            ),
            sets = listOf(
                StrengthSetPlan(
                    id = "bench-working-1",
                    order = 1,
                    kind = StrengthSetKind.WORKING,
                    targetWeight = plannedWeight,
                    repTarget = RepTarget.Fixed(reps = 10)
                )
            )
        )
        val plan = WorkoutPlan(
            id = "plan-strength-001",
            mode = WorkoutMode.STRENGTH,
            title = "Chest strength",
            blocks = listOf(block),
            createdAt = "2026-05-21T00:00:00Z",
            updatedAt = "2026-05-21T00:00:00Z"
        )
        val session = WorkoutSession(
            id = "session-001",
            planId = plan.id,
            mode = plan.mode,
            planSnapshot = WorkoutPlanSnapshot(
                title = plan.title,
                mode = plan.mode,
                blocks = plan.blocks
            ),
            status = SessionStatus.COMPLETED,
            strengthSetRecords = listOf(
                StrengthSetRecord(
                    id = "record-001",
                    exerciseId = "barbell-bench-press",
                    sourceSetPlanId = "bench-working-1",
                    setOrder = 1,
                    setKind = StrengthSetKind.WORKING,
                    plannedWeight = plannedWeight,
                    plannedRepTarget = RepTarget.Fixed(reps = 10),
                    actualWeight = WeightValue(value = 62.5, unit = WeightUnit.KG),
                    actualReps = 8,
                    activeDurationSec = 42,
                    actualRestAfterSec = 100,
                    effort = SetEffort.HARD
                )
            )
        )

        assertEquals("plan-strength-001", session.planId)
        assertEquals("Chest strength", session.planSnapshot.title)
        assertEquals(60.0, session.strengthSetRecords.single().plannedWeight!!.value, 0.0)
        assertEquals(62.5, session.strengthSetRecords.single().actualWeight!!.value, 0.0)
        assertEquals(8, session.strengthSetRecords.single().actualReps)
    }

    @Test
    fun commandAndEventBoundariesCoverUiAndFutureConsumers() {
        val command = CommandEnvelope(
            command = WorkoutCommand.ExtendRest(seconds = 30),
            source = CommandSource.UI,
            issuedAt = "2026-05-21T00:10:00Z"
        )
        val event: WorkoutEvent = WorkoutEvent.RestEnding(
            stepId = "rest-1",
            remainingSec = 5
        )

        assertEquals("ui", command.source.contractValue)
        assertEquals(30, (command.command as WorkoutCommand.ExtendRest).seconds)
        assertEquals(5, (event as WorkoutEvent.RestEnding).remainingSec)
    }

    @Test
    fun heartRateStateStaysAbstractAndDeviceAgnostic() {
        val state = HeartRateState(
            kind = HeartRateStateKind.UNAVAILABLE,
            sourceKind = HeartRateSourceKind.NONE,
            unavailableReason = HeartRateUnavailableReason.NO_SOURCE,
            message = "No source"
        )

        assertEquals("unavailable", state.kind.contractValue)
        assertEquals("none", state.sourceKind.contractValue)
        assertEquals("no_source", state.unavailableReason?.contractValue)
        assertEquals(null, state.bpm)
    }
}
