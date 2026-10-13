package com.smizflark.aigile.messenger

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import com.smizflark.aigile.messenger.ui.Markdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-115: tapping a ``` block hands over the whole command, to copy it. */
class CodeCopyTest {
    private val msg = "- Run this:\n\n```\ngit pull\nnpm test\n```\n\n- Then **Done**"

    @Test fun blocksAreFoundByTheirOpeningLine() {
        val lines = Markdown.squeeze(msg).lines()
        assertEquals(mapOf(2 to "git pull\nnpm test"), Markdown.codeBlocks(lines))
        assertEquals(mapOf(0 to "half"), Markdown.codeBlocks(listOf("```", "half")))   // never closed
    }

    @Test fun tappingAnyLineOfTheBlockCopiesAllOfIt() {
        var copied = ""
        val s = Markdown.render(msg, Color.Gray, Color.Blue, onCode = { copied = it })
        val links = s.getLinkAnnotations(0, s.length).map { it.item }.filterIsInstance<LinkAnnotation.Clickable>()
        assertEquals(2, links.size)   // one per line of the block
        links.last().linkInteractionListener!!.onClick(links.last())
        assertEquals("git pull\nnpm test", copied)
        assertEquals("•  Run this:\n\ngit pull\nnpm test\n\n•  Then Done", s.text)   // looks the same as before
    }

    @Test fun aBoxAtTheEndLeavesNoBlankLineBelow() {
        assertEquals("•  Run:\nnpm test", Markdown.render("- Run:\n```\nnpm test\n```", Color.Gray, Color.Blue).text)
    }

    @Test fun withoutACopierNothingIsTappable() {
        val s = Markdown.render(msg, Color.Gray, Color.Blue)
        assertTrue(s.getLinkAnnotations(0, s.length).isEmpty())
    }
}
