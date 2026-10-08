package io.github.lorenzolubrano.portafuori.ui.theme

import kotlin.math.pow

/** WCAG luminance and contrast on plain ARGB numbers, so the palette can be checked on the JVM. */
object Contrast {
    const val DARK_INK = 0xFF141414
    const val WHITE = 0xFFFFFFFF

    fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun ratio(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** Ink for text and icons on a bin colour the user picked: whichever of near-black and white reads better. */
    fun onColor(argb: Long): Long = if (ratio(argb, DARK_INK) >= ratio(argb, WHITE)) DARK_INK else WHITE

    /** A bin circle too close to its surface needs a dark edge to stay visible (3:1 for graphics). */
    fun needsEdge(color: Long, surface: Long): Boolean = ratio(color, surface) < 3.0
}
