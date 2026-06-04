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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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

@Composable
fun HomeRoute(
    onOpenExerciseLibrary: () -> Unit,
    onOpenTimedPlanEditor: () -> Unit,
    onOpenStrengthPlanEditor: () -> Unit,
    onOpenFollowAlong: () -> Unit,
    onOpenSettings: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TrainFlowSurfaceMuted)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TrainFlowNeutral100)
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
            onOpenSettings = {}
        )
    }
}
