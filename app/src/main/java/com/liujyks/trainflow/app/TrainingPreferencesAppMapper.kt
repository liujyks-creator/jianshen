package com.liujyks.trainflow.app

import com.liujyks.trainflow.core.datastore.TrainFlowPreferences
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.feature.plans.PlanEditorDefaults
import com.liujyks.trainflow.feature.settings.TrainingPreferencesScreenState
import com.liujyks.trainflow.feature.settings.heartRateSettingsUiState
import com.liujyks.trainflow.feature.settings.strengthSetTimerModePreferenceFromContract
import com.liujyks.trainflow.feature.settings.uiSkinPreferenceOptionsFromRegistry
import com.liujyks.trainflow.ui.theme.SkinRegistry
import com.liujyks.trainflow.ui.theme.TrainFlowSkin

internal fun TrainFlowPreferences.toTrainingPreferencesScreenState(): TrainingPreferencesScreenState {
    val skin = toTrainFlowSkin()
    return TrainingPreferencesScreenState(
        defaultCountdownThresholdSec = defaultCountdownThresholdSec,
        actionCueEnabled = actionCueEnabled,
        restCueEnabled = restCueEnabled,
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        emphasisAnimationEnabled = emphasisAnimationEnabled,
        strengthSetTimerMode = strengthSetTimerModePreferenceFromContract(strengthSetTimerMode),
        heartRateSettings = heartRateSettingsUiState(
            enabled = heartRateDisplayEnabled,
            savedDeviceIdentifier = bleHeartRateDeviceIdentifier,
            savedDeviceDisplayName = bleHeartRateDeviceDisplayName,
            manualSuppressed = heartRateManualSuppressed,
            ageYears = heartRateAgeYears,
            personalMaxHeartRateBpm = heartRatePersonalMaxBpm,
            alertThresholdBpm = heartRateAlertThresholdBpm
        ),
        selectedUiSkinId = skin.id,
        uiSkinOptions = uiSkinPreferenceOptionsFromRegistry(skin.id)
    )
}

internal fun TrainFlowPreferences.toPlanEditorDefaults(): PlanEditorDefaults {
    return PlanEditorDefaults(
        actionCueEnabled = actionCueEnabled,
        restCueEnabled = restCueEnabled,
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        emphasisAnimationEnabled = emphasisAnimationEnabled,
        defaultCountdownThresholdSec = defaultCountdownThresholdSec,
        strengthSetTimerMode = strengthSetTimerMode.toStrengthSetTimerMode()
    )
}

private fun String.toStrengthSetTimerMode(): StrengthSetTimerMode {
    return StrengthSetTimerMode.entries.firstOrNull { mode ->
        mode.contractValue == this
    } ?: StrengthSetTimerMode.MANUAL_START
}

internal fun TrainFlowPreferences.toTrainFlowSkin(): TrainFlowSkin {
    return SkinRegistry.resolve(uiSkinId)
}
