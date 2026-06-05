package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.PlanReminder
import com.liujyks.trainflow.core.model.RestBlock
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.notifications.PlanReminderNotificationPermissionState
import com.liujyks.trainflow.core.notifications.PlanReminderNotificationPermissionStatus
import com.liujyks.trainflow.core.notifications.PlanReminderScheduleRequest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal const val DefaultPlanManagementTimestamp = "2026-05-29T00:00:00Z"

internal data class PlanManagementScreenState(
    val plans: List<WorkoutPlan>,
    val selectedPlanId: String? = plans.firstOrNull()?.id,
    val pendingDeletePlanId: String? = null,
    val statusMessage: String? = null,
    val notificationPermissionState: PlanReminderNotificationPermissionState =
        PlanReminderNotificationPermissionState.resolve(
            sdkInt = 33,
            postNotificationsGranted = true
        )
) {
    val isEmpty: Boolean = plans.isEmpty()

    val listItems: List<PlanListItemUiState>
        get() = plans.map { plan ->
            plan.toListItem(selected = plan.id == selectedPlanId)
        }

    val selectedPlan: WorkoutPlan?
        get() = plans.firstOrNull { it.id == selectedPlanId } ?: plans.firstOrNull()

    val selectedDetail: PlanDetailUiState?
        get() = selectedPlan?.toDetailState(notificationPermissionState)

    val pendingDeletePlanTitle: String?
        get() = plans.firstOrNull { it.id == pendingDeletePlanId }?.title
}

internal data class PlanListItemUiState(
    val id: String,
    val title: String,
    val modeLabel: String,
    val modeBadge: String,
    val summary: String,
    val detailSummary: String,
    val reminderSummary: String,
    val metrics: List<PlanMetricUiState>,
    val selected: Boolean
)

internal data class PlanDetailUiState(
    val id: String,
    val title: String,
    val modeLabel: String,
    val modeBadge: String,
    val summary: String,
    val detailSummary: String,
    val metrics: List<PlanMetricUiState>,
    val sections: List<PlanDetailSectionUiState>,
    val reminder: PlanReminderUiState,
    val editStatus: String,
    val startStatus: String,
    val canStartTraining: Boolean = false
)

internal data class PlanMetricUiState(
    val label: String,
    val value: String
)

internal data class PlanReminderUiState(
    val summary: String,
    val permissionMessage: String,
    val boundaryCopy: String,
    val enabled: Boolean,
    val canRequestPermission: Boolean
)

internal data class PlanReminderPresetUiState(
    val label: String,
    val scheduleAt: String
)

internal data class PlanDetailSectionUiState(
    val title: String,
    val rows: List<String>
)

internal fun buildDefaultPlanManagementState(
    timestamp: String = DefaultPlanManagementTimestamp
): PlanManagementScreenState {
    return PlanManagementScreenState(
        plans = listOf(
            buildDefaultTimedPlanEditorState().toWorkoutPlan(
                planId = "plan-timed-default",
                timestamp = timestamp
            ).copy(
                description = "内存态计时计划，可用于列表、详情、复制、删除和启动执行验证。"
            ),
            buildDefaultStrengthPlanEditorState().toWorkoutPlan(
                planId = "plan-strength-default",
                timestamp = timestamp
            ).copy(
                description = "内存态力量计划，可用于列表、详情、复制、删除和启动执行验证。"
            )
        )
    )
}

internal fun PlanManagementScreenState.selectPlan(planId: String): PlanManagementScreenState {
    if (plans.none { it.id == planId }) return this

    return copy(
        selectedPlanId = planId,
        pendingDeletePlanId = null,
        statusMessage = null
    )
}

internal fun PlanManagementScreenState.updateNotificationPermissionState(
    permissionState: PlanReminderNotificationPermissionState
): PlanManagementScreenState {
    return copy(notificationPermissionState = permissionState)
}

