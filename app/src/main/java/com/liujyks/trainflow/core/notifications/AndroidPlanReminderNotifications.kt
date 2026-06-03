package com.liujyks.trainflow.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.liujyks.trainflow.R

internal interface PlanReminderScheduler {
    fun schedule(request: PlanReminderScheduleRequest): PlanReminderScheduleResult
    fun cancel(planId: String)
}

internal class AndroidPlanReminderScheduler(
    private val context: Context,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : PlanReminderScheduler {
    override fun schedule(request: PlanReminderScheduleRequest): PlanReminderScheduleResult {
        val result = PlanReminderSchedulePolicy.evaluate(
            request = request,
            nowEpochMillis = clock()
        )

        when (result) {
            is PlanReminderScheduleResult.Scheduled -> {
                ensurePlanReminderChannel(context)
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    result.request.scheduleAtEpochMillis ?: return result,
                    planReminderPendingIntent(context, result.request.planId, result.content)
                )
            }

            is PlanReminderScheduleResult.Cancelled -> cancel(result.planId)
            is PlanReminderScheduleResult.Ignored -> Unit
        }

        return result
    }

    override fun cancel(planId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(planReminderPendingIntent(context, planId, content = null))
    }
}

internal class PlanReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val content = intent.toPlanReminderNotificationContent() ?: return
        if (!context.resolvePlanReminderPermissionState().canPostNotifications) return

        ensurePlanReminderChannel(context)
        context.getSystemService(NotificationManager::class.java)
            .notify(content.notificationId, content.toNotification(context))
    }
}

internal fun Context.resolvePlanReminderPermissionState(): PlanReminderNotificationPermissionState {
    val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return PlanReminderNotificationPermissionState.resolve(
        sdkInt = Build.VERSION.SDK_INT,
        postNotificationsGranted = granted
    )
}

private fun ensurePlanReminderChannel(context: Context) {
    val channel = NotificationChannel(
        PlanReminderNotificationChannelId,
        PlanReminderNotificationChannelName,
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = PlanReminderNotificationChannelDescription
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

private fun planReminderPendingIntent(
    context: Context,
    planId: String,
    content: PlanReminderNotificationContent?
): PendingIntent {
    val intent = Intent(context, PlanReminderNotificationReceiver::class.java).apply {
        action = PlanReminderIntentAction
        putExtra(ExtraPlanId, planId)
        if (content != null) {
            putExtra(ExtraNotificationId, content.notificationId)
            putExtra(ExtraNotificationTitle, content.title)
            putExtra(ExtraNotificationText, content.text)
            putExtra(ExtraNotificationSubText, content.subText)
        }
    }

    return PendingIntent.getBroadcast(
        context,
        planId.stableRequestCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun Intent.toPlanReminderNotificationContent(): PlanReminderNotificationContent? {
    val notificationId = getIntExtra(ExtraNotificationId, MissingNotificationId)
        .takeUnless { it == MissingNotificationId }
        ?: return null
    val title = getStringExtra(ExtraNotificationTitle) ?: return null
    val text = getStringExtra(ExtraNotificationText) ?: return null
    val subText = getStringExtra(ExtraNotificationSubText) ?: return null

    return PlanReminderNotificationContent(
        channelId = PlanReminderNotificationChannelId,
        channelName = PlanReminderNotificationChannelName,
        channelDescription = PlanReminderNotificationChannelDescription,
        notificationId = notificationId,
        title = title,
        text = text,
        subText = subText
    )
}

private fun PlanReminderNotificationContent.toNotification(context: Context): Notification {
    return Notification.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(text)
        .setSubText(subText)
        .setStyle(Notification.BigTextStyle().bigText(text))
        .setShowWhen(true)
        .setWhen(System.currentTimeMillis())
        .setAutoCancel(true)
        .build()
}

private fun String.stableRequestCode(): Int {
    val value = hashCode()
    return if (value == Int.MIN_VALUE) 0 else kotlin.math.abs(value)
}

private const val PlanReminderIntentAction = "com.liujyks.trainflow.PLAN_REMINDER"
private const val ExtraPlanId = "extra_plan_id"
private const val ExtraNotificationId = "extra_notification_id"
private const val ExtraNotificationTitle = "extra_notification_title"
private const val ExtraNotificationText = "extra_notification_text"
private const val ExtraNotificationSubText = "extra_notification_sub_text"
private const val MissingNotificationId = Int.MIN_VALUE
