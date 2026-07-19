package com.liujyks.trainflow.core.health

import android.Manifest
import android.os.Build
import com.liujyks.trainflow.core.model.HeartRateFact
import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateTechnicalFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals(HeartRateFact.WAITING_FIRST_DATA, waiting.fact)
        assertEquals(HeartRateSourceKind.DEVICE, waiting.sourceKind)
        assertEquals("D8:F0:42:01:90:D7", waiting.sourceId)
        assertEquals("HUAWEI Band HR-OD7", waiting.sourceLabel)
        assertEquals(HeartRateStateKind.DEVICE_READING, live.kind)
        assertEquals(HeartRateFact.LIVE, live.fact)
        assertEquals(99, live.bpm)
        assertEquals(HeartRateStateKind.STALE_READING, disconnected.kind)
        assertEquals(HeartRateFact.LINK_DISCONNECTED, disconnected.fact)
        assertNull(disconnected.bpm)
        assertNull(disconnected.measuredAt)
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
        val scanning = BleHeartRateScanState(
            kind = BleHeartRateScanStateKind.SCANNING,
            message = "scanning"
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

        assertEquals(HeartRateFact.PERMISSION_REQUIRED, permission.toHeartRateState().fact)
        assertEquals(HeartRateFact.BLUETOOTH_OFF, bluetoothOff.toHeartRateState().fact)
        assertEquals(BleHeartRateScanStateKind.SCANNING, scanning.kind)
        assertEquals("D8:F0:42:01:90:D7", selected.toHeartRateState().sourceId)
        assertEquals(HeartRateFact.NOT_CONNECTED, selected.toHeartRateState().fact)
        assertEquals(HeartRateFact.TECHNICAL_FAILURE, error.toHeartRateState().fact)
        assertEquals(
            HeartRateTechnicalFailure.CONNECT_FAILED,
            error.toHeartRateState().technicalFailure
        )
    }

    @Test
    fun availabilityRefreshDoesNotReplaceActiveLiveProviderState() {
        val live = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.LIVE_BPM,
            message = "live",
            selectedDevice = BleHeartRateDeviceSelection(
                identifier = "D8:F0:42:01:90:D7",
                displayName = "HUAWEI Band HR-OD7"
            ),
            bpm = 105,
            measuredAt = "2026-07-19T13:16:04Z"
        )

        val resolved = providerStateAfterAvailabilityRefresh(
            currentState = live,
            availabilityState = BleHeartRateProviderState.noSource()
        )

        assertEquals(live, resolved)
    }

    @Test
    fun legacyMalformedErrorDoesNotPublishTechnicalFailureOrRefreshWithNewTime() {
        val selected = BleHeartRateDeviceSelection("id", "Band")
        val withPreviousValid = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.ERROR,
            message = "parser detail must not enter presentation",
            selectedDevice = selected,
            bpm = 88,
            measuredAt = "2026-07-19T13:16:04Z",
            recoverableReason = BleHeartRateRecoverableReason.PARSE_FAILED
        ).toHeartRateState()
        val beforeAnyValid = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.ERROR,
            message = "malformed",
            selectedDevice = selected,
            recoverableReason = BleHeartRateRecoverableReason.PARSE_FAILED
        ).toHeartRateState()

        assertEquals(HeartRateFact.LIVE, withPreviousValid.fact)
        assertEquals("2026-07-19T13:16:04Z", withPreviousValid.measuredAt)
        assertNull(withPreviousValid.technicalFailure)
        assertEquals(HeartRateFact.WAITING_FIRST_DATA, beforeAnyValid.fact)
        assertNull(beforeAnyValid.technicalFailure)
    }

    @Test
    fun staleFailureAndStopAlwaysClearLegacyReadingFields() {
        val selected = BleHeartRateDeviceSelection("id", "Band")
        val states = listOf(
            BleHeartRateProviderStateKind.STALE,
            BleHeartRateProviderStateKind.DISCONNECTED,
            BleHeartRateProviderStateKind.STOPPED,
            BleHeartRateProviderStateKind.ERROR
        ).map { kind ->
            BleHeartRateProviderState(
                kind = kind,
                message = "legacy",
                selectedDevice = selected,
                bpm = 88,
                measuredAt = "old",
                recoverableReason = if (kind == BleHeartRateProviderStateKind.ERROR) {
                    BleHeartRateRecoverableReason.CONNECTION_FAILED
                } else {
                    null
                }
            ).toHeartRateState()
        }

        states.forEach { state ->
            assertNull(state.bpm)
            assertNull(state.measuredAt)
            assertTrue(state.isValidE17State())
        }
    }
}
