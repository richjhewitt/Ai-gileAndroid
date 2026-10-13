package com.smizflark.aigile.messenger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** AII-33: every English text has a Dutch copy with the same placeholders (%1$s, %2$d...). */
class StringsTest {
    private fun read(dir: String): Map<String, String> =
        Regex("""<string name="([^"]+)">(.*?)</string>""").findAll(File("src/main/res/$dir/strings.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    private fun args(s: String) = Regex("""%\d+\$[sd]""").findAll(s).map { it.value }.sorted().toList()

    @Test fun dutchHasEveryText() {
        val en = read("values") - "app_name"
        val nl = read("values-nl")
        assertEquals(en.keys, nl.keys)
        en.forEach { (k, v) -> assertEquals("placeholders in $k", args(v), args(nl.getValue(k))) }
        assertTrue(en.size > 40)
    }
}
