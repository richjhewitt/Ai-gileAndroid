package com.smizflark.aigile.messenger.work

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.smizflark.aigile.messenger.data.Reminder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.smizflark.aigile.messenger.MainActivity
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.News
import com.smizflark.aigile.messenger.data.QuickReply
import com.smizflark.aigile.messenger.data.Replies
import com.smizflark.aigile.messenger.data.Store
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

    fun openChat(context: Context, chat: String?, code: Int, msg: String? = null): PendingIntent {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (chat != null) putExtra(CheckWorker.EXTRA_CHAT, chat)
            if (msg != null) putExtra(CheckWorker.EXTRA_MSG, msg)
        }
        return PendingIntent.getActivity(context, code, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun active(context: Context, id: Int): Notification? =
        context.getSystemService(NotificationManager::class.java).activeNotifications.firstOrNull { it.id == id }?.notification

    private fun styleOf(n: Notification?) =
        n?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }

    private fun chatCount(context: Context) = context.getSystemService(NotificationManager::class.java)
        .activeNotifications.count { it.id != SUMMARY && it.notification.group == GROUP && it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }

    /** AII-181: your quick replies as one-tap chips on the alert, or none when they are switched off. */
    fun choices(context: Context): Array<CharSequence>? {
        val store = Store(context)
        if (!store.quickReplies) return null
        return Replies.parse(store.quickReplyText).ifEmpty { context.resources.getStringArray(R.array.quick_replies_list).toList() }.toTypedArray()
    }

    /** A chat's notification with the lines of [style], and its Reply button. */
    private fun build(context: Context, chat: String, name: String, style: NotificationCompat.MessagingStyle, msg: String? = null, text: String? = null): NotificationCompat.Builder {
        val id = QuickReply.notificationId(chat)
        val reply = Intent(context, ReplyReceiver::class.java).setAction(ReplyReceiver.ACTION).putExtra(CheckWorker.EXTRA_CHAT, chat)
        val replyPi = PendingIntent.getBroadcast(context, id, reply,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0))
        val action = NotificationCompat.Action.Builder(R.drawable.ic_notify, context.getString(R.string.reply), replyPi)
            .addRemoteInput(RemoteInput.Builder(ReplyReceiver.KEY_TEXT).setLabel(context.getString(R.string.notify_reply_to, name))
                .setChoices(choices(context)).build())
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
        // AII-116: Mark as read, without opening the app.
        val read = Intent(context, ReadReceiver::class.java).setAction(ReadReceiver.ACTION).putExtra(CheckWorker.EXTRA_CHAT, chat)
        val readPi = PendingIntent.getBroadcast(context, id, read, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val readAction = NotificationCompat.Action.Builder(R.drawable.ic_notify, context.getString(R.string.mark_read), readPi)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
        // AII-207: Silent alerts for this chat: shown, but no sound and no buzz (Mute never gets here).
        val quiet = Store(context).let { com.smizflark.aigile.messenger.data.Silent.quiet(it.silent, it.muted(), chat) }
        val b = base(context).setContentTitle(name).setStyle(style).setSilent(quiet)
            .setContentIntent(openChat(context, chat, id)).addAction(action).addAction(readAction).setGroup(GROUP)
        // AII-210: Remind me in 1 hour, for the newest message in this alert (kept in the alert's extras so later lines keep it).
        if (msg != null && text != null) {
            val remind = Intent(context, RemindReceiver::class.java).setAction(RemindReceiver.ACTION).putExtra(CheckWorker.EXTRA_CHAT, chat)
                .putExtra(RemindReceiver.EXTRA_NAME, name).putExtra(CheckWorker.EXTRA_MSG, msg).putExtra(RemindReceiver.EXTRA_TEXT, text)
            val remindPi = PendingIntent.getBroadcast(context, id, remind, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            b.addAction(NotificationCompat.Action.Builder(R.drawable.ic_notify, context.getString(R.string.remind_in_hour), remindPi).setShowsUserInterface(false).build())
            b.addExtras(Bundle().apply { putString(RemindReceiver.EXTRA_MSG_KEY, msg); putString(RemindReceiver.EXTRA_TEXT, text) })
        }
        return b
    }

    private fun newStyle(context: Context, name: String) = NotificationCompat.MessagingStyle(Person.Builder().setName(context.getString(R.string.notify_me)).build())
        .setConversationTitle(name)

    private fun addLine(style: NotificationCompat.MessagingStyle, text: String, at: Long, from: Person?) {
        style.addMessage(text, at, from)
    }

    private fun trimmed(context: Context, style: NotificationCompat.MessagingStyle, name: String): NotificationCompat.MessagingStyle {
        if (style.messages.size <= MAX_LINES) return style
        val s = newStyle(context, name)
        style.messages.takeLast(MAX_LINES).forEach { s.addMessage(it) }
        return s
    }

    /** New message(s): add the line to this chat's notification (or start it), then refresh the summary. */
    fun post(context: Context, item: News.Item) {
        val nm = NotificationManagerCompat.from(context)
        val id = QuickReply.notificationId(item.chat)
        val sender = Person.Builder().setName(item.name).build()
        val prev = active(context, id)
        val style = styleOf(prev) ?: newStyle(context, item.name)
        val at = item.latestFromThem?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: System.currentTimeMillis()
        addLine(style, item.preview ?: context.getString(R.string.new_message), at, sender)
        val msg = item.latestKey ?: prev?.extras?.getString(RemindReceiver.EXTRA_MSG_KEY)
        val text = item.preview ?: prev?.extras?.getString(RemindReceiver.EXTRA_TEXT) ?: context.getString(R.string.new_message)
        nm.notify(id, build(context, item.chat, item.name, trimmed(context, style, item.name), msg, text).build())
        refreshSummary(context)
    }

    /** After a sent quick reply: the line you sent joins the stack, and the spinner goes. */
    fun sent(context: Context, chat: String, name: String, text: String) {
        val id = QuickReply.notificationId(chat)
        val prev = active(context, id)
        val style = styleOf(prev) ?: newStyle(context, name)
        addLine(style, text, System.currentTimeMillis(), null)
        NotificationManagerCompat.from(context).notify(id, build(context, chat, name, trimmed(context, style, name),
            prev?.extras?.getString(RemindReceiver.EXTRA_MSG_KEY), prev?.extras?.getString(RemindReceiver.EXTRA_TEXT)).build())
        refreshSummary(context)
    }

    /** The chat was opened or read: its notification goes. */
    fun clear(context: Context, chat: String) {
        NotificationManagerCompat.from(context).cancel(QuickReply.notificationId(chat))
        refreshSummary(context)
    }

    /** A waiting message gave up in the background (AII-151): say so; tapping opens its chat, where Retry and Delete are. */
    fun notSent(context: Context, chat: String, name: String, id: String, text: String) {
        val n = QuickReply.notificationId("notsent:$id")
        NotificationManagerCompat.from(context).notify(n, base(context).setContentTitle(name)
            .setContentText(context.getString(R.string.notify_gave_up, text)).setContentIntent(openChat(context, chat, n)).build())
    }

    /** A Send later message went by itself (AII-175): say so; tapping opens its chat. */
    fun sentLater(context: Context, chat: String, name: String, id: String, text: String) {
        val n = QuickReply.notificationId("sentlater:$id")
        NotificationManagerCompat.from(context).notify(n, base(context).setContentTitle(name)
            .setContentText(context.getString(R.string.notify_sent_later, text)).setContentIntent(openChat(context, chat, n)).build())
    }

    /** A reminder (AII-201) came due: show the message; tapping opens its chat at that message. */
    fun remind(context: Context, r: Reminder) {
        val n = QuickReply.notificationId("remind:${r.id}")
        // AII-224: Again in 1 hour reuses RemindReceiver; it dismisses this alert and sets the reminder 1 hour on.
        val again = Intent(context, RemindReceiver::class.java).setAction(RemindReceiver.ACTION).putExtra(CheckWorker.EXTRA_CHAT, r.chat)
            .putExtra(RemindReceiver.EXTRA_NAME, r.chatName).putExtra(CheckWorker.EXTRA_MSG, r.msg).putExtra(RemindReceiver.EXTRA_TEXT, r.text).putExtra(RemindReceiver.EXTRA_NOTE, r.note)
            .putExtra(RemindReceiver.EXTRA_NOTIFY, n)
        val againPi = PendingIntent.getBroadcast(context, n, again, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompat.from(context).notify(n, base(context).setContentTitle(context.getString(R.string.notify_remind, r.chatName))
            .setContentText(com.smizflark.aigile.messenger.data.Remind.withNote(r).replace('\n', ' '))
            .setStyle(NotificationCompat.BigTextStyle().bigText(com.smizflark.aigile.messenger.data.Remind.withNote(r)))
            .addAction(NotificationCompat.Action.Builder(R.drawable.ic_notify, context.getString(R.string.remind_again), againPi).setShowsUserInterface(false).build())
            .setContentIntent(openChat(context, r.chat, n, r.msg)).build())
    }

    /** The summary shows only while two or more chats have a notification. */
    fun refreshSummary(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        val n = chatCount(context)
        if (n < 2) { nm.cancel(SUMMARY); return }
        nm.notify(SUMMARY, base(context).setContentTitle(context.getString(R.string.notify_many, n))
            .setStyle(NotificationCompat.InboxStyle().setSummaryText(context.getString(R.string.notify_chats, n)))
            .setContentIntent(openChat(context, null, 0)).setGroup(GROUP).setGroupSummary(true).build())
    }
}
