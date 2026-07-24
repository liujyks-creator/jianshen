package com.liujyks.trainflow.ui.shell.official

import com.liujyks.trainflow.core.health.HeartRateRuntimeAction
import com.liujyks.trainflow.feature.settings.HeartRateBlePermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HeartRateLiveProviderLifecycleTest {
    @Test
    fun disabledDisplayDisablesApplicationOwner() {
        assertEquals(
            HeartRateRuntimeAction.Disable,
            heartRateRuntimeEligibilityAction(
                displayEnabled = false,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
                bluetoothAvailable = true
            )
        )
    }

    @Test
    fun missingPermissionFailsClosedWithoutScanOrConnect() {
        assertEquals(
            HeartRateRuntimeAction.PermissionLost,
            heartRateRuntimeEligibilityAction(
                displayEnabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.DENIED,
                bluetoothAvailable = true
            )
        )
    }

    @Test
    fun bluetoothDisabledFailsClosedWithoutScanOrConnect() {
        assertEquals(
            HeartRateRuntimeAction.BluetoothOff,
            heartRateRuntimeEligibilityAction(
                displayEnabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
                bluetoothAvailable = false
            )
        )
    }

    @Test
    fun eligibleForegroundOnlyEnablesAndNeverAutoConnectsSavedHint() {
        assertEquals(
            HeartRateRuntimeAction.Enable,
            heartRateRuntimeEligibilityAction(
                displayEnabled = true,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
                bluetoothAvailable = true
            )
        )
    }
}
