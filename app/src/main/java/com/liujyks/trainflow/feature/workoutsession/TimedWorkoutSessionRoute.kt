package com.liujyks.trainflow.feature.workoutsession
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendation
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineResult
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.engine.TimedSessionStepHistoryStatus
import com.liujyks.trainflow.core.media.AndroidWorkoutSoundCuePlayer
import com.liujyks.trainflow.core.media.CountdownReminderFeedbackDispatcher
import com.liujyks.trainflow.core.media.CountdownReminderFeedbackRequest
import com.liujyks.trainflow.core.media.WorkoutSoundCueController
import com.liujyks.trainflow.core.media.WorkoutSoundCueDispatcher
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutEvent
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutSession
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationClearReason
import com.liujyks.trainflow.core.notifications.AndroidActiveWorkoutNotificationController
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowAction
import com.liujyks.trainflow.ui.theme.TrainFlowError
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral200
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral500
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
import com.liujyks.trainflow.ui.designsystem.currentCardCorner
import com.liujyks.trainflow.ui.designsystem.currentPageHorizontalPadding
import com.liujyks.trainflow.ui.designsystem.currentSectionSpacing
import com.liujyks.trainflow.ui.theme.LocalTrainFlowReduceMotion
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin
import com.liujyks.trainflow.ui.theme.isBigType
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
internal fun TimedWorkoutSessionRoute(
    plan: WorkoutPlan,
    onBackToPlans: () -> Unit,
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit,
    onRecordWorkoutSession: suspend (WorkoutSession) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sessionId = remember(plan.id) { "session-${plan.id}-${System.currentTimeMillis()}" }
    var sessionStartedAt by remember(sessionId) { mutableStateOf<Instant?>(null) }
    var recordWriteState by remember(sessionId) { mutableStateOf(TerminalWorkoutSessionRecordWriteState()) }
    var restExtensionInteractionState by remember(sessionId) {
        mutableStateOf(TimedRestExtensionInteractionState())
    }
    var engineState by remember(plan.id, sessionId) {
        mutableStateOf(TimedWorkoutEngine.create(plan, sessionId = sessionId))
    }
    val soundCueController = rememberWorkoutSoundCueController()
    val hapticFeedbackSink = rememberCountdownReminderHapticFeedbackSink()
    val context = LocalContext.current
    val reduceMotion = LocalTrainFlowReduceMotion.current
    val activeWorkoutNotifications = remember(context) {
        AndroidActiveWorkoutNotificationController(context.applicationContext)
    }

    fun applyEngineResult(result: TimedWorkoutEngineResult) {
        engineState = result.state
        if (result.shouldDispatchTimedCountdownReminderFeedback()) {
            result.events.dispatchTimedWorkoutFeedback(
                state = result.state,
                soundCueController = soundCueController,
                hapticFeedbackSink = hapticFeedbackSink
            )
        }
    }

    LaunchedEffect(plan.id, sessionId) {
        while (true) {
            delay(1000)
            if (engineState.shouldTickTimedRouteClock()) {
                applyEngineResult(TimedWorkoutEngine.tick(engineState))
            }
        }
    }

    fun dispatch(command: WorkoutCommand) {
        applyEngineResult(TimedWorkoutEngine.dispatch(engineState, command))
    }

    fun onRestExtensionClick() {
        val nowMillis = System.currentTimeMillis()
        val result = restExtensionInteractionState.onRestExtensionClick(
            engineState = engineState,
            nowMillis = nowMillis
        )
        restExtensionInteractionState = result.state
        if (result.shouldDispatchExtendRest) {
            dispatch(WorkoutCommand.ExtendRest(seconds = TimedRestExtensionSeconds))
        }
    }

    fun startSessionFromReadyGate() {
        if (!engineState.isTimedReadyStartGate()) return

        sessionStartedAt = Instant.now()
        applyEngineResult(engineState.startTimedSessionFromReadyGate())
    }

    val uiState = engineState.toTimedWorkoutSessionScreenState()
    val readyGate = engineState.toTimedReadyStartGateUiState()
    val restExtensionControl = restExtensionInteractionState.toRestExtensionControlUiState(
        engineState = engineState,
        nowMillis = System.currentTimeMillis()
    )
    var endConfirmation by remember { mutableStateOf(WorkoutEndConfirmationUiState()) }
    val notificationState = timedActiveWorkoutNotificationState(
        planId = plan.id,
        status = engineState.status,
        uiState = uiState
    )
    LaunchedEffect(uiState.canEnd) {
        if (!uiState.canEnd) endConfirmation = endConfirmation.cancel()
    }
    LaunchedEffect(engineState.status, engineState.currentStep?.id) {
        restExtensionInteractionState = restExtensionInteractionState.clearForCurrentEngineStep(
            engineState = engineState,
            nowMillis = System.currentTimeMillis()
        )
    }
    LaunchedEffect(
        restExtensionInteractionState.pendingStepId,
        restExtensionInteractionState.pendingStartedAtMillis
    ) {
        if (restExtensionInteractionState.pendingStepId != null) {
            delay(TimedRestExtensionConfirmWindowMillis)
            restExtensionInteractionState = restExtensionInteractionState.clearForCurrentEngineStep(
                engineState = engineState,
                nowMillis = System.currentTimeMillis()
            )
        }
    }
    LaunchedEffect(
        restExtensionInteractionState.successStepId,
        restExtensionInteractionState.successStartedAtMillis
    ) {
        if (restExtensionInteractionState.successStepId != null) {
            delay(TimedRestExtensionSuccessFeedbackMillis)
            restExtensionInteractionState = restExtensionInteractionState.clearForCurrentEngineStep(
                engineState = engineState,
                nowMillis = System.currentTimeMillis()
            )
        }
    }
    LaunchedEffect(notificationState) {
        activeWorkoutNotifications.update(notificationState)
    }
    LaunchedEffect(engineState.status, engineState.sessionId) {
        val startedAt = sessionStartedAt
        if (engineState.shouldRecordTimedTerminalSession(startedAt) && startedAt != null) {
            recordWriteState = recordWriteState.recordTerminalSessionOnce(
                session = engineState.toWorkoutSessionRecord(
                    plan = plan,
                    startedAt = startedAt,
                    endedAt = Instant.now()
                ),
                onRecordWorkoutSession = onRecordWorkoutSession
            )
        }
    }
    DisposableEffect(activeWorkoutNotifications, plan.id) {
        onDispose {
            activeWorkoutNotifications.clear(ActiveWorkoutNotificationClearReason.ROUTE_DISPOSED)
        }
    }

    Crossfade(
        targetState = readyGate,
        animationSpec = timedRouteLocalLayoutTransitionSpec(reduceMotion),
        label = "TimedReadyGateToExecution"
    ) { targetReadyGate ->
        if (targetReadyGate != null) {
            TimedWorkoutReadyStartGateScreen(
                uiState = targetReadyGate,
                onStartSession = ::startSessionFromReadyGate,
                onBackToPlans = onBackToPlans,
                reduceMotion = reduceMotion,
                modifier = modifier
            )
        } else {
            TimedWorkoutSessionScreen(
                uiState = uiState,
                onPause = { dispatch(WorkoutCommand.PauseSession) },
                onResume = { dispatch(WorkoutCommand.ResumeSession) },
                onSkip = { dispatch(WorkoutCommand.SkipStep) },
                restExtensionControl = restExtensionControl,
                onExtendRest = ::onRestExtensionClick,
                showEndConfirmation = endConfirmation.visible,
                onRequestEnd = { endConfirmation = endConfirmation.request(uiState.canEnd) },
                onCancelEnd = { endConfirmation = endConfirmation.cancel() },
                onConfirmEnd = {
                    val result = endConfirmation.confirm(uiState.canEnd)
                    endConfirmation = result.nextState
                    result.command?.let(::dispatch)
                },
                onBackToPlans = onBackToPlans,
                onOpenRecoveryRecommendation = onOpenRecoveryRecommendation,
                reduceMotion = reduceMotion,
                modifier = modifier
            )
        }
    }
}

