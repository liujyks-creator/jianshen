package com.liujyks.trainflow.core.health

import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateTechnicalFailure

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
    val recoverableReason: BleHeartRateRecoverableReason? = null
) {
    /**
     * Compatibility adapter for the E16 provider. The legacy provider remains the production
     * owner during this Story, while E17 presentation consumes only the mapped public fact.
     */
    fun toHeartRateState(): HeartRateState {
        val source = sourceHint()
        val fact = when (kind) {
            BleHeartRateProviderStateKind.UNAVAILABLE -> HeartRateRuntimeFact.TechnicalFailure(
                reason = HeartRateTechnicalFailure.PLATFORM_UNAVAILABLE,
                source = source
            )

            BleHeartRateProviderStateKind.NO_SOURCE -> HeartRateRuntimeFact.NotConnected(source)
            BleHeartRateProviderStateKind.STOPPED -> HeartRateRuntimeFact.IntentionalStop(source)
            BleHeartRateProviderStateKind.PERMISSION_REQUIRED ->
                HeartRateRuntimeFact.PermissionRequired(source)
            BleHeartRateProviderStateKind.BLUETOOTH_DISABLED ->
                HeartRateRuntimeFact.BluetoothOff(source)
            BleHeartRateProviderStateKind.DEVICE_SELECTED ->
                HeartRateRuntimeFact.NotConnected(source)
            BleHeartRateProviderStateKind.CONNECTING -> HeartRateRuntimeFact.Connecting(source)
            BleHeartRateProviderStateKind.CONNECTED_WAITING_FOR_DATA ->
                HeartRateRuntimeFact.WaitingFirstData(source)
            BleHeartRateProviderStateKind.LIVE_BPM -> HeartRateRuntimeFact.Live(
                bpm = bpm ?: -1,
                measuredAt = measuredAt.orEmpty(),
                source = source
            )

            BleHeartRateProviderStateKind.STALE -> HeartRateRuntimeFact.DataInterrupted(source)
            BleHeartRateProviderStateKind.DISCONNECTED ->
                HeartRateRuntimeFact.LinkDisconnected(source)
            BleHeartRateProviderStateKind.ERROR -> legacyErrorFact(source)
        }
        return fact.toHeartRateState()
    }

    private fun sourceHint(): HeartRateSourceHint? {
        val identifier = selectedDevice?.identifier ?: candidate?.identifier
        val displayName = selectedDevice?.displayName ?: candidate?.displayName
        return if (identifier == null && displayName == null) {
            null
        } else {
            HeartRateSourceHint(identifier = identifier, displayName = displayName)
        }
    }

    private fun legacyErrorFact(source: HeartRateSourceHint?): HeartRateRuntimeFact {
        if (recoverableReason == BleHeartRateRecoverableReason.PARSE_FAILED) {
            // The legacy runtime has no monotonic freshness timeline. A cached wall-clock
            // reading therefore cannot prove that the previous sample is still fresh.
            return HeartRateRuntimeFact.DataInterrupted(source)
        }
        return HeartRateRuntimeFact.TechnicalFailure(
            reason = recoverableReason.toTechnicalFailure(),
            source = source
        )
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

private fun BleHeartRateRecoverableReason?.toTechnicalFailure(): HeartRateTechnicalFailure {
    return when (this) {
        BleHeartRateRecoverableReason.CONNECTION_FAILED -> HeartRateTechnicalFailure.CONNECT_FAILED
        BleHeartRateRecoverableReason.SERVICE_MISSING,
        BleHeartRateRecoverableReason.CHARACTERISTIC_MISSING ->
            HeartRateTechnicalFailure.SERVICE_DISCOVERY_FAILED
        BleHeartRateRecoverableReason.NOTIFY_UNAVAILABLE,
        BleHeartRateRecoverableReason.DESCRIPTOR_MISSING,
        BleHeartRateRecoverableReason.DESCRIPTOR_WRITE_FAILED -> HeartRateTechnicalFailure.CCCD_FAILED
        BleHeartRateRecoverableReason.SCAN_FAILED,
        BleHeartRateRecoverableReason.DEVICE_NOT_FOUND,
        BleHeartRateRecoverableReason.PARSE_FAILED,
        null -> HeartRateTechnicalFailure.PLATFORM_FAILURE
    }
}
