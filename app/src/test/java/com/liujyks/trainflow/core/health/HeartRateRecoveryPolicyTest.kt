package com.liujyks.trainflow.core.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateRecoveryPolicyTest {
    @Test
    fun eligibilityRequiresEverySavedTargetAndRuntimeCondition() {
        val baseline = eligibleInput()

        assertTrue(evaluateHeartRateRecoveryEligibility(baseline).eligible)

        val ineligible = listOf(
            baseline.copy(optedIn = false) to HeartRateRecoveryStopReason.OPTED_OUT,
            baseline.copy(savedTargetIdentifier = null) to
                HeartRateRecoveryStopReason.NO_SAVED_TARGET,
            baseline.copy(permissionGranted = false) to
                HeartRateRecoveryStopReason.PERMISSION_UNAVAILABLE,
            baseline.copy(bluetoothEnabled = false) to
                HeartRateRecoveryStopReason.BLUETOOTH_OFF,
            baseline.copy(manuallySuppressed = true) to
                HeartRateRecoveryStopReason.MANUAL_SUPPRESSION,
            baseline.copy(appVisible = false, activeTrainingFgsActive = false) to
                HeartRateRecoveryStopReason.BACKGROUND_WITHOUT_FGS
        )

        ineligible.forEach { (input, expectedReason) ->
            val decision = evaluateHeartRateRecoveryEligibility(input)
            assertFalse(decision.eligible)
            assertEquals(expectedReason, decision.stopReason)
        }
    }

    @Test
    fun activeTrainingFgsEligibilityCanReplaceForegroundVisibilityOnly() {
        val input = eligibleInput().copy(
            appVisible = false,
            activeTrainingFgsActive = true
        )

        val decision = evaluateHeartRateRecoveryEligibility(input)

        assertTrue(decision.eligible)
        assertEquals(TARGET, decision.targetIdentifier)
    }

    @Test
    fun blankTargetIsNotAnExactSavedTarget() {
        val decision = evaluateHeartRateRecoveryEligibility(
            eligibleInput().copy(savedTargetIdentifier = "  ")
        )

        assertFalse(decision.eligible)
        assertEquals(HeartRateRecoveryStopReason.NO_SAVED_TARGET, decision.stopReason)
    }

    @Test
    fun fullEligibilityMatrixHasExactlyTheDocumentedEligibleCombinations() {
        val values = listOf(false, true)
        values.forEach { optedIn ->
            values.forEach { hasTarget ->
                values.forEach { permission ->
                    values.forEach { bluetooth ->
                        values.forEach { suppressed ->
                            values.forEach { visible ->
                                values.forEach { fgs ->
                                    val input = HeartRateRecoveryEligibilityInput(
                                        optedIn = optedIn,
                                        savedTargetIdentifier = TARGET.takeIf { hasTarget },
                                        permissionGranted = permission,
                                        bluetoothEnabled = bluetooth,
                                        manuallySuppressed = suppressed,
                                        appVisible = visible,
                                        activeTrainingFgsActive = fgs
                                    )
                                    val expected = optedIn && hasTarget && permission &&
                                        bluetooth && !suppressed && (visible || fgs)

                                    assertEquals(
                                        "Unexpected decision for $input",
                                        expected,
                                        evaluateHeartRateRecoveryEligibility(input).eligible
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun eligibleInput() = HeartRateRecoveryEligibilityInput(
        optedIn = true,
        savedTargetIdentifier = TARGET,
        permissionGranted = true,
        bluetoothEnabled = true,
        manuallySuppressed = false,
        appVisible = true,
        activeTrainingFgsActive = false
    )

    private companion object {
        const val TARGET = "D8:F0:42:01:90:D7"
    }
}
