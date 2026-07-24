package com.liujyks.trainflow.core.health

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidBleHeartRateProviderScanFilterTest {
    @Test
    fun legacyProviderAndScannerOwnershipEntrypointsAreRetired() {
        assertFalse(
            File("src/main/java/com/liujyks/trainflow/core/health/AndroidBleHeartRateProvider.kt")
                .exists()
        )
        assertFalse(
            File("src/main/java/com/liujyks/trainflow/core/health/HeartRateDeviceScanner.kt")
                .exists()
        )
    }

    @Test
    fun productionCompositionHasExactlyOneRuntimeOwnerCreation() {
        val sources = File("src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        val creationPattern = Regex("""HeartRateRuntimeOwner\s*\(""")
        val creationLines = sources.flatMap { source ->
            source.readLines().mapIndexedNotNull { index, line ->
                "$source:${index + 1}:$line".takeIf {
                    creationPattern.containsMatchIn(line) &&
                        !line.contains("class HeartRateRuntimeOwner")
                }
            }
        }

        assertEquals(1, creationLines.size)
        assertTrue(creationLines.single().contains("TrainFlowApplication.kt"))
    }
}
