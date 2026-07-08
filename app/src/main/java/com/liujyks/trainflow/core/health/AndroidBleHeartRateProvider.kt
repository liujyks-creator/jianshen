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
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class AndroidBleHeartRateProvider(
    context: Context,
    private val scanWindowMillis: Long = DEFAULT_SCAN_WINDOW_MILLIS,
    private val now: () -> Instant = { Instant.now() },
    private val onState: (BleHeartRateProviderState) -> Unit = {}
) : HeartRateProvider, AutoCloseable {
    private val appContext = context.applicationContext
    private val bluetoothAdapter by lazy {
        appContext.getSystemService(BluetoothManager::class.java)?.adapter
    }
    private val scanner get() = bluetoothAdapter?.bluetoothLeScanner
    private val mainHandler = Handler(Looper.getMainLooper())
    private val devices = linkedMapOf<String, BluetoothDevice>()
    private val mutableProviderState = MutableStateFlow(BleHeartRateProviderState.noSource())
    private val mutableCandidates = MutableStateFlow<List<BleHeartRateDeviceCandidate>>(emptyList())
    private val mutableHeartRateState = MutableStateFlow(mutableProviderState.value.toHeartRateState())

    override val heartRateState: Flow<com.liujyks.trainflow.core.model.HeartRateState> =
        mutableHeartRateState
    val providerState: StateFlow<BleHeartRateProviderState> = mutableProviderState
    val candidates: StateFlow<List<BleHeartRateDeviceCandidate>> = mutableCandidates

    private var currentGatt: BluetoothGatt? = null
    private var selectedDevice: BleHeartRateDeviceSelection? = null
    private var lastBpm: Int? = null
    private var lastMeasuredAt: String? = null
    private var isScanning = false

    private val scanTimeoutRunnable = Runnable {
        if (isScanning) {
            stopScan(
                BleHeartRateProviderState(
                    kind = BleHeartRateProviderStateKind.STOPPED,
                    message = "Scan window ended; no background scan is kept running",
                    selectedDevice = selectedDevice
                )
            )
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            stopScan()
            publishError(
                message = "BLE scan failed code=$errorCode",
                reason = BleHeartRateRecoverableReason.SCAN_FAILED
            )
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val selection = gatt.device?.toSelection() ?: selectedDevice
            if (status != BluetoothGatt.GATT_SUCCESS) {
                closeGatt(gatt)
                publishError(
                    message = "GATT connection failed status=$status state=$newState",
                    reason = BleHeartRateRecoverableReason.CONNECTION_FAILED,
                    selection = selection
                )
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                selectedDevice = selection
                publish(
                    BleHeartRateProviderState(
                        kind = BleHeartRateProviderStateKind.CONNECTED_WAITING_FOR_DATA,
                        message = "Connected; discovering Heart Rate Service",
                        selectedDevice = selection
                    )
                )
                if (!gatt.discoverServices()) {
                    closeGatt(gatt)
                    publishError(
                        message = "Service discovery could not be started",
                        reason = BleHeartRateRecoverableReason.CONNECTION_FAILED,
                        selection = selection
                    )
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                closeGatt(gatt)
                publish(
                    BleHeartRateProviderState(
                        kind = BleHeartRateProviderStateKind.DISCONNECTED,
                        message = "Device disconnected; user can scan or reconnect",
                        selectedDevice = selection,
                        bpm = lastBpm,
                        measuredAt = lastMeasuredAt
                    )
                )
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val selection = gatt.device?.toSelection() ?: selectedDevice
            if (status != BluetoothGatt.GATT_SUCCESS) {
                closeGatt(gatt)
                publishError(
                    message = "Service discovery failed status=$status",
                    reason = BleHeartRateRecoverableReason.CONNECTION_FAILED,
                    selection = selection
                )
                return
            }

            val heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID)
            if (heartRateService == null) {
                closeGatt(gatt)
                publishError(
                    message = "Heart Rate Service 0x180D not found",
                    reason = BleHeartRateRecoverableReason.SERVICE_MISSING,
                    selection = selection
                )
                return
            }
            subscribeHeartRateMeasurement(gatt, heartRateService)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.characteristic.uuid != HEART_RATE_MEASUREMENT_UUID) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                publish(
                    BleHeartRateProviderState(
                        kind = BleHeartRateProviderStateKind.CONNECTED_WAITING_FOR_DATA,
                        message = "Heart Rate Measurement notify enabled; waiting for bpm",
                        selectedDevice = gatt.device?.toSelection() ?: selectedDevice
                    )
                )
            } else {
                closeGatt(gatt)
                publishError(
                    message = "CCCD write failed status=$status",
                    reason = BleHeartRateRecoverableReason.DESCRIPTOR_WRITE_FAILED,
                    selection = gatt.device?.toSelection() ?: selectedDevice
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

    fun refreshAvailability() {
        publish(availabilityState())
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val availability = availabilityState()
        if (availability.kind != BleHeartRateProviderStateKind.NO_SOURCE) {
            publish(availability)
            return
        }

        stopScan()
        devices.clear()
        mutableCandidates.value = emptyList()
        val adapter = bluetoothAdapter
        val leScanner = scanner
        if (adapter == null || leScanner == null) {
            publishError(
                message = "Bluetooth LE scanner is unavailable on this device",
                reason = BleHeartRateRecoverableReason.SCAN_FAILED
            )
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        leScanner.startScan(heartRateServiceScanFilters(), settings, scanCallback)
        isScanning = true
        mainHandler.removeCallbacks(scanTimeoutRunnable)
        mainHandler.postDelayed(scanTimeoutRunnable, scanWindowMillis)
        publish(
            BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.SCANNING,
                message = "Scanning for BLE Heart Rate Service devices"
            )
        )
    }

    @SuppressLint("MissingPermission")
    fun listBondedDevices() {
        val availability = availabilityState()
        if (availability.kind != BleHeartRateProviderStateKind.NO_SOURCE) {
            publish(availability)
            return
        }
        bluetoothAdapter?.bondedDevices.orEmpty().forEach { device ->
            addCandidate(device, rssi = null, advertisesHeartRateService = false)
        }
    }

    fun selectDevice(identifier: String): BleHeartRateDeviceSelection? {
        val device = devices[identifier] ?: return null.also {
            publishError(
                message = "Cannot select unknown BLE device identifier=$identifier",
                reason = BleHeartRateRecoverableReason.DEVICE_NOT_FOUND
            )
        }
        val selection = device.toSelection()
        selectedDevice = selection
        publish(
            BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.DEVICE_SELECTED,
                message = "BLE heart-rate source selected",
                selectedDevice = selection
            )
        )
        return selection
    }

    fun clearCandidates() {
        devices.clear()
        mutableCandidates.value = emptyList()
    }

    @SuppressLint("MissingPermission")
    fun connectSelectedDevice() {
        val selection = selectedDevice
        val device = selection?.let { devices[it.identifier] }
        if (selection == null || device == null) {
            publishError(
                message = "No selected BLE heart-rate source is available to connect",
                reason = BleHeartRateRecoverableReason.DEVICE_NOT_FOUND
            )
            return
        }

        val availability = availabilityState()
        if (availability.kind != BleHeartRateProviderStateKind.NO_SOURCE) {
            publish(availability.copy(selectedDevice = selection))
            return
        }

        stopScan()
        closeGatt()
        publish(
            BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.CONNECTING,
                message = "Connecting BLE heart-rate source",
                selectedDevice = selection
            )
        )
        currentGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(appContext, false, gattCallback)
        }
    }

    fun disconnect() {
        closeGatt()
        publish(
            BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.DISCONNECTED,
                message = "Disconnected by user",
                selectedDevice = selectedDevice,
                bpm = lastBpm,
                measuredAt = lastMeasuredAt
            )
        )
    }

    fun stop() {
        stopScan()
        closeGatt()
        selectedDevice = null
        lastBpm = null
        lastMeasuredAt = null
        publish(
            BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.STOPPED,
                message = "BLE HRS provider stopped"
            )
        )
    }

    override fun close() {
        stop()
        mainHandler.removeCallbacks(scanTimeoutRunnable)
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val advertisesHrs = result.scanRecord?.serviceUuids
            ?.any { it.uuid == HEART_RATE_SERVICE_UUID } == true
        addCandidate(device, result.rssi, advertisesHrs)
    }

    @SuppressLint("MissingPermission")
    private fun addCandidate(
        device: BluetoothDevice,
        rssi: Int?,
        advertisesHeartRateService: Boolean
    ) {
        devices[device.address] = device
        val candidate = BleHeartRateDeviceCandidate(
            identifier = device.address,
            displayName = device.displayName(),
            rssi = rssi,
            advertisesHeartRateService = advertisesHeartRateService
        )
        val updated = mutableCandidates.value
            .filterNot { it.identifier == candidate.identifier } + candidate
        mutableCandidates.value = updated
        publish(
            BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.DEVICE_FOUND,
                message = "BLE device found",
                candidate = candidate
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun subscribeHeartRateMeasurement(
        gatt: BluetoothGatt,
        heartRateService: BluetoothGattService
    ) {
        val selection = gatt.device?.toSelection() ?: selectedDevice
        val measurement = heartRateService.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
        if (measurement == null) {
            closeGatt(gatt)
            publishError(
                message = "Heart Rate Measurement 0x2A37 not found",
                reason = BleHeartRateRecoverableReason.CHARACTERISTIC_MISSING,
                selection = selection
            )
            return
        }

        val canNotify = measurement.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        val canIndicate = measurement.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        if (!canNotify && !canIndicate) {
            closeGatt(gatt)
            publishError(
                message = "Heart Rate Measurement does not support notify or indicate",
                reason = BleHeartRateRecoverableReason.NOTIFY_UNAVAILABLE,
                selection = selection
            )
            return
        }

        val cccd = measurement.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (cccd == null) {
            closeGatt(gatt)
            publishError(
                message = "Heart Rate Measurement CCCD 0x2902 not found",
                reason = BleHeartRateRecoverableReason.DESCRIPTOR_MISSING,
                selection = selection
            )
            return
        }

        if (!gatt.setCharacteristicNotification(measurement, true)) {
            closeGatt(gatt)
            publishError(
                message = "setCharacteristicNotification returned false",
                reason = BleHeartRateRecoverableReason.NOTIFY_UNAVAILABLE,
                selection = selection
            )
            return
        }

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
        if (writeResult != BluetoothGatt.GATT_SUCCESS) {
            closeGatt(gatt)
            publishError(
                message = "CCCD write could not be started result=$writeResult",
                reason = BleHeartRateRecoverableReason.DESCRIPTOR_WRITE_FAILED,
                selection = selection
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        if (characteristic.uuid != HEART_RATE_MEASUREMENT_UUID) return
        val measurement = HeartRateMeasurementParser.parse(value)
        val selection = gatt.device?.toSelection() ?: selectedDevice
        if (measurement == null) {
            publishError(
                message = "Heart Rate Measurement payload could not be parsed",
                reason = BleHeartRateRecoverableReason.PARSE_FAILED,
                selection = selection
            )
            return
        }
        lastBpm = measurement.bpm
        lastMeasuredAt = now().toString()
        publish(
            BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.LIVE_BPM,
                message = "Live bpm received",
                selectedDevice = selection,
                bpm = measurement.bpm,
                measuredAt = lastMeasuredAt
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun stopScan(
        state: BleHeartRateProviderState? = null
    ) {
        if (isScanning) {
            scanner?.stopScan(scanCallback)
        }
        isScanning = false
        mainHandler.removeCallbacks(scanTimeoutRunnable)
        state?.let(::publish)
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt(gatt: BluetoothGatt? = currentGatt) {
        if (gatt == null) return
        runCatching { gatt.disconnect() }
        runCatching { gatt.close() }
        if (currentGatt == gatt) {
            currentGatt = null
        }
    }

    private fun availabilityState(): BleHeartRateProviderState {
        val missingPermissions = missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            return BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.PERMISSION_REQUIRED,
                message = "Bluetooth permission is required before scanning",
                missingPermissions = missingPermissions
            )
        }
        val adapter = bluetoothAdapter
        return when {
            adapter == null -> BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.UNAVAILABLE,
                message = "Bluetooth adapter is unavailable on this device"
            )

            !adapter.isEnabled -> BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.BLUETOOTH_DISABLED,
                message = "Bluetooth is disabled"
            )

            else -> BleHeartRateProviderState.noSource()
        }
    }

    private fun missingPermissions(): List<String> {
        val granted = BleHeartRatePermissionPlanner.requiredPermissions().filter {
            appContext.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }.toSet()
        return BleHeartRatePermissionPlanner.missingPermissions(granted)
    }

    private fun heartRateServiceScanFilters(): List<ScanFilter> {
        return listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID))
                .build()
        )
    }

    private fun publishError(
        message: String,
        reason: BleHeartRateRecoverableReason,
        selection: BleHeartRateDeviceSelection? = selectedDevice
    ) {
        publish(
            BleHeartRateProviderState(
                kind = BleHeartRateProviderStateKind.ERROR,
                message = message,
                selectedDevice = selection,
                bpm = lastBpm,
                measuredAt = lastMeasuredAt,
                recoverableReason = reason
            )
        )
    }

    private fun publish(state: BleHeartRateProviderState) {
        mutableProviderState.value = state
        mutableHeartRateState.value = state.toHeartRateState()
        onState(state)
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toSelection(): BleHeartRateDeviceSelection {
        return BleHeartRateDeviceSelection(
            identifier = address,
            displayName = displayName()
        )
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.displayName(): String {
        return runCatching { name }.getOrNull()
            .takeUnless { it.isNullOrBlank() }
            ?: "(unknown BLE device)"
    }

    private companion object {
        const val DEFAULT_SCAN_WINDOW_MILLIS = 12_000L
        private val HEART_RATE_SERVICE_UUID: UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val HEART_RATE_MEASUREMENT_UUID: UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
