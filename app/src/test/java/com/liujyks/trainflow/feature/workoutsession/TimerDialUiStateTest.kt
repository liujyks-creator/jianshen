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
import com.liujyks.trainflow.feature.plans.PlanManagementScreenState
import com.liujyks.trainflow.feature.plans.buildDefaultTimedPlanEditorState
import com.liujyks.trainflow.feature.plans.selectPlan
import com.liujyks.trainflow.ui.theme.SkinRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
                    isCurrent = true,
                    colorHex = "not-a-color"
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
        assertEquals("#F26B4F", state.stageSegments.single().colorHex)
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
        assertEquals("暂停训练", dial.centerActionLabel)
        assertTrue(dial.canTogglePause)
    }

    @Test
    fun timerDialConsumesCustomStageColorAndTextColorFromPlan() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5, workColorHex = "#FFC107")),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        val screenState = state.toTimedWorkoutSessionScreenState()
        val dial = screenState.timerDial

        assertEquals("#FFC107", screenState.stageColorHex)
        assertEquals("#FFC107", dial.currentStageColorHex)
        assertEquals("#111820", dial.currentStageTextColorHex)
        assertEquals("#FFC107", dial.stageSegments.first().colorHex)
    }

    @Test
    fun timerDialFallsBackWhenStageColorIsInvalid() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5, workColorHex = "bad-color")),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals("#F26B4F", dial.currentStageColorHex)
        assertEquals("#F26B4F", dial.stageSegments.first().colorHex)
    }

    @Test
    fun planDetailReadyExecutionAndTimerDialConsumeBoundaryStageColors() {
        val plan = boundaryColoredEditorPlan()
        val detail = requireNotNull(
            PlanManagementScreenState(plans = listOf(plan))
                .selectPlan(plan.id)
                .selectedDetail
        )
        val ready = TimedWorkoutEngine.create(plan)
        val readyGate = requireNotNull(ready.toTimedReadyStartGateUiState())
        val started = ready.startTimedSessionFromReadyGate().state
        val warmupScreen = started.toTimedWorkoutSessionScreenState()

        assertTrue(detail.canStartTraining)
        assertEquals("开始计时训练", detail.startStatus)
        assertEquals(plan.title, readyGate.planTitle)
        assertEquals("#00BCD4", warmupScreen.stageColorHex)
        assertEquals("mobility", warmupScreen.stageIconKey)
        assertEquals("#00BCD4", warmupScreen.timerDial.currentStageColorHex)
        assertEquals("#00BCD4", warmupScreen.timerDial.stageSegments.single().colorHex)
        assertEquals("#111820", warmupScreen.timerDial.currentStageTextColorHex)

        val secondsBeforeCooldown = started.steps
            .takeWhile { step -> step.stageType != TimedStageType.COOLDOWN }
            .sumOf { step -> step.durationSec }
        val cooldown = TimedWorkoutEngine.tick(started, seconds = secondsBeforeCooldown).state
        val cooldownScreen = cooldown.toTimedWorkoutSessionScreenState()

        assertEquals(TimedStageType.COOLDOWN, cooldown.currentStep?.stageType)
        assertEquals("#FFC107", cooldownScreen.stageColorHex)
        assertEquals("moon", cooldownScreen.stageIconKey)
        assertEquals("#FFC107", cooldownScreen.timerDial.currentStageColorHex)
        assertEquals("#FFC107", cooldownScreen.timerDial.stageSegments.single().colorHex)
        assertEquals("#111820", cooldownScreen.timerDial.currentStageTextColorHex)
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
    fun innerBaseDotsAndStageMarkersShareDynamicMarkerData() {
        val dial = TimerDialUiState.Empty.copy(
            totalWorkoutStageCount = 7,
            completedWorkoutStageCount = 3
        ).clamped()

        val markers = dial.innerMarkerData()

        assertEquals(7, markers.size)
        markers.forEachIndexed { index, marker ->
            assertEquals(index, marker.index)
            assertEquals(timerDialMarkerProgress(index, 7), marker.progress, 0.0001f)
        }
        assertEquals(TimerDialInnerMarkerRole.TOTAL_COUNT, markers[0].role)
        assertEquals("7", markers[0].label)
        assertEquals(TimerDialInnerMarkerRole.COMPLETED_DOT, markers[1].role)
        assertEquals(TimerDialInnerMarkerRole.COMPLETED_DOT, markers[2].role)
        assertEquals(TimerDialInnerMarkerRole.LATEST_COMPLETED, markers[3].role)
        assertEquals("3", markers[3].label)
        assertEquals(TimerDialInnerMarkerRole.BASE_DOT, markers[4].role)
    }

    @Test
    fun innerMarkerDataChangesWithWorkoutStageCountAndRounds() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(twoCycleTimerDialPlan()),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        val twoCycleMarkers = state.toTimedWorkoutSessionScreenState().timerDial.innerMarkerData()

        state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(sevenCycleTimerDialPlan()),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.tick(state, seconds = 3 * 60 + 25).state
        val sevenCycleMarkers = state.toTimedWorkoutSessionScreenState().timerDial.innerMarkerData()

        assertEquals(2, twoCycleMarkers.size)
        assertEquals(7, sevenCycleMarkers.size)
        assertEquals(0.5f, twoCycleMarkers[1].progress, 0.0001f)
        assertEquals(1f / 7f, sevenCycleMarkers[1].progress, 0.0001f)
    }

    @Test
    fun markerAndRingLayerSemanticsKeepBaseBelowProgressAndMarkersAboveRings() {
        assertTrue(TimerDialDrawLayer.OUTER_TRACK.ordinal < TimerDialDrawLayer.OUTER_PROGRESS.ordinal)
        assertTrue(TimerDialDrawLayer.INNER_BASE_RING.ordinal < TimerDialDrawLayer.INNER_TOTAL_PROGRESS.ordinal)
        assertTrue(TimerDialDrawLayer.INNER_BASE_DOT.ordinal < TimerDialDrawLayer.INNER_STAGE_MARKER.ordinal)
        assertTrue(TimerDialDrawLayer.INNER_TOTAL_PROGRESS.ordinal < TimerDialDrawLayer.INNER_STAGE_MARKER.ordinal)
        assertTrue(TimerDialDrawLayer.FINAL_COUNTDOWN.ordinal < TimerDialDrawLayer.CENTER_SURFACE.ordinal)

        SkinRegistry.skins.forEach { skin ->
            val spec = skin.timerDialLayoutSpec()
            val innerRadius = spec.innerDiameterDp / 2f
            val centerRadius = spec.centerSizeDp / 2f
            val outerInnerEdge = spec.outerDiameterDp / 2f - spec.outerMaxStrokeDp / 2f
            val centerGap = innerRadius - spec.innerMarkerRadiusDp - centerRadius
            val outerGap = outerInnerEdge - (innerRadius + spec.innerMarkerRadiusDp)
            val centerBaseRingGap = innerRadius - spec.innerBaseStrokeDp / 2f - centerRadius
            val outerBaseRingGap = outerInnerEdge - (innerRadius + spec.innerBaseStrokeDp / 2f)
            val markerInternalGap = spec.innerMarkerBoundaryRadiusDp - spec.innerBaseDotRadiusDp

            assertTrue("${skin.id} base ring should sit under the thin total line", spec.innerBaseStrokeDp > spec.innerStrokeDp)
            assertTrue("${skin.id} base dots should stay lighter than numbered markers", spec.innerBaseDotRadiusDp < spec.innerMarkerRadiusDp)
            assertTrue("${skin.id} marker should keep visible space from center circle", centerGap >= 10f)
            assertTrue("${skin.id} marker should keep visible space from outer ring", outerGap >= 10f)
            assertTrue("${skin.id} wide base ring should not touch center circle", centerBaseRingGap >= 16f)
            assertTrue("${skin.id} wide base ring should not touch outer ring", outerBaseRingGap >= 16f)
            assertTrue("${skin.id} marker internals should stay inside the base ring boundary", markerInternalGap >= 3.5f)
            assertTrue(
                "${skin.id} completed dots should stay inside the base ring boundary",
                spec.innerMarkerBoundaryRadiusDp - spec.innerCompletedDotRadiusDp >= 3.5f
            )
        }
    }

    @Test
    fun officialFusionProductionTokensComeFromSkinTokens() {
        val skin = SkinRegistry.defaultSkin
        val tokens = TimerDialVisualVariant.OFFICIAL_FLOW.tokens(skin)

        assertEquals(skin.tokens.primary, tokens.pageBackground)
        assertEquals(skin.tokens.secondary, tokens.dialSurface)
        assertEquals(skin.tokens.action, tokens.work)
        assertEquals(skin.tokens.focus, tokens.rest)
        assertEquals(skin.tokens.accent, tokens.warmup)
        assertEquals(skin.tokens.neutral50, tokens.totalProgress)
        assertEquals(skin.tokens.neutral700.copy(alpha = 0.34f), tokens.innerBaseRing)
        assertEquals(skin.tokens.neutral100.copy(alpha = 0.3f), tokens.innerBaseDot)
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
    fun pendingAnchorUpdateDoesNotReuseStaleProjectionElapsed() {
        assertEquals(
            0L,
            timerDialSmoothProgressElapsedMillis(
                frameNanos = 2_000_000_000L,
                anchorNanos = 1_000_000_000L,
                anchorApplied = false
            )
        )
        assertEquals(
            TimerDialSmoothProgressMaxMillis,
            timerDialSmoothProgressElapsedMillis(
                frameNanos = 2_000_000_000L,
                anchorNanos = 1_000_000_000L,
                anchorApplied = true
            )
        )
    }

    @Test
    fun displayedProgressHoldsAtStageStartUntilFirstEngineTick() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state
        val initialDial = state.toTimedWorkoutSessionScreenState().timerDial
        val initialDisplayed = initialDial.monotonicDisplayedProgress(elapsedMillis = 500)

        assertTrue(initialDial.shouldHoldInitialSmoothProgress())
        assertTrue(initialDial.projectedStageProgress(elapsedMillis = 500) > initialDial.currentStageProgress)
        assertEquals(initialDial.currentStageProgress, initialDisplayed.currentStageProgress, 0.0001f)
        assertEquals(initialDial.totalProgress, initialDisplayed.totalProgress, 0.0001f)

        state = TimedWorkoutEngine.tick(state, seconds = 1).state
        val afterTickDial = state.toTimedWorkoutSessionScreenState().timerDial
        val afterTickDisplayed = afterTickDial.monotonicDisplayedProgress(elapsedMillis = 500)

        assertFalse(afterTickDial.shouldHoldInitialSmoothProgress())
        assertTrue(afterTickDisplayed.currentStageProgress > afterTickDial.currentStageProgress)
    }

    @Test
    fun displayedProgressHoldsAfterSkipIntoNextStageUntilFirstEngineTick() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state
        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.SkipStep).state

        val skippedDial = state.toTimedWorkoutSessionScreenState().timerDial
        val skippedDisplayed = skippedDial.monotonicDisplayedProgress(elapsedMillis = 500)

        assertEquals(TimerDialStageType.REST, skippedDial.currentStageType)
        assertEquals(0f, skippedDial.currentStageProgress, 0.0001f)
        assertTrue(skippedDial.shouldHoldInitialSmoothProgress())
        assertTrue(skippedDial.projectedStageProgress(elapsedMillis = 500) > skippedDial.currentStageProgress)
        assertEquals(skippedDial.currentStageProgress, skippedDisplayed.currentStageProgress, 0.0001f)
        assertEquals(skippedDial.totalProgress, skippedDisplayed.totalProgress, 0.0001f)
    }

    @Test
    fun smoothProgressIdentityStaysStableAcrossSameStageSecondTicks() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        val beforeTick = state.toTimedWorkoutSessionScreenState().timerDial

        state = TimedWorkoutEngine.tick(state, seconds = 1).state
        val afterTick = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(TimerDialStageType.WORK, beforeTick.currentStageType)
        assertEquals(TimerDialStageType.WORK, afterTick.currentStageType)
        assertEquals(0.4f, beforeTick.currentStageProgress, 0.0001f)
        assertEquals(0.5f, afterTick.currentStageProgress, 0.0001f)
        assertEquals(6, beforeTick.currentStageRemainingSec)
        assertEquals(5, afterTick.currentStageRemainingSec)
        assertEquals(beforeTick.smoothProgressIdentity(), afterTick.smoothProgressIdentity())
        assertNotEquals(beforeTick.smoothProgressAnchor(), afterTick.smoothProgressAnchor())
    }

    @Test
    fun sameStageAnchorUpdateCannotReduceDisplayedActiveProgress() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        val beforeTick = state.toTimedWorkoutSessionScreenState().timerDial
        val displayedBeforeTick = TimerDialDisplayedProgress(
            totalProgress = beforeTick.projectedTotalProgress(elapsedMillis = 1_000),
            currentStageProgress = beforeTick.projectedStageProgress(elapsedMillis = 1_000)
        )

        state = TimedWorkoutEngine.tick(state, seconds = 1).state
        val afterTick = state.toTimedWorkoutSessionScreenState().timerDial
        val displayedAfterTick = afterTick.monotonicDisplayedProgress(
            elapsedMillis = 0,
            previousDisplayed = displayedBeforeTick
        )

        assertEquals(beforeTick.smoothProgressIdentity(), afterTick.smoothProgressIdentity())
        assertTrue(displayedAfterTick.currentStageProgress >= displayedBeforeTick.currentStageProgress)
        assertTrue(displayedAfterTick.totalProgress >= displayedBeforeTick.totalProgress)
        assertEquals(
            displayedAfterTick.currentStageProgress,
            afterTick.stageSegments.single { segment -> segment.isCurrent }.progress,
            0.0001f
        )
    }

    @Test
    fun smoothProgressIdentityChangesWhenCurrentSegmentChanges() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 4, restSec = 6)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 3).state
        val beforeSwitch = state.toTimedWorkoutSessionScreenState().timerDial

        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        val afterSwitch = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(TimerDialStageType.WORK, beforeSwitch.currentStageType)
        assertEquals(TimerDialStageType.REST, afterSwitch.currentStageType)
        assertNotEquals(beforeSwitch.smoothProgressIdentity(), afterSwitch.smoothProgressIdentity())
    }

    @Test
    fun stageIdentityChangeCanResetDisplayedProgress() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 4, restSec = 6)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 3).state
        val beforeSwitch = state.toTimedWorkoutSessionScreenState().timerDial
        val displayedBeforeSwitch = beforeSwitch.monotonicDisplayedProgress(
            elapsedMillis = 0,
            previousDisplayed = null
        )

        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        val afterSwitch = state.toTimedWorkoutSessionScreenState().timerDial
        val displayedAfterSwitch = afterSwitch.monotonicDisplayedProgress(
            elapsedMillis = 0,
            previousDisplayed = null
        )

        assertNotEquals(beforeSwitch.smoothProgressIdentity(), afterSwitch.smoothProgressIdentity())
        assertTrue(displayedAfterSwitch.currentStageProgress < displayedBeforeSwitch.currentStageProgress)
    }

    @Test
    fun smoothProgressIdentityFreezesWhenPausedAndResumesOnSameSegment() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        val activeDial = state.toTimedWorkoutSessionScreenState().timerDial

        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession).state
        val pausedDial = state.toTimedWorkoutSessionScreenState().timerDial

        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ResumeSession).state
        val resumedDial = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(activeDial.canProjectSmoothProgress(reduceMotion = false))
        assertFalse(pausedDial.canProjectSmoothProgress(reduceMotion = false))
        assertNotEquals(activeDial.smoothProgressIdentity(), pausedDial.smoothProgressIdentity())
        assertEquals(activeDial.smoothProgressIdentity(), resumedDial.smoothProgressIdentity())
        assertEquals(activeDial.currentStageProgress, resumedDial.currentStageProgress, 0.0001f)
    }

    @Test
    fun reduceMotionDisablesSmoothProjectionAndKeepsSecondTickState() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertTrue(dial.canProjectSmoothProgress(reduceMotion = false))
        assertFalse(dial.canProjectSmoothProgress(reduceMotion = true))
        assertEquals(0.4f, dial.currentStageProgress, 0.0001f)
        assertEquals(
            dial.currentStageProgress,
            dial.projectedStageProgress(elapsedMillis = 500, reduceMotion = true),
            0.0001f
        )
        assertEquals(
            dial.totalProgress,
            dial.projectedTotalProgress(elapsedMillis = 500, reduceMotion = true),
            0.0001f
        )
        assertTrue(dial.projectedStageProgress(elapsedMillis = 500, reduceMotion = false) > dial.currentStageProgress)
        assertTrue(dial.projectedTotalProgress(elapsedMillis = 500, reduceMotion = false) > dial.totalProgress)
    }

    @Test
    fun reduceMotionDisplayedProgressRemainsDiscreteEvenWithPreviousFloor() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial
        val displayed = dial.monotonicDisplayedProgress(
            elapsedMillis = 500,
            reduceMotion = true,
            previousDisplayed = TimerDialDisplayedProgress(
                totalProgress = 0.8f,
                currentStageProgress = 0.8f
            )
        )

        assertEquals(dial.currentStageProgress, displayed.currentStageProgress, 0.0001f)
        assertEquals(dial.totalProgress, displayed.totalProgress, 0.0001f)
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
    fun pausedDisplayedProgressFreezeIgnoresPreviousRunningFloor() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 10, restSec = 5)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 4).state
        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.PauseSession).state
        val dial = state.toTimedWorkoutSessionScreenState().timerDial
        val displayed = dial.monotonicDisplayedProgress(
            elapsedMillis = 500,
            previousDisplayed = TimerDialDisplayedProgress(
                totalProgress = 0.8f,
                currentStageProgress = 0.8f
            )
        )

        assertTrue(dial.isPaused)
        assertEquals(dial.currentStageProgress, displayed.currentStageProgress, 0.0001f)
        assertEquals(dial.totalProgress, displayed.totalProgress, 0.0001f)
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
    fun terminalDisplayedProgressFreezeIgnoresPreviousRunningFloor() {
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
        val displayed = dial.monotonicDisplayedProgress(
            elapsedMillis = 500,
            previousDisplayed = TimerDialDisplayedProgress(
                totalProgress = 0.8f,
                currentStageProgress = 0.8f
            )
        )

        assertFalse(dial.canTogglePause)
        assertEquals(dial.currentStageProgress, displayed.currentStageProgress, 0.0001f)
        assertEquals(dial.totalProgress, displayed.totalProgress, 0.0001f)
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
    fun restExtensionDisplayedProgressDoesNotMoveBackwardFromPreviousProjection() {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(timerDialPlan(workSec = 4, restSec = 10)),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 9).state
        val beforeExtension = state.toTimedWorkoutSessionScreenState().timerDial
        val displayedBeforeExtension = beforeExtension.monotonicDisplayedProgress(
            elapsedMillis = 1_000,
            previousDisplayed = null
        )

        state = TimedWorkoutEngine.dispatch(state, WorkoutCommand.ExtendRest(seconds = 15)).state
        val afterExtension = state.toTimedWorkoutSessionScreenState().timerDial
        val displayedAfterExtension = afterExtension.monotonicDisplayedProgress(
            elapsedMillis = 0,
            previousDisplayed = displayedBeforeExtension
        )

        assertEquals(beforeExtension.smoothProgressIdentity(), afterExtension.smoothProgressIdentity())
        assertTrue(displayedAfterExtension.currentStageProgress >= displayedBeforeExtension.currentStageProgress)
        assertTrue(displayedAfterExtension.totalProgress >= displayedBeforeExtension.totalProgress)
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
            assertTrue("${skin.id} inner ring should not overlap center", spec.centerClearanceDp >= 48f)
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
                    cueSettings = CueSettings(actionEnding = CountdownCue(thresholdSec = 6))
                )
            ),
            WorkoutCommand.StartSession
        ).state

        state = TimedWorkoutEngine.tick(state, seconds = 2).state
        var dial = state.toTimedWorkoutSessionScreenState().timerDial

        assertEquals(6, dial.currentStageRemainingSec)
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
        assertEquals("继续训练", pausedDial.centerActionLabel)
        assertTrue(pausedDial.accessibilityDescription().contains("继续训练"))
        assertTrue(pausedDial.accessibilityDescription().contains("已暂停"))
        assertEquals(activeDial.currentStageProgress, pausedDial.currentStageProgress, 0.0001f)
        assertEquals(activeDial.totalProgress, pausedDial.totalProgress, 0.0001f)
    }

    private fun timerDialPlan(
        workSec: Int,
        restSec: Int,
        cueSettings: CueSettings? = null,
        workColorHex: String = TimedStageType.WORK.defaultColorHex
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
                            colorHex = workColorHex,
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

    private fun boundaryColoredEditorPlan(): WorkoutPlan {
        val editor = buildDefaultTimedPlanEditorState()
        val compactStages = editor.stages.map { stage ->
            when (stage.stageType) {
                TimedStageType.WARMUP -> stage.copy(
                    iconKey = "mobility",
                    colorHex = "#00BCD4",
                    durationSec = 5
                )
                TimedStageType.COOLDOWN -> stage.copy(
                    iconKey = "moon",
                    colorHex = "#FFC107",
                    durationSec = 5
                )
                else -> stage.copy(durationSec = 5)
            }
        }

        return editor.copy(
            title = "Boundary Color Editor Plan",
            rounds = 1,
            restBetweenRoundsSec = 0,
            stages = compactStages
        ).toWorkoutPlan(
            planId = "boundary-color-editor-plan",
            timestamp = "2026-06-15T00:00:00Z"
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
