package com.liujyks.trainflow.core.notifications

import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWorkoutNotificationContractsTest {
    @Test
    fun activeWorkoutContentShowsSummaryWithoutPreciseBackgroundPromise() {
        val content = ActiveWorkoutNotificationContentFactory.create(
            state = state(status = SessionStatus.ACTIVE)
        )

        assertEquals(ActiveWorkoutNotificationChannelId, content.channelId)
        assertEquals(ActiveWorkoutNotificationId, content.notificationId)
        assertEquals("计时训练进行中", content.title)
        assertTrue(content.text.contains("开合跳"))
        assertTrue(content.text.contains("02:10"))
        assertTrue(content.bigText.contains("普通状态提示"))
        assertTrue(content.bigText.contains("不保证后台精确计时"))
        assertTrue(content.ongoing)
        assertFalse(content.bigText.contains("闹铃级"))
        assertFalse(content.bigText.contains("强提醒"))
        assertFalse(content.bigText.contains("医疗"))
    }

    @Test
    fun pausedWorkoutContentStaysOngoingButHonest() {
        val content = ActiveWorkoutNotificationContentFactory.create(
            state = state(status = SessionStatus.PAUSED, secondaryText = "时间已冻结；这不是后台精确计时承诺。")
        )

        assertEquals("训练已暂停", content.title)
        assertTrue(content.bigText.contains("时间已冻结"))
        assertTrue(content.ongoing)
    }

    @Test
    fun policyPostsOnlyActiveOrPausedWhenPermissionAllows() {
        val active = ActiveWorkoutNotificationPolicy.evaluate(
            state = state(status = SessionStatus.ACTIVE),
            permissionState = grantedPermission()
        )
        val paused = ActiveWorkoutNotificationPolicy.evaluate(
            state = state(status = SessionStatus.PAUSED),
            permissionState = grantedPermission()
        )

        assertTrue(active is ActiveWorkoutNotificationUpdateResult.Posted)
        assertTrue(paused is ActiveWorkoutNotificationUpdateResult.Posted)
    }

    @Test
    fun policyClearsReadyCompletedAndAbandonedStates() {
        listOf(SessionStatus.READY, SessionStatus.COMPLETED, SessionStatus.ABANDONED).forEach { status ->
            val result = ActiveWorkoutNotificationPolicy.evaluate(
                state = state(status = status),
                permissionState = grantedPermission()
            )

            assertEquals(
                ActiveWorkoutNotificationUpdateResult.Cleared(
                    reason = ActiveWorkoutNotificationClearReason.READY_OR_TERMINAL
                ),
                result
            )
        }
    }

    @Test
    fun policyIgnoresPermissionDeniedWithoutBlockingTraining() {
        val result = ActiveWorkoutNotificationPolicy.evaluate(
            state = state(status = SessionStatus.ACTIVE),
            permissionState = PlanReminderNotificationPermissionState.resolve(
                sdkInt = 33,
                postNotificationsGranted = false
            )
        )

        val ignored = result as ActiveWorkoutNotificationUpdateResult.Ignored
        assertEquals(ActiveWorkoutNotificationIgnoredReason.NOTIFICATION_PERMISSION_DENIED, ignored.reason)
        assertTrue(ignored.message.contains("训练仍可正常执行"))
    }

    @Test
    fun activeWorkoutChannelCopyDoesNotPromiseForegroundServiceOrAlarmBehavior() {
        assertTrue(ActiveWorkoutNotificationChannelDescription.contains("状态摘要"))
        assertTrue(ActiveWorkoutNotificationChannelDescription.contains("不承诺后台精确计时"))
        assertFalse(ActiveWorkoutNotificationChannelDescription.contains("前台服务"))
        assertFalse(ActiveWorkoutNotificationChannelDescription.contains("闹钟级"))
        assertFalse(ActiveWorkoutNotificationChannelDescription.contains("全屏"))
    }

    private fun state(
        status: SessionStatus,
        secondaryText: String = "下一步 · 深蹲"
    ): ActiveWorkoutNotificationState {
        return ActiveWorkoutNotificationState(
            sessionKey = "timed:plan-1",
            mode = WorkoutMode.TIMED,
            planTitle = "全身计时循环",
            status = status,
            phaseLabel = "动作",
            primaryText = "开合跳",
            timerText = "02:10",
            progressText = "步骤 1 / 8",
            secondaryText = secondaryText
        )
    }

    private fun grantedPermission(): PlanReminderNotificationPermissionState {
        return PlanReminderNotificationPermissionState.resolve(
            sdkInt = 33,
            postNotificationsGranted = true
        )
    }
}
