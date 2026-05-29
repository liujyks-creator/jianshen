package com.liujyks.trainflow.core.engine

import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.PlanBlockKind
import com.liujyks.trainflow.core.model.RestBlock
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStep
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutEvent
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot

object TimedWorkoutEngine {
    fun create(
        plan: WorkoutPlan,
        sessionId: String = "session-${plan.id}"
    ): TimedWorkoutEngineState {
        return create(
            planSnapshot = WorkoutPlanSnapshot(
                title = plan.title,
                mode = plan.mode,
                blocks = plan.blocks,
                preferences = plan.preferences,
                followAlong = plan.followAlong
            ),
            sessionId = sessionId
        )
    }

    fun create(
        planSnapshot: WorkoutPlanSnapshot,
        sessionId: String
    ): TimedWorkoutEngineState {
        require(planSnapshot.mode == WorkoutMode.TIMED || planSnapshot.mode == WorkoutMode.FOLLOW_ALONG) {
            "TimedWorkoutEngine only supports timed or follow-along plan snapshots."
        }

        return TimedWorkoutEngineState(
            sessionId = sessionId,
            planTitle = planSnapshot.title,
            steps = planSnapshot.toTimedSteps()
        )
    }

    fun dispatch(
        state: TimedWorkoutEngineState,
        command: WorkoutCommand
    ): TimedWorkoutEngineResult {
        return when (command) {
            WorkoutCommand.StartSession -> start(state)
            WorkoutCommand.PauseSession -> pause(state)
            WorkoutCommand.ResumeSession -> resume(state)
            WorkoutCommand.SkipStep -> skipStep(state)
            is WorkoutCommand.ExtendRest -> extendRest(state, command.seconds)
            is WorkoutCommand.EndSession -> endSession(state)
            is WorkoutCommand.StartStrengthSet,
            is WorkoutCommand.CompleteStrengthSet,
            is WorkoutCommand.ConfirmStrengthSet,
            is WorkoutCommand.ReplaceExercise,
            is WorkoutCommand.UpdateActualWeight,
            is WorkoutCommand.UpdateActualReps -> TimedWorkoutEngineResult(state = state)
        }
    }

    fun tick(
        state: TimedWorkoutEngineState,
        seconds: Int = 1
    ): TimedWorkoutEngineResult {
        if (seconds <= 0 || state.status != SessionStatus.ACTIVE || state.currentStep == null) {
            return TimedWorkoutEngineResult(state = state)
        }

        var workingState = state
        val events = mutableListOf<WorkoutEvent>()

        repeat(seconds) {
            if (workingState.status != SessionStatus.ACTIVE || workingState.currentStep == null) {
                return@repeat
            }

            if (workingState.remainingSec > 1) {
                workingState = workingState.copy(remainingSec = workingState.remainingSec - 1)
                val cueResult = emitEndingCueIfNeeded(workingState)
                workingState = cueResult.state
                events += cueResult.events
            } else {
                val advanceResult = advanceToNextStep(
                    state = workingState.copy(completedStepCount = workingState.completedStepCount + 1)
                )
                workingState = advanceResult.state
                events += advanceResult.events
            }
        }

        return TimedWorkoutEngineResult(state = workingState, events = events)
    }

    private fun start(state: TimedWorkoutEngineState): TimedWorkoutEngineResult {
        if (state.status != SessionStatus.READY) {
            return TimedWorkoutEngineResult(state = state)
        }
        if (state.steps.isEmpty()) {
            return TimedWorkoutEngineResult(
                state = state.copy(status = SessionStatus.COMPLETED),
                events = listOf(WorkoutEvent.SessionCompleted(sessionId = state.sessionId))
            )
        }

        val firstStepState = state.copy(
            status = SessionStatus.ACTIVE,
            currentStepIndex = 0,
            remainingSec = state.steps.first().durationSec
        )
        val startEvents = mutableListOf<WorkoutEvent>(WorkoutEvent.SessionStarted(state.sessionId))
        val stepStartResult = emitStepStarted(firstStepState)
        val cueResult = emitEndingCueIfNeeded(stepStartResult.state)

        startEvents += stepStartResult.events
        startEvents += cueResult.events

        return TimedWorkoutEngineResult(state = cueResult.state, events = startEvents)
    }

