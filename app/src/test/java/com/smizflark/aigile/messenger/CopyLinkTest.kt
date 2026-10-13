package com.smizflark.aigile.messenger

import androidx.compose.ui.graphics.Color
import com.smizflark.aigile.messenger.ui.Markdown
import org.junit.Assert.assertEquals
import org.junit.Test

/** AII-129: a message's links, for Copy link in its menu. */
class CopyLinkTest {
    private fun links(text: String) = Markdown.links(Markdown.render(text, Color.Gray, Color.Blue))

    @Test fun findsTheLinksShown() =
        assertEquals(listOf("https://dnbloco.com/a", "http://x.lol"), links("- See https://dnbloco.com/a and http://x.lol"))

    @Test fun noLinksNoMenuLines() = assertEquals(emptyList<String>(), links("- Plain words, `code` and AIG-2"))

    @Test fun sameLinkOnceAndAtMostThree() =
        assertEquals(3, links("https://a.com https://a.com https://b.com https://c.com https://d.com").size)

    @Test fun shortLinksDropTheSchemeAndFit() {
        assertEquals("dnbloco.com/a", Markdown.shortLink("https://dnbloco.com/a"))
        assertEquals(10, Markdown.shortLink("https://averyveryverylong.example.com/path", 10).length)
    }
}
