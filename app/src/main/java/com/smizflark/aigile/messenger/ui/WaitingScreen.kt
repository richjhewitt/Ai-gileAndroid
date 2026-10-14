package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Outbox
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.Repo
import kotlinx.coroutines.launch

/** Every Send later message from all chats, soonest first (AII-166, from the Scheduled chip); tap one and its chat opens at it, hold it to Send now or Delete (AII-173). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitingScreen(repo: Repo, onOpen: (ChatRef, String) -> Unit, onBack: () -> Unit) {
    val chats = remember { repo.store.cachedChats() ?: emptyList() }
    var waiting by remember { mutableStateOf(Outbox.waiting(repo.store.outbox(), chats, Now.instant())) }
    val scope = rememberCoroutineScope()
    fun reload() { waiting = Outbox.waiting(repo.store.outbox(), chats, Now.instant()) }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                title = { Text(stringResource(R.string.waiting_title)) },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (waiting.isEmpty()) EmptyState(stringResource(R.string.empty_scheduled))
            LazyColumn(Modifier.fillMaxSize()) {
                items(waiting, key = { (_, p) -> p.id }) { (ref, p) ->
                    WaitingRow(ref, p, repo, onClick = { onOpen(ref, "p:${p.id}") },   // the waiting bubble's row id (ChatRows)
                        onSendNow = { waiting = waiting.filter { it.second.id != p.id }; scope.launch { repo.sendNow(ref, p); reload() } },
                        onDelete = { repo.discard(p.id); reload() })
                    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WaitingRow(ref: ChatRef, p: Pending, repo: Repo, onClick: () -> Unit, onSendNow: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { menu = true }, onLongClickLabel = stringResource(R.string.options)).padding(horizontal = 16.dp, vertical = 10.dp).testTag("waiting-row"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(ref, repo, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ref.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("⏰ " + sendTime(p.sendAt) + if (p.repeat != null) " 🔁" else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                val plain = Markdown.render(p.text, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary).text
                Text(plain.replace('\n', ' '), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                // AII-178: say how often a repeating one goes, not just 🔁.
                val every = when (p.repeat) { "day" -> R.string.every_day; "week" -> R.string.every_week; else -> null }
                if (every != null) Text("🔁 " + stringResource(every), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.send_now)) }, modifier = Modifier.testTag("waiting-send-now"), onClick = { menu = false; onSendNow() })
            DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, modifier = Modifier.testTag("waiting-delete"), onClick = { menu = false; onDelete() })
        }
    }
}
