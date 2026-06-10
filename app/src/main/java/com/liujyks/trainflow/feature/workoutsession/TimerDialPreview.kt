package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin
import com.liujyks.trainflow.ui.theme.SkinRegistry
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

@Composable
internal fun TimerDialPrototypeDemo(
    state: TimerDialUiState,
    onTogglePause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = state.visualVariant.tokens(LocalTrainFlowSkin.current)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.pageBackground)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TimerDial(
            state = state,
            onTogglePause = onTogglePause
        )
        TimerDialPrototypeControls(
            color = tokens.textPrimary,
            dangerColor = tokens.finalCountdown,
            onReset = onReset,
            onSkip = onSkip,
            onEnd = onEnd
        )
    }
}

@Composable
private fun TimerDialPrototypeControls(
    color: Color,
    dangerColor: Color,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onEnd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally)
    ) {
        TimerDialPrototypeIconButton(
            kind = TimerDialPrototypeActionIcon.RESET,
            contentDescription = "重置",
            color = color,
            onClick = onReset
        )
        TimerDialPrototypeIconButton(
            kind = TimerDialPrototypeActionIcon.SKIP,
            contentDescription = "跳过",
            color = color,
            onClick = onSkip
        )
        TimerDialPrototypeIconButton(
            kind = TimerDialPrototypeActionIcon.END,
            contentDescription = "结束",
            color = dangerColor,
            onClick = onEnd
        )
    }
}

@Composable
private fun TimerDialPrototypeIconButton(
    kind: TimerDialPrototypeActionIcon,
    contentDescription: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(52.dp)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Canvas(modifier = Modifier.padding(15.dp)) {
            val stroke = 2.4.dp.toPx()
            when (kind) {
                TimerDialPrototypeActionIcon.RESET -> {
                    drawArc(
                        color = color,
                        startAngle = 35f,
                        sweepAngle = 285f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawLine(
                        color = color,
                        start = Offset(size.width * 0.25f, size.height * 0.12f),
                        end = Offset(size.width * 0.48f, size.height * 0.12f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = color,
                        start = Offset(size.width * 0.25f, size.height * 0.12f),
                        end = Offset(size.width * 0.25f, size.height * 0.35f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
                TimerDialPrototypeActionIcon.SKIP -> {
                    drawTriangle(color, left = 0.18f)
                    drawTriangle(color, left = 0.48f)
                    drawLine(
                        color = color,
                        start = Offset(size.width * 0.86f, size.height * 0.18f),
                        end = Offset(size.width * 0.86f, size.height * 0.82f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
                TimerDialPrototypeActionIcon.END -> {
                    drawLine(
                        color = color,
                        start = Offset(size.width * 0.2f, size.height * 0.2f),
                        end = Offset(size.width * 0.8f, size.height * 0.8f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = color,
                        start = Offset(size.width * 0.8f, size.height * 0.2f),
                        end = Offset(size.width * 0.2f, size.height * 0.8f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private enum class TimerDialPrototypeActionIcon {
    RESET,
    SKIP,
    END
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTriangle(
    color: Color,
    left: Float
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * left, size.height * 0.18f)
        lineTo(size.width * (left + 0.26f), size.height * 0.5f)
        lineTo(size.width * left, size.height * 0.82f)
        close()
    }
    drawPath(path, color)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun OfficialFlowTimerDialPrototypePreview() {
    TrainFlowTheme {
        TimerDialPrototypeDemo(
            state = sampleTimerDialState(TimerDialVisualVariant.OFFICIAL_FLOW),
            onTogglePause = {},
            onReset = {},
            onSkip = {},
            onEnd = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun BlackRedTimerDialPrototypePreview() {
    TrainFlowTheme {
        TimerDialPrototypeDemo(
            state = sampleTimerDialState(TimerDialVisualVariant.BLACK_RED_HIGH_CONTRAST),
            onTogglePause = {},
            onReset = {},
            onSkip = {},
            onEnd = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun CyberNeonTimerDialPrototypePreview() {
    TrainFlowTheme {
        TimerDialPrototypeDemo(
            state = sampleTimerDialState(TimerDialVisualVariant.CYBER_NEON),
            onTogglePause = {},
            onReset = {},
            onSkip = {},
            onEnd = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PausedTimerDialPrototypePreview() {
    TrainFlowTheme(skin = SkinRegistry.skins.first { skin -> skin.builtInSkin.id == "big_type" }) {
        TimerDialPrototypeDemo(
            state = sampleTimerDialState(
                variant = TimerDialVisualVariant.OFFICIAL_FLOW,
                paused = true
            ),
            onTogglePause = {},
            onReset = {},
            onSkip = {},
            onEnd = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun FinalCountdownTimerDialPrototypePreview() {
    TrainFlowTheme {
        TimerDialPrototypeDemo(
            state = sampleTimerDialState(
                variant = TimerDialVisualVariant.OFFICIAL_FLOW,
                finalCountdown = true
            ),
            onTogglePause = {},
            onReset = {},
            onSkip = {},
            onEnd = {}
        )
    }
}

private fun sampleTimerDialState(
    variant: TimerDialVisualVariant,
    paused: Boolean = false,
    finalCountdown: Boolean = false
): TimerDialUiState {
    val currentProgress = if (finalCountdown) 0.88f else 0.42f
    val remainingSec = if (finalCountdown) 5 else 28
    return TimerDialUiState(
        totalRemainingSec = 868,
        totalProgress = 0.38f,
        currentStageProgress = currentProgress,
        currentStageType = TimerDialStageType.WORK,
        currentStageLabel = "爆发间歇",
        currentStageIndex = 3,
        currentStageRemainingSec = remainingSec,
        isPaused = paused,
        isFinalCountdown = finalCountdown,
        stageSegments = listOf(
            TimerDialStageSegmentUiState("warmup", "热身", TimerDialStageType.WARMUP, 180, 1f, false),
            TimerDialStageSegmentUiState("work-1", "工作", TimerDialStageType.WORK, 40, 1f, false),
            TimerDialStageSegmentUiState("rest-1", "休息", TimerDialStageType.REST, 20, 1f, false),
            TimerDialStageSegmentUiState("work-2", "爆发间歇", TimerDialStageType.WORK, 40, currentProgress, true),
            TimerDialStageSegmentUiState("rest-2", "休息", TimerDialStageType.REST, 20, 0f, false),
            TimerDialStageSegmentUiState("custom", "核心保持", TimerDialStageType.CUSTOM, 35, 0f, false),
            TimerDialStageSegmentUiState("cooldown", "放松", TimerDialStageType.COOLDOWN, 120, 0f, false)
        ),
        visualVariant = variant,
        currentStageIconKey = "work",
        currentStageTimeText = "00:${remainingSec.toString().padStart(2, '0')}",
        totalRemainingText = "14:28",
        centerActionLabel = if (paused) "双击继续" else "双击暂停",
        canTogglePause = true
    )
}
