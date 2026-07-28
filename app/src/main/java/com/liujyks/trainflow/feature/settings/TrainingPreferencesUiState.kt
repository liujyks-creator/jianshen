package com.liujyks.trainflow.feature.settings

import com.liujyks.trainflow.core.health.BleHeartRateDeviceCandidate
import com.liujyks.trainflow.core.health.BleHeartRateScanState
import com.liujyks.trainflow.core.health.BleHeartRateScanStateKind
import com.liujyks.trainflow.core.health.HeartRateRecoveryPhase
import com.liujyks.trainflow.core.health.HeartRateRecoveryState
import com.liujyks.trainflow.core.health.HeartRateRecoveryStopReason
import com.liujyks.trainflow.core.health.HeartRateRuntimeFact
import com.liujyks.trainflow.core.health.toHeartRateState
import com.liujyks.trainflow.core.model.HeartRateFact
import com.liujyks.trainflow.core.model.HeartRateState
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
    val manualSuppressed: Boolean = false,
    val ageYears: Int? = null,
    val personalMaxHeartRateBpm: Int? = null,
    val alertThresholdBpm: Int? = null,
    val appVisible: Boolean = false,
    val blePermissionStatus: HeartRateBlePermissionStatus = HeartRateBlePermissionStatus.NOT_REQUESTED,
    val heartRateState: HeartRateState = HeartRateRuntimeFact.Disabled.toHeartRateState(),
    val recoveryState: HeartRateRecoveryState = HeartRateRecoveryState.disarmed(
        HeartRateRecoveryStopReason.OPTED_OUT
    ),
    val recoveryPresentation: HeartRateRecoveryPresentation = heartRateRecoveryPresentation(
        recoveryState = recoveryState,
        scanState = BleHeartRateScanState.idle()
    ),
    val devicePickerState: HeartRateDevicePickerUiState = heartRateDevicePickerUiState(
        displayEnabled = enabled,
        blePermissionStatus = blePermissionStatus,
        heartRateState = heartRateState,
        savedDeviceIdentifier = savedDeviceIdentifier,
        savedDeviceDisplayName = savedDeviceDisplayName
    )
) {
    val sectionTitle: String = "心率与设备"

    val statusLabel: String
        get() = if (enabled) "已开启" else "未开启"

    val statusSummary: String
        get() = if (enabled) {
            if (appVisible) {
                "心率功能已开启；满足条件时会在 TrainFlow 前台自动恢复已保存设备。"
            } else {
                "心率功能已开启；非训练后台不会持续扫描或连接。"
            }
        } else {
            "默认关闭。TrainFlow 不显示心率胶囊。"
        }

    val sourceSummary: String
        get() = if (enabled) {
            when {
                savedDeviceIdentifier == null && savedDeviceDisplayName == null ->
                    "未保存设备 / 待选择设备。"
                manualSuppressed -> savedDeviceDisplayName?.let { displayName ->
                    "已保存设备：$displayName。你已断开设备，当前不会自动恢复。"
                } ?: "心率设备已由你断开，当前不会自动恢复。"
                savedDeviceDisplayName != null ->
                    "已保存设备：$savedDeviceDisplayName。实际连接状态见下方。"
                else -> "未连接源 / 待选择设备。"
            }
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
            "满足权限、蓝牙、已保存精确设备且未手动断开时，TrainFlow 前台会自动恢复；每次查找有限时，未找到后会等待再试。"
        } else {
            "关闭状态下不会显示心率胶囊，也不会扫描、连接或记录。"
        }

    val canDisconnect: Boolean
        get() = enabled && !manualSuppressed && savedDeviceIdentifier != null

    val canReconnect: Boolean
        get() = enabled && savedDeviceIdentifier != null

    val visibleConnectionActions: List<HeartRateSettingsActionUiState>
        get() = when {
            !enabled -> emptyList()
            showBlePermissionRationale -> listOf(
                HeartRateSettingsActionUiState(
                    HeartRateSettingsAction.REQUEST_PERMISSION,
                    blePermissionActionLabel
                )
            )
            blePermissionStatus == HeartRateBlePermissionStatus.PERMANENTLY_DENIED -> listOf(
                HeartRateSettingsActionUiState(
                    HeartRateSettingsAction.OPEN_APP_SETTINGS,
                    blePermissionActionLabel
                )
            )
            blePermissionStatus != HeartRateBlePermissionStatus.GRANTED -> listOf(
                HeartRateSettingsActionUiState(
                    HeartRateSettingsAction.PREPARE_PERMISSION,
                    blePermissionActionLabel
                )
            )
            heartRateState.fact == HeartRateFact.BLUETOOTH_OFF -> listOf(
                HeartRateSettingsActionUiState(
                    HeartRateSettingsAction.OPEN_BLUETOOTH_SETTINGS,
                    "打开蓝牙设置"
                )
            )
            devicePickerState.scanActive && devicePickerState.canStopScan -> listOf(
                HeartRateSettingsActionUiState(
                    HeartRateSettingsAction.STOP_SCAN,
                    "停止扫描"
                )
            )
            devicePickerState.scanActive -> listOf(
                HeartRateSettingsActionUiState(
                    HeartRateSettingsAction.CHANGE_DEVICE,
                    "更换设备"
                )
            )
            manualSuppressed -> savedTargetActions(primaryLabel = "重新连接")
            heartRateState.fact in setOf(
                HeartRateFact.CONNECTING,
                HeartRateFact.WAITING_FIRST_DATA,
                HeartRateFact.LIVE
            ) -> listOf(
                HeartRateSettingsActionUiState(
                    HeartRateSettingsAction.DISCONNECT,
                    "断开连接"
                ),
                HeartRateSettingsActionUiState(
                    HeartRateSettingsAction.CHANGE_DEVICE,
                    "更换设备"
                )
            )
            savedDeviceIdentifier != null &&
                heartRateState.fact in setOf(
                    HeartRateFact.DATA_INTERRUPTED,
                    HeartRateFact.LINK_DISCONNECTED,
                    HeartRateFact.TECHNICAL_FAILURE,
                    HeartRateFact.INTENTIONAL_STOP
                ) -> savedTargetActions(primaryLabel = "重新连接")
            savedDeviceIdentifier != null -> savedTargetActions(primaryLabel = "连接已保存设备")
            else -> listOf(
                HeartRateSettingsActionUiState(
                    HeartRateSettingsAction.SCAN_DEVICES,
                    "扫描心率设备"
                )
            )
        }

    val primaryConnectionAction: HeartRateSettingsActionUiState?
        get() = visibleConnectionActions.firstOrNull()

    val secondaryConnectionAction: HeartRateSettingsActionUiState?
        get() = visibleConnectionActions.getOrNull(1)

    val connectionIntentCopy: String
        get() = if (savedDeviceIdentifier == null) {
            recoveryPresentation.body
        } else if (manualSuppressed) {
            "已手动断开：保留设备与个人参数，但重启、回前台、蓝牙或权限恢复都不会自动恢复。"
        } else {
            recoveryPresentation.body
        }

    val disconnectActionCopy: String =
        "断开会立即停止当前心率连接，并保留开关、已保存设备和个人参数。"

    val clearDeviceActionCopy: String =
        "清除只删除已保存设备并断开连接；不关闭心率功能，也不删除个人参数。"

    val optOutActionCopy: String =
        "关闭心率功能会隐藏胶囊并停止扫描、连接和采集；个人参数仍保留。"

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

    private fun savedTargetActions(primaryLabel: String): List<HeartRateSettingsActionUiState> =
        listOf(
            HeartRateSettingsActionUiState(
                HeartRateSettingsAction.RECONNECT,
                primaryLabel
            ),
            HeartRateSettingsActionUiState(
                HeartRateSettingsAction.CHANGE_DEVICE,
                "更换设备"
            )
        )
}