internal data class TimedReadyStartGateUiState(
    val planTitle: String,
    val estimatedDurationLabel: String,
    val stageCountLabel: String,
    val roundsLabel: String
)

internal fun TimedWorkoutEngineState.isTimedReadyStartGate(): Boolean {
    return status == SessionStatus.READY && currentStep == null
}

internal fun TimedWorkoutEngineState.toTimedReadyStartGateUiState(): TimedReadyStartGateUiState? {
    if (!isTimedReadyStartGate()) return null

    val estimatedDurationSec = steps.sumOf { step -> step.durationSec }
    val roundCount = steps.mapNotNull { step -> step.roundCount }.maxOrNull()

    return TimedReadyStartGateUiState(
        planTitle = planTitle,
        estimatedDurationLabel = estimatedDurationSec.formatReadyDuration(),
        stageCountLabel = "${steps.size} 阶段",
        roundsLabel = roundCount?.let { "$it 轮" } ?: "单轮"
    )
}

internal fun TimedWorkoutEngineState.startTimedSessionFromReadyGate(): TimedWorkoutEngineResult {
    return if (isTimedReadyStartGate()) {
        TimedWorkoutEngine.dispatch(this, WorkoutCommand.StartSession)
    } else {
        TimedWorkoutEngineResult(state = this)
    }
}

internal fun TimedWorkoutEngineState.shouldRecordTimedTerminalSession(startedAt: Instant?): Boolean {
    return isTerminal && startedAt != null
}

