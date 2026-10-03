package io.github.lorenzolubrano.portafuori

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class BrandTest {
    @Test fun linksAreHttpsToTheExpectedHosts() {
        val kofi = URI(Brand.KOFI_URL)
        assertEquals("https", kofi.scheme)
        assertEquals("ko-fi.com", kofi.host)
        assertEquals("/portafuori", kofi.path)
        val src = URI(Brand.SOURCE_URL)
        assertEquals("https", src.scheme)
        assertEquals("github.com", src.host)
        assertEquals("/LorenzoLubrano/portafuori", src.path)
    }
}
