package com.liujyks.trainflow.core.engine

import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.ExerciseSide
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStep
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.SetEffort
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetCompletionDraft
import com.liujyks.trainflow.core.model.StrengthSetCompletionInput
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StrengthSetRecord
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutEvent
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot

object StrengthWorkoutEngine {
    fun create(
        plan: WorkoutPlan,
        sessionId: String = "session-${plan.id}"
    ): StrengthWorkoutEngineState {
        return create(
            planSnapshot = WorkoutPlanSnapshot(
                planId = plan.id,
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
    ): StrengthWorkoutEngineState {
        require(planSnapshot.mode == WorkoutMode.STRENGTH) {
            "StrengthWorkoutEngine only supports strength plan snapshots."
        }

        return StrengthWorkoutEngineState(
            sessionId = sessionId,
            planTitle = planSnapshot.title,
            setSteps = planSnapshot.toStrengthSetSteps(),
            restEndingCue = planSnapshot.preferences
                ?.cueSettings
                ?.restEnding
                .effectiveCue()
        )
    }

    fun dispatch(
        state: StrengthWorkoutEngineState,
        command: WorkoutCommand
    ): StrengthWorkoutEngineResult {
        return when (command) {
            WorkoutCommand.StartSession -> start(state)
            WorkoutCommand.PauseSession -> pause(state)
            WorkoutCommand.ResumeSession -> resume(state)
            is WorkoutCommand.StartStrengthSet -> startStrengthSet(state, command.setPlanId)
            is WorkoutCommand.CompleteStrengthSet -> completeStrengthSet(state, command.draft)
            is WorkoutCommand.ConfirmStrengthSet -> confirmStrengthSet(state, command.record)
            is WorkoutCommand.EndSession -> endSession(state, command.reason)
            WorkoutCommand.SkipStep -> skipExercise(state)
            is WorkoutCommand.ReplaceExercise -> replaceExercise(state, command.fromExerciseId, command.toExerciseId)
            is WorkoutCommand.ExtendRest,
            is WorkoutCommand.UpdateActualWeight,
            is WorkoutCommand.UpdateActualReps -> StrengthWorkoutEngineResult(state = state)
        }
    }

    fun tick(
        state: StrengthWorkoutEngineState,
        seconds: Int = 1
    ): StrengthWorkoutEngineResult {
        if (seconds <= 0 || state.currentStepKind == null) {
            return StrengthWorkoutEngineResult(state = state)
        }
        if (state.status == SessionStatus.PAUSED) {
            return StrengthWorkoutEngineResult(
                state = state.copy(pausedElapsedSec = state.pausedElapsedSec + seconds)
            )
        }
        if (state.status != SessionStatus.ACTIVE) {
            return StrengthWorkoutEngineResult(state = state)
        }

        var workingState = state
        val events = mutableListOf<WorkoutEvent>()

        repeat(seconds) {
            when (workingState.currentStepKind) {
                SessionStepKind.STRENGTH_ACTIVE_SET -> {
                    workingState = workingState.copy(
                        activeSetElapsedSec = workingState.activeSetElapsedSec + 1,
                        sessionElapsedSec = workingState.sessionElapsedSec + 1
                    )
                }
                SessionStepKind.STRENGTH_REST -> {
                    if (workingState.restRemainingSec > 1) {
                        workingState = workingState.copy(
                            restRemainingSec = workingState.restRemainingSec - 1,
                            restElapsedSec = workingState.restElapsedSec + 1,
                            sessionElapsedSec = workingState.sessionElapsedSec + 1
                        )
                        val cueResult = emitRestEndingCueIfNeeded(workingState)
                        workingState = cueResult.state
                        events += cueResult.events
                    } else {
                        val completedRestState = workingState.copy(
                            restRemainingSec = 0,
                            restElapsedSec = workingState.restElapsedSec + 1,
                            sessionElapsedSec = workingState.sessionElapsedSec + 1
                        ).completeCurrentRest()
                        val advanceResult = advanceAfterRest(
                            state = completedRestState,
                            nextSetIndex = completedRestState.currentSetIndex + 1
                        )
                        workingState = advanceResult.state
                        events += advanceResult.events
                    }
                }
                else -> return@repeat
            }
        }

        return StrengthWorkoutEngineResult(state = workingState, events = events)
    }

    private fun start(state: StrengthWorkoutEngineState): StrengthWorkoutEngineResult {
        if (state.status != SessionStatus.READY) {
            return StrengthWorkoutEngineResult(state = state)
        }
        if (state.setSteps.isEmpty()) {
            return StrengthWorkoutEngineResult(
                state = state.copy(status = SessionStatus.COMPLETED),
                events = listOf(WorkoutEvent.SessionCompleted(sessionId = state.sessionId))
            )
        }

        val startedState = state.copy(status = SessionStatus.ACTIVE)
            .appendControlHistory(type = StrengthWorkoutControlHistoryType.START_SESSION)
        val prepareResult = enterPrepare(startedState, setIndex = 0)

        return StrengthWorkoutEngineResult(
            state = prepareResult.state,
            events = listOf(WorkoutEvent.SessionStarted(sessionId = state.sessionId)) + prepareResult.events
        )
    }

    private fun pause(state: StrengthWorkoutEngineState): StrengthWorkoutEngineResult {
        if (state.status != SessionStatus.ACTIVE || state.currentStepKind == null) {
            return StrengthWorkoutEngineResult(state = state)
        }

        return StrengthWorkoutEngineResult(
            state = state.copy(status = SessionStatus.PAUSED)
                .appendControlHistory(type = StrengthWorkoutControlHistoryType.PAUSE_SESSION),
            events = listOf(WorkoutEvent.SessionPaused(sessionId = state.sessionId))
        )
    }

    private fun resume(state: StrengthWorkoutEngineState): StrengthWorkoutEngineResult {
        if (state.status != SessionStatus.PAUSED || state.currentStepKind == null) {
            return StrengthWorkoutEngineResult(state = state)
        }

        return StrengthWorkoutEngineResult(
            state = state.copy(status = SessionStatus.ACTIVE)
                .appendControlHistory(type = StrengthWorkoutControlHistoryType.RESUME_SESSION),
            events = listOf(WorkoutEvent.SessionResumed(sessionId = state.sessionId))
        )
    }

    private fun startStrengthSet(
        state: StrengthWorkoutEngineState,
        requestedSetPlanId: String?
    ): StrengthWorkoutEngineResult {
        if (state.status != SessionStatus.ACTIVE) {
            return StrengthWorkoutEngineResult(state = state)
        }

        if (state.currentStepKind == SessionStepKind.STRENGTH_REST) {
            val nextSetIndex = state.currentSetIndex + 1
            val nextSet = state.setSteps.getOrNull(nextSetIndex)
                ?: return completeSession(state.completeCurrentRest())
            if (!requestedSetPlanId.matches(nextSet.setPlanId)) {
                return StrengthWorkoutEngineResult(state = state)
            }
            return enterActive(
                state = state.completeCurrentRest(),
                setIndex = nextSetIndex
            )
        }

        val currentSet = state.currentSet ?: return StrengthWorkoutEngineResult(state = state)
        if (
            state.currentStepKind != SessionStepKind.STRENGTH_PREPARE_SET ||
            !requestedSetPlanId.matches(currentSet.setPlanId)
        ) {
            return StrengthWorkoutEngineResult(state = state)
        }

        return enterActive(state = state, setIndex = state.currentSetIndex)
    }

    private fun completeStrengthSet(
        state: StrengthWorkoutEngineState,
        draft: StrengthSetCompletionDraft?
    ): StrengthWorkoutEngineResult {
        val currentSet = state.currentSet
        if (
            state.status != SessionStatus.ACTIVE ||
            state.currentStepKind != SessionStepKind.STRENGTH_ACTIVE_SET ||
            currentSet == null
        ) {
            return StrengthWorkoutEngineResult(state = state)
        }

        val activeDurationSec = state.activeSetElapsedSec
        val confirmationDraft = currentSet.toDraft(activeDurationSec)
        val activeCompletedState = state.completeCurrentStep(
            status = StrengthSessionStepHistoryStatus.COMPLETED,
            endedAtElapsedSec = state.sessionElapsedSec,
            actualDurationSec = activeDurationSec
        )
        val confirmState = activeCompletedState.copy(
            currentStepKind = SessionStepKind.STRENGTH_CONFIRM_SET,
            activeSetElapsedSec = activeDurationSec,
            pendingDraft = confirmationDraft
        ).recordStepStarted(
            step = currentSet,
            kind = SessionStepKind.STRENGTH_CONFIRM_SET
        ).appendControlHistory(
            type = StrengthWorkoutControlHistoryType.COMPLETE_STRENGTH_SET,
            set = currentSet
        )

        return StrengthWorkoutEngineResult(
            state = confirmState,
            events = listOf(WorkoutEvent.StrengthSetCompleted(setRecordId = confirmationDraft.recordId))
        )
    }

    private fun confirmStrengthSet(
        state: StrengthWorkoutEngineState,
        input: StrengthSetCompletionInput
    ): StrengthWorkoutEngineResult {
        val currentSet = state.currentSet
        val draft = state.pendingDraft
        if (
            state.status != SessionStatus.ACTIVE ||
            state.currentStepKind != SessionStepKind.STRENGTH_CONFIRM_SET ||
            currentSet == null ||
            draft == null
        ) {
            return StrengthWorkoutEngineResult(state = state)
        }

        val record = draft.toRecord(input)
        val confirmedState = state.copy(
            completedSetCount = state.completedSetCount + 1,
            strengthSetRecords = state.strengthSetRecords + record,
            pendingDraft = null
        ).completeCurrentStep(
            status = StrengthSessionStepHistoryStatus.COMPLETED,
            endedAtElapsedSec = state.sessionElapsedSec,
            actualDurationSec = 0
        ).appendControlHistory(
            type = StrengthWorkoutControlHistoryType.CONFIRM_STRENGTH_SET,
            set = currentSet
        )

        val nextSetIndex = confirmedState.currentSetIndex + 1
        val hasNextSet = nextSetIndex < confirmedState.setSteps.size
        val restAfterSec = currentSet.restAfterSec ?: 0
        if (hasNextSet && restAfterSec > 0) {
            return enterRest(confirmedState, currentSet, restAfterSec)
        }
        if (hasNextSet) {
            return advanceToPrepare(confirmedState, nextSetIndex)
        }

        return completeSession(confirmedState)
    }

    private fun endSession(
        state: StrengthWorkoutEngineState,
        reason: String?
    ): StrengthWorkoutEngineResult {
        if (state.isTerminal) {
            return StrengthWorkoutEngineResult(state = state)
        }

        val currentSet = state.currentSet
        val endedState = state.completeCurrentStep(
            status = StrengthSessionStepHistoryStatus.ABANDONED,
            endedAtElapsedSec = state.sessionElapsedSec,
            actualDurationSec = state.currentStepActualDurationSec(),
            remainingSec = state.currentStepRemainingSec()
        ).copy(
            status = SessionStatus.ABANDONED,
            earlyEnd = StrengthWorkoutEarlyEndRecord(
                reason = reason,
                elapsedSec = state.sessionElapsedSec,
                completedSetCount = state.completedSetCount,
                currentStepId = state.currentSessionStep?.id,
                currentStepKind = state.currentStepKind,
                currentSetPlanId = currentSet?.setPlanId,
                currentStepRemainingSec = state.currentStepRemainingSec(),
                currentStepActualDurationSec = state.currentStepActualDurationSec()
            )
        ).appendControlHistory(
            type = StrengthWorkoutControlHistoryType.END_SESSION,
            set = currentSet,
            remainingSec = state.currentStepRemainingSec(),
            reason = reason
        )

        return StrengthWorkoutEngineResult(state = endedState)
    }

    private fun replaceExercise(
        state: StrengthWorkoutEngineState,
        fromExerciseId: String,
        toExerciseId: String
    ): StrengthWorkoutEngineResult {
        val currentSet = state.currentSet
        if (
            state.status != SessionStatus.ACTIVE ||
            state.currentStepKind !in replaceableStrengthSteps ||
            currentSet == null ||
            currentSet.exerciseId != fromExerciseId ||
            fromExerciseId == toExerciseId
        ) {
            return StrengthWorkoutEngineResult(state = state)
        }

        val originalExerciseId = currentSet.substitutedFromExerciseId ?: fromExerciseId
        val updatedSteps = state.setSteps.map { step ->
            if (step.blockId == currentSet.blockId && step.globalSetIndex >= state.currentSetIndex) {
                step.copy(
                    exerciseId = toExerciseId,
                    substitutedFromExerciseId = originalExerciseId
                )
            } else {
                step
            }
        }
        val updatedHistory = state.stepHistory.map { record ->
            if (
                record.blockId == currentSet.blockId &&
                record.setPlanId == currentSet.setPlanId &&
                record.status == StrengthSessionStepHistoryStatus.STARTED
            ) {
                record.copy(
                    exerciseId = toExerciseId,
                    substitutedFromExerciseId = originalExerciseId
                )
            } else {
                record
            }
        }
        val updatedDraft = state.pendingDraft?.copy(
            exerciseId = toExerciseId,
            substitutedFromExerciseId = originalExerciseId
        )
        val replacedState = state.copy(
            setSteps = updatedSteps,
            stepHistory = updatedHistory,
            pendingDraft = updatedDraft
        ).appendControlHistory(
            type = StrengthWorkoutControlHistoryType.REPLACE_EXERCISE,
            set = currentSet.copy(
                exerciseId = toExerciseId,
                substitutedFromExerciseId = originalExerciseId
            ),
            fromExerciseId = originalExerciseId,
            toExerciseId = toExerciseId
        )

        return StrengthWorkoutEngineResult(
            state = replacedState,
            events = listOf(WorkoutEvent.NextExerciseReady(exerciseId = toExerciseId))
        )
    }

    private fun skipExercise(state: StrengthWorkoutEngineState): StrengthWorkoutEngineResult {
        val currentSet = state.currentSet
        if (
            state.status != SessionStatus.ACTIVE ||
            state.currentStepKind !in skippableStrengthSteps ||
            currentSet == null
        ) {
            return StrengthWorkoutEngineResult(state = state)
        }

        val skippedCurrentState = when (state.currentStepKind) {
            SessionStepKind.STRENGTH_REST -> state.completeCurrentStep(
                status = StrengthSessionStepHistoryStatus.SKIPPED,
                endedAtElapsedSec = state.sessionElapsedSec,
                actualDurationSec = state.restElapsedSec,
                remainingSec = state.restRemainingSec
            ).copy(
                strengthSetRecords = state.strengthSetRecords.mapIndexed { index, record ->
                    if (index == state.strengthSetRecords.lastIndex) {
                        record.copy(actualRestAfterSec = state.restElapsedSec)
                    } else {
                        record
                    }
                }
            )

            else -> state.completeCurrentStep(
                status = StrengthSessionStepHistoryStatus.SKIPPED,
                endedAtElapsedSec = state.sessionElapsedSec,
                actualDurationSec = state.currentStepActualDurationSec(),
                remainingSec = state.currentStepRemainingSec()
            )
        }
        val firstSkippedSetIndex = if (state.currentStepKind == SessionStepKind.STRENGTH_REST) {
            state.currentSetIndex + 1
        } else {
            state.currentSetIndex
        }
        val nextSetIndex = skippedCurrentState.setSteps.indexOfFirst { step ->
            step.globalSetIndex > state.currentSetIndex && step.blockId != currentSet.blockId
        }.takeIf { index -> index >= 0 }
        val lastSkippedSetIndex = (nextSetIndex ?: skippedCurrentState.setSteps.size) - 1
        val withSkippedHistory = if (firstSkippedSetIndex <= lastSkippedSetIndex) {
            skippedCurrentState.recordSkippedSetSteps(
                firstIndex = firstSkippedSetIndex,
                lastIndex = lastSkippedSetIndex
            )
        } else {
            skippedCurrentState
        }
        val skippedState = withSkippedHistory.copy(
            pendingDraft = null,
            activeSetElapsedSec = 0,
            restRemainingSec = 0,
            restElapsedSec = 0
        ).appendControlHistory(
            type = StrengthWorkoutControlHistoryType.SKIP_EXERCISE,
            set = currentSet,
            remainingSec = state.currentStepRemainingSec(),
            fromExerciseId = currentSet.exerciseId
        )

        return if (nextSetIndex == null) {
            completeSession(skippedState)
        } else {
            advanceToPrepare(
                state = skippedState,
                nextSetIndex = nextSetIndex
            )
        }
    }

    private fun enterPrepare(
        state: StrengthWorkoutEngineState,
        setIndex: Int
    ): StrengthWorkoutEngineResult {
        return advanceToPrepare(state, setIndex)
    }

    private fun advanceToPrepare(
        state: StrengthWorkoutEngineState,
        nextSetIndex: Int
    ): StrengthWorkoutEngineResult {
        val nextSet = state.setSteps.getOrNull(nextSetIndex)
            ?: return completeSession(state)
        val previousSet = state.currentSet
        val prepareState = state.copy(
            currentSetIndex = nextSetIndex,
            currentStepKind = SessionStepKind.STRENGTH_PREPARE_SET,
            activeSetElapsedSec = 0,
            restRemainingSec = 0,
            restElapsedSec = 0,
            pendingDraft = null
        ).recordStepStarted(
            step = nextSet,
            kind = SessionStepKind.STRENGTH_PREPARE_SET
        )
        val events = buildList {
            if (previousSet != null && previousSet.exerciseId != nextSet.exerciseId) {
                add(WorkoutEvent.NextExerciseReady(exerciseId = nextSet.exerciseId))
            }
            add(
                WorkoutEvent.StrengthSetReady(
                    exerciseId = nextSet.exerciseId,
                    setPlanId = nextSet.setPlanId
                )
            )
        }

        return StrengthWorkoutEngineResult(state = prepareState, events = events)
    }

    private fun advanceAfterRest(
        state: StrengthWorkoutEngineState,
        nextSetIndex: Int
    ): StrengthWorkoutEngineResult {
        val nextSet = state.setSteps.getOrNull(nextSetIndex)
            ?: return completeSession(state)
        if (nextSet.setTimerMode != StrengthSetTimerMode.AUTO_AFTER_REST) {
            return advanceToPrepare(state, nextSetIndex)
        }

        val previousSet = state.currentSet
        val activeResult = enterActive(state = state, setIndex = nextSetIndex)
        val events = buildList {
            if (previousSet != null && previousSet.exerciseId != nextSet.exerciseId) {
                add(WorkoutEvent.NextExerciseReady(exerciseId = nextSet.exerciseId))
            }
            addAll(activeResult.events)
        }

        return activeResult.copy(events = events)
    }

    private fun enterActive(
        state: StrengthWorkoutEngineState,
        setIndex: Int
    ): StrengthWorkoutEngineResult {
        val set = state.setSteps.getOrNull(setIndex) ?: return StrengthWorkoutEngineResult(state = state)
        val activeState = state.copy(
            currentSetIndex = setIndex,
            currentStepKind = SessionStepKind.STRENGTH_ACTIVE_SET,
            activeSetElapsedSec = 0,
            pendingDraft = null
        ).recordStepStarted(
            step = set,
            kind = SessionStepKind.STRENGTH_ACTIVE_SET
        ).appendControlHistory(
            type = StrengthWorkoutControlHistoryType.START_STRENGTH_SET,
            set = set
        )

        return StrengthWorkoutEngineResult(
            state = activeState,
            events = listOf(
                WorkoutEvent.StrengthSetStarted(
                    exerciseId = set.exerciseId,
                    setPlanId = set.setPlanId
                )
            )
        )
    }

    private fun enterRest(
        state: StrengthWorkoutEngineState,
        completedSet: StrengthSessionSetStep,
        restAfterSec: Int
    ): StrengthWorkoutEngineResult {
        val restState = state.copy(
            currentStepKind = SessionStepKind.STRENGTH_REST,
            restRemainingSec = restAfterSec,
            restElapsedSec = 0,
            activeSetElapsedSec = 0,
            emittedRestEndingSeconds = emptySet()
        ).recordStepStarted(
            step = completedSet,
            kind = SessionStepKind.STRENGTH_REST
        )
        val cueResult = emitRestEndingCueIfNeeded(restState)

        return StrengthWorkoutEngineResult(
            state = cueResult.state,
            events = listOf(
                WorkoutEvent.RestStarted(
                    stepId = restState.currentSessionStep?.id ?: completedSet.restStepId,
                    durationSec = restAfterSec
                )
            ) + cueResult.events
        )
    }

    private fun completeSession(state: StrengthWorkoutEngineState): StrengthWorkoutEngineResult {
        return StrengthWorkoutEngineResult(
            state = state.copy(
                status = SessionStatus.COMPLETED,
                currentStepKind = SessionStepKind.COMPLETED,
                restRemainingSec = 0,
                activeSetElapsedSec = 0,
                pendingDraft = null
            ),
            events = listOf(WorkoutEvent.SessionCompleted(sessionId = state.sessionId))
        )
    }

    private fun emitRestEndingCueIfNeeded(state: StrengthWorkoutEngineState): StrengthWorkoutEngineResult {
        val cue = state.restEndingCue ?: return StrengthWorkoutEngineResult(state = state)
        val restDurationSec = state.currentSessionStep?.plannedDurationSec
            ?: return StrengthWorkoutEngineResult(state = state)
        val effectiveThresholdSec = cue.thresholdSec.coerceAtMost(restDurationSec)
        if (
            state.currentStepKind != SessionStepKind.STRENGTH_REST ||
            state.restRemainingSec > effectiveThresholdSec ||
            state.restRemainingSec <= 0 ||
            state.restRemainingSec in state.emittedRestEndingSeconds
        ) {
            return StrengthWorkoutEngineResult(state = state)
        }

        return StrengthWorkoutEngineResult(
            state = state.copy(
                emittedRestEndingSeconds = state.emittedRestEndingSeconds + state.restRemainingSec
            ),
            events = listOf(
                WorkoutEvent.RestEnding(
                    stepId = state.currentSessionStep?.id ?: "strength-rest",
                    remainingSec = state.restRemainingSec
                )
            )
        )
    }

    private fun StrengthWorkoutEngineState.recordStepStarted(
        step: StrengthSessionSetStep,
        kind: SessionStepKind
    ): StrengthWorkoutEngineState {
        val stepId = step.stepId(kind)
        if (stepHistory.any { record -> record.stepId == stepId }) {
            return this
        }

        return copy(
            stepHistory = stepHistory + StrengthSessionStepHistoryRecord(
                stepId = stepId,
                kind = kind,
                exerciseId = step.exerciseId,
                blockId = step.blockId,
                setPlanId = step.setPlanId,
                setOrder = step.setOrder,
                startedAtElapsedSec = sessionElapsedSec,
                substitutedFromExerciseId = step.substitutedFromExerciseId
            )
        )
    }

    private fun StrengthWorkoutEngineState.recordSkippedSetSteps(
        firstIndex: Int,
        lastIndex: Int
    ): StrengthWorkoutEngineState {
        val newRecords = setSteps
            .slice(firstIndex..lastIndex)
            .map { step ->
                StrengthSessionStepHistoryRecord(
                    stepId = step.prepareStepId,
                    kind = SessionStepKind.STRENGTH_PREPARE_SET,
                    exerciseId = step.exerciseId,
                    blockId = step.blockId,
                    setPlanId = step.setPlanId,
                    setOrder = step.setOrder,
                    startedAtElapsedSec = sessionElapsedSec,
                    endedAtElapsedSec = sessionElapsedSec,
                    status = StrengthSessionStepHistoryStatus.SKIPPED,
                    actualDurationSec = 0,
                    substitutedFromExerciseId = step.substitutedFromExerciseId
                )
            }
            .filterNot { skipped ->
                stepHistory.any { record -> record.stepId == skipped.stepId }
            }

        return copy(stepHistory = stepHistory + newRecords)
    }

    private fun StrengthWorkoutEngineState.completeCurrentStep(
        status: StrengthSessionStepHistoryStatus,
        endedAtElapsedSec: Int,
        actualDurationSec: Int?,
        remainingSec: Int? = null
    ): StrengthWorkoutEngineState {
        val currentStep = currentSessionStep ?: return this
        val existingRecord = stepHistory.lastOrNull { record -> record.stepId == currentStep.id }
        val completedRecord = (existingRecord ?: currentSet?.let { set ->
            StrengthSessionStepHistoryRecord(
                stepId = currentStep.id,
                kind = currentStep.kind,
                exerciseId = set.exerciseId,
                blockId = set.blockId,
                setPlanId = set.setPlanId,
                setOrder = set.setOrder,
                startedAtElapsedSec = sessionElapsedSec,
                substitutedFromExerciseId = set.substitutedFromExerciseId
            )
        } ?: return this).copy(
            endedAtElapsedSec = endedAtElapsedSec,
            status = status,
            actualDurationSec = actualDurationSec,
            remainingSec = remainingSec
        )
        val updatedHistory = if (existingRecord == null) {
            stepHistory + completedRecord
        } else {
            stepHistory.map { record ->
                if (record.stepId == currentStep.id) completedRecord else record
            }
        }

        return copy(stepHistory = updatedHistory)
    }

    private fun StrengthWorkoutEngineState.completeCurrentRest(): StrengthWorkoutEngineState {
        val restActualSec = restElapsedSec
        val completedState = completeCurrentStep(
            status = StrengthSessionStepHistoryStatus.COMPLETED,
            endedAtElapsedSec = sessionElapsedSec,
            actualDurationSec = restActualSec,
            remainingSec = restRemainingSec
        )

        return completedState.copy(
            strengthSetRecords = completedState.strengthSetRecords.mapIndexed { index, record ->
                if (index == completedState.strengthSetRecords.lastIndex) {
                    record.copy(actualRestAfterSec = restActualSec)
                } else {
                    record
                }
            }
        )
    }

    private fun StrengthWorkoutEngineState.appendControlHistory(
        type: StrengthWorkoutControlHistoryType,
        set: StrengthSessionSetStep? = currentSet,
        remainingSec: Int? = currentStepRemainingSec(),
        reason: String? = null,
        fromExerciseId: String? = null,
        toExerciseId: String? = null
    ): StrengthWorkoutEngineState {
        return copy(
            controlHistory = controlHistory + StrengthWorkoutControlHistoryEvent(
                type = type,
                elapsedSec = sessionElapsedSec,
                stepId = currentSessionStep?.id,
                stepKind = currentStepKind,
                setPlanId = set?.setPlanId,
                remainingSec = remainingSec,
                reason = reason,
                fromExerciseId = fromExerciseId,
                toExerciseId = toExerciseId
            )
        )
    }

    private fun String?.matches(setPlanId: String): Boolean {
        return this == null || this == setPlanId
    }

    private fun WorkoutPlanSnapshot.toStrengthSetSteps(): List<StrengthSessionSetStep> {
        return blocks
            .filterIsInstance<StrengthExerciseBlock>()
            .sortedBy { block -> block.order }
            .flatMap { block ->
                block.sets
                    .sortedBy { set -> set.order }
                    .mapIndexed { index, setPlan ->
                        block.toSetStep(
                            setPlan = setPlan,
                            exerciseSetIndex = index,
                            exerciseSetCount = block.sets.size
                        )
                    }
            }
            .mapIndexed { index, step ->
                step.copy(globalSetIndex = index, totalSetCount = blocks.totalStrengthSetCount())
            }
    }

    private fun List<PlanBlock>.totalStrengthSetCount(): Int {
        return filterIsInstance<StrengthExerciseBlock>().sumOf { block -> block.sets.size }
    }

    private fun StrengthExerciseBlock.toSetStep(
        setPlan: StrengthSetPlan,
        exerciseSetIndex: Int,
        exerciseSetCount: Int
    ): StrengthSessionSetStep {
        return StrengthSessionSetStep(
            blockId = id,
            exerciseId = exerciseId,
            setPlanId = setPlan.id,
            setOrder = setPlan.order,
            setKind = setPlan.kind,
            side = setPlan.side,
            plannedWeight = setPlan.targetWeight ?: target?.weight,
            plannedRepTarget = setPlan.repTarget ?: target?.repTarget,
            restAfterSec = setPlan.restAfterSec ?: target?.restAfterSetSec,
            exerciseSetIndex = exerciseSetIndex,
            exerciseSetCount = exerciseSetCount,
            substitutionExerciseIds = substitutions,
            setTimerMode = setTimerMode
        )
    }

    private fun StrengthSessionSetStep.toDraft(activeDurationSec: Int): StrengthSetDraft {
        return StrengthSetDraft(
            recordId = "$setPlanId-record",
            exerciseId = exerciseId,
            sourceSetPlanId = setPlanId,
            setOrder = setOrder,
            setKind = setKind,
            side = side,
            plannedWeight = plannedWeight,
            plannedRepTarget = plannedRepTarget,
            defaultActualWeight = plannedWeight,
            defaultActualReps = plannedRepTarget.defaultActualReps(),
            activeDurationSec = activeDurationSec,
            substitutedFromExerciseId = substitutedFromExerciseId
        )
    }

    private fun RepTarget?.defaultActualReps(): Int? {
        return when (this) {
            is RepTarget.Fixed -> reps
            is RepTarget.Range -> minReps
            null -> null
        }
    }

    private fun StrengthSetDraft.toRecord(input: StrengthSetCompletionInput): StrengthSetRecord {
        return StrengthSetRecord(
            id = recordId,
            exerciseId = exerciseId,
            sourceSetPlanId = sourceSetPlanId,
            setOrder = setOrder,
            setKind = setKind,
            side = side,
            plannedWeight = plannedWeight,
            plannedRepTarget = plannedRepTarget,
            actualWeight = input.actualWeight ?: defaultActualWeight,
            actualReps = input.actualReps ?: defaultActualReps,
            activeDurationSec = activeDurationSec,
            effort = input.effort,
            substitutedFromExerciseId = substitutedFromExerciseId,
            notes = input.notes
        )
    }

    private fun CountdownCue?.effectiveCue(): CountdownCue? {
        val cue = this ?: return null
        return cue.takeIf { it.enabled && it.thresholdSec > 0 }
    }
}

data class StrengthWorkoutEngineResult(
    val state: StrengthWorkoutEngineState,
    val events: List<WorkoutEvent> = emptyList()
)

data class StrengthWorkoutEngineState(
    val sessionId: String,
    val planTitle: String,
    val status: SessionStatus = SessionStatus.READY,
    val setSteps: List<StrengthSessionSetStep>,
    val currentSetIndex: Int = -1,
    val currentStepKind: SessionStepKind? = null,
    val activeSetElapsedSec: Int = 0,
    val restRemainingSec: Int = 0,
    val restElapsedSec: Int = 0,
    val sessionElapsedSec: Int = 0,
    val pausedElapsedSec: Int = 0,
    val completedSetCount: Int = 0,
    val pendingDraft: StrengthSetDraft? = null,
    val strengthSetRecords: List<StrengthSetRecord> = emptyList(),
    val stepHistory: List<StrengthSessionStepHistoryRecord> = emptyList(),
    val controlHistory: List<StrengthWorkoutControlHistoryEvent> = emptyList(),
    val earlyEnd: StrengthWorkoutEarlyEndRecord? = null,
    internal val restEndingCue: CountdownCue? = null,
    internal val emittedRestEndingSeconds: Set<Int> = emptySet()
) {
    val currentSet: StrengthSessionSetStep?
        get() = setSteps.getOrNull(currentSetIndex)

    val currentSessionStep: SessionStep?
        get() = currentSet?.let { set ->
            val kind = currentStepKind ?: return@let null
            SessionStep(
                id = set.stepId(kind),
                kind = kind,
                blockId = set.blockId,
                setPlanId = set.setPlanId,
                exerciseId = set.exerciseId,
                remainingSec = if (kind == SessionStepKind.STRENGTH_REST) restRemainingSec else null,
                plannedDurationSec = if (kind == SessionStepKind.STRENGTH_REST) set.restAfterSec else null
            )
        }

    val isTerminal: Boolean
        get() = status == SessionStatus.COMPLETED || status == SessionStatus.ABANDONED

    internal fun currentStepRemainingSec(): Int? {
        return if (currentStepKind == SessionStepKind.STRENGTH_REST) restRemainingSec else null
    }

    internal fun currentStepActualDurationSec(): Int {
        return when (currentStepKind) {
            SessionStepKind.STRENGTH_ACTIVE_SET -> activeSetElapsedSec
            SessionStepKind.STRENGTH_REST -> restElapsedSec
            else -> 0
        }
    }
}

data class StrengthSessionSetStep(
    val blockId: String,
    val exerciseId: String,
    val setPlanId: String,
    val setOrder: Int,
    val setKind: StrengthSetKind,
    val side: ExerciseSide? = null,
    val plannedWeight: WeightValue? = null,
    val plannedRepTarget: RepTarget? = null,
    val restAfterSec: Int? = null,
    val exerciseSetIndex: Int,
    val exerciseSetCount: Int,
    val globalSetIndex: Int = 0,
    val totalSetCount: Int = 0,
    val substitutedFromExerciseId: String? = null,
    val substitutionExerciseIds: List<String> = emptyList(),
    val setTimerMode: StrengthSetTimerMode = StrengthSetTimerMode.MANUAL_START
) {
    val prepareStepId: String = "$blockId-$setPlanId-prepare"
    val activeStepId: String = "$blockId-$setPlanId-active"
    val confirmStepId: String = "$blockId-$setPlanId-confirm"
    val restStepId: String = "$blockId-$setPlanId-rest"

    fun stepId(kind: SessionStepKind): String {
        return when (kind) {
            SessionStepKind.STRENGTH_PREPARE_SET -> prepareStepId
            SessionStepKind.STRENGTH_ACTIVE_SET -> activeStepId
            SessionStepKind.STRENGTH_CONFIRM_SET -> confirmStepId
            SessionStepKind.STRENGTH_REST -> restStepId
            else -> "$blockId-$setPlanId-${kind.contractValue}"
        }
    }
}

data class StrengthSetDraft(
    val recordId: String,
    val exerciseId: String,
    val sourceSetPlanId: String,
    val setOrder: Int,
    val setKind: StrengthSetKind,
    val side: ExerciseSide? = null,
    val plannedWeight: WeightValue? = null,
    val plannedRepTarget: RepTarget? = null,
    val defaultActualWeight: WeightValue? = null,
    val defaultActualReps: Int? = null,
    val activeDurationSec: Int,
    val substitutedFromExerciseId: String? = null
)

private val replaceableStrengthSteps = setOf(
    SessionStepKind.STRENGTH_PREPARE_SET,
    SessionStepKind.STRENGTH_ACTIVE_SET,
    SessionStepKind.STRENGTH_CONFIRM_SET
)

private val skippableStrengthSteps = setOf(
    SessionStepKind.STRENGTH_PREPARE_SET,
    SessionStepKind.STRENGTH_ACTIVE_SET,
    SessionStepKind.STRENGTH_CONFIRM_SET,
    SessionStepKind.STRENGTH_REST
)
