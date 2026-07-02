package com.liujyks.trainflow.feature.workoutsession

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
import com.liujyks.trainflow.ui.theme.LocalTrainFlowReduceMotion
import com.liujyks.trainflow.ui.theme.isBigType
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun TimerDial(
    state: TimerDialUiState,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeState = state.clamped()
    val skin = LocalTrainFlowSkin.current
    val reduceMotion = LocalTrainFlowReduceMotion.current
    val tokens = safeState.visualVariant.tokens(skin)
    val layoutSpec = skin.timerDialLayoutSpec()
    val currentStageColor = safeState.currentStageColorHex.toComposeColor(tokens.colorFor(safeState.currentStageType))
    val currentStageTextColor = safeState.currentStageTextColorHex.toComposeColor(tokens.textPrimary)
    val smoothProgressIdentity = safeState.smoothProgressIdentity()
    val smoothProgressAnchor = safeState.smoothProgressAnchor()
    var smoothProgressFrameNanos by remember(smoothProgressIdentity, reduceMotion) { mutableLongStateOf(0L) }
    var smoothProgressAnchorNanos by remember(smoothProgressIdentity, reduceMotion) { mutableLongStateOf(0L) }
    var appliedSmoothProgressAnchor by remember(smoothProgressIdentity, reduceMotion) {
        mutableStateOf(smoothProgressAnchor)
    }
    var previousDisplayedProgress by remember(smoothProgressIdentity, reduceMotion) {
        mutableStateOf(
            safeState.monotonicDisplayedProgress(
                elapsedMillis = 0L,
                reduceMotion = reduceMotion
            )
        )
    }
    LaunchedEffect(smoothProgressIdentity, reduceMotion) {
        smoothProgressFrameNanos = 0L
        smoothProgressAnchorNanos = 0L
        appliedSmoothProgressAnchor = smoothProgressAnchor
        if (safeState.canProjectSmoothProgress(reduceMotion)) {
            val startedAtNanos = withFrameNanos { frameTimeNanos -> frameTimeNanos }
            smoothProgressFrameNanos = startedAtNanos
            smoothProgressAnchorNanos = startedAtNanos
            appliedSmoothProgressAnchor = smoothProgressAnchor
            while (true) {
                smoothProgressFrameNanos = withFrameNanos { frameTimeNanos -> frameTimeNanos }
            }
        }
    }
    LaunchedEffect(smoothProgressIdentity, smoothProgressAnchor, reduceMotion) {
        if (safeState.canProjectSmoothProgress(reduceMotion)) {
            val anchorNanos = smoothProgressFrameNanos.takeIf { frameNanos -> frameNanos > 0L }
                ?: withFrameNanos { frameTimeNanos -> frameTimeNanos }
            smoothProgressFrameNanos = anchorNanos
            smoothProgressAnchorNanos = anchorNanos
            appliedSmoothProgressAnchor = smoothProgressAnchor
        } else {
            smoothProgressFrameNanos = 0L
            smoothProgressAnchorNanos = 0L
            appliedSmoothProgressAnchor = smoothProgressAnchor
        }
    }
    val smoothProgressElapsedMillis = timerDialSmoothProgressElapsedMillis(
        frameNanos = smoothProgressFrameNanos,
        anchorNanos = smoothProgressAnchorNanos,
        anchorApplied = appliedSmoothProgressAnchor == smoothProgressAnchor
    )
    val displayedProgress = safeState.monotonicDisplayedProgress(
        elapsedMillis = smoothProgressElapsedMillis,
        reduceMotion = reduceMotion,
        previousDisplayed = previousDisplayedProgress
    )
    SideEffect {
        previousDisplayedProgress = displayedProgress
    }
    val animatedTotalProgress = displayedProgress.totalProgress
    val animatedStageProgress = displayedProgress.currentStageProgress
    val finalPulse by animateFloatAsState(
        targetValue = timerDialFinalPulseTarget(
            isFinalCountdown = safeState.isFinalCountdown,
            isPaused = safeState.isPaused,
            reduceMotion = reduceMotion
        ),
        animationSpec = timerDialFinalPulseAnimationSpec(reduceMotion),
        label = "TimerDialFinalPulse"
    )
    val currentSegmentAlpha by animateFloatAsState(
        targetValue = if (safeState.isPaused) 0.5f else 1f,
        animationSpec = timerDialMarkerRingStateTransitionSpec(reduceMotion),
        label = "TimerDialCurrentSegmentAlpha"
    )
    val totalProgressAlpha by animateFloatAsState(
        targetValue = if (safeState.isPaused) 0.52f else 0.96f,
        animationSpec = timerDialMarkerRingStateTransitionSpec(reduceMotion),
        label = "TimerDialTotalProgressAlpha"
    )
    val totalBrushAlpha by animateFloatAsState(
        targetValue = if (safeState.isPaused) 0.62f else 1f,
        animationSpec = timerDialMarkerRingStateTransitionSpec(reduceMotion),
        label = "TimerDialTotalBrushAlpha"
    )
    val completedDotAlpha by animateFloatAsState(
        targetValue = if (safeState.isPaused) 0.58f else 0.92f,
        animationSpec = timerDialMarkerRingStateTransitionSpec(reduceMotion),
        label = "TimerDialCompletedDotAlpha"
    )
    val centerColor by animateColorAsState(
        targetValue = if (safeState.isPaused) {
            currentStageColor.copy(alpha = 0.38f)
        } else {
            currentStageColor
        },
        animationSpec = timerDialColorStateTransitionSpec(reduceMotion),
        label = "TimerDialCenterColor"
    )
    val centerBorderColor by animateColorAsState(
        targetValue = currentCenterBorderColor(safeState, tokens),
        animationSpec = timerDialColorStateTransitionSpec(reduceMotion),
        label = "TimerDialCenterBorderColor"
    )
    val centerInteractionSource = remember { MutableInteractionSource() }
    val centerPressed by centerInteractionSource.collectIsPressedAsState()
    val centerScale by animateFloatAsState(
        targetValue = timerDialCenterTouchScaleTarget(
            pressed = centerPressed,
            canTogglePause = safeState.canTogglePause,
            reduceMotion = reduceMotion
        ),
        animationSpec = timerDialTouchFeedbackSpec(reduceMotion),
        label = "TimerDialCenterTouchScale"
    )
    val centerIndication = if (reduceMotion) null else LocalIndication.current
    val dialSize = layoutSpec.dialSizeDp.dp
    val centerSize = layoutSpec.centerSizeDp.dp
    val centerPadding = if (skin.isBigType) 10.dp else 8.dp
    val timerFontSize = if (skin.isBigType) 48.sp else 44.sp
    val timerLineHeight = if (skin.isBigType) 50.sp else 46.sp
    val glyphSize = when {
        safeState.isPaused && skin.isBigType -> 44.dp
        safeState.isPaused -> 38.dp
        skin.isBigType -> 38.dp
        else -> 32.dp
    }
    val roundFontSize = when {
        safeState.isPaused && skin.isBigType -> 30.sp
        safeState.isPaused -> 26.sp
        skin.isBigType -> 26.sp
        else -> 23.sp
    }
    val roundLineHeight = when {
        skin.isBigType -> 32.sp
        else -> 28.sp
    }
    val contentDescription = safeState.accessibilityDescription()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = layoutSpec.minHeightDp.dp)
            .semantics {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.requiredSize(dialSize)) {
            val outerSafeStroke = layoutSpec.outerSafeStrokeDp.dp.toPx()
            val innerStroke = layoutSpec.innerStrokeDp.dp.toPx()
            val innerBaseStroke = layoutSpec.innerBaseStrokeDp.dp.toPx()
            val outerInset = outerSafeStroke / 2f
            val outerSize = Size(size.width - outerSafeStroke, size.height - outerSafeStroke)
            val outerTopLeft = Offset(outerInset, outerInset)
            val innerInset = layoutSpec.innerInsetDp.dp.toPx()
            val innerSize = Size(size.width - innerInset * 2f, size.height - innerInset * 2f)
            val innerTopLeft = Offset(innerInset, innerInset)
            val totalDuration = safeState.stageSegments.sumOf { segment -> segment.durationSec }
                .coerceAtLeast(1)
            val gapDegrees = if (safeState.stageSegments.size > 1) 3f else 0f

            var startAngle = -90f
            safeState.stageSegments.forEach { segment ->
                val rawSweep = 360f * segment.durationSec.toFloat() / totalDuration.toFloat()
                val visibleSweep = (rawSweep - gapDegrees).coerceAtLeast(0.6f)
                val segmentColor = segment.colorHex.toComposeColor(tokens.colorFor(segment.stageType))
                val progress = if (segment.isCurrent) animatedStageProgress else segment.progress
                val strokeWidth = segment.strokeWidthDp().dp.toPx()
                val activeAlpha = if (segment.isCurrent) currentSegmentAlpha else 0.48f

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
                        color = segmentColor.copy(alpha = activeAlpha),
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

            val innerCenter = Offset(
                x = innerTopLeft.x + innerSize.width / 2f,
                y = innerTopLeft.y + innerSize.height / 2f
            )
            val innerRadius = innerSize.width / 2f
            val innerMarkers = safeState.innerMarkerData()
            val markerProgress = animatedTotalProgress.coerceIn(0f, 1f)

            drawArc(
                color = tokens.innerBaseRing,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerSize,
                style = Stroke(width = innerBaseStroke, cap = StrokeCap.Round)
            )
            innerMarkers.forEach { marker ->
                drawCircle(
                    color = tokens.innerBaseDot,
                    radius = layoutSpec.innerBaseDotRadiusDp.dp.toPx(),
                    center = pointOnCircle(innerCenter, innerRadius, marker.progress)
                )
            }

            if (markerProgress > 0f) {
                drawArc(
                    color = tokens.totalProgress.copy(alpha = totalProgressAlpha),
                    startAngle = -90f,
                    sweepAngle = 360f * markerProgress,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = tokens.totalProgress.copy(alpha = totalBrushAlpha),
                    radius = layoutSpec.totalBrushRadiusDp.dp.toPx(),
                    center = pointOnCircle(innerCenter, innerRadius, markerProgress)
                )
            }

            innerMarkers.forEach { marker ->
                val center = pointOnCircle(innerCenter, innerRadius, marker.progress)
                when (marker.role) {
                    TimerDialInnerMarkerRole.TOTAL_COUNT -> {
                        drawTimerDialMarker(
                            center = center,
                            radius = layoutSpec.innerMarkerRadiusDp.dp.toPx(),
                            fillColor = tokens.work,
                            textColor = tokens.textPrimary,
                            text = marker.label.orEmpty()
                        )
                    }
                    TimerDialInnerMarkerRole.COMPLETED_DOT -> {
                        drawCircle(
                            color = tokens.totalProgress.copy(alpha = completedDotAlpha),
                            radius = layoutSpec.innerCompletedDotRadiusDp.dp.toPx(),
                            center = center
                        )
                    }
                    TimerDialInnerMarkerRole.LATEST_COMPLETED -> {
                        drawTimerDialMarker(
                            center = center,
                            radius = layoutSpec.innerMarkerRadiusDp.dp.toPx(),
                            fillColor = tokens.textPrimary,
                            textColor = tokens.work,
                            text = marker.label.orEmpty()
                        )
                    }
                    TimerDialInnerMarkerRole.BASE_DOT -> Unit
                }
            }

            if (safeState.isPaused) {
                drawCircle(
                    color = tokens.textSecondary.copy(alpha = 0.28f),
                    radius = size.minDimension / 2f - 2.dp.toPx(),
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 12.dp.toPx()))
                    )
                )
            }
        }

        if (safeState.isFinalCountdown && !reduceMotion) {
            Surface(
                modifier = Modifier.size(centerSize + (18 * finalPulse).dp),
                shape = CircleShape,
                color = tokens.finalCountdown.copy(alpha = 0.08f + finalPulse * 0.08f),
                border = BorderStroke(1.dp, tokens.finalCountdown.copy(alpha = 0.18f + finalPulse * 0.18f))
            ) {}
        }

        Surface(
            modifier = Modifier
                .size(centerSize)
                .graphicsLayer {
                    scaleX = centerScale
                    scaleY = centerScale
                }
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                }
                .clickable(
                    interactionSource = centerInteractionSource,
                    indication = centerIndication,
                    enabled = safeState.canTogglePause,
                    role = Role.Button,
                    onClick = onTogglePause
                ),
            shape = CircleShape,
            color = centerColor,
            border = BorderStroke(
                width = 1.dp,
                color = centerBorderColor
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = centerPadding, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Crossfade(
                    targetState = safeState.isPaused,
                    animationSpec = timerDialPlayPauseStateTransitionSpec(reduceMotion),
                    label = "TimerDialPlayPauseGlyph"
                ) { isPaused ->
                    TimerDialCenterGlyph(
                        isPaused = isPaused,
                        iconKey = safeState.currentStageIconKey,
                        stageType = safeState.currentStageType,
                        color = Color.White,
                        size = glyphSize
                    )
                }
                if (safeState.currentStageIndex > 0) {
                    Text(
                        text = safeState.currentStageIndex.toString(),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            fontSize = roundFontSize,
                            lineHeight = roundLineHeight
                        ),
                        color = currentStageTextColor.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                Text(
                    text = safeState.currentStageTimeText,
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = timerFontSize,
                    lineHeight = timerLineHeight,
                    fontWeight = FontWeight.ExtraBold,
                    color = currentStageTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

private fun pointOnCircle(
    center: Offset,
    radius: Float,
    progress: Float
): Offset {
    val angleRadians = Math.toRadians((progress.coerceIn(0f, 1f) * 360f - 90f).toDouble())
    return Offset(
        x = center.x + cos(angleRadians).toFloat() * radius,
        y = center.y + sin(angleRadians).toFloat() * radius
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimerDialMarker(
    center: Offset,
    radius: Float,
    fillColor: Color,
    textColor: Color,
    text: String
) {
    drawCircle(color = fillColor, radius = radius, center = center)
    drawContext.canvas.nativeCanvas.drawText(
        text,
        center.x,
        center.y - (markerTextPaint.descent() + markerTextPaint.ascent()) / 2f,
        markerTextPaint.apply {
            color = textColor.toArgb()
            textSize = radius * 1.42f
        }
    )
}

private val markerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

private fun currentCenterBorderColor(
    state: TimerDialUiState,
    tokens: TimerDialVisualTokens
): Color {
    val currentStageColor = state.currentStageColorHex.toComposeColor(tokens.colorFor(state.currentStageType))
    return when {
        state.isFinalCountdown -> tokens.finalCountdown.copy(alpha = 0.62f)
        state.isPaused -> tokens.textSecondary.copy(alpha = 0.28f)
        else -> currentStageColor.copy(alpha = 0.34f)
    }
}

private fun String.toComposeColor(defaultColor: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(this)) }
        .getOrElse { defaultColor }
}

@Composable
private fun TimerDialCenterGlyph(
    isPaused: Boolean,
    iconKey: String,
    stageType: TimerDialStageType,
    color: Color,
    size: Dp
) {
    if (isPaused) {
        TimerDialResumeGlyph(color = color, size = size)
    } else {
        TimerDialStageGlyph(iconKey = iconKey, stageType = stageType, color = color, size = size)
    }
}

@Composable
private fun TimerDialResumeGlyph(
    color: Color,
    size: Dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.toPx() * 0.36f, size.toPx() * 0.2f)
            lineTo(size.toPx() * 0.36f, size.toPx() * 0.8f)
            lineTo(size.toPx() * 0.78f, size.toPx() * 0.5f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun TimerDialStageGlyph(
    iconKey: String,
    stageType: TimerDialStageType,
    color: Color,
    size: Dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = (this.size.minDimension * 0.1f).coerceAtLeast(2.dp.toPx())
        val resolvedIconKey = iconKey.timerDialIconKey(stageType)
        when (resolvedIconKey) {
            "warmup" -> drawWarmupFlameIcon(color, stroke)
            "work" -> drawWorkActionIcon(color, stroke)
            "speed_up" -> drawSpeedUpArrowIcon(color, stroke)
            "sprint" -> drawSprintLightningIcon(color, stroke)
            "rest" -> drawRestSnowflakeIcon(color, stroke)
            "recover_breathe" -> drawRoundRecoveryIcon(color, stroke)
            "cooldown" -> drawCooldownDownshiftIcon(color, stroke)
            "strength" -> drawStrengthDumbbellIcon(color, stroke)
            "mobility" -> drawMobilityJointIcon(color, stroke)
            else -> drawCustomStageIcon(color, stroke)
        }
    }
}

private fun DrawScope.drawWarmupFlameIcon(color: Color, stroke: Float) {
    val w = size.width
    val h = size.height
    val flame = Path().apply {
        moveTo(w * 0.52f, h * 0.1f)
        cubicTo(w * 0.74f, h * 0.28f, w * 0.82f, h * 0.46f, w * 0.72f, h * 0.66f)
        cubicTo(w * 0.63f, h * 0.84f, w * 0.46f, h * 0.92f, w * 0.33f, h * 0.82f)
        cubicTo(w * 0.2f, h * 0.72f, w * 0.2f, h * 0.52f, w * 0.33f, h * 0.36f)
        cubicTo(w * 0.42f, h * 0.25f, w * 0.4f, h * 0.16f, w * 0.52f, h * 0.1f)
        close()
    }
    drawPath(flame, color)
    drawLine(
        color = color.copy(alpha = 0.72f),
        start = Offset(w * 0.5f, h * 0.36f),
        end = Offset(w * 0.43f, h * 0.7f),
        strokeWidth = stroke * 0.58f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawWorkActionIcon(color: Color, stroke: Float) {
    val w = size.width
    val h = size.height
    drawCircle(color = color, radius = w * 0.08f, center = Offset(w * 0.52f, h * 0.18f))
    drawLine(color, Offset(w * 0.5f, h * 0.3f), Offset(w * 0.42f, h * 0.52f), stroke)
    drawLine(color, Offset(w * 0.45f, h * 0.38f), Offset(w * 0.26f, h * 0.46f), stroke)
    drawLine(color, Offset(w * 0.46f, h * 0.38f), Offset(w * 0.66f, h * 0.46f), stroke)
    drawLine(color, Offset(w * 0.42f, h * 0.52f), Offset(w * 0.27f, h * 0.8f), stroke)
    drawLine(color, Offset(w * 0.42f, h * 0.52f), Offset(w * 0.69f, h * 0.74f), stroke)
}

private fun DrawScope.drawSpeedUpArrowIcon(color: Color, stroke: Float) {
    val w = size.width
    val h = size.height
    listOf(0.32f, 0.5f, 0.68f).forEachIndexed { index, y ->
        drawLine(
            color = color,
            start = Offset(w * (0.16f + index * 0.08f), h * y),
            end = Offset(w * 0.62f, h * y),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
    val arrow = Path().apply {
        moveTo(w * 0.62f, h * 0.22f)
        lineTo(w * 0.86f, h * 0.5f)
        lineTo(w * 0.62f, h * 0.78f)
    }
    drawPath(arrow, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawSprintLightningIcon(color: Color, stroke: Float) {
    val w = size.width
    val h = size.height
    val bolt = Path().apply {
        moveTo(w * 0.58f, h * 0.08f)
        lineTo(w * 0.28f, h * 0.5f)
        lineTo(w * 0.53f, h * 0.5f)
        lineTo(w * 0.38f, h * 0.92f)
        lineTo(w * 0.78f, h * 0.4f)
        lineTo(w * 0.53f, h * 0.4f)
        close()
    }
    drawPath(bolt, color)
    drawLine(color, Offset(w * 0.16f, h * 0.34f), Offset(w * 0.34f, h * 0.34f), stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.14f, h * 0.68f), Offset(w * 0.3f, h * 0.68f), stroke, cap = StrokeCap.Round)
}

private fun DrawScope.drawRestSnowflakeIcon(color: Color, stroke: Float) {
    val w = size.width
    val h = size.height
    val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawLine(color, Offset(w * 0.5f, h * 0.14f), Offset(w * 0.5f, h * 0.86f), stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.2f, h * 0.32f), Offset(w * 0.8f, h * 0.68f), stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.8f, h * 0.32f), Offset(w * 0.2f, h * 0.68f), stroke, cap = StrokeCap.Round)
    drawCircle(color = color, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.5f), style = style)
}

private fun DrawScope.drawRoundRecoveryIcon(color: Color, stroke: Float) {
    val w = size.width
    val h = size.height
    drawArc(
        color = color,
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(w * 0.14f, h * 0.14f),
        size = Size(w * 0.72f, h * 0.72f),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    drawLine(color, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.84f, h * 0.4f), stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.6f, h * 0.26f), stroke, cap = StrokeCap.Round)
    val breath = Path().apply {
        moveTo(w * 0.24f, h * 0.54f)
        cubicTo(w * 0.36f, h * 0.42f, w * 0.44f, h * 0.66f, w * 0.56f, h * 0.54f)
        cubicTo(w * 0.64f, h * 0.46f, w * 0.7f, h * 0.48f, w * 0.78f, h * 0.54f)
    }
    drawPath(breath, color, style = Stroke(width = stroke * 0.78f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawCooldownDownshiftIcon(color: Color, stroke: Float) {
    val w = size.width
    val h = size.height
    drawLine(color, Offset(w * 0.5f, h * 0.14f), Offset(w * 0.5f, h * 0.64f), stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.3f, h * 0.48f), Offset(w * 0.5f, h * 0.66f), stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.7f, h * 0.48f), Offset(w * 0.5f, h * 0.66f), stroke, cap = StrokeCap.Round)
    val wave = Path().apply {
        moveTo(w * 0.18f, h * 0.82f)
        cubicTo(w * 0.32f, h * 0.72f, w * 0.42f, h * 0.92f, w * 0.56f, h * 0.82f)
        cubicTo(w * 0.66f, h * 0.75f, w * 0.74f, h * 0.77f, w * 0.84f, h * 0.82f)
    }
    drawPath(wave, color, style = Stroke(width = stroke * 0.82f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawStrengthDumbbellIcon(color: Color, stroke: Float) {
    val w = size.width
    val h = size.height
    drawLine(color, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), stroke, cap = StrokeCap.Round)
    listOf(0.26f, 0.36f, 0.64f, 0.74f).forEach { x ->
        drawLine(color, Offset(w * x, h * 0.34f), Offset(w * x, h * 0.66f), stroke, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawMobilityJointIcon(color: Color, stroke: Float) {
    val w = size.width
    val h = size.height
    drawCircle(color = color, radius = w * 0.12f, center = Offset(w * 0.32f, h * 0.34f), style = Stroke(width = stroke))
    drawCircle(color = color, radius = w * 0.12f, center = Offset(w * 0.68f, h * 0.66f), style = Stroke(width = stroke))
    drawLine(color, Offset(w * 0.4f, h * 0.42f), Offset(w * 0.6f, h * 0.58f), stroke, cap = StrokeCap.Round)
    drawArc(
        color = color,
        startAngle = 210f,
        sweepAngle = 240f,
        useCenter = false,
        topLeft = Offset(w * 0.18f, h * 0.18f),
        size = Size(w * 0.64f, h * 0.64f),
        style = Stroke(width = stroke * 0.72f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCustomStageIcon(color: Color, stroke: Float) {
    val w = size.width
    val h = size.height
    drawCircle(color = color, radius = w * 0.34f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = stroke))
    drawCircle(color = color, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.5f))
}

private fun String.timerDialIconKey(stageType: TimerDialStageType): String {
    return when (trim()) {
        "warmup",
        "work",
        "speed_up",
        "sprint",
        "rest",
        "recover_breathe",
        "cooldown",
        "strength",
        "mobility",
        "custom" -> trim()
        else -> stageType.defaultTimerDialIconKey()
    }
}

private fun TimerDialStageType.defaultTimerDialIconKey(): String {
    return when (this) {
        TimerDialStageType.WARMUP -> "warmup"
        TimerDialStageType.WORK -> "work"
        TimerDialStageType.REST -> "rest"
        TimerDialStageType.COOLDOWN -> "cooldown"
        TimerDialStageType.CUSTOM -> "custom"
    }
}
