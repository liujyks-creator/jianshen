package com.liujyks.trainflow.core.health

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowBluetoothLeScanner
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothGatt
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class HeartRateRuntimeOwnerRecoveryTest {
    private lateinit var application: Application
    private lateinit var scanner: BluetoothLeScanner
    private lateinit var shadowScanner: ShadowBluetoothLeScanner
    private lateinit var owner: HeartRateRuntimeOwner

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        val adapter = application.getSystemService(BluetoothManager::class.java).adapter
        shadowOf(adapter).setEnabled(true)
        scanner = adapter.bluetoothLeScanner
        shadowScanner = shadowOf(scanner)
        owner = HeartRateRuntimeOwner(
            application,
            mainHandler = Handler(Looper.getMainLooper()),
            scanWindowMillis = SCAN_WINDOW_MS,
            recoveryIntervalMillis = RECOVERY_INTERVAL_MS
        )
    }

    @Test
    fun eligibleContextStartsFiniteWindowAndRemainsArmedAcrossRepeatedMisses() {
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        idleMain()

        assertEquals(HeartRateRecoveryPhase.SEARCHING, owner.recoveryState.value.phase)
        assertEquals(1, shadowScanner.scanCallbacks.size)

        idleFor(SCAN_WINDOW_MS)
        assertEquals(
            HeartRateRecoveryPhase.WINDOW_MISSED_ARMED,
            owner.recoveryState.value.phase
        )
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        idleFor(RECOVERY_INTERVAL_MS)
        assertEquals(HeartRateRecoveryPhase.SEARCHING, owner.recoveryState.value.phase)
        assertEquals(1, shadowScanner.scanCallbacks.size)

        idleFor(SCAN_WINDOW_MS + RECOVERY_INTERVAL_MS)
        assertEquals(HeartRateRecoveryPhase.SEARCHING, owner.recoveryState.value.phase)
        assertEquals(1, shadowScanner.scanCallbacks.size)
    }

    @Test
    fun stopScanAndManualScanCannotConsumeRecoveryObligation() {
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        idleMain()
        owner.submit(HeartRateRuntimeAction.StopScan)
        idleMain()

        assertEquals(HeartRateRecoveryPhase.WAITING_NEXT_WINDOW, owner.recoveryState.value.phase)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        assertEquals(1, shadowScanner.scanCallbacks.size)

        idleFor(SCAN_WINDOW_MS)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
        assertEquals(HeartRateRecoveryPhase.WAITING_NEXT_WINDOW, owner.recoveryState.value.phase)

        idleFor(RECOVERY_INTERVAL_MS)
        assertEquals(HeartRateRecoveryPhase.SEARCHING, owner.recoveryState.value.phase)
        assertEquals(1, shadowScanner.scanCallbacks.size)
    }

    @Test
    fun everyEligibilityLossDisarmsAndCancelsPendingWindow() {
        val contexts = listOf(
            eligibleInput().copy(optedIn = false) to HeartRateRecoveryStopReason.OPTED_OUT,
            eligibleInput().copy(savedTargetIdentifier = null) to
                HeartRateRecoveryStopReason.NO_SAVED_TARGET,
            eligibleInput().copy(permissionGranted = false) to
                HeartRateRecoveryStopReason.PERMISSION_UNAVAILABLE,
            eligibleInput().copy(bluetoothEnabled = false) to
                HeartRateRecoveryStopReason.BLUETOOTH_OFF,
            eligibleInput().copy(manuallySuppressed = true) to
                HeartRateRecoveryStopReason.MANUAL_SUPPRESSION,
            eligibleInput().copy(appVisible = false) to
                HeartRateRecoveryStopReason.BACKGROUND_WITHOUT_FGS
        )

        contexts.forEach { (context, expectedReason) ->
            owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
            idleMain()
            owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(context))
            idleMain()

            assertEquals(HeartRateRecoveryPhase.DISARMED, owner.recoveryState.value.phase)
            assertEquals(expectedReason, owner.recoveryState.value.stopReason)
            assertTrue(shadowScanner.scanCallbacks.isEmpty())

            idleFor(SCAN_WINDOW_MS + RECOVERY_INTERVAL_MS)
            assertTrue(shadowScanner.scanCallbacks.isEmpty())
        }
    }

    @Test
    fun manualDisconnectStaysSuppressedUntilExplicitEligibilityClearsIt() {
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        idleMain()
        owner.submit(HeartRateRuntimeAction.Disconnect)
        idleMain()

        assertEquals(HeartRateRecoveryPhase.DISARMED, owner.recoveryState.value.phase)
        assertEquals(
            HeartRateRecoveryStopReason.MANUAL_SUPPRESSION,
            owner.recoveryState.value.stopReason
        )
        idleFor(SCAN_WINDOW_MS + RECOVERY_INTERVAL_MS)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryEligibility(
                eligibleInput().copy(manuallySuppressed = false)
            )
        )
        idleMain()

        assertEquals(HeartRateRecoveryPhase.SEARCHING, owner.recoveryState.value.phase)
        assertEquals(1, shadowScanner.scanCallbacks.size)
    }

    @Test
    fun recoveryConnectsOnlyExactTargetAndKeepsTryingAfterWrongCandidate() {
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        idleMain()
        val callback = shadowScanner.scanCallbacks.single()
        val wrong = scanResult("AA:BB:CC:DD:EE:99", "Other HRS")
        callback.onScanResult(0, wrong)
        idleMain()

        assertTrue(
            Shadow.extract<ShadowBluetoothDevice>(requireNotNull(wrong.device))
                .bluetoothGatts.isEmpty()
        )
        assertEquals(HeartRateRecoveryPhase.SEARCHING, owner.recoveryState.value.phase)

        val target = scanResult(TARGET, "Saved Band")
        callback.onScanResult(0, target)
        idleMain()

        assertTrue(shadowScanner.scanCallbacks.isEmpty())
        assertEquals(
            1,
            Shadow.extract<ShadowBluetoothDevice>(requireNotNull(target.device))
                .bluetoothGatts.size
        )
        assertEquals(
            HeartRateRecoveryPhase.CONNECTING_OR_CONNECTED,
            owner.recoveryState.value.phase
        )
    }

    @Test
    fun changingSavedTargetClosesOldAttemptAndStartsRecoveryForNewTarget() {
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        idleMain()
        val first = scanResult(TARGET, "First")
        shadowScanner.scanCallbacks.single().onScanResult(0, first)
        idleMain()
        val firstGatt = Shadow.extract<ShadowBluetoothDevice>(requireNotNull(first.device))
            .bluetoothGatts.single()

        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryEligibility(
                eligibleInput().copy(savedTargetIdentifier = SECOND_TARGET)
            )
        )
        idleMain()

        assertTrue(Shadow.extract<ShadowBluetoothGatt>(firstGatt).isClosed)
        assertEquals(HeartRateRecoveryPhase.SEARCHING, owner.recoveryState.value.phase)
        assertEquals(SECOND_TARGET, owner.recoveryState.value.targetIdentifier)
        assertEquals(1, shadowScanner.scanCallbacks.size)
    }

    @Test
    fun changingSavedTargetDuringRecoveryScanRejectsOldTargetAndRestartsForNewTarget() {
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        idleMain()
        val oldCallback = shadowScanner.scanCallbacks.single()

        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryEligibility(
                eligibleInput().copy(savedTargetIdentifier = SECOND_TARGET)
            )
        )
        idleMain()

        assertEquals(1, shadowScanner.scanCallbacks.size)
        val newCallback = shadowScanner.scanCallbacks.single()
        assertFalse(oldCallback === newCallback)
        assertEquals(SECOND_TARGET, owner.recoveryState.value.targetIdentifier)

        val oldTarget = scanResult(TARGET, "Old target")
        oldCallback.onScanResult(0, oldTarget)
        idleMain()
        assertTrue(
            Shadow.extract<ShadowBluetoothDevice>(requireNotNull(oldTarget.device))
                .bluetoothGatts.isEmpty()
        )

        val newTarget = scanResult(SECOND_TARGET, "New target")
        newCallback.onScanResult(0, newTarget)
        idleMain()
        assertEquals(
            1,
            Shadow.extract<ShadowBluetoothDevice>(requireNotNull(newTarget.device))
                .bluetoothGatts.size
        )
    }

    @Test
    fun repeatedEligibleContextKeepsCurrentRecoveryWindowAndExactTarget() {
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        idleMain()
        val callback = shadowScanner.scanCallbacks.single()

        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        idleMain()

        assertEquals(HeartRateRecoveryPhase.SEARCHING, owner.recoveryState.value.phase)
        assertEquals(TARGET, owner.recoveryState.value.targetIdentifier)
        assertEquals(1, shadowScanner.scanCallbacks.size)
        assertTrue(callback === shadowScanner.scanCallbacks.single())
    }

    @Test
    fun invalidManualTargetDoesNotReplaceArmedRecoveryTarget() {
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        idleMain()

        owner.submit(HeartRateRuntimeAction.Connect("AA:BB:CC:DD:EE:00"))
        idleMain()

        idleFor(SCAN_WINDOW_MS + RECOVERY_INTERVAL_MS)
        assertEquals(TARGET, owner.recoveryState.value.targetIdentifier)
        assertEquals(HeartRateRecoveryPhase.SEARCHING, owner.recoveryState.value.phase)
        assertEquals(1, shadowScanner.scanCallbacks.size)
    }

    @Test
    fun unexpectedDisconnectAndScanFailureKeepRecoveryArmed() {
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        idleMain()
        val target = scanResult(TARGET, "Saved Band")
        shadowScanner.scanCallbacks.single().onScanResult(0, target)
        idleMain()
        val gatt = Shadow.extract<ShadowBluetoothDevice>(requireNotNull(target.device))
            .bluetoothGatts.single()
        val callback = Shadow.extract<ShadowBluetoothGatt>(gatt).gattCallback

        callback.onConnectionStateChange(
            gatt,
            19,
            BluetoothProfile.STATE_DISCONNECTED
        )
        idleMain()

        assertEquals(HeartRateRecoveryPhase.WAITING_NEXT_WINDOW, owner.recoveryState.value.phase)
        assertTrue(Shadow.extract<ShadowBluetoothGatt>(gatt).isClosed)
        idleFor(RECOVERY_INTERVAL_MS)
        assertEquals(HeartRateRecoveryPhase.SEARCHING, owner.recoveryState.value.phase)

        shadowScanner.scanCallbacks.single().onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        idleMain()
        assertEquals(
            HeartRateRecoveryPhase.WINDOW_MISSED_ARMED,
            owner.recoveryState.value.phase
        )
        idleFor(RECOVERY_INTERVAL_MS)
        assertEquals(HeartRateRecoveryPhase.SEARCHING, owner.recoveryState.value.phase)
    }

    @Test
    fun permissionToctouFailureDisarmsInsteadOfLoopingRecoveryWindows() {
        shadowOf(application).denyPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )

        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        idleMain()

        assertEquals(HeartRateRecoveryPhase.DISARMED, owner.recoveryState.value.phase)
        assertEquals(
            HeartRateRecoveryStopReason.PERMISSION_UNAVAILABLE,
            owner.recoveryState.value.stopReason
        )
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
        idleFor(SCAN_WINDOW_MS + RECOVERY_INTERVAL_MS * 2)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    @Test
    fun queuedEligibilityLossCancelsImmediateRecoveryBeforePlatformAction() {
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput()))
        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryEligibility(
                eligibleInput().copy(manuallySuppressed = true)
            )
        )
        idleMain()

        assertEquals(HeartRateRecoveryPhase.DISARMED, owner.recoveryState.value.phase)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    private fun eligibleInput() = HeartRateRecoveryEligibilityInput(
        optedIn = true,
        savedTargetIdentifier = TARGET,
        permissionGranted = true,
        bluetoothEnabled = true,
        manuallySuppressed = false,
        appVisible = true,
        activeTrainingFgsActive = false
    )

    private fun scanResult(address: String, name: String): ScanResult {
        val device = ShadowBluetoothDevice.newInstance(address)
        Shadow.extract<ShadowBluetoothDevice>(device).setName(name)
        @Suppress("DEPRECATION")
        return ScanResult(device, null, -45, 1L)
    }

    private fun idleMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun idleFor(durationMs: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(durationMs))
    }

    private companion object {
        const val TARGET = "D8:F0:42:01:90:D7"
        const val SECOND_TARGET = "AA:BB:CC:DD:EE:42"
        const val SCAN_WINDOW_MS = 100L
        const val RECOVERY_INTERVAL_MS = 200L
    }
}
