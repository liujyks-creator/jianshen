package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendation
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngineResult
import com.liujyks.trainflow.core.media.WorkoutSoundCueController
import com.liujyks.trainflow.core.media.WorkoutSoundCueDispatcher
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.SessionStatus
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
import com.liujyks.trainflow.ui.theme.TrainFlowSecondary
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
import com.liujyks.trainflow.ui.designsystem.currentCardCorner
import com.liujyks.trainflow.ui.designsystem.currentPageHorizontalPadding
import com.liujyks.trainflow.ui.designsystem.currentProminentCardCorner
import com.liujyks.trainflow.ui.designsystem.currentSectionSpacing
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin
import com.liujyks.trainflow.ui.theme.isBigType
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
internal fun StrengthWorkoutSessionRoute(
    plan: WorkoutPlan,
    onBackToPlans: () -> Unit,
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit,
    onRecordWorkoutSession: suspend (WorkoutSession) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sessionId = remember(plan.id) { "session-${plan.id}-${System.currentTimeMillis()}" }
    val sessionStartedAt = remember(sessionId) { Instant.now() }
    var recordWriteState by remember(sessionId) { mutableStateOf(TerminalWorkoutSessionRecordWriteState()) }
    var engineState by remember(plan.id, sessionId) {
        mutableStateOf(StrengthWorkoutEngine.create(plan, sessionId = sessionId))
    }
    val soundCueController = rememberWorkoutSoundCueController()
    val context = LocalContext.current
    val activeWorkoutNotifications = remember(context) {
        AndroidActiveWorkoutNotificationController(context.applicationContext)
    }

    fun applyEngineResult(result: StrengthWorkoutEngineResult) {
        engineState = result.state
        result.events.dispatchStrengthWorkoutSoundCues(
            cueSettings = plan.preferences?.cueSettings,
            soundCueController = soundCueController
        )
    }

    LaunchedEffect(plan.id) {
        applyEngineResult(StrengthWorkoutEngine.dispatch(engineState, WorkoutCommand.StartSession))
        while (true) {
            delay(1000)
            if (engineState.status == SessionStatus.ACTIVE || engineState.status == SessionStatus.PAUSED) {
                applyEngineResult(StrengthWorkoutEngine.tick(engineState))
            }
        }
    }

    fun dispatch(command: WorkoutCommand) {
        applyEngineResult(StrengthWorkoutEngine.dispatch(engineState, command))
    }

    val uiState = engineState.toStrengthWorkoutSessionScreenState()
    val notificationState = strengthActiveWorkoutNotificationState(
        planId = plan.id,
        status = engineState.status,
        uiState = uiState
    )
    LaunchedEffect(notificationState) {
        activeWorkoutNotifications.update(notificationState)
    }
    LaunchedEffect(engineState.status, engineState.sessionId) {
        if (engineState.isTerminal) {
            recordWriteState = recordWriteState.recordTerminalSessionOnce(
                session = engineState.toWorkoutSessionRecord(
                    plan = plan,
                    startedAt = sessionStartedAt,
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
    var confirmationInput by remember { mutableStateOf<StrengthSetConfirmationInputState?>(null) }
    LaunchedEffect(uiState.confirmation?.setKey) {
        confirmationInput = uiState.confirmation?.initialInputState()
    }
    val activeConfirmationInput = uiState.confirmation?.let { confirmation ->
        confirmationInput ?: confirmation.initialInputState()
    }
    var showReplacementOptions by remember { mutableStateOf(false) }
    var showSkipConfirmation by remember { mutableStateOf(false) }
    var endConfirmation by remember { mutableStateOf(WorkoutEndConfirmationUiState()) }
    LaunchedEffect(uiState.canReplaceExercise, uiState.canSkipExercise, uiState.currentExerciseName) {
        if (!uiState.canReplaceExercise) showReplacementOptions = false
        if (!uiState.canSkipExercise) showSkipConfirmation = false
    }
    LaunchedEffect(uiState.canEnd) {
        if (!uiState.canEnd) endConfirmation = endConfirmation.cancel()
    }

    StrengthWorkoutSessionScreen(
        uiState = uiState,
        confirmationInput = activeConfirmationInput,
        onConfirmationInputChange = { input -> confirmationInput = input },
        showReplacementOptions = showReplacementOptions,
        onToggleReplacementOptions = {
            showReplacementOptions = !showReplacementOptions
            showSkipConfirmation = false
        },
        onReplaceExercise = { exerciseId ->
            engineState.currentReplaceExerciseCommand(exerciseId)?.let(::dispatch)
            showReplacementOptions = false
        },
        showSkipConfirmation = showSkipConfirmation,
        onRequestSkipExercise = {
            showSkipConfirmation = true
            showReplacementOptions = false
        },
        onCancelSkipExercise = { showSkipConfirmation = false },
        onConfirmSkipExercise = {
            engineState.currentSkipExerciseCommand()?.let(::dispatch)
            showSkipConfirmation = false
        },
        onStartSet = {
            dispatch(WorkoutCommand.StartStrengthSet(engineState.currentSet?.setPlanId))
        },
        onCompleteSet = { dispatch(WorkoutCommand.CompleteStrengthSet()) },
        onConfirmSet = {
            val confirmation = uiState.confirmation
            val input = confirmation?.let { activeConfirmationInput?.validateFor(it)?.commandInput }
            if (input != null) {
                dispatch(WorkoutCommand.ConfirmStrengthSet(input))
            }
        },
        onStartNextDuringRest = { dispatch(WorkoutCommand.StartStrengthSet()) },
        onPause = { dispatch(WorkoutCommand.PauseSession) },
        onResume = { dispatch(WorkoutCommand.ResumeSession) },
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
        modifier = modifier
    )
}

private fun List<WorkoutEvent>.dispatchStrengthWorkoutSoundCues(
    cueSettings: CueSettings?,
    soundCueController: WorkoutSoundCueController
) {
    forEach { event ->
        val cue = WorkoutSoundCueDispatcher.cueFor(event = event, cueSettings = cueSettings)
        soundCueController.dispatch(WorkoutSoundCueDispatcher.requestFor(event = event, cue = cue))
    }
}

@Composable
private fun StrengthWorkoutSessionScreen(
    uiState: StrengthWorkoutSessionScreenState,
    confirmationInput: StrengthSetConfirmationInputState?,
    onConfirmationInputChange: (StrengthSetConfirmationInputState) -> Unit,
    showReplacementOptions: Boolean,
    onToggleReplacementOptions: () -> Unit,
    onReplaceExercise: (String) -> Unit,
    showSkipConfirmation: Boolean,
    onRequestSkipExercise: () -> Unit,
    onCancelSkipExercise: () -> Unit,
    onConfirmSkipExercise: () -> Unit,
    onStartSet: () -> Unit,
    onCompleteSet: () -> Unit,
    onConfirmSet: () -> Unit,
    onStartNextDuringRest: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    showEndConfirmation: Boolean,
    onRequestEnd: () -> Unit,
    onCancelEnd: () -> Unit,
    onConfirmEnd: () -> Unit,
    onBackToPlans: () -> Unit,
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(skin.tokens.primary)
    ) {
        val confirmationValidation = uiState.confirmation?.let { confirmation ->
            confirmationInput?.validateFor(confirmation)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = currentPageHorizontalPadding())
                .padding(
                    top = if (skin.isBigType) 14.dp else 22.dp,
                    bottom = if (uiState.isTerminal) {
                        22.dp
                    } else {
                        skin.tokens.executionControlReserveDp.dp
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(currentSectionSpacing())
        ) {
            StrengthSessionHeader(uiState)
            StrengthMainPanel(
                uiState = uiState,
                onPrimaryToggle = if (uiState.canResume) onResume else onPause
            )
            if (uiState.confirmation != null && confirmationInput != null && confirmationValidation != null) {
                StrengthSetConfirmationPanel(
                    confirmation = uiState.confirmation,
                    input = confirmationInput,
                    validation = confirmationValidation,
                    onInputChange = onConfirmationInputChange
                )
            }
            StrengthNextSetPanel(uiState)
            StrengthExerciseAdjustmentPanel(
                uiState = uiState,
                showReplacementOptions = showReplacementOptions,
                onToggleReplacementOptions = onToggleReplacementOptions,
                onReplaceExercise = onReplaceExercise,
                showSkipConfirmation = showSkipConfirmation,
                onRequestSkipExercise = onRequestSkipExercise,
                onCancelSkipExercise = onCancelSkipExercise,
                onConfirmSkipExercise = onConfirmSkipExercise
            )
            StrengthHeartRatePanel(uiState.heartRate)

            if (uiState.isTerminal) {
                StrengthTerminalPanel(uiState, onBackToPlans, onOpenRecoveryRecommendation)
            } else if (!skin.isBigType) {
                StrengthSecondaryControlsPanel(
                    uiState = uiState,
                    onPause = onPause,
                    onResume = onResume,
                    onEnd = onRequestEnd
                )
                StrengthControlHistoryPanel(uiState)
            }
        }

        if (!uiState.isTerminal) {
            StrengthSessionControls(
                uiState = uiState,
                confirmationValidation = confirmationValidation,
                onStartSet = onStartSet,
                onCompleteSet = onCompleteSet,
                onConfirmSet = onConfirmSet,
                onStartNextDuringRest = onStartNextDuringRest,
                onPause = onPause,
                onResume = onResume,
                onEnd = onRequestEnd,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        if (showEndConfirmation) {
            WorkoutEndConfirmationDialog(
                title = "结束本次力量训练？",
                text = "已确认的组记录会保留，未完成内容会作为提前结束处理。",
                onCancel = onCancelEnd,
                onConfirm = onConfirmEnd
            )
        }
    }
}

@Composable
private fun StrengthSessionHeader(uiState: StrengthWorkoutSessionScreenState) {
    val skin = LocalTrainFlowSkin.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = uiState.planTitle,
                style = if (skin.isBigType) {
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = 26.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = TrainFlowNeutral50
            )
            Text(
                text = "${uiState.completedSetCount} / ${uiState.totalSetCount} 组已确认",
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral200
            )
        }
        StrengthSessionPill(text = uiState.statusLabel)
    }
}

@Composable
private fun StrengthMainPanel(
    uiState: StrengthWorkoutSessionScreenState,
    onPrimaryToggle: () -> Unit
) {
    val skin = LocalTrainFlowSkin.current
    val isRest = uiState.phaseLabel == "休息"
    val isConfirm = uiState.canConfirmPlanned
    val panelColor = when {
        isConfirm -> skin.tokens.action.copy(alpha = 0.14f)
        isRest -> skin.tokens.accent.copy(alpha = 0.14f)
        else -> skin.tokens.secondary
    }
    val borderColor = when {
        isConfirm -> skin.tokens.action.copy(alpha = 0.55f)
        isRest -> skin.tokens.accent.copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.08f)
    }
    val metricColor = when {
        uiState.canCompleteSet || isConfirm -> skin.tokens.action
        isRest -> skin.tokens.accent
        else -> TrainFlowNeutral50
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = uiState.canPause || uiState.canResume) {
                onPrimaryToggle()
            },
        shape = RoundedCornerShape(currentProminentCardCorner()),
        colors = CardDefaults.cardColors(containerColor = panelColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(skin.tokens.executionPanelPaddingDp.dp),
            verticalArrangement = Arrangement.spacedBy(if (skin.isBigType) 12.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StrengthSessionPill(
                    text = uiState.phaseLabel,
                    containerColor = if (isRest) skin.tokens.accent else skin.tokens.action,
                    contentColor = skin.tokens.primary
                )
                if (uiState.setKindLabel.isNotBlank()) {
                    StrengthSessionPill(text = uiState.setKindLabel)
                }
            }
            Text(
                text = uiState.currentExerciseName,
                style = if (skin.isBigType) {
                    MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                color = TrainFlowNeutral50
            )
            Text(
                text = uiState.setProgressLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral200
            )
            if (uiState.isTerminal) {
                Text(
                    text = "${uiState.completedSetCount} / ${uiState.totalSetCount} 组已确认",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrainFlowNeutral200
                )
                LinearProgressIndicator(
                    progress = { uiState.progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = skin.tokens.accent,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
            } else {
                Text(
                    text = uiState.primaryMetricLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TrainFlowNeutral200
                )
                Text(
                    text = uiState.primaryMetricText,
                    fontSize = if (uiState.canStartSet) {
                        (30f * skin.tokens.fontScale).sp
                    } else {
                        (72f * skin.tokens.timerScale).sp
                    },
                    lineHeight = if (uiState.canStartSet) {
                        (36f * skin.tokens.fontScale).sp
                    } else {
                        (74f * skin.tokens.timerScale).sp
                    },
                    fontWeight = FontWeight.ExtraBold,
                    color = metricColor
                )
                LinearProgressIndicator(
                    progress = { uiState.progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isRest) skin.tokens.accent else skin.tokens.action,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
                if (!skin.isBigType || !uiState.canStartSet) {
                    Text(
                        text = uiState.targetSummary,
                        style = if (skin.isBigType) {
                            MaterialTheme.typography.titleLarge.copy(
                                fontSize = 24.sp,
                                lineHeight = 29.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        color = TrainFlowNeutral50
                    )
                }
            }
            if (!skin.isBigType) {
                Text(
                    text = uiState.shortCue,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TrainFlowNeutral100
                )
            }
        }
    }
}

@Composable
private fun StrengthSetConfirmationPanel(
    confirmation: StrengthSetConfirmationUiState,
    input: StrengthSetConfirmationInputState,
    validation: StrengthSetConfirmationValidation,
    onInputChange: (StrengthSetConfirmationInputState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(currentProminentCardCorner()),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, TrainFlowAction.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "确认本组",
                        style = MaterialTheme.typography.titleLarge,
                        color = TrainFlowNeutral50
                    )
                    Text(
                        text = "${confirmation.exerciseName} · ${confirmation.setProgressLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TrainFlowNeutral200
                    )
                }
                StrengthSessionPill(text = confirmation.setKindLabel)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StrengthConfirmationFact(
                    label = "计划重量",
                    value = confirmation.plannedWeightLabel,
                    modifier = Modifier.weight(1f)
                )
                StrengthConfirmationFact(
                    label = "计划次数",
                    value = confirmation.plannedRepLabel,
                    modifier = Modifier.weight(1f)
                )
                StrengthConfirmationFact(
                    label = "本组耗时",
                    value = confirmation.activeDurationLabel,
                    modifier = Modifier.weight(1f)
                )
            }

            if (LocalTrainFlowSkin.current.isBigType) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StrengthActualWeightField(
                        confirmation = confirmation,
                        input = input,
                        onInputChange = onInputChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StrengthActualRepsField(
                        input = input,
                        onInputChange = onInputChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StrengthActualWeightField(
                        confirmation = confirmation,
                        input = input,
                        onInputChange = onInputChange,
                        modifier = Modifier.weight(1f)
                    )
                    StrengthActualRepsField(
                        input = input,
                        onInputChange = onInputChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (confirmation.repQuickOptions.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    confirmation.repQuickOptions.forEach { reps ->
                        val selected = input.actualRepsInput == reps.toString()
                        OutlinedButton(
                            onClick = { onInputChange(input.copy(actualRepsInput = reps.toString())) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selected) TrainFlowAccent else Color.White.copy(alpha = 0.22f)
                            )
                        ) {
                            Text(
                                text = reps.toString(),
                                color = if (selected) TrainFlowAccent else TrainFlowNeutral50
                            )
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                confirmation.effortOptions.forEach { option ->
                    val selected = input.selectedEffort == option.effort
                    OutlinedButton(
                        onClick = { onInputChange(input.copy(selectedEffort = option.effort)) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) TrainFlowAccent.copy(alpha = 0.18f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) TrainFlowAccent else Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = option.label,
                            color = if (selected) TrainFlowAccent else TrainFlowNeutral50
                        )
                    }
                }
            }

            validation.errorText?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = TrainFlowError
                )
            }
        }
    }
}

@Composable
private fun StrengthActualWeightField(
    confirmation: StrengthSetConfirmationUiState,
    input: StrengthSetConfirmationInputState,
    onInputChange: (StrengthSetConfirmationInputState) -> Unit,
    modifier: Modifier
) {
    val skin = LocalTrainFlowSkin.current
    OutlinedTextField(
        value = input.actualWeightInput,
        onValueChange = { value ->
            onInputChange(input.copy(actualWeightInput = value))
        },
        modifier = modifier,
        enabled = confirmation.weightUnit != null,
        singleLine = true,
        label = { Text("实际重量") },
        suffix = { Text(confirmation.weightUnit?.contractValue.orEmpty()) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.titleMedium.copy(color = TrainFlowNeutral50),
        colors = if (skin.isBigType) {
            bigTypeConfirmationFieldColors()
        } else {
            OutlinedTextFieldDefaults.colors()
        }
    )
}

@Composable
private fun StrengthActualRepsField(
    input: StrengthSetConfirmationInputState,
    onInputChange: (StrengthSetConfirmationInputState) -> Unit,
    modifier: Modifier
) {
    val skin = LocalTrainFlowSkin.current
    OutlinedTextField(
        value = input.actualRepsInput,
        onValueChange = { value ->
            onInputChange(input.copy(actualRepsInput = value))
        },
        modifier = modifier,
        singleLine = true,
        label = { Text("实际次数") },
        suffix = { Text("次") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.titleMedium.copy(color = TrainFlowNeutral50),
        colors = if (skin.isBigType) {
            bigTypeConfirmationFieldColors()
        } else {
            OutlinedTextFieldDefaults.colors()
        }
    )
}

@Composable
private fun bigTypeConfirmationFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TrainFlowNeutral50,
    unfocusedTextColor = TrainFlowNeutral50,
    focusedLabelColor = TrainFlowNeutral200,
    unfocusedLabelColor = TrainFlowNeutral200,
    focusedBorderColor = LocalTrainFlowSkin.current.tokens.accent,
    unfocusedBorderColor = TrainFlowNeutral200,
    focusedSuffixColor = TrainFlowNeutral200,
    unfocusedSuffixColor = TrainFlowNeutral200,
    disabledLabelColor = TrainFlowNeutral500,
    disabledBorderColor = TrainFlowNeutral500,
    disabledSuffixColor = TrainFlowNeutral500
)

@Composable
private fun StrengthConfirmationFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TrainFlowNeutral500
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = TrainFlowNeutral50
        )
    }
}

@Composable
private fun StrengthNextSetPanel(uiState: StrengthWorkoutSessionScreenState) {
    if (uiState.isTerminal) return

    val isBigType = LocalTrainFlowSkin.current.isBigType
    StrengthDarkInfoPanel {
        Text(
            text = uiState.nextSetLabel,
            style = if (isBigType) {
                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = TrainFlowNeutral50
        )
        if (!isBigType) {
            Text(
                text = "力量训练按动作和组推进，休息结束后回到下一组准备。",
                style = MaterialTheme.typography.bodySmall,
                color = TrainFlowNeutral200
            )
        }
    }
}

@Composable
private fun StrengthExerciseAdjustmentPanel(
    uiState: StrengthWorkoutSessionScreenState,
    showReplacementOptions: Boolean,
    onToggleReplacementOptions: () -> Unit,
    onReplaceExercise: (String) -> Unit,
    showSkipConfirmation: Boolean,
    onRequestSkipExercise: () -> Unit,
    onCancelSkipExercise: () -> Unit,
    onConfirmSkipExercise: () -> Unit
) {
    if (uiState.isTerminal || (!uiState.canReplaceExercise && !uiState.canSkipExercise)) return

    StrengthDarkInfoPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "动作调整",
                    style = MaterialTheme.typography.titleMedium,
                    color = TrainFlowNeutral50
                )
                Text(
                    text = uiState.substitutionSummaryLabel.ifBlank { "设备不可用或状态变化时使用。" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TrainFlowNeutral200
                )
            }
            StrengthSessionPill(text = "辅助")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.canReplaceExercise) {
                OutlinedButton(
                    onClick = onToggleReplacementOptions,
                    modifier = if (uiState.canSkipExercise) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "替换动作", color = TrainFlowNeutral50)
                }
            }
            if (uiState.canSkipExercise) {
                OutlinedButton(
                    onClick = onRequestSkipExercise,
                    modifier = if (uiState.canReplaceExercise) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TrainFlowError.copy(alpha = 0.55f))
                ) {
                    Text(text = "跳过动作", color = TrainFlowError)
                }
            }
        }

        if (showReplacementOptions) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.replacementOptions.forEach { option ->
                    OutlinedButton(
                        onClick = { onReplaceExercise(option.exerciseId) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TrainFlowAccent.copy(alpha = 0.55f))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = option.exerciseName, color = TrainFlowNeutral50)
                            Text(
                                text = option.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = TrainFlowNeutral200
                            )
                        }
                    }
                }
            }
        }

        if (showSkipConfirmation) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = TrainFlowError.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, TrainFlowError.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "确认跳过当前动作剩余未完成组？",
                        style = MaterialTheme.typography.titleSmall,
                        color = TrainFlowNeutral50
                    )
                    Text(
                        text = "会直接进入下一动作；已确认的组记录会保留。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TrainFlowNeutral200
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancelSkipExercise,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "取消", color = TrainFlowNeutral50)
                        }
                        Button(
                            onClick = onConfirmSkipExercise,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TrainFlowError)
                        ) {
                            Text(text = "确认跳过", color = TrainFlowNeutral50)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StrengthHeartRatePanel(heartRate: HeartRateDisplayUiState) {
    val isBigType = LocalTrainFlowSkin.current.isBigType
    StrengthDarkInfoPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "心率",
                    style = MaterialTheme.typography.labelLarge,
                    color = TrainFlowNeutral200
                )
                Text(
                    text = heartRate.statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TrainFlowNeutral500
                )
                if (!isBigType && heartRate.auxiliaryText.isNotBlank()) {
                    Text(
                        text = heartRate.auxiliaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TrainFlowNeutral500
                    )
                }
                if (!isBigType) {
                    Text(
                        text = heartRate.boundaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TrainFlowNeutral500
                    )
                }
            }
            Text(
                text = heartRate.valueText,
                style = if (isBigType) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = if (heartRate.isAvailable) TrainFlowAccent else TrainFlowNeutral200
            )
        }
    }
}

