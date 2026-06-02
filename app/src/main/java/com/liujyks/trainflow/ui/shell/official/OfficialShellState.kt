package com.liujyks.trainflow.ui.shell.official

import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendation
import com.liujyks.trainflow.feature.plans.PlanManagementScreenState
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan

internal data class OfficialShellState(
    val currentDestination: OfficialShellDestination = OfficialShellDestination.TRAINING,
    val planManagementState: PlanManagementScreenState = buildDefaultPlanManagementState(),
    val activeTimedSessionPlan: WorkoutPlan? = null,
    val activeStrengthSessionPlan: WorkoutPlan? = null,
    val activeRecoveryRecommendation: BasicRecoveryRecommendation? = null
) {
    val showBottomBar: Boolean
        get() = !isSessionNavigationLocked

    private val isSessionNavigationLocked: Boolean
        get() = (currentDestination == OfficialShellDestination.TIMED_SESSION && activeTimedSessionPlan != null) ||
            (currentDestination == OfficialShellDestination.STRENGTH_SESSION && activeStrengthSessionPlan != null)

    fun selectDestination(destination: OfficialShellDestination): OfficialShellState {
        if (isSessionNavigationLocked && destination != currentDestination) {
            return this
        }

        return if (destination.enabled) {
            copy(currentDestination = destination)
        } else {
            this
        }
    }

    fun withPlanManagementState(planManagementState: PlanManagementScreenState): OfficialShellState {
        return copy(planManagementState = planManagementState)
    }

    fun startTimedSession(plan: WorkoutPlan): OfficialShellState {
        if (plan.mode != WorkoutMode.TIMED) return this

        return copy(
            currentDestination = OfficialShellDestination.TIMED_SESSION,
            activeTimedSessionPlan = plan,
            activeStrengthSessionPlan = null,
            activeRecoveryRecommendation = null
        )
    }

    fun startStrengthSession(plan: WorkoutPlan): OfficialShellState {
        if (plan.mode != WorkoutMode.STRENGTH) return this

        return copy(
            currentDestination = OfficialShellDestination.STRENGTH_SESSION,
            activeTimedSessionPlan = null,
            activeStrengthSessionPlan = plan,
            activeRecoveryRecommendation = null
        )
    }

    fun openRecoveryRecommendation(recommendation: BasicRecoveryRecommendation): OfficialShellState {
        return copy(
            currentDestination = OfficialShellDestination.RECOVERY,
            activeTimedSessionPlan = null,
            activeStrengthSessionPlan = null,
            activeRecoveryRecommendation = recommendation
        )
    }

    fun finishTimedSession(): OfficialShellState {
        return copy(
            currentDestination = OfficialShellDestination.PLANS,
            activeTimedSessionPlan = null,
            activeRecoveryRecommendation = null
        )
    }

    fun finishStrengthSession(): OfficialShellState {
        return copy(
            currentDestination = OfficialShellDestination.PLANS,
            activeStrengthSessionPlan = null,
            activeRecoveryRecommendation = null
        )
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
    FOLLOW_ALONG_ENTRY(
        label = "基础跟练",
        shortLabel = "跟",
        enabled = true,
        showInBottomBar = false
    ),
    TIMED_SESSION(
        label = "计时训练",
        shortLabel = "训",
        enabled = true,
        showInBottomBar = false
    ),
    STRENGTH_SESSION(
        label = "力量训练",
        shortLabel = "力",
        enabled = true,
        showInBottomBar = false
    ),
    RECOVERY(
        label = "恢复建议",
        shortLabel = "恢",
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
        enabled = true
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
        OfficialShellDestination.STRENGTH_PLAN_EDITOR,
        OfficialShellDestination.FOLLOW_ALONG_ENTRY,
        OfficialShellDestination.TIMED_SESSION,
        OfficialShellDestination.STRENGTH_SESSION -> OfficialShellDestination.TRAINING
        OfficialShellDestination.RECOVERY -> OfficialShellDestination.RECORDS
        else -> this
    }
}
