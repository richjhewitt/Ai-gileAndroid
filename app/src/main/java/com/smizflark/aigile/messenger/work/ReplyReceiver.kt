package com.smizflark.aigile.messenger.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.smizflark.aigile.messenger.App
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.QuickReply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The Reply button on a message alert (AII-24): sends through the outbox, so a failed send waits in the chat. */
class ReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val chat = intent.getStringExtra(CheckWorker.EXTRA_CHAT) ?: return
        val text = QuickReply.clean(RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT))
        val app = context.applicationContext as App
        if (text == null) { CheckWorker.settle(app, chat, null); return }

        val done = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo = app.repo
                val ref = repo.store.cachedChats()?.firstOrNull { it.key == chat }
                if (ref == null) { CheckWorker.settle(app, chat, outcome(app, false, app.getString(R.string.notify_chat_missing))); return@launch }
                val convo = repo.store.cachedConversation(chat)
                val p = repo.queue(ref, text, QuickReply.target(convo))
                val r = repo.deliver(ref, p)
                if (r.isSuccess) repo.store.markSeen(chat, convo?.lastFromThem)
                CheckWorker.settle(app, chat, outcome(app, r.isSuccess, r.exceptionOrNull()?.message), text)
            } finally {
                done.finish()
            }
        }
    }

    companion object {
        const val KEY_TEXT = "reply"
        const val ACTION = "com.smizflark.aigile.messenger.REPLY"
    }
}

/** What the notification says once the quick reply has gone, or failed, in the phone's language. */
private fun outcome(ctx: Context, sent: Boolean, error: String?) = QuickReply.outcome(sent, error,
    ctx.getString(R.string.notify_sent), ctx.getString(R.string.notify_not_sent, "%1\$s"), ctx.getString(R.string.notify_no_connection))
