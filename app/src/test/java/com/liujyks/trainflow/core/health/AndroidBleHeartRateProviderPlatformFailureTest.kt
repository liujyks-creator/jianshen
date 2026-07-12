package com.liujyks.trainflow.core.health

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import java.util.EnumMap
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AndroidBleHeartRateProviderPlatformFailureTest {
    private var provider: AndroidBleHeartRateProvider? = null

    @After fun tearDown() { provider?.close() }

    @Test
    fun availabilityAndStartScanFailuresEndScanAndNeverAutoRecover() {
        val platform = FakePlatform()
        provider = createProvider(platform)
        platform.fail(BlePlatformOperation.READ_ADAPTER_ENABLED)
        provider!!.refreshAvailability()
        assertPermissionFailure()

        platform.clearFailure()
        provider!!.refreshAvailability()
        assertFalse(provider!!.scanState.value.kind == BleHeartRateScanStateKind.SCANNING)

        platform.fail(BlePlatformOperation.START_SCAN)
        provider!!.startScan()
        assertEquals(BleHeartRateScanStateKind.ERROR, provider!!.scanState.value.kind)
        assertPermissionFailure()
        platform.clearFailure()
        provider!!.refreshAvailability()
        assertFalse(provider!!.scanState.value.kind == BleHeartRateScanStateKind.SCANNING)
        assertEquals(0, platform.connectCount)
    }

    @Test
    fun connectDiscoverServiceNotificationAndDescriptorFailuresUseRealProviderCallbackPath() {
        listOf(
            BlePlatformOperation.CONNECT_GATT,
            BlePlatformOperation.DISCOVER_SERVICES,
            BlePlatformOperation.READ_GATT_SERVICE,
            BlePlatformOperation.CONFIGURE_NOTIFICATION,
            BlePlatformOperation.WRITE_DESCRIPTOR
        ).forEach { operation ->
            provider?.close()
            val scheduler = FakeScheduler()
            val platform = FakePlatform()
            provider = createProvider(platform, scheduler)
            selectTarget(platform)
            platform.fail(operation)

            when (operation) {
                BlePlatformOperation.CONNECT_GATT -> {
                    provider!!.connectSelectedDevice()
                    idleMain()
                }
                else -> {
                    provider!!.connectSelectedDevice()
                    platform.callback!!.onConnectionStateChange(platform.gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
                    idleMain()
                    if (operation !in setOf(BlePlatformOperation.DISCOVER_SERVICES)) {
                        platform.callback!!.onServicesDiscovered(platform.gatt, BluetoothGatt.GATT_SUCCESS)
                        idleMain()
                    }
                }
            }

            assertPermissionFailure()
            assertFalse(provider!!.providerState.value.freshnessReason == HeartRateFreshnessReason.GATT_DISCONNECTED)
            if (operation != BlePlatformOperation.CONNECT_GATT) {
                assertEquals(1, platform.disconnectCount)
                assertEquals(1, platform.closeCount)
                val state = provider!!.providerState.value
                platform.callback!!.onConnectionStateChange(platform.gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED)
                platform.callback!!.onServicesDiscovered(platform.gatt, BluetoothGatt.GATT_SUCCESS)
                idleMain()
                assertEquals(state, provider!!.providerState.value)
                scheduler.runCanceled(HeartRateScheduledTask.WATCHDOG)
                assertEquals(1, platform.connectCount)
            }

            platform.clearFailure()
            provider!!.refreshAvailability()
            assertFalse(provider!!.providerState.value.reconnectInProgress)
            provider!!.connectSelectedDevice()
            assertEquals(2, platform.connectCount)
        }
    }

    @Test
    fun disconnectAndCloseFailuresReleaseIdentityRemainIdempotentAndDoNotForgeDisconnect() {
        listOf(BlePlatformOperation.DISCONNECT_GATT, BlePlatformOperation.CLOSE_GATT).forEach { operation ->
            provider?.close()
            val platform = FakePlatform()
            provider = createProvider(platform)
            selectTarget(platform)
            provider!!.connectSelectedDevice()
            platform.callback!!.onConnectionStateChange(platform.gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
            idleMain()
            platform.fail(operation)

            provider!!.disconnect()
            provider!!.disconnect()

            assertEquals(BleHeartRateProviderStateKind.STOPPED, provider!!.providerState.value.kind)
            assertNull(provider!!.providerState.value.freshnessReason)
            assertEquals(1, platform.disconnectCount)
            assertEquals(1, platform.closeCount)
            val stopped = provider!!.providerState.value
            platform.callback!!.onConnectionStateChange(platform.gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED)
            idleMain()
            assertEquals(stopped, provider!!.providerState.value)
            platform.clearFailure()
            provider!!.connectSelectedDevice()
            assertEquals(2, platform.connectCount)
        }
    }

    @Test
    fun programmingExceptionsPropagateWithoutPresentationOrAttemptCancellation() {
        listOf<RuntimeException>(IllegalStateException("mapping invariant"), UnsupportedOperationException("unknown runtime"))
            .forEach { error ->
                provider?.close()
                val platform = FakePlatform()
                provider = createProvider(platform)
                selectTarget(platform)
                platform.throwDirectly = error
                val thrown = runCatching { provider!!.connectSelectedDevice() }.exceptionOrNull()

                assertSame(error, thrown)
                assertEquals(BleHeartRateProviderStateKind.CONNECTING, provider!!.providerState.value.kind)
                assertFalse(provider!!.providerState.value.kind == BleHeartRateProviderStateKind.PERMISSION_REQUIRED)
                assertFalse(provider!!.providerState.value.kind == BleHeartRateProviderStateKind.BLUETOOTH_DISABLED)
                assertEquals(0, platform.disconnectCount)
                assertEquals(0, platform.closeCount)
                platform.throwDirectly = null
                provider!!.connectSelectedDevice()
                assertEquals(2, platform.connectCount)
            }
    }

    @Test
    fun descriptorMutationIllegalStatePropagatesOutsideBoundaryAndKeepsCurrentAttempt() {
        val platform = FakePlatform().apply { descriptor.mutationFailure = IllegalStateException("descriptor property bug") }
        provider = createProvider(platform)
        selectTarget(platform)
        provider!!.connectSelectedDevice()
        platform.callback!!.onConnectionStateChange(platform.gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        idleMain()
        platform.callback!!.onServicesDiscovered(platform.gatt, BluetoothGatt.GATT_SUCCESS)

        val thrown = runCatching { idleMain() }.exceptionOrNull()

        assertTrue(thrown != null)
        assertFalse(provider!!.providerState.value.kind == BleHeartRateProviderStateKind.PERMISSION_REQUIRED)
        assertFalse(provider!!.providerState.value.kind == BleHeartRateProviderStateKind.BLUETOOTH_DISABLED)
        assertEquals(0, platform.disconnectCount)
        assertEquals(0, platform.closeCount)
    }

    @Test
    fun gattFailureAfterLiveCancelsWatchdogFreshnessAndRetryAndCanceledClosuresCannotResurrect() {
        val scheduler = FakeScheduler()
        val platform = FakePlatform()
        provider = createProvider(platform, scheduler)
        selectTarget(platform)
        provider!!.connectSelectedDevice()
        val oldCallback = platform.callback!!
        oldCallback.onConnectionStateChange(platform.gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        idleMain()
        oldCallback.onServicesDiscovered(platform.gatt, BluetoothGatt.GATT_SUCCESS)
        idleMain()
        oldCallback.onDescriptorWrite(platform.gatt, platform.descriptor, BluetoothGatt.GATT_SUCCESS)
        oldCallback.onCharacteristicChanged(platform.gatt, platform.characteristic, byteArrayOf(0, 80))
        idleMain()
        assertEquals(BleHeartRateProviderStateKind.LIVE_BPM, provider!!.providerState.value.kind)

        oldCallback.onConnectionStateChange(platform.gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED)
        idleMain()
        scheduler.runActive(HeartRateScheduledTask.RETRY)
        assertEquals(2, platform.connectCount)
        val retryCallback = platform.callback!!
        platform.fail(BlePlatformOperation.DISCOVER_SERVICES)
        retryCallback.onConnectionStateChange(platform.gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        idleMain()
        assertPermissionFailure()
        val connectsAfterFailure = platform.connectCount

        scheduler.runCanceled(HeartRateScheduledTask.WATCHDOG)
        scheduler.runCanceled(HeartRateScheduledTask.FRESHNESS)
        scheduler.runCanceled(HeartRateScheduledTask.RETRY)
        oldCallback.onServicesDiscovered(platform.gatt, BluetoothGatt.GATT_SUCCESS)
        retryCallback.onServicesDiscovered(platform.gatt, BluetoothGatt.GATT_SUCCESS)
        idleMain()

        assertEquals(connectsAfterFailure, platform.connectCount)
        assertPermissionFailure()
        assertTrue(platform.closeCount >= 2)
        platform.clearFailure()
        provider!!.refreshAvailability()
        assertEquals(connectsAfterFailure, platform.connectCount)
        provider!!.connectSelectedDevice()
        assertEquals(connectsAfterFailure + 1, platform.connectCount)
    }

    private fun assertPermissionFailure() {
        assertEquals(BleHeartRateProviderStateKind.PERMISSION_REQUIRED, provider!!.providerState.value.kind)
        assertFalse(provider!!.providerState.value.reconnectInProgress)
        assertFalse(provider!!.scanState.value.kind == BleHeartRateScanStateKind.SCANNING)
    }

    private fun selectTarget(platform: FakePlatform) {
        provider!!.listBondedDevices()
        assertTrue(provider!!.selectDevice(platform.address) != null)
    }

    private fun createProvider(platform: FakePlatform, scheduler: FakeScheduler = FakeScheduler()): AndroidBleHeartRateProvider {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        @Suppress("DEPRECATION")
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(true)
        return AndroidBleHeartRateProvider(application, platformCalls = platform, controllerSchedulerOverride = scheduler)
    }

    private fun idleMain() = shadowOf(Looper.getMainLooper()).idle()

    private class FakePlatform : DelegatingBlePlatformCallBoundary() {
        val address = "D8:F0:42:01:90:D7"
        val device: BluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
        val descriptor = FakeDescriptor()
        val characteristic = FakeCharacteristic(descriptor)
        val service = FakeService(characteristic)
        val gatt = FakeGatt(service)
        var callback: BleGattCallback? = null
        var failureOperation: BlePlatformOperation? = null
        var throwDirectly: RuntimeException? = null
        var connectCount = 0
        var disconnectCount = 0
        var closeCount = 0

        fun fail(operation: BlePlatformOperation) { failureOperation = operation }
        fun clearFailure() { failureOperation = null }
        private fun <T> result(operation: BlePlatformOperation, value: T): BlePlatformCallResult<T> =
            if (failureOperation == operation) BlePlatformCallResult.ExpectedFailure(operation, SecurityException(operation.name))
            else BlePlatformCallResult.Success(value)

        override fun readAdapterEnabled(adapter: BluetoothAdapter) = result(BlePlatformOperation.READ_ADAPTER_ENABLED, true)
        override fun readBondedDevices(adapter: BluetoothAdapter) = result(BlePlatformOperation.READ_BONDED_DEVICES, setOf(device))
        override fun readDeviceIdentifier(device: BluetoothDevice) = result(BlePlatformOperation.READ_DEVICE_IDENTIFIER, address)
        override fun readDeviceName(device: BluetoothDevice) = result(BlePlatformOperation.READ_DEVICE_NAME, "Band 9")
        override fun startScan(scanner: android.bluetooth.le.BluetoothLeScanner, filters: List<android.bluetooth.le.ScanFilter>, settings: android.bluetooth.le.ScanSettings, callback: android.bluetooth.le.ScanCallback) = result(BlePlatformOperation.START_SCAN, Unit)
        override fun connectGatt(device: BluetoothDevice, context: Context, callback: BleGattCallback): BlePlatformCallResult<BleGattConnection?> {
            connectCount++
            throwDirectly?.let { throw it }
            this.callback = callback
            return result(BlePlatformOperation.CONNECT_GATT, gatt)
        }
        override fun discoverServices(gatt: BleGattConnection) = result(BlePlatformOperation.DISCOVER_SERVICES, true)
        override fun readGattService(gatt: BleGattConnection, uuid: UUID) = result(BlePlatformOperation.READ_GATT_SERVICE, service)
        override fun readGattCharacteristic(service: BleGattService, uuid: UUID) = result(BlePlatformOperation.READ_GATT_CHARACTERISTIC, characteristic)
        override fun readGattDescriptor(characteristic: BleGattCharacteristic, uuid: UUID) = result(BlePlatformOperation.READ_GATT_DESCRIPTOR, descriptor)
        override fun configureNotification(gatt: BleGattConnection, characteristic: BleGattCharacteristic) = result(BlePlatformOperation.CONFIGURE_NOTIFICATION, true)
        override fun writeDescriptor(gatt: BleGattConnection, descriptor: BleGattDescriptor, value: ByteArray) = result(BlePlatformOperation.WRITE_DESCRIPTOR, BluetoothGatt.GATT_SUCCESS)
        override fun disconnectGatt(gatt: BleGattConnection): BlePlatformCallResult<Unit> { disconnectCount++; return result(BlePlatformOperation.DISCONNECT_GATT, Unit) }
        override fun closeGatt(gatt: BleGattConnection): BlePlatformCallResult<Unit> { closeCount++; return result(BlePlatformOperation.CLOSE_GATT, Unit) }
    }

    private class FakeGatt(private val service: BleGattService) : BleGattConnection {
        override fun discoverServices() = true
        override fun getService(uuid: UUID) = service
        override fun setCharacteristicNotification(characteristic: BleGattCharacteristic, enabled: Boolean) = true
        override fun writeDescriptor(descriptor: BleGattDescriptor, value: ByteArray) = BluetoothGatt.GATT_SUCCESS
        override fun disconnect() = Unit
        override fun close() = Unit
    }
    private class FakeService(private val characteristic: BleGattCharacteristic) : BleGattService { override fun getCharacteristic(uuid: UUID) = characteristic }
    private class FakeCharacteristic(private val descriptor: BleGattDescriptor) : BleGattCharacteristic {
        override val uuid: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        override val properties: Int = BluetoothGattCharacteristic.PROPERTY_NOTIFY
        override fun getDescriptor(uuid: UUID) = descriptor
    }
    private class FakeDescriptor : BleGattDescriptor {
        override val characteristicUuid: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        var mutationFailure: RuntimeException? = null
        override fun setLegacyValue(value: ByteArray) { mutationFailure?.let { throw it } }
    }
    private class FakeScheduler : HeartRateControllerScheduler {
        private val active = EnumMap<HeartRateScheduledTask, () -> Unit>(HeartRateScheduledTask::class.java)
        private val canceled = EnumMap<HeartRateScheduledTask, () -> Unit>(HeartRateScheduledTask::class.java)
        override fun schedule(task: HeartRateScheduledTask, delayMs: Long, action: () -> Unit) { active[task] = action }
        override fun cancel(task: HeartRateScheduledTask) { active.remove(task)?.let { canceled[task] = it } }
        override fun cancelAll() { HeartRateScheduledTask.entries.forEach(::cancel) }
        fun runActive(task: HeartRateScheduledTask) { active.remove(task)?.invoke() }
        fun runCanceled(task: HeartRateScheduledTask) { canceled.remove(task)?.invoke() }
    }
}
