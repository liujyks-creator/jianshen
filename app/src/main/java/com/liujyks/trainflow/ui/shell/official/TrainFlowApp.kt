package com.liujyks.trainflow.ui.shell.official

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.DisposableEffect
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
import com.liujyks.trainflow.core.health.BleHeartRateProviderStateKind
import com.liujyks.trainflow.core.health.AndroidHeartRateDeviceScanner
import com.liujyks.trainflow.core.model.WorkoutSession
import com.liujyks.trainflow.core.model.WorkoutPlan
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
import com.liujyks.trainflow.feature.settings.StrengthSetTimerModePreference
import com.liujyks.trainflow.feature.settings.TrainingPreferencesScreenState
import com.liujyks.trainflow.feature.settings.defaultTrainingPreferencesScreenState
import com.liujyks.trainflow.feature.settings.heartRateDevicePickerUiState
import com.liujyks.trainflow.feature.settings.prepareBlePermissionRationale
import com.liujyks.trainflow.feature.settings.resolveHeartRateBlePermissionStatus
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
    onClearHeartRateDevicePreference: () -> Unit = {},
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
    val heartRateDeviceScanner = remember(context) {
        AndroidHeartRateDeviceScanner(context.applicationContext)
    }
    val heartRateScannerState by heartRateDeviceScanner.providerState.collectAsState()
    val heartRateDeviceCandidates by heartRateDeviceScanner.candidates.collectAsState()
    var heartRateScanActive by rememberSaveable {
        mutableStateOf(false)
    }
    var heartRateScanFinishedWithoutDevices by rememberSaveable {
        mutableStateOf(false)
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
    val settingsState = trainingPreferencesState.copy(
        heartRateSettings = trainingPreferencesState.heartRateSettings.copy(
            blePermissionStatus = resolvedHeartRateBlePermissionStatus,
            devicePickerState = heartRateDevicePickerUiState(
                displayEnabled = heartRateDisplayEnabled,
                blePermissionStatus = resolvedHeartRateBlePermissionStatus,
                scannerState = heartRateScannerState,
                scannerCandidates = heartRateDeviceCandidates,
                scanActive = heartRateScanActive,
                savedDeviceIdentifier = trainingPreferencesState.heartRateSettings.savedDeviceIdentifier,
                savedDeviceDisplayName = trainingPreferencesState.heartRateSettings.savedDeviceDisplayName,
                scanFinishedWithoutDevices = heartRateScanFinishedWithoutDevices
            )
        )
    )
    val heartRateBlePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val requiredPermissions = BleHeartRatePermissionPlanner.requiredPermissions()
        val granted = context.arePermissionsGranted(requiredPermissions)
            || requiredPermissions.all { permission -> result[permission] == true }
        heartRateBlePermissionStatus = if (granted) {
            HeartRateBlePermissionStatus.GRANTED
        } else if (context.hasPermanentlyDeniedPermissions(requiredPermissions, result)) {
            HeartRateBlePermissionStatus.PERMANENTLY_DENIED
        } else {
            HeartRateBlePermissionStatus.DENIED
        }
        heartRatePermissionRefreshKey += 1
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
    }

    LaunchedEffect(currentDestination, heartRateDisplayEnabled, resolvedHeartRateBlePermissionStatus) {
        if (
            currentDestination == OfficialShellDestination.SETTINGS &&
            heartRateDisplayEnabled &&
            resolvedHeartRateBlePermissionStatus == HeartRateBlePermissionStatus.GRANTED
        ) {
            heartRateDeviceScanner.refreshAvailability()
        } else if (!heartRateDisplayEnabled || resolvedHeartRateBlePermissionStatus != HeartRateBlePermissionStatus.GRANTED) {
            heartRateDeviceScanner.stopScan()
            heartRateScanActive = false
            heartRateScanFinishedWithoutDevices = false
        }
    }

    LaunchedEffect(heartRateScannerState.kind, heartRateDeviceCandidates) {
        val heartRateCandidateCount = heartRateDeviceCandidates.count { candidate ->
            candidate.advertisesHeartRateService
        }
        when (heartRateScannerState.kind) {
            BleHeartRateProviderStateKind.SCANNING -> {
                heartRateScanActive = true
                heartRateScanFinishedWithoutDevices = false
            }

            BleHeartRateProviderStateKind.DEVICE_FOUND -> {
                if (heartRateCandidateCount > 0) {
                    heartRateScanFinishedWithoutDevices = false
                }
            }

            BleHeartRateProviderStateKind.STOPPED -> {
                if (heartRateScanActive) {
                    heartRateScanFinishedWithoutDevices = heartRateCandidateCount == 0
                    heartRateScanActive = false
                }
            }

            BleHeartRateProviderStateKind.ERROR,
            BleHeartRateProviderStateKind.PERMISSION_REQUIRED,
            BleHeartRateProviderStateKind.BLUETOOTH_DISABLED,
            BleHeartRateProviderStateKind.UNAVAILABLE -> {
                heartRateScanActive = false
            }

            else -> Unit
        }
    }

    LaunchedEffect(currentDestination) {
        if (currentDestination != OfficialShellDestination.SETTINGS) {
            heartRateDeviceScanner.stopScan()
            heartRateScanActive = false
        }
    }

    DisposableEffect(heartRateDeviceScanner) {
        onDispose {
            heartRateDeviceScanner.close()
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
                        applyShellState(shellState.selectDestination(OfficialShellDestination.TRAINING))
                    },
                    onDefaultCountdownThresholdChanged = onDefaultCountdownThresholdChanged,
                    onActionCueEnabledChanged = onActionCueEnabledChanged,
                    onRestCueEnabledChanged = onRestCueEnabledChanged,
                    onSoundEnabledChanged = onSoundEnabledChanged,
                    onVibrationEnabledChanged = onVibrationEnabledChanged,
                    onEmphasisAnimationEnabledChanged = onEmphasisAnimationEnabledChanged,
                    onStrengthSetTimerModeChanged = onStrengthSetTimerModeChanged,
                    onHeartRateDisplayEnabledChanged = onHeartRateDisplayEnabledChanged,
                    onPrepareHeartRateBlePermission = {
                        heartRateBlePermissionStatus = settingsState
                            .heartRateSettings
                            .prepareBlePermissionRationale()
                            .blePermissionStatus
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
                            } else {
                                heartRateBlePermissionLauncher.launch(missingPermissions.toTypedArray())
                            }
                        }
                    },
                    onStartHeartRateDeviceScan = {
                        if (settingsState.heartRateSettings.devicePickerState.canStartScan) {
                            heartRateScanActive = true
                            heartRateScanFinishedWithoutDevices = false
                            heartRateDeviceScanner.startScan()
                        }
                    },
                    onStopHeartRateDeviceScan = {
                        heartRateDeviceScanner.stopScan()
                        heartRateScanActive = false
                        heartRateScanFinishedWithoutDevices = false
                    },
                    onSelectHeartRateDevice = { identifier ->
                        val selection = heartRateDeviceScanner.selectDevice(identifier)
                        val fallback = heartRateDeviceCandidates.firstOrNull { candidate ->
                            candidate.identifier == identifier
                        }
                        val displayName = selection?.displayName ?: fallback?.displayName
                        if (displayName != null) {
                            onSaveHeartRateDevicePreference(identifier, displayName)
                        }
                        heartRateDeviceScanner.stopScan()
                        heartRateScanActive = false
                        heartRateScanFinishedWithoutDevices = false
                    },
                    onClearHeartRateDevicePreference = onClearHeartRateDevicePreference,
                    onUiSkinChanged = onUiSkinChanged,
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
        }
    }
}

private fun Context.arePermissionsGranted(permissions: List<String>): Boolean {
    return permissions.all { permission -> isPermissionGranted(permission) }
}

private fun Context.isPermissionGranted(permission: String): Boolean {
    return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
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
