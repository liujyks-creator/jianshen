package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionTimelineAdapter
import com.liujyks.trainflow.core.model.WorkoutPlan

internal fun WorkoutPlan.hasStartableTimedCompositionPayload(): Boolean {
    val compositionBlocks = blocks.filterIsInstance<TimedCompositionBlock>()
    return compositionBlocks.isNotEmpty() &&
        compositionBlocks.all { block -> block.hasExecutableTimedCompositionTimeline() }
}

private fun TimedCompositionBlock.hasExecutableTimedCompositionTimeline(): Boolean {
    return runCatching {
        TimedCompositionTimelineAdapter.expand(this).steps.isNotEmpty()
    }.getOrDefault(false)
}
