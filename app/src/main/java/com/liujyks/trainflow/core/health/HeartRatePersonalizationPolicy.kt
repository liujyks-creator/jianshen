package com.liujyks.trainflow.core.health

internal data class HeartRatePersonalizationResult(
    val effectiveMaxHeartRateBpm: Int?,
    val presentation: HeartRatePersonalizationPresentation
)

internal enum class HeartRatePersonalizationPresentation {
    BPM_ONLY,
    LOW_INTENSITY,
    WARMUP,
    FAT_BURN,
    AEROBIC,
    ANAEROBIC,
    LIMIT,
    OVER_LIMIT
}

internal object HeartRatePersonalizationPolicy {
    fun evaluate(
        bpm: Int,
        ageYears: Int?,
        personalMaxHeartRateBpm: Int?,
        alertThresholdBpm: Int?
    ): HeartRatePersonalizationResult {
        val validAge = ageYears?.takeIf { it in 1..130 }
        val validPersonalMax = personalMaxHeartRateBpm?.takeIf { it in 30..260 }
        val validAlert = alertThresholdBpm?.takeIf { it in 30..260 }
        val effectiveMax = validPersonalMax ?: validAge?.let { 220 - it }
        if (validAlert != null && bpm > validAlert) {
            return HeartRatePersonalizationResult(
                effectiveMaxHeartRateBpm = effectiveMax,
                presentation = HeartRatePersonalizationPresentation.OVER_LIMIT
            )
        }
        if (effectiveMax == null) {
            return HeartRatePersonalizationResult(
                effectiveMaxHeartRateBpm = null,
                presentation = HeartRatePersonalizationPresentation.BPM_ONLY
            )
        }

        val ratioHundredths = bpm.toLong() * 100L
        val maximum = effectiveMax.toLong()
        val presentation = when {
            ratioHundredths < maximum * 50L ->
                HeartRatePersonalizationPresentation.LOW_INTENSITY
            ratioHundredths < maximum * 60L ->
                HeartRatePersonalizationPresentation.WARMUP
            ratioHundredths < maximum * 70L ->
                HeartRatePersonalizationPresentation.FAT_BURN
            ratioHundredths < maximum * 80L ->
                HeartRatePersonalizationPresentation.AEROBIC
            ratioHundredths < maximum * 90L ->
                HeartRatePersonalizationPresentation.ANAEROBIC
            else -> HeartRatePersonalizationPresentation.LIMIT
        }
        return HeartRatePersonalizationResult(effectiveMax, presentation)
    }
}
