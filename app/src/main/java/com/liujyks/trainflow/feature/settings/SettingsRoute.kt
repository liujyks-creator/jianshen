package com.liujyks.trainflow.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
    onChangeHeartRateDevice: () -> Unit,
    onStopHeartRateDeviceScan: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onSelectHeartRateDevice: (String) -> Unit,
    onDisconnectHeartRateDevice: () -> Unit,
    onReconnectHeartRateDevice: () -> Unit,
    onClearHeartRateDevicePreference: () -> Unit,
    onHeartRatePersonalParametersChanged: (Int?, Int?, Int?) -> Unit,
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
                onChangeHeartRateDevice = onChangeHeartRateDevice,
                onStopHeartRateDeviceScan = onStopHeartRateDeviceScan,
                onOpenBluetoothSettings = onOpenBluetoothSettings,
                onSelectHeartRateDevice = onSelectHeartRateDevice,
                onDisconnectHeartRateDevice = onDisconnectHeartRateDevice,
                onReconnectHeartRateDevice = onReconnectHeartRateDevice,
                onClearHeartRateDevicePreference = onClearHeartRateDevicePreference,
                onHeartRatePersonalParametersChanged = onHeartRatePersonalParametersChanged
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
    onChangeHeartRateDevice: () -> Unit,
    onStopHeartRateDeviceScan: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onSelectHeartRateDevice: (String) -> Unit,
    onDisconnectHeartRateDevice: () -> Unit,
    onReconnectHeartRateDevice: () -> Unit,
    onClearHeartRateDevicePreference: () -> Unit,
    onHeartRatePersonalParametersChanged: (Int?, Int?, Int?) -> Unit
) {
    SettingsCard(tileAccent = LocalTrainFlowSkin.current.tokens.focus) {
        SectionTitle(text = "心率功能")
        ToggleRow(
            title = "启用心率功能",
            checked = uiState.enabled,
            onCheckedChange = onHeartRateDisplayEnabledChanged
        )
        Text(
            text = if (uiState.enabled) {
                "已启用；满足条件时在 TrainFlow 前台自动恢复已保存设备。"
            } else {
                "默认关闭；关闭时不显示、不扫描、不连接、不记录。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TrainFlowNeutral700
        )

        SectionTitle(text = "设备连接")
        if (uiState.enabled) {
            StatusBlock(
                title = "连接状态：${uiState.devicePickerState.connectionStatusLabel}",
                body = uiState.sourceSummary
            )
            Text(
                text = uiState.connectionIntentCopy,
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral700
            )
            if (uiState.blePermissionStatus != HeartRateBlePermissionStatus.GRANTED) {
                StatusBlock(
                    title = uiState.blePermissionStatusTitle,
                    body = if (uiState.showBlePermissionRationale) {
                        uiState.blePermissionRationaleBullets.joinToString("\n")
                    } else {
                        uiState.blePermissionStatusCopy
                    }
                )
            }
            HeartRateDevicePickerBlock(
                uiState = uiState.devicePickerState,
                onSelectHeartRateDevice = onSelectHeartRateDevice
            )
            HeartRateConnectionActions(
                uiState = uiState,
                onPrepareHeartRateBlePermission = onPrepareHeartRateBlePermission,
                onRequestHeartRateBlePermission = onRequestHeartRateBlePermission,
                onStartHeartRateDeviceScan = onStartHeartRateDeviceScan,
                onChangeHeartRateDevice = onChangeHeartRateDevice,
                onStopHeartRateDeviceScan = onStopHeartRateDeviceScan,
                onOpenBluetoothSettings = onOpenBluetoothSettings,
                onDisconnectHeartRateDevice = onDisconnectHeartRateDevice,
                onReconnectHeartRateDevice = onReconnectHeartRateDevice
            )
        }

        SectionTitle(text = "心率区间与提醒")
        if (uiState.enabled) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, TrainFlowNeutral100)
            ) {
                Column {
                    OptionalHeartRateNumberRow(
                        label = "年龄",
                        rangeDescription = "1–130 · 可选",
                        value = uiState.ageYears,
                        range = 1..130,
                        onValidValue = { age ->
                            onHeartRatePersonalParametersChanged(
                                age,
                                uiState.personalMaxHeartRateBpm,
                                uiState.alertThresholdBpm
                            )
                        }
                    )
                    HorizontalDivider(color = TrainFlowNeutral100)
                    OptionalHeartRateNumberRow(
                        label = "个人最大心率",
                        rangeDescription = "bpm · 30–260 · 可选",
                        value = uiState.personalMaxHeartRateBpm,
                        range = 30..260,
                        onValidValue = { personalMax ->
                            onHeartRatePersonalParametersChanged(
                                uiState.ageYears,
                                personalMax,
                                uiState.alertThresholdBpm
                            )
                        }
                    )
                    HorizontalDivider(color = TrainFlowNeutral100)
                    OptionalHeartRateNumberRow(
                        label = "上限提醒",
                        rangeDescription = "bpm · 30–260 · 可选",
                        value = uiState.alertThresholdBpm,
                        range = 30..260,
                        onValidValue = { alert ->
                            onHeartRatePersonalParametersChanged(
                                uiState.ageYears,
                                uiState.personalMaxHeartRateBpm,
                                alert
                            )
                        }
                    )
                }
            }
            Text(
                text = "优先使用个人最大心率，未填写时使用 220 − 年龄；区间与上限提醒仅作训练参考。",
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral700
            )
        } else {
            Text(
                text = "启用后可填写年龄、个人最大心率和上限提醒。",
                style = MaterialTheme.typography.bodyMedium,
                color = TrainFlowNeutral700
            )
        }

        SectionTitle(text = "隐私与使用边界")
        Text(
            text = "心率仅在 TrainFlow App 内显示；无训练时不写入训练记录。蓝牙权限只在你的连接操作后请求。心率与区间不用于医疗诊断，也不会自动中断训练。",
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
            Text(
                text = uiState.clearDeviceActionCopy,
                style = MaterialTheme.typography.bodySmall,
                color = TrainFlowNeutral700
            )
        }
    }
}

