package com.smizflark.aigile.messenger.screens

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.wear.WearReply
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AII-414: a reply from the watch goes into the outbox like the notification's quick reply. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WearReplyTest {
    private fun repo(): Repo = Repo(ApplicationProvider.getApplicationContext<Application>()).also { it.startDemo() }

    @Test fun replyLandsInTheOutboxOfThatChat() {
        val repo = repo()
        val ref = runBlocking { repo.chats() }.first { it.id == "ai-gile" }
        val req = WearReply.parse("""{"key":"${ref.key}","text":"  On my way ","id":"x1"}""".toByteArray())!!
        var queued = false
        assertNull(WearReply.accept(repo, false, req) { _, _ -> queued = true })
        assertTrue(queued)
        val p = repo.pendingFor(ref.key).single()
        assertEquals("On my way", p.text)
    }

    @Test fun lockedPhoneRefusesAndQueuesNothing() {
        val repo = repo()
        val ref = runBlocking { repo.chats() }.first { it.id == "ai-gile" }
        val req = WearReply.Request(ref.key, "hi", "x2")
        assertEquals(WearReply.ERR_LOCKED, WearReply.accept(repo, true, req) { _, _ -> })
        assertEquals(0, repo.pendingFor(ref.key).size)
    }

    @Test fun unknownChatAndEmptyTextAreRefused() {
        val repo = repo()
        runBlocking { repo.chats() }
        assertEquals(WearReply.ERR_NO_CHAT, WearReply.accept(repo, false, WearReply.Request("lead:nope", "hi", "a")) { _, _ -> })
        val ref = runBlocking { repo.chats() }.first()
        assertEquals(WearReply.ERR_EMPTY, WearReply.accept(repo, false, WearReply.Request(ref.key, "  ", "b")) { _, _ -> })
    }

    @Test fun ackAndRepliesPayload() {
        val ok = Json.parseToJsonElement(String(WearReply.ack("i"))).jsonObject
        assertEquals("i", ok["id"]!!.jsonPrimitive.content); assertEquals("true", ok["ok"]!!.jsonPrimitive.content)
        val bad = Json.parseToJsonElement(String(WearReply.ack("i", WearReply.ERR_LOCKED))).jsonObject
        assertEquals("locked", bad["error"]!!.jsonPrimitive.content)
        assertEquals("""["A","B"]""", WearReply.repliesPayload("A\n\nB\nA", listOf("D")))
        assertEquals("""["D"]""", WearReply.repliesPayload("", listOf("D")))
        assertNull(WearReply.parse("junk".toByteArray()))
    }
}
