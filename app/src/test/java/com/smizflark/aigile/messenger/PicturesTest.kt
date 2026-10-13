package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Attachment
import com.smizflark.aigile.messenger.data.BoardSource
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.FriendMsg
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Upload
import com.smizflark.aigile.messenger.data.WireMsg
import com.smizflark.aigile.messenger.data.WireThread
import com.smizflark.aigile.messenger.data.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.util.Base64

/** Pictures in chats (AII-27): how the board's files show, and what the phone sends. */
class PicturesTest {
    private val lead = ChatRef(Kind.LEAD, "ai-gile", "AI-gile", "Lead", "🗂️")
    private val friend = ChatRef(Kind.FRIEND, "c1", "Davy", "", "🙂")
    private val watch = ChatRef(Kind.WATCHMAN, "_watchman", "Lead Scrum Master", "", "🧭")

    /* ---------- showing ---------- */

    @Test fun leadPicturesPointAtTheProjectsFiles() {
        val c = Mapper.conversation(lead, WireThread("ai-gile", messages = listOf(
            WireMsg("AIG-1", "💬 Bug", "Rich", "Look", "2026-10-01T10:00:00Z", mine = true,
                attachments = listOf(Attachment("abc-my shot.png", "my shot.png", "image/png", 10))),
        )))
        val p = c.bubbles.single().pics.single()
        assertEquals("/api/projects/ai-gile/attachments/abc-my%20shot.png", p.path)
        assertTrue(p.image); assertFalse(p.local)
    }

    @Test fun picturesOnlyMessagesAreKept() {
        val c = Mapper.conversation(watch, WireThread("_watchman", messages = listOf(
            WireMsg("_night", null, "Rich", "", "2026-10-01T10:00:00Z", mine = true, attachments = listOf(Attachment("x1-a.jpg", "a.jpg"))),
            WireMsg("_night", null, "Rich", "", "2026-10-01T10:01:00Z", mine = true),
        )))
        assertEquals("/api/projects/_watchman/attachments/x1-a.jpg", c.bubbles.single().pics.single().path)
    }

    @Test fun friendFilesComeFromTheirDmFolderAndOnlyWhenVerified() {
        val c = Mapper.friend(friend, "Rich", listOf(
            FriendMsg("m1", "in", "", "2026-10-01T10:00:00Z", attachments = listOf(Attachment("m10-cat.webp", "cat.webp", "image/webp"))),
            FriendMsg("m2", "in", "x", "2026-10-01T10:01:00Z", verified = false, attachments = listOf(Attachment("m20-evil.png", "evil.png"))),
        ))
        assertEquals("/api/dms/contacts/c1/files/m10-cat.webp", c.bubbles[0].pics.single().path)
        assertTrue(c.bubbles[1].pics.isEmpty())
    }

    @Test fun otherFilesAreNotPictures() {
        val c = Mapper.conversation(lead, WireThread("ai-gile", messages = listOf(
            WireMsg("AIG-1", null, "Lead", "log", "2026-10-01T10:00:00Z", attachments = listOf(Attachment("a-run.log", "run.log", "text/plain"))),
        )))
        assertFalse(c.bubbles.single().pics.single().image)
        assertEquals("image/jpeg", Mapper.mimeOf("IMG_1.JPG")); assertEquals("application/octet-stream", Mapper.mimeOf("x"))
    }

    /* ---------- sending, against a pretend board that records each call ---------- */

    private val calls = mutableListOf<Pair<String, JsonObject>>()
    private lateinit var src: BoardSource
    private val png = Upload("shot.png", "image/png", byteArrayOf(1, 2, 3))

    @Before fun up() {
        // No network: every call is recorded and answered here, the way the board would.
        val fake = OkHttpClient.Builder().addInterceptor { chain ->
            val req = chain.request()
            val body = req.body?.let { b -> Buffer().also { b.writeTo(it) }.readUtf8() } ?: ""
            val path = req.url.encodedPath
            calls += path to (if (body.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(body).jsonObject)
            val out = when {
                path == "/api/meta" -> """{"owner":"Rich"}"""
                path.endsWith("/attachments") -> """{"id":"k${calls.size}-shot.png","name":"shot.png","type":"image/png","size":3}"""
                path.endsWith("/cards") -> """{"id":"AIG-9"}"""
                else -> "{}"
            }
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(out.toResponseBody("application/json".toMediaType())).build()
        }.build()
        src = BoardSource("http://board.test", "key", fake)
    }

    private fun posts() = calls.filter { it.first != "/api/meta" }

    @Test fun replyUploadsFirstThenNamesTheFiles() = runBlocking {
        assertEquals("AIG-1", src.send(lead, "", "AIG-1", listOf(png)))
        val (up, comment) = posts()
        assertEquals("/api/projects/ai-gile/attachments", up.first)
        assertEquals(Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3)), up.second["data"]!!.jsonPrimitive.content)
        assertEquals("/api/projects/ai-gile/cards/AIG-1/comments", comment.first)
        assertEquals("k1-shot.png", comment.second["attachments"]!!.jsonArray.single().jsonObject["id"]!!.jsonPrimitive.content)
    }

    @Test fun newMessageWithOnlyAPictureGetsATitle() = runBlocking {
        assertEquals("AIG-9", src.send(lead, "  ", null, listOf(png)))
        val card = posts().last()
        assertEquals("/api/projects/ai-gile/cards", card.first)
        assertEquals("💬 📷 Picture", card.second["title"]!!.jsonPrimitive.content)
        assertEquals(1, card.second["attachments"]!!.jsonArray.size)
    }

    @Test fun scrumMasterFilesGoToItsOwnFolder() = runBlocking {
        src.send(watch, "see", null, listOf(png, png))
        assertEquals(listOf("/api/projects/_watchman/attachments", "/api/projects/_watchman/attachments", "/api/chats/_watchman"), posts().map { it.first })
        assertEquals(2, posts().last().second["attachments"]!!.jsonArray.size)
    }

    @Test fun friendFilesTravelInsideTheMessage() = runBlocking {
        src.send(friend, "hi", null, listOf(png))
        val (path, body) = posts().single()
        assertEquals("/api/dms/contacts/c1/messages", path)
        val f = body["attachments"]!!.jsonArray.single().jsonObject
        assertEquals("shot.png", f["name"]!!.jsonPrimitive.content)
        assertEquals("AQID", f["data"]!!.jsonPrimitive.content)
    }

    @Test fun plainTextSendsNoAttachments() = runBlocking {
        src.send(lead, "hello", "AIG-1")
        assertFalse("attachments" in posts().single().second)
    }
}
