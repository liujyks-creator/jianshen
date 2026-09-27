package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.RestBlock
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan

internal const val DefaultPlanManagementTimestamp = "2026-05-29T00:00:00Z"

internal data class PlanManagementScreenState(
    val plans: List<WorkoutPlan>,
    val selectedPlanId: String? = null,
    val pendingDeletePlanId: String? = null,
    val statusMessage: String? = null
) {
    val isEmpty: Boolean = plans.isEmpty()

    val listItems: List<PlanListItemUiState>
        get() = plans.map { plan ->
            plan.toListItem(selected = plan.id == selectedPlanId)
        }

    val selectedPlan: WorkoutPlan?
        get() = plans.firstOrNull { it.id == selectedPlanId }

    val selectedDetail: PlanDetailUiState?
        get() = plans.firstOrNull { it.id == selectedPlanId }?.toDetailState()

    val pendingDeletePlanTitle: String?
        get() = plans.firstOrNull { it.id == pendingDeletePlanId }?.title
}

internal data class PlanListItemUiState(
    val id: String,
    val title: String,
    val mode: WorkoutMode,
    val modeLabel: String,
    val modeBadge: String,
    val planColorHex: String,
    val summary: String,
    val detailSummary: String,
    val metrics: List<PlanMetricUiState>,
    val selected: Boolean
)

internal data class PlanDetailUiState(
    val id: String,
    val title: String,
    val mode: WorkoutMode,
    val modeLabel: String,
    val modeBadge: String,
    val planColorHex: String,
    val summary: String,
    val detailSummary: String,
    val metrics: List<PlanMetricUiState>,
    val sections: List<PlanDetailSectionUiState>,
    val editStatus: String,
    val startStatus: String,
    val editActionLabel: String,
    val canEditPlan: Boolean,
    val canStartTraining: Boolean = false
)

