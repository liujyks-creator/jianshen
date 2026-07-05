package com.liujyks.trainflow.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowAction
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral500
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral700
import com.liujyks.trainflow.ui.theme.TrainFlowPrimary
import com.liujyks.trainflow.ui.theme.TrainFlowSecondary
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
import com.liujyks.trainflow.ui.designsystem.TileFlowCard
import com.liujyks.trainflow.ui.designsystem.currentCardCorner
import com.liujyks.trainflow.ui.designsystem.currentPageHorizontalPadding
import com.liujyks.trainflow.ui.designsystem.currentProminentCardCorner
import com.liujyks.trainflow.ui.designsystem.currentSectionSpacing
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin
import com.liujyks.trainflow.ui.theme.isBigType
import com.liujyks.trainflow.ui.theme.isTileFlow

@Composable
fun HomeRoute(
    onOpenExerciseLibrary: () -> Unit,
    onOpenTimedPlanEditor: () -> Unit,
    onOpenStrengthPlanEditor: () -> Unit,
    onOpenFollowAlong: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenHeartRateBroadcastSmoke: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState = remember { buildHomeScreenState() }

    TrainFlowHomeScreen(
        uiState = uiState,
        onOpenExerciseLibrary = onOpenExerciseLibrary,
        onOpenTimedPlanEditor = onOpenTimedPlanEditor,
        onOpenStrengthPlanEditor = onOpenStrengthPlanEditor,
        onOpenFollowAlong = onOpenFollowAlong,
        onOpenSettings = onOpenSettings,
        onOpenPlans = onOpenPlans,
        onOpenRecords = onOpenRecords,
        onOpenHeartRateBroadcastSmoke = onOpenHeartRateBroadcastSmoke,
        modifier = modifier
    )
}

