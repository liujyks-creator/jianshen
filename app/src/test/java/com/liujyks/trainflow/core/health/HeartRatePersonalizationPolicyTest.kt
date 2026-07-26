package com.liujyks.trainflow.core.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRatePersonalizationPolicyTest {
    @Test
    fun personalMaximumTakesPrecedenceOverAgeEstimate() {
        val result = HeartRatePersonalizationPolicy.evaluate(
            bpm = 130,
            ageYears = 40,
            personalMaxHeartRateBpm = 200,
            alertThresholdBpm = null
        )

        assertEquals(200, result.effectiveMaxHeartRateBpm)
        assertEquals(HeartRatePersonalizationPresentation.FAT_BURN, result.presentation)
    }

    @Test
    fun age101IsValidAndUses220MinusAgeWithoutClamping() {
        val result = HeartRatePersonalizationPolicy.evaluate(
            bpm = 60,
            ageYears = 101,
            personalMaxHeartRateBpm = null,
            alertThresholdBpm = null
        )

        assertEquals(119, result.effectiveMaxHeartRateBpm)
        assertEquals(HeartRatePersonalizationPresentation.WARMUP, result.presentation)
    }

    @Test
    fun missingMaximumKeepsBpmOnlyWhileIndependentAlertStillWins() {
        val bpmOnly = HeartRatePersonalizationPolicy.evaluate(
            bpm = 100,
            ageYears = null,
            personalMaxHeartRateBpm = null,
            alertThresholdBpm = null
        )
        val alertOnly = HeartRatePersonalizationPolicy.evaluate(
            bpm = 101,
            ageYears = null,
            personalMaxHeartRateBpm = null,
            alertThresholdBpm = 100
        )

        assertNull(bpmOnly.effectiveMaxHeartRateBpm)
        assertEquals(HeartRatePersonalizationPresentation.BPM_ONLY, bpmOnly.presentation)
        assertNull(alertOnly.effectiveMaxHeartRateBpm)
        assertEquals(HeartRatePersonalizationPresentation.OVER_LIMIT, alertOnly.presentation)
    }

    @Test
    fun alertIsStrictlyGreaterAndHasPriorityOverZone() {
        val equal = HeartRatePersonalizationPolicy.evaluate(
            bpm = 150,
            ageYears = null,
            personalMaxHeartRateBpm = 200,
            alertThresholdBpm = 150
        )
        val exceeded = HeartRatePersonalizationPolicy.evaluate(
            bpm = 151,
            ageYears = null,
            personalMaxHeartRateBpm = 200,
            alertThresholdBpm = 150
        )

        assertEquals(HeartRatePersonalizationPresentation.AEROBIC, equal.presentation)
        assertEquals(HeartRatePersonalizationPresentation.OVER_LIMIT, exceeded.presentation)
    }

    @Test
    fun exactUnroundedCrossMultiplicationDefinesAllSixZoneBoundaries() {
        val maximum = 201
        val cases = listOf(
            100 to HeartRatePersonalizationPresentation.LOW_INTENSITY,
            101 to HeartRatePersonalizationPresentation.WARMUP,
            120 to HeartRatePersonalizationPresentation.WARMUP,
            121 to HeartRatePersonalizationPresentation.FAT_BURN,
            140 to HeartRatePersonalizationPresentation.FAT_BURN,
            141 to HeartRatePersonalizationPresentation.AEROBIC,
            160 to HeartRatePersonalizationPresentation.AEROBIC,
            161 to HeartRatePersonalizationPresentation.ANAEROBIC,
            180 to HeartRatePersonalizationPresentation.ANAEROBIC,
            181 to HeartRatePersonalizationPresentation.LIMIT
        )

        cases.forEach { (bpm, expected) ->
            val result = HeartRatePersonalizationPolicy.evaluate(
                bpm = bpm,
                ageYears = null,
                personalMaxHeartRateBpm = maximum,
                alertThresholdBpm = null
            )
            assertEquals("$bpm / $maximum", expected, result.presentation)
        }
    }

    @Test
    fun invalidDirectParametersFailClosedWithoutClamping() {
        val result = HeartRatePersonalizationPolicy.evaluate(
            bpm = 100,
            ageYears = 131,
            personalMaxHeartRateBpm = 261,
            alertThresholdBpm = 29
        )

        assertNull(result.effectiveMaxHeartRateBpm)
        assertEquals(HeartRatePersonalizationPresentation.BPM_ONLY, result.presentation)
    }
}
