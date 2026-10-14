package com.smizflark.aigile.messenger.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.encodeToString
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/* ---------- phone key, sealed with the Android Keystore ---------- */

/** The phone key never sits in plain text: it is sealed with a Keystore key that can't leave this phone. */
object Vault {
    private const val ALIAS = "aigile-messenger"

    private fun secret(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    fun seal(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secret()) }
        val out = c.iv + c.doFinal(plain.toByteArray())
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun open(sealed: String): String? = runCatching {
        val all = Base64.decode(sealed, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, all.copyOfRange(0, 12)))
        String(c.doFinal(all.copyOfRange(12, all.size)))
    }.getOrNull()
}

/* ---------- connection and settings ---------- */

class Store(context: Context) {
    /** Pairing: board address, sealed keys. Never backed up (AII-81). */
    private val prefs = context.getSharedPreferences("messenger", Context.MODE_PRIVATE)
    /** Settings that follow you to a new phone (AII-81): look, text size, pins, mutes, quiet hours. */
    private val settings = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE).also { moveSettings(prefs, it) }
    private val dir = File(context.filesDir, "cache").apply { mkdirs() }

    var url: String
        get() = prefs.getString("url", "") ?: ""
        set(v) = prefs.edit().putString("url", v).apply()

    var key: String
        get() = prefs.getString("key", null)?.let { Vault.open(it) } ?: ""
        set(v) = prefs.edit().putString("key", if (v.isEmpty()) null else Vault.seal(v)).apply()

    /** Device id from a sealed pairing (AII-19), Keystore-sealed like the key. Empty for an older pairing. */
    var device: String
        get() = prefs.getString("device", null)?.let { Vault.open(it) } ?: ""
        set(v) = prefs.edit().putString("device", if (v.isEmpty()) null else Vault.seal(v)).apply()

    var demo: Boolean
        get() = prefs.getBoolean("demo", false)
        set(v) = prefs.edit().putBoolean("demo", v).apply()

    var owner: String
        get() = prefs.getString("owner", "") ?: ""
        set(v) = prefs.edit().putString("owner", v).apply()

    val connected get() = demo || url.isNotBlank()

    var alerts: Boolean
        get() = settings.getBoolean("alerts", true)
        set(v) = settings.edit().putBoolean("alerts", v).apply()

    /** Ask for fingerprint, face or PIN when the app opens (AII-63). */
    var lock: Boolean
        get() = prefs.getBoolean("lock", false)
        set(v) = prefs.edit().putBoolean("lock", v).apply()

    /** AII-208: the hour "Send in the morning" uses (Send later, AII-146). */
    var morning: Int
        get() = settings.getInt("morning", 8)
        set(v) = settings.edit().putInt("morning", v).apply()

    /** AII-212: smaller chats list rows, so more chats fit. */
    var mutedLast: Boolean
        get() = settings.getBoolean("mutedLast", false)
        set(v) = settings.edit().putBoolean("mutedLast", v).apply()
    var compact: Boolean
        get() = settings.getBoolean("compact", false)
        set(v) = settings.edit().putBoolean("compact", v).apply()

    var quietFrom: Int
        get() = settings.getInt("quietFrom", 22)
        set(v) = settings.edit().putInt("quietFrom", v).apply()

    var quietTo: Int
        get() = settings.getInt("quietTo", 7)
        set(v) = settings.edit().putInt("quietTo", v).apply()

    /** Alerts paused until this time, epoch millis; 0 = not paused (AII-77). */
    var pausedUntil: Long
        get() = prefs.getLong("pausedUntil", 0L)
        set(v) = prefs.edit().putLong("pausedUntil", v).apply()

    var quietOn: Boolean
        get() = settings.getBoolean("quietOn", true)
        set(v) = settings.edit().putBoolean("quietOn", v).apply()

    /** Look: system, light, dark or black (pure black for OLED screens). */
    var look: String
        get() = settings.getString("look", "system") ?: "system"
        set(v) = settings.edit().putString("look", v).apply()

    /** Use the phone's own colours (Material You, Android 12+). */
    var phoneColours: Boolean
        get() = settings.getBoolean("phoneColours", true)
        set(v) = settings.edit().putBoolean("phoneColours", v).apply()

    /** Text size (AII-66): small, normal, large or huge. */
    var textSize: String
        get() = settings.getString("textSize", "normal") ?: "normal"
        set(v) = settings.edit().putString("textSize", v).apply()

    /** A Sent alert when a Send later message goes by itself (AII-175). */
    var sentAlerts: Boolean
        get() = settings.getBoolean("sentAlerts", true)
        set(v) = settings.edit().putBoolean("sentAlerts", v).apply()

    /** The keyboard's Enter key sends the message instead of starting a new line (AII-126). */
    /** AII-198: GIF links play in the chat (AII-188); off keeps them plain links. */
    var gifs: Boolean
        get() = settings.getBoolean("gifs", true)
        set(v) = settings.edit().putBoolean("gifs", v).apply()
    /** AII-216: what swiping a chat right / left does (SwipeChoice ids). */
    var swipeRight: String
        get() = settings.getString("swipeRight", SWIPE_RIGHT_DEFAULT.id) ?: SWIPE_RIGHT_DEFAULT.id
        set(v) = settings.edit().putString("swipeRight", v).apply()
    var swipeLeft: String
        get() = settings.getString("swipeLeft", SWIPE_LEFT_DEFAULT.id) ?: SWIPE_LEFT_DEFAULT.id
        set(v) = settings.edit().putString("swipeLeft", v).apply()
    /** AII-205: the little buzz on send and on a new reply (AII-49); off keeps the phone still. */
    var buzz: Boolean
        get() = settings.getBoolean("buzz", true)
        set(v) = settings.edit().putBoolean("buzz", v).apply()

    var showTimes: Boolean
        get() = settings.getBoolean("showTimes", false)
        set(v) = settings.edit().putBoolean("showTimes", v).apply()
    var undoSend: Boolean
        get() = settings.getBoolean("undoSend", false)
        set(v) = settings.edit().putBoolean("undoSend", v).apply()

    var enterSends: Boolean
        get() = settings.getBoolean("enterSends", false)
        set(v) = settings.edit().putBoolean("enterSends", v).apply()

    /** Ready-made replies above an empty message box (AII-179). */
    var quickReplies: Boolean
        get() = settings.getBoolean("quickReplies", true)
        set(v) = settings.edit().putBoolean("quickReplies", v).apply()

    /** Your own quick replies, one per line (AII-180); empty means the starting ones. */
    var quickReplyText: String
        get() = settings.getString("quickReplyText", "") ?: ""
        set(v) = settings.edit().putString("quickReplyText", v).apply()

    /** Chats pinned to the top of the list (AII-47), by chat key. */
    var pinned: Set<String>
        get() = settings.getStringSet("pinned", emptySet())?.toSet() ?: emptySet()
        set(v) = settings.edit().putStringSet("pinned", v).apply()

    var lastSync: Long
        get() = prefs.getLong("lastSync", 0)
        set(v) = prefs.edit().putLong("lastSync", v).apply()

    /** 'New version' bar (AII-132): when GitHub was last asked, and the newest release it named. */
    var updateChecked: Long
        get() = prefs.getLong("updateChecked", 0)
        set(v) = prefs.edit().putLong("updateChecked", v).apply()

    var release: Release?
        get() = prefs.getString("releaseUrl", null)?.let { Release(prefs.getInt("releaseBuild", 0), it) }
        set(v) = prefs.edit().putInt("releaseBuild", v?.build ?: 0).putString("releaseUrl", v?.url).apply()

    fun signOut() {
        prefs.edit().clear().apply()
        settings.edit().clear().apply()
        clearCache()
    }

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

    /** Chats whose alerts show with no sound and no buzz (AII-207), by chat key. */
    var silent: Set<String>
        get() = settings.getStringSet("silent", emptySet())?.toSet() ?: emptySet()
        set(v) = settings.edit().putStringSet("silent", v).apply()

    /* ---------- muted chats: no alerts from them (AII-48) ---------- */

    /** Muted now: a mute for a while (AII-157) ends by itself. */
    fun muted(): Set<String> = Mute.active(settings.getStringSet("muted", null)?.toSet() ?: emptySet(), mutedUntil(), Now.millis())
    /** When each timed mute ends, as "chat<TAB>epoch millis"; a chat not here is muted until unmuted. */
    fun mutedUntil(): Map<String, Long> = (settings.getStringSet("mutedUntil", null) ?: emptySet())
        .mapNotNull { e -> e.split('\t').takeIf { it.size == 2 }?.let { (c, t) -> t.toLongOrNull()?.let { c to it } } }.toMap()
    fun setMuted(chat: String, on: Boolean, until: Long? = null) {
        val ends = mutedUntil() - chat + (if (on && until != null) mapOf(chat to until) else emptyMap())
        settings.edit().putStringSet("muted", if (on) muted() + chat else muted() - chat)
            .putStringSet("mutedUntil", ends.filterKeys { it in muted() || it == chat }.map { (c, t) -> "$c\t$t" }.toSet()).apply()
    }

    /** Boards (project slugs) whose stars this phone has sent once (AII-141): the phone's list wins on the first sync. */
    var starsSynced: Set<String>
        get() = prefs.getStringSet("starsSynced", emptySet())?.toSet() ?: emptySet()
        set(v) = prefs.edit().putStringSet("starsSynced", v).apply()

    /* ---------- closed chats: off the list until a newer message (AII-131), per board ---------- */

    fun closed(): Map<String, String> = read("closed.json") ?: emptyMap()
    fun setClosed(all: Map<String, String>) = write("closed.json", all)

    /* ---------- recent searches (AII-155), per board ---------- */

    fun recentSearches(): List<String> = read("recent_searches.json") ?: emptyList()
    fun setRecentSearches(all: List<String>) = write("recent_searches.json", all)

    /* ---------- starred messages (AII-138), per board ---------- */

    fun stars(): List<Star> = read("stars.json") ?: emptyList()
    fun setStars(all: List<Star>) = write("stars.json", all)

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

    /** Back to defaults for every look/behaviour setting; pins, mutes, board connection and the rest stay. */
    fun resetSettings() { val e = settings.edit(); RESETTABLE.forEach { e.remove(it) }; e.apply() }

    companion object {
        const val SETTINGS = "settings"
        /** The preferences that go into backup. Everything else stays in the "messenger" file. */
        val BACKED_UP = listOf("alerts", "quietFrom", "quietTo", "morning", "quietOn", "look", "phoneColours", "textSize", "compact", "mutedLast", "enterSends", "undoSend", "gifs", "buzz", "quickReplies", "quickReplyText", "pinned", "muted", "silent", "showTimes", "swipeRight", "swipeLeft")

        /** What "Reset settings" clears (AII-217): every backed-up setting except the ones that are your data. */
        val KEPT_ON_RESET = setOf("pinned", "muted", "silent")
        val RESETTABLE = BACKED_UP.filter { it !in KEPT_ON_RESET }

        /** One-time move of the settings out of the pairing file, so the pairing file can stay out of backup. */
        fun moveSettings(from: SharedPreferences, to: SharedPreferences) {
            val old = BACKED_UP.filter { from.contains(it) }
            if (old.isEmpty()) return
            val e = to.edit(); val r = from.edit()
            for (k in old) {
                if (!to.contains(k)) when (val v = from.all[k]) {
                    is String -> e.putString(k, v)
                    is Boolean -> e.putBoolean(k, v)
                    is Int -> e.putInt(k, v)
                    is Long -> e.putLong(k, v)
                    is Float -> e.putFloat(k, v)
                    is Set<*> -> e.putStringSet(k, v.filterIsInstance<String>().toSet())
                }
                r.remove(k)
            }
            e.apply(); r.apply()
        }
    }
}
