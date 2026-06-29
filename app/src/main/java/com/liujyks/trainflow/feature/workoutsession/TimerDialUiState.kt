package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedSessionStep
import com.liujyks.trainflow.core.engine.TimedSessionStepKind
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionTimeline
import com.liujyks.trainflow.core.model.TimedCompositionTimelineAdapter
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStageKind
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStep
import com.liujyks.trainflow.core.model.TimedCompositionTimelineTargetKind
import com.liujyks.trainflow.core.model.TimedStageIconKey
import com.liujyks.trainflow.core.model.TimedStageStyle
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.normalizeTimedStageIconKey
import com.liujyks.trainflow.core.model.normalizeStageColorHex
import com.liujyks.trainflow.core.model.stageTextColorHexFor
import com.liujyks.trainflow.ui.theme.TrainFlowMotionTokens

internal data class TimerDialUiState(
    val totalRemainingSec: Int,
    val totalProgress: Float,
    val currentStageProgress: Float,
    val currentStageType: TimerDialStageType,
    val currentStageLabel: String,
    val currentStageIndex: Int,
    val currentStageRemainingSec: Int,
    val isPaused: Boolean,
    val isFinalCountdown: Boolean,
    val totalWorkoutStageCount: Int,
    val completedWorkoutStageCount: Int,
    val stageSegments: List<TimerDialStageSegmentUiState>,
    val visualVariant: TimerDialVisualVariant,
    val currentStageColorHex: String,
    val currentStageTextColorHex: String,
    val currentStageIconKey: String,
    val currentStageTimeText: String,
    val totalRemainingText: String,
    val centerActionLabel: String,
    val canTogglePause: Boolean
) {
    companion object {
        val Empty = TimerDialUiState(
            totalRemainingSec = 0,
            totalProgress = 0f,
            currentStageProgress = 0f,
            currentStageType = TimerDialStageType.WORK,
            currentStageLabel = "准备开始",
            currentStageIndex = 0,
            currentStageRemainingSec = 0,
            isPaused = false,
            isFinalCountdown = false,
            totalWorkoutStageCount = 0,
            completedWorkoutStageCount = 0,
            stageSegments = emptyList(),
            visualVariant = ProductionTimerDialVisualVariant,
            currentStageColorHex = TimedStageType.WORK.defaultColorHex,
            currentStageTextColorHex = "#FFFFFF",
            currentStageIconKey = "timer",
            currentStageTimeText = "00:00",
            totalRemainingText = "00:00",
            centerActionLabel = "当前不可切换",
            canTogglePause = false
        )
    }

    fun clamped(): TimerDialUiState {
        val safeTotalRemainingSec = totalRemainingSec.coerceAtLeast(0)
        val safeCurrentStageRemainingSec = currentStageRemainingSec.coerceAtLeast(0)
        return copy(
            totalProgress = totalProgress.clampedProgress(),
            currentStageProgress = currentStageProgress.clampedProgress(),
            totalWorkoutStageCount = totalWorkoutStageCount.coerceAtLeast(0),
            completedWorkoutStageCount = completedWorkoutStageCount.coerceIn(
                0,
                totalWorkoutStageCount.coerceAtLeast(0)
            ),
            stageSegments = stageSegments.map { segment -> segment.clamped() },
            currentStageColorHex = normalizeStageColorHex(currentStageColorHex, currentStageType.toTimedStageType()),
            currentStageTextColorHex = currentStageTextColorHex.takeIf { isValidColorHex(it) } ?: "#FFFFFF",
            currentStageRemainingSec = safeCurrentStageRemainingSec,
            totalRemainingSec = safeTotalRemainingSec,
            currentStageTimeText = safeCurrentStageRemainingSec.formatTimerText(),
            totalRemainingText = safeTotalRemainingSec.formatTimerText()
        )
    }
}

internal data class TimerDialStageSegmentUiState(
    val id: String,
    val label: String,
    val stageType: TimerDialStageType,
    val durationSec: Int,
    val progress: Float,
    val isCurrent: Boolean,
    val colorHex: String
) {
    fun clamped(): TimerDialStageSegmentUiState {
        return copy(
            durationSec = durationSec.coerceAtLeast(0),
            progress = progress.clampedProgress(),
            colorHex = normalizeStageColorHex(colorHex, stageType.toTimedStageType())
        )
    }
}

internal enum class TimerDialInnerMarkerRole {
    BASE_DOT,
    TOTAL_COUNT,
    COMPLETED_DOT,
    LATEST_COMPLETED
}

internal data class TimerDialInnerMarkerUiState(
    val index: Int,
    val progress: Float,
    val role: TimerDialInnerMarkerRole,
    val label: String?
)

