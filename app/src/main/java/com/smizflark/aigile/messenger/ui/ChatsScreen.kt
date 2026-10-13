package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Badge
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Repo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Journey 2: every chat in one list. Shows the last list at once, then fetches. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(repo: Repo, onOpen: (ChatRef) -> Unit, onNew: () -> Unit, onSettings: () -> Unit) {
    var chats by remember { mutableStateOf(repo.store.cachedChats() ?: emptyList()) }
    var seen by remember { mutableStateOf(repo.store.seen()) }
    val muted = remember { repo.store.muted() }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(chats.isNotEmpty()) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        try { chats = repo.chats(); error = null }
        catch (e: Exception) { error = e.message }
        seen = repo.store.seen(); loaded = true
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { load(); delay(20_000) }   // calm: every 20 s while you look, never in the background
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (repo.store.demo) "Messages · demo" else "Messages") },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") } },
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
                    val fresh = c.last != null && c.last > (seen[c.key] ?: "")
                    ChatRow(c, repo, fresh, c.key in muted) { onOpen(c) }
                    HorizontalDivider(Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
                item { Spacer(Modifier.size(88.dp)) }
            }
        }
    }
}

@Composable
private fun ChatRow(c: ChatRef, repo: Repo, fresh: Boolean, muted: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp).testTag("chat:${c.key}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(c, repo)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (fresh) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
                    if (muted) Icon(Icons.Default.NotificationsOff, "Muted", Modifier.padding(horizontal = 4.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(shortTime(c.last), style = MaterialTheme.typography.labelMedium,
                    color = if (fresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    c.subtitle,
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (c.waiting > 0) Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text("${c.waiting} waiting", Modifier.padding(horizontal = 4.dp))
                } else if (fresh) Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
        }
    }
}
