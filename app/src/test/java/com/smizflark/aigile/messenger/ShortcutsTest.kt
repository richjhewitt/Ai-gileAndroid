package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.ui.Shortcuts
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutsTest {
    private fun chat(id: String, last: String?) = ChatRef(Kind.LEAD, id, id.uppercase(), "", "", last = last)

    @Test fun newestFourWithMessages() {   // AII-398: 4, shared with the share sheet
        val l = listOf(
            chat("a", "2026-10-13T09:00:00Z"), chat("b", null), chat("c", "2026-10-13T11:00:00Z"),
            chat("d", "2026-10-12T23:00:00Z"), chat("e", "2026-10-13T10:00:00Z"),
        )
        assertEquals(listOf("c", "e", "a", "d"), Shortcuts.top(l).map { it.id })
    }

    @Test fun fewerChatsFewerShortcuts() {
        assertEquals(1, Shortcuts.top(listOf(chat("a", "2026-10-13T09:00:00Z"), chat("b", null))).size)
    }
}
