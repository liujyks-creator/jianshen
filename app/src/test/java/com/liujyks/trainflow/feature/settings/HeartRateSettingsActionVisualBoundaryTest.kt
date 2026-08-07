package com.liujyks.trainflow.feature.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateSettingsActionVisualBoundaryTest {
    @Test
    fun heartRateDeviceActionsUseSingleCompactActionAreaAndSystemBluetoothSettings() {
        val source = File("src/main/java/com/liujyks/trainflow/feature/settings/SettingsRoute.kt").readText()
        val shellSource = File(
            "src/main/java/com/liujyks/trainflow/ui/shell/official/TrainFlowApp.kt"
        ).readText()
        val stateSource = File(
            "src/main/java/com/liujyks/trainflow/feature/settings/TrainingPreferencesUiState.kt"
        ).readText()

        assertTrue(source.contains("import androidx.compose.material3.Button"))
        assertTrue(source.contains("import androidx.compose.material3.OutlinedButton"))
        assertTrue(source.contains("title = \"启用心率功能\""))
        assertTrue(source.contains("SectionTitle(text = \"心率功能\")"))
        assertTrue(source.contains("SectionTitle(text = \"设备连接\")"))
        assertTrue(source.contains("SectionTitle(text = \"心率区间与提醒\")"))
        assertTrue(source.contains("SectionTitle(text = \"隐私与使用边界\")"))
        assertTrue(stateSource.contains("\"重新连接\""))
        assertTrue(stateSource.contains("\"更换设备\""))
        assertTrue(source.contains("清除已保存设备"))
        assertFalse(source.contains("Text(text = \"关闭心率功能\")"))
        assertFalse(source.contains("Text(text = \"断开心率设备\")"))
        assertTrue(source.contains("个人最大心率"))
        assertTrue(source.contains("年龄"))
        assertTrue(source.contains("上限提醒"))
        assertTrue(shellSource.contains("Settings.ACTION_BLUETOOTH_SETTINGS"))
    }

    @Test
    fun heartRatePersonalParametersUseCompactAccessibleSettingRows() {
        val source = File("src/main/java/com/liujyks/trainflow/feature/settings/SettingsRoute.kt").readText()

        assertTrue(source.contains("OptionalHeartRateNumberRow("))
        assertTrue(source.contains("label = \"年龄\""))
        assertTrue(source.contains("label = \"个人最大心率\""))
        assertTrue(source.contains("label = \"上限提醒\""))
        assertTrue(source.contains("rangeDescription = \"1–130 · 可选\""))
        assertTrue(source.contains("rangeDescription = \"bpm · 30–260 · 可选\""))
        assertTrue(
            source.contains(
                "contentDescription = \"${'$'}label，当前值 ${'$'}currentValue，允许范围 ${'$'}rangeDescription\""
            )
        )
        assertTrue(source.contains("stateDescription = validationDescription"))
        assertFalse(source.contains("private fun OptionalHeartRateNumberField("))
        assertTrue(source.contains("iconRes = R.drawable.ic_person_outline_24"))
        assertTrue(source.contains("iconRes = R.drawable.ic_monitor_heart_24"))
        assertTrue(source.contains("iconRes = R.drawable.ic_notifications_none_24"))
        assertTrue(source.contains("painterResource(R.drawable.ic_chevron_right_24)"))
        assertTrue(source.contains(".heightIn(min = 64.dp)"))
        assertTrue(source.contains(".padding(horizontal = 12.dp, vertical = 6.dp)"))
        assertTrue(source.contains("Spacer(modifier = Modifier.width(16.dp))"))
        assertTrue(source.contains(".size(20.dp)"))
        assertTrue(source.contains("fontSize = 12.sp"))
        assertTrue(source.contains("lineHeight = 16.sp"))
        assertTrue(source.contains(".size(width = 60.dp, height = 40.dp)"))
        assertTrue(source.contains("shape = RoundedCornerShape(10.dp)"))
        assertTrue(source.contains("modifier = Modifier.padding(8.dp)"))
        assertTrue(source.contains("Spacer(modifier = Modifier.width(8.dp))"))
        assertTrue(source.contains("HorizontalDivider(thickness = 1.dp, color = TrainFlowNeutral100)"))

        listOf(
            "ic_person_outline_24.xml",
            "ic_monitor_heart_24.xml",
            "ic_notifications_none_24.xml",
            "ic_chevron_right_24.xml"
        ).forEach { name ->
            assertTrue(
                "Missing narrowly vendored official Material vector: $name",
                File("src/main/res/drawable/$name").isFile
            )
        }
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
    fun collapsedCapsuleCentersAndEllipsizesCopyWithoutLeadingRail() {
        val source = File(
            "src/main/java/com/liujyks/trainflow/ui/shell/official/HeartRateFloatingCapsule.kt"
        ).readText()

        assertTrue(source.contains(".widthIn(min = 116.dp, max = if (expanded) expandedMaxWidth else 180.dp)"))
        assertFalse(source.contains("textAlign = TextAlign.Center"))
        assertFalse(source.contains("import androidx.compose.ui.text.style.TextAlign"))
        assertFalse(
            source.contains(
                """else {
                Text(
                    text = uiState.collapsedLabel,
                    modifier = Modifier.fillMaxWidth(),"""
            )
        )
        assertTrue(
            "Expanded and collapsed branches must each render the 8dp tone dot",
            """\.size\(8\.dp\)""".toRegex().findAll(source).count() == 2
        )
        assertTrue(
            "Expanded and collapsed branches must each use an exact 8dp dot-to-label gap",
            """horizontalArrangement = Arrangement\.spacedBy\(8\.dp\)""".toRegex()
                .findAll(source)
                .count() >= 3
        )
        assertTrue(source.contains("maxLines = 1"))
        assertTrue(source.contains("overflow = TextOverflow.Ellipsis"))
        assertTrue(source.contains("Brush.horizontalGradient"))
        assertTrue(source.contains("heartRateCapsuleGradientStops("))
        assertFalse(source.contains("size = Size(width = 4.dp.toPx(), height = size.height)"))
    }

    @Test
    fun strengthPreferenceCardDoesNotExposeInternalTimerModeTokens() {
        val source = File("src/main/java/com/liujyks/trainflow/feature/settings/SettingsRoute.kt").readText()

        assertFalse(source.contains("mode.contractValue"))
        assertTrue(source.contains("Text(mode.label)"))
    }
}
