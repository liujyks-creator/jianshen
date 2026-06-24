package com.liujyks.trainflow.feature.plans

internal fun draggedItemTargetIndex(
    fromIndex: Int,
    dragOffsetPx: Float,
    itemHeightPx: Int,
    lastIndex: Int
): Int {
    if (fromIndex !in 0..lastIndex) return fromIndex
    val itemHeight = itemHeightPx.coerceAtLeast(1).toFloat()
    val halfItem = itemHeight / 2f
    val rowOffset = when {
        dragOffsetPx >= halfItem -> ((dragOffsetPx + halfItem) / itemHeight).toInt()
        dragOffsetPx <= -halfItem -> -(((-dragOffsetPx + halfItem) / itemHeight).toInt())
        else -> 0
    }
    return (fromIndex + rowOffset).coerceIn(0, lastIndex)
}

internal fun <T> List<T>.withItemMoved(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
    return toMutableList().also { list ->
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
    }
}

internal fun itemLayoutDisplacementPx(
    fromIndex: Int,
    toIndex: Int,
    itemKeys: List<String>,
    itemHeightsPx: Map<String, Int>,
    fallbackItemHeightPx: Int,
    itemGapPx: Int
): Int {
    if (fromIndex !in itemKeys.indices || toIndex !in itemKeys.indices || fromIndex == toIndex) {
        return 0
    }

    fun heightAt(index: Int): Int {
        return itemHeightsPx[itemKeys[index]] ?: fallbackItemHeightPx.coerceAtLeast(1)
    }

    val gap = itemGapPx.coerceAtLeast(0)
    return if (toIndex > fromIndex) {
        (fromIndex + 1..toIndex).sumOf { index -> heightAt(index) + gap }
    } else {
        -(toIndex until fromIndex).sumOf { index -> heightAt(index) + gap }
    }
}

internal fun placeholderShiftForIndexPx(
    index: Int,
    draggedIndex: Int,
    targetIndex: Int,
    draggedItemHeightPx: Int,
    itemGapPx: Int
): Int {
    if (draggedIndex == targetIndex || draggedIndex < 0 || targetIndex < 0) return 0
    val shift = draggedItemHeightPx.coerceAtLeast(1) + itemGapPx.coerceAtLeast(0)
    return when {
        targetIndex > draggedIndex && index in (draggedIndex + 1)..targetIndex -> -shift
        targetIndex < draggedIndex && index in targetIndex until draggedIndex -> shift
        else -> 0
    }
}