internal fun timerDialMarkerProgress(index: Int, count: Int): Float {
    val safeCount = count.coerceAtLeast(1)
    return index.coerceIn(0, safeCount).toFloat() / safeCount.toFloat()
}

internal fun TimerDialUiState.innerMarkerData(): List<TimerDialInnerMarkerUiState> {
    val markerCount = totalWorkoutStageCount.coerceAtLeast(0)
    if (markerCount == 0) {
        return emptyList()
    }

    val completedCount = completedWorkoutStageCount.coerceIn(0, markerCount)
    return List(markerCount) { index ->
        val role = when {
            index == 0 -> TimerDialInnerMarkerRole.TOTAL_COUNT
            completedCount in 1 until markerCount && index == completedCount ->
                TimerDialInnerMarkerRole.LATEST_COMPLETED
            index in 1 until completedCount -> TimerDialInnerMarkerRole.COMPLETED_DOT
            else -> TimerDialInnerMarkerRole.BASE_DOT
        }
        TimerDialInnerMarkerUiState(
            index = index,
            progress = timerDialMarkerProgress(index = index, count = markerCount),
            role = role,
            label = when (role) {
                TimerDialInnerMarkerRole.TOTAL_COUNT -> markerCount.toString()
                TimerDialInnerMarkerRole.LATEST_COMPLETED -> completedCount.toString()
                TimerDialInnerMarkerRole.BASE_DOT,
                TimerDialInnerMarkerRole.COMPLETED_DOT -> null
            }
        )
    }
}

internal fun TimedWorkoutEngineState.toTimerDialUiState(
    screenState: TimedWorkoutSessionScreenState,
    visualVariant: TimerDialVisualVariant = ProductionTimerDialVisualVariant,
    planBlocks: List<PlanBlock> = emptyList()
): TimerDialUiState {
    val totalPlannedDurationSec = steps.sumOf { step -> step.durationSec }.coerceAtLeast(1)
    val current = currentStep
    val currentStepProgress = current?.let {
        currentStepDisplayProgress()
    } ?: when (status) {
        SessionStatus.COMPLETED -> 1f
        else -> 0f
    }
    val totalRemainingSec = when {
        status == SessionStatus.COMPLETED -> 0
        status == SessionStatus.ABANDONED -> remainingSec
        currentStepIndex < 0 -> totalPlannedDurationSec
        else -> remainingSec + steps.drop(currentStepIndex + 1).sumOf { step -> step.durationSec }
    }
    timerDialCompositionMapping(
        screenState = screenState,
        currentStepProgress = currentStepProgress,
        totalRemainingSec = totalRemainingSec,
        visualVariant = visualVariant,
        planBlocks = planBlocks
    )?.let { mapping ->
        return mapping
    }

    val currentIndex = if (currentStepIndex >= 0) {
        (currentStepIndex + 1).coerceAtMost(steps.size)
    } else {
        0
    }
    val currentType = current?.timerDialStageType() ?: TimerDialStageType.WORK
    val segments = currentTimerDialCycleSegments(
        currentStepProgress = currentStepProgress
    )
    val workoutCycles = timerDialWorkoutCycles()
    val stageBasedTotalProgress = workoutCycles.stageBasedProgress(
        state = this,
        currentStepProgress = currentStepProgress,
        fallbackTotalPlannedDurationSec = totalPlannedDurationSec
    )
    val completedWorkoutStageCount = workoutCycles.completedCount(
        state = this
    )

    return TimerDialUiState(
        totalRemainingSec = totalRemainingSec,
        totalProgress = when (status) {
            SessionStatus.COMPLETED -> 1f
            else -> stageBasedTotalProgress
        },
        currentStageProgress = currentStepProgress,
        currentStageType = currentType,
        currentStageLabel = screenState.currentTitle,
        currentStageIndex = currentIndex,
        currentStageRemainingSec = if (status == SessionStatus.COMPLETED) 0 else remainingSec,
        isPaused = status == SessionStatus.PAUSED,
        isFinalCountdown = screenState.countdownReminder.isActive &&
            screenState.countdownReminder.emphasisAnimationEnabled &&
            status == SessionStatus.ACTIVE,
        totalWorkoutStageCount = workoutCycles.size,
        completedWorkoutStageCount = completedWorkoutStageCount,
        stageSegments = segments,
        visualVariant = visualVariant,
        currentStageColorHex = normalizeStageColorHex(screenState.stageColorHex, currentType.toTimedStageType()),
        currentStageTextColorHex = stageTextColorHexFor(screenState.stageColorHex, currentType.toTimedStageType()),
        currentStageIconKey = screenState.stageIconKey,
        currentStageTimeText = screenState.timerText,
        totalRemainingText = screenState.totalRemainingText,
        centerActionLabel = centerActionLabel(screenState),
        canTogglePause = screenState.canPause || screenState.canResume
    ).clamped()
}

