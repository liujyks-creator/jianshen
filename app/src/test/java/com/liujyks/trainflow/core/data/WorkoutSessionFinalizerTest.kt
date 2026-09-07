package com.liujyks.trainflow.core.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.RoomDatabase
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
import com.liujyks.trainflow.core.database.entity.SessionStepRecordEntity
import com.liujyks.trainflow.core.database.entity.StrengthSetRecordEntity
import com.liujyks.trainflow.core.database.entity.TimedRestExtensionRecordEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPlanEntity
import com.liujyks.trainflow.core.datastore.TrainFlowPreferencesDataSource
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.io.File
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutSessionFinalizerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: TrainFlowDatabase
    @Volatile
    private var guardedUpdateMutation: ((String) -> Unit)? = null

    @Before
    fun createDatabase() {
        openDatabase()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun frozenExecutionAndObservedEndCommitWithOriginalRecordingGraph() = runBlocking {
        seedActiveRecording()
        freezeStartMetadata()
        val before = requireGraph()
        val frozen = terminalRequest()
        val result = WorkoutSessionRepository(database).finalizeCanonicalSession(frozen)
        val graph = requireGraph()
        assertEquals("2026-09-07T16:00:02Z", graph.session.endedAt)
        assertEquals(12, graph.session.totalElapsedSec)
        assertEquals(9, graph.session.effectiveElapsedSec)
        assertEquals(3, graph.session.pausedElapsedSec)
        assertEquals(before.session.copy(status = "completed", endedAt = frozen.endedAt,
            totalElapsedSec = 12, effectiveElapsedSec = 9, pausedElapsedSec = 3,
            lastDurableOffsetMs = 2_000, lastMutationSequence = 4,
            trustedEndOffsetMs = 2_000, terminalReason = "completed"), graph.session)
        assertEquals(before.phases.map { it.copy(endOffsetMs = 2_000, endMutationSequence = 4,
            openMarker = null) }, graph.phases)
        assertEquals(before.acquisitions.map { it.copy(endOffsetMs = 2_000, endMutationSequence = 4,
            openMarker = null) }, graph.acquisitions)
        assertEquals(before.recording!!.copy(status = "terminal", endedOffsetMs = 2_000,
            endedMutationSequence = 4, originalAnalysisVersion = 1), graph.recording)
        assertEquals(before.samples, graph.samples)
        assertEquals(frozen.stepRecords, database.workoutSessionDao().stepRecordsForSession(SESSION_ID))
        assertEquals(frozen.restExtensions, database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID))
        assertEquals(frozen.strengthSets, database.workoutSessionDao().strengthSetRecordsForSession(SESSION_ID))
        assertEquals(CanonicalTuple(2_000, 4), result.finalTuple)
        assertEquals(1, result.analysisVersion)
        assertEquals(SNAPSHOT_CREATED_AT, graph.snapshots.single().createdAt)
        assertEquals(CanonicalValidationResult.Valid,
            AnalysisSnapshotV1Validator.validate(graph, graph.snapshots.single()))
    }

    @Test
    fun noRecordingTerminalPairsIncludeZeroDurationWithoutCreatingHeartRateRows() = runBlocking {
        listOf("completed" to "completed", "abandoned" to "user_abandoned",
            "abandoned" to "owner_cleared").forEachIndexed { index, (status, reason) ->
            if (index > 0) resetDatabase()
            seedActiveRecording()
            freezeStartMetadata()
            val sql = database.openHelper.writableDatabase
            sql.execSQL("DELETE FROM heart_rate_samples")
            sql.execSQL("DELETE FROM heart_rate_acquisition_intervals")
            sql.execSQL("DELETE FROM heart_rate_recordings")
            if (index == 0) sql.execSQL("UPDATE workout_sessions SET last_durable_offset_ms=0,last_mutation_sequence=0")
            val predecessor = if (index == 0) CanonicalTuple(0, 0) else CanonicalTuple(1_000, 3)
            val finalOffset = if (index == 0) 0L else 10_000L
            val finalSequence = if (index == 0) 1L else 4L
            val before = requireGraph()
            val frozen = terminalRequest(expected = RecorderExpectedState(SESSION_ID, "active",
                predecessor, PHASE_ID), finalOffsetMs = finalOffset, status = status, reason = reason,
                steps = emptyList(), totalElapsedSec = 0, effectiveElapsedSec = 0, pausedElapsedSec = 0,
                snapshotCreatedAt = null)
            val repository = WorkoutSessionRepository(database)
            val result = repository.finalizeCanonicalSession(frozen)
            val graph = requireGraph()
            assertEquals(before.copy(session = before.session.copy(status = status, terminalReason = reason,
                lastDurableOffsetMs = finalOffset, lastMutationSequence = finalSequence, trustedEndOffsetMs = finalOffset,
                endedAt = frozen.endedAt, totalElapsedSec = 0, effectiveElapsedSec = 0, pausedElapsedSec = 0),
                phases = before.phases.map { it.copy(endOffsetMs = finalOffset, endMutationSequence = finalSequence,
                    openMarker = null) }), graph)
            assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(graph))
            assertEquals(CanonicalTuple(finalOffset, finalSequence), result.finalTuple)
            assertEquals(null, result.analysisVersion)
            assertEquals(null, result.recordingId)
            assertEquals(emptyList<SessionStepRecordEntity>(), database.workoutSessionDao().stepRecordsForSession(SESSION_ID))
            assertEquals(emptyList<TimedRestExtensionRecordEntity>(), database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID))
            assertEquals(emptyList<StrengthSetRecordEntity>(), database.workoutSessionDao().strengthSetRecordsForSession(SESSION_ID))
            val committed = databaseSnapshot()
            assertEquals(result, WorkoutSessionRepository(database).finalizeCanonicalSession(frozen))
            if (index > 0) {
                val offsetOnly = terminalRequest(expected = frozen.expected.copy(durableTuple = CanonicalTuple(6_000, 3)),
                    finalOffsetMs = finalOffset, status = status, reason = reason, steps = emptyList(),
                    totalElapsedSec = 0, effectiveElapsedSec = 0, pausedElapsedSec = 0, snapshotCreatedAt = null)
                assertEquals(result, WorkoutSessionRepository(database).finalizeCanonicalSession(offsetOnly))
            }
            assertEquals(committed, databaseSnapshot())
        }
    }

    @Test
    fun terminalRetryUsesSequenceAndFullPayloadAllowsOnlyLegalOldOffsetDifference() = runBlocking {
        seedActiveRecording()
        freezeStartMetadata()
        val frozen = terminalRequest(finalOffsetMs = 10_000)
        val repository = WorkoutSessionRepository(database)
        val result = repository.finalizeCanonicalSession(frozen)
        val committed = databaseSnapshot()
        val offsetOnly = terminalRequest(expected = frozen.expected.copy(durableTuple = CanonicalTuple(6_000, 3)),
            finalOffsetMs = 10_000, snapshotCreatedAt = "2099-01-01T00:00:00Z")
        assertEquals(result, WorkoutSessionRepository(database).finalizeCanonicalSession(offsetOnly))
        assertEquals(committed, databaseSnapshot())
        val conflicts = listOf(
            terminalRequest(finalOffsetMs = 10_000, endedAt = "2026-09-07T16:00:03Z"),
            terminalRequest(finalOffsetMs = 10_000, totalElapsedSec = 13),
            terminalRequest(finalOffsetMs = 10_000, steps = frozen.stepRecords.map { it.copy(actualDurationSec = 3) }),
            terminalRequest(finalOffsetMs = 10_000, steps = emptyList()),
            terminalRequest(finalOffsetMs = 10_000, expected = frozen.expected.copy(durableTuple = CanonicalTuple(1_000, 2))),
            terminalRequest(finalOffsetMs = 10_001),
            terminalRequest(finalOffsetMs = 10_000, status = "abandoned", reason = "user_abandoned"),
            terminalRequest(finalOffsetMs = 10_000, expected = frozen.expected.copy(recordingId = "another-recording")),
            terminalRequest(finalOffsetMs = 10_000, expected = frozen.expected.copy(openPhaseId = "another-phase")))
        conflicts.forEach { conflict ->
            val failure = runCatching { repository.finalizeCanonicalSession(conflict) }.exceptionOrNull()
            assertTrue("unexpected $failure", failure is CanonicalFinalizationConflictException)
            assertEquals(committed, databaseSnapshot())
        }
        val invalid = terminalRequest(finalOffsetMs = 10_000,
            expected = frozen.expected.copy(durableTuple = CanonicalTuple(10_001, 3)))
        assertTrue(runCatching { repository.finalizeCanonicalSession(invalid) }.exceptionOrNull() is RecorderValidationException)
        assertEquals(committed, databaseSnapshot())
    }

    @Test
    fun outerExecutionAndInnerFinalizerFailuresRollbackEveryRowWithOriginalSignals() = runBlocking {
        val cases = listOf(
            """CREATE TRIGGER fail_execution BEFORE UPDATE OF ended_at ON workout_sessions
                BEGIN SELECT RAISE(ABORT,'execution_header_failure'); END""" to "execution_header_failure",
            """CREATE TRIGGER fail_step AFTER INSERT ON session_step_records
                BEGIN SELECT RAISE(ABORT,'execution_step_failure'); END""" to "execution_step_failure",
            """CREATE TRIGGER fail_phase BEFORE UPDATE OF end_offset_ms ON workout_phase_intervals
                BEGIN DELETE FROM workout_phase_intervals WHERE id=OLD.id; END""" to "finalize_close_open_phase",
            """CREATE TRIGGER fail_snapshot BEFORE INSERT ON heart_rate_analysis_snapshots
                BEGIN SELECT RAISE(ABORT,'original_snapshot_failure'); END""" to "original_snapshot_failure",
            """CREATE TRIGGER fail_binding AFTER INSERT ON heart_rate_analysis_snapshots
                BEGIN UPDATE heart_rate_recordings SET original_analysis_version=2; END""" to "bind_original_analysis",
            """CREATE TRIGGER fail_graph AFTER UPDATE OF original_analysis_version ON heart_rate_recordings
                WHEN NEW.original_analysis_version=1
                BEGIN UPDATE heart_rate_analysis_snapshots SET quality_reasons_json='{}'; END""" to "validation"
        )
        cases.forEachIndexed { index, (sql, signal) ->
            if (index > 0) resetDatabase()
            seedActiveRecording()
            freezeStartMetadata()
            database.openHelper.writableDatabase.execSQL(sql)
            val before = databaseSnapshot()
            val failure = runCatching {
                WorkoutSessionRepository(database).finalizeCanonicalSession(terminalRequest())
            }.exceptionOrNull()
            when (signal) {
                "finalize_close_open_phase", "bind_original_analysis" -> assertGuard(failure, signal, 0)
                "validation" -> assertTrue(failure is RecorderValidationException)
                else -> {
                    assertTrue("$signal: $failure", failure is SQLiteConstraintException)
                    assertTrue(failure!!.message.orEmpty().contains(signal))
                }
            }
            assertEquals("$signal outer rollback", before, databaseSnapshot())
        }
    }

    @Test
    fun outerLastExecutionReadbackDetectsMutationAndRollsBackCommittedInnerWork() = runBlocking {
        seedActiveRecording()
        freezeStartMetadata()
        val before = databaseSnapshot()
        val bindingReached = AtomicBoolean(false)
        val fired = AtomicBoolean(false)
        guardedUpdateMutation = { sql ->
            if (sql.contains("SET original_analysis_version = 1")) bindingReached.set(true)
            if (bindingReached.get() && sql.startsWith("SELECT * FROM session_step_records") &&
                fired.compareAndSet(false, true)) {
                guardedUpdateMutation = null
                database.openHelper.writableDatabase.execSQL(
                    "UPDATE session_step_records SET actual_duration_sec=99 WHERE id='execution-step'")
            }
        }
        val failure = runCatching {
            WorkoutSessionRepository(database).finalizeCanonicalSession(terminalRequest())
        }.exceptionOrNull()
        assertTrue(fired.get())
        assertTrue(failure is RecorderValidationException)
        assertEquals("terminal_graph_changed_during_write", (failure as RecorderValidationException).code)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun cancelledTerminalDeliveryRetriesTheDurableGraphWithoutAnotherSnapshot() = runBlocking {
        seedActiveRecording()
        freezeStartMetadata()
        val repository = WorkoutSessionRepository(database)
        val frozen = terminalRequest()
        val dispatches = LinkedBlockingQueue<Runnable>()
        val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) { dispatches.add(block) }
        }
        var delivered = false
        val operation = async(dispatcher) { repository.finalizeCanonicalSession(frozen); delivered = true }
        requireNotNull(dispatches.poll(5, TimeUnit.SECONDS)).run()
        val delivery = requireNotNull(dispatches.poll(5, TimeUnit.SECONDS))
        val durable = requireGraph()
        assertEquals("completed", durable.session.status)
        assertEquals(frozen.endedAt, durable.session.endedAt)
        assertEquals(frozen.stepRecords, database.workoutSessionDao().stepRecordsForSession(SESSION_ID))
        assertEquals(1, durable.recording?.originalAnalysisVersion)
        assertEquals(1, durable.snapshots.size)
        assertFalse(delivered)
        val original = CancellationException("terminal_delivery_lost")
        operation.cancel(original)
        delivery.run()
        val failure = runCatching { withTimeout(5_000) { operation.await() } }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        assertEquals(original.message, failure?.message)
        assertFalse(delivered)
        val committed = databaseSnapshot()
        database.openHelper.writableDatabase.execSQL("""CREATE TRIGGER no_repeat_analysis
            BEFORE INSERT ON heart_rate_analysis_snapshots BEGIN SELECT RAISE(ABORT,'analysis_repeated'); END""")
        val result = repository.finalizeCanonicalSession(frozen)
        assertEquals(CanonicalTuple(2_000, 4), result.finalTuple)
        assertEquals(committed, databaseSnapshot())
    }

    @Test
    fun fullActivePredecessorIsGuardedAndConcurrentTerminalIntentCannotOverwriteWinner() = runBlocking {
        val writer = WorkoutSessionRepository(database)
        writer.prepareRecorder()
        seedActiveRecording()
        freezeStartMetadata()
        val frozen = terminalRequest()
        val before = databaseSnapshot()
        listOf(frozen.expected.copy(durableTuple = CanonicalTuple(1_001, 3)),
            frozen.expected.copy(status = "paused"), frozen.expected.copy(openPhaseId = "wrong"),
            frozen.expected.copy(openAcquisitionId = "wrong")).forEach { stale ->
            val failure = runCatching { writer.finalizeCanonicalSession(terminalRequest(expected = stale)) }.exceptionOrNull()
            assertGuard(failure, "expected_state", 0)
            assertEquals(before, databaseSnapshot())
        }
        val abandoned = terminalRequest(status = "abandoned", reason = "user_abandoned", totalElapsedSec = 14)
        val requests = listOf(frozen, abandoned)
        val results = coroutineScope {
            requests.map { request -> async(Dispatchers.IO) {
                runCatching { WorkoutSessionRepository(database).finalizeCanonicalSession(request) }
            } }.map { it.await() }
        }
        assertEquals(1, results.count { it.isSuccess })
        assertTrue(results.single { it.isFailure }.exceptionOrNull() is CanonicalFinalizationConflictException)
        val winnerIndex = results.indexOfFirst { it.isSuccess }
        val winner = requests[winnerIndex]
        val graph = requireGraph()
        assertEquals(winner.terminalStatus, graph.session.status)
        assertEquals(winner.terminalReason, graph.session.terminalReason)
        assertEquals(winner.totalElapsedSec, graph.session.totalElapsedSec)
        assertEquals(winner.stepRecords, database.workoutSessionDao().stepRecordsForSession(SESSION_ID))
        assertEquals(1, graph.recording?.originalAnalysisVersion)
        assertEquals(1, graph.snapshots.size)
        val committed = databaseSnapshot()
        val repeats = coroutineScope {
            (1..2).map { async(Dispatchers.IO) {
                WorkoutSessionRepository(database).finalizeCanonicalSession(winner)
            } }.map { it.await() }
        }
        assertEquals(listOf(results[winnerIndex].getOrThrow(), results[winnerIndex].getOrThrow()), repeats)
        val sampleFailure = runCatching { writer.appendHeartRateSample(frozen.expected, CanonicalTuple(2_001, 5),
            HeartRateSampleEntity(RECORDING_ID, 1, 2_001, 5, 125)) }.exceptionOrNull()
        assertGuard(sampleFailure, "expected_state", 0)
        val phaseFailure = runCatching { writer.transitionPhase(frozen.expected, CanonicalTuple(2_001, 5),
            graph.phases.single().copy(id = "late-phase", sequence = 1, startOffsetMs = 2_001,
                startMutationSequence = 5, endOffsetMs = null, endMutationSequence = null, openMarker = 1)) }.exceptionOrNull()
        assertGuard(phaseFailure, "expected_state", 0)
        assertEquals(committed, databaseSnapshot())
    }

    @Test
    fun executionHeaderGuardRejectsTwoRowsAndRestFactsRemainAppendOnly() = runBlocking {
        seedActiveRecording()
        rebuildTableWithoutConstraints("workout_sessions")
        val before = databaseSnapshot("workout_sessions")
        val fired = armDuplicateBeforeGuardedUpdate(GuardRowCountTwoCase("workout_sessions",
            "set ended_at =", "id='$SESSION_ID'", "write_canonical_execution_header"))
        val failure = runCatching {
            WorkoutSessionRepository(database).finalizeCanonicalSession(terminalRequest())
        }.exceptionOrNull()
        assertTrue(fired.get())
        assertGuard(failure, "write_canonical_execution_header", 2)
        assertEquals(before, databaseSnapshot("workout_sessions"))

        resetDatabase()
        seedActiveRecording()
        val extension = TimedRestExtensionRecordEntity("extension-1", SESSION_ID, "rest-step", 1, 1,
            "rest", "Rest at first reference", "work", "Work at first reference", 15, 30, 5, 25, 15, 5)
        database.canonicalTimelineHeartRateDao().insertRestExtension(extension)
        val repository = WorkoutSessionRepository(database)
        val prior = databaseSnapshot()
        listOf(emptyList(), listOf(extension.copy(restStageTitle = "Changed"))).forEach { changed ->
            assertTrue(runCatching { repository.finalizeCanonicalSession(terminalRequest(extensions = changed)) }
                .exceptionOrNull() is CanonicalFinalizationConflictException)
            assertEquals(prior, databaseSnapshot())
        }
        val added = extension.copy(id = "extension-2", restElapsedBeforeExtensionSec = 6,
            extensionAtRemainingSec = 39, cumulativeExtraRestSec = 30, eventElapsedSec = 6)
        val metadata = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"squat","displayNameAtFirstReference":"深蹲","customNameAtFirstReference":null,"resolutionSource":"runtime_substitution"}]}"""
        val frozen = terminalRequest(extensions = listOf(added, extension), metadata = metadata)
        repository.finalizeCanonicalSession(frozen)
        assertEquals(listOf(extension, added), database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID))
        assertEquals(metadata, requireGraph().session.sessionDisplayMetadataJson)
        val committed = databaseSnapshot()
        assertEquals(CanonicalTuple(2_000, 4), repository.finalizeCanonicalSession(frozen).finalTuple)
        assertEquals(committed, databaseSnapshot())
        assertTrue(runCatching { repository.finalizeCanonicalSession(terminalRequest(
            extensions = listOf(extension.copy(eventElapsedSec = 6), added), metadata = metadata)) }
            .exceptionOrNull() is CanonicalFinalizationConflictException)
        assertTrue(runCatching { repository.finalizeCanonicalSession(terminalRequest(
            extensions = listOf(extension, added), metadata = metadata + " ")) }
            .exceptionOrNull() is CanonicalFinalizationConflictException)
        assertEquals(committed, databaseSnapshot())
    }

    @Test
    fun firstSnapshotMetadataMustMatchFrozenCreationTimeOrOuterTransactionRollsBack() = runBlocking {
        seedActiveRecording()
        val before = databaseSnapshot()
        database.openHelper.writableDatabase.execSQL("""CREATE TRIGGER change_snapshot_time
            AFTER UPDATE OF original_analysis_version ON heart_rate_recordings
            WHEN NEW.original_analysis_version=1
            BEGIN UPDATE heart_rate_analysis_snapshots SET created_at='2099-01-01T00:00:00Z'; END""")
        val failure = runCatching {
            WorkoutSessionRepository(database).finalizeCanonicalSession(terminalRequest())
        }.exceptionOrNull()
        assertTrue("first creation metadata was not preserved: $failure", failure is RecorderValidationException)
        assertEquals("terminal_graph_changed_during_write", (failure as RecorderValidationException).code)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun noRecordingLateGuardFailureRollsBackExecutionAndPhaseTogether() = runBlocking {
        seedActiveRecording()
        freezeStartMetadata()
        val sql = database.openHelper.writableDatabase
        sql.execSQL("DELETE FROM heart_rate_samples")
        sql.execSQL("DELETE FROM heart_rate_acquisition_intervals")
        sql.execSQL("DELETE FROM heart_rate_recordings")
        sql.execSQL("""CREATE TRIGGER change_status_after_close AFTER UPDATE OF end_offset_ms ON workout_phase_intervals
            BEGIN UPDATE workout_sessions SET status='paused' WHERE id='$SESSION_ID'; END""")
        val before = databaseSnapshot()
        val frozen = terminalRequest(expected = RecorderExpectedState(SESSION_ID, "active",
            CanonicalTuple(1_000, 3), PHASE_ID), snapshotCreatedAt = null)
        val failure = runCatching { WorkoutSessionRepository(database).finalizeCanonicalSession(frozen) }.exceptionOrNull()
        assertGuard(failure, "finalize_session_without_recording", 0)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun frozenStrengthExecutionRetainsEveryFieldAndRejectsChangedOrExtraRows() = runBlocking {
        seedActiveRecording()
        freezeStartMetadata()
        val snapshot = WorkoutPlanSnapshot(title = "Strength", mode = WorkoutMode.STRENGTH,
            blocks = listOf(StrengthExerciseBlock(id = "strength", order = 0, exerciseId = "squat",
                sets = listOf(StrengthSetPlan(id = "set", order = 0, kind = StrengthSetKind.WORKING))))).toStorageJson()
        val storage = (PlanSnapshotStorageV1Validator.validate(snapshot, WorkoutMode.STRENGTH) as
            PlanSnapshotStorageV1ValidationResult.Valid).storage
        val digest = requireNotNull(OrderedStructureSignatureInputV1.digestHexLowercase(storage))
        val identity = """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_active_set","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"$digest"},"payload":{"variant":"active_set","blockId":"strength","setPlanId":"set","plannedExerciseId":"squat","actualExerciseId":"squat","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":null}}"""
        database.openHelper.writableDatabase.execSQL("UPDATE workout_sessions SET mode='strength',plan_snapshot_json=?",
            arrayOf(snapshot))
        database.openHelper.writableDatabase.execSQL("UPDATE workout_phase_intervals SET phase_kind='strength_active_set',phase_identity_json=?",
            arrayOf(identity))
        assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(requireGraph()))
        val set = StrengthSetRecordEntity("execution-set", SESSION_ID, "squat", "set", 0, "working", "both",
            "weight=60.0,kg|rep=range,8,12", "weight=62.5,kg|reps=9", 7, 30, "hard", null, "原始备注")
        val step = SessionStepRecordEntity("strength-step", SESSION_ID, "strength:set", "strength_active_set",
            "strength", "item", "set", "squat", "2026-09-07T16:00:00Z", "2026-09-07T16:00:07Z", false, 7, null)
        val steps = mutableListOf(step)
        val sets = mutableListOf(set)
        val frozen = terminalRequest(steps = steps, sets = sets)
        steps.clear()
        sets.clear()
        val repository = WorkoutSessionRepository(database)
        repository.finalizeCanonicalSession(frozen)
        assertEquals(listOf(step), database.workoutSessionDao().stepRecordsForSession(SESSION_ID))
        assertEquals(listOf(set), database.workoutSessionDao().strengthSetRecordsForSession(SESSION_ID))
        val committed = databaseSnapshot()
        val variants = listOf(set.copy(actualJson = "weight=65.0,kg|reps=9"),
            set.copy(plannedJson = null), set.copy(notes = "different"), set.copy(activeDurationSec = 8),
            set.copy(actualRestAfterSec = 31), set.copy(side = "left"), set.copy(effort = "easy"),
            set.copy(substitutedFromExerciseId = "another"))
        variants.forEach { changed ->
            assertTrue(runCatching { repository.finalizeCanonicalSession(terminalRequest(steps = listOf(step), sets = listOf(changed))) }
                .exceptionOrNull() is CanonicalFinalizationConflictException)
            assertEquals(committed, databaseSnapshot())
        }
        assertTrue(runCatching { repository.finalizeCanonicalSession(terminalRequest(steps = listOf(step),
            sets = listOf(set, set.copy(id = "extra-set", setOrder = 1)))) }.exceptionOrNull() is CanonicalFinalizationConflictException)
        assertEquals(committed, databaseSnapshot())
    }

    @Test
    fun changedCurrentPlanAndParametersAndReopenedDatabaseNeverReplaceOriginalOrUnknownEnd() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(temporaryFolder.root, "terminal.db")
        database.close()
        database = Room.databaseBuilder(context, TrainFlowDatabase::class.java, file.absolutePath)
            .allowMainThreadQueries().build()
        val preferencesJob = SupervisorJob()
        val preferences = TrainFlowPreferencesDataSource(PreferenceDataStoreFactory.create(
            scope = CoroutineScope(preferencesJob + Dispatchers.IO),
            produceFile = { File(temporaryFolder.root, "current.preferences_pb") }))
        try {
            seedActiveRecording()
            freezeStartMetadata()
            database.openHelper.writableDatabase.execSQL("UPDATE workout_sessions SET plan_id='current-plan'")
            val original = requireGraph()
            val plan = WorkoutPlanEntity("current-plan", "timed", "Changed current title", blocksJson = "[]",
                createdAt = "2026-09-08T00:00:00Z", updatedAt = "2026-09-08T00:00:00Z")
            database.workoutPlanDao().upsertPlan(plan)
            preferences.setHeartRatePersonalParameters(40, 180, 170)
            assertEquals(180, preferences.preferences.first().heartRatePersonalMaxBpm)
            val frozen = terminalRequest(endedAt = null, status = "abandoned", reason = "owner_cleared")
            val result = WorkoutSessionRepository(database).finalizeCanonicalSession(frozen)
            val terminal = requireGraph()
            assertEquals(original.session.planSnapshotJson, terminal.session.planSnapshotJson)
            assertEquals(original.recording!!.copy(status = "terminal", endedOffsetMs = 2_000,
                endedMutationSequence = 4, originalAnalysisVersion = 1), terminal.recording)
            assertEquals(null, terminal.session.endedAt)
            assertEquals("2026-09-07T16:00:00Z", terminal.session.startedAt)
            assertEquals("2026-09-08", terminal.session.startLocalDate)
            assertEquals("Asia/Shanghai", terminal.session.startZoneId)
            assertEquals(28800L, terminal.session.startUtcOffsetSeconds)
            assertEquals(1L, terminal.session.timeMetadataSourceContractVersion)
            database.workoutPlanDao().upsertPlan(plan.copy(title = "Changed again", updatedAt = "2099-01-01T00:00:00Z"))
            preferences.setHeartRatePersonalParameters(50, 200, 190)
            assertEquals(200, preferences.preferences.first().heartRatePersonalMaxBpm)
            database.openHelper.writableDatabase.execSQL("""CREATE TRIGGER forbid_new_snapshot
                BEFORE INSERT ON heart_rate_analysis_snapshots BEGIN SELECT RAISE(ABORT,'original_must_not_repeat'); END""")
            val committed = databaseSnapshot()
            database.close()
            database = Room.databaseBuilder(context, TrainFlowDatabase::class.java, file.absolutePath)
                .allowMainThreadQueries().build()
            assertEquals(result, WorkoutSessionRepository(database).finalizeCanonicalSession(
                terminalRequest(endedAt = null, status = "abandoned", reason = "owner_cleared",
                    snapshotCreatedAt = "2099-01-01T00:00:00Z")))
            assertEquals(terminal, requireGraph())
            assertEquals(committed, databaseSnapshot())
        } finally {
            preferencesJob.cancelAndJoin()
        }
    }

    @Test
    fun invalidFrozenPayloadAndForeignExecutionPrimaryKeyNeverChangeAnySession() = runBlocking {
        seedActiveRecording()
        val frozen = terminalRequest()
        val before = databaseSnapshot()
        val invalid = listOf(terminalRequest(totalElapsedSec = -1),
            terminalRequest(steps = frozen.stepRecords + frozen.stepRecords),
            terminalRequest(steps = frozen.stepRecords.map { it.copy(sessionId = "other") }),
            terminalRequest(steps = frozen.stepRecords.map { it.copy(kind = "invented") }),
            terminalRequest(metadata = "{}"),
            terminalRequest(status = "abandoned", reason = "process_interrupted"))
        invalid.forEach { request ->
            assertTrue(runCatching { WorkoutSessionRepository(database).finalizeCanonicalSession(request) }
                .exceptionOrNull() is RecorderValidationException)
            assertEquals(before, databaseSnapshot())
        }
        database.workoutSessionDao().insertSession(WorkoutSessionEntity("other", mode = "timed", status = "completed",
            planSnapshotJson = VALID_PLAN_SNAPSHOT))
        database.workoutSessionDao().upsertStepRecords(frozen.stepRecords.map { it.copy(sessionId = "other") })
        val foreign = databaseSnapshot()
        assertTrue(runCatching { WorkoutSessionRepository(database).finalizeCanonicalSession(frozen) }
            .exceptionOrNull() is SQLiteConstraintException)
        assertEquals(foreign, databaseSnapshot())
    }

    private fun freezeStartMetadata() {
        database.openHelper.writableDatabase.execSQL("""
            UPDATE workout_sessions SET started_at='2026-09-07T16:00:00Z',
            start_local_date='2026-09-08',start_zone_id='Asia/Shanghai',
            start_utc_offset_seconds=28800,time_metadata_source_contract_version=1
            WHERE id='$SESSION_ID'
        """.trimIndent())
    }

    private fun terminalRequest(
        expected: RecorderExpectedState = RecorderExpectedState(SESSION_ID, "active",
            CanonicalTuple(1_000, 3), PHASE_ID, RECORDING_ID, ACQUISITION_ID),
        finalOffsetMs: Long = 2_000,
        status: String = "completed",
        reason: String = "completed",
        endedAt: String? = "2026-09-07T16:00:02Z",
        totalElapsedSec: Int? = 12,
        effectiveElapsedSec: Int? = 9,
        pausedElapsedSec: Int? = 3,
        steps: List<SessionStepRecordEntity> = listOf(SessionStepRecordEntity(
            id = "execution-step", sessionId = SESSION_ID, stepId = "block:warmup:target",
            kind = "timed_work", blockId = "block", itemId = "item", setPlanId = null,
            exerciseId = null, startedAt = "2026-09-07T16:00:00Z",
            endedAt = "2026-09-07T16:00:02Z", skipped = true,
            actualDurationSec = 2, plannedDurationSec = 10)),
        extensions: List<TimedRestExtensionRecordEntity> = emptyList(),
        sets: List<StrengthSetRecordEntity> = emptyList(),
        metadata: String = VALID_DISPLAY_METADATA,
        snapshotCreatedAt: String? = SNAPSHOT_CREATED_AT
    ) = FrozenCanonicalFinalizationRequest(expected, finalOffsetMs, status, reason, endedAt,
        totalElapsedSec, effectiveElapsedSec, pausedElapsedSec, metadata, steps, extensions, sets, snapshotCreatedAt)

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
    fun allFiveFinalizerGuardsRejectRowCountTwoAndRollbackAllThirteenTables() = runBlocking {
        val cases = listOf(
            GuardRowCountTwoCase(
                table = "workout_phase_intervals",
                updateMarker = "set end_offset_ms",
                rowPredicate = "id='$PHASE_ID'",
                guard = "finalize_close_open_phase"
            ),
            GuardRowCountTwoCase(
                table = "heart_rate_acquisition_intervals",
                updateMarker = "set end_offset_ms",
                rowPredicate = "id='$ACQUISITION_ID'",
                guard = "finalize_close_open_acquisition"
            ),
            GuardRowCountTwoCase(
                table = "heart_rate_recordings",
                updateMarker = "set status = 'terminal'",
                rowPredicate = "recording_id='$RECORDING_ID'",
                guard = "finalize_terminalize_recording"
            ),
            GuardRowCountTwoCase(
                table = "workout_sessions",
                updateMarker = "trusted_end_offset_ms",
                rowPredicate = "id='$SESSION_ID'",
                guard = "finalize_terminalize_session"
            ),
            GuardRowCountTwoCase(
                table = "heart_rate_recordings",
                updateMarker = "original_analysis_version = 1",
                rowPredicate = "recording_id='$RECORDING_ID'",
                guard = "bind_original_analysis"
            )
        )

        cases.forEachIndexed { index, case ->
            if (index > 0) resetDatabase()
            seedActiveRecording()
            rebuildTableWithoutConstraints(case.table)
            val mutationFired = armDuplicateBeforeGuardedUpdate(case)
            val before = databaseSnapshot(allowMissingPrimaryKeyFor = case.table)

            val failure = runCatching {
                WorkoutSessionRepository(database).finalizeRecordingSession(request())
            }.exceptionOrNull()

            assertTrue("${case.guard} did not reach its real Room UPDATE", mutationFired.get())
            assertGuard(failure, case.guard, 2)
            assertEquals(
                "guard ${case.guard} mutated one of 13 tables",
                before,
                databaseSnapshot(allowMissingPrimaryKeyFor = case.table)
            )
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
            .setQueryCallback(
                RoomDatabase.QueryCallback { sqlQuery, _ ->
                    guardedUpdateMutation?.invoke(sqlQuery)
                },
                Executor { command -> command.run() }
            )
            .build()
    }

    private fun resetDatabase() {
        guardedUpdateMutation = null
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

    private fun rebuildTableWithoutConstraints(table: String) {
        val sql = database.openHelper.writableDatabase
        val quotedTable = quoteSqlIdentifier(table)
        val columns = sql.query("PRAGMA table_info($quotedTable)").use { cursor ->
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
        }
        check(columns.isNotEmpty()) { "Cannot rebuild empty table $table" }
        val copyTable = quoteSqlIdentifier("${table}_guard_copy")
        val columnList = columns.joinToString(", ") { column -> quoteSqlIdentifier(column.name) }
        val definitions = columns.joinToString(", ") { column ->
            buildString {
                append(quoteSqlIdentifier(column.name))
                if (column.declaredType.isNotEmpty()) append(" ${column.declaredType}")
                if (column.notNull) append(" NOT NULL")
            }
        }
        sql.execSQL("PRAGMA foreign_keys=OFF")
        sql.execSQL("CREATE TABLE $copyTable AS SELECT $columnList FROM $quotedTable")
        sql.execSQL("DROP TABLE $quotedTable")
        sql.execSQL("CREATE TABLE $quotedTable ($definitions)")
        sql.execSQL("INSERT INTO $quotedTable ($columnList) SELECT $columnList FROM $copyTable")
        sql.execSQL("DROP TABLE $copyTable")
    }

    private fun armDuplicateBeforeGuardedUpdate(case: GuardRowCountTwoCase): AtomicBoolean {
        val fired = AtomicBoolean(false)
        guardedUpdateMutation = { rawSql ->
            val normalized = rawSql.lowercase().replace(Regex("\\s+"), " ").trim()
            if (
                normalized.startsWith("update") &&
                normalized.contains(case.table) &&
                normalized.contains(case.updateMarker) &&
                fired.compareAndSet(false, true)
            ) {
                guardedUpdateMutation = null
                val quotedTable = quoteSqlIdentifier(case.table)
                database.openHelper.writableDatabase.execSQL(
                    "INSERT INTO $quotedTable SELECT * FROM $quotedTable " +
                        "WHERE ${case.rowPredicate} LIMIT 1"
                )
            }
        }
        return fired
    }

    private fun databaseSnapshot(allowMissingPrimaryKeyFor: String? = null): List<String> {
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
            check(columns.isNotEmpty())
            check(primaryKey.isNotEmpty() || table == allowMissingPrimaryKeyFor) {
                "Room user table has no primary key: $table"
            }
            val select = columns.joinToString(", ") { quoteSqlIdentifier(it.name) }
            val order = (primaryKey.ifEmpty { columns }).joinToString(", ") {
                quoteSqlIdentifier(it.name)
            }
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

    private data class GuardRowCountTwoCase(
        val table: String,
        val updateMarker: String,
        val rowPredicate: String,
        val guard: String
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
