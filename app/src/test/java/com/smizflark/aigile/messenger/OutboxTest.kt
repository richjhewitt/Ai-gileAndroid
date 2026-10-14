package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Outbox
import com.smizflark.aigile.messenger.data.Pending
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A failed message can be retried or deleted (AII-62). */
class OutboxTest {
    private fun p(id: String, failed: Boolean = false, offline: Boolean = false) =
        Pending(id, "lead:ai-gile", "hi", null, "2026-10-01T10:00:00Z", failed, offline = offline)

    @Test fun failMarksOnlyThatMessage() {
        val l = Outbox.fail(listOf(p("a"), p("b")), "b", offline = false)
        assertFalse(l[0].failed); assertTrue(l[1].failed); assertFalse(l[1].offline)
    }

    @Test fun retryClearsTheMarkAndKeepsTheMessage() {
        val l = Outbox.retry(listOf(p("a", failed = true), p("b", failed = true)), "a")
        assertFalse(l[0].failed); assertEquals("hi", l[0].text); assertTrue(l[1].failed)
    }

    @Test fun deleteRemovesOnlyThatMessage() =
        assertEquals(listOf("b"), Outbox.remove(listOf(p("a", failed = true), p("b")), "a").map { it.id })

    /** AII-151: gave up since the last look (refused, or offline tries ran out): one alert each, never twice. */
    @Test fun newlyFailedOnesGetOneAlert() {
        val before = listOf(p("old", failed = true), p("refused"), p("ran-out", failed = true, offline = true), p("still-offline", failed = true, offline = true))
        val after = listOf(p("old", failed = true), p("refused", failed = true), p("ran-out", failed = true), p("still-offline", failed = true, offline = true))
        assertEquals(listOf("refused", "ran-out"), Outbox.newlyFailed(before, after).map { it.id })
        assertEquals(emptyList<String>(), Outbox.newlyFailed(after, after).map { it.id })
    }

    /** AII-169: Change time moves only that message, and it still waits. */
    @Test fun changeTimeMovesOnlyThatOne() {
        val l = listOf(p("a").copy(sendAt = "2026-10-13T14:00:00Z"), p("b").copy(sendAt = "2026-10-13T14:00:00Z"))
        val moved = Outbox.moveTo(l, "a", java.time.Instant.parse("2026-10-13T17:30:00Z"))
        assertEquals(listOf("2026-10-13T17:30:00Z", "2026-10-13T14:00:00Z"), moved.map { it.sendAt })
    }

    @Test fun onlyRefusedOnesGetTheRedMark() {
        val l = listOf(p("new"), p("offline", failed = true, offline = true), p("refused", failed = true))
        assertEquals(listOf("refused"), Outbox.failedForGood(l).map { it.id })
    }
}
