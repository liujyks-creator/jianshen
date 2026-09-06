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
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothGatt
import org.robolectric.shadows.ShadowBluetoothLeScanner

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35],
    shadows = [E17GattShadow::class, E17ScannerShadow::class, ObservationDeviceShadow::class]
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
        ObservationDeviceShadow.failure = ObservationDeviceFailure.NONE
        enableOwner()
    }

    @Test
    @Suppress("DEPRECATION")
    fun observationProtectedCallsEachPreservePermissionRevocation() {
        val calls = listOf("scan_start", "scan_stop", "address", "name", "connect", "discover",
            "service", "characteristic", "descriptor", "notify", "write", "disconnect", "close")
        calls.forEachIndexed { index, call ->
            E17GattShadow.resetFailures()
            E17ScannerShadow.resetFailures()
            ObservationDeviceShadow.failure = ObservationDeviceFailure.NONE
            owner = HeartRateRuntimeOwner(application)
            val ledger = mutableListOf<HeartRateObservation>()
            owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
            val address = "AA:BB:CC:DD:EF:${50 + index}"
            if (call == "scan_start") {
                E17ScannerShadow.throwStartSecurity = true
                owner.submit(HeartRateRuntimeAction.Enable)
                owner.submit(HeartRateRuntimeAction.StartScan)
                idleMain()
            } else if (call == "scan_stop") {
                scanDevice(address, "Permission fixture")
                E17ScannerShadow.throwStopSecurity = true
                owner.submit(HeartRateRuntimeAction.StopScan)
                idleMain()
            } else if (call == "address" || call == "name") {
                owner.submit(HeartRateRuntimeAction.Enable)
                owner.submit(HeartRateRuntimeAction.StartScan)
                idleMain()
                val device = ShadowBluetoothDevice.newInstance(address)
                ObservationDeviceShadow.failure = if (call == "address") ObservationDeviceFailure.ADDRESS else ObservationDeviceFailure.NAME
                val scanner = application.getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
                @Suppress("DEPRECATION")
                shadowOf(scanner).scanCallbacks.last().onScanResult(0, ScanResult(device, null, -40, 1L))
            } else if (call == "connect") {
                scanDevice(address, "Permission fixture")
                ObservationDeviceShadow.failure = ObservationDeviceFailure.CONNECT
                owner.submit(HeartRateRuntimeAction.Connect(address))
                idleMain()
            } else {
                E17GattShadow.failure = when (call) {
                    "discover" -> GattFailure.DISCOVER_SECURITY
                    "service" -> GattFailure.SERVICE_SECURITY
                    "notify" -> GattFailure.NOTIFICATION_SECURITY
                    "write" -> GattFailure.DESCRIPTOR_SECURITY
                    else -> GattFailure.NONE
                }
                val connected = connectWithConfiguration(address) { shadowGatt ->
                    var denyCharacteristic = call == "characteristic"
                    var denyDescriptor = call == "descriptor"
                    val service = object : BluetoothGattService(HRS_UUID, SERVICE_TYPE_PRIMARY) {
                        override fun getCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
                            if (denyCharacteristic) {
                                denyCharacteristic = false
                                throw SecurityException("characteristic denied")
                            }
                            return super.getCharacteristic(uuid)
                        }
                    }
                    val characteristic = object : BluetoothGattCharacteristic(MEASUREMENT_UUID, PROPERTY_NOTIFY, PERMISSION_READ) {
                        override fun getDescriptor(uuid: UUID): BluetoothGattDescriptor? {
                            if (denyDescriptor) {
                                denyDescriptor = false
                                throw SecurityException("descriptor denied")
                            }
                            return super.getDescriptor(uuid)
                        }
                    }
                    characteristic.addDescriptor(BluetoothGattDescriptor(CCCD_UUID, BluetoothGattDescriptor.PERMISSION_WRITE))
                    service.addCharacteristic(characteristic)
                    shadowGatt.addDiscoverableService(service)
                    shadowGatt.allowCharacteristicNotification(characteristic)
                }
                if (call == "disconnect" || call == "close") {
                    E17GattShadow.throwDisconnectSecurity = call == "disconnect"
                    E17GattShadow.throwCloseSecurity = call == "close"
                    owner.submit(HeartRateRuntimeAction.Disconnect)
                    idleMain()
                }
                assertTrue(call, connected.shadowGatt.isClosed || call == "disconnect" || call == "close")
            }
            assertEquals(call, HeartRateObservationCause.PERMISSION_REVOKED,
                (ledger.last().payload as HeartRateObservationPayload.RuntimeTransition).cause)
            assertEquals(call, (1L..ledger.size.toLong()).toList(), ledger.map { it.receipt })
            assertTrue(call, ledger.none { it.payload is HeartRateObservationPayload.ValidMeasurement })
            E17GattShadow.resetFailures()
            E17ScannerShadow.resetFailures()
            ObservationDeviceShadow.failure = ObservationDeviceFailure.NONE
            owner.close()
            idleMain()
            val scanner = application.getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
            shadowOf(scanner).scanCallbacks.toList().forEach(scanner::stopScan)
        }
    }

    @Test
    @Config(sdk = [32])
    fun observationLegacyDescriptorSecurityPreservesPermissionCause() {
        val ledger = mutableListOf<HeartRateObservation>()
        owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
        E17GattShadow.failure = GattFailure.DESCRIPTOR_SECURITY
        connect("AA:BB:CC:DD:EE:65")
        assertEquals(HeartRateObservationCause.PERMISSION_REVOKED,
            (ledger.last().payload as HeartRateObservationPayload.RuntimeTransition).cause)
    }

    @Test
    fun observationStopScanCapabilityLossDiffersFromDisabledAndUnknownFailure() {
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val ledger = mutableListOf<HeartRateObservation>()
        owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
        org.robolectric.shadows.ShadowBluetoothAdapter.setIsBluetoothSupported(false)
        E17ScannerShadow.throwStopIllegalState = true
        owner.submit(HeartRateRuntimeAction.StopScan)
        idleMain()
        assertEquals(listOf(HeartRateObservationPayload.RuntimeTransition(HeartRateObservationCause.PLATFORM_UNAVAILABLE)), ledger.map { it.payload })
    }

    @Test
    fun observationFinalCleanupPermissionIsNotOverwrittenByArmedScheduling() {
        val connected = connect("AA:BB:CC:DD:EE:66")
        owner.submit(HeartRateRuntimeAction.UpdateRecoveryEligibility(HeartRateRecoveryEligibilityInput(
            optedIn = true, savedTargetIdentifier = "AA:BB:CC:DD:EE:66", permissionGranted = true,
            bluetoothEnabled = true, manuallySuppressed = false, appVisible = true, activeTrainingFgsActive = false
        )))
        idleMain()
        val ledger = mutableListOf<HeartRateObservation>()
        owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
        E17GattShadow.throwCloseSecurity = true
        connected.callback.onConnectionStateChange(connected.gatt, 0, BluetoothProfile.STATE_DISCONNECTED)
        assertEquals(listOf(HeartRateObservationPayload.RuntimeTransition(HeartRateObservationCause.PERMISSION_REVOKED)), ledger.map { it.payload })
        assertEquals(listOf(1L), ledger.map { it.receipt })
        owner.submit(HeartRateRuntimeAction.Enable)
        idleMain()
        assertEquals(listOf(HeartRateObservationPayload.RuntimeTransition(HeartRateObservationCause.PERMISSION_REVOKED)), ledger.map { it.payload })
        E17GattShadow.throwCloseSecurity = false
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        assertEquals(HeartRateObservationCause.INITIAL_SEARCH,
            (ledger.last().payload as HeartRateObservationPayload.RuntimeTransition).cause)
    }

    @Test
    fun observationConnectionFailuresKeepUnknownReasonAndEstablishedDisconnectDistinct() {
        listOf("missing_candidate", "null_gatt", "connecting", "discovering", "subscribing", "waiting", "live", "status").forEachIndexed { index, phase ->
            E17GattShadow.resetFailures()
            ObservationDeviceShadow.failure = ObservationDeviceFailure.NONE
            owner = HeartRateRuntimeOwner(application)
            val ledger = mutableListOf<HeartRateObservation>()
            owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
            val address = "AA:BB:CC:DD:ED:${60 + index}"
            if (phase == "missing_candidate") {
                owner.submit(HeartRateRuntimeAction.Enable)
                owner.submit(HeartRateRuntimeAction.Connect(address))
                idleMain()
            } else {
                val device = scanDevice(address, "Connection fixture")
                if (phase == "null_gatt") ObservationDeviceShadow.failure = ObservationDeviceFailure.CONNECT_NULL
                E17GattShadow.failure = when (phase) {
                    "discovering" -> GattFailure.DISCOVER_HOLD
                    "subscribing" -> GattFailure.DESCRIPTOR_HOLD
                    else -> GattFailure.NONE
                }
                var connected: BluetoothGatt? = null
                Shadow.extract<ShadowBluetoothDevice>(device).setGattConnectionInterceptor { gatt ->
                    connected = gatt
                    val sg = Shadow.extract<ShadowBluetoothGatt>(gatt)
                    val characteristic = addHrs(sg, BluetoothGattCharacteristic.PROPERTY_NOTIFY, true)
                    if (phase != "connecting") sg.gattCallback.onConnectionStateChange(gatt, 0, BluetoothProfile.STATE_CONNECTED)
                    if (phase == "live") sg.gattCallback.onCharacteristicChanged(gatt, characteristic, byteArrayOf(0, 86))
                }
                owner.submit(HeartRateRuntimeAction.Connect(address))
                idleMain()
                connected?.let { gatt ->
                    val sg = Shadow.extract<ShadowBluetoothGatt>(gatt)
                    sg.gattCallback.onConnectionStateChange(gatt, 19,
                        if (phase == "status") BluetoothProfile.STATE_CONNECTED else BluetoothProfile.STATE_DISCONNECTED)
                    assertTrue(phase, sg.isClosed)
                }
            }
            val expected = if (phase in listOf("discovering", "subscribing", "waiting", "live")) {
                HeartRateObservationCause.UNEXPECTED_DISCONNECT
            } else HeartRateObservationCause.DISCONNECTED
            assertEquals(phase, expected, (ledger.last().payload as HeartRateObservationPayload.RuntimeTransition).cause)
            assertEquals(phase, (1L..ledger.size.toLong()).toList(), ledger.map { it.receipt })
            owner.close()
            idleMain()
        }
        ObservationDeviceShadow.failure = ObservationDeviceFailure.NONE
        E17GattShadow.resetFailures()
    }

    @Test
    fun observationEveryStreamCapabilityAndCallbackFailureUsesStreamReason() {
        listOf("discover_false", "discovery_status", "service_missing", "characteristic_missing",
            "properties", "descriptor_missing", "notify_false", "write_reject", "descriptor_status").forEachIndexed { index, fault ->
            owner = HeartRateRuntimeOwner(application)
            E17GattShadow.resetFailures()
            val ledger = mutableListOf<HeartRateObservation>()
            owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
            E17GattShadow.failure = when (fault) {
                "discover_false" -> GattFailure.DISCOVER_FALSE
                "discovery_status" -> GattFailure.DISCOVER_HOLD
                "service_missing" -> GattFailure.SERVICE_MISSING
                "write_reject" -> GattFailure.WRITE_REJECT
                "descriptor_status" -> GattFailure.DESCRIPTOR_HOLD
                else -> GattFailure.NONE
            }
            val connection = connectWithConfiguration("AA:BB:CC:DD:EC:${60 + index}") { sg ->
                val service = BluetoothGattService(HRS_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
                val characteristic = BluetoothGattCharacteristic(MEASUREMENT_UUID,
                    if (fault == "properties") BluetoothGattCharacteristic.PROPERTY_READ else BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ)
                if (fault != "descriptor_missing") characteristic.addDescriptor(BluetoothGattDescriptor(CCCD_UUID, 16))
                if (fault != "characteristic_missing") service.addCharacteristic(characteristic)
                sg.addDiscoverableService(service)
                if (fault != "notify_false") sg.allowCharacteristicNotification(characteristic)
            }
            if (fault == "discovery_status") connection.callback.onServicesDiscovered(connection.gatt, BluetoothGatt.GATT_FAILURE)
            if (fault == "descriptor_status") {
                val before = ledger.toList()
                connection.callback.onDescriptorWrite(connection.gatt, BluetoothGattDescriptor(CCCD_UUID, 16), BluetoothGatt.GATT_FAILURE)
                assertEquals(before, ledger)
                connection.callback.onDescriptorWrite(connection.gatt, connection.descriptor, BluetoothGatt.GATT_FAILURE)
            }
            assertEquals(fault, HeartRateObservationCause.MEASUREMENT_STREAM_UNAVAILABLE,
                (ledger.last().payload as HeartRateObservationPayload.RuntimeTransition).cause)
            assertTrue(fault, connection.shadowGatt.isClosed)
            assertTrue(fault, ledger.none { it.payload is HeartRateObservationPayload.ValidMeasurement })
        }
        E17GattShadow.resetFailures()
    }

    @Test
    @Config(sdk = [32])
    fun observationLegacyWriteRejectionUsesStreamReason() {
        val ledger = mutableListOf<HeartRateObservation>()
        owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
        E17GattShadow.failure = GattFailure.WRITE_REJECT
        connect("AA:BB:CC:DD:EB:60")
        assertEquals(HeartRateObservationCause.MEASUREMENT_STREAM_UNAVAILABLE,
            (ledger.last().payload as HeartRateObservationPayload.RuntimeTransition).cause)
    }

    @Test
    fun observationWrongGattCloseAndReentrantCleanupOnlyPublishFinalPermission() {
        listOf(false, true).forEach { duringCleanup ->
            E17GattShadow.resetFailures()
            owner = HeartRateRuntimeOwner(application)
            val connection = connect("AA:BB:CC:DD:EA:60")
            val ledger = mutableListOf<HeartRateObservation>()
            owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
            val wrong = ShadowBluetoothGatt.newInstance(connection.device)
            E17GattShadow.throwCloseSecurity = true
            val late = {
                connection.callback.onCharacteristicChanged(wrong, connection.characteristic, byteArrayOf(0, 99))
                connection.callback.onCharacteristicChanged(connection.gatt, connection.characteristic, byteArrayOf(0, 98))
            }
            if (duringCleanup) {
                E17GattShadow.beforeDisconnect = late
                connection.callback.onConnectionStateChange(connection.gatt, 0, BluetoothProfile.STATE_DISCONNECTED)
            } else late()
            assertEquals(listOf(HeartRateObservationPayload.RuntimeTransition(HeartRateObservationCause.PERMISSION_REVOKED)), ledger.map { it.payload })
            assertEquals(listOf(1L), ledger.map { it.receipt })
        }
        E17GattShadow.resetFailures()
    }

    @Test
    @Config(shadows = [ObservationAdapterShadow::class])
    fun observationUnavailableManagerAdapterScannerAndDisabledAdapterRemainDistinct() {
        listOf("manager", "adapter", "scanner", "disabled").forEach { missing ->
            val context = if (missing == "manager") object : ContextWrapper(application) {
                override fun getApplicationContext(): Context = this
                override fun getSystemService(name: String): Any? = if (name == Context.BLUETOOTH_SERVICE) null else super.getSystemService(name)
            } else application
            owner = HeartRateRuntimeOwner(context)
            val ledger = mutableListOf<HeartRateObservation>()
            owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
            if (missing == "adapter") org.robolectric.shadows.ShadowBluetoothAdapter.setIsBluetoothSupported(false)
            ObservationAdapterShadow.noScanner = missing == "scanner"
            if (missing == "disabled") shadowOf(application.getSystemService(BluetoothManager::class.java).adapter).setEnabled(false)
            owner.submit(HeartRateRuntimeAction.Enable)
            owner.submit(HeartRateRuntimeAction.StartScan)
            idleMain()
            assertEquals(missing, if (missing == "disabled") HeartRateObservationCause.BLUETOOTH_OFF else HeartRateObservationCause.PLATFORM_UNAVAILABLE,
                (ledger.last().payload as HeartRateObservationPayload.RuntimeTransition).cause)
            assertEquals(listOf(1L, 2L), ledger.map { it.receipt })
            org.robolectric.shadows.ShadowBluetoothAdapter.setIsBluetoothSupported(true)
            ObservationAdapterShadow.noScanner = false
        }
    }

    @Test
    fun observationStopScanDisabledPredicateAndUnknownExceptionKeepOriginalMeaning() {
        listOf(false, true).forEach { disabled ->
            E17ScannerShadow.resetFailures()
            owner = HeartRateRuntimeOwner(application)
            owner.submit(HeartRateRuntimeAction.Enable)
            owner.submit(HeartRateRuntimeAction.StartScan)
            idleMain()
            val ledger = mutableListOf<HeartRateObservation>()
            owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
            if (disabled) shadowOf(application.getSystemService(BluetoothManager::class.java).adapter).setEnabled(false)
            E17ScannerShadow.throwStopIllegalState = true
            owner.submit(HeartRateRuntimeAction.StopScan)
            if (disabled) {
                idleMain()
                assertEquals(listOf(HeartRateObservationPayload.RuntimeTransition(HeartRateObservationCause.BLUETOOTH_OFF)), ledger.map { it.payload })
            } else {
                val error = assertThrows(IllegalStateException::class.java) { idleMain() }
                org.junit.Assert.assertSame(E17ScannerShadow.stopError, error)
                assertTrue(ledger.isEmpty())
            }
            E17ScannerShadow.resetFailures()
            val adapter = application.getSystemService(BluetoothManager::class.java).adapter
            shadowOf(adapter).setEnabled(true)
            val scanner = adapter.bluetoothLeScanner
            shadowOf(scanner).scanCallbacks.toList().forEach(scanner::stopScan)
        }
    }

    @Test
    fun observationDisableAndBackgroundKeepExplicitCauseDespiteClosePermissionLoss() {
        listOf(HeartRateRuntimeAction.Disable to HeartRateObservationCause.NOT_OBSERVING,
            HeartRateRuntimeAction.BackgroundCleanup to HeartRateObservationCause.DISCONNECTED).forEach { (action, cause) ->
            E17GattShadow.resetFailures()
            owner = HeartRateRuntimeOwner(application)
            connect("AA:BB:CC:DD:E9:60")
            val ledger = mutableListOf<HeartRateObservation>()
            owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
            E17GattShadow.throwCloseSecurity = true
            owner.submit(action)
            idleMain()
            assertEquals(listOf(HeartRateObservationPayload.RuntimeTransition(cause)), ledger.map { it.payload })
        }
        E17GattShadow.resetFailures()
    }

    @Test
    fun observationReplacementDetachDisconnectAndCloseFailuresKeepPermissionCause() {
        listOf("disconnect", "close").forEach { operation ->
            E17GattShadow.resetFailures()
            owner = HeartRateRuntimeOwner(application)
            val first = connect("AA:BB:CC:DD:E8:60")
            val second = scanDevice("AA:BB:CC:DD:E8:61", "Replacement")
            val ledger = mutableListOf<HeartRateObservation>()
            owner.bindObservations(HeartRateObservationBindingId(), ledger::add)
            E17GattShadow.throwDisconnectSecurity = operation == "disconnect"
            E17GattShadow.throwCloseSecurity = operation == "close"
            owner.submit(HeartRateRuntimeAction.Connect(second.address))
            idleMain()
            assertEquals(operation, listOf(HeartRateObservationPayload.RuntimeTransition(HeartRateObservationCause.PERMISSION_REVOKED)), ledger.map { it.payload })
            assertTrue(Shadow.extract<ShadowBluetoothDevice>(second).bluetoothGatts.isEmpty())
            first.callback.onCharacteristicChanged(first.gatt, first.characteristic, byteArrayOf(0, 99))
            assertEquals(operation, 1, ledger.size)
        }
        E17GattShadow.resetFailures()
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
    fun api33NotifyPathWritesCccdAndOnlyValueOverloadConsumesMeasurements() {
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
        connected.characteristic.value = byteArrayOf(0x00, 99)
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
    fun scanTimeoutAfterAdapterTurnsOffToleratesPlatformStopScanIllegalState() {
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val timeout = privateRunnable("scanTimeoutRunnable")
        val adapter = application.getSystemService(BluetoothManager::class.java).adapter
        val callback = shadowOf(adapter.bluetoothLeScanner).scanCallbacks.single()
        shadowOf(adapter).setEnabled(false)
        E17ScannerShadow.throwStopIllegalState = true

        android.os.Handler(Looper.getMainLooper()).post(timeout)
        idleMain()

        val terminal = owner.heartRateState.value
        callback.onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        idleMain()

        assertEquals(HeartRateFact.BLUETOOTH_OFF, terminal.fact)
        assertEquals(terminal, owner.heartRateState.value)
    }

    @Test
    fun scanTimeoutWhileAdapterRemainsOnDoesNotHideUnknownStopScanIllegalState() {
        owner.submit(HeartRateRuntimeAction.StartScan)
        idleMain()
        val timeout = privateRunnable("scanTimeoutRunnable")
        E17ScannerShadow.throwStopIllegalState = true

        android.os.Handler(Looper.getMainLooper()).post(timeout)

        assertThrows(IllegalStateException::class.java) { idleMain() }
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
    SERVICE_SECURITY,
    SERVICE_MISSING,
    DISCOVER_FALSE,
    DISCOVER_HOLD,
    DESCRIPTOR_HOLD,
    WRITE_REJECT,
    NOTIFICATION_SECURITY,
    DESCRIPTOR_SECURITY,
    DISCOVER_ILLEGAL_STATE
}

@Implements(BluetoothGatt::class)
class E17GattShadow : ShadowBluetoothGatt() {
    @Implementation
    override fun getService(uuid: UUID): BluetoothGattService? {
        if (failure == GattFailure.SERVICE_SECURITY) throw SecurityException("service denied")
        if (failure == GattFailure.SERVICE_MISSING) return null
        return super.getService(uuid)
    }
    @Implementation
    override fun discoverServices(): Boolean {
        discoverCalls += 1
        when (failure) {
            GattFailure.DISCOVER_SECURITY -> throw SecurityException("test discovery revocation")
            GattFailure.DISCOVER_ILLEGAL_STATE -> throw IllegalStateException("unknown test defect")
            GattFailure.DISCOVER_FALSE -> return false
            GattFailure.DISCOVER_HOLD -> return true
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
        if (failure == GattFailure.WRITE_REJECT) return android.bluetooth.BluetoothStatusCodes.ERROR_UNKNOWN
        if (failure == GattFailure.DESCRIPTOR_HOLD) return android.bluetooth.BluetoothStatusCodes.SUCCESS
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
        if (failure == GattFailure.WRITE_REJECT) return false
        if (failure == GattFailure.DESCRIPTOR_HOLD) return true
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
        val hook = beforeDisconnect
        beforeDisconnect = null
        hook?.invoke()
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
        var beforeDisconnect: (() -> Unit)? = null

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
            beforeDisconnect = null
        }
    }
}

enum class ObservationDeviceFailure { NONE, ADDRESS, NAME, CONNECT, CONNECT_NULL }

@Implements(BluetoothDevice::class)
class ObservationDeviceShadow : ShadowBluetoothDevice() {
    @RealObject private lateinit var device: BluetoothDevice

    @Implementation
    fun getAddress(): String {
        if (failure == ObservationDeviceFailure.ADDRESS) throw SecurityException("address denied")
        return Shadow.directlyOn(device, BluetoothDevice::class.java, "getAddress")
    }

    @Implementation
    override fun getName(): String? {
        if (failure == ObservationDeviceFailure.NAME) throw SecurityException("name denied")
        return super.getName()
    }

    @Implementation
    override fun connectGatt(context: Context, autoConnect: Boolean, callback: BluetoothGattCallback,
        transport: Int, phy: Int, handler: android.os.Handler): BluetoothGatt? {
        if (failure == ObservationDeviceFailure.CONNECT) throw SecurityException("connect denied")
        if (failure == ObservationDeviceFailure.CONNECT_NULL) return null
        return super.connectGatt(context, autoConnect, callback, transport, phy, handler)
    }

    companion object {
        var failure = ObservationDeviceFailure.NONE
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
        if (throwStopIllegalState) throw stopError
        super.stopScan(callback)
    }

    companion object {
        var throwStartSecurity: Boolean = false
        var throwStopSecurity: Boolean = false
        var throwStopIllegalState: Boolean = false
        var stopError = IllegalStateException("test unknown scanner state")

        fun resetFailures() {
            throwStartSecurity = false
            throwStopSecurity = false
            throwStopIllegalState = false
            stopError = IllegalStateException("test unknown scanner state")
        }
    }
}

@Implements(android.bluetooth.BluetoothAdapter::class)
class ObservationAdapterShadow : org.robolectric.shadows.ShadowBluetoothAdapter() {
    @RealObject private lateinit var adapter: android.bluetooth.BluetoothAdapter

    @Implementation
    fun getBluetoothLeScanner(): BluetoothLeScanner? = if (noScanner) null else
        Shadow.directlyOn(adapter, android.bluetooth.BluetoothAdapter::class.java, "getBluetoothLeScanner")

    companion object { var noScanner = false }
}
