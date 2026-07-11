package com.liujyks.trainflow.core.health

import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateUnavailableReason

internal data class BleHeartRateDeviceCandidate(
    val identifier: String,
    val displayName: String,
    val rssi: Int? = null,
    val advertisesHeartRateService: Boolean = false
)

internal data class BleHeartRateDeviceSelection(
    val identifier: String,
    val displayName: String
)

internal data class BleHeartRateProviderState(
    val kind: BleHeartRateProviderStateKind,
    val message: String,
    val selectedDevice: BleHeartRateDeviceSelection? = null,
    val candidate: BleHeartRateDeviceCandidate? = null,
    val bpm: Int? = null,
    val measuredAt: String? = null,
    val missingPermissions: List<String> = emptyList(),
    val recoverableReason: BleHeartRateRecoverableReason? = null,
    val freshnessReason: HeartRateFreshnessReason? = null,
    val currentReconnectAttempt: Int = 0,
    val retryBudgetExhausted: Boolean = false,
    val reconnectInProgress: Boolean = false
) {
    fun toHeartRateState(): HeartRateState {
        return when (kind) {
            BleHeartRateProviderStateKind.UNAVAILABLE,
            BleHeartRateProviderStateKind.NO_SOURCE,
            BleHeartRateProviderStateKind.STOPPED -> HeartRateState(
                kind = HeartRateStateKind.UNAVAILABLE,
                sourceKind = HeartRateSourceKind.NONE,
                unavailableReason = HeartRateUnavailableReason.NO_SOURCE,
                message = message
            )

            BleHeartRateProviderStateKind.PERMISSION_REQUIRED -> HeartRateState(
                kind = HeartRateStateKind.PERMISSION_UNAVAILABLE,
                sourceKind = HeartRateSourceKind.DEVICE,
                unavailableReason = HeartRateUnavailableReason.PERMISSION_REQUIRED,
                message = message
            )

            BleHeartRateProviderStateKind.BLUETOOTH_DISABLED -> HeartRateState(
                kind = HeartRateStateKind.PROVIDER_UNAVAILABLE,
                sourceKind = HeartRateSourceKind.DEVICE,
                unavailableReason = HeartRateUnavailableReason.BLUETOOTH_DISABLED,
                message = message
            )

            BleHeartRateProviderStateKind.DEVICE_SELECTED,
            BleHeartRateProviderStateKind.CONNECTING -> HeartRateState(
                kind = HeartRateStateKind.PROVIDER_UNAVAILABLE,
                sourceKind = HeartRateSourceKind.DEVICE,
                sourceId = selectedDevice?.identifier ?: candidate?.identifier,
                sourceLabel = selectedDevice?.displayName ?: candidate?.displayName,
                unavailableReason = HeartRateUnavailableReason.NOT_CONFIGURED,
                message = message
            )

            BleHeartRateProviderStateKind.CONNECTED_WAITING_FOR_DATA -> HeartRateState(
                kind = HeartRateStateKind.DEVICE_CONNECTED_NO_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                sourceId = selectedDevice?.identifier,
                sourceLabel = selectedDevice?.displayName,
                message = message
            )

            BleHeartRateProviderStateKind.LIVE_BPM -> HeartRateState(
                kind = HeartRateStateKind.DEVICE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = bpm,
                measuredAt = measuredAt,
                sourceId = selectedDevice?.identifier,
                sourceLabel = selectedDevice?.displayName,
                message = message
            )

            BleHeartRateProviderStateKind.STALE,
            BleHeartRateProviderStateKind.DISCONNECTED -> HeartRateState(
                kind = HeartRateStateKind.STALE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                sourceId = selectedDevice?.identifier,
                sourceLabel = selectedDevice?.displayName,
                unavailableReason = HeartRateUnavailableReason.DEVICE_DISCONNECTED,
                message = message
            )

            BleHeartRateProviderStateKind.ERROR -> HeartRateState(
                kind = HeartRateStateKind.PROVIDER_UNAVAILABLE,
                sourceKind = HeartRateSourceKind.DEVICE,
                sourceId = selectedDevice?.identifier ?: candidate?.identifier,
                sourceLabel = selectedDevice?.displayName ?: candidate?.displayName,
                unavailableReason = recoverableReason.toUnavailableReason(),
                message = message
            )
        }
    }

    companion object {
        fun noSource(message: String = "No heart-rate source selected") = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.NO_SOURCE,
            message = message
        )
    }
}

internal enum class BleHeartRateProviderStateKind {
    UNAVAILABLE,
    NO_SOURCE,
    PERMISSION_REQUIRED,
    BLUETOOTH_DISABLED,
    DEVICE_SELECTED,
    CONNECTING,
    CONNECTED_WAITING_FOR_DATA,
    LIVE_BPM,
    STALE,
    DISCONNECTED,
    STOPPED,
    ERROR
}

internal data class BleHeartRateScanState(
    val kind: BleHeartRateScanStateKind,
    val message: String,
    val recoverableReason: BleHeartRateRecoverableReason? = null
) {
    companion object {
        fun idle(message: String = "BLE heart-rate scanner idle") = BleHeartRateScanState(
            kind = BleHeartRateScanStateKind.IDLE,
            message = message
        )
    }
}

internal enum class BleHeartRateScanStateKind {
    IDLE,
    SCANNING,
    STOPPED,
    ERROR
}

internal fun providerStateAfterAvailabilityRefresh(
    currentState: BleHeartRateProviderState,
    availabilityState: BleHeartRateProviderState
): BleHeartRateProviderState {
    val passiveStates = setOf(
        BleHeartRateProviderStateKind.UNAVAILABLE,
        BleHeartRateProviderStateKind.NO_SOURCE,
        BleHeartRateProviderStateKind.PERMISSION_REQUIRED,
        BleHeartRateProviderStateKind.BLUETOOTH_DISABLED,
        BleHeartRateProviderStateKind.STOPPED
    )
    return if (
        availabilityState.kind != BleHeartRateProviderStateKind.NO_SOURCE ||
        currentState.kind in passiveStates
    ) {
        availabilityState
    } else {
        currentState
    }
}

internal enum class BleHeartRateRecoverableReason {
    SCAN_FAILED,
    DEVICE_NOT_FOUND,
    CONNECTION_FAILED,
    SERVICE_MISSING,
    CHARACTERISTIC_MISSING,
    NOTIFY_UNAVAILABLE,
    DESCRIPTOR_MISSING,
    DESCRIPTOR_WRITE_FAILED,
    PARSE_FAILED
}

private fun BleHeartRateRecoverableReason?.toUnavailableReason(): HeartRateUnavailableReason {
    return when (this) {
        BleHeartRateRecoverableReason.CONNECTION_FAILED -> HeartRateUnavailableReason.CONNECTION_FAILED
        BleHeartRateRecoverableReason.PARSE_FAILED -> HeartRateUnavailableReason.READ_ERROR
        else -> HeartRateUnavailableReason.NOT_CONFIGURED
    }
}
