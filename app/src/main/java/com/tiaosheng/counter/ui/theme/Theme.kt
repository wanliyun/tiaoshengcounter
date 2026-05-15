package com.tiaosheng.counter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = SportOrange,
    secondary = SportGreen,
    tertiary = PauseYellow,
    error = ErrorRed,
    background = SurfaceDark,
    surface = SurfaceDark,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = SurfaceDark,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onError = TextWhite
)

@Composable
fun TiaoshengTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = TiaoshengTypography,
        content = content
    )
}
