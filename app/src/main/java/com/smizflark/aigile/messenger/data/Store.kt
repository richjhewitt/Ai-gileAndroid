package com.smizflark.aigile.messenger.data

import android.content.Context
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
    private val prefs = context.getSharedPreferences("messenger", Context.MODE_PRIVATE)
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
        get() = prefs.getBoolean("alerts", true)
        set(v) = prefs.edit().putBoolean("alerts", v).apply()

    /** Ask for fingerprint, face or PIN when the app opens (AII-63). */
    var lock: Boolean
        get() = prefs.getBoolean("lock", false)
        set(v) = prefs.edit().putBoolean("lock", v).apply()

    var quietFrom: Int
        get() = prefs.getInt("quietFrom", 22)
        set(v) = prefs.edit().putInt("quietFrom", v).apply()

    var quietTo: Int
        get() = prefs.getInt("quietTo", 7)
        set(v) = prefs.edit().putInt("quietTo", v).apply()

    var quietOn: Boolean
        get() = prefs.getBoolean("quietOn", true)
        set(v) = prefs.edit().putBoolean("quietOn", v).apply()

    /** Look: system, light, dark or black (pure black for OLED screens). */
    var look: String
        get() = prefs.getString("look", "system") ?: "system"
        set(v) = prefs.edit().putString("look", v).apply()

    /** Use the phone's own colours (Material You, Android 12+). */
    var phoneColours: Boolean
        get() = prefs.getBoolean("phoneColours", true)
        set(v) = prefs.edit().putBoolean("phoneColours", v).apply()

    /** Text size (AII-66): small, normal, large or huge. */
    var textSize: String
        get() = prefs.getString("textSize", "normal") ?: "normal"
        set(v) = prefs.edit().putString("textSize", v).apply()

    /** Chats pinned to the top of the list (AII-47), by chat key. */
    var pinned: Set<String>
        get() = prefs.getStringSet("pinned", emptySet())?.toSet() ?: emptySet()
        set(v) = prefs.edit().putStringSet("pinned", v).apply()

    var lastSync: Long
        get() = prefs.getLong("lastSync", 0)
        set(v) = prefs.edit().putLong("lastSync", v).apply()

    fun signOut() {
        prefs.edit().clear().apply()
        clearCache()
    }

    /** Saved chats, seen marks and the outbox: they belong to one board. Settings stay. */
    fun clearCache() { dir.listFiles()?.forEach { it.delete() } }
    /* ---------- drafts: half-typed replies, per chat ---------- */

    fun draft(chat: String): String = read<Map<String, String>>("drafts.json")?.get(chat) ?: ""
    fun saveDraft(chat: String, text: String) {
        val all = read<Map<String, String>>("drafts.json") ?: emptyMap()
        val next = Drafts.put(all, chat, text)
        if (next != all) write("drafts.json", next)
    }

    /* ---------- muted chats: no alerts from them (AII-48) ---------- */

    fun muted(): Set<String> = prefs.getStringSet("muted", null)?.toSet() ?: emptySet()
    fun setMuted(chat: String, on: Boolean) =
        prefs.edit().putStringSet("muted", if (on) muted() + chat else muted() - chat).apply()

    /* ---------- seen: the newest message from them you have looked at, per chat ---------- */

    fun seen(): Map<String, String> = read("seen.json") ?: emptyMap()
    fun markSeen(chat: String, at: String?) {
        if (at == null) return
        val s = seen().toMutableMap()
        if ((s[chat] ?: "") < at) { s[chat] = at; write("seen.json", s) }
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
