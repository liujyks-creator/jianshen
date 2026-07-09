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
}
