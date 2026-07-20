package com.liujyks.trainflow.core.health

/**
 * Provisional foreground-manual thresholds derived from the 2026-07-19 Band 9 M0 measurement.
 * E17-9 M1 lock-screen/background evidence must replace or confirm these values.
 */
internal data class HeartRateFreshnessConfig(
    val firstSampleWaitingBoundaryMs: Long = 3_000L,
    val liveFreshnessBoundaryMs: Long = 2_500L
)

/**
 * Monotonic facts retained around one notify subscription.
 *
 * Wall-clock [lastValidMeasuredAt] is display data only. Freshness uses only elapsed timestamps.
 */
internal data class HeartRateFreshnessTimeline(
    val notifyEnabledAtElapsedMs: Long? = null,
    val lastValidSampleElapsedMs: Long? = null,
    val lastValidBpm: Int? = null,
    val lastValidMeasuredAt: String? = null,
    val terminalReason: HeartRateFreshnessReason? = null,
    val terminalAtElapsedMs: Long? = null,
    val malformedSampleCount: Int = 0
) {
    fun notifyEnabled(atElapsedMs: Long): HeartRateFreshnessTimeline = copy(
        notifyEnabledAtElapsedMs = atElapsedMs,
        lastValidSampleElapsedMs = null,
        lastValidBpm = null,
        lastValidMeasuredAt = null,
        terminalReason = null,
        terminalAtElapsedMs = null,
        malformedSampleCount = 0
    )

    fun validSample(
        atElapsedMs: Long,
        bpm: Int,
        measuredAt: String
    ): HeartRateFreshnessTimeline = copy(
        lastValidSampleElapsedMs = atElapsedMs,
        lastValidBpm = bpm,
        lastValidMeasuredAt = measuredAt,
        terminalReason = null,
        terminalAtElapsedMs = null
    )

    /** A malformed payload changes diagnostics only and never refreshes the valid origin. */
    fun malformedSample(): HeartRateFreshnessTimeline = copy(
        malformedSampleCount = if (malformedSampleCount == Int.MAX_VALUE) {
            Int.MAX_VALUE
        } else {
            malformedSampleCount + 1
        }
    )

    fun explicitDisconnect(atElapsedMs: Long): HeartRateFreshnessTimeline = copy(
        terminalReason = HeartRateFreshnessReason.EXPLICIT_LINK_DISCONNECT,
        terminalAtElapsedMs = atElapsedMs
    )

    fun intentionalStop(atElapsedMs: Long): HeartRateFreshnessTimeline = copy(
        terminalReason = HeartRateFreshnessReason.INTENTIONAL_STOP,
        terminalAtElapsedMs = atElapsedMs
    )

    fun technicalFailure(
        reason: HeartRateFreshnessReason,
        atElapsedMs: Long
    ): HeartRateFreshnessTimeline = copy(
        terminalReason = if (reason.isTechnicalFailure) {
            reason
        } else {
            HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
        },
        terminalAtElapsedMs = atElapsedMs
    )
}

