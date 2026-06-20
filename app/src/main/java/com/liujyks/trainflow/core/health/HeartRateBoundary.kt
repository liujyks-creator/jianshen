package com.liujyks.trainflow.core.health

import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateUnavailableReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * E0.2 package boundary for abstract heart-rate state providers.
 *
 * Future implementations may adapt platform or device APIs, but those adapters
 * must expose TrainFlow source-aware state to training UI and the engine.
 */
internal object HeartRateBoundary

internal interface HeartRateProvider {
    val heartRateState: Flow<HeartRateState>
}

internal class DisabledHeartRateProvider : HeartRateProvider {
    override val heartRateState: Flow<HeartRateState> = MutableStateFlow(
        HeartRateState(
            kind = HeartRateStateKind.UNAVAILABLE,
            sourceKind = HeartRateSourceKind.NONE,
            unavailableReason = HeartRateUnavailableReason.NO_SOURCE
        )
    )
}

internal class MockHeartRateProvider(
    initialState: HeartRateState = HeartRateState(
        kind = HeartRateStateKind.PROVIDER_UNAVAILABLE,
        sourceKind = HeartRateSourceKind.NONE,
        unavailableReason = HeartRateUnavailableReason.NOT_CONFIGURED,
        message = "演示来源不可用"
    )
) : HeartRateProvider {
    private val mutableState = MutableStateFlow(initialState)

    override val heartRateState: StateFlow<HeartRateState> = mutableState

    fun update(state: HeartRateState) {
        mutableState.value = state
    }
}
