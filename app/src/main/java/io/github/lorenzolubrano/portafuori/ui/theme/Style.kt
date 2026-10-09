package io.github.lorenzolubrano.portafuori.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.lorenzolubrano.portafuori.data.StyleId

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

data class Tokens(val extra: ExtraColors, val dark: Boolean, val style: PortafuoriStyle)

val LocalTokens = staticCompositionLocalOf { Tokens(LineeExtraLight, dark = false, style = LineeStyle) }

object Styles {
    val Linee = LineeStyle
    /** The 1.0.x look, «Originale». */
    val Originale = OriginaleStyle

    /** The style for [id]; «Android» takes the wallpaper colours from Android 12 on, the fixed blue before. */
    fun of(id: StyleId, context: Context): PortafuoriStyle = when (id) {
        StyleId.LINEE -> Linee
        StyleId.ORIGINALE -> Originale
        StyleId.ANDROID -> if (Build.VERSION.SDK_INT >= 31) {
            androidStyle(dynamicLightColorScheme(context), dynamicDarkColorScheme(context))
        } else AndroidFallbackStyle
    }
}
