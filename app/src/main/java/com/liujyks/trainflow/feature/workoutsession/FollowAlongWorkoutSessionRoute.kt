package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineResult
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationClearReason
import com.liujyks.trainflow.core.notifications.AndroidActiveWorkoutNotificationController
import com.liujyks.trainflow.feature.followalong.buildDefaultFollowAlongScreenState
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
internal fun FollowAlongWorkoutSessionRoute(
    plan: WorkoutPlan,
    onBackToFollowAlong: () -> Unit,
    modifier: Modifier = Modifier
) {
    var engineState by remember(plan.id) {
        mutableStateOf(TimedWorkoutEngine.create(plan))
    }
    val context = LocalContext.current
    val activeWorkoutNotifications = remember(context) {
        AndroidActiveWorkoutNotificationController(context.applicationContext)
    }

    fun applyEngineResult(result: TimedWorkoutEngineResult) {
        engineState = result.state
    }

    fun dispatch(command: WorkoutCommand) {
        applyEngineResult(TimedWorkoutEngine.dispatch(engineState, command))
    }

    LaunchedEffect(plan.id) {
        applyEngineResult(TimedWorkoutEngine.dispatch(engineState, WorkoutCommand.StartSession))
        while (true) {
            delay(1000)
            if (engineState.status == SessionStatus.ACTIVE) {
                applyEngineResult(TimedWorkoutEngine.tick(engineState))
            }
        }
    }

    val uiState = engineState.toFollowAlongWorkoutSessionUiState()
    val notificationState = followAlongActiveWorkoutNotificationState(
        planId = plan.id,
        status = engineState.status,
        uiState = uiState
    )
    LaunchedEffect(notificationState) {
        activeWorkoutNotifications.update(notificationState)
    }
    DisposableEffect(activeWorkoutNotifications, plan.id) {
        onDispose {
            activeWorkoutNotifications.clear(ActiveWorkoutNotificationClearReason.ROUTE_DISPOSED)
        }
    }

    FollowAlongWorkoutSessionScreen(
        uiState = uiState,
        onPause = { dispatch(FollowAlongWorkoutSessionControl.PAUSE.toWorkoutCommand()) },
        onResume = { dispatch(FollowAlongWorkoutSessionControl.RESUME.toWorkoutCommand()) },
        onSkip = { dispatch(FollowAlongWorkoutSessionControl.SKIP.toWorkoutCommand()) },
        onEnd = { dispatch(FollowAlongWorkoutSessionControl.END.toWorkoutCommand()) },
        onBackToFollowAlong = onBackToFollowAlong,
        modifier = modifier
    )
}

@Composable
private fun FollowAlongWorkoutSessionScreen(
    uiState: FollowAlongWorkoutSessionUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onEnd: () -> Unit,
    onBackToFollowAlong: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrainFlowPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FollowAlongSessionHeader(uiState)
        FollowAlongMediaPanel(uiState)
        FollowAlongCountdownPanel(uiState)
        FollowAlongNextAndHeartRatePanel(uiState)
        FollowAlongDetailPanel(uiState.detailRows)
        FollowAlongBoundaryPanel(uiState.boundaryCopy)

        if (uiState.isTerminal) {
            FollowAlongTerminalPanel(uiState, onBackToFollowAlong)
        } else {
            FollowAlongControls(
                uiState = uiState,
                onPause = onPause,
                onResume = onResume,
                onSkip = onSkip,
                onEnd = onEnd
            )
        }
    }
}

@Composable
private fun FollowAlongSessionHeader(uiState: FollowAlongWorkoutSessionUiState) {
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
        FollowAlongPill(text = uiState.statusLabel)
    }
}

@Composable
private fun FollowAlongMediaPanel(uiState: FollowAlongWorkoutSessionUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TrainFlowSecondary),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = uiState.mediaPlaceholderTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = TrainFlowNeutral50
                )
                FollowAlongPill(
                    text = uiState.demoStatusLabel,
                    containerColor = TrainFlowAccent,
                    contentColor = TrainFlowPrimary
                )
            }
            Text(
                text = uiState.mediaPlaceholderDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral200
            )
        }
    }
}

