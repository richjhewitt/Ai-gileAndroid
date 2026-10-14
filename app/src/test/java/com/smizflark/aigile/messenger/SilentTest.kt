package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Silent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-207: silent alerts are per chat; Mute still wins. */
class SilentTest {
    @Test fun toggleSwitchesOnAndOff() {
        val on = Silent.toggle(emptySet(), "lead:a")
        assertEquals(setOf("lead:a"), on)
        assertEquals(emptySet<String>(), Silent.toggle(on, "lead:a"))
    }

    @Test fun otherChatsKeepTheirSetting() = assertEquals(setOf("lead:a", "lead:b"), Silent.toggle(setOf("lead:a"), "lead:b"))

    @Test fun onlySilentChatsAreQuiet() {
        assertTrue(Silent.quiet(setOf("lead:a"), emptySet(), "lead:a"))
        assertFalse(Silent.quiet(setOf("lead:a"), emptySet(), "lead:b"))
    }

    @Test fun muteWins() = assertFalse(Silent.quiet(setOf("lead:a"), setOf("lead:a"), "lead:a"))
}
