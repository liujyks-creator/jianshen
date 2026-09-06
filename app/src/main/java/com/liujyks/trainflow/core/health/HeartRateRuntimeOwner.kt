package com.liujyks.trainflow.core.health

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateTechnicalFailure
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Explicit inputs only. Construction never starts scanning, connecting, or reconnecting. */
internal sealed class HeartRateRuntimeAction {
    data object Enable : HeartRateRuntimeAction()
    data object Disable : HeartRateRuntimeAction()
    data object PermissionLost : HeartRateRuntimeAction()
    data object BluetoothOff : HeartRateRuntimeAction()
    data object BackgroundCleanup : HeartRateRuntimeAction()
    data object StartScan : HeartRateRuntimeAction()
    data object StopScan : HeartRateRuntimeAction()
    data class Connect(val identifier: String) : HeartRateRuntimeAction()
    data class UpdateRecoveryEligibility(
        val input: HeartRateRecoveryEligibilityInput
    ) : HeartRateRuntimeAction()
    data object Disconnect : HeartRateRuntimeAction()
    data object Stop : HeartRateRuntimeAction()
}

/**
 * Deterministic BLE runtime owner introduced in E17-6 and activated as the single
 * Application-scoped production owner in E17-7b. Activities, Compose, and debug tools may observe
 * state or submit actions, but cannot own BLE resources.
 */
