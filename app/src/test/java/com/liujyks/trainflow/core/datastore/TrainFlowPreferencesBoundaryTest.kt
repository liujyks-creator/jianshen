package com.liujyks.trainflow.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertFalse(preferences.heartRateManualDisconnectSuppressed)
        assertNull(preferences.ageYears)
        assertNull(preferences.personalMaxHeartRateBpm)
        assertNull(preferences.alertThresholdBpm)
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
    fun dataSourcePersistsOnlyBleHeartRateDeviceSelectionPreference() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = {
                File(temporaryFolder.root, "ble-hr-device.preferences_pb")
            }
        )
        val dataSource = TrainFlowPreferencesDataSource(dataStore)

        dataSource.setBleHeartRateDevicePreference(
            identifier = "D8:F0:42:01:90:D7",
            displayName = "HUAWEI Band HR-OD7"
        )

        val preferences = dataSource.preferences.first()

        assertEquals("D8:F0:42:01:90:D7", preferences.bleHeartRateDeviceIdentifier)
        assertEquals("HUAWEI Band HR-OD7", preferences.bleHeartRateDeviceDisplayName)
        assertFalse(preferences.heartRateDisplayEnabled)
        assertFalse(preferences.showDisconnectedHeartRatePlaceholder)
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
    fun manualDisconnectSuppressionPersistsUntilExplicitReconnectOrTargetSelection() = runBlocking {
        val dataStore = InMemoryPreferencesDataStore()
        val dataSource = TrainFlowPreferencesDataSource(dataStore)
        dataSource.setBleHeartRateDevicePreference("saved-id", "Saved HRS")
        dataSource.setHeartRateManualDisconnectSuppressed()
        dataSource.setHeartRateDisplayEnabled(false)
        dataSource.setHeartRateDisplayEnabled(true)
        val recreatedDataSource = TrainFlowPreferencesDataSource(dataStore)
        assertTrue(recreatedDataSource.preferences.first().heartRateManualDisconnectSuppressed)

        recreatedDataSource.clearBleHeartRateDevicePreference()
        assertTrue(recreatedDataSource.preferences.first().heartRateManualDisconnectSuppressed)

        recreatedDataSource.clearHeartRateManualDisconnectSuppression()
        assertFalse(recreatedDataSource.preferences.first().heartRateManualDisconnectSuppressed)

        recreatedDataSource.setHeartRateManualDisconnectSuppressed()
        recreatedDataSource.setBleHeartRateDevicePreference("new-id", "New HRS")
        assertFalse(recreatedDataSource.preferences.first().heartRateManualDisconnectSuppressed)
    }

    @Test
    fun age101AndInclusivePersonalizationBoundariesRoundTripWithoutClamping() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = {
                File(temporaryFolder.root, "heart-rate-personalization.preferences_pb")
            }
        )
        val dataSource = TrainFlowPreferencesDataSource(dataStore)

        dataSource.setHeartRatePersonalization(
            ageYears = 101,
            personalMaxHeartRateBpm = 30,
            alertThresholdBpm = 260
        )
        val first = TrainFlowPreferencesDataSource(dataStore).preferences.first()
        assertEquals(101, first.ageYears)
        assertEquals(30, first.personalMaxHeartRateBpm)
        assertEquals(260, first.alertThresholdBpm)

        val boundaryStore = PreferenceDataStoreFactory.create(
            produceFile = {
                File(temporaryFolder.root, "heart-rate-personalization-boundary.preferences_pb")
            }
        )
        val boundaryDataSource = TrainFlowPreferencesDataSource(boundaryStore)
        boundaryDataSource.setHeartRatePersonalization(130, 260, 30)
        val second = boundaryDataSource.preferences.first()
        assertEquals(130, second.ageYears)
        assertEquals(260, second.personalMaxHeartRateBpm)
        assertEquals(30, second.alertThresholdBpm)
    }

    @Test
    fun nullOrInvalidPersonalizationValuesRemoveOnlyTheirOwnKeys() = runBlocking {
        val dataStore = InMemoryPreferencesDataStore()
        val dataSource = TrainFlowPreferencesDataSource(dataStore)
        dataSource.setHeartRatePersonalization(40, 200, 180)

        dataSource.setHeartRatePersonalization(131, 200, 180)
        var preferences = dataSource.preferences.first()
        assertNull(preferences.ageYears)
        assertEquals(200, preferences.personalMaxHeartRateBpm)
        assertEquals(180, preferences.alertThresholdBpm)

        dataSource.setHeartRatePersonalization(null, 29, 261)
        preferences = dataSource.preferences.first()
        assertNull(preferences.ageYears)
        assertNull(preferences.personalMaxHeartRateBpm)
        assertNull(preferences.alertThresholdBpm)

        val stored = dataStore.data.first()
        assertFalse(stored.contains(TrainFlowPreferenceKeys.ageYears))
        assertFalse(stored.contains(TrainFlowPreferenceKeys.personalMaxHeartRateBpm))
        assertFalse(stored.contains(TrainFlowPreferenceKeys.alertThresholdBpm))
    }

    @Test
    fun corruptStoredPersonalizationValuesReadAsNullRatherThanClampedBoundaries() = runBlocking {
        val dataStore = InMemoryPreferencesDataStore()
        dataStore.edit { stored ->
            stored[TrainFlowPreferenceKeys.ageYears] = 0
            stored[TrainFlowPreferenceKeys.personalMaxHeartRateBpm] = 261
            stored[TrainFlowPreferenceKeys.alertThresholdBpm] = 29
        }

        val preferences = TrainFlowPreferencesDataSource(dataStore).preferences.first()

        assertNull(preferences.ageYears)
        assertNull(preferences.personalMaxHeartRateBpm)
        assertNull(preferences.alertThresholdBpm)
    }

    @Test
    fun preferenceStoreNameStaysInsideTrainFlowNamespace() {
        assertEquals("trainflow_preferences", TrainFlowPreferenceKeys.DATASTORE_NAME)
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val mutableData = MutableStateFlow(emptyPreferences())

        override val data = mutableData

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences {
            return transform(mutableData.value).also { mutableData.value = it }
        }
    }
}