internal fun PlanManagementScreenState.setPlanReminder(
    planId: String,
    scheduleAt: String,
    nowEpochMillis: Long = System.currentTimeMillis(),
    timestamp: String = DefaultPlanManagementTimestamp
): PlanManagementScreenState {
    val plan = plans.firstOrNull { it.id == planId } ?: return this
    val scheduleAtEpochMillis = scheduleAt.toEpochMillisOrNull()
        ?: return copy(statusMessage = "提醒时间格式暂无法识别，请重新选择。")
    if (scheduleAtEpochMillis <= nowEpochMillis) {
        return copy(statusMessage = "提醒时间已过，请选择未来时间。")
    }

    val updatedPlan = plan.copy(
        reminder = PlanReminder(enabled = true, scheduleAt = scheduleAt),
        updatedAt = timestamp
    )
    val request = updatedPlan.toPlanReminderScheduleRequest(notificationPermissionState)
    val message = if (notificationPermissionState.canPostNotifications) {
        "已为「${plan.title}」设置 ${formatReminderSchedule(scheduleAt)} 训练提醒；普通通知可能被系统延迟。"
    } else {
        "已保存「${plan.title}」的提醒时间，但 Android 13+ 通知权限关闭，暂不会弹出通知。"
    }

    return copy(
        plans = plans.replacePlan(updatedPlan),
        selectedPlanId = planId,
        pendingDeletePlanId = null,
        statusMessage = messageForScheduleRequest(request, message),
        notificationPermissionState = notificationPermissionState
    )
}

internal fun PlanManagementScreenState.clearPlanReminder(
    planId: String,
    timestamp: String = DefaultPlanManagementTimestamp
): PlanManagementScreenState {
    val plan = plans.firstOrNull { it.id == planId } ?: return this
    val updatedPlan = plan.copy(
        reminder = PlanReminder(enabled = false, scheduleAt = null),
        updatedAt = timestamp
    )

    return copy(
        plans = plans.replacePlan(updatedPlan),
        selectedPlanId = planId,
        pendingDeletePlanId = null,
        statusMessage = "已关闭「${plan.title}」的训练提醒。"
    )
}

internal fun PlanManagementScreenState.copyPlan(
    planId: String,
    timestamp: String = DefaultPlanManagementTimestamp
): PlanManagementScreenState {
    val original = plans.firstOrNull { it.id == planId } ?: return this
    val copiedPlan = original.copyAsNewPlan(
        id = nextCopyId(original.id),
        title = nextCopyTitle(original.title),
        timestamp = timestamp
    )

    return copy(
        plans = plans + copiedPlan,
        selectedPlanId = copiedPlan.id,
        pendingDeletePlanId = null,
        statusMessage = "已复制「${original.title}」，新计划暂存在本次内存态列表。"
    )
}

internal fun PlanManagementScreenState.requestDeletePlan(planId: String): PlanManagementScreenState {
    if (plans.none { it.id == planId }) return this

    return copy(
        pendingDeletePlanId = planId,
        statusMessage = null
    )
}

internal fun PlanManagementScreenState.cancelDeletePlan(): PlanManagementScreenState {
    return copy(pendingDeletePlanId = null)
}

internal fun PlanManagementScreenState.confirmDeletePlan(): PlanManagementScreenState {
    val deleteId = pendingDeletePlanId ?: return this
    val deletedPlan = plans.firstOrNull { it.id == deleteId } ?: return copy(pendingDeletePlanId = null)
    val remainingPlans = plans.filterNot { it.id == deleteId }
    val nextSelectedPlanId = when {
        selectedPlanId != deleteId && remainingPlans.any { it.id == selectedPlanId } -> selectedPlanId
        else -> remainingPlans.firstOrNull()?.id
    }

    return copy(
        plans = remainingPlans,
        selectedPlanId = nextSelectedPlanId,
        pendingDeletePlanId = null,
        statusMessage = "已删除「${deletedPlan.title}」。"
    )
}

