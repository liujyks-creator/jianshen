package com.liujyks.trainflow.ui.shell.official

import com.liujyks.trainflow.feature.plans.PlanManagementScreenState
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState

internal data class OfficialShellState(
    val currentDestination: OfficialShellDestination = OfficialShellDestination.TRAINING,
    val planManagementState: PlanManagementScreenState = buildDefaultPlanManagementState()
) {
    fun selectDestination(destination: OfficialShellDestination): OfficialShellState {
        return if (destination.enabled) {
            copy(currentDestination = destination)
        } else {
            this
        }
    }

    fun withPlanManagementState(planManagementState: PlanManagementScreenState): OfficialShellState {
        return copy(planManagementState = planManagementState)
    }
}

internal enum class OfficialShellDestination(
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
        enabled = true
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

internal data class OfficialShellNavigationEntry(
    val destination: OfficialShellDestination,
    val enabled: Boolean,
    val selected: Boolean
)

internal fun officialShellNavigationEntries(
    currentDestination: OfficialShellDestination
): List<OfficialShellNavigationEntry> {
    val selectedBottomDestination = currentDestination.selectedBottomDestination()
    return OfficialShellDestination.entries
        .filter { it.showInBottomBar }
        .map { destination ->
            OfficialShellNavigationEntry(
                destination = destination,
                enabled = destination.enabled,
                selected = selectedBottomDestination == destination
            )
        }
}

internal fun OfficialShellDestination.selectedBottomDestination(): OfficialShellDestination {
    return when (this) {
        OfficialShellDestination.TIMED_PLAN_EDITOR,
        OfficialShellDestination.STRENGTH_PLAN_EDITOR -> OfficialShellDestination.TRAINING
        else -> this
    }
}
