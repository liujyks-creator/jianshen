package com.liujyks.trainflow.feature.home

internal data class HomeScreenState(
    val summary: String,
    val primaryEntry: HomeEntryUiState,
    val peerEntries: List<HomeEntryUiState>,
    val futureEntries: List<HomeEntryUiState>
)

internal data class HomeEntryUiState(
    val id: HomeEntryId,
    val title: String,
    val description: String,
    val badge: String,
    val status: String,
    val enabled: Boolean,
    val recommended: Boolean = false
)

internal enum class HomeEntryId {
    TIMED_TRAINING,
    STRENGTH_TRAINING,
    EXERCISE_LIBRARY,
    FOLLOW_ALONG,
    SESSION_RECORDS,
    RECOVERY
}

internal fun buildHomeScreenState(): HomeScreenState {
    return HomeScreenState(
        summary = "把用户自定义训练计划稳定执行完，并留下可回顾记录。",
        primaryEntry = HomeEntryUiState(
            id = HomeEntryId.TIMED_TRAINING,
            title = "计时训练",
            description = "按动作、休息和轮次推进，是新用户默认推荐入口。",
            badge = "推荐默认",
            status = "编辑计时计划",
            enabled = true,
            recommended = true
        ),
        peerEntries = listOf(
            HomeEntryUiState(
                id = HomeEntryId.STRENGTH_TRAINING,
                title = "力量训练",
                description = "面向重量、次数、组数和组间休息的并联训练能力。",
                badge = "同层入口",
                status = "E2.3 接入计划编辑",
                enabled = false
            ),
            HomeEntryUiState(
                id = HomeEntryId.EXERCISE_LIBRARY,
                title = "动作库",
                description = "浏览首批动作，查看短提示、步骤、错误和替代动作。",
                badge = "已可浏览",
                status = "打开动作库",
                enabled = true
            )
        ),
        futureEntries = listOf(
            HomeEntryUiState(
                id = HomeEntryId.FOLLOW_ALONG,
                title = "跟练雏形",
                description = "后续复用计时流程和动作内容，不做课程平台。",
                badge = "后续",
                status = "E6 前收敛边界",
                enabled = false
            ),
            HomeEntryUiState(
                id = HomeEntryId.SESSION_RECORDS,
                title = "训练记录",
                description = "训练完成后沉淀 session records 和基础趋势。",
                badge = "后续",
                status = "E5 接入真实记录",
                enabled = false
            ),
            HomeEntryUiState(
                id = HomeEntryId.RECOVERY,
                title = "恢复建议",
                description = "由训练部位映射基础放松区域，不做医疗判断。",
                badge = "后续",
                status = "E5 接入训练后建议",
                enabled = false
            )
        )
    )
}
