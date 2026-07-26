package com.liujyks.trainflow.ui.shell.official

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateLiveProviderLifecycleTest {
    @Test
    fun composeConsumesApplicationStateWithoutOwningBleResources() {
        val source = File(
            "src/main/java/com/liujyks/trainflow/ui/shell/official/TrainFlowApp.kt"
        ).readText()

        assertFalse(source.contains("AndroidHeartRateDeviceScanner"))
        assertFalse(source.contains("AndroidBleHeartRateProvider"))
        assertFalse(source.contains("DisposableEffect(heartRate"))
        assertTrue(source.contains("heartRateState: HeartRateState"))
        assertTrue(source.contains("onDisconnectHeartRateDevice"))
        assertTrue(source.contains("onReconnectHeartRateDevice"))
    }
}
