package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.ui.theme.TrainFlowMotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class TimerDialMotionTest {
    @Test
    fun finalCountdownPulseUsesStateEmphasisMotionTokens() {
        val spec = timerDialFinalPulseAnimationSpec()

        assertEquals(TrainFlowMotionTokens.StateTransitionDurationMillis, spec.durationMillis)
        assertNotEquals(TrainFlowMotionTokens.LocalLayoutTransitionDurationMillis, spec.durationMillis)
        assertSame(TrainFlowMotionTokens.EmphasisEasing, spec.easing)
        assertNotSame(TrainFlowMotionTokens.StandardEasing, spec.easing)
    }

    @Test
    fun motionLandingSpecsUseTokenizedDurations() {
        assertEquals(
            TrainFlowMotionTokens.TouchFeedbackDurationMillis,
            timerDialTouchFeedbackSpec().durationMillis
        )
        assertEquals(
            TrainFlowMotionTokens.StateTransitionDurationMillis,
            timerDialPlayPauseStateTransitionSpec().durationMillis
        )
        assertEquals(
            TrainFlowMotionTokens.StateTransitionDurationMillis,
            timerDialMarkerRingStateTransitionSpec().durationMillis
        )
        assertEquals(
            TrainFlowMotionTokens.StateTransitionDurationMillis,
            timerDialColorStateTransitionSpec().durationMillis
        )
        assertEquals(
            TrainFlowMotionTokens.StateTransitionDurationMillis,
            timedRestExtensionStateTransitionSpec().durationMillis
        )
        assertEquals(
            TrainFlowMotionTokens.LocalLayoutTransitionDurationMillis,
            timedRouteLocalLayoutTransitionSpec().durationMillis
        )
    }

    @Test
    fun playPauseAndRestExtensionUseStateTransitionNotLayoutOrPageTiming() {
        val playPause = timerDialPlayPauseStateTransitionSpec()
        val restExtension = timedRestExtensionStateTransitionSpec()

        listOf(playPause, restExtension).forEach { spec ->
            assertEquals(TrainFlowMotionTokens.StateTransitionDurationMillis, spec.durationMillis)
            assertNotEquals(TrainFlowMotionTokens.LocalLayoutTransitionDurationMillis, spec.durationMillis)
            assertNotEquals(TrainFlowMotionTokens.PageTransitionDurationMillis, spec.durationMillis)
            assertSame(TrainFlowMotionTokens.StandardEasing, spec.easing)
        }
    }

    @Test
    fun reduceMotionSpecsSnapWithoutChangingTokenPolicy() {
        assertEquals(TrainFlowMotionTokens.ReducedMotionDurationMillis, timerDialTouchFeedbackSpec(true).durationMillis)
        assertEquals(
            TrainFlowMotionTokens.ReducedMotionDurationMillis,
            timerDialPlayPauseStateTransitionSpec(true).durationMillis
        )
        assertEquals(
            TrainFlowMotionTokens.ReducedMotionDurationMillis,
            timerDialMarkerRingStateTransitionSpec(true).durationMillis
        )
        assertEquals(
            TrainFlowMotionTokens.ReducedMotionDurationMillis,
            timedRestExtensionStateTransitionSpec(true).durationMillis
        )
        assertEquals(
            TrainFlowMotionTokens.ReducedMotionDurationMillis,
            timedRouteLocalLayoutTransitionSpec(true).durationMillis
        )
        assertEquals(0, TrainFlowMotionTokens.ReduceMotionPolicy.fallbackDurationMillis)
    }
}
