package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SophisticatedPurplePrimary,
    onPrimary = Color(0xFF381E72),
    secondary = Color(0xFFEFB8C8),
    tertiary = Color(0xFF8CF1FF),
    background = SophisticatedDarkBg,
    surface = SophisticatedDarkSurface,
    surfaceVariant = SophisticatedDarkElevated,
    onBackground = SophisticatedTextMain,
    onSurface = SophisticatedTextMain,
    onSurfaceVariant = SophisticatedTextMuted,
    outline = SophisticatedBorder,
    primaryContainer = SophisticatedPurplePrimary.copy(alpha = 0.2f),
    secondaryContainer = Color(0xFFEFB8C8).copy(alpha = 0.2f)
)

private val LightColorScheme = lightColorScheme(
    primary = NeonIndigo,
    secondary = MintEmerald,
    tertiary = SunsetCoral,
    background = LightBg,
    surface = LightSurface,
    onBackground = TextDark,
    onSurface = TextDark,
    primaryContainer = NeonIndigo.copy(alpha = 0.1f),
    secondaryContainer = MintEmerald.copy(alpha = 0.1f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to gorgeous dark mode as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
