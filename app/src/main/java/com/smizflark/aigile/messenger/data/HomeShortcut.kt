package com.smizflark.aigile.messenger.data

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.smizflark.aigile.messenger.MainActivity
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.work.CheckWorker

/** Add to home screen (AII-368, AII-376): a launcher shortcut that opens the app straight into one chat. */
object HomeShortcut {
    /** One shortcut per chat; pinning the same chat again just updates it. */
    fun id(chat: String) = "home:$chat"

    /** The same route an alert uses (MainActivity reads CheckWorker.EXTRA_CHAT). */
    fun intent(context: Context, chat: String): Intent =
        Intent(Intent.ACTION_VIEW).setClass(context, MainActivity::class.java).putExtra(CheckWorker.EXTRA_CHAT, chat)

    /** Asks the launcher to pin it; false when the launcher can't pin shortcuts. */
    fun pin(context: Context, chat: String, name: String): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val info = ShortcutInfoCompat.Builder(context, id(chat)).setShortLabel(name.take(24)).setLongLabel(name)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher)).setIntent(intent(context, chat)).build()
        return runCatching { ShortcutManagerCompat.requestPinShortcut(context, info, null) }.getOrDefault(false)
    }
}
