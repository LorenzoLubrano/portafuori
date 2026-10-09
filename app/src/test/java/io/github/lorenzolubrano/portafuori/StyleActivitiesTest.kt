package io.github.lorenzolubrano.portafuori

import android.content.Intent
import io.github.lorenzolubrano.portafuori.data.StyleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun aRestoredActivityDoesNotHandOverItsOldIntent() {
        // a calendar shared hours ago must not come back for import when the old activity is recreated
        assertTrue(StyleActivities.carriesLaunchIntent(restored = false))
        assertFalse(StyleActivities.carriesLaunchIntent(restored = true))
    }

    @Test fun aChangeOfStyleReopensOnceInTheNewStyle() {
        assertTrue(StyleActivities.reopens(MainActivity::class.java, StyleId.ORIGINALE, finishing = false))
        assertFalse(StyleActivities.reopens(MainActivityOriginale::class.java, StyleId.ORIGINALE, finishing = false))
        // a second quick change, while this activity is already on its way out, must not stack another one:
        // the activity that opens next reads the latest style and moves on by itself
        assertFalse(StyleActivities.reopens(MainActivity::class.java, StyleId.ANDROID, finishing = true))
    }

    @Test fun aFileFromAnotherAppOpensOnTopWithoutClosingThatApp() {
        val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
        // opened from a chat: on top of the chat, which stays where it is, and the file stays readable
        for (action in listOf(Intent.ACTION_SEND, Intent.ACTION_VIEW)) {
            assertEquals(read or Intent.FLAG_ACTIVITY_SINGLE_TOP, StyleActivities.forwardFlags(action, read or Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        // the app's own ways in (launcher, widget, notification) come back to the style's activity already there
        assertEquals(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            StyleActivities.forwardFlags(Intent.ACTION_MAIN, Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        assertEquals(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP, StyleActivities.forwardFlags(null, 0))
    }

    @Test fun backLeavesTheOtherStylesInTheBackgroundLikeLinee() {
        // Android does this by itself only for the launcher's activity (Linee): the others would be closed,
        // and the next opening would start from scratch, with Linee's colours below Android 13
        assertTrue(StyleActivities.backKeepsInBackground(MainActivityOriginale::class.java, taskRoot = true))
        assertTrue(StyleActivities.backKeepsInBackground(MainActivityAndroid::class.java, taskRoot = true))
        assertFalse(StyleActivities.backKeepsInBackground(MainActivity::class.java, taskRoot = true))
        // opened on top of another app (a file from a chat): Back goes back to that app
        assertFalse(StyleActivities.backKeepsInBackground(MainActivityOriginale::class.java, taskRoot = false))
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
