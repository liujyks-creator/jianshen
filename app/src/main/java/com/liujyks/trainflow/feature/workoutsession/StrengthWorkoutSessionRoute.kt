package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.engine.StrengthWorkoutEngineResult
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutPlan
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
import kotlinx.coroutines.delay

@Composable
internal fun StrengthWorkoutSessionRoute(
    plan: WorkoutPlan,
    onBackToPlans: () -> Unit,
    modifier: Modifier = Modifier
) {
    var engineState by remember(plan.id) {
        mutableStateOf(StrengthWorkoutEngine.create(plan))
    }

    fun applyEngineResult(result: StrengthWorkoutEngineResult) {
        engineState = result.state
    }

    LaunchedEffect(plan.id) {
        applyEngineResult(StrengthWorkoutEngine.dispatch(engineState, WorkoutCommand.StartSession))
        while (true) {
            delay(1000)
            if (engineState.status == SessionStatus.ACTIVE) {
                applyEngineResult(StrengthWorkoutEngine.tick(engineState))
            }
        }
    }

    fun dispatch(command: WorkoutCommand) {
        applyEngineResult(StrengthWorkoutEngine.dispatch(engineState, command))
    }

    val uiState = engineState.toStrengthWorkoutSessionScreenState()
    var confirmationInput by remember { mutableStateOf<StrengthSetConfirmationInputState?>(null) }
    LaunchedEffect(uiState.confirmation?.setKey) {
        confirmationInput = uiState.confirmation?.initialInputState()
    }
    val activeConfirmationInput = uiState.confirmation?.let { confirmation ->
        confirmationInput ?: confirmation.initialInputState()
    }
    var showReplacementOptions by remember { mutableStateOf(false) }
    var showSkipConfirmation by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.canReplaceExercise, uiState.canSkipExercise, uiState.currentExerciseName) {
        if (!uiState.canReplaceExercise) showReplacementOptions = false
        if (!uiState.canSkipExercise) showSkipConfirmation = false
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
        onEnd = { dispatch(WorkoutCommand.EndSession(reason = "user_requested")) },
        onBackToPlans = onBackToPlans,
        modifier = modifier
    )
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
    onEnd: () -> Unit,
    onBackToPlans: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TrainFlowPrimary)
    ) {
        val confirmationValidation = uiState.confirmation?.let { confirmation ->
            confirmationInput?.validateFor(confirmation)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(
                    top = 22.dp,
                    bottom = if (uiState.isTerminal) 22.dp else 132.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StrengthSessionHeader(uiState)
            StrengthMainPanel(uiState)
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
                StrengthTerminalPanel(uiState, onBackToPlans)
            } else {
                StrengthSecondaryControlsPanel(
                    uiState = uiState,
                    onPause = onPause,
                    onResume = onResume,
                    onEnd = onEnd
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
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun StrengthSessionHeader(uiState: StrengthWorkoutSessionScreenState) {
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
                style = MaterialTheme.typography.titleLarge,
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
private fun StrengthMainPanel(uiState: StrengthWorkoutSessionScreenState) {
    val isRest = uiState.phaseLabel == "休息"
    val isConfirm = uiState.canConfirmPlanned
    val panelColor = when {
        isConfirm -> TrainFlowAction.copy(alpha = 0.14f)
        isRest -> TrainFlowAccent.copy(alpha = 0.14f)
        else -> TrainFlowSecondary
    }
    val borderColor = when {
        isConfirm -> TrainFlowAction.copy(alpha = 0.55f)
        isRest -> TrainFlowAccent.copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.08f)
    }
    val metricColor = when {
        uiState.canCompleteSet || isConfirm -> TrainFlowAction
        isRest -> TrainFlowAccent
        else -> TrainFlowNeutral50
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = panelColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StrengthSessionPill(
                    text = uiState.phaseLabel,
                    containerColor = if (isRest) TrainFlowAccent else TrainFlowAction,
                    contentColor = TrainFlowPrimary
                )
                if (uiState.setKindLabel.isNotBlank()) {
                    StrengthSessionPill(text = uiState.setKindLabel)
                }
            }
            Text(
                text = uiState.currentExerciseName,
                style = MaterialTheme.typography.headlineMedium,
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
                    color = TrainFlowAccent,
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
                    fontSize = if (uiState.canStartSet) 30.sp else 72.sp,
                    lineHeight = if (uiState.canStartSet) 36.sp else 74.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = metricColor
                )
                LinearProgressIndicator(
                    progress = { uiState.progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isRest) TrainFlowAccent else TrainFlowAction,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
                Text(
                    text = uiState.targetSummary,
                    style = MaterialTheme.typography.titleMedium,
                    color = TrainFlowNeutral50
                )
            }
            Text(
                text = uiState.shortCue,
                style = MaterialTheme.typography.bodyLarge,
                color = TrainFlowNeutral100
            )
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
        shape = RoundedCornerShape(14.dp),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = input.actualWeightInput,
                    onValueChange = { value ->
                        onInputChange(input.copy(actualWeightInput = value))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = confirmation.weightUnit != null,
                    singleLine = true,
                    label = { Text("实际重量") },
                    suffix = { Text(confirmation.weightUnit?.contractValue.orEmpty()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = TrainFlowNeutral50)
                )
                OutlinedTextField(
                    value = input.actualRepsInput,
                    onValueChange = { value ->
                        onInputChange(input.copy(actualRepsInput = value))
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("实际次数") },
                    suffix = { Text("次") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = TrainFlowNeutral50)
                )
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

    StrengthDarkInfoPanel {
        Text(
            text = uiState.nextSetLabel,
            style = MaterialTheme.typography.titleMedium,
            color = TrainFlowNeutral50
        )
        Text(
            text = "力量训练按动作和组推进，休息结束后回到下一组准备。",
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral200
        )
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
private fun StrengthHeartRatePanel(heartRate: StrengthWorkoutHeartRateUiState) {
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
            }
            Text(
                text = heartRate.valueText,
                style = MaterialTheme.typography.titleLarge,
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TrainFlowPrimary,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrainFlowAction)
            ) {
                Text(
                    text = when {
                        uiState.canStartSet -> "开始本组"
                        uiState.canCompleteSet -> "完成本组"
                        uiState.canConfirmPlanned -> "确认本组"
                        uiState.canStartNextDuringRest -> "提前开始本组"
                        else -> "等待下一步"
                    },
                    color = TrainFlowNeutral50
                )
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
    onBackToPlans: () -> Unit
) {
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
            StrengthSessionSummaryPanel(summary)
        }
        Button(
            onClick = onBackToPlans,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TrainFlowAccent)
        ) {
            Text(text = "返回计划", color = TrainFlowPrimary)
        }
    }
}

@Composable
private fun StrengthSessionSummaryPanel(summary: StrengthWorkoutSummaryUiState) {
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
        StrengthRecoveryEntryPanel(summary.recoveryEntry)
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
private fun StrengthRecoveryEntryPanel(entry: StrengthWorkoutRecoveryEntryUiState) {
    OutlinedButton(
        onClick = {},
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
        shape = RoundedCornerShape(10.dp),
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
            onBackToPlans = {}
        )
    }
}
