package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.BoardError
import com.smizflark.aigile.messenger.data.BoardSource
import com.smizflark.aigile.messenger.data.Kind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * The app's real API client against a throwaway board (tools/test-board.mjs), never Rich's own.
 * Runs only when TEST_BOARD is set: tools/test-app.sh starts the board and sets it.
 */
class BoardSourceTest {
    private val base = System.getenv("TEST_BOARD") ?: ""
    private val slug = System.getenv("TEST_SLUG") ?: ""
    private val dm = System.getenv("TEST_DM") ?: ""
    private val ask = System.getenv("TEST_ASK") ?: ""
    private fun board() = BoardSource(base, "test-key")

    @Test fun readsReplyAndNewMessageEndToEnd(): Unit = runBlocking {
        assumeTrue("no test board", base.isNotEmpty())
        val b = board()
        assertEquals("Owner", b.owner())

        val chats = b.chats()
        val ref = chats.first { it.kind == Kind.LEAD && it.id == slug }
        assertEquals(1, ref.waiting)   // the colour question

        val c = b.conversation(ref)
        assertTrue(c.bubbles.any { !it.mine && it.text.contains("checkout") })
        val q = c.bubbles.first { it.options.isNotEmpty() }
        assertEquals(listOf("Teal", "Orange"), q.options)
        assertEquals(ask, c.defaultTarget)   // a reply answers the waiting question

        // Answer button: the option goes to that card and the question stops waiting.
        b.send(ref, "Teal", ask)
        val after = b.conversation(ref)
        assertTrue(after.bubbles.any { it.mine && it.text == "Teal" })
        assertEquals(0, b.chats().first { it.id == slug }.waiting)

        // Reply on the 💬 card.
        b.send(ref, "Great, thanks", dm)
        assertTrue(b.conversation(ref).bubbles.any { it.mine && it.text == "Great, thanks" && it.card == dm })

        // New message: a new 💬 card.
        val id = b.newMessage(slug, "Hello team\nsecond line")
        assertNotNull(id)
        val last = b.conversation(ref).bubbles.last()
        assertEquals(id, last.card)
        assertEquals("Hello team\nsecond line", last.text)
    }

    @Test fun watchmanChatReadsAndSends(): Unit = runBlocking {
        assumeTrue("no test board", base.isNotEmpty())
        val b = board()
        val wm = b.chats().first { it.kind == Kind.WATCHMAN }
        b.conversation(wm)   // must parse
        runCatching { b.send(wm, "hello from the phone test", null) }   // may be refused (No Token Mode): must not crash
    }

    @Test fun friendlyErrorWhenTheBoardIsOff(): Unit = runBlocking {
        val off = BoardSource("http://127.0.0.1:9", "k")
        val e = runCatching { off.owner() }.exceptionOrNull()
        assertTrue(e is BoardError)
        assertTrue(e!!.message!!.contains("Can't reach the board"))
    }
}