private fun WorkoutPlan.toListItem(selected: Boolean): PlanListItemUiState {
    return PlanListItemUiState(
        id = id,
        title = title,
        modeLabel = mode.modeLabel(),
        modeBadge = mode.modeBadge(),
        summary = planSummary(),
        detailSummary = planDetailSummary(),
        reminderSummary = planReminderSummary(),
        metrics = planMetrics(),
        selected = selected
    )
}

private fun WorkoutPlan.toDetailState(
    notificationPermissionState: PlanReminderNotificationPermissionState
): PlanDetailUiState {
    return PlanDetailUiState(
        id = id,
        title = title,
        modeLabel = mode.modeLabel(),
        modeBadge = mode.modeBadge(),
        summary = planSummary(),
        detailSummary = planDetailSummary(),
        metrics = planMetrics(),
        sections = detailSections(),
        reminder = toReminderUiState(notificationPermissionState),
        editStatus = "编辑回填后续接入",
        startStatus = when (mode) {
            WorkoutMode.TIMED -> "开始计时训练"
            WorkoutMode.STRENGTH -> "开始力量训练"
            WorkoutMode.FOLLOW_ALONG -> "跟练闭环留给 E6"
        },
        canStartTraining = mode == WorkoutMode.TIMED || mode == WorkoutMode.STRENGTH
    )
}

private fun WorkoutPlan.planMetrics(): List<PlanMetricUiState> {
    val reminderValue = if (reminder?.enabled == true && reminder.scheduleAt != null) {
        "已设置"
    } else {
        "未设置"
    }
    return when (mode) {
        WorkoutMode.TIMED -> {
            val circuits = blocks.filterIsInstance<TimedCircuitBlock>()
            val restValues = circuits.flatMap { block ->
                block.items.mapNotNull { it.restAfterSec } + listOfNotNull(block.restBetweenRoundsSec)
            } + blocks.filterIsInstance<RestBlock>().map { it.durationSec }
            listOf(
                PlanMetricUiState("动作", "${circuits.sumOf { it.items.size }} 个"),
                PlanMetricUiState("轮次", "${circuits.sumOf { it.rounds }} 轮"),
                PlanMetricUiState("时长", estimatedTimedDurationSec().formatDuration()),
                PlanMetricUiState("休息", restValues.distinct().toMetricDuration()),
                PlanMetricUiState("提醒", reminderValue)
            )
        }

        WorkoutMode.STRENGTH -> {
            val strengthBlocks = blocks.filterIsInstance<StrengthExerciseBlock>()
            val restValues = strengthBlocks.flatMap { block ->
                listOfNotNull(block.target?.restAfterSetSec) + block.sets.mapNotNull { it.restAfterSec }
            }.distinct()
            listOf(
                PlanMetricUiState("动作", "${strengthBlocks.size} 个"),
                PlanMetricUiState("组数", "${strengthBlocks.sumOf { it.sets.size }} 组"),
                PlanMetricUiState("休息", restValues.toMetricDuration()),
                PlanMetricUiState("提醒", reminderValue)
            )
        }

        WorkoutMode.FOLLOW_ALONG -> listOf(
            PlanMetricUiState("模式", "跟练雏形"),
            PlanMetricUiState("提醒", reminderValue)
        )
    }
}

private fun List<Int>.toMetricDuration(): String {
    return when (size) {
        0 -> "未设置"
        1 -> first().formatDuration()
        else -> "按步骤"
    }
}

private fun WorkoutMode.modeLabel(): String {
    return when (this) {
        WorkoutMode.TIMED -> "计时训练"
        WorkoutMode.STRENGTH -> "力量训练"
        WorkoutMode.FOLLOW_ALONG -> "跟练"
    }
}

