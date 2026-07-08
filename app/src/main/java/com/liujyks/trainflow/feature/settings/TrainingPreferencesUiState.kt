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
    val savedDeviceDisplayName: String? = null
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
        "记录边界：当前阶段只保存显示偏好；训练记录采样另拆后续任务。"

    val privacyCopy: String =
        "隐私：无训练时只在 App 内显示状态或实时心率，不写入训练记录。后续训练中记录会另行实现。"

    val nonMedicalCopy: String =
        "非医疗：心率区间仅作训练参考，不诊断疾病，不替代医生建议，不自动中断训练。"

    val permissionCopy: String =
        "权限：BLE 权限只会在后续用户主动选择设备或扫描时请求；本轮不请求权限。"

    val overlayCopy: String =
        "悬浮边界：不使用系统 overlay / 显示在其他应用上层权限，未来胶囊只显示在 TrainFlow App 内。"

    val enabledBoundaryCopy: String
        get() = if (enabled) {
            "开启后仅表示已启用显示偏好；不会自动扫描、连接或申请权限。"
        } else {
            "关闭状态下不会显示心率胶囊，也不会扫描、连接或记录。"
        }

    val canClearSavedDevice: Boolean
        get() = savedDeviceDisplayName != null
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
    savedDeviceDisplayName: String?
): HeartRateSettingsUiState {
    return HeartRateSettingsUiState(
        enabled = enabled,
        savedDeviceDisplayName = savedDeviceDisplayName?.takeIf { displayName ->
            displayName.isNotBlank()
        }
    )
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
