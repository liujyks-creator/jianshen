package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.data.WorkoutSessionHistoricalResult
import com.liujyks.trainflow.core.data.WorkoutSessionStrictReadResult

internal data class FollowAlongPersistedSummary(
    val totalElapsedSec: Int
)

internal data class FollowAlongWorkoutSessionUiState(
    val title: String,
    val statusLabel: String,
    val timerText: String,
    val isTerminal: Boolean,
    val canStop: Boolean,
    val canReturn: Boolean,
    val immediateControls: List<WorkoutImmediateControlUiState>,
    val endRequiresConfirmation: Boolean,
    val terminalTitle: String?,
    val terminalSummary: String?
)

internal fun WorkoutSessionHistoricalResult.Resolved.toPersistedFreeFollowAlongSummary(): FollowAlongPersistedSummary {
    val source = this.source as WorkoutSessionStrictReadResult.CanonicalTerminal
    val graph = source.graph
    return FollowAlongPersistedSummary(
        totalElapsedSec = requireNotNull(graph.session.totalElapsedSec)
    )
}

internal fun buildFollowAlongWorkoutSessionUiState(
    elapsedSec: Int,
    active: Boolean,
    stopping: Boolean = false,
    saved: Boolean = false,
    persistedSummary: FollowAlongPersistedSummary? = null,
    saveFailure: Throwable? = null,
    recapReadFailure: Throwable? = null,
    initializationFailure: Throwable? = null
): FollowAlongWorkoutSessionUiState {
    val canStop = active && !stopping
    val isTerminal = stopping || saved || saveFailure != null || initializationFailure != null
    val summary = when {
        initializationFailure != null -> "开始失败：${initializationFailure.message}"
        saveFailure != null -> "保存失败：${saveFailure.message}"
        recapReadFailure != null -> "读取已保存记录失败：${recapReadFailure.message}"
        persistedSummary != null ->
            "本次跟练 ${persistedSummary.totalElapsedSec} 秒"
        saved -> "正在读取已保存记录…"
        stopping -> "正在保存本次跟练…"
        else -> null
    }
    return FollowAlongWorkoutSessionUiState(
        title = "跟练",
        statusLabel = when {
            initializationFailure != null -> "开始失败"
            saveFailure != null -> "保存失败"
            saved -> "已保存"
            stopping -> "正在保存"
            active -> "跟练中"
            else -> "准备"
        },
        timerText = (persistedSummary?.totalElapsedSec ?: elapsedSec).formatFollowAlongTimer(),
        isTerminal = isTerminal,
        canStop = canStop,
        canReturn = saved || saveFailure != null || initializationFailure != null,
        immediateControls = if (canStop) listOf(
            WorkoutImmediateControlUiState(
                role = WorkoutImmediateControlRole.END_SESSION,
                label = "停止跟练",
                enabled = true,
                placement = WorkoutImmediateControlPlacement.FIXED_BOTTOM
            )
        ) else emptyList(),
        endRequiresConfirmation = canStop,
        terminalTitle = if (saved) "跟练完成" else if (isTerminal) "跟练未完成" else null,
        terminalSummary = summary
    )
}

private fun Int.formatFollowAlongTimer(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
