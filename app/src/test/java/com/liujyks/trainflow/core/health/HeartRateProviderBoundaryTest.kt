package com.liujyks.trainflow.core.health

import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateUnavailableReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HeartRateProviderBoundaryTest {
    @Test
    fun disabledProviderOutputsNoSourceAbstractState() = runBlocking {
        val provider = DisabledHeartRateProvider()

        assertEquals(
            HeartRateState(
                kind = HeartRateStateKind.UNAVAILABLE,
                sourceKind = HeartRateSourceKind.NONE,
                unavailableReason = HeartRateUnavailableReason.NO_SOURCE
            ),
            provider.heartRateState.first()
        )
    }

    @Test
    fun mockProviderCanEmitSourceAwareFixtureStatesWithoutPlatformSdk() = runBlocking {
        val provider = MockHeartRateProvider()
        val availableState = HeartRateState(
            kind = HeartRateStateKind.DEVICE_READING,
            sourceKind = HeartRateSourceKind.DEVICE,
            bpm = 122,
            measuredAt = "2026-06-03T14:40:00Z",
            sourceId = "mock-provider",
            sourceLabel = "开发模拟源"
        )

        provider.update(availableState)

        assertEquals(availableState, provider.heartRateState.first())
    }

    @Test
    fun mockProviderCanRepresentUnavailablePermissionManualAndStaleStates() = runBlocking {
        val provider = MockHeartRateProvider()
        val states = listOf(
            HeartRateState(
                kind = HeartRateStateKind.DEVICE_CONNECTED_NO_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                sourceLabel = "开发模拟源"
            ),
            HeartRateState(
                kind = HeartRateStateKind.MANUAL_READING,
                sourceKind = HeartRateSourceKind.MANUAL,
                bpm = 126,
                recordedAt = "2026-06-03T14:42:00Z"
            ),
            HeartRateState(
                kind = HeartRateStateKind.STALE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = 118,
                measuredAt = "2026-06-03T14:40:00Z",
                sourceId = "mock-provider"
            ),
            HeartRateState(
                kind = HeartRateStateKind.PERMISSION_UNAVAILABLE,
                sourceKind = HeartRateSourceKind.DEVICE,
                sourceId = "future-device-adapter"
            ),
            HeartRateState(
                kind = HeartRateStateKind.PROVIDER_UNAVAILABLE,
                sourceKind = HeartRateSourceKind.NONE,
                unavailableReason = HeartRateUnavailableReason.NOT_CONFIGURED
            )
        )

        states.forEach { state ->
            provider.update(state)

            assertEquals(state, provider.heartRateState.first())
        }
    }
}
