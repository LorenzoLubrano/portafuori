package io.github.lorenzolubrano.portafuori.data

import java.time.LocalDate

/**
 * Hard limits for anything the app stores. Real calendars stay far below them; they exist so a
 * crafted import (or a huge paste) can never create a row SQLite cannot read back (2 MB CursorWindow)
 * or data that makes the rules engine slow on every launch.
 */
object Limits {
    const val PROFILE_NAME = 60
    const val AREA = 120
    const val NOTES = 2000
    const val BIN_NAME = 40
    const val HOLIDAY_NAME = 60

    const val PROFILES = 20
    const val BINS = 30
    const val RULES_PER_BIN = 20
    const val DATES_PER_RULE = 400
    const val EXCEPTIONS = 2000
    const val HOLIDAYS = 100
    const val MAX_INTERVAL_WEEKS = 52

    /** Any text or file we are asked to import (a real backup is a few KB). */
    const val IMPORT_BYTES = 1 shl 20
    /** Decompressed JSON inside a QR/chat code. */
    const val INFLATED_BYTES = 512 * 1024

    val MIN_DATE: LocalDate = LocalDate.of(2000, 1, 1)
    val MAX_DATE: LocalDate = LocalDate.of(2100, 12, 31)
}

fun String.clamp(max: Int): String = if (length <= max) this else take(max)

fun ProfileEntity.sanitized() = copy(
    name = name.trim().clamp(Limits.PROFILE_NAME).ifBlank { "Casa" },
    areaNote = areaNote.clamp(Limits.AREA),
    notes = notes.clamp(Limits.NOTES),
)

fun BinEntity.sanitized() = copy(
    name = name.trim().clamp(Limits.BIN_NAME).ifBlank { "Bidone" },
    iconKey = iconKey.takeIf { it in Presets.icons } ?: "bag",
    presetKey = Presets.byKey(presetKey)?.key,
    colorArgb = (colorArgb and 0xFFFFFF) or 0xFF000000,
)

fun HolidayEntity.sanitized() = copy(name = name.trim().clamp(Limits.HOLIDAY_NAME).ifBlank { "Festività" })
