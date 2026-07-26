package com.liujyks.trainflow.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.Collections
import java.util.IdentityHashMap

internal enum class ProcessVisibilityFact {
    UNKNOWN,
    VISIBLE,
    BACKGROUND,
    CONFIGURATION_TRANSITION
}

/**
 * Identity-based reducer. It publishes facts only; heart-rate cleanup and recovery policy remain
 * owned by [TrainFlowApplication].
 */
internal class ProcessVisibilityReducer(
    private val publish: (ProcessVisibilityFact) -> Unit
) {
    private val startedActivities = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
    private var configurationGeneration = 0L
    private var currentFact = ProcessVisibilityFact.UNKNOWN

    fun activityStarted(identity: Any) {
        if (!startedActivities.add(identity)) {
            emit(ProcessVisibilityFact.UNKNOWN)
            return
        }
        configurationGeneration += 1
        emit(ProcessVisibilityFact.VISIBLE)
    }

    fun activityStopped(identity: Any, changingConfigurations: Boolean): Long? {
        if (!startedActivities.remove(identity)) {
            configurationGeneration += 1
            emit(ProcessVisibilityFact.UNKNOWN)
            return null
        }
        if (startedActivities.isNotEmpty()) return null
        return if (changingConfigurations) {
            val generation = ++configurationGeneration
            emit(ProcessVisibilityFact.CONFIGURATION_TRANSITION)
            generation
        } else {
            configurationGeneration += 1
            emit(ProcessVisibilityFact.BACKGROUND)
            null
        }
    }

    fun configurationTimeout(generation: Long) {
        if (
            generation == configurationGeneration &&
            startedActivities.isEmpty() &&
            currentFact == ProcessVisibilityFact.CONFIGURATION_TRANSITION
        ) {
            emit(ProcessVisibilityFact.UNKNOWN)
        }
    }

    private fun emit(fact: ProcessVisibilityFact) {
        if (currentFact != fact) {
            currentFact = fact
            publish(fact)
        }
    }
}

internal class ProcessVisibilityTracker(
    application: Application,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val configurationTimeoutMillis: Long = CONFIGURATION_TIMEOUT_MILLIS,
    onFact: (ProcessVisibilityFact) -> Unit
) : Application.ActivityLifecycleCallbacks {
    private val reducer = ProcessVisibilityReducer(onFact)

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        reducer.activityStarted(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        val generation = reducer.activityStopped(
            identity = activity,
            changingConfigurations = activity.isChangingConfigurations
        )
        if (generation != null) {
            mainHandler.postDelayed(
                { reducer.configurationTimeout(generation) },
                configurationTimeoutMillis
            )
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        const val CONFIGURATION_TIMEOUT_MILLIS = 1_000L
    }
}
