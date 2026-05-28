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
import com.liujyks.trainflow.feature.exerciselibrary.ExerciseLibraryRoute
import com.liujyks.trainflow.feature.home.HomeRoute
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import com.liujyks.trainflow.feature.plans.PlanManagementRoute
import com.liujyks.trainflow.feature.plans.StrengthPlanEditorRoute
import com.liujyks.trainflow.feature.plans.TimedPlanEditorRoute

@Composable
fun TrainFlowApp() {
    var currentDestination by rememberSaveable {
        mutableStateOf(OfficialShellDestination.TRAINING)
    }
    var planManagementState by remember {
        mutableStateOf(buildDefaultPlanManagementState())
    }
    val shellState = OfficialShellState(
        currentDestination = currentDestination,
        planManagementState = planManagementState
    )

    fun applyShellState(nextState: OfficialShellState) {
        currentDestination = nextState.currentDestination
        planManagementState = nextState.planManagementState
    }

    Surface {
        Scaffold(
            bottomBar = {
                OfficialBottomBar(
                    currentDestination = shellState.currentDestination,
                    onDestinationSelected = { destination ->
                        applyShellState(shellState.selectDestination(destination))
                    }
                )
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
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.TIMED_PLAN_EDITOR -> TimedPlanEditorRoute(
                    onBackToHome = {
                        applyShellState(shellState.selectDestination(OfficialShellDestination.TRAINING))
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.STRENGTH_PLAN_EDITOR -> StrengthPlanEditorRoute(
                    onBackToHome = {
                        applyShellState(shellState.selectDestination(OfficialShellDestination.TRAINING))
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.EXERCISE_LIBRARY -> ExerciseLibraryRoute(
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.PLANS -> PlanManagementRoute(
                    uiState = shellState.planManagementState,
                    onStateChange = { planManagementState ->
                        applyShellState(shellState.withPlanManagementState(planManagementState))
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.RECORDS -> HomeRoute(
                    onOpenExerciseLibrary = {
                        applyShellState(shellState.selectDestination(OfficialShellDestination.EXERCISE_LIBRARY))
                    },
                    onOpenTimedPlanEditor = {
                        applyShellState(shellState.selectDestination(OfficialShellDestination.TIMED_PLAN_EDITOR))
                    },
                    onOpenStrengthPlanEditor = {
                        applyShellState(shellState.selectDestination(OfficialShellDestination.STRENGTH_PLAN_EDITOR))
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
