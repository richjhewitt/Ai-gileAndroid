package com.smizflark.aigile.messenger.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
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

    fun queue(ref: ChatRef, text: String, target: String?, files: List<String> = emptyList()): Pending {
        val p = Pending(UUID.randomUUID().toString(), ref.key, text, target, Instant.now().toString(), files = files)
        store.saveOutbox(store.outbox() + p)
        return p
    }

    /** Sends one queued message. Returns the card it landed on; on failure it stays queued, marked failed. */
    suspend fun deliver(ref: ChatRef, p: Pending): Result<String?> = sending.withLock {
        if (store.outbox().none { it.id == p.id }) return Result.success(p.target)   // already sent
        val r = runCatching { source.send(ref, p.text, p.target, p.files.map { upload(it) }) }
        store.saveOutbox(
            if (r.isSuccess) store.outbox().filter { it.id != p.id }
            else store.outbox().map { if (it.id == p.id) it.copy(failed = true) else it }
        )
        if (r.isSuccess) p.files.forEach { File(it).delete() }
        r
    }

    fun discard(id: String) {
        store.outbox().firstOrNull { it.id == id }?.files?.forEach { File(it).delete() }
        store.saveOutbox(store.outbox().filter { it.id != id })
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

    fun pendingFor(chat: String) = store.outbox().filter { it.chat == chat }
}
