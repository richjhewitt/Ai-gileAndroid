package com.smizflark.aigile.messenger.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smizflark.aigile.messenger.App
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Remind
import com.smizflark.aigile.messenger.data.Reminder
import java.util.concurrent.TimeUnit

/** Remind me (AII-201): wakes at the reminder's time, even with the app closed, and shows the message as an alert. Reminders are kept in the Store, so they survive a restart. */
class RemindWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = (applicationContext as App).repo.store
        val now = Now.millis()
        val all = store.reminders()
        CheckWorker.channel(applicationContext)
        Remind.due(all, now).forEach { ChatNotifier.remind(applicationContext, it) }
        store.setReminders(Remind.left(all, now))
        return Result.success()
    }

    companion object {
        fun schedule(context: Context, r: Reminder) {
            val req = OneTimeWorkRequestBuilder<RemindWorker>()
                .setInitialDelay((r.at - Now.millis()).coerceAtLeast(0), TimeUnit.MILLISECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(Remind.workName(r.id), ExistingWorkPolicy.REPLACE, req)
        }

        fun cancel(context: Context, id: String) { WorkManager.getInstance(context).cancelUniqueWork(Remind.workName(id)) }
    }
}
