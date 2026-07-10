package com.liujyks.trainflow.feature.settings

import com.liujyks.trainflow.core.health.BleHeartRateDeviceCandidate
import com.liujyks.trainflow.core.health.BleHeartRateProviderState
import com.liujyks.trainflow.core.health.BleHeartRateProviderStateKind
import com.liujyks.trainflow.core.health.BleHeartRateScanState
import com.liujyks.trainflow.core.health.BleHeartRateScanStateKind
import com.liujyks.trainflow.core.model.PermissionPrivacyCopy
import com.liujyks.trainflow.core.model.PermissionPrivacySection
import com.liujyks.trainflow.ui.theme.SkinRegistry
import com.liujyks.trainflow.ui.theme.TrainFlowSkin

internal data class TrainingPreferencesScreenState(
    val defaultCountdownThresholdSec: Int = 5,
    val actionCueEnabled: Boolean = true,
    val restCueEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val emphasisAnimationEnabled: Boolean = true,
    val strengthSetTimerMode: StrengthSetTimerModePreference = StrengthSetTimerModePreference.MANUAL_START,
    val heartRateSettings: HeartRateSettingsUiState = HeartRateSettingsUiState(),
    val selectedUiSkinId: String = SkinRegistry.defaultSkin.id,
    val uiSkinOptions: List<UiSkinPreferenceOption> = uiSkinPreferenceOptionsFromRegistry(selectedUiSkinId),
    val permissionPrivacySections: List<PermissionPrivacySection> = PermissionPrivacyCopy.sections
) {
    val countdownSummary: String
        get() = "默认最后 ${defaultCountdownThresholdSec} 秒提醒"

    val feedbackSummary: String
        get() = listOfNotNull(
            "动作".takeIf { actionCueEnabled },
            "休息".takeIf { restCueEnabled },
            "声音".takeIf { soundEnabled },
            "震动".takeIf { vibrationEnabled },
            "强化动画".takeIf { emphasisAnimationEnabled }
        ).ifEmpty {
            listOf("仅保留训练流程")
        }.joinToString(" / ")

    val selectedSkinSummary: String
        get() = uiSkinOptions.firstOrNull { option -> option.selected }?.displayName
            ?: SkinRegistry.defaultSkin.displayName
}

