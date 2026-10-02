package com.liujyks.trainflow.feature.history

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.liujyks.trainflow.app.MainActivity
import com.liujyks.trainflow.app.TrainFlowApplication
import com.liujyks.trainflow.core.data.WorkoutSessionStrictReadResult
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutSessionExportContractTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun repairsInterruptedRecordFromHistoryLongPress() = runBlocking {
        val application = compose.activity.application as TrainFlowApplication
        val database = application.trainFlowDatabase
        val repository = application.workoutSessionRepository
        val dao = database.workoutSessionDao()
        val fixtureIds = setOf("e21-s03-ui-I1", "e21-s03-ui-T1")
        val originalIds = dao.sessionHeaders().map { it.id }.toSet()
        assertTrue(originalIds.intersect(fixtureIds).isEmpty())
        var inserted = false
        var failure: Throwable? = null
        try {
            database.withTransaction {
                for (id in fixtureIds) assertTrue(dao.insertSession(fixedHeader(id)) != -1L)
                for (id in fixtureIds) for (phase in fixedPhases()) {
                    database.canonicalTimelineHeartRateDao().insertPhaseInterval(
                        phase.copy(id = id + ":p:" + phase.sequence, sessionId = id))
                }
            }
            inserted = true
            val before = repository.readSessionStrict("e21-s03-ui-I1")
            assertTrue(before.toString(), before is WorkoutSessionStrictReadResult.CanonicalTerminal)
            before as WorkoutSessionStrictReadResult.CanonicalTerminal
            val t1 = repository.readSessionStrict("e21-s03-ui-T1")
            assertTrue(t1.toString(), t1 is WorkoutSessionStrictReadResult.CanonicalTerminal)
            assertEquals("process_interrupted", before.graph.session.terminalReason)
            assertEquals(14_000L, before.graph.session.trustedEndOffsetMs)
            assertNull(before.graph.session.endedAt)
            assertNull(before.graph.recording)
            assertTrue(before.graph.samples.isEmpty())

            compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("记录").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithContentDescription("记录").performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("计时新名"))
            compose.onAllNodesWithText("计时新名").onFirst().performTouchInput { longClick() }
            compose.onNodeWithText("修复").assertIsDisplayed()
            compose.onNodeWithText("删除").assertIsDisplayed()
            compose.onNodeWithText("修复").performClick()
            withTimeout(5_000) {
                repository.historyEntries.first { entries ->
                    entries.any { it.id == "e21-s03-ui-I1" && it.terminalReason == "user_abandoned" }
                }
            }
            val expected = before.copy(graph = before.graph.copy(
                session = before.graph.session.copy(terminalReason = "user_abandoned")))
            val after = repository.readSessionStrict("e21-s03-ui-I1")
            assertEquals(expected, after)
            assertEquals(t1, repository.readSessionStrict("e21-s03-ui-T1"))
            compose.onAllNodesWithText("计时新名").onFirst().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("用户手动结束").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("用户手动结束").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("e21-s03-ui-I1").assertIsDisplayed()
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            val screenshotFile = File(
                "/sdcard/Android/media/${instrumentation.targetContext.packageName}/additional_test_output/",
                "R04-history-repaired.png")
            check(screenshotFile.parentFile!!.mkdirs() || screenshotFile.parentFile!!.isDirectory)
            screenshotFile.outputStream().use { output -> check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output)) }
            screenshot.recycle()
            println("R04_NATIVE_SCREENSHOT=" + screenshotFile.absolutePath)
            compose.onNodeWithText("返回记录总览").performScrollTo().performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("计时新名"))
            compose.onAllNodesWithText("计时新名").onLast().performScrollTo().performTouchInput { longClick() }
            compose.onNodeWithText("确认删除").assertIsDisplayed()
            compose.onNodeWithText("取消").performClick()
            println("R04 id=e21-s03-ui-I1 before=${before.graph} after=$after expected=$expected; T1 originalDeleteEntry=PASS; originalFieldsAndTrustedEnd=PASS")
        } catch (cause: Throwable) {
            failure = cause
        } finally {
            try {
                if (inserted) repository.deleteHistorySessions(fixtureIds)
                assertEquals(originalIds, dao.sessionHeaders().map { it.id }.toSet())
            } catch (cause: Throwable) {
                if (failure == null) failure = cause else requireNotNull(failure).addSuppressed(cause)
            }
        }
        failure?.let { throw it }
        Unit
    }

    // Fixed L header/closed phases from accepted WorkoutSessionExportTest; no HR rows.
    private fun fixedHeader(id: String): WorkoutSessionEntity = WorkoutSessionEntity(
                id = "s-L", planId = "plan-old", mode = "timed", status = "completed",
                planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History L","mode":"timed","blocks":[{"id":"w","kind":"warmup","title":"Warm old","order":0,"durationSec":2,"items":[]},{"id":"t","kind":"stretch","order":1,"durationSec":3,"items":[]},{"id":"d","kind":"cooldown","title":"Cool old","order":2,"durationSec":4,"items":[]},{"id":"b","kind":"warmup","title":"Boundary old","order":3,"items":[{"id":"bw","exerciseId":"ex-b","labelOverride":"Boundary item","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":6,"restAfterSec":2,"autoAdvance":false},{"id":"br","labelOverride":"Boundary rest","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"a","kind":"timed_circuit","title":"Circuit A","order":4,"rounds":2,"restBetweenRoundsSec":5,"items":[{"id":"aw","exerciseId":"ex-a","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":10,"restAfterSec":4,"autoAdvance":false},{"id":"ar","labelOverride":"Item rest A","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"z","kind":"timed_circuit","title":"Circuit Z","order":5,"rounds":1,"restBetweenRoundsSec":0,"items":[{"id":"zw","exerciseId":"ex-z","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":8,"restAfterSec":2,"autoAdvance":false}]},{"id":"r","kind":"rest","title":"Rest title","order":6,"durationSec":7,"label":"Rest label"}],"preferences":null,"followAlong":null}""",
                startedAt = "2026-09-13T00:00:00Z", endedAt = "2026-09-13T00:00:14Z",
                totalElapsedSec = 14, effectiveElapsedSec = 12, pausedElapsedSec = 2,
                timelineVersion = 1, lastDurableOffsetMs = 14000L, lastMutationSequence = 14L,
                trustedEndOffsetMs = 14000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-b","displayNameAtFirstReference":"Metadata B","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-a","displayNameAtFirstReference":"Base A","customNameAtFirstReference":"Custom A","resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-z","displayNameAtFirstReference":"Exercise Z","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"}]}""",
                startLocalDate = "2026-09-13", startZoneId = "UTC", startUtcOffsetSeconds = 0L,
                timeMetadataSourceContractVersion = 1L
            )
        .let { original ->
            original.copy(id = id, planId = "e21-s03-ui-PT",
                planSnapshotJson = original.planSnapshotJson.replace("plan-old", "e21-s03-ui-PT")
                    .replace("History L", "计时新名"),
                status = "abandoned", terminalReason = if (id.endsWith("I1")) "process_interrupted" else "user_abandoned",
                startedAt = if (id.endsWith("I1")) "2026-01-01T03:00:00Z" else "2026-01-01T02:00:00Z",
                endedAt = null, startLocalDate = "2026-01-01", startZoneId = "Asia/Shanghai",
                startUtcOffsetSeconds = 28_800L)
        }

    private fun fixedPhases(): List<WorkoutPhaseIntervalEntity> = listOf(
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
            )
}
