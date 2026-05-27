package com.liujyks.trainflow.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainFlowPreferencesBoundaryTest {
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
    fun preferenceStoreNameStaysInsideTrainFlowNamespace() {
        assertEquals("trainflow_preferences", TrainFlowPreferenceKeys.DATASTORE_NAME)
    }
}
