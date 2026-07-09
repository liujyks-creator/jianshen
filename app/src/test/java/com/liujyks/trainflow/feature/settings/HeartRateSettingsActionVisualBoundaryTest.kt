package com.liujyks.trainflow.feature.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateSettingsActionVisualBoundaryTest {
    @Test
    fun heartRateDeviceActionsUseVisibleButtonsNotTextButtons() {
        val source = File("src/main/java/com/liujyks/trainflow/feature/settings/SettingsRoute.kt").readText()

        assertTrue(source.contains("import androidx.compose.material3.Button"))
        assertTrue(source.contains("import androidx.compose.material3.OutlinedButton"))
        assertFalse(source.contains("TextButton(onClick = onRequestHeartRateBlePermission)"))
        assertFalse(source.contains("TextButton(onClick = onPrepareHeartRateBlePermission)"))
        assertFalse(source.contains("TextButton(onClick = onStopHeartRateDeviceScan)"))
        assertFalse(source.contains("TextButton(\n                enabled = uiState.canStartScan"))
        assertFalse(source.contains("TextButton(onClick = { onSelectHeartRateDevice(device.identifier) })"))
    }

    @Test
    fun heartRateCapsuleUsesSinglePointerGestureForTapAndDrag() {
        val source = File(
            "src/main/java/com/liujyks/trainflow/ui/shell/official/HeartRateFloatingCapsule.kt"
        ).readText()

        assertFalse(source.contains("import androidx.compose.foundation.clickable"))
        assertFalse(source.contains(".clickable(onClick = onToggleExpanded)"))
        assertTrue(source.contains("awaitFirstDown(requireUnconsumed = false)"))
        assertTrue(source.contains("onTap = { expanded = !expanded }"))
    }
}
