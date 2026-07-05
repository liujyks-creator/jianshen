package com.liujyks.trainflow.core.health

internal object HeartRateMeasurementParser {
    fun parse(payload: ByteArray): HeartRateMeasurement? {
        if (payload.isEmpty()) return null

        val flags = HeartRateMeasurementFlags.from(payload[0].toInt() and 0xFF)
        val bpmByteCount = when (flags.bpmFormat) {
            HeartRateBpmFormat.UINT8 -> 1
            HeartRateBpmFormat.UINT16 -> 2
        }
        if (payload.size < 1 + bpmByteCount) return null

        val bpm = when (flags.bpmFormat) {
            HeartRateBpmFormat.UINT8 -> payload[1].toInt() and 0xFF
            HeartRateBpmFormat.UINT16 ->
                (payload[1].toInt() and 0xFF) or ((payload[2].toInt() and 0xFF) shl 8)
        }

        return HeartRateMeasurement(flags = flags, bpm = bpm)
    }
}

internal data class HeartRateMeasurement(
    val flags: HeartRateMeasurementFlags,
    val bpm: Int
)

internal data class HeartRateMeasurementFlags(
    val raw: Int,
    val bpmFormat: HeartRateBpmFormat,
    val sensorContactStatus: HeartRateSensorContactStatus,
    val energyExpendedPresent: Boolean,
    val rrIntervalPresent: Boolean
) {
    companion object {
        fun from(raw: Int): HeartRateMeasurementFlags {
            val sensorContactBits = (raw shr 1) and 0x03
            return HeartRateMeasurementFlags(
                raw = raw and 0xFF,
                bpmFormat = if (raw and 0x01 == 0) {
                    HeartRateBpmFormat.UINT8
                } else {
                    HeartRateBpmFormat.UINT16
                },
                sensorContactStatus = when (sensorContactBits) {
                    0x02 -> HeartRateSensorContactStatus.SUPPORTED_NOT_DETECTED
                    0x03 -> HeartRateSensorContactStatus.SUPPORTED_DETECTED
                    else -> HeartRateSensorContactStatus.NOT_SUPPORTED_OR_UNKNOWN
                },
                energyExpendedPresent = raw and 0x08 != 0,
                rrIntervalPresent = raw and 0x10 != 0
            )
        }
    }
}

internal enum class HeartRateBpmFormat {
    UINT8,
    UINT16
}

internal enum class HeartRateSensorContactStatus {
    NOT_SUPPORTED_OR_UNKNOWN,
    SUPPORTED_NOT_DETECTED,
    SUPPORTED_DETECTED
}
