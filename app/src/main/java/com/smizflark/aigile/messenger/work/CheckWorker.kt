package com.smizflark.aigile.messenger.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smizflark.aigile.messenger.App
import com.smizflark.aigile.messenger.MainActivity
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.News
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Looks for new messages about every 15 minutes (Android decides the exact time). No push server involved. */
class CheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = (applicationContext as App).repo
        val store = repo.store
        if (!store.connected || store.demo || !store.alerts) return Result.success()
        return try {
            check(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.success()   // offline or the PC is off: try again next round, quietly
        }
    }

    companion object {
        private const val NAME = "check-messages"
        const val CHANNEL = "messages"
        const val EXTRA_CHAT = "chat"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<CheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)

        /** Everything read: the "new messages" alert has nothing left to say. */
        fun clearAlert(context: Context) = NotificationManagerCompat.from(context).cancel(1)

        fun channel(context: Context) {
            val ch = NotificationChannel(CHANNEL, "Messages", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "New messages from your team leads and friends" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }

        /** One round: find chats with news, alert once for the batch. Also run when the app opens. */
        suspend fun check(context: Context) {
            val repo = (context.applicationContext as App).repo
            val store = repo.store
            val chats = repo.chats()
            val seen = store.seen()
            val told = store.told()
            val items = News.candidates(chats, seen, told).map { ref ->
                // Fetching the Lead Scrum Master's chat would mark it read on the board: go by its count instead.
                if (ref.kind == Kind.WATCHMAN) return@map News.Item(ref.key, ref.name, ref.last.takeIf { ref.unread > 0 })
                val convo = runCatching { repo.conversation(ref) }.getOrNull()
                News.Item(ref.key, ref.name, convo?.lastFromThem)
            }
            val (news, nextTold) = News.diff(items, seen, told)
            store.setTold(nextTold)
            val quiet = store.quietOn && News.quiet(LocalTime.now().hour, store.quietFrom, store.quietTo)
            if (news.isNotEmpty() && store.alerts && !quiet && !App.inForeground) notify(context, news)
        }

        private fun notify(context: Context, news: List<News.Item>) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (news.size == 1) putExtra(EXTRA_CHAT, news[0].chat)
            }
            val pi = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val title = if (news.size == 1) "New message" else "${news.size} chats have new messages"
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(News.summary(news))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
            NotificationManagerCompat.from(context).notify(1, n)
        }
    }
}