private fun TimedWorkoutEngineState.timerDialCompositionMapping(
    screenState: TimedWorkoutSessionScreenState,
    currentStepProgress: Float,
    totalRemainingSec: Int,
    visualVariant: TimerDialVisualVariant,
    planBlocks: List<PlanBlock>
): TimerDialUiState? {
    val sourceBlocks = planBlocks.filterIsInstance<TimedCompositionBlock>()
    if (sourceBlocks.isEmpty()) {
        return null
    }

    val current = currentStep
    if (current == null) {
        if (status != SessionStatus.COMPLETED) return null
        val completedTimeline = sourceBlocks
            .asSequence()
            .mapNotNull { block -> block.safeTimerDialTimeline()?.let { timeline -> block to timeline } }
            .firstOrNull { (_, timeline) -> timeline.steps.isNotEmpty() }
            ?: return null
        val totalStageCount = completedTimeline.second.stageInstanceCount
        return TimerDialUiState(
            totalRemainingSec = 0,
            totalProgress = 1f,
            currentStageProgress = 1f,
            currentStageType = TimerDialStageType.WORK,
            currentStageLabel = screenState.currentTitle,
            currentStageIndex = totalStageCount,
            currentStageRemainingSec = 0,
            isPaused = false,
            isFinalCountdown = false,
            totalWorkoutStageCount = totalStageCount,
            completedWorkoutStageCount = totalStageCount,
            stageSegments = emptyList(),
            visualVariant = visualVariant,
            currentStageColorHex = normalizeStageColorHex(screenState.stageColorHex, TimedStageType.WORK),
            currentStageTextColorHex = stageTextColorHexFor(screenState.stageColorHex, TimedStageType.WORK),
            currentStageIconKey = screenState.stageIconKey,
            currentStageTimeText = screenState.timerText,
            totalRemainingText = screenState.totalRemainingText,
            centerActionLabel = centerActionLabel(screenState),
            canTogglePause = screenState.canPause || screenState.canResume
        ).clamped()
    }

    val match = sourceBlocks
        .asSequence()
        .mapNotNull { block ->
            val timeline = block.safeTimerDialTimeline() ?: return@mapNotNull null
            val activeStep = timeline.steps.firstOrNull { step ->
                step.id == current.id && step.compositionBlockId == current.blockId
            } ?: return@mapNotNull null
            TimerDialCompositionMatch(
                sourceBlock = block,
                timeline = timeline,
                activeStep = activeStep
            )
        }
        .firstOrNull()
        ?: return null

    val segments = match.timerDialSegments(currentStepProgress = currentStepProgress)
    val activeStageProgress = segments.weightedProgress()
    val totalStageCount = match.timeline.stageInstanceCount.coerceAtLeast(1)
    val completedStageCount = if (status == SessionStatus.COMPLETED) {
        totalStageCount
    } else {
        (match.activeStep.stageInstanceIndex - 1).coerceAtLeast(0)
    }
    val currentStageType = match.activeStep.timerDialStageType()
    val currentColorHex = match.activeStep.timerDialColorHex(match.sourceBlock)
    val currentIconKey = match.activeStep.timerDialIconKey(match.sourceBlock)

    return TimerDialUiState(
        totalRemainingSec = if (status == SessionStatus.COMPLETED) 0 else totalRemainingSec,
        totalProgress = when (status) {
            SessionStatus.COMPLETED -> 1f
            else -> (completedStageCount.toFloat() + activeStageProgress) / totalStageCount.toFloat()
        },
        currentStageProgress = currentStepProgress,
        currentStageType = currentStageType,
        currentStageLabel = screenState.currentTitle,
        currentStageIndex = match.activeStep.stageInstanceIndex,
        currentStageRemainingSec = if (status == SessionStatus.COMPLETED) 0 else remainingSec,
        isPaused = status == SessionStatus.PAUSED,
        isFinalCountdown = screenState.countdownReminder.isActive &&
            screenState.countdownReminder.emphasisAnimationEnabled &&
            status == SessionStatus.ACTIVE,
        totalWorkoutStageCount = totalStageCount,
        completedWorkoutStageCount = completedStageCount,
        stageSegments = segments,
        visualVariant = visualVariant,
        currentStageColorHex = currentColorHex,
        currentStageTextColorHex = stageTextColorHexFor(currentColorHex, currentStageType.toTimedStageType()),
        currentStageIconKey = currentIconKey,
        currentStageTimeText = screenState.timerText,
        totalRemainingText = screenState.totalRemainingText,
        centerActionLabel = centerActionLabel(screenState),
        canTogglePause = screenState.canPause || screenState.canResume
    ).clamped()
}

