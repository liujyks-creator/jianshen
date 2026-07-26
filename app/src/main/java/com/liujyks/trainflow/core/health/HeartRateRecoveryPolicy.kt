package com.liujyks.trainflow.core.health

internal data class HeartRateRecoveryInputs(
    val optIn: Boolean,
    val savedTargetIdentifier: String?,
    val permissionGranted: Boolean,
    val bluetoothEnabled: Boolean,
    val manualDisconnectSuppressed: Boolean,
    val appVisible: Boolean,
    val legalTrainingFgs: Boolean
)

internal data class HeartRateRecoveryDecision(
    val intentArmed: Boolean,
    val eligible: Boolean,
    val exactTargetIdentifier: String?,
    val blockedReason: HeartRateRecoveryBlockedReason?
)

internal enum class HeartRateRecoveryBlockedReason {
    OPTED_OUT,
    SAVED_TARGET_MISSING,
    MANUAL_DISCONNECT_SUPPRESSED,
    PERMISSION_REQUIRED,
    BLUETOOTH_OFF,
    NOT_VISIBLE_OR_TRAINING_FGS
}

internal object HeartRateRecoveryPolicy {
    fun evaluate(inputs: HeartRateRecoveryInputs): HeartRateRecoveryDecision {
        val target = inputs.savedTargetIdentifier?.takeIf(String::isNotBlank)
        val disarmingReason = when {
            !inputs.optIn -> HeartRateRecoveryBlockedReason.OPTED_OUT
            target == null -> HeartRateRecoveryBlockedReason.SAVED_TARGET_MISSING
            inputs.manualDisconnectSuppressed ->
                HeartRateRecoveryBlockedReason.MANUAL_DISCONNECT_SUPPRESSED
            else -> null
        }
        if (disarmingReason != null) {
            return HeartRateRecoveryDecision(
                intentArmed = false,
                eligible = false,
                exactTargetIdentifier = target,
                blockedReason = disarmingReason
            )
        }

        val blockingReason = when {
            !inputs.permissionGranted -> HeartRateRecoveryBlockedReason.PERMISSION_REQUIRED
            !inputs.bluetoothEnabled -> HeartRateRecoveryBlockedReason.BLUETOOTH_OFF
            !inputs.appVisible && !inputs.legalTrainingFgs ->
                HeartRateRecoveryBlockedReason.NOT_VISIBLE_OR_TRAINING_FGS
            else -> null
        }
        return HeartRateRecoveryDecision(
            intentArmed = true,
            eligible = blockingReason == null,
            exactTargetIdentifier = target,
            blockedReason = blockingReason
        )
    }
}

internal enum class HeartRateRecoveryFact {
    BLOCKED,
    ARMED_WAITING,
    AUTO_SEARCHING,
    WINDOW_NO_MATCH_ARMED,
    CONNECTING,
    CONNECTED
}

internal data class HeartRateRecoveryState(
    val fact: HeartRateRecoveryFact,
    val exactTargetIdentifier: String? = null,
    val blockedReason: HeartRateRecoveryBlockedReason? = null
) {
    companion object {
        val Initial = HeartRateRecoveryState(
            fact = HeartRateRecoveryFact.BLOCKED,
            blockedReason = HeartRateRecoveryBlockedReason.OPTED_OUT
        )
    }
}
