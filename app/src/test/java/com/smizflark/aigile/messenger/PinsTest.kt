package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.pinFirst
import com.smizflark.aigile.messenger.data.mutedLast
import com.smizflark.aigile.messenger.data.sortChats
import com.smizflark.aigile.messenger.data.togglePin
import org.junit.Assert.assertEquals
import org.junit.Test

class PinsTest {
    private val a = ChatRef(Kind.LEAD, "a", "A", "", "")
    private val b = ChatRef(Kind.LEAD, "b", "B", "", "")
    private val c = ChatRef(Kind.FRIEND, "c", "C", "", "")
    private val d = ChatRef(Kind.LEAD, "d", "D", "", "")

    @Test fun pinnedGoFirstAndKeepTheirOrder() {
        val l = pinFirst(listOf(a, b, c, d), setOf(d.key, b.key))
        assertEquals(listOf(b, d, a, c), l)
    }

    @Test fun nothingPinnedChangesNothing() {
        assertEquals(listOf(a, b, c), pinFirst(listOf(a, b, c), emptySet()))
    }

    @Test fun aPinForAGoneChatIsHarmless() {
        assertEquals(listOf(a, b), pinFirst(listOf(a, b), setOf("gone")))
    }

    @Test fun toggleAddsThenRemoves() {
        val once = togglePin(emptySet(), a.key)
        assertEquals(setOf(a.key), once)
        assertEquals(emptySet<String>(), togglePin(once, a.key))
    }

    @Test fun mutedSinkButPinnedStayFirst() {
        assertEquals(listOf(d, c, a, b), mutedLast(listOf(a, b, c, d), setOf(d.key), setOf(a.key, b.key, d.key)))
        assertEquals(listOf(c, a), mutedLast(listOf(a, c), emptySet(), setOf(a.key)))
    }

    @Test fun aToZSortsByNameIgnoringCaseAndNewestKeepsBoardOrder() {
        val z = ChatRef(Kind.LEAD, "z", "zed", "", "")
        assertEquals(listOf(a, b, d, z), sortChats(listOf(z, b, d, a), true))
        assertEquals(listOf(z, b, a), sortChats(listOf(z, b, a), false))
    }

    @Test fun pinsAndMutedStillWinAfterSorting() {
        val sorted = sortChats(listOf(d, c, b, a), true)
        assertEquals(listOf(d, a, c, b), mutedLast(sorted, setOf(d.key), setOf(b.key, d.key)))
    }
}
