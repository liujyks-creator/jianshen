package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.RestBlock
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan

internal const val DefaultPlanManagementTimestamp = "2026-05-29T00:00:00Z"

internal data class PlanManagementScreenState(
    val plans: List<WorkoutPlan>,
    val selectedPlanId: String? = plans.firstOrNull()?.id,
    val pendingDeletePlanId: String? = null,
    val statusMessage: String? = null
) {
    val isEmpty: Boolean = plans.isEmpty()

    val listItems: List<PlanListItemUiState>
        get() = plans.map { plan ->
            plan.toListItem(selected = plan.id == selectedPlanId)
        }

    val selectedPlan: WorkoutPlan?
        get() = plans.firstOrNull { it.id == selectedPlanId } ?: plans.firstOrNull()

    val selectedDetail: PlanDetailUiState?
        get() = selectedPlan?.toDetailState()

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
    val selected: Boolean
)

internal data class PlanDetailUiState(
    val id: String,
    val title: String,
    val modeLabel: String,
    val modeBadge: String,
    val summary: String,
    val detailSummary: String,
    val sections: List<PlanDetailSectionUiState>,
    val editStatus: String,
    val startStatus: String,
    val canStartTraining: Boolean = false
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
        selected = selected
    )
}

private fun WorkoutPlan.toDetailState(): PlanDetailUiState {
    return PlanDetailUiState(
        id = id,
        title = title,
        modeLabel = mode.modeLabel(),
        modeBadge = mode.modeBadge(),
        summary = planSummary(),
        detailSummary = planDetailSummary(),
        sections = detailSections(),
        editStatus = "编辑回填后续接入",
        startStatus = when (mode) {
            WorkoutMode.TIMED -> "开始计时训练"
            WorkoutMode.STRENGTH -> "开始力量训练"
            WorkoutMode.FOLLOW_ALONG -> "跟练闭环留给 E6"
        },
        canStartTraining = mode == WorkoutMode.TIMED || mode == WorkoutMode.STRENGTH
    )
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
