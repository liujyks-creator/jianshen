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
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import java.time.Instant
import java.util.EnumMap
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class AndroidBleHeartRateProvider(
    context: Context,
    private val scanWindowMillis: Long = DEFAULT_SCAN_WINDOW_MILLIS,
    private val now: () -> Instant = { Instant.now() },
    private val onScanState: (BleHeartRateScanState) -> Unit = {},
    private val onCandidate: (BleHeartRateDeviceCandidate) -> Unit = {},
    private val onState: (BleHeartRateProviderState) -> Unit = {}
) : HeartRateProvider, AutoCloseable {
    private data class GattAttempt(
        val targetGeneration: Long,
        val attemptGeneration: Long
    )

    private val appContext = context.applicationContext
    private val bluetoothAdapter by lazy {
        appContext.getSystemService(BluetoothManager::class.java)?.adapter
    }
    private val scanner get() = bluetoothAdapter?.bluetoothLeScanner
    private val mainHandler = Handler(Looper.getMainLooper())
    private val controllerScheduler = HandlerHeartRateControllerScheduler(mainHandler)
    private val devices = linkedMapOf<String, BluetoothDevice>()
    private val mutableProviderState = MutableStateFlow(BleHeartRateProviderState.noSource())
    private val mutableScanState = MutableStateFlow(BleHeartRateScanState.idle())
    private val mutableCandidates = MutableStateFlow<List<BleHeartRateDeviceCandidate>>(emptyList())
    private val mutableHeartRateState = MutableStateFlow(mutableProviderState.value.toHeartRateState())
    private val controller = HeartRateForegroundReconnectController(
        clock = HeartRateMonotonicClock { SystemClock.elapsedRealtime() },
        scheduler = controllerScheduler,
        effectSink = ::handleControllerEffect
    )

    override val heartRateState: Flow<com.liujyks.trainflow.core.model.HeartRateState> = mutableHeartRateState
    val providerState: StateFlow<BleHeartRateProviderState> = mutableProviderState
    val scanState: StateFlow<BleHeartRateScanState> = mutableScanState
    val candidates: StateFlow<List<BleHeartRateDeviceCandidate>> = mutableCandidates

    private var currentGatt: BluetoothGatt? = null
    private var currentAttempt: GattAttempt? = null
    private val attemptGuard = HeartRateGattAttemptGuard<BluetoothGatt>()
    private var runtimeTarget: BluetoothDevice? = null
    private var selectedDevice: BleHeartRateDeviceSelection? = null
    private var lastBpm: Int? = null
    private var lastMeasuredAt: String? = null
    private var isScanning = false
    private var displayEnabled = true
    private var foregroundActive = true

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED) {
                mainHandler.post(::refreshAvailability)
            }
        }
    }

    init {
        appContext.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    private val scanTimeoutRunnable = Runnable {
        if (isScanning) {
            stopBleScan(
                BleHeartRateScanState(
                    kind = BleHeartRateScanStateKind.STOPPED,
                    message = "Scan window ended; no background scan is kept running"
                )
            )
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            mainHandler.post { handleScanResult(result) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            mainHandler.post { results.forEach(::handleScanResult) }
        }

        override fun onScanFailed(errorCode: Int) {
            mainHandler.post {
                stopBleScan(
                    BleHeartRateScanState(
                        kind = BleHeartRateScanStateKind.ERROR,
                        message = "BLE scan failed code=$errorCode",
                        recoverableReason = BleHeartRateRecoverableReason.SCAN_FAILED
                    )
                )
            }
        }
    }

    fun setDisplayEnabled(enabled: Boolean) {
        displayEnabled = enabled
        controller.setDisplayEnabled(enabled)
    }

    fun setForegroundActive(active: Boolean) {
        foregroundActive = active
        controller.setForeground(active)
        if (active) refreshAvailability()
    }

    fun refreshAvailability() {
        val availability = availabilityState()
        syncControllerAvailability(availability)
        val resolved = providerStateAfterAvailabilityRefresh(mutableProviderState.value, availability)
        if (resolved != mutableProviderState.value) publish(resolved)
    }

    fun stopScan() {
        if (isScanning) {
            stopBleScan(BleHeartRateScanState(BleHeartRateScanStateKind.STOPPED, "BLE heart-rate scan stopped"))
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val availability = availabilityState()
        syncControllerAvailability(availability)
        if (availability.kind != BleHeartRateProviderStateKind.NO_SOURCE) {
            publish(availability)
            return
        }
        stopScan()
        controller.onManualScanStarted()
        devices.clear()
        mutableCandidates.value = emptyList()
        val leScanner = scanner
        if (leScanner == null) {
            controller.onManualScanEnded()
            publishScanState(BleHeartRateScanState(BleHeartRateScanStateKind.ERROR, "Bluetooth LE scanner is unavailable", BleHeartRateRecoverableReason.SCAN_FAILED))
            return
        }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        leScanner.startScan(heartRateServiceScanFilters(), settings, scanCallback)
        isScanning = true
        mainHandler.removeCallbacks(scanTimeoutRunnable)
        mainHandler.postDelayed(scanTimeoutRunnable, scanWindowMillis)
        publishScanState(BleHeartRateScanState(BleHeartRateScanStateKind.SCANNING, "Scanning for BLE Heart Rate Service devices"))
    }

    @SuppressLint("MissingPermission")
    fun listBondedDevices() {
        val availability = availabilityState()
        syncControllerAvailability(availability)
        if (availability.kind != BleHeartRateProviderStateKind.NO_SOURCE) {
            publish(availability)
            return
        }
        bluetoothAdapter?.bondedDevices.orEmpty().forEach { addCandidate(it, null, false) }
    }

    fun selectDevice(identifier: String): BleHeartRateDeviceSelection? {
        val device = devices[identifier] ?: return null.also {
            publishError("Cannot select unknown BLE device identifier=$identifier", BleHeartRateRecoverableReason.DEVICE_NOT_FOUND)
        }
        stopScan()
        controller.onManualScanEnded()
        controller.clearTarget()
        val selection = device.toSelection()
        runtimeTarget = device
        selectedDevice = selection
        mutableCandidates.value = emptyList()
        publishScanState(BleHeartRateScanState.idle("Scan ended after device selection"))
        publish(BleHeartRateProviderState(BleHeartRateProviderStateKind.DEVICE_SELECTED, "BLE heart-rate source selected", selectedDevice = selection))
        return selection
    }

    fun clearCandidates() {
        devices.clear()
        mutableCandidates.value = emptyList()
    }

    fun connectSelectedDevice() {
        val selection = selectedDevice
        if (selection == null || runtimeTarget == null) {
            publishError("No selected BLE heart-rate runtime source is available to connect", BleHeartRateRecoverableReason.DEVICE_NOT_FOUND)
            return
        }
        val availability = availabilityState()
        syncControllerAvailability(availability)
        if (availability.kind != BleHeartRateProviderStateKind.NO_SOURCE || !displayEnabled || !foregroundActive) {
            publish(availability.copy(selectedDevice = selection))
            return
        }
        stopScan()
        controller.onManualScanEnded()
        controller.beginManualTarget()
    }

    fun disconnect() {
        controller.userStop()
    }

    fun stop() {
        stopScan()
        controller.userStop()
        controller.clearTarget()
        runtimeTarget = null
        selectedDevice = null
        lastBpm = null
        lastMeasuredAt = null
        publish(BleHeartRateProviderState(BleHeartRateProviderStateKind.STOPPED, "BLE HRS provider stopped"))
    }

    override fun close() {
        stopScan()
        controller.close()
        attemptGuard.clear()
        runtimeTarget = null
        selectedDevice = null
        mainHandler.removeCallbacks(scanTimeoutRunnable)
        controllerScheduler.cancelAll()
        runCatching { appContext.unregisterReceiver(bluetoothStateReceiver) }
    }

    private fun handleControllerEffect(effect: HeartRateReconnectEffect) {
        when (effect) {
            is HeartRateReconnectEffect.ConnectDirect -> startDirectConnection(effect)
            is HeartRateReconnectEffect.CloseAttempt -> closeAttempt(effect)
            is HeartRateReconnectEffect.StateChanged -> publishRuntimeState(effect.state)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDirectConnection(effect: HeartRateReconnectEffect.ConnectDirect) {
        val device = runtimeTarget ?: return
        val availability = availabilityState()
        if (availability.kind != BleHeartRateProviderStateKind.NO_SOURCE || !displayEnabled || !foregroundActive) {
            mainHandler.post {
                syncControllerAvailability(availability)
                publish(availability.copy(selectedDevice = selectedDevice))
            }
            return
        }
        val attempt = GattAttempt(effect.targetGeneration, effect.attemptGeneration)
        currentAttempt = attempt
        val callback = createGattCallback(attempt)
        val gatt = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(appContext, false, callback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(appContext, false, callback)
            }
        }.getOrNull()
        if (gatt == null) {
            currentAttempt = null
            mainHandler.post {
                controller.technicalFailure(
                    effect.targetGeneration,
                    effect.attemptGeneration,
                    HeartRateFreshnessReason.CONNECT_FAILED
                )
            }
            return
        }
        currentGatt = gatt
        attemptGuard.bind(gatt, attempt.targetGeneration, attempt.attemptGeneration)
    }

    @SuppressLint("MissingPermission")
    private fun createGattCallback(attempt: GattAttempt) = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            mainHandler.post { handleConnectionStateChange(gatt, attempt, status, newState) }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            mainHandler.post { handleServicesDiscovered(gatt, attempt, status) }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            mainHandler.post { handleDescriptorWrite(gatt, attempt, descriptor, status) }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            mainHandler.post { handleCharacteristicChanged(gatt, attempt, characteristic, value) }
        }

        @Deprecated("Deprecated by Android platform for API 33+")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: byteArrayOf()
            mainHandler.post { handleCharacteristicChanged(gatt, attempt, characteristic, value) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleConnectionStateChange(gatt: BluetoothGatt, attempt: GattAttempt, status: Int, newState: Int) {
        if (!isCurrent(gatt, attempt)) return closeGattInstance(gatt, disconnectFirst = false)
        if (status != BluetoothGatt.GATT_SUCCESS) {
            controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CONNECT_FAILED)
            return
        }
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            if (!gatt.discoverServices()) {
                controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED)
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            currentGatt = null
            currentAttempt = null
            attemptGuard.invalidate(gatt, attempt.targetGeneration, attempt.attemptGeneration)
            closeGattInstance(gatt, disconnectFirst = false)
            controller.disconnected(attempt.targetGeneration, attempt.attemptGeneration)
        }
    }

    private fun handleServicesDiscovered(gatt: BluetoothGatt, attempt: GattAttempt, status: Int) {
        if (!isCurrent(gatt, attempt)) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED)
            return
        }
        val service = gatt.getService(HEART_RATE_SERVICE_UUID)
        if (service == null) {
            controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED)
            return
        }
        subscribeHeartRateMeasurement(gatt, attempt, service)
    }

    private fun handleDescriptorWrite(gatt: BluetoothGatt, attempt: GattAttempt, descriptor: BluetoothGattDescriptor, status: Int) {
        if (!isCurrent(gatt, attempt) || descriptor.characteristic.uuid != HEART_RATE_MEASUREMENT_UUID) return
        if (status == BluetoothGatt.GATT_SUCCESS) {
            controller.notifyEnabled(attempt.targetGeneration, attempt.attemptGeneration)
        } else {
            controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CCCD_FAILED)
        }
    }

    private fun handleCharacteristicChanged(gatt: BluetoothGatt, attempt: GattAttempt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        if (!isCurrent(gatt, attempt) || characteristic.uuid != HEART_RATE_MEASUREMENT_UUID) return
        val measurement = HeartRateMeasurementParser.parse(value)
        if (measurement == null) {
            controller.parseFailure(attempt.targetGeneration, attempt.attemptGeneration)
            return
        }
        lastBpm = measurement.bpm
        lastMeasuredAt = now().toString()
        controller.validSample(attempt.targetGeneration, attempt.attemptGeneration, measurement.bpm)
    }

    @SuppressLint("MissingPermission")
    private fun subscribeHeartRateMeasurement(gatt: BluetoothGatt, attempt: GattAttempt, service: BluetoothGattService) {
        val measurement = service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
        if (measurement == null) return controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED)
        val canNotify = measurement.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        val canIndicate = measurement.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        if (!canNotify && !canIndicate) return controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CCCD_FAILED)
        val cccd = measurement.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            ?: return controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CCCD_FAILED)
        if (!gatt.setCharacteristicNotification(measurement, true)) {
            return controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CCCD_FAILED)
        }
        val value = if (canNotify) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(cccd, value)
        } else {
            @Suppress("DEPRECATION")
            run {
                cccd.value = value
                if (gatt.writeDescriptor(cccd)) BluetoothGatt.GATT_SUCCESS else -1
            }
        }
        if (result != BluetoothGatt.GATT_SUCCESS) {
            controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CCCD_FAILED)
        }
    }

    private fun publishRuntimeState(runtime: HeartRateReconnectRuntimeState) {
        val kind = when (runtime.kind) {
            HeartRateReconnectRuntimeKind.IDLE -> BleHeartRateProviderStateKind.NO_SOURCE
            HeartRateReconnectRuntimeKind.CONNECTING -> BleHeartRateProviderStateKind.CONNECTING
            HeartRateReconnectRuntimeKind.WAITING -> BleHeartRateProviderStateKind.CONNECTED_WAITING_FOR_DATA
            HeartRateReconnectRuntimeKind.LIVE -> BleHeartRateProviderStateKind.LIVE_BPM
            HeartRateReconnectRuntimeKind.STALE -> BleHeartRateProviderStateKind.STALE
            HeartRateReconnectRuntimeKind.OFFLINE -> BleHeartRateProviderStateKind.DISCONNECTED
            HeartRateReconnectRuntimeKind.TECHNICAL_ERROR -> BleHeartRateProviderStateKind.ERROR
            HeartRateReconnectRuntimeKind.STOPPED -> BleHeartRateProviderStateKind.STOPPED
        }
        publish(
            BleHeartRateProviderState(
                kind = kind,
                message = runtimeMessage(runtime),
                selectedDevice = selectedDevice,
                bpm = if (kind == BleHeartRateProviderStateKind.LIVE_BPM) runtime.bpm else null,
                measuredAt = if (kind == BleHeartRateProviderStateKind.LIVE_BPM) lastMeasuredAt else null,
                recoverableReason = runtime.reason.toRecoverableReason(),
                freshnessReason = runtime.reason,
                currentReconnectAttempt = runtime.currentReconnectAttempt,
                retryBudgetExhausted = runtime.retryBudgetExhausted,
                reconnectInProgress = runtime.reconnectInProgress
            )
        )
    }

    private fun runtimeMessage(runtime: HeartRateReconnectRuntimeState): String = when (runtime.kind) {
        HeartRateReconnectRuntimeKind.IDLE -> "No heart-rate runtime target"
        HeartRateReconnectRuntimeKind.CONNECTING -> if (runtime.reconnectInProgress) "Direct reconnect attempt ${runtime.currentReconnectAttempt}/3" else "Connecting BLE heart-rate source"
        HeartRateReconnectRuntimeKind.WAITING -> "Notify enabled; waiting for first valid bpm"
        HeartRateReconnectRuntimeKind.LIVE -> "Live bpm received"
        HeartRateReconnectRuntimeKind.STALE -> "Heart-rate data is stale"
        HeartRateReconnectRuntimeKind.OFFLINE -> "BLE heart-rate source disconnected"
        HeartRateReconnectRuntimeKind.TECHNICAL_ERROR -> "BLE heart-rate technical error"
        HeartRateReconnectRuntimeKind.STOPPED -> "BLE HRS provider stopped"
    }

    private fun HeartRateFreshnessReason?.toRecoverableReason(): BleHeartRateRecoverableReason? = when (this) {
        HeartRateFreshnessReason.CONNECT_FAILED -> BleHeartRateRecoverableReason.CONNECTION_FAILED
        HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED -> BleHeartRateRecoverableReason.SERVICE_MISSING
        HeartRateFreshnessReason.CCCD_FAILED -> BleHeartRateRecoverableReason.DESCRIPTOR_WRITE_FAILED
        HeartRateFreshnessReason.PARSE_FAILED -> BleHeartRateRecoverableReason.PARSE_FAILED
        HeartRateFreshnessReason.FIRST_SAMPLE_SILENCE,
        HeartRateFreshnessReason.NOTIFY_SILENCE -> BleHeartRateRecoverableReason.CONNECTION_FAILED
        else -> null
    }

    private fun syncControllerAvailability(availability: BleHeartRateProviderState) {
        controller.setDisplayEnabled(displayEnabled)
        controller.setForeground(foregroundActive)
        controller.setPermissionGranted(availability.kind != BleHeartRateProviderStateKind.PERMISSION_REQUIRED)
        controller.setBluetoothEnabled(availability.kind != BleHeartRateProviderStateKind.BLUETOOTH_DISABLED && availability.kind != BleHeartRateProviderStateKind.UNAVAILABLE)
    }

    private fun closeAttempt(effect: HeartRateReconnectEffect.CloseAttempt) {
        val active = currentAttempt
        val gatt = currentGatt
        if (active == null || gatt == null || active.targetGeneration != effect.targetGeneration || active.attemptGeneration != effect.attemptGeneration) return
        currentAttempt = null
        currentGatt = null
        attemptGuard.invalidate(gatt, active.targetGeneration, active.attemptGeneration)
        closeGattInstance(gatt, disconnectFirst = true)
    }

    @SuppressLint("MissingPermission")
    private fun closeGattInstance(gatt: BluetoothGatt, disconnectFirst: Boolean) {
        if (disconnectFirst) runCatching { gatt.disconnect() }
        runCatching { gatt.close() }
    }

    private fun isCurrent(gatt: BluetoothGatt, attempt: GattAttempt): Boolean =
        currentGatt === gatt && currentAttempt == attempt &&
            attemptGuard.isCurrent(gatt, attempt.targetGeneration, attempt.attemptGeneration) &&
            controller.currentState().targetGeneration == attempt.targetGeneration &&
            controller.currentState().attemptGeneration == attempt.attemptGeneration

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val advertisesHrs = result.scanRecord?.serviceUuids?.any { it.uuid == HEART_RATE_SERVICE_UUID } == true
        addCandidate(device, result.rssi, advertisesHrs)
    }

    @SuppressLint("MissingPermission")
    private fun addCandidate(device: BluetoothDevice, rssi: Int?, advertisesHeartRateService: Boolean) {
        devices[device.address] = device
        val candidate = BleHeartRateDeviceCandidate(device.address, device.displayName(), rssi, advertisesHeartRateService)
        mutableCandidates.value = mutableCandidates.value.filterNot { it.identifier == candidate.identifier } + candidate
        onCandidate(candidate)
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan(state: BleHeartRateScanState? = null) {
        if (isScanning) scanner?.stopScan(scanCallback)
        isScanning = false
        mainHandler.removeCallbacks(scanTimeoutRunnable)
        controller.onManualScanEnded()
        state?.let(::publishScanState)
    }

    private fun availabilityState(): BleHeartRateProviderState {
        val missing = missingPermissions()
        if (missing.isNotEmpty()) return BleHeartRateProviderState(BleHeartRateProviderStateKind.PERMISSION_REQUIRED, "Bluetooth permission is required before scanning", missingPermissions = missing)
        val adapter = bluetoothAdapter
        return when {
            adapter == null -> BleHeartRateProviderState(BleHeartRateProviderStateKind.UNAVAILABLE, "Bluetooth adapter is unavailable on this device")
            !adapter.isEnabled -> BleHeartRateProviderState(BleHeartRateProviderStateKind.BLUETOOTH_DISABLED, "Bluetooth is disabled")
            else -> BleHeartRateProviderState.noSource()
        }
    }

    private fun missingPermissions(): List<String> {
        val granted = BleHeartRatePermissionPlanner.requiredPermissions().filter { appContext.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }.toSet()
        return BleHeartRatePermissionPlanner.missingPermissions(granted)
    }

    private fun heartRateServiceScanFilters(): List<ScanFilter> = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID)).build())

    private fun publishError(message: String, reason: BleHeartRateRecoverableReason, selection: BleHeartRateDeviceSelection? = selectedDevice) {
        publish(BleHeartRateProviderState(BleHeartRateProviderStateKind.ERROR, message, selectedDevice = selection, recoverableReason = reason))
    }

    private fun publish(state: BleHeartRateProviderState) {
        mutableProviderState.value = state
        mutableHeartRateState.value = state.toHeartRateState()
        onState(state)
    }

    private fun publishScanState(state: BleHeartRateScanState) {
        mutableScanState.value = state
        onScanState(state)
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toSelection() = BleHeartRateDeviceSelection(address, displayName())

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.displayName(): String = runCatching { name }.getOrNull().takeUnless { it.isNullOrBlank() } ?: "(unknown BLE device)"

    private class HandlerHeartRateControllerScheduler(private val handler: Handler) : HeartRateControllerScheduler {
        private val callbacks = EnumMap<HeartRateScheduledTask, Runnable>(HeartRateScheduledTask::class.java)

        override fun schedule(task: HeartRateScheduledTask, delayMs: Long, action: () -> Unit) {
            cancel(task)
            lateinit var callback: Runnable
            callback = Runnable {
                if (callbacks[task] === callback) callbacks.remove(task)
                action()
            }
            callbacks[task] = callback
            handler.postDelayed(callback, delayMs)
        }

        override fun cancel(task: HeartRateScheduledTask) {
            callbacks.remove(task)?.let(handler::removeCallbacks)
        }

        override fun cancelAll() {
            HeartRateScheduledTask.entries.forEach(::cancel)
        }
    }

    private companion object {
        const val DEFAULT_SCAN_WINDOW_MILLIS = 12_000L
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
