package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.CardInfo
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.ChatSummary
import com.smizflark.aigile.messenger.data.Contact
import com.smizflark.aigile.messenger.data.FriendMsg
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.OpenCard
import com.smizflark.aigile.messenger.data.WireMsg
import com.smizflark.aigile.messenger.data.WireThread
import com.smizflark.aigile.messenger.data.json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Journeys 2 to 4 (AII-4..6): the board's answers as the screens show them. */
class MapperTest {
    private val lead = ChatRef(Kind.LEAD, "ai-gile", "AI-gile", "Lead", "🗂️")

    @Test fun chatsListLeadsWatchmanThenFriendsNewestFirst() {
        val l = Mapper.chats(
            listOf(ChatSummary("ai-gile", "AI-gile", unread = 2), ChatSummary("_watchman", watchman = true)),
            listOf(Contact("a", "Ann", last = "2026-10-01T00:00:00Z"), Contact("b", "Bob", last = "2026-10-02T00:00:00Z"),
                Contact("x", "Blocked", blocked = true)),
        )
        assertEquals(listOf("lead:ai-gile", "watchman:_watchman", "friend:b", "friend:a"), l.map { it.key })
        assertEquals(2, l[0].waiting)
    }

    @Test fun unreadComesThroughForWatchmanAndFriends() {
        val l = Mapper.chats(listOf(ChatSummary("_watchman", watchman = true, unread = 3)),
            listOf(Contact("a", "Ann", last = "2026-10-02T00:00:00Z", lastIn = "2026-10-01T00:00:00Z")))
        assertEquals(3, l[0].unread)
        assertEquals("2026-10-01T00:00:00Z", l[1].lastIn)
    }

    @Test fun realBoardJsonParses() {
        val raw = """[{"slug":"tunio","name":"Tunio","color":"#ca8a04","emoji":"🎵","logo":"/api/projects/tunio/logo?v=1",
            "lead":"Lead","leadPic":"","unread":1,"last":"2026-10-13T04:50:22.653Z","extra":{"x":1}}]"""
        val s = json.decodeFromString<List<ChatSummary>>(raw)
        val c = Mapper.chats(s, emptyList()).single()
        assertEquals("/api/projects/tunio/logo?v=1", c.pic)   // no lead picture: the project logo
        assertEquals(1, c.waiting)
    }

    private fun thread(vararg m: WireMsg, open: List<OpenCard> = emptyList(), cards: Map<String, CardInfo> = emptyMap()) =
        WireThread("ai-gile", messages = m.toList(), open = open, cards = cards)

    @Test fun replyGoesToTheQuestionWaitingOnYou() {
        val c = Mapper.conversation(lead, thread(
            WireMsg("A-1", "💬 Hi", "Rich", "hi", "2026-10-13T01:00:00Z", mine = true),
            WireMsg("A-2", "Dark mode?", "Lead", "Which?", "2026-10-13T02:00:00Z", options = listOf("Yes", "No")),
            open = listOf(OpenCard("A-2", "Dark mode?")),
            cards = mapOf("A-1" to CardInfo("💬 Hi", "idea"), "A-2" to CardInfo("Dark mode?", "question", waiting = true)),
        ))
        assertEquals("A-2", c.defaultTarget)
        assertEquals(listOf("A-2", "A-1", null), c.targets.map { it.id })
        assertTrue(c.bubbles[1].waiting)
        assertEquals("2026-10-13T02:00:00Z", c.lastFromThem)
    }

    @Test fun withNothingWaitingReplyCarriesOnInTheLastCard() {
        val c = Mapper.conversation(lead, thread(
            WireMsg("A-1", "💬 Hi", "Rich", "hi", "2026-10-13T01:00:00Z", mine = true),
            WireMsg("A-1", "💬 Hi", "Lead", "hello", "2026-10-13T01:01:00Z"),
            cards = mapOf("A-1" to CardInfo("💬 Hi", "idea")),
        ))
        assertEquals("A-1", c.defaultTarget)
    }

    @Test fun doneCardsAreNotReplyTargets() {
        val c = Mapper.conversation(lead, thread(
            WireMsg("A-1", "💬 Old", "Lead", "done", "2026-10-13T01:00:00Z"),
            cards = mapOf("A-1" to CardInfo("💬 Old", "idea", done = true)),
        ))
        assertNull(c.defaultTarget)
        assertEquals(listOf(null), c.targets.map { it.id })
    }

    @Test fun emptyMessagesAreDropped() {
        val c = Mapper.conversation(lead, thread(WireMsg("A-1", "x", "Rich", null, "2026-10-13T01:00:00Z", mine = true)))
        assertTrue(c.bubbles.isEmpty())
    }

    @Test fun friendMessagesShowUnverifiedPlainly() {
        val f = ChatRef(Kind.FRIEND, "d", "Davy", "🔒", "🔒")
        val c = Mapper.friend(f, "Rich", listOf(
            FriendMsg("1", "out", "hi", "2026-10-13T01:00:00Z"),
            FriendMsg("2", "in", null, "2026-10-13T02:00:00Z", verified = false),
        ))
        assertEquals("Rich", c.bubbles[0].author)
        assertTrue(c.bubbles[1].unverified)
        assertTrue(c.bubbles[1].text.startsWith("⚠️ Not verified"))
    }

    @Test fun quickTitleIsFirstLineLikeTheBoardPage() {
        assertEquals("💬 first line", Mapper.quickTitle("first line\nsecond"))
        assertEquals("💬 ".length + 80, Mapper.quickTitle("x".repeat(200)).length)
        assertEquals("Phone app", Mapper.clean("💬 Phone app"))
    }

    @Test fun `AII-64 initials when a chat has no picture or emoji`() {
        assertEquals("TD", Mapper.initials("Tunio Discord"))
        assertEquals("AG", Mapper.initials("AI-gile"))
        assertEquals("R", Mapper.initials("rich"))
        assertEquals("?", Mapper.initials("  "))
        val r = Mapper.chats(listOf(ChatSummary("x", "X")), listOf(Contact(id = "d", name = "Davy")))
        assertEquals("", r[0].emoji)
        assertEquals("", r[1].emoji)
    }
}
