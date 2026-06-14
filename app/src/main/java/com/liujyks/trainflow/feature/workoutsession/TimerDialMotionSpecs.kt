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

internal fun timerDialFinalPulseAnimationSpec(): TweenSpec<Float> {
    return tween(
        durationMillis = TrainFlowMotionTokens.StateTransitionDurationMillis,
        easing = TrainFlowMotionTokens.EmphasisEasing
    )
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
