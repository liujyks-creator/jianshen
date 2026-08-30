package com.liujyks.trainflow.core.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.AnalysisSnapshotV1Validator
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1Validator
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.CanonicalValidationResult
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutSessionFinalizerTest {
    private lateinit var database: TrainFlowDatabase

    @Before
    fun createDatabase() {
        openDatabase()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun requestPreflightAcceptsOnlyFourPairsAndRejectsInvalidFieldsBeforeWrites() = runBlocking {
        seedActiveRecording()
        val before = databaseSnapshot()
        val invalidRequests = listOf(
            request(terminalStatus = "completed", terminalReason = "user_abandoned"),
            request(terminalStatus = "abandoned", terminalReason = "completed"),
            request(terminalStatus = "active", terminalReason = "completed"),
            request(terminalStatus = "abandoned", terminalReason = "other")
        )
        invalidRequests.forEach { invalid ->
            val failure = runCatching {
                WorkoutSessionRepository(database).finalizeRecordingSession(invalid)
            }.exceptionOrNull()
            assertTrue(failure is RecorderValidationException)
            assertEquals("invalid_terminal_status_reason_v1", (failure as RecorderValidationException).code)
            assertEquals(before, databaseSnapshot())
        }

        val emptyCreatedAt = runCatching {
            WorkoutSessionRepository(database).finalizeRecordingSession(request(snapshotCreatedAt = ""))
        }.exceptionOrNull()
        assertTrue(emptyCreatedAt is RecorderValidationException)
        assertEquals("invalid_snapshot_created_at_v1", (emptyCreatedAt as RecorderValidationException).code)
        assertEquals(before, databaseSnapshot())

        val backwards = runCatching {
            WorkoutSessionRepository(database).finalizeRecordingSession(request(finalOffsetMs = 999))
        }.exceptionOrNull()
        assertTrue(backwards is RecorderValidationException)
        assertEquals("invalid_final_tuple_v1", (backwards as RecorderValidationException).code)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun eachAcceptedTerminalPairCommitsTheFixedBoundSnapshotTransaction() = runBlocking {
        val pairs = listOf(
            "completed" to "completed",
            "abandoned" to "user_abandoned",
            "abandoned" to "owner_cleared",
            "abandoned" to "process_interrupted"
        )
        pairs.forEachIndexed { index, (status, reason) ->
            if (index > 0) resetDatabase()
            seedActiveRecording()
            val result = WorkoutSessionRepository(database).finalizeRecordingSession(
                request(terminalStatus = status, terminalReason = reason)
            )

            assertEquals(SESSION_ID, result.sessionId)
            assertEquals(RECORDING_ID, result.recordingId)
            assertEquals(CanonicalTuple(2_000, 4), result.finalTuple)
            assertEquals(1, result.analysisVersion)

            val graph = requireGraph()
            assertEquals(status, graph.session.status)
            assertEquals(reason, graph.session.terminalReason)
            assertEquals(2_000L, graph.session.lastDurableOffsetMs)
            assertEquals(2_000L, graph.session.trustedEndOffsetMs)
            assertEquals(4L, graph.session.lastMutationSequence)
            assertEquals(null, graph.phases.single().openMarker)
            assertEquals(2_000L, graph.phases.single().endOffsetMs)
            assertEquals(4L, graph.phases.single().endMutationSequence)
            assertEquals("terminal", graph.recording?.status)
            assertEquals(2_000L, graph.recording?.endedOffsetMs)
            assertEquals(4L, graph.recording?.endedMutationSequence)
            assertEquals(1, graph.recording?.originalAnalysisVersion)
            assertEquals(null, graph.acquisitions.single().openMarker)
            assertEquals(2_000L, graph.acquisitions.single().endOffsetMs)
            assertEquals(1, graph.snapshots.size)
            assertEquals(SNAPSHOT_CREATED_AT, graph.snapshots.single().createdAt)
            assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(graph))
            assertEquals(
                CanonicalValidationResult.Valid,
                AnalysisSnapshotV1Validator.validate(graph, graph.snapshots.single())
            )
        }
    }

    @Test
    fun identityStatusAndTupleSubstitutionsFailBeforeFirstWriteWithExactNoMutation() = runBlocking {
        seedActiveRecording()
        val before = databaseSnapshot()
        val substitutions = listOf(
            request(sessionId = "other-session"),
            request(recordingId = "other-recording"),
            request(expectedStatus = "paused"),
            request(expectedTuple = CanonicalTuple(1_000, 2)),
            request(expectedTuple = CanonicalTuple(1_001, 3))
        )
        substitutions.forEachIndexed { index, substituted ->
            val failure = runCatching {
                WorkoutSessionRepository(database).finalizeRecordingSession(substituted)
            }.exceptionOrNull()
            assertTrue("substitution $index returned $failure", failure is RecorderGuardedWriteException)
            assertEquals(before, databaseSnapshot())
        }
    }

    @Test
    fun everyLateFailureRollsBackAllThirteenRoomTablesAndPreservesItsTypedSignal() = runBlocking {
        seedActiveRecording()
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER force_snapshot_conflict
            AFTER UPDATE OF status ON workout_sessions
            WHEN NEW.id='$SESSION_ID' AND NEW.status='completed'
            BEGIN
                INSERT INTO heart_rate_analysis_snapshots VALUES(
                    '$RECORDING_ID',1,'conflict',4,'no_canonical_samples','no_eligible_duration',
                    'unavailable_no_effective_max',0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,
                    '{}',NULL,'{}','{}','{}'
                );
            END
            """.trimIndent()
        )
        var before = databaseSnapshot()
        var failure = runCatching {
            WorkoutSessionRepository(database).finalizeRecordingSession(request())
        }.exceptionOrNull()
        assertTrue(failure is SQLiteConstraintException)
        assertEquals(before, databaseSnapshot())

        resetDatabase()
        seedActiveRecording()
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER force_binding_zero
            AFTER INSERT ON heart_rate_analysis_snapshots
            BEGIN
                UPDATE heart_rate_recordings
                SET original_analysis_version=2
                WHERE recording_id='$RECORDING_ID';
            END
            """.trimIndent()
        )
        before = databaseSnapshot()
        failure = runCatching {
            WorkoutSessionRepository(database).finalizeRecordingSession(request())
        }.exceptionOrNull()
        assertGuard(failure, "bind_original_analysis", 0)
        assertEquals(before, databaseSnapshot())

        resetDatabase()
        seedActiveRecording()
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER force_post_write_analysis_failure
            AFTER UPDATE OF original_analysis_version ON heart_rate_recordings
            WHEN NEW.recording_id='$RECORDING_ID' AND NEW.original_analysis_version=1
            BEGIN
                UPDATE heart_rate_analysis_snapshots
                SET quality_reasons_json='{}'
                WHERE recording_id='$RECORDING_ID' AND analysis_version=1;
            END
            """.trimIndent()
        )
        before = databaseSnapshot()
        failure = runCatching {
            WorkoutSessionRepository(database).finalizeRecordingSession(request())
        }.exceptionOrNull()
        assertTrue(failure is RecorderValidationException)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun phaseAcquisitionRecordingAndSessionGuardZeroEachRollbackTheWholeTransaction() = runBlocking {
        val cases = listOf(
            """
            CREATE TRIGGER force_phase_zero
            BEFORE UPDATE OF end_offset_ms ON workout_phase_intervals
            WHEN OLD.id='$PHASE_ID'
            BEGIN DELETE FROM workout_phase_intervals WHERE id=OLD.id; END
            """.trimIndent() to "finalize_close_open_phase",
            """
            CREATE TRIGGER force_acquisition_zero
            AFTER UPDATE OF end_offset_ms ON workout_phase_intervals
            WHEN NEW.id='$PHASE_ID'
            BEGIN DELETE FROM heart_rate_acquisition_intervals WHERE id='$ACQUISITION_ID'; END
            """.trimIndent() to "finalize_close_open_acquisition",
            """
            CREATE TRIGGER force_recording_zero
            AFTER UPDATE OF end_offset_ms ON heart_rate_acquisition_intervals
            WHEN NEW.id='$ACQUISITION_ID'
            BEGIN DELETE FROM heart_rate_recordings WHERE recording_id='$RECORDING_ID'; END
            """.trimIndent() to "finalize_terminalize_recording",
            """
            CREATE TRIGGER force_session_zero
            AFTER UPDATE OF status ON heart_rate_recordings
            WHEN NEW.recording_id='$RECORDING_ID' AND NEW.status='terminal'
            BEGIN UPDATE workout_sessions SET status='paused' WHERE id='$SESSION_ID'; END
            """.trimIndent() to "finalize_terminalize_session"
        )
        cases.forEachIndexed { index, (trigger, guard) ->
            if (index > 0) resetDatabase()
            seedActiveRecording()
            database.openHelper.writableDatabase.execSQL(trigger)
            val before = databaseSnapshot()
            val failure = runCatching {
                WorkoutSessionRepository(database).finalizeRecordingSession(request())
            }.exceptionOrNull()
            assertGuard(failure, guard, 0)
            assertEquals("guard $guard mutated rows", before, databaseSnapshot())
        }
    }

    @Test
    fun twoRepositoriesHaveExactlyOneCommitWinnerAndFreshReentryCannotDuplicateBinding() = runBlocking {
        seedActiveRecording()
        val results = coroutineScope {
            val first = async(Dispatchers.IO) {
                runCatching { WorkoutSessionRepository(database).finalizeRecordingSession(request()) }
            }
            val second = async(Dispatchers.IO) {
                runCatching { WorkoutSessionRepository(database).finalizeRecordingSession(request()) }
            }
            listOf(first.await(), second.await())
        }

        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, results.count { it.isFailure })
        assertTrue(
            results.single { it.isFailure }.exceptionOrNull() is RecorderGuardedWriteException ||
                results.single { it.isFailure }.exceptionOrNull() is RecorderValidationException
        )
        val committed = databaseSnapshot()
        assertEquals(1, database.canonicalTimelineHeartRateDao().analysisSnapshotCount())
        assertEquals(1, requireGraph().recording?.originalAnalysisVersion)

        val reentry = runCatching {
            WorkoutSessionRepository(database).finalizeRecordingSession(request())
        }.exceptionOrNull()
        assertTrue(reentry is RecorderGuardedWriteException || reentry is RecorderValidationException)
        assertEquals(committed, databaseSnapshot())
        assertEquals(1, database.canonicalTimelineHeartRateDao().analysisSnapshotCount())
    }

    @Test
    fun finalTupleSequenceOverflowPropagatesArithmeticExceptionAndRollsBack() = runBlocking {
        seedActiveRecording(expectedSequence = Long.MAX_VALUE)
        val before = databaseSnapshot()
        val failure = runCatching {
            WorkoutSessionRepository(database).finalizeRecordingSession(
                request(expectedTuple = CanonicalTuple(1_000, Long.MAX_VALUE))
            )
        }.exceptionOrNull()
        assertTrue(failure is ArithmeticException)
        assertEquals(before, databaseSnapshot())
    }

    private fun openDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TrainFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    private fun resetDatabase() {
        database.close()
        openDatabase()
    }

    private suspend fun seedActiveRecording(expectedSequence: Long = 3) {
        val session = WorkoutSessionEntity(
            id = SESSION_ID,
            mode = "timed",
            status = "active",
            planSnapshotJson = VALID_PLAN_SNAPSHOT,
            timelineVersion = 1,
            lastDurableOffsetMs = 1_000,
            lastMutationSequence = expectedSequence,
            displayMetadataContractVersion = 1,
            sessionDisplayMetadataJson = VALID_DISPLAY_METADATA
        )
        assertTrue(database.workoutSessionDao().insertSession(session) != -1L)
        database.canonicalTimelineHeartRateDao().insertPhaseInterval(
            WorkoutPhaseIntervalEntity(
                id = PHASE_ID,
                sessionId = SESSION_ID,
                sequence = 0,
                startOffsetMs = 0,
                endOffsetMs = null,
                startMutationSequence = 0,
                endMutationSequence = null,
                openMarker = 1,
                phaseKind = "timed_work",
                phaseIdentityJson = VALID_PHASE_IDENTITY
            )
        )
        database.canonicalTimelineHeartRateDao().insertRecording(
            HeartRateRecordingEntity(
                recordingId = RECORDING_ID,
                sessionId = SESSION_ID,
                status = "active",
                startedOffsetMs = 0,
                startedMutationSequence = 0,
                endedOffsetMs = null,
                endedMutationSequence = null,
                sourceContractVersion = 1,
                sourceKind = "ble_hrs",
                acquisitionContractVersion = 1,
                parameterSnapshotVersion = 1
            )
        )
        database.canonicalTimelineHeartRateDao().insertAcquisitionInterval(
            HeartRateAcquisitionIntervalEntity(
                id = ACQUISITION_ID,
                recordingId = RECORDING_ID,
                sequence = 0,
                startOffsetMs = 0,
                endOffsetMs = null,
                startMutationSequence = 0,
                endMutationSequence = null,
                openMarker = 1,
                recordingIntent = "expected_recording",
                intentReason = null,
                deviceState = "live",
                deviceReason = null
            )
        )
        database.canonicalTimelineHeartRateDao().insertSample(
            HeartRateSampleEntity(RECORDING_ID, 0, 0, 0, 120)
        )
        assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(requireGraph()))
    }

    private fun request(
        sessionId: String = SESSION_ID,
        recordingId: String = RECORDING_ID,
        expectedStatus: String = "active",
        expectedTuple: CanonicalTuple = CanonicalTuple(1_000, 3),
        finalOffsetMs: Long = 2_000,
        terminalStatus: String = "completed",
        terminalReason: String = "completed",
        snapshotCreatedAt: String = SNAPSHOT_CREATED_AT
    ) = RecordingFinalizationRequest(
        sessionId = sessionId,
        recordingId = recordingId,
        expectedStatus = expectedStatus,
        expectedTuple = expectedTuple,
        finalOffsetMs = finalOffsetMs,
        terminalStatus = terminalStatus,
        terminalReason = terminalReason,
        snapshotCreatedAt = snapshotCreatedAt
    )

    private suspend fun requireGraph(): CanonicalSessionGraphV1 {
        val rows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows(SESSION_ID))
        assertTrue(rows.recordings.size <= 1)
        val recording = rows.recordings.singleOrNull()
        return CanonicalSessionGraphV1(
            session = rows.session,
            phases = rows.phases,
            recording = recording?.recording,
            acquisitions = recording?.acquisitions.orEmpty(),
            samples = recording?.samples.orEmpty(),
            snapshots = recording?.snapshots.orEmpty()
        )
    }

    private fun assertGuard(failure: Throwable?, guard: String, rowCount: Int) {
        assertNotNull("expected $guard rowCount=$rowCount", failure)
        assertTrue("expected RecorderGuardedWriteException but was $failure", failure is RecorderGuardedWriteException)
        failure as RecorderGuardedWriteException
        assertEquals(guard, failure.guard)
        assertEquals(rowCount, failure.actualRowCount)
    }

    private fun databaseSnapshot(): List<String> {
        val sql = database.openHelper.writableDatabase
        val tables = sql.query(
            """
            SELECT name FROM sqlite_master
            WHERE type='table'
              AND name NOT GLOB 'sqlite_*'
              AND name NOT IN ('android_metadata','room_master_table')
            ORDER BY name
            """.trimIndent()
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
        check(tables == EXPECTED_USER_TABLES) { "Room user-table manifest changed: $tables" }
        return tables.flatMap { table ->
            val quoted = quoteSqlIdentifier(table)
            val columns = sql.query("PRAGMA table_info($quoted)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            SnapshotColumn(
                                name = cursor.getString(1),
                                declaredType = cursor.getString(2),
                                notNull = cursor.getInt(3) == 1,
                                primaryKeyOrder = cursor.getInt(5)
                            )
                        )
                    }
                }
            }.sortedBy { it.name }
            val primaryKey = columns.filter { it.primaryKeyOrder > 0 }.sortedBy { it.primaryKeyOrder }
            check(columns.isNotEmpty() && primaryKey.isNotEmpty())
            val select = columns.joinToString(", ") { quoteSqlIdentifier(it.name) }
            val order = primaryKey.joinToString(", ") { quoteSqlIdentifier(it.name) }
            buildList {
                add("$table|schema|" + columns.joinToString("|") {
                    "${it.name}:${it.declaredType}:${it.notNull}:${it.primaryKeyOrder}"
                })
                sql.query("SELECT $select FROM $quoted ORDER BY $order").use { cursor ->
                    while (cursor.moveToNext()) {
                        add("$table|row|" + columns.mapIndexed { index, column ->
                            "${column.name}=${cursor.snapshotValue(index)}"
                        }.joinToString("|"))
                    }
                }
            }
        }
    }

    private data class SnapshotColumn(
        val name: String,
        val declaredType: String,
        val notNull: Boolean,
        val primaryKeyOrder: Int
    )

    private fun Cursor.snapshotValue(index: Int): String = when (getType(index)) {
        Cursor.FIELD_TYPE_NULL -> "null"
        Cursor.FIELD_TYPE_INTEGER -> "integer:${getLong(index)}"
        Cursor.FIELD_TYPE_FLOAT -> "float:${java.lang.Double.toHexString(getDouble(index))}"
        Cursor.FIELD_TYPE_STRING -> getString(index).toByteArray(Charsets.UTF_8).let {
            "string:${it.size}:${it.toHexString()}"
        }
        Cursor.FIELD_TYPE_BLOB -> getBlob(index).let { "blob:${it.size}:${it.toHexString()}" }
        else -> error("Unsupported SQLite value type ${getType(index)}")
    }

    private fun ByteArray.toHexString(): String = joinToString("") {
        (it.toInt() and 0xff).toString(16).padStart(2, '0')
    }

    private fun quoteSqlIdentifier(identifier: String): String =
        "\"${identifier.replace("\"", "\"\"")}\""

    private companion object {
        const val SESSION_ID = "finalizer-session"
        const val RECORDING_ID = "finalizer-recording"
        const val PHASE_ID = "finalizer-phase"
        const val ACQUISITION_ID = "finalizer-acquisition"
        const val SNAPSHOT_CREATED_AT = "2026-08-31T00:00:00Z"
        val EXPECTED_USER_TABLES = listOf(
            "exercises",
            "heart_rate_acquisition_intervals",
            "heart_rate_analysis_snapshots",
            "heart_rate_recordings",
            "heart_rate_samples",
            "recovery_areas",
            "recovery_recommendations",
            "session_step_records",
            "strength_set_records",
            "timed_rest_extension_records",
            "workout_phase_intervals",
            "workout_plans",
            "workout_sessions"
        )
        const val VALID_DISPLAY_METADATA =
            "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
        const val VALID_PLAN_SNAPSHOT =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Timed\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"block\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[]}],\"preferences\":null,\"followAlong\":null}"
        val VALID_PHASE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"timed_work\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855\"},\"payload\":{\"variant\":\"warmup\",\"compositionVersion\":2,\"compositionBlockId\":\"block\",\"${"timelineStage" + "Id"}\":\"block:warmup\",\"timelineStageKind\":\"warmup\",\"stageGroupId\":\"block:warmup\",\"targetId\":\"block:warmup:target\",\"targetKind\":\"warmup\",\"roundIndex0\":null,\"stageGroupIndex0\":null,\"targetIndex0\":0,\"stageInstanceIndex0\":0,\"${"targetInstance" + "Index0"}\":0,\"stepIndex0\":0}}"
    }
}
