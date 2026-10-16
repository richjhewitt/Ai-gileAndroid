package com.smizflark.aigile.messenger.ui

import androidx.compose.ui.res.stringResource

import com.smizflark.aigile.messenger.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
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
import com.smizflark.aigile.messenger.data.Recent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.TextButton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Search across all chats (AII-30): searches what the phone has at once, then fetches every chat and searches again. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
/** [onOpen] gets the chat and the message found (null for a chat-name match), so the chat can open at it (AII-105). */
fun SearchScreen(repo: Repo, onOpen: (ChatRef, String?) -> Unit, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val chats = remember { repo.store.cachedChats() ?: emptyList() }
    var convs by remember { mutableStateOf(repo.cachedConversations(chats)) }
    var fetching by remember { mutableStateOf(true) }
    var pictures by remember { mutableStateOf(false) }
    var links by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf(false) }   // AII-267
    var starred by remember { mutableStateOf(false) }
    var last7 by remember { mutableStateOf(false) }   // AII-258
    var from by remember { mutableStateOf<String?>(null) }   // AII-386
    var fromMenu by remember { mutableStateOf(false) }
    val hits = remember(query, convs, pictures, links, files, starred, last7, from) { Search.run(query, chats, convs, pictures, links, if (starred) repo.store.stars() else null, last7, files, from) }
    val focus = remember { FocusRequester() }
    var recent by remember { mutableStateOf(repo.store.recentSearches()) }
    /** AII-155: a search counts once you press Search or open a result. */
    fun keep(q: String) { recent = Recent.add(recent, q); repo.store.setRecentSearches(recent) }

    LaunchedEffect(Unit) {
        // After the first frame, so the box is laid out; never crash Search over the keyboard not popping up.
        androidx.compose.runtime.withFrameNanos { }
        runCatching { focus.requestFocus() }
        // Chats never opened on this phone aren't cached yet; one quiet fetch each, failures just keep the cached copy.
        coroutineScope { chats.map { c -> async { runCatching { repo.conversation(c) } } }.awaitAll() }
        convs = repo.cachedConversations(chats); fetching = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                title = {
                    TextField(
                        query, { query = it },
                        placeholder = { Text(stringResource(R.string.search_all)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keep(query) }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("search"),
                    )
                },
                actions = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, stringResource(R.string.clear)) } },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (fetching) LinearProgressIndicator(Modifier.fillMaxWidth())
            val q = query.trim()
            // AII-192: the same chips as the chats list.
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = pictures, onClick = { pictures = !pictures }, label = { Text(stringResource(R.string.search_pictures)) },
                    modifier = Modifier.testTag("search-pictures"))
                FilterChip(selected = links, onClick = { links = !links }, label = { Text(stringResource(R.string.search_links)) },
                    modifier = Modifier.testTag("search-links"))
                FilterChip(selected = files, onClick = { files = !files }, label = { Text(stringResource(R.string.search_files)) },
                    modifier = Modifier.testTag("search-files"))
                FilterChip(selected = starred, onClick = { starred = !starred }, label = { Text(stringResource(R.string.starred)) },
                    modifier = Modifier.testTag("search-starred"))
                FilterChip(selected = last7, onClick = { last7 = !last7 }, label = { Text(stringResource(R.string.search_last7)) },
                    modifier = Modifier.testTag("search-last7"))
                // AII-386: pick a person; the chip then reads "From: name" with an X to clear it.
                androidx.compose.foundation.layout.Box {
                    val fromName = from?.let { if (it == Search.YOU) stringResource(R.string.search_from_you) else it }
                    val clearLabel = stringResource(R.string.search_from_clear)
                    FilterChip(selected = from != null, onClick = { fromMenu = true },
                        label = { Text(if (fromName == null) stringResource(R.string.search_from) else stringResource(R.string.search_from_named, fromName)) },
                        trailingIcon = if (from != null) { {
                            Icon(Icons.Default.Close, clearLabel, Modifier.size(18.dp).clickable { from = null }.testTag("search-from-clear"))
                        } } else null,
                        modifier = Modifier.testTag("search-from"))
                    androidx.compose.material3.DropdownMenu(expanded = fromMenu, onDismissRequest = { fromMenu = false }) {
                        val people = remember(convs) { Search.authors(convs) }
                        people.forEach { p ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(if (p == Search.YOU) stringResource(R.string.search_from_you) else p) },
                                onClick = { from = p; fromMenu = false },
                                modifier = Modifier.testTag("search-from-" + if (p == Search.YOU) "you" else p))
                        }
                    }
                }
            }
            val filtered = pictures || links || files || starred || last7 || from != null
            if (filtered && hits.isEmpty() && fetching) EmptyState(stringResource(R.string.search_searching))
            else if (filtered && hits.isEmpty()) EmptyState(stringResource(R.string.search_none_filtered))
            else if (filtered) { }
            else if (q.length < 2 && recent.isNotEmpty()) RecentList(recent, onPick = { query = it; keep(it) }) { recent = emptyList(); repo.store.setRecentSearches(recent) }
            else if (q.length < 2) EmptyState(androidx.compose.ui.res.stringResource(R.string.search_hint))
            else if (hits.isEmpty() && fetching) EmptyState(androidx.compose.ui.res.stringResource(R.string.search_searching))
            else if (hits.isEmpty()) EmptyState(androidx.compose.ui.res.stringResource(R.string.search_no_results, q),
                button = androidx.compose.ui.res.stringResource(R.string.search_clear)) { query = "" }
            LazyColumn(Modifier.fillMaxSize()) {
                items(hits, key = { "${it.ref.key}|${it.bubble?.key}" }) { h ->
                    HitRow(h, repo) { keep(query); onOpen(h.ref, h.bubble?.key) }
                    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

/** AII-155: the last searches, shown before you type; tap one to search it again. */
@Composable
private fun RecentList(recent: List<String>, onPick: (String) -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("recent")) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.search_recent), style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear, modifier = Modifier.testTag("recent-clear")) { Text(stringResource(R.string.search_recent_clear)) }
        }
        recent.forEach { r ->
            Row(Modifier.fillMaxWidth().clickable { onPick(r) }.padding(horizontal = 16.dp, vertical = 12.dp).testTag("recent:$r"),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Text(r, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                if (b == null) androidx.compose.ui.text.AnnotatedString(stringResource(R.string.open_chat))
                else androidx.compose.ui.text.buildAnnotatedString {
                    append(h.snippet)
                    h.marks.forEach { addStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface), it.first, it.last + 1) }
                },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
