package com.liujyks.trainflow.core.data

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.database.sqlite.SQLiteConstraintException
import android.os.Looper
import android.os.SystemClock
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.CanonicalSessionGraphV1
import com.liujyks.trainflow.core.database.AnalysisSnapshotV1Validator
import com.liujyks.trainflow.core.database.CanonicalValidationResult
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.SessionStepRecordEntity
import com.liujyks.trainflow.core.database.entity.TimedRestExtensionRecordEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.health.E17GattShadow
import com.liujyks.trainflow.core.health.E17ScannerShadow
import com.liujyks.trainflow.core.health.HeartRateBindingDisposition
import com.liujyks.trainflow.core.health.HeartRateObservationBindingId
import com.liujyks.trainflow.core.health.HeartRateRuntimeAction
import com.liujyks.trainflow.core.health.HeartRateRuntimeOwner
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.feature.workoutsession.legacyTimedTransitionFactsV1
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothGatt
import org.robolectric.shadows.ShadowPausedLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [E17GattShadow::class, E17ScannerShadow::class])
@LooperMode(LooperMode.Mode.PAUSED)
@Suppress("DEPRECATION")
class WorkoutSessionTimelineRecorderTest {
    private lateinit var application: Application
    private lateinit var database: TrainFlowDatabase
    private lateinit var runtime: HeartRateRuntimeOwner
    private lateinit var scope: CoroutineScope
    private lateinit var recorder: WorkoutSessionTimelineRecorder
    private var anchor = 0L
    private val scopes = mutableListOf<CoroutineScope>()
    private val writes = Collections.synchronizedList(mutableListOf<String>())
    private var terminalReadbackMutation: ((String) -> Unit)? = null

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(application.getSystemService(BluetoothManager::class.java).adapter).setEnabled(true)
        E17GattShadow.resetFailures()
        E17ScannerShadow.resetFailures()
        database = newDatabase()
        runtime = HeartRateRuntimeOwner(application)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun frozenProducerPrefixCommitsBeforeLaterReceipts() = runRecorderTest {
        val cut = SqlCut("start")
        reset(cut)
        val connection = connect()
        bind(recording())
        connection.notify(88)
        connection.notify(88)
        val initialization = recorder.freezeStart()
        try {
            assertTrue(cut.entered.await(5, TimeUnit.SECONDS))
            connection.notify(91)
        } finally {
            cut.release.countDown()
        }
        assertEquals(CanonicalTuple(0, 3), await(initialization).durableTuple)
        withTimeout(5000) { recorder.progress.first { it.confirmedState?.durableTuple == CanonicalTuple(0, 4) } }
        assertGraph(prefixGraph().copy(
            session = session().copy(lastMutationSequence = 4),
            samples = prefixGraph().samples + HeartRateSampleEntity(RECORDING_ID, 2, 0, 4, 91)
        ))
    }

    @Test
    fun orderedModeIntentAndRawFactsKeepFrozenRecording() = runRecorderTest {
        val connection = connect()
        val frozen = WorkoutPlanSnapshot("legacy-timed-plan", "Legacy timed plan", WorkoutMode.TIMED,
            listOf(TimedCircuitBlock(id = "legacy-circuit", order = 1, rounds = 1,
                items = listOf(TimedExerciseItem(id = "legacy-work", labelOverride = "Legacy work",
                    stageType = TimedStageType.WORK, workDurationSec = 5, restAfterSec = 4)))))
        val prepared = (PlanSnapshotStorageV1Validator.prepare(frozen.toStorageJson(), WorkoutMode.TIMED)
            as PreparedPlanSnapshotStorageV1Result.Valid).prepared
        val startedAt = Instant.parse("2026-09-06T16:30:00Z")
        val initial = TimedWorkoutEngine.create(frozen, SESSION_ID)
        val start = TimedWorkoutEngine.dispatch(initial, WorkoutCommand.StartSession)
        val startFact = legacyTimedTransitionFactsV1(prepared, initial, start, startedAt).phaseStarts.single()
        val header = session().copy(planId = frozen.planId, planSnapshotJson = frozen.toStorageJson())
        bind(null, header, RecorderPhaseInput(startFact.phaseKind, startFact.phaseIdentityJson))
        await(recorder.freezeStart())
        at(10)
        connection.notify(81)
        at(20)
        assertEquals(CanonicalTuple(20, 1), submit(RecorderActivityInput(now(),
            intent = RecorderIntentInput.FirstEnable(recording()))).confirmedState.durableTuple)
        val pause = TimedWorkoutEngine.dispatch(start.state, WorkoutCommand.PauseSession)
        val pauseFact = legacyTimedTransitionFactsV1(prepared, start.state, pause, startedAt).phaseStarts.single()
        at(30)
        val paused = submit(RecorderActivityInput(now(), RecorderPhaseInput(pauseFact.phaseKind, pauseFact.phaseIdentityJson),
            "paused", RecorderIntentInput.SetEnabled(false)))
        assertEquals(CanonicalTuple(30, 2), paused.confirmedState.durableTuple)
        connection.notify(88)
        connection.notify(88)
        val resume = TimedWorkoutEngine.dispatch(pause.state, WorkoutCommand.ResumeSession)
        val resumeFact = legacyTimedTransitionFactsV1(prepared, pause.state, resume, startedAt).phaseStarts.single()
        at(40)
        val resumed = submit(RecorderActivityInput(now(), RecorderPhaseInput(resumeFact.phaseKind, resumeFact.phaseIdentityJson),
            "active", RecorderIntentInput.SetEnabled(true)))
        assertEquals(CanonicalTuple(40, 5), resumed.confirmedState.durableTuple)
        val tick = TimedWorkoutEngine.tick(resume.state, 5)
        val restFact = legacyTimedTransitionFactsV1(prepared, resume.state, tick, startedAt).phaseStarts.single()
        at(5000)
        val rest = submit(RecorderActivityInput(now(), RecorderPhaseInput(restFact.phaseKind, restFact.phaseIdentityJson)))
        assertEquals(CanonicalTuple(5000, 7), rest.confirmedState.durableTuple)
        val extend = TimedWorkoutEngine.dispatch(tick.state, WorkoutCommand.ExtendRest(15))
        val event = legacyTimedTransitionFactsV1(prepared, tick.state, extend, startedAt).restExtensions.single().record
        val extension = TimedRestExtensionRecordEntity("$SESSION_ID:${event.id}", SESSION_ID, event.stepId, event.stepIndex,
            event.roundIndex, event.restStageId, event.restStageTitle, event.previousStageId, event.previousStageTitle,
            event.addedSec, event.plannedRestSec, event.restElapsedBeforeExtensionSec, event.extensionAtRemainingSec,
            event.cumulativeExtraRestSec, event.eventElapsedSec)
        assertEquals(CanonicalTuple(5000, 8), submit(RecorderActivityInput(now(),
            nextDisplayMetadataJson = DISPLAY_METADATA_WITH_ENTRY, restExtension = extension)).confirmedState.durableTuple)
        val expectedPhases = listOf(
            phase(0, 0, 0, startFact.phaseKind, startFact.phaseIdentityJson).copy(endOffsetMs = 30, endMutationSequence = 2, openMarker = null),
            phase(1, 30, 2, pauseFact.phaseKind, pauseFact.phaseIdentityJson).copy(endOffsetMs = 40, endMutationSequence = 5, openMarker = null),
            phase(2, 40, 5, resumeFact.phaseKind, resumeFact.phaseIdentityJson).copy(endOffsetMs = 5000, endMutationSequence = 7, openMarker = null),
            phase(3, 5000, 7, restFact.phaseKind, restFact.phaseIdentityJson)
        )
        assertEquals(expectedPhases[1].copy(endOffsetMs = null, endMutationSequence = null, openMarker = 1), paused.openedPhase)
        assertEquals(expectedPhases[2].copy(endOffsetMs = null, endMutationSequence = null, openMarker = 1), resumed.openedPhase)
        assertEquals(expectedPhases[3], rest.openedPhase)
        assertGraph(CanonicalSessionGraphV1(
            header.copy(lastDurableOffsetMs = 5000, lastMutationSequence = 8, sessionDisplayMetadataJson = DISPLAY_METADATA_WITH_ENTRY),
            expectedPhases, recording().copy(startedOffsetMs = 20, startedMutationSequence = 1),
            listOf(acquisition(0, 20, 1, "live").copy(endOffsetMs = 30, endMutationSequence = 2, openMarker = null),
                acquisition(1, 30, 2, "live", enabled = false).copy(endOffsetMs = 40, endMutationSequence = 5, openMarker = null),
                acquisition(2, 40, 5, "live").copy(endOffsetMs = 2530, endMutationSequence = 6, openMarker = null),
                acquisition(3, 2530, 6, "stale", "sample_stale_timeout")),
            listOf(HeartRateSampleEntity(RECORDING_ID, 0, 30, 3, 88), HeartRateSampleEntity(RECORDING_ID, 1, 30, 4, 88))
        ), listOf(extension))
    }