internal data class HeartRateSettingsActionUiState(
    val action: HeartRateSettingsAction,
    val label: String
)

internal enum class HeartRateSettingsAction {
    PREPARE_PERMISSION,
    REQUEST_PERMISSION,
    OPEN_APP_SETTINGS,
    OPEN_BLUETOOTH_SETTINGS,
    SCAN_DEVICES,
    STOP_SCAN,
    RECONNECT,
    CHANGE_DEVICE,
    DISCONNECT
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
    manualSuppressed: Boolean = false,
    ageYears: Int? = null,
    personalMaxHeartRateBpm: Int? = null,
    alertThresholdBpm: Int? = null,
    appVisible: Boolean = false,
    blePermissionStatus: HeartRateBlePermissionStatus = HeartRateBlePermissionStatus.NOT_REQUESTED,
    heartRateState: HeartRateState = HeartRateRuntimeFact.NotConnected().toHeartRateState(),
    recoveryState: HeartRateRecoveryState = HeartRateRecoveryState.disarmed(
        HeartRateRecoveryStopReason.NO_SAVED_TARGET
    ),
    scanState: BleHeartRateScanState = BleHeartRateScanState.idle(),
    scannerCandidates: List<BleHeartRateDeviceCandidate> = emptyList(),
    scanActive: Boolean = scanState.kind == BleHeartRateScanStateKind.SCANNING,
    scanFinishedWithoutDevices: Boolean = false,
    scanPurpose: HeartRateDeviceScanPurpose = HeartRateDeviceScanPurpose.NONE,
    lastCompletedScanPurpose: HeartRateDeviceScanPurpose = HeartRateDeviceScanPurpose.NONE,
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
        manualSuppressed = manualSuppressed,
        ageYears = ageYears,
        personalMaxHeartRateBpm = personalMaxHeartRateBpm,
        alertThresholdBpm = alertThresholdBpm,
        appVisible = appVisible,
        blePermissionStatus = resolvedPermissionStatus,
        heartRateState = heartRateState,
        recoveryState = recoveryState,
        recoveryPresentation = heartRateRecoveryPresentation(recoveryState, scanState),
        devicePickerState = heartRateDevicePickerUiState(
            displayEnabled = enabled,
            blePermissionStatus = resolvedPermissionStatus,
            heartRateState = heartRateState,
            scanState = scanState,
            scannerCandidates = scannerCandidates,
            scanActive = scanActive,
            savedDeviceIdentifier = sanitizedIdentifier,
            savedDeviceDisplayName = sanitizedDisplayName,
            scanFinishedWithoutDevices = scanFinishedWithoutDevices,
            scanPurpose = scanPurpose,
            lastCompletedScanPurpose = lastCompletedScanPurpose,
            recoveryState = recoveryState,
            scanWindowSeconds = scanWindowSeconds
        )
    )
}

