package com.liujyks.trainflow.architecture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageBoundaryTest {
    @Test
    fun requiredBoundaryPackagesExist() {
        val sourceRoot = sourceRoot()
        val requiredPackages = listOf(
            "app",
            "core/model",
            "core/data",
            "core/database",
            "core/datastore",
            "core/domain",
            "core/engine",
            "core/health",
            "core/media",
            "core/notifications",
            "feature/home",
            "feature/plans",
            "feature/exerciselibrary",
            "feature/workoutsession",
            "feature/history",
            "feature/recovery",
            "feature/settings",
            "platform/timer",
            "platform/voice",
            "ui/designsystem",
            "ui/theme",
            "ui/shell/official"
        )

        val missing = requiredPackages.filterNot { packagePath ->
            Files.exists(sourceRoot.resolve(packagePath))
        }

        assertTrue(
            "Missing E0.2 boundary package directories: ${missing.joinToString()}",
            missing.isEmpty()
        )
    }

    @Test
    fun forbiddenImportsDoNotCrossBoundaries() {
        val sourceRoot = sourceRoot()
        val kotlinFiles = Files.walk(sourceRoot)
            .filter { path -> path.toString().endsWith(".kt") }
            .toList()

        val violations = kotlinFiles.flatMap { file ->
            val source = SourceFile.read(file)
            forbiddenRules.flatMap { rule -> rule.violationsIn(source) }
        }

        assertTrue(
            "Forbidden package boundary imports found:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
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

    private data class SourceFile(
        val path: Path,
        val packageName: String,
        val imports: List<String>
    ) {
        companion object {
            fun read(path: Path): SourceFile {
                val lines = Files.readAllLines(path)
                val packageName = lines.firstOrNull { it.startsWith("package ") }
                    ?.removePrefix("package ")
                    ?.trim()
                    .orEmpty()
                val imports = lines.asSequence()
                    .filter { it.startsWith("import ") }
                    .map { it.removePrefix("import ").substringBefore(" as ").trim() }
                    .toList()

                return SourceFile(path = path, packageName = packageName, imports = imports)
            }
        }
    }

    private data class ForbiddenImportRule(
        val sourcePackagePrefix: String,
        val forbiddenImportPrefixes: List<String>,
        val reason: String
    ) {
        fun violationsIn(source: SourceFile): List<String> {
            if (!source.packageName.startsWith(sourcePackagePrefix)) {
                return emptyList()
            }

            return source.imports.flatMap { imported ->
                forbiddenImportPrefixes
                    .filter { forbiddenPrefix -> imported.startsWith(forbiddenPrefix) }
                    .map { forbiddenPrefix ->
                        "${source.path}: ${source.packageName} imports $imported ($reason; $forbiddenPrefix)"
                    }
            }
        }
    }

    private companion object {
        private const val ROOT = "com.liujyks.trainflow"

        private val forbiddenRules = listOf(
            ForbiddenImportRule(
                sourcePackagePrefix = "$ROOT.core.model",
                forbiddenImportPrefixes = listOf(
                    "android.",
                    "androidx.",
                    "$ROOT.core.database",
                    "$ROOT.core.datastore",
                    "$ROOT.feature",
                    "$ROOT.platform",
                    "$ROOT.ui"
                ),
                reason = "core model must stay portable and free of Android/UI/storage concerns"
            ),
            ForbiddenImportRule(
                sourcePackagePrefix = "$ROOT.core.domain",
                forbiddenImportPrefixes = listOf(
                    "androidx.compose",
                    "androidx.room",
                    "$ROOT.core.database",
                    "$ROOT.core.datastore",
                    "$ROOT.feature",
                    "$ROOT.platform",
                    "$ROOT.ui"
                ),
                reason = "domain rules must not depend on UI, storage implementation, or platform adapters"
            ),
            ForbiddenImportRule(
                sourcePackagePrefix = "$ROOT.core.engine",
                forbiddenImportPrefixes = listOf(
                    "androidx.compose",
                    "androidx.room",
                    "$ROOT.core.database",
                    "$ROOT.core.datastore",
                    "$ROOT.core.health",
                    "$ROOT.core.media",
                    "$ROOT.core.notifications",
                    "$ROOT.feature",
                    "$ROOT.platform",
                    "$ROOT.ui"
                ),
                reason = "workout engine must own state transitions without UI, storage, or platform adapters"
            ),
            ForbiddenImportRule(
                sourcePackagePrefix = "$ROOT.feature",
                forbiddenImportPrefixes = listOf(
                    "androidx.room",
                    "$ROOT.core.database",
                    "$ROOT.core.datastore",
                    "$ROOT.platform"
                ),
                reason = "feature UI must use domain/data boundaries instead of Room/DataStore/platform internals"
            ),
            ForbiddenImportRule(
                sourcePackagePrefix = "$ROOT.ui",
                forbiddenImportPrefixes = listOf(
                    "androidx.room",
                    "$ROOT.core.database",
                    "$ROOT.core.datastore",
                    "$ROOT.platform"
                ),
                reason = "UI shell and design packages must not bind directly to persistence or platform adapters"
            ),
            ForbiddenImportRule(
                sourcePackagePrefix = "$ROOT.platform",
                forbiddenImportPrefixes = listOf(
                    "androidx.compose",
                    "$ROOT.feature",
                    "$ROOT.ui"
                ),
                reason = "platform adapters must not depend on feature UI or Compose surfaces"
            ),
            ForbiddenImportRule(
                sourcePackagePrefix = "$ROOT.core.health",
                forbiddenImportPrefixes = listOf(
                    "$ROOT.feature",
                    "$ROOT.ui"
                ),
                reason = "heart-rate providers must expose abstract state without depending on UI"
            ),
            ForbiddenImportRule(
                sourcePackagePrefix = "$ROOT.core.media",
                forbiddenImportPrefixes = listOf(
                    "$ROOT.feature",
                    "$ROOT.ui"
                ),
                reason = "media consumers must react to workout events without depending on feature UI"
            ),
            ForbiddenImportRule(
                sourcePackagePrefix = "$ROOT.core.notifications",
                forbiddenImportPrefixes = listOf(
                    "$ROOT.feature",
                    "$ROOT.ui"
                ),
                reason = "notification adapters must not reverse-depend on feature UI"
            )
        )
    }
}