internal data class HeartRateSettingsUiState(
    val enabled: Boolean = false,
    val savedDeviceIdentifier: String? = null,
    val savedDeviceDisplayName: String? = null,
    val blePermissionStatus: HeartRateBlePermissionStatus = HeartRateBlePermissionStatus.NOT_REQUESTED,
    val devicePickerState: HeartRateDevicePickerUiState = heartRateDevicePickerUiState(
        displayEnabled = enabled,
        blePermissionStatus = blePermissionStatus,
        savedDeviceIdentifier = savedDeviceIdentifier,
        savedDeviceDisplayName = savedDeviceDisplayName
    )
) {
    val sectionTitle: String = "心率与设备"

    val statusLabel: String
        get() = if (enabled) "已开启" else "未开启"

    val statusSummary: String
        get() = if (enabled) {
            "已启用显示偏好；后续可选择设备。"
        } else {
            "默认关闭。TrainFlow 不显示心率胶囊。"
        }

    val sourceSummary: String
        get() = if (enabled) {
            savedDeviceDisplayName?.let { displayName ->
                "已保存设备：$displayName。保存设备不代表它在附近、正在广播、正在连接或已连接。"
            } ?: "未连接源 / 待选择设备。"
        } else {
            "关闭后不显示胶囊、不扫描、不连接、不记录。"
        }

    val purposeCopy: String =
        "用途：训练中显示 App 内实时心率胶囊，作为训练参考。"

    val recordingBoundaryCopy: String =
        "记录边界：无训练时只显示不记录；训练记录采样另拆后续任务。"

    val privacyCopy: String =
        "隐私：无训练时只在 App 内显示状态或实时心率，不写入训练记录。后续训练中记录会另行实现。"

    val nonMedicalCopy: String =
        "非医疗：心率区间仅作训练参考，不诊断疾病，不替代医生建议，不自动中断训练。"

    val permissionCopy: String =
        "权限：只有在你开启心率显示后，主动点击授权入口并看过说明，才会请求蓝牙权限。"

    val overlayCopy: String =
        "悬浮边界：不使用系统 overlay / 显示在其他应用上层权限，未来胶囊只显示在 TrainFlow App 内。"

    val enabledBoundaryCopy: String
        get() = if (enabled) {
            "开启后仍不会自动扫描或连接；只有你主动点击连接已保存设备或扫描心率设备才会查找附近设备。"
        } else {
            "关闭状态下不会显示心率胶囊，也不会扫描、连接或记录。"
        }

    val canClearSavedDevice: Boolean
        get() = savedDeviceIdentifier != null || savedDeviceDisplayName != null

    val canPrepareBlePermission: Boolean
        get() = enabled && blePermissionStatus.canPrepare

    val showBlePermissionRationale: Boolean
        get() = enabled && blePermissionStatus == HeartRateBlePermissionStatus.RATIONALE_VISIBLE

    val canRequestBlePermission: Boolean
        get() = showBlePermissionRationale

    val blePermissionActionLabel: String
        get() = when (blePermissionStatus) {
            HeartRateBlePermissionStatus.GRANTED -> "蓝牙权限已允许"
            HeartRateBlePermissionStatus.RATIONALE_VISIBLE -> "授权蓝牙权限"
            HeartRateBlePermissionStatus.DENIED -> "重新授权蓝牙权限"
            HeartRateBlePermissionStatus.PERMANENTLY_DENIED -> "去系统设置开启"
            HeartRateBlePermissionStatus.NOT_REQUESTED -> "准备连接设备"
        }

    val blePermissionStatusTitle: String
        get() = "蓝牙权限状态：${blePermissionStatus.label}"

    val blePermissionStatusCopy: String
        get() = if (!enabled) {
            "心率显示关闭时不会请求权限。开启后，你可以主动进入授权流程。"
        } else {
            when (blePermissionStatus) {
                HeartRateBlePermissionStatus.NOT_REQUESTED ->
                    "尚未请求蓝牙权限。点击准备连接设备后，TrainFlow 会先显示用途说明。"
                HeartRateBlePermissionStatus.RATIONALE_VISIBLE ->
                    "请先确认下方用途、记录和非医疗边界；确认后才会触发系统权限弹窗。"
                HeartRateBlePermissionStatus.GRANTED ->
                    "蓝牙权限已允许。你可以主动扫描并选择心率设备。"
                HeartRateBlePermissionStatus.DENIED ->
                    "权限未赋予。你可以稍后再次点击授权蓝牙权限重试；关闭心率显示后不会继续请求权限。"
                HeartRateBlePermissionStatus.PERMANENTLY_DENIED ->
                    "权限未赋予，系统可能不再弹出授权窗口。请到系统设置中为 TrainFlow 开启蓝牙权限。"
            }
        }

    val blePermissionRationaleTitle: String = "授权前说明"

    val blePermissionRationaleBullets: List<String> = listOf(
        "用途：查找并连接你主动选择的蓝牙心率设备。",
        "不用途：不使用系统悬浮窗，不后台无限扫描，不无提示扫描。",
        "记录边界：无训练时只显示不记录；训练记录采样另拆后续任务。",
        "非医疗边界：心率区间仅训练参考，不诊断疾病，不替代医生建议，不自动中断训练。"
    )
}

internal enum class HeartRateBlePermissionStatus(
    val label: String,
    val canPrepare: Boolean
) {
    NOT_REQUESTED(label = "待授权", canPrepare = true),
    RATIONALE_VISIBLE(label = "说明待确认", canPrepare = false),
    GRANTED(label = "已允许", canPrepare = false),
    DENIED(label = "权限未赋予", canPrepare = true),
    PERMANENTLY_DENIED(label = "需到系统设置开启", canPrepare = true)
}

