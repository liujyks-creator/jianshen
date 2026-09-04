package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.health.HeartRateRuntimeObservationCause
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CanonicalHeartRateObservationMapperTest {
    @Test
    fun everyReachableCauseMapsToTheIndependentTwelveStateFourteenReasonTable() {
        val cases = listOf(
            Case(HeartRateRuntimeObservationCause.NOT_OBSERVING, "not_observing", null),
            Case(HeartRateRuntimeObservationCause.NO_SOURCE_SELECTED, "no_source_selected", "source_not_selected"),
            Case(HeartRateRuntimeObservationCause.PERMISSION_MISSING, "permission_required", "permission_missing"),
            Case(HeartRateRuntimeObservationCause.PERMISSION_REVOKED, "permission_required", "permission_revoked"),
            Case(HeartRateRuntimeObservationCause.BLUETOOTH_OFF, "bluetooth_unavailable", "bluetooth_off"),
            Case(HeartRateRuntimeObservationCause.PLATFORM_UNAVAILABLE, "bluetooth_unavailable", "platform_unavailable"),
            Case(HeartRateRuntimeObservationCause.INITIAL_SEARCHING, "searching", "initial_acquisition"),
            Case(HeartRateRuntimeObservationCause.RECOVERY_SEARCHING, "searching", "automatic_recovery"),
            Case(HeartRateRuntimeObservationCause.INITIAL_CONNECTING, "connecting", "initial_acquisition"),
            Case(HeartRateRuntimeObservationCause.RECOVERY_CONNECTING, "connecting", "automatic_recovery"),
            Case(HeartRateRuntimeObservationCause.INITIAL_WAITING_FIRST_SAMPLE, "waiting_first_sample", "initial_acquisition"),
            Case(HeartRateRuntimeObservationCause.RECOVERY_WAITING_FIRST_SAMPLE, "waiting_first_sample", "automatic_recovery"),
            Case(HeartRateRuntimeObservationCause.LIVE, "live", null),
            Case(HeartRateRuntimeObservationCause.FIRST_SAMPLE_TIMEOUT, "stale", "first_sample_timeout"),
            Case(HeartRateRuntimeObservationCause.SAMPLE_STALE_TIMEOUT, "stale", "sample_stale_timeout"),
            Case(HeartRateRuntimeObservationCause.RECOVERY_RECONNECTING, "reconnecting", "automatic_recovery"),
            Case(HeartRateRuntimeObservationCause.UNEXPECTED_DISCONNECT_RECONNECTING, "reconnecting", "unexpected_disconnect"),
            Case(HeartRateRuntimeObservationCause.SOURCE_UNAVAILABLE, "disconnected", "source_unavailable"),
            Case(HeartRateRuntimeObservationCause.UNEXPECTED_DISCONNECT, "disconnected", "unexpected_disconnect"),
            Case(HeartRateRuntimeObservationCause.MEASUREMENT_STREAM_UNAVAILABLE, "technical_failure", "measurement_stream_unavailable"),
            Case(HeartRateRuntimeObservationCause.PLATFORM_FAILURE, "technical_failure", "platform_failure")
        )

        cases.forEach { case ->
            val actual = CanonicalHeartRateObservationMapper.mapCause(case.cause)
            assertEquals(case.state, actual.deviceState)
            assertEquals(case.reason, actual.deviceReason)
        }
        assertEquals(12, cases.map { it.state }.distinct().size)
        val reasons = cases.mapNotNull { it.reason }.toSet()
        assertEquals(13, reasons.size)
        assertFalse("connection_timeout is schema-valid but production-unreachable", "connection_timeout" in reasons)
    }

    @Test
    fun recordingIntentIsOrthogonalToTheDevicePairAndUsesOnlyTheLiteralExclusionReason() {
        val expected = CanonicalHeartRateObservationMapper.acquisition(
            cause = HeartRateRuntimeObservationCause.SAMPLE_STALE_TIMEOUT,
            recordingExpected = true,
            userExclusionReason = null
        )
        val excluded = CanonicalHeartRateObservationMapper.acquisition(
            cause = HeartRateRuntimeObservationCause.SAMPLE_STALE_TIMEOUT,
            recordingExpected = false,
            userExclusionReason = "user_turned_off"
        )

        assertEquals("expected_recording", expected.recordingIntent)
        assertNull(expected.intentReason)
        assertEquals("user_excluded", excluded.recordingIntent)
        assertEquals("user_turned_off", excluded.intentReason)
        assertEquals(expected.deviceState, excluded.deviceState)
        assertEquals(expected.deviceReason, excluded.deviceReason)
    }

    private data class Case(
        val cause: HeartRateRuntimeObservationCause,
        val state: String,
        val reason: String?
    )
}
