package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Held
import org.junit.Assert.assertEquals
import org.junit.Test

class UndoSendTest {
    @Test fun plainTextIsTrimmed() = assertEquals("hi", Held(" hi ", null, null, emptyList()).body)
    @Test fun quoteComesFirst() = assertEquals("> a\n\nhi", Held("hi", "> a", null, emptyList()).body)
    @Test fun picturesOnlyKeepsBlankText() = assertEquals("", Held("", "> a", null, listOf("f.jpg")).body)
}
