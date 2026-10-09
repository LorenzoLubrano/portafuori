package io.github.lorenzolubrano.portafuori.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StyleIdTest {
    /** Upgrades from 1.0.7 have no stored style; an unknown value (an older or newer build) must not crash. */
    @Test fun missingOrUnknownIsLinee() {
        assertEquals(StyleId.LINEE, StyleId.parse(null))
        assertEquals(StyleId.LINEE, StyleId.parse("STANDARD"))
        assertEquals(StyleId.ORIGINALE, StyleId.parse("ORIGINALE"))
        assertEquals(StyleId.ANDROID, StyleId.parse("ANDROID"))
    }
}
