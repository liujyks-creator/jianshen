package com.liujyks.trainflow.core.health

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidBleHeartRateProviderCallbackAdapterTest {
    @Test
    fun lifecycleCancellationRejectsQueuedReceiverAvailabilityGattAndSinkWork() {
        val gate = HeartRateProviderCallbackGate()
        val queued = gate.lifecycleToken()

        gate.invalidateLifecycle()

        assertFalse(gate.accepts(queued))
        assertTrue(gate.accepts(gate.lifecycleToken()))
    }

    @Test
    fun scanStopRejectsQueuedResultBatchFailureAndTimeoutWithoutClosingProvider() {
        val gate = HeartRateProviderCallbackGate()
        val scan = gate.beginScan()

        gate.invalidateScan()

        assertFalse(gate.accepts(scan))
        assertTrue(gate.isOpen())
        assertTrue(gate.accepts(gate.lifecycleToken()))
    }

    @Test
    fun closeRejectsEveryQueuedCallbackAndIsIdempotent() {
        val gate = HeartRateProviderCallbackGate()
        val lifecycle = gate.lifecycleToken()
        val scan = gate.beginScan()

        gate.close()
        gate.close()

        assertFalse(gate.isOpen())
        assertFalse(gate.accepts(lifecycle))
        assertFalse(gate.accepts(scan))
        assertFalse(gate.accepts(gate.lifecycleToken()))
    }
}
