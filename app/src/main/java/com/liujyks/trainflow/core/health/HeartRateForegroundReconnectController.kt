package com.liujyks.trainflow.core.health

internal fun interface HeartRateMonotonicClock {
    fun elapsedRealtimeMs(): Long
}

internal enum class HeartRateScheduledTask {
    RETRY,
    WATCHDOG,
    FRESHNESS
}

internal interface HeartRateControllerScheduler {
    fun schedule(task: HeartRateScheduledTask, delayMs: Long, action: () -> Unit)
    fun cancel(task: HeartRateScheduledTask)
    fun cancelAll()
}

internal enum class HeartRateReconnectRuntimeKind {
    IDLE,
    CONNECTING,
    WAITING,
    LIVE,
    STALE,
    OFFLINE,
    TECHNICAL_ERROR,
    STOPPED
}

internal data class HeartRateReconnectRuntimeState(
    val kind: HeartRateReconnectRuntimeKind = HeartRateReconnectRuntimeKind.IDLE,
    val reason: HeartRateFreshnessReason? = null,
    val currentReconnectAttempt: Int = 0,
    val retryBudgetExhausted: Boolean = false,
    val reconnectInProgress: Boolean = false,
    val bpm: Int? = null,
    val targetGeneration: Long = 0,
    val attemptGeneration: Long = 0
)

internal sealed interface HeartRateReconnectEffect {
    data class ConnectDirect(
        val targetGeneration: Long,
        val attemptGeneration: Long,
        val reconnectAttempt: Int,
        val manual: Boolean
    ) : HeartRateReconnectEffect

    data class CloseAttempt(
        val targetGeneration: Long,
        val attemptGeneration: Long
    ) : HeartRateReconnectEffect

    data class StateChanged(val state: HeartRateReconnectRuntimeState) : HeartRateReconnectEffect
}

/** Identity guard kept Android-free so delayed callbacks can be tested with plain fake GATT objects. */
internal class HeartRateGattAttemptGuard<Gatt : Any> {
    private var currentGatt: Gatt? = null
    private var targetGeneration = 0L
    private var attemptGeneration = 0L

    fun bind(gatt: Gatt, target: Long, attempt: Long) {
        currentGatt = gatt
        targetGeneration = target
        attemptGeneration = attempt
    }

    fun isCurrent(gatt: Gatt, target: Long, attempt: Long): Boolean =
        currentGatt === gatt && targetGeneration == target && attemptGeneration == attempt

    /** Invalidates identity before the adapter performs disconnect()/close(). */
    fun invalidate(gatt: Gatt, target: Long, attempt: Long): Boolean {
        if (!isCurrent(gatt, target, attempt)) return false
        currentGatt = null
        targetGeneration = 0L
        attemptGeneration = 0L
        return true
    }

    fun clear() {
        currentGatt = null
        targetGeneration = 0L
        attemptGeneration = 0L
    }
}

/**
 * Pure, single-owner state machine for one foreground process-local BLE runtime target.
 * Android GATT, Handler, wall time, scanning and UI are deliberately outside this type.
 */
