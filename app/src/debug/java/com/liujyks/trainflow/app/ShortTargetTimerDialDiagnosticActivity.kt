package com.liujyks.trainflow.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.TIMED_COMPOSITION_CURRENT_VERSION
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionStageGroup
import com.liujyks.trainflow.core.model.TimedCompositionTarget
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.feature.workoutsession.TimedWorkoutSessionRoute
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

class ShortTargetTimerDialDiagnosticActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val reduceMotion = intent.getBooleanExtra(EXTRA_REDUCE_MOTION, false)
        setContent {
            TrainFlowTheme(reduceMotion = reduceMotion) {
                TimedWorkoutSessionRoute(
                    plan = shortTargetTimerDialDiagnosticPlan(),
                    onBackToPlans = { finish() },
                    onOpenRecoveryRecommendation = {},
                    onReturnToTrainingHome = { finish() },
                    onRecordWorkoutSession = {}
                )
            }
        }
    }

    companion object {
        const val EXTRA_REDUCE_MOTION = "trainflow.reduce_motion"
    }
}

private fun shortTargetTimerDialDiagnosticPlan(): WorkoutPlan {
    return WorkoutPlan(
        id = "e15-5a-short-target-timerdial-diagnostic",
        mode = WorkoutMode.TIMED,
        title = "E15-5a Short Targets",
        blocks = listOf(
            TimedCompositionBlock(
                id = "e15-5a-composition",
                order = 1,
                title = "Short target motion diagnostic",
                compositionVersion = TIMED_COMPOSITION_CURRENT_VERSION,
                rounds = 1,
                restBetweenRoundsSec = 0,
                stageGroups = listOf(
                    TimedCompositionStageGroup(
                        id = "e15-5a-short-group",
                        order = 1,
                        name = "Short target group",
                        colorHex = TimedStageType.WORK.defaultColorHex,
                        iconKey = TimedStageType.WORK.defaultIconKey,
                        cueSettings = disabledDiagnosticCues(),
                        targets = listOf(
                            TimedCompositionTarget(
                                id = "e15-5a-two-second-work",
                                order = 1,
                                name = "Two second work",
                                kind = TimedCompositionTargetKind.ACTION,
                                durationSec = 2,
                                colorHex = TimedStageType.WORK.defaultColorHex,
                                iconKey = TimedStageType.WORK.defaultIconKey,
                                cueSettings = disabledDiagnosticCues()
                            ),
                            TimedCompositionTarget(
                                id = "e15-5a-one-second-rest",
                                order = 2,
                                name = "One second rest",
                                kind = TimedCompositionTargetKind.REST,
                                durationSec = 1,
                                colorHex = TimedStageType.REST.defaultColorHex,
                                iconKey = TimedStageType.REST.defaultIconKey,
                                cueSettings = disabledDiagnosticCues()
                            )
                        )
                    )
                )
            )
        ),
        preferences = PlanPreferences(cueSettings = disabledDiagnosticCues()),
        createdAt = "2026-07-03T00:00:00Z",
        updatedAt = "2026-07-03T00:00:00Z"
    )
}

private fun disabledDiagnosticCues(): CueSettings {
    return CueSettings(
        actionEnding = CountdownCue(enabled = false),
        restEnding = CountdownCue(enabled = false)
    )
}
