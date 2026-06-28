package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.ui.theme.TrainFlowMotionTokens
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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
    fun reduceMotionDisablesNonEssentialTouchScaleAndPulseTargets() {
        assertEquals(
            TrainFlowMotionTokens.TouchFeedbackScale,
            readyStartTouchScaleTarget(pressed = true, reduceMotion = false),
            0.0001f
        )
        assertEquals(1f, readyStartTouchScaleTarget(pressed = true, reduceMotion = true), 0.0001f)
        assertEquals(
            TrainFlowMotionTokens.TouchFeedbackScale,
            timerDialCenterTouchScaleTarget(pressed = true, canTogglePause = true, reduceMotion = false),
            0.0001f
        )
        assertEquals(
            1f,
            timerDialCenterTouchScaleTarget(pressed = true, canTogglePause = true, reduceMotion = true),
            0.0001f
        )
        assertEquals(
            TrainFlowMotionTokens.TouchFeedbackScale,
            timedRestExtensionTouchScaleTarget(
                pressed = true,
                canExtendRest = true,
                buttonEnabled = true,
                reduceMotion = false
            ),
            0.0001f
        )
        assertEquals(
            1f,
            timedRestExtensionTouchScaleTarget(
                pressed = true,
                canExtendRest = true,
                buttonEnabled = true,
                reduceMotion = true
            ),
            0.0001f
        )
        assertEquals(
            1f,
            timerDialFinalPulseTarget(isFinalCountdown = true, isPaused = false, reduceMotion = false),
            0.0001f
        )
        assertEquals(
            0f,
            timerDialFinalPulseTarget(isFinalCountdown = true, isPaused = false, reduceMotion = true),
            0.0001f
        )
        assertEquals(TrainFlowMotionTokens.ReducedMotionDurationMillis, timerDialFinalPulseAnimationSpec(true).durationMillis)
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
        assertEquals(
            TrainFlowMotionTokens.LocalLayoutTransitionDurationMillis,
            timerDialPauseMorphSpec().durationMillis
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
        assertEquals(
            TrainFlowMotionTokens.ReducedMotionDurationMillis,
            timerDialPauseMorphSpec(true).durationMillis
        )
        assertEquals(0, TrainFlowMotionTokens.ReduceMotionPolicy.fallbackDurationMillis)
    }

    @Test
    fun pauseMorphUsesSharedDialMotionAndSnapsForReduceMotion() {
        assertEquals(0f, timerDialPauseMorphTarget(isPaused = false, reduceMotion = false), 0.0001f)
        assertEquals(1f, timerDialPauseMorphTarget(isPaused = true, reduceMotion = false), 0.0001f)
        assertEquals(1f, timerDialPauseMorphTarget(isPaused = true, reduceMotion = true), 0.0001f)
        assertEquals(0, timerDialPauseMorphSpec(reduceMotion = true).durationMillis)

        assertEquals(1f, timerDialRunningLayerAlpha(0f), 0.0001f)
        assertEquals(0f, timerDialRunningLayerAlpha(1f), 0.0001f)
        assertEquals(1f, timerDialRunningLayerScale(0f), 0.0001f)
        assertEquals(0.9f, timerDialRunningLayerScale(1f), 0.0001f)
        assertEquals(0f, timerDialPausedCircleAlpha(0f), 0.0001f)
        assertEquals(1f, timerDialPausedCircleAlpha(1f), 0.0001f)
        assertEquals(0.72f, timerDialPausedCircleScale(0f), 0.0001f)
        assertEquals(1f, timerDialPausedCircleScale(1f), 0.0001f)
        assertEquals(0f, timerDialPausedContentAlpha(0.5f), 0.0001f)
        assertEquals(1f, timerDialPausedContentAlpha(1f), 0.0001f)
        assertEquals(1f, timerDialSupportingContentAlpha(0f), 0.0001f)
        assertEquals(0.92f, timerDialSupportingContentAlpha(1f), 0.0001f)
    }

    @Test
    fun productionMotionCallSitesConsumeReduceMotionLocal() {
        val routeSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)
        val dialSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDial.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(routeSource.contains("val reduceMotion = LocalTrainFlowReduceMotion.current"))
        assertTrue(routeSource.contains("timedRouteLocalLayoutTransitionSpec(reduceMotion)"))
        assertTrue(routeSource.contains("timerDialPauseMorphSpec(reduceMotion)"))
        assertTrue(routeSource.contains("timerDialTouchFeedbackSpec(reduceMotion)"))
        assertTrue(routeSource.contains("timedRestExtensionStateTransitionSpec(reduceMotion)"))
        assertTrue(routeSource.contains("readyStartTouchScaleTarget("))
        assertTrue(routeSource.contains("timedRestExtensionTouchScaleTarget("))

        assertTrue(dialSource.contains("val reduceMotion = LocalTrainFlowReduceMotion.current"))
        assertTrue(dialSource.contains("safeState.canProjectSmoothProgress(reduceMotion)"))
        assertTrue(dialSource.contains("timerDialSmoothProgressElapsedMillis("))
        assertTrue(dialSource.contains("monotonicDisplayedProgress("))
        assertTrue(dialSource.contains("reduceMotion = reduceMotion"))
        assertTrue(dialSource.contains("timerDialFinalPulseAnimationSpec(reduceMotion)"))
        assertTrue(dialSource.contains("timerDialMarkerRingStateTransitionSpec(reduceMotion)"))
        assertTrue(dialSource.contains("timerDialColorStateTransitionSpec(reduceMotion)"))
        assertTrue(dialSource.contains("timerDialPlayPauseStateTransitionSpec(reduceMotion)"))
        assertFalse(dialSource.contains("canProjectSmoothProgress())"))
    }

    @Test
    fun pausedExecutionUsesSharedMorphInsteadOfFullScreenHardCut() {
        val routeSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(routeSource.contains("TimerDialPauseMorph("))
        assertTrue(routeSource.contains("TimerDialPauseMorphProgress"))
        assertTrue(routeSource.contains("timerDialRunningLayerAlpha(morphProgress)"))
        assertTrue(routeSource.contains("timerDialPausedCircleScale(morphProgress)"))
        assertFalse(routeSource.contains("TimedWorkoutPausedScreen("))
        assertFalse(routeSource.contains("TimedExecutionPausedStateTransition"))
    }

    @Test
    fun executionBottomControlsKeepNavigationBarSafePadding() {
        val routeSource = File(
            "src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt"
        ).readText(Charsets.UTF_8)
        val pausedActionsUseNavigationPadding = Regex(
            pattern = "PausedBottomActionRow\\([\\s\\S]*modifier = Modifier\\s*\\.navigationBarsPadding\\(\\)"
        ).containsMatchIn(routeSource)

        assertTrue(routeSource.contains("useNavigationBarsPadding = true"))
        assertFalse(routeSource.contains("useNavigationBarsPadding = false"))
        assertTrue(pausedActionsUseNavigationPadding)
    }
}
