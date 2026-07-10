package com.liujyks.trainflow.ui.shell.official

import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateUnavailableReason
import com.liujyks.trainflow.feature.settings.HeartRateBlePermissionStatus
import com.liujyks.trainflow.feature.settings.HeartRateSettingsUiState

internal data class HeartRateFloatingCapsuleUiState(
    val visible: Boolean,
    val status: HeartRateFloatingCapsuleStatus,
    val collapsedLabel: String,
    val detailTitle: String,
    val detailBody: String,
    val deviceHint: String? = null,
    val infoTiles: List<HeartRateFloatingCapsuleInfoTile> = emptyList(),
    val forceCollapsed: Boolean = false
) {
    companion object {
        val Hidden = HeartRateFloatingCapsuleUiState(
            visible = false,
            status = HeartRateFloatingCapsuleStatus.HIDDEN,
            collapsedLabel = "",
            detailTitle = "",
            detailBody = ""
        )
    }
}

internal data class HeartRateFloatingCapsuleInfoTile(
    val label: String,
    val value: String
)

internal enum class HeartRateFloatingCapsuleStatus {
    HIDDEN,
    NO_SOURCE,
    PERMISSION_DENIED,
    BLUETOOTH_DISABLED,
    CONNECTING,
    WAITING_DATA,
    STALE,
    OFFLINE,
    SAVED_DEVICE,
    BPM_ONLY,
    ZONE_LOW,
    ZONE_WARMUP,
    ZONE_FAT_BURN,
    ZONE_AEROBIC,
    ZONE_ANAEROBIC,
    ZONE_LIMIT,
    OVER_LIMIT,
    ERROR
}

internal fun heartRateFloatingCapsuleUiState(
    settings: HeartRateSettingsUiState,
    liveState: HeartRateState? = null,
    userAgeYears: Int? = null,
    overLimitThresholdBpm: Int? = null,
    forceCollapsed: Boolean = false
): HeartRateFloatingCapsuleUiState {
    if (!settings.enabled) {
        return HeartRateFloatingCapsuleUiState.Hidden
    }

    if (settings.blePermissionStatus != HeartRateBlePermissionStatus.GRANTED) {
        return stateCapsule(
            status = HeartRateFloatingCapsuleStatus.PERMISSION_DENIED,
            label = "权限未赋予",
            title = "需要蓝牙权限",
            body = "进入心率与设备后，可按说明授权蓝牙权限。TrainFlow 不会在启动或训练开始时自动请求。",
            deviceHint = settings.savedDeviceDisplayName,
            forceCollapsed = forceCollapsed
        )
    }

    val liveReading = liveState?.bpm?.takeIf { bpm -> bpm > 0 }?.let { bpm ->
        when (liveState.kind) {
            HeartRateStateKind.DEVICE_READING,
            HeartRateStateKind.MANUAL_READING -> bpm
            else -> null
        }
    }
    if (liveReading != null) {
        return heartRateReadingCapsuleUiState(
            bpm = liveReading,
            sourceLabel = liveState.sourceLabel ?: settings.savedDeviceDisplayName,
            userAgeYears = userAgeYears,
            overLimitThresholdBpm = overLimitThresholdBpm,
            forceCollapsed = forceCollapsed
        )
    }

    liveState?.let { state ->
        when (state.kind) {
            HeartRateStateKind.DEVICE_CONNECTED_NO_READING -> {
                return stateCapsule(
                    status = HeartRateFloatingCapsuleStatus.WAITING_DATA,
                    label = "等待数据",
                    title = "等待心率数据",
                    body = "设备已连接，但当前还没有可展示的实时心率。",
                    deviceHint = state.sourceLabel ?: settings.savedDeviceDisplayName,
                    forceCollapsed = forceCollapsed
                )
            }

            HeartRateStateKind.STALE_READING -> {
                val offline = state.unavailableReason == HeartRateUnavailableReason.DEVICE_DISCONNECTED
                return stateCapsule(
                    status = if (offline) {
                        HeartRateFloatingCapsuleStatus.OFFLINE
                    } else {
                        HeartRateFloatingCapsuleStatus.STALE
                    },
                    label = if (offline) "离线" else "数据过期",
                    title = if (offline) "设备离线" else "心率数据过期",
                    body = if (offline) {
                        "最近没有找到已选择的设备；不会把旧数据当作实时心率。"
                    } else {
                        "最近没有收到新的心率数据；当前不会显示旧 bpm。"
                    },
                    deviceHint = state.sourceLabel ?: settings.savedDeviceDisplayName,
                    forceCollapsed = forceCollapsed
                )
            }

            HeartRateStateKind.PERMISSION_UNAVAILABLE -> {
                return stateCapsule(
                    status = HeartRateFloatingCapsuleStatus.PERMISSION_DENIED,
                    label = "权限未赋予",
                    title = "需要蓝牙权限",
                    body = "授权后才能扫描或连接你主动选择的心率设备。",
                    deviceHint = settings.savedDeviceDisplayName,
                    forceCollapsed = forceCollapsed
                )
            }

            HeartRateStateKind.PROVIDER_UNAVAILABLE -> {
                if (state.unavailableReason == HeartRateUnavailableReason.BLUETOOTH_DISABLED) {
                    return bluetoothDisabledCapsule(settings, forceCollapsed)
                }
                if (
                    state.unavailableReason == HeartRateUnavailableReason.CONNECTION_FAILED ||
                    state.unavailableReason == HeartRateUnavailableReason.READ_ERROR
                ) {
                    return stateCapsule(
                        status = HeartRateFloatingCapsuleStatus.ERROR,
                        label = "连接异常",
                        title = "心率连接异常",
                        body = "当前只显示错误状态；可到心率与设备中重新处理。",
                        deviceHint = state.sourceLabel ?: settings.savedDeviceDisplayName,
                        updateLabel = "异常",
                        forceCollapsed = forceCollapsed
                    )
                }
                if (state.sourceLabel != null || settings.savedDeviceDisplayName != null) {
                    return stateCapsule(
                        status = HeartRateFloatingCapsuleStatus.CONNECTING,
                        label = "正在连接",
                        title = "正在连接设备",
                        body = "TrainFlow 只显示连接状态，不会自动写入训练记录。",
                        deviceHint = state.sourceLabel ?: settings.savedDeviceDisplayName,
                        forceCollapsed = forceCollapsed
                    )
                }
            }

            else -> Unit
        }
    }

    return when {
        settings.savedDeviceDisplayName != null || settings.savedDeviceIdentifier != null ->
            stateCapsule(
                status = HeartRateFloatingCapsuleStatus.SAVED_DEVICE,
                label = "未连接",
                title = "尚未连接心率设备",
                body = "已保存设备仅供你主动连接，不代表设备在附近、已开启广播、正在连接或已经连接。",
                deviceHint = settings.savedDeviceDisplayName ?: settings.savedDeviceIdentifier,
                updateLabel = "未连接",
                forceCollapsed = forceCollapsed
            )

        else ->
            stateCapsule(
                status = HeartRateFloatingCapsuleStatus.NO_SOURCE,
                label = "未连接源",
                title = "未连接心率来源",
                body = "可在心率与设备中选择设备。关闭心率显示后胶囊会消失。",
                forceCollapsed = forceCollapsed
            )
    }
}