private data class TimerDialCompositionMatch(
    val sourceBlock: TimedCompositionBlock,
    val timeline: TimedCompositionTimeline,
    val activeStep: TimedCompositionTimelineStep
)

private fun TimedCompositionBlock.safeTimerDialTimeline(): TimedCompositionTimeline? {
    return runCatching { TimedCompositionTimelineAdapter.expand(this) }.getOrNull()
}

private fun TimerDialCompositionMatch.timerDialSegments(
    currentStepProgress: Float
): List<TimerDialStageSegmentUiState> {
    val stageSteps = if (activeStep.timelineStageKind == TimedCompositionTimelineStageKind.STAGE_GROUP) {
        timeline.steps.filter { step -> step.timelineStageId == activeStep.timelineStageId }
    } else {
        listOf(activeStep)
    }

    return stageSteps.map { step ->
        val progress = when {
            step.targetInstanceIndex < activeStep.targetInstanceIndex -> 1f
            step.id == activeStep.id -> currentStepProgress
            else -> 0f
        }
        step.toTimerDialStageSegment(
            sourceBlock = sourceBlock,
            progress = progress,
            isCurrent = step.id == activeStep.id
        )
    }
}

private fun TimedCompositionTimelineStep.toTimerDialStageSegment(
    sourceBlock: TimedCompositionBlock,
    progress: Float,
    isCurrent: Boolean
): TimerDialStageSegmentUiState {
    val stageType = timerDialStageType()
    return TimerDialStageSegmentUiState(
        id = id,
        label = displayName,
        stageType = stageType,
        durationSec = plannedDurationSec,
        progress = progress,
        isCurrent = isCurrent,
        colorHex = timerDialColorHex(sourceBlock)
    ).clamped()
}

private fun TimedCompositionTimelineStep.timerDialColorHex(sourceBlock: TimedCompositionBlock): String {
    val stageType = timerDialStageType().toTimedStageType()
    if (timelineStageKind != TimedCompositionTimelineStageKind.STAGE_GROUP) {
        val boundaryColorHex = boundaryStyle(sourceBlock)?.colorHex
        return when {
            isValidColorHex(boundaryColorHex) -> normalizeStageColorHex(boundaryColorHex, stageType)
            isValidColorHex(colorHex) -> normalizeStageColorHex(colorHex, stageType)
            else -> stageType.defaultColorHex
        }
    }

    val group = sourceBlock.stageGroups.firstOrNull { group -> group.id == stageGroupId }
    val target = group?.targets?.firstOrNull { target -> target.id == targetId }
    val targetColorHex = target?.colorHex
    val groupColorHex = group?.colorHex
    return when {
        isValidColorHex(targetColorHex) -> normalizeStageColorHex(targetColorHex, stageType)
        isValidColorHex(groupColorHex) -> normalizeStageColorHex(groupColorHex, stageType)
        isValidColorHex(colorHex) -> normalizeStageColorHex(colorHex, stageType)
        else -> stageType.defaultColorHex
    }
}

private fun TimedCompositionTimelineStep.timerDialIconKey(sourceBlock: TimedCompositionBlock): String {
    val stageType = timerDialStageType().toTimedStageType()
    if (timelineStageKind != TimedCompositionTimelineStageKind.STAGE_GROUP) {
        return normalizeTimedStageIconKey(boundaryStyle(sourceBlock)?.iconKey)
            ?: normalizeTimedStageIconKey(iconKey)
            ?: boundaryDefaultIconKey()
            ?: normalizeTimedStageIconKey(stageType.defaultIconKey)
            ?: TimedStageIconKey.CUSTOM.contractValue
    }

    val group = sourceBlock.stageGroups.firstOrNull { group -> group.id == stageGroupId }
    val target = group?.targets?.firstOrNull { target -> target.id == targetId }
    return normalizeTimedStageIconKey(target?.iconKey)
        ?: normalizeTimedStageIconKey(group?.iconKey)
        ?: normalizeTimedStageIconKey(iconKey)
        ?: normalizeTimedStageIconKey(stageType.defaultIconKey)
        ?: TimedStageIconKey.CUSTOM.contractValue
}

private fun TimedCompositionTimelineStep.boundaryStyle(sourceBlock: TimedCompositionBlock): TimedStageStyle? {
    return when (timelineStageKind) {
        TimedCompositionTimelineStageKind.WARMUP -> sourceBlock.warmupStyle
        TimedCompositionTimelineStageKind.COOLDOWN -> sourceBlock.cooldownStyle
        TimedCompositionTimelineStageKind.BETWEEN_ROUND_REST -> sourceBlock.restBetweenRoundsStyle
        TimedCompositionTimelineStageKind.STAGE_GROUP -> null
    }
}

