package io.github.lorenzolubrano.portafuori.ui.theme

/** ARGB values of one colour scheme, kept as numbers so JVM tests can check them. */
data class SchemeColors(
    val primary: Long, val onPrimary: Long,
    val primaryContainer: Long, val onPrimaryContainer: Long,
    val secondary: Long, val onSecondary: Long,
    val secondaryContainer: Long, val onSecondaryContainer: Long,
    val tertiary: Long, val onTertiary: Long,
    val tertiaryContainer: Long, val onTertiaryContainer: Long,
    val error: Long, val onError: Long,
    val errorContainer: Long, val onErrorContainer: Long,
    val background: Long, val onBackground: Long,
    val surface: Long, val onSurface: Long,
    val surfaceVariant: Long, val onSurfaceVariant: Long,
    val surfaceContainerLowest: Long, val surfaceContainerLow: Long, val surfaceContainer: Long,
    val surfaceContainerHigh: Long, val surfaceContainerHighest: Long,
    val inverseSurface: Long, val inverseOnSurface: Long, val inversePrimary: Long,
    val outline: Long, val outlineVariant: Long,
) {
    /** Every role used as a background, with the content colour Material's contentColorFor gives it. */
    val backgroundPairs: List<Pair<Long, Long>>
        get() = listOf(
            primary to onPrimary, secondary to onSecondary, tertiary to onTertiary, error to onError,
            primaryContainer to onPrimaryContainer, secondaryContainer to onSecondaryContainer,
            tertiaryContainer to onTertiaryContainer, errorContainer to onErrorContainer,
            background to onBackground, surface to onSurface, surfaceVariant to onSurfaceVariant,
            surfaceContainerLowest to onSurface, surfaceContainerLow to onSurface, surfaceContainer to onSurface,
            surfaceContainerHigh to onSurface, surfaceContainerHighest to onSurface,
            inverseSurface to inverseOnSurface,
        )
}

/** «Linee»: porcelain enamel by day, midnight-blue enamel by night. */
object LineePalette {
    val light = SchemeColors(
        primary = 0xFF0E1A33, onPrimary = 0xFFF3F5F9,
        primaryContainer = 0xFFDCE4F3, onPrimaryContainer = 0xFF0E1A33,
        secondary = 0xFF4A5671, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFE9EDF5, onSecondaryContainer = 0xFF0E1A33,
        tertiary = 0xFF8A4B00, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFFFE9C2, onTertiaryContainer = 0xFF2B1700,
        error = 0xFFB3261E, onError = 0xFFFFFFFF,
        errorContainer = 0xFFF9DEDC, onErrorContainer = 0xFF410E0B,
        background = 0xFFF3F5F9, onBackground = 0xFF0E1A33,
        surface = 0xFFF3F5F9, onSurface = 0xFF0E1A33,
        surfaceVariant = 0xFFE6EBF4, onSurfaceVariant = 0xFF4A5671,
        surfaceContainerLowest = 0xFFFFFFFF, surfaceContainerLow = 0xFFF0F3F8, surfaceContainer = 0xFFECEFF5,
        surfaceContainerHigh = 0xFFE3E8F2, surfaceContainerHighest = 0xFFD9E0EE,
        inverseSurface = 0xFF0E1A33, inverseOnSurface = 0xFFF3F5F9, inversePrimary = 0xFFF3F5F9,
        outline = 0xFF6E788F, outlineVariant = 0xFFD5DBE6,
    )
    val dark = SchemeColors(
        primary = 0xFFEEF2F9, onPrimary = 0xFF0B1630,
        primaryContainer = 0xFF1F3260, onPrimaryContainer = 0xFFEEF2F9,
        secondary = 0xFFA9B4CA, onSecondary = 0xFF0B1630,
        secondaryContainer = 0xFF24375F, onSecondaryContainer = 0xFFEEF2F9,
        tertiary = 0xFFFFC46B, onTertiary = 0xFF2B1700,
        tertiaryContainer = 0xFF3A2A00, onTertiaryContainer = 0xFFFFE2A8,
        error = 0xFFFF8A80, onError = 0xFF3B1210,
        errorContainer = 0xFF5C1A16, onErrorContainer = 0xFFFFDAD6,
        background = 0xFF0B1630, onBackground = 0xFFEEF2F9,
        surface = 0xFF0B1630, onSurface = 0xFFEEF2F9,
        surfaceVariant = 0xFF16264A, onSurfaceVariant = 0xFFA9B4CA,
        surfaceContainerLowest = 0xFF12213F, surfaceContainerLow = 0xFF0F1C38, surfaceContainer = 0xFF132346,
        surfaceContainerHigh = 0xFF1A2B50, surfaceContainerHighest = 0xFF22355E,
        inverseSurface = 0xFF060E22, inverseOnSurface = 0xFFEEF2F9, inversePrimary = 0xFF060E22,
        outline = 0xFF7686A9, outlineVariant = 0xFF233252,
    )
}

