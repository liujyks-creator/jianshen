package com.liujyks.trainflow.core.health

import com.liujyks.trainflow.core.model.HeartRateAvailability
import com.liujyks.trainflow.core.model.HeartRateState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HeartRateProviderBoundaryTest {
    @Test
    fun disabledProviderOutputsDisabledAbstractState() = runBlocking {
        val provider = DisabledHeartRateProvider()

        assertEquals(
            HeartRateState(availability = HeartRateAvailability.DISABLED),
            provider.heartRateState.first()
        )
    }

    @Test
    fun mockProviderCanEmitFixtureStatesWithoutPlatformSdk() = runBlocking {
        val provider = MockHeartRateProvider()
        val availableState = HeartRateState(
            availability = HeartRateAvailability.AVAILABLE,
            bpm = 122,
            measuredAt = "2026-06-03T14:40:00Z",
            sourceId = "mock-provider",
            message = "演示心率状态"
        )

        provider.update(availableState)

        assertEquals(availableState, provider.heartRateState.first())
    }
}
