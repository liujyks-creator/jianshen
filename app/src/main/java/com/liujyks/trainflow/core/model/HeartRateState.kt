package com.liujyks.trainflow.core.model

data class HeartRateState(
    val kind: HeartRateStateKind,
    val sourceKind: HeartRateSourceKind,
    val bpm: Int? = null,
    val measuredAt: String? = null,
    val recordedAt: String? = null,
    val sourceId: String? = null,
    val sourceLabel: String? = null,
    val unavailableReason: HeartRateUnavailableReason? = null,
    val message: String? = null
)

enum class HeartRateStateKind(val contractValue: String) {
    UNAVAILABLE("unavailable"),
    DEVICE_CONNECTED_NO_READING("device_connected_no_reading"),
    DEVICE_READING("device_reading"),
    MANUAL_READING("manual_reading"),
    STALE_READING("stale_reading"),
    PERMISSION_UNAVAILABLE("permission_unavailable"),
    PROVIDER_UNAVAILABLE("provider_unavailable")
}

enum class HeartRateSourceKind(val contractValue: String) {
    NONE("none"),
    DEVICE("device"),
    MANUAL("manual")
}

enum class HeartRateUnavailableReason(val contractValue: String) {
    NO_SOURCE("no_source"),
    DISABLED_BY_USER("disabled_by_user"),
    NOT_CONFIGURED("not_configured"),
    PERMISSION_REQUIRED("permission_required"),
    BLUETOOTH_DISABLED("bluetooth_disabled"),
    SCAN_STOPPED("scan_stopped"),
    DEVICE_DISCONNECTED("device_disconnected"),
    CONNECTION_FAILED("connection_failed"),
    READ_ERROR("read_error")
}
