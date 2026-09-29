package com.liujyks.trainflow.core.notifications

import android.os.Build
import com.liujyks.trainflow.core.model.PermissionPrivacyCopy
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutMode

internal const val ActiveWorkoutNotificationChannelId = "trainflow_active_workout"
internal const val ActiveWorkoutNotificationChannelName = "训练进行中"
internal const val ActiveWorkoutNotificationChannelDescription =
    PermissionPrivacyCopy.ACTIVE_WORKOUT_NOTIFICATION
internal const val ActiveWorkoutNotificationId = 7_200

internal data class ActiveWorkoutNotificationState(
    val sessionId: String,
    val mode: WorkoutMode,
    val planTitle: String,
    val status: SessionStatus,
    val phaseLabel: String,
    val primaryText: String,
    val timerText: String,
    val progressText: String,
    val secondaryText: String
)

internal data class ActiveWorkoutNotificationProducer(val sessionId: String, val token: Long)

internal enum class ActiveWorkoutForegroundPhase {
    NONE, STARTING, ACTIVE, RELEASING, RELEASE_UNCONFIRMED
}

internal data class ActiveWorkoutForegroundState(
    val phase: ActiveWorkoutForegroundPhase = ActiveWorkoutForegroundPhase.NONE,
    val generation: Long? = null,
    val failure: Throwable? = null
) {
    val active: Boolean get() = phase == ActiveWorkoutForegroundPhase.ACTIVE
}

internal data class ActiveWorkoutNotificationPermissionState(
    val status: ActiveWorkoutNotificationPermissionStatus,
    val rationale: String,
    val appNotificationsEnabled: Boolean = true,
    val channelNotificationsEnabled: Boolean = true
) {
    val canPostNotifications: Boolean
        get() = status != ActiveWorkoutNotificationPermissionStatus.DENIED &&
            appNotificationsEnabled && channelNotificationsEnabled

    companion object {
        fun resolve(
            sdkInt: Int = Build.VERSION.SDK_INT,
            postNotificationsGranted: Boolean,
            appNotificationsEnabled: Boolean = true,
            channelNotificationsEnabled: Boolean = true
        ): ActiveWorkoutNotificationPermissionState {
            val status = when {
                sdkInt < Build.VERSION_CODES.TIRAMISU -> ActiveWorkoutNotificationPermissionStatus.NOT_REQUIRED
                postNotificationsGranted -> ActiveWorkoutNotificationPermissionStatus.GRANTED
                else -> ActiveWorkoutNotificationPermissionStatus.DENIED
            }
            val rationale = when {
                status == ActiveWorkoutNotificationPermissionStatus.DENIED ->
                    "Android 13+ 通知权限关闭，训练仍可正常使用；训练中状态通知暂不会显示。"
                !appNotificationsEnabled || !channelNotificationsEnabled ->
                    "系统通知已关闭，训练仍可正常使用；训练中状态通知暂不会显示。"
                status == ActiveWorkoutNotificationPermissionStatus.NOT_REQUIRED ->
                    "当前 Android 版本不需要单独授予通知权限。"
                else -> "通知已开启，可接收训练中状态提示。"
            }
            return ActiveWorkoutNotificationPermissionState(
                status, rationale, appNotificationsEnabled, channelNotificationsEnabled
            )
        }
    }
}

internal enum class ActiveWorkoutNotificationPermissionStatus { GRANTED, DENIED, NOT_REQUIRED }

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
    data class Deferred(val phase: ActiveWorkoutForegroundPhase) : ActiveWorkoutNotificationUpdateResult
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
    NOTIFICATION_PERMISSION_DENIED,
    STALE_PRODUCER,
    SESSION_MISMATCH,
    STALE_VERSION
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
        val secondary = state.secondaryText.trim().ifBlank { "普通状态提示，不保证后台可靠计时。" }

        return ActiveWorkoutNotificationContent(
            channelId = ActiveWorkoutNotificationChannelId,
            channelName = ActiveWorkoutNotificationChannelName,
            channelDescription = ActiveWorkoutNotificationChannelDescription,
            notificationId = ActiveWorkoutNotificationId,
            title = title,
            text = text,
            subText = "普通状态提示",
            bigText = "$safePlanTitle · ${state.phaseLabel}\n$text\n$progressLine\n$secondary\n普通状态提示，不是 foreground service，不保证后台可靠计时。",
            ongoing = state.status == SessionStatus.ACTIVE || state.status == SessionStatus.PAUSED
        )
    }

    fun createForeground(
        state: ActiveWorkoutNotificationState,
        heartRateText: String
    ): ActiveWorkoutNotificationContent {
        val ordinary = create(state)
        return ordinary.copy(
            subText = heartRateText,
            bigText = "${state.planTitle.trim().ifBlank { "训练" }} · ${state.phaseLabel}\n" +
                "${ordinary.text}\n${state.progressText}\n$heartRateText"
        )
    }
}

internal object ActiveWorkoutNotificationPolicy {
    fun evaluate(
        state: ActiveWorkoutNotificationState,
        permissionState: ActiveWorkoutNotificationPermissionState
    ): ActiveWorkoutNotificationUpdateResult {
        if (state.status != SessionStatus.ACTIVE && state.status != SessionStatus.PAUSED) {
            return ActiveWorkoutNotificationUpdateResult.Cleared(
                reason = ActiveWorkoutNotificationClearReason.READY_OR_TERMINAL
            )
        }

        if (!permissionState.canPostNotifications) {
            return ActiveWorkoutNotificationUpdateResult.Ignored(
                reason = ActiveWorkoutNotificationIgnoredReason.NOTIFICATION_PERMISSION_DENIED,
                message = "通知权限关闭，训练仍可正常执行；训练中状态通知暂不会显示。"
            )
        }

        return ActiveWorkoutNotificationUpdateResult.Posted(
            content = ActiveWorkoutNotificationContentFactory.create(state)
        )
    }
}
