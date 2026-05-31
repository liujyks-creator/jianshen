package com.liujyks.trainflow.ui.shell.official

import com.liujyks.trainflow.feature.plans.confirmDeletePlan
import com.liujyks.trainflow.feature.plans.copyPlan
import com.liujyks.trainflow.feature.plans.requestDeletePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialShellStateTest {
    @Test
    fun bottomNavigationEntriesExposeEnabledState() {
        val entries = officialShellNavigationEntries(OfficialShellDestination.TRAINING)
            .associateBy { it.destination }

        assertTrue(requireNotNull(entries[OfficialShellDestination.PLANS]).enabled)
        assertFalse(requireNotNull(entries[OfficialShellDestination.RECORDS]).enabled)
        assertTrue(requireNotNull(entries[OfficialShellDestination.TRAINING]).enabled)
        assertTrue(requireNotNull(entries[OfficialShellDestination.EXERCISE_LIBRARY]).enabled)
    }

    @Test
    fun editorsStillSelectTrainingBottomDestination() {
        assertEquals(
            OfficialShellDestination.TRAINING,
            OfficialShellDestination.TIMED_PLAN_EDITOR.selectedBottomDestination()
        )
        assertEquals(
            OfficialShellDestination.TRAINING,
            OfficialShellDestination.STRENGTH_PLAN_EDITOR.selectedBottomDestination()
        )

        val timedEntries = officialShellNavigationEntries(OfficialShellDestination.TIMED_PLAN_EDITOR)
        val strengthEntries = officialShellNavigationEntries(OfficialShellDestination.STRENGTH_PLAN_EDITOR)

        assertTrue(requireNotNull(timedEntries.first { it.destination == OfficialShellDestination.TRAINING }).selected)
        assertTrue(requireNotNull(strengthEntries.first { it.destination == OfficialShellDestination.TRAINING }).selected)
    }

    @Test
    fun timedSessionSelectsTrainingBottomDestination() {
        assertEquals(
            OfficialShellDestination.TRAINING,
            OfficialShellDestination.TIMED_SESSION.selectedBottomDestination()
        )

        val entries = officialShellNavigationEntries(OfficialShellDestination.TIMED_SESSION)

        assertTrue(requireNotNull(entries.first { it.destination == OfficialShellDestination.TRAINING }).selected)
    }

    @Test
    fun strengthSessionSelectsTrainingBottomDestination() {
        assertEquals(
            OfficialShellDestination.TRAINING,
            OfficialShellDestination.STRENGTH_SESSION.selectedBottomDestination()
        )

        val entries = officialShellNavigationEntries(OfficialShellDestination.STRENGTH_SESSION)

        assertTrue(requireNotNull(entries.first { it.destination == OfficialShellDestination.TRAINING }).selected)
    }

    @Test
    fun plansSelectsPlansBottomDestination() {
        assertEquals(
            OfficialShellDestination.PLANS,
            OfficialShellDestination.PLANS.selectedBottomDestination()
        )

        val entries = officialShellNavigationEntries(OfficialShellDestination.PLANS)

        assertTrue(requireNotNull(entries.first { it.destination == OfficialShellDestination.PLANS }).selected)
    }

    @Test
    fun disabledRecordsDestinationDoesNotChangeCurrentShellDestination() {
        val state = OfficialShellState(currentDestination = OfficialShellDestination.TRAINING)
            .selectDestination(OfficialShellDestination.RECORDS)

        assertEquals(OfficialShellDestination.TRAINING, state.currentDestination)
    }

    @Test
    fun timedPlanStartsTimedSessionDestination() {
        val initial = OfficialShellState()
        val timedPlan = initial.planManagementState.plans.first()
        val sessionState = initial.startTimedSession(timedPlan)

        assertEquals(OfficialShellDestination.TIMED_SESSION, sessionState.currentDestination)
        assertEquals(timedPlan.id, sessionState.activeTimedSessionPlan?.id)
    }

    @Test
    fun activeTimedSessionHidesBottomBar() {
        val initial = OfficialShellState()
        val timedPlan = initial.planManagementState.plans.first()
        val sessionState = initial.startTimedSession(timedPlan)

        assertFalse(sessionState.showBottomBar)
    }

    @Test
    fun activeTimedSessionRejectsBottomNavigationSelection() {
        val initial = OfficialShellState()
        val timedPlan = initial.planManagementState.plans.first()
        val sessionState = initial.startTimedSession(timedPlan)
        val afterBottomNavigation = sessionState.selectDestination(OfficialShellDestination.PLANS)

        assertEquals(OfficialShellDestination.TIMED_SESSION, afterBottomNavigation.currentDestination)
        assertEquals(timedPlan.id, afterBottomNavigation.activeTimedSessionPlan?.id)
    }

    @Test
    fun strengthPlanDoesNotStartTimedSessionDestination() {
        val initial = OfficialShellState()
        val strengthPlan = initial.planManagementState.plans[1]
        val sessionState = initial.startTimedSession(strengthPlan)

        assertEquals(initial.currentDestination, sessionState.currentDestination)
        assertEquals(null, sessionState.activeTimedSessionPlan)
    }

    @Test
    fun strengthPlanStartsStrengthSessionDestination() {
        val initial = OfficialShellState()
        val strengthPlan = initial.planManagementState.plans[1]
        val sessionState = initial.startStrengthSession(strengthPlan)

        assertEquals(OfficialShellDestination.STRENGTH_SESSION, sessionState.currentDestination)
        assertEquals(strengthPlan.id, sessionState.activeStrengthSessionPlan?.id)
        assertEquals(null, sessionState.activeTimedSessionPlan)
    }

    @Test
    fun activeStrengthSessionHidesBottomBarAndRejectsBottomNavigationSelection() {
        val initial = OfficialShellState()
        val strengthPlan = initial.planManagementState.plans[1]
        val sessionState = initial.startStrengthSession(strengthPlan)
        val afterBottomNavigation = sessionState.selectDestination(OfficialShellDestination.PLANS)

        assertFalse(sessionState.showBottomBar)
        assertEquals(OfficialShellDestination.STRENGTH_SESSION, afterBottomNavigation.currentDestination)
        assertEquals(strengthPlan.id, afterBottomNavigation.activeStrengthSessionPlan?.id)
    }

    @Test
    fun timedPlanDoesNotStartStrengthSessionDestination() {
        val initial = OfficialShellState()
        val timedPlan = initial.planManagementState.plans.first()
        val sessionState = initial.startStrengthSession(timedPlan)

        assertEquals(initial.currentDestination, sessionState.currentDestination)
        assertEquals(null, sessionState.activeStrengthSessionPlan)
    }

    @Test
    fun finishingTimedSessionReturnsToPlansAndClearsActivePlan() {
        val initial = OfficialShellState()
        val timedPlan = initial.planManagementState.plans.first()
        val finished = initial
            .startTimedSession(timedPlan)
            .finishTimedSession()

        assertEquals(OfficialShellDestination.PLANS, finished.currentDestination)
        assertEquals(null, finished.activeTimedSessionPlan)
    }

    @Test
    fun finishingStrengthSessionReturnsToPlansAndClearsActivePlan() {
        val initial = OfficialShellState()
        val strengthPlan = initial.planManagementState.plans[1]
        val finished = initial
            .startStrengthSession(strengthPlan)
            .finishStrengthSession()

        assertEquals(OfficialShellDestination.PLANS, finished.currentDestination)
        assertEquals(null, finished.activeStrengthSessionPlan)
    }

    @Test
    fun copiedPlanSurvivesLeavingAndReturningToPlansTab() {
        val initial = OfficialShellState().selectDestination(OfficialShellDestination.PLANS)
        val originalPlan = initial.planManagementState.plans.first()
        val withCopiedPlan = initial.withPlanManagementState(
            initial.planManagementState.copyPlan(originalPlan.id)
        )

        val returnedToPlans = withCopiedPlan
            .selectDestination(OfficialShellDestination.TRAINING)
            .selectDestination(OfficialShellDestination.EXERCISE_LIBRARY)
            .selectDestination(OfficialShellDestination.PLANS)

        assertEquals(OfficialShellDestination.PLANS, returnedToPlans.currentDestination)
        assertEquals(3, returnedToPlans.planManagementState.plans.size)
        assertTrue(returnedToPlans.planManagementState.plans.any { it.id.startsWith("${originalPlan.id}-copy") })
    }

    @Test
    fun deletedPlanStaysDeletedAfterLeavingAndReturningToPlansTab() {
        val initial = OfficialShellState().selectDestination(OfficialShellDestination.PLANS)
        val deletedPlanId = initial.planManagementState.plans.first().id
        val withDeletedPlan = initial.withPlanManagementState(
            initial.planManagementState
                .requestDeletePlan(deletedPlanId)
                .confirmDeletePlan()
        )

        val returnedToPlans = withDeletedPlan
            .selectDestination(OfficialShellDestination.TRAINING)
            .selectDestination(OfficialShellDestination.EXERCISE_LIBRARY)
            .selectDestination(OfficialShellDestination.PLANS)

        assertEquals(OfficialShellDestination.PLANS, returnedToPlans.currentDestination)
        assertEquals(1, returnedToPlans.planManagementState.plans.size)
        assertFalse(returnedToPlans.planManagementState.plans.any { it.id == deletedPlanId })
    }
}
