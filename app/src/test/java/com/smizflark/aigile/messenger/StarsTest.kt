package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
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

    @Test fun shareAllIsChatAndTimeThenTheTextWithBlankLinesBetween() {
        val a = ChatRef(Kind.LEAD, "a", "Alpha", "", "")
        val b = ChatRef(Kind.LEAD, "b", "Beta", "", "")
        val list = Stars.everywhere(listOf(Stars.of(a.key, old), Stars.of(b.key, new)), listOf(a, b))
        assertEquals("Beta · T10\nsecond\n\nAlpha · T09\nfirst", Stars.asText(list) { "T" + it.substring(11, 13) })
        assertEquals("", Stars.asText(emptyList()) { it })
    }

    @Test fun everywhereListsAllChatsNewestFirstAndSkipsChatsThePhoneLost() {
        val a = ChatRef(Kind.LEAD, "a", "Alpha", "", "")
        val b = ChatRef(Kind.LEAD, "b", "Beta", "", "")
        val all = listOf(Stars.of(a.key, old), Stars.of(b.key, new), Stars.of("gone", new))
        assertEquals(listOf("Beta" to "k2", "Alpha" to "k1"), Stars.everywhere(all, listOf(a, b)).map { (r, s) -> r.name to s.key })
    }

    @Test fun previewIsTheFirstLineWithoutABullet() {
        assertEquals("Done: export", Stars.preview("\n• Done: export\nmore"))
        assertEquals("plain", Stars.preview("plain"))
    }
}
