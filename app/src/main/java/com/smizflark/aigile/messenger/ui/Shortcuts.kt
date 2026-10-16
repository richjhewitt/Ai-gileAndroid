package com.smizflark.aigile.messenger.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.smizflark.aigile.messenger.data.ShareTargets
import com.smizflark.aigile.messenger.MainActivity
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.work.CheckWorker

/** Hold the app icon (AII-53): New message, then your most recent chats. */
object Shortcuts {
    const val ACTION_NEW = "com.smizflark.aigile.messenger.NEW_MESSAGE"
    private var shown: List<Pair<String, String>>? = null

    /** The chats for the icon menu: newest first, at most [n]. */
    fun top(chats: List<ChatRef>, n: Int = ShareTargets.MAX) = chats.filter { it.last != null }.sortedByDescending { it.last }.take(n)

    /** Chat heads re-push a chat's shortcut with their own intent; forget what we showed so the next refresh puts ours back (AII-398). */
    fun forget() { shown = null }

    /** Puts the menu in step with the chats; only touches Android when the list really changed. */
    fun update(context: Context, chats: List<ChatRef>) {
        val pick = top(chats)
        val now = pick.map { it.key to it.name }
        if (now == shown) return
        val open = { action: String -> Intent(action).setClass(context, MainActivity::class.java) }
        val new = ShortcutInfoCompat.Builder(context, "new").setShortLabel(context.getString(R.string.new_message))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_new)).setIntent(open(ACTION_NEW)).setRank(0).build()
        val list = pick.mapIndexed { i, c ->
            // AII-398: long-lived with the share category, so the same shortcut is also a share sheet target.
            ShortcutInfoCompat.Builder(context, ShareTargets.id(c.key)).setShortLabel(c.name.take(24)).setLongLabel(c.name)
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_chat))
                .setIntent(open(Intent.ACTION_VIEW).putExtra(CheckWorker.EXTRA_CHAT, c.key)).setRank(i + 1)
                .setLongLived(true).setCategories(setOf(ShareTargets.CATEGORY))
                .setPerson(Person.Builder().setName(c.name).setKey(c.key).build()).build()
        }
        // Android limits how often a background app may change these; a miss just waits for the next refresh.
        if (runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, listOf(new) + list) }.isSuccess) shown = now
    }

    fun clear(context: Context) {
        runCatching { context.getSystemService(ShortcutManager::class.java)?.removeAllDynamicShortcuts() }
        shown = null
    }
}