internal fun TimedWorkoutEngineResult.shouldDispatchTimedCountdownReminderFeedback(): Boolean {
    return events.isNotEmpty() &&
        (state.status == SessionStatus.ACTIVE || events.any { event -> event is WorkoutEvent.SessionCompleted })
}

@Composable
private fun TimedWorkoutReadyStartGateScreen(
    uiState: TimedReadyStartGateUiState,
    onStartSession: () -> Unit,
    onBackToPlans: () -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(skin.tokens.primary)
            .padding(horizontal = currentPageHorizontalPadding(), vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = uiState.planTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    ),
                    color = TrainFlowNeutral50
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SessionPill(text = uiState.estimatedDurationLabel)
                    SessionPill(text = uiState.stageCountLabel)
                    SessionPill(text = uiState.roundsLabel)
                }
            }

            ReadyStartCenterButton(
                onClick = onStartSession,
                reduceMotion = reduceMotion
            )

            Text(
                text = "点击圆盘开始",
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral200
            )

            OutlinedButton(
                onClick = onBackToPlans,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "返回计划", color = TrainFlowNeutral200)
            }
        }
    }
}

@Composable
private fun ReadyStartCenterButton(
    onClick: () -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = readyStartTouchScaleTarget(
            pressed = pressed,
            reduceMotion = reduceMotion
        ),
        animationSpec = timerDialTouchFeedbackSpec(reduceMotion),
        label = "ReadyStartTouchScale"
    )
    val indication = if (reduceMotion) null else LocalIndication.current
    Surface(
        modifier = modifier
            .size(220.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                contentDescription = "开始计时训练"
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = indication,
                role = Role.Button,
                onClick = onClick
            ),
        shape = CircleShape,
        color = TrainFlowAction,
        border = BorderStroke(1.dp, TrainFlowNeutral50.copy(alpha = 0.16f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            ReadyStartPlayGlyph()
        }
    }
}

