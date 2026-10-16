package com.smizflark.aigile.messenger.wear

import android.content.Context
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.smizflark.aigile.messenger.App
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.QuickReply
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.Replies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** AII-414: a reply typed or spoken on the watch. It goes into the outbox exactly like the notification's quick reply (retries offline, shows in the chat). */
object WearReply {
    const val REPLIES = "/replies"
    const val REPLY = "/reply"
    const val ACK = "/reply-ack"
    const val ERR_LOCKED = "locked"
    const val ERR_NO_CHAT = "no-chat"
    const val ERR_EMPTY = "empty"

    class Request(val key: String, val text: String, val id: String)

    /** The watch's message, or null when it can't be read. */
    fun parse(data: ByteArray): Request? = runCatching {
        val o = Json.parseToJsonElement(String(data)).jsonObject
        Request(o["key"]!!.jsonPrimitive.content, o["text"]!!.jsonPrimitive.content, o["id"]!!.jsonPrimitive.content)
    }.getOrNull()

    /** {"id", "ok"} or {"id", "ok": false, "error"}. */
    fun ack(id: String, error: String? = null): ByteArray = JsonObject(buildMap {
        put("id", JsonPrimitive(id))
        put("ok", JsonPrimitive(error == null))
        if (error != null) put("error", JsonPrimitive(error))
    }).toString().toByteArray()

    /** The quick replies for the watch: yours from Settings, else the app's starting ones (what the alerts use). */
    fun repliesPayload(own: String, defaults: List<String>): String =
        JsonArray((Replies.parse(own).ifEmpty { defaults }).map { JsonPrimitive(it) }).toString()

    /** Queues the reply in the outbox like ReplyReceiver does. Returns the error to ack, or null when it is queued ([queued] gets the Pending to deliver). */
    fun accept(repo: Repo, locked: Boolean, req: Request, queued: (com.smizflark.aigile.messenger.data.ChatRef, Pending) -> Unit): String? {
        if (locked) return ERR_LOCKED
        val text = QuickReply.clean(req.text) ?: return ERR_EMPTY
        val ref = repo.store.cachedChats()?.firstOrNull { it.key == req.key } ?: return ERR_NO_CHAT
        val p = repo.queue(ref, text, QuickReply.target(repo.store.cachedConversation(req.key)))
        queued(ref, p)
        return null
    }

    fun sendReplies(ctx: Context, store: com.smizflark.aigile.messenger.data.Store) {
        runCatching {
            if (store.lock) return
            val defaults = ctx.resources.getStringArray(R.array.quick_replies_list).toList()
            Wearable.getDataClient(ctx).putDataItem(
                com.google.android.gms.wearable.PutDataRequest.create(REPLIES).setData(repliesPayload(store.quickReplyText, defaults).toByteArray()).setUrgent()
            ).addOnFailureListener { }
        }
    }
}

/** Receives "/reply" from the watch, answers "/reply-ack". */
class WearReplyService : WearableListenerService() {
    override fun onMessageReceived(e: MessageEvent) {
        if (e.path != WearReply.REPLY) return
        val req = WearReply.parse(e.data) ?: return
        val app = applicationContext as App
        val node = e.sourceNodeId
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            var job: Pair<com.smizflark.aigile.messenger.data.ChatRef, Pending>? = null
            val err = WearReply.accept(app.repo, app.repo.store.lock, req) { r, p -> job = r to p }
            runCatching { Wearable.getMessageClient(app).sendMessage(node, WearReply.ACK, WearReply.ack(req.id, err)) }
            job?.let { (r, p) -> runCatching { app.repo.deliver(r, p) } }
        }
    }
}
