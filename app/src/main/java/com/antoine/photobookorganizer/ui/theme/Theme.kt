package com.antoine.photobookorganizer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Terracotta40,
    onPrimary = WarmPaper,
    secondary = Sage40,
    onSecondary = WarmPaper,
    background = WarmPaper,
    onBackground = WarmInk,
    surface = WarmSurface,
    onSurface = WarmInk,
    surfaceVariant = WarmSurface,
    onSurfaceVariant = WarmInk
)

private val DarkColors = darkColorScheme(
    primary = Terracotta80,
    onPrimary = Darkroom,
    secondary = Sage80,
    onSecondary = Darkroom,
    background = Darkroom,
    onBackground = DarkroomInk,
    surface = DarkroomSurface,
    onSurface = DarkroomInk,
    surfaceVariant = DarkroomSurface,
    onSurfaceVariant = DarkroomInk
)

@Composable
fun PhotobookOrganizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
