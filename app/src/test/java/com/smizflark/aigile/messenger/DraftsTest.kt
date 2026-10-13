package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Drafts
import org.junit.Assert.assertEquals
import org.junit.Test

/** AII-42: half-typed replies kept per chat. */
class DraftsTest {
    @Test fun keepsTextPerChat() =
        assertEquals(mapOf("lead:a" to "hi", "lead:b" to "yo"), Drafts.put(mapOf("lead:a" to "hi"), "lead:b", "yo"))

    @Test fun newTextReplacesOld() =
        assertEquals(mapOf("lead:a" to "hello"), Drafts.put(mapOf("lead:a" to "hel"), "lead:a", "hello"))

    @Test fun sentOrClearedDropsTheChat() {
        assertEquals(emptyMap<String, String>(), Drafts.put(mapOf("lead:a" to "hi"), "lead:a", ""))
        assertEquals(mapOf("lead:b" to "yo"), Drafts.put(mapOf("lead:a" to "hi", "lead:b" to "yo"), "lead:a", "  \n"))
    }

    @Test fun keepsSpacesInsideTheText() =
        assertEquals("  two  lines\n", Drafts.put(emptyMap(), "friend:x", "  two  lines\n")["friend:x"])
}