@Composable
private fun FollowAlongCountdownPanel(uiState: FollowAlongWorkoutSessionUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FollowAlongPill(
                text = uiState.phaseLabel,
                containerColor = if (uiState.phaseLabel == "休息") TrainFlowAccent else TrainFlowAction,
                contentColor = TrainFlowPrimary
            )
            Text(
                text = uiState.currentActionTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = TrainFlowNeutral50
            )
            Text(
                text = uiState.timerText,
                fontSize = 68.sp,
                lineHeight = 70.sp,
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
private fun FollowAlongNextAndHeartRatePanel(uiState: FollowAlongWorkoutSessionUiState) {
    FollowAlongDarkPanel {
        Text(
            text = uiState.nextActionLabel,
            style = MaterialTheme.typography.titleMedium,
            color = TrainFlowNeutral50
        )
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
                    text = uiState.heartRate.statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TrainFlowNeutral500
                )
                if (uiState.heartRate.auxiliaryText.isNotBlank()) {
                    Text(
                        text = uiState.heartRate.auxiliaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TrainFlowNeutral500
                    )
                }
                Text(
                    text = uiState.heartRate.boundaryText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TrainFlowNeutral500
                )
            }
            Text(
                text = uiState.heartRate.valueText,
                style = MaterialTheme.typography.titleLarge,
                color = if (uiState.heartRate.isAvailable) TrainFlowAccent else TrainFlowNeutral200
            )
        }
    }
}

@Composable
private fun FollowAlongDetailPanel(rows: List<FollowAlongWorkoutDetailRowUiState>) {
    FollowAlongDarkPanel {
        Text(
            text = "动作详情",
            style = MaterialTheme.typography.titleMedium,
            color = TrainFlowNeutral50
        )
        rows.forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TrainFlowNeutral200
                )
                Text(
                    text = row.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrainFlowNeutral100
                )
            }
        }
    }
}

@Composable
private fun FollowAlongBoundaryPanel(copy: String) {
    FollowAlongDarkPanel {
        Text(
            text = "当前边界",
            style = MaterialTheme.typography.labelLarge,
            color = TrainFlowNeutral200
        )
        Text(
            text = copy,
            style = MaterialTheme.typography.bodySmall,
            color = TrainFlowNeutral500
        )
    }
}

@Composable
private fun FollowAlongControls(
    uiState: FollowAlongWorkoutSessionUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
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
                text = if (uiState.canResume) "继续" else "暂停",
                color = TrainFlowNeutral50
            )
        }
        OutlinedButton(
            onClick = onSkip,
            enabled = uiState.canSkip,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = "跳过当前步骤", color = TrainFlowNeutral50)
        }
        if (uiState.lastControlLabel.isNotBlank()) {
            Text(
                text = uiState.lastControlLabel,
                style = MaterialTheme.typography.bodySmall,
                color = TrainFlowNeutral500
            )
        }
        TextButton(
            onClick = onEnd,
            enabled = uiState.canEnd,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "提前结束", color = TrainFlowError)
        }
    }
}

@Composable
private fun FollowAlongTerminalPanel(
    uiState: FollowAlongWorkoutSessionUiState,
    onBackToFollowAlong: () -> Unit
) {
    FollowAlongDarkPanel {
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
            onClick = onBackToFollowAlong,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TrainFlowAccent)
        ) {
            Text(text = "返回基础跟练", color = TrainFlowPrimary)
        }
    }
}

@Composable
private fun FollowAlongDarkPanel(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun FollowAlongPill(
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
private fun FollowAlongWorkoutSessionRoutePreview() {
    TrainFlowTheme {
        FollowAlongWorkoutSessionRoute(
            plan = buildDefaultFollowAlongScreenState().plans.single().plan,
            onBackToFollowAlong = {}
        )
    }
}
