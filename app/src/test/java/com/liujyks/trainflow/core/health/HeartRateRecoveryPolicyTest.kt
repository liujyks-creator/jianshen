package com.liujyks.trainflow.core.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateRecoveryPolicyTest {
    @Test
    fun exactSavedTargetAndForegroundFactsArmAndEnableRecovery() {
        val decision = HeartRateRecoveryPolicy.evaluate(eligibleInputs())

        assertTrue(decision.intentArmed)
        assertTrue(decision.eligible)
        assertEquals("D8:F0:42:01:90:D7", decision.exactTargetIdentifier)
        assertNull(decision.blockedReason)
    }

    @Test
    fun trainingFgsCanReplaceVisibilityButNeitherFactCanBeMissing() {
        val trainingDecision = HeartRateRecoveryPolicy.evaluate(
            eligibleInputs().copy(appVisible = false, legalTrainingFgs = true)
        )
        val backgroundDecision = HeartRateRecoveryPolicy.evaluate(
            eligibleInputs().copy(appVisible = false, legalTrainingFgs = false)
        )

        assertTrue(trainingDecision.intentArmed)
        assertTrue(trainingDecision.eligible)
        assertTrue(backgroundDecision.intentArmed)
        assertFalse(backgroundDecision.eligible)
        assertEquals(
            HeartRateRecoveryBlockedReason.NOT_VISIBLE_OR_TRAINING_FGS,
            backgroundDecision.blockedReason
        )
    }

    @Test
    fun optOutMissingExactTargetAndSuppressionDisarmIntent() {
        val cases = listOf(
            eligibleInputs().copy(optIn = false) to HeartRateRecoveryBlockedReason.OPTED_OUT,
            eligibleInputs().copy(savedTargetIdentifier = null) to
                HeartRateRecoveryBlockedReason.SAVED_TARGET_MISSING,
            eligibleInputs().copy(savedTargetIdentifier = "  ") to
                HeartRateRecoveryBlockedReason.SAVED_TARGET_MISSING,
            eligibleInputs().copy(manualDisconnectSuppressed = true) to
                HeartRateRecoveryBlockedReason.MANUAL_DISCONNECT_SUPPRESSED
        )

        cases.forEach { (inputs, reason) ->
            val decision = HeartRateRecoveryPolicy.evaluate(inputs)
            assertFalse(reason.name, decision.intentArmed)
            assertFalse(reason.name, decision.eligible)
            assertEquals(reason, decision.blockedReason)
        }
    }

    @Test
    fun permissionAndBluetoothLossKeepIntentArmedButBlockOperations() {
        val permission = HeartRateRecoveryPolicy.evaluate(
            eligibleInputs().copy(permissionGranted = false)
        )
        val bluetooth = HeartRateRecoveryPolicy.evaluate(
            eligibleInputs().copy(bluetoothEnabled = false)
        )

        assertTrue(permission.intentArmed)
        assertFalse(permission.eligible)
        assertEquals(HeartRateRecoveryBlockedReason.PERMISSION_REQUIRED, permission.blockedReason)
        assertTrue(bluetooth.intentArmed)
        assertFalse(bluetooth.eligible)
        assertEquals(HeartRateRecoveryBlockedReason.BLUETOOTH_OFF, bluetooth.blockedReason)
    }

    private fun eligibleInputs() = HeartRateRecoveryInputs(
        optIn = true,
        savedTargetIdentifier = "D8:F0:42:01:90:D7",
        permissionGranted = true,
        bluetoothEnabled = true,
        manualDisconnectSuppressed = false,
        appVisible = true,
        legalTrainingFgs = false
    )
}