@Composable
private fun StrengthSessionControls(
    uiState: StrengthWorkoutSessionScreenState,
    confirmationValidation: StrengthSetConfirmationValidation?,
    onStartSet: () -> Unit,
    onCompleteSet: () -> Unit,
    onConfirmSet: () -> Unit,
    onStartNextDuringRest: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = skin.tokens.primary,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = currentPageHorizontalPadding(), vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val canConfirmSet = uiState.canConfirmPlanned && confirmationValidation?.canConfirm == true
            Button(
                onClick = {
                    when {
                        uiState.canStartSet -> onStartSet()
                        uiState.canCompleteSet -> onCompleteSet()
                        canConfirmSet -> onConfirmSet()
                        uiState.canStartNextDuringRest -> onStartNextDuringRest()
                    }
                },
                enabled = uiState.canStartSet ||
                    uiState.canCompleteSet ||
                    canConfirmSet ||
                    uiState.canStartNextDuringRest,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (skin.isBigType) {
                            Modifier.heightIn(min = skin.tokens.trainingButtonHeightDp.dp)
                        } else {
                            Modifier
                        }
                    ),
                shape = RoundedCornerShape(currentCardCorner()),
                colors = ButtonDefaults.buttonColors(containerColor = skin.tokens.action)
            ) {
                Text(
                    text = when {
                        uiState.canStartSet -> "开始本组"
                        uiState.canCompleteSet -> "完成本组"
                        uiState.canConfirmPlanned -> "确认本组"
                        uiState.canStartNextDuringRest -> "提前开始本组"
                        else -> "等待下一步"
                    },
                    fontSize = if (skin.isBigType) 20.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrainFlowNeutral50
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = if (uiState.canResume) onResume else onPause,
                    enabled = uiState.canResume || uiState.canPause,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = skin.tokens.secondaryButtonHeightDp.dp),
                    shape = RoundedCornerShape(currentCardCorner())
                ) {
                    Text(
                        text = if (uiState.canResume) "继续训练" else "暂停训练",
                        fontSize = if (skin.isBigType) 17.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrainFlowNeutral50
                    )
                }
                TextButton(
                    onClick = onEnd,
                    enabled = uiState.canEnd,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = skin.tokens.secondaryButtonHeightDp.dp)
                ) {
                    Text(
                        text = "结束训练",
                        fontSize = if (skin.isBigType) 17.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrainFlowError
                    )
                }
            }
        }
    }
}

