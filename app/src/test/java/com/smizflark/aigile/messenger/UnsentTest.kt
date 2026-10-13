package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.ui.unsentLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The chats list says when a chat has messages still waiting to send (AII-52). */
class UnsentTest {
    private fun p(id: String, failed: Boolean = false) = Pending(id, "lead:ai-gile", "hi", null, "2026-10-01T10:00:00Z", failed)

    @Test fun nothingWaitingSaysNothing() = assertNull(unsentLabel(emptyList()))

    @Test fun countsWhatIsWaiting() {
        assertEquals("⏳ 1 waiting to send", unsentLabel(listOf(p("a"))))
        assertEquals("⏳ 2 waiting to send", unsentLabel(listOf(p("a"), p("b"))))
    }

    @Test fun failedOnesWin() = assertEquals("⚠️ 1 not sent: tap to retry", unsentLabel(listOf(p("a"), p("b", failed = true))))
}
