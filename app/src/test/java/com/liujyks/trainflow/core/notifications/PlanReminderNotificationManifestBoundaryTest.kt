package com.liujyks.trainflow.core.notifications

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanReminderNotificationManifestBoundaryTest {
    @Test
    fun manifestKeepsNotificationAndScopedBlePermissionBoundary() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue(manifest.contains("android.permission.BLUETOOTH_SCAN"))
        assertTrue(manifest.contains("android:usesPermissionFlags=\"neverForLocation\""))
        assertTrue(manifest.contains("android.permission.BLUETOOTH_CONNECT"))
        assertTrue(manifest.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertTrue(manifest.contains("android:maxSdkVersion=\"30\""))
        forbiddenPermissions.forEach { permission ->
            assertFalse(
                "Manifest must not request $permission",
                manifest.contains("android:name=\"$permission\"")
            )
        }
        assertFalse(manifest.contains("PlanReminderNotificationReceiver"))
        assertFalse(manifest.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertFalse(manifest.contains("android.permission.USE_EXACT_ALARM"))
        assertFalse(manifest.contains("android.permission.FOREGROUND_SERVICE"))
    }

    @Test
    fun activeWorkoutNotificationDoesNotEnableForegroundServiceForE72() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val activeContracts = File(
            "src/main/java/com/liujyks/trainflow/core/notifications/ActiveWorkoutNotificationContracts.kt"
        ).readText()
        val activeAndroid = File(
            "src/main/java/com/liujyks/trainflow/core/notifications/AndroidActiveWorkoutNotifications.kt"
        ).readText()

        assertTrue(activeContracts.contains("ActiveWorkoutNotificationState"))
        assertTrue(activeAndroid.contains("NotificationManager"))
        assertTrue(activeAndroid.contains("notify("))
        assertFalse(manifest.contains("android.permission.FOREGROUND_SERVICE"))
        assertFalse(manifest.contains("android:foregroundServiceType"))
        assertFalse(manifest.contains("ActiveWorkoutForegroundService"))
        assertFalse(activeAndroid.contains("startForeground"))
        assertFalse(activeAndroid.contains("ServiceCompat.startForeground"))
    }

    private companion object {
        val forbiddenPermissions = listOf(
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.USE_EXACT_ALARM",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.BODY_SENSORS",
            "android.permission.BODY_SENSORS_BACKGROUND",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.settings.action.MANAGE_OVERLAY_PERMISSION",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.health.READ_EXERCISE",
            "android.permission.health.READ_ACTIVE_CALORIES_BURNED",
            "android.permission.health.READ_TOTAL_CALORIES_BURNED"
        )
    }
}
