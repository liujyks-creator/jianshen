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
import com.liujyks.trainflow.app.MainActivity
import com.liujyks.trainflow.core.health.WorkoutHeartRateService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface ActiveWorkoutNotificationController {
    val permissionState: StateFlow<ActiveWorkoutNotificationPermissionState>
    val currentTraining: StateFlow<ActiveWorkoutNotificationState?>
    val foregroundState: StateFlow<ActiveWorkoutForegroundState>
    fun initialize()
    fun beginSession(sessionId: String): ActiveWorkoutNotificationProducer
    fun update(
        producer: ActiveWorkoutNotificationProducer,
        stateVersion: Long,
        state: ActiveWorkoutNotificationState
    ): ActiveWorkoutNotificationUpdateResult
    fun release(producer: ActiveWorkoutNotificationProducer): ActiveWorkoutNotificationUpdateResult
    fun refreshPermission(): ActiveWorkoutNotificationPermissionState
    fun setForegroundDesired(eligible: Boolean, visible: Boolean, heartRateText: String)
    fun promotionContent(generation: Long): Notification?
    fun foregroundContent(generation: Long, writer: Any): Notification?
    fun foregroundPromoted(generation: Long, writer: Any)
    fun foregroundPromotionFailed(generation: Long, error: Throwable)
    fun foregroundUpdateFailed(generation: Long, writer: Any, error: Throwable)
    fun foregroundReleased(generation: Long, writer: Any)
    fun foregroundReleaseFailed(generation: Long, writer: Any, error: Throwable)
    fun foregroundDestroyed(generation: Long, writer: Any)
}

