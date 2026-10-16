package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.widget.ChatWidgetLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatWidgetLogicTest {
    private fun b(n: Int, who: String = "Ann", text: String = "hi $n", mine: Boolean = false, deleted: Boolean = false) =
        Bubble(key = "k$n", author = who, text = text, at = "2026-01-01T10:%02d:00Z".format(n), mine = mine, deleted = deleted)

    @Test fun newestThreeOldestFirst() {
        val l = ChatWidgetLogic.lines((1..5).map { b(it) }, "You")
        assertEquals(listOf("Ann: hi 3", "Ann: hi 4", "Ann: hi 5"), l)
    }

    @Test fun mineUsesYouAndSkipsDeletedAndBlank() {
        val l = ChatWidgetLogic.lines(listOf(b(1), b(2, mine = true), b(3, deleted = true), b(4, text = "  ")), "Jij")
        assertEquals(listOf("Ann: hi 1", "Jij: hi 2"), l)
    }

    @Test fun fewerThanThree() { assertEquals(1, ChatWidgetLogic.lines(listOf(b(1)), "You").size) }

    @Test fun trimSqueezesAndCuts() {
        assertEquals("a b c", ChatWidgetLogic.trim("a \n b\t c", 10))
        assertEquals("abcd…", ChatWidgetLogic.trim("abcdefgh", 5))
        assertEquals("abcde", ChatWidgetLogic.trim("abcde", 5))
    }

    @Test fun storedMappingRoundTrips() {
        val m = ChatWidgetLogic.with(ChatWidgetLogic.with(emptyMap(), 7, "lead:aii"), 9, "friend:a=b")
        assertEquals(m, ChatWidgetLogic.decode(ChatWidgetLogic.encode(m)))
        assertEquals(mapOf(9 to "friend:a=b"), ChatWidgetLogic.without(m, listOf(7)))
        assertEquals(mapOf(7 to "x"), ChatWidgetLogic.with(m, 7, "x").filterKeys { it == 7 })
    }

    @Test fun decodeIgnoresBadEntries() {
        assertEquals(mapOf(3 to "c"), ChatWidgetLogic.decode(setOf("3=c", "x=y", "=z", "5=", "nonsense")))
    }

    @Test fun clearedMessagesAreLeftOut() {   // AII-405
        val all = listOf(b(1, text = "old"), b(5, text = "new"))
        assertEquals(listOf("Ann: new"), ChatWidgetLogic.lines(all, "You", clearedAt = "2026-01-01T10:03:00Z"))
    }
}
