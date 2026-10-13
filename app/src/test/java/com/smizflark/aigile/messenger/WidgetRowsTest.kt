package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.widget.WidgetRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WidgetRowsTest {
    private val now = Instant.parse("2026-10-13T12:00:00Z")

    private fun chat(id: String, last: String?) =
        ChatRef(Kind.LEAD, id, id.uppercase(), "Lead", "🗂️", last = last)

    @Test fun newestFirstAndCapped() {
        val l = listOf(
            chat("a", "2026-10-13T09:00:00Z"), chat("b", "2026-10-13T11:00:00Z"),
            chat("c", null), chat("d", "2026-10-13T10:00:00Z"), chat("e", "2026-10-13T11:30:00Z"),
        )
        val rows = WidgetRows.rows(l, emptyMap(), now)
        assertEquals(listOf("lead:e", "lead:b", "lead:d", "lead:a"), rows.map { it.chat })
        assertEquals("🗂️ E", rows[0].title)
    }

    @Test fun newOnlyWhenNewerThanSeen() {
        val l = listOf(chat("a", "2026-10-13T11:00:00Z"), chat("b", "2026-10-13T11:00:00Z"), chat("c", null))
        val seen = mapOf("lead:a" to "2026-10-13T11:00:00Z", "lead:b" to "2026-10-13T10:00:00Z")
        val byChat = WidgetRows.rows(l, seen, now).associateBy { it.chat }
        assertFalse(byChat["lead:a"]!!.new)
        assertTrue(byChat["lead:b"]!!.new)
        assertFalse(byChat["lead:c"]!!.new)
    }

    @Test fun shortTimes() {
        assertEquals("now", WidgetRows.ago("2026-10-13T11:59:40Z", now))
        assertEquals("5m", WidgetRows.ago("2026-10-13T11:55:00Z", now))
        assertEquals("3h", WidgetRows.ago("2026-10-13T09:00:00Z", now))
        assertEquals("2d", WidgetRows.ago("2026-10-11T11:00:00Z", now))
        assertEquals("", WidgetRows.ago(null, now))
        assertEquals("", WidgetRows.ago("garbage", now))
    }
}
