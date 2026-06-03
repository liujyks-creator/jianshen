package com.liujyks.trainflow.core.health

import com.liujyks.trainflow.core.model.HeartRateAvailability
import com.liujyks.trainflow.core.model.HeartRateState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * E0.2 package boundary for abstract heart-rate state providers.
 *
 * Future implementations may adapt Health Connect, Wear OS, or device SDKs, but
 * those adapters must expose abstract state to training UI and the engine.
 */
internal object HeartRateBoundary

internal interface HeartRateProvider {
    val heartRateState: Flow<HeartRateState>
}

internal class DisabledHeartRateProvider : HeartRateProvider {
    override val heartRateState: Flow<HeartRateState> = MutableStateFlow(
        HeartRateState(availability = HeartRateAvailability.DISABLED)
    )
}

internal class MockHeartRateProvider(
    initialState: HeartRateState = HeartRateState(
        availability = HeartRateAvailability.NOT_CONNECTED,
        message = "演示心率状态"
    )
) : HeartRateProvider {
    private val mutableState = MutableStateFlow(initialState)

    override val heartRateState: StateFlow<HeartRateState> = mutableState

    fun update(state: HeartRateState) {
        mutableState.value = state
    }
}
