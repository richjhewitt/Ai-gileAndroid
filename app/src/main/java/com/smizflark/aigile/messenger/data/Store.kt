package com.smizflark.aigile.messenger.data

import android.content.Context
import android.content.SharedPreferences

/* ---------- connection and settings ---------- */

class Store(context: Context) : StoreFiles(context) {
    /** Pairing: board address, sealed keys. Never backed up (AII-81). */
    private val prefs = context.getSharedPreferences("messenger", Context.MODE_PRIVATE)
    /** Settings that follow you to a new phone (AII-81): look, text size, pins, mutes, quiet hours. */
    private val settings = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE).also { moveSettings(prefs, it) }

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

    /** AII-234: seconds away before the lock asks again; 0 = straight away, default 60 as before. Kept with [lock], so not backed up. */
    var lockAfter: Int
        get() = prefs.getInt("lockAfter", 60)
        set(v) = prefs.edit().putInt("lockAfter", v).apply()

    /** AII-208: the hour "Send in the morning" uses (Send later, AII-146). */
    var morning: Int
        get() = settings.getInt("morning", 8)
        set(v) = settings.edit().putInt("morning", v).apply()

    /** AII-212: smaller chats list rows, so more chats fit. */
    /** Voice note playback speed (AII-378): 1, 1.5 or 2. */
    var voiceSpeed: Float
        get() = Voice.parseSpeed(settings.getString("voiceSpeed", null))
        set(v) = settings.edit().putString("voiceSpeed", Voice.speedLabel(v)).apply()
    /** Sort chats (AII-241): "newest" (default) or "az". */
    var sortChats: String
        get() = settings.getString("sortChats", "newest") ?: "newest"
        set(v) = settings.edit().putString("sortChats", v).apply()
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
    /** Chat heads (AII-381): a floating bubble per chat alert; off by default. */
    var chatHeads: Boolean
        get() = settings.getBoolean("chatHeads", false)
        set(v) = settings.edit().putBoolean("chatHeads", v).apply()

    var sentAlerts: Boolean
        get() = settings.getBoolean("sentAlerts", true)
        set(v) = settings.edit().putBoolean("sentAlerts", v).apply()

    /** The keyboard's Enter key sends the message instead of starting a new line (AII-126). */
    /** AII-198: GIF links play in the chat (AII-188); off keeps them plain links. */
    var linkPreviews: Boolean
        get() = settings.getBoolean("linkPreviews", false)
        set(v) = settings.edit().putBoolean("linkPreviews", v).apply()
    /** AII-391: "always" or "wifi": on mobile data pictures wait for a tap. */
    var loadPictures: String
        get() = settings.getString("loadPictures", PicLoad.ALWAYS) ?: PicLoad.ALWAYS
        set(v) = settings.edit().putString("loadPictures", v).apply()
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
    /** AII-233: how long Undo send holds a message, 5 or 10 seconds. */
    var undoSeconds: Int
        get() = settings.getInt("undoSeconds", 5)
        set(v) = settings.edit().putInt("undoSeconds", v).apply()

    var enterSends: Boolean
        get() = settings.getBoolean("enterSends", false)
        set(v) = settings.edit().putBoolean("enterSends", v).apply()

    /** The Next unread button at the bottom of a chat (AII-254). */
    var nextUnread: Boolean
        get() = settings.getBoolean("nextUnread", true)
        set(v) = settings.edit().putBoolean("nextUnread", v).apply()

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

    /** What's new bar (AII-268): the version code last seen; 0 on a brand-new install. */
    var lastSeenVersion: Int
        get() = settings.getInt("lastSeenVersion", 0)
        set(v) = settings.edit().putInt("lastSeenVersion", v).apply()

    /** Demo welcome tips (AII-276): true once Got it was tapped. */
    var tipsSeen: Boolean
        get() = settings.getBoolean("tipsSeen", false)
        set(v) = settings.edit().putBoolean("tipsSeen", v).apply()

    var lastSync: Long
        get() = prefs.getLong("lastSync", 0)
        set(v) = prefs.edit().putLong("lastSync", v).apply()

    /** 'New version' bar (AII-132): when GitHub was last asked, and the newest release it named. */
    var updateChecked: Long
        get() = prefs.getLong("updateChecked", 0)
        set(v) = prefs.edit().putLong("updateChecked", v).apply()

    /** The newest build already announced by a notification (AII-305): each update alerts once. */
    var updateTold: Int
        get() = prefs.getInt("updateTold", 0)
        set(v) = prefs.edit().putInt("updateTold", v).apply()

    var release: Release?
        get() = prefs.getString("releaseUrl", null)?.let { Release(prefs.getInt("releaseBuild", 0), it) }
        set(v) = prefs.edit().putInt("releaseBuild", v?.build ?: 0).putString("releaseUrl", v?.url).apply()

    fun signOut() {
        prefs.edit().clear().apply()
        settings.edit().clear().apply()
        clearCache()
    }


    /** Chats whose alerts show with no sound and no buzz (AII-207), by chat key. */
    var silent: Set<String>
        get() = settings.getStringSet("silent", emptySet())?.toSet() ?: emptySet()
        set(v) = settings.edit().putStringSet("silent", v).apply()

    /** Chats with their own alert sound (AII-373), see [ChatSound]. */
    var chatSounds: Set<String>
        get() = settings.getStringSet("chatSounds", emptySet())?.toSet() ?: emptySet()
        set(v) = settings.edit().putStringSet("chatSounds", v).apply()

    /** Chats with their own colour (AII-375), see [ChatColour]. */
    var chatColours: Set<String>
        get() = settings.getStringSet("chatColours", emptySet())?.toSet() ?: emptySet()
        set(v) = settings.edit().putStringSet("chatColours", v).apply()

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


    /** Settings in a passphrase-sealed file (AII-349), and back. [importSettings] throws SettingsExport.BadFile on a wrong passphrase. */
    fun exportSettings(pass: CharArray): ByteArray = SettingsExport.seal(SettingsExport.dump(settings), pass)
    fun importSettings(file: ByteArray, pass: CharArray): Int = SettingsExport.load(SettingsExport.open(file, pass), settings)

    /** Back to defaults for every look/behaviour setting; pins, mutes, board connection and the rest stay. */
    fun resetSettings() { val e = settings.edit(); RESETTABLE.forEach { e.remove(it) }; e.apply() }

    companion object {
        const val SETTINGS = "settings"
        /** The preferences that go into backup. Everything else stays in the "messenger" file. */
        val BACKED_UP = listOf("alerts", "quietFrom", "quietTo", "morning", "quietOn", "look", "phoneColours", "textSize", "compact", "mutedLast", "sortChats", "enterSends", "undoSend", "undoSeconds", "gifs", "loadPictures", "linkPreviews", "buzz", "quickReplies", "quickReplyText", "pinned", "muted", "silent", "chatColours", "showTimes", "swipeRight", "swipeLeft", "nextUnread")

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
