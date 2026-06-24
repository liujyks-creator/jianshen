package com.liujyks.trainflow.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals(7, state.permissionPrivacySections.size)
        assertTrue(state.permissionPrivacySections.any { section -> section.title == "通知权限" })
        assertTrue(state.permissionPrivacySections.any { section -> section.title == "健康数据" })
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
    fun permissionPrivacyCopyStatesBoundariesWithoutUnavailableClaims() {
        val copy = defaultTrainingPreferencesScreenState()
            .permissionPrivacySections
            .joinToString(" ") { section -> "${section.title} ${section.body}" }

        assertTrue(copy.contains("关闭后训练仍可正常使用"))
        assertTrue(copy.contains("普通通知可能被系统延迟"))
        assertTrue(copy.contains("不是 foreground service"))
        assertTrue(copy.contains("不显示心率"))
        assertTrue(copy.contains("未接入真实设备"))
        assertTrue(copy.contains("未实现语音控制"))
        assertFalse(copy.contains("后台可靠计时已完成"))
        assertFalse(copy.contains("Health Connect 已接入"))
        assertFalse(copy.contains("语音教练已启用"))
        assertFalse(copy.contains("医疗诊断结果"))
    }

    @Test
    fun skinOptionsSelectKnownSkinAndFallbackUnknownSkinToOfficialFlow() {
        val bigTypeOptions = uiSkinPreferenceOptionsFromRegistry("big_type")
        val fallbackOptions = uiSkinPreferenceOptionsFromRegistry("third_party_skin")

        assertEquals("Big Type", bigTypeOptions.single { it.selected }.displayName)
        assertEquals("Official Flow", fallbackOptions.single { it.selected }.displayName)
        assertTrue(bigTypeOptions.all { option -> option.capabilityBoundary.isNotBlank() })
        val bigType = bigTypeOptions.single { it.id == "big_type" }
        assertTrue(bigType.description.contains("远距离可读"))
        assertTrue(bigType.capabilityBoundary.contains("信息密集页沿用现有组合"))
    }
}
