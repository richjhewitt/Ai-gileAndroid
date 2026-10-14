package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.nextUnread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextUnreadTest {
    private val a = ChatRef(Kind.LEAD, "a", "A", "", "")
    private val b = ChatRef(Kind.LEAD, "b", "B", "", "")
    private val c = ChatRef(Kind.FRIEND, "c", "C", "", "")
    private val all = listOf(a, b, c)
    private val one = mapOf(a.key to 1, b.key to 2, c.key to 3)

    @Test fun firstUnreadInListOrderSkippingTheCurrentOne() {
        assertEquals(b, nextUnread(all, emptySet(), one, emptySet(), emptySet(), a.key))
        assertEquals(a, nextUnread(all, emptySet(), one, emptySet(), emptySet(), c.key))
    }

    @Test fun pinnedChatsComeFirst() {
        assertEquals(c, nextUnread(all, setOf(c.key), one, emptySet(), emptySet(), a.key))
    }

    @Test fun mutedAndClosedAreSkipped() {
        assertEquals(c, nextUnread(all, emptySet(), one, setOf(b.key), emptySet(), a.key))
        assertEquals(c, nextUnread(all, emptySet(), one, emptySet(), setOf(b.key), a.key))
    }

    @Test fun readChatsAreSkippedAndNoneGivesNull() {
        assertEquals(c, nextUnread(all, emptySet(), mapOf(c.key to 1), emptySet(), emptySet(), a.key))
        assertNull(nextUnread(all, emptySet(), mapOf(a.key to 5), emptySet(), emptySet(), a.key))
        assertNull(nextUnread(emptyList(), emptySet(), one, emptySet(), emptySet(), a.key))
    }
}
