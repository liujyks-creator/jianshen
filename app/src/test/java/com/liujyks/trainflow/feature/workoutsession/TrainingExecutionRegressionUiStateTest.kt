package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.ui.unit.dp
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
    fun strengthConfirmRecordCollapsesCurrentSetSummaryOnlyInConfirmStep() {
        val plan = buildDefaultPlanManagementState().plans[1]
        var state = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val prepare = state.toStrengthWorkoutSessionScreenState()
        assertFalse(prepare.isCurrentSetSummaryCollapsed)

        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.StartStrengthSet()).state
        val active = state.toStrengthWorkoutSessionScreenState()
        assertFalse(active.isCurrentSetSummaryCollapsed)

        state = StrengthWorkoutEngine.tick(state, seconds = 5).state
        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.CompleteStrengthSet()).state
        val confirm = state.toStrengthWorkoutSessionScreenState()
        assertEquals(SessionStepKind.STRENGTH_CONFIRM_SET, state.currentSessionStep?.kind)
        assertTrue(confirm.isCurrentSetSummaryCollapsed)
        assertTrue(confirm.collapsedCurrentSetStatusLabel.contains("待确认记录"))
        assertTrue(confirm.collapsedCurrentSetStatusLabel.contains("完成耗时 00:05"))
        assertTrue(confirm.currentExerciseName.isNotBlank())
        assertTrue(confirm.setKindLabel.isNotBlank())
        assertTrue(confirm.setProgressLabel.contains("第 1 /"))

        val pausedConfirm = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession)
            .state
            .toStrengthWorkoutSessionScreenState()
        assertTrue(pausedConfirm.isCurrentSetSummaryCollapsed)
        assertTrue(pausedConfirm.collapsedCurrentSetStatusLabel.contains("已暂停"))

        state = StrengthWorkoutEngine.dispatch(state, WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput()))
            .state
        val rest = state.toStrengthWorkoutSessionScreenState()
        assertFalse(rest.isCurrentSetSummaryCollapsed)
        assertTrue(rest.collapsedCurrentSetStatusLabel.isBlank())
    }

    @Test
    fun strengthConfirmRecordCompactPanelDoesNotRenderLargeMetricProgressOrLongCue() {
        val routeSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/StrengthWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)
        val collapsedPanelSource = routeSource
            .substringAfter("private fun StrengthCollapsedCurrentSetPanel")
            .substringBefore("private fun StrengthSetConfirmationPanel")

        assertTrue(routeSource.contains("StrengthCollapsedCurrentSetPanel("))
        assertTrue(collapsedPanelSource.contains("uiState.collapsedCurrentSetStatusLabel"))
        assertTrue(collapsedPanelSource.contains("uiState.currentExerciseName"))
        assertTrue(collapsedPanelSource.contains("uiState.setProgressLabel"))
        assertTrue(collapsedPanelSource.contains("uiState.setKindLabel"))
        assertFalse(collapsedPanelSource.contains("LinearProgressIndicator"))
        assertFalse(collapsedPanelSource.contains("uiState.primaryMetricText"))
        assertFalse(collapsedPanelSource.contains("uiState.shortCue"))
    }

    @Test
    fun strengthConfirmRecordPrioritizesEffortChoicesBeforeActualInputsAndRepShortcuts() {
        val routeSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/StrengthWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)
        val confirmationPanelSource = routeSource
            .substringAfter("private fun StrengthSetConfirmationPanel")
            .substringBefore("private fun StrengthActualWeightField")
        val actualWeightIndex = confirmationPanelSource.indexOf("StrengthActualWeightField(")
        val actualRepsIndex = confirmationPanelSource.indexOf("StrengthActualRepsField(")
        val effortIndex = confirmationPanelSource.indexOf("confirmation.effortOptions")
        val repShortcutIndex = confirmationPanelSource.indexOf("confirmation.repQuickOptions")

        assertTrue(actualWeightIndex >= 0)
        assertTrue(actualRepsIndex >= 0)
        assertTrue(effortIndex >= 0)
        assertTrue(repShortcutIndex >= 0)
        assertTrue(effortIndex < actualWeightIndex)
        assertTrue(effortIndex < actualRepsIndex)
        assertTrue(effortIndex < repShortcutIndex)
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
    fun timedPausedMorphKeepsResumePlanTitleAndBottomActionSemanticsWithoutHeartRateUi() {
        val source = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(source.contains("TimedSessionTopBar(uiState = uiState)"))
        assertTrue(source.contains("text = uiState.planTitle"))
        assertFalse(source.contains("text = \"WORKOUTS\""))
        assertTrue(source.contains("timedExecutionLayoutSpec(compact)"))
        assertTrue(source.contains("layoutSpec.topElasticWeight"))
        assertTrue(source.contains("layoutSpec.bottomElasticWeight"))
        assertTrue(source.contains("TimerDialPauseMorph("))
        assertTrue(source.contains("timerDialLayoutSpec.dialSizeDp.dp"))
        assertTrue(source.contains("PausedResumeCircle("))
        assertTrue(source.contains("contentDescription = \"继续训练"))
        assertFalse(source.contains("HeartRatePanel("))
        assertFalse(source.contains("heartRate = uiState.heartRate"))
        assertTrue(source.contains("PausedBottomActionRow("))
        assertTrue(source.contains("contentDescription = \"返回阶段设定\""))
        assertTrue(source.contains("contentDescription = \"结束此次计时训练\""))

        val timerDialSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDial.kt"
        ).readText(Charsets.UTF_8)
        assertTrue(timerDialSource.contains("Modifier.requiredSize(dialSize)"))
    }

    @Test
    fun timedRestExtensionControlKeepsBottomButtonsStableDuringConfirmation() {
        val routeSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)
        val interactionSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedRestExtensionInteractionState.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(routeSource.contains("val controlHeight"))
        assertTrue(routeSource.contains(".height(controlHeight)"))
        assertTrue(routeSource.contains("targetState = restExtensionControl.buttonLabel"))
        assertTrue(routeSource.contains("maxLines = 1"))
        assertTrue(routeSource.contains("softWrap = false"))
        assertTrue(routeSource.contains("textAlign = TextAlign.Center"))
        assertTrue(routeSource.contains("contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)"))
        assertTrue(interactionSource.contains("buttonLabel = \"确认+15s\""))
        assertTrue(interactionSource.contains("buttonLabel = \"+15s\""))
        assertFalse(interactionSource.contains("+15秒"))
    }

    @Test
    fun timedExecutionLayoutSpecKeepsTimerDialPrimaryAfterHeartRateRemoval() {
        val compact = timedExecutionLayoutSpec(compact = true)
        val regular = timedExecutionLayoutSpec(compact = false)

        assertTrue(compact.titleToTotalSpacer <= 18.dp)
        assertTrue(compact.totalRemainingBlockHeight <= 90.dp)
        assertTrue(compact.topElasticWeight < compact.bottomElasticWeight)
        assertEquals(48.dp, compact.controlButtonMinHeight)

        assertTrue(regular.titleToTotalSpacer <= 26.dp)
        assertTrue(regular.totalRemainingBlockHeight <= 104.dp)
        assertTrue(regular.topElasticWeight < regular.bottomElasticWeight)
        assertEquals(48.dp, regular.controlButtonMinHeight)
    }

    @Test
    fun sharedExecutionBottomControlsReserveProtectsFollowAlongAndStrengthContent() {
        val specSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TrainingExecutionBottomControlsSpec.kt"
        ).readText(Charsets.UTF_8)
        val followAlongSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/FollowAlongWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)
        val strengthSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/StrengthWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(specSource.contains("primaryButtonMinHeight = maxOf(48.dp"))
        assertTrue(specSource.contains("secondaryButtonMinHeight = maxOf(48.dp"))
        assertTrue(specSource.contains("bottomSafeAreaReserve = 32.dp"))
        assertTrue(specSource.contains("fixedBottomContentReserve"))

        listOf(followAlongSource, strengthSource).forEach { source ->
            assertTrue(source.contains("trainingExecutionBottomControlsSpec()"))
            assertTrue(source.contains("bottomControlsSpec.fixedBottomContentReserve"))
            assertTrue(source.contains(".navigationBarsPadding()"))
            assertTrue(source.contains("controlsSpec.primaryButtonMinHeight"))
            assertTrue(source.contains("controlsSpec.secondaryButtonMinHeight"))
            assertTrue(source.contains("controlsSpec.verticalPadding"))
            assertTrue(source.contains("controlsSpec.rowSpacing"))
            assertFalse(source.contains("bottom = if (uiState.isTerminal) 22.dp else skin.tokens.executionControlReserveDp.dp"))
        }

        assertFalse(strengthSource.contains("if (skin.isBigType) {\n                            Modifier.heightIn"))
    }

    @Test
    fun timedReadyRunningPausedRestAndRestExtensionStatesStayWithinExistingSemantics() {
        val plan = buildDefaultPlanManagementState().plans.first()
        var state = TimedWorkoutEngine.create(plan)
        assertTrue(state.isTimedReadyStartGate())
        assertEquals("准备开始", state.toTimedWorkoutSessionScreenState().currentTitle)

        state = state.startTimedSessionFromReadyGate().state
        val running = state.toTimedWorkoutSessionScreenState()
        assertTrue(running.canPause)
        assertTrue(running.canSkip)
        assertFalse(running.canExtendRest)

        val paused = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession)
            .state
            .toTimedWorkoutSessionScreenState()
        assertTrue(paused.canResume)
        assertFalse(paused.canExtendRest)

        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        val rest = state.toTimedWorkoutSessionScreenState()
        assertTrue(rest.canExtendRest)
        assertTrue(rest.canPause)
        assertTrue(rest.canEnd)

        val interactionSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedRestExtensionInteractionState.kt"
        ).readText(Charsets.UTF_8)
        assertTrue(interactionSource.contains("buttonLabel = \"确认+15s\""))
        assertTrue(interactionSource.contains("buttonLabel = \"+15s\""))
    }

    @Test
    fun timedTerminalCompletedStateUsesDedicatedRecapPageWithoutLargeTimerDial() {
        val routeSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)
        val shellSource = File(
            "src/main/java/com/liujyks/trainflow/ui/shell/official/TrainFlowApp.kt"
        ).readText(Charsets.UTF_8)
        val recapScreenSource = routeSource
            .substringAfter("private fun TimedWorkoutCompletionRecapScreen")
            .substringBefore("private fun TimerDialPauseMorph")

        assertTrue(routeSource.contains("TimedWorkoutCompletionRecapScreen("))
        assertTrue(routeSource.contains("onReturnToTrainingHome"))
        assertTrue(routeSource.contains("CompletionRecapHero("))
        assertTrue(routeSource.contains("text = \"本次复盘\""))
        assertTrue(routeSource.contains("text = \"返回训练首页\""))
        assertTrue(shellSource.contains("onReturnToTrainingHome = {"))
        assertTrue(shellSource.contains(".selectDestination(OfficialShellDestination.TRAINING)"))
        assertTrue(routeSource.contains("TimedRecapKeyMetrics(summary = uiState.summary)"))
        assertTrue(routeSource.contains("TimedSessionSummaryPanel("))
        assertTrue(routeSource.contains("showMetrics = false"))
        assertFalse(routeSource.contains("private fun TimedWorkoutTerminalScreen"))
        assertFalse(routeSource.contains("private fun MainCountdownPanel"))
        assertFalse(recapScreenSource.contains("TimerDial("))
    }

    @Test
    fun timedCompletionRecapKeepsSummaryDetailsAndAbandonedToneSeparated() {
        val routeSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)
        val heroSource = routeSource
            .substringAfter("private fun CompletionRecapHero")
            .substringBefore("private fun CompletionRecapBadge")

        assertTrue(routeSource.contains("summary.metricItems.take(4)"))
        assertTrue(routeSource.contains("SummaryDetail(label = \"跳过内容\", text = summary.skippedSummary)"))
        assertTrue(routeSource.contains("SummaryDetail(label = \"休息延长\", text = summary.restExtensionSummary)"))
        assertTrue(routeSource.contains("SummaryDetail(label = \"结束状态\", text = summary.earlyEndSummary)"))
        assertTrue(routeSource.contains("text = uiState.terminalSummary.orEmpty()"))
        assertTrue(heroSource.contains("val showCelebration = isCompleted"))
        assertTrue(heroSource.contains("val statusLabel = if (isCompleted) \"已完成\" else \"已结束\""))
        assertTrue(heroSource.contains("本次训练已提前结束"))
        assertFalse(heroSource.contains("已完成\" else \"已完成"))
    }

    @Test
    fun strengthTerminalReturnActionIsStickyAndNotInsideRecapPanel() {
        val routeSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/StrengthWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)
        val screenSource = routeSource
            .substringAfter("private fun StrengthWorkoutSessionScreen")
            .substringBefore("@Composable\nprivate fun StrengthSessionHeader")
        val terminalPanelSource = routeSource
            .substringAfter("private fun StrengthTerminalPanel")
            .substringBefore("@Composable\nprivate fun StrengthTerminalReturnAction")
        val returnActionSource = routeSource
            .substringAfter("private fun StrengthTerminalReturnAction")
            .substringBefore("@Composable\nprivate fun StrengthSessionSummaryPanel")

        assertTrue(routeSource.contains("StrengthTerminalReturnAction("))
        assertTrue(screenSource.contains("bottom = bottomControlsSpec.fixedBottomContentReserve"))
        assertTrue(returnActionSource.contains(".navigationBarsPadding()"))
        assertTrue(returnActionSource.contains("controlsSpec.primaryButtonMinHeight"))
        assertTrue(returnActionSource.contains("text = \"返回计划\""))
        assertFalse(terminalPanelSource.contains("Button("))
        assertFalse(terminalPanelSource.contains("onBackToPlans"))
        assertFalse(terminalPanelSource.contains("text = \"返回计划\""))
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
        assertTrue(playerSource.contains("USAGE_MEDIA"))
        assertTrue(playerSource.contains("CONTENT_TYPE_MUSIC"))
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
            canEnd
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
            immediateControls.map { control -> control.role to control.placement }
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
