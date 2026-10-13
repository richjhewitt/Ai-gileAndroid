package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.BoardSource
import com.smizflark.aigile.messenger.data.ChatSummary
import com.smizflark.aigile.messenger.data.Mapper
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Lead chat read marks shared with the board (AII-44). */
class ReadMarksTest {
    @Test fun theBoardsMarkComesWithEachLeadChat() {
        val l = Mapper.chats(listOf(
            ChatSummary("ai-gile", "AI-gile", last = "2026-10-02T00:00:00Z", seen = "2026-10-01T00:00:00Z"),
            ChatSummary("tunio", "Tunio", seen = ""),
        ), emptyList())
        assertEquals("2026-10-01T00:00:00Z", l[0].seen)
        assertNull("an empty mark is no mark", l[1].seen)
    }

    @Test fun readingTellsTheBoard() = runBlocking {
        val sent = mutableListOf<String>()
        val src = BoardSource("http://board.test", "key", OkHttpClient.Builder().addInterceptor { chain ->
            val r = chain.request()
            sent += r.method + " " + r.url.encodedPath + " " + (r.body?.let { b -> Buffer().also { b.writeTo(it) }.readUtf8() } ?: "")
            Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""{"seen":"x"}""".toResponseBody("application/json".toMediaType())).build()
        }.build())
        src.markSeen("ai-gile", "2026-10-02T00:00:00Z")
        assertEquals(listOf("""POST /api/chats/ai-gile/seen {"at":"2026-10-02T00:00:00Z"}"""), sent)
    }
}
