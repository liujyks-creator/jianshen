package com.liujyks.trainflow.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class WorkoutSessionRecorderGuardedWriteTest {
    private lateinit var database: TrainFlowDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TrainFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun protectedWritesAcceptZeroDurationAndSameOffsetSequenceThroughTheRealGraphValidator() = runBlocking {
        database.workoutSessionDao().insertSession(legacySession("legacy-active", "active"))
        val repository = WorkoutSessionRepository(database)
        val gate = repository.startCanonicalSession(canonicalSession(), initialPhase())

        assertEquals(listOf("legacy-active"), gate.legacyResiduals.map { it.sessionId })
        repository.appendSessionDisplayMetadata(
            expected = expected(tuple = CanonicalTuple(0, 0)),
            nextTuple = CanonicalTuple(0, 1),
            nextJson = DISPLAY_METADATA_WITH_ENTRY
        )
        repository.transitionPhase(
            expected = expected(tuple = CanonicalTuple(0, 1)),
            nextTuple = CanonicalTuple(0, 2),
            nextPhase = pausedPhase()
        )
        repository.startHeartRateRecording(
            expected = expected(tuple = CanonicalTuple(0, 2), openPhaseId = PHASE_1_ID),
            nextTuple = CanonicalTuple(0, 3),
            recording = activeRecording(),
            initialAcquisition = acquisition(
                id = ACQUISITION_0_ID,
                sequence = 0,
                tuple = CanonicalTuple(0, 3),
                state = "searching",
                reason = "initial_acquisition"
            )
        )
        repository.transitionAcquisition(
            expected = expected(
                tuple = CanonicalTuple(0, 3),
                openPhaseId = PHASE_1_ID,
                recordingId = RECORDING_ID,
                openAcquisitionId = ACQUISITION_0_ID
            ),
            nextTuple = CanonicalTuple(0, 4),
            nextAcquisition = acquisition(
                id = ACQUISITION_1_ID,
                sequence = 1,
                tuple = CanonicalTuple(0, 4),
                state = "live",
                reason = null
            )
        )
        repository.appendHeartRateSample(
            expected = expected(
                tuple = CanonicalTuple(0, 4),
                openPhaseId = PHASE_1_ID,
                recordingId = RECORDING_ID,
                openAcquisitionId = ACQUISITION_1_ID
            ),
            nextTuple = CanonicalTuple(0, 5),
            sample = HeartRateSampleEntity(
                recordingId = RECORDING_ID,
                sampleSequence = 0,
                offsetMs = 0,
                mutationSequence = 5,
                bpm = 120
            )
        )

        val graph = requireGraph(SESSION_ID)
        assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(graph))
        assertEquals(CanonicalTuple(0, 5), CanonicalTuple(
            requireNotNull(graph.session.lastDurableOffsetMs),
            requireNotNull(graph.session.lastMutationSequence)
        ))
        assertEquals(2, graph.phases.size)
        assertEquals(0L, graph.phases.first().endOffsetMs)
        assertEquals(2L, graph.phases.first().endMutationSequence)
        assertEquals(DISPLAY_METADATA_WITH_ENTRY, graph.session.sessionDisplayMetadataJson)
        assertEquals(2, graph.acquisitions.size)
        assertEquals(0L, graph.acquisitions.first().endOffsetMs)
        assertEquals(4L, graph.acquisitions.first().endMutationSequence)
        assertEquals(listOf(0L), graph.samples.map { it.sampleSequence })
    }

    @Test
    fun recorderStartTransactionRollsBackEveryRowAndRecognizesTheExactCommittedGraph() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val admission = repository.admitRecorder("entry:atomic-guard")
        val candidate = CanonicalSessionGraphV1(
            session = canonicalSession(),
            phases = listOf(initialPhase()),
            recording = activeRecording().copy(startedMutationSequence = 0),
            acquisitions = listOf(
                acquisition(
                    id = ACQUISITION_0_ID,
                    sequence = 0,
                    tuple = CanonicalTuple(0, 0),
                    state = "not_observing",
                    reason = null
                )
            )
        )
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_atomic_acquisition BEFORE INSERT ON " +
                "heart_rate_acquisition_intervals BEGIN SELECT RAISE(ABORT, 'reject'); END"
        )
        val before = databaseSnapshot()

        val rejected = runCatching {
            repository.commitRecorderStart(admission.ownerToken, candidate)
        }

        assertTrue(rejected.exceptionOrNull() is android.database.sqlite.SQLiteException)
        assertEquals(before, databaseSnapshot())
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_atomic_acquisition")

        val first = repository.commitRecorderStart(admission.ownerToken, candidate)
        val resultLossRetry = repository.commitRecorderStart(admission.ownerToken, candidate)

        assertEquals(first, resultLossRetry)
        assertEquals(CanonicalTuple(0, 0), first.durableTuple)
        assertEquals(candidate, requireGraph(SESSION_ID))
    }

    @Test
    fun cacheHitPrepareAndEveryDirectMutationFrontDoorFailWhileRecorderOwnsOrder() = runBlocking {
        val outcomes = listOf(
            directFrontDoorOutcome("prepare", withRecording = false) { repository ->
                repository.prepareRecorder()
                Unit
            },
            directFrontDoorOutcome("display", withRecording = false) { repository ->
                repository.appendSessionDisplayMetadata(
                    expected = expected(CanonicalTuple(0, 0)),
                    nextTuple = CanonicalTuple(0, 1),
                    nextJson = DISPLAY_METADATA_WITH_ENTRY
                )
                Unit
            },
            directFrontDoorOutcome("phase", withRecording = false) { repository ->
                repository.transitionPhase(
                    expected = expected(CanonicalTuple(0, 0)),
                    nextTuple = CanonicalTuple(0, 1),
                    nextPhase = nextPhase(PHASE_1_ID, 0, 1)
                )
                Unit
            },
            directFrontDoorOutcome("recording", withRecording = false) { repository ->
                repository.startHeartRateRecording(
                    expected = expected(CanonicalTuple(0, 0)),
                    nextTuple = CanonicalTuple(0, 1),
                    recording = activeRecording().copy(startedMutationSequence = 1),
                    initialAcquisition = acquisition(
                        ACQUISITION_0_ID,
                        sequence = 0,
                        tuple = CanonicalTuple(0, 1),
                        state = "searching",
                        reason = "initial_acquisition"
                    )
                )
                Unit
            },
            directFrontDoorOutcome("acquisition", withRecording = true) { repository ->
                repository.transitionAcquisition(
                    expected = expected(
                        tuple = CanonicalTuple(0, 3),
                        recordingId = RECORDING_ID,
                        openAcquisitionId = ACQUISITION_0_ID
                    ),
                    nextTuple = CanonicalTuple(0, 4),
                    nextAcquisition = acquisition(
                        ACQUISITION_1_ID,
                        sequence = 1,
                        tuple = CanonicalTuple(0, 4),
                        state = "live",
                        reason = null
                    )
                )
                Unit
            },
            directFrontDoorOutcome("sample", withRecording = true) { repository ->
                repository.appendHeartRateSample(
                    expected = expected(
                        tuple = CanonicalTuple(0, 3),
                        recordingId = RECORDING_ID,
                        openAcquisitionId = ACQUISITION_0_ID
                    ),
                    nextTuple = CanonicalTuple(0, 4),
                    sample = HeartRateSampleEntity(
                        recordingId = RECORDING_ID,
                        sampleSequence = 0,
                        offsetMs = 0,
                        mutationSequence = 4,
                        bpm = 120
                    )
                )
                Unit
            }
        )

        assertEquals(
            emptyList<String>(),
            outcomes.filterNot { outcome ->
                outcome.failure is RecorderAdmissionBusyException &&
                    outcome.before == outcome.after
            }.map { outcome ->
                "${outcome.name}:${outcome.failure?.javaClass?.simpleName ?: "success"}:" +
                    "changed=${outcome.before != outcome.after}"
            }
        )
    }

    @Test
    fun durableTerminalResultLossRequiresTheCompleteProducedGraphAndOriginalBinding() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val admission = repository.admitRecorder("entry:durable-terminal")
        val candidate = CanonicalSessionGraphV1(
            session = canonicalSession(),
            phases = listOf(initialPhase()),
            recording = activeRecording().copy(startedMutationSequence = 0),
            acquisitions = listOf(
                acquisition(
                    id = ACQUISITION_0_ID,
                    sequence = 0,
                    tuple = CanonicalTuple(0, 0),
                    state = "not_observing",
                    reason = null
                )
            )
        )
        repository.commitRecorderStart(admission.ownerToken, candidate)
        val request = RecordingFinalizationRequest(
            sessionId = SESSION_ID,
            recordingId = RECORDING_ID,
            expectedStatus = "active",
            expectedTuple = CanonicalTuple(0, 0),
            finalOffsetMs = 10,
            terminalStatus = "completed",
            terminalReason = "completed",
            snapshotCreatedAt = "2026-09-04T00:00:00Z",
            authority = RecorderTerminalAuthority.ORDINARY
        )
        repository.finalizeRecorderRecording(admission.ownerToken, request)
        database.openHelper.writableDatabase.execSQL(
            "UPDATE heart_rate_analysis_snapshots SET quality_reasons_json=" +
                "'{\"qualityReasonsContractVersion\":1,\"sessionReasons\":[],\"phaseReasons\":[]}' " +
                "WHERE recording_id='$RECORDING_ID'"
        )

        val resultLossRetry = runCatching {
            repository.finalizeRecorderRecording(admission.ownerToken, request)
        }

        assertTrue(
            resultLossRetry.exceptionOrNull()?.stackTraceToString(),
            resultLossRetry.exceptionOrNull() is RecorderGuardedWriteException ||
                resultLossRetry.exceptionOrNull() is RecorderValidationException
        )
        assertEquals(1, database.canonicalTimelineHeartRateDao().analysisSnapshotCount())
    }

    @Test
    fun invalidStateReasonAndSampleAfterCutBothRollbackWithoutPartialWrites() = runBlocking {
        val repository = recordingRepository()
        val beforeInvalidPair = databaseSnapshot()

        val invalidPair = runCatching {
            repository.transitionAcquisition(
                expected = expected(
                    tuple = CanonicalTuple(0, 3),
                    openPhaseId = PHASE_0_ID,
                    recordingId = RECORDING_ID,
                    openAcquisitionId = ACQUISITION_0_ID
                ),
                nextTuple = CanonicalTuple(1, 4),
                nextAcquisition = acquisition(
                    id = ACQUISITION_1_ID,
                    sequence = 1,
                    tuple = CanonicalTuple(1, 4),
                    state = "live",
                    reason = "unexpected_disconnect"
                )
            )
        }

        assertTrue(invalidPair.exceptionOrNull() is RecorderValidationException)
        assertEquals(beforeInvalidPair, databaseSnapshot())

        val beforeAfterCut = databaseSnapshot()
        val afterCut = runCatching {
            repository.appendHeartRateSample(
                expected = expected(
                    tuple = CanonicalTuple(0, 3),
                    openPhaseId = PHASE_0_ID,
                    recordingId = RECORDING_ID,
                    openAcquisitionId = ACQUISITION_0_ID
                ),
                nextTuple = CanonicalTuple(1, 4),
                sample = HeartRateSampleEntity(
                    recordingId = RECORDING_ID,
                    sampleSequence = 0,
                    offsetMs = 2,
                    mutationSequence = 5,
                    bpm = 120
                )
            )
        }
        assertTrue(afterCut.exceptionOrNull() is RecorderValidationException)
        assertEquals(beforeAfterCut, databaseSnapshot())
    }

    @Test
    fun phaseGapOverlapNonTailOpenAndStaleExpectedTupleAreRejectedBeforeMutation() = runBlocking {
        val repository = emptyCanonicalRepository()
        val expected = expected(tuple = CanonicalTuple(0, 0))

        listOf(
            nextPhase("gap", startOffset = 11, startMutation = 1),
            nextPhase("overlap", startOffset = 9, startMutation = 1)
        ).forEach { invalidPhase ->
            val before = databaseSnapshot()
            val result = runCatching {
                repository.transitionPhase(
                    expected = expected,
                    nextTuple = CanonicalTuple(10, 1),
                    nextPhase = invalidPhase
                )
            }
            assertTrue(result.exceptionOrNull() is RecorderValidationException)
            assertEquals(before, databaseSnapshot())
        }

        val staleBefore = databaseSnapshot()
        val stale = runCatching {
            repository.appendSessionDisplayMetadata(
                expected = expected.copy(durableTuple = CanonicalTuple(1, 0)),
                nextTuple = CanonicalTuple(1, 1),
                nextJson = DISPLAY_METADATA_WITH_ENTRY
            )
        }
        assertTrue(stale.exceptionOrNull() is RecorderGuardedWriteException)
        assertEquals(staleBefore, databaseSnapshot())

        database.canonicalTimelineHeartRateDao().insertPhaseInterval(
            WorkoutPhaseIntervalEntity(
                id = "non-tail-closed",
                sessionId = SESSION_ID,
                sequence = 1,
                startOffsetMs = 0,
                endOffsetMs = 0,
                startMutationSequence = 0,
                endMutationSequence = 0,
                openMarker = null,
                phaseKind = "paused",
                phaseIdentityJson = VALID_PAUSED_PHASE_IDENTITY
            )
        )
        val nonTailBefore = databaseSnapshot()
        val nonTail = runCatching {
            repository.appendSessionDisplayMetadata(
                expected = expected,
                nextTuple = CanonicalTuple(0, 1),
                nextJson = DISPLAY_METADATA_WITH_ENTRY
            )
        }
        assertTrue(nonTail.exceptionOrNull() is RecorderValidationException)
        assertEquals(nonTailBefore, databaseSnapshot())
    }

    @Test
    fun guardedPhaseCloseRowCountZeroRollsBackTheHeaderAndTriggerMutation() = runBlocking {
        val repository = emptyCanonicalRepository()
        rebuildPhaseTableWithoutConstraints()
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER force_phase_guard_zero
            AFTER UPDATE OF last_durable_offset_ms ON workout_sessions
            WHEN NEW.id='$SESSION_ID'
            BEGIN
                DELETE FROM workout_phase_intervals WHERE id='$PHASE_0_ID';
            END
            """.trimIndent()
        )
        val before = databaseSnapshot()

        val result = runCatching {
            repository.transitionPhase(
                expected = expected(tuple = CanonicalTuple(0, 0)),
                nextTuple = CanonicalTuple(1, 1),
                nextPhase = nextPhase(PHASE_1_ID, startOffset = 1, startMutation = 1)
            )
        }

        val failure = result.exceptionOrNull()
        assertTrue(failure is RecorderGuardedWriteException)
        failure as RecorderGuardedWriteException
        assertEquals(0, failure.actualRowCount)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun guardedPhaseCloseRowCountTwoRollsBackTheHeaderAndInjectedDuplicate() = runBlocking {
        val repository = emptyCanonicalRepository()
        rebuildPhaseTableWithoutConstraints()
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER force_phase_guard_two
            AFTER UPDATE OF last_durable_offset_ms ON workout_sessions
            WHEN NEW.id='$SESSION_ID'
            BEGIN
                INSERT INTO workout_phase_intervals
                SELECT * FROM workout_phase_intervals WHERE id='$PHASE_0_ID' LIMIT 1;
            END
            """.trimIndent()
        )
        val before = databaseSnapshot()

        val result = runCatching {
            repository.transitionPhase(
                expected = expected(tuple = CanonicalTuple(0, 0)),
                nextTuple = CanonicalTuple(1, 1),
                nextPhase = nextPhase(PHASE_1_ID, startOffset = 1, startMutation = 1)
            )
        }

        val failure = result.exceptionOrNull()
        assertTrue(failure is RecorderGuardedWriteException)
        failure as RecorderGuardedWriteException
        assertEquals(2, failure.actualRowCount)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun concurrentProcessInterruptedTerminalAndMetadataAppendRemainSerializableAndAtomic() = runBlocking {
        val writer = emptyCanonicalRepository()
        val reconciler = WorkoutSessionRepository(database)

        val results = coroutineScope {
            val append = async(Dispatchers.IO) {
                runCatching {
                    writer.appendSessionDisplayMetadata(
                        expected = expected(tuple = CanonicalTuple(0, 0)),
                        nextTuple = CanonicalTuple(0, 1),
                        nextJson = DISPLAY_METADATA_WITH_ENTRY
                    )
                }
            }
            val reconcile = async(Dispatchers.IO) { runCatching { reconciler.prepareRecorder() } }
            append.await() to reconcile.await()
        }

        assertTrue(results.second.exceptionOrNull()?.stackTraceToString(), results.second.isSuccess)
        results.first.exceptionOrNull()?.let { failure ->
            assertTrue(failure is RecorderGuardedWriteException || failure is RecorderValidationException)
        }
        val graph = requireGraph(SESSION_ID)
        assertEquals("abandoned", graph.session.status)
        assertEquals("process_interrupted", graph.session.terminalReason)
        assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(graph))
        assertTrue(
            graph.session.sessionDisplayMetadataJson == VALID_DISPLAY_METADATA ||
                graph.session.sessionDisplayMetadataJson == DISPLAY_METADATA_WITH_ENTRY
        )
    }

    private suspend fun emptyCanonicalRepository(): WorkoutSessionRepository {
        val repository = WorkoutSessionRepository(database)
        repository.startCanonicalSession(canonicalSession(), initialPhase())
        return repository
    }

    private suspend fun directFrontDoorOutcome(
        name: String,
        withRecording: Boolean,
        action: suspend (WorkoutSessionRepository) -> Unit
    ): DirectFrontDoorOutcome {
        database.clearAllTables()
        val repository = WorkoutSessionRepository(database)
        repository.startCanonicalSession(canonicalSession(), initialPhase())
        if (withRecording) {
            repository.startHeartRateRecording(
                expected = expected(CanonicalTuple(0, 0)),
                nextTuple = CanonicalTuple(0, 3),
                recording = activeRecording(),
                initialAcquisition = acquisition(
                    ACQUISITION_0_ID,
                    sequence = 0,
                    tuple = CanonicalTuple(0, 3),
                    state = "searching",
                    reason = "initial_acquisition"
                )
            )
        }
        repository.admitRecorder("entry:front-door:$name")
        val before = databaseSnapshot()
        val failure = runCatching { action(repository) }.exceptionOrNull()
        return DirectFrontDoorOutcome(name, failure, before, databaseSnapshot())
    }

    private suspend fun recordingRepository(): WorkoutSessionRepository {
        val repository = emptyCanonicalRepository()
        repository.startHeartRateRecording(
            expected = expected(tuple = CanonicalTuple(0, 0)),
            nextTuple = CanonicalTuple(0, 3),
            recording = activeRecording(),
            initialAcquisition = acquisition(
                id = ACQUISITION_0_ID,
                sequence = 0,
                tuple = CanonicalTuple(0, 3),
                state = "searching",
                reason = "initial_acquisition"
            )
        )
        return repository
    }

    private fun canonicalSession() = WorkoutSessionEntity(
        id = SESSION_ID,
        mode = "timed",
        status = "active",
        planSnapshotJson = VALID_PLAN_SNAPSHOT,
        timelineVersion = 1,
        lastDurableOffsetMs = 0,
        lastMutationSequence = 0,
        displayMetadataContractVersion = 1,
        sessionDisplayMetadataJson = VALID_DISPLAY_METADATA
    )

    private fun initialPhase() = WorkoutPhaseIntervalEntity(
        id = PHASE_0_ID,
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

    private fun pausedPhase() = WorkoutPhaseIntervalEntity(
        id = PHASE_1_ID,
        sessionId = SESSION_ID,
        sequence = 1,
        startOffsetMs = 0,
        endOffsetMs = null,
        startMutationSequence = 2,
        endMutationSequence = null,
        openMarker = 1,
        phaseKind = "paused",
        phaseIdentityJson = VALID_PAUSED_PHASE_IDENTITY
    )

    private fun nextPhase(id: String, startOffset: Long, startMutation: Long) =
        WorkoutPhaseIntervalEntity(
            id = id,
            sessionId = SESSION_ID,
            sequence = 1,
            startOffsetMs = startOffset,
            endOffsetMs = null,
            startMutationSequence = startMutation,
            endMutationSequence = null,
            openMarker = 1,
            phaseKind = "paused",
            phaseIdentityJson = VALID_PAUSED_PHASE_IDENTITY
        )

    private fun activeRecording() = HeartRateRecordingEntity(
        recordingId = RECORDING_ID,
        sessionId = SESSION_ID,
        status = "active",
        startedOffsetMs = 0,
        startedMutationSequence = 3,
        endedOffsetMs = null,
        endedMutationSequence = null,
        sourceContractVersion = 1,
        sourceKind = "ble_hrs",
        acquisitionContractVersion = 1,
        parameterSnapshotVersion = 1,
        originalAnalysisVersion = null
    )

    private fun acquisition(
        id: String,
        sequence: Int,
        tuple: CanonicalTuple,
        state: String,
        reason: String?
    ) = HeartRateAcquisitionIntervalEntity(
        id = id,
        recordingId = RECORDING_ID,
        sequence = sequence,
        startOffsetMs = tuple.offsetMs,
        endOffsetMs = null,
        startMutationSequence = tuple.mutationSequence,
        endMutationSequence = null,
        openMarker = 1,
        recordingIntent = "expected_recording",
        intentReason = null,
        deviceState = state,
        deviceReason = reason
    )

    private fun expected(
        tuple: CanonicalTuple,
        openPhaseId: String = PHASE_0_ID,
        recordingId: String? = null,
        openAcquisitionId: String? = null
    ) = RecorderExpectedState(
        sessionId = SESSION_ID,
        status = "active",
        durableTuple = tuple,
        openPhaseId = openPhaseId,
        recordingId = recordingId,
        openAcquisitionId = openAcquisitionId
    )

    private suspend fun requireGraph(sessionId: String): CanonicalSessionGraphV1 {
        val rows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows(sessionId))
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

    private fun rebuildPhaseTableWithoutConstraints() {
        database.openHelper.writableDatabase.apply {
            execSQL("PRAGMA foreign_keys=OFF")
            execSQL("ALTER TABLE workout_phase_intervals RENAME TO workout_phase_intervals_original")
            execSQL(
                """
                CREATE TABLE workout_phase_intervals (
                    id TEXT NOT NULL,
                    session_id TEXT NOT NULL,
                    sequence INTEGER NOT NULL,
                    start_offset_ms INTEGER NOT NULL,
                    end_offset_ms INTEGER,
                    start_mutation_sequence INTEGER NOT NULL,
                    end_mutation_sequence INTEGER,
                    open_marker INTEGER,
                    phase_kind TEXT NOT NULL,
                    phase_identity_json TEXT NOT NULL
                )
                """.trimIndent()
            )
            execSQL("INSERT INTO workout_phase_intervals SELECT * FROM workout_phase_intervals_original")
            execSQL("DROP TABLE workout_phase_intervals_original")
            execSQL("PRAGMA foreign_keys=ON")
        }
    }

    private fun legacySession(id: String, status: String) = WorkoutSessionEntity(
        id = id,
        mode = "timed",
        status = status,
        planSnapshotJson = "{\"title\":\"Legacy\",\"mode\":\"timed\",\"blocks\":[]}"
    )

    private fun databaseSnapshot(): List<String> {
        val sql = database.openHelper.writableDatabase
        return listOf(
            "workout_sessions" to "id",
            "workout_phase_intervals" to "id, sequence",
            "heart_rate_recordings" to "recording_id",
            "heart_rate_acquisition_intervals" to "id, sequence",
            "heart_rate_samples" to "recording_id, sample_sequence",
            "heart_rate_analysis_snapshots" to "recording_id, analysis_version"
        ).flatMap { (table, orderBy) ->
            sql.query("SELECT * FROM $table ORDER BY $orderBy").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(buildString {
                            append(table)
                            repeat(cursor.columnCount) { index ->
                                append('|')
                                if (cursor.isNull(index)) append("<NULL>") else append(cursor.getString(index))
                            }
                        })
                    }
                }
            }
        }
    }

    private data class DirectFrontDoorOutcome(
        val name: String,
        val failure: Throwable?,
        val before: List<String>,
        val after: List<String>
    )

    private companion object {
        const val SESSION_ID = "canonical-session"
        const val PHASE_0_ID = "canonical-session:phase:0"
        const val PHASE_1_ID = "canonical-session:phase:1"
        const val RECORDING_ID = "canonical-session:recording"
        const val ACQUISITION_0_ID = "canonical-session:acquisition:0"
        const val ACQUISITION_1_ID = "canonical-session:acquisition:1"
        const val VALID_DISPLAY_METADATA =
            "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
        const val DISPLAY_METADATA_WITH_ENTRY =
            "{\"displayMetadataContractVersion\":1,\"entries\":[{\"entityKind\":\"exercise\",\"stableId\":\"exercise-1\",\"displayNameAtFirstReference\":\"深蹲\",\"customNameAtFirstReference\":null,\"resolutionSource\":\"plan_snapshot\"}]}"
        const val VALID_PLAN_SNAPSHOT =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Timed\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"block\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[]}],\"preferences\":null,\"followAlong\":null}"
        val VALID_PHASE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"timed_work\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855\"},\"payload\":{\"variant\":\"warmup\",\"compositionVersion\":2,\"compositionBlockId\":\"block\",\"${"timelineStage" + "Id"}\":\"block:warmup\",\"timelineStageKind\":\"warmup\",\"stageGroupId\":\"block:warmup\",\"targetId\":\"block:warmup:target\",\"targetKind\":\"warmup\",\"roundIndex0\":null,\"stageGroupIndex0\":null,\"targetIndex0\":0,\"stageInstanceIndex0\":0,\"${"targetInstance" + "Index0"}\":0,\"stepIndex0\":0}}"
        val VALID_PAUSED_PHASE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"paused\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855\"},\"payload\":{\"variant\":\"paused\",\"compositionVersion\":2,\"compositionBlockId\":null,\"${"timelineStage" + "Id"}\":null,\"timelineStageKind\":null,\"stageGroupId\":null,\"targetId\":null,\"targetKind\":null,\"roundIndex0\":null,\"stageGroupIndex0\":null,\"targetIndex0\":null,\"stageInstanceIndex0\":null,\"${"targetInstance" + "Index0"}\":null,\"stepIndex0\":null}}"
    }
}
