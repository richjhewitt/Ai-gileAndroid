package com.smizflark.aigile.messenger.chats

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
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smizflark.aigile.messenger.data.ChatRef

/**
 * Journey 2: one list of every chat (each project's lead, the 🧭 Lead Scrum Master, friends 🔒).
 * Callbacks only: the caller loads chats (Repo.chats), keeps `seen` (Store.seen) and opens a chat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    chats: List<ChatRef>,
    seen: Map<String, String>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpen: (ChatRef) -> Unit,
    /** Full URL for a board picture path; null in the demo (emoji only). */
    picUrl: ((String) -> String)? = null,
    error: String? = null,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (error != null) item {
                Text(
                    error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            if (chats.isEmpty() && !refreshing) item {
                Text(
                    "No chats yet. Pull down to refresh.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp),
                )
            }
            ChatList.sections(chats).forEach { (title, rows) ->
                item(key = "h:$title") {
                    Text(
                        title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(rows, key = { it.key }) { c ->
                    ChatRow(c, ChatList.unread(c, seen), picUrl, onOpen)
                    HorizontalDivider(Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatRow(c: ChatRef, unread: Boolean, picUrl: ((String) -> String)?, onOpen: (ChatRef) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onOpen(c) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            val pic = c.pic
            if (pic != null && picUrl != null) AsyncImage(pic.let(picUrl), contentDescription = null, modifier = Modifier.fillMaxSize())
            else Text(c.emoji, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    ChatList.timeLabel(c.last), style = MaterialTheme.typography.labelMedium,
                    color = if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    c.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                // Questions waiting on you get a count; anything else new gets a dot.
                if (c.waiting > 0) Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("${c.waiting}") }
                else if (unread) Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}