internal class HeartRateFreshnessPolicy(
    private val config: HeartRateFreshnessConfig = HeartRateFreshnessConfig()
) {
    fun evaluate(
        nowElapsedMs: Long,
        timeline: HeartRateFreshnessTimeline
    ): HeartRateFreshnessDecision {
        if (!hasValidStructure(nowElapsedMs, timeline)) {
            return invalidTimeDecision()
        }

        timeline.terminalReason?.let { reason ->
            return HeartRateFreshnessDecision(
                kind = when {
                    reason == HeartRateFreshnessReason.EXPLICIT_LINK_DISCONNECT ->
                        HeartRateFreshnessKind.LINK_DISCONNECTED
                    reason == HeartRateFreshnessReason.INTENTIONAL_STOP ->
                        HeartRateFreshnessKind.INTENTIONAL_STOP
                    else -> HeartRateFreshnessKind.TECHNICAL_FAILURE
                },
                reason = reason
            )
        }

        val notifyEnabledAtElapsedMs = timeline.notifyEnabledAtElapsedMs
            ?: return invalidTimeDecision()
        val lastValidSampleElapsedMs = timeline.lastValidSampleElapsedMs
        if (lastValidSampleElapsedMs == null) {
            return if (
                nowElapsedMs - notifyEnabledAtElapsedMs < config.firstSampleWaitingBoundaryMs
            ) {
                HeartRateFreshnessDecision(
                    kind = HeartRateFreshnessKind.WAITING,
                    reason = HeartRateFreshnessReason.WAITING_FIRST_SAMPLE
                )
            } else {
                HeartRateFreshnessDecision(
                    kind = HeartRateFreshnessKind.DATA_INTERRUPTED,
                    reason = HeartRateFreshnessReason.FIRST_SAMPLE_INTERRUPTED
                )
            }
        }

        return if (nowElapsedMs - lastValidSampleElapsedMs < config.liveFreshnessBoundaryMs) {
            HeartRateFreshnessDecision(
                kind = HeartRateFreshnessKind.LIVE,
                reason = HeartRateFreshnessReason.LIVE_VALID_SAMPLE,
                bpm = timeline.lastValidBpm,
                measuredAt = timeline.lastValidMeasuredAt
            )
        } else {
            HeartRateFreshnessDecision(
                kind = HeartRateFreshnessKind.DATA_INTERRUPTED,
                reason = HeartRateFreshnessReason.SAMPLE_INTERRUPTED
            )
        }
    }

    private fun hasValidStructure(
        nowElapsedMs: Long,
        timeline: HeartRateFreshnessTimeline
    ): Boolean {
        if (
            nowElapsedMs < 0L ||
            config.firstSampleWaitingBoundaryMs <= 0L ||
            config.liveFreshnessBoundaryMs <= 0L ||
            timeline.malformedSampleCount < 0
        ) {
            return false
        }

        val notifyAt = timeline.notifyEnabledAtElapsedMs
        val sampleAt = timeline.lastValidSampleElapsedMs
        val terminalAt = timeline.terminalAtElapsedMs
        val timestamps = listOfNotNull(notifyAt, sampleAt, terminalAt)
        if (timestamps.any { it < 0L || it > nowElapsedMs }) return false

        val hasSampleData = sampleAt != null ||
            timeline.lastValidBpm != null ||
            timeline.lastValidMeasuredAt != null
        if (hasSampleData) {
            if (
                notifyAt == null ||
                sampleAt == null ||
                timeline.lastValidBpm == null ||
                timeline.lastValidBpm <= 0 ||
                timeline.lastValidMeasuredAt.isNullOrBlank() ||
                sampleAt < notifyAt
            ) {
                return false
            }
        }

        val hasTerminalFact = timeline.terminalReason != null || terminalAt != null
        if (hasTerminalFact) {
            val reason = timeline.terminalReason ?: return false
            if (terminalAt == null || !reason.isTerminalFact) return false
            if (notifyAt != null && terminalAt < notifyAt) return false
            if (sampleAt != null && terminalAt < sampleAt) return false
        }

        return notifyAt != null || hasTerminalFact
    }

    private fun invalidTimeDecision() = HeartRateFreshnessDecision(
        kind = HeartRateFreshnessKind.TECHNICAL_FAILURE,
        reason = HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
    )
}

internal data class HeartRateFreshnessDecision(
    val kind: HeartRateFreshnessKind,
    val reason: HeartRateFreshnessReason,
    val bpm: Int? = null,
    val measuredAt: String? = null
)

internal enum class HeartRateFreshnessKind {
    WAITING,
    LIVE,
    DATA_INTERRUPTED,
    LINK_DISCONNECTED,
    TECHNICAL_FAILURE,
    INTENTIONAL_STOP
}

internal enum class HeartRateFreshnessReason(
    val code: String,
    val isTechnicalFailure: Boolean = false
) {
    WAITING_FIRST_SAMPLE("waiting_first_sample"),
    LIVE_VALID_SAMPLE("live_valid_sample"),
    FIRST_SAMPLE_INTERRUPTED("first_sample_interrupted"),
    SAMPLE_INTERRUPTED("sample_interrupted"),
    EXPLICIT_LINK_DISCONNECT("explicit_link_disconnect"),
    INTENTIONAL_STOP("intentional_stop"),
    CONNECT_FAILED("connect_failed", isTechnicalFailure = true),
    SERVICE_DISCOVERY_FAILED("service_discovery_failed", isTechnicalFailure = true),
    CCCD_FAILED("cccd_failed", isTechnicalFailure = true),
    PLATFORM_FAILURE("platform_failure", isTechnicalFailure = true),
    INVALID_MONOTONIC_TIME("invalid_monotonic_time", isTechnicalFailure = true);

    val isTerminalFact: Boolean
        get() = this == EXPLICIT_LINK_DISCONNECT ||
            this == INTENTIONAL_STOP ||
            isTechnicalFailure
}
