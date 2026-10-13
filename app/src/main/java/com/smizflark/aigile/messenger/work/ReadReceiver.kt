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
        val chat = intent.getStringExtra(CheckWorker.EXTRA_CHAT) ?: return
        val app = context.applicationContext as App
        ChatNotifier.clear(app, chat)

        val done = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val ref = app.repo.store.cachedChats()?.firstOrNull { it.key == chat } ?: return@launch
                runCatching { app.repo.markRead(ref) }   // offline: the alert is gone, the next look at the chat reads it
            } finally {
                done.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.smizflark.aigile.messenger.READ"
    }
}
