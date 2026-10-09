package io.github.lorenzolubrano.portafuori.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()
private val StandardType = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

// The look before «Linee» (1.0.x): warm "paper" neutrals and bottle green, shown as «Originale».
val OriginaleStyle = PortafuoriStyle(
    name = "Originale",
    light = OriginalePalette.light.toColorScheme(dark = false),
    dark = OriginalePalette.dark.toColorScheme(dark = true),
    lightExtra = ExtraColors(shell = Color(0xFFF6F3EC), onShell = Color(0xFF1B2420), line = Color(0xFFD3D0C6), stationFrame = Color(0xFFCDEBDD), success = Color(0xFF1E7A55)),
    darkExtra = ExtraColors(shell = Color(0xFF111714), onShell = Color(0xFFE3E8E4), line = Color(0xFF3F4944), stationFrame = Color(0xFF0F4A38), success = Color(0xFF7FD1B0)),
    typography = StandardType,
    shapes = Shapes(),
)
