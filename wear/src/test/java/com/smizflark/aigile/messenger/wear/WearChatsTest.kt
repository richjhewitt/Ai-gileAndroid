package com.smizflark.aigile.messenger.wear

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearChatsTest {
    @Test fun parsesChatsAndSortsNewestFirst() {
        val j = """[{"key":"a","name":"A","last":"x","time":"2026-01-01T10:00:00Z","unread":false},{"key":"b","name":"B","last":"y","time":"2026-01-02T10:00:00Z","unread":true}]"""
        val l = WearChats.newestFirst(WearChats.parse(j))
        assertEquals(listOf("b", "a"), l.map { it.key })
        assertTrue(l[0].unread)
    }

    @Test fun badJsonIsEmpty() {
        assertTrue(WearChats.parse("nope").isEmpty())
        assertTrue(WearChats.parseLines("{").isEmpty())
    }

    @Test fun parsesLines() {
        val l = WearChats.parseLines("""[{"who":"Sam","text":"hi","time":"t","mine":false},{"who":"Rich","text":"yo","time":"u","mine":true}]""")
        assertEquals(2, l.size)
        assertFalse(l[0].mine); assertTrue(l[1].mine)
        assertEquals("yo", l[1].text)
    }

    @Test fun lockedFlag() {
        assertTrue(WearChats.parseLocked("1".toByteArray()))
        assertFalse(WearChats.parseLocked("0".toByteArray()))
        assertFalse(WearChats.parseLocked(null))
    }

    @Test fun pathEscapesSlash() {
        assertEquals("/chat/lead:a%2Fb%25", WearChats.path("lead:a/b%"))
    }

    @Test fun clockTodayAndEarlier() {
        val z = ZoneId.of("UTC"); val now = Instant.parse("2026-10-16T12:00:00Z")
        assertEquals("09:41", WearChats.clock("2026-10-16T09:41:00Z", now, z))
        assertEquals("15 Oct", WearChats.clock("2026-10-15T19:30:00Z", now, z))
        assertEquals("", WearChats.clock("garbage", now, z))
    }
}
