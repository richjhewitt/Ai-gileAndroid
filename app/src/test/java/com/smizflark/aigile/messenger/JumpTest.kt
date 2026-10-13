package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.ui.unreadBelow
import org.junit.Assert.assertEquals
import org.junit.Test

/** Jump to newest (AII-46): how many replies came in below while you were scrolled up. */
class JumpTest {
    private fun b(key: String, mine: Boolean = false) = Bubble(key, if (mine) "Rich" else "Lead", "hi", "2026-10-13T10:00:00Z", mine)

    @Test fun countsTheirsAfterSeen() = assertEquals(2, unreadBelow(listOf(b("1"), b("2"), b("3"), b("4")), "2"))

    @Test fun skipsMine() = assertEquals(1, unreadBelow(listOf(b("1"), b("2", mine = true), b("3")), "1"))

    @Test fun nothingNewAtNewest() = assertEquals(0, unreadBelow(listOf(b("1"), b("2")), "2"))

    @Test fun nothingSeenYet() = assertEquals(0, unreadBelow(listOf(b("1"), b("2")), null))

    @Test fun seenMessageGone() = assertEquals(0, unreadBelow(listOf(b("1"), b("2")), "x"))
}