@SuppressLint("MissingPermission")
internal class HeartRateRuntimeOwner(
    context: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val scanWindowMillis: Long = DEFAULT_SCAN_WINDOW_MILLIS,
    private val recoveryIntervalMillis: Long = DEFAULT_RECOVERY_INTERVAL_MILLIS,
    private val freshnessConfig: HeartRateFreshnessConfig = HeartRateFreshnessConfig()
) : HeartRateProvider, AutoCloseable {
    private val appContext = context.applicationContext
    private val freshnessPolicy = HeartRateFreshnessPolicy(freshnessConfig)
    private val mutableHeartRateState = MutableStateFlow(
        HeartRateRuntimeFact.Disabled.toHeartRateState()
    )
    private val mutableScanState = MutableStateFlow(BleHeartRateScanState.idle())
    private val mutableCandidates = MutableStateFlow<List<BleHeartRateDeviceCandidate>>(emptyList())
    private val mutableRecoveryState = MutableStateFlow(
        HeartRateRecoveryState.disarmed(HeartRateRecoveryStopReason.OPTED_OUT)
    )

    override val heartRateState: StateFlow<HeartRateState> = mutableHeartRateState
    val scanState: StateFlow<BleHeartRateScanState> = mutableScanState
    val candidates: StateFlow<List<BleHeartRateDeviceCandidate>> = mutableCandidates
    val recoveryState: StateFlow<HeartRateRecoveryState> = mutableRecoveryState

    private val candidateDevices = linkedMapOf<String, BluetoothDevice>()
    private var scanGeneration = 0L
    private var attemptSequence = 0L
    private var activeScan: ActiveScan? = null
    private var activeAttempt: ActiveAttempt? = null
    private var scanTimeoutRunnable: Runnable? = null
    private var freshnessRunnable: Runnable? = null
    private var recoveryRunnable: Runnable? = null
    private var recoveryEligibilityInput = HeartRateRecoveryEligibilityInput(
        optedIn = false,
        savedTargetIdentifier = null,
        permissionGranted = false,
        bluetoothEnabled = false,
        manuallySuppressed = false,
        appVisible = false,
        activeTrainingFgsActive = false
    )
    private var enabled = false
    private var operationEligible = false
    private var ownerClosed = false
    private var cleanupInProgress = false
    private var cleanupGatt: BluetoothGatt? = null
    private var cleanupObservedPermissionLoss = false
    private var observationCause = HeartRateObservationCause.NOT_OBSERVING
    private var observationBinding: HeartRateObservationBinding? = null
    private var observationSink: ((HeartRateObservation) -> Unit)? = null
    private var observationReceipt = 0L
    private var observedPermissionGrant = false

    /** Synchronous main cut. The sink only enqueues; it must not do IO or reenter this owner. */
    fun bindObservations(
        bindingId: HeartRateObservationBindingId,
        sink: (HeartRateObservation) -> Unit
    ): HeartRateBindingDisposition {
        checkMainThread()
        val disposition = queryObservationBinding(bindingId)
        if (disposition != HeartRateBindingDisposition.KnownAbsent) return disposition
        val anchor = SystemClock.elapsedRealtime()
        val binding = HeartRateObservationBinding(
            bindingId,
            anchor,
            HeartRateObservation(
                bindingId, 0L, anchor,
                HeartRateObservationPayload.CurrentSnapshot(observationCause)
            )
        )
        observationBinding = binding
        observationSink = sink
        observationReceipt = 0L
        return HeartRateBindingDisposition.MatchingInstalled(binding)
    }

    /** A failed main dispatch is unresolved for the caller, never evidence of absence. */
    fun queryObservationBinding(bindingId: HeartRateObservationBindingId): HeartRateBindingDisposition {
        checkMainThread()
        val binding = observationBinding ?: return HeartRateBindingDisposition.KnownAbsent
        return if (binding.bindingId === bindingId) {
            HeartRateBindingDisposition.MatchingInstalled(binding)
        } else {
            HeartRateBindingDisposition.ConflictingInstalled(binding.bindingId)
        }
    }

    fun unbindObservations(bindingId: HeartRateObservationBindingId): HeartRateUnbindDisposition {
        checkMainThread()
        val binding = observationBinding ?: return HeartRateUnbindDisposition.KNOWN_ABSENT
        if (binding.bindingId !== bindingId) return HeartRateUnbindDisposition.CONFLICTING_INSTALLED
        observationBinding = null
        observationSink = null
        return HeartRateUnbindDisposition.REMOVED
    }

    /** Every typed action is queued, including calls made from the main thread. */
    fun submit(action: HeartRateRuntimeAction) {
        mainHandler.post { handleAction(action) }
    }

    override fun close() {
        submit(HeartRateRuntimeAction.Stop)
    }

    private fun handleAction(action: HeartRateRuntimeAction) {
        checkMainThread()
        if (ownerClosed) return
        when (action) {
            HeartRateRuntimeAction.Enable -> enableOnMain()
            HeartRateRuntimeAction.Disable -> disableOnMain()
            HeartRateRuntimeAction.PermissionLost -> permissionLostOnMain()
            HeartRateRuntimeAction.BluetoothOff -> bluetoothOffOnMain()
            HeartRateRuntimeAction.BackgroundCleanup -> backgroundCleanupOnMain()
            HeartRateRuntimeAction.StartScan -> startScanOnMain()
            HeartRateRuntimeAction.StopScan -> stopScanOnMain()
            is HeartRateRuntimeAction.Connect -> connectOnMain(action.identifier)
            is HeartRateRuntimeAction.UpdateRecoveryEligibility ->
                updateRecoveryEligibilityOnMain(action.input)
            HeartRateRuntimeAction.Disconnect -> {
                recoveryEligibilityInput = recoveryEligibilityInput.copy(manuallySuppressed = true)
                cancelRecovery()
                publishRecovery(
                    HeartRateRecoveryState.disarmed(
                        HeartRateRecoveryStopReason.MANUAL_SUPPRESSION
                    )
                )
                if (enabled) {
                    cleanup(HeartRateRuntimeFact.IntentionalStop(activeAttempt?.source))
                } else {
                    publish(HeartRateRuntimeFact.Disabled)
                }
            }
            HeartRateRuntimeAction.Stop -> {
                ownerClosed = true
                cancelRecovery()
                publishRecovery(
                    HeartRateRecoveryState.disarmed(HeartRateRecoveryStopReason.OWNER_CLOSED)
                )
                cleanup(
                    if (enabled) {
                        HeartRateRuntimeFact.IntentionalStop(activeAttempt?.source)
                    } else {
                        HeartRateRuntimeFact.Disabled
                    }
                )
            }
        }
    }

    private fun enableOnMain() {
        checkMainThread()
        if (enabled && (activeScan != null || activeAttempt != null)) return
        enabled = true
        operationEligible = true
        // Enable alone is not new permission evidence. A successful acquisition clears this fact.
        publish(HeartRateRuntimeFact.NotConnected(), when (observationCause) {
            HeartRateObservationCause.PERMISSION_MISSING,
            HeartRateObservationCause.PERMISSION_REVOKED -> observationCause
            else -> HeartRateObservationCause.DISCONNECTED
        })
    }

    private fun disableOnMain() {
        checkMainThread()
        if (!enabled && activeScan == null && activeAttempt == null) {
            publish(HeartRateRuntimeFact.Disabled)
            return
        }
        enabled = false
        operationEligible = false
        recoveryEligibilityInput = recoveryEligibilityInput.copy(optedIn = false)
        cancelRecovery()
        publishRecovery(
            HeartRateRecoveryState.disarmed(HeartRateRecoveryStopReason.OPTED_OUT)
        )
        cleanup(
            requestedFact = HeartRateRuntimeFact.Disabled,
            permissionLossOverridesFact = false
        )
    }

    private fun permissionLostOnMain() {
        checkMainThread()
        if (!enabled) {
            publish(HeartRateRuntimeFact.Disabled)
            return
        }
        operationEligible = false
        recoveryEligibilityInput = recoveryEligibilityInput.copy(permissionGranted = false)
        cancelRecovery()
        publishRecovery(
            HeartRateRecoveryState.disarmed(
                HeartRateRecoveryStopReason.PERMISSION_UNAVAILABLE
            )
        )
        cleanup(
            requestedFact = HeartRateRuntimeFact.PermissionRequired(currentSource()),
            permissionLossOverridesFact = false
        )
    }

    private fun bluetoothOffOnMain() {
        checkMainThread()
        if (!enabled) {
            publish(HeartRateRuntimeFact.Disabled)
            return
        }
        operationEligible = false
        recoveryEligibilityInput = recoveryEligibilityInput.copy(bluetoothEnabled = false)
        cancelRecovery()
        publishRecovery(
            HeartRateRecoveryState.disarmed(HeartRateRecoveryStopReason.BLUETOOTH_OFF)
        )
        cleanup(
            requestedFact = HeartRateRuntimeFact.BluetoothOff(currentSource()),
            permissionLossOverridesFact = false
        )
    }

    private fun backgroundCleanupOnMain() {
        checkMainThread()
        if (!enabled) {
            publish(HeartRateRuntimeFact.Disabled)
            return
        }
        operationEligible = false
        recoveryEligibilityInput = recoveryEligibilityInput.copy(
            appVisible = false,
            activeTrainingFgsActive = false
        )
        cancelRecovery()
        publishRecovery(
            HeartRateRecoveryState.disarmed(
                HeartRateRecoveryStopReason.BACKGROUND_WITHOUT_FGS
            )
        )
        cleanup(
            requestedFact = HeartRateRuntimeFact.IntentionalStop(activeAttempt?.source),
            permissionLossOverridesFact = false,
            requestedCause = HeartRateObservationCause.DISCONNECTED
        )
    }

    private fun startScanOnMain(
        origin: ScanOrigin = ScanOrigin.MANUAL,
        recoveryTargetIdentifier: String? = null
    ) {
        checkMainThread()
        if (!enabled) {
            publish(HeartRateRuntimeFact.Disabled)
            return
        }
        if (!operationEligible) return
        if (origin == ScanOrigin.MANUAL) {
            cancelRecovery()
        }
        if (!hasRequiredPermissions()) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(currentSource()))
            return
        }
        val adapter = bluetoothAdapterOrPublishFailure() ?: return
        val enabled = try {
            adapter.isEnabled
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(currentSource()))
            return
        }
        if (!enabled) {
            cleanup(HeartRateRuntimeFact.BluetoothOff(currentSource()))
            return
        }
        val scanner = try {
            adapter.bluetoothLeScanner
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(currentSource()))
            return
        }
        if (scanner == null) {
            cleanup(
                HeartRateRuntimeFact.TechnicalFailure(
                    HeartRateTechnicalFailure.PLATFORM_UNAVAILABLE,
                    currentSource()
                )
            )
            return
        }

        if (!detachAndStopActiveScan()) return
        candidateDevices.clear()
        mutableCandidates.value = emptyList()
        val generation = nextScanGeneration()
        val callback = RuntimeScanCallback(generation)
        activeScan = ActiveScan(
            generation = generation,
            scanner = scanner,
            callback = callback,
            origin = origin,
            recoveryTargetIdentifier = recoveryTargetIdentifier
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            scanner.startScan(heartRateServiceFilters(), settings, callback)
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(currentSource()))
            return
        }
        if (!scanMatches(generation, callback)) return
        val timeout = Runnable { handleScanTimeout(generation, callback) }
        scanTimeoutRunnable = timeout
        mainHandler.postDelayed(timeout, scanWindowMillis)
        mutableScanState.value = BleHeartRateScanState(
            kind = BleHeartRateScanStateKind.SCANNING,
            message = "Scanning standard BLE Heart Rate Service"
        )
        if (activeAttempt == null) {
            publish(HeartRateRuntimeFact.Scanning(), if (origin == ScanOrigin.RECOVERY) {
                HeartRateObservationCause.RECOVERY_SEARCH
            } else {
                HeartRateObservationCause.INITIAL_SEARCH
            })
        }
        if (origin == ScanOrigin.RECOVERY) {
            publishRecovery(
                HeartRateRecoveryState(
                    phase = HeartRateRecoveryPhase.SEARCHING,
                    targetIdentifier = recoveryTargetIdentifier
                )
            )
        }
    }

    private fun stopScanOnMain() {
        checkMainThread()
        if (!enabled) {
            publish(HeartRateRuntimeFact.Disabled)
            return
        }
        if (!operationEligible) return
        if (!detachAndStopActiveScan()) return
        mutableScanState.value = BleHeartRateScanState(
            kind = BleHeartRateScanStateKind.STOPPED,
            message = "BLE heart-rate scan stopped"
        )
        if (activeAttempt == null) {
            publish(HeartRateRuntimeFact.IntentionalStop(), HeartRateObservationCause.DISCONNECTED)
        }
        scheduleRecovery(
            delayMillis = recoveryIntervalMillis,
            pendingPhase = HeartRateRecoveryPhase.WAITING_NEXT_WINDOW
        )
    }

    private fun updateRecoveryEligibilityOnMain(
        input: HeartRateRecoveryEligibilityInput
    ) {
        checkMainThread()
        if (input.permissionGranted) observedPermissionGrant = true
        recoveryEligibilityInput = input.copy(
            savedTargetIdentifier = input.savedTargetIdentifier?.trim()
        )
        val decision = evaluateHeartRateRecoveryEligibility(recoveryEligibilityInput)
        if (!decision.eligible) {
            applyIneligibleRecoveryDecision(requireNotNull(decision.stopReason))
            return
        }

        enabled = true
        operationEligible = true
        val target = requireNotNull(decision.targetIdentifier)
        val scan = activeScan
        if (scan?.origin == ScanOrigin.RECOVERY) {
            if (scan.recoveryTargetIdentifier == target) {
                cancelRecovery()
                publishRecovery(
                    HeartRateRecoveryState(
                        phase = HeartRateRecoveryPhase.SEARCHING,
                        targetIdentifier = target
                    )
                )
                return
            }
            if (!detachAndStopActiveScan()) return
        }
        val attempt = activeAttempt
        if (attempt != null && attempt.targetIdentifier == target) {
            cancelRecovery()
            publishRecovery(
                HeartRateRecoveryState(
                    phase = HeartRateRecoveryPhase.CONNECTING_OR_CONNECTED,
                    targetIdentifier = target
                )
            )
            return
        }
        if (attempt != null && !detachAndCloseActiveAttempt()) return
        scheduleRecovery(delayMillis = 0L)
    }

    private fun applyIneligibleRecoveryDecision(reason: HeartRateRecoveryStopReason) {
        cancelRecovery()
        publishRecovery(HeartRateRecoveryState.disarmed(reason))
        when (reason) {
            HeartRateRecoveryStopReason.OPTED_OUT -> {
                enabled = false
                operationEligible = false
                cleanup(
                    requestedFact = HeartRateRuntimeFact.Disabled,
                    permissionLossOverridesFact = false
                )
            }
            HeartRateRecoveryStopReason.MANUAL_SUPPRESSION -> {
                enabled = true
                operationEligible = false
                cleanup(
                    requestedFact = HeartRateRuntimeFact.IntentionalStop(activeAttempt?.source),
                    permissionLossOverridesFact = false
                )
            }
            HeartRateRecoveryStopReason.NO_SAVED_TARGET -> {
                enabled = true
                operationEligible = true
                if (activeScan?.origin == ScanOrigin.MANUAL) {
                    return
                }
                cleanup(
                    requestedFact = HeartRateRuntimeFact.NotConnected(),
                    permissionLossOverridesFact = false,
                    requestedCause = HeartRateObservationCause.NO_SOURCE_SELECTED
                )
            }
            HeartRateRecoveryStopReason.PERMISSION_UNAVAILABLE -> {
                enabled = true
                operationEligible = false
                cleanup(
                    requestedFact = HeartRateRuntimeFact.PermissionRequired(currentSource()),
                    permissionLossOverridesFact = false
                )
            }
            HeartRateRecoveryStopReason.BLUETOOTH_OFF -> {
                enabled = true
                operationEligible = false
                cleanup(
                    requestedFact = HeartRateRuntimeFact.BluetoothOff(currentSource()),
                    permissionLossOverridesFact = false
                )
            }
            HeartRateRecoveryStopReason.BACKGROUND_WITHOUT_FGS -> {
                enabled = true
                operationEligible = false
                cleanup(
                    requestedFact = HeartRateRuntimeFact.IntentionalStop(activeAttempt?.source),
                    permissionLossOverridesFact = false,
                    requestedCause = HeartRateObservationCause.DISCONNECTED
                )
            }
            HeartRateRecoveryStopReason.OWNER_CLOSED -> Unit
        }
    }

    private fun scheduleRecovery(
        delayMillis: Long,
        pendingPhase: HeartRateRecoveryPhase = HeartRateRecoveryPhase.WAITING_NEXT_WINDOW
    ) {
        cancelRecovery()
        if (ownerClosed) {
            publishRecovery(
                HeartRateRecoveryState.disarmed(HeartRateRecoveryStopReason.OWNER_CLOSED)
            )
            return
        }
        val decision = evaluateHeartRateRecoveryEligibility(recoveryEligibilityInput)
        if (!decision.eligible) {
            publishRecovery(
                HeartRateRecoveryState.disarmed(requireNotNull(decision.stopReason))
            )
            return
        }
        val target = requireNotNull(decision.targetIdentifier)
        val attempt = activeAttempt
        if (attempt != null && attempt.targetIdentifier == target) {
            publishRecovery(
                HeartRateRecoveryState(
                    phase = HeartRateRecoveryPhase.CONNECTING_OR_CONNECTED,
                    targetIdentifier = target
                )
            )
            return
        }
        if (activeScan != null) {
            publishRecovery(
                HeartRateRecoveryState(
                    phase = pendingPhase,
                    targetIdentifier = target
                )
            )
            return
        }
        publishRecovery(
            HeartRateRecoveryState(
                phase = pendingPhase,
                targetIdentifier = target
            )
        )
        val runnable = Runnable {
            recoveryRunnable = null
            startRecoveryWindow()
        }
        recoveryRunnable = runnable
        mainHandler.postDelayed(runnable, delayMillis)
    }

    private fun startRecoveryWindow() {
        checkMainThread()
        val decision = evaluateHeartRateRecoveryEligibility(recoveryEligibilityInput)
        if (!decision.eligible) {
            publishRecovery(
                HeartRateRecoveryState.disarmed(requireNotNull(decision.stopReason))
            )
            return
        }
        val target = requireNotNull(decision.targetIdentifier)
        val attempt = activeAttempt
        if (attempt != null && attempt.targetIdentifier == target) {
            publishRecovery(
                HeartRateRecoveryState(
                    phase = HeartRateRecoveryPhase.CONNECTING_OR_CONNECTED,
                    targetIdentifier = target
                )
            )
            return
        }
        if (activeScan != null) {
            scheduleRecovery(recoveryIntervalMillis)
            return
        }
        startScanOnMain(
            origin = ScanOrigin.RECOVERY,
            recoveryTargetIdentifier = target
        )
    }

    private fun handleScanTimeout(generation: Long, callback: ScanCallback) {
        checkMainThread()
        if (!scanMatches(generation, callback)) return
        val timedOutScan = activeScan ?: return
        if (!detachAndStopActiveScan()) return
        mutableScanState.value = BleHeartRateScanState(
            kind = BleHeartRateScanStateKind.STOPPED,
            message = "Finite BLE heart-rate scan window ended"
        )
        if (activeAttempt == null) {
            publish(HeartRateRuntimeFact.NotConnected(), if (
                timedOutScan.recoveryTargetIdentifier != null ||
                recoveryEligibilityInput.savedTargetIdentifier != null
            ) {
                HeartRateObservationCause.SOURCE_UNAVAILABLE
            } else {
                HeartRateObservationCause.DISCONNECTED
            })
        }
        scheduleRecovery(
            delayMillis = recoveryIntervalMillis,
            pendingPhase = if (timedOutScan.origin == ScanOrigin.RECOVERY) {
                HeartRateRecoveryPhase.WINDOW_MISSED_ARMED
            } else {
                HeartRateRecoveryPhase.WAITING_NEXT_WINDOW
            }
        )
    }

    private fun handleScanResult(
        generation: Long,
        callback: ScanCallback,
        result: ScanResult
    ) {
        checkMainThread()
        if (!scanMatches(generation, callback)) return
        val device = result.device ?: return
        val identifier = try {
            device.address
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(currentSource()))
            return
        }
        val displayName = try {
            device.name
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(currentSource()))
            return
        }.takeUnless { it.isNullOrBlank() } ?: UNKNOWN_DEVICE_NAME
        val advertisesHrs = result.scanRecord?.serviceUuids
            ?.any { it.uuid == HEART_RATE_SERVICE_UUID } == true
        val candidate = BleHeartRateDeviceCandidate(
            identifier = identifier,
            displayName = displayName,
            rssi = result.rssi,
            advertisesHeartRateService = advertisesHrs
        )
        candidateDevices[identifier] = device
        mutableCandidates.value = mutableCandidates.value
            .filterNot { it.identifier == identifier } + candidate
        val scan = activeScan
        if (scan?.origin == ScanOrigin.RECOVERY &&
            identifier == scan.recoveryTargetIdentifier
        ) {
            connectOnMain(identifier, ScanOrigin.RECOVERY)
        }
    }

    private fun handleScanFailure(
        generation: Long,
        callback: ScanCallback,
        errorCode: Int
    ) {
        checkMainThread()
        if (!scanMatches(generation, callback)) return
        activeScan = null
        scanGeneration += 1L
        cancelScanTimeout()
        mutableScanState.value = BleHeartRateScanState(
            kind = BleHeartRateScanStateKind.ERROR,
            message = "BLE heart-rate scan failed (code $errorCode)",
            recoverableReason = BleHeartRateRecoverableReason.SCAN_FAILED
        )
        if (activeAttempt == null) {
            publish(
                HeartRateRuntimeFact.TechnicalFailure(
                    HeartRateTechnicalFailure.PLATFORM_FAILURE
                )
            )
        }
        scheduleRecovery(
            delayMillis = recoveryIntervalMillis,
            pendingPhase = HeartRateRecoveryPhase.WINDOW_MISSED_ARMED
        )
    }

    private fun connectOnMain(identifier: String, origin: ScanOrigin = ScanOrigin.MANUAL) {
        checkMainThread()
        if (!enabled) {
            publish(HeartRateRuntimeFact.Disabled)
            return
        }
        if (!operationEligible) return
        val device = candidateDevices[identifier]
        val candidate = mutableCandidates.value.firstOrNull { it.identifier == identifier }
        if (device == null || candidate == null) {
            if (activeAttempt == null) {
                publish(
                    HeartRateRuntimeFact.TechnicalFailure(
                        HeartRateTechnicalFailure.CONNECT_FAILED,
                        sourceForIdentifier(identifier)
                    )
                )
            }
            return
        }
        recoveryEligibilityInput = recoveryEligibilityInput.copy(
            savedTargetIdentifier = identifier,
            manuallySuppressed = false
        )
        cancelRecovery()
        if (!hasRequiredPermissions()) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(sourceForIdentifier(identifier)))
            return
        }
        val adapter = bluetoothAdapterOrPublishFailure() ?: return
        val enabled = try {
            adapter.isEnabled
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(sourceForIdentifier(identifier)))
            return
        }
        if (!enabled) {
            cleanup(HeartRateRuntimeFact.BluetoothOff(sourceForIdentifier(identifier)))
            return
        }
        if (!detachAndStopActiveScan()) return
        if (!detachAndCloseActiveAttempt()) return

        val source = HeartRateSourceHint(candidate.identifier, candidate.displayName)
        val attemptId = nextAttemptId()
        val attempt = ActiveAttempt(
            id = attemptId,
            ownerGeneration = scanGeneration,
            targetIdentifier = identifier,
            source = source,
            origin = origin,
            phase = AttemptPhase.CONNECTING
        )
        activeAttempt = attempt
        val callback = AttemptGattCallback(
            attemptId = attemptId,
            ownerGeneration = attempt.ownerGeneration,
            targetIdentifier = identifier
        )
        attempt.callback = callback
        publish(HeartRateRuntimeFact.Connecting(source))
        publishRecovery(
            HeartRateRecoveryState(
                phase = HeartRateRecoveryPhase.CONNECTING_OR_CONNECTED,
                targetIdentifier = identifier
            )
        )

        val returnedGatt = try {
            device.connectGatt(
                appContext,
                false,
                callback,
                BluetoothDevice.TRANSPORT_LE,
                BluetoothDevice.PHY_LE_1M_MASK,
                mainHandler
            )
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(source))
            return
        }
        if (returnedGatt == null) {
            cleanup(
                HeartRateRuntimeFact.TechnicalFailure(
                    HeartRateTechnicalFailure.CONNECT_FAILED,
                    source
                )
            )
            return
        }
        bindReturnedGatt(attemptId, attempt.ownerGeneration, identifier, returnedGatt)
    }

    private fun handleConnectionStateChange(
        attemptId: Long,
        ownerGeneration: Long,
        targetIdentifier: String,
        gatt: BluetoothGatt,
        status: Int,
        newState: Int
    ) {
        checkMainThread()
        val attempt = bindCallbackGatt(
            attemptId,
            ownerGeneration,
            targetIdentifier,
            gatt
        ) ?: return
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            cleanup(
                if (attempt.phase == AttemptPhase.CONNECTING) {
                    HeartRateRuntimeFact.TechnicalFailure(
                        HeartRateTechnicalFailure.CONNECT_FAILED,
                        attempt.source
                    )
                } else {
                    HeartRateRuntimeFact.LinkDisconnected(attempt.source)
                }
            )
            return
        }
        if (status != BluetoothGatt.GATT_SUCCESS) {
            cleanup(
                HeartRateRuntimeFact.TechnicalFailure(
                    HeartRateTechnicalFailure.CONNECT_FAILED,
                    attempt.source
                )
            )
            return
        }
        if (newState == BluetoothProfile.STATE_CONNECTED &&
            attempt.phase != AttemptPhase.CONNECTING
        ) {
            return
        }
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            discoverServices(attempt, gatt)
        }
    }

    private fun discoverServices(attempt: ActiveAttempt, gatt: BluetoothGatt) {
        if (!attemptMatches(attempt, gatt)) return
        attempt.phase = AttemptPhase.DISCOVERING
        val started = try {
            gatt.discoverServices()
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(attempt.source))
            return
        }
        if (!started && attemptMatches(attempt, gatt) && attempt.phase == AttemptPhase.DISCOVERING) {
            cleanup(
                HeartRateRuntimeFact.TechnicalFailure(
                    HeartRateTechnicalFailure.SERVICE_DISCOVERY_FAILED,
                    attempt.source
                )
            )
        }
    }

    private fun handleServicesDiscovered(
        attemptId: Long,
        ownerGeneration: Long,
        targetIdentifier: String,
        gatt: BluetoothGatt,
        status: Int
    ) {
        checkMainThread()
        val attempt = acceptBoundCallback(
            attemptId,
            ownerGeneration,
            targetIdentifier,
            gatt
        ) ?: return
        if (attempt.phase != AttemptPhase.DISCOVERING) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            cleanup(
                HeartRateRuntimeFact.TechnicalFailure(
                    HeartRateTechnicalFailure.SERVICE_DISCOVERY_FAILED,
                    attempt.source
                )
            )
            return
        }
        val service = try {
            gatt.getService(HEART_RATE_SERVICE_UUID)
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(attempt.source))
            return
        }
        if (service == null) {
            failSubscription(attempt, HeartRateTechnicalFailure.SERVICE_DISCOVERY_FAILED)
            return
        }
        subscribeMeasurement(attempt, gatt, service)
    }

    private fun subscribeMeasurement(
        attempt: ActiveAttempt,
        gatt: BluetoothGatt,
        service: BluetoothGattService
    ) {
        if (!attemptMatches(attempt, gatt)) return
        val characteristic = try {
            service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(attempt.source))
            return
        }
        if (characteristic == null) {
            failSubscription(attempt, HeartRateTechnicalFailure.SERVICE_DISCOVERY_FAILED)
            return
        }
        val canNotify = characteristic.properties and
            BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        val canIndicate = characteristic.properties and
            BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        if (!canNotify && !canIndicate) {
            failSubscription(attempt, HeartRateTechnicalFailure.CCCD_FAILED)
            return
        }
        val descriptor = try {
            characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(attempt.source))
            return
        }
        if (descriptor == null) {
            failSubscription(attempt, HeartRateTechnicalFailure.CCCD_FAILED)
            return
        }
        val notificationEnabled = try {
            gatt.setCharacteristicNotification(characteristic, true)
        } catch (_: SecurityException) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(attempt.source))
            return
        }
        if (!notificationEnabled) {
            failSubscription(attempt, HeartRateTechnicalFailure.CCCD_FAILED)
            return
        }

        attempt.phase = AttemptPhase.SUBSCRIBING
        attempt.measurement = characteristic
        attempt.cccd = descriptor
        val descriptorValue = if (canNotify) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }
        val writeStarted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = try {
                gatt.writeDescriptor(descriptor, descriptorValue)
            } catch (_: SecurityException) {
                cleanup(HeartRateRuntimeFact.PermissionRequired(attempt.source))
                return
            }
            result == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = descriptorValue
            @Suppress("DEPRECATION")
            try {
                gatt.writeDescriptor(descriptor)
            } catch (_: SecurityException) {
                cleanup(HeartRateRuntimeFact.PermissionRequired(attempt.source))
                return
            }
        }
        if (!writeStarted && attemptMatches(attempt, gatt) &&
            attempt.phase == AttemptPhase.SUBSCRIBING
        ) {
            failSubscription(attempt, HeartRateTechnicalFailure.CCCD_FAILED)
        }
    }

    private fun handleDescriptorWrite(
        attemptId: Long,
        ownerGeneration: Long,
        targetIdentifier: String,
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        checkMainThread()
        val attempt = acceptBoundCallback(
            attemptId,
            ownerGeneration,
            targetIdentifier,
            gatt
        ) ?: return
        if (attempt.phase != AttemptPhase.SUBSCRIBING) return
        if (descriptor !== attempt.cccd) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            failSubscription(attempt, HeartRateTechnicalFailure.CCCD_FAILED)
            return
        }
        val nowElapsed = SystemClock.elapsedRealtime()
        attempt.phase = AttemptPhase.WAITING_FIRST_DATA
        attempt.timeline = HeartRateFreshnessTimeline().notifyEnabled(nowElapsed)
        publish(HeartRateRuntimeFact.WaitingFirstData(attempt.source))
        publishRecovery(
            HeartRateRecoveryState(
                phase = HeartRateRecoveryPhase.CONNECTING_OR_CONNECTED,
                targetIdentifier = attempt.targetIdentifier
            )
        )
        scheduleFreshness(attempt, freshnessConfig.firstSampleWaitingBoundaryMs)
    }

    private fun handleCharacteristicChanged(
        attemptId: Long,
        ownerGeneration: Long,
        targetIdentifier: String,
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        checkMainThread()
        val attempt = acceptBoundCallback(
            attemptId,
            ownerGeneration,
            targetIdentifier,
            gatt
        ) ?: return
        if (characteristic !== attempt.measurement) return
        if (attempt.phase != AttemptPhase.WAITING_FIRST_DATA &&
            attempt.phase != AttemptPhase.LIVE &&
            attempt.phase != AttemptPhase.DATA_INTERRUPTED
        ) {
            return
        }
        val nowElapsed = SystemClock.elapsedRealtime()
        val measurement = HeartRateMeasurementParser.parse(value)
        if (measurement == null || measurement.bpm <= 0) {
            attempt.timeline = attempt.timeline.malformedSample()
            return
        }
        attempt.timeline = attempt.timeline.validSample(
            atElapsedMs = nowElapsed,
            bpm = measurement.bpm,
            measuredAt = Instant.now().toString()
        )
        attempt.phase = AttemptPhase.LIVE
        publish(freshnessPolicy.evaluate(nowElapsed, attempt.timeline).toRuntimeFact(attempt.source))
        emitObservation(HeartRateObservationPayload.ValidMeasurement(measurement.bpm))
        publishRecovery(
            HeartRateRecoveryState(
                phase = HeartRateRecoveryPhase.CONNECTING_OR_CONNECTED,
                targetIdentifier = attempt.targetIdentifier
            )
        )
        scheduleFreshness(attempt, freshnessConfig.liveFreshnessBoundaryMs)
    }

    private fun scheduleFreshness(attempt: ActiveAttempt, delayMillis: Long) {
        cancelFreshness()
        val gatt = attempt.gatt ?: return
        val runnable = Runnable {
            handleFreshnessDeadline(
                attempt.id,
                attempt.ownerGeneration,
                attempt.targetIdentifier,
                gatt
            )
        }
        freshnessRunnable = runnable
        mainHandler.postDelayed(runnable, delayMillis)
    }

    private fun handleFreshnessDeadline(
        attemptId: Long,
        ownerGeneration: Long,
        targetIdentifier: String,
        gatt: BluetoothGatt
    ) {
        checkMainThread()
        val attempt = acceptBoundCallback(
            attemptId,
            ownerGeneration,
            targetIdentifier,
            gatt
        ) ?: return
        freshnessRunnable = null
        val decision = freshnessPolicy.evaluate(SystemClock.elapsedRealtime(), attempt.timeline)
        publish(decision.toRuntimeFact(attempt.source))
        if (decision.kind == HeartRateFreshnessKind.DATA_INTERRUPTED) {
            attempt.phase = AttemptPhase.DATA_INTERRUPTED
        }
    }

    private fun failSubscription(
        attempt: ActiveAttempt,
        failure: HeartRateTechnicalFailure
    ) {
        cleanup(HeartRateRuntimeFact.TechnicalFailure(failure, attempt.source))
    }

    /**
     * Narrow attempt/raw-GATT identity harness. It owns no platform operation and accepts no
     * arbitrary business lambda: callback ID, generation, target, and raw identity are compared
     * directly here before any callback can mutate an attempt.
     */
    private fun bindCallbackGatt(
        attemptId: Long,
        ownerGeneration: Long,
        targetIdentifier: String,
        gatt: BluetoothGatt
    ): ActiveAttempt? {
        val attempt = activeAttempt
        if (attempt == null || attempt.id != attemptId ||
            attempt.ownerGeneration != ownerGeneration ||
            attempt.targetIdentifier != targetIdentifier
        ) {
            rejectGattUnlessCurrent(gatt)
            return null
        }
        val bound = attempt.gatt
        if (bound == null) {
            attempt.gatt = gatt
            return attempt
        }
        if (bound !== gatt) {
            rejectGattUnlessCurrent(gatt)
            return null
        }
        return attempt
    }

    private fun acceptBoundCallback(
        attemptId: Long,
        ownerGeneration: Long,
        targetIdentifier: String,
        gatt: BluetoothGatt
    ): ActiveAttempt? {
        val attempt = activeAttempt
        if (attempt == null || attempt.id != attemptId ||
            attempt.ownerGeneration != ownerGeneration ||
            attempt.targetIdentifier != targetIdentifier ||
            attempt.gatt !== gatt
        ) {
            rejectGattUnlessCurrent(gatt)
            return null
        }
        return attempt
    }

    private fun bindReturnedGatt(
        attemptId: Long,
        ownerGeneration: Long,
        targetIdentifier: String,
        returnedGatt: BluetoothGatt
    ) {
        checkMainThread()
        val attempt = activeAttempt
        if (attempt == null || attempt.id != attemptId ||
            attempt.ownerGeneration != ownerGeneration ||
            attempt.targetIdentifier != targetIdentifier
        ) {
            closeRejectedGatt(returnedGatt)
            return
        }
        val bound = attempt.gatt
        if (bound == null) {
            attempt.gatt = returnedGatt
        } else if (bound !== returnedGatt) {
            closeRejectedGatt(returnedGatt)
        }
    }

    private fun rejectGattUnlessCurrent(gatt: BluetoothGatt) {
        if (activeAttempt?.gatt === gatt) return
        closeRejectedGatt(gatt)
    }

    private fun closeRejectedGatt(gatt: BluetoothGatt) {
        if (cleanupInProgress && cleanupGatt === gatt) return
        try {
            gatt.close()
        } catch (_: SecurityException) {
            if (cleanupInProgress) {
                cleanupObservedPermissionLoss = true
            } else if (!ownerClosed && (activeAttempt != null || activeScan != null)) {
                cleanup(HeartRateRuntimeFact.PermissionRequired(currentSource()))
            }
        }
    }

    /** Invalidates identities and references before touching any detached platform resource. */
    private fun cleanup(
        requestedFact: HeartRateRuntimeFact,
        permissionLossOverridesFact: Boolean = true,
        requestedCause: HeartRateObservationCause = causeForFact(requestedFact)
    ) {
        checkMainThread()
        if (requestedFact is HeartRateRuntimeFact.PermissionRequired ||
            requestedFact is HeartRateRuntimeFact.BluetoothOff
        ) {
            operationEligible = false
        }
        if (requestedFact is HeartRateRuntimeFact.PermissionRequired) {
            recoveryEligibilityInput = recoveryEligibilityInput.copy(permissionGranted = false)
        } else if (requestedFact is HeartRateRuntimeFact.BluetoothOff) {
            recoveryEligibilityInput = recoveryEligibilityInput.copy(bluetoothEnabled = false)
        }
        scanGeneration += 1L
        attemptSequence += 1L

        val detachedScan = activeScan
        val detachedAttempt = activeAttempt
        activeScan = null
        activeAttempt = null
        candidateDevices.clear()
        mutableCandidates.value = emptyList()

        cancelScanTimeout()
        cancelFreshness()
        cancelRecovery()

        cleanupInProgress = true
        cleanupGatt = detachedAttempt?.gatt
        cleanupObservedPermissionLoss = false
        if (detachedScan != null) {
            try {
                detachedScan.scanner.stopScan(detachedScan.callback)
            } catch (_: SecurityException) {
                cleanupObservedPermissionLoss = true
            } catch (error: IllegalStateException) {
                if (requestedFact !is HeartRateRuntimeFact.BluetoothOff) throw error
            }
        }
        val detachedGatt = detachedAttempt?.gatt
        if (detachedGatt != null) {
            try {
                detachedGatt.disconnect()
            } catch (_: SecurityException) {
                cleanupObservedPermissionLoss = true
            }
            try {
                detachedGatt.close()
            } catch (_: SecurityException) {
                cleanupObservedPermissionLoss = true
            }
        }
        cleanupGatt = null
        cleanupInProgress = false

        mutableScanState.value = BleHeartRateScanState.idle("BLE runtime resources absent")
        val finalFact = if (cleanupObservedPermissionLoss && permissionLossOverridesFact) {
            HeartRateRuntimeFact.PermissionRequired(sourceFrom(requestedFact))
        } else {
            requestedFact
        }
        publish(finalFact, if (cleanupObservedPermissionLoss && permissionLossOverridesFact) {
            causeForFact(finalFact)
        } else {
            requestedCause
        })
        if (!ownerClosed) {
            val decision = evaluateHeartRateRecoveryEligibility(recoveryEligibilityInput)
            if (decision.eligible) {
                scheduleRecovery(
                    delayMillis = recoveryIntervalMillis,
                    pendingPhase = HeartRateRecoveryPhase.WAITING_NEXT_WINDOW
                )
            } else {
                publishRecovery(
                    HeartRateRecoveryState.disarmed(requireNotNull(decision.stopReason))
                )
            }
        }
    }

    private fun detachAndStopActiveScan(): Boolean {
        scanGeneration += 1L
        val detached = activeScan
        activeScan = null
        cancelScanTimeout()
        if (detached != null) {
            try {
                detached.scanner.stopScan(detached.callback)
            } catch (_: SecurityException) {
                cleanup(HeartRateRuntimeFact.PermissionRequired(currentSource()))
                return false
            } catch (error: IllegalStateException) {
                val cause = bluetoothUnavailabilityCause() ?: throw error
                cleanup(
                    requestedFact = HeartRateRuntimeFact.BluetoothOff(currentSource()),
                    permissionLossOverridesFact = false,
                    requestedCause = cause
                )
                return false
            }
        }
        return true
    }

    private fun detachAndCloseActiveAttempt(): Boolean {
        attemptSequence += 1L
        val detached = activeAttempt
        activeAttempt = null
        cancelFreshness()
        val gatt = detached?.gatt ?: return true
        var permissionLost = false
        cleanupInProgress = true
        cleanupGatt = gatt
        try {
            gatt.disconnect()
        } catch (_: SecurityException) {
            permissionLost = true
        }
        try {
            gatt.close()
        } catch (_: SecurityException) {
            permissionLost = true
        }
        cleanupGatt = null
        cleanupInProgress = false
        if (permissionLost) {
            cleanup(HeartRateRuntimeFact.PermissionRequired(detached.source))
            return false
        }
        return true
    }

    private fun cancelScanTimeout() {
        scanTimeoutRunnable?.let(mainHandler::removeCallbacks)
        scanTimeoutRunnable = null
    }

    private fun cancelFreshness() {
        freshnessRunnable?.let(mainHandler::removeCallbacks)
        freshnessRunnable = null
    }

    private fun cancelRecovery() {
        recoveryRunnable?.let(mainHandler::removeCallbacks)
        recoveryRunnable = null
    }

    private fun bluetoothAdapterOrPublishFailure(): android.bluetooth.BluetoothAdapter? {
        val manager = appContext.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter
        if (adapter == null) {
            cleanup(
                HeartRateRuntimeFact.TechnicalFailure(
                    HeartRateTechnicalFailure.PLATFORM_UNAVAILABLE,
                    currentSource()
                )
            )
        }
        return adapter
    }

    private fun bluetoothUnavailabilityCause(): HeartRateObservationCause? {
        val adapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return HeartRateObservationCause.PLATFORM_UNAVAILABLE
        return try {
            if (adapter.isEnabled) null else HeartRateObservationCause.BLUETOOTH_OFF
        } catch (_: SecurityException) {
            null
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val granted = BleHeartRatePermissionPlanner.requiredPermissions().all {
            appContext.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) observedPermissionGrant = true
        return granted
    }

    private fun heartRateServiceFilters(): List<ScanFilter> = listOf(
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID))
            .build()
    )

    private fun scanMatches(generation: Long, callback: ScanCallback): Boolean {
        val scan = activeScan
        return scan != null && scan.generation == generation && scan.callback === callback
    }

    private fun attemptMatches(attempt: ActiveAttempt, gatt: BluetoothGatt): Boolean {
        return activeAttempt === attempt && attempt.gatt === gatt
    }

    private fun nextScanGeneration(): Long {
        scanGeneration += 1L
        return scanGeneration
    }

    private fun nextAttemptId(): Long {
        attemptSequence += 1L
        return attemptSequence
    }

    private fun sourceForIdentifier(identifier: String): HeartRateSourceHint? {
        val candidate = mutableCandidates.value.firstOrNull { it.identifier == identifier }
        return candidate?.let { HeartRateSourceHint(it.identifier, it.displayName) }
    }

    private fun currentSource(): HeartRateSourceHint? = activeAttempt?.source

    private fun sourceFrom(fact: HeartRateRuntimeFact): HeartRateSourceHint? = when (fact) {
        HeartRateRuntimeFact.Disabled -> null
        is HeartRateRuntimeFact.PermissionRequired -> fact.source
        is HeartRateRuntimeFact.BluetoothOff -> fact.source
        is HeartRateRuntimeFact.NotConnected -> fact.source
        is HeartRateRuntimeFact.Scanning -> fact.source
        is HeartRateRuntimeFact.Connecting -> fact.source
        is HeartRateRuntimeFact.WaitingFirstData -> fact.source
        is HeartRateRuntimeFact.Live -> fact.source
        is HeartRateRuntimeFact.DataInterrupted -> fact.source
        is HeartRateRuntimeFact.LinkDisconnected -> fact.source
        is HeartRateRuntimeFact.TechnicalFailure -> fact.source
        is HeartRateRuntimeFact.IntentionalStop -> fact.source
    }

    private fun publish(
        fact: HeartRateRuntimeFact,
        cause: HeartRateObservationCause = causeForFact(fact)
    ) {
        checkMainThread()
        val state = fact.toHeartRateState()
        if (mutableHeartRateState.value != state) {
            mutableHeartRateState.value = state
        }
        publishObservationCause(cause)
    }

    private fun causeForFact(fact: HeartRateRuntimeFact): HeartRateObservationCause = when (fact) {
        HeartRateRuntimeFact.Disabled -> HeartRateObservationCause.NOT_OBSERVING
        is HeartRateRuntimeFact.PermissionRequired -> if (observedPermissionGrant) {
            HeartRateObservationCause.PERMISSION_REVOKED
        } else {
            HeartRateObservationCause.PERMISSION_MISSING
        }
        is HeartRateRuntimeFact.BluetoothOff -> HeartRateObservationCause.BLUETOOTH_OFF
        is HeartRateRuntimeFact.NotConnected -> HeartRateObservationCause.DISCONNECTED
        is HeartRateRuntimeFact.Scanning -> HeartRateObservationCause.INITIAL_SEARCH
        is HeartRateRuntimeFact.Connecting -> if (activeAttempt?.origin == ScanOrigin.RECOVERY) {
            HeartRateObservationCause.RECOVERY_CONNECT
        } else {
            HeartRateObservationCause.INITIAL_CONNECT
        }
        is HeartRateRuntimeFact.WaitingFirstData -> if (activeAttempt?.origin == ScanOrigin.RECOVERY) {
            HeartRateObservationCause.RECOVERY_WAIT
        } else {
            HeartRateObservationCause.INITIAL_WAIT
        }
        is HeartRateRuntimeFact.Live -> HeartRateObservationCause.LIVE
        is HeartRateRuntimeFact.DataInterrupted -> if (activeAttempt?.timeline?.lastValidSampleElapsedMs == null) {
            HeartRateObservationCause.FIRST_SAMPLE_TIMEOUT
        } else {
            HeartRateObservationCause.SAMPLE_STALE_TIMEOUT
        }
        is HeartRateRuntimeFact.LinkDisconnected -> HeartRateObservationCause.UNEXPECTED_DISCONNECT
        is HeartRateRuntimeFact.IntentionalStop -> HeartRateObservationCause.NOT_OBSERVING
        is HeartRateRuntimeFact.TechnicalFailure -> when (fact.reason) {
            HeartRateTechnicalFailure.PLATFORM_UNAVAILABLE -> HeartRateObservationCause.PLATFORM_UNAVAILABLE
            HeartRateTechnicalFailure.PLATFORM_FAILURE -> HeartRateObservationCause.PLATFORM_FAILURE
            HeartRateTechnicalFailure.CONNECT_FAILED -> HeartRateObservationCause.DISCONNECTED
            HeartRateTechnicalFailure.SERVICE_DISCOVERY_FAILED,
            HeartRateTechnicalFailure.CCCD_FAILED -> HeartRateObservationCause.MEASUREMENT_STREAM_UNAVAILABLE
            HeartRateTechnicalFailure.INVALID_MONOTONIC_TIME,
            HeartRateTechnicalFailure.INVALID_FACT -> error("Invalid internal heart-rate fact")
        }
    }

    private fun publishObservationCause(cause: HeartRateObservationCause) {
        if (observationCause == cause) return
        observationCause = cause
        emitObservation(HeartRateObservationPayload.RuntimeTransition(cause))
    }

    private fun emitObservation(payload: HeartRateObservationPayload) {
        val binding = observationBinding ?: return
        val receipt = Math.incrementExact(observationReceipt)
        observationReceipt = receipt
        requireNotNull(observationSink).invoke(
            HeartRateObservation(binding.bindingId, receipt, SystemClock.elapsedRealtime(), payload)
        )
    }

    private fun publishRecovery(state: HeartRateRecoveryState) {
        checkMainThread()
        if (mutableRecoveryState.value != state) {
            mutableRecoveryState.value = state
        }
        // Scheduling is not a replacement for an active stream or a final platform stop fact.
        if ((state.phase == HeartRateRecoveryPhase.WAITING_NEXT_WINDOW ||
                state.phase == HeartRateRecoveryPhase.WINDOW_MISSED_ARMED) &&
            activeAttempt == null && activeScan == null &&
            observationCause != HeartRateObservationCause.PERMISSION_MISSING &&
            observationCause != HeartRateObservationCause.PERMISSION_REVOKED &&
            observationCause != HeartRateObservationCause.BLUETOOTH_OFF &&
            observationCause != HeartRateObservationCause.PLATFORM_UNAVAILABLE
        ) {
            publishObservationCause(HeartRateObservationCause.RECOVERY_WAITING)
        }
    }

    private fun checkMainThread() {
        check(Looper.myLooper() === mainHandler.looper) {
            "HeartRateRuntimeOwner state transition must run on its main Handler"
        }
    }

    private inner class RuntimeScanCallback(
        private val generation: Long
    ) : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (Looper.myLooper() === mainHandler.looper) {
                handleScanResult(generation, this, result)
            } else {
                mainHandler.post { handleScanResult(generation, this, result) }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            val captured = results.toList()
            if (Looper.myLooper() === mainHandler.looper) {
                captured.forEach { handleScanResult(generation, this, it) }
            } else {
                mainHandler.post {
                    captured.forEach { handleScanResult(generation, this, it) }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            if (Looper.myLooper() === mainHandler.looper) {
                handleScanFailure(generation, this, errorCode)
            } else {
                mainHandler.post { handleScanFailure(generation, this, errorCode) }
            }
        }
    }

    /** Callback captures immutable attempt identity; raw GATT binding stays in the owner above. */
    private inner class AttemptGattCallback(
        private val attemptId: Long,
        private val ownerGeneration: Long,
        private val targetIdentifier: String
    ) : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (Looper.myLooper() === mainHandler.looper) {
                handleConnectionStateChange(
                    attemptId,
                    ownerGeneration,
                    targetIdentifier,
                    gatt,
                    status,
                    newState
                )
            } else {
                mainHandler.post {
                    handleConnectionStateChange(
                        attemptId,
                        ownerGeneration,
                        targetIdentifier,
                        gatt,
                        status,
                        newState
                    )
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (Looper.myLooper() === mainHandler.looper) {
                handleServicesDiscovered(
                    attemptId,
                    ownerGeneration,
                    targetIdentifier,
                    gatt,
                    status
                )
            } else {
                mainHandler.post {
                    handleServicesDiscovered(
                        attemptId,
                        ownerGeneration,
                        targetIdentifier,
                        gatt,
                        status
                    )
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (Looper.myLooper() === mainHandler.looper) {
                handleDescriptorWrite(
                    attemptId,
                    ownerGeneration,
                    targetIdentifier,
                    gatt,
                    descriptor,
                    status
                )
            } else {
                mainHandler.post {
                    handleDescriptorWrite(
                        attemptId,
                        ownerGeneration,
                        targetIdentifier,
                        gatt,
                        descriptor,
                        status
                    )
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
            val captured = value.copyOf()
            if (Looper.myLooper() === mainHandler.looper) {
                handleCharacteristicChanged(
                    attemptId,
                    ownerGeneration,
                    targetIdentifier,
                    gatt,
                    characteristic,
                    captured
                )
            } else {
                mainHandler.post {
                    handleCharacteristicChanged(
                        attemptId,
                        ownerGeneration,
                        targetIdentifier,
                        gatt,
                        characteristic,
                        captured
                    )
                }
            }
        }

        @Deprecated("Android 13 adds the value overload")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            val captured = characteristic.value?.copyOf() ?: byteArrayOf()
            if (Looper.myLooper() === mainHandler.looper) {
                handleCharacteristicChanged(
                    attemptId,
                    ownerGeneration,
                    targetIdentifier,
                    gatt,
                    characteristic,
                    captured
                )
            } else {
                mainHandler.post {
                    handleCharacteristicChanged(
                        attemptId,
                        ownerGeneration,
                        targetIdentifier,
                        gatt,
                        characteristic,
                        captured
                    )
                }
            }
        }
    }

    private data class ActiveScan(
        val generation: Long,
        val scanner: BluetoothLeScanner,
        val callback: ScanCallback,
        val origin: ScanOrigin,
        val recoveryTargetIdentifier: String?
    )

    private enum class ScanOrigin {
        MANUAL,
        RECOVERY
    }

    private data class ActiveAttempt(
        val id: Long,
        val ownerGeneration: Long,
        val targetIdentifier: String,
        val source: HeartRateSourceHint,
        val origin: ScanOrigin,
        var phase: AttemptPhase,
        var callback: BluetoothGattCallback? = null,
        var gatt: BluetoothGatt? = null,
        var measurement: BluetoothGattCharacteristic? = null,
        var cccd: BluetoothGattDescriptor? = null,
        var timeline: HeartRateFreshnessTimeline = HeartRateFreshnessTimeline()
    )

    private enum class AttemptPhase {
        CONNECTING,
        DISCOVERING,
        SUBSCRIBING,
        WAITING_FIRST_DATA,
        LIVE,
        DATA_INTERRUPTED
    }

    private companion object {
        const val DEFAULT_SCAN_WINDOW_MILLIS = 12_000L
        const val DEFAULT_RECOVERY_INTERVAL_MILLIS = 5_000L
        const val UNKNOWN_DEVICE_NAME = "(unknown BLE device)"
        val HEART_RATE_SERVICE_UUID: UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
