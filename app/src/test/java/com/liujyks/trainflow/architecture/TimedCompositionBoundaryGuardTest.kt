package com.liujyks.trainflow.architecture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.text.Charsets
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedCompositionBoundaryGuardTest {
    @Test
    fun compositionV2TermsStayOutOfExecutionTimerDialAndComposeRouteSources() {
        val sourceRoot = sourceRoot()
        val scannedPaths = listOf(
            sourceRoot.resolve("core/engine"),
            sourceRoot.resolve("feature/workoutsession"),
            sourceRoot.resolve("feature/plans/TimedPlanEditorRoute.kt")
        )
        val blockedTerms = listOf(
            "TimedComposition",
            "compositionVersion",
            "stageGroups",
            "timed_composition",
            "composition_v2"
        )
        val violations = scannedPaths.flatMap { path ->
            val files = if (Files.isDirectory(path)) {
                Files.walk(path).filter { file -> file.toString().endsWith(".kt") }.toList()
            } else {
                listOf(path)
            }
            files.flatMap { file -> blockedTerms.violationsIn(file) }
        }

        assertTrue(
            "Timed composition v2 terms must not enter execution, TimerDial, or Compose route sources:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    private fun List<String>.violationsIn(file: Path): List<String> {
        if (!Files.exists(file)) return emptyList()
        val text = Files.readAllBytes(file).toString(Charsets.UTF_8)
        return filter { term -> term in text }
            .map { term -> "$file contains $term" }
    }

    private fun sourceRoot(): Path {
        val current = Paths.get("").toAbsolutePath()
        val appModuleRoot = if (current.fileName.toString() == "app") {
            current
        } else {
            current.resolve("app")
        }
        return appModuleRoot.resolve("src/main/java/com/liujyks/trainflow")
    }
}