internal data class HeartRateDevicePickerUiState(
    val status: HeartRateDevicePickerStatus = HeartRateDevicePickerStatus.DISABLED,
    val scanActive: Boolean = false,
    val title: String = "设备来源：未启用",
    val body: String = "心率显示关闭时不会扫描或连接设备。",
    val actionLabel: String? = null,
    val canStartScan: Boolean = false,
    val canStopScan: Boolean = false,
    val connectionStatusLabel: String = "未连接",
    val devices: List<HeartRateDeviceCandidateUiState> = emptyList(),
    val hrsCandidates: List<HeartRateDeviceCandidateUiState> = devices,
    val scanWindowCopy: String = "扫描窗口约 12 秒，到时会自动停止。",
    val bandHint: String = "HUAWEI Band 9 需要先在手环开启心率广播模式。"
) {
    val showDeviceList: Boolean
        get() = hrsCandidates.isNotEmpty()
}

internal data class HeartRateDeviceCandidateUiState(
    val identifier: String,
    val displayName: String,
    val safeIdentifier: String,
    val signalSummary: String,
    val capabilitySummary: String
)

internal enum class HeartRateDevicePickerStatus {
    DISABLED,
    PERMISSION_REQUIRED,
    BLUETOOTH_DISABLED,
    IDLE_NO_SOURCE,
    SCANNING,
    DEVICES_FOUND,
    NO_DEVICES_FOUND,
    SAVED_DEVICE_NOT_FOUND,
    SELECTED,
    SCAN_FAILED
}

internal data class UiSkinPreferenceOption(
    val id: String,
    val displayName: String,
    val description: String,
    val targetUser: String,
    val capabilityBoundary: String,
    val selected: Boolean,
    val isDefault: Boolean
)

internal enum class StrengthSetTimerModePreference(
    val contractValue: String,
    val label: String,
    val description: String
) {
    MANUAL_START(
        contractValue = "manual_start",
        label = "手动开始",
        description = "休息结束后等待用户点按开始本组。"
    ),
    AUTO_AFTER_REST(
        contractValue = "auto_after_rest",
        label = "休息后自动",
        description = "休息结束后默认进入本组计时，仍保留训练中控制边界。"
    )
}

internal fun strengthSetTimerModePreferenceFromContract(
    contractValue: String
): StrengthSetTimerModePreference {
    return StrengthSetTimerModePreference.entries.firstOrNull { mode ->
        mode.contractValue == contractValue
    } ?: StrengthSetTimerModePreference.MANUAL_START
}

internal fun defaultTrainingPreferencesScreenState(): TrainingPreferencesScreenState {
    return TrainingPreferencesScreenState()
}

internal fun heartRateSettingsUiState(
    enabled: Boolean,
    savedDeviceIdentifier: String? = null,
    savedDeviceDisplayName: String? = null,
    blePermissionStatus: HeartRateBlePermissionStatus = HeartRateBlePermissionStatus.NOT_REQUESTED,
    providerState: BleHeartRateProviderState = BleHeartRateProviderState.noSource(),
    scanState: BleHeartRateScanState = BleHeartRateScanState.idle(),
    scannerCandidates: List<BleHeartRateDeviceCandidate> = emptyList(),
    scanActive: Boolean = scanState.kind == BleHeartRateScanStateKind.SCANNING,
    scanFinishedWithoutDevices: Boolean = false,
    scanWindowSeconds: Int = 12
): HeartRateSettingsUiState {
    val sanitizedIdentifier = savedDeviceIdentifier?.takeIf { identifier ->
        identifier.isNotBlank()
    }
    val sanitizedDisplayName = savedDeviceDisplayName?.takeIf { displayName ->
        displayName.isNotBlank()
    }
    val resolvedPermissionStatus = if (enabled) {
        blePermissionStatus
    } else {
        HeartRateBlePermissionStatus.NOT_REQUESTED
    }
    return HeartRateSettingsUiState(
        enabled = enabled,
        savedDeviceIdentifier = sanitizedIdentifier,
        savedDeviceDisplayName = sanitizedDisplayName,
        blePermissionStatus = resolvedPermissionStatus,
        devicePickerState = heartRateDevicePickerUiState(
            displayEnabled = enabled,
            blePermissionStatus = resolvedPermissionStatus,
            providerState = providerState,
            scanState = scanState,
            scannerCandidates = scannerCandidates,
            scanActive = scanActive,
            savedDeviceIdentifier = sanitizedIdentifier,
            savedDeviceDisplayName = sanitizedDisplayName,
            scanFinishedWithoutDevices = scanFinishedWithoutDevices,
            scanWindowSeconds = scanWindowSeconds
        )
    )
}

