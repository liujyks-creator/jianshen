package com.liujyks.trainflow.core.model

data class TimedCompositionTimeline(
    val compositionVersion: Int,
    val compositionBlockId: String,
    val steps: List<TimedCompositionTimelineStep>
) {
    val stageInstanceCount: Int
        get() = steps.maxOfOrNull { step -> step.stageInstanceIndex } ?: 0
}

data class TimedCompositionTimelineStep(
    val id: String,
    val stepKind: TimedCompositionTimelineStepKind,
    val compositionVersion: Int,
    val compositionBlockId: String,
    val timelineStageId: String,
    val timelineStageKind: TimedCompositionTimelineStageKind,
    val stageInstanceIndex: Int,
    val targetInstanceIndex: Int,
    val stageGroupId: String,
    val targetId: String,
    val targetKind: TimedCompositionTimelineTargetKind,
    val roundIndex: Int?,
    val stageGroupIndex: Int?,
    val targetIndex: Int,
    val plannedDurationSec: Int,
    val displayName: String,
    val colorHex: String,
    val iconKey: String? = null,
    val cueSettings: CueSettings? = null
) {
    val isWork: Boolean
        get() = stepKind == TimedCompositionTimelineStepKind.WORK

    val isRest: Boolean
        get() = stepKind == TimedCompositionTimelineStepKind.REST

    val isWarmup: Boolean
        get() = timelineStageKind == TimedCompositionTimelineStageKind.WARMUP

    val isCooldown: Boolean
        get() = timelineStageKind == TimedCompositionTimelineStageKind.COOLDOWN

    val isBetweenRoundRest: Boolean
        get() = timelineStageKind == TimedCompositionTimelineStageKind.BETWEEN_ROUND_REST
}

enum class TimedCompositionTimelineStageKind(val contractValue: String) {
    WARMUP("warmup"),
    STAGE_GROUP("stage_group"),
    BETWEEN_ROUND_REST("between_round_rest"),
    COOLDOWN("cooldown")
}

enum class TimedCompositionTimelineStepKind(val contractValue: String) {
    WORK("work"),
    REST("rest")
}

enum class TimedCompositionTimelineTargetKind(val contractValue: String) {
    ACTION("action"),
    REST("rest"),
    CUSTOM("custom"),
    WARMUP("warmup"),
    COOLDOWN("cooldown"),
    BETWEEN_ROUND_REST("between_round_rest")
}

object TimedCompositionTimelineAdapter {
    fun expand(block: TimedCompositionBlock): TimedCompositionTimeline {
        require(block.compositionVersion == TIMED_COMPOSITION_CURRENT_VERSION) {
            "Unsupported timed composition version: ${block.compositionVersion}"
        }

        val normalizedBlock = block.normalized()
        val builder = TimelineBuilder(normalizedBlock)

        if (normalizedBlock.warmupSec > 0) {
            val warmupStyle = normalizedBlock.warmupStyle
            builder.addBoundaryStep(
                stageKind = TimedCompositionTimelineStageKind.WARMUP,
                targetKind = TimedCompositionTimelineTargetKind.WARMUP,
                plannedDurationSec = normalizedBlock.warmupSec,
                displayName = TimedStageType.WARMUP.displayName,
                colorHex = warmupStyle?.colorHex ?: TimedStageType.WARMUP.defaultColorHex,
                iconKey = warmupStyle?.iconKey ?: TimedStageType.WARMUP.defaultIconKey
            )
        }

        for (round in 1..normalizedBlock.rounds) {
            normalizedBlock.stageGroups.forEachIndexed { stageGroupIndex, group ->
                builder.addStageGroupSteps(
                    roundIndex = round,
                    stageGroupIndex = stageGroupIndex + 1,
                    group = group
                )
            }

            if (round < normalizedBlock.rounds && normalizedBlock.restBetweenRoundsSec > 0) {
                builder.addBetweenRoundRest(roundIndex = round)
            }
        }

        if (normalizedBlock.cooldownSec > 0) {
            val cooldownStyle = normalizedBlock.cooldownStyle
            builder.addBoundaryStep(
                stageKind = TimedCompositionTimelineStageKind.COOLDOWN,
                targetKind = TimedCompositionTimelineTargetKind.COOLDOWN,
                plannedDurationSec = normalizedBlock.cooldownSec,
                displayName = TimedStageType.COOLDOWN.displayName,
                colorHex = cooldownStyle?.colorHex ?: TimedStageType.COOLDOWN.defaultColorHex,
                iconKey = cooldownStyle?.iconKey ?: TimedStageType.COOLDOWN.defaultIconKey
            )
        }

        return TimedCompositionTimeline(
            compositionVersion = normalizedBlock.compositionVersion,
            compositionBlockId = normalizedBlock.id,
            steps = builder.steps
        )
    }
}

