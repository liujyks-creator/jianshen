package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.media.WorkoutSoundCueAudioPolicy
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
        assertImmediateControl(
            controls = prepare.immediateControls,
            role = WorkoutImmediateControlRole.START_STRENGTH_SET,
            placement = WorkoutImmediateControlPlacement.FIXED_BOTTOM
        )
        assertImmediateControl(
            controls = prepare.immediateControls,
            role = WorkoutImmediateControlRole.PAUSE_SESSION,
            placement = WorkoutImmediateControlPlacement.RHYTHM_SURFACE
        )
        assertImmediateControl(
            controls = prepare.immediateControls,
            role = WorkoutImmediateControlRole.END_SESSION,
            placement = WorkoutImmediateControlPlacement.FIXED_BOTTOM
        )
        assertTrue(prepare.endRequiresConfirmation)

        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        val active = state.toStrengthWorkoutSessionScreenState()
        assertTrue(active.canCompleteSet)
        assertTrue(active.canPause)
        assertTrue(active.canEnd)
        assertImmediateControl(
            controls = active.immediateControls,
            role = WorkoutImmediateControlRole.COMPLETE_STRENGTH_SET,
            placement = WorkoutImmediateControlPlacement.FIXED_BOTTOM
        )
        assertImmediateControl(
            controls = active.immediateControls,
            role = WorkoutImmediateControlRole.PAUSE_SESSION,
            placement = WorkoutImmediateControlPlacement.RHYTHM_SURFACE
        )

        state = StrengthWorkoutEngine.tick(state, seconds = 5).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        val confirm = state.toStrengthWorkoutSessionScreenState()
        assertEquals(SessionStepKind.STRENGTH_CONFIRM_SET, state.currentSessionStep?.kind)
        assertTrue(confirm.canConfirmPlanned)
        assertTrue(requireNotNull(confirm.confirmation).canConfirm)
        assertTrue(confirm.canPause)
        assertTrue(confirm.canEnd)
        assertImmediateControl(
            controls = confirm.immediateControls,
            role = WorkoutImmediateControlRole.CONFIRM_STRENGTH_SET,
            placement = WorkoutImmediateControlPlacement.FIXED_BOTTOM
        )
        assertImmediateControl(
            controls = confirm.immediateControls,
            role = WorkoutImmediateControlRole.END_SESSION,
            placement = WorkoutImmediateControlPlacement.FIXED_BOTTOM
        )
        assertTrue(confirm.endRequiresConfirmation)

        state = StrengthWorkoutEngine.dispatch(
            state,
            WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())
        ).state
        val rest = state.toStrengthWorkoutSessionScreenState()
        assertTrue(rest.canStartNextDuringRest)
        assertTrue(rest.canPause)
        assertTrue(rest.canEnd)
        assertImmediateControl(
            controls = rest.immediateControls,
            role = WorkoutImmediateControlRole.START_NEXT_STRENGTH_SET,
            placement = WorkoutImmediateControlPlacement.FIXED_BOTTOM
        )
        assertImmediateControl(
            controls = rest.immediateControls,
            role = WorkoutImmediateControlRole.PAUSE_SESSION,
            placement = WorkoutImmediateControlPlacement.RHYTHM_SURFACE
        )
    }

    @Test
    fun followAlongExecutionKeepsPauseSkipAndEndImmediatelyReachable() {
        val plan = com.liujyks.trainflow.feature.followalong.buildDefaultFollowAlongScreenState()
            .plans
            .single()
            .plan
        val state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val active = state.toFollowAlongWorkoutSessionUiState()

        assertTrue(active.canPause)
        assertTrue(active.canSkip)
        assertTrue(active.canEnd)
        assertImmediateControl(
            controls = active.immediateControls,
            role = WorkoutImmediateControlRole.PAUSE_SESSION,
            placement = WorkoutImmediateControlPlacement.RHYTHM_SURFACE
        )
        assertImmediateControl(
            controls = active.immediateControls,
            role = WorkoutImmediateControlRole.PAUSE_SESSION,
            placement = WorkoutImmediateControlPlacement.FIXED_BOTTOM
        )
        assertImmediateControl(
            controls = active.immediateControls,
            role = WorkoutImmediateControlRole.SKIP_STEP,
            placement = WorkoutImmediateControlPlacement.FIXED_BOTTOM
        )
        assertImmediateControl(
            controls = active.immediateControls,
            role = WorkoutImmediateControlRole.END_SESSION,
            placement = WorkoutImmediateControlPlacement.FIXED_BOTTOM
        )
        assertTrue(active.endRequiresConfirmation)

        val paused = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession)
            .state
            .toFollowAlongWorkoutSessionUiState()
        assertImmediateControl(
            controls = paused.immediateControls,
            role = WorkoutImmediateControlRole.RESUME_SESSION,
            placement = WorkoutImmediateControlPlacement.RHYTHM_SURFACE
        )
        assertImmediateControl(
            controls = paused.immediateControls,
            role = WorkoutImmediateControlRole.RESUME_SESSION,
            placement = WorkoutImmediateControlPlacement.FIXED_BOTTOM
        )
        assertTrue(paused.endRequiresConfirmation)
    }

    @Test
    fun endWorkoutConfirmationRequiresExplicitConfirmBeforeEndCommand() {
        var confirmation = WorkoutEndConfirmationUiState()

        confirmation = confirmation.request(canEnd = true)
        assertTrue(confirmation.visible)

        confirmation = confirmation.cancel()
        assertFalse(confirmation.visible)
        assertEquals(null, confirmation.confirm(canEnd = true).command)

        confirmation = confirmation.request(canEnd = false)
        assertFalse(confirmation.visible)

        confirmation = confirmation.request(canEnd = true)
        val result = confirmation.confirm(canEnd = true)
        assertFalse(result.nextState.visible)
        assertEquals(WorkoutCommand.EndSession(reason = "user_requested"), result.command)
    }

    @Test
    fun timedRouteWiresEndControlThroughConfirmationDialog() {
        val source = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(source.contains("WorkoutEndConfirmationUiState"))
        assertTrue(source.contains("WorkoutEndConfirmationDialog"))
        assertTrue(source.contains("onRequestEnd"))
        assertTrue(source.contains("result.command?.let(::dispatch)"))
        assertFalse(source.contains("onEnd = { dispatch(WorkoutCommand.EndSession"))
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
        val playerSource = File(
            "src/main/java/com/liujyks/trainflow/core/media/AndroidWorkoutSoundCuePlayer.kt"
        ).readText(Charsets.UTF_8)
        val policy = WorkoutSoundCueAudioPolicy.coexistencePolicy

        assertTrue(playerSource.contains("SoundPool"))
        assertTrue(playerSource.contains("USAGE_ASSISTANCE_SONIFICATION"))
        assertTrue(playerSource.contains("CONTENT_TYPE_SONIFICATION"))
        assertFalse(policy.requestsAudioFocus)
        assertFalse(policy.allowsDucking)
        assertFalse(policy.pausesExternalAudio)
        assertFalse(playerSource.contains("requestAudioFocus"))
        assertFalse(playerSource.contains("AudioFocusRequest"))
        assertFalse(playerSource.contains("AUDIOFOCUS_GAIN"))
        assertFalse(playerSource.contains("AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK"))
        assertFalse(playerSource.contains("setWillPauseWhenDucked"))
        assertFalse(playerSource.contains("adjustStreamVolume"))
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
            immediateControls.map { control -> control.role to control.placement },
            heartRate.statusText,
            heartRate.valueText
        )
    }

    private fun assertImmediateControl(
        controls: List<WorkoutImmediateControlUiState>,
        role: WorkoutImmediateControlRole,
        placement: WorkoutImmediateControlPlacement
    ) {
        assertTrue(
            "Expected enabled $role at $placement in $controls",
            controls.any { control ->
                control.role == role &&
                    control.placement == placement &&
                    control.enabled
            }
        )
    }
}
