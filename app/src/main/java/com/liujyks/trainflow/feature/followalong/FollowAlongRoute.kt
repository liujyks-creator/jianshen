package com.liujyks.trainflow.feature.followalong

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowSurfaceMuted
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

@Composable
internal fun FollowAlongRoute(
    onStartFollowAlong: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState = remember { buildDefaultFollowAlongScreenState() }
    FollowAlongScreen(uiState, onStartFollowAlong, modifier)
}

@Composable
private fun FollowAlongScreen(
    uiState: FollowAlongScreenState,
    onStartFollowAlong: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrainFlowSurfaceMuted)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = uiState.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, TrainFlowNeutral100)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = uiState.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onStartFollowAlong,
                    enabled = uiState.canStartFollowAlong,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(uiState.startLabel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FollowAlongRoutePreview() {
    TrainFlowTheme {
        FollowAlongScreen(buildDefaultFollowAlongScreenState(), {})
    }
}
