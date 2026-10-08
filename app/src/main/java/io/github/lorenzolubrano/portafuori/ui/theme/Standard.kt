package io.github.lorenzolubrano.portafuori.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The look before «Linee» (1.0.x): warm "paper" neutrals and bottle green. Kept as the «Standard» style for phase two.
private val StandardLight = lightColorScheme(
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

private val StandardDark = darkColorScheme(
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
private val StandardType = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

val StandardStyle = PortafuoriStyle(
    name = "Standard",
    light = StandardLight,
    dark = StandardDark,
    lightExtra = ExtraColors(shell = Color(0xFFF6F3EC), onShell = Color(0xFF1B2420), line = Color(0xFFD3D0C6), stationFrame = Color(0xFFCDEBDD), success = Color(0xFF1E7A55)),
    darkExtra = ExtraColors(shell = Color(0xFF111714), onShell = Color(0xFFE3E8E4), line = Color(0xFF3F4944), stationFrame = Color(0xFF0F4A38), success = Color(0xFF7FD1B0)),
    typography = StandardType,
    shapes = Shapes(),
)
