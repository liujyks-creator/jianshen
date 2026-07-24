package com.liujyks.trainflow.app

import android.app.Activity
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.health.HeartRateRuntimeAction
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(application = TrainFlowApplication::class, sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class TrainFlowApplicationHeartRateOwnerTest {
    @Test
    fun realConfigurationChangeCallbacksRestoreForegroundWithoutCleanupFact() {
        val application =
            ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        val controller = Robolectric.buildActivity(Activity::class.java)
            .create()
            .start()
            .resume()
        shadowOf(Looper.getMainLooper()).idle()

        controller.configurationChange()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            ProcessVisibilityFact.ForegroundConfirmed,
            application.processVisibilityTracker.currentFact
        )
        controller.pause().stop().destroy()
    }

    @Test
    fun mainActivityWaitsForPersistedPreferencesBeforeMountingEligibilitySideEffects() {
        val source = File(
            "src/main/java/com/liujyks/trainflow/app/MainActivity.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(source.contains("collectAsState(initial = null)"))
        assertTrue(source.contains("?: return@setContent"))
        assertFalse(source.contains("collectAsState(\n                initial = TrainFlowPreferences()"))
    }

    @Test
    fun applicationCreatesOneOwnerAndActivityRecreationDoesNotReplaceIt() {
        val application =
            ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        val owner = application.heartRateRuntimeOwner

        val oldActivity = Robolectric.buildActivity(Activity::class.java)
            .create()
            .start()
            .resume()
        oldActivity.pause().stop().destroy()
        val replacement = Robolectric.buildActivity(Activity::class.java)
            .create()
            .start()
            .resume()
        shadowOf(Looper.getMainLooper()).idle()

        assertSame(owner, application.heartRateRuntimeOwner)
        replacement.pause().stop().destroy()
    }

    @Test
    fun eligibilityMapsToTypedOwnerActionsWithoutStartingScanOrConnect() {
        val actions = mutableListOf<HeartRateRuntimeAction>()
        val policy = HeartRateApplicationPolicy(actions::add)

        policy.onEligibilityChanged(
            displayEnabled = false,
            permissionsGranted = true,
            bluetoothAvailable = true
        )
        policy.onEligibilityChanged(
            displayEnabled = true,
            permissionsGranted = false,
            bluetoothAvailable = true
        )
        policy.onEligibilityChanged(
            displayEnabled = true,
            permissionsGranted = true,
            bluetoothAvailable = false
        )
        policy.onEligibilityChanged(
            displayEnabled = true,
            permissionsGranted = true,
            bluetoothAvailable = true
        )

        assertTrue(
            actions == listOf(
                HeartRateRuntimeAction.Disable,
                HeartRateRuntimeAction.PermissionLost,
                HeartRateRuntimeAction.BluetoothOff,
                HeartRateRuntimeAction.Enable
            )
        )
        assertFalse(actions.any { it is HeartRateRuntimeAction.StartScan })
        assertFalse(actions.any { it is HeartRateRuntimeAction.Connect })
    }

    @Test
    fun backgroundAndUnknownAlwaysCleanupBeforeE17_9EvenDuringTraining() {
        val actions = mutableListOf<HeartRateRuntimeAction>()
        val policy = HeartRateApplicationPolicy(actions::add)
        policy.onEligibilityChanged(true, true, true)
        policy.onTrainingActiveChanged(true)

        policy.onVisibilityChanged(ProcessVisibilityFact.BackgroundConfirmed)
        policy.onVisibilityChanged(ProcessVisibilityFact.Unknown)

        assertTrue(
            actions.takeLast(2) == listOf(
                HeartRateRuntimeAction.BackgroundCleanup,
                HeartRateRuntimeAction.BackgroundCleanup
            )
        )
    }

    @Test
    fun terminalRetentionRequiresEligibleForegroundOrControlledTransition() {
        val policy = HeartRateApplicationPolicy {}
        policy.onEligibilityChanged(true, true, true)

        policy.onVisibilityChanged(ProcessVisibilityFact.ForegroundConfirmed)
        assertTrue(policy.canRetainAttemptAtTrainingTerminal())

        policy.onVisibilityChanged(ProcessVisibilityFact.ConfigurationTransition(7L))
        assertTrue(policy.canRetainAttemptAtTrainingTerminal())

        policy.onVisibilityChanged(ProcessVisibilityFact.BackgroundConfirmed)
        assertFalse(policy.canRetainAttemptAtTrainingTerminal())

        policy.onVisibilityChanged(ProcessVisibilityFact.Unknown)
        assertFalse(policy.canRetainAttemptAtTrainingTerminal())
    }
}
