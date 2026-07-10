package com.liujyks.trainflow.ui.shell.official

import com.liujyks.trainflow.core.health.BleHeartRateProviderStateKind
import com.liujyks.trainflow.feature.settings.HeartRateBlePermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HeartRateLiveProviderLifecycleTest {
    @Test
    fun disabledDisplayStopsProviderInsteadOfConnecting() {
        val action = heartRateLiveProviderLifecycleAction(
            displayEnabled = false,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            providerStateKind = BleHeartRateProviderStateKind.DEVICE_SELECTED
        )

        assertEquals(HeartRateLiveProviderLifecycleAction.STOP_AND_DISCONNECT, action)
    }

    @Test
    fun missingPermissionStopsProviderInsteadOfConnecting() {
        val action = heartRateLiveProviderLifecycleAction(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.DENIED,
            providerStateKind = BleHeartRateProviderStateKind.DEVICE_SELECTED
        )

        assertEquals(HeartRateLiveProviderLifecycleAction.STOP_AND_DISCONNECT, action)
    }

    @Test
    fun bluetoothDisabledStopsProviderInsteadOfConnecting() {
        val action = heartRateLiveProviderLifecycleAction(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            providerStateKind = BleHeartRateProviderStateKind.BLUETOOTH_DISABLED
        )

        assertEquals(HeartRateLiveProviderLifecycleAction.STOP_AND_DISCONNECT, action)
    }

    @Test
    fun selectedDeviceConnectsOnlyAfterDisplayAndPermissionAreReady() {
        val action = heartRateLiveProviderLifecycleAction(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            providerStateKind = BleHeartRateProviderStateKind.DEVICE_SELECTED
        )

        assertEquals(HeartRateLiveProviderLifecycleAction.CONNECT_SELECTED_DEVICE, action)
    }

    @Test
    fun noSourceDoesNotScanOrConnect() {
        val action = heartRateLiveProviderLifecycleAction(
            displayEnabled = true,
            blePermissionStatus = HeartRateBlePermissionStatus.GRANTED,
            providerStateKind = BleHeartRateProviderStateKind.NO_SOURCE
        )

        assertEquals(HeartRateLiveProviderLifecycleAction.NONE, action)
    }
}
