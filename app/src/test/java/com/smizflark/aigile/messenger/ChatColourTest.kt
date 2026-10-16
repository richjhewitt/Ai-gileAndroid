package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatColour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-375: a chat's colour is stored per chat; every colour's text is readable in light and dark. */
class ChatColourTest {
    @Test fun defaultWhenNothingStored() {
        assertEquals("", ChatColour.of(emptySet(), "lead:a"))
        assertNull(ChatColour.tint("", false))
    }
    @Test fun putAndReadPerChat() {
        val s = ChatColour.put(ChatColour.put(emptySet(), "lead:a", "blue"), "friend:b", "pink")
        assertEquals("blue", ChatColour.of(s, "lead:a"))
        assertEquals("pink", ChatColour.of(s, "friend:b"))
        assertEquals("", ChatColour.of(s, "lead:c"))
    }
    @Test fun changingReplacesAndDefaultRemoves() {
        val s = ChatColour.put(ChatColour.put(emptySet(), "lead:a", "blue"), "lead:a", "green")
        assertEquals(setOf("lead:a\tgreen"), s)
        assertEquals(emptySet<String>(), ChatColour.put(s, "lead:a", ChatColour.DEFAULT))
    }
    @Test fun unknownIdIsDefault() = assertEquals("", ChatColour.of(setOf("lead:a\tmauve"), "lead:a"))
    @Test fun sixColoursWithBothVariants() {
        assertEquals(6, ChatColour.choices.size)
        ChatColour.choices.forEach { assertNotNull(ChatColour.tint(it.id, false)); assertNotNull(ChatColour.tint(it.id, true)) }
        assertTrue(ChatColour.tint("blue", false)!!.fill != ChatColour.tint("blue", true)!!.fill)
    }
    @Test fun textContrastIsAtLeast4point5() {
        assertEquals(21.0, ChatColour.contrast(0xFF000000, 0xFFFFFFFF), 0.01)
        ChatColour.choices.forEach { c ->
            listOf(c.light, c.dark).forEach { assertTrue("${c.id} ${ChatColour.contrast(it.fill, it.text)}", ChatColour.contrast(it.fill, it.text) >= 4.5) }
        }
    }
}