internal data class HeartRateRecoveryPresentation(
    val title: String,
    val body: String
)

internal fun heartRateRecoveryPresentation(
    recoveryState: HeartRateRecoveryState,
    scanState: BleHeartRateScanState
): HeartRateRecoveryPresentation = when (recoveryState.phase) {
    HeartRateRecoveryPhase.WAITING_NEXT_WINDOW -> HeartRateRecoveryPresentation(
        title = "等待重新连接",
        body = "当前仍会自动恢复已保存设备；稍后自动开始下一次有限时查找。"
    )
    HeartRateRecoveryPhase.SEARCHING -> HeartRateRecoveryPresentation(
        title = "正在重新连接",
        body = "正在自动查找 identifier 完全匹配的已保存设备，不会按名称切换设备。"
    )
    HeartRateRecoveryPhase.WINDOW_MISSED_ARMED -> HeartRateRecoveryPresentation(
        title = "本次未找到已保存设备",
        body = "本次有限窗口已结束；恢复资格仍成立时之后仍会继续自动查找。"
    )
    HeartRateRecoveryPhase.CONNECTING_OR_CONNECTED -> HeartRateRecoveryPresentation(
        title = "正在连接或已连接",
        body = "已找到精确目标，正在建立连接或保持当前连接。"
    )
    HeartRateRecoveryPhase.DISARMED -> {
        val body = when (recoveryState.stopReason) {
            HeartRateRecoveryStopReason.MANUAL_SUPPRESSION ->
                "你已手动断开；只有点击重新连接或重新选择设备才会恢复。"
            HeartRateRecoveryStopReason.OPTED_OUT -> "心率功能已关闭。"
            HeartRateRecoveryStopReason.NO_SAVED_TARGET -> "尚未保存心率设备。"
            HeartRateRecoveryStopReason.PERMISSION_UNAVAILABLE -> "当前缺少蓝牙权限。"
            HeartRateRecoveryStopReason.BLUETOOTH_OFF -> "当前蓝牙已关闭。"
            HeartRateRecoveryStopReason.BACKGROUND_WITHOUT_FGS ->
                "非训练后台不会持续扫描；回到 TrainFlow 前台后会重新判断恢复资格。"
            HeartRateRecoveryStopReason.OWNER_CLOSED -> "心率运行时已停止。"
            null -> "当前没有自动恢复任务。"
        }
        HeartRateRecoveryPresentation("自动恢复未运行", body)
    }
}

