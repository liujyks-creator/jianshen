package com.liujyks.trainflow.core.model

data class HeartRateState(
    val availability: HeartRateAvailability,
    val bpm: Int? = null,
    val measuredAt: String? = null,
    val sourceId: String? = null,
    val warningLevel: HeartRateWarningLevel? = null,
    val message: String? = null
)

enum class HeartRateAvailability(val contractValue: String) {
    DISABLED("disabled"),
    NOT_CONNECTED("not_connected"),
    CONNECTING("connecting"),
    AVAILABLE("available"),
    STALE("stale"),
    ERROR("error")
}

enum class HeartRateWarningLevel(val contractValue: String) {
    NONE("none"),
    ATTENTION("attention"),
    HIGH("high")
}
