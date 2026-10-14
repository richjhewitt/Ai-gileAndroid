package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Outbox
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.SendState
import com.smizflark.aigile.messenger.ui.Row
import com.smizflark.aigile.messenger.ui.rows
import com.smizflark.aigile.messenger.work.later
import com.smizflark.aigile.messenger.work.waitingToSend
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Send later (AII-146): held back until its time, then it goes like any waiting message. */
class SendLaterTest {
    private val now = Instant.parse("2026-10-14T03:00:00Z")
    private fun p(id: String, sendAt: String? = null, failed: Boolean = false, offline: Boolean = false) =
        Pending(id, "lead:ai-gile", "hi", null, "2026-10-14T02:59:00Z", failed, offline = offline, sendAt = sendAt)

    @After fun clock() { Now.clock = Clock.systemDefaultZone() }

    @Test fun heldUntilItsTime() {
        assertTrue(later(p("a", "2026-10-14T08:00:00Z"), now))
        assertFalse(later(p("b", "2026-10-14T02:00:00Z"), now))
        assertFalse(later(p("c"), now))
        assertFalse(later(p("d", "not a time"), now))
    }

    @Test fun onlyDueOnesGoByThemselves() {
        val out = listOf(p("now"), p("later", "2026-10-14T08:00:00Z"), p("due", "2026-10-14T02:30:00Z"),
            p("due-offline", "2026-10-14T02:30:00Z", failed = true, offline = true))
        assertEquals(listOf("now", "due", "due-offline"), waitingToSend(out, now).map { it.id })
    }

    /** AII-150: Edit puts its words back in the box, above anything already typed. */
    @Test fun editPutsItBackInTheBox() {
        assertEquals("hi", Outbox.editBack(p("a", "2026-10-14T08:00:00Z"), ""))
        assertEquals("hi", Outbox.editBack(p("a", "2026-10-14T08:00:00Z"), "  "))
        assertEquals("hi\nand more", Outbox.editBack(p("a", "2026-10-14T08:00:00Z"), "and more"))
    }

    @Test fun theChatShowsItsSendTime() {
        Now.clock = Clock.fixed(now, ZoneOffset.UTC)
        val r = rows(null, listOf(p("a", "2026-10-14T08:00:00Z"), p("b")), "Rich").filterIsInstance<Row.Msg>()
        assertEquals(SendState.LATER, r[0].b.state)
        assertEquals("2026-10-14T08:00:00Z", r[0].b.at)
        assertEquals(SendState.SENDING, r[1].b.state)
    }

    /** AII-175: only Send later messages that left the outbox get a Sent alert; ones still waiting, failed or sent at once don't. */
    @Test fun sentAlertOnlyForSendLaterThatWent() {
        val before = listOf(p("went", "2026-10-14T02:30:00Z"), p("waits", "2026-10-14T08:00:00Z"), p("plain"), p("stuck", "2026-10-14T02:00:00Z"))
        val after = listOf(before[1], before[3].copy(failed = true))
        assertEquals(listOf("went"), Outbox.sentLater(before, after).map { it.id })
    }

    /** AII-166: every waiting Send later message, in every chat, soonest first; sent-at-once and gone chats left out. */
    @Test fun waitingListSoonestFirst() {
        val a = com.smizflark.aigile.messenger.data.ChatRef(com.smizflark.aigile.messenger.data.Kind.LEAD, "a", "AI-gile", "", "")
        val b = com.smizflark.aigile.messenger.data.ChatRef(com.smizflark.aigile.messenger.data.Kind.FRIEND, "b", "Davy", "", "")
        val l = listOf(p("late", "2026-10-15T09:00:00Z").copy(chat = a.key), p("now").copy(chat = a.key),
            p("soon", "2026-10-14T05:00:00Z").copy(chat = b.key), p("gone", "2026-10-14T04:00:00Z").copy(chat = "lead:old"))
        assertEquals(listOf(b to "soon", a to "late"), Outbox.waiting(l, listOf(a, b), now).map { (c, x) -> c to x.id })
    }
}
