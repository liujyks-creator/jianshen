package com.liujyks.trainflow.app

import com.liujyks.trainflow.core.datastore.TrainFlowPreferences
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.feature.settings.StrengthSetTimerModePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TrainingPreferencesAppMapperTest {
    @Test
    fun mapsDataStorePreferencesToSettingsUiState() {
        val preferences = TrainFlowPreferences(
            actionCueEnabled = false,
            restCueEnabled = true,
            soundEnabled = false,
            vibrationEnabled = false,
            emphasisAnimationEnabled = false,
            defaultCountdownThresholdSec = 11,
            strengthSetTimerMode = "auto_after_rest",
            uiSkinId = "tile_flow"
        )
        val state = preferences.toTrainingPreferencesScreenState()

        assertEquals(11, state.defaultCountdownThresholdSec)
        assertFalse(state.actionCueEnabled)
        assertFalse(state.soundEnabled)
        assertEquals(StrengthSetTimerModePreference.AUTO_AFTER_REST, state.strengthSetTimerMode)
        assertEquals("tile_flow", state.selectedUiSkinId)
        assertEquals("Tile Flow", state.selectedSkinSummary)
    }

    @Test
    fun mapsDataStorePreferencesToPlanEditorDefaults() {
        val preferences = TrainFlowPreferences(
            defaultCountdownThresholdSec = 6,
            strengthSetTimerMode = "auto_after_rest"
        )
        val defaults = preferences.toPlanEditorDefaults()

        assertEquals(6, defaults.safeCountdownThresholdSec)
        assertEquals(StrengthSetTimerMode.AUTO_AFTER_REST, defaults.strengthSetTimerMode)
    }

    @Test
    fun mapsDataStoreSkinIdToThemeSkinWithOfficialFallback() {
        assertEquals(
            "big_type",
            TrainFlowPreferences(uiSkinId = "big_type").toTrainFlowSkin().id
        )
        assertEquals(
            "official_flow",
            TrainFlowPreferences(uiSkinId = "remote_skin").toTrainFlowSkin().id
        )
    }
}
