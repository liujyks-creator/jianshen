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
    fun legacyMalformedWithCachedReadingFailsClosedAndClearsAllReadingFields() {
        val selected = BleHeartRateDeviceSelection("id", "Band")
        val state = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.ERROR,
            message = "parser detail must not enter presentation",
            selectedDevice = selected,
            bpm = 88,
            measuredAt = "2026-07-19T13:16:04Z",
            recoverableReason = BleHeartRateRecoverableReason.PARSE_FAILED
        ).toHeartRateState()

        assertEquals(HeartRateFact.DATA_INTERRUPTED, state.fact)
        assertFalse(state.fact == HeartRateFact.LIVE)
        assertNull(state.bpm)
        assertNull(state.measuredAt)
        assertNull(state.recordedAt)
        assertNull(state.technicalFailure)
        assertTrue(state.isValidE17State())
    }

    @Test
    fun legacyMalformedWithoutCachedReadingIsInterruptedNotTechnicalFailure() {
        val state = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.ERROR,
            message = "malformed",
            selectedDevice = BleHeartRateDeviceSelection("id", "Band"),
            recoverableReason = BleHeartRateRecoverableReason.PARSE_FAILED
        ).toHeartRateState()

        assertEquals(HeartRateFact.DATA_INTERRUPTED, state.fact)
        assertNull(state.technicalFailure)
        assertNull(state.bpm)
        assertNull(state.measuredAt)
        assertNull(state.recordedAt)
    }

    @Test
    fun repeatedLegacyMalformedNeverRestoresCachedLiveReading() {
        val selected = BleHeartRateDeviceSelection("id", "Band")
        val live = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.LIVE_BPM,
            message = "valid",
            selectedDevice = selected,
            bpm = 88,
            measuredAt = "2026-07-19T13:16:04Z"
        ).toHeartRateState()

        val malformedStates = List(2) {
            BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.ERROR,
                message = "malformed $it",
                selectedDevice = selected,
                bpm = live.bpm,
                measuredAt = live.measuredAt,
                recoverableReason = BleHeartRateRecoverableReason.PARSE_FAILED
            ).toHeartRateState()
        }

        assertEquals(HeartRateFact.LIVE, live.fact)
        malformedStates.forEach { state ->
            assertEquals(HeartRateFact.DATA_INTERRUPTED, state.fact)
            assertNull(state.bpm)
            assertNull(state.measuredAt)
            assertNull(state.recordedAt)
        }
    }

    @Test
    fun validLegacySampleAfterMalformedCreatesLiveFromOnlyTheNewReading() {
        val selected = BleHeartRateDeviceSelection("id", "Band")
        val malformed = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.ERROR,
            message = "malformed",
            selectedDevice = selected,
            bpm = 88,
            measuredAt = "2026-07-19T13:16:04Z",
            recoverableReason = BleHeartRateRecoverableReason.PARSE_FAILED
        ).toHeartRateState()
        val nextValid = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.LIVE_BPM,
            message = "valid",
            selectedDevice = selected,
            bpm = 93,
            measuredAt = "2026-07-19T13:16:06Z"
        ).toHeartRateState()

        assertEquals(HeartRateFact.DATA_INTERRUPTED, malformed.fact)
        assertEquals(HeartRateFact.LIVE, nextValid.fact)
        assertEquals(93, nextValid.bpm)
        assertEquals("2026-07-19T13:16:06Z", nextValid.measuredAt)
        assertNull(nextValid.recordedAt)
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

    @Test
    fun otherLegacyReasonsKeepTheirDistinctPublicFacts() {
        val selected = BleHeartRateDeviceSelection("saved-id", "Saved Band")
        val disconnected = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.DISCONNECTED,
            message = "explicit disconnect",
            selectedDevice = selected,
            bpm = 88,
            measuredAt = "old"
        ).toHeartRateState()
        val technicalReasons = listOf(
            BleHeartRateRecoverableReason.CONNECTION_FAILED to
                HeartRateTechnicalFailure.CONNECT_FAILED,
            BleHeartRateRecoverableReason.SERVICE_MISSING to
                HeartRateTechnicalFailure.SERVICE_DISCOVERY_FAILED,
            BleHeartRateRecoverableReason.DESCRIPTOR_WRITE_FAILED to
                HeartRateTechnicalFailure.CCCD_FAILED
        )
        val stopped = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.STOPPED,
            message = "user stop",
            selectedDevice = selected,
            bpm = 88,
            measuredAt = "old"
        ).toHeartRateState()
        val savedHint = BleHeartRateProviderState(
            kind = BleHeartRateProviderStateKind.DEVICE_SELECTED,
            message = "saved hint",
            selectedDevice = selected,
            bpm = 88,
            measuredAt = "old"
        ).toHeartRateState()

        assertEquals(HeartRateFact.LINK_DISCONNECTED, disconnected.fact)
        assertEquals(HeartRateFact.INTENTIONAL_STOP, stopped.fact)
        assertEquals(HeartRateFact.NOT_CONNECTED, savedHint.fact)
        assertFalse(savedHint.fact == HeartRateFact.LIVE)
        listOf(disconnected, stopped, savedHint).forEach { state ->
            assertNull(state.bpm)
            assertNull(state.measuredAt)
            assertNull(state.recordedAt)
        }
        technicalReasons.forEach { (legacyReason, expectedFailure) ->
            val state = BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.ERROR,
                message = "must not classify by message",
                selectedDevice = selected,
                bpm = 88,
                measuredAt = "old",
                recoverableReason = legacyReason
            ).toHeartRateState()

            assertEquals(HeartRateFact.TECHNICAL_FAILURE, state.fact)
            assertEquals(expectedFailure, state.technicalFailure)
            assertNull(state.bpm)
            assertNull(state.measuredAt)
            assertNull(state.recordedAt)
        }
    }
}