internal class AndroidActiveWorkoutNotificationController(
    private val context: Context
) : ActiveWorkoutNotificationController {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val mutablePermissionState = MutableStateFlow(appContext.resolveActiveWorkoutPermissionState())
    override val permissionState: StateFlow<ActiveWorkoutNotificationPermissionState> = mutablePermissionState
    private val mutableCurrentTraining = MutableStateFlow<ActiveWorkoutNotificationState?>(null)
    override val currentTraining: StateFlow<ActiveWorkoutNotificationState?> = mutableCurrentTraining
    private val mutableForegroundState = MutableStateFlow(ActiveWorkoutForegroundState())
    override val foregroundState: StateFlow<ActiveWorkoutForegroundState> = mutableForegroundState
    private var initialized = false
    private var nextToken = 0L
    private var currentProducer: ActiveWorkoutNotificationProducer? = null
    private var lastAcceptedVersion = 0L
    private var latestDesiredState: ActiveWorkoutNotificationState? = null
    private var ordinaryPosted = false
    private var foregroundDesired = false
    private var visible = false
    private var heartRateText = "等待心率数据"
    private var nextHandoffGeneration = 0L
    private var pendingPromotion: Long? = null
    private var pendingPromotionContent: ActiveWorkoutNotificationContent? = null
    private var activeGeneration: Long? = null
    private var activeWriter: Any? = null
    private var pendingRelease: Long? = null
    private var releaseUnconfirmed = false
    private var promotionBlocked = false
    private var lastForegroundContent: ActiveWorkoutNotificationContent? = null

    override fun initialize() {
        if (initialized) return
        ensureActiveWorkoutChannel(appContext)
        notificationManager.cancel(ActiveWorkoutNotificationId)
        initialized = true
        mutablePermissionState.value = appContext.resolveActiveWorkoutPermissionState()
    }

    override fun beginSession(sessionId: String): ActiveWorkoutNotificationProducer {
        check(initialized)
        mutableCurrentTraining.value = null
        foregroundDesired = false
        promotionBlocked = false
        reconcile()
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
        if (!state.isRunning()) {
            currentProducer = null
            latestDesiredState = null
            mutableCurrentTraining.value = null
            foregroundDesired = false
            reconcile()
            return ActiveWorkoutNotificationUpdateResult.Cleared(
                ActiveWorkoutNotificationClearReason.READY_OR_TERMINAL
            )
        }
        latestDesiredState = state
        mutableCurrentTraining.value = state
        reconcile()
        if (pendingPromotion != null || activeGeneration != null || releaseUnconfirmed) {
            return ActiveWorkoutNotificationUpdateResult.Deferred(mutableForegroundState.value.phase)
        }
        return ActiveWorkoutNotificationPolicy.evaluate(state, mutablePermissionState.value)
    }

    override fun release(producer: ActiveWorkoutNotificationProducer): ActiveWorkoutNotificationUpdateResult {
        if (producer != currentProducer) return ActiveWorkoutNotificationUpdateResult.Ignored(
            ActiveWorkoutNotificationIgnoredReason.STALE_PRODUCER, "训练场次已结束或被替换。"
        )
        currentProducer = null
        latestDesiredState = null
        mutableCurrentTraining.value = null
        foregroundDesired = false
        promotionBlocked = false
        reconcile()
        if (pendingPromotion != null || activeGeneration != null || releaseUnconfirmed) {
            return ActiveWorkoutNotificationUpdateResult.Deferred(mutableForegroundState.value.phase)
        }
        return ActiveWorkoutNotificationUpdateResult.Cleared(ActiveWorkoutNotificationClearReason.ROUTE_DISPOSED)
    }

    override fun refreshPermission(): ActiveWorkoutNotificationPermissionState {
        val current = appContext.resolveActiveWorkoutPermissionState()
        mutablePermissionState.value = current
        reconcile()
        return current
    }

    override fun setForegroundDesired(eligible: Boolean, visible: Boolean, heartRateText: String) {
        if (!eligible) promotionBlocked = false
        this.foregroundDesired = eligible
        this.visible = visible
        this.heartRateText = heartRateText
        reconcile()
    }

    override fun promotionContent(generation: Long): Notification? {
        if (pendingPromotion != generation) return null
        return requireNotNull(pendingPromotionContent).toActiveWorkoutNotification(appContext)
    }

    override fun foregroundContent(generation: Long, writer: Any): Notification? {
        if (activeGeneration != generation || activeWriter !== writer || pendingRelease != null) return null
        return requireNotNull(latestDesiredState).foregroundContent().toActiveWorkoutNotification(appContext)
    }

    override fun foregroundPromoted(generation: Long, writer: Any) {
        if (pendingPromotion != generation) return
        pendingPromotion = null
        pendingPromotionContent = null
        activeGeneration = generation
        activeWriter = writer
        lastForegroundContent = latestDesiredState?.foregroundContent()
        mutableForegroundState.value = ActiveWorkoutForegroundState(
            ActiveWorkoutForegroundPhase.ACTIVE, generation
        )
        reconcile()
    }

    override fun foregroundPromotionFailed(generation: Long, error: Throwable) {
        if (pendingPromotion != generation) return
        pendingPromotion = null
        pendingPromotionContent = null
        promotionBlocked = true
        mutableForegroundState.value = ActiveWorkoutForegroundState(failure = error)
        reconcile()
    }

    override fun foregroundUpdateFailed(generation: Long, writer: Any, error: Throwable) {
        if (activeGeneration != generation || activeWriter !== writer || pendingRelease != null) return
        mutableForegroundState.value = ActiveWorkoutForegroundState(
            ActiveWorkoutForegroundPhase.ACTIVE, generation, error
        )
    }

    override fun foregroundReleased(generation: Long, writer: Any) {
        if (pendingRelease != generation || activeGeneration != generation || activeWriter !== writer ||
            releaseUnconfirmed
        ) return
        pendingRelease = null
        activeGeneration = null
        activeWriter = null
        lastForegroundContent = null
        mutableForegroundState.value = ActiveWorkoutForegroundState()
        reconcile()
    }

    override fun foregroundReleaseFailed(generation: Long, writer: Any, error: Throwable) {
        if (pendingRelease != generation || activeGeneration != generation || activeWriter !== writer) return
        releaseUnconfirmed = true
        mutableForegroundState.value = ActiveWorkoutForegroundState(
            ActiveWorkoutForegroundPhase.RELEASE_UNCONFIRMED, generation, error
        )
    }

    override fun foregroundDestroyed(generation: Long, writer: Any) {
        if (activeGeneration != generation || activeWriter !== writer || releaseUnconfirmed) return
        releaseUnconfirmed = true
        mutableForegroundState.value = ActiveWorkoutForegroundState(
            ActiveWorkoutForegroundPhase.RELEASE_UNCONFIRMED, generation,
            IllegalStateException("Foreground service destroyed before release acknowledgement")
        )
    }

    private fun reconcile() {
        if (releaseUnconfirmed) return
        val state = latestDesiredState?.takeIf { currentProducer != null && it.isRunning() }
        val targetFgs = state != null && foregroundDesired
        val generation = activeGeneration
        if (generation != null) {
            if (!targetFgs) {
                if (pendingRelease == null) {
                    pendingRelease = generation
                    mutableForegroundState.value = ActiveWorkoutForegroundState(
                        ActiveWorkoutForegroundPhase.RELEASING, generation
                    )
                    try {
                        appContext.startService(WorkoutHeartRateService.releaseIntent(appContext, generation))
                    } catch (error: RuntimeException) {
                        foregroundReleaseFailed(generation, requireNotNull(activeWriter), error)
                    }
                }
            } else if (pendingRelease == null) {
                val content = state!!.foregroundContent()
                if (content != lastForegroundContent) {
                    lastForegroundContent = content
                    try {
                        appContext.startService(WorkoutHeartRateService.updateIntent(appContext, generation))
                    } catch (error: RuntimeException) {
                        mutableForegroundState.value = ActiveWorkoutForegroundState(
                            ActiveWorkoutForegroundPhase.ACTIVE, generation, error
                        )
                    }
                }
            }
            return
        }
        if (pendingPromotion != null) return
        if (targetFgs && visible && !promotionBlocked) {
            clearPostedNotification()
            val newGeneration = ++nextHandoffGeneration
            pendingPromotion = newGeneration
            pendingPromotionContent = state!!.foregroundContent()
            mutableForegroundState.value = ActiveWorkoutForegroundState(
                ActiveWorkoutForegroundPhase.STARTING, newGeneration
            )
            try {
                val intent = WorkoutHeartRateService.promoteIntent(appContext, newGeneration)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            } catch (error: RuntimeException) {
                foregroundPromotionFailed(newGeneration, error)
            }
            return
        }
        if (state == null) {
            clearPostedNotification()
        } else {
            publishDesiredState(state)
        }
    }

    private fun ActiveWorkoutNotificationState.isRunning() =
        status == com.liujyks.trainflow.core.model.SessionStatus.ACTIVE ||
            status == com.liujyks.trainflow.core.model.SessionStatus.PAUSED

    private fun ActiveWorkoutNotificationState.foregroundContent() =
        ActiveWorkoutNotificationContentFactory.createForeground(this, heartRateText)

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
                result
            }

            is ActiveWorkoutNotificationUpdateResult.Ignored -> {
                clearPostedNotification()
                result
            }
            is ActiveWorkoutNotificationUpdateResult.Deferred -> result
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
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    return PendingIntent.getActivity(
        context,
        ActiveWorkoutNotificationId,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
