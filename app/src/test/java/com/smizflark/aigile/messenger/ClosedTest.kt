package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Closed
import com.smizflark.aigile.messenger.data.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-131: Close a chat, hidden until a newer message, keyed like the board's Messages list. */
class ClosedTest {
    private fun chat(kind: Kind, id: String, last: String? = "2026-10-14T01:00:00Z", waiting: Int = 0) =
        ChatRef(kind, id, id, "", "", waiting = waiting, last = last)

    @Test fun boardKeysMatchTheBoardsMessagesList() {
        assertEquals("tunio", Closed.boardKey(chat(Kind.LEAD, "tunio")))
        assertEquals("_watchman", Closed.boardKey(chat(Kind.WATCHMAN, "_watchman")))
        assertEquals("dm:abc", Closed.boardKey(chat(Kind.FRIEND, "abc")))
    }

    @Test fun hiddenUntilANewerMessage() {
        val c = chat(Kind.LEAD, "tunio")
        assertFalse(Closed.hidden(c, null))
        assertTrue(Closed.hidden(c, "2026-10-14T02:00:00Z"))
        assertFalse(Closed.hidden(c, "2026-10-14T00:30:00Z"))
    }

    @Test fun aQuestionWaitingOnYouAlwaysShows() {
        assertFalse(Closed.hidden(chat(Kind.LEAD, "tunio", waiting = 1), "2026-10-14T02:00:00Z"))
    }

    @Test fun theBoardsListMapsToChatKeys() {
        val chats = listOf(chat(Kind.LEAD, "tunio"), chat(Kind.FRIEND, "abc"), chat(Kind.LEAD, "other"))
        val m = Closed.fromBoard(mapOf("tunio" to "t1", "dm:abc" to "t2", "gone" to "t3"), chats)
        assertEquals(mapOf("lead:tunio" to "t1", "friend:abc" to "t2"), m)
    }
}
