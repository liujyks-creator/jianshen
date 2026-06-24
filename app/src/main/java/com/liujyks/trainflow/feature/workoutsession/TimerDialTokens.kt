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
    val innerBaseRing: Color,
    val innerBaseDot: Color,
    val totalProgress: Color,
    val textPrimary: Color,
    val textSecondary: Color,
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
            innerBaseRing = Color(0xFF2A3038).copy(alpha = 0.68f),
            innerBaseDot = Color(0xFFF8FAFC).copy(alpha = 0.24f),
            totalProgress = Color(0xFFFFFFFF),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFB8C0CA),
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
            innerBaseRing = Color(0xFF252B40).copy(alpha = 0.7f),
            innerBaseDot = Color(0xFF7CE7FF).copy(alpha = 0.28f),
            totalProgress = Color(0xFF7CE7FF),
            textPrimary = Color(0xFFF7FBFF),
            textSecondary = Color(0xFFB5C5E8),
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
            innerBaseRing = skin.tokens.neutral700.copy(alpha = 0.34f),
            innerBaseDot = skin.tokens.neutral100.copy(alpha = 0.3f),
            totalProgress = skin.tokens.neutral50,
            textPrimary = skin.tokens.neutral50,
            textSecondary = skin.tokens.neutral200,
            finalCountdown = skin.tokens.action,
            glowAlpha = 0.08f,
            work = skin.tokens.action,
            rest = skin.tokens.focus,
            warmup = skin.tokens.accent,
            cooldown = skin.tokens.focus.copy(alpha = 0.78f),
            custom = skin.tokens.neutral700
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
        isCurrent && stageType == TimerDialStageType.REST -> 9f
        isCurrent -> 12f
        stageType == TimerDialStageType.REST -> 5f
        else -> 7f
    }
}

internal data class TimerDialLayoutSpec(
    val dialSizeDp: Int,
    val centerSizeDp: Int,
    val minHeightDp: Int,
    val outerMaxStrokeDp: Float = 14f,
    val innerStrokeDp: Float = 5f,
    val innerInsetDp: Float = 36f,
    val innerBaseStrokeDp: Float = 24f,
    val innerBaseDotRadiusDp: Float = 5.5f,
    val innerCompletedDotRadiusDp: Float = 8f,
    val innerMarkerRadiusDp: Float = 16f,
    val totalBrushRadiusDp: Float = 9f
) {
    val outerInsetDp: Float = outerMaxStrokeDp / 2f
    val outerDiameterDp: Float = dialSizeDp - outerMaxStrokeDp
    val innerDiameterDp: Float = dialSizeDp - innerInsetDp * 2f
    val centerClearanceDp: Float = innerDiameterDp - centerSizeDp
    val innerMarkerBoundaryRadiusDp: Float = innerBaseStrokeDp / 2f

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
            dialSizeDp = 324,
            centerSizeDp = 184,
            minHeightDp = 346,
            innerInsetDp = 42f
        )
    } else {
        TimerDialLayoutSpec(
            dialSizeDp = 320,
            centerSizeDp = 180,
            minHeightDp = 342,
            innerInsetDp = 42f
        )
    }
}

internal enum class TimerDialDrawLayer {
    OUTER_TRACK,
    OUTER_PROGRESS,
    INNER_BASE_RING,
    INNER_BASE_DOT,
    INNER_TOTAL_PROGRESS,
    INNER_BRUSH,
    INNER_STAGE_MARKER,
    FINAL_COUNTDOWN,
    PAUSED_OVERLAY,
    CENTER_SURFACE
}
