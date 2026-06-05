package com.liujyks.trainflow.ui.theme

import androidx.compose.ui.graphics.Color

enum class BuiltInUiSkin(val id: String) {
    OFFICIAL_FLOW("official_flow"),
    TILE_FLOW("tile_flow"),
    BIG_TYPE("big_type")
}

data class TrainFlowSkin(
    val builtInSkin: BuiltInUiSkin,
    val displayName: String,
    val description: String,
    val targetUser: String,
    val capabilityBoundary: String,
    val tokens: TrainFlowSkinTokens,
    val isDefault: Boolean = false
) {
    val id: String = builtInSkin.id
}

data class TrainFlowSkinTokens(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val action: Color,
    val focus: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val neutral50: Color,
    val neutral100: Color,
    val neutral200: Color,
    val neutral500: Color,
    val neutral700: Color,
    val neutral900: Color,
    val error: Color,
    val pageHorizontalPaddingDp: Int,
    val sectionSpacingDp: Int,
    val cardCornerDp: Int,
    val prominentCardCornerDp: Int,
    val fontScale: Float,
    val timerScale: Float,
    val trainingButtonHeightDp: Int,
    val secondaryButtonHeightDp: Int,
    val executionPanelPaddingDp: Int,
    val executionControlReserveDp: Int
)

val TrainFlowSkin.isTileFlow: Boolean
    get() = builtInSkin == BuiltInUiSkin.TILE_FLOW

val TrainFlowSkin.isBigType: Boolean
    get() = builtInSkin == BuiltInUiSkin.BIG_TYPE

object SkinRegistry {
    val defaultSkin: TrainFlowSkin = TrainFlowSkin(
        builtInSkin = BuiltInUiSkin.OFFICIAL_FLOW,
        displayName = "Official Flow",
        description = "当前 DESIGN.md 官方默认皮肤，保持清晰、克制、专业的训练执行体验。",
        targetUser = "默认推荐，适合大多数计时与力量训练用户。",
        capabilityBoundary = "只映射官方 token 和组件表现，不改变训练命令、事件、计划或记录语义。",
        isDefault = true,
        tokens = TrainFlowSkinTokens(
            primary = TrainFlowPrimary,
            secondary = TrainFlowSecondary,
            accent = TrainFlowAccent,
            action = TrainFlowAction,
            focus = TrainFlowFocus,
            surface = TrainFlowSurface,
            surfaceMuted = TrainFlowSurfaceMuted,
            neutral50 = TrainFlowNeutral50,
            neutral100 = TrainFlowNeutral100,
            neutral200 = TrainFlowNeutral200,
            neutral500 = TrainFlowNeutral500,
            neutral700 = TrainFlowNeutral700,
            neutral900 = TrainFlowNeutral900,
            error = TrainFlowError,
            pageHorizontalPaddingDp = 20,
            sectionSpacingDp = 16,
            cardCornerDp = 10,
            prominentCardCornerDp = 14,
            fontScale = 1.0f,
            timerScale = 1.0f,
            trainingButtonHeightDp = 48,
            secondaryButtonHeightDp = 48,
            executionPanelPaddingDp = 22,
            executionControlReserveDp = 160
        )
    )

    val skins: List<TrainFlowSkin> = listOf(
        defaultSkin,
        TrainFlowSkin(
            builtInSkin = BuiltInUiSkin.TILE_FLOW,
            displayName = "Tile Flow",
            description = "清爽、模块化的磁贴式皮肤，用不同大小与轻量色彩区分训练入口优先级。",
            targetUser = "偏好更明快卡片和信息块的居家训练用户。",
            capabilityBoundary = "只改变页面表现与布局倾向，不改变训练流程、数据、权限或核心引擎语义。",
            tokens = TrainFlowSkinTokens(
                primary = Color(0xFF10233A),
                secondary = Color(0xFF173654),
                accent = Color(0xFF35B7A5),
                action = Color(0xFFFF7357),
                focus = Color(0xFF4E8EFF),
                surface = Color(0xFFFFFFFF),
                surfaceMuted = Color(0xFFF2F6F4),
                neutral50 = Color(0xFFFBFCFD),
                neutral100 = Color(0xFFE4EFEA),
                neutral200 = Color(0xFFC8DCD4),
                neutral500 = Color(0xFF5F7280),
                neutral700 = Color(0xFF314554),
                neutral900 = Color(0xFF10202C),
                error = TrainFlowError,
                pageHorizontalPaddingDp = 16,
                sectionSpacingDp = 12,
                cardCornerDp = 18,
                prominentCardCornerDp = 22,
                fontScale = 1.0f,
                timerScale = 1.0f,
                trainingButtonHeightDp = 48,
                secondaryButtonHeightDp = 48,
                executionPanelPaddingDp = 22,
                executionControlReserveDp = 160
            )
        ),
        TrainFlowSkin(
            builtInSkin = BuiltInUiSkin.BIG_TYPE,
            displayName = "Big Type",
            description = "远距离可读的大字训练皮肤，突出当前动作、主时间、本组目标和固定底部控制。",
            targetUser = "需要运动中少信息、大按钮、高对比和更强扫读层级的用户。",
            capabilityBoundary = "只重排首页与关键训练执行表现；信息密集页沿用现有组合，不改变训练流程、数据、权限或核心引擎语义。",
            tokens = TrainFlowSkinTokens(
                primary = Color(0xFF070B0D),
                secondary = Color(0xFF151D21),
                accent = Color(0xFF5EE0A5),
                action = Color(0xFFC94432),
                focus = Color(0xFF78B5FF),
                surface = Color(0xFFFFFFFF),
                surfaceMuted = Color(0xFFF4F6F7),
                neutral50 = Color(0xFFFFFFFF),
                neutral100 = Color(0xFFE9EEF0),
                neutral200 = Color(0xFFCCD6DA),
                neutral500 = Color(0xFF63727A),
                neutral700 = Color(0xFF28343A),
                neutral900 = Color(0xFF080D10),
                error = TrainFlowError,
                pageHorizontalPaddingDp = 16,
                sectionSpacingDp = 12,
                cardCornerDp = 8,
                prominentCardCornerDp = 10,
                fontScale = 1.2f,
                timerScale = 1.34f,
                trainingButtonHeightDp = 64,
                secondaryButtonHeightDp = 52,
                executionPanelPaddingDp = 18,
                executionControlReserveDp = 188
            )
        )
    )

    fun resolve(skinId: String?): TrainFlowSkin {
        return skins.firstOrNull { skin -> skin.id == skinId } ?: defaultSkin
    }
}
