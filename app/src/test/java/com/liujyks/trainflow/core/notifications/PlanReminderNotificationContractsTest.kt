package com.liujyks.trainflow.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanReminderNotificationContractsTest {
    @Test
    fun android13PermissionStateMapsGrantedDeniedAndNotRequired() {
        val granted = PlanReminderNotificationPermissionState.resolve(
            sdkInt = 33,
            postNotificationsGranted = true
        )
        val denied = PlanReminderNotificationPermissionState.resolve(
            sdkInt = 33,
            postNotificationsGranted = false
        )
        val notRequired = PlanReminderNotificationPermissionState.resolve(
            sdkInt = 32,
            postNotificationsGranted = false
        )

        assertEquals(PlanReminderNotificationPermissionStatus.GRANTED, granted.status)
        assertTrue(granted.canPostNotifications)
        assertEquals(PlanReminderNotificationPermissionStatus.DENIED, denied.status)
        assertFalse(denied.canPostNotifications)
        assertTrue(denied.rationale.contains("Android 13+"))
        assertTrue(denied.rationale.contains("训练仍可正常使用"))
        assertTrue(denied.rationale.contains("训练中状态通知"))
        assertEquals(PlanReminderNotificationPermissionStatus.NOT_REQUIRED, notRequired.status)
        assertTrue(notRequired.canPostNotifications)
    }

    @Test
    fun schedulePolicySchedulesFutureEnabledReminderWhenPermissionAllows() {
        val request = request(
            scheduleAtEpochMillis = 2_000L,
            enabled = true,
            permissionState = grantedPermission()
        )
        val result = PlanReminderSchedulePolicy.evaluate(request, nowEpochMillis = 1_000L)

        val scheduled = result as PlanReminderScheduleResult.Scheduled
        assertEquals(request, scheduled.request)
        assertEquals(PlanReminderNotificationChannelId, scheduled.content.channelId)
        assertTrue(scheduled.content.text.contains("全身计时循环"))
        assertTrue(scheduled.content.subText.contains("普通通知"))
        assertFalse(scheduled.content.subText.contains("闹钟"))
    }

    @Test
    fun schedulePolicyCancelsDisabledReminder() {
        val result = PlanReminderSchedulePolicy.evaluate(
            request = request(scheduleAtEpochMillis = 2_000L, enabled = false),
            nowEpochMillis = 1_000L
        )

        assertEquals(PlanReminderScheduleResult.Cancelled("plan-1"), result)
    }

    @Test
    fun schedulePolicyRejectsMissingPastAndPermissionDeniedRequests() {
        val missing = PlanReminderSchedulePolicy.evaluate(
            request = request(scheduleAtEpochMillis = null, enabled = true),
            nowEpochMillis = 1_000L
        )
        val past = PlanReminderSchedulePolicy.evaluate(
            request = request(scheduleAtEpochMillis = 999L, enabled = true),
            nowEpochMillis = 1_000L
        )
        val denied = PlanReminderSchedulePolicy.evaluate(
            request = request(
                scheduleAtEpochMillis = 2_000L,
                enabled = true,
                permissionState = deniedPermission()
            ),
            nowEpochMillis = 1_000L
        )

        assertIgnored(PlanReminderScheduleIgnoredReason.MISSING_SCHEDULE_AT, missing)
        assertIgnored(PlanReminderScheduleIgnoredReason.PAST_SCHEDULE_AT, past)
        assertIgnored(PlanReminderScheduleIgnoredReason.NOTIFICATION_PERMISSION_DENIED, denied)
    }

    @Test
    fun channelCopyIsOrdinaryNotificationNotStrongAlarm() {
        assertTrue(PlanReminderNotificationChannelDescription.contains("普通通知"))
        assertTrue(PlanReminderNotificationChannelDescription.contains("系统延迟"))
        assertTrue(PlanReminderNotificationChannelDescription.contains("训练仍可正常使用"))
        assertFalse(PlanReminderNotificationChannelDescription.contains("精确闹钟"))
        assertFalse(PlanReminderNotificationChannelDescription.contains("全屏"))
    }

    private fun assertIgnored(
        reason: PlanReminderScheduleIgnoredReason,
        result: PlanReminderScheduleResult
    ) {
        val ignored = result as PlanReminderScheduleResult.Ignored
        assertEquals(reason, ignored.reason)
    }

    private fun request(
        scheduleAtEpochMillis: Long?,
        enabled: Boolean,
        permissionState: PlanReminderNotificationPermissionState = grantedPermission()
    ): PlanReminderScheduleRequest {
        return PlanReminderScheduleRequest(
            planId = "plan-1",
            planTitle = "全身计时循环",
            scheduleAtEpochMillis = scheduleAtEpochMillis,
            enabled = enabled,
            permissionState = permissionState
        )
    }

    private fun grantedPermission(): PlanReminderNotificationPermissionState {
        return PlanReminderNotificationPermissionState.resolve(
            sdkInt = 33,
            postNotificationsGranted = true
        )
    }

    private fun deniedPermission(): PlanReminderNotificationPermissionState {
        return PlanReminderNotificationPermissionState.resolve(
            sdkInt = 33,
            postNotificationsGranted = false
        )
    }
}
