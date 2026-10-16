package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatHeads
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-381: chat heads need the switch and Android 11. */
class ChatHeadsTest {
    @Test fun needsSwitchAndApi30() {
        assertTrue(ChatHeads.enabled(true, 30))
        assertTrue(ChatHeads.enabled(true, 35))
        assertFalse(ChatHeads.enabled(false, 34))
        assertFalse(ChatHeads.enabled(true, 29))
    }

    @Test fun shortcutIsOnePerChatAndNotTheHomeOne() {
        assertEquals("chat:lead-1", ChatHeads.shortcutId("lead-1"))
    }
}
