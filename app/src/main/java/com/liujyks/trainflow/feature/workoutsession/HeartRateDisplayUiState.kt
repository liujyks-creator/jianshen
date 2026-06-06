package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.model.HeartRateAvailability
import com.liujyks.trainflow.core.model.HeartRateState
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
        isAvailable = availability == HeartRateAvailability.AVAILABLE && bpm != null
    )
}

private fun HeartRateState.valueText(): String {
    return when (availability) {
        HeartRateAvailability.AVAILABLE,
        HeartRateAvailability.STALE -> "${bpm ?: "--"} bpm"
        HeartRateAvailability.DISABLED,
        HeartRateAvailability.NOT_CONNECTED,
        HeartRateAvailability.CONNECTING,
        HeartRateAvailability.ERROR -> "-- bpm"
    }
}

private fun HeartRateState.statusText(): String {
    return when (availability) {
        HeartRateAvailability.DISABLED -> "心率显示已关闭"
        HeartRateAvailability.NOT_CONNECTED -> "未连接设备"
        HeartRateAvailability.CONNECTING -> "等待数据"
        HeartRateAvailability.AVAILABLE -> message ?: "演示心率状态"
        HeartRateAvailability.STALE -> message ?: "数据暂时中断"
        HeartRateAvailability.ERROR -> message ?: "心率状态暂不可用"
    }
}

private fun HeartRateState.auxiliaryText(): String {
    val details = buildList {
        measuredAt?.takeIf { it.isNotBlank() }?.let { measuredAt ->
            add("时间 $measuredAt")
        }
        sourceId?.takeIf { it.isNotBlank() }?.let { sourceId ->
            add("来源 $sourceId")
        }
        message
            ?.takeIf { availability != HeartRateAvailability.AVAILABLE }
            ?.takeIf { it.isNotBlank() }
            ?.let { message -> add(message) }
    }
    return details.joinToString(" · ")
}
