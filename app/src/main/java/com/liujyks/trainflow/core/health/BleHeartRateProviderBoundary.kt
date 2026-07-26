package com.liujyks.trainflow.core.health

/**
 * Resource-free compatibility facts shared by the E17 owner and settings presentation.
 *
 * These types cannot own a scanner, GATT, callback, connection attempt, retry policy, or platform
 * object. The former E16 provider/selection/public-state surface was retired by E17-7b.
 */
internal data class BleHeartRateDeviceCandidate(
    val identifier: String,
    val displayName: String,
    val rssi: Int? = null,
    val advertisesHeartRateService: Boolean = false
)

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

internal enum class BleHeartRateRecoverableReason {
    SCAN_FAILED
}
