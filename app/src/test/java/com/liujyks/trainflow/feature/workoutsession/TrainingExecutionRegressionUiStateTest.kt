package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.StrengthSetCompletionInput
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import com.liujyks.trainflow.feature.plans.modePillColors
import com.liujyks.trainflow.feature.plans.modePillContrastRatio
import com.liujyks.trainflow.ui.theme.SkinRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingExecutionRegressionUiStateTest {
    @Test
    fun timedExecutionKeepsSmallScreenFixedControlStatesReachable() {
        val plan = buildDefaultPlanManagementState().plans.first()
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        val active = state.toTimedWorkoutSessionScreenState()
        assertTrue(active.canPause)
        assertTrue(active.canSkip)
        assertTrue(active.canEnd)
        assertFalse(active.canExtendRest)

        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        val rest = state.toTimedWorkoutSessionScreenState()
        assertTrue(rest.canPause)
        assertTrue(rest.canSkip)
        assertTrue(rest.canExtendRest)
        assertTrue(rest.canEnd)

        val paused = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession)
            .state
            .toTimedWorkoutSessionScreenState()
        assertTrue(paused.canResume)
        assertTrue(paused.canEnd)
        assertFalse(paused.canPause)
        assertFalse(paused.canSkip)
        assertFalse(paused.canExtendRest)
    }

    @Test
    fun strengthExecutionAndConfirmationKeepPrimaryControlsReachable() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        val prepare = state.toStrengthWorkoutSessionScreenState()
        assertTrue(prepare.canStartSet)
        assertTrue(prepare.canPause)
        assertTrue(prepare.canEnd)

        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        val active = state.toStrengthWorkoutSessionScreenState()
        assertTrue(active.canCompleteSet)
        assertTrue(active.canPause)
        assertTrue(active.canEnd)

        state = StrengthWorkoutEngine.tick(state, seconds = 5).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        val confirm = state.toStrengthWorkoutSessionScreenState()
        assertEquals(SessionStepKind.STRENGTH_CONFIRM_SET, state.currentSessionStep?.kind)
        assertTrue(confirm.canConfirmPlanned)
        assertTrue(requireNotNull(confirm.confirmation).canConfirm)
        assertTrue(confirm.canPause)
        assertTrue(confirm.canEnd)

        state = StrengthWorkoutEngine.dispatch(
            state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        ).state
        val rest = state.toStrengthWorkoutSessionScreenState()
        assertTrue(rest.canStartNextDuringRest)
        assertTrue(rest.canPause)
        assertTrue(rest.canEnd)
    }

    @Test
    fun builtInSkinSwitchingKeepsTrainingSemanticStateAndControlContract() {
        val plans = buildDefaultPlanManagementState().plans
        val timedState = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plans.first()),
            WorkoutCommand.StartSession
        ).state
        val strengthState = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plans[1]),
            WorkoutCommand.StartSession
        ).state
        val timedSemantics = timedState.toTimedWorkoutSessionScreenState().semanticSnapshot()
        val strengthSemantics = strengthState.toStrengthWorkoutSessionScreenState().semanticSnapshot()

        SkinRegistry.skins.forEach { skin ->
            assertTrue(skin.capabilityBoundary.contains("不改变"))
            assertTrue(skin.tokens.trainingButtonHeightDp >= 48)
            assertTrue(skin.tokens.secondaryButtonHeightDp >= 48)
            assertTrue(skin.tokens.executionControlReserveDp >= 132)

            WorkoutMode.entries.forEach { mode ->
                val colors = modePillColors(mode = mode, skin = skin)

                assertTrue(
                    "${skin.id} ${mode.name} mode pill contrast should stay readable",
                    modePillContrastRatio(
                        contentColor = colors.contentColor,
                        containerColor = colors.containerColor
                    ) >= 4.5f
                )
            }

            assertEquals(timedSemantics, timedState.toTimedWorkoutSessionScreenState().semanticSnapshot())
            assertEquals(strengthSemantics, strengthState.toStrengthWorkoutSessionScreenState().semanticSnapshot())
        }
    }

    @Test
    fun countdownReminderAudioBoundaryDoesNotRequestAudioFocusOrDucking() {
        val source = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(source.contains("ToneGenerator"))
        assertTrue(source.contains("AudioManager.STREAM_NOTIFICATION"))
        assertFalse(source.contains("requestAudioFocus"))
        assertFalse(source.contains("AudioFocusRequest"))
        assertFalse(source.contains("AUDIOFOCUS_GAIN"))
        assertFalse(source.contains("AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK"))
        assertFalse(source.contains("setWillPauseWhenDucked"))
        assertFalse(source.contains("adjustStreamVolume"))
    }

    private fun TimedWorkoutSessionScreenState.semanticSnapshot(): List<Any?> {
        return listOf(
            planTitle,
            statusLabel,
            phaseLabel,
            currentTitle,
            timerText,
            canPause,
            canResume,
            canSkip,
            canExtendRest,
            canEnd,
            heartRate.statusText,
            heartRate.valueText
        )
    }

    private fun StrengthWorkoutSessionScreenState.semanticSnapshot(): List<Any?> {
        return listOf(
            planTitle,
            statusLabel,
            phaseLabel,
            currentExerciseName,
            primaryMetricLabel,
            primaryMetricText,
            canStartSet,
            canCompleteSet,
            canConfirmPlanned,
            canStartNextDuringRest,
            canPause,
            canResume,
            canEnd,
            heartRate.statusText,
            heartRate.valueText
        )
    }
}
