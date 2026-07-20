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

        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())

        idleMain()

        assertEquals(HeartRateFact.SCANNING, owner.heartRateState.value.fact)
        assertEquals(1, shadowScanner.scanCallbacks.size)
    }

    @Test
    fun multipleQueuedActionsAreConsumedInSubmissionOrder() {
        owner.submit(HeartRateRuntimeAction.StartScan)
        owner.submit(HeartRateRuntimeAction.StopScan)
        owner.submit(HeartRateRuntimeAction.StartScan)

        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)
        idleMain()

        assertEquals(HeartRateFact.SCANNING, owner.heartRateState.value.fact)
        assertEquals(1, shadowScanner.scanCallbacks.size)
    }

    @Test
    fun repeatedScanUsesNewGenerationAndRejectsOldResultAndFailure() {
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
        idleMain()

        assertEquals(HeartRateFact.PERMISSION_REQUIRED, owner.heartRateState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    @Test
    fun bluetoothOffProducesTypedFactWithoutStartingScan() {
        val adapter = application.getSystemService(BluetoothManager::class.java).adapter
        shadowOf(adapter).setEnabled(false)

        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()

        assertEquals(HeartRateFact.BLUETOOTH_OFF, owner.heartRateState.value.fact)
        assertTrue(shadowScanner.scanCallbacks.isEmpty())
    }

    @Test
    fun ownerCloseIsIdempotentAndCannotCreateANewAction() {
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

    private fun scanResult(address: String, name: String): ScanResult {
        val device = ShadowBluetoothDevice.newInstance(address)
        Shadow.extract<ShadowBluetoothDevice>(device).setName(name)
        @Suppress("DEPRECATION")
        return ScanResult(device, null, -45, 1L)
    }

    private fun idleMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun privateRunnable(fieldName: String): Runnable {
        val field = HeartRateRuntimeOwner::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return requireNotNull(field.get(owner) as Runnable?)
    }
}
