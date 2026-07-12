package com.liujyks.trainflow.core.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BlePlatformCallBoundaryTest {
    @Test
    fun everyTypedBleOperationContainsPermissionToctou() {
        BlePlatformOperation.entries.forEach { operation ->
            val exception = SecurityException(operation.name)
            val result = BlePlatformExceptionClassifier.securityOnly(operation) { throw exception }
            assertTrue(result is BlePlatformCallResult.ExpectedFailure)
            result as BlePlatformCallResult.ExpectedFailure
            assertEquals(operation, result.operation)
            assertSame(exception, result.exception)
        }
    }

    @Test
    fun onlyScannerApiStateChecksContainIllegalStateRace() {
        listOf(BlePlatformOperation.START_SCAN, BlePlatformOperation.STOP_SCAN).forEach { operation ->
            val exception = IllegalStateException("adapter changed while scanner API entered")
            val result = BlePlatformExceptionClassifier.scannerStateRace(operation) { throw exception }
            assertTrue(result is BlePlatformCallResult.ExpectedFailure)
            assertSame(exception, (result as BlePlatformCallResult.ExpectedFailure).exception)
        }
    }

    @Test
    fun nonBluetoothIllegalStateAndUnknownRuntimePropagate() {
        val illegalState = IllegalStateException("provider mapping invariant")
        val stateThrown = runCatching {
            BlePlatformExceptionClassifier.securityOnly(BlePlatformOperation.DISCOVER_SERVICES) { throw illegalState }
        }.exceptionOrNull()
        assertSame(illegalState, stateThrown)

        val runtime = UnsupportedOperationException("unexpected platform implementation")
        val runtimeThrown = runCatching {
            BlePlatformExceptionClassifier.scannerStateRace(BlePlatformOperation.START_SCAN) { throw runtime }
        }.exceptionOrNull()
        assertSame(runtime, runtimeThrown)
    }

    @Test
    fun descriptorMutationIsOutsideExpectedFailureBoundary() {
        val mutationError = IllegalStateException("descriptor mutation bug")
        val descriptor = object : BleGattDescriptor {
            override val characteristicUuid = java.util.UUID.randomUUID()
            override fun setLegacyValue(value: ByteArray) { throw mutationError }
        }

        val thrown = runCatching { descriptor.setLegacyValue(byteArrayOf(1, 0)) }.exceptionOrNull()

        assertSame(mutationError, thrown)
    }
}
