package com.liujyks.trainflow.feature.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liujyks.trainflow.core.data.HistoryEntry
import com.liujyks.trainflow.core.data.HistoryEntryClassification
import com.liujyks.trainflow.core.data.WorkoutSessionExportSelection
import com.liujyks.trainflow.core.data.WorkoutSessionRepository
import com.liujyks.trainflow.core.model.WorkoutMode
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Instant
import java.time.YearMonth
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

internal class WorkoutSessionExportViewModel(private val repository: WorkoutSessionRepository) : ViewModel() {
    var state by mutableStateOf(ExportSelectionState())
        private set
    val isSelectionOpen: Boolean get() = state.isSelectionOpen
    val frozenSelection: WorkoutSessionExportSelection? get() = state.frozenSelection

    suspend fun enterSelection() {
        val entries = repository.historyEntries.first()
        val units = entries.filter { it.frozenDate != null }.groupBy { it.mergeGroupId ?: it.id }
            .map { (key, members) -> ExportReviewUnit(key,
                members.sortedWith(compareBy<HistoryEntry> { it.startedAt?.let(Instant::parse) }.thenBy { it.id })) }
            .sortedWith(compareByDescending<ExportReviewUnit> { it.date }
                .thenByDescending { it.representative.startedAt?.let(Instant::parse) }.thenBy { it.representative.id })
        state = ExportSelectionState(isSelectionOpen = true, units = units)
    }

    fun leaveSelection() { state = ExportSelectionState() }
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
    class Factory(private val repository: WorkoutSessionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WorkoutSessionExportViewModel(repository) as T
    }
}

internal fun exportModeLabel(mode: String): String = when (mode) {
    "timed" -> "计时"
    "strength" -> "力量"
    "follow_along" -> "跟练"
    else -> error("Unsupported workout mode: $mode")
}
