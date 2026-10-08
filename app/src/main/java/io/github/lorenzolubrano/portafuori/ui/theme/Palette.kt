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
