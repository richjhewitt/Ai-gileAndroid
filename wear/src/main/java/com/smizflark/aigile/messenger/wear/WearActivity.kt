package com.smizflark.aigile.messenger.wear

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WearApp() } }
    }
}

/** What the watch shows: the last DataItems the phone sent (the Data Layer keeps them, so offline works). */
private class WearState {
    var chats by mutableStateOf<List<WearChat>>(emptyList())
    var locked by mutableStateOf(false)
    var lines by mutableStateOf<Map<String, List<WearLine>>>(emptyMap())   // by Data Layer path
    var updated by mutableStateOf<Long?>(null)

    fun take(item: DataItem, ctx: Context, stamp: Boolean) {
        val data = item.data
        val path = item.uri.path ?: return
        when {
            path == WearChats.LOCKED -> locked = WearChats.parseLocked(data)
            path == WearChats.PATH && data != null -> {
                chats = WearChats.parse(String(data))
                if (stamp) ctx.getSharedPreferences("wear", Context.MODE_PRIVATE).edit().putLong("updated", System.currentTimeMillis()).apply()
                updated = ctx.getSharedPreferences("wear", Context.MODE_PRIVATE).getLong("updated", 0L).takeIf { it > 0 }
            }
            path.startsWith(WearChats.CHAT) && data != null -> lines = lines + (path to WearChats.parseLines(String(data)))
        }
    }

    fun drop(path: String) {
        when {
            path == WearChats.PATH -> chats = emptyList()
            path.startsWith(WearChats.CHAT) -> lines = lines - path
        }
    }
}

@Composable
private fun WearApp() {
    val ctx = LocalContext.current
    val st = remember { WearState() }
    val debug = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    LaunchedEffect(Unit) {
        runCatching {
            val buf = Wearable.getDataClient(ctx).dataItems.await()
            try { buf.forEach { st.take(it, ctx, stamp = false) } } finally { buf.release() }
        }
    }
    DisposableEffect(Unit) {
        val client = runCatching { Wearable.getDataClient(ctx) }.getOrNull()
        val l = DataClient.OnDataChangedListener { events: DataEventBuffer ->
            events.forEach { e ->
                if (e.type == com.google.android.gms.wearable.DataEvent.TYPE_DELETED) e.dataItem.uri.path?.let { st.drop(it) }
                else st.take(e.dataItem, ctx, stamp = true)
            }
        }
        runCatching { client?.addListener(l) }
        onDispose { runCatching { client?.removeListener(l) } }
    }
    val nav = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(nav, startDestination = "list") {
        composable("list") {
            ChatList(st, debug, onOpen = { nav.navigate("chat/" + Uri.encode(it.key)) })
        }
        composable("chat/{key}", arguments = listOf(navArgument("key") { type = NavType.StringType })) { e ->
            val key = e.arguments?.getString("key") ?: ""
            val chat = st.chats.firstOrNull { it.key == key }
            ChatScreen(chat?.name ?: "", st.lines[WearChats.path(key)])
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatList(st: WearState, debug: Boolean, onOpen: (WearChat) -> Unit) {
    if (st.locked) {
        Centered { Text(stringResource(R.string.locked), textAlign = TextAlign.Center) }
        return
    }
    if (st.chats.isEmpty()) {
        Centered {
            // Debug builds only: a long press loads made-up chats so the screens can be seen without a phone.
            val mod = if (debug) Modifier.combinedClickable(onClick = {}, onLongClick = {
                st.chats = WearChats.demoChats()
                st.lines = WearChats.demoChats().mapNotNull { c -> WearChats.demoLines(c.key)?.let { WearChats.path(c.key) to it } }.toMap()
                st.updated = System.currentTimeMillis()
            }) else Modifier
            Text(stringResource(R.string.waiting), textAlign = TextAlign.Center, modifier = mod)
        }
        return
    }
    val chats = WearChats.newestFirst(st.chats)
    ScalingLazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        items(chats, key = { it.key }) { c ->
            Chip(
                onClick = { onOpen(c) },
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(c.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                secondaryLabel = if (c.last.isNotBlank()) ({ Text(c.last, maxLines = 1, overflow = TextOverflow.Ellipsis) }) else null,
                icon = if (c.unread) ({ Box(Modifier.padding(2.dp).background(MaterialTheme.colors.primary, CircleShape).padding(5.dp)) }) else null,
            )
        }
        st.updated?.let { t ->
            item {
                Text(stringResource(R.string.last_updated, WearChats.clock(java.time.Instant.ofEpochMilli(t).toString())),
                    style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
            }
        }
    }
}

@Composable
private fun ChatScreen(name: String, lines: List<WearLine>?) {
    if (lines == null) {
        Centered { Text(stringResource(R.string.not_yet), textAlign = TextAlign.Center) }
        return
    }
    val state = rememberScalingLazyListState()
    // Newest at the bottom, and that is where it opens.
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) state.scrollToItem(lines.size) }
    ScalingLazyColumn(Modifier.fillMaxSize(), state = state, horizontalAlignment = Alignment.CenterHorizontally) {
        item { Text(name, style = MaterialTheme.typography.title3, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 6.dp)) }
        items(lines.size) { i ->
            val l = lines[i]
            Box(Modifier.fillMaxWidth(), contentAlignment = if (l.mine) Alignment.CenterEnd else Alignment.CenterStart) {
                Column(
                    Modifier.fillMaxWidth(0.85f)
                        .background(if (l.mine) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.surface, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    horizontalAlignment = if (l.mine) Alignment.End else Alignment.Start,
                ) {
                    if (!l.mine && l.who.isNotBlank()) Text(l.who, style = MaterialTheme.typography.caption2, color = MaterialTheme.colors.primary, maxLines = 1)
                    Text(l.text, style = MaterialTheme.typography.body2)
                    Text(WearChats.clock(l.time), style = MaterialTheme.typography.caption3)
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) { content() }
}
