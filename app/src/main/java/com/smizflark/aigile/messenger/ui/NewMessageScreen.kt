package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.R
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Repo
import kotlinx.coroutines.launch

/** Journey 4: a new 💬 message to any team (or a friend, or the Lead Scrum Master). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(repo: Repo, draft: String = "", onBack: () -> Unit, onSent: (ChatRef) -> Unit) {
    var chats by remember { mutableStateOf(repo.store.cachedChats() ?: emptyList()) }
    var pick by rememberSaveable { mutableStateOf<String?>(null) }
    // Starts from text shared in from another app; a new share replaces it.
    var text by rememberSaveable(draft) { mutableStateOf(draft) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { chats = repo.chats() } }

    Scaffold(topBar = {
        TopAppBar(title = { Text("New message") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding().padding(horizontal = 16.dp)) {
            Text("To", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(Modifier.weight(1f).selectableGroup()) {
                if (chats.isEmpty()) item { EmptyState(androidx.compose.ui.res.stringResource(R.string.empty_pick_team)) }
                items(chats, key = { it.key }) { c ->
                    Row(
                        Modifier.fillMaxWidth().selectable(pick == c.key, role = Role.RadioButton) { pick = c.key }.padding(vertical = 6.dp).testTag("to:${c.key}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = pick == c.key, onClick = null)
                        Avatar(c, repo, 36.dp)
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(c.name, style = MaterialTheme.typography.titleSmall)
                            Text(if (c.kind == Kind.LEAD) "New 💬 card for ${c.subtitle}" else c.subtitle,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            OutlinedTextField(
                value = text, onValueChange = { text = it; error = null }, minLines = 3, maxLines = 8,
                placeholder = { Text("What do you want to say?") },
                trailingIcon = { MicButton { text = Dictate.join(text, it); error = null } }, modifier = Modifier.fillMaxWidth().testTag("input"),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
            Button(
                onClick = {
                    val to = chats.firstOrNull { it.key == pick } ?: run { error = "Pick who it's for."; return@Button }
                    busy = true
                    scope.launch {
                        try {
                            repo.source.send(to, text.trim(), null)
                            onSent(to)
                        } catch (e: Exception) { error = e.message }
                        busy = false
                    }
                },
                enabled = !busy && text.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = 52.dp).testTag("send"),
            ) { Text(if (busy) "Sending…" else "Send") }
        }
    }
}
