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
    fun keepsStrengthTrainingAndExerciseLibraryAtTheSameEntryLayer() {
        val state = buildHomeScreenState()
        val peerIds = state.peerEntries.map { it.id }
        val exerciseLibrary = state.peerEntries.first { it.id == HomeEntryId.EXERCISE_LIBRARY }
        val strength = state.peerEntries.first { it.id == HomeEntryId.STRENGTH_TRAINING }

        assertEquals(
            listOf(HomeEntryId.STRENGTH_TRAINING, HomeEntryId.EXERCISE_LIBRARY),
            peerIds
        )
        assertTrue(strength.enabled)
        assertEquals("编辑力量计划", strength.status)
        assertTrue(exerciseLibrary.enabled)
        assertEquals("打开动作库", exerciseLibrary.status)
    }

    @Test
    fun reservesFutureCapabilitiesWithoutEnablingFakeEntrances() {
        val state = buildHomeScreenState()

        assertEquals(
            listOf(
                HomeEntryId.FOLLOW_ALONG,
                HomeEntryId.SESSION_RECORDS,
                HomeEntryId.RECOVERY
            ),
            state.futureEntries.map { it.id }
        )
        assertTrue(state.futureEntries.all { !it.enabled })
        assertTrue(state.futureEntries.first().description.contains("不做课程平台"))
    }
}
