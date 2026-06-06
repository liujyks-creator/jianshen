package com.liujyks.trainflow.core.model

internal object PermissionPrivacyCopy {
    const val NOTIFICATION_PERMISSION: String =
        "通知用于计划提醒和训练中状态提示；关闭后训练仍可正常使用，只是不弹通知。普通通知可能被系统延迟，不是闹钟级强提醒。"

    const val ACTIVE_WORKOUT_NOTIFICATION: String =
        "活跃训练通知只是训练状态摘要，不是 foreground service，不保证后台可靠计时或进程死亡恢复。"

    const val HEART_RATE: String =
        "心率当前是抽象状态 / 占位展示，未接入真实设备、手环、手表或健康数据；不做医疗告警、危险判断或训练强度判断。"

    const val RECOVERY: String =
        "恢复建议基于训练动作 / 部位的基础放松映射，不是医疗诊断、康复治疗或疼痛处理建议。"

    const val AUDIO_PROMPT: String =
        "音频提示是短促训练提示音，目标是不降低、暂停或打断其他 App 音乐 / 视频；不同设备和 Android 版本表现仍需用户测试回看。"

    const val VOICE: String =
        "当前只保留训练命令 / 事件边界，未实现语音控制、语音读秒或自动语音教练。"

    const val DATA: String =
        "当前多数计划、历史、恢复仍是内存态、fixture 或基础展示边界，不代表云同步、账号体系或真实长期记录已完成。"

    val sections: List<PermissionPrivacySection> = listOf(
        PermissionPrivacySection(
            title = "通知权限",
            body = NOTIFICATION_PERMISSION
        ),
        PermissionPrivacySection(
            title = "活跃训练通知",
            body = ACTIVE_WORKOUT_NOTIFICATION
        ),
        PermissionPrivacySection(
            title = "心率",
            body = HEART_RATE
        ),
        PermissionPrivacySection(
            title = "恢复建议",
            body = RECOVERY
        ),
        PermissionPrivacySection(
            title = "音频提示",
            body = AUDIO_PROMPT
        ),
        PermissionPrivacySection(
            title = "语音",
            body = VOICE
        ),
        PermissionPrivacySection(
            title = "数据",
            body = DATA
        )
    )
}

internal data class PermissionPrivacySection(
    val title: String,
    val body: String
)
