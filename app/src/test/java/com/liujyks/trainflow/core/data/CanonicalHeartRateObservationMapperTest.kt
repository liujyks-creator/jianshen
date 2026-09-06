package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.database.AcquisitionV1Validator
import com.liujyks.trainflow.core.database.CanonicalValidationResult
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.health.HeartRateObservationCause
import org.junit.Assert.*
import org.junit.Test

class CanonicalHeartRateObservationMapperTest {
    @Test
    fun everyProductionCauseMapsToIndependentLiteralPairAcceptedByExistingValidator() {
        val expected = listOf(
            Triple(HeartRateObservationCause.NOT_OBSERVING, "not_observing", null),
            Triple(HeartRateObservationCause.NO_SOURCE_SELECTED, "no_source_selected", "source_not_selected"),
            Triple(HeartRateObservationCause.PERMISSION_MISSING, "permission_required", "permission_missing"),
            Triple(HeartRateObservationCause.PERMISSION_REVOKED, "permission_required", "permission_revoked"),
            Triple(HeartRateObservationCause.BLUETOOTH_OFF, "bluetooth_unavailable", "bluetooth_off"),
            Triple(HeartRateObservationCause.PLATFORM_UNAVAILABLE, "bluetooth_unavailable", "platform_unavailable"),
            Triple(HeartRateObservationCause.INITIAL_SEARCH, "searching", "initial_acquisition"),
            Triple(HeartRateObservationCause.RECOVERY_SEARCH, "searching", "automatic_recovery"),
            Triple(HeartRateObservationCause.INITIAL_CONNECT, "connecting", "initial_acquisition"),
            Triple(HeartRateObservationCause.RECOVERY_CONNECT, "connecting", "automatic_recovery"),
            Triple(HeartRateObservationCause.INITIAL_WAIT, "waiting_first_sample", "initial_acquisition"),
            Triple(HeartRateObservationCause.RECOVERY_WAIT, "waiting_first_sample", "automatic_recovery"),
            Triple(HeartRateObservationCause.LIVE, "live", null),
            Triple(HeartRateObservationCause.FIRST_SAMPLE_TIMEOUT, "stale", "first_sample_timeout"),
            Triple(HeartRateObservationCause.SAMPLE_STALE_TIMEOUT, "stale", "sample_stale_timeout"),
            Triple(HeartRateObservationCause.RECOVERY_WAITING, "reconnecting", "automatic_recovery"),
            Triple(HeartRateObservationCause.DISCONNECTED, "disconnected", null),
            Triple(HeartRateObservationCause.SOURCE_UNAVAILABLE, "disconnected", "source_unavailable"),
            Triple(HeartRateObservationCause.UNEXPECTED_DISCONNECT, "disconnected", "unexpected_disconnect"),
            Triple(HeartRateObservationCause.MEASUREMENT_STREAM_UNAVAILABLE, "technical_failure", "measurement_stream_unavailable"),
            Triple(HeartRateObservationCause.PLATFORM_FAILURE, "technical_failure", "platform_failure")
        )
        assertEquals(HeartRateObservationCause.entries.toSet(), expected.map { it.first }.toSet())
        expected.forEach { (cause, state, reason) ->
            val actual = CanonicalHeartRateObservationMapper.map(cause)
            assertEquals(cause.name, state, actual.deviceState)
            assertEquals(cause.name, reason, actual.deviceReason)
            assertEquals(cause.name, CanonicalValidationResult.Valid, AcquisitionV1Validator.validate(interval(actual.deviceState, actual.deviceReason)))
        }
    }

    @Test
    fun schemaAllowsNullAndConnectionTimeoutButRejectsUnknownAndCrossPairs() {
        val matrix = listOf(
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
            "disconnected" to setOf("source_unavailable", "unexpected_disconnect", "connection_timeout"),
            "technical_failure" to setOf("measurement_stream_unavailable", "platform_failure")
        )
        val reasons = matrix.flatMap { it.second }.toSet() + "unknown_reason"
        matrix.forEach { (state, allowed) ->
            assertEquals(CanonicalValidationResult.Valid, AcquisitionV1Validator.validate(interval(state, null)))
            reasons.forEach { reason ->
                assertEquals("$state/$reason", reason in allowed, AcquisitionV1Validator.validate(interval(state, reason)) == CanonicalValidationResult.Valid)
            }
        }
        assertTrue(AcquisitionV1Validator.validate(interval("unknown_state", null)) is CanonicalValidationResult.Invalid)
        assertTrue(HeartRateObservationCause.entries.none { CanonicalHeartRateObservationMapper.map(it).deviceReason == "connection_timeout" })
    }

    private fun interval(state: String, reason: String?) = HeartRateAcquisitionIntervalEntity(
        id = "interval", recordingId = "recording", sequence = 0,
        startOffsetMs = 0, startMutationSequence = 0, endOffsetMs = null,
        endMutationSequence = null, openMarker = 1,
        recordingIntent = "expected_recording", intentReason = null,
        deviceState = state, deviceReason = reason
    )
}
