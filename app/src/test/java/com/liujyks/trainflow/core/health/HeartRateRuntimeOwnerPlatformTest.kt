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
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Looper
import android.os.ParcelUuid
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.model.HeartRateFact
import com.liujyks.trainflow.core.model.HeartRateTechnicalFailure
import java.time.Duration
import java.util.UUID
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothGatt
import org.robolectric.shadows.ShadowBluetoothLeScanner

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35],
    shadows = [E17GattShadow::class, E17ScannerShadow::class]
)
@LooperMode(LooperMode.Mode.PAUSED)
class HeartRateRuntimeOwnerPlatformTest {
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
        owner = HeartRateRuntimeOwner(
            application,
            mainHandler = android.os.Handler(Looper.getMainLooper())
        )
        E17GattShadow.resetFailures()
        E17ScannerShadow.resetFailures()
        enableOwner()
    }

    @Test
    fun productionScanUsesStandardHeartRateServiceFilter() {
        val scanner = application.getSystemService(BluetoothManager::class.java)
            .adapter.bluetoothLeScanner

        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()

        val filters = shadowOf(scanner).activeScans.single().scanFilters()
        assertEquals(
            ParcelUuid.fromString("0000180d-0000-1000-8000-00805f9b34fb"),
            filters.single().serviceUuid
        )
    }

    @Test
    fun api33NotifyPathWritesCccdWaitsAndConsumesBothCallbackOverloadsOnce() {
        val connected = connect(
            address = "AA:BB:CC:DD:EE:21",
            properties = BluetoothGattCharacteristic.PROPERTY_NOTIFY
        )

        assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)
        assertArrayEquals(
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
            connected.shadowGatt.latestWrittenBytes
        )
        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 88)
        )
        val freshnessAfterApi33 = privateRunnable("freshnessRunnable")
        @Suppress("DEPRECATION")
        connected.characteristic.value = byteArrayOf(0x00, 88)
        @Suppress("DEPRECATION")
        connected.callback.onCharacteristicChanged(connected.gatt, connected.characteristic)

        assertEquals(HeartRateFact.LIVE, owner.heartRateState.value.fact)
        assertEquals(88, owner.heartRateState.value.bpm)
        assertTrue(freshnessAfterApi33 === privateRunnable("freshnessRunnable"))
    }

    @Test
    fun successfulDuplicateAndLateCallbacksRemainPhaseGatedWithoutTimelineRegression() {
        val connected = connect("AA:BB:CC:DD:EE:38")
        val discoverCalls = E17GattShadow.discoverCalls
        val notificationCalls = E17GattShadow.notificationCalls
        val descriptorWriteCalls = E17GattShadow.descriptorWriteCalls
        val waiting = owner.heartRateState.value
        val waitingFreshness = privateRunnable("freshnessRunnable")

        connected.callback.onConnectionStateChange(
            connected.gatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )
        connected.callback.onServicesDiscovered(connected.gatt, BluetoothGatt.GATT_SUCCESS)

        assertEquals(waiting, owner.heartRateState.value)
        assertTrue(waitingFreshness === privateRunnable("freshnessRunnable"))
        assertEquals(discoverCalls, E17GattShadow.discoverCalls)
        assertEquals(notificationCalls, E17GattShadow.notificationCalls)
        assertEquals(descriptorWriteCalls, E17GattShadow.descriptorWriteCalls)

        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 94)
        )
        val live = owner.heartRateState.value
        val freshness = privateRunnable("freshnessRunnable")

        connected.callback.onDescriptorWrite(
            connected.gatt,
            connected.descriptor,
            BluetoothGatt.GATT_SUCCESS
        )
        connected.callback.onServicesDiscovered(connected.gatt, BluetoothGatt.GATT_SUCCESS)
        connected.callback.onConnectionStateChange(
            connected.gatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        assertEquals(live, owner.heartRateState.value)
        assertTrue(freshness === privateRunnable("freshnessRunnable"))
        assertEquals(discoverCalls, E17GattShadow.discoverCalls)
        assertEquals(notificationCalls, E17GattShadow.notificationCalls)
        assertEquals(descriptorWriteCalls, E17GattShadow.descriptorWriteCalls)
        assertFalse(connected.shadowGatt.isClosed)
    }

    @Test
    fun notifyBeforeDescriptorCompletionIsIgnoredOnProductionCallbackPath() {
        val device = scanDevice("AA:BB:CC:DD:EE:39", "Early notify")
        Shadow.extract<ShadowBluetoothDevice>(device).setGattConnectionInterceptor { gatt ->
            val shadowGatt = Shadow.extract<ShadowBluetoothGatt>(gatt)
            val characteristic = addHrs(
                shadowGatt,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                allowNotification = true
            )
            val callback = shadowGatt.gattCallback
            E17GattShadow.beforeDescriptorWrite = {
                callback.onCharacteristicChanged(
                    gatt,
                    characteristic,
                    byteArrayOf(0x00, 103)
                )
            }
            callback.onConnectionStateChange(
                gatt,
                BluetoothGatt.GATT_SUCCESS,
                BluetoothProfile.STATE_CONNECTED
            )
        }

        owner.submit(HeartRateRuntimeAction.Connect(device.address))
        idleMain()

        assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)
        assertNull(owner.heartRateState.value.bpm)
        assertNull(owner.heartRateState.value.measuredAt)
    }

    @Test
    fun indicateOnlyCharacteristicUsesIndicationCccdValue() {
        val connected = connect(
            address = "AA:BB:CC:DD:EE:22",
            properties = BluetoothGattCharacteristic.PROPERTY_INDICATE
        )

        assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)
        assertArrayEquals(
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE,
            connected.shadowGatt.latestWrittenBytes
        )
    }

    @Test
    @Config(sdk = [32])
    fun legacyDescriptorWriteUsesLegacyValueAndReachesWaiting() {
        val connected = connect(
            address = "AA:BB:CC:DD:EE:23",
            properties = BluetoothGattCharacteristic.PROPERTY_NOTIFY
        )

        assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)
        @Suppress("DEPRECATION")
        assertArrayEquals(
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
            connected.descriptor.value
        )
        assertArrayEquals(
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
            connected.shadowGatt.latestWrittenBytes
        )
    }

    @Test
    fun malformedPayloadDoesNotRefreshLiveAndFreshnessClearsAtExactDeadline() {
        val connected = connect("AA:BB:CC:DD:EE:24")
        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 90)
        )
        val live = owner.heartRateState.value
        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x01, 0x20)
        )

        assertEquals(live, owner.heartRateState.value)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2_499))
        assertEquals(HeartRateFact.LIVE, owner.heartRateState.value.fact)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))

        assertEquals(HeartRateFact.DATA_INTERRUPTED, owner.heartRateState.value.fact)
        assertNull(owner.heartRateState.value.bpm)
        assertNull(owner.heartRateState.value.measuredAt)
    }

    @Test
    fun waitingFirstDataInterruptsAtExactBoundaryWithoutTechnicalFailure() {
        connect("AA:BB:CC:DD:EE:35")

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2_999))
        assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))

        assertEquals(HeartRateFact.DATA_INTERRUPTED, owner.heartRateState.value.fact)
        assertNull(owner.heartRateState.value.technicalFailure)
    }

    @Test
    fun gattCallbackFromBackgroundThreadWaitsForMainQueue() {
        val connected = connect("AA:BB:CC:DD:EE:36")
        val worker = Thread {
            connected.callback.onCharacteristicChanged(
                connected.gatt,
                connected.characteristic,
                byteArrayOf(0x00, 92)
            )
        }

        worker.start()
        worker.join()
        assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)

        idleMain()
        assertEquals(HeartRateFact.LIVE, owner.heartRateState.value.fact)
        assertEquals(92, owner.heartRateState.value.bpm)
    }

    @Test
    fun serviceCharacteristicCccdAndNotificationFailuresAreTypedAndCleaned() {
        val missingService = connectWithConfiguration("AA:BB:CC:DD:EE:25") { _ -> }
        assertFailure(
            HeartRateTechnicalFailure.SERVICE_DISCOVERY_FAILED,
            missingService
        )

        val missingCharacteristic = connectWithConfiguration("AA:BB:CC:DD:EE:26") { shadowGatt ->
            shadowGatt.addDiscoverableService(
                BluetoothGattService(HRS_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            )
        }
        assertFailure(
            HeartRateTechnicalFailure.SERVICE_DISCOVERY_FAILED,
            missingCharacteristic
        )

        val missingCccd = connectWithConfiguration("AA:BB:CC:DD:EE:27") { shadowGatt ->
            val service = BluetoothGattService(HRS_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            service.addCharacteristic(
                BluetoothGattCharacteristic(
                    MEASUREMENT_UUID,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ
                )
            )
            shadowGatt.addDiscoverableService(service)
        }
        assertFailure(HeartRateTechnicalFailure.CCCD_FAILED, missingCccd)

        val notificationRejected = connectWithConfiguration("AA:BB:CC:DD:EE:28") { shadowGatt ->
            addHrs(shadowGatt, BluetoothGattCharacteristic.PROPERTY_NOTIFY, allowNotification = false)
        }
        assertFailure(HeartRateTechnicalFailure.CCCD_FAILED, notificationRejected)

        owner = HeartRateRuntimeOwner(application)
        val device = scanDevice("AA:BB:CC:DD:EE:37", "Descriptor failure")
        lateinit var descriptorStatusFailure: ConnectedGatt
        Shadow.extract<ShadowBluetoothDevice>(device).setGattConnectionInterceptor { gatt ->
            val shadowGatt = Shadow.extract<ShadowBluetoothGatt>(gatt)
            val characteristic = addHrs(
                shadowGatt,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                allowNotification = true
            )
            val callback = shadowGatt.gattCallback
            val descriptor = requireNotNull(characteristic.getDescriptor(CCCD_UUID))
            E17GattShadow.beforeDescriptorWrite = {
                callback.onDescriptorWrite(gatt, descriptor, BluetoothGatt.GATT_FAILURE)
            }
            callback.onConnectionStateChange(
                gatt,
                BluetoothGatt.GATT_SUCCESS,
                BluetoothProfile.STATE_CONNECTED
            )
            descriptorStatusFailure = ConnectedGatt(
                device,
                gatt,
                shadowGatt,
                callback,
                characteristic,
                descriptor
            )
        }
        owner.submit(HeartRateRuntimeAction.Connect(device.address))
        idleMain()
        assertFailure(HeartRateTechnicalFailure.CCCD_FAILED, descriptorStatusFailure)
    }

    @Test
    fun activeExplicitDisconnectPublishesLinkDisconnectedButIntentionalStopDoesNot() {
        val connected = connect("AA:BB:CC:DD:EE:29")

        connected.callback.onConnectionStateChange(
            connected.gatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_DISCONNECTED
        )

        assertEquals(HeartRateFact.LINK_DISCONNECTED, owner.heartRateState.value.fact)
        assertTrue(connected.shadowGatt.isClosed)

        owner.submit(HeartRateRuntimeAction.Stop)
        idleMain()
        val stopped = owner.heartRateState.value
        connected.callback.onConnectionStateChange(
            connected.gatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_DISCONNECTED
        )
        assertEquals(stopped, owner.heartRateState.value)
        assertFalse(owner.heartRateState.value.fact == HeartRateFact.LINK_DISCONNECTED)
    }

    @Test
    fun status19ConnectedWhileWaitingIsConnectFailureAndLateCallbacksCannotRestoreAttempt() {
        val connected = connect("AA:BB:CC:DD:EE:41")
        val staleFreshness = privateRunnable("freshnessRunnable")
        val disconnectCalls = E17GattShadow.disconnectCalls
        val closeCalls = E17GattShadow.closeCalls
        val discoverCalls = E17GattShadow.discoverCalls
        val notificationCalls = E17GattShadow.notificationCalls
        val descriptorWriteCalls = E17GattShadow.descriptorWriteCalls

        connected.callback.onConnectionStateChange(
            connected.gatt,
            19,
            BluetoothProfile.STATE_CONNECTED
        )

        assertEquals(HeartRateFact.TECHNICAL_FAILURE, owner.heartRateState.value.fact)
        assertEquals(
            HeartRateTechnicalFailure.CONNECT_FAILED,
            owner.heartRateState.value.technicalFailure
        )
        assertNull(owner.heartRateState.value.bpm)
        assertNull(owner.heartRateState.value.measuredAt)
        assertTrue(connected.shadowGatt.isClosed)
        assertTrue(E17GattShadow.disconnectCalls > disconnectCalls)
        assertTrue(E17GattShadow.closeCalls > closeCalls)
        val failure = owner.heartRateState.value

        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 111)
        )
        connected.callback.onServicesDiscovered(connected.gatt, BluetoothGatt.GATT_SUCCESS)
        connected.callback.onDescriptorWrite(
            connected.gatt,
            connected.descriptor,
            BluetoothGatt.GATT_SUCCESS
        )
        connected.callback.onConnectionStateChange(
            connected.gatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )
        android.os.Handler(Looper.getMainLooper()).post(staleFreshness)
        idleMain()

        assertEquals(failure, owner.heartRateState.value)
        assertEquals(discoverCalls, E17GattShadow.discoverCalls)
        assertEquals(notificationCalls, E17GattShadow.notificationCalls)
        assertEquals(descriptorWriteCalls, E17GattShadow.descriptorWriteCalls)
        assertTrue(connected.shadowGatt.isClosed)
    }

    @Test
    fun status19ConnectedWhileLiveClearsReadingAndLateCallbacksCannotRestoreLive() {
        val connected = connect("AA:BB:CC:DD:EE:42")
        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 112)
        )
        assertEquals(HeartRateFact.LIVE, owner.heartRateState.value.fact)
        val staleFreshness = privateRunnable("freshnessRunnable")
        val disconnectCalls = E17GattShadow.disconnectCalls
        val closeCalls = E17GattShadow.closeCalls
        val discoverCalls = E17GattShadow.discoverCalls
        val notificationCalls = E17GattShadow.notificationCalls
        val descriptorWriteCalls = E17GattShadow.descriptorWriteCalls

        connected.callback.onConnectionStateChange(
            connected.gatt,
            19,
            BluetoothProfile.STATE_CONNECTED
        )

        assertEquals(HeartRateFact.TECHNICAL_FAILURE, owner.heartRateState.value.fact)
        assertEquals(
            HeartRateTechnicalFailure.CONNECT_FAILED,
            owner.heartRateState.value.technicalFailure
        )
        assertNull(owner.heartRateState.value.bpm)
        assertNull(owner.heartRateState.value.measuredAt)
        assertTrue(connected.shadowGatt.isClosed)
        assertTrue(E17GattShadow.disconnectCalls > disconnectCalls)
        assertTrue(E17GattShadow.closeCalls > closeCalls)
        val failure = owner.heartRateState.value

        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 113)
        )
        connected.callback.onServicesDiscovered(connected.gatt, BluetoothGatt.GATT_SUCCESS)
        connected.callback.onDescriptorWrite(
            connected.gatt,
            connected.descriptor,
            BluetoothGatt.GATT_SUCCESS
        )
        connected.callback.onConnectionStateChange(
            connected.gatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )
        android.os.Handler(Looper.getMainLooper()).post(staleFreshness)
        idleMain()

        assertEquals(failure, owner.heartRateState.value)
        assertEquals(discoverCalls, E17GattShadow.discoverCalls)
        assertEquals(notificationCalls, E17GattShadow.notificationCalls)
        assertEquals(descriptorWriteCalls, E17GattShadow.descriptorWriteCalls)
        assertTrue(connected.shadowGatt.isClosed)
    }

    @Test
    fun status19DisconnectWhileWaitingIsLinkDisconnectedAndClearsCurrentReading() {
        val connected = connect("AA:BB:CC:DD:EE:3A")

        connected.callback.onConnectionStateChange(
            connected.gatt,
            19,
            BluetoothProfile.STATE_DISCONNECTED
        )

        assertEquals(HeartRateFact.LINK_DISCONNECTED, owner.heartRateState.value.fact)
        assertNull(owner.heartRateState.value.bpm)
        assertNull(owner.heartRateState.value.measuredAt)
    }

    @Test
    fun status19DisconnectWhileLiveIsLinkDisconnectedAndClearsCurrentReading() {
        val connected = connect("AA:BB:CC:DD:EE:3B")
        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 96)
        )
        assertEquals(HeartRateFact.LIVE, owner.heartRateState.value.fact)

        connected.callback.onConnectionStateChange(
            connected.gatt,
            19,
            BluetoothProfile.STATE_DISCONNECTED
        )

        assertEquals(HeartRateFact.LINK_DISCONNECTED, owner.heartRateState.value.fact)
        assertNull(owner.heartRateState.value.bpm)
        assertNull(owner.heartRateState.value.measuredAt)
    }

    @Test
    fun activeLiveStaleTargetDoesNotReplaceStateTimelineOrGatt() {
        val connected = connect("AA:BB:CC:DD:EE:3C")
        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 98)
        )
        val live = owner.heartRateState.value
        val freshness = privateRunnable("freshnessRunnable")

        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        owner.submit(HeartRateRuntimeAction.Connect(connected.device.address))
        idleMain()

        assertEquals(live, owner.heartRateState.value)
        assertTrue(freshness === privateRunnable("freshnessRunnable"))
        assertFalse(connected.shadowGatt.isClosed)
        assertEquals(1, Shadow.extract<ShadowBluetoothDevice>(connected.device).bluetoothGatts.size)
    }

    @Test
    fun activeLiveInvalidIdentifierDoesNotReplaceStateTimelineOrGatt() {
        val connected = connect("AA:BB:CC:DD:EE:3D")
        connected.callback.onCharacteristicChanged(
            connected.gatt,
            connected.characteristic,
            byteArrayOf(0x00, 99)
        )
        val live = owner.heartRateState.value
        val freshness = privateRunnable("freshnessRunnable")

        owner.submit(HeartRateRuntimeAction.Connect("not-a-current-candidate"))
        idleMain()

        assertEquals(live, owner.heartRateState.value)
        assertTrue(freshness === privateRunnable("freshnessRunnable"))
        assertFalse(connected.shadowGatt.isClosed)
        assertEquals(1, Shadow.extract<ShadowBluetoothDevice>(connected.device).bluetoothGatts.size)
    }

    @Test
    fun activeLiveValidNewTargetClosesOldAttemptBeforeStartingNewConnect() {
        val old = connect("AA:BB:CC:DD:EE:3E")
        old.callback.onCharacteristicChanged(
            old.gatt,
            old.characteristic,
            byteArrayOf(0x00, 100)
        )
        val replacement = scanDevice("AA:BB:CC:DD:EE:3F", "Replacement")
        lateinit var replacementGatt: BluetoothGatt
        Shadow.extract<ShadowBluetoothDevice>(replacement).setGattConnectionInterceptor { gatt ->
            assertTrue(old.shadowGatt.isClosed)
            replacementGatt = gatt
            val shadowGatt = Shadow.extract<ShadowBluetoothGatt>(gatt)
            addHrs(
                shadowGatt,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                allowNotification = true
            )
            shadowGatt.gattCallback.onConnectionStateChange(
                gatt,
                BluetoothGatt.GATT_SUCCESS,
                BluetoothProfile.STATE_CONNECTED
            )
        }

        owner.submit(HeartRateRuntimeAction.Connect(replacement.address))
        idleMain()

        assertTrue(old.shadowGatt.isClosed)
        assertFalse(Shadow.extract<ShadowBluetoothGatt>(replacementGatt).isClosed)
        assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)
        assertNull(owner.heartRateState.value.bpm)
        assertNull(owner.heartRateState.value.measuredAt)
    }

    @Test
    fun invalidTargetWithoutActiveAttemptKeepsTypedConnectFailureSemantics() {
        owner.submit(HeartRateRuntimeAction.Connect("missing-target"))
        idleMain()

        assertEquals(HeartRateFact.TECHNICAL_FAILURE, owner.heartRateState.value.fact)
        assertEquals(
            HeartRateTechnicalFailure.CONNECT_FAILED,
            owner.heartRateState.value.technicalFailure
        )
        assertNull(owner.heartRateState.value.bpm)
        assertNull(owner.heartRateState.value.measuredAt)
    }

    @Test
    fun disableEnableThenExplicitScanAndConnectRestartsTheSameOwner() {
        val first = connect("AA:BB:CC:DD:EE:40")
        first.callback.onCharacteristicChanged(
            first.gatt,
            first.characteristic,
            byteArrayOf(0x00, 102)
        )
        assertEquals(HeartRateFact.LIVE, owner.heartRateState.value.fact)

        owner.submit(HeartRateRuntimeAction.Disable)
        idleMain()
        assertEquals(HeartRateFact.DISABLED, owner.heartRateState.value.fact)
        assertNull(owner.heartRateState.value.bpm)
        assertNull(owner.heartRateState.value.measuredAt)
        assertTrue(first.shadowGatt.isClosed)

        owner.submit(HeartRateRuntimeAction.Enable)
        idleMain()
        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)
        assertEquals(1, Shadow.extract<ShadowBluetoothDevice>(first.device).bluetoothGatts.size)

        val restarted = connect("AA:BB:CC:DD:EE:41")
        assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)
        assertFalse(restarted.shadowGatt.isClosed)
    }

    @Test
    fun userDisconnectIsReversibleAndDoesNotPermanentlyCloseOwner() {
        val connected = connect("AA:BB:CC:DD:EE:42")

        owner.submit(HeartRateRuntimeAction.Disconnect)
        idleMain()
        assertEquals(HeartRateFact.INTENTIONAL_STOP, owner.heartRateState.value.fact)
        assertTrue(connected.shadowGatt.isClosed)

        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        assertEquals(HeartRateFact.SCANNING, owner.heartRateState.value.fact)
    }

    @Test
    fun payloadFromNonMatchingRawGattCannotPublishLive() {
        val connected = connect("AA:BB:CC:DD:EE:30")
        val wrongGatt = ShadowBluetoothGatt.newInstance(connected.device)

        connected.callback.onCharacteristicChanged(
            wrongGatt,
            connected.characteristic,
            byteArrayOf(0x00, 111)
        )

        assertEquals(HeartRateFact.WAITING_FIRST_DATA, owner.heartRateState.value.fact)
        assertTrue(Shadow.extract<ShadowBluetoothGatt>(wrongGatt).isClosed)
    }

    @Test
    fun scanStartAndStopSecurityExceptionsPublishPermissionAndClearGeneration() {
        E17ScannerShadow.throwStartSecurity = true
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()

        assertEquals(HeartRateFact.PERMISSION_REQUIRED, owner.heartRateState.value.fact)
        assertTrue(
            shadowOf(
                application.getSystemService(BluetoothManager::class.java)
                    .adapter.bluetoothLeScanner
            ).scanCallbacks.isEmpty()
        )

        E17ScannerShadow.throwStartSecurity = false
        owner = HeartRateRuntimeOwner(application)
        enableOwner()
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val callback = shadowOf(
            application.getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
        ).scanCallbacks.single()
        E17ScannerShadow.throwStopSecurity = true
        owner.submit(HeartRateRuntimeAction.Stop)
        idleMain()
        val terminal = owner.heartRateState.value
        callback.onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        idleMain()

        assertEquals(HeartRateFact.PERMISSION_REQUIRED, terminal.fact)
        assertEquals(terminal, owner.heartRateState.value)
    }

    @Test
    fun adapterOffCleanupToleratesPlatformStopScanIllegalState() {
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        E17ScannerShadow.throwStopIllegalState = true

        owner.submit(HeartRateRuntimeAction.BluetoothOff)
        idleMain()

        assertEquals(HeartRateFact.BLUETOOTH_OFF, owner.heartRateState.value.fact)
    }

    @Test
    fun unknownStopScanIllegalStateRemainsObservableOutsideAdapterOffCleanup() {
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        E17ScannerShadow.throwStopIllegalState = true

        owner.submit(HeartRateRuntimeAction.Stop)

        assertThrows(IllegalStateException::class.java) { idleMain() }
    }

    @Test
    fun connectPermissionToctouUsesRealConnectGattCallAndDoesNotAutoRetry() {
        val snapshotContext = PermissionSnapshotContext(application)
        owner = HeartRateRuntimeOwner(snapshotContext)
        val device = scanDevice("AA:BB:CC:DD:EE:31", "TOCTOU connect")
        val shadowDevice = Shadow.extract<ShadowBluetoothDevice>(device)
        shadowDevice.setShouldThrowSecurityExceptions(true)
        shadowOf(application).denyPermissions(Manifest.permission.BLUETOOTH_CONNECT)

        owner.submit(HeartRateRuntimeAction.Connect(device.address))
        idleMain()

        assertEquals(HeartRateFact.PERMISSION_REQUIRED, owner.heartRateState.value.fact)
        assertTrue(shadowDevice.bluetoothGatts.isEmpty())

        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT)
        idleMain()
        assertEquals(HeartRateFact.PERMISSION_REQUIRED, owner.heartRateState.value.fact)
        assertTrue(shadowDevice.bluetoothGatts.isEmpty())
    }

    @Test
    fun permissionGatedDeviceNameReadHandlesRevocationAtScanCallback() {
        owner = HeartRateRuntimeOwner(PermissionSnapshotContext(application))
        enableOwner()
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val adapter = application.getSystemService(BluetoothManager::class.java).adapter
        val callback = shadowOf(adapter.bluetoothLeScanner).scanCallbacks.single()
        val device = ShadowBluetoothDevice.newInstance("AA:BB:CC:DD:EE:32")
        val shadowDevice = Shadow.extract<ShadowBluetoothDevice>(device)
        shadowDevice.setName("Revoked name")
        shadowDevice.setShouldThrowSecurityExceptions(true)
        shadowOf(application).denyPermissions(Manifest.permission.BLUETOOTH_CONNECT)

        @Suppress("DEPRECATION")
        callback.onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            ScanResult(device, null, -40, 2L)
        )
        idleMain()

        assertEquals(HeartRateFact.PERMISSION_REQUIRED, owner.heartRateState.value.fact)
        assertTrue(owner.candidates.value.isEmpty())
    }

    @Test
    fun discoveryNotificationAndDescriptorSecurityExceptionsUseNarrowPermissionPath() {
        listOf(
            GattFailure.DISCOVER_SECURITY,
            GattFailure.NOTIFICATION_SECURITY,
            GattFailure.DESCRIPTOR_SECURITY
        ).forEachIndexed { index, failure ->
            E17GattShadow.resetFailures()
            E17GattShadow.failure = failure
            owner = HeartRateRuntimeOwner(application)
            val connected = connectWithConfiguration(
                "AA:BB:CC:DD:EF:${40 + index}"
            ) { shadowGatt ->
                addHrs(
                    shadowGatt,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    allowNotification = true
                )
            }

            assertEquals(HeartRateFact.PERMISSION_REQUIRED, owner.heartRateState.value.fact)
            assertTrue(connected.shadowGatt.isClosed)
        }
    }

    @Test
    fun disconnectAndCloseRevocationStillClearsReferencesAndIsIdempotent() {
        val connected = connect("AA:BB:CC:DD:EE:33")
        E17GattShadow.throwDisconnectSecurity = true
        E17GattShadow.throwCloseSecurity = true

        owner.submit(HeartRateRuntimeAction.Stop)
        idleMain()
        val disconnectCalls = E17GattShadow.disconnectCalls
        val closeCalls = E17GattShadow.closeCalls
        val terminal = owner.heartRateState.value

        assertEquals(HeartRateFact.PERMISSION_REQUIRED, terminal.fact)
        assertTrue(disconnectCalls >= 1)
        assertTrue(closeCalls >= 1)

        owner.submit(HeartRateRuntimeAction.Stop)
        idleMain()
        assertEquals(disconnectCalls, E17GattShadow.disconnectCalls)
        assertEquals(closeCalls, E17GattShadow.closeCalls)
        connected.callback.onConnectionStateChange(
            connected.gatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_DISCONNECTED
        )

        assertEquals(disconnectCalls, E17GattShadow.disconnectCalls)
        assertTrue(E17GattShadow.closeCalls >= closeCalls)
        assertEquals(terminal, owner.heartRateState.value)
    }

    @Test
    fun unknownIllegalStateExceptionIsNotClassifiedAsPermissionOrBluetoothFailure() {
        E17GattShadow.failure = GattFailure.DISCOVER_ILLEGAL_STATE
        val device = scanDevice("AA:BB:CC:DD:EE:34", "Unknown failure")
        Shadow.extract<ShadowBluetoothDevice>(device).setGattConnectionInterceptor { gatt ->
            val shadowGatt = Shadow.extract<ShadowBluetoothGatt>(gatt)
            addHrs(
                shadowGatt,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                allowNotification = true
            )
            shadowGatt.gattCallback.onConnectionStateChange(
                gatt,
                BluetoothGatt.GATT_SUCCESS,
                BluetoothProfile.STATE_CONNECTED
            )
        }
        owner.submit(HeartRateRuntimeAction.Connect(device.address))

        assertThrows(IllegalStateException::class.java) { idleMain() }
        assertFalse(owner.heartRateState.value.fact == HeartRateFact.PERMISSION_REQUIRED)
        assertFalse(owner.heartRateState.value.fact == HeartRateFact.BLUETOOTH_OFF)
    }

    private fun connect(
        address: String,
        properties: Int = BluetoothGattCharacteristic.PROPERTY_NOTIFY
    ): ConnectedGatt {
        return connectWithConfiguration(address) { shadowGatt ->
            addHrs(shadowGatt, properties, allowNotification = true)
        }
    }

    private fun connectWithConfiguration(
        address: String,
        configure: (ShadowBluetoothGatt) -> Unit
    ): ConnectedGatt {
        val device = scanDevice(address, "HRS $address")
        lateinit var connected: ConnectedGatt
        Shadow.extract<ShadowBluetoothDevice>(device).setGattConnectionInterceptor { gatt ->
            val shadowGatt = Shadow.extract<ShadowBluetoothGatt>(gatt)
            configure(shadowGatt)
            val callback = shadowGatt.gattCallback
            callback.onConnectionStateChange(
                gatt,
                BluetoothGatt.GATT_SUCCESS,
                BluetoothProfile.STATE_CONNECTED
            )
            val characteristic = shadowGatt.getServiceOrNull(HRS_UUID)
                ?.getCharacteristic(MEASUREMENT_UUID)
            connected = ConnectedGatt(
                device = device,
                gatt = gatt,
                shadowGatt = shadowGatt,
                callback = callback,
                characteristicOrNull = characteristic,
                descriptorOrNull = characteristic?.getDescriptor(CCCD_UUID)
            )
        }
        owner.submit(HeartRateRuntimeAction.Connect(address))
        idleMain()
        return connected
    }

    private fun scanDevice(address: String, name: String): BluetoothDevice {
        val adapter = application.getSystemService(BluetoothManager::class.java).adapter
        val device = ShadowBluetoothDevice.newInstance(address)
        Shadow.extract<ShadowBluetoothDevice>(device).setName(name)
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
        return device
    }

    private fun addHrs(
        shadowGatt: ShadowBluetoothGatt,
        properties: Int,
        allowNotification: Boolean
    ): BluetoothGattCharacteristic {
        val service = BluetoothGattService(HRS_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            MEASUREMENT_UUID,
            properties,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        characteristic.addDescriptor(
            BluetoothGattDescriptor(CCCD_UUID, BluetoothGattDescriptor.PERMISSION_WRITE)
        )
        service.addCharacteristic(characteristic)
        shadowGatt.addDiscoverableService(service)
        if (allowNotification) shadowGatt.allowCharacteristicNotification(characteristic)
        return characteristic
    }

    private fun assertFailure(
        expected: HeartRateTechnicalFailure,
        connected: ConnectedGatt
    ) {
        assertEquals(HeartRateFact.TECHNICAL_FAILURE, owner.heartRateState.value.fact)
        assertEquals(expected, owner.heartRateState.value.technicalFailure)
        assertTrue(connected.shadowGatt.isClosed)
    }

    private fun ShadowBluetoothGatt.getServiceOrNull(uuid: UUID): BluetoothGattService? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val field = ShadowBluetoothGatt::class.java.getDeclaredField("discoverableServices")
            field.isAccessible = true
            (field.get(this) as Set<BluetoothGattService>).firstOrNull { it.uuid == uuid }
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    private fun idleMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun enableOwner() {
        owner.submit(HeartRateRuntimeAction.Enable)
        idleMain()
        assertEquals(HeartRateFact.NOT_CONNECTED, owner.heartRateState.value.fact)
    }

    private fun privateRunnable(fieldName: String): Runnable {
        val field = HeartRateRuntimeOwner::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return requireNotNull(field.get(owner) as Runnable?)
    }

    private class PermissionSnapshotContext(base: Context) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this

        override fun checkSelfPermission(permission: String): Int {
            return PackageManager.PERMISSION_GRANTED
        }
    }

    private data class ConnectedGatt(
        val device: BluetoothDevice,
        val gatt: BluetoothGatt,
        val shadowGatt: ShadowBluetoothGatt,
        val callback: BluetoothGattCallback,
        val characteristicOrNull: BluetoothGattCharacteristic?,
        val descriptorOrNull: BluetoothGattDescriptor?
    ) {
        val characteristic: BluetoothGattCharacteristic
            get() = requireNotNull(characteristicOrNull)
        val descriptor: BluetoothGattDescriptor
            get() = requireNotNull(descriptorOrNull)
    }

    private companion object {
        val HRS_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}

