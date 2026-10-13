package com.smizflark.aigile.messenger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** AII-70: every English text has a Dutch copy with the same placeholders (%1$s, %2$d...). */
class StringsTest {
    private fun read(dir: String): Map<String, String> {
        val xml = File("src/main/res/$dir/strings.xml").readText()
        val out = linkedMapOf<String, String>()
        Regex("""<string name="([^"]+)">(.*?)</string>""").findAll(xml).forEach { out[it.groupValues[1]] = it.groupValues[2] }
        Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { p ->
            Regex("""<item quantity="([^"]+)">(.*?)</item>""").findAll(p.groupValues[2])
                .forEach { out[p.groupValues[1] + "/" + it.groupValues[1]] = it.groupValues[2] }
        }
        return out
    }

    private fun args(s: String) = Regex("""%\d+\$[sd]""").findAll(s).map { it.value }.sorted().toList()

    @Test fun dutchHasEveryText() {
        val en = read("values")
        val nl = read("values-nl")
        assertEquals(en.keys, nl.keys)
        en.forEach { (k, v) -> assertEquals("placeholders in $k", args(v), args(nl.getValue(k))) }
        assertTrue(en.size > 100)
    }
}
