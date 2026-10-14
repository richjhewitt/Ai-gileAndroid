package com.smizflark.aigile.messenger

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import com.smizflark.aigile.messenger.ui.Markdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-191: a **Label:** value line gets a tap-to-copy mark after the value. */
class CopyValueTest {
    private fun taps(s: AnnotatedString) =
        s.getLinkAnnotations(0, s.length).map { it.item }.filterIsInstance<LinkAnnotation.Clickable>().filter { it.tag == "copy-value" }

    private fun copyOf(text: String): List<String> {
        val got = mutableListOf<String>()
        val s = Markdown.render(text, Color.Gray, Color.Blue, onCode = { got += it })
        taps(s).forEach { it.linkInteractionListener!!.onClick(it) }
        return got
    }

    @Test fun aWordValueCopiesWithoutTheLabel() {
        val s = Markdown.render("**Password:** abc123", Color.Gray, Color.Blue, onCode = {})
        assertEquals("Password: abc123 ⧉", s.text)
        assertEquals(listOf("abc123"), copyOf("**Password:** abc123"))
        assertEquals("Password: abc123", Markdown.plain(s))
    }

    @Test fun codeAndLinkValues() {
        assertEquals(listOf("x y"), copyOf("**Cmd:** `x y`"))
        assertEquals(listOf("https://a.com/b"), copyOf("**Open:** [here](https://a.com/b)"))
    }

    @Test fun onlyWhenOneValueEndsTheLine() {
        assertTrue(copyOf("**Note:** two words").isEmpty())
        assertTrue(copyOf("**Note** abc").isEmpty())
        assertTrue(copyOf("```\n**Password:** abc123\n```").isEmpty())
        assertEquals(listOf("a", "b"), copyOf("- **A:** a\n- **B:** b"))
    }

    @Test fun withoutACopierNothingChanges() {
        assertEquals("Password: abc123", Markdown.render("**Password:** abc123", Color.Gray, Color.Blue).text)
    }
}
