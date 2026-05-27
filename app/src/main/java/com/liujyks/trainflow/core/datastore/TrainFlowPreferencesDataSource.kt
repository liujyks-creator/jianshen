package com.liujyks.trainflow.core.datastore

import androidx.datastore.core.DataStore
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
                storedPreferences[TrainFlowPreferenceKeys.defaultCountdownThresholdSec]
                    ?: TrainFlowPreferences.DEFAULT_COUNTDOWN_THRESHOLD_SEC,
            strengthSetTimerMode = storedPreferences[TrainFlowPreferenceKeys.strengthSetTimerMode]
                ?: TrainFlowPreferences.STRENGTH_TIMER_MANUAL_START,
            heartRateDisplayEnabled =
                storedPreferences[TrainFlowPreferenceKeys.heartRateDisplayEnabled] ?: false,
            showDisconnectedHeartRatePlaceholder =
                storedPreferences[TrainFlowPreferenceKeys.showDisconnectedHeartRatePlaceholder]
                    ?: false
        )
    }
}
