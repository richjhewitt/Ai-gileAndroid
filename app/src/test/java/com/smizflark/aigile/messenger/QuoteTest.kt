package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Quote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Reply to one message, quoting it (AII-60). */
class QuoteTest {
    @Test fun quotesTheFirstLineWithWhoSaidIt() =
        assertEquals("> **Lead:** Menu is fixed", Quote.of("Lead", "\n- **Menu** is fixed\nmore detail"))

    @Test fun longLinesAreCut() {
        val q = Quote.of("Lead", "x".repeat(300))
        assertEquals(2 + "**Lead:** ".length + 120 + 1, q.length); assertEquals('…', q.last())
    }

    @Test fun quotingAReplyQuotesItsOwnWordsNotTheOldQuote() =
        assertEquals("> **Rich:** yes please", Quote.of("Rich", "> **Lead:** shall I?\n\nyes please"))

    @Test fun aPictureOnlyMessage() = assertEquals("> **Davy:** 📷 Picture", Quote.of("Davy", "", pics = 1))

    @Test fun sendingPutsTheQuoteFirst() {
        assertEquals("> **Lead:** hi\n\nhello", Quote.with("> **Lead:** hi", "hello"))
        assertEquals("hello", Quote.with(null, "hello"))
    }

    @Test fun splittingForTheBubble() {
        assertEquals("**Lead:** hi" to "hello", Quote.split("> **Lead:** hi\n\nhello"))
        val (q, rest) = Quote.split("plain text")
        assertNull(q); assertEquals("plain text", rest)
    }
}
