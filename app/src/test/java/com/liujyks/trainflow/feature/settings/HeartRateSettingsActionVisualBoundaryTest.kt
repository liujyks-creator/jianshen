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
        assertTrue(source.contains("断开心率设备"))
        assertTrue(source.contains("重新连接"))
        assertTrue(source.contains("清除已保存设备"))
        assertTrue(source.contains("关闭心率功能"))
        assertTrue(source.contains("个人最大心率"))
        assertTrue(source.contains("年龄"))
        assertTrue(source.contains("上限提醒"))
    }

    @Test
    fun productionShellDoesNotConstructLegacyBleOwners() {
        val shellSource = File(
            "src/main/java/com/liujyks/trainflow/ui/shell/official/TrainFlowApp.kt"
        ).readText()
        val applicationSource = File(
            "src/main/java/com/liujyks/trainflow/app/TrainFlowApplication.kt"
        ).readText()

        assertFalse(shellSource.contains("AndroidHeartRateDeviceScanner"))
        assertFalse(shellSource.contains("AndroidBleHeartRateProvider"))
        assertTrue(applicationSource.contains("HeartRateRuntimeOwner("))
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

    @Test
    fun strengthPreferenceCardDoesNotExposeInternalTimerModeTokens() {
        val source = File("src/main/java/com/liujyks/trainflow/feature/settings/SettingsRoute.kt").readText()

        assertFalse(source.contains("mode.contractValue"))
        assertTrue(source.contains("Text(mode.label)"))
    }
}
