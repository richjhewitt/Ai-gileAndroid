package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Format
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun wrapsSelection() {
        val r = Format.toggle("say hello now", 4, 9, Format.BOLD)
        assertEquals("say **hello** now", r.text); assertEquals(6, r.start); assertEquals(11, r.end)
    }
    @Test fun unwrapsMarkersOutsideSelection() {
        val r = Format.toggle("say **hello** now", 6, 11, Format.BOLD)
        assertEquals("say hello now", r.text); assertEquals(4, r.start); assertEquals(9, r.end)
    }
    @Test fun unwrapsMarkersInsideSelection() {
        val r = Format.toggle("say *hello* now", 4, 11, Format.ITALIC)
        assertEquals("say hello now", r.text); assertEquals(4, r.start); assertEquals(9, r.end)
    }
    @Test fun italicOnBoldWordsAddsAnotherStar() {
        assertEquals("***x***", Format.toggle("**x**", 2, 3, Format.ITALIC).text)
        assertEquals("***x***", Format.toggle("**x**", 0, 5, Format.ITALIC).text)
    }
    @Test fun boldOnItalicWordsWraps() {
        assertEquals("***x***", Format.toggle("*x*", 1, 2, Format.BOLD).text)
    }
    @Test fun italicShowsInBubble() {
        val s = com.smizflark.aigile.messenger.ui.Markdown.render("a *b* **c** 2 * 3 * 4", androidx.compose.ui.graphics.Color.Gray, androidx.compose.ui.graphics.Color.Blue)
        assertEquals("a b c 2 * 3 * 4", s.text)
        assertEquals(androidx.compose.ui.text.font.FontStyle.Italic, s.spanStyles.single { it.item.fontStyle != null }.item.fontStyle)
    }
    @Test fun emptySelectionDoesNothing() {
        val r = Format.toggle("hello", 2, 2, Format.CODE)
        assertEquals("hello", r.text); assertEquals(2, r.start); assertEquals(2, r.end)
    }
    @Test fun multiLineWrapsEachLine() {
        val r = Format.toggle("one\ntwo", 0, 7, Format.CODE)
        assertEquals("`one`\n`two`", r.text); assertEquals(0, r.start); assertEquals(11, r.end)
    }
    @Test fun multiLineUnwraps() {
        val r = Format.toggle("`one`\n`two`", 0, 11, Format.CODE)
        assertEquals("one\ntwo", r.text); assertEquals(7, r.end)
    }
    @Test fun backwardsSelectionWorks() {
        assertEquals("*ab*", Format.toggle("ab", 2, 0, Format.ITALIC).text)
    }
}
