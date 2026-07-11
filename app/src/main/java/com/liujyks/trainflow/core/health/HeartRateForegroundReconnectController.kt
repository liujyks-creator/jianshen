package com.liujyks.trainflow.core.health

internal fun interface HeartRateMonotonicClock {
    fun elapsedRealtimeMs(): Long
}

internal enum class HeartRateScheduledTask { RETRY, WATCHDOG, FRESHNESS }

internal interface HeartRateControllerScheduler {
    fun schedule(task: HeartRateScheduledTask, delayMs: Long, action: () -> Unit)
    fun cancel(task: HeartRateScheduledTask)
    fun cancelAll()
}

internal enum class HeartRateReconnectRuntimeKind {
    IDLE, CONNECTING, WAITING, LIVE, STALE, OFFLINE, TECHNICAL_ERROR, STOPPED
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

    data class CloseAttempt(val targetGeneration: Long, val attemptGeneration: Long) : HeartRateReconnectEffect
    data class StateChanged(val state: HeartRateReconnectRuntimeState) : HeartRateReconnectEffect
}

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

    fun invalidate(gatt: Gatt, target: Long, attempt: Long): Boolean {
        if (!isCurrent(gatt, target, attempt)) return false
        clear()
        return true
    }

    fun clear() {
        currentGatt = null
        targetGeneration = 0L
        attemptGeneration = 0L
    }
}

