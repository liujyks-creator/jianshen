package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.Alignment
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
import com.liujyks.trainflow.core.model.WorkoutSession
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
import com.liujyks.trainflow.ui.designsystem.currentCardCorner
import com.liujyks.trainflow.ui.designsystem.currentPageHorizontalPadding
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin
import com.liujyks.trainflow.ui.theme.isBigType
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
internal fun FollowAlongWorkoutSessionRoute(
    plan: WorkoutPlan,
    onBackToFollowAlong: () -> Unit,
    onRecordWorkoutSession: suspend (WorkoutSession) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sessionId = remember(plan.id) { "session-${plan.id}-${System.currentTimeMillis()}" }
    val sessionStartedAt = remember(sessionId) { Instant.now() }
    var recordWriteState by remember(sessionId) { mutableStateOf(TerminalWorkoutSessionRecordWriteState()) }
    var engineState by remember(plan.id, sessionId) {
        mutableStateOf(TimedWorkoutEngine.create(plan, sessionId = sessionId))
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
            if (engineState.status == SessionStatus.ACTIVE || engineState.status == SessionStatus.PAUSED) {
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
    var endConfirmation by remember { mutableStateOf(WorkoutEndConfirmationUiState()) }
    LaunchedEffect(uiState.canEnd) {
        if (!uiState.canEnd) endConfirmation = endConfirmation.cancel()
    }

    FollowAlongWorkoutSessionScreen(
        uiState = uiState,
        onPause = { dispatch(FollowAlongWorkoutSessionControl.PAUSE.toWorkoutCommand()) },
        onResume = { dispatch(FollowAlongWorkoutSessionControl.RESUME.toWorkoutCommand()) },
        onSkip = { dispatch(FollowAlongWorkoutSessionControl.SKIP.toWorkoutCommand()) },
        showEndConfirmation = endConfirmation.visible,
        onRequestEnd = { endConfirmation = endConfirmation.request(uiState.canEnd) },
        onCancelEnd = { endConfirmation = endConfirmation.cancel() },
        onConfirmEnd = {
            val result = endConfirmation.confirm(uiState.canEnd)
            endConfirmation = result.nextState
            result.command?.let(::dispatch)
        },
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
    showEndConfirmation: Boolean,
    onRequestEnd: () -> Unit,
    onCancelEnd: () -> Unit,
    onConfirmEnd: () -> Unit,
    onBackToFollowAlong: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(skin.tokens.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = currentPageHorizontalPadding())
                .padding(
                    top = if (skin.isBigType) 14.dp else 22.dp,
                    bottom = if (uiState.isTerminal) 22.dp else skin.tokens.executionControlReserveDp.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FollowAlongSessionHeader(uiState)
            FollowAlongMediaPanel(uiState)
            FollowAlongCountdownPanel(
                uiState = uiState,
                onPrimaryToggle = if (uiState.canResume) onResume else onPause
            )
            FollowAlongNextAndHeartRatePanel(uiState)
            FollowAlongDetailPanel(uiState.detailRows)
            FollowAlongBoundaryPanel(uiState.boundaryCopy)

            if (uiState.isTerminal) {
                FollowAlongTerminalPanel(uiState, onBackToFollowAlong)
            } else if (uiState.lastControlLabel.isNotBlank()) {
                Text(
                    text = uiState.lastControlLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TrainFlowNeutral500
                )
            }
        }

        if (!uiState.isTerminal) {
            FollowAlongControls(
                uiState = uiState,
                onPause = onPause,
                onResume = onResume,
                onSkip = onSkip,
                onEnd = onRequestEnd,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        if (showEndConfirmation) {
            WorkoutEndConfirmationDialog(
                title = "结束本次基础跟练？",
                text = "训练会提前结束，并保留当前内存态进度用于本次总结。",
                onCancel = onCancelEnd,
                onConfirm = onConfirmEnd
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
private fun FollowAlongCountdownPanel(
    uiState: FollowAlongWorkoutSessionUiState,
    onPrimaryToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = uiState.canPause || uiState.canResume) {
                onPrimaryToggle()
            },
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
            Button(
                onClick = if (uiState.canResume) onResume else onPause,
                enabled = uiState.canResume || uiState.canPause,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (skin.isBigType) skin.tokens.trainingButtonHeightDp.dp else 48.dp),
                shape = RoundedCornerShape(currentCardCorner()),
                colors = ButtonDefaults.buttonColors(containerColor = skin.tokens.action)
            ) {
                Text(
                    text = if (uiState.canResume) "继续训练" else "暂停训练",
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
                    onClick = onSkip,
                    enabled = uiState.canSkip,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = skin.tokens.secondaryButtonHeightDp.dp),
                    shape = RoundedCornerShape(currentCardCorner())
                ) {
                    Text(
                        text = "跳过 / 下一步",
                        fontSize = if (skin.isBigType) 17.sp else 14.sp,
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
