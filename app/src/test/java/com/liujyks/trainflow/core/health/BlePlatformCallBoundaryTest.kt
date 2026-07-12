package com.liujyks.trainflow.core.health

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BlePlatformCallBoundaryTest {
    @Test
    fun productionBoundaryContainsSecurityExceptionForEverySensitiveOperation() {
        BlePlatformOperation.entries.forEach { operation ->
            val exception = SecurityException(operation.name)

            val result = AndroidBlePlatformCallBoundary.call(operation) { throw exception }

            assertTrue(result is BlePlatformCallResult.ExpectedFailure)
            result as BlePlatformCallResult.ExpectedFailure
            assertEquals(operation, result.operation)
            assertSame(exception, result.exception)
        }
    }

    @Test
    fun productionBoundaryContainsBluetoothStateRaceButDoesNotHideProgrammingErrors() {
        val stateRace = IllegalStateException("Bluetooth turned off")
        val contained = AndroidBlePlatformCallBoundary.call(BlePlatformOperation.DISCOVER_SERVICES) {
            throw stateRace
        }
        assertTrue(contained is BlePlatformCallResult.ExpectedFailure)

        val programmingError = IllegalArgumentException("wrong descriptor")
        val thrown = runCatching {
            AndroidBlePlatformCallBoundary.call(BlePlatformOperation.WRITE_DESCRIPTOR) {
                throw programmingError
            }
        }.exceptionOrNull()
        assertSame(programmingError, thrown)
    }

    @Test
    fun connectionStageOperationsMapToStableTechnicalFactsNeverGattDisconnect() {
        assertEquals(
            HeartRateFreshnessReason.CONNECT_FAILED,
            BlePlatformOperation.CONNECT_GATT.technicalFailureReason()
        )
        assertEquals(
            HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED,
            BlePlatformOperation.DISCOVER_SERVICES.technicalFailureReason()
        )
        assertEquals(
            HeartRateFreshnessReason.CCCD_FAILED,
            BlePlatformOperation.CONFIGURE_NOTIFICATION.technicalFailureReason()
        )
        assertEquals(
            HeartRateFreshnessReason.CCCD_FAILED,
            BlePlatformOperation.WRITE_DESCRIPTOR.technicalFailureReason()
        )
    }

    @Test
    fun productionProviderConsumesBoundaryAcrossScanGattAvailabilityIdentityAndCleanupPaths() {
        val source = File("src/main/java/com/liujyks/trainflow/core/health/AndroidBleHeartRateProvider.kt").readText()
        val requiredOperations = listOf(
            "READ_ADAPTER_ENABLED",
            "READ_SCANNER",
            "START_SCAN",
            "STOP_SCAN",
            "READ_BONDED_DEVICES",
            "READ_DEVICE_IDENTIFIER",
            "READ_DEVICE_NAME",
            "CONNECT_GATT",
            "DISCOVER_SERVICES",
            "READ_GATT_SERVICE",
            "CONFIGURE_NOTIFICATION",
            "WRITE_DESCRIPTOR",
            "DISCONNECT_GATT",
            "CLOSE_GATT"
        )

        assertTrue(source.contains("private val platformCalls: BlePlatformCallBoundary"))
        requiredOperations.forEach { operation ->
            assertTrue("provider must consume $operation", source.contains("BlePlatformOperation.$operation"))
        }
    }
}
