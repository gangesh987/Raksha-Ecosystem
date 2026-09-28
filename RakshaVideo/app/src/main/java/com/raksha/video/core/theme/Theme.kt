package com.raksha.video.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF166534),
    onPrimary = Color.White,
    secondary = Color(0xFF0F766E),
    background = Color(0xFFF7F9F7),
    surface = Color.White,
    error = Color(0xFFB42318)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEA9B),
    secondary = Color(0xFF5DD8CF),
    background = Color(0xFF0B0F14),
    surface = Color(0xFF121820),
    error = Color(0xFFFF8A80)
)

@Composable
fun RakshaVideoTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
