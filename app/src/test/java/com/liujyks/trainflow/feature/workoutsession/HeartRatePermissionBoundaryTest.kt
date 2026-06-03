package com.liujyks.trainflow.feature.workoutsession

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class HeartRatePermissionBoundaryTest {
    @Test
    fun manifestDoesNotRequestHealthSensorOrBluetoothPermissions() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val forbiddenPermissions = listOf(
            "android.permission.BODY_SENSORS",
            "android.permission.BODY_SENSORS_BACKGROUND",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.health.READ_EXERCISE",
            "android.permission.health.READ_ACTIVE_CALORIES_BURNED",
            "android.permission.health.READ_TOTAL_CALORIES_BURNED"
        )

        forbiddenPermissions.forEach { permission ->
            assertFalse(manifest.contains(permission))
        }
        assertFalse(manifest.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertFalse(manifest.contains("android.permission.USE_EXACT_ALARM"))
        assertFalse(manifest.contains("android.permission.FOREGROUND_SERVICE"))
    }
}
