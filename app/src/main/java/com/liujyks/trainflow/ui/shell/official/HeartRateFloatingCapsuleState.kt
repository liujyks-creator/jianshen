package com.liujyks.trainflow.ui.shell.official

import com.liujyks.trainflow.core.model.HeartRateFact
import com.liujyks.trainflow.core.model.HeartRateState
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

    val state = liveState
    if (state?.fact == HeartRateFact.DISABLED) {
        return HeartRateFloatingCapsuleUiState.Hidden
    }
    if (state?.fact != null && !state.isValidE17State()) {
        return technicalFailureCapsule(
            deviceHint = state.sourceLabel ?: settings.savedDeviceDisplayName,
            forceCollapsed = forceCollapsed
        )
    }

    val deviceHint = state?.sourceLabel ?: settings.savedDeviceDisplayName
    when (state?.fact) {
        HeartRateFact.PERMISSION_REQUIRED -> return stateCapsule(
            status = HeartRateFloatingCapsuleStatus.PERMISSION_DENIED,
            label = "权限未赋予",
            title = "需要蓝牙权限",
            body = "授权后才能扫描或连接你主动选择的心率设备。",
            deviceHint = deviceHint,
            forceCollapsed = forceCollapsed
        )
        HeartRateFact.BLUETOOTH_OFF -> return bluetoothDisabledCapsule(settings, forceCollapsed)
        HeartRateFact.NOT_CONNECTED -> return notConnectedCapsule(
            deviceHint = deviceHint,
            hasSavedHint = settings.savedDeviceDisplayName != null ||
                settings.savedDeviceIdentifier != null,
            forceCollapsed = forceCollapsed
        )
        HeartRateFact.SCANNING -> return stateCapsule(
            status = HeartRateFloatingCapsuleStatus.CONNECTING,
            label = "正在扫描",
            title = "正在查找心率设备",
            body = "正在查找标准心率广播；这不代表设备已经连接。",
            deviceHint = deviceHint,
            forceCollapsed = forceCollapsed
        )
        HeartRateFact.CONNECTING -> return stateCapsule(
            status = HeartRateFloatingCapsuleStatus.CONNECTING,
            label = "正在连接",
            title = "正在连接设备",
            body = "正在建立前台连接；当前还没有实时心率数据。",
            deviceHint = deviceHint,
            forceCollapsed = forceCollapsed
        )
        HeartRateFact.WAITING_FIRST_DATA -> return stateCapsule(
            status = HeartRateFloatingCapsuleStatus.WAITING_DATA,
            label = "等待数据",
            title = "等待心率数据",
            body = "连接已建立，正在等待第一条有效心率数据。",
            deviceHint = deviceHint,
            forceCollapsed = forceCollapsed
        )
        HeartRateFact.LIVE -> return heartRateReadingCapsuleUiState(
            bpm = requireNotNull(state.bpm),
            sourceLabel = deviceHint,
            userAgeYears = userAgeYears,
            overLimitThresholdBpm = overLimitThresholdBpm,
            forceCollapsed = forceCollapsed
        )
        HeartRateFact.DATA_INTERRUPTED -> return stateCapsule(
            status = HeartRateFloatingCapsuleStatus.STALE,
            label = "数据中断",
            title = "心率数据已中断",
            body = "最近没有收到新的有效数据；旧 bpm 不会作为当前值显示。",
            deviceHint = deviceHint,
            forceCollapsed = forceCollapsed
        )
        HeartRateFact.LINK_DISCONNECTED -> return stateCapsule(
            status = HeartRateFloatingCapsuleStatus.OFFLINE,
            label = "连接已断开",
            title = "心率设备已断开",
            body = "连接已经明确断开；不会把旧数据当作实时心率。",
            deviceHint = deviceHint,
            updateLabel = "已断开",
            forceCollapsed = forceCollapsed
        )
        HeartRateFact.TECHNICAL_FAILURE -> return technicalFailureCapsule(
            deviceHint = deviceHint,
            forceCollapsed = forceCollapsed
        )
        HeartRateFact.INTENTIONAL_STOP -> return stateCapsule(
            status = HeartRateFloatingCapsuleStatus.SAVED_DEVICE,
            label = "已停止",
            title = "心率连接已停止",
            body = "本次前台心率连接已由用户停止。",
            deviceHint = deviceHint,
            updateLabel = "已停止",
            forceCollapsed = forceCollapsed
        )
        HeartRateFact.DISABLED -> return HeartRateFloatingCapsuleUiState.Hidden
        null -> Unit // Pre-E17 compatibility fields are intentionally not presentation inputs.
    }

    return notConnectedCapsule(
        deviceHint = settings.savedDeviceDisplayName,
        hasSavedHint = settings.savedDeviceDisplayName != null ||
            settings.savedDeviceIdentifier != null,
        forceCollapsed = forceCollapsed
    )
}

private fun notConnectedCapsule(
    deviceHint: String?,
    hasSavedHint: Boolean,
    forceCollapsed: Boolean
): HeartRateFloatingCapsuleUiState = if (hasSavedHint || deviceHint != null) {
    stateCapsule(
        status = HeartRateFloatingCapsuleStatus.SAVED_DEVICE,
        label = "未连接",
        title = "尚未连接心率设备",
        body = "已保存设备仅供主动连接，不代表设备在附近、正在连接或已经连接。",
        deviceHint = deviceHint ?: "已保存设备",
        updateLabel = "未连接",
        forceCollapsed = forceCollapsed
    )
} else {
    stateCapsule(
        status = HeartRateFloatingCapsuleStatus.NO_SOURCE,
        label = "未连接源",
        title = "未连接心率来源",
        body = "可在心率与设备中选择设备。关闭心率显示后胶囊会消失。",
        forceCollapsed = forceCollapsed
    )
}

private fun technicalFailureCapsule(
    deviceHint: String?,
    forceCollapsed: Boolean
) = stateCapsule(
    status = HeartRateFloatingCapsuleStatus.ERROR,
    label = "连接异常",
    title = "心率连接异常",
    body = "心率连接发生技术异常；可到心率与设备中重新处理。",
    deviceHint = deviceHint,
    updateLabel = "异常",
    forceCollapsed = forceCollapsed
)

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
        HeartRateFloatingCapsuleStatus.OVER_LIMIT -> "当前只显示"
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
