package com.smizflark.aigile.messenger.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smizflark.aigile.messenger.App
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Remind
import com.smizflark.aigile.messenger.data.Reminder

/** The Remind me in 1 hour button on a message alert (AII-210): the alert goes at once and the alert's newest message is set as a reminder (Remind.add + RemindWorker.schedule). */
class RemindReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val chat = intent.getStringExtra(CheckWorker.EXTRA_CHAT) ?: return
        val msg = intent.getStringExtra(CheckWorker.EXTRA_MSG) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT) ?: return
        val name = intent.getStringExtra(EXTRA_NAME) ?: chat
        val app = context.applicationContext as App
        ChatNotifier.clear(app, chat)
        val at = Remind.at(Now.millis(), Remind.When.HOUR) ?: return
        val r = Reminder(Remind.idOf(chat, msg), chat, name, msg, text, at)
        app.repo.store.setReminders(Remind.add(app.repo.store.reminders(), r))
        RemindWorker.schedule(app, r)
    }

    companion object {
        const val ACTION = "com.smizflark.aigile.messenger.REMIND"
        const val EXTRA_NAME = "remind_name"
        const val EXTRA_TEXT = "remind_text"
        /** Kept on the alert itself, so a later line (or your own reply) in it keeps the button. */
        const val EXTRA_MSG_KEY = "remind_msg"
    }
}
