package com.smizflark.aigile.messenger.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AII-414: reply payloads and ack handling on the watch. */
class WearRepliesTest {
    @Test fun parsesRepliesAndDropsBlanks() {
        assertEquals(listOf("A", "B"), WearReplies.parseReplies("""["A"," ","B"]"""))
        assertEquals(emptyList<String>(), WearReplies.parseReplies("nope"))
    }

    @Test fun requestCarriesKeyTextAndId() {
        val s = String(WearReplies.request("lead:a", "hi \"you\"", "id1"))
        assertEquals("""{"key":"lead:a","text":"hi \"you\"","id":"id1"}""", s)
    }

    @Test fun ackSettlesOnlyItsLine() {
        val l = listOf(Outgoing("1", "k", "a"), Outgoing("2", "k", "b"))
        val r = WearReplies.settle(l, WearReplies.parseAck("""{"id":"2","ok":true}""".toByteArray())!!)
        assertEquals(listOf(ReplyStatus.SENDING, ReplyStatus.SENT), r.map { it.status })
    }

    @Test fun lockedAckAndUnknownErrors() {
        assertEquals(ReplyStatus.LOCKED, WearReplies.statusFor(Ack("1", false, "locked")))
        assertEquals(ReplyStatus.NO_CHAT, WearReplies.statusFor(Ack("1", false, "no-chat")))
        assertEquals(ReplyStatus.FAILED, WearReplies.statusFor(Ack("1", false, "boom")))
        assertNull(WearReplies.parseAck("junk".toByteArray()))
    }

    @Test fun noAckInTimeMeansSavedButAnAnsweredLineStays() {
        val l = listOf(Outgoing("1", "k", "a", ReplyStatus.SENT), Outgoing("2", "k", "b"))
        assertEquals(ReplyStatus.SENT, WearReplies.timeout(l, "1")[0].status)
        assertEquals(ReplyStatus.SAVED, WearReplies.timeout(l, "2")[1].status)
    }
}
