package com.liujyks.trainflow.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.core.data.*
import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToLong

/** The source route owns the loaded historical result. This page owns only display state. */
@Composable
internal fun WorkoutSessionAnalysisRoute(
    sessionId: String,
    historical: WorkoutSessionHistoricalResult,
    sessionExists: Boolean,
    unknownDate: Boolean,
    exportViewModel: WorkoutSessionExportViewModel?,
    onChooseExportDirectory: () -> Unit,
    onRetry: suspend () -> Unit,
    returnLabel: String,
    onClose: () -> Unit,
    onReturnToRecords: () -> Unit
) {
    val listState = rememberLazyListState()
    var selectedPhases by remember(sessionId) { mutableStateOf<List<Int>>(emptyList()) }
    var selectedPhase by remember(sessionId) { mutableStateOf<Int?>(null) }
    var scrub by remember(sessionId) { mutableStateOf<HeartRateRawScrubResult?>(null) }
    var selectedTuple by remember(sessionId) { mutableStateOf<HeartRateRawTuple?>(null) }
    var qualityExpanded by remember(sessionId) { mutableStateOf(false) }
    var plotSize by remember(sessionId) { mutableStateOf(IntSize(1, 1)) }
    var readFailure by remember(sessionId) { mutableStateOf<Throwable?>(null) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scale = LocalTrainFlowSkin.current.tokens.fontScale
    val locale = LocalConfiguration.current.locales[0].toLanguageTag()
    val leftPadding = with(density) { 56.dp.roundToPx() }
    val bottomPadding = with(density) { 64.dp.roundToPx() }
    val lineHeight = with(density) { (MaterialTheme.typography.bodyMedium.lineHeight * scale).roundToPx() }
    val result = remember(historical, plotSize, selectedTuple, leftPadding, bottomPadding, lineHeight) {
        HeartRateChartProjector.project(historical, HeartRateChartRequest(
            (plotSize.width - leftPadding).coerceAtLeast(1),
            (plotSize.height - bottomPadding).coerceAtLeast(1), lineHeight,
            selectedRawTuple = selectedTuple))
    }
    val card = buildWorkoutSessionHeartRateCardUiState(historical, readFailure)
    val projection = (result as? HeartRateChartResult.Available)?.projection
    val aggregates = remember(projection?.snapshot) {
        projection?.snapshot?.let {
            (parseCanonicalJson(it.phaseAggregatesJson) as CanonicalJsonValue.Obj).objects("aggregates")
        }.orEmpty()
    }
    val exportOpen = exportViewModel?.isExportFlowOpen == true
    LaunchedEffect(exportOpen) {
        if (!exportOpen) exportViewModel?.takeReturnTarget()
    }
    BackHandler {
        if (exportOpen) exportViewModel?.leaveExportFlow() else onClose()
    }

    fun select(phases: List<Int>) {
        selectedPhases = phases
        selectedPhase = phases.firstOrNull()
    }
    fun query(offset: Long, tuple: HeartRateRawTuple? = null) {
        val found = HeartRateChartProjector.scrub(requireNotNull(projection), offset, tuple)
        scrub = found
        selectedTuple = (found as? HeartRateRawScrubResult.Recorded)?.let {
            (it.preferredRaw ?: it.points.first().raw).chartTuple()
        }
    }
    fun retry() {
        selectedPhases = emptyList()
        selectedPhase = null
        scrub = null
        selectedTuple = null
        readFailure = null
        scope.launch {
            try {
                onRetry()
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Throwable) {
                readFailure = cause
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        if (!sessionExists) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnalysisText("记录已删除")
                Button(onClick = onReturnToRecords) { AnalysisText("返回记录总览") }
            }
        } else if (exportOpen) {
            WorkoutSessionExportRoute(requireNotNull(exportViewModel), onChooseDirectory = onChooseExportDirectory)
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    TextButton(onClick = onClose) { AnalysisText(returnLabel) }
                    AnalysisText((historical as? WorkoutSessionHistoricalResult.Resolved)?.title ?: "本次心率分析",
                        style = MaterialTheme.typography.headlineSmall)
                    AnalysisText("场次：$sessionId")
                    projection?.let {
                        AnalysisText(when (it.session.status) {
                            "abandoned" -> "提前结束"
                            "completed" -> "已完成"
                            else -> "训练已结束"
                        })
                        AnalysisText("原始分析版本 ${it.originalAnalysisVersion}")
                    }
                }
                item {
                    AnalysisText("主要训练统计", style = MaterialTheme.typography.titleLarge)
                    when (card) {
                        is WorkoutSessionHeartRateCardUiState.Content -> {
                            card.messages.forEach { AnalysisText(it) }
                            card.metrics.forEach { AnalysisText(it) }
                            card.facts.forEach { AnalysisText(it) }
                        }
                        is WorkoutSessionHeartRateCardUiState.ReadError -> {
                            AnalysisText("暂时无法读取本次心率数据")
                            Button(onClick = ::retry) { AnalysisText("重试") }
                        }
                        is WorkoutSessionHeartRateCardUiState.Unavailable -> AnalysisText(card.message)
                        WorkoutSessionHeartRateCardUiState.Hidden -> AnalysisText("本次未记录心率。")
                    }
                }
                if (projection != null && readFailure == null) {
                    item {
                        if (projection.recording.startedOffsetMs > 0) {
                            AnalysisText("开始前未记录/尚未开始：0:00–${HeartRateChartProjector.formatElapsed(projection.recording.startedOffsetMs)}")
                        }
                        if (projection.raw.isNotEmpty()) {
                            HeartRateAnalysisChart(projection, selectedPhase, selectedPhases, leftPadding, bottomPadding,
                                onSize = { plotSize = it }, onTap = { offset ->
                                    projection.phaseBands.firstOrNull {
                                        offset >= it.phase.startOffsetMs && offset < it.phase.endOffsetMs!!
                                    }?.let { select(listOf(it.phase.sequence)) }
                                }, onScrub = { query(it) })
                            AnalysisText("实线：实测心率；细灰虚线：虚线期间没有记录；平均虚线：主要训练平均；圆点：训练最高。")
                            AnalysisText("完整曲线包含不计入主要训练统计的心率；点按选阶段，拖动查看原始点。")
                        }
                        scrub?.let { found ->
                            when (found) {
                                is HeartRateRawScrubResult.NotRecorded -> AnalysisText("${found.formattedTime}：未记录心率")
                                is HeartRateRawScrubResult.Recorded -> {
                                    val point = found.points.first { it.raw.chartTuple() == selectedTuple }
                                    val primary = aggregates.any { it.number("phaseSequence") == point.phase.sequence.toLong() } &&
                                        point.acquisition.recordingIntent == "expected_recording" &&
                                        (point.raw.offsetMs < projection.domain.endOffsetMs ||
                                            point.raw.offsetMs == projection.domain.endOffsetMs &&
                                            point.raw.mutationSequence < projection.session.lastMutationSequence!!)
                                    AnalysisText("${found.formattedTime} · ${point.raw.bpm} bpm · ${phaseLabel(projection, point.phase.sequence)}" +
                                        if (primary) "" else " · 不计入主要训练统计")
                                    AnalysisText("原始点 ${point.raw.sampleSequence}，时间 ${point.raw.offsetMs} ms；同毫秒点按原始顺序逐个访问。")
                                }
                            }
                        }
                        val ordinal = projection.raw.indexOfFirst { it.chartTuple() == selectedTuple }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val point = projection.raw[if (ordinal < 0) 0 else ordinal - 1]
                                query(point.offsetMs, point.chartTuple())
                            }, enabled = projection.raw.isNotEmpty() && ordinal != 0, modifier = Modifier.weight(1f)) {
                                AnalysisText("上一个原始点")
                            }
                            OutlinedButton(onClick = {
                                val point = projection.raw[ordinal + 1]
                                query(point.offsetMs, point.chartTuple())
                            }, enabled = projection.raw.isNotEmpty() && ordinal < projection.raw.lastIndex,
                                modifier = Modifier.weight(1f)) { AnalysisText("下一个原始点") }
                        }
                    }
                    item {
                        AnalysisText("阶段与结构", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { select(emptyList()) }) { AnalysisText("查看全程") }
                        AnalysisText("结构选择只改变详情，图表始终显示全程。")
                        val free = projection.phaseBands.any { it.identity.payload().text("variant") == "free_session" }
                        if (free) AnalysisText("自由跟练：整场") else {
                            projection.source.timedStructures.forEach { structure ->
                                val rounds = structure.phases.filter { it.roundIndex0 != null }
                                    .groupBy { it.blockId to it.roundIndex0 }
                                val blocks = rounds.keys.map { it.first }.distinct()
                                rounds.forEach { (key, phases) ->
                                    OutlinedButton(onClick = { select(phases.map { it.phaseSequence.toInt() }) }) {
                                        AnalysisText("计时区块 ${blocks.indexOf(key.first) + 1} · 第 ${key.second!! + 1} 轮")
                                    }
                                    if (structure.focusEligible) {
                                        val work = phases.filter { it.trueWork }.map { it.phaseSequence.toInt() }
                                        val rest = phases.filter { it.trueRest }.map { it.phaseSequence.toInt() }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { select(work) }, enabled = work.isNotEmpty(),
                                                modifier = Modifier.weight(1f)) { AnalysisText("本轮训练") }
                                            OutlinedButton(onClick = { select(rest) }, enabled = rest.isNotEmpty(),
                                                modifier = Modifier.weight(1f)) { AnalysisText("本轮休息") }
                                        }
                                    }
                                }
                            }
                            val nonTimed = projection.phaseBands.filter {
                                it.identity.text("family") in listOf("strength_v1", "follow_along_v1") &&
                                    it.identity.payload().text("variant") != "paused"
                            }
                            nonTimed.groupBy { band ->
                                val payload = band.identity.payload()
                                if (band.identity.text("family") == "strength_v1") {
                                    listOf(band.identity.text("family"), payload.fields.getValue("blockId"),
                                        payload.fields.getValue("actualExerciseId"))
                                } else {
                                    listOf(band.identity.text("family"), payload.fields.getValue("blockId"),
                                        payload.fields.getValue("itemId"), payload.fields.getValue("roundIndex0"))
                                }
                            }.values.forEachIndexed { index, bands ->
                                OutlinedButton(onClick = { select(bands.map { it.phase.sequence }) }) {
                                    AnalysisText("动作/结构 ${index + 1} · ${phaseLabel(projection, bands.first().phase.sequence)}")
                                }
                                if (bands.first().identity.text("family") == "strength_v1") {
                                    bands.groupBy { it.identity.payload().fields.getValue("setPlanId") }.values.forEach { set ->
                                        OutlinedButton(onClick = { select(set.map { it.phase.sequence }) }) {
                                            AnalysisText("第 ${set.first().identity.payload().number("exerciseSetIndex0")!! + 1} 组")
                                        }
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { select(listOf((selectedPhase ?: 1) - 1)) },
                                    enabled = selectedPhase == null || selectedPhase!! > projection.phases.first().sequence,
                                    modifier = Modifier.weight(1f)) { AnalysisText("上一阶段") }
                                OutlinedButton(onClick = { select(listOf((selectedPhase ?: -1) + 1)) },
                                    enabled = selectedPhase == null || selectedPhase!! < projection.phases.last().sequence,
                                    modifier = Modifier.weight(1f)) { AnalysisText("下一阶段") }
                            }
                            projection.phaseBands.forEach { band ->
                                TextButton(onClick = { select(listOf(band.phase.sequence)) }) {
                                    AnalysisText("阶段 ${band.phase.sequence + 1} · ${phaseLabel(projection, band.phase.sequence)}" +
                                        if (band.phase.sequence in selectedPhases) " · 已选择" else "")
                                }
                            }
                        }
                        selectedPhases.forEach { sequence ->
                            val phase = projection.phases.single { it.sequence == sequence }
                            AnalysisText("${phaseLabel(projection, sequence)}：${HeartRateChartProjector.formatElapsed(phase.startOffsetMs)}–" +
                                "${HeartRateChartProjector.formatElapsed(phase.endOffsetMs!!)}，${durationText(phase.endOffsetMs - phase.startOffsetMs)}")
                            aggregates.firstOrNull { it.number("phaseSequence") == sequence.toLong() }?.let {
                                AnalysisText(phaseMetricText(it))
                            }
                            if (phase.phaseKind == "paused") {
                                AnalysisText("暂停期间心率已记录，但不计入训练区间分布和阶段摘要。")
                            }
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                AnalysisText("数据质量", style = MaterialTheme.typography.titleLarge)
                                (card as WorkoutSessionHeartRateCardUiState.Content).messages.forEach { AnalysisText(it) }
                                OutlinedButton(onClick = { qualityExpanded = !qualityExpanded }) {
                                    AnalysisText(if (qualityExpanded) "收起质量详情" else "展开质量详情")
                                }
                                if (qualityExpanded) HeartRateQualityDetails(projection)
                            }
                        }
                    }
                    item { HeartRateZoneDistribution(projection) }
                    item {
                        AnalysisText("阶段事实摘要", style = MaterialTheme.typography.titleLarge)
                        if (projection.snapshot.coverageStatus != "insufficient") {
                            aggregates.filter { (it.fields.getValue("conclusionEligible") as CanonicalJsonValue.Bool).value }
                                .forEach {
                                    AnalysisText("${phaseLabel(projection, it.number("phaseSequence")!!.toInt())}：${phaseMetricText(it)}")
                                }
                        } else AnalysisText("本次有效心率覆盖不足 50%，暂不生成自动摘要。")
                    }
                }
                if (exportViewModel != null && card is WorkoutSessionHeartRateCardUiState.Content) {
                    item {
                        AnalysisText("导出本次数据", style = MaterialTheme.typography.titleLarge)
                        Button(onClick = { exportViewModel.enterSingleSession(sessionId, unknownDate, SessionExportAction.Save) }) {
                            AnalysisText("保存本次数据")
                        }
                        OutlinedButton(onClick = {
                            exportViewModel.enterSingleSession(sessionId, unknownDate, SessionExportAction.Share)
                            exportViewModel.confirmSave(locale)
                        }) { AnalysisText("分享本次数据") }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartRateAnalysisChart(
    projection: HeartRateChartProjection, selectedPhase: Int?, selectedPhases: List<Int>,
    leftPadding: Int, bottomPadding: Int, onSize: (IntSize) -> Unit,
    onTap: (Long) -> Unit, onScrub: (Long) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scale = LocalTrainFlowSkin.current.tokens.fontScale
    val labelStyle = MaterialTheme.typography.bodyMedium.let {
        it.copy(fontSize = it.fontSize * scale, lineHeight = it.lineHeight * scale, color = colors.onSurface)
    }
    val measurer = rememberTextMeasurer()
    val latestProjection by rememberUpdatedState(projection)
    val latestTap by rememberUpdatedState(onTap)
    val latestScrub by rememberUpdatedState(onScrub)
    Canvas(Modifier.fillMaxWidth().height(340.dp).onSizeChanged(onSize)
        .semantics { contentDescription = "全程实测心率图。点按查看阶段，横向拖动查询原始点；也可使用图下阶段和原始点按钮。" }
        .pointerInput(projection.source, leftPadding) {
            fun time(x: Float): Long {
                val domain = latestProjection.domain
                val fraction = ((x - leftPadding) / (size.width - leftPadding).coerceAtLeast(1)).coerceIn(0f, 1f)
                return domain.startOffsetMs + (fraction * (domain.endOffsetMs - domain.startOffsetMs)).roundToLong()
            }
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var dragging = false
                do {
                    val change = awaitPointerEvent().changes.first { it.id == down.id }
                    if (change.isConsumed) break
                    val delta = change.position - down.position
                    if (!dragging && delta.getDistance() > viewConfiguration.touchSlop) {
                        if (abs(delta.x) <= abs(delta.y)) break
                        dragging = true
                    }
                    if (dragging) {
                        latestScrub(time(change.position.x))
                        change.consume()
                    } else if (!change.pressed) latestTap(time(change.position.x))
                } while (change.pressed)
            }
        }) {
        val axis = requireNotNull(projection.yAxis)
        val left = leftPadding.toFloat()
        val right = size.width - 8.dp.toPx()
        val bottom = size.height - bottomPadding
        val top = 28.dp.toPx()
        val span = (projection.domain.endOffsetMs - projection.domain.startOffsetMs).coerceAtLeast(1L)
        fun x(time: Long) = left + (time - projection.domain.startOffsetMs).toFloat() / span * (right - left)
        fun y(bpm: Int) = bottom - (bpm - axis.lowerBpm).toFloat() / (axis.upperBpm - axis.lowerBpm) * (bottom - top)
        axis.ticks.forEach { tick ->
            val pos = y(tick.bpm)
            drawLine(colors.outlineVariant, Offset(left, pos), Offset(right, pos), 1.dp.toPx())
            val label = measurer.measure("${tick.bpm}", labelStyle)
            drawText(label, topLeft = Offset((left - label.size.width - 6.dp.toPx()).coerceAtLeast(0f), pos - label.size.height / 2))
        }
        val bandTop = bottom + 4.dp.toPx()
        var lastLabelRight = left
        projection.phaseBands.forEach { band ->
            val start = x(band.phase.startOffsetMs)
            val end = x(band.phase.endOffsetMs!!)
            drawRect(if (band.phase.sequence in selectedPhases) colors.primaryContainer else colors.surfaceVariant,
                Offset(start, bandTop), Size(end - start, 20.dp.toPx()))
            drawLine(colors.outline, Offset(start, bandTop), Offset(start, bandTop + 20.dp.toPx()), 1.dp.toPx())
            val label = measurer.measure("${band.phase.sequence + 1}", labelStyle)
            val labelLeft = (start + end - label.size.width) / 2
            if (label.size.width <= end - start && labelLeft >= lastLabelRight) {
                drawText(label, topLeft = Offset(labelLeft, bandTop))
                lastLabelRight = labelLeft + label.size.width + 6.dp.toPx()
            }
        }
        projection.solidSegments.forEach { segment ->
            val points = projection.displayOrdinals.filter { it in segment.firstOrdinal..segment.lastOrdinal }
            val path = Path()
            points.forEachIndexed { index, ordinal ->
                val point = projection.raw[ordinal]
                if (index == 0) path.moveTo(x(point.offsetMs), y(point.bpm)) else path.lineTo(x(point.offsetMs), y(point.bpm))
            }
            if (points.size == 1) {
                val point = projection.raw[points.single()]
                drawCircle(colors.primary, 2.dp.toPx(), Offset(x(point.offsetMs), y(point.bpm)))
            } else drawPath(path, colors.primary, style = Stroke(width = 2.dp.toPx()))
        }
        projection.visualGaps.filter { it.style == HeartRateGapStyle.DASHED }.forEach { gap ->
            val a = projection.raw[gap.leftOrdinal]
            val b = projection.raw[gap.rightOrdinal]
            drawLine(colors.outline.copy(alpha = 0.6f), Offset(x(a.offsetMs), y(a.bpm)), Offset(x(b.offsetMs), y(b.bpm)),
                strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
        }
        projection.originalAverageBpm?.let { average ->
            drawLine(colors.tertiary, Offset(left, y(average)), Offset(right, y(average)), 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 4.dp.toPx())))
        }
        projection.originalHighest?.let { highest ->
            drawCircle(colors.primary, 5.dp.toPx(), Offset(x(highest.offsetMs), y(highest.bpm)))
            val label = measurer.measure("训练最高", labelStyle)
            drawText(label, topLeft = Offset(x(highest.offsetMs).coerceIn(left, (right - label.size.width).coerceAtLeast(left)),
                (y(highest.bpm) - label.size.height - 8.dp.toPx()).coerceAtLeast(0f)))
        }
        projection.selectedRaw?.let { point ->
            drawLine(colors.secondary, Offset(x(point.offsetMs), top), Offset(x(point.offsetMs), bottom), 1.dp.toPx())
            drawCircle(colors.secondary, 6.dp.toPx(), Offset(x(point.offsetMs), y(point.bpm)), style = Stroke(2.dp.toPx()))
        }
        val startLabel = measurer.measure(projection.domain.startLabel, labelStyle)
        val endLabel = measurer.measure(projection.domain.endLabel, labelStyle)
        val timeY = size.height - maxOf(startLabel.size.height, endLabel.size.height)
        drawText(startLabel, topLeft = Offset(left, timeY))
        if (right - endLabel.size.width > left + startLabel.size.width) {
            drawText(endLabel, topLeft = Offset(right - endLabel.size.width, timeY))
        }
        selectedPhase?.let { sequence ->
            val phase = projection.phases.single { it.sequence == sequence }
            drawLine(colors.primary, Offset(x(phase.startOffsetMs), bandTop + 22.dp.toPx()),
                Offset(x(phase.endOffsetMs!!), bandTop + 22.dp.toPx()), 2.dp.toPx())
        }
    }
}