    @Test
    fun noHeartRateAndEnabledZeroSampleKeepExactGraphs() = runRecorderTest {
        for (enabled in listOf(false, true)) {
            reset()
            bind(if (enabled) recording() else null)
            assertEquals(CanonicalTuple(0, 0), await(recorder.freezeStart()).durableTuple)
            assertGraph(zeroGraph(enabled))
        }
    }

    @Test
    fun firstStartSqlFailureClosesQueuedInputWithOriginalCause() = runRecorderTest {
        val cut = SqlCut("start")
        reset(cut)
        val connection = connect()
        bind(recording())
        connection.notify(88)
        connection.notify(88)
        trigger("start_insert_original")
        val initialization = recorder.freezeStart()
        try {
            assertTrue(cut.entered.await(5, TimeUnit.SECONDS))
            connection.notify(91)
        } finally {
            cut.release.countDown()
        }
        val cause = failure(initialization)
        assertTrue(cause is SQLiteConstraintException)
        assertTrue(cause.message.orEmpty().contains("start_insert_original"))
        assertSame(cause, recorder.progress.value.originalCause)
        assertNull(recorder.progress.value.confirmedState)
        dropTrigger()
        connection.notify(92)
        assertSame(cause, (recorder.offer(pause(1)) as RecorderSubmission.Closed).originalCause)
        joinWorker()
        assertTrue(databaseSnapshot().isEmpty())
        assertEquals(listOf("start"), writes.toList())
    }

