package com.liujyks.trainflow.core.health

import com.liujyks.trainflow.core.model.HeartRateFact
import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateTechnicalFailure
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

internal data class HeartRateSourceHint(
    val identifier: String? = null,
    val displayName: String? = null
)

/**
 * Pure runtime facts. Platform BLE objects, exception text, SDK models, and user-facing copy do
 * not cross this boundary.
 */
internal sealed interface HeartRateRuntimeFact {
    data object Disabled : HeartRateRuntimeFact
    data class PermissionRequired(val source: HeartRateSourceHint? = null) : HeartRateRuntimeFact
    data class BluetoothOff(val source: HeartRateSourceHint? = null) : HeartRateRuntimeFact
    data class NotConnected(val source: HeartRateSourceHint? = null) : HeartRateRuntimeFact
    data class Scanning(val source: HeartRateSourceHint? = null) : HeartRateRuntimeFact
    data class Connecting(val source: HeartRateSourceHint? = null) : HeartRateRuntimeFact
    data class WaitingFirstData(val source: HeartRateSourceHint? = null) : HeartRateRuntimeFact
    data class Live(
        val bpm: Int,
        val measuredAt: String,
        val source: HeartRateSourceHint? = null
    ) : HeartRateRuntimeFact

    data class DataInterrupted(val source: HeartRateSourceHint? = null) : HeartRateRuntimeFact
    data class LinkDisconnected(val source: HeartRateSourceHint? = null) : HeartRateRuntimeFact
    data class TechnicalFailure(
        val reason: HeartRateTechnicalFailure,
        val source: HeartRateSourceHint? = null
    ) : HeartRateRuntimeFact

    data class IntentionalStop(val source: HeartRateSourceHint? = null) : HeartRateRuntimeFact
}

internal fun HeartRateRuntimeFact.toHeartRateState(): HeartRateState {
    if (this is HeartRateRuntimeFact.Live && (bpm <= 0 || measuredAt.isBlank())) {
        return HeartRateRuntimeFact.TechnicalFailure(
            reason = HeartRateTechnicalFailure.INVALID_FACT,
            source = source
        ).toHeartRateState()
    }

    val source = when (this) {
        HeartRateRuntimeFact.Disabled -> null
        is HeartRateRuntimeFact.PermissionRequired -> source
        is HeartRateRuntimeFact.BluetoothOff -> source
        is HeartRateRuntimeFact.NotConnected -> source
        is HeartRateRuntimeFact.Scanning -> source
        is HeartRateRuntimeFact.Connecting -> source
        is HeartRateRuntimeFact.WaitingFirstData -> source
        is HeartRateRuntimeFact.Live -> source
        is HeartRateRuntimeFact.DataInterrupted -> source
        is HeartRateRuntimeFact.LinkDisconnected -> source
        is HeartRateRuntimeFact.TechnicalFailure -> source
        is HeartRateRuntimeFact.IntentionalStop -> source
    }
    val publicFact = when (this) {
        HeartRateRuntimeFact.Disabled -> HeartRateFact.DISABLED
        is HeartRateRuntimeFact.PermissionRequired -> HeartRateFact.PERMISSION_REQUIRED
        is HeartRateRuntimeFact.BluetoothOff -> HeartRateFact.BLUETOOTH_OFF
        is HeartRateRuntimeFact.NotConnected -> HeartRateFact.NOT_CONNECTED
        is HeartRateRuntimeFact.Scanning -> HeartRateFact.SCANNING
        is HeartRateRuntimeFact.Connecting -> HeartRateFact.CONNECTING
        is HeartRateRuntimeFact.WaitingFirstData -> HeartRateFact.WAITING_FIRST_DATA
        is HeartRateRuntimeFact.Live -> HeartRateFact.LIVE
        is HeartRateRuntimeFact.DataInterrupted -> HeartRateFact.DATA_INTERRUPTED
        is HeartRateRuntimeFact.LinkDisconnected -> HeartRateFact.LINK_DISCONNECTED
        is HeartRateRuntimeFact.TechnicalFailure -> HeartRateFact.TECHNICAL_FAILURE
        is HeartRateRuntimeFact.IntentionalStop -> HeartRateFact.INTENTIONAL_STOP
    }
    val compatibilityKind = when (publicFact) {
        HeartRateFact.DISABLED,
        HeartRateFact.NOT_CONNECTED,
        HeartRateFact.INTENTIONAL_STOP -> HeartRateStateKind.UNAVAILABLE
        HeartRateFact.PERMISSION_REQUIRED -> HeartRateStateKind.PERMISSION_UNAVAILABLE
        HeartRateFact.BLUETOOTH_OFF,
        HeartRateFact.SCANNING,
        HeartRateFact.CONNECTING,
        HeartRateFact.TECHNICAL_FAILURE -> HeartRateStateKind.PROVIDER_UNAVAILABLE
        HeartRateFact.WAITING_FIRST_DATA -> HeartRateStateKind.DEVICE_CONNECTED_NO_READING
        HeartRateFact.LIVE -> HeartRateStateKind.DEVICE_READING
        HeartRateFact.DATA_INTERRUPTED,
        HeartRateFact.LINK_DISCONNECTED -> HeartRateStateKind.STALE_READING
    }
    val unavailableReason = when (publicFact) {
        HeartRateFact.DISABLED -> HeartRateUnavailableReason.DISABLED_BY_USER
        HeartRateFact.PERMISSION_REQUIRED -> HeartRateUnavailableReason.PERMISSION_REQUIRED
        HeartRateFact.BLUETOOTH_OFF -> HeartRateUnavailableReason.BLUETOOTH_DISABLED
        HeartRateFact.NOT_CONNECTED,
        HeartRateFact.SCANNING,
        HeartRateFact.CONNECTING -> HeartRateUnavailableReason.NOT_CONFIGURED
        HeartRateFact.DATA_INTERRUPTED -> HeartRateUnavailableReason.READ_ERROR
        HeartRateFact.LINK_DISCONNECTED -> HeartRateUnavailableReason.DEVICE_DISCONNECTED
        HeartRateFact.TECHNICAL_FAILURE -> HeartRateUnavailableReason.CONNECTION_FAILED
        HeartRateFact.INTENTIONAL_STOP -> HeartRateUnavailableReason.NO_SOURCE
        HeartRateFact.WAITING_FIRST_DATA,
        HeartRateFact.LIVE -> null
    }

    return HeartRateState(
        kind = compatibilityKind,
        sourceKind = if (
            publicFact == HeartRateFact.DISABLED ||
            publicFact == HeartRateFact.INTENTIONAL_STOP ||
            (publicFact == HeartRateFact.NOT_CONNECTED && source == null)
        ) {
            HeartRateSourceKind.NONE
        } else {
            HeartRateSourceKind.DEVICE
        },
        fact = publicFact,
        bpm = (this as? HeartRateRuntimeFact.Live)?.bpm,
        measuredAt = (this as? HeartRateRuntimeFact.Live)?.measuredAt,
        sourceId = source?.identifier,
        sourceLabel = source?.displayName,
        unavailableReason = unavailableReason,
        technicalFailure = (this as? HeartRateRuntimeFact.TechnicalFailure)?.reason
    )
}

