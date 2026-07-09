package com.liujyks.trainflow.ui.shell.official

import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateUnavailableReason
import com.liujyks.trainflow.feature.settings.HeartRateBlePermissionStatus
import com.liujyks.trainflow.feature.settings.heartRateSettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateFloatingCapsuleStateTest {
    @Test
    fun disabledPreferenceHidesCapsule() {
        val state = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(enabled = false)
        )

        assertFalse(state.visible)
    }

    @Test
    fun enabledWithoutSourceShowsNoSourceWhenPermissionGranted() {
        val state = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(
                enabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
            )
        )

        assertTrue(state.visible)
        assertEquals(HeartRateFloatingCapsuleStatus.NO_SOURCE, state.status)
        assertEquals("未连接源", state.collapsedLabel)
    }

    @Test
    fun permissionMissingShowsPermissionDeniedState() {
        val state = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(
                enabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.DENIED
            )
        )

        assertEquals(HeartRateFloatingCapsuleStatus.PERMISSION_DENIED, state.status)
        assertEquals("权限未赋予", state.collapsedLabel)
    }

    @Test
    fun bluetoothDisabledShowsBluetoothState() {
        val state = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(
                enabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
                scannerState = com.liujyks.trainflow.core.health.BleHeartRateProviderState(
                    kind = com.liujyks.trainflow.core.health.BleHeartRateProviderStateKind.BLUETOOTH_DISABLED,
                    message = "Bluetooth disabled"
                )
            )
        )

        assertEquals(HeartRateFloatingCapsuleStatus.BLUETOOTH_DISABLED, state.status)
        assertEquals("蓝牙关闭", state.collapsedLabel)
    }

    @Test
    fun selectedSourceShowsSelectedDeviceWithoutFakeBpm() {
        val state = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(
                enabled = true,
                savedDeviceIdentifier = "D8:F0:42:01:90:D7",
                savedDeviceDisplayName = "HUAWEI Band HR-OD7",
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
            )
        )

        assertEquals(HeartRateFloatingCapsuleStatus.SELECTED_DEVICE, state.status)
        assertEquals("已选择设备", state.collapsedLabel)
        assertEquals("HUAWEI Band HR-OD7", state.deviceHint)
    }

    @Test
    fun waitingForDataMapsToWaitingState() {
        val state = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(
                enabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
            ),
            liveState = HeartRateState(
                kind = HeartRateStateKind.DEVICE_CONNECTED_NO_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                sourceLabel = "HUAWEI Band HR-OD7"
            )
        )

        assertEquals(HeartRateFloatingCapsuleStatus.WAITING_DATA, state.status)
        assertEquals("等待数据", state.collapsedLabel)
    }

    @Test
    fun staleAndOfflineDoNotShowOldBpmAsLive() {
        val stale = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(
                enabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
            ),
            liveState = HeartRateState(
                kind = HeartRateStateKind.STALE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = 105,
                unavailableReason = HeartRateUnavailableReason.READ_ERROR
            )
        )
        val offline = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(
                enabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
            ),
            liveState = HeartRateState(
                kind = HeartRateStateKind.STALE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = 105,
                unavailableReason = HeartRateUnavailableReason.DEVICE_DISCONNECTED
            )
        )

        assertEquals("数据过期", stale.collapsedLabel)
        assertEquals("离线", offline.collapsedLabel)
    }

    @Test
    fun bpmWithoutAgeShowsBpmOnly() {
        val state = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(
                enabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
            ),
            liveState = HeartRateState(
                kind = HeartRateStateKind.DEVICE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = 105
            ),
            userAgeYears = null
        )

        assertEquals(HeartRateFloatingCapsuleStatus.BPM_ONLY, state.status)
        assertEquals("心率 105 bpm", state.collapsedLabel)
    }

    @Test
    fun bpmWithAgeShowsZoneAndBpm() {
        val state = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(
                enabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
            ),
            liveState = HeartRateState(
                kind = HeartRateStateKind.DEVICE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = 122
            ),
            userAgeYears = 40
        )

        assertEquals(HeartRateFloatingCapsuleStatus.ZONE_FAT_BURN, state.status)
        assertEquals("燃脂 122 bpm", state.collapsedLabel)
    }

    @Test
    fun overLimitUsesDeepRedVisualOnlyState() {
        val state = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(
                enabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
            ),
            liveState = HeartRateState(
                kind = HeartRateStateKind.DEVICE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = 188
            ),
            userAgeYears = 40,
            overLimitThresholdBpm = 180
        )

        assertEquals(HeartRateFloatingCapsuleStatus.OVER_LIMIT, state.status)
        assertEquals("超过上限 188 bpm", state.collapsedLabel)
        assertTrue(state.detailBody.contains("不触发声音、震动、强制暂停"))
    }
}
