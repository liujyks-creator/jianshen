package com.liujyks.trainflow.core.data

import android.database.Cursor
import android.os.Bundle
import android.os.Debug
import android.os.SystemClock
import android.util.JsonReader
import android.util.JsonToken
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.feature.workoutsession.freeFollowAlongPhase
import com.liujyks.trainflow.feature.workoutsession.freeFollowAlongSnapshot
import java.io.File
import java.io.StringReader
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E17ExportPerformanceContractTest {
    @Test fun singleSessionCompletePrepareMeetsEveryPerRunBudget() = runBlocking {
        runProfile("single", listOf(Profile("e21-s02-p05-single-000", "strength", 10000, 10000, 250000)))
    }

    @Test fun hundredSessionsCompletePrepareMeetsEveryPerRunBudget() = runBlocking {
        val profiles = (0..99).map { index ->
            val mode = when { index < 34 -> "timed"; index < 67 -> "strength"; else -> "follow_along" }
            val phases = when { index < 51 -> 149; index < 67 -> 148; else -> 1 }
            Profile("e21-s02-p05-hundred-" + index.toString().padStart(3, '0'), mode, phases, 100, 2500)
        }
        runProfile("hundred", profiles)
    }

    private suspend fun runProfile(group: String, profiles: List<Profile>) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val root = File(context.filesDir, "e21-s02-p05-$group-$suffix").apply { mkdirs() }
        val database = Room.databaseBuilder(context, TrainFlowDatabase::class.java, "e21-s02-p05-$group-$suffix.db")
            .addMigrations(TrainFlowDatabase.MIGRATION_1_2, TrainFlowDatabase.MIGRATION_2_3,
                TrainFlowDatabase.MIGRATION_3_4, TrainFlowDatabase.MIGRATION_4_5,
                TrainFlowDatabase.MIGRATION_5_6, TrainFlowDatabase.MIGRATION_6_7).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = WorkoutSessionRepository(database)
            for (profile in profiles) {
                val seedRepository = if (group == "hundred") WorkoutSessionRepository(database) else repository
                seed(database.openHelper.writableDatabase, seedRepository, profile)
            }
            assertProfile(database.openHelper.readableDatabase, profiles)
            val files = WorkoutSessionExportFiles(root, repository, scope, GENERATED_AT)
            val ids = profiles.map { it.id }
            val selection = WorkoutSessionExportSelection(
                if (group == "single") "single_session" else "calendar", null, null, null, null, ids, ids
            )
            for (run in 0..4) {
                val label = if (run < 2) "warmup-" + (run + 1) else "measured-" + (run - 1)
                val samples = Collections.synchronizedList(mutableListOf<Int>())
                val sampling = AtomicBoolean(true)
                samples += totalPssKb()
                val sampler = Thread({
                    while (sampling.get()) {
                        samples += totalPssKb()
                        SystemClock.sleep(50)
                    }
                }, "e21-s02-pss")
                sampler.start()
                val start = SystemClock.elapsedRealtimeNanos()
                val export: PreparedWorkoutSessionExport
                val end: Long
                try {
                    export = files.prepare(selection, GENERATED_AT, "zh-CN") { _, _ -> }
                    end = SystemClock.elapsedRealtimeNanos()
                    samples += totalPssKb()
                } finally {
                    sampling.set(false)
                    sampler.join()
                }
                val elapsed = end - start
                val peak = samples.max().toLong() * 1024L
                val bytes = export.file.length()
                val hash = sha256(export.file)
                emit("E21_S02_P05 group=$group label=$label elapsedNanos=$elapsed " +
                    "elapsedMs=" + elapsed / 1000000L + " peakPssBytes=$peak fileBytes=$bytes sha256=$hash " +
                    "includedCount=" + ids.size + " includedIds=" + ids.joinToString(",") +
                    " pssSamplesKb=" + samples.joinToString(","))
                independentReadback(export.file, database.openHelper.readableDatabase, profiles)
                emit("E21_S02_READBACK group=$group label=$label result=PASS samples=250000 phases=10000 acquisitions=10000")
                if (run >= 2) {
                    assertTrue("$label elapsedNanos=$elapsed", elapsed <= 30000000000L)
                    assertTrue("$label peakPssBytes=$peak", peak <= 402653184L)
                    assertTrue("$label fileBytes=$bytes", bytes <= 134217728L)
                }
                assertEquals(ids, export.includedSessionIds)
                if (run < 4) {
                    val deleted = files.discardUndelivered(export)
                    assertEquals(listOf(export.file), deleted.deletedFiles)
                    assertTrue(deleted.failures.isEmpty())
                } else {
                    val relative = "files/" + export.file.relativeTo(context.filesDir).invariantSeparatorsPath
                    emit("E21_S02_JSON group=$group label=measured-3 path=$relative bytes=$bytes sha256=$hash")
                }
            }
        } finally { scope.cancel(); database.close() }
    }

    private data class Profile(val id: String, val mode: String, val phases: Int,
        val acquisitions: Int, val samples: Int) {
        val recordingId get() = "$id-r"
        val plan get() = when (mode) {
            "timed" -> TIMED_PLAN_SNAPSHOT
            "strength" -> STRENGTH_PLAN_SNAPSHOT
            else -> freeFollowAlongSnapshot().toStorageJson()
        }
        val metadata get() = if (mode == "timed") TIMED_METADATA else DISPLAY_METADATA
        fun identity(index: Int): Pair<String, String> = when (mode) {
            "timed" -> if (index == 0 || index == phases - 1) "timed_work" to TIMED_WORK_IDENTITY
                else "paused" to TIMED_PAUSED_IDENTITY
            "strength" -> when (index) {
                0 -> "strength_prepare_set" to STRENGTH_PREPARE_IDENTITY
                phases - 1 -> "strength_active_set" to STRENGTH_ACTIVE_IDENTITY
                else -> "paused" to STRENGTH_PAUSED_IDENTITY
            }
            else -> {
                val prepared = (PlanSnapshotStorageV1Validator.prepare(plan, WorkoutMode.FOLLOW_ALONG)
                    as PreparedPlanSnapshotStorageV1Result.Valid).prepared
                "follow_along_action" to freeFollowAlongPhase(prepared).phaseIdentityJson
            }
        }
    }

    private suspend fun seed(sql: SupportSQLiteDatabase, repository: WorkoutSessionRepository, p: Profile) {
        val first = p.identity(0)
        val owner = repository.admitRecorder("e21-s02-p05", WorkoutSessionEntity(
            id = p.id, planId = if (p.mode == "timed") "plan-old" else null,
            mode = p.mode, status = "active", planSnapshotJson = p.plan,
            timelineVersion = 1, lastDurableOffsetMs = 0, lastMutationSequence = 0,
            displayMetadataContractVersion = 1, sessionDisplayMetadataJson = p.metadata
        ), WorkoutPhaseIntervalEntity(p.id + ":phase:0", p.id, 0, 0, null, 0, null, 1,
            first.first, first.second)).ownerToken
        sql.beginTransaction()
        try {
            sql.execSQL("INSERT INTO workout_sessions(" +
                "id,plan_id,mode,status,plan_snapshot_json,started_at,ended_at," +
                "total_elapsed_sec,effective_elapsed_sec,paused_elapsed_sec,timeline_version," +
                "last_durable_offset_ms,last_mutation_sequence,trusted_end_offset_ms,terminal_reason," +
                "display_metadata_contract_version,session_display_metadata_json) " +
                "VALUES(?,?,?,'active',?,NULL,NULL,NULL,NULL,NULL,1,28800000,1000000,NULL,NULL,1,?)",
                arrayOf(p.id, if (p.mode == "timed") "plan-old" else null, p.mode, p.plan, p.metadata))
            sql.compileStatement("INSERT INTO workout_phase_intervals(" +
                "id,session_id,sequence,start_offset_ms,end_offset_ms,start_mutation_sequence," +
                "end_mutation_sequence,open_marker,phase_kind,phase_identity_json) VALUES(?,?,?,?,?,?,?,?,?,?)").use { statement ->
                repeat(p.phases) { index ->
                    statement.clearBindings()
                    val bounds = if (p.phases == 1) 0L to 28800000L else boundary(index, p.phases)
                    statement.bindString(1, p.id + ":phase:$index"); statement.bindString(2, p.id)
                    statement.bindLong(3, index.toLong()); statement.bindLong(4, bounds.first)
                    if (index == p.phases - 1) statement.bindNull(5) else statement.bindLong(5, bounds.second)
                    statement.bindLong(6, index.toLong())
                    if (index == p.phases - 1) { statement.bindNull(7); statement.bindLong(8, 1) }
                    else { statement.bindLong(7, index + 1L); statement.bindNull(8) }
                    val identity = p.identity(index)
                    statement.bindString(9, identity.first); statement.bindString(10, identity.second)
                    statement.executeInsert()
                }
            }
            sql.execSQL("INSERT INTO heart_rate_recordings(" +
                "recording_id,session_id,status,started_offset_ms,started_mutation_sequence," +
                "ended_offset_ms,ended_mutation_sequence,source_contract_version,source_kind," +
                "acquisition_contract_version,parameter_snapshot_version,age,personal_max_bpm," +
                "effective_max_bpm,effective_max_source,alert_threshold_bpm,zone_snapshot_json,original_analysis_version) " +
                "VALUES(?,?,'active',0,0,NULL,NULL,1,'ble_hrs',1,1,NULL,200,200,'personal_max',NULL,?,NULL)",
                arrayOf(p.recordingId, p.id, ZONE_SNAPSHOT_200))
            sql.compileStatement("INSERT INTO heart_rate_acquisition_intervals(" +
                "id,recording_id,sequence,start_offset_ms,end_offset_ms,start_mutation_sequence," +
                "end_mutation_sequence,open_marker,recording_intent,intent_reason,device_state,device_reason) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)").use { statement ->
                repeat(p.acquisitions) { index ->
                    statement.clearBindings()
                    val bounds = boundary(index, p.acquisitions)
                    statement.bindString(1, p.recordingId + ":acquisition:$index")
                    statement.bindString(2, p.recordingId); statement.bindLong(3, index.toLong())
                    statement.bindLong(4, bounds.first)
                    if (index == p.acquisitions - 1) statement.bindNull(5) else statement.bindLong(5, bounds.second)
                    statement.bindLong(6, index.toLong())
                    if (index == p.acquisitions - 1) { statement.bindNull(7); statement.bindLong(8, 1) }
                    else { statement.bindLong(7, index + 1L); statement.bindNull(8) }
                    if (index == 0 || index == 1 || index == p.acquisitions - 1) {
                        statement.bindString(9, "expected_recording"); statement.bindNull(10)
                    } else {
                        statement.bindString(9, "user_excluded")
                        statement.bindString(10, USER_EXCLUSION_REASONS[(index - 2) % 3])
                    }
                    val device = DEVICE_FACTS[index % DEVICE_FACTS.size]
                    statement.bindString(11, device.first)
                    if (device.second == null) statement.bindNull(12) else statement.bindString(12, device.second!!)
                    statement.executeInsert()
                }
            }
            sql.compileStatement("INSERT INTO heart_rate_samples(recording_id,sample_sequence,offset_ms,mutation_sequence,bpm) " +
                "VALUES(?,?,?,?,?)").use { statement ->
                repeat(p.samples) { sequence ->
                    val offset: Long
                    val mutation: Long
                    val bpm: Int
                    when {
                        sequence < 32 -> { offset = 5000; mutation = 0; bpm = 80 }
                        sequence == 32 -> { offset = 28769999; mutation = 0; bpm = 90 }
                        sequence <= 42 -> {
                            offset = 28770000L + sequence - 33
                            mutation = if (sequence == 33) maxOf(p.phases - 1, p.acquisitions - 1).toLong() else 0
                            bpm = listOf(99, 100, 119, 120, 139, 140, 159, 160, 179, 180)[sequence - 33]
                        }
                        sequence == 43 -> { offset = 28795001; mutation = 0; bpm = 180 }
                        else -> { offset = 10000L + sequence - 44; mutation = 0; bpm = 80 + (sequence - 44) % 20 }
                    }
                    statement.clearBindings()
                    statement.bindString(1, p.recordingId); statement.bindLong(2, sequence.toLong())
                    statement.bindLong(3, offset); statement.bindLong(4, mutation); statement.bindLong(5, bpm.toLong())
                    statement.executeInsert()
                }
            }
            sql.setTransactionSuccessful()
        } finally { sql.endTransaction() }
        repository.finalizeRecordingSession(owner, RecordingFinalizationRequest(
            p.id, p.recordingId, "active", CanonicalTuple(28800000, 1000000),
            28800000, "completed", "completed", "2026-08-31T00:00:00Z"
        ))
    }

    private fun boundary(index: Int, count: Int): Pair<Long, Long> {
        if (index == 0) return 0L to 3000L
        if (index == count - 1) return 28770000L to 28800000L
        val ordinal = index - 1 - if (index > 20) 1 else 0
        val start = 3000L + 28767000L * ordinal / (count - 3)
        return start to if (index == 20) start else 3000L + 28767000L * (ordinal + 1) / (count - 3)
    }

    private fun assertProfile(sql: SupportSQLiteDatabase, profiles: List<Profile>) {
        assertEquals(250000L, scalar(sql, "SELECT COUNT(*) FROM heart_rate_samples"))
        assertEquals(10000L, scalar(sql, "SELECT COUNT(*) FROM workout_phase_intervals"))
        assertEquals(10000L, scalar(sql, "SELECT COUNT(*) FROM heart_rate_acquisition_intervals"))
        for (p in profiles) {
            assertEquals(p.samples.toLong(), scalar(sql, "SELECT COUNT(*) FROM heart_rate_samples WHERE recording_id=?", p.recordingId))
            assertEquals(32L, scalar(sql, "SELECT COUNT(*) FROM heart_rate_samples WHERE recording_id=? AND offset_ms=5000", p.recordingId))
            assertEquals(if (p.phases == 1) 0L else 1L, scalar(sql,
                "SELECT COUNT(*) FROM workout_phase_intervals WHERE session_id=? AND end_offset_ms=start_offset_ms " +
                    "AND end_mutation_sequence>start_mutation_sequence", p.id))
            assertEquals(1L, scalar(sql, "SELECT COUNT(*) FROM heart_rate_acquisition_intervals WHERE recording_id=? " +
                "AND end_offset_ms=start_offset_ms AND end_mutation_sequence>start_mutation_sequence", p.recordingId))
            val thresholds = listOf(99L, 100L, 119L, 120L, 139L, 140L, 159L, 160L, 179L, 180L)
            sql.query("SELECT sample_sequence,offset_ms,bpm FROM heart_rate_samples WHERE recording_id=? " +
                "AND sample_sequence BETWEEN 33 AND 43 ORDER BY sample_sequence", arrayOf(p.recordingId)).use { c ->
                thresholds.forEachIndexed { index, bpm ->
                    assertTrue(c.moveToNext()); assertEquals(33L + index, c.getLong(0))
                    assertEquals(28770000L + index, c.getLong(1)); assertEquals(bpm, c.getLong(2))
                }
                assertTrue(c.moveToNext()); assertEquals(43L, c.getLong(0))
                assertEquals(28795001L, c.getLong(1)); assertEquals(180L, c.getLong(2))
                assertFalse(c.moveToNext())
            }
            assertEquals(42L, scalar(sql, "SELECT sample_sequence FROM heart_rate_samples WHERE recording_id=? " +
                "ORDER BY bpm DESC,offset_ms,mutation_sequence,sample_sequence LIMIT 1", p.recordingId))
        }
    }

    /** Streaming oracle: each emitted row is compared with independently queried persisted input. */
    private fun independentReadback(file: File, sql: SupportSQLiteDatabase, profiles: List<Profile>) {
        var sampleCount = 0L; var phaseCount = 0L; var acquisitionCount = 0L
        file.reader(Charsets.UTF_8).use { input ->
            JsonReader(input).use { reader ->
                reader.beginObject(); assertEquals("trainFlowSessionExport", reader.nextName()); reader.beginObject()
                val seen = mutableSetOf<String>()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    assertTrue(seen.add(name))
                    when (name) {
                        "exportContractVersion" -> assertEquals(2L, reader.nextLong())
                        "generatedAt" -> assertEquals("2026-10-01T12:00:00.000Z", reader.nextString())
                        "displayLocale" -> assertEquals("zh-CN", reader.nextString())
                        "selection" -> {
                            val selection = objectValue(reader)
                            assertEquals(profiles.map { it.id }, selection["includedSessionIds"])
                            assertEquals(profiles.map { it.id }, selection["includedUnknownDateSessionIds"])
                            assertEquals(if (profiles.size == 1) "single_session" else "calendar", selection["source"])
                            assertNull(selection["startDateInclusive"]); assertNull(selection["endDateInclusive"])
                            assertNull(selection["modeFilter"]); assertNull(selection["planFilter"])
                        }
                        "sessions" -> {
                            reader.beginArray()
                            for (p in profiles) {
                                assertTrue(reader.hasNext()); reader.beginObject()
                                val sections = mutableSetOf<String>()
                                while (reader.hasNext()) {
                                    val section = reader.nextName(); assertTrue(sections.add(section))
                                    when (section) {
                                        "session" -> {
                                            val header = objectValue(reader)
                                            assertEquals(p.id, header["sessionId"]); assertEquals(p.mode, header["mode"])
                                            assertEquals(p.plan, header["planSnapshotJson"])
                                            assertNull(header["startedAt"]); assertNull(header["endedAt"])
                                        }
                                        "execution" -> {
                                            reader.beginObject()
                                            var phasesSeen = false
                                            while (reader.hasNext()) {
                                                if (reader.nextName() == "phases") {
                                                    phasesSeen = true
                                                    phaseCount += compareRows(reader, sql,
                                                        "SELECT * FROM workout_phase_intervals WHERE session_id=? ORDER BY sequence",
                                                        p.id, PHASE_FIELDS, p.phases, "phaseIdentity" to "phase_identity_json")
                                                } else reader.skipValue()
                                            }
                                            reader.endObject(); assertTrue(phasesSeen)
                                        }
                                        "heartRate" -> {
                                            reader.beginObject()
                                            val heartFields = mutableSetOf<String>()
                                            while (reader.hasNext()) {
                                                val field = reader.nextName(); assertTrue(heartFields.add(field))
                                                when (field) {
                                                    "samples" -> sampleCount += compareRows(reader, sql,
                                                        "SELECT * FROM heart_rate_samples WHERE recording_id=? " +
                                                            "ORDER BY offset_ms,mutation_sequence,sample_sequence",
                                                        p.recordingId, SAMPLE_FIELDS, p.samples)
                                                    "intentAndAcquisition" -> acquisitionCount += compareRows(reader, sql,
                                                        "SELECT * FROM heart_rate_acquisition_intervals WHERE recording_id=? ORDER BY sequence",
                                                        p.recordingId, ACQUISITION_FIELDS, p.acquisitions)
                                                    "originalAnalysis" -> compareAnalysis(objectValue(reader), sql, p.recordingId)
                                                    "durationAudit" -> sql.query(
                                                        "SELECT duration_breakdown_json FROM heart_rate_analysis_snapshots WHERE recording_id=?",
                                                        arrayOf(p.recordingId)).use { cursor ->
                                                        assertTrue(cursor.moveToFirst())
                                                        assertEquals(parse(cursor.getString(0)), readValue(reader))
                                                    }
                                                    else -> reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                            assertTrue(heartFields.containsAll(listOf("samples", "intentAndAcquisition",
                                                "originalAnalysis", "durationAudit")))
                                        }
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                assertTrue(sections.containsAll(listOf("session", "execution", "heartRate")))
                            }
                            assertFalse(reader.hasNext()); reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject(); reader.endObject(); assertEquals(JsonToken.END_DOCUMENT, reader.peek())
                assertTrue(seen.containsAll(listOf("exportContractVersion", "generatedAt", "displayLocale", "selection", "sessions")))
            }
        }
        assertEquals(250000L, sampleCount); assertEquals(10000L, phaseCount); assertEquals(10000L, acquisitionCount)
    }

    private fun compareRows(reader: JsonReader, sql: SupportSQLiteDatabase, query: String, id: String,
        fields: Map<String, String>, expectedCount: Int, jsonField: Pair<String, String>? = null): Long {
        var count = 0L
        sql.query(query, arrayOf(id)).use { cursor ->
            reader.beginArray()
            while (reader.hasNext()) {
                assertTrue("extra JSON row in $query", cursor.moveToNext())
                val row = objectValue(reader)
                fields.forEach { (json, column) ->
                    assertTrue(row.containsKey(json))
                    assertEquals("$id row=$count field=$json", cursorValue(cursor, column), row[json])
                }
                jsonField?.let { (json, column) ->
                    assertEquals(parse(cursor.getString(cursor.getColumnIndexOrThrow(column))), row[json])
                }
                count++
            }
            reader.endArray(); assertFalse("missing JSON rows in $query", cursor.moveToNext())
        }
        assertEquals(expectedCount.toLong(), count)
        return count
    }

    private fun compareAnalysis(actual: Map<String, Any?>, sql: SupportSQLiteDatabase, id: String) {
        sql.query("SELECT * FROM heart_rate_analysis_snapshots WHERE recording_id=?", arrayOf(id)).use { cursor ->
            assertTrue(cursor.moveToFirst())
            ANALYSIS_FIELDS.forEach { (json, column) ->
                assertTrue(actual.containsKey(json))
                assertEquals("$id analysis.$json", cursorValue(cursor, column), actual[json])
            }
            mapOf("analysisConfig" to "analysis_config_json", "zoneDurations" to "zone_durations_json",
                "phaseAggregates" to "phase_aggregates_json", "qualityReasons" to "quality_reasons_json").forEach { (json, column) ->
                val index = cursor.getColumnIndexOrThrow(column)
                assertTrue(actual.containsKey(json))
                assertEquals(if (cursor.isNull(index)) null else parse(cursor.getString(index)), actual[json])
            }
            assertFalse(cursor.moveToNext())
        }
    }

    private fun cursorValue(cursor: Cursor, column: String): Any? {
        val i = cursor.getColumnIndexOrThrow(column)
        return when (cursor.getType(i)) {
            Cursor.FIELD_TYPE_NULL -> null
            Cursor.FIELD_TYPE_INTEGER -> BigDecimal.valueOf(cursor.getLong(i))
            Cursor.FIELD_TYPE_FLOAT -> cursor.getString(i).toBigDecimal()
            Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
            else -> throw AssertionError("Unexpected SQL field $column")
        }
    }

    private fun readValue(reader: JsonReader): Any? = when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> {
            reader.beginObject(); val values = linkedMapOf<String, Any?>()
            while (reader.hasNext()) {
                val key = reader.nextName(); assertFalse(values.containsKey(key)); values[key] = readValue(reader)
            }
            reader.endObject(); values
        }
        JsonToken.BEGIN_ARRAY -> {
            reader.beginArray(); val values = mutableListOf<Any?>()
            while (reader.hasNext()) values += readValue(reader)
            reader.endArray(); values
        }
        JsonToken.STRING -> reader.nextString()
        JsonToken.NUMBER -> reader.nextString().toBigDecimal()
        JsonToken.BOOLEAN -> reader.nextBoolean()
        JsonToken.NULL -> { reader.nextNull(); null }
        else -> throw AssertionError("Unexpected token " + reader.peek())
    }
    @Suppress("UNCHECKED_CAST")
    private fun objectValue(reader: JsonReader) = readValue(reader) as Map<String, Any?>
    private fun parse(json: String): Any? = JsonReader(StringReader(json)).use { readValue(it) }
    private fun scalar(sql: SupportSQLiteDatabase, query: String, vararg args: String): Long =
        sql.query(query, args).use { cursor -> assertTrue(cursor.moveToFirst()); cursor.getLong(0) }
    private fun totalPssKb() = Debug.MemoryInfo().also(Debug::getMemoryInfo).totalPss
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun emit(message: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply { putString("stream", "\n$message\n") })
    }

    private companion object {
        val GENERATED_AT: Instant = Instant.parse("2026-10-01T12:00:00.000Z")
        val PHASE_FIELDS = linkedMapOf("sequence" to "sequence", "startOffsetMs" to "start_offset_ms",
            "endOffsetMs" to "end_offset_ms", "startMutationSequence" to "start_mutation_sequence",
            "endMutationSequence" to "end_mutation_sequence", "phaseKind" to "phase_kind")
        val ACQUISITION_FIELDS = linkedMapOf("sequence" to "sequence", "startOffsetMs" to "start_offset_ms",
            "endOffsetMs" to "end_offset_ms", "startMutationSequence" to "start_mutation_sequence",
            "endMutationSequence" to "end_mutation_sequence", "recordingIntent" to "recording_intent",
            "intentReason" to "intent_reason", "deviceState" to "device_state", "deviceReason" to "device_reason")
        val SAMPLE_FIELDS = linkedMapOf("offsetMs" to "offset_ms", "mutationSequence" to "mutation_sequence",
            "sampleSequence" to "sample_sequence", "bpm" to "bpm")
        val ANALYSIS_FIELDS = linkedMapOf("analysisVersion" to "analysis_version", "createdAt" to "created_at",
            "inputLastMutationSequence" to "input_last_mutation_sequence", "sampleStatus" to "sample_status",
            "coverageStatus" to "coverage_status", "zoneStatus" to "zone_status",
            "canonicalSampleCount" to "canonical_sample_count", "primaryPointSampleCount" to "primary_point_sample_count",
            "eligibleDurationMs" to "eligible_duration_ms", "coveredDurationMs" to "covered_duration_ms",
            "coverageBasisPoints" to "coverage_basis_points", "weightedBpmMs" to "weighted_bpm_ms",
            "observedAvgBpm" to "observed_avg_bpm", "observedMaxBpm" to "observed_max_bpm",
            "highestOffsetMs" to "highest_offset_ms", "highestMutationSequence" to "highest_mutation_sequence",
            "highestSampleSequence" to "highest_sample_sequence")
        const val DISPLAY_METADATA =
            "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
        const val STRENGTH_PLAN_SNAPSHOT =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Strength\",\"mode\":\"strength\",\"blocks\":[{\"id\":\"block\",\"kind\":\"strength_exercise\",\"order\":0,\"exerciseId\":\"exercise\",\"sets\":[{\"id\":\"set\",\"order\":0,\"kind\":\"working\"}],\"substitutions\":[],\"setTimerMode\":\"manual_start\"}],\"preferences\":null,\"followAlong\":null}"
        const val SIGNATURE = "c7e6dd87cd0794071a57be2dcbfde1f1adb2030364d2ff9549631eeda486e0e3"
        const val STRENGTH_PREPARE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"strength_v1\",\"payloadVersion\":1,\"mode\":\"strength\",\"phaseKind\":\"strength_prepare_set\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"$SIGNATURE\"},\"payload\":{\"variant\":\"prepare_set\",\"blockId\":\"block\",\"setPlanId\":\"set\",\"plannedExerciseId\":\"exercise\",\"actualExerciseId\":\"exercise\",\"exerciseSetIndex0\":0,\"globalSetIndex0\":0,\"setKind\":\"working\",\"substitutedFromExerciseId\":null}}"
        const val STRENGTH_ACTIVE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"strength_v1\",\"payloadVersion\":1,\"mode\":\"strength\",\"phaseKind\":\"strength_active_set\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"$SIGNATURE\"},\"payload\":{\"variant\":\"active_set\",\"blockId\":\"block\",\"setPlanId\":\"set\",\"plannedExerciseId\":\"exercise\",\"actualExerciseId\":\"exercise\",\"exerciseSetIndex0\":0,\"globalSetIndex0\":0,\"setKind\":\"working\",\"substitutedFromExerciseId\":null}}"
        const val STRENGTH_PAUSED_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"strength_v1\",\"payloadVersion\":1,\"mode\":\"strength\",\"phaseKind\":\"paused\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"$SIGNATURE\"},\"payload\":{\"variant\":\"paused\",\"blockId\":null,\"setPlanId\":null,\"plannedExerciseId\":null,\"actualExerciseId\":null,\"exerciseSetIndex0\":null,\"globalSetIndex0\":null,\"setKind\":null,\"substitutedFromExerciseId\":null}}"
        const val ZONE_SNAPSHOT_200 =
            "{\"zoneSnapshotContractVersion\":1,\"unit\":\"bpm\",\"effectiveMaxBpm\":200,\"effectiveMaxSource\":\"personal_max\",\"zones\":[{\"zoneId\":\"below_50\",\"lowerBoundBasisPointsInclusive\":null,\"upperBoundBasisPointsExclusive\":5000},{\"zoneId\":\"from_50_to_60\",\"lowerBoundBasisPointsInclusive\":5000,\"upperBoundBasisPointsExclusive\":6000},{\"zoneId\":\"from_60_to_70\",\"lowerBoundBasisPointsInclusive\":6000,\"upperBoundBasisPointsExclusive\":7000},{\"zoneId\":\"from_70_to_80\",\"lowerBoundBasisPointsInclusive\":7000,\"upperBoundBasisPointsExclusive\":8000},{\"zoneId\":\"from_80_to_90\",\"lowerBoundBasisPointsInclusive\":8000,\"upperBoundBasisPointsExclusive\":9000},{\"zoneId\":\"at_or_above_90\",\"lowerBoundBasisPointsInclusive\":9000,\"upperBoundBasisPointsExclusive\":null}]}"
        val USER_EXCLUSION_REASONS = listOf(
            "user_turned_off",
            "user_opted_out",
            "user_disconnected_suppress_recovery"
        )
        val DEVICE_FACTS = listOf(
            "not_observing" to null,
            "no_source_selected" to "source_not_selected",
            "permission_required" to "permission_missing",
            "permission_required" to "permission_revoked",
            "bluetooth_unavailable" to "bluetooth_off",
            "bluetooth_unavailable" to "platform_unavailable",
            "searching" to "initial_acquisition",
            "searching" to "automatic_recovery",
            "connecting" to "initial_acquisition",
            "waiting_first_sample" to "automatic_recovery",
            "live" to null,
            "stale" to "first_sample_timeout",
            "stale" to "sample_stale_timeout",
            "reconnecting" to "automatic_recovery",
            "reconnecting" to "unexpected_disconnect",
            "disconnected" to "source_unavailable",
            "disconnected" to "unexpected_disconnect",
            "disconnected" to "connection_timeout",
            "technical_failure" to "measurement_stream_unavailable",
            "technical_failure" to "platform_failure"
        )
        const val TIMED_PLAN_SNAPSHOT = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History L","mode":"timed","blocks":[{"id":"w","kind":"warmup","title":"Warm old","order":0,"durationSec":2,"items":[]},{"id":"t","kind":"stretch","order":1,"durationSec":3,"items":[]},{"id":"d","kind":"cooldown","title":"Cool old","order":2,"durationSec":4,"items":[]},{"id":"b","kind":"warmup","title":"Boundary old","order":3,"items":[{"id":"bw","exerciseId":"ex-b","labelOverride":"Boundary item","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":6,"restAfterSec":2,"autoAdvance":false},{"id":"br","labelOverride":"Boundary rest","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"a","kind":"timed_circuit","title":"Circuit A","order":4,"rounds":2,"restBetweenRoundsSec":5,"items":[{"id":"aw","exerciseId":"ex-a","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":10,"restAfterSec":4,"autoAdvance":false},{"id":"ar","labelOverride":"Item rest A","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"z","kind":"timed_circuit","title":"Circuit Z","order":5,"rounds":1,"restBetweenRoundsSec":0,"items":[{"id":"zw","exerciseId":"ex-z","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":8,"restAfterSec":2,"autoAdvance":false}]},{"id":"r","kind":"rest","title":"Rest title","order":6,"durationSec":7,"label":"Rest label"}],"preferences":null,"followAlong":null}"""
        const val TIMED_METADATA = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-b","displayNameAtFirstReference":"Metadata B","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-a","displayNameAtFirstReference":"Base A","customNameAtFirstReference":"Custom A","resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-z","displayNameAtFirstReference":"Exercise Z","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"}]}"""
        const val TIMED_WORK_IDENTITY = """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_block_work","blockId":"w","stepIndex0":0,"legacyBlockKind":"warmup","legacyStageType":"warmup","itemId":null,"exerciseId":null,"roundIndex0":null}}"""
        const val TIMED_PAUSED_IDENTITY = """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"paused","blockId":null,"stepIndex0":null,"legacyBlockKind":null,"legacyStageType":null,"itemId":null,"exerciseId":null,"roundIndex0":null}}"""
    }
}
