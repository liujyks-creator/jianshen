package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
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
internal fun TimedWorkoutSessionRoute(
    plan: WorkoutPlan,
    onBackToPlans: () -> Unit,
    modifier: Modifier = Modifier
) {
    var engineState by remember(plan.id) {
        mutableStateOf(startTimedWorkout(plan))
    }

    LaunchedEffect(plan.id) {
        while (true) {
            delay(1000)
            if (engineState.status == SessionStatus.ACTIVE) {
                engineState = TimedWorkoutEngine.tick(engineState).state
            }
        }
    }

    fun dispatch(command: WorkoutCommand) {
        engineState = TimedWorkoutEngine.dispatch(engineState, command).state
    }

    TimedWorkoutSessionScreen(
        uiState = engineState.toTimedWorkoutSessionScreenState(),
        onPause = { dispatch(WorkoutCommand.PauseSession) },
        onResume = { dispatch(WorkoutCommand.ResumeSession) },
        onSkip = { dispatch(WorkoutCommand.SkipStep) },
        onExtendRest = { dispatch(WorkoutCommand.ExtendRest(seconds = 15)) },
        onEnd = { dispatch(WorkoutCommand.EndSession(reason = "user_requested")) },
        onBackToPlans = onBackToPlans,
        modifier = modifier
    )
}

private fun startTimedWorkout(plan: WorkoutPlan): TimedWorkoutEngineState {
    val initial = TimedWorkoutEngine.create(plan)
    return TimedWorkoutEngine.dispatch(initial, WorkoutCommand.StartSession).state
}

@Composable
private fun TimedWorkoutSessionScreen(
    uiState: TimedWorkoutSessionScreenState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onExtendRest: () -> Unit,
    onEnd: () -> Unit,
    onBackToPlans: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrainFlowPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SessionHeader(uiState)
        MainCountdownPanel(uiState)
        NextStepPanel(uiState)
        HeartRatePanel(uiState.heartRate)

        if (uiState.isTerminal) {
            TerminalPanel(uiState, onBackToPlans)
        } else {
            SessionControls(
                uiState = uiState,
                onPause = onPause,
                onResume = onResume,
                onSkip = onSkip,
                onExtendRest = onExtendRest,
                onEnd = onEnd
            )
        }
    }
}

@Composable
private fun SessionHeader(uiState: TimedWorkoutSessionScreenState) {
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
                text = uiState.progressLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral200
            )
        }
        SessionPill(text = uiState.statusLabel)
    }
}

@Composable
private fun MainCountdownPanel(uiState: TimedWorkoutSessionScreenState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TrainFlowSecondary),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SessionPill(
                text = uiState.phaseLabel,
                containerColor = if (uiState.phaseLabel == "休息") TrainFlowAccent else TrainFlowAction,
                contentColor = TrainFlowPrimary
            )
            Text(
                text = uiState.currentTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = TrainFlowNeutral50
            )
            Text(
                text = uiState.timerText,
                fontSize = 72.sp,
                lineHeight = 74.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TrainFlowNeutral50
            )
            LinearProgressIndicator(
                progress = { uiState.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = TrainFlowAccent,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
            Text(
                text = uiState.shortCue,
                style = MaterialTheme.typography.bodyLarge,
                color = TrainFlowNeutral100
            )
        }
    }
}

@Composable
private fun NextStepPanel(uiState: TimedWorkoutSessionScreenState) {
    DarkInfoPanel {
        Text(
            text = uiState.nextStepLabel,
            style = MaterialTheme.typography.titleMedium,
            color = TrainFlowNeutral50
        )
        Text(
            text = "保持节奏，当前阶段结束后会自动进入下一步。",
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral200
        )
    }
}

@Composable
private fun HeartRatePanel(heartRate: TimedWorkoutHeartRateUiState) {
    DarkInfoPanel {
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
private fun SessionControls(
    uiState: TimedWorkoutSessionScreenState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onExtendRest: () -> Unit,
    onEnd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = if (uiState.canResume) onResume else onPause,
            enabled = uiState.canResume || uiState.canPause,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TrainFlowAction)
        ) {
            Text(
                text = if (uiState.canResume) "继续训练" else "暂停",
                color = TrainFlowNeutral50
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onSkip,
                enabled = uiState.canSkip,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "跳过", color = TrainFlowNeutral50)
            }
            OutlinedButton(
                onClick = onExtendRest,
                enabled = uiState.canExtendRest,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "+15秒", color = TrainFlowNeutral50)
            }
        }
        TextButton(
            onClick = onEnd,
            enabled = uiState.canEnd,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "结束训练", color = TrainFlowError)
        }
    }
}

@Composable
private fun TerminalPanel(
    uiState: TimedWorkoutSessionScreenState,
    onBackToPlans: () -> Unit
) {
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
private fun DarkInfoPanel(content: @Composable ColumnScope.() -> Unit) {
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

@Preview(showBackground = true)
@Composable
private fun TimedWorkoutSessionRoutePreview() {
    TrainFlowTheme {
        TimedWorkoutSessionRoute(
            plan = buildDefaultPlanManagementState().plans.first(),
            onBackToPlans = {}
        )
    }
}
