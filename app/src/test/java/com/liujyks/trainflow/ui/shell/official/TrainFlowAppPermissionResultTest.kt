package com.liujyks.trainflow.ui.shell.official

import com.liujyks.trainflow.feature.settings.HeartRateBlePermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainFlowAppPermissionResultTest {
    @Test
    fun deniedPermissionRequestResultStaysRetryableDenied() {
        val status = resolveHeartRateBlePermissionRequestResult(
            requiredPermissions = listOf("android.permission.BLUETOOTH_SCAN"),
            requestResult = mapOf("android.permission.BLUETOOTH_SCAN" to false),
            allPermissionsCurrentlyGranted = false,
            hasPermanentlyDeniedPermissions = false
        )

        assertEquals(HeartRateBlePermissionStatus.DENIED, status)
    }

    @Test
    fun permanentlyDeniedPermissionRequestResultOpensSettingsPath() {
        val status = resolveHeartRateBlePermissionRequestResult(
            requiredPermissions = listOf("android.permission.BLUETOOTH_SCAN"),
            requestResult = mapOf("android.permission.BLUETOOTH_SCAN" to false),
            allPermissionsCurrentlyGranted = false,
            hasPermanentlyDeniedPermissions = true
        )

        assertEquals(HeartRateBlePermissionStatus.PERMANENTLY_DENIED, status)
    }

    @Test
    fun grantedPermissionRequestResultWinsOverPreviousState() {
        val status = resolveHeartRateBlePermissionRequestResult(
            requiredPermissions = listOf(
                "android.permission.BLUETOOTH_SCAN",
                "android.permission.BLUETOOTH_CONNECT"
            ),
            requestResult = mapOf(
                "android.permission.BLUETOOTH_SCAN" to true,
                "android.permission.BLUETOOTH_CONNECT" to true
            ),
            allPermissionsCurrentlyGranted = false,
            hasPermanentlyDeniedPermissions = false
        )

        assertEquals(HeartRateBlePermissionStatus.GRANTED, status)
    }
}
