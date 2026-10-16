package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Contact
import com.smizflark.aigile.messenger.data.DemoSource
import com.smizflark.aigile.messenger.data.FriendMsg
import com.smizflark.aigile.messenger.data.FriendReacts
import com.smizflark.aigile.messenger.data.FriendReply
import com.smizflark.aigile.messenger.data.Held
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Quote
import com.smizflark.aigile.messenger.data.json
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-324: friend DMs show edited, deleted and reply lines, typing and the safety code; the demo does them in memory. */
class FriendDmTest {
    private val ref = ChatRef(Kind.FRIEND, "d", "Davy", "🔒", "")

    @Test fun contactReadsSafetyAndTyping() {
        val c = json.decodeFromString<Contact>("""{"id":"d","name":"Davy","safety":"a b c d e f","typing":true,"checked":false}""")
        val chat = Mapper.chats(emptyList(), listOf(c)).single()
        assertEquals("a b c d e f", chat.safety)
        assertTrue(chat.typing)
        assertFalse(chat.checked)
    }

    @Test fun anOlderBoardHasNoSafetyOrTyping() {
        val chat = Mapper.chats(emptyList(), listOf(json.decodeFromString<Contact>("""{"id":"d","name":"Davy"}"""))).single()
        assertEquals("", chat.safety)
        assertFalse(chat.typing)
    }

    @Test fun aDeletedLineStaysAsABubbleWithNoText() {
        val m = json.decodeFromString<List<FriendMsg>>("""[{"id":"1","dir":"out","text":"","at":"2026-10-13T01:00:00Z","deleted":"2026-10-13T02:00:00Z"}]""")
        val b = Mapper.friend(ref, "Rich", m).bubbles.single()
        assertTrue(b.deleted)
        assertEquals("", b.text)
        assertEquals("1", b.id)
    }

    @Test fun anEditedLineKeepsItsEditTime() {
        val b = Mapper.friend(ref, "Rich", listOf(FriendMsg("1", "out", "new", "2026-10-13T01:00:00Z", edited = "2026-10-13T02:00:00Z"))).bubbles.single()
        assertEquals("2026-10-13T02:00:00Z", b.edited)
        assertFalse(b.deleted)
        assertEquals("new", b.text)
    }

    @Test fun aReplyShowsItsQuotedLineAboveTheText() {
        val b = Mapper.friend(ref, "Rich", listOf(FriendMsg("2", "in", "Yes please", "2026-10-13T01:00:00Z", reply = FriendReply("1", "Fancy a game?")))).bubbles.single()
        val (quoted, rest) = Quote.split(b.text)
        assertEquals("Fancy a game?", quoted)
        assertEquals("Yes please", rest)
    }

    @Test fun aReplyToADeletedLineIsStillJustItsText() {
        val b = Mapper.friend(ref, "Rich", listOf(FriendMsg("2", "in", "Yes", "2026-10-13T01:00:00Z", reply = FriendReply("1", "")))).bubbles.single()
        assertNull(Quote.split(b.text).first)
    }

    @Test fun aHeldFriendReplySendsPlainTextAndItsId() {
        val h = Held("Yes please", "> **Davy:** Fancy a game?", null, emptyList(), reply = "d1")
        assertEquals("Yes please", h.body)
        assertEquals("d1", h.reply)
        assertTrue(Held("Yes", "> q", null, emptyList()).body.startsWith("> q"))
    }

    private fun davy(d: DemoSource) = runBlocking { d.chats() }.first { it.kind == Kind.FRIEND }

    @Test fun demoDavyHasASafetyCodeNotYetChecked() = runBlocking {
        val d = DemoSource()
        assertEquals(6, davy(d).safety.split(" ").size)
        assertFalse(davy(d).checked)
        d.setChecked(davy(d))
        assertTrue(davy(d).checked)
    }

    @Test fun demoEditChangesMyLineAndMarksIt() = runBlocking {
        val d = DemoSource()
        d.edit(davy(d), "d2", "After the barbers, yes")
        val b = d.conversation(davy(d)).bubbles.first { it.id == "d2" }
        assertEquals("After the barbers, yes", b.text)
        assertTrue(b.edited != null)
    }

    @Test fun demoDoesNotEditTheirLine() = runBlocking {
        val d = DemoSource()
        d.edit(davy(d), "d1", "changed")
        assertEquals("Fancy a game later?", d.conversation(davy(d)).bubbles.first { it.id == "d1" }.text)
    }

    @Test fun demoDeleteLeavesAMessageDeletedBubble() = runBlocking {
        val d = DemoSource()
        d.delete(davy(d), "d2")
        val c = d.conversation(davy(d))
        val b = c.bubbles.first { it.id == "d2" }
        assertTrue(b.deleted)
        assertEquals("", b.text)
        assertEquals(3, c.bubbles.size)
    }

    @Test fun reactsBecomeChipsWithCountsAndMine() {
        val m = json.decodeFromString<List<FriendMsg>>("""[{"id":"1","dir":"in","text":"hi","at":"2026-10-13T01:00:00Z","reacts":{"mine":["👍"],"theirs":["👍","❤️"]}}]""")
        val chips = Mapper.friend(ref, "Rich", m).bubbles.single().reacts
        assertEquals(listOf("👍" to 2, "❤️" to 1), chips.map { it.emoji to it.count })
        assertEquals(listOf(true, false), chips.map { it.mine })
    }

    @Test fun anOlderBoardHasNoReacts() {
        val m = json.decodeFromString<List<FriendMsg>>("""[{"id":"1","dir":"in","text":"hi","at":"2026-10-13T01:00:00Z"}]""")
        assertTrue(Mapper.friend(ref, "Rich", m).bubbles.single().reacts.isEmpty())
    }

    @Test fun aDeletedLineHasNoReacts() {
        val m = listOf(FriendMsg("1", "out", "", "2026-10-13T01:00:00Z", deleted = "2026-10-13T02:00:00Z", reacts = FriendReacts(theirs = listOf("👍"))))
        assertTrue(Mapper.friend(ref, "Rich", m).bubbles.single().reacts.isEmpty())
    }

    @Test fun demoReactTogglesMine() = runBlocking {
        val d = DemoSource()
        fun d1() = runBlocking { d.conversation(davy(d)).bubbles.first { it.id == "d1" }.reacts.single() }
        assertEquals(false to 1, d1().mine to d1().count)
        d.react(davy(d), "d1", "👍")
        assertEquals(true to 2, d1().mine to d1().count)
        d.react(davy(d), "d1", "👍")
        assertEquals(false to 1, d1().mine to d1().count)
    }

    @Test fun demoReplyCarriesTheQuotedLine() = runBlocking {
        val d = DemoSource()
        d.send(davy(d), "Yes please", null, emptyList(), "d1")
        val b = d.conversation(davy(d)).bubbles.last()
        assertEquals("Fancy a game later?", Quote.split(b.text).first)
        assertEquals("Yes please", Quote.split(b.text).second)
    }
}
