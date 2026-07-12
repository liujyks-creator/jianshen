package com.liujyks.trainflow.core.health

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import java.util.UUID

internal enum class BlePlatformOperation {
    READ_ADAPTER_ENABLED,
    READ_SCANNER,
    START_SCAN,
    STOP_SCAN,
    READ_BONDED_DEVICES,
    READ_DEVICE_IDENTIFIER,
    READ_DEVICE_NAME,
    CONNECT_GATT,
    DISCOVER_SERVICES,
    READ_GATT_SERVICE,
    READ_GATT_CHARACTERISTIC,
    READ_GATT_DESCRIPTOR,
    CONFIGURE_NOTIFICATION,
    WRITE_DESCRIPTOR,
    DISCONNECT_GATT,
    CLOSE_GATT
}

internal sealed interface BlePlatformCallResult<out T> {
    data class Success<T>(val value: T) : BlePlatformCallResult<T>
    data class ExpectedFailure(
        val operation: BlePlatformOperation,
        val exception: RuntimeException
    ) : BlePlatformCallResult<Nothing>
}

internal interface BleGattConnection {
    fun discoverServices(): Boolean
    fun getService(uuid: UUID): BleGattService?
    fun setCharacteristicNotification(characteristic: BleGattCharacteristic, enabled: Boolean): Boolean
    fun writeDescriptor(descriptor: BleGattDescriptor, value: ByteArray): Int
    fun disconnect()
    fun close()
}

internal interface BleGattService {
    fun getCharacteristic(uuid: UUID): BleGattCharacteristic?
}

internal interface BleGattCharacteristic {
    val uuid: UUID
    val properties: Int
    fun getDescriptor(uuid: UUID): BleGattDescriptor?
}

internal interface BleGattDescriptor {
    val characteristicUuid: UUID
    fun setLegacyValue(value: ByteArray)
}

internal interface BleGattCallback {
    fun onConnectionStateChange(gatt: BleGattConnection, status: Int, newState: Int)
    fun onServicesDiscovered(gatt: BleGattConnection, status: Int)
    fun onDescriptorWrite(gatt: BleGattConnection, descriptor: BleGattDescriptor, status: Int)
    fun onCharacteristicChanged(gatt: BleGattConnection, characteristic: BleGattCharacteristic, value: ByteArray)
}

/** Typed Android BLE API seam. Production callers cannot pass arbitrary business logic lambdas. */
internal interface BlePlatformCallBoundary {
    fun readAdapterEnabled(adapter: BluetoothAdapter): BlePlatformCallResult<Boolean>
    fun readScanner(adapter: BluetoothAdapter): BlePlatformCallResult<BluetoothLeScanner?>
    fun startScan(scanner: BluetoothLeScanner, filters: List<ScanFilter>, settings: ScanSettings, callback: ScanCallback): BlePlatformCallResult<Unit>
    fun stopScan(scanner: BluetoothLeScanner, callback: ScanCallback): BlePlatformCallResult<Unit>
    fun readBondedDevices(adapter: BluetoothAdapter): BlePlatformCallResult<Set<BluetoothDevice>>
    fun readDeviceIdentifier(device: BluetoothDevice): BlePlatformCallResult<String>
    fun readDeviceName(device: BluetoothDevice): BlePlatformCallResult<String?>
    fun connectGatt(device: BluetoothDevice, context: Context, callback: BleGattCallback): BlePlatformCallResult<BleGattConnection?>
    fun discoverServices(gatt: BleGattConnection): BlePlatformCallResult<Boolean>
    fun readGattService(gatt: BleGattConnection, uuid: UUID): BlePlatformCallResult<BleGattService?>
    fun readGattCharacteristic(service: BleGattService, uuid: UUID): BlePlatformCallResult<BleGattCharacteristic?>
    fun readGattDescriptor(characteristic: BleGattCharacteristic, uuid: UUID): BlePlatformCallResult<BleGattDescriptor?>
    fun configureNotification(gatt: BleGattConnection, characteristic: BleGattCharacteristic): BlePlatformCallResult<Boolean>
    fun writeDescriptor(gatt: BleGattConnection, descriptor: BleGattDescriptor, value: ByteArray): BlePlatformCallResult<Int>
    fun disconnectGatt(gatt: BleGattConnection): BlePlatformCallResult<Unit>
    fun closeGatt(gatt: BleGattConnection): BlePlatformCallResult<Unit>
}

internal open class DelegatingBlePlatformCallBoundary(
    private val delegate: BlePlatformCallBoundary = AndroidBlePlatformCallBoundary
) : BlePlatformCallBoundary by delegate