enum class GattFailure {
    NONE,
    DISCOVER_SECURITY,
    NOTIFICATION_SECURITY,
    DESCRIPTOR_SECURITY,
    DISCOVER_ILLEGAL_STATE
}

@Implements(BluetoothGatt::class)
class E17GattShadow : ShadowBluetoothGatt() {
    @Implementation
    override fun discoverServices(): Boolean {
        discoverCalls += 1
        when (failure) {
            GattFailure.DISCOVER_SECURITY -> throw SecurityException("test discovery revocation")
            GattFailure.DISCOVER_ILLEGAL_STATE -> throw IllegalStateException("unknown test defect")
            else -> Unit
        }
        return super.discoverServices()
    }

    @Implementation
    override fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enable: Boolean
    ): Boolean {
        notificationCalls += 1
        if (failure == GattFailure.NOTIFICATION_SECURITY) {
            throw SecurityException("test notification revocation")
        }
        return super.setCharacteristicNotification(characteristic, enable)
    }

    @Implementation(minSdk = 33)
    override fun writeDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray): Int {
        descriptorWriteCalls += 1
        if (failure == GattFailure.DESCRIPTOR_SECURITY) {
            throw SecurityException("test descriptor revocation")
        }
        val hook = beforeDescriptorWrite
        beforeDescriptorWrite = null
        hook?.invoke(descriptor)
        return super.writeDescriptor(descriptor, value)
    }

    @Implementation(maxSdk = 32)
    override fun writeDescriptor(descriptor: BluetoothGattDescriptor): Boolean {
        descriptorWriteCalls += 1
        if (failure == GattFailure.DESCRIPTOR_SECURITY) {
            throw SecurityException("test descriptor revocation")
        }
        val hook = beforeDescriptorWrite
        beforeDescriptorWrite = null
        hook?.invoke(descriptor)
        return super.writeDescriptor(descriptor)
    }

    @Implementation
    override fun disconnect() {
        disconnectCalls += 1
        if (throwDisconnectSecurity) throw SecurityException("test disconnect revocation")
        super.disconnect()
    }

    @Implementation
    override fun close() {
        closeCalls += 1
        if (throwCloseSecurity) throw SecurityException("test close revocation")
        super.close()
    }

    companion object {
        var failure: GattFailure = GattFailure.NONE
        var throwDisconnectSecurity: Boolean = false
        var throwCloseSecurity: Boolean = false
        var disconnectCalls: Int = 0
        var closeCalls: Int = 0
        var discoverCalls: Int = 0
        var notificationCalls: Int = 0
        var descriptorWriteCalls: Int = 0
        var beforeDescriptorWrite: ((BluetoothGattDescriptor) -> Unit)? = null

        fun resetFailures() {
            failure = GattFailure.NONE
            throwDisconnectSecurity = false
            throwCloseSecurity = false
            disconnectCalls = 0
            closeCalls = 0
            discoverCalls = 0
            notificationCalls = 0
            descriptorWriteCalls = 0
            beforeDescriptorWrite = null
        }
    }
}

@Implements(BluetoothLeScanner::class)
class E17ScannerShadow : ShadowBluetoothLeScanner() {
    @Implementation
    override fun startScan(
        filters: List<ScanFilter>,
        settings: ScanSettings,
        callback: ScanCallback
    ) {
        if (throwStartSecurity) throw SecurityException("test scan start revocation")
        super.startScan(filters, settings, callback)
    }

    @Implementation
    override fun stopScan(callback: ScanCallback) {
        if (throwStopSecurity) throw SecurityException("test scan stop revocation")
        if (throwStopIllegalState) throw IllegalStateException("test unknown scanner state")
        super.stopScan(callback)
    }

    companion object {
        var throwStartSecurity: Boolean = false
        var throwStopSecurity: Boolean = false
        var throwStopIllegalState: Boolean = false

        fun resetFailures() {
            throwStartSecurity = false
            throwStopSecurity = false
            throwStopIllegalState = false
        }
    }
}
