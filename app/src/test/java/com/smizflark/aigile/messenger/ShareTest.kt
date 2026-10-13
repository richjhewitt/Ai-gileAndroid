package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Share
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AII-36: text shared in from other apps. */
class ShareTest {
    @Test fun plainText() = assertEquals("hello", Share.text(null, "  hello \n"))

    @Test fun subjectGoesOnTop() =
        assertEquals("Bug in login\n\nhttps://example.com/x", Share.text("Bug in login", "https://example.com/x"))

    @Test fun subjectNotRepeated() =
        assertEquals("Great article https://a.b", Share.text("Great article", "Great article https://a.b"))

    @Test fun subjectOnly() = assertEquals("Just a title", Share.text("Just a title", null))

    @Test fun nothingToSend() {
        assertNull(Share.text(null, null))
        assertNull(Share.text("  ", "\n"))
    }
}
