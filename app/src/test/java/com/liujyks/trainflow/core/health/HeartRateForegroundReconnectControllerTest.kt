package com.liujyks.trainflow.core.health

import java.util.EnumMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateForegroundReconnectControllerTest {
    @Test
    fun gattIdentityGuardRejectsOldGattEvenForSameAddressAndInvalidatesBeforeClose() {
        data class FakeGatt(val address: String)
        val oldGatt = FakeGatt("D8:F0:42:01:90:D7")
        val newGatt = FakeGatt("D8:F0:42:01:90:D7")
        val guard = HeartRateGattAttemptGuard<FakeGatt>()

        guard.bind(oldGatt, target = 1, attempt = 1)
        guard.bind(newGatt, target = 1, attempt = 2)

        assertFalse(guard.isCurrent(oldGatt, target = 1, attempt = 1))
        assertFalse(guard.isCurrent(oldGatt, target = 1, attempt = 2))
        assertTrue(guard.isCurrent(newGatt, target = 1, attempt = 2))
        assertTrue(guard.invalidate(newGatt, target = 1, attempt = 2))
        assertFalse(guard.isCurrent(newGatt, target = 1, attempt = 2))
    }

    @Test
    fun firstSampleAndLiveFreshnessUseExactMonotonicBoundariesAndNeverExposeStaleBpm() {
        val fixture = Fixture()
        val manual = fixture.manualAttempt()
        fixture.controller.notifyEnabled(manual.targetGeneration, manual.attemptGeneration)

        fixture.advance(14_999)
        assertEquals(HeartRateReconnectRuntimeKind.WAITING, fixture.state.kind)
        fixture.advance(1)
        assertEquals(HeartRateReconnectRuntimeKind.STALE, fixture.state.kind)
        assertNull(fixture.state.bpm)
        fixture.advance(14_999)
        assertEquals(HeartRateReconnectRuntimeKind.STALE, fixture.state.kind)
        fixture.advance(1)
        assertEquals(HeartRateReconnectRuntimeKind.TECHNICAL_ERROR, fixture.state.kind)
        assertEquals(HeartRateFreshnessReason.FIRST_SAMPLE_SILENCE, fixture.state.reason)

        fixture.controller.beginManualTarget()
        val recovered = fixture.latestConnect()
        fixture.controller.notifyEnabled(recovered.targetGeneration, recovered.attemptGeneration)
        fixture.controller.validSample(recovered.targetGeneration, recovered.attemptGeneration, 91)
        fixture.advance(9_999)
        assertEquals(HeartRateReconnectRuntimeKind.LIVE, fixture.state.kind)
        assertEquals(91, fixture.state.bpm)
        fixture.advance(1)
        assertEquals(HeartRateReconnectRuntimeKind.STALE, fixture.state.kind)
        assertNull(fixture.state.bpm)
        fixture.advance(19_999)
        assertEquals(HeartRateReconnectRuntimeKind.STALE, fixture.state.kind)
        fixture.advance(1)
        assertEquals(HeartRateReconnectRuntimeKind.TECHNICAL_ERROR, fixture.state.kind)
        assertEquals(HeartRateFreshnessReason.NOTIFY_SILENCE, fixture.state.reason)
    }

    @Test
    fun newValidSampleCancelsOldDeadlineAndParseFailureDoesNotRefresh() {
        val fixture = Fixture()
        val attempt = fixture.manualAttempt()
        fixture.controller.notifyEnabled(attempt.targetGeneration, attempt.attemptGeneration)
        fixture.controller.validSample(attempt.targetGeneration, attempt.attemptGeneration, 80)
        fixture.advance(9_000)
        fixture.controller.validSample(attempt.targetGeneration, attempt.attemptGeneration, 82)
        fixture.advance(9_999)
        assertEquals(HeartRateReconnectRuntimeKind.LIVE, fixture.state.kind)
        assertEquals(82, fixture.state.bpm)
        fixture.controller.parseFailure(attempt.targetGeneration, attempt.attemptGeneration)
        assertEquals(HeartRateReconnectRuntimeKind.TECHNICAL_ERROR, fixture.state.kind)
        assertEquals(HeartRateFreshnessReason.PARSE_FAILED, fixture.state.reason)
        assertFalse(fixture.scheduler.has(HeartRateScheduledTask.FRESHNESS))
    }

    @Test
    fun liveDisconnectRetriesAtTwoFiveTenSecondsAndStopsAfterThree() {
        val fixture = Fixture()
        val manual = fixture.liveManualAttempt()
        fixture.controller.disconnected(manual.targetGeneration, manual.attemptGeneration)
        assertEquals(2_000L, fixture.scheduler.delayFor(HeartRateScheduledTask.RETRY))

        fixture.advance(2_000)
        val first = fixture.latestConnect()
        assertEquals(1, first.reconnectAttempt)
        fixture.controller.technicalFailure(first.targetGeneration, first.attemptGeneration, HeartRateFreshnessReason.CONNECT_FAILED)
        assertEquals(5_000L, fixture.scheduler.delayFor(HeartRateScheduledTask.RETRY))

        fixture.advance(5_000)
        val second = fixture.latestConnect()
        assertEquals(2, second.reconnectAttempt)
        fixture.controller.technicalFailure(second.targetGeneration, second.attemptGeneration, HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED)
        assertEquals(10_000L, fixture.scheduler.delayFor(HeartRateScheduledTask.RETRY))

        fixture.advance(10_000)
        val third = fixture.latestConnect()
        assertEquals(3, third.reconnectAttempt)
        fixture.controller.technicalFailure(third.targetGeneration, third.attemptGeneration, HeartRateFreshnessReason.CCCD_FAILED)
        assertTrue(fixture.state.retryBudgetExhausted)
        assertEquals(HeartRateReconnectRuntimeKind.TECHNICAL_ERROR, fixture.state.kind)
        assertEquals(HeartRateFreshnessReason.CCCD_FAILED, fixture.state.reason)
        assertFalse(fixture.scheduler.has(HeartRateScheduledTask.RETRY))
        assertEquals(4, fixture.connects.size) // one manual + three automatic
    }

    @Test
    fun manualAttemptIsFreeAndOnlyValidBpmResetsRetryCount() {
        val fixture = Fixture()
        val manual = fixture.liveManualAttempt()
        fixture.controller.disconnected(manual.targetGeneration, manual.attemptGeneration)
        fixture.advance(2_000)
        val retry = fixture.latestConnect()
        fixture.controller.notifyEnabled(retry.targetGeneration, retry.attemptGeneration)
        assertEquals(1, fixture.state.currentReconnectAttempt)
        fixture.controller.validSample(retry.targetGeneration, retry.attemptGeneration, 77)
        assertEquals(0, fixture.state.currentReconnectAttempt)
        assertFalse(fixture.state.retryBudgetExhausted)
    }

    @Test
    fun watchdogCoversConnectStagesThenNotifySwitchesToFreshness() {
        val fixture = Fixture()
        val first = fixture.manualAttempt()
        assertEquals(10_000L, fixture.scheduler.delayFor(HeartRateScheduledTask.WATCHDOG))
        fixture.advance(10_000)
        assertEquals(HeartRateReconnectRuntimeKind.TECHNICAL_ERROR, fixture.state.kind)
        assertEquals(HeartRateFreshnessReason.CONNECT_FAILED, fixture.state.reason)

        val secondFixture = Fixture()
        val second = secondFixture.manualAttempt()
        secondFixture.controller.notifyEnabled(second.targetGeneration, second.attemptGeneration)
        assertFalse(secondFixture.scheduler.has(HeartRateScheduledTask.WATCHDOG))
        assertEquals(15_000L, secondFixture.scheduler.delayFor(HeartRateScheduledTask.FRESHNESS))
        secondFixture.advance(10_000)
        assertEquals(HeartRateReconnectRuntimeKind.WAITING, secondFixture.state.kind)
    }

    @Test
    fun oldWatchdogRetryAndCallbacksCannotAffectNewTargetOrSameAddressAttempt() {
        val fixture = Fixture()
        val old = fixture.liveManualAttempt()
        fixture.controller.disconnected(old.targetGeneration, old.attemptGeneration)
        val nextTarget = fixture.controller.beginManualTarget()
        val fresh = fixture.latestConnect()
        assertTrue(nextTarget > old.targetGeneration)

        fixture.controller.notifyEnabled(old.targetGeneration, old.attemptGeneration)
        fixture.controller.validSample(old.targetGeneration, old.attemptGeneration, 199)
        fixture.controller.disconnected(old.targetGeneration, old.attemptGeneration)
        fixture.advance(20_000)

        assertEquals(fresh.targetGeneration, fixture.state.targetGeneration)
        assertFalse(fixture.state.bpm == 199)
        assertEquals(2, fixture.connects.size)
    }

    @Test
    fun neverLiveTargetColdStartAndMissingRuntimeTargetNeverRetry() {
        val fixture = Fixture()
        val attempt = fixture.manualAttempt()
        fixture.controller.disconnected(attempt.targetGeneration, attempt.attemptGeneration)
        assertFalse(fixture.scheduler.has(HeartRateScheduledTask.RETRY))

        val cold = Fixture()
        cold.controller.setForeground(true)
        cold.controller.setPermissionGranted(true)
        cold.controller.setBluetoothEnabled(true)
        cold.advance(60_000)
        assertTrue(cold.connects.isEmpty())
    }

    @Test
    fun everyEligibilityLossCancelsAllTasksAndNeverResumesAutomatically() {
        val cancellations: List<(HeartRateForegroundReconnectController) -> Unit> = listOf(
            { it.setDisplayEnabled(false) },
            { it.setPermissionGranted(false) },
            { it.setBluetoothEnabled(false) },
            { it.setForeground(false) },
            { it.userStop() },
            { it.clearTarget() },
            { it.close() }
        )
        cancellations.forEach { cancel ->
            val fixture = Fixture()
            val attempt = fixture.liveManualAttempt()
            fixture.controller.disconnected(attempt.targetGeneration, attempt.attemptGeneration)
            assertTrue(fixture.scheduler.has(HeartRateScheduledTask.RETRY))
            cancel(fixture.controller)
            assertTrue(fixture.scheduler.isEmpty())
            val count = fixture.connects.size
            fixture.controller.setForeground(true)
            fixture.controller.setDisplayEnabled(true)
            fixture.controller.setPermissionGranted(true)
            fixture.controller.setBluetoothEnabled(true)
            fixture.advance(60_000)
            assertEquals(count, fixture.connects.size)
        }
    }

    @Test
    fun disconnectedPendingRetryIsCancelledByManualScanButLiveScanIsIsolated() {
        val fixture = Fixture()
        val live = fixture.liveManualAttempt()
        fixture.controller.onManualScanStarted()
        assertEquals(HeartRateReconnectRuntimeKind.LIVE, fixture.state.kind)
        assertEquals(88, fixture.state.bpm)
        fixture.controller.onManualScanEnded()

        fixture.controller.disconnected(live.targetGeneration, live.attemptGeneration)
        assertTrue(fixture.scheduler.has(HeartRateScheduledTask.RETRY))
        fixture.controller.onManualScanStarted()
        assertFalse(fixture.scheduler.has(HeartRateScheduledTask.RETRY))
        fixture.controller.onManualScanEnded()
        fixture.advance(60_000)
        assertEquals(1, fixture.connects.size)
    }

    @Test
    fun backgroundPermissionAndBluetoothRestorationRequireANewManualCycle() {
        val fixture = Fixture()
        fixture.liveManualAttempt()
        fixture.controller.setForeground(false)
        fixture.controller.setForeground(true)
        fixture.controller.setPermissionGranted(false)
        fixture.controller.setPermissionGranted(true)
        fixture.controller.setBluetoothEnabled(false)
        fixture.controller.setBluetoothEnabled(true)
        fixture.advance(60_000)
        assertEquals(1, fixture.connects.size)

        fixture.controller.beginManualTarget()
        assertEquals(2, fixture.connects.size)
    }

    @Test
    fun cancellationInvalidatesAttemptBeforeCloseEffectAndLateDisconnectIsIgnored() {
        val fixture = Fixture()
        val attempt = fixture.liveManualAttempt()
        fixture.effects.clear()
        fixture.controller.userStop()
        val close = fixture.effects.filterIsInstance<HeartRateReconnectEffect.CloseAttempt>().single()
        assertEquals(attempt.attemptGeneration, close.attemptGeneration)
        val count = fixture.connects.size
        fixture.controller.disconnected(attempt.targetGeneration, attempt.attemptGeneration)
        fixture.advance(60_000)
        assertEquals(count, fixture.connects.size)
    }

    @Test
    fun cancellationRemovesWatchdogFreshnessAndBackoffTasks() {
        val watchdog = Fixture()
        watchdog.manualAttempt()
        assertTrue(watchdog.scheduler.has(HeartRateScheduledTask.WATCHDOG))
        watchdog.controller.userStop()
        assertTrue(watchdog.scheduler.isEmpty())

        val freshness = Fixture()
        val waiting = freshness.manualAttempt()
        freshness.controller.notifyEnabled(waiting.targetGeneration, waiting.attemptGeneration)
        assertTrue(freshness.scheduler.has(HeartRateScheduledTask.FRESHNESS))
        freshness.controller.setForeground(false)
        assertTrue(freshness.scheduler.isEmpty())

        val backoff = Fixture()
        val live = backoff.liveManualAttempt()
        backoff.controller.disconnected(live.targetGeneration, live.attemptGeneration)
        assertTrue(backoff.scheduler.has(HeartRateScheduledTask.RETRY))
        backoff.controller.onManualScanStarted()
        assertTrue(backoff.scheduler.isEmpty())
    }

    private class Fixture {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val effects = mutableListOf<HeartRateReconnectEffect>()
        val controller = HeartRateForegroundReconnectController(clock, scheduler, effects::add)
        val connects: List<HeartRateReconnectEffect.ConnectDirect>
            get() = effects.filterIsInstance<HeartRateReconnectEffect.ConnectDirect>()
        val state: HeartRateReconnectRuntimeState
            get() = controller.currentState()

        fun manualAttempt(): HeartRateReconnectEffect.ConnectDirect {
            controller.beginManualTarget()
            return latestConnect()
        }

        fun liveManualAttempt(): HeartRateReconnectEffect.ConnectDirect {
            val attempt = manualAttempt()
            controller.notifyEnabled(attempt.targetGeneration, attempt.attemptGeneration)
            controller.validSample(attempt.targetGeneration, attempt.attemptGeneration, 88)
            return attempt
        }

        fun latestConnect(): HeartRateReconnectEffect.ConnectDirect = connects.last()
        fun advance(deltaMs: Long) = scheduler.advanceBy(deltaMs)
    }

    private class FakeClock(var nowMs: Long = 0L) : HeartRateMonotonicClock {
        override fun elapsedRealtimeMs(): Long = nowMs
    }

    private class FakeScheduler(private val clock: FakeClock) : HeartRateControllerScheduler {
        private data class Entry(val atMs: Long, val action: () -> Unit)
        private val entries = EnumMap<HeartRateScheduledTask, Entry>(HeartRateScheduledTask::class.java)

        override fun schedule(task: HeartRateScheduledTask, delayMs: Long, action: () -> Unit) {
            entries[task] = Entry(clock.nowMs + delayMs, action)
        }

        override fun cancel(task: HeartRateScheduledTask) {
            entries.remove(task)
        }

        override fun cancelAll() {
            entries.clear()
        }

        fun has(task: HeartRateScheduledTask): Boolean = entries.containsKey(task)
        fun isEmpty(): Boolean = entries.isEmpty()
        fun delayFor(task: HeartRateScheduledTask): Long = entries.getValue(task).atMs - clock.nowMs

        fun advanceBy(deltaMs: Long) {
            val target = clock.nowMs + deltaMs
            while (true) {
                val next = entries.entries.minByOrNull { it.value.atMs } ?: break
                val task = next.key
                val entry = next.value
                if (entry.atMs > target) break
                entries.remove(task)
                clock.nowMs = entry.atMs
                entry.action()
            }
            clock.nowMs = target
        }
    }
}
