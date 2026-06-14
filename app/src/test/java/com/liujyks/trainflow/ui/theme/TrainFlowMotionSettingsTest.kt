package com.liujyks.trainflow.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrainFlowMotionSettingsTest {
    @Test
    fun animationScaleZeroRequestsReducedMotion() {
        assertFalse(
            shouldReduceMotionForAnimationScales(
                animatorDurationScale = 1f,
                transitionAnimationScale = 1f,
                windowAnimationScale = 1f
            )
        )
        assertTrue(
            shouldReduceMotionForAnimationScales(
                animatorDurationScale = 0f,
                transitionAnimationScale = 1f,
                windowAnimationScale = 1f
            )
        )
        assertTrue(
            shouldReduceMotionForAnimationScales(
                animatorDurationScale = 1f,
                transitionAnimationScale = 0f,
                windowAnimationScale = 1f
            )
        )
        assertTrue(
            shouldReduceMotionForAnimationScales(
                animatorDurationScale = 1f,
                transitionAnimationScale = 1f,
                windowAnimationScale = 0f
            )
        )
    }

    @Test
    fun platformSettingsReadSystemAnimationScale() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = context.contentResolver

        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        Settings.Global.putFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        Settings.Global.putFloat(resolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f)
        assertFalse(context.readTrainFlowReduceMotionFromSystemSettings())

        Settings.Global.putFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        assertTrue(context.readTrainFlowReduceMotionFromSystemSettings())
    }

    @Test
    fun rootCompositionProvidesSystemReduceMotionToTrainFlowTheme() {
        val activitySource = File(
            "src/main/java/com/liujyks/trainflow/app/MainActivity.kt"
        ).readText(Charsets.UTF_8)
        val themeSource = File(
            "src/main/java/com/liujyks/trainflow/ui/theme/TrainFlowTheme.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(activitySource.contains("val reduceMotion = rememberTrainFlowReduceMotion()"))
        assertTrue(activitySource.contains("reduceMotion = reduceMotion"))
        assertTrue(themeSource.contains("val LocalTrainFlowReduceMotion = staticCompositionLocalOf { false }"))
        assertTrue(themeSource.contains("LocalTrainFlowReduceMotion provides reduceMotion"))
    }
}