@Composable
private fun StrengthSecondaryControlsPanel(
    uiState: StrengthWorkoutSessionScreenState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit
) {
    StrengthDarkInfoPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = if (uiState.canResume) onResume else onPause,
                enabled = uiState.canResume || uiState.canPause,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (uiState.canResume) "继续训练" else "暂停",
                    color = TrainFlowNeutral50
                )
            }
            TextButton(
                onClick = onEnd,
                enabled = uiState.canEnd,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "结束训练", color = TrainFlowError)
            }
        }
    }
}

@Composable
private fun StrengthControlHistoryPanel(uiState: StrengthWorkoutSessionScreenState) {
    StrengthDarkInfoPanel {
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
private fun StrengthTerminalPanel(
    uiState: StrengthWorkoutSessionScreenState,
    onBackToPlans: () -> Unit,
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit
) {
    val skin = LocalTrainFlowSkin.current
    StrengthDarkInfoPanel {
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
            StrengthSessionSummaryPanel(summary, onOpenRecoveryRecommendation)
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
private fun StrengthSessionSummaryPanel(
    summary: StrengthWorkoutSummaryUiState,
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = summary.title,
            style = MaterialTheme.typography.titleMedium,
            color = when (summary.tone) {
                StrengthWorkoutSummaryTone.COMPLETED -> TrainFlowAccent
                StrengthWorkoutSummaryTone.ABANDONED -> TrainFlowNeutral200
            }
        )
        StrengthSummaryMetricGrid(summary.metricItems)
        Text(
            text = summary.durationSemanticsNote,
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral500
        )
        StrengthSummaryDetail(label = "计划 vs 实际", text = summary.planVsActualSummary)
        StrengthSummaryDetail(label = "实际休息", text = summary.restSummary)
        StrengthSummaryDetail(label = "替换动作", text = summary.replacementSummary)
        StrengthSummaryDetail(label = "跳过内容", text = summary.skippedSummary)
        StrengthSummaryDetail(label = "结束状态", text = summary.earlyEndSummary)
        summary.exerciseSummaries.forEach { exercise ->
            StrengthExerciseSummaryPanel(exercise)
        }
        StrengthRecoveryEntryPanel(summary.recoveryEntry, onOpenRecoveryRecommendation)
    }
}

@Composable
private fun StrengthSummaryMetricGrid(items: List<StrengthWorkoutSummaryMetricUiState>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    StrengthSummaryMetricItem(
                        item = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color.Transparent
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun StrengthSummaryMetricItem(
    item: StrengthWorkoutSummaryMetricUiState,
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
private fun StrengthSummaryDetail(
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
private fun StrengthExerciseSummaryPanel(exercise: StrengthWorkoutSummaryExerciseUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TrainFlowNeutral50
                    )
                    Text(
                        text = exercise.setProgressLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TrainFlowNeutral200
                    )
                }
                exercise.skippedLabel?.let { label ->
                    StrengthSessionPill(
                        text = label,
                        containerColor = TrainFlowError.copy(alpha = 0.18f),
                        contentColor = TrainFlowNeutral50
                    )
                }
            }
            exercise.replacementLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = TrainFlowAccent
                )
            }
            exercise.setItems.forEach { set ->
                StrengthSetSummaryRow(set)
            }
        }
    }
}

