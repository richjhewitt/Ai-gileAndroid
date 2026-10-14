package com.smizflark.aigile.messenger.data

import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/** Runs the app's BoardSource against a throwaway test board (tools/testboard): hidden, free port, stopped afterwards. */
class BoardSourceTest {

    companion object {
        private var url: String? = null
        private var script: File? = null

        private fun node(vararg args: String): Pair<Int, String> {
            val cmd = listOf("node", script!!.path) + args
            val p = ProcessBuilder(cmd).directory(script!!.parentFile).redirectErrorStream(true).start()
            p.outputStream.close()
            val out = p.inputStream.bufferedReader().readText()
            if (!p.waitFor(90, TimeUnit.SECONDS)) { p.destroyForcibly(); return -1 to out }
            return p.exitValue() to out.trim()
        }

        @BeforeClass @JvmStatic fun startBoard() {
            var d: File? = File("").absoluteFile
            while (d != null && !File(d, "tools/testboard/testboard.mjs").exists()) d = d.parentFile
            script = d?.let { File(it, "tools/testboard/testboard.mjs") }
            assumeTrue("tools/testboard not found", script != null)
            // The AI-gile board to run: AIGILE_BOARD, or a folder called AI-gile next to one of our parents.
            var b: File? = System.getenv("AIGILE_BOARD")?.let { File(it) }
            var up: File? = script!!.parentFile
            while (b == null && up != null) { File(up, "AI-gile/server.js").takeIf { it.exists() }?.let { b = it.parentFile }; up = up.parentFile }
            assumeTrue("no AI-gile board found (set AIGILE_BOARD)", b != null)
            val (code, out) = node("start", "--board", b!!.path)
            check(code == 0) { "test board did not start: $out" }
            url = out.lines().last { it.startsWith("http") }
        }

        @AfterClass @JvmStatic fun stopBoard() {
            if (url != null) node("stop")
        }
    }

    private fun source() = BoardSource(url!!, "test-key")
    private fun team(s: BoardSource) = runBlocking { s.chats() }.first { it.id == "test-team" }

    @Test fun signInReadsTheOwnerName() {
        assertEquals("Rich", runBlocking { source().owner() })
    }

    @Test fun chatsListHasTheTeam() {
        val chats = runBlocking { source().chats() }
        assertTrue(chats.any { it.id == "test-team" })
    }

    @Test fun openingAChatShowsTheSeededMessages() {
        val s = source()
        val c = runBlocking { s.conversation(team(s)) }
        assertTrue(c.bubbles.any { it.text.contains("Hi Rich, the test board is up") })
    }

    @Test fun aReplyIsSentAndReadBack() = runBlocking {
        val s = source()
        val ref = team(s)
        val target = s.conversation(ref).targets.firstOrNull { it.id != null }?.id
        assertNotNull("seeded card should be a reply target", target)
        val text = "Reply from the test ${System.nanoTime()}"
        assertEquals(target, s.send(ref, text, target))
        val back = s.conversation(ref).bubbles.firstOrNull { it.text == text }
        assertNotNull(back)
        assertTrue(back!!.mine)
    }

    @Test fun aReplyToTheLeadChatLandsThere() = runBlocking {
        val s = source()
        val text = "To the Lead chat ${System.nanoTime()}"
        assertEquals(LEAD_CHAT, s.send(team(s), text, LEAD_CHAT))
        val back = s.conversation(team(s)).bubbles.firstOrNull { it.text == text }
        assertNotNull(back)
        assertEquals(LEAD_CHAT, back!!.card)
    }

    @Test fun aNewMessageMakesACard() = runBlocking {
        val s = source()
        val text = "Brand new ${System.nanoTime()}"
        val id = s.newMessage("test-team", text)
        assertNotNull(id)
        val back = s.conversation(team(s)).bubbles.firstOrNull { it.text == text }
        assertNotNull(back)
        assertEquals(id, back!!.card)
    }
}