@Composable
private fun TimedWorkoutSessionScreen(
    uiState: TimedWorkoutSessionScreenState,
    restExtensionControl: TimedRestExtensionControlUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onExtendRest: () -> Unit,
    showEndConfirmation: Boolean,
    onRequestEnd: () -> Unit,
    onCancelEnd: () -> Unit,
    onConfirmEnd: () -> Unit,
    onBackToPlans: () -> Unit,
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(skin.tokens.primary)
    ) {
        if (uiState.isTerminal) {
            TimedWorkoutTerminalScreen(
                uiState = uiState,
                onBackToPlans = onBackToPlans,
                onOpenRecoveryRecommendation = onOpenRecoveryRecommendation,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            TimedWorkoutExecutionScreen(
                uiState = uiState,
                restExtensionControl = restExtensionControl,
                onPrimaryToggle = if (uiState.canResume) onResume else onPause,
                onResume = onResume,
                onSkip = onSkip,
                onExtendRest = onExtendRest,
                onRequestEnd = onRequestEnd,
                onBackToPlans = onBackToPlans,
                reduceMotion = reduceMotion,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showEndConfirmation) {
            WorkoutEndConfirmationDialog(
                title = "结束本次计时训练？",
                text = "训练会提前结束，已完成的阶段会保留在本次总结和本地记录中。",
                onCancel = onCancelEnd,
                onConfirm = onConfirmEnd
            )
        }
    }
}

@Composable
private fun TimedWorkoutExecutionScreen(
    uiState: TimedWorkoutSessionScreenState,
    restExtensionControl: TimedRestExtensionControlUiState,
    onPrimaryToggle: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onExtendRest: () -> Unit,
    onRequestEnd: () -> Unit,
    onBackToPlans: () -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    val morphProgress by animateFloatAsState(
        targetValue = timerDialPauseMorphTarget(
            isPaused = uiState.isPaused,
            reduceMotion = reduceMotion
        ),
        animationSpec = timerDialPauseMorphSpec(reduceMotion),
        label = "TimerDialPauseMorphProgress"
    )
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxHeight < 840.dp || skin.isBigType
        val totalFontSize = if (compact) 50.sp else 56.sp
        val totalLineHeight = if (compact) 54.sp else 60.sp
        val pausedFontSize = if (compact) 60.sp else 72.sp
        val pausedLineHeight = if (compact) 64.sp else 76.sp
        val pausedTotalFontSize = if (compact) 34.sp else 40.sp
        val pausedTotalLineHeight = if (compact) 38.sp else 44.sp
        val statusBlockHeight = if (compact) 120.dp else 146.dp
        val actionButtonSize = if (compact) 54.dp else 60.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = currentPageHorizontalPadding())
                .padding(top = if (compact) 18.dp else 26.dp, bottom = if (compact) 8.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimedSessionTopBar(uiState = uiState)

            Spacer(modifier = Modifier.height(if (compact) 32.dp else 44.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBlockHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.totalRemainingText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = timerDialRunningLayerAlpha(morphProgress)
                            translationY = -10.dp.toPx() * morphProgress
                        },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = totalFontSize,
                        lineHeight = totalLineHeight,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center
                    ),
                    color = TrainFlowNeutral50.copy(alpha = 0.92f),
                    maxLines = 1
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = timerDialPausedSupportingAlpha(morphProgress)
                            translationY = 14.dp.toPx() * (1f - morphProgress)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp)
                ) {
                    Text(
                        text = uiState.pausedClockText,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = pausedFontSize,
                            lineHeight = pausedLineHeight,
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.Center
                        ),
                        color = TrainFlowNeutral50.copy(alpha = 0.92f),
                        maxLines = 1
                    )
                    Text(
                        text = uiState.totalRemainingText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = pausedTotalFontSize,
                            lineHeight = pausedTotalLineHeight,
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.Center
                        ),
                        color = TrainFlowNeutral200.copy(alpha = 0.82f),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.weight(if (compact) 0.18f else 0.24f))

            TimerDialPauseMorph(
                uiState = uiState,
                onPrimaryToggle = onPrimaryToggle,
                onResume = onResume,
                morphProgress = morphProgress,
                reduceMotion = reduceMotion,
                compact = compact,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))



            Box(modifier = Modifier.fillMaxWidth()) {
                if (!uiState.isPaused || morphProgress < 1f) {
                    TimedSessionControls(
                        uiState = uiState,
                        restExtensionControl = restExtensionControl,
                        onSkip = onSkip,
                        onExtendRest = onExtendRest,
                        onEnd = onRequestEnd,
                        reduceMotion = reduceMotion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = timerDialRunningLayerAlpha(morphProgress)
                                translationY = 10.dp.toPx() * morphProgress
                            },
                        controlsEnabled = !uiState.isPaused,
                        useNavigationBarsPadding = true,
                        horizontalPadding = 0.dp,
                        verticalPadding = if (compact) 6.dp else 8.dp
                    )
                }
                if (uiState.isPaused || morphProgress > 0f) {
                    PausedBottomActionRow(
                        onBackToPlans = onBackToPlans,
                        onRequestEnd = onRequestEnd,
                        buttonSize = actionButtonSize,
                        enabled = uiState.isPaused,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .graphicsLayer {
                                alpha = timerDialPausedSupportingAlpha(morphProgress)
                                translationY = 10.dp.toPx() * (1f - morphProgress)
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimedWorkoutTerminalScreen(
    uiState: TimedWorkoutSessionScreenState,
    onBackToPlans: () -> Unit,
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = currentPageHorizontalPadding())
            .padding(top = if (skin.isBigType) 14.dp else 22.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(currentSectionSpacing())
    ) {
        SessionHeader(uiState)
        MainCountdownPanel(
            uiState = uiState,
            onPrimaryToggle = {}
        )
        TerminalPanel(uiState, onBackToPlans, onOpenRecoveryRecommendation)
    }
}

@Composable
private fun TimerDialPauseMorph(
    uiState: TimedWorkoutSessionScreenState,
    onPrimaryToggle: () -> Unit,
    onResume: () -> Unit,
    morphProgress: Float,
    reduceMotion: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val resumeInteractionSource = remember { MutableInteractionSource() }
    val resumePressed by resumeInteractionSource.collectIsPressedAsState()
    val resumeScale by animateFloatAsState(
        targetValue = timerDialCenterTouchScaleTarget(
            pressed = resumePressed,
            canTogglePause = uiState.canResume,
            reduceMotion = reduceMotion
        ),
        animationSpec = timerDialTouchFeedbackSpec(reduceMotion),
        label = "TimedPausedResumeScale"
    )
    val centerDialSize = if (compact) 184.dp else 232.dp
    val morphDialSize = if (compact) 270.dp else 340.dp
    val centerDialTimeSize = if (compact) 38.sp else 48.sp
    val centerDialTimeLineHeight = if (compact) 42.sp else 52.sp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(morphDialSize),
        contentAlignment = Alignment.Center
    ) {
        TimerDial(
            state = uiState.timerDial,
            onTogglePause = onPrimaryToggle,
            modifier = Modifier.graphicsLayer {
                alpha = timerDialRunningLayerAlpha(morphProgress) * 0.92f
                val ringScale = timerDialRunningLayerScale(morphProgress)
                scaleX = ringScale
                scaleY = ringScale
            }
        )
        if (uiState.isPaused || morphProgress > 0f) {
            PausedResumeCircle(
                uiState = uiState,
                centerDialSize = centerDialSize,
                centerDialTimeSize = centerDialTimeSize,
                centerDialTimeLineHeight = centerDialTimeLineHeight,
                compact = compact,
                resumeScale = resumeScale,
                morphProgress = morphProgress,
                reduceMotion = reduceMotion,
                resumeInteractionSource = resumeInteractionSource,
                onResume = onResume
            )
        }
    }
}

@Composable
private fun TimedSessionTopBar(
    uiState: TimedWorkoutSessionScreenState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uiState.planTitle,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = TrainFlowError,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "WORKOUTS",
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            ),
            color = TrainFlowNeutral50,
            maxLines = 1
        )
    }
}

