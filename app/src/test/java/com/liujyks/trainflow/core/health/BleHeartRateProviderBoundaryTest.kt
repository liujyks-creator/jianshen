package com.liujyks.trainflow.core.health

import android.Manifest
import android.os.Build
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
    fun compatibilityBoundaryRetainsOnlyPureScanCandidateAndReasonFacts() {
        val candidate = BleHeartRateDeviceCandidate(
            identifier = "D8:F0:42:01:90:D7",
            displayName = "HUAWEI Band HR-OD7",
            rssi = -46,
            advertisesHeartRateService = true
        )
        val scan = BleHeartRateScanState(
            kind = BleHeartRateScanStateKind.ERROR,
            message = "scan failed",
            recoverableReason = BleHeartRateRecoverableReason.SCAN_FAILED
        )

        assertEquals("D8:F0:42:01:90:D7", candidate.identifier)
        assertTrue(candidate.advertisesHeartRateService)
        assertEquals(BleHeartRateScanStateKind.ERROR, scan.kind)
        assertEquals(
            setOf(BleHeartRateRecoverableReason.SCAN_FAILED),
            BleHeartRateRecoverableReason.entries.toSet()
        )
    }
}
