package com.liujyks.trainflow.core.health

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.pm.ServiceInfo
import android.os.Looper
import com.liujyks.trainflow.app.MainActivity
import com.liujyks.trainflow.app.TrainFlowApplication
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.notifications.ActiveWorkoutForegroundPhase
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowService
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(application = TrainFlowApplication::class, sdk = [35])
class WorkoutHeartRateServiceTest {
    @Test
    fun servicePromotesImmediatelyWithDeniedNotificationPermissionAndIsNotSticky() = runBlocking {
        for (permissionGranted in listOf(true, false)) {
            val service = Robolectric.buildService(WorkoutHeartRateService::class.java).create().get()
            val application = service.application as TrainFlowApplication
            shadowOf(application).grantPermissions(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
            shadowOf(application.getSystemService(BluetoothManager::class.java).adapter)
                .setEnabled(true)
            if (permissionGranted) {
                shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
            }
            val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
            application.setHeartRateEnabled(true)
            application.selectHeartRateDevice("AA:BB:CC:DD:EE:71", "Band fixture")
            shadowOf(Looper.getMainLooper()).idle()
            val controller = application.activeWorkoutNotifications
            controller.refreshPermission()
            val producer = controller.beginSession("service-$permissionGranted")
            controller.update(producer, 1, state(producer.sessionId))
            shadowOf(Looper.getMainLooper()).idle()
            controller.setForegroundDesired(true, true, "等待心率数据")
            val generation = requireNotNull(controller.foregroundState.value.generation)

            val result = service.onStartCommand(
                WorkoutHeartRateService.promoteIntent(service, generation), 0, 1
            )

            assertEquals(Service.START_NOT_STICKY, result)
            assertEquals(7_200, shadowOf(service).lastForegroundNotificationId)
            assertNotNull(shadowOf(service).lastForegroundNotification)
            assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                service.foregroundServiceType)
            assertTrue(controller.foregroundState.value.active)
            controller.setForegroundDesired(false, true, "等待心率数据")
            service.onStartCommand(WorkoutHeartRateService.releaseIntent(service, generation), 0, 2)
            service.onDestroy()
            controller.release(producer)
            activity.close()
        }
    }

    @Test
    @Config(shadows = [ThrowingStopForegroundShadow::class])
    fun serviceReportsPromotionReleaseAndDestroyFailuresWithoutFalseAcknowledgement() {
        val service = Robolectric.buildService(WorkoutHeartRateService::class.java).create().get()
        val application = service.application as TrainFlowApplication
        val controller = application.activeWorkoutNotifications
        val producer = controller.beginSession("failure")
        controller.update(producer, 1, state(producer.sessionId))
        controller.setForegroundDesired(true, true, "等待心率数据")
        val failedGeneration = requireNotNull(controller.foregroundState.value.generation)
        val promotionError = SecurityException("promotion denied")
        shadowOf(service).setThrowInStartForeground(promotionError)
        service.onStartCommand(WorkoutHeartRateService.promoteIntent(service, failedGeneration), 0, 1)
        assertSame(promotionError, controller.foregroundState.value.failure)
        assertEquals(ActiveWorkoutForegroundPhase.NONE, controller.foregroundState.value.phase)

        val releasedService = Robolectric.buildService(WorkoutHeartRateService::class.java).create().get()
        val releasedController = (releasedService.application as TrainFlowApplication).activeWorkoutNotifications
        releasedController.setForegroundDesired(false, true, "等待心率数据")
        releasedController.setForegroundDesired(true, true, "等待心率数据")
        val generation = requireNotNull(releasedController.foregroundState.value.generation)
        releasedService.onStartCommand(
            WorkoutHeartRateService.promoteIntent(releasedService, generation), 0, 2
        )
        releasedController.setForegroundDesired(false, true, "等待心率数据")
        val releaseError = SecurityException("release denied")
        ThrowingStopForegroundShadow.failure = releaseError
        try {
            releasedService.onStartCommand(
                WorkoutHeartRateService.releaseIntent(releasedService, generation), 0, 3
            )
        } finally {
            ThrowingStopForegroundShadow.failure = null
        }
        assertEquals(ActiveWorkoutForegroundPhase.RELEASE_UNCONFIRMED,
            releasedController.foregroundState.value.phase)
        assertSame(releaseError, releasedController.foregroundState.value.failure)
        releasedService.onDestroy()
        assertSame(releaseError, releasedController.foregroundState.value.failure)
    }

    private fun state(sessionId: String) = ActiveWorkoutNotificationState(
        sessionId = sessionId,
        mode = WorkoutMode.TIMED,
        planTitle = "测试训练",
        status = SessionStatus.ACTIVE,
        phaseLabel = "动作",
        primaryText = "进行中",
        timerText = "00:03",
        progressText = "1 / 2",
        secondaryText = "下一步"
    )
}

@Implements(Service::class)
class ThrowingStopForegroundShadow : ShadowService() {
    @Implementation
    override fun stopForeground(flags: Int) {
        failure?.let { throw it }
        super.stopForeground(flags)
    }

    companion object {
        var failure: RuntimeException? = null
    }
}
