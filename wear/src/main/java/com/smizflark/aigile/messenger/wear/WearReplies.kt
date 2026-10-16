package com.smizflark.aigile.messenger.wear

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Where a reply sent from the watch stands (AII-414). */
enum class ReplyStatus { SENDING, SENT, SAVED, LOCKED, NO_CHAT, FAILED }

/** A reply sent from the watch, shown under the chat's lines. */
data class Outgoing(val id: String, val key: String, val text: String, val status: ReplyStatus = ReplyStatus.SENDING)

/** The phone's answer at "/reply-ack". */
data class Ack(val id: String, val ok: Boolean, val error: String?)

object WearReplies {
    const val REPLIES = "/replies"
    const val REPLY = "/reply"
    const val ACK = "/reply-ack"
    const val ACK_WAIT_MS = 10_000L
    val EMOJI = listOf("👍", "✅", "👀", "🙏")

    fun parseReplies(json: String): List<String> = runCatching {
        (Json.parseToJsonElement(json) as JsonArray).map { it.jsonPrimitive.content }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    /** The message the phone listens for: {key, text, id}. */
    fun request(key: String, text: String, id: String): ByteArray =
        JsonObject(mapOf("key" to JsonPrimitive(key), "text" to JsonPrimitive(text), "id" to JsonPrimitive(id))).toString().toByteArray()

    fun parseAck(data: ByteArray): Ack? = runCatching {
        val o = Json.parseToJsonElement(String(data)).jsonObject
        Ack(o["id"]!!.jsonPrimitive.content, o["ok"]!!.jsonPrimitive.boolean, o["error"]?.jsonPrimitive?.content)
    }.getOrNull()

    /** The status an ack gives its line; unknown errors count as failed. */
    fun statusFor(a: Ack): ReplyStatus = when {
        a.ok -> ReplyStatus.SENT
        a.error == "locked" -> ReplyStatus.LOCKED
        a.error == "no-chat" -> ReplyStatus.NO_CHAT
        else -> ReplyStatus.FAILED
    }

    /** An ack for the line's id settles it. */
    fun settle(list: List<Outgoing>, a: Ack): List<Outgoing> = list.map { if (it.id == a.id) it.copy(status = statusFor(a)) else it }

    /** No ack within [ACK_WAIT_MS]: a line still sending becomes "saved". */
    fun timeout(list: List<Outgoing>, id: String): List<Outgoing> =
        list.map { if (it.id == id && it.status == ReplyStatus.SENDING) it.copy(status = ReplyStatus.SAVED) else it }
}