    private fun pause(state: TimedWorkoutEngineState): TimedWorkoutEngineResult {
        if (state.status != SessionStatus.ACTIVE) {
            return TimedWorkoutEngineResult(state = state)
        }

        return TimedWorkoutEngineResult(
            state = state.copy(status = SessionStatus.PAUSED),
            events = listOf(WorkoutEvent.SessionPaused(sessionId = state.sessionId))
        )
    }

    private fun resume(state: TimedWorkoutEngineState): TimedWorkoutEngineResult {
        if (state.status != SessionStatus.PAUSED) {
            return TimedWorkoutEngineResult(state = state)
        }

        return TimedWorkoutEngineResult(
            state = state.copy(status = SessionStatus.ACTIVE),
            events = listOf(WorkoutEvent.SessionResumed(sessionId = state.sessionId))
        )
    }

    private fun skipStep(state: TimedWorkoutEngineState): TimedWorkoutEngineResult {
        val currentStep = state.currentStep
        if (state.status != SessionStatus.ACTIVE || currentStep == null) {
            return TimedWorkoutEngineResult(state = state)
        }

        return advanceToNextStep(
            state = state.copy(
                completedStepCount = state.completedStepCount + 1,
                skippedStepIds = state.skippedStepIds + currentStep.id
            )
        )
    }

    private fun extendRest(
        state: TimedWorkoutEngineState,
        seconds: Int
    ): TimedWorkoutEngineResult {
        val currentStep = state.currentStep
        if (
            state.status != SessionStatus.ACTIVE ||
            seconds <= 0 ||
            currentStep == null ||
            currentStep.kind != TimedSessionStepKind.REST
        ) {
            return TimedWorkoutEngineResult(state = state)
        }

        return TimedWorkoutEngineResult(
            state = state.copy(
                remainingSec = state.remainingSec + seconds,
                extendedRestSec = state.extendedRestSec + seconds
            )
        )
    }

    private fun endSession(state: TimedWorkoutEngineState): TimedWorkoutEngineResult {
        if (state.status == SessionStatus.COMPLETED || state.status == SessionStatus.ABANDONED) {
            return TimedWorkoutEngineResult(state = state)
        }

        return TimedWorkoutEngineResult(state = state.copy(status = SessionStatus.ABANDONED))
    }

    private fun advanceToNextStep(state: TimedWorkoutEngineState): TimedWorkoutEngineResult {
        val nextIndex = state.currentStepIndex + 1
        if (nextIndex >= state.steps.size) {
            return TimedWorkoutEngineResult(
                state = state.copy(
                    status = SessionStatus.COMPLETED,
                    currentStepIndex = state.steps.size,
                    remainingSec = 0
                ),
                events = listOf(WorkoutEvent.SessionCompleted(sessionId = state.sessionId))
            )
        }

        val nextStepState = state.copy(
            currentStepIndex = nextIndex,
            remainingSec = state.steps[nextIndex].durationSec
        )
        val stepStartResult = emitStepStarted(nextStepState)
        val cueResult = emitEndingCueIfNeeded(stepStartResult.state)

        return TimedWorkoutEngineResult(
            state = cueResult.state,
            events = stepStartResult.events + cueResult.events
        )
    }

    private fun emitStepStarted(state: TimedWorkoutEngineState): TimedWorkoutEngineResult {
        val step = state.currentStep ?: return TimedWorkoutEngineResult(state = state)
        val event = when (step.kind) {
            TimedSessionStepKind.WORK -> WorkoutEvent.TimedWorkStarted(
                stepId = step.id,
                exerciseId = step.exerciseId
            )
            TimedSessionStepKind.REST -> WorkoutEvent.RestStarted(
                stepId = step.id,
                durationSec = step.durationSec
            )
        }

        return TimedWorkoutEngineResult(state = state, events = listOf(event))
    }