private fun bluetoothDisabledCapsule(
    settings: HeartRateSettingsUiState,
    forceCollapsed: Boolean
): HeartRateFloatingCapsuleUiState {
    return stateCapsule(
        status = HeartRateFloatingCapsuleStatus.BLUETOOTH_DISABLED,
        label = "蓝牙关闭",
        title = "蓝牙已关闭",
        body = "开启蓝牙后，TrainFlow 才能连接已选择的心率设备。",
        deviceHint = settings.savedDeviceDisplayName,
        forceCollapsed = forceCollapsed
    )
}

private fun heartRateReadingCapsuleUiState(
    bpm: Int,
    sourceLabel: String?,
    userAgeYears: Int?,
    overLimitThresholdBpm: Int?,
    forceCollapsed: Boolean
): HeartRateFloatingCapsuleUiState {
    if (overLimitThresholdBpm != null && bpm > overLimitThresholdBpm) {
        return stateCapsule(
            status = HeartRateFloatingCapsuleStatus.OVER_LIMIT,
            label = "超过上限 $bpm bpm",
            title = "超过上限",
            body = "超过上限只是深红视觉提示，不触发声音、震动、强制暂停或医疗告警。",
            deviceHint = sourceLabel,
            zoneLabel = "超过上限",
            updateLabel = "实时",
            forceCollapsed = forceCollapsed
        )
    }

    val zone = userAgeYears?.takeIf { age -> age in 10..100 }?.let { age ->
        heartRateZoneForBpm(bpm = bpm, maxHeartRate = 220 - age)
    }
    if (zone == null) {
        return stateCapsule(
            status = HeartRateFloatingCapsuleStatus.BPM_ONLY,
            label = "心率 $bpm bpm",
            title = "实时心率",
            body = "未设置年龄时只显示 bpm，不计算心率区间。",
            deviceHint = sourceLabel,
            zoneLabel = "无",
            updateLabel = "实时",
            forceCollapsed = forceCollapsed
        )
    }

    return stateCapsule(
        status = zone.status,
        label = "${zone.label} $bpm bpm",
        title = zone.label,
        body = "区间基于用户年龄估算最大心率，仅作训练参考。",
        deviceHint = sourceLabel,
        zoneLabel = zone.label,
        updateLabel = "实时",
        forceCollapsed = forceCollapsed
    )
}

