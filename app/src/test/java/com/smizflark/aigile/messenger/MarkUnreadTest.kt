package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.BoardSource
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.News
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mark as unread (AII-119). */
class MarkUnreadTest {
    private val t1 = "2026-10-13T09:00:00Z"
    private val t2 = "2026-10-13T10:00:00Z"
    private fun b(at: String, mine: Boolean = false) = Bubble(at, "x", "hi", at, mine)

    @Test fun theNewestMessageFromThemCountsAsNewAgain() {
        val ref = ChatRef(Kind.LEAD, "a", "A", "", "", last = t2)
        val saved = Conversation(ref, listOf(b(t1), b(t2, mine = true)), lastFromThem = t1)
        val mark = News.unreadMark(ref, saved)!!
        assertTrue(mark < t1)
        assertEquals(1, News.unread(ref, mark, saved))
        assertTrue(News.fresh(ref, mark))
    }

    @Test fun aFriendWithNoSavedChatGoesByTheBoard() {
        val ref = ChatRef(Kind.FRIEND, "f", "F", "", "", last = t2, lastIn = t1)
        assertEquals("2026-10-13T08:59:59.999Z", News.unreadMark(ref, null))
        assertEquals(1, News.unread(ref, News.unreadMark(ref, null), null))
    }

    @Test fun theLeadScrumMasterAndEmptyChatsCantBeUnread() {
        assertNull(News.unreadMark(ChatRef(Kind.WATCHMAN, "w", "W", "", "", last = t2), null))
        assertNull(News.unreadMark(ChatRef(Kind.LEAD, "a", "A", "", ""), null))
    }

    @Test fun theBoardIsToldToMoveTheMarkBack() = runBlocking {
        val sent = mutableListOf<String>()
        val src = BoardSource("http://board.test", "key", OkHttpClient.Builder().addInterceptor { chain ->
            val r = chain.request()
            sent += (r.body?.let { b -> Buffer().also { b.writeTo(it) }.readUtf8() } ?: "")
            Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""{"seen":"x"}""".toResponseBody("application/json".toMediaType())).build()
        }.build())
        src.markSeen("ai-gile", t1, back = true)
        src.markSeen("ai-gile", t2)
        assertEquals(listOf("""{"at":"$t1","back":true}""", """{"at":"$t2"}"""), sent)
    }
}
