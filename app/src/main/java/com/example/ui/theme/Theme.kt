package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val StageDarkColorScheme = darkColorScheme(
    primary = BroadcastAmber,
    onPrimary = OnBroadcastAmber,
    primaryContainer = BroadcastAmberContainer,
    onPrimaryContainer = BroadcastAmberGlow,
    secondary = CueMusicCyan,
    onSecondary = StagePureBlack,
    secondaryContainer = StageSurfaceElevated,
    onSecondaryContainer = CueMusicCyan,
    tertiary = CueChorusCoral,
    onTertiary = StagePureBlack,
    background = StagePureBlack,
    onBackground = StageTextPrimary,
    surface = StageObsidian,
    onSurface = StageTextPrimary,
    surfaceVariant = StageSurface,
    onSurfaceVariant = StageTextSecondary,
    surfaceContainerLowest = StagePureBlack,
    surfaceContainerLow = StageObsidian,
    surfaceContainer = StageSurface,
    surfaceContainerHigh = StageSurfaceElevated,
    surfaceContainerHighest = StageSurfaceHighest,
    outline = StageBorder,
    outlineVariant = StageBorderSubtle,
    error = StageDanger,
    onError = StagePureBlack,
    errorContainer = StageDangerContainer,
    onErrorContainer = StageDanger
)

private val StageShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StagePromptTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StageDarkColorScheme,
        typography = StagePromptTypography,
        shapes = StageShapes,
        content = content
    )
}
