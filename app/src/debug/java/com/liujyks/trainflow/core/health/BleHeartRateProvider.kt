package com.liujyks.trainflow.core.health

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateUnavailableReason
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class BleHeartRateProvider(
    context: Context,
    private val onStatus: (BleHeartRateAdapterStatus) -> Unit = {}
) : HeartRateProvider {
    private val appContext = context.applicationContext
    private val bluetoothAdapter by lazy {
        appContext.getSystemService(BluetoothManager::class.java).adapter
    }
    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private val devices = linkedMapOf<String, BluetoothDevice>()
    private val seenDeviceLabels = linkedMapOf<String, String>()
    private val mutableHeartRateState = MutableStateFlow(
        HeartRateState(
            kind = HeartRateStateKind.UNAVAILABLE,
            sourceKind = HeartRateSourceKind.NONE,
            unavailableReason = HeartRateUnavailableReason.NOT_CONFIGURED,
            message = "Debug BLE HRS adapter is idle"
        )
    )
    private val mutableAdapterStatus = MutableStateFlow(
        BleHeartRateAdapterStatus(
            kind = BleHeartRateAdapterStatusKind.STOPPED,
            message = "adapter stopped"
        )
    )

    override val heartRateState: StateFlow<HeartRateState> = mutableHeartRateState
    val adapterStatus: StateFlow<BleHeartRateAdapterStatus> = mutableAdapterStatus

    private var currentGatt: BluetoothGatt? = null
    private var currentGattLabel: String? = null
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.ERROR,
                    message = "scan failed code=$errorCode"
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val source = gatt.device?.debugLabel() ?: currentGattLabel
            publish(
                BleHeartRateAdapterStatus(
                    kind = if (newState == BluetoothProfile.STATE_CONNECTED) {
                        BleHeartRateAdapterStatusKind.CONNECTING
                    } else {
                        BleHeartRateAdapterStatusKind.DISCONNECTED
                    },
                    message = "GATT connection status=$status state=$newState",
                    address = gatt.device?.address,
                    deviceLabel = source
                )
            )

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mutableHeartRateState.value = HeartRateState(
                    kind = HeartRateStateKind.DEVICE_CONNECTED_NO_READING,
                    sourceKind = HeartRateSourceKind.DEVICE,
                    sourceId = gatt.device?.address,
                    sourceLabel = source,
                    message = "Debug BLE HRS connected; waiting for measurement"
                )
                publish(
                    BleHeartRateAdapterStatus(
                        kind = BleHeartRateAdapterStatusKind.CONNECTING,
                        message = "GATT connected; discovering services",
                        address = gatt.device?.address,
                        deviceLabel = source
                    )
                )
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                publish(
                    BleHeartRateAdapterStatus(
                        kind = BleHeartRateAdapterStatusKind.DISCONNECTED,
                        message = "GATT disconnected",
                        address = gatt.device?.address,
                        deviceLabel = source
                    )
                )
                gatt.close()
                if (currentGatt == gatt) {
                    currentGatt = null
                    currentGattLabel = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val source = gatt.device?.debugLabel() ?: currentGattLabel
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.SERVICE_DISCOVERED,
                    message = "Services discovered status=$status count=${gatt.services.size}",
                    address = gatt.device?.address,
                    deviceLabel = source
                )
            )
            gatt.services.forEach { service ->
                publish(
                    BleHeartRateAdapterStatus(
                        kind = BleHeartRateAdapterStatusKind.SERVICE_DISCOVERED,
                        message = "service ${service.uuid.shortLabel()}",
                        address = gatt.device?.address,
                        deviceLabel = source
                    )
                )
                service.characteristics.forEach { characteristic ->
                    publish(
                        BleHeartRateAdapterStatus(
                            kind = BleHeartRateAdapterStatusKind.SERVICE_DISCOVERED,
                            message = "  characteristic ${characteristic.uuid.shortLabel()} " +
                                "props=${characteristic.properties.propertyLabel()}",
                            address = gatt.device?.address,
                            deviceLabel = source
                        )
                    )
                }
            }

            val heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID)
            if (heartRateService == null) {
                publish(
                    BleHeartRateAdapterStatus(
                        kind = BleHeartRateAdapterStatusKind.ERROR,
                        message = "RESULT: HRS 0x180D not found",
                        address = gatt.device?.address,
                        deviceLabel = source
                    )
                )
                return
            }

            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.SERVICE_DISCOVERED,
                    message = "RESULT: HRS 0x180D found",
                    address = gatt.device?.address,
                    deviceLabel = source
                )
            )
            subscribeHeartRateMeasurement(gatt, heartRateService)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val source = gatt.device?.debugLabel() ?: currentGattLabel
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.NOTIFY_ENABLED,
                    message = "Descriptor write ${descriptor.uuid.shortLabel()} status=$status " +
                        "for ${descriptor.characteristic.uuid.shortLabel()}",
                    address = gatt.device?.address,
                    deviceLabel = source
                )
            )
            if (descriptor.characteristic.uuid == HEART_RATE_MEASUREMENT_UUID &&
                status == BluetoothGatt.GATT_SUCCESS
            ) {
                publish(
                    BleHeartRateAdapterStatus(
                        kind = BleHeartRateAdapterStatusKind.NOTIFY_ENABLED,
                        message = "RESULT: 0x2A37 notify enabled",
                        address = gatt.device?.address,
                        deviceLabel = source
                    )
                )
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicChanged(gatt, characteristic, value)
        }

        @Deprecated("Deprecated by Android platform for API 33+")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            handleCharacteristicChanged(gatt, characteristic, characteristic.value ?: byteArrayOf())
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter
        val leScanner = scanner
        if (adapter == null || leScanner == null) {
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.ERROR,
                    message = "Bluetooth LE scanner unavailable on this device"
                )
            )
            return
        }
        if (!adapter.isEnabled) {
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.ERROR,
                    message = "Bluetooth adapter disabled"
                )
            )
            return
        }

        stopScan()
        clearCandidates()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        leScanner.startScan(null, settings, scanCallback)
        isScanning = true
        publish(
            BleHeartRateAdapterStatus(
                kind = BleHeartRateAdapterStatusKind.SCANNING,
                message = "Scan started: all BLE advertisements, low latency"
            )
        )
        publish(
            BleHeartRateAdapterStatus(
                kind = BleHeartRateAdapterStatusKind.SCANNING,
                message = "Expected positive path: device -> service 0x180D -> characteristic 0x2A37 notify"
            )
        )
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isScanning) {
            scanner?.stopScan(scanCallback)
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.STOPPED,
                    message = "Scan stopped"
                )
            )
        }
        isScanning = false
    }

    @SuppressLint("MissingPermission")
    fun listBondedDevices() {
        val bonded = bluetoothAdapter?.bondedDevices.orEmpty()
        publish(
            BleHeartRateAdapterStatus(
                kind = BleHeartRateAdapterStatusKind.DEVICE_FOUND,
                message = "Bonded devices count=${bonded.size}"
            )
        )
        bonded.forEach { addDevice(it, "bonded") }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        val device = devices[address]
        if (device == null) {
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.ERROR,
                    message = "Cannot connect unknown device address=$address",
                    address = address
                )
            )
            return
        }

        closeGatt()
        currentGattLabel = device.debugLabel()
        publish(
            BleHeartRateAdapterStatus(
                kind = BleHeartRateAdapterStatusKind.CONNECTING,
                message = "Connecting $currentGattLabel",
                address = device.address,
                deviceLabel = currentGattLabel
            )
        )
        currentGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(appContext, false, gattCallback)
        }
    }

    fun clearCandidates() {
        devices.clear()
        seenDeviceLabels.clear()
    }

    fun stop() {
        stopScan()
        closeGatt()
        mutableHeartRateState.value = HeartRateState(
            kind = HeartRateStateKind.UNAVAILABLE,
            sourceKind = HeartRateSourceKind.NONE,
            unavailableReason = HeartRateUnavailableReason.DISABLED_BY_USER,
            message = "Debug BLE HRS adapter stopped"
        )
        publish(
            BleHeartRateAdapterStatus(
                kind = BleHeartRateAdapterStatusKind.STOPPED,
                message = "adapter stopped"
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val serviceLabels = result.scanRecord?.serviceUuids
            ?.joinToString(prefix = "[", postfix = "]") { it.uuid.shortLabel() }
            ?: "[]"
        val name = device.safeName()
        val marker = when {
            result.scanRecord?.serviceUuids?.any { it.uuid == HEART_RATE_SERVICE_UUID } == true -> "HRS_ADV"
            name.contains("huawei", ignoreCase = true) -> "HUAWEI_NAME"
            name.contains("band", ignoreCase = true) -> "BAND_NAME"
            else -> "seen"
        }
        val label = "$marker rssi=${result.rssi} name=$name address=${device.address} services=$serviceLabels"
        val oldLabel = seenDeviceLabels[device.address]
        if (oldLabel != label) {
            seenDeviceLabels[device.address] = label
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.DEVICE_FOUND,
                    message = "scan $label",
                    address = device.address,
                    deviceLabel = device.debugLabel(),
                    candidateLabel = label
                )
            )
        }
        addDevice(device, label)
    }

    @SuppressLint("MissingPermission")
    private fun addDevice(device: BluetoothDevice, label: String) {
        if (devices.containsKey(device.address)) return
        devices[device.address] = device
        publish(
            BleHeartRateAdapterStatus(
                kind = BleHeartRateAdapterStatusKind.DEVICE_FOUND,
                message = "device found ${device.safeName()} ${device.address}",
                address = device.address,
                deviceLabel = device.debugLabel(),
                candidateLabel = label
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun subscribeHeartRateMeasurement(
        gatt: BluetoothGatt,
        heartRateService: BluetoothGattService
    ) {
        val source = gatt.device?.debugLabel() ?: currentGattLabel
        val measurement = heartRateService.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
        if (measurement == null) {
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.ERROR,
                    message = "RESULT: characteristic 0x2A37 not found",
                    address = gatt.device?.address,
                    deviceLabel = source
                )
            )
            return
        }

        val properties = measurement.properties
        val canNotify = properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        val canIndicate = properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        publish(
            BleHeartRateAdapterStatus(
                kind = BleHeartRateAdapterStatusKind.CHARACTERISTIC_FOUND,
                message = "RESULT: characteristic 0x2A37 found props=${properties.propertyLabel()}",
                address = gatt.device?.address,
                deviceLabel = source
            )
        )
        if (!canNotify && !canIndicate) {
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.ERROR,
                    message = "RESULT: 0x2A37 has no notify/indicate property",
                    address = gatt.device?.address,
                    deviceLabel = source
                )
            )
            return
        }

        val cccd = measurement.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (cccd == null) {
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.ERROR,
                    message = "RESULT: 0x2A37 CCCD 0x2902 not found",
                    address = gatt.device?.address,
                    deviceLabel = source
                )
            )
            return
        }

        val notificationSet = gatt.setCharacteristicNotification(measurement, true)
        publish(
            BleHeartRateAdapterStatus(
                kind = BleHeartRateAdapterStatusKind.CHARACTERISTIC_FOUND,
                message = "setCharacteristicNotification=$notificationSet",
                address = gatt.device?.address,
                deviceLabel = source
            )
        )
        val value = if (canNotify) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }
        val writeResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(cccd, value)
        } else {
            @Suppress("DEPRECATION")
            run {
                cccd.value = value
                if (gatt.writeDescriptor(cccd)) BluetoothGatt.GATT_SUCCESS else -1
            }
        }
        publish(
            BleHeartRateAdapterStatus(
                kind = BleHeartRateAdapterStatusKind.CHARACTERISTIC_FOUND,
                message = "write CCCD result=$writeResult",
                address = gatt.device?.address,
                deviceLabel = source
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun handleCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        if (characteristic.uuid != HEART_RATE_MEASUREMENT_UUID) return
        val measurement = HeartRateMeasurementParser.parse(value)
        val hex = value.joinToString(" ") { "%02X".format(it) }
        val source = gatt.device?.debugLabel() ?: currentGattLabel ?: "(unknown)"
        if (measurement == null) {
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.ERROR,
                    message = "RESULT: heart-rate notify unreadable source=$source bytes=$hex",
                    address = gatt.device?.address,
                    deviceLabel = source
                )
            )
        } else {
            mutableHeartRateState.value = HeartRateState(
                kind = HeartRateStateKind.DEVICE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = measurement.bpm,
                measuredAt = Instant.now().toString(),
                sourceId = gatt.device?.address,
                sourceLabel = source
            )
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.BPM_RECEIVED,
                    message = "RESULT: heart-rate notify bpm=${measurement.bpm} " +
                        "flags=0x${measurement.flags.raw.toString(16).uppercase(Locale.US)} " +
                        "format=${measurement.flags.bpmFormat.name.lowercase(Locale.US)} " +
                        "source=$source bytes=$hex",
                    address = gatt.device?.address,
                    deviceLabel = source,
                    bpm = measurement.bpm
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        currentGatt?.let {
            publish(
                BleHeartRateAdapterStatus(
                    kind = BleHeartRateAdapterStatusKind.STOPPED,
                    message = "Closing GATT",
                    address = it.device?.address,
                    deviceLabel = it.device?.debugLabel()
                )
            )
            it.disconnect()
            it.close()
        }
        currentGatt = null
        currentGattLabel = null
    }

    private fun publish(status: BleHeartRateAdapterStatus) {
        mutableAdapterStatus.value = status
        onStatus(status)
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeName(): String {
        return runCatching { name }.getOrNull().takeUnless { it.isNullOrBlank() } ?: "(unknown)"
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.debugLabel(): String {
        return "${safeName()} $address"
    }

    private fun UUID.shortLabel(): String {
        val text = toString().lowercase(Locale.US)
        return when {
            text.startsWith("0000") && text.endsWith("-0000-1000-8000-00805f9b34fb") ->
                "0x${text.substring(4, 8).uppercase(Locale.US)}"
            else -> text
        }
    }

    private fun Int.propertyLabel(): String {
        val labels = mutableListOf<String>()
        if (this and BluetoothGattCharacteristic.PROPERTY_READ != 0) labels += "read"
        if (this and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) labels += "write"
        if (this and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) labels += "write_no_response"
        if (this and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) labels += "notify"
        if (this and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) labels += "indicate"
        return labels.takeIf { it.isNotEmpty() }?.joinToString("|") ?: "none"
    }

    private companion object {
        private val HEART_RATE_SERVICE_UUID: UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val HEART_RATE_MEASUREMENT_UUID: UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}

internal data class BleHeartRateAdapterStatus(
    val kind: BleHeartRateAdapterStatusKind,
    val message: String,
    val address: String? = null,
    val deviceLabel: String? = null,
    val candidateLabel: String? = null,
    val bpm: Int? = null
)

internal enum class BleHeartRateAdapterStatusKind {
    SCANNING,
    DEVICE_FOUND,
    CONNECTING,
    SERVICE_DISCOVERED,
    CHARACTERISTIC_FOUND,
    NOTIFY_ENABLED,
    BPM_RECEIVED,
    DISCONNECTED,
    STOPPED,
    ERROR
}
