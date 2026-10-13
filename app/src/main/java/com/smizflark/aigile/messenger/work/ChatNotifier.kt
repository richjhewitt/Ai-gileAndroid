package com.smizflark.aigile.messenger.work

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.smizflark.aigile.messenger.MainActivity
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.News
import com.smizflark.aigile.messenger.data.QuickReply
import java.time.Instant

/**
 * One notification per chat (AII-65): the newest lines stack up in a MessagingStyle, a summary groups the chats,
 * and opening or reading a chat takes its notification away. The lines live in the posted notification itself.
 */
object ChatNotifier {
    private const val GROUP = "messages"
    const val SUMMARY = 1
    private const val MAX_LINES = 8

    private fun base(context: Context) = NotificationCompat.Builder(context, CheckWorker.CHANNEL)
        .setSmallIcon(R.drawable.ic_notify)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)

    fun openChat(context: Context, chat: String?, code: Int): PendingIntent {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (chat != null) putExtra(CheckWorker.EXTRA_CHAT, chat)
        }
        return PendingIntent.getActivity(context, code, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun active(context: Context, id: Int): Notification? =
        context.getSystemService(NotificationManager::class.java).activeNotifications.firstOrNull { it.id == id }?.notification

    private fun styleOf(n: Notification?) =
        n?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }

    private fun chatCount(context: Context) = context.getSystemService(NotificationManager::class.java)
        .activeNotifications.count { it.id != SUMMARY && it.notification.group == GROUP && it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }

    /** A chat's notification with the lines of [style], and its Reply button. */
    private fun build(context: Context, chat: String, name: String, style: NotificationCompat.MessagingStyle): NotificationCompat.Builder {
        val id = QuickReply.notificationId(chat)
        val reply = Intent(context, ReplyReceiver::class.java).setAction(ReplyReceiver.ACTION).putExtra(CheckWorker.EXTRA_CHAT, chat)
        val replyPi = PendingIntent.getBroadcast(context, id, reply,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0))
        val action = NotificationCompat.Action.Builder(R.drawable.ic_notify, "Reply", replyPi)
            .addRemoteInput(RemoteInput.Builder(ReplyReceiver.KEY_TEXT).setLabel("Reply to $name").build())
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
        return base(context).setContentTitle(name).setStyle(style)
            .setContentIntent(openChat(context, chat, id)).addAction(action).setGroup(GROUP)
    }

    private fun newStyle(name: String) = NotificationCompat.MessagingStyle(Person.Builder().setName("Me").build())
        .setConversationTitle(name)

    private fun addLine(style: NotificationCompat.MessagingStyle, text: String, at: Long, from: Person?) {
        style.addMessage(text, at, from)
    }

    private fun trimmed(style: NotificationCompat.MessagingStyle, name: String): NotificationCompat.MessagingStyle {
        if (style.messages.size <= MAX_LINES) return style
        val s = newStyle(name)
        style.messages.takeLast(MAX_LINES).forEach { s.addMessage(it) }
        return s
    }

    /** New message(s): add the line to this chat's notification (or start it), then refresh the summary. */
    fun post(context: Context, item: News.Item) {
        val nm = NotificationManagerCompat.from(context)
        val id = QuickReply.notificationId(item.chat)
        val sender = Person.Builder().setName(item.name).build()
        val style = styleOf(active(context, id)) ?: newStyle(item.name)
        val at = item.latestFromThem?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: System.currentTimeMillis()
        addLine(style, item.preview ?: "New message", at, sender)
        nm.notify(id, build(context, item.chat, item.name, trimmed(style, item.name)).build())
        refreshSummary(context)
    }

    /** After a sent quick reply: the line you sent joins the stack, and the spinner goes. */
    fun sent(context: Context, chat: String, name: String, text: String) {
        val id = QuickReply.notificationId(chat)
        val style = styleOf(active(context, id)) ?: newStyle(name)
        addLine(style, text, System.currentTimeMillis(), null)
        NotificationManagerCompat.from(context).notify(id, build(context, chat, name, trimmed(style, name)).build())
        refreshSummary(context)
    }

    /** The chat was opened or read: its notification goes. */
    fun clear(context: Context, chat: String) {
        NotificationManagerCompat.from(context).cancel(QuickReply.notificationId(chat))
        refreshSummary(context)
    }

    /** The summary shows only while two or more chats have a notification. */
    fun refreshSummary(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        val n = chatCount(context)
        if (n < 2) { nm.cancel(SUMMARY); return }
        nm.notify(SUMMARY, base(context).setContentTitle("$n chats have new messages")
            .setStyle(NotificationCompat.InboxStyle().setSummaryText("$n chats"))
            .setContentIntent(openChat(context, null, 0)).setGroup(GROUP).setGroupSummary(true).build())
    }
}
