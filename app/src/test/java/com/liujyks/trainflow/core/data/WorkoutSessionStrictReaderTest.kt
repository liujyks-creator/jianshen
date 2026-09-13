package com.liujyks.trainflow.core.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.os.Looper
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.dao.WorkoutSessionHeaderRow
import com.liujyks.trainflow.core.database.entity.*
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.core.health.HeartRateBindingDisposition
import com.liujyks.trainflow.core.health.HeartRateRuntimeOwner
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import java.time.Instant
import java.util.Collections
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutSessionStrictReaderTest {
    private lateinit var database: TrainFlowDatabase
    private val queries = Collections.synchronizedList(mutableListOf<Pair<String, List<Any?>>>())
    @Volatile private var intercept: ((String) -> Unit)? = null

    @After
    fun closeDatabase() {
        if (::database.isInitialized) database.close()
    }

    private fun freshDatabase() {
        intercept = null
        if (::database.isInitialized) database.close()
        queries.clear()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), TrainFlowDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCallback(RoomDatabase.QueryCallback { sql, args ->
                queries.add(sql to args.toList())
                intercept?.invoke(sql)
            }, Executor { command -> command.run() })
            .build()
    }

    @Test
    fun readsCanonicalTerminalGraphsWithoutRewriting() = runBlocking {
        for (case in 0..4) {
            freshDatabase()
            val repository = WorkoutSessionRepository(database)
            val expected = seedTerminal(repository, hr = case < 2, raw = case != 1,
                status = if (case < 3) "completed" else "abandoned",
                reason = when (case) { 3 -> "user_abandoned"; 4 -> "process_interrupted"; else -> "completed" },
                endedAt = if (case == 4) null else END)
            val before = databaseSnapshot()
            assertCanonical(expected, repository.readSessionStrict(SESSION_ID))
            assertEquals(before, databaseSnapshot())
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun readsSavedBeforeCleanupReleasesOwner() = runBlocking {
        freshDatabase()
        val repository = WorkoutSessionRepository(database)
        val admission = admitFinalizer(repository)
        val runtime = HeartRateRuntimeOwner(ApplicationProvider.getApplicationContext<Context>())
        val binding = runtime.bindObservations(admission.bindingId) {}
        seedActiveRecording()
        freezeStartMetadata()
        val saved = repository.finalizeCanonicalSession(admission.ownerToken, terminalRequest())
        assertEquals(CanonicalTuple(2_000, 4), saved.finalTuple)
        val expected = terminalExpected()
        val before = databaseSnapshot()
        val first = repository.readSessionStrict(SESSION_ID)
        assertCanonical(expected, first)
        assertEquals(binding, runtime.queryObservationBinding(admission.bindingId))
        assertTrue(runCatching { admitFinalizer(repository, "next", "next-entry") }.exceptionOrNull() is RecorderOwnerBusyException)
        assertEquals(before, databaseSnapshot())
        supervisorScope {
            val releaseScope = this
            val cleanup = withContext(Dispatchers.Default) {
                releaseScope.async(start = CoroutineStart.UNDISPATCHED) {
                    repository.releaseRecorderAfterTerminal(admission.ownerToken, SESSION_ID, runtime)
                }
            }
            assertFalse(cleanup.isCompleted)
            assertFalse(shadowOf(Looper.getMainLooper()).isIdle)
            assertEquals(binding, runtime.queryObservationBinding(admission.bindingId))
            assertEquals(before, databaseSnapshot())
            shadowOf(Looper.getMainLooper()).idle()
            cleanup.await()
        }
        assertEquals(HeartRateBindingDisposition.KnownAbsent, runtime.queryObservationBinding(admission.bindingId))
        val next = admitFinalizer(repository, "next", "next-entry")
        assertTrue(next.ownerToken !== admission.ownerToken)
        assertEquals(first, repository.readSessionStrict(SESSION_ID))
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun selectsOnlyExactOriginalAnalysis() = runBlocking {
        val errors = listOf(null, "invalid_original_analysis", "unsupported_original_analysis_version",
            "invalid_original_analysis", "invalid_session_graph")
        for (case in 0..4) {
            freshDatabase()
            val repository = WorkoutSessionRepository(database)
            val expected = seedTerminal(repository)
            if (case <= 2) {
                database.canonicalTimelineHeartRateDao().insertAnalysisSnapshot(expected.snapshots.single().copy(
                    analysisVersion = 2, createdAt = "2026-09-12T00:00:00Z", analysisConfigJson = "{"))
            }
            when (case) {
                1 -> sql("DELETE FROM heart_rate_analysis_snapshots WHERE analysis_version=1")
                2 -> sql("UPDATE heart_rate_recordings SET original_analysis_version=2")
                3 -> sql("UPDATE heart_rate_recordings SET original_analysis_version=NULL")
                4 -> sql("UPDATE heart_rate_analysis_snapshots SET input_last_mutation_sequence=3")
            }
            val before = databaseSnapshot()
            queries.clear()
            val result = repository.readSessionStrict(SESSION_ID)
            if (case == 0) {
                assertCanonical(expected, result)
                val snapshotQueries = queries.filter { it.first.lowercase().contains("from heart_rate_analysis_snapshots") }
                assertEquals(1, snapshotQueries.size)
                assertEquals("select * from heart_rate_analysis_snapshots where recording_id = ? and analysis_version = ?",
                    snapshotQueries.single().first.lowercase().trim())
                assertEquals(RECORDING_ID, snapshotQueries.single().second[0])
                assertEquals(1, (snapshotQueries.single().second[1] as Number).toInt())
            } else assertEquals(WorkoutSessionStrictReadResult.Unavailable(errors[case]!!), result)
            assertEquals(before, databaseSnapshot())
        }
    }

    @Test
    fun classifiesLegacyAndRunningSessions() = runBlocking {
        for (mode in listOf("timed", "strength", "follow_along")) {
            for (status in listOf("completed", "abandoned")) {
                freshDatabase()
                val session = legacy(mode, status)
                database.workoutSessionDao().insertSession(session)
                val before = databaseSnapshot()
                assertEquals(WorkoutSessionStrictReadResult.LegacyTerminal(session, legacyRoot(mode), EMPTY_EXECUTION),
                    WorkoutSessionRepository(database).readSessionStrict("legacy"))
                assertEquals(before, databaseSnapshot())
            }
        }
        for (status in listOf("ready", "active", "paused")) {
            freshDatabase()
            val session = legacy(status = status)
            database.workoutSessionDao().insertSession(session)
            val before = databaseSnapshot()
            assertEquals(WorkoutSessionStrictReadResult.Nonterminal(session,
                if (status == "ready") "legacy_incomplete_nonterminal" else "legacy_noncanonical_nonterminal"),
                WorkoutSessionRepository(database).readSessionStrict("legacy"))
            assertEquals(before, databaseSnapshot())
        }
        for (status in listOf("active", "paused")) {
            freshDatabase()
            seedActiveRecording(hr = false)
            sql("UPDATE workout_sessions SET status='$status'")
            val before = databaseSnapshot()
            assertEquals(WorkoutSessionStrictReadResult.Nonterminal(activeSession().copy(status = status), "canonical_v1_running"),
                WorkoutSessionRepository(database).readSessionStrict(SESSION_ID))
            assertEquals(before, databaseSnapshot())
        }
    }

    @Test
    fun rejectsInvalidPersistedBoundariesWithoutFallback() = runBlocking {
        val errors = listOf("unsupported_session_mode", "unsupported_session_version", "invalid_session_header",
            "invalid_session_header", "invalid_plan_snapshot", "invalid_plan_snapshot", "invalid_plan_snapshot",
            "invalid_session_graph", "invalid_session_graph", "invalid_session_graph", "invalid_session_graph",
            "invalid_session_graph", "invalid_session_graph", "invalid_session_graph")
        for (case in 0..13) {
            freshDatabase()
            val repository = WorkoutSessionRepository(database)
            val isLegacy = case == 0 || case in 4..8
            if (isLegacy) database.workoutSessionDao().insertSession(legacy()) else seedTerminal(repository)
            when (case) {
                0 -> sql("UPDATE workout_sessions SET mode='unknown'")
                1 -> sql("UPDATE workout_sessions SET timeline_version=2")
                2 -> sql("UPDATE workout_sessions SET display_metadata_contract_version=NULL")
                3 -> sql("UPDATE workout_sessions SET session_display_metadata_json='{'")
                4 -> sql("UPDATE workout_sessions SET plan_snapshot_json='{'")
                5 -> sql("UPDATE workout_sessions SET plan_snapshot_json=?", legacy("strength").planSnapshotJson)
                6 -> sql("UPDATE workout_sessions SET plan_snapshot_json=?", "{\"title\":\"Legacy\",\"mode\":\"timed\",\"blocks\":[],\"planSnapshotStorageContractVersion\":1}")
                7 -> database.canonicalTimelineHeartRateDao().insertRecording(activeRecording().copy(sessionId = "legacy"))
                8 -> database.canonicalTimelineHeartRateDao().insertPhaseInterval(activePhase().copy(sessionId = "legacy"))
                9 -> sql("UPDATE workout_phase_intervals SET phase_identity_json='{'")
                10 -> sql("UPDATE workout_phase_intervals SET end_offset_ms=NULL,end_mutation_sequence=NULL,open_marker=1")
                11 -> sql("UPDATE heart_rate_samples SET bpm=0")
                12 -> sql("UPDATE heart_rate_recordings SET source_contract_version=2")
                13 -> sql("UPDATE heart_rate_analysis_snapshots SET analysis_config_json=replace(analysis_config_json,'\"sampleIntervalContractVersion\":1','\"sampleIntervalContractVersion\":2')")
            }
            val before = databaseSnapshot()
            assertEquals("case ${case + 1}", WorkoutSessionStrictReadResult.Unavailable(errors[case]),
                repository.readSessionStrict(if (isLegacy) "legacy" else SESSION_ID))
            assertEquals(before, databaseSnapshot())
        }
    }

    @Test
    fun preservesFrozenDatesWithoutCurrentZoneRepair() = runBlocking {
        for (case in 0..6) {
            freshDatabase()
            val repository = WorkoutSessionRepository(database)
            val expected = if (case == 2) {
                database.workoutSessionDao().insertSession(legacy())
                null
            } else seedTerminal(repository)
            when (case) {
                1 -> sql("UPDATE workout_sessions SET start_local_date=NULL,start_zone_id=NULL,start_utc_offset_seconds=NULL,time_metadata_source_contract_version=NULL")
                3 -> sql("UPDATE workout_sessions SET start_local_date=NULL")
                4 -> sql("UPDATE workout_sessions SET start_zone_id='not/a-zone'")
                5 -> sql("UPDATE workout_sessions SET start_utc_offset_seconds=0")
                6 -> sql("UPDATE workout_sessions SET time_metadata_source_contract_version=2")
            }
            val before = databaseSnapshot()
            when (case) {
                0 -> {
                    val previous = TimeZone.getDefault()
                    try {
                        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
                        val utc = repository.readSessionStrict(SESSION_ID)
                        assertCanonical(expected!!, utc)
                        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
                        val honolulu = repository.readSessionStrict(SESSION_ID)
                        assertCanonical(expected, honolulu)
                        assertEquals(utc, honolulu)
                    } finally { TimeZone.setDefault(previous) }
                }
                1 -> assertCanonical(expected!!.copy(session = expected.session.copy(startLocalDate = null,
                    startZoneId = null, startUtcOffsetSeconds = null, timeMetadataSourceContractVersion = null)),
                    repository.readSessionStrict(SESSION_ID))
                2 -> assertEquals(WorkoutSessionStrictReadResult.LegacyTerminal(legacy(), legacyRoot("timed"), EMPTY_EXECUTION),
                    repository.readSessionStrict("legacy"))
                else -> assertEquals(WorkoutSessionStrictReadResult.Unavailable("invalid_session_time"), repository.readSessionStrict(SESSION_ID))
            }
            assertEquals(before, databaseSnapshot())
        }
    }

    @Test
    fun preservesAndOrdersEveryExecutionField() = runBlocking {
        freshDatabase()
        val expected = seedExecution()
        val before = databaseSnapshot()
        assertEquals(expected, WorkoutSessionRepository(database).readSessionStrict("legacy"))
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun rejectsInvalidExecutionColumns() = runBlocking {
        val mutations = listOf(
            "", "UPDATE workout_sessions SET started_at='bad'", "UPDATE workout_sessions SET ended_at='bad'",
            "UPDATE workout_sessions SET total_elapsed_sec=-1", "UPDATE workout_sessions SET effective_elapsed_sec=-1",
            "UPDATE workout_sessions SET paused_elapsed_sec=-1",
            "UPDATE session_step_records SET step_id='' WHERE id='row-0'",
            "UPDATE session_step_records SET kind='unknown' WHERE id='row-0'",
            "UPDATE session_step_records SET started_at='bad' WHERE id='row-0'",
            "UPDATE session_step_records SET ended_at='bad' WHERE id='row-0'",
            "UPDATE session_step_records SET actual_duration_sec=-1 WHERE id='row-0'",
            "UPDATE session_step_records SET planned_duration_sec=-1 WHERE id='row-0'",
            "UPDATE timed_rest_extension_records SET id='' WHERE id='e-a'",
            "UPDATE timed_rest_extension_records SET step_id='' WHERE id='e-a'",
            "UPDATE timed_rest_extension_records SET rest_stage_title='' WHERE id='e-a'",
            "UPDATE timed_rest_extension_records SET step_index=-1 WHERE id='e-a'",
            "UPDATE timed_rest_extension_records SET round_index=0 WHERE id='e-a'",
            "UPDATE timed_rest_extension_records SET added_sec=0 WHERE id='e-a'",
            "UPDATE timed_rest_extension_records SET planned_rest_sec=0 WHERE id='e-a'",
            "UPDATE timed_rest_extension_records SET rest_elapsed_before_extension_sec=-1 WHERE id='e-a'",
            "UPDATE timed_rest_extension_records SET extension_at_remaining_sec=-1 WHERE id='e-a'",
            "UPDATE timed_rest_extension_records SET cumulative_extra_rest_sec=4 WHERE id='e-a'",
            "UPDATE timed_rest_extension_records SET event_elapsed_sec=-1 WHERE id='e-a'",
            "UPDATE strength_set_records SET id='' WHERE id='k-a'",
            "UPDATE strength_set_records SET exercise_id='' WHERE id='k-a'",
            "UPDATE strength_set_records SET set_order=-1 WHERE id='k-a'",
            "UPDATE strength_set_records SET set_kind='unknown' WHERE id='k-a'",
            "UPDATE strength_set_records SET side='unknown' WHERE id='k-a'",
            "UPDATE strength_set_records SET effort='unknown' WHERE id='k-a'",
            "UPDATE strength_set_records SET active_duration_sec=-1 WHERE id='k-a'",
            "UPDATE strength_set_records SET actual_rest_after_sec=-1 WHERE id='k-a'")
        for ((case, mutation) in mutations.withIndex()) {
            freshDatabase()
            val id = if (case == 0) "" else "legacy"
            seedExecution(id)
            if (case != 0) sql(mutation)
            val before = databaseSnapshot()
            assertEquals("case ${case + 1}", WorkoutSessionStrictReadResult.Unavailable("invalid_session_execution"),
                WorkoutSessionRepository(database).readSessionStrict(id))
            assertEquals(before, databaseSnapshot())
        }
    }

    @Test
    fun rejectsMalformedStrengthStorageWithoutDroppingFields() = runBlocking {
        val values = listOf("", "", "other=1", "weight=1,kg|weight=2,kg", "weight1,kg", "weight=1",
            "weight=1,kg,x", "weight=1,stone", "weight=abc,kg", "weight=NaN,kg", "weight=Infinity,kg",
            "weight=-1,kg", "rep=fixed,0", "rep=fixed,201", "rep=range,12,8", "rep=unknown,1", "reps=-1", "reps=1.5")
        for ((case, value) in values.withIndex()) {
            freshDatabase()
            seedExecution()
            val column = if (case == 1 || case >= 16) "actual_json" else "planned_json"
            sql("UPDATE strength_set_records SET $column=? WHERE id='k-a'", value)
            val before = databaseSnapshot()
            assertEquals("case ${case + 1}", WorkoutSessionStrictReadResult.Unavailable("invalid_session_execution"),
                WorkoutSessionRepository(database).readSessionStrict("legacy"))
            assertEquals(before, databaseSnapshot())
        }
    }

    @Test
    fun listsOnlyHeadersAndKeepsInvalidSessionsVisible() = runBlocking {
        freshDatabase()
        val repository = WorkoutSessionRepository(database)
        val terminal = seedTerminal(repository, sessionId = "a")
        val b = legacy().copy(id = "b", mode = "unknown", planSnapshotJson = "{", endedAt = END)
        val c = legacy(status = "active").copy(id = "c", startedAt = "2026-09-07T16:00:01Z")
        val d = legacy(status = "ready").copy(id = "d")
        for (session in listOf(b, c, d)) database.workoutSessionDao().insertSession(session)
        val before = databaseSnapshot()
        queries.clear()
        val headers = repository.getSessionHeaders()
        val businessQueries = queries.filter { it.first.trimStart().startsWith("SELECT", ignoreCase = true) &&
            listOf("workout_sessions", "workout_phase_intervals", "heart_rate_", "session_step_records",
                "strength_set_records", "timed_rest_extension_records").any { table -> it.first.lowercase().contains(table) } }
        assertEquals(listOf(header(terminal.session.copy(id = "a")), header(b), header(c), header(d)), headers)
        assertEquals(1, businessQueries.size)
        assertEquals("select id, plan_id, mode, status, started_at, ended_at, total_elapsed_sec, effective_elapsed_sec, " +
            "paused_elapsed_sec, timeline_version, trusted_end_offset_ms, terminal_reason, start_local_date, start_zone_id, " +
            "start_utc_offset_seconds, time_metadata_source_contract_version, last_durable_offset_ms " +
            "from workout_sessions order by coalesce(ended_at, started_at, '') desc, id asc",
            businessQueries.single().first.trim().lowercase().replace(Regex("\\s+"), " "))
        assertEquals(WorkoutSessionStrictReadResult.Unavailable("unsupported_session_mode"), repository.readSessionStrict("b"))
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun deletionInterleavingsNeverReturnCachedRecords() = runBlocking {
        for (case in 0..1) {
            freshDatabase()
            val repository = WorkoutSessionRepository(database)
            val expected = seedTerminal(repository)
            if (case == 0) {
                repository.deleteAllSessions()
                assertEquals(WorkoutSessionStrictReadResult.NotFound, repository.readSessionStrict(SESSION_ID))
            } else {
                val entered = CountDownLatch(1)
                val release = CountDownLatch(1)
                val armed = AtomicBoolean(true)
                intercept = { query ->
                    if (query.lowercase().contains("select") && query.lowercase().contains("from workout_phase_intervals") &&
                        armed.compareAndSet(true, false)) {
                        entered.countDown()
                        check(release.await(5, TimeUnit.SECONDS))
                    }
                }
                val read = async(Dispatchers.IO) { repository.readSessionStrict(SESSION_ID) }
                val deletion = try {
                    assertTrue(entered.await(5, TimeUnit.SECONDS))
                    async(start = CoroutineStart.UNDISPATCHED) { repository.deleteAllSessions() }.also { assertFalse(it.isCompleted) }
                } finally { release.countDown() }
                assertCanonical(expected, withTimeout(5_000) { read.await() })
                withTimeout(5_000) { deletion.await() }
                intercept = null
                assertEquals(WorkoutSessionStrictReadResult.NotFound, repository.readSessionStrict(SESSION_ID))
                assertEquals(emptyList<WorkoutSessionHeaderRow>(), repository.getSessionHeaders())
            }
        }
    }

    @Test
    fun concurrentFinalizationExposesOnlyCommittedState() = runBlocking {
        freshDatabase()
        val referenceRepository = WorkoutSessionRepository(database)
        val referenceAdmission = admitFinalizer(referenceRepository)
        seedActiveRecording()
        referenceRepository.finalizeCanonicalSession(referenceAdmission.ownerToken, terminalRequest())
        val expected = terminalExpected(frozen = false)
        for (rollback in listOf(false, true)) {
            freshDatabase()
            val repository = WorkoutSessionRepository(database)
            val admission = admitFinalizer(repository)
            seedActiveRecording()
            val before = databaseSnapshot()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val armed = AtomicBoolean(true)
            if (rollback) sql("CREATE TRIGGER fail_strict_reader BEFORE INSERT ON heart_rate_analysis_snapshots BEGIN SELECT RAISE(ABORT,'strict_reader_rollback'); END")
            intercept = { query ->
                if (query.lowercase().contains("update workout_sessions") && query.lowercase().contains("set ended_at =") &&
                    armed.compareAndSet(true, false)) {
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                }
            }
            val write = async(Dispatchers.IO) { runCatching { repository.finalizeCanonicalSession(admission.ownerToken, terminalRequest()) } }
            val read = try {
                assertTrue(entered.await(5, TimeUnit.SECONDS))
                async(start = CoroutineStart.UNDISPATCHED) { repository.readSessionStrict(SESSION_ID) }.also { assertFalse(it.isCompleted) }
            } finally { release.countDown() }
            val written = withTimeout(5_000) { write.await() }
            val result = withTimeout(5_000) { read.await() }
            intercept = null
            if (rollback) {
                assertTrue(written.exceptionOrNull() is SQLiteConstraintException)
                assertEquals(WorkoutSessionStrictReadResult.Nonterminal(activeSession(), "canonical_v1_running"), result)
                assertEquals(before, databaseSnapshot())
            } else {
                assertEquals(CanonicalTuple(2_000, 4), written.getOrThrow().finalTuple)
                assertCanonical(expected, result)
            }
        }
    }

    private suspend fun admitFinalizer(repository: WorkoutSessionRepository, sessionId: String = SESSION_ID,
        entryId: String = "terminal-entry"): RecorderAdmission = repository.admitRecorder(entryId,
        activeSession().copy(id = sessionId, lastDurableOffsetMs = 0, lastMutationSequence = 0),
        activePhase().copy(sessionId = sessionId))

    private fun activeSession() = WorkoutSessionEntity(SESSION_ID, mode = "timed", status = "active",
        planSnapshotJson = VALID_PLAN_SNAPSHOT, timelineVersion = 1, lastDurableOffsetMs = 1_000,
        lastMutationSequence = 3, displayMetadataContractVersion = 1, sessionDisplayMetadataJson = VALID_DISPLAY_METADATA)

    private fun activePhase() = WorkoutPhaseIntervalEntity(PHASE_ID, SESSION_ID, 0, 0, null, 0, null, 1,
        "timed_work", VALID_PHASE_IDENTITY)

    private fun activeRecording() = HeartRateRecordingEntity(RECORDING_ID, SESSION_ID, "active", 0, 0, null, null,
        sourceContractVersion = 1, sourceKind = "ble_hrs", acquisitionContractVersion = 1, parameterSnapshotVersion = 1)

    private fun activeAcquisition() = HeartRateAcquisitionIntervalEntity(ACQUISITION_ID, RECORDING_ID, 0, 0, null, 0,
        null, 1, "expected_recording", null, "live", null)

    private suspend fun seedActiveRecording(hr: Boolean = true, raw: Boolean = true, sessionId: String = SESSION_ID) {
        assertTrue(database.workoutSessionDao().insertSession(activeSession().copy(id = sessionId)) != -1L)
        val dao = database.canonicalTimelineHeartRateDao()
        dao.insertPhaseInterval(activePhase().copy(sessionId = sessionId))
        if (hr) {
            dao.insertRecording(activeRecording().copy(sessionId = sessionId))
            dao.insertAcquisitionInterval(activeAcquisition())
            if (raw) dao.insertSample(HeartRateSampleEntity(RECORDING_ID, 0, 0, 0, 120))
        }
    }

    private fun freezeStartMetadata(sessionId: String = SESSION_ID) {
        sql("UPDATE workout_sessions SET started_at='2026-09-07T16:00:00Z',start_local_date='2026-09-08'," +
            "start_zone_id='Asia/Shanghai',start_utc_offset_seconds=28800,time_metadata_source_contract_version=1 WHERE id=?", sessionId)
    }

    private fun terminalRequest(hr: Boolean = true, status: String = "completed", reason: String = "completed",
        endedAt: String? = END, sessionId: String = SESSION_ID) = FrozenCanonicalFinalizationRequest(
        RecorderExpectedState(sessionId, "active", CanonicalTuple(1_000, 3), PHASE_ID,
            if (hr) RECORDING_ID else null, if (hr) ACQUISITION_ID else null),
        2_000, status, reason, endedAt, 12, 9, 3, VALID_DISPLAY_METADATA,
        terminalSteps().map { it.copy(sessionId = sessionId) }, emptyList(), emptyList(),
        "2026-08-31T00:00:00Z")

    private fun terminalSteps() = listOf(SessionStepRecordEntity("execution-step", SESSION_ID, "block:warmup:target",
        "timed_work", "block", "item", null, null, START, END, true, 2, 10))

    private suspend fun seedTerminal(repository: WorkoutSessionRepository, hr: Boolean = true, raw: Boolean = true,
        status: String = "completed", reason: String = "completed", endedAt: String? = END,
        sessionId: String = SESSION_ID): CanonicalSessionGraphV1 {
        val admission = admitFinalizer(repository, sessionId)
        seedActiveRecording(hr, raw, sessionId)
        freezeStartMetadata(sessionId)
        if (reason == "process_interrupted") {
            sql("UPDATE workout_sessions SET status='abandoned',ended_at=NULL,total_elapsed_sec=12,effective_elapsed_sec=9," +
                "paused_elapsed_sec=3,last_durable_offset_ms=2000,last_mutation_sequence=4,trusted_end_offset_ms=2000,terminal_reason='process_interrupted'")
            sql("UPDATE workout_phase_intervals SET end_offset_ms=2000,end_mutation_sequence=4,open_marker=NULL")
            database.workoutSessionDao().upsertStepRecords(terminalSteps())
        } else {
            repository.finalizeCanonicalSession(admission.ownerToken, terminalRequest(hr, status, reason, endedAt, sessionId))
        }
        val expected = terminalExpected(hr, raw, status, reason, endedAt)
        return expected.copy(session = expected.session.copy(id = sessionId),
            phases = expected.phases.map { it.copy(sessionId = sessionId) },
            recording = expected.recording?.copy(sessionId = sessionId))
    }

    private suspend fun terminalExpected(hr: Boolean = true, raw: Boolean = true, status: String = "completed",
        reason: String = "completed", endedAt: String? = END, frozen: Boolean = true): CanonicalSessionGraphV1 {
        // Only analysis payload is frozen from the independent DAO; all other fields are fixed input facts.
        val snapshots = if (hr) database.canonicalTimelineHeartRateDao().snapshotsInVersionOrder(RECORDING_ID) else emptyList()
        return CanonicalSessionGraphV1(activeSession().copy(status = status, startedAt = if (frozen) START else null,
            endedAt = endedAt, totalElapsedSec = 12, effectiveElapsedSec = 9, pausedElapsedSec = 3,
            lastDurableOffsetMs = 2_000, lastMutationSequence = 4, trustedEndOffsetMs = 2_000, terminalReason = reason,
            startLocalDate = if (frozen) "2026-09-08" else null, startZoneId = if (frozen) "Asia/Shanghai" else null,
            startUtcOffsetSeconds = if (frozen) 28_800L else null, timeMetadataSourceContractVersion = if (frozen) 1L else null),
            listOf(activePhase().copy(endOffsetMs = 2_000, endMutationSequence = 4, openMarker = null)),
            if (hr) activeRecording().copy(status = "terminal", endedOffsetMs = 2_000, endedMutationSequence = 4, originalAnalysisVersion = 1) else null,
            if (hr) listOf(activeAcquisition().copy(endOffsetMs = 2_000, endMutationSequence = 4, openMarker = null)) else emptyList(),
            if (hr && raw) listOf(HeartRateSampleEntity(RECORDING_ID, 0, 0, 0, 120)) else emptyList(), snapshots)
    }

    private fun assertCanonical(expected: CanonicalSessionGraphV1, result: WorkoutSessionStrictReadResult) {
        assertTrue("Expected canonical terminal, got $result", result is WorkoutSessionStrictReadResult.CanonicalTerminal)
        result as WorkoutSessionStrictReadResult.CanonicalTerminal
        assertEquals(expected, result.graph)
        assertEquals(parseCanonicalJson(VALID_PLAN_SNAPSHOT), result.planRoot)
        assertEquals(StrictSessionExecution(terminalSteps(), emptyList(), emptyList()), result.execution)
        assertArrayEquals(expected.session.planSnapshotJson.toByteArray(Charsets.UTF_8), result.graph.session.planSnapshotJson.toByteArray(Charsets.UTF_8))
        assertArrayEquals(expected.session.sessionDisplayMetadataJson!!.toByteArray(Charsets.UTF_8), result.graph.session.sessionDisplayMetadataJson!!.toByteArray(Charsets.UTF_8))
        for ((input, output) in expected.snapshots.zip(result.graph.snapshots)) {
            assertEquals(input, output)
            assertArrayEquals(input.analysisConfigJson.toByteArray(Charsets.UTF_8), output.analysisConfigJson.toByteArray(Charsets.UTF_8))
            assertArrayEquals(input.phaseAggregatesJson.toByteArray(Charsets.UTF_8), output.phaseAggregatesJson.toByteArray(Charsets.UTF_8))
            assertArrayEquals(input.durationBreakdownJson.toByteArray(Charsets.UTF_8), output.durationBreakdownJson.toByteArray(Charsets.UTF_8))
            assertArrayEquals(input.qualityReasonsJson.toByteArray(Charsets.UTF_8), output.qualityReasonsJson.toByteArray(Charsets.UTF_8))
        }
    }

    private fun legacy(mode: String = "timed", status: String = "completed") = WorkoutSessionEntity("legacy",
        mode = mode, status = status, planSnapshotJson = "{\"title\":\"Legacy\",\"mode\":\"$mode\",\"blocks\":[]}")

    private fun legacyRoot(mode: String) = CanonicalJsonValue.Obj(linkedMapOf("title" to CanonicalJsonValue.Str("Legacy"),
        "mode" to CanonicalJsonValue.Str(mode), "blocks" to CanonicalJsonValue.Arr(emptyList())))

    private suspend fun seedExecution(id: String = "legacy"): WorkoutSessionStrictReadResult.LegacyTerminal {
        val session = legacy("strength").copy(id = id, startedAt = START, endedAt = "2026-09-07T16:00:12Z",
            totalElapsedSec = 12, effectiveElapsedSec = 9, pausedElapsedSec = 3)
        val kinds = listOf("prepare", "timed_work", "timed_rest", "strength_prepare_set", "strength_active_set",
            "strength_confirm_set", "strength_rest", "stretch", "completed")
        val steps = kinds.mapIndexed { i, kind ->
            val start = when (i) { 0 -> "2026-09-08T00:00:00+08:00"; 1 -> START; else -> "2026-09-07T16:00:0${i}Z" }
            SessionStepRecordEntity("row-$i", id, when (i) { 0 -> "s-b"; 1 -> "s-a"; else -> "s-$i" }, kind,
                if (i % 2 == 0) "block" else null, if (i % 2 == 0) "item" else null,
                if (i % 2 == 0) "set" else null, if (i % 2 == 0) "exercise" else null,
                start, if (i < 2) null else Instant.parse(start).plusSeconds(1).toString(), i % 2 == 0,
                when (i) { 0 -> 0; 1 -> null; else -> 1 }, if (i == 0) null else 10)
        }
        val extensionKeys = listOf(Triple(2, 0, 5), Triple(1, 1, 5), Triple(1, 0, 6), Triple(1, 0, 5))
        val extensions = listOf("e-d", "e-c", "e-b", "e-a").mapIndexed { i, recordId ->
            val (event, index, cumulative) = extensionKeys[i]
            TimedRestExtensionRecordEntity(recordId, id, "rest", index, if (i == 3) 1 else null,
                if (i == 3) "stage" else null, "休息", if (i == 3) "prev" else null, if (i == 3) "前阶段" else null,
                5, 10, 0, 10, cumulative, event)
        }
        val texts = listOf(null to null, "weight=0.0,kg|rep=fixed,1" to "weight=0.0,kg|reps=0",
            "weight=60.0,lb|rep=range,1,200" to "weight=62.5,lb|reps=9", "rep=fixed,200" to "reps=12",
            "weight=1.5,kg" to "weight=1.5,kg", "rep=range,8,12|weight=60.0,kg" to "reps=9|weight=62.5,kg",
            "rep=range,1,1" to "reps=0", "rep=range,200,200" to null)
        val sets = listOf("k-b", "k-c", "k-a", "k-d", "k-e", "k-f", "k-g", "k-h").mapIndexed { i, recordId ->
            StrengthSetRecordEntity(recordId, id, "exercise", if (i % 2 == 0) "set" else null,
                listOf(1, 0, 0, 2, 3, 4, 5, 6)[i], listOf("warmup", "working", "drop", "backoff")[i % 4],
                listOf("both", "left", "right", "alternating", null, null, null, null)[i], texts[i].first, texts[i].second,
                when (i) { 0 -> 0; 1 -> null; else -> 7 }, if (i == 0) null else 0,
                listOf("easy", "good", "hard", "form_breakdown", null, null, null, null)[i],
                if (i % 2 == 0) "old" else null, if (i % 2 == 0) "原始备注" else null)
        }
        val dao = database.workoutSessionDao()
        dao.insertSession(session)
        dao.upsertStepRecords(steps)
        dao.upsertTimedRestExtensionRecords(extensions)
        dao.upsertStrengthSetRecords(sets)
        val decoded = listOf(
            StrictStrengthSetRecord(sets[0], null, null, null, null),
            StrictStrengthSetRecord(sets[1], WeightValue(0.0, WeightUnit.KG), RepTarget.Fixed(1), WeightValue(0.0, WeightUnit.KG), 0),
            StrictStrengthSetRecord(sets[2], WeightValue(60.0, WeightUnit.LB), RepTarget.Range(1, 200), WeightValue(62.5, WeightUnit.LB), 9),
            StrictStrengthSetRecord(sets[3], null, RepTarget.Fixed(200), null, 12),
            StrictStrengthSetRecord(sets[4], WeightValue(1.5, WeightUnit.KG), null, WeightValue(1.5, WeightUnit.KG), null),
            StrictStrengthSetRecord(sets[5], WeightValue(60.0, WeightUnit.KG), RepTarget.Range(8, 12), WeightValue(62.5, WeightUnit.KG), 9),
            StrictStrengthSetRecord(sets[6], null, RepTarget.Range(1, 1), null, 0),
            StrictStrengthSetRecord(sets[7], null, RepTarget.Range(200, 200), null, null))
        return WorkoutSessionStrictReadResult.LegacyTerminal(session, legacyRoot("strength"), StrictSessionExecution(
            listOf(steps[1], steps[0]) + steps.drop(2), listOf(extensions[3], extensions[2], extensions[1], extensions[0]),
            listOf(decoded[2], decoded[1], decoded[0]) + decoded.drop(3)))
    }

    private fun header(row: WorkoutSessionEntity) = WorkoutSessionHeaderRow(row.id, row.planId, row.mode, row.status,
        row.startedAt, row.endedAt, row.totalElapsedSec, row.effectiveElapsedSec, row.pausedElapsedSec, row.timelineVersion,
        row.trustedEndOffsetMs, row.terminalReason, row.startLocalDate, row.startZoneId, row.startUtcOffsetSeconds,
        row.timeMetadataSourceContractVersion, row.lastDurableOffsetMs)

    private fun sql(statement: String, vararg args: Any) {
        database.openHelper.writableDatabase.execSQL(statement, args)
    }

    private fun databaseSnapshot(): List<List<List<Any?>>> = listOf(
        "workout_sessions" to "id", "workout_phase_intervals" to "id", "heart_rate_recordings" to "recording_id",
        "heart_rate_acquisition_intervals" to "id", "heart_rate_samples" to "recording_id,sample_sequence",
        "heart_rate_analysis_snapshots" to "recording_id,analysis_version", "session_step_records" to "id",
        "timed_rest_extension_records" to "id", "strength_set_records" to "id"
    ).map { (table, key) ->
        database.openHelper.writableDatabase.query("SELECT * FROM $table ORDER BY $key").use { cursor ->
            buildList {
                while (cursor.moveToNext()) add((0 until cursor.columnCount).map { column ->
                    when (cursor.getType(column)) {
                        Cursor.FIELD_TYPE_NULL -> null
                        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(column)
                        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(column)
                        Cursor.FIELD_TYPE_STRING -> cursor.getString(column)
                        else -> error("Unexpected persisted column type")
                    }
                })
            }
        }
    }

    private companion object {
        const val SESSION_ID = "finalizer-session"
        const val RECORDING_ID = "finalizer-recording"
        const val PHASE_ID = "finalizer-phase"
        const val ACQUISITION_ID = "finalizer-acquisition"
        const val START = "2026-09-07T16:00:00Z"
        const val END = "2026-09-07T16:00:02Z"
        val EMPTY_EXECUTION = StrictSessionExecution(emptyList(), emptyList(), emptyList())
        const val VALID_DISPLAY_METADATA = "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
        const val VALID_PLAN_SNAPSHOT = "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Timed\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"block\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[]}],\"preferences\":null,\"followAlong\":null}"
        const val VALID_PHASE_IDENTITY = "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"timed_work\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855\"},\"payload\":{\"variant\":\"warmup\",\"compositionVersion\":2,\"compositionBlockId\":\"block\",\"timelineStageId\":\"block:warmup\",\"timelineStageKind\":\"warmup\",\"stageGroupId\":\"block:warmup\",\"targetId\":\"block:warmup:target\",\"targetKind\":\"warmup\",\"roundIndex0\":null,\"stageGroupIndex0\":null,\"targetIndex0\":0,\"stageInstanceIndex0\":0,\"targetInstanceIndex0\":0,\"stepIndex0\":0}}"
    }
}
