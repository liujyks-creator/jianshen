package com.liujyks.trainflow.feature.plans

import org.junit.Assert.assertEquals
import org.junit.Test

class PlanEditorDragTest {
    @Test
    fun draggedItemTargetIndexMovesAfterCrossingHalfItemHeight() {
        assertEquals(1, draggedItemTargetIndex(fromIndex = 1, dragOffsetPx = 49f, itemHeightPx = 100, lastIndex = 4))
        assertEquals(2, draggedItemTargetIndex(fromIndex = 1, dragOffsetPx = 50f, itemHeightPx = 100, lastIndex = 4))
        assertEquals(3, draggedItemTargetIndex(fromIndex = 1, dragOffsetPx = 150f, itemHeightPx = 100, lastIndex = 4))
        assertEquals(0, draggedItemTargetIndex(fromIndex = 1, dragOffsetPx = -50f, itemHeightPx = 100, lastIndex = 4))
    }

    @Test
    fun draggedItemTargetIndexSupportsMultiRowDragFromOriginalIndex() {
        assertEquals(0, draggedItemTargetIndex(fromIndex = 0, dragOffsetPx = 49f, itemHeightPx = 100, lastIndex = 4))
        assertEquals(1, draggedItemTargetIndex(fromIndex = 0, dragOffsetPx = 50f, itemHeightPx = 100, lastIndex = 4))
        assertEquals(2, draggedItemTargetIndex(fromIndex = 0, dragOffsetPx = 150f, itemHeightPx = 100, lastIndex = 4))
        assertEquals(3, draggedItemTargetIndex(fromIndex = 0, dragOffsetPx = 250f, itemHeightPx = 100, lastIndex = 4))
        assertEquals(4, draggedItemTargetIndex(fromIndex = 0, dragOffsetPx = 350f, itemHeightPx = 100, lastIndex = 4))
    }

    @Test
    fun draggedItemTargetIndexCoercesToListBounds() {
        assertEquals(0, draggedItemTargetIndex(fromIndex = 1, dragOffsetPx = -500f, itemHeightPx = 100, lastIndex = 4))
        assertEquals(4, draggedItemTargetIndex(fromIndex = 3, dragOffsetPx = 500f, itemHeightPx = 100, lastIndex = 4))
    }

    @Test
    fun withItemMovedInsertsItemAtTargetIndexWithoutMutatingSourceList() {
        val original = listOf("1", "2", "3", "4")
        val movedDown = original.withItemMoved(fromIndex = 0, toIndex = 2)
        val movedUp = original.withItemMoved(fromIndex = 3, toIndex = 1)

        assertEquals(listOf("2", "3", "1", "4"), movedDown)
        assertEquals(listOf("1", "4", "2", "3"), movedUp)
        assertEquals(listOf("1", "2", "3", "4"), original)
        assertEquals(original, original.withItemMoved(fromIndex = -1, toIndex = 2))
        assertEquals(original, original.withItemMoved(fromIndex = 1, toIndex = 4))
    }

    @Test
    fun itemLayoutDisplacementUsesCrossedItemHeightsForStableVisualDrag() {
        val itemKeys = listOf("dragged", "short", "tall", "last")
        val heights = mapOf(
            "dragged" to 240,
            "short" to 180,
            "tall" to 320,
            "last" to 220
        )

        assertEquals(
            196,
            itemLayoutDisplacementPx(
                fromIndex = 0,
                toIndex = 1,
                itemKeys = itemKeys,
                itemHeightsPx = heights,
                fallbackItemHeightPx = 240,
                itemGapPx = 16
            )
        )
        assertEquals(
            532,
            itemLayoutDisplacementPx(
                fromIndex = 0,
                toIndex = 2,
                itemKeys = itemKeys,
                itemHeightsPx = heights,
                fallbackItemHeightPx = 240,
                itemGapPx = 16
            )
        )
        assertEquals(
            -532,
            itemLayoutDisplacementPx(
                fromIndex = 3,
                toIndex = 1,
                itemKeys = itemKeys,
                itemHeightsPx = heights,
                fallbackItemHeightPx = 240,
                itemGapPx = 16
            )
        )
    }

    @Test
    fun placeholderShiftKeepsDraggedItemFingerAnchoredWhileOtherItemsGiveWay() {
        assertEquals(0, placeholderShiftForIndexPx(index = 0, draggedIndex = 0, targetIndex = 2, draggedItemHeightPx = 240, itemGapPx = 16))
        assertEquals(-256, placeholderShiftForIndexPx(index = 1, draggedIndex = 0, targetIndex = 2, draggedItemHeightPx = 240, itemGapPx = 16))
        assertEquals(-256, placeholderShiftForIndexPx(index = 2, draggedIndex = 0, targetIndex = 2, draggedItemHeightPx = 240, itemGapPx = 16))
        assertEquals(0, placeholderShiftForIndexPx(index = 3, draggedIndex = 0, targetIndex = 2, draggedItemHeightPx = 240, itemGapPx = 16))

        assertEquals(256, placeholderShiftForIndexPx(index = 1, draggedIndex = 3, targetIndex = 1, draggedItemHeightPx = 240, itemGapPx = 16))
        assertEquals(256, placeholderShiftForIndexPx(index = 2, draggedIndex = 3, targetIndex = 1, draggedItemHeightPx = 240, itemGapPx = 16))
        assertEquals(0, placeholderShiftForIndexPx(index = 3, draggedIndex = 3, targetIndex = 1, draggedItemHeightPx = 240, itemGapPx = 16))
    }
}
