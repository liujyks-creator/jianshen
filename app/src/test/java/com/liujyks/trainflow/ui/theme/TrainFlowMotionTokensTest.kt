package com.liujyks.trainflow.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainFlowMotionTokensTest {
    @Test
    fun motionDurationsStayInsideTrainingInteractionRanges() {
        assertTrue(TrainFlowMotionTokens.TouchFeedbackDurationMillis in 80..120)
        assertTrue(TrainFlowMotionTokens.StateTransitionDurationMillis in 120..180)
        assertTrue(TrainFlowMotionTokens.LocalLayoutTransitionDurationMillis in 180..240)
        assertTrue(TrainFlowMotionTokens.PageTransitionDurationMillis in 220..300)
        assertEquals(1_000L, TrainFlowMotionTokens.ContinuousProjectionMaxDurationMillis)
    }

    @Test
    fun categoryNamesKeepUsesDistinct() {
        val categories = TrainFlowMotionTokens.Categories

        assertEquals(
            listOf(
                "touch_feedback",
                "state_transition",
                "local_layout_transition",
                "page_transition"
            ),
            categories.map { category -> category.name }
        )
        assertEquals(categories.size, categories.map { category -> category.name }.toSet().size)
        categories.forEach { category ->
            assertTrue("${category.name} should describe its intended use", category.intendedUse.length >= 24)
        }
        assertTrue(
            TrainFlowMotionTokens.TouchFeedbackDurationMillis <
                TrainFlowMotionTokens.StateTransitionDurationMillis
        )
        assertTrue(
            TrainFlowMotionTokens.StateTransitionDurationMillis <
                TrainFlowMotionTokens.LocalLayoutTransitionDurationMillis
        )
        assertTrue(
            TrainFlowMotionTokens.LocalLayoutTransitionDurationMillis <
                TrainFlowMotionTokens.PageTransitionDurationMillis
        )
    }

    @Test
    fun reduceMotionPolicyHasExplicitFallbacks() {
        val policy = TrainFlowMotionTokens.ReduceMotionPolicy

        assertEquals(0, policy.fallbackDurationMillis)
        assertTrue(policy.snapStateTransitions)
        assertTrue(policy.disableNonEssentialMotion)
        assertTrue(policy.disableContinuousProjection)
    }

    @Test
    fun tokenValuesAvoidMagicMotionNumbers() {
        assertTrue(TrainFlowMotionTokens.TouchFeedbackScale in 0.94f..0.99f)
        assertTrue(TrainFlowMotionTokens.PressedAlpha in 0.70f..0.95f)
        assertTrue(TrainFlowMotionTokens.DisabledMotionAlpha in 0.60f..0.85f)
        assertFalse(TrainFlowMotionTokens.StandardEasing === TrainFlowMotionTokens.ContinuousProgressEasing)
    }
}
