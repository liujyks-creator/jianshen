package com.liujyks.trainflow.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.core.model.PermissionPrivacySection
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
import com.liujyks.trainflow.ui.designsystem.currentCardCorner
import com.liujyks.trainflow.ui.designsystem.currentPageHorizontalPadding
import com.liujyks.trainflow.ui.designsystem.currentSectionSpacing
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin
import com.liujyks.trainflow.ui.theme.isTileFlow

@Composable
internal fun SettingsRoute(
    uiState: TrainingPreferencesScreenState,
    onBackToTraining: () -> Unit,
    onDefaultCountdownThresholdChanged: (Int) -> Unit,
    onActionCueEnabledChanged: (Boolean) -> Unit,
    onRestCueEnabledChanged: (Boolean) -> Unit,
    onSoundEnabledChanged: (Boolean) -> Unit,
    onVibrationEnabledChanged: (Boolean) -> Unit,
    onEmphasisAnimationEnabledChanged: (Boolean) -> Unit,
    onStrengthSetTimerModeChanged: (StrengthSetTimerModePreference) -> Unit,
    onHeartRateDisplayEnabledChanged: (Boolean) -> Unit,
    onPrepareHeartRateBlePermission: () -> Unit,
    onRequestHeartRateBlePermission: () -> Unit,
    onStartHeartRateDeviceScan: () -> Unit,
    onStopHeartRateDeviceScan: () -> Unit,
    onSelectHeartRateDevice: (String) -> Unit,
    onClearHeartRateDevicePreference: () -> Unit,
    onUiSkinChanged: (String) -> Unit,
    heartRateFocusRequestKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val heartRateItemIndex = remember { 4 }
    LaunchedEffect(heartRateFocusRequestKey) {
        if (heartRateFocusRequestKey > 0) {
            listState.animateScrollToItem(heartRateItemIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = currentPageHorizontalPadding(), vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(currentSectionSpacing())
    ) {
        item {
            TextButton(onClick = onBackToTraining) {
                Text(text = "返回训练首页")
            }
        }

        item {
            SettingsHeader(uiState)
        }

        item {
            CountdownPreferencesCard(
                uiState = uiState,
                onDefaultCountdownThresholdChanged = onDefaultCountdownThresholdChanged,
                onActionCueEnabledChanged = onActionCueEnabledChanged,
                onRestCueEnabledChanged = onRestCueEnabledChanged,
                onSoundEnabledChanged = onSoundEnabledChanged,
                onVibrationEnabledChanged = onVibrationEnabledChanged,
                onEmphasisAnimationEnabledChanged = onEmphasisAnimationEnabledChanged
            )
        }

        item {
            StrengthPreferencesCard(
                uiState = uiState,
                onStrengthSetTimerModeChanged = onStrengthSetTimerModeChanged
            )
        }

        item {
            HeartRatePreferencesCard(
                uiState = uiState.heartRateSettings,
                onHeartRateDisplayEnabledChanged = onHeartRateDisplayEnabledChanged,
                onPrepareHeartRateBlePermission = onPrepareHeartRateBlePermission,
                onRequestHeartRateBlePermission = onRequestHeartRateBlePermission,
                onStartHeartRateDeviceScan = onStartHeartRateDeviceScan,
                onStopHeartRateDeviceScan = onStopHeartRateDeviceScan,
                onSelectHeartRateDevice = onSelectHeartRateDevice,
                onClearHeartRateDevicePreference = onClearHeartRateDevicePreference
            )
        }

        item {
            SkinPreferencesCard(
                uiState = uiState,
                onUiSkinChanged = onUiSkinChanged
            )
        }

        item {
            PermissionPrivacyCard(uiState.permissionPrivacySections)
        }
    }
}

@Composable
private fun SettingsHeader(uiState: TrainingPreferencesScreenState) {
    val skin = LocalTrainFlowSkin.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(text = "设置与皮肤", color = skin.tokens.accent, contentColor = skin.tokens.primary)
        Text(
            text = "训练偏好设置",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "${uiState.countdownSummary} · ${uiState.feedbackSummary} · ${uiState.selectedSkinSummary}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "训练反馈设置作为新计划默认值；UI 皮肤只改变表现和 token，不改变训练计划、记录或命令。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun CountdownPreferencesCard(
    uiState: TrainingPreferencesScreenState,
    onDefaultCountdownThresholdChanged: (Int) -> Unit,
    onActionCueEnabledChanged: (Boolean) -> Unit,
    onRestCueEnabledChanged: (Boolean) -> Unit,
    onSoundEnabledChanged: (Boolean) -> Unit,
    onVibrationEnabledChanged: (Boolean) -> Unit,
    onEmphasisAnimationEnabledChanged: (Boolean) -> Unit
) {
    SettingsCard(tileAccent = LocalTrainFlowSkin.current.tokens.accent) {
        SectionTitle(text = "训练内倒计时反馈")
        NumberField(
            label = "默认临近结束秒数",
            value = uiState.defaultCountdownThresholdSec,
            onValueChanged = onDefaultCountdownThresholdChanged,
            modifier = Modifier.fillMaxWidth()
        )
        ToggleRow(
            title = "动作临近结束提醒",
            checked = uiState.actionCueEnabled,
            onCheckedChange = onActionCueEnabledChanged
        )
        ToggleRow(
            title = "休息临近结束提醒",
            checked = uiState.restCueEnabled,
            onCheckedChange = onRestCueEnabledChanged
        )
        ToggleRow(
            title = "声音",
            checked = uiState.soundEnabled,
            onCheckedChange = onSoundEnabledChanged
        )
        ToggleRow(
            title = "震动",
            checked = uiState.vibrationEnabled,
            onCheckedChange = onVibrationEnabledChanged
        )
        ToggleRow(
            title = "强化动画",
            checked = uiState.emphasisAnimationEnabled,
            onCheckedChange = onEmphasisAnimationEnabledChanged
        )
        Text(
            text = "音频只是短促提示音；当前不包含语音读秒、自动语音教练或后台可靠计时保障。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun StrengthPreferencesCard(
    uiState: TrainingPreferencesScreenState,
    onStrengthSetTimerModeChanged: (StrengthSetTimerModePreference) -> Unit
) {
    SettingsCard(tileAccent = LocalTrainFlowSkin.current.tokens.action) {
        SectionTitle(text = "力量训练默认")
        Text(
            text = "本组计时默认模式",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        StrengthSetTimerModePreference.entries.forEach { mode ->
            FilterChip(
                selected = uiState.strengthSetTimerMode == mode,
                onClick = { onStrengthSetTimerModeChanged(mode) },
                label = {
                    Text(mode.label)
                }
            )
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral700
            )
        }
    }
}

@Composable
private fun HeartRatePreferencesCard(
    uiState: HeartRateSettingsUiState,
    onHeartRateDisplayEnabledChanged: (Boolean) -> Unit,
    onPrepareHeartRateBlePermission: () -> Unit,
    onRequestHeartRateBlePermission: () -> Unit,
    onStartHeartRateDeviceScan: () -> Unit,
    onStopHeartRateDeviceScan: () -> Unit,
    onSelectHeartRateDevice: (String) -> Unit,
    onClearHeartRateDevicePreference: () -> Unit
) {
    SettingsCard(tileAccent = LocalTrainFlowSkin.current.tokens.focus) {
        SectionTitle(text = uiState.sectionTitle)
        ToggleRow(
            title = "心率显示",
            checked = uiState.enabled,
            onCheckedChange = onHeartRateDisplayEnabledChanged
        )
        StatusBlock(title = "心率显示：${uiState.statusLabel}", body = uiState.statusSummary)
        if (uiState.enabled) {
            StatusBlock(
                title = "连接状态：${uiState.devicePickerState.connectionStatusLabel}",
                body = uiState.sourceSummary
            )
            uiState.savedDeviceDisplayName?.let { displayName ->
                StatusBlock(
                    title = "已保存设备：$displayName",
                    body = "保存设备仅供你主动连接，不代表设备在附近、已开启广播、正在连接或已经连接。"
                )
            }
        }
        StatusBlock(title = uiState.blePermissionStatusTitle, body = uiState.blePermissionStatusCopy)
        if (uiState.showBlePermissionRationale) {
            StatusBlock(
                title = uiState.blePermissionRationaleTitle,
                body = uiState.blePermissionRationaleBullets.joinToString("\n")
            )
            Button(
                onClick = onRequestHeartRateBlePermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = uiState.blePermissionActionLabel)
            }
        } else if (uiState.enabled && uiState.canPrepareBlePermission) {
            Button(
                onClick = onPrepareHeartRateBlePermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = uiState.blePermissionActionLabel)
            }
        }
        HeartRateDevicePickerBlock(
            uiState = uiState.devicePickerState,
            onStartHeartRateDeviceScan = onStartHeartRateDeviceScan,
            onStopHeartRateDeviceScan = onStopHeartRateDeviceScan,
            onSelectHeartRateDevice = onSelectHeartRateDevice
        )
        StatusBlock(title = "显示用途", body = uiState.purposeCopy)
        StatusBlock(title = "记录边界", body = uiState.recordingBoundaryCopy)
        StatusBlock(title = "隐私说明", body = uiState.privacyCopy)
        StatusBlock(title = "非医疗说明", body = uiState.nonMedicalCopy)
        StatusBlock(title = "权限说明", body = uiState.permissionCopy)
        StatusBlock(title = "悬浮边界", body = uiState.overlayCopy)
        Text(
            text = uiState.enabledBoundaryCopy,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
        if (uiState.canClearSavedDevice) {
            OutlinedButton(
                onClick = onClearHeartRateDevicePreference,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "清除已保存设备")
            }
        }
    }
}

@Composable
private fun HeartRateDevicePickerBlock(
    uiState: HeartRateDevicePickerUiState,
    onStartHeartRateDeviceScan: () -> Unit,
    onStopHeartRateDeviceScan: () -> Unit,
    onSelectHeartRateDevice: (String) -> Unit
) {
    StatusBlock(title = uiState.title, body = uiState.body)
    Text(
        text = "${uiState.scanWindowCopy} ${uiState.bandHint}",
        style = MaterialTheme.typography.bodyMedium,
        color = TrainFlowNeutral700
    )
    when {
        uiState.canStopScan -> {
            OutlinedButton(
                onClick = onStopHeartRateDeviceScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = uiState.actionLabel ?: "停止扫描")
            }
        }

        uiState.actionLabel != null -> {
            Button(
                enabled = uiState.canStartScan,
                onClick = onStartHeartRateDeviceScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = uiState.actionLabel)
            }
        }
    }
    if (uiState.showDeviceList) {
        uiState.devices.forEach { device ->
            HeartRateDeviceCandidateRow(
                device = device,
                onSelectHeartRateDevice = onSelectHeartRateDevice
            )
        }
    }
}

@Composable
private fun HeartRateDeviceCandidateRow(
    device: HeartRateDeviceCandidateUiState,
    onSelectHeartRateDevice: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = device.displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${device.safeIdentifier} · ${device.signalSummary} · ${device.capabilitySummary}",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
        Button(
            onClick = { onSelectHeartRateDevice(device.identifier) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "选择设备")
        }
    }
}

@Composable
private fun SkinPreferencesCard(
    uiState: TrainingPreferencesScreenState,
    onUiSkinChanged: (String) -> Unit
) {
    SettingsCard(tileAccent = LocalTrainFlowSkin.current.tokens.accent) {
        SectionTitle(text = "UI 皮肤")
        Text(
            text = "三套皮肤都是内置注册项。Big Type 重点放大训练首页与执行主信息，计划编辑、动作详情和历史等信息密集页面继续沿用现有布局。",
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
        uiState.uiSkinOptions.forEach { skin ->
            FilterChip(
                selected = skin.selected,
                onClick = { onUiSkinChanged(skin.id) },
                label = {
                    Text(
                        text = if (skin.isDefault) {
                            "${skin.displayName} · 默认"
                        } else {
                            skin.displayName
                        }
                    )
                }
            )
            Text(
                text = skin.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${skin.targetUser} ${skin.capabilityBoundary}",
                style = MaterialTheme.typography.bodySmall,
                color = TrainFlowNeutral700
            )
        }
    }
}

@Composable
private fun PermissionPrivacyCard(sections: List<PermissionPrivacySection>) {
    SettingsCard {
        SectionTitle(text = "权限与隐私")
        Text(
            text = "用户测试前请按这些边界理解当前能力。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        sections.forEach { section ->
            BoundaryTextRow(section)
        }
    }
}

@Composable
private fun BoundaryTextRow(section: PermissionPrivacySection) {
    StatusBlock(title = section.title, body = section.body)
}

@Composable
private fun StatusBlock(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { input ->
            input.filter { it.isDigit() }
                .toIntOrNull()
                ?.let(onValueChanged)
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true
    )
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsCard(
    tileAccent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val skin = LocalTrainFlowSkin.current
    val containerColor = if (skin.isTileFlow && tileAccent != null) {
        tileAccent.copy(alpha = 0.07f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (skin.isTileFlow && tileAccent != null) {
        tileAccent.copy(alpha = 0.22f)
    } else {
        TrainFlowNeutral100
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(currentCardCorner()),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun StatusPill(
    text: String,
    color: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsRoutePreview() {
    TrainFlowTheme {
        SettingsRoute(
            uiState = defaultTrainingPreferencesScreenState(),
            onBackToTraining = {},
            onDefaultCountdownThresholdChanged = {},
            onActionCueEnabledChanged = {},
            onRestCueEnabledChanged = {},
            onSoundEnabledChanged = {},
            onVibrationEnabledChanged = {},
            onEmphasisAnimationEnabledChanged = {},
            onStrengthSetTimerModeChanged = {},
            onHeartRateDisplayEnabledChanged = {},
            onPrepareHeartRateBlePermission = {},
            onRequestHeartRateBlePermission = {},
            onStartHeartRateDeviceScan = {},
            onStopHeartRateDeviceScan = {},
            onSelectHeartRateDevice = {},
            onClearHeartRateDevicePreference = {},
            onUiSkinChanged = {}
        )
    }
}
