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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
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
    onUiSkinChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(if (skin.isTileFlow) MaterialTheme.colorScheme.background else TrainFlowSurfaceMuted)
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
            SkinPreferencesCard(
                uiState = uiState,
                onUiSkinChanged = onUiSkinChanged
            )
        }

        item {
            NotificationBoundaryCard()
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
            text = "当前不包含语音读秒、自动语音教练或后台可靠计时保障。",
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
                    Text("${mode.label} · ${mode.contractValue}")
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
private fun SkinPreferencesCard(
    uiState: TrainingPreferencesScreenState,
    onUiSkinChanged: (String) -> Unit
) {
    SettingsCard(tileAccent = LocalTrainFlowSkin.current.tokens.accent) {
        SectionTitle(text = "UI 皮肤")
        Text(
            text = "三套皮肤都是内置注册项。Tile Flow 已适配关键页面；Big Type 仍保持后续视觉重做占位。",
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
private fun NotificationBoundaryCard() {
    SettingsCard {
        SectionTitle(text = "通知边界说明")
        Text(
            text = "计划提醒通知由 E7.1 提供，活跃训练普通 ongoing 状态提示由 E7.2 提供。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "本页只保存训练内倒计时反馈默认值，不新增闹钟级强提醒、前台服务或通知操作控制训练。",
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
            onUiSkinChanged = {}
        )
    }
}
