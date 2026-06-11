package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.ui.graphics.Color
import com.liujyks.trainflow.ui.theme.TrainFlowSkin
import com.liujyks.trainflow.ui.theme.isBigType

internal enum class TimerDialVisualVariant {
    BLACK_RED_HIGH_CONTRAST,
    CYBER_NEON,
    OFFICIAL_FLOW
}

internal val ProductionTimerDialVisualVariant = TimerDialVisualVariant.OFFICIAL_FLOW

internal val PreviewOnlyTimerDialVisualVariants = setOf(
    TimerDialVisualVariant.BLACK_RED_HIGH_CONTRAST,
    TimerDialVisualVariant.CYBER_NEON
)

internal enum class TimerDialStageType {
    WARMUP,
    WORK,
    REST,
    COOLDOWN,
    CUSTOM
}

internal data class TimerDialVisualTokens(
    val pageBackground: Color,
    val dialSurface: Color,
    val track: Color,
    val totalProgress: Color,
    val centerSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val pausedOverlay: Color,
    val finalCountdown: Color,
    val glowAlpha: Float,
    val work: Color,
    val rest: Color,
    val warmup: Color,
    val cooldown: Color,
    val custom: Color
) {
    fun colorFor(stageType: TimerDialStageType): Color {
        return when (stageType) {
            TimerDialStageType.WARMUP -> warmup
            TimerDialStageType.WORK -> work
            TimerDialStageType.REST -> rest
            TimerDialStageType.COOLDOWN -> cooldown
            TimerDialStageType.CUSTOM -> custom
        }
    }
}

internal fun TimerDialVisualVariant.tokens(skin: TrainFlowSkin): TimerDialVisualTokens {
    return when (this) {
        TimerDialVisualVariant.BLACK_RED_HIGH_CONTRAST -> TimerDialVisualTokens(
            pageBackground = Color(0xFF080A0D),
            dialSurface = Color(0xFF14181E),
            track = Color(0xFF2A3038),
            totalProgress = Color(0xFFFFFFFF),
            centerSurface = Color(0xFF11151A),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFB8C0CA),
            pausedOverlay = Color(0xCC080A0D),
            finalCountdown = Color(0xFFFF3E34),
            glowAlpha = 0.16f,
            work = Color(0xFFFF453A),
            rest = Color(0xFF4A7D9F),
            warmup = Color(0xFF34C185),
            cooldown = Color(0xFF5F8FD8),
            custom = Color(0xFFD9921E)
        )
        TimerDialVisualVariant.CYBER_NEON -> TimerDialVisualTokens(
            pageBackground = Color(0xFF060912),
            dialSurface = Color(0xFF101424),
            track = Color(0xFF252B40),
            totalProgress = Color(0xFF7CE7FF),
            centerSurface = Color(0xFF0B1020),
            textPrimary = Color(0xFFF7FBFF),
            textSecondary = Color(0xFFB5C5E8),
            pausedOverlay = Color(0xCC060912),
            finalCountdown = Color(0xFFFF4F9D),
            glowAlpha = 0.24f,
            work = Color(0xFFFF4F6D),
            rest = Color(0xFF36E6FF),
            warmup = Color(0xFF2DFFB3),
            cooldown = Color(0xFF8C6BFF),
            custom = Color(0xFFE2B84C)
        )
        TimerDialVisualVariant.OFFICIAL_FLOW -> TimerDialVisualTokens(
            pageBackground = skin.tokens.primary,
            dialSurface = skin.tokens.secondary,
            track = skin.tokens.neutral700.copy(alpha = 0.58f),
            totalProgress = skin.tokens.neutral50,
            centerSurface = skin.tokens.secondary,
            textPrimary = skin.tokens.neutral50,
            textSecondary = skin.tokens.neutral200,
            pausedOverlay = skin.tokens.primary.copy(alpha = 0.78f),
            finalCountdown = skin.tokens.action,
            glowAlpha = 0.08f,
            work = skin.tokens.action,
            rest = skin.tokens.focus,
            warmup = skin.tokens.accent,
            cooldown = Color(0xFF367FD6),
            custom = Color(0xFFD9921E)
        )
    }
}

internal fun TimerDialStageType.strokeWidthDp(): Float {
    return when (this) {
        TimerDialStageType.WORK -> 18f
        TimerDialStageType.REST -> 9f
        TimerDialStageType.WARMUP,
        TimerDialStageType.COOLDOWN -> 12f
        TimerDialStageType.CUSTOM -> 11f
    }
}

internal fun TimerDialStageSegmentUiState.strokeWidthDp(): Float {
    return when {
        isCurrent -> 18f
        stageType == TimerDialStageType.REST -> 8f
        else -> 10f
    }
}

internal data class TimerDialLayoutSpec(
    val dialSizeDp: Int,
    val centerSizeDp: Int,
    val minHeightDp: Int,
    val outerMaxStrokeDp: Float = 20f,
    val innerStrokeDp: Float = 6f,
    val innerInsetDp: Float = 42f
) {
    val outerInsetDp: Float = outerMaxStrokeDp / 2f
    val outerDiameterDp: Float = dialSizeDp - outerMaxStrokeDp
    val innerDiameterDp: Float = dialSizeDp - innerInsetDp * 2f
    val centerClearanceDp: Float = innerDiameterDp - centerSizeDp

    fun keepsDialInsideBounds(): Boolean {
        return dialSizeDp <= minHeightDp &&
            outerDiameterDp > innerDiameterDp &&
            innerDiameterDp > centerSizeDp &&
            centerClearanceDp >= 16f
    }
}

internal fun TrainFlowSkin.timerDialLayoutSpec(): TimerDialLayoutSpec {
    return if (isBigType) {
        TimerDialLayoutSpec(
            dialSizeDp = 284,
            centerSizeDp = 196,
            minHeightDp = 312,
            innerInsetDp = 28f
        )
    } else {
        TimerDialLayoutSpec(
            dialSizeDp = 250,
            centerSizeDp = 172,
            minHeightDp = 276,
            innerInsetDp = 24f
        )
    }
}
