package com.liujyks.trainflow.core.health

import android.Manifest
import android.os.Build
import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateUnavailableReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleHeartRateProviderBoundaryTest {
    @Test
    fun permissionPlannerUsesAndroid12BluetoothPermissions() {
        assertEquals(
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ),
            BleHeartRatePermissionPlanner.requiredPermissions(Build.VERSION_CODES.S)
        )
    }

    @Test
    fun permissionPlannerUsesLocationOnlyForPreAndroid12ScanCompatibility() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            BleHeartRatePermissionPlanner.requiredPermissions(Build.VERSION_CODES.R)
        )
    }

    @Test
    fun permissionPlannerOnlyAllowsExplicitUserActionRequests() {
        assertFalse(
            BleHeartRatePermissionPlanner.shouldRequestPermissions(
                BleHeartRatePermissionTrigger.APP_STARTUP
            )
        )
        assertFalse(
            BleHeartRatePermissionPlanner.shouldRequestPermissions(
                BleHeartRatePermissionTrigger.SCREEN_OPENED
            )
        )
        assertTrue(
            BleHeartRatePermissionPlanner.shouldRequestPermissions(
                BleHeartRatePermissionTrigger.EXPLICIT_USER_ACTION
            )
        )
    }

    @Test
    fun providerStatesMapToAbstractHeartRateStateWithoutBleSdkModels() {
        val selected = BleHeartRateDeviceSelection(
            identifier = "D8:F0:42:01:90:D7",
            displayName = "HUAWEI Band HR-OD7"
        )

        val waiting = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.CONNECTED_WAITING_FOR_DATA,
            message = "waiting",
            selectedDevice = selected
        ).toHeartRateState()
        val live = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.LIVE_BPM,
            message = "live",
            selectedDevice = selected,
            bpm = 99,
            measuredAt = "2026-07-06T01:14:00Z"
        ).toHeartRateState()
        val disconnected = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.DISCONNECTED,
            message = "disconnected",
            selectedDevice = selected,
            bpm = 99,
            measuredAt = "2026-07-06T01:14:00Z"
        ).toHeartRateState()

        assertEquals(HeartRateStateKind.DEVICE_CONNECTED_NO_READING, waiting.kind)
        assertEquals(HeartRateSourceKind.DEVICE, waiting.sourceKind)
        assertEquals("D8:F0:42:01:90:D7", waiting.sourceId)
        assertEquals("HUAWEI Band HR-OD7", waiting.sourceLabel)
        assertEquals(HeartRateStateKind.DEVICE_READING, live.kind)
        assertEquals(99, live.bpm)
        assertEquals(HeartRateStateKind.STALE_READING, disconnected.kind)
        assertEquals(HeartRateUnavailableReason.DEVICE_DISCONNECTED, disconnected.unavailableReason)
    }

    @Test
    fun providerCanRepresentPermissionBluetoothScanSelectionAndRecoverableErrorStates() {
        val permission = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.PERMISSION_REQUIRED,
            message = "permission",
            missingPermissions = listOf(Manifest.permission.BLUETOOTH_SCAN)
        )
        val bluetoothOff = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.BLUETOOTH_DISABLED,
            message = "bluetooth"
        )
        val candidate = BleHeartRateDeviceCandidate(
            identifier = "D8:F0:42:01:90:D7",
            displayName = "HUAWEI Band HR-OD7",
            rssi = -46,
            advertisesHeartRateService = true
        )
        val found = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.DEVICE_FOUND,
            message = "found",
            candidate = candidate
        )
        val selected = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.DEVICE_SELECTED,
            message = "selected",
            selectedDevice = BleHeartRateDeviceSelection(candidate.identifier, candidate.displayName)
        )
        val error = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.ERROR,
            message = "failed",
            selectedDevice = selected.selectedDevice,
            recoverableReason = BleHeartRateRecoverableReason.CONNECTION_FAILED
        )

        assertEquals(HeartRateStateKind.PERMISSION_UNAVAILABLE, permission.toHeartRateState().kind)
        assertEquals(
            HeartRateUnavailableReason.PERMISSION_REQUIRED,
            permission.toHeartRateState().unavailableReason
        )
        assertEquals(
            HeartRateUnavailableReason.BLUETOOTH_DISABLED,
            bluetoothOff.toHeartRateState().unavailableReason
        )
        assertEquals("HUAWEI Band HR-OD7", found.toHeartRateState().sourceLabel)
        assertEquals("D8:F0:42:01:90:D7", selected.toHeartRateState().sourceId)
        assertEquals(HeartRateUnavailableReason.CONNECTION_FAILED, error.toHeartRateState().unavailableReason)
    }
}
