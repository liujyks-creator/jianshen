package com.liujyks.trainflow.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.liujyks.trainflow.core.data.WorkoutPlanRepository
import com.liujyks.trainflow.core.data.WorkoutSessionRepository
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.datastore.TrainFlowPreferences
import com.liujyks.trainflow.core.datastore.TrainFlowPreferencesDataSource
import com.liujyks.trainflow.core.datastore.trainFlowPreferencesDataStore
import com.liujyks.trainflow.ui.shell.official.TrainFlowApp
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
import com.liujyks.trainflow.ui.theme.rememberTrainFlowReduceMotion
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val preferencesDataSource = remember(context) {
                TrainFlowPreferencesDataSource(context.trainFlowPreferencesDataStore)
            }
            val trainFlowDatabase = remember(context) {
                TrainFlowDatabase.create(context.applicationContext)
            }
            val workoutPlanRepository = remember(trainFlowDatabase) {
                WorkoutPlanRepository(trainFlowDatabase)
            }
            val workoutSessionRepository = remember(trainFlowDatabase) {
                WorkoutSessionRepository(trainFlowDatabase)
            }
            val preferences by preferencesDataSource.preferences.collectAsState(
                initial = TrainFlowPreferences()
            )
            val reduceMotion = rememberTrainFlowReduceMotion()
            val workoutPlans by workoutPlanRepository.plans.collectAsState(
                initial = emptyList()
            )
            val workoutSessions by workoutSessionRepository.sessions.collectAsState(
                initial = emptyList()
            )
            val scope = rememberCoroutineScope()
            val openHeartRateBroadcastSmoke = remember(this) {
                createDebugHeartRateBroadcastSmokeLauncher(this)
            }

            TrainFlowTheme(
                skin = preferences.toTrainFlowSkin(),
                reduceMotion = reduceMotion
            ) {
                TrainFlowApp(
                    workoutPlans = workoutPlans,
                    workoutSessions = workoutSessions,
                    trainingPreferencesState = preferences.toTrainingPreferencesScreenState(),
                    planEditorDefaults = preferences.toPlanEditorDefaults(),
                    onSaveWorkoutPlan = { plan ->
                        scope.launch {
                            workoutPlanRepository.upsertPlan(plan)
                        }
                    },
                    onDeleteWorkoutPlan = { planId ->
                        scope.launch {
                            workoutPlanRepository.deletePlan(planId)
                        }
                    },
                    onRecordWorkoutSession = { session ->
                        workoutSessionRepository.upsertSession(session)
                    },
                    onClearAllWorkoutSessions = {
                        scope.launch {
                            workoutSessionRepository.deleteAllSessions()
                        }
                    },
                    onClearWorkoutSessionsForPlan = { planId ->
                        scope.launch {
                            workoutSessionRepository.deleteSessionsForPlan(planId)
                        }
                    },
                    onClearWorkoutSessionsStartedOnDate = { date ->
                        scope.launch {
                            workoutSessionRepository.deleteSessionsStartedOnDate(date)
                        }
                    },
                    onDefaultCountdownThresholdChanged = { seconds ->
                        scope.launch {
                            preferencesDataSource.setDefaultCountdownThresholdSec(seconds)
                        }
                    },
                    onActionCueEnabledChanged = { enabled ->
                        scope.launch {
                            preferencesDataSource.setActionCueEnabled(enabled)
                        }
                    },
                    onRestCueEnabledChanged = { enabled ->
                        scope.launch {
                            preferencesDataSource.setRestCueEnabled(enabled)
                        }
                    },
                    onSoundEnabledChanged = { enabled ->
                        scope.launch {
                            preferencesDataSource.setSoundEnabled(enabled)
                        }
                    },
                    onVibrationEnabledChanged = { enabled ->
                        scope.launch {
                            preferencesDataSource.setVibrationEnabled(enabled)
                        }
                    },
                    onEmphasisAnimationEnabledChanged = { enabled ->
                        scope.launch {
                            preferencesDataSource.setEmphasisAnimationEnabled(enabled)
                        }
                    },
                    onStrengthSetTimerModeChanged = { mode ->
                        scope.launch {
                            preferencesDataSource.setStrengthSetTimerMode(mode.contractValue)
                        }
                    },
                    onUiSkinChanged = { skinId ->
                        scope.launch {
                            preferencesDataSource.setUiSkinId(skinId)
                        }
                    },
                    onOpenHeartRateBroadcastSmoke = openHeartRateBroadcastSmoke
                )
            }
        }
    }
}

private fun createDebugHeartRateBroadcastSmokeLauncher(
    activity: ComponentActivity
): (() -> Unit)? {
    val isDebuggable = activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    if (!isDebuggable) return null
    val smokeActivityClass = runCatching {
        Class.forName("com.liujyks.trainflow.app.HeartRateBroadcastSmokeActivity")
    }.getOrNull() ?: return null

    return {
        activity.startActivity(Intent(activity, smokeActivityClass))
    }
}
