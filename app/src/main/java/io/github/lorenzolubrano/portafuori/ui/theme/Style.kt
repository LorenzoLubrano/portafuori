package io.github.lorenzolubrano.portafuori.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colours Material has no role for. */
data class ExtraColors(
    /** Top bar and tab bar. */
    val shell: Color,
    val onShell: Color,
    /** The neutral line the stops sit on, separators. */
    val line: Color,
    /** Frame of the station (tonight's container). */
    val stationFrame: Color,
    /** "It worked" green, readable on the surfaces. */
    val success: Color,
)

/** One visual style: everything components read instead of hard-coded values. */
data class PortafuoriStyle(
    val name: String,
    val light: ColorScheme,
    val dark: ColorScheme,
    val lightExtra: ExtraColors,
    val darkExtra: ExtraColors,
    val typography: Typography,
    val shapes: Shapes,
)

data class Tokens(val extra: ExtraColors, val dark: Boolean)

val LocalTokens = staticCompositionLocalOf { Tokens(LineeExtraLight, dark = false) }

object Styles {
    val Linee = LineeStyle
    /** Today's look, kept for the second phase (not selectable yet). */
    val Standard = StandardStyle
    /** The style the app ships with. Phase two reads it from the settings. */
    val current: PortafuoriStyle get() = Linee
}