internal class HeartRateForegroundReconnectController(
    private val clock: HeartRateMonotonicClock,
    private val scheduler: HeartRateControllerScheduler,
    private val effectSink: (HeartRateReconnectEffect) -> Unit
) {
    private val freshnessPolicy = HeartRateFreshnessPolicy()
    private var targetGeneration = 0L
    private var attemptGeneration = 0L
    private var freshnessGeneration = 0L
    private var hasRuntimeTarget = false
    private var targetEverLive = false
    private var foreground = true
    private var displayEnabled = true
    private var permissionGranted = true
    private var bluetoothEnabled = true
    private var scanConflict = false
    private var stopSuppressed = false
    private var closed = false
    private var retryAttempt = 0
    private var retryBudgetExhausted = false
    private var activeAttempt = false
    private var timeline = HeartRateFreshnessTimeline()
    private var state = HeartRateReconnectRuntimeState()

    fun currentState(): HeartRateReconnectRuntimeState = state

    fun beginManualTarget(): Long {
        invalidateCurrent(close = true)
        targetGeneration += 1
        hasRuntimeTarget = true
        targetEverLive = false
        stopSuppressed = false
        scanConflict = false
        retryAttempt = 0
        retryBudgetExhausted = false
        timeline = HeartRateFreshnessTimeline()
        startAttempt(manual = true, reconnectAttempt = 0)
        return targetGeneration
    }

    fun notifyEnabled(target: Long, attempt: Long) {
        if (!isCurrentAttempt(target, attempt)) return
        scheduler.cancel(HeartRateScheduledTask.WATCHDOG)
        timeline = timeline.notifyEnabled(clock.elapsedRealtimeMs())
        publishFreshness()
        scheduleFreshness(delayMs = FIRST_SAMPLE_STALE_MS)
    }

    fun validSample(target: Long, attempt: Long, bpm: Int) {
        if (!isCurrentAttempt(target, attempt)) return
        targetEverLive = true
        retryAttempt = 0
        retryBudgetExhausted = false
        state = state.copy(currentReconnectAttempt = 0, reconnectInProgress = false)
        timeline = timeline.validSample(clock.elapsedRealtimeMs())
        publishFreshness(bpm)
        scheduleFreshness(delayMs = LIVE_STALE_MS)
    }

    fun parseFailure(target: Long, attempt: Long) {
        technicalFailure(target, attempt, HeartRateFreshnessReason.PARSE_FAILED)
    }

    fun disconnected(target: Long, attempt: Long) {
        if (!isCurrentAttempt(target, attempt)) return
        activeAttempt = false
        cancelAttemptTasks()
        timeline = timeline.disconnected()
        publishFreshness()
        scheduleRetryIfEligible()
    }

    fun technicalFailure(
        target: Long,
        attempt: Long,
        reason: HeartRateFreshnessReason
    ) {
        if (!isCurrentAttempt(target, attempt)) return
        invalidateAttempt(close = true)
        timeline = timeline.technicalFailure(reason)
        publishFreshness()
        scheduleRetryIfEligible()
    }

    fun setForeground(isForeground: Boolean) {
        foreground = isForeground
        if (!isForeground) cancelAutomaticRecovery(close = true)
    }

    fun setDisplayEnabled(enabled: Boolean) {
        displayEnabled = enabled
        if (!enabled) cancelAutomaticRecovery(close = true)
    }

    fun setPermissionGranted(granted: Boolean) {
        permissionGranted = granted
        if (!granted) cancelAutomaticRecovery(close = true)
    }

    fun setBluetoothEnabled(enabled: Boolean) {
        bluetoothEnabled = enabled
        if (!enabled) cancelAutomaticRecovery(close = true)
    }

    fun onManualScanStarted() {
        if (state.kind != HeartRateReconnectRuntimeKind.LIVE) {
            scanConflict = true
            cancelAutomaticRecovery(close = activeAttempt)
        }
    }

    fun onManualScanEnded() {
        scanConflict = false
    }

    fun userStop() {
        stopSuppressed = true
        cancelAutomaticRecovery(close = true)
        publish(state.copy(kind = HeartRateReconnectRuntimeKind.STOPPED, bpm = null, reconnectInProgress = false))
    }

    fun clearTarget() {
        invalidateCurrent(close = true)
        hasRuntimeTarget = false
        targetEverLive = false
        stopSuppressed = true
        publish(HeartRateReconnectRuntimeState(kind = HeartRateReconnectRuntimeKind.IDLE))
    }

    fun close() {
        if (closed) return
        closed = true
        invalidateCurrent(close = true)
        hasRuntimeTarget = false
        publish(HeartRateReconnectRuntimeState(kind = HeartRateReconnectRuntimeKind.STOPPED))
    }

    private fun startAttempt(manual: Boolean, reconnectAttempt: Int) {
        if (closed || !hasRuntimeTarget) return
        if (!manual && !eligibleForAutomaticReconnect()) return
        scheduler.cancel(HeartRateScheduledTask.RETRY)
        attemptGeneration += 1
        activeAttempt = true
        val attempt = attemptGeneration
        publish(
            state.copy(
                kind = HeartRateReconnectRuntimeKind.CONNECTING,
                reason = null,
                currentReconnectAttempt = reconnectAttempt,
                retryBudgetExhausted = false,
                reconnectInProgress = !manual,
                bpm = null,
                targetGeneration = targetGeneration,
                attemptGeneration = attempt
            )
        )
        effectSink(
            HeartRateReconnectEffect.ConnectDirect(
                targetGeneration = targetGeneration,
                attemptGeneration = attempt,
                reconnectAttempt = reconnectAttempt,
                manual = manual
            )
        )
        scheduler.schedule(HeartRateScheduledTask.WATCHDOG, WATCHDOG_MS) {
            if (isCurrentAttempt(targetGeneration, attempt)) {
                technicalFailure(targetGeneration, attempt, HeartRateFreshnessReason.CONNECT_FAILED)
            }
        }
    }

    private fun scheduleRetryIfEligible() {
        if (!eligibleForAutomaticReconnect()) return
        if (retryAttempt >= MAX_RETRIES) {
            retryBudgetExhausted = true
            scheduler.cancelAll()
            publish(
                state.copy(
                    currentReconnectAttempt = MAX_RETRIES,
                    retryBudgetExhausted = true,
                    reconnectInProgress = false,
                    bpm = null
                )
            )
            return
        }
        val nextAttempt = retryAttempt + 1
        retryAttempt = nextAttempt
        publish(
            state.copy(
                currentReconnectAttempt = nextAttempt,
                reconnectInProgress = true,
                retryBudgetExhausted = false,
                bpm = null
            )
        )
        val expectedTarget = targetGeneration
        val delay = RETRY_DELAYS_MS[nextAttempt - 1]
        scheduler.schedule(HeartRateScheduledTask.RETRY, delay) {
            if (expectedTarget == targetGeneration && eligibleForAutomaticReconnect()) {
                startAttempt(manual = false, reconnectAttempt = nextAttempt)
            }
        }
    }

    private fun eligibleForAutomaticReconnect(): Boolean =
        !closed && foreground && displayEnabled && permissionGranted && bluetoothEnabled &&
            !stopSuppressed && !scanConflict && hasRuntimeTarget && targetEverLive &&
            !retryBudgetExhausted && !activeAttempt

    private fun scheduleFreshness(delayMs: Long) {
        scheduler.cancel(HeartRateScheduledTask.FRESHNESS)
        freshnessGeneration += 1
        val expectedFreshness = freshnessGeneration
        val expectedTarget = targetGeneration
        val expectedAttempt = attemptGeneration
        scheduler.schedule(HeartRateScheduledTask.FRESHNESS, delayMs) {
            if (
                expectedFreshness == freshnessGeneration &&
                isCurrentAttempt(expectedTarget, expectedAttempt)
            ) {
                val decision = publishFreshness()
                when (decision.kind) {
                    HeartRateFreshnessKind.WAITING -> scheduleFreshness(FIRST_SAMPLE_STALE_MS)
                    HeartRateFreshnessKind.LIVE -> scheduleFreshness(LIVE_STALE_MS)
                    HeartRateFreshnessKind.STALE -> {
                        val origin = timeline.lastValidSampleElapsedMs
                            ?: timeline.notifyEnabledAtElapsedMs
                            ?: clock.elapsedRealtimeMs()
                        scheduleFreshness((TECHNICAL_ERROR_MS - (clock.elapsedRealtimeMs() - origin)).coerceAtLeast(0L))
                    }
                    HeartRateFreshnessKind.TECHNICAL_ERROR -> {
                        invalidateAttempt(close = true)
                        scheduleRetryIfEligible()
                    }
                    HeartRateFreshnessKind.OFFLINE -> Unit
                }
            }
        }
    }

    private fun publishFreshness(bpm: Int? = null): HeartRateFreshnessDecision {
        val decision = freshnessPolicy.evaluate(clock.elapsedRealtimeMs(), timeline)
        val runtimeKind = when (decision.kind) {
            HeartRateFreshnessKind.WAITING -> HeartRateReconnectRuntimeKind.WAITING
            HeartRateFreshnessKind.LIVE -> HeartRateReconnectRuntimeKind.LIVE
            HeartRateFreshnessKind.STALE -> HeartRateReconnectRuntimeKind.STALE
            HeartRateFreshnessKind.OFFLINE -> HeartRateReconnectRuntimeKind.OFFLINE
            HeartRateFreshnessKind.TECHNICAL_ERROR -> HeartRateReconnectRuntimeKind.TECHNICAL_ERROR
        }
        publish(
            state.copy(
                kind = runtimeKind,
                reason = decision.reason,
                retryBudgetExhausted = retryBudgetExhausted,
                reconnectInProgress = state.reconnectInProgress && runtimeKind != HeartRateReconnectRuntimeKind.LIVE,
                bpm = if (runtimeKind == HeartRateReconnectRuntimeKind.LIVE) bpm ?: state.bpm else null,
                targetGeneration = targetGeneration,
                attemptGeneration = attemptGeneration
            )
        )
        return decision
    }

    private fun cancelAutomaticRecovery(close: Boolean) {
        scheduler.cancelAll()
        freshnessGeneration += 1
        if (close) invalidateAttempt(close = true) else activeAttempt = false
        if (hasRuntimeTarget && state.kind !in setOf(HeartRateReconnectRuntimeKind.IDLE, HeartRateReconnectRuntimeKind.STOPPED)) {
            timeline = timeline.disconnected()
            publish(
                state.copy(
                    kind = HeartRateReconnectRuntimeKind.OFFLINE,
                    reason = HeartRateFreshnessReason.GATT_DISCONNECTED,
                    reconnectInProgress = false,
                    bpm = null,
                    attemptGeneration = attemptGeneration
                )
            )
        }
    }

    private fun invalidateCurrent(close: Boolean) {
        scheduler.cancelAll()
        freshnessGeneration += 1
        invalidateAttempt(close)
    }

    private fun invalidateAttempt(close: Boolean) {
        val oldAttempt = attemptGeneration
        val oldTarget = targetGeneration
        activeAttempt = false
        attemptGeneration += 1
        cancelAttemptTasks()
        if (close && oldAttempt > 0L) {
            effectSink(HeartRateReconnectEffect.CloseAttempt(oldTarget, oldAttempt))
        }
    }

    private fun cancelAttemptTasks() {
        scheduler.cancel(HeartRateScheduledTask.WATCHDOG)
        scheduler.cancel(HeartRateScheduledTask.FRESHNESS)
        freshnessGeneration += 1
    }

    private fun isCurrentAttempt(target: Long, attempt: Long): Boolean =
        hasRuntimeTarget && activeAttempt && target == targetGeneration && attempt == attemptGeneration

    private fun publish(next: HeartRateReconnectRuntimeState) {
        state = next
        effectSink(HeartRateReconnectEffect.StateChanged(next))
    }

    private companion object {
        const val LIVE_STALE_MS = 10_000L
        const val FIRST_SAMPLE_STALE_MS = 15_000L
        const val TECHNICAL_ERROR_MS = 30_000L
        const val WATCHDOG_MS = 10_000L
        const val MAX_RETRIES = 3
        val RETRY_DELAYS_MS = longArrayOf(2_000L, 5_000L, 10_000L)
    }
}
