package com.liujyks.trainflow.app

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class E17HeartRateEvidenceFormatterTest {
    @Test
    fun formatsStandardUuidsAndCharacteristicProperties() {
        assertEquals(
            "0x180D",
            E17HrsEvidenceFormatter.uuid(
                UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
            )
        )
        assertEquals(
            "0x30 modes=[notify, indicate]",
            E17HrsEvidenceFormatter.characteristicProperties(0x30)
        )
    }

    @Test
    fun notifyLineKeepsSourceRawPayloadAndParserResultTogether() {
        val line = E17HrsEvidenceFormatter.notifyLine(
            sourceLabel = "HUAWEI Band HR-OD7",
            sourceIdentifier = "D8:F0:42:01:90:D7",
            rawPayload = byteArrayOf(0x06, 0x5A),
            parsedBpm = 90,
            flags = 0x06,
            format = "uint8"
        )

        assertTrue(line.contains("source_label=\"HUAWEI Band HR-OD7\""))
        assertTrue(line.contains("source_identifier=\"D8:F0:42:01:90:D7\""))
        assertTrue(line.contains("raw=\"06 5A\""))
        assertTrue(line.contains("parsed_bpm=90"))
        assertTrue(line.contains("flags=0x06 format=uint8"))
    }
}
