package com.liujyks.trainflow.feature.history

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liujyks.trainflow.core.data.HistoryEntry
import com.liujyks.trainflow.core.data.HistoryEntryClassification
import com.liujyks.trainflow.core.data.WorkoutSessionExportSelection
import com.liujyks.trainflow.core.data.WorkoutSessionRepository
import com.liujyks.trainflow.core.data.PreparedWorkoutSessionExport
import com.liujyks.trainflow.core.data.WorkoutSessionExportCleanupResult
import com.liujyks.trainflow.core.data.WorkoutSessionExportFiles
import com.liujyks.trainflow.core.model.WorkoutMode
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Instant
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.io.FileNotFoundException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

internal data class ExportReviewUnit(val key: String, val members: List<HistoryEntry>) {
    val representative: HistoryEntry get() = members.last()
    val isGroup: Boolean get() = representative.mergeGroupId != null
    val date: LocalDate get() = LocalDate.parse(representative.frozenDate)
    val eligible: Boolean get() = members.all { it.classification == HistoryEntryClassification.Available } &&
        (isGroup || representative.terminalReason != "process_interrupted")
}

internal data class ExportSelectionState(
    val isSelectionOpen: Boolean = false,
    val units: List<ExportReviewUnit> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val modeFilter: Set<String>? = null,
    val planFilter: Set<String>? = null,
    val excludedKeys: Set<String> = emptySet(),
    val expandedKeys: Set<String> = emptySet(),
    val dateInputOpen: Boolean = false,
    val dateFields: List<String> = List(6) { "" },
    val dateError: String? = null,
    val filterMessage: String? = null,
    val frozenSelection: WorkoutSessionExportSelection? = null
) {
    val visibleUnits: List<ExportReviewUnit> get() = units.filter { unit ->
        unit.eligible && (startDate == null || unit.date >= startDate) &&
            (endDate == null || unit.date <= endDate) && unit.members.all { member ->
                (modeFilter == null || member.mode!!.contractValue in modeFilter) &&
                    (planFilter == null || member.session!!.planId in planFilter)
            }
    }
    val selectedUnits: List<ExportReviewUnit> get() = visibleUnits.filter { it.key !in excludedKeys }
    val canConfirm: Boolean get() = dateError == null &&
        ((startDate == null && endDate == null) || (startDate != null && endDate != null)) &&
        selectedUnits.isNotEmpty()
    val plans: List<Pair<String, String>> get() = units.flatMap { it.members }.mapNotNull { entry ->
        entry.session?.let { session -> session.planId?.let { it to session.planSnapshot.title } }
    }.distinctBy { it.first }
    fun dayDescription(day: LocalDate): String {
        val onDay = units.filter { it.date == day }
        val counts = WorkoutMode.entries.mapNotNull { mode ->
            val count = onDay.count { it.representative.mode == mode }
            if (count == 0) null else "${exportModeLabel(mode.contractValue)}${count}"
        }
        val unfinished = onDay.count { !it.isGroup && it.representative.terminalReason == "process_interrupted" }
        return "$day " + if (onDay.isEmpty()) "无记录" else
            (counts + if (unfinished > 0) listOf("未完成$unfinished") else emptyList()).joinToString("，")
    }
}

internal enum class SessionExportAction { Save, Share }
internal enum class SessionExportStage { Selection, Location, Generating, Result }
internal sealed interface SessionExportOrigin {
    data class Single(val sessionId: String) : SessionExportOrigin
    data object Batch : SessionExportOrigin
}
internal data class SessionExportDeliveryState(
    val origin: SessionExportOrigin? = null,
    val action: SessionExportAction? = null,
    val stage: SessionExportStage = SessionExportStage.Selection,
    val target: Uri? = null,
    val directoryRequested: Boolean = false,
    val processed: Int = 0,
    val total: Int = 0,
    val prepared: PreparedWorkoutSessionExport? = null,
    val pendingShare: PreparedWorkoutSessionExport? = null,
    val shareClaimed: Boolean = false,
    val retainFile: Boolean = false,
    val message: String? = null,
    val cause: Throwable? = null,
    val cleanup: WorkoutSessionExportCleanupResult? = null,
    val canRetrySave: Boolean = false
)

