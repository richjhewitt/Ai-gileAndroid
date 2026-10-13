package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.ChatSummary
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.SendState
import com.smizflark.aigile.messenger.ui.previewLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The chats list shows the newest message, "You: ..." when it is yours (AII-106). */
class PreviewLineTest {
    private val at = "2026-10-01T10:00:00Z"
    private fun lead(text: String?, mine: Boolean? = null) =
        Mapper.chats(listOf(ChatSummary("x", "X", lead = "Pixel", last = at, lastText = text, lastMine = mine)), emptyList())[0]
    private fun cached(ref: ChatRef, vararg b: Bubble) = Conversation(ref, b.toList())

    @Test fun noTextKeepsTheRoleLine() {
        assertNull(lead(null).preview)
        assertNull(lead("  ").preview)
        assertEquals("Pixel", previewLabel(lead(null)))
    }

    @Test fun theirMessageShowsAsIs() = assertEquals("Done, have a look", previewLabel(lead("Done,\n have a look")))

    @Test fun boardSaysMine() = assertEquals("You: thanks", previewLabel(lead("thanks", mine = true)))

    @Test fun olderBoardTheCachedChatTells() {
        val r = lead("thanks")
        assertFalse(r.previewMine)
        assertTrue(Mapper.mineFromCache(r, cached(r, Bubble("1", "Rich", "thanks", at, mine = true))).previewMine)
    }

    @Test fun cacheOfAnOlderMessageDoesNotCount() {
        val r = lead("new from the team")
        assertFalse(Mapper.mineFromCache(r, cached(r, Bubble("1", "Rich", "old", "2026-09-30T10:00:00Z", mine = true))).previewMine)
        assertFalse(Mapper.mineFromCache(r, cached(r, Bubble("1", "Pixel", "new from the team", at, mine = false))).previewMine)
        assertFalse(Mapper.mineFromCache(r, null).previewMine)
    }

    @Test fun unsentBubblesAreSkipped() {
        val r = lead("their reply")
        val c = cached(r, Bubble("1", "Pixel", "their reply", at, mine = false), Bubble("p", "Rich", "typing", "2026-10-01T11:00:00Z", mine = true, state = SendState.FAILED))
        assertFalse(Mapper.mineFromCache(r, c).previewMine)
    }

    @Test fun markdownIsDropped() {
        assertEquals("Done: Export button on each playlist - ok", Mapper.plainLine("- Done: **Export** button on each playlist - ok"))
        assertEquals("Run npm test", Mapper.plainLine("1. Run `npm test`"))
        assertEquals("Heading", Mapper.plainLine("## Heading"))
    }

    @Test fun widgetShowsItToo() =
        assertEquals("You: thanks", com.smizflark.aigile.messenger.widget.WidgetRows.rows(listOf(lead("thanks", mine = true)), emptyMap(), java.time.Instant.parse(at))[0].subtitle)

    @Test fun friendsKeepTheirLine() {
        val f = ChatRef(Kind.FRIEND, "d", "Davy", "🔒 Private", "")
        assertEquals("🔒 Private", previewLabel(f))
    }
}
