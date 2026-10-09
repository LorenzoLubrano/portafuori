package io.github.lorenzolubrano.portafuori.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {
    private val schemes = mapOf(
        "chiaro" to LineePalette.light, "scuro" to LineePalette.dark,
        "originale chiaro" to OriginalePalette.light, "originale scuro" to OriginalePalette.dark,
        "android chiaro" to AndroidFallbackPalette.light, "android scuro" to AndroidFallbackPalette.dark,
    )

    /** Material picks the content colour by matching the background value: equal backgrounds must want the same content. */
    @Test fun equalBackgroundsShareTheirContentColour() {
        schemes.forEach { (name, s) ->
            s.backgroundPairs.groupBy { it.first }.forEach { (bg, pairs) ->
                assertEquals("$name %08X".format(bg), 1, pairs.map { it.second }.distinct().size)
            }
        }
    }

    @Test fun everyTextPairReaches45to1() {
        schemes.forEach { (name, s) ->
            s.backgroundPairs.forEach { (bg, fg) ->
                assertTrue("$name %08X su %08X".format(fg, bg), Contrast.ratio(bg, fg) >= 4.5)
            }
            // secondary text on every container it appears on
            listOf(s.background, s.surfaceContainerLowest, s.surfaceContainer, s.surfaceContainerHigh).forEach { bg ->
                assertTrue("$name secondario su %08X".format(bg), Contrast.ratio(bg, s.onSurfaceVariant) >= 4.5)
            }
        }
    }

    /** Material draws switch, field, outlined-button and chip borders with outline: WCAG 1.4.11 wants 3:1. */
    @Test fun controlBordersReach3to1() {
        schemes.forEach { (name, s) ->
            listOf(s.background, s.surfaceContainerLowest, s.surfaceContainerHighest).forEach { bg ->
                assertTrue("$name outline su %08X".format(bg), Contrast.ratio(bg, s.outline) >= 3.0)
            }
        }
    }
}