internal fun heartRateDevicePickerUiState(
    displayEnabled: Boolean,
    blePermissionStatus: HeartRateBlePermissionStatus,
    providerState: BleHeartRateProviderState = BleHeartRateProviderState.noSource(),
    scanState: BleHeartRateScanState = BleHeartRateScanState.idle(),
    scannerCandidates: List<BleHeartRateDeviceCandidate> = emptyList(),
    scanActive: Boolean = scanState.kind == BleHeartRateScanStateKind.SCANNING,
    savedDeviceIdentifier: String? = null,
    savedDeviceDisplayName: String? = null,
    scanFinishedWithoutDevices: Boolean = false,
    savedDeviceReconnectNotFound: Boolean = false,
    scanPurpose: HeartRateDeviceScanPurpose = HeartRateDeviceScanPurpose.NONE,
    scanWindowSeconds: Int = 12
): HeartRateDevicePickerUiState {
    val heartRateCandidates = scannerCandidates
        .filter { candidate -> candidate.advertisesHeartRateService }
        .sortedWith(
            compareByDescending<BleHeartRateDeviceCandidate> { candidate ->
                candidate.advertisesHeartRateService
            }.thenByDescending { candidate -> candidate.rssi ?: Int.MIN_VALUE }
        )
        .map { candidate -> candidate.toUiState() }
    val savedName = savedDeviceDisplayName?.takeIf { it.isNotBlank() }
    val savedId = savedDeviceIdentifier?.takeIf { it.isNotBlank() }

    if (!displayEnabled) {
        return HeartRateDevicePickerUiState()
    }

    if (blePermissionStatus != HeartRateBlePermissionStatus.GRANTED) {
        return HeartRateDevicePickerUiState(
            status = HeartRateDevicePickerStatus.PERMISSION_REQUIRED,
            title = "设备来源：需要蓝牙权限",
            body = "请先完成上方蓝牙授权流程；未授权时不会扫描设备。"
        )
    }

    when (providerState.kind) {
        BleHeartRateProviderStateKind.PERMISSION_REQUIRED -> HeartRateDevicePickerUiState(
            status = HeartRateDevicePickerStatus.PERMISSION_REQUIRED,
            title = "设备来源：需要蓝牙权限",
            body = "系统权限状态已变化。请先重新授权蓝牙权限，TrainFlow 不会继续扫描。"
        )

        BleHeartRateProviderStateKind.BLUETOOTH_DISABLED,
        BleHeartRateProviderStateKind.UNAVAILABLE -> HeartRateDevicePickerUiState(
            status = HeartRateDevicePickerStatus.BLUETOOTH_DISABLED,
            title = "设备来源：蓝牙关闭",
            body = "蓝牙已关闭或当前设备不可用。开启蓝牙后再扫描心率设备。",
            actionLabel = "扫描心率设备"
        )

        else -> null
    }?.let { unavailableState -> return unavailableState }

    val selectedName = providerState.selectedDevice?.displayName ?: savedName
    val selectedId = providerState.selectedDevice?.identifier ?: savedId
    val hasSelectedSource = selectedName != null || selectedId != null
    if (scanActive || scanState.kind == BleHeartRateScanStateKind.SCANNING) {
        return scanningState(
            heartRateCandidates = heartRateCandidates,
            scanWindowSeconds = scanWindowSeconds,
            hasSelectedSource = hasSelectedSource,
            scanPurpose = scanPurpose,
            providerState = providerState
        )
    }

    if (scanState.kind == BleHeartRateScanStateKind.ERROR) {
        return HeartRateDevicePickerUiState(
            status = HeartRateDevicePickerStatus.SCAN_FAILED,
            title = "设备来源：扫描失败",
            body = "扫描未完成。当前连接状态不会被扫描失败覆盖；请确认蓝牙和权限后重试。",
            actionLabel = "重新扫描",
            canStartScan = true,
            devices = heartRateCandidates,
            hrsCandidates = heartRateCandidates,
            scanWindowCopy = "每次扫描窗口约 ${scanWindowSeconds} 秒。"
        )
    }

    if (scanState.kind == BleHeartRateScanStateKind.STOPPED) {
        return when {
            savedDeviceReconnectNotFound -> HeartRateDevicePickerUiState(
                status = HeartRateDevicePickerStatus.SAVED_DEVICE_NOT_FOUND,
                title = "设备来源：未找到已保存设备",
                body = "这次没有找到与已保存 identifier 完全匹配的设备。不会因同名设备自动连接；可从下方候选列表手动选择。",
                actionLabel = "连接已保存设备",
                canStartScan = true,
                devices = heartRateCandidates,
                hrsCandidates = heartRateCandidates,
                connectionStatusLabel = providerState.connectionStatusLabel(),
                scanWindowCopy = "每次连接尝试约 ${scanWindowSeconds} 秒，到时自动停止。"
            )

            heartRateCandidates.isNotEmpty() -> HeartRateDevicePickerUiState(
                status = HeartRateDevicePickerStatus.DEVICES_FOUND,
                title = "设备来源：发现心率设备",
                body = "扫描已停止。未选择新设备时继续使用当前连接；已发现设备仍可选择。",
                actionLabel = "重新扫描",
                canStartScan = true,
                devices = heartRateCandidates,
                hrsCandidates = heartRateCandidates,
                connectionStatusLabel = providerState.connectionStatusLabel(),
                scanWindowCopy = "每次扫描窗口约 ${scanWindowSeconds} 秒。"
            )

            scanFinishedWithoutDevices -> HeartRateDevicePickerUiState(
                status = HeartRateDevicePickerStatus.NO_DEVICES_FOUND,
                title = "设备来源：未发现心率设备",
                body = "这次没有找到心率设备。请确认设备已开启心率广播模式并靠近手机。",
                actionLabel = "重新扫描",
                canStartScan = true,
                scanWindowCopy = "每次扫描窗口约 ${scanWindowSeconds} 秒。"
            )

            else -> idleOrSelectedState(
                identifier = selectedId,
                displayName = selectedName,
                providerState = providerState,
                scanWindowSeconds = scanWindowSeconds
            )
        }
    }

    return when (providerState.kind) {
        BleHeartRateProviderStateKind.DEVICE_SELECTED -> selectedState(
            displayName = selectedName,
            identifier = selectedId,
            providerState = providerState,
            scanWindowSeconds = scanWindowSeconds
        )

        else -> idleOrSelectedState(
            identifier = selectedId,
            displayName = selectedName,
            providerState = providerState,
            scanWindowSeconds = scanWindowSeconds
        )
    }
}

