package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.ui.unsentLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The chats list says when a chat has messages still waiting to send (AII-52). */
class UnsentTest {
    private fun p(id: String, failed: Boolean = false, offline: Boolean = false) = Pending(id, "lead:ai-gile", "hi", null, "2026-10-01T10:00:00Z", failed, offline = offline)

    @Test fun nothingWaitingSaysNothing() = assertNull(unsentLabel(emptyList()))

    @Test fun countsWhatIsWaiting() {
        assertEquals("⏳ 1 waiting to send", unsentLabel(listOf(p("a"))))
        assertEquals("⏳ 2 waiting to send", unsentLabel(listOf(p("a"), p("b"))))
    }

    @Test fun noSignalIsStillWaitingNotFailed() = assertEquals("⏳ 2 waiting to send", unsentLabel(listOf(p("a"), p("b", failed = true, offline = true))))

    /** AII-148: only Send later ones: when the first goes; mixed with others, those are counted. */
    @Test fun sendLaterSaysWhen() {
        val now = java.time.Instant.parse("2026-10-01T10:00:00Z")
        val a = p("a").copy(sendAt = "2026-10-01T15:00:00Z"); val b = p("b").copy(sendAt = "2026-10-01T12:00:00Z")
        assertEquals("⏰ Sends at " + com.smizflark.aigile.messenger.ui.clock(b.sendAt), unsentLabel(listOf(a, b), now))
        assertEquals("⏳ 1 waiting to send", unsentLabel(listOf(a, p("c")), now))
        assertEquals("⏳ 1 waiting to send", unsentLabel(listOf(p("d").copy(sendAt = "2026-10-01T09:00:00Z")), now))   // due: just waiting
    }

    @Test fun failedOnesWin() = assertEquals("⚠️ 1 not sent: tap to retry", unsentLabel(listOf(p("a"), p("b", failed = true))))
}
