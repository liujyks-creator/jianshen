package com.liujyks.trainflow.core.health

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.liujyks.trainflow.app.TrainFlowApplication
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationId

/** Owns only the platform foreground writer; the Application retains BLE and training state. */
internal class WorkoutHeartRateService : Service() {
    private val controller get() = (application as TrainFlowApplication).activeWorkoutNotifications
    private var ownedGeneration: Long? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val generation = intent?.getLongExtra(EXTRA_GENERATION, -1L) ?: -1L
        when (intent?.action) {
            ACTION_PROMOTE -> {
                val content = controller.promotionContent(generation)
                if (content != null) {
                    try {
                        ServiceCompat.startForeground(
                            this,
                            ActiveWorkoutNotificationId,
                            content,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                            } else {
                                0
                            }
                        )
                        ownedGeneration = generation
                        controller.foregroundPromoted(generation, this)
                    } catch (error: RuntimeException) {
                        controller.foregroundPromotionFailed(generation, error)
                        stopSelfResult(startId)
                    }
                } else {
                    stopSelfResult(startId)
                }
            }
            ACTION_UPDATE -> {
                if (ownedGeneration == generation) {
                    val content = controller.foregroundContent(generation, this)
                    if (content != null) {
                        try {
                            getSystemService(NotificationManager::class.java)
                                .notify(ActiveWorkoutNotificationId, content)
                        } catch (error: RuntimeException) {
                            controller.foregroundUpdateFailed(generation, this, error)
                        }
                    }
                }
            }
            ACTION_RELEASE -> {
                if (ownedGeneration == generation) {
                    try {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        ownedGeneration = null
                        controller.foregroundReleased(generation, this)
                        stopSelfResult(startId)
                    } catch (error: RuntimeException) {
                        controller.foregroundReleaseFailed(generation, this, error)
                    }
                }
            }
            else -> stopSelfResult(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ownedGeneration?.let { controller.foregroundDestroyed(it, this) }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_GENERATION = "com.liujyks.trainflow.foregroundGeneration"
        private const val ACTION_PROMOTE = "com.liujyks.trainflow.action.PROMOTE_WORKOUT_HEART_RATE"
        private const val ACTION_UPDATE = "com.liujyks.trainflow.action.UPDATE_WORKOUT_HEART_RATE"
        private const val ACTION_RELEASE = "com.liujyks.trainflow.action.RELEASE_WORKOUT_HEART_RATE"

        fun promoteIntent(context: Context, generation: Long): Intent =
            Intent(context, WorkoutHeartRateService::class.java)
                .setAction(ACTION_PROMOTE).putExtra(EXTRA_GENERATION, generation)

        fun updateIntent(context: Context, generation: Long): Intent =
            Intent(context, WorkoutHeartRateService::class.java)
                .setAction(ACTION_UPDATE).putExtra(EXTRA_GENERATION, generation)

        fun releaseIntent(context: Context, generation: Long): Intent =
            Intent(context, WorkoutHeartRateService::class.java)
                .setAction(ACTION_RELEASE).putExtra(EXTRA_GENERATION, generation)
    }
}
