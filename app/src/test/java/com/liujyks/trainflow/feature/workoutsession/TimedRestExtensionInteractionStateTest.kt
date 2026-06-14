package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedRestExtensionInteractionStateTest {
    @Test
    fun firstClickOnlyArmsConfirmationWithoutDispatchingOrRecording() {
        val restState = activeRestState()
        val result = TimedRestExtensionInteractionState().onRestExtensionClick(
            engineState = restState,
            nowMillis = 1_000
        )
        val uiState = result.state.toRestExtensionControlUiState(restState, nowMillis = 1_000)

        assertFalse(result.shouldDispatchExtendRest)
        assertEquals(0, restState.extendedRestSec)
        assertTrue(restState.restExtensionHistory.isEmpty())
        assertEquals("确认 +15秒", uiState.buttonLabel)
        assertTrue(uiState.buttonEnabled)
    }

    @Test
    fun secondClickWithinTwoSecondsDispatchesAndShowsSuccessFeedback() {
        var restState = activeRestState()
        var interaction = TimedRestExtensionInteractionState()

        val first = interaction.onRestExtensionClick(restState, nowMillis = 1_000)
        interaction = first.state
        val second = interaction.onRestExtensionClick(restState, nowMillis = 2_500)
        assertTrue(second.shouldDispatchExtendRest)

        restState = TimedWorkoutEngine.dispatch(
            restState,
            WorkoutCommand.ExtendRest(seconds = TimedRestExtensionSeconds)
        ).state
        val uiState = second.state.toRestExtensionControlUiState(restState, nowMillis = 2_500)

        assertEquals(15, restState.extendedRestSec)
        assertEquals(1, restState.restExtensionHistory.size)
        assertEquals("已加 15秒", uiState.buttonLabel)
        assertFalse(uiState.buttonEnabled)
        assertEquals(1, uiState.extensionCount)
        assertEquals(15, uiState.cumulativeExtraRestSec)
    }

    @Test
    fun pendingConfirmationExpiresAfterTwoSecondsWithoutRecording() {
        val restState = activeRestState()
        val armed = TimedRestExtensionInteractionState()
            .onRestExtensionClick(restState, nowMillis = 1_000)
            .state
        val expiredUi = armed.toRestExtensionControlUiState(restState, nowMillis = 3_001)
        val secondClickAfterExpiry = armed.onRestExtensionClick(restState, nowMillis = 3_001)

        assertEquals("+15秒", expiredUi.buttonLabel)
        assertTrue(expiredUi.buttonEnabled)
        assertFalse(secondClickAfterExpiry.shouldDispatchExtendRest)
        assertTrue(restState.restExtensionHistory.isEmpty())
    }

    @Test
    fun successFeedbackRestoresAfterEightHundredMillis() {
        var restState = activeRestState()
        var interaction = TimedRestExtensionInteractionState()
            .onRestExtensionClick(restState, nowMillis = 1_000)
            .state
        val confirmed = interaction.onRestExtensionClick(restState, nowMillis = 1_400)
        restState = TimedWorkoutEngine.dispatch(
            restState,
            WorkoutCommand.ExtendRest(seconds = TimedRestExtensionSeconds)
        ).state
        interaction = confirmed.state

        assertEquals(
            "已加 15秒",
            interaction.toRestExtensionControlUiState(restState, nowMillis = 1_900).buttonLabel
        )
        assertEquals(
            "+15秒",
            interaction.toRestExtensionControlUiState(restState, nowMillis = 2_200).buttonLabel
        )
    }

    @Test
    fun eachRestStepAllowsAtMostFourConfirmedExtensions() {
        var restState = activeRestState()
        var interaction = TimedRestExtensionInteractionState()
        repeat(4) { index ->
            val armed = interaction.onRestExtensionClick(restState, nowMillis = 1_000L + index * 3_000L)
            assertFalse(armed.shouldDispatchExtendRest)
            val confirmed = armed.state.onRestExtensionClick(restState, nowMillis = 1_500L + index * 3_000L)
            assertTrue(confirmed.shouldDispatchExtendRest)
            restState = TimedWorkoutEngine.dispatch(
                restState,
                WorkoutCommand.ExtendRest(seconds = TimedRestExtensionSeconds)
            ).state
            interaction = confirmed.state
        }

        val fifth = interaction.onRestExtensionClick(restState, nowMillis = 20_000)
        val limitUi = fifth.state.toRestExtensionControlUiState(restState, nowMillis = 20_000)

        assertFalse(fifth.shouldDispatchExtendRest)
        assertEquals(60, restState.extendedRestSec)
        assertEquals(4, restState.restExtensionHistory.size)
        assertEquals(4, limitUi.extensionCount)
        assertEquals(60, limitUi.cumulativeExtraRestSec)
        assertTrue(limitUi.hitExtensionLimit)
        assertFalse(limitUi.buttonEnabled)
        assertEquals("已额外休息 1 分钟，需要更久可以暂停训练", limitUi.helperText)
    }

    @Test
    fun pendingConfirmationClearsAcrossRestSteps() {
        val firstRest = activeRestState(twoRestStepsPlan())
        val armed = TimedRestExtensionInteractionState()
            .onRestExtensionClick(firstRest, nowMillis = 1_000)
            .state
        var nextRest = TimedWorkoutEngine.dispatch(firstRest, WorkoutCommand.SkipStep).state
        nextRest = TimedWorkoutEngine.tick(nextRest, seconds = 4).state
        val cleared = armed.clearForCurrentEngineStep(nextRest, nowMillis = 1_500)
        val secondClick = cleared.onRestExtensionClick(nextRest, nowMillis = 1_500)

        assertEquals("+15秒", cleared.toRestExtensionControlUiState(nextRest, nowMillis = 1_500).buttonLabel)
        assertFalse(secondClick.shouldDispatchExtendRest)
        assertEquals("确认 +15秒", secondClick.state.toRestExtensionControlUiState(nextRest, nowMillis = 1_500).buttonLabel)
    }

    @Test
    fun multipleConfirmationsAccumulateCumulativeExtraRest() {
        var restState = activeRestState()
        var interaction = TimedRestExtensionInteractionState()
        repeat(3) { index ->
            val now = 1_000L + index * 3_000L
            interaction = interaction.onRestExtensionClick(restState, nowMillis = now).state
            val confirmed = interaction.onRestExtensionClick(restState, nowMillis = now + 500)
            assertTrue(confirmed.shouldDispatchExtendRest)
            restState = TimedWorkoutEngine.dispatch(
                restState,
                WorkoutCommand.ExtendRest(seconds = TimedRestExtensionSeconds)
            ).state
            interaction = confirmed.state
        }

        assertEquals(listOf(15, 30, 45), restState.restExtensionHistory.map { it.cumulativeAddedSec })
        val uiState = interaction.toRestExtensionControlUiState(restState, nowMillis = 10_500)
        assertEquals(3, uiState.extensionCount)
        assertEquals(45, uiState.cumulativeExtraRestSec)
    }

    private fun activeRestState(plan: WorkoutPlan = restExtensionPlan()): TimedWorkoutEngineState {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        return state
    }

    private fun restExtensionPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "rest-extension-confirmation-plan",
            mode = WorkoutMode.TIMED,
            title = "Rest extension confirmation",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "confirm-circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "work",
                            labelOverride = "Work",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 4,
                            restAfterSec = 10
                        )
                    )
                )
            ),
            preferences = PlanPreferences(
                cueSettings = CueSettings(restEnding = CountdownCue(thresholdSec = 2))
            ),
            createdAt = "2026-06-14T00:00:00Z",
            updatedAt = "2026-06-14T00:00:00Z"
        )
    }

    private fun twoRestStepsPlan(): WorkoutPlan {
        return restExtensionPlan().copy(
            id = "rest-extension-two-rest-steps-plan",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "confirm-circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "work-1",
                            labelOverride = "Work 1",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 4,
                            restAfterSec = 10
                        ),
                        TimedExerciseItem(
                            id = "work-2",
                            labelOverride = "Work 2",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 4,
                            restAfterSec = 10
                        )
                    )
                )
            )
        )
    }
}
