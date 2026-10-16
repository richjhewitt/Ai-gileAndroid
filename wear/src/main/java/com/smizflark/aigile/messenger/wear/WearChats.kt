package com.smizflark.aigile.messenger.wear

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A chat as the phone sends it over the Data Layer at [WearChats.PATH] (AII-412). */
data class WearChat(val key: String, val name: String, val last: String, val time: String, val unread: Boolean)

/** One line of a chat, sent at "/chat/<key>" (AII-413). */
data class WearLine(val who: String, val text: String, val time: String, val mine: Boolean)

object WearChats {
    const val PATH = "/chats"
    const val CHAT = "/chat/"
    const val LOCKED = "/locked"

    fun parse(json: String): List<WearChat> = runCatching {
        (Json.parseToJsonElement(json) as JsonArray).map {
            val o = it.jsonObject
            WearChat(o["key"]!!.jsonPrimitive.content, o["name"]!!.jsonPrimitive.content, o["last"]?.jsonPrimitive?.content ?: "",
                o["time"]?.jsonPrimitive?.content ?: "", o["unread"]?.jsonPrimitive?.boolean ?: false)
        }
    }.getOrDefault(emptyList())

    fun parseLines(json: String): List<WearLine> = runCatching {
        (Json.parseToJsonElement(json) as JsonArray).map {
            val o = it.jsonObject
            WearLine(o["who"]?.jsonPrimitive?.content ?: "", o["text"]?.jsonPrimitive?.content ?: "",
                o["time"]?.jsonPrimitive?.content ?: "", o["mine"]?.jsonPrimitive?.boolean ?: false)
        }
    }.getOrDefault(emptyList())

    /** The phone's flag: "1" = App lock is on. */
    fun parseLocked(data: ByteArray?): Boolean = data?.let { String(it).trim() == "1" } ?: false

    /** Same escaping as the phone's chat path. */
    fun path(key: String): String = CHAT + key.replace("%", "%25").replace("/", "%2F")

    /** Newest first, as the list shows them. */
    fun newestFirst(chats: List<WearChat>): List<WearChat> = chats.sortedByDescending { it.time }

    /** "14:05" for today, "12 Oct" for earlier days; empty when the time can't be read. */
    fun clock(iso: String, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
        val at = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
        val day = at.atZone(zone).toLocalDate()
        return if (day == LocalDate.ofInstant(now, zone)) DateTimeFormatter.ofPattern("HH:mm").format(at.atZone(zone))
        else DateTimeFormatter.ofPattern("d MMM").format(at.atZone(zone))
    }

    /** Three made-up chats for the debug-only demo on an emulator without a phone. */
    fun demoChats(): List<WearChat> = listOf(
        WearChat("demo:1", "Sam", "See you at 6!", "2026-10-16T09:41:00Z", true),
        WearChat("demo:2", "Lead Scrum Master", "Card AII-7 is ready for review", "2026-10-16T08:10:00Z", false),
        WearChat("demo:3", "Family", "You: On my way", "2026-10-15T19:30:00Z", false),
    )

    fun demoLines(key: String): List<WearLine>? = when (key) {
        "demo:1" -> listOf(
            WearLine("Sam", "Are we still on for tonight?", "2026-10-16T09:38:00Z", false),
            WearLine("Rich", "Yes, what time?", "2026-10-16T09:40:00Z", true),
            WearLine("Sam", "See you at 6!", "2026-10-16T09:41:00Z", false))
        "demo:2" -> listOf(WearLine("Lead Scrum Master", "Card AII-7 is ready for review", "2026-10-16T08:10:00Z", false))
        else -> null
    }
}
