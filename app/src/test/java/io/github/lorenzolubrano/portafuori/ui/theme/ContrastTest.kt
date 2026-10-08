package io.github.lorenzolubrano.portafuori.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    @Test fun blackOnWhiteIs21() = assertEquals(21.0, Contrast.ratio(0xFF000000, 0xFFFFFFFF), 0.01)

    @Test fun textOnBinColoursPicksTheReadableInk() {
        assertEquals(Contrast.DARK_INK, Contrast.onColor(0xFFF2C230)) // giallo plastica
        assertEquals(Contrast.WHITE, Contrast.onColor(0xFF8D5B3A))    // marrone organico
        assertEquals(Contrast.WHITE, Contrast.onColor(0xFF2F6FD6))    // blu carta
        assertEquals(Contrast.DARK_INK, Contrast.onColor(0xFF8E6CC1)) // viola: il nero legge meglio
    }

    @Test fun iconOnAnyPresetReaches3to1() {
        val presets = listOf(0xFF8D5B3A, 0xFFF2C230, 0xFF2F6FD6, 0xFF2E9E5B, 0xFF6B7280, 0xFF1FA2C7, 0xFF6E8B3D, 0xFF8E6CC1, 0xFFD64545, 0xFFE67E22, 0xFF222222, 0xFFB0B7C3)
        presets.forEach { c -> assertTrue("%08X".format(c), Contrast.ratio(c, Contrast.onColor(c)) >= 3.0) }
    }

    /** Review Focus 2: a pale bin on white enamel gets a dark edge, so the circle reaches 3:1 against the surface (WCAG 1.4.11). */
    @Test fun lightBinStillHasRing() {
        val white = LineePalette.light.surfaceContainerLowest
        assertTrue(Contrast.needsEdge(0xFFB0B7C3, white)) // grigio chiaro
        assertTrue(Contrast.needsEdge(0xFFFFFFFF, white)) // bianco
        assertTrue(Contrast.needsEdge(0xFFF2C230, white)) // giallo plastica
        assertTrue(!Contrast.needsEdge(0xFF2F6FD6, white)) // blu: basta il colore
        assertTrue(Contrast.ratio(LineePalette.light.onSurfaceVariant, white) >= 3.0) // the edge colour itself
        assertTrue(Contrast.ratio(LineePalette.dark.onSurfaceVariant, LineePalette.dark.surfaceContainerLowest) >= 3.0)
    }
}
