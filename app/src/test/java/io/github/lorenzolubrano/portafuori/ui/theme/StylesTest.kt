package io.github.lorenzolubrano.portafuori.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylesTest {
    /** Review Focus 1: Linee's shell is dark by day too; Originale and Android have a light shell in the light theme. */
    @Test fun barIconsFollowTheShell() {
        assertTrue(lightBarIcons(Styles.Linee, dark = false))
        assertFalse(lightBarIcons(Styles.Originale, dark = false))
        assertTrue(lightBarIcons(Styles.Originale, dark = true))
        assertFalse(lightBarIcons(AndroidFallbackStyle, dark = false))
        assertTrue(lightBarIcons(AndroidFallbackStyle, dark = true))
    }
}
