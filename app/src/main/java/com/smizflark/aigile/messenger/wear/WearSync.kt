package com.smizflark.aigile.messenger.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Cleared
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Store
import com.smizflark.aigile.messenger.ui.previewLabel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** AII-412/413: puts the top chats, their last lines and the lock flag on the Wear OS Data Layer for the watch app (the :wear module). */
object WearSync {
    const val PATH = "/chats"
    const val MAX = 20
    const val CHAT = "/chat/"
    const val LOCKED = "/locked"
    const val CHAT_CHATS = 5
    const val CHAT_LINES = 15
    const val CHAT_TEXT = 500

    /** The payload: newest [MAX] chats (this phone's own names, cleared / deleted lines hidden), JSON. Pure, unit tested. */
    fun payload(chats: List<ChatRef>, cleared: Map<String, String>): String =
        JsonArray(chats.map { Cleared.row(it, cleared[it.key]) }.sortedByDescending { it.last ?: "" }.take(MAX).map { c ->
            JsonObject(mapOf(
                "key" to JsonPrimitive(c.key),
                "name" to JsonPrimitive(c.name),
                "last" to JsonPrimitive(previewLabel(c)),
                "time" to JsonPrimitive(c.last ?: ""),
                "unread" to JsonPrimitive(c.unread > 0),
            ))
        }).toString()

    /** The Data Layer path of one chat's lines; "%" and "/" in the key are escaped (the watch does the same). */
    fun chatPath(key: String): String = CHAT + key.replace("%", "%25").replace("/", "%2F")

    /** The last [CHAT_LINES] lines of a chat as JSON: who, text (capped), time, mine. Cleared and deleted lines are left out. Pure, unit tested. */
    fun chatPayload(bubbles: List<Bubble>, clearedMark: String?): String =
        JsonArray(Cleared.visible(bubbles, clearedMark).filter { !it.deleted }.sortedBy { it.at }.takeLast(CHAT_LINES).map { b ->
            JsonObject(mapOf(
                "who" to JsonPrimitive(b.author),
                "text" to JsonPrimitive(b.text.take(CHAT_TEXT)),
                "time" to JsonPrimitive(b.at),
                "mine" to JsonPrimitive(b.mine),
            ))
        }).toString()

    private fun put(ctx: Context, path: String, data: ByteArray) {
        Wearable.getDataClient(ctx).putDataItem(PutDataRequest.create(path).setData(data).setUrgent()).addOnFailureListener { }
    }

    /** Called when the chats list refreshes. Never throws: no Play services / Wearable API = nothing happens.
     *  App lock on: only the "/locked" flag goes, and the chats sent before are taken off the watch. */
    fun update(ctx: Context, chats: List<ChatRef>) {
        runCatching {
            val store = Store(ctx)
            if (store.lock) {
                put(ctx, LOCKED, "1".toByteArray())
                val client = Wearable.getDataClient(ctx)
                client.deleteDataItems(android.net.Uri.parse("wear://*$PATH")).addOnFailureListener { }
                client.deleteDataItems(android.net.Uri.parse("wear://*/chat")).addOnFailureListener { }
                return
            }
            put(ctx, LOCKED, "0".toByteArray())
            val cleared = store.cleared()
            put(ctx, PATH, payload(chats, cleared).toByteArray())
            // The top chats' lines, from what this phone has kept; a chat opened on the phone refreshes its own (updateChat).
            chats.map { Cleared.row(it, cleared[it.key]) }.sortedByDescending { it.last ?: "" }.take(CHAT_CHATS).forEach { c ->
                store.cachedConversation(c.key)?.let { put(ctx, chatPath(c.key), chatPayload(it.bubbles, cleared[c.key]).toByteArray()) }
            }
        }
    }

    /** A chat's conversation was loaded: send its lines for the watch. */
    fun updateChat(ctx: Context, c: Conversation) {
        runCatching {
            val store = Store(ctx)
            if (store.lock) return
            put(ctx, chatPath(c.ref.key), chatPayload(c.bubbles, store.cleared()[c.ref.key]).toByteArray())
        }
    }
}
