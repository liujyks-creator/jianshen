package com.liujyks.trainflow.app

import android.app.Activity
import android.os.Handler
import android.os.Looper
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class ProcessVisibilityTrackerTest {
    @Test
    fun coldStartAndFirstActivityStartAreReducedOnMainQueue() {
        val harness = Harness()
        val activity = activity()

        assertEquals(ProcessVisibilityFact.Unknown, harness.tracker.currentFact)
        harness.tracker.onActivityStarted(activity)
        assertEquals(ProcessVisibilityFact.Unknown, harness.tracker.currentFact)

        harness.idle()

        assertEquals(ProcessVisibilityFact.ForegroundConfirmed, harness.tracker.currentFact)
        assertEquals(listOf(ProcessVisibilityFact.ForegroundConfirmed), harness.facts)
    }

    @Test
    fun pauseWhileStartedRemainsForegroundAndLastOrdinaryStopIsBackground() {
        val harness = Harness()
        val activity = activity()
        harness.start(activity)
        harness.tracker.onActivityResumed(activity)
        harness.tracker.onActivityPaused(activity)
        harness.idle()

        assertEquals(ProcessVisibilityFact.ForegroundConfirmed, harness.tracker.currentFact)

        harness.tracker.onActivityStopped(activity)
        harness.idle()

        assertEquals(ProcessVisibilityFact.BackgroundConfirmed, harness.tracker.currentFact)
    }

    @Test
    fun overlappingActivitiesRemainForegroundUntilTheLastOneStops() {
        val harness = Harness()
        val first = activity()
        val second = activity()
        harness.start(first)
        harness.start(second)

        harness.tracker.onActivityStopped(first)
        harness.idle()
        assertEquals(ProcessVisibilityFact.ForegroundConfirmed, harness.tracker.currentFact)

        harness.tracker.onActivityStopped(second)
        harness.idle()
        assertEquals(ProcessVisibilityFact.BackgroundConfirmed, harness.tracker.currentFact)
    }

    @Test
    fun configurationStopAndMatchingReplacementDoNotPublishBackgroundOrUnknown() {
        val harness = Harness()
        val oldActivity = activity()
        val replacement = activity()
        harness.start(oldActivity)

        harness.tracker.onActivityStopped(oldActivity, changingConfigurations = true)
        harness.idle()
        val transition = harness.tracker.currentFact
        assertTrue(transition is ProcessVisibilityFact.ConfigurationTransition)

        harness.tracker.onActivityStarted(replacement)
        harness.idle()

        assertEquals(ProcessVisibilityFact.ForegroundConfirmed, harness.tracker.currentFact)
        assertTrue(ProcessVisibilityFact.BackgroundConfirmed !in harness.facts)
        assertEquals(1, harness.facts.filterIsInstance<ProcessVisibilityFact.ConfigurationTransition>().size)
    }

    @Test
    fun missingConfigurationReplacementTimesOutToUnknown() {
        val harness = Harness(timeoutMillis = 100)
        val oldActivity = activity()
        harness.start(oldActivity)
        harness.tracker.onActivityStopped(oldActivity, changingConfigurations = true)
        harness.idle()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))

        assertEquals(ProcessVisibilityFact.Unknown, harness.tracker.currentFact)
    }

    @Test
    fun replacementAfterExpiredGenerationFailsClosedUnknown() {
        val harness = Harness(timeoutMillis = 100)
        val oldActivity = activity()
        val replacement = activity()
        harness.start(oldActivity)
        harness.tracker.onActivityStopped(oldActivity, changingConfigurations = true)
        harness.idle()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        harness.tracker.onActivityStarted(replacement)
        harness.idle()

        assertEquals(ProcessVisibilityFact.Unknown, harness.tracker.currentFact)
    }

    @Test
    fun duplicateAndUnbalancedCallbacksFailClosedUnknown() {
        val duplicateStartHarness = Harness()
        val activity = activity()
        duplicateStartHarness.start(activity)
        duplicateStartHarness.tracker.onActivityStarted(activity)
        duplicateStartHarness.idle()
        assertEquals(ProcessVisibilityFact.Unknown, duplicateStartHarness.tracker.currentFact)

        val unbalancedStopHarness = Harness()
        unbalancedStopHarness.tracker.onActivityStopped(activity())
        unbalancedStopHarness.idle()
        assertEquals(ProcessVisibilityFact.Unknown, unbalancedStopHarness.tracker.currentFact)
    }

    @Test
    fun stopWhileStillResumedFailsClosedUnknown() {
        val harness = Harness()
        val activity = activity()
        harness.start(activity)
        harness.tracker.onActivityResumed(activity)
        harness.idle()

        harness.tracker.onActivityStopped(activity)
        harness.idle()

        assertEquals(ProcessVisibilityFact.Unknown, harness.tracker.currentFact)
    }

    private fun activity(): Activity = Robolectric.buildActivity(Activity::class.java).get()

    private class Harness(timeoutMillis: Long = 1_000L) {
        val facts = mutableListOf<ProcessVisibilityFact>()
        val tracker = ProcessVisibilityTracker(
            mainHandler = Handler(Looper.getMainLooper()),
            configurationTransitionTimeoutMillis = timeoutMillis,
            onFact = facts::add
        )

        fun start(activity: Activity) {
            tracker.onActivityStarted(activity)
            idle()
        }

        fun idle() {
            shadowOf(Looper.getMainLooper()).idle()
        }
    }
}
