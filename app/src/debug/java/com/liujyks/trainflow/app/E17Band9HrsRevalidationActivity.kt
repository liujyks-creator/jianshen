package com.liujyks.trainflow.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.liujyks.trainflow.core.health.BleHeartRatePermissionPlanner
import com.liujyks.trainflow.core.health.HeartRateBpmFormat
import com.liujyks.trainflow.core.health.HeartRateMeasurementParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Debug-only evidence harness for E17-1. It is not a production provider. */
class E17Band9HrsRevalidationActivity : ComponentActivity() {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val candidateButtons = linkedMapOf<String, Button>()
    private val devices = linkedMapOf<String, BluetoothDevice>()
    private val bluetoothAdapter by lazy {
        getSystemService(BluetoothManager::class.java)?.adapter
    }

    private lateinit var statusView: TextView
    private lateinit var deviceList: LinearLayout
    private lateinit var logView: TextView
    private var isScanning = false
    private var currentGatt: BluetoothGatt? = null
    private val measurementLock = Any()
    private var connectionSequence = 0L
    private var notifyCycleSequence = 0L
    private var activeConnectionSessionId = "none"
    private var activeNotifyCycleId = "none"
    private var notifyEnabledElapsedMs: Long? = null
    private var lastValidSampleElapsedMs: Long? = null

    private val scanTimeout = Runnable {
        stopScan("scan_window_ended")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            recordScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::recordScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            mainHandler.removeCallbacks(scanTimeout)
            logPlatformFailure(
                stage = "scan",
                failureCode = "SCAN_CALLBACK_FAILED",
                platformStatus = errorCode
            )
            logEvidence("SCAN_FAILED error_code=$errorCode")
            showStatus("Scan failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (gatt !== currentGatt) {
                logEvidence("IGNORED_OLD_GATT_CALLBACK ${sourceFields(gatt)} status=$status state=$newState")
                closeGatt(gatt, requestDisconnect = false)
                return
            }
            logEvidence(
                "GATT_CONNECTION ${sourceFields(gatt)} status=$status state=$newState " +
                    "success=${status == BluetoothGatt.GATT_SUCCESS}"
            )
            when {
                status != BluetoothGatt.GATT_SUCCESS -> {
                    logPlatformFailure(
                        stage = "connection_state",
                        failureCode = "GATT_STATUS_NON_SUCCESS",
                        platformStatus = status,
                        gatt = gatt
                    )
                    closeGatt(gatt, requestDisconnect = false)
                    showStatus("GATT connection failed: $status")
                }

                newState == BluetoothProfile.STATE_CONNECTED -> {
                    showStatus("Connected; discovering services")
                    val started = gatt.discoverServices()
                    logEvidence("SERVICE_DISCOVERY_START ${sourceFields(gatt)} started=$started")
                    if (!started) {
                        logPlatformFailure(
                            stage = "service_discovery_start",
                            failureCode = "PLATFORM_CALL_NOT_STARTED",
                            gatt = gatt
                        )
                        closeGatt(gatt, requestDisconnect = true)
                    }
                }

                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    logTypedOutcome(
                        outcome = "EXPLICIT_DISCONNECT",
                        gatt = gatt,
                        fields = "gatt_status=$status new_state=$newState"
                    )
                    logEvidence("GATT_DISCONNECTED ${sourceFields(gatt)}")
                    closeGatt(gatt, requestDisconnect = false)
                    showStatus("Disconnected")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (!isCurrentGatt(gatt, "services_discovered")) return
            val services = gatt.services.orEmpty().joinToString { service ->
                E17HrsEvidenceFormatter.uuid(service.uuid)
            }
            logEvidence(
                "SERVICE_DISCOVERY_RESULT ${sourceFields(gatt)} status=$status " +
                    "success=${status == BluetoothGatt.GATT_SUCCESS} services=[$services]"
            )
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logPlatformFailure(
                    stage = "service_discovery_result",
                    failureCode = "GATT_STATUS_NON_SUCCESS",
                    platformStatus = status,
                    gatt = gatt
                )
                closeGatt(gatt, requestDisconnect = true)
                return
            }

            val service = gatt.getService(HEART_RATE_SERVICE_UUID)
            logEvidence("HRS_SERVICE ${sourceFields(gatt)} uuid=0x180D found=${service != null}")
            if (service == null) {
                logPlatformFailure(
                    stage = "service_validation",
                    failureCode = "HRS_SERVICE_MISSING",
                    gatt = gatt
                )
                closeGatt(gatt, requestDisconnect = true)
                return
            }

            val characteristic = service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
            logEvidence(
                "HRS_MEASUREMENT ${sourceFields(gatt)} uuid=0x2A37 found=${characteristic != null}" +
                    if (characteristic != null) {
                        " properties=${E17HrsEvidenceFormatter.characteristicProperties(characteristic.properties)}"
                    } else {
                        ""
                    }
            )
            if (characteristic == null) {
                logPlatformFailure(
                    stage = "characteristic_validation",
                    failureCode = "HEART_RATE_MEASUREMENT_MISSING",
                    gatt = gatt
                )
                closeGatt(gatt, requestDisconnect = true)
                return
            }
            subscribe(gatt, characteristic)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (!isCurrentGatt(gatt, "descriptor_write")) return
            logEvidence(
                "CCCD_WRITE ${sourceFields(gatt)} descriptor=${E17HrsEvidenceFormatter.uuid(descriptor.uuid)} " +
                    "characteristic=${E17HrsEvidenceFormatter.uuid(descriptor.characteristic.uuid)} " +
                    "status=$status success=${status == BluetoothGatt.GATT_SUCCESS}"
            )
            showStatus(
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    "Notify/indicate enabled; waiting for payloads"
                } else {
                    "CCCD write failed: $status"
                }
            )
            if (status == BluetoothGatt.GATT_SUCCESS) {
                beginNotifyCycle(gatt)
            } else {
                logPlatformFailure(
                    stage = "cccd_write_result",
                    failureCode = "GATT_STATUS_NON_SUCCESS",
                    platformStatus = status,
                    gatt = gatt
                )
                closeGatt(gatt, requestDisconnect = true)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            recordNotification(gatt, characteristic, value)
        }