private fun WorkoutMode.modeBadge(): String {
    return when (this) {
        WorkoutMode.TIMED -> "计时"
        WorkoutMode.STRENGTH -> "力量"
        WorkoutMode.FOLLOW_ALONG -> "跟练"
    }
}

private fun WorkoutPlan.planSummary(): String {
    return when (mode) {
        WorkoutMode.TIMED -> {
            val circuitCount = blocks.filterIsInstance<TimedCircuitBlock>().sumOf { it.items.size }
            val rounds = blocks.filterIsInstance<TimedCircuitBlock>().sumOf { it.rounds }
            "$circuitCount 个动作 · $rounds 轮 · 预计 ${estimatedTimedDurationSec().formatDuration()}"
        }

        WorkoutMode.STRENGTH -> {
            val strengthBlocks = blocks.filterIsInstance<StrengthExerciseBlock>()
            "${strengthBlocks.size} 个动作 · ${strengthBlocks.sumOf { it.sets.size }} 组"
        }

        WorkoutMode.FOLLOW_ALONG -> "跟练雏形计划 · 后续接入"
    }
}

private fun WorkoutPlan.planDetailSummary(): String {
    return when (mode) {
        WorkoutMode.TIMED -> {
            val cue = preferences?.cueSettings
            val actionCue = cue?.actionEnding?.thresholdSec?.let { "动作提醒 ${it}秒" } ?: "动作提醒未设"
            val restCue = cue?.restEnding?.thresholdSec?.let { "休息提醒 ${it}秒" } ?: "休息提醒关闭"
            "$actionCue · $restCue"
        }

        WorkoutMode.STRENGTH -> {
            val restValues = blocks.filterIsInstance<StrengthExerciseBlock>()
                .mapNotNull { it.target?.restAfterSetSec }
                .distinct()
            val restSummary = if (restValues.size == 1) "${restValues.single()}秒休息" else "按动作休息"
            "$restSummary · 计划值预填实际记录"
        }

        WorkoutMode.FOLLOW_ALONG -> "复用计时流程和动作内容"
    }
}

private fun WorkoutPlan.planReminderSummary(): String {
    val reminder = reminder
    return if (reminder?.enabled == true && reminder.scheduleAt != null) {
        "训练提醒 · ${formatReminderSchedule(reminder.scheduleAt)}"
    } else {
        "训练提醒未设置"
    }
}

private fun WorkoutPlan.toReminderUiState(
    notificationPermissionState: PlanReminderNotificationPermissionState
): PlanReminderUiState {
    val reminderEnabled = reminder?.enabled == true && reminder.scheduleAt != null
    return PlanReminderUiState(
        summary = planReminderSummary(),
        permissionMessage = notificationPermissionState.rationale,
        boundaryCopy = "首版只使用普通通知，允许系统延迟；不使用闹钟级强提醒、全屏提示或锁屏强打断。",
        enabled = reminderEnabled,
        canRequestPermission =
            notificationPermissionState.status == PlanReminderNotificationPermissionStatus.DENIED
    )
}

private fun WorkoutPlan.detailSections(): List<PlanDetailSectionUiState> {
    return when (mode) {
        WorkoutMode.TIMED -> timedDetailSections()
        WorkoutMode.STRENGTH -> strengthDetailSections()
        WorkoutMode.FOLLOW_ALONG -> listOf(
            PlanDetailSectionUiState(
                title = "边界",
                rows = listOf("跟练计划元数据已保留，完整跟练闭环后续接入。")
            )
        )
    }
}

