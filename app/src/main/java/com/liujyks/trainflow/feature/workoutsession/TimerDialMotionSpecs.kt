package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import com.liujyks.trainflow.ui.theme.TrainFlowMotionTokens

internal fun timerDialFinalPulseAnimationSpec(): TweenSpec<Float> {
    return tween(
        durationMillis = TrainFlowMotionTokens.StateTransitionDurationMillis,
        easing = TrainFlowMotionTokens.EmphasisEasing
    )
}
