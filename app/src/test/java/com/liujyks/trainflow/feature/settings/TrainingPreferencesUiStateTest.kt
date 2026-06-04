package com.liujyks.trainflow.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingPreferencesUiStateTest {
    @Test
    fun defaultsMatchTrainingPreferenceStoryBoundary() {
        val state = defaultTrainingPreferencesScreenState()

        assertEquals(5, state.defaultCountdownThresholdSec)
        assertEquals(StrengthSetTimerModePreference.MANUAL_START, state.strengthSetTimerMode)
        assertTrue(state.actionCueEnabled)
        assertTrue(state.restCueEnabled)
        assertTrue(state.soundEnabled)
        assertTrue(state.vibrationEnabled)
        assertTrue(state.emphasisAnimationEnabled)
        assertEquals("默认最后 5 秒提醒", state.countdownSummary)
        assertEquals("official_flow", state.selectedUiSkinId)
        assertEquals("Official Flow", state.selectedSkinSummary)
        assertEquals(3, state.uiSkinOptions.size)
    }

    @Test
    fun strengthSetTimerModeContractFallsBackToManualStart() {
        assertEquals(
            StrengthSetTimerModePreference.AUTO_AFTER_REST,
            strengthSetTimerModePreferenceFromContract("auto_after_rest")
        )
        assertEquals(
            StrengthSetTimerModePreference.MANUAL_START,
            strengthSetTimerModePreferenceFromContract("voice_coach")
        )
    }

    @Test
    fun feedbackSummarySeparatesCountdownFeedbackFromNotificationCapabilities() {
        val state = TrainingPreferencesScreenState(
            actionCueEnabled = false,
            restCueEnabled = false,
            soundEnabled = false,
            vibrationEnabled = false,
            emphasisAnimationEnabled = false
        )

        assertEquals("仅保留训练流程", state.feedbackSummary)
    }

    @Test
    fun skinOptionsSelectKnownSkinAndFallbackUnknownSkinToOfficialFlow() {
        val bigTypeOptions = uiSkinPreferenceOptionsFromRegistry("big_type")
        val fallbackOptions = uiSkinPreferenceOptionsFromRegistry("third_party_skin")

        assertEquals("Big Type", bigTypeOptions.single { it.selected }.displayName)
        assertEquals("Official Flow", fallbackOptions.single { it.selected }.displayName)
        assertTrue(bigTypeOptions.all { option -> option.capabilityBoundary.isNotBlank() })
    }
}