@Composable
private fun HeartRateZoneDistribution(projection: HeartRateChartProjection) {
    AnalysisText("心率区间", style = MaterialTheme.typography.titleLarge)
    val snapshot = projection.snapshot
    val breakdown = parseCanonicalJson(snapshot.durationBreakdownJson) as CanonicalJsonValue.Obj
    val partition = breakdown.fields.getValue("primaryAnalysisPartition") as CanonicalJsonValue.Obj
    val excluded = breakdown.fields.getValue("phaseAxis") as CanonicalJsonValue.Obj
    val intent = breakdown.fields.getValue("intentAxis") as CanonicalJsonValue.Obj
    AnalysisText("阶段排除 ${durationText(excluded.number("phaseExcludedDurationMs"))}；用户排除 ${durationText(intent.number("userExcludedDurationMs"))}，分列展示，不计入区间分母。")
    if (snapshot.zoneDurationsJson == null) {
        AnalysisText(if (snapshot.zoneStatus == "unavailable_no_effective_max") "本次未设置最大心率，无法计算区间。"
            else "本次没有可纳入区间分布的时长。")
        return
    }
    val zones = parseCanonicalJson(snapshot.zoneDurationsJson) as CanonicalJsonValue.Obj
    val fields = listOf("below50DurationMs", "from50To60DurationMs", "from60To70DurationMs",
        "from70To80DurationMs", "from80To90DurationMs", "atOrAbove90DurationMs")
    val names = listOf("低于50%", "50–60%", "60–70%", "70–80%", "80–90%", "90%及以上", "未覆盖")
    val durations = fields.map { zones.number(it)!! } + partition.number("eligibleUncoveredDurationMs")!!
    val eligible = snapshot.eligibleDurationMs!!
    val colors = MaterialTheme.colorScheme
    val fills = listOf(colors.primaryContainer, colors.secondaryContainer, colors.tertiaryContainer,
        colors.primary, colors.secondary, colors.tertiary, colors.surfaceVariant)
    val measurer = rememberTextMeasurer()
    val scale = LocalTrainFlowSkin.current.tokens.fontScale
    val labelStyle = MaterialTheme.typography.bodyMedium.let {
        it.copy(fontSize = it.fontSize * scale, lineHeight = it.lineHeight * scale)
    }
    AnalysisText("六区间与未覆盖合计100%；分母为原主要训练有效时长 ${durationText(eligible)}。区间按本场冻结最大心率 ${projection.recording.effectiveMaxBpm} bpm，条内数字为区间编号，斜纹为未覆盖。")
    Canvas(Modifier.fillMaxWidth().height(32.dp).semantics { contentDescription = "六个心率区间与斜纹未覆盖分布，下方文字给出各区间时长和比例" }) {
        var x = 0f
        durations.forEachIndexed { index, duration ->
            val width = size.width * duration.toFloat() / eligible
            drawRect(fills[index], Offset(x, 0f), Size(width, size.height))
            if (width > 0f) {
                drawRect(colors.outline, Offset(x, 0f), Size(width, size.height), style = Stroke(1.dp.toPx()))
                if (index == 6) {
                    var stripe = x
                    while (stripe < x + width) {
                        drawLine(colors.onSurfaceVariant, Offset(stripe, size.height),
                            Offset((stripe + size.height).coerceAtMost(x + width), 0f), 1.dp.toPx())
                        stripe += 8.dp.toPx()
                    }
                } else {
                    val label = measurer.measure("${index + 1}", labelStyle)
                    if (label.size.width + 4.dp.toPx() <= width) {
                        drawText(label, color = if (index < 3) colors.onSurface else colors.onPrimary,
                            topLeft = Offset(x + (width - label.size.width) / 2, (size.height - label.size.height) / 2))
                    }
                }
            }
            x += width
        }
    }
    durations.forEachIndexed { index, duration ->
        AnalysisText("${if (index < 6) "区间${index + 1} " else "斜纹 "}${names[index]}：${durationText(duration)}，${duration * 100 / eligible}%")
    }
}

