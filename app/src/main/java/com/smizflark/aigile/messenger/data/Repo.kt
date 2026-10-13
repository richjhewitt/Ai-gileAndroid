package com.smizflark.aigile.messenger.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID

/** The one place the screens and the background check get their chats from. */
class Repo(context: Context) {
    val store = Store(context)
    private val demo by lazy { DemoSource() }
    private var board: BoardSource? = null
    private val sending = Mutex()

    val source: ChatSource
        get() = if (store.demo) demo else board?.takeIf { it.base == store.url } ?: BoardSource(store.url, store.key).also { board = it }

    /** The board connection (for pictures), or null in the demo. */
    val boardSource: BoardSource? get() = if (store.demo) null else source as? BoardSource

    /** Checks an address and key by asking the board who owns it; saves them when it answers. */
    suspend fun connect(url: String, key: String): String {
        val b = BoardSource(Mapper.normaliseUrl(url), key.trim())
        val owner = b.owner()
        b.chats()   // a board that answers /api/meta but refuses chats would only fail later
        store.demo = false; store.url = b.base; store.key = key.trim(); store.owner = owner
        board = b
        return owner
    }

    /** Settings: a new address and/or key without signing out. A blank key keeps the current one.
     *  Moving to another board (or out of the demo) clears what was cached for the old one. */
    suspend fun change(url: String, key: String): String {
        val wasDemo = store.demo; val old = store.url
        val owner = connect(url, key.ifBlank { store.key })
        if (Mapper.boardChanged(wasDemo, old, store.url)) store.clearCache()
        return owner
    }

    fun startDemo() { store.demo = true; store.owner = "Rich" }

    fun signOut() { board = null; store.signOut() }

    suspend fun chats(): List<ChatRef> {
        val l = source.chats()
        store.cacheChats(l); store.lastSync = System.currentTimeMillis()
        return l
    }

    suspend fun conversation(ref: ChatRef): Conversation {
        val c = source.conversation(ref)
        store.cacheConversation(c)
        return c
    }

    /* ---------- sending through the outbox: nothing typed is lost ---------- */

    fun queue(ref: ChatRef, text: String, target: String?): Pending {
        val p = Pending(UUID.randomUUID().toString(), ref.key, text, target, Instant.now().toString())
        store.saveOutbox(store.outbox() + p)
        return p
    }

    /** Sends one queued message. Returns the card it landed on; on failure it stays queued, marked failed. */
    suspend fun deliver(ref: ChatRef, p: Pending): Result<String?> = sending.withLock {
        if (store.outbox().none { it.id == p.id }) return Result.success(p.target)   // already sent
        val r = runCatching { source.send(ref, p.text, p.target) }
        store.saveOutbox(
            if (r.isSuccess) store.outbox().filter { it.id != p.id }
            else store.outbox().map { if (it.id == p.id) it.copy(failed = true) else it }
        )
        r
    }

    fun discard(id: String) = store.saveOutbox(store.outbox().filter { it.id != id })

    fun pendingFor(chat: String) = store.outbox().filter { it.chat == chat }
}
