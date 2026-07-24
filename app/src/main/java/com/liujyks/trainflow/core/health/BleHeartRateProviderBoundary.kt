package com.liujyks.trainflow.core.health

/** Pure scan candidate fact retained for the E17-6 owner and E17-7 settings picker. */
internal data class BleHeartRateDeviceCandidate(
    val identifier: String,
    val displayName: String,
    val rssi: Int? = null,
    val advertisesHeartRateService: Boolean = false
)

/** Pure scan-window fact. It owns no scanner, callback, target, attempt, or GATT resource. */
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

/** The deterministic owner only exposes a typed recoverable scan failure at this boundary. */
internal enum class BleHeartRateRecoverableReason {
    SCAN_FAILED
}
