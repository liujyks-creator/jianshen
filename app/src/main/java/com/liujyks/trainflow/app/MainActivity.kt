package com.liujyks.trainflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.liujyks.trainflow.core.datastore.TrainFlowPreferences
import com.liujyks.trainflow.core.datastore.TrainFlowPreferencesDataSource
import com.liujyks.trainflow.core.datastore.trainFlowPreferencesDataStore
import com.liujyks.trainflow.ui.shell.official.TrainFlowApp
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
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
            val preferences by preferencesDataSource.preferences.collectAsState(
                initial = TrainFlowPreferences()
            )
            val scope = rememberCoroutineScope()

            TrainFlowTheme(skin = preferences.toTrainFlowSkin()) {
                TrainFlowApp(
                    trainingPreferencesState = preferences.toTrainingPreferencesScreenState(),
                    planEditorDefaults = preferences.toPlanEditorDefaults(),
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
                    }
                )
            }
        }
    }
}
