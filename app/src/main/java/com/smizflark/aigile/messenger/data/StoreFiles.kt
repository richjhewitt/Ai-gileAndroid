package com.smizflark.aigile.messenger.data

import android.content.Context
import kotlinx.serialization.encodeToString
import java.io.File

/** The saved files of [Store] (drafts, chats, seen marks, outbox, ...), split out to keep Store small. Store extends it, so callers see one class. */
open class StoreFiles(context: Context) {
    private val dir = File(context.filesDir, "cache").apply { mkdirs() }

    /** Saved chats, seen marks and the outbox: they belong to one board. Settings stay. */
    fun clearCache() { dir.listFiles()?.forEach { it.delete() } }
    /* ---------- drafts: half-typed replies, per chat ---------- */

    fun drafts(): Map<String, String> = read<Map<String, String>>("drafts.json") ?: emptyMap()
    fun draft(chat: String): String = drafts()[chat] ?: ""
    fun saveDraft(chat: String, text: String) {
        val all = read<Map<String, String>>("drafts.json") ?: emptyMap()
        val next = Drafts.put(all, chat, text)
        if (next != all) write("drafts.json", next)
    }

    /* ---------- closed chats: off the list until a newer message (AII-131), per board ---------- */

    fun closed(): Map<String, String> = read("closed.json") ?: emptyMap()
    fun setClosed(all: Map<String, String>) = write("closed.json", all)

    /* ---------- cleared chats (AII-363): chat key -> hide messages up to this time, this phone only ---------- */

    fun cleared(): Map<String, String> = read("cleared.json") ?: emptyMap()
    fun setCleared(all: Map<String, String>) = write("cleared.json", all)

    /* ---------- recent searches (AII-155), per board ---------- */

    fun recentSearches(): List<String> = read("recent_searches.json") ?: emptyList()
    fun setRecentSearches(all: List<String>) = write("recent_searches.json", all)

    /* ---------- starred messages (AII-138), per board ---------- */

    fun stars(): List<Star> = read("stars.json") ?: emptyList()
    fun setStars(all: List<Star>) = write("stars.json", all)

    /* ---------- pinned messages (AII-339), per board, this phone only ---------- */

    fun pinnedMsgs(): List<PinnedMsg> = read("pinned_msgs.json") ?: emptyList()
    fun setPinnedMsgs(all: List<PinnedMsg>) = write("pinned_msgs.json", all)

    /* ---------- friends' safety words last accepted (AII-333), per board ---------- */

    fun safetySeen(): Map<String, String> = read("safety_seen.json") ?: emptyMap()
    fun setSafetySeen(all: Map<String, String>) = write("safety_seen.json", all)

    /* ---------- reminders (AII-201) ---------- */

    fun reminders(): List<Reminder> = read("reminders.json") ?: emptyList()
    fun setReminders(all: List<Reminder>) = write("reminders.json", all)

    /* ---------- seen: the newest message from them you have looked at, per chat ---------- */

    fun seen(): Map<String, String> = read("seen.json") ?: emptyMap()
    fun markSeen(chat: String, at: String?) {
        if (at == null) return
        val s = seen().toMutableMap()
        if ((s[chat] ?: "") < at) { s[chat] = at; write("seen.json", s) }
    }
    /** Mark as unread (AII-119): sets the mark exactly, even back. */
    fun unsee(chat: String, at: String) { write("seen.json", seen() + (chat to at)) }
    /** Undo Mark all as read (AII-158): these chats' marks exactly as [before] had them, none if it had none. */
    fun restoreSeen(chats: List<String>, before: Map<String, String>) {
        write("seen.json", chats.fold(seen()) { s, c -> before[c]?.let { s + (c to it) } ?: (s - c) })
    }
    /** The newest message per chat an alert already told you about. */
    fun told(): Map<String, String> = read("told.json") ?: emptyMap()
    fun setTold(all: Map<String, String>) = write("told.json", all)

    /* ---------- cache: show the last chats at once, also offline ---------- */

    fun cachedChats(): List<ChatRef>? = read("chats.json")
    fun cacheChats(l: List<ChatRef>) = write("chats.json", l)
    fun cachedConversation(chat: String): Conversation? = read(file(chat))
    fun cacheConversation(c: Conversation) = write(file(c.ref.key), c)
    private fun file(chat: String) = "c_" + chat.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json"

    /* ---------- outbox: typed but not yet on the board ---------- */

    fun outbox(): List<Pending> = read("outbox.json") ?: emptyList()
    fun saveOutbox(l: List<Pending>) = write("outbox.json", l)

    private inline fun <reified T> read(name: String): T? = runCatching {
        val f = File(dir, name); if (!f.exists()) null else json.decodeFromString<T>(f.readText())
    }.getOrNull()

    private inline fun <reified T> write(name: String, v: T) {
        val f = File(dir, name); val tmp = File(dir, "$name.tmp")
        tmp.writeText(json.encodeToString(v)); java.nio.file.Files.move(tmp.toPath(), f.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }
}
