package com.liujyks.trainflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

internal fun trainFlowLightColorSchemeForSkin(skin: TrainFlowSkin) = lightColorScheme(
    primary = skin.tokens.primary,
    onPrimary = skin.tokens.neutral50,
    secondary = skin.tokens.secondary,
    onSecondary = skin.tokens.neutral50,
    tertiary = skin.tokens.action,
    onTertiary = skin.tokens.neutral50,
    background = skin.tokens.surfaceMuted,
    onBackground = skin.tokens.neutral900,
    surface = skin.tokens.surface,
    onSurface = skin.tokens.neutral900,
    surfaceVariant = skin.tokens.neutral100,
    onSurfaceVariant = skin.tokens.neutral700,
    outline = skin.tokens.neutral200,
    error = skin.tokens.error
)

@Composable
fun TrainFlowTheme(
    skin: TrainFlowSkin = SkinRegistry.defaultSkin,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = trainFlowLightColorSchemeForSkin(skin),
        typography = TrainFlowTypography,
        content = content
    )
}
