package io.github.lorenzolubrano.portafuori

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The synchronous copy of theme and style travels with the settings it copies. */
class BackupRulesTest {
    private fun includes(path: String): List<Element> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path)).getElementsByTagName("include")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    // Each place the settings go (a section, or a required flag), the copy must go too: a style restored without
    // its copy reopens the app once in another style, and a calendar shared in that moment is lost
    private fun assertCopyFollowsSettings(path: String, where: (Element) -> String) {
        val all = includes(path)
        val settings = all.filter { it.getAttribute("domain") == "file" && it.getAttribute("path") == "datastore/" }.map(where)
        val copy = all.filter { it.getAttribute("domain") == "sharedpref" && it.getAttribute("path") == "ui.xml" }.map(where)
        assertEquals(settings.sorted(), copy.sorted())
    }

    @Test fun theStyleCopyIsBackedUpWithTheSettings() {
        assertCopyFollowsSettings("src/main/res/xml/data_extraction_rules.xml") { (it.parentNode as Element).tagName }
        assertCopyFollowsSettings("src/main/res/xml/backup_rules.xml") { it.getAttribute("requireFlags") }
    }
}
