package com.liujyks.trainflow.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutSessionRecorderReconciliationTest {
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
    fun legacyActiveAndPausedRelaunchReturnsTypedResidualsWithoutAnyDatabaseMutation() = runBlocking {
        insertSession(legacySession("legacy-active", "active"))
        insertSession(legacySession("legacy-paused", "paused"))
        val before = databaseSnapshot()

        val result = WorkoutSessionRepository(database).prepareRecorder()

        assertTrue(result is RecorderReconciliationResult.Succeeded)
        result as RecorderReconciliationResult.Succeeded
        assertEquals(
            listOf(
                LegacySessionResidual(
                    sessionId = "legacy-active",
                    status = "active",
                    timelineStatus = "legacy_noncanonical_nonterminal"
                ),
                LegacySessionResidual(
                    sessionId = "legacy-paused",
                    status = "paused",
                    timelineStatus = "legacy_noncanonical_nonterminal"
                )
            ),
            result.legacyResiduals
        )
        assertTrue(result.reconciledSessions.isEmpty())
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun mixedScanClassifiesEverythingBeforeFailingInvalidPartialWithoutReconcilingCanonicalRows() = runBlocking {
        insertSession(legacySession("legacy-active", "active"))
        insertCanonicalRunningSession("canonical-running")
        insertSession(
            legacySession("invalid-partial", "active").copy(timelineVersion = 1)
        )
        val before = databaseSnapshot()

        val result = WorkoutSessionRepository(database).prepareRecorder()

        assertTrue(result is RecorderReconciliationResult.ManualResolutionRequired)
        result as RecorderReconciliationResult.ManualResolutionRequired
        assertEquals(listOf("legacy-active"), result.legacyResiduals.map { it.sessionId })
        assertEquals(1, result.failures.size)
        assertEquals("invalid-partial", result.failures.single().sessionId)
        assertEquals(
            RecorderFailureKind.INVALID_PARTIAL_CANONICAL_HEADER,
            result.failures.single().kind
        )
        assertEquals("invalid_partial_canonical_header", result.failures.single().code)
        assertEquals(false, result.failures.single().retryable)
        assertEquals(true, result.failures.single().manualResolutionRequired)
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun unknownVersionAndCorruptJsonAreTypedNonretryableManualResolutionFailures() = runBlocking {
        insertSession(
            canonicalHeader("unknown-version").copy(timelineVersion = 2)
        )
        insertSession(
            canonicalHeader("corrupt-json").copy(sessionDisplayMetadataJson = "{}")
        )
        val before = databaseSnapshot()

        val result = WorkoutSessionRepository(database).prepareRecorder()

        assertTrue(result is RecorderReconciliationResult.ManualResolutionRequired)
        result as RecorderReconciliationResult.ManualResolutionRequired
        assertEquals(
            listOf(RecorderFailureKind.CORRUPT_JSON, RecorderFailureKind.UNKNOWN_VERSION),
            result.failures.sortedBy { it.sessionId }.map { it.kind }
        )
        assertTrue(result.failures.all { failure ->
            !failure.retryable && failure.manualResolutionRequired
        })
        assertEquals(before, databaseSnapshot())
    }

    @Test
    fun canonicalNoHeartRateRelaunchUsesExactCasTupleThenBecomesIdempotent() = runBlocking {
        insertCanonicalRunningSession("canonical-running")
        val repository = WorkoutSessionRepository(database)

        val first = repository.prepareRecorder()
        val afterFirst = databaseSnapshot()
        val second = repository.prepareRecorder()

        assertSame(first, second)
        assertTrue(first is RecorderReconciliationResult.Succeeded)
        first as RecorderReconciliationResult.Succeeded
        assertEquals(
            listOf(
                ReconciledCanonicalSession(
                    sessionId = "canonical-running",
                    expectedTuple = CanonicalTuple(offsetMs = 100, mutationSequence = 4),
                    reconciledTuple = CanonicalTuple(offsetMs = 100, mutationSequence = 5),
                    reconciliationContractVersion = 1
                )
            ),
            first.reconciledSessions
        )
        val session = requireNotNull(
            database.canonicalTimelineHeartRateDao().sessionById("canonical-running")
        )
        assertEquals("abandoned", session.status)
        assertEquals(100L, session.lastDurableOffsetMs)
        assertEquals(5L, session.lastMutationSequence)
        assertEquals(100L, session.trustedEndOffsetMs)
        assertEquals("process_interrupted", session.terminalReason)
        val phase = database.canonicalTimelineHeartRateDao()
            .phaseIntervals("canonical-running")
            .single()
        assertEquals(100L, phase.endOffsetMs)
        assertEquals(5L, phase.endMutationSequence)
        assertEquals(null, phase.openMarker)
        assertEquals(afterFirst, databaseSnapshot())

        val losingCasRowCount = database.workoutSessionDao().reconcileProcessInterrupted(
            sessionId = "canonical-running",
            expectedStatus = "active",
            expectedOffsetMs = 100,
            expectedMutationSequence = 4,
            reconciledMutationSequence = 5,
            reconciliationContractVersion = 1
        )
        assertEquals(0, losingCasRowCount)
        assertEquals(afterFirst, databaseSnapshot())
    }

    @Test
    fun canonicalActiveRecordingRelaunchReturnsUncachedFinalizerPendingWithoutAnyMutation() = runBlocking {
        insertCanonicalRunningSessionWithActiveRecording("canonical-with-recording")
        val before = databaseSnapshot()
        val repository = WorkoutSessionRepository(database)

        val result = repository.prepareRecorder()

        assertTrue(result !is RecorderReconciliationResult.Succeeded)
        assertTrue(result !is RecorderReconciliationResult.ManualResolutionRequired)
        assertTrue(result is RecorderReconciliationResult.FinalizerPrerequisitePending)
        result as RecorderReconciliationResult.FinalizerPrerequisitePending
        assertEquals("FINALIZER_PREREQUISITE_PENDING", result.code)
        assertTrue(result.legacyResiduals.isEmpty())
        assertTrue(result.reconciledSessions.isEmpty())
        assertEquals(
            listOf(
                FinalizerPrerequisiteCandidate(
                    sessionId = "canonical-with-recording",
                    expectedStatus = "active",
                    expectedTuple = CanonicalTuple(offsetMs = 100, mutationSequence = 4),
                    recordingId = "canonical-with-recording:recording",
                    reconciliationContractVersion = 1
                )
            ),
            result.candidates
        )
        assertEquals(before, databaseSnapshot())

        val protectedWrite = runCatching {
            repository.appendSessionDisplayMetadata(
                expected = RecorderExpectedState(
                    sessionId = "canonical-with-recording",
                    status = "active",
                    durableTuple = CanonicalTuple(offsetMs = 100, mutationSequence = 4),
                    openPhaseId = "canonical-with-recording:phase:0",
                    recordingId = "canonical-with-recording:recording",
                    openAcquisitionId = "canonical-with-recording:acquisition"
                ),
                nextTuple = CanonicalTuple(offsetMs = 100, mutationSequence = 5),
                nextJson = VALID_DISPLAY_METADATA
            )
        }
        val blocked = protectedWrite.exceptionOrNull()
        assertTrue(blocked is RecorderGateBlockedException)
        blocked as RecorderGateBlockedException
        assertTrue(blocked.result is RecorderReconciliationResult.FinalizerPrerequisitePending)
        assertEquals(before, databaseSnapshot())

        val retried = repository.prepareRecorder()
        assertTrue(retried is RecorderReconciliationResult.FinalizerPrerequisitePending)
        assertTrue(result !== retried)
        assertEquals(before, databaseSnapshot())

        // CS-04B restores the final Succeeded obligation after the CS-05 finalizer exists.
    }

    @Test
    fun concurrentPendingAccessSharesOneFlightButDoesNotCachePendingAfterCompletion() = runBlocking {
        insertCanonicalRunningSessionWithActiveRecording("canonical-with-recording")
        val repository = WorkoutSessionRepository(database)

        val concurrent = coroutineScope {
            listOf(
                async(Dispatchers.IO) { repository.prepareRecorder() },
                async(Dispatchers.IO) { repository.prepareRecorder() }
            ).map { deferred -> deferred.await() }
        }

        assertSame(concurrent[0], concurrent[1])
        assertTrue(concurrent[0] is RecorderReconciliationResult.FinalizerPrerequisitePending)
        val retried = repository.prepareRecorder()
        assertTrue(retried is RecorderReconciliationResult.FinalizerPrerequisitePending)
        assertTrue(concurrent[0] !== retried)
    }

    @Test
    fun concurrentFirstProtectedAccessSharesOneCompletedGateResult() = runBlocking {
        insertSession(legacySession("legacy-active", "active"))
        val repository = WorkoutSessionRepository(database)

        val results = coroutineScope {
            listOf(
                async(Dispatchers.IO) { repository.prepareRecorder() },
                async(Dispatchers.IO) { repository.prepareRecorder() }
            ).map { deferred -> deferred.await() }
        }
        insertSession(legacySession("legacy-after-gate", "paused"))
        val cached = repository.prepareRecorder()

        assertSame(results[0], results[1])
        assertSame(results[0], cached)
        assertTrue(cached is RecorderReconciliationResult.Succeeded)
        cached as RecorderReconciliationResult.Succeeded
        assertEquals(listOf("legacy-active"), cached.legacyResiduals.map { it.sessionId })
    }

    private suspend fun insertCanonicalRunningSession(sessionId: String) {
        insertSession(canonicalHeader(sessionId))
        database.canonicalTimelineHeartRateDao().insertPhaseInterval(
            WorkoutPhaseIntervalEntity(
                id = "$sessionId:phase:0",
                sessionId = sessionId,
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
    }

    private suspend fun insertCanonicalRunningSessionWithActiveRecording(sessionId: String) {
        insertCanonicalRunningSession(sessionId)
        database.canonicalTimelineHeartRateDao().insertRecording(
            HeartRateRecordingEntity(
                recordingId = "$sessionId:recording",
                sessionId = sessionId,
                status = "active",
                startedOffsetMs = 0,
                startedMutationSequence = 0,
                endedOffsetMs = null,
                endedMutationSequence = null,
                sourceContractVersion = 1,
                sourceKind = "ble_hrs",
                acquisitionContractVersion = 1,
                parameterSnapshotVersion = 1,
                originalAnalysisVersion = null
            )
        )
        database.canonicalTimelineHeartRateDao().insertAcquisitionInterval(
            HeartRateAcquisitionIntervalEntity(
                id = "$sessionId:acquisition",
                recordingId = "$sessionId:recording",
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
    }

    private fun canonicalHeader(sessionId: String) = WorkoutSessionEntity(
        id = sessionId,
        mode = "timed",
        status = "active",
        planSnapshotJson = VALID_PLAN_SNAPSHOT,
        timelineVersion = 1,
        lastDurableOffsetMs = 100,
        lastMutationSequence = 4,
        displayMetadataContractVersion = 1,
        sessionDisplayMetadataJson = VALID_DISPLAY_METADATA
    )

    private suspend fun insertSession(session: WorkoutSessionEntity) {
        assertTrue(database.workoutSessionDao().insertSession(session) != -1L)
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
            "workout_phase_intervals" to "id",
            "heart_rate_recordings" to "recording_id",
            "heart_rate_acquisition_intervals" to "id",
            "heart_rate_samples" to "recording_id, sample_sequence",
            "heart_rate_analysis_snapshots" to "recording_id, analysis_version"
        ).flatMap { (table, orderBy) ->
            sql.query("SELECT * FROM $table ORDER BY $orderBy").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            buildString {
                                append(table)
                                repeat(cursor.columnCount) { index ->
                                    append('|')
                                    if (cursor.isNull(index)) append("<NULL>") else append(cursor.getString(index))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val VALID_DISPLAY_METADATA =
            "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
        const val VALID_PLAN_SNAPSHOT =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Timed\",\"mode\":\"timed\",\"blocks\":[{\"id\":\"block\",\"kind\":\"timed_composition\",\"order\":0,\"compositionVersion\":2,\"warmupSec\":10,\"cooldownSec\":0,\"rounds\":1,\"restBetweenRoundsSec\":0,\"stageGroups\":[]}],\"preferences\":null,\"followAlong\":null}"
        val VALID_PHASE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"timed_composition_v2\",\"payloadVersion\":2,\"mode\":\"timed\",\"phaseKind\":\"timed_work\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855\"},\"payload\":{\"variant\":\"warmup\",\"compositionVersion\":2,\"compositionBlockId\":\"block\",\"${"timelineStage" + "Id"}\":\"block:warmup\",\"timelineStageKind\":\"warmup\",\"stageGroupId\":\"block:warmup\",\"targetId\":\"block:warmup:target\",\"targetKind\":\"warmup\",\"roundIndex0\":null,\"stageGroupIndex0\":null,\"targetIndex0\":0,\"stageInstanceIndex0\":0,\"${"targetInstance" + "Index0"}\":0,\"stepIndex0\":0}}"
    }
}
