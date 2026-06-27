package com.liujyks.trainflow.architecture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.text.Charsets
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedCompositionBoundaryGuardTest {
    @Test
    fun compositionV2TermsStayLimitedToModelEditorAndMinimumEngineBridgeSources() {
        val sourceRoot = sourceRoot()
        val scannedPaths = listOf(
            sourceRoot.resolve("core/engine"),
            sourceRoot.resolve("feature/workoutsession"),
            sourceRoot.resolve("feature/timer")
        )
        val blockedTerms = listOf(
            "TimedComposition",
            "compositionVersion",
            "stageGroups",
            "timed_composition",
            "composition_v2"
        )
        val allowedRelativePaths = setOf("core/engine/TimedWorkoutEngine.kt")
        val violations = scannedPaths.flatMap { path ->
            path.kotlinFiles().flatMap { file ->
                if (file.relativeToSourceRoot(sourceRoot) in allowedRelativePaths) {
                    emptyList()
                } else {
                    blockedTerms.violationsIn(file)
                }
            }
        }

        assertTrue(
            "Timed composition v2 terms must stay out of unintended execution or TimerDial sources:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun timelineAdapterTermsStayLimitedToAdapterAndMinimumEngineBridgeSources() {
        val sourceRoot = sourceRoot()
        val scannedPaths = listOf(
            sourceRoot.resolve("core/engine"),
            sourceRoot.resolve("feature/workoutsession"),
            sourceRoot.resolve("feature/timer"),
            sourceRoot.resolve("feature/plans/TimedPlanEditorRoute.kt"),
            sourceRoot.resolve("feature/plans/PlanManagementRoute.kt"),
            sourceRoot.resolve("feature/plans/PlanManagementUiState.kt")
        )
        val blockedTerms = listOf(
            "TimedCompositionTimeline",
            "TimedCompositionTimelineAdapter",
            "timelineStageId",
            "targetInstanceIndex"
        )
        val allowedRelativePaths = setOf("core/engine/TimedWorkoutEngine.kt")
        val violations = scannedPaths.flatMap { path ->
            path.kotlinFiles().flatMap { file ->
                if (file.relativeToSourceRoot(sourceRoot) in allowedRelativePaths) {
                    emptyList()
                } else {
                    blockedTerms.violationsIn(file)
                }
            }
        }

        assertTrue(
            "Timeline adapter terms must stay out of unintended engine, TimerDial, or route sources:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun timelineAdapterTermsInTestsStayInAdapterAndBridgeExpectationSurfaces() {
        val testRoot = testRoot()
        val allowedRelativePaths = setOf(
            "architecture/TimedCompositionBoundaryGuardTest.kt",
            "core/model/TimedCompositionTimelineAdapterTest.kt",
            "core/engine/TimedCompositionEngineBridgeTest.kt",
            "feature/workoutsession/TimedCompositionSessionRecordCompatibilityTest.kt"
        )
        val guardedTerms = listOf(
            "TimedCompositionTimeline",
            "TimedCompositionTimelineAdapter",
            "timelineStageId",
            "targetInstanceIndex"
        )
        val violations = testRoot.kotlinFiles().flatMap { file ->
            val relativePath = testRoot.relativize(file).toString().replace('\\', '/')
            if (relativePath in allowedRelativePaths) {
                emptyList()
            } else {
                guardedTerms.violationsIn(file)
            }
        }

        assertTrue(
            "Timeline adapter terms in tests must stay limited to adapter tests, boundary guard, " +
                "bridge expectation, and session record compatibility test files:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun workoutCommandAndEventDoNotGrowCompositionBridgePayload() {
        val sourceRoot = sourceRoot()
        val scannedPaths = listOf(
            sourceRoot.resolve("core/model/WorkoutCommand.kt"),
            sourceRoot.resolve("core/model/WorkoutEvent.kt")
        )
        val blockedTerms = listOf(
            "TimedComposition",
            "compositionVersion",
            "timelineStageId",
            "targetInstanceIndex",
            "stageGroupId",
            "targetKind"
        )
        val violations = scannedPaths.flatMap { path ->
            path.kotlinFiles().flatMap { file -> blockedTerms.violationsIn(file) }
        }

        assertTrue(
            "WorkoutCommand and WorkoutEvent must not carry v2 bridge payload in this gate:\n" +
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

    private fun Path.kotlinFiles(): List<Path> {
        if (!Files.exists(this)) return emptyList()
        return if (Files.isDirectory(this)) {
            Files.walk(this).filter { file -> file.toString().endsWith(".kt") }.toList()
        } else {
            listOf(this)
        }
    }

    private fun Path.relativeToSourceRoot(sourceRoot: Path): String {
        return sourceRoot.relativize(this).toString().replace('\\', '/')
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

    private fun testRoot(): Path {
        val current = Paths.get("").toAbsolutePath()
        val appModuleRoot = if (current.fileName.toString() == "app") {
            current
        } else {
            current.resolve("app")
        }
        return appModuleRoot.resolve("src/test/java/com/liujyks/trainflow")
    }
}
