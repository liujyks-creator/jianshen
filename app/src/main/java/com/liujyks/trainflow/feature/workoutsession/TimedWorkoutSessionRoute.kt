package com.liujyks.trainflow.feature.workoutsession

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendation
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineResult
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.media.CountdownReminderFeedbackDispatcher
import com.liujyks.trainflow.core.media.CountdownReminderFeedbackRequest
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutEvent
import com.liujyks.trainflow.core.model.WorkoutPlan
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
import kotlinx.coroutines.delay

@Composable
internal fun TimedWorkoutSessionRoute(
    plan: WorkoutPlan,
    onBackToPlans: () -> Unit,
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit,
    modifier: Modifier = Modifier
) {
    var engineState by remember(plan.id) {
        mutableStateOf(TimedWorkoutEngine.create(plan))
    }
    val feedbackSink = rememberCountdownReminderFeedbackSink()
    val context = LocalContext.current
    val activeWorkoutNotifications = remember(context) {
        AndroidActiveWorkoutNotificationController(context.applicationContext)
    }

    fun applyEngineResult(result: TimedWorkoutEngineResult) {
        engineState = result.state
        result.events.dispatchCountdownReminders(
            state = result.state,
            feedbackSink = feedbackSink
        )
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

    fun dispatch(command: WorkoutCommand) {
        applyEngineResult(TimedWorkoutEngine.dispatch(engineState, command))
    }

    val uiState = engineState.toTimedWorkoutSessionScreenState()
    val notificationState = timedActiveWorkoutNotificationState(
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

    TimedWorkoutSessionScreen(
        uiState = uiState,
        onPause = { dispatch(WorkoutCommand.PauseSession) },
        onResume = { dispatch(WorkoutCommand.ResumeSession) },
        onSkip = { dispatch(WorkoutCommand.SkipStep) },
        onExtendRest = { dispatch(WorkoutCommand.ExtendRest(seconds = 15)) },
        onEnd = { dispatch(WorkoutCommand.EndSession(reason = "user_requested")) },
        onBackToPlans = onBackToPlans,
        onOpenRecoveryRecommendation = onOpenRecoveryRecommendation,
        modifier = modifier
    )
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
    onOpenRecoveryRecommendation: (BasicRecoveryRecommendation) -> Unit,
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
                    bottom = if (uiState.isTerminal) {
                        22.dp
                    } else {
                        skin.tokens.executionControlReserveDp.dp
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(currentSectionSpacing())
        ) {
            SessionHeader(uiState)
            MainCountdownPanel(
                uiState = uiState,
                onPrimaryToggle = if (uiState.canResume) onResume else onPause
            )
            if (uiState.shouldShowNextStepPanel) {
                NextStepPanel(uiState)
            }
            HeartRatePanel(uiState.heartRate)

            if (uiState.isTerminal) {
                TerminalPanel(uiState, onBackToPlans, onOpenRecoveryRecommendation)
            } else if (!skin.isBigType) {
                TimedControlHistoryPanel(uiState)
            }
        }

        if (!uiState.isTerminal) {
            TimedSessionControls(
                uiState = uiState,
                onPause = onPause,
                onResume = onResume,
                onSkip = onSkip,
                onExtendRest = onExtendRest,
                onEnd = onEnd,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun SessionHeader(uiState: TimedWorkoutSessionScreenState) {
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
                text = uiState.progressLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral200
            )
        }
        SessionPill(text = uiState.statusLabel)
    }
}

@Composable
private fun MainCountdownPanel(
    uiState: TimedWorkoutSessionScreenState,
    onPrimaryToggle: () -> Unit
) {
    val skin = LocalTrainFlowSkin.current
    val reminder = uiState.countdownReminder
    val reminderActive = reminder.isActive && reminder.emphasisAnimationEnabled
    val panelColor = if (reminderActive) {
        skin.tokens.action.copy(alpha = 0.18f)
    } else {
        skin.tokens.secondary
    }
    val borderColor = if (reminderActive) {
        skin.tokens.action.copy(alpha = 0.7f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val timerColor = if (reminderActive) skin.tokens.action else skin.tokens.neutral50
    val progressColor = if (reminderActive) skin.tokens.action else uiState.stageColorHex.toDialColor(skin.tokens.accent)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                SessionPill(
                    text = if (reminder.isActive) reminder.type.label else uiState.phaseLabel,
                    containerColor = progressColor,
                    contentColor = skin.tokens.primary
                )
                Text(
                    text = "总剩余 ${uiState.totalRemainingText}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TrainFlowNeutral200
                )
            }
            StageDial(
                uiState = uiState,
                progressColor = progressColor,
                timerColor = timerColor,
                onPrimaryToggle = onPrimaryToggle
            )
            if (reminder.isActive) {
                ReminderStatusPanel(reminder)
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
private fun StageDial(
    uiState: TimedWorkoutSessionScreenState,
    progressColor: Color,
    timerColor: Color,
    onPrimaryToggle: () -> Unit
) {
    val skin = LocalTrainFlowSkin.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (skin.isBigType) 360.dp else 320.dp)
            .clickable(enabled = uiState.canPause || uiState.canResume) {
                onPrimaryToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(if (skin.isBigType) 330.dp else 292.dp)
        ) {
            val stroke = if (skin.isBigType) 24.dp.toPx() else 20.dp.toPx()
            val inset = stroke / 2f
            val arcSize = size.copy(width = size.width - stroke, height = size.height - stroke)
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.minDimension / 2f - inset,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = progressColor.copy(alpha = 0.38f),
                startAngle = -90f,
                sweepAngle = 360f * uiState.progressFraction.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * uiState.stageProgressFraction.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset + stroke, inset + stroke),
                size = arcSize.copy(width = arcSize.width - stroke * 2f, height = arcSize.height - stroke * 2f),
                style = Stroke(width = stroke * 0.72f, cap = StrokeCap.Round)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = uiState.stageIconKey,
                style = MaterialTheme.typography.labelLarge,
                color = TrainFlowNeutral200
            )
            Text(
                text = uiState.currentTitle,
                style = if (skin.isBigType) {
                    MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 36.sp,
                        lineHeight = 41.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                color = TrainFlowNeutral50
            )
            Text(
                text = uiState.timerText,
                fontSize = (72f * skin.tokens.timerScale).sp,
                lineHeight = (74f * skin.tokens.timerScale).sp,
                fontWeight = FontWeight.ExtraBold,
                color = timerColor
            )
            Text(
                text = if (uiState.canResume) "点击圆盘继续" else "点击圆盘暂停",
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral200
            )
        }
    }
}

@Composable
private fun ReminderStatusPanel(reminder: TimedWorkoutCountdownReminderUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = reminder.message,
                style = MaterialTheme.typography.titleMedium,
                color = TrainFlowNeutral50
            )
            Text(
                text = reminder.feedbackLabel,
                style = MaterialTheme.typography.bodySmall,
                color = TrainFlowNeutral200
            )
        }
    }
}

