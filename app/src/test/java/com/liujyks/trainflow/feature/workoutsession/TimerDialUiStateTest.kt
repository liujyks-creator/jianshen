package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedWorkoutEngine
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.ui.theme.SkinRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerDialUiStateTest {
    @Test
    fun progressValuesAreClamped() {
        val state = TimerDialUiState.Empty.copy(
            totalProgress = 1.4f,
            currentStageProgress = -0.2f,
            totalRemainingSec = -3,
            currentStageRemainingSec = -1,
            stageSegments = listOf(
                TimerDialStageSegmentUiState(
                    id = "work",
                    label = "Work",
                    stageType = TimerDialStageType.WORK,
                    durationSec = -10,
                    progress = Float.NaN,
                    isCurrent = true
                )
            )
        ).clamped()

        assertEquals(1f, state.totalProgress, 0.0001f)
        assertEquals(0f, state.currentStageProgress, 0.0001f)
        assertEquals(0, state.totalRemainingSec)
        assertEquals(0, state.currentStageRemainingSec)
        assertEquals("00:00", state.totalRemainingText)
        assertEquals("00:00", state.currentStageTimeText)
        assertEquals(0, state.stageSegments.single().durationSec)
        assertEquals(0f, state.stageSegments.single().progress, 0.0001f)
    }

    @Test
    fun productionDefaultVariantIsOfficialFlow() {
        val dial = TimerDialUiState.Empty

        assertEquals(TimerDialVisualVariant.OFFICIAL_FLOW, ProductionTimerDialVisualVariant)
        assertEquals(ProductionTimerDialVisualVariant, dial.visualVariant)
        assertFalse(ProductionTimerDialVisualVariant in PreviewOnlyTimerDialVisualVariants)
    }

    @Test
    fun screenStateMapsProductionTimerDialDefaults() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        val screenState = state.toTimedWorkoutSessionScreenState()
        val dial = screenState.timerDial

        assertEquals(ProductionTimerDialVisualVariant, dial.visualVariant)
        assertEquals(screenState.currentTitle, dial.currentStageLabel)
        assertEquals(screenState.timerText, dial.currentStageTimeText)
        assertEquals(screenState.totalRemainingText, dial.totalRemainingText)
        assertEquals("双击暂停", dial.centerActionLabel)
        assertTrue(dial.canTogglePause)
    }

    @Test
    fun engineStateMapsTotalAndCurrentStageProgress() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(0.4f, dial.currentStageProgress, 0.0001f)
        assertEquals(4f / 15f, dial.totalProgress, 0.0001f)
        assertEquals(11, dial.totalRemainingSec)
        assertEquals(1, dial.currentStageIndex)
        assertEquals(TimerDialStageType.WORK, dial.currentStageType)
        assertEquals(2, dial.stageSegments.size)
        assertTrue(dial.stageSegments.first().isCurrent)
        assertEquals(0.4f, dial.stageSegments.first().progress, 0.0001f)
        assertEquals(0f, dial.stageSegments.last().progress, 0.0001f)
    }

    @Test
    fun completedSegmentsAndRestSemanticsMapAcrossStageSwitch() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 4, restSec = 6)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 5).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(TimerDialStageType.REST, dial.currentStageType)
        assertEquals(1f, dial.stageSegments.first().progress, 0.0001f)
        assertTrue(dial.stageSegments.last().isCurrent)
        assertEquals(1f / 6f, dial.currentStageProgress, 0.0001f)
        assertTrue(dial.stageSegments.last().strokeWidthDp() > dial.stageSegments.first().strokeWidthDp())
    }

    @Test
    fun outerSegmentsOnlyMapCurrentWorkRestCycle() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(twoCycleTimerDialPlan()),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        var dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(2, dial.stageSegments.size)
        assertEquals(TimerDialStageType.WORK, dial.stageSegments.first().stageType)
        assertEquals(TimerDialStageType.REST, dial.stageSegments.last().stageType)
        assertTrue(dial.stageSegments.first().isCurrent)
        assertEquals(0.5f, dial.stageSegments.first().progress, 0.0001f)
        assertEquals(0f, dial.stageSegments.last().progress, 0.0001f)
        assertTrue(dial.stageSegments.first().strokeWidthDp() > dial.stageSegments.last().strokeWidthDp())

        state = TimedWorkoutEngine.tick(state, seconds = 3).state
        dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(2, dial.stageSegments.size)
        assertEquals(TimerDialStageType.WORK, dial.stageSegments.first().stageType)
        assertEquals(TimerDialStageType.REST, dial.stageSegments.last().stageType)
        assertEquals(TimerDialStageType.REST, dial.currentStageType)
        assertEquals(1f, dial.stageSegments.first().progress, 0.0001f)
        assertEquals(0.5f, dial.stageSegments.last().progress, 0.0001f)
        assertTrue(dial.stageSegments.last().isCurrent)
        assertTrue(dial.stageSegments.last().strokeWidthDp() > dial.stageSegments.first().strokeWidthDp())
    }

    @Test
    fun innerProgressMapsWorkoutStageMarkersInsteadOfRawStepCount() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(sevenCycleTimerDialPlan()),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 3 * 60 + 25).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(7, dial.totalWorkoutStageCount)
        assertEquals(3, dial.completedWorkoutStageCount)
        assertEquals(TimerDialStageType.WORK, dial.currentStageType)
        assertEquals(25f / 45f, dial.currentStageProgress, 0.0001f)
        assertEquals((3f + 25f / 60f) / 7f, dial.totalProgress, 0.0001f)
        assertEquals(2, dial.stageSegments.size)
        assertEquals(45, dial.stageSegments.first().durationSec)
        assertEquals(15, dial.stageSegments.last().durationSec)
    }

    @Test
    fun innerProgressHoldsCompletedStageCountDuringRoundRest() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(roundRestTimerDialPlan()),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 6).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(2, dial.totalWorkoutStageCount)
        assertEquals(1, dial.completedWorkoutStageCount)
        assertEquals(0.5f, dial.totalProgress, 0.0001f)
    }

    @Test
    fun restExtensionKeepsOuterAndInnerProgressMonotonic() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 4, restSec = 10)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 9).state
        val beforeExtension = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(TimerDialStageType.REST, beforeExtension.currentStageType)
        assertEquals(0.5f, beforeExtension.currentStageProgress, 0.0001f)

        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ExtendRest(seconds = 15)).state
        val afterExtension = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(afterExtension.currentStageProgress >= beforeExtension.currentStageProgress)
        assertTrue(afterExtension.totalProgress >= beforeExtension.totalProgress)
        assertEquals(beforeExtension.currentStageProgress, afterExtension.currentStageProgress, 0.0001f)
        assertEquals(beforeExtension.totalProgress, afterExtension.totalProgress, 0.0001f)
        assertEquals(
            beforeExtension.stageSegments.last().progress,
            afterExtension.stageSegments.last().progress,
            0.0001f
        )

        state = TimedWorkoutEngine.tick(state, seconds = 1).state
        val afterTick = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(afterTick.currentStageProgress > afterExtension.currentStageProgress)
        assertTrue(afterTick.totalProgress > afterExtension.totalProgress)
        assertTrue(afterTick.stageSegments.last().progress > afterExtension.stageSegments.last().progress)
    }

    @Test
    fun smoothProjectionAfterRestExtensionStaysMonotonic() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 4, restSec = 10)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 9).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ExtendRest(seconds = 15)).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(0.5f, dial.currentStageProgress, 0.0001f)
        assertTrue(dial.projectedStageProgress(elapsedMillis = 500) > dial.currentStageProgress)
        assertTrue(dial.projectedTotalProgress(elapsedMillis = 500) > dial.totalProgress)
        assertTrue(dial.projectedStageProgress(elapsedMillis = 500) >= dial.currentStageProgress)
        assertTrue(dial.projectedTotalProgress(elapsedMillis = 500) >= dial.totalProgress)
    }

    @Test
    fun smoothProjectionAdvancesBetweenEngineSecondTicks() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(0.4f, dial.currentStageProgress, 0.0001f)
        assertEquals(0.45f, dial.projectedStageProgress(elapsedMillis = 500), 0.0001f)
        assertEquals(0.5f, dial.projectedStageProgress(elapsedMillis = 1_000), 0.0001f)
        assertEquals(0.5f, dial.projectedStageProgress(elapsedMillis = 2_500), 0.0001f)
    }

    @Test
    fun smoothProjectionFreezesWhenPaused() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(dial.isPaused)
        assertEquals(dial.currentStageProgress, dial.projectedStageProgress(elapsedMillis = 500), 0.0001f)
        assertEquals(dial.totalProgress, dial.projectedTotalProgress(elapsedMillis = 500), 0.0001f)
    }

    @Test
    fun smoothProjectionFreezesWhenCompleted() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 4, restSec = 0)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertFalse(dial.canTogglePause)
        assertEquals(dial.currentStageProgress, dial.projectedStageProgress(elapsedMillis = 500), 0.0001f)
        assertEquals(dial.totalProgress, dial.projectedTotalProgress(elapsedMillis = 500), 0.0001f)
    }

    @Test
    fun smoothProjectionFreezesWhenAbandoned() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        state = TimedWorkoutEngine.dispatch(
            state,
            WorkoutCommand.EndSession(reason = "user_requested")
        ).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertFalse(dial.canTogglePause)
        assertEquals(dial.currentStageProgress, dial.projectedStageProgress(elapsedMillis = 500), 0.0001f)
        assertEquals(dial.totalProgress, dial.projectedTotalProgress(elapsedMillis = 500), 0.0001f)
    }

    @Test
    fun smoothProjectionAdvancesInnerProgressWithinCurrentCycle() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(4f / 15f, dial.totalProgress, 0.0001f)
        assertEquals(4.5f / 15f, dial.projectedTotalProgress(elapsedMillis = 500), 0.0001f)
    }

    @Test
    fun pausedRestExtensionProgressDoesNotAdvance() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 4, restSec = 10)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 9).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ExtendRest(seconds = 15)).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession).state
        val beforePausedTick = state.toTimedWorkoutSessionScreenState().timerDial

        state = TimedWorkoutEngine.tick(state, seconds = 8).state
        val afterPausedTick = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(afterPausedTick.isPaused)
        assertEquals(beforePausedTick.currentStageProgress, afterPausedTick.currentStageProgress, 0.0001f)
        assertEquals(beforePausedTick.totalProgress, afterPausedTick.totalProgress, 0.0001f)
        assertEquals(
            beforePausedTick.stageSegments.last().progress,
            afterPausedTick.stageSegments.last().progress,
            0.0001f
        )
    }

    @Test
    fun terminalRestExtensionProgressDoesNotAdvance() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 4, restSec = 10)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 9).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ExtendRest(seconds = 15)).state
        state = TimedWorkoutEngine.dispatch(
            state,
            WorkoutCommand.EndSession(reason = "user_requested")
        ).state
        val beforeTerminalTick = state.toTimedWorkoutSessionScreenState().timerDial

        state = TimedWorkoutEngine.tick(state, seconds = 8).state
        val afterTerminalScreenState = state.toTimedWorkoutSessionScreenState()
        val afterTerminalTick = afterTerminalScreenState.timerDial

        assertTrue(afterTerminalScreenState.isTerminal)
        assertEquals(beforeTerminalTick.currentStageProgress, afterTerminalTick.currentStageProgress, 0.0001f)
        assertEquals(beforeTerminalTick.totalProgress, afterTerminalTick.totalProgress, 0.0001f)
        assertEquals(
            beforeTerminalTick.stageSegments.last().progress,
            afterTerminalTick.stageSegments.last().progress,
            0.0001f
        )
    }

    @Test
    fun visualVariantsStayLimitedToThreePrototypeDirections() {
        val variants = TimerDialVisualVariant.entries
        val skinIds = SkinRegistry.skins.map { skin -> skin.id }.toSet()

        assertEquals(3, variants.size)
        assertTrue(TimerDialVisualVariant.BLACK_RED_HIGH_CONTRAST in variants)
        assertTrue(TimerDialVisualVariant.CYBER_NEON in variants)
        assertTrue(TimerDialVisualVariant.OFFICIAL_FLOW in variants)
        assertEquals(
            setOf(TimerDialVisualVariant.BLACK_RED_HIGH_CONTRAST, TimerDialVisualVariant.CYBER_NEON),
            PreviewOnlyTimerDialVisualVariants
        )
        assertFalse("black/red preview must not be a global skin", "black_red_high_contrast" in skinIds)
        assertFalse("cyber neon preview must not be a global skin", "cyber_neon" in skinIds)
        variants.forEach { variant ->
            val tokens = variant.tokens(SkinRegistry.defaultSkin)
            assertTrue(tokens.work != tokens.rest)
            assertTrue(tokens.textPrimary != tokens.pageBackground)
        }
    }

    @Test
    fun layoutSpecsKeepRingsAndCenterSeparatedForBuiltInSkins() {
        SkinRegistry.skins.forEach { skin ->
            val spec = skin.timerDialLayoutSpec()

            assertTrue("${skin.id} dial should stay within its minimum height", spec.keepsDialInsideBounds())
            assertTrue("${skin.id} inner ring should not overlap center", spec.centerClearanceDp >= 16f)
            assertTrue("${skin.id} outer ring should leave a visible inner ring", spec.outerDiameterDp > spec.innerDiameterDp)
        }
    }

    @Test
    fun finalCountdownFlagMapsFromActiveCueOnly() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                timerDialPlan(
                    workSec = 8,
                    restSec = 4,
                    cueSettings = CueSettings(actionEnding = CountdownCue(thresholdSec = 5))
                )
            ),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 3).state
        var dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(dial.isFinalCountdown)

        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession).state
        dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(dial.isPaused)
        assertFalse(dial.isFinalCountdown)
    }

    @Test
    fun finalCountdownVisualFlagRespectsEmphasisAnimationSetting() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(
                timerDialPlan(
                    workSec = 8,
                    restSec = 4,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(
                            thresholdSec = 5,
                            emphasisAnimationEnabled = false
                        )
                    )
                )
            ),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 3).state
        val screenState = state.toTimedWorkoutSessionScreenState()

        assertTrue(screenState.countdownReminder.isActive)
        assertFalse(screenState.countdownReminder.emphasisAnimationEnabled)
        assertFalse(screenState.timerDial.isFinalCountdown)
    }

    @Test
    fun pausedStatePreservesProgressAndOffersResumeAction() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 12, restSec = 4)),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.tick(state, seconds = 5).state
        val activeDial = state.toTimedWorkoutSessionScreenState().timerDial

        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession).state
        state = TimedWorkoutEngine.tick(state, seconds = 9).state
        val pausedDial = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(pausedDial.isPaused)
        assertTrue(pausedDial.canTogglePause)
        assertEquals("双击继续", pausedDial.centerActionLabel)
        assertEquals(activeDial.currentStageProgress, pausedDial.currentStageProgress, 0.0001f)
        assertEquals(activeDial.totalProgress, pausedDial.totalProgress, 0.0001f)
    }

    private fun timerDialPlan(
        workSec: Int,
        restSec: Int,
        cueSettings: CueSettings? = null
    ): WorkoutPlan {
        return WorkoutPlan(
            id = "timer-dial-test",
            mode = WorkoutMode.TIMED,
            title = "Timer Dial Test",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "timer-dial-circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "warm",
                            labelOverride = "Warmup",
                            stageType = TimedStageType.WARMUP,
                            workDurationSec = 0
                        ),
                        TimedExerciseItem(
                            id = "work",
                            labelOverride = "Work",
                            stageType = TimedStageType.WORK,
                            workDurationSec = workSec,
                            restAfterSec = restSec
                        )
                    )
                )
            ),
            preferences = cueSettings?.let { PlanPreferences(cueSettings = it) },
            createdAt = "2026-06-10T00:00:00Z",
            updatedAt = "2026-06-10T00:00:00Z"
        )
    }

    private fun twoCycleTimerDialPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "timer-dial-two-cycle-test",
            mode = WorkoutMode.TIMED,
            title = "Timer Dial Two Cycle Test",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "timer-dial-two-cycle",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "work-1",
                            labelOverride = "Work 1",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 4,
                            restAfterSec = 2
                        ),
                        TimedExerciseItem(
                            id = "work-2",
                            labelOverride = "Work 2",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 5,
                            restAfterSec = 3
                        )
                    )
                )
            ),
            createdAt = "2026-06-10T00:00:00Z",
            updatedAt = "2026-06-10T00:00:00Z"
        )
    }

    private fun sevenCycleTimerDialPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "timer-dial-seven-cycle-test",
            mode = WorkoutMode.TIMED,
            title = "Timer Dial Seven Cycle Test",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "timer-dial-seven-cycle",
                    order = 1,
                    rounds = 1,
                    items = (1..7).map { index ->
                        TimedExerciseItem(
                            id = "work-$index",
                            labelOverride = "Work $index",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 45,
                            restAfterSec = 15
                        )
                    }
                )
            ),
            createdAt = "2026-06-12T00:00:00Z",
            updatedAt = "2026-06-12T00:00:00Z"
        )
    }

    private fun roundRestTimerDialPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "timer-dial-round-rest-test",
            mode = WorkoutMode.TIMED,
            title = "Timer Dial Round Rest Test",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "timer-dial-round-rest",
                    order = 1,
                    rounds = 2,
                    restBetweenRoundsSec = 10,
                    items = listOf(
                        TimedExerciseItem(
                            id = "work",
                            labelOverride = "Work",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 4,
                            restAfterSec = 2
                        )
                    )
                )
            ),
            createdAt = "2026-06-12T00:00:00Z",
            updatedAt = "2026-06-12T00:00:00Z"
        )
    }
}
