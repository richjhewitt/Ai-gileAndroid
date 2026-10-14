package com.smizflark.aigile.messenger.widget

import com.smizflark.aigile.messenger.ui.Words

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.smizflark.aigile.messenger.MainActivity
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.News
import com.smizflark.aigile.messenger.data.Store
import com.smizflark.aigile.messenger.work.CheckWorker
import java.time.Duration
import java.time.Instant

/* ---------- rows: what the widget shows, worked out without Android so it can be tested ---------- */

object WidgetRows {
    data class Row(val chat: String, val title: String, val subtitle: String, val ago: String, val new: Boolean, val badge: String = "")

    /** The newest chats first, at most [max]. A chat is new when its last message is newer than the one you saw. */
    fun rows(chats: List<ChatRef>, seen: Map<String, String>, now: Instant, max: Int = 4,
             unread: Map<String, Int> = emptyMap(), muted: Set<String> = emptySet()): List<Row> =
        chats.sortedByDescending { it.last ?: "" }.take(max).map { c ->
            Row(
                chat = c.key,
                title = "${c.emoji} ${c.name}".trim(),
                subtitle = com.smizflark.aigile.messenger.ui.previewLabel(c),   // newest message, as on the chats list (AII-106)
                ago = ago(c.last, now),
                new = c.last != null && c.last > (seen[c.key] ?: ""),
                badge = if (c.key in muted || !com.smizflark.aigile.messenger.data.News.fresh(c, seen[c.key])) "" else badge(unread[c.key] ?: 0),
            )
        }

    /** The unread badge text, as on the chats list: nothing for 0, "99+" over 99 (AII-218). */
    fun badge(n: Int): String = if (n <= 0) "" else if (n > 99) "99+" else "$n"

    /** "now", "5m", "3h", "2d": short enough for a widget. */
    fun ago(iso: String?, now: Instant): String {
        val t = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
        val m = Duration.between(t, now).toMinutes().coerceAtLeast(0)
        return when {
            m < 1 -> Words.now
            m < 60 -> Words.minutes.format(m)
            m < 60 * 24 -> Words.hours.format(m / 60)
            else -> Words.days.format(m / (60 * 24))
        }
    }
}

/* ---------- widget: latest chats on the home screen, from the phone's cache (no network here) ---------- */

class LatestWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = render(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        private val ROWS = listOf(
            intArrayOf(R.id.row0, R.id.title0, R.id.sub0, R.id.ago0, R.id.dot0, R.id.badge0),
            intArrayOf(R.id.row1, R.id.title1, R.id.sub1, R.id.ago1, R.id.dot1, R.id.badge1),
            intArrayOf(R.id.row2, R.id.title2, R.id.sub2, R.id.ago2, R.id.dot2, R.id.badge2),
            intArrayOf(R.id.row3, R.id.title3, R.id.sub3, R.id.ago3, R.id.dot3, R.id.badge3),
        )

        /** Redraws every widget on the home screen. Called when the chat list is fetched or you sign out. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LatestWidget::class.java))
            if (ids.isEmpty()) return
            val views = render(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun open(context: Context, code: Int, chat: String?): PendingIntent {
            val i = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (chat != null) putExtra(CheckWorker.EXTRA_CHAT, chat)
            }
            return PendingIntent.getActivity(context, 100 + code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun render(context: Context): RemoteViews {
            val store = Store(context)
            val chats = store.cachedChats().orEmpty()
            val seen = store.seen()
            val unread = chats.associate { it.key to News.unread(it, seen[it.key], store.cachedConversation(it.key)) }
            val rows = if (store.connected) WidgetRows.rows(chats, seen, Instant.now(), unread = unread, muted = store.muted()) else emptyList()
            val v = RemoteViews(context.packageName, R.layout.widget_latest)
            v.setOnClickPendingIntent(R.id.header, open(context, 0, null))
            v.setViewVisibility(R.id.empty, if (rows.isEmpty()) View.VISIBLE else View.GONE)
            v.setTextViewText(R.id.empty, context.getString(if (store.connected) R.string.widget_no_chats else R.string.widget_connect))
            v.setOnClickPendingIntent(R.id.empty, open(context, 0, null))
            ROWS.forEachIndexed { n, ids ->
                val (row, title, sub, ago, dot) = ids; val badge = ids[5]
                val r = rows.getOrNull(n)
                v.setViewVisibility(row, if (r == null) View.GONE else View.VISIBLE)
                if (r == null) return@forEachIndexed
                v.setTextViewText(title, r.title)
                v.setTextViewText(sub, r.subtitle)
                v.setTextViewText(ago, r.ago)
                v.setViewVisibility(dot, if (r.new) View.VISIBLE else View.GONE)
                v.setViewVisibility(badge, if (r.badge.isEmpty()) View.GONE else View.VISIBLE)
                v.setTextViewText(badge, r.badge)
                v.setOnClickPendingIntent(row, open(context, n + 1, r.chat))
            }
            return v
        }
    }
}