@Composable
private fun PausedResumeCircle(
    uiState: TimedWorkoutSessionScreenState,
    centerDialSize: Dp,
    centerDialTimeSize: androidx.compose.ui.unit.TextUnit,
    centerDialTimeLineHeight: androidx.compose.ui.unit.TextUnit,
    compact: Boolean,
    resumeScale: Float,
    morphProgress: Float,
    reduceMotion: Boolean,
    resumeInteractionSource: MutableInteractionSource,
    onResume: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(centerDialSize)
            .graphicsLayer {
                val morphScale = timerDialPausedCircleScale(morphProgress)
                scaleX = resumeScale * morphScale
                scaleY = resumeScale * morphScale
                alpha = timerDialPausedCircleAlpha(morphProgress)
            }
            .semantics {
                contentDescription = "继续训练，${uiState.progressLabel}，已进行 ${uiState.currentStageElapsedText}"
                role = Role.Button
            }
            .clickable(
                interactionSource = resumeInteractionSource,
                indication = if (reduceMotion) null else LocalIndication.current,
                enabled = uiState.canResume,
                role = Role.Button,
                onClick = onResume
            ),
        shape = CircleShape,
        color = uiState.stageColorHex.toPausedStageColor(),
        border = BorderStroke(1.dp, TrainFlowNeutral50.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = if (compact) 22.dp else 28.dp)
                .graphicsLayer {
                    alpha = timerDialPausedContentAlpha(morphProgress)
                    translationY = 6.dp.toPx() * (1f - morphProgress)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ReadyStartPlayGlyph(modifier = Modifier.size(if (compact) 40.dp else 52.dp))
            Text(
                text = uiState.timerDial.currentStageIndex.coerceAtLeast(1).toString(),
                modifier = Modifier.padding(top = if (compact) 14.dp else 22.dp),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                ),
                color = TrainFlowNeutral50,
                maxLines = 1
            )
            Text(
                text = uiState.currentStageElapsedText,
                modifier = Modifier.padding(top = if (compact) 4.dp else 8.dp),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = centerDialTimeSize,
                    lineHeight = centerDialTimeLineHeight,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                ),
                color = TrainFlowNeutral50.copy(alpha = 0.86f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PausedBottomActionRow(
    onBackToPlans: () -> Unit,
    onRequestEnd: () -> Unit,
    buttonSize: Dp,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(buttonSize + 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PausedRoundIconButton(
            contentDescription = "返回阶段设定",
            onClick = onBackToPlans,
            contentColor = TrainFlowNeutral50,
            buttonSize = buttonSize,
            enabled = enabled
        ) {
            PausedBackGlyph(
                color = TrainFlowNeutral50,
                modifier = Modifier.size(buttonSize * 0.48f)
            )
        }
        PausedRoundIconButton(
            contentDescription = "结束此次计时训练",
            onClick = onRequestEnd,
            contentColor = TrainFlowError,
            buttonSize = buttonSize,
            enabled = enabled
        ) {
            PausedStopGlyph(
                color = TrainFlowError,
                modifier = Modifier.size(buttonSize * 0.42f)
            )
        }
    }
}

@Composable
private fun PausedRoundIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    contentColor: Color,
    buttonSize: Dp,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .size(buttonSize)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.24f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun PausedBackGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 3.2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.24f, size.height * 0.50f),
            end = Offset(size.width * 0.76f, size.height * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.26f, size.height * 0.50f),
            end = Offset(size.width * 0.46f, size.height * 0.30f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.26f, size.height * 0.50f),
            end = Offset(size.width * 0.46f, size.height * 0.70f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color.copy(alpha = 0.72f),
            start = Offset(size.width * 0.80f, size.height * 0.28f),
            end = Offset(size.width * 0.80f, size.height * 0.72f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PausedStopGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.25f, size.height * 0.25f),
            size = Size(size.width * 0.50f, size.height * 0.50f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
    }
}

private fun String?.toPausedStageColor(): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(this ?: TimedStageType.WORK.defaultColorHex))
    }.getOrElse { TrainFlowAction }
}

@Composable
private fun SessionHeader(uiState: TimedWorkoutSessionScreenState) {
    val skin = LocalTrainFlowSkin.current
    Text(
        text = uiState.totalRemainingText,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = if (skin.isBigType) 62.sp else 54.sp,
            lineHeight = if (skin.isBigType) 64.sp else 56.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        ),
        color = TrainFlowNeutral50
    )
}

