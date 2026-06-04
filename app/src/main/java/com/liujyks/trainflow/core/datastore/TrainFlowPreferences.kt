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
        const val STRENGTH_TIMER_AUTO_AFTER_REST = "auto_after_rest"
        const val MIN_COUNTDOWN_THRESHOLD_SEC = 1
        const val MAX_COUNTDOWN_THRESHOLD_SEC = 60

        fun sanitizeCountdownThresholdSec(value: Int): Int {
            return value.coerceIn(MIN_COUNTDOWN_THRESHOLD_SEC, MAX_COUNTDOWN_THRESHOLD_SEC)
        }

        fun sanitizeStrengthSetTimerMode(value: String): String {
            return when (value) {
                STRENGTH_TIMER_MANUAL_START,
                STRENGTH_TIMER_AUTO_AFTER_REST -> value
                else -> STRENGTH_TIMER_MANUAL_START
            }
        }
    }
}
