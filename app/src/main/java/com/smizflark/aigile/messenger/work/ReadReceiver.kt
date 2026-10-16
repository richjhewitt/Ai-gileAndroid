package com.smizflark.aigile.messenger.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smizflark.aigile.messenger.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The Mark as read button on a message alert (AII-116): the alert goes at once, then the chat is read here and on the board. */
class ReadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as App
        // AII-387: Mark all as read on the summary: every chat with an alert, the same way as one.
        val chats = if (intent.action == ACTION_ALL) ChatNotifier.alertingChats(app)
            else listOfNotNull(intent.getStringExtra(CheckWorker.EXTRA_CHAT))
        if (chats.isEmpty()) { if (intent.action == ACTION_ALL) ChatNotifier.refreshSummary(app); return }
        chats.forEach { ChatNotifier.clear(app, it) }

        val done = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val known = app.repo.store.cachedChats() ?: return@launch
                chats.forEach { chat ->
                    val ref = known.firstOrNull { it.key == chat } ?: return@forEach
                    runCatching { app.repo.markRead(ref) }   // offline: the alert is gone, the next look at the chat reads it
                }
            } finally {
                done.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.smizflark.aigile.messenger.READ"
        const val ACTION_ALL = "com.smizflark.aigile.messenger.READ_ALL"
    }
}
