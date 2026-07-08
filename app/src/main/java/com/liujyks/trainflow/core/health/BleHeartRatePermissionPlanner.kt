package com.liujyks.trainflow.core.health

import android.Manifest
import android.os.Build

internal object BleHeartRatePermissionPlanner {
    fun requiredPermissions(apiLevel: Int = Build.VERSION.SDK_INT): List<String> {
        return if (apiLevel >= Build.VERSION_CODES.S) {
            listOf(
                BLUETOOTH_SCAN_PERMISSION,
                BLUETOOTH_CONNECT_PERMISSION
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun missingPermissions(
        grantedPermissions: Set<String>,
        apiLevel: Int = Build.VERSION.SDK_INT
    ): List<String> {
        return requiredPermissions(apiLevel).filterNot { it in grantedPermissions }
    }

    fun shouldRequestPermissions(trigger: BleHeartRatePermissionTrigger): Boolean {
        return trigger == BleHeartRatePermissionTrigger.EXPLICIT_USER_ACTION
    }
}

private const val BLUETOOTH_SCAN_PERMISSION = "android.permission.BLUETOOTH_SCAN"
private const val BLUETOOTH_CONNECT_PERMISSION = "android.permission.BLUETOOTH_CONNECT"

internal enum class BleHeartRatePermissionTrigger {
    APP_STARTUP,
    SCREEN_OPENED,
    EXPLICIT_USER_ACTION
}