    private fun emitEndingCueIfNeeded(state: TimedWorkoutEngineState): TimedWorkoutEngineResult {
        val step = state.currentStep ?: return TimedWorkoutEngineResult(state = state)
        val thresholdSec = step.endingCueThresholdSec ?: return TimedWorkoutEngineResult(state = state)
        if (state.remainingSec > thresholdSec || state.remainingSec <= 0) {
            return TimedWorkoutEngineResult(state = state)
        }

        val cueKey = "${step.id}:${step.kind}:${state.remainingSec}"
        if (cueKey in state.emittedEndingCueKeys) {
            return TimedWorkoutEngineResult(state = state)
        }

        val event = when (step.kind) {
            TimedSessionStepKind.WORK -> WorkoutEvent.TimedWorkEnding(
                stepId = step.id,
                remainingSec = state.remainingSec
            )
            TimedSessionStepKind.REST -> WorkoutEvent.RestEnding(
                stepId = step.id,
                remainingSec = state.remainingSec
            )
        }

        return TimedWorkoutEngineResult(
            state = state.copy(emittedEndingCueKeys = state.emittedEndingCueKeys + cueKey),
            events = listOf(event)
        )
    }

    private fun WorkoutPlanSnapshot.toTimedSteps(): List<TimedSessionStep> {
        val globalCues = preferences?.cueSettings

        return blocks
            .sortedBy { block -> block.order }
            .flatMap { block -> block.toTimedSteps(globalCues) }
    }

    private fun PlanBlock.toTimedSteps(globalCues: CueSettings?): List<TimedSessionStep> {
        return when (this) {
            is WarmupBlock -> timedBlockSteps(
                blockId = id,
                blockKind = kind,
                durationSec = durationSec,
                items = items,
                globalCues = globalCues
            )
            is StretchBlock -> timedBlockSteps(
                blockId = id,
                blockKind = kind,
                durationSec = durationSec,
                items = items,
                globalCues = globalCues
            )
            is CooldownBlock -> timedBlockSteps(
                blockId = id,
                blockKind = kind,
                durationSec = durationSec,
                items = items,
                globalCues = globalCues
            )
            is RestBlock -> listOfNotNull(
                restStep(
                    id = "$id-rest",
                    blockId = id,
                    durationSec = durationSec,
                    cue = globalCues?.restEnding
                )
            )
            is TimedCircuitBlock -> circuitSteps(globalCues)
            else -> emptyList()
        }
    }

    private fun timedBlockSteps(
        blockId: String,
        blockKind: PlanBlockKind,
        durationSec: Int?,
        items: List<TimedExerciseItem>,
        globalCues: CueSettings?
    ): List<TimedSessionStep> {
        if (items.isNotEmpty()) {
            return items.flatMap { item ->
                item.toWorkAndRestSteps(
                    idPrefix = "$blockId-${item.id}",
                    blockId = blockId,
                    blockKind = blockKind,
                    round = null,
                    roundCount = null,
                    globalCues = globalCues
                )
            }
        }

        val duration = durationSec ?: return emptyList()
        return listOfNotNull(
            workStep(
                id = "$blockId-work",
                blockId = blockId,
                blockKind = blockKind,
                item = null,
                durationSec = duration,
                round = null,
                roundCount = null,
                cue = globalCues?.actionEnding
            )
        )
    }

    private fun TimedCircuitBlock.circuitSteps(globalCues: CueSettings?): List<TimedSessionStep> {
        if (rounds <= 0 || items.isEmpty()) {
            return emptyList()
        }

        return (1..rounds).flatMap { round ->
            val itemSteps = items.flatMap { item ->
                item.toWorkAndRestSteps(
                    idPrefix = "$id-r$round-${item.id}",
                    blockId = id,
                    blockKind = kind,
                    round = round,
                    roundCount = rounds,
                    globalCues = globalCues
                )
            }
            val roundRest = if (round < rounds) {
                listOfNotNull(
                    restStep(
                        id = "$id-r$round-round-rest",
                        blockId = id,
                        durationSec = restBetweenRoundsSec,
                        round = round,
                        roundCount = rounds,
                        cue = globalCues?.restEnding
                    )
                )
            } else {
                emptyList()
            }

            itemSteps + roundRest
        }
    }

