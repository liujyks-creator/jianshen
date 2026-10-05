package com.liujyks.trainflow.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liujyks.trainflow.ui.theme.SkinRegistry
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
import java.util.concurrent.CountDownLatch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutSessionHeartRateCardContractTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun rendersStateMatrixAndVisibleRetry() {
        val hidden = WorkoutSessionHeartRateCardUiState.Hidden
        val error = WorkoutSessionHeartRateCardUiState.ReadError(null, IllegalStateException("read-failure"))
        val facts = listOf("已记录样本数 2", "暂停时长 1:20", "额外休息 0:20")
        fun content(status: String, coverage: String, percent: Int?, messages: List<String>,
            metrics: List<String> = emptyList(), sample: String = "primary_points_available",
            zone: String = "available", recordFacts: List<String> = facts) =
            WorkoutSessionHeartRateCardUiState.Content(status, sample, coverage, zone, percent,
                messages, metrics, recordFacts)
        val partialNoZones = content("partial", "partial", 79, listOf(
            "本次心率记录不完整，以下结果仅基于已记录片段。", "本次未设置最大心率，无法计算区间。"),
            listOf("已记录片段平均 112 bpm", "已记录片段最高 151 bpm"), zone = "unavailable_no_effective_max")
        val insufficientNoZones = content("insufficient", "insufficient", 49, listOf(
            "本次有效心率覆盖不足 50%，暂不生成自动摘要。", "本次未设置最大心率，无法计算区间。"),
            listOf("已记录片段平均 112 bpm", "已记录片段最高 151 bpm"), zone = "unavailable_no_effective_max")
        val humanRows = listOf(partialNoZones, insufficientNoZones, error)
        var state by mutableStateOf<WorkoutSessionHeartRateCardUiState>(hidden)
        var skin by mutableStateOf("official_flow")
        var human by mutableStateOf(false)
        var row by mutableStateOf(0)
        var feedback by mutableStateOf("")
        var automaticRequests = 0
        var humanRequests = 0
        val finished = CountDownLatch(1)
        compose.setContent {
            TrainFlowTheme(skin = SkinRegistry.resolve(skin)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (human) {
                        Text("测试演示 · ${row + 1}/3")
                        Button(onClick = { skin = "official_flow" }) { Text("普通皮肤") }
                        Button(onClick = { skin = "big_type" }) { Text("大字皮肤") }
                    }
                    WorkoutSessionHeartRateCard(state, onRetry = {
                        if (human) {
                            humanRequests += 1
                            feedback = "收到重试操作"
                        } else automaticRequests += 1
                    })
                    if (human) {
                        if (feedback.isNotEmpty()) Text(feedback)
                        Button(onClick = {
                            if (row < 2) {
                                row += 1
                                state = humanRows[row]
                                feedback = ""
                            }
                        }, enabled = row < 2) { Text("下一项") }
                        Button(onClick = {
                            println("H1_HOST_COMPLETED / row=${row + 1} / skin=$skin / retryRequests=$humanRequests")
                            finished.countDown()
                        }) { Text("结束验收") }
                        Button(onClick = {
                            println("H1_HOST_STOPPED / row=${row + 1} / skin=$skin / retryRequests=$humanRequests")
                            finished.countDown()
                        }) { Text("停止验收") }
                    }
                }
            }
        }
        fun show(input: WorkoutSessionHeartRateCardUiState, vararg expected: String) {
            compose.runOnIdle { state = input }
            if (input == hidden) compose.onNodeWithText("主要训练心率").assertDoesNotExist()
            else compose.onNodeWithText("主要训练心率").performScrollTo().assertIsDisplayed()
            expected.forEach { compose.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
            compose.onNodeWithText("查看心率分析").assertDoesNotExist()
            compose.onNodeWithText("心率曲线").assertDoesNotExist()
            compose.onNodeWithText("平均 0 bpm").assertDoesNotExist()
            compose.onNodeWithText("最高 0 bpm").assertDoesNotExist()
            val metrics = (input as? WorkoutSessionHeartRateCardUiState.Content)?.metrics
                ?.count { it.endsWith("bpm") } ?: 0
            compose.onAllNodesWithText("bpm", substring = true).assertCountEquals(metrics)
        }
        // Hidden inputs represent both legal no-HR identities and NotFound at this rendering boundary.
        show(hidden)
        show(WorkoutSessionHeartRateCardUiState.Unavailable("本次心率记录尚未形成终态分析。"),
            "本次心率记录尚未形成终态分析。")
        show(content("no_eligible_duration", "no_eligible_duration", null,
            listOf("本次没有可纳入主要训练心率统计的时长。"), sample = "no_canonical_samples",
            recordFacts = listOf("已记录样本数 0", "暂停时长 1:20", "额外休息 0:20")),
            "本次没有可纳入主要训练心率统计的时长。")
        show(content("zero_samples", "insufficient", 0,
            listOf("已开启心率记录，但本次未获得有效心率数据。"), sample = "no_canonical_samples",
            recordFacts = listOf("已记录样本数 0", "暂停时长 1:20", "额外休息 0:20")),
            "已开启心率记录，但本次未获得有效心率数据。")
        show(content("insufficient", "insufficient", 0, listOf(
            "本次有效心率覆盖不足 50%，暂不生成自动摘要。", "已记录心率均不计入主要训练统计。"),
            listOf("未获得可统计的平均", "未获得可统计的最高"), sample = "canonical_only_excluded"),
            "本次有效心率覆盖不足 50%，暂不生成自动摘要。", "已记录心率均不计入主要训练统计。",
            "未获得可统计的平均", "未获得可统计的最高")
        show(content("insufficient", "insufficient", 49,
            listOf("本次有效心率覆盖不足 50%，暂不生成自动摘要。"),
            listOf("已记录片段平均 112 bpm", "已记录片段最高 151 bpm")),
            "本次有效心率覆盖不足 50%，暂不生成自动摘要。", "已记录片段平均 112 bpm", "已记录片段最高 151 bpm")
        show(content("partial", "partial", 50,
            listOf("本次心率记录不完整，以下结果仅基于已记录片段。"),
            listOf("已记录片段平均 112 bpm", "已记录片段最高 151 bpm")),
            "本次心率记录不完整，以下结果仅基于已记录片段。", "已记录片段平均 112 bpm", "已记录片段最高 151 bpm")
        show(partialNoZones, "本次心率记录不完整，以下结果仅基于已记录片段。",
            "本次未设置最大心率，无法计算区间。", "已记录片段平均 112 bpm", "已记录片段最高 151 bpm",
            "暂停时长 1:20", "额外休息 0:20")
        show(insufficientNoZones, "本次有效心率覆盖不足 50%，暂不生成自动摘要。",
            "本次未设置最大心率，无法计算区间。", "已记录片段平均 112 bpm", "已记录片段最高 151 bpm")
        show(content("recorded", "normal", 80, listOf("本次有效心率覆盖 80%。"),
            listOf("平均 112 bpm", "最高 151 bpm")), "本次有效心率覆盖 80%。", "平均 112 bpm", "最高 151 bpm")
        show(content("recorded_no_zones", "normal", 80,
            listOf("本次有效心率覆盖 80%。", "本次未设置最大心率，无法计算区间。"),
            listOf("平均 112 bpm", "最高 151 bpm"), zone = "unavailable_no_effective_max"),
            "本次有效心率覆盖 80%。", "本次未设置最大心率，无法计算区间。", "平均 112 bpm", "最高 151 bpm")
        show(content("insufficient", "insufficient", 0,
            listOf("本次有效心率覆盖不足 50%，暂不生成自动摘要。"),
            listOf("未获得可统计的平均", "已记录片段最高 151 bpm")),
            "未获得可统计的平均", "已记录片段最高 151 bpm")
        show(error, "暂时无法读取本次心率数据", "重试")
        compose.onNodeWithText("重试").performScrollTo().assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, automaticRequests) }
        compose.runOnIdle {
            state = partialNoZones
            human = true
        }
        println("V2_AUTOMATED_CHECKS_COMPLETE / H1_READY")
        finished.await()
    }
}