@Composable
private fun HeartRateQualityDetails(projection: HeartRateChartProjection) {
    val quality = parseCanonicalJson(projection.snapshot.qualityReasonsJson) as CanonicalJsonValue.Obj
    quality.objects("sessionReasons").forEach {
        val code = it.text("reasonCode")
        AnalysisText("整场 · ${qualityReason(code)}；起止未知；时长 ${durationText(it.number("durationMs"))}；${qualityImpact(code)}")
    }
    quality.objects("phaseReasons").forEach {
        val sequence = it.number("phaseSequence")!!.toInt()
        val code = it.text("reasonCode")
        AnalysisText("${phaseLabel(projection, sequence)} · ${qualityReason(code)}；该原因起止未知；时长 ${durationText(it.number("durationMs"))}；${qualityImpact(code)}")
    }
    projection.acquisitions.forEach { acquisition ->
        val start = acquisition.startOffsetMs
        val end = acquisition.endOffsetMs!!
        val phases = projection.phaseBands.filter { it.phase.startOffsetMs < end && it.phase.endOffsetMs!! > start }
            .joinToString("、") { phaseLabel(projection, it.phase.sequence) }.ifEmpty { "无正时长阶段" }
        AnalysisText("记录窗口 ${HeartRateChartProjector.formatElapsed(start)}–${HeartRateChartProjector.formatElapsed(end)}；" +
            "时长 ${durationText(end - start)}；${acquisitionState(acquisition.deviceState)}；" +
            "原因 ${acquisition.deviceReason?.let(::deviceReason) ?: "未提供"}；阶段：$phases。")
        AnalysisText(when (acquisition.recordingIntent) {
            "user_excluded" -> "${qualityReason(when (acquisition.intentReason) {
                "user_turned_off" -> "user_turned_off_excluded"
                "user_opted_out" -> "user_opted_out_excluded"
                "user_disconnected_suppress_recovery" -> "user_disconnected_suppress_recovery_excluded"
                else -> error("Unexpected intent reason: ${acquisition.intentReason}")
            })}，不计入主要训练统计。"
            "expected_recording" -> "预期记录；统计影响沿原覆盖和阶段事实，设备状态本身不代表有无样本。"
            else -> error("Unexpected recording intent: ${acquisition.recordingIntent}")
        })
    }
    if (projection.phases.any { it.phaseKind == "paused" }) {
        AnalysisText("暂停期间心率已记录，但不计入训练区间分布和阶段摘要。")
    }
}

