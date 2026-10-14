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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.Stars

/** Every starred message from all chats, newest first (AII-140); tap one and its chat opens at it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredScreen(repo: Repo, onOpen: (ChatRef, String) -> Unit, onBack: () -> Unit) {
    val stars = remember { Stars.everywhere(repo.store.stars(), repo.store.cachedChats() ?: emptyList()) }
    Scaffold(
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
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpen(ref, s.key) }.padding(horizontal = 16.dp, vertical = 10.dp).testTag("starred-row"),
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
                            Text("★ " + plain.lines().firstOrNull { it.isNotBlank() }.orEmpty().trim(),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}
