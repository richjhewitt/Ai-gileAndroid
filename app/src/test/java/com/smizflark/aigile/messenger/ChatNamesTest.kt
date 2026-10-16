package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatNames
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AII-365: a chat's own name on this phone; the board's name is kept for Reset. */
class ChatNamesTest {
    private fun lead(slug: String = "a", name: String = "Alpha") = ChatRef(Kind.LEAD, slug, name, "", "")

    @Test fun cleanTrimsSqueezesAndCaps() {
        assertEquals("My team", ChatNames.clean("  My \n team  "))
        assertEquals("", ChatNames.clean("   "))
        assertEquals(40, ChatNames.clean("x".repeat(60)).length)
    }
    @Test fun putReplacesAndBlankResets() {
        val s = ChatNames.put(ChatNames.put(emptySet(), "lead:a", "One"), "lead:a", " Two ")
        assertEquals(setOf("lead:a\tTwo"), s)
        assertEquals("Two", ChatNames.of(s, "lead:a"))
        assertNull(ChatNames.of(s, "lead:b"))
        assertEquals(emptySet<String>(), ChatNames.put(s, "lead:a", "  "))
    }
    @Test fun applyRenamesOnlyThatChatAndKeepsTheBoardName() {
        val saved = ChatNames.put(emptySet(), "lead:a", "Mine")
        val out = ChatNames.apply(listOf(lead("a"), lead("b", "Beta")), saved)
        assertEquals("Mine", out[0].name); assertEquals("Alpha", out[0].boardName)
        assertEquals("Beta", out[1].name); assertNull(out[1].boardName)
        assertEquals("Alpha", ChatNames.boardName(out[0]))
    }
    @Test fun applyTwiceChangesNothing() {
        val saved = ChatNames.put(emptySet(), "lead:a", "Mine")
        val once = ChatNames.apply(listOf(lead("a")), saved)
        assertEquals(once, ChatNames.apply(once, saved))
    }
    @Test fun resetBringsTheBoardNameBack() {
        val once = ChatNames.apply(listOf(lead("a")), ChatNames.put(emptySet(), "lead:a", "Mine"))
        assertEquals(lead("a"), ChatNames.apply(once, emptySet())[0])
    }
    @Test fun aBoardRenameShowsThroughAfterReset() {
        assertEquals("Alpha 2", ChatNames.apply(listOf(lead("a", "Alpha 2")), emptySet())[0].name)
    }
    @Test fun sameAsBoardNameIsNotARename() {
        val out = ChatNames.apply(listOf(lead("a")), ChatNames.put(emptySet(), "lead:a", "Alpha"))
        assertNull(out[0].boardName)
    }
}
