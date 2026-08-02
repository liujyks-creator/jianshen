package com.liujyks.trainflow.ui.shell.official

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowAction
import com.liujyks.trainflow.ui.theme.TrainFlowError
import com.liujyks.trainflow.ui.theme.TrainFlowFocus
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral200
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral500
import kotlin.math.roundToInt

internal enum class HeartRateCapsuleExclusionPolicy {
    STANDARD,
    BOTTOM_NAV,
    TIMED_SESSION,
    STRENGTH_SESSION,
    IME_VISIBLE
}

@Composable
internal fun HeartRateFloatingCapsuleOverlay(
    uiState: HeartRateFloatingCapsuleUiState,
    exclusionPolicy: HeartRateCapsuleExclusionPolicy,
    onOpenHeartRateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.visible) return

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val viewport = with(density) {
            HeartRateCapsuleViewport(
                width = maxWidth.toPx(),
                height = maxHeight.toPx()
            )
        }
        val edgeMargin = with(density) { 14.dp.toPx() }
        val topInset = WindowInsets.statusBars.getTop(density).toFloat()
        val navInset = WindowInsets.navigationBars.getBottom(density).toFloat()
        val imeInset = WindowInsets.ime.getBottom(density).toFloat()
        val effectivePolicy = if (imeInset > 0f || uiState.forceCollapsed) {
            HeartRateCapsuleExclusionPolicy.IME_VISIBLE
        } else {
            exclusionPolicy
        }
        val safeInsets = HeartRateCapsuleSafeInsets(
            top = topInset,
            bottom = navInset.coerceAtLeast(imeInset)
        )
        val exclusionZones = heartRateCapsuleExclusionZones(
            viewport = viewport,
            navInset = navInset,
            imeInset = imeInset,
            densityScale = density.density,
            policy = effectivePolicy
        )
        val regularExpandedSize = with(density) {
            HeartRateCapsuleSize(width = 276.dp.toPx(), height = 214.dp.toPx())
        }
        val compactExpandedSize = with(density) {
            HeartRateCapsuleSize(width = 252.dp.toPx(), height = 190.dp.toPx())
        }
        val regularExpandedFits = effectivePolicy != HeartRateCapsuleExclusionPolicy.IME_VISIBLE &&
            hasSafeHeartRateCapsulePlacement(
                capsuleSize = regularExpandedSize,
                viewport = viewport,
                safeInsets = safeInsets,
                exclusionZones = exclusionZones,
                edgeMargin = edgeMargin
            )
        val compactExpandedFits = effectivePolicy != HeartRateCapsuleExclusionPolicy.IME_VISIBLE &&
            hasSafeHeartRateCapsulePlacement(
                capsuleSize = compactExpandedSize,
                viewport = viewport,
                safeInsets = safeInsets,
                exclusionZones = exclusionZones,
                edgeMargin = edgeMargin
            )
        var expanded by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(effectivePolicy, expanded, compactExpandedFits) {
            if (
                (effectivePolicy == HeartRateCapsuleExclusionPolicy.IME_VISIBLE || !compactExpandedFits) &&
                expanded
            ) {
                expanded = false
            }
        }
        val renderedExpanded = expanded &&
            effectivePolicy != HeartRateCapsuleExclusionPolicy.IME_VISIBLE &&
            compactExpandedFits
        val compactExpanded = renderedExpanded && !regularExpandedFits
        var capsuleSize by remember { mutableStateOf(HeartRateCapsuleSize(0f, 0f)) }
        var capsuleX by rememberSaveable { mutableStateOf(Float.NaN) }
        var capsuleY by rememberSaveable { mutableStateOf(Float.NaN) }
        var snapEdge by rememberSaveable { mutableStateOf(HeartRateCapsuleSnapEdge.RIGHT) }

        fun snapFrom(point: HeartRateCapsulePoint): HeartRateCapsulePlacement {
            return snapHeartRateCapsuleToSafeEdge(
                releasePoint = point,
                capsuleSize = capsuleSize,
                viewport = viewport,
                safeInsets = safeInsets,
                exclusionZones = exclusionZones,
                edgeMargin = edgeMargin
            )
        }

        LaunchedEffect(
            viewport,
            capsuleSize,
            uiState.collapsedLabel,
            renderedExpanded,
            effectivePolicy
        ) {
            if (capsuleSize.width > 0f && viewport.width > 0f && viewport.height > 0f) {
                val currentPoint = if (capsuleX.isNaN() || capsuleY.isNaN()) {
                    HeartRateCapsulePoint(
                        x = if (snapEdge == HeartRateCapsuleSnapEdge.LEFT) {
                            edgeMargin
                        } else {
                            viewport.width
                        },
                        y = topInset + with(density) { 82.dp.toPx() }
                    )
                } else {
                    HeartRateCapsulePoint(capsuleX, capsuleY)
                }
                val placement = snapFrom(currentPoint)
                capsuleX = placement.point.x
                capsuleY = placement.point.y
                snapEdge = placement.edge
            }
        }

        HeartRateFloatingCapsule(
            uiState = uiState,
            expanded = renderedExpanded,
            compactExpanded = compactExpanded,
            modifier = Modifier
                .align(Alignment.TopStart)
                .onSizeChanged { size ->
                    capsuleSize = HeartRateCapsuleSize(
                        width = size.width.toFloat(),
                        height = size.height.toFloat()
                    )
                }
                .then(
                    if (!capsuleX.isNaN() && !capsuleY.isNaN()) {
                        Modifier.offsetPx(capsuleX, capsuleY)
                    } else {
                        Modifier
                    }
                )
                .heartRateCapsulePointerInput(
                    thresholdPx = with(density) { 10.dp.toPx() },
                    onTap = { expanded = !expanded },
                    onDragBy = { delta ->
                        capsuleX = (capsuleX.takeUnless { it.isNaN() } ?: 0f)
                            .plus(delta.x)
                            .coerceIn(0f, (viewport.width - capsuleSize.width).coerceAtLeast(0f))
                        capsuleY = (capsuleY.takeUnless { it.isNaN() } ?: 0f)
                            .plus(delta.y)
                            .coerceIn(0f, (viewport.height - capsuleSize.height).coerceAtLeast(0f))
                    },
                    onDragEnd = {
                        if (capsuleSize.width > 0f) {
                            val placement = snapFrom(HeartRateCapsulePoint(capsuleX, capsuleY))
                            capsuleX = placement.point.x
                            capsuleY = placement.point.y
                            snapEdge = placement.edge
                        }
                    }
                )
        )
    }
}

