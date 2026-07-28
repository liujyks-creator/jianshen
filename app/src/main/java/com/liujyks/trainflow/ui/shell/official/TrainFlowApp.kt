package com.liujyks.trainflow.ui.shell.official

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendation
import com.liujyks.trainflow.core.health.BleHeartRatePermissionPlanner
import com.liujyks.trainflow.core.health.BleHeartRatePermissionTrigger
import com.liujyks.trainflow.core.health.BleHeartRateDeviceCandidate
import com.liujyks.trainflow.core.health.BleHeartRateScanState
import com.liujyks.trainflow.core.health.BleHeartRateScanStateKind
import com.liujyks.trainflow.core.health.HeartRateRecoveryState
import com.liujyks.trainflow.core.health.HeartRateRecoveryPhase
import com.liujyks.trainflow.core.health.HeartRateRecoveryStopReason
import com.liujyks.trainflow.core.health.HeartRateRuntimeFact
import com.liujyks.trainflow.core.health.toHeartRateState
import com.liujyks.trainflow.core.model.WorkoutSession
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateFact
import com.liujyks.trainflow.core.notifications.AndroidPlanReminderScheduler
import com.liujyks.trainflow.core.notifications.resolvePlanReminderPermissionState
import com.liujyks.trainflow.feature.exerciselibrary.ExerciseLibraryRoute
import com.liujyks.trainflow.feature.followalong.FollowAlongRoute
import com.liujyks.trainflow.feature.history.HistoryRoute
import com.liujyks.trainflow.feature.home.HomeRoute
import com.liujyks.trainflow.feature.plans.PlanEditorDefaults
import com.liujyks.trainflow.feature.plans.PlanManagementRoute
import com.liujyks.trainflow.feature.plans.PlanManagementScreenState
import com.liujyks.trainflow.feature.plans.StrengthPlanEditorRoute
import com.liujyks.trainflow.feature.plans.TimedPlanEditorRoute
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import com.liujyks.trainflow.feature.plans.dispatchPlanReminderReplacementForEditedPlan
import com.liujyks.trainflow.feature.plans.upsertPlan
import com.liujyks.trainflow.feature.plans.withPlans
import com.liujyks.trainflow.feature.recovery.RecoveryRoute
import com.liujyks.trainflow.feature.recovery.emptyRecoveryScreenState
import com.liujyks.trainflow.feature.recovery.toRecoveryScreenState
import com.liujyks.trainflow.feature.settings.SettingsRoute
import com.liujyks.trainflow.feature.settings.HeartRateBlePermissionStatus
import com.liujyks.trainflow.feature.settings.HeartRateDeviceScanPurpose
import com.liujyks.trainflow.feature.settings.StrengthSetTimerModePreference
import com.liujyks.trainflow.feature.settings.TrainingPreferencesScreenState
import com.liujyks.trainflow.feature.settings.defaultTrainingPreferencesScreenState
import com.liujyks.trainflow.feature.settings.heartRateDevicePickerUiState
import com.liujyks.trainflow.feature.settings.heartRateSettingsUiState
import com.liujyks.trainflow.feature.settings.prepareBlePermissionRationale
import com.liujyks.trainflow.feature.settings.resolveHeartRateBlePermissionStatus
import com.liujyks.trainflow.feature.settings.savedDeviceReconnectCandidateIdentifier
import com.liujyks.trainflow.feature.workoutsession.FollowAlongWorkoutSessionRoute
import com.liujyks.trainflow.feature.workoutsession.StrengthWorkoutSessionRoute
import com.liujyks.trainflow.feature.workoutsession.TimedWorkoutSessionRoute
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary

