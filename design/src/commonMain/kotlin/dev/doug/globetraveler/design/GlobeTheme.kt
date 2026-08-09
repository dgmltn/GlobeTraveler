package dev.doug.globetraveler.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// background matches the bundled basemap ground colors (Positron / Dark, see
// scripts/strip-basemap-labels.py), so scrims over the map read as the map fading out.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    secondary = Color(0xFF546E7A),
    surface = Color(0xFFFAFAF7),
    background = Color(0xFFF2F3F0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0D2A0F),
    secondary = Color(0xFF90A4AE),
    surface = Color(0xFF121412),
    background = Color(0xFF0C0C0C),
)

@Composable
fun GlobeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
