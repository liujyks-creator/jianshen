package com.liujyks.trainflow.feature.workoutsession

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRatePermissionBoundaryTest {
    @Test
    fun manifestOnlyAllowsScopedBleRuntimePermissionsForHeartRateSetup() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android.permission.BLUETOOTH_SCAN"))
        assertTrue(manifest.contains("android:usesPermissionFlags=\"neverForLocation\""))
        assertTrue(manifest.contains("android.permission.BLUETOOTH_CONNECT"))
        assertTrue(manifest.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertTrue(manifest.contains("android:maxSdkVersion=\"30\""))

        val forbiddenPermissions = listOf(
            "android.permission.BODY_SENSORS",
            "android.permission.BODY_SENSORS_BACKGROUND",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.health.READ_EXERCISE",
            "android.permission.health.READ_ACTIVE_CALORIES_BURNED",
            "android.permission.health.READ_TOTAL_CALORIES_BURNED"
        )

        forbiddenPermissions.forEach { permission ->
            assertFalse(manifest.contains("android:name=\"$permission\""))
        }
        assertFalse(manifest.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertFalse(manifest.contains("android.permission.USE_EXACT_ALARM"))
        assertFalse(manifest.contains("android.permission.FOREGROUND_SERVICE"))
    }

    @Test
    fun debugHeartRateSmokeAddsSeparateDiagnosticLauncherWithoutReplacingMain() {
        val mainManifest = File("src/main/AndroidManifest.xml").readText()
        val debugManifest = File("src/debug/AndroidManifest.xml").readText()

        assertTrue(mainManifest.contains("android.intent.category.LAUNCHER"))
        assertTrue(mainManifest.contains(".app.MainActivity"))
        assertFalse(mainManifest.contains("DebugEntryActivity"))
        assertFalse(mainManifest.contains("HeartRateBroadcastSmokeActivity"))
        assertFalse(mainManifest.contains("HR Broadcast Smoke"))
        assertFalse(mainManifest.contains("进入 TrainFlow"))

        assertTrue(debugManifest.contains(".app.DebugEntryActivity"))
        assertTrue(debugManifest.contains(".app.HeartRateBroadcastSmokeActivity"))
        assertTrue(debugManifest.contains(".app.E17Band9HrsRevalidationActivity"))
        assertTrue(debugManifest.contains("android:label=\"TrainFlow Debug\""))
        assertTrue(debugManifest.contains("android.intent.action.MAIN"))
        assertTrue(debugManifest.contains("android.intent.category.LAUNCHER"))
    }
}
