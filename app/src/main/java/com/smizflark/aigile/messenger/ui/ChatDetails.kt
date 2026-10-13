package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.CardDetail
import com.smizflark.aigile.messenger.data.ChatInfo
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.togglePin
import kotlinx.coroutines.launch

/** Tap the chat name (AII-80): the team, its open cards, the pictures sent here, and the Mute and Pin switches. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetails(ref: ChatRef, repo: Repo, info: ChatInfo, onClose: () -> Unit) {
    var muted by remember { mutableStateOf(ref.key in repo.store.muted()) }
    var pinned by remember { mutableStateOf(ref.key in repo.store.pinned) }
    var shown by remember { mutableStateOf<CardDetail?>(null) }   // AII-85: the open card tapped, shown under its row
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().testTag("chat-details")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(ref, repo, 56.dp)
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(ref.name, style = MaterialTheme.typography.titleLarge)
                    Text(ref.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SwitchRow(stringResource(R.string.mute_alerts), muted, "details-mute") {
                muted = it; repo.store.setMuted(ref.key, it)
            }
            SwitchRow(stringResource(R.string.pin), pinned, "details-pin") {
                repo.store.pinned = togglePin(repo.store.pinned, ref.key); pinned = ref.key in repo.store.pinned
            }
            Text(stringResource(R.string.open_cards), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            if (info.openCards.isEmpty()) Text(stringResource(R.string.no_open_cards), color = MaterialTheme.colorScheme.onSurfaceVariant)
            info.openCards.forEach { t ->
                Text("⏳ ${t.label}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth()
                    .clickable { if (shown?.id == t.id) shown = null else scope.launch { shown = t.id?.let { repo.card(ref, it) } } }
                    .padding(vertical = 6.dp).testTag("details-card-${t.id}"))
                shown?.takeIf { it.id == t.id }?.let { CardDetails(it) }
            }
            Text(stringResource(R.string.chat_pictures), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            if (info.pictures.isEmpty()) Text(stringResource(R.string.no_chat_pictures), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else MessagePictures(info.pictures, repo)
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SwitchRow(label: String, on: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = on, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}
