package com.cliente.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Gold = Color(0xFFC9A24D)
private val Ivory = Color(0xFFF5F2EA)
private val Black = Color(0xFF0A0A0A)

private val Light = lightColorScheme(
    primary = Gold,
    background = Ivory,
    surface = Ivory,
    onBackground = Black,
    onSurface = Black,
)

private val Dark = darkColorScheme(
    primary = Gold,
    background = Black,
    surface = Color(0xFF131313),
    onBackground = Ivory,
    onSurface = Ivory,
)

@Composable
fun ClienteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dark, typography = Typography, content = content)
}