private fun TimedCompositionTimelineStep.boundaryDefaultIconKey(): String? {
    return when (timelineStageKind) {
        TimedCompositionTimelineStageKind.WARMUP -> TimedStageType.WARMUP.defaultIconKey
        TimedCompositionTimelineStageKind.COOLDOWN -> TimedStageType.COOLDOWN.defaultIconKey
        TimedCompositionTimelineStageKind.BETWEEN_ROUND_REST -> TimedStageIconKey.RECOVER_BREATHE.contractValue
        TimedCompositionTimelineStageKind.STAGE_GROUP -> null
    }
}

private fun TimedCompositionTimelineStep.timerDialStageType(): TimerDialStageType {
    return when (targetKind) {
        TimedCompositionTimelineTargetKind.ACTION -> TimerDialStageType.WORK
        TimedCompositionTimelineTargetKind.REST -> TimerDialStageType.REST
        TimedCompositionTimelineTargetKind.CUSTOM -> TimerDialStageType.CUSTOM
        TimedCompositionTimelineTargetKind.WARMUP -> TimerDialStageType.WARMUP
        TimedCompositionTimelineTargetKind.COOLDOWN -> TimerDialStageType.COOLDOWN
        TimedCompositionTimelineTargetKind.BETWEEN_ROUND_REST -> TimerDialStageType.REST
    }
}

private fun List<TimerDialStageSegmentUiState>.weightedProgress(): Float {
    val totalDurationSec = sumOf { segment -> segment.durationSec.coerceAtLeast(0) }
    if (totalDurationSec <= 0) return 0f

    val elapsedSec = sumOf { segment ->
        segment.durationSec.coerceAtLeast(0).toDouble() *
            segment.progress.clampedProgress().toDouble()
    }
    return (elapsedSec / totalDurationSec.toDouble()).toFloat().clampedProgress()
}

private fun centerActionLabel(screenState: TimedWorkoutSessionScreenState): String {
    return when {
        screenState.canResume -> "继续训练"
        screenState.canPause -> "暂停训练"
        else -> "当前不可切换"
    }
}

internal fun TimerDialUiState.accessibilityDescription(): String {
    return buildString {
        append(currentStageLabel)
        append("，剩余 ")
        append(currentStageTimeText)
        append("，总剩余 ")
        append(totalRemainingText)
        append(if (isPaused) "，已暂停，" else "，进行中，")
        append(centerActionLabel)
    }
}

private fun TimedSessionStep.timerDialStageType(): TimerDialStageType {
    if (kind == TimedSessionStepKind.REST) {
        return TimerDialStageType.REST
    }

    return when (stageType) {
        TimedStageType.WARMUP -> TimerDialStageType.WARMUP
        TimedStageType.WORK -> TimerDialStageType.WORK
        TimedStageType.REST -> TimerDialStageType.REST
        TimedStageType.COOLDOWN -> TimerDialStageType.COOLDOWN
        TimedStageType.CUSTOM -> TimerDialStageType.CUSTOM
        null -> TimerDialStageType.WORK
    }
}

private fun TimedWorkoutEngineState.currentTimerDialCycleSegments(
    currentStepProgress: Float
): List<TimerDialStageSegmentUiState> {
    if (currentStepIndex !in steps.indices) {
        return emptyList()
    }

    val indexes = currentCycleIndexes().ifEmpty { listOf(currentStepIndex) }
    return indexes.mapNotNull { index ->
        val step = steps.getOrNull(index) ?: return@mapNotNull null
        val progress = when {
            status == SessionStatus.COMPLETED || index < currentStepIndex -> 1f
            index == currentStepIndex -> currentStepProgress
            else -> 0f
        }
        TimerDialStageSegmentUiState(
            id = step.id,
            label = step.title,
            stageType = step.timerDialStageType(),
            durationSec = step.durationSec,
            progress = progress,
            isCurrent = index == currentStepIndex,
            colorHex = normalizeStageColorHex(step.colorHex, step.timerDialStageType().toTimedStageType())
        )
    }
}

private fun TimerDialStageType.toTimedStageType(): TimedStageType {
    return when (this) {
        TimerDialStageType.WARMUP -> TimedStageType.WARMUP
        TimerDialStageType.WORK -> TimedStageType.WORK
        TimerDialStageType.REST -> TimedStageType.REST
        TimerDialStageType.COOLDOWN -> TimedStageType.COOLDOWN
        TimerDialStageType.CUSTOM -> TimedStageType.CUSTOM
    }
}

private fun isValidColorHex(hex: String?): Boolean {
    val value = hex?.trim() ?: return false
    return Regex("#[0-9A-Fa-f]{6}").matches(value)
}

