package com.smizflark.aigile.messenger.newmsg

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smizflark.aigile.messenger.data.ChatRef

/** A search box only once the list gets long. */
private const val SEARCH_FROM = 7

/**
 * Journey 4: pick a project's team, type, send. It lands as a new 💬 card in that team's For the team.
 * Callbacks only, like ChatsScreen: the caller passes the chats it already has (Repo.chats) and sends with
 * Repo.queue(team, text, target = null) + Repo.deliver, so a failed send stays in the outbox.
 */
@Composable
fun NewMessageScreen(
    chats: List<ChatRef>,
    /** The team you last wrote to, picked to start with. */
    lastSlug: String?,
    sending: Boolean,
    onSend: (team: ChatRef, text: String) -> Unit,
    /** Full URL for a board picture path; null in the demo (emoji only). */
    picUrl: ((String) -> String)? = null,
    error: String? = null,
    modifier: Modifier = Modifier,
) {
    val teams = NewMessage.teams(chats)
    var pickedId by rememberSaveable { mutableStateOf(NewMessage.preselect(teams, lastSlug)?.id) }
    var query by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    val picked = teams.firstOrNull { it.id == pickedId }

    Column(modifier.fillMaxSize().imePadding()) {
        if (teams.size >= SEARCH_FROM) OutlinedTextField(
            query, { query = it }, singleLine = true, placeholder = { Text("Find a project") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.weight(1f)) {
            if (teams.isEmpty()) item {
                Text(
                    "No projects yet. Pull down on Chats to refresh.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp),
                )
            }
            items(NewMessage.search(teams, query), key = { it.key }) { t ->
                TeamRow(t, t.id == pickedId, picUrl) { pickedId = t.id }
            }
        }
        if (error != null) Text(
            error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                text, { text = it }, maxLines = 6, modifier = Modifier.weight(1f),
                placeholder = { Text(picked?.let { "Message ${it.name}" } ?: "Pick a project first") },
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = { picked?.let { onSend(it, text.trim()) } },
                enabled = NewMessage.canSend(picked, text, sending),
            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
        }
    }
}

@Composable
private fun TeamRow(t: ChatRef, picked: Boolean, picUrl: ((String) -> String)?, onPick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            val pic = t.pic
            if (pic != null && picUrl != null) AsyncImage(pic.let(picUrl), contentDescription = null, modifier = Modifier.fillMaxSize())
            else Text(t.emoji, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (t.subtitle.isNotBlank()) Text(
                t.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        RadioButton(selected = picked, onClick = onPick)
    }
}
