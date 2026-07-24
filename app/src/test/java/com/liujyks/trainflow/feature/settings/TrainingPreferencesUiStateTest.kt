package com.liujyks.trainflow.feature.settings

import com.liujyks.trainflow.core.health.BleHeartRateDeviceCandidate
import com.liujyks.trainflow.core.health.BleHeartRateRecoverableReason
import com.liujyks.trainflow.core.health.BleHeartRateScanState
import com.liujyks.trainflow.core.health.BleHeartRateScanStateKind
import com.liujyks.trainflow.core.health.HeartRateRuntimeFact
import com.liujyks.trainflow.core.health.HeartRateSourceHint
import com.liujyks.trainflow.core.health.toHeartRateState
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
        assertEquals("未开启", state.heartRateSettings.statusLabel)
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

        assertEquals("已开启", state.statusLabel)
        assertEquals("已启用显示偏好；后续可选择设备。", state.statusSummary)
        assertEquals("未连接源 / 待选择设备。", state.sourceSummary)
        assertEquals("开启后仍不会自动扫描或连接；只有你主动点击连接已保存设备或扫描心率设备才会查找附近设备。", state.enabledBoundaryCopy)
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
            "已保存设备：HUAWEI Band HR-OD7。保存设备不代表它在附近、正在广播、正在连接或已连接。",
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
    fun deniedHeartRatePermissionKeepsRetryActionVisible() {
        val denied = heartRateSettingsUiState(
            enabled = true,
            savedDeviceDisplayName = null,
            blePermissionStatus = HeartRateBlePermissionStatus.DENIED
        )
        val rationale = denied.prepareBlePermissionRationale()

        assertTrue(denied.canPrepareBlePermission)
        assertEquals("重新授权蓝牙权限", denied.blePermissionActionLabel)
        assertEquals(HeartRateBlePermissionStatus.RATIONALE_VISIBLE, rationale.blePermissionStatus)
        assertTrue(rationale.canRequestBlePermission)
    }

    @Test
    fun permanentlyDeniedHeartRatePermissionKeepsSettingsActionVisible() {
        val permanentlyDenied = heartRateSettingsUiState(
            enabled = true,
            savedDeviceDisplayName = null,
            blePermissionStatus = HeartRateBlePermissionStatus.PERMANENTLY_DENIED
        )

        assertTrue(permanentlyDenied.canPrepareBlePermission)
        assertEquals("去系统设置开启", permanentlyDenied.blePermissionActionLabel)
        assertEquals(
            HeartRateBlePermissionStatus.PERMANENTLY_DENIED,
            permanentlyDenied.prepareBlePermissionRationale().blePermissionStatus
        )
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

        assertTrue(granted.blePermissionStatusCopy.contains("蓝牙权限已允许"))
        assertTrue(granted.blePermissionStatusCopy.contains("主动扫描"))
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
        assertEquals(
            HeartRateBlePermissionStatus.DENIED,
            resolveHeartRateBlePermissionStatus(
                displayEnabled = true,
                allPermissionsGranted = false,
                requestResult = HeartRateBlePermissionStatus.DENIED
            )
        )
    }

    @Test
    fun heartRateDevicePickerDoesNotScanWhenDisabledOrPermissionMissing() {
        val disabled = heartRateDevicePickerUiState(
            displayEnabled = false,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
        )
        val permissionRequired = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.DENIED
        )

        assertEquals(HeartRateDevicePickerStatus.DISABLED, disabled.status)
        assertFalse(disabled.canStartScan)
        assertEquals(HeartRateDevicePickerStatus.PERMISSION_REQUIRED, permissionRequired.status)
        assertFalse(permissionRequired.canStartScan)
        assertTrue(permissionRequired.body.contains("未授权时不会扫描"))
    }

    @Test
    fun heartRateDevicePickerBlocksScanWhenBluetoothDisabled() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            runtimeState = HeartRateRuntimeFact.BluetoothOff().toHeartRateState()
        )

        assertEquals(HeartRateDevicePickerStatus.BLUETOOTH_DISABLED, state.status)
        assertFalse(state.canStartScan)
        assertTrue(state.body.contains("蓝牙已关闭"))
    }

    @Test
    fun heartRateDevicePickerStartScanMovesToScanningState() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            scanState = BleHeartRateScanState(
                kind = BleHeartRateScanStateKind.SCANNING,
                message = "Scanning"
            )
        )

        assertEquals(HeartRateDevicePickerStatus.SCANNING, state.status)
        assertTrue(state.scanActive)
        assertFalse(state.canStartScan)
        assertTrue(state.canStopScan)
        assertTrue(state.body.contains("扫描窗口约 12 秒"))
    }

    @Test
    fun heartRateDevicePickerKeepsScanningDuringActiveNonHrsResults() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            scanState = BleHeartRateScanState(
                kind = BleHeartRateScanStateKind.SCANNING,
                message = "Scanning"
            ),
            scannerCandidates = listOf(
                BleHeartRateDeviceCandidate(
                    identifier = "AA:BB:CC:DD:EE:FF",
                    displayName = "Keyboard",
                    rssi = -30,
                    advertisesHeartRateService = false
                )
            )
        )

        assertEquals(HeartRateDevicePickerStatus.SCANNING, state.status)
        assertTrue(state.scanActive)
        assertTrue(state.canStopScan)
        assertFalse(state.canStartScan)
        assertTrue(state.devices.isEmpty())
        assertTrue(state.hrsCandidates.isEmpty())
    }

    @Test
    fun heartRateDevicePickerLabelsRescanAsScanningOtherDevicesWhenSourceIsSelected() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            runtimeState = HeartRateRuntimeFact.Live(
                bpm = 105,
                measuredAt = "2026-07-19T13:16:04Z",
                source = HeartRateSourceHint(
                    identifier = "D8:F0:42:01:90:D7",
                    displayName = "HUAWEI Band HR-OD7"
                )
            ).toHeartRateState(),
            scanState = BleHeartRateScanState(
                kind = BleHeartRateScanStateKind.SCANNING,
                message = "Scanning"
            )
        )

        assertEquals(HeartRateDevicePickerStatus.SCANNING, state.status)
        assertEquals("设备来源：正在扫描其他设备", state.title)
        assertTrue(state.body.contains("当前连接和心率胶囊不会因扫描中断"))
        assertTrue(state.body.contains("只有选择新设备后"))
    }

    @Test
    fun heartRateDevicePickerShowsOnlyHeartRateCapableCandidates() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            scanState = BleHeartRateScanState(
                kind = BleHeartRateScanStateKind.STOPPED,
                message = "Scan window ended"
            ),
            scannerCandidates = listOf(
                BleHeartRateDeviceCandidate(
                    identifier = "AA:BB:CC:DD:EE:FF",
                    displayName = "Keyboard",
                    rssi = -30,
                    advertisesHeartRateService = false
                ),
                BleHeartRateDeviceCandidate(
                    identifier = "D8:F0:42:01:90:D7",
                    displayName = "HUAWEI Band HR-OD7",
                    rssi = -46,
                    advertisesHeartRateService = true
                )
            )
        )

        assertEquals(HeartRateDevicePickerStatus.DEVICES_FOUND, state.status)
        assertTrue(state.canStartScan)
        assertEquals(1, state.devices.size)
        assertEquals("HUAWEI Band HR-OD7", state.devices.single().displayName)
        assertEquals("D8:F0:42:**:**:D7", state.devices.single().safeIdentifier)
        assertTrue(state.devices.single().capabilitySummary.contains("0x180D"))
        assertTrue(state.devices.single().signalSummary.contains("-46"))
    }

    @Test
    fun heartRateDevicePickerRetainsHeartRateCandidatesAfterTimeout() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            scanState = BleHeartRateScanState(
                kind = BleHeartRateScanStateKind.STOPPED,
                message = "Scan window ended"
            ),
            scannerCandidates = listOf(
                BleHeartRateDeviceCandidate(
                    identifier = "D8:F0:42:01:90:D7",
                    displayName = "HUAWEI Band HR-OD7",
                    rssi = -46,
                    advertisesHeartRateService = true
                )
            ),
            scanFinishedWithoutDevices = true
        )

        assertEquals(HeartRateDevicePickerStatus.DEVICES_FOUND, state.status)
        assertFalse(state.scanActive)
        assertTrue(state.canStartScan)
        assertFalse(state.canStopScan)
        assertEquals(1, state.hrsCandidates.size)
        assertEquals("HUAWEI Band HR-OD7", state.devices.single().displayName)
        assertTrue(state.body.contains("仍可选择"))
    }

    @Test
    fun heartRateDevicePickerRetainsHeartRateCandidatesAfterManualStop() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            scanState = BleHeartRateScanState(
                kind = BleHeartRateScanStateKind.STOPPED,
                message = "BLE HRS provider stopped"
            ),
            scannerCandidates = listOf(
                BleHeartRateDeviceCandidate(
                    identifier = "D8:F0:42:01:90:D7",
                    displayName = "HUAWEI Band HR-OD7",
                    rssi = -46,
                    advertisesHeartRateService = true
                )
            ),
            scanFinishedWithoutDevices = false
        )

        assertEquals(HeartRateDevicePickerStatus.DEVICES_FOUND, state.status)
        assertFalse(state.scanActive)
        assertTrue(state.canStartScan)
        assertEquals(1, state.hrsCandidates.size)
        assertTrue(state.body.contains("扫描已停止"))
    }

    @Test
    fun heartRateDevicePickerShowsNoDevicesAfterTimeoutWithoutCandidates() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            scanState = BleHeartRateScanState(
                kind = BleHeartRateScanStateKind.STOPPED,
                message = "Scan window ended"
            ),
            scanFinishedWithoutDevices = true
        )

        assertEquals(HeartRateDevicePickerStatus.NO_DEVICES_FOUND, state.status)
        assertTrue(state.canStartScan)
        assertFalse(state.canStopScan)
        assertTrue(state.hrsCandidates.isEmpty())
        assertTrue(state.body.contains("没有找到心率设备"))
        assertTrue(state.body.contains("心率广播模式"))
    }

    @Test
    fun heartRateDevicePickerShowsSelectedDevicePreference() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            runtimeState = HeartRateRuntimeFact.NotConnected(
                source = HeartRateSourceHint(
                    identifier = "D8:F0:42:01:90:D7",
                    displayName = "HUAWEI Band HR-OD7"
                )
            ).toHeartRateState()
        )

        assertEquals(HeartRateDevicePickerStatus.SELECTED, state.status)
        assertTrue(state.canStartScan)
        assertTrue(state.body.contains("仅保存"))
        assertEquals("连接已保存设备", state.actionLabel)
        assertEquals("未连接", state.connectionStatusLabel)
    }

    @Test
    fun savedDeviceReconnectScansOnlyAfterExplicitActionAndMatchesIdentifierExactly() {
        val savedIdentifier = "D8:F0:42:01:90:D7"
        val scanning = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            scanState = BleHeartRateScanState(BleHeartRateScanStateKind.SCANNING, "Scanning"),
            savedDeviceIdentifier = savedIdentifier,
            savedDeviceDisplayName = "HUAWEI Band HR-OD7",
            scanPurpose = HeartRateDeviceScanPurpose.CONNECT_SAVED_DEVICE
        )
        val sameNameDifferentIdentifier = BleHeartRateDeviceCandidate(
            identifier = "AA:BB:CC:DD:EE:FF",
            displayName = "HUAWEI Band HR-OD7",
            advertisesHeartRateService = true
        )
        val exactMatch = sameNameDifferentIdentifier.copy(identifier = savedIdentifier)

        assertEquals("设备来源：正在查找已保存设备", scanning.title)
        assertTrue(scanning.body.contains("identifier 完全匹配"))
        assertEquals(null, savedDeviceReconnectCandidateIdentifier(savedIdentifier, listOf(sameNameDifferentIdentifier)))
        assertEquals(savedIdentifier, savedDeviceReconnectCandidateIdentifier(savedIdentifier, listOf(sameNameDifferentIdentifier, exactMatch)))
    }

    @Test
    fun savedDeviceReconnectTimeoutKeepsOtherHrsCandidatesForManualChoice() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            scanState = BleHeartRateScanState(BleHeartRateScanStateKind.STOPPED, "Scan window ended"),
            savedDeviceIdentifier = "D8:F0:42:01:90:D7",
            savedDeviceDisplayName = "HUAWEI Band HR-OD7",
            savedDeviceReconnectNotFound = true,
            scannerCandidates = listOf(
                BleHeartRateDeviceCandidate(
                    identifier = "AA:BB:CC:DD:EE:FF",
                    displayName = "Other HRS",
                    advertisesHeartRateService = true
                )
            )
        )

        assertEquals(HeartRateDevicePickerStatus.SAVED_DEVICE_NOT_FOUND, state.status)
        assertEquals("设备来源：未找到已保存设备", state.title)
        assertEquals("连接已保存设备", state.actionLabel)
        assertEquals(1, state.hrsCandidates.size)
    }

    @Test
    fun liveBpmUsesScanOtherDevicesActionWithoutChangingConnectionStatus() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            runtimeState = HeartRateRuntimeFact.Live(
                bpm = 105,
                measuredAt = "2026-07-19T13:16:04Z"
            ).toHeartRateState(),
            savedDeviceIdentifier = "D8:F0:42:01:90:D7",
            savedDeviceDisplayName = "HUAWEI Band HR-OD7"
        )

        assertEquals("扫描其他设备", state.actionLabel)
        assertEquals("已连接", state.connectionStatusLabel)
    }

    @Test
    fun heartRateDevicePickerShowsScanFailedState() {
        val state = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            scanState = BleHeartRateScanState(
                kind = BleHeartRateScanStateKind.ERROR,
                message = "failed",
                recoverableReason = BleHeartRateRecoverableReason.SCAN_FAILED
            )
        )

        assertEquals(HeartRateDevicePickerStatus.SCAN_FAILED, state.status)
        assertTrue(state.canStartScan)
        assertTrue(state.body.contains("扫描未完成"))
    }

    @Test
    fun clearSelectionReturnsPickerToIdleNoSource() {
        val selected = heartRateSettingsUiState(
            enabled = true,
            savedDeviceIdentifier = "D8:F0:42:01:90:D7",
            savedDeviceDisplayName = "HUAWEI Band HR-OD7",
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
        )
        val cleared = heartRateSettingsUiState(
            enabled = true,
            savedDeviceIdentifier = null,
            savedDeviceDisplayName = null,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
        )

        assertEquals(HeartRateDevicePickerStatus.SELECTED, selected.devicePickerState.status)
        assertEquals(HeartRateDevicePickerStatus.IDLE_NO_SOURCE, cleared.devicePickerState.status)
        assertTrue(cleared.devicePickerState.canStartScan)
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
