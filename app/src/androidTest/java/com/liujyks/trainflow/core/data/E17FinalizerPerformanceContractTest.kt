package com.liujyks.trainflow.core.data

import android.content.Context
import android.os.Debug
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.liujyks.trainflow.core.database.AnalysisSnapshotV1Validator
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1Validator
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.CanonicalValidationResult
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E17FinalizerPerformanceContractTest {
    private lateinit var context: Context
    private lateinit var database: TrainFlowDatabase

    @Before
    fun createFixedProfile() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TrainFlowDatabase.DATABASE_NAME)
        database = TrainFlowDatabase.create(context)
        seedPBalancedV2()
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(TrainFlowDatabase.DATABASE_NAME)
    }

    @Test
    fun pBalancedV2FinalizerMeetsEveryPerRunBudgetOnRealRoomSqlite() = runBlocking {
        assertProfileIdentity()
        repeat(WARMUP_RUNS) { runIndex ->
            val measurement = measureFinalization("warmup-${runIndex + 1}")
            assertCommittedGraph(measurement)
            resetCandidate()
        }
        repeat(MEASURED_RUNS) { runIndex ->
            val measurement = measureFinalization("measured-${runIndex + 1}")
            assertCommittedGraph(measurement)
            assertTrue(
                "run ${runIndex + 1} exceeded ${MAX_TIME_MS}ms: ${measurement.elapsedMs}",
                measurement.elapsedMs <= MAX_TIME_MS
            )
            assertTrue(
                "run ${runIndex + 1} exceeded ${MAX_PSS_BYTES} bytes: ${measurement.peakPssBytes}",
                measurement.peakPssBytes <= MAX_PSS_BYTES
            )
            assertTrue(
                "run ${runIndex + 1} exceeded ${MAX_JSON_BYTES} JSON bytes: ${measurement.jsonBytes}",
                measurement.jsonBytes <= MAX_JSON_BYTES
            )
            if (runIndex != MEASURED_RUNS - 1) resetCandidate()
        }
    }

    private suspend fun measureFinalization(label: String): Measurement {
        val pssSamplesKb = Collections.synchronizedList(mutableListOf<Int>())
        val sampling = AtomicBoolean(true)
        pssSamplesKb += currentTotalPssKb()
        val sampler = Thread(
            {
                while (sampling.get()) {
                    pssSamplesKb += currentTotalPssKb()
                    SystemClock.sleep(PSS_SAMPLE_INTERVAL_MS)
                }
            },
            "e17-finalizer-pss"
        )
        sampler.start()
        val startNanos = SystemClock.elapsedRealtimeNanos()
        val result = WorkoutSessionRepository(database).finalizeRecordingSession(REQUEST)
        val endNanos = SystemClock.elapsedRealtimeNanos()
        pssSamplesKb += currentTotalPssKb()
        sampling.set(false)
        sampler.join()
        pssSamplesKb += currentTotalPssKb()

        val graph = requireGraph()
        val snapshot = graph.snapshots.single()
        val jsonBytes = listOf(
            snapshot.analysisConfigJson,
            snapshot.zoneDurationsJson.orEmpty(),
            snapshot.phaseAggregatesJson,
            snapshot.durationBreakdownJson,
            snapshot.qualityReasonsJson
        ).fold(0L) { total, json ->
            Math.addExact(total, json.toByteArray(Charsets.UTF_8).size.toLong())
        }
        val measurement = Measurement(
            result = result,
            graph = graph,
            elapsedMs = (endNanos - startNanos) / 1_000_000L,
            peakPssBytes = pssSamplesKb.max().toLong() * 1_024L,
            jsonBytes = jsonBytes
        )
        println(
            "E17_CS_05_PERF profile=$PROFILE label=$label " +
                "elapsedMs=${measurement.elapsedMs} peakTotalPssBytes=${measurement.peakPssBytes} " +
                "analysisJsonBytes=${measurement.jsonBytes} pssSamplesKb=${pssSamplesKb.joinToString(",")}"
        )
        return measurement
    }

    private fun assertCommittedGraph(measurement: Measurement) {
        assertEquals(SESSION_ID, measurement.result.sessionId)
        assertEquals(RECORDING_ID, measurement.result.recordingId)
        assertEquals(FINAL_TUPLE, measurement.result.finalTuple)
        assertEquals(1, measurement.result.analysisVersion)
        assertEquals(
            CanonicalValidationResult.Valid,
            CanonicalSessionGraphV1Validator.validate(measurement.graph)
        )
        assertEquals(
            CanonicalValidationResult.Valid,
            AnalysisSnapshotV1Validator.validate(
                measurement.graph,
                measurement.graph.snapshots.single()
            )
        )
        val snapshot = measurement.graph.snapshots.single()
        assertEquals(SAMPLE_COUNT.toLong(), snapshot.canonicalSampleCount)
        assertTrue(snapshot.phaseAggregatesJson.contains("\"phaseSequence\":9999"))
        DEVICE_STATES.forEach { key ->
            assertTrue("missing device state $key", snapshot.durationBreakdownJson.contains("\"$key\":"))
        }
        DEVICE_REASONS.forEach { key ->
            assertTrue("missing device reason $key", snapshot.durationBreakdownJson.contains("\"$key\":"))
        }
        listOf(
            "strength_prepare_excluded",
            "paused_excluded",
            "user_turned_off_excluded",
            "user_opted_out_excluded",
            "user_disconnected_suppress_recovery_excluded"
        ).forEach { reason ->
            assertTrue("missing quality reason $reason", snapshot.qualityReasonsJson.contains(reason))
        }
        assertTrue(snapshot.zoneDurationsJson?.contains("\"atOrAbove90DurationMs\":") == true)
        assertEquals(HIGHEST_FIRST_OFFSET, snapshot.highestOffsetMs)
        assertEquals(HIGHEST_FIRST_SAMPLE_SEQUENCE, snapshot.highestSampleSequence)
    }

    private fun assertProfileIdentity() {
        val sql = database.openHelper.writableDatabase
        assertEquals(PHASE_COUNT, sql.longForQuery("SELECT COUNT(*) FROM workout_phase_intervals"))
        assertEquals(
            ACQUISITION_COUNT,
            sql.longForQuery("SELECT COUNT(*) FROM heart_rate_acquisition_intervals")
        )
        assertEquals(SAMPLE_COUNT, sql.longForQuery("SELECT COUNT(*) FROM heart_rate_samples"))
        assertEquals(
            1,
            sql.longForQuery(
                "SELECT COUNT(*) FROM workout_phase_intervals " +
                    "WHERE end_offset_ms=start_offset_ms AND end_mutation_sequence>start_mutation_sequence"
            )
        )
        assertEquals(
            1,
            sql.longForQuery(
                "SELECT COUNT(*) FROM heart_rate_acquisition_intervals " +
                    "WHERE end_offset_ms=start_offset_ms AND end_mutation_sequence>start_mutation_sequence"
            )
        )
        assertEquals(
            SAME_OFFSET_BURST,
            sql.longForQuery(
                "SELECT COUNT(*) FROM heart_rate_samples WHERE offset_ms=$BURST_OFFSET"
            )
        )
        assertEquals(SESSION_DURATION_MS, REQUEST.finalOffsetMs)
    }

    private fun seedPBalancedV2() {
        val sql = database.openHelper.writableDatabase
        sql.beginTransaction()
        try {
            sql.execSQL(
                """
                INSERT INTO workout_sessions(
                    id,plan_id,mode,status,plan_snapshot_json,started_at,ended_at,
                    total_elapsed_sec,effective_elapsed_sec,paused_elapsed_sec,timeline_version,
                    last_durable_offset_ms,last_mutation_sequence,trusted_end_offset_ms,terminal_reason,
                    display_metadata_contract_version,session_display_metadata_json
                ) VALUES(?,NULL,'strength','active',?,NULL,NULL,NULL,NULL,NULL,1,?,?,NULL,NULL,1,?)
                """.trimIndent(),
                arrayOf<Any?>(
                    SESSION_ID,
                    STRENGTH_PLAN_SNAPSHOT,
                    SESSION_DURATION_MS,
                    EXPECTED_SEQUENCE,
                    DISPLAY_METADATA
                )
            )
            insertPhases(sql)
            sql.execSQL(
                """
                INSERT INTO heart_rate_recordings(
                    recording_id,session_id,status,started_offset_ms,started_mutation_sequence,
                    ended_offset_ms,ended_mutation_sequence,source_contract_version,source_kind,
                    acquisition_contract_version,parameter_snapshot_version,age,personal_max_bpm,
                    effective_max_bpm,effective_max_source,alert_threshold_bpm,zone_snapshot_json,
                    original_analysis_version
                ) VALUES(?,?,'active',0,0,NULL,NULL,1,'ble_hrs',1,1,NULL,200,200,'personal_max',NULL,?,NULL)
                """.trimIndent(),
                arrayOf(RECORDING_ID, SESSION_ID, ZONE_SNAPSHOT_200)
            )
            insertAcquisitions(sql)
            insertSamples(sql)
            sql.setTransactionSuccessful()
        } finally {
            sql.endTransaction()
        }
    }

    private fun insertPhases(sql: androidx.sqlite.db.SupportSQLiteDatabase) {
        val statement = sql.compileStatement(
            """
            INSERT INTO workout_phase_intervals(
                id,session_id,sequence,start_offset_ms,end_offset_ms,start_mutation_sequence,
                end_mutation_sequence,open_marker,phase_kind,phase_identity_json
            ) VALUES(?,?,?,?,?,?,?,?,?,?)
            """.trimIndent()
        )
        repeat(PHASE_COUNT) { index ->
            statement.clearBindings()
            val boundary = phaseBoundary(index)
            statement.bindString(1, "$SESSION_ID:phase:$index")
            statement.bindString(2, SESSION_ID)
            statement.bindLong(3, index.toLong())
            statement.bindLong(4, boundary.first)
            if (index == PHASE_COUNT - 1) statement.bindNull(5) else statement.bindLong(5, boundary.second)
            statement.bindLong(6, index.toLong())
            if (index == PHASE_COUNT - 1) statement.bindNull(7) else statement.bindLong(7, index + 1L)
            if (index == PHASE_COUNT - 1) statement.bindLong(8, 1) else statement.bindNull(8)
            when (index) {
                0 -> {
                    statement.bindString(9, "strength_prepare_set")
                    statement.bindString(10, STRENGTH_PREPARE_IDENTITY)
                }
                PHASE_COUNT - 1 -> {
                    statement.bindString(9, "strength_active_set")
                    statement.bindString(10, STRENGTH_ACTIVE_IDENTITY)
                }
                else -> {
                    statement.bindString(9, "paused")
                    statement.bindString(10, STRENGTH_PAUSED_IDENTITY)
                }
            }
            statement.executeInsert()
        }
    }

    private fun insertAcquisitions(sql: androidx.sqlite.db.SupportSQLiteDatabase) {
        val statement = sql.compileStatement(
            """
            INSERT INTO heart_rate_acquisition_intervals(
                id,recording_id,sequence,start_offset_ms,end_offset_ms,start_mutation_sequence,
                end_mutation_sequence,open_marker,recording_intent,intent_reason,device_state,device_reason
            ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent()
        )
        repeat(ACQUISITION_COUNT) { index ->
            statement.clearBindings()
            val boundary = phaseBoundary(index)
            statement.bindString(1, "$RECORDING_ID:acquisition:$index")
            statement.bindString(2, RECORDING_ID)
            statement.bindLong(3, index.toLong())
            statement.bindLong(4, boundary.first)
            if (index == ACQUISITION_COUNT - 1) statement.bindNull(5) else statement.bindLong(5, boundary.second)
            statement.bindLong(6, index.toLong())
            if (index == ACQUISITION_COUNT - 1) statement.bindNull(7) else statement.bindLong(7, index + 1L)
            if (index == ACQUISITION_COUNT - 1) statement.bindLong(8, 1) else statement.bindNull(8)
            if (index == 0 || index == 1 || index == ACQUISITION_COUNT - 1) {
                statement.bindString(9, "expected_recording")
                statement.bindNull(10)
            } else {
                statement.bindString(9, "user_excluded")
                statement.bindString(10, USER_EXCLUSION_REASONS[(index - 2) % USER_EXCLUSION_REASONS.size])
            }
            val device = DEVICE_FACTS[index % DEVICE_FACTS.size]
            statement.bindString(11, device.first)
            device.second?.let { statement.bindString(12, it) } ?: statement.bindNull(12)
            statement.executeInsert()
        }
    }

    private fun insertSamples(sql: androidx.sqlite.db.SupportSQLiteDatabase) {
        val statement = sql.compileStatement(
            "INSERT INTO heart_rate_samples(recording_id,sample_sequence,offset_ms,mutation_sequence,bpm) " +
                "VALUES(?,?,?,?,?)"
        )
        var sequence = 0L
        repeat(SAME_OFFSET_BURST) {
            insertSample(statement, sequence++, BURST_OFFSET, 0, 80)
        }
        insertSample(statement, sequence++, PRIMARY_START - 1, 0, 90)
        val thresholdBpms = listOf(99, 100, 119, 120, 139, 140, 159, 160, 179, 180)
        thresholdBpms.forEachIndexed { index, bpm ->
            insertSample(
                statement,
                sequence++,
                PRIMARY_START + index,
                if (index == 0) PHASE_COUNT.toLong() - 1 else 0,
                bpm
            )
        }
        insertSample(statement, sequence++, PRIMARY_START + 25_001, 0, 180)
        val bulkCount = SAMPLE_COUNT - sequence.toInt()
        repeat(bulkCount) { index ->
            insertSample(statement, sequence++, BULK_START + index, 0, 80 + index % 20)
        }
        check(sequence == SAMPLE_COUNT.toLong())
    }

    private fun insertSample(
        statement: androidx.sqlite.db.SupportSQLiteStatement,
        sequence: Long,
        offset: Long,
        mutation: Long,
        bpm: Int
    ) {
        statement.clearBindings()
        statement.bindString(1, RECORDING_ID)
        statement.bindLong(2, sequence)
        statement.bindLong(3, offset)
        statement.bindLong(4, mutation)
        statement.bindLong(5, bpm.toLong())
        statement.executeInsert()
    }

    private fun phaseBoundary(index: Int): Pair<Long, Long> {
        if (index == 0) return 0L to FIRST_EXCLUDED_END
        if (index == PHASE_COUNT - 1) return PRIMARY_START to SESSION_DURATION_MS
        val positiveOrdinalBefore = when {
            index <= ZERO_DURATION_INDEX -> index - 1
            else -> index - 2
        }.coerceAtLeast(0)
        val start = FIRST_EXCLUDED_END +
            (EXCLUDED_SPAN * positiveOrdinalBefore / POSITIVE_PAUSED_INTERVALS)
        if (index == ZERO_DURATION_INDEX) return start to start
        val positiveOrdinalAfter = positiveOrdinalBefore + 1
        val end = FIRST_EXCLUDED_END +
            (EXCLUDED_SPAN * positiveOrdinalAfter / POSITIVE_PAUSED_INTERVALS)
        return start to end
    }

    private suspend fun resetCandidate() {
        val sql = database.openHelper.writableDatabase
        sql.beginTransaction()
        try {
            sql.execSQL("DELETE FROM heart_rate_analysis_snapshots WHERE recording_id='$RECORDING_ID'")
            sql.execSQL(
                "UPDATE heart_rate_recordings SET status='active',ended_offset_ms=NULL," +
                    "ended_mutation_sequence=NULL,original_analysis_version=NULL " +
                    "WHERE recording_id='$RECORDING_ID'"
            )
            sql.execSQL(
                "UPDATE workout_phase_intervals SET end_offset_ms=NULL,end_mutation_sequence=NULL," +
                    "open_marker=1 WHERE id='$OPEN_PHASE_ID'"
            )
            sql.execSQL(
                "UPDATE heart_rate_acquisition_intervals SET end_offset_ms=NULL," +
                    "end_mutation_sequence=NULL,open_marker=1 WHERE id='$OPEN_ACQUISITION_ID'"
            )
            sql.execSQL(
                "UPDATE workout_sessions SET status='active',last_durable_offset_ms=$SESSION_DURATION_MS," +
                    "last_mutation_sequence=$EXPECTED_SEQUENCE,trusted_end_offset_ms=NULL," +
                    "terminal_reason=NULL WHERE id='$SESSION_ID'"
            )
            sql.setTransactionSuccessful()
        } finally {
            sql.endTransaction()
        }
        assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(requireGraph()))
    }

    private suspend fun requireGraph(): CanonicalSessionGraphV1 {
        val rows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows(SESSION_ID))
        val recording = rows.recordings.single()
        return CanonicalSessionGraphV1(
            session = rows.session,
            phases = rows.phases,
            recording = recording.recording,
            acquisitions = recording.acquisitions,
            samples = recording.samples,
            snapshots = recording.snapshots
        )
    }

    private fun currentTotalPssKb(): Int = Debug.MemoryInfo().also(Debug::getMemoryInfo).totalPss

    private fun androidx.sqlite.db.SupportSQLiteDatabase.longForQuery(sql: String): Int =
        query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getInt(0)
        }

    private data class Measurement(
        val result: RecordingFinalizationResult,
        val graph: CanonicalSessionGraphV1,
        val elapsedMs: Long,
        val peakPssBytes: Long,
        val jsonBytes: Long
    )

    private companion object {
        const val PROFILE = "P-BALANCED-V2"
        const val SESSION_ID = "e17-performance-session"
        const val RECORDING_ID = "e17-performance-recording"
        const val PHASE_COUNT = 10_000
        const val ACQUISITION_COUNT = 10_000
        const val SAMPLE_COUNT = 250_000
        const val SAME_OFFSET_BURST = 32
        const val SESSION_DURATION_MS = 28_800_000L
        const val PRIMARY_DURATION_MS = 30_000L
        const val PRIMARY_START = SESSION_DURATION_MS - PRIMARY_DURATION_MS
        const val FIRST_EXCLUDED_END = 3_000L
        const val ZERO_DURATION_INDEX = 20
        const val POSITIVE_PAUSED_INTERVALS = 9_997L
        const val EXCLUDED_SPAN = PRIMARY_START - FIRST_EXCLUDED_END
        const val BURST_OFFSET = 5_000L
        const val BULK_START = 10_000L
        const val EXPECTED_SEQUENCE = 1_000_000L
        const val SNAPSHOT_CREATED_AT = "2026-08-31T00:00:00Z"
        const val WARMUP_RUNS = 2
        const val MEASURED_RUNS = 5
        const val PSS_SAMPLE_INTERVAL_MS = 50L
        const val MAX_TIME_MS = 8_000L
        const val MAX_PSS_BYTES = 384L * 1_024L * 1_024L
        const val MAX_JSON_BYTES = 16L * 1_024L * 1_024L
        val FINAL_TUPLE = CanonicalTuple(SESSION_DURATION_MS, EXPECTED_SEQUENCE + 1)
        val REQUEST = RecordingFinalizationRequest(
            sessionId = SESSION_ID,
            recordingId = RECORDING_ID,
            expectedStatus = "active",
            expectedTuple = CanonicalTuple(SESSION_DURATION_MS, EXPECTED_SEQUENCE),
            finalOffsetMs = SESSION_DURATION_MS,
            terminalStatus = "completed",
            terminalReason = "completed",
            snapshotCreatedAt = SNAPSHOT_CREATED_AT
        )
        const val OPEN_PHASE_ID = "$SESSION_ID:phase:9999"
        const val OPEN_ACQUISITION_ID = "$RECORDING_ID:acquisition:9999"
        const val HIGHEST_FIRST_OFFSET = PRIMARY_START + 9
        const val HIGHEST_FIRST_SAMPLE_SEQUENCE = 42L
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
        val DEVICE_STATES = listOf(
            "not_observing", "no_source_selected", "permission_required", "bluetooth_unavailable",
            "searching", "connecting", "waiting_first_sample", "live", "stale", "reconnecting",
            "disconnected", "technical_failure"
        )
        val DEVICE_REASONS = listOf(
            "initial_acquisition", "automatic_recovery", "source_not_selected", "source_unavailable",
            "permission_missing", "permission_revoked", "bluetooth_off", "platform_unavailable",
            "first_sample_timeout", "sample_stale_timeout", "unexpected_disconnect",
            "connection_timeout", "measurement_stream_unavailable", "platform_failure"
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
    }
}
