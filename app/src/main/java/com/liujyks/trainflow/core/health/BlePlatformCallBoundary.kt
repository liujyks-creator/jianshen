package com.liujyks.trainflow.core.health

internal enum class BlePlatformOperation {
    READ_ADAPTER_ENABLED,
    READ_SCANNER,
    START_SCAN,
    STOP_SCAN,
    READ_BONDED_DEVICES,
    READ_DEVICE_IDENTIFIER,
    READ_DEVICE_NAME,
    CONNECT_GATT,
    DISCOVER_SERVICES,
    READ_GATT_SERVICE,
    CONFIGURE_NOTIFICATION,
    WRITE_DESCRIPTOR,
    DISCONNECT_GATT,
    CLOSE_GATT
}

internal sealed interface BlePlatformCallResult<out T> {
    data class Success<T>(val value: T) : BlePlatformCallResult<T>
    data class ExpectedFailure(
        val operation: BlePlatformOperation,
        val exception: RuntimeException
    ) : BlePlatformCallResult<Nothing>
}

/** Narrow boundary for permission/Bluetooth-state TOCTOU failures from Android BLE APIs. */
internal interface BlePlatformCallBoundary {
    fun <T> call(operation: BlePlatformOperation, block: () -> T): BlePlatformCallResult<T>
}

internal object AndroidBlePlatformCallBoundary : BlePlatformCallBoundary {
    override fun <T> call(operation: BlePlatformOperation, block: () -> T): BlePlatformCallResult<T> =
        try {
            BlePlatformCallResult.Success(block())
        } catch (exception: SecurityException) {
            BlePlatformCallResult.ExpectedFailure(operation, exception)
        } catch (exception: IllegalStateException) {
            BlePlatformCallResult.ExpectedFailure(operation, exception)
        }
}

internal fun BlePlatformOperation.technicalFailureReason(): HeartRateFreshnessReason = when (this) {
    BlePlatformOperation.DISCOVER_SERVICES,
    BlePlatformOperation.READ_GATT_SERVICE -> HeartRateFreshnessReason.SERVICE_DISCOVERY_FAILED
    BlePlatformOperation.CONFIGURE_NOTIFICATION,
    BlePlatformOperation.WRITE_DESCRIPTOR -> HeartRateFreshnessReason.CCCD_FAILED
    else -> HeartRateFreshnessReason.CONNECT_FAILED
}