@Composable
private fun NextStepPanel(uiState: TimedWorkoutSessionScreenState) {
    val isBigType = LocalTrainFlowSkin.current.isBigType
    DarkInfoPanel {
        Text(
            text = uiState.nextStepLabel,
            style = if (isBigType) {
                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = TrainFlowNeutral50
        )
        if (!isBigType) {
            Text(
                text = "保持节奏，当前阶段结束后会自动进入下一步。",
                style = MaterialTheme.typography.bodySmall,
                color = TrainFlowNeutral200
            )
        }
    }
}

@Composable
private fun HeartRatePanel(heartRate: HeartRateDisplayUiState) {
    val isBigType = LocalTrainFlowSkin.current.isBigType
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
private fun TimedSessionControls(
    uiState: TimedWorkoutSessionScreenState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onExtendRest: () -> Unit,
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
                    text = if (uiState.canResume) "继续训练" else "暂停训练",
                    fontSize = if (skin.isBigType) 20.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrainFlowNeutral50
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSkip,
                    enabled = uiState.canSkip,
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
                ) {
                    Text(
                        text = "跳过",
                        fontSize = if (skin.isBigType) 17.sp else 14.sp,
                        color = TrainFlowNeutral50
                    )
                }
                OutlinedButton(
                    onClick = onExtendRest,
                    enabled = uiState.canExtendRest,
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
                ) {
                    Text(
                        text = "+15秒",
                        fontSize = if (skin.isBigType) 17.sp else 14.sp,
                        color = TrainFlowNeutral50
                    )
                }
                TextButton(
                    onClick = onEnd,
                    enabled = uiState.canEnd,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (skin.isBigType) {
                                Modifier.heightIn(min = skin.tokens.secondaryButtonHeightDp.dp)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Text(
                        text = if (skin.isBigType) "结束训练" else "结束",
                        fontSize = if (skin.isBigType) 16.sp else 14.sp,
                        color = TrainFlowError
                    )
                }
            }
        }
    }
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
private fun DarkInfoPanel(content: @Composable ColumnScope.() -> Unit) {
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
private fun rememberCountdownReminderFeedbackSink(): CountdownReminderFeedbackSink {
    val hapticFeedback = LocalHapticFeedback.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60) }

    DisposableEffect(toneGenerator) {
        onDispose { toneGenerator.release() }
    }

    return remember(hapticFeedback, toneGenerator) {
        AndroidCountdownReminderFeedbackSink(
            toneGenerator = toneGenerator,
            hapticFeedback = hapticFeedback
        )
    }
}

