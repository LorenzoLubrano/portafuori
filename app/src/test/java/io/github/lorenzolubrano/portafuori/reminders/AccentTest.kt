package io.github.lorenzolubrano.portafuori.reminders

import io.github.lorenzolubrano.portafuori.data.StyleId
import org.junit.Assert.assertEquals
import org.junit.Test

class AccentTest {
    @Test fun accentFollowsTheStyle() {
        assertEquals(0xFF0E1A33.toInt(), accentFor(StyleId.LINEE, systemAccent = null))
        assertEquals(0xFF1E5E4A.toInt(), accentFor(StyleId.ORIGINALE, systemAccent = null))
        assertEquals(0xFF305DA8.toInt(), accentFor(StyleId.ANDROID, systemAccent = null))
        assertEquals(0xFF123456.toInt(), accentFor(StyleId.ANDROID, systemAccent = 0xFF123456.toInt()))
    }
}
