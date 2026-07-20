package com.liujyks.trainflow.core.model

data class HeartRateState(
    val kind: HeartRateStateKind,
    val sourceKind: HeartRateSourceKind,
    /**
     * E17 public fact consumed by new presentation code.
     *
     * A null value means the producer still emits the pre-E17 compatibility shape. Legacy
     * [kind], [recordedAt], [unavailableReason], and [message] values must not be interpreted as
     * an E17 live-device fact.
     */
    val fact: HeartRateFact? = null,
    val bpm: Int? = null,
    val measuredAt: String? = null,
    val recordedAt: String? = null,
    val sourceId: String? = null,
    val sourceLabel: String? = null,
    val unavailableReason: HeartRateUnavailableReason? = null,
    val message: String? = null,
    val technicalFailure: HeartRateTechnicalFailure? = null
) {
    fun e17ContractViolations(): Set<HeartRateStateContractViolation> = buildSet {
        val currentFact = fact
        if (currentFact == null) {
            add(HeartRateStateContractViolation.MISSING_E17_FACT)
            return@buildSet
        }

        val isLive = currentFact == HeartRateFact.LIVE
        if (isLive) {
            if (sourceKind != HeartRateSourceKind.DEVICE) {
                add(HeartRateStateContractViolation.LIVE_REQUIRES_DEVICE_SOURCE)
            }
            if (bpm == null || bpm <= 0) {
                add(HeartRateStateContractViolation.LIVE_REQUIRES_POSITIVE_BPM)
            }
            if (measuredAt.isNullOrBlank()) {
                add(HeartRateStateContractViolation.LIVE_REQUIRES_MEASURED_AT)
            }
        } else if (bpm != null || measuredAt != null) {
            add(HeartRateStateContractViolation.NON_LIVE_CARRIES_CURRENT_READING)
        }

        if (currentFact == HeartRateFact.TECHNICAL_FAILURE) {
            if (technicalFailure == null) {
                add(HeartRateStateContractViolation.TECHNICAL_FAILURE_REQUIRES_REASON)
            }
        } else if (technicalFailure != null) {
            add(HeartRateStateContractViolation.NON_FAILURE_CARRIES_FAILURE_REASON)
        }
    }

    fun isValidE17State(): Boolean = e17ContractViolations().isEmpty()
}

enum class HeartRateFact(val contractValue: String) {
    DISABLED("disabled"),
    PERMISSION_REQUIRED("permission_required"),
    BLUETOOTH_OFF("bluetooth_off"),
    NOT_CONNECTED("not_connected"),
    SCANNING("scanning"),
    CONNECTING("connecting"),
    WAITING_FIRST_DATA("waiting_first_data"),
    LIVE("live"),
    DATA_INTERRUPTED("data_interrupted"),
    LINK_DISCONNECTED("link_disconnected"),
    TECHNICAL_FAILURE("technical_failure"),
    INTENTIONAL_STOP("intentional_stop")
}

enum class HeartRateTechnicalFailure(val contractValue: String) {
    PLATFORM_UNAVAILABLE("platform_unavailable"),
    PLATFORM_FAILURE("platform_failure"),
    CONNECT_FAILED("connect_failed"),
    SERVICE_DISCOVERY_FAILED("service_discovery_failed"),
    CCCD_FAILED("cccd_failed"),
    INVALID_MONOTONIC_TIME("invalid_monotonic_time"),
    INVALID_FACT("invalid_fact")
}

enum class HeartRateStateContractViolation {
    MISSING_E17_FACT,
    LIVE_REQUIRES_DEVICE_SOURCE,
    LIVE_REQUIRES_POSITIVE_BPM,
    LIVE_REQUIRES_MEASURED_AT,
    NON_LIVE_CARRIES_CURRENT_READING,
    TECHNICAL_FAILURE_REQUIRES_REASON,
    NON_FAILURE_CARRIES_FAILURE_REASON
}

/** Pre-E17 compatibility values retained until E17-7 removes the old provider surface. */
enum class HeartRateStateKind(val contractValue: String) {
    UNAVAILABLE("unavailable"),
    DEVICE_CONNECTED_NO_READING("device_connected_no_reading"),
    DEVICE_READING("device_reading"),
    MANUAL_READING("manual_reading"),
    STALE_READING("stale_reading"),
    PERMISSION_UNAVAILABLE("permission_unavailable"),
    PROVIDER_UNAVAILABLE("provider_unavailable")
}

/** Pre-E17 compatibility source values. E17 live facts still require [DEVICE]. */
enum class HeartRateSourceKind(val contractValue: String) {
    NONE("none"),
    DEVICE("device"),
    MANUAL("manual")
}

/** Pre-E17 compatibility reasons; new presentation consumes [HeartRateFact] instead. */
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
