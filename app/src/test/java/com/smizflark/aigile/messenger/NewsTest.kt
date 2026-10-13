package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatRef
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

    @Test fun bulletsLoseTheirBlankLines() {
        assertEquals("- one\n- two\n\nAfter", Markdown.squeeze("- one\n\n- two\n\n\n\nAfter\n"))
    }
}
