package com.smizflark.aigile.messenger.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.smizflark.aigile.messenger.MainActivity
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.work.CheckWorker

/** Hold the app icon (AII-53): New message, then your most recent chats. */
object Shortcuts {
    const val ACTION_NEW = "com.smizflark.aigile.messenger.NEW_MESSAGE"
    private var shown: List<Pair<String, String>>? = null

    /** The chats for the icon menu: newest first, at most [n]. */
    fun top(chats: List<ChatRef>, n: Int = 3) = chats.filter { it.last != null }.sortedByDescending { it.last }.take(n)

    /** Puts the menu in step with the chats; only touches Android when the list really changed. */
    fun update(context: Context, chats: List<ChatRef>) {
        val pick = top(chats)
        val now = pick.map { it.key to it.name }
        if (now == shown) return
        val sm = context.getSystemService(ShortcutManager::class.java) ?: return
        val open = { action: String -> Intent(action).setClass(context, MainActivity::class.java) }
        val new = ShortcutInfo.Builder(context, "new").setShortLabel(context.getString(R.string.new_message))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_new)).setIntent(open(ACTION_NEW)).setRank(0).build()
        val list = pick.mapIndexed { i, c ->
            ShortcutInfo.Builder(context, "chat:${c.key}").setShortLabel(c.name.take(24)).setLongLabel(c.name)
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_chat))
                .setIntent(open(Intent.ACTION_VIEW).putExtra(CheckWorker.EXTRA_CHAT, c.key)).setRank(i + 1).build()
        }
        // Android limits how often a background app may change these; a miss just waits for the next refresh.
        if (runCatching { sm.dynamicShortcuts = listOf(new) + list }.isSuccess) shown = now
    }

    fun clear(context: Context) {
        runCatching { context.getSystemService(ShortcutManager::class.java)?.removeAllDynamicShortcuts() }
        shown = null
    }
}
