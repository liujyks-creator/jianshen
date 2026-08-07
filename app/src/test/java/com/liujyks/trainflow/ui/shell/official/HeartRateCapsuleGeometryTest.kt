package com.liujyks.trainflow.ui.shell.official

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateCapsuleGeometryTest {
    private val viewport = HeartRateCapsuleViewport(width = 360f, height = 640f)
    private val capsuleSize = HeartRateCapsuleSize(width = 142f, height = 44f)
    private val safeInsets = HeartRateCapsuleSafeInsets(top = 24f, bottom = 24f)

    @Test
    fun releaseNearLeftSnapsToLeftSafeEdge() {
        val placement = snapHeartRateCapsuleToSafeEdge(
            releasePoint = HeartRateCapsulePoint(x = 40f, y = 120f),
            capsuleSize = capsuleSize,
            viewport = viewport,
            safeInsets = safeInsets,
            exclusionZones = emptyList(),
            edgeMargin = 12f
        )

        assertEquals(HeartRateCapsuleSnapEdge.LEFT, placement.edge)
        assertEquals(12f, placement.point.x, 0.01f)
    }

    @Test
    fun releaseNearRightSnapsToRightSafeEdge() {
        val placement = snapHeartRateCapsuleToSafeEdge(
            releasePoint = HeartRateCapsulePoint(x = 260f, y = 120f),
            capsuleSize = capsuleSize,
            viewport = viewport,
            safeInsets = safeInsets,
            exclusionZones = emptyList(),
            edgeMargin = 12f
        )

        assertEquals(HeartRateCapsuleSnapEdge.RIGHT, placement.edge)
        assertEquals(206f, placement.point.x, 0.01f)
    }

    @Test
    fun rightEdgeSurvivesCollapsedAndExpandedResize() {
        val collapsed = placeAtStoredEdge(
            desiredPoint = HeartRateCapsulePoint(x = 70f, y = 120f),
            edge = HeartRateCapsuleSnapEdge.RIGHT,
            size = HeartRateCapsuleSize(width = 116f, height = 44f)
        )
        val expanded = placeAtStoredEdge(
            desiredPoint = HeartRateCapsulePoint(x = 230f, y = 120f),
            edge = HeartRateCapsuleSnapEdge.RIGHT,
            size = HeartRateCapsuleSize(width = 276f, height = 214f)
        )

        assertEquals(HeartRateCapsuleSnapEdge.RIGHT, collapsed.edge)
        assertEquals(230f, collapsed.point.x, 0.01f)
        assertEquals(HeartRateCapsuleSnapEdge.RIGHT, expanded.edge)
        assertEquals(70f, expanded.point.x, 0.01f)
    }

    @Test
    fun leftEdgeSurvivesCollapsedAndExpandedResize() {
        val collapsed = placeAtStoredEdge(
            desiredPoint = HeartRateCapsulePoint(x = 14f, y = 120f),
            edge = HeartRateCapsuleSnapEdge.LEFT,
            size = HeartRateCapsuleSize(width = 116f, height = 44f)
        )
        val expanded = placeAtStoredEdge(
            desiredPoint = HeartRateCapsulePoint(x = 14f, y = 120f),
            edge = HeartRateCapsuleSnapEdge.LEFT,
            size = HeartRateCapsuleSize(width = 276f, height = 214f)
        )

        assertEquals(HeartRateCapsuleSnapEdge.LEFT, collapsed.edge)
        assertEquals(14f, collapsed.point.x, 0.01f)
        assertEquals(HeartRateCapsuleSnapEdge.LEFT, expanded.edge)
        assertEquals(14f, expanded.point.x, 0.01f)
    }

    @Test
    fun releaseOverBottomButtonSnapsAwayFromBottomZone() {
        val bottomZone = HeartRateCapsuleExclusionZone(
            left = 0f,
            top = 548f,
            right = viewport.width,
            bottom = viewport.height
        )

        val placement = snapHeartRateCapsuleToSafeEdge(
            releasePoint = HeartRateCapsulePoint(x = 260f, y = 570f),
            capsuleSize = capsuleSize,
            viewport = viewport,
            safeInsets = safeInsets,
            exclusionZones = listOf(bottomZone),
            edgeMargin = 12f
        )

        assertTrue(placement.point.y + capsuleSize.height <= bottomZone.top)
    }

    @Test
    fun releaseOverConfirmRecordControlsSnapsAwayFromUnsafeControls() {
        val confirmRecordZone = HeartRateCapsuleExclusionZone(
            left = 0f,
            top = 330f,
            right = viewport.width,
            bottom = viewport.height
        )

        val placement = snapHeartRateCapsuleToSafeEdge(
            releasePoint = HeartRateCapsulePoint(x = 30f, y = 360f),
            capsuleSize = capsuleSize,
            viewport = viewport,
            safeInsets = safeInsets,
            exclusionZones = listOf(confirmRecordZone),
            edgeMargin = 12f
        )

        assertTrue(placement.point.y + capsuleSize.height <= confirmRecordZone.top)
    }

    @Test
    fun snapClampsAwayFromStatusAndGestureInsets() {
        val placement = snapHeartRateCapsuleToSafeEdge(
            releasePoint = HeartRateCapsulePoint(x = 30f, y = 0f),
            capsuleSize = capsuleSize,
            viewport = viewport,
            safeInsets = safeInsets,
            exclusionZones = emptyList(),
            edgeMargin = 12f
        )

        assertTrue(placement.point.y >= safeInsets.top + 12f)
    }

    @Test
    fun tapThresholdDoesNotBecomeDrag() {
        assertFalse(
            hasMovedBeyondHeartRateCapsuleDragThreshold(
                totalDx = 3f,
                totalDy = 4f,
                thresholdPx = 10f
            )
        )
        assertTrue(
            hasMovedBeyondHeartRateCapsuleDragThreshold(
                totalDx = 9f,
                totalDy = 7f,
                thresholdPx = 10f
            )
        )
    }

    @Test
    fun expandedPlacementCanFallBackToCompactSizeOnSmallViewport() {
        val unsafeBottomZone = HeartRateCapsuleExclusionZone(
            left = 0f,
            top = 300f,
            right = viewport.width,
            bottom = viewport.height
        )

        val regularExpandedFits = hasSafeHeartRateCapsulePlacement(
            capsuleSize = HeartRateCapsuleSize(width = 248f, height = 270f),
            viewport = viewport,
            safeInsets = safeInsets,
            exclusionZones = listOf(unsafeBottomZone),
            edgeMargin = 12f
        )
        val compactExpandedFits = hasSafeHeartRateCapsulePlacement(
            capsuleSize = HeartRateCapsuleSize(width = 236f, height = 150f),
            viewport = viewport,
            safeInsets = safeInsets,
            exclusionZones = listOf(unsafeBottomZone),
            edgeMargin = 12f
        )

        assertFalse(regularExpandedFits)
        assertTrue(compactExpandedFits)
    }

    @Test
    fun expandedPlacementCanReportNoSafeSpace() {
        val almostFullScreenUnsafeZone = HeartRateCapsuleExclusionZone(
            left = 0f,
            top = 120f,
            right = viewport.width,
            bottom = viewport.height
        )

        val canPlace = hasSafeHeartRateCapsulePlacement(
            capsuleSize = HeartRateCapsuleSize(width = 236f, height = 150f),
            viewport = viewport,
            safeInsets = safeInsets,
            exclusionZones = listOf(almostFullScreenUnsafeZone),
            edgeMargin = 12f
        )

        assertFalse(canPlace)
    }

    private fun placeAtStoredEdge(
        desiredPoint: HeartRateCapsulePoint,
        edge: HeartRateCapsuleSnapEdge,
        size: HeartRateCapsuleSize
    ) = placeHeartRateCapsuleAtSafeEdge(
        desiredPoint = desiredPoint,
        edge = edge,
        capsuleSize = size,
        viewport = viewport,
        safeInsets = safeInsets,
        exclusionZones = emptyList(),
        edgeMargin = 14f
    )
}
