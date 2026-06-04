package com.liujyks.trainflow.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object TrainFlowPreferenceKeys {
    const val DATASTORE_NAME = "trainflow_preferences"

    val actionCueEnabled = booleanPreferencesKey("action_cue_enabled")
    val restCueEnabled = booleanPreferencesKey("rest_cue_enabled")
    val soundEnabled = booleanPreferencesKey("sound_enabled")
    val vibrationEnabled = booleanPreferencesKey("vibration_enabled")
    val emphasisAnimationEnabled = booleanPreferencesKey("emphasis_animation_enabled")
    val defaultCountdownThresholdSec = intPreferencesKey("default_countdown_threshold_sec")
    val strengthSetTimerMode = stringPreferencesKey("strength_set_timer_mode")
    val heartRateDisplayEnabled = booleanPreferencesKey("heart_rate_display_enabled")
    val showDisconnectedHeartRatePlaceholder =
        booleanPreferencesKey("show_disconnected_heart_rate_placeholder")
    val uiSkinId = stringPreferencesKey("ui_skin_id")
}
