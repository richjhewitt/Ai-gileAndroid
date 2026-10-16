package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.QuickReply
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-24: replying straight from the notification. */
class QuickReplyTest {
    private val ref = ChatRef(Kind.LEAD, "othertrains", "OtherTrains", "Conductor", "🚂")
    private fun bubble(text: String, mine: Boolean) = Bubble("k$text", if (mine) "Rich" else "Conductor", text, "2026-10-13T01:00:00Z", mine)

    @Test fun emptyRepliesAreNotSent() {
        assertNull(QuickReply.clean(null))
        assertNull(QuickReply.clean("   \n"))
        assertEquals("yes please", QuickReply.clean("  yes please \n"))
    }

    @Test fun replyGoesWhereTheChatScreenWouldSendIt() {
        assertEquals("OTH-9", QuickReply.target(Conversation(ref, emptyList(), defaultTarget = "OTH-9")))
        assertNull(QuickReply.target(Conversation(ref, emptyList())))   // a new 💬 message
        assertNull(QuickReply.target(null))
    }

    @Test fun previewIsTheirNewestMessageShortened() {
        val c = Conversation(ref, listOf(bubble("old one", false), bubble("Go?\n\n- build it  tonight", false), bubble("mine", true)))
        assertEquals("Go? - build it tonight", QuickReply.preview(c))
        val long = QuickReply.preview(Conversation(ref, listOf(bubble("x".repeat(500), false))), max = 20)!!
        assertEquals(20, long.length)
        assertTrue(long.endsWith("…"))
        assertNull(QuickReply.preview(Conversation(ref, listOf(bubble("only mine", true)))))
    }

    @Test fun eachChatKeepsItsOwnNotification() {
        assertEquals(QuickReply.notificationId("lead:a"), QuickReply.notificationId("lead:a"))
        assertNotEquals(QuickReply.notificationId("lead:a"), QuickReply.notificationId("friend:a"))
        for (k in listOf("lead:a", "friend:zz", "watchman:_watchman", "")) assertTrue(QuickReply.notificationId(k) >= 100)
    }

    @Test fun outcomeSaysWhatHappened() {
        assertTrue(QuickReply.outcome(true, null).startsWith("Sent"))
        assertTrue(QuickReply.outcome(false, "Can't reach the board.").contains("Can't reach the board."))
        assertTrue(QuickReply.outcome(false, null).contains("try again"))
    }

    @Test fun recentLinesAreTheirNewestOnesOldestFirst() {
        val bs = (1..7).map { Bubble("k$it", "Conductor", "msg $it", "2026-10-13T0$it:00:00Z", false) } +
            Bubble("m", "Rich", "mine", "2026-10-13T08:00:00Z", true)
        val lines = QuickReply.recentLines(Conversation(ref, bs), null)
        assertEquals(listOf("msg 3", "msg 4", "msg 5", "msg 6", "msg 7"), lines)
        assertEquals(listOf("msg 6", "msg 7"), QuickReply.recentLines(Conversation(ref, bs), "2026-10-13T05:00:00Z"))
        assertTrue(QuickReply.recentLines(null, null).isEmpty())
    }

    @Test fun longLinesAreCut() {
        assertEquals("a b", QuickReply.cut("  a \n b ", 10))
        assertEquals("abcd…", QuickReply.cut("abcdefgh", 5))
    }

    @Test fun summaryGoesWhenNoChatAlertsRemain() {
        val id = QuickReply.notificationId("lead:a")
        assertTrue(QuickReply.summaryIsLonely(emptyList()))
        assertTrue(QuickReply.summaryIsLonely(listOf(QuickReply.SUMMARY_ID)))
        assertEquals(false, QuickReply.summaryIsLonely(listOf(QuickReply.SUMMARY_ID, id)))
    }
}
