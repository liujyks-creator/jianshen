package com.liujyks.trainflow.core.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.liujyks.trainflow.core.data.WorkoutPlanRepository
import kotlin.math.abs

private const val LegacyPlanReminderChannelId = "trainflow_plan_reminders"
private const val LegacyPlanReminderReceiver =
    "com.liujyks.trainflow.core.notifications.PlanReminderNotificationReceiver"
private const val LegacyPlanReminderAction = "com.liujyks.trainflow.PLAN_REMINDER"

internal suspend fun clearLegacyPlanReminders(
    context: Context,
    repository: WorkoutPlanRepository
) {
    val appContext = context.applicationContext
    val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    repository.getPlans().forEach { plan ->
        val intent = Intent(LegacyPlanReminderAction).setClassName(appContext, LegacyPlanReminderReceiver)
        val requestCode = plan.id.hashCode().let { if (it == Int.MIN_VALUE) 0 else abs(it) }
        val pending = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    val notifications = appContext.getSystemService(NotificationManager::class.java)
    notifications.activeNotifications
        .filter { it.notification.channelId == LegacyPlanReminderChannelId }
        .forEach { notifications.cancel(it.tag, it.id) }

    repository.clearLegacyReminders()
}
