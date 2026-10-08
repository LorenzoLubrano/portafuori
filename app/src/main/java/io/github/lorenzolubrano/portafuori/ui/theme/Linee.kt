package io.github.lorenzolubrano.portafuori.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lorenzolubrano.portafuori.R

val Atkinson = FontFamily(
    listOf(400, 700, 800).map { w -> Font(R.font.atkinson_next, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) },
)

private fun a(w: Int, size: Int, line: Int) = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight(w), fontSize = size.sp, lineHeight = line.sp)

val LineeType = Typography(
    displayLarge = a(800, 48, 54), displayMedium = a(800, 40, 46), displaySmall = a(800, 34, 40),
    headlineLarge = a(800, 30, 36), headlineMedium = a(800, 28, 34), headlineSmall = a(800, 24, 30),
    titleLarge = a(700, 21, 27), titleMedium = a(700, 17, 23), titleSmall = a(700, 15, 20),
    bodyLarge = a(400, 17, 24), bodyMedium = a(400, 15, 21), bodySmall = a(400, 13, 18),
    labelLarge = a(700, 16, 20), labelMedium = a(700, 13, 17), labelSmall = a(700, 12, 16),
)

val LineeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun c(v: Long) = Color(v)

fun SchemeColors.toColorScheme(dark: Boolean): ColorScheme {
    val tint = c(surface) // no tonal tint: the palette is explicit
    return if (dark) darkColorScheme(
        primary = c(primary), onPrimary = c(onPrimary), primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
        inversePrimary = c(inversePrimary), secondary = c(secondary), onSecondary = c(onSecondary),
        secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
        tertiary = c(tertiary), onTertiary = c(onTertiary), tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
        background = c(background), onBackground = c(onBackground), surface = c(surface), onSurface = c(onSurface),
        surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant), surfaceTint = tint,
        inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface),
        error = c(error), onError = c(onError), errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer),
        outline = c(outline), outlineVariant = c(outlineVariant),
        surfaceContainer = c(surfaceContainer), surfaceContainerHigh = c(surfaceContainerHigh), surfaceContainerHighest = c(surfaceContainerHighest),
        surfaceContainerLow = c(surfaceContainerLow), surfaceContainerLowest = c(surfaceContainerLowest),
    ) else lightColorScheme(
        primary = c(primary), onPrimary = c(onPrimary), primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
        inversePrimary = c(inversePrimary), secondary = c(secondary), onSecondary = c(onSecondary),
        secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
        tertiary = c(tertiary), onTertiary = c(onTertiary), tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
        background = c(background), onBackground = c(onBackground), surface = c(surface), onSurface = c(onSurface),
        surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant), surfaceTint = tint,
        inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface),
        error = c(error), onError = c(onError), errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer),
        outline = c(outline), outlineVariant = c(outlineVariant),
        surfaceContainer = c(surfaceContainer), surfaceContainerHigh = c(surfaceContainerHigh), surfaceContainerHighest = c(surfaceContainerHighest),
        surfaceContainerLow = c(surfaceContainerLow), surfaceContainerLowest = c(surfaceContainerLowest),
    )
}

val LineeExtraLight = ExtraColors(shell = Color(0xFF0E1A33), onShell = Color(0xFFF3F5F9), line = Color(0xFFC3CAD8), stationFrame = Color(0xFF0E1A33), success = Color(0xFF1E7A55))
val LineeExtraDark = ExtraColors(shell = Color(0xFF060E22), onShell = Color(0xFFEEF2F9), line = Color(0xFF2C3C5E), stationFrame = Color(0xFFEEF2F9), success = Color(0xFF7FD1B0))

val LineeStyle = PortafuoriStyle(
    name = "Linee",
    light = LineePalette.light.toColorScheme(dark = false),
    dark = LineePalette.dark.toColorScheme(dark = true),
    lightExtra = LineeExtraLight,
    darkExtra = LineeExtraDark,
    typography = LineeType,
    shapes = LineeShapes,
)