private interface CountdownReminderFeedbackSink {
    fun dispatch(request: CountdownReminderFeedbackRequest)
}

private class AndroidCountdownReminderFeedbackSink(
    private val toneGenerator: ToneGenerator,
    private val hapticFeedback: HapticFeedback
) : CountdownReminderFeedbackSink {
    override fun dispatch(request: CountdownReminderFeedbackRequest) {
        if (request.soundEnabled) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
        }
        if (request.vibrationEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

private fun List<WorkoutEvent>.dispatchCountdownReminders(
    state: TimedWorkoutEngineState,
    feedbackSink: CountdownReminderFeedbackSink
) {
    forEach { event ->
        val cue = state.endingCueFor(event)
        val request = CountdownReminderFeedbackDispatcher.requestFor(event = event, cue = cue)
        if (request != null) {
            feedbackSink.dispatch(request)
        }
    }
}

private fun TimedWorkoutEngineState.endingCueFor(event: WorkoutEvent) = when (event) {
    is WorkoutEvent.TimedWorkEnding -> steps.firstOrNull { step -> step.id == event.stepId }?.endingCue
    is WorkoutEvent.RestEnding -> steps.firstOrNull { step -> step.id == event.stepId }?.endingCue
    else -> null
}

private val TimedWorkoutCountdownReminderType.label: String
    get() = when (this) {
        TimedWorkoutCountdownReminderType.ACTION_ENDING -> "阶段提醒"
        TimedWorkoutCountdownReminderType.REST_ENDING -> "休息提醒"
        TimedWorkoutCountdownReminderType.NONE -> ""
    }

private val TimedWorkoutCountdownReminderUiState.feedbackLabel: String
    get() {
        val enabled = listOfNotNull(
            "声音".takeIf { soundEnabled },
            "震动".takeIf { vibrationEnabled },
            "强化动画".takeIf { emphasisAnimationEnabled }
        )
        return if (enabled.isEmpty()) {
            "仅显示屏幕提醒"
        } else {
            "已启用：${enabled.joinToString("、")}"
        }
    }

private fun String?.toDialColor(fallback: Color): Color {
    val value = this ?: return fallback
    return runCatching { Color(android.graphics.Color.parseColor(value)) }
        .getOrElse { fallback }
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
