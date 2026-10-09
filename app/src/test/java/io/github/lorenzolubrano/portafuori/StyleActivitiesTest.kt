package io.github.lorenzolubrano.portafuori

import io.github.lorenzolubrano.portafuori.data.StyleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The system draws the starting window of a reopened task from its top activity's manifest theme. */
class StyleActivitiesTest {
    private val ns = "http://schemas.android.com/apk/res/android"

    private fun elements(path: String, tag: String): List<Element> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val nodes = factory.newDocumentBuilder().parse(File(path)).getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    @Test fun eachStyleHasItsOwnActivity() {
        assertEquals(MainActivity::class.java, StyleActivities.of(StyleId.LINEE))
        assertEquals(StyleId.entries.size, StyleId.entries.map { StyleActivities.of(it) }.toSet().size)
    }

    @Test fun theManifestGivesEachActivityItsStyleLaunchTheme() {
        // Linee keeps the application theme (Theme.App.Starting)
        val themes = mapOf(
            StyleId.LINEE to "",
            StyleId.ORIGINALE to "@style/Theme.App.Starting.Originale",
            StyleId.ANDROID to "@style/Theme.App.Starting.Android",
        )
        val activities = elements("src/main/AndroidManifest.xml", "activity").associateBy { it.getAttributeNS(ns, "name") }
        val styles = elements("src/main/res/values/themes.xml", "style").map { it.getAttribute("name") }.toSet()
        for ((id, theme) in themes) {
            val activity = activities.getValue("." + StyleActivities.of(id).simpleName)
            assertEquals(theme, activity.getAttributeNS(ns, "theme"))
            assertEquals("singleTop", activity.getAttributeNS(ns, "launchMode"))
            if (id != StyleId.LINEE) {
                assertEquals("false", activity.getAttributeNS(ns, "exported"))
                assertTrue(theme.removePrefix("@style/") in styles)
            }
        }
    }

    @Test fun eachActivityHandsTheTaskOverToTheNextStyle() {
        // The task's identity must follow the activity that replaced it: otherwise a task started by a share
        // drops an identical share as "already open", and after a change of style an intent without the
        // launcher's flags stacks a second activity
        val activities = elements("src/main/AndroidManifest.xml", "activity").associateBy { it.getAttributeNS(ns, "name") }
        for (id in StyleId.entries) {
            assertEquals("true", activities.getValue("." + StyleActivities.of(id).simpleName).getAttributeNS(ns, "relinquishTaskIdentity"))
        }
    }
}
