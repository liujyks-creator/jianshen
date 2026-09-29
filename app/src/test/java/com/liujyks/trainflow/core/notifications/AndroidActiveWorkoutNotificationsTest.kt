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
import org.junit.Assert.assertNotEquals
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
    fun foregroundHandoffsKeepLatestStateAndRejectStaleAcknowledgements() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = application.getSystemService(NotificationManager::class.java)
        val controller = AndroidActiveWorkoutNotificationController(application)
        controller.initialize()
        val writer1 = Any()
        val writer2 = Any()

        for (mode in WorkoutMode.entries) {
            val producer = controller.beginSession("session-$mode")
            controller.update(producer, 1, state(producer.sessionId, "开始", mode = mode))
            assertEquals("开始 · 00:03", shadowOf(manager).getNotification(7200)
                .extras.getCharSequence(Notification.EXTRA_TEXT).toString())

            controller.setForegroundDesired(true, true, "等待心率数据")
            val firstGeneration = requireNotNull(controller.foregroundState.value.generation)
            assertNull(shadowOf(manager).getNotification(7200))
            assertEquals(ActiveWorkoutForegroundPhase.STARTING, controller.foregroundState.value.phase)
            controller.foregroundPromoted(firstGeneration, writer1)
            assertTrue(controller.foregroundState.value.active)
            controller.update(producer, 2, state(producer.sessionId, "更新", mode = mode))
            assertEquals("更新 · 00:03", controller.foregroundContent(firstGeneration, writer1)
                ?.extras?.getCharSequence(Notification.EXTRA_TEXT).toString())

            controller.setForegroundDesired(false, true, "等待心率数据")
            controller.update(producer, 3, state(producer.sessionId, "交接期间", mode = mode))
            controller.foregroundReleased(firstGeneration, writer2)
            controller.foregroundReleased(firstGeneration - 1L, writer1)
            assertEquals(ActiveWorkoutForegroundPhase.RELEASING, controller.foregroundState.value.phase)
            assertNull(shadowOf(manager).getNotification(7200))
            controller.foregroundReleased(firstGeneration, writer1)
            assertEquals("交接期间 · 00:03", shadowOf(manager).getNotification(7200)
                .extras.getCharSequence(Notification.EXTRA_TEXT).toString())

            controller.setForegroundDesired(true, true, "心率 92 bpm")
            val secondGeneration = requireNotNull(controller.foregroundState.value.generation)
            assertNotEquals(firstGeneration, secondGeneration)
            controller.foregroundPromoted(secondGeneration, writer2)
            assertTrue(controller.foregroundContent(secondGeneration, writer2)
                ?.extras?.getCharSequence(Notification.EXTRA_SUB_TEXT).toString().contains("92 bpm"))
            controller.update(producer, 4, state(producer.sessionId, "结束", SessionStatus.COMPLETED, mode))
            controller.foregroundReleased(firstGeneration, writer1)
            assertEquals(ActiveWorkoutForegroundPhase.RELEASING, controller.foregroundState.value.phase)
            controller.foregroundReleased(secondGeneration, writer2)
            controller.foregroundReleased(secondGeneration, writer2)
            assertEquals(ActiveWorkoutForegroundPhase.NONE, controller.foregroundState.value.phase)
            assertNull(shadowOf(manager).getNotification(7200))
        }
    }

    @Test
    fun releaseUnconfirmedFreezesOrdinaryAndDoesNotAcceptLateAcknowledgements() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = application.getSystemService(NotificationManager::class.java)
        val controller = AndroidActiveWorkoutNotificationController(application)
        controller.initialize()
        val producer = controller.beginSession("session-A")
        controller.update(producer, 1, state("session-A", "初始"))
        controller.setForegroundDesired(true, true, "等待心率数据")
        val generation = requireNotNull(controller.foregroundState.value.generation)
        val writer = Any()
        controller.foregroundPromoted(generation, writer)
        controller.setForegroundDesired(false, false, "正在重新连接")
        val original = SecurityException("stopForeground failed")
        controller.foregroundReleaseFailed(generation, writer, original)
        controller.update(producer, 2, state("session-A", "新状态"))
        controller.foregroundReleased(generation, writer)
        assertEquals(ActiveWorkoutForegroundPhase.RELEASE_UNCONFIRMED, controller.foregroundState.value.phase)
        assertTrue(controller.foregroundState.value.failure === original)
        assertNull(shadowOf(manager).getNotification(7200))
        controller.release(producer)
        controller.release(producer)
        controller.foregroundReleased(generation, writer)
        assertNull(shadowOf(manager).getNotification(7200))

        val destroyedController = AndroidActiveWorkoutNotificationController(application)
        destroyedController.initialize()
        val second = destroyedController.beginSession("session-B")
        destroyedController.update(second, 1, state("session-B", "B"))
        destroyedController.setForegroundDesired(true, true, "等待心率数据")
        val secondGeneration = requireNotNull(destroyedController.foregroundState.value.generation)
        val secondWriter = Any()
        destroyedController.foregroundPromoted(secondGeneration, secondWriter)
        destroyedController.foregroundDestroyed(secondGeneration, secondWriter)
        destroyedController.setForegroundDesired(false, false, "等待心率数据")
        destroyedController.foregroundReleased(secondGeneration, secondWriter)
        assertEquals(ActiveWorkoutForegroundPhase.RELEASE_UNCONFIRMED,
            destroyedController.foregroundState.value.phase)
        assertNull(shadowOf(manager).getNotification(7200))
    }

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
        status: SessionStatus = SessionStatus.ACTIVE,
        mode: WorkoutMode = WorkoutMode.TIMED
    ) = ActiveWorkoutNotificationState(
        sessionId = sessionId,
        mode = mode,
        planTitle = "同一个计划",
        status = status,
        phaseLabel = "动作",
        primaryText = primary,
        timerText = "00:03",
        progressText = "1 / 2",
        secondaryText = "下一步"
    )
}
