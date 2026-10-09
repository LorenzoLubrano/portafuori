package io.github.lorenzolubrano.portafuori.reminders

import io.github.lorenzolubrano.portafuori.data.StyleId

/** The notification accent follows the style; «Android» takes the system accent from Android 12 ([systemAccent]). */
fun accentFor(style: StyleId, systemAccent: Int?): Int = when (style) {
    StyleId.LINEE -> 0xFF0E1A33.toInt()
    StyleId.ORIGINALE -> 0xFF1E5E4A.toInt()
    StyleId.ANDROID -> systemAccent ?: 0xFF305DA8.toInt()
}