internal fun HeartRateFreshnessDecision.toRuntimeFact(
    source: HeartRateSourceHint? = null
): HeartRateRuntimeFact = when (kind) {
    HeartRateFreshnessKind.WAITING -> HeartRateRuntimeFact.WaitingFirstData(source)
    HeartRateFreshnessKind.LIVE -> HeartRateRuntimeFact.Live(
        bpm = bpm ?: -1,
        measuredAt = measuredAt.orEmpty(),
        source = source
    )
    HeartRateFreshnessKind.DATA_INTERRUPTED -> HeartRateRuntimeFact.DataInterrupted(source)
    HeartRateFreshnessKind.LINK_DISCONNECTED -> HeartRateRuntimeFact.LinkDisconnected(source)
    HeartRateFreshnessKind.TECHNICAL_FAILURE -> HeartRateRuntimeFact.TechnicalFailure(
        reason = reason.toTechnicalFailure(),
        source = source
    )
    HeartRateFreshnessKind.INTENTIONAL_STOP -> HeartRateRuntimeFact.IntentionalStop(source)
}

private fun HeartRateFreshnessReason.toTechnicalFailure(): HeartRateTechnicalFailure = when (this) {
    HeartRateFreshnessReason.CONNECT_FAILED -> HeartRateTechnicalFailure.CONNECT_FAILED
    HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED ->
        HeartRateTechnicalFailure.SERVICE_DISCOVERY_FAILED
    HeartRateFreshnessReason.CCCD_FAILED -> HeartRateTechnicalFailure.CCCD_FAILED
    HeartRateFreshnessReason.PLATFORM_FAILURE -> HeartRateTechnicalFailure.PLATFORM_FAILURE
    HeartRateFreshnessReason.INVALID_MONOTONIC_TIME ->
        HeartRateTechnicalFailure.INVALID_MONOTONIC_TIME
    else -> HeartRateTechnicalFailure.INVALID_FACT
}

internal class DisabledHeartRateProvider : HeartRateProvider {
    override val heartRateState: Flow<HeartRateState> = MutableStateFlow(
        HeartRateRuntimeFact.Disabled.toHeartRateState()
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
