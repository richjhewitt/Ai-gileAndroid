package com.smizflark.aigile.messenger.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import com.smizflark.aigile.messenger.widget.LatestWidget
import com.smizflark.aigile.messenger.ui.Shortcuts
import com.smizflark.aigile.messenger.work.SendWorker
import com.smizflark.aigile.messenger.work.isOffline
import com.smizflark.aigile.messenger.work.waitingToSend
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID

/** The one place the screens and the background check get their chats from. */
class Repo(context: Context) {
    private val app = context.applicationContext
    private val outboxDir by lazy { File(app.filesDir, "outbox").apply { mkdirs() } }
    val store = Store(context)
    private val demo by lazy { DemoSource() }
    private var board: BoardSource? = null
    private val sending = Mutex()

    val source: ChatSource
        get() = if (store.demo) demo else board?.takeIf { it.base == store.url } ?: BoardSource(store.url, store.key, deviceId = store.device).also { board = it }

    /** The board connection (for pictures), or null in the demo. */
    val boardSource: BoardSource? get() = if (store.demo) null else source as? BoardSource

    /** Checks an address and key ("id:key" from a sealed pairing) by asking the board who owns it; saves them when it answers. */
    suspend fun connect(url: String, credential: String): String {
        val (device, key) = Mapper.splitCredential(credential)
        val b = BoardSource(Mapper.normaliseUrl(url), key, deviceId = device)
        val owner = b.owner()
        b.chats()   // a board that answers /api/meta but refuses chats would only fail later
        store.demo = false; store.url = b.base; store.key = key; store.device = device; store.owner = owner
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

    fun signOut() { board = null; store.signOut(); LatestWidget.refresh(app); Shortcuts.clear(app) }

    suspend fun chats(): List<ChatRef> {
        val l = source.chats().map { Mapper.mineFromCache(it, store.cachedConversation(it.key)) }   // "You:" on the chats list (AII-106)
        store.cacheChats(l); store.lastSync = System.currentTimeMillis()
        // In step with the board (AII-25): the Lead Scrum Master read on the PC counts as read here too.
        l.filter { it.kind == Kind.WATCHMAN && it.unread == 0 }.forEach { store.markSeen(it.key, it.last) }
        LatestWidget.refresh(app)
        Shortcuts.update(app, l)   // hold the app icon: your newest chats (AII-53)
        // ...and so are lead chats (AII-44): the board keeps one read mark per chat, set by the PC or this phone.
        l.filter { it.kind == Kind.LEAD }.forEach { store.markSeen(it.key, it.seen) }
        // Closed chats (AII-131): the board's list, so the PC and this phone agree; offline, the last one kept here.
        if (!store.demo) runCatching { source.closed() }.getOrNull()?.let { store.setClosed(Closed.fromBoard(it, l)) }
        return l
    }

    /** Close or reopen a chat (AII-131): here at once, and on the board's Messages list too. */
    suspend fun setClosed(ref: ChatRef, closed: Boolean) {
        store.setClosed(if (closed) store.closed() + (ref.key to Now.instant().toString()) else store.closed() - ref.key)
        if (!store.demo) runCatching { source.setClosed(Closed.boardKey(ref), closed) }   // offline: closed here until the next check brings the board's list
    }

    /** This chat as the chats list has it now: is the team at work, and on which card (AII-112/113)? Null when the board can't be reached. */
    suspend fun fresh(ref: ChatRef): ChatRef? = runCatching { source.chats().firstOrNull { it.key == ref.key } }.getOrNull()

    /** Mark as read (AII-25). Opening the Lead Scrum Master's chat is what reads it on the board, so that one is fetched. */
    suspend fun markRead(ref: ChatRef) {
        val saved = if (ref.kind == Kind.WATCHMAN) runCatching { conversation(ref) }.getOrNull() else store.cachedConversation(ref.key)
        seen(ref, News.readUpTo(ref, saved))
    }

    /** Read up to [at] here, and on the board for a lead chat (AII-44). Only a mark that moved on is sent. */
    suspend fun seen(ref: ChatRef, at: String?) {
        com.smizflark.aigile.messenger.work.ChatNotifier.clear(app, ref.key)   // opened or read: its notification goes (AII-65)
        if (at == null) return
        val moved = at > (store.seen()[ref.key] ?: "")
        store.markSeen(ref.key, at)
        if (moved && ref.kind == Kind.LEAD && !store.demo) runCatching { source.markSeen(ref.id, at) }   // offline or an older board: kept here
    }

    /** Mark as unread (AII-119): the newest message from them counts as new again, here and on the board for a lead chat. */
    suspend fun markUnread(ref: ChatRef) {
        val at = News.unreadMark(ref, store.cachedConversation(ref.key)) ?: return
        store.unsee(ref.key, at)
        if (ref.kind == Kind.LEAD && !store.demo) runCatching { source.markSeen(ref.id, at, back = true) }   // offline or an older board: kept here until the next check
    }

    /** Undo Mark all as read (AII-158): the read marks go back to [before], here and on the board for lead chats. */
    suspend fun restoreSeen(refs: List<ChatRef>, before: Map<String, String>) {
        store.restoreSeen(refs.map { it.key }, before)
        if (!store.demo) refs.filter { it.kind == Kind.LEAD }.forEach { r ->
            before[r.key]?.let { runCatching { source.markSeen(r.id, it, back = true) } }   // offline or an older board: kept here
        }
    }

    suspend fun conversation(ref: ChatRef): Conversation {
        val c = source.conversation(ref)
        store.cacheConversation(c)
        syncStars(c)
        return c
    }

    /* ---------- stars kept in step with the board (AII-141): lead chats only ---------- */

    /** First time: the phone's stars go to the board. After that the board's list is the one. */
    private suspend fun syncStars(c: Conversation) {
        val board = c.boardStars ?: return
        val ref = c.ref
        if (ref.kind != Kind.LEAD || store.demo) return
        if (ref.id !in store.starsSynced) {
            val mine = Stars.inChat(store.stars(), ref.key).map { Stars.boardKey(it).split('|', limit = 2).let { (f, at) -> f to at } }
            if (runCatching { source.replaceStars(ref.id, mine) }.isSuccess) store.starsSynced = store.starsSynced + ref.id
        } else store.setStars(Stars.fromBoard(store.stars(), ref.key, board, c.bubbles))
    }

    /** Star or unstar a message: here at once, and on the board for a lead chat. Offline, the phone's list wins at the next sync. */
    suspend fun setStar(ref: ChatRef?, s: Star, on: Boolean) {
        val all = store.stars().filterNot { it.chat == s.chat && it.key == s.key }
        store.setStars(if (on) all + s else all)
        if (ref?.kind != Kind.LEAD || store.demo) return
        val (fold, at) = Stars.boardKey(s).split('|', limit = 2)
        if (runCatching { source.setStar(ref.id, fold, at, on) }.isFailure) store.starsSynced = store.starsSynced - ref.id
    }

    /** Every chat's conversation as last fetched, for search (AII-30). */
    fun cachedConversations(chats: List<ChatRef>) = chats.mapNotNull { store.cachedConversation(it.key) }
    /** One card for the card view (AII-45); null if it can't be fetched (the view still shows its messages). */
    suspend fun card(ref: ChatRef, id: String): CardDetail? = runCatching { source.card(ref.id, id) }.getOrNull()

    /* ---------- sending through the outbox: nothing typed is lost ---------- */

    fun queue(ref: ChatRef, text: String, target: String?, files: List<String> = emptyList(), sendAt: Instant? = null): Pending {
        val p = Pending(UUID.randomUUID().toString(), ref.key, text, target, Instant.now().toString(), files = files, sendAt = sendAt?.toString())
        store.saveOutbox(store.outbox() + p)
        return p
    }

    /** Send later (AII-146): kept in the outbox until [at], then sent by SendWorker (or by the open chat, in the demo). */
    fun later(ref: ChatRef, text: String, target: String?, files: List<String>, at: Instant): Pending {
        val p = queue(ref, text, target, files, at)
        if (!store.demo) SendWorker.scheduleAt(app, p.id, at)
        return p
    }

    /** Send now, on a Send later message (AII-146). */
    suspend fun sendNow(ref: ChatRef, p: Pending): Result<String?> {
        store.saveOutbox(store.outbox().map { if (it.id == p.id) it.copy(sendAt = null) else it })
        return deliver(ref, p.copy(sendAt = null))
    }

    /** Sends one queued message. Returns the card it landed on; on failure it stays queued, marked failed. */
    suspend fun deliver(ref: ChatRef, p: Pending): Result<String?> = sending.withLock {
        if (store.outbox().none { it.id == p.id }) return Result.success(p.target)   // already sent
        val r = runCatching { source.send(ref, p.text, p.target, p.files.map { upload(it) }) }
        val offline = r.exceptionOrNull()?.let(::isOffline) == true
        store.saveOutbox(
            if (r.isSuccess) Outbox.remove(store.outbox(), p.id)
            else Outbox.fail(store.outbox(), p.id, offline)
        )
        if (r.isSuccess) p.files.forEach { File(it).delete() }
        if (offline && !store.demo) SendWorker.schedule(app)
        r
    }

    /**
     * Sends everything still waiting (AII-55). Returns how many are still stuck for want of a connection.
     * giveUp: the last try; anything still stuck then shows Not sent, with Tap to retry.
     */
    suspend fun sendWaiting(giveUp: Boolean = false): Int {
        val chats = store.cachedChats().orEmpty().associateBy { it.key }
        for (p in waitingToSend(store.outbox())) {
            val ref = chats[p.chat] ?: continue   // a chat no longer listed: left for Tap to retry
            deliver(ref, p)
        }
        val stuck = store.outbox().filter { it.offline }
        if (giveUp) store.saveOutbox(store.outbox().map { it.copy(offline = false) })
        return stuck.size
    }

    fun discard(id: String) {
        store.outbox().firstOrNull { it.id == id }?.files?.forEach { File(it).delete() }
        store.saveOutbox(Outbox.remove(store.outbox(), id))
    }

    /** Edit a Send later message (AII-150): off the outbox, but its pictures are kept for the box. */
    fun takeBack(id: String) = store.saveOutbox(Outbox.remove(store.outbox(), id))

    /** Tap Retry on a failed message (AII-62): marks it fresh, then sends it again. */
    suspend fun retry(ref: ChatRef, p: Pending): Result<String?> {
        store.saveOutbox(Outbox.retry(store.outbox(), p.id))
        return deliver(ref, p.copy(failed = false, offline = false))
    }

    /* ---------- pictures (AII-27) ---------- */

    /**
     * Copies a picked picture into the app, so it is still there if sending has to wait. Big photos are
     * shrunk to 2048 px JPEG (the board takes up to 10 MB a file, friend DMs 8 MB a message). Null if unreadable.
     */
    fun keepPicture(uri: Uri): String? = runCatching {
        val r = app.contentResolver
        val name = r.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "picture.jpg"
        val out = File(outboxDir, UUID.randomUUID().toString().take(8) + "-" + name.replace(Regex("[^\\w.\\- ]+"), "_"))
        r.openInputStream(uri)!!.use { i -> out.outputStream().use { i.copyTo(it) } }
        if (out.length() <= BIG) return@runCatching out.path
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeFile(out.path, it) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val bmp = BitmapFactory.decodeFile(out.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@runCatching out.path
        val scale = MAX_SIDE.toFloat() / maxOf(bmp.width, bmp.height)
        val fit = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        val jpg = File(out.path.substringBeforeLast('.') + ".jpg")
        jpg.outputStream().use { fit.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (jpg.path != out.path) out.delete()
        jpg.path
    }.getOrNull()

    private fun upload(path: String): Upload {
        val f = File(path)
        return Upload(f.name.substringAfter('-'), Mapper.mimeOf(f.name), f.readBytes())
    }

    private companion object {
        const val BIG = 2L * 1024 * 1024
        const val MAX_SIDE = 2048
    }

    /** AII-38: a bug report or crash log to the app's team, as a 💬 message. Null in the demo: nothing leaves the phone. */
    suspend fun reportBug(text: String): String? = if (store.demo) null else source.newMessage(Crash.TEAM, text)

    fun pendingFor(chat: String) = store.outbox().filter { it.chat == chat }
}
