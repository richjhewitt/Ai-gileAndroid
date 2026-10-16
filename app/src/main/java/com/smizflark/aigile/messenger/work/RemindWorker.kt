package com.smizflark.aigile.messenger.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smizflark.aigile.messenger.App
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Remind
import com.smizflark.aigile.messenger.data.RemindAlarm
import com.smizflark.aigile.messenger.data.Reminder
import java.util.concurrent.TimeUnit

/** Remind me (AII-201): wakes at the reminder's time, even with the app closed, and shows the message as an alert. Reminders are kept in the Store, so they survive a restart. */
class RemindWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        fireDue(applicationContext)
        return Result.success()
    }

    companion object {
        /** Remind me in 1 hour about one message (AII-210, AII-240): saves the reminder and schedules it; returns when, or null when no time fits. */
        fun inAnHour(context: Context, store: com.smizflark.aigile.messenger.data.Store, chat: String, name: String, msg: String, text: String, note: String? = null): Long? {
            val at = Remind.at(Now.millis(), Remind.When.HOUR) ?: return null
            val r = Reminder(Remind.idOf(chat, msg), chat, name, msg, text, at, note)
            store.setReminders(Remind.add(store.reminders(), r))
            schedule(context, r)
            return at
        }

        /** Shows every reminder whose time has come and keeps the rest (the worker and the exact alarm both end here). */
        fun fireDue(context: Context) {
            val store = (context.applicationContext as App).repo.store
            val now = Now.millis()
            val all = store.reminders()
            CheckWorker.channel(context)
            Remind.due(all, now).forEach { ChatNotifier.remind(context, it) }
            store.setReminders(Remind.left(all, now))
        }

        fun canExact(context: Context): Boolean =
            RemindAlarm.useExact(Build.VERSION.SDK_INT, Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms())

        private fun alarmIntent(context: Context, id: String): PendingIntent = PendingIntent.getBroadcast(context, RemindAlarm.requestCode(id),
            Intent(context, RemindAlarmReceiver::class.java).setAction(RemindAlarmReceiver.ACTION_ALARM).setData(Uri.parse("remind://" + Uri.encode(id))),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        /** Exact alarm (AII-396) when allowed, else WorkManager; the other path is always cleared so a reminder never fires twice. */
        fun schedule(context: Context, r: Reminder) {
            val am = context.getSystemService(AlarmManager::class.java)
            if (canExact(context)) {
                WorkManager.getInstance(context).cancelUniqueWork(Remind.workName(r.id))
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, alarmIntent(context, r.id))
            } else {
                am.cancel(alarmIntent(context, r.id))
                scheduleWork(context, r)
            }
        }

        private fun scheduleWork(context: Context, r: Reminder) {
            val req = OneTimeWorkRequestBuilder<RemindWorker>()
                .setInitialDelay((r.at - Now.millis()).coerceAtLeast(0), TimeUnit.MILLISECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(Remind.workName(r.id), ExistingWorkPolicy.REPLACE, req)
        }

        fun cancel(context: Context, id: String) {
            WorkManager.getInstance(context).cancelUniqueWork(Remind.workName(id))
            context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context, id))
        }
    }
}
