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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.RemoteInput
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
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
    var replies by mutableStateOf<List<String>>(emptyList())
    var outgoing by mutableStateOf<List<Outgoing>>(emptyList())

    fun take(item: DataItem, ctx: Context, stamp: Boolean) {
        val data = item.data
        val path = item.uri.path ?: return
        when {
            path == WearReplies.REPLIES && data != null -> replies = WearReplies.parseReplies(String(data))
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
    DisposableEffect(Unit) {
        val mc = runCatching { Wearable.getMessageClient(ctx) }.getOrNull()
        val ml = MessageClient.OnMessageReceivedListener { e: MessageEvent ->
            if (e.path == WearReplies.ACK) WearReplies.parseAck(e.data)?.let { st.outgoing = WearReplies.settle(st.outgoing, it) }
        }
        runCatching { mc?.addListener(ml) }
        onDispose { runCatching { mc?.removeListener(ml) } }
    }
    val scope = rememberCoroutineScope()
    // Sends a reply to the phone and waits 10 s for its ack; no ack = "saved, sends when your phone is online".
    val send: (String, String) -> Unit = { key, text ->
        val id = java.util.UUID.randomUUID().toString()
        st.outgoing = st.outgoing + Outgoing(id, key, text)
        scope.launch {
            runCatching {
                val nodes = Wearable.getNodeClient(ctx).connectedNodes.await()
                nodes.forEach { Wearable.getMessageClient(ctx).sendMessage(it.id, WearReplies.REPLY, WearReplies.request(key, text, id)).await() }
            }
            delay(WearReplies.ACK_WAIT_MS)
            st.outgoing = WearReplies.timeout(st.outgoing, id)
        }
    }
    val nav = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(nav, startDestination = "list") {
        composable("list") {
            ChatList(st, debug, onOpen = { nav.navigate("chat/" + Uri.encode(it.key)) })
        }
        composable("chat/{key}", arguments = listOf(navArgument("key") { type = NavType.StringType })) { e ->
            val key = e.arguments?.getString("key") ?: ""
            val chat = st.chats.firstOrNull { it.key == key }
            ChatScreen(chat?.name ?: "", st.lines[WearChats.path(key)], st.outgoing.filter { it.key == key }, st.locked, onReply = { nav.navigate("reply/" + Uri.encode(key)) })
        }
        composable("reply/{key}", arguments = listOf(navArgument("key") { type = NavType.StringType })) { e ->
            val key = e.arguments?.getString("key") ?: ""
            ReplyScreen(st.replies, st.locked, onSend = { text -> send(key, text); nav.popBackStack() })
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

/** Round screens cut the sides: keep the column inside by about 12% of the width each side (AII-414). */
@Composable
private fun roundInset(): PaddingValues {
    val w = LocalConfiguration.current.screenWidthDp
    return PaddingValues(horizontal = (w * 0.12f).dp, vertical = 24.dp)
}

@Composable
private fun ChatScreen(name: String, lines: List<WearLine>?, outgoing: List<Outgoing>, locked: Boolean, onReply: () -> Unit) {
    if (lines == null) {
        Centered { Text(stringResource(R.string.not_yet), textAlign = TextAlign.Center) }
        return
    }
    val state = rememberScalingLazyListState()
    // Newest at the bottom, and that is where it opens.
    LaunchedEffect(lines.size, outgoing.size) { state.scrollToItem(lines.size + outgoing.size) }
    ScalingLazyColumn(Modifier.fillMaxSize(), state = state, horizontalAlignment = Alignment.CenterHorizontally, contentPadding = roundInset()) {
        item { Text(name, style = MaterialTheme.typography.title3, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 6.dp)) }
        items(lines.size) { i ->
            val l = lines[i]
            Bubble(l.mine, l.who, l.text, WearChats.clock(l.time), null)
        }
        items(outgoing.size) { i ->
            val o = outgoing[i]
            Bubble(true, "", o.text, "", o.status)
        }
        item {
            Chip(onClick = onReply, enabled = !locked, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), colors = ChipDefaults.primaryChipColors(),
                label = { Text(stringResource(R.string.reply), maxLines = 1) })
        }
    }
}

@Composable
private fun Bubble(mine: Boolean, who: String, text: String, time: String, status: ReplyStatus?) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier.widthIn(max = 150.dp)
                .background(if (mine) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.surface, RoundedCornerShape(14.dp))
                .padding(8.dp),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        ) {
            if (!mine && who.isNotBlank()) Text(who, style = MaterialTheme.typography.caption2, color = MaterialTheme.colors.primary, maxLines = 1)
            Text(text, style = MaterialTheme.typography.body2)
            if (time.isNotEmpty()) Text(time, style = MaterialTheme.typography.caption3)
            if (status != null) Text(statusText(status), style = MaterialTheme.typography.caption3, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun statusText(s: ReplyStatus): String = when (s) {
    ReplyStatus.SENDING -> stringResource(R.string.sending)
    ReplyStatus.SENT -> "✓ " + stringResource(R.string.sent)
    ReplyStatus.SAVED -> stringResource(R.string.saved_offline)
    ReplyStatus.LOCKED -> stringResource(R.string.locked)
    ReplyStatus.NO_CHAT -> stringResource(R.string.no_chat)
    ReplyStatus.FAILED -> stringResource(R.string.send_failed)
}

/** Quick replies from the phone, a Speak chip (voice or keyboard through the watch's input screen) and a few emoji. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReplyScreen(replies: List<String>, locked: Boolean, onSend: (String) -> Unit) {
    if (locked) {
        Centered { Text(stringResource(R.string.locked), textAlign = TextAlign.Center) }
        return
    }
    val label = stringResource(R.string.speak_label)
    val speak = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val data = r.data ?: return@rememberLauncherForActivityResult
        RemoteInput.getResultsFromIntent(data)?.getCharSequence(REPLY_INPUT)?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(onSend)
    }
    ScalingLazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = roundInset()) {
        item {
            Chip(
                onClick = {
                    // The same screen androidx.wear.input's RemoteInputIntentHelper opens: voice, keyboard, handwriting.
                    val input = RemoteInput.Builder(REPLY_INPUT).setLabel(label).build()
                    val i = Intent("android.support.wearable.input.action.REMOTE_INPUT")
                        .putExtra("android.support.wearable.input.extra.INPUTS_LIST", arrayOf(input))
                    runCatching { speak.launch(i) }
                },
                modifier = Modifier.fillMaxWidth(), colors = ChipDefaults.primaryChipColors(),
                label = { Text(stringResource(R.string.speak), maxLines = 1) },
            )
        }
        items(replies.size) { i ->
            Chip(onClick = { onSend(replies[i]) }, modifier = Modifier.fillMaxWidth(), colors = ChipDefaults.secondaryChipColors(),
                label = { Text(replies[i], maxLines = 2, overflow = TextOverflow.Ellipsis) })
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), Arrangement.SpaceEvenly) {
                WearReplies.EMOJI.forEach { e ->
                    Text(e, Modifier.combinedClickable(onClick = { onSend(e) }).padding(4.dp), style = MaterialTheme.typography.title3)
                }
            }
        }
    }
}

private const val REPLY_INPUT = "reply"

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) { content() }
}
