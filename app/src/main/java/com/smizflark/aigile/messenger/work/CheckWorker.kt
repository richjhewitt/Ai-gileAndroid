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
import com.smizflark.aigile.messenger.data.QuickReply
import androidx.core.app.RemoteInput
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
        private const val GROUP = "messages"
        private const val SUMMARY = 1

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<CheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)

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
                val convo = runCatching { repo.conversation(ref) }.getOrNull()
                News.Item(ref.key, ref.name, convo?.lastFromThem, QuickReply.preview(convo))
            }
            val (news, nextTold) = News.diff(items, seen, told)
            store.setTold(nextTold)
            val quiet = store.quietOn && News.quiet(LocalTime.now().hour, store.quietFrom, store.quietTo)
            if (news.isNotEmpty() && store.alerts && !quiet && !App.inForeground) notify(context, news)
        }

        private fun allowed(context: Context) = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        private fun openChat(context: Context, chat: String?, code: Int): PendingIntent {
            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (chat != null) putExtra(EXTRA_CHAT, chat)
            }
            return PendingIntent.getActivity(context, code, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun base(context: Context) = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)

        private fun notify(context: Context, news: List<News.Item>) {
            if (!allowed(context)) return
            val nm = NotificationManagerCompat.from(context)
            if (news.size > QuickReply.MAX_SINGLE) {
                // Too many to reply to one by one: one line, opens the chats list.
                nm.notify(SUMMARY, base(context).setContentTitle("${news.size} chats have new messages")
                    .setContentText(News.summary(news)).setContentIntent(openChat(context, null, 0)).build())
                return
            }
            // One alert per chat, each with its own Reply button (AII-24); grouped when there are several.
            for (n in news) {
                val id = QuickReply.notificationId(n.chat)
                val reply = Intent(context, ReplyReceiver::class.java).setAction(ReplyReceiver.ACTION).putExtra(EXTRA_CHAT, n.chat)
                val replyPi = PendingIntent.getBroadcast(context, id, reply,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0))
                val action = NotificationCompat.Action.Builder(R.drawable.ic_notify, "Reply", replyPi)
                    .addRemoteInput(RemoteInput.Builder(ReplyReceiver.KEY_TEXT).setLabel("Reply to ${n.name}").build())
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                    .setShowsUserInterface(false)
                    .build()
                val text = n.preview ?: "New message"
                nm.notify(id, base(context)
                    .setContentTitle(n.name)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(openChat(context, n.chat, id))
                    .addAction(action)
                    .setGroup(GROUP)
                    .build())
            }
            if (news.size > 1) nm.notify(SUMMARY, base(context).setContentTitle("${news.size} chats have new messages")
                .setContentText(News.summary(news)).setGroup(GROUP).setGroupSummary(true).build())
            else nm.cancel(SUMMARY)
        }

        /** After a quick reply: say how it went in the same alert (Android keeps a spinner until we do). */
        fun settle(context: Context, chat: String, outcome: String?) {
            val nm = NotificationManagerCompat.from(context)
            val id = QuickReply.notificationId(chat)
            if (outcome == null || !allowed(context)) { nm.cancel(id); return }
            val sent = outcome.startsWith("Sent")
            val name = (context.applicationContext as App).repo.store.cachedChats()?.firstOrNull { it.key == chat }?.name ?: "Messenger"
            val b = base(context).setContentTitle(name).setContentText(outcome).setContentIntent(openChat(context, chat, id)).setGroup(GROUP)
            if (sent) b.setTimeoutAfter(4000)
            nm.notify(id, b.build())
        }
    }
}
