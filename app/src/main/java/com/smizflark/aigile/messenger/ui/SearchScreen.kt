package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Hit
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.Search
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Search across all chats (AII-30): searches what the phone has at once, then fetches every chat and searches again. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(repo: Repo, onOpen: (ChatRef) -> Unit, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val chats = remember { repo.store.cachedChats() ?: emptyList() }
    var convs by remember { mutableStateOf(repo.cachedConversations(chats)) }
    var fetching by remember { mutableStateOf(true) }
    val hits = remember(query, convs) { Search.run(query, chats, convs) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focus.requestFocus()
        // Chats never opened on this phone aren't cached yet; one quiet fetch each, failures just keep the cached copy.
        coroutineScope { chats.map { c -> async { runCatching { repo.conversation(c) } } }.awaitAll() }
        convs = repo.cachedConversations(chats); fetching = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = {
                    TextField(
                        query, { query = it },
                        placeholder = { Text("Search all chats") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("search"),
                    )
                },
                actions = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear") } },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (fetching) LinearProgressIndicator(Modifier.fillMaxWidth())
            val hint = when {
                query.trim().length < 2 -> "Type at least 2 letters to search every chat."
                hits.isEmpty() && fetching -> "Searching…"
                hits.isEmpty() -> "Nothing found for \"${query.trim()}\"."
                else -> null
            }
            if (hint != null) Text(hint, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(Modifier.fillMaxSize()) {
                items(hits, key = { "${it.ref.key}|${it.bubble?.key}" }) { h ->
                    HitRow(h, repo) { onOpen(h.ref) }
                    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun HitRow(h: Hit, repo: Repo, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp).testTag("hit:${h.ref.key}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(h.ref, repo, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val b = h.bubble
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (b == null) h.ref.name else "${h.ref.name} · ${b.author}",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (b != null) Text(shortTime(b.at), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (b == null) "Open chat" else h.snippet,
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
