package com.example.id.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppDarkColorScheme = darkColorScheme(
    primary          = AccentLavender,
    onPrimary        = BgBase,
    primaryContainer = BgCard,
    secondary        = AccentGreen,
    onSecondary      = BgBase,
    tertiary         = AccentOrange,
    onTertiary       = BgBase,
    error            = AccentRed,
    background       = BgBase,
    onBackground     = Color.White,
    surface          = BgSurface,
    onSurface        = Color.White,
    surfaceVariant   = BgCard,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    outline          = Color.White.copy(alpha = 0.12f),
)

@Composable
fun IDTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = Typography,
        content = content
    )
}
