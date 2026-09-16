package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SynapseColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = NeonCyan,
    secondary = NeonAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3E1F00),
    onSecondaryContainer = NeonAmber,
    tertiary = NeonMagenta,
    onTertiary = Color.Black,
    background = DarkObsidianBg,
    onBackground = TextPrimary,
    surface = RackFaceplateSurface,
    onSurface = TextPrimary,
    surfaceVariant = ModuleSurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = RackBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SynapseColorScheme,
        typography = Typography,
        content = content
    )
}
