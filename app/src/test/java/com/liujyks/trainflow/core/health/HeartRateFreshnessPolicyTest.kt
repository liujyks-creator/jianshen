package com.liujyks.trainflow.core.health

import com.liujyks.trainflow.core.model.HeartRateFact
import com.liujyks.trainflow.core.model.HeartRateTechnicalFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateFreshnessPolicyTest {
    private val config = HeartRateFreshnessConfig(
        firstSampleWaitingBoundaryMs = 3_000,
        liveFreshnessBoundaryMs = 2_500
    )
    private val policy = HeartRateFreshnessPolicy(config)

    @Test
    fun notifyEnabledWaitsUntilExactProvisionalBoundaryThenInterrupts() {
        val timeline = HeartRateFreshnessTimeline().notifyEnabled(atElapsedMs = 1_000)

        assertDecision(timeline, 1_000, HeartRateFreshnessKind.WAITING)
        assertDecision(timeline, 3_999, HeartRateFreshnessKind.WAITING)
        val boundary = assertDecision(timeline, 4_000, HeartRateFreshnessKind.DATA_INTERRUPTED)
        val after = assertDecision(timeline, 40_000, HeartRateFreshnessKind.DATA_INTERRUPTED)

        assertEquals(HeartRateFreshnessReason.FIRST_SAMPLE_INTERRUPTED, boundary.reason)
        assertEquals(HeartRateFreshnessReason.FIRST_SAMPLE_INTERRUPTED, after.reason)
        assertNull(boundary.bpm)
        assertNull(boundary.measuredAt)
    }

    @Test
    fun firstValidSampleIsLiveAndExactFreshnessBoundaryClearsReading() {
        val timeline = sampleTimeline(sampleAt = 2_000, bpm = 88)

        val fresh = assertDecision(timeline, 4_499, HeartRateFreshnessKind.LIVE)
        val boundary = assertDecision(timeline, 4_500, HeartRateFreshnessKind.DATA_INTERRUPTED)
        val after = assertDecision(timeline, 30_000, HeartRateFreshnessKind.DATA_INTERRUPTED)

        assertEquals(88, fresh.bpm)
        assertEquals("wall-A", fresh.measuredAt)
        assertNull(boundary.bpm)
        assertNull(boundary.measuredAt)
        assertNull(after.bpm)
        assertEquals(HeartRateFreshnessReason.SAMPLE_INTERRUPTED, after.reason)
    }

    @Test
    fun eachValidSampleResetsMonotonicOriginAndCurrentReading() {
        val first = sampleTimeline(sampleAt = 2_000, bpm = 88)
        val second = first.validSample(
            atElapsedMs = 4_000,
            bpm = 89,
            measuredAt = "wall-B"
        )

        assertDecision(first, 4_500, HeartRateFreshnessKind.DATA_INTERRUPTED)
        val live = assertDecision(second, 4_500, HeartRateFreshnessKind.LIVE)
        assertEquals(4_000L, second.lastValidSampleElapsedMs)
        assertEquals(89, live.bpm)
        assertEquals("wall-B", live.measuredAt)
    }

    @Test
    fun malformedBeforeFirstSampleDoesNotRefreshWaitingOriginOrCreateError() {
        val waiting = HeartRateFreshnessTimeline().notifyEnabled(atElapsedMs = 1_000)
        val malformed = waiting.malformedSample()

        assertEquals(1, malformed.malformedSampleCount)
        assertNull(malformed.lastValidSampleElapsedMs)
        assertDecision(malformed, 3_999, HeartRateFreshnessKind.WAITING)
        assertDecision(malformed, 4_000, HeartRateFreshnessKind.DATA_INTERRUPTED)
    }

    @Test
    fun consecutiveMalformedSamplesDoNotMoveOriginalLiveDeadline() {
        val live = sampleTimeline(sampleAt = 2_000, bpm = 88)
        val malformed = live.malformedSample().malformedSample().malformedSample()

        assertEquals(3, malformed.malformedSampleCount)
        assertEquals(2_000L, malformed.lastValidSampleElapsedMs)
        assertDecision(malformed, 4_499, HeartRateFreshnessKind.LIVE)
        assertDecision(malformed, 4_500, HeartRateFreshnessKind.DATA_INTERRUPTED)
    }

    @Test
    fun validSampleAfterMalformedEstablishesNewOriginNormally() {
        val recovered = sampleTimeline(sampleAt = 2_000, bpm = 88)
            .malformedSample()
            .validSample(atElapsedMs = 5_000, bpm = 90, measuredAt = "wall-C")

        val live = assertDecision(recovered, 7_499, HeartRateFreshnessKind.LIVE)
        assertDecision(recovered, 7_500, HeartRateFreshnessKind.DATA_INTERRUPTED)
        assertEquals(90, live.bpm)
        assertEquals(1, recovered.malformedSampleCount)
    }

    @Test
    fun explicitDisconnectTechnicalFailureAndIntentionalStopAreIndependentFacts() {
        val live = sampleTimeline(sampleAt = 2_000, bpm = 88)
        val disconnected = live.explicitDisconnect(atElapsedMs = 2_100)
        val failed = live.technicalFailure(
            reason = HeartRateFreshnessReason.PLATFORM_FAILURE,
            atElapsedMs = 2_100
        )
        val stopped = live.intentionalStop(atElapsedMs = 2_100)

        assertDecision(disconnected, 60_000, HeartRateFreshnessKind.LINK_DISCONNECTED)
        assertDecision(failed, 60_000, HeartRateFreshnessKind.TECHNICAL_FAILURE)
        assertDecision(stopped, 60_000, HeartRateFreshnessKind.INTENTIONAL_STOP)

        listOf(disconnected, failed, stopped).forEach { timeline ->
            val decision = policy.evaluate(60_000, timeline)
            assertNull(decision.bpm)
            assertNull(decision.measuredAt)
        }
    }

    @Test
    fun silenceNeverEscalatesToTechnicalFailureWithoutIndependentFailureFact() {
        val waiting = HeartRateFreshnessTimeline().notifyEnabled(atElapsedMs = 1_000)
        val live = sampleTimeline(sampleAt = 2_000, bpm = 88)

        assertDecision(waiting, 300_000, HeartRateFreshnessKind.DATA_INTERRUPTED)
        assertDecision(live, 300_000, HeartRateFreshnessKind.DATA_INTERRUPTED)
    }

    @Test
    fun wallClockDisplayChangesCannotAffectFreshness() {
        val firstWall = sampleTimeline(sampleAt = 2_000, bpm = 88)
        val changedWall = firstWall.copy(lastValidMeasuredAt = "2099-01-01T00:00:00Z")

        val firstDecision = policy.evaluate(4_499, firstWall)
        val changedDecision = policy.evaluate(4_499, changedWall)

        assertEquals(firstDecision.kind, changedDecision.kind)
        assertEquals(firstDecision.reason, changedDecision.reason)
        assertEquals(HeartRateFreshnessKind.LIVE, changedDecision.kind)
    }

    @Test
    fun invalidMonotonicAndImpossibleStructuresFailClosedWithoutThrowing() {
        val cases = listOf(
            -1L to HeartRateFreshnessTimeline().notifyEnabled(0),
            999L to HeartRateFreshnessTimeline().notifyEnabled(1_000),
            1_999L to sampleTimeline(sampleAt = 2_000, bpm = 88),
            3_000L to HeartRateFreshnessTimeline(
                notifyEnabledAtElapsedMs = 2_000,
                lastValidSampleElapsedMs = 1_999,
                lastValidBpm = 88,
                lastValidMeasuredAt = "wall"
            ),
            3_000L to HeartRateFreshnessTimeline(
                notifyEnabledAtElapsedMs = 1_000,
                lastValidSampleElapsedMs = 2_000,
                lastValidBpm = null,
                lastValidMeasuredAt = "wall"
            ),
            3_000L to HeartRateFreshnessTimeline(
                notifyEnabledAtElapsedMs = 1_000,
                terminalReason = HeartRateFreshnessReason.EXPLICIT_LINK_DISCONNECT,
                terminalAtElapsedMs = 999
            ),
            3_000L to HeartRateFreshnessTimeline(malformedSampleCount = -1)
        )

        cases.forEach { (now, timeline) ->
            val decision = policy.evaluate(now, timeline)
            assertEquals(HeartRateFreshnessKind.TECHNICAL_FAILURE, decision.kind)
            assertEquals(HeartRateFreshnessReason.INVALID_MONOTONIC_TIME, decision.reason)
            assertNull(decision.bpm)
            assertNull(decision.measuredAt)
        }
    }

    @Test
    fun invalidConfigurationFailsClosedAndContainsNoE16Thresholds() {
        val timeline = sampleTimeline(sampleAt = 2_000, bpm = 88)
        val invalidPolicy = HeartRateFreshnessPolicy(
            HeartRateFreshnessConfig(
                firstSampleWaitingBoundaryMs = 0,
                liveFreshnessBoundaryMs = -1
            )
        )

        assertEquals(3_000L, config.firstSampleWaitingBoundaryMs)
        assertEquals(2_500L, config.liveFreshnessBoundaryMs)
        assertTrue(
            listOf(config.firstSampleWaitingBoundaryMs, config.liveFreshnessBoundaryMs)
                .none { it == 10_000L || it == 15_000L || it == 30_000L }
        )
        assertEquals(
            HeartRateFreshnessReason.INVALID_MONOTONIC_TIME,
            invalidPolicy.evaluate(2_001, timeline).reason
        )
    }

    @Test
    fun freshnessDecisionMapsThroughRuntimeFactAndClearsOldValues() {
        val liveTimeline = sampleTimeline(sampleAt = 2_000, bpm = 88)
        val source = HeartRateSourceHint("id", "Band")
        val liveState = policy.evaluate(4_499, liveTimeline)
            .toRuntimeFact(source)
            .toHeartRateState()
        val interruptedState = policy.evaluate(4_500, liveTimeline)
            .toRuntimeFact(source)
            .toHeartRateState()
        val invalidState = policy.evaluate(1_999, liveTimeline)
            .toRuntimeFact(source)
            .toHeartRateState()

        assertEquals(HeartRateFact.LIVE, liveState.fact)
        assertEquals(88, liveState.bpm)
        assertEquals(HeartRateFact.DATA_INTERRUPTED, interruptedState.fact)
        assertNull(interruptedState.bpm)
        assertNull(interruptedState.measuredAt)
        assertEquals(HeartRateFact.TECHNICAL_FAILURE, invalidState.fact)
        assertEquals(HeartRateTechnicalFailure.INVALID_MONOTONIC_TIME, invalidState.technicalFailure)
    }

    @Test
    fun transitionsRemainImmutableAndCounterSaturates() {
        val original = HeartRateFreshnessTimeline().notifyEnabled(1_000)
        val updated = original.malformedSample()
        val saturated = updated.copy(malformedSampleCount = Int.MAX_VALUE).malformedSample()

        assertNotSame(original, updated)
        assertEquals(0, original.malformedSampleCount)
        assertEquals(1, updated.malformedSampleCount)
        assertEquals(Int.MAX_VALUE, saturated.malformedSampleCount)
    }

    private fun sampleTimeline(sampleAt: Long, bpm: Int) = HeartRateFreshnessTimeline()
        .notifyEnabled(atElapsedMs = 1_000)
        .validSample(atElapsedMs = sampleAt, bpm = bpm, measuredAt = "wall-A")

    private fun assertDecision(
        timeline: HeartRateFreshnessTimeline,
        nowElapsedMs: Long,
        expectedKind: HeartRateFreshnessKind
    ): HeartRateFreshnessDecision {
        val decision = policy.evaluate(nowElapsedMs, timeline)
        assertEquals(expectedKind, decision.kind)
        return decision
    }
}
