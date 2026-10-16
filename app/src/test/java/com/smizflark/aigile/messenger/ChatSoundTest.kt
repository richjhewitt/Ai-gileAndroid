package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatSound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-373: a chat's own alert sound, one channel per chat and sound. */
class ChatSoundTest {
    @Test fun noChoiceUsesTheNormalChannel() {
        assertEquals("", ChatSound.of(emptySet(), "lead:a"))
        assertEquals("messages", ChatSound.channelFor(emptySet(), "lead:a", "messages"))
    }

    @Test fun ownSoundGetsItsOwnChannel() {
        val saved = ChatSound.put(emptySet(), "lead:a", "content://media/1")
        val id = ChatSound.channelFor(saved, "lead:a", "messages")
        assertTrue(id.startsWith(ChatSound.prefix("lead:a")))
        assertEquals("messages", ChatSound.channelFor(saved, "lead:b", "messages"))
    }

    @Test fun anotherSoundIsAnotherChannel() =
        assertNotEquals(ChatSound.channelId("lead:a", "content://media/1", "m"), ChatSound.channelId("lead:a", "content://media/2", "m"))

    @Test fun puttingReplacesAndEmptyClears() {
        val one = ChatSound.put(emptySet(), "lead:a", "x")
        val two = ChatSound.put(ChatSound.put(one, "lead:b", "y"), "lead:a", "z")
        assertEquals("z", ChatSound.of(two, "lead:a")); assertEquals("y", ChatSound.of(two, "lead:b"))
        assertEquals(setOf<String>(), ChatSound.put(one, "lead:a", ""))
    }

    @Test fun silentChoiceIsKept() {
        val saved = ChatSound.put(emptySet(), "lead:a", ChatSound.NONE)
        assertTrue(ChatSound.isNone(ChatSound.of(saved, "lead:a")))
    }
}