        @Deprecated("Deprecated by Android platform for API 33+")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            recordNotification(gatt, characteristic, characteristic.value ?: byteArrayOf())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildView())
        logEvidence("E17_1_HRS_REVALIDATION_READY model=${quoted(Build.MODEL)} android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}")
        logEvidence("EVIDENCE_SCOPE real Band 9 actions must be performed by the user; AVD cannot validate BLE peripheral behavior")
    }

    override fun onStop() {
        cleanup("activity_stopped")
        super.onStop()
    }

    private fun buildView(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }
        root.addView(TextView(this).apply {
            text = "E17-1 Band 9 HRS Revalidation"
            textSize = 22f
        })
        root.addView(TextView(this).apply {
            text = "Debug-only evidence tool: scan source, 0x180D, 0x2A37 properties, 0x2902 result, raw payload, parsed bpm, and cleanup."
            textSize = 14f
        })
        statusView = TextView(this).apply {
            text = "Status: ready"
            textSize = 14f
        }
        root.addView(statusView)
        root.addView(button("Grant Bluetooth Permissions") { requestBluetoothPermissions() })
        root.addView(button("Scan Standard HRS (12s)") { startScan() })
        root.addView(button("Stop / Disconnect") { cleanup("user_stop") })
        root.addView(button("Clear Visible Log / Candidates") {
            candidateButtons.clear()
            devices.clear()
            deviceList.removeAllViews()
            logView.text = ""
            logEvidence("VISIBLE_LOG_CLEARED active_connection_unchanged=${currentGatt != null}")
        })
        root.addView(TextView(this).apply {
            text = "Discovered standard HRS sources"
            textSize = 18f
        })
        deviceList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(deviceList)
        root.addView(TextView(this).apply {
            text = "M0 evidence only (logcat tag $LOG_TAG; verbose diagnostics use $VERBOSE_LOG_TAG)"
            textSize = 18f
        })
        logView = TextView(this).apply {
            textSize = 12f
            setTextIsSelectable(true)
        }
        root.addView(logView)
        return ScrollView(this).apply {
            addView(
                root,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        setAllCaps(false)
        setOnClickListener { action() }
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (!hasRequiredPermissions()) {
            logEvidence("SCAN_BLOCKED missing_permissions=${missingPermissions().joinToString()}")
            requestBluetoothPermissions()
            return
        }
        val adapter = bluetoothAdapter
        val scanner = adapter?.bluetoothLeScanner
        if (adapter == null || scanner == null) {
            logEvidence("SCAN_BLOCKED bluetooth_le_scanner_unavailable=true")
            return
        }
        if (!adapter.isEnabled) {
            logEvidence("SCAN_BLOCKED bluetooth_disabled=true")
            return
        }

        cleanup("new_scan")
        devices.clear()
        candidateButtons.clear()
        deviceList.removeAllViews()
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(listOf(filter), settings, scanCallback)
        isScanning = true
        mainHandler.postDelayed(scanTimeout, SCAN_WINDOW_MILLIS)
        showStatus("Scanning standard HRS for 12 seconds")
        logEvidence("SCAN_STARTED filter_service=0x180D window_ms=$SCAN_WINDOW_MILLIS")
    }

    @SuppressLint("MissingPermission")
    private fun recordScanResult(result: ScanResult) {
        val device = result.device ?: return
        val identifier = device.address
        val label = deviceLabel(device)
        val services = result.scanRecord?.serviceUuids.orEmpty().joinToString { parcelUuid ->
            E17HrsEvidenceFormatter.uuid(parcelUuid.uuid)
        }
        devices[identifier] = device
        logEvidence(
            "SCAN_SOURCE label=${quoted(label)} identifier=${quoted(identifier)} rssi=${result.rssi} services=[$services]"
        )
        runOnUiThread {
            if (candidateButtons.containsKey(identifier)) return@runOnUiThread
            val candidateButton = button("$label\n$identifier rssi=${result.rssi} services=[$services]") {
                connect(device)
            }
            candidateButtons[identifier] = candidateButton
            deviceList.addView(candidateButton)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        stopScan("source_selected")
        closeGatt(currentGatt, requestDisconnect = true)
        beginConnectionSession(device)
        showStatus("Connecting ${deviceLabel(device)}")
        logEvidence("CONNECT_REQUEST label=${quoted(deviceLabel(device))} identifier=${quoted(device.address)} transport=LE auto_connect=false")
        currentGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(applicationContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(applicationContext, false, gattCallback)
        }
        if (currentGatt == null) {
            logPlatformFailure(
                stage = "connect_request",
                failureCode = "PLATFORM_CALL_RETURNED_NULL"
            )
            logEvidence("CONNECT_REQUEST_RETURNED_NULL identifier=${quoted(device.address)}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribe(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val canNotify = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        val canIndicate = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        if (!canNotify && !canIndicate) {
            logPlatformFailure(
                stage = "characteristic_validation",
                failureCode = "NOTIFY_OR_INDICATE_UNSUPPORTED",
                gatt = gatt
            )
            logEvidence("SUBSCRIBE_BLOCKED ${sourceFields(gatt)} notify=false indicate=false")
            closeGatt(gatt, requestDisconnect = true)
            return
        }
        val cccd = characteristic.getDescriptor(CCCD_UUID)
        logEvidence("CCCD_DISCOVERY ${sourceFields(gatt)} descriptor=0x2902 found=${cccd != null}")
        if (cccd == null) {
            logPlatformFailure(
                stage = "descriptor_validation",
                failureCode = "CCCD_MISSING",
                gatt = gatt
            )
            closeGatt(gatt, requestDisconnect = true)
            return
        }

        val localNotification = gatt.setCharacteristicNotification(characteristic, true)
        val mode = if (canNotify) "notify" else "indicate"
        val value = if (canNotify) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }
        logEvidence(
            "LOCAL_NOTIFICATION ${sourceFields(gatt)} characteristic=0x2A37 enabled=$localNotification mode=$mode"
        )
        if (!localNotification) {
            logPlatformFailure(
                stage = "local_notification_enable",
                failureCode = "PLATFORM_CALL_NOT_STARTED",
                gatt = gatt
            )
            closeGatt(gatt, requestDisconnect = true)
            return
        }

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(cccd, value)
        } else {
            @Suppress("DEPRECATION")
            run {
                cccd.value = value
                if (gatt.writeDescriptor(cccd)) BluetoothGatt.GATT_SUCCESS else -1
            }
        }
        logEvidence(
            "CCCD_WRITE_START ${sourceFields(gatt)} descriptor=0x2902 mode=$mode " +
                "value=${quoted(E17HrsEvidenceFormatter.bytes(value))} result=$result " +
                "started=${result == BluetoothGatt.GATT_SUCCESS}"
        )
        if (result != BluetoothGatt.GATT_SUCCESS) {
            logPlatformFailure(
                stage = "cccd_write_start",
                failureCode = "PLATFORM_CALL_NOT_STARTED",
                platformStatus = result,
                gatt = gatt
            )
            closeGatt(gatt, requestDisconnect = true)
        }
    }

    private fun recordNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        if (!isCurrentGatt(gatt, "notification")) return
        if (characteristic.uuid != HEART_RATE_MEASUREMENT_UUID) {
            logEvidence("IGNORED_CHARACTERISTIC uuid=${E17HrsEvidenceFormatter.uuid(characteristic.uuid)} ${sourceFields(gatt)}")
            return
        }
        val receivedElapsedMs = SystemClock.elapsedRealtime()
        val measurement = HeartRateMeasurementParser.parse(value)
        val format = when (measurement?.flags?.bpmFormat) {
            HeartRateBpmFormat.UINT8 -> "uint8"
            HeartRateBpmFormat.UINT16 -> "uint16"
            null -> null
        }
        val rawPayload = E17HrsEvidenceFormatter.bytes(value)
        if (measurement == null) {
            val measurementSnapshot = measurementSnapshot()
            logTypedOutcome(
                outcome = "MALFORMED_PAYLOAD",
                gatt = gatt,
                receivedElapsedMs = receivedElapsedMs,
                fields = "raw_payload=${quoted(rawPayload)} " +
                    "last_valid_interval_origin_unchanged=true " +
                    "last_valid_sample_elapsed_ms=${measurementSnapshot.lastValidSampleElapsedMs ?: "none"}"
            )
        } else {
            val timing = recordValidSample(receivedElapsedMs)
            logTypedOutcome(
                outcome = "VALID_SAMPLE",
                gatt = gatt,
                receivedElapsedMs = receivedElapsedMs,
                fields = "notify_enabled_elapsed_ms=${timing.notifyEnabledElapsedMs ?: "none"} " +
                    "first_valid_sample_delay_ms=${timing.firstValidSampleDelayMs ?: "not_first"} " +
                    "valid_interval_ms=${timing.validIntervalMs ?: "first"} " +
                    "bpm=${measurement.bpm} raw_payload=${quoted(rawPayload)} " +
                    "flags=${measurement.flags.raw} bpm_format=${format ?: "unknown"}"
            )
        }
        logEvidence(
            E17HrsEvidenceFormatter.notifyLine(
                sourceLabel = deviceLabel(gatt.device),
                sourceIdentifier = deviceIdentifier(gatt.device),
                rawPayload = value,
                parsedBpm = measurement?.bpm,
                flags = measurement?.flags?.raw,
                format = format
            )
        )
        showStatus(measurement?.let { "Live ${it.bpm} bpm" } ?: "Unparseable payload")
    }

    @SuppressLint("MissingPermission")
    private fun cleanup(reason: String) {
        val hadScan = isScanning
        val gatt = currentGatt
        stopScan(reason, logWhenInactive = false)
        val source = gatt?.let(::sourceFields) ?: "source_label=\"none\" source_identifier=\"none\""
        closeGatt(gatt, requestDisconnect = true)
        logEvidence(
            "CLEANUP reason=$reason $source scan_stopped=$hadScan " +
                "gatt_disconnect_requested=${gatt != null} gatt_closed=${gatt != null}"
        )
        showStatus("Stopped / disconnected")
    }

    @SuppressLint("MissingPermission")
    private fun beginConnectionSession(device: BluetoothDevice) {
        val receivedElapsedMs = SystemClock.elapsedRealtime()
        val snapshot = synchronized(measurementLock) {
            connectionSequence += 1
            activeConnectionSessionId = "connection-$connectionSequence"
            activeNotifyCycleId = "none"
            notifyEnabledElapsedMs = null
            lastValidSampleElapsedMs = null
            measurementSnapshotLocked()
        }
        logEvidence(
            "M0_CONNECTION_SESSION_STARTED connection_session_id=${snapshot.connectionSessionId} " +
                "notify_cycle_id=${snapshot.notifyCycleId} received_elapsed_ms=$receivedElapsedMs " +
                "source_label=${quoted(deviceLabel(device))} " +
                "source_identifier=${quoted(deviceIdentifier(device))} interval_baseline_reset=true"
        )
    }

    private fun beginNotifyCycle(gatt: BluetoothGatt) {
        val enabledElapsedMs = SystemClock.elapsedRealtime()
        val snapshot = synchronized(measurementLock) {
            notifyCycleSequence += 1
            activeNotifyCycleId = "notify-$notifyCycleSequence"
            notifyEnabledElapsedMs = enabledElapsedMs
            lastValidSampleElapsedMs = null
            measurementSnapshotLocked()
        }
        logEvidence(
            "M0_NOTIFY_ENABLED connection_session_id=${snapshot.connectionSessionId} " +
                "notify_cycle_id=${snapshot.notifyCycleId} notify_enabled_elapsed_ms=$enabledElapsedMs " +
                "${sourceFields(gatt)} interval_baseline_reset=true"
        )
    }

    private fun recordValidSample(receivedElapsedMs: Long): ValidSampleTiming =
        synchronized(measurementLock) {
            val previousValidElapsedMs = lastValidSampleElapsedMs
            val enabledElapsedMs = notifyEnabledElapsedMs
            lastValidSampleElapsedMs = receivedElapsedMs
            ValidSampleTiming(
                notifyEnabledElapsedMs = enabledElapsedMs,
                firstValidSampleDelayMs = if (previousValidElapsedMs == null && enabledElapsedMs != null) {
                    receivedElapsedMs - enabledElapsedMs
                } else {
                    null
                },
                validIntervalMs = previousValidElapsedMs?.let(receivedElapsedMs::minus)
            )
        }

    private fun measurementSnapshot(): MeasurementSnapshot =
        synchronized(measurementLock) { measurementSnapshotLocked() }

    private fun measurementSnapshotLocked(): MeasurementSnapshot = MeasurementSnapshot(
        connectionSessionId = activeConnectionSessionId,
        notifyCycleId = activeNotifyCycleId,
        notifyEnabledElapsedMs = notifyEnabledElapsedMs,
        lastValidSampleElapsedMs = lastValidSampleElapsedMs
    )

    private fun logTypedOutcome(
        outcome: String,
        gatt: BluetoothGatt? = null,
        receivedElapsedMs: Long = SystemClock.elapsedRealtime(),
        fields: String
    ) {
        val snapshot = measurementSnapshot()
        val source = gatt?.let(::sourceFields)
            ?: "source_label=\"none\" source_identifier=\"none\""
        logEvidence(
            "$outcome connection_session_id=${snapshot.connectionSessionId} " +
                "notify_cycle_id=${snapshot.notifyCycleId} received_elapsed_ms=$receivedElapsedMs " +
                "$source $fields"
        )
    }

    private fun logPlatformFailure(
        stage: String,
        failureCode: String,
        platformStatus: Int? = null,
        gatt: BluetoothGatt? = null
    ) {
        logTypedOutcome(
            outcome = "PLATFORM_FAILURE",
            gatt = gatt,
            fields = "stage=$stage failure_code=$failureCode " +
                "platform_status=${platformStatus ?: "none"}"
        )
    }

    @SuppressLint("MissingPermission")
    private fun stopScan(reason: String, logWhenInactive: Boolean = true) {
        if (isScanning) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            mainHandler.removeCallbacks(scanTimeout)
            logEvidence("SCAN_STOPPED reason=$reason")
        } else if (logWhenInactive) {
            logEvidence("SCAN_ALREADY_STOPPED reason=$reason")
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt(gatt: BluetoothGatt?, requestDisconnect: Boolean) {
        if (gatt == null) return
        if (requestDisconnect) gatt.disconnect()
        gatt.close()
        if (currentGatt === gatt) currentGatt = null
    }

    private fun isCurrentGatt(gatt: BluetoothGatt, callback: String): Boolean {
        if (gatt === currentGatt) return true
        logEvidence("IGNORED_OLD_GATT_CALLBACK callback=$callback ${sourceFields(gatt)}")
        return false
    }

    private fun requestBluetoothPermissions() {
        val missing = missingPermissions()
        if (missing.isEmpty()) {
            logEvidence("PERMISSIONS already_granted=true")
            return
        }
        logEvidence("PERMISSION_REQUEST permissions=${missing.joinToString()}")
        requestPermissions(missing.toTypedArray(), REQUEST_BLUETOOTH)
    }

    @Deprecated("Debug-only evidence activity uses the platform permission callback directly.")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_BLUETOOTH) return
        permissions.forEachIndexed { index, permission ->
            val granted = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
            logEvidence("PERMISSION_RESULT permission=$permission granted=$granted")
        }
    }

    private fun missingPermissions(): List<String> =
        BleHeartRatePermissionPlanner.requiredPermissions().filter { permission ->
            checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }

    private fun hasRequiredPermissions(): Boolean = missingPermissions().isEmpty()

    @SuppressLint("MissingPermission")
    private fun sourceFields(gatt: BluetoothGatt): String =
        "source_label=${quoted(deviceLabel(gatt.device))} source_identifier=${quoted(deviceIdentifier(gatt.device))}"

    @SuppressLint("MissingPermission")
    private fun deviceLabel(device: BluetoothDevice?): String =
        device?.let { runCatching { it.name }.getOrNull() }
            .takeUnless { it.isNullOrBlank() }
            ?: "(unknown BLE device)"

    @SuppressLint("MissingPermission")
    private fun deviceIdentifier(device: BluetoothDevice?): String =
        device?.let { runCatching { it.address }.getOrNull() } ?: "(unknown identifier)"

    private fun showStatus(message: String) {
        runOnUiThread { statusView.text = "Status: $message" }
    }

    private fun logEvidence(message: String) {
        val timestamp = synchronized(timeFormat) { timeFormat.format(Date()) }
        val line = "$timestamp $message"
        val isM0Evidence = M0_EVIDENCE_PREFIXES.any(message::startsWith)
        Log.i(if (isM0Evidence) LOG_TAG else VERBOSE_LOG_TAG, line)
        if (isM0Evidence) {
            runOnUiThread {
                logView.append(line)
                logView.append("\n")
            }
        }
    }

    private fun quoted(value: String): String = "\"${value.replace("\"", "'")}\""

    private data class MeasurementSnapshot(
        val connectionSessionId: String,
        val notifyCycleId: String,
        val notifyEnabledElapsedMs: Long?,
        val lastValidSampleElapsedMs: Long?
    )

    private data class ValidSampleTiming(
        val notifyEnabledElapsedMs: Long?,
        val firstValidSampleDelayMs: Long?,
        val validIntervalMs: Long?
    )

    private companion object {
        const val LOG_TAG = "TrainFlowE17Hrs"
        const val VERBOSE_LOG_TAG = "TrainFlowE17HrsVerbose"
        const val REQUEST_BLUETOOTH = 17101
        const val SCAN_WINDOW_MILLIS = 12_000L
        val M0_EVIDENCE_PREFIXES = listOf(
            "M0_CONNECTION_SESSION_STARTED",
            "M0_NOTIFY_ENABLED",
            "VALID_SAMPLE",
            "MALFORMED_PAYLOAD",
            "EXPLICIT_DISCONNECT",
            "PLATFORM_FAILURE",
            "CLEANUP"
        )
        val HEART_RATE_SERVICE_UUID: UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
