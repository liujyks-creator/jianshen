package com.liujyks.trainflow.app

import android.app.Application
import com.liujyks.trainflow.core.health.HeartRateRuntimeAction
import com.liujyks.trainflow.core.health.HeartRateRuntimeOwner

class TrainFlowApplication : Application() {
    internal lateinit var heartRateRuntimeOwner: HeartRateRuntimeOwner
        private set
    internal lateinit var heartRateApplicationPolicy: HeartRateApplicationPolicy
        private set
    internal lateinit var processVisibilityTracker: ProcessVisibilityTracker
        private set

    override fun onCreate() {
        super.onCreate()
        heartRateRuntimeOwner = HeartRateRuntimeOwner(this)
        heartRateApplicationPolicy = HeartRateApplicationPolicy(heartRateRuntimeOwner::submit)
        processVisibilityTracker = ProcessVisibilityTracker(
            onFact = heartRateApplicationPolicy::onVisibilityChanged
        )
        registerActivityLifecycleCallbacks(processVisibilityTracker)
    }
}

/**
 * Application policy is the only layer that turns visibility and product eligibility into owner
 * actions. Before E17-9, background or uncertain visibility always cleans up, even during training.
 */
internal class HeartRateApplicationPolicy(
    private val submitAction: (HeartRateRuntimeAction) -> Unit
) {
    private var displayEnabled = false
    private var permissionsGranted = false
    private var bluetoothAvailable = false
    private var trainingActive = false
    private var visibilityFact: ProcessVisibilityFact = ProcessVisibilityFact.Unknown
    private var foregroundEligibilityRestorePending = false

    fun onEligibilityChanged(
        displayEnabled: Boolean,
        permissionsGranted: Boolean,
        bluetoothAvailable: Boolean
    ) {
        this.displayEnabled = displayEnabled
        this.permissionsGranted = permissionsGranted
        this.bluetoothAvailable = bluetoothAvailable
        val action = heartRateRuntimeEligibilityAction(
            displayEnabled = displayEnabled,
            permissionsGranted = permissionsGranted,
            bluetoothAvailable = bluetoothAvailable
        )
        if (
            action != HeartRateRuntimeAction.Enable ||
            !foregroundEligibilityRestorePending
        ) {
            submitAction(action)
        }
    }

    fun onTrainingActiveChanged(active: Boolean) {
        trainingActive = active
    }

    fun onVisibilityChanged(fact: ProcessVisibilityFact) {
        visibilityFact = fact
        if (
            fact == ProcessVisibilityFact.BackgroundConfirmed ||
            fact == ProcessVisibilityFact.Unknown
        ) {
            foregroundEligibilityRestorePending = true
            submitAction(HeartRateRuntimeAction.BackgroundCleanup)
        } else if (
            fact == ProcessVisibilityFact.ForegroundConfirmed &&
            foregroundEligibilityRestorePending
        ) {
            foregroundEligibilityRestorePending = false
            if (displayEnabled && permissionsGranted && bluetoothAvailable) {
                submitAction(HeartRateRuntimeAction.Enable)
            }
        }
    }

    fun canRetainAttemptAtTrainingTerminal(): Boolean {
        val visibleOrControlledTransition =
            visibilityFact == ProcessVisibilityFact.ForegroundConfirmed ||
                visibilityFact is ProcessVisibilityFact.ConfigurationTransition
        return displayEnabled &&
            permissionsGranted &&
            bluetoothAvailable &&
            visibleOrControlledTransition
    }
}

internal fun heartRateRuntimeEligibilityAction(
    displayEnabled: Boolean,
    permissionsGranted: Boolean,
    bluetoothAvailable: Boolean
): HeartRateRuntimeAction = when {
    !displayEnabled -> HeartRateRuntimeAction.Disable
    !permissionsGranted -> HeartRateRuntimeAction.PermissionLost
    !bluetoothAvailable -> HeartRateRuntimeAction.BluetoothOff
    else -> HeartRateRuntimeAction.Enable
}
