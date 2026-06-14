package com.liujyks.trainflow.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing

internal object TrainFlowMotionTokens {
    const val TouchFeedbackDurationMillis = 100
    const val StateTransitionDurationMillis = 160
    const val LocalLayoutTransitionDurationMillis = 220
    const val PageTransitionDurationMillis = 260
    const val ContinuousProjectionMaxDurationMillis = 1_000L
    const val ReducedMotionDurationMillis = 0

    const val TouchFeedbackScale = 0.97f
    const val PressedAlpha = 0.86f
    const val DisabledMotionAlpha = 0.72f

    val StandardEasing: Easing = CubicBezierEasing(0.16f, 1f, 0.30f, 1f)
    val EmphasisEasing: Easing = CubicBezierEasing(0.34f, 1.16f, 0.64f, 1f)
    val ContinuousProgressEasing: Easing = LinearEasing

    val ReduceMotionPolicy = TrainFlowReduceMotionPolicy(
        fallbackDurationMillis = ReducedMotionDurationMillis,
        snapStateTransitions = true,
        disableNonEssentialMotion = true,
        disableContinuousProjection = true
    )

    val Categories: List<TrainFlowMotionCategory> = listOf(
        TrainFlowMotionCategory(
            name = "touch_feedback",
            durationMillis = TouchFeedbackDurationMillis,
            intendedUse = "press/release, center dial response, small button scale"
        ),
        TrainFlowMotionCategory(
            name = "state_transition",
            durationMillis = StateTransitionDurationMillis,
            intendedUse = "play/pause, confirm +15 sec, marker and phase color states"
        ),
        TrainFlowMotionCategory(
            name = "local_layout_transition",
            durationMillis = LocalLayoutTransitionDurationMillis,
            intendedUse = "ready gate to execution, paused to active, local controls"
        ),
        TrainFlowMotionCategory(
            name = "page_transition",
            durationMillis = PageTransitionDurationMillis,
            intendedUse = "plan detail to ready gate, execution to summary"
        )
    )
}

internal data class TrainFlowMotionCategory(
    val name: String,
    val durationMillis: Int,
    val intendedUse: String
)

internal data class TrainFlowReduceMotionPolicy(
    val fallbackDurationMillis: Int,
    val snapStateTransitions: Boolean,
    val disableNonEssentialMotion: Boolean,
    val disableContinuousProjection: Boolean
)
