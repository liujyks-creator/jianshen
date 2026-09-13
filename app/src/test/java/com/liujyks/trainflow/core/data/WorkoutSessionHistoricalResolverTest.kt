package com.liujyks.trainflow.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPlanEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.database.renderCanonicalJson
import com.liujyks.trainflow.feature.workoutsession.TimedFocusIdentityV1
import com.liujyks.trainflow.feature.workoutsession.TimedFocusValidationV1
import com.liujyks.trainflow.feature.workoutsession.TimedResolvedFocusPhaseV1
import com.liujyks.trainflow.feature.workoutsession.TimedResolvedStructureV1
import com.liujyks.trainflow.feature.workoutsession.restoreTimedFocusV1
import com.liujyks.trainflow.feature.workoutsession.validateTimedFocusV1
import java.util.Collections
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutSessionHistoricalResolverTest {
    private lateinit var database: TrainFlowDatabase
    private val queries = Collections.synchronizedList(mutableListOf<String>())

    @After
    fun closeDatabase() {
        if (::database.isInitialized) database.close()
    }

    private fun freshDatabase(observeQueries: Boolean = false) {
        if (::database.isInitialized) database.close()
        queries.clear()
        val builder = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), TrainFlowDatabase::class.java
        ).allowMainThreadQueries()
        if (observeQueries) {
            builder.setQueryCallback(RoomDatabase.QueryCallback { sql, _ ->
                queries.add(sql)
            }, Executor { command -> command.run() })
        }
        database = builder.build()
    }

    private suspend fun seedFixture(fixture: Fixture) {
        database.workoutSessionDao().insertSession(fixture.header)
        for (phase in fixture.phases) database.canonicalTimelineHeartRateDao().insertPhaseInterval(phase)
    }

    private fun assertResolved(
        fixture: Fixture,
        input: WorkoutSessionStrictReadResult,
        result: WorkoutSessionHistoricalResult,
        locale: String = "zh-CN"
    ): WorkoutSessionHistoricalResult.Resolved {
        assertTrue(input.toString(), input is WorkoutSessionStrictReadResult.CanonicalTerminal)
        assertTrue(result.toString(), result is WorkoutSessionHistoricalResult.Resolved)
        result as WorkoutSessionHistoricalResult.Resolved
        assertSame(input, result.source)
        assertEquals(fixture.title, result.title)
        assertEquals(fixture.mode, result.mode)
        assertEquals(locale, result.displayLocale)
        assertEquals(fixture.displays.size, result.phaseDisplays.size)
        fixture.displays.forEachIndexed { index, expected ->
            val actual = result.phaseDisplays[index]
            assertEquals(expected.sequence, actual.sequence)
            assertEquals(expected.identity, actual.phaseIdentity.renderCanonicalJson())
            assertEquals(expected.plannedMs, actual.timedPlannedDurationMs)
            assertEquals(1, actual.display.displayContractVersion)
            assertEquals(locale, actual.display.locale)
            assertEquals(expected.status, actual.display.resolutionStatus)
            assertEquals(expected.label, actual.display.label)
        }
        assertEquals(fixture.structures, result.timedStructures)
        return result
    }

    @Test
    fun resolvesFrozenPhasesForFourFamilies() = runBlocking {
        for (key in listOf("L", "C", "S", "F")) {
            freshDatabase()
            val fixture = fixtures.getValue(key)
            seedFixture(fixture)
            val input = WorkoutSessionRepository(database).readSessionStrict(fixture.header.id)
            assertResolved(fixture, input, resolveWorkoutSessionHistorical(input, "zh-CN"))
        }
    }

    @Test
    fun derivesPlannedDurationsForSharedTimedPredicate() = runBlocking {
        for (key in listOf("L0", "C0")) {
            freshDatabase()
            val fixture = fixtures.getValue(key)
            seedFixture(fixture)
            val input = WorkoutSessionRepository(database).readSessionStrict(fixture.header.id)
            assertResolved(fixture, input, resolveWorkoutSessionHistorical(input, "zh-CN"))
        }
        freshDatabase()
        seedFixture(fixtures.getValue("O"))
        val input = WorkoutSessionRepository(database).readSessionStrict("s-O")
        assertTrue(input.toString(), input is WorkoutSessionStrictReadResult.CanonicalTerminal)
        assertEquals(
            WorkoutSessionHistoricalResult.InvalidPlannedDuration("s-O", "legacy_timed_v1", 0, 9223372036854776L),
            resolveWorkoutSessionHistorical(input, "zh-CN")
        )
    }

    @Test
    fun preservesBlockLocalFocusWithoutSamples() = runBlocking {
        for (key in listOf("L", "C", "M")) {
            freshDatabase()
            val fixture = fixtures.getValue(key)
            seedFixture(fixture)
            val input = WorkoutSessionRepository(database).readSessionStrict(fixture.header.id)
            val result = assertResolved(fixture, input, resolveWorkoutSessionHistorical(input, "zh-CN"))
            if (key != "M") {
                val structure = result.timedStructures.single()
                val focuses = focusCases.getValue(key)
                for ((json, expected) in focuses) {
                    assertEquals(TimedFocusValidationV1.Valid(expected), validateTimedFocusV1(structure, json))
                }
                val invalid = invalidFocuses.getValue(key)
                assertEquals(TimedFocusValidationV1.Invalid, validateTimedFocusV1(structure, invalid))
                assertEquals(focuses.first().second, restoreTimedFocusV1(structure, invalid))
            }
        }
    }

    @Test
    fun distinguishesMissingAndEmptyFrozenNames() = runBlocking {
        for (fixture in nameCases) {
            freshDatabase()
            seedFixture(fixture)
            val input = WorkoutSessionRepository(database).readSessionStrict(fixture.header.id)
            assertResolved(fixture, input, resolveWorkoutSessionHistorical(input, "zh-CN"))
        }
    }

    @Test
    fun ignoresLaterCurrentNames() = runBlocking {
        freshDatabase(observeQueries = true)
        val fixture = fixtures.getValue("S")
        seedFixture(fixture)
        val plan = WorkoutPlanEntity(
            id = "plan-old", mode = "strength", title = "Current before", description = null,
            blocksJson = STRENGTH_BLOCKS,
            reminderJson = null, preferencesJson = null, followAlongJson = null,
            createdAt = "2026-09-13T00:00:00Z", updatedAt = "2026-09-13T00:00:00Z"
        )
        database.workoutPlanDao().upsertPlan(plan)
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO exercises(id,name,category,equipment_json,difficulty,capabilities_json,content_status,updated_at) VALUES(?,?,?,?,?,?,?,?)",
            arrayOf("ex-sa", "Library before", "strength", "[]", "beginner", "{}", "draft", "2026-09-13T00:00:00Z")
        )
        val repository = WorkoutSessionRepository(database)
        val firstInput = repository.readSessionStrict("s-S")
        assertResolved(fixture, firstInput, resolveWorkoutSessionHistorical(firstInput, "zh-CN"))
        database.workoutPlanDao().upsertPlan(plan.copy(title = "Current after", updatedAt = "2026-09-13T00:01:00Z"))
        database.openHelper.writableDatabase.execSQL(
            "UPDATE exercises SET name=?,updated_at=? WHERE id=?",
            arrayOf("Library after", "2026-09-13T00:01:00Z", "ex-sa")
        )
        assertEquals("Current after", database.workoutPlanDao().getPlan("plan-old")!!.title)
        database.openHelper.readableDatabase.query("SELECT name FROM exercises WHERE id=?", arrayOf("ex-sa")).use {
            assertTrue(it.moveToFirst())
            assertEquals("Library after", it.getString(0))
        }
        queries.clear()
        assertResolved(fixture, firstInput, resolveWorkoutSessionHistorical(firstInput, "en-US"), "en-US")
        assertTrue(queries.toString(), queries.isEmpty())
        queries.clear()
        val reread = repository.readSessionStrict("s-S")
        assertResolved(fixture, reread, resolveWorkoutSessionHistorical(reread, "en-US"), "en-US")
        synchronized(queries) {
            assertTrue(queries.toString(), queries.none {
                Regex("\\b(workout_plans|exercises)\\b", RegexOption.IGNORE_CASE).containsMatchIn(it)
            })
            assertTrue(queries.toString(), queries.none {
                Regex("^\\s*(INSERT|UPDATE|DELETE|REPLACE)\\b", RegexOption.IGNORE_CASE).containsMatchIn(it)
            })
        }
    }

    @Test
    fun leavesLegacyAndReadFailuresUnpromoted() = runBlocking {
        for ((header, mode) in legacyHeaders.zip(listOf("timed", "strength", "follow_along"))) {
            freshDatabase()
            database.workoutSessionDao().insertSession(header)
            val input = WorkoutSessionRepository(database).readSessionStrict(header.id)
            assertTrue(input.toString(), input is WorkoutSessionStrictReadResult.LegacyTerminal)
            val result = resolveWorkoutSessionHistorical(input, "zh-CN")
            assertTrue(result.toString(), result is WorkoutSessionHistoricalResult.Resolved)
            result as WorkoutSessionHistoricalResult.Resolved
            assertSame(input, result.source)
            assertEquals("Legacy old", result.title)
            assertEquals(mode, result.mode)
            assertEquals("zh-CN", result.displayLocale)
            assertEquals(emptyList<HistoricalPhaseDisplay>(), result.phaseDisplays)
            assertEquals(emptyList<TimedResolvedStructureV1>(), result.timedStructures)
        }
        freshDatabase()
        database.workoutSessionDao().insertSession(legacyHeaders.first().copy(status = "active"))
        val active = WorkoutSessionRepository(database).readSessionStrict("legacy-t")
        assertTrue(active.toString(), active is WorkoutSessionStrictReadResult.Nonterminal)
        assertEquals("legacy_noncanonical_nonterminal", (active as WorkoutSessionStrictReadResult.Nonterminal).timelineStatus)
        val forwarded = resolveWorkoutSessionHistorical(active, "zh-CN")
        assertTrue(forwarded is WorkoutSessionHistoricalResult.Forwarded)
        assertSame(active, (forwarded as WorkoutSessionHistoricalResult.Forwarded).source)
        freshDatabase()
        val absent = WorkoutSessionRepository(database).readSessionStrict("absent")
        assertSame(WorkoutSessionStrictReadResult.NotFound, absent)
        val notFound = resolveWorkoutSessionHistorical(absent, "zh-CN")
        assertTrue(notFound is WorkoutSessionHistoricalResult.Forwarded)
        assertSame(absent, (notFound as WorkoutSessionHistoricalResult.Forwarded).source)
    }

    @Test
    fun rejectsCorruptRowsBeforeDisplayResolution() = runBlocking {
        val base = fixtures.getValue("L")
        val corruptions = listOf(
            base.copy(header = base.header.copy(sessionDisplayMetadataJson = "{")) to "invalid_session_header",
            base.copy(phases = base.phases.mapIndexed { index, phase ->
                if (index == 0) phase.copy(phaseIdentityJson = phase.phaseIdentityJson.replace("\"payloadVersion\":1", "\"payloadVersion\":99"))
                else phase
            }) to "invalid_session_graph"
        )
        for ((fixture, code) in corruptions) {
            freshDatabase()
            seedFixture(fixture)
            val input = WorkoutSessionRepository(database).readSessionStrict("s-L")
            assertEquals(WorkoutSessionStrictReadResult.Unavailable(code), input)
            val result = resolveWorkoutSessionHistorical(input, "zh-CN")
            assertTrue(result is WorkoutSessionHistoricalResult.Forwarded)
            assertSame(input, (result as WorkoutSessionHistoricalResult.Forwarded).source)
        }
    }

    private data class ExpectedDisplay(
        val sequence: Int,
        val identity: String,
        val plannedMs: Long?,
        val status: String,
        val label: String?
    )

    private data class Fixture(
        val header: WorkoutSessionEntity,
        val phases: List<WorkoutPhaseIntervalEntity>,
        val title: String,
        val mode: String,
        val displays: List<ExpectedDisplay>,
        val structures: List<TimedResolvedStructureV1>
    )

    // Fixed inputs and independent expected values transcribed from the approved appendices.
    private val fixtures = mapOf(
        "L" to Fixture(
            header = WorkoutSessionEntity(
                id = "s-L", planId = "plan-old", mode = "timed", status = "completed",
                planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History L","mode":"timed","blocks":[{"id":"w","kind":"warmup","title":"Warm old","order":0,"durationSec":2,"items":[]},{"id":"t","kind":"stretch","order":1,"durationSec":3,"items":[]},{"id":"d","kind":"cooldown","title":"Cool old","order":2,"durationSec":4,"items":[]},{"id":"b","kind":"warmup","title":"Boundary old","order":3,"items":[{"id":"bw","exerciseId":"ex-b","labelOverride":"Boundary item","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":6,"restAfterSec":2,"autoAdvance":false},{"id":"br","labelOverride":"Boundary rest","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"a","kind":"timed_circuit","title":"Circuit A","order":4,"rounds":2,"restBetweenRoundsSec":5,"items":[{"id":"aw","exerciseId":"ex-a","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":10,"restAfterSec":4,"autoAdvance":false},{"id":"ar","labelOverride":"Item rest A","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"z","kind":"timed_circuit","title":"Circuit Z","order":5,"rounds":1,"restBetweenRoundsSec":0,"items":[{"id":"zw","exerciseId":"ex-z","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":8,"restAfterSec":2,"autoAdvance":false}]},{"id":"r","kind":"rest","title":"Rest title","order":6,"durationSec":7,"label":"Rest label"}],"preferences":null,"followAlong":null}""",
                startedAt = "2026-09-13T00:00:00Z", endedAt = "2026-09-13T00:00:14Z",
                totalElapsedSec = 14, effectiveElapsedSec = 12, pausedElapsedSec = 2,
                timelineVersion = 1, lastDurableOffsetMs = 14000L, lastMutationSequence = 14L,
                trustedEndOffsetMs = 14000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-b","displayNameAtFirstReference":"Metadata B","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-a","displayNameAtFirstReference":"Base A","customNameAtFirstReference":"Custom A","resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-z","displayNameAtFirstReference":"Exercise Z","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"}]}""",
                startLocalDate = "2026-09-13", startZoneId = "UTC", startUtcOffsetSeconds = 0L,
                timeMetadataSourceContractVersion = 1L
            ),
            phases = listOf(
                WorkoutPhaseIntervalEntity("s-L:p:0", "s-L", 0, 0L, 1000L, 0L, 1L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_block_work","blockId":"w","stepIndex0":0,"legacyBlockKind":"warmup","legacyStageType":"warmup","itemId":null,"exerciseId":null,"roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:1", "s-L", 1, 1000L, 2000L, 1L, 2L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_block_work","blockId":"t","stepIndex0":0,"legacyBlockKind":"stretch","legacyStageType":"cooldown","itemId":null,"exerciseId":null,"roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:2", "s-L", 2, 2000L, 3000L, 2L, 3L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_block_work","blockId":"d","stepIndex0":0,"legacyBlockKind":"cooldown","legacyStageType":"cooldown","itemId":null,"exerciseId":null,"roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:3", "s-L", 3, 3000L, 4000L, 3L, 4L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_item_work","blockId":"b","stepIndex0":0,"legacyBlockKind":"warmup","legacyStageType":"work","itemId":"bw","exerciseId":"ex-b","roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:4", "s-L", 4, 4000L, 5000L, 4L, 5L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_item_rest","blockId":"b","stepIndex0":2,"legacyBlockKind":"warmup","legacyStageType":"rest","itemId":"br","exerciseId":null,"roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:5", "s-L", 5, 5000L, 6000L, 5L, 6L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_rest_after_item","blockId":"b","stepIndex0":1,"legacyBlockKind":"warmup","legacyStageType":"rest","itemId":"bw","exerciseId":"ex-b","roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:6", "s-L", 6, 6000L, 7000L, 6L, 7L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"circuit_item_work","blockId":"a","stepIndex0":0,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"aw","exerciseId":"ex-a","roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:7", "s-L", 7, 7000L, 8000L, 7L, 8L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"circuit_item_rest","blockId":"a","stepIndex0":2,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":"ar","exerciseId":null,"roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:8", "s-L", 8, 8000L, 9000L, 8L, 9L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"circuit_rest_after_item","blockId":"a","stepIndex0":1,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":"aw","exerciseId":"ex-a","roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:9", "s-L", 9, 9000L, 10000L, 9L, 10L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"between_round_rest","blockId":"a","stepIndex0":3,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":null,"exerciseId":null,"roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:10", "s-L", 10, 10000L, 11000L, 10L, 11L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"standalone_rest","blockId":"r","stepIndex0":0,"legacyBlockKind":"rest","legacyStageType":"rest","itemId":null,"exerciseId":null,"roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:11", "s-L", 11, 11000L, 13000L, 11L, 12L, null, "paused", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"paused","blockId":null,"stepIndex0":null,"legacyBlockKind":null,"legacyStageType":null,"itemId":null,"exerciseId":null,"roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:12", "s-L", 12, 13000L, 13000L, 12L, 13L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"circuit_item_work","blockId":"z","stepIndex0":0,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"zw","exerciseId":"ex-z","roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-L:p:13", "s-L", 13, 13000L, 14000L, 13L, 14L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"circuit_rest_after_item","blockId":"z","stepIndex0":1,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":"zw","exerciseId":"ex-z","roundIndex0":0}}""")
            ),
            title = "History L", mode = "timed",
            displays = listOf(
                ExpectedDisplay(0, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_block_work","blockId":"w","stepIndex0":0,"legacyBlockKind":"warmup","legacyStageType":"warmup","itemId":null,"exerciseId":null,"roundIndex0":null}}""", 2000L, "resolved", "Warm old"),
                ExpectedDisplay(1, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_block_work","blockId":"t","stepIndex0":0,"legacyBlockKind":"stretch","legacyStageType":"cooldown","itemId":null,"exerciseId":null,"roundIndex0":null}}""", 3000L, "unresolved_missing_metadata", null),
                ExpectedDisplay(2, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_block_work","blockId":"d","stepIndex0":0,"legacyBlockKind":"cooldown","legacyStageType":"cooldown","itemId":null,"exerciseId":null,"roundIndex0":null}}""", 4000L, "resolved", "Cool old"),
                ExpectedDisplay(3, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_item_work","blockId":"b","stepIndex0":0,"legacyBlockKind":"warmup","legacyStageType":"work","itemId":"bw","exerciseId":"ex-b","roundIndex0":null}}""", 6000L, "resolved", "Boundary item"),
                ExpectedDisplay(4, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_item_rest","blockId":"b","stepIndex0":2,"legacyBlockKind":"warmup","legacyStageType":"rest","itemId":"br","exerciseId":null,"roundIndex0":null}}""", 3000L, "resolved", "Boundary rest"),
                ExpectedDisplay(5, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_rest_after_item","blockId":"b","stepIndex0":1,"legacyBlockKind":"warmup","legacyStageType":"rest","itemId":"bw","exerciseId":"ex-b","roundIndex0":null}}""", 2000L, "resolved", "Boundary item"),
                ExpectedDisplay(6, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"circuit_item_work","blockId":"a","stepIndex0":0,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"aw","exerciseId":"ex-a","roundIndex0":0}}""", 10000L, "resolved", "Custom A"),
                ExpectedDisplay(7, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"circuit_item_rest","blockId":"a","stepIndex0":2,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":"ar","exerciseId":null,"roundIndex0":0}}""", 3000L, "resolved", "Item rest A"),
                ExpectedDisplay(8, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"circuit_rest_after_item","blockId":"a","stepIndex0":1,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":"aw","exerciseId":"ex-a","roundIndex0":0}}""", 4000L, "resolved", "Custom A"),
                ExpectedDisplay(9, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"between_round_rest","blockId":"a","stepIndex0":3,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":null,"exerciseId":null,"roundIndex0":0}}""", 5000L, "unresolved_missing_metadata", null),
                ExpectedDisplay(10, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"standalone_rest","blockId":"r","stepIndex0":0,"legacyBlockKind":"rest","legacyStageType":"rest","itemId":null,"exerciseId":null,"roundIndex0":null}}""", 7000L, "resolved", "Rest label"),
                ExpectedDisplay(11, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"paused","blockId":null,"stepIndex0":null,"legacyBlockKind":null,"legacyStageType":null,"itemId":null,"exerciseId":null,"roundIndex0":null}}""", null, "unresolved_missing_metadata", null),
                ExpectedDisplay(12, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"circuit_item_work","blockId":"z","stepIndex0":0,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"zw","exerciseId":"ex-z","roundIndex0":0}}""", 8000L, "resolved", "Exercise Z"),
                ExpectedDisplay(13, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"circuit_rest_after_item","blockId":"z","stepIndex0":1,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":"zw","exerciseId":"ex-z","roundIndex0":0}}""", 2000L, "resolved", "Exercise Z")
            ),
            structures = listOf(
                TimedResolvedStructureV1("s-L", "legacy_timed_v1", "f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354", listOf(
                    TimedResolvedFocusPhaseV1(0L, "w", null, false, false),
                    TimedResolvedFocusPhaseV1(1L, "t", null, false, false),
                    TimedResolvedFocusPhaseV1(2L, "d", null, false, false),
                    TimedResolvedFocusPhaseV1(3L, "b", null, false, false),
                    TimedResolvedFocusPhaseV1(4L, "b", null, false, false),
                    TimedResolvedFocusPhaseV1(5L, "b", null, false, false),
                    TimedResolvedFocusPhaseV1(6L, "a", 0L, true, false),
                    TimedResolvedFocusPhaseV1(7L, "a", 0L, false, true),
                    TimedResolvedFocusPhaseV1(8L, "a", 0L, false, true),
                    TimedResolvedFocusPhaseV1(9L, "a", 0L, false, true),
                    TimedResolvedFocusPhaseV1(10L, "r", null, false, false),
                    TimedResolvedFocusPhaseV1(11L, null, null, false, false),
                    TimedResolvedFocusPhaseV1(12L, "z", 0L, true, false),
                    TimedResolvedFocusPhaseV1(13L, "z", 0L, false, true)
                ), true, true, true)
            )
        ),
        "C" to Fixture(
            header = WorkoutSessionEntity(
                id = "s-C", planId = "plan-old", mode = "timed", status = "completed",
                planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History C","mode":"timed","blocks":[{"id":"ca","kind":"timed_composition","title":"Composition A","order":0,"compositionVersion":2,"warmupSec":2,"cooldownSec":4,"rounds":2,"restBetweenRoundsSec":3,"stageGroups":[{"id":"glate","order":1,"name":"Group late","colorHex":"#112233","targets":[{"id":"late","order":0,"name":"Late old","kind":"action","durationSec":7,"colorHex":"#112233","autoAdvance":false}]},{"id":"ga","order":0,"name":"Group A","colorHex":"#112233","targets":[{"id":"rr","order":1,"name":"Rest target old","kind":"rest","durationSec":5,"colorHex":"#223344","autoAdvance":true},{"id":"aa","order":0,"name":"Action old","kind":"action","durationSec":11,"colorHex":"#112233","autoAdvance":false},{"id":"xx","order":2,"name":"Custom old","kind":"custom","durationSec":13,"colorHex":"#334455","autoAdvance":false}]}]},{"id":"cz","kind":"timed_composition","title":"Composition Z","order":1,"compositionVersion":2,"warmupSec":0,"cooldownSec":0,"rounds":1,"restBetweenRoundsSec":0,"stageGroups":[{"id":"gz","order":0,"name":"Group Z","colorHex":"#112233","targets":[{"id":"za","order":0,"name":"Z action old","kind":"action","durationSec":6,"colorHex":"#112233","autoAdvance":false},{"id":"zr","order":1,"name":"Z rest old","kind":"rest","durationSec":2,"colorHex":"#223344","autoAdvance":true}]}]}],"preferences":null,"followAlong":null}""",
                startedAt = "2026-09-13T00:00:00Z", endedAt = "2026-09-13T00:00:11Z",
                totalElapsedSec = 11, effectiveElapsedSec = 9, pausedElapsedSec = 2,
                timelineVersion = 1, lastDurableOffsetMs = 11000L, lastMutationSequence = 10L,
                trustedEndOffsetMs = 11000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[]}""",
                startLocalDate = "2026-09-13", startZoneId = "UTC", startUtcOffsetSeconds = 0L,
                timeMetadataSourceContractVersion = 1L
            ),
            phases = listOf(
                WorkoutPhaseIntervalEntity("s-C:p:0", "s-C", 0, 0L, 1000L, 0L, 1L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"warmup","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:warmup","timelineStageKind":"warmup","stageGroupId":"ca:warmup","targetId":"ca:warmup:target","targetKind":"warmup","roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":0,"targetInstanceIndex0":0,"stepIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-C:p:1", "s-C", 1, 1000L, 2000L, 1L, 2L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_action","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g1:ga","timelineStageKind":"stage_group","stageGroupId":"ga","targetId":"aa","targetKind":"action","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":1,"targetInstanceIndex0":1,"stepIndex0":1}}"""),
                WorkoutPhaseIntervalEntity("s-C:p:2", "s-C", 2, 2000L, 3000L, 2L, 3L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_custom","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g1:ga","timelineStageKind":"stage_group","stageGroupId":"ga","targetId":"xx","targetKind":"custom","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":2,"stageInstanceIndex0":1,"targetInstanceIndex0":3,"stepIndex0":3}}"""),
                WorkoutPhaseIntervalEntity("s-C:p:3", "s-C", 3, 3000L, 4000L, 3L, 4L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_rest","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g1:ga","timelineStageKind":"stage_group","stageGroupId":"ga","targetId":"rr","targetKind":"rest","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":1,"stageInstanceIndex0":1,"targetInstanceIndex0":2,"stepIndex0":2}}"""),
                WorkoutPhaseIntervalEntity("s-C:p:4", "s-C", 4, 4000L, 5000L, 4L, 5L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"between_round_rest","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:between-round-rest","timelineStageKind":"between_round_rest","stageGroupId":"ca:r1:between-round-rest","targetId":"ca:r1:between-round-rest:target","targetKind":"between_round_rest","roundIndex0":0,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":3,"targetInstanceIndex0":5,"stepIndex0":5}}"""),
                WorkoutPhaseIntervalEntity("s-C:p:5", "s-C", 5, 5000L, 6000L, 5L, 6L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"cooldown","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:cooldown","timelineStageKind":"cooldown","stageGroupId":"ca:cooldown","targetId":"ca:cooldown:target","targetKind":"cooldown","roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":6,"targetInstanceIndex0":10,"stepIndex0":10}}"""),
                WorkoutPhaseIntervalEntity("s-C:p:6", "s-C", 6, 6000L, 8000L, 6L, 7L, null, "paused", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"paused","compositionVersion":2,"compositionBlockId":null,"timelineStageId":null,"timelineStageKind":null,"stageGroupId":null,"targetId":null,"targetKind":null,"roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":null,"stageInstanceIndex0":null,"targetInstanceIndex0":null,"stepIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-C:p:7", "s-C", 7, 8000L, 9000L, 7L, 8L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_action","compositionVersion":2,"compositionBlockId":"cz","timelineStageId":"cz:r1:g1:gz","timelineStageKind":"stage_group","stageGroupId":"gz","targetId":"za","targetKind":"action","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":0,"targetInstanceIndex0":0,"stepIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-C:p:8", "s-C", 8, 9000L, 10000L, 8L, 9L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_rest","compositionVersion":2,"compositionBlockId":"cz","timelineStageId":"cz:r1:g1:gz","timelineStageKind":"stage_group","stageGroupId":"gz","targetId":"zr","targetKind":"rest","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":1,"stageInstanceIndex0":0,"targetInstanceIndex0":1,"stepIndex0":1}}"""),
                WorkoutPhaseIntervalEntity("s-C:p:9", "s-C", 9, 10000L, 11000L, 9L, 10L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_action","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g2:glate","timelineStageKind":"stage_group","stageGroupId":"glate","targetId":"late","targetKind":"action","roundIndex0":0,"stageGroupIndex0":1,"targetIndex0":0,"stageInstanceIndex0":2,"targetInstanceIndex0":4,"stepIndex0":4}}""")
            ),
            title = "History C", mode = "timed",
            displays = listOf(
                ExpectedDisplay(0, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"warmup","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:warmup","timelineStageKind":"warmup","stageGroupId":"ca:warmup","targetId":"ca:warmup:target","targetKind":"warmup","roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":0,"targetInstanceIndex0":0,"stepIndex0":0}}""", 2000L, "unresolved_missing_metadata", null),
                ExpectedDisplay(1, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_action","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g1:ga","timelineStageKind":"stage_group","stageGroupId":"ga","targetId":"aa","targetKind":"action","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":1,"targetInstanceIndex0":1,"stepIndex0":1}}""", 11000L, "resolved", "Action old"),
                ExpectedDisplay(2, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_custom","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g1:ga","timelineStageKind":"stage_group","stageGroupId":"ga","targetId":"xx","targetKind":"custom","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":2,"stageInstanceIndex0":1,"targetInstanceIndex0":3,"stepIndex0":3}}""", 13000L, "resolved", "Custom old"),
                ExpectedDisplay(3, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_rest","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g1:ga","timelineStageKind":"stage_group","stageGroupId":"ga","targetId":"rr","targetKind":"rest","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":1,"stageInstanceIndex0":1,"targetInstanceIndex0":2,"stepIndex0":2}}""", 5000L, "resolved", "Rest target old"),
                ExpectedDisplay(4, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"between_round_rest","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:between-round-rest","timelineStageKind":"between_round_rest","stageGroupId":"ca:r1:between-round-rest","targetId":"ca:r1:between-round-rest:target","targetKind":"between_round_rest","roundIndex0":0,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":3,"targetInstanceIndex0":5,"stepIndex0":5}}""", 3000L, "unresolved_missing_metadata", null),
                ExpectedDisplay(5, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"cooldown","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:cooldown","timelineStageKind":"cooldown","stageGroupId":"ca:cooldown","targetId":"ca:cooldown:target","targetKind":"cooldown","roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":6,"targetInstanceIndex0":10,"stepIndex0":10}}""", 4000L, "unresolved_missing_metadata", null),
                ExpectedDisplay(6, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"paused","compositionVersion":2,"compositionBlockId":null,"timelineStageId":null,"timelineStageKind":null,"stageGroupId":null,"targetId":null,"targetKind":null,"roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":null,"stageInstanceIndex0":null,"targetInstanceIndex0":null,"stepIndex0":null}}""", null, "unresolved_missing_metadata", null),
                ExpectedDisplay(7, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_action","compositionVersion":2,"compositionBlockId":"cz","timelineStageId":"cz:r1:g1:gz","timelineStageKind":"stage_group","stageGroupId":"gz","targetId":"za","targetKind":"action","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":0,"targetInstanceIndex0":0,"stepIndex0":0}}""", 6000L, "resolved", "Z action old"),
                ExpectedDisplay(8, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_rest","compositionVersion":2,"compositionBlockId":"cz","timelineStageId":"cz:r1:g1:gz","timelineStageKind":"stage_group","stageGroupId":"gz","targetId":"zr","targetKind":"rest","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":1,"stageInstanceIndex0":0,"targetInstanceIndex0":1,"stepIndex0":1}}""", 2000L, "resolved", "Z rest old"),
                ExpectedDisplay(9, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c"},"payload":{"variant":"stage_group_action","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g2:glate","timelineStageKind":"stage_group","stageGroupId":"glate","targetId":"late","targetKind":"action","roundIndex0":0,"stageGroupIndex0":1,"targetIndex0":0,"stageInstanceIndex0":2,"targetInstanceIndex0":4,"stepIndex0":4}}""", 7000L, "resolved", "Late old")
            ),
            structures = listOf(
                TimedResolvedStructureV1("s-C", "timed_composition_v2", "9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c", listOf(
                    TimedResolvedFocusPhaseV1(0L, "ca", null, false, false),
                    TimedResolvedFocusPhaseV1(1L, "ca", 0L, true, false),
                    TimedResolvedFocusPhaseV1(2L, "ca", 0L, true, false),
                    TimedResolvedFocusPhaseV1(3L, "ca", 0L, false, true),
                    TimedResolvedFocusPhaseV1(4L, "ca", 0L, false, true),
                    TimedResolvedFocusPhaseV1(5L, "ca", null, false, false),
                    TimedResolvedFocusPhaseV1(6L, null, null, false, false),
                    TimedResolvedFocusPhaseV1(7L, "cz", 0L, true, false),
                    TimedResolvedFocusPhaseV1(8L, "cz", 0L, false, true),
                    TimedResolvedFocusPhaseV1(9L, "ca", 0L, true, false)
                ), true, true, true)
            )
        ),
        "S" to Fixture(
            header = WorkoutSessionEntity(
                id = "s-S", planId = "plan-old", mode = "strength", status = "completed",
                planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History S","mode":"strength","blocks":[{"id":"sa","kind":"strength_exercise","title":"Strength block old","order":0,"exerciseId":"ex-sp","sets":[{"id":"set-a","order":0,"kind":"working"}],"substitutions":["ex-sa"],"setTimerMode":"manual_start"}],"preferences":null,"followAlong":null}""",
                startedAt = "2026-09-13T00:00:00Z", endedAt = "2026-09-13T00:00:06Z",
                totalElapsedSec = 6, effectiveElapsedSec = 4, pausedElapsedSec = 2,
                timelineVersion = 1, lastDurableOffsetMs = 6000L, lastMutationSequence = 5L,
                trustedEndOffsetMs = 6000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-sp","displayNameAtFirstReference":"Planned name","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-sa","displayNameAtFirstReference":"Actual base","customNameAtFirstReference":"Actual custom","resolutionSource":"runtime_substitution"}]}""",
                startLocalDate = "2026-09-13", startZoneId = "UTC", startUtcOffsetSeconds = 0L,
                timeMetadataSourceContractVersion = 1L
            ),
            phases = listOf(
                WorkoutPhaseIntervalEntity("s-S:p:0", "s-S", 0, 0L, 1000L, 0L, 1L, null, "strength_prepare_set", """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_prepare_set","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"prepare_set","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}"""),
                WorkoutPhaseIntervalEntity("s-S:p:1", "s-S", 1, 1000L, 2000L, 1L, 2L, null, "strength_active_set", """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_active_set","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"active_set","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}"""),
                WorkoutPhaseIntervalEntity("s-S:p:2", "s-S", 2, 2000L, 3000L, 2L, 3L, null, "strength_confirm_set", """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_confirm_set","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"confirm_set","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}"""),
                WorkoutPhaseIntervalEntity("s-S:p:3", "s-S", 3, 3000L, 4000L, 3L, 4L, null, "strength_rest", """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"rest","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}"""),
                WorkoutPhaseIntervalEntity("s-S:p:4", "s-S", 4, 4000L, 6000L, 4L, 5L, null, "paused", """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"paused","blockId":null,"setPlanId":null,"plannedExerciseId":null,"actualExerciseId":null,"exerciseSetIndex0":null,"globalSetIndex0":null,"setKind":null,"substitutedFromExerciseId":null}}""")
            ),
            title = "History S", mode = "strength",
            displays = listOf(
                ExpectedDisplay(0, """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_prepare_set","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"prepare_set","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}""", null, "resolved", "Actual custom"),
                ExpectedDisplay(1, """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_active_set","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"active_set","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}""", null, "resolved", "Actual custom"),
                ExpectedDisplay(2, """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_confirm_set","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"confirm_set","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}""", null, "resolved", "Actual custom"),
                ExpectedDisplay(3, """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"rest","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}""", null, "resolved", "Actual custom"),
                ExpectedDisplay(4, """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"paused","blockId":null,"setPlanId":null,"plannedExerciseId":null,"actualExerciseId":null,"exerciseSetIndex0":null,"globalSetIndex0":null,"setKind":null,"substitutedFromExerciseId":null}}""", null, "unresolved_missing_metadata", null)
            ),
            structures = emptyList()
        ),
        "F" to Fixture(
            header = WorkoutSessionEntity(
                id = "s-F", planId = "plan-old", mode = "follow_along", status = "completed",
                planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History F","mode":"follow_along","blocks":[{"id":"fw","kind":"warmup","title":"Follow warm","order":0,"durationSec":2,"items":[]},{"id":"fb","kind":"warmup","title":"Follow item block","order":1,"items":[{"id":"fi","exerciseId":"ex-f","labelOverride":"Follow item old","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":6,"restAfterSec":2,"autoAdvance":false}]},{"id":"fa","kind":"timed_circuit","order":2,"rounds":2,"restBetweenRoundsSec":3,"items":[{"id":"fc","exerciseId":"ex-fc","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":5,"restAfterSec":1,"autoAdvance":false}]},{"id":"fr","kind":"rest","order":3,"durationSec":4,"label":"Follow rest"}],"preferences":null,"followAlong":null}""",
                startedAt = "2026-09-13T00:00:00Z", endedAt = "2026-09-13T00:00:09Z",
                totalElapsedSec = 9, effectiveElapsedSec = 7, pausedElapsedSec = 2,
                timelineVersion = 1, lastDurableOffsetMs = 9000L, lastMutationSequence = 8L,
                trustedEndOffsetMs = 9000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-fc","displayNameAtFirstReference":"Follow circuit old","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-f","displayNameAtFirstReference":"Metadata F","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"}]}""",
                startLocalDate = "2026-09-13", startZoneId = "UTC", startUtcOffsetSeconds = 0L,
                timeMetadataSourceContractVersion = 1L
            ),
            phases = listOf(
                WorkoutPhaseIntervalEntity("s-F:p:0", "s-F", 0, 0L, 1000L, 0L, 1L, null, "follow_along_action", """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_action","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"circuit_action","blockId":"fa","stepIndex0":0,"followAlongStepKind":"action","itemId":"fc","exerciseId":"ex-fc","roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-F:p:1", "s-F", 1, 1000L, 2000L, 1L, 2L, null, "follow_along_action", """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_action","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"non_circuit_action","blockId":"fb","stepIndex0":0,"followAlongStepKind":"action","itemId":"fi","exerciseId":"ex-f","roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-F:p:2", "s-F", 2, 2000L, 3000L, 2L, 3L, null, "follow_along_rest", """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"circuit_rest_after_action","blockId":"fa","stepIndex0":1,"followAlongStepKind":"rest_after_action","itemId":"fc","exerciseId":"ex-fc","roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-F:p:3", "s-F", 3, 3000L, 4000L, 3L, 4L, null, "follow_along_rest", """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"non_circuit_rest_after_action","blockId":"fb","stepIndex0":1,"followAlongStepKind":"rest_after_action","itemId":"fi","exerciseId":"ex-f","roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-F:p:4", "s-F", 4, 4000L, 5000L, 4L, 5L, null, "follow_along_rest", """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"between_round_rest","blockId":"fa","stepIndex0":2,"followAlongStepKind":"between_round_rest","itemId":null,"exerciseId":null,"roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-F:p:5", "s-F", 5, 5000L, 6000L, 5L, 6L, null, "follow_along_rest", """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"block_rest","blockId":"fr","stepIndex0":0,"followAlongStepKind":"block_rest","itemId":null,"exerciseId":null,"roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-F:p:6", "s-F", 6, 6000L, 7000L, 6L, 7L, null, "follow_along_action", """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_action","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"boundary","blockId":"fw","stepIndex0":0,"followAlongStepKind":"boundary","itemId":null,"exerciseId":null,"roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-F:p:7", "s-F", 7, 7000L, 9000L, 7L, 8L, null, "paused", """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"paused","blockId":null,"stepIndex0":null,"followAlongStepKind":null,"itemId":null,"exerciseId":null,"roundIndex0":null}}""")
            ),
            title = "History F", mode = "follow_along",
            displays = listOf(
                ExpectedDisplay(0, """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_action","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"circuit_action","blockId":"fa","stepIndex0":0,"followAlongStepKind":"action","itemId":"fc","exerciseId":"ex-fc","roundIndex0":0}}""", null, "resolved", "Follow circuit old"),
                ExpectedDisplay(1, """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_action","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"non_circuit_action","blockId":"fb","stepIndex0":0,"followAlongStepKind":"action","itemId":"fi","exerciseId":"ex-f","roundIndex0":null}}""", null, "resolved", "Follow item old"),
                ExpectedDisplay(2, """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"circuit_rest_after_action","blockId":"fa","stepIndex0":1,"followAlongStepKind":"rest_after_action","itemId":"fc","exerciseId":"ex-fc","roundIndex0":0}}""", null, "resolved", "Follow circuit old"),
                ExpectedDisplay(3, """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"non_circuit_rest_after_action","blockId":"fb","stepIndex0":1,"followAlongStepKind":"rest_after_action","itemId":"fi","exerciseId":"ex-f","roundIndex0":null}}""", null, "resolved", "Follow item old"),
                ExpectedDisplay(4, """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"between_round_rest","blockId":"fa","stepIndex0":2,"followAlongStepKind":"between_round_rest","itemId":null,"exerciseId":null,"roundIndex0":0}}""", null, "unresolved_missing_metadata", null),
                ExpectedDisplay(5, """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"block_rest","blockId":"fr","stepIndex0":0,"followAlongStepKind":"block_rest","itemId":null,"exerciseId":null,"roundIndex0":null}}""", null, "resolved", "Follow rest"),
                ExpectedDisplay(6, """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"follow_along_action","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"boundary","blockId":"fw","stepIndex0":0,"followAlongStepKind":"boundary","itemId":null,"exerciseId":null,"roundIndex0":null}}""", null, "resolved", "Follow warm"),
                ExpectedDisplay(7, """{"phaseIdentityContractVersion":1,"family":"follow_along_v1","payloadVersion":1,"mode":"follow_along","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c790e674e859ef27a0e15034766b803a9f8f70ba2b52f2f81c30c48df757f218"},"payload":{"variant":"paused","blockId":null,"stepIndex0":null,"followAlongStepKind":null,"itemId":null,"exerciseId":null,"roundIndex0":null}}""", null, "unresolved_missing_metadata", null)
            ),
            structures = emptyList()
        ),
        "L0" to Fixture(
            header = WorkoutSessionEntity(
                id = "s-L0", planId = "plan-old", mode = "timed", status = "completed",
                planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History L0","mode":"timed","blocks":[{"id":"a0","kind":"timed_circuit","order":0,"rounds":1,"restBetweenRoundsSec":0,"items":[{"id":"w0","labelOverride":"Zero work","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":0,"autoAdvance":false},{"id":"r0","labelOverride":"Zero rest","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":0,"autoAdvance":true}]}],"preferences":null,"followAlong":null}""",
                startedAt = "2026-09-13T00:00:00Z", endedAt = "2026-09-13T00:00:02Z",
                totalElapsedSec = 2, effectiveElapsedSec = 2, pausedElapsedSec = 0,
                timelineVersion = 1, lastDurableOffsetMs = 2000L, lastMutationSequence = 2L,
                trustedEndOffsetMs = 2000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[]}""",
                startLocalDate = "2026-09-13", startZoneId = "UTC", startUtcOffsetSeconds = 0L,
                timeMetadataSourceContractVersion = 1L
            ),
            phases = listOf(
                WorkoutPhaseIntervalEntity("s-L0:p:0", "s-L0", 0, 0L, 1000L, 0L, 1L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"615326dad7bf37051537efda9d54db84baab88c889e1574ad2ebd25b9a582dc8"},"payload":{"variant":"circuit_item_work","blockId":"a0","stepIndex0":0,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"w0","exerciseId":null,"roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-L0:p:1", "s-L0", 1, 1000L, 2000L, 1L, 2L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"615326dad7bf37051537efda9d54db84baab88c889e1574ad2ebd25b9a582dc8"},"payload":{"variant":"circuit_item_rest","blockId":"a0","stepIndex0":1,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":"r0","exerciseId":null,"roundIndex0":0}}""")
            ),
            title = "History L0", mode = "timed",
            displays = listOf(
                ExpectedDisplay(0, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"615326dad7bf37051537efda9d54db84baab88c889e1574ad2ebd25b9a582dc8"},"payload":{"variant":"circuit_item_work","blockId":"a0","stepIndex0":0,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"w0","exerciseId":null,"roundIndex0":0}}""", 0L, "resolved", "Zero work"),
                ExpectedDisplay(1, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"615326dad7bf37051537efda9d54db84baab88c889e1574ad2ebd25b9a582dc8"},"payload":{"variant":"circuit_item_rest","blockId":"a0","stepIndex0":1,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":"r0","exerciseId":null,"roundIndex0":0}}""", 0L, "resolved", "Zero rest")
            ),
            structures = listOf(
                TimedResolvedStructureV1("s-L0", "legacy_timed_v1", "615326dad7bf37051537efda9d54db84baab88c889e1574ad2ebd25b9a582dc8", listOf(
                    TimedResolvedFocusPhaseV1(0L, "a0", 0L, false, false),
                    TimedResolvedFocusPhaseV1(1L, "a0", 0L, false, false)
                ), false, false, false)
            )
        ),
        "C0" to Fixture(
            header = WorkoutSessionEntity(
                id = "s-C0", planId = "plan-old", mode = "timed", status = "completed",
                planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History C0","mode":"timed","blocks":[{"id":"c0","kind":"timed_composition","order":0,"compositionVersion":2,"warmupSec":0,"cooldownSec":0,"rounds":1,"restBetweenRoundsSec":0,"stageGroups":[{"id":"g0","order":0,"name":"Zero group","colorHex":"#112233","targets":[{"id":"a0","order":0,"name":"Zero action","kind":"action","durationSec":0,"colorHex":"#112233","autoAdvance":false},{"id":"x0","order":1,"name":"Zero custom","kind":"custom","durationSec":0,"colorHex":"#112233","autoAdvance":false},{"id":"r0","order":2,"name":"Zero rest","kind":"rest","durationSec":0,"colorHex":"#223344","autoAdvance":true}]}]}],"preferences":null,"followAlong":null}""",
                startedAt = "2026-09-13T00:00:00Z", endedAt = "2026-09-13T00:00:03Z",
                totalElapsedSec = 3, effectiveElapsedSec = 3, pausedElapsedSec = 0,
                timelineVersion = 1, lastDurableOffsetMs = 3000L, lastMutationSequence = 3L,
                trustedEndOffsetMs = 3000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[]}""",
                startLocalDate = "2026-09-13", startZoneId = "UTC", startUtcOffsetSeconds = 0L,
                timeMetadataSourceContractVersion = 1L
            ),
            phases = listOf(
                WorkoutPhaseIntervalEntity("s-C0:p:0", "s-C0", 0, 0L, 1000L, 0L, 1L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"2378a45686643cda24c5109b6c5e68ac52c40ff16337b4ec1871cf67e8a44134"},"payload":{"variant":"stage_group_action","compositionVersion":2,"compositionBlockId":"c0","timelineStageId":"c0:r1:g1:g0","timelineStageKind":"stage_group","stageGroupId":"g0","targetId":"a0","targetKind":"action","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":0,"targetInstanceIndex0":0,"stepIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-C0:p:1", "s-C0", 1, 1000L, 2000L, 1L, 2L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"2378a45686643cda24c5109b6c5e68ac52c40ff16337b4ec1871cf67e8a44134"},"payload":{"variant":"stage_group_custom","compositionVersion":2,"compositionBlockId":"c0","timelineStageId":"c0:r1:g1:g0","timelineStageKind":"stage_group","stageGroupId":"g0","targetId":"x0","targetKind":"custom","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":1,"stageInstanceIndex0":0,"targetInstanceIndex0":1,"stepIndex0":1}}"""),
                WorkoutPhaseIntervalEntity("s-C0:p:2", "s-C0", 2, 2000L, 3000L, 2L, 3L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"2378a45686643cda24c5109b6c5e68ac52c40ff16337b4ec1871cf67e8a44134"},"payload":{"variant":"stage_group_rest","compositionVersion":2,"compositionBlockId":"c0","timelineStageId":"c0:r1:g1:g0","timelineStageKind":"stage_group","stageGroupId":"g0","targetId":"r0","targetKind":"rest","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":2,"stageInstanceIndex0":0,"targetInstanceIndex0":2,"stepIndex0":2}}""")
            ),
            title = "History C0", mode = "timed",
            displays = listOf(
                ExpectedDisplay(0, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"2378a45686643cda24c5109b6c5e68ac52c40ff16337b4ec1871cf67e8a44134"},"payload":{"variant":"stage_group_action","compositionVersion":2,"compositionBlockId":"c0","timelineStageId":"c0:r1:g1:g0","timelineStageKind":"stage_group","stageGroupId":"g0","targetId":"a0","targetKind":"action","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":0,"targetInstanceIndex0":0,"stepIndex0":0}}""", 0L, "resolved", "Zero action"),
                ExpectedDisplay(1, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"2378a45686643cda24c5109b6c5e68ac52c40ff16337b4ec1871cf67e8a44134"},"payload":{"variant":"stage_group_custom","compositionVersion":2,"compositionBlockId":"c0","timelineStageId":"c0:r1:g1:g0","timelineStageKind":"stage_group","stageGroupId":"g0","targetId":"x0","targetKind":"custom","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":1,"stageInstanceIndex0":0,"targetInstanceIndex0":1,"stepIndex0":1}}""", 0L, "resolved", "Zero custom"),
                ExpectedDisplay(2, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"2378a45686643cda24c5109b6c5e68ac52c40ff16337b4ec1871cf67e8a44134"},"payload":{"variant":"stage_group_rest","compositionVersion":2,"compositionBlockId":"c0","timelineStageId":"c0:r1:g1:g0","timelineStageKind":"stage_group","stageGroupId":"g0","targetId":"r0","targetKind":"rest","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":2,"stageInstanceIndex0":0,"targetInstanceIndex0":2,"stepIndex0":2}}""", 0L, "resolved", "Zero rest")
            ),
            structures = listOf(
                TimedResolvedStructureV1("s-C0", "timed_composition_v2", "2378a45686643cda24c5109b6c5e68ac52c40ff16337b4ec1871cf67e8a44134", listOf(
                    TimedResolvedFocusPhaseV1(0L, "c0", 0L, false, false),
                    TimedResolvedFocusPhaseV1(1L, "c0", 0L, false, false),
                    TimedResolvedFocusPhaseV1(2L, "c0", 0L, false, false)
                ), false, false, false)
            )
        ),
        "M" to Fixture(
            header = WorkoutSessionEntity(
                id = "s-M", planId = "plan-old", mode = "timed", status = "completed",
                planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History M","mode":"timed","blocks":[{"id":"w","kind":"warmup","title":"Warm old","order":0,"durationSec":2,"items":[]},{"id":"t","kind":"stretch","order":1,"durationSec":3,"items":[]},{"id":"d","kind":"cooldown","title":"Cool old","order":2,"durationSec":4,"items":[]},{"id":"b","kind":"warmup","title":"Boundary old","order":3,"items":[{"id":"bw","exerciseId":"ex-b","labelOverride":"Boundary item","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":6,"restAfterSec":2,"autoAdvance":false},{"id":"br","labelOverride":"Boundary rest","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"a","kind":"timed_circuit","title":"Circuit A","order":4,"rounds":2,"restBetweenRoundsSec":5,"items":[{"id":"aw","exerciseId":"ex-a","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":10,"restAfterSec":4,"autoAdvance":false},{"id":"ar","labelOverride":"Item rest A","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"z","kind":"timed_circuit","title":"Circuit Z","order":5,"rounds":1,"restBetweenRoundsSec":0,"items":[{"id":"zw","exerciseId":"ex-z","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":8,"restAfterSec":2,"autoAdvance":false}]},{"id":"r","kind":"rest","title":"Rest title","order":6,"durationSec":7,"label":"Rest label"},{"id":"ca","kind":"timed_composition","title":"Composition A","order":0,"compositionVersion":2,"warmupSec":2,"cooldownSec":4,"rounds":2,"restBetweenRoundsSec":3,"stageGroups":[{"id":"glate","order":1,"name":"Group late","colorHex":"#112233","targets":[{"id":"late","order":0,"name":"Late old","kind":"action","durationSec":7,"colorHex":"#112233","autoAdvance":false}]},{"id":"ga","order":0,"name":"Group A","colorHex":"#112233","targets":[{"id":"rr","order":1,"name":"Rest target old","kind":"rest","durationSec":5,"colorHex":"#223344","autoAdvance":true},{"id":"aa","order":0,"name":"Action old","kind":"action","durationSec":11,"colorHex":"#112233","autoAdvance":false},{"id":"xx","order":2,"name":"Custom old","kind":"custom","durationSec":13,"colorHex":"#334455","autoAdvance":false}]}]},{"id":"cz","kind":"timed_composition","title":"Composition Z","order":1,"compositionVersion":2,"warmupSec":0,"cooldownSec":0,"rounds":1,"restBetweenRoundsSec":0,"stageGroups":[{"id":"gz","order":0,"name":"Group Z","colorHex":"#112233","targets":[{"id":"za","order":0,"name":"Z action old","kind":"action","durationSec":6,"colorHex":"#112233","autoAdvance":false},{"id":"zr","order":1,"name":"Z rest old","kind":"rest","durationSec":2,"colorHex":"#223344","autoAdvance":true}]}]}],"preferences":null,"followAlong":null}""",
                startedAt = "2026-09-13T00:00:00Z", endedAt = "2026-09-13T00:00:06Z",
                totalElapsedSec = 6, effectiveElapsedSec = 4, pausedElapsedSec = 2,
                timelineVersion = 1, lastDurableOffsetMs = 6000L, lastMutationSequence = 6L,
                trustedEndOffsetMs = 6000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-b","displayNameAtFirstReference":"Metadata B","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-a","displayNameAtFirstReference":"Base A","customNameAtFirstReference":"Custom A","resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-z","displayNameAtFirstReference":"Exercise Z","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"}]}""",
                startLocalDate = "2026-09-13", startZoneId = "UTC", startUtcOffsetSeconds = 0L,
                timeMetadataSourceContractVersion = 1L
            ),
            phases = listOf(
                WorkoutPhaseIntervalEntity("s-M:p:0", "s-M", 0, 0L, 1000L, 0L, 1L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"circuit_item_work","blockId":"a","stepIndex0":0,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"aw","exerciseId":"ex-a","roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-M:p:1", "s-M", 1, 1000L, 2000L, 1L, 2L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"circuit_rest_after_item","blockId":"a","stepIndex0":1,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":"aw","exerciseId":"ex-a","roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-M:p:2", "s-M", 2, 2000L, 3000L, 2L, 3L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"stage_group_action","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g1:ga","timelineStageKind":"stage_group","stageGroupId":"ga","targetId":"aa","targetKind":"action","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":1,"targetInstanceIndex0":1,"stepIndex0":1}}"""),
                WorkoutPhaseIntervalEntity("s-M:p:3", "s-M", 3, 3000L, 4000L, 3L, 4L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"stage_group_rest","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g1:ga","timelineStageKind":"stage_group","stageGroupId":"ga","targetId":"rr","targetKind":"rest","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":1,"stageInstanceIndex0":1,"targetInstanceIndex0":2,"stepIndex0":2}}"""),
                WorkoutPhaseIntervalEntity("s-M:p:4", "s-M", 4, 4000L, 5000L, 4L, 5L, null, "paused", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"paused","blockId":null,"stepIndex0":null,"legacyBlockKind":null,"legacyStageType":null,"itemId":null,"exerciseId":null,"roundIndex0":null}}"""),
                WorkoutPhaseIntervalEntity("s-M:p:5", "s-M", 5, 5000L, 6000L, 5L, 6L, null, "paused", """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"paused","compositionVersion":2,"compositionBlockId":null,"timelineStageId":null,"timelineStageKind":null,"stageGroupId":null,"targetId":null,"targetKind":null,"roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":null,"stageInstanceIndex0":null,"targetInstanceIndex0":null,"stepIndex0":null}}""")
            ),
            title = "History M", mode = "timed",
            displays = listOf(
                ExpectedDisplay(0, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"circuit_item_work","blockId":"a","stepIndex0":0,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"aw","exerciseId":"ex-a","roundIndex0":0}}""", 10000L, "resolved", "Custom A"),
                ExpectedDisplay(1, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"circuit_rest_after_item","blockId":"a","stepIndex0":1,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":"aw","exerciseId":"ex-a","roundIndex0":0}}""", 4000L, "resolved", "Custom A"),
                ExpectedDisplay(2, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"stage_group_action","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g1:ga","timelineStageKind":"stage_group","stageGroupId":"ga","targetId":"aa","targetKind":"action","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":1,"targetInstanceIndex0":1,"stepIndex0":1}}""", 11000L, "resolved", "Action old"),
                ExpectedDisplay(3, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"stage_group_rest","compositionVersion":2,"compositionBlockId":"ca","timelineStageId":"ca:r1:g1:ga","timelineStageKind":"stage_group","stageGroupId":"ga","targetId":"rr","targetKind":"rest","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":1,"stageInstanceIndex0":1,"targetInstanceIndex0":2,"stepIndex0":2}}""", 5000L, "resolved", "Rest target old"),
                ExpectedDisplay(4, """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"paused","blockId":null,"stepIndex0":null,"legacyBlockKind":null,"legacyStageType":null,"itemId":null,"exerciseId":null,"roundIndex0":null}}""", null, "unresolved_missing_metadata", null),
                ExpectedDisplay(5, """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e"},"payload":{"variant":"paused","compositionVersion":2,"compositionBlockId":null,"timelineStageId":null,"timelineStageKind":null,"stageGroupId":null,"targetId":null,"targetKind":null,"roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":null,"stageInstanceIndex0":null,"targetInstanceIndex0":null,"stepIndex0":null}}""", null, "unresolved_missing_metadata", null)
            ),
            structures = listOf(
                TimedResolvedStructureV1("s-M", "legacy_timed_v1", "86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e", listOf(
                    TimedResolvedFocusPhaseV1(0L, "a", 0L, true, false),
                    TimedResolvedFocusPhaseV1(1L, "a", 0L, false, true),
                    TimedResolvedFocusPhaseV1(4L, null, null, false, false)
                ), true, true, true),
                TimedResolvedStructureV1("s-M", "timed_composition_v2", "86248faa7fbb09db2ed842f97beb98b788b6552e7f926d608834212efd98cb9e", listOf(
                    TimedResolvedFocusPhaseV1(2L, "ca", 0L, true, false),
                    TimedResolvedFocusPhaseV1(3L, "ca", 0L, false, true),
                    TimedResolvedFocusPhaseV1(5L, null, null, false, false)
                ), true, true, true)
            )
        ),
        "O" to Fixture(
            header = WorkoutSessionEntity(
                id = "s-O", planId = "plan-old", mode = "timed", status = "completed",
                planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History O","mode":"timed","blocks":[{"id":"a0","kind":"timed_circuit","order":0,"rounds":1,"restBetweenRoundsSec":0,"items":[{"id":"w0","labelOverride":"Zero work","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":9223372036854776,"autoAdvance":false},{"id":"r0","labelOverride":"Zero rest","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":0,"autoAdvance":true}]}],"preferences":null,"followAlong":null}""",
                startedAt = "2026-09-13T00:00:00Z", endedAt = "2026-09-13T00:00:02Z",
                totalElapsedSec = 2, effectiveElapsedSec = 2, pausedElapsedSec = 0,
                timelineVersion = 1, lastDurableOffsetMs = 2000L, lastMutationSequence = 2L,
                trustedEndOffsetMs = 2000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[]}""",
                startLocalDate = "2026-09-13", startZoneId = "UTC", startUtcOffsetSeconds = 0L,
                timeMetadataSourceContractVersion = 1L
            ),
            phases = listOf(
                WorkoutPhaseIntervalEntity("s-O:p:0", "s-O", 0, 0L, 1000L, 0L, 1L, null, "timed_work", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"01a59e8c136ef85b38af9021446b576e32ecfc17f1fa04473a6b74011e290efc"},"payload":{"variant":"circuit_item_work","blockId":"a0","stepIndex0":0,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"w0","exerciseId":null,"roundIndex0":0}}"""),
                WorkoutPhaseIntervalEntity("s-O:p:1", "s-O", 1, 1000L, 2000L, 1L, 2L, null, "timed_rest", """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"01a59e8c136ef85b38af9021446b576e32ecfc17f1fa04473a6b74011e290efc"},"payload":{"variant":"circuit_item_rest","blockId":"a0","stepIndex0":1,"legacyBlockKind":"timed_circuit","legacyStageType":"rest","itemId":"r0","exerciseId":null,"roundIndex0":0}}""")
            ),
            title = "History O", mode = "timed",
            displays = emptyList(),
            structures = emptyList()
        ),
    )

    private val nameCases = listOf(
        fixtures.getValue("S").copy(
            header = fixtures.getValue("S").header.copy(sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-sp","displayNameAtFirstReference":"Planned name","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"}]}"""),
            displays = fixtures.getValue("S").displays.map { expected ->
                if (expected.sequence in listOf(0, 1, 2, 3)) expected.copy(status = "unresolved_missing_metadata", label = null) else expected
            }
        ),
        fixtures.getValue("S").copy(
            header = fixtures.getValue("S").header.copy(sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-sp","displayNameAtFirstReference":"Planned name","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-sa","displayNameAtFirstReference":"","customNameAtFirstReference":null,"resolutionSource":"runtime_substitution"}]}"""),
            displays = fixtures.getValue("S").displays.map { expected ->
                if (expected.sequence in listOf(0, 1, 2, 3)) expected.copy(status = "unresolved_invalid_metadata", label = null) else expected
            }
        ),
        fixtures.getValue("S").copy(
            header = fixtures.getValue("S").header.copy(sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-sp","displayNameAtFirstReference":"Planned name","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-sa","displayNameAtFirstReference":"Actual base","customNameAtFirstReference":"","resolutionSource":"runtime_substitution"}]}"""),
            displays = fixtures.getValue("S").displays.map { expected ->
                if (expected.sequence in listOf(0, 1, 2, 3)) expected.copy(status = "unresolved_invalid_metadata", label = null) else expected
            }
        ),
        fixtures.getValue("S").copy(
            header = fixtures.getValue("S").header.copy(sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-sp","displayNameAtFirstReference":"Planned name","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-sa","displayNameAtFirstReference":"Actual base","customNameAtFirstReference":" ","resolutionSource":"runtime_substitution"}]}"""),
            displays = fixtures.getValue("S").displays.map { expected ->
                if (expected.sequence in listOf(0, 1, 2, 3)) expected.copy(status = "resolved", label = " ") else expected
            }
        ),
        fixtures.getValue("L").copy(
            header = fixtures.getValue("L").header.copy(planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History L","mode":"timed","blocks":[{"id":"w","kind":"warmup","title":"Warm old","order":0,"durationSec":2,"items":[]},{"id":"t","kind":"stretch","order":1,"durationSec":3,"items":[]},{"id":"d","kind":"cooldown","title":"Cool old","order":2,"durationSec":4,"items":[]},{"id":"b","kind":"warmup","title":"Boundary old","order":3,"items":[{"id":"bw","exerciseId":"ex-b","labelOverride":"Boundary item","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":6,"restAfterSec":2,"autoAdvance":false},{"id":"br","labelOverride":"Boundary rest","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"a","kind":"timed_circuit","title":"Circuit A","order":4,"rounds":2,"restBetweenRoundsSec":5,"items":[{"id":"aw","exerciseId":"ex-a","labelOverride":"","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":10,"restAfterSec":4,"autoAdvance":false},{"id":"ar","labelOverride":"Item rest A","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"z","kind":"timed_circuit","title":"Circuit Z","order":5,"rounds":1,"restBetweenRoundsSec":0,"items":[{"id":"zw","exerciseId":"ex-z","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":8,"restAfterSec":2,"autoAdvance":false}]},{"id":"r","kind":"rest","title":"Rest title","order":6,"durationSec":7,"label":"Rest label"}],"preferences":null,"followAlong":null}"""),
            displays = fixtures.getValue("L").displays.map { expected ->
                if (expected.sequence in listOf(6, 8)) expected.copy(status = "unresolved_invalid_metadata", label = null) else expected
            }
        ),
        fixtures.getValue("L").copy(
            header = fixtures.getValue("L").header.copy(planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History L","mode":"timed","blocks":[{"id":"w","kind":"warmup","title":"Warm old","order":0,"durationSec":2,"items":[]},{"id":"t","kind":"stretch","order":1,"durationSec":3,"items":[]},{"id":"d","kind":"cooldown","title":"Cool old","order":2,"durationSec":4,"items":[]},{"id":"b","kind":"warmup","title":"Boundary old","order":3,"items":[{"id":"bw","exerciseId":"ex-b","labelOverride":"Boundary item","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":6,"restAfterSec":2,"autoAdvance":false},{"id":"br","labelOverride":"Boundary rest","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"a","kind":"timed_circuit","title":"Circuit A","order":4,"rounds":2,"restBetweenRoundsSec":5,"items":[{"id":"aw","exerciseId":"ex-a","labelOverride":"Override old","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":10,"restAfterSec":4,"autoAdvance":false},{"id":"ar","labelOverride":"Item rest A","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"z","kind":"timed_circuit","title":"Circuit Z","order":5,"rounds":1,"restBetweenRoundsSec":0,"items":[{"id":"zw","exerciseId":"ex-z","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":8,"restAfterSec":2,"autoAdvance":false}]},{"id":"r","kind":"rest","title":"Rest title","order":6,"durationSec":7,"label":"Rest label"}],"preferences":null,"followAlong":null}"""),
            displays = fixtures.getValue("L").displays.map { expected ->
                if (expected.sequence in listOf(6, 8)) expected.copy(status = "resolved", label = "Override old") else expected
            }
        ),
        fixtures.getValue("L").copy(
            header = fixtures.getValue("L").header.copy(sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-b","displayNameAtFirstReference":"Metadata B","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-a","displayNameAtFirstReference":"Base A","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-z","displayNameAtFirstReference":"Exercise Z","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"}]}"""),
            displays = fixtures.getValue("L").displays.map { expected ->
                if (expected.sequence in listOf(6, 8)) expected.copy(status = "resolved", label = "Base A") else expected
            }
        ),
        fixtures.getValue("C").copy(
            header = fixtures.getValue("C").header.copy(planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History C","mode":"timed","blocks":[{"id":"ca","kind":"timed_composition","title":"Composition A","order":0,"compositionVersion":2,"warmupSec":2,"cooldownSec":4,"rounds":2,"restBetweenRoundsSec":3,"stageGroups":[{"id":"glate","order":1,"name":"Group late","colorHex":"#112233","targets":[{"id":"late","order":0,"name":"Late old","kind":"action","durationSec":7,"colorHex":"#112233","autoAdvance":false}]},{"id":"ga","order":0,"name":"Group A","colorHex":"#112233","targets":[{"id":"rr","order":1,"name":"Rest target old","kind":"rest","durationSec":5,"colorHex":"#223344","autoAdvance":true},{"id":"aa","order":0,"name":"","kind":"action","durationSec":11,"colorHex":"#112233","autoAdvance":false},{"id":"xx","order":2,"name":"Custom old","kind":"custom","durationSec":13,"colorHex":"#334455","autoAdvance":false}]}]},{"id":"cz","kind":"timed_composition","title":"Composition Z","order":1,"compositionVersion":2,"warmupSec":0,"cooldownSec":0,"rounds":1,"restBetweenRoundsSec":0,"stageGroups":[{"id":"gz","order":0,"name":"Group Z","colorHex":"#112233","targets":[{"id":"za","order":0,"name":"Z action old","kind":"action","durationSec":6,"colorHex":"#112233","autoAdvance":false},{"id":"zr","order":1,"name":"Z rest old","kind":"rest","durationSec":2,"colorHex":"#223344","autoAdvance":true}]}]}],"preferences":null,"followAlong":null}"""),
            displays = fixtures.getValue("C").displays.map { expected ->
                if (expected.sequence in listOf(1)) expected.copy(status = "unresolved_invalid_metadata", label = null) else expected
            }
        ),
        fixtures.getValue("L").copy(
            header = fixtures.getValue("L").header.copy(planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History L","mode":"timed","blocks":[{"id":"w","kind":"warmup","title":"Warm old","order":0,"durationSec":2,"items":[]},{"id":"t","kind":"stretch","order":1,"durationSec":3,"items":[]},{"id":"d","kind":"cooldown","title":"Cool old","order":2,"durationSec":4,"items":[]},{"id":"b","kind":"warmup","title":"Boundary old","order":3,"items":[{"id":"bw","exerciseId":"ex-b","labelOverride":"Boundary item","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":6,"restAfterSec":2,"autoAdvance":false},{"id":"br","labelOverride":"Boundary rest","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"a","kind":"timed_circuit","title":"Circuit A","order":4,"rounds":2,"restBetweenRoundsSec":5,"items":[{"id":"aw","exerciseId":"ex-a","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":10,"restAfterSec":4,"autoAdvance":false},{"id":"ar","labelOverride":"Item rest A","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"z","kind":"timed_circuit","title":"Circuit Z","order":5,"rounds":1,"restBetweenRoundsSec":0,"items":[{"id":"zw","exerciseId":"ex-z","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":8,"restAfterSec":2,"autoAdvance":false}]},{"id":"r","kind":"rest","title":"Rest title","order":6,"durationSec":7,"label":""}],"preferences":null,"followAlong":null}"""),
            displays = fixtures.getValue("L").displays.map { expected ->
                if (expected.sequence in listOf(10)) expected.copy(status = "unresolved_invalid_metadata", label = null) else expected
            }
        ),
        fixtures.getValue("L").copy(
            header = fixtures.getValue("L").header.copy(planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History L","mode":"timed","blocks":[{"id":"w","kind":"warmup","order":0,"durationSec":2,"items":[]},{"id":"t","kind":"stretch","order":1,"durationSec":3,"items":[]},{"id":"d","kind":"cooldown","title":"Cool old","order":2,"durationSec":4,"items":[]},{"id":"b","kind":"warmup","title":"Boundary old","order":3,"items":[{"id":"bw","exerciseId":"ex-b","labelOverride":"Boundary item","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":6,"restAfterSec":2,"autoAdvance":false},{"id":"br","labelOverride":"Boundary rest","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"a","kind":"timed_circuit","title":"Circuit A","order":4,"rounds":2,"restBetweenRoundsSec":5,"items":[{"id":"aw","exerciseId":"ex-a","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":10,"restAfterSec":4,"autoAdvance":false},{"id":"ar","labelOverride":"Item rest A","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"z","kind":"timed_circuit","title":"Circuit Z","order":5,"rounds":1,"restBetweenRoundsSec":0,"items":[{"id":"zw","exerciseId":"ex-z","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":8,"restAfterSec":2,"autoAdvance":false}]},{"id":"r","kind":"rest","title":"Rest title","order":6,"durationSec":7,"label":"Rest label"}],"preferences":null,"followAlong":null}"""),
            displays = fixtures.getValue("L").displays.map { expected ->
                if (expected.sequence in listOf(0)) expected.copy(status = "unresolved_missing_metadata", label = null) else expected
            }
        ),
    )

    private val focusCases = mapOf(
        "L" to listOf(
            """{"focusContractVersion":1,"sessionId":"s-L","family":"legacy_timed_v1","structureDigestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354","focusKind":"whole","legacyBlockId":null,"compositionBlockId":null,"roundIndex0":null,"phaseSequence":null}""" to
                TimedFocusIdentityV1(1, "s-L", "legacy_timed_v1", "f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354", "whole", null, null, null, null),
            """{"focusContractVersion":1,"sessionId":"s-L","family":"legacy_timed_v1","structureDigestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354","focusKind":"round","legacyBlockId":"a","compositionBlockId":null,"roundIndex0":0,"phaseSequence":null}""" to
                TimedFocusIdentityV1(1, "s-L", "legacy_timed_v1", "f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354", "round", "a", null, 0L, null),
            """{"focusContractVersion":1,"sessionId":"s-L","family":"legacy_timed_v1","structureDigestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354","focusKind":"round","legacyBlockId":"z","compositionBlockId":null,"roundIndex0":0,"phaseSequence":null}""" to
                TimedFocusIdentityV1(1, "s-L", "legacy_timed_v1", "f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354", "round", "z", null, 0L, null),
            """{"focusContractVersion":1,"sessionId":"s-L","family":"legacy_timed_v1","structureDigestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354","focusKind":"work","legacyBlockId":"a","compositionBlockId":null,"roundIndex0":0,"phaseSequence":6}""" to
                TimedFocusIdentityV1(1, "s-L", "legacy_timed_v1", "f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354", "work", "a", null, 0L, 6L),
            """{"focusContractVersion":1,"sessionId":"s-L","family":"legacy_timed_v1","structureDigestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354","focusKind":"rest","legacyBlockId":"z","compositionBlockId":null,"roundIndex0":0,"phaseSequence":13}""" to
                TimedFocusIdentityV1(1, "s-L", "legacy_timed_v1", "f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354", "rest", "z", null, 0L, 13L)
        ),
        "C" to listOf(
            """{"focusContractVersion":1,"sessionId":"s-C","family":"timed_composition_v2","structureDigestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c","focusKind":"whole","legacyBlockId":null,"compositionBlockId":null,"roundIndex0":null,"phaseSequence":null}""" to
                TimedFocusIdentityV1(1, "s-C", "timed_composition_v2", "9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c", "whole", null, null, null, null),
            """{"focusContractVersion":1,"sessionId":"s-C","family":"timed_composition_v2","structureDigestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c","focusKind":"round","legacyBlockId":null,"compositionBlockId":"ca","roundIndex0":0,"phaseSequence":null}""" to
                TimedFocusIdentityV1(1, "s-C", "timed_composition_v2", "9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c", "round", null, "ca", 0L, null),
            """{"focusContractVersion":1,"sessionId":"s-C","family":"timed_composition_v2","structureDigestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c","focusKind":"round","legacyBlockId":null,"compositionBlockId":"cz","roundIndex0":0,"phaseSequence":null}""" to
                TimedFocusIdentityV1(1, "s-C", "timed_composition_v2", "9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c", "round", null, "cz", 0L, null),
            """{"focusContractVersion":1,"sessionId":"s-C","family":"timed_composition_v2","structureDigestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c","focusKind":"work","legacyBlockId":null,"compositionBlockId":"ca","roundIndex0":0,"phaseSequence":1}""" to
                TimedFocusIdentityV1(1, "s-C", "timed_composition_v2", "9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c", "work", null, "ca", 0L, 1L),
            """{"focusContractVersion":1,"sessionId":"s-C","family":"timed_composition_v2","structureDigestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c","focusKind":"rest","legacyBlockId":null,"compositionBlockId":"cz","roundIndex0":0,"phaseSequence":8}""" to
                TimedFocusIdentityV1(1, "s-C", "timed_composition_v2", "9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c", "rest", null, "cz", 0L, 8L)
        ),
    )

    private val invalidFocuses = mapOf(
        "L" to """{"focusContractVersion":1,"sessionId":"s-L","family":"legacy_timed_v1","structureDigestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354","focusKind":"rest","legacyBlockId":"a","compositionBlockId":null,"roundIndex0":0,"phaseSequence":13}""",
        "C" to """{"focusContractVersion":1,"sessionId":"s-C","family":"timed_composition_v2","structureDigestHexLowercase":"9e3f21f094c5c0ec72a8a976f3a72e6ea88f357e82703bc13f2837128bd2453c","focusKind":"rest","legacyBlockId":null,"compositionBlockId":"ca","roundIndex0":0,"phaseSequence":8}"""
    )

    private val legacyHeaders = listOf(
        WorkoutSessionEntity(
            id = "legacy-t", planId = null, mode = "timed", status = "completed",
            planSnapshotJson = """{"title":"Legacy old","mode":"timed","blocks":[]}""",
            startedAt = null, endedAt = null, totalElapsedSec = null, effectiveElapsedSec = null, pausedElapsedSec = null,
            timelineVersion = null, lastDurableOffsetMs = null, lastMutationSequence = null, trustedEndOffsetMs = null,
            terminalReason = null, displayMetadataContractVersion = null, sessionDisplayMetadataJson = null,
            startLocalDate = null, startZoneId = null, startUtcOffsetSeconds = null, timeMetadataSourceContractVersion = null
        ),
        WorkoutSessionEntity(
            id = "legacy-s", planId = null, mode = "strength", status = "completed",
            planSnapshotJson = """{"title":"Legacy old","mode":"strength","blocks":[]}""",
            startedAt = null, endedAt = null, totalElapsedSec = null, effectiveElapsedSec = null, pausedElapsedSec = null,
            timelineVersion = null, lastDurableOffsetMs = null, lastMutationSequence = null, trustedEndOffsetMs = null,
            terminalReason = null, displayMetadataContractVersion = null, sessionDisplayMetadataJson = null,
            startLocalDate = null, startZoneId = null, startUtcOffsetSeconds = null, timeMetadataSourceContractVersion = null
        ),
        WorkoutSessionEntity(
            id = "legacy-f", planId = null, mode = "follow_along", status = "completed",
            planSnapshotJson = """{"title":"Legacy old","mode":"follow_along","blocks":[]}""",
            startedAt = null, endedAt = null, totalElapsedSec = null, effectiveElapsedSec = null, pausedElapsedSec = null,
            timelineVersion = null, lastDurableOffsetMs = null, lastMutationSequence = null, trustedEndOffsetMs = null,
            terminalReason = null, displayMetadataContractVersion = null, sessionDisplayMetadataJson = null,
            startLocalDate = null, startZoneId = null, startUtcOffsetSeconds = null, timeMetadataSourceContractVersion = null
        ),
    )

    private companion object {
        const val STRENGTH_BLOCKS = """[{"id":"sa","kind":"strength_exercise","title":"Strength block old","order":0,"exerciseId":"ex-sp","sets":[{"id":"set-a","order":0,"kind":"working"}],"substitutions":["ex-sa"],"setTimerMode":"manual_start"}]"""
    }
}
