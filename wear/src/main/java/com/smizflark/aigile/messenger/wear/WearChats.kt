package com.smizflark.aigile.messenger.wear

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A chat as the phone sends it over the Data Layer at [WearChats.PATH] (AII-412). */
data class WearChat(val key: String, val name: String, val last: String, val time: String, val unread: Boolean)

object WearChats {
    const val PATH = "/chats"

    fun parse(json: String): List<WearChat> = runCatching {
        (Json.parseToJsonElement(json) as JsonArray).map {
            val o = it.jsonObject
            WearChat(o["key"]!!.jsonPrimitive.content, o["name"]!!.jsonPrimitive.content, o["last"]?.jsonPrimitive?.content ?: "",
                o["time"]?.jsonPrimitive?.content ?: "", o["unread"]?.jsonPrimitive?.boolean ?: false)
        }
    }.getOrDefault(emptyList())
}
