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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.liujyks.trainflow.feature.exerciselibrary.ExerciseLibraryRoute
import com.liujyks.trainflow.feature.home.HomeRoute
import com.liujyks.trainflow.feature.plans.StrengthPlanEditorRoute
import com.liujyks.trainflow.feature.plans.TimedPlanEditorRoute

@Composable
fun TrainFlowApp() {
    var currentDestination by rememberSaveable {
        mutableStateOf(OfficialShellDestination.TRAINING)
    }

    Surface {
        Scaffold(
            bottomBar = {
                OfficialBottomBar(
                    currentDestination = currentDestination,
                    onDestinationSelected = { destination ->
                        if (destination.enabled) {
                            currentDestination = destination
                        }
                    }
                )
            }
        ) { innerPadding ->
            when (currentDestination) {
                OfficialShellDestination.TRAINING -> HomeRoute(
                    onOpenExerciseLibrary = {
                        currentDestination = OfficialShellDestination.EXERCISE_LIBRARY
                    },
                    onOpenTimedPlanEditor = {
                        currentDestination = OfficialShellDestination.TIMED_PLAN_EDITOR
                    },
                    onOpenStrengthPlanEditor = {
                        currentDestination = OfficialShellDestination.STRENGTH_PLAN_EDITOR
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.TIMED_PLAN_EDITOR -> TimedPlanEditorRoute(
                    onBackToHome = {
                        currentDestination = OfficialShellDestination.TRAINING
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.STRENGTH_PLAN_EDITOR -> StrengthPlanEditorRoute(
                    onBackToHome = {
                        currentDestination = OfficialShellDestination.TRAINING
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.EXERCISE_LIBRARY -> ExerciseLibraryRoute(
                    modifier = Modifier.padding(innerPadding)
                )

                OfficialShellDestination.PLANS,
                OfficialShellDestination.RECORDS -> HomeRoute(
                    onOpenExerciseLibrary = {
                        currentDestination = OfficialShellDestination.EXERCISE_LIBRARY
                    },
                    onOpenTimedPlanEditor = {
                        currentDestination = OfficialShellDestination.TIMED_PLAN_EDITOR
                    },
                    onOpenStrengthPlanEditor = {
                        currentDestination = OfficialShellDestination.STRENGTH_PLAN_EDITOR
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
        val selectedBottomDestination = currentDestination.selectedBottomDestination()

        OfficialShellDestination.entries
            .filter { it.showInBottomBar }
            .forEach { destination ->
                NavigationBarItem(
                    selected = selectedBottomDestination == destination,
                    enabled = destination.enabled,
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

private enum class OfficialShellDestination(
    val label: String,
    val shortLabel: String,
    val enabled: Boolean,
    val showInBottomBar: Boolean = true
) {
    TRAINING(
        label = "训练",
        shortLabel = "训",
        enabled = true
    ),
    TIMED_PLAN_EDITOR(
        label = "计时计划编辑",
        shortLabel = "计",
        enabled = true,
        showInBottomBar = false
    ),
    STRENGTH_PLAN_EDITOR(
        label = "力量计划编辑",
        shortLabel = "力",
        enabled = true,
        showInBottomBar = false
    ),
    PLANS(
        label = "计划",
        shortLabel = "计",
        enabled = false
    ),
    EXERCISE_LIBRARY(
        label = "动作库",
        shortLabel = "动",
        enabled = true
    ),
    RECORDS(
        label = "记录",
        shortLabel = "录",
        enabled = false
    )
}

private fun OfficialShellDestination.selectedBottomDestination(): OfficialShellDestination {
    return when (this) {
        OfficialShellDestination.TIMED_PLAN_EDITOR,
        OfficialShellDestination.STRENGTH_PLAN_EDITOR -> OfficialShellDestination.TRAINING
        else -> this
    }
}
