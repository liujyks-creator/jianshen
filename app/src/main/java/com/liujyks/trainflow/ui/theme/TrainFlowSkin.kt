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
    val timerScale: Float
)

val TrainFlowSkin.isTileFlow: Boolean
    get() = builtInSkin == BuiltInUiSkin.TILE_FLOW

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
            timerScale = 1.0f
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
                timerScale = 1.0f
            )
        ),
        TrainFlowSkin(
            builtInSkin = BuiltInUiSkin.BIG_TYPE,
            displayName = "Big Type",
            description = "大字训练皮肤的内置注册占位，本阶段仅放大关键排版倾向。",
            targetUser = "需要运动中更高可读性和更少视觉噪声的用户。",
            capabilityBoundary = "E8.1 不重排执行页；后续 E8.3 才实现完整大字训练形态。",
            tokens = TrainFlowSkinTokens(
                primary = Color(0xFF10161A),
                secondary = Color(0xFF1F2A30),
                accent = Color(0xFF36B87F),
                action = Color(0xFFE15D49),
                focus = Color(0xFF5599F2),
                surface = Color(0xFFFFFFFF),
                surfaceMuted = Color(0xFFF7F8F9),
                neutral50 = TrainFlowNeutral50,
                neutral100 = Color(0xFFE8ECEF),
                neutral200 = Color(0xFFD0D8DD),
                neutral500 = Color(0xFF5B6670),
                neutral700 = Color(0xFF2E3942),
                neutral900 = Color(0xFF0E1418),
                error = TrainFlowError,
                pageHorizontalPaddingDp = 20,
                sectionSpacingDp = 16,
                cardCornerDp = 12,
                prominentCardCornerDp = 14,
                timerScale = 1.16f
            )
        )
    )

    fun resolve(skinId: String?): TrainFlowSkin {
        return skins.firstOrNull { skin -> skin.id == skinId } ?: defaultSkin
    }
}
