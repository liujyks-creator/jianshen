package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.WorkoutSession
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.notifications.PlanReminderNotificationPermissionState
import com.liujyks.trainflow.core.notifications.PlanReminderNotificationPermissionStatus
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.ui.theme.SkinRegistry
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanManagementUiStateTest {
    @Test
    fun defaultPlanListShowsTimedAndStrengthSummaries() {
        val state = buildDefaultPlanManagementState()
        val items = state.listItems

        assertEquals(2, items.size)
        assertEquals(WorkoutMode.TIMED, items[0].mode)
        assertEquals("计时训练", items[0].modeLabel)
        assertEquals("计时", items[0].modeBadge)
        assertEquals("#F44336", items[0].planColorHex)
        assertTrue(items[0].summary.contains("预计"))
        assertTrue(items[0].detailSummary.contains("阶段提醒"))
        assertTrue(items[0].reminderSummary.contains("训练提醒未设置"))
        assertTrue(items[0].metrics.any { it.label == "时长" })
        assertTrue(items[0].metrics.any { it.label == "轮次" })
        assertTrue(items[0].metrics.any { it.label == "休息" })
        assertTrue(items[0].metrics.any { it.label == "提醒" && it.value == "未设置" })
        assertEquals(WorkoutMode.STRENGTH, items[1].mode)
        assertEquals("力量训练", items[1].modeLabel)
        assertEquals("力量", items[1].modeBadge)
        assertTrue(items[1].summary.contains("组"))
        assertTrue(items[1].detailSummary.contains("计划值预填"))
        assertTrue(items[1].metrics.any { it.label == "组数" })
        assertTrue(items[1].metrics.any { it.label == "休息" })
    }

    @Test
    fun modePillColorsMeetReadableContrastForBuiltInSkins() {
        SkinRegistry.skins.forEach { skin ->
            WorkoutMode.entries.forEach { mode ->
                val colors = modePillColors(mode = mode, skin = skin)

                assertTrue(
                    "${skin.id} ${mode.name} mode pill contrast should meet WCAG AA",
                    modePillContrastRatio(
                        contentColor = colors.contentColor,
                        containerColor = colors.containerColor
                    ) >= 4.5f
                )
            }
        }
    }

    @Test
    fun persistedPlanListCanStartEmptyAndRecoverSavedPlans() {
        val empty = PlanManagementScreenState(plans = emptyList())
        val savedTimedPlan = buildDefaultTimedPlanEditorState().toWorkoutPlan(
            planId = "saved-timed",
            timestamp = "2026-06-13T08:00:00Z"
        )
        val recovered = empty.withPlans(listOf(savedTimedPlan))

        assertTrue(empty.isEmpty)
        assertNull(recovered.selectedPlanId)
        assertNull(recovered.selectedPlan)
        assertNull(recovered.selectedDetail)
        assertTrue(recovered.selectPlan("saved-timed").selectedDetail?.canStartTraining == true)
    }

    @Test
    fun upsertPlanSelectsSavedPlanAndKeepsLocalPlanSummary() {
        val empty = PlanManagementScreenState(plans = emptyList())
        val savedTimedPlan = buildDefaultTimedPlanEditorState()
            .updateTitle("保存后的计时计划")
            .toWorkoutPlan(
                planId = "saved-timed",
                timestamp = "2026-06-13T08:00:00Z"
            )
        val updated = empty.upsertPlan(savedTimedPlan)

        assertFalse(updated.isEmpty)
        assertEquals("saved-timed", updated.selectedPlanId)
        assertEquals("保存后的计时计划", updated.listItems.single().title)
        assertTrue(requireNotNull(updated.statusMessage).contains("本地计划"))
    }

    @Test
    fun selectingAPlanMapsDetailSectionsWithoutStartingTraining() {
        val strengthId = buildDefaultPlanManagementState().plans[1].id
        val state = buildDefaultPlanManagementState().selectPlan(strengthId)
        val detail = requireNotNull(state.selectedDetail)

        assertEquals(strengthId, detail.id)
        assertEquals("力量训练", detail.modeLabel)
        assertTrue(detail.canStartTraining)
        assertTrue(detail.canEditPlan)
        assertEquals("编辑力量计划", detail.editActionLabel)
        assertEquals("开始力量训练", detail.startStatus)
        assertTrue(detail.editStatus.contains("本地计划"))
        assertTrue(detail.sections.any { section -> section.title == "动作与组" })
        assertTrue(detail.reminder.boundaryCopy.contains("普通通知"))
        assertFalse(detail.sections.flatMap { it.rows }.joinToString().contains("manual_start"))
    }

    @Test
    fun selectingCurrentPlanAgainCollapsesPlaylistDetail() {
        val state = buildDefaultPlanManagementState()
        val selectedId = state.plans.first().id
        val expanded = state.selectPlan(selectedId)
        val collapsed = expanded.selectPlan(selectedId)

        assertNull(collapsed.selectedDetail)
        assertFalse(collapsed.listItems.first { it.id == selectedId }.selected)
        assertEquals(selectedId, expanded.selectedDetail?.id)
        assertTrue(expanded.listItems.first { it.id == selectedId }.selected)
    }

    @Test
    fun detailCopyUsesUserFacingPlanStructureTerms() {
        val defaultState = buildDefaultPlanManagementState()
        val timedDetailRows = requireNotNull(
            defaultState.selectPlan(defaultState.plans.first().id).selectedDetail
        )
            .sections
            .flatMap { it.rows }
            .joinToString(" ")
        val strengthDetailRows = buildDefaultPlanManagementState()
            .selectPlan(buildDefaultPlanManagementState().plans[1].id)
            .selectedDetail
            .let(::requireNotNull)
            .sections
            .flatMap { it.rows }
            .joinToString(" ")

        assertTrue(timedDetailRows.contains("训练阶段"))
        assertFalse(timedDetailRows.contains("interval stage"))
        assertFalse(strengthDetailRows.contains("strength block"))
        assertFalse(strengthDetailRows.contains("planned set"))
        assertFalse(strengthDetailRows.contains("manual_start"))
    }

    @Test
    fun timedPlanDetailEnablesStartTrainingForE3SessionScreen() {
        val defaultState = buildDefaultPlanManagementState()
        val state = defaultState.selectPlan(defaultState.plans.first().id)
        val detail = requireNotNull(state.selectedDetail)

        assertEquals("计时训练", detail.modeLabel)
        assertTrue(detail.canStartTraining)
        assertTrue(detail.canEditPlan)
        assertEquals("编辑计时计划", detail.editActionLabel)
        assertEquals("开始计时训练", detail.startStatus)
    }

    @Test
    fun followAlongPlanDoesNotExposeFakeEditEntry() {
        val followAlong = WorkoutPlan(
            id = "follow-along-saved",
            mode = WorkoutMode.FOLLOW_ALONG,
            title = "基础跟练",
            blocks = emptyList(),
            followAlong = com.liujyks.trainflow.core.model.FollowAlongPlanMeta(preset = true),
            createdAt = "2026-06-14T01:00:00Z",
            updatedAt = "2026-06-14T01:00:00Z"
        )
        val detail = requireNotNull(
            PlanManagementScreenState(plans = listOf(followAlong))
                .selectPlan(followAlong.id)
                .selectedDetail
        )

        assertFalse(detail.canEditPlan)
        assertFalse(detail.canStartTraining)
        assertEquals("待完整编排", detail.editActionLabel)
        assertTrue(detail.editStatus.contains("不提供假编辑入口"))
    }

    @Test
    fun copyingAPlanCreatesNewPlanWithNewIdAndKeepsContractShape() {
        val state = buildDefaultPlanManagementState()
        val original = state.plans.first()
        val copiedState = state.copyPlan(original.id)
        val copied = requireNotNull(copiedState.selectedPlan)
        val originalCircuit = original.blocks.filterIsInstance<TimedCircuitBlock>().single()
        val copiedCircuit = copied.blocks.filterIsInstance<TimedCircuitBlock>().single()

        assertEquals(3, copiedState.plans.size)
        assertNotEquals(original.id, copied.id)
        assertEquals("纯间歇计时器 副本", copied.title)
        assertEquals(original.mode, copied.mode)
        assertEquals(originalCircuit.items.size, copiedCircuit.items.size)
        assertNotEquals(originalCircuit.id, copiedCircuit.id)
        assertNotEquals(originalCircuit.items.first().id, copiedCircuit.items.first().id)
        assertTrue(requireNotNull(copiedState.statusMessage).contains("已复制"))
        assertTrue(requireNotNull(copiedState.statusMessage).contains("本地计划"))
    }

    @Test
    fun deleteRequiresConfirmationAndCanBeCancelled() {
        val state = buildDefaultPlanManagementState()
        val planId = state.plans.first().id
        val pending = state.requestDeletePlan(planId)
        val cancelled = pending.cancelDeletePlan()

        assertEquals(planId, pending.pendingDeletePlanId)
        assertNotNull(pending.pendingDeletePlanTitle)
        assertEquals(2, pending.plans.size)
        assertNull(cancelled.pendingDeletePlanId)
        assertEquals(2, cancelled.plans.size)
    }

    @Test
    fun confirmedDeleteRemovesPlanAndMovesSelection() {
        val state = buildDefaultPlanManagementState()
        val firstId = state.plans.first().id
        val secondId = state.plans[1].id
        val deleted = state
            .selectPlan(firstId)
            .requestDeletePlan(firstId)
            .confirmDeletePlan()

        assertEquals(1, deleted.plans.size)
        assertFalse(deleted.plans.any { it.id == firstId })
        assertEquals(secondId, deleted.selectedPlanId)
        assertTrue(requireNotNull(deleted.statusMessage).contains("已删除"))
    }

    @Test
    fun deletingLastPlanShowsEmptyState() {
        val firstPlanOnly = PlanManagementScreenState(
            plans = listOf(buildDefaultPlanManagementState().plans.first())
        )
        val deleted = firstPlanOnly
            .requestDeletePlan(firstPlanOnly.plans.first().id)
            .confirmDeletePlan()

        assertTrue(deleted.isEmpty)
        assertTrue(deleted.listItems.isEmpty())
        assertNull(deleted.selectedDetail)
        assertNull(deleted.selectedPlanId)
    }

    @Test
    fun strengthCopyKeepsStrengthBlocksAndSetCount() {
        val state = buildDefaultPlanManagementState()
        val strength = state.plans[1]
        val copied = requireNotNull(state.copyPlan(strength.id).selectedPlan)
        val originalBlocks = strength.blocks.filterIsInstance<StrengthExerciseBlock>()
        val copiedBlocks = copied.blocks.filterIsInstance<StrengthExerciseBlock>()

        assertEquals(originalBlocks.size, copiedBlocks.size)
        assertEquals(originalBlocks.sumOf { it.sets.size }, copiedBlocks.sumOf { it.sets.size })
        assertNotEquals(originalBlocks.first().sets.first().id, copiedBlocks.first().sets.first().id)
        assertTrue(copied.title.contains("副本"))
    }

    @Test
    fun settingPlanReminderStoresPlanReminderAndKeepsOrdinaryNotificationCopy() {
        val state = buildDefaultPlanManagementState()
        val planId = state.plans.first().id
        val scheduleAt = "2026-06-04T11:30:00Z"
        val updated = state.setPlanReminder(
            planId = planId,
            scheduleAt = scheduleAt,
            nowEpochMillis = Instant.parse("2026-06-03T11:30:00Z").toEpochMilli()
        )
        val plan = requireNotNull(updated.selectedPlan)
        val detail = requireNotNull(updated.selectedDetail)

        assertTrue(requireNotNull(plan.reminder).enabled)
        assertEquals(scheduleAt, requireNotNull(plan.reminder).scheduleAt)
        assertTrue(requireNotNull(updated.statusMessage).contains("普通通知"))
        assertTrue(requireNotNull(updated.statusMessage).contains("不是闹钟级强提醒"))
        assertTrue(detail.reminder.enabled)
        assertTrue(detail.reminder.boundaryCopy.contains("关闭后训练仍可正常使用"))
        assertTrue(detail.reminder.boundaryCopy.contains("系统延迟"))
        assertFalse(detail.reminder.boundaryCopy.contains("锁屏强打断"))
    }

    @Test
    fun notificationPermissionDeniedShowsClearAndroid13CopyWithoutBlockingPlanState() {
        val denied = PlanReminderNotificationPermissionState.resolve(
            sdkInt = 33,
            postNotificationsGranted = false
        )
        val state = buildDefaultPlanManagementState()
            .updateNotificationPermissionState(denied)
        val planId = state.plans.first().id
        val updated = state.setPlanReminder(
            planId = planId,
            scheduleAt = "2026-06-04T11:30:00Z",
            nowEpochMillis = Instant.parse("2026-06-03T11:30:00Z").toEpochMilli()
        )
        val detail = requireNotNull(updated.selectedDetail)

        assertEquals(PlanReminderNotificationPermissionStatus.DENIED, updated.notificationPermissionState.status)
        assertTrue(requireNotNull(updated.selectedPlan?.reminder).enabled)
        assertTrue(requireNotNull(updated.statusMessage).contains("权限关闭"))
        assertTrue(requireNotNull(updated.statusMessage).contains("训练仍可正常使用"))
        assertTrue(detail.reminder.permissionMessage.contains("Android 13+"))
        assertTrue(detail.reminder.canRequestPermission)
        assertTrue(detail.canStartTraining)
    }

    @Test
    fun pastPlanReminderTimeIsRejectedWithoutChangingPlan() {
        val state = buildDefaultPlanManagementState()
        val planId = state.plans.first().id
        val updated = state.setPlanReminder(
            planId = planId,
            scheduleAt = "2026-06-03T10:30:00Z",
            nowEpochMillis = Instant.parse("2026-06-03T11:30:00Z").toEpochMilli()
        )

        assertNull(updated.selectedPlan?.reminder)
        assertTrue(requireNotNull(updated.statusMessage).contains("已过"))
    }

    @Test
    fun clearPlanReminderDisablesReminderAndSchedulerRequest() {
        val state = buildDefaultPlanManagementState()
        val planId = state.plans.first().id
        val withReminder = state.setPlanReminder(
            planId = planId,
            scheduleAt = "2026-06-04T11:30:00Z",
            nowEpochMillis = Instant.parse("2026-06-03T11:30:00Z").toEpochMilli()
        )
        val cleared = withReminder.clearPlanReminder(planId)
        val request = requireNotNull(cleared.selectedPlan)
            .toPlanReminderScheduleRequest(cleared.notificationPermissionState)

        assertFalse(requireNotNull(cleared.selectedPlan?.reminder).enabled)
        assertFalse(request.enabled)
        assertTrue(requireNotNull(cleared.statusMessage).contains("已关闭"))
    }

    @Test
    fun copiedPlanDoesNotKeepEnabledReminderSchedule() {
        val state = buildDefaultPlanManagementState()
        val original = state.plans.first()
        val withReminder = state.setPlanReminder(
            planId = original.id,
            scheduleAt = "2026-06-04T11:30:00Z",
            nowEpochMillis = Instant.parse("2026-06-03T11:30:00Z").toEpochMilli()
        )
        val copied = requireNotNull(withReminder.copyPlan(original.id).selectedPlan)

        assertFalse(requireNotNull(copied.reminder).enabled)
        assertNull(requireNotNull(copied.reminder).scheduleAt)
    }

    @Test
    fun editingCurrentPlanDoesNotRewriteHistoricalSessionPlanSnapshot() {
        val state = buildDefaultPlanManagementState()
        val original = state.plans.first()
        val historicalSession = WorkoutSession(
            id = "session-before-edit",
            planId = original.id,
            mode = original.mode,
            planSnapshot = WorkoutPlanSnapshot(
                planId = original.id,
                title = original.title,
                mode = original.mode,
                blocks = original.blocks,
                preferences = original.preferences,
                followAlong = original.followAlong
            ),
            status = SessionStatus.COMPLETED
        )
        val editedPlan = original
            .toTimedPlanEditorState()
            .updateTitle("编辑后的当前计划")
            .toWorkoutPlan(timestamp = "2026-06-15T01:00:00Z")
        val updatedState = state.upsertPlan(editedPlan)

        assertEquals("编辑后的当前计划", updatedState.selectedPlan?.title)
        assertEquals(original.title, historicalSession.planSnapshot.title)
        assertEquals(original.blocks, historicalSession.planSnapshot.blocks)
        assertEquals(original.preferences, historicalSession.planSnapshot.preferences)
    }

    @Test
    fun planReminderPresetOptionsAreFutureInstants() {
        val now = Instant.parse("2026-06-03T11:30:00Z")
        val options = buildPlanReminderPresetOptions(
            now = now,
            zoneId = ZoneId.of("Asia/Shanghai")
        )

        assertEquals(2, options.size)
        assertTrue(options.all { Instant.parse(it.scheduleAt) > now })
        assertTrue(options.any { it.label == "20:00" })
        assertTrue(options.any { it.label == "明早 07:30" })
    }
}
