package com.liujyks.trainflow.feature.settings

internal data class TrainingPreferencesScreenState(
    val defaultCountdownThresholdSec: Int = 5,
    val actionCueEnabled: Boolean = true,
    val restCueEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val emphasisAnimationEnabled: Boolean = true,
    val strengthSetTimerMode: StrengthSetTimerModePreference = StrengthSetTimerModePreference.MANUAL_START
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
}

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
