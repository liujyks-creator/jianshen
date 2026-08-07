package com.liujyks.trainflow.core.health

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidBleHeartRateProviderScanFilterTest {
    @Test
    fun legacyProviderAndScannerOwnershipEntrypointsAreRetired() {
        val healthDirectory = File("src/main/java/com/liujyks/trainflow/core/health")
        val application = File(
            "src/main/java/com/liujyks/trainflow/app/TrainFlowApplication.kt"
        ).readText()
        val debugEntries = listOf(
            "src/debug/java/com/liujyks/trainflow/app/HeartRateBroadcastSmokeActivity.kt",
            "src/debug/java/com/liujyks/trainflow/app/E17Band9HrsRevalidationActivity.kt"
        ).map { path -> File(path).readText() }

        assertFalse(File(healthDirectory, "AndroidBleHeartRateProvider.kt").exists())
        assertFalse(File(healthDirectory, "HeartRateDeviceScanner.kt").exists())
        assertTrue(application.contains("HeartRateRuntimeOwner(this)"))
        debugEntries.forEach { source ->
            assertFalse(source.contains("BluetoothGatt"))
            assertFalse(source.contains("ScanCallback"))
            assertFalse(source.contains("connectGatt"))
            assertFalse(source.contains("HeartRateRuntimeOwner("))
        }
    }
}