@Composable
private fun MainCountdownPanel(
    uiState: TimedWorkoutSessionScreenState,
    onPrimaryToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (skin.isBigType) 10.dp else 12.dp)
    ) {
        TimerDial(
            state = uiState.timerDial,
            onTogglePause = onPrimaryToggle
        )
    }
}

internal fun TimedWorkoutEngineState.shouldTickTimedRouteClock(): Boolean {
    return status == SessionStatus.ACTIVE || status == SessionStatus.PAUSED
}

private fun Int.formatReadyDuration(): String {
    val safeSeconds = coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return when {
        minutes > 0 && seconds > 0 -> "约 ${minutes}分${seconds}秒"
        minutes > 0 -> "约 ${minutes}分钟"
        else -> "约 ${seconds}秒"
    }
}

@Composable
private fun TimedSessionControls(
    uiState: TimedWorkoutSessionScreenState,
    restExtensionControl: TimedRestExtensionControlUiState,
    onSkip: () -> Unit,
    onExtendRest: () -> Unit,
    onEnd: () -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    controlsEnabled: Boolean = true,
    useNavigationBarsPadding: Boolean = true,
    horizontalPadding: Dp = currentPageHorizontalPadding(),
    verticalPadding: Dp = 12.dp
) {
    val skin = LocalTrainFlowSkin.current
    val restExtensionInteractionSource = remember { MutableInteractionSource() }
    val restExtensionPressed by restExtensionInteractionSource.collectIsPressedAsState()
    val restExtensionScale by animateFloatAsState(
        targetValue = timedRestExtensionTouchScaleTarget(
            pressed = restExtensionPressed,
            canExtendRest = uiState.canExtendRest,
            buttonEnabled = restExtensionControl.buttonEnabled,
            reduceMotion = reduceMotion
        ),
        animationSpec = timerDialTouchFeedbackSpec(reduceMotion),
        label = "TimedRestExtensionTouchScale"
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = skin.tokens.primary,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (useNavigationBarsPadding) Modifier.navigationBarsPadding() else Modifier)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimedIconControlButton(
                    icon = TimedControlIcon.SKIP,
                    contentDescription = "跳过当前阶段",
                    onClick = onSkip,
                    enabled = controlsEnabled && uiState.canSkip,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (skin.isBigType) {
                                Modifier.heightIn(min = skin.tokens.secondaryButtonHeightDp.dp)
                            } else {
                                Modifier
                            }
                        ),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedButton(
                    onClick = onExtendRest,
                    enabled = controlsEnabled && uiState.canExtendRest && restExtensionControl.buttonEnabled,
                    interactionSource = restExtensionInteractionSource,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = restExtensionScale
                            scaleY = restExtensionScale
                        }
                        .then(
                            if (skin.isBigType) {
                                Modifier.heightIn(min = skin.tokens.secondaryButtonHeightDp.dp)
                            } else {
                                Modifier
                            }
                        ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Crossfade(
                        targetState = restExtensionControl.buttonLabel,
                        animationSpec = timedRestExtensionStateTransitionSpec(reduceMotion),
                        label = "TimedRestExtensionLabel"
                    ) { buttonLabel ->
                        Text(
                            text = buttonLabel,
                            fontSize = if (skin.isBigType) 17.sp else 14.sp,
                            color = if (controlsEnabled && uiState.canExtendRest && restExtensionControl.buttonEnabled) {
                                TrainFlowNeutral50
                            } else {
                                TrainFlowNeutral500
                            }
                        )
                    }
                }
                TimedIconControlButton(
                    icon = TimedControlIcon.END,
                    contentDescription = "结束训练",
                    onClick = onEnd,
                    enabled = controlsEnabled && uiState.canEnd,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (skin.isBigType) {
                                Modifier.heightIn(min = skin.tokens.secondaryButtonHeightDp.dp)
                            } else {
                                Modifier
                            }
                        )
                )
            }
            restExtensionControl.helperText?.let { helperText ->
                Text(
                    text = helperText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                    color = TrainFlowNeutral200
                )
            }
        }
    }
}

@Composable
private fun TimedIconControlButton(
    icon: TimedControlIcon,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Button
        },
        shape = shape
    ) {
        TimedControlGlyph(
            icon = icon,
            color = if (enabled) {
                when (icon) {
                    TimedControlIcon.END -> TrainFlowError
                    else -> TrainFlowNeutral50
                }
            } else {
                TrainFlowNeutral500
            }
        )
    }
}