private fun scanningState(
    heartRateCandidates: List<HeartRateDeviceCandidateUiState>,
    scanWindowSeconds: Int,
    hasSelectedSource: Boolean,
    scanPurpose: HeartRateDeviceScanPurpose,
    providerState: BleHeartRateProviderState
): HeartRateDevicePickerUiState {
    val scanningSavedDevice = scanPurpose == HeartRateDeviceScanPurpose.CONNECT_SAVED_DEVICE
    val scanningOtherDevices = providerState.kind == BleHeartRateProviderStateKind.LIVE_BPM &&
        scanPurpose == HeartRateDeviceScanPurpose.SCAN_OTHER_DEVICES
    return HeartRateDevicePickerUiState(
        status = HeartRateDevicePickerStatus.SCANNING,
        scanActive = true,
        title = when {
            scanningSavedDevice -> "设备来源：正在查找已保存设备"
            scanningOtherDevices || hasSelectedSource -> "设备来源：正在扫描其他设备"
            else -> "设备来源：扫描中"
        },
        body = when {
            scanningSavedDevice -> "仅会自动连接 identifier 完全匹配的已保存设备；不会按名称自动连接。"
            scanningOtherDevices || hasSelectedSource ->
            "当前连接和心率胶囊不会因扫描中断；只有选择新设备后才会切换连接目标。"
            else -> "正在查找附近广播标准心率服务的设备；扫描窗口约 ${scanWindowSeconds} 秒。"
        },
        actionLabel = "停止扫描",
        canStopScan = true,
        connectionStatusLabel = providerState.connectionStatusLabel(),
        devices = heartRateCandidates,
        hrsCandidates = heartRateCandidates,
        scanWindowCopy = "正在扫描，约 ${scanWindowSeconds} 秒后自动停止。"
    )
}

