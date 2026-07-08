package com.liujyks.trainflow.feature.settings

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
    val savedDeviceDisplayName: String? = null,
    val blePermissionStatus: HeartRateBlePermissionStatus = HeartRateBlePermissionStatus.NOT_REQUESTED
) {
    val sectionTitle: String = "心率与设备"

    val statusLabel: String
        get() = if (enabled) "已启用" else "未启用"

    val statusSummary: String
        get() = if (enabled) {
            "已启用显示偏好；后续可选择设备。"
        } else {
            "默认关闭。TrainFlow 不显示心率胶囊。"
        }

    val sourceSummary: String
        get() = if (enabled) {
            savedDeviceDisplayName?.let { displayName ->
                "已保存设备名称：$displayName。本轮不会自动扫描或连接。"
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
            "开启后仍不会自动扫描或连接；选择设备会在后续任务实现。"
        } else {
            "关闭状态下不会显示心率胶囊，也不会扫描、连接或记录。"
        }

    val canClearSavedDevice: Boolean
        get() = savedDeviceDisplayName != null

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
                    "蓝牙权限已允许 / 可选择设备。本轮仍不展示真实设备列表，不扫描、不连接。"
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
    PERMANENTLY_DENIED(label = "需到系统设置开启", canPrepare = false)
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
    savedDeviceDisplayName: String?,
    blePermissionStatus: HeartRateBlePermissionStatus = HeartRateBlePermissionStatus.NOT_REQUESTED
): HeartRateSettingsUiState {
    return HeartRateSettingsUiState(
        enabled = enabled,
        savedDeviceDisplayName = savedDeviceDisplayName?.takeIf { displayName ->
            displayName.isNotBlank()
        },
        blePermissionStatus = if (enabled) {
            blePermissionStatus
        } else {
            HeartRateBlePermissionStatus.NOT_REQUESTED
        }
    )
}

internal fun HeartRateSettingsUiState.prepareBlePermissionRationale(): HeartRateSettingsUiState {
    return if (enabled && blePermissionStatus.canPrepare) {
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
