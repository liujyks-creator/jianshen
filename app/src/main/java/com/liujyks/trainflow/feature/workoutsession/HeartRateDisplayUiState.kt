package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.PermissionPrivacyCopy

internal data class HeartRateDisplayUiState(
    val valueText: String,
    val statusText: String,
    val auxiliaryText: String,
    val boundaryText: String = PermissionPrivacyCopy.HEART_RATE,
    val isAvailable: Boolean
)

internal fun HeartRateState.toHeartRateDisplayUiState(): HeartRateDisplayUiState {
    return HeartRateDisplayUiState(
        valueText = valueText(),
        statusText = statusText(),
        auxiliaryText = auxiliaryText(),
        isAvailable = kind in currentReadingKinds && bpm != null
    )
}

private fun HeartRateState.valueText(): String {
    return when (kind) {
        HeartRateStateKind.DEVICE_READING,
        HeartRateStateKind.MANUAL_READING,
        HeartRateStateKind.STALE_READING -> "${bpm ?: "--"} bpm"
        HeartRateStateKind.UNAVAILABLE,
        HeartRateStateKind.DEVICE_CONNECTED_NO_READING,
        HeartRateStateKind.PERMISSION_UNAVAILABLE,
        HeartRateStateKind.PROVIDER_UNAVAILABLE -> "-- bpm"
    }
}

private fun HeartRateState.statusText(): String {
    return when (kind) {
        HeartRateStateKind.UNAVAILABLE -> "未获取心率"
        HeartRateStateKind.DEVICE_CONNECTED_NO_READING -> "设备已连接，等待读数"
        HeartRateStateKind.DEVICE_READING -> sourceText()
        HeartRateStateKind.MANUAL_READING -> "手动录入"
        HeartRateStateKind.STALE_READING -> staleStatusText()
        HeartRateStateKind.PERMISSION_UNAVAILABLE -> "权限不可用"
        HeartRateStateKind.PROVIDER_UNAVAILABLE -> "来源不可用"
    }
}

private fun HeartRateState.auxiliaryText(): String {
    val details = buildList {
        measuredAt?.takeIf { it.isNotBlank() }?.let { measuredAt ->
            add("时间 $measuredAt")
        }
        recordedAt?.takeIf { it.isNotBlank() }?.let { recordedAt ->
            add("记录 $recordedAt")
        }
        sourceId?.takeIf { it.isNotBlank() }?.let { sourceId ->
            add("来源 $sourceId")
        }
        sourceLabel
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { it != sourceText() }
            ?.let { sourceLabel -> add(sourceLabel) }
        message
            ?.takeIf { it != statusText() }
            ?.takeIf { it.isNotBlank() }
            ?.let { message -> add(message) }
    }
    return details.joinToString(" · ")
}

private val currentReadingKinds = setOf(
    HeartRateStateKind.DEVICE_READING,
    HeartRateStateKind.MANUAL_READING
)

private fun HeartRateState.sourceText(): String {
    return when (sourceKind) {
        HeartRateSourceKind.DEVICE -> sourceLabel?.takeIf { it.isNotBlank() } ?: "设备数据"
        HeartRateSourceKind.MANUAL -> "手动录入"
        HeartRateSourceKind.NONE -> ""
    }
}

private fun HeartRateState.staleStatusText(): String {
    val source = sourceText().takeIf { it.isNotBlank() }
    return if (source == null) {
        "数据已过期"
    } else {
        "数据已过期 · $source"
    }
}