@Composable
private fun AnalysisText(text: String, style: TextStyle = LocalTextStyle.current) {
    val scale = LocalTrainFlowSkin.current.tokens.fontScale
    Text(text, style = style.copy(fontSize = style.fontSize * scale, lineHeight = style.lineHeight * scale))
}

private fun CanonicalJsonValue.Obj.objects(key: String) =
    (fields.getValue(key) as CanonicalJsonValue.Arr).values.map { it as CanonicalJsonValue.Obj }
private fun CanonicalJsonValue.Obj.text(key: String) = (fields.getValue(key) as CanonicalJsonValue.Str).value
private fun CanonicalJsonValue.Obj.number(key: String): Long? = when (val value = fields.getValue(key)) {
    is CanonicalJsonValue.Num -> value.value.longValueExact()
    CanonicalJsonValue.Null -> null
    else -> error("Expected validated numeric fact: $key")
}
private fun CanonicalJsonValue.Obj.payload() = fields.getValue("payload") as CanonicalJsonValue.Obj
private fun durationText(ms: Long?): String = ms?.let { "${it / 1000.0} 秒" } ?: "未知"

private fun phaseLabel(projection: HeartRateChartProjection, sequence: Int): String {
    val band = projection.phaseBands.single { it.phase.sequence == sequence }
    val kind = when (band.phase.phaseKind) {
        "timed_work" -> "训练"
        "timed_rest" -> "休息"
        "strength_prepare_set" -> "准备组"
        "strength_active_set" -> "训练组"
        "strength_confirm_set" -> "确认组"
        "strength_rest" -> "组间休息"
        "follow_along_action" -> "跟练动作"
        "follow_along_rest" -> "跟练休息"
        "paused" -> "暂停"
        else -> error("Unexpected phase kind: ${band.phase.phaseKind}")
    }
    return band.display?.display?.label?.let { "$it · $kind" } ?: kind
}

