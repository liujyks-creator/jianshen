package com.liujyks.trainflow.core.health

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleHeartRateProviderBoundaryTest {
    @Test
    fun compatibilityBoundaryContainsOnlyResourceFreeScanFacts() {
        val source = File(
            "src/main/java/com/liujyks/trainflow/core/health/BleHeartRateProviderBoundary.kt"
        ).readText()

        assertTrue(source.contains("data class BleHeartRateDeviceCandidate"))
        assertTrue(source.contains("data class BleHeartRateScanState"))
        assertTrue(source.contains("enum class BleHeartRateScanStateKind"))
        assertTrue(source.contains("enum class BleHeartRateRecoverableReason"))
        assertFalse(source.contains("BluetoothGatt"))
        assertFalse(source.contains("ScanCallback"))
        assertFalse(source.contains("BleHeartRateProviderState"))
        assertFalse(source.contains("BleHeartRateDeviceSelection"))
    }

    @Test
    fun candidateAndScanFactsRemainStableForOwnerAndSettings() {
        val candidate = BleHeartRateDeviceCandidate(
            identifier = "D8:F0:42:01:90:D7",
            displayName = "HUAWEI Band HR-OD7",
            rssi = -46,
            advertisesHeartRateService = true
        )
        val scan = BleHeartRateScanState(
            kind = BleHeartRateScanStateKind.ERROR,
            message = "failed",
            recoverableReason = BleHeartRateRecoverableReason.SCAN_FAILED
        )

        assertTrue(candidate.advertisesHeartRateService)
        assertEquals(BleHeartRateScanStateKind.ERROR, scan.kind)
        assertEquals(BleHeartRateRecoverableReason.SCAN_FAILED, scan.recoverableReason)
        assertEquals(
            listOf(BleHeartRateRecoverableReason.SCAN_FAILED),
            BleHeartRateRecoverableReason.entries
        )
    }
}
