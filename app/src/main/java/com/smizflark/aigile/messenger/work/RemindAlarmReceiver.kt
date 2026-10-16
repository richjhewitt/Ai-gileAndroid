package com.smizflark.aigile.messenger.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smizflark.aigile.messenger.App
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.RemindAlarm

/** Reminders on the minute (AII-396): the exact alarm lands here and shows the alert; after a reboot or an app update it sets the alarms again. */
class RemindAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as App
        // Anything due is shown; the rest gets its alarm again (alarms do not survive a reboot or an update).
        RemindWorker.fireDue(app)
        RemindAlarm.toReschedule(app.repo.store.reminders(), Now.millis()).forEach { RemindWorker.schedule(app, it) }
    }

    companion object {
        const val ACTION_ALARM = "com.smizflark.aigile.messenger.REMIND_ALARM"
    }
}
