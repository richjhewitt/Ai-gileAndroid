package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.R

import androidx.compose.ui.res.stringResource

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
import com.smizflark.aigile.messenger.data.Replies
import androidx.compose.ui.res.stringArrayResource
import com.smizflark.aigile.messenger.work.CheckWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repo: Repo, onBack: () -> Unit, onSignedOut: () -> Unit) {
    val s = repo.store
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf(s.alerts) }
    var lockOn by remember { mutableStateOf(s.lock) }
    var quietOn by remember { mutableStateOf(s.quietOn) }
    var sentAlerts by remember { mutableStateOf(s.sentAlerts) }   // AII-175
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
        if (!ok) note = ctx.getString(R.string.alerts_permission)
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.settings)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.connection), style = MaterialTheme.typography.titleMedium)
            key(saved) {
                Text(if (s.demo) stringResource(R.string.demo_note)
                    else stringResource(R.string.board_info, s.url, s.owner, Mapper.maskKey(s.key)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("connection"))
            }
            ChangeBoard(repo, onSaved = { owner ->
                saved++; note = ctx.getString(R.string.saved_as, owner)
                if (s.alerts) CheckWorker.schedule(ctx)
            })
            HorizontalDivider()

            AppearanceSettings(s)
            Toggle(stringResource(R.string.enter_sends), stringResource(R.string.enter_sends_sub), Appearance.enterSends, Modifier.testTag("enterSends")) {
                Appearance.enterSends = it; s.enterSends = it
            }
            Toggle(stringResource(R.string.quick_replies), stringResource(R.string.quick_replies_sub), Appearance.quickReplies, Modifier.testTag("quickReplies")) {
                Appearance.quickReplies = it; s.quickReplies = it
            }
            // AII-180: write your own, one per line; clear the box to get the starting ones back.
            if (Appearance.quickReplies) {
                val starting = stringArrayResource(R.array.quick_replies_list).joinToString("\n")
                var mine by remember { mutableStateOf(s.quickReplyText.ifBlank { starting }) }
                OutlinedTextField(mine, { mine = it; s.quickReplyText = it; Appearance.replies = Replies.parse(it) },
                    Modifier.fillMaxWidth().testTag("quickReplyText"), label = { Text(stringResource(R.string.quick_replies_mine)) }, minLines = 2)
            }
            HorizontalDivider()

            Text(stringResource(R.string.privacy), style = MaterialTheme.typography.titleMedium)
            Toggle(stringResource(R.string.lock), stringResource(R.string.lock_sub), lockOn) {
                if (it && !Lock.available(ctx)) note = ctx.getString(R.string.lock_unavailable)
                else { lockOn = it; s.lock = it }
            }
            HorizontalDivider()

            Text(stringResource(R.string.alerts), style = MaterialTheme.typography.titleMedium)
            Toggle(stringResource(R.string.alerts_new), stringResource(R.string.alerts_new_sub), alerts) {
                alerts = it; s.alerts = it
                if (it) {
                    if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    if (!s.demo) CheckWorker.schedule(ctx)
                } else CheckWorker.cancel(ctx)
            }
            Toggle(stringResource(R.string.alerts_sent_later), stringResource(R.string.alerts_sent_later_sub), sentAlerts, Modifier.testTag("sentAlerts")) { sentAlerts = it; s.sentAlerts = it }
            Toggle(stringResource(R.string.quiet_hours), stringResource(R.string.quiet_sub, hour(from), hour(to)), quietOn) { quietOn = it; s.quietOn = it }
            if (quietOn) {
                Stepper(stringResource(R.string.quiet_from), from) { from = it; s.quietFrom = it }
                Stepper(stringResource(R.string.quiet_to), to) { to = it; s.quietTo = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!s.demo) OutlinedButton(onClick = {
                    scope.launch { note = runCatching { CheckWorker.check(ctx); ctx.getString(R.string.checked_ok) }.getOrElse { it.message } }
                }) { Text(stringResource(R.string.check_now)) }
                OutlinedButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }) { Text(stringResource(R.string.sound_vibration)) }
            }
            note?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("note")) }
            HorizontalDivider()

            Text(stringResource(R.string.report_bug), style = MaterialTheme.typography.titleMedium)
            if (crash != null) Text(stringResource(R.string.crash_note),
                color = MaterialTheme.colorScheme.error)
            OutlinedTextField(bug, { bug = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.bug_hint)) }, minLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !sending && !s.demo && (bug.isNotBlank() || crash != null), onClick = {
                    sending = true
                    scope.launch {
                        bugNote = runCatching { repo.reportBug(Crash.report(bug, crash, BuildConfig.VERSION_NAME)) }.fold(
                            { id -> Crash.clear(ctx); crash = null; bug = ""; if (id != null) ctx.getString(R.string.bug_sent_as, id) else ctx.getString(R.string.bug_sent) },
                            { it.message })
                        sending = false
                    }
                }) { Text(stringResource(R.string.bug_send)) }
                if (crash != null) TextButton(onClick = { Crash.clear(ctx); crash = null }) { Text(stringResource(R.string.crash_forget)) }
            }
            if (s.demo) Text(stringResource(R.string.demo_nothing_leaves), color = MaterialTheme.colorScheme.onSurfaceVariant)
            bugNote?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            HorizontalDivider()

            TextButton(onClick = { confirmSignOut = true }) {
                Text(stringResource(if (s.demo) R.string.leave_demo else R.string.sign_out), color = MaterialTheme.colorScheme.error)
            }
            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (confirmSignOut) AlertDialog(
        onDismissRequest = { confirmSignOut = false },
        title = { Text(stringResource(if (s.demo) R.string.leave_demo_title else R.string.sign_out_title)) },
        text = {
            Text(stringResource(if (s.demo) R.string.leave_demo_text else R.string.sign_out_text))
        },
        confirmButton = {
            TextButton(onClick = { confirmSignOut = false; CheckWorker.cancel(ctx); repo.signOut(); onSignedOut() }) {
                Text(stringResource(if (s.demo) R.string.leave else R.string.sign_out_short), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

/* ---------- change the board address or key without signing out ---------- */

@Composable
private fun ChangeBoard(repo: Repo, onSaved: (String) -> Unit) {
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

private fun hour(h: Int) = "%02d:00".format(h)

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
