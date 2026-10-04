package com.liujyks.trainflow.app

import android.os.Bundle
import android.content.Intent
import android.content.ClipData
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.liujyks.trainflow.feature.history.WorkoutSessionExportViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import com.liujyks.trainflow.core.data.WorkoutPlanRepository
import com.liujyks.trainflow.core.datastore.TrainFlowPreferences
import com.liujyks.trainflow.core.datastore.TrainFlowPreferencesDataSource
import com.liujyks.trainflow.ui.shell.official.TrainFlowApp
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
import com.liujyks.trainflow.ui.theme.rememberTrainFlowReduceMotion
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    internal lateinit var workoutSessionExportViewModel: WorkoutSessionExportViewModel
        private set
    private val exportDirectoryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        workoutSessionExportViewModel.selectExternalTarget(uri)
    }
    override fun onResume() {
        super.onResume()
        (application as TrainFlowApplication).activeWorkoutNotifications.refreshPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workoutSessionExportViewModel = ViewModelProvider(this, WorkoutSessionExportViewModel.Factory(
            (application as TrainFlowApplication).workoutSessionRepository,
            (application as TrainFlowApplication).workoutSessionExportFiles,
            application.contentResolver))[WorkoutSessionExportViewModel::class.java]
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(workoutSessionExportViewModel.delivery.pendingShare) {
                val export = workoutSessionExportViewModel.claimPendingShare() ?: return@LaunchedEffect
                val cause = try {
                    val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.sessionexports", export.file)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri("TrainFlow 导出", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(send, "分享训练记录"))
                    null
                } catch (failure: Exception) { failure }
                workoutSessionExportViewModel.reportShareLaunchResult(export.operationId, cause)
            }
            val trainFlowApplication = application as TrainFlowApplication
            val preferencesDataSource: TrainFlowPreferencesDataSource =
                trainFlowApplication.preferencesDataSource
            val trainFlowDatabase = trainFlowApplication.trainFlowDatabase
            val workoutPlanRepository = remember(trainFlowDatabase) {
                WorkoutPlanRepository(trainFlowDatabase)
            }
            val workoutSessionRepository = trainFlowApplication.workoutSessionRepository
            val activeWorkoutNotifications = trainFlowApplication.activeWorkoutNotifications
            val notificationPermissionState by activeWorkoutNotifications.permissionState.collectAsState()
            val preferences by preferencesDataSource.preferences.collectAsState(
                initial = TrainFlowPreferences()
            )
            val heartRateState by
                trainFlowApplication.heartRateRuntimeOwner.heartRateState.collectAsState()
            val heartRateScanState by
                trainFlowApplication.heartRateRuntimeOwner.scanState.collectAsState()
            val heartRateCandidates by
                trainFlowApplication.heartRateRuntimeOwner.candidates.collectAsState()
            val heartRateRecoveryState by
                trainFlowApplication.heartRateRuntimeOwner.recoveryState.collectAsState()
            val processVisibility by
                trainFlowApplication.processVisibility.collectAsState()
            val reduceMotion = rememberTrainFlowReduceMotion()
            val workoutPlans by workoutPlanRepository.plans.collectAsState(
                initial = emptyList()
            )
            val historyEntries by workoutSessionRepository.historyEntries.collectAsState(
                initial = emptyList()
            )
            val scope = rememberCoroutineScope()

            TrainFlowTheme(
                skin = preferences.toTrainFlowSkin(),
                reduceMotion = reduceMotion
            ) {
                TrainFlowApp(
                    activeWorkoutNotifications = activeWorkoutNotifications,
                    notificationPermissionState = notificationPermissionState,
                    onOpenNotificationSettings = {
                        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        })
                    },
                    workoutSessionRepository = workoutSessionRepository,
                    workoutSessionExportViewModel = workoutSessionExportViewModel,
                    onChooseExportDirectory = { exportDirectoryLauncher.launch(null) },
                    heartRateRuntimeOwner = trainFlowApplication.heartRateRuntimeOwner,
                    workoutPlans = workoutPlans,
                    historyEntries = historyEntries,
                    trainingPreferencesState = preferences.toTrainingPreferencesScreenState(),
                    heartRateState = heartRateState,
                    heartRateScanState = heartRateScanState,
                    heartRateDeviceCandidates = heartRateCandidates,
                    heartRateRecoveryState = heartRateRecoveryState,
                    appVisible = processVisibility == ProcessVisibilityFact.VISIBLE,
                    planEditorDefaults = preferences.toPlanEditorDefaults(),
                    onSaveWorkoutPlan = { plan ->
                        scope.launch {
                            workoutPlanRepository.upsertPlan(plan)
                        }
                    },
                    onDeleteWorkoutPlan = { planId ->
                        scope.launch {
                            workoutPlanRepository.deletePlan(planId)
                        }
                    },
                    onRecordWorkoutSession = { session ->
                        workoutSessionRepository.upsertSession(session)
                    },
                    onDefaultCountdownThresholdChanged = { seconds ->
                        scope.launch {
                            preferencesDataSource.setDefaultCountdownThresholdSec(seconds)
                        }
                    },
                    onActionCueEnabledChanged = { enabled ->
                        scope.launch {
                            preferencesDataSource.setActionCueEnabled(enabled)
                        }
                    },
                    onRestCueEnabledChanged = { enabled ->
                        scope.launch {
                            preferencesDataSource.setRestCueEnabled(enabled)
                        }
                    },
                    onSoundEnabledChanged = { enabled ->
                        scope.launch {
                            preferencesDataSource.setSoundEnabled(enabled)
                        }
                    },
                    onVibrationEnabledChanged = { enabled ->
                        scope.launch {
                            preferencesDataSource.setVibrationEnabled(enabled)
                        }
                    },
                    onEmphasisAnimationEnabledChanged = { enabled ->
                        scope.launch {
                            preferencesDataSource.setEmphasisAnimationEnabled(enabled)
                        }
                    },
                    onStrengthSetTimerModeChanged = { mode ->
                        scope.launch {
                            preferencesDataSource.setStrengthSetTimerMode(mode.contractValue)
                        }
                    },
                    onHeartRateDisplayEnabledChanged = { enabled ->
                        scope.launch {
                            trainFlowApplication.setHeartRateEnabled(enabled)
                        }
                    },
                    onSaveHeartRateDevicePreference = { identifier, displayName ->
                        scope.launch {
                            trainFlowApplication.selectHeartRateDevice(identifier, displayName)
                        }
                    },
                    onStartHeartRateDeviceScan = {
                        trainFlowApplication.startManualHeartRateScan()
                    },
                    onChangeHeartRateDevice = {
                        scope.launch { trainFlowApplication.changeHeartRateDevice() }
                    },
                    onStopHeartRateDeviceScan = {
                        trainFlowApplication.stopManualHeartRateScan()
                    },
                    onDisconnectHeartRateDevice = {
                        scope.launch { trainFlowApplication.disconnectHeartRateDevice() }
                    },
                    onReconnectHeartRateDevice = {
                        scope.launch { trainFlowApplication.reconnectHeartRateDevice() }
                    },
                    onClearHeartRateDevicePreference = {
                        scope.launch {
                            trainFlowApplication.clearHeartRateDevice()
                        }
                    },
                    onHeartRatePersonalParametersChanged = { age, personalMax, alert ->
                        scope.launch {
                            trainFlowApplication.setHeartRatePersonalParameters(
                                ageYears = age,
                                personalMaxHeartRateBpm = personalMax,
                                alertThresholdBpm = alert
                            )
                        }
                    },
                    onHeartRateEnvironmentChanged = {
                        trainFlowApplication.refreshHeartRateEnvironment()
                    },
                    onUiSkinChanged = { skinId ->
                        scope.launch {
                            preferencesDataSource.setUiSkinId(skinId)
                        }
                    }
                )
            }
        }
    }
}