private fun WorkoutPlan.timedDetailSections(): List<PlanDetailSectionUiState> {
    val blockRows = blocks.map { block ->
        when (block) {
            is WarmupBlock -> "热身 · ${block.durationSec?.formatDuration() ?: "按动作"}"
            is TimedCircuitBlock -> {
                val exerciseNames = block.items.joinToString("、") { item -> item.exerciseLabel() }
                "正式训练 · ${block.rounds} 轮 · $exerciseNames"
            }

            is StretchBlock -> "拉伸 · ${block.durationSec?.formatDuration() ?: "按动作"}"
            is RestBlock -> "休息 · ${block.durationSec.formatDuration()}"
            is CooldownBlock -> "冷却 · ${block.durationSec?.formatDuration() ?: "按动作"}"
            is StrengthExerciseBlock -> "力量动作块 · ${block.exerciseLabel()}"
        }
    }

    return listOf(
        PlanDetailSectionUiState(
            title = "摘要",
            rows = listOf(planSummary(), planDetailSummary())
        ),
        PlanDetailSectionUiState(
            title = "结构",
            rows = blockRows
        )
    )
}

private fun WorkoutPlan.strengthDetailSections(): List<PlanDetailSectionUiState> {
    val strengthBlocks = blocks.filterIsInstance<StrengthExerciseBlock>()
    val actionRows = strengthBlocks.map { block ->
        val setSummary = block.sets.groupBy { it.kind }.entries.joinToString("，") { (kind, sets) ->
            "${kind.contractValue} ${sets.size}组"
        }
        "${block.exerciseLabel()} · $setSummary · ${block.setTimerMode.contractValue}"
    }

    return listOf(
        PlanDetailSectionUiState(
            title = "摘要",
            rows = listOf(planSummary(), planDetailSummary())
        ),
        PlanDetailSectionUiState(
            title = "动作与组",
            rows = actionRows
        )
    )
}

private fun WorkoutPlan.estimatedTimedDurationSec(): Int {
    return blocks.sumOf { block ->
        when (block) {
            is WarmupBlock -> block.durationSec ?: block.items.sumTimedItemsOnce()
            is TimedCircuitBlock -> {
                val roundDuration = block.items.sumTimedItemsOnce()
                val roundRest = block.restBetweenRoundsSec.orZero() * (block.rounds - 1).coerceAtLeast(0)
                roundDuration * block.rounds + roundRest
            }

            is StretchBlock -> block.durationSec ?: block.items.sumTimedItemsOnce()
            is CooldownBlock -> block.durationSec ?: block.items.sumTimedItemsOnce()
            is RestBlock -> block.durationSec
            is StrengthExerciseBlock -> 0
        }
    }
}

private fun List<TimedExerciseItem>.sumTimedItemsOnce(): Int {
    return sumOf { item -> item.workDurationSec + item.restAfterSec.orZero() }
}

private fun TimedExerciseItem.exerciseLabel(): String {
    return exerciseName(exerciseId)
}

private fun StrengthExerciseBlock.exerciseLabel(): String {
    return title ?: exerciseName(exerciseId)
}

private fun exerciseName(exerciseId: String): String {
    return FirstActionExerciseFixtures.entries
        .firstOrNull { it.exercise.id == exerciseId }
        ?.exercise
        ?.name
        ?: exerciseId
}

private fun WorkoutPlan.copyAsNewPlan(
    id: String,
    title: String,
    timestamp: String
): WorkoutPlan {
    return copy(
        id = id,
        title = title,
        blocks = blocks.duplicateForPlanCopy(id),
        reminder = reminder?.copy(enabled = false, scheduleAt = null),
        createdAt = timestamp,
        updatedAt = timestamp
    )
}

private fun List<PlanBlock>.duplicateForPlanCopy(planId: String): List<PlanBlock> {
    return mapIndexed { blockIndex, block ->
        val blockId = "$planId-block-${blockIndex + 1}"
        when (block) {
            is WarmupBlock -> block.copy(
                id = blockId,
                items = block.items.duplicateTimedItems(blockId)
            )

            is TimedCircuitBlock -> block.copy(
                id = blockId,
                items = block.items.duplicateTimedItems(blockId)
            )

            is StretchBlock -> block.copy(
                id = blockId,
                items = block.items.duplicateTimedItems(blockId)
            )

            is CooldownBlock -> block.copy(
                id = blockId,
                items = block.items.duplicateTimedItems(blockId)
            )

            is RestBlock -> block.copy(id = blockId)
            is StrengthExerciseBlock -> block.copy(
                id = blockId,
                sets = block.sets.duplicateStrengthSets(blockId)
            )
        }
    }
}

