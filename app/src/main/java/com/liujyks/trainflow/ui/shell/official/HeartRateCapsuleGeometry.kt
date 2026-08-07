package com.liujyks.trainflow.ui.shell.official

import kotlin.math.abs

internal data class HeartRateCapsulePoint(
    val x: Float,
    val y: Float
)

internal data class HeartRateCapsuleSize(
    val width: Float,
    val height: Float
)

internal data class HeartRateCapsuleViewport(
    val width: Float,
    val height: Float
)

internal data class HeartRateCapsuleSafeInsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
)

internal data class HeartRateCapsuleExclusionZone(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

internal enum class HeartRateCapsuleSnapEdge {
    LEFT,
    RIGHT
}

internal data class HeartRateCapsulePlacement(
    val point: HeartRateCapsulePoint,
    val edge: HeartRateCapsuleSnapEdge
)

internal fun hasSafeHeartRateCapsulePlacement(
    capsuleSize: HeartRateCapsuleSize,
    viewport: HeartRateCapsuleViewport,
    safeInsets: HeartRateCapsuleSafeInsets,
    exclusionZones: List<HeartRateCapsuleExclusionZone>,
    edgeMargin: Float
): Boolean {
    if (capsuleSize.width <= 0f || capsuleSize.height <= 0f) return false
    val minX = safeInsets.left + edgeMargin
    val maxX = viewport.width - safeInsets.right - edgeMargin - capsuleSize.width
    val minY = safeInsets.top + edgeMargin
    val maxY = viewport.height - safeInsets.bottom - edgeMargin - capsuleSize.height
    if (maxX < minX || maxY < minY) return false

    return listOf(minX, maxX).any { x ->
        nearestSafeY(
            desiredY = minY,
            x = x,
            minY = minY,
            maxY = maxY,
            capsuleSize = capsuleSize,
            exclusionZones = exclusionZones,
            margin = edgeMargin
        ).let { y ->
            !intersectsAnyExclusion(
                x = x,
                y = y,
                capsuleSize = capsuleSize,
                exclusionZones = exclusionZones
            )
        }
    }
}

internal fun snapHeartRateCapsuleToSafeEdge(
    releasePoint: HeartRateCapsulePoint,
    capsuleSize: HeartRateCapsuleSize,
    viewport: HeartRateCapsuleViewport,
    safeInsets: HeartRateCapsuleSafeInsets,
    exclusionZones: List<HeartRateCapsuleExclusionZone>,
    edgeMargin: Float
): HeartRateCapsulePlacement {
    val edge = if (releasePoint.x + capsuleSize.width / 2f < viewport.width / 2f) {
        HeartRateCapsuleSnapEdge.LEFT
    } else {
        HeartRateCapsuleSnapEdge.RIGHT
    }
    return placeHeartRateCapsuleAtSafeEdge(
        desiredPoint = releasePoint,
        edge = edge,
        capsuleSize = capsuleSize,
        viewport = viewport,
        safeInsets = safeInsets,
        exclusionZones = exclusionZones,
        edgeMargin = edgeMargin
    )
}

internal fun placeHeartRateCapsuleAtSafeEdge(
    desiredPoint: HeartRateCapsulePoint,
    edge: HeartRateCapsuleSnapEdge,
    capsuleSize: HeartRateCapsuleSize,
    viewport: HeartRateCapsuleViewport,
    safeInsets: HeartRateCapsuleSafeInsets,
    exclusionZones: List<HeartRateCapsuleExclusionZone>,
    edgeMargin: Float
): HeartRateCapsulePlacement {
    val targetX = when (edge) {
        HeartRateCapsuleSnapEdge.LEFT -> safeInsets.left + edgeMargin
        HeartRateCapsuleSnapEdge.RIGHT -> viewport.width - safeInsets.right - edgeMargin - capsuleSize.width
    }.coerceIn(
        safeInsets.left + edgeMargin,
        viewport.width - safeInsets.right - edgeMargin - capsuleSize.width
    )
    val minY = safeInsets.top + edgeMargin
    val maxY = (viewport.height - safeInsets.bottom - edgeMargin - capsuleSize.height).coerceAtLeast(minY)
    val targetY = nearestSafeY(
        desiredY = desiredPoint.y.coerceIn(minY, maxY),
        x = targetX,
        minY = minY,
        maxY = maxY,
        capsuleSize = capsuleSize,
        exclusionZones = exclusionZones,
        margin = edgeMargin
    )
    return HeartRateCapsulePlacement(
        point = HeartRateCapsulePoint(targetX, targetY),
        edge = edge
    )
}

internal fun hasMovedBeyondHeartRateCapsuleDragThreshold(
    totalDx: Float,
    totalDy: Float,
    thresholdPx: Float
): Boolean {
    return totalDx * totalDx + totalDy * totalDy > thresholdPx * thresholdPx
}

private fun nearestSafeY(
    desiredY: Float,
    x: Float,
    minY: Float,
    maxY: Float,
    capsuleSize: HeartRateCapsuleSize,
    exclusionZones: List<HeartRateCapsuleExclusionZone>,
    margin: Float
): Float {
    val candidates = buildList {
        add(desiredY)
        add(minY)
        add(maxY)
        exclusionZones.forEach { zone ->
            add(zone.top - capsuleSize.height - margin)
            add(zone.bottom + margin)
        }
    }
        .map { candidate -> candidate.coerceIn(minY, maxY) }
        .distinct()
        .sortedBy { candidate -> abs(candidate - desiredY) }

    return candidates.firstOrNull { candidate ->
        !intersectsAnyExclusion(
            x = x,
            y = candidate,
            capsuleSize = capsuleSize,
            exclusionZones = exclusionZones
        )
    } ?: desiredY.coerceIn(minY, maxY)
}

private fun intersectsAnyExclusion(
    x: Float,
    y: Float,
    capsuleSize: HeartRateCapsuleSize,
    exclusionZones: List<HeartRateCapsuleExclusionZone>
): Boolean {
    val right = x + capsuleSize.width
    val bottom = y + capsuleSize.height
    return exclusionZones.any { zone ->
        x < zone.right && right > zone.left && y < zone.bottom && bottom > zone.top
    }
}
