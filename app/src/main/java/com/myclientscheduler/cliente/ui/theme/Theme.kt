package com.myclientscheduler.cliente.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Gold = Color(0xFFC9A24D)
private val Ivory = Color(0xFFFFF9EE)
private val Ink = Color(0xFF111111)

private val LightColors = lightColorScheme(
    primary = Gold,
    onPrimary = Ink,
    background = Ivory,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Gold,
    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFF3EBDD),
    surface = Color(0xFF151515),
    onSurface = Color(0xFFF3EBDD),
)

@Composable
fun ClienteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = ClienteTypography,
        content = content,
    )
}
