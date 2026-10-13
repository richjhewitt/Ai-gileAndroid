package com.smizflark.aigile.messenger.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.BuildConfig
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Crash
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.work.CheckWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repo: Repo, onBack: () -> Unit, onSignedOut: () -> Unit) {
    val s = repo.store
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf(s.alerts) }
    var quietOn by remember { mutableStateOf(s.quietOn) }
    var from by remember { mutableStateOf(s.quietFrom) }
    var to by remember { mutableStateOf(s.quietTo) }
    var note by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableIntStateOf(0) }   // bumps after a change, so the connection text re-reads the store
    var confirmSignOut by remember { mutableStateOf(false) }
    var crash by remember { mutableStateOf(Crash.last(ctx)) }
    var bug by remember { mutableStateOf("") }
    var bugNote by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) note = "Alerts need permission: allow notifications in Android settings."
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Connection", style = MaterialTheme.typography.titleMedium)
            key(saved) {
                Text(if (s.demo) "Demo: made-up chats, nothing leaves the phone"
                    else "Board: ${s.url}\nSigned in as ${s.owner}\nPhone key: ${Mapper.maskKey(s.key)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("connection"))
            }
            ChangeBoard(repo, onSaved = { owner ->
                saved++; note = "Saved: signed in as $owner"
                if (s.alerts) CheckWorker.schedule(ctx)
            })
            HorizontalDivider()

            AppearanceSettings(s)
            HorizontalDivider()

            Text("Alerts", style = MaterialTheme.typography.titleMedium)
            Toggle("New message alerts", "Checks about every 15 minutes, one alert per batch", alerts) {
                alerts = it; s.alerts = it
                if (it) {
                    if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    if (!s.demo) CheckWorker.schedule(ctx)
                } else CheckWorker.cancel(ctx)
            }
            Toggle("Quiet hours", "No alerts from ${hour(from)} to ${hour(to)}", quietOn) { quietOn = it; s.quietOn = it }
            if (quietOn) {
                Stepper("From", from) { from = it; s.quietFrom = it }
                Stepper("To", to) { to = it; s.quietTo = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!s.demo) OutlinedButton(onClick = {
                    scope.launch { note = runCatching { CheckWorker.check(ctx); "Checked: all up to date." }.getOrElse { it.message } }
                }) { Text("Check now") }
                OutlinedButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }) { Text("Sound and vibration") }
            }
            note?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("note")) }
            HorizontalDivider()

            Text("Report a bug", style = MaterialTheme.typography.titleMedium)
            if (crash != null) Text("The app crashed last time. Sending includes the crash log.",
                color = MaterialTheme.colorScheme.error)
            OutlinedTextField(bug, { bug = it }, Modifier.fillMaxWidth(), label = { Text("What went wrong?") }, minLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !sending && !s.demo && (bug.isNotBlank() || crash != null), onClick = {
                    sending = true
                    scope.launch {
                        bugNote = runCatching { repo.reportBug(Crash.report(bug, crash, BuildConfig.VERSION_NAME)) }.fold(
                            { id -> Crash.clear(ctx); crash = null; bug = ""; "Sent to the team" + (id?.let { " as $it" } ?: "") + "." },
                            { it.message })
                        sending = false
                    }
                }) { Text("Send to the team") }
                if (crash != null) TextButton(onClick = { Crash.clear(ctx); crash = null }) { Text("Forget the crash") }
            }
            if (s.demo) Text("In the demo nothing leaves the phone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            bugNote?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            HorizontalDivider()

            TextButton(onClick = { confirmSignOut = true }) {
                Text(if (s.demo) "Leave the demo" else "Sign out of this board", color = MaterialTheme.colorScheme.error)
            }
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (confirmSignOut) AlertDialog(
        onDismissRequest = { confirmSignOut = false },
        title = { Text(if (s.demo) "Leave the demo?" else "Sign out?") },
        text = {
            Text(if (s.demo) "You go back to the connect screen."
                else "This phone forgets the board, its key and the chats saved on it. Messages not yet sent are lost.")
        },
        confirmButton = {
            TextButton(onClick = { confirmSignOut = false; CheckWorker.cancel(ctx); repo.signOut(); onSignedOut() }) {
                Text(if (s.demo) "Leave" else "Sign out", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
    )
}

/* ---------- change the board address or key without signing out ---------- */

@Composable
private fun ChangeBoard(repo: Repo, onSaved: (String) -> Unit) {
    val s = repo.store
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf(s.url) }
    var key by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (!open) {
        OutlinedButton(onClick = { open = true; url = s.url; key = ""; error = null }, modifier = Modifier.testTag("change")) {
            Text(if (s.demo) "Connect to your board" else "Change address or key")
        }
        return
    }
    OutlinedTextField(
        value = url, onValueChange = { url = it; error = null },
        label = { Text("Board address") }, placeholder = { Text("my-pc.tail1234.ts.net") },
        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("url"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
    )
    OutlinedTextField(
        value = key, onValueChange = { key = it; error = null },
        label = { Text("Phone key") },
        placeholder = { if (!s.demo && s.key.isNotEmpty()) Text("Leave blank to keep ${Mapper.maskKey(s.key)}") },
        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("key"),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
    )
    if (Mapper.insecure(Mapper.normaliseUrl(url))) Text(
        "⚠️ Plain http outside Tailscale sends your key unprotected. Use https or your Tailscale address.",
        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
    )
    if (Mapper.boardChanged(s.demo, s.url, Mapper.normaliseUrl(url))) Text(
        "Another board: chats saved on this phone are cleared when you save.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("error")) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(enabled = !busy, onClick = {
            if (url.isBlank()) { error = "Type your board's address first."; return@Button }
            busy = true; error = null
            scope.launch {
                try { val owner = repo.change(url, key); open = false; key = ""; onSaved(owner) }
                catch (e: Exception) { error = e.message ?: "Couldn't connect. Nothing was changed." }
                busy = false
            }
        }) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Save")
        }
        TextButton(enabled = !busy, onClick = { open = false }) { Text("Cancel") }
    }
}

private fun hour(h: Int) = "%02d:00".format(h)

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = on, onCheckedChange = onChange)
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        TextButton(onClick = { onChange((value + 23) % 24) }) { Text("−") }
        Text(hour(value), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { onChange((value + 1) % 24) }) { Text("+") }
    }
}