private class TimelineBuilder(
    private val block: TimedCompositionBlock
) {
    private val mutableSteps = mutableListOf<TimedCompositionTimelineStep>()
    private var nextStageInstanceIndex = 1
    private var nextTargetInstanceIndex = 1

    val steps: List<TimedCompositionTimelineStep>
        get() = mutableSteps.toList()

    fun addBoundaryStep(
        stageKind: TimedCompositionTimelineStageKind,
        targetKind: TimedCompositionTimelineTargetKind,
        plannedDurationSec: Int,
        displayName: String,
        colorHex: String,
        iconKey: String?
    ) {
        val stageInstanceIndex = nextStageInstanceIndex++
        val timelineStageId = "${block.id}:${stageKind.contractValue}"
        val targetId = "$timelineStageId:target"
        mutableSteps += TimedCompositionTimelineStep(
            id = "$timelineStageId:t1",
            stepKind = TimedCompositionTimelineStepKind.WORK,
            compositionVersion = block.compositionVersion,
            compositionBlockId = block.id,
            timelineStageId = timelineStageId,
            timelineStageKind = stageKind,
            stageInstanceIndex = stageInstanceIndex,
            targetInstanceIndex = nextTargetInstanceIndex++,
            stageGroupId = timelineStageId,
            targetId = targetId,
            targetKind = targetKind,
            roundIndex = null,
            stageGroupIndex = null,
            targetIndex = 1,
            plannedDurationSec = plannedDurationSec,
            displayName = displayName,
            colorHex = normalizeStageColorHex(colorHex, targetKind.toTimedStageType()),
            iconKey = iconKey
        )
    }

    fun addStageGroupSteps(
        roundIndex: Int,
        stageGroupIndex: Int,
        group: TimedCompositionStageGroup
    ) {
        val stageInstanceIndex = nextStageInstanceIndex++
        val timelineStageId = "${block.id}:r$roundIndex:g$stageGroupIndex:${group.id}"
        group.targets.forEachIndexed { targetIndex, target ->
            val targetKind = target.kind.toTimelineTargetKind()
            mutableSteps += TimedCompositionTimelineStep(
                id = "$timelineStageId:t${targetIndex + 1}:${target.id}",
                stepKind = targetKind.toStepKind(),
                compositionVersion = block.compositionVersion,
                compositionBlockId = block.id,
                timelineStageId = timelineStageId,
                timelineStageKind = TimedCompositionTimelineStageKind.STAGE_GROUP,
                stageInstanceIndex = stageInstanceIndex,
                targetInstanceIndex = nextTargetInstanceIndex++,
                stageGroupId = group.id,
                targetId = target.id,
                targetKind = targetKind,
                roundIndex = roundIndex,
                stageGroupIndex = stageGroupIndex,
                targetIndex = targetIndex + 1,
                plannedDurationSec = target.durationSec,
                displayName = target.name,
                colorHex = normalizeStageColorHex(target.colorHex, targetKind.toTimedStageType()),
                iconKey = target.iconKey ?: targetKind.toTimedStageType().defaultIconKey,
                cueSettings = target.cueSettings ?: group.cueSettings
            )
        }
    }

    fun addBetweenRoundRest(roundIndex: Int) {
        val stageInstanceIndex = nextStageInstanceIndex++
        val timelineStageId = "${block.id}:r$roundIndex:between-round-rest"
        val targetId = "$timelineStageId:target"
        val restStyle = block.restBetweenRoundsStyle
        mutableSteps += TimedCompositionTimelineStep(
            id = "$timelineStageId:t1",
            stepKind = TimedCompositionTimelineStepKind.REST,
            compositionVersion = block.compositionVersion,
            compositionBlockId = block.id,
            timelineStageId = timelineStageId,
            timelineStageKind = TimedCompositionTimelineStageKind.BETWEEN_ROUND_REST,
            stageInstanceIndex = stageInstanceIndex,
            targetInstanceIndex = nextTargetInstanceIndex++,
            stageGroupId = timelineStageId,
            targetId = targetId,
            targetKind = TimedCompositionTimelineTargetKind.BETWEEN_ROUND_REST,
            roundIndex = roundIndex,
            stageGroupIndex = null,
            targetIndex = 1,
            plannedDurationSec = block.restBetweenRoundsSec,
            displayName = TimedStageType.REST.displayName,
            colorHex = restStyle?.colorHex ?: TimedStageType.REST.defaultColorHex,
            iconKey = restStyle?.iconKey ?: TimedStageIconKey.RECOVER_BREATHE.contractValue
        )
    }
}

private fun TimedCompositionTargetKind.toTimelineTargetKind(): TimedCompositionTimelineTargetKind {
    return when (this) {
        TimedCompositionTargetKind.ACTION -> TimedCompositionTimelineTargetKind.ACTION
        TimedCompositionTargetKind.REST -> TimedCompositionTimelineTargetKind.REST
        TimedCompositionTargetKind.CUSTOM -> TimedCompositionTimelineTargetKind.CUSTOM
    }
}

private fun TimedCompositionTimelineTargetKind.toStepKind(): TimedCompositionTimelineStepKind {
    return when (this) {
        TimedCompositionTimelineTargetKind.REST,
        TimedCompositionTimelineTargetKind.BETWEEN_ROUND_REST -> TimedCompositionTimelineStepKind.REST
        TimedCompositionTimelineTargetKind.ACTION,
        TimedCompositionTimelineTargetKind.CUSTOM,
        TimedCompositionTimelineTargetKind.WARMUP,
        TimedCompositionTimelineTargetKind.COOLDOWN -> TimedCompositionTimelineStepKind.WORK
    }
}

private fun TimedCompositionTimelineTargetKind.toTimedStageType(): TimedStageType {
    return when (this) {
        TimedCompositionTimelineTargetKind.ACTION -> TimedStageType.WORK
        TimedCompositionTimelineTargetKind.REST -> TimedStageType.REST
        TimedCompositionTimelineTargetKind.CUSTOM -> TimedStageType.CUSTOM
        TimedCompositionTimelineTargetKind.WARMUP -> TimedStageType.WARMUP
        TimedCompositionTimelineTargetKind.COOLDOWN -> TimedStageType.COOLDOWN
        TimedCompositionTimelineTargetKind.BETWEEN_ROUND_REST -> TimedStageType.REST
    }
}
