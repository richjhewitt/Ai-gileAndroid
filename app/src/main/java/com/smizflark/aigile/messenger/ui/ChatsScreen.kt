package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.News
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.work.CheckWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Journey 2: every chat in one list. Shows the last list at once, then fetches. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(repo: Repo, onOpen: (ChatRef) -> Unit, onNew: () -> Unit, onSettings: () -> Unit) {
    var chats by remember { mutableStateOf(repo.store.cachedChats() ?: emptyList()) }
    var seen by remember { mutableStateOf(repo.store.seen()) }
    fun counts() = chats.associate { it.key to News.unread(it, seen[it.key], repo.store.cachedConversation(it.key)) }
    var unread by remember { mutableStateOf(counts()) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(chats.isNotEmpty()) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        try { chats = repo.chats(); error = null }
        catch (e: Exception) { error = e.message }
        seen = repo.store.seen(); unread = counts(); loaded = true
    }

    val context = LocalContext.current
    /** Mark as read (AII-25): one chat from its menu, or all from the top bar. */
    fun markRead(l: List<ChatRef>) = scope.launch {
        l.forEach { repo.markRead(it) }
        seen = repo.store.seen(); unread = counts()
        if (chats.none { News.fresh(it, seen[it.key]) }) CheckWorker.clearAlert(context)
    }
    val total = unread.values.sum()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { load(); delay(20_000) }   // calm: every 20 s while you look, never in the background
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((if (repo.store.demo) "Messages · demo" else "Messages") + if (total > 0) " ($total)" else "") },
                actions = {
                    if (chats.any { News.fresh(it, seen[it.key]) })
                        IconButton(onClick = { markRead(chats) }, Modifier.testTag("readAll")) { Icon(Icons.Default.DoneAll, "Mark all as read") }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNew, icon = { Icon(Icons.AutoMirrored.Filled.Chat, null) },
                text = { Text("New message") }, modifier = Modifier.testTag("new"))
        },
    ) { pad ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
            modifier = Modifier.fillMaxSize().padding(pad),
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                error?.let { e ->
                    item {
                        Text(
                            if (chats.isEmpty()) e else "$e\nShowing chats from ${shortTime(java.time.Instant.ofEpochMilli(repo.store.lastSync).toString())}.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp)).padding(12.dp),
                        )
                    }
                }
                if (loaded && chats.isEmpty() && error == null) item {
                    Text("No chats yet. Tap New message to write to a team.", Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(chats, key = { it.key }) { c ->
                    ChatRow(c, repo, News.fresh(c, seen[c.key]), unread[c.key] ?: 0, onRead = { markRead(listOf(c)) }) { onOpen(c) }
                    HorizontalDivider(Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
                item { Spacer(Modifier.size(88.dp)) }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ChatRow(c: ChatRef, repo: Repo, fresh: Boolean, unread: Int, onRead: () -> Unit, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { menu = true }, onLongClickLabel = "Options")
            .padding(horizontal = 16.dp, vertical = 12.dp).testTag("chat:${c.key}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Mark as read") }, enabled = fresh, onClick = { menu = false; onRead() })
        }
        Avatar(c, repo)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (fresh) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(shortTime(c.last), style = MaterialTheme.typography.labelMedium,
                    color = if (fresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    c.subtitle,
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (c.waiting > 0) Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                    Text("${c.waiting} waiting", Modifier.padding(horizontal = 4.dp))
                }
                if (fresh && unread > 0) Badge(containerColor = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("unread:${c.key}")) {
                    Text(if (unread > 99) "99+" else "$unread", Modifier.padding(horizontal = 2.dp))
                } else if (fresh && c.waiting == 0) Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
        }
    }
}
