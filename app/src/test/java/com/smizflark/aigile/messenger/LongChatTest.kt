package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.ui.Find
import com.smizflark.aigile.messenger.ui.Row
import com.smizflark.aigile.messenger.ui.daysAbove
import com.smizflark.aigile.messenger.ui.rows
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * AII-348: a 10,000 message chat. Everything the chat screen works out from the messages stays linear,
 * so opening, finding and the day pill keep up. Budgets are loose so a busy PC never fails them.
 */
class LongChatTest {
    private val n = 10_000
    private val start = Instant.parse("2026-01-01T09:00:00Z")
    private val chat = Conversation(
        ChatRef(Kind.LEAD, "big", "Big", "", ""),
        List(n) { i ->
            Bubble("k$i", if (i % 2 == 0) "Rich" else "Lead", "Message number $i about the plan" + if (i % 97 == 0) " banana" else "",
                start.plusSeconds(i * 600L).toString(), mine = i % 2 == 0, card = "AIG-${i / 50}", cardTitle = "Card ${i / 50}")
        })

    private fun ms(block: () -> Unit): Long { val t = System.nanoTime(); block(); return (System.nanoTime() - t) / 1_000_000 }

    @Test fun rowsForTenThousandMessages() {
        rows(chat, emptyList(), "Rich")   // warm up the JIT
        var out = emptyList<Row>()
        val took = ms { out = rows(chat, emptyList(), "Rich", seenAtOpen = "k9000") }
        assertEquals(n, out.count { it is Row.Msg })
        assertTrue("rows took $took ms", took < 1_500)
        val pill = ms { daysAbove(out.asReversed()) }
        assertTrue("day pill took $pill ms", pill < 500)
    }

    @Test fun findInTenThousandMessages() {
        val texts = chat.bubbles.map { it.text }
        Find.matches(texts, "banana")
        var hits = emptyList<Int>()
        val took = ms { hits = Find.matches(texts, "banana") }
        assertEquals((0 until n).count { it % 97 == 0 }, hits.size)
        assertTrue("find took $took ms", took < 1_000)
    }

    /** The saved copy is read when the chat opens; it must come back whole and quickly. */
    @Test fun savedCopyOfTenThousandMessages() {
        val json = Json { ignoreUnknownKeys = true }
        val text = json.encodeToString(Conversation.serializer(), chat)
        json.decodeFromString(Conversation.serializer(), text)
        var back: Conversation? = null
        val took = ms { back = json.decodeFromString(Conversation.serializer(), text) }
        assertEquals(n, back!!.bubbles.size)
        println("AII-348: 10,000 messages = ${text.length / 1024} KB saved, read back in $took ms")
        assertTrue("reading the saved chat took $took ms", took < 2_000)
    }
}
