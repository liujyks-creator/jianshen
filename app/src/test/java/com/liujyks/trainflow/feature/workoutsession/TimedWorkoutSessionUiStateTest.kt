package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.FollowAlongPlanMeta
import com.liujyks.trainflow.core.model.HeartRateAvailability
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.feature.plans.buildDefaultPlanManagementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedWorkoutSessionUiStateTest {
    @Test
    fun activeWorkStepMapsCurrentActionTimerAndNextStep() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val uiState = started.toTimedWorkoutSessionScreenState()

        assertEquals(plan.title, uiState.planTitle)
        assertEquals("热身", uiState.phaseLabel)
        assertEquals("03:00", uiState.timerText)
        assertTrue(uiState.currentTitle.isNotBlank())
        assertTrue(uiState.nextStepLabel.startsWith("下一步"))
        assertTrue(uiState.shortCue.isNotBlank())
        assertTrue(uiState.shouldShowNextStepPanel)
        assertTrue(uiState.canPause)
        assertTrue(uiState.canSkip)
        assertFalse(uiState.canExtendRest)
    }

    @Test
    fun restStepMapsRestControlAndPreparesNextAction() {
        val plan = buildDefaultPlanManagementState().plans.first()
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        val uiState = state.toTimedWorkoutSessionScreenState()

        assertEquals("休息", uiState.phaseLabel)
        assertTrue(uiState.currentTitle.contains("休息"))
        assertTrue(uiState.shortCue.contains("准备"))
        assertTrue(uiState.canExtendRest)
    }

    @Test
    fun pausedStateFreezesControlsAndOffersResume() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val paused = TimedWorkoutEngine.dispatch(started, WorkoutCommand.PauseSession).state
        val uiState = paused.toTimedWorkoutSessionScreenState()

        assertEquals("已暂停", uiState.phaseLabel)
        assertTrue(uiState.isPaused)
        assertTrue(uiState.canResume)
        assertFalse(uiState.canPause)
        assertFalse(uiState.canSkip)
        assertTrue(uiState.shortCue.contains("冻结"))
    }

    @Test
    fun routeClockTicksPausedStateAndUiShowsPausedElapsedWithoutAdvancingActiveTime() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val paused = TimedWorkoutEngine.dispatch(started, WorkoutCommand.PauseSession).state

        assertTrue(paused.shouldTickTimedRouteClock())

        val pausedAfterWallClock = TimedWorkoutEngine.tick(paused, seconds = 30).state
        val uiState = pausedAfterWallClock.toTimedWorkoutSessionScreenState()

        assertEquals(paused.remainingSec, pausedAfterWallClock.remainingSec)
        assertEquals(paused.activeElapsedSec, pausedAfterWallClock.activeElapsedSec)
        assertEquals(30, uiState.pausedTotalSec)
        assertEquals("30秒", uiState.pausedDurationLabel)
        assertTrue(uiState.historySummaryLabel.contains("累计 30秒"))
    }

    @Test
    fun routeClockDoesNotTickTerminalState() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val completed = TimedWorkoutEngine.tick(
            started,
            seconds = started.steps.sumOf { step -> step.durationSec }
        ).state

        assertFalse(completed.shouldTickTimedRouteClock())
    }

    @Test
    fun heartRatePlaceholderStaysSecondaryAndAbstract() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val uiState = started.toTimedWorkoutSessionScreenState(
            heartRateState = HeartRateState(
                availability = HeartRateAvailability.NOT_CONNECTED
            )
        )

        assertEquals("-- bpm", uiState.heartRate.valueText)
        assertEquals("未连接设备", uiState.heartRate.statusText)
        assertFalse(uiState.heartRate.isAvailable)
    }

    @Test
    fun completedTerminalStateIncludesLightweightSummary() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val completed = TimedWorkoutEngine.tick(
            started,
            seconds = started.steps.sumOf { step -> step.durationSec }
        ).state
        val uiState = completed.toTimedWorkoutSessionScreenState()

        assertTrue(uiState.isTerminal)
        assertEquals("计时训练完成", uiState.terminalTitle)
        assertEquals(
            "已完成 ${completed.completedStepCount} / ${completed.steps.size} 步。" +
                uiState.historySummaryLabel,
            uiState.terminalSummary
        )
        assertFalse(uiState.shouldShowNextStepPanel)
        assertFalse(uiState.canEnd)
    }

    @Test
    fun completedSummaryMapsDurationPhasesRoundsAndRecoveryPlaceholder() {
        val plan = summaryPlan(rounds = 2, restBetweenRoundsSec = 3)
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.tick(
            state,
            seconds = state.steps.sumOf { step -> step.durationSec }
        ).state

        val summary = requireNotNull(state.toTimedWorkoutSessionScreenState().summary)
        val metrics = summary.metricItems.associateBy { metric -> metric.label }

        assertEquals("完成复盘", summary.title)
        assertEquals(TimedWorkoutSummaryTone.COMPLETED, summary.tone)
        assertEquals("21秒", summary.durationLabel)
        assertEquals("21秒", metrics.getValue("总时长").value)
        assertEquals("动作 4 · 休息 3", metrics.getValue("完成阶段").value)
        assertEquals("7 / 7", metrics.getValue("步骤进度").value)
        assertEquals("2 / 2 轮", metrics.getValue("轮次进度").value)
        assertTrue(summary.trainedAreaSummary.contains("全身"))
        assertTrue(summary.trainedAreaSummary.contains("臀部"))
        assertEquals("查看恢复建议", summary.recoveryEntry.title)
        assertTrue(summary.recoveryEntry.enabled)
        assertTrue(summary.recoveryEntry.generated)
        assertFalse(summary.recoveryEntry.description.contains("E5.4"))
        val recovery = requireNotNull(summary.recoveryEntry.recommendation)
        assertTrue(recovery.recommendation.areaIds.contains("lower-body-release"))
        assertTrue(recovery.recommendation.trainedMuscleIds.contains("quads"))
    }

    @Test
    fun skippedSummaryListsSkippedTimedContent() {
        val plan = summaryPlan(rounds = 1)
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        state = TimedWorkoutEngine.tick(
            state,
            seconds = state.steps.sumOf { step -> step.durationSec }
        ).state

        val summary = requireNotNull(state.toTimedWorkoutSessionScreenState().summary)

        assertTrue(summary.skippedSummary.contains("跳过 2 步"))
        assertTrue(summary.skippedSummary.contains("开合跳"))
        assertTrue(summary.skippedSummary.contains("休息"))
        assertEquals("本次已到达完成终态，其中包含主动跳过内容。", summary.earlyEndSummary)
        assertFalse(summary.earlyEndSummary.contains("本次按流程完成"))
        assertEquals("2 步", summary.metricItems.first { it.label == "跳过内容" }.value)
        assertTrue(summary.metricItems.first { it.label == "完成阶段" }.helper.contains("含跳过"))
        assertTrue(summary.metricItems.first { it.label == "轮次进度" }.helper.contains("含跳过"))
    }

    @Test
    fun restExtensionSummaryUsesRestExtensionHistory() {
        val plan = reminderPlan(
            workSec = 2,
            restSec = 5,
            cueSettings = CueSettings(restEnding = CountdownCue(thresholdSec = 1))
        )
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ExtendRest(seconds = 15)).state
        state = TimedWorkoutEngine.tick(state, seconds = 20).state

        val summary = requireNotNull(state.toTimedWorkoutSessionScreenState().summary)

        assertTrue(summary.restExtensionSummary.contains("额外休息 +15秒"))
        assertTrue(summary.restExtensionSummary.contains("共 1 次"))
        assertTrue(summary.restExtensionSummary.contains("开合跳后 +15秒"))
        assertEquals("15秒", summary.metricItems.first { it.label == "延长休息" }.value)
    }

    @Test
    fun abandonedSummaryKeepsNeutralReasonAndProgress() {
        val plan = summaryPlan(rounds = 1)
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        state = TimedWorkoutEngine.dispatch(
            state,
            WorkoutCommand.EndSession(reason = "user_requested")
        ).state

        val summary = requireNotNull(state.toTimedWorkoutSessionScreenState().summary)

        assertEquals("提前结束记录", summary.title)
        assertEquals(TimedWorkoutSummaryTone.ABANDONED, summary.tone)
        assertTrue(summary.earlyEndSummary.contains("原因：用户主动结束"))
        assertTrue(summary.earlyEndSummary.contains("当前步骤已执行 2秒"))
        assertTrue(summary.earlyEndSummary.contains("剩余 2秒"))
        assertFalse(summary.earlyEndSummary.contains("user_requested"))
    }

    @Test
    fun abandonedAfterPartialWorkDoesNotGenerateRecoveryRecommendation() {
        val plan = summaryPlan(rounds = 1)
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        state = TimedWorkoutEngine.dispatch(
            state,
            WorkoutCommand.EndSession(reason = "user_requested")
        ).state

        val summary = requireNotNull(state.toTimedWorkoutSessionScreenState().summary)

        assertEquals("本次未识别到动作部位", summary.trainedAreaSummary)
        assertEquals(
            "本次没有可识别的已完成动作，暂不生成恢复建议。",
            summary.recoveryEntry.description
        )
        assertFalse(summary.recoveryEntry.description.contains("已根据本次完成动作生成"))
        assertFalse(summary.recoveryEntry.enabled)
        assertFalse(summary.recoveryEntry.generated)
        assertEquals(null, summary.recoveryEntry.recommendation)
    }

    @Test
    fun immediatelyAbandonedWorkDoesNotCreateTrainedAreaSummary() {
        val plan = summaryPlan(rounds = 1)
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.dispatch(
            state,
            WorkoutCommand.EndSession(reason = "user_requested")
        ).state

        val summary = requireNotNull(state.toTimedWorkoutSessionScreenState().summary)

        assertEquals("本次未识别到动作部位", summary.trainedAreaSummary)
        assertEquals(
            "本次没有可识别的已完成动作，暂不生成恢复建议。",
            summary.recoveryEntry.description
        )
        assertFalse(summary.recoveryEntry.enabled)
        assertFalse(summary.recoveryEntry.generated)
        assertEquals(null, summary.recoveryEntry.recommendation)
    }

    @Test
    fun emptySkipAndRestExtensionSummaryStayHonest() {
        val plan = reminderPlan(
            workSec = 3,
            cueSettings = CueSettings(actionEnding = CountdownCue(thresholdSec = 1))
        )
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 3).state

        val summary = requireNotNull(state.toTimedWorkoutSessionScreenState().summary)

        assertEquals("没有跳过内容。", summary.skippedSummary)
        assertEquals("没有延长休息。", summary.restExtensionSummary)
        assertTrue(summary.recoveryEntry.generated)
        assertFalse(summary.recoveryEntry.description.contains("E5.4"))
    }

    @Test
    fun userRequestedEarlyEndReasonIsLocalizedInTerminalSummary() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val ended = TimedWorkoutEngine.dispatch(
            started,
            WorkoutCommand.EndSession(reason = "user_requested")
        ).state
        val uiState = ended.toTimedWorkoutSessionScreenState()
        val terminalSummary = requireNotNull(uiState.terminalSummary)

        assertTrue(uiState.isTerminal)
        assertEquals("计时训练已提前结束", uiState.terminalTitle)
        assertTrue(terminalSummary.contains("提前结束"))
        assertTrue(terminalSummary.contains("原因：用户主动结束"))
        assertFalse(terminalSummary.contains("user_requested"))
        assertFalse(uiState.shouldShowNextStepPanel)
        assertFalse(uiState.canEnd)
    }

    @Test
    fun unknownEarlyEndReasonDoesNotLeakInternalToken() {
        val plan = buildDefaultPlanManagementState().plans.first()
        val started = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        val ended = TimedWorkoutEngine.dispatch(
            started,
            WorkoutCommand.EndSession(reason = "internal_debug_token")
        ).state
        val terminalSummary = requireNotNull(
            ended.toTimedWorkoutSessionScreenState().terminalSummary
        )

        assertTrue(terminalSummary.contains("原因：提前结束"))
        assertFalse(terminalSummary.contains("internal_debug_token"))
    }

    @Test
    fun controlHistorySummaryMapsSkippedRestExtensionAndLastControl() {
        val plan = reminderPlan(
            workSec = 2,
            restSec = 5,
            cueSettings = CueSettings(restEnding = CountdownCue(thresholdSec = 1))
        )
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ExtendRest(seconds = 15)).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state
        val uiState = state.toTimedWorkoutSessionScreenState()

        assertEquals(1, uiState.skippedStepCount)
        assertEquals(15, uiState.extendedRestTotalSec)
        assertEquals("跳过当前步骤", uiState.lastControlLabel)
        assertEquals("跳过 1 步，休息延长 15 秒，暂停 0 次，累计 0秒。", uiState.historySummaryLabel)
    }

    @Test
    fun actionEndingReminderMapsCueFlagsAndMessage() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                reminderPlan(
                    workSec = 6,
                    restSec = 4,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(
                            thresholdSec = 3,
                            soundEnabled = false,
                            vibrationEnabled = true,
                            emphasisAnimationEnabled = false
                        ),
                        restEnding = CountdownCue(thresholdSec = 2)
                    )
                )
            ),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 3).state
        val uiState = state.toTimedWorkoutSessionScreenState()

        assertEquals(TimedWorkoutCountdownReminderType.ACTION_ENDING, uiState.countdownReminder.type)
        assertEquals(3, uiState.countdownReminder.remainingSec)
        assertTrue(uiState.countdownReminder.message.contains("阶段即将结束"))
        assertFalse(uiState.countdownReminder.soundEnabled)
        assertTrue(uiState.countdownReminder.vibrationEnabled)
        assertFalse(uiState.countdownReminder.emphasisAnimationEnabled)
        assertTrue(uiState.shortCue.contains("阶段即将结束"))
    }

    @Test
    fun restEndingReminderIsDistinctFromActionEnding() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                reminderPlan(
                    workSec = 2,
                    restSec = 5,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(thresholdSec = 1),
                        restEnding = CountdownCue(
                            thresholdSec = 2,
                            soundEnabled = true,
                            vibrationEnabled = false,
                            emphasisAnimationEnabled = true
                        )
                    )
                )
            ),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        state = TimedWorkoutEngine.tick(state, seconds = 3).state
        val uiState = state.toTimedWorkoutSessionScreenState()

        assertEquals("休息", uiState.phaseLabel)
        assertEquals(TimedWorkoutCountdownReminderType.REST_ENDING, uiState.countdownReminder.type)
        assertEquals(2, uiState.countdownReminder.remainingSec)
        assertTrue(uiState.countdownReminder.message.contains("休息即将结束"))
        assertTrue(uiState.countdownReminder.soundEnabled)
        assertFalse(uiState.countdownReminder.vibrationEnabled)
        assertTrue(uiState.countdownReminder.emphasisAnimationEnabled)
    }

    @Test
    fun actionBasedTimedItemPrefersExerciseNameAndShortCueOverDefaultStageType() {
        val state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                reminderPlan(
                    workSec = 20,
                    cueSettings = CueSettings(actionEnding = CountdownCue(thresholdSec = 5))
                )
            ),
            WorkoutCommand.StartSession
        ).state

        val uiState = state.toTimedWorkoutSessionScreenState()

        assertEquals("开合跳", uiState.currentTitle)
        assertEquals("轻落地，手脚同步打开。", uiState.shortCue)
        assertFalse(uiState.shortCue.contains("工作阶段"))
        assertFalse(uiState.currentTitle.contains("jumping-jacks"))
    }

    @Test
    fun followAlongNullableActionItemKeepsActionSemanticWhenExerciseIdExists() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(followAlongCompatibilityPlan()),
            WorkoutCommand.StartSession
        ).state

        var uiState = state.toTimedWorkoutSessionScreenState()

        assertEquals("开合跳", uiState.currentTitle)
        assertEquals("轻落地，手脚同步打开。", uiState.shortCue)

        state = TimedWorkoutEngine.tick(state, seconds = 5).state
        uiState = state.toTimedWorkoutSessionScreenState()

        assertEquals("节奏保持", uiState.currentTitle)
        assertTrue(uiState.shortCue.contains("自定义阶段"))
    }

    @Test
    fun disabledAndTooLargeThresholdsDoNotCreateReminderState() {
        var disabledState = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                reminderPlan(
                    workSec = 4,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(enabled = false, thresholdSec = 3)
                    )
                )
            ),
            WorkoutCommand.StartSession
        ).state

        disabledState = TimedWorkoutEngine.tick(disabledState, seconds = 2).state
        assertFalse(disabledState.toTimedWorkoutSessionScreenState().countdownReminder.isActive)

        var shortDurationState = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                reminderPlan(
                    workSec = 3,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(thresholdSec = 5)
                    )
                )
            ),
            WorkoutCommand.StartSession
        ).state

        shortDurationState = TimedWorkoutEngine.tick(shortDurationState).state
        assertFalse(shortDurationState.toTimedWorkoutSessionScreenState().countdownReminder.isActive)
    }

    private fun reminderPlan(
        workSec: Int,
        restSec: Int? = null,
        cueSettings: CueSettings
    ): WorkoutPlan {
        return WorkoutPlan(
            id = "plan-reminder-test",
            mode = WorkoutMode.TIMED,
            title = "Reminder test",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "jump",
                            exerciseId = "jumping-jacks",
                            workDurationSec = workSec,
                            restAfterSec = restSec
                        )
                    )
                )
            ),
            preferences = PlanPreferences(cueSettings = cueSettings),
            createdAt = "2026-05-30T00:00:00Z",
            updatedAt = "2026-05-30T00:00:00Z"
        )
    }

    private fun summaryPlan(
        rounds: Int,
        restBetweenRoundsSec: Int? = null
    ): WorkoutPlan {
        return WorkoutPlan(
            id = "plan-summary-test",
            mode = WorkoutMode.TIMED,
            title = "Summary test",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "summary-circuit",
                    order = 1,
                    rounds = rounds,
                    restBetweenRoundsSec = restBetweenRoundsSec,
                    items = listOf(
                        TimedExerciseItem(
                            id = "jump",
                            exerciseId = "jumping-jacks",
                            workDurationSec = 4,
                            restAfterSec = 2
                        ),
                        TimedExerciseItem(
                            id = "squat",
                            exerciseId = "bodyweight-squat",
                            workDurationSec = 3
                        )
                    )
                )
            ),
            createdAt = "2026-05-30T00:00:00Z",
            updatedAt = "2026-05-30T00:00:00Z"
        )
    }

    private fun followAlongCompatibilityPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "plan-follow-along-compat",
            mode = WorkoutMode.FOLLOW_ALONG,
            title = "Follow along compat",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "follow-circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "jump",
                            exerciseId = "jumping-jacks",
                            workDurationSec = 5,
                            autoAdvance = true
                        ),
                        TimedExerciseItem(
                            id = "hold",
                            exerciseId = null,
                            labelOverride = "节奏保持",
                            stageType = TimedStageType.CUSTOM,
                            workDurationSec = 5,
                            autoAdvance = true
                        )
                    )
                )
            ),
            followAlong = FollowAlongPlanMeta(preset = true),
            createdAt = "2026-05-30T00:00:00Z",
            updatedAt = "2026-05-30T00:00:00Z"
        )
    }
}
