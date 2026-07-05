package com.liujyks.trainflow.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HeartRateBroadcastSmokeActivity : ComponentActivity() {
    private val bluetoothAdapter by lazy {
        getSystemService(BluetoothManager::class.java).adapter
    }
    private val scanner by lazy { bluetoothAdapter.bluetoothLeScanner }
    private val devices = linkedMapOf<String, BluetoothDevice>()
    private val seenDeviceLabels = linkedMapOf<String, String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private lateinit var deviceList: LinearLayout
    private lateinit var logView: TextView
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
            appendLog("SCAN FAILED code=$errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            appendLog("GATT connection status=$status state=$newState")
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                appendLog("GATT connected; discovering services")
                gatt.discoverServices()
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                appendLog("GATT disconnected")
                gatt.close()
                if (currentGatt == gatt) {
                    currentGatt = null
                    currentGattLabel = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            appendLog("Services discovered status=$status count=${gatt.services.size}")
            gatt.services.forEach { service ->
                appendLog("service ${service.uuid.shortLabel()}")
                service.characteristics.forEach { characteristic ->
                    appendLog(
                        "  characteristic ${characteristic.uuid.shortLabel()} " +
                            "props=${characteristic.properties.propertyLabel()}"
                    )
                }
            }

            val heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID)
            if (heartRateService == null) {
                appendLog("RESULT: HRS 0x180D not found")
                return
            }

            appendLog("RESULT: HRS 0x180D found")
            subscribeHeartRateMeasurement(gatt, heartRateService)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            appendLog(
                "Descriptor write ${descriptor.uuid.shortLabel()} status=$status " +
                    "for ${descriptor.characteristic.uuid.shortLabel()}"
            )
            if (descriptor.characteristic.uuid == HEART_RATE_MEASUREMENT_UUID &&
                status == BluetoothGatt.GATT_SUCCESS
            ) {
                appendLog("RESULT: 0x2A37 notify enabled")
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
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            handleCharacteristicChanged(gatt, characteristic, characteristic.value ?: byteArrayOf())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildView())
        appendLog("HR Broadcast Smoke ready")
        appendLog("Use Band 9 heart-rate broadcast mode, then scan.")
        appendLog("This debug-only tool does not write TrainFlow records.")
    }

    override fun onStop() {
        super.onStop()
        stopScan()
        closeGatt()
    }

    private fun buildView(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }

        fun button(label: String, onClick: () -> Unit): Button {
            return Button(this).apply {
                text = label
                setOnClickListener { onClick() }
            }
        }

        root.addView(TextView(this).apply {
            text = "HR Broadcast Smoke"
            textSize = 22f
        })
        root.addView(TextView(this).apply {
            text = "Debug-only BLE HRS scan. Production TrainFlow still has no heart-rate UI."
            textSize = 14f
        })
        root.addView(button("Grant Bluetooth Permissions") { requestBluetoothPermissions() })
        root.addView(button("Scan All BLE Devices") { startScan() })
        root.addView(button("List Bonded Devices") { listBondedDevices() })
        root.addView(button("Stop / Disconnect") {
            stopScan()
            closeGatt()
        })
        root.addView(button("Clear") {
            devices.clear()
            seenDeviceLabels.clear()
            deviceList.removeAllViews()
            logView.text = ""
            appendLog("Cleared log/device list; active GATT remains until Stop / Disconnect.")
        })
        root.addView(TextView(this).apply {
            text = "Devices"
            textSize = 18f
        })
        deviceList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(deviceList)
        root.addView(TextView(this).apply {
            text = "Log"
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

    private fun requestBluetoothPermissions() {
        val missing = requiredPermissions().filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            appendLog("Permissions already granted")
            return
        }
        appendLog("Requesting ${missing.joinToString()}")
        requestPermissions(missing.toTypedArray(), REQUEST_BLUETOOTH)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH) {
            permissions.forEachIndexed { index, permission ->
                val granted = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
                appendLog("permission $permission granted=$granted")
            }
        }
    }

    private fun requiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (!hasRequiredPermissions()) {
            appendLog("Missing Bluetooth permissions; tap Grant first")
            requestBluetoothPermissions()
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            appendLog("Bluetooth adapter disabled")
            return
        }

        stopScan()
        devices.clear()
        seenDeviceLabels.clear()
        deviceList.removeAllViews()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(null, settings, scanCallback)
        isScanning = true
        appendLog("Scan started: all BLE advertisements, low latency")
        appendLog("Expected positive path: device -> service 0x180D -> characteristic 0x2A37 notify")
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (isScanning && hasRequiredPermissions()) {
            scanner.stopScan(scanCallback)
            appendLog("Scan stopped")
        }
        isScanning = false
    }

    @SuppressLint("MissingPermission")
    private fun listBondedDevices() {
        if (!hasRequiredPermissions()) {
            appendLog("Missing Bluetooth permissions; tap Grant first")
            requestBluetoothPermissions()
            return
        }
        val bonded = bluetoothAdapter.bondedDevices.orEmpty()
        appendLog("Bonded devices count=${bonded.size}")
        bonded.forEach { addDevice(it, "bonded") }
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
            appendLog("scan $label")
        }
        addDevice(device, label)
    }

    @SuppressLint("MissingPermission")
    private fun addDevice(device: BluetoothDevice, label: String) {
        if (devices.containsKey(device.address)) return
        devices[device.address] = device
        runOnUiThread {
            deviceList.addView(Button(this).apply {
                text = "${device.safeName()} ${device.address}\n$label"
                setAllCaps(false)
                setOnClickListener {
                    stopScan()
                    connect(device)
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        if (!hasRequiredPermissions()) {
            appendLog("Missing Bluetooth permissions; tap Grant first")
            requestBluetoothPermissions()
            return
        }
        closeGatt()
        currentGattLabel = device.debugLabel()
        appendLog("Connecting $currentGattLabel")
        currentGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(this, false, gattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeHeartRateMeasurement(
        gatt: BluetoothGatt,
        heartRateService: BluetoothGattService
    ) {
        val measurement = heartRateService.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
        if (measurement == null) {
            appendLog("RESULT: characteristic 0x2A37 not found")
            return
        }

        val properties = measurement.properties
        val canNotify = properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        val canIndicate = properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        appendLog("RESULT: characteristic 0x2A37 found props=${properties.propertyLabel()}")
        if (!canNotify && !canIndicate) {
            appendLog("RESULT: 0x2A37 has no notify/indicate property")
            return
        }

        val cccd = measurement.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (cccd == null) {
            appendLog("RESULT: 0x2A37 CCCD 0x2902 not found")
            return
        }

        val notificationSet = gatt.setCharacteristicNotification(measurement, true)
        appendLog("setCharacteristicNotification=$notificationSet")
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
        appendLog("write CCCD result=$writeResult")
    }

    @SuppressLint("MissingPermission")
    private fun handleCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        if (characteristic.uuid != HEART_RATE_MEASUREMENT_UUID) return
        val bpm = parseHeartRateBpm(value)
        val hex = value.joinToString(" ") { "%02X".format(it) }
        val source = gatt.device?.debugLabel() ?: currentGattLabel ?: "(unknown)"
        if (bpm == null) {
            appendLog("RESULT: heart-rate notify unreadable source=$source bytes=$hex")
        } else {
            appendLog("RESULT: heart-rate notify bpm=$bpm source=$source bytes=$hex")
        }
    }

    private fun parseHeartRateBpm(value: ByteArray): Int? {
        if (value.size < 2) return null
        val flags = value[0].toInt() and 0xFF
        return if (flags and 0x01 == 0) {
            value[1].toInt() and 0xFF
        } else {
            if (value.size < 3) null else {
                (value[1].toInt() and 0xFF) or ((value[2].toInt() and 0xFF) shl 8)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        currentGatt?.let {
            appendLog("Closing GATT")
            it.disconnect()
            it.close()
        }
        currentGatt = null
        currentGattLabel = null
    }

    private fun appendLog(message: String) {
        val line = "${timeFormat.format(Date())} $message"
        runOnUiThread {
            logView.append(line)
            logView.append("\n")
        }
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

    companion object {
        private const val REQUEST_BLUETOOTH = 8102
        private val HEART_RATE_SERVICE_UUID: UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val HEART_RATE_MEASUREMENT_UUID: UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
