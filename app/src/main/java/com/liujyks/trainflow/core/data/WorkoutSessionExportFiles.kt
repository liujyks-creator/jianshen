package com.liujyks.trainflow.core.data

import android.system.Os
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal data class PreparedWorkoutSessionExport(
    val operationId: String,
    val file: File,
    val includedSessionIds: List<String>,
    val generatedAt: Instant
)

internal data class WorkoutSessionExportCleanupFailure(val file: File, val cause: Throwable)

internal data class WorkoutSessionExportCleanupResult(
    val deletedFiles: List<File>,
    val failures: List<WorkoutSessionExportCleanupFailure>
)

/** Owns only the private export files; the caller owns the generation Job. */
internal class WorkoutSessionExportFiles(
    filesDir: File,
    private val repository: WorkoutSessionRepository,
    applicationScope: CoroutineScope,
    startupCleanupAt: Instant
) {
    private val root = File(filesDir, "session-exports")
    private val incomplete = File(root, "incomplete")
    private val ready = File(root, "ready")
    private val shared = File(root, "shared")

    val startupCleanup: Deferred<WorkoutSessionExportCleanupResult> =
        applicationScope.async(Dispatchers.IO) { cleanup(startupCleanupAt, manually = false) }

    suspend fun prepare(
        selection: WorkoutSessionExportSelection,
        generatedAt: Instant,
        displayLocale: String,
        onProcessed: (Int, Int) -> Unit
    ): PreparedWorkoutSessionExport {
        val frozen = selection.copy(
            modeFilter = selection.modeFilter?.toSet(),
            planFilter = selection.planFilter?.toSet(),
            includedSessionIds = selection.includedSessionIds.toList(),
            includedUnknownDateSessionIds = selection.includedUnknownDateSessionIds.toList()
        )
        var ownedFile: File? = null
        try {
            return withContext(Dispatchers.IO) {
                startupCleanup.await()
                val report = cleanup(generatedAt, manually = false)
                val blocking = report.failures.filter { it.file != shared && it.file.parentFile != shared }
                report.failures.filter { it.file == shared || it.file.parentFile == shared }.forEach {
                    Log.w("SessionExport", "Could not clean ${it.file}", it.cause)
                }
                if (blocking.isNotEmpty()) {
                    val primary = blocking.first().cause
                    blocking.drop(1).forEach { primary.addSuppressed(it.cause) }
                    throw primary
                }
                ensureDirectory(root)
                ensureDirectory(incomplete)
                ensureDirectory(ready)
                currentCoroutineContext().ensureActive()
                val operationId = UUID.randomUUID().toString()
                val part = File(incomplete, "export-$operationId.json.part")
                if (!part.createNewFile()) throw FileAlreadyExistsException(part.path)
                ownedFile = part
                FileOutputStream(part).use { stream ->
                    stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        val encoder = WorkoutSessionExport(writer, frozen, generatedAt, displayLocale)
                        frozen.includedSessionIds.forEachIndexed { index, id ->
                            currentCoroutineContext().ensureActive()
                            val result = repository.readSessionStrict(id)
                            currentCoroutineContext().ensureActive()
                            encoder.writeSession(result)
                            currentCoroutineContext().ensureActive()
                            onProcessed(index + 1, frozen.includedSessionIds.size)
                        }
                        currentCoroutineContext().ensureActive()
                        encoder.finish()
                        currentCoroutineContext().ensureActive()
                        writer.flush()
                        currentCoroutineContext().ensureActive()
                        stream.fd.sync()
                    }
                }
                currentCoroutineContext().ensureActive()
                validateCompletedFile(part, frozen, generatedAt, displayLocale)
                currentCoroutineContext().ensureActive()
                val target = File(ready, "export-$operationId.json")
                requireAbsent(target)
                Os.rename(part.path, target.path)
                ownedFile = target
                currentCoroutineContext().ensureActive()
                PreparedWorkoutSessionExport(operationId, target, frozen.includedSessionIds, generatedAt)
            }
        } catch (primary: Throwable) {
            try {
                withContext(NonCancellable + Dispatchers.IO) {
                    ownedFile?.let { Files.delete(it.toPath()) }
                }
            } catch (secondary: Throwable) {
                primary.addSuppressed(secondary)
            }
            throw primary
        }
    }

    suspend fun markShareAttempt(
        export: PreparedWorkoutSessionExport,
        shareAttemptAt: Instant
    ): PreparedWorkoutSessionExport = withContext(Dispatchers.IO) {
        ensureDirectory(root)
        ensureDirectory(shared)
        val target = File(shared, "shared-${shareAttemptAt.toEpochMilli()}-${export.operationId}.json")
        requireAbsent(target)
        currentCoroutineContext().ensureActive()
        Os.rename(export.file.path, target.path)
        export.copy(file = target)
    }

    suspend fun deleteAfterSuccessfulSave(
        export: PreparedWorkoutSessionExport
    ): WorkoutSessionExportCleanupResult = withContext(Dispatchers.IO) { deleteCopy(export.file) }

    suspend fun discardUndelivered(
        export: PreparedWorkoutSessionExport
    ): WorkoutSessionExportCleanupResult = withContext(Dispatchers.IO) { deleteCopy(export.file) }

    suspend fun cleanupManually(): WorkoutSessionExportCleanupResult = withContext(Dispatchers.IO) {
        startupCleanup.await()
        cleanup(Instant.now(), manually = true)
    }

    private fun deleteCopy(file: File): WorkoutSessionExportCleanupResult = try {
        Files.delete(file.toPath())
        WorkoutSessionExportCleanupResult(listOf(file), emptyList())
    } catch (cause: Exception) {
        WorkoutSessionExportCleanupResult(emptyList(), listOf(WorkoutSessionExportCleanupFailure(file, cause)))
    }

    private fun cleanup(now: Instant, manually: Boolean): WorkoutSessionExportCleanupResult {
        val deleted = mutableListOf<File>()
        val failures = mutableListOf<WorkoutSessionExportCleanupFailure>()
        try {
            if (!existingDirectory(root)) return WorkoutSessionExportCleanupResult(deleted, failures)
        } catch (cause: Exception) {
            return WorkoutSessionExportCleanupResult(deleted, listOf(WorkoutSessionExportCleanupFailure(root, cause)))
        }
        for (directory in listOf(incomplete, ready, shared)) {
            try {
                if (!existingDirectory(directory)) continue
                Files.newDirectoryStream(directory.toPath()).use { entries ->
                    for (path in entries) {
                        val name = path.fileName.toString()
                        val matches = when (directory) {
                            incomplete -> PART_NAME.matches(name)
                            ready -> READY_NAME.matches(name)
                            else -> SHARED_NAME.matches(name)
                        }
                        if (!matches) continue
                        try {
                            if (!attributes(path).isRegularFile) continue
                            if (directory == shared && !manually) {
                                val millis = SHARED_NAME.matchEntire(name)!!.groupValues[1].toLong()
                                if (Duration.between(Instant.ofEpochMilli(millis), now) <= Duration.ofHours(24)) continue
                            }
                            Files.delete(path)
                            deleted += path.toFile()
                        } catch (cause: Exception) {
                            failures += WorkoutSessionExportCleanupFailure(path.toFile(), cause)
                        }
                    }
                }
            } catch (cause: Exception) {
                failures += WorkoutSessionExportCleanupFailure(directory, cause)
            }
        }
        return WorkoutSessionExportCleanupResult(deleted, failures)
    }

    private fun existingDirectory(directory: File): Boolean {
        val attrs = try { attributes(directory.toPath()) } catch (_: NoSuchFileException) { return false }
        if (!attrs.isDirectory) throw IOException("Export path is not a directory: $directory")
        return true
    }

    private fun ensureDirectory(directory: File) {
        if (!existingDirectory(directory)) Files.createDirectory(directory.toPath())
    }

    private fun requireAbsent(file: File) {
        try { attributes(file.toPath()) } catch (_: NoSuchFileException) { return }
        throw FileAlreadyExistsException(file.path)
    }

    private fun attributes(path: Path): BasicFileAttributes =
        Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS)

    internal fun validateCompletedFile(
        file: File,
        selection: WorkoutSessionExportSelection,
        generatedAt: Instant,
        displayLocale: String
    ) {
        file.inputStream().use { stream ->
            stream.bufferedReader(Charsets.UTF_8).use { input ->
                JsonReader(input).use { reader ->
                reader.isLenient = false
                reader.beginObject()
                check(reader.nextName() == "trainFlowSessionExport") { "export envelope" }
                reader.beginObject()
                val seen = mutableSetOf<String>()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    check(seen.add(name)) { "duplicate export field: $name" }
                    when (name) {
                        "exportContractVersion" -> check(reader.peek() == JsonToken.NUMBER && reader.nextLong() == 2L) { name }
                        "generatedAt" -> check(reader.nextString() == MILLIS_FORMAT.format(generatedAt)) { name }
                        "displayLocale" -> check(reader.nextString() == displayLocale) { name }
                        "selection" -> {
                            reader.beginObject()
                            var included = false
                            while (reader.hasNext()) {
                                if (reader.nextName() == "includedSessionIds") {
                                    check(!included) { "duplicate includedSessionIds" }
                                    included = true
                                    readIds(reader, selection.includedSessionIds)
                                } else reader.skipValue()
                            }
                            reader.endObject()
                            check(included) { "selection.includedSessionIds" }
                        }
                        "sessions" -> readSessions(reader, selection.includedSessionIds)
                        else -> reader.skipValue()
                    }
                }
                check(seen.containsAll(listOf("exportContractVersion", "generatedAt", "displayLocale", "selection", "sessions"))) {
                    "incomplete export envelope"
                }
                reader.endObject()
                reader.endObject()
                check(reader.peek() == JsonToken.END_DOCUMENT) { "trailing JSON" }
                }
            }
        }
    }

    private fun readIds(reader: JsonReader, expected: List<String>) {
        reader.beginArray()
        var count = 0
        while (reader.hasNext()) {
            check(count < expected.size && reader.peek() == JsonToken.STRING && reader.nextString() == expected[count]) {
                "selection.includedSessionIds[$count]"
            }
            count++
        }
        reader.endArray()
        check(count == expected.size) { "selection.includedSessionIds count" }
    }

    private fun readSessions(reader: JsonReader, expected: List<String>) {
        reader.beginArray()
        var count = 0
        while (reader.hasNext()) {
            check(count < expected.size) { "sessions count" }
            reader.beginObject()
            var sessionSeen = false
            while (reader.hasNext()) {
                if (reader.nextName() != "session") {
                    reader.skipValue()
                    continue
                }
                check(!sessionSeen) { "duplicate session header" }
                sessionSeen = true
                reader.beginObject()
                var idSeen = false
                while (reader.hasNext()) {
                    if (reader.nextName() == "sessionId") {
                        check(!idSeen && reader.peek() == JsonToken.STRING && reader.nextString() == expected[count]) {
                            "sessions[$count].sessionId"
                        }
                        idSeen = true
                    } else reader.skipValue()
                }
                reader.endObject()
                check(idSeen) { "sessionId missing" }
            }
            reader.endObject()
            check(sessionSeen) { "session header missing" }
            count++
        }
        reader.endArray()
        check(count == expected.size) { "sessions count" }
    }

    private companion object {
        const val UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        val PART_NAME = Regex("export-$UUID_PATTERN\\.json\\.part")
        val READY_NAME = Regex("export-$UUID_PATTERN\\.json")
        val SHARED_NAME = Regex("shared-(-?[0-9]+)-$UUID_PATTERN\\.json")
        val MILLIS_FORMAT = DateTimeFormatterBuilder().appendInstant(3).toFormatter()
    }
}
