package com.liujyks.trainflow.feature.history

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.SemanticsMatcher
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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.isDisplayed
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.liujyks.trainflow.app.MainActivity
import com.liujyks.trainflow.app.TrainFlowApplication
import com.liujyks.trainflow.core.data.WorkoutSessionStrictReadResult
import com.liujyks.trainflow.core.data.HistoryItemKey
import com.liujyks.trainflow.core.data.WorkoutSessionExportSelection
import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1Validator
import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1Result
import com.liujyks.trainflow.core.data.toStorageJson
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.feature.workoutsession.freeFollowAlongPhase
import com.liujyks.trainflow.feature.workoutsession.freeFollowAlongSnapshot
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.TimeZone
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
    fun calendarCountsUseFrozenDatesAndOriginalNavigation() = withSelectionFixture("CUI01") {
        openRecords()
        // Original overview order: T2, group, I1, T1, S1, F1, T0.
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(12)
        compose.onAllNodesWithText("计时新名").onLast().performClick()
        waitText("e21-s03-ui-T1")
        compose.onNodeWithText("e21-s03-ui-T1").assertIsDisplayed()
        clickText("返回记录总览")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("合并记录（3段）"))
        compose.onNodeWithText("合并记录（3段）").performClick()
        val groupCard = compose.onNode(hasText("合并记录（3段）") and hasClickAction()).fetchSemanticsNode()
        val siblings = groupCard.parent!!.children
        val groupIndex = siblings.indexOfFirst { it.id == groupCard.id }
        // HistoryRoute emits the group card followed by its three member cards;
        // HistoryUiState orders those members by startedAt: G1, G2, G3.
        val members = siblings.subList(groupIndex + 1, groupIndex + 4)
        val secondMemberId = members[1].id
        compose.onNode(SemanticsMatcher("second original member of the expanded group") {
            it.id == secondMemberId
        }).performScrollTo().performClick()
        waitText("e21-s03-ui-G2")
        compose.onNodeWithText("e21-s03-ui-G2").assertIsDisplayed()
        systemBack()
        enterExport()
        showMonth("2025-12")
        compose.onNodeWithContentDescription("2025-12-31 计时1").assertIsDisplayed()
        showMonth("2026-01")
        assertJanuaryCounts()
        assertFalse(vm().state.visibleUnits.flatMap { it.members }.any { it.id == "e21-s03-ui-I1" })
        val originalZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            // Recompose the month grid in the same Activity; CUI04 owns the sole recreate.
            clickText("下个月")
            clickText("上个月")
            assertJanuaryCounts()
            showMonth("2025-12")
            compose.onNodeWithContentDescription("2025-12-31 计时1").assertIsDisplayed()
            showMonth("2026-01")
            screenshot("CUI01-calendar.png")
        } finally { TimeZone.setDefault(originalZone) }
        println("CUI01 literalCalendar=2025-12-31:timed1;2026-01-01:timed3,strength1,follow_along1,unfinished1;2026-01-02:timed1;2026-01-04:empty; originalNavigation=T1,G2; actualUnits=${vm().state.units}")
    }

    @Test
    fun calendarAndDateInputShareInclusiveRange() = withSelectionFixture("CUI02") {
        openRecords(); enterExport()
        showMonth("2025-12")
        clickDay("2025-12-31")
        showMonth("2026-01")
        clickDay("2026-01-02")
        val calendar = confirmIds(listOf("T2", "G1", "G2", "G3", "T1", "S1", "F1", "T0"))
        assertEquals(LocalDate.parse("2025-12-31"), calendar.startDateInclusive)
        assertEquals(LocalDate.parse("2026-01-02"), calendar.endDateInclusive)
        inputDates(listOf("2025", "12", "31", "2026", "1", "2"))
        clickText("应用日期")
        assertEquals(calendar, confirmIds(listOf("T2", "G1", "G2", "G3", "T1", "S1", "F1", "T0")))
        inputDates(listOf("2026", "1", "1", "2026", "1", "1"))
        clickText("应用日期")
        confirmIds(listOf("G1", "G2", "G3", "T1", "S1", "F1"))
        inputDates(listOf("2026", "2", "30", "2026", "3", "1"))
        compose.onNode(hasText("请输入有效日期") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText("应用日期").assertIsNotEnabled()
        assertEquals("30", vm().state.dateFields[2])
        assertFalse(vm().state.canConfirm)
        replaceDateFields(listOf("2026", "1", "2", "2026", "1", "1"))
        compose.onNode(hasText("结束日期不能早于开始日期") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText("应用日期").assertIsNotEnabled()
        assertEquals(listOf("2026", "1", "2", "2026", "1", "1"), vm().state.dateFields)
        replaceDateFields(listOf("2026", "1", "1", "2026", "1", "2"))
        compose.onNodeWithText("应用日期").assertIsEnabled().performClick()
        confirmIds(listOf("T2", "G1", "G2", "G3", "T1", "S1", "F1"))
        val previousFrozen = vm().frozenSelection
        val stateA = vm().state
        val frozenA = stateA.frozenSelection
        println("CUI02_DIAG stage=A stateStart=${stateA.startDate} stateEnd=${stateA.endDate} selectedIds=${stateA.selectedUnits.flatMap { it.members.map { member -> member.id } }} frozenStart=${frozenA?.startDateInclusive} frozenEnd=${frozenA?.endDateInclusive} frozenIds=${frozenA?.includedSessionIds} sameAsA=${frozenA === previousFrozen}")
        clickText("不限日期")
        val stateB = vm().state
        val frozenB = stateB.frozenSelection
        println("CUI02_DIAG stage=B stateStart=${stateB.startDate} stateEnd=${stateB.endDate} selectedIds=${stateB.selectedUnits.flatMap { it.members.map { member -> member.id } }} frozenStart=${frozenB?.startDateInclusive} frozenEnd=${frozenB?.endDateInclusive} frozenIds=${frozenB?.includedSessionIds} sameAsA=${frozenB === previousFrozen}")
        val expected = listOf("T2", "G1", "G2", "G3", "T1", "S1", "F1", "T0")
        val matcher = hasText("确认选择") and hasClickAction()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(matcher)
        val node = compose.onNode(matcher)
        println("CUI02_CLICK matchedNodes=${compose.onAllNodes(matcher).fetchSemanticsNodes().size}")
        val target = node.fetchSemanticsNode()
        val rootBounds = target.boundsInRoot
        val windowBounds = target.boundsInWindow
        val disabled = SemanticsProperties.Disabled in target.config
        println("CUI02_CLICK enabled=${!disabled} disabled=$disabled boundsInRoot=$rootBounds boundsInWindow=$windowBounds expectedCenterInRoot=${rootBounds.center} expectedCenterInWindow=${windowBounds.center} coordinates=physicalPixels_clippedVisibleBounds_rootAndWindowOrigins expectedCenter=sourceCalculated_notCapturedMotionEvent")
        screenshot("CUI02-before-final-confirm.png")
        node.performClick()
        val frozenC = vm().frozenSelection
        val stateC = vm().state
        println("CUI02_DIAG stage=C stateStart=${stateC.startDate} stateEnd=${stateC.endDate} selectedIds=${stateC.selectedUnits.flatMap { it.members.map { member -> member.id } }} frozenStart=${frozenC?.startDateInclusive} frozenEnd=${frozenC?.endDateInclusive} frozenIds=${frozenC?.includedSessionIds} sameAsA=${frozenC === previousFrozen}")
        compose.runOnIdle {
            val stateD = vm().state
            val frozenD = vm().frozenSelection
            println("CUI02_DIAG stage=D stateStart=${stateD.startDate} stateEnd=${stateD.endDate} selectedIds=${stateD.selectedUnits.flatMap { it.members.map { member -> member.id } }} frozenStart=${frozenD?.startDateInclusive} frozenEnd=${frozenD?.endDateInclusive} frozenIds=${frozenD?.includedSessionIds} sameAsA=${frozenD === previousFrozen} sameAsC=${frozenD === frozenC}")
            val actual = requireNotNull(frozenD)
            assertEquals(expected.map { "e21-s03-ui-$it" }, actual.includedSessionIds)
            assertEquals("calendar", actual.source)
            assertTrue(actual.includedUnknownDateSessionIds.isEmpty())
            println("CUI_FROZEN expected=${expected.map { "e21-s03-ui-$it" }} actual=$actual")
            val all = actual
            assertNull(all.startDateInclusive); assertNull(all.endDateInclusive)
        }
        screenshot("CUI02-date-range.png")
    }

    @Test
    fun filtersAndWholeMergeGroupFreezeOriginalIds() = withSelectionFixture("CUI03") {
        openRecords(); enterExport()
        confirmIds(listOf("T2", "G1", "G2", "G3", "T1", "S1", "F1", "T0"))
        clickText("计时")
        val timed = confirmIds(listOf("T2", "G1", "G2", "G3", "T1", "T0"))
        assertEquals(setOf("timed"), timed.modeFilter)
        clickText("力量")
        assertEquals(setOf("timed", "strength"), confirmIds(listOf("T2", "G1", "G2", "G3", "T1", "S1", "T0")).modeFilter)
        clickText("全部模式")
        clickText("计时新名（e21-s03-ui-PT）")
        assertEquals(setOf("e21-s03-ui-PT"), confirmIds(listOf("T2", "G1", "G2", "G3", "T1", "T0")).planFilter)
        clickText("力量历史名（e21-s03-ui-PS）")
        assertEquals(setOf("e21-s03-ui-PT", "e21-s03-ui-PS"), confirmIds(listOf("T2", "G1", "G2", "G3", "T1", "S1", "T0")).planFilter)
        clickText("全部计划")
        val all = confirmIds(listOf("T2", "G1", "G2", "G3", "T1", "S1", "F1", "T0"))
        assertNull(all.modeFilter); assertNull(all.planFilter)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("选择原记录 e21-s03-ui-T0"))
        compose.onNodeWithContentDescription("选择原记录 e21-s03-ui-T0").assertIsOn()
        compose.onNodeWithText("计时旧名").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("选择原记录 e21-s03-ui-T1"))
        compose.onNodeWithContentDescription("选择原记录 e21-s03-ui-T1").assertIsOn()
        assertEquals("计时新名", vm().state.visibleUnits.first { it.key == "e21-s03-ui-T1" }.representative.session!!.planSnapshot.title)
        clickText("审阅原段")
        assertTrue(vm().state.expandedKeys.isNotEmpty())
        for (id in listOf("G1", "G2", "G3")) {
            assertTrue(repository().readSessionStrict("e21-s03-ui-$id") is WorkoutSessionStrictReadResult.CanonicalTerminal)
            assertTrue(compose.onAllNodesWithContentDescription("选择原记录 e21-s03-ui-$id").fetchSemanticsNodes().isEmpty())
        }
        clickDescription("选择合并记录（3段）")
        confirmIds(listOf("T2", "T1", "S1", "F1", "T0"))
        clickDescription("选择原记录 e21-s03-ui-T1")
        confirmIds(listOf("T2", "S1", "F1", "T0"))
        assertTrue(repository().readSessionStrict("e21-s03-ui-T1") is WorkoutSessionStrictReadResult.CanonicalTerminal)
        clickText("计时")
        compose.onNodeWithText("筛选已更新，请重新确认场次").assertIsDisplayed()
        val old = confirmIds(listOf("T2", "G1", "G2", "G3", "T1", "T0"))
        assertEquals("calendar", old.source)
        assertEquals(setOf("timed"), old.modeFilter); assertNull(old.planFilter)
        assertTrue(old.includedUnknownDateSessionIds.isEmpty())
        clickDescription("选择原记录 e21-s03-ui-T1")
        assertEquals(ids("T2", "G1", "G2", "G3", "T1", "T0"), old.includedSessionIds)
        assertSame(old, vm().frozenSelection)
        screenshot("CUI03-frozen-selection.png")
    }

    @Test
    fun selectionSurvivesRecreationAndReloadsAfterAppNavigation() = withSelectionFixture("CUI04", includeNew = true) {
        openRecords(); enterExport()
        showMonth("2026-01"); clickDay("2026-01-01"); clickDay("2026-01-01")
        clickDescription("选择原记录 e21-s03-ui-T1")
        clickText("审阅原段")
        val frozen = confirmIds(listOf("G1", "G2", "G3", "S1", "F1"))
        val before = vm().state
        compose.activityRule.scenario.recreate()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        waitText("导出选择")
        assertEquals(before, vm().state)
        assertSame(frozen, vm().frozenSelection)
        clickDescription("选择原记录 e21-s03-ui-T1", click = false)
        compose.onNodeWithContentDescription("选择原记录 e21-s03-ui-T1").assertIsOff()
        clickText("收起原段", click = false)
        compose.onNodeWithText("收起原段").assertIsDisplayed()
        screenshot("CUI04-recreated-selection.png")
        compose.onNodeWithContentDescription("训练").performClick()
        compose.runOnIdle {
            assertFalse(vm().isSelectionOpen); assertNull(vm().frozenSelection)
            assertTrue(vm().state.units.isEmpty())
        }
        insertSelectionSessions(listOf("N1"))
        openRecords(); enterExport()
        val reentered = confirmIds(listOf("N1", "T2", "G1", "G2", "G3", "T1", "S1", "F1", "T0"))
        assertNull(reentered.startDateInclusive); assertNull(reentered.endDateInclusive)
        assertNull(reentered.modeFilter); assertNull(reentered.planFilter)
        assertFalse(vm().state.visibleUnits.flatMap { it.members }.any { it.id == "e21-s03-ui-I1" })
        clickText("返回记录总览"); assertFalse(vm().isSelectionOpen)
        enterExport(); systemBack(); assertFalse(vm().isSelectionOpen)
        clickText("导出记录", click = false)
        compose.onNodeWithText("导出记录").assertIsDisplayed()
    }

    private fun vm() = compose.activity.workoutSessionExportViewModel
    private fun repository() = (compose.activity.application as TrainFlowApplication).workoutSessionRepository
    private fun ids(vararg names: String) = names.map { "e21-s03-ui-$it" }

    private fun withSelectionFixture(method: String, includeNew: Boolean = false, block: suspend () -> Unit) = runBlocking {
        val database = (compose.activity.application as TrainFlowApplication).trainFlowDatabase
        val dao = database.workoutSessionDao()
        val names = listOf("T0", "T1", "S1", "F1", "T2", "I1", "G1", "G2", "G3")
        val fixtureIds = (names + if (includeNew) listOf("N1") else emptyList()).map { "e21-s03-ui-$it" }.toSet()
        val originalIds = dao.sessionHeaders().map { it.id }.toSet()
        assertTrue(originalIds.intersect(fixtureIds).isEmpty())
        var inserted = false
        var groupId: String? = null
        var failure: Throwable? = null
        try {
            insertSelectionSessions(names)
            inserted = true
            for (name in names) {
                val result = repository().readSessionStrict("e21-s03-ui-$name")
                assertTrue("$name: $result", result is WorkoutSessionStrictReadResult.CanonicalTerminal)
            }
            groupId = repository().createMergeGroup(ids("G1", "G2", "G3").toSet())
            withTimeout(5_000) { repository().historyEntries.first { entries ->
                entries.count { it.id in fixtureIds } == 9 && entries.count { it.mergeGroupId == groupId } == 3
            } }
            println("$method fixtureIds=${names.map { "e21-s03-ui-$it" }} groupId=$groupId originalUserIds=$originalIds")
            block()
        } catch (cause: Throwable) { failure = cause }
        finally {
            try {
                if (inserted) {
                    if (groupId == null) repository().deleteHistorySessions(fixtureIds) else {
                        repository().deleteHistoryItems(setOf(HistoryItemKey.Group(requireNotNull(groupId))))
                        repository().deleteHistorySessions(fixtureIds - ids("G1", "G2", "G3").toSet())
                    }
                }
                assertEquals(originalIds, dao.sessionHeaders().map { it.id }.toSet())
            } catch (cause: Throwable) {
                if (failure == null) failure = cause else requireNotNull(failure).addSuppressed(cause)
            }
        }
        failure?.let { throw it }
        Unit
    }

    private suspend fun insertSelectionSessions(names: List<String>) {
        val database = (compose.activity.application as TrainFlowApplication).trainFlowDatabase
        database.withTransaction {
            for (name in names) check(database.workoutSessionDao().insertSession(selectionHeader(name)) != -1L)
            for (name in names) for (phase in selectionPhases(name)) {
                database.canonicalTimelineHeartRateDao().insertPhaseInterval(phase)
            }
        }
    }

    private fun openRecords() {
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("记录").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithContentDescription("记录").performClick()
    }
    private fun waitText(text: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun clickText(text: String, click: Boolean = true) {
        val matcher = hasText(text) and hasClickAction()
        if (compose.onAllNodes(matcher).fetchSemanticsNodes().isEmpty())
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(matcher)
        val node = compose.onNode(matcher)
        if (!node.isDisplayed()) node.performScrollTo()
        if (click) node.performClick()
    }
    private fun clickDescription(description: String, click: Boolean = true) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription(description))
        val node = compose.onNodeWithContentDescription(description).performScrollTo()
        if (click) node.performClick()
    }
    private fun enterExport() {
        clickText("导出记录")
        compose.waitUntil(5_000) { compose.activity.workoutSessionExportViewModel.isSelectionOpen }
        waitText("导出选择")
    }
    private fun showMonth(month: String) {
        val difference = java.time.temporal.ChronoUnit.MONTHS.between(vm().state.month, YearMonth.parse(month)).toInt()
        repeat(kotlin.math.abs(difference)) { clickText(if (difference < 0) "上个月" else "下个月") }
        compose.onNodeWithText(month).assertIsDisplayed()
    }
    private fun clickDay(date: String) {
        compose.onNodeWithContentDescription(vm().state.dayDescription(LocalDate.parse(date))).performClick()
    }
    private fun assertJanuaryCounts() {
        compose.onNodeWithContentDescription("2026-01-01 计时3，力量1，跟练1，未完成1").assertIsDisplayed()
        compose.onNodeWithContentDescription("2026-01-02 计时1").assertIsDisplayed()
        compose.onNodeWithContentDescription("2026-01-04 无记录").assertIsDisplayed()
    }
    private fun inputDates(fields: List<String>) { clickText("输入起止日期"); replaceDateFields(fields) }
    private fun replaceDateFields(fields: List<String>) {
        listOf("开始年", "开始月", "开始日", "结束年", "结束月", "结束日").forEachIndexed { index, label ->
            compose.onNodeWithText(label).performTextReplacement(fields[index])
        }
    }
    private fun confirmIds(expected: List<String>): WorkoutSessionExportSelection {
        clickText("确认选择")
        val actual = requireNotNull(vm().frozenSelection)
        assertEquals(expected.map { "e21-s03-ui-$it" }, actual.includedSessionIds)
        assertEquals("calendar", actual.source)
        assertTrue(actual.includedUnknownDateSessionIds.isEmpty())
        println("CUI_FROZEN expected=${expected.map { "e21-s03-ui-$it" }} actual=$actual")
        return actual
    }
    private fun systemBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }
    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val file = File("/sdcard/Android/media/${instrumentation.targetContext.packageName}/additional_test_output/", name)
        check(file.parentFile!!.mkdirs() || file.parentFile!!.isDirectory)
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        println("CUI_NATIVE_SCREENSHOT=" + file.absolutePath)
    }

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

    private fun selectionHeader(name: String): WorkoutSessionEntity {
        val id = "e21-s03-ui-$name"
        val (start, day) = when (name) {
            "T0" -> "2025-12-31T15:59:59Z" to "2025-12-31"
            "T1" -> "2026-01-01T02:00:00Z" to "2026-01-01"
            "S1" -> "2026-01-01T01:00:00Z" to "2026-01-01"
            "F1" -> "2026-01-01T00:00:00Z" to "2026-01-01"
            "T2" -> "2026-01-02T02:00:00Z" to "2026-01-02"
            "I1" -> "2026-01-01T03:00:00Z" to "2026-01-01"
            "G1" -> "2026-01-01T10:00:00Z" to "2026-01-01"
            "G2" -> "2026-01-01T11:00:00Z" to "2026-01-01"
            "G3" -> "2026-01-01T12:00:00Z" to "2026-01-01"
            "N1" -> "2026-01-03T02:00:00Z" to "2026-01-03"
            else -> error("Unknown approved fixture")
        }
        val original = when (name) {
            "S1" -> strengthHeader().copy(planId = "e21-s03-ui-PS",
                planSnapshotJson = strengthHeader().planSnapshotJson.replace("plan-old", "e21-s03-ui-PS")
                    .replace("History S", "力量历史名"))
            "F1" -> WorkoutSessionEntity(id = id, mode = "follow_along", status = "completed",
                planSnapshotJson = freeFollowAlongSnapshot().toStorageJson(),
                startedAt = start, endedAt = "2026-01-01T00:00:03Z",
                totalElapsedSec = 3, effectiveElapsedSec = 3, pausedElapsedSec = 0,
                timelineVersion = 1, lastDurableOffsetMs = 3000L, lastMutationSequence = 1L,
                trustedEndOffsetMs = 3000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[]}""",
                startLocalDate = day, startZoneId = "Asia/Shanghai", startUtcOffsetSeconds = 28_800L,
                timeMetadataSourceContractVersion = 1L)
            else -> fixedHeader(id).let { header ->
                header.copy(planSnapshotJson = if (name == "T0") header.planSnapshotJson.replace("计时新名", "计时旧名")
                    else header.planSnapshotJson)
            }
        }
        val interrupted = name in setOf("I1", "G1", "G2")
        val abandoned = interrupted || name == "T1"
        return original.copy(id = id, startedAt = start, startLocalDate = day,
            startZoneId = "Asia/Shanghai", startUtcOffsetSeconds = 28_800L,
            status = if (abandoned) "abandoned" else "completed",
            terminalReason = if (interrupted) "process_interrupted" else if (abandoned) "user_abandoned" else "completed",
            endedAt = if (abandoned) null else Instant.parse(start).plusSeconds(original.totalElapsedSec!!.toLong()).toString())
    }

    private fun selectionPhases(name: String): List<WorkoutPhaseIntervalEntity> {
        val id = "e21-s03-ui-$name"
        val original = when (name) {
            "S1" -> strengthPhases()
            "F1" -> {
                val prepared = (PlanSnapshotStorageV1Validator.prepare(freeFollowAlongSnapshot().toStorageJson(),
                    WorkoutMode.FOLLOW_ALONG) as PreparedPlanSnapshotStorageV1Result.Valid).prepared
                listOf(WorkoutPhaseIntervalEntity("s-F0:p:0", "s-F0", 0, 0L, 3000L, 0L, 1L, null,
                    "follow_along_action", freeFollowAlongPhase(prepared).phaseIdentityJson))
            }
            else -> fixedPhases()
        }
        return original.map { it.copy(id = "$id:p:${it.sequence}", sessionId = id) }
    }

    // Literal S header/phases copied from accepted WorkoutSessionExportTest, without HR rows.
    private fun strengthHeader(): WorkoutSessionEntity = WorkoutSessionEntity(
                id = "s-S", planId = "plan-old", mode = "strength", status = "completed",
                planSnapshotJson = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History S","mode":"strength","blocks":[{"id":"sa","kind":"strength_exercise","title":"Strength block old","order":0,"exerciseId":"ex-sp","sets":[{"id":"set-a","order":0,"kind":"working"}],"substitutions":["ex-sa"],"setTimerMode":"manual_start"}],"preferences":null,"followAlong":null}""",
                startedAt = "2026-09-13T00:00:00Z", endedAt = "2026-09-13T00:00:06Z",
                totalElapsedSec = 6, effectiveElapsedSec = 4, pausedElapsedSec = 2,
                timelineVersion = 1, lastDurableOffsetMs = 6000L, lastMutationSequence = 5L,
                trustedEndOffsetMs = 6000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-sp","displayNameAtFirstReference":"Planned name","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-sa","displayNameAtFirstReference":"Actual base","customNameAtFirstReference":"Actual custom","resolutionSource":"runtime_substitution"}]}""",
                startLocalDate = "2026-09-13", startZoneId = "UTC", startUtcOffsetSeconds = 0L,
                timeMetadataSourceContractVersion = 1L
            )

    private fun strengthPhases(): List<WorkoutPhaseIntervalEntity> = listOf(
                WorkoutPhaseIntervalEntity("s-S:p:0", "s-S", 0, 0L, 1000L, 0L, 1L, null, "strength_prepare_set", """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_prepare_set","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"prepare_set","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}"""),
                WorkoutPhaseIntervalEntity("s-S:p:1", "s-S", 1, 1000L, 2000L, 1L, 2L, null, "strength_active_set", """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_active_set","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"active_set","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}"""),
                WorkoutPhaseIntervalEntity("s-S:p:2", "s-S", 2, 2000L, 3000L, 2L, 3L, null, "strength_confirm_set", """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_confirm_set","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"confirm_set","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}"""),
                WorkoutPhaseIntervalEntity("s-S:p:3", "s-S", 3, 3000L, 4000L, 3L, 4L, null, "strength_rest", """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"strength_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"rest","blockId":"sa","setPlanId":"set-a","plannedExerciseId":"ex-sp","actualExerciseId":"ex-sa","exerciseSetIndex0":0,"globalSetIndex0":0,"setKind":"working","substitutedFromExerciseId":"ex-sp"}}"""),
                WorkoutPhaseIntervalEntity("s-S:p:4", "s-S", 4, 4000L, 6000L, 4L, 5L, null, "paused", """{"phaseIdentityContractVersion":1,"family":"strength_v1","payloadVersion":1,"mode":"strength","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"c15e75e2fe6c0dbb4ec88ab90ac5cfb5f53a3889d6c06e29aa174c45ce210cb5"},"payload":{"variant":"paused","blockId":null,"setPlanId":null,"plannedExerciseId":null,"actualExerciseId":null,"exerciseSetIndex0":null,"globalSetIndex0":null,"setKind":null,"substitutedFromExerciseId":null}}""")
            )

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
