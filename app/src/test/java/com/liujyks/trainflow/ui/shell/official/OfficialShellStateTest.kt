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