internal class WorkoutSessionExportViewModel(
    private val repository: WorkoutSessionRepository,
    private val files: WorkoutSessionExportFiles,
    private val contentResolver: ContentResolver
) : ViewModel() {
    var state by mutableStateOf(ExportSelectionState())
        private set
    val isSelectionOpen: Boolean get() = state.isSelectionOpen
    val frozenSelection: WorkoutSessionExportSelection? get() = state.frozenSelection
    var delivery by mutableStateOf(SessionExportDeliveryState())
        private set
    val isExportFlowOpen: Boolean get() = isSelectionOpen || delivery.origin != null
    private var generationJob: Job? = null
    private var returnTarget by mutableStateOf<SessionExportOrigin?>(null)
    var cacheCleanupResult by mutableStateOf<WorkoutSessionExportCleanupResult?>(null)
        private set

    suspend fun enterSelection() {
        val entries = repository.historyEntries.first()
        val units = entries.filter { it.frozenDate != null }.groupBy { it.mergeGroupId ?: it.id }
            .map { (key, members) -> ExportReviewUnit(key,
                members.sortedWith(compareBy<HistoryEntry> { it.startedAt?.let(Instant::parse) }.thenBy { it.id })) }
            .sortedWith(compareByDescending<ExportReviewUnit> { it.date }
                .thenByDescending { it.representative.startedAt?.let(Instant::parse) }.thenBy { it.representative.id })
        state = ExportSelectionState(isSelectionOpen = true, units = units)
        delivery = SessionExportDeliveryState(origin = SessionExportOrigin.Batch)
        returnTarget = null
    }

    fun leaveSelection() = leaveExportFlow()

    fun enterSingleSession(sessionId: String, unknownDate: Boolean, action: SessionExportAction) {
        state = ExportSelectionState(frozenSelection = WorkoutSessionExportSelection(
            "single_session", null, null, null, null, listOf(sessionId),
            if (unknownDate) listOf(sessionId) else emptyList()))
        delivery = SessionExportDeliveryState(origin = SessionExportOrigin.Single(sessionId),
            action = action, stage = SessionExportStage.Location)
        returnTarget = null
    }

    fun beginBatchDelivery(action: SessionExportAction) {
        freezeSelection()
        delivery = SessionExportDeliveryState(origin = SessionExportOrigin.Batch,
            action = action, stage = SessionExportStage.Location)
    }

    fun selectInternalTarget() { delivery = delivery.copy(target = null) }
    fun requestExternalTarget() { delivery = delivery.copy(directoryRequested = true) }
    fun selectExternalTarget(uri: Uri?) {
        if (!delivery.directoryRequested || delivery.stage != SessionExportStage.Location) return
        delivery = delivery.copy(directoryRequested = false, target = uri ?: delivery.target)
    }

    fun confirmSave(displayLocale: String) {
        check(delivery.stage == SessionExportStage.Location)
        val selection = requireNotNull(frozenSelection)
        val previous = generationJob
        val generatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val request = delivery
        delivery = request.copy(stage = SessionExportStage.Generating, total = selection.includedSessionIds.size)
        generationJob = viewModelScope.launch {
            previous?.join()
            var prepared: PreparedWorkoutSessionExport? = null
            var preserveShared = false
            try {
                prepared = files.prepare(selection, generatedAt, displayLocale) { processed, total ->
                    viewModelScope.launch {
                        if (frozenSelection === selection && delivery.stage == SessionExportStage.Generating)
                            delivery = delivery.copy(processed = processed, total = total)
                    }
                }
                delivery = delivery.copy(prepared = prepared,
                    processed = selection.includedSessionIds.size)
                when {
                    request.action == SessionExportAction.Share -> {
                        preserveShared = true
                        val shared = files.markShareAttempt(prepared, Instant.now())
                        delivery = delivery.copy(prepared = shared, pendingShare = shared, retainFile = true)
                    }
                    request.target == null -> delivery = delivery.copy(stage = SessionExportStage.Result,
                        retainFile = true, message = "已生成到App内部导出缓存")
                    else -> savePrepared(prepared, request.target)
                }
            } catch (cause: CancellationException) {
                if (prepared != null && !preserveShared) {
                    val cleanup = withContext(NonCancellable) { files.discardUndelivered(prepared) }
                    cacheCleanupResult = cleanup
                    cleanup.failures.forEach { cause.addSuppressed(it.cause) }
                }
                throw cause
            } catch (cause: Throwable) {
                delivery = delivery.copy(stage = SessionExportStage.Result, cause = cause)
            }
        }
    }

    private suspend fun savePrepared(prepared: PreparedWorkoutSessionExport, tree: Uri) {
        try {
            withContext(Dispatchers.IO) {
                val parent = DocumentsContract.buildDocumentUriUsingTree(tree,
                    DocumentsContract.getTreeDocumentId(tree))
                val document = DocumentsContract.createDocument(contentResolver, parent,
                    "application/json", prepared.file.name)
                    ?: throw FileNotFoundException("无法在所选目录创建导出文件：$tree")
                val output = contentResolver.openOutputStream(document, "w")
                    ?: throw FileNotFoundException("无法打开所选导出文件：$document")
                output.use { destination ->
                    prepared.file.inputStream().use { source ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = source.read(buffer)
                            if (count < 0) break
                            destination.write(buffer, 0, count)
                        }
                        currentCoroutineContext().ensureActive()
                        destination.flush()
                    }
                }
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            delivery = delivery.copy(stage = SessionExportStage.Result, cause = cause, canRetrySave = true)
            return
        }
        delivery = delivery.copy(stage = SessionExportStage.Result, message = "已保存",
            retainFile = true, cause = null, canRetrySave = false)
        val cleanup = files.deleteAfterSuccessfulSave(prepared)
        delivery = delivery.copy(cleanup = cleanup)
    }

    fun retrySave() {
        check(delivery.canRetrySave)
        val request = delivery
        val prepared = requireNotNull(request.prepared)
        val target = requireNotNull(request.target)
        val previous = generationJob
        delivery = request.copy(stage = SessionExportStage.Generating, cause = null, canRetrySave = false)
        generationJob = viewModelScope.launch {
            previous?.join()
            try { savePrepared(prepared, target) }
            catch (cause: CancellationException) {
                val cleanup = withContext(NonCancellable) { files.discardUndelivered(prepared) }
                cacheCleanupResult = cleanup
                cleanup.failures.forEach { cause.addSuppressed(it.cause) }
                throw cause
            }
        }
    }

    fun claimPendingShare(): PreparedWorkoutSessionExport? {
        val pending = delivery.pendingShare ?: return null
        delivery = delivery.copy(pendingShare = null, shareClaimed = true)
        return pending
    }

    fun reportShareLaunchResult(operationId: String, cause: Throwable?) {
        if (delivery.prepared?.operationId != operationId || !delivery.shareClaimed) return
        delivery = delivery.copy(stage = SessionExportStage.Result,
            message = if (cause == null) "已交给系统分享" else null, cause = cause)
    }

    fun leaveExportFlow() {
        val request = delivery
        val previous = generationJob
        val wasActive = previous?.isActive == true
        previous?.cancel()
        returnTarget = request.origin
        state = ExportSelectionState()
        delivery = SessionExportDeliveryState()
        if (!wasActive && request.prepared != null && !request.retainFile) {
            generationJob = viewModelScope.launch {
                previous?.join()
                cacheCleanupResult = files.discardUndelivered(request.prepared)
            }
        }
    }

    fun takeReturnTarget(): SessionExportOrigin? = returnTarget.also { returnTarget = null }
    fun cleanupExportCache() {
        viewModelScope.launch { cacheCleanupResult = files.cleanupManually() }
    }
    fun moveMonth(amount: Long) { state = state.copy(month = state.month.plusMonths(amount)) }

    private fun changed(next: ExportSelectionState) {
        state = next.copy(excludedKeys = emptySet(), filterMessage = "筛选已更新，请重新确认场次")
    }

    fun selectDay(day: LocalDate) {
        val start = state.startDate
        if (start == null || state.endDate != null) {
            changed(state.copy(startDate = day, endDate = null, dateError = null))
        } else if (day < start) {
            state = state.copy(endDate = day, dateError = "结束日期不能早于开始日期")
        } else {
            changed(state.copy(endDate = day, dateError = null))
        }
    }

    fun clearDates() {
        changed(state.copy(startDate = null, endDate = null, dateError = null,
            dateFields = List(6) { "" }))
    }
    fun setAllModes() { if (state.modeFilter != null) changed(state.copy(modeFilter = null)) }
    fun toggleMode(mode: String) {
        val next = state.modeFilter?.let { if (mode in it) it - mode else it + mode } ?: setOf(mode)
        changed(state.copy(modeFilter = next.takeIf { it.isNotEmpty() }))
    }
    fun setAllPlans() { if (state.planFilter != null) changed(state.copy(planFilter = null)) }
    fun togglePlan(plan: String) {
        val next = state.planFilter?.let { if (plan in it) it - plan else it + plan } ?: setOf(plan)
        changed(state.copy(planFilter = next.takeIf { it.isNotEmpty() }))
    }
    fun toggleUnit(key: String) {
        state = state.copy(excludedKeys = if (key in state.excludedKeys) state.excludedKeys - key
            else state.excludedKeys + key)
    }
    fun toggleGroup(key: String) {
        state = state.copy(expandedKeys = if (key in state.expandedKeys) state.expandedKeys - key
            else state.expandedKeys + key)
    }
    fun openDateInput() {
        fun fields(date: LocalDate?) = date?.let { listOf(it.year.toString(), it.monthValue.toString(), it.dayOfMonth.toString()) }
            ?: listOf("", "", "")
        state = state.copy(dateInputOpen = true, dateFields = fields(state.startDate) + fields(state.endDate))
    }
    fun closeDateInput() { state = state.copy(dateInputOpen = false) }
    fun editDateField(index: Int, value: String) {
        state = state.copy(dateFields = state.dateFields.mapIndexed { i, old -> if (i == index) value else old })
        validateDateInput()
    }
    private fun inputDates(): Pair<LocalDate, LocalDate>? {
        val numbers = state.dateFields.map { it.toIntOrNull() }
        if (numbers.any { it == null }) {
            state = state.copy(dateError = "请输入完整有效的起止年月日")
            return null
        }
        val dates = try {
            LocalDate.of(numbers[0]!!, numbers[1]!!, numbers[2]!!) to
                LocalDate.of(numbers[3]!!, numbers[4]!!, numbers[5]!!)
        } catch (_: DateTimeException) {
            state = state.copy(dateError = "请输入有效日期")
            return null
        }
        if (dates.second < dates.first) {
            state = state.copy(dateError = "结束日期不能早于开始日期")
            return null
        }
        state = state.copy(dateError = null)
        return dates
    }
    private fun validateDateInput() { inputDates() }
    fun applyDateInput() {
        val (start, end) = inputDates() ?: return
        changed(state.copy(startDate = start, endDate = end, month = YearMonth.from(start),
            dateInputOpen = false, dateError = null))
    }
    fun freezeSelection(): WorkoutSessionExportSelection {
        check(state.canConfirm)
        val selection = WorkoutSessionExportSelection("calendar", state.startDate, state.endDate,
            state.modeFilter?.toSet(), state.planFilter?.toSet(),
            state.selectedUnits.flatMap { it.members.map { member -> member.id } }, emptyList())
        state = state.copy(frozenSelection = selection)
        return selection
    }
    class Factory(private val repository: WorkoutSessionRepository,
        private val files: WorkoutSessionExportFiles,
        private val contentResolver: ContentResolver) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WorkoutSessionExportViewModel(repository, files, contentResolver) as T
    }
}

internal fun exportModeLabel(mode: String): String = when (mode) {
    "timed" -> "计时"
    "strength" -> "力量"
    "follow_along" -> "跟练"
    else -> error("Unsupported workout mode: $mode")
}
