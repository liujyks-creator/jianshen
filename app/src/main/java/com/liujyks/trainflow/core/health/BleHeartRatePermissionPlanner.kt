package com.liujyks.trainflow.core.health

import android.Manifest
import android.os.Build

internal object BleHeartRatePermissionPlanner {
    fun requiredPermissions(apiLevel: Int = Build.VERSION.SDK_INT): List<String> {
        return if (apiLevel >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
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

internal enum class BleHeartRatePermissionTrigger {
    APP_STARTUP,
    SCREEN_OPENED,
    EXPLICIT_USER_ACTION
}
