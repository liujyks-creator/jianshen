package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.health.HeartRateRuntimeObservationCause

internal data class CanonicalHeartRateDevicePair(
    val deviceState: String,
    val deviceReason: String?
)

internal data class CanonicalHeartRateAcquisitionFact(
    val recordingIntent: String,
    val intentReason: String?,
    val deviceState: String,
    val deviceReason: String?
)

internal object CanonicalHeartRateObservationMapper {
    fun mapCause(cause: HeartRateRuntimeObservationCause): CanonicalHeartRateDevicePair =
        when (cause) {
            HeartRateRuntimeObservationCause.NOT_OBSERVING -> pair("not_observing")
            HeartRateRuntimeObservationCause.NO_SOURCE_SELECTED ->
                pair("no_source_selected", "source_not_selected")
            HeartRateRuntimeObservationCause.PERMISSION_MISSING ->
                pair("permission_required", "permission_missing")
            HeartRateRuntimeObservationCause.PERMISSION_REVOKED ->
                pair("permission_required", "permission_revoked")
            HeartRateRuntimeObservationCause.BLUETOOTH_OFF ->
                pair("bluetooth_unavailable", "bluetooth_off")
            HeartRateRuntimeObservationCause.PLATFORM_UNAVAILABLE ->
                pair("bluetooth_unavailable", "platform_unavailable")
            HeartRateRuntimeObservationCause.INITIAL_SEARCHING ->
                pair("searching", "initial_acquisition")
            HeartRateRuntimeObservationCause.RECOVERY_SEARCHING ->
                pair("searching", "automatic_recovery")
            HeartRateRuntimeObservationCause.INITIAL_CONNECTING ->
                pair("connecting", "initial_acquisition")
            HeartRateRuntimeObservationCause.RECOVERY_CONNECTING ->
                pair("connecting", "automatic_recovery")
            HeartRateRuntimeObservationCause.INITIAL_WAITING_FIRST_SAMPLE ->
                pair("waiting_first_sample", "initial_acquisition")
            HeartRateRuntimeObservationCause.RECOVERY_WAITING_FIRST_SAMPLE ->
                pair("waiting_first_sample", "automatic_recovery")
            HeartRateRuntimeObservationCause.LIVE -> pair("live")
            HeartRateRuntimeObservationCause.FIRST_SAMPLE_TIMEOUT ->
                pair("stale", "first_sample_timeout")
            HeartRateRuntimeObservationCause.SAMPLE_STALE_TIMEOUT ->
                pair("stale", "sample_stale_timeout")
            HeartRateRuntimeObservationCause.RECOVERY_RECONNECTING ->
                pair("reconnecting", "automatic_recovery")
            HeartRateRuntimeObservationCause.UNEXPECTED_DISCONNECT_RECONNECTING ->
                pair("reconnecting", "unexpected_disconnect")
            HeartRateRuntimeObservationCause.SOURCE_UNAVAILABLE ->
                pair("disconnected", "source_unavailable")
            HeartRateRuntimeObservationCause.UNEXPECTED_DISCONNECT ->
                pair("disconnected", "unexpected_disconnect")
            HeartRateRuntimeObservationCause.MEASUREMENT_STREAM_UNAVAILABLE ->
                pair("technical_failure", "measurement_stream_unavailable")
            HeartRateRuntimeObservationCause.PLATFORM_FAILURE ->
                pair("technical_failure", "platform_failure")
        }

    fun acquisition(
        cause: HeartRateRuntimeObservationCause,
        recordingExpected: Boolean,
        userExclusionReason: String?
    ): CanonicalHeartRateAcquisitionFact {
        if (recordingExpected) {
            require(userExclusionReason == null) {
                "Expected recording cannot carry a user exclusion reason"
            }
        } else {
            require(userExclusionReason in USER_EXCLUSION_REASONS) {
                "User-excluded acquisition requires a canonical exclusion reason"
            }
        }
        val device = mapCause(cause)
        return CanonicalHeartRateAcquisitionFact(
            recordingIntent = if (recordingExpected) "expected_recording" else "user_excluded",
            intentReason = userExclusionReason,
            deviceState = device.deviceState,
            deviceReason = device.deviceReason
        )
    }

    private fun pair(
        state: String,
        reason: String? = null
    ) = CanonicalHeartRateDevicePair(state, reason)

    private val USER_EXCLUSION_REASONS = setOf(
        "user_turned_off",
        "user_opted_out",
        "user_disconnected_suppress_recovery"
    )
}
