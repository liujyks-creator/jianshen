package com.liujyks.trainflow.core.datastore

data class TrainFlowPreferences(
    val actionCueEnabled: Boolean = true,
    val restCueEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val emphasisAnimationEnabled: Boolean = true,
    val defaultCountdownThresholdSec: Int = DEFAULT_COUNTDOWN_THRESHOLD_SEC,
    val strengthSetTimerMode: String = STRENGTH_TIMER_MANUAL_START,
    val heartRateDisplayEnabled: Boolean = false,
    val showDisconnectedHeartRatePlaceholder: Boolean = false
) {
    companion object {
        const val DEFAULT_COUNTDOWN_THRESHOLD_SEC = 5
        const val STRENGTH_TIMER_MANUAL_START = "manual_start"
    }
}
