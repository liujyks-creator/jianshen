package com.liujyks.trainflow.core.database

import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CanonicalEntityContractTest {
    @Test
    fun heartRateSampleContainsOnlyCanonicalObservationFields() {
        val fieldNames = HeartRateSampleEntity::class.java.declaredFields
            .filterNot { field -> field.isSynthetic || field.name.startsWith("$") }
            .map { field -> field.name }
            .toSet()

        assertEquals(
            setOf("recordingId", "sampleSequence", "offsetMs", "mutationSequence", "bpm"),
            fieldNames
        )
        FORBIDDEN_SAMPLE_PROVENANCE_FIELDS.forEach { forbidden ->
            assertFalse("Forbidden per-sample provenance field: $forbidden", forbidden in fieldNames)
        }
    }

    private companion object {
        val FORBIDDEN_SAMPLE_PROVENANCE_FIELDS = setOf(
            "sourceKind",
            "sourceLabel",
            "sourceId",
            "deviceId",
            "deviceName",
            "deviceAddress",
            "phaseId",
            "measuredAt",
            "recordedAt",
            "wallTimestamp",
            "gattCode",
            "diagnostic"
        )
    }
}
