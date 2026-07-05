package com.liujyks.trainflow.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.liujyks.trainflow.core.health.BleHeartRateAdapterStatus
import com.liujyks.trainflow.core.health.BleHeartRateAdapterStatusKind
import com.liujyks.trainflow.core.health.BleHeartRateProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeartRateBroadcastSmokeActivity : ComponentActivity() {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val candidateButtons = linkedMapOf<String, Button>()
    private val provider by lazy {
        BleHeartRateProvider(this) { status ->
            handleAdapterStatus(status)
        }
    }

    private lateinit var adapterStateView: TextView
    private lateinit var deviceList: LinearLayout
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildView())
        appendLog("HR Broadcast Smoke ready")
        appendLog("Use Band 9 heart-rate broadcast mode, then scan.")
        appendLog("This debug-only tool does not write TrainFlow records.")
        appendLog("Production TrainFlow still has no heart-rate UI, records, or trends.")
    }

    override fun onStop() {
        super.onStop()
        provider.stop()
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
            text = "Debug-only BLE HRS adapter spike. It scans 0x180D / 0x2A37 notify and maps bpm to HeartRateState only inside this debug tool."
            textSize = 14f
        })
        adapterStateView = TextView(this).apply {
            text = "Adapter: stopped"
            textSize = 14f
        }
        root.addView(adapterStateView)
        root.addView(button("Grant Bluetooth Permissions") { requestBluetoothPermissions() })
        root.addView(button("Scan All BLE Devices") { startScan() })
        root.addView(button("List Bonded Devices") { listBondedDevices() })
        root.addView(button("Stop / Disconnect") { provider.stop() })
        root.addView(button("Clear") {
            provider.clearCandidates()
            candidateButtons.clear()
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

    private fun startScan() {
        if (!hasRequiredPermissions()) {
            appendLog("Missing Bluetooth permissions; tap Grant first")
            requestBluetoothPermissions()
            return
        }
        candidateButtons.clear()
        deviceList.removeAllViews()
        provider.startScan()
    }

    private fun listBondedDevices() {
        if (!hasRequiredPermissions()) {
            appendLog("Missing Bluetooth permissions; tap Grant first")
            requestBluetoothPermissions()
            return
        }
        provider.listBondedDevices()
    }

    private fun handleAdapterStatus(status: BleHeartRateAdapterStatus) {
        runOnUiThread {
            adapterStateView.text = "Adapter: ${status.kind.name.lowercase(Locale.US)}"
            appendLog(status.message)
            if (status.kind == BleHeartRateAdapterStatusKind.DEVICE_FOUND &&
                status.address != null &&
                status.deviceLabel != null &&
                !candidateButtons.containsKey(status.address)
            ) {
                val button = Button(this).apply {
                    text = "${status.deviceLabel}\n${status.candidateLabel ?: status.message}"
                    setAllCaps(false)
                    setOnClickListener {
                        provider.stopScan()
                        provider.connect(status.address)
                    }
                }
                candidateButtons[status.address] = button
                deviceList.addView(button)
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val missing = requiredPermissions().filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            appendLog("Permissions already granted")
            return
        }
        appendLog("Requesting ${missing.joinToString()}")
        requestPermissions(missing.toTypedArray(), REQUEST_BLUETOOTH)
    }

    @Deprecated("Debug smoke keeps the existing simple permission request path.")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH) {
            permissions.forEachIndexed { index, permission ->
                val granted = grantResults.getOrNull(index) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
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
            checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun appendLog(message: String) {
        val line = "${timeFormat.format(Date())} $message"
        logView.append(line)
        logView.append("\n")
    }

    companion object {
        private const val REQUEST_BLUETOOTH = 8102
    }
}
