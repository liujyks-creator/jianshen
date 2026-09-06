package com.liujyks.trainflow.core.health

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import java.time.Duration
import java.util.UUID
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothGatt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 35], shadows = [E17GattShadow::class, E17ScannerShadow::class])
@LooperMode(LooperMode.Mode.PAUSED)
@Suppress("DEPRECATION")
class HeartRateRuntimeObservationTest {
    private lateinit var application: Application
    private lateinit var owner: HeartRateRuntimeOwner
    private val ledger = mutableListOf<HeartRateObservation>()
    private lateinit var id: HeartRateObservationBindingId

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        shadowOf(application.getSystemService(BluetoothManager::class.java).adapter).setEnabled(true)
        E17GattShadow.resetFailures()
        E17ScannerShadow.resetFailures()
        owner = HeartRateRuntimeOwner(application)
        id = HeartRateObservationBindingId()
    }

    @Test
    fun permissionHistoryDistinguishesMissingRevokedAndRepeatedRevocation() {
        shadowOf(application).denyPermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
        bind()
        owner.submit(HeartRateRuntimeAction.Enable)
        owner.submit(HeartRateRuntimeAction.StartScan)
        idle()
        assertEquals(HeartRateObservationCause.PERMISSION_MISSING, causes().last())
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
        owner.submit(HeartRateRuntimeAction.Enable)
        owner.submit(HeartRateRuntimeAction.StartScan)
        idle()
        owner.submit(HeartRateRuntimeAction.PermissionLost)
        idle()
        assertEquals(HeartRateObservationCause.PERMISSION_REVOKED, causes().last())
        val before = ledger.toList()
        owner.submit(HeartRateRuntimeAction.PermissionLost)
        idle()
        assertEquals(before, ledger)
        assertEquals(HeartRateUnbindDisposition.REMOVED, owner.unbindObservations(id))
        val restored = bind()
        assertEquals(HeartRateObservationPayload.CurrentSnapshot(HeartRateObservationCause.PERMISSION_REVOKED), restored.snapshot.payload)
    }

    @Test
    fun manualAttemptAndConcurrentScanDoNotLoseInitialOrLiveCause() {
        owner = HeartRateRuntimeOwner(application, scanWindowMillis = 100)
        val connection = connect()
        bind()
        val input = HeartRateRecoveryEligibilityInput(true, "AA:BB:CC:DD:EE:71", true, true, false, true, false)
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(input))
        owner.submit(HeartRateRuntimeAction.StartScan)
        idle()
        val scanner = application.getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
        shadowOf(scanner).scanCallbacks.single().onScanFailed(android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        assertTrue(ledger.isEmpty())
        owner.submit(HeartRateRuntimeAction.StartScan)
        idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertTrue(ledger.isEmpty())
        connection.notify(byteArrayOf(0, 91))
        val liveLedger = ledger.toList()
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(input))
        idle()
        assertEquals(liveLedger, ledger)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2_500))
        val staleLedger = ledger.toList()
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(input))
        idle()
        assertEquals(staleLedger, ledger)
        assertEquals(listOf(HeartRateObservationCause.LIVE, HeartRateObservationCause.SAMPLE_STALE_TIMEOUT), causes())
    }

    @Test
    fun manualBrowseTimeoutOnlyClaimsSourceUnavailableWhenThereIsExpectedTarget() {
        owner = HeartRateRuntimeOwner(application, scanWindowMillis = 100)
        bind()
        owner.submit(HeartRateRuntimeAction.Enable)
        owner.submit(HeartRateRuntimeAction.StartScan)
        idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertEquals(listOf(HeartRateObservationCause.DISCONNECTED, HeartRateObservationCause.INITIAL_SEARCH, HeartRateObservationCause.DISCONNECTED), causes())
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(HeartRateRecoveryEligibilityInput(true, null, true, true, false, true, false)))
        idle()
        assertEquals(HeartRateObservationCause.NO_SOURCE_SELECTED, causes().last())
    }

    @Test
    fun bindingCutIsSynchronousAndOffMainCallsDoNotInstall() {
        owner.submit(HeartRateRuntimeAction.Enable)
        val binding = bind()
        assertEquals(HeartRateObservationPayload.CurrentSnapshot(HeartRateObservationCause.NOT_OBSERVING), binding.snapshot.payload)
        assertTrue(ledger.isEmpty())
        idle()
        assertEquals(listOf(1L), ledger.map { it.receipt })
        val before = ledger.toList()
        val calls = java.util.concurrent.FutureTask {
            assertThrows(IllegalStateException::class.java) { owner.unbindObservations(id) }
            assertThrows(IllegalStateException::class.java) { owner.queryObservationBinding(id) }
            assertThrows(IllegalStateException::class.java) { owner.bindObservations(HeartRateObservationBindingId()) {} }
        }
        Thread(calls).apply { start(); join() }
        calls.get()
        assertSame(binding, (owner.queryObservationBinding(id) as HeartRateBindingDisposition.MatchingInstalled).binding)
        assertEquals(before, ledger)
    }

    @Test
    fun checkedReceiptOverflowIsNotDeliveredAsSuccess() {
        val connection = connect()
        connection.notify(byteArrayOf(0, 80))
        bind()
        // Counter-boundary injection only; callbacks and dispatch are the production path.
        val field = HeartRateRuntimeOwner::class.java.getDeclaredField("observationReceipt")
        field.isAccessible = true
        field.setLong(owner, Long.MAX_VALUE)
        assertThrows(ArithmeticException::class.java) { connection.notify(byteArrayOf(0, 81)) }
        assertTrue(ledger.isEmpty())
        assertEquals(Long.MAX_VALUE, field.getLong(owner))
    }

    @Test
    fun identicalSameMillisecondNotificationsRemainDistinctAndOrdered() {
        val connection = connect()
        val binding = bind()
        val now = SystemClock.elapsedRealtime()
        connection.notify(byteArrayOf(0, 88))
        connection.notify(byteArrayOf(0, 88))
        connection.notify(byteArrayOf(0, 91))
        connection.notify(byteArrayOf(1, 44, 1))

        assertEquals(listOf(88, 88, 91, 300), measurements())
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), ledger.map { it.receipt })
        assertTrue(ledger.all { it.bindingId === id && it.elapsedRealtimeMs == now })
        assertEquals(HeartRateObservationPayload.RuntimeTransition(HeartRateObservationCause.LIVE), ledger[0].payload)
        assertEquals(HeartRateObservationPayload.CurrentSnapshot(HeartRateObservationCause.INITIAL_WAIT), binding.snapshot.payload)
    }

    @Test
    fun versionEntryCopiesBytesBeforePostingAndIgnoresOtherOverload() {
        val connection = connect()
        bind()
        val bytes = byteArrayOf(0, 87)
        val notifications = java.util.concurrent.FutureTask {
            connection.notify(bytes)
            bytes[1] = 120
            connection.characteristic.value = byteArrayOf(0, 121)
            if (Build.VERSION.SDK_INT >= 33) {
                connection.callback.onCharacteristicChanged(connection.gatt, connection.characteristic)
            } else {
                // SDK32 cannot dispatch this absent framework method. Exercise the owner's
                // concrete override only to verify its version guard, without a production seam.
                val valueOverload = connection.callback.javaClass.getDeclaredMethod(
                    "onCharacteristicChanged", BluetoothGatt::class.java,
                    BluetoothGattCharacteristic::class.java, ByteArray::class.java
                )
                valueOverload.isAccessible = true
                valueOverload.invoke(connection.callback, connection.gatt, connection.characteristic, byteArrayOf(0, 122))
            }
        }
        Thread(notifications).apply { start(); join() }
        notifications.get()
        assertTrue(ledger.isEmpty())
        idle()
        assertEquals(listOf(87), measurements())
    }

    @Test
    fun originalSnapshotAndSinkSurviveRetryQueryConflictAndExactUnbind() {
        val connection = connect()
        connection.notify(byteArrayOf(0, 80))
        val binding = bind()
        assertEquals(0L, binding.snapshot.receipt)
        assertEquals(binding.anchorElapsedRealtimeMs, binding.snapshot.elapsedRealtimeMs)
        assertEquals(HeartRateObservationPayload.CurrentSnapshot(HeartRateObservationCause.LIVE), binding.snapshot.payload)
        assertTrue(ledger.isEmpty())
        val otherLedger = mutableListOf<HeartRateObservation>()
        val other = HeartRateObservationBindingId()
        assertSame(binding, (owner.bindObservations(id, otherLedger::add) as HeartRateBindingDisposition.MatchingInstalled).binding)
        assertSame(binding, (owner.queryObservationBinding(id) as HeartRateBindingDisposition.MatchingInstalled).binding)
        assertEquals(HeartRateBindingDisposition.ConflictingInstalled(id), owner.bindObservations(other, otherLedger::add))
        assertEquals(HeartRateBindingDisposition.ConflictingInstalled(id), owner.queryObservationBinding(other))
        assertEquals(HeartRateUnbindDisposition.CONFLICTING_INSTALLED, owner.unbindObservations(other))
        connection.notify(byteArrayOf(0, 81))
        assertEquals(listOf(81), measurements())
        assertEquals(listOf(1L), ledger.map { it.receipt })
        assertTrue(otherLedger.isEmpty())
        assertSame(binding, (owner.bindObservations(id, otherLedger::add) as HeartRateBindingDisposition.MatchingInstalled).binding)
        assertEquals(listOf(1L), ledger.map { it.receipt })
        assertEquals(HeartRateUnbindDisposition.REMOVED, owner.unbindObservations(id))
        assertEquals(HeartRateUnbindDisposition.KNOWN_ABSENT, owner.unbindObservations(id))
        assertEquals(HeartRateBindingDisposition.KnownAbsent, owner.queryObservationBinding(id))
        connection.notify(byteArrayOf(0, 82))
        assertEquals(listOf(81), measurements())
        assertFalse(connection.shadow.isClosed)
        assertEquals(82, owner.heartRateState.value.bpm)
        val next = (owner.bindObservations(other, otherLedger::add) as HeartRateBindingDisposition.MatchingInstalled).binding
        assertEquals(0L, next.snapshot.receipt)
        assertTrue(otherLedger.isEmpty())
        connection.notify(byteArrayOf(0, 83))
        assertEquals(listOf(1L), otherLedger.map { it.receipt })
        assertEquals(HeartRateObservationPayload.ValidMeasurement(83), otherLedger.single().payload)
    }

    @Test
    fun malformedAndWrongCharacteristicDoNotRefreshOrEmitAndTimeoutCausesDiffer() {
        val connection = connect()
        bind()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10_000))
        assertEquals(listOf(HeartRateObservationCause.FIRST_SAMPLE_TIMEOUT), causes())
        connection.notify(byteArrayOf(0, 90))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2_000))
        connection.notify(byteArrayOf(1))
        val wrong = BluetoothGattCharacteristic(UUID.randomUUID(), 16, 1)
        if (Build.VERSION.SDK_INT >= 33) {
            connection.callback.onCharacteristicChanged(connection.gatt, wrong, byteArrayOf(0, 110))
        } else {
            wrong.value = byteArrayOf(0, 110)
            connection.callback.onCharacteristicChanged(connection.gatt, wrong)
        }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        assertEquals(listOf(90), measurements())
        assertEquals(listOf(HeartRateObservationCause.FIRST_SAMPLE_TIMEOUT, HeartRateObservationCause.LIVE, HeartRateObservationCause.SAMPLE_STALE_TIMEOUT), causes())
    }

    private fun bind(): HeartRateObservationBinding =
        (owner.bindObservations(id, ledger::add) as HeartRateBindingDisposition.MatchingInstalled).binding

    private fun measurements() = ledger.mapNotNull { (it.payload as? HeartRateObservationPayload.ValidMeasurement)?.bpm }
    private fun causes() = ledger.mapNotNull { (it.payload as? HeartRateObservationPayload.RuntimeTransition)?.cause }
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun connect(): Connection {
        owner.submit(HeartRateRuntimeAction.Enable)
        owner.submit(HeartRateRuntimeAction.StartScan)
        idle()
        val device = ShadowBluetoothDevice.newInstance("AA:BB:CC:DD:EE:71")
        shadowOf(device).setName("Observation fixture")
        val scanner = application.getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
        shadowOf(scanner).scanCallbacks.single().onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, ScanResult(device, null, -40, 1L))
        lateinit var result: Connection
        shadowOf(device).setGattConnectionInterceptor { gatt ->
            val shadow = Shadow.extract<ShadowBluetoothGatt>(gatt)
            val service = BluetoothGattService(UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"), 0)
            val characteristic = BluetoothGattCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"), 16, 1)
            characteristic.addDescriptor(BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), 16))
            service.addCharacteristic(characteristic)
            shadow.addDiscoverableService(service)
            shadow.allowCharacteristicNotification(characteristic)
            result = Connection(gatt, shadow, characteristic)
            shadow.gattCallback.onConnectionStateChange(gatt, 0, BluetoothProfile.STATE_CONNECTED)
        }
        owner.submit(HeartRateRuntimeAction.Connect(device.address))
        idle()
        return result
    }

    private class Connection(val gatt: BluetoothGatt, val shadow: ShadowBluetoothGatt, val characteristic: BluetoothGattCharacteristic) {
        val callback get() = shadow.gattCallback
        fun notify(bytes: ByteArray) {
            if (Build.VERSION.SDK_INT >= 33) callback.onCharacteristicChanged(gatt, characteristic, bytes)
            else {
                characteristic.value = bytes
                callback.onCharacteristicChanged(gatt, characteristic)
            }
        }
    }
}
