package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.unreadChatCount
import org.junit.Assert.assertEquals
import org.junit.Test

class UnreadChatCountTest {
    private val a = ChatRef(Kind.LEAD, "a", "A", "", "")
    private val b = ChatRef(Kind.LEAD, "b", "B", "", "")
    private val c = ChatRef(Kind.FRIEND, "c", "C", "", "")
    private val none = { _: ChatRef -> false }

    @Test fun countsChatsWithBadge() = assertEquals(2, unreadChatCount(listOf(a, b, c), { it != c }, none))
    @Test fun mutedNotCounted() = assertEquals(1, unreadChatCount(listOf(a, b, c), { it != c }, { it == b }))
    @Test fun zeroWhenNoBadges() = assertEquals(0, unreadChatCount(listOf(a, b), none, none))
}