private fun List<TimedExerciseItem>.duplicateTimedItems(blockId: String): List<TimedExerciseItem> {
    return mapIndexed { index, item ->
        item.copy(id = "$blockId-item-${index + 1}")
    }
}

private fun List<StrengthSetPlan>.duplicateStrengthSets(blockId: String): List<StrengthSetPlan> {
    return mapIndexed { index, set ->
        set.copy(id = "$blockId-set-${index + 1}")
    }
}

private fun PlanManagementScreenState.nextCopyId(sourcePlanId: String): String {
    var index = plans.count { it.id.startsWith("$sourcePlanId-copy") } + 1
    var candidate = "$sourcePlanId-copy-$index"
    val existingIds = plans.map { it.id }.toSet()
    while (candidate in existingIds) {
        index += 1
        candidate = "$sourcePlanId-copy-$index"
    }
    return candidate
}

private fun PlanManagementScreenState.nextCopyTitle(sourceTitle: String): String {
    val base = "$sourceTitle 副本"
    if (plans.none { it.title == base }) return base

    var index = 2
    var candidate = "$base $index"
    val existingTitles = plans.map { it.title }.toSet()
    while (candidate in existingTitles) {
        index += 1
        candidate = "$base $index"
    }
    return candidate
}

private fun Int?.orZero(): Int = this ?: 0

internal fun WorkoutPlan.toPlanReminderScheduleRequest(
    permissionState: PlanReminderNotificationPermissionState
): PlanReminderScheduleRequest {
    return PlanReminderScheduleRequest(
        planId = id,
        planTitle = title,
        scheduleAtEpochMillis = reminder?.scheduleAt?.toEpochMillisOrNull(),
        enabled = reminder?.enabled == true,
        permissionState = permissionState
    )
}

internal fun buildPlanReminderPresetOptions(
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<PlanReminderPresetUiState> {
    val localNow = now.atZone(zoneId)
    val evening = nextLocalTime(
        date = localNow.toLocalDate(),
        time = LocalTime.of(20, 0),
        now = localNow.toInstant(),
        zoneId = zoneId
    )
    val morning = LocalDate.from(localNow).plusDays(1)
        .atTime(7, 30)
        .atZone(zoneId)
        .toInstant()

    return listOf(
        PlanReminderPresetUiState(
            label = "20:00",
            scheduleAt = evening.toString()
        ),
        PlanReminderPresetUiState(
            label = "明早 07:30",
            scheduleAt = morning.toString()
        )
    )
}

internal fun formatReminderSchedule(scheduleAt: String): String {
    return scheduleAt.toEpochMillisOrNull()
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(ReminderFormatter) }
        ?: "未识别时间"
}

private fun nextLocalTime(
    date: LocalDate,
    time: LocalTime,
    now: Instant,
    zoneId: ZoneId
): Instant {
    val candidate = date.atTime(time).atZone(zoneId).toInstant()
    return if (candidate > now) candidate else date.plusDays(1).atTime(time).atZone(zoneId).toInstant()
}

private fun String.toEpochMillisOrNull(): Long? {
    return runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
}

private fun List<WorkoutPlan>.replacePlan(updatedPlan: WorkoutPlan): List<WorkoutPlan> {
    return map { plan -> if (plan.id == updatedPlan.id) updatedPlan else plan }
}

private fun messageForScheduleRequest(
    request: PlanReminderScheduleRequest,
    message: String
): String {
    return if (request.enabled && request.scheduleAtEpochMillis != null) message else "请先选择未来的提醒时间。"
}

private val ReminderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