private fun phaseMetricText(aggregate: CanonicalJsonValue.Obj): String {
    val prefix = if (aggregate.text("coverageStatus") == "normal") "" else "已记录片段"
    val average = aggregate.number("observedAvgBpm")?.let { "$it bpm" } ?: "未获得可统计值"
    val maximum = aggregate.number("observedMaxBpm")?.let { "$it bpm" } ?: "未获得可统计值"
    val coverage = aggregate.number("coverageBasisPoints")?.let { "${it / 100}%" } ?: "未知"
    return "${prefix}平均 $average，${prefix}最高 $maximum；覆盖 $coverage。"
}

private fun qualityReason(code: String): String = when (code) {
    "no_eligible_duration" -> "无可纳入主要训练统计的时长"
    "no_canonical_samples" -> "未获得有效心率样本"
    "canonical_only_excluded" -> "已记录样本均不计入主要训练统计"
    "eligible_uncovered_present" -> "有效时长中有未覆盖片段"
    "insufficient_coverage" -> "有效心率覆盖不足50%"
    "partial_coverage" -> "心率记录不完整"
    "unavailable_no_effective_max" -> "本场未设置最大心率"
    "not_requested_before_recording_start" -> "开始前未记录/尚未开始"
    "strength_prepare_excluded" -> "力量准备阶段排除"
    "paused_excluded" -> "暂停阶段排除"
    "user_turned_off_excluded" -> "用户关闭记录"
    "user_opted_out_excluded" -> "用户选择不记录"
    "user_disconnected_suppress_recovery_excluded" -> "用户主动断开并停止恢复"
    "process_interrupted" -> "进程中断"
    else -> error("Unexpected quality reason: $code")
}
private fun qualityImpact(code: String): String = when (code) {
    "unavailable_no_effective_max" -> "无法计算区间，保留已有bpm事实。"
    "insufficient_coverage" -> "主要统计仅基于已记录片段，不生成自动摘要。"
    "partial_coverage", "eligible_uncovered_present", "no_canonical_samples", "process_interrupted" ->
        "覆盖存在不足；统计和摘要范围沿原分析事实。"
    "no_eligible_duration", "canonical_only_excluded", "not_requested_before_recording_start",
    "strength_prepare_excluded", "paused_excluded", "user_turned_off_excluded", "user_opted_out_excluded",
    "user_disconnected_suppress_recovery_excluded" -> "对应片段不计入主要训练统计。"
    else -> error("Unexpected quality reason: $code")
}
private fun acquisitionState(code: String): String = when (code) {
    "not_observing" -> "未观察设备"
    "no_source_selected" -> "未选择来源"
    "permission_required" -> "需要权限"
    "bluetooth_unavailable" -> "蓝牙不可用"
    "searching" -> "搜索中"
    "connecting" -> "连接中"
    "waiting_first_sample" -> "等待首次样本"
    "live" -> "实时记录"
    "stale" -> "样本过期"
    "reconnecting" -> "重新连接中"
    "disconnected" -> "已断开"
    "technical_failure" -> "技术故障"
    else -> error("Unexpected device state: $code")
}
private fun deviceReason(code: String): String = when (code) {
    "initial_acquisition" -> "首次连接"
    "automatic_recovery" -> "自动恢复"
    "source_not_selected" -> "未选择来源"
    "source_unavailable" -> "来源不可用"
    "permission_missing" -> "缺少权限"
    "permission_revoked" -> "权限被撤回"
    "bluetooth_off" -> "蓝牙关闭"
    "platform_unavailable" -> "平台不可用"
    "first_sample_timeout" -> "首次样本等待超时"
    "sample_stale_timeout" -> "样本过期超时"
    "unexpected_disconnect" -> "意外断开"
    "connection_timeout" -> "连接超时"
    "measurement_stream_unavailable" -> "测量流不可用"
    "platform_failure" -> "平台故障"
    else -> error("Unexpected device reason: $code")
}
