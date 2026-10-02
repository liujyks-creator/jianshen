package com.liujyks.trainflow.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.core.model.WorkoutMode

@Composable
internal fun WorkoutSessionExportRoute(viewModel: WorkoutSessionExportViewModel, modifier: Modifier = Modifier) {
    val state = viewModel.state
    BackHandler { viewModel.leaveSelection() }
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 20.dp)) {
        item {
            Text("导出选择", style = MaterialTheme.typography.headlineLarge)
            TextButton(onClick = viewModel::leaveSelection) { Text("返回记录总览") }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { viewModel.moveMonth(-1) }) { Text("上个月") }
                Text(state.month.toString(), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { viewModel.moveMonth(1) }) { Text("下个月") }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                    Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                }
            }
            val first = state.month.atDay(1)
            val offset = first.dayOfWeek.value - 1
            val cells = List(offset) { null } + (1..state.month.lengthOfMonth()).map { state.month.atDay(it) }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { index ->
                        val day = week.getOrNull(index)
                        if (day == null) Spacer(Modifier.weight(1f)) else {
                            val selected = state.startDate != null && day >= state.startDate &&
                                day <= (state.endDate ?: state.startDate)
                            val description = state.dayDescription(day)
                            Column(Modifier.weight(1f).heightIn(min = 48.dp)
                                .selectable(selected = selected, role = Role.Button, onClick = { viewModel.selectDay(day) })
                                .semantics { contentDescription = description }.padding(vertical = 4.dp)) {
                                Text(day.dayOfMonth.toString(), color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface)
                                if (!description.endsWith("无记录")) Text(description.substringAfter(' '),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            Text(if (state.startDate == null) "不限日期" else "${state.startDate} 至 ${state.endDate ?: "请选择结束日期"}")
            Row {
                TextButton(onClick = viewModel::clearDates) { Text("不限日期") }
                TextButton(onClick = viewModel::openDateInput) { Text("输入起止日期") }
            }
            state.dateError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        item {
            Text("模式", style = MaterialTheme.typography.titleMedium)
            ExportFilterChoice("全部模式", state.modeFilter == null, viewModel::setAllModes)
            WorkoutMode.entries.forEach { mode ->
                ExportFilterChoice(exportModeLabel(mode.contractValue), state.modeFilter?.contains(mode.contractValue) == true) {
                    viewModel.toggleMode(mode.contractValue)
                }
            }
            Text("历史计划", style = MaterialTheme.typography.titleMedium)
            ExportFilterChoice("全部计划", state.planFilter == null, viewModel::setAllPlans)
            state.plans.forEach { (id, title) ->
                ExportFilterChoice("$title（$id）", state.planFilter?.contains(id) == true) { viewModel.togglePlan(id) }
            }
            state.filterMessage?.let { Text(it) }
        }
        item {
            Text("审阅场次（${state.selectedUnits.size}/${state.visibleUnits.size}）", style = MaterialTheme.typography.titleMedium)
        }
        items(state.visibleUnits, key = { it.key }) { unit ->
            val member = unit.representative
            val title = if (unit.isGroup) "合并记录（${unit.members.size}段）" else member.session!!.planSnapshot.title
            Column {
                Row(Modifier.fillMaxWidth().toggleable(value = unit.key !in state.excludedKeys, role = Role.Checkbox,
                    onValueChange = { viewModel.toggleUnit(unit.key) })
                    .semantics { contentDescription = if (unit.isGroup) "选择$title" else "选择原记录 ${member.id}" },
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = unit.key !in state.excludedKeys, onCheckedChange = null)
                    Column(Modifier.weight(1f)) {
                        Text(title)
                        Text("${unit.date} · ${exportModeLabel(member.mode!!.contractValue)}")
                        if (!unit.isGroup) Text(member.id, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (unit.isGroup) {
                    TextButton(onClick = { viewModel.toggleGroup(unit.key) }) {
                        Text(if (unit.key in state.expandedKeys) "收起原段" else "审阅原段")
                    }
                    if (unit.key in state.expandedKeys) unit.members.forEach { original ->
                        Text("${original.session!!.planSnapshot.title} · ${original.id} · ${original.startedAt}")
                    }
                }
            }
        }
        item {
            Button(onClick = { viewModel.freezeSelection() }, enabled = state.canConfirm,
                modifier = Modifier.fillMaxWidth()) { Text("确认选择") }
            state.frozenSelection?.let { Text("已确认选择：${it.includedSessionIds.size}条原记录") }
        }
    }
    if (state.dateInputOpen) AlertDialog(
        onDismissRequest = viewModel::closeDateInput,
        title = { Text("起止年月日") },
        text = {
            Column {
                listOf("开始", "结束").forEachIndexed { row, label ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("年", "月", "日").forEachIndexed { column, part ->
                            val index = row * 3 + column
                            OutlinedTextField(value = state.dateFields[index],
                                onValueChange = { viewModel.editDateField(index, it) },
                                label = { Text("$label$part") }, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        }
                    }
                }
                state.dateError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::applyDateInput,
            enabled = state.dateError == null && state.dateFields.all { it.isNotEmpty() }) { Text("应用日期") } },
        dismissButton = { TextButton(onClick = viewModel::closeDateInput) { Text("取消") } }
    )
}

@Composable
private fun ExportFilterChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() }),
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = selected, onCheckedChange = null)
        Text(label)
    }
}
