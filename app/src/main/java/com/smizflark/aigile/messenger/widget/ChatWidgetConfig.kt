package com.smizflark.aigile.messenger.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Store
import com.smizflark.aigile.messenger.ui.MessengerTheme

/** Opens when a One chat widget is added: pick the chat from the cached list (no network, AII-380). */
class ChatWidgetConfig : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(Activity.RESULT_CANCELED)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val store = Store(this)
        val chats = if (store.connected) store.cachedChats().orEmpty().sortedByDescending { it.last ?: "" } else emptyList()
        setContent {
            MessengerTheme(store) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.statusBarsPadding()) {
                        Text(stringResource(R.string.widget_pick_chat), Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                        if (chats.isEmpty()) Text(stringResource(if (store.connected) R.string.widget_no_chats else R.string.widget_connect), Modifier.padding(16.dp))
                        LazyColumn {
                            items(chats, key = { it.key }) { c ->
                                Text("${c.emoji} ${c.name}".trim(),
                                    Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { pick(id, c.key) }.padding(horizontal = 16.dp, vertical = 14.dp),
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun pick(id: Int, chat: String) {
        ChatWidgetPicks.save(this, ChatWidgetLogic.with(ChatWidgetPicks.load(this), id, chat))
        AppWidgetManager.getInstance(this).updateAppWidget(id, ChatWidget.render(this, id))
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        finish()
    }
}
