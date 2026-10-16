package com.smizflark.aigile.messenger.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearTileContentTest {
    private fun c(k: String, t: String, unread: Boolean) = WearChat(k, k, "", t, unread)

    @Test fun countsUnreadAndShowsTopThreeUnreadFirst() {
        val chats = listOf(c("old-unread", "2026-10-01T10:00:00Z", true), c("new-read", "2026-10-16T10:00:00Z", false),
            c("mid-read", "2026-10-10T10:00:00Z", false), c("new-unread", "2026-10-15T10:00:00Z", true), c("oldest", "2026-09-01T10:00:00Z", false))
        val t = TileContent.build(chats, false)
        assertEquals(2, t.unread)
        assertEquals(listOf("new-unread", "old-unread", "new-read"), t.top.map { it.key })
    }

    @Test fun lockedShowsNothing() {
        val t = TileContent.build(listOf(c("a", "2026-10-16T10:00:00Z", true)), true)
        assertTrue(t.locked); assertEquals(0, t.unread); assertTrue(t.top.isEmpty())
    }

    @Test fun emptyIsZero() {
        assertEquals(TileContent(false, 0, emptyList()), TileContent.build(emptyList(), false))
    }

    @Test fun noPhoneNodeMeansNotSentAndRetryable() {
        val l = listOf(Outgoing("1", "k", "a", ReplyStatus.SENT), Outgoing("2", "k", "b"))
        val r = WearReplies.noPhone(l, "2")
        assertEquals(ReplyStatus.NOT_CONNECTED, r[1].status)
        assertEquals(ReplyStatus.SENT, r[0].status)
        assertEquals("b", WearReplies.retryable(r)?.text)
    }

    @Test fun nothingToRetryWhenAllReachedThePhone() {
        assertNull(WearReplies.retryable(listOf(Outgoing("1", "k", "a", ReplyStatus.SENT), Outgoing("2", "k", "b", ReplyStatus.UNCONFIRMED))))
    }

    @Test fun retryDropsTheOldLine() {
        assertEquals(emptyList<Outgoing>(), WearReplies.withoutLine(listOf(Outgoing("1", "k", "a", ReplyStatus.NOT_CONNECTED)), "1"))
    }
}
