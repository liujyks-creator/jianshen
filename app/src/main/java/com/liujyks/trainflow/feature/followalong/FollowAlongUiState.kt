package com.liujyks.trainflow.feature.followalong

import com.liujyks.trainflow.core.data.fixture.ActionExerciseFixture
import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.FollowAlongPlanMeta
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan

internal const val DefaultFollowAlongTimestamp = "2026-06-02T00:00:00Z"

internal data class FollowAlongScreenState(
    val summary: String,
    val plans: List<FollowAlongPlanUiState>,
    val emptyStateTitle: String,
    val emptyStateDescription: String
) {
    val isEmpty: Boolean = plans.isEmpty()
}

internal data class FollowAlongPlanUiState(
    val plan: WorkoutPlan,
    val title: String,
    val badge: String,
    val summary: String,
    val mediaStatus: String,
    val actionRows: List<String>,
    val boundaryRows: List<String>,
    val nextStepStatus: String,
    val canStartFollowAlong: Boolean
)

internal fun buildDefaultFollowAlongScreenState(
    entries: List<ActionExerciseFixture> = FirstActionExerciseFixtures.entries,
    timestamp: String = DefaultFollowAlongTimestamp
): FollowAlongScreenState {
    val followAlongEntries = entries.filter { entry ->
        entry.exercise.capabilities.supportsFollowAlong &&
            entry.exercise.capabilities.supportsTimedTraining &&
            entry.timedDefault != null
    }

    val plan = buildBasicFollowAlongPlan(
        entries = followAlongEntries,
        timestamp = timestamp
    )

    return FollowAlongScreenState(
        summary = "基础跟练/雏形体验：复用计时计划结构和首批动作短提示，只从内存态 preset 启动。",
        plans = plan?.let { listOf(it.toFollowAlongPlanUiState(followAlongEntries)) }.orEmpty(),
        emptyStateTitle = "暂无可跟练内容",
        emptyStateDescription = "当前动作 fixture 中没有同时支持跟练和计时流程的动作，因此不展示虚假的跟练入口。"
    )
}

private fun buildBasicFollowAlongPlan(
    entries: List<ActionExerciseFixture>,
    timestamp: String
): WorkoutPlan? {
    val seedEntries = entries
        .preferExerciseIds(
            "jumping-jacks",
            "bodyweight-squat",
            "forearm-plank",
            "glute-bridge"
        )
        .take(4)

    if (seedEntries.isEmpty()) return null

    return WorkoutPlan(
        id = "follow-along-basic-flow",
        mode = WorkoutMode.FOLLOW_ALONG,
        title = "基础跟练：全身动作提示",
        description = "E6.2 内存态基础跟练计划，只用于雏形执行体验验证。",
        blocks = listOf(
            TimedCircuitBlock(
                id = "follow-along-basic-circuit",
                order = 1,
                title = "基础跟练流程",
                rounds = 1,
                items = seedEntries.mapIndexed { index, entry ->
                    entry.toFollowAlongTimedItem(index + 1)
                }
            )
        ),
        preferences = PlanPreferences(
            cueSettings = CueSettings(
                actionEnding = CountdownCue(thresholdSec = 5),
                restEnding = CountdownCue(thresholdSec = 5)
            )
        ),
        followAlong = FollowAlongPlanMeta(preset = true),
        createdAt = timestamp,
        updatedAt = timestamp
    )
}

private fun List<ActionExerciseFixture>.preferExerciseIds(vararg ids: String): List<ActionExerciseFixture> {
    val byId = associateBy { it.exercise.id }
    val preferred = ids.mapNotNull { id -> byId[id] }
    val remaining = filterNot { entry -> ids.contains(entry.exercise.id) }
    return preferred + remaining
}

private fun ActionExerciseFixture.toFollowAlongTimedItem(order: Int): TimedExerciseItem {
    val default = requireNotNull(timedDefault) {
        "Follow-along seed can only consume timed fixture defaults."
    }

    return TimedExerciseItem(
        id = "follow-along-item-$order-${exercise.id}",
        exerciseId = exercise.id,
        side = default.side,
        workDurationSec = default.workDurationSec,
        restAfterSec = default.restAfterSec,
        autoAdvance = true
    )
}

private fun WorkoutPlan.toFollowAlongPlanUiState(
    sourceEntries: List<ActionExerciseFixture>
): FollowAlongPlanUiState {
    val actionRows = blocks
        .filterIsInstance<TimedCircuitBlock>()
        .flatMap { block -> block.items }
        .map { item ->
            val exercise = sourceEntries.firstOrNull { entry -> entry.exercise.id == item.exerciseId }?.exercise
            val name = exercise?.name ?: item.exerciseId
            val cue = exercise?.instructions?.shortCue ?: "使用首批动作短提示。"
            "$name · ${item.workDurationSec}秒 · $cue"
        }

    return FollowAlongPlanUiState(
        plan = this,
        title = title,
        badge = "基础跟练 / 雏形体验",
        summary = "${actionRows.size} 个动作 · 预计 ${estimatedTimedDurationSec().formatFollowAlongDuration()}",
        mediaStatus = "动作演示媒体位已保留；首版没有教练视频播放，当前使用动作短提示作为内容来源。",
        actionRows = actionRows,
        boundaryRows = listOf(
            "只复用计时流程与首批 fixture 动作内容。",
            "不提供真实媒体播放、动作分析、音乐编排或自动口令。",
            "后续是否支持兼容计时计划进入跟练视图，仍保留为 O-002。"
        ),
        nextStepStatus = "开始基础跟练",
        canStartFollowAlong = true
    )
}

internal fun WorkoutPlan.followAlongActionExerciseIds(): List<String> {
    return blocks
        .filterIsInstance<TimedCircuitBlock>()
        .flatMap { block -> block.items.mapNotNull { item -> item.exerciseId } }
}

private fun WorkoutPlan.estimatedTimedDurationSec(): Int {
    return blocks.sumOf { block ->
        when (block) {
            is TimedCircuitBlock -> {
                val roundDuration = block.items.sumOf { item -> item.workDurationSec + item.restAfterSec.orZero() }
                val roundRest = block.restBetweenRoundsSec.orZero() * (block.rounds - 1).coerceAtLeast(0)
                roundDuration * block.rounds + roundRest
            }

            else -> 0
        }
    }
}

private fun Int.formatFollowAlongDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return if (seconds == 0) "${minutes}分" else "${minutes}分${seconds}秒"
}

private fun Int?.orZero(): Int = this ?: 0
