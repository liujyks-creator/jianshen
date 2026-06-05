package com.liujyks.trainflow.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateTest {
    @Test
    fun promotesTimedTrainingAsRecommendedDefaultEntry() {
        val state = buildHomeScreenState()

        assertEquals(HomeEntryId.TIMED_TRAINING, state.primaryEntry.id)
        assertTrue(state.primaryEntry.recommended)
        assertTrue(state.primaryEntry.enabled)
        assertEquals("推荐默认", state.primaryEntry.badge)
        assertEquals("编辑计时计划", state.primaryEntry.status)
    }

    @Test
    fun keepsStrengthFollowAlongAndExerciseLibraryAtTheSameEntryLayer() {
        val state = buildHomeScreenState()
        val peerIds = state.peerEntries.map { it.id }
        val exerciseLibrary = state.peerEntries.first { it.id == HomeEntryId.EXERCISE_LIBRARY }
        val followAlong = state.peerEntries.first { it.id == HomeEntryId.FOLLOW_ALONG }
        val strength = state.peerEntries.first { it.id == HomeEntryId.STRENGTH_TRAINING }

        assertEquals(
            listOf(HomeEntryId.STRENGTH_TRAINING, HomeEntryId.FOLLOW_ALONG, HomeEntryId.EXERCISE_LIBRARY),
            peerIds
        )
        assertTrue(strength.enabled)
        assertEquals("编辑力量计划", strength.status)
        assertTrue(followAlong.enabled)
        assertEquals("雏形体验", followAlong.badge)
        assertEquals("查看跟练入口", followAlong.status)
        assertTrue(followAlong.description.contains("复用计时流程"))
        assertTrue(exerciseLibrary.enabled)
        assertEquals("打开动作库", exerciseLibrary.status)
    }

    @Test
    fun reservesRemainingFutureCapabilitiesWithoutEnablingFakeEntrances() {
        val state = buildHomeScreenState()

        assertEquals(
            listOf(
                HomeEntryId.SESSION_RECORDS,
                HomeEntryId.RECOVERY
            ),
            state.futureEntries.map { it.id }
        )
        assertTrue(state.futureEntries.all { !it.enabled })
    }

    @Test
    fun tileFlowQuickWorkspaceKeepsRealPlansSettingsRemindersAndRecordsDiscoverable() {
        val state = buildHomeScreenState()

        assertEquals(
            listOf(
                HomeEntryId.EXERCISE_LIBRARY,
                HomeEntryId.RECENT_PLAN,
                HomeEntryId.TRAINING_PREFERENCES,
                HomeEntryId.REMINDER_STATUS,
                HomeEntryId.SESSION_RECORDS
            ),
            state.quickEntries.map { it.id }
        )
        assertTrue(state.quickEntries.all { it.enabled })
    }
}