private fun idleOrSelectedState(
    identifier: String?,
    displayName: String?,
    providerState: BleHeartRateProviderState,
    scanWindowSeconds: Int
): HeartRateDevicePickerUiState {
    return if (displayName != null || identifier != null) {
        selectedState(displayName, identifier, providerState, scanWindowSeconds)
    } else {
        HeartRateDevicePickerUiState(
            status = HeartRateDevicePickerStatus.IDLE_NO_SOURCE,
            title = "设备来源：未连接源",
            body = "还没有选择心率设备。请先让设备进入心率广播模式，再手动扫描。",
            actionLabel = "扫描心率设备",
            canStartScan = true,
            scanWindowCopy = "每次扫描窗口约 ${scanWindowSeconds} 秒。"
        )
    }
}

private fun selectedState(
    displayName: String?,
    identifier: String?,
    providerState: BleHeartRateProviderState,
    scanWindowSeconds: Int
): HeartRateDevicePickerUiState {
    val name = displayName ?: "已保存设备"
    val idCopy = identifier?.let { "（${maskDeviceIdentifier(it)}）" }.orEmpty()
    return HeartRateDevicePickerUiState(
        status = HeartRateDevicePickerStatus.SELECTED,
        title = if (providerState.kind == BleHeartRateProviderStateKind.LIVE_BPM) {
            "设备来源：已连接设备"
        } else {
            "设备来源：已保存设备"
        },
        body = if (providerState.kind == BleHeartRateProviderStateKind.LIVE_BPM) {
            "$name$idCopy 正在提供实时心率；扫描其他设备不会中断当前连接。"
        } else {
            "$name$idCopy 仅保存为你主动连接时的偏好，不代表设备在附近、已开启广播、正在连接或已经连接。"
        },
        actionLabel = if (providerState.kind == BleHeartRateProviderStateKind.LIVE_BPM) {
            "扫描其他设备"
        } else {
            "连接已保存设备"
        },
        canStartScan = true,
        connectionStatusLabel = providerState.connectionStatusLabel(),
        scanWindowCopy = "每次扫描窗口约 ${scanWindowSeconds} 秒。"
    )
}

internal enum class HeartRateDeviceScanPurpose {
    NONE,
    CONNECT_SAVED_DEVICE,
    SCAN_OTHER_DEVICES,
    SCAN_DEVICES
}

internal fun savedDeviceReconnectCandidateIdentifier(
    savedDeviceIdentifier: String?,
    candidates: List<BleHeartRateDeviceCandidate>
): String? {
    val savedIdentifier = savedDeviceIdentifier?.takeIf { it.isNotBlank() } ?: return null
    return candidates.firstOrNull { candidate ->
        candidate.advertisesHeartRateService && candidate.identifier == savedIdentifier
    }?.identifier
}

