package com.liujyks.trainflow.feature.followalong

import com.liujyks.trainflow.core.data.fixture.FirstActionExerciseFixtures
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowAlongUiStateTest {
    @Test
    fun defaultStateProvidesBasicFollowAlongPreset() {
        val state = buildDefaultFollowAlongScreenState()
        val plan = state.plans.single()

        assertFalse(state.isEmpty)
        assertEquals(WorkoutMode.FOLLOW_ALONG, plan.plan.mode)
        assertEquals(true, plan.plan.followAlong?.preset)
        assertEquals("基础跟练 / 雏形体验", plan.badge)
        assertTrue(plan.summary.contains("4 个动作"))
        assertTrue(plan.summary.contains("预计"))
        assertFalse(plan.canStartFollowAlong)
        assertEquals("跟练执行页 E6.2 接入", plan.nextStepStatus)
    }

    @Test
    fun presetOnlyUsesExercisesThatSupportFollowAlong() {
        val state = buildDefaultFollowAlongScreenState()
        val exerciseIds = state.plans.single().plan.followAlongActionExerciseIds()
        val fixtureById = FirstActionExerciseFixtures.entries.associateBy { it.exercise.id }

        assertTrue(exerciseIds.isNotEmpty())
        assertTrue(
            exerciseIds.all { id ->
                val entry = requireNotNull(fixtureById[id])
                entry.exercise.capabilities.supportsFollowAlong &&
                    entry.exercise.capabilities.supportsTimedTraining
            }
        )
    }

    @Test
    fun presetStoresTimedCircuitStructureAndActionCueRows() {
        val state = buildDefaultFollowAlongScreenState()
        val plan = state.plans.single()
        val block = plan.plan.blocks.filterIsInstance<TimedCircuitBlock>().single()

        assertEquals(1, block.rounds)
        assertEquals(4, block.items.size)
        assertEquals(4, plan.actionRows.size)
        assertTrue(plan.actionRows.all { it.contains("秒") })
    }

    @Test
    fun copyMakesCurrentBoundaryClearWithoutFakeCoursePlatform() {
        val state = buildDefaultFollowAlongScreenState()
        val plan = state.plans.single()
        val boundaryCopy = (listOf(state.summary, plan.mediaStatus) + plan.boundaryRows).joinToString(" ")

        assertTrue(boundaryCopy.contains("基础跟练"))
        assertTrue(boundaryCopy.contains("雏形体验"))
        assertTrue(boundaryCopy.contains("首批 fixture"))
        assertTrue(boundaryCopy.contains("不提供完整课程平台"))
        assertTrue(boundaryCopy.contains("不提供完整课程平台、教练视频库、AI 纠错、音乐编排或语音教练"))
    }

    @Test
    fun emptyStateIsHonestWhenNoFollowAlongFixturesExist() {
        val state = buildDefaultFollowAlongScreenState(entries = emptyList())

        assertTrue(state.isEmpty)
        assertTrue(state.plans.isEmpty())
        assertEquals("暂无可跟练内容", state.emptyStateTitle)
        assertTrue(state.emptyStateDescription.contains("不展示虚假的跟练入口"))
    }
}
