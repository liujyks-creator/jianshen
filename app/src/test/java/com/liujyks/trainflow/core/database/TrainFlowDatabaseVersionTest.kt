package com.liujyks.trainflow.core.database

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainFlowDatabaseVersionTest {
    @Test
    fun canonicalTimelineFoundationUsesRoomVersionFiveAndRegistersMigration() {
        val current = Paths.get("").toAbsolutePath()
        val appModuleRoot = if (current.fileName.toString() == "app") current else current.resolve("app")
        val sourcePath = appModuleRoot.resolve("src/main/java/com/liujyks/trainflow/core/database/TrainFlowDatabase.kt")
        val source = Files.readAllBytes(sourcePath).toString(Charsets.UTF_8)

        assertTrue("Canonical timeline storage requires Room version 5.", "version = 5" in source)
        assertTrue(
            "The existing migration chain must register MIGRATION_4_5.",
            "MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5" in source
        )
    }
}
