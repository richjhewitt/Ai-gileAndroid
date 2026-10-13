package com.smizflark.aigile.messenger

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.smizflark.aigile.messenger.data.News
import com.smizflark.aigile.messenger.data.QuickReply
import com.smizflark.aigile.messenger.work.ChatNotifier
import com.smizflark.aigile.messenger.work.CheckWorker
import com.smizflark.aigile.messenger.work.ReadReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AII-65: one notification per chat with stacked lines, a summary for several, cleared when the chat is opened. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatNotifyTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val nm get() = context.getSystemService(NotificationManager::class.java)

    @Before fun fresh() { nm.cancelAll() }

    private fun item(chat: String, name: String, text: String) = News.Item(chat, name, "2026-10-13T10:00:00Z", text)
    private fun chats() = nm.activeNotifications.filter { it.id != ChatNotifier.SUMMARY }
    private fun lines(chat: String): List<String> {
        val n = nm.activeNotifications.first { it.id == QuickReply.notificationId(chat) }.notification
        return NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)!!.messages.map { it.text.toString() }
    }

    @Test fun twoMessagesInOneChatGiveOneNotificationWithTwoLines() {
        ChatNotifier.post(context, item("lead:a", "AI-gile", "first"))
        ChatNotifier.post(context, item("lead:a", "AI-gile", "second"))
        assertEquals(1, chats().size)
        assertEquals(listOf("first", "second"), lines("lead:a"))
        assertNull(nm.activeNotifications.firstOrNull { it.id == ChatNotifier.SUMMARY })
    }

    @Test fun twoChatsGiveTwoNotificationsAndASummary() {
        ChatNotifier.post(context, item("lead:a", "AI-gile", "hi"))
        ChatNotifier.post(context, item("lead:b", "Tunio", "yo"))
        assertEquals(2, chats().size)
        val summary = nm.activeNotifications.firstOrNull { it.id == ChatNotifier.SUMMARY }
        assertNotNull(summary)
        assertTrue(summary!!.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
    }

    @Test fun openingAChatClearsItAndTheSummaryWithOneLeft() {
        ChatNotifier.post(context, item("lead:a", "AI-gile", "hi"))
        ChatNotifier.post(context, item("lead:b", "Tunio", "yo"))
        ChatNotifier.clear(context, "lead:a")
        assertEquals(listOf(QuickReply.notificationId("lead:b")), chats().map { it.id })
        assertNull(nm.activeNotifications.firstOrNull { it.id == ChatNotifier.SUMMARY })
        ChatNotifier.clear(context, "lead:b")
        assertEquals(0, nm.activeNotifications.size)
    }

    // AII-116
    @Test fun anAlertHasReplyAndMarkAsRead() {
        ChatNotifier.post(context, item("lead:a", "AI-gile", "hi"))
        val n = nm.activeNotifications.first { it.id == QuickReply.notificationId("lead:a") }.notification
        assertEquals(listOf("Reply", "Mark as read"), n.actions.map { it.title.toString() })
        assertEquals(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ, NotificationCompat.getAction(n, 1)!!.semanticAction)
    }

    @Test fun markAsReadTakesTheAlertAway() {
        ChatNotifier.post(context, item("lead:a", "AI-gile", "hi"))
        context.sendBroadcast(android.content.Intent(context, ReadReceiver::class.java)
            .setAction(ReadReceiver.ACTION).putExtra(CheckWorker.EXTRA_CHAT, "lead:a"))
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(0, nm.activeNotifications.size)
    }

    @Test fun aSentQuickReplyAddsItsLine() {
        ChatNotifier.post(context, item("lead:a", "AI-gile", "question"))
        ChatNotifier.sent(context, "lead:a", "AI-gile", "my answer")
        assertEquals(listOf("question", "my answer"), lines("lead:a"))
    }
}
