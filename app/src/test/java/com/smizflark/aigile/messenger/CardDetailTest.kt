package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.BoardError
import com.smizflark.aigile.messenger.data.BoardSource
import com.smizflark.aigile.messenger.ui.statusLabel
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/** The card view shows the card itself (AII-45). */
class CardDetailTest {
    private val asked = mutableListOf<String>()

    /** A pretend board answering every call with this code and body. */
    private fun board(code: Int, body: String) = BoardSource("http://board.test", "key", OkHttpClient.Builder().addInterceptor { chain ->
        asked += chain.request().url.encodedPath
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("x")
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }.build())

    @Test fun readsOneCard() = runBlocking {
        val c = board(200, """{"id":"AIG-12","title":"💬 Fix the menu","status":"verify","type":"bug","description":"- broken","testSteps":"- Open it","assignee":"Forge"}""")
            .card("ai-gile", "AIG-12")!!
        assertEquals(listOf("/api/projects/ai-gile/cards/AIG-12"), asked)
        assertEquals("- broken", c.description); assertEquals("- Open it", c.testSteps); assertEquals("verify", c.status)
    }

    @Test fun verdictGoesToTheBoardsVerdictRoute() = runBlocking {
        board(200, "{}").verdict("ai-gile", "AIG-12", accept = false, text = "Broken")
        assertEquals(listOf("/api/projects/ai-gile/cards/AIG-12/verdict"), asked)
    }

    @Test fun anOlderBoardWithoutTheRouteGivesNothing() = runBlocking {
        assertNull(board(404, """{"error":"not found"}""").card("ai-gile", "AIG-12"))
    }

    @Test fun otherErrorsStillSaySo() = runBlocking {
        try { board(401, "{}").card("ai-gile", "AIG-12"); fail("expected a BoardError") }
        catch (e: BoardError) { assertEquals(401, e.code) }
    }

    @Test fun columnsUseTheBoardsNames() {
        assertEquals("For you to test", statusLabel("verify"))
        assertEquals("In progress", statusLabel("doing"))
        assertEquals("Odd", statusLabel("odd"))
    }
}