/** «Originale»: the 1.0.x look (warm paper neutrals, bottle green). Light outline darkened from #86908A to reach 3:1. */
object OriginalePalette {
    val light = SchemeColors(
        primary = 0xFF1E5E4A, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFCDEBDD, onPrimaryContainer = 0xFF07261B,
        secondary = 0xFF5D6B63, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFE2E8E2, onSecondaryContainer = 0xFF1A241F,
        tertiary = 0xFF9A5B00, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFFFE2BC, onTertiaryContainer = 0xFF2F1800,
        error = 0xFFB3261E, onError = 0xFFFFFFFF,
        errorContainer = 0xFFF9DEDC, onErrorContainer = 0xFF410E0B,
        background = 0xFFF6F3EC, onBackground = 0xFF1B2420,
        surface = 0xFFF6F3EC, onSurface = 0xFF1B2420,
        surfaceVariant = 0xFFE4E1D8, onSurfaceVariant = 0xFF55605A,
        surfaceContainerLowest = 0xFFFFFFFF, surfaceContainerLow = 0xFFF1EEE6, surfaceContainer = 0xFFECE9E0,
        surfaceContainerHigh = 0xFFE6E3DA, surfaceContainerHighest = 0xFFE0DDD4,
        inverseSurface = 0xFF2F3632, inverseOnSurface = 0xFFEFF1ED, inversePrimary = 0xFF7FD1B0,
        outline = 0xFF6E7872, outlineVariant = 0xFFD3D0C6,
    )
    val dark = SchemeColors(
        primary = 0xFF7FD1B0, onPrimary = 0xFF003826,
        primaryContainer = 0xFF0F4A38, onPrimaryContainer = 0xFFBFEEDB,
        secondary = 0xFFB9C7BF, onSecondary = 0xFF24322B,
        secondaryContainer = 0xFF3A4841, onSecondaryContainer = 0xFFD5E3DA,
        tertiary = 0xFFFFB95C, onTertiary = 0xFF4A2800,
        tertiaryContainer = 0xFF6A3C00, onTertiaryContainer = 0xFFFFDDB6,
        error = 0xFFF2B8B5, onError = 0xFF601410,
        errorContainer = 0xFF8C1D18, onErrorContainer = 0xFFF9DEDC,
        background = 0xFF111714, onBackground = 0xFFE3E8E4,
        surface = 0xFF111714, onSurface = 0xFFE3E8E4,
        surfaceVariant = 0xFF3F4944, onSurfaceVariant = 0xFFBEC9C2,
        surfaceContainerLowest = 0xFF0C110F, surfaceContainerLow = 0xFF171E1B, surfaceContainer = 0xFF1B2420,
        surfaceContainerHigh = 0xFF252E2A, surfaceContainerHighest = 0xFF303935,
        inverseSurface = 0xFFE3E8E4, inverseOnSurface = 0xFF2B322E, inversePrimary = 0xFF1E5E4A,
        outline = 0xFF89938D, outlineVariant = 0xFF3F4944,
    )
}

/** «Android» before Android 12 (no wallpaper colours): a fixed Material blue. */
object AndroidFallbackPalette {
    val light = SchemeColors(
        primary = 0xFF305DA8, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFD7E2FF, onPrimaryContainer = 0xFF001A41,
        secondary = 0xFF565E71, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFDAE2F9, onSecondaryContainer = 0xFF131C2B,
        tertiary = 0xFF705575, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFFAD8FD, onTertiaryContainer = 0xFF28132E,
        error = 0xFFBA1A1A, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFFDAD6, onErrorContainer = 0xFF410002,
        background = 0xFFF9F9FF, onBackground = 0xFF1A1B20,
        surface = 0xFFF9F9FF, onSurface = 0xFF1A1B20,
        surfaceVariant = 0xFFE0E2EC, onSurfaceVariant = 0xFF44474F,
        surfaceContainerLowest = 0xFFFFFFFF, surfaceContainerLow = 0xFFF3F3FA, surfaceContainer = 0xFFEDEDF4,
        surfaceContainerHigh = 0xFFE7E8EE, surfaceContainerHighest = 0xFFE2E2E9,
        inverseSurface = 0xFF2F3036, inverseOnSurface = 0xFFF0F0F7, inversePrimary = 0xFFADC6FF,
        outline = 0xFF74777F, outlineVariant = 0xFFC4C6D0,
    )
    val dark = SchemeColors(
        primary = 0xFFADC6FF, onPrimary = 0xFF002E69,
        primaryContainer = 0xFF144694, onPrimaryContainer = 0xFFD7E2FF,
        secondary = 0xFFBEC6DC, onSecondary = 0xFF283141,
        secondaryContainer = 0xFF3E4759, onSecondaryContainer = 0xFFDAE2F9,
        tertiary = 0xFFDDBCE0, onTertiary = 0xFF3F2844,
        tertiaryContainer = 0xFF573E5C, onTertiaryContainer = 0xFFFAD8FD,
        error = 0xFFFFB4AB, onError = 0xFF690005,
        errorContainer = 0xFF93000A, onErrorContainer = 0xFFFFDAD6,
        background = 0xFF111318, onBackground = 0xFFE2E2E9,
        surface = 0xFF111318, onSurface = 0xFFE2E2E9,
        surfaceVariant = 0xFF44474F, onSurfaceVariant = 0xFFC4C6D0,
        surfaceContainerLowest = 0xFF0C0E13, surfaceContainerLow = 0xFF1A1B20, surfaceContainer = 0xFF1E1F25,
        surfaceContainerHigh = 0xFF282A2F, surfaceContainerHighest = 0xFF33353A,
        inverseSurface = 0xFFE2E2E9, inverseOnSurface = 0xFF2F3036, inversePrimary = 0xFF305DA8,
        outline = 0xFF8E9099, outlineVariant = 0xFF44474F,
    )
}
