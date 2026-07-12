package com.liujyks.trainflow.core.health

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidBleHeartRateProviderScanFilterTest {
    @Test
    fun providerScanUsesHeartRateServiceFilter() {
        val source = File("src/main/java/com/liujyks/trainflow/core/health/AndroidBleHeartRateProvider.kt")
            .readText()

        assertTrue(source.contains("import android.bluetooth.le.ScanFilter"))
        assertTrue(source.contains("import android.os.ParcelUuid"))
        assertTrue(source.contains("setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID))"))
        assertTrue(source.contains("platformCalls.startScan(leScanner, heartRateServiceScanFilters(), settings, callback)"))
        assertTrue(source.contains("0000180d-0000-1000-8000-00805f9b34fb"))
        assertFalse(source.contains("startScan(null, settings, callback)"))
    }
}
