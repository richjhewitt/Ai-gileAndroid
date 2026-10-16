package com.smizflark.aigile.messenger.widget

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
import com.smizflark.aigile.messenger.data.Store
import com.smizflark.aigile.messenger.ui.Look
import com.smizflark.aigile.messenger.work.CheckWorker

/** Home-screen widget: one chat you picked, its newest 3 messages, from the phone's cache (AII-380). */
class ChatWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, render(context, it)) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ChatWidgetPicks.save(context, ChatWidgetLogic.without(ChatWidgetPicks.load(context), ids.toList()))
    }

    companion object {
        private val LINES = intArrayOf(R.id.line0, R.id.line1, R.id.line2)

        /** Redraws every One chat widget; called from LatestWidget.refresh. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, ChatWidget::class.java)).forEach { manager.updateAppWidget(it, render(context, it)) }
        }

        fun render(context: Context, id: Int): RemoteViews {
            val store = Store(context)
            val key = ChatWidgetPicks.load(context)[id]
            val chat = key?.let { k -> store.cachedChats().orEmpty().firstOrNull { it.key == k } }
            val lines = if (store.connected && chat != null)
                ChatWidgetLogic.lines(store.cachedConversation(chat.key)?.bubbles.orEmpty(), context.getString(R.string.widget_you)) else emptyList()
            val v = RemoteViews(context.packageName, R.layout.widget_chat)
            val nightOn = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val c = WidgetLook.colours(Look.of(store.look), nightOn)
            v.setInt(R.id.root, "setBackgroundResource", intArrayOf(R.drawable.widget_bg_light, R.drawable.widget_bg_dark, R.drawable.widget_bg_black)[c.bg])
            v.setTextColor(R.id.header, c.brand)
            v.setTextColor(R.id.empty, c.sub)
            v.setTextViewText(R.id.header, chat?.let { "${it.emoji} ${it.name}".trim() } ?: context.getString(R.string.app_name))
            val msg = when { !store.connected -> R.string.widget_connect; chat == null -> R.string.widget_chat_gone; else -> R.string.widget_no_messages }
            v.setTextViewText(R.id.empty, context.getString(msg))
            v.setViewVisibility(R.id.empty, if (lines.isEmpty()) View.VISIBLE else View.GONE)
            LINES.forEachIndexed { n, line ->
                val t = lines.getOrNull(n)
                v.setViewVisibility(line, if (t == null) View.GONE else View.VISIBLE)
                if (t != null) { v.setTextViewText(line, t); v.setTextColor(line, c.title) }
            }
            val i = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (chat != null) putExtra(CheckWorker.EXTRA_CHAT, chat.key)
            }
            // Same route as alerts and HomeShortcut; one request code per widget so each keeps its own chat.
            v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(context, 1000 + id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            return v
        }
    }
}

/** Which chat each widget shows, per appWidgetId. */
object ChatWidgetPicks {
    private const val PREFS = "chat_widgets"
    private const val KEY = "picks"
    fun load(context: Context): Map<Int, String> =
        ChatWidgetLogic.decode(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY, emptySet()).orEmpty())
    fun save(context: Context, map: Map<Int, String>) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(KEY, ChatWidgetLogic.encode(map)).apply()
}
