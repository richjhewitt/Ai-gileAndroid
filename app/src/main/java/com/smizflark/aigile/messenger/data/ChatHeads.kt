package com.smizflark.aigile.messenger.data

import android.content.Context
import android.content.Intent
import androidx.core.app.Person
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.smizflark.aigile.messenger.BubbleActivity
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.work.CheckWorker

/** Chat heads (AII-381): a floating bubble per chat alert (Android notification bubbles, API 30+). */
object ChatHeads {
    /** Bubbles need Android 11 (API 30) and the Settings switch. */
    fun enabled(switchOn: Boolean, sdk: Int) = switchOn && sdk >= 30

    /** The chat's own long-lived shortcut; not "home:" so a pinned launcher shortcut is never replaced. */
    fun shortcutId(chat: String) = ShareTargets.id(chat)

    /** What the bubble opens: the one-chat activity. */
    fun intent(context: Context, chat: String): Intent =
        Intent(Intent.ACTION_VIEW).setClass(context, BubbleActivity::class.java).putExtra(CheckWorker.EXTRA_CHAT, chat)

    /** Android only bubbles an alert linked to a long-lived shortcut with a person and locus. */
    fun pushShortcut(context: Context, chat: String, name: String) {
        val person = Person.Builder().setName(name).setKey(chat).build()
        val info = ShortcutInfoCompat.Builder(context, shortcutId(chat)).setShortLabel(name.take(24)).setLongLabel(name)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher)).setIntent(intent(context, chat))
            .setLongLived(true).setPerson(person).setLocusId(LocusIdCompat(shortcutId(chat))).build()
        runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, info) }
        com.smizflark.aigile.messenger.ui.Shortcuts.forget()   // AII-398: the share-target version comes back on the next refresh
    }
}