@Composable
private fun StrengthSetSummaryRow(set: StrengthWorkoutSummarySetUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = set.setLabel,
            style = MaterialTheme.typography.labelMedium,
            color = TrainFlowNeutral200
        )
        Text(
            text = set.actualExerciseLabel,
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowAccent
        )
        Text(
            text = "重量 ${set.plannedWeightLabel} / ${set.actualWeightLabel} · 次数 ${set.plannedRepLabel} / ${set.actualRepLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral100
        )
        Text(
            text = "组耗时 ${set.activeDurationLabel} · 实际休息 ${set.restAfterLabel} · 感受 ${set.effortLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral200
        )
        Text(
            text = set.differenceLabel,
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral500
        )
    }
}

@Composable
private fun StrengthRecoveryEntryPanel(
    entry: StrengthWorkoutRecoveryEntryUiState,
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
private fun StrengthDarkInfoPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(currentCardCorner()),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun StrengthSessionPill(
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

@Preview(showBackground = true)
@Composable
private fun StrengthWorkoutSessionRoutePreview() {
    TrainFlowTheme {
        StrengthWorkoutSessionRoute(
            plan = buildDefaultPlanManagementState().plans[1],
            onBackToPlans = {},
            onOpenRecoveryRecommendation = {}
        )
    }
}
