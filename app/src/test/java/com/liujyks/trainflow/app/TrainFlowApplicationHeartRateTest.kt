package com.liujyks.trainflow.app

import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.feature.settings.HeartRateBlePermissionStatus
import com.liujyks.trainflow.core.health.HeartRateRecoveryPhase
import com.liujyks.trainflow.core.model.HeartRateFact
import com.liujyks.trainflow.feature.settings.HeartRateDeviceScanPurpose
import com.liujyks.trainflow.ui.shell.official.PendingHeartRatePermissionAction
import com.liujyks.trainflow.ui.shell.official.pendingHeartRatePermissionActionAfterDisplayChange
import com.liujyks.trainflow.ui.shell.official.shouldConsumeManualSavedDeviceScanMatch
import com.liujyks.trainflow.ui.shell.official.shouldInvalidateHeartRateScanIntent
import com.liujyks.trainflow.ui.shell.official.shouldResumeHeartRatePermissionAction
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(application = TrainFlowApplication::class, sdk = [35])
class TrainFlowApplicationHeartRateTest {
    @Test
    fun applicationExposesOneStableRuntimeOwner() {
        val application =
            ApplicationProvider.getApplicationContext<TrainFlowApplication>()

        assertSame(application.heartRateRuntimeOwner, application.heartRateRuntimeOwner)
    }

    @Test
    fun explicitReconnectContinuesAfterPermissionGrant() {
        assertTrue(
            shouldResumeHeartRatePermissionAction(
                pendingAction = PendingHeartRatePermissionAction.RECONNECT,
                permissionStatus = HeartRateBlePermissionStatus.GRANTED
            )
        )
    }

    @Test
    fun genericPermissionAndDeniedResultsDoNotTriggerReconnect() {
        assertFalse(
            shouldResumeHeartRatePermissionAction(
                pendingAction = PendingHeartRatePermissionAction.NONE,
                permissionStatus = HeartRateBlePermissionStatus.GRANTED
            )
        )
        assertFalse(
            shouldResumeHeartRatePermissionAction(
                pendingAction = PendingHeartRatePermissionAction.RECONNECT,
                permissionStatus = HeartRateBlePermissionStatus.DENIED
            )
        )
    }

    @Test
    fun optOutClearsPendingReconnectSoReEnableCannotResurrectIt() {
        val afterOptOut = pendingHeartRatePermissionActionAfterDisplayChange(
            displayEnabled = false,
            pendingAction = PendingHeartRatePermissionAction.RECONNECT
        )
        val afterReEnable = pendingHeartRatePermissionActionAfterDisplayChange(
            displayEnabled = true,
            pendingAction = afterOptOut
        )

        assertEquals(PendingHeartRatePermissionAction.NONE, afterOptOut)
        assertEquals(PendingHeartRatePermissionAction.NONE, afterReEnable)
    }

    @Test
    fun automaticRecoveryNeverConsumesStaleManualSavedDeviceIntent() {
        assertFalse(
            shouldConsumeManualSavedDeviceScanMatch(
                scanPurpose = HeartRateDeviceScanPurpose.CONNECT_SAVED_DEVICE,
                recoveryPhase = HeartRateRecoveryPhase.SEARCHING
            )
        )
        assertTrue(
            shouldConsumeManualSavedDeviceScanMatch(
                scanPurpose = HeartRateDeviceScanPurpose.CONNECT_SAVED_DEVICE,
                recoveryPhase = HeartRateRecoveryPhase.DISARMED
            )
        )
    }

    @Test
    fun cleanupFactsAndBackgroundInvalidateManualScanIntent() {
        assertTrue(
            shouldInvalidateHeartRateScanIntent(
                displayEnabled = true,
                appVisible = false,
                fact = HeartRateFact.SCANNING
            )
        )
        assertTrue(
            shouldInvalidateHeartRateScanIntent(
                displayEnabled = true,
                appVisible = true,
                fact = HeartRateFact.LINK_DISCONNECTED
            )
        )
        assertFalse(
            shouldInvalidateHeartRateScanIntent(
                displayEnabled = true,
                appVisible = true,
                fact = HeartRateFact.SCANNING
            )
        )
    }

    @Test
    fun clearingTargetPreservesPersistedManualSuppression() = runBlocking {
        val application =
            ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        application.preferencesDataSource.setBleHeartRateDevicePreference(
            identifier = "D8:F0:42:01:90:D7",
            displayName = "HUAWEI Band HR-OD7"
        )
        application.preferencesDataSource.setHeartRateManualSuppressed(true)

        application.clearHeartRateDevice()

        val preferences = application.preferencesDataSource.preferences.first()
        assertNull(preferences.bleHeartRateDeviceIdentifier)
        assertNull(preferences.bleHeartRateDeviceDisplayName)
        assertTrue(preferences.heartRateManualSuppressed)
        application.preferencesDataSource.setHeartRateManualSuppressed(false)
    }

    @Test
    fun changeDeviceClearsManualSuppressionBeforeStartingManualScan() = runBlocking {
        val application =
            ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        application.preferencesDataSource.setHeartRateManualSuppressed(true)

        application.changeHeartRateDevice()

        assertFalse(
            application.preferencesDataSource.preferences.first().heartRateManualSuppressed
        )
    }
}
