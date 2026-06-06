package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.HeartRateAvailability
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateWarningLevel
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.feature.followalong.buildDefaultFollowAlongScreenState
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateDisplayUiStateTest {
    @Test
    fun mapperCoversAllAvailabilityStatesWithNeutralCopy() {
        val states = mapOf(
            HeartRateAvailability.DISABLED to HeartRateState(HeartRateAvailability.DISABLED),
            HeartRateAvailability.NOT_CONNECTED to HeartRateState(HeartRateAvailability.NOT_CONNECTED),
            HeartRateAvailability.CONNECTING to HeartRateState(HeartRateAvailability.CONNECTING),
            HeartRateAvailability.AVAILABLE to HeartRateState(
                availability = HeartRateAvailability.AVAILABLE,
                bpm = 126
            ),
            HeartRateAvailability.STALE to HeartRateState(
                availability = HeartRateAvailability.STALE,
                bpm = 118
            ),
            HeartRateAvailability.ERROR to HeartRateState(HeartRateAvailability.ERROR)
        ).mapValues { (_, state) -> state.toHeartRateDisplayUiState() }

        assertEquals("心率显示已关闭", states.getValue(HeartRateAvailability.DISABLED).statusText)
        assertEquals("未连接设备", states.getValue(HeartRateAvailability.NOT_CONNECTED).statusText)
        assertEquals("等待数据", states.getValue(HeartRateAvailability.CONNECTING).statusText)
        assertEquals("126 bpm", states.getValue(HeartRateAvailability.AVAILABLE).valueText)
        assertEquals("演示心率状态", states.getValue(HeartRateAvailability.AVAILABLE).statusText)
        assertEquals("118 bpm", states.getValue(HeartRateAvailability.STALE).valueText)
        assertEquals("数据暂时中断", states.getValue(HeartRateAvailability.STALE).statusText)
        assertEquals("心率状态暂不可用", states.getValue(HeartRateAvailability.ERROR).statusText)
        assertTrue(states.getValue(HeartRateAvailability.NOT_CONNECTED).boundaryText.contains("未接入真实设备"))
        assertTrue(states.getValue(HeartRateAvailability.NOT_CONNECTED).boundaryText.contains("不做医疗告警"))
        assertTrue(states.getValue(HeartRateAvailability.AVAILABLE).isAvailable)
        assertFalse(states.getValue(HeartRateAvailability.STALE).isAvailable)
    }

    @Test
    fun measuredAtSourceIdAndMessageStayInAuxiliaryText() {
        val uiState = HeartRateState(
            availability = HeartRateAvailability.STALE,
            bpm = 116,
            measuredAt = "2026-06-03T14:20:00Z",
            sourceId = "mock-provider",
            message = "数据暂时中断"
        ).toHeartRateDisplayUiState()

        assertEquals("116 bpm", uiState.valueText)
        assertEquals("数据暂时中断", uiState.statusText)
        assertTrue(uiState.auxiliaryText.contains("时间 2026-06-03T14:20:00Z"))
        assertTrue(uiState.auxiliaryText.contains("来源 mock-provider"))
        assertTrue(uiState.auxiliaryText.contains("数据暂时中断"))
        assertFalse(uiState.isAvailable)
    }

    @Test
    fun warningLevelDoesNotCreateAlarmCopyOrChangeAvailability() {
        val attention = HeartRateState(
            availability = HeartRateAvailability.AVAILABLE,
            bpm = 130,
            warningLevel = HeartRateWarningLevel.ATTENTION
        ).toHeartRateDisplayUiState()
        val high = HeartRateState(
            availability = HeartRateAvailability.AVAILABLE,
            bpm = 130,
            warningLevel = HeartRateWarningLevel.HIGH
        ).toHeartRateDisplayUiState()
        val forbiddenWords = listOf("危险心率", "异常心率", "过高", "热量", "强度建议")

        assertEquals("演示心率状态", attention.statusText)
        assertEquals(attention, high)
        forbiddenWords.forEach { word ->
            assertFalse(attention.combinedCopy().contains(word))
            assertFalse(high.combinedCopy().contains(word))
        }
    }

    @Test
    fun timedStrengthAndFollowAlongUseSameHeartRateMapperCopy() {
        val heartRateState = HeartRateState(
            availability = HeartRateAvailability.AVAILABLE,
            bpm = 124,
            measuredAt = "2026-06-03T14:30:00Z",
            sourceId = "mock-provider",
            message = "演示心率状态"
        )
        val expected = heartRateState.toHeartRateDisplayUiState()
        val planState = buildDefaultPlanManagementState()
        val timed = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(planState.plans.first()),
            WorkoutCommand.StartSession
        ).state.toTimedWorkoutSessionScreenState(heartRateState = heartRateState)
        val strength = StrengthWorkoutEngine.dispatch(
            StrengthWorkoutEngine.create(planState.plans[1]),
            WorkoutCommand.StartSession
        ).state.toStrengthWorkoutSessionScreenState(heartRateState = heartRateState)
        val followAlongPlan = buildDefaultFollowAlongScreenState().plans.single().plan
        val followAlong = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(followAlongPlan),
            WorkoutCommand.StartSession
        ).state.toFollowAlongWorkoutSessionUiState(heartRateState = heartRateState)

        assertEquals(expected, timed.heartRate)
        assertEquals(expected, strength.heartRate)
        assertEquals(expected, followAlong.heartRate)
    }

    @Test
    fun heartRateStateDoesNotChangeTrainingControls() {
        val timedPlan = buildDefaultPlanManagementState().plans.first()
        val active = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timedPlan),
            WorkoutCommand.StartSession
        ).state
        val notConnected = active.toTimedWorkoutSessionScreenState(
            heartRateState = HeartRateState(HeartRateAvailability.NOT_CONNECTED)
        )
        val error = active.toTimedWorkoutSessionScreenState(
            heartRateState = HeartRateState(
                availability = HeartRateAvailability.ERROR,
                warningLevel = HeartRateWarningLevel.HIGH,
                message = "心率状态暂不可用"
            )
        )

        assertEquals(notConnected.canPause, error.canPause)
        assertEquals(notConnected.canResume, error.canResume)
        assertEquals(notConnected.canSkip, error.canSkip)
        assertEquals(notConnected.canExtendRest, error.canExtendRest)
        assertEquals(notConnected.canEnd, error.canEnd)
        assertEquals(notConnected.phaseLabel, error.phaseLabel)
    }

    @Test
    fun heartRateCopyAvoidsOutOfScopeTerms() {
        val allCopy = HeartRateAvailability.entries.joinToString(" ") { availability ->
            HeartRateState(
                availability = availability,
                bpm = if (availability == HeartRateAvailability.AVAILABLE) 120 else null,
                message = null
            ).toHeartRateDisplayUiState().combinedCopy()
        }
        val forbiddenWords = listOf("医疗", "危险", "异常", "过高", "告警", "热量", "强度判断")

        forbiddenWords.forEach { word ->
            assertFalse(allCopy.contains(word))
        }
    }

    private fun HeartRateDisplayUiState.combinedCopy(): String {
        return listOf(valueText, statusText, auxiliaryText).joinToString(" ")
    }
}