    @Test
    fun firstActivitySqlFailureRetainsConfirmedPrefix() = runRecorderTest {
        bind(recording())
        val initial = await(recorder.freezeStart())
        val before = databaseSnapshot()
        trigger("activity_original_failure")
        at(1)
        val cause = failure((recorder.offer(pause(1)) as RecorderSubmission.Accepted).completion)
        assertTrue(cause is SQLiteConstraintException)
        assertTrue(cause.message.orEmpty().contains("activity_original_failure"))
        assertSame(cause, recorder.progress.value.originalCause)
        assertEquals(initial, recorder.progress.value.confirmedState)
        dropTrigger()
        at(2)
        assertSame(cause, (recorder.offer(resume(2)) as RecorderSubmission.Closed).originalCause)
        joinWorker()
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun clearStopsNewWritesAroundStartAndActivity() = runRecorderTest {
        for (stage in listOf("start", "activity")) for (rollback in listOf(false, true)) {
            val cut = SqlCut(stage)
            reset(cut)
            val connection = if (stage == "start") connect() else null
            bind(recording())
            val operation: Deferred<*>
            val before: List<String>
            if (stage == "start") {
                connection!!.notify(88)
                connection.notify(88)
                before = databaseSnapshot()
                if (rollback) trigger("clear_original_failure")
                operation = recorder.freezeStart()
            } else {
                await(recorder.freezeStart())
                before = databaseSnapshot()
                if (rollback) trigger("clear_original_failure")
                at(1)
                operation = (recorder.offer(pause(1)) as RecorderSubmission.Accepted).completion
            }
            lateinit var queued: Deferred<RecorderActivityConfirmation>
            try {
                assertTrue(cut.entered.await(5, TimeUnit.SECONDS))
                queued = (recorder.offer(if (stage == "start") pause(1) else resume(2)) as RecorderSubmission.Accepted).completion
                recorder.clear()
                assertTrue(recorder.progress.value.inputClosed)
                assertTrue(recorder.offer(resume(3)) is RecorderSubmission.Closed)
            } finally {
                cut.release.countDown()
            }
            if (rollback) {
                val cause = failure(operation)
                assertTrue(cause is SQLiteConstraintException)
                assertTrue(cause.message.orEmpty().contains("clear_original_failure"))
                assertSame(cause, recorder.progress.value.originalCause)
            } else {
                await(operation)
            }
            assertTrue(failure(queued) is CancellationException)
            joinWorker()
            assertTrue(recorder.progress.value.inputClosed)
            assertEquals(if (stage == "start") listOf("start") else listOf("start", "activity"), writes.toList())
            if (rollback) assertEquals(before, databaseSnapshot())
            else assertGraph(if (stage == "start") prefixGraph() else pausedGraph())
        }
    }

    @Test
    fun cancelledScopeStartsNoQueuedWrite() = runRecorderTest {
        val connection = connect()
        bind(recording())
        scope.cancel(CancellationException("s06a_scope_cancelled"))
        val initialization = recorder.freezeStart()
        assertTrue(failure(initialization) is CancellationException)
        assertTrue(recorder.offer(pause(1)) is RecorderSubmission.Closed)
        connection.notify(88)
        joinWorker()
        assertNull(recorder.progress.value.confirmedState)
        assertTrue(recorder.progress.value.inputClosed)
        assertTrue(writes.isEmpty())
        assertTrue(databaseSnapshot().isEmpty())
    }

    @Test
    fun cancelledScopeKeepsInFlightTransactionAtomic() = runRecorderTest {
        for (stage in listOf("start", "activity")) {
            val cut = SqlCut(stage)
            reset(cut)
            val connection = if (stage == "start") connect() else null
            bind(recording())
            val operation: Deferred<*>
            val before: List<String>
            val previous: RecorderExpectedState?
            if (stage == "start") {
                connection!!.notify(88)
                connection.notify(88)
                previous = null
                before = databaseSnapshot()
                operation = recorder.freezeStart()
            } else {
                previous = await(recorder.freezeStart())
                before = databaseSnapshot()
                at(1)
                operation = (recorder.offer(pause(1)) as RecorderSubmission.Accepted).completion
            }
            val newGraph = if (stage == "start") prefixGraph() else pausedGraph()
            lateinit var queued: Deferred<RecorderActivityConfirmation>
            try {
                assertTrue(cut.entered.await(5, TimeUnit.SECONDS))
                queued = (recorder.offer(if (stage == "start") pause(1) else resume(2)) as RecorderSubmission.Accepted).completion
                scope.cancel(CancellationException("s06a_scope_cancelled"))
            } finally {
                cut.release.countDown()
            }
            joinWorker()
            val cause = failure(operation)
            assertTrue(cause is CancellationException)
            assertTrue(generateSequence(cause) { it.cause }.any { it.message.orEmpty().contains("s06a_scope_cancelled") })
            assertTrue(failure(queued) is CancellationException)
            assertEquals(previous, recorder.progress.value.confirmedState)
            assertTrue(recorder.progress.value.inputClosed)
            assertTrue(recorder.offer(resume(3)) is RecorderSubmission.Closed)
            assertEquals(if (stage == "start") listOf("start") else listOf("start", "activity"), writes.toList())
            if (databaseSnapshot() != before) assertGraph(newGraph)
        }
    }

    @Test
    fun frozenTerminalDrainsAcceptedPrefixAndKeepsOriginalSaved() = runRecorderTest {
        val cut = SqlCut("start")
        reset(cut)
        val repository = WorkoutSessionRepository(database)
        val connection = connect()
        bind(recording(), repository = repository)
        val bindingId = (runtime.queryObservationBinding(HeartRateObservationBindingId())
            as HeartRateBindingDisposition.ConflictingInstalled).observedBindingId
        val installed = runtime.queryObservationBinding(bindingId)
        val steps = listOf(SessionStepRecordEntity(
            id = "execution-step", sessionId = SESSION_ID, stepId = "block:warmup:target",
            kind = "timed_work", blockId = "block", itemId = null, setPlanId = null,
            exerciseId = null, startedAt = "2026-09-06T16:30:00Z",
            endedAt = "2026-09-06T16:30:01Z", skipped = false,
            actualDurationSec = 1, plannedDurationSec = 10))
        val input = RecorderTerminalInput(anchor + 1000, RecorderTerminalKind.COMPLETED,
            "2026-09-06T16:30:01Z", 1, 1, 0, VALID_DISPLAY_METADATA,
            steps, emptyList(), emptyList(), "2026-09-06T16:30:01Z")
        connection.notify(88)
        connection.notify(88)
        val initialization = recorder.freezeStart()
        lateinit var submission: RecorderTerminalSubmission.Accepted
        try {
            assertTrue(cut.entered.await(5, TimeUnit.SECONDS))
            at(100)
            connection.notify(91)
            at(1000)
            submission = recorder.freezeTerminal(input) as RecorderTerminalSubmission.Accepted
            assertSame(submission, recorder.freezeTerminal(RecorderTerminalInput(anchor + 1100,
                RecorderTerminalKind.COMPLETED, "2099-01-01T00:00:00Z", 99, 1, 0,
                VALID_DISPLAY_METADATA, steps, emptyList(), emptyList(), "2099-01-01T00:00:00Z")))
            at(1100)
            connection.notify(99)
            assertTrue(recorder.offer(RecorderActivityInput(anchor + 1100)) is RecorderSubmission.Closed)
        } finally {
            cut.release.countDown()
        }
        await(initialization)
        val operation = submission.operation
        val saved = await(operation.saved)
        assertEquals(CanonicalFinalizationResult(SESSION_ID, RECORDING_ID, CanonicalTuple(1000, 5), 1), saved)
        val main = awaitCleanupDispatch(operation)
        val rows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows(SESSION_ID))
        val recorded = rows.recordings.single()
        val graph = CanonicalSessionGraphV1(rows.session, rows.phases, recorded.recording,
            recorded.acquisitions, recorded.samples, recorded.snapshots)
        val prefix = prefixGraph()
        val expected = prefix.copy(
            session = session().copy(status = "completed", endedAt = "2026-09-06T16:30:01Z",
                totalElapsedSec = 1, effectiveElapsedSec = 1, pausedElapsedSec = 0,
                lastDurableOffsetMs = 1000, lastMutationSequence = 5,
                trustedEndOffsetMs = 1000, terminalReason = "completed"),
            phases = listOf(phase().copy(endOffsetMs = 1000, endMutationSequence = 5, openMarker = null)),
            recording = recording().copy(status = "terminal", endedOffsetMs = 1000,
                endedMutationSequence = 5, originalAnalysisVersion = 1),
            acquisitions = prefix.acquisitions.dropLast(1) + prefix.acquisitions.last().copy(
                endOffsetMs = 1000, endMutationSequence = 5, openMarker = null),
            samples = prefix.samples + HeartRateSampleEntity(RECORDING_ID, 2, 100, 4, 91),
            snapshots = graph.snapshots)
        assertEquals(expected, graph)
        assertEquals(3, graph.samples.size)
        assertEquals("2026-09-06T16:30:01Z", graph.snapshots.single().createdAt)
        assertEquals(CanonicalValidationResult.Valid,
            AnalysisSnapshotV1Validator.validate(graph, graph.snapshots.single()))
        assertEquals(steps, database.workoutSessionDao().stepRecordsForSession(SESSION_ID))
        assertEquals(emptyList<Any>(), database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID))
        assertEquals(emptyList<Any>(), database.workoutSessionDao().strengthSetRecordsForSession(SESSION_ID))
        val durable = databaseSnapshot()
        assertEquals(installed, runtime.queryObservationBinding(bindingId))
        val admission = async(start = CoroutineStart.UNDISPATCHED) {
            repository.admitRecorder("s06b-next-entry",
                session().copy(id = "s06b-next-session"), phase().copy(id = "s06b-next-session:phase:0",
                    sessionId = "s06b-next-session"))
        }
        val cancellation = CancellationException("s06b_admission_probe_cancelled")
        try {
            assertFalse(admission.isCompleted)
            admission.cancel(cancellation)
            val cause = failure(admission)
            assertTrue(generateSequence(cause) { it.cause }.any { it === cancellation })
        } finally {
            admission.cancel(cancellation)
        }
        assertEquals(installed, runtime.queryObservationBinding(bindingId))
        assertEquals(durable, databaseSnapshot())
        main.idle()
        await(operation.released)
        assertEquals(HeartRateBindingDisposition.KnownAbsent, runtime.queryObservationBinding(bindingId))
        assertEquals(durable, databaseSnapshot())
        assertSame(submission, recorder.freezeTerminal(input))
        assertSame(saved, await(operation.saved))
        assertEquals(durable, databaseSnapshot())
        assertEquals(1, writes.count { it == "terminal" })
    }

    @Test
    fun noRecordingAndZeroSampleTerminalsKeepExactGraphs() = runRecorderTest {
        for (enabled in listOf(false, true)) {
            if (enabled) reset()
            bind(if (enabled) recording() else null)
            val bindingId = (runtime.queryObservationBinding(HeartRateObservationBindingId())
                as HeartRateBindingDisposition.ConflictingInstalled).observedBindingId
            val installed = runtime.queryObservationBinding(bindingId)
            await(recorder.freezeStart())
            assertGraph(zeroGraph(enabled))
            val input = RecorderTerminalInput(anchor,
                if (enabled) RecorderTerminalKind.USER_ABANDONED else RecorderTerminalKind.COMPLETED,
                "2026-09-06T16:30:00Z", 0, 0, 0, VALID_DISPLAY_METADATA,
                emptyList(), emptyList(), emptyList(), "2026-09-06T16:30:00Z")
            val operation = (recorder.freezeTerminal(input) as RecorderTerminalSubmission.Accepted).operation
            val saved = await(operation.saved)
            assertEquals(CanonicalFinalizationResult(SESSION_ID, if (enabled) RECORDING_ID else null,
                CanonicalTuple(0, 1), if (enabled) 1 else null), saved)
            val main = awaitCleanupDispatch(operation)
            val rows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows(SESSION_ID))
            assertEquals(if (enabled) 1 else 0, rows.recordings.size)
            val recorded = rows.recordings.singleOrNull()
            val graph = CanonicalSessionGraphV1(rows.session, rows.phases, recorded?.recording,
                recorded?.acquisitions.orEmpty(), recorded?.samples.orEmpty(), recorded?.snapshots.orEmpty())
            val expected = CanonicalSessionGraphV1(
                session().copy(status = if (enabled) "abandoned" else "completed",
                    endedAt = "2026-09-06T16:30:00Z", totalElapsedSec = 0, effectiveElapsedSec = 0,
                    pausedElapsedSec = 0, lastMutationSequence = 1, trustedEndOffsetMs = 0,
                    terminalReason = if (enabled) "user_abandoned" else "completed"),
                listOf(phase().copy(endOffsetMs = 0, endMutationSequence = 1, openMarker = null)),
                if (enabled) recording().copy(status = "terminal", endedOffsetMs = 0,
                    endedMutationSequence = 1, originalAnalysisVersion = 1) else null,
                if (enabled) listOf(acquisition(0, 0, 0, "not_observing")
                    .copy(endOffsetMs = 0, endMutationSequence = 1, openMarker = null)) else emptyList(),
                emptyList(), if (enabled) graph.snapshots else emptyList())
            assertEquals(expected, graph)
            assertEquals(emptyList<Any>(), database.workoutSessionDao().stepRecordsForSession(SESSION_ID))
            assertEquals(emptyList<Any>(), database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID))
            assertEquals(emptyList<Any>(), database.workoutSessionDao().strengthSetRecordsForSession(SESSION_ID))
            if (enabled) {
                assertEquals("2026-09-06T16:30:00Z", graph.snapshots.single().createdAt)
                assertEquals(CanonicalValidationResult.Valid,
                    AnalysisSnapshotV1Validator.validate(graph, graph.snapshots.single()))
            }
            val durable = databaseSnapshot()
            assertEquals(installed, runtime.queryObservationBinding(bindingId))
            main.idle()
            await(operation.released)
            assertEquals(HeartRateBindingDisposition.KnownAbsent, runtime.queryObservationBinding(bindingId))
            assertEquals(durable, databaseSnapshot())
        }
    }

    @Test
    fun activityFailurePreventsQueuedTerminal() = runRecorderTest {
        val cut = SqlCut("activity")
        reset(cut)
        val repository = WorkoutSessionRepository(database)
        val connection = connect()
        bind(recording(), repository = repository)
        connection.notify(88)
        connection.notify(88)
        await(recorder.freezeStart())
        assertGraph(prefixGraph())
        val confirmed = recorder.progress.value.confirmedState
        val before = databaseSnapshot()
        trigger("s06b_activity_original_failure")
        val input = RecorderTerminalInput(anchor + 2, RecorderTerminalKind.COMPLETED,
            "2026-09-06T16:30:00.002Z", 0, 0, 0, VALID_DISPLAY_METADATA,
            emptyList(), emptyList(), emptyList(), "2026-09-06T16:30:00.002Z")
        val pause = recorder.offer(pause(1)) as RecorderSubmission.Accepted
        lateinit var submission: RecorderTerminalSubmission.Accepted
        try {
            assertTrue(cut.entered.await(5, TimeUnit.SECONDS))
            submission = recorder.freezeTerminal(input) as RecorderTerminalSubmission.Accepted
        } finally {
            cut.release.countDown()
        }
        val cause = failure(pause.completion)
        assertTrue(cause is SQLiteConstraintException)
        assertTrue(cause.message.orEmpty().contains("s06b_activity_original_failure"))
        assertSame(cause, failure(submission.operation.saved))
        assertSame(cause, failure(submission.operation.released))
        assertSame(cause, recorder.progress.value.originalCause)
        assertEquals(confirmed, recorder.progress.value.confirmedState)
        joinWorker()
        assertEquals(0, writes.count { it == "terminal" })
        assertEquals(before, databaseSnapshot())
        dropTrigger()
        assertSame(submission, recorder.freezeTerminal(input))
        assertSame(cause, failure(submission.operation.saved))
        assertSame(cause, failure(submission.operation.released))
        assertEquals(before, databaseSnapshot())
        assertEquals(0, writes.count { it == "terminal" })
    }

    @Test
    fun terminalReadbackFailureRollsBackWithoutRetry() = runRecorderTest {
        val repository = WorkoutSessionRepository(database)
        val connection = connect()
        bind(recording(), repository = repository)
        connection.notify(88)
        connection.notify(88)
        await(recorder.freezeStart())
        assertGraph(prefixGraph())
        val bindingId = (runtime.queryObservationBinding(HeartRateObservationBindingId())
            as HeartRateBindingDisposition.ConflictingInstalled).observedBindingId
        val installed = runtime.queryObservationBinding(bindingId)
        val steps = listOf(SessionStepRecordEntity(
            id = "execution-step", sessionId = SESSION_ID, stepId = "block:warmup:target",
            kind = "timed_work", blockId = "block", itemId = null, setPlanId = null,
            exerciseId = null, startedAt = "2026-09-06T16:30:00Z",
            endedAt = "2026-09-06T16:30:01Z", skipped = false,
            actualDurationSec = 1, plannedDurationSec = 10))
        val input = RecorderTerminalInput(anchor + 1000, RecorderTerminalKind.COMPLETED,
            "2026-09-06T16:30:01Z", 1, 1, 0, VALID_DISPLAY_METADATA,
            steps, emptyList(), emptyList(), "2026-09-06T16:30:01Z")
        val before = databaseSnapshot()
        val bindingReached = AtomicBoolean(false)
        val fired = AtomicBoolean(false)
        terminalReadbackMutation = { sql ->
            if (sql.contains("SET original_analysis_version = 1")) bindingReached.set(true)
            if (bindingReached.get() && sql.startsWith("SELECT * FROM session_step_records") &&
                fired.compareAndSet(false, true)) {
                terminalReadbackMutation = null
                database.openHelper.writableDatabase.execSQL(
                    "UPDATE session_step_records SET actual_duration_sec=99 WHERE id='execution-step'")
            }
        }
        at(1000)
        val submission = recorder.freezeTerminal(input) as RecorderTerminalSubmission.Accepted
        val cause = failure(submission.operation.saved)
        assertTrue(fired.get())
        assertTrue(cause is RecorderValidationException)
        assertEquals("terminal_graph_changed_during_write", (cause as RecorderValidationException).code)
        assertSame(cause, failure(submission.operation.released))
        joinWorker()
        assertEquals(before, databaseSnapshot())
        assertEquals(installed, runtime.queryObservationBinding(bindingId))
        assertSame(submission, recorder.freezeTerminal(input))
        assertSame(cause, failure(submission.operation.saved))
        assertSame(cause, failure(submission.operation.released))
        assertEquals(before, databaseSnapshot())
        assertEquals(1, writes.count { it == "terminal" })
    }

    @Test
    fun clearKeepsOnlyAlreadyInFlightTerminal() = runRecorderTest {
        val queuedCut = SqlCut("activity")
        reset(queuedCut)
        val repository = WorkoutSessionRepository(database)
        val connection = connect()
        bind(recording(), repository = repository)
        connection.notify(88)
        connection.notify(88)
        await(recorder.freezeStart())
        assertGraph(prefixGraph())
        val input = RecorderTerminalInput(anchor + 2, RecorderTerminalKind.COMPLETED,
            "2026-09-06T16:30:00.002Z", 0, 0, 0, VALID_DISPLAY_METADATA,
            emptyList(), emptyList(), emptyList(), "2026-09-06T16:30:00.002Z")
        val pause = recorder.offer(pause(1)) as RecorderSubmission.Accepted
        lateinit var queued: RecorderTerminalSubmission.Accepted
        try {
            assertTrue(queuedCut.entered.await(5, TimeUnit.SECONDS))
            queued = recorder.freezeTerminal(input) as RecorderTerminalSubmission.Accepted
            recorder.clear()
            assertTrue(recorder.offer(RecorderActivityInput(anchor + 2)) is RecorderSubmission.Closed)
            recorder.clear()
        } finally {
            queuedCut.release.countDown()
        }
        await(pause.completion)
        assertTrue(failure(queued.operation.saved) is CancellationException)
        assertTrue(failure(queued.operation.released) is CancellationException)
        joinWorker()
        val prefix = prefixGraph()
        val paused = prefix.copy(
            session = session().copy(status = "paused", lastDurableOffsetMs = 1, lastMutationSequence = 4),
            phases = listOf(phase().copy(endOffsetMs = 1, endMutationSequence = 4, openMarker = null),
                phase(1, 1, 4, "paused", VALID_PAUSED_PHASE_IDENTITY)),
            acquisitions = prefix.acquisitions.dropLast(1) + listOf(
                prefix.acquisitions.last().copy(endOffsetMs = 1, endMutationSequence = 4, openMarker = null),
                acquisition(2, 1, 4, "live", enabled = false)))
        assertGraph(paused)
        assertEquals(0, writes.count { it == "terminal" })

        for (rollback in listOf(false, true)) {
            val cut = SqlCut("terminal")
            reset(cut)
            val repository = WorkoutSessionRepository(database)
            val connection = connect()
            bind(recording(), repository = repository)
            connection.notify(88)
            connection.notify(88)
            await(recorder.freezeStart())
            assertGraph(prefixGraph())
            val bindingId = (runtime.queryObservationBinding(HeartRateObservationBindingId())
                as HeartRateBindingDisposition.ConflictingInstalled).observedBindingId
            val installed = runtime.queryObservationBinding(bindingId)
            val steps = listOf(SessionStepRecordEntity(
                id = "execution-step", sessionId = SESSION_ID, stepId = "block:warmup:target",
                kind = "timed_work", blockId = "block", itemId = null, setPlanId = null,
                exerciseId = null, startedAt = "2026-09-06T16:30:00Z",
                endedAt = "2026-09-06T16:30:01Z", skipped = false,
                actualDurationSec = 1, plannedDurationSec = 10))
            val input = RecorderTerminalInput(anchor + 1000, RecorderTerminalKind.COMPLETED,
                "2026-09-06T16:30:01Z", 1, 1, 0, VALID_DISPLAY_METADATA,
                steps, emptyList(), emptyList(), "2026-09-06T16:30:01Z")
            val before = databaseSnapshot()
            if (rollback) database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_terminal_clear BEFORE INSERT ON heart_rate_analysis_snapshots BEGIN SELECT RAISE(ABORT,'s06b_terminal_clear_rollback'); END")
            at(1000)
            val submission = recorder.freezeTerminal(input) as RecorderTerminalSubmission.Accepted
            try {
                assertTrue(cut.entered.await(5, TimeUnit.SECONDS))
                recorder.clear()
                assertTrue(recorder.offer(RecorderActivityInput(anchor + 1000)) is RecorderSubmission.Closed)
                recorder.clear()
            } finally {
                cut.release.countDown()
            }
            if (rollback) {
                val cause = failure(submission.operation.saved)
                assertTrue(cause is SQLiteConstraintException)
                assertTrue(cause.message.orEmpty().contains("s06b_terminal_clear_rollback"))
                assertSame(cause, failure(submission.operation.released))
                joinWorker()
                assertEquals(before, databaseSnapshot())
            } else {
                val saved = await(submission.operation.saved)
                assertEquals(CanonicalFinalizationResult(SESSION_ID, RECORDING_ID, CanonicalTuple(1000, 4), 1), saved)
                val main = awaitCleanupDispatch(submission.operation)
                val rows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows(SESSION_ID))
                val recorded = rows.recordings.single()
                val graph = CanonicalSessionGraphV1(rows.session, rows.phases, recorded.recording,
                    recorded.acquisitions, recorded.samples, recorded.snapshots)
                val prefix = prefixGraph()
                val expected = prefix.copy(
                    session = session().copy(status = "completed", endedAt = "2026-09-06T16:30:01Z",
                        totalElapsedSec = 1, effectiveElapsedSec = 1, pausedElapsedSec = 0,
                        lastDurableOffsetMs = 1000, lastMutationSequence = 4,
                        trustedEndOffsetMs = 1000, terminalReason = "completed"),
                    phases = listOf(phase().copy(endOffsetMs = 1000, endMutationSequence = 4, openMarker = null)),
                    recording = recording().copy(status = "terminal", endedOffsetMs = 1000,
                        endedMutationSequence = 4, originalAnalysisVersion = 1),
                    acquisitions = prefix.acquisitions.dropLast(1) + prefix.acquisitions.last().copy(
                        endOffsetMs = 1000, endMutationSequence = 4, openMarker = null),
                    samples = prefix.samples,
                    snapshots = graph.snapshots)
                assertEquals(expected, graph)
                assertEquals(2, graph.samples.size)
                assertEquals("2026-09-06T16:30:01Z", graph.snapshots.single().createdAt)
                assertEquals(CanonicalValidationResult.Valid,
                    AnalysisSnapshotV1Validator.validate(graph, graph.snapshots.single()))
                assertEquals(steps, database.workoutSessionDao().stepRecordsForSession(SESSION_ID))
                assertEquals(emptyList<Any>(), database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID))
                assertEquals(emptyList<Any>(), database.workoutSessionDao().strengthSetRecordsForSession(SESSION_ID))
                val durable = databaseSnapshot()
                assertEquals(installed, runtime.queryObservationBinding(bindingId))
                main.idle()
                await(submission.operation.released)
                assertEquals(HeartRateBindingDisposition.KnownAbsent, runtime.queryObservationBinding(bindingId))
                assertEquals(durable, databaseSnapshot())
            }
            assertEquals(1, writes.count { it == "terminal" })
        }
    }

    @Test
    fun cancelledScopeKeepsTerminalAtomicWithoutNewWork() = runRecorderTest {
        for (inFlight in listOf(false, true)) {
            val cut = SqlCut(if (inFlight) "terminal" else "activity")
            reset(cut)
            val repository = WorkoutSessionRepository(database)
            val connection = connect()
            bind(recording(), repository = repository)
            connection.notify(88)
            connection.notify(88)
            await(recorder.freezeStart())
            assertGraph(prefixGraph())
            val bindingId = (runtime.queryObservationBinding(HeartRateObservationBindingId())
                as HeartRateBindingDisposition.ConflictingInstalled).observedBindingId
            val installed = runtime.queryObservationBinding(bindingId)
            val steps = listOf(SessionStepRecordEntity(
                id = "execution-step", sessionId = SESSION_ID, stepId = "block:warmup:target",
                kind = "timed_work", blockId = "block", itemId = null, setPlanId = null,
                exerciseId = null, startedAt = "2026-09-06T16:30:00Z",
                endedAt = "2026-09-06T16:30:01Z", skipped = false,
                actualDurationSec = 1, plannedDurationSec = 10))
            val input = RecorderTerminalInput(anchor + 1000, RecorderTerminalKind.COMPLETED,
                "2026-09-06T16:30:01Z", 1, 1, 0, VALID_DISPLAY_METADATA,
                steps, emptyList(), emptyList(), "2026-09-06T16:30:01Z")
            val before = databaseSnapshot()
            val cancellation = CancellationException("s06b_scope_cancelled")
            val pause = if (inFlight) null else recorder.offer(pause(1)) as RecorderSubmission.Accepted
            lateinit var submission: RecorderTerminalSubmission.Accepted
            if (inFlight) {
                at(1000)
                submission = recorder.freezeTerminal(input) as RecorderTerminalSubmission.Accepted
            }
            try {
                assertTrue(cut.entered.await(5, TimeUnit.SECONDS))
                if (!inFlight) submission = recorder.freezeTerminal(RecorderTerminalInput(anchor + 2,
                    RecorderTerminalKind.COMPLETED, "2026-09-06T16:30:00.002Z", 0, 0, 0,
                    VALID_DISPLAY_METADATA, emptyList(), emptyList(), emptyList(),
                    "2026-09-06T16:30:00.002Z")) as RecorderTerminalSubmission.Accepted
                scope.cancel(cancellation)
            } finally {
                cut.release.countDown()
            }
            val savedFailure = failure(submission.operation.saved)
            val releasedFailure = failure(submission.operation.released)
            assertTrue(savedFailure is CancellationException)
            assertTrue(releasedFailure is CancellationException)
            assertTrue(generateSequence(savedFailure) { it.cause }.any { it === cancellation })
            assertTrue(generateSequence(releasedFailure) { it.cause }.any { it === cancellation })
            if (pause != null) assertTrue(failure(pause.completion) is CancellationException)
            joinWorker()
            assertEquals(if (inFlight) 1 else 0, writes.count { it == "terminal" })
            if (databaseSnapshot() != before) {
                if (inFlight) {
                    val rows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows(SESSION_ID))
                    val recorded = rows.recordings.single()
                    val graph = CanonicalSessionGraphV1(rows.session, rows.phases, recorded.recording,
                        recorded.acquisitions, recorded.samples, recorded.snapshots)
                    val prefix = prefixGraph()
                    val expected = prefix.copy(
                        session = session().copy(status = "completed", endedAt = "2026-09-06T16:30:01Z",
                            totalElapsedSec = 1, effectiveElapsedSec = 1, pausedElapsedSec = 0,
                            lastDurableOffsetMs = 1000, lastMutationSequence = 4,
                            trustedEndOffsetMs = 1000, terminalReason = "completed"),
                        phases = listOf(phase().copy(endOffsetMs = 1000, endMutationSequence = 4, openMarker = null)),
                        recording = recording().copy(status = "terminal", endedOffsetMs = 1000,
                            endedMutationSequence = 4, originalAnalysisVersion = 1),
                        acquisitions = prefix.acquisitions.dropLast(1) + prefix.acquisitions.last().copy(
                            endOffsetMs = 1000, endMutationSequence = 4, openMarker = null),
                        samples = prefix.samples,
                        snapshots = graph.snapshots)
                    assertEquals(expected, graph)
                    assertEquals(2, graph.samples.size)
                    assertEquals("2026-09-06T16:30:01Z", graph.snapshots.single().createdAt)
                    assertEquals(CanonicalValidationResult.Valid,
                        AnalysisSnapshotV1Validator.validate(graph, graph.snapshots.single()))
                    assertEquals(steps, database.workoutSessionDao().stepRecordsForSession(SESSION_ID))
                    assertEquals(emptyList<Any>(), database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID))
                    assertEquals(emptyList<Any>(), database.workoutSessionDao().strengthSetRecordsForSession(SESSION_ID))
                } else {
                    val prefix = prefixGraph()
                    val paused = prefix.copy(
                        session = session().copy(status = "paused", lastDurableOffsetMs = 1, lastMutationSequence = 4),
                        phases = listOf(phase().copy(endOffsetMs = 1, endMutationSequence = 4, openMarker = null),
                            phase(1, 1, 4, "paused", VALID_PAUSED_PHASE_IDENTITY)),
                        acquisitions = prefix.acquisitions.dropLast(1) + listOf(
                            prefix.acquisitions.last().copy(endOffsetMs = 1, endMutationSequence = 4, openMarker = null),
                            acquisition(2, 1, 4, "live", enabled = false)))
                    assertGraph(paused)
                }
            }
            val durable = databaseSnapshot()
            assertEquals(installed, runtime.queryObservationBinding(bindingId))
            assertSame(submission, recorder.freezeTerminal(input))
            assertTrue(recorder.offer(RecorderActivityInput(anchor + 1100)) is RecorderSubmission.Closed)
            assertEquals(durable, databaseSnapshot())
            assertEquals(if (inFlight) 1 else 0, writes.count { it == "terminal" })
        }
    }

    @Test
    fun savedCleanupFailureKeepsOriginalAndBlocksAdmission() = runRecorderTest {
        for (cancelCleanup in listOf(false, true)) {
            val cut = SqlCut("terminal")
            reset(cut)
            val repository = WorkoutSessionRepository(database)
            val connection = connect()
            bind(recording(), repository = repository)
            connection.notify(88)
            connection.notify(88)
            await(recorder.freezeStart())
            assertGraph(prefixGraph())
            val steps = listOf(SessionStepRecordEntity(
                id = "execution-step", sessionId = SESSION_ID, stepId = "block:warmup:target",
                kind = "timed_work", blockId = "block", itemId = null, setPlanId = null,
                exerciseId = null, startedAt = "2026-09-06T16:30:00Z",
                endedAt = "2026-09-06T16:30:01Z", skipped = false,
                actualDurationSec = 1, plannedDurationSec = 10))
            val input = RecorderTerminalInput(anchor + 1000, RecorderTerminalKind.COMPLETED,
                "2026-09-06T16:30:01Z", 1, 1, 0, VALID_DISPLAY_METADATA,
                steps, emptyList(), emptyList(), "2026-09-06T16:30:01Z")
            val foreignId = HeartRateObservationBindingId()
            lateinit var observedId: HeartRateObservationBindingId
            lateinit var installed: HeartRateBindingDisposition
            at(1000)
            val submission = recorder.freezeTerminal(input) as RecorderTerminalSubmission.Accepted
            try {
                assertTrue(cut.entered.await(5, TimeUnit.SECONDS))
                val originalId = (runtime.queryObservationBinding(HeartRateObservationBindingId())
                    as HeartRateBindingDisposition.ConflictingInstalled).observedBindingId
                if (cancelCleanup) {
                    observedId = originalId
                    installed = runtime.queryObservationBinding(originalId)
                } else {
                    runtime.unbindObservations(originalId)
                    observedId = foreignId
                    installed = runtime.bindObservations(foreignId) {}
                }
            } finally {
                cut.release.countDown()
            }
            val saved = await(submission.operation.saved)
            assertEquals(CanonicalFinalizationResult(SESSION_ID, RECORDING_ID, CanonicalTuple(1000, 4), 1), saved)
            val main = awaitCleanupDispatch(submission.operation)
            val rows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows(SESSION_ID))
            val recorded = rows.recordings.single()
            val graph = CanonicalSessionGraphV1(rows.session, rows.phases, recorded.recording,
                recorded.acquisitions, recorded.samples, recorded.snapshots)
            val prefix = prefixGraph()
            val expected = prefix.copy(
                session = session().copy(status = "completed", endedAt = "2026-09-06T16:30:01Z",
                    totalElapsedSec = 1, effectiveElapsedSec = 1, pausedElapsedSec = 0,
                    lastDurableOffsetMs = 1000, lastMutationSequence = 4,
                    trustedEndOffsetMs = 1000, terminalReason = "completed"),
                phases = listOf(phase().copy(endOffsetMs = 1000, endMutationSequence = 4, openMarker = null)),
                recording = recording().copy(status = "terminal", endedOffsetMs = 1000,
                    endedMutationSequence = 4, originalAnalysisVersion = 1),
                acquisitions = prefix.acquisitions.dropLast(1) + prefix.acquisitions.last().copy(
                    endOffsetMs = 1000, endMutationSequence = 4, openMarker = null),
                samples = prefix.samples,
                snapshots = graph.snapshots)
            assertEquals(expected, graph)
            assertEquals(2, graph.samples.size)
            assertEquals("2026-09-06T16:30:01Z", graph.snapshots.single().createdAt)
            assertEquals(CanonicalValidationResult.Valid,
                AnalysisSnapshotV1Validator.validate(graph, graph.snapshots.single()))
            assertEquals(steps, database.workoutSessionDao().stepRecordsForSession(SESSION_ID))
            assertEquals(emptyList<Any>(), database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID))
            assertEquals(emptyList<Any>(), database.workoutSessionDao().strengthSetRecordsForSession(SESSION_ID))
            val durable = databaseSnapshot()
            assertEquals(installed, runtime.queryObservationBinding(observedId))
            val cancellation = CancellationException("s06b_scope_cancelled")
            if (cancelCleanup) scope.cancel(cancellation)
            main.idle()
            val cause = failure(submission.operation.released)
            if (cancelCleanup) {
                assertTrue(cause is CancellationException)
                assertTrue(generateSequence(cause) { it.cause }.any { it === cancellation })
            } else {
                assertTrue(cause is RecorderBindingConflictException)
                assertEquals("recorder_binding_conflict", cause.message)
            }
            joinWorker()
            assertSame(saved, await(submission.operation.saved))
            assertNull(recorder.progress.value.originalCause)
            assertEquals(installed, runtime.queryObservationBinding(observedId))
            val busy = runCatching { repository.admitRecorder("s06b-next-entry",
                session().copy(id = "s06b-next-session"), phase().copy(id = "s06b-next-session:phase:0",
                    sessionId = "s06b-next-session")) }.exceptionOrNull()
            assertTrue(busy is RecorderOwnerBusyException)
            assertEquals(RecorderOwnerDisposition.OWNER_BLOCKED, (busy as RecorderOwnerBusyException).disposition)
            assertSame(submission, recorder.freezeTerminal(input))
            assertSame(saved, await(submission.operation.saved))
            val repeatedCause = failure(submission.operation.released)
            assertEquals(cause.javaClass, repeatedCause.javaClass)
            assertEquals(cause.message, repeatedCause.message)
            assertSame(
                generateSequence(cause) { it.cause }.last(),
                generateSequence(repeatedCause) { it.cause }.last()
            )
            assertEquals(durable, databaseSnapshot())
            assertEquals(installed, runtime.queryObservationBinding(observedId))
            assertEquals(1, writes.count { it == "terminal" })
        }
    }

    @Test
    fun releasedOldRecorderCannotChangeNextOwner() = runRecorderTest {
        val repository = WorkoutSessionRepository(database)
        val connection = connect()
        bind(recording(), repository = repository)
        connection.notify(88)
        connection.notify(88)
        await(recorder.freezeStart())
        assertGraph(prefixGraph())
        val bindingId = (runtime.queryObservationBinding(HeartRateObservationBindingId())
            as HeartRateBindingDisposition.ConflictingInstalled).observedBindingId
        val installed = runtime.queryObservationBinding(bindingId)
        val steps = listOf(SessionStepRecordEntity(
            id = "execution-step", sessionId = SESSION_ID, stepId = "block:warmup:target",
            kind = "timed_work", blockId = "block", itemId = null, setPlanId = null,
            exerciseId = null, startedAt = "2026-09-06T16:30:00Z",
            endedAt = "2026-09-06T16:30:01Z", skipped = false,
            actualDurationSec = 1, plannedDurationSec = 10))
        val input = RecorderTerminalInput(anchor + 1000, RecorderTerminalKind.COMPLETED,
            "2026-09-06T16:30:01Z", 1, 1, 0, VALID_DISPLAY_METADATA,
            steps, emptyList(), emptyList(), "2026-09-06T16:30:01Z")
        at(1000)
        val submission = recorder.freezeTerminal(input) as RecorderTerminalSubmission.Accepted
        val saved = await(submission.operation.saved)
        assertEquals(CanonicalFinalizationResult(SESSION_ID, RECORDING_ID, CanonicalTuple(1000, 4), 1), saved)
        val main = awaitCleanupDispatch(submission.operation)
        val rows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows(SESSION_ID))
        val recorded = rows.recordings.single()
        val graph = CanonicalSessionGraphV1(rows.session, rows.phases, recorded.recording,
            recorded.acquisitions, recorded.samples, recorded.snapshots)
        val prefix = prefixGraph()
        val expected = prefix.copy(
            session = session().copy(status = "completed", endedAt = "2026-09-06T16:30:01Z",
                totalElapsedSec = 1, effectiveElapsedSec = 1, pausedElapsedSec = 0,
                lastDurableOffsetMs = 1000, lastMutationSequence = 4,
                trustedEndOffsetMs = 1000, terminalReason = "completed"),
            phases = listOf(phase().copy(endOffsetMs = 1000, endMutationSequence = 4, openMarker = null)),
            recording = recording().copy(status = "terminal", endedOffsetMs = 1000,
                endedMutationSequence = 4, originalAnalysisVersion = 1),
            acquisitions = prefix.acquisitions.dropLast(1) + prefix.acquisitions.last().copy(
                endOffsetMs = 1000, endMutationSequence = 4, openMarker = null),
            samples = prefix.samples,
            snapshots = graph.snapshots)
        assertEquals(expected, graph)
        assertEquals(2, graph.samples.size)
        assertEquals("2026-09-06T16:30:01Z", graph.snapshots.single().createdAt)
        assertEquals(CanonicalValidationResult.Valid,
            AnalysisSnapshotV1Validator.validate(graph, graph.snapshots.single()))
        assertEquals(steps, database.workoutSessionDao().stepRecordsForSession(SESSION_ID))
        assertEquals(emptyList<Any>(), database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID))
        assertEquals(emptyList<Any>(), database.workoutSessionDao().strengthSetRecordsForSession(SESSION_ID))
        val savedGraph = databaseSnapshot()
        assertEquals(installed, runtime.queryObservationBinding(bindingId))
        main.idle()
        await(submission.operation.released)
        assertEquals(HeartRateBindingDisposition.KnownAbsent, runtime.queryObservationBinding(bindingId))
        assertEquals(savedGraph, databaseSnapshot())
        val nextScope = CoroutineScope(currentCoroutineContext() + Job(currentCoroutineContext()[Job]))
        scopes += nextScope
        val nextSession = session().copy(id = "s06b-next-session")
        val nextRecorder = withContext(nextScope.coroutineContext) {
            WorkoutSessionTimelineRecorder.admitAndBind(repository, runtime, nextScope, "s06b-next-entry",
                nextSession, RecorderPhaseInput("timed_work", VALID_PHASE_IDENTITY), null)
        }
        val nextConfirmed = await(nextRecorder.freezeStart())
        assertEquals(RecorderExpectedState("s06b-next-session", "active", CanonicalTuple(0, 0),
            "s06b-next-session:phase:0"), nextConfirmed)
        val nextRows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows("s06b-next-session"))
        assertEquals(nextSession, nextRows.session)
        assertEquals(listOf(phase().copy(id = "s06b-next-session:phase:0", sessionId = "s06b-next-session")), nextRows.phases)
        assertTrue(nextRows.recordings.isEmpty())
        assertEquals(emptyList<Any>(), database.workoutSessionDao().stepRecordsForSession("s06b-next-session"))
        assertEquals(emptyList<Any>(), database.workoutSessionDao().restExtensionRecordsForSession("s06b-next-session"))
        assertEquals(emptyList<Any>(), database.workoutSessionDao().strengthSetRecordsForSession("s06b-next-session"))
        val nextId = (runtime.queryObservationBinding(HeartRateObservationBindingId())
            as HeartRateBindingDisposition.ConflictingInstalled).observedBindingId
        val nextBinding = runtime.queryObservationBinding(nextId)
        assertTrue(nextBinding is HeartRateBindingDisposition.MatchingInstalled)
        val durable = databaseSnapshot()
        val priorWrites = writes.toList()
        assertSame(submission, recorder.freezeTerminal(input))
        assertSame(saved, await(submission.operation.saved))
        recorder.clear()
        assertTrue(recorder.offer(RecorderActivityInput(anchor + 1100)) is RecorderSubmission.Closed)
        assertEquals(nextBinding, runtime.queryObservationBinding(nextId))
        assertEquals(durable, databaseSnapshot())
        assertEquals(priorWrites, writes.toList())
    }

    private fun runRecorderTest(block: suspend CoroutineScope.() -> Unit) = runBlocking {
        try {
            block()
        } finally {
            scopes.forEach { it.cancel() }
            withTimeout(5000) { scopes.forEach { requireNotNull(it.coroutineContext[Job]).join() } }
        }
    }

    private suspend fun bind(
        recording: HeartRateRecordingEntity?,
        session: WorkoutSessionEntity = session(),
        phase: RecorderPhaseInput = RecorderPhaseInput("timed_work", VALID_PHASE_IDENTITY),
        repository: WorkoutSessionRepository = WorkoutSessionRepository(database)
    ) {
        scope = CoroutineScope(currentCoroutineContext() + Job(currentCoroutineContext()[Job]))
        scopes += scope
        recorder = withContext(scope.coroutineContext) {
            WorkoutSessionTimelineRecorder.admitAndBind(repository, runtime, scope,
                "s06a-entry", session, phase, recording)
        }
        anchor = SystemClock.elapsedRealtime()
    }

    private suspend fun reset(cut: SqlCut? = null) {
        if (::scope.isInitialized) {
            scope.cancel()
            withTimeout(5000) { requireNotNull(scope.coroutineContext[Job]).join() }
        }
        database.close()
        writes.clear()
        database = newDatabase(cut)
        runtime = HeartRateRuntimeOwner(application)
    }

    private fun newDatabase(cut: SqlCut? = null): TrainFlowDatabase =
        Room.inMemoryDatabaseBuilder(application, TrainFlowDatabase::class.java).allowMainThreadQueries()
            .setQueryCallback(RoomDatabase.QueryCallback { sql, _ ->
                val stage = when {
                    sql.contains("INSERT OR IGNORE INTO `workout_sessions`") -> "start"
                    sql.contains("UPDATE workout_sessions") && sql.contains("SET last_durable_offset_ms") -> "activity"
                    sql.lowercase().contains("update workout_sessions") && sql.lowercase().contains("set ended_at =") -> "terminal"
                    else -> null
                }
                if (stage != null) {
                    writes += stage
                    if (cut != null && stage == cut.stage && cut.armed.compareAndSet(true, false)) {
                        cut.entered.countDown()
                        check(cut.release.await(5, TimeUnit.SECONDS))
                    }
                }
                terminalReadbackMutation?.invoke(sql)
            }, Executor { it.run() }).build()

    private class SqlCut(val stage: String) {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val armed = AtomicBoolean(true)
    }

    private suspend fun joinWorker() = withTimeout(5000) {
        requireNotNull(scope.coroutineContext[Job]).children.toList().forEach { it.join() }
    }

    private suspend fun <T> await(result: Deferred<T>): T = withTimeout(5000) { result.await() }

    private fun awaitCleanupDispatch(operation: RecorderTerminalOperation): ShadowPausedLooper {
        assertSame(Looper.getMainLooper().thread, Thread.currentThread())
        val before = SystemClock.elapsedRealtime()
        assertFalse(operation.released.isCompleted)
        val main = shadowOf(Looper.getMainLooper()) as ShadowPausedLooper
        main.poll(5000L)
        assertFalse(main.isIdle)
        assertFalse(operation.released.isCompleted)
        assertEquals(before, SystemClock.elapsedRealtime())
        return main
    }

    private suspend fun failure(result: Deferred<*>): Throwable {
        val cause = runCatching { await(result) }.exceptionOrNull()
        return requireNotNull(cause) { "Expected original failure" }
    }

    private suspend fun submit(input: RecorderActivityInput) =
        await((recorder.offer(input) as RecorderSubmission.Accepted).completion)

    private fun at(offset: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(anchor + offset - SystemClock.elapsedRealtime()))
    }

    private fun now() = SystemClock.elapsedRealtime()

    private fun pause(offset: Long) = RecorderActivityInput(anchor + offset,
        RecorderPhaseInput("paused", VALID_PAUSED_PHASE_IDENTITY), "paused", RecorderIntentInput.SetEnabled(false))

    private fun resume(offset: Long) = RecorderActivityInput(anchor + offset,
        RecorderPhaseInput("timed_work", VALID_PHASE_IDENTITY), "active", RecorderIntentInput.SetEnabled(true))

    private fun trigger(message: String) {
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER s06a_failure BEFORE INSERT ON heart_rate_acquisition_intervals BEGIN SELECT RAISE(ABORT, '$message'); END")
    }

    private fun dropTrigger() = database.openHelper.writableDatabase.execSQL("DROP TRIGGER s06a_failure")

    private fun connect(): Connection {
        runtime.submit(HeartRateRuntimeAction.Enable)
        runtime.submit(HeartRateRuntimeAction.StartScan)
        shadowOf(Looper.getMainLooper()).idle()
        val device = ShadowBluetoothDevice.newInstance("AA:BB:CC:DD:EE:71")
        shadowOf(device).setName("Observation fixture")
        val scanner = application.getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
        shadowOf(scanner).scanCallbacks.single().onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            ScanResult(device, null, -40, 1L))
        lateinit var result: Connection
        shadowOf(device).setGattConnectionInterceptor { gatt ->
            val shadow = Shadow.extract<ShadowBluetoothGatt>(gatt)
            val service = BluetoothGattService(UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"), 0)
            val characteristic = BluetoothGattCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"), 16, 1)
            characteristic.addDescriptor(BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), 16))
            service.addCharacteristic(characteristic)
            shadow.addDiscoverableService(service)
            shadow.allowCharacteristicNotification(characteristic)
            result = Connection(gatt, shadow, characteristic)
            shadow.gattCallback.onConnectionStateChange(gatt, 0, BluetoothProfile.STATE_CONNECTED)
        }
        runtime.submit(HeartRateRuntimeAction.Connect(device.address))
        shadowOf(Looper.getMainLooper()).idle()
        return result
    }

    private class Connection(val gatt: BluetoothGatt, val shadow: ShadowBluetoothGatt, val characteristic: BluetoothGattCharacteristic) {
        fun notify(bpm: Int) {
            shadow.gattCallback.onCharacteristicChanged(gatt, characteristic, byteArrayOf(0, bpm.toByte()))
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun session() = WorkoutSessionEntity(SESSION_ID, mode = "timed", status = "active",
        planSnapshotJson = VALID_PLAN_SNAPSHOT, startedAt = "2026-09-06T16:30:00Z", timelineVersion = 1,
        lastDurableOffsetMs = 0, lastMutationSequence = 0, displayMetadataContractVersion = 1,
        sessionDisplayMetadataJson = VALID_DISPLAY_METADATA, startLocalDate = "2026-09-07",
        startZoneId = "Asia/Shanghai", startUtcOffsetSeconds = 28800, timeMetadataSourceContractVersion = 1)

    private fun recording() = HeartRateRecordingEntity(RECORDING_ID, SESSION_ID, "active", 0, 0,
        null, null, 1, "ble_hrs", 1, 1, alertThresholdBpm = 150)

    private fun phase(sequence: Int = 0, offset: Long = 0, mutation: Long = 0,
        kind: String = "timed_work", identity: String = VALID_PHASE_IDENTITY) =
        WorkoutPhaseIntervalEntity("$SESSION_ID:phase:$sequence", SESSION_ID, sequence, offset, null,
            mutation, null, 1, kind, identity)

    private fun acquisition(sequence: Int, offset: Long, mutation: Long, device: String,
        reason: String? = null, enabled: Boolean = true) =
        HeartRateAcquisitionIntervalEntity("$RECORDING_ID:acquisition:$sequence", RECORDING_ID, sequence,
            offset, null, mutation, null, 1, if (enabled) "expected_recording" else "user_excluded",
            if (enabled) null else "user_turned_off", device, reason)

    private fun zeroGraph(enabled: Boolean = true) = CanonicalSessionGraphV1(session(), listOf(phase()),
        if (enabled) recording() else null,
        if (enabled) listOf(acquisition(0, 0, 0, "not_observing")) else emptyList())

    private fun prefixGraph() = CanonicalSessionGraphV1(session().copy(lastMutationSequence = 3),
        listOf(phase()), recording(),
        listOf(acquisition(0, 0, 0, "waiting_first_sample", "initial_acquisition")
            .copy(endOffsetMs = 0, endMutationSequence = 1, openMarker = null), acquisition(1, 0, 1, "live")),
        listOf(HeartRateSampleEntity(RECORDING_ID, 0, 0, 2, 88), HeartRateSampleEntity(RECORDING_ID, 1, 0, 3, 88)))

    private fun pausedGraph() = CanonicalSessionGraphV1(
        session().copy(status = "paused", lastDurableOffsetMs = 1, lastMutationSequence = 1),
        listOf(phase().copy(endOffsetMs = 1, endMutationSequence = 1, openMarker = null),
            phase(1, 1, 1, "paused", VALID_PAUSED_PHASE_IDENTITY)), recording(),
        listOf(acquisition(0, 0, 0, "not_observing").copy(endOffsetMs = 1, endMutationSequence = 1, openMarker = null),
            acquisition(1, 1, 1, "not_observing", enabled = false)))

    private suspend fun assertGraph(expected: CanonicalSessionGraphV1, extensions: List<TimedRestExtensionRecordEntity> = emptyList()) {
        val rows = requireNotNull(database.canonicalTimelineHeartRateDao().canonicalGraphRows(SESSION_ID))
        assertTrue(rows.recordings.size <= 1)
        val recording = rows.recordings.singleOrNull()
        assertEquals(expected, CanonicalSessionGraphV1(rows.session, rows.phases, recording?.recording,
            recording?.acquisitions.orEmpty(), recording?.samples.orEmpty(), recording?.snapshots.orEmpty()))
        assertEquals(extensions, database.workoutSessionDao().restExtensionRecordsForSession(SESSION_ID))
        assertTrue(databaseSnapshot().none { it.startsWith("session_step_records|") || it.startsWith("strength_set_records|") })
    }

    private fun databaseSnapshot(): List<String> {
        val sql = database.openHelper.writableDatabase
        return listOf("workout_sessions" to "id", "workout_phase_intervals" to "id, sequence",
            "heart_rate_recordings" to "recording_id", "heart_rate_acquisition_intervals" to "id, sequence",
            "heart_rate_samples" to "recording_id, sample_sequence", "heart_rate_analysis_snapshots" to "recording_id, analysis_version",
            "session_step_records" to "session_id, id", "timed_rest_extension_records" to "session_id, id",
            "strength_set_records" to "session_id, id").flatMap { (table, orderBy) ->
            sql.query("SELECT * FROM $table ORDER BY $orderBy").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(buildString {
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

    private companion object {
        const val SESSION_ID = "canonical-session"
        const val RECORDING_ID = "canonical-session:recording"
        const val VALID_DISPLAY_METADATA = "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
        const val DISPLAY_METADATA_WITH_ENTRY = "{\"displayMetadataContractVersion\":1,\"entries\":[{\"entityKind\":\"exercise\",\"stableId\":\"exercise-1\",\"displayNameAtFirstReference\":\"深蹲\",\"customNameAtFirstReference\":null,\"resolutionSource\":\"plan_snapshot\"}]}"
        const val VALID_PLAN_SNAPSHOT = "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Timed\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"block\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[]}],\"preferences\":null,\"followAlong\":null}"
        const val VALID_PHASE_IDENTITY = "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"timed_work\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855\"},\"payload\":{\"variant\":\"warmup\",\"compositionVersion\":2,\"compositionBlockId\":\"block\",\"timelineStageId\":\"block:warmup\",\"timelineStageKind\":\"warmup\",\"stageGroupId\":\"block:warmup\",\"targetId\":\"block:warmup:target\",\"targetKind\":\"warmup\",\"roundIndex0\":null,\"stageGroupIndex0\":null,\"targetIndex0\":0,\"stageInstanceIndex0\":0,\"targetInstanceIndex0\":0,\"stepIndex0\":0}}"
        const val VALID_PAUSED_PHASE_IDENTITY = "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"paused\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855\"},\"payload\":{\"variant\":\"paused\",\"compositionVersion\":2,\"compositionBlockId\":null,\"timelineStageId\":null,\"timelineStageKind\":null,\"stageGroupId\":null,\"targetId\":null,\"targetKind\":null,\"roundIndex0\":null,\"stageGroupIndex0\":null,\"targetIndex0\":null,\"stageInstanceIndex0\":null,\"targetInstanceIndex0\":null,\"stepIndex0\":null}}"
    }
}
