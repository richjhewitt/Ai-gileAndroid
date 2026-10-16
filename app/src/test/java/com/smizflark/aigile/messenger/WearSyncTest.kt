package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.wear.WearSync
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearSyncTest {
    private fun chat(id: String, name: String, last: String, preview: String? = "hi", unread: Int = 0) =
        ChatRef(Kind.LEAD, id, name, "", "", last = last, preview = preview, unread = unread)

    private fun parse(s: String) = Json.parseToJsonElement(s).jsonArray.map { it.jsonObject }

    @Test fun topTwentyNewestFirst() {
        val chats = (1..30).map { chat("c$it", "Chat $it", "2026-01-%02dT10:00:00Z".format(it)) }
        val out = parse(WearSync.payload(chats, emptyMap()))
        assertEquals(20, out.size)
        assertEquals("lead:c30", out.first()["key"]!!.jsonPrimitive.content)
    }

    @Test fun clearedLineIsHiddenAndLocalNameKept() {
        val c = chat("a", "My own name", "2026-01-01T10:00:00Z", preview = "secret", unread = 2)
        val out = parse(WearSync.payload(listOf(c), mapOf(c.key to "2026-01-02T00:00:00Z"))).single()
        assertEquals("My own name", out["name"]!!.jsonPrimitive.content)
        assertTrue(!out["last"]!!.jsonPrimitive.content.contains("secret"))
        assertEquals("true", out["unread"]!!.jsonPrimitive.content)
    }

    @Test fun lineAndTimeSent() {
        val out = parse(WearSync.payload(listOf(chat("a", "A", "2026-01-01T10:00:00Z", preview = "hello")), emptyMap())).single()
        assertEquals("hello", out["last"]!!.jsonPrimitive.content)
        assertEquals("2026-01-01T10:00:00Z", out["time"]!!.jsonPrimitive.content)
        assertEquals("false", out["unread"]!!.jsonPrimitive.content)
    }
}
