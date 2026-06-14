package com.liujyks.trainflow.ui.theme

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberTrainFlowReduceMotion(): Boolean {
    val context = LocalContext.current
    val appContext = context.applicationContext
    var reduceMotion by remember(appContext) {
        mutableStateOf(appContext.readTrainFlowReduceMotionFromSystemSettings())
    }

    DisposableEffect(appContext) {
        val resolver = appContext.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduceMotion = appContext.readTrainFlowReduceMotionFromSystemSettings()
            }
        }

        trainFlowAnimationScaleSettingUris().forEach { uri ->
            resolver.registerContentObserver(uri, false, observer)
        }

        onDispose {
            resolver.unregisterContentObserver(observer)
        }
    }

    return reduceMotion
}

internal fun Context.readTrainFlowReduceMotionFromSystemSettings(): Boolean {
    return shouldReduceMotionForAnimationScales(
        animatorDurationScale = contentResolver.readAnimationScale(
            Settings.Global.ANIMATOR_DURATION_SCALE
        ),
        transitionAnimationScale = contentResolver.readAnimationScale(
            Settings.Global.TRANSITION_ANIMATION_SCALE
        ),
        windowAnimationScale = contentResolver.readAnimationScale(
            Settings.Global.WINDOW_ANIMATION_SCALE
        )
    )
}

internal fun shouldReduceMotionForAnimationScales(
    animatorDurationScale: Float,
    transitionAnimationScale: Float,
    windowAnimationScale: Float
): Boolean {
    return listOf(
        animatorDurationScale,
        transitionAnimationScale,
        windowAnimationScale
    ).any { scale -> scale.isFinite() && scale <= 0f }
}

private fun ContentResolver.readAnimationScale(name: String): Float {
    return Settings.Global.getFloat(this, name, DefaultAnimationScale)
}

private fun trainFlowAnimationScaleSettingUris(): List<Uri> {
    return listOf(
        Settings.Global.ANIMATOR_DURATION_SCALE,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        Settings.Global.WINDOW_ANIMATION_SCALE
    ).map(Settings.Global::getUriFor)
}

private const val DefaultAnimationScale = 1f
