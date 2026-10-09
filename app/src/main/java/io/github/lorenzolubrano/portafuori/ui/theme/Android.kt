package io.github.lorenzolubrano.portafuori.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color

/** «Android»: Material colours (from the wallpaper on Android 12+), the system font and Material shapes, like Google's apps. */
fun androidStyle(light: ColorScheme, dark: ColorScheme) = PortafuoriStyle(
    name = "Android",
    light = light,
    dark = dark,
    lightExtra = androidExtra(light, success = Color(0xFF1E7A55)),
    darkExtra = androidExtra(dark, success = Color(0xFF7FD1B0)),
    typography = Typography(),
    shapes = Shapes(),
)

private fun androidExtra(s: ColorScheme, success: Color) =
    ExtraColors(shell = s.surfaceContainer, onShell = s.onSurface, line = s.outlineVariant, stationFrame = s.primary, success = success)

/** «Android» without wallpaper colours (before Android 12): the fixed blue. */
val AndroidFallbackStyle = androidStyle(AndroidFallbackPalette.light.toColorScheme(dark = false), AndroidFallbackPalette.dark.toColorScheme(dark = true))
