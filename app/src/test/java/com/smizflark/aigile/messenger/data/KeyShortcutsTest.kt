package com.smizflark.aigile.messenger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyShortcutsTest {
    @Test fun combos() {
        assertEquals(KeyAction.SEARCH, keyAction(Keys.F, ctrl = true, alt = false))
        assertEquals(KeyAction.NEW_MESSAGE, keyAction(Keys.N, ctrl = true, alt = false))
        assertEquals(KeyAction.BACK, keyAction(Keys.ESCAPE, ctrl = false, alt = false))
        assertEquals(KeyAction.NEXT_CHAT, keyAction(Keys.DPAD_DOWN, ctrl = false, alt = true))
        assertEquals(KeyAction.PREVIOUS_CHAT, keyAction(Keys.DPAD_UP, ctrl = false, alt = true))
    }

    @Test fun plainKeysAreLeftAlone() {
        assertNull(keyAction(Keys.F, ctrl = false, alt = false))
        assertNull(keyAction(Keys.N, ctrl = false, alt = false))
        assertNull(keyAction(Keys.DPAD_DOWN, ctrl = false, alt = false))
        assertNull(keyAction(Keys.DPAD_UP, ctrl = true, alt = false))
        assertNull(keyAction(Keys.ESCAPE, ctrl = true, alt = false))
    }

    @Test fun neighbours() {
        val k = listOf("a", "b", "c")
        assertEquals("b", neighbourChat(k, "a", 1))
        assertEquals("a", neighbourChat(k, "b", -1))
        assertNull(neighbourChat(k, "c", 1))
        assertNull(neighbourChat(k, "a", -1))
        assertEquals("a", neighbourChat(k, null, 1))
        assertEquals("c", neighbourChat(k, null, -1))
        assertEquals("a", neighbourChat(k, "gone", 1))
        assertNull(neighbourChat(emptyList(), "a", 1))
    }
}
