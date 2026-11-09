package com.korentingeorget.troccampus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldLight.copy(alpha = 0.3f),
    onPrimaryContainer = EmeraldDark,
    secondary = AmberSecondary,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = TextDark,
    surface = SurfaceLight,
    onSurface = TextDark,
)

private val DarkColors = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = AmberLight,
    onSecondary = Color.Black,
    background = Color(0xFF0B1320),
    onBackground = Color.White,
    surface = Color(0xFF131D2E),
    onSurface = Color.White,
)

@Composable
fun TrocCampusTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = TrocTypography, content = content)
}
