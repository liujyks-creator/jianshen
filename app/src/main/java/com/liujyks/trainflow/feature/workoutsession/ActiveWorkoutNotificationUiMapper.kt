package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationState

internal fun timedActiveWorkoutNotificationState(
    planId: String,
    status: SessionStatus,
    uiState: TimedWorkoutSessionScreenState
): ActiveWorkoutNotificationState {
    return ActiveWorkoutNotificationState(
        sessionKey = "timed:$planId",
        mode = WorkoutMode.TIMED,
        planTitle = uiState.planTitle,
        status = status,
        phaseLabel = uiState.phaseLabel,
        primaryText = uiState.currentTitle,
        timerText = uiState.timerText,
        progressText = uiState.progressLabel,
        secondaryText = when {
            uiState.isTerminal -> "训练已结束，活跃训练状态通知会清理。"
            uiState.isPaused -> "时间已冻结；这不是后台精确计时承诺。"
            else -> uiState.nextStepLabel
        }
    )
}

internal fun strengthActiveWorkoutNotificationState(
    planId: String,
    status: SessionStatus,
    uiState: StrengthWorkoutSessionScreenState
): ActiveWorkoutNotificationState {
    return ActiveWorkoutNotificationState(
        sessionKey = "strength:$planId",
        mode = WorkoutMode.STRENGTH,
        planTitle = uiState.planTitle,
        status = status,
        phaseLabel = uiState.phaseLabel,
        primaryText = uiState.currentExerciseName,
        timerText = uiState.primaryMetricText,
        progressText = uiState.setProgressLabel,
        secondaryText = when {
            uiState.isTerminal -> "训练已结束，活跃训练状态通知会清理。"
            uiState.isPaused -> "当前组和休息计时已冻结；这不是后台精确计时承诺。"
            else -> "${uiState.primaryMetricLabel} · ${uiState.nextSetLabel}"
        }
    )
}

internal fun followAlongActiveWorkoutNotificationState(
    planId: String,
    status: SessionStatus,
    uiState: FollowAlongWorkoutSessionUiState
): ActiveWorkoutNotificationState {
    return ActiveWorkoutNotificationState(
        sessionKey = "follow_along:$planId",
        mode = WorkoutMode.FOLLOW_ALONG,
        planTitle = uiState.planTitle,
        status = status,
        phaseLabel = uiState.phaseLabel,
        primaryText = uiState.currentActionTitle,
        timerText = uiState.timerText,
        progressText = uiState.progressLabel,
        secondaryText = when {
            uiState.isTerminal -> "基础跟练已结束，活跃训练状态通知会清理。"
            uiState.isPaused -> "时间已冻结；这不是后台精确计时承诺。"
            else -> uiState.nextActionLabel
        }
    )
}
