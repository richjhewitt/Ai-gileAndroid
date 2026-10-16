package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.PinnedMsgs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-339: pinned messages, per chat, newest pin first. */
class PinnedMsgsTest {
    private fun b(key: String, text: String = "hello\nworld") = Bubble(key, "Ann", text, "2026-01-01T10:00:00Z", false)

    @Test fun toggleAddsThenRemoves() {
        val p = PinnedMsgs.of("c1", b("k1"), 5)
        assertEquals("hello", p.text)
        val on = PinnedMsgs.toggle(emptyList(), p)
        assertTrue(PinnedMsgs.has(on, "c1", "k1"))
        assertFalse(PinnedMsgs.has(PinnedMsgs.toggle(on, p), "c1", "k1"))
    }

    @Test fun inChatNewestPinFirstAndOtherChatsLeftOut() {
        val all = listOf(PinnedMsgs.of("c1", b("a"), 1), PinnedMsgs.of("c2", b("x"), 9), PinnedMsgs.of("c1", b("b"), 3))
        assertEquals(listOf("b", "a"), PinnedMsgs.inChat(all, "c1").map { it.key })
    }

    @Test fun removeOnlyThatPin() {
        val all = listOf(PinnedMsgs.of("c1", b("a"), 1), PinnedMsgs.of("c2", b("a"), 2))
        assertEquals(listOf("c2"), PinnedMsgs.remove(all, "c1", "a").map { it.chat })
    }

    @Test fun cyclingWrapsAndClamps() {
        assertEquals(1, PinnedMsgs.next(0, 3)); assertEquals(0, PinnedMsgs.next(2, 3)); assertEquals(0, PinnedMsgs.next(0, 0))
        assertEquals(1, PinnedMsgs.clamp(2, 2)); assertEquals(0, PinnedMsgs.clamp(4, 0))
    }

    /** AII-411: the bar skips pins whose message was cleared or deleted, but they stay stored. */
    @Test fun shownHidesClearedAndDeletedPins() {
        val old = Bubble("old", "Ann", "a", "2026-01-01T10:00:00Z", false)
        val new = Bubble("new", "Ann", "b", "2026-01-03T10:00:00Z", false)
        val gone = Bubble("gone", "Ann", "c", "2026-01-04T10:00:00Z", false, deleted = true)
        val all = listOf(old, new, gone).mapIndexed { i, x -> PinnedMsgs.of("c1", x, i.toLong()) }
        val cut = "2026-01-02T00:00:00Z"
        assertEquals(listOf("new"), PinnedMsgs.shown(all, "c1", listOf(old, new, gone), cut).map { it.key })
        assertEquals(listOf("old", "new"), PinnedMsgs.shown(all, "c1", listOf(old, new, gone), null).map { it.key }.sorted().reversed().sortedBy { it != "old" })
        assertEquals(3, all.size)
    }
}
