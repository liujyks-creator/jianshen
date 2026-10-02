package com.liujyks.trainflow.core.data

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.JsonReader
import android.util.JsonToken
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.feature.workoutsession.freeFollowAlongPhase
import com.liujyks.trainflow.feature.workoutsession.freeFollowAlongSnapshot
import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutSessionExportFilesTest {
    @Test fun prepareFreezesSelectionAndReturnsWholeReadyDocument() = runBlocking {
        fixture { f ->
            val files = f.files()
            val ids = IDS.toMutableList()
            val unknown = mutableListOf(IDS[1])
            val modes = mutableSetOf("follow_along")
            val selection = selection().copy(includedSessionIds = ids,
                includedUnknownDateSessionIds = unknown, modeFilter = modes)
            val progress = mutableListOf<Pair<Int, Int>>()
            val export = files.prepare(selection, NOW, "zh-CN") { processed, total ->
                progress += processed to total
                if (processed == 1) { ids.clear(); unknown.clear(); modes.clear() }
            }
            val document = document(export.file)
            assertEquals(2L, document["exportContractVersion"])
            assertEquals("2026-10-01T00:00:00.000Z", document["generatedAt"])
            assertEquals("zh-CN", document["displayLocale"])
            assertTrue(document["dataDictionary"] is Map<*, *>)
            val frozen = obj(document["selection"])
            assertEquals(IDS, frozen["includedSessionIds"])
            assertEquals(listOf(IDS[1]), frozen["includedUnknownDateSessionIds"])
            assertEquals(listOf("follow_along"), frozen["modeFilter"])
            assertEquals(IDS, arr(document["sessions"]).map { obj(obj(it)["session"])["sessionId"] })
            assertEquals(IDS, export.includedSessionIds)
            assertEquals(listOf(1 to 2, 2 to 2), progress)
            assertEquals("ready", export.file.parentFile!!.name)
            assertTrue(f.partFiles().isEmpty())
        }
    }

    @Test fun strictReadFailurePreservesPrimaryAndCleanupSecondary() = runBlocking {
        fixture { f ->
            val error = fails<WorkoutSessionExportReadRejected> {
                f.files().prepare(missingSelection(), NOW, "zh-CN") { _, _ -> }
            }
            assertMissing(error)
            assertTrue(f.partFiles().isEmpty())
            assertTrue(f.readyFiles().isEmpty())
        }
        fixture { f ->
            try {
                val error = fails<WorkoutSessionExportReadRejected> {
                    f.files().prepare(missingSelection(), NOW, "zh-CN") { count, _ ->
                        if (count == 1) Os.chmod(f.incomplete.path, 0b101000000)
                    }
                }
                assertMissing(error)
                assertTrue(error.suppressed.any { it is IOException })
                assertEquals(1, f.partFiles().size)
                assertTrue(f.readyFiles().isEmpty())
            } finally { Os.chmod(f.incomplete.path, 0b111000000) }
        }
    }

    @Test fun cancelAfterFirstProcessedSessionCleansOperation() = runBlocking {
        fixture { f ->
            val processed = mutableListOf<Int>()
            coroutineScope {
                lateinit var operation: Deferred<PreparedWorkoutSessionExport>
                operation = async(start = CoroutineStart.LAZY) {
                    f.files().prepare(selection(), NOW, "zh-CN") { count, _ ->
                        processed += count
                        if (count == 1) operation.cancel(CancellationException("user-left-export-flow"))
                    }
                }
                operation.start()
                val error = fails<CancellationException> { operation.await() }
                assertEquals("user-left-export-flow", error.message)
            }
            assertEquals(listOf(1), processed)
            assertTrue(f.partFiles().isEmpty())
            assertTrue(f.readyFiles().isEmpty())
        }
    }

    @Test fun createAndPublishFailuresPreserveExistingFiles() = runBlocking {
        fixture { f ->
            f.incomplete.mkdirs()
            try {
                Os.chmod(f.incomplete.path, 0b101000000)
                fails<IOException> { f.files().prepare(selection(), NOW, "zh-CN") { _, _ -> } }
                assertTrue(f.readyFiles().isEmpty())
                assertTrue(f.partFiles().isEmpty())
            } finally { Os.chmod(f.incomplete.path, 0b111000000) }
        }
        fixture { f ->
            lateinit var marker: File
            fails<FileAlreadyExistsException> {
                f.files().prepare(selection(), NOW, "zh-CN") { count, _ ->
                    if (count == 1) {
                        marker = File(f.ready, f.partFiles().single().name.removeSuffix(".part"))
                        marker.writeText("old-ready-marker", Charsets.UTF_8)
                    }
                }
            }
            assertEquals("old-ready-marker", marker.readText(Charsets.UTF_8))
            assertTrue(f.partFiles().isEmpty())
        }
        fixture { f ->
            try {
                val error = fails<ErrnoException> {
                    f.files().prepare(selection(), NOW, "zh-CN") { count, _ ->
                        if (count == 1) Os.chmod(f.ready.path, 0b101000000)
                    }
                }
                assertEquals(OsConstants.EACCES, error.errno)
                assertTrue(f.readyFiles().isEmpty())
                assertTrue(f.partFiles().isEmpty())
            } finally { Os.chmod(f.ready.path, 0b111000000) }
        }
    }

    @Test fun completedFileCheckRejectsBrokenOrMismatchedDocument() = runBlocking {
        fixture { f ->
            val files = f.files()
            val export = files.prepare(selection(), NOW, "zh-CN") { _, _ -> }
            val bytes = export.file.readBytes()
            val truncated = File(f.root, "truncated.json").apply { writeBytes(bytes.copyOf(bytes.size - 1)) }
            val trailing = File(f.root, "trailing.json").apply { writeBytes(bytes + "{}".toByteArray()) }
            fails<Exception> { files.validateCompletedFile(truncated, selection(), NOW, "zh-CN") }
            fails<Exception> { files.validateCompletedFile(trailing, selection(), NOW, "zh-CN") }
            fails<IllegalStateException> {
                files.validateCompletedFile(export.file, selection().copy(includedSessionIds = IDS.reversed()), NOW, "zh-CN")
            }
            files.validateCompletedFile(export.file, selection(), NOW, "zh-CN")
            assertArrayEquals(bytes, export.file.readBytes())
        }
    }

    @Test fun shareRenameKeepsBytesAndSuccessfulSaveDeletesAppCopy() = runBlocking {
        fixture { f ->
            val files = f.files()
            val original = files.prepare(selection(), NOW, "zh-CN") { _, _ -> }
            val hash = sha256(original.file)
            val at = NOW.plusSeconds(3600)
            val share = files.markShareAttempt(original, at)
            assertEquals("shared-${at.toEpochMilli()}-${original.operationId}.json", share.file.name)
            assertEquals("shared", share.file.parentFile!!.name)
            assertFalse(original.file.exists())
            assertEquals(hash, sha256(share.file))
            assertEquals(NOW, share.generatedAt)
            assertEquals("2026-10-01T00:00:00.000Z", document(share.file)["generatedAt"])
            val saved = files.prepare(selection(), NOW, "zh-CN") { _, _ -> }
            val saveReport = files.deleteAfterSuccessfulSave(saved)
            assertEquals(listOf(saved.file), saveReport.deletedFiles)
            assertTrue(saveReport.failures.isEmpty())
            assertFalse(saved.file.exists())
            val abandoned = files.prepare(selection(), NOW, "zh-CN") { _, _ -> }
            val discard = files.discardUndelivered(abandoned)
            assertEquals(listOf(abandoned.file), discard.deletedFiles)
            assertTrue(discard.failures.isEmpty())
            assertFalse(abandoned.file.exists())
            assertTrue(share.file.exists())
        }
    }

    @Test fun automaticCleanupHonoursRestartAndTwentyFourHourCutoff() = runBlocking {
        fixture { f ->
            val startupFiles = oldFiles(f)
            val files = f.files()
            val report = files.startupCleanup.await()
            assertTrue(report.failures.isEmpty())
            assertCutoff(startupFiles)
            assertEquals(startupFiles.filterIndexed { index, _ -> index in listOf(0, 1, 4) }.toSet(),
                report.deletedFiles.toSet())
            val beforeGeneration = oldFiles(f)
            val export = files.prepare(selection(), NOW, "zh-CN") { _, _ -> }
            assertCutoff(beforeGeneration)
            assertEquals(IDS, arr(document(export.file)["sessions"]).map { obj(obj(it)["session"])["sessionId"] })
            assertTrue(export.file.exists())
        }
    }

    @Test fun cleanupFailuresFollowStartupAndGenerationPolicy() = runBlocking {
        fixture { f ->
            f.ready.mkdirs()
            val residual = File(f.ready, "export-${UUID.randomUUID()}.json").apply { writeText("residual") }
            try {
                Os.chmod(f.ready.path, 0b101000000)
                val files = f.files()
                val report = files.startupCleanup.await()
                assertEquals(residual, report.failures.single().file)
                assertTrue(report.failures.single().cause is IOException)
                assertTrue(residual.exists())
                fails<IOException> { files.prepare(selection(), NOW, "zh-CN") { _, _ -> } }
                assertEquals(listOf(residual), f.readyFiles())
                assertTrue(f.partFiles().isEmpty())
            } finally { Os.chmod(f.ready.path, 0b111000000) }
        }
        fixture { f ->
            f.shared.mkdirs()
            val residual = File(f.shared, "shared-${NOW.minusSeconds(86401).toEpochMilli()}-${UUID.randomUUID()}.json")
                .apply { writeText("shared-residual") }
            try {
                Os.chmod(f.shared.path, 0b101000000)
                val files = f.files()
                val report = files.startupCleanup.await()
                assertEquals(residual, report.failures.single().file)
                assertTrue(report.failures.single().cause is IOException)
                val export = files.prepare(selection(), NOW, "zh-CN") { _, _ -> }
                assertTrue(residual.exists())
                assertEquals(IDS, arr(document(export.file)["sessions"]).map { obj(obj(it)["session"])["sessionId"] })
            } finally { Os.chmod(f.shared.path, 0b111000000) }
        }
    }

    private suspend fun fixture(block: suspend (Fixture) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val database = Room.databaseBuilder(context, TrainFlowDatabase::class.java, "e21-s02-files-$suffix.db")
            .addMigrations(TrainFlowDatabase.MIGRATION_1_2, TrainFlowDatabase.MIGRATION_2_3,
                TrainFlowDatabase.MIGRATION_3_4, TrainFlowDatabase.MIGRATION_4_5,
                TrainFlowDatabase.MIGRATION_5_6, TrainFlowDatabase.MIGRATION_6_7).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val root = File(context.filesDir, "e21-s02-files-$suffix").apply { mkdirs() }
        try {
            val plan = freeFollowAlongSnapshot().toStorageJson()
            val prepared = (PlanSnapshotStorageV1Validator.prepare(plan, WorkoutMode.FOLLOW_ALONG)
                as PreparedPlanSnapshotStorageV1Result.Valid).prepared
            val phase = freeFollowAlongPhase(prepared)
            IDS.forEachIndexed { index, id ->
                database.workoutSessionDao().insertSession(WorkoutSessionEntity(
                    id = id, mode = "follow_along", status = "completed", planSnapshotJson = plan,
                    startedAt = "2026-09-29T00:00:00Z", endedAt = "2026-09-29T00:00:03Z",
                    totalElapsedSec = 3, effectiveElapsedSec = 3, pausedElapsedSec = 0,
                    timelineVersion = 1, lastDurableOffsetMs = 3000, lastMutationSequence = 1,
                    trustedEndOffsetMs = 3000, terminalReason = "completed",
                    displayMetadataContractVersion = 1,
                    sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[]}""",
                    startLocalDate = if (index == 0) "2026-09-29" else null,
                    startZoneId = if (index == 0) "UTC" else null,
                    startUtcOffsetSeconds = if (index == 0) 0L else null,
                    timeMetadataSourceContractVersion = if (index == 0) 1L else null
                ))
                database.canonicalTimelineHeartRateDao().insertPhaseInterval(WorkoutPhaseIntervalEntity(
                    "$id:p:0", id, 0, 0, 3000, 0, 1, null, "follow_along_action", phase.phaseIdentityJson
                ))
            }
            block(Fixture(root, database, scope))
        } finally { scope.cancel(); database.close() }
    }

    private data class Fixture(val root: File, val database: TrainFlowDatabase, val scope: CoroutineScope) {
        val incomplete get() = File(root, "session-exports/incomplete")
        val ready get() = File(root, "session-exports/ready")
        val shared get() = File(root, "session-exports/shared")
        fun files() = WorkoutSessionExportFiles(root, WorkoutSessionRepository(database), scope, NOW)
        fun partFiles() = incomplete.listFiles()?.toList().orEmpty()
        fun readyFiles() = ready.listFiles()?.toList().orEmpty()
    }

    private fun oldFiles(f: Fixture): List<File> {
        listOf(f.incomplete, f.ready, f.shared).forEach { it.mkdirs() }
        return listOf(
            File(f.incomplete, "export-${UUID.randomUUID()}.json.part"),
            File(f.ready, "export-${UUID.randomUUID()}.json"),
            File(f.shared, "shared-${NOW.minusSeconds(3600).toEpochMilli()}-${UUID.randomUUID()}.json"),
            File(f.shared, "shared-${NOW.minusSeconds(86400).toEpochMilli()}-${UUID.randomUUID()}.json"),
            File(f.shared, "shared-${NOW.minusMillis(86400001).toEpochMilli()}-${UUID.randomUUID()}.json"),
            File(f.shared, "shared-${NOW.plusSeconds(3600).toEpochMilli()}-${UUID.randomUUID()}.json"),
            File(f.ready, "not-an-export.txt")
        ).onEach { it.writeText("old-file", Charsets.UTF_8) }
    }

    private fun assertCutoff(files: List<File>) {
        files.forEachIndexed { index, file -> assertEquals(file.path, index !in listOf(0, 1, 4), file.exists()) }
    }

    private fun assertMissing(error: WorkoutSessionExportReadRejected) {
        assertEquals("missing-session", error.requestedSessionId)
        assertTrue(error.source is WorkoutSessionStrictReadResult.NotFound)
    }

    private fun selection() = WorkoutSessionExportSelection("calendar", null, null,
        setOf("follow_along"), null, IDS, listOf(IDS[1]))
    private fun missingSelection() = selection().copy(includedSessionIds = listOf(IDS[0], "missing-session"),
        includedUnknownDateSessionIds = listOf("missing-session"))

    private suspend inline fun <reified T : Throwable> fails(block: suspend () -> Unit): T {
        try { block() } catch (error: Throwable) {
            if (error is T) return error
            throw error
        }
        throw AssertionError("Expected ${T::class.java.name}")
    }

    private fun document(file: File): Map<String, Any?> = file.reader(Charsets.UTF_8).use { input ->
        JsonReader(input).use { reader ->
            val root = obj(readValue(reader))
            assertEquals(setOf("trainFlowSessionExport"), root.keys)
            assertEquals(JsonToken.END_DOCUMENT, reader.peek())
            obj(root["trainFlowSessionExport"])
        }
    }

    private fun readValue(reader: JsonReader): Any? = when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> {
            reader.beginObject()
            val result = linkedMapOf<String, Any?>()
            while (reader.hasNext()) result[reader.nextName()] = readValue(reader)
            reader.endObject(); result
        }
        JsonToken.BEGIN_ARRAY -> {
            reader.beginArray(); val result = mutableListOf<Any?>()
            while (reader.hasNext()) result += readValue(reader)
            reader.endArray(); result
        }
        JsonToken.STRING -> reader.nextString()
        JsonToken.NUMBER -> reader.nextLong()
        JsonToken.BOOLEAN -> reader.nextBoolean()
        JsonToken.NULL -> { reader.nextNull(); null }
        else -> throw AssertionError("Unexpected ${reader.peek()}")
    }
    @Suppress("UNCHECKED_CAST") private fun obj(value: Any?) = value as Map<String, Any?>
    @Suppress("UNCHECKED_CAST") private fun arr(value: Any?) = value as List<Any?>
    private fun sha256(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        .joinToString("") { "%02x".format(it) }

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-01T00:00:00.000Z")
        val IDS = listOf("e21-s02-files-known", "e21-s02-files-unknown")
    }
}