@SuppressLint("MissingPermission")
internal object AndroidBlePlatformCallBoundary : BlePlatformCallBoundary {
    override fun readAdapterEnabled(adapter: BluetoothAdapter) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.READ_ADAPTER_ENABLED) { adapter.isEnabled }
    override fun readScanner(adapter: BluetoothAdapter) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.READ_SCANNER) { adapter.bluetoothLeScanner }
    override fun startScan(scanner: BluetoothLeScanner, filters: List<ScanFilter>, settings: ScanSettings, callback: ScanCallback) =
        BlePlatformExceptionClassifier.scannerStateRace(BlePlatformOperation.START_SCAN) { scanner.startScan(filters, settings, callback) }
    override fun stopScan(scanner: BluetoothLeScanner, callback: ScanCallback) =
        BlePlatformExceptionClassifier.scannerStateRace(BlePlatformOperation.STOP_SCAN) { scanner.stopScan(callback) }
    override fun readBondedDevices(adapter: BluetoothAdapter) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.READ_BONDED_DEVICES) { adapter.bondedDevices }
    override fun readDeviceIdentifier(device: BluetoothDevice) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.READ_DEVICE_IDENTIFIER) { device.address }
    override fun readDeviceName(device: BluetoothDevice) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.READ_DEVICE_NAME) { device.name }

    override fun connectGatt(device: BluetoothDevice, context: Context, callback: BleGattCallback): BlePlatformCallResult<BleGattConnection?> =
        BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.CONNECT_GATT) {
            lateinit var connection: AndroidBleGattConnection
            val androidCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) = callback.onConnectionStateChange(connection, status, newState)
                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) = callback.onServicesDiscovered(connection, status)
                override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) = callback.onDescriptorWrite(connection, AndroidBleGattDescriptor(descriptor), status)
                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) = callback.onCharacteristicChanged(connection, AndroidBleGattCharacteristic(characteristic), value)
                @Deprecated("Deprecated by Android platform for API 33+")
                @Suppress("DEPRECATION")
                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) =
                    callback.onCharacteristicChanged(connection, AndroidBleGattCharacteristic(characteristic), characteristic.value ?: byteArrayOf())
            }
            val androidGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, androidCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, androidCallback)
            }
            androidGatt?.let { AndroidBleGattConnection(it).also { created -> connection = created } }
        }

    override fun discoverServices(gatt: BleGattConnection) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.DISCOVER_SERVICES) { gatt.discoverServices() }
    override fun readGattService(gatt: BleGattConnection, uuid: UUID) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.READ_GATT_SERVICE) { gatt.getService(uuid) }
    override fun readGattCharacteristic(service: BleGattService, uuid: UUID) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.READ_GATT_CHARACTERISTIC) { service.getCharacteristic(uuid) }
    override fun readGattDescriptor(characteristic: BleGattCharacteristic, uuid: UUID) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.READ_GATT_DESCRIPTOR) { characteristic.getDescriptor(uuid) }
    override fun configureNotification(gatt: BleGattConnection, characteristic: BleGattCharacteristic) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.CONFIGURE_NOTIFICATION) { gatt.setCharacteristicNotification(characteristic, true) }
    override fun writeDescriptor(gatt: BleGattConnection, descriptor: BleGattDescriptor, value: ByteArray) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.WRITE_DESCRIPTOR) { gatt.writeDescriptor(descriptor, value) }
    override fun disconnectGatt(gatt: BleGattConnection) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.DISCONNECT_GATT) { gatt.disconnect() }
    override fun closeGatt(gatt: BleGattConnection) = BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.CLOSE_GATT) { gatt.close() }
}

internal object BlePlatformExceptionClassifier {
    inline fun <T> securityOnly(operation: BlePlatformOperation, apiCall: () -> T): BlePlatformCallResult<T> =
        try {
            BlePlatformCallResult.Success(apiCall())
        } catch (exception: SecurityException) {
            BlePlatformCallResult.ExpectedFailure(operation, exception)
        }

    inline fun <T> scannerStateRace(operation: BlePlatformOperation, apiCall: () -> T): BlePlatformCallResult<T> =
        try {
            securityOnly(operation, apiCall)
        } catch (exception: IllegalStateException) {
            BlePlatformCallResult.ExpectedFailure(operation, exception)
        }
}

@SuppressLint("MissingPermission")
private class AndroidBleGattConnection(private val delegate: BluetoothGatt) : BleGattConnection {
    override fun discoverServices(): Boolean = delegate.discoverServices()
    override fun getService(uuid: UUID): BleGattService? = delegate.getService(uuid)?.let(::AndroidBleGattService)
    override fun setCharacteristicNotification(characteristic: BleGattCharacteristic, enabled: Boolean): Boolean = delegate.setCharacteristicNotification((characteristic as AndroidBleGattCharacteristic).delegate, enabled)
    override fun writeDescriptor(descriptor: BleGattDescriptor, value: ByteArray): Int {
        val androidDescriptor = (descriptor as AndroidBleGattDescriptor).delegate
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) delegate.writeDescriptor(androidDescriptor, value) else {
            @Suppress("DEPRECATION")
            if (delegate.writeDescriptor(androidDescriptor)) BluetoothGatt.GATT_SUCCESS else -1
        }
    }
    override fun disconnect() = delegate.disconnect()
    override fun close() = delegate.close()
}

private class AndroidBleGattService(private val delegate: BluetoothGattService) : BleGattService {
    override fun getCharacteristic(uuid: UUID): BleGattCharacteristic? = delegate.getCharacteristic(uuid)?.let(::AndroidBleGattCharacteristic)
}

private class AndroidBleGattCharacteristic(internal val delegate: BluetoothGattCharacteristic) : BleGattCharacteristic {
    override val uuid: UUID get() = delegate.uuid
    override val properties: Int get() = delegate.properties
    override fun getDescriptor(uuid: UUID): BleGattDescriptor? = delegate.getDescriptor(uuid)?.let(::AndroidBleGattDescriptor)
}

private class AndroidBleGattDescriptor(internal val delegate: BluetoothGattDescriptor) : BleGattDescriptor {
    override val characteristicUuid: UUID get() = delegate.characteristic.uuid
    @Suppress("DEPRECATION")
    override fun setLegacyValue(value: ByteArray) { delegate.value = value }
}
