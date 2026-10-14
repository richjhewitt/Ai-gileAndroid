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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.Star
import com.smizflark.aigile.messenger.data.Stars

/** Every starred message from all chats, newest first (AII-140); tap one and its chat opens at it, hold it to remove the star (AII-143). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredScreen(repo: Repo, onOpen: (ChatRef, String) -> Unit, onBack: () -> Unit) {
    val chats = remember { repo.store.cachedChats() ?: emptyList() }
    var all by remember { mutableStateOf(repo.store.stars()) }
    val stars = remember(all) { Stars.everywhere(all, chats) }
    val snack = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                title = { Text(stringResource(R.string.starred)) },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (stars.isEmpty()) EmptyState(stringResource(R.string.no_starred))
            LazyColumn(Modifier.fillMaxSize()) {
                items(stars, key = { (ref, s) -> "${ref.key}|${s.key}" }) { (ref, s) ->
                    StarRow(ref, s, repo, onClick = { onOpen(ref, s.key) },
                        onUnstar = {
                            all = Stars.toggle(all, s); repo.store.setStars(all)
                            // AII-156: a bar with Undo puts the star straight back.
                            scope.launch {
                                snack.currentSnackbarData?.dismiss()
                                val r = snack.showSnackbar(context.getString(R.string.star_removed), context.getString(R.string.undo), duration = androidx.compose.material3.SnackbarDuration.Short)
                                if (r == androidx.compose.material3.SnackbarResult.ActionPerformed && !Stars.has(all, s.chat, s.key)) { all = all + s; repo.store.setStars(all) }
                            }
                        })
                    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StarRow(ref: ChatRef, s: Star, repo: Repo, onClick: () -> Unit, onUnstar: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { menu = true }, onLongClickLabel = stringResource(R.string.options))
                .padding(horizontal = 16.dp, vertical = 10.dp).testTag("starred-row"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(ref, repo, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(listOf(ref.name, s.author).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(shortTime(s.at), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val plain = Markdown.render(s.text, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary).text   // without the ** marks
                Text("★ " + Stars.preview(plain),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.unstar)) }, modifier = Modifier.testTag("starred-unstar"),
                onClick = { menu = false; onUnstar() })
        }
    }
}
