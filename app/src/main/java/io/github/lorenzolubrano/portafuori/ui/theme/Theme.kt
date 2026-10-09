package io.github.lorenzolubrano.portafuori.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.lorenzolubrano.portafuori.data.ThemeMode

@Composable
fun PortafuoriTheme(mode: ThemeMode, style: PortafuoriStyle, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalTokens provides Tokens(if (dark) style.darkExtra else style.lightExtra, dark, style)) {
        MaterialTheme(colorScheme = if (dark) style.dark else style.light, typography = style.typography, shapes = style.shapes, content = content)
    }
}

@Composable
fun extra(): ExtraColors = LocalTokens.current.extra

/** The style the app is drawn with right now. */
@Composable
fun currentStyle(): PortafuoriStyle = LocalTokens.current.style

/** Success green that works on both themes. */
@Composable
fun okColor(): Color = extra().success

/** Light status and navigation bar icons: a dark theme, or a style whose shell is dark in the light theme too. */
fun lightBarIcons(style: PortafuoriStyle, dark: Boolean): Boolean = dark || style.lightExtra.shell.luminance() < 0.3f
