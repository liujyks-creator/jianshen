package com.liujyks.trainflow.feature.settings

import com.liujyks.trainflow.core.health.BleHeartRateDeviceCandidate
import com.liujyks.trainflow.core.health.BleHeartRateScanState
import com.liujyks.trainflow.core.health.BleHeartRateScanStateKind
import com.liujyks.trainflow.core.health.HeartRateRecoveryPhase
import com.liujyks.trainflow.core.health.HeartRateRecoveryState
import com.liujyks.trainflow.core.health.HeartRateRuntimeFact
import com.liujyks.trainflow.core.health.toHeartRateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingPreferencesUiStateTest {
    @Test
    fun automaticRecoveryScanDoesNotMasqueradeAsManualScan() {
        val state = heartRateSettingsUiState(
            enabled = true,
            savedDeviceIdentifier = "AA:BB",
            savedDeviceDisplayName = "Band",
            appVisible = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            scanState = BleHeartRateScanState(
                kind = BleHeartRateScanStateKind.SCANNING,
                message = "recovery"
            ),
            recoveryState = HeartRateRecoveryState(
                phase = HeartRateRecoveryPhase.SEARCHING,
                targetIdentifier = "AA:BB"
            )
        )

        assertEquals(
            "自动恢复：正在查找已保存设备",
            state.devicePickerState.title
        )
        assertFalse(state.devicePickerState.canStopScan)
        assertFalse(state.devicePickerState.body.contains("主动扫描结果"))
    }

    @Test
    fun missedAutomaticWindowDoesNotCreateManualNoDevicesResult() {
        val state = heartRateSettingsUiState(
            enabled = true,
            savedDeviceIdentifier = "AA:BB",
            savedDeviceDisplayName = "Band",
            appVisible = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            scanState = BleHeartRateScanState(
                kind = BleHeartRateScanStateKind.STOPPED,
                message = "recovery ended"
            ),
            scanFinishedWithoutDevices = true,
            lastCompletedScanPurpose = HeartRateDeviceScanPurpose.NONE,
            recoveryState = HeartRateRecoveryState(
                phase = HeartRateRecoveryPhase.WINDOW_MISSED_ARMED,
                targetIdentifier = "AA:BB"
            )
        )

        assertEquals(HeartRateDevicePickerStatus.SELECTED, state.devicePickerState.status)
        assertEquals("本次未找到已保存设备", state.recoveryPresentation.title)
    }

    @Test
    fun defaultsKeepHeartRateDisabledAndTrainingPreferencesStable() {
        val state = defaultTrainingPreferencesScreenState()

        assertEquals(5, state.defaultCountdownThresholdSec)
        assertEquals(StrengthSetTimerModePreference.MANUAL_START, state.strengthSetTimerMode)
        assertFalse(state.heartRateSettings.enabled)
        assertEquals("关闭后不显示胶囊、不扫描、不连接、不记录。", state.heartRateSettings.sourceSummary)
        assertEquals("official_flow", state.selectedUiSkinId)
        assertEquals(3, state.uiSkinOptions.size)
    }

    @Test
    fun enablingDoesNotRequestPermissionButExplainsForegroundRecovery() {
        val state = heartRateSettingsUiState(enabled = true, appVisible = true)

        assertEquals(HeartRateBlePermissionStatus.NOT_REQUESTED, state.blePermissionStatus)
        assertTrue(state.canPrepareBlePermission)
        assertFalse(state.canRequestBlePermission)
        assertTrue(state.statusSummary.contains("前台"))
        assertTrue(state.enabledBoundaryCopy.contains("自动恢复"))
    }

    @Test
    fun permissionRationalePrecedesSystemRequest() {
        val rationale = heartRateSettingsUiState(enabled = true)
            .prepareBlePermissionRationale()

        assertEquals(HeartRateBlePermissionStatus.RATIONALE_VISIBLE, rationale.blePermissionStatus)
        assertTrue(rationale.canRequestBlePermission)
        assertTrue(rationale.blePermissionRationaleBullets.any { it.contains("查找并连接") })
        assertTrue(rationale.blePermissionRationaleBullets.any { it.contains("非医疗") })
    }

    @Test
    fun explicitActionsKeepSuppressionAndPersonalParametersDistinct() {
        val state = heartRateSettingsUiState(
            enabled = true,
            savedDeviceIdentifier = "D8:F0:42:01:90:D7",
            savedDeviceDisplayName = "HUAWEI Band HR-OD7",
            manualSuppressed = true,
            ageYears = 101,
            personalMaxHeartRateBpm = 188,
            alertThresholdBpm = 181,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            appVisible = true
        )

        assertTrue(state.canReconnect)
        assertFalse(state.canDisconnect)
        assertTrue(state.canClearSavedDevice)
        assertEquals(101, state.ageYears)
        assertEquals(188, state.personalMaxHeartRateBpm)
        assertEquals(181, state.alertThresholdBpm)
        assertTrue(state.connectionIntentCopy.contains("不会自动恢复"))
        assertTrue(state.disconnectActionCopy.contains("保留"))
        assertTrue(state.clearDeviceActionCopy.contains("不关闭心率功能"))
        assertTrue(state.optOutActionCopy.contains("隐藏胶囊"))
    }

    @Test
    fun clearedTargetTakesPresentationPriorityOverPersistedSuppression() {
        val state = heartRateSettingsUiState(
            enabled = true,
            savedDeviceIdentifier = null,
            savedDeviceDisplayName = null,
            manualSuppressed = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            appVisible = true
        )

        assertEquals("未保存设备 / 待选择设备。", state.sourceSummary)
        assertFalse(state.connectionIntentCopy.contains("保留设备"))
        assertFalse(state.canReconnect)
        assertFalse(state.canDisconnect)
    }

    @Test
    fun automaticRecoveryStatesDoNotPretendToBeManualScan() {
        val states = listOf(
            HeartRateRecoveryState(
                HeartRateRecoveryPhase.WAITING_NEXT_WINDOW,
                targetIdentifier = "D8:F0:42:01:90:D7"
            ),
            HeartRateRecoveryState(
                HeartRateRecoveryPhase.SEARCHING,
                targetIdentifier = "D8:F0:42:01:90:D7"
            ),
            HeartRateRecoveryState(
                HeartRateRecoveryPhase.WINDOW_MISSED_ARMED,
                targetIdentifier = "D8:F0:42:01:90:D7"
            )
        ).map { recovery ->
            heartRateRecoveryPresentation(recovery, BleHeartRateScanState.idle())
        }

        assertTrue(states[0].body.contains("稍后自动"))
        assertTrue(states[1].body.contains("自动查找"))
        assertTrue(states[2].body.contains("仍会继续"))
        assertTrue(states.none { it.body.contains("你已开始扫描") })
        assertTrue(states.none { it.body.contains("永久停止") })
    }

    @Test
    fun manualScanShowsOnlyHrsCandidatesAndExactSavedMatch() {
        val savedIdentifier = "D8:F0:42:01:90:D7"
        val other = BleHeartRateDeviceCandidate(
            identifier = "AA:BB:CC:DD:EE:FF",
            displayName = "HUAWEI Band HR-OD7",
            rssi = -30,
            advertisesHeartRateService = true
        )
        val exact = other.copy(identifier = savedIdentifier, rssi = -46)
        val keyboard = other.copy(
            identifier = "11:22:33:44:55:66",
            displayName = "Keyboard",
            advertisesHeartRateService = false
        )
        val picker = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            heartRateState = HeartRateRuntimeFact.NotConnected().toHeartRateState(),
            scanState = BleHeartRateScanState(
                BleHeartRateScanStateKind.SCANNING,
                "manual"
            ),
            scannerCandidates = listOf(keyboard, other, exact),
            savedDeviceIdentifier = savedIdentifier,
            savedDeviceDisplayName = "HUAWEI Band HR-OD7",
            scanPurpose = HeartRateDeviceScanPurpose.CONNECT_SAVED_DEVICE
        )

        assertEquals(HeartRateDevicePickerStatus.SCANNING, picker.status)
        assertEquals(2, picker.devices.size)
        assertTrue(picker.body.contains("identifier 完全匹配"))
        assertEquals(
            savedIdentifier,
            savedDeviceReconnectCandidateIdentifier(savedIdentifier, listOf(other, exact))
        )
    }

    @Test
    fun savedTargetCopyExplainsAutomaticRecoveryWithoutClaimingConnection() {
        val picker = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            savedDeviceIdentifier = "D8:F0:42:01:90:D7",
            savedDeviceDisplayName = "HUAWEI Band HR-OD7"
        )

        assertTrue(picker.body.contains("自动恢复"))
        assertTrue(picker.body.contains("不代表设备在附近"))
        assertFalse(picker.body.contains("仅保存为你主动连接"))
        assertEquals("未连接", picker.connectionStatusLabel)
    }

    @Test
    fun liveStateKeepsCurrentConnectionWhileOfferingOtherDeviceScan() {
        val picker = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            heartRateState = HeartRateRuntimeFact.Live(
                bpm = 105,
                measuredAt = "2026-07-26T00:00:00Z",
                source = com.liujyks.trainflow.core.health.HeartRateSourceHint(
                    identifier = "D8:F0:42:01:90:D7",
                    displayName = "HUAWEI Band HR-OD7"
                )
            ).toHeartRateState(),
            savedDeviceIdentifier = "D8:F0:42:01:90:D7",
            savedDeviceDisplayName = "HUAWEI Band HR-OD7"
        )

        assertEquals("已连接", picker.connectionStatusLabel)
        assertEquals("扫描其他设备", picker.actionLabel)
    }

    @Test
    fun permissionAndBluetoothFactsBlockManualScan() {
        val denied = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.DENIED
        )
        val bluetoothOff = heartRateDevicePickerUiState(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            heartRateState = HeartRateRuntimeFact.BluetoothOff().toHeartRateState()
        )

        assertFalse(denied.canStartScan)
        assertEquals(HeartRateDevicePickerStatus.PERMISSION_REQUIRED, denied.status)
        assertFalse(bluetoothOff.canStartScan)
        assertEquals(HeartRateDevicePickerStatus.BLUETOOTH_DISABLED, bluetoothOff.status)
    }

    @Test
    fun skinAndStrengthFallbackContractsRemainStable() {
        assertEquals(
            StrengthSetTimerModePreference.MANUAL_START,
            strengthSetTimerModePreferenceFromContract("unknown")
        )
        assertEquals(
            "Official Flow",
            uiSkinPreferenceOptionsFromRegistry("unknown").single { it.selected }.displayName
        )
    }
}
