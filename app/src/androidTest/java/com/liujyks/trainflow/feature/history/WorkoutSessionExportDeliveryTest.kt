package com.liujyks.trainflow.feature.history

import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.room.withTransaction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.liujyks.trainflow.app.MainActivity
import com.liujyks.trainflow.app.TrainFlowApplication
import com.liujyks.trainflow.core.data.*
import com.liujyks.trainflow.core.database.CanonicalTuple
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.feature.workoutsession.freeFollowAlongPhase
import com.liujyks.trainflow.feature.workoutsession.freeFollowAlongSnapshot
import java.io.File
import java.io.StringReader
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutSessionExportDeliveryTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val application get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TrainFlowApplication
    private val repository get() = application.workoutSessionRepository
    private val database get() = application.trainFlowDatabase
    private val smallNames = listOf("T1", "S1", "F1", "G1", "G2", "G3")
    private val smallBatchNames = listOf("G1", "G2", "G3", "T1", "S1", "F1")
    private fun smallIds(names: List<String>) = names.map { "e21-s04-small-$it" }
    private fun lifeIds() = (0..99).map { "e21-s04-life-%03d".format(it) }
    private fun vm(): WorkoutSessionExportViewModel {
        lateinit var result: WorkoutSessionExportViewModel
        scenario.onActivity { result = it.workoutSessionExportViewModel }
        return result
    }
    private fun generationJob(): Job? = WorkoutSessionExportViewModel::class.java.getDeclaredField("generationJob")
        .apply { isAccessible = true }.get(vm()) as Job?

    @Test
    fun singleSaveUsesInternalDefaultAndExternalTree() = withFixture("singleSaveUsesInternalDefaultAndExternalTree", false) {
        openSingle()
        s04Tap("保存", capture = true); s04Await("保存位置")
        assertNull(vm().delivery.target); s04Screenshot("A01-default-position")
        s04Tap("选择其他目录", capture = true)
        pickerPackage()
        s04Key(android.view.KeyEvent.KEYCODE_BACK, capture = true)
        s04Await("保存位置")
        assertNull(vm().delivery.prepared); assertNull(vm().delivery.message)
        assertEquals(SessionExportStage.Location, vm().delivery.stage)
        s04Tap("确定", capture = true); waitResult()
        val internal = requireNotNull(vm().delivery.prepared)
        assertEquals(1, vm().delivery.processed); assertEquals(1, vm().delivery.total)
        assertEquals("已生成到App内部导出缓存", vm().delivery.message)
        assertSmall(internal.file.readBytes(), listOf("T1"), "single_session", internal.generatedAt)
        s04Screenshot("A01-internal-result")
        returnSingle(); assertTrue(internal.file.isFile)
        s04Tap("保存", capture = true); s04Await("保存位置"); assertNull(vm().delivery.target)
        chooseDirectory("success", "A01-success-root")
        s04Tap("确定", capture = true); waitResult()
        assertEquals("已保存", vm().delivery.message)
        val external = requireNotNull(vm().delivery.prepared)
        assertFalse(external.file.exists())
        assertSmall(readExternal(external.file.name), listOf("T1"), "single_session", external.generatedAt)
        s04Screenshot("A01-external-result")
        returnSingle(); assertNotNull(s04Find("e21-s04-small-T1"))
    }

    @Test
    fun batchSaveFailureRetainsFileForOneManualRetry() = withFixture("batchSaveFailureRetainsFileForOneManualRetry", false) {
        openBatch("2026-01-01")
        s04Tap("保存", capture = true); s04Await("保存位置")
        assertEquals(smallIds(smallBatchNames), vm().frozenSelection!!.includedSessionIds)
        chooseDirectory("success")
        s04Tap("确定", capture = true); waitResult()
        assertEquals("已保存", vm().delivery.message)
        val saved = requireNotNull(vm().delivery.prepared)
        assertSmall(readExternal(saved.file.name), smallBatchNames, "calendar", saved.generatedAt)
        s04Return()
        openBatch("2026-01-01")
        s04Tap("保存", capture = true); s04Await("保存位置")
        chooseDirectory("failure", "A02-failure-root")
        s04Tap("确定", capture = true); waitResult()
        val ready = requireNotNull(vm().delivery.prepared)
        val bytes = ready.file.readBytes()
        val selection = vm().frozenSelection
        assertSmall(bytes, smallBatchNames, "calendar", ready.generatedAt)
        assertTrue(vm().delivery.canRetrySave); assertNull(vm().delivery.message)
        assertTrue(requireNotNull(vm().delivery.cause).message.orEmpty().contains("E21-S04 fixed open failure"))
        s04Screenshot("A02-first-failure")
        println("S04_READY operationId=${ready.operationId} relativePath=files/session-exports/ready/${ready.file.name} size=${bytes.size} sha256=${hash(bytes)} generatedAt=${ready.generatedAt}")
        s04Tap("重试保存", capture = true); waitResult()
        assertEquals(ready, vm().delivery.prepared); assertSame(selection, vm().frozenSelection)
        assertArrayEquals(bytes, ready.file.readBytes()); assertEquals(ready.generatedAt, vm().delivery.prepared!!.generatedAt)
        assertNull(vm().delivery.message); assertTrue(vm().delivery.canRetrySave)
        assertTrue(requireNotNull(vm().delivery.cause).message.orEmpty().contains("E21-S04 fixed open failure"))
        s04Screenshot("A02-retry-result")
        println("S04_HOST_REQUIRED provider failure create/open=2; exact ledger=files/e21-s04-documents/ledger.jsonl")
    }

    @Test
    fun shareUsesReadOnlyExternalReceiverAndDoesNotReplay() = withFixture("shareUsesReadOnlyExternalReceiverAndDoesNotReplay", false) {
        openSingle(); s04Tap("分享", capture = true)
        acceptShare(1, "A03-single-chooser", "A03-single-receiver")
        val single = requireNotNull(vm().delivery.prepared)
        assertSmall(single.file.readBytes(), listOf("T1"), "single_session", single.generatedAt)
        logShared(single)
        assertEquals("已交给系统分享", vm().delivery.message)
        scenario.recreate(); s04Await("导出结果")
        assertNull(vm().delivery.pendingShare); assertTrue(single.file.isFile)
        returnSingle(); s04Tap("返回记录总览", capture = true)
        openBatch("2026-01-01"); s04Tap("分享", capture = true)
        acceptShare(2, receiverScreenshot = "A03-batch-receiver")
        val batch = requireNotNull(vm().delivery.prepared)
        assertSmall(batch.file.readBytes(), smallBatchNames, "calendar", batch.generatedAt)
        logShared(batch); s04Return()
        openBatch("2026-01-01"); s04Tap("分享", capture = true)
        val chooser = chooserPackage()
        s04Key(android.view.KeyEvent.KEYCODE_BACK, capture = true)
        s04Await("导出结果")
        assertEquals("已交给系统分享", vm().delivery.message)
        val cancelled = requireNotNull(vm().delivery.prepared)
        logShared(cancelled)
        scenario.recreate(); s04Await("导出结果")
        assertNull(vm().delivery.pendingShare)
        assertTrue(single.file.isFile); assertTrue(batch.file.isFile); assertTrue(cancelled.file.isFile)
        s04Screenshot("A03-cancel-result"); s04Return()
        println("S04_HOST_REQUIRED receiver-count=2 package=$chooser counter=files/e21-s04-evidence/receiver-count.txt")
    }

    @Test
    fun generationReattachesAndContinuesWhileProcessLives() = withFixture("generationReattachesAndContinuesWhileProcessLives", true) {
        startLifeSave()
        val firstVm = vm(); val firstJob = activeLifeJob(); val firstSelection = vm().frozenSelection
        val part = operationPart()
        scenario.recreate(); s04Await("正在导出")
        assertSame(firstVm, vm()); assertSame(firstJob, generationJob()); assertSame(firstSelection, vm().frozenSelection)
        assertTrue("UNPROVEN: generation finished before recreation observation", firstJob.isActive)
        s04Screenshot("A04-recreated")
        finishLife(firstJob, part); s04Return()
        startLifeSave()
        val homeJob = activeLifeJob(); val homeSelection = vm().frozenSelection; val homePart = operationPart()
        s04Key(android.view.KeyEvent.KEYCODE_HOME, capture = true)
        s04Key(android.view.KeyEvent.KEYCODE_APP_SWITCH)
        s04Tap("TrainFlow", capture = true, packageName = externalPackage())
        assertSame(firstVm, vm()); assertSame(homeJob, generationJob()); assertSame(homeSelection, vm().frozenSelection)
        s04Screenshot("A04-home-return")
        finishLife(homeJob, homePart); s04Return()
        startLifeSave()
        val lockJob = activeLifeJob(); val lockSelection = vm().frozenSelection; val lockPart = operationPart()
        s04Key(android.view.KeyEvent.KEYCODE_POWER, capture = true)
        s04Key(android.view.KeyEvent.KEYCODE_WAKEUP)
        if (s04Automation.rootInActiveWindow.packageName?.toString() != s04Package) s04Swipe(true, externalPackage())
        assertSame(firstVm, vm()); assertSame(lockJob, generationJob()); assertSame(lockSelection, vm().frozenSelection)
        s04Screenshot("A04-unlocked")
        finishLife(lockJob, lockPart); s04Return()
    }

    @Test
    fun appNavigationAndBackCancelActiveGeneration() = withFixture("appNavigationAndBackCancelActiveGeneration", true) {
        startLifeSave()
        val navigationJob = activeLifeJob(); val navigationPart = operationPart()
        s04Tap("设置", capture = true)
        assertFalse(vm().isExportFlowOpen); assertNull(vm().frozenSelection); assertTrue(vm().state.units.isEmpty())
        withTimeout(120_000) { navigationJob.join() }
        assertTrue(navigationJob.isCancelled); assertCancelledOperation(navigationPart)
        s04Screenshot("A05-navigation")
        s04Tap("记录"); startLifeSave()
        val backJob = activeLifeJob(); val backPart = operationPart()
        s04Key(android.view.KeyEvent.KEYCODE_BACK, capture = true); s04Await("导出记录")
        assertFalse(vm().isExportFlowOpen); assertNull(vm().frozenSelection)
        withTimeout(120_000) { backJob.join() }
        assertTrue(backJob.isCancelled); assertCancelledOperation(backPart)
        s04Screenshot("A05-back")
        s04Enter(); assertNull(vm().frozenSelection)
        assertNull(vm().state.startDate); assertNull(vm().state.modeFilter); assertNull(vm().state.planFilter)
        s04Return()
    }

    @Test
    fun prepareProcessDeathFixture() = runBlocking {
        s04Method = "prepareProcessDeathFixture"
        val original = database.workoutSessionDao().sessionHeaders().map { it.id }.toSet()
        assertTrue(original.intersect(lifeIds().toSet()).isEmpty())
        seedLife()
        saveFixtureLedger("process-death-fixture.json", original, lifeIds(), null)
        println("S04_D01 fixtureIds=${lifeIds()} originalUserIds=$original ownerReleased=true")
    }

    @Test
    fun inspectProcessDeathNoReplay() = runBlocking {
        s04Method = "inspectProcessDeathNoReplay"
        val ledger = readFixtureLedger("process-death-fixture.json")
        scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            s04Tap("记录"); s04Await("导出记录")
            assertFalse(vm().isExportFlowOpen); assertNull(vm().frozenSelection)
            assertNull(generationJob()); assertNull(vm().delivery.pendingShare)
            assertEquals(ledger.getJSONArray("fixtureIds").strings().toSet(),
                database.workoutSessionDao().sessionHeaders().map { it.id }.filter { it.startsWith("e21-s04-life-") }.toSet())
            val operationId = ledger.getString("operationId")
            assertFalse(File(application.filesDir, "session-exports/incomplete/export-$operationId.json.part").exists())
            assertFalse(File(application.filesDir, "session-exports/ready/export-$operationId.json").exists())
            assertFalse(File(application.filesDir, "session-exports/shared").listFiles().orEmpty()
                .any { it.name.endsWith("$operationId.json") })
            s04Screenshot("D02-new-process-records")
            println("S04_D02 pid=${android.os.Process.myPid()} independentInstrumentationProcess=true; H01 evidence must precede this run")
        } finally { scenario.close() }
    }

    @Test
    fun cleanupProcessDeathFixture() = runBlocking {
        s04Method = "cleanupProcessDeathFixture"
        cleanupPersistentFixture("process-death-fixture.json")
    }

    @Test
    fun prepareManualFixture() = runBlocking {
        s04Method = "prepareManualFixture"
        assertManualComponentsDisabled()
        val original = database.workoutSessionDao().sessionHeaders().map { it.id }.toSet()
        assertTrue(original.intersect(smallIds(smallNames).toSet()).isEmpty())
        val group = seedSmall()
        saveFixtureLedger("manual-fixture.json", original, smallIds(smallNames), group)
        for (name in smallNames) {
            val header = smallHeader(name)
            println("S04_B01 id=${header.id} title=${JSONObject(header.planSnapshotJson).getString("title")} startedAt=${header.startedAt} day=${header.startLocalDate} groupId=$group")
        }
        println("S04_B01 originalUserIds=$original componentsDisabled=true productUiOperations=0")
    }

    @Test
    fun cleanupManualFixture() = runBlocking {
        s04Method = "cleanupManualFixture"
        assertManualComponentsDisabled()
        cleanupPersistentFixture("manual-fixture.json")
    }

    private fun openSingle() {
        s04Tap("记录")
        s04Tap("S04测试计时")
        s04Await("单条记录详情")
        assertNotNull(s04Find("保存")); assertNotNull(s04Find("分享"))
        assertNotNull(s04Find("e21-s04-small-T1"))
    }
    private fun returnSingle() {
        s04Tap("返回原记录详情", capture = true, scrollUp = false)
        s04Await("单条记录详情")
        assertNotNull(s04Find("e21-s04-small-T1"))
    }
    private fun openBatch(date: String) {
        if (s04Find("导出记录") == null) s04Tap("记录")
        s04Enter()
        val day = LocalDate.parse(date)
        s04Dates(listOf(day.year.toString(), day.monthValue.toString(), day.dayOfMonth.toString(),
            day.year.toString(), day.monthValue.toString(), day.dayOfMonth.toString()))
        s04Tap("应用日期")
    }
    private fun waitResult() {
        compose.waitUntil(120_000) { vm().delivery.stage == SessionExportStage.Result }
        s04Await("导出结果")
    }
    private fun externalPackage(): String = requireNotNull(s04Automation.rootInActiveWindow.packageName).toString()
        .also { check(it != s04Package) { "Expected an actual external system window" } }
    private fun pickerPackage(): String {
        compose.waitUntil(5_000) { s04Automation.rootInActiveWindow?.packageName?.toString()?.let { it != s04Package } == true }
        return externalPackage()
    }
    private fun chooserPackage(): String {
        compose.waitUntil(120_000) { vm().delivery.shareClaimed }
        return pickerPackage()
    }
    private fun chooseDirectory(root: String, screenshot: String? = null) {
        s04Tap("选择其他目录", capture = true)
        val system = pickerPackage()
        val label = if (root == "success") "E21-S04 成功目标" else "E21-S04 失败目标"
        if (s04Find(label, system) == null) {
            val rootsButton = s04Find("Show roots", system) ?: s04Find("显示根目录", system)
            s04Touch(requireNotNull(rootsButton) { "System directory roots control is not visible" }, capture = true)
        }
        s04Tap(label, capture = true, packageName = system)
        screenshot?.let(::s04Screenshot)
        s04Touch(s04Await("action_menu_select", system, byId = true), capture = true)
        s04Touch(s04Await("button1", system, byId = true), capture = true)
        s04Await("保存位置")
        assertEquals(root, DocumentsContract.getTreeDocumentId(requireNotNull(vm().delivery.target)))
    }
    private fun readExternal(displayName: String): ByteArray {
        val tree = requireNotNull(vm().delivery.target)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val matches = mutableListOf<String>()
        requireNotNull(application.contentResolver.query(children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null)).use { cursor ->
            while (cursor.moveToNext()) if (cursor.getString(1) == displayName) matches += cursor.getString(0)
        }
        val id = matches.single()
        val document = DocumentsContract.buildDocumentUriUsingTree(tree, id)
        val bytes = requireNotNull(application.contentResolver.openInputStream(document)).use { it.readBytes() }
        assertTrue(bytes.isNotEmpty())
        println("S04_EXTERNAL uri=$document relativePath=files/e21-s04-documents/$id size=${bytes.size} sha256=${hash(bytes)}")
        return bytes
    }
    private fun acceptShare(count: Int, chooserScreenshot: String? = null, receiverScreenshot: String) {
        val chooser = chooserPackage()
        chooserScreenshot?.let(::s04Screenshot)
        s04Tap("E21-S04 文件接收", capture = true, packageName = chooser)
        val testPackage = InstrumentationRegistry.getInstrumentation().context.packageName
        s04Await("返回App", testPackage)
        val receipt = s04Automation.windows.mapNotNull { it.root }.filter { it.packageName?.toString() == testPackage }
            .flatMap(::s04Nodes).mapNotNull { it.text?.toString() }.single { it.startsWith("E21-S04 文件接收\n") }
        assertTrue(receipt.contains("readSuccess=true")); assertTrue(receipt.contains("writeDenied=true"))
        assertTrue(receipt.contains("count=$count\n"))
        val receiverUid = receipt.lineSequence().single { it.startsWith("receiverUid=") }.substringAfter('=').toInt()
        val targetUid = receipt.lineSequence().single { it.startsWith("targetUid=") }.substringAfter('=').toInt()
        assertNotEquals(targetUid, receiverUid)
        receiverScreenshot.let(::s04Screenshot)
        s04Tap("返回App", capture = true, packageName = testPackage); s04Await("导出结果")
    }
    private fun logShared(export: PreparedWorkoutSessionExport) {
        val bytes = export.file.readBytes()
        println("S04_SHARED operationId=${export.operationId} relativePath=files/session-exports/shared/${export.file.name} size=${bytes.size} sha256=${hash(bytes)} receiverRelativePath=files/e21-s04-evidence/${export.operationId}/received.json receiptRelativePath=files/e21-s04-evidence/${export.operationId}/receiver.json")
    }
    private fun startLifeSave() {
        openBatch("2026-08-31")
        s04Tap("保存", capture = true); s04Await("保存位置")
        assertEquals(lifeIds().reversed(), vm().frozenSelection!!.includedSessionIds)
        s04Tap("确定", capture = true)
    }
    private fun activeLifeJob(): Job {
        val job = requireNotNull(generationJob())
        assertTrue("UNPROVEN: no real active generation window", job.isActive)
        assertTrue(vm().delivery.processed in 0..99); assertEquals(100, vm().delivery.total)
        println("S04_ACTIVE job=${System.identityHashCode(job)} vm=${System.identityHashCode(vm())} processed=${vm().delivery.processed}/100")
        return job
    }
    private fun operationPart(): File = requireNotNull(File(application.filesDir, "session-exports/incomplete").listFiles())
        .single { it.name.startsWith("export-") && it.name.endsWith(".json.part") }
        .also { println("S04_PART=${it.name}") }
    private fun finishLife(job: Job, part: File) {
        compose.waitUntil(120_000) { job.isCompleted }
        assertFalse(job.isCancelled); assertSame(job, generationJob())
        assertEquals(100, vm().delivery.processed); assertEquals(100, vm().delivery.total)
        assertEquals("已生成到App内部导出缓存", vm().delivery.message)
        val export = requireNotNull(vm().delivery.prepared)
        assertEquals(part.name.removePrefix("export-").removeSuffix(".json.part"), export.operationId)
        assertCompleteLife(export.file); assertFalse(part.exists())
    }
    private fun assertCancelledOperation(part: File) {
        assertFalse(part.exists())
        val id = part.name.removePrefix("export-").removeSuffix(".json.part")
        assertFalse(File(application.filesDir, "session-exports/ready/export-$id.json").exists())
        assertTrue(File(application.filesDir, "session-exports/shared").listFiles().orEmpty().none { it.name.endsWith("$id.json") })
    }
    private fun assertCompleteLife(file: File) {
        val ids = mutableListOf<String>()
        android.util.JsonReader(file.bufferedReader(Charsets.UTF_8)).use { reader ->
            reader.isLenient = false
            reader.beginObject(); assertEquals("trainFlowSessionExport", reader.nextName()); reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "exportContractVersion" -> assertEquals(2, reader.nextInt())
                    "sessions" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                if (reader.nextName() == "session") {
                                    reader.beginObject()
                                    while (reader.hasNext()) if (reader.nextName() == "sessionId") ids += reader.nextString() else reader.skipValue()
                                    reader.endObject()
                                } else reader.skipValue()
                            }
                            reader.endObject()
                        }
                        reader.endArray()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject(); reader.endObject()
            assertEquals(android.util.JsonToken.END_DOCUMENT, reader.peek())
        }
        assertEquals(lifeIds().reversed(), ids)
    }
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }


    private fun withFixture(method: String, life: Boolean, block: suspend () -> Unit) = runBlocking {
        s04Method = method
        val original = database.workoutSessionDao().sessionHeaders().map { it.id }.toSet()
        val ids = (if (life) lifeIds() else smallIds(smallNames)).toSet()
        assertTrue(original.intersect(ids).isEmpty())
        var group: String? = null
        var failure: Throwable? = null
        try {
            if (life) seedLife() else group = seedSmall()
            println("S04_FIXTURE method=$method fixtureIds=$ids groupId=$group originalUserIds=$original")
            scenario = ActivityScenario.launch(MainActivity::class.java)
            block()
        } catch (cause: Throwable) { failure = cause }
        finally {
            try {
                if (::scenario.isInitialized) scenario.close()
                deleteFixture(ids, group)
                assertEquals(original, database.workoutSessionDao().sessionHeaders().map { it.id }.toSet())
            } catch (cause: Throwable) {
                if (failure == null) failure = cause else requireNotNull(failure).addSuppressed(cause)
            }
        }
        failure?.let { throw it }
        Unit
    }

    private suspend fun seedSmall(): String {
        database.withTransaction {
            for (name in smallNames) check(database.workoutSessionDao().insertSession(smallHeader(name)) != -1L)
            for (name in smallNames) for (phase in smallPhases(name)) {
                database.canonicalTimelineHeartRateDao().insertPhaseInterval(phase)
            }
        }
        return repository.createMergeGroup(smallIds(listOf("G1", "G2", "G3")).toSet())
    }

    private suspend fun seedLife() {
        for (index in 0..99) {
            val mode = when (index) { in 0..33 -> "timed"; in 34..66 -> "strength"; else -> "follow_along" }
            val phases = when (index) { in 0..50 -> 149; in 51..66 -> 148; else -> 1 }
            seedLifeProfile(Profile(lifeIds()[index], mode, phases, 100, 2500), index)
        }
    }

    private fun fixtureLedger(name: String) = File(application.filesDir, "e21-s04-fixtures/$name")
    private fun saveFixtureLedger(name: String, original: Set<String>, ids: List<String>, group: String?) {
        val file = fixtureLedger(name)
        check(file.parentFile!!.mkdirs() || file.parentFile!!.isDirectory)
        check(file.createNewFile())
        file.writeText(JSONObject().put("originalUserIds", JSONArray(original.sorted()))
            .put("fixtureIds", JSONArray(ids)).put("groupId", group ?: JSONObject.NULL).toString(), Charsets.UTF_8)
        println("S04_FIXTURE_LEDGER=files/e21-s04-fixtures/$name")
    }
    private fun readFixtureLedger(name: String) = JSONObject(fixtureLedger(name).readText(Charsets.UTF_8))
    private suspend fun deleteFixture(ids: Set<String>, group: String?) {
        if (group != null) {
            repository.deleteHistoryItems(setOf(HistoryItemKey.Group(group)))
            repository.deleteHistorySessions(ids - smallIds(listOf("G1", "G2", "G3")).toSet())
        } else repository.deleteHistorySessions(ids)
    }
    private suspend fun cleanupPersistentFixture(name: String) {
        val ledger = readFixtureLedger(name)
        deleteFixture(ledger.getJSONArray("fixtureIds").strings().toSet(),
            if (ledger.isNull("groupId")) null else ledger.getString("groupId"))
        assertEquals(ledger.getJSONArray("originalUserIds").strings().toSet(),
            database.workoutSessionDao().sessionHeaders().map { it.id }.toSet())
        check(fixtureLedger(name).delete())
        println("S04_FIXTURE_CLEANUP name=$name originalUserIdsPreserved=true")
    }
    private fun assertManualComponentsDisabled() {
        val context = s04Instrumentation.context
        val manager = context.packageManager
        for (name in listOf(ExportShareReceiverActivity::class.java.name, ExportDocumentsProvider::class.java.name)) {
            val component = android.content.ComponentName(context.packageName, name)
            val state = manager.getComponentEnabledSetting(component)
            val enabled = if (state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                if (name == ExportShareReceiverActivity::class.java.name)
                    manager.getActivityInfo(component, android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS).enabled
                else manager.getProviderInfo(component, android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS).enabled
            } else state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            assertFalse("$component must be disabled on B", enabled)
        }
    }

    private fun assertSmall(bytes: ByteArray, names: List<String>, source: String, generatedAt: Instant) {
        val text = bytes.toString(Charsets.UTF_8)
        android.util.JsonReader(StringReader(text)).use {
            it.isLenient = false
            it.skipValue()
            assertEquals(android.util.JsonToken.END_DOCUMENT, it.peek())
        }
        val outer = JSONObject(text)
        assertEquals(setOf("trainFlowSessionExport"), outer.keys().asSequence().toSet())
        val root = outer.getJSONObject("trainFlowSessionExport")
        assertEquals(2, root.getInt("exportContractVersion"))
        val millis = DateTimeFormatterBuilder().appendInstant(3).toFormatter()
        assertEquals(millis.format(generatedAt), root.getString("generatedAt"))
        val locale = application.resources.configuration.locales[0].toLanguageTag()
        assertEquals(locale, root.getString("displayLocale"))
        assertEquals(1, root.getInt("displayContractVersion"))
        val selection = JSONObject().put("includedSessionIds", JSONArray(smallIds(names)))
            .put("includedUnknownDateSessionIds", JSONArray()).put("source", source)
            .put("selectionScope", "user_selected_subset")
            .put("startDateInclusive", if (source == "calendar") "2026-01-01" else JSONObject.NULL)
            .put("endDateInclusive", if (source == "calendar") "2026-01-01" else JSONObject.NULL)
            .put("modeFilter", JSONObject.NULL).put("planFilter", JSONObject.NULL)
        assertJson(selection, root.getJSONObject("selection"), "$.selection")
        val sessions = root.getJSONArray("sessions")
        assertEquals(names.size, sessions.length())
        for ((index, name) in names.withIndex()) {
            val header = smallHeader(name)
            val item = sessions.getJSONObject(index)
            val expected = JSONObject().put("sessionId", header.id).put("planId", header.planId ?: JSONObject.NULL)
                .put("mode", header.mode).put("terminalStatus", header.status).put("timelineStatus", "canonical_v1")
                .put("timelineVersion", 1).put("trustedEndOffsetMs", header.trustedEndOffsetMs)
                .put("canonicalSessionDurationMs", header.trustedEndOffsetMs).put("terminalReason", header.terminalReason)
                .put("startedAt", header.startedAt).put("endedAt", header.endedAt ?: JSONObject.NULL)
                .put("planSnapshotJson", header.planSnapshotJson).put("planSnapshotStorageContractVersion", 1)
                .put("displayMetadata", JSONObject(requireNotNull(header.sessionDisplayMetadataJson)))
                .put("timeMetadata", JSONObject().put("sourceContractVersion", 1).put("startLocalDate", "2026-01-01")
                    .put("startZoneId", "Asia/Shanghai").put("startUtcOffsetSeconds", 28800)
                    .put("startTimestampBasis", "observed")
                    .put("endTimestampBasis", if (header.endedAt == null) "unknown" else "observed")
                    .put("trustedEndAtBasis", "start_anchor_plus_trusted_offset")
                    .put("trustedEndAtUtc", millis.format(Instant.parse(header.startedAt).plusMillis(header.trustedEndOffsetMs!!))))
            assertJson(expected, item.getJSONObject("session"), "$.sessions[$index].session")
            val execution = item.getJSONObject("execution")
            for (field in listOf("sessionStepRecords", "timedRestExtensions", "strengthSetRecords"))
                assertEquals("$.sessions[$index].execution.$field", 0, execution.getJSONArray(field).length())
            val actualPhases = execution.getJSONArray("phases")
            val expectedPhases = smallPhases(name)
            assertEquals(expectedPhases.size, actualPhases.length())
            for ((phaseIndex, phase) in expectedPhases.withIndex()) {
                val actual = actualPhases.getJSONObject(phaseIndex)
                val expectedPhase = JSONObject().put("sequence", phase.sequence).put("startOffsetMs", phase.startOffsetMs)
                    .put("endOffsetMs", phase.endOffsetMs).put("startMutationSequence", phase.startMutationSequence)
                    .put("endMutationSequence", phase.endMutationSequence).put("phaseKind", phase.phaseKind)
                    .put("phaseIdentity", JSONObject(phase.phaseIdentityJson))
                for (field in expectedPhase.keys()) assertJson(expectedPhase.get(field), actual.get(field),
                    "$.sessions[$index].execution.phases[$phaseIndex].$field")
                val display = actual.getJSONObject("display")
                assertEquals(1, display.getInt("displayContractVersion"))
                assertEquals(locale, display.getString("locale"))
            }
            assertJson(JSONObject().put("status", "not_recorded").put("recording", JSONObject.NULL)
                .put("intentAndAcquisition", JSONArray()).put("samples", JSONArray())
                .put("originalAnalysis", JSONObject.NULL).put("durationAudit", JSONObject.NULL),
                item.getJSONObject("heartRate"), "$.sessions[$index].heartRate")
        }
    }
    private fun assertJson(expected: Any, actual: Any, path: String) {
        when (expected) {
            is JSONObject -> {
                assertTrue(path, actual is JSONObject)
                actual as JSONObject
                assertEquals(path, expected.keys().asSequence().toSet(), actual.keys().asSequence().toSet())
                for (key in expected.keys()) assertJson(expected.get(key), actual.get(key), "$path.$key")
            }
            is JSONArray -> {
                assertTrue(path, actual is JSONArray)
                actual as JSONArray
                assertEquals(path, expected.length(), actual.length())
                for (index in 0 until expected.length()) assertJson(expected.get(index), actual.get(index), "$path[$index]")
            }
            is Number -> { assertTrue(path, actual is Number); assertEquals(path, expected.toLong(), (actual as Number).toLong()) }
            else -> assertEquals(path, expected, actual)
        }
    }
    private fun smallHeader(name: String): WorkoutSessionEntity {
        val id = "e21-s04-small-$name"
        val (start, day) = when (name) {
            "T1" -> "2026-01-01T02:00:00Z" to "2026-01-01"
            "S1" -> "2026-01-01T01:00:00Z" to "2026-01-01"
            "F1" -> "2026-01-01T00:00:00Z" to "2026-01-01"
            "G1" -> "2026-01-01T10:00:00Z" to "2026-01-01"
            "G2" -> "2026-01-01T11:00:00Z" to "2026-01-01"
            "G3" -> "2026-01-01T12:00:00Z" to "2026-01-01"
            else -> error("Unknown approved fixture")
        }
        val original = when (name) {
            "S1" -> strengthHeader().copy(planId = "e21-s04-small-PS",
                planSnapshotJson = strengthHeader().planSnapshotJson.replace("plan-old", "e21-s04-small-PS")
                    .replace("History S", "S04测试力量"))
            "F1" -> WorkoutSessionEntity(id = id, mode = "follow_along", status = "completed",
                planSnapshotJson = freeFollowAlongSnapshot().toStorageJson(),
                startedAt = start, endedAt = "2026-01-01T00:00:03Z",
                totalElapsedSec = 3, effectiveElapsedSec = 3, pausedElapsedSec = 0,
                timelineVersion = 1, lastDurableOffsetMs = 3000L, lastMutationSequence = 1L,
                trustedEndOffsetMs = 3000L, terminalReason = "completed", displayMetadataContractVersion = 1,
                sessionDisplayMetadataJson = """{"displayMetadataContractVersion":1,"entries":[]}""",
                startLocalDate = day, startZoneId = "Asia/Shanghai", startUtcOffsetSeconds = 28_800L,
                timeMetadataSourceContractVersion = 1L)
            else -> fixedHeader(id)
        }
        val interrupted = name in setOf("G1", "G2")
        val abandoned = interrupted || name == "T1"
        return original.copy(id = id, startedAt = start, startLocalDate = day,
            startZoneId = "Asia/Shanghai", startUtcOffsetSeconds = 28_800L,
            status = if (abandoned) "abandoned" else "completed",
            terminalReason = if (interrupted) "process_interrupted" else if (abandoned) "user_abandoned" else "completed",
            endedAt = if (abandoned) null else Instant.parse(start).plusSeconds(original.totalElapsedSec!!.toLong()).toString())
    }

    private fun smallPhases(name: String): List<WorkoutPhaseIntervalEntity> {
        val id = "e21-s04-small-$name"
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
            original.copy(id = id, planId = "e21-s04-small-PT",
                planSnapshotJson = original.planSnapshotJson.replace("plan-old", "e21-s04-small-PT")
                    .replace("History L", "S04测试计时"),
                status = "abandoned", terminalReason = "user_abandoned",
                startedAt = "2026-01-01T02:00:00Z",
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
    private data class Profile(val id: String, val mode: String, val phases: Int,
        val acquisitions: Int, val samples: Int) {
        val recordingId get() = "$id-r"
        val plan get() = when (mode) {
            "timed" -> TIMED_PLAN_SNAPSHOT
            "strength" -> STRENGTH_PLAN_SNAPSHOT
            else -> freeFollowAlongSnapshot().toStorageJson()
        }
        val metadata get() = if (mode == "timed") TIMED_METADATA else DISPLAY_METADATA
        fun identity(index: Int): Pair<String, String> = when (mode) {
            "timed" -> if (index == 0 || index == phases - 1) "timed_work" to TIMED_WORK_IDENTITY
                else "paused" to TIMED_PAUSED_IDENTITY
            "strength" -> when (index) {
                0 -> "strength_prepare_set" to STRENGTH_PREPARE_IDENTITY
                phases - 1 -> "strength_active_set" to STRENGTH_ACTIVE_IDENTITY
                else -> "paused" to STRENGTH_PAUSED_IDENTITY
            }
            else -> {
                val prepared = (PlanSnapshotStorageV1Validator.prepare(plan, WorkoutMode.FOLLOW_ALONG)
                    as PreparedPlanSnapshotStorageV1Result.Valid).prepared
                "follow_along_action" to freeFollowAlongPhase(prepared).phaseIdentityJson
            }
        }
    }

    private suspend fun seedLifeProfile(p: Profile, index: Int) {
        val sql = database.openHelper.writableDatabase
        val start = Instant.parse("2026-08-30T16:00:00Z").plusSeconds(index * 60L)
        val first = p.identity(0)
        val owner = repository.admitRecorder("e21-s04-life", WorkoutSessionEntity(
            id = p.id, planId = if (p.mode == "timed") "plan-old" else null,
            mode = p.mode, status = "active", planSnapshotJson = p.plan,
            startedAt = start.toString(), startLocalDate = "2026-08-31",
            startZoneId = "Asia/Shanghai", startUtcOffsetSeconds = 28_800L,
            timeMetadataSourceContractVersion = 1L,
            timelineVersion = 1, lastDurableOffsetMs = 0, lastMutationSequence = 0,
            displayMetadataContractVersion = 1, sessionDisplayMetadataJson = p.metadata
        ), WorkoutPhaseIntervalEntity(p.id + ":phase:0", p.id, 0, 0, null, 0, null, 1,
            first.first, first.second)).ownerToken
        database.withTransaction {
            sql.execSQL("INSERT INTO workout_sessions(" +
                "id,plan_id,mode,status,plan_snapshot_json,started_at,ended_at," +
                "total_elapsed_sec,effective_elapsed_sec,paused_elapsed_sec,timeline_version," +
                "last_durable_offset_ms,last_mutation_sequence,trusted_end_offset_ms,terminal_reason," +
                "display_metadata_contract_version,session_display_metadata_json," +
                "start_local_date,start_zone_id,start_utc_offset_seconds,time_metadata_source_contract_version) " +
                "VALUES(?,?,?,'active',?,?,NULL,NULL,NULL,NULL,1,28800000,1000000,NULL,NULL,1,?,'2026-08-31','Asia/Shanghai',28800,1)",
                arrayOf(p.id, if (p.mode == "timed") "plan-old" else null, p.mode, p.plan, start.toString(), p.metadata))
            sql.compileStatement("INSERT INTO workout_phase_intervals(" +
                "id,session_id,sequence,start_offset_ms,end_offset_ms,start_mutation_sequence," +
                "end_mutation_sequence,open_marker,phase_kind,phase_identity_json) VALUES(?,?,?,?,?,?,?,?,?,?)").use { statement ->
                repeat(p.phases) { index ->
                    statement.clearBindings()
                    val bounds = if (p.phases == 1) 0L to 28800000L else boundary(index, p.phases)
                    statement.bindString(1, p.id + ":phase:$index"); statement.bindString(2, p.id)
                    statement.bindLong(3, index.toLong()); statement.bindLong(4, bounds.first)
                    if (index == p.phases - 1) statement.bindNull(5) else statement.bindLong(5, bounds.second)
                    statement.bindLong(6, index.toLong())
                    if (index == p.phases - 1) { statement.bindNull(7); statement.bindLong(8, 1) }
                    else { statement.bindLong(7, index + 1L); statement.bindNull(8) }
                    val identity = p.identity(index)
                    statement.bindString(9, identity.first); statement.bindString(10, identity.second)
                    statement.executeInsert()
                }
            }
            sql.execSQL("INSERT INTO heart_rate_recordings(" +
                "recording_id,session_id,status,started_offset_ms,started_mutation_sequence," +
                "ended_offset_ms,ended_mutation_sequence,source_contract_version,source_kind," +
                "acquisition_contract_version,parameter_snapshot_version,age,personal_max_bpm," +
                "effective_max_bpm,effective_max_source,alert_threshold_bpm,zone_snapshot_json,original_analysis_version) " +
                "VALUES(?,?,'active',0,0,NULL,NULL,1,'ble_hrs',1,1,NULL,200,200,'personal_max',NULL,?,NULL)",
                arrayOf(p.recordingId, p.id, ZONE_SNAPSHOT_200))
            sql.compileStatement("INSERT INTO heart_rate_acquisition_intervals(" +
                "id,recording_id,sequence,start_offset_ms,end_offset_ms,start_mutation_sequence," +
                "end_mutation_sequence,open_marker,recording_intent,intent_reason,device_state,device_reason) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)").use { statement ->
                repeat(p.acquisitions) { index ->
                    statement.clearBindings()
                    val bounds = boundary(index, p.acquisitions)
                    statement.bindString(1, p.recordingId + ":acquisition:$index")
                    statement.bindString(2, p.recordingId); statement.bindLong(3, index.toLong())
                    statement.bindLong(4, bounds.first)
                    if (index == p.acquisitions - 1) statement.bindNull(5) else statement.bindLong(5, bounds.second)
                    statement.bindLong(6, index.toLong())
                    if (index == p.acquisitions - 1) { statement.bindNull(7); statement.bindLong(8, 1) }
                    else { statement.bindLong(7, index + 1L); statement.bindNull(8) }
                    if (index == 0 || index == 1 || index == p.acquisitions - 1) {
                        statement.bindString(9, "expected_recording"); statement.bindNull(10)
                    } else {
                        statement.bindString(9, "user_excluded")
                        statement.bindString(10, USER_EXCLUSION_REASONS[(index - 2) % 3])
                    }
                    val device = DEVICE_FACTS[index % DEVICE_FACTS.size]
                    statement.bindString(11, device.first)
                    if (device.second == null) statement.bindNull(12) else statement.bindString(12, device.second!!)
                    statement.executeInsert()
                }
            }
            sql.compileStatement("INSERT INTO heart_rate_samples(recording_id,sample_sequence,offset_ms,mutation_sequence,bpm) " +
                "VALUES(?,?,?,?,?)").use { statement ->
                repeat(p.samples) { sequence ->
                    val offset: Long
                    val mutation: Long
                    val bpm: Int
                    when {
                        sequence < 32 -> { offset = 5000; mutation = 0; bpm = 80 }
                        sequence == 32 -> { offset = 28769999; mutation = 0; bpm = 90 }
                        sequence <= 42 -> {
                            offset = 28770000L + sequence - 33
                            mutation = if (sequence == 33) maxOf(p.phases - 1, p.acquisitions - 1).toLong() else 0
                            bpm = listOf(99, 100, 119, 120, 139, 140, 159, 160, 179, 180)[sequence - 33]
                        }
                        sequence == 43 -> { offset = 28795001; mutation = 0; bpm = 180 }
                        else -> { offset = 10000L + sequence - 44; mutation = 0; bpm = 80 + (sequence - 44) % 20 }
                    }
                    statement.clearBindings()
                    statement.bindString(1, p.recordingId); statement.bindLong(2, sequence.toLong())
                    statement.bindLong(3, offset); statement.bindLong(4, mutation); statement.bindLong(5, bpm.toLong())
                    statement.executeInsert()
                }
            }
        }
        repository.finalizeRecordingSession(owner, RecordingFinalizationRequest(
            p.id, p.recordingId, "active", CanonicalTuple(28800000, 1000000),
            28800000, "completed", "completed", start.plusMillis(28800000).toString()
        ))
        repository.releaseRecorderAfterTerminal(owner, p.id, application.heartRateRuntimeOwner)
    }

    private fun boundary(index: Int, count: Int): Pair<Long, Long> {
        if (index == 0) return 0L to 3000L
        if (index == count - 1) return 28770000L to 28800000L
        val ordinal = index - 1 - if (index > 20) 1 else 0
        val start = 3000L + 28767000L * ordinal / (count - 3)
        return start to if (index == 20) start else 3000L + 28767000L * (ordinal + 1) / (count - 3)
    }

    private companion object {
        const val DISPLAY_METADATA =
            "{\"displayMetadataContractVersion\":1,\"entries\":[]}"
        const val STRENGTH_PLAN_SNAPSHOT =
            "{\"planSnapshotStorageContractVersion\":1,\"planId\":null,\"title\":\"Strength\",\"mode\":\"strength\",\"blocks\":[{\"id\":\"block\",\"kind\":\"strength_exercise\",\"order\":0,\"exerciseId\":\"exercise\",\"sets\":[{\"id\":\"set\",\"order\":0,\"kind\":\"working\"}],\"substitutions\":[],\"setTimerMode\":\"manual_start\"}],\"preferences\":null,\"followAlong\":null}"
        const val SIGNATURE = "c7e6dd87cd0794071a57be2dcbfde1f1adb2030364d2ff9549631eeda486e0e3"
        const val STRENGTH_PREPARE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"strength_v1\",\"payloadVersion\":1,\"mode\":\"strength\",\"phaseKind\":\"strength_prepare_set\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"$SIGNATURE\"},\"payload\":{\"variant\":\"prepare_set\",\"blockId\":\"block\",\"setPlanId\":\"set\",\"plannedExerciseId\":\"exercise\",\"actualExerciseId\":\"exercise\",\"exerciseSetIndex0\":0,\"globalSetIndex0\":0,\"setKind\":\"working\",\"substitutedFromExerciseId\":null}}"
        const val STRENGTH_ACTIVE_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"strength_v1\",\"payloadVersion\":1,\"mode\":\"strength\",\"phaseKind\":\"strength_active_set\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"$SIGNATURE\"},\"payload\":{\"variant\":\"active_set\",\"blockId\":\"block\",\"setPlanId\":\"set\",\"plannedExerciseId\":\"exercise\",\"actualExerciseId\":\"exercise\",\"exerciseSetIndex0\":0,\"globalSetIndex0\":0,\"setKind\":\"working\",\"substitutedFromExerciseId\":null}}"
        const val STRENGTH_PAUSED_IDENTITY =
            "{\"phaseIdentityContractVersion\":1,\"family\":\"strength_v1\",\"payloadVersion\":1,\"mode\":\"strength\",\"phaseKind\":\"paused\",\"orderedStructureSignature\":{\"signatureContractVersion\":1,\"algorithm\":\"sha256\",\"digestHexLowercase\":\"$SIGNATURE\"},\"payload\":{\"variant\":\"paused\",\"blockId\":null,\"setPlanId\":null,\"plannedExerciseId\":null,\"actualExerciseId\":null,\"exerciseSetIndex0\":null,\"globalSetIndex0\":null,\"setKind\":null,\"substitutedFromExerciseId\":null}}"
        const val ZONE_SNAPSHOT_200 =
            "{\"zoneSnapshotContractVersion\":1,\"unit\":\"bpm\",\"effectiveMaxBpm\":200,\"effectiveMaxSource\":\"personal_max\",\"zones\":[{\"zoneId\":\"below_50\",\"lowerBoundBasisPointsInclusive\":null,\"upperBoundBasisPointsExclusive\":5000},{\"zoneId\":\"from_50_to_60\",\"lowerBoundBasisPointsInclusive\":5000,\"upperBoundBasisPointsExclusive\":6000},{\"zoneId\":\"from_60_to_70\",\"lowerBoundBasisPointsInclusive\":6000,\"upperBoundBasisPointsExclusive\":7000},{\"zoneId\":\"from_70_to_80\",\"lowerBoundBasisPointsInclusive\":7000,\"upperBoundBasisPointsExclusive\":8000},{\"zoneId\":\"from_80_to_90\",\"lowerBoundBasisPointsInclusive\":8000,\"upperBoundBasisPointsExclusive\":9000},{\"zoneId\":\"at_or_above_90\",\"lowerBoundBasisPointsInclusive\":9000,\"upperBoundBasisPointsExclusive\":null}]}"
        val USER_EXCLUSION_REASONS = listOf(
            "user_turned_off",
            "user_opted_out",
            "user_disconnected_suppress_recovery"
        )
        val DEVICE_FACTS = listOf(
            "not_observing" to null,
            "no_source_selected" to "source_not_selected",
            "permission_required" to "permission_missing",
            "permission_required" to "permission_revoked",
            "bluetooth_unavailable" to "bluetooth_off",
            "bluetooth_unavailable" to "platform_unavailable",
            "searching" to "initial_acquisition",
            "searching" to "automatic_recovery",
            "connecting" to "initial_acquisition",
            "waiting_first_sample" to "automatic_recovery",
            "live" to null,
            "stale" to "first_sample_timeout",
            "stale" to "sample_stale_timeout",
            "reconnecting" to "automatic_recovery",
            "reconnecting" to "unexpected_disconnect",
            "disconnected" to "source_unavailable",
            "disconnected" to "unexpected_disconnect",
            "disconnected" to "connection_timeout",
            "technical_failure" to "measurement_stream_unavailable",
            "technical_failure" to "platform_failure"
        )
        const val TIMED_PLAN_SNAPSHOT = """{"planSnapshotStorageContractVersion":1,"planId":"plan-old","title":"History L","mode":"timed","blocks":[{"id":"w","kind":"warmup","title":"Warm old","order":0,"durationSec":2,"items":[]},{"id":"t","kind":"stretch","order":1,"durationSec":3,"items":[]},{"id":"d","kind":"cooldown","title":"Cool old","order":2,"durationSec":4,"items":[]},{"id":"b","kind":"warmup","title":"Boundary old","order":3,"items":[{"id":"bw","exerciseId":"ex-b","labelOverride":"Boundary item","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":6,"restAfterSec":2,"autoAdvance":false},{"id":"br","labelOverride":"Boundary rest","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"a","kind":"timed_circuit","title":"Circuit A","order":4,"rounds":2,"restBetweenRoundsSec":5,"items":[{"id":"aw","exerciseId":"ex-a","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":10,"restAfterSec":4,"autoAdvance":false},{"id":"ar","labelOverride":"Item rest A","stageType":"rest","iconKey":"custom","colorHex":"#223344","workDurationSec":3,"autoAdvance":true}]},{"id":"z","kind":"timed_circuit","title":"Circuit Z","order":5,"rounds":1,"restBetweenRoundsSec":0,"items":[{"id":"zw","exerciseId":"ex-z","stageType":"work","iconKey":"custom","colorHex":"#112233","workDurationSec":8,"restAfterSec":2,"autoAdvance":false}]},{"id":"r","kind":"rest","title":"Rest title","order":6,"durationSec":7,"label":"Rest label"}],"preferences":null,"followAlong":null}"""
        const val TIMED_METADATA = """{"displayMetadataContractVersion":1,"entries":[{"entityKind":"exercise","stableId":"ex-b","displayNameAtFirstReference":"Metadata B","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-a","displayNameAtFirstReference":"Base A","customNameAtFirstReference":"Custom A","resolutionSource":"plan_snapshot"},{"entityKind":"exercise","stableId":"ex-z","displayNameAtFirstReference":"Exercise Z","customNameAtFirstReference":null,"resolutionSource":"plan_snapshot"}]}"""
        const val TIMED_WORK_IDENTITY = """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"boundary_block_work","blockId":"w","stepIndex0":0,"legacyBlockKind":"warmup","legacyStageType":"warmup","itemId":null,"exerciseId":null,"roundIndex0":null}}"""
        const val TIMED_PAUSED_IDENTITY = """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"f97cc813bcef55f121ba250982771a2a455cb9b310523f9f1254716dbd670354"},"payload":{"variant":"paused","blockId":null,"stepIndex0":null,"legacyBlockKind":null,"legacyStageType":null,"itemId":null,"exerciseId":null,"roundIndex0":null}}"""
    }

    private var s04Method = ""
    private var s04Event = 0
    private val s04Instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val s04Automation get() = s04Instrumentation.uiAutomation
    private val s04Package get() = s04Instrumentation.targetContext.packageName
    private fun s04Folder(): File = File(s04Instrumentation.targetContext.filesDir,
        "e21-s04-ui/$s04Method").apply { check(mkdirs() || isDirectory) }
    private fun s04Nodes(node: android.view.accessibility.AccessibilityNodeInfo): List<android.view.accessibility.AccessibilityNodeInfo> =
        listOf(node) + (0 until node.childCount).flatMap { node.getChild(it)?.let(::s04Nodes).orEmpty() }
    private fun s04Find(label: String, packageName: String = s04Package,
        editable: Boolean = false, byId: Boolean = false): android.view.accessibility.AccessibilityNodeInfo? {
        s04Automation.serviceInfo = s04Automation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        val windows = s04Automation.windows
        val candidates = windows.flatMap { window ->
            val root = window.root ?: return@flatMap emptyList()
            if (root.packageName?.toString() != packageName) return@flatMap emptyList()
            val windowBounds = android.graphics.Rect().also { window.getBoundsInScreen(it) }
            s04Nodes(root).filter { node ->
                val text = node.text?.toString().orEmpty()
                val description = node.contentDescription?.toString().orEmpty()
                val match = if (byId) node.viewIdResourceName?.endsWith(":id/$label") == true
                    else if (editable) node.isEditable && (text.contains(label) || description.contains(label) ||
                        node.hintText?.toString()?.contains(label) == true)
                    else text == label || description == label
                val bounds = android.graphics.Rect().also { node.getBoundsInScreen(it) }
                match && (!editable || node.isEditable) && node.isVisibleToUser && bounds.intersect(windowBounds) &&
                    windows.none { overlay ->
                        overlay.layer > window.layer && android.graphics.Rect().also { overlay.getBoundsInScreen(it) }
                            .contains(bounds.centerX(), bounds.centerY())
                    }
            }
        }
        check(candidates.size <= 1) { "Ambiguous visible control $label: ${candidates.size}" }
        return candidates.singleOrNull()
    }
    private fun s04Await(label: String, packageName: String = s04Package, byId: Boolean = false):
        android.view.accessibility.AccessibilityNodeInfo {
        compose.waitUntil(5_000) { s04Find(label, packageName, byId = byId) != null }
        return requireNotNull(s04Find(label, packageName, byId = byId))
    }
    private fun s04Dump(name: String) {
        val writer = java.io.StringWriter()
        val xml = android.util.Xml.newSerializer()
        xml.setOutput(writer); xml.startDocument("UTF-8", true); xml.startTag(null, "windows")
        for (window in s04Automation.windows) {
            xml.startTag(null, "window"); xml.attribute(null, "id", window.id.toString())
            xml.attribute(null, "layer", window.layer.toString())
            val root = window.root
            if (root != null) for (node in s04Nodes(root)) {
                xml.startTag(null, "node")
                xml.attribute(null, "package", node.packageName?.toString().orEmpty())
                xml.attribute(null, "text", node.text?.toString().orEmpty())
                xml.attribute(null, "description", node.contentDescription?.toString().orEmpty())
                xml.attribute(null, "id", node.viewIdResourceName.orEmpty())
                xml.attribute(null, "bounds", android.graphics.Rect().also { node.getBoundsInScreen(it) }.toShortString())
                xml.attribute(null, "enabled", node.isEnabled.toString())
                xml.attribute(null, "visible", node.isVisibleToUser.toString())
                xml.endTag(null, "node")
            }
            xml.endTag(null, "window")
        }
        xml.endTag(null, "windows"); xml.endDocument()
        val file = File(s04Folder(), "$name.xml")
        check(file.createNewFile())
        file.writeText(writer.toString(), Charsets.UTF_8)
        println("S04_XML=files/e21-s04-ui/$s04Method/${file.name}")
    }
    private fun s04Screenshot(name: String) {
        val bitmap = requireNotNull(s04Automation.takeScreenshot())
        val file = File(s04Folder(), "$name.png")
        check(file.createNewFile())
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        println("S04_SCREENSHOT=files/e21-s04-ui/$s04Method/${file.name}")
    }
    private fun s04Motion(action: Int, down: Long, time: Long, x: Float, y: Float) {
        val event = android.view.MotionEvent.obtain(down, time, action, x, y, 0)
        event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
        val injected = s04Automation.injectInputEvent(event, true)
        println("S04_MOTION method=$s04Method action=$action down=$down time=$time x=$x y=$y injected=$injected")
        event.recycle(); check(injected)
    }
    private fun s04Idle() { s04Automation.waitForIdle(500, 5_000) }
    private fun s04Touch(node: android.view.accessibility.AccessibilityNodeInfo, capture: Boolean = false) {
        check(node.isEnabled && node.isVisibleToUser)
        val bounds = android.graphics.Rect().also { node.getBoundsInScreen(it) }
        val windows = s04Automation.windows
        val window = windows.single { it.id == node.windowId }
        check(bounds.intersect(android.graphics.Rect().also { window.getBoundsInScreen(it) }))
        check(windows.none { overlay -> overlay.layer > window.layer &&
            android.graphics.Rect().also { overlay.getBoundsInScreen(it) }.contains(bounds.centerX(), bounds.centerY()) })
        val label = node.text?.toString() ?: node.contentDescription?.toString()
        val number = ++s04Event
        if (capture) s04Dump("$number-before")
        println("S04_TARGET package=${node.packageName} text=$label id=${node.viewIdResourceName} bounds=$bounds")
        val time = android.os.SystemClock.uptimeMillis()
        s04Motion(android.view.MotionEvent.ACTION_DOWN, time, time, bounds.exactCenterX(), bounds.exactCenterY())
        s04Motion(android.view.MotionEvent.ACTION_UP, time, time + 50, bounds.exactCenterX(), bounds.exactCenterY())
        s04Idle()
        if (capture) s04Dump("$number-after")
    }
    private fun s04Swipe(up: Boolean, packageName: String = s04Package) {
        val root = s04Automation.windows.sortedByDescending { it.layer }.mapNotNull { it.root }
            .first { it.packageName?.toString() == packageName }
        val bounds = android.graphics.Rect().also { root.getBoundsInScreen(it) }
        val x = bounds.exactCenterX()
        val start = bounds.top + bounds.height() * (if (up) 0.72f else 0.30f)
        val end = bounds.top + bounds.height() * (if (up) 0.30f else 0.72f)
        val time = android.os.SystemClock.uptimeMillis()
        s04Motion(android.view.MotionEvent.ACTION_DOWN, time, time, x, start)
        for (i in 1..8) s04Motion(android.view.MotionEvent.ACTION_MOVE, time, time + i * 30, x, start + (end - start) * i / 8)
        s04Motion(android.view.MotionEvent.ACTION_UP, time, time + 250, x, end)
        s04Idle()
    }
    private fun s04Tap(label: String, capture: Boolean = false, scrollUp: Boolean = true,
        packageName: String = s04Package, byId: Boolean = false) {
        var node = s04Find(label, packageName, byId = byId)
        var scrolls = 0
        while (node == null && scrolls < 12) {
            s04Swipe(scrollUp, packageName); scrolls++
            node = s04Find(label, packageName, byId = byId)
        }
        s04Touch(requireNotNull(node) { "Visible control not found after $scrolls scrolls: $label" }, capture)
    }
    private fun s04Key(code: Int, meta: Int = 0, capture: Boolean = false) {
        val number = ++s04Event
        if (capture) s04Dump("$number-before-key")
        val time = android.os.SystemClock.uptimeMillis()
        for (action in listOf(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.ACTION_UP)) {
            val accepted = s04Automation.injectInputEvent(android.view.KeyEvent(time, time, action, code, 0, meta), true)
            println("S04_KEY code=$code action=$action meta=$meta time=$time injected=$accepted")
            check(accepted)
        }
        s04Idle()
        if (capture) s04Dump("$number-after-key")
    }
    private fun s04Dates(values: List<String>) {
        s04Tap("输入起止日期", scrollUp = false)
        s04ReplaceDates(values)
    }
    private fun s04ReplaceDates(values: List<String>) {
        for ((index, label) in listOf("开始年", "开始月", "开始日", "结束年", "结束月", "结束日").withIndex()) {
            s04Touch(requireNotNull(s04Find(label, editable = true)))
            s04Key(android.view.KeyEvent.KEYCODE_A, android.view.KeyEvent.META_CTRL_ON)
            s04Key(android.view.KeyEvent.KEYCODE_DEL)
            for (digit in values[index]) s04Key(android.view.KeyEvent.KEYCODE_0 + digit.digitToInt())
        }
        s04Key(android.view.KeyEvent.KEYCODE_BACK)
    }
    private fun s04Enter() {
        s04Tap("导出记录", capture = true, scrollUp = false)
        s04Await("导出选择")
    }
    private fun s04Return() {
        s04Tap("返回记录总览", capture = true, scrollUp = false)
        s04Await("导出记录")
    }
}