@Composable
private fun TimedControlGlyph(
    icon: TimedControlIcon,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(28.dp)) {
        val stroke = 3.dp.toPx()
        when (icon) {
            TimedControlIcon.SKIP -> {
                val first = Path().apply {
                    moveTo(size.width * 0.18f, size.height * 0.22f)
                    lineTo(size.width * 0.50f, size.height * 0.50f)
                    lineTo(size.width * 0.18f, size.height * 0.78f)
                    close()
                }
                val second = Path().apply {
                    moveTo(size.width * 0.48f, size.height * 0.22f)
                    lineTo(size.width * 0.78f, size.height * 0.50f)
                    lineTo(size.width * 0.48f, size.height * 0.78f)
                    close()
                }
                drawPath(first, color)
                drawPath(second, color)
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.84f, size.height * 0.22f),
                    end = Offset(size.width * 0.84f, size.height * 0.78f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
            TimedControlIcon.END -> {
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.24f, size.height * 0.24f),
                    end = Offset(size.width * 0.76f, size.height * 0.76f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.76f, size.height * 0.24f),
                    end = Offset(size.width * 0.24f, size.height * 0.76f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private enum class TimedControlIcon {
    SKIP,
    END
}

@Composable
private fun TimedControlHistoryPanel(uiState: TimedWorkoutSessionScreenState) {
    DarkInfoPanel {
        Text(
            text = uiState.historySummaryLabel,
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral200
        )
        if (uiState.lastControlLabel.isNotBlank()) {
            Text(
                text = uiState.lastControlLabel,
                style = MaterialTheme.typography.bodySmall,
                color = TrainFlowNeutral500
            )
        }
    }
}

@Composable
private fun TerminalPanel(
    uiState: TimedWorkoutSessionScreenState,
    onBackToPlans: () -> Unit,
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit
) {
    val skin = LocalTrainFlowSkin.current
    DarkInfoPanel {
        Text(
            text = uiState.terminalTitle.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            color = TrainFlowNeutral50
        )
        Text(
            text = uiState.terminalSummary.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral200
        )
        uiState.summary?.let { summary ->
            TimedSessionSummaryPanel(summary, onOpenRecoveryRecommendation)
        }
        Button(
            onClick = onBackToPlans,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = skin.tokens.accent)
        ) {
            Text(text = "返回计划", color = skin.tokens.primary)
        }
    }
}

@Composable
private fun TimedSessionSummaryPanel(
    summary: TimedWorkoutSummaryUiState,
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = summary.title,
            style = MaterialTheme.typography.titleMedium,
            color = when (summary.tone) {
                TimedWorkoutSummaryTone.COMPLETED -> TrainFlowAccent
                TimedWorkoutSummaryTone.ABANDONED -> TrainFlowNeutral200
            }
        )
        SummaryMetricGrid(summary.metricItems)
        Text(
            text = summary.durationSemanticsNote,
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral500
        )
        SummaryDetail(label = "跳过内容", text = summary.skippedSummary)
        SummaryDetail(label = "休息延长", text = summary.restExtensionSummary)
        SummaryDetail(label = "结束状态", text = summary.earlyEndSummary)
        SummaryDetail(label = "训练部位", text = summary.trainedAreaSummary)
        RecoveryEntryPanel(summary.recoveryEntry, onOpenRecoveryRecommendation)
    }
}

@Composable
private fun SummaryMetricGrid(items: List<TimedWorkoutSummaryMetricUiState>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    SummaryMetricItem(
                        item = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    DarkMetricSpacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    item: TimedWorkoutSummaryMetricUiState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                color = TrainFlowNeutral200
            )
            Text(
                text = item.value,
                style = MaterialTheme.typography.titleMedium,
                color = TrainFlowNeutral50
            )
            Text(
                text = item.helper,
                style = MaterialTheme.typography.bodySmall,
                color = TrainFlowNeutral500
            )
        }
    }
}

@Composable
private fun DarkMetricSpacer(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.Transparent
    ) {}
}

@Composable
private fun SummaryDetail(
    label: String,
    text: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TrainFlowNeutral200
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral100
        )
    }
}

@Composable
private fun RecoveryEntryPanel(entry: TimedWorkoutRecoveryEntryUiState) {
    RecoveryEntryPanel(entry = entry, onOpenRecoveryRecommendation = {})
}

@Composable
private fun RecoveryEntryPanel(
    entry: TimedWorkoutRecoveryEntryUiState,
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit
) {
    OutlinedButton(
        onClick = {
            entry.recommendation?.let(onOpenRecoveryRecommendation)
        },
        enabled = entry.enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = entry.title,
                color = TrainFlowNeutral50
            )
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = TrainFlowNeutral200
            )
        }
    }
}

@Composable
private fun DarkInfoPanel(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(currentCardCorner()),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content
        )
    }
}

@Composable
private fun SessionPill(
    text: String,
    containerColor: Color = Color.White.copy(alpha = 0.1f),
    contentColor: Color = TrainFlowNeutral50
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}

