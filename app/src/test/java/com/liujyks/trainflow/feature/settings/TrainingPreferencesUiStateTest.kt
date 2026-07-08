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
        assertFalse(state.heartRateSettings.enabled)
        assertEquals("心率与设备", state.heartRateSettings.sectionTitle)
        assertEquals("未启用", state.heartRateSettings.statusLabel)
        assertEquals("关闭后不显示胶囊、不扫描、不连接、不记录。", state.heartRateSettings.sourceSummary)
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
    fun heartRateSettingsCopyMatchesOptInAndNonMedicalBoundaries() {
        val disabled = defaultTrainingPreferencesScreenState().heartRateSettings
        val disabledCopy = listOf(
            disabled.statusSummary,
            disabled.sourceSummary,
            disabled.purposeCopy,
            disabled.recordingBoundaryCopy,
            disabled.privacyCopy,
            disabled.nonMedicalCopy,
            disabled.permissionCopy,
            disabled.overlayCopy,
            disabled.enabledBoundaryCopy
        ).joinToString(" ")

        assertTrue(disabledCopy.contains("默认关闭"))
        assertTrue(disabledCopy.contains("不显示胶囊、不扫描、不连接、不记录"))
        assertTrue(disabledCopy.contains("无训练时只显示不记录"))
        assertTrue(disabledCopy.contains("训练记录采样另拆后续任务"))
        assertTrue(disabledCopy.contains("主动点击授权入口并看过说明"))
        assertTrue(disabledCopy.contains("不使用系统 overlay"))
        assertTrue(disabledCopy.contains("不诊断疾病"))
        assertTrue(disabledCopy.contains("不自动中断训练"))
        assertFalse(disabledCopy.contains("开始扫描"))
        assertFalse(disabledCopy.contains("正在连接"))
    }

    @Test
    fun enabledHeartRateSettingsOnlyIndicateDisplayPreferenceAndFutureDeviceChoice() {
        val state = heartRateSettingsUiState(
            enabled = true,
            savedDeviceDisplayName = null
        )

        assertEquals("已启用", state.statusLabel)
        assertEquals("已启用显示偏好；后续可选择设备。", state.statusSummary)
        assertEquals("未连接源 / 待选择设备。", state.sourceSummary)
        assertEquals("开启后仍不会自动扫描或连接；选择设备会在后续任务实现。", state.enabledBoundaryCopy)
        assertEquals(HeartRateBlePermissionStatus.NOT_REQUESTED, state.blePermissionStatus)
        assertTrue(state.canPrepareBlePermission)
        assertFalse(state.canClearSavedDevice)
    }

    @Test
    fun savedHeartRateDeviceCanBeShownAsConvenienceHintOnly() {
        val state = heartRateSettingsUiState(
            enabled = true,
            savedDeviceDisplayName = "HUAWEI Band HR-OD7"
        )

        assertTrue(state.canClearSavedDevice)
        assertEquals(
            "已保存设备名称：HUAWEI Band HR-OD7。本轮不会自动扫描或连接。",
            state.sourceSummary
        )
    }

    @Test
    fun heartRatePermissionFlowShowsRationaleBeforeRequest() {
        val enabled = heartRateSettingsUiState(
            enabled = true,
            savedDeviceDisplayName = null
        )

        assertTrue(enabled.canPrepareBlePermission)
        assertFalse(enabled.showBlePermissionRationale)
        assertFalse(enabled.canRequestBlePermission)

        val rationale = enabled.prepareBlePermissionRationale()

        assertEquals(HeartRateBlePermissionStatus.RATIONALE_VISIBLE, rationale.blePermissionStatus)
        assertTrue(rationale.showBlePermissionRationale)
        assertTrue(rationale.canRequestBlePermission)
        assertTrue(rationale.blePermissionRationaleBullets.any { text -> text.contains("查找并连接") })
        assertTrue(rationale.blePermissionRationaleBullets.any { text -> text.contains("不使用系统悬浮窗") })
        assertTrue(rationale.blePermissionRationaleBullets.any { text -> text.contains("无训练时只显示不记录") })
        assertTrue(rationale.blePermissionRationaleBullets.any { text -> text.contains("不替代医生建议") })
    }

    @Test
    fun disabledHeartRatePermissionFlowCannotRequest() {
        val disabled = heartRateSettingsUiState(
            enabled = false,
            savedDeviceDisplayName = null
        )

        assertFalse(disabled.canPrepareBlePermission)
        assertFalse(disabled.prepareBlePermissionRationale().showBlePermissionRationale)
        assertEquals(HeartRateBlePermissionStatus.NOT_REQUESTED, disabled.blePermissionStatus)
    }

    @Test
    fun heartRatePermissionResultCopyCoversGrantedDeniedAndSettingsBoundary() {
        val granted = heartRateSettingsUiState(
            enabled = true,
            savedDeviceDisplayName = null,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
        )
        val denied = heartRateSettingsUiState(
            enabled = true,
            savedDeviceDisplayName = null,
            blePermissionStatus = HeartRateBlePermissionStatus.DENIED
        )
        val permanentlyDenied = heartRateSettingsUiState(
            enabled = true,
            savedDeviceDisplayName = null,
            blePermissionStatus = HeartRateBlePermissionStatus.PERMANENTLY_DENIED
        )

        assertTrue(granted.blePermissionStatusCopy.contains("蓝牙权限已允许 / 可选择设备"))
        assertTrue(granted.blePermissionStatusCopy.contains("不扫描、不连接"))
        assertTrue(denied.blePermissionStatusCopy.contains("权限未赋予"))
        assertTrue(denied.blePermissionStatusCopy.contains("稍后再次点击"))
        assertTrue(permanentlyDenied.blePermissionStatusCopy.contains("系统设置"))
        assertFalse(permanentlyDenied.blePermissionStatusCopy.contains("开始扫描"))
    }

    @Test
    fun heartRatePermissionStatusResolverDoesNotTreatSwitchOnAsRequest() {
        assertEquals(
            HeartRateBlePermissionStatus.NOT_REQUESTED,
            resolveHeartRateBlePermissionStatus(
                displayEnabled = true,
                allPermissionsGranted = false,
                requestResult = HeartRateBlePermissionStatus.NOT_REQUESTED
            )
        )
        assertEquals(
            HeartRateBlePermissionStatus.GRANTED,
            resolveHeartRateBlePermissionStatus(
                displayEnabled = true,
                allPermissionsGranted = true,
                requestResult = HeartRateBlePermissionStatus.NOT_REQUESTED
            )
        )
        assertEquals(
            HeartRateBlePermissionStatus.NOT_REQUESTED,
            resolveHeartRateBlePermissionStatus(
                displayEnabled = false,
                allPermissionsGranted = true,
                requestResult = HeartRateBlePermissionStatus.GRANTED
            )
        )
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
