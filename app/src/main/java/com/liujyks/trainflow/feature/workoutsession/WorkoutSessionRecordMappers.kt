package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthSessionStepHistoryStatus
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngineState
import com.liujyks.trainflow.core.engine.TimedSessionStepHistoryStatus
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.model.SessionStepRecord
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.WorkoutSession
import java.time.Duration
import java.time.Instant

internal fun TimedWorkoutEngineState.toWorkoutSessionRecord(
    plan: WorkoutPlan,
    startedAt: Instant,
    endedAt: Instant
): WorkoutSession {
    return WorkoutSession(
        id = sessionId,
        planId = plan.id,
        mode = plan.mode,
        planSnapshot = plan.toSnapshot(),
        status = status,
        startedAt = startedAt.toString(),
        endedAt = endedAt.toString(),
        totalElapsedSec = totalElapsedSec(
            startedAt = startedAt,
            endedAt = endedAt,
            effectiveElapsedSec = activeElapsedSec,
            pausedElapsedSec = pausedElapsedSec
        ),
        effectiveElapsedSec = activeElapsedSec,
        pausedElapsedSec = pausedElapsedSec,
        currentStep = currentSessionStep,
        stepHistory = stepHistory.mapNotNull { record ->
            val duration = record.actualDurationSec ?: return@mapNotNull null
            SessionStepRecord(
                stepId = record.stepId,
                kind = record.kind,
                startedAt = startedAt.plusSeconds(record.startedAtElapsedSec.toLong()).toString(),
                endedAt = record.endedAtElapsedSec?.let { endedSec ->
                    startedAt.plusSeconds(endedSec.toLong()).toString()
                },
                skipped = record.status == TimedSessionStepHistoryStatus.SKIPPED,
                actualDurationSec = duration
            )
        }
    )
}

internal fun StrengthWorkoutEngineState.toWorkoutSessionRecord(
    plan: WorkoutPlan,
    startedAt: Instant,
    endedAt: Instant
): WorkoutSession {
    return WorkoutSession(
        id = sessionId,
        planId = plan.id,
        mode = plan.mode,
        planSnapshot = plan.toSnapshot(),
        status = status,
        startedAt = startedAt.toString(),
        endedAt = endedAt.toString(),
        totalElapsedSec = totalElapsedSec(
            startedAt = startedAt,
            endedAt = endedAt,
            effectiveElapsedSec = sessionElapsedSec,
            pausedElapsedSec = pausedElapsedSec
        ),
        effectiveElapsedSec = sessionElapsedSec,
        pausedElapsedSec = pausedElapsedSec,
        currentStep = currentSessionStep,
        stepHistory = stepHistory.mapNotNull { record ->
            val duration = record.actualDurationSec ?: return@mapNotNull null
            SessionStepRecord(
                stepId = record.stepId,
                kind = record.kind,
                startedAt = startedAt.plusSeconds(record.startedAtElapsedSec.toLong()).toString(),
                endedAt = record.endedAtElapsedSec?.let { endedSec ->
                    startedAt.plusSeconds(endedSec.toLong()).toString()
                },
                skipped = record.status == StrengthSessionStepHistoryStatus.SKIPPED,
                actualDurationSec = duration
            )
        },
        strengthSetRecords = strengthSetRecords
    )
}

private fun WorkoutPlan.toSnapshot(): WorkoutPlanSnapshot {
    return WorkoutPlanSnapshot(
        planId = id,
        title = title,
        mode = mode,
        blocks = blocks,
        preferences = preferences,
        followAlong = followAlong
    )
}

private fun totalElapsedSec(
    startedAt: Instant,
    endedAt: Instant,
    effectiveElapsedSec: Int,
    pausedElapsedSec: Int
): Int {
    val wallClockSec = Duration.between(startedAt, endedAt)
        .seconds
        .coerceAtLeast(0)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
    return wallClockSec.coerceAtLeast(effectiveElapsedSec + pausedElapsedSec)
}
