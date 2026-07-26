package com.liujyks.trainflow.core.health

internal data class HeartRateRecoveryEligibilityInput(
    val optedIn: Boolean,
    val savedTargetIdentifier: String?,
    val permissionGranted: Boolean,
    val bluetoothEnabled: Boolean,
    val manuallySuppressed: Boolean,
    val appVisible: Boolean,
    val activeTrainingFgsActive: Boolean
)

internal enum class HeartRateRecoveryStopReason {
    OPTED_OUT,
    NO_SAVED_TARGET,
    PERMISSION_UNAVAILABLE,
    BLUETOOTH_OFF,
    MANUAL_SUPPRESSION,
    BACKGROUND_WITHOUT_FGS,
    OWNER_CLOSED
}

internal data class HeartRateRecoveryEligibilityDecision(
    val eligible: Boolean,
    val targetIdentifier: String? = null,
    val stopReason: HeartRateRecoveryStopReason? = null
)

internal enum class HeartRateRecoveryPhase {
    DISARMED,
    WAITING_NEXT_WINDOW,
    SEARCHING,
    WINDOW_MISSED_ARMED,
    CONNECTING_OR_CONNECTED
}

internal data class HeartRateRecoveryState(
    val phase: HeartRateRecoveryPhase,
    val targetIdentifier: String? = null,
    val stopReason: HeartRateRecoveryStopReason? = null
) {
    companion object {
        fun disarmed(reason: HeartRateRecoveryStopReason) = HeartRateRecoveryState(
            phase = HeartRateRecoveryPhase.DISARMED,
            stopReason = reason
        )
    }
}

internal fun evaluateHeartRateRecoveryEligibility(
    input: HeartRateRecoveryEligibilityInput
): HeartRateRecoveryEligibilityDecision {
    val target = input.savedTargetIdentifier?.trim()?.takeIf(String::isNotEmpty)
    val reason = when {
        !input.optedIn -> HeartRateRecoveryStopReason.OPTED_OUT
        input.manuallySuppressed -> HeartRateRecoveryStopReason.MANUAL_SUPPRESSION
        target == null -> HeartRateRecoveryStopReason.NO_SAVED_TARGET
        !input.permissionGranted -> HeartRateRecoveryStopReason.PERMISSION_UNAVAILABLE
        !input.bluetoothEnabled -> HeartRateRecoveryStopReason.BLUETOOTH_OFF
        !input.appVisible && !input.activeTrainingFgsActive ->
            HeartRateRecoveryStopReason.BACKGROUND_WITHOUT_FGS
        else -> null
    }
    return if (reason == null) {
        HeartRateRecoveryEligibilityDecision(
            eligible = true,
            targetIdentifier = target
        )
    } else {
        HeartRateRecoveryEligibilityDecision(
            eligible = false,
            stopReason = reason
        )
    }
}