/** Pure single-owner foreground reconnect state machine. */
internal class HeartRateForegroundReconnectController(
    private val clock: HeartRateMonotonicClock,
    private val scheduler: HeartRateControllerScheduler,
    private val effectSink: (HeartRateReconnectEffect) -> Unit
) {
    private val freshnessPolicy = HeartRateFreshnessPolicy()
    private var targetGeneration = 0L
    private var attemptGeneration = 0L
    private var freshnessGeneration = 0L
    private var recoveryEpoch = 0L
    private var hasRuntimeTarget = false
    private var targetEverLive = false
    private var foreground = true
    private var displayEnabled = true
    private var permissionGranted = true
    private var bluetoothEnabled = true
    private var scanActive = false
    private var scanMayResumeQueue = false
    private var scanTargetGeneration = 0L
    private var pendingRetryAfterScan = false
    private var stopSuppressed = false
    private var closed = false
    private var retryAttempt = 0
    private var retryBudgetExhausted = false
    private var activeAttempt = false
    private var timeline = HeartRateFreshnessTimeline()
    private var state = HeartRateReconnectRuntimeState()

    fun currentState(): HeartRateReconnectRuntimeState = state

    /** A new selection is the only operation that creates a target generation and resets its facts. */
    fun selectNewTarget(): Long {
        if (closed) return targetGeneration
        invalidateCurrent(close = true)
        targetGeneration += 1
        hasRuntimeTarget = true
        targetEverLive = false
        stopSuppressed = false
        retryAttempt = 0
        retryBudgetExhausted = false
        timeline = HeartRateFreshnessTimeline()
        publish(HeartRateReconnectRuntimeState(targetGeneration = targetGeneration, attemptGeneration = attemptGeneration))
        return targetGeneration
    }

    /** A manual attempt for the existing target is free and preserves ever-live and retry facts until valid bpm. */
    fun beginManualAttempt() {
        if (closed || !hasRuntimeTarget || !baseEligibility() || scanActive) return
        stopSuppressed = false
        invalidateAttempt(close = true)
        startAttempt(manual = true, reconnectAttempt = retryAttempt)
    }

    fun notifyEnabled(target: Long, attempt: Long) {
        if (!isCurrentAttempt(target, attempt)) return
        scheduler.cancel(HeartRateScheduledTask.WATCHDOG)
        timeline = timeline.notifyEnabled(clock.elapsedRealtimeMs())
        publishFreshness()
        scheduleFreshness(FIRST_SAMPLE_STALE_MS)
    }

    fun validSample(target: Long, attempt: Long, bpm: Int) {
        if (!isCurrentAttempt(target, attempt)) return
        targetEverLive = true
        retryAttempt = 0
        retryBudgetExhausted = false
        pendingRetryAfterScan = false
        state = state.copy(currentReconnectAttempt = 0, retryBudgetExhausted = false, reconnectInProgress = false)
        timeline = timeline.validSample(clock.elapsedRealtimeMs())
        publishFreshness(bpm)
        scheduleFreshness(LIVE_STALE_MS)
    }

    fun parseFailure(target: Long, attempt: Long) =
        technicalFailure(target, attempt, HeartRateFreshnessReason.PARSE_FAILED)

    fun disconnected(target: Long, attempt: Long) {
        if (!isCurrentAttempt(target, attempt)) return
        activeAttempt = false
        cancelAttemptTasks()
        timeline = timeline.disconnected()
        publishFreshness()
        queueOrScheduleRetry()
    }

    fun technicalFailure(target: Long, attempt: Long, reason: HeartRateFreshnessReason) {
        if (!isCurrentAttempt(target, attempt)) return
        invalidateAttempt(close = true, cancelRecovery = false)
        timeline = timeline.technicalFailure(reason)
        publishFreshness()
        queueOrScheduleRetry()
    }

    fun setForeground(value: Boolean) {
        foreground = value
        if (!value) cancelAutomaticRecovery(close = true)
    }

    fun setDisplayEnabled(value: Boolean) {
        displayEnabled = value
        if (!value) cancelAutomaticRecovery(close = true)
    }

    fun setPermissionGranted(value: Boolean) {
        permissionGranted = value
        if (!value) cancelAutomaticRecovery(close = true)
    }

    fun setBluetoothEnabled(value: Boolean) {
        bluetoothEnabled = value
        if (!value) cancelAutomaticRecovery(close = true)
    }

    fun onManualScanStarted() {
        if (closed) return
        scanActive = true
        scanTargetGeneration = targetGeneration
        scanMayResumeQueue = state.kind == HeartRateReconnectRuntimeKind.LIVE && targetEverLive
        pendingRetryAfterScan = false
        scheduler.cancel(HeartRateScheduledTask.RETRY)
        if (!scanMayResumeQueue) cancelAutomaticRecovery(close = activeAttempt)
    }

    fun onManualScanEnded() {
        if (!scanActive) return
        scanActive = false
        val mayResume = scanMayResumeQueue && pendingRetryAfterScan && scanTargetGeneration == targetGeneration
        scanMayResumeQueue = false
        pendingRetryAfterScan = false
        if (mayResume && eligibleForAutomaticReconnect()) scheduleCurrentRetry()
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
        // Provider closes its sinks before this call; keep internal state observable for tests only.
        state = HeartRateReconnectRuntimeState(kind = HeartRateReconnectRuntimeKind.STOPPED)
    }

    private fun startAttempt(manual: Boolean, reconnectAttempt: Int) {
        if (closed || !hasRuntimeTarget || !baseEligibility() || scanActive) return
        if (!manual && !eligibleForAutomaticReconnect()) return
        scheduler.cancel(HeartRateScheduledTask.RETRY)
        attemptGeneration += 1
        activeAttempt = true
        val target = targetGeneration
        val attempt = attemptGeneration
        val epoch = recoveryEpoch
        publish(
            state.copy(
                kind = HeartRateReconnectRuntimeKind.CONNECTING,
                reason = null,
                currentReconnectAttempt = retryAttempt,
                retryBudgetExhausted = retryBudgetExhausted,
                reconnectInProgress = !manual,
                bpm = null,
                targetGeneration = target,
                attemptGeneration = attempt
            )
        )
        effectSink(HeartRateReconnectEffect.ConnectDirect(target, attempt, reconnectAttempt, manual))
        scheduler.schedule(HeartRateScheduledTask.WATCHDOG, WATCHDOG_MS) {
            if (epoch == recoveryEpoch && isCurrentAttempt(target, attempt) && baseEligibility() && !scanActive) {
                technicalFailure(target, attempt, HeartRateFreshnessReason.CONNECT_FAILED)
            }
        }
    }

    private fun queueOrScheduleRetry() {
        if (!automaticFactsEligible()) return
        if (retryAttempt >= MAX_RETRIES) {
            retryBudgetExhausted = true
            pendingRetryAfterScan = false
            scheduler.cancel(HeartRateScheduledTask.RETRY)
            publish(state.copy(currentReconnectAttempt = MAX_RETRIES, retryBudgetExhausted = true, reconnectInProgress = false, bpm = null))
            return
        }
        retryAttempt += 1
        publish(state.copy(currentReconnectAttempt = retryAttempt, retryBudgetExhausted = false, reconnectInProgress = true, bpm = null))
        if (scanActive) {
            pendingRetryAfterScan = scanMayResumeQueue && scanTargetGeneration == targetGeneration
            return
        }
        if (eligibleForAutomaticReconnect()) scheduleCurrentRetry()
    }

    private fun scheduleCurrentRetry() {
        val attemptNumber = retryAttempt
        if (attemptNumber !in 1..MAX_RETRIES) return
        val target = targetGeneration
        val epoch = recoveryEpoch
        scheduler.schedule(HeartRateScheduledTask.RETRY, RETRY_DELAYS_MS[attemptNumber - 1]) {
            if (
                epoch == recoveryEpoch && target == targetGeneration && retryAttempt == attemptNumber &&
                eligibleForAutomaticReconnect()
            ) startAttempt(manual = false, reconnectAttempt = attemptNumber)
        }
    }

    private fun automaticFactsEligible(): Boolean =
        !closed && baseEligibility() && !stopSuppressed && hasRuntimeTarget && targetEverLive && !retryBudgetExhausted && !activeAttempt

    private fun eligibleForAutomaticReconnect(): Boolean = automaticFactsEligible() && !scanActive

    private fun baseEligibility(): Boolean = foreground && displayEnabled && permissionGranted && bluetoothEnabled

    private fun scheduleFreshness(delayMs: Long) {
        scheduler.cancel(HeartRateScheduledTask.FRESHNESS)
        freshnessGeneration += 1
        val freshness = freshnessGeneration
        val target = targetGeneration
        val attempt = attemptGeneration
        val epoch = recoveryEpoch
        scheduler.schedule(HeartRateScheduledTask.FRESHNESS, delayMs) {
            if (epoch != recoveryEpoch || freshness != freshnessGeneration || !isCurrentAttempt(target, attempt)) return@schedule
            val decision = publishFreshness()
            when (decision.kind) {
                HeartRateFreshnessKind.WAITING -> scheduleFreshness(FIRST_SAMPLE_STALE_MS)
                HeartRateFreshnessKind.LIVE -> scheduleFreshness(LIVE_STALE_MS)
                HeartRateFreshnessKind.STALE -> {
                    val origin = timeline.lastValidSampleElapsedMs ?: timeline.notifyEnabledAtElapsedMs ?: clock.elapsedRealtimeMs()
                    scheduleFreshness((TECHNICAL_ERROR_MS - (clock.elapsedRealtimeMs() - origin)).coerceAtLeast(0L))
                }
                HeartRateFreshnessKind.TECHNICAL_ERROR -> {
                    invalidateAttempt(close = true, cancelRecovery = false)
                    queueOrScheduleRetry()
                }
                HeartRateFreshnessKind.OFFLINE -> Unit
            }
        }
    }

    private fun publishFreshness(bpm: Int? = null): HeartRateFreshnessDecision {
        val decision = freshnessPolicy.evaluate(clock.elapsedRealtimeMs(), timeline)
        val kind = when (decision.kind) {
            HeartRateFreshnessKind.WAITING -> HeartRateReconnectRuntimeKind.WAITING
            HeartRateFreshnessKind.LIVE -> HeartRateReconnectRuntimeKind.LIVE
            HeartRateFreshnessKind.STALE -> HeartRateReconnectRuntimeKind.STALE
            HeartRateFreshnessKind.OFFLINE -> HeartRateReconnectRuntimeKind.OFFLINE
            HeartRateFreshnessKind.TECHNICAL_ERROR -> HeartRateReconnectRuntimeKind.TECHNICAL_ERROR
        }
        publish(state.copy(kind = kind, reason = decision.reason, retryBudgetExhausted = retryBudgetExhausted,
            reconnectInProgress = state.reconnectInProgress && kind != HeartRateReconnectRuntimeKind.LIVE,
            bpm = if (kind == HeartRateReconnectRuntimeKind.LIVE) bpm ?: state.bpm else null,
            targetGeneration = targetGeneration, attemptGeneration = attemptGeneration))
        return decision
    }

    private fun cancelAutomaticRecovery(close: Boolean) {
        recoveryEpoch += 1
        scheduler.cancelAll()
        pendingRetryAfterScan = false
        scanMayResumeQueue = false
        freshnessGeneration += 1
        if (close) invalidateAttempt(close = true, cancelRecovery = false) else activeAttempt = false
        if (hasRuntimeTarget && state.kind !in setOf(HeartRateReconnectRuntimeKind.IDLE, HeartRateReconnectRuntimeKind.STOPPED)) {
            timeline = timeline.disconnected()
            publish(state.copy(kind = HeartRateReconnectRuntimeKind.OFFLINE, reason = HeartRateFreshnessReason.GATT_DISCONNECTED,
                reconnectInProgress = false, bpm = null, attemptGeneration = attemptGeneration))
        }
    }

    private fun invalidateCurrent(close: Boolean) {
        recoveryEpoch += 1
        scheduler.cancelAll()
        pendingRetryAfterScan = false
        scanMayResumeQueue = false
        scanActive = false
        freshnessGeneration += 1
        invalidateAttempt(close, cancelRecovery = false)
    }

    private fun invalidateAttempt(close: Boolean, cancelRecovery: Boolean = true) {
        if (cancelRecovery) recoveryEpoch += 1
        val oldAttempt = attemptGeneration
        val oldTarget = targetGeneration
        val wasActive = activeAttempt
        activeAttempt = false
        attemptGeneration += 1
        cancelAttemptTasks()
        if (close && wasActive && oldAttempt > 0L) effectSink(HeartRateReconnectEffect.CloseAttempt(oldTarget, oldAttempt))
    }

    private fun cancelAttemptTasks() {
        scheduler.cancel(HeartRateScheduledTask.WATCHDOG)
        scheduler.cancel(HeartRateScheduledTask.FRESHNESS)
        freshnessGeneration += 1
    }

    private fun isCurrentAttempt(target: Long, attempt: Long): Boolean =
        !closed && hasRuntimeTarget && activeAttempt && target == targetGeneration && attempt == attemptGeneration

    private fun publish(next: HeartRateReconnectRuntimeState) {
        if (closed) return
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
