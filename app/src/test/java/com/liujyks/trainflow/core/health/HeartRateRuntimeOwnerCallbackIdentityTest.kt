package com.liujyks.trainflow.core.health

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.model.HeartRateFact
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothGatt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class HeartRateRuntimeOwnerCallbackIdentityTest {
    private lateinit var application: Application
    private lateinit var owner: HeartRateRuntimeOwner

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        shadowOf(application.getSystemService(BluetoothManager::class.java).adapter).setEnabled(true)
        owner = HeartRateRuntimeOwner(application)
    }

    @Test
    fun synchronousCallbackBeforeConnectReturnBindsTheSameRawGatt() {
        val device = scanDevice("AA:BB:CC:DD:EE:01", "Early HRS")
        val shadowDevice: ShadowBluetoothDevice = Shadow.extract(device)
        lateinit var returnedGatt: BluetoothGatt
        shadowDevice.setGattConnectionInterceptor { gatt ->
            returnedGatt = gatt
            val shadowGatt: ShadowBluetoothGatt = Shadow.extract(gatt)
            configureHrs(shadowGatt)
            shadowGatt.gattCallback.onConnectionStateChange(
                gatt,
                BluetoothGatt.GATT_SUCCESS,
                BluetoothProfile.STATE_CONNECTED
            )
            // This assertion executes inside connectGatt(), before the raw GATT is returned.
            assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)
        }

        owner.submit(HeartRateRuntimeAction.Connect(device.address))
        idleMain()

        assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)
        assertFalse(Shadow.extract<ShadowBluetoothGatt>(returnedGatt).isClosed)
    }

    @Test
    fun callbackFirstRawGattMismatchClosesReturnedGattAndKeepsCallbackGatt() {
        val device = scanDevice("AA:BB:CC:DD:EE:02", "Mismatch HRS")
        val shadowDevice = Shadow.extract<ShadowBluetoothDevice>(device)
        lateinit var callbackGatt: BluetoothGatt
        lateinit var returnedGatt: BluetoothGatt
        lateinit var callback: BluetoothGattCallback
        lateinit var characteristic: BluetoothGattCharacteristic
        shadowDevice.setGattConnectionInterceptor { gatt ->
            returnedGatt = gatt
            callback = Shadow.extract<ShadowBluetoothGatt>(gatt).gattCallback
            callbackGatt = ShadowBluetoothGatt.newInstance(device)
            val callbackShadow = Shadow.extract<ShadowBluetoothGatt>(callbackGatt)
            callbackShadow.setGattCallback(callback)
            characteristic = configureHrs(callbackShadow)
            callback.onConnectionStateChange(
                callbackGatt,
                BluetoothGatt.GATT_SUCCESS,
                BluetoothProfile.STATE_CONNECTED
            )
        }

        owner.submit(HeartRateRuntimeAction.Connect(device.address))
        idleMain()

        assertTrue(Shadow.extract<ShadowBluetoothGatt>(returnedGatt).isClosed)
        assertFalse(Shadow.extract<ShadowBluetoothGatt>(callbackGatt).isClosed)
        callback.onCharacteristicChanged(callbackGatt, characteristic, byteArrayOf(0x00, 88))
        assertEquals(HeartRateFact.LIVE, owner.heartRateState.value.fact)
        assertEquals(88, owner.heartRateState.value.bpm)
    }

    @Test
    fun returnAfterEarlyFailureFindsInvalidAttemptAndClosesReturnedGatt() {
        val device = scanDevice("AA:BB:CC:DD:EE:03", "Failed HRS")
        lateinit var returnedGatt: BluetoothGatt
        Shadow.extract<ShadowBluetoothDevice>(device).setGattConnectionInterceptor { gatt ->
            returnedGatt = gatt
            Shadow.extract<ShadowBluetoothGatt>(gatt).gattCallback.onConnectionStateChange(
                gatt,
                BluetoothGatt.GATT_FAILURE,
                BluetoothProfile.STATE_DISCONNECTED
            )
            assertEquals(HeartRateFact.TECHNICAL_FAILURE, owner.heartRateState.value.fact)
        }

        owner.submit(HeartRateRuntimeAction.Connect(device.address))
        idleMain()

        assertEquals(HeartRateFact.TECHNICAL_FAILURE, owner.heartRateState.value.fact)
        assertTrue(Shadow.extract<ShadowBluetoothGatt>(returnedGatt).isClosed)
    }

    @Test
    fun oldAttemptAndWrongRawGattCannotMutateCurrentAttempt() {
        val first = connectWaiting("AA:BB:CC:DD:EE:04", "First")
        val secondDevice = scanDevice("AA:BB:CC:DD:EE:05", "Second")
        val second = connectWaiting(secondDevice)
        val before = owner.heartRateState.value

        first.callback.onServicesDiscovered(first.gatt, BluetoothGatt.GATT_FAILURE)
        first.callback.onCharacteristicChanged(
            second.gatt,
            second.characteristic,
            byteArrayOf(0x00, 99)
        )
        val wrongGatt = ShadowBluetoothGatt.newInstance(secondDevice)
        second.callback.onConnectionStateChange(
            wrongGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        assertEquals(before, owner.heartRateState.value)
        assertFalse(Shadow.extract<ShadowBluetoothGatt>(second.gatt).isClosed)
        assertTrue(Shadow.extract<ShadowBluetoothGatt>(wrongGatt).isClosed)
    }

    @Test
    fun cleanupRejectsLateServicesDescriptorNotifyAndDisconnectCallbacks() {
        val connected = connectWaiting("AA:BB:CC:DD:EE:06", "Late callbacks")

        owner.submit(HeartRateRuntimeAction.Stop)
        idleMain()
        val terminal = owner.heartRateState.value

        connected.callback.onServicesDiscovered(connected.gatt, BluetoothGatt.GATT_SUCCESS)
        connected.callback.onDescriptorWrite(
            connected.gatt,
            connected.descriptor,
            BluetoothGatt.GATT_SUCCESS
        )
        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 101)
        )
        connected.callback.onConnectionStateChange(
            connected.gatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_DISCONNECTED
        )

        assertEquals(terminal, owner.heartRateState.value)
        assertEquals(HeartRateFact.INTENTIONAL_STOP, owner.heartRateState.value.fact)
        assertTrue(Shadow.extract<ShadowBluetoothGatt>(connected.gatt).isClosed)
    }

    private fun connectWaiting(address: String, name: String): ConnectedGatt {
        return connectWaiting(scanDevice(address, name))
    }

    private fun connectWaiting(device: BluetoothDevice): ConnectedGatt {
        lateinit var connected: ConnectedGatt
        Shadow.extract<ShadowBluetoothDevice>(device).setGattConnectionInterceptor { gatt ->
            val shadowGatt = Shadow.extract<ShadowBluetoothGatt>(gatt)
            val characteristic = configureHrs(shadowGatt)
            val callback = shadowGatt.gattCallback
            callback.onConnectionStateChange(
                gatt,
                BluetoothGatt.GATT_SUCCESS,
                BluetoothProfile.STATE_CONNECTED
            )
            connected = ConnectedGatt(
                gatt,
                callback,
                characteristic,
                requireNotNull(characteristic.getDescriptor(CCCD_UUID))
            )
        }
        owner.submit(HeartRateRuntimeAction.Connect(device.address))
        idleMain()
        assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)
        return connected
    }

    private fun scanDevice(address: String, name: String): BluetoothDevice {
        val adapter = application.getSystemService(BluetoothManager::class.java).adapter
        val device = ShadowBluetoothDevice.newInstance(address)
        Shadow.extract<ShadowBluetoothDevice>(device).setName(name)
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val callback = shadowOf(adapter.bluetoothLeScanner).scanCallbacks.single()
        @Suppress("DEPRECATION")
        callback.onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            ScanResult(device, null, -45, 1L)
        )
        idleMain()
        return device
    }

    private fun configureHrs(shadowGatt: ShadowBluetoothGatt): BluetoothGattCharacteristic {
        val service = BluetoothGattService(HRS_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            MEASUREMENT_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        characteristic.addDescriptor(
            BluetoothGattDescriptor(CCCD_UUID, BluetoothGattDescriptor.PERMISSION_WRITE)
        )
        service.addCharacteristic(characteristic)
        shadowGatt.addDiscoverableService(service)
        shadowGatt.allowCharacteristicNotification(characteristic)
        return characteristic
    }

    private fun idleMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private data class ConnectedGatt(
        val gatt: BluetoothGatt,
        val callback: BluetoothGattCallback,
        val characteristic: BluetoothGattCharacteristic,
        val descriptor: BluetoothGattDescriptor
    )

    private companion object {
        val HRS_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