private fun TimedWorkoutEngineState.currentStepDisplayProgress(): Float {
    val step = currentStep ?: return when (status) {
        SessionStatus.COMPLETED -> 1f
        else -> 0f
    }
    if (step.durationSec <= 0) {
        return 0f
    }

    val startedAtElapsedSec = stepHistory
        .lastOrNull { record -> record.stepId == step.id }
        ?.startedAtElapsedSec
        ?: activeElapsedSec
    val extensions = restExtensionHistory
        .filter { extension ->
            extension.stepId == step.id &&
                extension.elapsedSec >= startedAtElapsedSec &&
                extension.elapsedSec <= activeElapsedSec
        }
        .sortedBy { extension -> extension.elapsedSec }

    var progressFloor = 0f
    var segmentStartElapsedSec = startedAtElapsedSec
    var segmentRemainingSec = step.durationSec.toFloat()

    extensions.forEach { extension ->
        val elapsedInSegment = (extension.elapsedSec - segmentStartElapsedSec)
            .coerceAtLeast(0)
            .toFloat()
            .coerceAtMost(segmentRemainingSec)
        val segmentProgress = elapsedInSegment.safeProgressOf(segmentRemainingSec)
        progressFloor += (1f - progressFloor) * segmentProgress

        segmentStartElapsedSec = extension.elapsedSec
        segmentRemainingSec = (segmentRemainingSec - elapsedInSegment)
            .coerceAtLeast(0f) + extension.addedSec.coerceAtLeast(0)
    }

    val elapsedInCurrentSegment = (activeElapsedSec - segmentStartElapsedSec)
        .coerceAtLeast(0)
        .toFloat()
        .coerceAtMost(segmentRemainingSec)
    val currentSegmentProgress = elapsedInCurrentSegment.safeProgressOf(segmentRemainingSec)

    return (progressFloor + (1f - progressFloor) * currentSegmentProgress).clampedProgress()
}

private fun TimedWorkoutEngineState.currentCycleIndexes(): List<Int> {
    val current = steps.getOrNull(currentStepIndex) ?: return emptyList()
    return when (current.kind) {
        TimedSessionStepKind.WORK -> {
            val nextRestIndex = (currentStepIndex + 1).takeIf { index ->
                val next = steps.getOrNull(index)
                next?.kind == TimedSessionStepKind.REST &&
                    next.blockId == current.blockId &&
                    next.round == current.round
            }
            listOfNotNull(currentStepIndex, nextRestIndex)
        }
        TimedSessionStepKind.REST -> {
            val previousWorkIndex = (currentStepIndex - 1).takeIf { index ->
                val previous = steps.getOrNull(index)
                previous?.kind == TimedSessionStepKind.WORK &&
                    previous.blockId == current.blockId &&
                    previous.round == current.round
            }
            listOfNotNull(previousWorkIndex, currentStepIndex)
        }
    }
}

private data class TimerDialWorkoutCycle(
    val workIndex: Int,
    val restIndex: Int?,
    val durationSec: Int
) {
    val endIndex: Int
        get() = restIndex ?: workIndex
}

private fun TimedWorkoutEngineState.timerDialWorkoutCycles(): List<TimerDialWorkoutCycle> {
    return steps.mapIndexedNotNull { index, step ->
        if (!step.isWorkoutStageForTotalMarker()) {
            return@mapIndexedNotNull null
        }

        val restIndex = (index + 1).takeIf { candidateIndex ->
            val next = steps.getOrNull(candidateIndex)
            next?.kind == TimedSessionStepKind.REST &&
                next.blockId == step.blockId &&
                next.round == step.round
        }
        val restDurationSec = restIndex?.let { steps[it].durationSec } ?: 0
        TimerDialWorkoutCycle(
            workIndex = index,
            restIndex = restIndex,
            durationSec = (step.durationSec + restDurationSec).coerceAtLeast(1)
        )
    }
}

private fun TimedSessionStep.isWorkoutStageForTotalMarker(): Boolean {
    if (kind != TimedSessionStepKind.WORK) {
        return false
    }

    return when (timerDialStageType()) {
        TimerDialStageType.WORK,
        TimerDialStageType.CUSTOM -> true
        TimerDialStageType.WARMUP,
        TimerDialStageType.REST,
        TimerDialStageType.COOLDOWN -> false
    }
}

private fun List<TimerDialWorkoutCycle>.completedCount(
    state: TimedWorkoutEngineState
): Int {
    if (isEmpty()) {
        return 0
    }
    if (state.status == SessionStatus.COMPLETED) {
        return size
    }

    return count { cycle -> state.currentStepIndex > cycle.endIndex }
}

