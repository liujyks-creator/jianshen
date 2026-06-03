package com.liujyks.trainflow.core.notifications

import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutMode

internal const val ActiveWorkoutNotificationChannelId = "trainflow_active_workout"
internal const val ActiveWorkoutNotificationChannelName = "训练进行中"
internal const val ActiveWorkoutNotificationChannelDescription =
    "训练进行中显示当前状态摘要。普通 ongoing 通知不承诺后台精确计时或闹铃级提醒。"
internal const val ActiveWorkoutNotificationId = 7_200

internal data class ActiveWorkoutNotificationState(
    val sessionKey: String,
    val mode: WorkoutMode,
    val planTitle: String,
    val status: SessionStatus,
    val phaseLabel: String,
    val primaryText: String,
    val timerText: String,
    val progressText: String,
    val secondaryText: String
)

internal data class ActiveWorkoutNotificationContent(
    val channelId: String,
    val channelName: String,
    val channelDescription: String,
    val notificationId: Int,
    val title: String,
    val text: String,
    val subText: String,
    val bigText: String,
    val ongoing: Boolean
)

internal sealed interface ActiveWorkoutNotificationUpdateResult {
    data class Posted(
        val content: ActiveWorkoutNotificationContent
    ) : ActiveWorkoutNotificationUpdateResult

    data class Cleared(
        val reason: ActiveWorkoutNotificationClearReason
    ) : ActiveWorkoutNotificationUpdateResult

    data class Ignored(
        val reason: ActiveWorkoutNotificationIgnoredReason,
        val message: String
    ) : ActiveWorkoutNotificationUpdateResult
}

internal enum class ActiveWorkoutNotificationClearReason {
    READY_OR_TERMINAL,
    ROUTE_DISPOSED,
    MANUAL_CLEAR
}

internal enum class ActiveWorkoutNotificationIgnoredReason {
    NOTIFICATION_PERMISSION_DENIED
}

internal object ActiveWorkoutNotificationContentFactory {
    fun create(state: ActiveWorkoutNotificationState): ActiveWorkoutNotificationContent {
        val safePlanTitle = state.planTitle.trim().ifBlank { "训练" }
        val title = when {
            state.status == SessionStatus.PAUSED -> "训练已暂停"
            state.mode == WorkoutMode.STRENGTH -> "力量训练进行中"
            state.mode == WorkoutMode.FOLLOW_ALONG -> "基础跟练进行中"
            else -> "计时训练进行中"
        }
        val text = "${state.primaryText.trim().ifBlank { state.phaseLabel }} · ${state.timerText}"
        val progressLine = state.progressText.trim().ifBlank { state.phaseLabel }
        val secondary = state.secondaryText.trim().ifBlank { "普通状态提示，不保证后台精确计时。" }

        return ActiveWorkoutNotificationContent(
            channelId = ActiveWorkoutNotificationChannelId,
            channelName = ActiveWorkoutNotificationChannelName,
            channelDescription = ActiveWorkoutNotificationChannelDescription,
            notificationId = ActiveWorkoutNotificationId,
            title = title,
            text = text,
            subText = "普通状态提示",
            bigText = "$safePlanTitle · ${state.phaseLabel}\n$text\n$progressLine\n$secondary\n普通状态提示，不保证后台精确计时。",
            ongoing = state.status == SessionStatus.ACTIVE || state.status == SessionStatus.PAUSED
        )
    }
}

internal object ActiveWorkoutNotificationPolicy {
    fun evaluate(
        state: ActiveWorkoutNotificationState,
        permissionState: PlanReminderNotificationPermissionState
    ): ActiveWorkoutNotificationUpdateResult {
        if (state.status != SessionStatus.ACTIVE && state.status != SessionStatus.PAUSED) {
            return ActiveWorkoutNotificationUpdateResult.Cleared(
                reason = ActiveWorkoutNotificationClearReason.READY_OR_TERMINAL
            )
        }

        if (!permissionState.canPostNotifications) {
            return ActiveWorkoutNotificationUpdateResult.Ignored(
                reason = ActiveWorkoutNotificationIgnoredReason.NOTIFICATION_PERMISSION_DENIED,
                message = "通知权限关闭，训练仍可正常执行；活跃训练状态通知暂不会显示。"
            )
        }

        return ActiveWorkoutNotificationUpdateResult.Posted(
            content = ActiveWorkoutNotificationContentFactory.create(state)
        )
    }
}