@Composable
private fun HeartRateConnectionActions(
    uiState: HeartRateSettingsUiState,
    onPrepareHeartRateBlePermission: () -> Unit,
    onRequestHeartRateBlePermission: () -> Unit,
    onStartHeartRateDeviceScan: () -> Unit,
    onChangeHeartRateDevice: () -> Unit,
    onStopHeartRateDeviceScan: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onDisconnectHeartRateDevice: () -> Unit,
    onReconnectHeartRateDevice: () -> Unit
) {
    uiState.primaryConnectionAction?.let { action ->
        Button(
            onClick = {
                dispatchHeartRateSettingsAction(
                    action = action.action,
                    onPrepareHeartRateBlePermission = onPrepareHeartRateBlePermission,
                    onRequestHeartRateBlePermission = onRequestHeartRateBlePermission,
                    onStartHeartRateDeviceScan = onStartHeartRateDeviceScan,
                    onChangeHeartRateDevice = onChangeHeartRateDevice,
                    onStopHeartRateDeviceScan = onStopHeartRateDeviceScan,
                    onOpenBluetoothSettings = onOpenBluetoothSettings,
                    onDisconnectHeartRateDevice = onDisconnectHeartRateDevice,
                    onReconnectHeartRateDevice = onReconnectHeartRateDevice
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = action.label)
        }
    }
    uiState.secondaryConnectionAction?.let { action ->
        OutlinedButton(
            onClick = {
                dispatchHeartRateSettingsAction(
                    action = action.action,
                    onPrepareHeartRateBlePermission = onPrepareHeartRateBlePermission,
                    onRequestHeartRateBlePermission = onRequestHeartRateBlePermission,
                    onStartHeartRateDeviceScan = onStartHeartRateDeviceScan,
                    onChangeHeartRateDevice = onChangeHeartRateDevice,
                    onStopHeartRateDeviceScan = onStopHeartRateDeviceScan,
                    onOpenBluetoothSettings = onOpenBluetoothSettings,
                    onDisconnectHeartRateDevice = onDisconnectHeartRateDevice,
                    onReconnectHeartRateDevice = onReconnectHeartRateDevice
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = action.label)
        }
    }
}

internal fun dispatchHeartRateSettingsAction(
    action: HeartRateSettingsAction,
    onPrepareHeartRateBlePermission: () -> Unit,
    onRequestHeartRateBlePermission: () -> Unit,
    onStartHeartRateDeviceScan: () -> Unit,
    onChangeHeartRateDevice: () -> Unit,
    onStopHeartRateDeviceScan: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onDisconnectHeartRateDevice: () -> Unit,
    onReconnectHeartRateDevice: () -> Unit
) {
    when (action) {
        HeartRateSettingsAction.PREPARE_PERMISSION,
        HeartRateSettingsAction.OPEN_APP_SETTINGS -> onPrepareHeartRateBlePermission()
        HeartRateSettingsAction.REQUEST_PERMISSION -> onRequestHeartRateBlePermission()
        HeartRateSettingsAction.OPEN_BLUETOOTH_SETTINGS -> onOpenBluetoothSettings()
        HeartRateSettingsAction.SCAN_DEVICES -> onStartHeartRateDeviceScan()
        HeartRateSettingsAction.STOP_SCAN -> onStopHeartRateDeviceScan()
        HeartRateSettingsAction.RECONNECT -> onReconnectHeartRateDevice()
        HeartRateSettingsAction.CHANGE_DEVICE -> onChangeHeartRateDevice()
        HeartRateSettingsAction.DISCONNECT -> onDisconnectHeartRateDevice()
    }
}

@Composable
private fun OptionalHeartRateNumberRow(
    label: String,
    rangeDescription: String,
    value: Int?,
    range: IntRange,
    onValidValue: (Int?) -> Unit
) {
    var text by remember(value) { mutableStateOf(value?.toString().orEmpty()) }
    val parsed = text.toIntOrNull()
    val invalid = text.isNotBlank() && parsed !in range
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentValue = text.ifBlank { "未填写" }
    val validationDescription = if (invalid) {
        "输入无效，请输入 ${range.first}–${range.last}，或留空"
    } else {
        "有效，可编辑"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
            .semantics(mergeDescendants = true) {
                contentDescription = "$label，当前值 $currentValue，允许范围 $rangeDescription"
                stateDescription = validationDescription
            }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = rangeDescription,
                style = MaterialTheme.typography.bodySmall,
                color = if (invalid) MaterialTheme.colorScheme.error else TrainFlowNeutral700
            )
            if (invalid) {
                Text("请输入 ${range.first}–${range.last}，或留空。")
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { next ->
                if (next.length <= 3 && next.all(Char::isDigit)) {
                    text = next
                    val nextValue = next.toIntOrNull()
                    if (next.isBlank() || nextValue in range) {
                        onValidValue(nextValue)
                    }
                }
            },
            isError = invalid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier
                .width(88.dp)
                .focusRequester(focusRequester)
        )
    }
}

@Composable
private fun HeartRateDevicePickerBlock(
    uiState: HeartRateDevicePickerUiState,
    onSelectHeartRateDevice: (String) -> Unit
) {
    StatusBlock(title = uiState.title, body = uiState.body)
    Text(
        text = "${uiState.scanWindowCopy} ${uiState.bandHint}",
        style = MaterialTheme.typography.bodyMedium,
        color = TrainFlowNeutral700
    )
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
            onChangeHeartRateDevice = {},
            onStopHeartRateDeviceScan = {},
            onOpenBluetoothSettings = {},
            onSelectHeartRateDevice = {},
            onDisconnectHeartRateDevice = {},
            onReconnectHeartRateDevice = {},
            onClearHeartRateDevicePreference = {},
            onHeartRatePersonalParametersChanged = { _, _, _ -> },
            onUiSkinChanged = {}
        )
    }
}
