package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.Stars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Star a message and find it again (AII-138). */
class StarsTest {
    private val old = Bubble("k1", "Ann", "first", "2026-10-13T09:00:00Z", false)
    private val new = Bubble("k2", "Rich", "second", "2026-10-13T10:00:00Z", true)

    @Test fun starringTwiceTakesTheStarOff() {
        val on = Stars.toggle(emptyList(), Stars.of("a", old))
        assertTrue(Stars.has(on, "a", "k1"))
        assertFalse(Stars.has(Stars.toggle(on, Stars.of("a", old)), "a", "k1"))
    }

    @Test fun theSameMessageKeyInAnotherChatIsItsOwnStar() {
        val all = Stars.toggle(emptyList(), Stars.of("a", old))
        assertFalse(Stars.has(all, "b", "k1"))
        assertEquals(2, Stars.toggle(all, Stars.of("b", old)).size)
    }

    @Test fun aChatListsOnlyItsOwnStarsNewestFirst() {
        val all = listOf(Stars.of("a", old), Stars.of("b", new), Stars.of("a", new))
        assertEquals(listOf("k2", "k1"), Stars.inChat(all, "a").map { it.key })
        assertEquals("second", Stars.inChat(all, "b").single().text)
    }
}
