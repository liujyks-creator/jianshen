package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import com.liujyks.trainflow.ui.theme.TrainFlowMotionTokens

internal fun timedRouteLocalLayoutTransitionSpec(
    reduceMotion: Boolean = false
): TweenSpec<Float> {
    return tween(
        durationMillis = motionDuration(
            reduceMotion = reduceMotion,
            durationMillis = TrainFlowMotionTokens.LocalLayoutTransitionDurationMillis
        ),
        easing = TrainFlowMotionTokens.StandardEasing
    )
}

internal fun timerDialTouchFeedbackSpec(
    reduceMotion: Boolean = false
): TweenSpec<Float> {
    return tween(
        durationMillis = motionDuration(
            reduceMotion = reduceMotion,
            durationMillis = TrainFlowMotionTokens.TouchFeedbackDurationMillis
        ),
        easing = TrainFlowMotionTokens.StandardEasing
    )
}

internal fun timerDialPlayPauseStateTransitionSpec(
    reduceMotion: Boolean = false
): TweenSpec<Float> {
    return timerDialStateTransitionSpec(reduceMotion)
}

internal fun timerDialMarkerRingStateTransitionSpec(
    reduceMotion: Boolean = false
): TweenSpec<Float> {
    return timerDialStateTransitionSpec(reduceMotion)
}

internal fun timedRestExtensionStateTransitionSpec(
    reduceMotion: Boolean = false
): TweenSpec<Float> {
    return timerDialStateTransitionSpec(reduceMotion)
}

internal fun timerDialPauseMorphSpec(
    reduceMotion: Boolean = false
): TweenSpec<Float> {
    return timedRouteLocalLayoutTransitionSpec(reduceMotion)
}

internal fun timerDialPauseMorphTarget(
    isPaused: Boolean,
    reduceMotion: Boolean
): Float {
    return when {
        reduceMotion && isPaused -> 1f
        isPaused -> 1f
        else -> 0f
    }
}

internal fun timerDialColorStateTransitionSpec(
    reduceMotion: Boolean = false
): TweenSpec<Color> {
    return tween(
        durationMillis = motionDuration(
            reduceMotion = reduceMotion,
            durationMillis = TrainFlowMotionTokens.StateTransitionDurationMillis
        ),
        easing = TrainFlowMotionTokens.StandardEasing
    )
}

internal fun timerDialFinalPulseAnimationSpec(
    reduceMotion: Boolean = false
): TweenSpec<Float> {
    return tween(
        durationMillis = motionDuration(
            reduceMotion = reduceMotion,
            durationMillis = TrainFlowMotionTokens.StateTransitionDurationMillis
        ),
        easing = TrainFlowMotionTokens.EmphasisEasing
    )
}

internal fun readyStartTouchScaleTarget(
    pressed: Boolean,
    reduceMotion: Boolean
): Float {
    return if (pressed && !reduceMotion) {
        TrainFlowMotionTokens.TouchFeedbackScale
    } else {
        1f
    }
}

internal fun timerDialCenterTouchScaleTarget(
    pressed: Boolean,
    canTogglePause: Boolean,
    reduceMotion: Boolean
): Float {
    return if (pressed && canTogglePause && !reduceMotion) {
        TrainFlowMotionTokens.TouchFeedbackScale
    } else {
        1f
    }
}

internal fun timedRestExtensionTouchScaleTarget(
    pressed: Boolean,
    canExtendRest: Boolean,
    buttonEnabled: Boolean,
    reduceMotion: Boolean
): Float {
    return if (pressed && canExtendRest && buttonEnabled && !reduceMotion) {
        TrainFlowMotionTokens.TouchFeedbackScale
    } else {
        1f
    }
}

internal fun timerDialFinalPulseTarget(
    isFinalCountdown: Boolean,
    isPaused: Boolean,
    reduceMotion: Boolean
): Float {
    return if (isFinalCountdown && !isPaused && !reduceMotion) {
        1f
    } else {
        0f
    }
}

internal fun timerDialRunningLayerAlpha(morphProgress: Float): Float {
    return (1f - morphProgress).coerceIn(0f, 1f)
}

internal fun timerDialRunningLayerScale(morphProgress: Float): Float {
    return (1f - 0.10f * morphProgress.coerceIn(0f, 1f)).coerceIn(0.90f, 1f)
}

internal fun timerDialPausedCircleAlpha(morphProgress: Float): Float {
    return ((morphProgress - 0.12f) / 0.88f).coerceIn(0f, 1f)
}

internal fun timerDialPausedCircleScale(morphProgress: Float): Float {
    return (0.72f + 0.28f * morphProgress.coerceIn(0f, 1f)).coerceIn(0.72f, 1f)
}

internal fun timerDialPausedContentAlpha(morphProgress: Float): Float {
    return ((morphProgress - 0.62f) / 0.38f).coerceIn(0f, 1f)
}

internal fun timerDialPausedSupportingAlpha(morphProgress: Float): Float {
    return ((morphProgress - 0.18f) / 0.82f).coerceIn(0f, 1f)
}

internal fun timerDialSupportingContentAlpha(morphProgress: Float): Float {
    return (1f - 0.08f * morphProgress.coerceIn(0f, 1f)).coerceIn(0.92f, 1f)
}

private fun timerDialStateTransitionSpec(
    reduceMotion: Boolean = false
): TweenSpec<Float> {
    return tween(
        durationMillis = motionDuration(
            reduceMotion = reduceMotion,
            durationMillis = TrainFlowMotionTokens.StateTransitionDurationMillis
        ),
        easing = TrainFlowMotionTokens.StandardEasing
    )
}

private fun motionDuration(
    reduceMotion: Boolean,
    durationMillis: Int
): Int {
    return if (reduceMotion) {
        TrainFlowMotionTokens.ReduceMotionPolicy.fallbackDurationMillis
    } else {
        durationMillis
    }
}