private data class HeartRateZoneResult(
    val status: HeartRateFloatingCapsuleStatus,
    val label: String
)

private fun heartRateZoneForBpm(
    bpm: Int,
    maxHeartRate: Int
): HeartRateZoneResult {
    val percent = bpm.toFloat() / maxHeartRate.coerceAtLeast(1).toFloat()
    return when {
        percent < 0.50f -> HeartRateZoneResult(HeartRateFloatingCapsuleStatus.ZONE_LOW, "低强度")
        percent < 0.60f -> HeartRateZoneResult(HeartRateFloatingCapsuleStatus.ZONE_WARMUP, "热身")
        percent < 0.70f -> HeartRateZoneResult(HeartRateFloatingCapsuleStatus.ZONE_FAT_BURN, "燃脂")
        percent < 0.80f -> HeartRateZoneResult(HeartRateFloatingCapsuleStatus.ZONE_AEROBIC, "有氧")
        percent < 0.90f -> HeartRateZoneResult(HeartRateFloatingCapsuleStatus.ZONE_ANAEROBIC, "无氧")
        else -> HeartRateZoneResult(HeartRateFloatingCapsuleStatus.ZONE_LIMIT, "极限")
    }
}

private fun stateCapsule(
    status: HeartRateFloatingCapsuleStatus,
    label: String,
    title: String,
    body: String,
    deviceHint: String? = null,
    zoneLabel: String = "无",
    updateLabel: String = "无数据",
    forceCollapsed: Boolean
): HeartRateFloatingCapsuleUiState {
    val sourceLabel = deviceHint
        ?.takeIf { it.isNotBlank() }
        ?: when (status) {
            HeartRateFloatingCapsuleStatus.PERMISSION_DENIED -> "未授权"
            HeartRateFloatingCapsuleStatus.BLUETOOTH_DISABLED -> "蓝牙关闭"
            HeartRateFloatingCapsuleStatus.CONNECTING -> "查找中"
            HeartRateFloatingCapsuleStatus.NO_SOURCE -> "未连接"
            else -> label
        }
    val recordingLabel = when (status) {
        HeartRateFloatingCapsuleStatus.HIDDEN,
        HeartRateFloatingCapsuleStatus.NO_SOURCE,
        HeartRateFloatingCapsuleStatus.PERMISSION_DENIED,
        HeartRateFloatingCapsuleStatus.BLUETOOTH_DISABLED -> "未记录"
        HeartRateFloatingCapsuleStatus.CONNECTING,
        HeartRateFloatingCapsuleStatus.WAITING_DATA,
        HeartRateFloatingCapsuleStatus.STALE,
        HeartRateFloatingCapsuleStatus.OFFLINE,
        HeartRateFloatingCapsuleStatus.ERROR,
        HeartRateFloatingCapsuleStatus.SAVED_DEVICE -> "当前只显示状态"
        HeartRateFloatingCapsuleStatus.BPM_ONLY,
        HeartRateFloatingCapsuleStatus.ZONE_LOW,
        HeartRateFloatingCapsuleStatus.ZONE_WARMUP,
        HeartRateFloatingCapsuleStatus.ZONE_FAT_BURN,
        HeartRateFloatingCapsuleStatus.ZONE_AEROBIC,
        HeartRateFloatingCapsuleStatus.ZONE_ANAEROBIC,
        HeartRateFloatingCapsuleStatus.ZONE_LIMIT,
        HeartRateFloatingCapsuleStatus.OVER_LIMIT -> "训练记录：后续开启"
    }
    return HeartRateFloatingCapsuleUiState(
        visible = true,
        status = status,
        collapsedLabel = label,
        detailTitle = title,
        detailBody = body,
        deviceHint = deviceHint?.takeIf { it.isNotBlank() },
        infoTiles = listOf(
            HeartRateFloatingCapsuleInfoTile(
                label = "来源",
                value = if (status == HeartRateFloatingCapsuleStatus.SAVED_DEVICE) {
                    "已保存：$sourceLabel"
                } else {
                    sourceLabel
                }
            ),
            HeartRateFloatingCapsuleInfoTile(label = "记录", value = recordingLabel),
            HeartRateFloatingCapsuleInfoTile(label = "区间", value = zoneLabel),
            HeartRateFloatingCapsuleInfoTile(label = "更新", value = updateLabel)
        ),
        forceCollapsed = forceCollapsed
    )
}