@Composable
internal fun rememberWorkoutSoundCueController(): WorkoutSoundCueController {
    val context = LocalContext.current
    val soundCuePlayer = remember(context) {
        AndroidWorkoutSoundCuePlayer(context.applicationContext)
    }

    DisposableEffect(soundCuePlayer) {
        onDispose { soundCuePlayer.release() }
    }

    return remember(soundCuePlayer) {
        WorkoutSoundCueController(soundCuePlayer)
    }
}

@Composable
private fun rememberCountdownReminderHapticFeedbackSink(): CountdownReminderHapticFeedbackSink {
    val hapticFeedback = LocalHapticFeedback.current

    return remember(hapticFeedback) {
        AndroidCountdownReminderHapticFeedbackSink(
            hapticFeedback = hapticFeedback
        )
    }
}

private interface CountdownReminderHapticFeedbackSink {
    fun dispatch(request: CountdownReminderFeedbackRequest)
}

private class AndroidCountdownReminderHapticFeedbackSink(
    private val hapticFeedback: HapticFeedback
) : CountdownReminderHapticFeedbackSink {
    override fun dispatch(request: CountdownReminderFeedbackRequest) {
        if (request.vibrationEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

private fun List<WorkoutEvent>.dispatchTimedWorkoutFeedback(
    state: TimedWorkoutEngineState,
    soundCueController: WorkoutSoundCueController,
    hapticFeedbackSink: CountdownReminderHapticFeedbackSink
) {
    forEach { event ->
        val cue = state.soundCueFor(event)
        val soundRequest = WorkoutSoundCueDispatcher.requestFor(event = event, cue = cue)
            ?.takeUnless { event.isInitialTimedStageStart(state) }
        soundCueController.dispatch(soundRequest)
        val hapticRequest = CountdownReminderFeedbackDispatcher.requestFor(event = event, cue = cue)
        if (hapticRequest != null) {
            hapticFeedbackSink.dispatch(hapticRequest)
        }
    }
}

private fun WorkoutEvent.isInitialTimedStageStart(state: TimedWorkoutEngineState): Boolean {
    return state.activeElapsedSec == 0 &&
        (this is WorkoutEvent.TimedWorkStarted || this is WorkoutEvent.RestStarted)
}

internal fun TimedWorkoutEngineState.soundCueFor(event: WorkoutEvent) = when (event) {
    is WorkoutEvent.TimedWorkStarted -> completedPreviousStepCueFor(startedStepId = event.stepId)
    is WorkoutEvent.TimedWorkEnding -> steps.firstOrNull { step -> step.id == event.stepId }?.endingCue
    is WorkoutEvent.RestStarted -> completedPreviousStepCueFor(startedStepId = event.stepId)
    is WorkoutEvent.RestEnding -> steps.firstOrNull { step -> step.id == event.stepId }?.endingCue
    is WorkoutEvent.SessionCompleted -> completedFinalStepCue()
    else -> null
}

private fun TimedWorkoutEngineState.completedPreviousStepCueFor(startedStepId: String) =
    steps.indexOfFirst { step -> step.id == startedStepId }
        .takeIf { index -> index > 0 }
        ?.let { startedIndex -> steps[startedIndex - 1] }
        ?.let { previousStep ->
            val previousStepHistory = stepHistory.lastOrNull { record -> record.stepId == previousStep.id }
            previousStep.endingCue.takeIf {
                previousStepHistory?.status == TimedSessionStepHistoryStatus.COMPLETED
            }
        }

private fun TimedWorkoutEngineState.completedFinalStepCue() = steps.lastOrNull()?.let { finalStep ->
    val finalStepHistory = stepHistory.lastOrNull { record -> record.stepId == finalStep.id }
    finalStep.endingCue.takeIf {
        status == SessionStatus.COMPLETED &&
            finalStepHistory?.status == TimedSessionStepHistoryStatus.COMPLETED
    }
}

private val TimedWorkoutCountdownReminderType.label: String
    get() = when (this) {
        TimedWorkoutCountdownReminderType.ACTION_ENDING -> "阶段提醒"
        TimedWorkoutCountdownReminderType.REST_ENDING -> "休息提醒"
        TimedWorkoutCountdownReminderType.NONE -> ""
    }

@Preview(showBackground = true)
@Composable
private fun TimedWorkoutSessionRoutePreview() {
    TrainFlowTheme {
        TimedWorkoutSessionRoute(
            plan = buildDefaultPlanManagementState().plans.first(),
            onBackToPlans = {},
            onOpenRecoveryRecommendation = {}
        )
    }
}

@Composable
private fun ReadyStartPlayGlyph(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(58.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.34f, size.height * 0.18f)
            lineTo(size.width * 0.34f, size.height * 0.82f)
            lineTo(size.width * 0.82f, size.height * 0.50f)
            close()
        }
        drawPath(path, TrainFlowNeutral50)
    }
}
