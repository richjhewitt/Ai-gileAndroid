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

    @Test fun noChatYet() = assertTrue(chatInfo(null).let { it.openCards.isEmpty() && it.pictures.isEmpty() })

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
}
