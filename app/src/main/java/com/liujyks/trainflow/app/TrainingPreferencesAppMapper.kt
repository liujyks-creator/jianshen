package com.liujyks.trainflow.app

import com.liujyks.trainflow.core.datastore.TrainFlowPreferences
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.feature.plans.PlanEditorDefaults
import com.liujyks.trainflow.feature.settings.TrainingPreferencesScreenState
import com.liujyks.trainflow.feature.settings.strengthSetTimerModePreferenceFromContract

internal fun TrainFlowPreferences.toTrainingPreferencesScreenState(): TrainingPreferencesScreenState {
    return TrainingPreferencesScreenState(
        defaultCountdownThresholdSec = defaultCountdownThresholdSec,
        actionCueEnabled = actionCueEnabled,
        restCueEnabled = restCueEnabled,
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        emphasisAnimationEnabled = emphasisAnimationEnabled,
        strengthSetTimerMode = strengthSetTimerModePreferenceFromContract(strengthSetTimerMode)
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
