package com.liujyks.trainflow.core.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1Validator
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.CanonicalValidationResult
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.health.HeartRatePersistenceBindingDisposition
import com.liujyks.trainflow.core.health.HeartRatePersistenceBindingId
import com.liujyks.trainflow.core.health.HeartRateRuntimeAction
import com.liujyks.trainflow.core.health.HeartRateRuntimeObservation
import com.liujyks.trainflow.core.health.HeartRateRuntimeObservationCause
import com.liujyks.trainflow.core.health.HeartRateRuntimeObservationPayload
import com.liujyks.trainflow.core.health.HeartRateRuntimeOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class WorkoutSessionTimelineRecorderTest {
    private lateinit var database: TrainFlowDatabase
    private lateinit var runtimeOwner: HeartRateRuntimeOwner
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TrainFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runtimeOwner = HeartRateRuntimeOwner(context, Handler(Looper.getMainLooper()))
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        runtimeOwner.close()
        shadowOf(Looper.getMainLooper()).idle()
        database.close()
    }

    @Test
    fun atomicStartFoldsSnapshotAndFrozenReceiptsBeforePublishingStarted() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val request = startRequest(recording = activeRecording())
        val recorder = WorkoutSessionTimelineRecorder.prepare(
            entryId = "entry:atomic",
            startRequest = request,
            repository = repository,
            runtimeOwner = runtimeOwner,
            scope = scope
        )
        runtimeOwner.submit(HeartRateRuntimeAction.Enable)
        shadowOf(Looper.getMainLooper()).idle()

        val result = recorder.start(request)

        assertEquals(WorkoutSessionTimelineRecorderState.Started, recorder.state.value)
        assertEquals(CanonicalTuple(0, 1), result.durableTuple)
        val graph = requireGraph()
        assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(graph))
        assertEquals(listOf("not_observing", "no_source_selected"), graph.acquisitions.map { it.deviceState })
        assertEquals(listOf(null, "source_not_selected"), graph.acquisitions.map { it.deviceReason })
        assertEquals(listOf(0, 1), graph.acquisitions.map { it.sequence })
        assertEquals(0, graph.samples.size)
    }

    @Test
    fun startFailureRollsBackAllRowsUnbindsAndAllowsALaterFreshAdmission() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:rollback", activeRecording())
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_start BEFORE INSERT ON workout_sessions BEGIN SELECT RAISE(ABORT, 'reject start'); END"
        )

        val failed = runCatching {
            runRecorderCall { recorder.start(startRequest(recording = activeRecording())) }
        }

        assertTrue(failed.exceptionOrNull() is android.database.sqlite.SQLiteException)
        assertEquals(emptyList<String>(), databaseSnapshot())
        assertEquals(RecorderOwnerState.Open, repository.recorderOwnerState())
        assertTrue(runtimeOwner.persistenceBindingDisposition(bindingIdFor(1)) is
            HeartRatePersistenceBindingDisposition.KnownAbsent)

        database.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_start")
        val later = prepared(repository, "entry:later", activeRecording())
        assertEquals(WorkoutSessionTimelineRecorderState.Prepared, later.state.value)
    }

    @Test
    fun ownerLeasePreservesPrimarySecondaryAndOnlyExactCleanupReopensAdmission() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val admission = repository.admitRecorder("entry:owner")
        val primary = IllegalStateException("bind result uncertain")
        val secondary = RecorderBindingDispositionException("conflicting installed binding")
        val knownAbsent = runtimeOwner.persistenceBindingDisposition(admission.bindingId) as
            HeartRatePersistenceBindingDisposition.KnownAbsent
        val cleanupProof = RecorderCleanupProof.KnownAbsent(knownAbsent)

        assertEquals(
            RecorderOwnerReleaseResult.Stale,
            repository.releaseRecorderOwner(RecorderOwnerToken(admission.ownerToken.value + 1))
        )
        val pending = repository.beginRecorderOwnerClearHandoff(admission.ownerToken) as
            RecorderOwnerClearResult.Pending
        assertEquals(
            RecorderOwnerBlockResult.Stale,
            repository.blockRecorderOwner(
                ownerToken = RecorderOwnerToken(admission.ownerToken.value + 1),
                handoffToken = pending.handoffToken,
                primaryCause = primary,
                secondaryCause = secondary
            )
        )
        assertEquals(
            RecorderOwnerReleaseResult.Stale,
            repository.releaseRecorderOwner(
                ownerToken = admission.ownerToken,
                cleanupProof = cleanupProof,
                handoffToken = RecorderHandoffToken(pending.handoffToken.value + 1)
            )
        )
        val block = repository.blockRecorderOwner(
            ownerToken = admission.ownerToken,
            handoffToken = pending.handoffToken,
            primaryCause = primary,
            secondaryCause = secondary
        ) as RecorderOwnerBlockResult.Blocked

        val blockedState = repository.recorderOwnerState() as RecorderOwnerState.Blocked
        assertSame(primary, blockedState.primaryCause)
        assertSame(secondary, blockedState.secondaryCause)
        val blocked = runCatching { repository.admitRecorder("entry:blocked") }
        assertTrue(blocked.exceptionOrNull() is RecorderOwnerBlockedException)
        assertEquals(
            RecorderOwnerReleaseResult.CleanupRequired,
            repository.releaseRecorderOwner(admission.ownerToken)
        )
        assertEquals(
            RecorderOwnerReleaseResult.Stale,
            repository.releaseRecorderOwner(
                ownerToken = admission.ownerToken,
                cleanupProof = cleanupProof,
                blockToken = RecorderBlockToken(block.blockToken.value + 1)
            )
        )
        assertEquals(
            RecorderOwnerReleaseResult.Released,
            repository.releaseRecorderOwner(
                ownerToken = admission.ownerToken,
                cleanupProof = cleanupProof,
                blockToken = block.blockToken
            )
        )
        assertEquals(RecorderOwnerState.Open, repository.recorderOwnerState())

        val newer = repository.admitRecorder("entry:newer")
        assertTrue(newer.ownerToken.value > admission.ownerToken.value)
        assertEquals(
            RecorderOwnerReleaseResult.Stale,
            repository.releaseRecorderOwner(admission.ownerToken)
        )
        assertEquals(
            newer.ownerToken,
            (repository.recorderOwnerState() as RecorderOwnerState.Active).ownerToken
        )
    }

    @Test
    fun noRecordingKeepsOnlyLatestFactAndFirstEnableDoesNotBackfill() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:mid-enable", recording = null)
        recorder.start(startRequest(recording = null))
        runtimeOwner.submit(HeartRateRuntimeAction.Enable)
        shadowOf(Looper.getMainLooper()).idle()
        runtimeOwner.submit(HeartRateRuntimeAction.Disable)
        shadowOf(Looper.getMainLooper()).idle()
        recorder.enableRecording(activeRecording(), offsetMs = 5)
        recorder.setRecordingExpected(false, "user_turned_off", offsetMs = 6)
        recorder.setRecordingExpected(true, null, offsetMs = 7)

        val graph = requireGraph()
        assertEquals(RECORDING_ID, graph.recording?.recordingId)
        assertEquals(5L, graph.recording?.startedOffsetMs)
        assertEquals(0, graph.samples.size)
        assertEquals(
            listOf("expected_recording", "user_excluded", "expected_recording"),
            graph.acquisitions.map { it.recordingIntent }
        )
        assertEquals(listOf(null, "user_turned_off", null), graph.acquisitions.map { it.intentReason })
        assertEquals(listOf("not_observing", "not_observing", "not_observing"),
            graph.acquisitions.map { it.deviceState })
    }

    @Test
    fun wrongOrDuplicateReceiptAndFirstPersistenceFailureStopLaterCanonicalWrites() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:failure", activeRecording())
        recorder.start(startRequest(recording = activeRecording()))
        val bindingId = recorder.bindingId
        val before = databaseSnapshot()

        recorder.acceptRuntimeObservation(
            HeartRateRuntimeObservation(
                bindingId,
                receipt = 1,
                elapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                payload = HeartRateRuntimeObservationPayload.ValidMeasurement(91)
            )
        )
        withTimeout(2_000) {
            while (database.canonicalTimelineHeartRateDao().sampleCount() != 1) delay(1)
        }
        val confirmed = databaseSnapshot()
        recorder.acceptRuntimeObservation(
            HeartRateRuntimeObservation(
                HeartRatePersistenceBindingId("wrong"),
                receipt = 2,
                elapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                payload = HeartRateRuntimeObservationPayload.ValidMeasurement(92)
            )
        )
        awaitState(recorder) { it is WorkoutSessionTimelineRecorderState.ActivePersistenceFailed }

        val failed = recorder.state.value as WorkoutSessionTimelineRecorderState.ActivePersistenceFailed
        assertTrue(failed.cause is RecorderObservationRejectedException)
        assertTrue(confirmed != before)
        assertEquals(confirmed, databaseSnapshot())
        recorder.acceptRuntimeObservation(
            HeartRateRuntimeObservation(
                bindingId,
                receipt = 2,
                elapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                payload = HeartRateRuntimeObservationPayload.ValidMeasurement(93)
            )
        )
        delay(20)
        assertEquals(confirmed, databaseSnapshot())
        assertSame(failed.cause, (recorder.state.value as
            WorkoutSessionTimelineRecorderState.ActivePersistenceFailed).cause)
    }

    @Test
    fun recordingTerminalDelegatesToFinalizerAndSameRequestDoesNotCreateASecondSnapshot() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:terminal-hr", activeRecording())
        recorder.start(startRequest(recording = activeRecording()))
        val request = WorkoutTimelineTerminalRequest(
            terminalStatus = "completed",
            terminalReason = "completed",
            finalOffsetMs = 10,
            snapshotCreatedAt = "2026-09-04T00:00:00Z"
        )

        val first = runRecorderCall { recorder.terminalize(request) }
        val second = runRecorderCall {
            recorder.terminalize(request.copy(snapshotCreatedAt = "2030-01-01T00:00:00Z"))
        }

        assertEquals(first, second)
        assertEquals(1, database.canonicalTimelineHeartRateDao().analysisSnapshotCount())
        assertEquals("completed", requireGraph().session.status)
        assertEquals(WorkoutSessionTimelineRecorderState.Released, recorder.state.value)
        assertEquals(RecorderOwnerState.Open, repository.recorderOwnerState())
    }

    @Test
    fun noRecordingTerminalClosesOnlySessionAndPhaseAndCreatesNoHeartRateRows() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:terminal-no-hr", recording = null)
        recorder.start(startRequest(recording = null))

        runRecorderCall {
            recorder.terminalize(
                WorkoutTimelineTerminalRequest(
                    terminalStatus = "abandoned",
                    terminalReason = "user_abandoned",
                    finalOffsetMs = 8,
                    snapshotCreatedAt = "2026-09-04T00:00:00Z"
                )
            )
        }

        val graph = requireGraph()
        assertEquals("abandoned", graph.session.status)
        assertEquals("user_abandoned", graph.session.terminalReason)
        assertTrue(graph.phases.none { it.openMarker == 1 })
        assertEquals(null, graph.recording)
        assertTrue(graph.acquisitions.isEmpty())
        assertTrue(graph.samples.isEmpty())
        assertTrue(graph.snapshots.isEmpty())
    }

    @Test
    fun ownerClearInstallsPendingBeforeReturnBlocksAdmissionAndReleasesAfterTerminalCleanup() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:clear", recording = null)
        recorder.start(startRequest(recording = null))

        val handoff = recorder.beginOwnerClearHandoff(
            WorkoutTimelineOwnerClearRequest(
                finalOffsetMs = 9,
                snapshotCreatedAt = "2026-09-04T00:00:00Z"
            )
        )

        assertTrue(handoff is RecorderOwnerClearResult.Pending)
        assertTrue(repository.recorderOwnerState() is RecorderOwnerState.OwnerClearPending)
        val racingAdmission = async(Dispatchers.Default) {
            runCatching { repository.admitRecorder("entry:racing") }
        }.await()
        assertTrue(racingAdmission.exceptionOrNull() is RecorderAdmissionBusyException)
        awaitState(recorder) { it == WorkoutSessionTimelineRecorderState.Released }
        assertEquals("owner_cleared", requireGraph().session.terminalReason)
        assertEquals(RecorderOwnerState.Open, repository.recorderOwnerState())

        val newer = repository.admitRecorder("entry:newer")
        assertEquals(
            RecorderOwnerClearResult.AlreadyReleased,
            recorder.beginOwnerClearHandoff(
                WorkoutTimelineOwnerClearRequest(
                    finalOffsetMs = 10,
                    snapshotCreatedAt = "2030-01-01T00:00:00Z"
                )
            )
        )
        assertEquals(newer.ownerToken, (repository.recorderOwnerState() as RecorderOwnerState.Active).ownerToken)
    }

    @Test
    fun deterministicStartValidationRunsBeforeAdmissionOrRuntimeBinding() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val request = startRequest(activeRecording()).copy(
            initialPhase = initialPhase().copy(sessionId = "different-session")
        )

        val result = runCatching {
            WorkoutSessionTimelineRecorder.prepare(
                entryId = "entry:invalid-before-admission",
                startRequest = request,
                repository = repository,
                runtimeOwner = runtimeOwner,
                scope = scope
            )
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(RecorderOwnerState.Open, repository.recorderOwnerState())
        assertTrue(runtimeOwner.persistenceBindingDisposition(bindingIdFor(1)) is
            HeartRatePersistenceBindingDisposition.KnownAbsent)
        assertEquals(emptyList<String>(), databaseSnapshot())
    }

    @Test
    fun pendingCutRejectsLaterObservationAndCleanupSurvivesCallerScopeCancellation() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:cancelled-clear", activeRecording())
        recorder.start(startRequest(activeRecording()))

        val handoff = recorder.beginOwnerClearHandoff(
            WorkoutTimelineOwnerClearRequest(
                finalOffsetMs = 9,
                snapshotCreatedAt = "2026-09-04T00:00:00Z"
            )
        )
        assertTrue(handoff is RecorderOwnerClearResult.Pending)
        recorder.acceptRuntimeObservation(
            HeartRateRuntimeObservation(
                bindingId = recorder.bindingId,
                receipt = 1,
                elapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                payload = HeartRateRuntimeObservationPayload.ValidMeasurement(93)
            )
        )
        scope.cancel()

        awaitState(recorder) { it == WorkoutSessionTimelineRecorderState.Released }
        val graph = requireGraph()
        assertEquals("owner_cleared", graph.session.terminalReason)
        assertTrue(graph.samples.isEmpty())
        assertEquals(RecorderOwnerState.Open, repository.recorderOwnerState())
    }

    @Test
    fun activePersistenceFailureLatchesCauseAndOwnerClearBlocksWithoutFabricatingTerminal() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:active-failure-clear", activeRecording())
        recorder.start(startRequest(activeRecording()))
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_active_sample BEFORE INSERT ON heart_rate_samples " +
                "BEGIN SELECT RAISE(ABORT, 'reject active sample'); END"
        )

        recorder.acceptRuntimeObservation(
            HeartRateRuntimeObservation(
                bindingId = recorder.bindingId,
                receipt = 1,
                elapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                payload = HeartRateRuntimeObservationPayload.ValidMeasurement(94)
            )
        )
        awaitState(recorder) { it is WorkoutSessionTimelineRecorderState.ActivePersistenceFailed }
        val original = (recorder.state.value as
            WorkoutSessionTimelineRecorderState.ActivePersistenceFailed).cause

        val laterMutation = runCatching {
            recorder.setRecordingExpected(false, "user_turned_off", offsetMs = 1)
        }
        assertSame(original, laterMutation.exceptionOrNull())

        assertTrue(
            recorder.beginOwnerClearHandoff(
                WorkoutTimelineOwnerClearRequest(
                    finalOffsetMs = 2,
                    snapshotCreatedAt = "2026-09-04T00:00:00Z"
                )
            ) is RecorderOwnerClearResult.Pending
        )
        awaitOwnerState(repository) { it is RecorderOwnerState.Blocked }
        val blocked = repository.recorderOwnerState() as RecorderOwnerState.Blocked
        assertSame(original, blocked.primaryCause)
        assertEquals("active", requireGraph().session.status)
        assertEquals(null, requireGraph().session.terminalReason)
    }

    @Test
    fun terminalFailureThenOwnerClearBecomesBlockedWithTheOriginalCause() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:terminal-failure-clear", activeRecording())
        recorder.start(startRequest(activeRecording()))
        installTerminalFailureTrigger()

        val failed = runCatching {
            runRecorderCall { recorder.terminalize(completedRequest()) }
        }
        val original = requireNotNull(failed.exceptionOrNull())
        assertSame(
            original,
            (recorder.state.value as WorkoutSessionTimelineRecorderState.TerminalFailed).cause
        )

        assertTrue(
            recorder.beginOwnerClearHandoff(ownerClearRequest()) is
                RecorderOwnerClearResult.Pending
        )
        awaitOwnerState(repository) { it is RecorderOwnerState.Blocked }
        assertSame(
            original,
            (repository.recorderOwnerState() as RecorderOwnerState.Blocked).primaryCause
        )
        assertEquals("active", requireGraph().session.status)
    }

    @Test
    fun failedTerminalIntentRejectsDifferentSemanticsAndExactRetryReusesFirstMetadata() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:terminal-retry", activeRecording())
        recorder.start(startRequest(activeRecording()))
        installTerminalFailureTrigger()
        val first = completedRequest()

        assertTrue(runCatching { runRecorderCall { recorder.terminalize(first) } }.exceptionOrNull() is
            android.database.sqlite.SQLiteException)
        val conflicting = runCatching {
            runRecorderCall {
                recorder.terminalize(
                    first.copy(
                        terminalStatus = "abandoned",
                        terminalReason = "user_abandoned"
                    )
                )
            }
        }
        assertTrue(conflicting.exceptionOrNull() is RecorderTerminalConflictException)

        database.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_terminal_write")
        val result = runRecorderCall {
            recorder.terminalize(first.copy(snapshotCreatedAt = "2030-01-01T00:00:00Z"))
        }

        assertEquals(CanonicalTuple(10, 1), result.finalTuple)
        assertEquals("2026-09-04T00:00:00Z", requireGraph().snapshots.single().createdAt)
        assertEquals(1, database.canonicalTimelineHeartRateDao().analysisSnapshotCount())
    }

    @Test
    fun sameProcessRecorderRejectsProcessInterruptedWithoutChangingTheActiveGraph() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:terminal-authority", activeRecording())
        recorder.start(startRequest(activeRecording()))
        val before = databaseSnapshot()

        val rejected = runCatching {
            runRecorderCall {
                recorder.terminalize(
                    completedRequest().copy(
                        terminalStatus = "abandoned",
                        terminalReason = "process_interrupted"
                    )
                )
            }
        }

        assertTrue(rejected.exceptionOrNull() is RecorderValidationException)
        assertEquals(before, databaseSnapshot())
        assertEquals(WorkoutSessionTimelineRecorderState.Started, recorder.state.value)
        assertTrue(repository.recorderOwnerState() is RecorderOwnerState.Active)
    }

    @Test
    fun identicalIntentAndDeviceFactsAreNoOpsButEveryEqualBpmObservationPersists() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val request = startRequest(activeRecording())
        val recorder = WorkoutSessionTimelineRecorder.prepare(
            entryId = "entry:no-op-facts",
            startRequest = request,
            repository = repository,
            runtimeOwner = runtimeOwner,
            scope = scope
        )
        runtimeOwner.submit(HeartRateRuntimeAction.Enable)
        runtimeOwner.submit(HeartRateRuntimeAction.Enable)
        shadowOf(Looper.getMainLooper()).idle()

        recorder.start(request)
        recorder.setRecordingExpected(true, null, offsetMs = 0)
        recorder.acceptRuntimeObservation(
            HeartRateRuntimeObservation(
                recorder.bindingId,
                receipt = 3,
                elapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                payload = HeartRateRuntimeObservationPayload.ValidMeasurement(95)
            )
        )
        recorder.acceptRuntimeObservation(
            HeartRateRuntimeObservation(
                recorder.bindingId,
                receipt = 4,
                elapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                payload = HeartRateRuntimeObservationPayload.ValidMeasurement(95)
            )
        )
        withTimeout(2_000) {
            while (database.canonicalTimelineHeartRateDao().sampleCount() != 2) delay(1)
        }

        val graph = requireGraph()
        assertEquals(
            listOf("not_observing", "no_source_selected", "live"),
            graph.acquisitions.map { it.deviceState }
        )
        assertEquals(listOf(95, 95), graph.samples.map { it.bpm })
        assertEquals(listOf(2L, 3L), graph.samples.map { it.mutationSequence })
        assertEquals(3L, graph.session.lastMutationSequence)
    }

    @Test
    fun recorderSerializesDisplayThenPhaseThenConcurrentSampleOnOneMutationOrder() = runBlocking {
        val repository = WorkoutSessionRepository(database)
        val recorder = prepared(repository, "entry:mutation-order", activeRecording())
        recorder.start(startRequest(activeRecording()))

        recorder.appendSessionDisplayMetadata(DISPLAY_METADATA_WITH_ENTRY, offsetMs = 1)
        val phase = async(start = CoroutineStart.UNDISPATCHED) {
            recorder.transitionPhase(
                phaseId = PHASE_1_ID,
                phaseKind = "paused",
                phaseIdentityJson = VALID_PAUSED_PHASE_IDENTITY,
                offsetMs = 1
            )
        }
        recorder.acceptRuntimeObservation(
            HeartRateRuntimeObservation(
                recorder.bindingId,
                receipt = 1,
                elapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                payload = HeartRateRuntimeObservationPayload.ValidMeasurement(96)
            )
        )
        phase.await()
        withTimeout(2_000) {
            while (database.canonicalTimelineHeartRateDao().sampleCount() != 1) delay(1)
        }

        val graph = requireGraph()
        assertEquals(DISPLAY_METADATA_WITH_ENTRY, graph.session.sessionDisplayMetadataJson)
        assertEquals(listOf(0L, 2L), graph.phases.map { it.startMutationSequence })
        assertEquals(3L, graph.samples.single().mutationSequence)
        assertEquals(3L, graph.session.lastMutationSequence)
    }

    private suspend fun prepared(
        repository: WorkoutSessionRepository,
        entryId: String,
        recording: HeartRateRecordingEntity?
    ): WorkoutSessionTimelineRecorder {
        val request = startRequest(recording)
        return WorkoutSessionTimelineRecorder.prepare(
            entryId = entryId,
            startRequest = request,
            repository = repository,
            runtimeOwner = runtimeOwner,
            scope = scope
        )
    }

    private fun startRequest(recording: HeartRateRecordingEntity?) = WorkoutTimelineStartRequest(
        session = canonicalSession(),
        initialPhase = initialPhase(),
        recording = recording
    )

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

    private fun activeRecording() = HeartRateRecordingEntity(
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

    private suspend fun requireGraph(): CanonicalSessionGraphV1 {
        val rows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows(SESSION_ID))
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

    private suspend fun awaitState(
        recorder: WorkoutSessionTimelineRecorder,
        predicate: (WorkoutSessionTimelineRecorderState) -> Boolean
    ) {
        withTimeout(2_000) {
            while (!predicate(recorder.state.value)) {
                shadowOf(Looper.getMainLooper()).idle()
                delay(1)
            }
        }
    }

    private suspend fun awaitOwnerState(
        repository: WorkoutSessionRepository,
        predicate: (RecorderOwnerState) -> Boolean
    ) {
        withTimeout(2_000) {
            while (!predicate(repository.recorderOwnerState())) {
                shadowOf(Looper.getMainLooper()).idle()
                delay(1)
            }
        }
    }

    private suspend fun <T> runRecorderCall(block: suspend () -> T): T = coroutineScope {
        val call = async(start = CoroutineStart.UNDISPATCHED) { block() }
        while (!call.isCompleted) {
            shadowOf(Looper.getMainLooper()).idle()
            delay(1)
        }
        call.await()
    }

    private fun completedRequest() = WorkoutTimelineTerminalRequest(
        terminalStatus = "completed",
        terminalReason = "completed",
        finalOffsetMs = 10,
        snapshotCreatedAt = "2026-09-04T00:00:00Z"
    )

    private fun ownerClearRequest() = WorkoutTimelineOwnerClearRequest(
        finalOffsetMs = 10,
        snapshotCreatedAt = "2026-09-04T00:00:00Z"
    )

    private fun installTerminalFailureTrigger() {
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_terminal_write BEFORE UPDATE OF status ON workout_sessions " +
                "WHEN NEW.status != 'active' BEGIN SELECT RAISE(ABORT, 'reject terminal'); END"
        )
    }

    private fun bindingIdFor(token: Long) = HeartRatePersistenceBindingId("recorder-binding:$token")

    private companion object {
        const val SESSION_ID = "timeline-session"
        const val PHASE_ID = "timeline-session:phase:0"
        const val PHASE_1_ID = "timeline-session:phase:1"
        const val RECORDING_ID = "timeline-session:recording"
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
