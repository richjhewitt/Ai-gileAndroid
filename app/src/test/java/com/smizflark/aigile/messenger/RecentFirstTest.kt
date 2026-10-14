package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Mapper
import org.junit.Assert.assertEquals
import org.junit.Test

/** AII-248: New message lists the chats you used most recently first. */
class RecentFirstTest {
    private fun chat(id: String, last: String?) = ChatRef(kind = Kind.LEAD, id = id, name = id, subtitle = "Lead", emoji = "", last = last)

    @Test fun newestFirstAndNeverUsedLast() {
        val got = Mapper.recentFirst(listOf(chat("a", "2026-10-01T10:00:00Z"), chat("b", null), chat("c", "2026-10-14T09:00:00Z"), chat("d", null)))
        assertEquals(listOf("c", "a", "b", "d"), got.map { it.id })
    }
}
