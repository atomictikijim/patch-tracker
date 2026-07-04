package com.prolocity.patchtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LeagueBlueLight,
    onPrimary = LeagueNavy,
    primaryContainer = LeagueBlueContainerDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFB0BEC5),
    onSecondary = LeagueNavy,
    secondaryContainer = LeagueSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFD3E3EC),
    tertiary = LeagueGoldLight,
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = LeagueGoldContainerDark,
    onTertiaryContainer = LeagueGoldContainerLight,
    error = LeagueRedLight,
    onError = Color.White,
    errorContainer = LeagueRedContainerDark,
    onErrorContainer = LeagueRedContainerLight,
    background = Color(0xFF0A1A2A),
    onBackground = Color(0xFFE1E6EB),
    surface = Color(0xFF10263D),
    onSurface = Color(0xFFE1E6EB),
    surfaceVariant = Color(0xFF1B3A5A),
    onSurfaceVariant = Color(0xFFC4CCD3),
    outline = Color(0xFF8D9399),
    outlineVariant = Color(0xFF3F454B),
    surfaceTint = LeagueBlueLight,
    inverseSurface = Color(0xFFE1E6EB),
    inverseOnSurface = Color(0xFF10263D),
    inversePrimary = LeagueBlue,
    surfaceContainerLowest = LeagueSurfaceContainerLowestDark,
    surfaceContainerLow = LeagueSurfaceContainerLowDark,
    surfaceContainer = LeagueSurfaceContainerDark,
    surfaceContainerHigh = LeagueSurfaceContainerHighDark,
    surfaceContainerHighest = LeagueSurfaceContainerHighestDark,
    surfaceBright = LeagueSurfaceBrightDark,
    surfaceDim = LeagueSurfaceDimDark
)

private val LightColorScheme = lightColorScheme(
    primary = LeagueBlue,
    onPrimary = Color.White,
    primaryContainer = LeagueBlueContainerLight,
    onPrimaryContainer = LeagueNavy,
    secondary = LeagueGray,
    onSecondary = Color.White,
    secondaryContainer = LeagueSecondaryContainerLight,
    onSecondaryContainer = Color(0xFF1B2630),
    tertiary = LeagueGold,
    onTertiary = Color.White,
    tertiaryContainer = LeagueGoldContainerLight,
    onTertiaryContainer = Color(0xFF3F2E00),
    error = LeagueRed,
    onError = Color.White,
    errorContainer = LeagueRedContainerLight,
    onErrorContainer = LeagueOnRedContainerLight,
    background = Color(0xFFF7F9FB),
    onBackground = Color(0xFF14181C),
    surface = Color.White,
    onSurface = Color(0xFF14181C),
    surfaceVariant = LeagueGraySurface,
    onSurfaceVariant = Color(0xFF43474C),
    outline = Color(0xFF74777C),
    outlineVariant = Color(0xFFC4C7CB),
    surfaceTint = LeagueBlue,
    inverseSurface = Color(0xFF2B3034),
    inverseOnSurface = Color(0xFFEFF1F4),
    inversePrimary = LeagueBlueLight,
    surfaceContainerLowest = LeagueSurfaceContainerLowestLight,
    surfaceContainerLow = LeagueSurfaceContainerLowLight,
    surfaceContainer = LeagueSurfaceContainerLight,
    surfaceContainerHigh = LeagueSurfaceContainerHighLight,
    surfaceContainerHighest = LeagueSurfaceContainerHighestLight,
    surfaceBright = LeagueSurfaceBrightLight,
    surfaceDim = LeagueSurfaceDimLight
)

@Composable
fun PatchTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is off by default so the app keeps its own league-blue identity
    // instead of tinting to the user's wallpaper.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
