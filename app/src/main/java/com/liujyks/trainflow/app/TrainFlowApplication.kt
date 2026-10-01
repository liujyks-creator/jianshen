package com.liujyks.trainflow.app

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.liujyks.trainflow.core.data.WorkoutSessionExportFiles
import com.liujyks.trainflow.core.data.WorkoutSessionRepository
import com.liujyks.trainflow.core.data.WorkoutPlanRepository
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.datastore.TrainFlowPreferences
import com.liujyks.trainflow.core.datastore.TrainFlowPreferencesDataSource
import com.liujyks.trainflow.core.datastore.trainFlowPreferencesDataStore
import com.liujyks.trainflow.core.health.HeartRateRecoveryEligibilityInput
import com.liujyks.trainflow.core.health.HeartRateRuntimeAction
import com.liujyks.trainflow.core.health.HeartRateRuntimeOwner
import com.liujyks.trainflow.core.model.HeartRateFact
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationController
import com.liujyks.trainflow.core.notifications.AndroidActiveWorkoutNotificationController
import com.liujyks.trainflow.core.notifications.ActiveWorkoutForegroundPhase
import com.liujyks.trainflow.core.notifications.clearLegacyPlanReminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class TrainFlowApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    internal lateinit var heartRateRuntimeOwner: HeartRateRuntimeOwner
        private set

    internal lateinit var preferencesDataSource: TrainFlowPreferencesDataSource
        private set

    internal lateinit var trainFlowDatabase: TrainFlowDatabase
        private set

    internal lateinit var workoutSessionRepository: WorkoutSessionRepository
        private set

    internal lateinit var workoutSessionExportFiles: WorkoutSessionExportFiles
        private set

    internal lateinit var activeWorkoutNotifications: ActiveWorkoutNotificationController
        private set

    private var latestPreferences = TrainFlowPreferences()
    private var visibilityFact = ProcessVisibilityFact.UNKNOWN
    private val mutableProcessVisibility = MutableStateFlow(ProcessVisibilityFact.UNKNOWN)
    internal val processVisibility: StateFlow<ProcessVisibilityFact> = mutableProcessVisibility

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                refreshHeartRateEnvironment()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        trainFlowDatabase = TrainFlowDatabase.create(this)
        workoutSessionRepository = WorkoutSessionRepository(trainFlowDatabase)
        workoutSessionExportFiles = WorkoutSessionExportFiles(
            filesDir, workoutSessionRepository, applicationScope, Instant.now()
        )
        applicationScope.launch {
            workoutSessionExportFiles.startupCleanup.await().failures.forEach {
                Log.w("SessionExport", "Startup cleanup failed for ${it.file}", it.cause)
            }
        }
        activeWorkoutNotifications = AndroidActiveWorkoutNotificationController(applicationContext)
        activeWorkoutNotifications.initialize()
        applicationScope.launch {
            clearLegacyPlanReminders(applicationContext, WorkoutPlanRepository(trainFlowDatabase))
        }
        heartRateRuntimeOwner = HeartRateRuntimeOwner(this)
        preferencesDataSource = TrainFlowPreferencesDataSource(trainFlowPreferencesDataStore)
        applicationScope.launch {
            activeWorkoutNotifications.currentTraining.collectLatest { applyHeartRateContext() }
        }
        applicationScope.launch {
            activeWorkoutNotifications.foregroundState.collectLatest { applyHeartRateContext() }
        }
        applicationScope.launch {
            heartRateRuntimeOwner.heartRateState.collectLatest { refreshForegroundContent() }
        }
        applicationScope.launch {
            heartRateRuntimeOwner.recoveryState.collectLatest { refreshForegroundContent() }
        }
        ProcessVisibilityTracker(this) { fact ->
            visibilityFact = fact
            mutableProcessVisibility.value = fact
            when (fact) {
                ProcessVisibilityFact.VISIBLE -> applyHeartRateContext()
                ProcessVisibilityFact.BACKGROUND,
                ProcessVisibilityFact.UNKNOWN -> applyHeartRateContext()
                ProcessVisibilityFact.CONFIGURATION_TRANSITION -> Unit
            }
        }
        registerBluetoothStateReceiver()
        applicationScope.launch {
            preferencesDataSource.preferences.collectLatest { preferences ->
                latestPreferences = preferences
                applyHeartRateContext()
            }
        }
    }

    internal fun refreshHeartRateEnvironment() {
        applyHeartRateContext()
    }

    internal fun startManualHeartRateScan() {
        heartRateRuntimeOwner.submit(HeartRateRuntimeAction.StartScan)
    }

    internal suspend fun changeHeartRateDevice() {
        preferencesDataSource.setHeartRateManualSuppressed(false)
        latestPreferences = latestPreferences.copy(heartRateManualSuppressed = false)
        applyHeartRateContext()
        heartRateRuntimeOwner.submit(HeartRateRuntimeAction.StartScan)
    }

    internal fun stopManualHeartRateScan() {
        heartRateRuntimeOwner.submit(HeartRateRuntimeAction.StopScan)
    }

    internal suspend fun setHeartRateEnabled(enabled: Boolean) {
        preferencesDataSource.setHeartRateDisplayEnabled(enabled)
        latestPreferences = latestPreferences.copy(heartRateDisplayEnabled = enabled)
        applyHeartRateContext()
    }

    internal suspend fun selectHeartRateDevice(identifier: String, displayName: String) {
        preferencesDataSource.setBleHeartRateDevicePreference(identifier, displayName)
        latestPreferences = latestPreferences.copy(
            bleHeartRateDeviceIdentifier = identifier,
            bleHeartRateDeviceDisplayName = displayName,
            heartRateManualSuppressed = false
        )
        applyHeartRateContext()
        heartRateRuntimeOwner.submit(HeartRateRuntimeAction.Connect(identifier))
    }

    internal suspend fun disconnectHeartRateDevice() {
        preferencesDataSource.setHeartRateManualSuppressed(true)
        latestPreferences = latestPreferences.copy(heartRateManualSuppressed = true)
        applyHeartRateContext()
        heartRateRuntimeOwner.submit(HeartRateRuntimeAction.Disconnect)
    }

    internal suspend fun reconnectHeartRateDevice() {
        preferencesDataSource.setHeartRateManualSuppressed(false)
        latestPreferences = latestPreferences.copy(heartRateManualSuppressed = false)
        applyHeartRateContext()
    }

    internal suspend fun clearHeartRateDevice() {
        preferencesDataSource.clearBleHeartRateDevicePreference()
        latestPreferences = preferencesDataSource.preferences.first()
        applyHeartRateContext()
    }

    internal suspend fun setHeartRatePersonalParameters(
        ageYears: Int?,
        personalMaxHeartRateBpm: Int?,
        alertThresholdBpm: Int?
    ) {
        preferencesDataSource.setHeartRatePersonalParameters(
            ageYears = ageYears,
            personalMaxHeartRateBpm = personalMaxHeartRateBpm,
            alertThresholdBpm = alertThresholdBpm
        )
        latestPreferences = latestPreferences.copy(
            heartRateAgeYears = TrainFlowPreferences.sanitizeHeartRateAgeYears(ageYears),
            heartRatePersonalMaxBpm =
                TrainFlowPreferences.sanitizePersonalHeartRateBpm(personalMaxHeartRateBpm),
            heartRateAlertThresholdBpm =
                TrainFlowPreferences.sanitizePersonalHeartRateBpm(alertThresholdBpm)
        )
    }

    private fun applyHeartRateContext() {
        if (!::heartRateRuntimeOwner.isInitialized) return
        val activeTrainingFgsActive = refreshForegroundContent()
        if (latestPreferences.heartRateDisplayEnabled) {
            heartRateRuntimeOwner.submit(HeartRateRuntimeAction.Enable)
        } else {
            heartRateRuntimeOwner.submit(HeartRateRuntimeAction.Disable)
        }
        heartRateRuntimeOwner.submit(
            HeartRateRuntimeAction.UpdateRecoveryEligibility(
                HeartRateRecoveryEligibilityInput(
                    optedIn = latestPreferences.heartRateDisplayEnabled,
                    savedTargetIdentifier = latestPreferences.bleHeartRateDeviceIdentifier,
                    permissionGranted = hasHeartRatePermissions(),
                    bluetoothEnabled = isBluetoothEnabled(),
                    manuallySuppressed = latestPreferences.heartRateManualSuppressed,
                    appVisible = visibilityFact == ProcessVisibilityFact.VISIBLE,
                    activeTrainingFgsActive = activeTrainingFgsActive
                )
            )
        )
    }

    private fun refreshForegroundContent(): Boolean {
        if (!::heartRateRuntimeOwner.isInitialized) return false
        val training = activeWorkoutNotifications.currentTraining.value
        val permissions = hasHeartRatePermissions()
        val bluetooth = isBluetoothEnabled()
        val eligible = training != null && latestPreferences.heartRateDisplayEnabled &&
            !latestPreferences.bleHeartRateDeviceIdentifier.isNullOrBlank() &&
            !latestPreferences.heartRateManualSuppressed && permissions && bluetooth
        val visible = visibilityFact == ProcessVisibilityFact.VISIBLE
        val foreground = activeWorkoutNotifications.foregroundState.value
        val desired = eligible && (
            visible || foreground.active ||
                foreground.phase == ActiveWorkoutForegroundPhase.STARTING
            )
        val heartRate = heartRateRuntimeOwner.heartRateState.value
        val text = when (heartRate.fact) {
            HeartRateFact.LIVE -> "心率 ${requireNotNull(heartRate.bpm)} bpm"
            HeartRateFact.LINK_DISCONNECTED,
            HeartRateFact.SCANNING,
            HeartRateFact.CONNECTING -> "正在重新连接"
            HeartRateFact.WAITING_FIRST_DATA,
            HeartRateFact.DATA_INTERRUPTED -> "等待心率数据"
            else -> "心率暂不可用"
        }
        activeWorkoutNotifications.setForegroundDesired(desired, visible, text)
        return desired && activeWorkoutNotifications.foregroundState.value.active
    }

    private fun hasHeartRatePermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return permissions.all { permission ->
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        return try {
            getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun registerBluetoothStateReceiver() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(bluetoothStateReceiver, filter)
        }
    }
}
