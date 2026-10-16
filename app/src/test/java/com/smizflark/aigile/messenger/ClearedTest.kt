package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.Cleared
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.ui.Row
import com.smizflark.aigile.messenger.ui.rows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** AII-363: Clear chat for me. The clock is always passed in, so nothing here depends on today's date. */
class ClearedTest {
    private val now = Instant.parse("2026-10-16T10:00:00Z")
    private fun b(k: String, at: String) = Bubble(k, "Lead", "text $k", at, mine = false)
    private val msgs = listOf(b("a", "2026-10-16T08:00:00Z"), b("b", "2026-10-16T09:59:59Z"), b("c", "2026-10-16T10:00:00Z"), b("d", "2026-10-16T10:05:00Z"))

    @Test fun neverClearedShowsAll() = assertEquals(msgs, Cleared.visible(msgs, null))

    @Test fun hidesUpToTheMarkAndKeepsNewer() {
        assertEquals(listOf("d"), Cleared.visible(msgs, "2026-10-16T10:00:00Z").map { it.key })
        assertEquals(listOf("c", "d"), Cleared.visible(msgs, "2026-10-16T09:59:59.500Z").map { it.key })
    }

    @Test fun markIsNowOrTheNewestMessageIfThatIsLater() {
        assertEquals(now.toString(), Cleared.mark(msgs.take(2), now))
        assertEquals("2026-10-16T10:05:00Z", Cleared.mark(msgs, now))
        assertEquals(now.toString(), Cleared.mark(emptyList(), now))
    }

    @Test fun clearingHidesEverythingThenNewerShows() {
        val mark = Cleared.mark(msgs, now)
        assertTrue(Cleared.visible(msgs, mark).isEmpty())
        val later = msgs + b("e", "2026-10-16T10:06:00Z")
        assertEquals(listOf("e"), Cleared.visible(later, mark).map { it.key })
    }

    @Test fun undoPutsTheOldMarkBack() {
        val one = Cleared.with(emptyMap(), "lead:x", "2026-10-16T10:00:00Z")
        assertEquals(mapOf("lead:x" to "2026-10-16T10:00:00Z"), one)
        assertEquals(mapOf("lead:x" to "2026-10-16T10:00:00Z", "dm:y" to "m"), Cleared.with(one, "dm:y", "m"))
        assertEquals(emptyMap<String, String>(), Cleared.with(one, "lead:x", null))
    }

    @Test fun unreadableTimeStays() = assertEquals(1, Cleared.visible(listOf(b("x", "soon")), "2026-10-16T10:00:00Z").size)

    @Test fun rowsOnlyBuildTheVisibleMessages() {
        val chat = Conversation(ChatRef(Kind.LEAD, "x", "X", "", ""), msgs)
        val out = rows(chat, emptyList(), "Rich", clearedAt = "2026-10-16T10:00:00Z").filterIsInstance<Row.Msg>()
        assertEquals(listOf("d"), out.map { it.b.key })
    }
}