internal data class PlanMetricUiState(
    val label: String,
    val value: String
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

internal fun PlanManagementScreenState.withPlans(plans: List<WorkoutPlan>): PlanManagementScreenState {
    val nextSelectedPlanId = when {
        plans.any { plan -> plan.id == selectedPlanId } -> selectedPlanId
        selectedPlanId == null -> null
        else -> plans.firstOrNull()?.id
    }
    val nextPendingDeletePlanId = pendingDeletePlanId?.takeIf { pendingId ->
        plans.any { plan -> plan.id == pendingId }
    }
    return copy(
        plans = plans,
        selectedPlanId = nextSelectedPlanId,
        pendingDeletePlanId = nextPendingDeletePlanId
    )
}

internal fun PlanManagementScreenState.upsertPlan(plan: WorkoutPlan): PlanManagementScreenState {
    val updatedPlans = if (plans.any { existing -> existing.id == plan.id }) {
        plans.replacePlan(plan)
    } else {
        listOf(plan) + plans
    }
    return copy(
        plans = updatedPlans,
        selectedPlanId = plan.id,
        pendingDeletePlanId = null,
        statusMessage = "已保存「${plan.title}」到本地计划。"
    )
}

internal fun PlanManagementScreenState.selectPlan(planId: String): PlanManagementScreenState {
    if (plans.none { it.id == planId }) return this

    return copy(
        selectedPlanId = if (selectedPlanId == planId) null else planId,
        pendingDeletePlanId = null,
        statusMessage = null
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
        statusMessage = "已复制「${original.title}」，新计划已保存到本地计划。"
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
        selectedPlanId == null -> null
        selectedPlanId != deleteId && remainingPlans.any { it.id == selectedPlanId } -> selectedPlanId
        else -> remainingPlans.firstOrNull()?.id
    }

    return copy(
        plans = remainingPlans,
        selectedPlanId = nextSelectedPlanId,
        pendingDeletePlanId = null,
        statusMessage = "已删除本地计划「${deletedPlan.title}」。"
    )
}

private fun WorkoutPlan.toListItem(selected: Boolean): PlanListItemUiState {
    return PlanListItemUiState(
        id = id,
        title = title,
        mode = mode,
        modeLabel = mode.modeLabel(),
        modeBadge = mode.modeBadge(),
        planColorHex = planDisplayColorHex(),
        summary = planSummary(),
        detailSummary = planDetailSummary(),
        metrics = planMetrics(),
        selected = selected
    )
}

private fun WorkoutPlan.toDetailState(): PlanDetailUiState {
    val hasTimedCompositionPayload = hasTimedCompositionPayload()
    val startableTimedComposition = hasStartableTimedCompositionPayload()
    val timedCanStart = mode == WorkoutMode.TIMED &&
        (!hasTimedCompositionPayload || startableTimedComposition)
    return PlanDetailUiState(
        id = id,
        title = title,
        mode = mode,
        modeLabel = mode.modeLabel(),
        modeBadge = mode.modeBadge(),
        planColorHex = planDisplayColorHex(),
        summary = planSummary(),
        detailSummary = planDetailSummary(),
        metrics = planMetrics(),
        sections = detailSections(),
        editStatus = when (mode) {
            WorkoutMode.TIMED -> if (hasTimedCompositionPayload) {
                if (startableTimedComposition) {
                    "可编辑已保存的阶段编排，也可从当前计划开始计时训练。"
                } else {
                    "阶段编排暂无可执行步骤，编辑后再开始训练。"
                }
            } else {
                "可编辑已保存的阶段、轮次、颜色、图标和提醒设置，并保存回同一个本地计划。"
            }
            WorkoutMode.STRENGTH -> "可编辑已保存的动作、目标、组、休息和逐组计划，并保存回同一个本地计划。"
            WorkoutMode.FOLLOW_ALONG -> "跟练完整编排未进入本阶段，因此不提供假编辑入口。"
        },
        startStatus = when (mode) {
            WorkoutMode.TIMED -> if (!hasTimedCompositionPayload || startableTimedComposition) {
                "开始计时训练"
            } else {
                "阶段编排暂无可执行步骤"
            }
            WorkoutMode.STRENGTH -> "开始力量训练"
            WorkoutMode.FOLLOW_ALONG -> "跟练计划保存待完整编排"
        },
        editActionLabel = when (mode) {
            WorkoutMode.TIMED -> "编辑计时计划"
            WorkoutMode.STRENGTH -> "编辑力量计划"
            WorkoutMode.FOLLOW_ALONG -> "待完整编排"
        },
        canEditPlan = mode == WorkoutMode.TIMED || mode == WorkoutMode.STRENGTH,
        canStartTraining = timedCanStart || mode == WorkoutMode.STRENGTH
    )
}

private fun WorkoutPlan.planMetrics(): List<PlanMetricUiState> {
    return when (mode) {
        WorkoutMode.TIMED -> {
            val compositions = blocks.filterIsInstance<TimedCompositionBlock>()
            if (compositions.isNotEmpty()) {
                val restValues = compositions.flatMap { block ->
                    block.stageGroups.flatMap { group ->
                        group.targets
                            .filter { target -> target.kind == TimedCompositionTargetKind.REST }
                            .map { target -> target.durationSec }
                    } + listOf(block.restBetweenRoundsSec)
                }
                listOf(
                    PlanMetricUiState("阶段", "${compositions.sumOf { it.stageGroups.size }} 个"),
                    PlanMetricUiState("轮次", "${compositions.sumOf { it.rounds }} 轮"),
                    PlanMetricUiState("时长", estimatedTimedDurationSec().formatDuration()),
                    PlanMetricUiState("休息", restValues.filter { it > 0 }.distinct().toMetricDuration())
                )
            } else {
                val circuits = blocks.filterIsInstance<TimedCircuitBlock>()
                val restValues = circuits.flatMap { block ->
                    block.items.mapNotNull { it.restAfterSec } + listOfNotNull(block.restBetweenRoundsSec)
                } + blocks.filterIsInstance<RestBlock>().map { it.durationSec }
                listOf(
                    PlanMetricUiState("阶段", "${circuits.sumOf { it.items.size }} 个"),
                    PlanMetricUiState("轮次", "${circuits.sumOf { it.rounds }} 轮"),
                    PlanMetricUiState("时长", estimatedTimedDurationSec().formatDuration()),
                    PlanMetricUiState("休息", restValues.distinct().toMetricDuration())
                )
            }
        }

        WorkoutMode.STRENGTH -> {
            val strengthBlocks = blocks.filterIsInstance<StrengthExerciseBlock>()
            val restValues = strengthBlocks.flatMap { block ->
                listOfNotNull(block.target?.restAfterSetSec) + block.sets.mapNotNull { it.restAfterSec }
            }.distinct()
            listOf(
                PlanMetricUiState("动作", "${strengthBlocks.size} 个"),
                PlanMetricUiState("组数", "${strengthBlocks.sumOf { it.sets.size }} 组"),
                PlanMetricUiState("休息", restValues.toMetricDuration())
            )
        }

        WorkoutMode.FOLLOW_ALONG -> listOf(PlanMetricUiState("模式", "跟练雏形"))
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
            val compositions = blocks.filterIsInstance<TimedCompositionBlock>()
            if (compositions.isNotEmpty()) {
                "${compositions.sumOf { it.stageGroups.size }} 个阶段 · ${compositions.sumOf { it.rounds }} 轮 · 预计 ${estimatedTimedDurationSec().formatDuration()}"
            } else {
                val circuitCount = blocks.filterIsInstance<TimedCircuitBlock>().sumOf { it.items.size }
                val rounds = blocks.filterIsInstance<TimedCircuitBlock>().sumOf { it.rounds }
                "$circuitCount 个阶段 · $rounds 轮 · 预计 ${estimatedTimedDurationSec().formatDuration()}"
            }
        }

        WorkoutMode.STRENGTH -> {
            val strengthBlocks = blocks.filterIsInstance<StrengthExerciseBlock>()
            "${strengthBlocks.size} 个动作 · ${strengthBlocks.sumOf { it.sets.size }} 组"
        }

        WorkoutMode.FOLLOW_ALONG -> "跟练雏形计划 · 待完整编排"
    }
}

