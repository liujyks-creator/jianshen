package com.liujyks.trainflow.app

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.liujyks.trainflow.core.health.AndroidBleHeartRateProvider
import com.liujyks.trainflow.core.health.BleHeartRateDeviceCandidate
import com.liujyks.trainflow.core.health.BleHeartRatePermissionPlanner
import com.liujyks.trainflow.core.health.BleHeartRatePermissionTrigger
import com.liujyks.trainflow.core.health.BleHeartRateProviderState
import com.liujyks.trainflow.core.health.BleHeartRateProviderStateKind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeartRateBroadcastSmokeActivity : ComponentActivity() {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val candidateButtons = linkedMapOf<String, Button>()
    private val provider by lazy {
        AndroidBleHeartRateProvider(this) { state ->
            handleProviderState(state)
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
        appendLog("Permissions are requested only after tapping the explicit permission button.")
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
            text = "Debug-only BLE HRS harness. It explicitly starts the production-capable provider, scans 0x180D / 0x2A37 notify, and keeps bpm out of TrainFlow records."
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

    private fun handleProviderState(state: BleHeartRateProviderState) {
        runOnUiThread {
            adapterStateView.text = "Provider: ${state.kind.name.lowercase(Locale.US)}"
            appendLog(state.logLine())
            if (state.kind == BleHeartRateProviderStateKind.DEVICE_FOUND && state.candidate != null) {
                addCandidateButton(state.candidate)
            }
        }
    }

    private fun addCandidateButton(candidate: BleHeartRateDeviceCandidate) {
        if (candidateButtons.containsKey(candidate.identifier)) return
        val button = Button(this).apply {
            text = buildString {
                append(candidate.displayName)
                append("\n")
                append(candidate.identifier)
                candidate.rssi?.let { append(" rssi=$it") }
                if (candidate.advertisesHeartRateService) append(" services=[0x180D]")
            }
            setAllCaps(false)
            setOnClickListener {
                val selected = provider.selectDevice(candidate.identifier)
                appendLog("Selected ${selected?.displayName ?: candidate.displayName}")
                appendLog("Preference boundary: save identifier/display name only; no GATT or SDK model.")
                provider.connectSelectedDevice()
            }
        }
        candidateButtons[candidate.identifier] = button
        deviceList.addView(button)
    }

    private fun requestBluetoothPermissions() {
        if (!BleHeartRatePermissionPlanner.shouldRequestPermissions(
                BleHeartRatePermissionTrigger.EXPLICIT_USER_ACTION
            )
        ) {
            appendLog("Permission request blocked: explicit user action is required.")
            return
        }
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
        return BleHeartRatePermissionPlanner.requiredPermissions()
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

    private fun BleHeartRateProviderState.logLine(): String {
        return buildString {
            append(kind.name.lowercase(Locale.US))
            append(": ")
            append(message)
            if (missingPermissions.isNotEmpty()) {
                append(" missing=")
                append(missingPermissions.joinToString())
            }
            selectedDevice?.let {
                append(" selected=")
                append(it.displayName)
                append(" ")
                append(it.identifier)
            }
            candidate?.let {
                append(" candidate=")
                append(it.displayName)
                append(" ")
                append(it.identifier)
            }
            bpm?.let {
                append(" bpm=")
                append(it)
            }
            recoverableReason?.let {
                append(" recoverable=")
                append(it.name.lowercase(Locale.US))
            }
        }
    }

    companion object {
        private const val REQUEST_BLUETOOTH = 8102
    }
}