    private fun TimedExerciseItem.toWorkAndRestSteps(
        idPrefix: String,
        blockId: String,
        blockKind: PlanBlockKind,
        round: Int?,
        roundCount: Int?,
        globalCues: CueSettings?
    ): List<TimedSessionStep> {
        return listOfNotNull(
            workStep(
                id = "$idPrefix-work",
                blockId = blockId,
                blockKind = blockKind,
                item = this,
                durationSec = workDurationSec,
                round = round,
                roundCount = roundCount,
                cue = cueSettings?.actionEnding ?: globalCues?.actionEnding
            ),
            restStep(
                id = "$idPrefix-rest",
                blockId = blockId,
                itemId = id,
                durationSec = restAfterSec,
                round = round,
                roundCount = roundCount,
                cue = cueSettings?.restEnding ?: globalCues?.restEnding
            )
        )
    }

    private fun workStep(
        id: String,
        blockId: String,
        blockKind: PlanBlockKind,
        item: TimedExerciseItem?,
        durationSec: Int,
        round: Int?,
        roundCount: Int?,
        cue: CountdownCue?
    ): TimedSessionStep? {
        if (durationSec <= 0) {
            return null
        }

        return TimedSessionStep(
            id = id,
            kind = TimedSessionStepKind.WORK,
            sessionStepKind = if (blockKind == PlanBlockKind.STRETCH) {
                SessionStepKind.STRETCH
            } else {
                SessionStepKind.TIMED_WORK
            },
            blockId = blockId,
            itemId = item?.id,
            exerciseId = item?.exerciseId,
            durationSec = durationSec,
            round = round,
            roundCount = roundCount,
            endingCueThresholdSec = cue.effectiveThresholdSec(durationSec)
        )
    }

    private fun restStep(
        id: String,
        blockId: String,
        itemId: String? = null,
        durationSec: Int?,
        round: Int? = null,
        roundCount: Int? = null,
        cue: CountdownCue?
    ): TimedSessionStep? {
        val duration = durationSec ?: return null
        if (duration <= 0) {
            return null
        }

        return TimedSessionStep(
            id = id,
            kind = TimedSessionStepKind.REST,
            sessionStepKind = SessionStepKind.TIMED_REST,
            blockId = blockId,
            itemId = itemId,
            durationSec = duration,
            round = round,
            roundCount = roundCount,
            endingCueThresholdSec = cue.effectiveThresholdSec(duration)
        )
    }

    private fun CountdownCue?.effectiveThresholdSec(durationSec: Int): Int? {
        val cue = this ?: return null
        return cue.thresholdSec.takeIf { threshold ->
            cue.enabled && threshold > 0 && threshold <= durationSec
        }
    }
}

data class TimedWorkoutEngineResult(
    val state: TimedWorkoutEngineState,
    val events: List<WorkoutEvent> = emptyList()
)

data class TimedWorkoutEngineState(
    val sessionId: String,
    val planTitle: String,
    val status: SessionStatus = SessionStatus.READY,
    val steps: List<TimedSessionStep>,
    val currentStepIndex: Int = -1,
    val remainingSec: Int = 0,
    val completedStepCount: Int = 0,
    val skippedStepIds: List<String> = emptyList(),
    val extendedRestSec: Int = 0,
    internal val emittedEndingCueKeys: Set<String> = emptySet()
) {
    val currentStep: TimedSessionStep?
        get() = steps.getOrNull(currentStepIndex)

    val currentSessionStep: SessionStep?
        get() = currentStep?.let { step ->
            SessionStep(
                id = step.id,
                kind = step.sessionStepKind,
                blockId = step.blockId,
                itemId = step.itemId,
                exerciseId = step.exerciseId,
                remainingSec = remainingSec,
                plannedDurationSec = step.durationSec
            )
        }

    val isTerminal: Boolean
        get() = status == SessionStatus.COMPLETED || status == SessionStatus.ABANDONED
}

data class TimedSessionStep(
    val id: String,
    val kind: TimedSessionStepKind,
    val sessionStepKind: SessionStepKind,
    val blockId: String,
    val itemId: String? = null,
    val exerciseId: String? = null,
    val durationSec: Int,
    val round: Int? = null,
    val roundCount: Int? = null,
    val endingCueThresholdSec: Int? = null
)

enum class TimedSessionStepKind {
    WORK,
    REST
}
