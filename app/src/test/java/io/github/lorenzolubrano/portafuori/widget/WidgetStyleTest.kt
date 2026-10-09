package io.github.lorenzolubrano.portafuori.widget

import io.github.lorenzolubrano.portafuori.data.StyleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
