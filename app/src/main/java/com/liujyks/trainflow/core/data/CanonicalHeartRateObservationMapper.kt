package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.health.HeartRateObservationCause

internal data class CanonicalHeartRateDeviceState(val deviceState: String, val deviceReason: String?)

/** Acquisition v1 projection; pair legality remains owned by AcquisitionV1Validator. */
internal object CanonicalHeartRateObservationMapper {
    fun map(cause: HeartRateObservationCause): CanonicalHeartRateDeviceState = when (cause) {
        HeartRateObservationCause.NOT_OBSERVING -> CanonicalHeartRateDeviceState("not_observing", null)
        HeartRateObservationCause.NO_SOURCE_SELECTED -> CanonicalHeartRateDeviceState("no_source_selected", "source_not_selected")
        HeartRateObservationCause.PERMISSION_MISSING -> CanonicalHeartRateDeviceState("permission_required", "permission_missing")
        HeartRateObservationCause.PERMISSION_REVOKED -> CanonicalHeartRateDeviceState("permission_required", "permission_revoked")
        HeartRateObservationCause.BLUETOOTH_OFF -> CanonicalHeartRateDeviceState("bluetooth_unavailable", "bluetooth_off")
        HeartRateObservationCause.PLATFORM_UNAVAILABLE -> CanonicalHeartRateDeviceState("bluetooth_unavailable", "platform_unavailable")
        HeartRateObservationCause.INITIAL_SEARCH -> CanonicalHeartRateDeviceState("searching", "initial_acquisition")
        HeartRateObservationCause.RECOVERY_SEARCH -> CanonicalHeartRateDeviceState("searching", "automatic_recovery")
        HeartRateObservationCause.INITIAL_CONNECT -> CanonicalHeartRateDeviceState("connecting", "initial_acquisition")
        HeartRateObservationCause.RECOVERY_CONNECT -> CanonicalHeartRateDeviceState("connecting", "automatic_recovery")
        HeartRateObservationCause.INITIAL_WAIT -> CanonicalHeartRateDeviceState("waiting_first_sample", "initial_acquisition")
        HeartRateObservationCause.RECOVERY_WAIT -> CanonicalHeartRateDeviceState("waiting_first_sample", "automatic_recovery")
        HeartRateObservationCause.LIVE -> CanonicalHeartRateDeviceState("live", null)
        HeartRateObservationCause.FIRST_SAMPLE_TIMEOUT -> CanonicalHeartRateDeviceState("stale", "first_sample_timeout")
        HeartRateObservationCause.SAMPLE_STALE_TIMEOUT -> CanonicalHeartRateDeviceState("stale", "sample_stale_timeout")
        HeartRateObservationCause.RECOVERY_WAITING -> CanonicalHeartRateDeviceState("reconnecting", "automatic_recovery")
        HeartRateObservationCause.DISCONNECTED -> CanonicalHeartRateDeviceState("disconnected", null)
        HeartRateObservationCause.SOURCE_UNAVAILABLE -> CanonicalHeartRateDeviceState("disconnected", "source_unavailable")
        HeartRateObservationCause.UNEXPECTED_DISCONNECT -> CanonicalHeartRateDeviceState("disconnected", "unexpected_disconnect")
        HeartRateObservationCause.MEASUREMENT_STREAM_UNAVAILABLE -> CanonicalHeartRateDeviceState("technical_failure", "measurement_stream_unavailable")
        HeartRateObservationCause.PLATFORM_FAILURE -> CanonicalHeartRateDeviceState("technical_failure", "platform_failure")
    }
}
