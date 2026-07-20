package com.liujyks.trainflow.core.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRateMeasurementParserTest {
    @Test
    fun parsesEightBitHeartRateMeasurement() {
        val measurement = HeartRateMeasurementParser.parse(byteArrayOf(0x00, 0x5A))

        requireNotNull(measurement)
        assertEquals(HeartRateBpmFormat.UINT8, measurement.flags.bpmFormat)
        assertEquals(90, measurement.bpm)
        assertEquals(0x00, measurement.flags.raw)
        assertEquals(
            HeartRateSensorContactStatus.NOT_SUPPORTED_OR_UNKNOWN,
            measurement.flags.sensorContactStatus
        )
    }

    @Test
    fun parsesSixteenBitHeartRateMeasurementLittleEndian() {
        val measurement = HeartRateMeasurementParser.parse(byteArrayOf(0x01, 0x2C, 0x01))

        requireNotNull(measurement)
        assertEquals(HeartRateBpmFormat.UINT16, measurement.flags.bpmFormat)
        assertEquals(300, measurement.bpm)
    }

    @Test
    fun parsesStandardFlagsWithoutLeakingBleModels() {
        val measurement = HeartRateMeasurementParser.parse(byteArrayOf(0x1F, 0x78, 0x00))

        requireNotNull(measurement)
        assertEquals(0x1F, measurement.flags.raw)
        assertEquals(HeartRateBpmFormat.UINT16, measurement.flags.bpmFormat)
        assertEquals(
            HeartRateSensorContactStatus.SUPPORTED_DETECTED,
            measurement.flags.sensorContactStatus
        )
        assertEquals(true, measurement.flags.energyExpendedPresent)
        assertEquals(true, measurement.flags.rrIntervalPresent)
    }

    @Test
    fun safelyRejectsEmptyAndTruncatedPayloads() {
        assertNull(HeartRateMeasurementParser.parse(byteArrayOf()))
        assertNull(HeartRateMeasurementParser.parse(byteArrayOf(0x00)))
        assertNull(HeartRateMeasurementParser.parse(byteArrayOf(0x01, 0x2C)))
    }

    @Test
    fun parsesRepresentativeBand9PayloadWithoutVendorModels() {
        val measurement = HeartRateMeasurementParser.parse(byteArrayOf(0x06, 0x54))

        requireNotNull(measurement)
        assertEquals(84, measurement.bpm)
        assertEquals(0x06, measurement.flags.raw)
        assertEquals(HeartRateBpmFormat.UINT8, measurement.flags.bpmFormat)
        assertEquals(
            HeartRateSensorContactStatus.SUPPORTED_DETECTED,
            measurement.flags.sensorContactStatus
        )
    }

    @Test
    fun structurallyValidZeroBpmRemainsParserOutputForFactLayerValidation() {
        val measurement = HeartRateMeasurementParser.parse(byteArrayOf(0x00, 0x00))

        requireNotNull(measurement)
        assertEquals(0, measurement.bpm)
    }
}
