package com.liujyks.trainflow.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessVisibilityTrackerTest {
    @Test
    fun firstStartedActivityMakesProcessVisibleAndFinalStopMakesItBackground() {
        val facts = mutableListOf<ProcessVisibilityFact>()
        val reducer = ProcessVisibilityReducer(facts::add)
        val activity = Any()

        reducer.activityStarted(activity)
        reducer.activityStopped(activity, changingConfigurations = false)

        assertEquals(
            listOf(ProcessVisibilityFact.VISIBLE, ProcessVisibilityFact.BACKGROUND),
            facts
        )
    }

    @Test
    fun overlappingActivitiesRemainVisibleUntilTheLastActivityStops() {
        val facts = mutableListOf<ProcessVisibilityFact>()
        val reducer = ProcessVisibilityReducer(facts::add)
        val first = Any()
        val second = Any()

        reducer.activityStarted(first)
        reducer.activityStarted(second)
        reducer.activityStopped(first, changingConfigurations = false)
        reducer.activityStopped(second, changingConfigurations = false)

        assertEquals(
            listOf(ProcessVisibilityFact.VISIBLE, ProcessVisibilityFact.BACKGROUND),
            facts
        )
    }

    @Test
    fun configurationReplacementDoesNotEmitBackgroundOrUnknown() {
        val facts = mutableListOf<ProcessVisibilityFact>()
        val reducer = ProcessVisibilityReducer(facts::add)
        val oldActivity = Any()
        val replacement = Any()

        reducer.activityStarted(oldActivity)
        val generation = reducer.activityStopped(
            oldActivity,
            changingConfigurations = true
        )
        reducer.activityStarted(replacement)
        reducer.configurationTimeout(requireNotNull(generation))

        assertEquals(
            listOf(
                ProcessVisibilityFact.VISIBLE,
                ProcessVisibilityFact.CONFIGURATION_TRANSITION,
                ProcessVisibilityFact.VISIBLE
            ),
            facts
        )
    }

    @Test
    fun unresolvedConfigurationTransitionFailsClosedToUnknown() {
        val facts = mutableListOf<ProcessVisibilityFact>()
        val reducer = ProcessVisibilityReducer(facts::add)
        val activity = Any()

        reducer.activityStarted(activity)
        val generation = reducer.activityStopped(
            activity,
            changingConfigurations = true
        )
        reducer.configurationTimeout(requireNotNull(generation))

        assertEquals(ProcessVisibilityFact.UNKNOWN, facts.last())
    }

    @Test
    fun duplicateStartFailsClosedToUnknown() {
        val facts = mutableListOf<ProcessVisibilityFact>()
        val reducer = ProcessVisibilityReducer(facts::add)
        val activity = Any()

        reducer.activityStarted(activity)
        reducer.activityStarted(activity)

        assertEquals(
            listOf(ProcessVisibilityFact.VISIBLE, ProcessVisibilityFact.UNKNOWN),
            facts
        )
    }

    @Test
    fun unmatchedStopFailsClosedToUnknown() {
        val facts = mutableListOf<ProcessVisibilityFact>()
        val reducer = ProcessVisibilityReducer(facts::add)

        reducer.activityStarted(Any())
        reducer.activityStopped(Any(), changingConfigurations = false)

        assertEquals(ProcessVisibilityFact.UNKNOWN, facts.last())
    }
}