@Composable
private fun HeartRateFloatingCapsule(
    uiState: HeartRateFloatingCapsuleUiState,
    expanded: Boolean,
    compactExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val tone = uiState.status.heartRateCapsuleAccentColor()
    val background = heartRateCapsuleBackgroundColor(
        status = uiState.status,
        surface = MaterialTheme.colorScheme.surface
    )
    val foreground = heartRateCapsuleForegroundColor(background)
    val expandedMaxWidth = if (compactExpanded) 252.dp else 276.dp
    val expandedMaxHeight = if (compactExpanded) 190.dp else 214.dp
    val shape = RoundedCornerShape(if (expanded) 20.dp else 999.dp)
    val zonePresentation = uiState.status.usesZoneTint()
    Surface(
        modifier = modifier
            .widthIn(min = 116.dp, max = if (expanded) expandedMaxWidth else 180.dp)
            .then(if (expanded) Modifier.heightIn(max = expandedMaxHeight) else Modifier)
            .animateContentSize(animationSpec = tween(durationMillis = 220))
            .then(
                if (zonePresentation) {
                    Modifier.shadow(
                        elevation = 7.dp,
                        shape = shape,
                        ambientColor = tone.copy(alpha = HEART_RATE_CAPSULE_ZONE_HALO_ALPHA),
                        spotColor = tone.copy(alpha = HEART_RATE_CAPSULE_ZONE_HALO_ALPHA)
                    )
                } else {
                    Modifier
                }
            )
            .background(
                brush = Brush.horizontalGradient(
                    colors = heartRateCapsuleGradientStops(uiState.status, MaterialTheme.colorScheme.surface)
                ),
                shape = shape
            )
            .semantics {
                contentDescription = "心率胶囊：${uiState.collapsedLabel}"
                role = Role.Button
            },
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, tone.copy(alpha = 0.42f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (expanded) 12.dp else 13.dp,
                vertical = if (expanded) 10.dp else 9.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (expanded) 7.dp else 8.dp)
        ) {
            if (expanded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tone)
                    )
                    Text(
                        text = uiState.collapsedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tone)
                    )
                    Text(
                        text = uiState.collapsedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (expanded) {
                Text(
                    text = uiState.detailTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                HeartRateCapsuleInfoGrid(
                    tiles = uiState.infoTiles,
                    compact = compactExpanded,
                    tone = tone,
                    foreground = foreground
                )
                Text(
                    text = uiState.detailBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = foreground.copy(alpha = 0.78f),
                    maxLines = if (compactExpanded) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HeartRateCapsuleInfoGrid(
    tiles: List<HeartRateFloatingCapsuleInfoTile>,
    compact: Boolean,
    tone: Color,
    foreground: Color
) {
    val displayTiles = if (tiles.size >= 4) {
        tiles.take(4)
    } else {
        tiles + List(4 - tiles.size) { HeartRateFloatingCapsuleInfoTile(label = "-", value = "-") }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        displayTiles.chunked(2).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTiles.forEach { tile ->
                    HeartRateCapsuleInfoTile(
                        tile = tile,
                        compact = compact,
                        tone = tone,
                        foreground = foreground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeartRateCapsuleInfoTile(
    tile: HeartRateFloatingCapsuleInfoTile,
    compact: Boolean,
    tone: Color,
    foreground: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = tone.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (compact) 6.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tile.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = foreground.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tile.value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal const val HEART_RATE_CAPSULE_ZONE_TINT_ALPHA = 0.16f
internal const val HEART_RATE_CAPSULE_ZONE_HALO_ALPHA = 0.16f

internal fun HeartRateFloatingCapsuleStatus.heartRateCapsuleAccentColor(): Color {
    return when (this) {
        HeartRateFloatingCapsuleStatus.PERMISSION_DENIED,
        HeartRateFloatingCapsuleStatus.BLUETOOTH_DISABLED,
        HeartRateFloatingCapsuleStatus.CONNECTING -> Color(0xFFD9921E)
        HeartRateFloatingCapsuleStatus.WAITING_DATA,
        HeartRateFloatingCapsuleStatus.BPM_ONLY -> TrainFlowFocus
        HeartRateFloatingCapsuleStatus.ZONE_LOW -> Color(0xFF7A8EA1)
        HeartRateFloatingCapsuleStatus.ZONE_WARMUP -> Color(0xFF4F8FDB)
        HeartRateFloatingCapsuleStatus.ZONE_FAT_BURN -> TrainFlowAccent
        HeartRateFloatingCapsuleStatus.ZONE_AEROBIC -> Color(0xFFD4A62A)
        HeartRateFloatingCapsuleStatus.ZONE_ANAEROBIC -> TrainFlowAction
        HeartRateFloatingCapsuleStatus.ZONE_LIMIT -> TrainFlowError
        HeartRateFloatingCapsuleStatus.OVER_LIMIT -> Color(0xFF7A1118)
        HeartRateFloatingCapsuleStatus.STALE,
        HeartRateFloatingCapsuleStatus.OFFLINE,
        HeartRateFloatingCapsuleStatus.SAVED_DEVICE -> TrainFlowNeutral500
        HeartRateFloatingCapsuleStatus.ERROR -> TrainFlowError
        HeartRateFloatingCapsuleStatus.NO_SOURCE,
        HeartRateFloatingCapsuleStatus.HIDDEN -> TrainFlowNeutral200
    }
}

internal fun heartRateCapsuleBackgroundColor(
    status: HeartRateFloatingCapsuleStatus,
    surface: Color
): Color {
    if (!status.usesZoneTint()) return surface.copy(alpha = 0.96f)
    val accent = status.heartRateCapsuleAccentColor()
    val alpha = HEART_RATE_CAPSULE_ZONE_TINT_ALPHA
    return Color(
        red = accent.red * alpha + surface.red * (1f - alpha),
        green = accent.green * alpha + surface.green * (1f - alpha),
        blue = accent.blue * alpha + surface.blue * (1f - alpha),
        alpha = 1f
    )
}

internal fun heartRateCapsuleGradientStops(
    status: HeartRateFloatingCapsuleStatus,
    surface: Color
): List<Color> {
    val center = heartRateCapsuleBackgroundColor(status, surface)
    if (!status.usesZoneTint()) return listOf(center, center)
    val edge = blendHeartRateCapsuleColor(
        foreground = status.heartRateCapsuleAccentColor(),
        background = surface,
        alpha = HEART_RATE_CAPSULE_ZONE_TINT_ALPHA * 0.58f
    )
    return listOf(edge, center, edge)
}

private fun blendHeartRateCapsuleColor(
    foreground: Color,
    background: Color,
    alpha: Float
): Color = Color(
    red = foreground.red * alpha + background.red * (1f - alpha),
    green = foreground.green * alpha + background.green * (1f - alpha),
    blue = foreground.blue * alpha + background.blue * (1f - alpha),
    alpha = 1f
)

internal fun heartRateCapsuleForegroundColor(background: Color): Color {
    val dark = Color(0xFF15171A)
    val light = Color(0xFFFFFFFF)
    return if (
        heartRateCapsuleContrastRatio(dark, background) >=
        heartRateCapsuleContrastRatio(light, background)
    ) {
        dark
    } else {
        light
    }
}

internal fun heartRateCapsuleContrastRatio(
    foreground: Color,
    background: Color
): Double {
    val lighter = maxOf(foreground.luminance(), background.luminance()).toDouble()
    val darker = minOf(foreground.luminance(), background.luminance()).toDouble()
    return (lighter + 0.05) / (darker + 0.05)
}

private fun HeartRateFloatingCapsuleStatus.usesZoneTint(): Boolean = this in setOf(
    HeartRateFloatingCapsuleStatus.ZONE_LOW,
    HeartRateFloatingCapsuleStatus.ZONE_WARMUP,
    HeartRateFloatingCapsuleStatus.ZONE_FAT_BURN,
    HeartRateFloatingCapsuleStatus.ZONE_AEROBIC,
    HeartRateFloatingCapsuleStatus.ZONE_ANAEROBIC,
    HeartRateFloatingCapsuleStatus.ZONE_LIMIT,
    HeartRateFloatingCapsuleStatus.OVER_LIMIT
)

private fun Modifier.offsetPx(
    x: Float,
    y: Float
): Modifier {
    return offset {
        IntOffset(x.roundToInt(), y.roundToInt())
    }
}

private fun Modifier.heartRateCapsulePointerInput(
    thresholdPx: Float,
    onTap: () -> Unit,
    onDragBy: (Offset) -> Unit,
    onDragEnd: () -> Unit
): Modifier {
    return pointerInput(thresholdPx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var totalDx = 0f
            var totalDy = 0f
            var dragging = false
            var current: PointerInputChange?
            do {
                val event = awaitPointerEvent()
                current = event.changes.firstOrNull { change -> change.id == down.id }
                if (current != null && current.pressed) {
                    val delta = current.positionChange()
                    totalDx += delta.x
                    totalDy += delta.y
                    if (!dragging && hasMovedBeyondHeartRateCapsuleDragThreshold(totalDx, totalDy, thresholdPx)) {
                        dragging = true
                    }
                    if (dragging) {
                        current.consume()
                        onDragBy(delta)
                    }
                }
            } while (current?.pressed == true)
            if (dragging) {
                onDragEnd()
            } else {
                onTap()
            }
        }
    }
}

private fun heartRateCapsuleExclusionZones(
    viewport: HeartRateCapsuleViewport,
    navInset: Float,
    imeInset: Float,
    densityScale: Float,
    policy: HeartRateCapsuleExclusionPolicy
): List<HeartRateCapsuleExclusionZone> {
    fun dp(value: Float) = value * densityScale
    val zones = mutableListOf<HeartRateCapsuleExclusionZone>()
    if (navInset > 0f || policy == HeartRateCapsuleExclusionPolicy.BOTTOM_NAV) {
        zones += HeartRateCapsuleExclusionZone(
            left = 0f,
            top = viewport.height - navInset - dp(82f),
            right = viewport.width,
            bottom = viewport.height
        )
    }
    when (policy) {
        HeartRateCapsuleExclusionPolicy.TIMED_SESSION -> {
            zones += HeartRateCapsuleExclusionZone(
                left = 0f,
                top = viewport.height - navInset - dp(168f),
                right = viewport.width,
                bottom = viewport.height
            )
        }

        HeartRateCapsuleExclusionPolicy.STRENGTH_SESSION -> {
            zones += HeartRateCapsuleExclusionZone(
                left = 0f,
                top = viewport.height - navInset - dp(440f),
                right = viewport.width,
                bottom = viewport.height
            )
        }

        HeartRateCapsuleExclusionPolicy.IME_VISIBLE -> {
            zones += HeartRateCapsuleExclusionZone(
                left = 0f,
                top = viewport.height - imeInset - dp(420f),
                right = viewport.width,
                bottom = viewport.height
            )
        }

        HeartRateCapsuleExclusionPolicy.STANDARD,
        HeartRateCapsuleExclusionPolicy.BOTTOM_NAV -> Unit
    }
    return zones
}
