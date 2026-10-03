package io.github.lorenzolubrano.portafuori.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.lorenzolubrano.portafuori.data.ThemeMode

// Warm "paper" neutrals so the bin colours carry the meaning; bottle green as the brand.
private val Light = lightColorScheme(
    primary = Color(0xFF1E5E4A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBDD),
    onPrimaryContainer = Color(0xFF07261B),
    secondary = Color(0xFF5D6B63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8E2),
    onSecondaryContainer = Color(0xFF1A241F),
    tertiary = Color(0xFF9A5B00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE2BC),
    onTertiaryContainer = Color(0xFF2F1800),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF6F3EC),
    onBackground = Color(0xFF1B2420),
    surface = Color(0xFFF6F3EC),
    onSurface = Color(0xFF1B2420),
    surfaceVariant = Color(0xFFE4E1D8),
    onSurfaceVariant = Color(0xFF55605A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1EEE6),
    surfaceContainer = Color(0xFFECE9E0),
    surfaceContainerHigh = Color(0xFFE6E3DA),
    surfaceContainerHighest = Color(0xFFE0DDD4),
    outline = Color(0xFF86908A),
    outlineVariant = Color(0xFFD3D0C6),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FD1B0),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF0F4A38),
    onPrimaryContainer = Color(0xFFBFEEDB),
    secondary = Color(0xFFB9C7BF),
    onSecondary = Color(0xFF24322B),
    secondaryContainer = Color(0xFF3A4841),
    onSecondaryContainer = Color(0xFFD5E3DA),
    tertiary = Color(0xFFFFB95C),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6A3C00),
    onTertiaryContainer = Color(0xFFFFDDB6),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF111714),
    onBackground = Color(0xFFE3E8E4),
    surface = Color(0xFF111714),
    onSurface = Color(0xFFE3E8E4),
    surfaceVariant = Color(0xFF3F4944),
    onSurfaceVariant = Color(0xFFBEC9C2),
    surfaceContainerLowest = Color(0xFF0C110F),
    surfaceContainerLow = Color(0xFF171E1B),
    surfaceContainer = Color(0xFF1B2420),
    surfaceContainerHigh = Color(0xFF252E2A),
    surfaceContainerHighest = Color(0xFF303935),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF3F4944),
)

private val base = Typography()
private val AppTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

val Kicker = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)

@Composable
fun PortafuoriTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = AppTypography, content = content)
}

/** Success green that works on both themes. */
@Composable
fun okColor(): Color = if (MaterialTheme.colorScheme.background.luminanceIsDark()) Color(0xFF7FD1B0) else Color(0xFF1E7A55)

private fun Color.luminanceIsDark() = (0.299 * red + 0.587 * green + 0.114 * blue) < 0.5