private fun BleHeartRateProviderState.connectionStatusLabel(): String = when (kind) {
    BleHeartRateProviderStateKind.CONNECTING -> "正在连接"
    BleHeartRateProviderStateKind.CONNECTED_WAITING_FOR_DATA -> "等待数据"
    BleHeartRateProviderStateKind.LIVE_BPM -> "已连接"
    BleHeartRateProviderStateKind.ERROR -> "连接异常"
    else -> "未连接"
}

private fun BleHeartRateDeviceCandidate.toUiState(): HeartRateDeviceCandidateUiState {
    return HeartRateDeviceCandidateUiState(
        identifier = identifier,
        displayName = displayName,
        safeIdentifier = maskDeviceIdentifier(identifier),
        signalSummary = rssi?.let { "信号 $it dBm" } ?: "信号未知",
        capabilitySummary = if (advertisesHeartRateService) {
            "广播标准心率服务 0x180D"
        } else {
            "未确认心率广播"
        }
    )
}

internal fun maskDeviceIdentifier(identifier: String): String {
    val parts = identifier.split(":")
    return if (parts.size == 6 && parts.all { part -> part.length == 2 }) {
        "${parts[0]}:${parts[1]}:${parts[2]}:**:**:${parts[5]}"
    } else if (identifier.length > 8) {
        "${identifier.take(4)}…${identifier.takeLast(4)}"
    } else {
        identifier
    }
}

internal fun HeartRateSettingsUiState.prepareBlePermissionRationale(): HeartRateSettingsUiState {
    return if (
        enabled &&
        blePermissionStatus in setOf(
            HeartRateBlePermissionStatus.NOT_REQUESTED,
            HeartRateBlePermissionStatus.DENIED
        )
    ) {
        copy(blePermissionStatus = HeartRateBlePermissionStatus.RATIONALE_VISIBLE)
    } else {
        this
    }
}

internal fun resolveHeartRateBlePermissionStatus(
    displayEnabled: Boolean,
    allPermissionsGranted: Boolean,
    requestResult: HeartRateBlePermissionStatus
): HeartRateBlePermissionStatus {
    if (!displayEnabled) {
        return HeartRateBlePermissionStatus.NOT_REQUESTED
    }
    if (allPermissionsGranted) {
        return HeartRateBlePermissionStatus.GRANTED
    }
    return when (requestResult) {
        HeartRateBlePermissionStatus.GRANTED -> HeartRateBlePermissionStatus.NOT_REQUESTED
        HeartRateBlePermissionStatus.RATIONALE_VISIBLE -> HeartRateBlePermissionStatus.RATIONALE_VISIBLE
        HeartRateBlePermissionStatus.DENIED -> HeartRateBlePermissionStatus.DENIED
        HeartRateBlePermissionStatus.PERMANENTLY_DENIED -> HeartRateBlePermissionStatus.PERMANENTLY_DENIED
        HeartRateBlePermissionStatus.NOT_REQUESTED -> HeartRateBlePermissionStatus.NOT_REQUESTED
    }
}

internal fun uiSkinPreferenceOptionsFromRegistry(selectedSkinId: String): List<UiSkinPreferenceOption> {
    val selectedSkin = SkinRegistry.resolve(selectedSkinId)
    return SkinRegistry.skins.map { skin ->
        skin.toUiSkinPreferenceOption(selected = skin.id == selectedSkin.id)
    }
}

private fun TrainFlowSkin.toUiSkinPreferenceOption(selected: Boolean): UiSkinPreferenceOption {
    return UiSkinPreferenceOption(
        id = id,
        displayName = displayName,
        description = description,
        targetUser = targetUser,
        capabilityBoundary = capabilityBoundary,
        selected = selected,
        isDefault = isDefault
    )
}