private fun List<TimerDialWorkoutCycle>.stageBasedProgress(
    state: TimedWorkoutEngineState,
    currentStepProgress: Float,
    fallbackTotalPlannedDurationSec: Int
): Float {
    if (isEmpty()) {
        return state.activeElapsedSec.toFloat() / fallbackTotalPlannedDurationSec.toFloat()
    }
    if (state.status == SessionStatus.COMPLETED) {
        return 1f
    }

    val currentCycleIndex = indexOfFirst { cycle ->
        state.currentStepIndex in cycle.workIndex..cycle.endIndex
    }
    if (currentCycleIndex < 0) {
        return completedCount(state).toFloat() / size.toFloat()
    }

    val currentCycle = this[currentCycleIndex]
    val elapsedBeforeCurrentStep = state.steps
        .subList(currentCycle.workIndex, state.currentStepIndex.coerceAtLeast(currentCycle.workIndex))
        .sumOf { step -> step.durationSec }
    val currentStepElapsedSec = state.currentStep?.let { step ->
        step.durationSec.toFloat() * currentStepProgress.clampedProgress()
    } ?: 0f
    val cycleProgress = (
        elapsedBeforeCurrentStep.toFloat() + currentStepElapsedSec
        ) / currentCycle.durationSec.toFloat()

    return (currentCycleIndex.toFloat() + cycleProgress.clampedProgress()) / size.toFloat()
}

private fun Float.clampedProgress(): Float {
    return when {
        isNaN() -> 0f
        else -> coerceIn(0f, 1f)
    }
}

private fun Float.safeProgressOf(total: Float): Float {
    if (total <= 0f) {
        return 1f
    }
    return (this / total).clampedProgress()
}

internal val TimerDialSmoothProgressMaxMillis = TrainFlowMotionTokens.ContinuousProjectionMaxDurationMillis

internal data class TimerDialSmoothProgressIdentity(
    val currentSegmentId: String?,
    val isPaused: Boolean,
    val canTogglePause: Boolean,
    val isProjectable: Boolean,
    val segmentStructureSignature: String
)

internal data class TimerDialSmoothProgressAnchor(
    val totalProgress: Float,
    val currentStageProgress: Float,
    val totalRemainingSec: Int,
    val currentStageRemainingSec: Int,
    val segmentProgressSignature: String
)

internal data class TimerDialDisplayedProgress(
    val totalProgress: Float,
    val currentStageProgress: Float
)

internal fun TimerDialUiState.smoothProgressIdentity(): TimerDialSmoothProgressIdentity {
    return TimerDialSmoothProgressIdentity(
        currentSegmentId = stageSegments.firstOrNull { segment -> segment.isCurrent }?.id,
        isPaused = isPaused,
        canTogglePause = canTogglePause,
        isProjectable = !isPaused && canTogglePause && currentStageRemainingSec > 0,
        segmentStructureSignature = stageSegments.joinToString(separator = "|") { segment ->
            "${segment.id}:${segment.stageType}:${segment.durationSec}:${segment.isCurrent}"
        }
    )
}

internal fun TimerDialUiState.smoothProgressAnchor(): TimerDialSmoothProgressAnchor {
    return TimerDialSmoothProgressAnchor(
        totalProgress = totalProgress,
        currentStageProgress = currentStageProgress,
        totalRemainingSec = totalRemainingSec,
        currentStageRemainingSec = currentStageRemainingSec,
        segmentProgressSignature = stageSegments.joinToString(separator = "|") { segment ->
            "${segment.id}:${segment.progress}:${segment.isCurrent}"
        }
    )
}

internal fun TimerDialUiState.canProjectSmoothProgress(
    reduceMotion: Boolean = false
): Boolean {
    return !reduceMotion && !isPaused && canTogglePause && currentStageRemainingSec > 0
}

internal fun timerDialSmoothProgressElapsedMillis(
    frameNanos: Long,
    anchorNanos: Long,
    anchorApplied: Boolean
): Long {
    if (!anchorApplied) {
        return 0L
    }

    return ((frameNanos - anchorNanos).coerceAtLeast(0L) / 1_000_000L)
        .coerceAtMost(TimerDialSmoothProgressMaxMillis)
}

internal fun TimerDialUiState.monotonicDisplayedProgress(
    elapsedMillis: Long,
    reduceMotion: Boolean = false,
    previousDisplayed: TimerDialDisplayedProgress? = null
): TimerDialDisplayedProgress {
    val anchor = TimerDialDisplayedProgress(
        totalProgress = totalProgress.clampedProgress(),
        currentStageProgress = currentStageProgress.clampedProgress()
    )
    val projected = TimerDialDisplayedProgress(
        totalProgress = projectedTotalProgress(
            elapsedMillis = elapsedMillis,
            reduceMotion = reduceMotion
        ),
        currentStageProgress = projectedStageProgress(
            elapsedMillis = elapsedMillis,
            reduceMotion = reduceMotion
        )
    )

    if (!canProjectSmoothProgress(reduceMotion) || previousDisplayed == null) {
        return projected
    }

    return TimerDialDisplayedProgress(
        totalProgress = catchUpDisplayedProgress(
            previousProgress = previousDisplayed.totalProgress,
            anchorProgress = anchor.totalProgress,
            projectedProgress = projected.totalProgress
        ),
        currentStageProgress = catchUpDisplayedProgress(
            previousProgress = previousDisplayed.currentStageProgress,
            anchorProgress = anchor.currentStageProgress,
            projectedProgress = projected.currentStageProgress
        )
    )
}

