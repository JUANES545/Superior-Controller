package com.example.superiorcontroller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GamepadColorScheme = darkColorScheme(
    primary = GamepadPrimary,
    secondary = GamepadSecondary,
    tertiary = GamepadTertiary,
    background = GamepadBackground,
    surface = GamepadSurface,
    surfaceVariant = GamepadSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D)
)

@Composable
fun SuperiorControllerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GamepadColorScheme,
        typography = Typography,
        content = content
    )
}
