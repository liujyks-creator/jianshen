package com.liujyks.trainflow.core.health

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.model.HeartRateFact
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothLeScanner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class HeartRateRuntimeOwnerTest {
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
            mainHandler = android.os.Handler(Looper.getMainLooper()),
            scanWindowMillis = 100L
        )
    }

    @Test
    fun constructionDoesNotScanOrConnectAndQueuedActionsWaitForMain() {
        owner.submit(HeartRateRuntimeAction.StartScan)
        owner.submit(HeartRateRuntimeAction.Connect("AA:BB:CC:DD:EE:00"))

        assertEquals(HeartRateFact.DISABLED, owner.heartRateState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        idleMain()

        assertEquals(HeartRateFact.DISABLED, owner.heartRateState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        owner.submit(HeartRateRuntimeAction.Enable)
        assertEquals(HeartRateFact.DISABLED, owner.heartRateState.value.fact)
        idleMain()
        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)
    }

    @Test
    fun multipleQueuedActionsAreConsumedInSubmissionOrder() {
        owner.submit(HeartRateRuntimeAction.Enable)
        owner.submit(HeartRateRuntimeAction.StartScan)
        owner.submit(HeartRateRuntimeAction.StopScan)
        owner.submit(HeartRateRuntimeAction.StartScan)

        assertEquals(HeartRateFact.DISABLED, owner.heartRateState.value.fact)
        idleMain()

        assertEquals(HeartRateFact.SCANNING, owner.heartRateState.value.fact)
        assertEquals(1, shadowScanner.scanCallbacks.size)
    }

    @Test
    fun repeatedScanUsesNewGenerationAndRejectsOldResultAndFailure() {
        enableOwner()
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val oldCallback = shadowScanner.scanCallbacks.single()

        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val newCallback = shadowScanner.scanCallbacks.single()

        assertNotSame(oldCallback, newCallback)
        oldCallback.onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            scanResult("AA:BB:CC:DD:EE:10", "Old")
        )
        oldCallback.onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        idleMain()

        assertTrue(owner.candidates.value.isEmpty())
        assertEquals(HeartRateFact.SCANNING, owner.heartRateState.value.fact)

        newCallback.onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            scanResult("AA:BB:CC:DD:EE:11", "Current")
        )
        idleMain()
        assertEquals(listOf("AA:BB:CC:DD:EE:11"), owner.candidates.value.map { it.identifier })
    }

    @Test
    fun scanTimeoutIsFiniteAndOldTimeoutCannotStopNewGeneration() {
        owner = HeartRateRuntimeOwner(
            application,
            mainHandler = android.os.Handler(Looper.getMainLooper()),
            scanWindowMillis = 100L
        )
        enableOwner()
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val oldTimeout = privateRunnable("scanTimeoutRunnable")

        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()

        android.os.Handler(Looper.getMainLooper()).post(oldTimeout)
        idleMain()

        assertEquals(HeartRateFact.SCANNING, owner.heartRateState.value.fact)
        assertEquals(1, shadowScanner.scanCallbacks.size)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    @Test
    fun stopInvalidatesGenerationBeforeLateCallbackAndManualTimeoutExecution() {
        owner = HeartRateRuntimeOwner(
            application,
            mainHandler = android.os.Handler(Looper.getMainLooper()),
            scanWindowMillis = 100L
        )
        enableOwner()
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val callback = shadowScanner.scanCallbacks.single()
        val timeout = privateRunnable("scanTimeoutRunnable")

        owner.submit(HeartRateRuntimeAction.Stop)
        idleMain()
        val terminal = owner.heartRateState.value

        callback.onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            scanResult("AA:BB:CC:DD:EE:12", "Late")
        )
        callback.onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        android.os.Handler(Looper.getMainLooper()).post(timeout)
        idleMain()

        assertEquals(terminal, owner.heartRateState.value)
        assertEquals(HeartRateFact.INTENTIONAL_STOP, owner.heartRateState.value.fact)
        assertTrue(owner.candidates.value.isEmpty())
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    @Test
    fun permissionRecoveryDoesNotCreateAutomaticAction() {
        enableOwner()
        shadowOf(application).denyPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        assertEquals(HeartRateFact.PERMISSION_REQUIRED, owner.heartRateState.value.fact)

        shadowOf(application).grantPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        owner.submit(HeartRateRuntimeAction.Enable)
        assertEquals(HeartRateFact.PERMISSION_REQUIRED, owner.heartRateState.value.fact)
        idleMain()

        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    @Test
    fun bluetoothOffProducesTypedFactWithoutStartingScan() {
        enableOwner()
        val adapter = application.getSystemService(BluetoothManager::class.java).adapter
        shadowOf(adapter).setEnabled(false)

        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()

        assertEquals(HeartRateFact.BLUETOOTH_OFF, owner.heartRateState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    @Test
    fun ownerCloseIsIdempotentAndCannotCreateANewAction() {
        enableOwner()
        owner.close()
        idleMain()
        val terminal = owner.heartRateState.value

        owner.close()
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()

        assertEquals(HeartRateFact.INTENTIONAL_STOP, terminal.fact)
        assertEquals(terminal, owner.heartRateState.value)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    @Test
    fun disableEnableAndExplicitRestartAreReversibleOnTheSameOwner() {
        enableOwner()
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val result = scanResult("AA:BB:CC:DD:EE:13", "Reusable")
        shadowScanner.scanCallbacks.single().onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            result
        )
        idleMain()
        assertFalse(owner.candidates.value.isEmpty())

        owner.submit(HeartRateRuntimeAction.Disable)
        owner.submit(HeartRateRuntimeAction.Disable)
        idleMain()

        assertEquals(HeartRateFact.DISABLED, owner.heartRateState.value.fact)
        assertTrue(owner.candidates.value.isEmpty())
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        owner.submit(HeartRateRuntimeAction.Enable)
        owner.submit(HeartRateRuntimeAction.Enable)
        idleMain()
        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        assertEquals(HeartRateFact.SCANNING, owner.heartRateState.value.fact)
        assertEquals(1, shadowScanner.scanCallbacks.size)
    }

    @Test
    fun lifecycleLossFactsRequireEnableAndNeverAutoResumeQueuedActions() {
        enableOwner()
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val result = scanResult("AA:BB:CC:DD:EE:14", "Queued target")
        val device = requireNotNull(result.device)
        shadowScanner.scanCallbacks.single().onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            result
        )
        idleMain()

        owner.submit(HeartRateRuntimeAction.PermissionLost)
        owner.submit(HeartRateRuntimeAction.StartScan)
        owner.submit(HeartRateRuntimeAction.Connect(device.address))
        owner.submit(HeartRateRuntimeAction.Enable)
        idleMain()

        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
        assertTrue(Shadow.extract<ShadowBluetoothDevice>(device).bluetoothGatts.isEmpty())

        owner.submit(HeartRateRuntimeAction.BluetoothOff)
        idleMain()
        assertEquals(HeartRateFact.BLUETOOTH_OFF, owner.heartRateState.value.fact)
        owner.submit(HeartRateRuntimeAction.Enable)
        idleMain()
        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)

        owner.submit(HeartRateRuntimeAction.BackgroundCleanup)
        idleMain()
        assertEquals(HeartRateFact.INTENTIONAL_STOP, owner.heartRateState.value.fact)
        owner.submit(HeartRateRuntimeAction.Enable)
        idleMain()
        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)
    }

    @Test
    fun lifecycleLossInputsWhileDisabledKeepDisabledAndTouchNoBlePlatform() {
        owner.submit(HeartRateRuntimeAction.PermissionLost)
        owner.submit(HeartRateRuntimeAction.BluetoothOff)
        owner.submit(HeartRateRuntimeAction.BackgroundCleanup)
        owner.submit(HeartRateRuntimeAction.StartScan)
        owner.submit(HeartRateRuntimeAction.Connect("AA:BB:CC:DD:EE:15"))
        idleMain()

        assertEquals(HeartRateFact.DISABLED, owner.heartRateState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    @Test
    fun eligibleRecoveryStartsImmediatelyThenRepeatsFiniteWindowsAcrossFixedGaps() {
        val target = "AA:BB:CC:DD:EE:50"

        owner.submit(HeartRateRuntimeAction.UpdateRecoveryContext(eligibleRecovery(target)))
        idleMain()

        assertEquals(HeartRateRecoveryFact.AUTO_SEARCHING, owner.recoveryState.value.fact)
        assertEquals(target, owner.recoveryState.value.exactTargetIdentifier)
        val firstCallback = shadowScanner.scanCallbacks.single()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(99))
        assertEquals(HeartRateRecoveryFact.AUTO_SEARCHING, owner.recoveryState.value.fact)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
        assertEquals(HeartRateRecoveryFact.WINDOW_NO_MATCH_ARMED, owner.recoveryState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(9_999))
        assertEquals(HeartRateRecoveryFact.WINDOW_NO_MATCH_ARMED, owner.recoveryState.value.fact)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
        val secondCallback = shadowScanner.scanCallbacks.single()
        assertNotSame(firstCallback, secondCallback)
        assertEquals(HeartRateRecoveryFact.AUTO_SEARCHING, owner.recoveryState.value.fact)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10_000))
        val thirdCallback = shadowScanner.scanCallbacks.single()
        assertNotSame(secondCallback, thirdCallback)
        assertEquals(HeartRateRecoveryFact.AUTO_SEARCHING, owner.recoveryState.value.fact)
    }

    @Test
    fun suppressionAndTargetChangeCancelStaleRecoveryClosuresByContextGeneration() {
        val firstTarget = "AA:BB:CC:DD:EE:51"
        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryContext(eligibleRecovery(firstTarget))
        )
        idleMain()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        val staleGap = privateRunnable("recoveryGapRunnable")

        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryContext(
                eligibleRecovery(firstTarget).copy(manualDisconnectSuppressed = true)
            )
        )
        idleMain()
        assertEquals(HeartRateRecoveryFact.BLOCKED, owner.recoveryState.value.fact)
        assertEquals(
            HeartRateRecoveryBlockedReason.MANUAL_DISCONNECT_SUPPRESSED,
            owner.recoveryState.value.blockedReason
        )

        android.os.Handler(Looper.getMainLooper()).post(staleGap)
        idleMain()
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        val secondTarget = "AA:BB:CC:DD:EE:52"
        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryContext(eligibleRecovery(secondTarget))
        )
        idleMain()
        val currentCallback = shadowScanner.scanCallbacks.single()
        android.os.Handler(Looper.getMainLooper()).post(staleGap)
        idleMain()

        assertEquals(secondTarget, owner.recoveryState.value.exactTargetIdentifier)
        assertEquals(setOf(currentCallback), shadowScanner.scanCallbacks)
    }

    @Test
    fun exactIdentifierAutoConnectsButSameNameWrongIdentifierDoesNot() {
        val targetAddress = "AA:BB:CC:DD:EE:53"
        val wrong = scanResult("AA:BB:CC:DD:EE:54", "Same name")
        val target = scanResult(targetAddress, "Same name")
        val targetDevice = requireNotNull(target.device)

        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryContext(eligibleRecovery(targetAddress))
        )
        idleMain()
        val callback = shadowScanner.scanCallbacks.single()
        callback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, wrong)
        idleMain()
        assertTrue(
            Shadow.extract<ShadowBluetoothDevice>(requireNotNull(wrong.device))
                .bluetoothGatts
                .isEmpty()
        )
        assertEquals(HeartRateRecoveryFact.AUTO_SEARCHING, owner.recoveryState.value.fact)

        callback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, target)
        idleMain()

        assertEquals(1, Shadow.extract<ShadowBluetoothDevice>(targetDevice).bluetoothGatts.size)
        assertEquals(HeartRateRecoveryFact.CONNECTING, owner.recoveryState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    @Test
    fun queuedEligibilityLossWinsAndManualScanDoesNotMasqueradeAsRecovery() {
        val target = "AA:BB:CC:DD:EE:55"
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryContext(eligibleRecovery(target)))
        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryContext(
                eligibleRecovery(target).copy(appVisible = false)
            )
        )
        idleMain()

        assertEquals(HeartRateRecoveryFact.BLOCKED, owner.recoveryState.value.fact)
        assertEquals(
            HeartRateRecoveryBlockedReason.NOT_VISIBLE_OR_TRAINING_FGS,
            owner.recoveryState.value.blockedReason
        )
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        owner.submit(HeartRateRuntimeAction.Enable)
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        assertEquals(HeartRateFact.SCANNING, owner.heartRateState.value.fact)
        assertEquals(HeartRateRecoveryFact.BLOCKED, owner.recoveryState.value.fact)
    }

    @Test
    fun typedEligibilityLossesCancelRecoveryAndExplicitContextReturnRestartsIt() {
        val target = "AA:BB:CC:DD:EE:56"
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryContext(eligibleRecovery(target)))
        idleMain()

        owner.submit(HeartRateRuntimeAction.PermissionLost)
        idleMain()
        assertEquals(
            HeartRateRecoveryBlockedReason.PERMISSION_REQUIRED,
            owner.recoveryState.value.blockedReason
        )
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        owner.submit(HeartRateRuntimeAction.UpdateRecoveryContext(eligibleRecovery(target)))
        idleMain()
        assertEquals(HeartRateRecoveryFact.AUTO_SEARCHING, owner.recoveryState.value.fact)
        owner.submit(HeartRateRuntimeAction.BluetoothOff)
        idleMain()
        assertEquals(
            HeartRateRecoveryBlockedReason.BLUETOOTH_OFF,
            owner.recoveryState.value.blockedReason
        )

        owner.submit(HeartRateRuntimeAction.UpdateRecoveryContext(eligibleRecovery(target)))
        idleMain()
        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryContext(
                eligibleRecovery(target).copy(savedTargetIdentifier = null)
            )
        )
        idleMain()
        assertEquals(
            HeartRateRecoveryBlockedReason.SAVED_TARGET_MISSING,
            owner.recoveryState.value.blockedReason
        )
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        owner.submit(HeartRateRuntimeAction.UpdateRecoveryContext(eligibleRecovery(target)))
        idleMain()
        owner.submit(HeartRateRuntimeAction.Disable)
        idleMain()
        assertEquals(
            HeartRateRecoveryBlockedReason.OPTED_OUT,
            owner.recoveryState.value.blockedReason
        )
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    @Test
    fun explicitDisconnectSuppressesRecoveryUntilExplicitContextClear() {
        val target = "AA:BB:CC:DD:EE:57"
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryContext(eligibleRecovery(target)))
        idleMain()

        owner.submit(HeartRateRuntimeAction.Disconnect)
        idleMain()
        assertEquals(HeartRateFact.INTENTIONAL_STOP, owner.heartRateState.value.fact)
        assertEquals(
            HeartRateRecoveryBlockedReason.MANUAL_DISCONNECT_SUPPRESSED,
            owner.recoveryState.value.blockedReason
        )
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryContext(
                eligibleRecovery(target).copy(manualDisconnectSuppressed = false)
            )
        )
        idleMain()
        assertEquals(HeartRateRecoveryFact.AUTO_SEARCHING, owner.recoveryState.value.fact)
        assertEquals(1, shadowScanner.scanCallbacks.size)
    }

    private fun scanResult(address: String, name: String): ScanResult {
        val device = ShadowBluetoothDevice.newInstance(address)
        Shadow.extract<ShadowBluetoothDevice>(device).setName(name)
        @Suppress("DEPRECATION")
        return ScanResult(device, null, -45, 1L)
    }

    private fun idleMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun enableOwner() {
        owner.submit(HeartRateRuntimeAction.Enable)
        idleMain()
        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)
    }

    private fun eligibleRecovery(target: String) = HeartRateRecoveryInputs(
        optIn = true,
        savedTargetIdentifier = target,
        permissionGranted = true,
        bluetoothEnabled = true,
        manualDisconnectSuppressed = false,
        appVisible = true,
        legalTrainingFgs = false
    )

    private fun privateRunnable(fieldName: String): Runnable {
        val field = HeartRateRuntimeOwner::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return requireNotNull(field.get(owner) as Runnable?)
    }
}
