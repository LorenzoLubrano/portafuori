package io.github.lorenzolubrano.portafuori.widget

import io.github.lorenzolubrano.portafuori.data.StyleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class WidgetStyleTest {
    @Test fun eachStyleHasItsOwnLayouts() {
        val all = StyleId.entries.flatMap { listOf(WidgetStyle.layout(it, compact = false), WidgetStyle.layout(it, compact = true)) }
        assertEquals(6, all.toSet().size)
    }

    /** Review Focus 4: white and near-black bins keep an edge whatever theme the launcher draws the widget in. */
    @Test fun paleAndDarkBinsGetAnEdgeInEveryStyle() {
        StyleId.entries.forEach { s ->
            assertTrue("$s bianco", WidgetStyle.needsEdge(0xFFFFFFFF, s))
            assertTrue("$s nero", WidgetStyle.needsEdge(0xFF222222, s))
        }
    }

    /** The edges are decided on these values, so they must be the colours the widget is really drawn on. */
    @Test fun backgroundsMatchTheColourResources() {
        fun colours(path: String): Map<String, String> {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path)).getElementsByTagName("color")
            return (0 until nodes.length).map { nodes.item(it) as Element }.associate { it.getAttribute("name") to it.textContent.trim() }
        }
        val day = colours("src/main/res/values/colors.xml")
        val night = colours("src/main/res/values-night/colors.xml")
        val names = mapOf(StyleId.LINEE to "widget_bg", StyleId.ORIGINALE to "widget_originale_bg", StyleId.ANDROID to "widget_android_bg")
        fun hex(c: Long) = "#%06X".format(c and 0xFFFFFF)
        for ((style, name) in names) {
            val (d, n) = WidgetStyle.backgrounds(style)
            assertEquals("$style giorno", day.getValue(name), hex(d))
            assertEquals("$style notte", night.getValue(name), hex(n))
        }
    }
}
