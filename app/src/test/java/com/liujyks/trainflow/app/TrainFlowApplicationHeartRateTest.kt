package com.liujyks.trainflow.app

import android.Manifest
import android.bluetooth.BluetoothManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.data.RecorderReconciliationResult
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.health.BleHeartRateScanStateKind
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@RunWith(RobolectricTestRunner::class)
@Config(application = TrainFlowApplication::class, sdk = [35])
class TrainFlowApplicationHeartRateTest {
    @Test
    fun applicationProvisionsOneStableRepositoryAndLeavesItsRecorderGateLazy() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        val result = withContext(Dispatchers.IO) {
            application.trainFlowDatabase.clearAllTables()
            application.trainFlowDatabase.workoutSessionDao().insertSession(
                WorkoutSessionEntity(
                    id = "legacy-created-after-application-on-create",
                    mode = "timed",
                    status = "active",
                    planSnapshotJson = "{\"title\":\"Legacy\",\"mode\":\"timed\",\"blocks\":[]}"
                )
            )
            application.workoutSessionRepository.prepareRecorder()
        }

        val repository = application.workoutSessionRepository
        val database = application.trainFlowDatabase

        assertSame(repository, application.workoutSessionRepository)
        assertSame(database, application.trainFlowDatabase)
        assertTrue(result is RecorderReconciliationResult.Succeeded)
        result as RecorderReconciliationResult.Succeeded
        assertEquals(
            listOf("legacy-created-after-application-on-create"),
            result.legacyResiduals.map { residual -> residual.sessionId }
        )
        withContext(Dispatchers.IO) {
            application.trainFlowDatabase.clearAllTables()
        }
    }

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
    fun changeDeviceClearsSuppressionAndStartsFiniteManualDeviceSelectionScan() = runBlocking {
        val application =
            ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        shadowOf(application).grantPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        shadowOf(
            application.getSystemService(BluetoothManager::class.java).adapter
        ).setEnabled(true)
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(ProcessVisibilityFact.VISIBLE, application.processVisibility.value)
        application.setHeartRateEnabled(true)
        application.preferencesDataSource.setHeartRateManualSuppressed(true)

        application.changeHeartRateDevice()
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(
            application.preferencesDataSource.preferences.first().heartRateManualSuppressed
        )
        assertEquals(
            "fact=${application.heartRateRuntimeOwner.heartRateState.value.fact} " +
                "recovery=${application.heartRateRuntimeOwner.recoveryState.value}",
            BleHeartRateScanStateKind.SCANNING,
            application.heartRateRuntimeOwner.scanState.value.kind
        )
        application.stopManualHeartRateScan()
        shadowOf(Looper.getMainLooper()).idle()
        activity.close()
    }
}
