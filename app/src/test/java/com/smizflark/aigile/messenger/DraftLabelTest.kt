package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.draftLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The chats list shows a half-typed reply as "Draft: ..." (AII-98). */
class DraftLabelTest {
    @Test fun noDraftSaysNothing() {
        assertNull(draftLabel(null))
        assertNull(draftLabel("   \n "))
    }

    @Test fun showsTheText() = assertEquals("Draft: see you at 5", draftLabel("see you at 5"))

    @Test fun keptToOneLine() = assertEquals("Draft: line one line two", draftLabel("  line one\n\n line two "))
}
