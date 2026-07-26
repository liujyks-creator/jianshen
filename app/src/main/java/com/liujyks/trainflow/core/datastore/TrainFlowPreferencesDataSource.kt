package com.liujyks.trainflow.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrainFlowPreferencesDataSource(
    private val dataStore: DataStore<Preferences>
) {
    val preferences: Flow<TrainFlowPreferences> = dataStore.data.map { storedPreferences ->
        TrainFlowPreferences(
            actionCueEnabled = storedPreferences[TrainFlowPreferenceKeys.actionCueEnabled] ?: true,
            restCueEnabled = storedPreferences[TrainFlowPreferenceKeys.restCueEnabled] ?: true,
            soundEnabled = storedPreferences[TrainFlowPreferenceKeys.soundEnabled] ?: true,
            vibrationEnabled = storedPreferences[TrainFlowPreferenceKeys.vibrationEnabled] ?: true,
            emphasisAnimationEnabled =
                storedPreferences[TrainFlowPreferenceKeys.emphasisAnimationEnabled] ?: true,
            defaultCountdownThresholdSec =
                TrainFlowPreferences.sanitizeCountdownThresholdSec(
                    storedPreferences[TrainFlowPreferenceKeys.defaultCountdownThresholdSec]
                        ?: TrainFlowPreferences.DEFAULT_COUNTDOWN_THRESHOLD_SEC
                ),
            strengthSetTimerMode = TrainFlowPreferences.sanitizeStrengthSetTimerMode(
                storedPreferences[TrainFlowPreferenceKeys.strengthSetTimerMode]
                    ?: TrainFlowPreferences.STRENGTH_TIMER_MANUAL_START
            ),
            heartRateDisplayEnabled =
                storedPreferences[TrainFlowPreferenceKeys.heartRateDisplayEnabled] ?: false,
            showDisconnectedHeartRatePlaceholder =
                storedPreferences[TrainFlowPreferenceKeys.showDisconnectedHeartRatePlaceholder]
                    ?: false,
            bleHeartRateDeviceIdentifier =
                storedPreferences[TrainFlowPreferenceKeys.bleHeartRateDeviceIdentifier],
            bleHeartRateDeviceDisplayName =
                storedPreferences[TrainFlowPreferenceKeys.bleHeartRateDeviceDisplayName],
            heartRateManualDisconnectSuppressed =
                storedPreferences[TrainFlowPreferenceKeys.heartRateManualDisconnectSuppressed]
                    ?: false,
            ageYears = TrainFlowPreferences.validAgeYearsOrNull(
                storedPreferences[TrainFlowPreferenceKeys.ageYears]
            ),
            personalMaxHeartRateBpm = TrainFlowPreferences.validHeartRateParameterBpmOrNull(
                storedPreferences[TrainFlowPreferenceKeys.personalMaxHeartRateBpm]
            ),
            alertThresholdBpm = TrainFlowPreferences.validHeartRateParameterBpmOrNull(
                storedPreferences[TrainFlowPreferenceKeys.alertThresholdBpm]
            ),
            uiSkinId = TrainFlowPreferences.sanitizeUiSkinId(
                storedPreferences[TrainFlowPreferenceKeys.uiSkinId]
                    ?: TrainFlowPreferences.DEFAULT_UI_SKIN_ID
            )
        )
    }

    suspend fun setActionCueEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.actionCueEnabled] = enabled
        }
    }

    suspend fun setTrainingFeedbackPreferences(preferences: TrainFlowPreferences) {
        dataStore.edit { storedPreferences ->
            storedPreferences[TrainFlowPreferenceKeys.actionCueEnabled] = preferences.actionCueEnabled
            storedPreferences[TrainFlowPreferenceKeys.restCueEnabled] = preferences.restCueEnabled
            storedPreferences[TrainFlowPreferenceKeys.soundEnabled] = preferences.soundEnabled
            storedPreferences[TrainFlowPreferenceKeys.vibrationEnabled] = preferences.vibrationEnabled
            storedPreferences[TrainFlowPreferenceKeys.emphasisAnimationEnabled] =
                preferences.emphasisAnimationEnabled
            storedPreferences[TrainFlowPreferenceKeys.defaultCountdownThresholdSec] =
                TrainFlowPreferences.sanitizeCountdownThresholdSec(
                    preferences.defaultCountdownThresholdSec
                )
            storedPreferences[TrainFlowPreferenceKeys.strengthSetTimerMode] =
                TrainFlowPreferences.sanitizeStrengthSetTimerMode(preferences.strengthSetTimerMode)
            storedPreferences[TrainFlowPreferenceKeys.uiSkinId] =
                TrainFlowPreferences.sanitizeUiSkinId(preferences.uiSkinId)
        }
    }

    suspend fun setRestCueEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.restCueEnabled] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.soundEnabled] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.vibrationEnabled] = enabled
        }
    }

    suspend fun setEmphasisAnimationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.emphasisAnimationEnabled] = enabled
        }
    }

    suspend fun setDefaultCountdownThresholdSec(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.defaultCountdownThresholdSec] =
                TrainFlowPreferences.sanitizeCountdownThresholdSec(seconds)
        }
    }

    suspend fun setStrengthSetTimerMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.strengthSetTimerMode] =
                TrainFlowPreferences.sanitizeStrengthSetTimerMode(mode)
        }
    }

    suspend fun setUiSkinId(skinId: String) {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.uiSkinId] = TrainFlowPreferences.sanitizeUiSkinId(skinId)
        }
    }

    suspend fun setHeartRateDisplayEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.heartRateDisplayEnabled] = enabled
            preferences[TrainFlowPreferenceKeys.showDisconnectedHeartRatePlaceholder] = false
        }
    }

    suspend fun setBleHeartRateDevicePreference(identifier: String, displayName: String) {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.bleHeartRateDeviceIdentifier] = identifier
            preferences[TrainFlowPreferenceKeys.bleHeartRateDeviceDisplayName] = displayName
            preferences[TrainFlowPreferenceKeys.heartRateManualDisconnectSuppressed] = false
        }
    }

    suspend fun clearBleHeartRateDevicePreference() {
        dataStore.edit { preferences ->
            preferences.remove(TrainFlowPreferenceKeys.bleHeartRateDeviceIdentifier)
            preferences.remove(TrainFlowPreferenceKeys.bleHeartRateDeviceDisplayName)
        }
    }

    suspend fun setHeartRateManualDisconnectSuppressed() {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.heartRateManualDisconnectSuppressed] = true
        }
    }

    suspend fun clearHeartRateManualDisconnectSuppression() {
        dataStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.heartRateManualDisconnectSuppressed] = false
        }
    }

    suspend fun setHeartRatePersonalization(
        ageYears: Int?,
        personalMaxHeartRateBpm: Int?,
        alertThresholdBpm: Int?
    ) {
        dataStore.edit { preferences ->
            preferences.putOrRemove(
                TrainFlowPreferenceKeys.ageYears,
                TrainFlowPreferences.validAgeYearsOrNull(ageYears)
            )
            preferences.putOrRemove(
                TrainFlowPreferenceKeys.personalMaxHeartRateBpm,
                TrainFlowPreferences.validHeartRateParameterBpmOrNull(personalMaxHeartRateBpm)
            )
            preferences.putOrRemove(
                TrainFlowPreferenceKeys.alertThresholdBpm,
                TrainFlowPreferences.validHeartRateParameterBpmOrNull(alertThresholdBpm)
            )
        }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.putOrRemove(
        key: Preferences.Key<Int>,
        value: Int?
    ) {
        if (value == null) {
            remove(key)
        } else {
            this[key] = value
        }
    }
}
