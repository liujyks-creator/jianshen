package com.liujyks.trainflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TrainFlowLightColorScheme = lightColorScheme(
    primary = TrainFlowPrimary,
    onPrimary = TrainFlowNeutral50,
    secondary = TrainFlowSecondary,
    onSecondary = TrainFlowNeutral50,
    tertiary = TrainFlowAction,
    onTertiary = TrainFlowNeutral50,
    background = TrainFlowSurfaceMuted,
    onBackground = TrainFlowNeutral900,
    surface = TrainFlowSurface,
    onSurface = TrainFlowNeutral900,
    surfaceVariant = TrainFlowNeutral100,
    onSurfaceVariant = TrainFlowNeutral700,
    outline = TrainFlowNeutral200
)

@Composable
fun TrainFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TrainFlowLightColorScheme,
        typography = TrainFlowTypography,
        content = content
    )
}
