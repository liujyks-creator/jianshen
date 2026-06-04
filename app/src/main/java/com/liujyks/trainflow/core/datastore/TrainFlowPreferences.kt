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
    val showDisconnectedHeartRatePlaceholder: Boolean = false,
    val uiSkinId: String = DEFAULT_UI_SKIN_ID
) {
    companion object {
        const val DEFAULT_COUNTDOWN_THRESHOLD_SEC = 5
        const val STRENGTH_TIMER_MANUAL_START = "manual_start"
        const val STRENGTH_TIMER_AUTO_AFTER_REST = "auto_after_rest"
        const val UI_SKIN_OFFICIAL_FLOW = "official_flow"
        const val UI_SKIN_TILE_FLOW = "tile_flow"
        const val UI_SKIN_BIG_TYPE = "big_type"
        const val DEFAULT_UI_SKIN_ID = UI_SKIN_OFFICIAL_FLOW
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

        fun sanitizeUiSkinId(value: String): String {
            return when (value) {
                UI_SKIN_OFFICIAL_FLOW,
                UI_SKIN_TILE_FLOW,
                UI_SKIN_BIG_TYPE -> value
                else -> DEFAULT_UI_SKIN_ID
            }
        }
    }
}
