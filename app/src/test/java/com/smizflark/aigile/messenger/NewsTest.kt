package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.News
import com.smizflark.aigile.messenger.ui.Markdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Journey 5 (AII-7): what counts as new, quiet hours; and the bubble text tidy-up. */
class NewsTest {
    private val t1 = "2026-10-13T01:00:00Z"
    private val t2 = "2026-10-13T02:00:00Z"

    @Test fun firstLookOnlyRecordsNoAlertForOldHistory() {
        val (news, told) = News.diff(listOf(News.Item("lead:a", "A", t1)), emptyMap(), emptyMap())
        assertTrue(news.isEmpty())
        assertEquals(t1, told["lead:a"])
    }

    @Test fun newerMessageFromThemAlertsOnce() {
        val items = listOf(News.Item("lead:a", "A", t2))
        val (news, told) = News.diff(items, mapOf("lead:a" to t1), emptyMap())
        assertEquals(1, news.size)
        val (again, _) = News.diff(items, mapOf("lead:a" to t1), told)
        assertTrue(again.isEmpty())   // already told
    }

    @Test fun seenInTheAppMeansNoAlert() {
        val (news, _) = News.diff(listOf(News.Item("lead:a", "A", t1)), mapOf("lead:a" to t2), emptyMap())
        assertTrue(news.isEmpty())
    }

    @Test fun candidatesAreChatsThatMoved() {
        val a = ChatRef(Kind.LEAD, "a", "A", "", "", last = t2)
        val b = ChatRef(Kind.LEAD, "b", "B", "", "", last = t1)
        assertEquals(listOf("lead:a"), News.candidates(listOf(a, b), mapOf("lead:b" to t1), mapOf("lead:a" to t1)).map { it.key })
    }

    /* ---------- AII-25: unread badges and mark as read ---------- */

    private val t3 = "2026-10-13T03:00:00Z"
    private fun b(at: String, mine: Boolean = false) = Bubble(at, "x", "hi", at, mine)
    private fun convo(ref: ChatRef, vararg b: Bubble) = Conversation(ref, b.toList())

    @Test fun leadCountsMessagesFromThemAfterSeen() {
        val a = ChatRef(Kind.LEAD, "a", "A", "", "", last = t3)
        val saved = convo(a, b(t1), b(t2), b(t3, mine = true))
        assertEquals(1, News.unread(a, t1, saved))   // t2 only: t1 seen, t3 is mine
        assertEquals(2, News.unread(a, null, saved))
        assertEquals(0, News.unread(a, t3, saved))
        assertEquals(0, News.unread(a, t1, null))      // nothing saved: the list shows a dot instead
        assertTrue(News.fresh(a, t1))
    }

    @Test fun watchmanGoesByTheBoardsCount() {
        val w = ChatRef(Kind.WATCHMAN, "_watchman", "LSM", "", "", last = t3, unread = 0)
        assertEquals(0, News.unread(w, null, convo(w, b(t3))))   // read on the PC: read here
        assertFalse(News.fresh(w, null))
        val w2 = w.copy(unread = 3)
        assertEquals(3, News.unread(w2, t3, null))
        assertTrue(News.fresh(w2, t3))
    }

    @Test fun friendWithNewerMessageCountsAtLeastOne() {
        val f = ChatRef(Kind.FRIEND, "d", "Davy", "", "", last = t3, lastIn = t2)
        assertEquals(1, News.unread(f, t1, null))
        assertEquals(0, News.unread(f, t2, null))
        assertEquals(0, News.unread(f.copy(lastIn = null), t1, null))   // only my own message moved it
    }

    @Test fun markAsReadGoesUpToTheNewestMessage() {
        val a = ChatRef(Kind.LEAD, "a", "A", "", "", last = t2)
        assertEquals(t3, News.readUpTo(a, convo(a, b(t3))))
        assertEquals(t2, News.readUpTo(a, null))
        assertEquals(null, News.readUpTo(a.copy(last = null), null))
    }

    @Test fun quietHoursAcrossMidnight() {
        assertTrue(News.quiet(23, 22, 7)); assertTrue(News.quiet(3, 22, 7))
        assertFalse(News.quiet(7, 22, 7)); assertFalse(News.quiet(12, 22, 7))
        assertTrue(News.quiet(13, 12, 14)); assertFalse(News.quiet(5, 5, 5))
    }

    @Test fun summaryNamesTheChats() {
        val i = { n: String -> News.Item(n, n, t1) }
        assertEquals("A", News.summary(listOf(i("A"))))
        assertEquals("A, B", News.summary(listOf(i("A"), i("B"))))
        assertEquals("A and 2 more", News.summary(listOf(i("A"), i("B"), i("C"))))
    }

    @Test fun onlyThisChatsCardIdsBecomeLinks() {   // AII-28
        val refs = Markdown.cardRefs("See AIG-464 and AII-12, not XAIG-4640 or AIG-9", setOf("AIG-464", "AII-12"))
        assertEquals(listOf("AIG-464", "AII-12"), refs.map { it.second })
        assertEquals(4..10, refs[0].first)
        assertTrue(Markdown.cardRefs("AIG-464", emptySet()).isEmpty())
    }

    @Test fun bulletsLoseTheirBlankLines() {
        assertEquals("- one\n- two\n\nAfter", Markdown.squeeze("- one\n\n- two\n\n\n\nAfter\n"))
    }
}
