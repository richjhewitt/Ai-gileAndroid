package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatHeads
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.ShareTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AII-398: your top chats as direct share targets. */
class ShareTargetsTest {
    private fun chat(id: String, last: String?) = ChatRef(Kind.FRIEND, id, id, "", "", last = last)

    @Test fun idAndKeyRoundTripAndMatchChatHeads() {
        val key = chat("abc", null).key
        assertEquals(key, ShareTargets.chatKey(ShareTargets.id(key)))
        assertEquals(ChatHeads.shortcutId(key), ShareTargets.id(key))
    }

    @Test fun otherShortcutIdsAreNotChats() {
        assertNull(ShareTargets.chatKey(null))
        assertNull(ShareTargets.chatKey(""))
        assertNull(ShareTargets.chatKey("home:abc"))
        assertNull(ShareTargets.chatKey("chat:"))
    }

    @Test fun topIsTheFourNewestChatsWithMessages() {
        val chats = listOf(chat("a", "2026-10-01T00:00:00Z"), chat("b", "2026-10-05T00:00:00Z"), chat("c", null),
            chat("d", "2026-10-03T00:00:00Z"), chat("e", "2026-10-04T00:00:00Z"), chat("f", "2026-10-02T00:00:00Z"))
        assertEquals(listOf("b", "e", "d", "f"), ShareTargets.top(chats).map { it.id })
    }
}
