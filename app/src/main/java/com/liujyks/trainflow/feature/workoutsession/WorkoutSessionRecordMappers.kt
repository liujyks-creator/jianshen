package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthSessionStepHistoryStatus
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngineState
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngineResult
import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1
import com.liujyks.trainflow.core.data.RecorderPhaseInput
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.engine.TimedSessionStepHistoryStatus
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.model.SessionStepRecord
import com.liujyks.trainflow.core.model.TimedRestExtensionRecord
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutSession
import java.time.Duration
import java.time.Instant
import org.json.JSONObject

internal data class StrengthTransitionFacts(
    val phaseStarts: List<RecorderPhaseInput>,
    val terminalStatus: SessionStatus?
)

internal fun freeFollowAlongSnapshot(): WorkoutPlanSnapshot = WorkoutPlanSnapshot(
    planId = null,
    title = "跟练",
    mode = WorkoutMode.FOLLOW_ALONG,
    blocks = emptyList(),
    preferences = null,
    followAlong = null
)

internal fun freeFollowAlongPhase(snapshot: PreparedPlanSnapshotStorageV1): RecorderPhaseInput {
    val payload = JSONObject()
        .put("variant", "free_session")
        .put("blockId", JSONObject.NULL)
        .put("stepIndex0", JSONObject.NULL)
        .put("followAlongStepKind", JSONObject.NULL)
        .put("itemId", JSONObject.NULL)
        .put("exerciseId", JSONObject.NULL)
        .put("roundIndex0", JSONObject.NULL)
    val identity = JSONObject()
        .put("phaseIdentityContractVersion", 1)
        .put("family", "follow_along_v1")
        .put("payloadVersion", 1)
        .put("mode", "follow_along")
        .put("phaseKind", "follow_along_action")
        .put("orderedStructureSignature", JSONObject()
            .put("signatureContractVersion", 1)
            .put("algorithm", "sha256")
            .put("digestHexLowercase", snapshot.orderedStructureDigestHexLowercase()))
        .put("payload", payload)
    return RecorderPhaseInput("follow_along_action", identity.toString())
}

internal fun freeFollowAlongElapsedSeconds(startedAt: Instant, endedAt: Instant): Int =
    totalElapsedSec(startedAt, endedAt, 0, 0)

internal fun strengthTransitionFacts(
    snapshot: PreparedPlanSnapshotStorageV1,
    before: StrengthWorkoutEngineState,
    result: StrengthWorkoutEngineResult
): StrengthTransitionFacts {
    val after = result.state
    val terminal = after.status.takeIf { after.isTerminal && !before.isTerminal }
    if (after.isTerminal || after.status == SessionStatus.READY ||
        (before.status == after.status && before.currentSetIndex == after.currentSetIndex &&
            before.currentStepKind == after.currentStepKind)) {
        return StrengthTransitionFacts(emptyList(), terminal)
    }
    val paused = after.status == SessionStatus.PAUSED
    val kind = if (paused) "paused" else requireNotNull(after.currentStepKind).contractValue
    val variant = if (paused) "paused" else when (after.currentStepKind) {
        SessionStepKind.STRENGTH_PREPARE_SET -> "prepare_set"
        SessionStepKind.STRENGTH_ACTIVE_SET -> "active_set"
        SessionStepKind.STRENGTH_CONFIRM_SET -> "confirm_set"
        SessionStepKind.STRENGTH_REST -> "rest"
        else -> error("Not a strength phase: ${after.currentStepKind}")
    }
    val payload = JSONObject().put("variant", variant)
    if (paused) {
        payload.put("blockId", JSONObject.NULL).put("setPlanId", JSONObject.NULL)
            .put("plannedExerciseId", JSONObject.NULL).put("actualExerciseId", JSONObject.NULL)
            .put("exerciseSetIndex0", JSONObject.NULL).put("globalSetIndex0", JSONObject.NULL)
            .put("setKind", JSONObject.NULL).put("substitutedFromExerciseId", JSONObject.NULL)
    } else {
        val set = requireNotNull(after.currentSet)
        payload.put("blockId", set.blockId).put("setPlanId", set.setPlanId)
            .put("plannedExerciseId", set.exerciseId).put("actualExerciseId", set.exerciseId)
            .put("exerciseSetIndex0", set.exerciseSetIndex).put("globalSetIndex0", set.globalSetIndex)
            .put("setKind", set.setKind.contractValue).put("substitutedFromExerciseId", JSONObject.NULL)
    }
    val identity = JSONObject().put("phaseIdentityContractVersion", 1)
        .put("family", "strength_v1").put("payloadVersion", 1).put("mode", "strength")
        .put("phaseKind", kind).put("orderedStructureSignature", JSONObject()
            .put("signatureContractVersion", 1).put("algorithm", "sha256")
            .put("digestHexLowercase", snapshot.orderedStructureDigestHexLowercase()))
        .put("payload", payload).toString()
    return StrengthTransitionFacts(listOf(RecorderPhaseInput(kind, identity)), terminal)
}

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
        stepHistory = toTimedSessionStepRecords(startedAt),
        timedRestExtensionRecords = toTimedRestExtensionRecords()
    )
}

internal fun TimedWorkoutEngineState.toTimedSessionStepRecords(
    startedAt: Instant
): List<SessionStepRecord> {
    return stepHistory.mapNotNull { record ->
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
}

internal fun TimedWorkoutEngineState.toTimedRestExtensionRecords(): List<TimedRestExtensionRecord> {
    return restExtensionHistory.mapIndexed { index, record ->
        TimedRestExtensionRecord(
            id = "timed-rest-extension-${index + 1}",
            stepId = record.stepId,
            stepIndex = record.stepIndex,
            roundIndex = record.roundIndex,
            restStageId = record.restStageId,
            restStageTitle = record.title,
            previousStageId = record.previousStageId,
            previousStageTitle = record.previousStageTitle,
            addedSec = record.addedSec,
            plannedRestSec = record.plannedRestSec,
            restElapsedBeforeExtensionSec = record.restElapsedBeforeExtensionSec,
            extensionAtRemainingSec = record.extensionAtRemainingSec,
            cumulativeExtraRestSec = record.cumulativeAddedSec,
            eventElapsedSec = record.elapsedSec
        )
    }
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
