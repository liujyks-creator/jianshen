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
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.TimedRestExtensionRecordEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.health.E17GattShadow
import com.liujyks.trainflow.core.health.E17ScannerShadow
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
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
        phase: RecorderPhaseInput = RecorderPhaseInput("timed_work", VALID_PHASE_IDENTITY)
    ) {
        scope = CoroutineScope(currentCoroutineContext() + Job(currentCoroutineContext()[Job]))
        scopes += scope
        recorder = withContext(scope.coroutineContext) {
            WorkoutSessionTimelineRecorder.admitAndBind(WorkoutSessionRepository(database), runtime, scope,
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
                    else -> null
                }
                if (stage != null) {
                    writes += stage
                    if (cut != null && stage == cut.stage && cut.armed.compareAndSet(true, false)) {
                        cut.entered.countDown()
                        check(cut.release.await(5, TimeUnit.SECONDS))
                    }
                }
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
