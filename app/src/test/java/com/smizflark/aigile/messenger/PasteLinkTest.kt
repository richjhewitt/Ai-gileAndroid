package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.pairingIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Connect with a pairing link you copied (AII-54). */
class PasteLinkTest {
    private val link = "aigile://pair?url=https%3A%2F%2Fpc.tail1.ts.net&key=abc123"

    @Test fun aLinkOnItsOwn() = assertEquals("https://pc.tail1.ts.net" to "abc123", pairingIn(link))

    @Test fun aLinkInsideAMessage() {
        assertEquals("https://pc.tail1.ts.net" to "abc123", pairingIn("Here you go: $link."))
        assertEquals("https://pc.tail1.ts.net" to "abc123", pairingIn("(\n$link\n)"))
    }

    @Test fun anythingElseIsNotALink() {
        assertNull(pairingIn(null))
        assertNull(pairingIn("https://pc.tail1.ts.net"))
        assertNull(pairingIn("aigile://pair?url=x"))
    }
}
