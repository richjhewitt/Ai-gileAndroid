package com.smizflark.aigile.messenger

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.ui.Markdown
import org.junit.Assert.assertEquals
import org.junit.Test

/** AII-187: links shown the way the board's chat shows them. */
class NamedLinkTest {
    private fun render(text: String) = Markdown.render(text, Color.Gray, Color.Blue)
    private fun linked(text: String) = render(text).let { s ->
        s.getLinkAnnotations(0, s.length).map { s.text.substring(it.start, it.end) to (it.item as LinkAnnotation.Url).url }
    }

    @Test fun namedLinkShowsItsName() {
        assertEquals("- See the guide".replace("- ", "•  "), render("- See [the guide](https://dnbloco.com/guide)").text)
        assertEquals(listOf("the guide" to "https://dnbloco.com/guide"), linked("See [the guide](https://dnbloco.com/guide)"))
    }

    @Test fun bareWebAddressIsTappable() =
        assertEquals(listOf("dnbloco.com" to "https://dnbloco.com", "smizflark.com/x" to "https://smizflark.com/x"),
            linked("Open dnbloco.com, then smizflark.com/x."))

    @Test fun closingFullStopIsNotPartOfTheLink() =
        assertEquals(listOf("https://dnbloco.com/a" to "https://dnbloco.com/a"), linked("Live at https://dnbloco.com/a."))

    @Test fun fileNamesAndEmailsAreNotLinks() =
        assertEquals(emptyList<Pair<String, String>>(), linked("Changed Markdown.kt and rich@smizflark.com, see AIG-2"))

    @Test fun dotBulletsLookLikeBullets() = assertEquals("•  One\n•  Two", render("• One\n• Two").text)

    @Test fun previewShowsTheName() =
        assertEquals("See the guide now", Mapper.plainLine("- See [the guide](https://dnbloco.com/guide) now"))
}
