package com.liujyks.trainflow.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TrainFlowPreferencesBoundaryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun defaultsMatchFirstVersionTrainingPreferenceBoundary() {
        val preferences = TrainFlowPreferences()

        assertTrue(preferences.actionCueEnabled)
        assertTrue(preferences.restCueEnabled)
        assertTrue(preferences.soundEnabled)
        assertTrue(preferences.vibrationEnabled)
        assertTrue(preferences.emphasisAnimationEnabled)
        assertEquals(5, preferences.defaultCountdownThresholdSec)
        assertEquals("manual_start", preferences.strengthSetTimerMode)
        assertFalse(preferences.heartRateDisplayEnabled)
        assertFalse(preferences.showDisconnectedHeartRatePlaceholder)
    }

    @Test
    fun dataSourcePersistsTrainingFeedbackPreferences() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = {
                File(temporaryFolder.root, "training-preferences.preferences_pb")
            }
        )
        val dataSource = TrainFlowPreferencesDataSource(dataStore)

        dataSource.setTrainingFeedbackPreferences(
            TrainFlowPreferences(
                defaultCountdownThresholdSec = 9,
                actionCueEnabled = false,
                restCueEnabled = true,
                soundEnabled = false,
                vibrationEnabled = false,
                emphasisAnimationEnabled = false,
                strengthSetTimerMode = TrainFlowPreferences.STRENGTH_TIMER_AUTO_AFTER_REST
            )
        )

        val preferences = dataSource.preferences.first()

        assertEquals(9, preferences.defaultCountdownThresholdSec)
        assertFalse(preferences.actionCueEnabled)
        assertTrue(preferences.restCueEnabled)
        assertFalse(preferences.soundEnabled)
        assertFalse(preferences.vibrationEnabled)
        assertFalse(preferences.emphasisAnimationEnabled)
        assertEquals("auto_after_rest", preferences.strengthSetTimerMode)
    }

    @Test
    fun dataSourceClampsPreferenceValuesInsideContractBoundary() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = {
                File(temporaryFolder.root, "clamped-training-preferences.preferences_pb")
            }
        )
        val dataSource = TrainFlowPreferencesDataSource(dataStore)

        dataSource.setTrainingFeedbackPreferences(
            TrainFlowPreferences(
                defaultCountdownThresholdSec = 1000,
                strengthSetTimerMode = "voice_coach"
            )
        )

        val preferences = dataSource.preferences.first()

        assertEquals(TrainFlowPreferences.MAX_COUNTDOWN_THRESHOLD_SEC, preferences.defaultCountdownThresholdSec)
        assertEquals("manual_start", preferences.strengthSetTimerMode)
    }

    @Test
    fun preferenceStoreNameStaysInsideTrainFlowNamespace() {
        assertEquals("trainflow_preferences", TrainFlowPreferenceKeys.DATASTORE_NAME)
    }
}
