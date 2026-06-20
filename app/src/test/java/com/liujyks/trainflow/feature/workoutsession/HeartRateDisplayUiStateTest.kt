package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.StrengthWorkoutEngine
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateUnavailableReason
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.feature.followalong.buildDefaultFollowAlongScreenState
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateDisplayUiStateTest {
    @Test
    fun mapperCoversSourceAwareStatesWithNeutralCopy() {
        val states = mapOf(
            HeartRateStateKind.UNAVAILABLE to HeartRateState(
                kind = HeartRateStateKind.UNAVAILABLE,
                sourceKind = HeartRateSourceKind.NONE,
                unavailableReason = HeartRateUnavailableReason.NO_SOURCE
            ),
            HeartRateStateKind.DEVICE_CONNECTED_NO_READING to HeartRateState(
                kind = HeartRateStateKind.DEVICE_CONNECTED_NO_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                sourceLabel = "训练手环"
            ),
            HeartRateStateKind.DEVICE_READING to HeartRateState(
                kind = HeartRateStateKind.DEVICE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = 126
            ),
            HeartRateStateKind.MANUAL_READING to HeartRateState(
                kind = HeartRateStateKind.MANUAL_READING,
                sourceKind = HeartRateSourceKind.MANUAL,
                bpm = 126
            ),
            HeartRateStateKind.STALE_READING to HeartRateState(
                kind = HeartRateStateKind.STALE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = 118
            ),
            HeartRateStateKind.PERMISSION_UNAVAILABLE to HeartRateState(
                kind = HeartRateStateKind.PERMISSION_UNAVAILABLE,
                sourceKind = HeartRateSourceKind.DEVICE
            ),
            HeartRateStateKind.PROVIDER_UNAVAILABLE to HeartRateState(
                kind = HeartRateStateKind.PROVIDER_UNAVAILABLE,
                sourceKind = HeartRateSourceKind.NONE
            )
        ).mapValues { (_, state) -> state.toHeartRateDisplayUiState() }

        assertEquals("-- bpm", states.getValue(HeartRateStateKind.UNAVAILABLE).valueText)
        assertEquals("未获取心率", states.getValue(HeartRateStateKind.UNAVAILABLE).statusText)
        assertEquals("-- bpm", states.getValue(HeartRateStateKind.DEVICE_CONNECTED_NO_READING).valueText)
        assertEquals("设备已连接，等待读数", states.getValue(HeartRateStateKind.DEVICE_CONNECTED_NO_READING).statusText)
        assertEquals("126 bpm", states.getValue(HeartRateStateKind.DEVICE_READING).valueText)
        assertEquals("设备数据", states.getValue(HeartRateStateKind.DEVICE_READING).statusText)
        assertEquals("126 bpm", states.getValue(HeartRateStateKind.MANUAL_READING).valueText)
        assertEquals("手动录入", states.getValue(HeartRateStateKind.MANUAL_READING).statusText)
        assertEquals("118 bpm", states.getValue(HeartRateStateKind.STALE_READING).valueText)
        assertEquals("数据已过期 · 设备数据", states.getValue(HeartRateStateKind.STALE_READING).statusText)
        assertEquals("权限不可用", states.getValue(HeartRateStateKind.PERMISSION_UNAVAILABLE).statusText)
        assertEquals("来源不可用", states.getValue(HeartRateStateKind.PROVIDER_UNAVAILABLE).statusText)
        assertTrue(states.getValue(HeartRateStateKind.UNAVAILABLE).boundaryText.contains("未接入真实设备"))
        assertTrue(states.getValue(HeartRateStateKind.UNAVAILABLE).boundaryText.contains("不做医疗告警"))
        assertTrue(states.getValue(HeartRateStateKind.DEVICE_READING).isAvailable)
        assertTrue(states.getValue(HeartRateStateKind.MANUAL_READING).isAvailable)
        assertFalse(states.getValue(HeartRateStateKind.STALE_READING).isAvailable)
    }

    @Test
    fun sourceMetadataAndMessageStayInAuxiliaryText() {
        val uiState = HeartRateState(
            kind = HeartRateStateKind.STALE_READING,
            sourceKind = HeartRateSourceKind.DEVICE,
            bpm = 116,
            measuredAt = "2026-06-03T14:20:00Z",
            recordedAt = "2026-06-03T14:21:00Z",
            sourceId = "mock-provider",
            sourceLabel = "开发模拟源",
            message = "最后一次设备读数"
        ).toHeartRateDisplayUiState()

        assertEquals("116 bpm", uiState.valueText)
        assertEquals("数据已过期 · 开发模拟源", uiState.statusText)
        assertTrue(uiState.auxiliaryText.contains("时间 2026-06-03T14:20:00Z"))
        assertTrue(uiState.auxiliaryText.contains("记录 2026-06-03T14:21:00Z"))
        assertTrue(uiState.auxiliaryText.contains("来源 mock-provider"))
        assertTrue(uiState.auxiliaryText.contains("最后一次设备读数"))
        assertFalse(uiState.isAvailable)
    }

    @Test
    fun sourceAwareStatesDoNotCreateAlarmCopyOrChangeControls() {
        val device = HeartRateState(
            kind = HeartRateStateKind.DEVICE_READING,
            sourceKind = HeartRateSourceKind.DEVICE,
            bpm = 130
        ).toHeartRateDisplayUiState()
        val manual = HeartRateState(
            kind = HeartRateStateKind.MANUAL_READING,
            sourceKind = HeartRateSourceKind.MANUAL,
            bpm = 130
        ).toHeartRateDisplayUiState()
        val forbiddenWords = listOf("危险心率", "异常心率", "过高", "热量", "强度建议")

        assertEquals("设备数据", device.statusText)
        assertEquals("手动录入", manual.statusText)
        forbiddenWords.forEach { word ->
            assertFalse(device.combinedCopy().contains(word))
            assertFalse(manual.combinedCopy().contains(word))
        }
    }

    @Test
    fun timedStrengthAndFollowAlongUseSameHeartRateMapperCopy() {
        val heartRateState = HeartRateState(
            kind = HeartRateStateKind.DEVICE_READING,
            sourceKind = HeartRateSourceKind.DEVICE,
            bpm = 124,
            measuredAt = "2026-06-03T14:30:00Z",
            sourceId = "mock-provider"
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
        val noSource = active.toTimedWorkoutSessionScreenState(
            heartRateState = HeartRateState(
                kind = HeartRateStateKind.UNAVAILABLE,
                sourceKind = HeartRateSourceKind.NONE,
                unavailableReason = HeartRateUnavailableReason.NO_SOURCE
            )
        )
        val providerUnavailable = active.toTimedWorkoutSessionScreenState(
            heartRateState = HeartRateState(
                kind = HeartRateStateKind.PROVIDER_UNAVAILABLE,
                sourceKind = HeartRateSourceKind.NONE,
                message = "当前构建未接入来源"
            )
        )

        assertEquals(noSource.canPause, providerUnavailable.canPause)
        assertEquals(noSource.canResume, providerUnavailable.canResume)
        assertEquals(noSource.canSkip, providerUnavailable.canSkip)
        assertEquals(noSource.canExtendRest, providerUnavailable.canExtendRest)
        assertEquals(noSource.canEnd, providerUnavailable.canEnd)
        assertEquals(noSource.phaseLabel, providerUnavailable.phaseLabel)
    }

    @Test
    fun heartRateCopyAvoidsOutOfScopeTerms() {
        val allCopy = HeartRateStateKind.entries.joinToString(" ") { kind ->
            HeartRateState(
                kind = kind,
                sourceKind = when (kind) {
                    HeartRateStateKind.DEVICE_CONNECTED_NO_READING,
                    HeartRateStateKind.DEVICE_READING,
                    HeartRateStateKind.STALE_READING,
                    HeartRateStateKind.PERMISSION_UNAVAILABLE -> HeartRateSourceKind.DEVICE
                    HeartRateStateKind.MANUAL_READING -> HeartRateSourceKind.MANUAL
                    HeartRateStateKind.UNAVAILABLE,
                    HeartRateStateKind.PROVIDER_UNAVAILABLE -> HeartRateSourceKind.NONE
                },
                bpm = if (kind in currentReadingOrStaleKinds) 120 else null,
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

    private val currentReadingOrStaleKinds = setOf(
        HeartRateStateKind.DEVICE_READING,
        HeartRateStateKind.MANUAL_READING,
        HeartRateStateKind.STALE_READING
    )
}
