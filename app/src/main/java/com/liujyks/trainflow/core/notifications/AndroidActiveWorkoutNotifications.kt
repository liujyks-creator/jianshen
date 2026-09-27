package com.liujyks.trainflow.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.liujyks.trainflow.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface ActiveWorkoutNotificationController {
    val permissionState: StateFlow<ActiveWorkoutNotificationPermissionState>
    fun initialize()
    fun beginSession(sessionId: String): ActiveWorkoutNotificationProducer
    fun update(
        producer: ActiveWorkoutNotificationProducer,
        stateVersion: Long,
        state: ActiveWorkoutNotificationState
    ): ActiveWorkoutNotificationUpdateResult
    fun release(producer: ActiveWorkoutNotificationProducer): ActiveWorkoutNotificationUpdateResult
    fun refreshPermission(): ActiveWorkoutNotificationPermissionState
}

internal class AndroidActiveWorkoutNotificationController(
    private val context: Context
) : ActiveWorkoutNotificationController {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val mutablePermissionState = MutableStateFlow(appContext.resolveActiveWorkoutPermissionState())
    override val permissionState: StateFlow<ActiveWorkoutNotificationPermissionState> = mutablePermissionState
    private var initialized = false
    private var nextToken = 0L
    private var currentProducer: ActiveWorkoutNotificationProducer? = null
    private var lastAcceptedVersion = 0L
    private var latestDesiredState: ActiveWorkoutNotificationState? = null
    private var ordinaryPosted = false

    override fun initialize() {
        if (initialized) return
        ensureActiveWorkoutChannel(appContext)
        notificationManager.cancel(ActiveWorkoutNotificationId)
        initialized = true
        mutablePermissionState.value = appContext.resolveActiveWorkoutPermissionState()
    }

    override fun beginSession(sessionId: String): ActiveWorkoutNotificationProducer {
        check(initialized)
        clearPostedNotification()
        val producer = ActiveWorkoutNotificationProducer(sessionId, ++nextToken)
        currentProducer = producer
        lastAcceptedVersion = 0L
        latestDesiredState = null
        return producer
    }

    override fun update(
        producer: ActiveWorkoutNotificationProducer,
        stateVersion: Long,
        state: ActiveWorkoutNotificationState
    ): ActiveWorkoutNotificationUpdateResult {
        if (producer != currentProducer) return ActiveWorkoutNotificationUpdateResult.Ignored(
            ActiveWorkoutNotificationIgnoredReason.STALE_PRODUCER, "训练场次已结束或被替换。"
        )
        if (state.sessionId != producer.sessionId) return ActiveWorkoutNotificationUpdateResult.Ignored(
            ActiveWorkoutNotificationIgnoredReason.SESSION_MISMATCH, "通知状态与当前训练场次不符。"
        )
        if (stateVersion <= lastAcceptedVersion) return ActiveWorkoutNotificationUpdateResult.Ignored(
            ActiveWorkoutNotificationIgnoredReason.STALE_VERSION, "旧训练状态已忽略。"
        )
        lastAcceptedVersion = stateVersion
        latestDesiredState = state
        return publishDesiredState(state)
    }

    override fun release(producer: ActiveWorkoutNotificationProducer): ActiveWorkoutNotificationUpdateResult {
        if (producer != currentProducer) return ActiveWorkoutNotificationUpdateResult.Ignored(
            ActiveWorkoutNotificationIgnoredReason.STALE_PRODUCER, "训练场次已结束或被替换。"
        )
        clearPostedNotification()
        currentProducer = null
        latestDesiredState = null
        return ActiveWorkoutNotificationUpdateResult.Cleared(ActiveWorkoutNotificationClearReason.ROUTE_DISPOSED)
    }

    override fun refreshPermission(): ActiveWorkoutNotificationPermissionState {
        val current = appContext.resolveActiveWorkoutPermissionState()
        mutablePermissionState.value = current
        val state = latestDesiredState
        if (currentProducer != null && state != null) publishDesiredState(state, current)
        return current
    }

    private fun publishDesiredState(
        state: ActiveWorkoutNotificationState,
        permission: ActiveWorkoutNotificationPermissionState = appContext.resolveActiveWorkoutPermissionState()
    ): ActiveWorkoutNotificationUpdateResult {
        mutablePermissionState.value = permission
        return when (val result = ActiveWorkoutNotificationPolicy.evaluate(state, permission)) {
            is ActiveWorkoutNotificationUpdateResult.Posted -> {
                notificationManager.notify(
                    result.content.notificationId,
                    result.content.toActiveWorkoutNotification(appContext)
                )
                ordinaryPosted = true
                result
            }

            is ActiveWorkoutNotificationUpdateResult.Cleared -> {
                clearPostedNotification()
                currentProducer = null
                latestDesiredState = null
                result
            }

            is ActiveWorkoutNotificationUpdateResult.Ignored -> {
                clearPostedNotification()
                result
            }
        }
    }

    private fun clearPostedNotification() {
        if (ordinaryPosted) {
            notificationManager.cancel(ActiveWorkoutNotificationId)
            ordinaryPosted = false
        }
    }
}

internal fun Context.resolveActiveWorkoutPermissionState(): ActiveWorkoutNotificationPermissionState {
    val manager = getSystemService(NotificationManager::class.java)
    val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return ActiveWorkoutNotificationPermissionState.resolve(
        sdkInt = Build.VERSION.SDK_INT,
        postNotificationsGranted = granted,
        appNotificationsEnabled = manager.areNotificationsEnabled(),
        channelNotificationsEnabled = manager.getNotificationChannel(ActiveWorkoutNotificationChannelId)
            ?.importance != NotificationManager.IMPORTANCE_NONE
    )
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
        .setVisibility(Notification.VISIBILITY_SECRET)
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
