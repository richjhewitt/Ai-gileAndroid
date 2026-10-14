package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Repo
import kotlinx.coroutines.launch

/** A small heading over a group of rows in Settings (AII-232); [first] skips the line above. */
@Composable
internal fun SectionHeader(title: String, first: Boolean = false) {
    if (!first) HorizontalDivider(Modifier.padding(top = 8.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("section"))
}

/* ---------- change the board address or key without signing out ---------- */

@Composable
internal fun ChangeBoard(repo: Repo, onSaved: (String) -> Unit) {
    val s = repo.store
    val scope = rememberCoroutineScope()
    val res = LocalContext.current.resources
    var open by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf(s.url) }
    var key by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (!open) {
        OutlinedButton(onClick = { open = true; url = s.url; key = ""; error = null }, modifier = Modifier.testTag("change")) {
            Text(stringResource(if (s.demo) R.string.connect_to_board else R.string.change_board))
        }
        return
    }
    OutlinedTextField(
        value = url, onValueChange = { url = it; error = null },
        label = { Text(stringResource(R.string.board_address)) }, placeholder = { Text("my-pc.tail1234.ts.net") },
        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("url"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
    )
    OutlinedTextField(
        value = key, onValueChange = { key = it; error = null },
        label = { Text(stringResource(R.string.phone_key)) },
        placeholder = { if (!s.demo && s.key.isNotEmpty()) Text(stringResource(R.string.key_keep, Mapper.maskKey(s.key))) },
        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("key"),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
    )
    if (Mapper.insecure(Mapper.normaliseUrl(url))) Text(
        stringResource(R.string.http_warning),
        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
    )
    if (Mapper.boardChanged(s.demo, s.url, Mapper.normaliseUrl(url))) Text(
        stringResource(R.string.board_changed),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("error")) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(enabled = !busy, onClick = {
            if (url.isBlank()) { error = res.getString(R.string.connect_need_address); return@Button }
            busy = true; error = null
            scope.launch {
                try { val owner = repo.change(url, key); open = false; key = ""; onSaved(owner) }
                catch (e: Exception) { error = e.message ?: res.getString(R.string.connect_failed_unchanged) }
                busy = false
            }
        }) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.save))
        }
        TextButton(enabled = !busy, onClick = { open = false }) { Text(stringResource(R.string.cancel)) }
    }
}

internal fun hour(h: Int) = "%02d:00".format(h)

@Composable
internal fun Toggle(title: String, sub: String, on: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = on, onCheckedChange = onChange)
    }
}

@Composable
internal fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        TextButton(onClick = { onChange((value + 23) % 24) }) { Text("−") }
        Text(hour(value), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { onChange((value + 1) % 24) }) { Text("+") }
    }
}

/** The words for a swipe choice (AII-216). */
@Composable
internal fun swipeLabel(c: com.smizflark.aigile.messenger.data.SwipeChoice) = stringResource(when (c) {
    com.smizflark.aigile.messenger.data.SwipeChoice.READ -> R.string.mark_read
    com.smizflark.aigile.messenger.data.SwipeChoice.MUTE -> R.string.mute
    com.smizflark.aigile.messenger.data.SwipeChoice.PIN -> R.string.pin
    com.smizflark.aigile.messenger.data.SwipeChoice.CLOSE -> R.string.close_chat
    com.smizflark.aigile.messenger.data.SwipeChoice.NOTHING -> R.string.swipe_nothing
})

/** A row showing what a swipe does now; tap it for the pick list (AII-216). */
@Composable
internal fun SwipePick(title: String, value: com.smizflark.aigile.messenger.data.SwipeChoice, tag: String, onPick: (com.smizflark.aigile.messenger.data.SwipeChoice) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = androidx.compose.ui.semantics.Role.Button) { open = true }.testTag(tag),
        verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(swipeLabel(value), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                com.smizflark.aigile.messenger.data.SwipeChoice.entries.forEach { c ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = c == value, role = androidx.compose.ui.semantics.Role.RadioButton) { onPick(c); open = false }
                        .testTag("$tag:${c.id}"), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = c == value, onClick = null)
                        Text(swipeLabel(c), Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } })
}

/** Sort chats (AII-241): Newest first or A to Z. */
@Composable
internal fun SortPick(az: Boolean, onPick: (Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val title = stringResource(R.string.sort_chats)
    Column(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = androidx.compose.ui.semantics.Role.Button) { open = true }.testTag("sortChats"),
        verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(if (az) R.string.sort_az else R.string.sort_newest), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                listOf(false to R.string.sort_newest, true to R.string.sort_az).forEach { (v, label) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = v == az, role = androidx.compose.ui.semantics.Role.RadioButton) { onPick(v); open = false }
                        .testTag("sortChats:${if (v) "az" else "newest"}"), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = v == az, onClick = null)
                        Text(stringResource(label), Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } })
}
