package com.liujyks.trainflow.core.health

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
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
    private val onState: (BleHeartRateProviderState) -> Unit = {},
    private val platformCalls: BlePlatformCallBoundary = AndroidBlePlatformCallBoundary,
    controllerSchedulerOverride: HeartRateControllerScheduler? = null,
    monotonicClock: HeartRateMonotonicClock = HeartRateMonotonicClock { SystemClock.elapsedRealtime() }
) : HeartRateProvider, AutoCloseable {
    private data class GattAttempt(
        val targetGeneration: Long,
        val attemptGeneration: Long
    )

    private val appContext = context.applicationContext
    private val bluetoothAdapter by lazy {
        appContext.getSystemService(BluetoothManager::class.java)?.adapter
    }
    private val scanner get() = bluetoothAdapter?.let { adapter -> platformValue(platformCalls.readScanner(adapter)) }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val callbackGate = HeartRateProviderCallbackGate()
    private val controllerScheduler = controllerSchedulerOverride ?: HandlerHeartRateControllerScheduler(mainHandler)
    private val devices = linkedMapOf<String, BluetoothDevice>()
    private val mutableProviderState = MutableStateFlow(BleHeartRateProviderState.noSource())
    private val mutableScanState = MutableStateFlow(BleHeartRateScanState.idle())
    private val mutableCandidates = MutableStateFlow<List<BleHeartRateDeviceCandidate>>(emptyList())
    private val mutableHeartRateState = MutableStateFlow(mutableProviderState.value.toHeartRateState())
    private val controller = HeartRateForegroundReconnectController(
        clock = monotonicClock,
        scheduler = controllerScheduler,
        effectSink = ::handleControllerEffect
    )

    override val heartRateState: Flow<com.liujyks.trainflow.core.model.HeartRateState> = mutableHeartRateState
    val providerState: StateFlow<BleHeartRateProviderState> = mutableProviderState
    val scanState: StateFlow<BleHeartRateScanState> = mutableScanState
    val candidates: StateFlow<List<BleHeartRateDeviceCandidate>> = mutableCandidates

    private var currentGatt: BleGattConnection? = null
    private var currentAttempt: GattAttempt? = null
    private val attemptGuard = HeartRateGattAttemptGuard<BleGattConnection>()
    private var runtimeTarget: BluetoothDevice? = null
    private var selectedDevice: BleHeartRateDeviceSelection? = null
    private var lastBpm: Int? = null
    private var lastMeasuredAt: String? = null
    private var isScanning = false
    private var displayEnabled = true
    private var foregroundActive = true
    private var activeScanCallback: ScanCallback? = null
    private var activeScanTimeout: Runnable? = null
    private var lastPlatformFailure: BlePlatformCallResult.ExpectedFailure? = null

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED) {
                val token = callbackGate.lifecycleToken()
                mainHandler.post { if (callbackGate.accepts(token)) refreshAvailability() }
            }
        }
    }

    init {
        appContext.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    fun setDisplayEnabled(enabled: Boolean) {
        if (!callbackGate.isOpen()) return
        displayEnabled = enabled
        if (!enabled) {
            callbackGate.invalidateLifecycle()
            stopScanForCancellation("BLE heart-rate scan stopped because heart-rate display was disabled")
        }
        controller.setDisplayEnabled(enabled)
    }

    fun setForegroundActive(active: Boolean) {
        if (!callbackGate.isOpen()) return
        foregroundActive = active
        if (!active) {
            callbackGate.invalidateLifecycle()
            stopScanForCancellation("BLE heart-rate scan stopped when the app left foreground")
        }
        controller.setForeground(active)
        if (active) refreshAvailability()
    }

    fun refreshAvailability() {
        if (!callbackGate.isOpen()) return
        val availability = availabilityState()
        if (availability.kind != BleHeartRateProviderStateKind.NO_SOURCE) {
            callbackGate.invalidateLifecycle()
            stopScanForCancellation("BLE heart-rate scan stopped because Bluetooth is unavailable")
        }
        syncControllerAvailability(availability)
        val resolved = providerStateAfterAvailabilityRefresh(mutableProviderState.value, availability)
        if (resolved != mutableProviderState.value) publish(resolved)
    }

    fun stopScan() {
        if (!callbackGate.isOpen()) return
        if (isScanning) {
            stopBleScan(BleHeartRateScanState(BleHeartRateScanStateKind.STOPPED, "BLE heart-rate scan stopped"))
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!callbackGate.isOpen()) return
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
            if (lastPlatformFailure?.operation == BlePlatformOperation.READ_SCANNER) {
                publishScanState(platformScanFailureState(BlePlatformOperation.READ_SCANNER))
                publishPlatformAvailabilityFailure(BlePlatformOperation.READ_SCANNER)
            } else {
                publishScanState(BleHeartRateScanState(BleHeartRateScanStateKind.ERROR, "Bluetooth LE scanner is unavailable", BleHeartRateRecoverableReason.SCAN_FAILED))
            }
            return
        }
        val token = callbackGate.beginScan()
        val callback = createScanCallback(token)
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        val started = platformUnit(platformCalls.startScan(leScanner, heartRateServiceScanFilters(), settings, callback))
        if (!started) {
            callbackGate.invalidateScan()
            controller.onManualScanEnded()
            publishScanState(platformScanFailureState(BlePlatformOperation.START_SCAN))
            publishPlatformAvailabilityFailure(BlePlatformOperation.START_SCAN)
            return
        }
        activeScanCallback = callback
        isScanning = true
        val timeout = Runnable {
            if (callbackGate.accepts(token) && isScanning && activeScanCallback === callback) {
                stopBleScan(BleHeartRateScanState(BleHeartRateScanStateKind.STOPPED, "Scan window ended; no background scan is kept running"))
            }
        }
        activeScanTimeout = timeout
        mainHandler.postDelayed(timeout, scanWindowMillis)
        publishScanState(BleHeartRateScanState(BleHeartRateScanStateKind.SCANNING, "Scanning for BLE Heart Rate Service devices"))
    }

    @SuppressLint("MissingPermission")
    fun listBondedDevices() {
        if (!callbackGate.isOpen()) return
        val availability = availabilityState()
        syncControllerAvailability(availability)
        if (availability.kind != BleHeartRateProviderStateKind.NO_SOURCE) {
            publish(availability)
            return
        }
        val bonded = bluetoothAdapter?.let { adapter -> platformValue(platformCalls.readBondedDevices(adapter)) }
            ?: return if (hasExpectedPlatformFailure(BlePlatformOperation.READ_BONDED_DEVICES)) {
            publishPlatformAvailabilityFailure(BlePlatformOperation.READ_BONDED_DEVICES)
        } else Unit
        bonded.forEach { addCandidate(it, null, false) }
    }

    fun selectDevice(identifier: String): BleHeartRateDeviceSelection? {
        if (!callbackGate.isOpen()) return null
        val device = devices[identifier] ?: return null.also {
            publishError("Cannot select unknown BLE device identifier=$identifier", BleHeartRateRecoverableReason.DEVICE_NOT_FOUND)
        }
        stopScan()
        controller.onManualScanEnded()
        callbackGate.invalidateLifecycle()
        controller.clearTarget()
        val selection = device.toSelection() ?: return null
        runtimeTarget = device
        controller.selectNewTarget()
        selectedDevice = selection
        mutableCandidates.value = emptyList()
        publishScanState(BleHeartRateScanState.idle("Scan ended after device selection"))
        publish(BleHeartRateProviderState(BleHeartRateProviderStateKind.DEVICE_SELECTED, "BLE heart-rate source selected", selectedDevice = selection))
        return selection
    }

    fun clearCandidates() {
        if (!callbackGate.isOpen()) return
        devices.clear()
        mutableCandidates.value = emptyList()
    }

    fun connectSelectedDevice() {
        if (!callbackGate.isOpen()) return
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
        controller.beginManualAttempt()
    }

    fun disconnect() {
        if (!callbackGate.isOpen()) return
        callbackGate.invalidateLifecycle()
        stopBleScan()
        controller.userStop()
    }

    fun stop() {
        if (!callbackGate.isOpen()) return
        callbackGate.invalidateLifecycle()
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
        if (!callbackGate.isOpen()) return
        stopBleScan()
        controller.close()
        callbackGate.close()
        attemptGuard.clear()
        runtimeTarget = null
        selectedDevice = null
        activeScanTimeout?.let(mainHandler::removeCallbacks)
        activeScanTimeout = null
        controllerScheduler.cancelAll()
        runCatching { appContext.unregisterReceiver(bluetoothStateReceiver) }
    }

    private fun handleControllerEffect(effect: HeartRateReconnectEffect) {
        if (!callbackGate.isOpen()) return
        when (effect) {
            is HeartRateReconnectEffect.ConnectDirect -> {
                val current = controller.currentState()
                if (current.targetGeneration == effect.targetGeneration && current.attemptGeneration == effect.attemptGeneration) {
                    startDirectConnection(effect)
                }
            }
            is HeartRateReconnectEffect.CloseAttempt -> closeAttempt(effect)
            is HeartRateReconnectEffect.StateChanged -> {
                if (controller.currentState() == effect.state) publishRuntimeState(effect.state)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDirectConnection(effect: HeartRateReconnectEffect.ConnectDirect) {
        if (!callbackGate.isOpen()) return
        val device = runtimeTarget ?: return
        val availability = availabilityState()
        if (availability.kind != BleHeartRateProviderStateKind.NO_SOURCE || !displayEnabled || !foregroundActive) {
            val token = callbackGate.lifecycleToken()
            mainHandler.post {
                if (!callbackGate.accepts(token)) return@post
                syncControllerAvailability(availability)
                publish(availability.copy(selectedDevice = selectedDevice))
            }
            return
        }
        val attempt = GattAttempt(effect.targetGeneration, effect.attemptGeneration)
        currentAttempt = attempt
        val lifecycleToken = callbackGate.lifecycleToken()
        val callback = createGattCallback(attempt, lifecycleToken)
        val gatt = platformValue(platformCalls.connectGatt(device, appContext, callback))
        val connectFailure = lastPlatformFailure?.takeIf { it.operation == BlePlatformOperation.CONNECT_GATT }
        if (gatt == null) {
            currentAttempt = null
            mainHandler.post {
                if (!callbackGate.accepts(lifecycleToken)) return@post
                if (connectFailure != null) {
                    publishPlatformAvailabilityFailure(BlePlatformOperation.CONNECT_GATT, connectFailure)
                } else {
                    controller.technicalFailure(
                        effect.targetGeneration,
                        effect.attemptGeneration,
                        HeartRateFreshnessReason.CONNECT_FAILED
                    )
                }
            }
            return
        }
        currentGatt = gatt
        attemptGuard.bind(gatt, attempt.targetGeneration, attempt.attemptGeneration)
    }

    @SuppressLint("MissingPermission")
    private fun createGattCallback(attempt: GattAttempt, token: HeartRateProviderCallbackGate.Token) = object : BleGattCallback {
        override fun onConnectionStateChange(gatt: BleGattConnection, status: Int, newState: Int) {
            mainHandler.post { if (callbackGate.accepts(token)) handleConnectionStateChange(gatt, attempt, status, newState) }
        }

        override fun onServicesDiscovered(gatt: BleGattConnection, status: Int) {
            mainHandler.post { if (callbackGate.accepts(token)) handleServicesDiscovered(gatt, attempt, status) }
        }

        override fun onDescriptorWrite(gatt: BleGattConnection, descriptor: BleGattDescriptor, status: Int) {
            mainHandler.post { if (callbackGate.accepts(token)) handleDescriptorWrite(gatt, attempt, descriptor, status) }
        }

        override fun onCharacteristicChanged(gatt: BleGattConnection, characteristic: BleGattCharacteristic, value: ByteArray) {
            mainHandler.post { if (callbackGate.accepts(token)) handleCharacteristicChanged(gatt, attempt, characteristic, value) }
        }
    }

    private fun createScanCallback(token: HeartRateProviderCallbackGate.Token) = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            mainHandler.post { if (callbackGate.accepts(token)) handleScanResult(result) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            mainHandler.post { if (callbackGate.accepts(token)) results.forEach(::handleScanResult) }
        }

        override fun onScanFailed(errorCode: Int) {
            mainHandler.post {
                if (!callbackGate.accepts(token)) return@post
                stopBleScan(BleHeartRateScanState(BleHeartRateScanStateKind.ERROR, "BLE scan failed code=$errorCode", BleHeartRateRecoverableReason.SCAN_FAILED))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleConnectionStateChange(gatt: BleGattConnection, attempt: GattAttempt, status: Int, newState: Int) {
        if (!isCurrent(gatt, attempt)) return closeGattInstance(gatt, disconnectFirst = false)
        if (status != BluetoothGatt.GATT_SUCCESS) {
            controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CONNECT_FAILED)
            return
        }
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            val discovered = platformValue(platformCalls.discoverServices(gatt))
            if (discovered != true) {
                if (hasExpectedPlatformFailure(BlePlatformOperation.DISCOVER_SERVICES)) {
                    publishPlatformAvailabilityFailure(BlePlatformOperation.DISCOVER_SERVICES)
                } else {
                    controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED)
                }
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            currentGatt = null
            currentAttempt = null
            attemptGuard.invalidate(gatt, attempt.targetGeneration, attempt.attemptGeneration)
            closeGattInstance(gatt, disconnectFirst = false)
            controller.disconnected(attempt.targetGeneration, attempt.attemptGeneration)
        }
    }

    private fun handleServicesDiscovered(gatt: BleGattConnection, attempt: GattAttempt, status: Int) {
        if (!isCurrent(gatt, attempt)) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED)
            return
        }
        val service = platformValue(platformCalls.readGattService(gatt, HEART_RATE_SERVICE_UUID))
        if (service == null) {
            if (hasExpectedPlatformFailure(BlePlatformOperation.READ_GATT_SERVICE)) {
                publishPlatformAvailabilityFailure(BlePlatformOperation.READ_GATT_SERVICE)
            } else {
                controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED)
            }
            return
        }
        subscribeHeartRateMeasurement(gatt, attempt, service)
    }

    private fun handleDescriptorWrite(gatt: BleGattConnection, attempt: GattAttempt, descriptor: BleGattDescriptor, status: Int) {
        if (!isCurrent(gatt, attempt) || descriptor.characteristicUuid != HEART_RATE_MEASUREMENT_UUID) return
        if (status == BluetoothGatt.GATT_SUCCESS) {
            controller.notifyEnabled(attempt.targetGeneration, attempt.attemptGeneration)
        } else {
            controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CCCD_FAILED)
        }
    }

    private fun handleCharacteristicChanged(gatt: BleGattConnection, attempt: GattAttempt, characteristic: BleGattCharacteristic, value: ByteArray) {
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
    private fun subscribeHeartRateMeasurement(gatt: BleGattConnection, attempt: GattAttempt, service: BleGattService) {
        val measurement = platformValue(platformCalls.readGattCharacteristic(service, HEART_RATE_MEASUREMENT_UUID))
        if (measurement == null) return controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED)
        val canNotify = measurement.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        val canIndicate = measurement.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        if (!canNotify && !canIndicate) return controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CCCD_FAILED)
        val cccd = platformValue(platformCalls.readGattDescriptor(measurement, CLIENT_CHARACTERISTIC_CONFIG_UUID))
            ?: return controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CCCD_FAILED)
        val notificationConfigured = platformValue(platformCalls.configureNotification(gatt, measurement))
        if (notificationConfigured != true) {
            return if (hasExpectedPlatformFailure(BlePlatformOperation.CONFIGURE_NOTIFICATION)) {
                publishPlatformAvailabilityFailure(BlePlatformOperation.CONFIGURE_NOTIFICATION)
            } else {
                controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CCCD_FAILED)
            }
        }
        val value = if (canNotify) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) cccd.setLegacyValue(value)
        val result = platformValue(platformCalls.writeDescriptor(gatt, cccd, value))
        if (result != BluetoothGatt.GATT_SUCCESS) {
            if (hasExpectedPlatformFailure(BlePlatformOperation.WRITE_DESCRIPTOR)) {
                publishPlatformAvailabilityFailure(BlePlatformOperation.WRITE_DESCRIPTOR)
            } else {
                controller.technicalFailure(attempt.targetGeneration, attempt.attemptGeneration, HeartRateFreshnessReason.CCCD_FAILED)
            }
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
    private fun closeGattInstance(gatt: BleGattConnection, disconnectFirst: Boolean) {
        if (disconnectFirst) platformUnit(platformCalls.disconnectGatt(gatt))
        platformUnit(platformCalls.closeGatt(gatt))
    }

    private fun isCurrent(gatt: BleGattConnection, attempt: GattAttempt): Boolean =
        callbackGate.isOpen() && currentGatt === gatt && currentAttempt == attempt &&
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
        val identifier = platformValue(platformCalls.readDeviceIdentifier(device))
            ?: return publishPlatformAvailabilityFailure(BlePlatformOperation.READ_DEVICE_IDENTIFIER)
        devices[identifier] = device
        val displayName = device.displayName() ?: return
        val candidate = BleHeartRateDeviceCandidate(identifier, displayName, rssi, advertisesHeartRateService)
        mutableCandidates.value = mutableCandidates.value.filterNot { it.identifier == candidate.identifier } + candidate
        onCandidate(candidate)
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan(state: BleHeartRateScanState? = null) {
        val callback = activeScanCallback
        if (isScanning && callback != null) {
            val activeScanner = scanner
            if (activeScanner != null) platformUnit(platformCalls.stopScan(activeScanner, callback))
        }
        isScanning = false
        activeScanCallback = null
        activeScanTimeout?.let(mainHandler::removeCallbacks)
        activeScanTimeout = null
        callbackGate.invalidateScan()
        controller.onManualScanEnded()
        if (callbackGate.isOpen()) state?.let(::publishScanState)
    }

    private fun stopScanForCancellation(message: String) {
        if (!isScanning) return
        stopBleScan(BleHeartRateScanState(BleHeartRateScanStateKind.STOPPED, message))
    }

    private fun availabilityState(): BleHeartRateProviderState {
        val missing = missingPermissions()
        if (missing.isNotEmpty()) return BleHeartRateProviderState(BleHeartRateProviderStateKind.PERMISSION_REQUIRED, "Bluetooth permission is required before scanning", missingPermissions = missing)
        val adapter = bluetoothAdapter
        return when {
            adapter == null -> BleHeartRateProviderState(BleHeartRateProviderStateKind.UNAVAILABLE, "Bluetooth adapter is unavailable on this device")
            platformValue(platformCalls.readAdapterEnabled(adapter)) != true ->
                platformAvailabilityFailureState(BlePlatformOperation.READ_ADAPTER_ENABLED)
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
        if (!callbackGate.isOpen()) return
        mutableProviderState.value = state
        mutableHeartRateState.value = state.toHeartRateState()
        onState(state)
    }

    private fun publishScanState(state: BleHeartRateScanState) {
        if (!callbackGate.isOpen()) return
        mutableScanState.value = state
        onScanState(state)
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toSelection(): BleHeartRateDeviceSelection? {
        val identifier = platformValue(platformCalls.readDeviceIdentifier(this))
            ?: return null.also { publishPlatformAvailabilityFailure(BlePlatformOperation.READ_DEVICE_IDENTIFIER) }
        val displayName = displayName() ?: return null
        return BleHeartRateDeviceSelection(identifier, displayName)
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.displayName(): String? {
        val displayName = platformValue(platformCalls.readDeviceName(this))
        if (displayName == null && hasExpectedPlatformFailure(BlePlatformOperation.READ_DEVICE_NAME)) {
            publishPlatformAvailabilityFailure(BlePlatformOperation.READ_DEVICE_NAME)
            return null
        }
        return displayName.takeUnless { it.isNullOrBlank() } ?: "(unknown BLE device)"
    }

    private fun <T> platformValue(result: BlePlatformCallResult<T>): T? {
        lastPlatformFailure = null
        return when (result) {
            is BlePlatformCallResult.Success -> result.value
            is BlePlatformCallResult.ExpectedFailure -> {
                lastPlatformFailure = result
                null
            }
        }
    }

    private fun hasExpectedPlatformFailure(operation: BlePlatformOperation): Boolean =
        lastPlatformFailure?.operation == operation

    private fun platformUnit(result: BlePlatformCallResult<Unit>): Boolean {
        lastPlatformFailure = null
        return when (result) {
            is BlePlatformCallResult.Success -> true
            is BlePlatformCallResult.ExpectedFailure -> {
                lastPlatformFailure = result
                false
            }
        }
    }

    private fun platformAvailabilityFailureState(
        operation: BlePlatformOperation,
        capturedFailure: BlePlatformCallResult.ExpectedFailure? = lastPlatformFailure?.takeIf { it.operation == operation }
    ): BleHeartRateProviderState {
        val failure = capturedFailure
        return if (failure?.exception is SecurityException) {
            BleHeartRateProviderState(
                BleHeartRateProviderStateKind.PERMISSION_REQUIRED,
                "Bluetooth permission became unavailable during a BLE operation",
                missingPermissions = missingPermissions()
            )
        } else {
            BleHeartRateProviderState(
                BleHeartRateProviderStateKind.BLUETOOTH_DISABLED,
                "Bluetooth became unavailable during a BLE operation"
            )
        }
    }

    private fun publishPlatformAvailabilityFailure(
        operation: BlePlatformOperation,
        capturedFailure: BlePlatformCallResult.ExpectedFailure? = lastPlatformFailure?.takeIf { it.operation == operation }
    ) {
        val state = platformAvailabilityFailureState(operation, capturedFailure).copy(selectedDevice = selectedDevice)
        callbackGate.invalidateLifecycle()
        stopBleScan()
        syncControllerAvailability(state)
        publish(state)
    }

    private fun platformScanFailureState(operation: BlePlatformOperation) = BleHeartRateScanState(
        BleHeartRateScanStateKind.ERROR,
        "BLE scan stopped because permission or Bluetooth state changed",
        BleHeartRateRecoverableReason.SCAN_FAILED
    )

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
