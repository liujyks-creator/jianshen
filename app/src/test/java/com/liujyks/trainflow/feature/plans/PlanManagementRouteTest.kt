package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.PlanReminder
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

    @Test
    fun editedPlanWithEnabledReminderCancelsAndReschedulesExistingAlarm() {
        val planId = "plan-timed-default"
        val nowEpochMillis = Instant.parse("2026-06-03T11:30:00Z").toEpochMilli()
        val plan = buildDefaultPlanManagementState()
            .plans
            .first { it.id == planId }
            .copy(
                title = "更新后的计时提醒",
                reminder = PlanReminder(
                    enabled = true,
                    scheduleAt = "2026-06-04T11:30:00Z"
                )
            )
        val scheduler = RecordingPlanReminderScheduler(
            nowEpochMillis = nowEpochMillis,
            activeAlarmPlanIds = mutableSetOf(planId)
        )

        val result = dispatchPlanReminderReplacementForEditedPlan(
            plan = plan,
            wasEditingExistingPlan = true,
            permissionState = grantedPermission(),
            scheduler = scheduler
        )

        assertTrue(result is PlanReminderScheduleResult.Scheduled)
        assertEquals(listOf(planId), scheduler.cancelledPlanIds)
        assertEquals(listOf(planId), scheduler.scheduledAlarmRequests.map { it.planId })
        assertEquals("更新后的计时提醒", scheduler.scheduledAlarmRequests.single().planTitle)
        assertTrue(scheduler.activeAlarmPlanIds.contains(planId))
    }

    @Test
    fun editedPlanWithDisabledOrNullReminderClearsExistingAlarm() {
        val planId = "plan-timed-default"
        val nowEpochMillis = Instant.parse("2026-06-03T11:30:00Z").toEpochMilli()
        val basePlan = buildDefaultPlanManagementState()
            .plans
            .first { it.id == planId }
        val disabledPlan = basePlan.copy(
            reminder = PlanReminder(enabled = false, scheduleAt = null)
        )
        val nullReminderPlan = basePlan.copy(reminder = null)
        val disabledScheduler = RecordingPlanReminderScheduler(
            nowEpochMillis = nowEpochMillis,
            activeAlarmPlanIds = mutableSetOf(planId)
        )
        val nullScheduler = RecordingPlanReminderScheduler(
            nowEpochMillis = nowEpochMillis,
            activeAlarmPlanIds = mutableSetOf(planId)
        )

        val disabledResult = dispatchPlanReminderReplacementForEditedPlan(
            plan = disabledPlan,
            wasEditingExistingPlan = true,
            permissionState = grantedPermission(),
            scheduler = disabledScheduler
        )
        val nullResult = dispatchPlanReminderReplacementForEditedPlan(
            plan = nullReminderPlan,
            wasEditingExistingPlan = true,
            permissionState = grantedPermission(),
            scheduler = nullScheduler
        )

        assertEquals(PlanReminderScheduleResult.Cancelled(planId), disabledResult)
        assertEquals(PlanReminderScheduleResult.Cancelled(planId), nullResult)
        assertFalse(disabledScheduler.activeAlarmPlanIds.contains(planId))
        assertFalse(nullScheduler.activeAlarmPlanIds.contains(planId))
        assertTrue(disabledScheduler.scheduledAlarmRequests.isEmpty())
        assertTrue(nullScheduler.scheduledAlarmRequests.isEmpty())
    }

    @Test
    fun createModeSaveDoesNotTouchPlanReminderScheduler() {
        val planId = "plan-timed-new"
        val nowEpochMillis = Instant.parse("2026-06-03T11:30:00Z").toEpochMilli()
        val plan = buildDefaultTimedPlanEditorState().toWorkoutPlan(planId = planId)
        val scheduler = RecordingPlanReminderScheduler(nowEpochMillis = nowEpochMillis)

        val result = dispatchPlanReminderReplacementForEditedPlan(
            plan = plan,
            wasEditingExistingPlan = false,
            permissionState = grantedPermission(),
            scheduler = scheduler
        )

        assertEquals(null, result)
        assertTrue(scheduler.cancelledPlanIds.isEmpty())
        assertTrue(scheduler.scheduledAlarmRequests.isEmpty())
    }

    private fun grantedPermission(): PlanReminderNotificationPermissionState {
        return PlanReminderNotificationPermissionState.resolve(
            sdkInt = 33,
            postNotificationsGranted = true
        )
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
