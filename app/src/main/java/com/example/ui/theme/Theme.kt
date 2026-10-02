package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RevocaHubColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = OnAmber,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberVariant,
    secondary = AmberVariant,
    onSecondary = OnAmber,
    background = CharcoalNavyDark,
    onBackground = TextPrimary,
    surface = PanelNavy,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = BorderNavy
)

@Composable
fun RevocaHubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RevocaHubColorScheme,
        typography = Typography,
        content = content
    )
}

