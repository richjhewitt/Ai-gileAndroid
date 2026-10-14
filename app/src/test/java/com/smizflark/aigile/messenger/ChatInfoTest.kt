package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Target
import com.smizflark.aigile.messenger.data.DemoSource
import com.smizflark.aigile.messenger.data.chatInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-80: what the chat details sheet lists. */
class ChatInfoTest {
    private val ref = ChatRef(Kind.LEAD, "x", "X", "", "")
    private fun b(k: String, vararg p: Pic) = Bubble(k, "a", "t", "2026-01-01", false, pics = p.toList())
    private val a = Pic("/api/a", "a.png", "image/png")
    private val c = Pic("/api/c", "c.jpg", "image/jpeg")
    private val doc = Pic("/api/d", "d.pdf", "application/pdf")

    @Test fun linksNewestFirstOnceEach() {
        val l = com.smizflark.aigile.messenger.data.Links.of(listOf("old https://a.com/x.", "[Named](https://b.org/p) then https://a.com/x", "dnbloco.com, and (https://c.io)"))
        assertEquals(listOf("https://dnbloco.com", "https://c.io", "https://b.org/p", "https://a.com/x"), l.map { it.url })
        assertEquals("Named", l[2].title)
        assertEquals("dnbloco.com", l[0].title)
    }
    @Test fun linksSkipCodeAndCapAt50() {
        val ls = com.smizflark.aigile.messenger.data.Links
        assertTrue(ls.inText("`https://a.com` and ```\nhttps://b.com\n``` ok").isEmpty())
        assertTrue(ls.inText("mail me@dnbloco.com, version 1.2.3 and file.txt").isEmpty())
        assertEquals(50, ls.of((1..80).map { "https://x.com/$it" }).size)
        assertEquals("https://x.com/80", ls.of((1..80).map { "https://x.com/$it" }).first().url)
    }
    @Test fun chatInfoCarriesLinks() {
        val c = Conversation(ref, listOf(Bubble("1", "a", "go to https://a.com", "2026-01-01", false)), emptyList())
        assertEquals(listOf("https://a.com"), chatInfo(c).links.map { it.url })
        assertTrue(chatInfo(null).links.isEmpty())
    }

    @Test fun filesNewestFirstOnceNoImages() {
        val x = Pic("/api/x", "x.txt")
        val convo = Conversation(ref, listOf(b("1", doc, a), b("2", x), b("3", doc)))
        assertEquals(listOf(doc, x), chatInfo(convo).files)
        assertTrue(chatInfo(null).files.isEmpty())
    }
    @Test fun mimeFromTypeOrName() {
        assertEquals("application/pdf", com.smizflark.aigile.messenger.data.fileMime(doc))
        assertEquals("text/csv", com.smizflark.aigile.messenger.data.fileMime(Pic("/api/q", "Data.CSV")))
        assertEquals("application/zip", com.smizflark.aigile.messenger.data.fileMime(Pic("/api/q", "a.zip", "application/octet-stream")))
        assertEquals("*/*", com.smizflark.aigile.messenger.data.fileMime(Pic("/api/q", "noext")))
    }

    @Test fun noChatYet()= assertTrue(chatInfo(null).let { it.openCards.isEmpty() && it.pictures.isEmpty() })

    @Test fun openCardsAreTheWaitingOnes() {
        val t = listOf(Target("A-1", "A-1 one", waiting = true), Target("A-2", "A-2 two"), Target(null, "New message"))
        assertEquals(listOf("A-1"), chatInfo(Conversation(ref, emptyList(), t)).openCards.map { it.id })
    }

    @Test fun picturesNewestFirstImagesOnlyOnce() {
        val convo = Conversation(ref, listOf(b("1", a), b("2", doc, c), b("3", a)))
        assertEquals(listOf(a, c), chatInfo(convo).pictures)
    }

    @Test fun demoChatHasPicturesOnThePhone() = runBlocking {   // AII-86, AII-91: two, so swiping can be tried
        val demo = DemoSource()
        val pics = chatInfo(demo.conversation(demo.chats().first { it.id == "ai-gile" })).pictures
        assertEquals(2, pics.size)
        assertTrue(pics.all { it.local && it.image })
    }

    @Test fun aboutCountsSentMessagesAndFirstDay() {
        fun m(at: String, mine: Boolean, st: com.smizflark.aigile.messenger.data.SendState = com.smizflark.aigile.messenger.data.SendState.SENT) =
            Bubble(at, "a", "t", at, mine, state = st)
        val conv = Conversation(ref, listOf(m("2026-10-03T08:00:00Z", false), m("2026-10-04T08:00:00Z", true), m("2026-10-05T08:00:00Z", true, com.smizflark.aigile.messenger.data.SendState.FAILED)))
        val ab = com.smizflark.aigile.messenger.data.about(conv, java.time.ZoneOffset.UTC)!!
        assertEquals(2, ab.total); assertEquals(1, ab.mine); assertEquals(java.time.LocalDate.of(2026, 10, 3), ab.since)
        assertEquals(null, com.smizflark.aigile.messenger.data.about(Conversation(ref, emptyList())))
    }
    @Test fun aboutDateAddsYearOnlyWhenNotThisYear() {
        val today = java.time.LocalDate.of(2026, 10, 14)
        assertEquals("3 Oct", com.smizflark.aigile.messenger.data.aboutDate(java.time.LocalDate.of(2026, 10, 3), today, java.util.Locale.ENGLISH))
        assertEquals("3 Oct 2025", com.smizflark.aigile.messenger.data.aboutDate(java.time.LocalDate.of(2025, 10, 3), today, java.util.Locale.ENGLISH))
    }
}
