package com.liujyks.trainflow.core.database

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainFlowDatabaseVersionTest {
    @Test
    fun timedCompositionFoundationDoesNotChangeRoomSchemaVersion() {
        val current = Paths.get("").toAbsolutePath()
        val appModuleRoot = if (current.fileName.toString() == "app") current else current.resolve("app")
        val sourcePath = appModuleRoot.resolve("src/main/java/com/liujyks/trainflow/core/database/TrainFlowDatabase.kt")
        val source = Files.readAllBytes(sourcePath).toString(Charsets.UTF_8)

        assertTrue("Room database version must remain 4.", "version = 4" in source)
    }
}
