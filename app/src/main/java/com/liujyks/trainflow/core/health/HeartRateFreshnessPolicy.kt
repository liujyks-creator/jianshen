package com.liujyks.trainflow.core.health

/**
 * Monotonic timestamps retained by the BLE adapter around a single notify subscription.
 *
 * This type intentionally has no clock, scheduler, Android, scan, connect, or retry dependency.
 * Callers provide elapsed-time values and keep wall-clock display timestamps elsewhere.
 */
internal data class HeartRateFreshnessTimeline(
    val notifyEnabledAtElapsedMs: Long? = null,
    val lastValidSampleElapsedMs: Long? = null,
    val latestFailureReason: HeartRateFreshnessReason? = null
) {
    fun notifyEnabled(atElapsedMs: Long): HeartRateFreshnessTimeline = copy(
        notifyEnabledAtElapsedMs = atElapsedMs,
        lastValidSampleElapsedMs = null,
        latestFailureReason = null
    )

    fun validSample(atElapsedMs: Long): HeartRateFreshnessTimeline = copy(
        lastValidSampleElapsedMs = atElapsedMs,
        latestFailureReason = null
    )

    fun malformedSample(): HeartRateFreshnessTimeline =
        technicalFailure(HeartRateFreshnessReason.PARSE_FAILED)

    fun disconnected(): HeartRateFreshnessTimeline = copy(
        latestFailureReason = HeartRateFreshnessReason.GATT_DISCONNECTED
    )

    fun technicalFailure(reason: HeartRateFreshnessReason): HeartRateFreshnessTimeline = copy(
        latestFailureReason = if (reason.isTechnicalFailure) {
            reason
        } else {
            HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
        }
    )

    /** Retry exhaustion changes recovery budget, not the latest observed connection fact. */
    fun retryExhausted(): HeartRateFreshnessTimeline = this
}

internal class HeartRateFreshnessPolicy {
    fun evaluate(
        nowElapsedMs: Long,
        timeline: HeartRateFreshnessTimeline
    ): HeartRateFreshnessDecision {
        timeline.latestFailureReason?.let { reason ->
            return HeartRateFreshnessDecision(
                kind = if (reason == HeartRateFreshnessReason.GATT_DISCONNECTED) {
                    HeartRateFreshnessKind.OFFLINE
                } else {
                    HeartRateFreshnessKind.TECHNICAL_ERROR
                },
                reason = reason
            )
        }

        val notifyEnabledAtElapsedMs = timeline.notifyEnabledAtElapsedMs
            ?: return invalidTimeDecision()
        val lastValidSampleElapsedMs = timeline.lastValidSampleElapsedMs
        if (
            nowElapsedMs < 0L ||
            notifyEnabledAtElapsedMs < 0L ||
            notifyEnabledAtElapsedMs > nowElapsedMs ||
            lastValidSampleElapsedMs != null && (
                lastValidSampleElapsedMs < notifyEnabledAtElapsedMs ||
                    lastValidSampleElapsedMs > nowElapsedMs
                )
        ) {
            return invalidTimeDecision()
        }

        return if (lastValidSampleElapsedMs == null) {
            evaluateFirstSampleWait(nowElapsedMs - notifyEnabledAtElapsedMs)
        } else {
            evaluateValidSampleAge(nowElapsedMs - lastValidSampleElapsedMs)
        }
    }

    private fun evaluateFirstSampleWait(ageMs: Long): HeartRateFreshnessDecision = when {
        ageMs >= TechnicalErrorAfterMs -> HeartRateFreshnessDecision(
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.FIRST_SAMPLE_SILENCE
        )

        ageMs >= FirstSampleStaleAfterMs -> HeartRateFreshnessDecision(
            HeartRateFreshnessKind.STALE,
            HeartRateFreshnessReason.FIRST_SAMPLE_STALE
        )

        else -> HeartRateFreshnessDecision(
            HeartRateFreshnessKind.WAITING,
            HeartRateFreshnessReason.WAITING_FIRST_SAMPLE
        )
    }

    private fun evaluateValidSampleAge(ageMs: Long): HeartRateFreshnessDecision = when {
        ageMs >= TechnicalErrorAfterMs -> HeartRateFreshnessDecision(
            HeartRateFreshnessKind.TECHNICAL_ERROR,
            HeartRateFreshnessReason.NOTIFY_SILENCE
        )

        ageMs >= LiveSampleStaleAfterMs -> HeartRateFreshnessDecision(
            HeartRateFreshnessKind.STALE,
            HeartRateFreshnessReason.SAMPLE_STALE
        )

        else -> HeartRateFreshnessDecision(
            HeartRateFreshnessKind.LIVE,
            HeartRateFreshnessReason.LIVE_VALID_SAMPLE
        )
    }

    private fun invalidTimeDecision() = HeartRateFreshnessDecision(
        HeartRateFreshnessKind.TECHNICAL_ERROR,
        HeartRateFreshnessReason.INVALID_MONOTONIC_TIME
    )

    private companion object {
        const val LiveSampleStaleAfterMs = 10_000L
        const val FirstSampleStaleAfterMs = 15_000L
        const val TechnicalErrorAfterMs = 30_000L
    }
}

internal data class HeartRateFreshnessDecision(
    val kind: HeartRateFreshnessKind,
    val reason: HeartRateFreshnessReason
)

internal enum class HeartRateFreshnessKind {
    WAITING,
    LIVE,
    STALE,
    OFFLINE,
    TECHNICAL_ERROR
}

internal enum class HeartRateFreshnessReason(
    val code: String,
    val isTechnicalFailure: Boolean = false
) {
    WAITING_FIRST_SAMPLE("waiting_first_sample"),
    LIVE_VALID_SAMPLE("live_valid_sample"),
    FIRST_SAMPLE_STALE("first_sample_stale"),
    SAMPLE_STALE("sample_stale"),
    GATT_DISCONNECTED("gatt_disconnected"),
    CONNECT_FAILED("connect_failed", isTechnicalFailure = true),
    SERVICE_DISCOVERY_FAILED("service_discovery_failed", isTechnicalFailure = true),
    CCCD_FAILED("cccd_failed", isTechnicalFailure = true),
    FIRST_SAMPLE_SILENCE("first_sample_silence", isTechnicalFailure = true),
    NOTIFY_SILENCE("notify_silence", isTechnicalFailure = true),
    PARSE_FAILED("parse_failed", isTechnicalFailure = true),
    INVALID_MONOTONIC_TIME("invalid_monotonic_time", isTechnicalFailure = true)
}