@Composable
internal fun TrainFlowApp(
    workoutPlans: List<WorkoutPlan> = buildDefaultPlanManagementState().plans,
    workoutSessions: List<WorkoutSession> = emptyList(),
    trainingPreferencesState: TrainingPreferencesScreenState = defaultTrainingPreferencesScreenState(),
    heartRateState: HeartRateState = HeartRateRuntimeFact.Disabled.toHeartRateState(),
    heartRateScanState: BleHeartRateScanState = BleHeartRateScanState.idle(),
    heartRateDeviceCandidates: List<BleHeartRateDeviceCandidate> = emptyList(),
    heartRateRecoveryState: HeartRateRecoveryState = HeartRateRecoveryState.disarmed(
        HeartRateRecoveryStopReason.OPTED_OUT
    ),
    appVisible: Boolean = false,
    planEditorDefaults: PlanEditorDefaults = PlanEditorDefaults(),
    onSaveWorkoutPlan: (WorkoutPlan) -> Unit = {},
    onDeleteWorkoutPlan: (String) -> Unit = {},
    onRecordWorkoutSession: suspend (WorkoutSession) -> Unit = {},
    onClearAllWorkoutSessions: () -> Unit = {},
    onClearWorkoutSessionsForPlan: (String) -> Unit = {},
    onClearWorkoutSessionsStartedOnDate: (String) -> Unit = {},
    onDefaultCountdownThresholdChanged: (Int) -> Unit = {},
    onActionCueEnabledChanged: (Boolean) -> Unit = {},
    onRestCueEnabledChanged: (Boolean) -> Unit = {},
    onSoundEnabledChanged: (Boolean) -> Unit = {},
    onVibrationEnabledChanged: (Boolean) -> Unit = {},
    onEmphasisAnimationEnabledChanged: (Boolean) -> Unit = {},
    onStrengthSetTimerModeChanged: (StrengthSetTimerModePreference) -> Unit = {},
    onHeartRateDisplayEnabledChanged: (Boolean) -> Unit = {},
    onSaveHeartRateDevicePreference: (String, String) -> Unit = { _, _ -> },
    onStartHeartRateDeviceScan: () -> Unit = {},
    onChangeHeartRateDevice: () -> Unit = {},
    onStopHeartRateDeviceScan: () -> Unit = {},
    onDisconnectHeartRateDevice: () -> Unit = {},
    onReconnectHeartRateDevice: () -> Unit = {},
    onClearHeartRateDevicePreference: () -> Unit = {},
    onHeartRatePersonalParametersChanged: (Int?, Int?, Int?) -> Unit = { _, _, _ -> },
    onHeartRateEnvironmentChanged: () -> Unit = {},
    onUiSkinChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val planReminderScheduler = remember(context) {
        AndroidPlanReminderScheduler(context.applicationContext)
    }
    var heartRateBlePermissionStatus by rememberSaveable {
        mutableStateOf(HeartRateBlePermissionStatus.NOT_REQUESTED)
    }
    var heartRatePermissionRefreshKey by rememberSaveable {
        mutableStateOf(0)
    }
    var pendingHeartRatePermissionAction by rememberSaveable {
        mutableStateOf(PendingHeartRatePermissionAction.NONE)
    }
    var heartRateScanFinishedWithoutDevices by rememberSaveable {
        mutableStateOf(false)
    }
    var heartRateScanPurpose by rememberSaveable {
        mutableStateOf(HeartRateDeviceScanPurpose.NONE)
    }
    var lastCompletedHeartRateScanPurpose by rememberSaveable {
        mutableStateOf(HeartRateDeviceScanPurpose.NONE)
    }
    var heartRateSettingsFocusRequestKey by rememberSaveable {
        mutableStateOf(0)
    }
    val heartRateDisplayEnabled = trainingPreferencesState.heartRateSettings.enabled
    val allHeartRateBlePermissionsGranted = heartRatePermissionRefreshKey.let {
        context.arePermissionsGranted(BleHeartRatePermissionPlanner.requiredPermissions())
    }
    val resolvedHeartRateBlePermissionStatus = resolveHeartRateBlePermissionStatus(
        displayEnabled = heartRateDisplayEnabled,
        allPermissionsGranted = allHeartRateBlePermissionsGranted,
        requestResult = heartRateBlePermissionStatus
    )
    val preferenceHeartRateState = trainingPreferencesState.heartRateSettings
    val settingsState = trainingPreferencesState.copy(
        heartRateSettings = heartRateSettingsUiState(
            enabled = heartRateDisplayEnabled,
            savedDeviceIdentifier = preferenceHeartRateState.savedDeviceIdentifier,
            savedDeviceDisplayName = preferenceHeartRateState.savedDeviceDisplayName,
            manualSuppressed = preferenceHeartRateState.manualSuppressed,
            ageYears = preferenceHeartRateState.ageYears,
            personalMaxHeartRateBpm = preferenceHeartRateState.personalMaxHeartRateBpm,
            alertThresholdBpm = preferenceHeartRateState.alertThresholdBpm,
            appVisible = appVisible,
            blePermissionStatus = resolvedHeartRateBlePermissionStatus,
            heartRateState = heartRateState,
            recoveryState = heartRateRecoveryState,
            scanState = heartRateScanState,
            scannerCandidates = heartRateDeviceCandidates,
            scanFinishedWithoutDevices = heartRateScanFinishedWithoutDevices,
            scanPurpose = heartRateScanPurpose,
            lastCompletedScanPurpose = lastCompletedHeartRateScanPurpose
        )
    )
    val heartRateBlePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val requiredPermissions = BleHeartRatePermissionPlanner.requiredPermissions()
        val resolvedStatus = resolveHeartRateBlePermissionRequestResult(
            requiredPermissions = requiredPermissions,
            requestResult = result,
            allPermissionsCurrentlyGranted = context.arePermissionsGranted(requiredPermissions),
            hasPermanentlyDeniedPermissions = context.hasPermanentlyDeniedPermissions(
                permissions = requiredPermissions,
                requestResult = result
            )
        )
        heartRateBlePermissionStatus = resolvedStatus
        heartRatePermissionRefreshKey += 1
        onHeartRateEnvironmentChanged()
        if (shouldResumeHeartRatePermissionAction(
                pendingAction = pendingHeartRatePermissionAction,
                permissionStatus = resolvedStatus
            )
        ) {
            onReconnectHeartRateDevice()
        }
        pendingHeartRatePermissionAction = PendingHeartRatePermissionAction.NONE
    }
    var currentDestination by rememberSaveable {
        mutableStateOf(OfficialShellDestination.TRAINING)
    }
    var planManagementState by remember {
        mutableStateOf(PlanManagementScreenState(plans = workoutPlans))
    }
    var editingPlanId by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var activeTimedSessionPlan by remember {
        mutableStateOf<WorkoutPlan?>(null)
    }
    var activeStrengthSessionPlan by remember {
        mutableStateOf<WorkoutPlan?>(null)
    }
    var activeFollowAlongSessionPlan by remember {
        mutableStateOf<WorkoutPlan?>(null)
    }
    var activeRecoveryRecommendation by remember {
        mutableStateOf<BasicRecoveryRecommendation?>(null)
    }
    val shellState = OfficialShellState(
        currentDestination = currentDestination,
        planManagementState = planManagementState,
        editingPlanId = editingPlanId,
        activeTimedSessionPlan = activeTimedSessionPlan,
        activeStrengthSessionPlan = activeStrengthSessionPlan,
        activeFollowAlongSessionPlan = activeFollowAlongSessionPlan,
        activeRecoveryRecommendation = activeRecoveryRecommendation
    )

    LaunchedEffect(workoutPlans) {
        planManagementState = planManagementState.withPlans(workoutPlans)
    }

    LaunchedEffect(heartRateDisplayEnabled, allHeartRateBlePermissionsGranted) {
        heartRateBlePermissionStatus = when {
            !heartRateDisplayEnabled -> HeartRateBlePermissionStatus.NOT_REQUESTED
            allHeartRateBlePermissionsGranted -> HeartRateBlePermissionStatus.GRANTED
            heartRateBlePermissionStatus == HeartRateBlePermissionStatus.GRANTED ->
                HeartRateBlePermissionStatus.NOT_REQUESTED
            else -> heartRateBlePermissionStatus
        }
        pendingHeartRatePermissionAction = pendingHeartRatePermissionActionAfterDisplayChange(
            displayEnabled = heartRateDisplayEnabled,
            pendingAction = pendingHeartRatePermissionAction
        )
    }

    LaunchedEffect(heartRateDisplayEnabled, allHeartRateBlePermissionsGranted) {
        onHeartRateEnvironmentChanged()
    }

    LaunchedEffect(
        heartRateScanState.kind,
        heartRateDeviceCandidates,
        heartRateScanPurpose,
        trainingPreferencesState.heartRateSettings.savedDeviceIdentifier
    ) {
        if (
            shouldConsumeManualSavedDeviceScanMatch(
                scanPurpose = heartRateScanPurpose,
                recoveryPhase = heartRateRecoveryState.phase
            ) &&
            heartRateScanState.kind == BleHeartRateScanStateKind.SCANNING
        ) {
            val matchedIdentifier = savedDeviceReconnectCandidateIdentifier(
                savedDeviceIdentifier = trainingPreferencesState.heartRateSettings.savedDeviceIdentifier,
                candidates = heartRateDeviceCandidates
            )
            if (matchedIdentifier != null) {
                val matchedCandidate = heartRateDeviceCandidates.first { candidate ->
                    candidate.identifier == matchedIdentifier
                }
                heartRateScanPurpose = HeartRateDeviceScanPurpose.NONE
                onSaveHeartRateDevicePreference(
                    matchedIdentifier,
                    matchedCandidate.displayName
                )
                return@LaunchedEffect
            }
        }
        val heartRateCandidateCount = heartRateDeviceCandidates.count { candidate ->
            candidate.advertisesHeartRateService
        }
        when (heartRateScanState.kind) {
            BleHeartRateScanStateKind.SCANNING -> {
                heartRateScanFinishedWithoutDevices = false
                if (heartRateScanPurpose == HeartRateDeviceScanPurpose.NONE) {
                    lastCompletedHeartRateScanPurpose = HeartRateDeviceScanPurpose.NONE
                }
            }

            BleHeartRateScanStateKind.STOPPED -> {
                if (heartRateScanPurpose != HeartRateDeviceScanPurpose.NONE) {
                    lastCompletedHeartRateScanPurpose = heartRateScanPurpose
                    heartRateScanFinishedWithoutDevices = heartRateCandidateCount == 0
                }
                heartRateScanPurpose = HeartRateDeviceScanPurpose.NONE
            }

            BleHeartRateScanStateKind.ERROR,
            -> {
                heartRateScanPurpose = HeartRateDeviceScanPurpose.NONE
            }

            BleHeartRateScanStateKind.IDLE -> Unit
        }
    }

    LaunchedEffect(
        heartRateDisplayEnabled,
        appVisible,
        heartRateState.fact
    ) {
        if (
            shouldInvalidateHeartRateScanIntent(
                displayEnabled = heartRateDisplayEnabled,
                appVisible = appVisible,
                fact = heartRateState.fact
            )
        ) {
            heartRateScanPurpose = HeartRateDeviceScanPurpose.NONE
            heartRateScanFinishedWithoutDevices = false
        }
    }

    LaunchedEffect(currentDestination) {
        if (
            currentDestination != OfficialShellDestination.SETTINGS &&
            heartRateScanPurpose != HeartRateDeviceScanPurpose.NONE
        ) {
            onStopHeartRateDeviceScan()
            heartRateScanPurpose = HeartRateDeviceScanPurpose.NONE
        }
    }

    LaunchedEffect(
        heartRateDisplayEnabled,
        allHeartRateBlePermissionsGranted,
        pendingHeartRatePermissionAction
    ) {
        if (
            heartRateDisplayEnabled &&
            allHeartRateBlePermissionsGranted &&
            pendingHeartRatePermissionAction == PendingHeartRatePermissionAction.RECONNECT
        ) {
            pendingHeartRatePermissionAction = PendingHeartRatePermissionAction.NONE
            onHeartRateEnvironmentChanged()
            onReconnectHeartRateDevice()
        }
    }

    fun applyShellState(nextState: OfficialShellState) {
        currentDestination = nextState.currentDestination
        planManagementState = nextState.planManagementState
        editingPlanId = nextState.editingPlanId
        activeTimedSessionPlan = nextState.activeTimedSessionPlan
        activeStrengthSessionPlan = nextState.activeStrengthSessionPlan
        activeFollowAlongSessionPlan = nextState.activeFollowAlongSessionPlan
        activeRecoveryRecommendation = nextState.activeRecoveryRecommendation
    }

    fun refreshEditedPlanReminder(plan: WorkoutPlan) {
        dispatchPlanReminderReplacementForEditedPlan(
            plan = plan,
            wasEditingExistingPlan = shellState.editingPlanId == plan.id,
            permissionState = context.resolvePlanReminderPermissionState(),
            scheduler = planReminderScheduler
        )
    }

    Surface {
        Scaffold(
            bottomBar = {
                if (shellState.showBottomBar) {
                    OfficialBottomBar(
                        currentDestination = shellState.currentDestination,
                        onDestinationSelected = { destination ->
                            applyShellState(shellState.selectDestination(destination))
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (shellState.currentDestination) {
                    OfficialShellDestination.TRAINING -> HomeRoute(
                        onOpenExerciseLibrary = {
                            applyShellState(shellState.selectDestination(OfficialShellDestination.EXERCISE_LIBRARY))
                        },
                        onOpenTimedPlanEditor = {
                            applyShellState(shellState.openTimedPlanEditorForCreate())
                        },
                        onOpenStrengthPlanEditor = {
                            applyShellState(shellState.openStrengthPlanEditorForCreate())
                        },
                        onOpenFollowAlong = {
                            applyShellState(shellState.selectDestination(OfficialShellDestination.FOLLOW_ALONG_ENTRY))
                        },
                        onOpenSettings = {
                            applyShellState(shellState.selectDestination(OfficialShellDestination.SETTINGS))
                        },
                        onOpenPlans = {
                            applyShellState(shellState.selectDestination(OfficialShellDestination.PLANS))
                        },
                        onOpenRecords = {
                            applyShellState(shellState.selectDestination(OfficialShellDestination.RECORDS))
                        },
                        modifier = Modifier.padding(innerPadding)
                    )

                    OfficialShellDestination.TIMED_PLAN_EDITOR -> TimedPlanEditorRoute(
                    onBackToHome = {
                        applyShellState(
                            shellState
                                .selectDestination(OfficialShellDestination.TRAINING)
                                .copy(editingPlanId = null)
                        )
                    },
                    onStartTimedPlan = { plan ->
                        applyShellState(shellState.startTimedSession(plan))
                    },
                    onSaveTimedPlan = { plan ->
                        onSaveWorkoutPlan(plan)
                        refreshEditedPlanReminder(plan)
                        val nextPlanManagementState = shellState.planManagementState.upsertPlan(plan)
                        applyShellState(
                            shellState.finishPlanEdit(nextPlanManagementState)
                        )
                    },
                    planEditorDefaults = planEditorDefaults,
                    initialPlan = shellState.planManagementState.plans.firstOrNull { plan ->
                        plan.id == shellState.editingPlanId
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                    OfficialShellDestination.STRENGTH_PLAN_EDITOR -> StrengthPlanEditorRoute(
                    onBackToHome = {
                        applyShellState(
                            shellState
                                .selectDestination(OfficialShellDestination.TRAINING)
                                .copy(editingPlanId = null)
                        )
                    },
                    onStartStrengthPlan = { plan ->
                        applyShellState(shellState.startStrengthSession(plan))
                    },
                    onSaveStrengthPlan = { plan ->
                        onSaveWorkoutPlan(plan)
                        refreshEditedPlanReminder(plan)
                        val nextPlanManagementState = shellState.planManagementState.upsertPlan(plan)
                        applyShellState(
                            shellState.finishPlanEdit(nextPlanManagementState)
                        )
                    },
                    planEditorDefaults = planEditorDefaults,
                    initialPlan = shellState.planManagementState.plans.firstOrNull { plan ->
                        plan.id == shellState.editingPlanId
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                    OfficialShellDestination.SETTINGS -> SettingsRoute(
                    uiState = settingsState,
                    onBackToTraining = {
                        applyShellState(shellState.returnFromSettings())
                    },
                    onDefaultCountdownThresholdChanged = onDefaultCountdownThresholdChanged,
                    onActionCueEnabledChanged = onActionCueEnabledChanged,
                    onRestCueEnabledChanged = onRestCueEnabledChanged,
                    onSoundEnabledChanged = onSoundEnabledChanged,
                    onVibrationEnabledChanged = onVibrationEnabledChanged,
                    onEmphasisAnimationEnabledChanged = onEmphasisAnimationEnabledChanged,
                    onStrengthSetTimerModeChanged = onStrengthSetTimerModeChanged,
                    onHeartRateDisplayEnabledChanged = { enabled ->
                        if (!enabled) {
                            pendingHeartRatePermissionAction =
                                PendingHeartRatePermissionAction.NONE
                            heartRateScanPurpose = HeartRateDeviceScanPurpose.NONE
                            heartRateScanFinishedWithoutDevices = false
                        }
                        onHeartRateDisplayEnabledChanged(enabled)
                    },
                    onPrepareHeartRateBlePermission = {
                        pendingHeartRatePermissionAction = PendingHeartRatePermissionAction.NONE
                        if (
                            settingsState.heartRateSettings.blePermissionStatus ==
                            HeartRateBlePermissionStatus.PERMANENTLY_DENIED
                        ) {
                            context.openTrainFlowAppSettings()
                        } else {
                            heartRateBlePermissionStatus = settingsState
                                .heartRateSettings
                                .prepareBlePermissionRationale()
                                .blePermissionStatus
                        }
                    },
                    onRequestHeartRateBlePermission = {
                        if (
                            heartRateDisplayEnabled &&
                            settingsState.heartRateSettings.canRequestBlePermission &&
                            BleHeartRatePermissionPlanner.shouldRequestPermissions(
                                BleHeartRatePermissionTrigger.EXPLICIT_USER_ACTION
                            )
                        ) {
                            val missingPermissions = BleHeartRatePermissionPlanner
                                .requiredPermissions()
                                .filterNot { permission -> context.isPermissionGranted(permission) }
                            if (missingPermissions.isEmpty()) {
                                heartRateBlePermissionStatus = HeartRateBlePermissionStatus.GRANTED
                                heartRatePermissionRefreshKey += 1
                                onHeartRateEnvironmentChanged()
                                if (shouldResumeHeartRatePermissionAction(
                                        pendingAction = pendingHeartRatePermissionAction,
                                        permissionStatus = HeartRateBlePermissionStatus.GRANTED
                                    )
                                ) {
                                    onReconnectHeartRateDevice()
                                }
                                pendingHeartRatePermissionAction =
                                    PendingHeartRatePermissionAction.NONE
                            } else {
                                heartRateBlePermissionLauncher.launch(missingPermissions.toTypedArray())
                            }
                        }
                    },
                    onStartHeartRateDeviceScan = {
                        if (settingsState.heartRateSettings.devicePickerState.canStartScan) {
                            heartRateScanFinishedWithoutDevices = false
                            lastCompletedHeartRateScanPurpose =
                                HeartRateDeviceScanPurpose.NONE
                            heartRateScanPurpose = when {
                                trainingPreferencesState.heartRateSettings.savedDeviceIdentifier != null &&
                                    heartRateState.fact != com.liujyks.trainflow.core.model.HeartRateFact.LIVE ->
                                    HeartRateDeviceScanPurpose.CONNECT_SAVED_DEVICE
                                heartRateState.fact == com.liujyks.trainflow.core.model.HeartRateFact.LIVE ->
                                    HeartRateDeviceScanPurpose.SCAN_OTHER_DEVICES
                                else -> HeartRateDeviceScanPurpose.SCAN_DEVICES
                            }
                            onStartHeartRateDeviceScan()
                        }
                    },
                    onChangeHeartRateDevice = {
                        heartRateScanFinishedWithoutDevices = false
                        lastCompletedHeartRateScanPurpose =
                            HeartRateDeviceScanPurpose.NONE
                        heartRateScanPurpose = HeartRateDeviceScanPurpose.SCAN_DEVICES
                        onChangeHeartRateDevice()
                    },
                    onStopHeartRateDeviceScan = {
                        heartRateScanPurpose = HeartRateDeviceScanPurpose.NONE
                        onStopHeartRateDeviceScan()
                        heartRateScanFinishedWithoutDevices = false
                    },
                    onOpenBluetoothSettings = {
                        context.openBluetoothSettings()
                    },
                    onSelectHeartRateDevice = { identifier ->
                        val selected = heartRateDeviceCandidates.firstOrNull { candidate ->
                            candidate.identifier == identifier
                        }
                        if (selected != null) {
                            onSaveHeartRateDevicePreference(identifier, selected.displayName)
                        }
                        heartRateScanFinishedWithoutDevices = false
                        heartRateScanPurpose = HeartRateDeviceScanPurpose.NONE
                    },
                    onDisconnectHeartRateDevice = {
                        pendingHeartRatePermissionAction =
                            PendingHeartRatePermissionAction.NONE
                        heartRateScanPurpose = HeartRateDeviceScanPurpose.NONE
                        heartRateScanFinishedWithoutDevices = false
                        onDisconnectHeartRateDevice()
                    },
                    onReconnectHeartRateDevice = {
                        when (settingsState.heartRateSettings.blePermissionStatus) {
                            HeartRateBlePermissionStatus.GRANTED ->
                                onReconnectHeartRateDevice()
                            HeartRateBlePermissionStatus.PERMANENTLY_DENIED -> {
                                pendingHeartRatePermissionAction =
                                    PendingHeartRatePermissionAction.RECONNECT
                                context.openTrainFlowAppSettings()
                            }
                            else -> {
                                pendingHeartRatePermissionAction =
                                    PendingHeartRatePermissionAction.RECONNECT
                                heartRateBlePermissionStatus = settingsState
                                    .heartRateSettings
                                    .prepareBlePermissionRationale()
                                    .blePermissionStatus
                            }
                        }
                    },
                    onClearHeartRateDevicePreference = {
                        pendingHeartRatePermissionAction =
                            PendingHeartRatePermissionAction.NONE
                        heartRateScanPurpose = HeartRateDeviceScanPurpose.NONE
                        heartRateScanFinishedWithoutDevices = false
                        onClearHeartRateDevicePreference()
                    },
                    onHeartRatePersonalParametersChanged =
                        onHeartRatePersonalParametersChanged,
                    onUiSkinChanged = onUiSkinChanged,
                    heartRateFocusRequestKey = heartRateSettingsFocusRequestKey,
                    modifier = Modifier.padding(innerPadding)
                )

                    OfficialShellDestination.FOLLOW_ALONG_ENTRY -> FollowAlongRoute(
                    onStartFollowAlong = { plan ->
                        applyShellState(shellState.startFollowAlongSession(plan))
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                    OfficialShellDestination.FOLLOW_ALONG_SESSION -> {
                    val activePlan = shellState.activeFollowAlongSessionPlan
                    if (activePlan != null) {
                        FollowAlongWorkoutSessionRoute(
                            plan = activePlan,
                            onBackToFollowAlong = {
                                applyShellState(shellState.finishFollowAlongSession())
                            },
                            onRecordWorkoutSession = onRecordWorkoutSession,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        FollowAlongRoute(
                            onStartFollowAlong = { plan ->
                                applyShellState(shellState.startFollowAlongSession(plan))
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                    OfficialShellDestination.TIMED_SESSION -> {
                    val activePlan = shellState.activeTimedSessionPlan
                    if (activePlan != null) {
                        TimedWorkoutSessionRoute(
                            plan = activePlan,
                            onBackToPlans = {
                                applyShellState(shellState.finishTimedSession())
                            },
                            onOpenRecoveryRecommendation = { recommendation ->
                                applyShellState(shellState.openRecoveryRecommendation(recommendation))
                            },
                            onReturnToTrainingHome = {
                                applyShellState(
                                    shellState
                                        .finishTimedSession()
                                        .selectDestination(OfficialShellDestination.TRAINING)
                                )
                            },
                            onRecordWorkoutSession = onRecordWorkoutSession,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        PlanManagementRoute(
                            uiState = shellState.planManagementState,
                            onStateChange = { planManagementState ->
                                applyShellState(shellState.withPlanManagementState(planManagementState))
                            },
                            onPersistPlan = onSaveWorkoutPlan,
                            onDeletePlan = onDeleteWorkoutPlan,
                            onEditPlan = { plan ->
                                applyShellState(shellState.editPlan(plan))
                            },
                            onCreateTimedPlan = {
                                applyShellState(shellState.openTimedPlanEditorForCreate())
                            },
                            onCreateStrengthPlan = {
                                applyShellState(shellState.openStrengthPlanEditorForCreate())
                            },
                            onStartTimedPlan = { plan ->
                                applyShellState(shellState.startTimedSession(plan))
                            },
                            onStartStrengthPlan = { plan ->
                                applyShellState(shellState.startStrengthSession(plan))
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                    OfficialShellDestination.STRENGTH_SESSION -> {
                    val activePlan = shellState.activeStrengthSessionPlan
                    if (activePlan != null) {
                        StrengthWorkoutSessionRoute(
                            plan = activePlan,
                            onBackToPlans = {
                                applyShellState(shellState.finishStrengthSession())
                            },
                            onOpenRecoveryRecommendation = { recommendation ->
                                applyShellState(shellState.openRecoveryRecommendation(recommendation))
                            },
                            onRecordWorkoutSession = onRecordWorkoutSession,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        PlanManagementRoute(
                            uiState = shellState.planManagementState,
                            onStateChange = { planManagementState ->
                                applyShellState(shellState.withPlanManagementState(planManagementState))
                            },
                            onPersistPlan = onSaveWorkoutPlan,
                            onDeletePlan = onDeleteWorkoutPlan,
                            onEditPlan = { plan ->
                                applyShellState(shellState.editPlan(plan))
                            },
                            onCreateTimedPlan = {
                                applyShellState(shellState.openTimedPlanEditorForCreate())
                            },
                            onCreateStrengthPlan = {
                                applyShellState(shellState.openStrengthPlanEditorForCreate())
                            },
                            onStartTimedPlan = { plan ->
                                applyShellState(shellState.startTimedSession(plan))
                            },
                            onStartStrengthPlan = { plan ->
                                applyShellState(shellState.startStrengthSession(plan))
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                    OfficialShellDestination.EXERCISE_LIBRARY -> ExerciseLibraryRoute(
                        modifier = Modifier.padding(innerPadding)
                    )

                    OfficialShellDestination.PLANS -> PlanManagementRoute(
                        uiState = shellState.planManagementState,
                        onStateChange = { planManagementState ->
                            applyShellState(shellState.withPlanManagementState(planManagementState))
                        },
                        onPersistPlan = onSaveWorkoutPlan,
                        onDeletePlan = onDeleteWorkoutPlan,
                        onEditPlan = { plan ->
                            applyShellState(shellState.editPlan(plan))
                        },
                        onCreateTimedPlan = {
                            applyShellState(shellState.openTimedPlanEditorForCreate())
                        },
                        onCreateStrengthPlan = {
                            applyShellState(shellState.openStrengthPlanEditorForCreate())
                        },
                        onStartTimedPlan = { plan ->
                            applyShellState(shellState.startTimedSession(plan))
                        },
                        onStartStrengthPlan = { plan ->
                            applyShellState(shellState.startStrengthSession(plan))
                        },
                        modifier = Modifier.padding(innerPadding)
                    )

                    OfficialShellDestination.RECORDS -> HistoryRoute(
                        sessions = workoutSessions,
                        onClearAllHistory = onClearAllWorkoutSessions,
                        onClearPlanHistory = onClearWorkoutSessionsForPlan,
                        onClearDateHistory = onClearWorkoutSessionsStartedOnDate,
                        modifier = Modifier.padding(innerPadding)
                    )

                    OfficialShellDestination.RECOVERY -> RecoveryRoute(
                        uiState = shellState.activeRecoveryRecommendation
                            ?.toRecoveryScreenState()
                            ?: emptyRecoveryScreenState(),
                        onBackToRecords = {
                            applyShellState(shellState.selectDestination(OfficialShellDestination.RECORDS))
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                HeartRateFloatingCapsuleOverlay(
                    uiState = heartRateFloatingCapsuleUiState(
                        settings = settingsState.heartRateSettings,
                        liveState = heartRateState,
                        userAgeYears = settingsState.heartRateSettings.ageYears,
                        personalMaxHeartRateBpm =
                            settingsState.heartRateSettings.personalMaxHeartRateBpm,
                        overLimitThresholdBpm =
                            settingsState.heartRateSettings.alertThresholdBpm
                    ),
                    exclusionPolicy = shellState.heartRateCapsuleExclusionPolicy(),
                    onOpenHeartRateSettings = {
                        heartRateSettingsFocusRequestKey += 1
                        applyShellState(shellState.openHeartRateSettingsFromCapsule())
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun OfficialShellState.heartRateCapsuleExclusionPolicy(): HeartRateCapsuleExclusionPolicy {
    return when {
        currentDestination == OfficialShellDestination.STRENGTH_SESSION && activeStrengthSessionPlan != null ->
            HeartRateCapsuleExclusionPolicy.STRENGTH_SESSION
        (
            currentDestination == OfficialShellDestination.TIMED_SESSION && activeTimedSessionPlan != null
            ) || (
            currentDestination == OfficialShellDestination.FOLLOW_ALONG_SESSION &&
                activeFollowAlongSessionPlan != null
            ) -> HeartRateCapsuleExclusionPolicy.TIMED_SESSION
        showBottomBar -> HeartRateCapsuleExclusionPolicy.BOTTOM_NAV
        else -> HeartRateCapsuleExclusionPolicy.STANDARD
    }
}

private fun Context.arePermissionsGranted(permissions: List<String>): Boolean {
    return permissions.all { permission -> isPermissionGranted(permission) }
}

private fun Context.isPermissionGranted(permission: String): Boolean {
    return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}

private fun Context.openTrainFlowAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

private fun Context.openBluetoothSettings() {
    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

private fun Context.hasPermanentlyDeniedPermissions(
    permissions: List<String>,
    requestResult: Map<String, Boolean>
): Boolean {
    val activity = findActivity() ?: return false
    return permissions.any { permission ->
        requestResult[permission] == false &&
            !activity.shouldShowRequestPermissionRationale(permission)
    }
}

internal fun resolveHeartRateBlePermissionRequestResult(
    requiredPermissions: List<String>,
    requestResult: Map<String, Boolean>,
    allPermissionsCurrentlyGranted: Boolean,
    hasPermanentlyDeniedPermissions: Boolean
): HeartRateBlePermissionStatus {
    val granted = allPermissionsCurrentlyGranted ||
        requiredPermissions.all { permission -> requestResult[permission] == true }
    return when {
        granted -> HeartRateBlePermissionStatus.GRANTED
        hasPermanentlyDeniedPermissions -> HeartRateBlePermissionStatus.PERMANENTLY_DENIED
        else -> HeartRateBlePermissionStatus.DENIED
    }
}

internal enum class PendingHeartRatePermissionAction {
    NONE,
    RECONNECT
}

internal fun shouldResumeHeartRatePermissionAction(
    pendingAction: PendingHeartRatePermissionAction,
    permissionStatus: HeartRateBlePermissionStatus
): Boolean {
    return pendingAction == PendingHeartRatePermissionAction.RECONNECT &&
        permissionStatus == HeartRateBlePermissionStatus.GRANTED
}

internal fun pendingHeartRatePermissionActionAfterDisplayChange(
    displayEnabled: Boolean,
    pendingAction: PendingHeartRatePermissionAction
): PendingHeartRatePermissionAction {
    return if (displayEnabled) pendingAction else PendingHeartRatePermissionAction.NONE
}

internal fun shouldConsumeManualSavedDeviceScanMatch(
    scanPurpose: HeartRateDeviceScanPurpose,
    recoveryPhase: HeartRateRecoveryPhase
): Boolean {
    return scanPurpose == HeartRateDeviceScanPurpose.CONNECT_SAVED_DEVICE &&
        recoveryPhase != HeartRateRecoveryPhase.SEARCHING
}

internal fun shouldInvalidateHeartRateScanIntent(
    displayEnabled: Boolean,
    appVisible: Boolean,
    fact: HeartRateFact?
): Boolean {
    return !displayEnabled ||
        !appVisible ||
        fact in setOf(
            HeartRateFact.DISABLED,
            HeartRateFact.PERMISSION_REQUIRED,
            HeartRateFact.BLUETOOTH_OFF,
            HeartRateFact.LINK_DISCONNECTED,
            HeartRateFact.TECHNICAL_FAILURE,
            HeartRateFact.INTENTIONAL_STOP
        )
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun OfficialBottomBar(
    currentDestination: OfficialShellDestination,
    onDestinationSelected: (OfficialShellDestination) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            officialShellNavigationEntries(currentDestination)
                .forEach { entry ->
                    val destination = entry.destination
                    CompactBottomDestination(
                        label = destination.label,
                        shortLabel = destination.shortLabel,
                        selected = entry.selected,
                        enabled = entry.enabled,
                        onClick = { onDestinationSelected(destination) },
                        modifier = Modifier.weight(1f)
                    )
                }
        }
    }
}

@Composable
private fun CompactBottomDestination(
    label: String,
    shortLabel: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) TrainFlowPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = label }
                .clickable(enabled = enabled, onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = shortLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
        }
    }
}
