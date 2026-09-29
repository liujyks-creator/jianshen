package com.liujyks.trainflow.app

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.data.RecorderReconciliationResult
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.health.BleHeartRateScanStateKind
import com.liujyks.trainflow.feature.settings.HeartRateBlePermissionStatus
import com.liujyks.trainflow.core.health.HeartRateRecoveryPhase
import com.liujyks.trainflow.core.health.E17GattShadow
import com.liujyks.trainflow.core.health.E17ScannerShadow
import com.liujyks.trainflow.core.health.ObservationDeviceShadow
import com.liujyks.trainflow.core.health.WorkoutHeartRateService
import com.liujyks.trainflow.core.health.ThrowingStopForegroundShadow
import com.liujyks.trainflow.core.health.HeartRateRecoveryStopReason
import com.liujyks.trainflow.core.notifications.ActiveWorkoutForegroundPhase
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationState
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationProducer
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationUpdateResult
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutMode
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
import org.robolectric.annotation.LooperMode
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothGatt
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@RunWith(RobolectricTestRunner::class)
@Config(application = TrainFlowApplication::class, sdk = [35])
class TrainFlowApplicationHeartRateTest {
    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun applicationEligibilityLossStopsBackgroundRecovery() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        val adapter = application.getSystemService(BluetoothManager::class.java).adapter
        val cases = listOf(
            HeartRateRecoveryStopReason.OPTED_OUT,
            HeartRateRecoveryStopReason.MANUAL_SUPPRESSION,
            HeartRateRecoveryStopReason.NO_SAVED_TARGET,
            HeartRateRecoveryStopReason.PERMISSION_UNAVAILABLE,
            HeartRateRecoveryStopReason.BLUETOOTH_OFF
        )
        for ((index, expected) in cases.withIndex()) {
            shadowOf(application).grantPermissions(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.POST_NOTIFICATIONS
            )
            shadowOf(adapter).setEnabled(true)
            val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
            idleMain()
            val fixture = startEligibleForeground(application, "eligibility-$index")
            activity.pause().stop()
            idleMain()
            assertTrue(application.activeWorkoutNotifications.foregroundState.value.active)
            when (expected) {
                HeartRateRecoveryStopReason.OPTED_OUT -> application.setHeartRateEnabled(false)
                HeartRateRecoveryStopReason.MANUAL_SUPPRESSION -> application.disconnectHeartRateDevice()
                HeartRateRecoveryStopReason.NO_SAVED_TARGET -> application.clearHeartRateDevice()
                HeartRateRecoveryStopReason.PERMISSION_UNAVAILABLE -> {
                    shadowOf(application).denyPermissions(Manifest.permission.BLUETOOTH_CONNECT)
                    application.refreshHeartRateEnvironment()
                }
                HeartRateRecoveryStopReason.BLUETOOTH_OFF -> {
                    shadowOf(adapter).setEnabled(false)
                    application.refreshHeartRateEnvironment()
                }
                else -> error("Unexpected fixture input")
            }
            idleMain()
            assertEquals(expected, application.heartRateRuntimeOwner.recoveryState.value.stopReason)
            assertEquals(ActiveWorkoutForegroundPhase.RELEASING,
                application.activeWorkoutNotifications.foregroundState.value.phase)
            fixture.service.onStartCommand(
                WorkoutHeartRateService.releaseIntent(application, fixture.generation), 0, index + 2
            )
            idleMain()
            assertEquals(ActiveWorkoutForegroundPhase.NONE,
                application.activeWorkoutNotifications.foregroundState.value.phase)
            application.activeWorkoutNotifications.release(fixture.producer)
            activity.close()
            idleMain()
        }
    }

    @Test
    @Config(shadows = [ThrowingStopForegroundShadow::class])
    @LooperMode(LooperMode.Mode.PAUSED)
    fun releaseFailureInBackgroundStopsRecovery() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        idleMain()
        val fixture = startEligibleForeground(application, "failure-background")
        activity.pause().stop()
        idleMain()
        val original = SecurityException("release in background")
        ThrowingStopForegroundShadow.failure = original
        try {
            shadowOf(application).denyPermissions(Manifest.permission.BLUETOOTH_CONNECT)
            application.refreshHeartRateEnvironment()
            idleMain()
            fixture.service.onStartCommand(
                WorkoutHeartRateService.releaseIntent(application, fixture.generation), 0, 2
            )
        } finally {
            ThrowingStopForegroundShadow.failure = null
        }
        assertReleaseUnconfirmed(application, fixture, original)
        assertEquals(HeartRateRecoveryStopReason.PERMISSION_UNAVAILABLE,
            application.heartRateRuntimeOwner.recoveryState.value.stopReason)
        activity.close()
    }

    @Test
    @Config(shadows = [ThrowingStopForegroundShadow::class])
    @LooperMode(LooperMode.Mode.PAUSED)
    fun releaseFailureInUnknownStopsRecovery() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        idleMain()
        val fixture = startEligibleForeground(application, "failure-unknown")
        setUnknownVisibility(application)
        val original = SecurityException("release while visibility unknown")
        ThrowingStopForegroundShadow.failure = original
        try {
            shadowOf(application.getSystemService(BluetoothManager::class.java).adapter)
                .setEnabled(false)
            application.refreshHeartRateEnvironment()
            idleMain()
            fixture.service.onStartCommand(
                WorkoutHeartRateService.releaseIntent(application, fixture.generation), 0, 2
            )
        } finally {
            ThrowingStopForegroundShadow.failure = null
        }
        assertReleaseUnconfirmed(application, fixture, original)
        assertEquals(HeartRateRecoveryStopReason.BLUETOOTH_OFF,
            application.heartRateRuntimeOwner.recoveryState.value.stopReason)
        activity.close()
    }

    @Test
    @Config(shadows = [ThrowingStopForegroundShadow::class])
    @LooperMode(LooperMode.Mode.PAUSED)
    fun releaseFailureInVisibleThenBackgroundStopsRecovery() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        idleMain()
        val fixture = startEligibleForeground(application, "failure-visible-background")
        val original = SecurityException("release before background")
        ThrowingStopForegroundShadow.failure = original
        try {
            application.disconnectHeartRateDevice()
            idleMain()
            fixture.service.onStartCommand(
                WorkoutHeartRateService.releaseIntent(application, fixture.generation), 0, 2
            )
        } finally {
            ThrowingStopForegroundShadow.failure = null
        }
        activity.pause().stop()
        idleMain()
        assertReleaseUnconfirmed(application, fixture, original)
        assertEquals(HeartRateRecoveryStopReason.MANUAL_SUPPRESSION,
            application.heartRateRuntimeOwner.recoveryState.value.stopReason)
        activity.close()
    }

    @Test
    @Config(shadows = [ThrowingStopForegroundShadow::class])
    @LooperMode(LooperMode.Mode.PAUSED)
    fun releaseFailureInVisibleThenUnknownStopsRecovery() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        idleMain()
        val fixture = startEligibleForeground(application, "failure-visible-unknown")
        val original = SecurityException("release before unknown")
        ThrowingStopForegroundShadow.failure = original
        try {
            application.disconnectHeartRateDevice()
            idleMain()
            fixture.service.onStartCommand(
                WorkoutHeartRateService.releaseIntent(application, fixture.generation), 0, 2
            )
        } finally {
            ThrowingStopForegroundShadow.failure = null
        }
        setUnknownVisibility(application)
        assertReleaseUnconfirmed(application, fixture, original)
        assertEquals(HeartRateRecoveryStopReason.MANUAL_SUPPRESSION,
            application.heartRateRuntimeOwner.recoveryState.value.stopReason)
        activity.close()
    }

    @Test
    @Config(shadows = [E17GattShadow::class, E17ScannerShadow::class, ObservationDeviceShadow::class])
    @LooperMode(LooperMode.Mode.PAUSED)
    fun applicationKeepsSameOwnerForTrainingBackgroundAndStopsAtTerminal() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        shadowOf(application).grantPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS
        )
        shadowOf(application.getSystemService(BluetoothManager::class.java).adapter).setEnabled(true)
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        idleMain()
        application.setHeartRateEnabled(true)
        application.startManualHeartRateScan()
        idleMain()

        val address = "AA:BB:CC:DD:EE:71"
        val device = ShadowBluetoothDevice.newInstance(address)
        Shadow.extract<ShadowBluetoothDevice>(device).setName("Band fixture")
        lateinit var gatt: BluetoothGatt
        lateinit var callback: BluetoothGattCallback
        lateinit var measurement: BluetoothGattCharacteristic
        Shadow.extract<ShadowBluetoothDevice>(device).setGattConnectionInterceptor { connected ->
            gatt = connected
            val shadowGatt = Shadow.extract<ShadowBluetoothGatt>(connected)
            val service = BluetoothGattService(
                UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"),
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            measurement = BluetoothGattCharacteristic(
                UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            measurement.addDescriptor(BluetoothGattDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                BluetoothGattDescriptor.PERMISSION_WRITE
            ))
            service.addCharacteristic(measurement)
            shadowGatt.addDiscoverableService(service)
            shadowGatt.allowCharacteristicNotification(measurement)
            callback = shadowGatt.gattCallback
            callback.onConnectionStateChange(
                connected, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED
            )
        }
        val scanner = application.getSystemService(BluetoothManager::class.java)
            .adapter.bluetoothLeScanner
        shadowOf(scanner).scanCallbacks.single().onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES, ScanResult(device, null, -45, 1L)
        )
        idleMain()
        application.selectHeartRateDevice(address, "Band fixture")
        idleMain()
        callback.onCharacteristicChanged(gatt, measurement, byteArrayOf(0x00, 90))
        idleMain()
        assertEquals(HeartRateFact.LIVE, application.heartRateRuntimeOwner.heartRateState.value.fact)

        activity.pause().stop()
        idleMain()
        callback.onCharacteristicChanged(gatt, measurement, byteArrayOf(0x00, 91))
        idleMain()
        assertFalse(Shadow.extract<ShadowBluetoothGatt>(gatt).isClosed)
        assertEquals(91, application.heartRateRuntimeOwner.heartRateState.value.bpm)
        assertEquals(ActiveWorkoutForegroundPhase.NONE,
            application.activeWorkoutNotifications.foregroundState.value.phase)
        val trainingActivity = Robolectric.buildActivity(MainActivity::class.java).setup()
        idleMain()

        val owner = application.heartRateRuntimeOwner
        val controller = application.activeWorkoutNotifications
        val producer = controller.beginSession("background-hold")
        controller.update(producer, 1, trainingState(producer.sessionId))
        idleMain()
        val generation = requireNotNull(controller.foregroundState.value.generation)
        val platformService = Robolectric.buildService(WorkoutHeartRateService::class.java).create().get()
        platformService.onStartCommand(WorkoutHeartRateService.promoteIntent(application, generation), 0, 1)
        idleMain()
        assertTrue(controller.foregroundState.value.active)
        trainingActivity.pause().stop()
        idleMain()
        callback.onCharacteristicChanged(gatt, measurement, byteArrayOf(0x00, 92))
        idleMain()
        assertSame(owner, application.heartRateRuntimeOwner)
        assertFalse(Shadow.extract<ShadowBluetoothGatt>(gatt).isClosed)
        assertEquals(92, owner.heartRateState.value.bpm)
        controller.update(producer, 2, trainingState(producer.sessionId, SessionStatus.PAUSED))
        idleMain()
        assertTrue(controller.foregroundState.value.active)
        assertFalse(Shadow.extract<ShadowBluetoothGatt>(gatt).isClosed)

        val resumed = Robolectric.buildActivity(MainActivity::class.java).setup()
        idleMain()
        controller.update(producer, 3, trainingState(producer.sessionId, SessionStatus.COMPLETED))
        idleMain()
        platformService.onStartCommand(WorkoutHeartRateService.releaseIntent(application, generation), 0, 2)
        idleMain()
        assertFalse(Shadow.extract<ShadowBluetoothGatt>(gatt).isClosed)
        assertEquals(ActiveWorkoutForegroundPhase.NONE, controller.foregroundState.value.phase)

        val second = controller.beginSession("background-terminal")
        controller.update(second, 1, trainingState(second.sessionId))
        idleMain()
        val secondGeneration = requireNotNull(controller.foregroundState.value.generation)
        val secondService = Robolectric.buildService(WorkoutHeartRateService::class.java).create().get()
        secondService.onStartCommand(WorkoutHeartRateService.promoteIntent(application, secondGeneration), 0, 3)
        idleMain()
        resumed.pause().stop()
        idleMain()
        controller.update(second, 2, trainingState(second.sessionId, SessionStatus.COMPLETED))
        idleMain()
        secondService.onStartCommand(WorkoutHeartRateService.releaseIntent(application, secondGeneration), 0, 4)
        idleMain()
        assertFalse(Shadow.extract<ShadowBluetoothGatt>(gatt).isClosed)
        callback.onCharacteristicChanged(gatt, measurement, byteArrayOf(0x00, 93))
        idleMain()
        assertEquals(93, owner.heartRateState.value.bpm)

        val unknownActivity = Robolectric.buildActivity(MainActivity::class.java).setup()
        idleMain()
        val third = controller.beginSession("unknown-terminal")
        controller.update(third, 1, trainingState(third.sessionId))
        idleMain()
        val thirdGeneration = requireNotNull(controller.foregroundState.value.generation)
        val thirdService = Robolectric.buildService(WorkoutHeartRateService::class.java).create().get()
        thirdService.onStartCommand(WorkoutHeartRateService.promoteIntent(application, thirdGeneration), 0, 5)
        idleMain()
        val visibilityField = TrainFlowApplication::class.java.getDeclaredField("visibilityFact")
        visibilityField.isAccessible = true
        visibilityField.set(application, ProcessVisibilityFact.UNKNOWN)
        application.refreshHeartRateEnvironment()
        idleMain()
        controller.update(third, 2, trainingState(third.sessionId, SessionStatus.COMPLETED))
        idleMain()
        thirdService.onStartCommand(WorkoutHeartRateService.releaseIntent(application, thirdGeneration), 0, 6)
        idleMain()
        assertFalse(controller.foregroundState.value.active)
        assertFalse(Shadow.extract<ShadowBluetoothGatt>(gatt).isClosed)
        callback.onCharacteristicChanged(gatt, measurement, byteArrayOf(0x00, 94))
        idleMain()
        assertEquals(94, owner.heartRateState.value.bpm)
        unknownActivity.close()
    }

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

    private fun idleMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private suspend fun startEligibleForeground(
        application: TrainFlowApplication,
        sessionId: String
    ): ForegroundFixture {
        shadowOf(application).grantPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS
        )
        shadowOf(application.getSystemService(BluetoothManager::class.java).adapter).setEnabled(true)
        application.setHeartRateEnabled(true)
        application.selectHeartRateDevice("AA:BB:CC:DD:EE:71", "Band fixture")
        idleMain()
        val controller = application.activeWorkoutNotifications
        val producer = controller.beginSession(sessionId)
        controller.update(producer, 1, trainingState(sessionId))
        idleMain()
        val generation = requireNotNull(controller.foregroundState.value.generation)
        val service = Robolectric.buildService(WorkoutHeartRateService::class.java).create().get()
        service.onStartCommand(WorkoutHeartRateService.promoteIntent(application, generation), 0, 1)
        idleMain()
        assertTrue(controller.foregroundState.value.active)
        return ForegroundFixture(producer, generation, service)
    }

    private fun setUnknownVisibility(application: TrainFlowApplication) {
        val visibilityField = TrainFlowApplication::class.java.getDeclaredField("visibilityFact")
        visibilityField.isAccessible = true
        visibilityField.set(application, ProcessVisibilityFact.UNKNOWN)
        application.refreshHeartRateEnvironment()
        idleMain()
    }

    private fun assertReleaseUnconfirmed(
        application: TrainFlowApplication,
        fixture: ForegroundFixture,
        original: RuntimeException
    ) {
        val controller = application.activeWorkoutNotifications
        assertEquals(ActiveWorkoutForegroundPhase.RELEASE_UNCONFIRMED,
            controller.foregroundState.value.phase)
        assertSame(original, controller.foregroundState.value.failure)
        val manager = application.getSystemService(NotificationManager::class.java)
        val before = shadowOf(manager).getNotification(7_200)
            ?.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)
        val result = controller.update(fixture.producer, 2, trainingState(fixture.producer.sessionId))
        assertTrue(result is ActiveWorkoutNotificationUpdateResult.Deferred)
        controller.foregroundReleased(fixture.generation, fixture.service)
        assertEquals(ActiveWorkoutForegroundPhase.RELEASE_UNCONFIRMED,
            controller.foregroundState.value.phase)
        assertEquals(before, shadowOf(manager).getNotification(7_200)
            ?.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT))
    }

    private data class ForegroundFixture(
        val producer: ActiveWorkoutNotificationProducer,
        val generation: Long,
        val service: WorkoutHeartRateService
    )

    private fun trainingState(
        sessionId: String,
        status: SessionStatus = SessionStatus.ACTIVE
    ) = ActiveWorkoutNotificationState(
        sessionId = sessionId,
        mode = WorkoutMode.TIMED,
        planTitle = "测试训练",
        status = status,
        phaseLabel = "动作",
        primaryText = "进行中",
        timerText = "00:03",
        progressText = "1 / 2",
        secondaryText = "下一步"
    )
}
