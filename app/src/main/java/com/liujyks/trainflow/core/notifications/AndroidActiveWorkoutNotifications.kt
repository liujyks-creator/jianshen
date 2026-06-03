package com.liujyks.trainflow.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.liujyks.trainflow.R

internal interface ActiveWorkoutNotificationController {
    fun update(state: ActiveWorkoutNotificationState): ActiveWorkoutNotificationUpdateResult
    fun clear(
        reason: ActiveWorkoutNotificationClearReason = ActiveWorkoutNotificationClearReason.MANUAL_CLEAR
    ): ActiveWorkoutNotificationUpdateResult.Cleared
}

internal class AndroidActiveWorkoutNotificationController(
    private val context: Context
) : ActiveWorkoutNotificationController {
    private val appContext = context.applicationContext

    override fun update(state: ActiveWorkoutNotificationState): ActiveWorkoutNotificationUpdateResult {
        return when (
            val result = ActiveWorkoutNotificationPolicy.evaluate(
                state = state,
                permissionState = appContext.resolvePlanReminderPermissionState()
            )
        ) {
            is ActiveWorkoutNotificationUpdateResult.Posted -> {
                ensureActiveWorkoutChannel(appContext)
                appContext.getSystemService(NotificationManager::class.java)
                    .notify(
                        result.content.notificationId,
                        result.content.toActiveWorkoutNotification(appContext)
                    )
                result
            }

            is ActiveWorkoutNotificationUpdateResult.Cleared -> {
                clear(result.reason)
            }

            is ActiveWorkoutNotificationUpdateResult.Ignored -> {
                clear(ActiveWorkoutNotificationClearReason.MANUAL_CLEAR)
                result
            }
        }
    }

    override fun clear(
        reason: ActiveWorkoutNotificationClearReason
    ): ActiveWorkoutNotificationUpdateResult.Cleared {
        appContext.getSystemService(NotificationManager::class.java)
            .cancel(ActiveWorkoutNotificationId)
        return ActiveWorkoutNotificationUpdateResult.Cleared(reason = reason)
    }
}

private fun ensureActiveWorkoutChannel(context: Context) {
    val channel = NotificationChannel(
        ActiveWorkoutNotificationChannelId,
        ActiveWorkoutNotificationChannelName,
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = ActiveWorkoutNotificationChannelDescription
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

private fun ActiveWorkoutNotificationContent.toActiveWorkoutNotification(
    context: Context
): Notification {
    return Notification.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(text)
        .setSubText(subText)
        .setStyle(Notification.BigTextStyle().bigText(bigText))
        .setContentIntent(trainFlowLaunchPendingIntent(context))
        .setShowWhen(false)
        .setOngoing(ongoing)
        .setOnlyAlertOnce(true)
        .setAutoCancel(false)
        .setCategory(Notification.CATEGORY_STATUS)
        .build()
}

private fun trainFlowLaunchPendingIntent(context: Context): PendingIntent? {
    val launchIntent = context.packageManager
        .getLaunchIntentForPackage(context.packageName)
        ?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        ?: return null

    return PendingIntent.getActivity(
        context,
        ActiveWorkoutNotificationId,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
