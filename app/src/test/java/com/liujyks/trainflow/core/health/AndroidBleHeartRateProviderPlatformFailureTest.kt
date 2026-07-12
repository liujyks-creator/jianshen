package com.liujyks.trainflow.core.health

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AndroidBleHeartRateProviderPlatformFailureTest {
    private var provider: AndroidBleHeartRateProvider? = null

    @After
    fun tearDown() {
        provider?.close()
    }

    @Test
    fun adapterAvailabilitySecurityRaceCancelsToPermissionStateAndRecoveryDoesNotAutoScan() {
        val boundary = ThrowingBoundary(
            BlePlatformOperation.READ_ADAPTER_ENABLED,
            SecurityException("permission changed")
        )
        provider = createProvider(boundary)

        provider!!.refreshAvailability()

        assertEquals(BleHeartRateProviderStateKind.PERMISSION_REQUIRED, provider!!.providerState.value.kind)
        assertFalse(provider!!.scanState.value.kind == BleHeartRateScanStateKind.SCANNING)

        boundary.failure = null
        provider!!.refreshAvailability()
        assertFalse(provider!!.scanState.value.kind == BleHeartRateScanStateKind.SCANNING)
        assertFalse(provider!!.providerState.value.reconnectInProgress)
    }

    @Test
    fun startScanSecurityRaceEndsProductionScanGenerationAndDoesNotRestartAfterRecovery() {
        val boundary = ThrowingBoundary(
            BlePlatformOperation.START_SCAN,
            SecurityException("permission changed before startScan")
        )
        provider = createProvider(boundary)

        provider!!.startScan()

        assertEquals(BleHeartRateScanStateKind.ERROR, provider!!.scanState.value.kind)
        assertEquals(BleHeartRateProviderStateKind.PERMISSION_REQUIRED, provider!!.providerState.value.kind)
        assertFalse(provider!!.providerState.value.reconnectInProgress)

        boundary.failure = null
        provider!!.refreshAvailability()
        assertFalse(provider!!.scanState.value.kind == BleHeartRateScanStateKind.SCANNING)
    }

    private fun createProvider(boundary: ThrowingBoundary): AndroidBleHeartRateProvider {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        @Suppress("DEPRECATION")
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(true)
        return AndroidBleHeartRateProvider(application, platformCalls = boundary)
    }

    private class ThrowingBoundary(
        private val operation: BlePlatformOperation,
        failure: RuntimeException?
    ) : BlePlatformCallBoundary {
        var failure: RuntimeException? = failure

        override fun <T> call(operation: BlePlatformOperation, block: () -> T): BlePlatformCallResult<T> {
            val current = failure
            return if (operation == this.operation && current != null) {
                BlePlatformCallResult.ExpectedFailure(operation, current)
            } else {
                BlePlatformCallResult.Success(block())
            }
        }
    }
}