private fun catchUpDisplayedProgress(
    previousProgress: Float,
    anchorProgress: Float,
    projectedProgress: Float
): Float {
    val previous = previousProgress.clampedProgress()
    val anchor = anchorProgress.clampedProgress()
    val projected = projectedProgress.clampedProgress()

    if (previous >= anchor) {
        return maxOf(previous, projected)
    }

    val projectedDelta = (projected - anchor).coerceAtLeast(0f)
    return (previous + projectedDelta)
        .clampedProgress()
        .coerceAtLeast(previous)
        .coerceAtMost(projected)
}

internal fun TimerDialUiState.projectedStageProgress(
    elapsedMillis: Long,
    reduceMotion: Boolean = false
): Float {
    return projectTimerDialProgress(
        baseProgress = currentStageProgress,
        remainingSec = currentStageRemainingSec,
        elapsedMillis = elapsedMillis,
        isRunning = canProjectSmoothProgress(reduceMotion)
    )
}

internal fun TimerDialUiState.projectedTotalProgress(
    elapsedMillis: Long,
    reduceMotion: Boolean = false
): Float {
    if (!canProjectSmoothProgress(reduceMotion)) {
        return totalProgress.clampedProgress()
    }

    if (totalWorkoutStageCount <= 0) {
        return projectTimerDialProgress(
            baseProgress = totalProgress,
            remainingSec = totalRemainingSec,
            elapsedMillis = elapsedMillis,
            isRunning = true
        )
    }

    if (!stageSegments.any { segment ->
            segment.stageType == TimerDialStageType.WORK || segment.stageType == TimerDialStageType.CUSTOM
        }
    ) {
        return totalProgress.clampedProgress()
    }

    val cycleProgress = stageSegments.cycleProgress()
    val cycleRemainingSec = stageSegments.cycleRemainingSec(currentStageRemainingSec)
    val projectedCycleProgress = projectTimerDialProgress(
        baseProgress = cycleProgress,
        remainingSec = cycleRemainingSec,
        elapsedMillis = elapsedMillis,
        isRunning = true
    )
    val projectedTotalProgress = (
        completedWorkoutStageCount.coerceAtLeast(0).toFloat() + projectedCycleProgress
        ) / totalWorkoutStageCount.toFloat()

    return projectedTotalProgress.clampedProgress()
        .coerceAtLeast(totalProgress.clampedProgress())
}

internal fun projectTimerDialProgress(
    baseProgress: Float,
    remainingSec: Int,
    elapsedMillis: Long,
    isRunning: Boolean
): Float {
    val safeBaseProgress = baseProgress.clampedProgress()
    if (!isRunning || remainingSec <= 0 || elapsedMillis <= 0L) {
        return safeBaseProgress
    }

    val elapsedSec = elapsedMillis.coerceIn(0L, TimerDialSmoothProgressMaxMillis).toFloat() / 1_000f
    val progressPerSecond = (1f - safeBaseProgress) / remainingSec.toFloat()
    return (safeBaseProgress + progressPerSecond * elapsedSec).clampedProgress()
}

private fun List<TimerDialStageSegmentUiState>.cycleProgress(): Float {
    val totalDurationSec = sumOf { segment -> segment.durationSec.coerceAtLeast(0) }
    if (totalDurationSec <= 0) {
        return 0f
    }

    val elapsedSec = sumOf { segment ->
        segment.durationSec.coerceAtLeast(0).toDouble() *
            segment.progress.clampedProgress().toDouble()
    }
    return (elapsedSec / totalDurationSec.toDouble()).toFloat().clampedProgress()
}

private fun List<TimerDialStageSegmentUiState>.cycleRemainingSec(currentStageRemainingSec: Int): Int {
    val remainingSec = sumOf { segment ->
        when {
            segment.isCurrent -> currentStageRemainingSec.coerceAtLeast(0)
            segment.progress <= 0f -> segment.durationSec.coerceAtLeast(0)
            else -> 0
        }
    }
    return remainingSec.coerceAtLeast(1)
}

private fun Int.formatTimerText(): String {
    val safeSeconds = coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
