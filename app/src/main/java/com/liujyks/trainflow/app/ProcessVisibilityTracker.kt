package com.liujyks.trainflow.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.Collections
import java.util.IdentityHashMap

internal sealed interface ProcessVisibilityFact {
    data object ForegroundConfirmed : ProcessVisibilityFact
    data object BackgroundConfirmed : ProcessVisibilityFact
    data class ConfigurationTransition(val generation: Long) : ProcessVisibilityFact
    data object Unknown : ProcessVisibilityFact
}

/**
 * Publishes process-visibility facts only. Heart-rate cleanup and training/FGS eligibility belong
 * to the Application policy that consumes these facts.
 */
internal class ProcessVisibilityTracker(
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val configurationTransitionTimeoutMillis: Long =
        DEFAULT_CONFIGURATION_TRANSITION_TIMEOUT_MILLIS,
    private val onFact: (ProcessVisibilityFact) -> Unit
) : Application.ActivityLifecycleCallbacks {
    private val startedActivities = identitySet<Activity>()
    private val resumedActivities = identitySet<Activity>()
    private var transitionSequence = 0L
    private var activeTransition: ConfigurationTransitionState? = null
    private var transitionTimeout: Runnable? = null
    private var uncertainAfterFailure = false

    var currentFact: ProcessVisibilityFact = ProcessVisibilityFact.Unknown
        private set

    override fun onActivityStarted(activity: Activity) {
        val observedTransitionGeneration = activeTransition?.generation
        mainHandler.post {
            handleStarted(activity, observedTransitionGeneration)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        mainHandler.post {
            if (activity !in startedActivities || !resumedActivities.add(activity)) {
                failClosed()
            } else if (!uncertainAfterFailure) {
                publish(ProcessVisibilityFact.ForegroundConfirmed)
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        mainHandler.post {
            if (!resumedActivities.remove(activity)) {
                failClosed()
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        onActivityStopped(activity, activity.isChangingConfigurations)
    }

    internal fun onActivityStopped(activity: Activity, changingConfigurations: Boolean) {
        mainHandler.post {
            handleStopped(activity, changingConfigurations)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        mainHandler.post {
            val wasStarted = startedActivities.remove(activity)
            val wasResumed = resumedActivities.remove(activity)
            if (wasStarted || wasResumed) {
                failClosed()
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    private fun handleStarted(activity: Activity, observedTransitionGeneration: Long?) {
        checkMainThread()
        if (activity in startedActivities) {
            failClosed()
            return
        }

        val transition = activeTransition
        if (transition != null) {
            if (
                observedTransitionGeneration != null &&
                observedTransitionGeneration != transition.generation
            ) {
                failClosed()
                return
            }
            cancelTransitionTimeout()
            activeTransition = null
            startedActivities.add(activity)
            uncertainAfterFailure = false
            publish(ProcessVisibilityFact.ForegroundConfirmed)
            return
        }

        startedActivities.add(activity)
        if (uncertainAfterFailure || observedTransitionGeneration != null) {
            failClosed()
        } else {
            publish(ProcessVisibilityFact.ForegroundConfirmed)
        }
    }

    private fun handleStopped(activity: Activity, changingConfigurations: Boolean) {
        checkMainThread()
        if (!startedActivities.remove(activity)) {
            failClosed()
            return
        }
        if (resumedActivities.remove(activity)) {
            failClosed()
            return
        }
        if (startedActivities.isNotEmpty()) {
            if (!uncertainAfterFailure) {
                publish(ProcessVisibilityFact.ForegroundConfirmed)
            }
            return
        }
        if (!changingConfigurations) {
            cancelActiveTransition()
            uncertainAfterFailure = false
            publish(ProcessVisibilityFact.BackgroundConfirmed)
            return
        }

        val transition = ConfigurationTransitionState(++transitionSequence)
        activeTransition = transition
        uncertainAfterFailure = false
        publish(ProcessVisibilityFact.ConfigurationTransition(transition.generation))
        scheduleTransitionTimeout(transition)
    }

    private fun scheduleTransitionTimeout(transition: ConfigurationTransitionState) {
        cancelTransitionTimeout()
        val timeout = Runnable {
            checkMainThread()
            if (activeTransition === transition) {
                activeTransition = null
                transitionTimeout = null
                uncertainAfterFailure = true
                publish(ProcessVisibilityFact.Unknown)
            }
        }
        transitionTimeout = timeout
        mainHandler.postDelayed(timeout, configurationTransitionTimeoutMillis)
    }

    private fun cancelActiveTransition() {
        activeTransition = null
        cancelTransitionTimeout()
    }

    private fun cancelTransitionTimeout() {
        transitionTimeout?.let(mainHandler::removeCallbacks)
        transitionTimeout = null
    }

    private fun failClosed() {
        checkMainThread()
        cancelActiveTransition()
        uncertainAfterFailure = true
        publish(ProcessVisibilityFact.Unknown)
    }

    private fun publish(fact: ProcessVisibilityFact) {
        checkMainThread()
        if (currentFact != fact) {
            currentFact = fact
            onFact(fact)
        }
    }

    private fun checkMainThread() {
        check(Looper.myLooper() === mainHandler.looper) {
            "Process visibility must be reduced on its main Handler"
        }
    }

    private data class ConfigurationTransitionState(val generation: Long)

    private companion object {
        const val DEFAULT_CONFIGURATION_TRANSITION_TIMEOUT_MILLIS = 1_000L

        fun <T : Any> identitySet(): MutableSet<T> =
            Collections.newSetFromMap(IdentityHashMap())
    }
}
