package com.marcm.cronicasapetito.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A4E2D),
    onPrimary = Color.White,
    secondary = Color(0xFFB7763D),
    background = Color(0xFFFFF8EE),
    surface = Color(0xFFFFF1DC),
    onBackground = Color(0xFF2A1B0E),
    onSurface = Color(0xFF2A1B0E)
)

@Composable
fun CronicasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
