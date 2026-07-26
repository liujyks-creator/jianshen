package com.liujyks.trainflow.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
        assertEquals(null, preferences.bleHeartRateDeviceIdentifier)
        assertEquals(null, preferences.bleHeartRateDeviceDisplayName)
        assertFalse(preferences.heartRateManualSuppressed)
        assertEquals(null, preferences.heartRateAgeYears)
        assertEquals(null, preferences.heartRatePersonalMaxBpm)
        assertEquals(null, preferences.heartRateAlertThresholdBpm)
        assertEquals("official_flow", preferences.uiSkinId)
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
                strengthSetTimerMode = TrainFlowPreferences.STRENGTH_TIMER_AUTO_AFTER_REST,
                uiSkinId = TrainFlowPreferences.UI_SKIN_TILE_FLOW
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
        assertEquals("tile_flow", preferences.uiSkinId)
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
                strengthSetTimerMode = "voice_coach",
                uiSkinId = "remote_market_skin"
            )
        )

        val preferences = dataSource.preferences.first()

        assertEquals(TrainFlowPreferences.MAX_COUNTDOWN_THRESHOLD_SEC, preferences.defaultCountdownThresholdSec)
        assertEquals("manual_start", preferences.strengthSetTimerMode)
        assertEquals("official_flow", preferences.uiSkinId)
    }

    @Test
    fun dataSourcePersistsBuiltInUiSkinId() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = {
                File(temporaryFolder.root, "ui-skin-preferences.preferences_pb")
            }
        )
        val dataSource = TrainFlowPreferencesDataSource(dataStore)

        dataSource.setUiSkinId("big_type")

        val preferences = dataSource.preferences.first()

        assertEquals("big_type", preferences.uiSkinId)
    }

    @Test
    fun deviceSelectionClearsManualSuppressionWithoutChangingOptIn() = runBlocking {
        val dataStore = InMemoryPreferencesDataStore()
        val dataSource = TrainFlowPreferencesDataSource(dataStore)

        dataSource.setHeartRateManualSuppressed(true)
        dataSource.setBleHeartRateDevicePreference(
            identifier = "D8:F0:42:01:90:D7",
            displayName = "HUAWEI Band HR-OD7"
        )

        val preferences = dataSource.preferences.first()

        assertEquals("D8:F0:42:01:90:D7", preferences.bleHeartRateDeviceIdentifier)
        assertEquals("HUAWEI Band HR-OD7", preferences.bleHeartRateDeviceDisplayName)
        assertFalse(preferences.heartRateDisplayEnabled)
        assertFalse(preferences.showDisconnectedHeartRatePlaceholder)
        assertFalse(preferences.heartRateManualSuppressed)
    }

    @Test
    fun dataSourcePersistsHeartRateDisplayOptInWithoutPlaceholder() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = {
                File(temporaryFolder.root, "heart-rate-opt-in-${System.nanoTime()}.preferences_pb")
            }
        )
        val dataSource = TrainFlowPreferencesDataSource(dataStore)

        dataSource.setHeartRateDisplayEnabled(true)
        val enabledPreferences = dataSource.preferences.first()

        assertTrue(enabledPreferences.heartRateDisplayEnabled)
        assertFalse(enabledPreferences.showDisconnectedHeartRatePlaceholder)

        val disabledDataStore = PreferenceDataStoreFactory.create(
            produceFile = {
                File(temporaryFolder.root, "heart-rate-disabled-${System.nanoTime()}.preferences_pb")
            }
        )
        val disabledDataSource = TrainFlowPreferencesDataSource(disabledDataStore)

        disabledDataSource.setHeartRateDisplayEnabled(false)
        val disabledPreferences = disabledDataSource.preferences.first()

        assertFalse(disabledPreferences.heartRateDisplayEnabled)
        assertFalse(disabledPreferences.showDisconnectedHeartRatePlaceholder)
    }

    @Test
    fun preferenceStoreNameStaysInsideTrainFlowNamespace() {
        assertEquals("trainflow_preferences", TrainFlowPreferenceKeys.DATASTORE_NAME)
    }

    @Test
    fun suppressionAndPersonalParametersSurviveFileBackedRecreation() = runBlocking {
        val file = File(temporaryFolder.root, "heart-rate-personal.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val firstStore = PreferenceDataStoreFactory.create(
            scope = firstScope,
            produceFile = { file }
        )

        firstStore.edit { preferences ->
            preferences[TrainFlowPreferenceKeys.heartRateDisplayEnabled] = true
            preferences[TrainFlowPreferenceKeys.bleHeartRateDeviceIdentifier] =
                "D8:F0:42:01:90:D7"
            preferences[TrainFlowPreferenceKeys.bleHeartRateDeviceDisplayName] =
                "HUAWEI Band HR-OD7"
            preferences[TrainFlowPreferenceKeys.heartRateManualSuppressed] = true
            preferences[TrainFlowPreferenceKeys.heartRateAgeYears] = 101
            preferences[TrainFlowPreferenceKeys.heartRatePersonalMaxBpm] = 205
            preferences[TrainFlowPreferenceKeys.heartRateAlertThresholdBpm] = 198
        }
        firstScope.cancel()

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val secondStore = PreferenceDataStoreFactory.create(
            scope = secondScope,
            produceFile = { file }
        )
        val restored = TrainFlowPreferencesDataSource(secondStore).preferences.first()

        assertTrue(restored.heartRateManualSuppressed)
        assertTrue(restored.heartRateDisplayEnabled)
        assertEquals("D8:F0:42:01:90:D7", restored.bleHeartRateDeviceIdentifier)
        assertEquals("HUAWEI Band HR-OD7", restored.bleHeartRateDeviceDisplayName)
        assertEquals(101, restored.heartRateAgeYears)
        assertEquals(205, restored.heartRatePersonalMaxBpm)
        assertEquals(198, restored.heartRateAlertThresholdBpm)
        secondScope.cancel()
    }

    @Test
    fun parameterBoundariesPersistAndInvalidValuesFailClosedToNull() = runBlocking {
        val minimumSource = dataSource("heart-rate-parameter-minimum.preferences_pb")
        minimumSource.setHeartRatePersonalParameters(
            ageYears = 1,
            personalMaxHeartRateBpm = 30,
            alertThresholdBpm = 260
        )
        val minimums = minimumSource.preferences.first()
        assertEquals(1, minimums.heartRateAgeYears)
        assertEquals(30, minimums.heartRatePersonalMaxBpm)
        assertEquals(260, minimums.heartRateAlertThresholdBpm)

        val maximumSource = dataSource("heart-rate-parameter-maximum.preferences_pb")
        maximumSource.setHeartRatePersonalParameters(
            ageYears = 130,
            personalMaxHeartRateBpm = 260,
            alertThresholdBpm = 30
        )
        val maximums = maximumSource.preferences.first()
        assertEquals(130, maximums.heartRateAgeYears)
        assertEquals(260, maximums.heartRatePersonalMaxBpm)
        assertEquals(30, maximums.heartRateAlertThresholdBpm)

        val invalidSource = dataSource("heart-rate-parameter-invalid.preferences_pb")
        invalidSource.setHeartRatePersonalParameters(
            ageYears = 131,
            personalMaxHeartRateBpm = 29,
            alertThresholdBpm = 261
        )
        val invalid = invalidSource.preferences.first()
        assertEquals(null, invalid.heartRateAgeYears)
        assertEquals(null, invalid.heartRatePersonalMaxBpm)
        assertEquals(null, invalid.heartRateAlertThresholdBpm)
    }

    private fun dataSource(fileName: String): TrainFlowPreferencesDataSource =
        TrainFlowPreferencesDataSource(
            PreferenceDataStoreFactory.create(
                produceFile = { File(temporaryFolder.root, fileName) }
            )
        )

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
