package com.smizflark.aigile.messenger.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smizflark.aigile.messenger.App

/** The Remind me in 1 hour button on a message alert (AII-210): the alert goes at once and the alert's newest message is set as a reminder (Remind.add + RemindWorker.schedule). */
class RemindReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val chat = intent.getStringExtra(CheckWorker.EXTRA_CHAT) ?: return
        val msg = intent.getStringExtra(CheckWorker.EXTRA_MSG) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT) ?: return
        val name = intent.getStringExtra(EXTRA_NAME) ?: chat
        val app = context.applicationContext as App
        // AII-224: from a reminder alert only that alert goes; from a message alert the chat's alert goes.
        val own = intent.getIntExtra(EXTRA_NOTIFY, 0)
        if (own != 0) androidx.core.app.NotificationManagerCompat.from(app).cancel(own) else ChatNotifier.clear(app, chat)
        RemindWorker.inAnHour(app, app.repo.store, chat, name, msg, text, intent.getStringExtra(EXTRA_NOTE))
    }

    companion object {
        const val ACTION = "com.smizflark.aigile.messenger.REMIND"
        const val EXTRA_NOTE = "remind_note"
        const val EXTRA_NOTIFY = "remind_notify"
        const val EXTRA_NAME = "remind_name"
        const val EXTRA_TEXT = "remind_text"
        /** Kept on the alert itself, so a later line (or your own reply) in it keeps the button. */
        const val EXTRA_MSG_KEY = "remind_msg"
    }
}
