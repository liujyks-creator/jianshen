package com.liujyks.trainflow.core.notifications

import android.os.Build

internal const val PlanReminderNotificationChannelId = "trainflow_plan_reminders"
internal const val PlanReminderNotificationChannelName = "训练提醒"
internal const val PlanReminderNotificationChannelDescription =
    "用于提醒你按计划开始训练。普通通知可能被系统延迟，不作为闹钟级强提醒。"

internal data class PlanReminderNotificationPermissionState(
    val status: PlanReminderNotificationPermissionStatus,
    val rationale: String
) {
    val canPostNotifications: Boolean
        get() = status != PlanReminderNotificationPermissionStatus.DENIED

    companion object {
        fun resolve(
            sdkInt: Int = Build.VERSION.SDK_INT,
            postNotificationsGranted: Boolean
        ): PlanReminderNotificationPermissionState {
            return if (sdkInt < Build.VERSION_CODES.TIRAMISU) {
                PlanReminderNotificationPermissionState(
                    status = PlanReminderNotificationPermissionStatus.NOT_REQUIRED,
                    rationale = "当前 Android 版本不需要单独授予通知权限。"
                )
            } else if (postNotificationsGranted) {
                PlanReminderNotificationPermissionState(
                    status = PlanReminderNotificationPermissionStatus.GRANTED,
                    rationale = "通知权限已开启，可接收训练计划提醒。"
                )
            } else {
                PlanReminderNotificationPermissionState(
                    status = PlanReminderNotificationPermissionStatus.DENIED,
                    rationale = "Android 13+ 通知权限关闭，计划提醒暂不会弹出；训练执行仍可正常使用。"
                )
            }
        }
    }
}

internal enum class PlanReminderNotificationPermissionStatus {
    GRANTED,
    DENIED,
    NOT_REQUIRED
}

internal data class PlanReminderScheduleRequest(
    val planId: String,
    val planTitle: String,
    val scheduleAtEpochMillis: Long?,
    val enabled: Boolean,
    val permissionState: PlanReminderNotificationPermissionState
)

internal sealed interface PlanReminderScheduleResult {
    data class Scheduled(
        val request: PlanReminderScheduleRequest,
        val content: PlanReminderNotificationContent
    ) : PlanReminderScheduleResult

    data class Cancelled(
        val planId: String
    ) : PlanReminderScheduleResult

    data class Ignored(
        val reason: PlanReminderScheduleIgnoredReason,
        val message: String
    ) : PlanReminderScheduleResult
}

internal enum class PlanReminderScheduleIgnoredReason {
    DISABLED,
    MISSING_SCHEDULE_AT,
    PAST_SCHEDULE_AT,
    NOTIFICATION_PERMISSION_DENIED
}

internal data class PlanReminderNotificationContent(
    val channelId: String,
    val channelName: String,
    val channelDescription: String,
    val notificationId: Int,
    val title: String,
    val text: String,
    val subText: String
)

internal object PlanReminderNotificationContentFactory {
    fun create(planId: String, planTitle: String): PlanReminderNotificationContent {
        return PlanReminderNotificationContent(
            channelId = PlanReminderNotificationChannelId,
            channelName = PlanReminderNotificationChannelName,
            channelDescription = PlanReminderNotificationChannelDescription,
            notificationId = planId.stableNotificationId(),
            title = "训练提醒",
            text = "「${planTitle.trim().ifBlank { "训练计划" }}」可以开始训练了。",
            subText = "普通通知，可能被系统延迟"
        )
    }
}

internal object PlanReminderSchedulePolicy {
    fun evaluate(
        request: PlanReminderScheduleRequest,
        nowEpochMillis: Long
    ): PlanReminderScheduleResult {
        if (!request.enabled) {
            return PlanReminderScheduleResult.Cancelled(planId = request.planId)
        }

        val scheduleAt = request.scheduleAtEpochMillis ?: return PlanReminderScheduleResult.Ignored(
            reason = PlanReminderScheduleIgnoredReason.MISSING_SCHEDULE_AT,
            message = "请先选择未来的提醒时间。"
        )

        if (scheduleAt <= nowEpochMillis) {
            return PlanReminderScheduleResult.Ignored(
                reason = PlanReminderScheduleIgnoredReason.PAST_SCHEDULE_AT,
                message = "提醒时间已过，请选择未来时间。"
            )
        }

        if (!request.permissionState.canPostNotifications) {
            return PlanReminderScheduleResult.Ignored(
                reason = PlanReminderScheduleIgnoredReason.NOTIFICATION_PERMISSION_DENIED,
                message = request.permissionState.rationale
            )
        }

        return PlanReminderScheduleResult.Scheduled(
            request = request,
            content = PlanReminderNotificationContentFactory.create(
                planId = request.planId,
                planTitle = request.planTitle
            )
        )
    }
}

private fun String.stableNotificationId(): Int {
    return hashCode().let { value ->
        if (value == Int.MIN_VALUE) 0 else value.absoluteValue
    }
}

private val Int.absoluteValue: Int
    get() = if (this < 0) -this else this
