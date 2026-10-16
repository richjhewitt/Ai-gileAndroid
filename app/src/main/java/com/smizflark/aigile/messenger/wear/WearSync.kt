package com.smizflark.aigile.messenger.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Cleared
import com.smizflark.aigile.messenger.data.Store
import com.smizflark.aigile.messenger.ui.previewLabel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** AII-412: puts the top chats on the Wear OS Data Layer at /chats for the watch app (the :wear module). */
object WearSync {
    const val PATH = "/chats"
    const val MAX = 20

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

    /** Called when the chats list refreshes. Never throws: no Play services / Wearable API = nothing happens. */
    fun update(ctx: Context, chats: List<ChatRef>) {
        // TODO(AII-412 part 2): show a lock on the watch instead of sending nothing when App lock is on.
        runCatching {
            val store = Store(ctx)
            if (store.lock) return
            val req = PutDataRequest.create(PATH).setData(payload(chats, store.cleared()).toByteArray()).setUrgent()
            Wearable.getDataClient(ctx).putDataItem(req).addOnFailureListener { }
        }
    }
}
