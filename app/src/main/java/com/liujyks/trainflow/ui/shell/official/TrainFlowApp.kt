package com.liujyks.trainflow.ui.shell.official

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendation
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
import com.liujyks.trainflow.feature.settings.StrengthSetTimerModePreference
import com.liujyks.trainflow.feature.settings.TrainingPreferencesScreenState
import com.liujyks.trainflow.feature.settings.defaultTrainingPreferencesScreenState
import com.liujyks.trainflow.feature.workoutsession.FollowAlongWorkoutSessionRoute
import com.liujyks.trainflow.feature.workoutsession.StrengthWorkoutSessionRoute
import com.liujyks.trainflow.feature.workoutsession.TimedWorkoutSessionRoute

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
    onUiSkinChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val planReminderScheduler = remember(context) {
        AndroidPlanReminderScheduler(context.applicationContext)
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
                    uiState = trainingPreferencesState,
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

@Composable
private fun OfficialBottomBar(
    currentDestination: OfficialShellDestination,
    onDestinationSelected: (OfficialShellDestination) -> Unit
) {
    NavigationBar {
        officialShellNavigationEntries(currentDestination)
            .forEach { entry ->
                val destination = entry.destination
                NavigationBarItem(
                    selected = entry.selected,
                    enabled = entry.enabled,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        Text(text = destination.shortLabel)
                    },
                    label = {
                        Text(text = destination.label)
                    }
                )
            }
    }
}
