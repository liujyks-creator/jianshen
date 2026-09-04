package com.liujyks.trainflow.core.health

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothGatt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [E17GattShadow::class, E17ScannerShadow::class])
@LooperMode(LooperMode.Mode.PAUSED)
class HeartRateRuntimeObservationTest {
    private lateinit var application: Application
    private lateinit var owner: HeartRateRuntimeOwner

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
        owner = HeartRateRuntimeOwner(application, Handler(Looper.getMainLooper()))
    }

    @Test
    fun bindReturnsSnapshotZeroAndDoesNotReplayFactsPublishedWithoutABinding() {
        owner.submit(HeartRateRuntimeAction.Enable)
        owner.submit(HeartRateRuntimeAction.Disable)
        idleMain()
        val received = mutableListOf<HeartRateRuntimeObservation>()

        val installed = owner.bindPersistenceSink(bindingId("one"), received::add)

        assertTrue(installed is HeartRatePersistenceBindResult.Installed)
        val binding = (installed as HeartRatePersistenceBindResult.Installed).binding
        assertEquals(0L, binding.snapshot.receipt)
        assertEquals(
            HeartRateRuntimeObservationPayload.CurrentSnapshot(
                HeartRateRuntimeObservationCause.NOT_OBSERVING
            ),
            binding.snapshot.payload
        )
        assertTrue(received.isEmpty())

        owner.submit(HeartRateRuntimeAction.Enable)
        idleMain()
        assertEquals(listOf(1L), received.map { it.receipt })
        assertEquals(
            HeartRateRuntimeObservationPayload.RuntimeTransition(
                HeartRateRuntimeObservationCause.NO_SOURCE_SELECTED
            ),
            received.single().payload
        )
    }

    @Test
    fun everyEqualBpmNotifyHasItsOwnStrictReceiptBeforePresentationDeduplication() {
        val received = mutableListOf<HeartRateRuntimeObservation>()
        owner.bindPersistenceSink(bindingId("samples"), received::add)
        val connected = connect("AA:BB:CC:DD:EE:51")

        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 88)
        )
        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 88)
        )

        val samples = received.filter {
            it.payload is HeartRateRuntimeObservationPayload.ValidMeasurement
        }
        assertEquals(listOf(88, 88), samples.map {
            (it.payload as HeartRateRuntimeObservationPayload.ValidMeasurement).bpm
        })
        assertEquals(2, samples.map { it.receipt }.distinct().size)
        assertEquals((1L..received.size.toLong()).toList(), received.map { it.receipt })
    }

    @Test
    fun bindDispositionAndExactUnbindShareTheMainLooperPublicationCut() {
        val executor = Executors.newSingleThreadExecutor()
        val entered = CountDownLatch(1)
        try {
            val bindCompleted = CountDownLatch(1)
            val bind = executor.submit<HeartRatePersistenceBindResult> {
                entered.countDown()
                try {
                    owner.bindPersistenceSink(bindingId("linearized")) { }
                } finally {
                    bindCompleted.countDown()
                }
            }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            assertFalse(
                "off-main bind must wait for the main-looper cut",
                bindCompleted.await(200, TimeUnit.MILLISECONDS)
            )

            idleMain()
            assertTrue(bind.get(2, TimeUnit.SECONDS) is HeartRatePersistenceBindResult.Installed)

            val dispositionCompleted = CountDownLatch(1)
            val disposition = executor.submit<HeartRatePersistenceBindingDisposition> {
                try {
                    owner.persistenceBindingDisposition(bindingId("linearized"))
                } finally {
                    dispositionCompleted.countDown()
                }
            }
            assertFalse(
                "off-main disposition must wait for the main-looper cut",
                dispositionCompleted.await(200, TimeUnit.MILLISECONDS)
            )
            idleMain()
            assertTrue(disposition.get(2, TimeUnit.SECONDS) is
                HeartRatePersistenceBindingDisposition.MatchingInstalled)

            val unbindCompleted = CountDownLatch(1)
            val unbind = executor.submit<HeartRatePersistenceUnbindResult> {
                try {
                    owner.exactUnbindPersistenceSink(bindingId("linearized"))
                } finally {
                    unbindCompleted.countDown()
                }
            }
            assertFalse(
                "off-main unbind must wait for the main-looper cut",
                unbindCompleted.await(200, TimeUnit.MILLISECONDS)
            )
            idleMain()
            assertEquals(
                HeartRatePersistenceUnbindResult.Unbound(bindingId("linearized")),
                unbind.get(2, TimeUnit.SECONDS)
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun onlyThePairedPlatformOverloadsForOneCallbackAreDeduplicated() {
        val received = mutableListOf<HeartRateRuntimeObservation>()
        owner.bindPersistenceSink(bindingId("overloads"), received::add)
        val connected = connect("AA:BB:CC:DD:EE:52")
        val payload = byteArrayOf(0x00, 89)

        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            payload
        )
        @Suppress("DEPRECATION")
        run {
            connected.characteristic.value = payload
            connected.callback.onCharacteristicChanged(
                connected.gatt,
                connected.characteristic
            )
        }
        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            payload
        )
        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            payload
        )

        val samples = received.filter {
            it.payload is HeartRateRuntimeObservationPayload.ValidMeasurement
        }
        assertEquals(listOf(89, 89, 89), samples.map {
            (it.payload as HeartRateRuntimeObservationPayload.ValidMeasurement).bpm
        })
        assertEquals(3, samples.map { it.receipt }.distinct().size)
    }

    @Test
    fun unexpectedDisconnectPublishesItsRealReconnectObligation() {
        val received = mutableListOf<HeartRateRuntimeObservation>()
        owner.bindPersistenceSink(bindingId("unexpected-reconnect"), received::add)
        val address = "AA:BB:CC:DD:EE:53"
        val connected = connect(address)
        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryEligibility(eligibleInput(address))
        )
        idleMain()

        connected.callback.onConnectionStateChange(
            connected.gatt,
            19,
            BluetoothProfile.STATE_DISCONNECTED
        )
        idleMain()

        assertTrue(received.causes().contains(
            HeartRateRuntimeObservationCause.UNEXPECTED_DISCONNECT
        ))
        assertTrue(received.causes().contains(
            HeartRateRuntimeObservationCause.UNEXPECTED_DISCONNECT_RECONNECTING
        ))
    }

    @Test
    fun failedRecoveryWindowPublishesRecoveryReconnectingFromTheRealScanCallback() {
        val received = mutableListOf<HeartRateRuntimeObservation>()
        owner.bindPersistenceSink(bindingId("recovery-reconnect"), received::add)
        owner.submit(
            HeartRateRuntimeAction.UpdateRecoveryEligibility(
                eligibleInput("AA:BB:CC:DD:EE:54")
            )
        )
        idleMain()

        val scanner = application.getSystemService(BluetoothManager::class.java)
            .adapter.bluetoothLeScanner
        shadowOf(scanner).scanCallbacks.single()
            .onScanFailed(android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        idleMain()

        assertTrue(received.causes().contains(
            HeartRateRuntimeObservationCause.RECOVERY_RECONNECTING
        ))
    }

    @Test
    fun dispositionAndExactUnbindAreIdentityBoundAcrossResultLossAndConflict() {
        val first = bindingId("first")
        val second = bindingId("second")
        val ignoredBindResult = owner.bindPersistenceSink(first) { }
        assertTrue(ignoredBindResult is HeartRatePersistenceBindResult.Installed)

        assertTrue(owner.persistenceBindingDisposition(first) is
            HeartRatePersistenceBindingDisposition.MatchingInstalled)
        val conflict = owner.persistenceBindingDisposition(second)
        assertEquals(
            HeartRatePersistenceBindingDisposition.ConflictingInstalled(second, first),
            conflict
        )
        assertEquals(
            HeartRatePersistenceUnbindResult.ConflictingInstalled(second, first),
            owner.exactUnbindPersistenceSink(second)
        )
        assertTrue(owner.persistenceBindingDisposition(first) is
            HeartRatePersistenceBindingDisposition.MatchingInstalled)
        assertEquals(
            HeartRatePersistenceUnbindResult.Unbound(first),
            owner.exactUnbindPersistenceSink(first)
        )
        assertEquals(
            HeartRatePersistenceBindingDisposition.KnownAbsent(first),
            owner.persistenceBindingDisposition(first)
        )
    }

    @Test
    fun aFreshOwnerStartsAtReceiptZeroAndCannotReplayTheDeadOwnersReceipts() {
        val old = mutableListOf<HeartRateRuntimeObservation>()
        owner.bindPersistenceSink(bindingId("old"), old::add)
        owner.submit(HeartRateRuntimeAction.Enable)
        idleMain()
        assertEquals(listOf(1L), old.map { it.receipt })
        owner.close()
        idleMain()

        val freshOwner = HeartRateRuntimeOwner(application, Handler(Looper.getMainLooper()))
        val fresh = mutableListOf<HeartRateRuntimeObservation>()
        val result = freshOwner.bindPersistenceSink(bindingId("fresh"), fresh::add)
            as HeartRatePersistenceBindResult.Installed

        assertEquals(0L, result.binding.snapshot.receipt)
        assertTrue(fresh.isEmpty())
    }

    @Test
    fun aClosedOwnerReportsUnresolvedInsteadOfInventingBindingCertainty() {
        owner.close()
        idleMain()

        assertEquals(
            HeartRatePersistenceBindingDisposition.Unresolved(bindingId("closed")),
            owner.persistenceBindingDisposition(bindingId("closed"))
        )
    }

    private fun connect(address: String): ConnectedGatt {
        val adapter = application.getSystemService(BluetoothManager::class.java).adapter
        @Suppress("DEPRECATION")
        val device = ShadowBluetoothDevice.newInstance(address)
        Shadow.extract<ShadowBluetoothDevice>(device).setName("HRS $address")
        owner.submit(HeartRateRuntimeAction.Enable)
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val callback = shadowOf(adapter.bluetoothLeScanner).scanCallbacks.single()
        @Suppress("DEPRECATION")
        callback.onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            ScanResult(device, null, -45, 1L)
        )
        idleMain()

        lateinit var connected: ConnectedGatt
        Shadow.extract<ShadowBluetoothDevice>(device).setGattConnectionInterceptor { gatt ->
            val shadowGatt = Shadow.extract<ShadowBluetoothGatt>(gatt)
            val service = BluetoothGattService(HRS_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val characteristic = BluetoothGattCharacteristic(
                MEASUREMENT_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            val descriptor = BluetoothGattDescriptor(
                CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_WRITE
            )
            characteristic.addDescriptor(descriptor)
            service.addCharacteristic(characteristic)
            shadowGatt.addDiscoverableService(service)
            shadowGatt.allowCharacteristicNotification(characteristic)
            val gattCallback = shadowGatt.gattCallback
            gattCallback.onConnectionStateChange(
                gatt,
                BluetoothGatt.GATT_SUCCESS,
                BluetoothProfile.STATE_CONNECTED
            )
            connected = ConnectedGatt(gatt, gattCallback, characteristic, descriptor)
        }
        owner.submit(HeartRateRuntimeAction.Connect(address))
        idleMain()
        connected.callback.onDescriptorWrite(
            connected.gatt,
            connected.descriptor,
            BluetoothGatt.GATT_SUCCESS
        )
        idleMain()
        return connected
    }

    private fun idleMain() = shadowOf(Looper.getMainLooper()).idle()

    private fun eligibleInput(address: String) = HeartRateRecoveryEligibilityInput(
        optedIn = true,
        savedTargetIdentifier = address,
        permissionGranted = true,
        bluetoothEnabled = true,
        manuallySuppressed = false,
        appVisible = true,
        activeTrainingFgsActive = false
    )

    private fun List<HeartRateRuntimeObservation>.causes() = mapNotNull { observation ->
        when (val payload = observation.payload) {
            is HeartRateRuntimeObservationPayload.CurrentSnapshot -> payload.cause
            is HeartRateRuntimeObservationPayload.RuntimeTransition -> payload.cause
            is HeartRateRuntimeObservationPayload.ValidMeasurement -> null
        }
    }

    private fun bindingId(value: String) = HeartRatePersistenceBindingId("binding:$value")

    private data class ConnectedGatt(
        val gatt: BluetoothGatt,
        val callback: BluetoothGattCallback,
        val characteristic: BluetoothGattCharacteristic,
        val descriptor: BluetoothGattDescriptor
    )

    private companion object {
        val HRS_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
