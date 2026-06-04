package com.liujyks.trainflow.ui.shell.official

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendation
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.feature.exerciselibrary.ExerciseLibraryRoute
import com.liujyks.trainflow.feature.followalong.FollowAlongRoute
import com.liujyks.trainflow.feature.history.HistoryRoute
import com.liujyks.trainflow.feature.home.HomeRoute
import com.liujyks.trainflow.feature.plans.PlanEditorDefaults
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import com.liujyks.trainflow.feature.plans.PlanManagementRoute
import com.liujyks.trainflow.feature.plans.StrengthPlanEditorRoute
import com.liujyks.trainflow.feature.plans.TimedPlanEditorRoute
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
    trainingPreferencesState: TrainingPreferencesScreenState = defaultTrainingPreferencesScreenState(),
    planEditorDefaults: PlanEditorDefaults = PlanEditorDefaults(),
    onDefaultCountdownThresholdChanged: (Int) -> Unit = {},
    onActionCueEnabledChanged: (Boolean) -> Unit = {},
    onRestCueEnabledChanged: (Boolean) -> Unit = {},
    onSoundEnabledChanged: (Boolean) -> Unit = {},
    onVibrationEnabledChanged: (Boolean) -> Unit = {},
    onEmphasisAnimationEnabledChanged: (Boolean) -> Unit = {},
    onStrengthSetTimerModeChanged: (StrengthSetTimerModePreference) -> Unit = {},
    onUiSkinChanged: (String) -> Unit = {}
) {
    var currentDestination by rememberSaveable {
        mutableStateOf(OfficialShellDestination.TRAINING)
    }
    var planManagementState by remember {
        mutableStateOf(buildDefaultPlanManagementState())
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
        activeTimedSessionPlan = activeTimedSessionPlan,
        activeStrengthSessionPlan = activeStrengthSessionPlan,
        activeFollowAlongSessionPlan = activeFollowAlongSessionPlan,
        activeRecoveryRecommendation = activeRecoveryRecommendation
    )

    fun applyShellState(nextState: OfficialShellState) {
        currentDestination = nextState.currentDestination
        planManagementState = nextState.planManagementState
        activeTimedSessionPlan = nextState.activeTimedSessionPlan
        activeStrengthSessionPlan = nextState.activeStrengthSessionPlan
        activeFollowAlongSessionPlan = nextState.activeFollowAlongSessionPlan
        activeRecoveryRecommendation = nextState.activeRecoveryRecommendation
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
                        applyShellState(shellState.selectDestination(OfficialShellDestination.TIMED_PLAN_EDITOR))
                    },
                    onOpenStrengthPlanEditor = {
                        applyShellState(shellState.selectDestination(OfficialShellDestination.STRENGTH_PLAN_EDITOR))
                    },
                    onOpenFollowAlong = {
                        applyShellState(shellState.selectDestination(OfficialShellDestination.FOLLOW_ALONG_ENTRY))
                    },
                    onOpenSettings = {
                        applyShellState(shellState.selectDestination(OfficialShellDestination.SETTINGS))
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.TIMED_PLAN_EDITOR -> TimedPlanEditorRoute(
                    onBackToHome = {
                        applyShellState(shellState.selectDestination(OfficialShellDestination.TRAINING))
                    },
                    planEditorDefaults = planEditorDefaults,
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.STRENGTH_PLAN_EDITOR -> StrengthPlanEditorRoute(
                    onBackToHome = {
                        applyShellState(shellState.selectDestination(OfficialShellDestination.TRAINING))
                    },
                    planEditorDefaults = planEditorDefaults,
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
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        PlanManagementRoute(
                            uiState = shellState.planManagementState,
                            onStateChange = { planManagementState ->
                                applyShellState(shellState.withPlanManagementState(planManagementState))
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
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        PlanManagementRoute(
                            uiState = shellState.planManagementState,
                            onStateChange = { planManagementState ->
                                applyShellState(shellState.withPlanManagementState(planManagementState))
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
                    onStartTimedPlan = { plan ->
                        applyShellState(shellState.startTimedSession(plan))
                    },
                    onStartStrengthPlan = { plan ->
                        applyShellState(shellState.startStrengthSession(plan))
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.RECORDS -> HistoryRoute(
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