internal fun heartRateDevicePickerUiState(
    displayEnabled: Boolean,
    blePermissionStatus: HeartRateBlePermissionStatus,
    heartRateState: HeartRateState = HeartRateRuntimeFact.NotConnected().toHeartRateState(),
    scanState: BleHeartRateScanState = BleHeartRateScanState.idle(),
    scannerCandidates: List<BleHeartRateDeviceCandidate> = emptyList(),
    scanActive: Boolean = scanState.kind == BleHeartRateScanStateKind.SCANNING,
    savedDeviceIdentifier: String? = null,
    savedDeviceDisplayName: String? = null,
    scanFinishedWithoutDevices: Boolean = false,
    scanPurpose: HeartRateDeviceScanPurpose = HeartRateDeviceScanPurpose.NONE,
    lastCompletedScanPurpose: HeartRateDeviceScanPurpose = HeartRateDeviceScanPurpose.NONE,
    recoveryState: HeartRateRecoveryState = HeartRateRecoveryState.disarmed(
        HeartRateRecoveryStopReason.NO_SAVED_TARGET
    ),
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

    when (heartRateState.fact) {
        HeartRateFact.PERMISSION_REQUIRED -> HeartRateDevicePickerUiState(
            status = HeartRateDevicePickerStatus.PERMISSION_REQUIRED,
            title = "设备来源：需要蓝牙权限",
            body = "系统权限状态已变化。请先重新授权蓝牙权限，TrainFlow 不会继续扫描。"
        )

        HeartRateFact.BLUETOOTH_OFF -> HeartRateDevicePickerUiState(
            status = HeartRateDevicePickerStatus.BLUETOOTH_DISABLED,
            title = "设备来源：蓝牙关闭",
            body = "蓝牙已关闭或当前设备不可用。开启蓝牙后再扫描心率设备。",
            actionLabel = "扫描心率设备"
        )

        else -> null
    }?.let { unavailableState -> return unavailableState }

    val selectedName = heartRateState.sourceLabel ?: savedName
    val selectedId = heartRateState.sourceId ?: savedId
    val hasSelectedSource = selectedName != null || selectedId != null
    if (scanActive || scanState.kind == BleHeartRateScanStateKind.SCANNING) {
        return scanningState(
            heartRateCandidates = heartRateCandidates,
            scanWindowSeconds = scanWindowSeconds,
            hasSelectedSource = hasSelectedSource,
            scanPurpose = scanPurpose,
            automaticRecoverySearching =
                recoveryState.phase == HeartRateRecoveryPhase.SEARCHING,
            heartRateState = heartRateState
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
        if (lastCompletedScanPurpose == HeartRateDeviceScanPurpose.NONE) {
            return idleOrSelectedState(
                identifier = selectedId,
                displayName = selectedName,
                heartRateState = heartRateState,
                scanWindowSeconds = scanWindowSeconds
            )
        }
        return when {
            heartRateCandidates.isNotEmpty() -> HeartRateDevicePickerUiState(
                status = HeartRateDevicePickerStatus.DEVICES_FOUND,
                title = "设备来源：发现心率设备",
                body = "扫描已停止。未选择新设备时继续使用当前连接；已发现设备仍可选择。",
                actionLabel = "重新扫描",
                canStartScan = true,
                devices = heartRateCandidates,
                hrsCandidates = heartRateCandidates,
                connectionStatusLabel = heartRateState.connectionStatusLabel(),
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
                heartRateState = heartRateState,
                scanWindowSeconds = scanWindowSeconds
            )
        }
    }

    return idleOrSelectedState(
        identifier = selectedId,
        displayName = selectedName,
        heartRateState = heartRateState,
        scanWindowSeconds = scanWindowSeconds
    )
}

private fun scanningState(
    heartRateCandidates: List<HeartRateDeviceCandidateUiState>,
    scanWindowSeconds: Int,
    hasSelectedSource: Boolean,
    scanPurpose: HeartRateDeviceScanPurpose,
    automaticRecoverySearching: Boolean,
    heartRateState: HeartRateState
): HeartRateDevicePickerUiState {
    val scanningSavedDevice = scanPurpose == HeartRateDeviceScanPurpose.CONNECT_SAVED_DEVICE
    val scanningOtherDevices = heartRateState.fact == HeartRateFact.LIVE &&
        scanPurpose == HeartRateDeviceScanPurpose.SCAN_OTHER_DEVICES
    return HeartRateDevicePickerUiState(
        status = HeartRateDevicePickerStatus.SCANNING,
        scanActive = true,
        title = when {
            automaticRecoverySearching -> "自动恢复：正在查找已保存设备"
            scanningSavedDevice -> "设备来源：正在查找已保存设备"
            scanningOtherDevices || hasSelectedSource -> "设备来源：正在扫描其他设备"
            else -> "设备来源：扫描中"
        },
        body = when {
            automaticRecoverySearching ->
                "正在自动查找 identifier 完全匹配的已保存设备；这是有限时恢复窗口，不是用户主动扫描。"
            scanningSavedDevice -> "仅会自动连接 identifier 完全匹配的已保存设备；不会按名称自动连接。"
            scanningOtherDevices || hasSelectedSource ->
            "当前连接和心率胶囊不会因扫描中断；只有选择新设备后才会切换连接目标。"
            else -> "正在查找附近广播标准心率服务的设备；扫描窗口约 ${scanWindowSeconds} 秒。"
        },
        actionLabel = "停止扫描".takeUnless { automaticRecoverySearching },
        canStopScan = !automaticRecoverySearching,
        connectionStatusLabel = heartRateState.connectionStatusLabel(),
        devices = heartRateCandidates,
        hrsCandidates = heartRateCandidates,
        scanWindowCopy = "正在扫描，约 ${scanWindowSeconds} 秒后自动停止。"
    )
}

private fun idleOrSelectedState(
    identifier: String?,
    displayName: String?,
    heartRateState: HeartRateState,
    scanWindowSeconds: Int
): HeartRateDevicePickerUiState {
    return if (displayName != null || identifier != null) {
        selectedState(displayName, identifier, heartRateState, scanWindowSeconds)
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
    heartRateState: HeartRateState,
    scanWindowSeconds: Int
): HeartRateDevicePickerUiState {
    val name = displayName ?: "已保存设备"
    val idCopy = identifier?.let { "（${maskDeviceIdentifier(it)}）" }.orEmpty()
    return HeartRateDevicePickerUiState(
        status = HeartRateDevicePickerStatus.SELECTED,
        title = if (heartRateState.fact == HeartRateFact.LIVE) {
            "设备来源：已连接设备"
        } else {
            "设备来源：已保存设备"
        },
        body = if (heartRateState.fact == HeartRateFact.LIVE) {
            "$name$idCopy 正在提供实时心率；扫描其他设备不会中断当前连接。"
        } else {
            "$name$idCopy 是自动恢复和手动重连使用的精确目标；不代表设备在附近、已开启广播、正在连接或已经连接。"
        },
        actionLabel = if (heartRateState.fact == HeartRateFact.LIVE) {
            "扫描其他设备"
        } else {
            "连接已保存设备"
        },
        canStartScan = true,
        connectionStatusLabel = heartRateState.connectionStatusLabel(),
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

private fun HeartRateState.connectionStatusLabel(): String = when (fact) {
    HeartRateFact.CONNECTING -> "正在连接"
    HeartRateFact.WAITING_FIRST_DATA -> "等待数据"
    HeartRateFact.LIVE -> "已连接"
    HeartRateFact.TECHNICAL_FAILURE -> "连接异常"
    HeartRateFact.LINK_DISCONNECTED -> "连接已断开"
    HeartRateFact.INTENTIONAL_STOP -> "已手动断开"
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
