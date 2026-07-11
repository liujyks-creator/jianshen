package com.liujyks.trainflow.core.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class HeartRateFreshnessPolicyTest {
    private val policy = HeartRateFreshnessPolicy()

    @Test
    fun waitingForFirstSampleUsesExactFifteenAndThirtySecondBoundaries() {
        val timeline = HeartRateFreshnessTimeline().notifyEnabled(atElapsedMs = 1_000)

        assertDecision(timeline, 15_999, HeartRateFreshnessKind.WAITING, HeartRateFreshnessReason.WAITING_FIRST_SAMPLE)
        assertDecision(timeline, 16_000, HeartRateFreshnessKind.STALE, HeartRateFreshnessReason.FIRST_SAMPLE_STALE)
        assertDecision(timeline, 30_999, HeartRateFreshnessKind.STALE, HeartRateFreshnessReason.FIRST_SAMPLE_STALE)
        assertDecision(timeline, 31_000, HeartRateFreshnessKind.TECHNICAL_ERROR, HeartRateFreshnessReason.FIRST_SAMPLE_SILENCE)
    }

    @Test
    fun validSampleUsesExactTenAndThirtySecondBoundaries() {
        val timeline = HeartRateFreshnessTimeline()
            .notifyEnabled(atElapsedMs = 1_000)
            .validSample(atElapsedMs = 5_000)

        assertDecision(timeline, 14_999, HeartRateFreshnessKind.LIVE, HeartRateFreshnessReason.LIVE_VALID_SAMPLE)
        assertDecision(timeline, 15_000, HeartRateFreshnessKind.STALE, HeartRateFreshnessReason.SAMPLE_STALE)
        assertDecision(timeline, 34_999, HeartRateFreshnessKind.STALE, HeartRateFreshnessReason.SAMPLE_STALE)
        assertDecision(timeline, 35_000, HeartRateFreshnessKind.TECHNICAL_ERROR, HeartRateFreshnessReason.NOTIFY_SILENCE)
    }

    @Test
    fun eachValidSampleResetsTheMonotonicFreshnessOrigin() {
        val before = HeartRateFreshnessTimeline()
            .notifyEnabled(atElapsedMs = 1_000)
            .validSample(atElapsedMs = 5_000)
        val after = before.validSample(atElapsedMs = 14_000)

        assertDecision(before, 15_000, HeartRateFreshnessKind.STALE, HeartRateFreshnessReason.SAMPLE_STALE)
        assertDecision(after, 15_000, HeartRateFreshnessKind.LIVE, HeartRateFreshnessReason.LIVE_VALID_SAMPLE)
        assertEquals(5_000L, before.lastValidSampleElapsedMs)
        assertEquals(14_000L, after.lastValidSampleElapsedMs)
    }

    @Test
    fun wallClockDisplayTimestampCannotAffectFreshness() {
        val timeline = HeartRateFreshnessTimeline()
            .notifyEnabled(atElapsedMs = 1_000)
            .validSample(atElapsedMs = 2_000)

        val beforeWallClockChange = policy.evaluate(nowElapsedMs = 11_999, timeline = timeline)
        val afterWallClockChange = policy.evaluate(nowElapsedMs = 11_999, timeline = timeline)

        assertEquals(beforeWallClockChange, afterWallClockChange)
        assertEquals(HeartRateFreshnessKind.LIVE, afterWallClockChange.kind)
    }

    @Test
    fun malformedPayloadAndParseFailureDoNotRefreshLastValidSample() {
        val live = HeartRateFreshnessTimeline()
            .notifyEnabled(atElapsedMs = 1_000)
            .validSample(atElapsedMs = 2_000)
        val malformed = live.malformedSample()
        val parseFailed = live.technicalFailure(HeartRateFreshnessReason.PARSE_FAILED)

        assertEquals(2_000L, malformed.lastValidSampleElapsedMs)
        assertEquals(2_000L, parseFailed.lastValidSampleElapsedMs)
        assertDecision(malformed, 12_000, HeartRateFreshnessKind.TECHNICAL_ERROR, HeartRateFreshnessReason.PARSE_FAILED)
        assertDecision(parseFailed, 2_001, HeartRateFreshnessKind.TECHNICAL_ERROR, HeartRateFreshnessReason.PARSE_FAILED)
    }

    @Test
    fun validSampleAfterParseFailureClearsTheFailureAndStartsFreshnessAgain() {
        val recovered = HeartRateFreshnessTimeline()
            .notifyEnabled(atElapsedMs = 1_000)
            .validSample(atElapsedMs = 2_000)
            .technicalFailure(HeartRateFreshnessReason.PARSE_FAILED)
            .validSample(atElapsedMs = 8_000)

        assertEquals(8_000L, recovered.lastValidSampleElapsedMs)
        assertDecision(recovered, 17_999, HeartRateFreshnessKind.LIVE, HeartRateFreshnessReason.LIVE_VALID_SAMPLE)
        assertDecision(recovered, 18_000, HeartRateFreshnessKind.STALE, HeartRateFreshnessReason.SAMPLE_STALE)
    }

    @Test
    fun explicitGattDisconnectRemainsOffline() {
        val disconnected = HeartRateFreshnessTimeline()
            .notifyEnabled(atElapsedMs = 1_000)
            .validSample(atElapsedMs = 2_000)
            .disconnected()

        assertDecision(disconnected, 60_000, HeartRateFreshnessKind.OFFLINE, HeartRateFreshnessReason.GATT_DISCONNECTED)
    }

    @Test
    fun disconnectCannotBypassNotifyTimestampValidation() {
        val disconnected = HeartRateFreshnessTimeline()
            .notifyEnabled(atElapsedMs = 1_000)
            .disconnected()

        assertDecision(
            disconnected,
            999,
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
        )
    }

    @Test
    fun technicalFailureCannotBypassSampleTimestampValidation() {
        val failed = HeartRateFreshnessTimeline()
            .notifyEnabled(atElapsedMs = 1_000)
            .validSample(atElapsedMs = 2_000)
            .technicalFailure(HeartRateFreshnessReason.PARSE_FAILED)

        assertDecision(
            failed,
            1_999,
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
        )
    }

    @Test
    fun failureOnlyTimelineStillRejectsNegativeNow() {
        assertDecision(
            HeartRateFreshnessTimeline().disconnected(),
            -1,
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
        )
    }

    @Test
    fun failureOnlyTimelinesMapFactsWithoutNotifyOrSampleTimestamps() {
        assertDecision(
            HeartRateFreshnessTimeline().disconnected(),
            1_000,
            HeartRateFreshnessKind.OFFLINE,
            HeartRateFreshnessReason.GATT_DISCONNECTED
        )
        assertDecision(
            HeartRateFreshnessTimeline().technicalFailure(HeartRateFreshnessReason.CONNECT_FAILED),
            1_000,
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.CONNECT_FAILED
        )
    }

    @Test
    fun failureFactsStillMapAfterValidMonotonicTimestamps() {
        val validTimeline = HeartRateFreshnessTimeline()
            .notifyEnabled(atElapsedMs = 1_000)
            .validSample(atElapsedMs = 2_000)

        assertDecision(
            validTimeline.disconnected(),
            2_001,
            HeartRateFreshnessKind.OFFLINE,
            HeartRateFreshnessReason.GATT_DISCONNECTED
        )
        assertDecision(
            validTimeline.technicalFailure(HeartRateFreshnessReason.PARSE_FAILED),
            2_001,
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.PARSE_FAILED
        )
    }

    @Test
    fun constructorRejectsEveryNonFailureReasonStoredAsLatestFailure() {
        val nonFailureReasons = listOf(
            HeartRateFreshnessReason.WAITING_FIRST_SAMPLE,
            HeartRateFreshnessReason.LIVE_VALID_SAMPLE,
            HeartRateFreshnessReason.FIRST_SAMPLE_STALE,
            HeartRateFreshnessReason.SAMPLE_STALE
        )

        nonFailureReasons.forEach { reason ->
            val invalidTimeline = HeartRateFreshnessTimeline(latestFailureReason = reason)

            assertDecision(
                invalidTimeline,
                1_000,
                HeartRateFreshnessKind.TECHNICAL_ERROR,
                HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
            )
        }
    }

    @Test
    fun copyRejectsNonFailureReasonEvenWhenTimestampsAreValid() {
        val validTimeline = HeartRateFreshnessTimeline()
            .notifyEnabled(atElapsedMs = 1_000)
            .validSample(atElapsedMs = 2_000)
        val invalidTimeline = validTimeline.copy(
            latestFailureReason = HeartRateFreshnessReason.FIRST_SAMPLE_STALE
        )

        assertDecision(
            invalidTimeline,
            2_001,
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
        )
    }

    @Test
    fun invalidFailureReasonWithInvalidMonotonicTimestampFailsClosedStably() {
        val invalidTimeline = HeartRateFreshnessTimeline(
            notifyEnabledAtElapsedMs = 2_000,
            latestFailureReason = HeartRateFreshnessReason.LIVE_VALID_SAMPLE
        )

        assertDecision(
            invalidTimeline,
            1_999,
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
        )
    }

    @Test
    fun constructorAndCopyKeepValidFailureFacts() {
        val disconnected = HeartRateFreshnessTimeline(
            latestFailureReason = HeartRateFreshnessReason.GATT_DISCONNECTED
        )
        val technicalFailure = HeartRateFreshnessTimeline().copy(
            latestFailureReason = HeartRateFreshnessReason.CONNECT_FAILED
        )

        assertDecision(
            disconnected,
            1_000,
            HeartRateFreshnessKind.OFFLINE,
            HeartRateFreshnessReason.GATT_DISCONNECTED
        )
        assertDecision(
            technicalFailure,
            1_000,
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.CONNECT_FAILED
        )
    }

    @Test
    fun technicalFailureStillNormalizesNonTechnicalReason() {
        val normalized = HeartRateFreshnessTimeline()
            .technicalFailure(HeartRateFreshnessReason.LIVE_VALID_SAMPLE)

        assertDecision(
            normalized,
            1_000,
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
        )
    }

    @Test
    fun failureTimelineRejectsSampleBeforeNotifyEnabled() {
        val failed = HeartRateFreshnessTimeline(
            notifyEnabledAtElapsedMs = 2_000,
            lastValidSampleElapsedMs = 1_999,
            latestFailureReason = HeartRateFreshnessReason.GATT_DISCONNECTED
        )

        assertDecision(
            failed,
            3_000,
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
        )
    }

    @Test
    fun failureTimelineRejectsNegativePresentTimestamps() {
        val cases = listOf(
            HeartRateFreshnessTimeline(
                notifyEnabledAtElapsedMs = -1,
                latestFailureReason = HeartRateFreshnessReason.GATT_DISCONNECTED
            ),
            HeartRateFreshnessTimeline(
                notifyEnabledAtElapsedMs = 0,
                lastValidSampleElapsedMs = -1,
                latestFailureReason = HeartRateFreshnessReason.PARSE_FAILED
            )
        )

        cases.forEach { timeline ->
            assertDecision(
                timeline,
                1_000,
                HeartRateFreshnessKind.TECHNICAL_ERROR,
                HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
            )
        }
    }

    @Test
    fun technicalFailuresRemainTechnicalErrors() {
        val reasons = listOf(
            HeartRateFreshnessReason.CONNECT_FAILED,
            HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED,
            HeartRateFreshnessReason.CCCD_FAILED,
            HeartRateFreshnessReason.NOTIFY_SILENCE,
            HeartRateFreshnessReason.PARSE_FAILED
        )

        reasons.forEach { reason ->
            val timeline = HeartRateFreshnessTimeline().technicalFailure(reason)
            assertDecision(timeline, 1_000, HeartRateFreshnessKind.TECHNICAL_ERROR, reason)
        }
    }

    @Test
    fun retryExhaustedDoesNotReplaceTheLatestFact() {
        val disconnected = HeartRateFreshnessTimeline().disconnected()
        val technicalError = HeartRateFreshnessTimeline()
            .technicalFailure(HeartRateFreshnessReason.CONNECT_FAILED)

        assertEquals(disconnected, disconnected.retryExhausted())
        assertEquals(technicalError, technicalError.retryExhausted())
        assertDecision(disconnected.retryExhausted(), 1_000, HeartRateFreshnessKind.OFFLINE, HeartRateFreshnessReason.GATT_DISCONNECTED)
        assertDecision(technicalError.retryExhausted(), 1_000, HeartRateFreshnessKind.TECHNICAL_ERROR, HeartRateFreshnessReason.CONNECT_FAILED)
    }

    @Test
    fun reasonCodesAreStableAndDoNotExposeGattStatusValues() {
        assertEquals("gatt_disconnected", HeartRateFreshnessReason.GATT_DISCONNECTED.code)
        assertEquals("connect_failed", HeartRateFreshnessReason.CONNECT_FAILED.code)
        assertEquals("service_discovery_failed", HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED.code)
        assertEquals("cccd_failed", HeartRateFreshnessReason.CCCD_FAILED.code)
        assertEquals("first_sample_silence", HeartRateFreshnessReason.FIRST_SAMPLE_SILENCE.code)
        assertEquals("notify_silence", HeartRateFreshnessReason.NOTIFY_SILENCE.code)
        assertEquals("parse_failed", HeartRateFreshnessReason.PARSE_FAILED.code)
    }

    @Test
    fun invalidTimeInputsFailClosedWithoutFabricatingLiveData() {
        val cases = listOf(
            -1L to HeartRateFreshnessTimeline().notifyEnabled(atElapsedMs = 0),
            1_000L to HeartRateFreshnessTimeline(),
            2_000L to HeartRateFreshnessTimeline(lastValidSampleElapsedMs = 1_000),
            999L to HeartRateFreshnessTimeline().notifyEnabled(atElapsedMs = 1_000),
            1_999L to HeartRateFreshnessTimeline()
                .notifyEnabled(atElapsedMs = 1_000)
                .validSample(atElapsedMs = 2_000),
            2_000L to HeartRateFreshnessTimeline(
                notifyEnabledAtElapsedMs = 1_500,
                lastValidSampleElapsedMs = 1_000
            ),
            2_000L to HeartRateFreshnessTimeline(notifyEnabledAtElapsedMs = -1)
        )

        cases.forEach { (now, timeline) ->
            assertDecision(
                timeline,
                now,
                HeartRateFreshnessKind.TECHNICAL_ERROR,
                HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
            )
        }
    }

    @Test
    fun evaluationAndTransitionsAreImmutableAndProduceNoActionSideEffects() {
        val original = HeartRateFreshnessTimeline().notifyEnabled(atElapsedMs = 1_000)
        val updated = original.validSample(atElapsedMs = 2_000)

        assertNotSame(original, updated)
        assertEquals(null, original.lastValidSampleElapsedMs)
        assertEquals(2_000L, updated.lastValidSampleElapsedMs)
        assertEquals(
            HeartRateFreshnessDecision(
                kind = HeartRateFreshnessKind.LIVE,
                reason = HeartRateFreshnessReason.LIVE_VALID_SAMPLE
            ),
            policy.evaluate(nowElapsedMs = 2_000, timeline = updated)
        )
    }

    private fun assertDecision(
        timeline: HeartRateFreshnessTimeline,
        nowElapsedMs: Long,
        expectedKind: HeartRateFreshnessKind,
        expectedReason: HeartRateFreshnessReason
    ) {
        assertEquals(
            HeartRateFreshnessDecision(expectedKind, expectedReason),
            policy.evaluate(nowElapsedMs = nowElapsedMs, timeline = timeline)
        )
    }
}