private fun WorkoutPlan.planDetailSummary(): String {
    return when (mode) {
        WorkoutMode.TIMED -> {
            if (hasTimedCompositionPayload()) {
                if (hasStartableTimedCompositionPayload()) {
                    "阶段编排已保存 · 可开始计时训练"
                } else {
                    "阶段编排暂无可执行步骤"
                }
            } else {
                val cue = preferences?.cueSettings
                val actionCue = cue?.actionEnding?.thresholdSec?.let { "阶段提醒 ${it}秒" } ?: "阶段提醒未设"
                val restCue = cue?.restEnding?.thresholdSec?.let { "休息提醒 ${it}秒" } ?: "休息提醒关闭"
                "$actionCue · $restCue"
            }
        }

        WorkoutMode.STRENGTH -> {
            val restValues = blocks.filterIsInstance<StrengthExerciseBlock>()
                .mapNotNull { it.target?.restAfterSetSec }
                .distinct()
            val restSummary = if (restValues.size == 1) "${restValues.single()}秒休息" else "按动作休息"
            "$restSummary · ${strengthSetTimerModeSummary()} · 计划值预填实际记录"
        }

        WorkoutMode.FOLLOW_ALONG -> "复用计时流程和动作内容"
    }
}

private fun WorkoutPlan.planDisplayColorHex(): String {
    return "#F44336"
}

private fun WorkoutPlan.detailSections(): List<PlanDetailSectionUiState> {
    return when (mode) {
        WorkoutMode.TIMED -> timedDetailSections()
        WorkoutMode.STRENGTH -> strengthDetailSections()
        WorkoutMode.FOLLOW_ALONG -> listOf(
            PlanDetailSectionUiState(
                title = "边界",
                rows = listOf("跟练计划元数据已保留，完整跟练编排和保存入口留给对应 story。")
            )
        )
    }
}

private fun WorkoutPlan.timedDetailSections(): List<PlanDetailSectionUiState> {
    val blockRows = blocks.map { block ->
        when (block) {
            is WarmupBlock -> "热身 · ${block.durationSec?.formatDuration() ?: "按动作"}"
            is TimedCircuitBlock -> {
                val stageNames = block.items.joinToString("、") { item -> item.exerciseLabel() }
                "训练阶段 · ${block.rounds} 轮 · $stageNames"
            }

            is StretchBlock -> "拉伸 · ${block.durationSec?.formatDuration() ?: "按动作"}"
            is RestBlock -> "休息 · ${block.durationSec.formatDuration()}"
            is CooldownBlock -> "冷却 · ${block.durationSec?.formatDuration() ?: "按动作"}"
            is StrengthExerciseBlock -> "力量动作 · ${block.exerciseLabel()}"
            is TimedCompositionBlock -> {
                val groupRows = block.stageGroups.joinToString("、") { group -> group.name }
                "阶段编排 · ${block.stageGroups.size} 个阶段 · ${block.rounds} 轮 · $groupRows"
            }
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
            "${kind.displayLabel()} ${sets.size}组"
        }
        "${block.exerciseLabel()} · $setSummary"
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

private fun WorkoutPlan.strengthSetTimerModeSummary(): String {
    val modes = blocks.filterIsInstance<StrengthExerciseBlock>()
        .map { block -> block.setTimerMode }
        .distinct()
    if (modes.isEmpty()) return "本组计时未设置"
    return when (modes.singleOrNull()) {
        StrengthSetTimerMode.AUTO_AFTER_REST -> "休息后自动开始下一组"
        StrengthSetTimerMode.MANUAL_START -> "手动开始下一组"
        null -> "按动作设置"
    }
}

private fun StrengthSetKind.displayLabel(): String {
    return when (this) {
        StrengthSetKind.WARMUP -> "热身"
        StrengthSetKind.WORKING -> "正式"
        StrengthSetKind.DROP -> "递减"
        StrengthSetKind.BACKOFF -> "退阶"
    }
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
            is TimedCompositionBlock -> {
                val repeatedDuration = block.stageGroups.sumOf { group -> group.durationSec }
                block.warmupSec + block.cooldownSec + repeatedDuration * block.rounds +
                    block.restBetweenRoundsSec * (block.rounds - 1).coerceAtLeast(0)
            }
        }
    }
}

private fun WorkoutPlan.hasTimedCompositionPayload(): Boolean {
    return blocks.any { block -> block is TimedCompositionBlock }
}

private fun List<TimedExerciseItem>.sumTimedItemsOnce(): Int {
    return sumOf { item -> item.workDurationSec + item.restAfterSec.orZero() }
}

private fun TimedExerciseItem.exerciseLabel(): String {
    return labelOverride ?: exerciseId?.let(::exerciseName) ?: stageType.displayName
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

            is TimedCompositionBlock -> block
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

private fun List<WorkoutPlan>.replacePlan(updatedPlan: WorkoutPlan): List<WorkoutPlan> {
    return map { plan -> if (plan.id == updatedPlan.id) updatedPlan else plan }
}