@Composable
private fun TrainFlowHomeScreen(
    uiState: HomeScreenState,
    onOpenExerciseLibrary: () -> Unit,
    onOpenTimedPlanEditor: () -> Unit,
    onOpenStrengthPlanEditor: () -> Unit,
    onOpenFollowAlong: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenHeartRateBroadcastSmoke: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (LocalTrainFlowSkin.current.isTileFlow) {
        TileFlowHomeScreen(
            uiState = uiState,
            onOpenExerciseLibrary = onOpenExerciseLibrary,
            onOpenTimedPlanEditor = onOpenTimedPlanEditor,
            onOpenStrengthPlanEditor = onOpenStrengthPlanEditor,
            onOpenFollowAlong = onOpenFollowAlong,
            onOpenSettings = onOpenSettings,
            onOpenPlans = onOpenPlans,
            onOpenRecords = onOpenRecords,
            onOpenHeartRateBroadcastSmoke = onOpenHeartRateBroadcastSmoke,
            modifier = modifier
        )
        return
    }
    if (LocalTrainFlowSkin.current.isBigType) {
        BigTypeHomeScreen(
            uiState = uiState,
            onOpenExerciseLibrary = onOpenExerciseLibrary,
            onOpenTimedPlanEditor = onOpenTimedPlanEditor,
            onOpenStrengthPlanEditor = onOpenStrengthPlanEditor,
            onOpenFollowAlong = onOpenFollowAlong,
            onOpenSettings = onOpenSettings,
            onOpenHeartRateBroadcastSmoke = onOpenHeartRateBroadcastSmoke,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TrainFlowSurfaceMuted)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onOpenHeartRateBroadcastSmoke != null) {
            item {
                HeartRateBroadcastSmokeDebugButton(onClick = onOpenHeartRateBroadcastSmoke)
            }
        }

        item {
            HomeHeader(
                summary = uiState.summary,
                onOpenSettings = onOpenSettings
            )
        }

        item {
            TimedTrainingEntryCard(
                entry = uiState.primaryEntry,
                onClick = onOpenTimedPlanEditor
            )
        }

        item {
            Text(
                text = "同层入口",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        uiState.peerEntries.forEach { entry ->
            item {
                HomeEntryCard(
                    entry = entry,
                    accent = when (entry.id) {
                        HomeEntryId.STRENGTH_TRAINING -> TrainFlowAction
                        HomeEntryId.FOLLOW_ALONG -> TrainFlowAction
                        HomeEntryId.EXERCISE_LIBRARY -> TrainFlowAccent
                        else -> TrainFlowNeutral500
                    },
                    onClick = if (entry.id == HomeEntryId.EXERCISE_LIBRARY) {
                        onOpenExerciseLibrary
                    } else if (entry.id == HomeEntryId.STRENGTH_TRAINING) {
                        onOpenStrengthPlanEditor
                    } else if (entry.id == HomeEntryId.FOLLOW_ALONG) {
                        onOpenFollowAlong
                    } else {
                        null
                    }
                )
            }
        }

        item {
            FutureBoundaryPanel(entries = uiState.futureEntries)
        }
    }
}

@Composable
private fun BigTypeHomeScreen(
    uiState: HomeScreenState,
    onOpenExerciseLibrary: () -> Unit,
    onOpenTimedPlanEditor: () -> Unit,
    onOpenStrengthPlanEditor: () -> Unit,
    onOpenFollowAlong: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHeartRateBroadcastSmoke: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val strength = uiState.peerEntries.first { it.id == HomeEntryId.STRENGTH_TRAINING }
    val followAlong = uiState.peerEntries.first { it.id == HomeEntryId.FOLLOW_ALONG }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = currentPageHorizontalPadding(), vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(currentSectionSpacing())
    ) {
        if (onOpenHeartRateBroadcastSmoke != null) {
            item {
                HeartRateBroadcastSmokeDebugButton(onClick = onOpenHeartRateBroadcastSmoke)
            }
        }

        item {
            BigTypeHomeHeader(onOpenSettings = onOpenSettings)
        }
        item {
            BigTypePrimaryEntry(
                entry = uiState.primaryEntry,
                onClick = onOpenTimedPlanEditor
            )
        }
        item {
            Text(
                text = "其他训练",
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        item {
            BigTypeSecondaryEntry(
                entry = strength,
                accent = LocalTrainFlowSkin.current.tokens.action,
                actionLabel = "进入力量训练",
                onClick = onOpenStrengthPlanEditor
            )
        }
        item {
            BigTypeSecondaryEntry(
                entry = followAlong,
                accent = LocalTrainFlowSkin.current.tokens.accent,
                actionLabel = "进入基础跟练",
                onClick = onOpenFollowAlong
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenExerciseLibrary,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(currentCardCorner())
                ) {
                    Text(text = "动作库", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(currentCardCorner())
                ) {
                    Text(text = "训练设置", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BigTypeHomeHeader(onOpenSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TrainFlow",
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onOpenSettings) {
                Text(text = "设置", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = "选一种训练，直接开始。",
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BigTypePrimaryEntry(
    entry: HomeEntryUiState,
    onClick: () -> Unit
) {
    val skin = LocalTrainFlowSkin.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(currentProminentCardCorner()),
        colors = CardDefaults.cardColors(containerColor = skin.tokens.primary),
        border = BorderStroke(2.dp, skin.tokens.accent.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "推荐开始",
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = skin.tokens.accent
            )
            Text(
                text = entry.title,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = skin.tokens.neutral50
            )
            Text(
                text = "按动作时间、休息和轮次推进",
                fontSize = 20.sp,
                lineHeight = 26.sp,
                color = skin.tokens.neutral100
            )
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = skin.tokens.trainingButtonHeightDp.dp),
                shape = RoundedCornerShape(currentCardCorner()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = skin.tokens.accent,
                    contentColor = skin.tokens.primary
                )
            ) {
                Text(text = "开始计时训练", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun BigTypeSecondaryEntry(
    entry: HomeEntryUiState,
    accent: Color,
    actionLabel: String,
    onClick: () -> Unit
) {
    val skin = LocalTrainFlowSkin.current
    val buttonContentColor = if (accent == skin.tokens.accent) {
        skin.tokens.primary
    } else {
        skin.tokens.neutral50
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(currentCardCorner()),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, accent.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = entry.title,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (entry.id == HomeEntryId.STRENGTH_TRAINING) {
                    "重量、次数、组数和休息"
                } else {
                    "跟随动作提示与计时流程"
                },
                fontSize = 18.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(currentCardCorner()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = buttonContentColor
                )
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = buttonContentColor
                )
            }
        }
    }
}

@Composable
private fun TileFlowHomeScreen(
    uiState: HomeScreenState,
    onOpenExerciseLibrary: () -> Unit,
    onOpenTimedPlanEditor: () -> Unit,
    onOpenStrengthPlanEditor: () -> Unit,
    onOpenFollowAlong: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenHeartRateBroadcastSmoke: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    val strength = uiState.peerEntries.first { it.id == HomeEntryId.STRENGTH_TRAINING }
    val followAlong = uiState.peerEntries.first { it.id == HomeEntryId.FOLLOW_ALONG }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = currentPageHorizontalPadding(), vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(currentSectionSpacing())
    ) {
        if (onOpenHeartRateBroadcastSmoke != null) {
            item {
                HeartRateBroadcastSmokeDebugButton(onClick = onOpenHeartRateBroadcastSmoke)
            }
        }

        item {
            HomeHeader(
                summary = "今天从一块清晰的训练磁贴开始。",
                onOpenSettings = onOpenSettings
            )
        }
        item {
            TileFlowPrimaryEntry(
                entry = uiState.primaryEntry,
                onClick = onOpenTimedPlanEditor
            )
        }
        item {
            Text(
                text = "选择训练方式",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(currentSectionSpacing())
            ) {
                TileFlowEntry(
                    entry = strength,
                    containerColor = skin.tokens.action.copy(alpha = 0.12f),
                    borderColor = skin.tokens.action.copy(alpha = 0.24f),
                    onClick = onOpenStrengthPlanEditor,
                    modifier = Modifier.weight(1f)
                )
                TileFlowEntry(
                    entry = followAlong,
                    containerColor = skin.tokens.accent.copy(alpha = 0.12f),
                    borderColor = skin.tokens.accent.copy(alpha = 0.24f),
                    onClick = onOpenFollowAlong,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Text(
                text = "训练工作区",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        uiState.quickEntries.chunked(2).forEach { entries ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(currentSectionSpacing())
                ) {
                    entries.forEach { entry ->
                        TileFlowQuickEntry(
                            entry = entry,
                            onClick = when (entry.id) {
                                HomeEntryId.EXERCISE_LIBRARY -> onOpenExerciseLibrary
                                HomeEntryId.RECENT_PLAN -> onOpenPlans
                                HomeEntryId.TRAINING_PREFERENCES,
                                HomeEntryId.REMINDER_STATUS -> onOpenSettings
                                HomeEntryId.SESSION_RECORDS -> onOpenRecords
                                else -> ({})
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (entries.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            FutureBoundaryPanel(
                entries = uiState.futureEntries.filterNot { entry ->
                    entry.id == HomeEntryId.SESSION_RECORDS
                }
            )
        }
    }
}

@Composable
private fun TileFlowPrimaryEntry(
    entry: HomeEntryUiState,
    onClick: () -> Unit
) {
    val skin = LocalTrainFlowSkin.current
    TileFlowCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = skin.tokens.primary,
        borderColor = skin.tokens.primary,
        prominent = true,
        onClick = onClick
    ) {
        StatusPill(text = entry.badge, color = skin.tokens.accent, contentColor = skin.tokens.primary)
        Text(
            text = "开始计时训练",
            style = MaterialTheme.typography.headlineMedium,
            color = skin.tokens.neutral50
        )
        Text(
            text = entry.description,
            style = MaterialTheme.typography.bodyLarge,
            color = skin.tokens.neutral50.copy(alpha = 0.82f)
        )
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = skin.tokens.accent,
                contentColor = skin.tokens.primary
            )
        ) {
            Text(text = entry.status, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TileFlowEntry(
    entry: HomeEntryUiState,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TileFlowCard(
        modifier = modifier.heightIn(min = 164.dp),
        containerColor = containerColor,
        borderColor = borderColor,
        onClick = onClick
    ) {
        Text(
            text = entry.badge,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = entry.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = entry.status,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TileFlowQuickEntry(
    entry: HomeEntryUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TileFlowCard(
        modifier = modifier.heightIn(min = 126.dp),
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        onClick = onClick
    ) {
        Text(
            text = entry.badge,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = entry.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeHeader(
    summary: String,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TrainFlow",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onOpenSettings) {
                Text(text = "训练偏好")
            }
        }
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeartRateBroadcastSmokeDebugButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TrainFlowAction,
            contentColor = TrainFlowNeutral50
        )
    ) {
        Text(
            text = "HR Broadcast Smoke",
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TimedTrainingEntryCard(
    entry: HomeEntryUiState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TrainFlowPrimary)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusPill(text = entry.badge, color = TrainFlowAccent, contentColor = TrainFlowPrimary)
            Text(
                text = entry.title,
                style = MaterialTheme.typography.headlineMedium,
                color = TrainFlowNeutral50
            )
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyLarge,
                color = TrainFlowNeutral50.copy(alpha = 0.82f)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(text = "动作倒计时")
                StatusPill(text = "休息提醒")
                StatusPill(text = "训练后记录")
            }
            Button(
                onClick = onClick,
                enabled = entry.enabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrainFlowAccent,
                    contentColor = TrainFlowPrimary,
                    disabledContainerColor = TrainFlowNeutral100,
                    disabledContentColor = TrainFlowNeutral700
                )
            ) {
                Text(
                    text = entry.status,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HomeEntryCard(
    entry: HomeEntryUiState,
    accent: Color,
    onClick: (() -> Unit)?
) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = entry.enabled && onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = entry.badge,
                    style = MaterialTheme.typography.labelLarge,
                    color = TrainFlowNeutral700
                )
            }
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entry.enabled && onClick != null) {
                TextButton(onClick = onClick) {
                    Text(text = entry.status)
                }
            } else {
                DisabledStatusButton(text = entry.status)
            }
        }
    }
}

@Composable
private fun FutureBoundaryPanel(entries: List<HomeEntryUiState>) {
    val skin = LocalTrainFlowSkin.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            if (skin.isTileFlow) skin.tokens.cardCornerDp.dp else 10.dp
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (skin.isTileFlow) skin.tokens.neutral100 else TrainFlowNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "后续边界",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            entries.forEach { entry ->
                FutureBoundaryRow(entry)
            }
        }
    }
}

@Composable
private fun FutureBoundaryRow(entry: HomeEntryUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = entry.status,
                style = MaterialTheme.typography.labelLarge,
                color = TrainFlowNeutral500
            )
        }
        Text(
            text = entry.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color = TrainFlowSecondary,
    contentColor: Color = TrainFlowNeutral50
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

@Composable
private fun DisabledStatusButton(text: String) {
    Button(
        onClick = {},
        enabled = false,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = TrainFlowNeutral100,
            disabledContentColor = TrainFlowNeutral700
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainFlowHomeScreenPreview() {
    TrainFlowTheme {
        TrainFlowHomeScreen(
            uiState = buildHomeScreenState(),
            onOpenExerciseLibrary = {},
            onOpenTimedPlanEditor = {},
            onOpenStrengthPlanEditor = {},
            onOpenFollowAlong = {},
            onOpenSettings = {},
            onOpenPlans = {},
            onOpenRecords = {},
            onOpenHeartRateBroadcastSmoke = null
        )
    }
}
