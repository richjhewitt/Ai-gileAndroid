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
        val r = Format.toggle("say _hello_ now", 4, 11, Format.ITALIC)
        assertEquals("say hello now", r.text); assertEquals(4, r.start); assertEquals(9, r.end)
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
        assertEquals("_ab_", Format.toggle("ab", 2, 0, Format.ITALIC).text)
    }
}
