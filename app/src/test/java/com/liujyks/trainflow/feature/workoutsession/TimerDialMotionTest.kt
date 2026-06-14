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
}
