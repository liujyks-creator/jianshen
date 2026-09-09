package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.health.HeartRateRuntimeOwner
import com.liujyks.trainflow.core.health.HeartRateBindingDisposition
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
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
import com.liujyks.trainflow.core.database.entity.TimedRestExtensionRecordEntity
import com.liujyks.trainflow.core.health.HeartRateObservation
import com.liujyks.trainflow.core.health.HeartRateObservationBinding
import com.liujyks.trainflow.core.health.HeartRateObservationBindingId
import com.liujyks.trainflow.core.health.HeartRateObservationCause
import com.liujyks.trainflow.core.health.HeartRateObservationPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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
    fun currentOwnerCannotApplyAnyActivityToAnotherSession() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val admission = repository.admitRecorder("A", collectedSession(), initialPhase())
        val other = WorkoutSessionRepository(database)
        val otherSession = collectedSession().copy(id = "other-session")
        val otherPhase = initialPhase().copy(id = "other-phase", sessionId = otherSession.id)
        val otherAdmission = other.admitRecorder("B", otherSession, otherPhase)
        val runtime = HeartRateRuntimeOwner(ApplicationProvider.getApplicationContext<Context>())
        val binding = (runtime.bindObservations(otherAdmission.bindingId) {} as HeartRateBindingDisposition.MatchingInstalled).binding
        val otherRequest = FrozenCanonicalStartRequest(otherSession, otherPhase, binding, emptyList())
        repository.startCanonicalSession(admission.ownerToken, frozenRequest(admission))
        val original = launch { other.startCanonicalSession(otherAdmission.ownerToken, otherRequest) }
        original.join()
        val otherExpected = RecorderExpectedState(otherSession.id, "active", CanonicalTuple(0, 0), otherPhase.id)
        val terminal = FrozenCanonicalFinalizationRequest(otherExpected, 1, "completed", "completed",
            null, null, null, null, VALID_DISPLAY_METADATA, emptyList(), emptyList(), emptyList(), null)
        val before = databaseSnapshot()
        val calls: List<suspend () -> Unit> = listOf(
            { repository.startCanonicalSession(admission.ownerToken, otherRequest); Unit },
            { repository.startCanonicalSession(admission.ownerToken, otherSession, otherPhase) },
            { repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(otherExpected, CanonicalTuple(1, 1))); Unit },
            { repository.appendSessionDisplayMetadata(admission.ownerToken, otherExpected, CanonicalTuple(1, 1), VALID_DISPLAY_METADATA) },
            { repository.transitionPhase(admission.ownerToken, otherExpected, CanonicalTuple(1, 1), otherPhase) },
            { repository.startHeartRateRecording(admission.ownerToken, otherExpected, CanonicalTuple(1, 1), activeRecording(), acquisition(ACQUISITION_0_ID, 0, CanonicalTuple(1, 1), "live", null)) },
            { repository.transitionAcquisition(admission.ownerToken, otherExpected, CanonicalTuple(1, 1), acquisition(ACQUISITION_0_ID, 0, CanonicalTuple(1, 1), "live", null)) },
            { repository.appendHeartRateSample(admission.ownerToken, otherExpected, CanonicalTuple(1, 1), HeartRateSampleEntity(RECORDING_ID, 0, 1, 1, 100)) },
            { repository.finalizeCanonicalSession(admission.ownerToken, terminal); Unit },
            { repository.finalizeRecordingSession(admission.ownerToken, RecordingFinalizationRequest(otherSession.id, RECORDING_ID, "active", CanonicalTuple(0, 0), 1, "completed", "completed", "2026-09-07T00:00:00Z")); Unit },
            { repository.resolveCanonicalStart(admission.ownerToken, otherRequest, original); Unit },
            { repository.beginOwnerClearHandoff(admission.ownerToken, otherSession.id) },
            { repository.reportRecorderActivityFailure(admission.ownerToken, otherSession.id, IllegalStateException("foreign")) },
            { repository.releaseRecorderBeforeStart(admission.ownerToken, otherSession.id, runtime, IllegalStateException("foreign")); Unit },
            { repository.releaseRecorderAfterTerminal(admission.ownerToken, otherSession.id, runtime) }
        )
        calls.forEachIndexed { index, call ->
            val failure = runCatching { call() }.exceptionOrNull()
            assertEquals("entry $index", "owner_session_mismatch", (failure as RecorderValidationException).code)
            assertEquals(before, databaseSnapshot())
            assertEquals(HeartRateBindingDisposition.MatchingInstalled(binding), runtime.queryObservationBinding(otherAdmission.bindingId))
        }
        assertTrue(runCatching { repository.beginOwnerClearHandoff(otherAdmission.ownerToken, SESSION_ID) }
            .exceptionOrNull() is RecorderStaleOwnerException)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun frozenStartRejectsAnotherAdmissionBinding() = runBlocking {
        val (repository, admission) = admittedRepository()
        val (_, foreign) = admittedRepository()
        val request = frozenRequest(foreign)
        val failure = runCatching { repository.startCanonicalSession(admission.ownerToken, request) }.exceptionOrNull()
        assertEquals("owner_binding_mismatch", (failure as RecorderValidationException).code)
        assertTrue(databaseSnapshot().isEmpty())
        assertEquals(CanonicalTuple(0, 0), repository.startCanonicalSession(admission.ownerToken, frozenRequest(admission)).durableTuple)
    }

    @Test
    fun clearDuringActivityAllowsOnlyOriginalCommitOrRollback() = runBlocking {
        for (rollback in listOf(false, true)) {
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val armed = AtomicBoolean(false)
            database.close()
            database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), TrainFlowDatabase::class.java)
                .allowMainThreadQueries().setQueryCallback(RoomDatabase.QueryCallback { sql, _ ->
                    if (sql.contains("UPDATE workout_sessions") && sql.contains("SET last_durable_offset_ms") && armed.compareAndSet(true, false)) {
                        entered.countDown()
                        check(release.await(5, TimeUnit.SECONDS))
                    }
                }, Executor { it.run() }).build()
            val (repository, admission) = recordingRepository()
            val confirmed = expected(CanonicalTuple(0, 3), recordingId = RECORDING_ID, openAcquisitionId = ACQUISITION_0_ID)
            val request = CanonicalActivityRequest(confirmed, CanonicalTuple(1, 4), nextStatus = "paused",
                nextPhase = pausedPhase().copy(startOffsetMs = 1, startMutationSequence = 4),
                nextAcquisition = acquisition(ACQUISITION_1_ID, 1, CanonicalTuple(1, 4), "live", null))
            val before = databaseSnapshot()
            if (rollback) database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_clear_activity BEFORE INSERT ON heart_rate_acquisition_intervals BEGIN SELECT RAISE(ABORT, 'clear_activity_rollback'); END")
            armed.set(true)
            val operation = async(Dispatchers.IO) { runCatching { repository.applyCanonicalActivity(admission.ownerToken, request) } }
            var queuedTerminal: kotlinx.coroutines.Deferred<Result<CanonicalFinalizationResult>>? = null
            try {
                val paused = entered.await(5, TimeUnit.SECONDS)
                assertTrue("activity did not reach SQL: ${if (operation.isCompleted) operation.await() else "still running"}", paused)
                queuedTerminal = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    runCatching { repository.finalizeCanonicalSession(admission.ownerToken,
                        FrozenCanonicalFinalizationRequest(confirmed, 2, "completed", "completed", null,
                            null, null, null, VALID_DISPLAY_METADATA, emptyList(), emptyList(), emptyList(), "2026-09-07T00:00:00Z")) }
                }
                assertFalse("terminal is only queued, outside its Room block", queuedTerminal.isCompleted)
                repository.beginOwnerClearHandoff(admission.ownerToken, SESSION_ID)
                val queued = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    runCatching { repository.applyCanonicalActivity(admission.ownerToken, request) }
                }
                assertTrue("clear must reject before waiting for Room", queued.isCompleted)
                assertTrue(queued.await().isFailure)
            } finally {
                release.countDown()
            }
            val result = operation.await()
            assertTrue(requireNotNull(queuedTerminal).await().isFailure)
            if (rollback) {
                assertTrue(result.exceptionOrNull() is android.database.sqlite.SQLiteConstraintException)
                assertEquals(before, databaseSnapshot())
            } else {
                assertEquals(CanonicalTuple(1, 4), result.getOrThrow().durableTuple)
                assertEquals("paused", requireGraph(SESSION_ID).session.status)
                assertEquals(2, requireGraph(SESSION_ID).acquisitions.size)
            }
            val durable = databaseSnapshot()
            assertTrue(requireGraph(SESSION_ID).snapshots.isEmpty())
            assertTrue(runCatching { repository.applyCanonicalActivity(admission.ownerToken, request) }.isFailure)
            assertTrue(runCatching { repository.prepareRecorder() }.isFailure)
            assertEquals(durable, databaseSnapshot())
        }
    }

    @Test
    fun clearDuringStartAllowsOnlyOriginalCommitOrRollback() = runBlocking {
        for (rollback in listOf(false, true)) {
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val once = AtomicBoolean(true)
            database.close()
            database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), TrainFlowDatabase::class.java)
                .allowMainThreadQueries()
                .setQueryCallback(RoomDatabase.QueryCallback { sql, _ ->
                    if (sql.contains("INSERT OR IGNORE INTO `workout_sessions`") && once.compareAndSet(true, false)) {
                        entered.countDown()
                        check(release.await(5, TimeUnit.SECONDS))
                    }
                }, Executor { it.run() }).build()
            val (repository, admission) = admittedRepository()
            val request = frozenRequest(admission, true)
            if (rollback) database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_clear_start BEFORE INSERT ON heart_rate_acquisition_intervals BEGIN SELECT RAISE(ABORT, 'clear_start_rollback'); END")
            val operation = async(Dispatchers.IO) { runCatching { repository.startCanonicalSession(admission.ownerToken, request) } }
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS))
                val activity = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    runCatching { repository.applyCanonicalActivity(admission.ownerToken,
                        CanonicalActivityRequest(expected(CanonicalTuple(0, 0)), CanonicalTuple(1, 1))) }
                }
                assertTrue("Start in flight must reject activity at entry", activity.isCompleted)
                assertTrue(activity.await().exceptionOrNull() is RecorderOwnerBusyException)
                repository.beginOwnerClearHandoff(admission.ownerToken, SESSION_ID)
                val queued = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    runCatching { repository.startCanonicalSession(admission.ownerToken, request) }
                }
                assertTrue("clear must reject before waiting for Room", queued.isCompleted)
                assertTrue(queued.await().isFailure)
            } finally {
                release.countDown()
            }
            val result = operation.await()
            if (rollback) {
                assertTrue(result.exceptionOrNull() is android.database.sqlite.SQLiteConstraintException)
                assertTrue(databaseSnapshot().isEmpty())
            } else {
                assertTrue(result.isSuccess)
                val graph = requireGraph(SESSION_ID)
                assertEquals(request.session, graph.session)
                assertEquals(request.recording, graph.recording)
                assertTrue(graph.snapshots.isEmpty())
            }
            val durable = databaseSnapshot()
            assertTrue(runCatching { repository.startCanonicalSession(admission.ownerToken, request) }.isFailure)
            assertTrue(runCatching { repository.prepareRecorder() }.isFailure)
            assertEquals(durable, databaseSnapshot())
        }
    }

    @Test
    fun startResolutionDoesNotRestoreWritePermission() = runBlocking {
        val (repository, admission) = admittedRepository()
        val request = frozenRequest(admission)
        val dispatches = LinkedBlockingQueue<Runnable>()
        val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) { dispatches.add(block) }
        }
        val operation = async(dispatcher, start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            repository.startCanonicalSession(admission.ownerToken, request)
        }
        val delivery = requireNotNull(dispatches.poll(5, TimeUnit.SECONDS))
        assertFalse(operation.isCompleted)
        assertEquals(request.session, requireGraph(SESSION_ID).session)
        val committed = databaseSnapshot()
        operation.cancel(CancellationException("start_delivery_lost"))
        delivery.run()
        assertTrue(runCatching { operation.await() }.exceptionOrNull() is CancellationException)
        val resolution = repository.resolveCanonicalStart(admission.ownerToken, request, operation)
        assertTrue(resolution is CanonicalStartResolution.Committed)
        val failure = runCatching {
            repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
                (resolution as CanonicalStartResolution.Committed).confirmedState, CanonicalTuple(1, 1)))
        }.exceptionOrNull()
        assertNotNull(failure)
        val runtime = HeartRateRuntimeOwner(ApplicationProvider.getApplicationContext<Context>())
        assertTrue(runCatching { repository.startCanonicalSession(admission.ownerToken, request) }.exceptionOrNull() is CancellationException)
        assertTrue(runCatching { repository.releaseRecorderBeforeStart(admission.ownerToken, SESSION_ID, runtime, requireNotNull(failure)) }.exceptionOrNull() is CancellationException)
        assertTrue(runCatching { repository.admitRecorder(admission.entryId, collectedSession(), initialPhase()) }.exceptionOrNull() is RecorderOwnerBusyException)
        assertEquals(committed, databaseSnapshot())
    }

    @Test
    fun activeSqlFailureClosesSubsequentWrites() = runBlocking {
        val (repository, admission) = recordingRepository()
        val confirmed = expected(CanonicalTuple(0, 3), recordingId = RECORDING_ID, openAcquisitionId = ACQUISITION_0_ID)
        val request = CanonicalActivityRequest(confirmed, CanonicalTuple(1, 4),
            nextAcquisition = acquisition(ACQUISITION_1_ID, 1, CanonicalTuple(1, 4), "live", null))
        val before = databaseSnapshot()
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_activity BEFORE INSERT ON heart_rate_acquisition_intervals BEGIN SELECT RAISE(ABORT, 'activity_original_failure'); END")
        val primary = runCatching { repository.applyCanonicalActivity(admission.ownerToken, request) }.exceptionOrNull()
        assertTrue(primary is android.database.sqlite.SQLiteConstraintException)
        assertEquals(before, databaseSnapshot())
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_activity")
        val repeated = runCatching { repository.applyCanonicalActivity(admission.ownerToken, request) }.exceptionOrNull()
        assertTrue("first real error must remain the write barrier", repeated === primary)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun roomFailureBeforeTransactionBodyStillClosesOwnerWithOriginalCause() = runBlocking {
        database.close()
        val transactionExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
        val queries = java.util.concurrent.atomic.AtomicInteger()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), TrainFlowDatabase::class.java)
            .allowMainThreadQueries().setTransactionExecutor(transactionExecutor)
            .setQueryCallback(RoomDatabase.QueryCallback { _, _ -> queries.incrementAndGet() }, Executor { it.run() }).build()
        try {
            val (repository, admission) = recordingRepository()
            val request = CanonicalActivityRequest(expected(CanonicalTuple(0, 3), recordingId = RECORDING_ID,
                openAcquisitionId = ACQUISITION_0_ID), CanonicalTuple(1, 4))
            transactionExecutor.shutdown()
            assertTrue(transactionExecutor.awaitTermination(5, TimeUnit.SECONDS))
            val before = queries.get()
            val original = runCatching { repository.applyCanonicalActivity(admission.ownerToken, request) }.exceptionOrNull()
            assertNotNull(original)
            assertEquals("Unable to acquire a thread to perform the database transaction.", original?.message)
            assertTrue(generateSequence(original) { it.cause }.any { it is java.util.concurrent.RejectedExecutionException })
            assertEquals("no Room body query can execute", before, queries.get())
            val repeated = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                runCatching { repository.applyCanonicalActivity(admission.ownerToken, request) }
            }
            try {
                assertTrue("failed Room scheduling must reject before another transaction is queued", repeated.isCompleted)
                assertTrue("Room failure before body must remain the owner barrier", original === repeated.await().exceptionOrNull())
            } finally {
                repeated.cancel()
            }
        } finally {
            transactionExecutor.shutdownNow()
        }
    }

    @Test
    fun persistedValidationAndReportedFailuresKeepFirstCause() = runBlocking {
        val (repository, admission) = recordingRepository()
        val confirmed = expected(CanonicalTuple(0, 3), recordingId = RECORDING_ID, openAcquisitionId = ACQUISITION_0_ID)
        val invalidInput = runCatching { repository.appendSessionDisplayMetadata(admission.ownerToken,
            confirmed, CanonicalTuple(1, 4), "{") }.exceptionOrNull()
        assertTrue(invalidInput is RecorderValidationException)
        val next = repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(confirmed, CanonicalTuple(1, 4)))
        database.openHelper.writableDatabase.execSQL("UPDATE workout_sessions SET session_display_metadata_json = '{' WHERE id = '$SESSION_ID'")
        val before = databaseSnapshot()
        val primary = runCatching { repository.applyCanonicalActivity(admission.ownerToken,
            CanonicalActivityRequest(next, CanonicalTuple(2, 5))) }.exceptionOrNull()
        assertTrue(primary is RecorderValidationException)
        assertTrue(primary !== invalidInput)
        val secondary = IllegalStateException("late_queue_failure")
        repository.reportRecorderActivityFailure(admission.ownerToken, SESSION_ID, requireNotNull(primary))
        repository.reportRecorderActivityFailure(admission.ownerToken, SESSION_ID, secondary)
        repository.reportRecorderActivityFailure(admission.ownerToken, SESSION_ID, secondary)
        assertEquals(listOf(secondary), primary.suppressed.toList())
        repository.beginOwnerClearHandoff(admission.ownerToken, SESSION_ID)
        repository.beginOwnerClearHandoff(admission.ownerToken, SESSION_ID)
        val calls: List<suspend () -> Unit> = listOf(
            { repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(next, CanonicalTuple(2, 5))); Unit },
            { repository.appendSessionDisplayMetadata(admission.ownerToken, next, CanonicalTuple(2, 5), VALID_DISPLAY_METADATA) },
            { repository.transitionPhase(admission.ownerToken, next, CanonicalTuple(2, 5), pausedPhase()) },
            { repository.startHeartRateRecording(admission.ownerToken, next, CanonicalTuple(2, 5), activeRecording(), acquisition(ACQUISITION_0_ID, 0, CanonicalTuple(2, 5), "live", null)) },
            { repository.transitionAcquisition(admission.ownerToken, next, CanonicalTuple(2, 5), acquisition(ACQUISITION_1_ID, 1, CanonicalTuple(2, 5), "live", null)) },
            { repository.appendHeartRateSample(admission.ownerToken, next, CanonicalTuple(2, 5), HeartRateSampleEntity(RECORDING_ID, 0, 2, 5, 100)) }
        )
        calls.forEach { assertTrue(runCatching { it() }.exceptionOrNull() === primary) }
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun frozenStartCommitsEveryReceiptAndNeverSamplesSnapshotOrLaterInput() = runBlocking {
        val (repository, admission) = admittedRepository()
        val bindingId = admission.bindingId
        val binding = HeartRateObservationBinding(
            bindingId, 500,
            HeartRateObservation(bindingId, 0, 500,
                HeartRateObservationPayload.CurrentSnapshot(HeartRateObservationCause.INITIAL_WAIT))
        )
        val receipts = mutableListOf(
            HeartRateObservation(bindingId, 1, 500,
                HeartRateObservationPayload.RuntimeTransition(HeartRateObservationCause.LIVE)),
            HeartRateObservation(bindingId, 2, 500, HeartRateObservationPayload.ValidMeasurement(88)),
            HeartRateObservation(bindingId, 3, 500, HeartRateObservationPayload.ValidMeasurement(88))
        )
        val request = FrozenCanonicalStartRequest(
            collectedSession(), initialPhase(), binding, receipts,
            activeRecording().copy(startedMutationSequence = 0)
        )
        receipts += HeartRateObservation(bindingId, 4, 501, HeartRateObservationPayload.ValidMeasurement(99))

        repository.startCanonicalSession(admission.ownerToken, request)

        val graph = requireGraph(SESSION_ID)
        assertEquals(request.recording, graph.recording)
        assertEquals(request.session.copy(lastMutationSequence = 3), graph.session)
        assertEquals(listOf(initialPhase()), graph.phases)
        assertEquals(listOf(
            acquisition("$RECORDING_ID:acquisition:0", 0, CanonicalTuple(0, 0), "waiting_first_sample", "initial_acquisition")
                .copy(endOffsetMs = 0, endMutationSequence = 1, openMarker = null),
            acquisition("$RECORDING_ID:acquisition:1", 1, CanonicalTuple(0, 1), "live", null)
        ), graph.acquisitions)
        assertEquals(listOf(
            HeartRateSampleEntity(RECORDING_ID, 0, 0, 2, 88),
            HeartRateSampleEntity(RECORDING_ID, 1, 0, 3, 88)
        ), graph.samples)
        assertTrue(graph.snapshots.isEmpty())
        assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(graph))
    }

    @Test
    fun combinedActivityCommitsStatusPhaseAcquisitionAndMetadataAtOneCut() = runBlocking {
        val (repository, admission) = recordingRepository()
        val recording = requireGraph(SESSION_ID).recording
        val cut = CanonicalTuple(0, 4)
        repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
            expected(CanonicalTuple(0, 3), recordingId = RECORDING_ID, openAcquisitionId = ACQUISITION_0_ID),
            cut, nextStatus = "paused",
            nextPhase = pausedPhase().copy(startMutationSequence = 4),
            nextAcquisition = acquisition(ACQUISITION_1_ID, 1, cut, "live", null),
            nextDisplayMetadataJson = DISPLAY_METADATA_WITH_ENTRY
        ))
        val graph = requireGraph(SESSION_ID)
        assertEquals("paused", graph.session.status)
        assertEquals(DISPLAY_METADATA_WITH_ENTRY, graph.session.sessionDisplayMetadataJson)
        assertEquals(recording, graph.recording)
        assertEquals(listOf(
            initialPhase().copy(endOffsetMs = 0, endMutationSequence = 4, openMarker = null),
            pausedPhase().copy(startMutationSequence = 4)
        ), graph.phases)
        assertEquals(listOf(
            acquisition(ACQUISITION_0_ID, 0, CanonicalTuple(0, 3), "searching", "initial_acquisition")
                .copy(endOffsetMs = 0, endMutationSequence = 4, openMarker = null),
            acquisition(ACQUISITION_1_ID, 1, cut, "live", null)
        ), graph.acquisitions)
        assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(graph))
    }

    @Test
    fun startResolutionRejectsSameTupleGraphWithChangedSessionValue() = runBlocking {
        val (repository, admission) = admittedRepository()
        val request = frozenRequest(admission)
        val operation = launch { repository.startCanonicalSession(admission.ownerToken, request) }
        operation.join()
        database.openHelper.writableDatabase.execSQL("UPDATE workout_sessions SET plan_id='changed' WHERE id='$SESSION_ID'")
        assertEquals(CanonicalStartResolution.ConflictingGraph,
            repository.resolveCanonicalStart(admission.ownerToken, request, operation))
    }

    @Test
    fun noHeartRateAndEnabledZeroSampleStartsHaveExactFrozenRows() = runBlocking {
        for (enabled in listOf(false, true)) {
            val (repository, admission) = admittedRepository()
            val request = frozenRequest(admission, enabled)
            repository.startCanonicalSession(admission.ownerToken, request)
            val graph = requireGraph(SESSION_ID)
            assertEquals(request.session, graph.session)
            assertEquals(listOf(initialPhase()), graph.phases)
            assertEquals(request.recording, graph.recording)
            assertEquals(if (enabled) listOf(acquisition("$RECORDING_ID:acquisition:0", 0,
                CanonicalTuple(0, 0), "live", null)) else emptyList<HeartRateAcquisitionIntervalEntity>(),
                graph.acquisitions)
            assertTrue(graph.samples.isEmpty())
            assertTrue(graph.snapshots.isEmpty())
            assertTrue(database.workoutSessionDao().stepRecordsForSession(SESSION_ID).isEmpty())
            assertTrue(database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID).isEmpty())
            assertTrue(database.workoutSessionDao().strengthSetRecordsForSession(SESSION_ID).isEmpty())
            repository.deleteAllSessions()
        }
    }

    @Test
    fun startFailuresAtCandidateInsertGuardAndReadbackRollbackAllNineTables() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        var admission = repository.admitRecorder("entry", collectedSession(), initialPhase())
        val runtime = HeartRateRuntimeOwner(ApplicationProvider.getApplicationContext<Context>())
                val base = frozenRequest(admission, true)
        var request = FrozenCanonicalStartRequest(base.session, base.initialPhase, base.binding,
            listOf(HeartRateObservation(base.binding.bindingId, 1, 500, HeartRateObservationPayload.ValidMeasurement(88)),
                HeartRateObservation(base.binding.bindingId, 2, 500, HeartRateObservationPayload.ValidMeasurement(88))),
            base.recording)
        val invalid = FrozenCanonicalStartRequest(request.session.copy(planSnapshotJson = "{}"),
            request.initialPhase, request.binding, request.receipts, request.recording)
        val before = databaseSnapshot()
        val validation = runCatching { repository.startCanonicalSession(admission.ownerToken, invalid) }.exceptionOrNull()
        assertTrue(validation is RecorderValidationException)
        assertEquals(before, databaseSnapshot())
        val triggers = listOf(
            "BEFORE INSERT ON heart_rate_acquisition_intervals BEGIN SELECT RAISE(ABORT, 'start_insert_original'); END",
            "BEFORE INSERT ON workout_sessions BEGIN SELECT RAISE(IGNORE); END",
            "AFTER INSERT ON heart_rate_acquisition_intervals BEGIN UPDATE workout_phase_intervals SET start_offset_ms=1; END",
            "AFTER INSERT ON heart_rate_acquisition_intervals BEGIN UPDATE workout_sessions SET plan_id='unexpected'; END",
            "AFTER INSERT ON heart_rate_acquisition_intervals BEGIN INSERT INTO session_step_records(id,session_id,step_id,kind,started_at,skipped) VALUES('extra','$SESSION_ID','step','work','2026-09-06T16:30:00Z',0); END",
            "BEFORE INSERT ON heart_rate_samples WHEN NEW.sample_sequence=1 BEGIN SELECT RAISE(ABORT, 'start_insert_original'); END"
        )
        triggers.forEachIndexed { index, body ->
            database.openHelper.writableDatabase.execSQL("CREATE TRIGGER start_failure $body")
            try {
                val failure = runCatching { repository.startCanonicalSession(admission.ownerToken, request) }.exceptionOrNull()
                assertNotNull("stage $index must fail", failure)
                when (index) {
                    0, 5 -> {
                        assertTrue(failure is android.database.sqlite.SQLiteConstraintException)
                        assertTrue(failure!!.message!!.contains("start_insert_original"))
                    }
                    1 -> {
                        assertTrue(failure is RecorderGuardedWriteException)
                        assertEquals(0, (failure as RecorderGuardedWriteException).actualRowCount)
                    }
                    else -> assertTrue(failure is RecorderValidationException)
                }
                assertEquals("stage $index", before, databaseSnapshot())
                assertTrue(repository.releaseRecorderBeforeStart(admission.ownerToken, SESSION_ID, runtime, requireNotNull(failure)) === failure)
                if (index < triggers.lastIndex) {
                    admission = repository.admitRecorder("entry", collectedSession(), initialPhase())
                    val next = frozenRequest(admission, true)
                    request = FrozenCanonicalStartRequest(next.session, next.initialPhase, next.binding,
                        request.receipts.map { it.copy(bindingId = admission.bindingId) }, next.recording)
                }
            } finally {
                database.openHelper.writableDatabase.execSQL("DROP TRIGGER start_failure")
            }
        }
    }

    @Test
    fun completeStartGraphRecognitionRejectsMissingExtraAndChangedRows() = runBlocking {
        val mutations = listOf(
            "UPDATE workout_sessions SET started_at='2026-09-06T16:30:01Z'",
            "UPDATE workout_sessions SET start_local_date='2026-09-08'",
            "UPDATE workout_sessions SET start_zone_id='UTC'",
            "UPDATE workout_sessions SET start_utc_offset_seconds=0",
            "UPDATE workout_sessions SET time_metadata_source_contract_version=2",
            "UPDATE workout_sessions SET plan_snapshot_json=plan_snapshot_json || ' '",
            "UPDATE workout_sessions SET session_display_metadata_json=session_display_metadata_json || ' '",
            "UPDATE workout_phase_intervals SET phase_identity_json=phase_identity_json || ' '",
            "DELETE FROM workout_phase_intervals",
            "UPDATE heart_rate_recordings SET alert_threshold_bpm=180",
            "DELETE FROM heart_rate_acquisition_intervals",
            "UPDATE heart_rate_acquisition_intervals SET device_state='stale'",
            "UPDATE heart_rate_samples SET bpm=89",
            "DELETE FROM heart_rate_samples WHERE sample_sequence=0",
            "INSERT INTO heart_rate_samples(recording_id,sample_sequence,offset_ms,mutation_sequence,bpm) VALUES('$RECORDING_ID',2,0,2,88)",
            "INSERT INTO heart_rate_analysis_snapshots(recording_id,analysis_version,created_at,input_last_mutation_sequence,sample_status,coverage_status,zone_status,canonical_sample_count,primary_point_sample_count,analysis_config_json,phase_aggregates_json,duration_breakdown_json,quality_reasons_json) VALUES('$RECORDING_ID',1,'2026-09-06T16:30:00Z',2,'samples_present','sufficient','no_zones',2,2,'{}','{}','{}','{}')",
            "INSERT INTO session_step_records(id,session_id,step_id,kind,started_at,skipped) VALUES('extra','$SESSION_ID','step','work','2026-09-06T16:30:00Z',0)",
            "INSERT INTO strength_set_records(id,session_id,exercise_id,set_order,set_kind) VALUES('extra','$SESSION_ID','exercise',0,'working')",
            "INSERT INTO timed_rest_extension_records(id,session_id,step_id,step_index,rest_stage_title,added_sec,planned_rest_sec,rest_elapsed_before_extension_sec,extension_at_remaining_sec,cumulative_extra_rest_sec,event_elapsed_sec) VALUES('extra','$SESSION_ID','step',0,'Rest',1,5,1,4,1,1)"
        )
        for (mutation in mutations) {
            val (repository, admission) = admittedRepository()
            val base = frozenRequest(admission, true)
            val request = FrozenCanonicalStartRequest(base.session, base.initialPhase, base.binding,
                listOf(HeartRateObservation(base.binding.bindingId, 1, 500, HeartRateObservationPayload.ValidMeasurement(88)),
                    HeartRateObservation(base.binding.bindingId, 2, 500, HeartRateObservationPayload.ValidMeasurement(88))),
                base.recording)
            val operation = launch { repository.startCanonicalSession(admission.ownerToken, request) }
            operation.join()
            val expected = expected(CanonicalTuple(0, 2), recordingId = RECORDING_ID,
                openAcquisitionId = "$RECORDING_ID:acquisition:0")
            assertEquals(CanonicalStartResolution.Committed(expected), repository.resolveCanonicalStart(admission.ownerToken, request, operation))
            database.openHelper.writableDatabase.execSQL(mutation)
            val changed = databaseSnapshot()
            assertEquals(mutation, CanonicalStartResolution.ConflictingGraph,
                repository.resolveCanonicalStart(admission.ownerToken, request, operation))
            assertEquals(changed, databaseSnapshot())
            repository.deleteAllSessions()
        }
    }

    @Test
    fun lostStartDeliveryRecognizesCommittedRowsWithoutReinserting() = runBlocking {
        val (repository, admission) = admittedRepository()
        val request = frozenRequest(admission, true)
        val dispatches = LinkedBlockingQueue<Runnable>()
        val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) { dispatches.add(block) }
        }
        var delivered = false
        val operation = async(dispatcher, start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { repository.startCanonicalSession(admission.ownerToken, request); delivered = true }
        val delivery = requireNotNull(dispatches.poll(5, TimeUnit.SECONDS))
        assertFalse(operation.isCompleted)
        val committed = databaseSnapshot()
        assertEquals(request.recording, requireGraph(SESSION_ID).recording)
        assertFalse(delivered)
        val original = CancellationException("start_delivery_lost")
        operation.cancel(original)
        delivery.run()
        val failure = runCatching { withTimeout(5_000) { operation.await() } }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        assertEquals(original.message, failure?.message)
        assertFalse(delivered)
        assertTrue(repository.resolveCanonicalStart(admission.ownerToken, request, operation) is CanonicalStartResolution.Committed)
        assertEquals(committed, databaseSnapshot())
    }

    @Test
    fun unfinishedStartIsUnresolvedAndOnlyEndedRollbackCanBeAbsent() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val blockOnce = AtomicBoolean(true)
        database.close()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), TrainFlowDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCallback(RoomDatabase.QueryCallback { query, _ ->
                if (query.contains("INSERT OR IGNORE INTO `workout_sessions`") && blockOnce.compareAndSet(true, false)) {
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                }
            }, Executor { it.run() }).build()
        val (repository, admission) = admittedRepository()
        val request = frozenRequest(admission, true)
        val operation = async(Dispatchers.IO) { repository.startCanonicalSession(admission.ownerToken, request) }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertEquals(CanonicalStartResolution.Unresolved, repository.resolveCanonicalStart(admission.ownerToken, request, operation))
            operation.cancel(CancellationException("start_rolled_back"))
        } finally {
            release.countDown()
        }
        val failure = runCatching { withTimeout(5_000) { operation.await() } }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        assertEquals("start_rolled_back", failure?.message)
        assertEquals(CanonicalStartResolution.RolledBack, repository.resolveCanonicalStart(admission.ownerToken, request, operation))
        assertTrue(databaseSnapshot().isEmpty())
        database.openHelper.writableDatabase.execSQL("INSERT INTO session_step_records(id,session_id,step_id,kind,started_at,skipped) VALUES('orphan','$SESSION_ID','step','work','2026-09-06T16:30:00Z',0)")
        assertEquals(CanonicalStartResolution.ConflictingGraph, repository.resolveCanonicalStart(admission.ownerToken, request, operation))
    }

    @Test
    fun frozenStartRejectsMissingRecordingForExpectedIntentAndInvalidTimeSources() = runBlocking {
        val (repository, admission) = admittedRepository()
        val base = frozenRequest(admission)
        val missingRecording = FrozenCanonicalStartRequest(base.session, base.initialPhase,
            base.binding, emptyList(), recording = null, heartRateEnabledAtStart = true)
        val before = databaseSnapshot()
        assertTrue("expected intent needs the initial recording in the same commit",
            runCatching { repository.startCanonicalSession(admission.ownerToken, missingRecording) }.isFailure)
        val invalidSessions = listOf(
            canonicalSession(),
            collectedSession().copy(startedAt = null),
            collectedSession().copy(startedAt = "invalid"),
            collectedSession().copy(startLocalDate = null),
            collectedSession().copy(startLocalDate = "2026-9-7"),
            collectedSession().copy(startLocalDate = "2026-09-06"),
            collectedSession().copy(startZoneId = "not/a/zone"),
            collectedSession().copy(startUtcOffsetSeconds = Long.MAX_VALUE),
            collectedSession().copy(timeMetadataSourceContractVersion = null),
            collectedSession().copy(timeMetadataSourceContractVersion = 6),
            collectedSession().copy(timelineVersion = 2)
        )
        invalidSessions.forEach { session ->
            val request = FrozenCanonicalStartRequest(session, base.initialPhase, base.binding,
                emptyList())
            assertTrue(session.toString(), runCatching { repository.startCanonicalSession(admission.ownerToken, request) }.isFailure)
            assertEquals(before, databaseSnapshot())
        }
    }

    @Test
    fun lateEnableOffOnResumeAndSameMillisecondSamplesKeepOneRecordingAndFrozenTime() = runBlocking {
        val (repository, admission) = admittedRepository()
        repository.startCanonicalSession(admission.ownerToken, frozenRequest(admission))
        val recording = activeRecording().copy(startedOffsetMs = 20, startedMutationSequence = 1)
        val first = acquisition(ACQUISITION_0_ID, 0, CanonicalTuple(20, 1), "live", null)
        var confirmed = repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
            expected(CanonicalTuple(0, 0)), CanonicalTuple(20, 1), newRecording = recording, nextAcquisition = first))
        assertEquals(recording, requireGraph(SESSION_ID).recording)
        val afterEnable = databaseSnapshot()
        val second = runCatching { repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
            confirmed, CanonicalTuple(20, 2), newRecording = recording.copy(recordingId = "second"),
            nextAcquisition = first.copy(id = "second", recordingId = "second"))) }
        assertTrue(second.exceptionOrNull() is RecorderGuardedWriteException)
        assertEquals(afterEnable, databaseSnapshot())
        confirmed = repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
            confirmed, CanonicalTuple(20, 2), nextStatus = "paused",
            nextPhase = pausedPhase().copy(startOffsetMs = 20, startMutationSequence = 2),
            nextAcquisition = acquisition(ACQUISITION_1_ID, 1, CanonicalTuple(20, 2), "live", null)
                .copy(recordingIntent = "user_excluded", intentReason = "user_turned_off")))
        confirmed = repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
            confirmed, CanonicalTuple(20, 3), nextStatus = "active",
            nextPhase = initialPhase().copy(id = "$SESSION_ID:phase:2", sequence = 2,
                startOffsetMs = 20, startMutationSequence = 3),
            nextAcquisition = acquisition("$RECORDING_ID:acquisition:2", 2, CanonicalTuple(20, 3), "live", null)))
        for (index in 0L..1L) {
            confirmed = repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
                confirmed, CanonicalTuple(20, 4 + index),
                sample = HeartRateSampleEntity(RECORDING_ID, index, 20, 4 + index, 88)))
        }
        val graph = requireGraph(SESSION_ID)
        assertEquals(collectedSession().copy(lastDurableOffsetMs = 20, lastMutationSequence = 5), graph.session)
        assertEquals(recording, graph.recording)
        assertEquals(listOf(first.copy(endOffsetMs = 20, endMutationSequence = 2, openMarker = null),
            acquisition(ACQUISITION_1_ID, 1, CanonicalTuple(20, 2), "live", null).copy(
                endOffsetMs = 20, endMutationSequence = 3, openMarker = null,
                recordingIntent = "user_excluded", intentReason = "user_turned_off"),
            acquisition("$RECORDING_ID:acquisition:2", 2, CanonicalTuple(20, 3), "live", null)), graph.acquisitions)
        assertEquals(listOf(HeartRateSampleEntity(RECORDING_ID, 0, 20, 4, 88),
            HeartRateSampleEntity(RECORDING_ID, 1, 20, 5, 88)), graph.samples)
        assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(graph))
    }

    @Test
    fun activityStaleGuardsAndSqlFailureNeverReturnAnAdvancedConfirmation() = runBlocking {
        val (repository, admission) = recordingRepository()
        val confirmed = expected(CanonicalTuple(0, 3), recordingId = RECORDING_ID, openAcquisitionId = ACQUISITION_0_ID)
        val before = databaseSnapshot()
        val staleStates = listOf(
            confirmed.copy(sessionId = "missing"), confirmed.copy(status = "paused"),
            confirmed.copy(durableTuple = CanonicalTuple(1, 3)),
            confirmed.copy(openPhaseId = "missing"), confirmed.copy(recordingId = "missing"),
            confirmed.copy(openAcquisitionId = "missing")
        )
        for (stale in staleStates) {
            val failure = runCatching { repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(stale,
                CanonicalTuple(1, 4), nextDisplayMetadataJson = DISPLAY_METADATA_WITH_ENTRY)) }.exceptionOrNull()
            if (stale.sessionId == "missing") {
                assertEquals("owner_session_mismatch", (failure as RecorderValidationException).code)
            } else {
                assertEquals("expected_state", (failure as RecorderGuardedWriteException).guard)
            }
            assertEquals(before, databaseSnapshot())
        }
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_activity BEFORE INSERT ON heart_rate_acquisition_intervals BEGIN SELECT RAISE(ABORT, 'activity_original_failure'); END")
        val failure = runCatching { repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
            confirmed, CanonicalTuple(1, 4), nextStatus = "paused",
            nextPhase = pausedPhase().copy(startOffsetMs = 1, startMutationSequence = 4),
            nextAcquisition = acquisition(ACQUISITION_1_ID, 1, CanonicalTuple(1, 4), "live", null),
            nextDisplayMetadataJson = DISPLAY_METADATA_WITH_ENTRY)) }.exceptionOrNull()
        assertTrue(failure is android.database.sqlite.SQLiteConstraintException)
        assertTrue(failure!!.message!!.contains("activity_original_failure"))
        assertEquals(before, databaseSnapshot())
        assertEquals(CanonicalTuple(0, 3), confirmed.durableTuple)
    }

    @Test
    fun lostActivityDeliveryDoesNotPublishNextConfirmedTuple() = runBlocking {
        val (repository, admission) = recordingRepository()
        val confirmed = expected(CanonicalTuple(0, 3), recordingId = RECORDING_ID, openAcquisitionId = ACQUISITION_0_ID)
        val dispatches = LinkedBlockingQueue<Runnable>()
        val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) { dispatches.add(block) }
        }
        var published = confirmed
        val operation = async(dispatcher, start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            published = repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
                confirmed, CanonicalTuple(1, 4), nextDisplayMetadataJson = DISPLAY_METADATA_WITH_ENTRY))
        }
        val delivery = requireNotNull(dispatches.poll(5, TimeUnit.SECONDS))
        assertFalse(operation.isCompleted)
        assertEquals(4L, requireGraph(SESSION_ID).session.lastMutationSequence)
        operation.cancel(CancellationException("activity_delivery_lost"))
        delivery.run()
        val failure = runCatching { withTimeout(5_000) { operation.await() } }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        assertEquals("activity_delivery_lost", failure?.message)
        assertEquals(confirmed, published)
    }

    @Test
    fun extraRestMetadataAppendsAtTheActivityCutAndDuplicateFailureRollsBackEverything() = runBlocking {
        val (repository, admission) = admittedRepository()
        val start = repository.startCanonicalSession(admission.ownerToken, frozenRequest(admission))
        val extension = TimedRestExtensionRecordEntity(
            id = "extension-1", sessionId = SESSION_ID, stepId = "rest-step", stepIndex = 1,
            roundIndex = 1, restStageId = "rest", restStageTitle = "Rest",
            addedSec = 15, plannedRestSec = 30, restElapsedBeforeExtensionSec = 5,
            extensionAtRemainingSec = 25, cumulativeExtraRestSec = 15, eventElapsedSec = 5
        )
        val beforeInvalid = databaseSnapshot()
        for (invalid in listOf(extension.copy(sessionId = "other-session"), extension.copy(addedSec = 0))) {
            assertTrue(runCatching { repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
                start, CanonicalTuple(5_000, 1), restExtension = invalid)) }.exceptionOrNull() is RecorderValidationException)
            assertEquals(beforeInvalid, databaseSnapshot())
        }
        val confirmed = repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
            start, CanonicalTuple(5_000, 1), nextDisplayMetadataJson = DISPLAY_METADATA_WITH_ENTRY,
            restExtension = extension))
        assertEquals(listOf(extension), database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID))
        assertEquals(CanonicalTuple(5_000, 1), confirmed.durableTuple)
        val before = databaseSnapshot()
        val duplicate = runCatching { repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
            confirmed, CanonicalTuple(5_000, 2), nextStatus = "paused",
            nextPhase = pausedPhase().copy(startOffsetMs = 5_000), restExtension = extension)) }
        assertTrue(duplicate.exceptionOrNull() is android.database.sqlite.SQLiteConstraintException)
        assertEquals(before, databaseSnapshot())
        assertEquals(CanonicalTuple(5_000, 1), confirmed.durableTuple)
    }

    @Test
    fun initializationNoOpsAndNoHeartRateReceiptsKeepIndependentMutationSequences() = runBlocking {
        for (enabled in listOf(false, true)) {
            val (repository, admission) = admittedRepository()
            val base = frozenRequest(admission, enabled)
            val request = FrozenCanonicalStartRequest(base.session, base.initialPhase, base.binding, listOf(
                HeartRateObservation(base.binding.bindingId, 1, 500,
                    HeartRateObservationPayload.RuntimeTransition(HeartRateObservationCause.LIVE)),
                HeartRateObservation(base.binding.bindingId, 2, 501, HeartRateObservationPayload.ValidMeasurement(88)),
                HeartRateObservation(base.binding.bindingId, 3, 501,
                    HeartRateObservationPayload.RuntimeTransition(HeartRateObservationCause.LIVE)),
                HeartRateObservation(base.binding.bindingId, 4, 501, HeartRateObservationPayload.ValidMeasurement(88))
            ), base.recording)
            val confirmed = repository.startCanonicalSession(admission.ownerToken, request)
            val graph = requireGraph(SESSION_ID)
            assertEquals(if (enabled) CanonicalTuple(1, 2) else CanonicalTuple(0, 0), confirmed.durableTuple)
            assertEquals(if (enabled) listOf(HeartRateSampleEntity(RECORDING_ID, 0, 1, 1, 88),
                HeartRateSampleEntity(RECORDING_ID, 1, 1, 2, 88)) else emptyList<HeartRateSampleEntity>(), graph.samples)
            assertEquals(if (enabled) 1 else 0, graph.acquisitions.size)
            repository.deleteAllSessions()
        }
    }

    @Test
    fun invalidFrozenSnapshotReceiptOrderAndRecordingVersionWriteNothing() = runBlocking {
        val (repository, admission) = admittedRepository()
        val base = frozenRequest(admission, true)
        val measurement = HeartRateObservation(base.binding.bindingId, 1, 500, HeartRateObservationPayload.ValidMeasurement(88))
        val invalidBatches = listOf(
            listOf(measurement.copy(receipt = 2)),
            listOf(measurement, measurement),
            listOf(measurement.copy(bindingId = HeartRateObservationBindingId())),
            listOf(measurement.copy(elapsedRealtimeMs = 499)),
            listOf(measurement.copy(payload = HeartRateObservationPayload.CurrentSnapshot(HeartRateObservationCause.LIVE)))
        )
        for (batch in invalidBatches) {
            val failure = runCatching { repository.startCanonicalSession(admission.ownerToken, FrozenCanonicalStartRequest(
                base.session, base.initialPhase, base.binding, batch, base.recording)) }.exceptionOrNull()
            assertTrue(failure is RecorderValidationException)
            assertTrue(databaseSnapshot().isEmpty())
        }
        for (binding in listOf(base.binding.copy(anchorElapsedRealtimeMs = 499),
            base.binding.copy(snapshot = base.binding.snapshot.copy(receipt = 1)),
            base.binding.copy(snapshot = base.binding.snapshot.copy(payload = HeartRateObservationPayload.ValidMeasurement(88))))) {
            assertTrue(runCatching { repository.startCanonicalSession(admission.ownerToken, FrozenCanonicalStartRequest(
                base.session, base.initialPhase, binding, emptyList(), base.recording)) }.exceptionOrNull() is RecorderValidationException)
            assertTrue(databaseSnapshot().isEmpty())
        }
        assertTrue(runCatching { repository.startCanonicalSession(admission.ownerToken, FrozenCanonicalStartRequest(
            base.session, base.initialPhase, base.binding, emptyList(),
            requireNotNull(base.recording).copy(sourceContractVersion = 2))) }.exceptionOrNull() is RecorderValidationException)
        assertTrue(databaseSnapshot().isEmpty())
    }

    @Test
    fun combinedActivityGuardRowCountsZeroAndTwoRestoreThePriorCompleteGraph() = runBlocking {
        assertCombinedActivityGuardRollback(0)
    }

    @Test
    fun combinedActivityGuardRowCountTwoRestoresThePriorCompleteGraph() = runBlocking {
        assertCombinedActivityGuardRollback(2)
    }

    private suspend fun assertCombinedActivityGuardRollback(rowCount: Int) {
        val (repository, admission) = recordingRepository()
        rebuildPhaseTableWithoutConstraints()
        val confirmed = expected(CanonicalTuple(0, 3), recordingId = RECORDING_ID, openAcquisitionId = ACQUISITION_0_ID)
        val before = databaseSnapshot()
            val mutation = if (rowCount == 0) "DELETE FROM workout_phase_intervals WHERE id='$PHASE_0_ID';"
                else "INSERT INTO workout_phase_intervals SELECT * FROM workout_phase_intervals WHERE id='$PHASE_0_ID' LIMIT 1;"
            database.openHelper.writableDatabase.execSQL("CREATE TRIGGER activity_guard AFTER UPDATE OF last_durable_offset_ms ON workout_sessions BEGIN $mutation END")
            try {
                val failure = runCatching { repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
                    confirmed, CanonicalTuple(1, 4), nextStatus = "paused",
                    nextPhase = pausedPhase().copy(startOffsetMs = 1, startMutationSequence = 4),
                    nextAcquisition = acquisition(ACQUISITION_1_ID, 1, CanonicalTuple(1, 4), "live", null),
                    nextDisplayMetadataJson = DISPLAY_METADATA_WITH_ENTRY)) }.exceptionOrNull()
                assertTrue(failure is RecorderGuardedWriteException)
                assertEquals(rowCount, (failure as RecorderGuardedWriteException).actualRowCount)
                assertEquals(before, databaseSnapshot())
                val blocked = runCatching { repository.applyCanonicalActivity(admission.ownerToken,
                    CanonicalActivityRequest(confirmed, CanonicalTuple(1, 4))) }.exceptionOrNull()
                assertTrue(blocked === failure)
                assertEquals(before, databaseSnapshot())
            } finally {
                database.openHelper.writableDatabase.execSQL("DROP TRIGGER activity_guard")
            }
    }

    @Test
    fun sharedTimeContractAcceptsUtcAndFixedSecondOffsetsAndRejectsCorruptActivityTime() = runBlocking {
        val sessions = listOf(
            collectedSession().copy(startedAt = "2026-09-07T00:00:00Z", startZoneId = "UTC", startUtcOffsetSeconds = 0),
            collectedSession().copy(startedAt = "2026-09-06T23:59:40Z", startZoneId = "+00:00:30", startUtcOffsetSeconds = 30)
        )
        for (session in sessions) {
            val (repository, admission) = admittedRepository()
            val base = frozenRequest(admission)
            val confirmed = repository.startCanonicalSession(admission.ownerToken, FrozenCanonicalStartRequest(
                session, base.initialPhase, base.binding, emptyList()))
            validateSessionTimeMetadata(requireGraph(SESSION_ID).session)
            assertEquals(session, requireGraph(SESSION_ID).session)
            database.openHelper.writableDatabase.execSQL("UPDATE workout_sessions SET time_metadata_source_contract_version=2")
            val before = databaseSnapshot()
            val failure = runCatching { repository.applyCanonicalActivity(admission.ownerToken, CanonicalActivityRequest(
                confirmed, CanonicalTuple(0, 1), nextDisplayMetadataJson = DISPLAY_METADATA_WITH_ENTRY)) }.exceptionOrNull()
            assertTrue(failure is RecorderValidationException)
            assertEquals(before, databaseSnapshot())
            repository.deleteAllSessions()
        }
        validateSessionTimeMetadata(canonicalSession())
        assertTrue(databaseSnapshot().isEmpty())
    }

    @Test
    fun malformedCollectedTimeIsRejectedBeforeCanonicalStart() = runBlocking {
        val (repository, admission) = admittedRepository()
        val before = databaseSnapshot()
        val result = runCatching {
            repository.startCanonicalSession(admission.ownerToken, collectedSession().copy(startUtcOffsetSeconds = 0), initialPhase())
        }
        assertTrue("offset must match the frozen instant and zone", result.isFailure)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun protectedWritesAcceptZeroDurationAndSameOffsetSequenceThroughTheRealGraphValidator() = runBlocking {
        database.workoutSessionDao().insertSession(legacySession("legacy-active", "active"))
        val repository = WorkoutSessionRepository(database)
        val gate = repository.prepareRecorder() as RecorderReconciliationResult.Succeeded
        assertEquals(listOf("legacy-active"), gate.legacyResiduals.map { it.sessionId })
        val admission = repository.admitRecorder("entry", collectedSession(), initialPhase())
        repository.startCanonicalSession(admission.ownerToken, canonicalSession(), initialPhase())
        repository.appendSessionDisplayMetadata(admission.ownerToken,
            expected = expected(tuple = CanonicalTuple(0, 0)),
            nextTuple = CanonicalTuple(0, 1),
            nextJson = DISPLAY_METADATA_WITH_ENTRY
        )
        repository.transitionPhase(admission.ownerToken,
            expected = expected(tuple = CanonicalTuple(0, 1)),
            nextTuple = CanonicalTuple(0, 2),
            nextPhase = pausedPhase()
        )
        repository.startHeartRateRecording(admission.ownerToken,
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
        repository.transitionAcquisition(admission.ownerToken,
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
        repository.appendHeartRateSample(admission.ownerToken,
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
    fun invalidStateReasonAndSampleAfterCutBothRollbackWithoutPartialWrites() = runBlocking {
        val (repository, admission) = recordingRepository()
        val beforeInvalidPair = databaseSnapshot()

        val invalidPair = runCatching {
            repository.transitionAcquisition(admission.ownerToken,
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
            repository.appendHeartRateSample(admission.ownerToken,
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
        val (repository, admission) = emptyCanonicalRepository()
        val expected = expected(tuple = CanonicalTuple(0, 0))

        listOf(
            nextPhase("gap", startOffset = 11, startMutation = 1),
            nextPhase("overlap", startOffset = 9, startMutation = 1)
        ).forEach { invalidPhase ->
            val before = databaseSnapshot()
            val result = runCatching {
                repository.transitionPhase(admission.ownerToken,
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
            repository.appendSessionDisplayMetadata(admission.ownerToken,
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
            repository.appendSessionDisplayMetadata(admission.ownerToken,
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
        val (repository, admission) = emptyCanonicalRepository()
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
            repository.transitionPhase(admission.ownerToken,
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
        val (repository, admission) = emptyCanonicalRepository()
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
            repository.transitionPhase(admission.ownerToken,
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
        val (writer, writerAdmission) = emptyCanonicalRepository()
        val reconciler = WorkoutSessionRepository(database)

        val results = coroutineScope {
            val append = async(Dispatchers.IO) {
                runCatching {
                    writer.appendSessionDisplayMetadata(writerAdmission.ownerToken,
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

    @Test
    fun missingSessionWithMatchingOwnerPreservesSqlGuard() = runBlocking {
        val missing = WorkoutSessionRepository(database)
        val missingAdmission = missing.admitRecorder("missing-entry", collectedSession().copy(id = "missing"),
            initialPhase().copy(sessionId = "missing"))
        recordingRepository()
        val before = databaseSnapshot()
        val failure = runCatching { missing.applyCanonicalActivity(missingAdmission.ownerToken,
            CanonicalActivityRequest(expected(CanonicalTuple(0, 3), RECORDING_ID, ACQUISITION_0_ID)
                .copy(sessionId = "missing"), CanonicalTuple(1, 4))) }.exceptionOrNull()
        assertEquals("expected_session", (failure as RecorderGuardedWriteException).guard)
        assertEquals(0, failure.actualRowCount)
        assertEquals(before, databaseSnapshot())
    }

    private suspend fun admittedRepository(): Pair<WorkoutSessionRepository, RecorderAdmission> {
        val repository = WorkoutSessionRepository(database)
        val admission = repository.admitRecorder("entry", collectedSession(), initialPhase())
        return repository to admission
    }

    private suspend fun emptyCanonicalRepository(): Pair<WorkoutSessionRepository, RecorderAdmission> {
        val (repository, admission) = admittedRepository()
        repository.startCanonicalSession(admission.ownerToken, canonicalSession(), initialPhase())
        return repository to admission
    }

    private suspend fun recordingRepository(): Pair<WorkoutSessionRepository, RecorderAdmission> {
        val (repository, admission) = emptyCanonicalRepository()
        repository.startHeartRateRecording(admission.ownerToken,
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
        return repository to admission
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

    private fun collectedSession() = canonicalSession().copy(
        startedAt = "2026-09-06T16:30:00Z",
        startLocalDate = "2026-09-07",
        startZoneId = "Asia/Shanghai",
        startUtcOffsetSeconds = 28800,
        timeMetadataSourceContractVersion = 1
    )

    private fun frozenRequest(admission: RecorderAdmission, recording: Boolean = false): FrozenCanonicalStartRequest {
        val bindingId = admission.bindingId
        return FrozenCanonicalStartRequest(
            collectedSession(), initialPhase(),
            HeartRateObservationBinding(bindingId, 500, HeartRateObservation(bindingId, 0, 500,
                HeartRateObservationPayload.CurrentSnapshot(HeartRateObservationCause.LIVE))),
            emptyList(), if (recording) activeRecording().copy(startedMutationSequence = 0) else null
        )
    }

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
            "heart_rate_analysis_snapshots" to "recording_id, analysis_version",
            "session_step_records" to "session_id, id",
            "timed_rest_extension_records" to "session_id, id",
            "strength_set_records" to "session_id, id"
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
