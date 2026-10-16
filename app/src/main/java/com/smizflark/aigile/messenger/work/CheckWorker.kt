package com.smizflark.aigile.messenger.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.smizflark.aigile.messenger.BuildConfig
import com.smizflark.aigile.messenger.data.Update
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
import com.smizflark.aigile.messenger.data.Pause
import com.smizflark.aigile.messenger.data.QuickReply
import androidx.core.app.RemoteInput
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Looks for new messages about every 15 minutes (Android decides the exact time). No push server involved. */
class CheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = (applicationContext as App).repo
        val store = repo.store
        if (!store.demo) runCatching { updateAlert(applicationContext) }   // AII-305: independent of message alerts
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
        const val EXTRA_MSG = "msg"
        /** AII-314: opened from the update alert, install the board build. */
        const val EXTRA_UPDATE = "update"
        private const val GROUP = "messages"
        private const val SUMMARY = ChatNotifier.SUMMARY

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<CheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)

        /** Everything read: the "new messages" alert has nothing left to say. */
        fun clearAlert(context: Context) = NotificationManagerCompat.from(context).cancel(SUMMARY)

        fun channel(context: Context) {
            val ch = NotificationChannel(CHANNEL, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_desc) }
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
                News.Item(ref.key, ref.name, convo?.lastFromThem, QuickReply.preview(convo), convo?.bubbles?.lastOrNull { !it.mine }?.key)
            }
            val (all, nextTold) = News.diff(items, seen, told)
            val news = News.audible(all, store.muted())
            store.setTold(nextTold)
            val quiet = (store.quietOn && News.quiet(LocalTime.now().hour, store.quietFrom, store.quietTo)) ||
                Pause.paused(System.currentTimeMillis(), store.pausedUntil)
            if (news.isNotEmpty() && store.alerts && !quiet && !App.inForeground) notify(context, news)
        }

        /** AII-305: a new build on GitHub (asked every few hours) gets one 'Update ready, tap to install' notification. */
        private suspend fun updateAlert(context: Context) {
            val store = (context.applicationContext as App).repo.store
            val now = System.currentTimeMillis()
            if (Update.due(now, store.updateChecked, Update.EVERY)) {
                store.updateChecked = now
                Update.check((context.applicationContext as App).repo.source, BuildConfig.VERSION_CODE)?.let { store.release = it }
            }
            val r = store.release?.takeIf { Update.newer(it, BuildConfig.VERSION_CODE) && it.build > store.updateTold } ?: return
            if (!allowed(context)) return
            // A board build (AII-314) needs the app to fetch it over the board link: the tap opens the app, which installs it.
            val tap = if (r.board) Intent(context, MainActivity::class.java).putExtra(EXTRA_UPDATE, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                else Intent(Intent.ACTION_VIEW, Uri.parse(r.url))
            val open = PendingIntent.getActivity(context, UPDATE_ID, tap,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            NotificationManagerCompat.from(context).notify(UPDATE_ID, base(context)
                .setContentTitle(context.getString(R.string.update_ready)).setContentText(context.getString(R.string.update_ready_text))
                .setContentIntent(open).build())
            store.updateTold = r.build
        }

        private const val UPDATE_ID = 7305

        private fun allowed(context: Context) = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        private fun notify(context: Context, news: List<News.Item>) {
            if (!allowed(context)) return
            val nm = NotificationManagerCompat.from(context)
            if (news.size > QuickReply.MAX_SINGLE) {
                // Too many to reply to one by one: one line, opens the chats list.
                nm.notify(SUMMARY, base(context).setContentTitle(context.getString(R.string.notify_many, news.size))
                    .setContentText(News.summary(news, context.getString(R.string.notify_more))).setContentIntent(ChatNotifier.openChat(context, null, 0)).build())
                return
            }
            // One notification per chat (AII-65), lines stacked, with a summary when there are several.
            for (n in news) ChatNotifier.post(context, n)
        }

        private fun base(context: Context) = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)

        /** After a quick reply: say how it went in the same alert (Android keeps a spinner until we do). */
        fun settle(context: Context, chat: String, outcome: String?, sentText: String? = null) {
            val nm = NotificationManagerCompat.from(context)
            val id = QuickReply.notificationId(chat)
            if (outcome == null || !allowed(context)) { ChatNotifier.clear(context, chat); return }
            val name = (context.applicationContext as App).repo.store.cachedChats()?.firstOrNull { it.key == chat }?.name ?: "Messenger"
            if (outcome.startsWith("Sent") && sentText != null) { ChatNotifier.sent(context, chat, name, sentText); return }
            nm.notify(id, base(context).setContentTitle(name).setContentText(outcome)
                .setContentIntent(ChatNotifier.openChat(context, chat, id)).setGroup(GROUP).build())
        }
    }
}
