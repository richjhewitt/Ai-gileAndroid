package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.chats.ChatList
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class ChatListTest {
    private val zone = ZoneOffset.UTC
    private val now = Instant.parse("2026-10-13T12:00:00Z")   // a Tuesday

    private fun ref(kind: Kind, id: String, last: String?) = ChatRef(kind, id, id, "", "🗂️", last = last)

    @Test fun unreadWhenNewerThanSeen() {
        val c = ref(Kind.LEAD, "lic", "2026-10-13T10:00:00Z")
        assertTrue(ChatList.unread(c, emptyMap()))
        assertTrue(ChatList.unread(c, mapOf(c.key to "2026-10-13T09:00:00Z")))
        assertFalse(ChatList.unread(c, mapOf(c.key to "2026-10-13T10:00:00Z")))
        assertFalse(ChatList.unread(ref(Kind.LEAD, "x", null), emptyMap()))
    }

    @Test fun timeLabels() {
        assertEquals("09:05", ChatList.timeLabel("2026-10-13T09:05:00Z", now, zone))
        assertEquals("Yesterday", ChatList.timeLabel("2026-10-12T23:59:00Z", now, zone))
        assertEquals("Fri", ChatList.timeLabel("2026-10-09T08:00:00Z", now, zone))
        assertEquals("2 Oct", ChatList.timeLabel("2026-10-02T08:00:00Z", now, zone))
        assertEquals("2 Oct 2025", ChatList.timeLabel("2025-10-02T08:00:00Z", now, zone))
        assertEquals("", ChatList.timeLabel(null, now, zone))
        assertEquals("", ChatList.timeLabel("not a date", now, zone))
    }

    @Test fun teamsThenFriends() {
        val s = ChatList.sections(listOf(
            ref(Kind.WATCHMAN, "_watchman", null), ref(Kind.FRIEND, "f1", null), ref(Kind.LEAD, "lic", null),
        ))
        assertEquals(listOf("Teams", "Friends"), s.map { it.first })
        assertEquals(listOf("_watchman", "lic"), s[0].second.map { it.id })
        assertEquals(listOf("f1"), s[1].second.map { it.id })
        assertEquals(listOf("Teams"), ChatList.sections(listOf(ref(Kind.LEAD, "lic", null))).map { it.first })
    }
}
