package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.BoardSource
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test

/** Journey 3 (AII-5): read a lead's chat and reply, against a real throwaway board. */
class ReplyTest {
    companion object {
        private var board: TestBoard? = null

        @BeforeClass @JvmStatic fun start() {
            board = TestBoard.find()?.also { it.call("POST", "/api/projects", """{"name":"Test Team","key":"TST"}""") }
        }

        @AfterClass @JvmStatic fun stop() { board?.close() }
    }

    /** Rich's 💬 card with the lead's question on it (Red / Blue buttons). Each test gets its own, so order doesn't matter. */
    private fun ask(): String {
        val b = board!!
        val card = b.call("POST", "/api/projects/test-team/cards",
            """{"title":"💬 Pick a colour","description":"Which colour?","type":"task","status":"team","createdBy":"Rich","quick":true}""")
        val id = json.parseToJsonElement(card).jsonObject["id"]!!.jsonPrimitive.content
        b.call("POST", "/api/projects/test-team/cards/$id/comments",
            """{"author":"Lead","text":"Red or blue?","needsYou":true,"mark":"ask","options":"Red,Blue"}""")
        return id
    }

    private fun source() = board!!.let { BoardSource(it.url, "") }

    private suspend fun team() = source().chats().first { it.kind == Kind.LEAD && it.id == "test-team" }

    @Test fun questionShowsWithAnswerButtonsAndIsTheDefaultTarget() = runTest {
        assumeTrue("no node or AI-gile folder", board != null)
        val question = ask()
        val c = source().conversation(team())
        val asked = c.bubbles.last { it.card == question && it.text.contains("Red or blue?") }
        assertEquals(listOf("Red", "Blue"), asked.options)
        assertTrue(asked.waiting)
        assertTrue(asked.cardTitle!!.contains("Pick a colour"))
        assertEquals(question, c.defaultTarget)
    }

    @Test fun answerButtonReplyLandsOnTheQuestionAndReadsBack() = runTest {
        assumeTrue("no node or AI-gile folder", board != null)
        val question = ask()
        val src = source()
        assertEquals(question, src.send(team(), "Blue", question))
        val mine = src.conversation(team()).bubbles.last { it.mine }
        assertEquals("Blue", mine.text)
        assertEquals(question, mine.card)
    }

    @Test fun replyWithNoTargetStartsANewMessage() = runTest {
        assumeTrue("no node or AI-gile folder", board != null)
        val src = source()
        val id = src.send(team(), "A brand new thought", null)
        assertNotNull(id)
        assertTrue(id!!.startsWith("TST-"))
        val back = src.conversation(team()).bubbles.last { it.text.contains("A brand new thought") }
        assertTrue(back.mine)
        assertEquals(id, back.card)
    }
}
