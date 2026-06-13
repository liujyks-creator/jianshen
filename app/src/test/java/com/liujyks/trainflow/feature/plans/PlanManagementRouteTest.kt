package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.notifications.PlanReminderNotificationPermissionState
import com.liujyks.trainflow.core.notifications.PlanReminderScheduleIgnoredReason
import com.liujyks.trainflow.core.notifications.PlanReminderSchedulePolicy
import com.liujyks.trainflow.core.notifications.PlanReminderScheduleRequest
import com.liujyks.trainflow.core.notifications.PlanReminderScheduleResult
import com.liujyks.trainflow.core.notifications.PlanReminderScheduler
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanManagementRouteTest {
    @Test
    fun updatingPlanReminderWithDeniedPermissionPersistsNewTimeAndCancelsStaleAlarm() {
        val planId = "plan-timed-default"
        val oldScheduleAt = "2026-06-04T11:30:00Z"
        val newScheduleAt = "2026-06-04T12:45:00Z"
        val nowEpochMillis = Instant.parse("2026-06-03T11:30:00Z").toEpochMilli()
        val deniedPermission = PlanReminderNotificationPermissionState.resolve(
            sdkInt = 33,
            postNotificationsGranted = false
        )
        val stateWithOldReminder = buildDefaultPlanManagementState()
            .setPlanReminder(
                planId = planId,
                scheduleAt = oldScheduleAt,
                nowEpochMillis = nowEpochMillis
            )
            .updateNotificationPermissionState(deniedPermission)
        val scheduler = RecordingPlanReminderScheduler(
            nowEpochMillis = nowEpochMillis,
            activeAlarmPlanIds = mutableSetOf(planId)
        )

        val updated = stateWithOldReminder.setPlanReminder(
            planId = planId,
            scheduleAt = newScheduleAt,
            nowEpochMillis = nowEpochMillis
        )
        dispatchPlanReminderReplacement(
            plan = requireNotNull(updated.selectedPlan),
            permissionState = deniedPermission,
            scheduler = scheduler
        )

        assertEquals(oldScheduleAt, requireNotNull(stateWithOldReminder.selectedPlan?.reminder).scheduleAt)
        assertEquals(newScheduleAt, requireNotNull(updated.selectedPlan?.reminder).scheduleAt)
        assertTrue(requireNotNull(updated.selectedPlan?.reminder).enabled)
        assertEquals(listOf(planId), scheduler.cancelledPlanIds)
        assertTrue(scheduler.scheduledAlarmRequests.isEmpty())
        assertFalse(scheduler.activeAlarmPlanIds.contains(planId))
        val ignored = scheduler.results.single() as PlanReminderScheduleResult.Ignored
        assertEquals(PlanReminderScheduleIgnoredReason.NOTIFICATION_PERMISSION_DENIED, ignored.reason)
    }

    private class RecordingPlanReminderScheduler(
        private val nowEpochMillis: Long,
        val activeAlarmPlanIds: MutableSet<String> = mutableSetOf()
    ) : PlanReminderScheduler {
        val cancelledPlanIds = mutableListOf<String>()
        val scheduledAlarmRequests = mutableListOf<PlanReminderScheduleRequest>()
        val results = mutableListOf<PlanReminderScheduleResult>()

        override fun schedule(request: PlanReminderScheduleRequest): PlanReminderScheduleResult {
            val result = PlanReminderSchedulePolicy.evaluate(
                request = request,
                nowEpochMillis = nowEpochMillis
            )
            if (result is PlanReminderScheduleResult.Scheduled) {
                activeAlarmPlanIds += result.request.planId
                scheduledAlarmRequests += result.request
            }
            results += result
            return result
        }

        override fun cancel(planId: String) {
            cancelledPlanIds += planId
            activeAlarmPlanIds -= planId
        }
    }
}
