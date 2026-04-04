package com.example.superiorcontroller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GamepadColorScheme = darkColorScheme(
    primary = GamepadPrimary,
    secondary = Teal40,
    tertiary = Cyan40,
    background = GamepadBackground,
    surface = GamepadSurface,
    surfaceVariant = GamepadSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFAAAAAA)
)

@Composable
fun SuperiorControllerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GamepadColorScheme,
        typography = Typography,
        content = content
    )
}
