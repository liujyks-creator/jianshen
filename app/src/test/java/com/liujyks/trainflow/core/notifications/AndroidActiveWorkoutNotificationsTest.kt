package com.liujyks.trainflow.core.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AndroidActiveWorkoutNotificationsTest {
    @Test
    fun samePlanNewSessionRejectsOldProducerAndVersion() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = application.getSystemService(NotificationManager::class.java)
        val controller = AndroidActiveWorkoutNotificationController(application)
        controller.initialize()

        val first = controller.beginSession("session-A")
        controller.update(first, 1, state("session-A", "A"))
        val second = controller.beginSession("session-B")
        controller.update(second, 1, state("session-B", "B-1"))
        controller.update(second, 3, state("session-B", "B-3"))

        assertTrue(controller.update(first, 2, state("session-A", "A-late")) is ActiveWorkoutNotificationUpdateResult.Ignored)
        assertTrue(controller.release(first) is ActiveWorkoutNotificationUpdateResult.Ignored)
        assertTrue(controller.update(second, 2, state("session-B", "B-old")) is ActiveWorkoutNotificationUpdateResult.Ignored)
        assertEquals("B-3 · 00:03", shadowOf(manager).getNotification(7200).extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test
    fun cleanupDoesNotClearNewSessionOrReviveFinishedSession() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = application.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(ActiveWorkoutNotificationChannelId, "训练进行中", NotificationManager.IMPORTANCE_LOW))
        manager.notify(7200, Notification.Builder(application, ActiveWorkoutNotificationChannelId)
            .setSmallIcon(com.liujyks.trainflow.R.drawable.ic_launcher_foreground)
            .setContentText("旧通知").build())
        assertEquals("旧通知", shadowOf(manager).getNotification(7200).extras.getCharSequence(Notification.EXTRA_TEXT).toString())

        val controller = AndroidActiveWorkoutNotificationController(application)
        controller.initialize()
        assertNull(shadowOf(manager).getNotification(7200))
        val first = controller.beginSession("session-A")
        controller.update(first, 1, state("session-A", "A"))
        controller.update(first, 2, state("session-A", "结束", SessionStatus.COMPLETED))
        assertNull(shadowOf(manager).getNotification(7200))
        assertTrue(controller.update(first, 3, state("session-A", "复活")) is ActiveWorkoutNotificationUpdateResult.Ignored)

        val second = controller.beginSession("session-B")
        controller.update(second, 1, state("session-B", "B"))
        controller.release(second)
        assertNull(shadowOf(manager).getNotification(7200))
        assertTrue(controller.update(second, 2, state("session-B", "复活")) is ActiveWorkoutNotificationUpdateResult.Ignored)

        val third = controller.beginSession("session-C")
        controller.update(third, 1, state("session-C", "C"))
        controller.initialize()
        assertTrue(controller.release(first) is ActiveWorkoutNotificationUpdateResult.Ignored)
        assertTrue(controller.release(second) is ActiveWorkoutNotificationUpdateResult.Ignored)
        assertEquals("C · 00:03", shadowOf(manager).getNotification(7200).extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test
    fun permissionRefreshUsesCurrentStateAndSecretVisibility() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = application.getSystemService(NotificationManager::class.java)
        val controller = AndroidActiveWorkoutNotificationController(application)
        controller.initialize()
        val producer = controller.beginSession("session-A")
        controller.update(producer, 1, state("session-A", "A-1"))

        shadowOf(manager).setNotificationsEnabled(false)
        val denied = controller.update(producer, 2, state("session-A", "A-2"))
        assertFalse(denied is ActiveWorkoutNotificationUpdateResult.Posted)
        assertFalse(controller.refreshPermission().canPostNotifications)
        assertNull(shadowOf(manager).getNotification(7200))

        shadowOf(manager).setNotificationsEnabled(true)
        assertTrue(controller.refreshPermission().canPostNotifications)
        val restored = shadowOf(manager).getNotification(7200)
        assertEquals("A-2 · 00:03", restored.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(Notification.VISIBILITY_SECRET, restored.visibility)
    }

    private fun state(
        sessionId: String,
        primary: String,
        status: SessionStatus = SessionStatus.ACTIVE
    ) = ActiveWorkoutNotificationState(
        sessionId = sessionId,
        mode = WorkoutMode.TIMED,
        planTitle = "同一个计划",
        status = status,
        phaseLabel = "动作",
        primaryText = primary,
        timerText = "00:03",
        progressText = "1 / 2",
        secondaryText = "下一步"
    )
}
