package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin
import com.liujyks.trainflow.ui.theme.isBigType

@Composable
internal fun TimerDial(
    state: TimerDialUiState,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeState = state.clamped()
    val skin = LocalTrainFlowSkin.current
    val tokens = safeState.visualVariant.tokens(skin)
    val animatedTotalProgress by animateFloatAsState(
        targetValue = safeState.totalProgress,
        animationSpec = tween(durationMillis = 360),
        label = "TimerDialTotalProgress"
    )
    val animatedStageProgress by animateFloatAsState(
        targetValue = safeState.currentStageProgress,
        animationSpec = tween(durationMillis = 260),
        label = "TimerDialStageProgress"
    )
    val finalPulse by animateFloatAsState(
        targetValue = if (safeState.isFinalCountdown && !safeState.isPaused) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "TimerDialFinalPulse"
    )
    val dialSize = if (skin.isBigType) 330.dp else 300.dp
    val centerSize = if (skin.isBigType) 208.dp else 184.dp
    val contentDescription = buildString {
        append(safeState.currentStageLabel)
        append("，剩余 ")
        append(safeState.currentStageTimeText)
        append("，总剩余 ")
        append(safeState.totalRemainingText)
        append(if (safeState.isPaused) "，已暂停，" else "，进行中，")
        append(safeState.centerActionLabel)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (skin.isBigType) 360.dp else 328.dp)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(enabled = safeState.canTogglePause) {
                onTogglePause()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(dialSize)) {
            val outerMaxStroke = 20.dp.toPx()
            val innerStroke = 6.dp.toPx()
            val outerInset = outerMaxStroke / 2f
            val outerSize = Size(size.width - outerMaxStroke, size.height - outerMaxStroke)
            val outerTopLeft = Offset(outerInset, outerInset)
            val innerInset = outerMaxStroke + 22.dp.toPx()
            val innerSize = Size(size.width - innerInset * 2f, size.height - innerInset * 2f)
            val innerTopLeft = Offset(innerInset, innerInset)
            val totalDuration = safeState.stageSegments.sumOf { segment -> segment.durationSec }
                .coerceAtLeast(1)
            val gapDegrees = if (safeState.stageSegments.size > 1) 3f else 0f

            var startAngle = -90f
            safeState.stageSegments.forEach { segment ->
                val rawSweep = 360f * segment.durationSec.toFloat() / totalDuration.toFloat()
                val visibleSweep = (rawSweep - gapDegrees).coerceAtLeast(0.6f)
                val segmentColor = tokens.colorFor(segment.stageType)
                val progress = if (segment.isCurrent) {
                    animatedStageProgress
                } else {
                    segment.progress
                }
                val strokeWidth = segment.stageType.strokeWidthDp().dp.toPx()
                val activeAlpha = if (segment.isCurrent) 1f else 0.48f

                drawArc(
                    color = tokens.track.copy(alpha = 0.6f),
                    startAngle = startAngle,
                    sweepAngle = visibleSweep,
                    useCenter = false,
                    topLeft = outerTopLeft,
                    size = outerSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                if (progress > 0f) {
                    drawArc(
                        color = segmentColor.copy(alpha = if (safeState.isPaused) 0.5f else activeAlpha),
                        startAngle = startAngle,
                        sweepAngle = visibleSweep * progress,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                if (segment.isCurrent && tokens.glowAlpha > 0f) {
                    drawArc(
                        color = segmentColor.copy(alpha = tokens.glowAlpha + finalPulse * 0.16f),
                        startAngle = startAngle,
                        sweepAngle = visibleSweep * progress,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerSize,
                        style = Stroke(width = strokeWidth + 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                startAngle += rawSweep
            }

            drawArc(
                color = tokens.track.copy(alpha = 0.46f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerSize,
                style = Stroke(width = innerStroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = tokens.totalProgress.copy(alpha = if (safeState.isPaused) 0.52f else 0.95f),
                startAngle = -90f,
                sweepAngle = 360f * animatedTotalProgress,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerSize,
                style = Stroke(width = innerStroke, cap = StrokeCap.Round)
            )
        }

        if (safeState.isFinalCountdown) {
            Surface(
                modifier = Modifier.size(centerSize + (18 * finalPulse).dp),
                shape = CircleShape,
                color = tokens.finalCountdown.copy(alpha = 0.08f + finalPulse * 0.08f),
                border = BorderStroke(1.dp, tokens.finalCountdown.copy(alpha = 0.18f + finalPulse * 0.18f))
            ) {}
        }

        Surface(
            modifier = Modifier.size(centerSize),
            shape = CircleShape,
            color = if (safeState.isPaused) tokens.pausedOverlay else tokens.centerSurface,
            border = BorderStroke(
                width = 1.dp,
                color = currentCenterBorderColor(safeState, tokens)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TimerDialStageGlyph(
                    stageType = safeState.currentStageType,
                    color = tokens.colorFor(safeState.currentStageType),
                    size = if (skin.isBigType) 34.dp else 30.dp
                )
                Text(
                    text = if (safeState.currentStageIndex > 0) {
                        "阶段 ${safeState.currentStageIndex.toString().padStart(2, '0')}"
                    } else {
                        "准备"
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.textSecondary
                )
                Text(
                    text = safeState.currentStageLabel,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = safeState.currentStageTimeText,
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = if (skin.isBigType) 70.sp else 58.sp,
                    lineHeight = if (skin.isBigType) 72.sp else 60.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (safeState.isFinalCountdown) tokens.finalCountdown else tokens.textPrimary
                )
                Text(
                    text = if (safeState.isPaused) "已暂停" else safeState.centerActionLabel,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.textSecondary
                )
            }
        }
    }
}

private fun currentCenterBorderColor(
    state: TimerDialUiState,
    tokens: TimerDialVisualTokens
): Color {
    return when {
        state.isFinalCountdown -> tokens.finalCountdown.copy(alpha = 0.62f)
        state.isPaused -> tokens.textSecondary.copy(alpha = 0.28f)
        else -> tokens.colorFor(state.currentStageType).copy(alpha = 0.34f)
    }
}

@Composable
private fun TimerDialStageGlyph(
    stageType: TimerDialStageType,
    color: Color,
    size: Dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 3.dp.toPx()
        when (stageType) {
            TimerDialStageType.WARMUP -> {
                drawCircle(color = color, radius = size.toPx() * 0.22f)
                listOf(0f, 90f, 180f, 270f).forEach { angle ->
                    val radians = Math.toRadians(angle.toDouble())
                    val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                    val start = Offset(
                        x = center.x + kotlin.math.cos(radians).toFloat() * size.toPx() * 0.32f,
                        y = center.y + kotlin.math.sin(radians).toFloat() * size.toPx() * 0.32f
                    )
                    val end = Offset(
                        x = center.x + kotlin.math.cos(radians).toFloat() * size.toPx() * 0.46f,
                        y = center.y + kotlin.math.sin(radians).toFloat() * size.toPx() * 0.46f
                    )
                    drawLine(color = color, start = start, end = end, strokeWidth = stroke)
                }
            }
            TimerDialStageType.WORK -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.toPx() * 0.34f, size.toPx() * 0.12f)
                    lineTo(size.toPx() * 0.68f, size.toPx() * 0.45f)
                    lineTo(size.toPx() * 0.52f, size.toPx() * 0.45f)
                    lineTo(size.toPx() * 0.66f, size.toPx() * 0.88f)
                    lineTo(size.toPx() * 0.30f, size.toPx() * 0.52f)
                    lineTo(size.toPx() * 0.48f, size.toPx() * 0.52f)
                    close()
                }
                drawPath(path, color)
            }
            TimerDialStageType.REST -> {
                drawLine(
                    color = color,
                    start = Offset(size.toPx() * 0.38f, size.toPx() * 0.22f),
                    end = Offset(size.toPx() * 0.38f, size.toPx() * 0.78f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(size.toPx() * 0.62f, size.toPx() * 0.22f),
                    end = Offset(size.toPx() * 0.62f, size.toPx() * 0.78f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
            TimerDialStageType.COOLDOWN -> {
                drawArc(
                    color = color,
                    startAngle = 30f,
                    sweepAngle = 280f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = color.copy(alpha = 0.34f),
                    radius = size.toPx() * 0.2f,
                    center = Offset(size.toPx() * 0.5f, size.toPx() * 0.5f)
                )
            }
            TimerDialStageType.CUSTOM -> {
                drawCircle(
                    color = color,
                    radius = size.toPx() * 0.34f,
                    style = Stroke(width = stroke)
                )
                drawCircle(color = color, radius = size.toPx() * 0.08f)
            }
        }
    }
}
