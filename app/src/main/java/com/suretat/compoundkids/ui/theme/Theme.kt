package com.suretat.compoundkids.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary          = GreenMid,
    onPrimary        = androidx.compose.ui.graphics.Color.White,
    primaryContainer = GreenSurf,
    onPrimaryContainer = GreenOnSurf,
    secondary        = Amber,
    onSecondary      = androidx.compose.ui.graphics.Color.White,
    background       = Surface,
    onBackground     = OnSurf,
    surface          = Surface,
    onSurface        = OnSurf,
    outline          = Outline
)

@Composable
fun CompoundKidsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
